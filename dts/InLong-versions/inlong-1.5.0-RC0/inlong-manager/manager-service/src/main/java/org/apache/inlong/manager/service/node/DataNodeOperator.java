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

package org.apache.inlong.manager.service.node;

import org.apache.inlong.manager.dao.entity.DataNodeEntity;
import org.apache.inlong.manager.pojo.node.DataNodeInfo;
import org.apache.inlong.manager.pojo.node.DataNodeRequest;

import java.util.Map;

/**
 * Interface of the data node operator.
 */
public interface DataNodeOperator {

    /**
     * Determines whether the current instance matches the specified type.
     */
    Boolean accept(String dataNodeType);

    /**
     * Get the data node type.
     *
     * @return data node type string
     */
    String getDataNodeType();

    /**
     * Save the data node info.
     *
     * @param request request of the data node
     * @param operator name of the operator
     * @return data node id after saving
     */
    Integer saveOpt(DataNodeRequest request, String operator);

    /**
     * Get the data node info from the given entity.
     *
     * @param entity get field value from the entity
     * @return cluster info after encapsulating
     */
    DataNodeInfo getFromEntity(DataNodeEntity entity);

    /**
     * Update the data node info.
     *
     * @param request request of update
     * @param operator name of operator
     */
    void updateOpt(DataNodeRequest request, String operator);

    /**
     * Parse data node info to sort-standalone sink params
     * @param info DataNodeInfo
     * @return Sink params
     */
    Map<String, String> parse2SinkParams(DataNodeInfo info);

    /**
     * Test connection
     * @param request request of the data node
     * @return Whether the connection is successful
     */
    Boolean testConnection(DataNodeRequest request);

}
