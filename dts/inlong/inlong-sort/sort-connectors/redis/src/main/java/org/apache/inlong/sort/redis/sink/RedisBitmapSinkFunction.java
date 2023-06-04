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
public class RedisBitmapSinkFunction
        extends
            AbstractRedisSinkFunction<Tuple4<Boolean, String, Long, Boolean>> {

    public static final Logger LOG = LoggerFactory.getLogger(RedisBitmapSinkFunction.class);

    public RedisBitmapSinkFunction(
            SerializationSchema<RowData> serializationSchema,
            StateEncoder<Tuple4<Boolean, String, Long, Boolean>> stateEncoder,
            long batchSize,
            Duration flushInterval,
            Duration configuration,
            FlinkJedisConfigBase flinkJedisConfigBase,
            String inlongMetric,
            String auditHostAndPorts) {
        super(TypeInformation.of(new TypeHint<Tuple4<Boolean, String, Long, Boolean>>() {
        }),
                serializationSchema,
                stateEncoder,
                batchSize,
                flushInterval,
                configuration,
                flinkJedisConfigBase,
                inlongMetric,
                auditHostAndPorts);
        LOG.info("Creating RedisBitmapStaticKvPairSinkFunction ...");
    }

    @Override
    public void open(Configuration parameters) {
        super.open(parameters);
        LOG.info("Opening RedisBitmapStaticKvPairSinkFunction ...");
    }

    @Override
    protected void flushInternal(List<Tuple4<Boolean, String, Long, Boolean>> rows) {
        for (Tuple4<Boolean, String, Long, Boolean> row : rows) {
            Boolean rowKind = row.f0;
            String key = row.f1;
            Long offset = row.f2;
            Boolean value = row.f3;
            if (rowKind) {
                redisCommandsContainer.setBit(key, offset, value);
            } else {
                redisCommandsContainer.del(key);
            }
        }
    }
}
