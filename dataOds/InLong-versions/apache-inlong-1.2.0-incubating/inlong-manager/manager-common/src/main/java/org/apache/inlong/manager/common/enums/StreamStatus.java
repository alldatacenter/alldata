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

package org.apache.inlong.manager.common.enums;

/**
 * Stream status enum
 */
public enum StreamStatus {

    DRAFT(0, "draft"),

    // Stream related status
    NEW(100, "new"),
    CONFIG_ING(110, "in configure"),
    CONFIG_FAILED(120, "configuration failed"),
    CONFIG_SUCCESSFUL(130, "configuration successful"),

    SUSPENDING(141, "suspending"),
    SUSPENDED(140, "suspended"),

    RESTARTING(151, "restarting"),
    RESTARTED(150, "restarted"),

    DELETING(41, "deleting"),
    DELETED(40, "deleted");

    private final Integer code;
    private final String description;

    StreamStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static StreamStatus forCode(int code) {
        for (StreamStatus status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalStateException(String.format("Illegal code=%s for StreamStatus", code));
    }

}