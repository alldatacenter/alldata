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

package org.apache.inlong.tubemq.manager.service.tube;

import static org.apache.inlong.tubemq.manager.service.TubeConst.BATCH_ADD_TOPIC;
import static org.apache.inlong.tubemq.manager.service.TubeConst.OP_MODIFY;
import static org.apache.inlong.tubemq.manager.service.TubeConst.WEB_API;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.tubemq.manager.controller.node.request.AddTopicReq;
import org.apache.inlong.tubemq.manager.service.tube.TubeHttpTopicInfoList.TopicInfoList.TopicInfo;

/**
 * json class for topic info list from master http service.
 */
@Data
public class TubeHttpTopicInfoList {

    private boolean result;

    private String errMsg;

    private int errCode;

    private List<TopicInfoList> data;

    @Data
    public static class TopicInfoList {

        @Data
        public static class TopicInfo {

            @Data
            public static class RunInfo {

                private String acceptPublish;
                private String acceptSubscribe;
                private String numPartitions;
                private String numTopicStores;
                private String brokerManageStatus;
            }

            private String topicName;
            private int topicStatusId;
            private int brokerId;
            private String brokerIp;
            private int brokerPort;
            private int numPartitions;
            private int unflushThreshold;
            private int unflushInterval;
            private int unFlushDataHold;
            private String deleteWhen;
            private String deletePolicy;
            private boolean acceptPublish;
            private boolean acceptSubscribe;
            private int numTopicStores;
            private int memCacheMsgSizeInMB;
            private int memCacheFlushIntvl;
            private int memCacheMsgCntInK;
            private String createUser;
            private String createDate;
            private String modifyUser;
            private String modifyDate;
            private RunInfo runInfo;

        }

        private String topicName;
        private List<TopicInfo> topicInfo;
    }

    public List<Integer> getTopicBrokerIdList() {
        List<Integer> tmpBrokerIdList = new ArrayList<>();
        if (data != null) {
            for (TopicInfoList topicInfoList : data) {
                if (topicInfoList.getTopicInfo() != null) {
                    for (TopicInfo topicInfo : topicInfoList.getTopicInfo()) {
                        tmpBrokerIdList.add(topicInfo.getBrokerId());
                    }
                }
            }
        }
        return tmpBrokerIdList;
    }

    public List<TopicInfo> getTopicInfo() {
        if (CollectionUtils.isEmpty(data)) {
            return Lists.newArrayList();
        }
        return data.get(0).getTopicInfo();
    }

    /**
     * topic add information
     *
     * @param brokerIds this is the broker ids
     * @param targetTopicNames The topic name of the target
     * @param token authorized key
     * @return topic data
     */
    public AddTopicReq getAddTopicReq(List<Integer> brokerIds, List<String> targetTopicNames, String token) {

        AddTopicReq req = new AddTopicReq();
        TopicInfoList topicInfoList = data.get(0);
        if (topicInfoList == null) {
            return req;
        }
        List<TopicInfo> topicInfos = topicInfoList.getTopicInfo();
        if (CollectionUtils.isEmpty(topicInfos)) {
            return req;
        }

        TopicInfo topicInfo = topicInfos.get(0);
        String brokerStr = StringUtils.join(brokerIds, ",");
        String topic = StringUtils.join(targetTopicNames, ",");

        setAttributes(token, req, topicInfo, brokerStr, topic);
        return req;
    }

    private void setAttributes(String token, AddTopicReq req, TopicInfo topicInfo, String brokerStr,
            String topic) {
        req.setBrokerId(brokerStr);
        req.setTopicName(topic);
        req.setMethod(BATCH_ADD_TOPIC);
        req.setAcceptPublish(topicInfo.acceptPublish);
        req.setAcceptSubscribe(topicInfo.acceptSubscribe);
        req.setType(OP_MODIFY);
        req.setCreateUser(WEB_API);
        req.setDeleteWhen(topicInfo.getDeleteWhen());
        req.setNumPartitions(topicInfo.getNumPartitions());
        req.setUnflushInterval(topicInfo.getUnflushInterval());
        req.setConfModAuthToken(token);
        req.setDeletePolicy(topicInfo.getDeletePolicy());
    }
}
