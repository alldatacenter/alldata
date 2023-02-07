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

package org.apache.inlong.sort.cdc.oracle.table;

import io.debezium.connector.AbstractSourceInfo;
import io.debezium.data.Envelope;
import io.debezium.data.Envelope.FieldName;
import io.debezium.relational.Column;
import io.debezium.relational.Table;
import io.debezium.relational.history.TableChanges;
import io.debezium.relational.history.TableChanges.TableChange;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.data.GenericArrayData;
import org.apache.flink.table.data.GenericMapData;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;
import org.apache.flink.table.types.DataType;
import org.apache.inlong.sort.cdc.base.debezium.table.MetadataConverter;
import org.apache.inlong.sort.formats.json.canal.CanalJson;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;

/** Defines the supported metadata columns for {@link OracleTableSource}. */
public enum OracleReadableMetaData {

    /**
     * Name of the table that contain the row.
     */
    TABLE_NAME(
            "table_name",
            DataTypes.STRING().notNull(),
            new MetadataConverter() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return StringData.fromString(getMetaData(record, AbstractSourceInfo.TABLE_NAME_KEY));
                }
            }),

    /**
     * Name of the schema that contain the row.
     */
    SCHEMA_NAME(
            "schema_name",
            DataTypes.STRING().notNull(),
            new MetadataConverter() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return StringData.fromString(getMetaData(record, AbstractSourceInfo.SCHEMA_NAME_KEY));
                }
            }),

    /**
     * Name of the database that contain the row.
     */
    DATABASE_NAME(
            "database_name",
            DataTypes.STRING().notNull(),
            new MetadataConverter() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return StringData.fromString(getMetaData(record, AbstractSourceInfo.DATABASE_NAME_KEY));
                }
            }),

    /**
     * It indicates the time that the change was made in the database.
     */
    OP_TS(
            "op_ts",
            DataTypes.TIMESTAMP_WITH_LOCAL_TIME_ZONE(3).notNull(),
            new MetadataConverter() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    Struct messageStruct = (Struct) record.value();
                    Struct sourceStruct = messageStruct.getStruct(FieldName.SOURCE);
                    return TimestampData.fromEpochMillis(
                            (Long) sourceStruct.get(AbstractSourceInfo.TIMESTAMP_KEY));
                }
            }),

    /**
     * Name of the table that contain the row.
     */
    META_TABLE_NAME(
            "meta.table_name",
            DataTypes.STRING().notNull(),
            new MetadataConverter() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return StringData.fromString(getMetaData(record, AbstractSourceInfo.TABLE_NAME_KEY));
                }
            }),

    /**
     * Name of the schema that contain the row.
     */
    META_SCHEMA_NAME(
            "meta.schema_name",
            DataTypes.STRING().notNull(),
            new MetadataConverter() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return StringData.fromString(getMetaData(record, AbstractSourceInfo.SCHEMA_NAME_KEY));
                }
            }),

    /**
     * Name of the database that contain the row.
     */
    META_DATABASE_NAME(
            "meta.database_name",
            DataTypes.STRING().notNull(),
            new MetadataConverter() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return StringData.fromString(getMetaData(record, AbstractSourceInfo.DATABASE_NAME_KEY));
                }
            }),

    /**
     * It indicates the time that the change was made in the database.
     */
    META_OP_TS(
            "meta.op_ts",
            DataTypes.TIMESTAMP_WITH_LOCAL_TIME_ZONE(3).notNull(),
            new MetadataConverter() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    Struct messageStruct = (Struct) record.value();
                    Struct sourceStruct = messageStruct.getStruct(FieldName.SOURCE);
                    return TimestampData.fromEpochMillis(
                            (Long) sourceStruct.get(AbstractSourceInfo.TIMESTAMP_KEY));
                }
            }),

    DATA(
            "meta.data",
            DataTypes.STRING(),
            new MetadataConverter() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return null;
                }

                @Override
                public Object read(SourceRecord record,
                        @Nullable TableChanges.TableChange tableSchema, RowData rowData) {
                    return getCanalData(record, tableSchema, (GenericRowData) rowData);
                }
            }),

    DATA_CANAL(
            "meta.data_canal",
            DataTypes.STRING(),
            new MetadataConverter() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return null;
                }

                @Override
                public Object read(SourceRecord record,
                        @Nullable TableChanges.TableChange tableSchema, RowData rowData) {
                    return getCanalData(record, tableSchema, (GenericRowData) rowData);
                }
            }),

    /**
     * Operation type, INSERT/UPDATE/DELETE.
     */
    OP_TYPE(
            "meta.op_type",
            DataTypes.STRING().notNull(),
            new MetadataConverter() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return StringData.fromString(getOpType(record));
                }
            }),

    /**
     * Source does not emit ddl data.
     */
    IS_DDL(
            "meta.is_ddl",
            DataTypes.BOOLEAN().notNull(),
            new MetadataConverter() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return false;
                }
            }),

    ORACLE_TYPE(
            "meta.oracle_type",
            DataTypes.MAP(DataTypes.STRING().nullable(), DataTypes.STRING().nullable()).nullable(),
            new MetadataConverter() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return null;
                }

                @Override
                public Object read(
                        SourceRecord record, @Nullable TableChanges.TableChange tableSchema) {
                    Map<String, String> oracleType = getOracleType(tableSchema);
                    if (oracleType == null) {
                        return null;
                    }
                    return new GenericMapData(oracleType);
                }
            }),

    PK_NAMES(
            "meta.pk_names",
            DataTypes.ARRAY(DataTypes.STRING().nullable()).nullable(),
            new MetadataConverter() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return null;
                }

                @Override
                public Object read(
                        SourceRecord record, @Nullable TableChanges.TableChange tableSchema) {
                    if (tableSchema == null) {
                        return null;
                    }
                    return new GenericArrayData(
                            tableSchema.getTable().primaryKeyColumnNames().stream()
                                    .map(StringData::fromString)
                                    .toArray());
                }
            }),

    SQL(
            "meta.sql",
            DataTypes.STRING().nullable(),
            new MetadataConverter() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return StringData.fromString("");
                }
            }),

    SQL_TYPE(
            "meta.sql_type",
            DataTypes.MAP(DataTypes.STRING().nullable(), DataTypes.INT().nullable()).nullable(),
            new MetadataConverter() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return null;
                }

                @Override
                public Object read(
                        SourceRecord record, @Nullable TableChanges.TableChange tableSchema) {
                    if (tableSchema == null) {
                        return null;
                    }
                    Map<StringData, Integer> sqlType = new HashMap<>();
                    final Table table = tableSchema.getTable();
                    table.columns()
                            .forEach(
                                    column -> {
                                        sqlType.put(
                                                StringData.fromString(column.name()),
                                                column.jdbcType());
                                    });

                    return new GenericMapData(sqlType);
                }
            }),

    TS(
            "meta.ts",
            DataTypes.TIMESTAMP_WITH_LOCAL_TIME_ZONE(3).notNull(),
            new MetadataConverter() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    Struct messageStruct = (Struct) record.value();
                    return TimestampData.fromEpochMillis(
                            (Long) messageStruct.get(FieldName.TIMESTAMP));
                }
            });

    private static StringData getCanalData(SourceRecord record, TableChange tableSchema, GenericRowData rowData) {
        // construct canal json
        Struct messageStruct = (Struct) record.value();
        Struct sourceStruct = messageStruct.getStruct(FieldName.SOURCE);
        // tableName
        String tableName = getMetaData(record, AbstractSourceInfo.TABLE_NAME_KEY);
        // databaseName
        String databaseName = getMetaData(record, AbstractSourceInfo.DATABASE_NAME_KEY);
        // schemaName
        String schemaName = getMetaData(record, AbstractSourceInfo.SCHEMA_NAME_KEY);
        // opTs
        long opTs = (Long) sourceStruct.get(AbstractSourceInfo.TIMESTAMP_KEY);
        // ts
        long ts = (Long) messageStruct.get(FieldName.TIMESTAMP);
        // actual data
        GenericRowData data = rowData;
        Map<String, Object> field = (Map<String, Object>) data.getField(0);
        List<Map<String, Object>> dataList = new ArrayList<>();
        dataList.add(field);

        CanalJson canalJson = CanalJson.builder()
                .data(dataList).database(databaseName).schema(schemaName)
                .sql("").es(opTs).isDdl(false).pkNames(getPkNames(tableSchema))
                .oracleType(getOracleType(tableSchema))
                .table(tableName).ts(ts)
                .type(getOpType(record)).sqlType(getSqlType(tableSchema)).build();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return StringData.fromString(objectMapper.writeValueAsString(canalJson));
        } catch (Exception e) {
            throw new IllegalStateException("exception occurs when get meta data", e);
        }
    }

    private final String key;

    private final DataType dataType;

    private final MetadataConverter converter;

    private static final String OP_INSERT = "INSERT";
    private static final String OP_DELETE = "DELETE";
    private static final String OP_UPDATE = "UPDATE";
    private static final String REGEX_FORMATTED = "\\w.+\\([\\d ,]+\\)";
    private static final String FORMAT_PRECISION = "%s(%d)";
    private static final String FORMAT_PRECISION_SCALE = "%s(%d, %d)";

    OracleReadableMetaData(String key, DataType dataType, MetadataConverter converter) {
        this.key = key;
        this.dataType = dataType;
        this.converter = converter;
    }

    private static String getOpType(SourceRecord record) {
        String opType;
        final Envelope.Operation op = Envelope.operationFor(record);
        if (op == Envelope.Operation.CREATE || op == Envelope.Operation.READ) {
            opType = OP_INSERT;
        } else if (op == Envelope.Operation.DELETE) {
            opType = OP_DELETE;
        } else {
            opType = OP_UPDATE;
        }
        return opType;
    }

    private static List<String> getPkNames(@Nullable TableChanges.TableChange tableSchema) {
        if (tableSchema == null) {
            return null;
        }
        return tableSchema.getTable().primaryKeyColumnNames();
    }

    public static Map<String, String> getOracleType(@Nullable TableChanges.TableChange tableSchema) {
        if (tableSchema == null) {
            return null;
        }
        Map<String, String> oracleType = new LinkedHashMap<>();
        final Table table = tableSchema.getTable();
        for (Column column : table.columns()) {
            // The typeName contains precision and does not need to be formatted.
            if (column.typeName().matches(REGEX_FORMATTED)) {
                oracleType.put(column.name(), column.typeName());
                continue;
            }
            if (column.scale().isPresent()) {
                oracleType.put(
                        column.name(),
                        String.format(FORMAT_PRECISION_SCALE,
                                column.typeName(), column.length(), column.scale().get()));
            } else {
                oracleType.put(
                        column.name(),
                        String.format(FORMAT_PRECISION, column.typeName(), column.length()));
            }
        }
        return oracleType;
    }

    public static Map<String, Integer> getSqlType(@Nullable TableChanges.TableChange tableSchema) {
        if (tableSchema == null) {
            return null;
        }
        Map<String, Integer> sqlType = new LinkedHashMap<>();
        final Table table = tableSchema.getTable();
        table.columns()
                .forEach(
                        column -> {
                            sqlType.put(
                                    column.name(),
                                    column.jdbcType());
                        });
        return sqlType;
    }

    private static String getMetaData(SourceRecord record, String tableNameKey) {
        Struct messageStruct = (Struct) record.value();
        Struct sourceStruct = messageStruct.getStruct(FieldName.SOURCE);
        return sourceStruct.getString(tableNameKey);
    }

    public String getKey() {
        return key;
    }

    public DataType getDataType() {
        return dataType;
    }

    public MetadataConverter getConverter() {
        return converter;
    }

}
