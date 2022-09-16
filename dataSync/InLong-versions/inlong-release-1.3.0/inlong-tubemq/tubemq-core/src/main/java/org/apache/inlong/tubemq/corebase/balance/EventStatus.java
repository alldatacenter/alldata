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

package org.apache.inlong.tubemq.corebase.balance;

public enum EventStatus {
    /**
     * To be processed state.
     * */
    TODO(0, "To be processed"),
    /**
     * On processing state.
     * */
    PROCESSING(1, "Being processed"),
    /**
     * Processed state.
     * */
    DONE(2, "Process Done"),

    /**
     * Unknown state.
     * */
    UNKNOWN(-1, "Unknown event status"),
    /**
     * Failed state.
     * */
    FAILED(-2, "Process failed");

    private int value;
    private String description;

    EventStatus(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public static EventStatus valueOf(int value) {
        for (EventStatus status : EventStatus.values()) {
            if (status.getValue() == value) {
                return status;
            }
        }
        return UNKNOWN;
    }

    public int getValue() {
        return value;
    }

    public String getDesc() {
        return description;
    }

}

