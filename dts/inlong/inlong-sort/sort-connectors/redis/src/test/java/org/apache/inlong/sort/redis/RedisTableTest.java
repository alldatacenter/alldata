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

package org.apache.inlong.sort.redis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;
import org.apache.flink.util.NetUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.embedded.RedisServer;

public class RedisTableTest {

    private static int redisPort;

    private static RedisServer redisServer;

    @BeforeClass
    public static void setup() {
        redisPort = NetUtils.getAvailablePort();
        redisServer = RedisServer.builder().setting("maxmemory 128m").port(redisPort).build();
        redisServer.start();
    }

    @AfterClass
    public static void cleanup() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }

    @Before
    public void prepare() {
        Jedis jedis = new Jedis("localhost", redisPort);
        // Deletes all keys from all databases.
        jedis.flushAll();
    }

    @Test
    public void testSinkWithPlain() throws Exception {
        StreamExecutionEnvironment executionEnv =
                StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tableEnv =
                StreamTableEnvironment.create(executionEnv);

        executionEnv.setParallelism(1);

        String address = "localhost:" + redisPort;

        DataStream<Row> source =
                executionEnv.fromCollection(
                        Arrays.asList(
                                Row.of("1", "r12", 1.2, 1),
                                Row.of("2", "r22", 2.2, 2),
                                Row.of("3", "r32", 3.2, 3)));
        tableEnv.registerDataStream("source", source, "aaa, bbb, ccc, ddd");

        tableEnv.executeSql("CREATE TABLE sink (" +
                "    key STRING," +
                "    aaa STRING," +
                "    bbb DOUBLE," +
                "    ccc BIGINT," +
                "    PRIMARY KEY (`key`) NOT ENFORCED" +
                ") WITH (" +
                "  'connector' = 'redis-inlong'," +
                "  'sink.batch-size' = '1'," +
                "  'format' = 'csv'," +
                "  'data-type' = 'PLAIN'," +
                "  'redis-mode' = 'standalone'," +
                "  'host' = 'localhost'," +
                "  'port' = '" + redisPort + "'," +
                "  'maxIdle' = '8'," +
                "  'minIdle' = '1'," +
                "  'maxTotal' = '2'," +
                "  'timeout' = '2000'" +
                ")");
        Jedis jedis = new Jedis("localhost", redisPort);
        assertNull(jedis.get("1"));
        assertNull(jedis.get("2"));
        assertNull(jedis.get("3"));

        String query = "INSERT INTO sink SELECT * FROM source";
        tableEnv.executeSql(query);

        Thread.sleep(4000);

        assertEquals("r12,1.2,1", jedis.get("1"));
        assertEquals("r22,2.2,2", jedis.get("2"));
        assertEquals("r32,3.2,3", jedis.get("3"));
    }

    @Test
    public void testSinkWithHashPrefixMatch() throws Exception {
        StreamExecutionEnvironment executionEnv =
                StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tableEnv =
                StreamTableEnvironment.create(executionEnv);

        executionEnv.setParallelism(1);

        String address = "localhost:" + redisPort;

        DataStream<Row> source =
                executionEnv.fromCollection(
                        Arrays.asList(
                                Row.of("1", "r12", 1.2, 1),
                                Row.of("2", "r22", 2.2, 2),
                                Row.of("3", "r32", 3.2, 3)));
        tableEnv.registerDataStream("source", source, "aaa, bbb, ccc, ddd");

        tableEnv.executeSql("CREATE TABLE sink (" +
                "    key STRING," +
                "    aaa STRING," +
                "    bbb DOUBLE," +
                "    ccc BIGINT," +
                "    PRIMARY KEY (`key`) NOT ENFORCED" +
                ") WITH (" +
                "  'connector' = 'redis-inlong'," +
                "  'sink.batch-size' = '1'," +
                "  'format' = 'csv'," +
                "  'data-type' = 'HASH'," +
                "  'redis-mode' = 'standalone'," +
                "  'host' = 'localhost'," +
                "  'port' = '" + redisPort + "'," +
                "  'maxIdle' = '8'," +
                "  'minIdle' = '1'," +
                "  'maxTotal' = '2'," +
                "  'timeout' = '2000'" +
                ")");
        Jedis jedis = new Jedis("localhost", redisPort);
        assertNull(jedis.get("1"));
        assertNull(jedis.get("2"));
        assertNull(jedis.get("3"));

        String query = "INSERT INTO sink SELECT * FROM source";
        tableEnv.executeSql(query);

        Thread.sleep(4000);

        assertEquals("1.2,1", jedis.hget("1", "r12"));
        assertEquals("2.2,2", jedis.hget("2", "r22"));
        assertEquals("3.2,3", jedis.hget("3", "r32"));
    }

    @Test
    public void testSinkWithHashKvPair() throws Exception {
        StreamExecutionEnvironment executionEnv =
                StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tableEnv =
                StreamTableEnvironment.create(executionEnv);

        executionEnv.setParallelism(1);

        String address = "localhost:" + redisPort;

        DataStream<Row> source =
                executionEnv.fromCollection(
                        Arrays.asList(
                                Row.of("1", "r12", 1.2, "r14", 1),
                                Row.of("2", "r22", 2.2, "r24", 2),
                                Row.of("3", "r32", 3.2, "r34", 3)));
        tableEnv.registerDataStream("source", source, "aaa, bbb, ccc, ddd,eee");

        tableEnv.executeSql("CREATE TABLE sink (" +
                "    key STRING," +
                "    aaa STRING," +
                "    bbb STRING," +
                "    ccc STRING," +
                "    ddd STRING," +
                "    PRIMARY KEY (`key`) NOT ENFORCED" +
                ") WITH (" +
                "  'connector' = 'redis-inlong'," +
                "  'sink.batch-size' = '1'," +
                "  'format' = 'csv'," +
                "  'data-type' = 'HASH'," +
                "  'schema-mapping-mode' = 'STATIC_KV_PAIR'," +
                "  'redis-mode' = 'standalone'," +
                "  'host' = 'localhost'," +
                "  'port' = '" + redisPort + "'," +
                "  'maxIdle' = '8'," +
                "  'minIdle' = '1'," +
                "  'maxTotal' = '2'," +
                "  'timeout' = '2000'" +
                ")");
        Jedis jedis = new Jedis("localhost", redisPort);
        assertNull(jedis.get("1"));
        assertNull(jedis.get("2"));
        assertNull(jedis.get("3"));

        String query = "INSERT INTO sink SELECT aaa,bbb,cast(ccc as STRING),ddd, cast(eee as STRING) FROM source";
        tableEnv.executeSql(query);

        Thread.sleep(4000);

        assertEquals("1.2", jedis.hget("1", "r12"));
        assertEquals("2.2", jedis.hget("2", "r22"));
        assertEquals("3.2", jedis.hget("3", "r32"));
        assertEquals("1", jedis.hget("1", "r14"));
        assertEquals("2", jedis.hget("2", "r24"));
        assertEquals("3", jedis.hget("3", "r34"));
    }

    @Test
    public void testSinkWithDynamic() throws Exception {
        StreamExecutionEnvironment executionEnv =
                StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tableEnv =
                StreamTableEnvironment.create(executionEnv);

        executionEnv.setParallelism(1);

        String address = "localhost:" + redisPort;

        DataStream<Row> source =
                executionEnv.fromCollection(
                        Arrays.asList(
                                Row.of("1", "r12", 1.2, "r14", 1),
                                Row.of("2", "r22", 2.2, "r24", 2),
                                Row.of("3", "r32", 3.2, "r34", 3)));
        tableEnv.registerDataStream("source", source, "aaa, bbb, ccc, ddd,eee");

        tableEnv.executeSql("CREATE TABLE sink (" +
                "    key STRING," +
                "    aaa MAP<STRING,STRING>," +
                "    PRIMARY KEY (`key`) NOT ENFORCED" +
                ") WITH (" +
                "  'connector' = 'redis-inlong'," +
                "  'sink.batch-size' = '1'," +
                "  'format' = 'csv'," +
                "  'data-type' = 'HASH'," +
                "  'schema-mapping-mode' = 'DYNAMIC'," +
                "  'redis-mode' = 'standalone'," +
                "  'host' = 'localhost'," +
                "  'port' = '" + redisPort + "'," +
                "  'maxIdle' = '8'," +
                "  'minIdle' = '1'," +
                "  'maxTotal' = '2'," +
                "  'timeout' = '2000'" +
                ")");
        Jedis jedis = new Jedis("localhost", redisPort);
        assertNull(jedis.get("1"));
        assertNull(jedis.get("2"));
        assertNull(jedis.get("3"));

        String query = "INSERT INTO sink "
                + "SELECT "
                + "aaa as key, "
                + "MAP[bbb,cast(ccc as STRING),ddd,cast(eee as STRING)] as fv "
                + "FROM source";
        tableEnv.executeSql(query);

        Thread.sleep(4000);

        assertEquals("1.2", jedis.hget("1", "r12"));
        assertEquals("2.2", jedis.hget("2", "r22"));
        assertEquals("3.2", jedis.hget("3", "r32"));
        assertEquals("1", jedis.hget("1", "r14"));
        assertEquals("2", jedis.hget("2", "r24"));
        assertEquals("3", jedis.hget("3", "r34"));
    }

    @Test
    public void testSinkWithBitmap() throws Exception {
        StreamExecutionEnvironment executionEnv =
                StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tableEnv =
                StreamTableEnvironment.create(executionEnv);

        executionEnv.setParallelism(1);

        String address = "localhost:" + redisPort;

        DataStream<Row> source =
                executionEnv.fromCollection(
                        Arrays.asList(
                                Row.of("1", 2, 1.2, 4, 1),
                                Row.of("2", 2, 2.2, 4, 2),
                                Row.of("3", 2, 3.2, 4, 3)));
        tableEnv.registerDataStream("source", source, "aaa, bbb, ccc, ddd,eee");

        tableEnv.executeSql("CREATE TABLE sink (" +
                "    key STRING," +
                "    aaa BIGINT," +
                "    bbb BOOLEAN," +
                "    ccc BIGINT," +
                "    ddd BOOLEAN," +
                "    PRIMARY KEY (`key`) NOT ENFORCED" +
                ") WITH (" +
                "  'connector' = 'redis-inlong'," +
                "  'sink.batch-size' = '1'," +
                "  'format' = 'csv'," +
                "  'data-type' = 'BITMAP'," +
                "  'schema-mapping-mode' = 'STATIC_KV_PAIR'," +
                "  'redis-mode' = 'standalone'," +
                "  'host' = 'localhost'," +
                "  'port' = '" + redisPort + "'," +
                "  'maxIdle' = '8'," +
                "  'minIdle' = '1'," +
                "  'maxTotal' = '2'," +
                "  'timeout' = '2000'" +
                ")");
        Jedis jedis = new Jedis("localhost", redisPort);
        assertNull(jedis.get("1"));
        assertNull(jedis.get("2"));
        assertNull(jedis.get("3"));

        String query = "INSERT INTO sink "
                + "SELECT "
                + "aaa as key, "
                + "bbb,"
                + "ccc = cast(1.2 as double) as cccc,"
                + "ddd,"
                + "eee = cast(1 as bigint) as eeee "
                + "FROM source";
        tableEnv.executeSql(query);

        Thread.sleep(4000);

        assertTrue(jedis.getbit("1", 2));
        assertTrue(jedis.getbit("1", 4));

        assertFalse(jedis.getbit("2", 2));
        assertFalse(jedis.getbit("2", 4));

        assertFalse(jedis.getbit("3", 2));
        assertFalse(jedis.getbit("3", 4));
    }

}
