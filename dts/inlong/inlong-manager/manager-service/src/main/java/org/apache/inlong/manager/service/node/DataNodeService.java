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

import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.UpdateResult;
import org.apache.inlong.manager.pojo.node.DataNodeInfo;
import org.apache.inlong.manager.pojo.node.DataNodePageRequest;
import org.apache.inlong.manager.pojo.node.DataNodeRequest;
import org.apache.inlong.manager.pojo.user.UserInfo;

import java.util.List;

/**
 * Data node service layer interface
 */
public interface DataNodeService {

    /**
     * Save data node.
     *
     * @param request  data node info
     * @param operator name of operator
     * @return cluster id after saving
     */
    Integer save(DataNodeRequest request, String operator);

    /**
     * Save data node.
     *
     * @param request data node info
     * @param opInfo  userinfo of operator
     * @return cluster id after saving
     */
    Integer save(DataNodeRequest request, UserInfo opInfo);

    /**
     * Get data node by id.
     *
     * @param id node id
     * @param currentUser current user
     * @return node info
     */
    DataNodeInfo get(Integer id, String currentUser);

    /**
     * Get data node by id.
     *
     * @param id     node id
     * @param opInfo userinfo of operator
     * @return node info
     */
    DataNodeInfo get(Integer id, UserInfo opInfo);

    /**
     * Get data node by name and type.
     *
     * @param name node name
     * @param type node type
     * @return node info
     */
    DataNodeInfo get(String name, String type);

    /**
     * Paging query nodes according to conditions.
     *
     * @param request page request conditions
     * @return node list
     */
    PageResult<DataNodeInfo> list(DataNodePageRequest request);

    /**
     * Query nodes according to conditions.
     *
     * @param request page request conditions
     * @param opInfo  userinfo of operator
     * @return node list
     */
    List<DataNodeInfo> list(DataNodePageRequest request, UserInfo opInfo);

    /**
     * Update data node.
     *
     * @param request  node info to be modified
     * @param operator current operator
     * @return whether succeed
     */
    Boolean update(DataNodeRequest request, String operator);

    /**
     * Update data node.
     *
     * @param request node info to be modified
     * @param opInfo  userinfo of operator
     * @return whether succeed
     */
    Boolean update(DataNodeRequest request, UserInfo opInfo);

    /**
     * Update data node by key.
     *
     * @param request  node info to be modified
     * @param operator current operator
     * @return update result
     */
    UpdateResult updateByKey(DataNodeRequest request, String operator);

    /**
     * Delete data node.
     *
     * @param id       node id to be deleted
     * @param operator current operator
     * @return whether succeed
     */
    Boolean delete(Integer id, String operator);

    /**
     * Delete data node.
     *
     * @param id     node id to be deleted
     * @param opInfo userinfo of operator
     * @return whether succeed
     */
    Boolean delete(Integer id, UserInfo opInfo);

    /**
     * Delete data node by key.
     *
     * @param name     node name to be deleted
     * @param type     node type to be deleted
     * @param operator current operator
     * @return whether succeed
     */
    Boolean deleteByKey(String name, String type, String operator);

    /**
     * Test whether the connection can be successfully established.
     *
     * @param request connection request
     * @return true or false
     */
    Boolean testConnection(DataNodeRequest request);

}
