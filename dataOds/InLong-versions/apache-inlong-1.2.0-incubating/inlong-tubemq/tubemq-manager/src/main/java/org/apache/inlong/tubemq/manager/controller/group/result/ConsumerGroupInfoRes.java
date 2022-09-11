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

package org.apache.inlong.tubemq.manager.controller.group.result;

import lombok.Data;

import java.util.List;

@Data
public class ConsumerGroupInfoRes {
    private String topicName;
    private boolean isEnable;
    private String createUser;
    private String createDate;
    private List<AuthConsumeGroup> authConsumeGroup;
    private double groupCount;
    private List<AuthFilterCondSet> authFilterCondSet;
    private double filterCount;

    @Data
    private static class AuthConsumeGroup {
        private String topicName;
        private String groupName;
        private String createUser;
        private String createDate;
    }

    @Data
    private static class AuthFilterCondSet {
        private String topicName;
        private String groupName;
        private double condStatus;
        private String filterConds;
        private String createUser;
        private String createDate;
    }
}
