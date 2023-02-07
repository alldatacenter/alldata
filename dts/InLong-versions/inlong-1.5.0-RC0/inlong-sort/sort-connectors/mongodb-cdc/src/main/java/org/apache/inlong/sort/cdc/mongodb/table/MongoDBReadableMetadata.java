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

package org.apache.inlong.sort.cdc.mongodb.table;

import com.ververica.cdc.connectors.mongodb.internal.MongoDBEnvelope;
import io.debezium.connector.AbstractSourceInfo;
import io.debezium.data.Envelope;
import io.debezium.relational.Table;
import io.debezium.relational.history.TableChanges;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;
import org.apache.flink.table.types.DataType;
import org.apache.inlong.sort.cdc.mongodb.debezium.table.MetadataConverter;
import org.apache.inlong.sort.cdc.mongodb.debezium.utils.RecordUtils;
import org.apache.inlong.sort.formats.json.canal.CanalJson;
import org.apache.inlong.sort.formats.json.debezium.DebeziumJson;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;

/** Defines the supported metadata columns for {@link MongoDBTableSource}. */
public enum MongoDBReadableMetadata {

    COLLECTION(
            "collection_name",
            DataTypes.STRING().notNull(),
            new MetadataConverter() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    Struct value = (Struct) record.value();
                    Struct to = value.getStruct(MongoDBEnvelope.NAMESPACE_FIELD);
                    return StringData.fromString(
                            to.getString(MongoDBEnvelope.NAMESPACE_COLLECTION_FIELD));
                }
            }),

    DATABASE(
            "database_name",
            DataTypes.STRING().notNull(),
            new MetadataConverter() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    Struct value = (Struct) record.value();
                    Struct to = value.getStruct(MongoDBEnvelope.NAMESPACE_FIELD);
                    return StringData.fromString(
                            to.getString(MongoDBEnvelope.NAMESPACE_DATABASE_FIELD));
                }
            }),

    OP_TS(
            "op_ts",
            DataTypes.TIMESTAMP_LTZ(3).notNull(),
            new MetadataConverter() {

                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    Struct value = (Struct) record.value();
                    Struct source = value.getStruct(Envelope.FieldName.SOURCE);
                    return TimestampData.fromEpochMillis(
                            (Long) source.get(AbstractSourceInfo.TIMESTAMP_KEY));
                }
            }),

    DATA_DEBEZIUM(
            "meta.data_debezium",
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

                    // construct debezium json
                    Struct messageStruct = (Struct) record.value();
                    Struct to = messageStruct.getStruct(MongoDBEnvelope.NAMESPACE_FIELD);
                    Struct sourceStruct = messageStruct.getStruct(Envelope.FieldName.SOURCE);
                    GenericRowData data = (GenericRowData) rowData;
                    Map<String, Object> field = (Map<String, Object>) data.getField(0);
                    Map<String, String> mysqlType = (Map<String, String>) data.getField(1);
                    Map<String, Integer> sqlType = new HashMap<>();
                    mysqlType.forEach((name, value) -> sqlType.put(name, RecordUtils.getSqlType(value)));
                    String debeziumOp = getDebeziumOpType(rowData);
                    if (StringUtils.isBlank(debeziumOp)) {
                        return null;
                    }
                    DebeziumJson.Source source = DebeziumJson.Source.builder()
                            .db(to.getString(MongoDBEnvelope.NAMESPACE_DATABASE_FIELD))
                            .table(to.getString(MongoDBEnvelope.NAMESPACE_COLLECTION_FIELD))
                            .name("mongo_binlog_source")
                            .mysqlType(mysqlType)
                            .sqlType(sqlType)
                            .pkNames(null)
                            .build();
                    DebeziumJson debeziumJson = DebeziumJson.builder()
                            .source(source)
                            .after(field)
                            .tsMs((Long) sourceStruct.get(AbstractSourceInfo.TIMESTAMP_KEY))
                            .op(debeziumOp)
                            .tableChange(tableSchema)
                            .build();
                    try {
                        return StringData.fromString(OBJECT_MAPPER.writeValueAsString(debeziumJson));
                    } catch (Exception e) {
                        throw new IllegalStateException("exception occurs when get meta data", e);
                    }
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
                    // construct canal json
                    Struct messageStruct = (Struct) record.value();
                    Struct to = messageStruct.getStruct(MongoDBEnvelope.NAMESPACE_FIELD);
                    Struct sourceStruct = messageStruct.getStruct(Envelope.FieldName.SOURCE);
                    String canalOp = getCanalOpType(rowData);
                    if (StringUtils.isBlank(canalOp)) {
                        return null;
                    }
                    GenericRowData data = (GenericRowData) rowData;
                    Map<String, Object> field = (Map<String, Object>) data.getField(0);
                    Map<String, String> mysqlType = (Map<String, String>) data.getField(1);
                    Map<String, Integer> sqlType = new HashMap<>();
                    mysqlType.forEach((name, value) -> sqlType.put(name, RecordUtils.getSqlType(value)));
                    List<Map<String, Object>> dataList = new ArrayList<>();
                    dataList.add(field);
                    CanalJson canalJson = CanalJson.builder()
                            .data(dataList)
                            .database(to.getString(MongoDBEnvelope.NAMESPACE_DATABASE_FIELD))
                            .sql("")
                            .es((Long) sourceStruct.get(AbstractSourceInfo.TIMESTAMP_KEY))
                            .isDdl(false)
                            .pkNames(null)
                            .mysqlType(getMysqlType(tableSchema))
                            .table(to.getString(MongoDBEnvelope.NAMESPACE_COLLECTION_FIELD))
                            .ts((Long) sourceStruct.get(AbstractSourceInfo.TIMESTAMP_KEY))
                            .type(canalOp)
                            .sqlType(sqlType)
                            .build();
                    try {
                        return StringData.fromString(OBJECT_MAPPER.writeValueAsString(canalJson));
                    } catch (Exception e) {
                        throw new IllegalStateException("exception occurs when get meta data", e);
                    }
                }
            });

    private final String key;
    private final DataType dataType;
    private final MetadataConverter converter;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    MongoDBReadableMetadata(String key, DataType dataType, MetadataConverter converter) {
        this.key = key;
        this.dataType = dataType;
        this.converter = converter;
    }

    public static Map<String, String> getMysqlType(@Nullable TableChanges.TableChange tableSchema) {
        if (tableSchema == null) {
            return null;
        }
        Map<String, String> mysqlType = new LinkedHashMap<>();
        final Table table = tableSchema.getTable();
        table.columns()
                .forEach(
                        column -> {
                            mysqlType.put(
                                    column.name(),
                                    String.format(
                                            "%s(%d)",
                                            column.typeName(),
                                            column.length()));
                        });
        return mysqlType;
    }

    private static String getDebeziumOpType(RowData rowData) {
        String opType = null;
        switch (rowData.getRowKind()) {
            case INSERT:
                opType = "c";
                break;
            case DELETE:
                opType = "d";
                break;
            case UPDATE_AFTER:
            case UPDATE_BEFORE:
                opType = "u";
                break;
            default:
                return null;
        }
        return opType;
    }

    private static String getCanalOpType(RowData rowData) {
        String opType = null;
        switch (rowData.getRowKind()) {
            case INSERT:
                opType = "INSERT";
                break;
            case DELETE:
                opType = "DELETE";
                break;
            case UPDATE_AFTER:
            case UPDATE_BEFORE:
                opType = "UPDATE";
                break;
            default:
                return null;
        }
        return opType;
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
