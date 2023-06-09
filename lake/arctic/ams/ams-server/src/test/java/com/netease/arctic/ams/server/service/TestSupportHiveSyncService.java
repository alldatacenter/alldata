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

package com.netease.arctic.ams.server.service;

import com.netease.arctic.ams.server.optimize.TestSupportHiveBase;
import com.netease.arctic.ams.server.service.impl.SupportHiveSyncService;
import com.netease.arctic.hive.HiveTableProperties;
import com.netease.arctic.hive.io.writer.AdaptHiveGenericTaskWriterBuilder;
import com.netease.arctic.hive.table.HiveLocationKind;
import com.netease.arctic.hive.table.SupportHive;
import com.netease.arctic.hive.utils.HivePartitionUtil;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.utils.TableFileUtils;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.PrincipalPrivilegeSet;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.OutputFile;
import org.apache.iceberg.io.TaskWriter;
import org.apache.iceberg.util.StructLikeMap;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.netease.arctic.utils.TablePropertyUtil.EMPTY_STRUCT;

public class TestSupportHiveSyncService extends TestSupportHiveBase {
  @Test
  public void testUnPartitionTableSyncInIceberg() throws Exception {
    StructLikeMap<Map<String, String>> partitionProperty = testUnPartitionKeyedHiveTable.baseTable().partitionProperty();
    Assert.assertEquals(0, partitionProperty.size());
    String newLocation = createEmptyLocationForHive(testUnPartitionKeyedHiveTable);
    testUnPartitionKeyedHiveTable.baseTable().updatePartitionProperties(null)
        .set(EMPTY_STRUCT, HiveTableProperties.PARTITION_PROPERTIES_KEY_HIVE_LOCATION, newLocation).commit();
    String hiveLocation = ((SupportHive) testUnPartitionKeyedHiveTable).getHMSClient().run(client -> {
      Table hiveTable = client.getTable(testUnPartitionKeyedHiveTable.id().getDatabase(),
          testUnPartitionKeyedHiveTable.id().getTableName());
      return hiveTable.getSd().getLocation();
    });
    Assert.assertNotEquals(newLocation, hiveLocation);

    SupportHiveSyncService.SupportHiveSyncTask.syncIcebergToHive(testUnPartitionKeyedHiveTable);
    hiveLocation = ((SupportHive) testUnPartitionKeyedHiveTable).getHMSClient().run(client -> {
      Table hiveTable = client.getTable(testUnPartitionKeyedHiveTable.id().getDatabase(),
          testUnPartitionKeyedHiveTable.id().getTableName());
      return hiveTable.getSd().getLocation();
    });
    Assert.assertEquals(newLocation, hiveLocation);
  }

  @Test
  public void testUnPartitionTableSyncNotInIceberg() throws Exception {
    StructLikeMap<Map<String, String>> partitionProperty = testUnPartitionKeyedHiveTable.baseTable().partitionProperty();
    Assert.assertEquals(0, partitionProperty.size());

    String oldHiveLocation = ((SupportHive) testUnPartitionKeyedHiveTable).getHMSClient().run(client -> {
      Table hiveTable = client.getTable(testUnPartitionKeyedHiveTable.id().getDatabase(),
          testUnPartitionKeyedHiveTable.id().getTableName());
      return hiveTable.getSd().getLocation();
    });

    SupportHiveSyncService.SupportHiveSyncTask.syncIcebergToHive(testUnPartitionKeyedHiveTable);
    String newHiveLocation = ((SupportHive) testUnPartitionKeyedHiveTable).getHMSClient().run(client -> {
      Table hiveTable = client.getTable(testUnPartitionKeyedHiveTable.id().getDatabase(),
          testUnPartitionKeyedHiveTable.id().getTableName());
      return hiveTable.getSd().getLocation();
    });
    Assert.assertEquals(oldHiveLocation, newHiveLocation);
  }

  @Test
  public void testSyncOnlyInIceberg() throws Exception {
    StructLikeMap<Map<String, String>> partitionProperty = testKeyedHiveTable.baseTable().partitionProperty();
    Assert.assertEquals(0, partitionProperty.size());
    List<DataFile> dataFiles = insertTableHiveDataFiles(testKeyedHiveTable, 1);
    String partitionLocation = TableFileUtils.getFileDir(dataFiles.get(0).path().toString());
    testKeyedHiveTable.baseTable().updatePartitionProperties(null)
        .set(dataFiles.get(0).partition(), HiveTableProperties.PARTITION_PROPERTIES_KEY_HIVE_LOCATION, partitionLocation)
        .commit();

    List<String> partitionValues =
        HivePartitionUtil.partitionValuesAsList(dataFiles.get(0).partition(), testKeyedHiveTable.spec().partitionType());
    Assert.assertThrows(NoSuchObjectException.class, () -> ((SupportHive) testKeyedHiveTable).getHMSClient().run(client ->
        client.getPartition(testKeyedHiveTable.id().getDatabase(),
            testKeyedHiveTable.id().getTableName(), partitionValues)));

    SupportHiveSyncService.SupportHiveSyncTask.syncIcebergToHive(testKeyedHiveTable);
    Partition hivePartition = ((SupportHive) testKeyedHiveTable).getHMSClient().run(client ->
        client.getPartition(testKeyedHiveTable.id().getDatabase(),
            testKeyedHiveTable.id().getTableName(), partitionValues));
    Assert.assertEquals(partitionLocation, hivePartition.getSd().getLocation());
  }

  @Test
  public void testSyncOnlyInHiveCreateByArctic() throws Exception {
    StructLikeMap<Map<String, String>> partitionProperty = testKeyedHiveTable.baseTable().partitionProperty();
    Assert.assertEquals(0, partitionProperty.size());

    List<DataFile> dataFiles = insertTableHiveDataFiles(testKeyedHiveTable, 1);
    String partitionLocation = TableFileUtils.getFileDir(dataFiles.get(0).path().toString());
    List<String> partitionValues =
        HivePartitionUtil.partitionValuesAsList(dataFiles.get(0).partition(), testKeyedHiveTable.spec().partitionType());
    ((SupportHive) testKeyedHiveTable).getHMSClient().run(client ->
    {
      Table hiveTable = client.getTable(testKeyedHiveTable.id().getDatabase(), testKeyedHiveTable.id().getTableName());
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

    Partition hivePartition = ((SupportHive) testKeyedHiveTable).getHMSClient().run(client ->
        client.getPartition(testKeyedHiveTable.id().getDatabase(),
            testKeyedHiveTable.id().getTableName(), partitionValues));
    Assert.assertEquals(partitionLocation, hivePartition.getSd().getLocation());

    SupportHiveSyncService.SupportHiveSyncTask.syncIcebergToHive(testKeyedHiveTable);

    Assert.assertThrows(NoSuchObjectException.class, () -> ((SupportHive) testKeyedHiveTable).getHMSClient().run(client ->
        client.getPartition(testKeyedHiveTable.id().getDatabase(),
            testKeyedHiveTable.id().getTableName(), partitionValues)));
  }

  @Test
  public void testSyncOnlyInHiveCreateNotByArctic() throws Exception {
    StructLikeMap<Map<String, String>> partitionProperty = testKeyedHiveTable.baseTable().partitionProperty();
    Assert.assertEquals(0, partitionProperty.size());

    List<DataFile> dataFiles = insertTableHiveDataFiles(testKeyedHiveTable, 1);
    String partitionLocation = TableFileUtils.getFileDir(dataFiles.get(0).path().toString());
    List<String> partitionValues =
        HivePartitionUtil.partitionValuesAsList(dataFiles.get(0).partition(), testKeyedHiveTable.spec().partitionType());
    ((SupportHive) testKeyedHiveTable).getHMSClient().run(client ->
    {
      Table hiveTable = client.getTable(testKeyedHiveTable.id().getDatabase(), testKeyedHiveTable.id().getTableName());
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

    Partition hivePartition = ((SupportHive) testKeyedHiveTable).getHMSClient().run(client ->
        client.getPartition(testKeyedHiveTable.id().getDatabase(),
            testKeyedHiveTable.id().getTableName(), partitionValues));
    Assert.assertEquals(partitionLocation, hivePartition.getSd().getLocation());

    SupportHiveSyncService.SupportHiveSyncTask.syncIcebergToHive(testKeyedHiveTable);

    hivePartition = ((SupportHive) testKeyedHiveTable).getHMSClient().run(client ->
        client.getPartition(testKeyedHiveTable.id().getDatabase(),
            testKeyedHiveTable.id().getTableName(), partitionValues));
    Assert.assertEquals(partitionLocation, hivePartition.getSd().getLocation());
  }

  @Test
  public void testSyncInBoth() throws Exception {
    StructLikeMap<Map<String, String>> partitionProperty = testKeyedHiveTable.baseTable().partitionProperty();
    Assert.assertEquals(0, partitionProperty.size());

    List<DataFile> dataFiles = insertTableHiveDataFiles(testKeyedHiveTable, 1);
    String partitionLocation = TableFileUtils.getFileDir(dataFiles.get(0).path().toString());
    List<String> partitionValues =
        HivePartitionUtil.partitionValuesAsList(dataFiles.get(0).partition(), testKeyedHiveTable.spec().partitionType());
    ((SupportHive) testKeyedHiveTable).getHMSClient().run(client ->
    {
      Table hiveTable = client.getTable(testKeyedHiveTable.id().getDatabase(), testKeyedHiveTable.id().getTableName());
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

    Partition hivePartition = ((SupportHive) testKeyedHiveTable).getHMSClient().run(client ->
        client.getPartition(testKeyedHiveTable.id().getDatabase(),
            testKeyedHiveTable.id().getTableName(), partitionValues));
    Assert.assertEquals(partitionLocation, hivePartition.getSd().getLocation());

    List<DataFile> newDataFiles = insertTableHiveDataFiles(testKeyedHiveTable, 2);
    String newPartitionLocation = TableFileUtils.getFileDir(newDataFiles.get(0).path().toString());
    testKeyedHiveTable.baseTable().updatePartitionProperties(null)
        .set(newDataFiles.get(0).partition(), HiveTableProperties.PARTITION_PROPERTIES_KEY_HIVE_LOCATION, newPartitionLocation)
        .commit();
    Assert.assertNotEquals(newPartitionLocation, hivePartition.getSd().getLocation());

    SupportHiveSyncService.SupportHiveSyncTask.syncIcebergToHive(testKeyedHiveTable);

    hivePartition = ((SupportHive) testKeyedHiveTable).getHMSClient().run(client ->
        client.getPartition(testKeyedHiveTable.id().getDatabase(),
            testKeyedHiveTable.id().getTableName(), partitionValues));
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

  private List<DataFile> insertTableHiveDataFiles(ArcticTable arcticTable, long transactionId) throws IOException {

    Supplier<TaskWriter<Record>> taskWriterSupplier = () -> arcticTable.isKeyedTable() ?
        AdaptHiveGenericTaskWriterBuilder.builderFor(arcticTable)
            .withTransactionId(transactionId)
            .buildWriter(HiveLocationKind.INSTANT) :
        AdaptHiveGenericTaskWriterBuilder.builderFor(arcticTable)
            .buildWriter(HiveLocationKind.INSTANT);
    List<DataFile> baseDataFiles = insertBaseDataFiles(taskWriterSupplier, arcticTable.schema());
    UnkeyedTable baseTable = arcticTable.isKeyedTable() ?
        arcticTable.asKeyedTable().baseTable() : arcticTable.asUnkeyedTable();
    AppendFiles baseAppend = baseTable.newAppend();
    baseDataFiles.forEach(baseAppend::appendFile);
    baseAppend.commit();
    return baseDataFiles;
  }
}
