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

package org.apache.inlong.manager.client.api;

import org.apache.inlong.manager.client.api.impl.LowLevelInlongClientImpl;
import org.apache.inlong.manager.pojo.cluster.ClusterRequest;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.group.InlongGroupBriefInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupPageRequest;

/**
 * An interface to provide original Inlong Apis.
 * Not recommend to use
 */
public interface LowLevelInlongClient {

    /**
     * Create inlong client.
     *
     * @param serviceUrl the service url
     * @param configuration the configuration
     * @return the inlong client
     */
    static LowLevelInlongClient create(String serviceUrl, ClientConfiguration configuration) {
        return new LowLevelInlongClientImpl(serviceUrl, configuration);
    }

    /**
     * Create cluster.
     *
     * @param request cluster request
     * @return cluster index
     * @throws Exception the exception may throw
     */
    Integer saveCluster(ClusterRequest request) throws Exception;

    /**
     * List inlong group.
     *
     * @param request page request
     * @return group info page
     * @throws Exception the exception may throw
     */
    PageResult<InlongGroupBriefInfo> listGroup(InlongGroupPageRequest request) throws Exception;

}
