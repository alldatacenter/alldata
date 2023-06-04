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
import org.apache.inlong.manager.client.api.service.WorkflowApproverApi;
import org.apache.inlong.manager.client.api.util.ClientUtils;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.workflow.ApproverPageRequest;
import org.apache.inlong.manager.pojo.workflow.ApproverRequest;
import org.apache.inlong.manager.pojo.workflow.ApproverResponse;

import java.util.List;
import java.util.Map;

/**
 * Client for {@link org.apache.inlong.manager.client.api.service.WorkflowApproverApi}.
 */
@Slf4j
public class WorkflowApproverClient {

    private final WorkflowApproverApi workflowApproverApi;

    public WorkflowApproverClient(ClientConfiguration configuration) {
        workflowApproverApi = ClientUtils.createRetrofit(configuration)
                .create(WorkflowApproverApi.class);
    }

    /**
     * Save workflow approver
     *
     * @param request approver request
     */
    public Integer save(ApproverRequest request) {
        Preconditions.expectNotBlank(request.getProcessName(), ErrorCodeEnum.INVALID_PARAMETER,
                "process name cannot be empty");
        Preconditions.expectNotBlank(request.getTaskName(), ErrorCodeEnum.INVALID_PARAMETER,
                "task name cannot be empty");
        Preconditions.expectNotBlank(request.getApprovers(), ErrorCodeEnum.INVALID_PARAMETER,
                "approvers cannot be empty");

        Response<Integer> response = ClientUtils.executeHttpCall(workflowApproverApi.save(request));
        ClientUtils.assertRespSuccess(response);

        return response.getData();
    }

    /**
     * Get workflow approver by ID
     *
     * @param id approver id
     * @return approver info
     */
    public ApproverResponse get(Integer id) {
        Preconditions.expectNotNull(id, "id cannot be null");

        Response<ApproverResponse> response = ClientUtils.executeHttpCall(workflowApproverApi.get(id));
        ClientUtils.assertRespSuccess(response);

        return response.getData();
    }

    /**
     * List the workflow approvers according to the query request
     *
     * @param request page query request
     * @return approver list
     */
    public List<ApproverResponse> listByCondition(ApproverPageRequest request) {
        Preconditions.expectNotNull(request.getPageNum(), "page num cannot be null");
        Preconditions.expectNotNull(request.getPageSize(), "page size cannot be null");

        Map<String, Object> requestMap = JsonUtils.OBJECT_MAPPER.convertValue(request,
                new TypeReference<Map<String, Object>>() {
                });
        Response<PageResult<ApproverResponse>> response = ClientUtils.executeHttpCall(
                workflowApproverApi.listByCondition(requestMap));
        ClientUtils.assertRespSuccess(response);

        return response.getData().getList();
    }

    /**
     * Delete workflow approver by ID
     *
     * @param id approver id
     */
    public Boolean delete(Integer id) {
        Preconditions.expectNotNull(id, "id cannot be null");

        Response<Boolean> response = ClientUtils.executeHttpCall(workflowApproverApi.delete(id));
        ClientUtils.assertRespSuccess(response);

        return response.getData();
    }

    /**
     * Update workflow approve.
     *
     * @param request approver request
     */
    public Integer update(ApproverRequest request) {
        Preconditions.expectNotNull(request.getId(), "id cannot be null");

        Response<Integer> response = ClientUtils.executeHttpCall(workflowApproverApi.update(request));
        ClientUtils.assertRespSuccess(response);

        return response.getData();
    }

}
