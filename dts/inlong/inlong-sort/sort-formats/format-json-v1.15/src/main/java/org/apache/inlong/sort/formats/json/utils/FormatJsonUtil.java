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

package org.apache.inlong.sort.formats.json.utils;

import org.apache.flink.formats.common.TimestampFormat;
import org.apache.flink.formats.json.JsonFormatOptions.MapNullKeyMode;
import org.apache.flink.formats.json.RowDataToJsonConverters;
import org.apache.flink.formats.json.RowDataToJsonConverters.RowDataToJsonConverter;
import org.apache.flink.shaded.guava30.com.google.common.collect.ImmutableMap;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.BigIntType;
import org.apache.flink.table.types.logical.BinaryType;
import org.apache.flink.table.types.logical.BooleanType;
import org.apache.flink.table.types.logical.CharType;
import org.apache.flink.table.types.logical.DateType;
import org.apache.flink.table.types.logical.DecimalType;
import org.apache.flink.table.types.logical.DoubleType;
import org.apache.flink.table.types.logical.FloatType;
import org.apache.flink.table.types.logical.IntType;
import org.apache.flink.table.types.logical.LocalZonedTimestampType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.SmallIntType;
import org.apache.flink.table.types.logical.TimeType;
import org.apache.flink.table.types.logical.TimestampType;
import org.apache.flink.table.types.logical.TinyIntType;
import org.apache.flink.table.types.logical.VarBinaryType;
import org.apache.flink.table.types.logical.VarCharType;

import java.util.Map;

public class FormatJsonUtil {

    private static final int DEFAULT_DECIMAL_PRECISION = 15;
    private static final int DEFAULT_DECIMAL_SCALE = 5;
    private static final Integer ORACLE_TIMESTAMP_TIME_ZONE = -101;

    public static RowDataToJsonConverter rowDataToJsonConverter(DataType physicalRowDataType) {
        return rowDataToJsonConverter(TimestampFormat.SQL, null, physicalRowDataType);
    }

    public static RowDataToJsonConverter rowDataToJsonConverter(TimestampFormat timestampFormat,
            String mapNullKeyLiteral,
            DataType physicalRowDataType) {
        return rowDataToJsonConverter(timestampFormat, MapNullKeyMode.DROP, mapNullKeyLiteral, physicalRowDataType);
    }

    public static RowDataToJsonConverter rowDataToJsonConverter(TimestampFormat timestampFormat,
            MapNullKeyMode mapNullKeyMode,
            String mapNullKeyLiteral, DataType physicalRowDataType) {
        return new RowDataToJsonConverters(timestampFormat, mapNullKeyMode, mapNullKeyLiteral)
                .createConverter(physicalRowDataType.getLogicalType());
    }

    public static RowDataToJsonConverter rowDataToJsonConverter(LogicalType rowType) {
        return rowDataToJsonConverter(TimestampFormat.SQL, null, rowType);
    }

    public static RowDataToJsonConverter rowDataToJsonConverter(TimestampFormat timestampFormat,
            String mapNullKeyLiteral,
            LogicalType rowType) {
        return rowDataToJsonConverter(timestampFormat, MapNullKeyMode.DROP, mapNullKeyLiteral, rowType);
    }

    public static RowDataToJsonConverter rowDataToJsonConverter(TimestampFormat timestampFormat,
            MapNullKeyMode mapNullKeyMode,
            String mapNullKeyLiteral, LogicalType rowType) {
        return new RowDataToJsonConverters(timestampFormat, mapNullKeyMode, mapNullKeyLiteral)
                .createConverter(rowType);
    }

    public static final Map<Integer, LogicalType> SQL_TYPE_2_FLINK_TYPE_MAPPING =
            ImmutableMap.<Integer, LogicalType>builder()
                    .put(java.sql.Types.CHAR, new CharType())
                    .put(java.sql.Types.VARCHAR, new VarCharType())
                    .put(java.sql.Types.SMALLINT, new SmallIntType())
                    .put(java.sql.Types.INTEGER, new IntType())
                    .put(java.sql.Types.BIGINT, new BigIntType())
                    .put(java.sql.Types.REAL, new FloatType())
                    .put(java.sql.Types.DOUBLE, new DoubleType())
                    .put(java.sql.Types.FLOAT, new FloatType())
                    .put(java.sql.Types.DECIMAL, new DecimalType(DEFAULT_DECIMAL_PRECISION, DEFAULT_DECIMAL_SCALE))
                    .put(java.sql.Types.NUMERIC, new DecimalType(DEFAULT_DECIMAL_PRECISION, DEFAULT_DECIMAL_SCALE))
                    .put(java.sql.Types.BIT, new BooleanType())
                    .put(java.sql.Types.TIME, new TimeType())
                    .put(java.sql.Types.TIME_WITH_TIMEZONE, new TimeType())
                    .put(java.sql.Types.TIMESTAMP_WITH_TIMEZONE, new LocalZonedTimestampType())
                    .put(ORACLE_TIMESTAMP_TIME_ZONE, new LocalZonedTimestampType())
                    .put(java.sql.Types.TIMESTAMP, new TimestampType())
                    .put(java.sql.Types.BINARY, new BinaryType())
                    .put(java.sql.Types.VARBINARY, new VarBinaryType())
                    .put(java.sql.Types.BLOB, new VarBinaryType())
                    .put(java.sql.Types.CLOB, new VarBinaryType())
                    .put(java.sql.Types.DATE, new DateType())
                    .put(java.sql.Types.BOOLEAN, new BooleanType())
                    .put(java.sql.Types.LONGNVARCHAR, new VarCharType())
                    .put(java.sql.Types.LONGVARBINARY, new VarCharType())
                    .put(java.sql.Types.LONGVARCHAR, new VarCharType())
                    .put(java.sql.Types.ARRAY, new VarCharType())
                    .put(java.sql.Types.NCHAR, new CharType())
                    .put(java.sql.Types.NCLOB, new VarBinaryType())
                    .put(java.sql.Types.TINYINT, new TinyIntType())
                    .put(java.sql.Types.OTHER, new VarCharType())
                    .build();

    public static final Map<Integer, LogicalType> SQL_TYPE_2_SPARK_SUPPORTED_FLINK_TYPE_MAPPING =
            ImmutableMap.<Integer, LogicalType>builder()
                    .put(java.sql.Types.CHAR, new CharType())
                    .put(java.sql.Types.VARCHAR, new VarCharType())
                    .put(java.sql.Types.SMALLINT, new SmallIntType())
                    .put(java.sql.Types.INTEGER, new IntType())
                    .put(java.sql.Types.BIGINT, new BigIntType())
                    .put(java.sql.Types.REAL, new FloatType())
                    .put(java.sql.Types.DOUBLE, new DoubleType())
                    .put(java.sql.Types.FLOAT, new FloatType())
                    .put(java.sql.Types.DECIMAL, new DecimalType(DEFAULT_DECIMAL_PRECISION, DEFAULT_DECIMAL_SCALE))
                    .put(java.sql.Types.NUMERIC, new DecimalType(DEFAULT_DECIMAL_PRECISION, DEFAULT_DECIMAL_SCALE))
                    .put(java.sql.Types.BIT, new BooleanType())
                    .put(java.sql.Types.TIME, new VarCharType())
                    .put(java.sql.Types.TIMESTAMP_WITH_TIMEZONE, new LocalZonedTimestampType())
                    .put(ORACLE_TIMESTAMP_TIME_ZONE, new LocalZonedTimestampType())
                    .put(java.sql.Types.TIMESTAMP, new LocalZonedTimestampType())
                    .put(java.sql.Types.BINARY, new BinaryType())
                    .put(java.sql.Types.VARBINARY, new VarBinaryType())
                    .put(java.sql.Types.BLOB, new VarBinaryType())
                    .put(java.sql.Types.DATE, new DateType())
                    .put(java.sql.Types.BOOLEAN, new BooleanType())
                    .put(java.sql.Types.LONGNVARCHAR, new VarCharType())
                    .put(java.sql.Types.LONGVARBINARY, new VarCharType())
                    .put(java.sql.Types.LONGVARCHAR, new VarCharType())
                    .put(java.sql.Types.ARRAY, new VarCharType())
                    .put(java.sql.Types.NCHAR, new CharType())
                    .put(java.sql.Types.NCLOB, new VarBinaryType())
                    .put(java.sql.Types.TINYINT, new TinyIntType())
                    .put(java.sql.Types.OTHER, new VarCharType())
                    .build();

    public static final Map<String, LogicalType> DEBEZIUM_TYPE_2_FLINK_TYPE_MAPPING =
            ImmutableMap.<String, LogicalType>builder()
                    .put("BOOLEAN", new BooleanType())
                    .put("INT8", new TinyIntType())
                    .put("INT16", new SmallIntType())
                    .put("INT32", new IntType())
                    .put("INT64", new BigIntType())
                    .put("FLOAT32", new FloatType())
                    .put("FLOAT64", new DoubleType())
                    .put("STRING", new VarCharType())
                    .put("BYTES", new VarBinaryType())
                    .build();
}
