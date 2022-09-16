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

package org.apache.inlong.sort.protocol.enums;

/**
 * This class defines redis deploy mode
 */
public enum RedisMode {

    /**
     * Standalone mode
     */
    STANDALONE("standalone"),
    /**
     * Cluster mode
     */
    CLUSTER("cluster"),
    /**
     * Sentinel mode
     */
    SENTINEL("sentinel");

    private final String value;

    RedisMode(String value) {
        this.value = value;
    }

    public static RedisMode forName(String name) {
        for (RedisMode redisMode : values()) {
            if (redisMode.name().equals(name)) {
                return redisMode;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported redis-mode=%s for Inlong", name));
    }

    public String getValue() {
        return value;
    }
}
