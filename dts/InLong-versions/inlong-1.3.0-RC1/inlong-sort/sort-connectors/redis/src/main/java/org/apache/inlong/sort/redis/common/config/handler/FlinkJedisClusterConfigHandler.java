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
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisClusterConfig;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisConfigBase;
import org.apache.flink.util.Preconditions;
import org.apache.inlong.sort.redis.common.config.RedisOptions;
import org.apache.inlong.sort.redis.common.handler.InlongJedisConfigHandler;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import static org.apache.flink.streaming.connectors.redis.descriptor.RedisValidator.REDIS_CLUSTER;
import static org.apache.flink.streaming.connectors.redis.descriptor.RedisValidator.REDIS_MODE;

/**
 * jedis cluster config handler to find and create jedis cluster config use meta and
 * copy from {@link org.apache.flink.streaming.connectors.redis.common.config.handler.FlinkJedisClusterConfigHandler}
 */
public class FlinkJedisClusterConfigHandler implements InlongJedisConfigHandler {

    public FlinkJedisClusterConfigHandler() {
    }

    @Override
    public FlinkJedisConfigBase createFlinkJedisConfig(ReadableConfig config) {
        String nodesInfo = config.get(RedisOptions.CLUSTER_NODES);
        Preconditions.checkNotNull(nodesInfo, "nodes should not be null");
        Set<InetSocketAddress> nodes = Arrays.stream(nodesInfo.split(",")).map(r -> {
            String[] arr = r.split(":");
            return new InetSocketAddress(arr[0].trim(), Integer.parseInt(arr[1].trim()));
        }).collect(Collectors.toSet());
        FlinkJedisClusterConfig.Builder builder = new FlinkJedisClusterConfig.Builder()
                .setNodes(nodes).setPassword(config.get(RedisOptions.PASSWORD));
        builder.setMaxIdle(config.get(RedisOptions.MAXIDLE))
                .setMinIdle(config.get(RedisOptions.MINIDLE))
                .setMaxTotal(config.get(RedisOptions.MAX_TOTAL))
                .setTimeout(config.get(RedisOptions.TIMEOUT));
        return builder.build();
    }

    @Override
    public Map<String, String> requiredContext() {
        Map<String, String> require = new HashMap<>();
        require.put(REDIS_MODE, REDIS_CLUSTER);
        return require;
    }
}
