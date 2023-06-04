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
 * This class defines the redis command supportted in Sort-connector-redis
 */
public enum RedisCommand {

    /**
     * The command for hget
     */
    HGET("hget"),
    /**
     * The command for get
     */
    GET("get"),
    /**
     * The command for zscore
     */
    ZSCORE("zscore"),
    /**
     * The command for zrevrank
     */
    ZREVRANK("zrevrank");

    private final String value;

    RedisCommand(String value) {
        this.value = value;
    }

    public static RedisCommand forName(String name) {
        for (RedisCommand command : values()) {
            if (command.name().equals(name)) {
                return command;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported redis command=%s for Inlong", name));
    }

    public String getValue() {
        return value;
    }

}
