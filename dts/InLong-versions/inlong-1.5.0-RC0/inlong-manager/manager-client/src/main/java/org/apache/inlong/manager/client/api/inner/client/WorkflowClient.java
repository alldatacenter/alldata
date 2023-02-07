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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.client.api.ClientConfiguration;
import org.apache.inlong.manager.client.api.service.WorkflowApi;
import org.apache.inlong.manager.client.api.util.ClientUtils;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.workflow.ProcessDetailResponse;
import org.apache.inlong.manager.pojo.workflow.ProcessRequest;
import org.apache.inlong.manager.pojo.workflow.ProcessResponse;
import org.apache.inlong.manager.pojo.workflow.TaskRequest;
import org.apache.inlong.manager.pojo.workflow.TaskResponse;
import org.apache.inlong.manager.pojo.workflow.WorkflowOperationRequest;
import org.apache.inlong.manager.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.pojo.workflow.form.process.ApplyGroupProcessForm;

import java.util.Map;

/**
 * Client for {@link WorkflowApi}.
 */
@Slf4j
public class WorkflowClient {

    private final WorkflowApi workflowApi;

    public WorkflowClient(ClientConfiguration configuration) {
        workflowApi = ClientUtils.createRetrofit(configuration).create(WorkflowApi.class);
    }

    /**
     * Approval the process
     *
     * @param taskId taskId
     * @param groupProcessForm inlong group process form
     * @return workflow result info
     */
    public WorkflowResult startInlongGroup(int taskId, ApplyGroupProcessForm groupProcessForm) {
        ObjectNode workflowTaskOperation = JsonUtils.OBJECT_MAPPER.createObjectNode();
        workflowTaskOperation.putPOJO("transferTo", Lists.newArrayList());
        workflowTaskOperation.put("remark", "approved by system");

        ObjectNode groupApproveForm = JsonUtils.OBJECT_MAPPER.createObjectNode();
        groupApproveForm.putPOJO("groupApproveInfo", groupProcessForm.getGroupInfo());
        groupApproveForm.putPOJO("streamApproveInfoList", groupProcessForm.getStreamInfoList());
        groupApproveForm.put("formName", "InlongGroupApproveForm");
        workflowTaskOperation.set("form", groupApproveForm);

        log.info("startInlongGroup workflowTaskOperation: {}", groupApproveForm);

        Map<String, Object> requestMap = JsonUtils.OBJECT_MAPPER.convertValue(workflowTaskOperation,
                new TypeReference<Map<String, Object>>() {
                });
        Response<WorkflowResult> response = ClientUtils.executeHttpCall(
                workflowApi.startInlongGroup(taskId, requestMap));
        ClientUtils.assertRespSuccess(response);

        return response.getData();
    }

    /**
     * Initiation process
     *
     * @param request workflow operation request
     * @return workflow result info
     */
    public WorkflowResult start(WorkflowOperationRequest request) {
        Preconditions.checkNotNull(request.getName(), "process name cannot be null");
        Preconditions.checkNotNull(request.getForm(), "form cannot be null");

        Response<WorkflowResult> response = ClientUtils.executeHttpCall(workflowApi.start(request));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Initiation process
     *
     * @param processId process id
     * @param request workflow operation request
     * @return workflow result info
     */
    public WorkflowResult cancel(Integer processId, WorkflowOperationRequest request) {
        Preconditions.checkNotNull(processId, "process id cannot be null");

        Response<WorkflowResult> response = ClientUtils.executeHttpCall(workflowApi.cancel(processId, request));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Continue process when pending or failed
     *
     * @param processId process id
     * @param request workflow operation request
     * @return workflow result info
     */
    public WorkflowResult continueProcess(Integer processId, WorkflowOperationRequest request) {
        Preconditions.checkNotNull(processId, "process id cannot be null");

        Response<WorkflowResult> response = ClientUtils.executeHttpCall(
                workflowApi.continueProcess(processId, request));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Cancellation process application
     *
     * @param taskId taskId
     * @param request workflow operation request
     * @return workflow result info
     */
    public WorkflowResult reject(Integer taskId, WorkflowOperationRequest request) {
        Preconditions.checkNotNull(taskId, "task id cannot be null");

        Response<WorkflowResult> response = ClientUtils.executeHttpCall(workflowApi.reject(taskId, request));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Complete task-true to automatic task
     *
     * @param taskId taskId
     * @param request workflow operation request
     * @return workflow result info
     */
    public WorkflowResult complete(Integer taskId, WorkflowOperationRequest request) {
        Preconditions.checkNotNull(taskId, "task id cannot be null");

        Response<WorkflowResult> response = ClientUtils.executeHttpCall(workflowApi.complete(taskId, request));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Query process details according to the tracking number
     *
     * @param processId processId
     * @param taskId taskId
     * @return process detail response
     */
    public ProcessDetailResponse detail(Integer processId, Integer taskId) {
        Preconditions.checkNotNull(processId, "process id cannot be null");

        Response<ProcessDetailResponse> response = ClientUtils.executeHttpCall(workflowApi.detail(processId, taskId));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Get process list
     *
     * @param request workflow process request
     * @return process response list
     */
    public PageResult<ProcessResponse> listProcess(ProcessRequest request) {
        Preconditions.checkNotNull(request, "process request cannot be null");

        Map<String, Object> requestMap = JsonUtils.OBJECT_MAPPER.convertValue(request,
                new TypeReference<Map<String, Object>>() {
                });
        Response<PageResult<ProcessResponse>> response = ClientUtils.executeHttpCall(
                workflowApi.listProcess(requestMap));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Get task list
     *
     * @param request workflow task query request
     * @return task response list
     */
    public PageResult<TaskResponse> listTask(TaskRequest request) {
        Preconditions.checkNotNull(request, "task request cannot be null");

        Map<String, Object> requestMap = JsonUtils.OBJECT_MAPPER.convertValue(request,
                new TypeReference<Map<String, Object>>() {
                });
        Response<PageResult<TaskResponse>> response = ClientUtils.executeHttpCall(workflowApi.listTask(requestMap));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

}
