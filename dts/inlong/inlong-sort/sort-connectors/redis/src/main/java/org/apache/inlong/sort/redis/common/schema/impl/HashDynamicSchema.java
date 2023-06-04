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

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.data.ArrayData;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.MapData;
import org.apache.flink.table.data.StringData;
import org.apache.inlong.sort.redis.common.schema.StateEncoder;

/**
 * The RedisSchema for Hash data-type and Dynamic mode.
 */
public class HashDynamicSchema extends HashStaticPrefixMatchSchema {

    public HashDynamicSchema(ResolvedSchema resolvedSchema) {
        super(resolvedSchema);
    }

    @Override
    public void validate(ResolvedSchema resolvedSchema) {
        validateDynamic(resolvedSchema);
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
    public StateEncoder<Tuple4<Boolean, String, String, String>> getStateEncoder() {
        return (in, serializationSchema) -> {
            boolean rowKind = getRowKind(in.getRowKind());

            GenericRowData row = (GenericRowData) in;
            String rowKey = row.getString(getKeyIndex()).toString();
            MapData fieldMap = row.getMap(getValueIndex());

            ArrayData keyArray = fieldMap.keyArray();
            ArrayData valueMap = fieldMap.valueArray();
            return IntStream
                    .range(0, fieldMap.size())
                    .boxed()
                    .map(i -> {
                        String fieldName = keyArray.getString(i).toString();
                        byte[] binary = valueMap.getBinary(i);
                        String fieldValue = StringData.fromBytes(binary).toString();
                        return Tuple4.of(rowKind, rowKey, fieldName, fieldValue);
                    })
                    .collect(Collectors.toList());
        };
    }
}
