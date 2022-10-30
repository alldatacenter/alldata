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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

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
 * Tests for {@link KvSerializationSchema}.
 */
public class KvSerializationSchemaTest {

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
    public void testNormal() {
        Consumer<KvSerializationSchema.Builder> config = builder -> {
        };

        testBasicSerialization(config, StringFormatInfo.INSTANCE, "hello", "f=hello");
        testBasicSerialization(config, BooleanFormatInfo.INSTANCE, true, "f=true");
        testBasicSerialization(config, ByteFormatInfo.INSTANCE, (byte) 124, "f=124");
        testBasicSerialization(config, ShortFormatInfo.INSTANCE, (short) 10000, "f=10000");
        testBasicSerialization(config, IntFormatInfo.INSTANCE, 1234567, "f=1234567");
        testBasicSerialization(config, LongFormatInfo.INSTANCE, 12345678910L, "f=12345678910");
        testBasicSerialization(config, FloatFormatInfo.INSTANCE, 0.33333334f, "f=0.33333334");
        testBasicSerialization(config, DoubleFormatInfo.INSTANCE, 0.33333333332, "f=0.33333333332");
        testBasicSerialization(config, DecimalFormatInfo.INSTANCE, new BigDecimal("1234.0000000000000000000000001"),
                "f=1234.0000000000000000000000001");
        testBasicSerialization(config, new DateFormatInfo("dd/MM/yyyy"), Date.valueOf("2020-03-22"), "f=22/03/2020");
        testBasicSerialization(config, new TimeFormatInfo("ss/mm/hh"), Time.valueOf("11:12:13"), "f=13/12/11");
        testBasicSerialization(config, new TimestampFormatInfo("dd/MM/yyyy hh:mm:ss"),
                Timestamp.valueOf("2020-03-22 11:12:13"), "f=22/03/2020 11:12:13");
    }

    @Test
    public void testNullIteral() {
        String nullLiteral = "n/a";
        String nullField = "f=n/a";
        Consumer<KvSerializationSchema.Builder> config = builder ->
                                                                 builder.setNullLiteral(nullLiteral);

        testBasicSerialization(config, StringFormatInfo.INSTANCE, null, nullField);
        testBasicSerialization(config, BooleanFormatInfo.INSTANCE, null, nullField);
        testBasicSerialization(config, ByteFormatInfo.INSTANCE, null, nullField);
        testBasicSerialization(config, ShortFormatInfo.INSTANCE, null, nullField);
        testBasicSerialization(config, IntFormatInfo.INSTANCE, null, nullField);
        testBasicSerialization(config, LongFormatInfo.INSTANCE, null, nullField);
        testBasicSerialization(config, FloatFormatInfo.INSTANCE, null, nullField);
        testBasicSerialization(config, DoubleFormatInfo.INSTANCE, null, nullField);
        testBasicSerialization(config, DecimalFormatInfo.INSTANCE, null, nullField);
        testBasicSerialization(config, new DateFormatInfo("dd/MM/yyyy"), null, nullField);
        testBasicSerialization(config, new TimeFormatInfo("ss/mm/hh"), null, nullField);
        testBasicSerialization(config, new TimestampFormatInfo("dd/MM/yyyy hh:mm:ss"), null, nullField);
    }

    @Test
    public void testDelimiter() {
        Consumer<KvSerializationSchema.Builder> config = builder ->
                                                                 builder.setEntryDelimiter('|').setKvDelimiter(',');

        testRowSerialization(
                config,
                Row.of(10, "field1", "field2", "field3"),
                "f1,10|f2,field1|f3,field2|f4,field3".getBytes()
        );
    }

    @Test
    public void testEscape() {
        Consumer<KvSerializationSchema.Builder> config = builder ->
                                                                 builder.setEscapeCharacter('\\')
                                                                         .setQuoteCharacter('\"');

        testRowSerialization(
                config,
                Row.of(10, "field1&field2", "field3", "field4"),
                "f1=10&f2=field1\\&field2&f3=field3&f4=field4".getBytes()
        );
        testRowSerialization(
                config,
                Row.of(10, "field1\\", "field2", "field3"),
                "f1=10&f2=field1\\\\&f3=field2&f4=field3".getBytes()
        );
        testRowSerialization(
                config,
                Row.of(10, "field1\"", "field2", "field3"),
                "f1=10&f2=field1\\\"&f3=field2&f4=field3".getBytes()
        );
    }

    @Test
    public void testQuote() {
        Consumer<KvSerializationSchema.Builder> config = builder ->
                                                                 builder.setQuoteCharacter('\"');

        testRowSerialization(
                config,
                Row.of(10, "field1&field2", "field3", "field4"),
                "f1=10&f2=field1\"&\"field2&f3=field3&f4=field4".getBytes()
        );
    }

    @Test
    public void testCharset() {
        Consumer<KvSerializationSchema.Builder> config = builder ->
                                                                 builder.setCharset(StandardCharsets.UTF_16.name());

        testRowSerialization(
                config,
                Row.of(10, "field1", "field2", "field3"),
                "f1=10&f2=field1&f3=field2&f4=field3".getBytes(StandardCharsets.UTF_16)
        );
    }

    @Test(expected = RuntimeException.class)
    public void testErrors() {
        Consumer<KvSerializationSchema.Builder> config = builder -> {
        };
        testRowSerialization(
                config,
                Row.of("na", "field1", "field2", "field3"),
                "f1=&f2=field1&f3=field2&f4=field3".getBytes()
        );
    }

    private static <T> void testBasicSerialization(
            Consumer<KvSerializationSchema.Builder> config,
            BasicFormatInfo<T> basicFormatInfo,
            T record,
            String expectedText
    ) {
        RowFormatInfo rowFormatInfo =
                new RowFormatInfo(
                        new String[]{"f"},
                        new FormatInfo[]{basicFormatInfo}
                );

        KvSerializationSchema.Builder builder =
                new KvSerializationSchema.Builder(rowFormatInfo);
        config.accept(builder);

        KvSerializationSchema serializer = builder.build();

        String text = new String(serializer.serialize(Row.of(record)));
        assertEquals(expectedText, text);
    }

    private static void testRowSerialization(
            Consumer<KvSerializationSchema.Builder> config,
            Row row,
            byte[] expectedBytes
    ) {
        KvSerializationSchema.Builder builder =
                new KvSerializationSchema.Builder(TEST_ROW_INFO);
        config.accept(builder);

        KvSerializationSchema serializer = builder.build();

        byte[] bytes = serializer.serialize(row);
        assertArrayEquals(expectedBytes, bytes);
    }
}
