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
import org.apache.inlong.manager.client.api.service.StreamTransformApi;
import org.apache.inlong.manager.client.api.util.ClientUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.transform.TransformRequest;
import org.apache.inlong.manager.pojo.transform.TransformResponse;

import java.util.List;

/**
 * Client for {@link StreamTransformApi}.
 */
public class StreamTransformClient {

    private final StreamTransformApi streamTransformApi;

    public StreamTransformClient(ClientConfiguration configuration) {
        streamTransformApi = ClientUtils.createRetrofit(configuration).create(StreamTransformApi.class);
    }

    /**
     * Create a conversion function info.
     */
    public Integer createTransform(TransformRequest transformRequest) {
        Response<Integer> response = ClientUtils.executeHttpCall(streamTransformApi.createTransform(transformRequest));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Get all conversion function info.
     */
    public List<TransformResponse> listTransform(String groupId, String streamId) {
        Response<List<TransformResponse>> response = ClientUtils.executeHttpCall(
                streamTransformApi.listTransform(groupId, streamId));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Update conversion function info.
     */
    public Pair<Boolean, String> updateTransform(TransformRequest transformRequest) {
        Response<Boolean> response = ClientUtils.executeHttpCall(streamTransformApi.updateTransform(transformRequest));

        if (response.getData() != null) {
            return Pair.of(response.getData(), response.getErrMsg());
        } else {
            return Pair.of(false, response.getErrMsg());
        }
    }

    /**
     * Delete conversion function info.
     */
    public boolean deleteTransform(TransformRequest transformRequest) {
        Preconditions.checkNotEmpty(transformRequest.getInlongGroupId(), "inlongGroupId should not be null");
        Preconditions.checkNotEmpty(transformRequest.getInlongStreamId(), "inlongStreamId should not be null");
        Preconditions.checkNotEmpty(transformRequest.getTransformName(), "transformName should not be null");

        Response<Boolean> response = ClientUtils.executeHttpCall(
                streamTransformApi.deleteTransform(transformRequest.getInlongGroupId(),
                        transformRequest.getInlongStreamId(), transformRequest.getTransformName()));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }
}
