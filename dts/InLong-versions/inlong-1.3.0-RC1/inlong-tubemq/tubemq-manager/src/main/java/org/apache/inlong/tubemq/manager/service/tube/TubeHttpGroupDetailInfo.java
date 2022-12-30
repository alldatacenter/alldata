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

import com.google.common.collect.Lists;

import java.util.List;

import lombok.Data;

@Data
public class TubeHttpGroupDetailInfo {

    private String errMsg;

    private int errCode;

    private int count;

    private List<String> topicSet;

    private String consumeGroup;

    private List<ConsumerInfo> data;

    @Data
    private static class ConsumerInfo {

        private String consumerId;

        private Integer parCount;

        private List<PartitionInfo> parInfo;

        @Data
        public static class PartitionInfo {
            private String brokerAddr;
            private String topic;
            private Integer partId;
        }

    }

    public List<String> getConsumerIds() {
        List<String> consumerIds = Lists.newArrayList();
        data.forEach(consumerInfo -> {
            consumerIds.add(consumerInfo.getConsumerId());
        });
        return consumerIds;
    }

}
