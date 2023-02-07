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

package org.apache.inlong.sort.cdc.base.util;

import io.debezium.connector.AbstractSourceInfo;
import io.debezium.data.Envelope;
import io.debezium.relational.Column;
import io.debezium.relational.TableId;
import java.util.Arrays;
import java.util.List;
import org.apache.flink.table.types.logical.BigIntType;
import org.apache.flink.table.types.logical.BinaryType;
import org.apache.flink.table.types.logical.BooleanType;
import org.apache.flink.table.types.logical.DecimalType;
import org.apache.flink.table.types.logical.DoubleType;
import org.apache.flink.table.types.logical.FloatType;
import org.apache.flink.table.types.logical.IntType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.SmallIntType;
import org.apache.flink.table.types.logical.TimestampType;
import org.apache.flink.table.types.logical.TinyIntType;
import org.apache.flink.table.types.logical.VarCharType;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;

/**
 * Utility class to deal record.
 */
public class RecordUtils {

    private static final List<String> NUMBER_TYPE = Arrays.asList("NUMBER");
    private static final List<String> FLOAT_TYPE = Arrays.asList("FLOAT", "BINARY_FLOAT");
    private static final List<String> DOUBLE_TYPE = Arrays.asList("DOUBLE PRECISION", "BINARY_DOUBLE");
    private static final List<String> TIMESTAMP_TYPE = Arrays
            .asList("DATE", "TIMESTAMP", "WITH LOCAL TIME ZONE", "TIMESTAMP WITH TIME ZONE");
    private static final List<String> VARCHAR_TYPE = Arrays
            .asList("CHAR", "NCHAR", "NVARCHAR2", "NVCHAER", "VARCHAR", "VARCHAR2", "CLOB", "NCLOB", "XMLType");
    private static final List<String> BINARY_TYPE = Arrays.asList("BLOB", "ROWID");
    private static final List<String> BIGINT_TYPE = Arrays.asList("INTERVAL DAY TO SECOND", "INTERVAL YEAR TO MONTH");

    private RecordUtils() {

    }

    /**
     * Get table id from source record
     * @param dataRecord
     * @return
     */
    public static TableId getTableId(SourceRecord dataRecord) {
        Struct value = (Struct) dataRecord.value();
        Struct source = value.getStruct(Envelope.FieldName.SOURCE);
        String dbName = source.getString(AbstractSourceInfo.DATABASE_NAME_KEY);
        String schemaName = source.getString(AbstractSourceInfo.SCHEMA_NAME_KEY);
        String tableName = source.getString(AbstractSourceInfo.TABLE_NAME_KEY);
        return new TableId(dbName, schemaName, tableName);
    }

    /**
     * According column's type, get it's logicalType
     * Refer: https://ververica.github.io/flink-cdc-connectors/master/content/connectors/oracle-cdc.html?from_wecom=1
     * @param column
     * @param struct
     * @return
     */
    public static LogicalType convertLogicType(Column column, Struct struct) {
        String typeName = column.typeName();
        // Oracle Number Type default precision is 38
        Integer p = column.length() == 0 ? 38 : column.length();
        Integer s = column.scale().isPresent() ? column.scale().orElse(0) : struct.getInt32("scale");
        if (NUMBER_TYPE.contains(typeName)) {
            if (p == 1) {
                return new BooleanType();
            }
            if (s <= 0 && p - s < 3) {
                return new TinyIntType();
            }
            if (s <= 0 && p - s < 5) {
                return new SmallIntType();
            }
            if (s <= 0 && p - s < 10) {
                return new IntType();
            }
            if (s <= 0 && p - s < 19) {
                return new BigIntType();
            }
            if (s <= 0 && p - s >= 10 && p - s <= 38) {
                return new DecimalType(p - s, 0);
            }
            if (s > 0) {
                return new DecimalType(p, s);
            }
            if (s <= 0 && p - s > 38) {
                return new VarCharType(Integer.MAX_VALUE);
            }
        }
        if (FLOAT_TYPE.contains(typeName)) {
            return new FloatType();
        }
        if (DOUBLE_TYPE.contains(typeName)) {
            return new DoubleType();
        }
        if (TIMESTAMP_TYPE.contains(typeName)) {
            return new TimestampType(p);
        }
        if (VARCHAR_TYPE.contains(typeName)) {
            return new VarCharType(Integer.MAX_VALUE);
        }
        if (BINARY_TYPE.contains(typeName)) {
            return new BinaryType();
        }
        if (BIGINT_TYPE.contains(typeName)) {
            return new BigIntType();
        }
        return null;
    }

}
