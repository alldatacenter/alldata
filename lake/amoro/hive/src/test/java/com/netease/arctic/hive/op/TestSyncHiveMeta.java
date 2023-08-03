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

import com.netease.arctic.TableTestHelper;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.CatalogTestHelper;
import com.netease.arctic.catalog.TableTestBase;
import com.netease.arctic.hive.HiveTableProperties;
import com.netease.arctic.hive.TestHMS;
import com.netease.arctic.hive.catalog.HiveCatalogTestHelper;
import com.netease.arctic.hive.catalog.HiveTableTestHelper;
import com.netease.arctic.hive.io.HiveDataTestHelpers;
import com.netease.arctic.hive.utils.HivePartitionUtil;
import com.netease.arctic.hive.utils.HiveSchemaUtil;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.utils.ArcticTableUtil;
import com.netease.arctic.utils.TableFileUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.OverwriteFiles;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.hadoop.Util;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(Parameterized.class)
public class TestSyncHiveMeta extends TableTestBase {

  @ClassRule
  public static TestHMS TEST_HMS = new TestHMS();

  public TestSyncHiveMeta(CatalogTestHelper catalogTestHelper, TableTestHelper tableTestHelper) {
    super(catalogTestHelper, tableTestHelper);
  }

  @Parameterized.Parameters(name = "{0}, {1}")
  public static Object[] parameters() {
    return new Object[][] {{new HiveCatalogTestHelper(TableFormat.MIXED_HIVE, TEST_HMS.getHiveConf()),
                            new HiveTableTestHelper(true, true)},
                           {new HiveCatalogTestHelper(TableFormat.MIXED_HIVE, TEST_HMS.getHiveConf()),
                            new HiveTableTestHelper(true, false)},
                           {new HiveCatalogTestHelper(TableFormat.MIXED_HIVE, TEST_HMS.getHiveConf()),
                            new HiveTableTestHelper(false, true)},
                           {new HiveCatalogTestHelper(TableFormat.MIXED_HIVE, TEST_HMS.getHiveConf()),
                            new HiveTableTestHelper(false, false)}
    };
  }

  @Test
  public void testSyncHiveSchemaChange() throws TException {
    Table hiveTable = TEST_HMS.getHiveClient().getTable(
        getArcticTable().id().getDatabase(), getArcticTable().id().getTableName());
    hiveTable.getSd().getCols().add(new FieldSchema("add_column", "bigint", "add column"));
    TEST_HMS.getHiveClient().alter_table(getArcticTable().id().getDatabase(),
        getArcticTable().id().getTableName(), hiveTable);
    hiveTable = TEST_HMS.getHiveClient().getTable(
        getArcticTable().id().getDatabase(),
        getArcticTable().id().getTableName());
    getArcticTable().refresh();
    Assert.assertEquals(hiveTable.getSd().getCols(), HiveSchemaUtil.hiveTableFields(
        getArcticTable().schema(), getArcticTable().spec()));
  }

  @Test
  public void testSyncHiveDataChange() throws TException, IOException, InterruptedException {
    getArcticTable().updateProperties().set(HiveTableProperties.AUTO_SYNC_HIVE_DATA_WRITE, "true").commit();
    List<Record> insertRecords = org.apache.iceberg.relocated.com.google.common.collect.Lists.newArrayList();
    insertRecords.add(tableTestHelper().generateTestRecord(1, "john", 0, "2022-01-01T12:00:00"));

    List<DataFile> dataFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true);
    UnkeyedTable baseStore = ArcticTableUtil.baseStore(getArcticTable());
    OverwriteFiles overwriteFiles = baseStore.newOverwrite();
    dataFiles.forEach(overwriteFiles::addFile);
    overwriteFiles.commit();

    Assert.assertEquals(1, dataFiles.size());
    String dataFilePath = dataFiles.get(0).path().toString();
    FileSystem fs = Util.getFs(new Path(dataFilePath), new Configuration());
    fs.rename(new Path(dataFilePath), new Path(dataFilePath + ".bak"));
    if (isPartitionedTable()) {
      Partition partition = TEST_HMS.getHiveClient().getPartition(getArcticTable().id().getDatabase(),
          getArcticTable().id().getTableName(), Lists.newArrayList("2022-01-01"));
      TimeUnit.SECONDS.sleep(1);
      partition.getParameters().clear();
      TEST_HMS.getHiveClient().alter_partition(getArcticTable().id().getDatabase(),
          getArcticTable().id().getTableName(), partition, null);
    } else {
      Table hiveTable = TEST_HMS.getHiveClient().getTable(
          getArcticTable().id().getDatabase(),
          getArcticTable().id().getTableName());
      hiveTable.putToParameters("transient_lastDdlTime", "0");
      TEST_HMS.getHiveClient().alter_table(getArcticTable().id().getDatabase(),
          getArcticTable().id().getTableName(), hiveTable);
    }

    getArcticTable().refresh();
    Assert.assertEquals(
        Sets.newHashSet(dataFilePath + ".bak"),
        listTableFiles(baseStore).stream().map(DataFile::path).collect(Collectors.toSet()));
  }

  @Test
  public void testSyncHivePartitionChange() throws TException {
    Assume.assumeTrue(isPartitionedTable());
    getArcticTable().updateProperties().set(HiveTableProperties.AUTO_SYNC_HIVE_DATA_WRITE, "true").commit();
    List<Record> insertRecords = Lists.newArrayList();
    insertRecords.add(tableTestHelper().generateTestRecord(1, "john", 0, "2022-01-01T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-02T12:00:00"));
    List<DataFile> dataFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true);
    UnkeyedTable baseStore = ArcticTableUtil.baseStore(getArcticTable());
    OverwriteFiles overwriteFiles = baseStore.newOverwrite();
    dataFiles.forEach(overwriteFiles::addFile);
    overwriteFiles.commit();

    Table hiveTable = TEST_HMS.getHiveClient().getTable(
        getArcticTable().id().getDatabase(),
        getArcticTable().id().getTableName());

    //test add new hive partition
    insertRecords.clear();
    insertRecords.add(tableTestHelper().generateTestRecord(3, "lily", 0, "2022-01-03T12:00:00"));
    List<DataFile> newFiles = HiveDataTestHelpers.writeBaseStore(getArcticTable(), 1L, insertRecords, false, true);
    Assert.assertEquals(1, newFiles.size());
    Partition newPartition = HivePartitionUtil.newPartition(hiveTable, Lists.newArrayList("2022-01-03"),
        TableFileUtil.getFileDir(newFiles.get(0).path().toString()), newFiles,
        (int) (System.currentTimeMillis() / 1000));
    newPartition.getParameters().remove(HiveTableProperties.ARCTIC_TABLE_FLAG);
    TEST_HMS.getHiveClient().add_partition(newPartition);
    getArcticTable().refresh();

    Assert.assertEquals(
        Stream.concat(dataFiles.stream(), newFiles.stream()).map(DataFile::path).collect(Collectors.toSet()),
        listTableFiles(baseStore).stream().map(DataFile::path).collect(Collectors.toSet()));
  }

  private List<DataFile> listTableFiles(UnkeyedTable table) {
    List<DataFile> dataFiles = Lists.newArrayList();
    table.newScan().planFiles().forEach(fileScanTask -> dataFiles.add(fileScanTask.file()));
    return dataFiles;
  }
}
