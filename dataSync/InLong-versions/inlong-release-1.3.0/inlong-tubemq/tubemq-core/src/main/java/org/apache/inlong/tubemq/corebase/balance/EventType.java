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

public enum EventType {
    /**
     * Connect to broker.
     * */
    CONNECT(1, "Connect to some broker after Disconnecting from some other broker."),
    /**
     * DisConnect from broker.
     * */
    DISCONNECT(2, "Disconnect from some broker."),
    /**
     * Report state.
     * */
    REPORT(3, "Report current status."),
    /**
     * Update producer published topic information.
     * */
    REFRESH(4, "Update whole producer published topic info"),
    /**
     * Stop re-balance thread.
     * */
    STOPREBALANCE(5, "Stop rebalance thread"),
    /**
     * Connect to some broker only.
     * */
    ONLY_CONNECT(10, "Only connect to some broker"),
    /**
     * Disconnect from broker and finish.
     * */
    ONLY_DISCONNECT(20, "Disconnect from some broker,then finish."),
    /**
     * Unknown operation.
     * */
    UNKNOWN(-1, "Unknown operation type");

    private int value;
    private String description;

    EventType(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public  int getValue() {
        return value;
    }

    public String getDesc() {
        return description;
    }

    public static EventType valueOf(int value) {
        for (EventType type : EventType.values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        return UNKNOWN;
    }

}
