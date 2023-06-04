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

import java.util.Collections;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.StringData;
import org.apache.inlong.sort.redis.common.schema.StateEncoder;

public class PlainPrefixMatchSchema extends AbstractRedisSchema<Tuple3<Boolean, String, String>> {

    public PlainPrefixMatchSchema(ResolvedSchema resolvedSchema) {
        super(resolvedSchema);
    }

    @Override
    public void validate(ResolvedSchema resolvedSchema) {
        validateStaticPrefixMatch(resolvedSchema);
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
    public StateEncoder<Tuple3<Boolean, String, String>> getStateEncoder() {
        return (in, serializationSchema) -> {
            boolean rowKind = getRowKind(in.getRowKind());
            GenericRowData rowData = (GenericRowData) in;
            String rowKey = rowData.getString(getKeyIndex()).toString();

            GenericRowData valueRowData = new GenericRowData(in.getRowKind(), in.getArity() - 1);
            for (int i = 0; i < rowData.getArity() - 1; ++i) {
                valueRowData.setField(i, rowData.getField(i + 1));
            }
            byte[] valueBytes = serializationSchema.serialize(valueRowData);
            String value = StringData.fromBytes(valueBytes).toString();
            return Collections.singletonList(Tuple3.of(rowKind, rowKey, value));
        };
    }
}
