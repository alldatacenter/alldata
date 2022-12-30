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

package org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.commons.collections.CollectionUtils;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TErrCodeConstants;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.corebase.utils.Tuple2;
import org.apache.inlong.tubemq.server.common.paramcheck.ParamCheckResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumeGroupInfo {
    private static final Logger logger =
            LoggerFactory.getLogger(ConsumeGroupInfo.class);
    private final String groupName;
    private final ConsumeType consumeType;
    private final long createTime;                                        //create time
    private final Set<String> topicSet = new HashSet<>();                 //topic set
    private final Map<String, TreeSet<String>> topicConditions =          //filter condition set
            new HashMap<>();
    private final ReadWriteLock csmInfoRWLock = new ReentrantReadWriteLock();
    private final Map<String, ConsumerInfo> consumerInfoMap =   //consumer info
            new HashMap<>();
    // session key, the same batch consumer have the same session key
    private String sessionKey = "";
    // session start time
    private long sessionTime = TBaseConstants.META_VALUE_UNDEFINED;
    // consumer count(specific by client)
    private int sourceCount = TBaseConstants.META_VALUE_UNDEFINED;
    // select the bigger offset when offset conflict
    private boolean isSelectedBig = true;
    // allocate offset flag
    private final AtomicBoolean notAllocate =
            new AtomicBoolean(true);
    // current check cycle
    private final AtomicLong curCheckCycle = new AtomicLong(0);
    // allocate times
    private final AtomicInteger allocatedTimes = new AtomicInteger(0);
    // partition info
    private final ConcurrentHashMap<String, String> partitionInfoMap =
            new ConcurrentHashMap<>();
    // partition offset
    private final ConcurrentHashMap<String, Long> partOffsetMap =
            new ConcurrentHashMap<>();
    // load balance
    private final ConcurrentHashMap<String, NodeRebInfo> balanceNodeMap =
            new ConcurrentHashMap<>();
    // config broker/client ratio
    private int confResourceRate = TBaseConstants.META_VALUE_UNDEFINED;
    // current broker/client ratio
    private int curResourceRate = TBaseConstants.META_VALUE_UNDEFINED;
    // minimal client count according to above ratio
    private int minReqClientCnt = TBaseConstants.META_VALUE_UNDEFINED;
    // rebalance check status
    private int balanceChkStatus = TBaseConstants.META_VALUE_UNDEFINED;
    // log print flag
    private boolean enableBalanceChkPrint = true;
    //
    private final AtomicLong csmCtrlId =
            new AtomicLong(TBaseConstants.META_VALUE_UNDEFINED);
    private final AtomicLong topicMetaInfoId =
            new AtomicLong(TBaseConstants.META_VALUE_UNDEFINED);
    private final ConcurrentHashMap<String, String> topicMetaInfoMap =
            new ConcurrentHashMap<>();
    private final AtomicLong lastMetaInfoFreshTime =
            new AtomicLong(TBaseConstants.META_VALUE_UNDEFINED);

    /**
     *  Initial a Consume group node information.
     *
     * @param consumer   the consumer of consume group.
     */
    public ConsumeGroupInfo(ConsumerInfo consumer) {
        this.groupName = consumer.getGroupName();
        this.consumeType = consumer.getConsumeType();
        this.topicSet.addAll(consumer.getTopicSet());
        this.topicConditions.putAll(consumer.getTopicConditions());
        this.createTime = System.currentTimeMillis();
    }

    /**
     * Add consumer to consume group
     *
     * @param inConsumer consumer object
     * @param sBuffer    the string buffer
     * @param result     the process result
     * @return           whether the addition is successful
     */
    public boolean addConsumer(ConsumerInfo inConsumer,
                               StringBuilder sBuffer,
                               ParamCheckResult result) {
        try {
            csmInfoRWLock.writeLock().lock();
            if (this.consumerInfoMap.isEmpty()) {
                if (this.consumeType == ConsumeType.CONSUME_BAND) {
                    this.sessionKey = inConsumer.getSessionKey();
                    this.sessionTime = inConsumer.getStartTime();
                    this.sourceCount = inConsumer.getSourceCount();
                    this.isSelectedBig = inConsumer.isSelectedBig();
                    this.curCheckCycle.set(0);
                } else if (this.consumeType == ConsumeType.CONSUME_CLIENT_REB) {
                    this.sourceCount = inConsumer.getSourceCount();
                }
            } else {
                if (!validConsumerInfo(inConsumer, sBuffer, result)) {
                    return false;
                }
                ConsumerInfo curConsumerInfo =
                        consumerInfoMap.get(inConsumer.getConsumerId());
                if (curConsumerInfo != null) {
                    curConsumerInfo.updCurConsumerInfo(inConsumer);
                    result.setCheckData(false);
                    return true;
                }
            }
            this.consumerInfoMap.put(inConsumer.getConsumerId(), inConsumer);
            if (consumeType == ConsumeType.CONSUME_BAND) {
                bookPartitionInfo(inConsumer);
            }
            result.setCheckData(true);
            return true;
        } finally {
            csmInfoRWLock.writeLock().unlock();
        }
    }

    /**
     * Remove consumer
     *
     * @param consumerId consumer id
     * @return  consumer object
     */
    public ConsumerInfo removeConsumer(String consumerId) {
        if (consumerId == null) {
            return null;
        }
        try {
            csmInfoRWLock.writeLock().lock();
            this.balanceNodeMap.remove(consumerId);
            List<String> partKeyList = new ArrayList<>();
            for (Map.Entry<String, String> entry : partitionInfoMap.entrySet()) {
                if (entry.getValue() != null) {
                    if (entry.getValue().equals(consumerId)) {
                        partKeyList.add(entry.getKey());
                    }
                }
            }
            for (String partKey : partKeyList) {
                partitionInfoMap.remove(partKey);
                partOffsetMap.remove(partKey);
            }
            return this.consumerInfoMap.remove(consumerId);
        } finally {
            csmInfoRWLock.writeLock().unlock();
        }
    }

    /**
     * Get consumer group count
     *
     * @return group count
     */
    public int getGroupCnt() {
        try {
            csmInfoRWLock.readLock().lock();
            return this.consumerInfoMap.size();
        } finally {
            csmInfoRWLock.readLock().unlock();
        }
    }

    /**
     * Add node balance info
     *
     * @param clientId        need add client id
     * @param waitDuration    wait duration
     */
    public void addNodeRelInfo(String clientId, int waitDuration) {
        NodeRebInfo nodeRebInfo = this.balanceNodeMap.get(clientId);
        if (nodeRebInfo != null) {
            if (nodeRebInfo.getStatus() == 4) {
                this.balanceNodeMap.remove(clientId);
            } else {
                return;
            }
        }
        try {
            csmInfoRWLock.readLock().lock();
            if (consumerInfoMap.containsKey(clientId)) {
                this.balanceNodeMap.putIfAbsent(clientId,
                        new NodeRebInfo(clientId, waitDuration));
            }
        } finally {
            csmInfoRWLock.readLock().unlock();
        }
    }

    /**
     * Get the client nodes that need to be balanced
     *
     * @return     the client nodes
     */
    public RebProcessInfo getNeedBalanceNodes() {
        List<String> needProcessList = new ArrayList<>();
        List<String> needEscapeList = new ArrayList<>();
        List<String> needRemoved = new ArrayList<>();
        for (NodeRebInfo nodeRebInfo : this.balanceNodeMap.values()) {
            if (nodeRebInfo.getStatus() == 0) {
                nodeRebInfo.setStatus(1);
                needProcessList.add(nodeRebInfo.getClientId());
            } else {
                if (nodeRebInfo.getReqType() == 1
                        && nodeRebInfo.getStatus() == 2) {
                    if (nodeRebInfo.decrAndGetWaitDuration() <= 0) {
                        nodeRebInfo.setStatus(4);
                        needRemoved.add(nodeRebInfo.getClientId());
                    } else {
                        needEscapeList.add(nodeRebInfo.getClientId());
                    }
                }
            }
        }
        for (String clientId : needRemoved) {
            this.balanceNodeMap.remove(clientId);
        }
        return new RebProcessInfo(needProcessList, needEscapeList);
    }

    /**
     * Update the client nodes status that has completed balancing
     *
     * @param processList the client nodes that has completed balancing
     */
    public void setBalanceNodeProcessed(List<String> processList) {
        if (processList == null
                || processList.isEmpty()
                || this.balanceNodeMap.isEmpty()) {
            return;
        }
        List<String> needRemoved = new ArrayList<>();
        for (NodeRebInfo nodeRebInfo : this.balanceNodeMap.values()) {
            if (processList.contains(nodeRebInfo.getClientId())) {
                if (nodeRebInfo.getReqType() == 0) {
                    nodeRebInfo.setStatus(4);
                    needRemoved.add(nodeRebInfo.getClientId());
                } else {
                    nodeRebInfo.setStatus(2);
                }
            }
        }
        for (String clientId : needRemoved) {
            this.balanceNodeMap.remove(clientId);
        }
    }

    public void updCsmFromMaxCtrlId() {
        csmCtrlId.set(System.currentTimeMillis());
    }

    /**
     * Update topic meta information
     *
     * @param newMetaInfoMap  the newly acquired topic metadata
     */
    public void updCsmTopicMetaInfo(Map<String, String> newMetaInfoMap) {
        lastMetaInfoFreshTime.set(System.currentTimeMillis());
        if (newMetaInfoMap == null || newMetaInfoMap.isEmpty()) {
            return;
        }
        String newConfig;
        String curCOnfig;
        boolean isChanged = false;
        Set<String> newTopics = newMetaInfoMap.keySet();
        Set<String> curTopics = topicMetaInfoMap.keySet();
        if (newTopics.size() != curTopics.size()
                || !newTopics.containsAll(curTopics)) {
            isChanged = true;
        } else {
            for (String topicKey : newTopics) {
                newConfig = newMetaInfoMap.get(topicKey);
                curCOnfig = topicMetaInfoMap.get(topicKey);
                if (newConfig == null) {
                    continue;
                }
                if (!newConfig.equals(curCOnfig)) {
                    isChanged = true;
                    break;
                }
            }
        }
        if (isChanged) {
            for (String newTopic : newTopics) {
                topicMetaInfoMap.put(newTopic, newMetaInfoMap.get(newTopic));
            }
            topicMetaInfoId.set(System.currentTimeMillis());
        }
    }

    public boolean isUnReadyServerBalance() {
        return (consumeType == ConsumeType.CONSUME_BAND
                && notAllocate.get()
                && !partitionInfoMap.isEmpty()
                && allocatedTimes.get() < 2);
    }

    public boolean isNotNeedBoundBalance() {
        return (consumeType != ConsumeType.CONSUME_BAND
                || !notAllocate.get()
                || partitionInfoMap.isEmpty()
                || allocatedTimes.get() >= 2);
    }

    public boolean needResourceCheck() {
        return (consumeType == ConsumeType.CONSUME_NORMAL && balanceChkStatus <= 0);
    }

    public ConsumeType getConsumeType() {
        return consumeType;
    }

    public boolean isClientBalance() {
        return consumeType == ConsumeType.CONSUME_CLIENT_REB;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public boolean isNotAllocate() {
        return notAllocate.get();
    }

    public void settAllocated() {
        this.notAllocate.compareAndSet(true, false);
    }

    public int getAllocatedTimes() {
        return allocatedTimes.get();
    }

    public int addAllocatedTimes() {
        return this.allocatedTimes.incrementAndGet();
    }

    public boolean isGroupEmpty() {
        try {
            csmInfoRWLock.readLock().lock();
            return consumerInfoMap.isEmpty();
        } finally {
            csmInfoRWLock.readLock().unlock();
        }
    }

    public boolean isGroupFullSize() {
        try {
            csmInfoRWLock.readLock().lock();
            return this.consumerInfoMap.size() >= this.sourceCount;
        } finally {
            csmInfoRWLock.readLock().unlock();
        }
    }

    public boolean isBalanceMapEmpty() {
        return this.balanceNodeMap.isEmpty();
    }

    public long getSessionTime() {
        return sessionTime;
    }

    public int getSourceCount() {
        return sourceCount;
    }

    public long getCreateTime() {
        return createTime;
    }

    public long getCurCheckCycle() {
        return curCheckCycle.get();
    }

    public long addCurCheckCycle() {
        return curCheckCycle.addAndGet(1);
    }

    /**
     * Set current consumer  broker/client ratio
     *
     * @param confResourceRate configured broker/client ratio
     * @param curResourceRate  current broker/client ratio
     * @param minReqClientCnt  minimal client count
     * @param isRebalanced     Whether balanced
     */
    public void setConsumeResourceInfo(int confResourceRate,
                                       int curResourceRate,
                                       int minReqClientCnt,
                                       boolean isRebalanced) {
        this.confResourceRate = confResourceRate;
        this.curResourceRate = curResourceRate;
        this.minReqClientCnt = minReqClientCnt;
        this.enableBalanceChkPrint = false;
        if (isRebalanced) {
            this.balanceChkStatus = 1;
        } else {
            this.balanceChkStatus = 0;
        }
    }

    public boolean isEnableBalanceChkPrint() {
        return enableBalanceChkPrint;
    }

    public int getConfResourceRate() {
        return confResourceRate;
    }

    public int getCurResourceRate() {
        return curResourceRate;
    }

    public int getMinReqClientCnt() {
        return minReqClientCnt;
    }

    public int getBalanceChkStatus() {
        return balanceChkStatus;
    }

    public Set<String> getTopicSet() {
        return this.topicSet;
    }

    public boolean isSelectedBig() {
        return isSelectedBig;
    }

    public Map<String, TreeSet<String>> getTopicConditions() {
        return this.topicConditions;
    }

    public long getCsmFromMaxCtrlId() {
        return csmCtrlId.get();
    }

    public Tuple2<Long, List<String>> getTopicMetaInfo() {
        List<String> topicMetaInfoList = new ArrayList<>();
        for (String metaInfo : topicMetaInfoMap.values()) {
            if (TStringUtils.isBlank(metaInfo)) {
                continue;
            }
            topicMetaInfoList.add(metaInfo);
        }
        return new Tuple2<>(topicMetaInfoId.get(), topicMetaInfoList);
    }

    public AtomicLong getLastMetaInfoFreshTime() {
        return lastMetaInfoFreshTime;
    }

    public List<ConsumerInfo> getConsumerInfoList() {
        try {
            csmInfoRWLock.readLock().lock();
            return new ArrayList<>(this.consumerInfoMap.values());
        } finally {
            csmInfoRWLock.readLock().unlock();
        }
    }

    public List<String> getConsumerIdList() {
        try {
            csmInfoRWLock.readLock().lock();
            return new ArrayList<>(this.consumerInfoMap.keySet());
        } finally {
            csmInfoRWLock.readLock().unlock();
        }
    }

    public ConsumerInfo getConsumerInfo(String consumerId) {
        try {
            csmInfoRWLock.readLock().lock();
            return consumerInfoMap.get(consumerId);
        } finally {
            csmInfoRWLock.readLock().unlock();
        }
    }

    public List<String> getConsumerViewInfos() {
        List<String> result = new ArrayList<>();
        try {
            csmInfoRWLock.readLock().lock();
            for (ConsumerInfo consumerInfo : this.consumerInfoMap.values()) {
                if (consumerInfo == null) {
                    continue;
                }
                result.add(consumerInfo.getConsumerViewInfo());
            }
        } finally {
            csmInfoRWLock.readLock().unlock();
        }
        return result;
    }

    public List<Tuple2<String, Boolean>> getConsumerIdAndTlsInfos() {
        List<Tuple2<String, Boolean>> result = new ArrayList<>();
        try {
            csmInfoRWLock.readLock().lock();
            for (ConsumerInfo consumerInfo : this.consumerInfoMap.values()) {
                if (consumerInfo == null) {
                    continue;
                }
                result.add(consumerInfo.getConsumerIdAndTlsInfoTuple());
            }
        } finally {
            csmInfoRWLock.readLock().unlock();
        }
        return result;
    }

    public Map<String, String> getPartitionInfoMap() {
        return this.partitionInfoMap;
    }

    public Map<String, Long> getPartOffsetMap() {
        return this.partOffsetMap;
    }

    public Map<String, NodeRebInfo> getBalanceMap() {
        return this.balanceNodeMap;
    }

    /**
     * book bound report partition information
     *
     * @param consumer consumer info
     */
    private void bookPartitionInfo(ConsumerInfo consumer) {
        if (consumeType != ConsumeType.CONSUME_BAND) {
            return;
        }
        Map<String, Long> consumerPartMap = consumer.getRequiredPartition();
        if (consumerPartMap == null || consumerPartMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Long> entry : consumerPartMap.entrySet()) {
            String oldClientId = this.partitionInfoMap.get(entry.getKey());
            if (oldClientId == null) {
                this.partitionInfoMap.put(entry.getKey(), consumer.getConsumerId());
                this.partOffsetMap.put(entry.getKey(), entry.getValue());
            } else {
                ConsumerInfo oldConsumerInfo = this.consumerInfoMap.get(oldClientId);
                if (oldConsumerInfo == null) {
                    this.partitionInfoMap.put(entry.getKey(), consumer.getConsumerId());
                    this.partOffsetMap.put(entry.getKey(), entry.getValue());
                } else {
                    Map<String, Long> oldConsumerPartMap = oldConsumerInfo.getRequiredPartition();
                    if (oldConsumerPartMap == null || oldConsumerPartMap.isEmpty()) {
                        this.partitionInfoMap.put(entry.getKey(), consumer.getConsumerId());
                        this.partOffsetMap.put(entry.getKey(), entry.getValue());
                    } else {
                        Long oldConsumerOff = oldConsumerPartMap.get(entry.getKey());
                        if (oldConsumerOff == null) {
                            this.partitionInfoMap.put(entry.getKey(), consumer.getConsumerId());
                            this.partOffsetMap.put(entry.getKey(), entry.getValue());
                        } else {
                            if (this.isSelectedBig) {
                                if (entry.getValue() >= oldConsumerOff) {
                                    this.partitionInfoMap.put(entry.getKey(), consumer.getConsumerId());
                                    this.partOffsetMap.put(entry.getKey(), entry.getValue());
                                }
                            } else {
                                if (entry.getValue() < oldConsumerOff) {
                                    this.partitionInfoMap.put(entry.getKey(), consumer.getConsumerId());
                                    this.partOffsetMap.put(entry.getKey(), entry.getValue());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Check the validity of consumer's parameters
     *
     * @param inConsumer consumer info
     * @param sBuffer string buffer
     * @param result process result
     * @return true if valid, or false if invalid
     */
    private boolean validConsumerInfo(ConsumerInfo inConsumer,
                                      StringBuilder sBuffer,
                                      ParamCheckResult result) {
        // check whether the consumer behavior is consistent
        if (inConsumer.getConsumeType() != this.consumeType) {
            sBuffer.append("[Inconsistency subscribe] ").append(inConsumer.getConsumerId())
                    .append(" using ").append(inConsumer.getConsumeType().getName())
                    .append(" subscribe is inconsistency with other consumers using ")
                    .append(this.consumeType.getName())
                    .append(" subscribe in the group");
            result.setCheckResult(false,
                    TErrCodeConstants.CLIENT_INCONSISTENT_CONSUMETYPE, sBuffer.toString());
            logger.warn(sBuffer.toString());
            sBuffer.delete(0, sBuffer.length());
            return false;
        }
        // check the topics of consumption
        if (CollectionUtils.isNotEmpty(topicSet)
                && (topicSet.size() != inConsumer.getTopicSet().size()
                || !topicSet.containsAll(inConsumer.getTopicSet()))) {
            sBuffer.append("[Inconsistency subscribe] ").append(inConsumer.getConsumerId())
                    .append(" subscribed topics ").append(inConsumer.getTopicSet())
                    .append(" is inconsistency with other consumers in the group, existedTopics: ")
                    .append(topicSet);
            result.setCheckResult(false,
                    TErrCodeConstants.CLIENT_INCONSISTENT_TOPICSET, sBuffer.toString());
            logger.warn(sBuffer.toString());
            sBuffer.delete(0, sBuffer.length());
            return false;
        }
        // check the topic conditions of consumption
        boolean isCondEqual = true;
        if (topicConditions.isEmpty()) {
            if (!inConsumer.getTopicConditions().isEmpty()) {
                isCondEqual = false;
                sBuffer.append("[Inconsistency subscribe] ").append(inConsumer.getConsumerId())
                        .append(" subscribe with filter condition ")
                        .append(inConsumer.getTopicConditions())
                        .append(" is inconsistency with other consumers in the group: topic without conditions");
            }
        } else {
            // check the filter conditions of the topic
            if (inConsumer.getTopicConditions().isEmpty()) {
                isCondEqual = false;
                sBuffer.append("[Inconsistency subscribe] ").append(inConsumer.getConsumerId())
                        .append(" subscribe without filter condition ")
                        .append(" is inconsistency with other consumers in the group, existed topic conditions is ")
                        .append(topicConditions);
            } else {
                Set<String> existedCondTopics = topicConditions.keySet();
                Set<String> reqCondTopics = inConsumer.getTopicConditions().keySet();
                if (existedCondTopics.size() != reqCondTopics.size()
                        || !existedCondTopics.containsAll(reqCondTopics)) {
                    isCondEqual = false;
                    sBuffer.append("[Inconsistency subscribe] ").append(inConsumer.getConsumerId())
                            .append(" subscribe with filter condition ")
                            .append(inConsumer.getTopicConditions())
                            .append(" is inconsistency with other consumers in the group, existed topic conditions is ")
                            .append(topicConditions);
                } else {
                    for (String topicKey : existedCondTopics) {
                        if ((topicConditions.get(topicKey).size()
                                != inConsumer.getTopicConditions().get(topicKey).size())
                                || (!topicConditions.get(topicKey).containsAll(
                                inConsumer.getTopicConditions().get(topicKey)))) {
                            isCondEqual = false;
                            sBuffer.append("[Inconsistency subscribe] ")
                                    .append(inConsumer.getConsumerId())
                                    .append(" subscribe with filter condition ")
                                    .append(inConsumer.getTopicConditions())
                                    .append(" is inconsistency with other consumers in the group,")
                                    .append(" existed topic conditions is ")
                                    .append(topicConditions);
                            break;
                        }
                    }
                }
            }
        }
        if (!isCondEqual) {
            result.setCheckResult(false,
                    TErrCodeConstants.CLIENT_INCONSISTENT_FILTERSET, sBuffer.toString());
            logger.warn(sBuffer.toString());
            return false;
        }
        // Check the validity of bound consumer's parameters
        if (this.consumeType == ConsumeType.CONSUME_BAND) {
            if (!validBoundParameters(inConsumer, sBuffer, result)) {
                return false;
            }
        } else if (this.consumeType == ConsumeType.CONSUME_CLIENT_REB) {
            if (this.sourceCount > 0) {
                if (this.sourceCount != inConsumer.getSourceCount()) {
                    sBuffer.append("[Inconsistency subscribe] ").append(inConsumer.getConsumerId())
                            .append("'s sourceCount is inconsistency with other consumers in the group, required is ")
                            .append(sourceCount).append(", request is ").append(inConsumer.getSourceCount());
                    result.setCheckResult(false,
                            TErrCodeConstants.CLIENT_INCONSISTENT_SOURCECOUNT, sBuffer.toString());
                    logger.warn(sBuffer.toString());
                    return false;
                }
                boolean foundEqual = false;
                String occupiedConsumerId = null;
                for (ConsumerInfo consumerInfo : consumerInfoMap.values()) {
                    if (consumerInfo == null) {
                        continue;
                    }
                    if (consumerInfo.getNodeId() == inConsumer.getNodeId()
                            && !consumerInfo.getConsumerId().equals(inConsumer.getConsumerId())) {
                        foundEqual = true;
                        occupiedConsumerId = consumerInfo.getConsumerId();
                        break;
                    }
                }
                if (foundEqual) {
                    sBuffer.append("[Inconsistency subscribe] ").append(inConsumer.getConsumerId())
                            .append("'s nodeId value(").append(inConsumer.getNodeId())
                            .append(") is occupied by ").append(occupiedConsumerId)
                            .append(" in the group!");
                    result.setCheckResult(false,
                            TErrCodeConstants.CLIENT_DUPLICATE_INDEXID, sBuffer.toString());
                    logger.warn(sBuffer.toString());
                    return false;
                }
            }
        }
        result.setCheckData("Ok");
        return true;
    }

    /**
     * Check the validity of bound consumer's parameters
     *
     * @param inConsumer consumer info
     * @param sBuffer string buffer
     * @param result process result
     * @return true if valid, or false if invalid
     */
    private boolean validBoundParameters(ConsumerInfo inConsumer,
                                         StringBuilder sBuffer,
                                         ParamCheckResult result) {
        if (consumeType != ConsumeType.CONSUME_BAND) {
            result.setCheckData("");
            return true;
        }
        // If the sessionKey is inconsistent, it means that the previous round of consumption has not completely
        // exited. In order to avoid the incomplete offset setting, it is necessary to completely clear the above
        // data before resetting and consuming this round of consumption
        if (!sessionKey.equals(inConsumer.getSessionKey())) {
            sBuffer.append("[Inconsistency subscribe] ").append(inConsumer.getConsumerId())
                    .append("'s sessionKey is inconsistency with other consumers in the group, required is ")
                    .append(sessionKey).append(", request is ").append(inConsumer.getSessionKey());
            result.setCheckResult(false,
                    TErrCodeConstants.CLIENT_INCONSISTENT_SESSIONKEY, sBuffer.toString());
            logger.warn(sBuffer.toString());
            return false;
        }
        // check the offset config
        if (isSelectedBig != inConsumer.isSelectedBig()) {
            sBuffer.append("[Inconsistency subscribe] ").append(inConsumer.getConsumerId())
                    .append("'s isSelectBig is inconsistency with other consumers in the group, required is ")
                    .append(isSelectedBig).append(", request is ").append(inConsumer.isSelectedBig());
            result.setCheckResult(false,
                    TErrCodeConstants.CLIENT_INCONSISTENT_SELECTBIG, sBuffer.toString());
            logger.warn(sBuffer.toString());
            return false;
        }
        // check the consumers count
        if (sourceCount != inConsumer.getSourceCount()) {
            sBuffer.append("[Inconsistency subscribe] ").append(inConsumer.getConsumerId())
                    .append("'s sourceCount is inconsistency with other consumers in the group, required is ")
                    .append(sourceCount).append(", request is ").append(inConsumer.getSourceCount());
            result.setCheckResult(false,
                    TErrCodeConstants.CLIENT_INCONSISTENT_SOURCECOUNT, sBuffer.toString());
            logger.warn(sBuffer.toString());
            return false;
        }
        result.setCheckData("Ok");
        return true;
    }
}
