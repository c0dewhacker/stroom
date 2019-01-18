package stroom.task.api.job;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.multibindings.MapBinder;
import stroom.task.shared.Task;

import java.util.Objects;
import java.util.function.Consumer;

public abstract class ScheduledJobsModule extends AbstractModule {
    private MapBinder<ScheduledJob, Consumer> mapBinder;

    @Override
    protected void configure() {
        super.configure();
        mapBinder = MapBinder.newMapBinder(binder(), ScheduledJob.class, Consumer.class);
    }

    public Builder bindJob() {
        return new Builder(mapBinder);
    }

    public static final class Builder {
        private final MapBinder<ScheduledJob, Consumer> mapBinder;

        // Mandatory
        private String name;
        private Schedule schedule;

        // Optional
        private boolean enabled = true;
        private boolean advanced = true;
        private boolean managed = true;
        private String description = "";

        Builder(final MapBinder<ScheduledJob, Consumer> mapBinder) {
            this.mapBinder = mapBinder;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder advanced(boolean advanced) {
            this.advanced = advanced;
            return this;
        }

        public Builder managed(boolean managed) {
            this.managed = managed;
            return this;
        }

        public Builder schedule(Schedule.ScheduleType scheduleType, String schedule) {
            this.schedule = new Schedule(scheduleType, schedule);
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public void to(final Provider<Consumer<Task>> provider) {
            Objects.requireNonNull(schedule);
            Objects.requireNonNull(name);
            final ScheduledJob scheduledJob = new ScheduledJob(schedule, name, description, enabled, advanced, managed);
            mapBinder.addBinding(scheduledJob).toProvider(provider);
        }
    }
}
