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
 */

package stroom.index.impl;

import stroom.docstore.api.DocumentActionHandlerBinder;
import stroom.explorer.api.ExplorerActionHandler;
import stroom.importexport.api.ImportExportActionHandler;
import stroom.index.shared.IndexDoc;
import stroom.job.api.ScheduledJobsBinder;
import stroom.lifecycle.api.LifecycleBinder;
import stroom.searchable.api.Searchable;
import stroom.util.RunnableWrapper;
import stroom.util.entityevent.EntityEvent;
import stroom.util.guice.GuiceUtil;
import stroom.util.guice.HasSystemInfoBinder;
import stroom.util.guice.RestResourcesBinder;
import stroom.util.shared.Clearable;

import com.google.inject.AbstractModule;
import jakarta.inject.Inject;

import static stroom.job.api.Schedule.ScheduleType.CRON;
import static stroom.job.api.Schedule.ScheduleType.PERIODIC;

public class IndexModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new IndexElementModule());

        bind(IndexShardWriterCache.class).to(IndexShardWriterCacheImpl.class);
        bind(IndexStructureCache.class).to(IndexStructureCacheImpl.class);
        bind(IndexStore.class).to(IndexStoreImpl.class);
        bind(IndexVolumeService.class).to(IndexVolumeServiceImpl.class);
        bind(IndexVolumeGroupService.class).to(IndexVolumeGroupServiceImpl.class);
        bind(IndexShardService.class).to(IndexShardServiceImpl.class);
        bind(Indexer.class).to(IndexerImpl.class);

        GuiceUtil.buildMultiBinder(binder(), Clearable.class)
                .addBinding(IndexStructureCacheImpl.class)
                .addBinding(IndexVolumeServiceImpl.class);

        GuiceUtil.buildMultiBinder(binder(), EntityEvent.Handler.class)
                .addBinding(IndexConfigCacheEntityEventHandler.class);

        GuiceUtil.buildMultiBinder(binder(), ExplorerActionHandler.class)
                .addBinding(IndexStoreImpl.class);

        GuiceUtil.buildMultiBinder(binder(), ImportExportActionHandler.class)
                .addBinding(IndexStoreImpl.class);

        GuiceUtil.buildMultiBinder(binder(), Searchable.class)
                .addBinding(IndexShardServiceImpl.class);

        RestResourcesBinder.create(binder())
                .bind(IndexResourceImpl.class)
                .bind(IndexVolumeGroupResourceImpl.class)
                .bind(IndexVolumeResourceImpl.class);

        DocumentActionHandlerBinder.create(binder())
                .bind(IndexDoc.DOCUMENT_TYPE, IndexStoreImpl.class);

        GuiceUtil.buildMultiBinder(binder(), EntityEvent.Handler.class)
                .addBinding(IndexVolumeServiceImpl.class);

        ScheduledJobsBinder.create(binder())
                .bindJobTo(IndexShardDelete.class, builder -> builder
                        .name("Index Shard Delete")
                        .description("Job to delete index shards from disk that have been marked as deleted")
                        .schedule(CRON, "0 0 *"))
                .bindJobTo(IndexShardRetention.class, builder -> builder
                        .name("Index Shard Retention")
                        .description("Job to set index shards to have a status of deleted that have past their " +
                                "retention period")
                        .schedule(PERIODIC, "10m"))
                .bindJobTo(IndexWriterCacheSweep.class, builder -> builder
                        .name("Index Writer Cache Sweep")
                        .description("Job to remove old index shard writers from the cache")
                        .schedule(PERIODIC, "10m"))
                .bindJobTo(IndexWriterFlush.class, builder -> builder
                        .name("Index Writer Flush")
                        .description("Job to flush index shard data to disk")
                        .schedule(PERIODIC, "10m"))
                .bindJobTo(VolumeStatus.class, builder -> builder
                        .name("Index Volume Status")
                        .description("Update the usage status of volumes owned by the node")
                        .schedule(PERIODIC, "5m"));

        LifecycleBinder.create(binder())
                .bindStartupTaskTo(IndexShardWriterCacheStartup.class)
                .bindShutdownTaskTo(IndexShardWriterCacheShutdown.class);

        HasSystemInfoBinder.create(binder())
                .bind(IndexVolumeServiceImpl.class);
        HasSystemInfoBinder.create(binder()).bind(IndexSystemInfo.class);
    }

    private static class IndexShardDelete extends RunnableWrapper {

        @Inject
        IndexShardDelete(final IndexShardManager indexShardManager) {
            super(indexShardManager::deleteFromDisk);
        }
    }

    private static class IndexShardRetention extends RunnableWrapper {

        @Inject
        IndexShardRetention(final IndexShardManager indexShardManager) {
            super(indexShardManager::checkRetention);
        }
    }

    private static class IndexWriterCacheSweep extends RunnableWrapper {

        @Inject
        IndexWriterCacheSweep(final IndexShardWriterCache indexShardWriterCache) {
            super(indexShardWriterCache::sweep);
        }
    }

    private static class IndexWriterFlush extends RunnableWrapper {

        @Inject
        IndexWriterFlush(final IndexShardWriterCache indexShardWriterCache) {
            super(indexShardWriterCache::flushAll);
        }
    }

    private static class VolumeStatus extends RunnableWrapper {

        @Inject
        VolumeStatus(final IndexVolumeService volumeService) {
            super(volumeService::rescan);
        }
    }

    private static class IndexShardWriterCacheStartup extends RunnableWrapper {

        @Inject
        IndexShardWriterCacheStartup(final IndexShardWriterCacheImpl indexShardWriterCache) {
            super(indexShardWriterCache::startup);
        }
    }

    private static class IndexShardWriterCacheShutdown extends RunnableWrapper {

        @Inject
        IndexShardWriterCacheShutdown(final IndexShardWriterCacheImpl indexShardWriterCache) {
            super(indexShardWriterCache::shutdown);
        }
    }
}
