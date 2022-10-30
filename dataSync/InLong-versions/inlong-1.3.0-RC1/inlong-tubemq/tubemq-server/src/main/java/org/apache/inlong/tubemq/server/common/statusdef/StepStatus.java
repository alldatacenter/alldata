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
 * The step status of broker operation
 */
public enum StepStatus {

    STEP_STATUS_UNDEFINED(-2, "idle", 0, 0),
    STEP_STATUS_LOAD_DATA(1, "load_data", 0, 0),
    STEP_STATUS_WAIT_ONLINE(2, "wait_online", 0, 0),
    STEP_STATUS_WAIT_SYNC(3, "wait_sync", 0, 0),
    STEP_STATUS_WAIT_SUBSCRIBE(4, "wait_sub", 55000, 40000),
    STEP_STATUS_WAIT_PUBLISH(5, "wait_pub", 25000, 10000);

    private int code;
    private String description;
    private long normalDelayDurIdnMs;
    private long shortDelayDurIdnMs;

    StepStatus(int code, String description,
               long normalDelayDurIdnMs, long shortDelayDurIdnMs) {
        this.code = code;
        this.description = description;
        this.normalDelayDurIdnMs = normalDelayDurIdnMs;
        this.shortDelayDurIdnMs = shortDelayDurIdnMs;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public long getNormalDelayDurInMs() {
        return normalDelayDurIdnMs;
    }

    public long getShortDelayDurIdnMs() {
        return shortDelayDurIdnMs;
    }

    public static StepStatus valueOf(int code) {
        for (StepStatus status : StepStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException(String.format(
                "unknown broker step status code %s", code));
    }

}
