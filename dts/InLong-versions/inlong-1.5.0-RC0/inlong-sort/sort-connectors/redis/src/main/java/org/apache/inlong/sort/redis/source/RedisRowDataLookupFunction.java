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

import org.apache.flink.shaded.guava18.com.google.common.cache.Cache;
import org.apache.flink.shaded.guava18.com.google.common.cache.CacheBuilder;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisConfigBase;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.functions.FunctionContext;
import org.apache.flink.table.functions.TableFunction;
import org.apache.inlong.sort.redis.common.config.RedisLookupOptions;
import org.apache.inlong.sort.redis.common.container.InlongRedisCommandsContainer;
import org.apache.inlong.sort.redis.common.container.RedisCommandsContainerBuilder;
import org.apache.inlong.sort.redis.common.mapper.RedisCommand;
import org.apache.inlong.sort.redis.common.mapper.RedisCommandDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Redis RowData lookup function
 */
public class RedisRowDataLookupFunction extends TableFunction<RowData> {

    private static final Logger LOG = LoggerFactory.getLogger(RedisRowDataLookupFunction.class);

    private static final long serialVersionUID = 1L;

    private final long cacheMaxSize;
    private final long cacheExpireMs;
    private final int maxRetryTimes;
    private final FlinkJedisConfigBase flinkJedisConfigBase;
    private final String additionalKey;
    private final RedisCommand redisCommand;
    private transient Cache<RowData, RowData> cache;
    private InlongRedisCommandsContainer redisCommandsContainer;

    RedisRowDataLookupFunction(RedisCommandDescription redisCommandDescription,
            FlinkJedisConfigBase flinkJedisConfigBase, RedisLookupOptions redisLookupOptions) {
        this.flinkJedisConfigBase = flinkJedisConfigBase;
        this.redisCommand = redisCommandDescription.getCommand();
        this.additionalKey = redisCommandDescription.getAdditionalKey();
        this.cacheMaxSize = redisLookupOptions.getCacheMaxSize();
        this.cacheExpireMs = redisLookupOptions.getCacheExpireMs();
        this.maxRetryTimes = redisLookupOptions.getMaxRetryTimes();
    }

    /**
     * This is a lookup method which is called by Flink framework in runtime, only support one key
     *
     * @param keys lookup keys
     */
    public void eval(Object... keys) {
        RowData keyRow = GenericRowData.of(keys);
        if (cache != null) {
            RowData cachedRow = cache.getIfPresent(keyRow);
            if (cachedRow != null) {
                collect(cachedRow);
                return;
            }
        }
        for (int retry = 0; retry <= maxRetryTimes; retry++) {
            try {
                RowData rowData;
                switch (redisCommand) {
                    case GET:
                        rowData = GenericRowData
                                .of(StringData.fromString(keys[0].toString()), StringData
                                        .fromString(this.redisCommandsContainer.get(keys[0].toString())));
                        break;
                    case HGET:
                        rowData = GenericRowData
                                .of(StringData.fromString(keys[0].toString()), StringData.fromString(
                                        this.redisCommandsContainer.hget(this.additionalKey, keys[0].toString())));
                        break;
                    case ZREVRANK:
                        rowData = GenericRowData
                                .of(StringData.fromString(keys[0].toString()),
                                        this.redisCommandsContainer.zrevrank(this.additionalKey, keys[0].toString()));
                        break;
                    case ZSCORE:
                        rowData = GenericRowData
                                .of(StringData.fromString(keys[0].toString()),
                                        this.redisCommandsContainer.zscore(this.additionalKey, keys[0].toString()));
                        break;
                    default:
                        throw new UnsupportedOperationException(
                                String.format("Unsupported for redisCommand: %s", redisCommand));
                }
                if (cache == null) {
                    collect(rowData);
                } else {
                    collect(rowData);
                    cache.put(keyRow, rowData);
                }
                break;
            } catch (Exception e) {
                LOG.error(String.format("Redis query error, retry times = %d", retry), e);
                if (retry >= maxRetryTimes) {
                    throw new RuntimeException("Redis query error failed.", e);
                }
                try {
                    Thread.sleep(1000 * retry);
                } catch (InterruptedException e1) {
                    throw new RuntimeException(e1);
                }
            }
        }
    }

    @Override
    public void open(FunctionContext context) throws Exception {
        try {
            this.redisCommandsContainer = RedisCommandsContainerBuilder.build(this.flinkJedisConfigBase);
            this.redisCommandsContainer.open();
            this.cache = cacheMaxSize == -1 || cacheExpireMs == -1 ? null
                    : CacheBuilder.newBuilder()
                            .expireAfterWrite(cacheExpireMs, TimeUnit.MILLISECONDS)
                            .maximumSize(cacheMaxSize)
                            .build();
        } catch (Exception e) {
            LOG.error("Redis has not been properly initialized: ", e);
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        if (redisCommandsContainer != null) {
            redisCommandsContainer.close();
        }
    }

}
