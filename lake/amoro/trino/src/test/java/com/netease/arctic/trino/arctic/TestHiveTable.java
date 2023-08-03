/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.trino.arctic;

import com.google.common.collect.ImmutableMap;
import com.netease.arctic.ams.api.MockArcticMetastoreServer;
import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.hive.io.writer.AdaptHiveGenericTaskWriterBuilder;
import com.netease.arctic.hive.table.HiveLocationKind;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.BaseLocationKind;
import com.netease.arctic.table.ChangeLocationKind;
import com.netease.arctic.table.LocationKind;
import io.trino.sql.query.QueryAssertions;
import io.trino.testing.QueryRunner;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.Files;
import org.apache.iceberg.OverwriteFiles;
import org.apache.iceberg.Schema;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.data.parquet.AdaptHiveGenericParquetReaders;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.TaskWriter;
import org.apache.iceberg.io.WriteResult;
import org.apache.iceberg.parquet.AdaptHiveParquet;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TestHiveTable extends TestHiveTableBaseForTrino {

  private final String TEST_HIVE_TABLE_FULL_NAME = "arctic." + HIVE_TABLE_ID.getDatabase() + "." + HIVE_TABLE_ID.getTableName();

  private final String TEST_HIVE_PK_TABLE_FULL_NAME =
      "arctic." + HIVE_PK_TABLE_ID.getDatabase() + "." + HIVE_PK_TABLE_ID.getTableName();

  private final String TEST_HIVE_PK_TABLE_FULL_NAME_BASE =
      "arctic." + HIVE_PK_TABLE_ID.getDatabase() + "." + "\"" + HIVE_PK_TABLE_ID.getTableName() + "#base\"";

  private final String TEST_UN_PARTITION_HIVE_TABLE_FULL_NAME =
      "arctic." + UN_PARTITION_HIVE_TABLE_ID.getDatabase() + "." + UN_PARTITION_HIVE_TABLE_ID.getTableName();

  private final String TEST_UN_PARTITION_HIVE_PK_TABLE_FULL_NAME =
      "arctic." + UN_PARTITION_HIVE_PK_TABLE_ID.getDatabase() + "." + UN_PARTITION_HIVE_PK_TABLE_ID.getTableName();

  private final String TEST_UN_PARTITION_HIVE_PK_TABLE_FULL_NAME_BASE =
      "arctic." + UN_PARTITION_HIVE_PK_TABLE_ID.getDatabase() + "." + "\"" + UN_PARTITION_HIVE_PK_TABLE_ID.getTableName() + "#base\"";

  private long txid = 1;

  @Override
  protected QueryRunner createQueryRunner() throws Exception {
    AMS = MockArcticMetastoreServer.getInstance();
    tmp.create();
    tempFolder.create();
    startMetastore();
    setupTables();
    initData();
    return ArcticQueryRunner.builder()
        .setExtraProperties(ImmutableMap.of("http-server.http.port", "8080"))
        .setIcebergProperties(ImmutableMap.of("arctic.url",
            String.format("thrift://localhost:%s/%s", AMS.port(), HIVE_CATALOG_NAME)))
        .build();
  }

  private void initData() throws IOException {
    write(testHiveTable, BaseLocationKind.INSTANT, HiveTestRecords.baseRecords());
    write(testHiveTable, HiveLocationKind.INSTANT, HiveTestRecords.hiveRecords());

    write(testKeyedHiveTable, ChangeLocationKind.INSTANT, HiveTestRecords.changeInsertRecords());
    write(testKeyedHiveTable, BaseLocationKind.INSTANT, HiveTestRecords.baseRecords());
    write(testKeyedHiveTable, HiveLocationKind.INSTANT, HiveTestRecords.hiveRecords());
    write(testKeyedHiveTable, ChangeLocationKind.INSTANT, HiveTestRecords.changeDeleteRecords(), ChangeAction.DELETE);

    write(testUnPartitionHiveTable, BaseLocationKind.INSTANT, HiveTestRecords.baseRecords());
    write(testUnPartitionHiveTable, HiveLocationKind.INSTANT, HiveTestRecords.hiveRecords());

    write(testUnPartitionKeyedHiveTable, ChangeLocationKind.INSTANT, HiveTestRecords.changeInsertRecords());
    write(testUnPartitionKeyedHiveTable, BaseLocationKind.INSTANT, HiveTestRecords.baseRecords());
    write(testUnPartitionKeyedHiveTable, HiveLocationKind.INSTANT, HiveTestRecords.hiveRecords());
    write(testUnPartitionKeyedHiveTable, ChangeLocationKind.INSTANT, HiveTestRecords.changeDeleteRecords(), ChangeAction.DELETE);
  }

  @Test
  public void testHiveTableMOR() throws InterruptedException {
    assertCommon("select id, name, op_time, \"d$d\", map_name, array_name, struct_name from "
        + TEST_HIVE_TABLE_FULL_NAME, ImmutableList.of(v1, v2, v3, v4));
  }

  @Test
  public void testKeyedHiveTableMOR() {
    assertCommon("select id, name, op_time, \"d$d\", map_name, array_name, struct_name from " + TEST_HIVE_PK_TABLE_FULL_NAME,
        ImmutableList.of(v2, v4, v6));
  }

  @Test
  public void testKeyedHiveTableBase() {
    assertCommon("select id, name, op_time, \"d$d\", map_name, array_name, struct_name from "
        + TEST_HIVE_PK_TABLE_FULL_NAME_BASE, ImmutableList.of(v1, v2, v3, v4));
  }

  @Test
  public void testNoPartitionHiveTableMOR() {
    assertCommon("select id, name, op_time, \"d$d\", map_name, array_name, struct_name from "
        + TEST_HIVE_PK_TABLE_FULL_NAME_BASE, ImmutableList.of(v1, v2, v3, v4));
  }

  @Test
  public void testNoPartitionKeyedHiveTableMOR() {
    assertCommon("select id, name, op_time, \"d$d\", map_name, array_name, struct_name from "
        + TEST_UN_PARTITION_HIVE_PK_TABLE_FULL_NAME, ImmutableList.of(v2, v4, v6));
  }

  @Test
  public void testNoPartitionKeyedHiveTableBase() {
    assertCommon("select id, name, op_time, \"d$d\", map_name, array_name, struct_name from "
        + TEST_UN_PARTITION_HIVE_PK_TABLE_FULL_NAME_BASE, ImmutableList.of(v1, v2, v3, v4));
  }

  @Test
  public void testStats() {
    assertThat(query("SHOW STATS FOR " + TEST_HIVE_PK_TABLE_FULL_NAME))
        .skippingTypesCheck()
        .matches("VALUES " +
            "('id', NULL, NULL, 0e0, NULL, '1', '6'), " +
            "('op_time', NULL, NULL, 0e0, NULL, NULL, NULL), " +
            "('op_time_with_zone', NULL, NULL, 0e0, NULL, NULL, NULL), " +
            "('d$d', NULL, NULL, 0e0, NULL, '100.0', '105.0'), " +
            "('map_name', NULL, NULL, NULL, NULL, NULL, NULL), " +
            "('array_name', NULL, NULL, NULL, NULL, NULL, NULL), " +
            "('struct_name', NULL, NULL, NULL, NULL, NULL, NULL), " +
            "('name', 618e0, NULL, 0e0, NULL, NULL, NULL), " +
            "(NULL, NULL, NULL, NULL, 9e0, NULL, NULL)");
  }


  private void assertCommon(String query, List<List<String>> values) {
    QueryAssertions.QueryAssert queryAssert = assertThat(query(query));
    StringJoiner stringJoiner = new StringJoiner(",", "VALUES", "");
    for (List<String> value : values) {
      stringJoiner.add(value.stream().collect(Collectors.joining(",", "(", ")")));
    }
    queryAssert.skippingTypesCheck().matches(stringJoiner.toString());
  }

  @AfterClass(alwaysRun = true)
  public void clear() {
    clearTable();
    stopMetastore();
  }

  private void write(ArcticTable table, LocationKind locationKind, List<Record> records) throws
      IOException {
    write(table, locationKind, records, ChangeAction.INSERT);
  }

  private void write(ArcticTable table, LocationKind locationKind, List<Record> records, ChangeAction changeAction) throws
      IOException {
    AdaptHiveGenericTaskWriterBuilder builder = AdaptHiveGenericTaskWriterBuilder
        .builderFor(table)
        .withChangeAction(changeAction)
        .withTransactionId(table.isKeyedTable() ? txid++ : null);

    TaskWriter<Record> changeWrite = builder.buildWriter(locationKind);
    for (Record record : records) {
      changeWrite.write(record);
    }
    WriteResult complete = changeWrite.complete();
    if (locationKind == ChangeLocationKind.INSTANT) {
      AppendFiles appendFiles = table.asKeyedTable().changeTable().newAppend();
      Arrays.stream(complete.dataFiles()).forEach(s -> appendFiles.appendFile(s));
      appendFiles.commit();
    } else {
      if (table.isUnkeyedTable()) {
        OverwriteFiles overwriteFiles = table.asUnkeyedTable().newOverwrite();
        Arrays.stream(complete.dataFiles()).forEach(s -> overwriteFiles.addFile(s));
        overwriteFiles.commit();
      } else {
        OverwriteFiles overwriteFiles = table.asKeyedTable().baseTable().newOverwrite();
        Arrays.stream(complete.dataFiles()).forEach(s -> overwriteFiles.addFile(s));
        overwriteFiles.commit();
      }
    }
  }

  private CloseableIterable<Record> readParquet(Schema schema, String path) {
    AdaptHiveParquet.ReadBuilder builder = AdaptHiveParquet.read(
            Files.localInput(new File(path)))
        .project(schema)
        .createReaderFunc(fileSchema -> AdaptHiveGenericParquetReaders.buildReader(schema, fileSchema, new HashMap<>()))
        .caseSensitive(false);

    CloseableIterable<Record> iterable = builder.build();
    return iterable;
  }

  private String base(String table) {
    return "\"" + table + "#base\"";
  }

  List<String> v1 = ImmutableList.of(
      "1",
      "varchar 'john'",
      "TIMESTAMP'2022-01-01 12:00:00.000000'",
      "CAST(100 AS decimal(10,0))",
      "map(ARRAY[varchar 'map_key'],ARRAY[varchar 'map_value'])",
      "ARRAY[varchar 'array_element']",
      "CAST(ROW(varchar 'struct_sub1', varchar 'struct_sub2') " +
          "AS ROW(struct_name_sub_1 varchar, struct_name_sub_2 varchar))"
  );

  List<String> v2 = ImmutableList.of(
      "2",
      "varchar 'lily'",
      "TIMESTAMP'2022-01-02 12:00:00.000000'",
      "CAST(101 AS decimal(10,0))",
      "map(ARRAY[varchar 'map_key'],ARRAY[varchar 'map_value'])",
      "ARRAY[varchar 'array_element']",
      "CAST(ROW(varchar 'struct_sub1', varchar 'struct_sub2') " +
          "AS ROW(struct_name_sub_1 varchar, struct_name_sub_2 varchar))"
  );

  List<String> v3 = ImmutableList.of(
      "3",
      "varchar 'jake'",
      "TIMESTAMP'2022-01-03 12:00:00.000000'",
      "CAST(102 AS decimal(10,0))",
      "map(ARRAY[varchar 'map_key'],ARRAY[varchar 'map_value'])",
      "ARRAY[varchar 'array_element']",
      "CAST(ROW(varchar 'struct_sub1', varchar 'struct_sub2') " +
          "AS ROW(struct_name_sub_1 varchar, struct_name_sub_2 varchar))"
  );

  List<String> v4 = ImmutableList.of(
      "4",
      "varchar 'sam'",
      "TIMESTAMP'2022-01-04 12:00:00.000000'",
      "CAST(103 AS decimal(10,0))",
      "map(ARRAY[varchar 'map_key'],ARRAY[varchar 'map_value'])",
      "ARRAY[varchar 'array_element']",
      "CAST(ROW(varchar 'struct_sub1', varchar 'struct_sub2') " +
          "AS ROW(struct_name_sub_1 varchar, struct_name_sub_2 varchar))"
  );

  List<String> v6 = ImmutableList.of(
      "6",
      "varchar 'mack'",
      "TIMESTAMP'2022-01-01 12:00:00.000000'",
      "CAST(105 AS decimal(10,0))",
      "map(ARRAY[varchar 'map_key'],ARRAY[varchar 'map_value'])",
      "ARRAY[varchar 'array_element']",
      "CAST(ROW(varchar 'struct_sub1', varchar 'struct_sub2') " +
          "AS ROW(struct_name_sub_1 varchar, struct_name_sub_2 varchar))"
  );
}
