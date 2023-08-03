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

import org.apache.inlong.common.constant.MQType;
import org.apache.inlong.common.constant.ProtocolType;
import org.apache.inlong.common.enums.ComponentTypeEnum;
import org.apache.inlong.common.heartbeat.HeartbeatMsg;
import org.apache.inlong.common.pojo.dataproxy.DataProxyNodeInfo;
import org.apache.inlong.common.pojo.dataproxy.DataProxyNodeResponse;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.pojo.cluster.ClusterInfo;
import org.apache.inlong.manager.pojo.cluster.ClusterNodeRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterNodeResponse;
import org.apache.inlong.manager.pojo.cluster.ClusterPageRequest;
import org.apache.inlong.manager.pojo.cluster.dataproxy.DataProxyClusterRequest;
import org.apache.inlong.manager.pojo.cluster.pulsar.PulsarClusterInfo;
import org.apache.inlong.manager.pojo.cluster.pulsar.PulsarClusterRequest;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.UpdateResult;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.heartbeat.HeartbeatManager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Comparator;
import java.util.List;

/**
 * Inlong cluster service test for {@link InlongClusterService}
 */
public class InlongClusterServiceTest extends ServiceBaseTest {

    @Autowired
    private InlongClusterService clusterService;
    @Autowired
    private HeartbeatManager heartbeatManager;

    /**
     * Save data proxy cluster
     */
    public Integer saveDataProxyCluster(String clusterTag, String clusterName, String extTag) {
        DataProxyClusterRequest request = new DataProxyClusterRequest();
        request.setClusterTags(clusterTag);
        request.setName(clusterName);
        request.setType(ClusterType.DATAPROXY);
        request.setExtTag(extTag);
        request.setInCharges(GLOBAL_OPERATOR);
        return clusterService.save(request, GLOBAL_OPERATOR);
    }

    /**
     * Save Pulsar cluster
     */
    public Integer savePulsarCluster(String clusterTag, String clusterName, String adminUrl) {
        PulsarClusterRequest request = new PulsarClusterRequest();
        request.setClusterTags(clusterTag);
        request.setName(clusterName);
        request.setType(ClusterType.PULSAR);
        request.setAdminUrl(adminUrl);
        request.setInCharges(GLOBAL_OPERATOR);
        return clusterService.save(request, GLOBAL_OPERATOR);
    }

    /**
     * List clusters by page.
     */
    public PageResult<ClusterInfo> listCluster(String type, String clusterTag) {
        ClusterPageRequest request = new ClusterPageRequest();
        request.setType(type);
        request.setClusterTag(clusterTag);
        return clusterService.list(request);
    }

    /**
     * Update cluster info.
     */
    public Boolean updatePulsarCluster(Integer id, String clusterType, String name, String clusterTag, String adminUrl,
            Integer version) {
        PulsarClusterRequest request = new PulsarClusterRequest();
        request.setId(id);
        request.setType(clusterType);
        request.setName(name);
        request.setClusterTags(clusterTag);
        request.setAdminUrl(adminUrl);
        request.setInCharges(GLOBAL_OPERATOR);
        request.setVersion(version);
        return clusterService.update(request, GLOBAL_OPERATOR);
    }

    /**
     * Update cluster by unique key.
     */
    public UpdateResult updatePulsarClusterByKey(String name, String clusterType, String clusterTag, String adminUrl,
            Integer version) {
        PulsarClusterRequest request = new PulsarClusterRequest();
        request.setName(name);
        request.setType(clusterType);
        request.setClusterTags(clusterTag);
        request.setAdminUrl(adminUrl);
        request.setInCharges(GLOBAL_OPERATOR);
        request.setVersion(version);
        return clusterService.updateByKey(request, GLOBAL_OPERATOR);
    }

    /**
     * Delete cluster info by id.
     */
    public Boolean deleteCluster(Integer id) {
        return clusterService.delete(id, GLOBAL_OPERATOR);
    }

    /**
     * Delete cluster info by name and type.
     */
    public Boolean deleteClusterByKey(String name, String type) {
        return clusterService.deleteByKey(name, type, GLOBAL_OPERATOR);
    }

    /**
     * Save cluster node info.
     */
    public Integer saveClusterNode(Integer parentId, String type, String ip, Integer port, String protocolType) {
        ClusterNodeRequest request = new ClusterNodeRequest();
        request.setParentId(parentId);
        request.setType(type);
        request.setIp(ip);
        request.setPort(port);
        request.setProtocolType(protocolType);
        return clusterService.saveNode(request, GLOBAL_OPERATOR);
    }

    /**
     * List cluster nodes by page.
     */
    public PageResult<ClusterNodeResponse> listClusterNode(String type, String keyword, Integer parentId) {
        ClusterPageRequest request = new ClusterPageRequest();
        request.setType(type);
        request.setKeyword(keyword);
        request.setParentId(parentId);
        return clusterService.listNode(request, "admin");
    }

    /**
     * Update cluster node info.
     */
    public Boolean updateClusterNode(Integer id, Integer parentId, String clusterType, String ip, Integer port,
            Integer version) {
        ClusterNodeRequest request = new ClusterNodeRequest();
        request.setId(id);
        request.setParentId(parentId);
        request.setType(clusterType);
        request.setIp(ip);
        request.setPort(port);
        request.setVersion(version);
        return clusterService.updateNode(request, GLOBAL_OPERATOR);
    }

    private HeartbeatMsg createHeartbeatMsg(String clusterName, String ip, String port, String type,
            String protocolType) {
        HeartbeatMsg heartbeatMsg = new HeartbeatMsg();
        heartbeatMsg.setIp(ip);
        heartbeatMsg.setPort(port);
        heartbeatMsg.setClusterTag("default_cluster");
        heartbeatMsg.setProtocolType(protocolType);
        heartbeatMsg.setLoad(0xFFFF);
        heartbeatMsg.setComponentType(type);
        heartbeatMsg.setReportTime(System.currentTimeMillis());
        heartbeatMsg.setClusterName(clusterName);
        return heartbeatMsg;
    }

    /**
     * Delete cluster node info.
     */
    public Boolean deleteClusterNode(Integer id) {
        return clusterService.deleteNode(id, GLOBAL_OPERATOR);
    }

    /**
     * test cluster interface.
     */
    @Test
    public void testPulsarCluster() {
        // save cluster
        String clusterName = "default_pulsar";
        String clusterTag = "default_cluster";
        String adminUrl = "http://127.0.0.1:8080";
        Integer id = this.savePulsarCluster(clusterTag, clusterName, adminUrl);
        Assertions.assertNotNull(id);

        // list cluster
        PageResult<ClusterInfo> listCluster = this.listCluster(ClusterType.PULSAR, clusterTag);
        Assertions.assertTrue(listCluster.getList().size() > 0);
        ClusterInfo clusterInfo = listCluster.getList().get(0);
        PulsarClusterInfo pulsarCluster = (PulsarClusterInfo) clusterInfo;
        Assertions.assertEquals(adminUrl, pulsarCluster.getAdminUrl());

        // update cluster
        String clusterNameUpdate = "default_pulsar";
        String clusterTagUpdate = "default_cluster_2";
        String adminUrlUpdate = "http://127.0.0.1:8088";
        Boolean updateSuccess =
                this.updatePulsarCluster(id, ClusterType.PULSAR, clusterNameUpdate, clusterTagUpdate, adminUrlUpdate,
                        pulsarCluster.getVersion());
        Assertions.assertTrue(updateSuccess);

        // save cluster node
        Integer parentId = id;
        String ip = "127.0.0.1";
        Integer port = 8080;
        Integer nodeId = this.saveClusterNode(parentId, ClusterType.PULSAR, ip, port, ProtocolType.HTTP);
        Assertions.assertNotNull(nodeId);

        // list cluster node
        PageResult<ClusterNodeResponse> listNode = this.listClusterNode(ClusterType.PULSAR, ip, id);
        Assertions.assertEquals(listNode.getTotal(), 1);

        // update cluster node
        String ipUpdate = "localhost";
        Integer portUpdate = 8083;
        Integer version = listNode.getList().get(0).getVersion();
        Boolean updateNodeSuccess =
                this.updateClusterNode(nodeId, parentId, ClusterType.PULSAR, ipUpdate, portUpdate, version);
        Assertions.assertTrue(updateNodeSuccess);

        // delete cluster node
        Boolean deleteNodeSuccess = this.deleteClusterNode(nodeId);
        Assertions.assertTrue(deleteNodeSuccess);

        // delete cluster
        Boolean success = this.deleteCluster(id);
        Assertions.assertTrue(success);
    }

    @Test
    public void testPulsarClusterByKey() {
        // save cluster
        String clusterName = "default_pulsar";
        String clusterTag = "default_cluster";
        String adminUrl = "http://127.0.0.1:8080";
        Integer id = this.savePulsarCluster(clusterTag, clusterName, adminUrl);
        Assertions.assertNotNull(id);

        // list cluster
        PageResult<ClusterInfo> listCluster = this.listCluster(ClusterType.PULSAR, clusterTag);
        Assertions.assertTrue(listCluster.getList().size() > 0);
        ClusterInfo clusterInfo = listCluster.getList().get(0);
        PulsarClusterInfo pulsarCluster = (PulsarClusterInfo) clusterInfo;
        Assertions.assertEquals(adminUrl, pulsarCluster.getAdminUrl());

        // update cluster by unique key
        String clusterTagUpdate = "default_cluster_2";
        String adminUrlUpdate = "http://127.0.0.1:8088";
        UpdateResult updateResult =
                this.updatePulsarClusterByKey(clusterName, ClusterType.PULSAR, clusterTagUpdate, adminUrlUpdate,
                        pulsarCluster.getVersion());
        Assertions.assertTrue(updateResult.getSuccess());
        Assertions.assertEquals(pulsarCluster.getVersion() + 1, updateResult.getVersion());
        ClusterInfo afterUpdate = clusterService.getOne(clusterTagUpdate, clusterName, ClusterType.PULSAR);
        Assertions.assertNotNull(afterUpdate);
        PulsarClusterInfo pulsarClusterAfterUpdate = (PulsarClusterInfo) afterUpdate;
        Assertions.assertEquals(pulsarClusterAfterUpdate.getAdminUrl(), adminUrlUpdate);

        // save cluster node
        Integer parentId = id;
        String ip = "127.0.0.1";
        Integer port = 8080;
        Integer nodeId = this.saveClusterNode(parentId, ClusterType.PULSAR, ip, port, ProtocolType.HTTP);
        Assertions.assertNotNull(nodeId);

        // list cluster node
        PageResult<ClusterNodeResponse> listNode = this.listClusterNode(ClusterType.PULSAR, ip, id);
        Assertions.assertEquals(listNode.getTotal(), 1);

        // update cluster node
        String ipUpdate = "localhost";
        Integer portUpdate = 8083;
        Integer version = listNode.getList().get(0).getVersion();
        Boolean updateNodeSuccess =
                this.updateClusterNode(nodeId, parentId, ClusterType.PULSAR, ipUpdate, portUpdate, version);
        Assertions.assertTrue(updateNodeSuccess);

        // delete cluster node
        Boolean deleteNodeSuccess = this.deleteClusterNode(nodeId);
        Assertions.assertTrue(deleteNodeSuccess);

        // delete cluster by unique key
        Boolean success = this.deleteClusterByKey(clusterName, ClusterType.PULSAR);
        Assertions.assertTrue(success);
        Assertions.assertThrows(BusinessException.class,
                () -> clusterService.getOne(clusterTagUpdate, clusterName, ClusterType.PULSAR));

    }

    @Test
    public void testDataProxyCluster() {
        String clusterTag = "default_cluster";
        String clusterName = "test_data_proxy";
        String extTag = "ext_1";

        // save cluster
        Integer id = this.saveDataProxyCluster(clusterTag, clusterName, extTag);
        Assertions.assertNotNull(id);

        // save cluster node
        String ip = "127.0.0.1";
        Integer port1 = 46800;
        Integer nodeId1 = this.saveClusterNode(id, ClusterType.DATAPROXY, ip, port1, ProtocolType.TCP);
        Assertions.assertNotNull(nodeId1);

        Integer port2 = 46801;
        Integer nodeId2 = this.saveClusterNode(id, ClusterType.DATAPROXY, ip, port2, ProtocolType.TCP);
        Assertions.assertNotNull(nodeId2);

        // create an inlong group which use the clusterTag
        String inlongGroupId = "test_cluster_tag_group";
        InlongGroupInfo inlongGroup = super.createInlongGroup(inlongGroupId, MQType.PULSAR);
        InlongGroupInfo updateGroupInfo = groupService.get(inlongGroupId);
        updateGroupInfo.setInlongClusterTag(clusterTag);
        groupService.update(updateGroupInfo.genRequest(), GLOBAL_OPERATOR);

        // report heartbeat
        HeartbeatMsg msg1 = createHeartbeatMsg(clusterName, ip, String.valueOf(port1),
                ComponentTypeEnum.DataProxy.getType(), ProtocolType.TCP);
        heartbeatManager.reportHeartbeat(msg1);
        HeartbeatMsg msg2 = createHeartbeatMsg(clusterName, ip, String.valueOf(port2),
                ComponentTypeEnum.DataProxy.getType(), ProtocolType.TCP);
        heartbeatManager.reportHeartbeat(msg2);
        // get the data proxy nodes, the first port should is p1, second port is p2
        DataProxyNodeResponse nodeResponse = clusterService.getDataProxyNodes(inlongGroupId, ProtocolType.TCP);
        List<DataProxyNodeInfo> nodeInfoList = nodeResponse.getNodeList();
        nodeInfoList.sort(Comparator.comparingInt(DataProxyNodeInfo::getId));
        Assertions.assertEquals(nodeInfoList.size(), 2);
        Assertions.assertEquals(port1, nodeInfoList.get(0).getPort());
        Assertions.assertEquals(port2, nodeInfoList.get(1).getPort());

        // report heartbeat
        HeartbeatMsg msg3 = createHeartbeatMsg(clusterName, ip, String.valueOf(port1),
                ComponentTypeEnum.DataProxy.getType(), ProtocolType.HTTP);
        heartbeatManager.reportHeartbeat(msg3);
        HeartbeatMsg msg4 = createHeartbeatMsg(clusterName, ip, String.valueOf(port2),
                ComponentTypeEnum.DataProxy.getType(), ProtocolType.HTTP);
        heartbeatManager.reportHeartbeat(msg4);
        nodeResponse = clusterService.getDataProxyNodes(inlongGroupId, ProtocolType.HTTP);
        nodeInfoList = nodeResponse.getNodeList();
        nodeInfoList.sort(Comparator.comparingInt(DataProxyNodeInfo::getId));
        Assertions.assertEquals(nodeInfoList.size(), 2);
        Assertions.assertEquals(port1, nodeInfoList.get(0).getPort());
        Assertions.assertEquals(port2, nodeInfoList.get(1).getPort());

        // delete all cluster nodes
        // TODO should query by cluster parent id
        nodeResponse = clusterService.getDataProxyNodes(inlongGroupId, null);
        for (DataProxyNodeInfo nodeInfo : nodeResponse.getNodeList()) {
            this.deleteClusterNode(nodeInfo.getId());
        }

        this.deleteCluster(id);
    }

}
