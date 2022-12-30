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

import org.apache.commons.lang3.tuple.Pair;
import org.apache.inlong.manager.client.api.ClientConfiguration;
import org.apache.inlong.manager.client.api.service.InlongStreamApi;
import org.apache.inlong.manager.client.api.util.ClientUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.stream.InlongStreamBriefInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamPageRequest;

import java.util.List;

/**
 * Client for {@link InlongStreamApi}.
 */
public class InlongStreamClient {

    private final InlongStreamApi inlongStreamApi;

    public InlongStreamClient(ClientConfiguration configuration) {
        inlongStreamApi = ClientUtils.createRetrofit(configuration).create(InlongStreamApi.class);
    }

    /**
     * Create an inlong stream.
     */
    public Integer createStreamInfo(InlongStreamInfo streamInfo) {
        Response<Integer> response = ClientUtils.executeHttpCall(inlongStreamApi.createStream(streamInfo));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Query whether the inlong stream ID exists
     *
     * @param streamInfo inlong stream info
     * @return true: exists, false: does not exist
     */
    public Boolean isStreamExists(InlongStreamInfo streamInfo) {
        final String groupId = streamInfo.getInlongGroupId();
        final String streamId = streamInfo.getInlongStreamId();
        Preconditions.checkNotEmpty(groupId, "InlongGroupId should not be empty");
        Preconditions.checkNotEmpty(streamId, "InlongStreamId should not be empty");

        Response<Boolean> response = ClientUtils.executeHttpCall(inlongStreamApi.isStreamExists(groupId, streamId));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * InlongStream info that needs to be modified
     *
     * @param streamInfo inlong stream info that needs to be modified
     * @return whether succeed
     */
    public Pair<Boolean, String> updateStreamInfo(InlongStreamInfo streamInfo) {
        Response<Boolean> resp = ClientUtils.executeHttpCall(inlongStreamApi.updateStream(streamInfo));

        if (resp.getData() != null) {
            return Pair.of(resp.getData(), resp.getErrMsg());
        } else {
            return Pair.of(false, resp.getErrMsg());
        }
    }

    /**
     * Get inlong stream by the given groupId and streamId.
     */
    public InlongStreamInfo getStreamInfo(String groupId, String streamId) {
        Response<InlongStreamInfo> response = ClientUtils.executeHttpCall(inlongStreamApi.getStream(groupId, streamId));

        if (response.isSuccess()) {
            return response.getData();
        }
        if (response.getErrMsg().contains("not exist")) {
            return null;
        } else {
            throw new RuntimeException(response.getErrMsg());
        }
    }

    /**
     * Get inlong stream by the given inlong group id and stream id.
     *
     * @param streamInfo the given inlong stream info
     * @return inlong stream info if exists, null will be returned if not exits
     */
    public InlongStreamInfo getStreamIfExists(InlongStreamInfo streamInfo) {
        if (this.isStreamExists(streamInfo)) {
            return getStreamInfo(streamInfo.getInlongGroupId(), streamInfo.getInlongStreamId());
        }
        return null;
    }

    /**
     * Paging query inlong stream brief info list
     *
     * @param request query request
     * @return inlong stream brief list
     */
    public PageResult<InlongStreamBriefInfo> listByCondition(InlongStreamPageRequest request) {
        Response<PageResult<InlongStreamBriefInfo>> response = ClientUtils.executeHttpCall(
                inlongStreamApi.listByCondition(request));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Get inlong stream info.
     */
    public List<InlongStreamInfo> listStreamInfo(String inlongGroupId) {
        InlongStreamPageRequest pageRequest = new InlongStreamPageRequest();
        pageRequest.setInlongGroupId(inlongGroupId);

        Response<PageResult<InlongStreamInfo>> response = ClientUtils.executeHttpCall(
                inlongStreamApi.listStream(pageRequest));
        ClientUtils.assertRespSuccess(response);
        return response.getData().getList();
    }

    /**
     * Create stream in synchronous/asynchronous way.
     *
     * @param groupId inlong group id
     * @param streamId inlong stream id
     * @return whether succeed
     */
    public boolean startProcess(String groupId, String streamId) {
        Preconditions.checkNotEmpty(groupId, "InlongGroupId should not be empty");
        Preconditions.checkNotEmpty(streamId, "InlongStreamId should not be empty");
        Response<Boolean> response = ClientUtils.executeHttpCall(inlongStreamApi.startProcess(groupId, streamId));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Suspend stream in synchronous/asynchronous way.
     *
     * @param groupId inlong group id
     * @param streamId inlong stream id
     * @return whether succeed
     */
    public boolean suspendProcess(String groupId, String streamId) {
        Preconditions.checkNotEmpty(groupId, "InlongGroupId should not be empty");
        Preconditions.checkNotEmpty(streamId, "InlongStreamId should not be empty");
        Response<Boolean> response = ClientUtils.executeHttpCall(inlongStreamApi.suspendProcess(groupId, streamId));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Restart stream in synchronous/asynchronous way.
     *
     * @param groupId inlong group id
     * @param streamId inlong stream id
     * @return whether succeed
     */
    public boolean restartProcess(String groupId, String streamId) {
        Preconditions.checkNotEmpty(groupId, "InlongGroupId should not be empty");
        Preconditions.checkNotEmpty(streamId, "InlongStreamId should not be empty");
        Response<Boolean> response = ClientUtils.executeHttpCall(inlongStreamApi.restartProcess(groupId, streamId));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Delete stream in synchronous/asynchronous way.
     *
     * @param groupId inlong group id
     * @param streamId inlong stream id
     * @return whether succeed
     */
    public boolean deleteProcess(String groupId, String streamId) {
        Preconditions.checkNotEmpty(groupId, "InlongGroupId should not be empty");
        Preconditions.checkNotEmpty(streamId, "InlongStreamId should not be empty");
        Response<Boolean> response = ClientUtils.executeHttpCall(inlongStreamApi.deleteProcess(groupId, streamId));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Delete the specified inlong stream
     *
     * @param groupId inlong group id
     * @param streamId inlong stream id
     * @return whether succeed
     */
    public boolean delete(String groupId, String streamId) {
        Preconditions.checkNotEmpty(groupId, "InlongGroupId should not be empty");
        Preconditions.checkNotEmpty(streamId, "InlongStreamId should not be empty");
        Response<Boolean> response = ClientUtils.executeHttpCall(inlongStreamApi.delete(groupId, streamId));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }
}
