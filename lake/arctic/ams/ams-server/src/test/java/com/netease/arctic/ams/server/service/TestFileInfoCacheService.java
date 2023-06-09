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

import com.netease.arctic.TableTestBase;
import com.netease.arctic.ams.api.CommitMetaProducer;
import com.netease.arctic.ams.api.DataFile;
import com.netease.arctic.ams.api.DataFileInfo;
import com.netease.arctic.ams.api.MetaException;
import com.netease.arctic.ams.api.PartitionFieldData;
import com.netease.arctic.ams.api.TableChange;
import com.netease.arctic.ams.api.TableCommitMeta;
import com.netease.arctic.ams.api.TableIdentifier;
import com.netease.arctic.ams.server.model.TransactionsOfTable;
import com.netease.arctic.ams.server.util.TableUtil;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileMetadata;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.netease.arctic.ams.server.AmsTestBase.AMS_TEST_CATALOG_NAME;
import static com.netease.arctic.ams.server.AmsTestBase.AMS_TEST_DB_NAME;
import static com.netease.arctic.ams.server.AmsTestBase.catalog;

public class TestFileInfoCacheService extends TableTestBase {

  TableIdentifier tableIdentifier = new TableIdentifier(AMS_TEST_CATALOG_NAME, "test", "test");
  Random random = new Random();

  @Test
  public void testAppendCommit() throws MetaException {
    TableCommitMeta meta = new TableCommitMeta();
    meta.setAction("append");
    meta.setCommitTime(System.currentTimeMillis());
    meta.setCommitMetaProducer(CommitMetaProducer.INGESTION);
    meta.setTableIdentifier(tableIdentifier);
    List<TableChange> changes = new ArrayList<>();
    TableChange change = new TableChange();
    change.setParentSnapshotId(-1);
    change.setInnerTable("base");
    List<DataFile> dataFiles = new ArrayList<>();
    dataFiles.add(genDatafile());
    change.setAddFiles(dataFiles);
    long snapshotId = 1L;
    change.setSnapshotId(snapshotId);
    TableChange change1 = new TableChange();
    change1.setParentSnapshotId(snapshotId);
    change1.setInnerTable("base");
    List<DataFile> dataFiles1 = new ArrayList<>();
    dataFiles1.add(genDatafile());
    change1.setAddFiles(dataFiles1);
    long snapshotId1 = 2L;
    change1.setSnapshotId(snapshotId1);

    changes.add(change);
    changes.add(change1);
    meta.setChanges(changes);
    Map<String, String> properties = new HashMap<>();
    properties.put(TableProperties.TABLE_EVENT_TIME_FIELD, "eventTime");
    meta.setProperties(properties);
    ServiceContainer.getFileInfoCacheService().commitCacheFileInfo(meta);

    List<TransactionsOfTable> transactionsOfTables =
        ServiceContainer.getFileInfoCacheService().getTxExcludeOptimize(tableIdentifier);
    Assert.assertEquals(2, transactionsOfTables.size());
    Assert.assertEquals(snapshotId1, transactionsOfTables.get(0).getTransactionId());
    Assert.assertEquals(snapshotId, transactionsOfTables.get(1).getTransactionId());
  }

  @Test
  public void testNoNewFilesCommit() throws MetaException {
    TableCommitMeta meta = new TableCommitMeta();
    meta.setAction("append");
    meta.setCommitTime(System.currentTimeMillis());
    meta.setCommitMetaProducer(CommitMetaProducer.INGESTION);
    TableIdentifier tableIdentifier = new TableIdentifier(AMS_TEST_CATALOG_NAME, "test", "testNoNewFilesCommit");
    meta.setTableIdentifier(tableIdentifier);
    List<TableChange> changes = new ArrayList<>();
    TableChange change = new TableChange();
    change.setParentSnapshotId(-1);
    change.setInnerTable("base");
    long snapshotId = 1L;
    change.setSnapshotId(snapshotId);

    changes.add(change);
    meta.setChanges(changes);
    Map<String, String> properties = new HashMap<>();
    properties.put(TableProperties.TABLE_EVENT_TIME_FIELD, "eventTime");
    meta.setProperties(properties);
    ServiceContainer.getFileInfoCacheService().commitCacheFileInfo(meta);

    List<TransactionsOfTable> transactionsOfTables =
        ServiceContainer.getFileInfoCacheService().getTxExcludeOptimize(tableIdentifier, false);
    Assert.assertEquals(1, transactionsOfTables.size());
    Assert.assertEquals(snapshotId, transactionsOfTables.get(0).getTransactionId());
  }

  @Test
  public void testNeedFixCacheWhenParentNotCached() throws MetaException {
    TableCommitMeta meta = new TableCommitMeta();
    meta.setAction("append");
    meta.setCommitTime(System.currentTimeMillis());
    meta.setCommitMetaProducer(CommitMetaProducer.INGESTION);
    meta.setTableIdentifier(tableIdentifier);
    List<TableChange> changes = new ArrayList<>();
    TableChange change = new TableChange();
    change.setParentSnapshotId(1);
    change.setInnerTable("base");
    List<DataFile> dataFiles = new ArrayList<>();
    dataFiles.add(genDatafile());
    change.setAddFiles(dataFiles);
    long snapshotId = 2L;
    change.setSnapshotId(snapshotId);
    ServiceContainer.getFileInfoCacheService().commitCacheFileInfo(meta);
    meta.setChanges(changes);

    AtomicBoolean isCached = new AtomicBoolean(false);
    ServiceContainer.getFileInfoCacheService().getTxExcludeOptimize(tableIdentifier).forEach(transactionsOfTable -> {
      if (transactionsOfTable.getTransactionId() == snapshotId) {
        isCached.set(true);
      }
    });
    Assert.assertFalse(isCached.get());
  }

  @Test
  public void testUpdateDeleteCommit() throws MetaException {
    TableCommitMeta meta = new TableCommitMeta();
    meta.setAction("append");
    meta.setCommitTime(System.currentTimeMillis());
    meta.setCommitMetaProducer(CommitMetaProducer.INGESTION);
    meta.setTableIdentifier(tableIdentifier);
    List<TableChange> changes = new ArrayList<>();
    TableChange change = new TableChange();
    change.setParentSnapshotId(-1);
    change.setInnerTable("base");
    List<DataFile> dataFiles = new ArrayList<>();
    dataFiles.add(genDatafile());
    change.setAddFiles(dataFiles);
    long snapshotId = 1L;
    change.setSnapshotId(snapshotId);

    TableChange change1 = new TableChange();
    change1.setParentSnapshotId(snapshotId);
    change1.setInnerTable("base");
    List<DataFile> dataFiles1 = new ArrayList<>();
    dataFiles1.add(genDatafile());
    change1.setDeleteFiles(dataFiles);
    change1.setAddFiles(dataFiles1);
    long snapshotId1 = 2L;
    change1.setSnapshotId(snapshotId1);

    changes.add(change);
    changes.add(change1);
    meta.setChanges(changes);
    Map<String, String> properties = new HashMap<>();
    properties.put(TableProperties.TABLE_EVENT_TIME_FIELD, "eventTime");
    meta.setProperties(properties);
    ServiceContainer.getFileInfoCacheService().commitCacheFileInfo(meta);

    List<TransactionsOfTable> transactionsOfTables =
        ServiceContainer.getFileInfoCacheService().getTxExcludeOptimize(tableIdentifier);
    Assert.assertEquals(2, transactionsOfTables.size());
    Assert.assertEquals(snapshotId1, transactionsOfTables.get(0).getTransactionId());
    Assert.assertEquals(snapshotId, transactionsOfTables.get(1).getTransactionId());

    List<DataFileInfo> dataFileInfos = ServiceContainer.getFileInfoCacheService().getOptimizeDatafiles(
        tableIdentifier,
        "base");
    Assert.assertEquals(dataFiles1.size(), dataFileInfos.size());
    Assert.assertEquals(dataFiles1.get(0).getPath(), dataFileInfos.get(0).getPath());
  }

  @Test
  public void testUnkeyedTableSyncFileCache() {
    com.netease.arctic.table.TableIdentifier tableId =
        com.netease.arctic.table.TableIdentifier.of(AMS_TEST_CATALOG_NAME, AMS_TEST_DB_NAME,
            "file_sync_test_unkeyed_table");
    UnkeyedTable fileSyncUnkeyedTable = catalog
        .newTableBuilder(
            tableId,
            TABLE_SCHEMA).withPartitionSpec(SPEC).create().asUnkeyedTable();
    testSyncFileCache(fileSyncUnkeyedTable, tableId);
  }

  @Test
  public void testKeyedTableSyncFileCache() {
    com.netease.arctic.table.TableIdentifier tableId =
        com.netease.arctic.table.TableIdentifier.of(AMS_TEST_CATALOG_NAME, AMS_TEST_DB_NAME,
            "file_sync_test_keyed_table");
    KeyedTable fileSyncKeyedTable = catalog
        .newTableBuilder(
            tableId,
            TABLE_SCHEMA).withPrimaryKeySpec(PRIMARY_KEY_SPEC).withPartitionSpec(SPEC).create().asKeyedTable();
    testSyncFileCache(fileSyncKeyedTable.baseTable(), tableId);
  }

  public void testSyncFileCache(UnkeyedTable fileSyncUnkeyedTable, com.netease.arctic.table.TableIdentifier tableId) {
    // Step1: append FILE_A FILE_B, sync file info cache, and check
    fileSyncUnkeyedTable.newFastAppend()
        .appendFile(FILE_A)
        .appendFile(FILE_B)
        .commit();
    List<DataFileInfo> commitDataFileInfos = ServiceContainer.getFileInfoCacheService().getOptimizeDatafiles(
        fileSyncUnkeyedTable.id().buildTableIdentifier(),
        "base");
    List<TransactionsOfTable> commitSnapInfos = ServiceContainer.getFileInfoCacheService().getTxExcludeOptimize(
        fileSyncUnkeyedTable.id().buildTableIdentifier());
    ServiceContainer.getFileInfoCacheService().deleteTableCache(tableId);
    List<DataFileInfo> cacheDataFileInfos = ServiceContainer.getFileInfoCacheService().getOptimizeDatafiles(
        fileSyncUnkeyedTable.id().buildTableIdentifier(),
        "base");
    Assert.assertEquals(0, cacheDataFileInfos.size());
    ServiceContainer.getFileInfoCacheService()
        .syncTableFileInfo(fileSyncUnkeyedTable.id().buildTableIdentifier(), "base");
    List<DataFileInfo> syncDataFileInfos1 = ServiceContainer.getFileInfoCacheService().getOptimizeDatafiles(
        fileSyncUnkeyedTable.id().buildTableIdentifier(),
        "base");
    List<TransactionsOfTable> syncSnapInfos = ServiceContainer.getFileInfoCacheService().getTxExcludeOptimize(
        fileSyncUnkeyedTable.id().buildTableIdentifier());
    Assert.assertEquals(commitDataFileInfos.size(), syncDataFileInfos1.size());
    for (DataFileInfo commitDataFileInfo : commitDataFileInfos) {
      boolean isCached = false;
      for (DataFileInfo syncDataFileInfo : syncDataFileInfos1) {
        if (commitDataFileInfo.getPath().equals(syncDataFileInfo.getPath())) {
          isCached = true;
          break;
        }
      }
      Assert.assertTrue(isCached);
    }
    Assert.assertEquals(commitSnapInfos.size(), syncSnapInfos.size());
    for (int i = 0; i < commitSnapInfos.size(); i++) {
      Assert.assertEquals(commitSnapInfos.get(i).getTransactionId(), syncSnapInfos.get(i).getTransactionId());
    }

    Snapshot snapshot1 = fileSyncUnkeyedTable.currentSnapshot();
    assertDataFilesWithSnapshot(fileSyncUnkeyedTable, syncDataFileInfos1, snapshot1);

    // Step2: overwrite FILE_A FILE_B with FILE_C, clean, sync file info cache, and check
    fileSyncUnkeyedTable.newOverwrite()
        .deleteFile(FILE_A)
        .deleteFile(FILE_B)
        .addFile(FILE_C)
        .commit();
    List<TransactionsOfTable> overwriteCommitSnapInfos =
        ServiceContainer.getFileInfoCacheService().getTxExcludeOptimize(
            fileSyncUnkeyedTable.id().buildTableIdentifier());
    ServiceContainer.getFileInfoCacheService().deleteTableCache(tableId);
    ServiceContainer.getFileInfoCacheService()
        .syncTableFileInfo(fileSyncUnkeyedTable.id().buildTableIdentifier(), "base");
    List<DataFileInfo> syncDataFileInfos2 = ServiceContainer.getFileInfoCacheService().getOptimizeDatafiles(
        fileSyncUnkeyedTable.id().buildTableIdentifier(),
        "base");
    List<TransactionsOfTable> overwriteSnapInfos = ServiceContainer.getFileInfoCacheService().getTxExcludeOptimize(
        fileSyncUnkeyedTable.id().buildTableIdentifier());
    Assert.assertEquals(1, syncDataFileInfos2.size());
    Assert.assertEquals(FILE_C.path(), syncDataFileInfos2.get(0).getPath());
    Assert.assertEquals(1, overwriteSnapInfos.size());
    Assert.assertEquals(
        overwriteCommitSnapInfos.get(0).getTransactionId(),
        overwriteSnapInfos.get(0).getTransactionId());

    try {
      ServiceContainer.getFileInfoCacheService()
          .getOptimizeDatafilesWithSnapshot(fileSyncUnkeyedTable.id().buildTableIdentifier(), "base", snapshot1);
      Assert.fail("should not get file info with old snapshot, since it has been removed");
    } catch (Throwable t) {
      Assert.assertTrue(t instanceof IllegalArgumentException);
    }

    Snapshot snapshot2 = fileSyncUnkeyedTable.currentSnapshot();

    // Step3: append FILE_D, and check
    fileSyncUnkeyedTable.newFastAppend()
        .appendFile(FILE_D)
        .commit();
    List<DataFileInfo> syncDataFileInfos3 = ServiceContainer.getFileInfoCacheService().getOptimizeDatafiles(
        fileSyncUnkeyedTable.id().buildTableIdentifier(),
        "base");
    Assert.assertEquals(2, syncDataFileInfos3.size());
    assertDataFilesWithSnapshot(fileSyncUnkeyedTable, syncDataFileInfos2, snapshot2);

    Snapshot snapshot3 = fileSyncUnkeyedTable.currentSnapshot();

    // Step4: overwrite FILE_C with FILE_A FILE_B, and check
    fileSyncUnkeyedTable.newOverwrite()
        .deleteFile(FILE_C)
        .addFile(FILE_A)
        .addFile(FILE_B)
        .commit();
    List<DataFileInfo> syncDataFileInfos4 = ServiceContainer.getFileInfoCacheService().getOptimizeDatafiles(
        fileSyncUnkeyedTable.id().buildTableIdentifier(),
        "base");
    Assert.assertEquals(3, syncDataFileInfos4.size());
    assertDataFilesWithSnapshot(fileSyncUnkeyedTable, syncDataFileInfos2, snapshot2);

    assertDataFilesWithSnapshot(fileSyncUnkeyedTable, syncDataFileInfos3, snapshot3);

    Snapshot snapshot4 = fileSyncUnkeyedTable.currentSnapshot();
    assertDataFilesWithSnapshot(fileSyncUnkeyedTable, syncDataFileInfos4, snapshot4);

  }

  private void assertDataFilesWithSnapshot(UnkeyedTable fileSyncUnkeyedTable, List<DataFileInfo> syncDataFileInfos,
                                           Snapshot snapshot) {
    List<DataFileInfo> datafilesWithSnapshot = ServiceContainer.getFileInfoCacheService()
        .getOptimizeDatafilesWithSnapshot(fileSyncUnkeyedTable.id().buildTableIdentifier(), "base", snapshot);
    Assert.assertTrue(CollectionUtils.isEqualCollection(syncDataFileInfos, datafilesWithSnapshot));
  }

  @Test
  public void testIcebergTable() {
    Map<String, String> pro = new HashMap<>();
    pro.put(org.apache.iceberg.TableProperties.FORMAT_VERSION, "2");
    ArcticTable table = TableUtil.createIcebergTable("test_iceberg_file_cache", TABLE_SCHEMA, pro, TableTestBase.SPEC);
    DeleteFile posDelete = FileMetadata.deleteFileBuilder(table.spec())
        .ofPositionDeletes()
        .withPath("/path/to/data-unpartitioned-pos-deletes.parquet")
        .withFileSizeInBytes(10)
        .withPartitionPath("op_time_day=2022-01-01")
        .withRecordCount(1)
        .build();
    DeleteFile eqDelete = FileMetadata.deleteFileBuilder(table.spec())
        .ofEqualityDeletes()
        .withPath("/path/to/data-unpartitioned-eq-deletes.parquet")
        .withFileSizeInBytes(10)
        .withPartitionPath("op_time_day=2022-01-01")
        .withRecordCount(1)
        .build();
    table.asUnkeyedTable().newFastAppend()
        .appendFile(FILE_A)
        .appendFile(FILE_B)
        .commit();
    table.asUnkeyedTable().newRewrite().rewriteFiles(Sets.newHashSet(FILE_B), Sets.newHashSet(FILE_D))
        .commit();
    table.asUnkeyedTable().newOverwrite()
        .deleteFile(FILE_A)
        .addFile(FILE_C)
        .commit();
    table.asUnkeyedTable().newRowDelta().addDeletes(eqDelete).addDeletes(posDelete).commit();
    List<TransactionsOfTable> transactionsOfTables =
        ServiceContainer.getFileInfoCacheService().getTxExcludeOptimize(table.id().buildTableIdentifier());
    Collections.reverse(transactionsOfTables);
    Assert.assertEquals(3, transactionsOfTables.size());

    Assert.assertEquals(ServiceContainer.getFileInfoCacheService().getDatafilesInfo(
        table.id().buildTableIdentifier(),
        transactionsOfTables.get(0).getTransactionId()).get(0).getPath(), FILE_A.path().toString());
    Assert.assertEquals(ServiceContainer.getFileInfoCacheService().getDatafilesInfo(
        table.id().buildTableIdentifier(),
        transactionsOfTables.get(0).getTransactionId()).get(0).getOperation(), "add");
    Assert.assertEquals(ServiceContainer.getFileInfoCacheService().getDatafilesInfo(
        table.id().buildTableIdentifier(),
        transactionsOfTables.get(0).getTransactionId()).get(1).getPath(), FILE_B.path().toString());
    Assert.assertEquals(ServiceContainer.getFileInfoCacheService().getDatafilesInfo(
        table.id().buildTableIdentifier(),
        transactionsOfTables.get(0).getTransactionId()).get(1).getOperation(), "add");

    Assert.assertEquals(ServiceContainer.getFileInfoCacheService().getDatafilesInfo(
        table.id().buildTableIdentifier(),
        transactionsOfTables.get(1).getTransactionId()).get(0).getPath(), FILE_C.path().toString());
    Assert.assertEquals(ServiceContainer.getFileInfoCacheService().getDatafilesInfo(
        table.id().buildTableIdentifier(),
        transactionsOfTables.get(1).getTransactionId()).get(0).getOperation(), "add");
    Assert.assertEquals(ServiceContainer.getFileInfoCacheService().getDatafilesInfo(
        table.id().buildTableIdentifier(),
        transactionsOfTables.get(1).getTransactionId()).get(1).getPath(), FILE_A.path().toString());
    Assert.assertEquals(ServiceContainer.getFileInfoCacheService().getDatafilesInfo(
        table.id().buildTableIdentifier(),
        transactionsOfTables.get(1).getTransactionId()).get(1).getOperation(), "remove");
    Assert.assertEquals(transactionsOfTables.get(1).getFileCount(), 2);
    Assert.assertEquals(transactionsOfTables.get(1).getFileSize(), 20);

    Assert.assertEquals(ServiceContainer.getFileInfoCacheService().getDatafilesInfo(
        table.id().buildTableIdentifier(),
        transactionsOfTables.get(2).getTransactionId()).get(0).getPath(), "/path/to/data-unpartitioned-eq-deletes" +
        ".parquet");
    Assert.assertEquals(ServiceContainer.getFileInfoCacheService().getDatafilesInfo(
        table.id().buildTableIdentifier(),
        transactionsOfTables.get(2).getTransactionId()).get(0).getType(), "eq-deletes");
    Assert.assertEquals(
        ServiceContainer.getFileInfoCacheService().getDatafilesInfo(
            table.id().buildTableIdentifier(),
            transactionsOfTables.get(2).getTransactionId()).get(1).getPath(),
        "/path/to/data-unpartitioned-pos-deletes.parquet");
    Assert.assertEquals(ServiceContainer.getFileInfoCacheService().getDatafilesInfo(
        table.id().buildTableIdentifier(),
        transactionsOfTables.get(2).getTransactionId()).get(1).getType(), "pos-deletes");
  }

  @Test
  public void testGetChangeTableTTLDataFiles() throws MetaException {
    TableIdentifier tableIdentifier = new TableIdentifier(AMS_TEST_CATALOG_NAME, "test", "test1");
    TableCommitMeta meta = new TableCommitMeta();
    meta.setAction("append");
    long commitTime = System.currentTimeMillis();
    meta.setCommitTime(commitTime);
    meta.setCommitMetaProducer(CommitMetaProducer.INGESTION);
    meta.setTableIdentifier(tableIdentifier);
    List<TableChange> changes = new ArrayList<>();
    TableChange change = new TableChange();
    change.setParentSnapshotId(-1);
    change.setInnerTable("change");
    List<DataFile> dataFiles = new ArrayList<>();
    DataFile dataFile = genDatafile();
    dataFiles.add(dataFile);
    change.setAddFiles(dataFiles);
    long snapshotId = 1L;
    long snapshotSequence = 20L;
    change.setSnapshotId(snapshotId);
    change.setSnapshotSequence(snapshotSequence);
    TableChange change1 = new TableChange();
    change1.setParentSnapshotId(snapshotId);
    change1.setInnerTable("change");
    List<DataFile> dataFiles1 = new ArrayList<>();
    DataFile dataFile1 = genDatafile();
    dataFiles1.add(dataFile1);
    change1.setAddFiles(dataFiles1);
    long snapshotId1 = 2L;
    long snapshotSequence1 = 30L;
    change1.setSnapshotId(snapshotId1);
    change1.setSnapshotSequence(snapshotSequence1);

    changes.add(change);
    changes.add(change1);
    meta.setChanges(changes);
    Map<String, String> properties = new HashMap<>();
    properties.put(TableProperties.TABLE_EVENT_TIME_FIELD, "eventTime");
    meta.setProperties(properties);
    ServiceContainer.getFileInfoCacheService().commitCacheFileInfo(meta);

    List<DataFileInfo> changeFiles =
        ServiceContainer.getFileInfoCacheService().getOptimizeDatafiles(tableIdentifier, "change");

    assertDataFile(dataFile, commitTime, snapshotSequence, changeFiles.get(0));
    assertDataFile(dataFile1, commitTime, snapshotSequence1, changeFiles.get(1));

    List<DataFileInfo> emptyDataFiles =
        ServiceContainer.getFileInfoCacheService().getChangeTableTTLDataFiles(tableIdentifier, commitTime - 1);
    Assert.assertEquals(0, emptyDataFiles.size());

    List<DataFileInfo> ttlDataFiles =
        ServiceContainer.getFileInfoCacheService().getChangeTableTTLDataFiles(tableIdentifier, commitTime);
    Assert.assertEquals(2, ttlDataFiles.size());
    assertDataFile(dataFile, commitTime, snapshotSequence, ttlDataFiles.get(0));
    assertDataFile(dataFile1, commitTime, snapshotSequence1, ttlDataFiles.get(1));
  }

  private void assertDataFile(DataFile file, long commitTime, long sequence, DataFileInfo dataFileInfo) {
    Assert.assertEquals(file.getPath().toString(), dataFileInfo.getPath());
    Assert.assertEquals("pt=2022-08-31", dataFileInfo.getPartition());
    Assert.assertEquals(file.getIndex(), dataFileInfo.getIndex());
    Assert.assertEquals(file.getMask(), dataFileInfo.getMask());
    Assert.assertEquals(sequence, dataFileInfo.getSequence());
    Assert.assertEquals(file.getFileSize(), dataFileInfo.getSize());
    Assert.assertEquals(file.getRecordCount(), dataFileInfo.getRecordCount());
    Assert.assertEquals(commitTime, dataFileInfo.getCommitTime());
    Assert.assertEquals(file.getFileType(), dataFileInfo.getType());
    Assert.assertEquals(file.getSpecId(), dataFileInfo.getSpecId());
  }

  private DataFile genDatafile() {
    DataFile dataFile = new DataFile();
    dataFile.setFileSize(1);
    dataFile.setFileType("INSERT_FILE");
    dataFile.setIndex(0);
    dataFile.setMask(0);
    dataFile.setPath("/tmp/test" + random.nextInt() + ".file");
    Map<String, ByteBuffer> upperBounds = new HashMap<>();
    byte[] bytes = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(1000L).array();
    upperBounds.put("eventTime", ByteBuffer.wrap(bytes));
    dataFile.setUpperBounds(upperBounds);
    PartitionFieldData partitionFieldData = new PartitionFieldData();
    partitionFieldData.setName("pt");
    partitionFieldData.setValue("2022-08-31");
    List<PartitionFieldData> partitionFieldDataList = new ArrayList<>();
    partitionFieldDataList.add(partitionFieldData);
    dataFile.setPartition(partitionFieldDataList);
    return dataFile;
  }
}
