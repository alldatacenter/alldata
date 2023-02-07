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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Sort task status enum
 */
public enum SortStatus {

    NOT_EXISTS(40, "job not exists"),
    NEW(100, "job not started: draft, pending, etc."),
    RUNNING(110, "job is running"),
    PAUSED(120, "job was paused"),
    STOPPED(130, "job stopped without error, e.g canceled"),
    FAILED(140, "job failed with an error"),
    FINISHED(200, "job finished successfully"),
    OPERATING(300, "job in an intermediate status such as restarting, canceling, etc."),
    UNKNOWN(400, "job status unknown"),

    ;

    @JsonValue
    private final Integer code;
    private final String description;

    SortStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static SortStatus forCode(int code) {
        for (SortStatus status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalStateException(String.format("Illegal code=%s for SortStatus", code));
    }

}
