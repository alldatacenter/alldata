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

package org.apache.inlong.sort.redis.source;

import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisConfigBase;
import org.apache.flink.streaming.connectors.redis.common.hanlder.RedisHandlerServices;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.connector.source.LookupTableSource;
import org.apache.flink.table.connector.source.TableFunctionProvider;
import org.apache.flink.table.types.logical.LogicalTypeRoot;
import org.apache.flink.util.Preconditions;
import org.apache.inlong.sort.redis.common.config.RedisLookupOptions;
import org.apache.inlong.sort.redis.common.config.RedisOptions;
import org.apache.inlong.sort.redis.common.handler.InlongJedisConfigHandler;
import org.apache.inlong.sort.redis.common.handler.RedisMapperHandler;
import org.apache.inlong.sort.redis.common.mapper.RedisCommand;
import org.apache.inlong.sort.redis.common.mapper.RedisMapper;
import org.apache.inlong.sort.redis.table.SchemaValidator;

import java.util.Map;
import static org.apache.flink.table.types.logical.LogicalTypeRoot.BIGINT;
import static org.apache.flink.table.types.logical.LogicalTypeRoot.DOUBLE;
import static org.apache.flink.table.types.logical.LogicalTypeRoot.VARCHAR;

/**
 * Redis dynamic table source
 */
public class RedisDynamicTableSource implements LookupTableSource {

    private final FlinkJedisConfigBase flinkJedisConfigBase;

    private final RedisMapper redisMapper;
    private final ResolvedSchema tableSchema;
    private final ReadableConfig config;
    private final RedisLookupOptions redisLookupOptions;
    private final Map<String, String> properties;

    public RedisDynamicTableSource(Map<String, String> properties, ResolvedSchema tableSchema,
            ReadableConfig config, RedisLookupOptions redisLookupOptions) {
        this.properties = properties;
        Preconditions.checkNotNull(properties, "properties should not be null");
        this.tableSchema = tableSchema;
        Preconditions.checkNotNull(tableSchema, "tableSchema should not be null");
        this.config = config;
        properties.putIfAbsent(RedisOptions.REDIS_MODE.key(), config.get(RedisOptions.REDIS_MODE));
        redisMapper = RedisHandlerServices
                .findRedisHandler(RedisMapperHandler.class, properties)
                .createRedisMapper(properties);
        RedisCommand command = redisMapper.getCommandDescription().getCommand();
        new SchemaValidator()
                .register(RedisCommand.GET, new LogicalTypeRoot[]{VARCHAR, VARCHAR})
                .register(RedisCommand.HGET, new LogicalTypeRoot[]{VARCHAR, VARCHAR})
                .register(RedisCommand.ZSCORE, new LogicalTypeRoot[]{VARCHAR, DOUBLE})
                .register(RedisCommand.ZREVRANK, new LogicalTypeRoot[]{VARCHAR, BIGINT})
                .validate(command, tableSchema);
        flinkJedisConfigBase = RedisHandlerServices
                .findRedisHandler(InlongJedisConfigHandler.class, properties).createFlinkJedisConfig(config);
        this.redisLookupOptions = redisLookupOptions;
    }

    @Override
    public DynamicTableSource copy() {
        return new RedisDynamicTableSource(properties, tableSchema, config, redisLookupOptions);
    }

    @Override
    public String asSummaryString() {
        return "REDIS";
    }

    @Override
    public LookupRuntimeProvider getLookupRuntimeProvider(LookupContext context) {
        return TableFunctionProvider.of(new RedisRowDataLookupFunction(
                redisMapper.getCommandDescription(), flinkJedisConfigBase, this.redisLookupOptions));
    }
}
