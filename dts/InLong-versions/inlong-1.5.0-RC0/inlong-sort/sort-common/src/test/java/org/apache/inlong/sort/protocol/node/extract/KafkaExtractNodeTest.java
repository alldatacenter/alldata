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

package org.apache.inlong.sort.protocol.node.extract;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.inlong.common.enums.MetaField;
import org.apache.inlong.sort.SerializeBaseTest;
import org.apache.inlong.sort.formats.common.IntFormatInfo;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.enums.KafkaScanStartupMode;
import org.apache.inlong.sort.protocol.node.format.CsvFormat;
import org.apache.inlong.sort.protocol.node.format.InLongMsgFormat;
import org.apache.inlong.sort.protocol.node.format.RawFormat;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;

/**
 * Test for {@link KafkaExtractNode}
 */
public class KafkaExtractNodeTest extends SerializeBaseTest<KafkaExtractNode> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public KafkaExtractNode getTestObject() {
        List<FieldInfo> fields = Arrays.asList(
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()));
        return new KafkaExtractNode("1", "kafka_input", fields, null, null, "workerCsv",
                "localhost:9092", new CsvFormat(), KafkaScanStartupMode.EARLIEST_OFFSET, null, "groupId", null, null);
    }

    @Test
    public void testKafkaExtractNodeForScanSpecificOffsets() throws JsonProcessingException {
        List<FieldInfo> fields = Arrays.asList(
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()));
        KafkaExtractNode kafkaExtractNode = new KafkaExtractNode("1", "kafka_input", fields, null, null, "workerCsv",
                "localhost:9092", new CsvFormat(), KafkaScanStartupMode.SPECIFIC_OFFSETS, null, "groupId",
                "partition:0,offset:42;partition:1,offset:300",
                null);
        KafkaExtractNode expected = objectMapper.readValue(objectMapper.writeValueAsString(kafkaExtractNode),
                KafkaExtractNode.class);
        assertEquals(expected, kafkaExtractNode);
    }

    @Test
    public void testKafkaExtractNodeForScanTimestampMillis() throws JsonProcessingException {
        List<FieldInfo> fields = Arrays.asList(
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()));
        KafkaExtractNode kafkaExtractNode = new KafkaExtractNode("1", "kafka_input", fields, null, null, "workerCsv",
                "localhost:9092", new RawFormat(), KafkaScanStartupMode.TIMESTAMP_MILLIS, null, "groupId", null,
                "1665198979108");
        KafkaExtractNode expected = objectMapper.readValue(objectMapper.writeValueAsString(kafkaExtractNode),
                KafkaExtractNode.class);
        assertEquals(expected, kafkaExtractNode);
    }

    @Test
    public void testMetaFields() {
        Map<MetaField, String> formatMap = new HashMap<>();
        formatMap.put(MetaField.PROCESS_TIME, "AS PROCTIME()");
        formatMap.put(MetaField.TABLE_NAME, "STRING METADATA FROM 'value.table'");
        formatMap.put(MetaField.DATABASE_NAME, "STRING METADATA FROM 'value.database'");
        formatMap.put(MetaField.OP_TYPE, "STRING METADATA FROM 'value.type'");
        formatMap.put(MetaField.OP_TS, "TIMESTAMP_LTZ(3) METADATA FROM 'value.event-timestamp'");
        formatMap.put(MetaField.IS_DDL, "BOOLEAN METADATA FROM 'value.is-ddl'");
        formatMap.put(MetaField.TS, "TIMESTAMP_LTZ(3) METADATA FROM 'value.ingestion-timestamp'");
        formatMap.put(MetaField.SQL_TYPE, "MAP<STRING, INT> METADATA FROM 'value.sql-type'");
        formatMap.put(MetaField.MYSQL_TYPE, "MAP<STRING, STRING> METADATA FROM 'value.mysql-type'");
        formatMap.put(MetaField.PK_NAMES, "ARRAY<STRING> METADATA FROM 'value.pk-names'");
        formatMap.put(MetaField.BATCH_ID, "BIGINT METADATA FROM 'value.batch-id'");
        formatMap.put(MetaField.UPDATE_BEFORE, "ARRAY<MAP<STRING, STRING>> METADATA FROM 'value.update-before'");
        formatMap.put(MetaField.KEY, "STRING METADATA FROM 'key' VIRTUAL");
        formatMap.put(MetaField.VALUE, "STRING METADATA FROM 'value' VIRTUAL");
        formatMap.put(MetaField.HEADERS, "MAP<STRING, BINARY> METADATA FROM 'headers' VIRTUAL");
        formatMap.put(MetaField.HEADERS_TO_JSON_STR, "STRING METADATA FROM 'headers_to_json_str' VIRTUAL");
        formatMap.put(MetaField.OFFSET, "BIGINT METADATA FROM 'offset' VIRTUAL");
        formatMap.put(MetaField.PARTITION, "BIGINT METADATA FROM 'partition' VIRTUAL");
        formatMap.put(MetaField.TIMESTAMP, "TIMESTAMP_LTZ(3) METADATA FROM 'timestamp' VIRTUAL");

        KafkaExtractNode node = getTestObject();
        boolean formatEquals = true;
        for (MetaField metaField : node.supportedMetaFields()) {
            formatEquals = node.format(metaField).equals(formatMap.get(metaField));
            if (!formatEquals) {
                break;
            }
        }
        Assert.assertTrue(formatEquals);
    }

    @Test
    public void testInLongFormat() {
        List<FieldInfo> fields = Arrays.asList(
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()));

        KafkaExtractNode kafkaNode = getTestObject();
        InLongMsgFormat inLongMsgFormat = new InLongMsgFormat(new CsvFormat(), false);
        kafkaNode.setFormat(inLongMsgFormat);

        Map<String, String> options = kafkaNode.tableOptions();
        assertEquals("inlong-msg", options.get("format"));
        assertEquals("csv", options.get("inlong-msg.inner.format"));
        assertEquals("true", options.get("inlong-msg.csv.ignore-parse-errors"));

        kafkaNode.setFormat(new CsvFormat());
        Map<String, String> csvOptions = kafkaNode.tableOptions();
        assertEquals("csv", csvOptions.get("format"));
        assertEquals("true", csvOptions.get("csv.ignore-parse-errors"));
    }
}
