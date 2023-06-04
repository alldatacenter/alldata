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

import static org.apache.flink.util.Preconditions.checkState;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.shaded.guava18.com.google.common.base.Preconditions;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.catalog.Column;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.connector.format.EncodingFormat;
import org.apache.flink.table.connector.sink.DynamicTableSink.Context;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.KeyValueDataType;
import org.apache.flink.table.types.utils.DataTypeUtils;
import org.apache.flink.table.utils.TableSchemaUtils;
import org.apache.flink.types.RowKind;
import org.apache.inlong.sort.redis.common.schema.RedisSchema;

/**
 * The base class of {@link RedisSchema}
 *
 * @param <T> The type of Flink state.
 */
public abstract class AbstractRedisSchema<T> implements RedisSchema<T> {

    private final DataType physicalFormatDataType;

    public AbstractRedisSchema(ResolvedSchema resolvedSchema) {
        TableSchema physicalSchema = TableSchemaUtils.getPhysicalSchema(
                TableSchema.fromResolvedSchema(resolvedSchema));
        int valueIndex = getValueIndex();
        int[] projection = IntStream.range(valueIndex, physicalSchema.getFieldCount()).toArray();

        physicalFormatDataType = DataTypeUtils.projectRow(physicalSchema.toPhysicalRowDataType(), projection);
    }

    @Override
    public SerializationSchema<RowData> getSerializationSchema(
            Context context,
            EncodingFormat<SerializationSchema<RowData>> format) {
        return format.createRuntimeEncoder(context, physicalFormatDataType);
    }

    protected boolean getRowKind(RowKind rowKind) {
        return rowKind == RowKind.INSERT || rowKind == RowKind.UPDATE_AFTER;
    }

    protected void validateStaticKvPair(ResolvedSchema resolvedSchema, Class keyClass, Class valueClass) {
        validateRowKey(resolvedSchema);
        List<Column> columns = resolvedSchema.getColumns();
        Preconditions.checkState(columns.size() % 2 == 1,
                "The number of elements must be odd, and all even elements are field.");
        for (int i = 1; i < columns.size(); i += 2) {
            Column offsetColumn = columns.get(i);
            Column valueColumn = columns.get(i + 1);
            Preconditions.checkState(offsetColumn.getDataType().getConversionClass() == keyClass,
                    "The type of key/offset field (odd column) must be " + keyClass.getName());
            Preconditions.checkState(valueColumn.getDataType().getConversionClass() == valueClass,
                    "The type of value field (even column) must be " + valueClass.getName());
        }
    }

    protected void validateStaticPrefixMatch(ResolvedSchema resolvedSchema) {
        validateRowKey(resolvedSchema);
    }

    protected void validateDynamic(ResolvedSchema resolvedSchema) {
        validateRowKey(resolvedSchema);
        List<Column> columns = resolvedSchema.getColumns();
        Preconditions.checkState(columns.size() == 2,
                "The number of elements must be 2.");
        Column column = columns.get(1);
        DataType dataType = column.getDataType();
        Preconditions.checkState(dataType instanceof KeyValueDataType, "The second element's Type must be Map");
        Preconditions.checkState(((KeyValueDataType) dataType).getKeyDataType().getConversionClass() == String.class,
                "The key type of second element(Map) must be String");
        Preconditions.checkState(((KeyValueDataType) dataType).getValueDataType().getConversionClass() == String.class,
                "The value type of second element(Map) must be String");
    }

    private void validateRowKey(ResolvedSchema resolvedSchema) {
        Optional<Column> column = resolvedSchema.getColumn(0);
        checkState(column.get().getDataType().getConversionClass() == String.class,
                "The rowKey (first element) must be String");
    }
}
