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

package org.apache.inlong.sort.redis.common.mapper;

import org.apache.flink.streaming.connectors.redis.common.mapper.RedisDataType;

import java.io.Serializable;
import java.util.Objects;

/**
 * The description of the command type.
 * Copy from {@link org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommandDescription}
 */
public class RedisCommandDescription implements Serializable {

    private static final long serialVersionUID = 1L;

    private RedisCommand redisCommand;

    /**
     * This additional key is needed for the group {@link RedisDataType#HASH} and {@link RedisDataType#SORTED_SET}.
     * Other {@link RedisDataType} works only with two variable i.e. name of the list and value to be added.
     * But for {@link RedisDataType#HASH} and {@link RedisDataType#SORTED_SET} we need three variables.
     * <p>For {@link RedisDataType#HASH} we need hash name, hash key and element. Its possible to use TTL.
     * {@link #getAdditionalKey()} used as hash name for {@link RedisDataType#HASH}
     * <p>For {@link RedisDataType#SORTED_SET} we need set name, the element and it's score.
     * {@link #getAdditionalKey()} used as set name for {@link RedisDataType#SORTED_SET}
     */
    private String additionalKey;

    /**
     * This additional key is optional for the group {@link RedisDataType#HASH}, required for {@link
     * RedisCommand#SETEX}.
     * For the other types and commands, its not used.
     * <p>For {@link RedisDataType#HASH} we need hash name, hash key and element. Its possible to use TTL.
     * {@link #getAdditionalTTL()} used as time to live (TTL) for {@link RedisDataType#HASH}
     * <p>For {@link RedisCommand#SETEX}, we need key, value and time to live (TTL).
     */
    private Integer additionalTTL;

    /**
     * Default constructor for {@link RedisCommandDescription}.
     * For {@link RedisDataType#HASH} and {@link RedisDataType#SORTED_SET} data types, {@code additionalKey} is
     * required.
     * For {@link RedisCommand#SETEX} command, {@code additionalTTL} is required.
     * In both cases, if the respective variables are not provided, it throws an {@link IllegalArgumentException}
     *
     * @param redisCommand the redis command type {@link RedisCommand}
     * @param additionalKey additional key for Hash data type
     * @param additionalTTL additional TTL optional for Hash data type
     */
    public RedisCommandDescription(RedisCommand redisCommand, String additionalKey, Integer additionalTTL) {
        Objects.requireNonNull(redisCommand, "Redis command type can not be null");
        this.redisCommand = redisCommand;
        this.additionalKey = additionalKey;
        this.additionalTTL = additionalTTL;

        if (redisCommand.getRedisDataType() == RedisDataType.HASH
                || redisCommand.getRedisDataType() == RedisDataType.SORTED_SET) {
            if (additionalKey == null) {
                throw new IllegalArgumentException("Hash and Sorted-Set should have additional key");
            }
        }

        if (redisCommand.equals(RedisCommand.SETEX)) {
            if (additionalTTL == null) {
                throw new IllegalArgumentException("SETEX command should have time to live (TTL)");
            }
        }

        if (redisCommand.equals(RedisCommand.INCRBY_EX)) {
            if (additionalTTL == null) {
                throw new IllegalArgumentException("INCRBY_EX command should have time to live (TTL)");
            }
        }

        if (redisCommand.equals(RedisCommand.DESCRBY_EX)) {
            if (additionalTTL == null) {
                throw new IllegalArgumentException("INCRBY_EX command should have time to live (TTL)");
            }
        }
    }

    /**
     * Use this constructor when data type is {@link RedisDataType#HASH} (without TTL) or {@link
     * RedisDataType#SORTED_SET}.
     * If different data type is specified, {@code additionalKey} is ignored.
     *
     * @param redisCommand the redis command type {@link RedisCommand}
     * @param additionalKey additional key for Hash and Sorted set data type
     */
    public RedisCommandDescription(RedisCommand redisCommand, String additionalKey) {
        this(redisCommand, additionalKey, null);
    }

    /**
     * Use this constructor when using SETEX command {@link RedisDataType#STRING}.
     * This command requires a TTL. Throws {@link IllegalArgumentException} if it is null.
     *
     * @param redisCommand the redis command type {@link RedisCommand}
     * @param additionalTTL additional TTL required for SETEX command
     */
    public RedisCommandDescription(RedisCommand redisCommand, Integer additionalTTL) {
        this(redisCommand, null, additionalTTL);
    }

    /**
     * Use this constructor when command type is not in group {@link RedisDataType#HASH} or {@link
     * RedisDataType#SORTED_SET}.
     *
     * @param redisCommand the redis data type {@link RedisCommand}
     */
    public RedisCommandDescription(RedisCommand redisCommand) {
        this(redisCommand, null, null);
    }

    /**
     * Returns the {@link RedisCommand}.
     *
     * @return the command type of the mapping
     */
    public RedisCommand getCommand() {
        return redisCommand;
    }

    /**
     * Returns the additional key if data type is {@link RedisDataType#HASH} and {@link RedisDataType#SORTED_SET}.
     *
     * @return the additional key
     */
    public String getAdditionalKey() {
        return additionalKey;
    }

    /**
     * Returns the additional time to live (TTL) if data type is {@link RedisDataType#HASH}.
     *
     * @return the additional TTL
     */
    public Integer getAdditionalTTL() {
        return additionalTTL;
    }
}
