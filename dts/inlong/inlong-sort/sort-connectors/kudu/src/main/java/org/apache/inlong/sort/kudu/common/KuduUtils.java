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

package org.apache.inlong.sort.kudu.common;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.table.types.DataType;
import org.apache.flink.util.StringUtils;
import org.apache.inlong.sort.formats.common.BooleanTypeInfo;
import org.apache.inlong.sort.formats.common.ByteTypeInfo;
import org.apache.inlong.sort.formats.common.DateTypeInfo;
import org.apache.inlong.sort.formats.common.DecimalTypeInfo;
import org.apache.inlong.sort.formats.common.DoubleTypeInfo;
import org.apache.inlong.sort.formats.common.FloatTypeInfo;
import org.apache.inlong.sort.formats.common.IntTypeInfo;
import org.apache.inlong.sort.formats.common.LongTypeInfo;
import org.apache.inlong.sort.formats.common.RowTypeInfo;
import org.apache.inlong.sort.formats.common.StringTypeInfo;
import org.apache.inlong.sort.formats.common.TimestampTypeInfo;
import org.apache.inlong.sort.formats.common.TypeInfo;
import org.apache.kudu.ColumnSchema;
import org.apache.kudu.Schema;
import org.apache.kudu.Type;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.flink.util.Preconditions.checkState;
import static org.apache.kudu.Type.BINARY;
import static org.apache.kudu.Type.BOOL;
import static org.apache.kudu.Type.DATE;
import static org.apache.kudu.Type.DECIMAL;
import static org.apache.kudu.Type.DOUBLE;
import static org.apache.kudu.Type.FLOAT;
import static org.apache.kudu.Type.INT16;
import static org.apache.kudu.Type.INT32;
import static org.apache.kudu.Type.INT64;
import static org.apache.kudu.Type.INT8;
import static org.apache.kudu.Type.STRING;
import static org.apache.kudu.Type.UNIXTIME_MICROS;
import static org.apache.kudu.Type.VARCHAR;

/**
 * The utility class for Kudu.
 */
public class KuduUtils {

    private static final Map<Type, TypeInfo> KUDU_TYPE_2_TYPE_INFO_MAP = new HashMap<>();
    private static final Map<Type, TypeInfo> KUDU_TYPE_2_DATA_TYPE_MAP = new HashMap<>();

    static {
        // kudu type to flink typeInfo
        KUDU_TYPE_2_TYPE_INFO_MAP.put(INT8, IntTypeInfo.INSTANCE);
        KUDU_TYPE_2_TYPE_INFO_MAP.put(INT16, IntTypeInfo.INSTANCE);
        KUDU_TYPE_2_TYPE_INFO_MAP.put(INT32, IntTypeInfo.INSTANCE);
        KUDU_TYPE_2_TYPE_INFO_MAP.put(INT64, LongTypeInfo.INSTANCE);
        KUDU_TYPE_2_TYPE_INFO_MAP.put(BINARY, ByteTypeInfo.INSTANCE);
        KUDU_TYPE_2_TYPE_INFO_MAP.put(STRING, StringTypeInfo.INSTANCE);
        KUDU_TYPE_2_TYPE_INFO_MAP.put(BOOL, BooleanTypeInfo.INSTANCE);
        KUDU_TYPE_2_TYPE_INFO_MAP.put(FLOAT, FloatTypeInfo.INSTANCE);
        KUDU_TYPE_2_TYPE_INFO_MAP.put(DOUBLE, DoubleTypeInfo.INSTANCE);
        KUDU_TYPE_2_TYPE_INFO_MAP.put(UNIXTIME_MICROS, TimestampTypeInfo.INSTANCE);
        KUDU_TYPE_2_TYPE_INFO_MAP.put(DECIMAL, DecimalTypeInfo.INSTANCE);
        KUDU_TYPE_2_TYPE_INFO_MAP.put(VARCHAR, StringTypeInfo.INSTANCE);
        KUDU_TYPE_2_TYPE_INFO_MAP.put(DATE, DateTypeInfo.INSTANCE);
    }

    /**
     * Check if types match.
     *
     * @param kuduTableSchema the table schema of kudu.
     * @param fieldNames the name of field.
     * @param typeInfo the type of user settings.
     */
    public static void checkSchema(Schema kuduTableSchema, String[] fieldNames, RowTypeInfo typeInfo) {
        List<ColumnSchema> kuduColumns = kuduTableSchema.getColumns();
        String actualTypes = kuduColumns.stream().map((ColumnSchema ti) -> {
            String fieldName = ti.getName();
            TypeInfo info = KUDU_TYPE_2_TYPE_INFO_MAP.get(ti.getType());
            if (info == null) {
                return fieldName + ":" + ti.getType().getName();
            } else {
                String simpleName = info.getClass().getSimpleName();
                return fieldName + ":" + simpleName.substring(0, simpleName.length() - 8);
            }
        }).collect(Collectors.joining(", ", "[", "]"));

        TypeInfo[] fieldTypeInfos = typeInfo.getFieldTypeInfos();
        String expectedTypes = IntStream.range(0, fieldTypeInfos.length).boxed().map(index -> {
            String simpleName = fieldTypeInfos[index].getClass().getSimpleName();
            String typeName = simpleName.substring(0, simpleName.length() - 8);
            String fieldName = fieldNames[index];
            return fieldName + ":" + typeName;
        }).collect(Collectors.joining(", ", "[", "]"));

        checkState(kuduColumns.size() == fieldTypeInfos.length,
                "The number of kudu fields mismatch, expected is %s, but actual is %s, "
                        + "\nexpect: %s,\nactual: %s.",
                kuduColumns.size(), fieldTypeInfos.length, expectedTypes, actualTypes);

        for (int i = 0; i < kuduColumns.size(); i++) {
            ColumnSchema columnSchema = kuduColumns.get(i);
            Type columnSchemaType = columnSchema.getType();
            TypeInfo actualTypeInfo = fieldTypeInfos[i];
            TypeInfo expectedTypeInfo = KUDU_TYPE_2_TYPE_INFO_MAP.get(columnSchemaType);

            checkState(Objects.equals(actualTypeInfo, expectedTypeInfo),
                    "The %sth field type mismatch, \nexpect: %s, \nactual: %s.",
                    i + 1, expectedTypes, actualTypes);
        }
    }

    /**
     * Check schema according to field name
     */
    public static void checkSchema(
            String[] fieldNames,
            DataType[] dataTypes,
            Schema kuduTableSchema) {
        List<ColumnSchema> kuduColumns = kuduTableSchema.getColumns();
        Map<String, ColumnSchema> columnMap = kuduColumns
                .stream()
                .map(col -> Tuple2.of(col.getName(), col))
                .collect(Collectors.toMap(t2 -> t2.f0, t2 -> t2.f1));
        String missFields = Arrays
                .stream(fieldNames)
                .filter(s -> !columnMap.containsKey(s))
                .collect(Collectors.joining(","));
        checkState(StringUtils.isNullOrWhitespaceOnly(missFields), "Can not find fields {} in kudu table!", missFields);
    }

    public static String[] getExpectedFieldNames(Schema kuduTableSchema) {
        return kuduTableSchema.getColumns().stream().map(ColumnSchema::getName).toArray(String[]::new);
    }
}
