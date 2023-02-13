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

package org.apache.inlong.sort.cdc.postgres.table;

import io.debezium.connector.AbstractSourceInfo;
import io.debezium.data.Envelope;
import io.debezium.data.Envelope.FieldName;
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
import org.apache.inlong.sort.formats.json.debezium.DebeziumJson;
import org.apache.inlong.sort.formats.json.debezium.DebeziumJson.Source;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;

/**
 * Defines the supported metadata columns for {@link PostgreSQLTableSource}.
 */
public enum PostgreSQLReadableMetaData {

    /**
     * Name of the table that contain the row.
     */
    TABLE_NAME("table_name", DataTypes.STRING().notNull(), new MetadataConverter() {

        private static final long serialVersionUID = 1L;

        @Override
        public Object read(SourceRecord record) {
            return StringData.fromString(getMetaData(record, AbstractSourceInfo.TABLE_NAME_KEY));
        }
    }),
    /**
     * Name of the schema that contain the row.
     */
    SCHEMA_NAME("schema_name", DataTypes.STRING().notNull(), new MetadataConverter() {

        private static final long serialVersionUID = 1L;

        @Override
        public Object read(SourceRecord record) {
            return StringData.fromString(getMetaData(record, AbstractSourceInfo.SCHEMA_NAME_KEY));
        }
    }),

    /**
     * Name of the database that contain the row.
     */
    DATABASE_NAME("database_name", DataTypes.STRING().notNull(), new MetadataConverter() {

        private static final long serialVersionUID = 1L;

        @Override
        public Object read(SourceRecord record) {
            return StringData.fromString(getMetaData(record, AbstractSourceInfo.DATABASE_NAME_KEY));
        }
    }),

    /**
     * It indicates the time that the change was made in the database. If the record is read from
     * snapshot of the table instead of the change stream, the value is always 0.
     */
    OP_TS("op_ts", DataTypes.TIMESTAMP_LTZ(3).notNull(), new MetadataConverter() {

        private static final long serialVersionUID = 1L;

        @Override
        public Object read(SourceRecord record) {
            Struct messageStruct = (Struct) record.value();
            Struct sourceStruct = messageStruct.getStruct(FieldName.SOURCE);
            return TimestampData.fromEpochMillis((Long) sourceStruct.get(AbstractSourceInfo.TIMESTAMP_KEY));
        }
    }),

    /**
     * It indicates the data streams with canal-json format.
     */
    DATA("meta.data", DataTypes.STRING(), new MetadataConverter() {

        private static final long serialVersionUID = 1L;

        @Override
        public Object read(SourceRecord record) {
            return null;
        }

        @Override
        public Object read(SourceRecord record, @Nullable TableChange tableSchema, RowData rowData) {
            return getCanalData(record, tableSchema, (GenericRowData) rowData);
        }
    }),

    /**
     * It indicates the data streams with canal-json format.
     */
    DATA_CANAL("meta.data_canal", DataTypes.STRING(), new MetadataConverter() {

        private static final long serialVersionUID = 1L;

        @Override
        public Object read(SourceRecord record) {
            return null;
        }

        @Override
        public Object read(SourceRecord record, @Nullable TableChange tableSchema, RowData rowData) {
            return getCanalData(record, tableSchema, (GenericRowData) rowData);
        }
    }),

    /**
     * It indicates the data streams with debezium-json format.
     */
    DATA_DEBEZIUM("meta.data_debezium", DataTypes.STRING(), new MetadataConverter() {

        private static final long serialVersionUID = 1L;

        @Override
        public Object read(SourceRecord record) {
            return null;
        }

        @Override
        public Object read(SourceRecord record, @Nullable TableChanges.TableChange tableSchema, RowData rowData) {
            // construct debezium json
            Struct messageStruct = (Struct) record.value();
            Struct sourceStruct = messageStruct.getStruct(FieldName.SOURCE);
            GenericRowData data = (GenericRowData) rowData;
            Map<String, Object> field = (Map<String, Object>) data.getField(0);

            Source source = Source.builder().db(getMetaData(record, AbstractSourceInfo.DATABASE_NAME_KEY))
                    .table(getMetaData(record, AbstractSourceInfo.TABLE_NAME_KEY))
                    .name(sourceStruct.getString(AbstractSourceInfo.SERVER_NAME_KEY)).sqlType(getSqlType(tableSchema))
                    .pkNames(getPkNames(tableSchema)).build();
            DebeziumJson debeziumJson = DebeziumJson.builder().after(field).source(source)
                    .tsMs(sourceStruct.getInt64(AbstractSourceInfo.TIMESTAMP_KEY)).op(getDebeziumOpType(record))
                    .tableChange(tableSchema).build();

            try {
                return StringData.fromString(OBJECT_MAPPER.writeValueAsString(debeziumJson));
            } catch (Exception e) {
                throw new IllegalStateException("exception occurs when get meta data", e);
            }
        }
    }),

    /**
     * Name of the table that contain the row. .
     */
    META_TABLE_NAME("meta.table_name", DataTypes.STRING().notNull(), new MetadataConverter() {

        private static final long serialVersionUID = 1L;

        @Override
        public Object read(SourceRecord record) {
            return StringData.fromString(getMetaData(record, AbstractSourceInfo.TABLE_NAME_KEY));
        }
    }),

    /**
     * Name of the schema that contain the row.
     */
    META_SCHEMA_NAME("meta.schema_name", DataTypes.STRING().notNull(), new MetadataConverter() {

        private static final long serialVersionUID = 1L;

        @Override
        public Object read(SourceRecord record) {
            return StringData.fromString(getMetaData(record, AbstractSourceInfo.SCHEMA_NAME_KEY));
        }
    }),

    /**
     * Name of the database that contain the row.
     */
    META_DATABASE_NAME("meta.database_name", DataTypes.STRING().notNull(), new MetadataConverter() {

        private static final long serialVersionUID = 1L;

        @Override
        public Object read(SourceRecord record) {
            return StringData.fromString(getMetaData(record, AbstractSourceInfo.DATABASE_NAME_KEY));
        }
    }),

    /**
     * It indicates the time that the change was made in the database. If the record is read from
     * snapshot of the table instead of the binlog, the value is always 0.
     */
    META_OP_TS("meta.op_ts", DataTypes.TIMESTAMP_WITH_LOCAL_TIME_ZONE(3).notNull(), new MetadataConverter() {

        private static final long serialVersionUID = 1L;

        @Override
        public Object read(SourceRecord record) {
            Struct messageStruct = (Struct) record.value();
            Struct sourceStruct = messageStruct.getStruct(FieldName.SOURCE);
            return TimestampData.fromEpochMillis((Long) sourceStruct.get(AbstractSourceInfo.TIMESTAMP_KEY));
        }
    }),

    /**
     * Operation type, INSERT/UPDATE/DELETE.
     */
    OP_TYPE("meta.op_type", DataTypes.STRING().notNull(), new MetadataConverter() {

        private static final long serialVersionUID = 1L;

        @Override
        public Object read(SourceRecord record) {
            return StringData.fromString(getOpType(record));
        }
    }),

    /**
     * Not important, a simple increment counter.
     */
    BATCH_ID("meta.batch_id", DataTypes.BIGINT().nullable(), new MetadataConverter() {

        private static final long serialVersionUID = 1L;

        private long id = 0;

        @Override
        public Object read(SourceRecord record) {
            return id++;
        }
    }),

    /**
     * Source does not emit ddl data.
     */
    IS_DDL("meta.is_ddl", DataTypes.BOOLEAN().notNull(), new MetadataConverter() {

        private static final long serialVersionUID = 1L;

        @Override
        public Object read(SourceRecord record) {
            return false;
        }
    }),

    /**
     * The update-before data for UPDATE record.
     */
    OLD("meta.update_before",
            DataTypes.ARRAY(DataTypes.MAP(DataTypes.STRING().nullable(), DataTypes.STRING().nullable()).nullable())
                    .nullable(),
            new MetadataConverter() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    final Envelope.Operation op = Envelope.operationFor(record);
                    if (op != Envelope.Operation.UPDATE) {
                        return null;
                    }
                    return record;
                }
            }),

    /**
     * Primary keys of the table
     */
    PK_NAMES("meta.pk_names", DataTypes.ARRAY(DataTypes.STRING().nullable()).nullable(), new MetadataConverter() {

        private static final long serialVersionUID = 1L;

        @Override
        public Object read(SourceRecord record) {
            return null;
        }

        @Override
        public Object read(SourceRecord record, @Nullable TableChange tableSchema) {
            if (tableSchema == null) {
                return null;
            }
            return new GenericArrayData(
                    tableSchema.getTable().primaryKeyColumnNames().stream().map(StringData::fromString).toArray());
        }
    }),

    /**
     * The sql which generate the data change stream. Not implement yet.
     */
    SQL("meta.sql", DataTypes.STRING().nullable(), new MetadataConverter() {

        private static final long serialVersionUID = 1L;

        @Override
        public Object read(SourceRecord record) {
            return StringData.fromString("");
        }
    }),

    /**
     * The PostgreSQL column type
     */
    SQL_TYPE("meta.sql_type", DataTypes.MAP(DataTypes.STRING().nullable(), DataTypes.INT().nullable()).nullable(),
            new MetadataConverter() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return null;
                }

                @Override
                public Object read(SourceRecord record, @Nullable TableChange tableSchema) {

                    Map<StringData, Integer> postgresType = new HashMap<>();
                    final Table table = tableSchema.getTable();
                    table.columns().forEach(column -> {
                        postgresType.put(StringData.fromString(column.name()), column.jdbcType());
                    });

                    return new GenericMapData(postgresType);
                }
            }),

    /**
     * It indicates the time that the change was made in the database. If the record is read from
     * snapshot of the table instead of the binlog, the value is always 0.
     *
     * Used when data stream is debezium json format.
     */
    TS("meta.ts", DataTypes.TIMESTAMP_WITH_LOCAL_TIME_ZONE(3).notNull(), new MetadataConverter() {

        private static final long serialVersionUID = 1L;

        @Override
        public Object read(SourceRecord record) {
            Struct messageStruct = (Struct) record.value();
            return TimestampData.fromEpochMillis((Long) messageStruct.get(FieldName.TIMESTAMP));
        }
    });

    /**
     * Generate a canal json message
     *
     * @param record
     * @param tableSchema
     * @param rowData
     * @return
     */
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

        CanalJson canalJson = CanalJson.builder().data(dataList).database(databaseName).schema(schemaName).sql("")
                .es(opTs).isDdl(false).pkNames(getPkNames(tableSchema)).table(tableName).ts(ts).type(getOpType(record))
                .sqlType(getSqlType(tableSchema)).build();
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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    PostgreSQLReadableMetaData(String key, DataType dataType, MetadataConverter converter) {
        this.key = key;
        this.dataType = dataType;
        this.converter = converter;
    }

    /**
     * convert debezium operation to canal-json operation type
     *
     * @param record
     * @return
     */
    private static String getOpType(SourceRecord record) {
        String opType;
        final Envelope.Operation op = Envelope.operationFor(record);
        if (op == Envelope.Operation.CREATE || op == Envelope.Operation.READ) {
            opType = "INSERT";
        } else if (op == Envelope.Operation.DELETE) {
            opType = "DELETE";
        } else {
            opType = "UPDATE";
        }
        return opType;
    }

    /**
     * get primary key names
     *
     * @param tableSchema
     * @return
     */
    private static List<String> getPkNames(@Nullable TableChange tableSchema) {
        if (tableSchema == null) {
            return null;
        }
        return tableSchema.getTable().primaryKeyColumnNames();
    }

    /**
     * get a map about column name and type
     *
     * @param tableSchema
     * @return
     */
    public static Map<String, Integer> getSqlType(@Nullable TableChange tableSchema) {
        if (tableSchema == null) {
            return null;
        }
        Map<String, Integer> postgresType = new LinkedHashMap<>();
        final Table table = tableSchema.getTable();
        table.columns().forEach(column -> {
            postgresType.put(column.name(), column.jdbcType());
        });
        return postgresType;
    }

    /**
     * convert debezium operation to debezium-json operation type
     *
     * @param record
     * @return
     */
    private static String getDebeziumOpType(SourceRecord record) {
        String opType;
        final Envelope.Operation op = Envelope.operationFor(record);
        if (op == Envelope.Operation.CREATE || op == Envelope.Operation.READ) {
            opType = "c";
        } else if (op == Envelope.Operation.DELETE) {
            opType = "d";
        } else {
            opType = "u";
        }
        return opType;
    }

    /**
     * get meta info from debezium-json data stream
     *
     * @param record
     * @param tableNameKey
     * @return
     */
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
