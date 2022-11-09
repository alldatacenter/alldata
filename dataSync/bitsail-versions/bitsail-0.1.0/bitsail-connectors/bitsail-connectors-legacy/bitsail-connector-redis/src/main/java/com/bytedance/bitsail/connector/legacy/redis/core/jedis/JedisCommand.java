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
import org.apache.flink.api.common.typeinfo.PrimitiveArrayTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;

/**
 * All available commands for Redis. Each command belongs to a {@link JedisDataType} group.
 * https://github.com/apache/bahir-flink/tree/master/flink-connector-redis
 */
public enum JedisCommand {

  /**
   * Unsupported because it's not idempotent.
   * Insert the specified value at the head of the list stored at key.
   * If key does not exist, it is created as empty list before performing the push operations.
   */
  LPUSH(JedisDataType.LIST, 2),

  /**
   * Unsupported because it's not idempotent.
   * Insert the specified value at the tail of the list stored at key.
   * If key does not exist, it is created as empty list before performing the push operation.
   */
  RPUSH(JedisDataType.LIST, 2),

  /**
   * Add the specified member to the set stored at key.
   * Specified member that is already a member of this set is ignored.
   */
  SADD(JedisDataType.SET, 2),

  /**
   * Set key to hold the string value. If key already holds a value,
   * it is overwritten, regardless of its type.
   */
  SET(JedisDataType.STRING, 2),

  /**
   * Set key to hold the string value, with a time to live (TTL). If key already holds a value,
   * it is overwritten, regardless of its type.
   */
  SETEX(JedisDataType.STRING, 2),

  /**
   * todo: support this operation.
   * Adds the element to the HyperLogLog data structure stored at the variable name specified as first argument.
   */
  PFADD(JedisDataType.HYPER_LOG_LOG, 2),

  /**
   * todo: support this operation.
   * Posts a message to the given channel.
   */
  PUBLISH(JedisDataType.PUBSUB, 2),

  /**
   * Adds the specified members with the specified score to the sorted set stored at key.
   */
  ZADD(JedisDataType.SORTED_SET, 3),

  /**
   * todo: support this operation.
   * Removes the specified members from the sorted set stored at key.
   */
  ZREM(JedisDataType.SORTED_SET, 3),

  /**
   * Sets field in the hash stored at key to value. If key does not exist,
   * a new key holding a hash is created. If field already exists in the hash, it is overwritten.
   */
  HSET(JedisDataType.HASH, 3);

  /**
   * The {@link JedisDataType} this command belongs to.
   */
  @Getter
  private JedisDataType jedisDataType;

  @Getter
  private final int columnSize;
  @Getter
  private final int keyIndex;
  @Getter
  private final int valueIndex;
  @Getter
  private final int defaultScoreOrHashKeyIndex;
  @Getter
  private final RowTypeInfo rowTypeInfo;

  JedisCommand(JedisDataType jedisDataType, int columnSize) {
    this.jedisDataType = jedisDataType;
    this.columnSize = columnSize;
    this.keyIndex = 0;
    this.valueIndex = columnSize - 1;
    this.defaultScoreOrHashKeyIndex = 1;
    TypeInformation[] typeInformations = new TypeInformation[columnSize];
    for (int i = 0; i < columnSize; i++) {
      typeInformations[i] = PrimitiveArrayTypeInfo.BYTE_PRIMITIVE_ARRAY_TYPE_INFO;
    }
    this.rowTypeInfo = new RowTypeInfo(typeInformations);
  }
}

