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

import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.TableTestHelper;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.catalog.CatalogTestHelper;
import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.op.UpdatePartitionProperties;
import com.netease.arctic.server.dashboard.utils.AmsUtil;
import com.netease.arctic.server.optimizing.OptimizingStatus;
import com.netease.arctic.server.table.ServerTableIdentifier;
import com.netease.arctic.server.table.TableRuntime;
import com.netease.arctic.server.utils.IcebergTableUtil;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFiles;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.relocated.com.google.common.collect.Iterables;
import org.apache.iceberg.relocated.com.google.common.collect.Iterators;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@RunWith(Parameterized.class)
public class TestSnapshotExpire extends ExecutorTestBase {

  private final List<DataFile> changeTableFiles = new ArrayList<>();

  @Parameterized.Parameters(name = "{0}, {1}")
  public static Object[] parameters() {
    return new Object[][]{
        {new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
            new BasicTableTestHelper(true, true)},
        {new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
            new BasicTableTestHelper(true, false)},
        {new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
            new BasicTableTestHelper(false, true)},
        {new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
            new BasicTableTestHelper(false, false)}};
  }

  public TestSnapshotExpire(CatalogTestHelper catalogTestHelper, TableTestHelper tableTestHelper) {
    super(catalogTestHelper, tableTestHelper);
  }

  @Test
  public void testDeleteChangeFiles() throws Exception {
    Assume.assumeTrue(isKeyedTable());
    Assume.assumeTrue(isPartitionedTable());
    KeyedTable testKeyedTable = getArcticTable().asKeyedTable();
    List<DataFile> s1Files = insertChangeDataFiles(testKeyedTable, 1);
    List<StructLike> partitions =
        new ArrayList<>(s1Files.stream().collect(Collectors.groupingBy(ContentFile::partition)).keySet());
    Assert.assertEquals(2, partitions.size());

    UpdatePartitionProperties updateProperties = testKeyedTable.baseTable().updatePartitionProperties(null);
    updateProperties.set(partitions.get(0), TableProperties.PARTITION_OPTIMIZED_SEQUENCE, "3");
    updateProperties.set(partitions.get(1), TableProperties.PARTITION_OPTIMIZED_SEQUENCE, "0");
    updateProperties.commit();
    List<DataFile> existedDataFiles = new ArrayList<>();
    try (CloseableIterable<FileScanTask> fileScanTasks = testKeyedTable.changeTable().newScan().planFiles()) {
      fileScanTasks.forEach(fileScanTask -> existedDataFiles.add(fileScanTask.file()));
    }
    Assert.assertEquals(4, existedDataFiles.size());

    SnapshotsExpiringExecutor.deleteChangeFile(
        testKeyedTable, changeTableFiles, testKeyedTable.changeTable().currentSnapshot().sequenceNumber());
    Set<String> currentDataFiles = new HashSet<>();
    try (CloseableIterable<FileScanTask> fileScanTasks = testKeyedTable.changeTable().newScan().planFiles()) {
      fileScanTasks.forEach(fileScanTask -> currentDataFiles.add(fileScanTask.file().path().toString()));
    }
    Set<String> expectedDataFiles = existedDataFiles.stream().filter(
        file -> file.partition().equals(partitions.get(1))).map(f -> f.path().toString()).collect(Collectors.toSet());
    Assert.assertEquals(expectedDataFiles, currentDataFiles);
    changeTableFiles.forEach(file -> Assert.assertTrue(testKeyedTable.io().exists(file.path().toString())));
  }

  @Test
  public void testExpireChangeTableFiles() throws Exception {
    Assume.assumeTrue(isKeyedTable());
    KeyedTable testKeyedTable = getArcticTable().asKeyedTable();
    List<DataFile> s1Files = insertChangeDataFiles(testKeyedTable, 1);
    List<StructLike> partitions =
        new ArrayList<>(s1Files.stream().collect(Collectors.groupingBy(ContentFile::partition)).keySet());
    if (isPartitionedTable()) {
      Assert.assertEquals(2, partitions.size());
    } else {
      Assert.assertEquals(1, partitions.size());
    }

    UpdatePartitionProperties updateProperties = testKeyedTable.baseTable().updatePartitionProperties(null);
    updateProperties.set(partitions.get(0), TableProperties.PARTITION_OPTIMIZED_SEQUENCE, "3");
    if (isPartitionedTable()) {
      updateProperties.set(partitions.get(1), TableProperties.PARTITION_OPTIMIZED_SEQUENCE, "1");
    }
    updateProperties.commit();
    s1Files.forEach(file -> Assert.assertTrue(testKeyedTable.io().exists(file.path().toString())));
    SnapshotsExpiringExecutor.deleteChangeFile(
        testKeyedTable, changeTableFiles, testKeyedTable.changeTable().currentSnapshot().sequenceNumber());
    Assert.assertEquals(2, Iterables.size(testKeyedTable.changeTable().snapshots()));
    List<DataFile> existedDataFiles = new ArrayList<>();
    try (CloseableIterable<FileScanTask> fileScanTasks = testKeyedTable.changeTable().newScan().planFiles()) {
      fileScanTasks.forEach(fileScanTask -> existedDataFiles.add(fileScanTask.file()));
    }
    Assert.assertEquals(0, existedDataFiles.size());

    insertChangeDataFiles(testKeyedTable, 2);
    Snapshot expectedSnapshot = testKeyedTable.changeTable().currentSnapshot();
    SnapshotsExpiringExecutor.expireSnapshots(
        testKeyedTable.changeTable(), System.currentTimeMillis(), new HashSet<>());
    Assert.assertEquals(1, Iterables.size(testKeyedTable.changeTable().snapshots()));
    Assert.assertEquals(expectedSnapshot, testKeyedTable.changeTable().snapshots().iterator().next());
    s1Files.forEach(file -> Assert.assertFalse(testKeyedTable.io().exists(file.path().toString())));
  }

  @Test
  public void testExpiredChangeTableFilesInBase() {
    Assume.assumeTrue(isKeyedTable());
    Assume.assumeTrue(isPartitionedTable());
    KeyedTable testKeyedTable = getArcticTable().asKeyedTable();
    List<DataFile> s1Files = insertChangeDataFiles(testKeyedTable, 1);
    testKeyedTable.baseTable().newAppend().appendFile(s1Files.get(0)).commit();
    List<StructLike> partitions =
        new ArrayList<>(s1Files.stream().collect(Collectors.groupingBy(ContentFile::partition)).keySet());
    Assert.assertEquals(2, partitions.size());

    UpdatePartitionProperties updateProperties = testKeyedTable.baseTable().updatePartitionProperties(null);
    updateProperties.set(partitions.get(0), TableProperties.PARTITION_OPTIMIZED_SEQUENCE, "3");
    updateProperties.set(partitions.get(1), TableProperties.PARTITION_OPTIMIZED_SEQUENCE, "1");
    updateProperties.commit();
    Assert.assertTrue(testKeyedTable.io().exists((String) s1Files.get(0).path()));
    SnapshotsExpiringExecutor.deleteChangeFile(
        testKeyedTable, changeTableFiles, testKeyedTable.changeTable().currentSnapshot().sequenceNumber());
    Assert.assertEquals(2, Iterables.size(testKeyedTable.changeTable().snapshots()));

    Set<String> exclude = IcebergTableUtil.getAllContentFilePath(testKeyedTable.baseTable());
    insertChangeDataFiles(testKeyedTable, 2);
    Snapshot expectedSnapshot = testKeyedTable.changeTable().currentSnapshot();
    SnapshotsExpiringExecutor.expireSnapshots(testKeyedTable.changeTable(), System.currentTimeMillis(), exclude);

    Assert.assertEquals(1, Iterables.size(testKeyedTable.changeTable().snapshots()));
    Assert.assertEquals(expectedSnapshot, testKeyedTable.changeTable().snapshots().iterator().next());
    Assert.assertTrue(testKeyedTable.io().exists(s1Files.get(0).path().toString()));
    Assert.assertFalse(testKeyedTable.io().exists(s1Files.get(1).path().toString()));
  }

  @Test
  public void testGetClosestExpiredFiles() {
    Assume.assumeTrue(isKeyedTable());
    KeyedTable testKeyedTable = getArcticTable().asKeyedTable();
    insertChangeDataFiles(testKeyedTable, 1);
    Snapshot firstSnapshot = testKeyedTable.changeTable().currentSnapshot();
    insertChangeDataFiles(testKeyedTable, 2);
    long secondCommitTime = testKeyedTable.changeTable().currentSnapshot().timestampMillis();
    testKeyedTable.changeTable().newAppend().commit();

    Set<DataFile> top8Files = new HashSet<>();
    testKeyedTable.changeTable().newScan().planFiles().forEach(task -> top8Files.add(task.file()));
    Assert.assertEquals(8, top8Files.size());
    Assert.assertEquals(3, Iterables.size(testKeyedTable.changeTable().snapshots()));

    long thirdCommitTime = testKeyedTable.changeTable().currentSnapshot().timestampMillis();
    Snapshot thirdSnapshot = testKeyedTable.changeTable().currentSnapshot();

    insertChangeDataFiles(testKeyedTable, 3);
    Assert.assertEquals(12, Iterables.size(testKeyedTable.changeTable().newScan().planFiles()));
    Assert.assertEquals(4, Iterables.size(testKeyedTable.changeTable().snapshots()));

    Snapshot closestExpireSnapshot = SnapshotsExpiringExecutor.getClosestExpireSnapshot(
        testKeyedTable.changeTable(), thirdCommitTime);
    Snapshot closestExpireSnapshot2 = SnapshotsExpiringExecutor.getClosestExpireSnapshot(
        testKeyedTable.changeTable(), thirdCommitTime + 1);
    Snapshot closestExpireSnapshot3 = SnapshotsExpiringExecutor.getClosestExpireSnapshot(
        testKeyedTable.changeTable(), secondCommitTime - 1);
    Assert.assertEquals(thirdSnapshot, closestExpireSnapshot);
    Assert.assertEquals(thirdSnapshot, closestExpireSnapshot2);
    Assert.assertEquals(firstSnapshot, closestExpireSnapshot3);

    Set<CharSequence> closestFilesPath = new HashSet<>(SnapshotsExpiringExecutor.getClosestExpireDataFiles(
        testKeyedTable.changeTable(), closestExpireSnapshot))
        .stream().map(ContentFile::path).collect(Collectors.toSet());
    Set<CharSequence> top8FilesPath = top8Files.stream().map(ContentFile::path).collect(Collectors.toSet());

    Assert.assertTrue(top8FilesPath.equals(closestFilesPath));
  }

  @Test
  public void testNotExpireFlinkLatestCommit4ChangeTable() {
    Assume.assumeTrue(isKeyedTable());
    KeyedTable testKeyedTable = getArcticTable().asKeyedTable();
    insertChangeDataFiles(testKeyedTable, 1);
    insertChangeDataFiles(testKeyedTable, 2);
    Assert.assertEquals(Long.MAX_VALUE,
        SnapshotsExpiringExecutor.fetchLatestFlinkCommittedSnapshotTime(testKeyedTable.changeTable()));

    AppendFiles appendFiles = testKeyedTable.changeTable().newAppend();
    appendFiles.set(SnapshotsExpiringExecutor.FLINK_MAX_COMMITTED_CHECKPOINT_ID, "100");
    appendFiles.commit();
    long checkpointTime = testKeyedTable.changeTable().currentSnapshot().timestampMillis();
    Assert.assertEquals(checkpointTime,
        SnapshotsExpiringExecutor.fetchLatestFlinkCommittedSnapshotTime(testKeyedTable.changeTable()));

    AppendFiles appendFiles2 = testKeyedTable.changeTable().newAppend();
    appendFiles2.set(SnapshotsExpiringExecutor.FLINK_MAX_COMMITTED_CHECKPOINT_ID, "101");
    appendFiles2.commit();
    Snapshot checkpointTime2Snapshot = testKeyedTable.changeTable().currentSnapshot();
    Assert.assertEquals(checkpointTime2Snapshot.timestampMillis(),
        SnapshotsExpiringExecutor.fetchLatestFlinkCommittedSnapshotTime(testKeyedTable.changeTable()));

    insertChangeDataFiles(testKeyedTable, 2);
    Snapshot lastSnapshot = testKeyedTable.changeTable().currentSnapshot();
    Assert.assertEquals(checkpointTime2Snapshot.timestampMillis(),
        SnapshotsExpiringExecutor.fetchLatestFlinkCommittedSnapshotTime(testKeyedTable.changeTable()));

    testKeyedTable.updateProperties().set(TableProperties.CHANGE_SNAPSHOT_KEEP_MINUTES, "0").commit();
    testKeyedTable.updateProperties().set(TableProperties.CHANGE_DATA_TTL, "0").commit();
    TableRuntime tableRuntime = Mockito.mock(TableRuntime.class);
    Mockito.when(tableRuntime.getTableIdentifier()).thenReturn(
        ServerTableIdentifier.of(AmsUtil.toTableIdentifier(testKeyedTable.id())));
    Mockito.when(tableRuntime.getOptimizingStatus()).thenReturn(OptimizingStatus.IDLE);

    Assert.assertEquals(5, Iterables.size(testKeyedTable.changeTable().snapshots()));
    SnapshotsExpiringExecutor.expireArcticTable(
        testKeyedTable, tableRuntime);

    Assert.assertEquals(2, Iterables.size(testKeyedTable.changeTable().snapshots()));
    HashSet<Snapshot> expectedSnapshots = new HashSet<>();
    expectedSnapshots.add(checkpointTime2Snapshot);
    expectedSnapshots.add(lastSnapshot);
    Iterators.elementsEqual(expectedSnapshots.iterator(), testKeyedTable.changeTable().snapshots().iterator());
  }

  @Test
  public void testNotExpireFlinkLatestCommit4All() {
    UnkeyedTable table = isKeyedTable() ? getArcticTable().asKeyedTable().baseTable() :
        getArcticTable().asUnkeyedTable();
    writeAndCommitBaseStore(table);
    Assert.assertEquals(Long.MAX_VALUE,
        SnapshotsExpiringExecutor.fetchLatestFlinkCommittedSnapshotTime(table));

    AppendFiles appendFiles = table.newAppend();
    appendFiles.set(SnapshotsExpiringExecutor.FLINK_MAX_COMMITTED_CHECKPOINT_ID, "100");
    appendFiles.commit();
    long checkpointTime = table.currentSnapshot().timestampMillis();
    Assert.assertEquals(checkpointTime,
        SnapshotsExpiringExecutor.fetchLatestFlinkCommittedSnapshotTime(table));

    AppendFiles appendFiles2 = table.newAppend();
    appendFiles2.set(SnapshotsExpiringExecutor.FLINK_MAX_COMMITTED_CHECKPOINT_ID, "101");
    appendFiles2.commit();
    Snapshot checkpointTime2Snapshot = table.currentSnapshot();
    Assert.assertEquals(checkpointTime2Snapshot.timestampMillis(),
        SnapshotsExpiringExecutor.fetchLatestFlinkCommittedSnapshotTime(table));

    writeAndCommitBaseStore(table);
    Snapshot lastSnapshot = table.currentSnapshot();
    Assert.assertEquals(checkpointTime2Snapshot.timestampMillis(),
        SnapshotsExpiringExecutor.fetchLatestFlinkCommittedSnapshotTime(table));

    table.updateProperties().set(TableProperties.BASE_SNAPSHOT_KEEP_MINUTES, "0").commit();
    TableRuntime tableRuntime = Mockito.mock(TableRuntime.class);
    Mockito.when(tableRuntime.getTableIdentifier()).thenReturn(
        ServerTableIdentifier.of(AmsUtil.toTableIdentifier(table.id())));
    Mockito.when(tableRuntime.getOptimizingStatus()).thenReturn(OptimizingStatus.IDLE);

    Assert.assertEquals(4, Iterables.size(table.snapshots()));
    SnapshotsExpiringExecutor.expireArcticTable(
        table, tableRuntime);

    Assert.assertEquals(2, Iterables.size(table.snapshots()));
    HashSet<Snapshot> expectedSnapshots = new HashSet<>();
    expectedSnapshots.add(checkpointTime2Snapshot);
    expectedSnapshots.add(lastSnapshot);
    Iterators.elementsEqual(expectedSnapshots.iterator(), table.snapshots().iterator());
  }

  @Test
  public void testNotExpireOptimizeCommit4All() {
    UnkeyedTable table = isKeyedTable() ? getArcticTable().asKeyedTable().baseTable() :
        getArcticTable().asUnkeyedTable();
    table.newAppend().commit();
    table.newAppend().commit();
    table.updateProperties().set(TableProperties.BASE_SNAPSHOT_KEEP_MINUTES, "0").commit();

    TableRuntime tableRuntime = Mockito.mock(TableRuntime.class);
    Mockito.when(tableRuntime.getTableIdentifier()).thenReturn(
        ServerTableIdentifier.of(AmsUtil.toTableIdentifier(table.id())));
    Mockito.when(tableRuntime.getOptimizingStatus()).thenReturn(OptimizingStatus.IDLE);
    SnapshotsExpiringExecutor.expireArcticTable(table, tableRuntime);
    Assert.assertEquals(1, Iterables.size(table.snapshots()));

    table.newAppend().commit();

    // mock tableRuntime which has optimizing task not committed
    long optimizeSnapshotId = table.currentSnapshot().snapshotId();
    Mockito.when(tableRuntime.getOptimizingStatus()).thenReturn(OptimizingStatus.COMMITTING);
    Mockito.when(tableRuntime.getCurrentSnapshotId()).thenReturn(optimizeSnapshotId);
    HashSet<Snapshot> expectedSnapshots = new HashSet<>();
    expectedSnapshots.add(table.currentSnapshot());

    table.newAppend().commit();
    expectedSnapshots.add(table.currentSnapshot());
    table.newAppend().commit();
    expectedSnapshots.add(table.currentSnapshot());

    SnapshotsExpiringExecutor.expireArcticTable(table, tableRuntime);
    Assert.assertEquals(3, Iterables.size(table.snapshots()));
    Iterators.elementsEqual(expectedSnapshots.iterator(), table.snapshots().iterator());
  }

  @Test
  public void testExpireTableFiles4All() {
    UnkeyedTable table = isKeyedTable() ? getArcticTable().asKeyedTable().baseTable() :
        getArcticTable().asUnkeyedTable();
    List<DataFile> dataFiles = writeAndCommitBaseStore(table);

    DeleteFiles deleteFiles = table.newDelete();
    for (DataFile dataFile : dataFiles) {
      Assert.assertTrue(table.io().exists(dataFile.path().toString()));
      deleteFiles.deleteFile(dataFile);
    }
    deleteFiles.commit();

    List<DataFile> newDataFiles = writeAndCommitBaseStore(table);
    Assert.assertEquals(3, Iterables.size(table.snapshots()));
    SnapshotsExpiringExecutor.expireSnapshots(table, System.currentTimeMillis(),
        new HashSet<>());
    Assert.assertEquals(1, Iterables.size(table.snapshots()));

    dataFiles.forEach(file -> Assert.assertFalse(table.io().exists(file.path().toString())));
    newDataFiles.forEach(file -> Assert.assertTrue(table.io().exists(file.path().toString())));
  }

  private List<DataFile> insertChangeDataFiles(KeyedTable testKeyedTable, long transactionId) {
    List<DataFile> changeInsertFiles = writeAndCommitChangeStore(
        testKeyedTable, transactionId, ChangeAction.INSERT, createRecords(1, 100));
    changeTableFiles.addAll(changeInsertFiles);
    return changeInsertFiles;
  }
}
