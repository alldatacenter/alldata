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

package org.apache.inlong.tubemq.client.common;

public enum StatsLevel {
    ZERO(0, "closed", "Statistics are turned off"),
    SIMPLEST(1, "simplest", "Simplest statistics"),
    MEDIUM(2, "medium", "Medium statistics"),
    FULL(3, "full", "Full statistics");

    StatsLevel(int id, String name, String desc) {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public static StatsLevel valueOf(int value) {
        for (StatsLevel outputLevel : StatsLevel.values()) {
            if (outputLevel.getId() == value) {
                return outputLevel;
            }
        }
        return SIMPLEST;
    }

    private final int id;
    private final String name;
    private final String desc;
}
