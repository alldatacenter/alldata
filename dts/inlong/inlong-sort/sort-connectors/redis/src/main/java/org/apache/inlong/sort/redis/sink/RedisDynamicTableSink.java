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

package org.apache.inlong.sort.redis.sink;

import static org.apache.flink.util.TimeUtils.parseDuration;
import static org.apache.inlong.sort.redis.common.config.RedisOptions.EXPIRE_TIME;
import static org.apache.inlong.sort.redis.common.config.RedisOptions.SINK_BATCH_SIZE;
import static org.apache.inlong.sort.redis.common.config.RedisOptions.SINK_FLUSH_INTERVAL;
import static org.apache.inlong.sort.redis.common.config.SchemaMappingMode.DYNAMIC;
import static org.apache.inlong.sort.redis.common.config.SchemaMappingMode.STATIC_KV_PAIR;
import static org.apache.inlong.sort.redis.common.config.SchemaMappingMode.STATIC_PREFIX_MATCH;

import java.time.Duration;
import java.util.Map;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisConfigBase;
import org.apache.flink.streaming.connectors.redis.common.hanlder.RedisHandlerServices;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.format.EncodingFormat;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.sink.SinkFunctionProvider;
import org.apache.flink.table.data.RowData;
import org.apache.flink.types.RowKind;
import org.apache.inlong.sort.redis.common.config.RedisDataType;
import org.apache.inlong.sort.redis.common.config.SchemaMappingMode;
import org.apache.inlong.sort.redis.common.handler.InlongJedisConfigHandler;
import org.apache.inlong.sort.redis.common.schema.RedisSchema;
import org.apache.inlong.sort.redis.common.schema.RedisSchemaFactory;
import org.apache.inlong.sort.redis.common.schema.StateEncoder;

public class RedisDynamicTableSink implements DynamicTableSink {

    private final FlinkJedisConfigBase flinkJedisConfigBase;

    private final ResolvedSchema resolvedSchema;

    private final EncodingFormat<SerializationSchema<RowData>> format;

    private final RedisDataType dataType;
    private final SchemaMappingMode mappingMode;
    private final ReadableConfig config;
    private final Map<String, String> properties;

    private final String inlongMetric;
    private final String auditHostAndPorts;

    public RedisDynamicTableSink(
            EncodingFormat<SerializationSchema<RowData>> format,
            ResolvedSchema resolvedSchema,
            RedisDataType dataType,
            SchemaMappingMode schemaMappingMode,
            ReadableConfig config,
            Map<String, String> properties,
            String inlongMetric,
            String auditHostAndPorts) {
        this.format = format;
        this.resolvedSchema = resolvedSchema;
        this.dataType = dataType;
        this.mappingMode = schemaMappingMode;
        this.config = config;
        this.properties = properties;

        this.inlongMetric = inlongMetric;
        this.auditHostAndPorts = auditHostAndPorts;

        flinkJedisConfigBase = RedisHandlerServices
                .findRedisHandler(InlongJedisConfigHandler.class, properties)
                .createFlinkJedisConfig(config);

        batchSize = config.get(SINK_BATCH_SIZE);
        flushInterval = parseDuration(config.get(SINK_FLUSH_INTERVAL));
        expireTime = parseDuration(config.get(EXPIRE_TIME));
    }

    private final Duration expireTime;
    private final Long batchSize;
    private final Duration flushInterval;

    @Override
    public ChangelogMode getChangelogMode(ChangelogMode requestedMode) {
        // UPSERT mode
        ChangelogMode.Builder builder = ChangelogMode.newBuilder();
        for (RowKind kind : requestedMode.getContainedKinds()) {
            if (kind != RowKind.UPDATE_BEFORE) {
                builder.addContainedKind(kind);
            }
        }
        return builder.build();
    }

    @Override
    public SinkRuntimeProvider getSinkRuntimeProvider(Context context) {

        RedisSchema redisSchema = RedisSchemaFactory.createRedisSchema(dataType, mappingMode, resolvedSchema);
        redisSchema.validate(resolvedSchema);

        SerializationSchema serializationSchema =
                mappingMode == DYNAMIC ? null : redisSchema.getSerializationSchema(context, format);
        StateEncoder stateEncoder = redisSchema.getStateEncoder();

        RichSinkFunction<RowData> redisSinkFunction;
        switch (dataType) {
            case HASH:
                redisSinkFunction = new RedisHashSinkFunction(
                        serializationSchema,
                        stateEncoder,
                        batchSize,
                        flushInterval,
                        expireTime,
                        flinkJedisConfigBase,
                        inlongMetric,
                        auditHostAndPorts);
                break;
            case PLAIN:
                redisSinkFunction = new RedisPlainSinkFunction(
                        serializationSchema,
                        stateEncoder,
                        batchSize,
                        flushInterval,
                        expireTime,
                        flinkJedisConfigBase,
                        inlongMetric,
                        auditHostAndPorts);
                break;
            case BITMAP:
                redisSinkFunction = new RedisBitmapSinkFunction(
                        serializationSchema,
                        stateEncoder,
                        batchSize,
                        flushInterval,
                        expireTime,
                        flinkJedisConfigBase,

                        inlongMetric,
                        auditHostAndPorts);
                break;
            default:
                throw new UnsupportedOperationException();
        }

        return SinkFunctionProvider.of(redisSinkFunction);
    }

    private int getValueStartIndexInSchema(
            RedisDataType dataType,
            SchemaMappingMode mappingMode) {
        switch (dataType) {
            case PLAIN:
                return 1;
            case HASH:
                if (mappingMode == STATIC_PREFIX_MATCH) {
                    return 2;
                } else if (mappingMode == STATIC_KV_PAIR) {
                    // Serialize data without using schema
                    return 0;
                } else {
                    return 1;
                }
            case BITMAP:
                if (mappingMode == STATIC_KV_PAIR) {
                    // Serialize data without using schema
                    return 0;
                }
                return 0;
            default:
                throw new UnsupportedOperationException("The dataType '" + dataType + "' is not supported.");
        }
    }

    @Override
    public DynamicTableSink copy() {
        return new RedisDynamicTableSink(
                format,
                resolvedSchema,
                dataType,
                mappingMode,
                config,
                properties,
                inlongMetric,
                auditHostAndPorts);
    }

    @Override
    public String asSummaryString() {
        return "Redis";
    }
}
