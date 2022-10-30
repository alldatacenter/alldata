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
 * The topic status change step
 */
public enum TopicStsChgType {
    STATUS_CHANGE_SOFT_DELETE(0, "Soft deleted"),
    STATUS_CHANGE_REMOVE(1, "Soft removed"),
    STATUS_CHANGE_REDO_SFDEL(2, "Redo soft delete");

    private int code;
    private String description;

    TopicStsChgType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static TopicStsChgType valueOf(int code) {
        for (TopicStsChgType changeType : TopicStsChgType.values()) {
            if (changeType.getCode() == code) {
                return changeType;
            }
        }
        throw new IllegalArgumentException(String.format("unknown status change code %s", code));
    }

}
