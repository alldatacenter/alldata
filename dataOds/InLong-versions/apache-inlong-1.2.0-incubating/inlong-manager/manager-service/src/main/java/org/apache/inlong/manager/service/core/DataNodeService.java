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

package org.apache.inlong.manager.service.core;

import com.github.pagehelper.PageInfo;
import org.apache.inlong.manager.common.pojo.node.DataNodePageRequest;
import org.apache.inlong.manager.common.pojo.node.DataNodeRequest;
import org.apache.inlong.manager.common.pojo.node.DataNodeResponse;

/**
 * Data node service layer interface
 */
public interface DataNodeService {

    /**
     * Save data node.
     *
     * @param request data node info
     * @param operator name of operator
     * @return cluster id after saving
     */
    Integer save(DataNodeRequest request, String operator);

    /**
     * Get data node by id.
     *
     * @param id node id
     * @return node info
     */
    DataNodeResponse get(Integer id);

    /**
     * Paging query nodes according to conditions.
     *
     * @param request page request conditions
     * @return node list
     */
    PageInfo<DataNodeResponse> list(DataNodePageRequest request);

    /**
     * Update data node.
     *
     * @param request node info to be modified
     * @param operator current operator
     * @return whether succeed
     */
    Boolean update(DataNodeRequest request, String operator);

    /**
     * Delete data node.
     *
     * @param id node id to be deleted
     * @param operator current operator
     * @return whether succeed
     */
    Boolean delete(Integer id, String operator);

    /**
     * Test whether the connection can be successfully established.
     *
     * @param request connection request
     * @return true or false
     */
    Boolean testConnection(DataNodeRequest request);

}
