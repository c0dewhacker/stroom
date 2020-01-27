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

package stroom.job.shared;

import stroom.job.shared.JobNode.JobType;

public class GetScheduledTimesRequest {
    private JobType jobType;
    private Long scheduleReferenceTime;
    private Long lastExecutedTime;
    private String schedule;

    public GetScheduledTimesRequest() {
        // Default constructor necessary for GWT serialisation.
    }

    public GetScheduledTimesRequest(final JobType jobType,
                                    final Long scheduleReferenceTime,
                                    final Long lastExecutedTime,
                                    final String schedule) {
        this.jobType = jobType;
        this.scheduleReferenceTime = scheduleReferenceTime;
        this.lastExecutedTime = lastExecutedTime;
        this.schedule = schedule;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(final JobType jobType) {
        this.jobType = jobType;
    }

    public Long getScheduleReferenceTime() {
        return scheduleReferenceTime;
    }

    public void setScheduleReferenceTime(final Long scheduleReferenceTime) {
        this.scheduleReferenceTime = scheduleReferenceTime;
    }

    public Long getLastExecutedTime() {
        return lastExecutedTime;
    }

    public void setLastExecutedTime(final Long lastExecutedTime) {
        this.lastExecutedTime = lastExecutedTime;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(final String schedule) {
        this.schedule = schedule;
    }
}