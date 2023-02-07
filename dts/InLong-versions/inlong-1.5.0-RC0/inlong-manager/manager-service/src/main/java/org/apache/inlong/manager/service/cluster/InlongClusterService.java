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

import org.apache.inlong.common.pojo.dataproxy.DataProxyConfig;
import org.apache.inlong.common.pojo.dataproxy.DataProxyNodeResponse;
import org.apache.inlong.manager.pojo.cluster.BindTagRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterInfo;
import org.apache.inlong.manager.pojo.cluster.ClusterNodeRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterNodeResponse;
import org.apache.inlong.manager.pojo.cluster.ClusterPageRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterTagPageRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterTagRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterTagResponse;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.UpdateResult;
import org.apache.inlong.manager.pojo.user.UserInfo;

import java.util.List;

/**
 * Inlong cluster service layer interface
 */
public interface InlongClusterService {

    /**
     * Save cluster tag.
     *
     * @param request cluster tag
     * @param operator name of operator
     * @return cluster tag id after saving
     */
    Integer saveTag(ClusterTagRequest request, String operator);

    /**
     * Save cluster tag.
     *
     * @param request cluster tag
     * @param opInfo userinfo of operator
     * @return cluster tag id after saving
     */
    Integer saveTag(ClusterTagRequest request, UserInfo opInfo);

    /**
     * Get cluster tag by id.
     *
     * @param id cluster tag id
     * @param currentUser current operator
     * @return cluster tag info
     */
    ClusterTagResponse getTag(Integer id, String currentUser);

    /**
     * Get cluster tag by id.
     *
     * @param id cluster tag id
     * @param opInfo userinfo of operator
     * @return cluster tag info
     */
    ClusterTagResponse getTag(Integer id, UserInfo opInfo);

    /**
     * Paging query cluster tags according to conditions.
     *
     * @param request page request conditions
     * @return cluster tag list
     */
    PageResult<ClusterTagResponse> listTag(ClusterTagPageRequest request);

    /**
     * Query cluster tags according to conditions.
     *
     * @param request page request conditions
     * @param opInfo userinfo of operator
     * @return cluster tag list
     */
    List<ClusterTagResponse> listTag(ClusterTagPageRequest request, UserInfo opInfo);

    /**
     * Update cluster tag.
     *
     * @param request cluster tag to be modified
     * @param operator current operator
     * @return whether succeed
     */
    Boolean updateTag(ClusterTagRequest request, String operator);

    /**
     * Update cluster tag.
     *
     * @param request cluster tag to be modified
     * @param opInfo userinfo of operator
     * @return whether succeed
     */
    Boolean updateTag(ClusterTagRequest request, UserInfo opInfo);

    /**
     * Delete cluster tag.
     *
     * @param id cluster tag id to be deleted
     * @param operator current operator
     * @return whether succeed
     */
    Boolean deleteTag(Integer id, String operator);

    /**
     * Delete cluster tag.
     *
     * @param id cluster tag id to be deleted
     * @param opInfo userinfo of operator
     * @return whether succeed
     */
    Boolean deleteTag(Integer id, UserInfo opInfo);

    /**
     * Save cluster info.
     *
     * @param request inlong cluster info
     * @param operator name of operator
     * @return cluster id after saving
     */
    Integer save(ClusterRequest request, String operator);

    /**
     * Save cluster info.
     *
     * @param request inlong cluster info
     * @param opInfo userinfo of operator
     * @return cluster id after saving
     */
    Integer save(ClusterRequest request, UserInfo opInfo);

    /**
     * Get cluster info by id.
     *
     * @param id cluster id
     * @param currentUser current operator
     * @return cluster info
     */
    ClusterInfo get(Integer id, String currentUser);

    /**
     * Get cluster info by id.
     *
     * @param id cluster id
     * @param opInfo userinfo of operator
     * @return cluster info
     */
    ClusterInfo get(Integer id, UserInfo opInfo);

    /**
     * Get one cluster by the cluster tag, cluster name and cluster type.
     *
     * @param clusterTag cluster tag
     * @param clusterName cluster name
     * @param clusterType cluster type
     * @return cluster info
     * @apiNote No matter how many clusters there are, only one cluster is returned.
     */
    ClusterInfo getOne(String clusterTag, String clusterName, String clusterType);

    /**
     * Paging query clusters according to conditions.
     *
     * @param request page request conditions
     * @return cluster list
     */
    PageResult<ClusterInfo> list(ClusterPageRequest request);

    /**
     * Query clusters according to conditions.
     *
     * @param request page request conditions
     * @param opInfo userinfo of operator
     * @return cluster list
     */
    List<ClusterInfo> list(ClusterPageRequest request, UserInfo opInfo);

    /**
     * List clusters by tag and type
     *
     * @param clusterTag cluster tag
     * @param clusterType cluster type
     * @return cluster info list
     */
    List<ClusterInfo> listByTagAndType(String clusterTag, String clusterType);

    /**
     * Update cluster information
     *
     * @param request cluster info to be modified
     * @param operator current operator
     * @return whether succeed
     */
    Boolean update(ClusterRequest request, String operator);

    /**
     * Update cluster information
     *
     * @param request cluster info to be modified
     * @param opInfo userinfo of operator
     * @return whether succeed
     */
    Boolean update(ClusterRequest request, UserInfo opInfo);

    /**
     * Update cluster information by unique key
     *
     * @param request cluster info to be modified
     * @param operator current operator
     * @return update result
     */
    UpdateResult updateByKey(ClusterRequest request, String operator);

    /**
     * Bind or unbind cluster tag for clusters.
     *
     * @param request cluster info to be modified
     * @param operator current operator
     * @return whether succeed
     */
    Boolean bindTag(BindTagRequest request, String operator);

    /**
     * Bind or unbind cluster tag for clusters.
     *
     * @param request cluster info to be modified
     * @param opInfo userinfo of operator
     * @return whether succeed
     */
    Boolean bindTag(BindTagRequest request, UserInfo opInfo);

    /**
     * Delete cluster information.
     *
     * @param id cluster id to be deleted
     * @param operator current operator
     * @return whether succeed
     */
    Boolean delete(Integer id, String operator);

    /**
     * Delete cluster information.
     *
     * @param id cluster id to be deleted
     * @param opInfo userinfo of operator
     * @return whether succeed
     */
    Boolean delete(Integer id, UserInfo opInfo);

    /**
     * Delete cluster by cluster name and type
     *
     * @param name cluster name
     * @param type cluster type
     * @param operator current operator
     * @return whether succeed
     */
    Boolean deleteByKey(String name, String type, String operator);

    /**
     * Save cluster node info.
     *
     * @param request inlong cluster info
     * @param operator name of operator
     * @return cluster id after saving
     */
    Integer saveNode(ClusterNodeRequest request, String operator);

    /**
     * Save cluster node info.
     *
     * @param request inlong cluster info
     * @param opInfo userinfo of operator
     * @return cluster id after saving
     */
    Integer saveNode(ClusterNodeRequest request, UserInfo opInfo);

    /**
     * Get cluster node info by id.
     *
     * @param id cluster id
     * @param currentUser current operator
     * @return cluster info
     */
    ClusterNodeResponse getNode(Integer id, String currentUser);

    /**
     * Get cluster node info by id.
     *
     * @param id cluster id
     * @param opInfo userinfo of operator
     * @return cluster info
     */
    ClusterNodeResponse getNode(Integer id, UserInfo opInfo);

    /**
     * Paging query cluster nodes according to conditions.
     *
     * @param request page request conditions
     * @param currentUser current operator
     * @return cluster node list
     */
    PageResult<ClusterNodeResponse> listNode(ClusterPageRequest request, String currentUser);

    /**
     * Query cluster nodes according to conditions.
     *
     * @param request page request conditions
     * @param opInfo userinfo of operator
     * @return cluster node list
     */
    List<ClusterNodeResponse> listNode(ClusterPageRequest request, UserInfo opInfo);

    /**
     * List cluster nodes
     *
     * @param inlongGroupId inlong group id
     * @param clusterType cluster type
     * @param protocolType protocol type, such as: TCP, HTTP
     * @return cluster node list
     */
    List<ClusterNodeResponse> listNodeByGroupId(String inlongGroupId, String clusterType, String protocolType);

    /**
     * List cluster nodes
     *
     * @param inlongGroupId inlong group id
     * @param clusterType cluster type
     * @param protocolType protocol type, such as: TCP, HTTP
     * @param opInfo userinfo of operator
     * @return cluster node list
     */
    List<ClusterNodeResponse> listNodeByGroupId(
            String inlongGroupId, String clusterType, String protocolType, UserInfo opInfo);

    /**
     * Query node IP list by cluster type
     *
     * @param type cluster type
     * @return cluster node ip list
     */
    List<String> listNodeIpByType(String type);

    /**
     * Update cluster node.
     *
     * @param request cluster node to be modified
     * @param operator current operator
     * @return whether succeed
     */
    Boolean updateNode(ClusterNodeRequest request, String operator);

    /**
     * Update cluster node.
     *
     * @param request cluster node to be modified
     * @param opInfo userinfo of operator
     * @return whether succeed
     */
    Boolean updateNode(ClusterNodeRequest request, UserInfo opInfo);

    /**
     * Delete cluster node.
     *
     * @param id cluster node id to be deleted
     * @param operator current operator
     * @return whether succeed
     */
    Boolean deleteNode(Integer id, String operator);

    /**
     * Delete cluster node.
     *
     * @param id cluster node id to be deleted
     * @param opInfo userinfo of operator
     * @return whether succeed
     */
    Boolean deleteNode(Integer id, UserInfo opInfo);

    /**
     * Query data proxy nodes by the given inlong group id and protocol type
     *
     * @param inlongGroupId inlong group id
     * @param protocolType protocol type
     * @return data proxy node response
     */
    DataProxyNodeResponse getDataProxyNodes(String inlongGroupId, String protocolType);

    /**
     * Get the configuration of DataProxy through the cluster name to which DataProxy belongs.
     *
     * @param clusterTag cluster tag
     * @param clusterName cluster name
     * @return data proxy config, includes mq clusters and topics
     */
    DataProxyConfig getDataProxyConfig(String clusterTag, String clusterName);

    /**
     * Get data proxy cluster list by the given cluster name
     *
     * @return data proxy config
     */
    String getAllConfig(String clusterName, String md5);

    /**
     * Test whether the connection can be successfully established.
     *
     * @param request connection request
     * @return true or false
     */
    Boolean testConnection(ClusterRequest request);

}
