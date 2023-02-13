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
import org.apache.inlong.manager.client.api.service.HeartbeatApi;
import org.apache.inlong.manager.client.api.util.ClientUtils;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.heartbeat.ComponentHeartbeatResponse;
import org.apache.inlong.manager.pojo.heartbeat.GroupHeartbeatResponse;
import org.apache.inlong.manager.pojo.heartbeat.HeartbeatPageRequest;
import org.apache.inlong.manager.pojo.heartbeat.HeartbeatQueryRequest;
import org.apache.inlong.manager.pojo.heartbeat.StreamHeartbeatResponse;

/**
 * Client for {@link HeartbeatApi}.
 */
public class HeartbeatClient {

    private final HeartbeatApi heartbeatApi;

    public HeartbeatClient(ClientConfiguration configuration) {
        heartbeatApi = ClientUtils.createRetrofit(configuration).create(HeartbeatApi.class);
    }

    /**
     * Get component heartbeat
     *
     * @param request query request of heartbeat
     * @return component heartbeat
     */
    public ComponentHeartbeatResponse getComponentHeartbeat(HeartbeatQueryRequest request) {
        Preconditions.checkNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getComponent(), ErrorCodeEnum.REQUEST_COMPONENT_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getInstance(), ErrorCodeEnum.REQUEST_INSTANCE_EMPTY.getMessage());

        Response<ComponentHeartbeatResponse> response = ClientUtils.executeHttpCall(
                heartbeatApi.getComponentHeartbeat(request));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Get inlong group heartbeat
     *
     * @param request query request of heartbeat
     * @return group heartbeat
     */
    public GroupHeartbeatResponse getGroupHeartbeat(HeartbeatQueryRequest request) {
        Preconditions.checkNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getComponent(), ErrorCodeEnum.REQUEST_COMPONENT_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getInstance(), ErrorCodeEnum.REQUEST_INSTANCE_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getInlongGroupId(), ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());

        Response<GroupHeartbeatResponse> response = ClientUtils.executeHttpCall(
                heartbeatApi.getGroupHeartbeat(request));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Get inlong stream heartbeat
     *
     * @param request query request of heartbeat
     * @return stream heartbeat
     */
    public StreamHeartbeatResponse getStreamHeartbeat(HeartbeatQueryRequest request) {
        Preconditions.checkNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getComponent(), ErrorCodeEnum.REQUEST_COMPONENT_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getInstance(), ErrorCodeEnum.REQUEST_INSTANCE_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getInlongGroupId(), ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getInlongStreamId(), ErrorCodeEnum.STREAM_ID_IS_EMPTY.getMessage());

        Response<StreamHeartbeatResponse> response = ClientUtils.executeHttpCall(
                heartbeatApi.getStreamHeartbeat(request));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * List component heartbeat by page
     *
     * @param request paging query request
     * @return list of component heartbeat
     */
    public PageResult<ComponentHeartbeatResponse> listComponentHeartbeat(HeartbeatPageRequest request) {
        Preconditions.checkNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getComponent(), ErrorCodeEnum.REQUEST_COMPONENT_EMPTY.getMessage());

        Response<PageResult<ComponentHeartbeatResponse>> response = ClientUtils.executeHttpCall(
                heartbeatApi.listComponentHeartbeat(request));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * List group heartbeat by page
     *
     * @param request paging query request
     * @return list of group heartbeat
     */
    public PageResult<GroupHeartbeatResponse> listGroupHeartbeat(HeartbeatPageRequest request) {
        Preconditions.checkNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getComponent(), ErrorCodeEnum.REQUEST_COMPONENT_EMPTY.getMessage());

        Response<PageResult<GroupHeartbeatResponse>> response = ClientUtils.executeHttpCall(
                heartbeatApi.listGroupHeartbeat(request));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * List stream heartbeat by page
     *
     * @param request paging query request
     * @return list of stream heartbeat
     */
    public PageResult<StreamHeartbeatResponse> listStreamHeartbeat(HeartbeatPageRequest request) {
        Preconditions.checkNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getComponent(), ErrorCodeEnum.REQUEST_COMPONENT_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getInlongGroupId(), ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());

        Response<PageResult<StreamHeartbeatResponse>> response = ClientUtils.executeHttpCall(
                heartbeatApi.listStreamHeartbeat(request));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }
}
