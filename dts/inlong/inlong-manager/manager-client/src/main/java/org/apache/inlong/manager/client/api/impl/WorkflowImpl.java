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

package org.apache.inlong.manager.client.api.impl;

import org.apache.inlong.manager.client.api.ClientConfiguration;
import org.apache.inlong.manager.client.api.Workflow;
import org.apache.inlong.manager.client.api.inner.client.ClientFactory;
import org.apache.inlong.manager.client.api.inner.client.WorkflowClient;
import org.apache.inlong.manager.client.api.util.ClientUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.workflow.ProcessDetailResponse;
import org.apache.inlong.manager.pojo.workflow.ProcessRequest;
import org.apache.inlong.manager.pojo.workflow.ProcessResponse;
import org.apache.inlong.manager.pojo.workflow.TaskRequest;
import org.apache.inlong.manager.pojo.workflow.TaskResponse;
import org.apache.inlong.manager.pojo.workflow.WorkflowOperationRequest;
import org.apache.inlong.manager.pojo.workflow.WorkflowResult;

public class WorkflowImpl implements Workflow {

    private final WorkflowClient workflowClient;

    public WorkflowImpl(ClientConfiguration configuration) {
        ClientFactory clientFactory = ClientUtils.getClientFactory(configuration);
        this.workflowClient = clientFactory.getWorkflowClient();
    }

    @Override
    public WorkflowResult start(WorkflowOperationRequest request) {
        Preconditions.expectNotNull(request.getName(), "process name cannot be null");
        Preconditions.expectNotNull(request.getForm(), "form cannot be null");
        return workflowClient.start(request);
    }

    @Override
    public WorkflowResult cancel(Integer processId, WorkflowOperationRequest request) {
        Preconditions.expectNotNull(processId, "process id cannot be null");
        return workflowClient.cancel(processId, request);
    }

    @Override
    public WorkflowResult continueProcess(Integer processId, WorkflowOperationRequest request) {
        Preconditions.expectNotNull(processId, "process id cannot be null");
        return workflowClient.continueProcess(processId, request);
    }

    @Override
    public WorkflowResult reject(Integer taskId, WorkflowOperationRequest request) {
        Preconditions.expectNotNull(taskId, "task id cannot be null");
        return workflowClient.reject(taskId, request);
    }

    @Override
    public WorkflowResult complete(Integer taskId, WorkflowOperationRequest request) {
        Preconditions.expectNotNull(taskId, "task id cannot be null");
        return workflowClient.complete(taskId, request);
    }

    @Override
    public ProcessDetailResponse detail(Integer processId, Integer taskId) {
        Preconditions.expectNotNull(processId, "process id cannot be null");
        return workflowClient.detail(processId, taskId);
    }

    @Override
    public PageResult<ProcessResponse> listProcess(ProcessRequest request) {
        Preconditions.expectNotNull(request, "process request cannot be null");
        return workflowClient.listProcess(request);
    }

    @Override
    public PageResult<TaskResponse> listTask(TaskRequest request) {
        Preconditions.expectNotNull(request, "task request cannot be null");
        return workflowClient.listTask(request);
    }
}
