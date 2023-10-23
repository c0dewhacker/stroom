/*
 * Copyright 2016 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.index.lucene980;

import stroom.dictionary.api.WordListProvider;
import stroom.expression.api.DateTimeSettings;
import stroom.index.impl.IndexShardSearchConfig;
import stroom.index.impl.IndexShardWriter;
import stroom.index.impl.IndexShardWriterCache;
import stroom.index.impl.LuceneShardSearcher;
import stroom.index.lucene980.SearchExpressionQueryBuilder.SearchExpressionQuery;
import stroom.index.shared.IndexFieldsMap;
import stroom.index.shared.IndexShard;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.QueryKey;
import stroom.query.common.v2.SearchProgressLog;
import stroom.query.common.v2.SearchProgressLog.SearchPhase;
import stroom.query.language.functions.Val;
import stroom.query.language.functions.ValString;
import stroom.query.language.functions.ValuesConsumer;
import stroom.query.language.functions.ref.ErrorConsumer;
import stroom.search.impl.SearchException;
import stroom.task.api.ExecutorProvider;
import stroom.task.api.TaskContext;
import stroom.task.api.TaskContextFactory;
import stroom.task.api.TaskTerminatedException;
import stroom.task.api.TerminateHandlerFactory;
import stroom.task.api.ThreadPoolImpl;
import stroom.task.shared.ThreadPool;
import stroom.util.concurrent.UncheckedInterruptedException;
import stroom.util.io.PathCreator;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import org.apache.lucene980.document.Document;
import org.apache.lucene980.index.IndexWriter;
import org.apache.lucene980.index.IndexableField;
import org.apache.lucene980.search.IndexSearcher;
import org.apache.lucene980.search.Query;
import org.apache.lucene980.search.SearcherManager;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.LongAdder;

class Lucene980ShardSearcher implements LuceneShardSearcher {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(Lucene980ShardSearcher.class);

    public static final ThreadPool THREAD_POOL = new ThreadPoolImpl("Search Index Shard");

    private final IndexShardWriterCache indexShardWriterCache;
    private final IndexShardSearchConfig shardConfig;
    private final Executor executor;
    private final TaskContextFactory taskContextFactory;
    private final PathCreator pathCreator;

    private final QueryKey queryKey;
    private final Query query;

    Lucene980ShardSearcher(final IndexShardWriterCache indexShardWriterCache,
                           final IndexShardSearchConfig shardConfig,
                           final ExecutorProvider executorProvider,
                           final TaskContextFactory taskContextFactory,
                           final PathCreator pathCreator,
                           final IndexFieldsMap indexFieldsMap,
                           final ExpressionOperator expression,
                           final WordListProvider dictionaryStore,
                           final DateTimeSettings dateTimeSettings,
                           final QueryKey queryKey) {
        this.queryKey = queryKey;
        this.indexShardWriterCache = indexShardWriterCache;
        this.shardConfig = shardConfig;
        this.executor = executorProvider.get(THREAD_POOL);
        this.taskContextFactory = taskContextFactory;
        this.pathCreator = pathCreator;

        final SearchExpressionQueryBuilder searchExpressionQueryBuilder = new SearchExpressionQueryBuilder(
                dictionaryStore,
                indexFieldsMap,
                dateTimeSettings);
        SearchExpressionQuery searchExpressionQuery = searchExpressionQueryBuilder.buildQuery(expression);
        query = searchExpressionQuery.getQuery();

        // Make sure the query was created successfully.
        if (query == null) {
            throw new SearchException("Failed to build Lucene query given expression");
        } else {
            LOGGER.debug(() -> "Lucene Query is " + query);
        }
    }

    @Override
    public void searchShard(final TaskContext taskContext,
                            final IndexShard indexShard,
                            final String[] storedFieldNames,
                            final LongAdder hitCount,
                            final int shardNumber,
                            final int shardTotal,
                            final ValuesConsumer valuesConsumer,
                            final ErrorConsumer errorConsumer) {
        IndexShardSearcher indexShardSearcher = null;
        try {
            if (!taskContext.isTerminated()) {
                taskContext.reset();
                taskContext.info(() ->
                        "Searching shard " + shardNumber + " of " + shardTotal +
                                " (id=" + indexShard.getId() + ")", LOGGER);

                final IndexWriter indexWriter = getWriter(indexShard.getId());

                indexShardSearcher = new IndexShardSearcher(indexShard, indexWriter, pathCreator);

                // Start searching.
                searchShard(
                        taskContext,
                        storedFieldNames,
                        hitCount,
                        indexShardSearcher,
                        valuesConsumer,
                        errorConsumer);
            }
        } catch (final RuntimeException e) {
            LOGGER.debug(e::getMessage, e);
            error(errorConsumer, e);

        } finally {
            if (indexShardSearcher != null) {
                taskContext.info(() -> "Closing searcher for index shard " + indexShard.getId(), LOGGER);
                indexShardSearcher.destroy();
            }
        }
    }

    private IndexWriter getWriter(final Long indexShardId) {
        IndexWriter indexWriter = null;

        // Load the current index shard.
        final IndexShardWriter indexShardWriter = indexShardWriterCache.getWriterByShardId(indexShardId);
        if (indexShardWriter instanceof final Lucene980IndexShardWriter writer) {
            indexWriter = writer.getWriter();
        }

        return indexWriter;
    }

    private void searchShard(final TaskContext parentContext,
                             final String[] storedFieldNames,
                             final LongAdder hitCount,
                             final IndexShardSearcher indexShardSearcher,
                             final ValuesConsumer valuesConsumer,
                             final ErrorConsumer errorConsumer) {
        SearchProgressLog.increment(queryKey, SearchPhase.INDEX_SHARD_SEARCH_TASK_HANDLER_SEARCH_SHARD);

        // Get the index shard that this searcher uses.
        final IndexShard indexShard = indexShardSearcher.getIndexShard();

        // If there is an error building the query then it will be null here.
        if (query != null) {
            final int maxDocIdQueueSize = shardConfig.getMaxDocIdQueueSize();
            LOGGER.debug(() -> "Creating docIdStore with size " + maxDocIdQueueSize);
            final DocIdQueue docIdQueue = new DocIdQueue(maxDocIdQueueSize);
            try {
                final SearcherManager searcherManager = indexShardSearcher.getSearcherManager();
                final IndexSearcher searcher = searcherManager.acquire();
                try {
                    final Runnable runnable = taskContextFactory.childContext(
                            parentContext,
                            "Index Searcher",
                            TerminateHandlerFactory.NOOP_FACTORY,
                            taskContext -> {
                                try {
                                    LOGGER.logDurationIfDebugEnabled(() -> {
                                        try {
                                            // Create a collector.
                                            final IndexShardHitCollector collector = new IndexShardHitCollector(
                                                    taskContext,
                                                    queryKey,
                                                    indexShard,
                                                    query,
                                                    docIdQueue,
                                                    hitCount);

                                            searcher.search(query, collector);

                                            LOGGER.debug("Shard search complete. {}, query term [{}]",
                                                    collector,
                                                    query);

                                        } catch (final TaskTerminatedException e) {
                                            // Expected error on early completion.
                                            LOGGER.trace(e::getMessage, e);
                                        } catch (final IOException e) {
                                            error(errorConsumer, e);
                                        }
                                    }, () -> "searcher.search()");
                                } finally {
                                    docIdQueue.complete();
                                }
                            });
                    // Start searching async.
                    CompletableFuture.runAsync(runnable, executor);

                    // Start converting found docIds into stored data values
                    boolean done = false;
                    while (!done) {
                        // Uncomment this to slow searches down in dev
//                            ThreadUtil.sleepAtLeastIgnoreInterrupts(1_000);

                        // Take the next item.
                        // When we get null we are done.
                        Integer docId = docIdQueue.take();
                        if (parentContext.isTerminated()) {
                            // We are terminating so take from the queue until we get null so the process adding to
                            // the queue has a chance to complete.
                            while (docId != null) {
                                docId = docIdQueue.take();
                            }
                            done = true;
                        } else if (docId == null) {
                            done = true;
                        } else {
                            try {
                                // If we have a doc id then retrieve the stored data for it.
                                SearchProgressLog.increment(queryKey,
                                        SearchPhase.INDEX_SHARD_SEARCH_TASK_HANDLER_DOC_ID_STORE_TAKE);
                                getStoredData(storedFieldNames, valuesConsumer, searcher, docId, errorConsumer);
                            } catch (final RuntimeException e) {
                                error(errorConsumer, e);
                            }
                        }
                    }

                } finally {
                    searcherManager.release(searcher);
                }
            } catch (final RuntimeException | IOException e) {
                error(errorConsumer, e);
            }
        }
    }

    /**
     * This method takes a list of document id's and extracts the stored fields
     * that are required for data display. In some cases such as batch search we
     * only want to get stream and event ids, in these cases no values are
     * retrieved, only stream and event ids.
     */
    private void getStoredData(final String[] storedFieldNames,
                               final ValuesConsumer valuesConsumer,
                               final IndexSearcher searcher,
                               final int docId,
                               final ErrorConsumer errorConsumer) {
        try {
            SearchProgressLog.increment(queryKey, SearchPhase.INDEX_SHARD_SEARCH_TASK_HANDLER_GET_STORED_DATA);
            final Val[] values = new Val[storedFieldNames.length];
            final Document document = searcher.doc(docId);

            for (int i = 0; i < storedFieldNames.length; i++) {
                final String storedField = storedFieldNames[i];

                // If the field is null then it isn't stored.
                if (storedField != null) {
                    final IndexableField indexableField = document.getField(storedField);

                    // If the field is not in fact stored then it will be null here.
                    if (indexableField != null) {
                        final String value = indexableField.stringValue();
                        if (value != null) {
                            final String trimmed = value.trim();
                            if (trimmed.length() > 0) {
                                values[i] = ValString.create(trimmed);
                            }
                        }
                    }
                }
            }

            valuesConsumer.accept(Val.of(values));
        } catch (final UncheckedInterruptedException e) {
            throw e;
        } catch (final IOException | RuntimeException e) {
            error(errorConsumer, e);
        }
    }

    private void error(final ErrorConsumer errorConsumer,
                       final Throwable t) {
        if (!(t instanceof UncheckedInterruptedException)) {
            if (errorConsumer == null) {
                LOGGER.error(t::getMessage, t);
            } else {
                errorConsumer.add(t);
            }
        }
    }
}
