/*
 * Copyright 2017 Crown Copyright
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
 *
 */

package stroom.search.elastic.search;

import stroom.annotation.api.AnnotationFields;
import stroom.query.api.v2.DateTimeSettings;
import stroom.query.api.v2.ExpressionOperator;
import stroom.query.api.v2.Query;
import stroom.query.common.v2.Coprocessor;
import stroom.query.common.v2.Coprocessors;
import stroom.search.elastic.shared.ElasticIndexDoc;
import stroom.search.extraction.ExpressionFilter;
import stroom.search.extraction.ExtractionDecoratorFactory;
import stroom.search.extraction.StoredDataQueue;
import stroom.security.api.SecurityContext;
import stroom.task.api.TaskContext;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;

class ElasticClusterSearchTaskHandler {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(ElasticClusterSearchTaskHandler.class);

    private final ElasticSearchFactory elasticSearchFactory;
    private final ExtractionDecoratorFactory extractionDecoratorFactory;
    private final SecurityContext securityContext;

    private final AtomicLong hitCount = new AtomicLong();
    private final AtomicLong extractionCount = new AtomicLong();

    private TaskContext taskContext;

    @Inject
    ElasticClusterSearchTaskHandler(final ElasticSearchFactory elasticSearchFactory,
                                    final ExtractionDecoratorFactory extractionDecoratorFactory,
                                    final SecurityContext securityContext) {
        this.elasticSearchFactory = elasticSearchFactory;
        this.extractionDecoratorFactory = extractionDecoratorFactory;
        this.securityContext = securityContext;
    }

    public void exec(final TaskContext taskContext,
                     final ElasticAsyncSearchTask task,
                     final ElasticIndexDoc elasticIndex,
                     final Query query,
                     final String[] storedFields,
                     final long now,
                     final DateTimeSettings dateTimeSettings,
                     final Coprocessors coprocessors
    ) {
        securityContext.useAsRead(() -> {
            if (!Thread.currentThread().isInterrupted()) {
                taskContext.info(() -> "Initialising...");

                try {
                    // Make sure we have been given a query.
                    if (query.getExpression() == null) {
                        throw new SearchException("Search expression has not been set");
                    }

                    // Get the stored fields that search is hoping to use.
                    if (storedFields == null || storedFields.length == 0) {
                        throw new SearchException("No stored fields have been requested");
                    }

                    if (coprocessors.size() > 0) {
                        // Start searching.
                        search(taskContext, task, elasticIndex, query, now, dateTimeSettings, coprocessors);
                    }
                } catch (final RuntimeException e) {
                    try {
                        coprocessors.getErrorConsumer().add(e);
                    } catch (final RuntimeException e2) {
                        // If we failed to send the result or the source node rejected the result because the
                        // source task has been terminated then terminate the task.
                        LOGGER.info(() -> "Terminating search because we were unable to send result");
                        Thread.currentThread().interrupt();
                    }
                } finally {
                    LOGGER.trace(() -> "Search is complete, setting searchComplete to true and " +
                            "counting down searchCompleteLatch");
                    // Tell the client that the search has completed.
                    coprocessors.getCompletionState().signalComplete();
                }
            }
        });
    }

    private void search(final TaskContext taskContext,
                        final ElasticAsyncSearchTask task,
                        final ElasticIndexDoc elasticIndex,
                        final Query query,
                        final long now,
                        final DateTimeSettings dateTimeSettings,
                        final Coprocessors coprocessors) {
        taskContext.info(() -> "Searching...");
        LOGGER.debug(() -> "Incoming search request:\n" + query.getExpression().toString());

        try {
            final StoredDataQueue storedDataQueue = extractionDecoratorFactory.create(
                    taskContext,
                    coprocessors,
                    extractionCount,
                    query);

            // Search all index shards.
            final ExpressionFilter expressionFilter = ExpressionFilter.builder()
                    .addPrefixExcludeFilter(AnnotationFields.ANNOTATION_FIELD_PREFIX)
                    .build();
            final ExpressionOperator expression = expressionFilter.copy(query.getExpression());
            final AtomicLong hitCount = new AtomicLong();

            elasticSearchFactory.search(
                    task,
                    elasticIndex,
                    coprocessors.getFieldIndex(),
                    now,
                    expression,
                    storedDataQueue,
                    coprocessors.getErrorConsumer(),
                    taskContext,
                    hitCount,
                    dateTimeSettings);

            // Wait for extraction to complete.
            while (!extractionDecoratorFactory.awaitCompletion(1, TimeUnit.SECONDS)) {
                updateInfo();
            }

            LOGGER.debug(() -> "Complete");
        } catch (final InterruptedException e) {
            LOGGER.trace(e::getMessage, e);
            // Keep interrupting this thread.
            Thread.currentThread().interrupt();
        } catch (final RuntimeException e) {
            throw SearchException.wrap(e);
        }
    }

    public void updateInfo() {
        taskContext.info(() -> "" +
                "Searching... " +
                "found "
                + hitCount.get() +
                " documents" +
                " performed " +
                extractionCount.get() +
                " extractions");
    }
}
