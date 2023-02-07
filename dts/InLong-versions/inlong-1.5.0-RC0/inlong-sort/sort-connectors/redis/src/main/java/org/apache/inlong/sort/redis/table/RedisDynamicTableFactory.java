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

package org.apache.inlong.sort.redis.table;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.streaming.connectors.redis.descriptor.RedisValidator;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.factories.DynamicTableSourceFactory;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.flink.util.Preconditions;
import org.apache.flink.util.StringUtils;
import org.apache.inlong.sort.redis.common.config.RedisLookupOptions;
import org.apache.inlong.sort.redis.common.config.RedisOptions;
import org.apache.inlong.sort.redis.common.descriptor.InlongRedisValidator;
import org.apache.inlong.sort.redis.common.mapper.RedisCommand;
import org.apache.inlong.sort.redis.source.RedisDynamicTableSource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import static org.apache.flink.util.Preconditions.checkState;
import static org.apache.inlong.sort.redis.common.config.RedisOptions.LOOKUP_ASYNC;
import static org.apache.inlong.sort.redis.common.config.RedisOptions.LOOKUP_CACHE_MAX_ROWS;
import static org.apache.inlong.sort.redis.common.config.RedisOptions.LOOKUP_CACHE_TTL;
import static org.apache.inlong.sort.redis.common.config.RedisOptions.LOOKUP_MAX_RETRIES;

/**
 * Redis dynamic table factory
 */
public class RedisDynamicTableFactory implements DynamicTableSourceFactory {

    /**
     * The identifier of Redis Connector
     */
    public static final String IDENTIFIER = "redis-inlong";
    /**
     * Supported redis mode, contains [standalone|cluster|sentinel].
     */
    public static final Set<String> SUPPORT_REDIS_MODE = new HashSet<String>() {

        private static final long serialVersionUID = 1L;

        {
            add(RedisValidator.REDIS_CLUSTER);
            add(RedisValidator.REDIS_SENTINEL);
            add(InlongRedisValidator.REDIS_STANDALONE);
        }
    };
    /**
     * Supported redis source commands, contain [GET|HGET|ZREVRANK|ZSCORE] at now.
     */
    public static Set<String> SUPPORT_SOURCE_COMMANDS = new HashSet<String>() {

        private static final long serialVersionUID = 1L;

        {
            add(RedisCommand.GET.name());
            add(RedisCommand.HGET.name());
            add(RedisCommand.ZREVRANK.name());
            add(RedisCommand.ZSCORE.name());
        }
    };

    @Override
    public DynamicTableSource createDynamicTableSource(Context context) {
        FactoryUtil.TableFactoryHelper helper = FactoryUtil.createTableFactoryHelper(this, context);
        ReadableConfig config = helper.getOptions();
        helper.validate();
        validateConfigOptions(config, SUPPORT_SOURCE_COMMANDS);
        return new RedisDynamicTableSource(context.getCatalogTable().getOptions(),
                context.getCatalogTable().getResolvedSchema(), config, getJdbcLookupOptions(config));
    }

    private RedisLookupOptions getJdbcLookupOptions(ReadableConfig readableConfig) {
        return new RedisLookupOptions(readableConfig.get(LOOKUP_CACHE_MAX_ROWS),
                readableConfig.get(LOOKUP_CACHE_TTL),
                readableConfig.get(LOOKUP_MAX_RETRIES), readableConfig.get(LOOKUP_ASYNC));
    }

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        final Set<ConfigOption<?>> requiredOptions = new HashSet<>();
        requiredOptions.add(RedisOptions.COMMAND);
        return requiredOptions;
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        final Set<ConfigOption<?>> options = new HashSet<>();
        options.add(RedisOptions.CLUSTER_NODES);
        options.add(RedisOptions.DATABASE);
        options.add(RedisOptions.PASSWORD);
        options.add(RedisOptions.REDIS_MODE);
        options.add(RedisOptions.ADDITIONAL_KEY);
        options.add(RedisOptions.MAXIDLE);
        options.add(RedisOptions.MINIDLE);
        options.add(RedisOptions.REDIS_MASTER_NAME);
        options.add(LOOKUP_ASYNC);
        options.add(LOOKUP_CACHE_MAX_ROWS);
        options.add(LOOKUP_CACHE_TTL);
        options.add(LOOKUP_MAX_RETRIES);
        options.add(RedisOptions.HOST);
        options.add(RedisOptions.MAX_TOTAL);
        options.add(RedisOptions.PORT);
        options.add(RedisOptions.SENTINELS_INFO);
        options.add(RedisOptions.SOCKET_TIMEOUT);
        options.add(RedisOptions.TIMEOUT);
        return options;
    }

    private void validateConfigOptions(ReadableConfig config, Set<String> supportCommands) {
        String redisMode = config.get(RedisOptions.REDIS_MODE);
        List<String> matchRedisMode = SUPPORT_REDIS_MODE.stream().filter(e -> e.equals(redisMode.toLowerCase().trim()))
                .collect(Collectors.toList());
        checkState(!matchRedisMode.isEmpty(),
                "Unsupported redis-mode " + redisMode + ". The supported redis-mode " + Arrays
                        .deepToString(SUPPORT_REDIS_MODE.toArray()));
        String command = config.get(RedisOptions.COMMAND);
        Preconditions.checkState(!StringUtils.isNullOrWhitespaceOnly(command),
                "Command can not be empty. The supported command are " + Arrays
                        .deepToString(supportCommands.toArray()));
        List<String> matchCommand = supportCommands
                .stream().filter(e -> e.equals(command.toUpperCase().trim())).collect(Collectors.toList());
        checkState(!matchCommand.isEmpty(), "Unsupported command " + command + ". The supported command " + Arrays
                .deepToString(supportCommands.toArray()));
    }
}