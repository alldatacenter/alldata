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

package com.netease.arctic.server.table.executor;

import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.hive.HiveTableProperties;
import com.netease.arctic.hive.TestHMS;
import com.netease.arctic.hive.catalog.HiveCatalogTestHelper;
import com.netease.arctic.hive.catalog.HiveTableTestHelper;
import com.netease.arctic.hive.table.SupportHive;
import com.netease.arctic.hive.utils.HivePartitionUtil;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.utils.TableFileUtil;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.PrincipalPrivilegeSet;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.io.OutputFile;
import org.apache.iceberg.util.StructLikeMap;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.netease.arctic.utils.TablePropertyUtil.EMPTY_STRUCT;

@RunWith(Parameterized.class)
public class TestHiveCommitSync extends ExecutorTestBase {
  @ClassRule
  public static TestHMS TEST_HMS = new TestHMS();

  public TestHiveCommitSync(boolean ifKeyed, boolean ifPartitioned) {
    super(new HiveCatalogTestHelper(TableFormat.MIXED_HIVE, TEST_HMS.getHiveConf()),
        new HiveTableTestHelper(ifKeyed, ifPartitioned));
  }

  @Parameterized.Parameters(name = "ifKeyed = {0}, ifPartitioned = {1}")
  public static Object[][] parameters() {
    return new Object[][] {
        {true, true},
        {true, false},
        {false, true},
        {false, false}};
  }


  @Test
  public void testUnPartitionTableSyncInIceberg() throws Exception {
    Assume.assumeFalse(isPartitionedTable());
    UnkeyedTable baseTable = isKeyedTable() ?
        getArcticTable().asKeyedTable().baseTable() : getArcticTable().asUnkeyedTable();
    StructLikeMap<Map<String, String>> partitionProperty = baseTable.partitionProperty();
    Assert.assertEquals(0, partitionProperty.size());
    String newLocation = createEmptyLocationForHive(getArcticTable());
    baseTable.updatePartitionProperties(null)
        .set(EMPTY_STRUCT, HiveTableProperties.PARTITION_PROPERTIES_KEY_HIVE_LOCATION, newLocation).commit();
    String hiveLocation = ((SupportHive) getArcticTable()).getHMSClient().run(client -> {
      Table hiveTable = client.getTable(getArcticTable().id().getDatabase(),
          getArcticTable().id().getTableName());
      return hiveTable.getSd().getLocation();
    });
    Assert.assertNotEquals(newLocation, hiveLocation);

    HiveCommitSyncExecutor.syncIcebergToHive(getArcticTable());
    hiveLocation = ((SupportHive) getArcticTable()).getHMSClient().run(client -> {
      Table hiveTable = client.getTable(getArcticTable().id().getDatabase(),
          getArcticTable().id().getTableName());
      return hiveTable.getSd().getLocation();
    });
    Assert.assertEquals(newLocation, hiveLocation);
  }

  @Test
  public void testUnPartitionTableSyncNotInIceberg() throws Exception {
    Assume.assumeFalse(isPartitionedTable());
    UnkeyedTable baseTable = isKeyedTable() ?
        getArcticTable().asKeyedTable().baseTable() : getArcticTable().asUnkeyedTable();
    StructLikeMap<Map<String, String>> partitionProperty = baseTable.partitionProperty();
    Assert.assertEquals(0, partitionProperty.size());

    String oldHiveLocation = ((SupportHive) getArcticTable()).getHMSClient().run(client -> {
      Table hiveTable = client.getTable(getArcticTable().id().getDatabase(),
          getArcticTable().id().getTableName());
      return hiveTable.getSd().getLocation();
    });

    HiveCommitSyncExecutor.syncIcebergToHive(getArcticTable());
    String newHiveLocation = ((SupportHive) getArcticTable()).getHMSClient().run(client -> {
      Table hiveTable = client.getTable(getArcticTable().id().getDatabase(),
          getArcticTable().id().getTableName());
      return hiveTable.getSd().getLocation();
    });
    Assert.assertEquals(oldHiveLocation, newHiveLocation);
  }

  @Test
  public void testSyncOnlyInIceberg() throws Exception {
    Assume.assumeTrue(isPartitionedTable());
    UnkeyedTable baseTable = isKeyedTable() ?
        getArcticTable().asKeyedTable().baseTable() : getArcticTable().asUnkeyedTable();
    StructLikeMap<Map<String, String>> partitionProperty = baseTable.partitionProperty();
    Assert.assertEquals(0, partitionProperty.size());
    List<DataFile> dataFiles = writeAndCommitBaseAndHive(getArcticTable(), 1, true);
    String partitionLocation = TableFileUtil.getFileDir(dataFiles.get(0).path().toString());
    baseTable.updatePartitionProperties(null)
        .set(dataFiles.get(0).partition(), HiveTableProperties.PARTITION_PROPERTIES_KEY_HIVE_LOCATION,
            partitionLocation)
        .commit();

    List<String> partitionValues =
        HivePartitionUtil.partitionValuesAsList(dataFiles.get(0).partition(), getArcticTable().spec().partitionType());
    Assert.assertThrows(NoSuchObjectException.class, () -> ((SupportHive) getArcticTable()).getHMSClient().run(client ->
        client.getPartition(getArcticTable().id().getDatabase(),
            getArcticTable().id().getTableName(), partitionValues)));

    HiveCommitSyncExecutor.syncIcebergToHive(getArcticTable());
    Partition hivePartition = ((SupportHive) getArcticTable()).getHMSClient().run(client ->
        client.getPartition(getArcticTable().id().getDatabase(),
            getArcticTable().id().getTableName(), partitionValues));
    Assert.assertEquals(partitionLocation, hivePartition.getSd().getLocation());
  }

  @Test
  public void testSyncOnlyInHiveCreateByArctic() throws Exception {
    Assume.assumeTrue(isPartitionedTable());
    UnkeyedTable baseTable = isKeyedTable() ?
        getArcticTable().asKeyedTable().baseTable() : getArcticTable().asUnkeyedTable();
    StructLikeMap<Map<String, String>> partitionProperty = baseTable.partitionProperty();
    Assert.assertEquals(0, partitionProperty.size());

    List<DataFile> dataFiles = writeAndCommitBaseAndHive(getArcticTable(), 1, true);
    String partitionLocation = TableFileUtil.getFileDir(dataFiles.get(0).path().toString());
    List<String> partitionValues =
        HivePartitionUtil.partitionValuesAsList(dataFiles.get(0).partition(), getArcticTable().spec().partitionType());
    ((SupportHive) getArcticTable()).getHMSClient().run(client -> {
      Table hiveTable = client.getTable(getArcticTable().id().getDatabase(), getArcticTable().id().getTableName());
      StorageDescriptor tableSd = hiveTable.getSd();
      PrincipalPrivilegeSet privilegeSet = hiveTable.getPrivileges();
      int lastAccessTime = (int) (System.currentTimeMillis() / 1000);
      Partition p = new Partition();
      p.setValues(partitionValues);
      p.setDbName(hiveTable.getDbName());
      p.setTableName(hiveTable.getTableName());
      p.setCreateTime(lastAccessTime);
      p.setLastAccessTime(lastAccessTime);
      StorageDescriptor sd = tableSd.deepCopy();
      sd.setLocation(partitionLocation);
      p.setSd(sd);

      int files = dataFiles.size();
      long totalSize = dataFiles.stream().map(ContentFile::fileSizeInBytes).reduce(0L, Long::sum);
      p.putToParameters("transient_lastDdlTime", lastAccessTime + "");
      p.putToParameters("totalSize", totalSize + "");
      p.putToParameters("numFiles", files + "");
      p.putToParameters(HiveTableProperties.ARCTIC_TABLE_FLAG, "true");
      if (privilegeSet != null) {
        p.setPrivileges(privilegeSet.deepCopy());
      }

      return client.addPartition(p);
    });

    Partition hivePartition = ((SupportHive) getArcticTable()).getHMSClient().run(client ->
        client.getPartition(getArcticTable().id().getDatabase(),
            getArcticTable().id().getTableName(), partitionValues));
    Assert.assertEquals(partitionLocation, hivePartition.getSd().getLocation());

    HiveCommitSyncExecutor.syncIcebergToHive(getArcticTable());

    Assert.assertThrows(NoSuchObjectException.class, () -> ((SupportHive) getArcticTable()).getHMSClient().run(client ->
        client.getPartition(getArcticTable().id().getDatabase(),
            getArcticTable().id().getTableName(), partitionValues)));
  }

  @Test
  public void testSyncOnlyInHiveCreateNotByArctic() throws Exception {
    Assume.assumeTrue(isPartitionedTable());
    UnkeyedTable baseTable = isKeyedTable() ?
        getArcticTable().asKeyedTable().baseTable() : getArcticTable().asUnkeyedTable();
    StructLikeMap<Map<String, String>> partitionProperty = baseTable.partitionProperty();
    Assert.assertEquals(0, partitionProperty.size());

    List<DataFile> dataFiles = writeAndCommitBaseAndHive(getArcticTable(), 1, true);
    String partitionLocation = TableFileUtil.getFileDir(dataFiles.get(0).path().toString());
    List<String> partitionValues =
        HivePartitionUtil.partitionValuesAsList(dataFiles.get(0).partition(), getArcticTable().spec().partitionType());
    ((SupportHive) getArcticTable()).getHMSClient().run(client -> {
      Table hiveTable = client.getTable(getArcticTable().id().getDatabase(), getArcticTable().id().getTableName());
      StorageDescriptor tableSd = hiveTable.getSd();
      PrincipalPrivilegeSet privilegeSet = hiveTable.getPrivileges();
      int lastAccessTime = (int) (System.currentTimeMillis() / 1000);
      Partition p = new Partition();
      p.setValues(partitionValues);
      p.setDbName(hiveTable.getDbName());
      p.setTableName(hiveTable.getTableName());
      p.setCreateTime(lastAccessTime);
      p.setLastAccessTime(lastAccessTime);
      StorageDescriptor sd = tableSd.deepCopy();
      sd.setLocation(partitionLocation);
      p.setSd(sd);

      int files = dataFiles.size();
      long totalSize = dataFiles.stream().map(ContentFile::fileSizeInBytes).reduce(0L, Long::sum);
      p.putToParameters("transient_lastDdlTime", lastAccessTime + "");
      p.putToParameters("totalSize", totalSize + "");
      p.putToParameters("numFiles", files + "");
      if (privilegeSet != null) {
        p.setPrivileges(privilegeSet.deepCopy());
      }

      return client.addPartition(p);
    });

    Partition hivePartition = ((SupportHive) getArcticTable()).getHMSClient().run(client ->
        client.getPartition(getArcticTable().id().getDatabase(),
            getArcticTable().id().getTableName(), partitionValues));
    Assert.assertEquals(partitionLocation, hivePartition.getSd().getLocation());

    HiveCommitSyncExecutor.syncIcebergToHive(getArcticTable());

    hivePartition = ((SupportHive) getArcticTable()).getHMSClient().run(client ->
        client.getPartition(getArcticTable().id().getDatabase(),
            getArcticTable().id().getTableName(), partitionValues));
    Assert.assertEquals(partitionLocation, hivePartition.getSd().getLocation());
  }

  @Test
  public void testSyncInBoth() throws Exception {
    Assume.assumeTrue(isPartitionedTable());
    UnkeyedTable baseTable = isKeyedTable() ?
        getArcticTable().asKeyedTable().baseTable() : getArcticTable().asUnkeyedTable();
    StructLikeMap<Map<String, String>> partitionProperty = baseTable.partitionProperty();
    Assert.assertEquals(0, partitionProperty.size());

    List<DataFile> dataFiles = writeAndCommitBaseAndHive(getArcticTable(), 1, true);
    String partitionLocation = TableFileUtil.getFileDir(dataFiles.get(0).path().toString());
    List<String> partitionValues =
        HivePartitionUtil.partitionValuesAsList(dataFiles.get(0).partition(), getArcticTable().spec().partitionType());
    ((SupportHive) getArcticTable()).getHMSClient().run(client -> {
      Table hiveTable = client.getTable(getArcticTable().id().getDatabase(), getArcticTable().id().getTableName());
      StorageDescriptor tableSd = hiveTable.getSd();
      PrincipalPrivilegeSet privilegeSet = hiveTable.getPrivileges();
      int lastAccessTime = (int) (System.currentTimeMillis() / 1000);
      Partition p = new Partition();
      p.setValues(partitionValues);
      p.setDbName(hiveTable.getDbName());
      p.setTableName(hiveTable.getTableName());
      p.setCreateTime(lastAccessTime);
      p.setLastAccessTime(lastAccessTime);
      StorageDescriptor sd = tableSd.deepCopy();
      sd.setLocation(partitionLocation);
      p.setSd(sd);

      int files = dataFiles.size();
      long totalSize = dataFiles.stream().map(ContentFile::fileSizeInBytes).reduce(0L, Long::sum);
      p.putToParameters("transient_lastDdlTime", lastAccessTime + "");
      p.putToParameters("totalSize", totalSize + "");
      p.putToParameters("numFiles", files + "");
      if (privilegeSet != null) {
        p.setPrivileges(privilegeSet.deepCopy());
      }

      return client.addPartition(p);
    });

    Partition hivePartition = ((SupportHive) getArcticTable()).getHMSClient().run(client ->
        client.getPartition(getArcticTable().id().getDatabase(),
            getArcticTable().id().getTableName(), partitionValues));
    Assert.assertEquals(partitionLocation, hivePartition.getSd().getLocation());

    List<DataFile> newDataFiles = writeAndCommitBaseAndHive(getArcticTable(), 2, true);
    String newPartitionLocation = TableFileUtil.getFileDir(newDataFiles.get(0).path().toString());
    baseTable.updatePartitionProperties(null)
        .set(newDataFiles.get(0).partition(), HiveTableProperties.PARTITION_PROPERTIES_KEY_HIVE_LOCATION,
            newPartitionLocation)
        .commit();
    Assert.assertNotEquals(newPartitionLocation, hivePartition.getSd().getLocation());

    HiveCommitSyncExecutor.syncIcebergToHive(getArcticTable());

    hivePartition = ((SupportHive) getArcticTable()).getHMSClient().run(client ->
        client.getPartition(getArcticTable().id().getDatabase(),
            getArcticTable().id().getTableName(), partitionValues));
    Assert.assertEquals(newPartitionLocation, hivePartition.getSd().getLocation());
  }

  private String createEmptyLocationForHive(ArcticTable arcticTable) {
    // create a new empty location for hive
    String newLocation = ((SupportHive) arcticTable).hiveLocation() + "/ts_" + System.currentTimeMillis();
    OutputFile file = arcticTable.io().newOutputFile(newLocation + "/.keep");
    try {
      file.createOrOverwrite().close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return newLocation;
  }
}
