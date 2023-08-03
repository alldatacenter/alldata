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

package org.apache.inlong.manager.service.resource.sink.redis;

import org.apache.inlong.manager.pojo.node.redis.RedisDataNodeRequest;

import lombok.Builder;
import org.apache.hadoop.hbase.exceptions.IllegalArgumentIOException;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisSentinelPool;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A client for interacting with Redis resources.
 */
public class RedisResourceClient {

    private static final String REDIS_TEST_KEY = "__inLong_test_key__";
    private static final String REDIS_TEST_VALUE = "__inLong_test_value__";
    private static final String HOST_PORT_SEPARATOR = ":";
    private static final String NODE_LINE_SEPARATOR = ",|;";

    /**
     * Test the connection to Redis.
     *
     * @param request RedisDataNodeRequest
     * @throws IOException if there is an error testing the connection
     */
    public static boolean testConnection(RedisDataNodeRequest request) throws IOException {
        String clusterMode = request.getClusterMode();
        switch (clusterMode) {
            case "standalone":
                return RedisStandaloneTester.builder().host(request.getHost()).port(request.getPort()).build()
                        .testConnection();
            case "cluster":
                return RedisClusterTester.builder().nodes(request.getClusterNodes()).build().testConnection();
            case "sentinel":
                return RedisSentinelTester.builder().masterName(request.getMasterName())
                        .sentinelsInfo(request.getSentinelsInfo()).build().testConnection();
            default:
                throw new IllegalArgumentIOException("Unknown cluster mode: " + clusterMode);
        }
    }

    /**
     * Interface for testing Redis connection.
     */
    interface RedisTester {

        /**
         * Test the connection to Redis.
         *
         * @throws IOException if there is an error testing the connection
         */
        boolean testConnection() throws IOException;
    }

    /**
     * Builder for Redis Cluster Tester.
     */
    @Builder
    static class RedisClusterTester implements RedisTester {

        private String nodes;

        @Override
        public boolean testConnection() throws IOException {
            String result;
            Set<HostAndPort> hostPorts = Arrays.stream(nodes.split(NODE_LINE_SEPARATOR)).map(s -> {
                String[] split = s.split(HOST_PORT_SEPARATOR);
                int port = Integer.parseInt(split[1]);
                return new HostAndPort(split[0], port);
            }).collect(Collectors.toSet());
            try (JedisCluster jedisCluster = new JedisCluster(hostPorts)) {
                jedisCluster.set(REDIS_TEST_KEY, REDIS_TEST_VALUE);
                result = jedisCluster.get(REDIS_TEST_KEY);
            }
            return REDIS_TEST_VALUE.equals(result);
        }
    }

    /**
     * Builder for Redis Sentinel Tester.
     */
    @Builder
    static class RedisSentinelTester implements RedisTester {

        private String masterName;
        private String sentinelsInfo;

        @Override
        public boolean testConnection() throws IOException {
            Set<String> sentinels = new HashSet<>(Arrays.asList(sentinelsInfo.split(NODE_LINE_SEPARATOR)));
            String result;
            try (JedisSentinelPool pool = new JedisSentinelPool(masterName, sentinels)) {
                Jedis jedis = pool.getResource();
                jedis.set(REDIS_TEST_KEY, REDIS_TEST_VALUE);
                result = jedis.get(REDIS_TEST_KEY);
            }
            return REDIS_TEST_VALUE.equals(result);
        }
    }

    /**
     * Builder for Redis Standalone Tester.
     */
    @Builder
    static class RedisStandaloneTester implements RedisTester {

        private String host;
        private int port;

        @Override
        public boolean testConnection() throws IOException {
            String result;
            try (Jedis jedis = new Jedis(host, port)) {
                jedis.set(REDIS_TEST_KEY, REDIS_TEST_VALUE);
                result = jedis.get(REDIS_TEST_KEY);
            }
            return REDIS_TEST_VALUE.equals(result);
        }
    }

}
