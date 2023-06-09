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

package com.netease.arctic.hive.io;

import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableMap;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static com.netease.arctic.hive.HiveTableTestBase.COLUMN_NAME_D;
import static com.netease.arctic.hive.HiveTableTestBase.COLUMN_NAME_ID;
import static com.netease.arctic.hive.HiveTableTestBase.COLUMN_NAME_NAME;
import static com.netease.arctic.hive.HiveTableTestBase.COLUMN_NAME_OP_TIME;
import static com.netease.arctic.hive.HiveTableTestBase.COLUMN_NAME_OP_TIME_WITH_ZONE;
import static com.netease.arctic.hive.HiveTableTestBase.HIVE_TABLE_SCHEMA;

public class HiveTestRecords {

  public static List<Record> baseRecords() {
    GenericRecord record = GenericRecord.create(HIVE_TABLE_SCHEMA);

    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    builder.add(record.copy(ImmutableMap.of(COLUMN_NAME_ID, 3,
        COLUMN_NAME_OP_TIME, LocalDateTime.of(2022, 1, 3, 12, 0, 0),
        COLUMN_NAME_OP_TIME_WITH_ZONE, OffsetDateTime.of(
            LocalDateTime.of(2022, 1, 3, 12, 0, 0), ZoneOffset.UTC),
        COLUMN_NAME_D, new BigDecimal("102"), COLUMN_NAME_NAME, "jake")));
    builder.add(record.copy(ImmutableMap.of(COLUMN_NAME_ID, 4,
        COLUMN_NAME_OP_TIME, LocalDateTime.of(2022, 1, 4, 12, 0, 0),
        COLUMN_NAME_OP_TIME_WITH_ZONE, OffsetDateTime.of(
            LocalDateTime.of(2022, 1, 4, 12, 0, 0), ZoneOffset.UTC),
        COLUMN_NAME_D, new BigDecimal("103"), COLUMN_NAME_NAME, "sam")));

    return builder.build();
  }

  public static List<Record> hiveRecords() {
    GenericRecord record = GenericRecord.create(HIVE_TABLE_SCHEMA);

    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    builder.add(record.copy(ImmutableMap.of(COLUMN_NAME_ID, 1,
        COLUMN_NAME_OP_TIME, LocalDateTime.of(2022, 1, 1, 12, 0, 0),
        COLUMN_NAME_OP_TIME_WITH_ZONE, OffsetDateTime.of(
            LocalDateTime.of(2022, 1, 1, 12, 0, 0), ZoneOffset.UTC),
        COLUMN_NAME_D, new BigDecimal("100"), COLUMN_NAME_NAME, "john")));
    builder.add(record.copy(ImmutableMap.of(COLUMN_NAME_ID, 2,
        COLUMN_NAME_OP_TIME, LocalDateTime.of(2022, 1, 2, 12, 0, 0),
        COLUMN_NAME_OP_TIME_WITH_ZONE, OffsetDateTime.of(
            LocalDateTime.of(2022, 1, 2, 12, 0, 0), ZoneOffset.UTC),
        COLUMN_NAME_D, new BigDecimal("101"), COLUMN_NAME_NAME, "lily")));
    return builder.build();
  }

  public static List<Record> changeInsertRecords() {
    GenericRecord record = GenericRecord.create(HIVE_TABLE_SCHEMA);

    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    builder.add(record.copy(ImmutableMap.of(COLUMN_NAME_ID, 5,
        COLUMN_NAME_OP_TIME, LocalDateTime.of(2022, 1, 1, 12, 0, 0),
        COLUMN_NAME_OP_TIME_WITH_ZONE, OffsetDateTime.of(
            LocalDateTime.of(2022, 1, 1, 12, 0, 0), ZoneOffset.UTC),
        COLUMN_NAME_D, new BigDecimal("104"), COLUMN_NAME_NAME, "mary")));
    builder.add(record.copy(ImmutableMap.of(COLUMN_NAME_ID, 6,
        COLUMN_NAME_OP_TIME, LocalDateTime.of(2022, 1, 1, 12, 0, 0),
        COLUMN_NAME_OP_TIME_WITH_ZONE, OffsetDateTime.of(
            LocalDateTime.of(2022, 1, 1, 12, 0, 0), ZoneOffset.UTC),
        COLUMN_NAME_D, new BigDecimal("105"), COLUMN_NAME_NAME, "mack")));
    return builder.build();
  }

  public static List<Record> changeDeleteRecords() {
    GenericRecord record = GenericRecord.create(HIVE_TABLE_SCHEMA);
    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    builder.add(record.copy(ImmutableMap.of(COLUMN_NAME_ID, 5,
        COLUMN_NAME_OP_TIME, LocalDateTime.of(2022, 1, 1, 12, 0, 0),
        COLUMN_NAME_OP_TIME_WITH_ZONE, OffsetDateTime.of(
            LocalDateTime.of(2022, 1, 1, 12, 0, 0), ZoneOffset.UTC),
        COLUMN_NAME_D, new BigDecimal("104"), COLUMN_NAME_NAME, "mary")));
    builder.add(record.copy(ImmutableMap.of(COLUMN_NAME_ID, 1,
        COLUMN_NAME_OP_TIME, LocalDateTime.of(2022, 1, 1, 12, 0, 0),
        COLUMN_NAME_OP_TIME_WITH_ZONE, OffsetDateTime.of(
            LocalDateTime.of(2022, 1, 1, 12, 0, 0), ZoneOffset.UTC),
        COLUMN_NAME_D, new BigDecimal("100"), COLUMN_NAME_NAME, "john")));
    builder.add(record.copy(ImmutableMap.of(COLUMN_NAME_ID, 3,
        COLUMN_NAME_OP_TIME, LocalDateTime.of(2022, 1, 3, 12, 0, 0),
        COLUMN_NAME_OP_TIME_WITH_ZONE, OffsetDateTime.of(
            LocalDateTime.of(2022, 1, 3, 12, 0, 0), ZoneOffset.UTC),
        COLUMN_NAME_D, new BigDecimal("102"), COLUMN_NAME_NAME, "jake")));
    return builder.build();
  }

  public static List<Record> testRecords() {
    GenericRecord record = GenericRecord.create(HIVE_TABLE_SCHEMA);

    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    builder.add(record.copy(ImmutableMap.of(COLUMN_NAME_ID, 1,
        COLUMN_NAME_OP_TIME, LocalDateTime.of(2022, 1, 1, 12, 0, 0),
        COLUMN_NAME_OP_TIME_WITH_ZONE, OffsetDateTime.of(
            LocalDateTime.of(2022, 1, 1, 12, 0, 0), ZoneOffset.UTC),
        COLUMN_NAME_D, new BigDecimal("101"), COLUMN_NAME_NAME, "jake")));
    builder.add(record.copy(ImmutableMap.of(COLUMN_NAME_ID, 2,
        COLUMN_NAME_OP_TIME, LocalDateTime.of(2022, 1, 2, 12, 0, 0),
        COLUMN_NAME_OP_TIME_WITH_ZONE, OffsetDateTime.of(
            LocalDateTime.of(2022, 1, 2, 12, 0, 0), ZoneOffset.UTC),
        COLUMN_NAME_D, new BigDecimal("102"), COLUMN_NAME_NAME, "sam")));
    builder.add(record.copy(ImmutableMap.of(COLUMN_NAME_ID, 3,
        COLUMN_NAME_OP_TIME, LocalDateTime.of(2022, 1, 3, 12, 0, 0),
        COLUMN_NAME_OP_TIME_WITH_ZONE, OffsetDateTime.of(
            LocalDateTime.of(2022, 1, 3, 12, 0, 0), ZoneOffset.UTC),
        COLUMN_NAME_D, new BigDecimal("103"), COLUMN_NAME_NAME, "mack")));
    builder.add(record.copy(ImmutableMap.of(COLUMN_NAME_ID, 4,
        COLUMN_NAME_OP_TIME, LocalDateTime.of(2022, 1, 4, 12, 0, 0),
        COLUMN_NAME_OP_TIME_WITH_ZONE, OffsetDateTime.of(
            LocalDateTime.of(2022, 1, 4, 12, 0, 0), ZoneOffset.UTC),
        COLUMN_NAME_D, new BigDecimal("104"), COLUMN_NAME_NAME, "mack")));

    return builder.build();
  }
}
