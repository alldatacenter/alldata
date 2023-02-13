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
import org.apache.inlong.manager.client.api.StreamSource;
import org.apache.inlong.manager.client.api.inner.client.ClientFactory;
import org.apache.inlong.manager.client.api.inner.client.StreamSourceClient;
import org.apache.inlong.manager.client.api.util.ClientUtils;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.util.Preconditions;

public class StreamSourceImpl implements StreamSource {

    private final StreamSourceClient sourceClient;

    public StreamSourceImpl(ClientConfiguration configuration) {
        ClientFactory clientFactory = ClientUtils.getClientFactory(configuration);
        this.sourceClient = clientFactory.getSourceClient();
    }

    @Override
    public Boolean forceDelete(String groupId, String streamId) {
        Preconditions.checkNotNull(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY.getMessage());
        Preconditions.checkNotNull(streamId, ErrorCodeEnum.STREAM_ID_IS_EMPTY.getMessage());
        return sourceClient.forceDelete(groupId, streamId);
    }
}
