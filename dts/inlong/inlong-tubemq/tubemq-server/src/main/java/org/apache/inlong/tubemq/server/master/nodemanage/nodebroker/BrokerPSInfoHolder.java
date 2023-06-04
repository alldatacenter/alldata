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

package org.apache.inlong.tubemq.server.master.nodemanage.nodebroker;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.inlong.tubemq.corebase.cluster.Partition;
import org.apache.inlong.tubemq.corebase.cluster.TopicInfo;
import org.apache.inlong.tubemq.corebase.utils.ConcurrentHashSet;
import org.apache.inlong.tubemq.corebase.utils.Tuple2;
import org.apache.inlong.tubemq.corebase.utils.Tuple3;
import org.apache.inlong.tubemq.server.common.statusdef.ManageStatus;

/*
 *  Broker publish and subscribe information holder
 */
public class BrokerPSInfoHolder {

    // broker manage status
    private final ConcurrentHashSet<Integer/* brokerId */> enablePubBrokerIdSet =
            new ConcurrentHashSet<>();
    private final ConcurrentHashSet<Integer/* brokerId */> enableSubBrokerIdSet =
            new ConcurrentHashSet<>();
    // broker subscribe topic view info
    private final BrokerTopicInfoView subTopicInfoView = new BrokerTopicInfoView();
    // broker publish topic view info
    private final BrokerTopicInfoView pubTopicInfoView = new BrokerTopicInfoView();

    public BrokerPSInfoHolder() {

    }

    /**
     * remove broker all configure info
     *
     * @param brokerId broker id index
     */
    public void rmvBrokerAllPushedInfo(int brokerId) {
        // remove broker status Info
        enablePubBrokerIdSet.remove(brokerId);
        enableSubBrokerIdSet.remove(brokerId);
        // remove broker topic info
        subTopicInfoView.rmvBrokerTopicInfo(brokerId);
        pubTopicInfoView.rmvBrokerTopicInfo(brokerId);
    }

    /**
     * initial broker configure info
     *
     * @param brokerId broker id index
     * @param mngStatus broker's manage status
     * @param topicInfoMap broker's topic configure info,
     *                     if topicInfoMap is null, reserve current configure;
     *                     if topicInfoMap is empty, clear current configure.
     */
    public void iniBrokerConfigInfo(int brokerId, ManageStatus mngStatus,
            Map<String, TopicInfo> topicInfoMap) {
        // initial broker manage status
        updBrokerMangeStatus(brokerId, mngStatus);
        if (topicInfoMap == null) {
            return;
        }
        // initial broker subscribe info
        subTopicInfoView.updBrokerTopicConfInfo(brokerId, topicInfoMap);
        // initial broker publish info
        pubTopicInfoView.updBrokerTopicConfInfo(brokerId, topicInfoMap);
    }

    /**
     * update broker manage status
     *
     * @param brokerId broker id index
     * @param mngStatus broker's manage status
     */
    public void updBrokerMangeStatus(int brokerId, ManageStatus mngStatus) {
        if (mngStatus.isAcceptPublish()) {
            enablePubBrokerIdSet.add(brokerId);
        } else {
            enablePubBrokerIdSet.remove(brokerId);
        }
        if (mngStatus.isAcceptSubscribe()) {
            enableSubBrokerIdSet.add(brokerId);
        } else {
            enableSubBrokerIdSet.remove(brokerId);
        }
    }

    public void getBrokerPubStatus(int brokerId, Tuple2<Boolean, Boolean> result) {
        result.setF0AndF1(enablePubBrokerIdSet.contains(brokerId),
                enableSubBrokerIdSet.contains(brokerId));
    }

    /**
     * update broker's subscribe topicInfo configures
     *
     * @param brokerId broker id index
     * @param topicInfoMap broker's topic configure info,
     *                    if topicInfoMap is null, reserve current configure;
     *                    if topicInfoMap is empty, clear current configure.
     * @return if fast sync data
     */
    public boolean updBrokerSubTopicConfInfo(int brokerId,
            Map<String, TopicInfo> topicInfoMap) {
        if (topicInfoMap == null) {
            return true;
        }
        subTopicInfoView.updBrokerTopicConfInfo(brokerId, topicInfoMap);
        return pubTopicInfoView.fastUpdBrokerTopicConfInfo(brokerId, topicInfoMap);
    }

    /**
     * update broker's publish topicInfo configures
     *
     * @param brokerId broker id index
     * @param topicInfoMap broker's topic configure info,
     *                    if topicInfoMap is null, reserve current configure;
     *                    if topicInfoMap is empty, clear current configure.
     */
    public void updBrokerPubTopicConfInfo(int brokerId,
            Map<String, TopicInfo> topicInfoMap) {
        if (topicInfoMap == null) {
            return;
        }
        pubTopicInfoView.updBrokerTopicConfInfo(brokerId, topicInfoMap);
    }

    /**
     * Get the maximum number of broker distributions of topic
     *
     * @param topicSet need query topic set
     */
    public int getTopicMaxSubBrokerCnt(Set<String> topicSet) {
        return subTopicInfoView.getMaxTopicBrokerCnt(topicSet, enableSubBrokerIdSet);
    }

    /**
     * Gets the map of topic partitions whose subscribe status is enabled
     *
     * @param topicSet need query topic set
     */
    public Map<String, Partition> getAcceptSubParts(Set<String> topicSet) {
        return subTopicInfoView.getAcceptSubParts(topicSet, enableSubBrokerIdSet);
    }

    /**
     * Gets the list of topic partitions whose subscribe status is enabled
     *
     * @param topic need query topic set
     */
    public List<Partition> getAcceptSubParts(String topic) {
        return subTopicInfoView.getAcceptSubParts(topic, enableSubBrokerIdSet);
    }

    /**
     * Get the subscribed TopicInfo information of topic in broker
     *
     * @param brokerId need query broker
     * @param topic    need query topic
     * @param result   query result(broker accept subscribe, null or topicInfo configure)
     */
    public void getBrokerSubPushedTopicInfo(int brokerId, String topic,
            Tuple2<Boolean, TopicInfo> result) {
        result.setF0AndF1(enableSubBrokerIdSet.contains(brokerId),
                subTopicInfoView.getBrokerPushedTopicInfo(brokerId, topic));
    }

    /**
     * Gets the string map of topic partitions whose publish status is enabled
     *
     * @param topicSizeMap need query topic set and topic's max message size
     */
    public Map<String, String> getAcceptPubPartInfo(Map<String, Integer> topicSizeMap) {
        return pubTopicInfoView.getAcceptPubPartInfo(topicSizeMap, enablePubBrokerIdSet);
    }

    /**
     * Get the published TopicInfo information of topic in broker
     *
     * @param brokerId need query broker
     * @param topic    need query topic
     * @param result   query result(broker accept publish,
     *                       broker accept subscribe, null or topicInfo configure)
     */
    public void getBrokerPubPushedTopicInfo(int brokerId, String topic,
            Tuple3<Boolean, Boolean, TopicInfo> result) {
        result.setFieldsValue(enablePubBrokerIdSet.contains(brokerId),
                enableSubBrokerIdSet.contains(brokerId),
                pubTopicInfoView.getBrokerPushedTopicInfo(brokerId, topic));
    }

    /**
     * Get all published TopicInfo information of broker
     *
     * @param brokerId need query broker
     * @param result   query result(broker accept publish,
     *                       broker accept subscribe, null or topicInfo configure)
     */
    public void getPubBrokerPushedTopicInfo(int brokerId,
            Tuple3<Boolean, Boolean, List<TopicInfo>> result) {
        result.setFieldsValue(enablePubBrokerIdSet.contains(brokerId),
                enableSubBrokerIdSet.contains(brokerId),
                pubTopicInfoView.getBrokerPushedTopicInfo(brokerId));
    }

}
