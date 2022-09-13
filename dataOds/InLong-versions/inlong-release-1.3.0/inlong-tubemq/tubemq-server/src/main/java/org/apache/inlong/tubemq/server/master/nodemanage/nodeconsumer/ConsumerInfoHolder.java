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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.inlong.tubemq.corebase.utils.ConcurrentHashSet;
import org.apache.inlong.tubemq.corebase.utils.Tuple2;
import org.apache.inlong.tubemq.server.common.paramcheck.ParamCheckResult;
import org.apache.inlong.tubemq.server.common.utils.RowLock;
import org.apache.inlong.tubemq.server.master.MasterConfig;
import org.apache.inlong.tubemq.server.master.TMaster;
import org.apache.inlong.tubemq.server.master.stats.MasterSrvStatsHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerInfoHolder {

    private static final Logger logger =
            LoggerFactory.getLogger(ConsumerInfoHolder.class);
    private final MasterConfig masterConfig;     // master configure
    private final RowLock groupRowLock;    //lock
    private final ConcurrentHashMap<String/* group */, ConsumeGroupInfo> groupInfoMap =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String/* consumerId */, String/* group */> consumerIndexMap =
            new ConcurrentHashMap<>();
    private final ConcurrentHashSet<String/* group */> serverBalanceGroupSet =
            new ConcurrentHashSet<>();
    private final ConcurrentHashSet<String/* group */> clientBalanceGroupSet =
            new ConcurrentHashSet<>();
    // topic-group map
    private final ConcurrentHashMap<String/* topic */, ConcurrentHashSet<String>> topicGroupMap
            = new ConcurrentHashMap<>();

    public ConsumerInfoHolder(TMaster tMaster) {
        this.masterConfig = tMaster.getMasterConfig();
        this.groupRowLock = new RowLock("Group-RowLock",
                this.masterConfig.getRowLockWaitDurMs());
    }

    public int getDefResourceRate() {
        return masterConfig.getMaxGroupBrokerConsumeRate();
    }

    /**
     * Judge whether the consumer group is empty
     *
     * @param groupName group name
     * @return true: empty, false: not empty
     */
    public boolean isConsumeGroupEmpty(String groupName) {
        if (groupName == null) {
            return true;
        }
        ConsumeGroupInfo consumeGroupInfo = groupInfoMap.get(groupName);
        return (consumeGroupInfo == null || consumeGroupInfo.isGroupEmpty());
    }

    /**
     * Get consumer id list in a group
     *
     * @param group the consumer group name
     * @return the consumer id list of the group
     */
    public List<String> getConsumerIdList(String group) {
        if (group == null) {
            return Collections.emptyList();
        }
        ConsumeGroupInfo consumeGroupInfo = groupInfoMap.get(group);
        if (consumeGroupInfo == null) {
            return Collections.emptyList();
        }
        return consumeGroupInfo.getConsumerIdList();
    }

    /**
     * Get the client information of the consumer group
     *
     * The query content of this API is the content presented when
     *  the Web API queries the client information.
     *
     * @param group the consumer group name
     * @return the consumer id with subscribed topic and link type of the group
     */
    public List<String> getConsumerViewList(String group) {
        if (group == null) {
            return Collections.emptyList();
        }
        ConsumeGroupInfo consumeGroupInfo = groupInfoMap.get(group);
        if (consumeGroupInfo == null) {
            return Collections.emptyList();
        }
        return consumeGroupInfo.getConsumerViewInfos();
    }

    /**
     * Get the consumerId and tls information of the consumer group
     *
     * include consumerId and isOverTLS information.
     *
     * @param group the consumer group name
     * @return the consumer info of the group
     */
    public List<Tuple2<String, Boolean>> getConsumerIdAndTlsInfos(String group) {
        if (group == null) {
            return Collections.emptyList();
        }
        ConsumeGroupInfo consumeGroupInfo = groupInfoMap.get(group);
        if (consumeGroupInfo == null) {
            return Collections.emptyList();
        }
        return consumeGroupInfo.getConsumerIdAndTlsInfos();
    }

    /**
     * Get consume group information
     *
     * @param group group name
     * @return consume group information
     */
    public ConsumeGroupInfo getConsumeGroupInfo(String group) {
        if (group == null) {
            return null;
        }
        return groupInfoMap.get(group);
    }

    /**
     * Add current check cycle
     *
     * @param group group name
     * @return updated check cycle value
     */
    public Long addCurCheckCycle(String group) {
        if (group == null) {
            return null;
        }
        ConsumeGroupInfo consumeGroupInfo = groupInfoMap.get(group);
        if (consumeGroupInfo != null) {
            return consumeGroupInfo.addCurCheckCycle();
        }
        return null;
    }

    /**
     * get subscribed topic set of group
     *
     * @param group group name
     * @return subscribed topic set
     */
    public Set<String> getGroupTopicSet(String group) {
        if (group == null) {
            return Collections.emptySet();
        }
        ConsumeGroupInfo consumeGroupInfo = groupInfoMap.get(group);
        if (consumeGroupInfo != null) {
            return consumeGroupInfo.getTopicSet();
        }
        return Collections.emptySet();
    }

    /**
     * get current consumer count of group
     *
     * @param group group name
     * @return consumer count
     */
    public int getConsumerCnt(String group) {
        int count = 0;
        if (group != null) {
            ConsumeGroupInfo consumeGroupInfo = groupInfoMap.get(group);
            if (consumeGroupInfo != null) {
                count = consumeGroupInfo.getGroupCnt();
            }
        }
        return count;
    }

    /**
     * get need rebalanced consumer of group
     *
     * @param group group name
     * @return need rebalanced consumer
     */
    public RebProcessInfo getNeedRebNodeList(String group) {
        RebProcessInfo rebProcessInfo = new RebProcessInfo();
        if (group == null) {
            return rebProcessInfo;
        }
        ConsumeGroupInfo consumeGroupInfo = groupInfoMap.get(group);
        if (consumeGroupInfo != null) {
            rebProcessInfo = consumeGroupInfo.getNeedBalanceNodes();
        }
        return rebProcessInfo;
    }

    /**
     * set rebalanced consumer id of group
     *
     * @param group group name
     * @param processList rebalanced consumer id
     */
    public void setRebNodeProcessed(String group,
                                    List<String> processList) {
        if (group == null) {
            return;
        }
        ConsumeGroupInfo consumeGroupInfo = groupInfoMap.get(group);
        if (consumeGroupInfo != null) {
            consumeGroupInfo.setBalanceNodeProcessed(processList);
        }
    }

    /**
     * booked need re-balance consumer of group
     *
     * @param group group name
     * @param consumerIdSet need re-balance consumerId
     * @param waitDuration wait duration
     */
    public void addRebConsumerInfo(String group, Set<String> consumerIdSet, int waitDuration) {
        ConsumeGroupInfo consumeGroupInfo = groupInfoMap.get(group);
        if (consumeGroupInfo != null) {
            for (String consumerId : consumerIdSet) {
                String oldGroup = consumerIndexMap.get(consumerId);
                if (group.equals(oldGroup)) {
                    consumeGroupInfo.addNodeRelInfo(consumerId, waitDuration);
                }
            }
        }
    }

    /**
     * Check if allocated
     *
     * @param group group name
     * @return allocate status
     */
    public boolean isNotAllocated(String group) {
        if (group == null) {
            return false;
        }
        ConsumeGroupInfo consumeGroupInfo =
                groupInfoMap.get(group);
        if (consumeGroupInfo != null) {
            return consumeGroupInfo.isNotAllocate();
        }
        return false;
    }

    /**
     * get group name of consumer
     *
     * @param consumerId consumer id
     * @return the group name of consumer
     */
    public String getGroupName(String consumerId) {
        return consumerIndexMap.get(consumerId);
    }

    /**
     * get all registered group name
     *
     * @return the group name registered
     */
    public List<String> getAllGroupName() {
        if (groupInfoMap.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(groupInfoMap.keySet());
    }

    /**
     * get all registered topic-group map information
     *
     * @return the registered topic-group map
     */
    public ConcurrentHashMap<String, ConcurrentHashSet<String>> getRegTopicGroupMap() {
        return topicGroupMap;
    }

    /**
     * get all server-balance group name
     *
     * @return the group name registered
     */
    public List<String> getAllServerBalanceGroups() {
        if (serverBalanceGroupSet.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(serverBalanceGroupSet);
    }

    /**
     * get all client-balance group name
     *
     * @return the group name registered
     */
    public List<String> getAllClientBalanceGroups() {
        if (clientBalanceGroupSet.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(clientBalanceGroupSet);
    }

    /**
     * get all registered group name
     *
     * @param consumerId  the consumer id
     * @return the consumer info
     */
    public ConsumerInfo getConsumerInfo(String consumerId) {
        ConsumerInfo consumerInfo = null;
        String groupName = consumerIndexMap.get(consumerId);
        if (groupName != null) {
            ConsumeGroupInfo consumeGroupInfo = groupInfoMap.get(groupName);
            if (consumeGroupInfo != null) {
                consumerInfo = consumeGroupInfo.getConsumerInfo(consumerId);
            }
        }
        return consumerInfo;
    }

    /**
     * Add consumer and return group object,
     * if the consumer is the first one, then create the group object
     *
     * @param consumer consumer info
     * @param isNotAllocated whether balanced
     * @param sBuffer  string buffer
     * @param result   check result
     * @return process result
     */
    public boolean addConsumer(ConsumerInfo consumer, boolean isNotAllocated,
                               StringBuilder sBuffer, ParamCheckResult result) {
        ConsumeGroupInfo consumeGroupInfo;
        String group = consumer.getGroupName();
        Integer lid = null;
        try {
            lid = groupRowLock.getLock(null,
                    StringUtils.getBytesUtf8(group), true);
            consumeGroupInfo = groupInfoMap.get(group);
            if (consumeGroupInfo == null) {
                ConsumeGroupInfo tmpGroupInfo = new ConsumeGroupInfo(consumer);
                consumeGroupInfo = groupInfoMap.putIfAbsent(group, tmpGroupInfo);
                if (consumeGroupInfo == null) {
                    consumeGroupInfo = tmpGroupInfo;
                    if (tmpGroupInfo.isClientBalance()) {
                        clientBalanceGroupSet.add(group);
                    } else {
                        serverBalanceGroupSet.add(group);
                    }
                    // add topic-group map information
                    ConcurrentHashSet<String> groupSet;
                    for (String topicName: consumeGroupInfo.getTopicSet()) {
                        groupSet = topicGroupMap.get(topicName);
                        if (groupSet == null) {
                            ConcurrentHashSet<String> tmpGroupSet = new ConcurrentHashSet<>();
                            groupSet = topicGroupMap.putIfAbsent(topicName, tmpGroupSet);
                            if (groupSet == null) {
                                groupSet = tmpGroupSet;
                            }
                        }
                        groupSet.add(group);
                    }
                    // statistic data
                    MasterSrvStatsHolder.incConsumerCnt(true,
                            consumeGroupInfo.isClientBalance());
                }
            }
            if (consumeGroupInfo.addConsumer(consumer, sBuffer, result)) {
                if ((Boolean) result.checkData) {
                    MasterSrvStatsHolder.incConsumerCnt(false,
                            consumeGroupInfo.isClientBalance());
                }
                if (!isNotAllocated) {
                    consumeGroupInfo.settAllocated();
                }
                consumerIndexMap.put(consumer.getConsumerId(), group);
                result.setCheckData(consumeGroupInfo);
            }
        } catch (IOException e) {
            logger.warn("Failed to lock.", e);
        } finally {
            if (lid != null) {
                groupRowLock.releaseRowLock(lid);
            }
        }
        return result.result;
    }

    /**
     * remove the consumer and return consumer object,
     * if the consumer is the latest one, then removed the group object
     *
     * @param group group name of consumer
     * @param consumerId consumer id
     * @param isTimeout if timeout
     * @return ConsumerInfo
     */
    public ConsumerInfo removeConsumer(String group, String consumerId, boolean isTimeout) {
        if (group == null || consumerId == null) {
            return null;
        }
        boolean rmvGroup;
        boolean isCltBal = false;
        ConsumerInfo consumer = null;
        Integer lid = null;
        try {
            lid = groupRowLock.getLock(null,
                    StringUtils.getBytesUtf8(group), true);
            ConsumeGroupInfo consumeGroupInfo = groupInfoMap.get(group);
            if (consumeGroupInfo != null) {
                consumer = consumeGroupInfo.removeConsumer(consumerId);
                if (consumeGroupInfo.isGroupEmpty()) {
                    rmvGroup = (groupInfoMap.remove(group) != null);
                    if (consumeGroupInfo.isClientBalance()) {
                        isCltBal = true;
                        clientBalanceGroupSet.remove(group);
                    } else {
                        serverBalanceGroupSet.remove(group);
                    }
                    // remove topic-group map information
                    ConcurrentHashSet<String> groupSet;
                    for (String topicName: consumeGroupInfo.getTopicSet()) {
                        groupSet = topicGroupMap.get(topicName);
                        if (groupSet == null) {
                            continue;
                        }
                        groupSet.remove(group);
                    }
                    // statistic data
                    if (rmvGroup) {
                        if (consumer == null) {
                            MasterSrvStatsHolder.decConsumeGroupCnt(isTimeout, isCltBal);
                        } else {
                            MasterSrvStatsHolder.decConsumerCnt(isTimeout, true, isCltBal);
                        }
                    }
                } else {
                    if (consumer != null) {
                        MasterSrvStatsHolder.decConsumerCnt(isTimeout, false, false);
                    }
                }
            }
            consumerIndexMap.remove(consumerId);
        } catch (IOException e) {
            logger.warn("Failed to lock.", e);
        } finally {
            if (lid != null) {
                groupRowLock.releaseRowLock(lid);
            }
        }
        return consumer;
    }
}
