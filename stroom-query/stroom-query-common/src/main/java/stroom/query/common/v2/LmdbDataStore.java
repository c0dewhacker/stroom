/*
 * Copyright 2018 Crown Copyright
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

package stroom.query.common.v2;

import stroom.dashboard.expression.v1.Any.AnySelector;
import stroom.dashboard.expression.v1.Bottom.BottomSelector;
import stroom.dashboard.expression.v1.Expression;
import stroom.dashboard.expression.v1.FieldIndex;
import stroom.dashboard.expression.v1.Generator;
import stroom.dashboard.expression.v1.Last.LastSelector;
import stroom.dashboard.expression.v1.Nth.NthSelector;
import stroom.dashboard.expression.v1.Selection;
import stroom.dashboard.expression.v1.Selector;
import stroom.dashboard.expression.v1.Top.TopSelector;
import stroom.dashboard.expression.v1.Val;
import stroom.dashboard.expression.v1.ValNull;
import stroom.dashboard.expression.v1.ValSerialiser;
import stroom.lmdb.LmdbEnv;
import stroom.lmdb.LmdbEnv.BatchingWriteTxn;
import stroom.lmdb.LmdbEnvFactory;
import stroom.query.api.v2.QueryKey;
import stroom.query.api.v2.TableSettings;
import stroom.query.common.v2.SearchProgressLog.SearchPhase;
import stroom.util.concurrent.CompleteException;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.logging.LogUtil;
import stroom.util.logging.Metrics;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.unsafe.UnsafeByteBufferInput;
import com.esotericsoftware.kryo.unsafe.UnsafeByteBufferOutput;
import org.lmdbjava.CursorIterable;
import org.lmdbjava.CursorIterable.KeyVal;
import org.lmdbjava.Dbi;
import org.lmdbjava.EnvFlags;
import org.lmdbjava.KeyRange;
import org.lmdbjava.PutFlags;
import org.lmdbjava.Txn;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.inject.Provider;

public class LmdbDataStore implements DataStore {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(LmdbDataStore.class);

    private static final long COMMIT_FREQUENCY_MS = 1000;

    private final LmdbEnv lmdbEnv;
    private final ResultStoreConfig resultStoreConfig;
    private final int minValueSize;
    private final int maxValueSize;
    private final Dbi<ByteBuffer> dbi;

    private final CompiledField[] compiledFields;
    private final CompiledSorter<HasGenerators>[] compiledSorters;
    private final CompiledDepths compiledDepths;
    private final Sizes maxResults;
    private final AtomicLong totalResultCount = new AtomicLong();
    private final boolean limitResultCount;
    private final AtomicLong resultCount = new AtomicLong();

    private final AtomicBoolean hasEnoughData = new AtomicBoolean();
    private final AtomicBoolean shutdown = new AtomicBoolean();

    private final LmdbKVQueue queue;
    private final CountDownLatch complete = new CountDownLatch(1);
    private final CompletionState completionState = new CompletionStateImpl(this, complete);
    private final AtomicLong uniqueKey = new AtomicLong();
    private final QueryKey queryKey;
    private final String componentId;
    private final boolean producePayloads;
    private final ErrorConsumer errorConsumer;
    private final LmdbPayloadCreator payloadCreator;

    private final LmdbKey rootParentRowKey;
    private final TransferState transferState = new TransferState();

    LmdbDataStore(final LmdbEnvFactory lmdbEnvFactory,
                  final ResultStoreConfig resultStoreConfig,
                  final QueryKey queryKey,
                  final String componentId,
                  final TableSettings tableSettings,
                  final FieldIndex fieldIndex,
                  final Map<String, String> paramMap,
                  final Sizes maxResults,
                  final boolean producePayloads,
                  final Provider<Executor> executorProvider,
                  final ErrorConsumer errorConsumer) {
        this.resultStoreConfig = resultStoreConfig;
        this.maxResults = maxResults;
        this.queryKey = queryKey;
        this.componentId = componentId;
        this.producePayloads = producePayloads;
        this.errorConsumer = errorConsumer;

        queue = new LmdbKVQueue(resultStoreConfig.getValueQueueSize());
        minValueSize = (int) resultStoreConfig.getMinValueSize().getBytes();
        maxValueSize = (int) resultStoreConfig.getMaxValueSize().getBytes();
        compiledFields = CompiledFields.create(tableSettings.getFields(), fieldIndex, paramMap);
        compiledDepths = new CompiledDepths(compiledFields, tableSettings.showDetail());
        compiledSorters = CompiledSorter.create(compiledDepths.getMaxDepth(), compiledFields);
        payloadCreator = new LmdbPayloadCreator(queryKey, this, compiledFields, resultStoreConfig);

        rootParentRowKey = new LmdbKey.Builder()
                .keyBytes(Key.root().getBytes())
                .build();

        final String uuid = (queryKey + "_" + componentId + "_" + UUID.randomUUID());
        // Make safe for the file system.
        final String dirName = uuid.replaceAll("[^A-Za-z0-9]", "_");
        this.lmdbEnv = lmdbEnvFactory.builder(resultStoreConfig.getLmdbConfig())
                .withMaxDbCount(1)
                .withSubDirectory(dirName)
                .addEnvFlag(EnvFlags.MDB_NOTLS)
                .build();
        this.dbi = lmdbEnv.openDbi(uuid);

        // Find out if we have any sorting.
        boolean hasSort = false;
        for (final CompiledSorter<HasGenerators> sorter : compiledSorters) {
            if (sorter != null) {
                hasSort = true;
                break;
            }
        }

        // Determine if we are going to limit the result count.
        limitResultCount = maxResults != null && !hasSort && !compiledDepths.hasGroup();

        // Start transfer loop.
        // TODO : Use provided executor and allow it to be terminated by search termination.
        executorProvider.get().execute(this::transfer);
    }

    /**
     * Add some values to the data store.
     *
     * @param values The values to add to the store.
     */
    @Override
    public void add(final Val[] values) {
        SearchProgressLog.increment(queryKey, SearchPhase.LMDB_DATA_STORE_ADD);
        LOGGER.trace(() -> LogUtil.message("add() called for {} values", values.length));
        final int[] groupSizeByDepth = compiledDepths.getGroupSizeByDepth();
        final boolean[][] groupIndicesByDepth = compiledDepths.getGroupIndicesByDepth();
        final boolean[][] valueIndicesByDepth = compiledDepths.getValueIndicesByDepth();

        Key key = Key.root();
        LmdbKey parentRowKey = rootParentRowKey;

        for (int depth = 0; depth < groupIndicesByDepth.length; depth++) {
            final LmdbKey.Builder rowKeyBuilder = new LmdbKey.Builder();
            final Generator[] generators = new Generator[compiledFields.length];

            final int groupSize = groupSizeByDepth[depth];
            final boolean[] groupIndices = groupIndicesByDepth[depth];
            final boolean[] valueIndices = valueIndicesByDepth[depth];

            Val[] groupValues = ValSerialiser.EMPTY_VALUES;
            if (groupSize > 0) {
                groupValues = new Val[groupSize];
            }

            int groupIndex = 0;
            for (int fieldIndex = 0; fieldIndex < compiledFields.length; fieldIndex++) {
                final CompiledField compiledField = compiledFields[fieldIndex];

                final Expression expression = compiledField.getExpression();
                if (expression != null) {
                    Generator generator = null;
                    Val value = null;

                    // If this is the first level then check if we should filter out this data.
                    if (depth == 0) {
                        final CompiledFilter compiledFilter = compiledField.getCompiledFilter();
                        if (compiledFilter != null) {
                            generator = expression.createGenerator();
                            generator.set(values);

                            // If we are filtering then we need to evaluate this field
                            // now so that we can filter the resultant value.
                            value = generator.eval();

                            if (!compiledFilter.match(value.toString())) {
                                // We want to exclude this item so get out of this method ASAP.
                                return;
                            }
                        }
                    }

                    // If we are grouping at this level then evaluate the expression and add to the group values.
                    if (groupIndices[fieldIndex]) {
                        // If we haven't already created the generator then do so now.
                        if (value == null) {
                            generator = expression.createGenerator();
                            generator.set(values);
                            value = generator.eval();
                        }
                        groupValues[groupIndex++] = value;
                    }

                    // If we need a value at this level then evaluate the expression and add the value.
                    if (valueIndices[fieldIndex]) {
                        // If we haven't already created the generator then do so now.
                        if (generator == null) {
                            generator = expression.createGenerator();
                            generator.set(values);
                        }
                        generators[fieldIndex] = generator;
                    }
                }
            }

            final boolean grouped = depth <= compiledDepths.getMaxGroupDepth();
            final byte[] keyBytes;
            if (grouped) {
                // This is a grouped item.
                key = key.resolve(groupValues);
                keyBytes = key.getBytes();

                final LmdbKey rowKey = rowKeyBuilder
                        .depth(depth)
                        .parentRowKey(parentRowKey)
                        .keyBytes(keyBytes)
                        .group(true)
                        .build();
                final LmdbValue rowValue = new LmdbValue(
                        keyBytes,
                        new Generators(compiledFields, generators));
                parentRowKey = rowKey;
                put(new LmdbKV(rowKey, rowValue));

            } else {
                // This item will not be grouped.
                final long uniqueId = getUniqueId();
                key = key.resolve(uniqueId);
                keyBytes = key.getBytes();

                final LmdbKey rowKey = rowKeyBuilder
                        .depth(depth)
                        .parentRowKey(parentRowKey)
                        .uniqueId(uniqueId)
                        .group(false)
                        .build();
                final LmdbValue rowValue = new LmdbValue(
                        keyBytes,
                        new Generators(compiledFields, generators));
                put(new LmdbKV(rowKey, rowValue));
            }
        }
    }

    long getUniqueId() {
        return uniqueKey.incrementAndGet();
    }

    void put(final LmdbKV queueItem) {
        LOGGER.trace(() -> "put");
        SearchProgressLog.increment(queryKey, SearchPhase.LMDB_DATA_STORE_PUT);

        // Some searches can be terminated early if the user is not sorting or grouping.
        boolean allow = true;
        if (limitResultCount) {
            // No sorting or grouping, so we can stop the search as soon as we have the number of results requested by
            // the client
            allow = !hasEnoughData.get();
            if (allow) {
                final long currentResultCount = totalResultCount.getAndIncrement();
                if (currentResultCount >= maxResults.size(0)) {
                    allow = false;

                    // If we have enough data then we can stop transferring data and complete.
                    if (hasEnoughData.compareAndSet(false, true)) {
                        completionState.signalComplete();
                    }
                }
            }
        }

        if (allow) {
            doPut(queueItem);
        }
    }

    private void doPut(final LmdbKV queueItem) {
        try {
            queue.put(queueItem);
        } catch (final InterruptedException e) {
            LOGGER.trace(e.getMessage(), e);
            // Keep interrupting this thread.
            Thread.currentThread().interrupt();
        }
    }

    private void transfer() {
        Metrics.measure("Transfer", () -> {
            transferState.setThread(Thread.currentThread());

            final int maxPutsBeforeCommit = resultStoreConfig.getMaxPutsBeforeCommit();
            try (final BatchingWriteTxn batchingWriteTxn = lmdbEnv.openBatchingWriteTxn(maxPutsBeforeCommit)) {
                long lastCommitMs = System.currentTimeMillis();
                long uncommittedCount = 0;

                try {
                    while (!transferState.isTerminated()) {
                        LOGGER.trace("Transferring");
                        SearchProgressLog.increment(queryKey, SearchPhase.LMDB_DATA_STORE_QUEUE_POLL);
                        final LmdbKV lmdbKV = queue.poll(1, TimeUnit.SECONDS);

                        if (lmdbKV != null) {
                            insert(batchingWriteTxn, dbi, lmdbKV);
                            uncommittedCount++;
                        }

                        if (producePayloads && payloadCreator.isEmpty()) {
                            // Commit
                            LOGGER.debug(() -> "Committing for new payload");
                            batchingWriteTxn.commit();
                            lastCommitMs = System.currentTimeMillis();
                            uncommittedCount = 0;

                            // Create payload and clear the DB.
                            payloadCreator.addPayload(batchingWriteTxn, dbi, false);

                        } else if (uncommittedCount > 0) {
                            if (uncommittedCount >= maxPutsBeforeCommit ||
                                    lastCommitMs < System.currentTimeMillis() - COMMIT_FREQUENCY_MS) {

                                // Commit
                                LOGGER.debug(() -> "Committing for elapsed time");
                                batchingWriteTxn.commit();
                                lastCommitMs = System.currentTimeMillis();
                                uncommittedCount = 0;
                            }
                        }
                    }
                } catch (final InterruptedException e) {
                    LOGGER.trace(e::getMessage, e);
                    // Keep interrupting this thread.
                    Thread.currentThread().interrupt();
                } catch (final CompleteException e) {
                    LOGGER.debug(() -> "Complete");
                    LOGGER.trace(e::getMessage, e);
                }

                if (!transferState.isTerminated() && uncommittedCount > 0) {
                    LOGGER.debug(() -> "Final commit");
                    batchingWriteTxn.commit();
                }

                // Create final payloads and ensure they are all delivered before we complete.
                if (!transferState.isTerminated() && producePayloads) {
                    LOGGER.debug(() -> "Producing final payloads");
                    // Create payload and clear the DB.
                    boolean finalPayload = false;
                    while (!finalPayload) {
                        finalPayload = payloadCreator.addPayload(batchingWriteTxn, dbi, true);
                    }
                    // Make sure we end with an empty payload to indicate completion.
                    // Adding a final empty payload to the queue ensures that a consuming node will have to request the
                    // payload from the queue before we complete.
                    LOGGER.debug(() -> "Final payload");
                    payloadCreator.finalPayload();
                }

            } catch (final RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
                errorConsumer.add(e);
            } finally {
                // Ensure we complete.
                complete.countDown();
                LOGGER.debug("Finished transfer while loop");
                transferState.setThread(null);
            }
        });
    }

    private void insert(final BatchingWriteTxn batchingWriteTxn,
                        final Dbi<ByteBuffer> dbi,
                        final LmdbKV queueItem) {
        SearchProgressLog.increment(queryKey, SearchPhase.LMDB_DATA_STORE_INSERT);
        Metrics.measure("Insert", () -> {
            try {
                LOGGER.trace(() -> "insert");

                final LmdbKey rowKey = queueItem.getRowKey();
                final LmdbValue rowValue = queueItem.getRowValue();

                // Just try to put first.
                final boolean success = put(
                        batchingWriteTxn,
                        dbi,
                        rowKey.getByteBuffer(),
                        rowValue.getByteBuffer(),
                        PutFlags.MDB_NOOVERWRITE);
                if (success) {
                    resultCount.incrementAndGet();

                } else if (rowKey.isGroup()) {
                    // Get the existing entry for this key.
                    final ByteBuffer existingValueBuffer = dbi.get(batchingWriteTxn.getTxn(), rowKey.getByteBuffer());

                    final int minValSize = Math.max(minValueSize, existingValueBuffer.remaining());
                    try (final UnsafeByteBufferOutput output =
                            new UnsafeByteBufferOutput(minValSize, maxValueSize)) {
                        boolean merged = false;

                        try (final UnsafeByteBufferInput input = new UnsafeByteBufferInput(existingValueBuffer)) {
                            while (!input.end()) {
                                final LmdbValue existingRowValue = LmdbValue.read(compiledFields, input);

                                // If this is the same value the update it and reinsert.
                                if (existingRowValue.getKey().equals(rowValue.getKey())) {
                                    final Generator[] generators = existingRowValue.getGenerators().getGenerators();
                                    final Generator[] newValue = rowValue.getGenerators().getGenerators();
                                    final Generator[] combined = combine(generators, newValue);

                                    LOGGER.trace("Merging combined value to output");
                                    final LmdbValue combinedValue = new LmdbValue(
                                            existingRowValue.getKey().getBytes(),
                                            new Generators(compiledFields, combined));
                                    combinedValue.write(output);

                                    // Copy any remaining values.
                                    if (!input.end()) {
                                        final byte[] remainingBytes = input.readAllBytes();
                                        output.writeBytes(remainingBytes, 0, remainingBytes.length);
                                    }

                                    merged = true;

                                } else {
                                    LOGGER.debug("Copying value to output");
                                    existingRowValue.write(output);
                                }
                            }
                        }

                        // Append if we didn't merge.
                        if (!merged) {
                            LOGGER.debug("Appending value to output");
                            rowValue.write(output);
                            resultCount.incrementAndGet();
                        }

                        final ByteBuffer newValue = output.getByteBuffer().flip();
                        final boolean ok = put(batchingWriteTxn, dbi, rowKey.getByteBuffer(), newValue);
                        if (!ok) {
                            LOGGER.debug("Unable to update");
                            throw new RuntimeException("Unable to update");
                        }
                    }

                } else {
                    // We do not expect a key collision here.
                    LOGGER.debug("Unexpected collision");
                    throw new RuntimeException("Unexpected collision");
                }

            } catch (final RuntimeException | IOException e) {
                LOGGER.debug("Error putting " + queueItem + " (" + e.getMessage() + ")", e);
                errorConsumer.add(new RuntimeException("Error putting " + queueItem + " (" + e.getMessage() + ")", e));

                // Treat all errors as fatal so complete.
                completionState.signalComplete();
            }
        });
    }

    private boolean put(final BatchingWriteTxn batchingWriteTxn,
                        final Dbi<ByteBuffer> dbi,
                        final ByteBuffer key,
                        final ByteBuffer val,
                        final PutFlags... flags) {
        try {
            SearchProgressLog.increment(queryKey, SearchPhase.LMDB_DATA_STORE_DBI_PUT);
            final boolean didPutSucceed = dbi.put(batchingWriteTxn.getTxn(), key, val, flags);
            if (didPutSucceed) {
                batchingWriteTxn.commitIfRequired();
            }
            return didPutSucceed;
        } catch (final Exception e) {
            LOGGER.debug(e.getMessage(), e);
            errorConsumer.add(e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private Generator[] combine(final Generator[] existing, final Generator[] value) {
        SearchProgressLog.increment(queryKey, SearchPhase.LMDB_DATA_STORE_COMBINE);
        return Metrics.measure("Combine", () -> {
            // Combine the new item into the original item.
            for (int i = 0; i < existing.length; i++) {
                Generator existingGenerator = existing[i];
                Generator newGenerator = value[i];
                if (newGenerator != null) {
                    if (existingGenerator == null) {
                        existing[i] = newGenerator;
                    } else {
                        existingGenerator.merge(newGenerator);
                    }
                }
            }

            return existing;
        });
    }

    /**
     * Get data from the store
     * Synchronised with clear to prevent a shutdown happening while reads are going on.
     *
     * @param consumer Consumer for the data.
     */
    @Override
    public synchronized void getData(final Consumer<Data> consumer) {
        SearchProgressLog.increment(queryKey, SearchPhase.LMDB_DATA_STORE_GET);
        LOGGER.trace("getData()");

        if (lmdbEnv.isClosed()) {
            // If we query LMDB after the env has been closed then we are likely to crash the JVM
            // see https://github.com/lmdbjava/lmdbjava/issues/185
            throw new RuntimeException(LogUtil.message(
                    "getData() called (queryKey ={}, componentId={}) after store has been shut down",
                    queryKey, componentId));
        }

        lmdbEnv.doWithReadTxn(readTxn ->
                Metrics.measure("getData", () ->
                        consumer.accept(new LmdbData(
                                dbi,
                                readTxn,
                                compiledFields,
                                compiledSorters,
                                maxResults,
                                queryKey))));
    }

    private static class LmdbData implements Data {

        private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(LmdbData.class);

        private final Dbi<ByteBuffer> dbi;
        private final Txn<ByteBuffer> readTxn;
        private final CompiledField[] compiledFields;
        private final CompiledSorter<HasGenerators>[] compiledSorters;
        private final Sizes maxResults;
        private final QueryKey queryKey;

        public LmdbData(final Dbi<ByteBuffer> dbi,
                        final Txn<ByteBuffer> readTxn,
                        final CompiledField[] compiledFields,
                        final CompiledSorter<HasGenerators>[] compiledSorters,
                        final Sizes maxResults,
                        final QueryKey queryKey) {
            this.dbi = dbi;
            this.readTxn = readTxn;
            this.compiledFields = compiledFields;
            this.compiledSorters = compiledSorters;
            this.maxResults = maxResults;
            this.queryKey = queryKey;
        }

        /**
         * Get root items from the data store.
         *
         * @return Root items.
         */
        @Override
        public Items get() {
            LOGGER.trace("get() called");
            return get(Key.root());
        }

        /**
         * Get child items from the data store for the provided parent key.
         * Synchronised with clear to prevent a shutdown happening while reads are going on.
         *
         * @param parentKey The parent key to get child items for.
         * @return The child items for the parent key.
         */
        @Override
        public Items get(final Key parentKey) {
            SearchProgressLog.increment(queryKey, SearchPhase.LMDB_DATA_STORE_GET);
            LOGGER.trace("get() called for parentKey: {}", parentKey);

            return Metrics.measure("get", () -> {
                final int depth = parentKey.size();
                final int trimmedSize = maxResults.size(depth);

                final ItemArrayList list = getChildren(parentKey, depth, trimmedSize, true, false);

                return new Items() {
                    @Override
                    @Nonnull
                    public Iterator<Item> iterator() {
                        return new Iterator<>() {
                            private int pos = 0;

                            @Override
                            public boolean hasNext() {
                                return list.size > pos;
                            }

                            @Override
                            public Item next() {
                                return list.array[pos++];
                            }
                        };
                    }

                    @Override
                    public int size() {
                        return list.size();
                    }
                };
            });
        }

        private ItemArrayList getChildren(final Key parentKey,
                                          final int depth,
                                          final int trimmedSize,
                                          final boolean allowSort,
                                          final boolean trimTop) {
            SearchProgressLog.increment(queryKey, SearchPhase.LMDB_DATA_STORE_GET_CHILDREN);
            // If we don't have any children at the requested depth then return an empty list.
            if (compiledSorters.length <= depth) {
                return ItemArrayList.EMPTY;
            }

            final ItemArrayList list = new ItemArrayList(10);

            final ByteBuffer start = LmdbKey.createKeyStem(depth, parentKey);
            final KeyRange<ByteBuffer> keyRange = KeyRange.atLeast(start);

            final int maxSize;
            if (trimmedSize < Integer.MAX_VALUE / 2) {
                maxSize = Math.max(1000, trimmedSize * 2);
            } else {
                maxSize = Integer.MAX_VALUE;
            }
            final CompiledSorter<HasGenerators> sorter = compiledSorters[depth];

            final AtomicBoolean trimmed = new AtomicBoolean(true);
            final AtomicBoolean inRange = new AtomicBoolean(true);

            try (final CursorIterable<ByteBuffer> cursorIterable = dbi.iterate(readTxn, keyRange)) {
                final Iterator<KeyVal<ByteBuffer>> iterator = cursorIterable.iterator();

                while (iterator.hasNext()
                        && inRange.get()
                        && !Thread.currentThread().isInterrupted()) {

                    final KeyVal<ByteBuffer> keyVal = iterator.next();

                    // Make sure the first part of the row key matches the start key we are looking for.
                    boolean match = true;
                    for (int i = 0; i < start.remaining() && match; i++) {
                        if (start.get(i) != keyVal.key().get(i)) {
                            match = false;
                        }
                    }

                    if (match) {
                        final ByteBuffer valueBuffer = keyVal.val();
                        try (final UnsafeByteBufferInput input = new UnsafeByteBufferInput(valueBuffer)) {
                            while (!input.end() && inRange.get()) {
                                final LmdbValue rowValue = LmdbValue.read(compiledFields, input);
                                final Key key = rowValue.getKey();
                                if (key.getParent().equals(parentKey)) {
                                    final Generator[] generators = rowValue.getGenerators().getGenerators();
                                    list.add(new ItemImpl(this, key, generators));
                                    if (!allowSort && list.size >= trimmedSize) {
                                        // Stop without sorting etc.
                                        inRange.set(false);

                                    } else {
                                        trimmed.set(false);
                                        if (list.size() > maxSize) {
                                            list.sortAndTrim(sorter, trimmedSize, trimTop);
                                            trimmed.set(true);
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        inRange.set(false);
                    }
                }
            }

            if (!trimmed.get()) {
                list.sortAndTrim(sorter, trimmedSize, trimTop);
            }

            return list;
        }
    }

    /**
     * Clear the data store.
     * Synchronised with get() to prevent a shutdown happening while reads are going on.
     */
    @Override
    public synchronized void clear() {
        LOGGER.debug("clear called");
        if (shutdown.compareAndSet(false, true)) {
            SearchProgressLog.increment(queryKey, SearchPhase.LMDB_DATA_STORE_CLEAR);

            // Let the transfer loop know it should stop ASAP.
            transferState.terminate();

            // Clear the queue.
            queue.clear();

            // If the transfer loop is waiting on new queue items ensure it loops once more.
            completionState.signalComplete();

            // Wait for transferring to stop.
            try {
                LOGGER.debug("Waiting for transfer to stop");
                completionState.awaitCompletion();
            } catch (final InterruptedException e) {
                LOGGER.trace(e.getMessage(), e);
                // Keep interrupting this thread.
                Thread.currentThread().interrupt();
            }

            try {
                try {
                    dbi.close();
                } catch (final RuntimeException e) {
                    LOGGER.error(e.getMessage(), e);
                    errorConsumer.add(e);
                }

                try {
                    lmdbEnv.close();
                } catch (final RuntimeException e) {
                    LOGGER.error(e.getMessage(), e);
                    errorConsumer.add(e);
                }

                try {
                    lmdbEnv.delete();
                } catch (final RuntimeException e) {
                    LOGGER.error(e.getMessage(), e);
                    errorConsumer.add(e);
                }
            } finally {
                resultCount.set(0);
                totalResultCount.set(0);
            }
        }
    }

    /**
     * Get the completion state associated with receiving all search results and having added them to the store
     * successfully.
     *
     * @return The search completion state for the data store.
     */
    @Override
    public CompletionState getCompletionState() {
        return completionState;
    }

    /**
     * Read items from the supplied input and transfer them to the data store.
     *
     * @param input The input to read.
     */
    @Override
    public void readPayload(final Input input) {
        // Return false if we aren't happy to accept any more data.
        payloadCreator.readPayload(input);
    }

    /**
     * Write data from the data store to an output removing them from the datastore as we go as they will be transferred
     * to another store.
     *
     * @param output The output to write to.
     */
    @Override
    public void writePayload(final Output output) {
        if (!producePayloads) {
            throw new RuntimeException("Not producing payloads");
        }
        payloadCreator.writePayload(output);
    }

    private static class ItemArrayList {

        private static final ItemArrayList EMPTY = new ItemArrayList(0);

        private final int minArraySize;
        private ItemImpl[] array;
        private int size;

        public ItemArrayList(final int minArraySize) {
            this.minArraySize = minArraySize;
            array = new ItemImpl[minArraySize];
        }

        void sortAndTrim(final CompiledSorter<HasGenerators> sorter,
                         final int trimmedSize,
                         final boolean trimTop) {
            if (sorter != null && size > 0) {
                Arrays.sort(array, 0, size, sorter);
            }
            if (size > trimmedSize) {
                final int len = Math.max(minArraySize, trimmedSize);
                final ItemImpl[] newArray = new ItemImpl[len];
                if (trimTop) {
                    System.arraycopy(array, size - trimmedSize, newArray, 0, trimmedSize);
                } else {
                    System.arraycopy(array, 0, newArray, 0, trimmedSize);
                }
                array = newArray;
                size = trimmedSize;
            }
        }

        void add(final ItemImpl item) {
            if (array.length <= size) {
                final ItemImpl[] newArray = new ItemImpl[size * 2];
                System.arraycopy(array, 0, newArray, 0, array.length);
                array = newArray;
            }
            array[size++] = item;
        }

        ItemImpl get(final int index) {
            return array[index];
        }

        int size() {
            return size;
        }
    }

    public static class ItemImpl implements Item, HasGenerators {

        private final LmdbData data;
        private final Key key;
        private final Generator[] generators;

        public ItemImpl(final LmdbData data,
                        final Key key,
                        final Generator[] generators) {
            this.data = data;
            this.key = key;
            this.generators = generators;
        }

        @Override
        public Key getKey() {
            return key;
        }

        @Override
        public Val getValue(final int index) {
            Val val = null;

            final Generator generator = generators[index];
            if (generator instanceof Selector) {
                if (key.isGrouped()) {
                    int maxRows = 1;
                    boolean sort = true;
                    boolean trimTop = false;

                    if (generator instanceof AnySelector) {
                        sort = false;
//                    } else if (generator instanceof FirstSelector) {
                    } else if (generator instanceof LastSelector) {
                        trimTop = true;
                    } else if (generator instanceof TopSelector) {
                        maxRows = ((TopSelector) generator).getLimit();
                    } else if (generator instanceof BottomSelector) {
                        maxRows = ((BottomSelector) generator).getLimit();
                        trimTop = true;
                    } else if (generator instanceof NthSelector) {
                        maxRows = ((NthSelector) generator).getPos() + 1;
                    }

                    final ItemArrayList items = data.getChildren(
                            key,
                            key.size(),
                            maxRows,
                            sort,
                            trimTop);

                    final Selector selector = (Selector) generator;
                    val = selector.select(new Selection<>() {
                        @Override
                        public int size() {
                            return items.size;
                        }

                        @Override
                        public Val get(final int pos) {
                            if (pos < items.size) {
                                return items.get(pos).generators[index].eval();
                            }
                            return ValNull.INSTANCE;
                        }
                    });

                } else {
                    val = generator.eval();
                }
            } else if (generator != null) {
                val = generator.eval();
            }

            return val;
        }

        @Override
        public Generator[] getGenerators() {
            return generators;
        }
    }

    private static class CompletionStateImpl implements CompletionState {

        private final LmdbDataStore lmdbDataStore;
        private final CountDownLatch complete;

        public CompletionStateImpl(final LmdbDataStore lmdbDataStore,
                                   final CountDownLatch complete) {
            this.lmdbDataStore = lmdbDataStore;
            this.complete = complete;
        }

        @Override
        public void signalComplete() {
            if (!isComplete()) {
                // Add an empty item to the transfer queue.
                lmdbDataStore.queue.complete();
            }
        }

        @Override
        public boolean isComplete() {
            boolean complete = true;

            try {
                complete = this.complete.await(0, TimeUnit.MILLISECONDS);
            } catch (final InterruptedException e) {
                LOGGER.trace(e.getMessage(), e);
                // Keep interrupting this thread.
                Thread.currentThread().interrupt();
            }
            return complete;
        }

        @Override
        public void awaitCompletion() throws InterruptedException {
            complete.await();
        }

        @Override
        public boolean awaitCompletion(final long timeout, final TimeUnit unit) throws InterruptedException {
            return complete.await(timeout, unit);
        }
    }

    private static class TransferState {

        private final AtomicBoolean terminated = new AtomicBoolean();
        private volatile Thread thread;

        public boolean isTerminated() {
            return terminated.get();
        }

        public synchronized void terminate() {
            terminated.set(true);
            if (thread != null) {
                thread.interrupt();
            }
        }

        public synchronized void setThread(final Thread thread) {
            this.thread = thread;
            if (terminated.get()) {
                if (thread != null) {
                    thread.interrupt();
                } else if (Thread.interrupted()) {
                    LOGGER.debug("Cleared interrupt state");
                }
            }
        }
    }
}
