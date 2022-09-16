/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.apache.inlong.sort.protocol.node.extract;

import org.apache.inlong.common.enums.MetaField;
import org.apache.inlong.sort.SerializeBaseTest;
import org.apache.inlong.sort.formats.common.IntFormatInfo;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test for {@link PostgresExtractNodeTest}
 */
public class PostgresExtractNodeTest extends SerializeBaseTest<PostgresExtractNode> {

    @Override
    public PostgresExtractNode getTestObject() {
        List<FieldInfo> fields = Arrays.asList(
                new FieldInfo("name", new StringFormatInfo()),
                new FieldInfo("age", new IntFormatInfo()));
        return new PostgresExtractNode("1", "postgres_input", fields, null, null, null, Arrays.asList("mytable"),
                "localhost", "username", "password", "database", "public", 5432, null);
    }

    @Test
    public void testMetaFields() {
        Map<MetaField, String> formatMap = new HashMap<>();
        formatMap.put(MetaField.PROCESS_TIME, "AS PROCTIME()");
        formatMap.put(MetaField.TABLE_NAME, "STRING METADATA FROM 'table_name' VIRTUAL");
        formatMap.put(MetaField.DATABASE_NAME, "STRING METADATA FROM 'database_name' VIRTUAL");
        formatMap.put(MetaField.OP_TS, "TIMESTAMP_LTZ(3) METADATA FROM 'op_ts' VIRTUAL");
        formatMap.put(MetaField.SCHEMA_NAME, "STRING METADATA FROM 'schema_name' VIRTUAL");
        PostgresExtractNode node = getTestObject();
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
