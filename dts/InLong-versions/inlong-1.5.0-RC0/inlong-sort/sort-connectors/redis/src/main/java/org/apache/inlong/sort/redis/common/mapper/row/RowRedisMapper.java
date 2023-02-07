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

package org.apache.inlong.sort.redis.common.mapper.row;

import org.apache.flink.table.data.RowData;
import org.apache.inlong.sort.redis.common.handler.RedisMapperHandler;
import org.apache.inlong.sort.redis.common.mapper.RedisCommand;
import org.apache.inlong.sort.redis.common.mapper.RedisCommandDescription;
import org.apache.inlong.sort.redis.common.mapper.RedisMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import static org.apache.flink.streaming.connectors.redis.descriptor.RedisValidator.REDIS_COMMAND;

/**
 * Base row redis mapper implement.
 * Copy from {@link org.apache.flink.streaming.connectors.redis.common.mapper.row.RowRedisMapper}
 */
public abstract class RowRedisMapper implements RedisMapper<RowData>, RedisMapperHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RowRedisMapper.class);

    private Integer ttl;

    private RedisCommand redisCommand;

    private String additionalKey;

    public RowRedisMapper() {
    }

    public RowRedisMapper(Integer ttl, RedisCommand redisCommand) {
        this(ttl, null, redisCommand);
    }

    public RowRedisMapper(Integer ttl, String additionalKey, RedisCommand redisCommand) {
        this.ttl = ttl;
        this.additionalKey = additionalKey;
        this.redisCommand = redisCommand;
    }

    public RowRedisMapper(String additionalKey, RedisCommand redisCommand) {
        this(null, additionalKey, redisCommand);
    }

    public RowRedisMapper(RedisCommand redisCommand) {
        this.redisCommand = redisCommand;
    }

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public RedisCommand getRedisCommand() {
        return redisCommand;
    }

    public void setRedisCommand(RedisCommand redisCommand) {
        this.redisCommand = redisCommand;
    }

    @Override
    public RedisCommandDescription getCommandDescription() {
        return new RedisCommandDescription(redisCommand, additionalKey, ttl);
    }

    @Override
    public String getKeyFromData(RowData data) {
        return data.getString(0).toString();
    }

    @Override
    public String getValueFromData(RowData data) {
        return data.getString(1).toString();
    }

    @Override
    public Map<String, String> requiredContext() {
        Map<String, String> require = new HashMap<>();
        require.put(REDIS_COMMAND, getRedisCommand().name().toLowerCase(Locale.ROOT));
        return require;
    }

    @Override
    public boolean equals(Object obj) {
        RedisCommand redisCommand = ((RowRedisMapper) obj).redisCommand;
        return this.redisCommand == redisCommand;
    }

    @Override
    public Optional<Integer> getAdditionalTTL(RowData data) {
        return Optional.ofNullable(getTtl());
    }

    @Override
    public Optional<String> getAdditionalKey(RowData data) {
        return Optional.ofNullable(additionalKey);
    }
}
