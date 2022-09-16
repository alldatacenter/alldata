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

package org.apache.inlong.tubemq.server.master.nodemanage.nodeconsumer;

public enum ConsumeType {
    CONSUME_NORMAL(0, "unbound", "Normal consume without reset offset"),
    CONSUME_BAND(1, "bound", "Consume data with reset offset"),
    CONSUME_CLIENT_REB(2, "client-rebalance", "Consume data with client assigned partitions");

    private final int code;
    private final String name;
    private final String description;

    ConsumeType(int code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static ConsumeType valueOf(int code) {
        for (ConsumeType csmType : ConsumeType.values()) {
            if (csmType.getCode() == code) {
                return csmType;
            }
        }
        throw new IllegalArgumentException(String.format("unknown ConsumeType code %s", code));
    }
}
