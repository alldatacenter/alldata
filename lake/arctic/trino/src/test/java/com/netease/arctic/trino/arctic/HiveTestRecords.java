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

package com.netease.arctic.trino.arctic;

import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableMap;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static com.netease.arctic.trino.arctic.TestHiveTableBaseForTrino.COLUMN_NAME_ARRAY;
import static com.netease.arctic.trino.arctic.TestHiveTableBaseForTrino.COLUMN_NAME_D;
import static com.netease.arctic.trino.arctic.TestHiveTableBaseForTrino.COLUMN_NAME_ID;
import static com.netease.arctic.trino.arctic.TestHiveTableBaseForTrino.COLUMN_NAME_MAP;
import static com.netease.arctic.trino.arctic.TestHiveTableBaseForTrino.COLUMN_NAME_NAME;
import static com.netease.arctic.trino.arctic.TestHiveTableBaseForTrino.COLUMN_NAME_OP_TIME;
import static com.netease.arctic.trino.arctic.TestHiveTableBaseForTrino.COLUMN_NAME_OP_TIME_WITH_ZONE;
import static com.netease.arctic.trino.arctic.TestHiveTableBaseForTrino.COLUMN_NAME_STRUCT;
import static com.netease.arctic.trino.arctic.TestHiveTableBaseForTrino.COLUMN_NAME_STRUCT_SUB1;
import static com.netease.arctic.trino.arctic.TestHiveTableBaseForTrino.COLUMN_NAME_STRUCT_SUB2;
import static com.netease.arctic.trino.arctic.TestHiveTableBaseForTrino.HIVE_TABLE_SCHEMA;
import static com.netease.arctic.trino.arctic.TestHiveTableBaseForTrino.STRUCT_SUB_SCHEMA;

public class HiveTestRecords {

  public static List<Record> baseRecords() {
    GenericRecord record = GenericRecord.create(HIVE_TABLE_SCHEMA);
    GenericRecord structRecord = GenericRecord.create(STRUCT_SUB_SCHEMA);
    ImmutableList.Builder<Record> builder = ImmutableList.builder();

    {
      ImmutableMap columns = ImmutableMap.builder().put(
          COLUMN_NAME_ID, 3
      ).put(
          COLUMN_NAME_OP_TIME, LocalDateTime.of(2022, 1, 3, 12, 0, 0)
      ).put(
          COLUMN_NAME_OP_TIME_WITH_ZONE, OffsetDateTime.of(
                  LocalDateTime.of(2022, 1, 3, 12, 0, 0), ZoneOffset.UTC)
      )
      .put(
          COLUMN_NAME_D, new BigDecimal("102")
      ).put(
          COLUMN_NAME_NAME, "jake"
      ).put(
          COLUMN_NAME_MAP, ImmutableMap.of("map_key", "map_value")
      ).put(
          COLUMN_NAME_ARRAY, ImmutableList.of("array_element")
      ).put(
          COLUMN_NAME_STRUCT, structRecord.copy(COLUMN_NAME_STRUCT_SUB1, "struct_sub1", COLUMN_NAME_STRUCT_SUB2,
              "struct_sub2")
      ).build();
      builder.add(record.copy(columns));
    }

    {
      ImmutableMap columns = ImmutableMap.builder().put(
          COLUMN_NAME_ID, 4
      ).put(
          COLUMN_NAME_OP_TIME, LocalDateTime.of(2022, 1, 4, 12, 0, 0)
      ).put(
          COLUMN_NAME_OP_TIME_WITH_ZONE, OffsetDateTime.of(
              LocalDateTime.of(2022, 1, 4, 12, 0, 0), ZoneOffset.UTC)
      )
      .put(
          COLUMN_NAME_D, new BigDecimal("103")
      ).put(
          COLUMN_NAME_NAME, "sam"
      ).put(
          COLUMN_NAME_MAP, ImmutableMap.of("map_key", "map_value")
      ).put(
          COLUMN_NAME_ARRAY, ImmutableList.of("array_element")
      ).put(
          COLUMN_NAME_STRUCT, structRecord.copy(COLUMN_NAME_STRUCT_SUB1, "struct_sub1", COLUMN_NAME_STRUCT_SUB2,
              "struct_sub2")
      ).build();

      builder.add(record.copy(columns));
    }
    return builder.build();
  }

  public static List<Record> hiveRecords() {
    GenericRecord record = GenericRecord.create(HIVE_TABLE_SCHEMA);
    GenericRecord structRecord = GenericRecord.create(STRUCT_SUB_SCHEMA);
    ImmutableList.Builder<Record> builder = ImmutableList.builder();

    {
      ImmutableMap columns = ImmutableMap.builder().put(
              COLUMN_NAME_ID, 1
          ).put(
              COLUMN_NAME_OP_TIME, LocalDateTime.of(2022, 1, 1, 12, 0, 0)
          ).put(
              COLUMN_NAME_OP_TIME_WITH_ZONE, OffsetDateTime.of(
                  LocalDateTime.of(2022, 1, 1, 12, 0, 0), ZoneOffset.UTC)
          )
          .put(
              COLUMN_NAME_D, new BigDecimal("100")
          ).put(
              COLUMN_NAME_NAME, "john"
          ).put(
              COLUMN_NAME_MAP, ImmutableMap.of("map_key", "map_value")
          ).put(
              COLUMN_NAME_ARRAY, ImmutableList.of("array_element")
          ).put(
              COLUMN_NAME_STRUCT, structRecord.copy(COLUMN_NAME_STRUCT_SUB1, "struct_sub1", COLUMN_NAME_STRUCT_SUB2,
                  "struct_sub2")
          ).build();
      builder.add(record.copy(columns));
    }

    {
      ImmutableMap columns = ImmutableMap.builder().put(
              COLUMN_NAME_ID, 2
          ).put(
              COLUMN_NAME_OP_TIME, LocalDateTime.of(2022, 1, 2, 12, 0, 0)
          ).put(
              COLUMN_NAME_OP_TIME_WITH_ZONE, OffsetDateTime.of(
                  LocalDateTime.of(2022, 1, 2, 12, 0, 0), ZoneOffset.UTC)
          )
          .put(
              COLUMN_NAME_D, new BigDecimal("101")
          ).put(
              COLUMN_NAME_NAME, "lily"
          ).put(
              COLUMN_NAME_MAP, ImmutableMap.of("map_key", "map_value")
          ).put(
              COLUMN_NAME_ARRAY, ImmutableList.of("array_element")
          ).put(
              COLUMN_NAME_STRUCT, structRecord.copy(COLUMN_NAME_STRUCT_SUB1, "struct_sub1", COLUMN_NAME_STRUCT_SUB2,
                  "struct_sub2")
          ).build();
      builder.add(record.copy(columns));
    }

    return builder.build();
  }

  public static List<Record> changeInsertRecords() {
    GenericRecord record = GenericRecord.create(HIVE_TABLE_SCHEMA);
    GenericRecord structRecord = GenericRecord.create(STRUCT_SUB_SCHEMA);
    ImmutableList.Builder<Record> builder = ImmutableList.builder();

    {
      ImmutableMap columns = ImmutableMap.builder().put(
              COLUMN_NAME_ID, 5
          ).put(
              COLUMN_NAME_OP_TIME, LocalDateTime.of(2022, 1, 1, 12, 0, 0)
          ).put(
              COLUMN_NAME_OP_TIME_WITH_ZONE, OffsetDateTime.of(
                  LocalDateTime.of(2022, 1, 1, 12, 0, 0), ZoneOffset.UTC)
          )
          .put(
              COLUMN_NAME_D, new BigDecimal("104")
          ).put(
              COLUMN_NAME_NAME, "mary"
          ).put(
              COLUMN_NAME_MAP, ImmutableMap.of("map_key", "map_value")
          ).put(
              COLUMN_NAME_ARRAY, ImmutableList.of("array_element")
          ).put(
              COLUMN_NAME_STRUCT, structRecord.copy(COLUMN_NAME_STRUCT_SUB1, "struct_sub1", COLUMN_NAME_STRUCT_SUB2,
                  "struct_sub2")
          ).build();
      builder.add(record.copy(columns));
    }

    {
      ImmutableMap columns = ImmutableMap.builder().put(
              COLUMN_NAME_ID, 6
          ).put(
              COLUMN_NAME_OP_TIME, LocalDateTime.of(2022, 1, 1, 12, 0, 0)
          ).put(
              COLUMN_NAME_OP_TIME_WITH_ZONE, OffsetDateTime.of(
                  LocalDateTime.of(2022, 1, 1, 12, 0, 0), ZoneOffset.UTC)
          )
          .put(
              COLUMN_NAME_D, new BigDecimal("105")
          ).put(
              COLUMN_NAME_NAME, "mack"
          ).put(
              COLUMN_NAME_MAP, ImmutableMap.of("map_key", "map_value")
          ).put(
              COLUMN_NAME_ARRAY, ImmutableList.of("array_element")
          ).put(
              COLUMN_NAME_STRUCT, structRecord.copy(COLUMN_NAME_STRUCT_SUB1, "struct_sub1", COLUMN_NAME_STRUCT_SUB2,
                  "struct_sub2")
          ).build();
      builder.add(record.copy(columns));
    }

    return builder.build();
  }

  public static List<Record> changeDeleteRecords() {
    GenericRecord record = GenericRecord.create(HIVE_TABLE_SCHEMA);
    GenericRecord structRecord = GenericRecord.create(STRUCT_SUB_SCHEMA);
    ImmutableList.Builder<Record> builder = ImmutableList.builder();

    {
      ImmutableMap columns = ImmutableMap.builder().put(
              COLUMN_NAME_ID, 5
          ).put(
              COLUMN_NAME_OP_TIME, LocalDateTime.of(2022, 1, 1, 12, 0, 0)
          ).put(
              COLUMN_NAME_OP_TIME_WITH_ZONE, OffsetDateTime.of(
                  LocalDateTime.of(2022, 1, 1, 12, 0, 0), ZoneOffset.UTC)
          )
          .put(
              COLUMN_NAME_D, new BigDecimal("104")
          ).put(
              COLUMN_NAME_NAME, "mary"
          ).put(
              COLUMN_NAME_MAP, ImmutableMap.of("map_key", "map_value")
          ).put(
              COLUMN_NAME_ARRAY, ImmutableList.of("array_element")
          ).put(
              COLUMN_NAME_STRUCT, structRecord.copy(COLUMN_NAME_STRUCT_SUB1, "struct_sub1", COLUMN_NAME_STRUCT_SUB2,
                  "struct_sub2")
          ).build();
      builder.add(record.copy(columns));
    }

    {
      ImmutableMap columns = ImmutableMap.builder().put(
              COLUMN_NAME_ID, 1
          ).put(
              COLUMN_NAME_OP_TIME, LocalDateTime.of(2022, 1, 1, 12, 0, 0)
          ).put(
              COLUMN_NAME_OP_TIME_WITH_ZONE, OffsetDateTime.of(
                  LocalDateTime.of(2022, 1, 1, 12, 0, 0), ZoneOffset.UTC)
          )
          .put(
              COLUMN_NAME_D, new BigDecimal("100")
          ).put(
              COLUMN_NAME_NAME, "john"
          ).put(
              COLUMN_NAME_MAP, ImmutableMap.of("map_key", "map_value")
          ).put(
              COLUMN_NAME_ARRAY, ImmutableList.of("array_element")
          ).put(
              COLUMN_NAME_STRUCT, structRecord.copy(COLUMN_NAME_STRUCT_SUB1, "struct_sub1", COLUMN_NAME_STRUCT_SUB2,
                  "struct_sub2")
          ).build();
      builder.add(record.copy(columns));
    }

    {
      ImmutableMap columns = ImmutableMap.builder().put(
              COLUMN_NAME_ID, 3
          ).put(
              COLUMN_NAME_OP_TIME, LocalDateTime.of(2022, 1, 3, 12, 0, 0)
          ).put(
              COLUMN_NAME_OP_TIME_WITH_ZONE, OffsetDateTime.of(
                  LocalDateTime.of(2022, 1, 3, 12, 0, 0), ZoneOffset.UTC)
          )
          .put(
              COLUMN_NAME_D, new BigDecimal("102")
          ).put(
              COLUMN_NAME_NAME, "jake"
          ).put(
              COLUMN_NAME_MAP, ImmutableMap.of("map_key", "map_value")
          ).put(
              COLUMN_NAME_ARRAY, ImmutableList.of("array_element")
          ).put(
              COLUMN_NAME_STRUCT, structRecord.copy(COLUMN_NAME_STRUCT_SUB1, "struct_sub1", COLUMN_NAME_STRUCT_SUB2,
                  "struct_sub2")
          ).build();
      builder.add(record.copy(columns));
    }

    return builder.build();
  }
}
