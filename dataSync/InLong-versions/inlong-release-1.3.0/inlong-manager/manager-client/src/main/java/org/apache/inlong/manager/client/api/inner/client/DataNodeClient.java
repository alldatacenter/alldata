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
import org.apache.inlong.manager.client.api.service.DataNodeApi;
import org.apache.inlong.manager.client.api.util.ClientUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.node.DataNodeRequest;
import org.apache.inlong.manager.pojo.node.DataNodeResponse;

/**
 * Client for {@link DataNodeApi}.
 */
public class DataNodeClient {

    private final DataNodeApi dataNodeApi;

    public DataNodeClient(ClientConfiguration configuration) {
        dataNodeApi = ClientUtils.createRetrofit(configuration).create(DataNodeApi.class);
    }

    /**
     * Save data node.
     *
     * @param request data node info
     * @return cluster id after saving
     */
    public Integer save(DataNodeRequest request) {
        Preconditions.checkNotNull(request, "request cannot be null");
        Preconditions.checkNotEmpty(request.getName(), "data node name cannot be empty");
        Preconditions.checkNotEmpty(request.getType(), "data node type cannot be empty");
        Response<Integer> response = ClientUtils.executeHttpCall(dataNodeApi.save(request));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Get data node by id.
     *
     * @param id node id
     * @return node info
     */
    public DataNodeResponse get(Integer id) {
        Preconditions.checkNotNull(id, "data node id cannot be null");
        Response<DataNodeResponse> response = ClientUtils.executeHttpCall(dataNodeApi.get(id));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Paging query nodes according to conditions.
     *
     * @param request page request conditions
     * @return node list
     */
    public PageResult<DataNodeResponse> list(DataNodeRequest request) {
        Preconditions.checkNotNull(request, "request cannot be null");
        Response<PageResult<DataNodeResponse>> response = ClientUtils.executeHttpCall(dataNodeApi.list(request));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Update data node.
     *
     * @param request node info to be modified
     * @return whether succeed
     */
    public Boolean update(DataNodeRequest request) {
        Preconditions.checkNotNull(request, "request cannot be null");
        Preconditions.checkNotEmpty(request.getName(), "data node name cannot be empty");
        Preconditions.checkNotEmpty(request.getType(), "data node type cannot be empty");
        Preconditions.checkNotNull(request.getId(), "data node id cannot be null");
        Response<Boolean> response = ClientUtils.executeHttpCall(dataNodeApi.update(request));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Delete data node.
     *
     * @param id node id to be deleted
     * @return whether succeed
     */
    public Boolean delete(Integer id) {
        Preconditions.checkNotNull(id, "data node id cannot be null");
        Response<Boolean> response = ClientUtils.executeHttpCall(dataNodeApi.delete(id));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

}
