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

package org.apache.inlong.tubemq.manager.controller.topic.request;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
public class AddTopicTask {

    public static final int MAX_TOPIC_LENGTH = 64;
    public static final String TOPIC_NAME_REGEX = "^[a-zA-Z]\\w+$";
    private String topicName;
    private Long clusterId;

    public boolean legal() {
        if (StringUtils.isBlank(topicName)) {
            return false;
        }
        if (topicName.length() > MAX_TOPIC_LENGTH) {
            throw new IllegalArgumentException("topic name length larger than " + MAX_TOPIC_LENGTH);
        }
        if (!topicName.matches(TOPIC_NAME_REGEX)) {
            throw new IllegalArgumentException("topic name must begin with a letter, can"
                    + " only contain characters,numbers,and underscores");
        }
        return true;
    }
}
