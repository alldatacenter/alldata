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
import org.apache.inlong.manager.client.api.DataNode;
import org.apache.inlong.manager.client.api.inner.client.ClientFactory;
import org.apache.inlong.manager.client.api.inner.client.DataNodeClient;
import org.apache.inlong.manager.client.api.util.ClientUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.node.DataNodeInfo;
import org.apache.inlong.manager.pojo.node.DataNodeRequest;

public class DataNodeImpl implements DataNode {

    private final DataNodeClient dataNodeClient;

    public DataNodeImpl(ClientConfiguration configuration) {
        ClientFactory clientFactory = ClientUtils.getClientFactory(configuration);
        this.dataNodeClient = clientFactory.getDataNodeClient();
    }

    @Override
    public Integer save(DataNodeRequest request) {
        Preconditions.checkNotNull(request, "request cannot be null");
        Preconditions.checkNotEmpty(request.getName(), "data node name cannot be empty");
        Preconditions.checkNotEmpty(request.getType(), "data node type cannot be empty");
        return dataNodeClient.save(request);
    }

    @Override
    public DataNodeInfo get(Integer id) {
        Preconditions.checkNotNull(id, "data node id cannot be null");
        return dataNodeClient.get(id);
    }

    @Override
    public PageResult<DataNodeInfo> list(DataNodeRequest request) {
        Preconditions.checkNotNull(request, "request cannot be null");
        return dataNodeClient.list(request);
    }

    @Override
    public Boolean update(DataNodeRequest request) {
        Preconditions.checkNotNull(request, "request cannot be null");
        Preconditions.checkNotEmpty(request.getName(), "data node name cannot be empty");
        Preconditions.checkNotEmpty(request.getType(), "data node type cannot be empty");
        Preconditions.checkNotNull(request.getId(), "data node id cannot be null");
        return dataNodeClient.update(request);
    }

    @Override
    public Boolean delete(Integer id) {
        Preconditions.checkNotNull(id, "data node id cannot be null");
        return dataNodeClient.delete(id);
    }
}
