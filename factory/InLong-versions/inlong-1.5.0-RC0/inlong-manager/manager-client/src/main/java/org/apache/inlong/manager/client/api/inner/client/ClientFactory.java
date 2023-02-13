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

import lombok.Getter;
import org.apache.inlong.manager.client.api.ClientConfiguration;

/**
 * Factory for {@link org.apache.inlong.manager.client.api.inner.client}.
 */
@Getter
public class ClientFactory {

    private final InlongGroupClient groupClient;

    private final InlongStreamClient streamClient;

    private final StreamSinkClient sinkClient;

    private final StreamSourceClient sourceClient;

    private final InlongClusterClient clusterClient;

    private final StreamTransformClient transformClient;

    private final WorkflowClient workflowClient;

    private final DataNodeClient dataNodeClient;

    private final UserClient userClient;

    private final NoAuthClient noAuthClient;

    private final HeartbeatClient heartbeatClient;

    private final WorkflowApproverClient workflowApproverClient;
    private final WorkflowEventClient workflowEventClient;
    private final InlongConsumeClient consumeClient;

    public ClientFactory(ClientConfiguration configuration) {
        groupClient = new InlongGroupClient(configuration);
        streamClient = new InlongStreamClient(configuration);
        sourceClient = new StreamSourceClient(configuration);
        sinkClient = new StreamSinkClient(configuration);
        clusterClient = new InlongClusterClient(configuration);
        transformClient = new StreamTransformClient(configuration);
        workflowClient = new WorkflowClient(configuration);
        dataNodeClient = new DataNodeClient(configuration);
        userClient = new UserClient(configuration);
        noAuthClient = new NoAuthClient(configuration);
        heartbeatClient = new HeartbeatClient(configuration);
        workflowApproverClient = new WorkflowApproverClient(configuration);
        workflowEventClient = new WorkflowEventClient(configuration);
        consumeClient = new InlongConsumeClient(configuration);
    }
}
