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

import org.apache.inlong.common.enums.MetaField;
import org.apache.inlong.sort.SerializeBaseTest;
import org.apache.inlong.sort.formats.common.IntFormatInfo;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.enums.KafkaScanStartupMode;
import org.apache.inlong.sort.protocol.node.format.CsvFormat;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test for {@link KafkaExtractNode}
 */
public class KafkaExtractNodeTest extends SerializeBaseTest<KafkaExtractNode> {

    @Override
    public KafkaExtractNode getTestObject() {
        List<FieldInfo> fields = Arrays.asList(
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()));
        return new KafkaExtractNode("1", "kafka_input", fields, null, null, "workerCsv",
                "localhost:9092", new CsvFormat(), KafkaScanStartupMode.EARLIEST_OFFSET, null, "groupId", null);
    }

    @Test
    public void testMetaFields() {
        Map<MetaField, String> formatMap = new HashMap<>();
        formatMap.put(MetaField.PROCESS_TIME, "AS PROCTIME()");
        formatMap.put(MetaField.TABLE_NAME, "STRING METADATA FROM 'value.table'");
        formatMap.put(MetaField.DATABASE_NAME, "STRING METADATA FROM 'value.database'");
        formatMap.put(MetaField.OP_TYPE, "STRING METADATA FROM 'value.op-type'");
        formatMap.put(MetaField.OP_TS, "TIMESTAMP_LTZ(3) METADATA FROM 'value.event-timestamp'");
        formatMap.put(MetaField.IS_DDL, "BOOLEAN METADATA FROM 'value.is-ddl'");
        formatMap.put(MetaField.TS, "TIMESTAMP_LTZ(3) METADATA FROM 'value.ingestion-timestamp'");
        formatMap.put(MetaField.SQL_TYPE, "MAP<STRING, INT> METADATA FROM 'value.sql-type'");
        formatMap.put(MetaField.MYSQL_TYPE, "MAP<STRING, STRING> METADATA FROM 'value.mysql-type'");
        formatMap.put(MetaField.PK_NAMES, "ARRAY<STRING> METADATA FROM 'value.pk-names'");
        formatMap.put(MetaField.BATCH_ID, "BIGINT METADATA FROM 'value.batch-id'");
        formatMap.put(MetaField.UPDATE_BEFORE, "ARRAY<MAP<STRING, STRING>> METADATA FROM 'value.update-before'");
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
}
