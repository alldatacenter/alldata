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

package org.apache.inlong.sort.cdc.mongodb.source.utils;

import com.ververica.cdc.connectors.mongodb.internal.MongoDBEnvelope;
import io.debezium.connector.AbstractSourceInfo;
import io.debezium.data.Envelope;
import io.debezium.data.Envelope.FieldName;
import io.debezium.relational.history.TableChanges.TableChange;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.inlong.sort.cdc.mongodb.debezium.DebeziumJson;
import org.apache.inlong.sort.cdc.mongodb.debezium.DebeziumJson.Source;
import org.apache.inlong.sort.cdc.mongodb.debezium.utils.RecordUtils;
import org.apache.inlong.sort.formats.json.canal.CanalJson;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;

public class MetaDataUtils {

    private static final String MONGODB_DEFAULT_PRIMARY_KEY = "_id";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * get collection name from record
     */
    public static String getMetaData(SourceRecord record, String metaDataKey) {
        Struct value = (Struct) record.value();
        Struct to = value.getStruct(MongoDBEnvelope.NAMESPACE_FIELD);
        return to.getString(metaDataKey);
    }

    /**
     * get sql type from row data, represents the jdbc data type
     */
    public static Map<String, Integer> getSqlType(@Nullable RowData rowData) {
        if (rowData == null) {
            return null;
        }
        GenericRowData data = (GenericRowData) rowData;
        Map<String, String> mongoDbType = (Map<String, String>) data.getField(1);
        Map<String, Integer> sqlType = new LinkedHashMap<>();
        mongoDbType.forEach((name, value) -> sqlType.put(name, RecordUtils.getSqlType(value)));
        return sqlType;
    }

    private static String getDebeziumOpType(RowData rowData) {
        String opType;
        switch (rowData.getRowKind()) {
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

    private static String getCanalOpType(RowData rowData) {
        String opType;
        switch (rowData.getRowKind()) {
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

    public static StringData getCanalData(SourceRecord record, RowData rowData,
            TableChange tableSchema) {
        // construct canal json
        Struct messageStruct = (Struct) record.value();
        Struct sourceStruct = messageStruct.getStruct(Envelope.FieldName.SOURCE);
        GenericRowData data = (GenericRowData) rowData;
        Map<String, Object> field = (Map<String, Object>) data.getField(0);
        Map<String, String> mongoDbType = (Map<String, String>) data.getField(1);

        String database = getMetaData(record, MongoDBEnvelope.NAMESPACE_DATABASE_FIELD);
        String table = getMetaData(record, MongoDBEnvelope.NAMESPACE_COLLECTION_FIELD);
        Long opTs = (Long) sourceStruct.get(AbstractSourceInfo.TIMESTAMP_KEY);
        long ts = (Long) messageStruct.get(FieldName.TIMESTAMP);

        List<Map<String, Object>> dataList = new ArrayList<>();
        dataList.add(field);
        CanalJson canalJson = CanalJson.builder()
                .data(dataList)
                .database(database)
                .sql("")
                .es(opTs)
                .isDdl(false)
                .pkNames(Collections.singletonList(MONGODB_DEFAULT_PRIMARY_KEY))
                .mysqlType(mongoDbType)
                .table(table)
                .ts(ts)
                .type(getCanalOpType(rowData))
                .sqlType(getSqlType(data))
                .build();
        try {
            return StringData.fromString(OBJECT_MAPPER.writeValueAsString(canalJson));
        } catch (Exception e) {
            throw new IllegalStateException("exception occurs when get meta data", e);
        }
    }

    public static StringData getDebeziumData(SourceRecord record, TableChange tableSchema,
            RowData rowData) {
        // construct debezium json
        Struct messageStruct = (Struct) record.value();
        GenericRowData data = (GenericRowData) rowData;
        Map<String, Object> field = (Map<String, Object>) data.getField(0);
        Map<String, String> mongoDbType = (Map<String, String>) data.getField(1);

        String database = getMetaData(record, MongoDBEnvelope.NAMESPACE_DATABASE_FIELD);
        String table = getMetaData(record, MongoDBEnvelope.NAMESPACE_COLLECTION_FIELD);
        long ts = (Long) messageStruct.get(FieldName.TIMESTAMP);
        String debeziumOp = getDebeziumOpType(rowData);

        Source source = Source.builder()
                .db(database)
                .table(table)
                .name("mongodb_cdc_source")
                .mysqlType(mongoDbType)
                .sqlType(getSqlType(rowData))
                .pkNames(Collections.singletonList(MONGODB_DEFAULT_PRIMARY_KEY))
                .build();
        DebeziumJson debeziumJson = DebeziumJson.builder()
                .source(source)
                .after(field)
                .tsMs(ts)
                .op(debeziumOp)
                .tableChange(tableSchema)
                .build();
        try {
            return StringData.fromString(OBJECT_MAPPER.writeValueAsString(debeziumJson));
        } catch (Exception e) {
            throw new IllegalStateException("exception occurs when get meta data", e);
        }
    }

}
