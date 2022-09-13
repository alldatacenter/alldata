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

import org.apache.inlong.manager.pojo.cluster.ClusterInfo;
import org.apache.inlong.manager.pojo.cluster.ClusterNodeResponse;
import org.apache.inlong.manager.pojo.cluster.ClusterRequest;

import java.util.List;

public interface InlongCluster {

    /**
     * List nodes by clusterName and clusterType
     *
     * @param clusterName cluster name
     * @param clusterType cluster type
     *         {@link org.apache.inlong.manager.common.enums.ClusterType}
     * @return node list
     */
    List<ClusterNodeResponse> listNodes(String clusterName, String clusterType);

    /**
     * List nodes by clusterName, clusterType and tags
     *
     * @param clusterName cluster name
     * @param clusterType cluster type
     *         {@link org.apache.inlong.manager.common.enums.ClusterType}
     * @param clusterTags cluster tags
     * @return node list
     */
    List<ClusterNodeResponse> listNodes(String clusterName, String clusterType, List<String> clusterTags);

    /**
     * List nodes by clusterName, clusterType and tag
     *
     * @param clusterName cluster name
     * @param clusterType cluster type
     *         {@link org.apache.inlong.manager.common.enums.ClusterType}
     * @param clusterTag cluster tag
     * @return node list
     */
    List<ClusterNodeResponse> listNodes(String clusterName, String clusterType, String clusterTag);

    /**
     * Save DATA_PROXY|PULSAR|TUBE cluster
     *
     * @param clusterRequest
     * @return clusterInfo
     */
    ClusterInfo saveCluster(ClusterRequest clusterRequest);

}
