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

package org.apache.inlong.sort.formats.inlongmsg;

import org.apache.flink.api.common.functions.util.ListCollector;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.catalog.Column;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.connector.format.DecodingFormat;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.TimestampData;
import org.apache.flink.table.data.binary.BinaryStringData;
import org.apache.flink.table.factories.utils.FactoryMocks;
import org.apache.flink.table.runtime.connector.source.ScanRuntimeProviderContext;
import org.apache.inlong.common.msg.InLongMsg;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class InLongMsgRowDataSerDeTest {

    @Test
    public void testDeserializeInLongMsg() throws Exception {
        // mock data
        InLongMsg inLongMsg1 = InLongMsg.newInLongMsg();
        inLongMsg1.addMsg("streamId=HAHA&t=202201011112",
                "1,asdqw".getBytes(StandardCharsets.UTF_8));
        inLongMsg1.addMsg("streamId=xixi&t=202201011112",
                "2,testData".getBytes(StandardCharsets.UTF_8));
        InLongMsg inLongMsg2 = InLongMsg.newInLongMsg();
        inLongMsg1.addMsg("streamId=oooo&t=202201011112",
                "3,dwqdqw".getBytes(StandardCharsets.UTF_8));
        inLongMsg1.addMsg("streamId=bubub&t=202201011112",
                "4,asdqdqwe".getBytes(StandardCharsets.UTF_8));
        List<InLongMsg> inLongMsgs = Stream.of(inLongMsg1).collect(Collectors.toList());
        List<byte[]> input = inLongMsgs.stream()
                .map(inLongMsg -> inLongMsg.buildArray())
                .collect(Collectors.toList());
        final List<RowData> exceptedOutput = Stream.of(
                GenericRowData.of(1L, BinaryStringData.fromString("asdqw")),
                GenericRowData.of(2L, BinaryStringData.fromString("testData")),
                GenericRowData.of(3L, BinaryStringData.fromString("dwqdqw")),
                GenericRowData.of(4L, BinaryStringData.fromString("asdqdqwe"))).collect(Collectors.toList());

        // deserialize
        final Map<String, String> tableOptions =
                InLongMsgFormatFactoryTest.getModifiedOptions(opts -> {
                    opts.put("inlong-msg.inner.format", "csv");
                });
        ResolvedSchema schema = ResolvedSchema.of(
                Column.physical("id", DataTypes.BIGINT()),
                Column.physical("name", DataTypes.STRING()));
        DeserializationSchema<RowData> inLongMsgDeserializationSchema =
                InLongMsgFormatFactoryTest.createDeserializationSchema(tableOptions, schema);
        List<RowData> deData = new ArrayList<>();
        ListCollector<RowData> out = new ListCollector<>(deData);
        for (byte[] bytes : input) {
            inLongMsgDeserializationSchema.deserialize(bytes, out);
        }

        assertEquals(exceptedOutput, deData);
    }

    @Test
    public void testDeserializeInLongMsgWithError() throws Exception {
        // mock data
        InLongMsg inLongMsg1 = InLongMsg.newInLongMsg();
        inLongMsg1.addMsg("asdq",
                "1, asd".getBytes(StandardCharsets.UTF_8));
        List<InLongMsg> inLongMsgs = Stream.of(inLongMsg1).collect(Collectors.toList());
        List<byte[]> input = inLongMsgs.stream()
                .map(inLongMsg -> inLongMsg.buildArray())
                .collect(Collectors.toList());

        // deserialize
        final Map<String, String> tableOptions =
                InLongMsgFormatFactoryTest.getModifiedOptions(opts -> {
                    opts.put("inlong-msg.inner.format", "csv");
                    opts.put("inlong-msg.ignore-parse-errors", "true");
                });
        ResolvedSchema schema = ResolvedSchema.of(
                Column.physical("id", DataTypes.BIGINT()),
                Column.physical("name", DataTypes.STRING()));
        DeserializationSchema<RowData> inLongMsgDeserializationSchema =
                InLongMsgFormatFactoryTest.createDeserializationSchema(tableOptions, schema);
        List<RowData> deData = new ArrayList<>();
        ListCollector<RowData> out = new ListCollector<>(deData);
        for (byte[] bytes : input) {
            inLongMsgDeserializationSchema.deserialize(bytes, out);
        }
        assertEquals(Collections.emptyList(), deData);
    }

    @Test
    public void testDeserializeInLongMsgWithMetadata() throws Exception {
        InLongMsg inLongMsg1 = InLongMsg.newInLongMsg();
        inLongMsg1.addMsg("streamId=HAHA&dt=1652153467000",
                "1,asdqw".getBytes(StandardCharsets.UTF_8));
        inLongMsg1.addMsg("streamId=xixi&dt=1652153467000",
                "2,testData".getBytes(StandardCharsets.UTF_8));
        InLongMsg inLongMsg2 = InLongMsg.newInLongMsg();
        inLongMsg1.addMsg("streamId=oooo&dt=1652153468000",
                "3,dwqdqw".getBytes(StandardCharsets.UTF_8));
        inLongMsg1.addMsg("streamId=bubub&dt=1652153469000",
                "4,asdqdqwe".getBytes(StandardCharsets.UTF_8));
        List<InLongMsg> inLongMsgs = Stream.of(inLongMsg1).collect(Collectors.toList());
        List<byte[]> input = inLongMsgs.stream()
                .map(inLongMsg -> inLongMsg.buildArray())
                .collect(Collectors.toList());
        final List<RowData> exceptedOutput = Stream.of(
                GenericRowData.of(1L, BinaryStringData.fromString("asdqw"),
                        TimestampData.fromTimestamp(new Timestamp(1652153467000L))),
                GenericRowData.of(2L, BinaryStringData.fromString("testData"),
                        TimestampData.fromTimestamp(new Timestamp(1652153467000L))),
                GenericRowData.of(3L, BinaryStringData.fromString("dwqdqw"),
                        TimestampData.fromTimestamp(new Timestamp(1652153468000L))),
                GenericRowData.of(4L, BinaryStringData.fromString("asdqdqwe"),
                        TimestampData.fromTimestamp(new Timestamp(1652153469000L))))
                .collect(Collectors.toList());

        // deserialize
        final Map<String, String> tableOptions = new HashMap<>();
        tableOptions.put("inner.format", "csv");
        ResolvedSchema schema = ResolvedSchema.of(
                Column.physical("id", DataTypes.BIGINT()),
                Column.physical("name", DataTypes.STRING()),
                Column.metadata("time", DataTypes.TIMESTAMP(3), "create-time", false)

        );

        // apply metadata
        InLongMsgFormatFactory factory = new InLongMsgFormatFactory();
        DecodingFormat<DeserializationSchema<RowData>> decodingFormat = factory.createDecodingFormat(FactoryMocks
                .createTableContext(schema, tableOptions), Configuration.fromMap(tableOptions));
        decodingFormat.applyReadableMetadata(Stream.of("create-time").collect(Collectors.toList()));
        DeserializationSchema<RowData> inLongMsgDeserializationSchema = decodingFormat
                .createRuntimeDecoder(ScanRuntimeProviderContext.INSTANCE, schema.toPhysicalRowDataType());

        List<RowData> deData = new ArrayList<>();
        ListCollector<RowData> out = new ListCollector<>(deData);
        for (byte[] bytes : input) {
            inLongMsgDeserializationSchema.deserialize(bytes, out);
        }

        assertEquals(exceptedOutput, deData);
    }
}
