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

package stroom.processor.impl;

import stroom.processor.shared.ProcessorTaskList;

import java.time.Instant;

public interface ProcessorTaskManager {

    ProcessorTaskList assignTasks(String nodeName, int count);

    Boolean abandonTasks(ProcessorTaskList processorTaskList);

    void createAndQueueTasks();

    int getTaskQueueSize();

    void writeQueueStatistics();

    Instant getLastCreateTime();

    void startup();

    void shutdown();

    void setAllowAsyncTaskCreation(boolean allowAsyncFillTaskStore);

    void setAllowTaskCreation(boolean allowCreateTasks);
}
