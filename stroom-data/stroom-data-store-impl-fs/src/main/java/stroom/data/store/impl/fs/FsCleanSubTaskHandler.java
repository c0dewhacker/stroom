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

package stroom.data.store.impl.fs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.data.store.impl.DataStoreMaintenanceService;
import stroom.data.store.impl.ScanVolumePathResult;
import stroom.security.api.SecurityContext;
import stroom.task.api.ExecutorProvider;
import stroom.task.api.TaskContext;
import stroom.task.api.ThreadPoolImpl;
import stroom.task.shared.ThreadPool;
import stroom.util.shared.ModelStringUtil;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Task to clean the stream store.
 */

class FsCleanSubTaskHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(FsCleanSubTaskHandler.class);

    private final DataStoreMaintenanceService streamMaintenanceService;
    private final ExecutorProvider executorProvider;
    private final Provider<TaskContext> taskContextProvider;
    private final SecurityContext securityContext;
    private final DataStoreServiceConfig config;

    @Inject
    FsCleanSubTaskHandler(final DataStoreMaintenanceService streamMaintenanceService,
                          final ExecutorProvider executorProvider,
                          final Provider<TaskContext> taskContextProvider,
                          final SecurityContext securityContext,
                          final DataStoreServiceConfig config) {
        this.streamMaintenanceService = streamMaintenanceService;
        this.executorProvider = executorProvider;
        this.taskContextProvider = taskContextProvider;
        this.securityContext = securityContext;
        this.config = config;
    }

    public void exec(final FsCleanSubTask task, final Consumer<List<String>> deleteListConsumer) {
        securityContext.secure(() -> {
            final TaskContext taskContext = taskContextProvider.get();
            final ThreadPool threadPool = new ThreadPoolImpl("File System Clean#", 1, 1, config.getFileSystemCleanBatchSize(), Integer.MAX_VALUE);
            final Executor executor = executorProvider.get(threadPool);

            taskContext.setName("File system clean");
            taskContext.info(() -> "Cleaning: " + task.getVolume().getPath() + " - " + task.getPath());

            if (Thread.currentThread().isInterrupted()) {
                LOGGER.info("exec() - Been asked to Quit");

            } else {
                final ScanVolumePathResult result = streamMaintenanceService.scanVolumePath(
                        task.getVolume(),
                        task.isDelete(),
                        task.getPath(),
                        task.getOldAge());

                task.getTaskProgress().addResult(result);

                // Write a list of all deleted items.
                if (deleteListConsumer != null && result.getDeleteList() != null) {
                    deleteListConsumer.accept(result.getDeleteList());
                }

                // Add a log line to indicate progress 1/3,44/100
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("createRunnableForPath() -" + task.getLogPrefix() + "  - " + task.getPath() + ".  Scanned "
                            + ModelStringUtil.formatCsv(result.getFileCount()) + " files, deleted "
                            + ModelStringUtil.formatCsv(result.getDeleteList().size()) + ", too new to delete "
                            + ModelStringUtil.formatCsv(result.getTooNewToDeleteCount()) + ".  Totals "
                            + task.getTaskProgress().traceInfo());
                }

                if (Thread.currentThread().isInterrupted()) {
                    LOGGER.info("exec() - Been asked to Quit");

                } else {
                    if (result.getChildDirectoryList() != null && result.getChildDirectoryList().size() > 0) {
                        // Add to the task steps remaining.
                        task.getTaskProgress().addScanPending(result.getChildDirectoryList().size());

                        final CountDownLatch countDownLatch = new CountDownLatch(result.getChildDirectoryList().size());
                        for (final String subPath : result.getChildDirectoryList()) {
                            final FsCleanSubTask subTask = new FsCleanSubTask(task.getTaskProgress(), task.getVolume(), subPath, task.getLogPrefix(), task.getOldAge(), task.isDelete());
                            Runnable runnable = () -> exec(subTask, deleteListConsumer);
                            runnable = taskContext.sub(runnable);
                            CompletableFuture
                                    .runAsync(runnable, executor)
                                    .whenComplete((r, t) -> {
                                        countDownLatch.countDown();
                                        task.getTaskProgress().addScanComplete();
                                    });
                        }

                        try {
                            if (task.getPath().length() == 0) {
                                while (!countDownLatch.await(1, TimeUnit.SECONDS)) {
                                    final FsCleanProgress taskProgress = task.getTaskProgress();

                                    taskContext.info(() -> task.getVolume().getPath() +
                                            " (Scan Dir/File " +
                                            taskProgress.getScanDirCount() +
                                            "/" +
                                            taskProgress.getScanFileCount() +
                                            ", Del " +
                                            taskProgress.getScanDeleteCount() +
                                            ") ");
                                }
                            } else {
                                countDownLatch.await();
                            }
                        } catch (final InterruptedException e) {
                            // Continue to interrupt.
                            Thread.currentThread().interrupt();
                            LOGGER.debug(e.getMessage(), e);
                        }
                    }
                }
            }
        });
    }
}
