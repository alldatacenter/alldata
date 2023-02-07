/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.workflow.core;

import org.apache.inlong.manager.common.enums.ProcessEvent;
import org.apache.inlong.manager.common.enums.TaskEvent;

/**
 * Event listener service
 */
public interface EventListenerService {

    /**
     * Execute the listener based on the log ID
     */
    void executeEventListener(Integer eventLogId);

    /**
     * Re-execute the specified listener according to the process ID
     */
    void executeProcessEventListener(Integer processId, String listenerName);

    /**
     * Re-execute the specified listener based on the task ID
     */
    void executeTaskEventListener(Integer taskId, String listenerName);

    /**
     * Re-trigger the process event based on the process ID
     */
    void triggerProcessEvent(Integer processId, ProcessEvent processEvent);

    /**
     * Re-trigger the task event based on the task ID
     */
    void triggerTaskEvent(Integer taskId, TaskEvent taskEvent);

}
