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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Scheduler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.common.heartbeat.AbstractHeartbeatManager;
import org.apache.inlong.common.heartbeat.ComponentHeartbeat;
import org.apache.inlong.common.heartbeat.HeartbeatMsg;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ClusterStatus;
import org.apache.inlong.manager.common.enums.NodeStatus;
import org.apache.inlong.manager.dao.entity.InlongClusterEntity;
import org.apache.inlong.manager.dao.entity.InlongClusterNodeEntity;
import org.apache.inlong.manager.dao.mapper.InlongClusterEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongClusterNodeEntityMapper;
import org.apache.inlong.manager.pojo.cluster.ClusterInfo;
import org.apache.inlong.manager.pojo.cluster.ClusterNodeRequest;
import org.apache.inlong.manager.service.cluster.InlongClusterOperator;
import org.apache.inlong.manager.service.cluster.InlongClusterOperatorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class HeartbeatManager implements AbstractHeartbeatManager {

    private static final String AUTO_REGISTERED = "auto registered";

    @Getter
    private Cache<ComponentHeartbeat, HeartbeatMsg> heartbeatCache;
    @Getter
    private LoadingCache<ComponentHeartbeat, ClusterInfo> clusterInfoCache;
    @Autowired
    private InlongClusterOperatorFactory clusterOperatorFactory;
    @Autowired
    private InlongClusterEntityMapper clusterMapper;
    @Autowired
    private InlongClusterNodeEntityMapper clusterNodeMapper;

    @PostConstruct
    public void init() {
        long expireTime = heartbeatInterval() * 2L;
        Scheduler evictScheduler = Scheduler.forScheduledExecutorService(Executors.newSingleThreadScheduledExecutor());
        heartbeatCache = Caffeine.newBuilder()
                .scheduler(evictScheduler)
                .expireAfterAccess(expireTime, TimeUnit.SECONDS)
                .removalListener((ComponentHeartbeat k, HeartbeatMsg msg, RemovalCause c) -> {
                    if ((c.wasEvicted() || c == RemovalCause.EXPLICIT) && msg != null) {
                        evictClusterNode(msg);
                    }
                }).build();

        // The expire time of cluster info cache must be greater than heartbeat cache
        // because the eviction handler needs to query cluster info cache
        clusterInfoCache = Caffeine.newBuilder()
                .expireAfterAccess(expireTime * 2L, TimeUnit.SECONDS)
                .build(this::fetchCluster);
    }

    @Override
    public void reportHeartbeat(HeartbeatMsg heartbeat) {
        ComponentHeartbeat componentHeartbeat = heartbeat.componentHeartbeat();
        ClusterInfo clusterInfo = clusterInfoCache.get(componentHeartbeat);
        if (clusterInfo == null) {
            log.error("not found any cluster by name={} and type={}", componentHeartbeat.getClusterName(),
                    componentHeartbeat.getComponentType());
            return;
        }
        HeartbeatMsg lastHeartbeat = heartbeatCache.getIfPresent(componentHeartbeat);
        if (lastHeartbeat == null) {
            InlongClusterNodeEntity clusterNode = getClusterNode(clusterInfo, heartbeat);
            if (clusterNode == null) {
                insertClusterNode(clusterInfo, heartbeat, clusterInfo.getCreator());
                log.info("insert node success");
            } else {
                updateClusterNode(clusterNode);
                log.info("update node success");
            }
        }
        heartbeatCache.put(componentHeartbeat, heartbeat);
    }

    private void evictClusterNode(HeartbeatMsg heartbeat) {
        log.debug("evict cluster node");
        ComponentHeartbeat componentHeartbeat = heartbeat.componentHeartbeat();
        ClusterInfo clusterInfo = clusterInfoCache.getIfPresent(componentHeartbeat);
        if (clusterInfo == null) {
            log.error("not found any cluster by name={} and type={}", componentHeartbeat.getClusterName(),
                    componentHeartbeat.getComponentType());
            return;
        }
        InlongClusterNodeEntity clusterNode = getClusterNode(clusterInfo, heartbeat);
        if (clusterNode == null) {
            log.error("not found any cluster node by type={}, ip={}, port={}",
                    heartbeat.getComponentType(), heartbeat.getIp(), heartbeat.getPort());
            return;
        }
        clusterNode.setStatus(NodeStatus.HEARTBEAT_TIMEOUT.getStatus());
        clusterNodeMapper.updateById(clusterNode);
    }

    private InlongClusterNodeEntity getClusterNode(ClusterInfo clusterInfo, HeartbeatMsg heartbeat) {
        ClusterNodeRequest nodeRequest = new ClusterNodeRequest();
        nodeRequest.setParentId(clusterInfo.getId());
        nodeRequest.setType(heartbeat.getComponentType());
        nodeRequest.setIp(heartbeat.getIp());
        nodeRequest.setPort(heartbeat.getPort());
        return clusterNodeMapper.selectByUniqueKey(nodeRequest);
    }

    private void insertClusterNode(ClusterInfo clusterInfo, HeartbeatMsg heartbeat, String creator) {
        InlongClusterNodeEntity clusterNode = new InlongClusterNodeEntity();
        clusterNode.setParentId(clusterInfo.getId());
        clusterNode.setType(heartbeat.getComponentType());
        clusterNode.setIp(heartbeat.getIp());
        clusterNode.setPort(heartbeat.getPort());
        clusterNode.setStatus(ClusterStatus.NORMAL.getStatus());
        clusterNode.setCreator(creator);
        clusterNode.setModifier(creator);
        clusterNode.setDescription(AUTO_REGISTERED);
        clusterNodeMapper.insertOnDuplicateKeyUpdate(clusterNode);
    }

    private void updateClusterNode(InlongClusterNodeEntity clusterNode) {
        clusterNode.setStatus(ClusterStatus.NORMAL.getStatus());
        clusterNodeMapper.updateById(clusterNode);
    }

    private ClusterInfo fetchCluster(ComponentHeartbeat componentHeartbeat) {
        final String clusterName = componentHeartbeat.getClusterName();
        final String type = componentHeartbeat.getComponentType();
        final String clusterTag = componentHeartbeat.getClusterTag();
        InlongClusterEntity entity = clusterMapper.selectByNameAndType(clusterName, type);
        if (null != entity) {
            // TODO Load balancing needs to be considered.
            InlongClusterOperator operator = clusterOperatorFactory.getInstance(entity.getType());
            return operator.getFromEntity(entity);
        }

        InlongClusterEntity cluster = new InlongClusterEntity();
        cluster.setName(clusterName);
        cluster.setType(type);
        cluster.setClusterTags(clusterTag);
        String inCharges = componentHeartbeat.getInCharges();
        if (StringUtils.isBlank(inCharges)) {
            inCharges = InlongConstants.ADMIN_USER;
        }
        String creator = inCharges.split(InlongConstants.COMMA)[0];
        cluster.setInCharges(inCharges);
        cluster.setCreator(creator);
        cluster.setModifier(creator);
        cluster.setStatus(ClusterStatus.NORMAL.getStatus());
        cluster.setDescription(AUTO_REGISTERED);
        clusterMapper.insertOnDuplicateKeyUpdate(cluster);

        InlongClusterOperator operator = clusterOperatorFactory.getInstance(cluster.getType());
        ClusterInfo clusterInfo = operator.getFromEntity(cluster);

        log.debug("success to fetch cluster for heartbeat: {}", componentHeartbeat);
        return clusterInfo;
    }
}
