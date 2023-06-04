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

package org.apache.inlong.sort.redis.common.container;

import org.apache.flink.streaming.connectors.redis.common.container.RedisClusterContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisCluster;

/**
 * The redis cluster contain expand from {@link RedisClusterContainer}
 */
public class InlongRedisClusterContainer extends RedisClusterContainer implements InlongRedisCommandsContainer {

    private static final Logger LOG = LoggerFactory.getLogger(InlongRedisClusterContainer.class);

    private final transient JedisCluster jedisCluster;

    public InlongRedisClusterContainer(JedisCluster jedisCluster) {
        super(jedisCluster);
        this.jedisCluster = jedisCluster;
    }

    @Override
    public void del(String key) {
        try {
            jedisCluster.del(key);
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Cannot send Redis message with command del to key {}  error message {}",
                        key, e.getMessage());
            }
            throw e;
        }
    }

    @Override
    public void hdel(String key, String hashField) {
        try {
            jedisCluster.hdel(key, hashField);
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Cannot send Redis message with command hdel to key {} of field {}   error message {}",
                        key, hashField, e.getMessage());
            }
            throw e;
        }
    }

    @Override
    public String get(String key) {
        try {
            return jedisCluster.get(key);
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Cannot get value with get command from key {} error message {}", key, e.getMessage());
            }
            throw e;
        }
    }

    @Override
    public String hget(String key, String hashField) {
        try {
            return jedisCluster.hget(key, hashField);
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Cannot get value with hget command from key {} of field {}  error message {}",
                        key, hashField, e.getMessage());
            }
            throw e;
        }
    }

    @Override
    public Double zscore(String key, String member) {
        try {
            return jedisCluster.zscore(key, member);
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Cannot get value with zscore command from key {} of member {}  error message {}",
                        key, member, e.getMessage());
            }
            throw e;
        }
    }

    @Override
    public Long zrevrank(String key, String member) {
        try {
            return jedisCluster.zrevrank(key, member);
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Cannot get value with zrevrank command from key {} of member {}  error message {}",
                        key, member, e.getMessage());
            }
            throw e;
        }
    }

    @Override
    public void setBit(String key, Long offset, Boolean value) {
        try {
            jedisCluster.setbit(key, offset, value);
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error(
                        "Cannot send Redis message with command setBit, key: {}, offset: {}, value: {}, error message {}",
                        key, offset, value, e.getMessage());
            }
            throw e;
        }
    }
}
