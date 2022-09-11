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

package org.apache.inlong.manager.service.core;

import com.github.pagehelper.PageInfo;
import org.apache.inlong.manager.common.pojo.workflow.EventLogQuery;
import org.apache.inlong.manager.common.pojo.workflow.EventLogView;
import org.apache.inlong.manager.workflow.event.process.ProcessEvent;
import org.apache.inlong.manager.workflow.event.task.TaskEvent;

/**
 * Workflow event related services
 */
public interface WorkflowEventService {

    /**
     * Get event log based on ID
     *
     * @param id ID of the event log
     * @return Event log view
     */
    EventLogView get(Integer id);

    /**
     * Query event logs based on conditions
     *
     * @param query Query conditions
     * @return Log list
     */
    PageInfo<EventLogView> list(EventLogQuery query);

    /**
     * Execute the listener based on the log ID
     *
     * @param eventLogId Log record ID
     */
    void executeEventListener(Integer eventLogId);

    /**
     * Re-execute the specified listener according to the process ID
     *
     * @param processId WorkflowProcess ID
     * @param listenerName Listener name
     */
    void executeProcessEventListener(Integer processId, String listenerName);

    /**
     * Re-execute the specified listener based on the task ID
     *
     * @param taskId WorkflowTask ID
     * @param listenerName Listener name
     */
    void executeTaskEventListener(Integer taskId, String listenerName);

    /**
     * Re-trigger the process event based on the process ID
     *
     * @param processId WorkflowProcess ID
     * @param processEvent WorkflowProcess event
     */
    void triggerProcessEvent(Integer processId, ProcessEvent processEvent);

    /**
     * Re-trigger task events based on task ID
     *
     * @param taskId WorkflowTask ID
     * @param taskEvent WorkflowTask event
     */
    void triggerTaskEvent(Integer taskId, TaskEvent taskEvent);

}
