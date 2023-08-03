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

package com.netease.arctic.flink.shuffle;

import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.log.Bytes;
import com.netease.arctic.log.FormatTestBase;
import com.netease.arctic.log.FormatVersion;
import com.netease.arctic.log.LogData;
import com.netease.arctic.log.LogDataJsonDeserialization;
import com.netease.arctic.log.LogDataJsonSerialization;
import com.netease.arctic.utils.IdGenerator;
import org.apache.flink.table.data.GenericArrayData;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.iceberg.Schema;
import org.apache.iceberg.types.Types;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * This is a {@link LogRecordV1} log data test, include all data types.
 */
public class TestLogRecordV1 extends FormatTestBase {

  public final Schema userSchema = new Schema(
      new ArrayList<Types.NestedField>() {
        {
          add(Types.NestedField.optional(0, "f_boolean", Types.BooleanType.get()));
          add(Types.NestedField.optional(1, "f_int", Types.IntegerType.get()));
          add(Types.NestedField.optional(2, "f_long", Types.LongType.get()));
          add(Types.NestedField.optional(3, "f_list_string", Types.ListType.ofOptional(
              4, Types.StringType.get()
          )));
        }
      });

  @Test
  public void testLogDataSerialize() throws IOException {

    LogDataJsonSerialization<RowData> logDataJsonSerialization =
        new LogDataJsonSerialization<>(userSchema, LogRecordV1.fieldGetterFactory);
    GenericRowData rowData = new GenericRowData(4);
    rowData.setField(0, true);
    rowData.setField(1, 1);
    rowData.setField(2, 123456789L);
    rowData.setField(3,
        new GenericArrayData(
            new StringData[] {
                null,
                StringData.fromString("b"),
                null,
                StringData.fromString("c"),
                null}));
    LogData<RowData> logData = new LogRecordV1(
        FormatVersion.FORMAT_VERSION_V1,
        IdGenerator.generateUpstreamId(),
        123455L,
        false,
        ChangeAction.INSERT,
        rowData
    );

    byte[] bytes = logDataJsonSerialization.serialize(logData);

    Assert.assertNotNull(bytes);
    String actualJson = new String(Bytes.subByte(bytes, 18, bytes.length - 18));
    String expected =
        "{\"f_boolean\":true,\"f_int\":1,\"f_long\":123456789,\"f_list_string\":[null,\"b\",null,\"c\",null]}";
    assertEquals(expected, actualJson);

    LogDataJsonDeserialization<RowData> logDataJsonDeserialization =
        new LogDataJsonDeserialization<>(
            userSchema,
            LogRecordV1.factory,
            LogRecordV1.arrayFactory,
            LogRecordV1.mapFactory);
    LogData<RowData> result = logDataJsonDeserialization.deserialize(bytes);
    Assert.assertNotNull(result);
    check(logData, result);
  }

  @Test
  public void testLogDataSerializeNullList() throws IOException {

    LogDataJsonSerialization<RowData> logDataJsonSerialization =
        new LogDataJsonSerialization<>(userSchema, LogRecordV1.fieldGetterFactory);
    GenericRowData rowData = new GenericRowData(4);
    rowData.setField(0, true);
    rowData.setField(1, 1);
    rowData.setField(2, 123456789L);
    rowData.setField(3,
        new GenericArrayData(
            new StringData[] {
                null,
                null,
                null}));
    LogData<RowData> logData = new LogRecordV1(
        FormatVersion.FORMAT_VERSION_V1,
        IdGenerator.generateUpstreamId(),
        123455L,
        false,
        ChangeAction.INSERT,
        rowData
    );

    byte[] bytes = logDataJsonSerialization.serialize(logData);

    Assert.assertNotNull(bytes);
    String actualJson = new String(Bytes.subByte(bytes, 18, bytes.length - 18));
    String expected =
        "{\"f_boolean\":true,\"f_int\":1,\"f_long\":123456789,\"f_list_string\":[null,null,null]}";
    assertEquals(expected, actualJson);

    LogDataJsonDeserialization<RowData> logDataJsonDeserialization =
        new LogDataJsonDeserialization<>(
            userSchema,
            LogRecordV1.factory,
            LogRecordV1.arrayFactory,
            LogRecordV1.mapFactory);
    LogData<RowData> result = logDataJsonDeserialization.deserialize(bytes);
    Assert.assertNotNull(result);
    check(logData, result);
  }

  private void check(LogData<RowData> expected, LogData<RowData> actual) {
    assertArrayEquals(expected.getVersionBytes(), actual.getVersionBytes());
    assertArrayEquals(expected.getUpstreamIdBytes(), actual.getUpstreamIdBytes());
    assertEquals(expected.getEpicNo(), actual.getEpicNo());
    assertEquals(expected.getFlip(), actual.getFlip());
    assertEquals(expected.getChangeActionByte(), actual.getChangeActionByte());
    assertEquals(expected.getActualValue().toString(), actual.getActualValue().toString());
  }
}