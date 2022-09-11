/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.master.balance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.tubemq.corebase.cluster.Partition;
import org.apache.inlong.tubemq.server.master.metamanage.MetaDataService;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.GroupResCtrlEntity;
import org.apache.inlong.tubemq.server.master.nodemanage.nodebroker.BrokerRunManager;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.ConsumeGroupInfo;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.ConsumerInfo;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.ConsumerInfoHolder;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.NodeRebInfo;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.RebProcessInfo;
import org.apache.inlong.tubemq.server.master.stats.MasterSrvStatsHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* Load balance class for server side load balance, (partition size) mod (consumer size) */
public class DefaultLoadBalancer implements LoadBalancer {
    private static final Logger logger = LoggerFactory.getLogger(LoadBalancer.class);
    private static final Random RANDOM = new Random(System.currentTimeMillis());

    public DefaultLoadBalancer() {
        // initial information
    }

    /**
     * Load balance
     *
     * @param clusterState
     * @param consumerHolder
     * @param brokerRunManager
     * @param groupSet
     * @param defMetaDataService
     * @param strBuffer
     * @return
     */
    @Override
    public Map<String, Map<String, List<Partition>>> balanceCluster(
            Map<String, Map<String, Map<String, Partition>>> clusterState,
            ConsumerInfoHolder consumerHolder,
            BrokerRunManager brokerRunManager,
            List<String> groupSet,
            MetaDataService defMetaDataService,
            StringBuilder strBuffer) {
        // #lizard forgives
        // load balance according to group
        Map<String/* consumer */,
                Map<String/* topic */, List<Partition>>> finalSubInfoMap =
                new HashMap<>();
        Map<String, RebProcessInfo> rejGroupClientInfoMap = new HashMap<>();
        Set<String> onlineOfflineGroupSet = new HashSet<>();
        Set<String> boundGroupSet = new HashSet<>();
        for (String group : groupSet) {
            if (group == null) {
                continue;
            }
            ConsumeGroupInfo consumeGroupInfo = consumerHolder.getConsumeGroupInfo(group);
            if (consumeGroupInfo == null || consumeGroupInfo.isClientBalance()) {
                continue;
            }
            List<ConsumerInfo> consumerList =
                    consumeGroupInfo.getConsumerInfoList();
            if (CollectionUtils.isEmpty(consumerList)) {
                continue;
            }
            // deal with regular consumer allocation, bound consume not in this part
            if (consumeGroupInfo.isUnReadyServerBalance()) {
                boundGroupSet.add(group);
                continue;
            }
            List<ConsumerInfo> newConsumerList = new ArrayList<>();
            for (ConsumerInfo consumerInfo : consumerList) {
                if (consumerInfo != null) {
                    newConsumerList.add(consumerInfo);
                }
            }
            if (CollectionUtils.isEmpty(newConsumerList)) {
                continue;
            }
            Set<String> topicSet = consumeGroupInfo.getTopicSet();
            if (consumeGroupInfo.needResourceCheck()) {
                // check if current client meet minimal requirements
                GroupResCtrlEntity offsetResetGroupEntity =
                        defMetaDataService.getGroupCtrlConf(group);
                int confAllowBClientRate = (offsetResetGroupEntity != null
                        && offsetResetGroupEntity.getAllowedBrokerClientRate() > 0)
                        ? offsetResetGroupEntity.getAllowedBrokerClientRate() : -2;
                int allowRate = confAllowBClientRate > 0
                        ? confAllowBClientRate : consumerHolder.getDefResourceRate();
                int maxBrokerCount =
                        brokerRunManager.getSubTopicMaxBrokerCount(topicSet);
                int curBClientRate = (int) Math.floor(maxBrokerCount / newConsumerList.size());
                if (curBClientRate > allowRate) {
                    int minClientCnt = maxBrokerCount / allowRate;
                    if (maxBrokerCount % allowRate != 0) {
                        minClientCnt += 1;
                    }
                    consumeGroupInfo.setConsumeResourceInfo(confAllowBClientRate,
                            curBClientRate, minClientCnt, false);
                    if (consumeGroupInfo.isEnableBalanceChkPrint()) {
                        logger.info(strBuffer.append("[UnBound Alloc 2] Not allocate partition :group(")
                                .append(group).append(")'s consumer getCachedSize(")
                                .append(consumeGroupInfo.getGroupCnt())
                                .append(") low than min required client count:")
                                .append(minClientCnt).toString());
                        strBuffer.delete(0, strBuffer.length());
                    }
                    continue;
                } else {
                    consumeGroupInfo.setConsumeResourceInfo(confAllowBClientRate,
                            curBClientRate, -2, true);
                }
            }
            RebProcessInfo rebProcessInfo = new RebProcessInfo();
            if (!consumeGroupInfo.isBalanceMapEmpty()) {
                rebProcessInfo = consumerHolder.getNeedRebNodeList(group);
                if (!rebProcessInfo.isProcessInfoEmpty()) {
                    rejGroupClientInfoMap.put(group, rebProcessInfo);
                }
            }
            List<ConsumerInfo> newConsumerList2 = new ArrayList<>();
            Map<String, Partition> partMap =
                    brokerRunManager.getSubBrokerAcceptSubParts(topicSet);
            Map<String, NodeRebInfo> rebProcessInfoMap = consumeGroupInfo.getBalanceMap();
            for (ConsumerInfo consumer : newConsumerList) {
                Map<String, List<Partition>> partitions = new HashMap<>();
                finalSubInfoMap.put(consumer.getConsumerId(), partitions);
                Map<String, Map<String, Partition>> relation = clusterState.get(consumer.getConsumerId());
                if (relation != null) {
                    // filter client which can not meet requirements
                    if (rebProcessInfo.needProcessList.contains(consumer.getConsumerId())
                            || rebProcessInfo.needEscapeList.contains(consumer.getConsumerId())) {
                        NodeRebInfo tmpNodeRegInfo =
                                rebProcessInfoMap.get(consumer.getConsumerId());
                        if (tmpNodeRegInfo != null
                                && tmpNodeRegInfo.getReqType() == 0) {
                            newConsumerList2.add(consumer);
                        }
                        for (Entry<String, Map<String, Partition>> entry : relation.entrySet()) {
                            partitions.put(entry.getKey(), new ArrayList<>());
                        }
                        continue;
                    }
                    newConsumerList2.add(consumer);
                    for (Entry<String, Map<String, Partition>> entry : relation.entrySet()) {
                        List<Partition> ps = new ArrayList<>();
                        partitions.put(entry.getKey(), ps);
                        Map<String, Partition> partitionMap = entry.getValue();
                        if (partitionMap != null && !partitionMap.isEmpty()) {
                            for (Partition partition : partitionMap.values()) {
                                Partition curPart = partMap.remove(partition.getPartitionKey());
                                if (curPart != null) {
                                    ps.add(curPart);
                                }
                            }
                        }
                    }
                }
            }
            // random allocate
            if (!partMap.isEmpty()) {
                onlineOfflineGroupSet.add(group);
                if (!newConsumerList2.isEmpty()) {
                    this.randomAssign(partMap, newConsumerList2,
                            finalSubInfoMap, clusterState, rebProcessInfo.needProcessList);
                }
            }
        }
        List<String> groupsNeedToBalance = new ArrayList<>();
        if (onlineOfflineGroupSet.isEmpty()) {
            for (String group : groupSet) {
                if (group == null) {
                    continue;
                }
                groupsNeedToBalance.add(group);
            }
        } else {
            for (String group : groupSet) {
                if (group == null) {
                    continue;
                }
                if (!onlineOfflineGroupSet.contains(group)) {
                    groupsNeedToBalance.add(group);
                }
            }
        }
        if (!boundGroupSet.isEmpty()) {
            for (String group : boundGroupSet) {
                groupsNeedToBalance.remove(group);
            }
        }
        if (!groupsNeedToBalance.isEmpty()) {
            balance(finalSubInfoMap, consumerHolder, brokerRunManager,
                    groupsNeedToBalance, clusterState, rejGroupClientInfoMap);
        }
        if (!rejGroupClientInfoMap.isEmpty()) {
            for (Entry<String, RebProcessInfo> entry :
                    rejGroupClientInfoMap.entrySet()) {
                consumerHolder.setRebNodeProcessed(entry.getKey(),
                        entry.getValue().needProcessList);
            }
        }
        return finalSubInfoMap;
    }

    // #lizard forgives
    private void balance(
            Map<String, Map<String, List<Partition>>> clusterState,
            ConsumerInfoHolder consumerHolder,
            BrokerRunManager brokerRunManager,
            List<String> groupSet,
            Map<String, Map<String, Map<String, Partition>>> oldClusterState,
            Map<String, RebProcessInfo> rejGroupClientInfoMap) {
        // according to group
        for (String group : groupSet) {
            ConsumeGroupInfo consumeGroupInfo = consumerHolder.getConsumeGroupInfo(group);
            if (consumeGroupInfo == null || consumeGroupInfo.isClientBalance()) {
                continue;
            }
            // filter consumer which don't need to handle
            List<ConsumerInfo> consumerList = new ArrayList<>();
            List<ConsumerInfo> consumerList1 = consumeGroupInfo.getConsumerInfoList();
            RebProcessInfo rebProcessInfo = rejGroupClientInfoMap.get(group);
            if (rebProcessInfo != null) {
                for (ConsumerInfo consumerInfo : consumerList1) {
                    if (consumerInfo == null) {
                        continue;
                    }
                    if (rebProcessInfo.needProcessList.contains(consumerInfo.getConsumerId())
                            || rebProcessInfo.needEscapeList.contains(consumerInfo.getConsumerId())) {
                        Map<String, List<Partition>> partitions2 =
                                clusterState.computeIfAbsent(
                                        consumerInfo.getConsumerId(), k -> new HashMap<>());
                        Map<String, Map<String, Partition>> relation =
                                oldClusterState.get(consumerInfo.getConsumerId());
                        if (relation != null) {
                            for (String topic : relation.keySet()) {
                                partitions2.put(topic, new ArrayList<Partition>());
                            }
                        }
                        continue;
                    }
                    consumerList.add(consumerInfo);
                }
            } else {
                consumerList = consumerList1;
            }
            if (CollectionUtils.isEmpty(consumerList)) {
                continue;
            }
            // sort consumer and partitions, then mod
            Set<String> topics = consumeGroupInfo.getTopicSet();
            Map<String, Partition> psPartMap =
                    brokerRunManager.getSubBrokerAcceptSubParts(topics);
            int min = psPartMap.size() / consumerList.size();
            int max = psPartMap.size() % consumerList.size() == 0 ? min : min + 1;
            int serverNumToLoadMax = psPartMap.size() % consumerList.size();
            Queue<Partition> partitionToMove = new LinkedBlockingQueue<>();
            Map<String, Integer> serverToTake = new HashMap<>();
            for (ConsumerInfo consumer : consumerList) {
                Map<String, List<Partition>> partitions =
                        clusterState.get(consumer.getConsumerId());
                if (partitions == null) {
                    partitions = new HashMap<>();
                }
                int load = 0;
                for (List<Partition> entry : partitions.values()) {
                    load += entry.size();
                }
                if (load < max) {
                    if (load == 0) {
                        serverToTake.put(consumer.getConsumerId(), max - load);
                    } else if (load < min) {
                        serverToTake.put(consumer.getConsumerId(), max - load);
                    }
                    continue;
                }
                int numToOffload;
                if (serverNumToLoadMax > 0) {
                    serverNumToLoadMax--;
                    numToOffload = load - max;
                } else {
                    numToOffload = load - min;
                }
                // calculate if current consumer partition need to release or add
                for (List<Partition> entry : partitions.values()) {
                    if (entry.size() > numToOffload) {
                        int condition = numToOffload;
                        for (int i = 0; i < condition; i++) {
                            partitionToMove.add(entry.remove(0));
                            numToOffload--;
                        }
                        if (numToOffload <= 0) {
                            break;
                        }
                    } else {
                        numToOffload -= entry.size();
                        partitionToMove.addAll(entry);
                        entry.clear();
                        if (numToOffload <= 0) {
                            break;
                        }
                    }
                }
            }
            // random allocate the rest partition
            for (Entry<String, Integer> entry : serverToTake.entrySet()) {
                for (int i = 0; i < entry.getValue() && partitionToMove.size() > 0; i++) {
                    Partition partition = partitionToMove.poll();
                    assign(partition, clusterState, entry.getKey());
                }
            }
            // load balance partition between consumer
            if (!partitionToMove.isEmpty()) {
                for (String consumerId : serverToTake.keySet()) {
                    if (partitionToMove.isEmpty()) {
                        break;
                    }
                    assign(partitionToMove.poll(), clusterState, consumerId);
                }
            }
        }
    }

    private void assign(Partition partition,
                        Map<String, Map<String, List<Partition>>> clusterState,
                        String consumerId) {
        Map<String, List<Partition>> partitions =
                clusterState.computeIfAbsent(consumerId, k -> new HashMap<>());
        List<Partition> ps = partitions.computeIfAbsent(
                partition.getTopic(), k -> new ArrayList<>());
        ps.add(partition);
    }

    /**
     * Random assign partition
     *
     * @param partitionToAssignMap
     * @param consumerList
     * @param clusterState
     * @param oldClusterState
     * @param filterList
     */
    private void randomAssign(Map<String, Partition> partitionToAssignMap,
                              List<ConsumerInfo> consumerList,
                              Map<String, Map<String, List<Partition>>> clusterState,
                              Map<String, Map<String, Map<String, Partition>>> oldClusterState,
                              List<String> filterList) {
        int searched = 1;
        int consumerSize = consumerList.size();
        for (Partition partition : partitionToAssignMap.values()) {
            ConsumerInfo consumer = null;
            int indeId = RANDOM.nextInt(consumerSize);
            do {
                searched = 1;
                consumer = consumerList.get(indeId);
                if (filterList.contains(consumer.getConsumerId())) {
                    if (consumerList.size() == 1) {
                        searched = 0;
                        break;
                    }
                    Map<String, Map<String, Partition>> oldPartitionMap =
                            oldClusterState.get(consumer.getConsumerId());
                    if (oldPartitionMap != null) {
                        Map<String, Partition> oldPartitions =
                                oldPartitionMap.get(partition.getTopic());
                        if (oldPartitions != null) {
                            if (oldPartitions.get(partition.getPartitionKey()) != null) {
                                searched = 2;
                                indeId = (indeId + 1) % consumerSize;
                            }
                        }
                    }
                }
            } while (searched >= 2);
            if (searched == 0) {
                return;
            }
            Map<String, List<Partition>> partitions =
                    clusterState.computeIfAbsent(consumer.getConsumerId(), k -> new HashMap<>());
            List<Partition> ps = partitions.computeIfAbsent(
                    partition.getTopic(), k -> new ArrayList<>());
            ps.add(partition);
        }
    }

    /**
     * Round robin assign partitions
     *
     * @param partitions
     * @param consumers
     * @return
     */
    @Override
    public Map<String, List<Partition>> roundRobinAssignment(List<Partition> partitions,
                                                             List<String> consumers) {
        if (partitions.isEmpty() || consumers.isEmpty()) {
            return null;
        }
        Map<String, List<Partition>> assignments = new TreeMap<>();
        int numPartitions = partitions.size();
        int numServers = consumers.size();
        int max = (int) Math.ceil((float) numPartitions / numServers);
        int serverIdx = 0;
        if (numServers > 1) {
            serverIdx = RANDOM.nextInt(numServers);
        }
        int partitionIdx = 0;
        for (int j = 0; j < numServers; j++) {
            String server = consumers.get((j + serverIdx) % numServers);
            List<Partition> serverPartitions = new ArrayList<>(max);
            for (int i = partitionIdx; i < numPartitions; i += numServers) {
                serverPartitions.add(partitions.get(i % numPartitions));
            }
            assignments.put(server, serverPartitions);
            partitionIdx++;
        }
        return assignments;
    }

    @Override
    public ConsumerInfo randomAssignment(List<ConsumerInfo> consumers) {
        if (consumers == null || consumers.isEmpty()) {
            logger.warn("Wanted to do random assignment but no servers to assign to");
            return null;
        }

        return consumers.get(RANDOM.nextInt(consumers.size()));
    }

    /**
     * Assign consumer partitions
     *
     * @param consumerHolder
     * @param brokerRunManager
     * @param groupSet
     * @param defMetaDataService
     * @param strBuffer
     * @return
     */
    @Override
    public Map<String, Map<String, List<Partition>>> bukAssign(
            ConsumerInfoHolder consumerHolder,
            BrokerRunManager brokerRunManager,
            List<String> groupSet,
            MetaDataService defMetaDataService,
            StringBuilder strBuffer) {
        // #lizard forgives
        // regular consumer allocate operation
        Map<String, Map<String, List<Partition>>> finalSubInfoMap =
                new HashMap<>();
        for (String group : groupSet) {
            ConsumeGroupInfo consumeGroupInfo =
                    consumerHolder.getConsumeGroupInfo(group);
            if (consumeGroupInfo == null
                    || consumeGroupInfo.isClientBalance()
                    || consumeGroupInfo.isUnReadyServerBalance()) {
                continue;
            }
            List<ConsumerInfo> consumerList = consumeGroupInfo.getConsumerInfoList();
            if (CollectionUtils.isEmpty(consumerList)) {
                continue;
            }
            // check if current client meet minimal requirements
            Set<String> topicSet = consumeGroupInfo.getTopicSet();
            GroupResCtrlEntity offsetResetGroupEntity =
                    defMetaDataService.getGroupCtrlConf(group);
            int confAllowBClientRate = (offsetResetGroupEntity != null
                    && offsetResetGroupEntity.getAllowedBrokerClientRate() > 0)
                    ? offsetResetGroupEntity.getAllowedBrokerClientRate() : -2;
            int allowRate = confAllowBClientRate > 0
                    ? confAllowBClientRate : consumerHolder.getDefResourceRate();
            int maxBrokerCount =
                    brokerRunManager.getSubTopicMaxBrokerCount(topicSet);
            int curBClientRate = (int) Math.floor(maxBrokerCount / consumerList.size());
            if (curBClientRate > allowRate) {
                int minClientCnt = maxBrokerCount / allowRate;
                if (maxBrokerCount % allowRate != 0) {
                    minClientCnt += 1;
                }
                consumeGroupInfo.setConsumeResourceInfo(confAllowBClientRate,
                        curBClientRate, minClientCnt, false);
                if (consumeGroupInfo.isEnableBalanceChkPrint()) {
                    logger.info(strBuffer.append("[UnBound Alloc 1] Not allocate partition :group(")
                            .append(group).append(")'s consumer getCachedSize(")
                            .append(consumeGroupInfo.getGroupCnt())
                            .append(") low than min required client count:")
                            .append(minClientCnt).toString());
                    strBuffer.delete(0, strBuffer.length());
                }
                continue;
            } else {
                consumeGroupInfo.setConsumeResourceInfo(confAllowBClientRate,
                        curBClientRate, -2, true);
            }
            // sort and mod
            Collections.sort(consumerList);
            for (String topic : topicSet) {
                List<Partition> partPubList =
                        brokerRunManager.getSubBrokerAcceptSubParts(topic);
                Collections.sort(partPubList);
                int partsPerConsumer = partPubList.size() / consumerList.size();
                int consumersWithExtraPart = partPubList.size() % consumerList.size();
                for (int i = 0; i < consumerList.size(); i++) {
                    String consumerId = consumerList.get(i).getConsumerId();
                    Map<String, List<Partition>> topicSubPartMap =
                            finalSubInfoMap.computeIfAbsent(consumerId, k -> new HashMap<>());
                    List<Partition> partList =
                            topicSubPartMap.computeIfAbsent(topic, k -> new ArrayList<>());
                    int startIndex = partsPerConsumer * i + Math.min(i, consumersWithExtraPart);
                    int parts = partsPerConsumer + ((i + 1) > consumersWithExtraPart ? 0 : 1);
                    for (int j = startIndex; j < startIndex + parts; j++) {
                        final Partition part = partPubList.get(j);
                        partList.add(part);
                    }
                }
            }
        }
        return finalSubInfoMap;
    }

    /**
     * Reset
     *
     * @param consumerHolder
     * @param brokerRunManager
     * @param groupSet
     * @param defMetaDataService
     * @param strBuffer
     * @return
     */
    @Override
    public Map<String, Map<String, Map<String, Partition>>> resetBukAssign(
            ConsumerInfoHolder consumerHolder, BrokerRunManager brokerRunManager,
            List<String> groupSet, MetaDataService defMetaDataService,
            final StringBuilder strBuffer) {
        return inReBalanceCluster(false, consumerHolder,
                brokerRunManager, groupSet, defMetaDataService, strBuffer);
    }

    /**
     * Reset balance
     *
     * @param clusterState
     * @param consumerHolder
     * @param brokerRunManager
     * @param groupSet
     * @param defMetaDataService
     * @param strBuffer
     * @return
     */
    @Override
    public Map<String, Map<String, Map<String, Partition>>> resetBalanceCluster(
            Map<String, Map<String, Map<String, Partition>>> clusterState,
            ConsumerInfoHolder consumerHolder, BrokerRunManager brokerRunManager,
            List<String> groupSet, MetaDataService defMetaDataService, StringBuilder strBuffer) {
        return inReBalanceCluster(true, consumerHolder,
                brokerRunManager, groupSet, defMetaDataService, strBuffer);
    }

    // #lizard forgives
    private Map<String, Map<String, Map<String, Partition>>> inReBalanceCluster(
            boolean isResetRebalance,
            ConsumerInfoHolder consumerHolder,
            BrokerRunManager brokerRunManager,
            List<String> groupSet,
            MetaDataService defMetaDataService,
            StringBuilder strBuffer) {
        // band consume reset offset
        Map<String, Map<String, Map<String, Partition>>> finalSubInfoMap =
                new HashMap<>();
        // according group
        for (String group : groupSet) {
            if (group == null) {
                continue;
            }
            // filter band consumer
            ConsumeGroupInfo consumeGroupInfo =
                    consumerHolder.getConsumeGroupInfo(group);
            if (consumeGroupInfo == null
                    || consumeGroupInfo.isGroupEmpty()
                    || consumeGroupInfo.isNotNeedBoundBalance()) {
                continue;
            }
            if (!consumeGroupInfo.isGroupFullSize()) {
                // check if client size meet minimal requirements
                Long checkCycle = consumerHolder.addCurCheckCycle(group);
                if (isResetRebalance) {
                    if (checkCycle != null && checkCycle % 15 == 0) {
                        logger.info(strBuffer.append("[Bound Alloc 2] Not allocate partition :group(")
                                .append(group).append(")'s consumer getCachedSize(")
                                .append(consumeGroupInfo.getGroupCnt())
                                .append(") low than required source count:")
                                .append(consumeGroupInfo.getSourceCount())
                                .append(", checked cycle is ")
                                .append(checkCycle).toString());
                        strBuffer.delete(0, strBuffer.length());
                    }
                } else {
                    logger.info(strBuffer.append("[Bound Alloc 1] Not allocate partition :group(")
                            .append(group).append(")'s consumer getCachedSize(")
                            .append(consumeGroupInfo.getGroupCnt())
                            .append(") low than required source count:")
                            .append(consumeGroupInfo.getSourceCount()).toString());
                    strBuffer.delete(0, strBuffer.length());
                }
                continue;
            }
            Map<String, Partition> partPubMap =
                    brokerRunManager.getSubBrokerAcceptSubParts(consumeGroupInfo.getTopicSet());
            Map<String, Partition> partitionMap = new HashMap<>();
            for (Partition partition : partPubMap.values()) {
                partitionMap.put(partition.getPartitionKey(), partition);
            }
            Map<String, String> partsConsumerMap =
                    consumeGroupInfo.getPartitionInfoMap();
            for (Entry<String, String> entry : partsConsumerMap.entrySet()) {
                Partition foundPart = partitionMap.get(entry.getKey());
                if (foundPart != null) {
                    String consumerId = entry.getValue();
                    Map<String, Map<String, Partition>> topicSubPartMap =
                            finalSubInfoMap.computeIfAbsent(consumerId, k -> new HashMap<>());
                    Map<String, Partition> partMap =
                            topicSubPartMap.computeIfAbsent(
                                    foundPart.getTopic(), k -> new HashMap<>());
                    partMap.put(foundPart.getPartitionKey(), foundPart);
                    partitionMap.remove(entry.getKey());
                }
            }
            if (consumeGroupInfo.addAllocatedTimes() > 0) {
                MasterSrvStatsHolder.updSvrBalResetDurations(
                        System.currentTimeMillis() - consumeGroupInfo.getCreateTime());
            }
        }
        return finalSubInfoMap;
    }
}
