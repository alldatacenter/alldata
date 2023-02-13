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

import org.apache.inlong.manager.client.api.impl.InlongClientImpl;
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
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupStatusInfo;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * An interface to manipulate Inlong Cluster
 * <p/>
 * Example:
 *
 * <pre>
 * <code>
 *
 * ClientConfiguration configuration = ..
 * InlongClient client = InlongClient.create(${serviceUrl}, configuration);
 * InlongGroupInfo groupInfo = ..
 * InlongGroup group = client.createGroup(groupInfo);
 * InlongStreamInfo streamInfo = ..
 * InlongStreamBuilder builder = group.createStream(streamInfo);
 * StreamSource source = ..
 * StreamSink sink = ..
 * List StreamField fields = ..
 * InlongStream stream = builder.source(source).sink(sink).fields(fields).init();
 * group.init();
 * </code>
 * </pre>
 */
public interface InlongClient {

    /**
     * Create inlong client.
     *
     * @param serviceUrl the service url
     * @param configuration the configuration
     * @return the inlong client
     */
    static InlongClient create(String serviceUrl, ClientConfiguration configuration) {
        return new InlongClientImpl(serviceUrl, configuration);
    }

    /**
     * Cluster Api for Inlong
     *
     * @return cluster Api
     * @throws Exception
     */
    InlongCluster cluster() throws Exception;

    /**
     * Create inlong group by the given group info
     *
     * @param groupInfo the group info
     * @return the inlong group
     * @throws Exception the exception
     */
    InlongGroup forGroup(InlongGroupInfo groupInfo) throws Exception;

    /**
     * List group list.
     *
     * @return the list
     * @throws Exception the exception
     */
    List<InlongGroup> listGroup(String expr, int status, int pageNum, int pageSize) throws Exception;

    /**
     * List group status
     *
     * @param groupIds inlong group id list
     * @return map of inlong group status list
     * @throws Exception the exception
     */
    Map<String, InlongGroupStatusInfo> listGroupStatus(List<String> groupIds) throws Exception;

    /**
     * List group status
     *
     * @param groupIds inlong group id list
     * @param credentials auth info to query sort task such as sort cluster token
     * @return map of inlong group status list
     * @throws Exception the exception
     */
    Map<String, InlongGroupStatusInfo> listGroupStatus(List<String> groupIds, String credentials) throws Exception;

    /**
     * Gets group.
     *
     * @param groupName the group name
     * @return the group
     * @throws Exception the exception
     */
    InlongGroup getGroup(String groupName) throws Exception;

    /**
     * Save cluster tag.
     *
     * @param request cluster tag
     * @return saved cluster tag id
     */
    Integer saveTag(ClusterTagRequest request);

    /**
     * Get cluster tag by id.
     *
     * @param id cluster tag id
     * @return cluster tag info
     */
    ClusterTagResponse getTag(Integer id);

    /**
     * Paging query cluster tags according to conditions.
     *
     * @param request page request conditions
     * @return cluster tag list
     */
    PageResult<ClusterTagResponse> listTag(ClusterTagPageRequest request);

    /**
     * Update cluster tag.
     *
     * @param request cluster tag to be modified
     * @return whether succeed
     */
    Boolean updateTag(ClusterTagRequest request);

    /**
     * Delete cluster tag.
     *
     * @param id cluster tag id to be deleted
     * @return whether succeed
     */
    Boolean deleteTag(Integer id);

    /**
     * Save component cluster for Inlong.
     *
     * @param request cluster create request
     * @return clusterIndex
     */
    Integer saveCluster(ClusterRequest request);

    /**
     * Get cluster info by id.
     *
     * @param id cluster id
     * @return cluster info
     */
    ClusterInfo get(Integer id);

    /**
     * Paging query clusters according to conditions.
     *
     * @param request query conditions
     * @return cluster list
     */
    PageResult<ClusterInfo> list(ClusterPageRequest request);

    /**
     * Update cluster information.
     *
     * @param request cluster to be modified
     * @return whether succeed
     */
    Boolean update(ClusterRequest request);

    /**
     * Bind or unbind cluster tag for clusters.
     *
     * @param request cluster to be modified
     * @return whether succeed
     */
    Boolean bindTag(BindTagRequest request);

    /**
     * Delete cluster information.
     *
     * @param id cluster id to be deleted
     * @return whether succeed
     */
    Boolean delete(Integer id);

    /**
     * Save cluster node info.
     *
     * @param request cluster info
     * @return id after saving
     */
    Integer saveNode(ClusterNodeRequest request);

    /**
     * Get cluster node info by id.
     *
     * @param id cluster id
     * @return cluster info
     */
    ClusterNodeResponse getNode(Integer id);

    /**
     * Paging query cluster nodes according to conditions.
     *
     * @param request page request conditions
     * @return cluster node list
     */
    PageResult<ClusterNodeResponse> listNode(ClusterPageRequest request);

    /**
     * List cluster nodes
     *
     * @param inlongGroupId inlong group id
     * @param clusterType cluster type
     * @param protocolType protocol type, such as: TCP, HTTP
     * @return cluster node list
     */
    List<ClusterNodeResponse> listNode(String inlongGroupId, String clusterType, @Nullable String protocolType);

    /**
     * Update cluster node.
     *
     * @param request cluster node to be modified
     * @return whether succeed
     */
    Boolean updateNode(ClusterNodeRequest request);

    /**
     * Delete cluster node.
     *
     * @param id cluster node id to be deleted
     * @return whether succeed
     */
    Boolean deleteNode(Integer id);
}
