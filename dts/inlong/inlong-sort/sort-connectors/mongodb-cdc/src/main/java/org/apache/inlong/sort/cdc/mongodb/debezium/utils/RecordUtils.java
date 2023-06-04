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

package org.apache.inlong.sort.cdc.mongodb.debezium.utils;

import com.google.common.collect.ImmutableMap;
import com.mongodb.client.model.changestream.OperationType;
import io.debezium.connector.AbstractSourceInfo;
import io.debezium.relational.TableId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.flink.table.types.logical.ArrayType;
import org.apache.flink.table.types.logical.BigIntType;
import org.apache.flink.table.types.logical.BinaryType;
import org.apache.flink.table.types.logical.BooleanType;
import org.apache.flink.table.types.logical.DecimalType;
import org.apache.flink.table.types.logical.DoubleType;
import org.apache.flink.table.types.logical.IntType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.TimestampType;
import org.apache.flink.table.types.logical.VarCharType;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.bson.BsonType;
import org.bson.BsonValue;

/**
 * Utility class to deal record.
 */
public class RecordUtils {

    public static final String DOCUMENT_TO_FIELD = "to";
    private static final List<BsonType> INT_TYPE = Arrays.asList(BsonType.INT32, BsonType.INT64);
    private static final List<BsonType> BOOL_TYPE = Arrays.asList(BsonType.BOOLEAN);
    private static final List<BsonType> DOUBLE_TYPE = Arrays.asList(BsonType.DOUBLE, BsonType.DECIMAL128);
    private static final List<BsonType> DECIMAL_TYPE = Arrays.asList();

    private static final List<BsonType> TIMESTAMP_TYPE = Arrays
            .asList(BsonType.DATE_TIME, BsonType.TIMESTAMP);

    private static final List<BsonType> VARCHAR_TYPE = Arrays
            .asList(BsonType.END_OF_DOCUMENT, BsonType.STRING,
                    BsonType.UNDEFINED, BsonType.OBJECT_ID, BsonType.NULL,
                    BsonType.REGULAR_EXPRESSION, BsonType.DB_POINTER, BsonType.JAVASCRIPT,
                    BsonType.SYMBOL, BsonType.JAVASCRIPT_WITH_SCOPE, BsonType.MIN_KEY,
                    BsonType.MAX_KEY, BsonType.ARRAY,
                    BsonType.DOCUMENT, BsonType.BINARY);
    private static final List<BsonType> ARRAY_TYPE = Arrays.asList();

    private static final List<BsonType> BINARY_TYPE = Arrays.asList();

    private static final Map<String, Integer> SQL_TYPE_MAP =
            ImmutableMap.<String, Integer>builder()
                    .put("BIGINT", java.sql.Types.BIGINT)
                    .put("BOOLEAN", java.sql.Types.BOOLEAN)
                    .put("DOUBLE", java.sql.Types.DOUBLE)
                    .put("DECIMAL", java.sql.Types.DECIMAL)
                    .put("TIMESTAMP", java.sql.Types.TIMESTAMP)
                    .put("VARCHAR", java.sql.Types.VARCHAR)
                    .put("BINARY", java.sql.Types.BINARY)
                    .put("OTHER", java.sql.Types.OTHER)
                    .build();

    public static Integer getSqlType(String sqlTypeStr) {
        return SQL_TYPE_MAP.get(sqlTypeStr) == null ? java.sql.Types.VARCHAR : SQL_TYPE_MAP.get(sqlTypeStr);
    }

    private RecordUtils() {

    }

    /**
     * Get table id from source record
     * @param dataRecord
     * @return
     */
    public static TableId getTableId(SourceRecord dataRecord) {
        Struct value = (Struct) dataRecord.value();
        Struct source = value.getStruct("ns");
        String dbName = source.getString(AbstractSourceInfo.DATABASE_NAME_KEY);
        String tableName = source.getString("coll");
        return new TableId(dbName, null, tableName);
    }

    public static LogicalType convertLogicType(BsonValue bsonValue) {
        BsonType bsonType = bsonValue.getBsonType();
        if (INT_TYPE.contains(bsonType)) {
            return new BigIntType();
        }
        if (BOOL_TYPE.contains(bsonType)) {
            return new BooleanType();
        }
        if (DOUBLE_TYPE.contains(bsonType)) {
            return new DoubleType();
        }
        if (DECIMAL_TYPE.contains(bsonType)) {
            return new DecimalType();
        }
        if (TIMESTAMP_TYPE.contains(bsonType)) {
            return new TimestampType();
        }
        if (VARCHAR_TYPE.contains(bsonType)) {
            return new VarCharType();
        }
        if (BINARY_TYPE.contains(bsonType)) {
            return new BinaryType();
        }
        if (ARRAY_TYPE.contains(bsonType)) {
            return new ArrayType(new IntType());
        }
        return null;
    }

    /**
     * Whether the MongoDB event's operation is a dml operation.
     */
    public static boolean isDMLOperation(OperationType op) {
        if (OperationType.INSERT.equals(op) || OperationType.DELETE.equals(op)
                || OperationType.UPDATE.equals(op) || OperationType.REPLACE.equals(op)) {
            return true;
        }
        return false;
    }
}
