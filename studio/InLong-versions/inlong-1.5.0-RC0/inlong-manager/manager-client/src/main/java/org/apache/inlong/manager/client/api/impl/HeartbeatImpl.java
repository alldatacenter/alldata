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
import org.apache.inlong.manager.client.api.Heartbeat;
import org.apache.inlong.manager.client.api.inner.client.ClientFactory;
import org.apache.inlong.manager.client.api.inner.client.HeartbeatClient;
import org.apache.inlong.manager.client.api.util.ClientUtils;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.heartbeat.ComponentHeartbeatResponse;
import org.apache.inlong.manager.pojo.heartbeat.GroupHeartbeatResponse;
import org.apache.inlong.manager.pojo.heartbeat.HeartbeatPageRequest;
import org.apache.inlong.manager.pojo.heartbeat.HeartbeatQueryRequest;
import org.apache.inlong.manager.pojo.heartbeat.StreamHeartbeatResponse;

/**
 * Heartbeat interface implementation
 */
public class HeartbeatImpl implements Heartbeat {

    private final HeartbeatClient heartbeatClient;

    public HeartbeatImpl(ClientConfiguration configuration) {
        ClientFactory clientFactory = ClientUtils.getClientFactory(configuration);
        this.heartbeatClient = clientFactory.getHeartbeatClient();
    }

    @Override
    public ComponentHeartbeatResponse getComponentHeartbeat(HeartbeatQueryRequest request) {
        Preconditions.checkNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getComponent(), ErrorCodeEnum.REQUEST_COMPONENT_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getInstance(), ErrorCodeEnum.REQUEST_INSTANCE_EMPTY.getMessage());

        return heartbeatClient.getComponentHeartbeat(request);
    }

    @Override
    public GroupHeartbeatResponse getGroupHeartbeat(HeartbeatQueryRequest request) {
        Preconditions.checkNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getComponent(), ErrorCodeEnum.REQUEST_COMPONENT_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getInstance(), ErrorCodeEnum.REQUEST_INSTANCE_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getInlongGroupId(), ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());

        return heartbeatClient.getGroupHeartbeat(request);
    }

    @Override
    public StreamHeartbeatResponse getStreamHeartbeat(HeartbeatQueryRequest request) {
        Preconditions.checkNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getComponent(), ErrorCodeEnum.REQUEST_COMPONENT_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getInstance(), ErrorCodeEnum.REQUEST_INSTANCE_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getInlongGroupId(), ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getInlongStreamId(), ErrorCodeEnum.STREAM_ID_IS_EMPTY.getMessage());

        return heartbeatClient.getStreamHeartbeat(request);
    }

    @Override
    public PageResult<ComponentHeartbeatResponse> listComponentHeartbeat(HeartbeatPageRequest request) {
        Preconditions.checkNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getComponent(), ErrorCodeEnum.REQUEST_COMPONENT_EMPTY.getMessage());

        return heartbeatClient.listComponentHeartbeat(request);
    }

    @Override
    public PageResult<GroupHeartbeatResponse> listGroupHeartbeat(HeartbeatPageRequest request) {
        Preconditions.checkNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getComponent(), ErrorCodeEnum.REQUEST_COMPONENT_EMPTY.getMessage());

        return heartbeatClient.listGroupHeartbeat(request);
    }

    @Override
    public PageResult<StreamHeartbeatResponse> listStreamHeartbeat(HeartbeatPageRequest request) {
        Preconditions.checkNotNull(request, ErrorCodeEnum.REQUEST_IS_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getComponent(), ErrorCodeEnum.REQUEST_COMPONENT_EMPTY.getMessage());
        Preconditions.checkNotEmpty(request.getInlongGroupId(), ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());

        return heartbeatClient.listStreamHeartbeat(request);
    }
}
