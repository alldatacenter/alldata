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

import org.apache.inlong.manager.client.api.ClientConfiguration;
import org.apache.inlong.manager.client.api.service.InlongConsumeApi;
import org.apache.inlong.manager.client.api.util.ClientUtils;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.consume.InlongConsumeBriefInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumeCountInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumeInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumePageRequest;
import org.apache.inlong.manager.pojo.consume.InlongConsumeRequest;
import org.apache.inlong.manager.pojo.workflow.WorkflowResult;

/**
 * Client for {@link InlongConsumeApi}.
 */
public class InlongConsumeClient {

    private final InlongConsumeApi inlongConsumeApi;

    public InlongConsumeClient(ClientConfiguration configuration) {
        inlongConsumeApi = ClientUtils.createRetrofit(configuration).create(InlongConsumeApi.class);
    }

    /**
     * Save inlong consume info.
     *
     * @param request consume request need to save
     * @return inlong consume id after saving
     */
    public Integer save(InlongConsumeRequest request) {
        Preconditions.expectNotNull(request, "inlong consume request cannot be null");
        Preconditions.expectNotBlank(request.getTopic(), ErrorCodeEnum.INVALID_PARAMETER,
                "inlong consume topic cannot be null");
        Preconditions.expectNotBlank(request.getConsumerGroup(), ErrorCodeEnum.INVALID_PARAMETER,
                "inlong consume topic cannot be null");

        Response<Integer> response = ClientUtils.executeHttpCall(inlongConsumeApi.save(request));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Get inlong consume info based on ID
     *
     * @param id inlong consume id
     * @return detail of inlong group
     */
    public InlongConsumeInfo get(Integer id) {
        Preconditions.expectNotNull(id, "inlong consume id cannot be null");

        Response<InlongConsumeInfo> response = ClientUtils.executeHttpCall(inlongConsumeApi.get(id));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Query the inlong consume statistics info via the username
     *
     * @return inlong consume status statistics
     */
    public InlongConsumeCountInfo countStatusByUser() {
        Response<InlongConsumeCountInfo> response = ClientUtils.executeHttpCall(inlongConsumeApi.countStatusByUser());
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Paging query inlong consume info list
     *
     * @param request pagination query request
     * @return inlong consume list
     */
    public PageResult<InlongConsumeBriefInfo> list(InlongConsumePageRequest request) {
        Preconditions.expectNotNull(request, "inlong consume request cannot be null");

        Response<PageResult<InlongConsumeBriefInfo>> response = ClientUtils.executeHttpCall(
                inlongConsumeApi.list(request));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Update the inlong consume
     *
     * @param request inlong consume request that needs to be updated
     * @return inlong consume id after saving
     */
    public Integer update(InlongConsumeRequest request) {
        Preconditions.expectNotNull(request, "inlong consume request cannot be null");

        Response<Integer> response = ClientUtils.executeHttpCall(inlongConsumeApi.update(request));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Delete the inlong consume by the id
     *
     * @param id inlong consume id that needs to be deleted
     * @return whether succeed
     */
    public Boolean delete(Integer id) {
        Preconditions.expectNotNull(id, "inlong consume id cannot be null");

        Response<Boolean> response = ClientUtils.executeHttpCall(inlongConsumeApi.delete(id));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Start the process for the specified ID.
     *
     * @param id inlong consume id
     * @return workflow result
     */
    public WorkflowResult startProcess(Integer id) {
        Preconditions.expectNotNull(id, "inlong consume id cannot be null");

        Response<WorkflowResult> response = ClientUtils.executeHttpCall(inlongConsumeApi.startProcess(id));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }
}
