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

package org.apache.inlong.sort.formats.json.canal;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.formats.common.TimestampFormat;
import org.apache.flink.formats.json.JsonOptions;
import org.apache.flink.formats.json.canal.CanalJsonOptions;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.catalog.Column;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.runtime.typeutils.InternalTypeInfo;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.utils.DataTypeUtils;
import org.apache.flink.util.Collector;
import org.apache.inlong.sort.formats.json.canal.CanalJsonEnhancedDecodingFormat.ReadableMetadata;
import org.apache.inlong.sort.formats.json.canal.CanalJsonEnhancedEncodingFormat.WriteableMetadata;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class CanalJsonEnhancedSerDeSchemaTest {

    public static final String DATABASE = "TEST";

    public static final String TABLE = "TEST";

    public static final ResolvedSchema SCHEMA =
            ResolvedSchema.of(
                    Column.metadata("database", DataTypes.BOOLEAN(), "database", false),
                    Column.physical("id", DataTypes.BIGINT()),
                    Column.physical("name", DataTypes.STRING()),
                    Column.metadata("table", DataTypes.BOOLEAN(), "table", false),
                    Column.metadata("sql_type",
                            DataTypes.MAP(DataTypes.STRING(), DataTypes.INT()), "sql-type", false),
                    Column.metadata("pk_names",
                            DataTypes.ARRAY(DataTypes.STRING()), "pk-names", false),
                    Column.metadata("ingestion_timestamp",
                            DataTypes.TIMESTAMP_LTZ(3), "ingestion-timestamp", false),
                    Column.metadata("event_timestamp",
                            DataTypes.TIMESTAMP_LTZ(3), "event-timestamp", false),
                    Column.metadata("op_type", DataTypes.STRING(), "op-type", false),
                    Column.metadata("is_ddl", DataTypes.BOOLEAN(), "is-ddl", false),
                    Column.metadata("mysql_type",
                            DataTypes.MAP(DataTypes.STRING(), DataTypes.STRING()), "mysql-type", false),
                    Column.metadata("batch_id", DataTypes.BIGINT(), "batch-id", false),
                    Column.metadata("update_before",
                            DataTypes.ARRAY(DataTypes.MAP(DataTypes.STRING(), DataTypes.STRING())),
                            "update-before", false));

    public static final DataType PHYSICAL_DATA_TYPE = SCHEMA.toPhysicalRowDataType();

    public static final List<ReadableMetadata> READABLE_METADATA =
            Stream.of(
                    ReadableMetadata.DATABASE,
                    ReadableMetadata.TABLE,
                    ReadableMetadata.SQL_TYPE,
                    ReadableMetadata.PK_NAMES,
                    ReadableMetadata.INGESTION_TIMESTAMP,
                    ReadableMetadata.EVENT_TIMESTAMP,
                    ReadableMetadata.OP_TYPE,
                    ReadableMetadata.IS_DDL,
                    ReadableMetadata.MYSQL_TYPE,
                    ReadableMetadata.BATCH_ID,
                    ReadableMetadata.UPDATE_BEFORE).collect(Collectors.toList());

    public static final List<WriteableMetadata> WRITEABLE_METADATA =
            Stream.of(
                    WriteableMetadata.DATABASE,
                    WriteableMetadata.TABLE,
                    WriteableMetadata.SQL_TYPE,
                    WriteableMetadata.PK_NAMES,
                    WriteableMetadata.INGESTION_TIMESTAMP,
                    WriteableMetadata.EVENT_TIMESTAMP,
                    WriteableMetadata.OP_TYPE,
                    WriteableMetadata.IS_DDL,
                    WriteableMetadata.MYSQL_TYPE,
                    WriteableMetadata.BATCH_ID,
                    WriteableMetadata.UPDATE_BEFORE).collect(Collectors.toList());

    @Test
    public void testSerDeWithMetadata() throws Exception {
        List<String> lines = readLines("canal-json-inlong-data.txt");
        DeserializationSchema<RowData> deserializationSchema = createCanalJsonDeserializationSchema(
                PHYSICAL_DATA_TYPE, READABLE_METADATA);
        // deserialize
        SimpleCollector out = new SimpleCollector();
        for (String line : lines) {
            deserializationSchema.deserialize(line.getBytes(StandardCharsets.UTF_8), out);
        }
        List<RowData> res = out.result();

        // serialize
        SerializationSchema<RowData> serializationSchema = createCanalJsonSerializationSchema(
                PHYSICAL_DATA_TYPE, WRITEABLE_METADATA);
        serializationSchema.open(null);
        for (int i = 0; i < lines.size(); i++) {
            String json = new String(serializationSchema.serialize(res.get(i)), StandardCharsets.UTF_8);
            compareJson(json, lines.get(i));
        }
    }

    // =======================================Utils=======================================================

    private CanalJsonEnhancedDeserializationSchema createCanalJsonDeserializationSchema(
            DataType physicalDataType, List<ReadableMetadata> requestedMetadata) {
        final DataType producedDataType =
                DataTypeUtils.appendRowFields(
                        physicalDataType,
                        requestedMetadata.stream()
                                .map(m -> DataTypes.FIELD(m.key, m.dataType))
                                .collect(Collectors.toList()));
        return CanalJsonEnhancedDeserializationSchema.builder(
                PHYSICAL_DATA_TYPE,
                requestedMetadata,
                InternalTypeInfo.of(producedDataType.getLogicalType()))
                .setDatabase(DATABASE)
                .setTable(TABLE)
                .setIgnoreParseErrors(JsonOptions.IGNORE_PARSE_ERRORS.defaultValue())
                .setTimestampFormat(TimestampFormat.valueOf(CanalJsonOptions.TIMESTAMP_FORMAT.defaultValue()))
                .build();
    }

    private CanalJsonEnhancedSerializationSchema createCanalJsonSerializationSchema(
            DataType physicalDataType, List<WriteableMetadata> requestedMetadata) {
        return new CanalJsonEnhancedSerializationSchema(
                physicalDataType,
                requestedMetadata,
                TimestampFormat.valueOf(CanalJsonOptions.TIMESTAMP_FORMAT.defaultValue()),
                JsonOptions.MapNullKeyMode.valueOf(CanalJsonOptions.JSON_MAP_NULL_KEY_MODE.defaultValue()),
                CanalJsonOptions.JSON_MAP_NULL_KEY_LITERAL.defaultValue(),
                JsonOptions.ENCODE_DECIMAL_AS_PLAIN_NUMBER.defaultValue());
    }

    private static List<String> readLines(String resource) throws IOException {
        final URL url = CanalJsonEnhancedSerDeSchemaTest.class.getClassLoader().getResource(resource);
        assert url != null;
        Path path = new File(url.getFile()).toPath();
        return Files.readAllLines(path);
    }

    private static List<RowData> readRowDatas(String resource) throws IOException, ClassNotFoundException {
        final URL url = CanalJsonEnhancedSerDeSchemaTest.class.getClassLoader().getResource(resource);
        assert url != null;
        Path path = new File(url.getFile()).toPath();
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(path.toString()));
        return (List<RowData>) in.readObject();
    }

    public void compareJson(String json1, String json2) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node1 = objectMapper.readTree(json1);
        JsonNode node2 = objectMapper.readTree(json2);
        assertEquals(node1, node2);
    }

    private static class SimpleCollector implements Collector<RowData> {

        private List<RowData> list = new ArrayList<>();

        @Override
        public void collect(RowData record) {
            list.add(record);
        }

        @Override
        public void close() {
            // do nothing
        }

        public List<RowData> result() {
            List<RowData> newList = new ArrayList<>();
            list.forEach(row -> newList.add(row));
            list.clear();
            return newList;
        }
    }

}
