package stroom.data.meta.impl.db;

import com.google.inject.Inject;
import com.google.inject.Provider;
import stroom.task.api.job.ScheduledJobsModule;

import static stroom.task.api.job.Schedule.ScheduleType.PERIODIC;

class DataMetaJobsModule extends ScheduledJobsModule {
    private Provider<MetaValueService> metaValueServiceProvider;

    @Inject
    DataMetaJobsModule(final Provider<MetaValueService> metaValueServiceProvider) {
        this.metaValueServiceProvider = metaValueServiceProvider;
    }

    @Override
    protected void configure() {
        super.configure();
        bindJob()
                .name("Flush DataMetaDb")
                .managed(false)
                .schedule(PERIODIC, "10s")
                .to(() -> (task) -> metaValueServiceProvider.get().flush());
        bindJob()
                .name("Data Attributes Retention")
                .description("Delete attributes older than system property stroom.meta.deleteAge")
                .schedule(PERIODIC, "1d")
                .to(() -> (task) -> metaValueServiceProvider.get().deleteOldValues());
    }
}
