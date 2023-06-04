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

import static org.apache.flink.api.java.ClosureCleaner.ensureSerializable;
import static org.apache.flink.util.Preconditions.checkNotNull;

import java.time.Duration;
import java.util.List;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisConfigBase;
import org.apache.flink.table.data.RowData;
import org.apache.inlong.sort.redis.common.schema.StateEncoder;

/**
 * The Flink Redis Producer.
 */
public class RedisSinkFunction
        extends
            AbstractRedisSinkFunction<Tuple3<Boolean, String, String>> {

    private static final long serialVersionUID = 1L;

    public RedisSinkFunction(
            SerializationSchema<RowData> serializationSchema,
            StateEncoder<Tuple3<Boolean, String, String>> stateEncoder,
            long batchSize,
            Duration flushInterval,
            Duration configuration,
            FlinkJedisConfigBase flinkJedisConfigBase,
            String inlongMetric,
            String auditHostAndPorts) {
        super(TypeInformation.of(new TypeHint<Tuple3<Boolean, String, String>>() {
        }),
                serializationSchema,
                stateEncoder,
                batchSize,
                flushInterval,
                configuration,
                flinkJedisConfigBase,
                inlongMetric,
                auditHostAndPorts);
        checkNotNull(serializationSchema, "The serialization schema must not be null.");
        ensureSerializable(serializationSchema);
    }

    @Override
    protected void flushInternal(List<Tuple3<Boolean, String, String>> rows) {
        for (Tuple3<Boolean, String, String> row : rows) {
            String key = row.f1;
            String value = row.f2;
            if (row.f0) {
                if (expireTime != null) {
                    redisCommandsContainer.setex(key, value, expireTime);
                } else {
                    redisCommandsContainer.set(key, value);
                }
            } else {
                redisCommandsContainer.del(key);
            }
        }
    }
}
