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

package org.apache.inlong.tubemq.manager.controller.topic.result;

import lombok.Data;

@Data
public class TopicInfoRes {
    private String topicName;
    private String topicStatusId;
    private String brokerId;
    private String brokerIp;
    private String brokerPort;
    private String numPartitions;
    private String unflushThreshold;
    private String unflushStringerval;
    private String unflushDataHold;
    private String deleteWhen;
    private String deletePolicy;
    private boolean acceptPublish;
    private boolean acceptSubscribe;
    private String numTopicStores;
    private String memCacheMsgSizeInMB;
    private String memCacheFlushStringvl;
    private String memCacheMsgCntInK;
    private String maxMsgSizeInMB;
    private String createUser;
    private String createDate;
    private String modifyUser;
    private String modifyDate;
    private TopicRunInfo runInfo;

    @Data
    private static class TopicRunInfo {
        private boolean acceptPublish;
        private boolean acceptSubscribe;
        private String numPartitions;
        private String numTopicStores;
        private String brokerManageStatus;
    }
}
