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

package org.apache.inlong.manager.service.cluster.node;

import org.apache.inlong.manager.dao.entity.InlongClusterNodeEntity;
import org.apache.inlong.manager.pojo.cluster.ClusterNodeRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterNodeResponse;

public interface InlongClusterNodeOperator {

    /**
     * Determines whether the current instance matches the specified type.
     */
    Boolean accept(String clusterType);

    /**
     * Get the cluster node type.
     *
     * @return cluster node type string
     */
    String getClusterNodeType();

    /**
     * Save the inlong cluster node info.
     *
     * @param request request of the cluster node
     * @param operator name of the operator
     * @return cluster node id after saving
     */
    Integer saveOpt(ClusterNodeRequest request, String operator);

    /**
     * Get the cluster node info from the given entity.
     *
     * @param entity get field value from the entity
     * @return cluster info after encapsulating
     */
    ClusterNodeResponse getFromEntity(InlongClusterNodeEntity entity);

    /**
     * Update the inlong cluster node info.
     *
     * @param request request of update
     * @param operator name of operator
     */
    void updateOpt(ClusterNodeRequest request, String operator);
}
