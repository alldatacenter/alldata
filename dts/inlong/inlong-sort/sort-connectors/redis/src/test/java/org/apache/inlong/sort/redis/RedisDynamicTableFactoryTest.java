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

import static org.apache.flink.table.factories.utils.FactoryMocks.createTableSink;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.catalog.Column;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.sink.SinkFunctionProvider;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.runtime.connector.sink.SinkRuntimeProviderContext;
import org.apache.inlong.sort.redis.sink.RedisBitmapSinkFunction;
import org.apache.inlong.sort.redis.sink.RedisDynamicTableSink;
import org.apache.inlong.sort.redis.sink.RedisHashSinkFunction;
import org.apache.inlong.sort.redis.sink.RedisPlainSinkFunction;
import org.junit.Test;

public class RedisDynamicTableFactoryTest {

    private static final ResolvedSchema TEST_SCHEMA = ResolvedSchema.of(
            Column.physical("key", DataTypes.STRING()),
            Column.physical("f1", DataTypes.BIGINT()),
            Column.physical("f2", DataTypes.INT()),
            Column.physical("f3", DataTypes.FLOAT()),
            Column.physical("f4", DataTypes.DOUBLE()),
            Column.physical("f5", DataTypes.BOOLEAN()));

    private Map<String, String> getProperties() {
        Map<String, String> properties = new HashMap<>();

        properties.put("connector", "redis-inlong");

        properties.put("schema.0.name", "key");
        properties.put("schema.0.type", "STRING");
        properties.put("schema.1.name", "f1");
        properties.put("schema.1.type", "BIGINT");
        properties.put("schema.2.name", "f2");
        properties.put("schema.2.type", "INT");
        properties.put("schema.3.name", "f3");
        properties.put("schema.3.type", "FLOAT");
        properties.put("schema.4.name", "f4");
        properties.put("schema.4.type", "DOUBLE");
        properties.put("schema.5.name", "f5");
        properties.put("schema.5.type", "BOOLEAN");

        properties.put("format", "csv");
        properties.put("format.property-version", "1");
        properties.put("format.derive-schema", "true");

        // cluster mode redis
        properties.put("redis-mode", "cluster");
        properties.put("cluster-nodes", "127.0.0.1:6379,127.0.0.1:6378");
        properties.put("password", "password");
        properties.put("maxIdle", "8");
        properties.put("minIdle", "1");
        properties.put("maxTotal", "2");
        properties.put("timeout", "2000");

        return properties;
    }

    @Test
    public void testCreateTableSink() {
        Map<String, String> properties = getProperties();

        DynamicTableSink tableSink = createTableSink(TEST_SCHEMA, properties);
        assertTrue(tableSink instanceof RedisDynamicTableSink);
    }

    @Test
    public void testCreateHashSinkFunction() {
        Map<String, String> properties = getProperties();
        properties.put("data-type", "HASH");
        DynamicTableSink tableSink = createTableSink(TEST_SCHEMA, properties);

        DynamicTableSink.SinkRuntimeProvider sinkRuntimeProvider =
                tableSink.getSinkRuntimeProvider(new SinkRuntimeProviderContext(false));
        assertTrue(sinkRuntimeProvider instanceof SinkFunctionProvider);

        SinkFunction<RowData> sinkFunction = ((SinkFunctionProvider) sinkRuntimeProvider).createSinkFunction();
        assertTrue(sinkFunction instanceof RedisHashSinkFunction);
    }

    @Test
    public void testCreateBitmapSinkFunction() {

        ResolvedSchema TEST_SCHEMA = ResolvedSchema.of(
                Column.physical("key", DataTypes.STRING()),
                Column.physical("f1", DataTypes.BIGINT()),
                Column.physical("f2", DataTypes.BOOLEAN()),
                Column.physical("f3", DataTypes.BIGINT()),
                Column.physical("f4", DataTypes.BOOLEAN()),
                Column.physical("f5", DataTypes.BIGINT()),
                Column.physical("f6", DataTypes.BOOLEAN()));

        Map<String, String> properties = getProperties();
        properties.put("data-type", "BITMAP");
        properties.put("schema-mapping-mode", "STATIC_KV_PAIR");

        DynamicTableSink tableSink = createTableSink(TEST_SCHEMA, properties);

        DynamicTableSink.SinkRuntimeProvider sinkRuntimeProvider =
                tableSink.getSinkRuntimeProvider(new SinkRuntimeProviderContext(false));
        assertTrue(sinkRuntimeProvider instanceof SinkFunctionProvider);

        SinkFunction<RowData> sinkFunction = ((SinkFunctionProvider) sinkRuntimeProvider).createSinkFunction();
        assertTrue(sinkFunction instanceof RedisBitmapSinkFunction);
    }

    @Test
    public void testCreatePlainSinkFunction() {
        Map<String, String> properties = getProperties();
        properties.put("data-type", "PLAIN");
        properties.put("schema-mapping-mode", "STATIC_PREFIX_MATCH");
        DynamicTableSink tableSink = createTableSink(TEST_SCHEMA, properties);

        DynamicTableSink.SinkRuntimeProvider sinkRuntimeProvider =
                tableSink.getSinkRuntimeProvider(new SinkRuntimeProviderContext(false));
        assertTrue(sinkRuntimeProvider instanceof SinkFunctionProvider);

        SinkFunction<RowData> sinkFunction = ((SinkFunctionProvider) sinkRuntimeProvider).createSinkFunction();
        assertTrue(sinkFunction instanceof RedisPlainSinkFunction);
    }
}
