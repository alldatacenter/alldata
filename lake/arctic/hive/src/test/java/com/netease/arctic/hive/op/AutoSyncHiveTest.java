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

package com.netease.arctic.hive.op;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.netease.arctic.hive.HiveTableProperties;
import com.netease.arctic.hive.HiveTableTestBase;
import com.netease.arctic.hive.io.writer.AdaptHiveGenericTaskWriterBuilder;
import com.netease.arctic.hive.table.HiveLocationKind;
import com.netease.arctic.hive.utils.HivePartitionUtil;
import com.netease.arctic.hive.utils.HiveSchemaUtil;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.LocationKind;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.utils.TableFileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.OverwriteFiles;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.hadoop.Util;
import org.apache.iceberg.io.TaskWriter;
import org.apache.iceberg.io.WriteResult;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableMap;
import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

public class AutoSyncHiveTest extends HiveTableTestBase {

  @Test
  public void testAutoSyncHiveSchemaChange() throws TException {
    testAutoSyncHiveSchemaChange(testHiveTable);
    testAutoSyncHiveSchemaChange(testKeyedHiveTable);
    testAutoSyncHiveSchemaChange(testUnPartitionHiveTable);
    testAutoSyncHiveSchemaChange(testUnPartitionKeyedHiveTable);
  }

  @Test
  public void testAutoSyncPartitionedTableHiveDataWrite() throws TException, IOException {
    testAutoSyncPartitionedTableHiveDataWrite(testHiveTable);
    testAutoSyncPartitionedTableHiveDataWrite(testKeyedHiveTable);
  }

  @Test
  public void testAutoSyncUnpartitionedTableHiveDataWrite() throws TException, IOException {
    testAutoSyncUnpartitionedTableHiveDataWrite(testUnPartitionHiveTable);
    testAutoSyncUnpartitionedTableHiveDataWrite(testUnPartitionKeyedHiveTable);
  }

  private void testAutoSyncUnpartitionedTableHiveDataWrite(ArcticTable testTable) throws TException, IOException {
    testTable.updateProperties().set(HiveTableProperties.AUTO_SYNC_HIVE_DATA_WRITE, "true").commit();
    List<DataFile> dataFiles = writeDataFiles(testTable, HiveLocationKind.INSTANT,
        writeRecords("p1"));
    UnkeyedTable tableStore;
    if (testTable.isKeyedTable()) {
      tableStore = testTable.asKeyedTable().baseTable();
    } else {
      tableStore = testTable.asUnkeyedTable();
    }
    OverwriteFiles overwriteFiles = tableStore.newOverwrite();
    dataFiles.forEach(overwriteFiles::addFile);
    overwriteFiles.commit();

    Table hiveTable = hms.getClient().getTable(testTable.id().getDatabase(), testTable.id().getTableName());

    //test rewrite table
    Assert.assertEquals(1, dataFiles.size());
    String dataFilePath = dataFiles.get(0).path().toString();
    FileSystem fs = Util.getFs(new Path(dataFilePath), new Configuration());
    fs.rename(new Path(dataFilePath), new Path(dataFilePath + ".bak"));
    hiveTable.putToParameters("transient_lastDdlTime", "0");
    hms.getClient().alter_table(testTable.id().getDatabase(), testTable.id().getTableName(), hiveTable);
    testTable.refresh();
    Assert.assertEquals(
        Sets.newHashSet(dataFilePath + ".bak"),
        listTableFiles(tableStore).stream().map(DataFile::path).collect(Collectors.toSet()));
  }

  private void testAutoSyncPartitionedTableHiveDataWrite(ArcticTable testTable) throws TException, IOException {
    testTable.updateProperties().set(HiveTableProperties.AUTO_SYNC_HIVE_DATA_WRITE, "true").commit();
    Table hiveTable = hms.getClient().getTable(testTable.id().getDatabase(), testTable.id().getTableName());
    List<DataFile> dataFiles = writeDataFiles(testTable, HiveLocationKind.INSTANT,
        writeRecords("p1", "p2"));
    UnkeyedTable tableStore;
    if (testTable.isKeyedTable()) {
      tableStore = testTable.asKeyedTable().baseTable();
    } else {
      tableStore = testTable.asUnkeyedTable();
    }
    OverwriteFiles overwriteFiles = tableStore.newOverwrite();
    dataFiles.forEach(overwriteFiles::addFile);
    overwriteFiles.commit();

    //test add new hive partition
    List<DataFile> newFiles = writeDataFiles(testTable, HiveLocationKind.INSTANT,
        writeRecords("p3"));
    dataFiles.addAll(newFiles);
    Partition newPartition = HivePartitionUtil.newPartition(hiveTable, Lists.newArrayList("p3"),
        TableFileUtils.getFileDir(newFiles.get(0).path().toString()), newFiles,
        (int) (System.currentTimeMillis() / 1000));
    newPartition.getParameters().remove(HiveTableProperties.ARCTIC_TABLE_FLAG);
    hms.getClient().add_partition(newPartition);
    testTable.refresh();
    Assert.assertEquals(
        dataFiles.stream().map(DataFile::path).collect(Collectors.toSet()),
        listTableFiles(tableStore).stream().map(DataFile::path).collect(Collectors.toSet()));
  }

  private List<DataFile> listTableFiles(UnkeyedTable table) {
    List<DataFile> dataFiles = Lists.newArrayList();
    table.newScan().planFiles().forEach(fileScanTask -> dataFiles.add(fileScanTask.file()));
    return dataFiles;
  }

  private List<Record> writeRecords(String... partitionValues) {
    GenericRecord record = GenericRecord.create(HIVE_TABLE_SCHEMA);

    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    for (String partitionValue : partitionValues) {
      builder.add(record.copy(ImmutableMap.of(COLUMN_NAME_ID, 1, COLUMN_NAME_NAME, partitionValue,
          COLUMN_NAME_OP_TIME, LocalDateTime.of(2022, 1, 1, 12, 0, 0),
          COLUMN_NAME_OP_TIME_WITH_ZONE, OffsetDateTime.of(
              LocalDateTime.of(2022, 1, 1, 12, 0, 0), ZoneOffset.UTC),
          COLUMN_NAME_D, new BigDecimal("100"))));
    }
    return builder.build();
  }

  private List<DataFile> writeDataFiles(ArcticTable table, LocationKind locationKind,
      List<Record> records) throws IOException {
    AdaptHiveGenericTaskWriterBuilder builder = AdaptHiveGenericTaskWriterBuilder
        .builderFor(table)
        .withTransactionId(table.isKeyedTable() ? 1L : null);

    TaskWriter<Record> writer = builder.buildWriter(locationKind);
    for (Record record: records) {
      writer.write(record);
    }
    WriteResult complete = writer.complete();
    return Lists.newArrayList(complete.dataFiles());
  }

  private void testAutoSyncHiveSchemaChange(ArcticTable testTable) throws TException {
    Table hiveTable = hms.getClient().getTable(testTable.id().getDatabase(), testTable.id().getTableName());
    List<FieldSchema> hiveFields = Lists.newArrayList();
    hiveTable.getSd().getCols().add(new FieldSchema("add_column", "bigint", "add column"));
    hiveFields.addAll(hiveTable.getSd().getCols());
    hms.getClient().alter_table(testTable.id().getDatabase(), testTable.id().getTableName(), hiveTable);
    hiveTable = hms.getClient().getTable(testTable.id().getDatabase(), testTable.id().getTableName());
    testTable.refresh();
    Assert.assertEquals(hiveTable.getSd().getCols(), HiveSchemaUtil.hiveTableFields(
        testTable.schema(), testTable.spec()));
  }
}
