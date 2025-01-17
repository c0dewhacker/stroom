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
 */

package stroom.job.impl;

import stroom.event.logging.rs.api.AutoLogged;
import stroom.job.shared.GetScheduledTimesRequest;
import stroom.job.shared.ScheduledTimeResource;
import stroom.job.shared.ScheduledTimes;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

@AutoLogged
public class ScheduledTimeResourceImpl implements ScheduledTimeResource {

    private final Provider<ScheduleService> scheduleServiceProvider;

    @Inject
    ScheduledTimeResourceImpl(final Provider<ScheduleService> scheduleServiceProvider) {
        this.scheduleServiceProvider = scheduleServiceProvider;
    }

    @Override
    public ScheduledTimes get(final GetScheduledTimesRequest request) {
        return scheduleServiceProvider.get().getScheduledTimes(
                request.getJobType(),
                request.getScheduleReferenceTime(),
                request.getLastExecutedTime(),
                request.getSchedule());
    }
}
