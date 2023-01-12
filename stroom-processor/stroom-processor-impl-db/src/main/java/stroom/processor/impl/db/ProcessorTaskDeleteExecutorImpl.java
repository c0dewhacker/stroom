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

package stroom.processor.impl.db;

import stroom.cluster.lock.api.ClusterLockService;
import stroom.processor.impl.ProcessorConfig;
import stroom.processor.impl.ProcessorDao;
import stroom.processor.impl.ProcessorFilterDao;
import stroom.processor.impl.ProcessorTaskDao;
import stroom.processor.impl.ProcessorTaskDeleteExecutor;
import stroom.processor.impl.ProcessorTaskManager;
import stroom.task.api.TaskContext;
import stroom.util.date.DateUtil;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.logging.LogExecutionTime;
import stroom.util.time.StroomDuration;
import stroom.util.time.TimeUtils;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;

class ProcessorTaskDeleteExecutorImpl implements ProcessorTaskDeleteExecutor {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(ProcessorTaskDeleteExecutorImpl.class);

    private static final String TASK_NAME = "Processor Task Delete Executor";
    private static final String LOCK_NAME = "ProcessorTaskDeleteExecutor";

    private final ClusterLockService clusterLockService;
    private final ProcessorConfig processorConfig;
    private final ProcessorDao processorDao;
    private final ProcessorFilterDao processorFilterDao;
    private final ProcessorTaskDao processorTaskDao;
    private final ProcessorTaskManager processorTaskManager;
    private final TaskContext taskContext;

    @Inject
    ProcessorTaskDeleteExecutorImpl(final ClusterLockService clusterLockService,
                                    final ProcessorConfig processorConfig,
                                    final ProcessorDao processorDao,
                                    final ProcessorFilterDao processorFilterDao,
                                    final ProcessorTaskDao processorTaskDao,
                                    final ProcessorTaskManager processorTaskManager,
                                    final TaskContext taskContext) {
        this.clusterLockService = clusterLockService;
        this.processorConfig = processorConfig;
        this.processorDao = processorDao;
        this.processorFilterDao = processorFilterDao;
        this.processorTaskDao = processorTaskDao;
        this.processorTaskManager = processorTaskManager;
        this.taskContext = taskContext;
    }

    public void exec() {
        final AtomicLong nextDeleteMs = processorTaskManager.getNextDeleteMs();
        try {
            if (nextDeleteMs.get() == 0) {
                LOGGER.debug("deleteSchedule() - no schedule set .... maybe we aren't in charge of creating tasks");
            } else {
                LOGGER.debug("deleteSchedule() - nextDeleteMs={}",
                        DateUtil.createNormalDateTimeString(nextDeleteMs.get()));
                // Have we gone past our next delete schedule?
                if (nextDeleteMs.get() < System.currentTimeMillis()) {
                    taskContext.info(() -> "Deleting old processor tasks");
                    lockAndDelete();
                }
            }
        } catch (final RuntimeException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    final void lockAndDelete() {
        LOGGER.info(TASK_NAME + " - start");
        clusterLockService.tryLock(LOCK_NAME, () -> {
            try {
                if (!Thread.currentThread().isInterrupted()) {
                    final LogExecutionTime logExecutionTime = new LogExecutionTime();

                    final StroomDuration deleteAge = processorConfig.getDeleteAge();

                    if (!deleteAge.isZero()) {
                        final Instant deleteThreshold = TimeUtils.durationToThreshold(deleteAge);
                        LOGGER.info(TASK_NAME + " - using deleteAge: {}, deleteThreshold: {}",
                                deleteAge, deleteThreshold);
                        // Delete qualifying records prior to this date.
                        delete(deleteThreshold);
                    }
                    LOGGER.info(TASK_NAME + " - finished in {}", logExecutionTime);
                }
            } catch (final RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
            }
        });
    }

    @Override
    public void delete(final Instant deleteThreshold) {
        processorTaskDao.physicallyDeleteOldTasks(deleteThreshold);
        deleteOldFilters(deleteThreshold);
        deleteDeletedTasksAndProcessors(deleteThreshold);
    }

    private void deleteDeletedTasksAndProcessors(final Instant deleteThreshold) {
        // Delete old filters.
        processorFilterDao.physicalDeleteOldProcessorFilters(deleteThreshold);

        // Delete old processors.
        processorDao.physicalDeleteOldProcessors(deleteThreshold);
//
//        // Get deleted processors.
//        final List<Integer> deletedProcessorIds =
//                JooqUtil.contextResult(processorDbConnProvider, context ->
//                                context
//                                        .select(PROCESSOR.ID)
//                                        .from(PROCESSOR)
//                                        .where(PROCESSOR.DELETED.eq(true))
//                                        .and(PROCESSOR.UPDATE_TIME_MS.lessThan(deleteThreshold.toEpochMilli()))
//                                        .fetch())
//                        .map(Record1::value1);
//        LOGGER.debug(() -> LogUtil.message("Found {} 'DELETED' processors", deletedProcessorIds.size()));
//
//        final List<Integer> deletedProcFilterIds =
//                JooqUtil.contextResult(processorDbConnProvider, context ->
//                        context
//                                .select(PROCESSOR_FILTER.ID)
//                                .from(PROCESSOR_FILTER)
//                                .where(PROCESSOR_FILTER.DELETED.eq(true))
//                                .or(PROCESSOR_FILTER.FK_PROCESSOR_ID.in(deletedProcessorIds))
//                                .and(PROCESSOR_FILTER.UPDATE_TIME_MS.lessThan(deleteThreshold.toEpochMilli()))
//                                .fetch())
//                        .map(Record1::value1);
//        LOGGER.debug(() ->
//                LogUtil.message("Found {} processor filters that are 'DELETED' or whose processor is 'DELETED'",
//                        deletedProcFilterIds.size()));
//
//        // Delete tasks.
//        LOGGER.debug(() -> LogUtil.message("Deleting processor tasks that are 'DELETED' or are link to one of {} " +
//                "'DELETED' filter IDs", deletedProcFilterIds.size()));
//        final Integer taskDeleteCount = JooqUtil.contextResult(processorDbConnProvider, context ->
//                context
//                        .deleteFrom(PROCESSOR_TASK)
//                        .where(PROCESSOR_TASK.STATUS.eq(TaskStatus.DELETED.getPrimitiveValue()))
//                        .or(PROCESSOR_TASK.FK_PROCESSOR_FILTER_ID.in(deletedProcFilterIds))
//                        .and(PROCESSOR_TASK.STATUS_TIME_MS.lessThan(deleteThreshold.toEpochMilli()))
//                        .execute());
//        LOGGER.debug("Deleted {} processor tasks", taskDeleteCount);
//
//        // Delete filters one by one as there may still be some constraint failures.
//        for (final Integer processorFilterId : deletedProcFilterIds) {
//            try {
//                processorFilterDao.physicalDeleteByProcessorFilterId(processorFilterId);
//            } catch (final RuntimeException e) {
//                LOGGER.debug(e.getMessage(), e);
//            }
//        }
//
//        // Delete processors one by one as there may still be some constraint failures.
//        for (final int processorId : deletedProcessorIds) {
//            try {
//                LOGGER.debug("Deleting processor with id {}", processorId);
//                final Integer processorDeleteCount = JooqUtil.contextResult(processorDbConnProvider, context ->
//                        context
//                                .deleteFrom(PROCESSOR)
//                                .where(PROCESSOR.ID.eq(processorId))
//                                .execute());
//                LOGGER.debug("Deleted {} processors", processorDeleteCount);
//            } catch (final RuntimeException e) {
//                LOGGER.debug(e.getMessage(), e);
//            }
//        }
    }

    private void deleteOldFilters(final Instant deleteThreshold) {
        try {
            processorFilterDao.logicallyDeleteOldProcessorFilters(deleteThreshold);

//            // Get all filters that have not been polled for a while.
//            final ExpressionOperator expression = ExpressionOperator.builder()
//                    .addTerm(
//                            ProcessorFilterFields.LAST_POLL_MS,
//                            ExpressionTerm.Condition.LESS_THAN,
//                            deleteThreshold.toEpochMilli())
//                    .build();
//            final ExpressionCriteria criteria = new ExpressionCriteria(expression);
////            criteria.setLastPollPeriod(new Period(null, age));
//            final List<ProcessorFilter> filters = processorFilterDao.find(criteria).getValues();
//            for (final ProcessorFilter filter : filters) {
//                final ProcessorFilterTracker tracker = filter.getProcessorFilterTracker();
//
//                if (tracker != null &&
//                        (ProcessorFilterTracker.COMPLETE.equals(tracker.getStatus()) ||
//                                ProcessorFilterTracker.ERROR.equals(tracker.getStatus()))) {
//                    // The tracker thinks that no more tasks will ever be
//                    // created for this filter so we can delete it if there are
//                    // no remaining tasks for this filter.
//                    //
//                    // The database constraint will not allow filters to be
//                    // deleted that still have associated tasks.
//                    try {
//                        LOGGER.debug("deleteCompleteOrFailedTasks() - Removing old complete filter {}", filter);
//                        processorFilterDao.logicalDeleteByProcessorFilterId(filter.getId());
//
//                    } catch (final RuntimeException e) {
//                        // The database constraint will not allow filters to be
//                        // deleted that still have associated tasks. This is
//                        // what we want to happen but output debug here to help
//                        // diagnose problems.
//                        LOGGER.debug("deleteCompleteOrFailedTasks() - Failed as tasks still remain for this filter - "
//                                + e.getMessage(), e);
//                    }
//                }
//            }
        } catch (final RuntimeException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
