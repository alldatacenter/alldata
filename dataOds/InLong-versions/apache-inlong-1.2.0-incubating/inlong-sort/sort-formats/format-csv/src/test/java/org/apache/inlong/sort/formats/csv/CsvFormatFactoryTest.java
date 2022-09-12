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

package org.apache.inlong.sort.formats.csv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.Types;
import org.apache.flink.table.descriptors.Schema;
import org.apache.flink.types.Row;
import org.apache.inlong.sort.formats.base.DefaultTableFormatDeserializer;
import org.apache.inlong.sort.formats.base.DefaultTableFormatSerializer;
import org.apache.inlong.sort.formats.base.TableFormatDeserializer;
import org.apache.inlong.sort.formats.base.TableFormatSerializer;
import org.apache.inlong.sort.formats.base.TableFormatUtils;
import org.apache.inlong.sort.formats.common.DateFormatInfo;
import org.apache.inlong.sort.formats.common.FormatInfo;
import org.apache.inlong.sort.formats.common.FormatUtils;
import org.apache.inlong.sort.formats.common.IntFormatInfo;
import org.apache.inlong.sort.formats.common.LongFormatInfo;
import org.apache.inlong.sort.formats.common.RowFormatInfo;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.junit.Test;

/**
 * Tests for {@link CsvFormatFactory}.
 */
public class CsvFormatFactoryTest {

    private static final TypeInformation<Row> SCHEMA =
            Types.ROW(
                    new String[]{"student_name", "score", "date"},
                    new TypeInformation[]{Types.STRING(), Types.INT(), Types.SQL_DATE()}
            );

    private static final TypeInformation<Row> KEYED_SCHEMA =
            Types.ROW(
                    new String[]{"key", "student_name", "score", "date"},
                    new TypeInformation[]{Types.LONG(), Types.STRING(), Types.INT(), Types.SQL_DATE()}
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

    private static final RowFormatInfo TEST_KEYED_FORMAT_SCHEMA =
            new RowFormatInfo(
                    new String[]{"keyed", "student_name", "score", "date"},
                    new FormatInfo[]{
                            LongFormatInfo.INSTANCE,
                            StringFormatInfo.INSTANCE,
                            IntFormatInfo.INSTANCE,
                            new DateFormatInfo("yyyy-MM-dd")
                    }
            );

    @Test
    public void testCreateTableFormatDeserializer() throws Exception {
        final Map<String, String> properties =
                new Csv()
                        .schema(FormatUtils.marshall(TEST_FORMAT_SCHEMA))
                        .delimiter(';')
                        .charset(StandardCharsets.ISO_8859_1)
                        .escapeCharacter('\\')
                        .quoteCharacter('\"')
                        .nullLiteral("null")
                        .toProperties();
        assertNotNull(properties);

        final CsvDeserializationSchema deserializationSchema =
                new CsvDeserializationSchema(
                        TEST_FORMAT_SCHEMA,
                        StandardCharsets.ISO_8859_1.name(),
                        ';',
                        '\\',
                        '\"',
                        "null");

        final DefaultTableFormatDeserializer expectedDeser =
                new DefaultTableFormatDeserializer(deserializationSchema);

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
        properties.putAll(new Csv().deriveSchema().toProperties());

        final CsvDeserializationSchema deserializationSchema =
                new CsvDeserializationSchema(TEST_FORMAT_SCHEMA);

        final DefaultTableFormatDeserializer expectedDeser =
                new DefaultTableFormatDeserializer(deserializationSchema);

        final TableFormatDeserializer actualDeser =
                TableFormatUtils.getTableFormatDeserializer(
                        properties,
                        getClass().getClassLoader()
                );

        assertEquals(expectedDeser, actualDeser);
    }

    @Test
    public void testCreateTableFormatSerializer() throws Exception {
        final Map<String, String> properties =
                new Csv()
                        .schema(FormatUtils.marshall(TEST_FORMAT_SCHEMA))
                        .delimiter(';')
                        .charset(StandardCharsets.ISO_8859_1)
                        .escapeCharacter('\\')
                        .quoteCharacter('\"')
                        .nullLiteral("null")
                        .toProperties();
        assertNotNull(properties);

        final CsvSerializationSchema serializationSchema =
                new CsvSerializationSchema(
                        TEST_FORMAT_SCHEMA,
                        StandardCharsets.ISO_8859_1.name(),
                        ';',
                        '\\',
                        '\"',
                        "null");

        final DefaultTableFormatSerializer expectedSer =
                new DefaultTableFormatSerializer(serializationSchema);

        final TableFormatSerializer actualSer =
                TableFormatUtils.getTableFormatSerializer(
                        properties,
                        getClass().getClassLoader()
                );

        assertEquals(expectedSer, actualSer);
    }

    @Test
    public void testCreateTableFormatSerializerWithDerivation() {
        final Map<String, String> properties = new HashMap<>();
        properties.putAll(
                new Schema()
                        .schema(TableSchema.fromTypeInfo(SCHEMA))
                        .toProperties()
        );
        properties.putAll(new Csv().deriveSchema().toProperties());

        final CsvSerializationSchema serializationSchema =
                new CsvSerializationSchema(TEST_FORMAT_SCHEMA);

        final DefaultTableFormatSerializer expectedSer =
                new DefaultTableFormatSerializer(serializationSchema);

        final TableFormatSerializer actualSer =
                TableFormatUtils.getTableFormatSerializer(
                        properties,
                        getClass().getClassLoader()
                );

        assertEquals(expectedSer, actualSer);
    }

    @Test
    public void testCreateProjectedDeserializationSchema() throws IOException {
        final Map<String, String> properties =
                new Csv()
                        .schema(FormatUtils.marshall(TEST_KEYED_FORMAT_SCHEMA))
                        .delimiter(';')
                        .charset(StandardCharsets.ISO_8859_1)
                        .escapeCharacter('\\')
                        .quoteCharacter('\"')
                        .nullLiteral("null")
                        .toProperties();
        assertNotNull(properties);

        final CsvDeserializationSchema expectdDeser =
                new CsvDeserializationSchema(
                        TEST_FORMAT_SCHEMA,
                        StandardCharsets.ISO_8859_1.name(),
                        ';',
                        '\\',
                        '\"',
                        "null");

        final DeserializationSchema<Row> actualDeser =
                TableFormatUtils.getProjectedDeserializationSchema(
                        properties,
                        new int[]{1, 2, 3},
                        getClass().getClassLoader()
                );

        assertEquals(expectdDeser, actualDeser);
    }

    @Test
    public void testCreateProjectedDeserializationSchemaWithDerivation() {
        final Map<String, String> properties = new HashMap<>();
        properties.putAll(
                new Schema()
                        .schema(TableSchema.fromTypeInfo(KEYED_SCHEMA))
                        .toProperties()
        );
        properties.putAll(new Csv().deriveSchema().toProperties());

        final CsvDeserializationSchema expectedDeser =
                new CsvDeserializationSchema(TEST_FORMAT_SCHEMA);

        final DeserializationSchema<Row> actualDeser =
                TableFormatUtils.getProjectedDeserializationSchema(
                        properties,
                        new int[]{1, 2, 3},
                        getClass().getClassLoader()
                );

        assertEquals(expectedDeser, actualDeser);
    }

    @Test
    public void testCreateProjectedSerializationSchema() throws IOException {
        final Map<String, String> properties =
                new Csv()
                        .schema(FormatUtils.marshall(TEST_KEYED_FORMAT_SCHEMA))
                        .delimiter(';')
                        .charset(StandardCharsets.ISO_8859_1)
                        .escapeCharacter('\\')
                        .quoteCharacter('\"')
                        .nullLiteral("null")
                        .toProperties();
        assertNotNull(properties);

        final CsvSerializationSchema expectdSer =
                new CsvSerializationSchema(
                        TEST_FORMAT_SCHEMA,
                        StandardCharsets.ISO_8859_1.name(),
                        ';',
                        '\\',
                        '\"',
                        "null");

        final SerializationSchema<Row> actualSer =
                TableFormatUtils.getProjectedSerializationSchema(
                        properties,
                        new int[]{1, 2, 3},
                        getClass().getClassLoader()
                );

        assertEquals(expectdSer, actualSer);
    }

    @Test
    public void testCreateProjectedSerializationSchemaWithDerivation() {
        final Map<String, String> properties = new HashMap<>();
        properties.putAll(
                new Schema()
                        .schema(TableSchema.fromTypeInfo(KEYED_SCHEMA))
                        .toProperties()
        );
        properties.putAll(new Csv().deriveSchema().toProperties());

        final CsvSerializationSchema expectedSer =
                new CsvSerializationSchema(TEST_FORMAT_SCHEMA);

        final SerializationSchema<Row> actualSer =
                TableFormatUtils.getProjectedSerializationSchema(
                        properties,
                        new int[]{1, 2, 3},
                        getClass().getClassLoader()
                );

        assertEquals(expectedSer, actualSer);
    }
}
