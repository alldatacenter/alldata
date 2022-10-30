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

package org.apache.inlong.tubemq.server.common.statusdef;

/*
 * The topic state life cycle
 */
public enum TopicStatus {
    STATUS_TOPIC_UNDEFINED(-2, "Undefined"),
    STATUS_TOPIC_OK(0, "Normal"),
    STATUS_TOPIC_SOFT_DELETE(1, "Soft deleted"),
    STATUS_TOPIC_SOFT_REMOVE(2, "Soft removed"),
    STATUS_TOPIC_HARD_REMOVE(3, "Hard removed");

    private int code;
    private String description;

    TopicStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static TopicStatus valueOf(int code) {
        for (TopicStatus status : TopicStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException(String.format("unknown topic status code %s", code));
    }

}
