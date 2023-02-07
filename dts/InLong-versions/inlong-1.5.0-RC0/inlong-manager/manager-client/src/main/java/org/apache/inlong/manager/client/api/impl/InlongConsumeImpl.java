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
import org.apache.inlong.manager.client.api.InlongConsume;
import org.apache.inlong.manager.client.api.inner.client.ClientFactory;
import org.apache.inlong.manager.client.api.inner.client.InlongConsumeClient;
import org.apache.inlong.manager.client.api.util.ClientUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.consume.InlongConsumeBriefInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumeCountInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumeInfo;
import org.apache.inlong.manager.pojo.consume.InlongConsumePageRequest;
import org.apache.inlong.manager.pojo.consume.InlongConsumeRequest;
import org.apache.inlong.manager.pojo.workflow.WorkflowResult;

public class InlongConsumeImpl implements InlongConsume {

    private final InlongConsumeClient consumeClient;

    public InlongConsumeImpl(ClientConfiguration configuration) {
        ClientFactory clientFactory = ClientUtils.getClientFactory(configuration);
        this.consumeClient = clientFactory.getConsumeClient();
    }

    @Override
    public Integer save(InlongConsumeRequest request) {
        Preconditions.checkNotNull(request, "inlong consume request cannot be null");
        Preconditions.checkNotNull(request.getTopic(), "inlong consume topic cannot be null");
        Preconditions.checkNotNull(request.getConsumerGroup(), "inlong consume topic cannot be null");

        return consumeClient.save(request);
    }

    @Override
    public InlongConsumeInfo get(Integer id) {
        Preconditions.checkNotNull(id, "inlong consume id cannot be null");

        return consumeClient.get(id);
    }

    @Override
    public InlongConsumeCountInfo countStatusByUser() {
        return consumeClient.countStatusByUser();
    }

    @Override
    public PageResult<InlongConsumeBriefInfo> list(InlongConsumePageRequest request) {
        return consumeClient.list(request);
    }

    @Override
    public Integer update(InlongConsumeRequest request) {
        Preconditions.checkNotNull(request, "inlong consume request cannot be null");

        return consumeClient.update(request);
    }

    @Override
    public Boolean delete(Integer id) {
        Preconditions.checkNotNull(id, "inlong consume id cannot be null");

        return consumeClient.delete(id);
    }

    @Override
    public WorkflowResult startProcess(Integer id) {
        Preconditions.checkNotNull(id, "inlong consume id cannot be null");

        return consumeClient.startProcess(id);
    }
}
