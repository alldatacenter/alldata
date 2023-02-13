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

package org.apache.inlong.sort.redis.common.config.handler;

import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisConfigBase;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisSentinelConfig;
import org.apache.inlong.sort.redis.common.config.RedisOptions;
import org.apache.inlong.sort.redis.common.handler.InlongJedisConfigHandler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import static org.apache.flink.streaming.connectors.redis.descriptor.RedisValidator.REDIS_MODE;
import static org.apache.flink.streaming.connectors.redis.descriptor.RedisValidator.REDIS_SENTINEL;

/**
 * Jedis sentinel config handler to find and create jedis Sentinel config use meta and
 * copy from {@link org.apache.flink.streaming.connectors.redis.common.config.handler.FlinkJedisSentinelConfigHandler}
 */
public class FlinkJedisSentinelConfigHandler implements InlongJedisConfigHandler {

    public FlinkJedisSentinelConfigHandler() {

    }

    @Override
    public FlinkJedisConfigBase createFlinkJedisConfig(ReadableConfig config) {
        String masterName = config.get(RedisOptions.REDIS_MASTER_NAME);
        String sentinelsInfo = config.get(RedisOptions.SENTINELS_INFO);
        Objects.requireNonNull(masterName, "master should not be null in sentinel mode");
        Objects.requireNonNull(sentinelsInfo, "sentinels should not be null in sentinel mode");
        Set<String> sentinels = new HashSet<>(Arrays.asList(sentinelsInfo.split(",")));
        String sentinelsPassword = config.get(RedisOptions.PASSWORD);
        return new FlinkJedisSentinelConfig.Builder()
                .setMasterName(masterName).setSentinels(sentinels).setPassword(sentinelsPassword)
                .setMaxIdle(config.get(RedisOptions.MAXIDLE))
                .setMinIdle(config.get(RedisOptions.MINIDLE))
                .setMaxTotal(config.get(RedisOptions.MAX_TOTAL))
                .setDatabase(config.get(RedisOptions.DATABASE))
                .setConnectionTimeout(config.get(RedisOptions.TIMEOUT))
                .setSoTimeout(config.get(RedisOptions.SOCKET_TIMEOUT)).build();
    }

    @Override
    public Map<String, String> requiredContext() {
        Map<String, String> require = new HashMap<>();
        require.put(REDIS_MODE, REDIS_SENTINEL);
        return require;
    }
}
