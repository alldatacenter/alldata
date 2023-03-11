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

package com.netease.arctic.log;

import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.utils.IdGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class LogDataJsonSerializationTest extends BaseFormatTest {

  @Test
  public void testLogDataSerialize() throws IOException {

    LogDataJsonSerialization<UserPojo> logDataJsonSerialization =
        new LogDataJsonSerialization<>(userSchema, fieldGetterFactory);
    UserPojo subUserPojo = new UserPojo();
    subUserPojo.objects = new Object[] {false, 2, 987654321L};
    UserPojo userPojo = new UserPojo();
    userPojo.objects = new Object[] {
        // boolean
        true,
        // int
        1,
        // long
        123456789L,
        // struct
        subUserPojo,
        // float double
        123.45f, 123.456789d,
        // date int
        (int) LocalDate.of(2022, 11, 11).toEpochDay(),
        // time nanosecond long
        LocalTime.of(13, 23, 23, 98766545).toNanoOfDay(),
        // timestamp local time
        LocalDateTime.of(2022, 12, 12, 13, 14, 14, 987654234),
        // timestamp with time zone
        Instant.parse("2022-12-13T13:33:44.98765432Z"),
        "ssss_string",
        // uuid
        new byte[] {1},
        // fixed
        new byte[] {'1'},
        // binary
        new byte[] {2},
        BigDecimal.valueOf(111.111),
        new GenericArrayData(new Long[] {123L, 234L, null, 345L}, 4, false),
        new GenericArrayData(new int[] {123, 234, 0, 345}, 4, true),
        new GenericArrayData(new UserPojo[] {subUserPojo}, 1, false),
        new GenericMapData(new HashMap<Long, String>() {
          {
            put(1123L, "Str_123");
            put(1124L, "Str_123");
            put(1125L, "Str_123");
          }
        })
    };
    LogData<UserPojo> logData = new LogDataUser(
        FormatVersion.FORMAT_VERSION_V1.asBytes(),
        IdGenerator.generateUpstreamId(),
        123455L,
        false,
        ChangeAction.INSERT,
        userPojo
    );

    byte[] bytes = logDataJsonSerialization.serialize(logData);

    Assert.assertNotNull(bytes);
    String actualJson = new String(Bytes.subByte(bytes, 18, bytes.length - 18));
    String expected =
        "{\"f_boolean\":true,\"f_int\":1,\"f_long\":123456789,\"f_struct\":{\"f_sub_boolean\":false,\"f_sub_int\":2," +
            "\"f_sub_long\":987654321},\"f_float\":123.45,\"f_double\":123.456789,\"f_date\":\"2022-11-11\"," +
            "\"f_time\":\"13:23:23.098766545\",\"f_timestamp_local\":\"2022-12-12 13:14:14.987654234\"," +
            "\"f_timestamp_tz\":\"2022-12-13T13:33:44.98765432Z\",\"f_string\":\"ssss_string\"," +
            "\"f_uuid\":\"AQ==\",\"f_fixed\":\"MQ==\",\"f_binary\":\"Ag==\",\"f_decimal\":111.111," +
            "\"f_list\":[123,234,null,345],\"f_list2\":[123,234,0,345],\"f_list3\":[{\"f_sub_boolean\":false," +
            "\"f_sub_int\":2,\"f_sub_long\":987654321}],\"f_map\":{\"1123\":\"Str_123\",\"1124\":\"Str_123\"," +
            "\"1125\":\"Str_123\"}}";
    assertEquals(expected, actualJson);

    LogDataJsonDeserialization<UserPojo> logDataJsonDeserialization =
        new LogDataJsonDeserialization<>(userSchema, factory, arrayFactory, mapFactory);
    LogData<UserPojo> result = logDataJsonDeserialization.deserialize(bytes);
    Assert.assertNotNull(result);
    check(logData, result);
  }

  private void check(LogData<UserPojo> expected, LogData<UserPojo> actual) {
    assertArrayEquals(expected.getVersionBytes(), actual.getVersionBytes());
    assertArrayEquals(expected.getUpstreamIdBytes(), actual.getUpstreamIdBytes());
    assertEquals(expected.getEpicNo(), actual.getEpicNo());
    assertEquals(expected.getFlip(), actual.getFlip());
    assertEquals(expected.getChangeActionByte(), actual.getChangeActionByte());
    assertEquals(expected.getActualValue().toString(), actual.getActualValue().toString());
  }
}