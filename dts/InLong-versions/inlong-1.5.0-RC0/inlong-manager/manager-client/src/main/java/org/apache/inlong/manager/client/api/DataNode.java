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

import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.node.DataNodeInfo;
import org.apache.inlong.manager.pojo.node.DataNodeRequest;

public interface DataNode {

    /**
     * Save data node.
     *
     * @param request data node info
     * @return cluster id after saving
     */
    Integer save(DataNodeRequest request);

    /**
     * Get data node by id.
     *
     * @param id node id
     * @return node info
     */
    DataNodeInfo get(Integer id);

    /**
     * Paging query nodes according to conditions.
     *
     * @param request page request conditions
     * @return node list
     */
    PageResult<DataNodeInfo> list(DataNodeRequest request);

    /**
     * Update data node.
     *
     * @param request node info to be modified
     * @return whether succeed
     */
    public Boolean update(DataNodeRequest request);

    /**
     * Delete data node.
     *
     * @param id node id to be deleted
     * @return whether succeed
     */
    Boolean delete(Integer id);

}
