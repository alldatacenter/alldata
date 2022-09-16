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

package org.apache.inlong.tubemq.server.master.nodemanage.nodebroker;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.inlong.tubemq.corebase.utils.ConcurrentHashSet;
import org.apache.inlong.tubemq.server.master.TMaster;
import org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer.ConsumerInfoHolder;

/**
 * Topic Publication/Subscription info management
 */
public class TopicPSInfoManager {

    private final TMaster master;
    private final ConcurrentHashMap<String/* topic */,
            ConcurrentHashSet<String/* producerId */>> topicPubInfoMap =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String/* topic */,
            ConcurrentHashSet<String/* group */>> topicSubInfoMap =
            new ConcurrentHashMap<>();

    public TopicPSInfoManager(TMaster master) {
        this.master = master;
    }

    /**
     * Get groups according to topic
     *
     * @param topic  query topic
     * @return  query result
     */
    public Set<String> getTopicSubInfo(String topic) {
        return topicSubInfoMap.get(topic);
    }

    /**
     * Add group subscribe topic info
     *
     * @param groupName    the group name
     * @param topicSet     the topic set which the group subscribed
     */
    public void addGroupSubTopicInfo(String groupName, Set<String> topicSet) {
        for (String topic : topicSet) {
            ConcurrentHashSet<String> groupSet = topicSubInfoMap.get(topic);
            if (groupSet == null) {
                ConcurrentHashSet<String> tmpGroupSet =
                        new ConcurrentHashSet<>();
                groupSet = topicSubInfoMap.putIfAbsent(topic, tmpGroupSet);
                if (groupSet == null) {
                    groupSet = tmpGroupSet;
                }
            }
            groupSet.add(groupName);
        }
    }

    /**
     * Remove the group's topic set
     *
     * @param group    the group name which needs removed topic
     * @param topicSet the topic set which the group name subscribed
     */
    public void rmvGroupSubTopicInfo(String group, Set<String> topicSet) {
        if (topicSet == null || group == null) {
            return;
        }
        ConcurrentHashSet<String> groupSet;
        for (String topic : topicSet) {
            if (topic == null) {
                continue;
            }
            groupSet = topicSubInfoMap.get(topic);
            if (groupSet == null) {
                continue;
            }
            groupSet.remove(group);
        }
    }

    /**
     * Get producer IDs for a topic
     *
     * @param topic  query topic
     * @return   target published producerId set
     */
    public Set<String> getTopicPubInfo(String topic) {
        return topicPubInfoMap.get(topic);
    }

    /**
     * Add producer produce topic set
     *
     * @param producerId  need add producer id
     * @param topicList   need add topic set
     */
    public void addProducerTopicPubInfo(String producerId, Set<String> topicList) {
        for (String topic : topicList) {
            ConcurrentHashSet<String> producerIdSet =
                    topicPubInfoMap.get(topic);
            if (producerIdSet == null) {
                ConcurrentHashSet<String> tmpProducerIdSet =
                        new ConcurrentHashSet<>();
                producerIdSet =
                        topicPubInfoMap.putIfAbsent(topic, tmpProducerIdSet);
                if (producerIdSet == null) {
                    producerIdSet = tmpProducerIdSet;
                }
            }
            producerIdSet.add(producerId);
        }
    }

    /**
     * Remove producer produce topic set
     *
     * @param producerId  need removed producer id
     * @param topicList   need removed topic set
     */
    public void rmvProducerTopicPubInfo(String producerId, Set<String> topicList) {
        if (topicList != null) {
            for (String topic : topicList) {
                if (topic != null) {
                    ConcurrentHashSet<String> producerIdSet =
                            topicPubInfoMap.get(topic);
                    if (producerIdSet != null) {
                        producerIdSet.remove(producerId);
                    }
                }
            }
        }
    }

    public void clear() {
        topicPubInfoMap.clear();
        topicSubInfoMap.clear();
    }

    /**
     * Get the set of online groups subscribed to the specified topic.
     * If the specified query consumer group is empty, then the full amount of
     * online consumer groups will be taken; if the specified subscription topic
     * is empty, then all online consumer groups will be taken.
     *
     * @param qryGroupSet
     * @param subTopicSet
     * @return online groups
     */
    public Set<String> getGroupSetWithSubTopic(Set<String> qryGroupSet,
                                               Set<String> subTopicSet) {
        Set<String> resultSet = new HashSet<>();
        if (subTopicSet.isEmpty()) {
            // get all online group
            ConsumerInfoHolder consumerHolder = master.getConsumerHolder();
            List<String> onlineGroups = consumerHolder.getAllGroupName();
            if (!onlineGroups.isEmpty()) {
                if (qryGroupSet.isEmpty()) {
                    resultSet.addAll(onlineGroups);
                } else {
                    for (String group : qryGroupSet) {
                        if (onlineGroups.contains(group)) {
                            resultSet.add(group);
                        }
                    }
                }
            }
        } else {
            // filter subscribed online group
            Set<String> tmpGroupSet;
            if (qryGroupSet.isEmpty()) {
                for (String topic : subTopicSet) {
                    tmpGroupSet = topicSubInfoMap.get(topic);
                    if (tmpGroupSet != null && !tmpGroupSet.isEmpty()) {
                        resultSet.addAll(tmpGroupSet);
                    }
                }
            } else {
                for (String topic : subTopicSet) {
                    tmpGroupSet = topicSubInfoMap.get(topic);
                    if (tmpGroupSet == null || tmpGroupSet.isEmpty()) {
                        continue;
                    }
                    for (String group : qryGroupSet) {
                        if (tmpGroupSet.contains(group)) {
                            resultSet.add(group);
                        }
                    }
                    qryGroupSet.removeAll(resultSet);
                    if (qryGroupSet.isEmpty()) {
                        break;
                    }
                }
            }
        }
        return resultSet;
    }
}
