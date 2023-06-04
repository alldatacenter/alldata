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

package org.apache.inlong.manager.service.heartbeat;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Scheduler;
import com.google.common.base.Joiner;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.common.enums.NodeSrvStatus;
import org.apache.inlong.common.heartbeat.AbstractHeartbeatManager;
import org.apache.inlong.common.heartbeat.ComponentHeartbeat;
import org.apache.inlong.common.heartbeat.HeartbeatMsg;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.ClusterStatus;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.NodeStatus;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.InlongClusterEntity;
import org.apache.inlong.manager.dao.entity.InlongClusterNodeEntity;
import org.apache.inlong.manager.dao.mapper.InlongClusterEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongClusterNodeEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSourceEntityMapper;
import org.apache.inlong.manager.pojo.cluster.ClusterInfo;
import org.apache.inlong.manager.pojo.cluster.ClusterNodeRequest;
import org.apache.inlong.manager.pojo.cluster.agent.AgentClusterNodeDTO;
import org.apache.inlong.manager.service.cluster.InlongClusterOperator;
import org.apache.inlong.manager.service.cluster.InlongClusterOperatorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Lazy
@Component
public class HeartbeatManager implements AbstractHeartbeatManager {

    private static final String AUTO_REGISTERED = "auto registered";
    private static final Gson GSON = new Gson();

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
    @Autowired
    private StreamSourceEntityMapper sourceMapper;

    /**
     * Check whether the configuration information carried in the heartbeat has been updated
     *
     * @param oldHB last heartbeat msg
     * @param newHB current heartbeat msg
     * @return
     */
    private static boolean heartbeatConfigModified(HeartbeatMsg oldHB, HeartbeatMsg newHB) {
        // todo: only support dynamic renew node tag. Support clusterName/port/ip... later
        if (oldHB == null) {
            return true;
        }
        return !Objects.equals(oldHB.getNodeGroup(), newHB.getNodeGroup()) || !Objects.equals(oldHB.getLoad(),
                newHB.getLoad());
    }

    @PostConstruct
    public void init() {
        // When the manager restarts, set the heartbeat timeout state of all nodes
        // and wait for the heartbeat report of the corresponding node
        clusterNodeMapper.updateStatus(null, NodeStatus.HEARTBEAT_TIMEOUT.getStatus(), NodeStatus.NORMAL.getStatus());
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

        // The expiry time of cluster info cache must be greater than heartbeat cache
        // because the eviction handler needs to query cluster info cache
        clusterInfoCache = Caffeine.newBuilder()
                .expireAfterAccess(expireTime * 2L, TimeUnit.SECONDS)
                .build(this::fetchCluster);
    }

    @SneakyThrows
    @Override
    public void reportHeartbeat(HeartbeatMsg heartbeat) {
        ComponentHeartbeat componentHeartbeat = heartbeat.componentHeartbeat();
        ClusterInfo clusterInfo = clusterInfoCache.get(componentHeartbeat);
        if (clusterInfo == null) {
            log.error("not found any cluster by name={} and type={}", componentHeartbeat.getClusterName(),
                    componentHeartbeat.getComponentType());
            return;
        }

        // if the heartbeat was not in the cache, insert or update the node by the heartbeat info
        HeartbeatMsg lastHeartbeat = heartbeatCache.getIfPresent(componentHeartbeat);

        // protocolType may be null, and the protocolTypes' length may be less than ports' length
        String[] ports = heartbeat.getPort().split(InlongConstants.COMMA);
        String[] ips = heartbeat.getIp().split(InlongConstants.COMMA);
        String protocolType = heartbeat.getProtocolType();
        String[] protocolTypes = null;
        if (StringUtils.isNotBlank(protocolType) && ports.length > 1) {
            protocolTypes = protocolType.split(InlongConstants.COMMA);
            if (protocolTypes.length < ports.length) {
                protocolTypes = null;
            }
        }

        int handlerNum = 0;
        for (int i = 0; i < ports.length; i++) {
            // deep clone the heartbeat
            HeartbeatMsg heartbeatMsg = JsonUtils.parseObject(JsonUtils.toJsonByte(heartbeat), HeartbeatMsg.class);
            assert heartbeatMsg != null;
            heartbeatMsg.setPort(ports[i].trim());
            heartbeatMsg.setIp(ips[i].trim());
            if (protocolTypes != null) {
                heartbeatMsg.setProtocolType(protocolTypes[i]);
            } else {
                heartbeatMsg.setProtocolType(protocolType);
            }
            // uninstall node event
            if (NodeSrvStatus.SERVICE_UNINSTALL.equals(heartbeat.getNodeSrvStatus())) {
                InlongClusterNodeEntity clusterNode = getClusterNode(clusterInfo, heartbeatMsg);
                deleteClusterNode(clusterNode);
                continue;
            }

            if (heartbeatConfigModified(lastHeartbeat, heartbeat)) {
                InlongClusterNodeEntity clusterNode = getClusterNode(clusterInfo, heartbeatMsg);
                if (clusterNode == null) {
                    handlerNum += insertClusterNode(clusterInfo, heartbeatMsg, clusterInfo.getCreator());
                } else {
                    handlerNum += updateClusterNode(clusterNode, heartbeatMsg);
                    // If the agent report succeeds, restore the source status
                    if (Objects.equals(clusterNode.getType(), ClusterType.AGENT)) {
                        // If the agent report succeeds, restore the source status
                        List<Integer> needUpdateIds = sourceMapper.selectHeartbeatTimeoutIds(null, heartbeat.getIp(),
                                heartbeat.getClusterName());
                        // restore state for all source by ip and type
                        if (CollectionUtils.isNotEmpty(needUpdateIds)) {
                            sourceMapper.rollbackTimeoutStatusByIds(needUpdateIds, null);
                        }
                    }
                }
            }
        }

        // if the heartbeat already exists, or does not exist but insert/update success, then put it into the cache
        if (lastHeartbeat == null || handlerNum == ports.length) {
            heartbeatCache.put(componentHeartbeat, heartbeat);
        }
    }

    @SneakyThrows
    private void evictClusterNode(HeartbeatMsg heartbeat) {
        log.debug("evict cluster node");
        ComponentHeartbeat componentHeartbeat = heartbeat.componentHeartbeat();
        ClusterInfo clusterInfo = clusterInfoCache.getIfPresent(componentHeartbeat);
        if (clusterInfo == null) {
            log.error("not found any cluster by name={} and type={}", componentHeartbeat.getClusterName(),
                    componentHeartbeat.getComponentType());
            return;
        }

        // protocolType may be null, and the protocolTypes' length may be less than ports' length
        String[] ports = heartbeat.getPort().split(InlongConstants.COMMA);
        String[] ips = heartbeat.getIp().split(InlongConstants.COMMA);
        String protocolType = heartbeat.getProtocolType();
        String[] protocolTypes = null;
        if (StringUtils.isNotBlank(protocolType) && ports.length > 1) {
            protocolTypes = protocolType.split(InlongConstants.COMMA);
            if (protocolTypes.length < ports.length) {
                protocolTypes = null;
            }
        }

        for (int i = 0; i < ports.length; i++) {
            // deep clone the heartbeat
            HeartbeatMsg heartbeatMsg = JsonUtils.parseObject(JsonUtils.toJsonByte(heartbeat), HeartbeatMsg.class);
            assert heartbeatMsg != null;
            heartbeatMsg.setPort(ports[i].trim());
            heartbeatMsg.setIp(ips[i].trim());
            if (protocolTypes != null) {
                heartbeatMsg.setProtocolType(protocolTypes[i]);
            } else {
                heartbeatMsg.setProtocolType(protocolType);
            }
            InlongClusterNodeEntity clusterNode = getClusterNode(clusterInfo, heartbeatMsg);
            if (clusterNode == null) {
                log.error("not found any cluster node by type={}, ip={}, port={}",
                        heartbeat.getComponentType(), heartbeat.getIp(), heartbeat.getPort());
                return;
            }
            clusterNode.setStatus(NodeStatus.HEARTBEAT_TIMEOUT.getStatus());
            clusterNodeMapper.updateById(clusterNode);
        }
    }

    private InlongClusterNodeEntity getClusterNode(ClusterInfo clusterInfo, HeartbeatMsg heartbeat) {
        ClusterNodeRequest nodeRequest = new ClusterNodeRequest();
        nodeRequest.setParentId(clusterInfo.getId());
        nodeRequest.setType(heartbeat.getComponentType());
        nodeRequest.setIp(heartbeat.getIp());
        nodeRequest.setPort(Integer.valueOf(heartbeat.getPort()));
        nodeRequest.setProtocolType(heartbeat.getProtocolType());
        return clusterNodeMapper.selectByUniqueKey(nodeRequest);
    }

    private int insertClusterNode(ClusterInfo clusterInfo, HeartbeatMsg heartbeat, String creator) {
        InlongClusterNodeEntity clusterNode = new InlongClusterNodeEntity();
        clusterNode.setParentId(clusterInfo.getId());
        clusterNode.setType(heartbeat.getComponentType());
        clusterNode.setIp(heartbeat.getIp());
        clusterNode.setPort(Integer.valueOf(heartbeat.getPort()));
        clusterNode.setProtocolType(heartbeat.getProtocolType());
        clusterNode.setNodeLoad(heartbeat.getLoad());
        clusterNode.setStatus(ClusterStatus.NORMAL.getStatus());
        clusterNode.setCreator(creator);
        clusterNode.setModifier(creator);
        clusterNode.setDescription(AUTO_REGISTERED);
        insertOrUpdateNodeGroup(clusterNode, heartbeat);
        return clusterNodeMapper.insertOnDuplicateKeyUpdate(clusterNode);
    }

    private int updateClusterNode(InlongClusterNodeEntity clusterNode, HeartbeatMsg heartbeat) {
        clusterNode.setStatus(ClusterStatus.NORMAL.getStatus());
        clusterNode.setNodeLoad(heartbeat.getLoad());
        insertOrUpdateNodeGroup(clusterNode, heartbeat);
        return clusterNodeMapper.updateById(clusterNode);
    }

    private void insertOrUpdateNodeGroup(InlongClusterNodeEntity clusterNode, HeartbeatMsg heartbeat) {
        Set<String> groupSet = StringUtils.isBlank(heartbeat.getNodeGroup()) ? new HashSet<>()
                : Arrays.stream(heartbeat.getNodeGroup().split(InlongConstants.COMMA)).collect(Collectors.toSet());
        AgentClusterNodeDTO agentClusterNodeDTO = new AgentClusterNodeDTO();
        if (StringUtils.isNotBlank(clusterNode.getExtParams())) {
            agentClusterNodeDTO = AgentClusterNodeDTO.getFromJson(clusterNode.getExtParams());
            agentClusterNodeDTO.setAgentGroup(Joiner.on(InlongConstants.COMMA).join(groupSet));
        }
        clusterNode.setExtParams(GSON.toJson(agentClusterNodeDTO));
    }

    private int deleteClusterNode(InlongClusterNodeEntity clusterNode) {
        return clusterNodeMapper.deleteById(clusterNode.getId());
    }

    private ClusterInfo fetchCluster(ComponentHeartbeat componentHeartbeat) {
        final String clusterName = componentHeartbeat.getClusterName();
        final String type = componentHeartbeat.getComponentType();
        final String clusterTag = componentHeartbeat.getClusterTag();
        final String extTag = componentHeartbeat.getExtTag();
        Preconditions.expectNotBlank(clusterTag, ErrorCodeEnum.INVALID_PARAMETER, "cluster tag cannot be null");
        Preconditions.expectNotBlank(type, ErrorCodeEnum.INVALID_PARAMETER, "cluster type cannot be null");
        Preconditions.expectNotBlank(clusterName, ErrorCodeEnum.INVALID_PARAMETER, "cluster name cannot be null");
        InlongClusterEntity entity = clusterMapper.selectByNameAndType(clusterName, type);
        if (null != entity) {
            // TODO Load balancing needs to be considered.
            InlongClusterOperator operator = clusterOperatorFactory.getInstance(entity.getType());
            return operator.getFromEntity(entity);
        }

        InlongClusterEntity cluster = new InlongClusterEntity();
        cluster.setName(clusterName);
        cluster.setDisplayName(clusterName);
        cluster.setType(type);
        cluster.setClusterTags(clusterTag);
        cluster.setExtTag(extTag);
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
