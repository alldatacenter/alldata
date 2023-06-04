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

import java.time.Duration;
import java.util.List;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisConfigBase;
import org.apache.flink.table.data.RowData;
import org.apache.inlong.sort.redis.common.schema.StateEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Flink Redis Producer.
 */
public class RedisHashSinkFunction
        extends
            AbstractRedisSinkFunction<Tuple4<Boolean, String, String, String>> {

    public static final Logger LOG = LoggerFactory.getLogger(RedisHashSinkFunction.class);

    public RedisHashSinkFunction(
            SerializationSchema<RowData> serializationSchema,
            StateEncoder<Tuple4<Boolean, String, String, String>> stateEncoder,
            long batchSize,
            Duration flushInterval,
            Duration expireTime,
            FlinkJedisConfigBase flinkJedisConfigBase,
            String inlongMetric,
            String auditHostAndPorts) {
        super(TypeInformation.of(new TypeHint<Tuple4<Boolean, String, String, String>>() {
        }),
                serializationSchema,
                stateEncoder,
                batchSize,
                flushInterval,
                expireTime,
                flinkJedisConfigBase,

                inlongMetric,
                auditHostAndPorts);
        LOG.info("Creating RedisHashSinkFunction ...");
    }

    @Override
    public void open(Configuration parameters) {
        super.open(parameters);
        LOG.info("Open RedisHashSinkFunction ...");
    }

    @Override
    protected void flushInternal(List<Tuple4<Boolean, String, String, String>> rows) {
        for (Tuple4<Boolean, String, String, String> row : rows) {
            LOG.info("Flush new row: {}.", row);
            Boolean rowKind = row.f0;
            String key = row.f1;
            String field = row.f2;
            String value = row.f3;
            if (rowKind) {
                redisCommandsContainer.hset(key, field, value, expireTime);
            } else {
                redisCommandsContainer.del(key);
            }
        }
    }
}
