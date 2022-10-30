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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.cluster.Partition;
import org.apache.inlong.tubemq.corebase.cluster.TopicInfo;
import org.apache.inlong.tubemq.corebase.utils.ConcurrentHashSet;
import org.apache.inlong.tubemq.corebase.utils.Tuple2;
import org.apache.inlong.tubemq.server.common.utils.SerialIdUtils;

/*
 * Topic view of Broker's current operations
 */
public class BrokerTopicInfoView {
    public AtomicLong topicChangeId = new AtomicLong(0);
    private final ConcurrentHashMap<String/* topicName */,
            ConcurrentHashMap<Integer/* brokerId */, TopicInfo>> topicConfInfoMap =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer/* brokerId */, ConcurrentHashSet<String/* topicName */>>
            brokerIdIndexMap = new ConcurrentHashMap<>();

    public BrokerTopicInfoView() {

    }

    /**
     * update broker's topicInfo configures
     *
     * @param brokerId broker id index
     * @param topicInfoMap broker's topic configure info,
     *                    if topicInfoMap is null, reserve current configure;
     *                    if topicInfoMap is empty, clear current configure.
     */
    public void updBrokerTopicConfInfo(int brokerId,
                                       Map<String, TopicInfo> topicInfoMap) {
        if (topicInfoMap == null) {
            return;
        }
        // get removed topic info
        rmvBrokerTopicInfo(brokerId, topicInfoMap);
        // add or update TopicInfo
        repBrokerTopicInfo(brokerId, topicInfoMap);
        SerialIdUtils.updTimeStampSerialIdValue(this.topicChangeId);
    }

    /**
     * update broker's topicInfo configures
     *
     * @param brokerId broker id index
     * @param topicInfoMap broker's topic configure info,
     *                    if topicInfoMap is null, reserve current configure;
     *                    if topicInfoMap is empty, clear current configure.
     * @return if fast sync data
     */
    public boolean fastUpdBrokerTopicConfInfo(int brokerId,
                                              Map<String, TopicInfo> topicInfoMap) {
        if (topicInfoMap == null) {
            return true;
        }
        // get removed topic info
        rmvBrokerTopicInfo(brokerId, topicInfoMap);
        // update TopicInfo and judge if fast update
        Tuple2<Boolean, Boolean> retTuple =
                updBrokerTopicInfo(brokerId, topicInfoMap);
        SerialIdUtils.updTimeStampSerialIdValue(this.topicChangeId);
        return retTuple.getF1();
    }

    /**
     * Get the maximum number of broker distributions of topic
     *
     * @param topicSet need query topic set
     * @return   max configure broker count for each topic
     */
    public int getMaxTopicBrokerCnt(Set<String> topicSet) {
        int tmpSize;
        int maxCount = -1;
        ConcurrentHashMap<Integer, TopicInfo> topicInfoView;
        if (topicSet == null || topicSet.isEmpty()) {
            return maxCount;
        }
        for (String topic : topicSet) {
            if (topic == null) {
                continue;
            }
            topicInfoView = topicConfInfoMap.get(topic);
            if (topicInfoView == null
                    || topicInfoView.isEmpty()) {
                continue;
            }
            tmpSize = topicInfoView.size();
            if (maxCount < tmpSize) {
                maxCount = tmpSize;
            }
        }
        return maxCount;
    }

    /**
     * Gets the map of topic partitions whose subscribe status is enabled
     *
     * @param topicSet need query topic set
     * @param enableSubBrokerIdSet  need filtered broker id set
     * @return  query result
     */
    public Map<String, Partition> getAcceptSubParts(Set<String> topicSet,
                                             Set<Integer> enableSubBrokerIdSet) {
        Map<String, Partition> partMap = new HashMap<>();
        if (topicSet == null || topicSet.isEmpty()) {
            return partMap;
        }
        List<Partition> tmpPartList;
        for (String topic : topicSet) {
            tmpPartList = getAcceptSubParts(topic, enableSubBrokerIdSet);
            for (Partition partition : tmpPartList) {
                partMap.put(partition.getPartitionKey(), partition);
            }
        }
        return partMap;
    }

    /**
     * Gets the list of topic partitions whose subscribe status is enabled
     *
     * @param topic need query topic set
     * @param enableSubBrokerIdSet  need filter broker id set
     * @return   query result
     */
    public List<Partition> getAcceptSubParts(String topic, Set<Integer> enableSubBrokerIdSet) {
        TopicInfo topicInfo;
        List<Partition> partList = new ArrayList<>();
        if (topic == null) {
            return partList;
        }
        ConcurrentHashMap<Integer, TopicInfo> topicInfoView =
                topicConfInfoMap.get(topic);
        if (topicInfoView == null
                || topicInfoView.isEmpty()) {
            return partList;
        }
        for (Map.Entry<Integer, TopicInfo> entry : topicInfoView.entrySet()) {
            if (entry.getKey() == null
                    || entry.getValue() == null
                    || !enableSubBrokerIdSet.contains(entry.getKey())) {
                continue;
            }
            topicInfo = entry.getValue();
            if (topicInfo.isAcceptSubscribe()) {
                for (int j = 0; j < topicInfo.getTopicStoreNum(); j++) {
                    int baseValue = j * TBaseConstants.META_STORE_INS_BASE;
                    for (int i = 0; i < topicInfo.getPartitionNum(); i++) {
                        partList.add(new Partition(topicInfo.getBroker(),
                                topicInfo.getTopic(), baseValue + i));
                    }
                }
            }
        }
        return partList;
    }

    /**
     * Gets the string map of topic partitions whose publish status is enabled
     *
     * @param topicSet need query topic set
     * @param enablePubBrokerIdSet  need filtered broker id set
     * @return  query result
     */
    public Map<String, String> getAcceptPubPartInfo(Set<String> topicSet,
                                                    Set<Integer> enablePubBrokerIdSet) {
        TopicInfo topicInfo;
        ConcurrentHashMap<Integer, TopicInfo> topicInfoView;
        Map<String, String> topicPartStrMap = new HashMap<>();
        Map<String, StringBuilder> topicPartBufferMap = new HashMap<>();
        if (topicSet == null || topicSet.isEmpty()) {
            return topicPartStrMap;
        }
        for (String topic : topicSet) {
            if (topic == null) {
                continue;
            }
            topicInfoView = topicConfInfoMap.get(topic);
            if (topicInfoView == null
                    || topicInfoView.isEmpty()) {
                continue;
            }
            for (Map.Entry<Integer, TopicInfo> entry : topicInfoView.entrySet()) {
                if (entry.getKey() == null
                        || entry.getValue() == null
                        || !enablePubBrokerIdSet.contains(entry.getKey())) {
                    continue;
                }
                topicInfo = entry.getValue();
                if (topicInfo.isAcceptPublish()) {
                    StringBuilder tmpValue = topicPartBufferMap.get(topic);
                    if (tmpValue == null) {
                        StringBuilder strBuffer =
                                new StringBuilder(512).append(topic)
                                        .append(TokenConstants.SEGMENT_SEP)
                                        .append(topicInfo.getSimpleValue());
                        topicPartBufferMap.put(topic, strBuffer);
                    } else {
                        tmpValue.append(TokenConstants.ARRAY_SEP)
                                .append(topicInfo.getSimpleValue());
                    }
                }
            }
        }
        for (Map.Entry<String, StringBuilder> entry : topicPartBufferMap.entrySet()) {
            if (entry.getValue() != null) {
                topicPartStrMap.put(entry.getKey(), entry.getValue().toString());
            }
        }
        topicPartBufferMap.clear();
        return topicPartStrMap;
    }

    /**
     * Get the TopicInfo information of topic in broker
     *
     * @param brokerId need query broker
     * @param topic    need query topic
     *
     * @return null or topicInfo configure
     */
    public TopicInfo getBrokerPushedTopicInfo(int brokerId, String topic) {
        ConcurrentHashMap<Integer, TopicInfo> topicInfoView =
                topicConfInfoMap.get(topic);
        if (topicInfoView == null) {
            return null;
        }
        return topicInfoView.get(brokerId);
    }

    /**
     * Get all TopicInfo information of broker
     *
     * @param brokerId need query broker
     */
    public List<TopicInfo> getBrokerPushedTopicInfo(int brokerId) {
        TopicInfo topicInfo;
        ConcurrentHashMap<Integer, TopicInfo> topicInfoView;
        List<TopicInfo> topicInfoList = new ArrayList<>();
        ConcurrentHashSet<String> topicSet = brokerIdIndexMap.get(brokerId);
        if (topicSet == null) {
            return topicInfoList;
        }
        for (String topic : topicSet) {
            if (topic == null) {
                continue;
            }
            topicInfoView = topicConfInfoMap.get(topic);
            if (topicInfoView == null
                    || topicInfoView.isEmpty()) {
                continue;
            }
            topicInfo = topicInfoView.get(brokerId);
            if (topicInfo == null) {
                continue;
            }
            topicInfoList.add(topicInfo);
        }
        return topicInfoList;
    }

    /**
     * Remove broker all topic info
     *
     * @param brokerId  need removed broker
     */
    public void rmvBrokerTopicInfo(int brokerId) {
        ConcurrentHashMap<Integer, TopicInfo> topicInfoView;
        // remove pub info
        ConcurrentHashSet<String> topicSet =
                brokerIdIndexMap.remove(brokerId);
        if (topicSet == null || topicSet.isEmpty()) {
            return;
        }
        for (String topic : topicSet) {
            if (topic == null) {
                continue;
            }
            topicInfoView = topicConfInfoMap.get(topic);
            if (topicInfoView == null
                    || topicInfoView.isEmpty()) {
                continue;
            }
            topicInfoView.remove(brokerId);
        }
        SerialIdUtils.updTimeStampSerialIdValue(this.topicChangeId);
    }

    // remove broker special topic info
    private boolean rmvBrokerTopicInfo(int brokerId,
                                       Map<String, TopicInfo> topicInfoMap) {
        boolean changed = false;
        Set<String> delTopicSet = new HashSet<>();
        ConcurrentHashSet<String> curTopicSet = brokerIdIndexMap.get(brokerId);
        if (curTopicSet != null) {
            for (String topic : curTopicSet) {
                if (!topicInfoMap.containsKey(topic)) {
                    delTopicSet.add(topic);
                }
            }
        }
        if (delTopicSet.isEmpty()) {
            return false;
        }
        ConcurrentHashMap<Integer, TopicInfo> topicInfoView;
        ConcurrentHashSet<String> topicSet = brokerIdIndexMap.get(brokerId);
        if (topicSet == null || topicSet.isEmpty()) {
            return changed;
        }
        for (String topic : delTopicSet) {
            if (topicSet.remove(topic)) {
                changed = true;
            }
            topicInfoView = topicConfInfoMap.get(topic);
            if ((topicInfoView == null)
                    || topicInfoView.isEmpty()) {
                continue;
            }
            if (topicInfoView.remove(brokerId) != null) {
                changed = true;
            }
        }
        return changed;
    }

    // add or update broker special topic info
    private void repBrokerTopicInfo(int brokerId,
                                    Map<String, TopicInfo> topicInfoMap) {
        if (topicInfoMap == null || topicInfoMap.isEmpty()) {
            return;
        }
        // add topic info
        ConcurrentHashMap<Integer, TopicInfo> newTopicInfoView;
        ConcurrentHashMap<Integer, TopicInfo> curTopicInfoView;
        for (TopicInfo topicInfo : topicInfoMap.values()) {
            if (topicInfo == null) {
                continue;
            }
            curTopicInfoView = topicConfInfoMap.get(topicInfo.getTopic());
            if (curTopicInfoView == null) {
                newTopicInfoView = new ConcurrentHashMap<Integer, TopicInfo>();
                curTopicInfoView = topicConfInfoMap.putIfAbsent(
                        topicInfo.getTopic(), newTopicInfoView);
                if (curTopicInfoView == null) {
                    curTopicInfoView = newTopicInfoView;
                }
            }
            curTopicInfoView.put(brokerId, topicInfo.clone());
        }
        // add broker index
        ConcurrentHashSet<String> curTopicSet = brokerIdIndexMap.get(brokerId);
        if (curTopicSet == null) {
            ConcurrentHashSet<String> newTopicSet = new ConcurrentHashSet<>();
            curTopicSet = brokerIdIndexMap.putIfAbsent(brokerId, newTopicSet);
            if (curTopicSet == null) {
                curTopicSet = newTopicSet;
            }
        }
        curTopicSet.addAll(topicInfoMap.keySet());
    }

    // update current broker special topic info
    private Tuple2<Boolean, Boolean> updBrokerTopicInfo(int brokerId,
                                                        Map<String, TopicInfo> topicInfoMap) {
        boolean isChanged = false;
        boolean isFastSync = true;
        if (topicInfoMap == null || topicInfoMap.isEmpty()) {
            return new Tuple2<>(isChanged, isFastSync);
        }
        Tuple2<Boolean, Boolean> retResult;
        ConcurrentHashMap<Integer, TopicInfo> curTopicInfoView;
        for (TopicInfo newTopicInfo : topicInfoMap.values()) {
            if (newTopicInfo == null) {
                continue;
            }
            curTopicInfoView = topicConfInfoMap.get(newTopicInfo.getTopic());
            if (curTopicInfoView == null) {
                isFastSync = false;
                continue;
            }
            TopicInfo curTopicInfo = curTopicInfoView.get(brokerId);
            if (curTopicInfo == null) {
                isFastSync = false;
                continue;
            }
            retResult = curTopicInfo.updAndJudgeTopicInfo(newTopicInfo);
            if (retResult.getF0() && !isChanged) {
                isChanged = true;
            }
            if (retResult.getF1() && isFastSync) {
                isFastSync = false;
            }
        }
        return new Tuple2<>(isChanged, isFastSync);
    }

}
