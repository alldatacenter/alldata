/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.formats.inlongmsgcsv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.Types;
import org.apache.flink.table.descriptors.Schema;
import org.apache.flink.types.Row;
import org.apache.inlong.sort.formats.base.TableFormatDeserializer;
import org.apache.inlong.sort.formats.base.TableFormatUtils;
import org.apache.inlong.sort.formats.common.DateFormatInfo;
import org.apache.inlong.sort.formats.common.FormatInfo;
import org.apache.inlong.sort.formats.common.FormatUtils;
import org.apache.inlong.sort.formats.common.IntFormatInfo;
import org.apache.inlong.sort.formats.common.RowFormatInfo;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.formats.inlongmsg.InLongMsgUtils;
import org.junit.Test;

/**
 * Tests for {@link InLongMsgCsvFormatFactory}.
 */
public class InLongMsgCsvFormatFactoryTest {

    private static final TypeInformation<Row> SCHEMA =
            Types.ROW(
                    new String[]{"time", "attributes", "student_name", "score", "date"},
                    new TypeInformation[]{
                            Types.SQL_TIMESTAMP(),
                            Types.MAP(Types.STRING(), Types.STRING()),
                            Types.STRING(),
                            Types.INT(),
                            Types.SQL_DATE()
                    }
            );

    private static final RowFormatInfo TEST_FORMAT_SCHEMA =
            new RowFormatInfo(
                    new String[]{"student_name", "score", "date"},
                    new FormatInfo[]{
                            StringFormatInfo.INSTANCE,
                            IntFormatInfo.INSTANCE,
                            new DateFormatInfo("yyyy-MM-dd")
                    }
            );

    @Test
    public void testCreateTableFormatDeserializer() throws Exception {
        final Map<String, String> properties =
                new InLongMsgCsv()
                        .schema(FormatUtils.marshall(TEST_FORMAT_SCHEMA))
                        .delimiter(';')
                        .charset(StandardCharsets.ISO_8859_1)
                        .escapeCharacter('\\')
                        .quoteCharacter('\"')
                        .nullLiteral("null")
                        .toProperties();
        assertNotNull(properties);

        final InLongMsgCsvFormatDeserializer expectedDeser =
                new InLongMsgCsvFormatDeserializer(
                        TEST_FORMAT_SCHEMA,
                        InLongMsgUtils.DEFAULT_TIME_FIELD_NAME,
                        InLongMsgUtils.DEFAULT_ATTRIBUTES_FIELD_NAME,
                        StandardCharsets.ISO_8859_1.name(),
                        ';',
                        '\\',
                        '\"',
                        "null",
                        true,
                        false
                );

        final TableFormatDeserializer actualDeser =
                TableFormatUtils.getTableFormatDeserializer(
                        properties,
                        getClass().getClassLoader()
                );

        assertEquals(expectedDeser, actualDeser);
    }

    @Test
    public void testCreateTableFormatDeserializerWithDerivation() {
        final Map<String, String> properties = new HashMap<>();
        properties.putAll(
                new Schema()
                        .schema(TableSchema.fromTypeInfo(SCHEMA))
                        .toProperties()
        );
        properties.putAll(new InLongMsgCsv().deriveSchema().toProperties());

        final InLongMsgCsvFormatDeserializer expectedDeser =
                new InLongMsgCsvFormatDeserializer(TEST_FORMAT_SCHEMA);

        final TableFormatDeserializer actualDeser =
                TableFormatUtils.getTableFormatDeserializer(
                        properties,
                        getClass().getClassLoader()
                );

        assertEquals(expectedDeser, actualDeser);
    }
}
