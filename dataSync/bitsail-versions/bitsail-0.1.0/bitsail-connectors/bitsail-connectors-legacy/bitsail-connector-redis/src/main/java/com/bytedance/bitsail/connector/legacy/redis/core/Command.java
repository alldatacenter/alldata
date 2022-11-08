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

package com.bytedance.bitsail.connector.legacy.redis.core;

import com.bytedance.bitsail.connector.legacy.redis.core.jedis.JedisCommand;
import com.bytedance.bitsail.connector.legacy.redis.core.jedis.JedisCommandDescription;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
public class Command {
  private JedisCommand jedisCommand;
  private byte[] key;
  private byte[] hashField;
  private double score;
  private byte[] value;
  private int ttlInSeconds;

  public Command(JedisCommandDescription commandDescription, byte[] key, byte[] hashField, byte[] value) {
    this(commandDescription, key, value);
    this.hashField = hashField;
  }

  public Command(JedisCommandDescription commandDescription, byte[] key, double score, byte[] value) {
    this(commandDescription, key, value);
    this.score = score;
  }

  public Command(JedisCommandDescription commandDescription, byte[] key, byte[] value) {
    this.jedisCommand = commandDescription.getJedisCommand();
    this.key = key;
    this.value = value;
    this.ttlInSeconds = commandDescription.getAdditionalTTL() == null ? 0 : commandDescription.getAdditionalTTL();
  }

  public String print() {
    switch (jedisCommand) {
      case SET:
      case SETEX:
        return new String(key);
      case SADD:
      case ZADD:
        return new String(key) + ":" + new String(value);
      case HSET:
        return new String(key) + ":" + new String(hashField);
      default:
        return StringUtils.EMPTY;
    }
  }
}

