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

package org.apache.inlong.sort.formats.kv;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.function.Consumer;
import org.apache.flink.types.Row;
import org.apache.inlong.sort.formats.common.BasicFormatInfo;
import org.apache.inlong.sort.formats.common.BooleanFormatInfo;
import org.apache.inlong.sort.formats.common.ByteFormatInfo;
import org.apache.inlong.sort.formats.common.DateFormatInfo;
import org.apache.inlong.sort.formats.common.DecimalFormatInfo;
import org.apache.inlong.sort.formats.common.DoubleFormatInfo;
import org.apache.inlong.sort.formats.common.FloatFormatInfo;
import org.apache.inlong.sort.formats.common.FormatInfo;
import org.apache.inlong.sort.formats.common.IntFormatInfo;
import org.apache.inlong.sort.formats.common.LongFormatInfo;
import org.apache.inlong.sort.formats.common.RowFormatInfo;
import org.apache.inlong.sort.formats.common.ShortFormatInfo;
import org.apache.inlong.sort.formats.common.StringFormatInfo;
import org.apache.inlong.sort.formats.common.TimeFormatInfo;
import org.apache.inlong.sort.formats.common.TimestampFormatInfo;
import org.junit.Test;

/**
 * Tests for {@link KvDeserializationSchema}.
 */
public class KvDeserializationSchemaTest {

    private static final RowFormatInfo TEST_ROW_INFO =
            new RowFormatInfo(
                    new String[]{"f1", "f2", "f3", "f4"},
                    new FormatInfo[]{
                            IntFormatInfo.INSTANCE,
                            StringFormatInfo.INSTANCE,
                            StringFormatInfo.INSTANCE,
                            StringFormatInfo.INSTANCE
                    }
            );

    @Test
    public void testNormal() throws Exception {
        Consumer<KvDeserializationSchema.Builder> config = builder -> {
        };

        testBasicDeserialization(config, StringFormatInfo.INSTANCE, "hello", "f=hello");
        testBasicDeserialization(config, BooleanFormatInfo.INSTANCE, true, "f=true");
        testBasicDeserialization(config, ByteFormatInfo.INSTANCE, (byte) 124, "f=124");
        testBasicDeserialization(config, ShortFormatInfo.INSTANCE, (short) 10000, "f=10000");
        testBasicDeserialization(config, IntFormatInfo.INSTANCE, 1234567, "f=1234567");
        testBasicDeserialization(config, LongFormatInfo.INSTANCE, 12345678910L, "f=12345678910");
        testBasicDeserialization(config, FloatFormatInfo.INSTANCE, 0.33333334f, "f=0.33333334");
        testBasicDeserialization(config, DoubleFormatInfo.INSTANCE, 0.33333333332, "f=0.33333333332");
        testBasicDeserialization(config, DecimalFormatInfo.INSTANCE, new BigDecimal("1234.0000000000000000000000001"),
                "f=1234.0000000000000000000000001");
        testBasicDeserialization(config, new DateFormatInfo("dd/MM/yyyy"), Date.valueOf("2020-03-22"), "f=22/03/2020");
        testBasicDeserialization(config, new TimeFormatInfo("ss/mm/hh"), Time.valueOf("11:12:13"), "f=13/12/11");
        testBasicDeserialization(config, new TimestampFormatInfo("dd/MM/yyyy hh:mm:ss"),
                Timestamp.valueOf("2020-03-22 11:12:13"), "f=22/03/2020 11:12:13");
    }

    @Test
    public void testNullIteral() throws Exception {
        String nullLiteral = "n/a";
        String nullField = "f=n/a";
        Consumer<KvDeserializationSchema.Builder> config = builder ->
                                                                   builder.setNullLiteral(nullLiteral);

        testBasicDeserialization(config, StringFormatInfo.INSTANCE, null, nullField);
        testBasicDeserialization(config, BooleanFormatInfo.INSTANCE, null, nullField);
        testBasicDeserialization(config, ByteFormatInfo.INSTANCE, null, nullField);
        testBasicDeserialization(config, ShortFormatInfo.INSTANCE, null, nullField);
        testBasicDeserialization(config, IntFormatInfo.INSTANCE, null, nullField);
        testBasicDeserialization(config, LongFormatInfo.INSTANCE, null, nullField);
        testBasicDeserialization(config, FloatFormatInfo.INSTANCE, null, nullField);
        testBasicDeserialization(config, DoubleFormatInfo.INSTANCE, null, nullField);
        testBasicDeserialization(config, DecimalFormatInfo.INSTANCE, null, nullField);
        testBasicDeserialization(config, new DateFormatInfo("dd/MM/yyyy"), null, nullField);
        testBasicDeserialization(config, new TimeFormatInfo("ss/mm/hh"), null, nullField);
        testBasicDeserialization(config, new TimestampFormatInfo("dd/MM/yyyy hh:mm:ss"), null, nullField);
    }

    @Test
    public void testDelimiter() throws Exception {
        Consumer<KvDeserializationSchema.Builder> config = builder ->
                                                                   builder.setEntryDelimiter('|').setKvDelimiter(',');

        testRowDeserialization(
                config,
                Row.of(10, "aa", "bb", "cc"),
                "f1,10|f2,aa|f3,bb|f4,cc".getBytes()
        );
    }

    @Test
    public void testEscape() throws Exception {
        Consumer<KvDeserializationSchema.Builder> config = builder ->
                                                                   builder.setEscapeCharacter('\\')
                                                                           .setQuoteCharacter('\"');

        testRowDeserialization(
                config,
                Row.of(10, "field1&field2", "field3", "field4"),
                "f1=10&f2=field1\\&field2&f3=field3&f4=field4".getBytes()
        );
        testRowDeserialization(
                config,
                Row.of(10, "field1\\", "field2", "field3"),
                "f1=10&f2=field1\\\\&f3=field2&f4=field3".getBytes()
        );
        testRowDeserialization(
                config,
                Row.of(10, "field1\"", "field2", "field3"),
                "f1=10&f2=field1\\\"&f3=field2&f4=field3".getBytes()
        );
    }

    @Test
    public void testQuote() throws Exception {
        Consumer<KvDeserializationSchema.Builder> config = builder ->
                                                                   builder.setEscapeCharacter('\\')
                                                                           .setQuoteCharacter('\"');

        testRowDeserialization(
                config,
                Row.of(10, "field1&field2", "field3", "field4"),
                "f1=10&f2=\"field1&field2\"&f3=field3&f4=field4".getBytes()
        );
        testRowDeserialization(
                config,
                Row.of(10, "field1\\", "field2", "field3"),
                "f1=10&f2=\"field1\\\"&f3=field2&f4=field3".getBytes()
        );
    }

    @Test
    public void testExtractSpecificKeys() throws Exception {
        Consumer<KvDeserializationSchema.Builder> config = builder ->
                                                                   builder.setEscapeCharacter('\\')
                                                                           .setQuoteCharacter('\"');

        testRowDeserialization(
                config,
                Row.of(10, "field1", "field2", "field3"),
                "f1=10&f2=field1&f3=field2&f4=field3&f5=field4".getBytes()
        );
    }

    @Test
    public void testCharset() throws Exception {
        Consumer<KvDeserializationSchema.Builder> config = builder ->
                                                                   builder.setCharset(StandardCharsets.UTF_16.name());

        testRowDeserialization(
                config,
                Row.of(10, "aa", "bb", "cc"),
                "f1=10&f2=aa&f3=bb&f4=cc".getBytes(StandardCharsets.UTF_16)
        );
    }

    @Test(expected = Exception.class)
    public void testErrors() throws Exception {
        Consumer<KvDeserializationSchema.Builder> config = builder -> {
        };
        testRowDeserialization(
                config,
                Row.of(null, "field1", "field2", "field3"),
                "f1=na&f2=field1&f3=field2&f4=field3".getBytes()
        );
    }

    @Test
    public void testMissingField() throws Exception {
        Consumer<KvDeserializationSchema.Builder> config = builder -> {
        };

        testRowDeserialization(
                config,
                Row.of(null, "aa", "bb", "cc"),
                "f2=aa&f3=bb&f4=cc".getBytes()
        );
    }

    @Test
    public void testExtraField() throws Exception {
        Consumer<KvDeserializationSchema.Builder> config = builder -> {
        };

        testRowDeserialization(
                config,
                Row.of(10, "aa", "bb", "cc"),
                "f1=10&f2=aa&f3=bb&f4=cc&f5=dd".getBytes()
        );
    }

    private static <T> void testBasicDeserialization(
            Consumer<KvDeserializationSchema.Builder> config,
            BasicFormatInfo<T> basicFormatInfo,
            T expectedRecord,
            String text
    ) throws IOException {
        RowFormatInfo rowFormatInfo =
                new RowFormatInfo(
                        new String[]{"f"},
                        new FormatInfo[]{basicFormatInfo}
                );

        KvDeserializationSchema.Builder builder =
                new KvDeserializationSchema.Builder(rowFormatInfo);
        config.accept(builder);

        KvDeserializationSchema deserializer = builder.build();

        Row row = deserializer.deserialize(text.getBytes());
        assertEquals(1, row.getArity());
        assertEquals(expectedRecord, row.getField(0));
    }

    private void testRowDeserialization(
            Consumer<KvDeserializationSchema.Builder> config,
            Row expectedRow,
            byte[] bytes
    ) throws Exception {
        KvDeserializationSchema.Builder builder =
                new KvDeserializationSchema.Builder(TEST_ROW_INFO);
        config.accept(builder);

        KvDeserializationSchema deserializer = builder.build();

        Row row = deserializer.deserialize(bytes);
        assertEquals(expectedRow, row);
    }
}
