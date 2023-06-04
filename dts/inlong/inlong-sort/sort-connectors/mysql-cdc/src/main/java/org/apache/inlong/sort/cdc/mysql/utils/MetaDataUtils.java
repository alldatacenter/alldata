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

package org.apache.inlong.sort.cdc.mysql.utils;

import static org.apache.inlong.sort.base.Constants.DDL_FIELD_NAME;
import static org.apache.inlong.sort.cdc.mysql.source.utils.RecordUtils.isSnapshotRecord;
import static org.apache.inlong.sort.cdc.mysql.utils.OperationUtils.generateOperation;
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
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.StringData;
import org.apache.inlong.sort.cdc.base.util.RecordUtils;
import org.apache.inlong.sort.formats.json.canal.CanalJson;
import org.apache.inlong.sort.formats.json.debezium.DebeziumJson;
import org.apache.inlong.sort.formats.json.debezium.DebeziumJson.Source;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utils for generating metadata in mysql cdc.
 */
public class MetaDataUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Logger LOG = LoggerFactory.getLogger(MetaDataUtils.class);

    /**
     * get sql type from table schema, represents the jdbc data type
     *
     * @param tableSchema table schema
     */
    public static Map<String, Integer> getSqlType(@Nullable TableChanges.TableChange tableSchema) {
        if (tableSchema == null) {
            return null;
        }
        Map<String, Integer> sqlType = new LinkedHashMap<>();
        final Table table = tableSchema.getTable();
        table.columns().forEach(
                column -> sqlType.put(column.name(), column.jdbcType()));
        return sqlType;
    }

    public static String getMetaData(SourceRecord record, String tableNameKey) {
        Struct messageStruct = (Struct) record.value();
        Struct sourceStruct = messageStruct.getStruct(FieldName.SOURCE);
        return sourceStruct.getString(tableNameKey);
    }

    public static String getOpType(SourceRecord record) {
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

    public static String getCanalOpType(GenericRowData record) {
        String opType;
        switch (record.getRowKind()) {
            case DELETE:
            case UPDATE_BEFORE:
                opType = "DELETE";
                break;
            case INSERT:
            case UPDATE_AFTER:
                opType = "INSERT";
                break;
            default:
                throw new IllegalStateException("the record only have states in DELETE, "
                        + "UPDATE_BEFORE, INSERT and UPDATE_AFTER");
        }
        return opType;
    }

    public static String getDebeziumOpType(GenericRowData record) {
        String opType;
        switch (record.getRowKind()) {
            case DELETE:
            case UPDATE_BEFORE:
                opType = "d";
                break;
            case INSERT:
            case UPDATE_AFTER:
                opType = "c";
                break;
            default:
                throw new IllegalStateException("the record only have states in DELETE, "
                        + "UPDATE_BEFORE, INSERT and UPDATE_AFTER");
        }
        return opType;
    }

    public static List<String> getPkNames(@Nullable TableChanges.TableChange tableSchema) {
        if (tableSchema == null) {
            return null;
        }
        return tableSchema.getTable().primaryKeyColumnNames();
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

    public static StringData getCanalData(SourceRecord record, GenericRowData rowData,
            TableChange tableSchema) {
        Struct messageStruct = (Struct) record.value();
        Struct sourceStruct = messageStruct.getStruct(FieldName.SOURCE);
        // tableName
        String tableName = getMetaData(record, AbstractSourceInfo.TABLE_NAME_KEY);
        // databaseName
        String databaseName = getMetaData(record, AbstractSourceInfo.DATABASE_NAME_KEY);
        // opTs
        long opTs = (Long) sourceStruct.get(AbstractSourceInfo.TIMESTAMP_KEY);
        // actual data
        Map<String, Object> field = (Map<String, Object>) rowData.getField(0);
        List<Map<String, Object>> dataList = new ArrayList<>();

        CanalJson canalJson = CanalJson.builder()
                .database(databaseName)
                .es(opTs).pkNames(getPkNames(tableSchema))
                .mysqlType(getMysqlType(tableSchema)).table(tableName)
                .incremental(!isSnapshotRecord(sourceStruct))
                .dataSourceName(sourceStruct.getString(AbstractSourceInfo.SERVER_NAME_KEY))
                .type(getCanalOpType(rowData))
                .sqlType(getSqlType(tableSchema))
                .build();

        try {
            if (RecordUtils.isDdlRecord(messageStruct)) {
                String sql = (String) field.get(DDL_FIELD_NAME);
                canalJson.setSql(sql);
                canalJson.setOperation(generateOperation(sql, getSqlType(tableSchema)));
                canalJson.setDdl(true);
                canalJson.setData(dataList);
            } else {
                canalJson.setDdl(false);
                canalJson.setTs((Long) messageStruct.get(FieldName.TIMESTAMP));
                dataList.add(field);
                canalJson.setData(dataList);
            }
            LOG.debug("canal json: {}", canalJson);
            return StringData.fromString(OBJECT_MAPPER.writeValueAsString(canalJson));
        } catch (Exception e) {
            throw new IllegalStateException("exception occurs when get meta data", e);
        }
    }

    public static StringData getDebeziumData(SourceRecord record, TableChange tableSchema,
            GenericRowData rowData) {
        // construct debezium json
        Struct messageStruct = (Struct) record.value();
        Struct sourceStruct = messageStruct.getStruct(FieldName.SOURCE);
        GenericRowData data = rowData;
        Map<String, Object> field = (Map<String, Object>) data.getField(0);

        Source source = Source.builder().db(getMetaData(record, AbstractSourceInfo.DATABASE_NAME_KEY))
                .table(getMetaData(record, AbstractSourceInfo.TABLE_NAME_KEY))
                .name(sourceStruct.getString(AbstractSourceInfo.SERVER_NAME_KEY))
                .sqlType(getSqlType(tableSchema))
                .pkNames(getPkNames(tableSchema))
                .mysqlType(getMysqlType(tableSchema))
                .build();
        DebeziumJson debeziumJson = DebeziumJson.builder().source(source)
                .tsMs(sourceStruct.getInt64(AbstractSourceInfo.TIMESTAMP_KEY)).op(getDebeziumOpType(data))
                .dataSourceName(sourceStruct.getString(AbstractSourceInfo.SERVER_NAME_KEY))
                .tableChange(tableSchema).incremental(!isSnapshotRecord(sourceStruct)).build();

        try {
            if (RecordUtils.isDdlRecord(messageStruct)) {
                String sql = (String) field.get(DDL_FIELD_NAME);
                debeziumJson.setDdl(sql);
                debeziumJson.setOperation(generateOperation(sql, getSqlType(tableSchema)));
                debeziumJson.setAfter(new HashMap<>());
            } else {
                debeziumJson.setAfter(field);
            }
            return StringData.fromString(OBJECT_MAPPER.writeValueAsString(debeziumJson));
        } catch (Exception e) {
            throw new IllegalStateException("exception occurs when get meta data {}", e);
        }

    }

}
