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

package org.apache.inlong.manager.service.cluster;

import org.apache.inlong.manager.dao.entity.InlongClusterEntity;
import org.apache.inlong.manager.pojo.cluster.ClusterInfo;
import org.apache.inlong.manager.pojo.cluster.ClusterRequest;

/**
 * Interface of the inlong cluster operator.
 */
public interface InlongClusterOperator {

    /**
     * Determines whether the current instance matches the specified type.
     */
    Boolean accept(String clusterType);

    /**
     * Get the cluster type.
     *
     * @return cluster type string
     */
    String getClusterType();

    /**
     * Save the inlong cluster info.
     *
     * @param request request of the cluster
     * @param operator name of the operator
     * @return cluster id after saving
     */
    Integer saveOpt(ClusterRequest request, String operator);

    /**
     * Get the cluster info from the given entity.
     *
     * @param entity get field value from the entity
     * @return cluster info after encapsulating
     */
    ClusterInfo getFromEntity(InlongClusterEntity entity);

    /**
     * Update the inlong cluster info.
     *
     * @param request request of update
     * @param operator name of operator
     */
    void updateOpt(ClusterRequest request, String operator);

    /**
     * Test connection
     *
     * @param request request of the cluster
     * @return Whether the connection is successful
     */
    Boolean testConnection(ClusterRequest request);

}
