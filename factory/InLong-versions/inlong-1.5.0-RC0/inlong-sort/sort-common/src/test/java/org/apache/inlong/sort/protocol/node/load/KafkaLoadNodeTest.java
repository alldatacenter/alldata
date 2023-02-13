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

package org.apache.inlong.sort.protocol.node.load;

import org.apache.inlong.common.enums.MetaField;
import org.apache.inlong.sort.SerializeBaseTest;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.node.format.CanalJsonFormat;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Test for {@link KafkaLoadNode}
 */
public class KafkaLoadNodeTest extends SerializeBaseTest<KafkaLoadNode> {

    @Override
    public KafkaLoadNode getTestObject() {
        return new KafkaLoadNode("1", null,
                Arrays.asList(new FieldInfo("field", new StringFormatInfo())),
                Arrays.asList(new FieldRelation(new FieldInfo("field", new StringFormatInfo()),
                        new FieldInfo("field", new StringFormatInfo()))),
                null, null,
                "topic", "localhost:9092", new CanalJsonFormat(),
                1, new TreeMap<>(), null);
    }

    @Test
    public void testMetaFields() {
        Map<MetaField, String> formatMap = new HashMap<>();
        formatMap.put(MetaField.PROCESS_TIME, "AS PROCTIME()");
        formatMap.put(MetaField.DATA_CANAL, "STRING METADATA FROM 'value.data_canal'");
        formatMap.put(MetaField.DATA, "STRING METADATA FROM 'value.data_canal'");
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
        KafkaLoadNode node = getTestObject();
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
