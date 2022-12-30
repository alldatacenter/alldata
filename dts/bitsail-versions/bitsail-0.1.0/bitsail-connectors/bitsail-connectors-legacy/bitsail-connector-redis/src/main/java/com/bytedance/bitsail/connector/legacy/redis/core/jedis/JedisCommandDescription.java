/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.legacy.redis.core.jedis;

import lombok.Getter;

import java.io.Serializable;
import java.util.Objects;

/**
 * The description of the command type.
 * <p>When creating descriptor for the group of {@link JedisDataType#HASH} and {@link JedisDataType#SORTED_SET},
 * you need to use first constructor {@link #JedisCommandDescription(JedisCommand, String)}.
 * If the {@code additionalKey} is {@code null} it will throw {@code IllegalArgumentException}
 * <p>
 * If command is {@link JedisCommand#SETEX}, its required to use TTL.
 */
public class JedisCommandDescription implements Serializable {

  private static final long serialVersionUID = 1L;

  @Getter
  private JedisCommand jedisCommand;

  /**
   * This additional key is needed for the group {@link JedisDataType#HASH} and {@link JedisDataType#SORTED_SET}.
   * Other {@link JedisDataType} works only with two variable i.e. name of the list and value to be added.
   * But for {@link JedisDataType#HASH} and {@link JedisDataType#SORTED_SET} we need three variables.
   * <p>For {@link JedisDataType#HASH} we need hash name, hash key and element. Its possible to use TTL.
   * {@link #getAdditionalKey()} used as hash name for {@link JedisDataType#HASH}
   * <p>For {@link JedisDataType#SORTED_SET} we need set name, the element and it's score.
   * {@link #getAdditionalKey()} used as set name for {@link JedisDataType#SORTED_SET}
   */
  @Getter
  private String additionalKey;

  /**
   * This additional key is optional for the group {@link JedisDataType#HASH}, required for {@link JedisCommand#SETEX}.
   * For the other types and commands, its not used.
   * <p>For {@link JedisDataType#HASH} we need hash name, hash key and element. Its possible to use TTL.
   * {@link #getAdditionalTTL()} used as time to live (TTL) for {@link JedisDataType#HASH}
   * <p>For {@link JedisCommand#SETEX}, we need key, value and time to live (TTL).
   */
  @Getter
  private Integer additionalTTL;

  /**
   * Default constructor for {@link JedisCommandDescription}.
   * For {@link JedisDataType#HASH} and {@link JedisDataType#SORTED_SET} data types, {@code additionalKey} is required.
   * For {@link JedisCommand#SETEX} command, {@code additionalTTL} is required.
   * In both cases, if the respective variables are not provided, it throws an {@link IllegalArgumentException}
   *
   * @param jedisCommand  the jedis command type {@link JedisCommand}
   * @param additionalKey additional key for Hash data type
   * @param additionalTTL additional TTL optional for Hash data type
   */
  public JedisCommandDescription(JedisCommand jedisCommand, String additionalKey, Integer additionalTTL) {
    Objects.requireNonNull(jedisCommand, "Jedis command type can not be null");
    this.jedisCommand = jedisCommand;
    this.additionalKey = additionalKey;
    this.additionalTTL = additionalTTL;

    if (jedisCommand.getJedisDataType() == JedisDataType.HASH ||
        jedisCommand.getJedisDataType() == JedisDataType.SORTED_SET) {
      if (additionalKey == null) {
        throw new IllegalArgumentException("Hash and Sorted Set should have additional key");
      }
    }

    if (jedisCommand.equals(JedisCommand.SETEX)) {
      if (additionalTTL == null) {
        throw new IllegalArgumentException("SETEX command should have time to live (TTL)");
      }
    }
  }

  /**
   * Use this constructor when data type is {@link JedisDataType#HASH} (without TTL) or {@link JedisDataType#SORTED_SET}.
   * If different data type is specified, {@code additionalKey} is ignored.
   *
   * @param jedisCommand  the jedis command type {@link JedisCommand}
   * @param additionalKey additional key for Hash and Sorted set data type
   */
  public JedisCommandDescription(JedisCommand jedisCommand, String additionalKey) {
    this(jedisCommand, additionalKey, null);
  }
}

