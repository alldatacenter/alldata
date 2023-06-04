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

import org.apache.flink.streaming.connectors.redis.common.container.RedisContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSentinelPool;

/**
 * The redis contain expand from {@link RedisContainer}
 */
public class InlongRedisContainer extends RedisContainer implements InlongRedisCommandsContainer {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(InlongRedisContainer.class);
    private final transient JedisPool jedisPool;
    private final transient JedisSentinelPool jedisSentinelPool;

    /**
     * Use this constructor if to connect with single Redis server.
     *
     * @param jedisPool JedisPool which actually manages Jedis instances
     */
    public InlongRedisContainer(JedisPool jedisPool) {
        super(jedisPool);
        this.jedisPool = jedisPool;
        this.jedisSentinelPool = null;
    }

    /**
     * Use this constructor if Redis environment is clustered with sentinels.
     *
     * @param sentinelPool SentinelPool which actually manages Jedis instances
     */
    public InlongRedisContainer(JedisSentinelPool sentinelPool) {
        super(sentinelPool);
        this.jedisPool = null;
        this.jedisSentinelPool = sentinelPool;
    }

    @Override
    public void del(String key) {
        Jedis jedis = null;
        try {
            jedis = getInstance();
            jedis.del(key);
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Cannot send Redis with del command to key {}  error message {}",
                        key, e.getMessage());
            }
            throw e;
        } finally {
            releaseInstance(jedis);
        }
    }

    @Override
    public void hdel(String key, String hashField) {
        Jedis jedis = null;
        try {
            jedis = getInstance();
            jedis.hdel(key, hashField);
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Cannot send Redis with hdel command to key {} of field {}  error message {}",
                        key, hashField, e.getMessage());
            }
            throw e;
        } finally {
            releaseInstance(jedis);
        }
    }

    @Override
    public String get(String key) {
        Jedis jedis = null;
        try {
            jedis = getInstance();
            return jedis.get(key);
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Cannot get value with get command from key {} error message {}", key, e.getMessage());
            }
            throw e;
        } finally {
            releaseInstance(jedis);
        }
    }

    @Override
    public String hget(String key, String hashField) {
        Jedis jedis = null;
        try {
            jedis = getInstance();
            return jedis.hget(key, hashField);
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Cannot get value with hget command from key {} of field {}  error message {}",
                        key, hashField, e.getMessage());
            }
            throw e;
        } finally {
            releaseInstance(jedis);
        }
    }

    @Override
    public Double zscore(String key, String member) {
        Jedis jedis = null;
        try {
            jedis = getInstance();
            return jedis.zscore(key, member);
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Cannot get value with zscore command from key {} of member {}  error message {}",
                        key, member, e.getMessage());
            }
            throw e;
        } finally {
            releaseInstance(jedis);
        }
    }

    @Override
    public Long zrevrank(String key, String member) {
        Jedis jedis = null;
        try {
            jedis = getInstance();
            return jedis.zrevrank(key, member);
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Cannot get value with zrevrank command from key {} of member {}  error message {}",
                        key, member, e.getMessage());
            }
            throw e;
        } finally {
            releaseInstance(jedis);
        }
    }

    @Override
    public void setBit(String key, Long offset, Boolean value) {
        Jedis jedis = null;
        try {
            jedis = getInstance();
            jedis.setbit(key, offset, value);
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Cannot properly execute setBit command, key: {}, offset: {}, value:{}, error message {}",
                        key, offset, value, e.getMessage());
            }
            throw e;
        } finally {
            releaseInstance(jedis);
        }
    }

    public Jedis getInstance() {
        if (jedisSentinelPool != null) {
            return jedisSentinelPool.getResource();
        } else {
            return jedisPool.getResource();
        }
    }

    /**
     * Closes the jedis instance after finishing the command.
     *
     * @param jedis The jedis instance
     */
    public void releaseInstance(final Jedis jedis) {
        if (jedis == null) {
            return;
        }
        try {
            jedis.close();
        } catch (Exception e) {
            LOG.error("Failed to close (return) instance to pool", e);
        }
    }
}
