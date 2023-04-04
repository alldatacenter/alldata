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

package com.netease.arctic.hive.utils;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.netease.arctic.hive.HMSClient;
import com.netease.arctic.hive.HMSClientImpl;
import com.netease.arctic.hive.HMSClientPool;
import com.netease.arctic.hive.HiveTableProperties;
import com.netease.arctic.hive.HiveTableTestBase;
import com.netease.arctic.hive.io.writer.AdaptHiveGenericTaskWriterBuilder;
import com.netease.arctic.hive.op.RewriteHiveFiles;
import com.netease.arctic.hive.table.HiveLocationKind;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.LocationKind;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.utils.TableFileUtils;
import java.util.HashSet;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.OverwriteFiles;
import org.apache.iceberg.RewriteFiles;
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

public class HiveMetaSynchronizerTest extends HiveTableTestBase {

  @Test
  public void testSyncMetaToHive() throws TException {
    Table hiveTable = hms.getClient().getTable(HIVE_TABLE_ID.getDatabase(), HIVE_TABLE_ID.getTableName());
    Assert.assertEquals(
        HiveSchemaUtil.hiveTableFields(testHiveTable.schema(), testHiveTable.spec()),
        hiveTable.getSd().getCols());
    List<FieldSchema> hiveFields = Lists.newArrayList(hiveTable.getSd().getCols());
    List<FieldSchema> addedFields = Lists.newArrayList(hiveFields);
    addedFields.add(new FieldSchema("add_column",
        "struct<id:bigint, name:string>", "add column"));
    hiveTable.getSd().setCols(addedFields);
    hms.getClient().alter_table(HIVE_TABLE_ID.getDatabase(), HIVE_TABLE_ID.getTableName(), hiveTable);
    HiveMetaSynchronizer.syncHiveSchemaToArctic(testHiveTable, new TestHMSClient());
    Assert.assertEquals(addedFields, hiveTable.getSd().getCols());
    Assert.assertEquals(addedFields, HiveSchemaUtil.hiveTableFields(testHiveTable.schema(), testHiveTable.spec()));

    addedFields = Lists.newArrayList(hiveFields);
    addedFields.add(new FieldSchema("add_column",
        "struct<id:bigint, name:string, add_column:string>", "add column"));
    hiveTable.getSd().setCols(addedFields);
    hms.getClient().alter_table(HIVE_TABLE_ID.getDatabase(), HIVE_TABLE_ID.getTableName(), hiveTable);
    HiveMetaSynchronizer.syncHiveSchemaToArctic(testHiveTable, new TestHMSClient());
    Assert.assertEquals(addedFields, hiveTable.getSd().getCols());
    Assert.assertEquals(addedFields, HiveSchemaUtil.hiveTableFields(testHiveTable.schema(), testHiveTable.spec()));
  }

  @Test
  public void testSyncDataToHive() throws IOException, TException {
    Table hiveTable = hms.getClient().getTable(HIVE_TABLE_ID.getDatabase(), HIVE_TABLE_ID.getTableName());
    Assert.assertEquals(0, Iterables.size(testHiveTable.snapshots()));
    List<DataFile> dataFiles = writeDataFiles(testHiveTable, HiveLocationKind.INSTANT,
        writeRecords("p1", "p2"));
    String partition1FilePath = null;
    String partition2FilePath = null;
    String partition3FilePath = null;
    for (DataFile dataFile : dataFiles) {
      if (dataFile.partition().get(0, String.class).equals("p1")) {
        partition1FilePath = dataFile.path().toString();
      } else if (dataFile.partition().get(0, String.class).equals("p2")) {
        partition2FilePath = dataFile.path().toString();
      }
    }
    Assert.assertNotNull(partition1FilePath);
    Assert.assertNotNull(partition2FilePath);

    OverwriteFiles overwriteFiles = testHiveTable.newOverwrite();
    dataFiles.forEach(overwriteFiles::addFile);
    overwriteFiles.commit();
    Assert.assertEquals(1, Iterables.size(testHiveTable.snapshots()));
    List<Partition> partitions = hms.getClient()
        .listPartitions(HIVE_TABLE_ID.getDatabase(), HIVE_TABLE_ID.getTableName(), Short.MAX_VALUE);
    Assert.assertEquals(2, partitions.size());

    //test add new hive partition
    List<DataFile> newFiles = writeDataFiles(testHiveTable, HiveLocationKind.INSTANT,
        writeRecords("p3"));
    Assert.assertEquals(1, newFiles.size());
    partition3FilePath = newFiles.get(0).path().toString();
    Partition newPartition = HivePartitionUtil.newPartition(hiveTable, Lists.newArrayList("p3"),
        TableFileUtils.getFileDir(newFiles.get(0).path().toString()), newFiles,
        (int) (System.currentTimeMillis() / 1000));
    newPartition.getParameters().remove(HiveTableProperties.ARCTIC_TABLE_FLAG);
    hms.getClient().add_partition(newPartition);
    HiveMetaSynchronizer.syncHiveDataToArctic(testHiveTable, new TestHMSClient());
    Assert.assertEquals(2, Iterables.size(testHiveTable.snapshots()));
    partitions = hms.getClient()
        .listPartitions(HIVE_TABLE_ID.getDatabase(), HIVE_TABLE_ID.getTableName(), Short.MAX_VALUE);
    Assert.assertEquals(3, partitions.size());
    Assert.assertEquals(
        Sets.newHashSet(partition1FilePath, partition2FilePath, partition3FilePath),
        listTableFiles(testHiveTable).stream().map(DataFile::path).collect(Collectors.toSet()));

    //test drop hive partition
    hms.getClient().dropPartition(HIVE_TABLE_ID.getDatabase(), HIVE_TABLE_ID.getTableName(),
        Lists.newArrayList("p1"));
    testHiveTable.io().deleteFile(partition1FilePath);
    HiveMetaSynchronizer.syncHiveDataToArctic(testHiveTable, new TestHMSClient());
    Assert.assertEquals(3, Iterables.size(testHiveTable.snapshots()));
    partitions = hms.getClient()
        .listPartitions(HIVE_TABLE_ID.getDatabase(), HIVE_TABLE_ID.getTableName(), Short.MAX_VALUE);
    Assert.assertEquals(2, partitions.size());
    dataFiles.remove(0); // remove p1 file
    Assert.assertEquals(
        Sets.newHashSet(partition2FilePath, partition3FilePath),
        listTableFiles(testHiveTable).stream().map(DataFile::path).collect(Collectors.toSet()));

    //test rewrite hive partition
    FileSystem fs = Util.getFs(new Path(partition2FilePath), new Configuration());
    fs.rename(new Path(partition2FilePath), new Path(partition2FilePath + ".bak"));
    Partition hivePartition2 = hms.getClient().getPartition(HIVE_TABLE_ID.getDatabase(), HIVE_TABLE_ID.getTableName(),
        Lists.newArrayList("p2"));
    hivePartition2.putToParameters("transient_lastDdlTime", String.valueOf(1000));
    hms.getClient().alter_partition(HIVE_TABLE_ID.getDatabase(), HIVE_TABLE_ID.getTableName(), hivePartition2, null);
    HiveMetaSynchronizer.syncHiveDataToArctic(testHiveTable, new TestHMSClient());
    Assert.assertEquals(4, Iterables.size(testHiveTable.snapshots()));
    partitions = hms.getClient()
        .listPartitions(HIVE_TABLE_ID.getDatabase(), HIVE_TABLE_ID.getTableName(), Short.MAX_VALUE);
    Assert.assertEquals(2, partitions.size());
    partition2FilePath = partition2FilePath + ".bak";
    Assert.assertEquals(Sets.newHashSet(partition2FilePath, partition3FilePath),
        listTableFiles(testHiveTable).stream().map(DataFile::path).collect(Collectors.toSet()));

    //should not sync hive data to arctic when both hive location and hive transient_lastDdlTime not changed
    OverwriteFiles p4OverwriteFiles = testHiveTable.newOverwrite();
    List<DataFile> p4DataFiles = writeDataFiles(testHiveTable, HiveLocationKind.INSTANT,
        writeRecords("p4"));
    p4DataFiles.forEach(p4OverwriteFiles::addFile);
    p4OverwriteFiles.commit();
    Partition hiveOldPartition = hms.getClient().getPartition(HIVE_TABLE_ID.getDatabase(), HIVE_TABLE_ID.getTableName(),
        Lists.newArrayList("p4"));
    List<DataFile> p4NewFiles = writeDataFiles(testHiveTable, HiveLocationKind.INSTANT,
        writeRecords("p4"));
    RewriteHiveFiles p4NewRewrite = testHiveTable.newRewrite();
    p4NewRewrite.rewriteFiles(new HashSet<>(p4DataFiles), new HashSet<>(p4NewFiles));
    p4NewRewrite.commit();
    Partition hiveNewPartition = hms.getClient().getPartition(HIVE_TABLE_ID.getDatabase(), HIVE_TABLE_ID.getTableName(),
        Lists.newArrayList("p4"));
    Assert.assertEquals(p4NewFiles.get(0)
        .path()
        .toString()
        .substring(0, p4NewFiles.get(0).path().toString().lastIndexOf("/")), hiveNewPartition.getSd().getLocation());
    hms.getClient().alter_partition(HIVE_TABLE_ID.getDatabase(), HIVE_TABLE_ID.getTableName(), hiveOldPartition, null);
    int snapshotSizeBeforeSync = Iterables.size(testHiveTable.snapshots());
    HiveMetaSynchronizer.syncHiveDataToArctic(testHiveTable, new TestHMSClient());
    int snapshotSizeAfterSync = Iterables.size(testHiveTable.snapshots());
    Assert.assertEquals(snapshotSizeBeforeSync, snapshotSizeAfterSync);
  }

  @Test
  public void testKeyedTableSyncDataToHive() throws IOException, TException {
    Table hiveTable = hms.getClient()
        .getTable(UN_PARTITION_HIVE_PK_TABLE_ID.getDatabase(), UN_PARTITION_HIVE_PK_TABLE_ID.getTableName());
    List<DataFile> p1DataFiles = writeDataFiles(testUnPartitionKeyedHiveTable, HiveLocationKind.INSTANT,
        writeRecords("p1"));
    hiveTable.getSd().setLocation(p1DataFiles.get(0).path().toString().substring(
        0,
        p1DataFiles.get(0).path().toString().lastIndexOf("/")));
    hms.getClient().alter_table(UN_PARTITION_HIVE_PK_TABLE_ID.getDatabase(),
        UN_PARTITION_HIVE_PK_TABLE_ID.getTableName(), hiveTable);
    OverwriteFiles p1OverwriteFiles = testUnPartitionKeyedHiveTable.baseTable().newOverwrite();
    p1DataFiles.forEach(p1OverwriteFiles::addFile);
    p1OverwriteFiles.commit();
    Assert.assertEquals(1, p1DataFiles.size());
    hiveTable.getParameters().remove(HiveTableProperties.ARCTIC_TABLE_FLAG);
    hiveTable.putToParameters("transient_lastDdlTime", String.valueOf(100));
    hms.getClient().alter_table(UN_PARTITION_HIVE_PK_TABLE_ID.getDatabase(),
        UN_PARTITION_HIVE_PK_TABLE_ID.getTableName(), hiveTable);
    HiveMetaSynchronizer.syncHiveDataToArctic(testUnPartitionKeyedHiveTable, new TestHMSClient());
    Assert.assertEquals(2, Iterables.size(testUnPartitionKeyedHiveTable.baseTable().snapshots()));

    //should not sync hive data to arctic when both hive location and hive transient_lastDdlTime not changed
    hiveTable = hms.getClient()
        .getTable(UN_PARTITION_HIVE_PK_TABLE_ID.getDatabase(), UN_PARTITION_HIVE_PK_TABLE_ID.getTableName());
    List<DataFile> p1NewFiles = writeDataFiles(testUnPartitionKeyedHiveTable, HiveLocationKind.INSTANT,
        writeRecords("p1"));
    RewriteFiles p1NewRewrite = testUnPartitionKeyedHiveTable.baseTable().newRewrite();
    p1NewRewrite.rewriteFiles(new HashSet<>(p1DataFiles), new HashSet<>(p1NewFiles));
    p1NewRewrite.commit();
    hiveTable.putToParameters("transient_lastDdlTime", String.valueOf(1000));
    hms.getClient().alter_table(UN_PARTITION_HIVE_PK_TABLE_ID.getDatabase(),
        UN_PARTITION_HIVE_PK_TABLE_ID.getTableName(), hiveTable);
    int snapshotSizeBeforeSync = Iterables.size(testHiveTable.snapshots());
    HiveMetaSynchronizer.syncHiveDataToArctic(testUnPartitionKeyedHiveTable, new TestHMSClient());
    int snapshotSizeAfterSync = Iterables.size(testHiveTable.snapshots());
    Assert.assertEquals(snapshotSizeBeforeSync, snapshotSizeAfterSync);
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

  private List<DataFile> writeDataFiles(
      ArcticTable table, LocationKind locationKind,
      List<Record> records) throws IOException {
    AdaptHiveGenericTaskWriterBuilder builder = AdaptHiveGenericTaskWriterBuilder
        .builderFor(table)
        .withTransactionId(table.isKeyedTable() ? 1L : null);

    TaskWriter<Record> writer = builder.buildWriter(locationKind);
    for (Record record : records) {
      writer.write(record);
    }
    WriteResult complete = writer.complete();
    return Lists.newArrayList(complete.dataFiles());
  }

  private static class TestHMSClient implements HMSClientPool {

    @Override
    public <R> R run(Action<R, HMSClient, TException> action) throws TException, InterruptedException {
      return action.run(new HMSClientImpl(hms.getClient()));
    }

    @Override
    public <R> R run(Action<R, HMSClient, TException> action, boolean retry) throws TException, InterruptedException {
      return action.run(new HMSClientImpl(hms.getClient()));
    }
  }
}
