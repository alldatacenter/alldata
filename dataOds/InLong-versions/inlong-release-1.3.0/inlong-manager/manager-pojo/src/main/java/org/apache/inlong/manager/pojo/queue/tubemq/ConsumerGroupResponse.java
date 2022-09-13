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

package org.apache.inlong.manager.pojo.queue.tubemq;

import lombok.Data;

import java.util.List;

/**
 * Topic view of TubeMQ
 */
@Data
public class ConsumerGroupResponse {

    // true, or false
    private boolean result;

    // 0 is success, other is failed
    private int errCode;

    // OK, or err msg
    private String errMsg;

    private List<ConsumerGroupInfo> data;

    private int count;

    @Data
    public static class ConsumerGroupInfo {

        private String topicName;
        private String groupName;
        private String createUser;
        private String modifyUser;
        private String createDate; // 20150619115100
        private String modifyDate;
    }

}