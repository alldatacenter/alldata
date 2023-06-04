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

package org.apache.inlong.manager.service.core.heartbeat;

import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.common.enums.ComponentTypeEnum;
import org.apache.inlong.common.heartbeat.HeartbeatMsg;
import org.apache.inlong.common.constant.ProtocolType;
import org.apache.inlong.manager.common.enums.NodeStatus;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.dao.entity.InlongClusterEntity;
import org.apache.inlong.manager.dao.entity.InlongClusterNodeEntity;
import org.apache.inlong.manager.dao.mapper.InlongClusterEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongClusterNodeEntityMapper;
import org.apache.inlong.manager.pojo.cluster.ClusterNodeRequest;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.heartbeat.HeartbeatManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import java.util.List;

@Slf4j
@EnableAutoConfiguration
public class HeartbeatManagerTest extends ServiceBaseTest {

    @Autowired
    private HeartbeatManager heartbeatManager;
    @Autowired
    private InlongClusterEntityMapper clusterMapper;
    @Autowired
    private InlongClusterNodeEntityMapper clusterNodeMapper;

    @Test
    void testReportHeartbeat() throws InterruptedException {
        HeartbeatMsg msg = createHeartbeatMsg();
        heartbeatManager.reportHeartbeat(msg);
        InlongClusterEntity entity = clusterMapper.selectById(1);
        log.info(JsonUtils.toJsonString(entity));
        List<InlongClusterEntity> clusterEntities = clusterMapper.selectByKey(null, msg.getClusterName(),
                msg.getComponentType());
        Assertions.assertEquals(1, clusterEntities.size());
        Assertions.assertEquals(clusterEntities.get(0).getName(), msg.getClusterName());

        int clusterId = clusterEntities.get(0).getId();
        ClusterNodeRequest nodeRequest = new ClusterNodeRequest();
        nodeRequest.setParentId(clusterId);
        nodeRequest.setType(msg.getComponentType());
        nodeRequest.setIp(msg.getIp());
        nodeRequest.setPort(Integer.valueOf(msg.getPort()));
        nodeRequest.setProtocolType(ProtocolType.HTTP);
        nodeRequest.setNodeLoad(0xFFFF);
        InlongClusterNodeEntity clusterNode = clusterNodeMapper.selectByUniqueKey(nodeRequest);
        Assertions.assertNotNull(clusterNode);
        Assertions.assertEquals((int) clusterNode.getStatus(), NodeStatus.NORMAL.getStatus());

        heartbeatManager.getHeartbeatCache().invalidateAll();
        Thread.sleep(1000);

        clusterNode = clusterNodeMapper.selectByUniqueKey(nodeRequest);
        log.debug(JsonUtils.toJsonString(clusterNode));
        Assertions.assertEquals((int) clusterNode.getStatus(), NodeStatus.HEARTBEAT_TIMEOUT.getStatus());

        heartbeatManager.reportHeartbeat(msg);
        clusterNode = clusterNodeMapper.selectByUniqueKey(nodeRequest);
        Assertions.assertEquals((int) clusterNode.getStatus(), NodeStatus.NORMAL.getStatus());
    }

    private HeartbeatMsg createHeartbeatMsg() {
        HeartbeatMsg heartbeatMsg = new HeartbeatMsg();
        heartbeatMsg.setIp("127.0.0.1");
        heartbeatMsg.setPort("46802");
        heartbeatMsg.setClusterTag("default_cluster");
        heartbeatMsg.setProtocolType(ProtocolType.HTTP);
        heartbeatMsg.setComponentType(ComponentTypeEnum.DataProxy.getType());
        heartbeatMsg.setReportTime(System.currentTimeMillis());
        return heartbeatMsg;
    }

}
