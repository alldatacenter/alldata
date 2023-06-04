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

package org.apache.inlong.sort.redis.common.schema.impl;

import static org.apache.flink.shaded.guava18.com.google.common.base.Preconditions.checkState;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.data.GenericRowData;
import org.apache.inlong.sort.redis.common.schema.StateEncoder;

/**
 * The RedisSchema for Bitmap data-type and StaticKvPair mode.
 */
public class BitmapStaticKvPairSchema
        extends
            AbstractRedisSchema<Tuple4<Boolean, String, Long, Boolean>> {

    public BitmapStaticKvPairSchema(ResolvedSchema resolvedSchema) {
        super(resolvedSchema);
    }

    @Override
    public void validate(ResolvedSchema resolvedSchema) {
        validateStaticKvPair(resolvedSchema, Long.class, Boolean.class);
    }

    @Override
    public int getKeyIndex() {
        return 0;
    }

    @Override
    public int getValueIndex() {
        return 1;
    }

    @Override
    public StateEncoder<Tuple4<Boolean, String, Long, Boolean>> getStateEncoder() {
        return (in, serializationSchema) -> {
            boolean rowKind = getRowKind(in.getRowKind());
            GenericRowData row = (GenericRowData) in;
            String rowKey = row.getString(getKeyIndex()).toString();

            checkState(row.getArity() % 2 == 1,
                    "The number of elements must be odd, and all even elements are field.");

            return IntStream
                    .range(1, row.getArity() / 2 + 1)
                    .boxed()
                    .map(i -> {
                        int keyPos = 2 * i - 1;
                        long offset = row.getLong(keyPos);
                        boolean aBoolean = row.getBoolean(keyPos + 1);
                        return Tuple4.of(rowKind, rowKey, offset, aBoolean);
                    })
                    .collect(Collectors.toList());

        };
    }
}
