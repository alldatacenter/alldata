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

package org.apache.inlong.manager.client.api.inner.client;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.client.api.ClientConfiguration;
import org.apache.inlong.manager.client.api.service.WorkflowEventApi;
import org.apache.inlong.manager.client.api.util.ClientUtils;
import org.apache.inlong.manager.common.enums.ProcessEvent;
import org.apache.inlong.manager.common.enums.TaskEvent;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.workflow.EventLogRequest;
import org.apache.inlong.manager.pojo.workflow.EventLogResponse;

import java.util.List;
import java.util.Map;

/**
 * Client for {@link org.apache.inlong.manager.client.api.service.WorkflowEventApi}.
 */
@Slf4j
public class WorkflowEventClient {

    private final WorkflowEventApi workflowEventApi;

    public WorkflowEventClient(ClientConfiguration configuration) {
        workflowEventApi = ClientUtils.createRetrofit(configuration)
                .create(WorkflowEventApi.class);
    }

    /**
     * Get event list by paginating
     */
    public List<EventLogResponse> list(EventLogRequest request) {
        Map<String, Object> requestMap = JsonUtils.OBJECT_MAPPER.convertValue(request,
                new TypeReference<Map<String, Object>>() {
                });

        Response<PageResult<EventLogResponse>> response = ClientUtils.executeHttpCall(
                workflowEventApi.list(requestMap));
        ClientUtils.assertRespSuccess(response);

        return response.getData().getList();
    }

    /**
     * Execute the listener based on the event log ID
     */
    public void executeEventListener(Integer id) {
        Preconditions.checkNotNull(id, "event id cannot be null");

        Response<Object> response = ClientUtils.executeHttpCall(workflowEventApi.executeEventListener(id));
        ClientUtils.assertRespSuccess(response);
    }

    /**
     * Re-execute the specified listener based on the process ID
     */
    public void executeProcessEventListener(Integer processId, String listenerName) {
        Preconditions.checkNotNull(processId, "processId cannot be null");
        Preconditions.checkNotEmpty(listenerName, "listenerName cannot be null");

        Response<Object> response = ClientUtils.executeHttpCall(
                workflowEventApi.executeProcessEventListener(processId, listenerName));
        ClientUtils.assertRespSuccess(response);
    }

    /**
     * Re-execute the specified listener based on the task ID
     */
    public void executeTaskEventListener(Integer taskId, String listenerName) {
        Preconditions.checkNotNull(taskId, "taskId cannot be null");
        Preconditions.checkNotEmpty(listenerName, "listenerName cannot be null");

        Response<Object> response = ClientUtils.executeHttpCall(
                workflowEventApi.executeTaskEventListener(taskId, listenerName));
        ClientUtils.assertRespSuccess(response);
    }

    /**
     * Re-trigger the process event based on the process ID
     */
    public void triggerProcessEvent(Integer processId, ProcessEvent processEvent) {
        Preconditions.checkNotNull(processId, "processId cannot be null");
        Preconditions.checkNotNull(processEvent, "processEvent cannot be null");

        Response<Object> response = ClientUtils.executeHttpCall(
                workflowEventApi.triggerProcessEvent(processId, processEvent));
        ClientUtils.assertRespSuccess(response);
    }

    /**
     * Re-trigger the process event based on the task ID
     */
    public void triggerTaskEvent(Integer processId, TaskEvent taskEvent) {
        Preconditions.checkNotNull(processId, "processId cannot be null");
        Preconditions.checkNotNull(taskEvent, "taskEvent cannot be null");

        Response<Object> response = ClientUtils.executeHttpCall(
                workflowEventApi.triggerTaskEvent(processId, taskEvent));
        ClientUtils.assertRespSuccess(response);
    }
}
