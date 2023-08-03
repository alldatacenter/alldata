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

import com.netease.arctic.hive.utils.TableTypeUtil;
import com.netease.arctic.server.optimizing.OptimizingStatus;
import com.netease.arctic.server.table.TableManager;
import com.netease.arctic.server.table.TableRuntime;
import com.netease.arctic.server.utils.HiveLocationUtil;
import com.netease.arctic.server.utils.IcebergTableUtil;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.utils.CompatiblePropertyUtil;
import com.netease.arctic.utils.TableFileUtil;
import com.netease.arctic.utils.TablePropertyUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.hadoop.fs.Path;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFiles;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.relocated.com.google.common.collect.Streams;
import org.apache.iceberg.util.StructLikeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.apache.iceberg.relocated.com.google.common.primitives.Longs.min;

/**
 * Service for expiring tables periodically.
 */
public class SnapshotsExpiringExecutor extends BaseTableExecutor {
  private static final Logger LOG = LoggerFactory.getLogger(SnapshotsExpiringExecutor.class);

  public static final String FLINK_MAX_COMMITTED_CHECKPOINT_ID = "flink.max-committed-checkpoint-id";

  private static final int DATA_FILE_LIST_SPLIT = 3000;

  // 1 days
  private static final long INTERVAL = 60 * 60 * 1000L;

  public SnapshotsExpiringExecutor(TableManager tableRuntimes, int poolSize) {
    super(tableRuntimes, poolSize);
  }

  @Override
  protected long getNextExecutingTime(TableRuntime tableRuntime) {
    return INTERVAL;
  }

  @Override
  protected boolean enabled(TableRuntime tableRuntime) {
    return tableRuntime.getTableConfiguration().isExpireSnapshotEnabled();
  }

  @Override
  public void execute(TableRuntime tableRuntime) {
    try {
      ArcticTable arcticTable = loadTable(tableRuntime);
      boolean needClean = CompatiblePropertyUtil.propertyAsBoolean(
          arcticTable.properties(),
          TableProperties.ENABLE_TABLE_EXPIRE,
          TableProperties.ENABLE_TABLE_EXPIRE_DEFAULT);
      if (!needClean) {
        return;
      }

      expireArcticTable(arcticTable, tableRuntime);
    } catch (Throwable t) {
      LOG.error("unexpected expire error of table {} ", tableRuntime.getTableIdentifier(), t);
    }
  }

  public static void expireArcticTable(ArcticTable arcticTable, TableRuntime tableRuntime) {
    long startTime = System.currentTimeMillis();
    LOG.info("{} start expire", tableRuntime.getTableIdentifier());

    long changeDataTTL = CompatiblePropertyUtil.propertyAsLong(
        arcticTable.properties(),
        TableProperties.CHANGE_DATA_TTL,
        TableProperties.CHANGE_DATA_TTL_DEFAULT) * 60 * 1000;
    long baseSnapshotsKeepTime = CompatiblePropertyUtil.propertyAsLong(
        arcticTable.properties(),
        TableProperties.BASE_SNAPSHOT_KEEP_MINUTES,
        TableProperties.BASE_SNAPSHOT_KEEP_MINUTES_DEFAULT) * 60 * 1000;
    long changeSnapshotsKeepTime = CompatiblePropertyUtil.propertyAsLong(
        arcticTable.properties(),
        TableProperties.CHANGE_SNAPSHOT_KEEP_MINUTES,
        TableProperties.CHANGE_SNAPSHOT_KEEP_MINUTES_DEFAULT) * 60 * 1000;

    Set<String> hiveLocations = new HashSet<>();
    if (TableTypeUtil.isHive(arcticTable)) {
      hiveLocations = HiveLocationUtil.getHiveLocation(arcticTable);
    }

    if (arcticTable.isKeyedTable()) {
      KeyedTable keyedArcticTable = arcticTable.asKeyedTable();
      Set<String> finalHiveLocations = hiveLocations;
      keyedArcticTable.io().doAs(() -> {
        UnkeyedTable baseTable = keyedArcticTable.baseTable();
        UnkeyedTable changeTable = keyedArcticTable.changeTable();

        // getRuntime valid files in the change store which shouldn't physically delete when expire the snapshot
        // in the base store
        Set<String> baseExcludePaths = IcebergTableUtil.getAllContentFilePath(changeTable);
        baseExcludePaths.addAll(finalHiveLocations);
        long latestBaseFlinkCommitTime = fetchLatestFlinkCommittedSnapshotTime(baseTable);
        long optimizingSnapshotTime = fetchOptimizingSnapshotTime(baseTable, tableRuntime);
        long baseOlderThan = startTime - baseSnapshotsKeepTime;
        LOG.info("{} base table expire with latestFlinkCommitTime={}, optimizingSnapshotTime={}, olderThan={}",
            arcticTable.id(), latestBaseFlinkCommitTime, optimizingSnapshotTime, baseOlderThan);
        expireSnapshots(
            baseTable,
            min(latestBaseFlinkCommitTime, optimizingSnapshotTime, baseOlderThan),
            baseExcludePaths);
        long baseCleanedTime = System.currentTimeMillis();
        LOG.info("{} base expire cost {} ms", arcticTable.id(), baseCleanedTime - startTime);
        // delete ttl files
        Snapshot closestExpireSnapshot = getClosestExpireSnapshot(
            changeTable, System.currentTimeMillis() - changeDataTTL);
        if (closestExpireSnapshot != null) {
          List<DataFile> closestExpireDataFiles = getClosestExpireDataFiles(changeTable, closestExpireSnapshot);
          deleteChangeFile(keyedArcticTable, closestExpireDataFiles, closestExpireSnapshot.sequenceNumber());
        }

        // getRuntime valid files in the base store which shouldn't physically delete when expire the snapshot
        // in the change store
        Set<String> changeExclude = IcebergTableUtil.getAllContentFilePath(baseTable);
        changeExclude.addAll(finalHiveLocations);

        long latestChangeFlinkCommitTime = fetchLatestFlinkCommittedSnapshotTime(changeTable);
        // keep ttl <= snapshot keep time to avoid getting expired snapshots(but can't getRuntime) when deleting files.
        // if ttl > snapshot keep time, some files will not be deleted correctly.
        long changeOlderThan = changeDataTTL <= changeSnapshotsKeepTime ?
            startTime - changeSnapshotsKeepTime : startTime - changeDataTTL;
        LOG.info("{} change table expire with latestFlinkCommitTime={}, olderThan={}", arcticTable.id(),
            latestChangeFlinkCommitTime, changeOlderThan);
        expireSnapshots(
            changeTable,
            Math.min(latestChangeFlinkCommitTime, changeOlderThan),
            changeExclude);
        return null;
      });
      LOG.info("{} expire cost total {} ms", arcticTable.id(), System.currentTimeMillis() - startTime);
    } else {
      UnkeyedTable unKeyedArcticTable = arcticTable.asUnkeyedTable();
      long latestFlinkCommitTime = fetchLatestFlinkCommittedSnapshotTime(unKeyedArcticTable);
      long optimizingSnapshotTime = fetchOptimizingSnapshotTime(unKeyedArcticTable, tableRuntime);
      long olderThan = startTime - baseSnapshotsKeepTime;
      LOG.info("{} unKeyedTable expire with latestFlinkCommitTime={}, optimizingSnapshotTime={}, olderThan={}",
          arcticTable.id(), latestFlinkCommitTime, optimizingSnapshotTime, olderThan);
      expireSnapshots(
          unKeyedArcticTable,
          min(latestFlinkCommitTime, optimizingSnapshotTime, olderThan),
          hiveLocations);
      long baseCleanedTime = System.currentTimeMillis();
      LOG.info("{} unKeyedTable expire cost {} ms", arcticTable.id(), baseCleanedTime - startTime);
    }
  }

  /**
   * When committing a snapshot, Flink will write a checkpoint id into the snapshot summary.
   * The latest snapshot with checkpoint id should not be expired or the flink job can't recover from state.
   *
   * @param table -
   * @return commit time of snapshot with the latest flink checkpointId in summary
   */
  public static long fetchLatestFlinkCommittedSnapshotTime(UnkeyedTable table) {
    long latestCommitTime = Long.MAX_VALUE;
    for (Snapshot snapshot : table.snapshots()) {
      if (snapshot.summary().containsKey(FLINK_MAX_COMMITTED_CHECKPOINT_ID)) {
        latestCommitTime = snapshot.timestampMillis();
      }
    }
    return latestCommitTime;
  }

  /**
   * When optimizing tasks are not committed, the snapshot with which it planned should not be expired, since
   * it will use the snapshot to check conflict when committing.
   *
   * @param table - table
   * @return commit time of snapshot for optimizing
   */
  public static long fetchOptimizingSnapshotTime(UnkeyedTable table, TableRuntime tableRuntime) {
    if (tableRuntime.getOptimizingStatus() != OptimizingStatus.IDLE) {
      long currentSnapshotId = tableRuntime.getCurrentSnapshotId();
      for (Snapshot snapshot : table.snapshots()) {
        if (snapshot.snapshotId() == currentSnapshotId) {
          return snapshot.timestampMillis();
        }
      }
    }
    return Long.MAX_VALUE;
  }

  public static void expireSnapshots(
      UnkeyedTable arcticInternalTable,
      long olderThan,
      Set<String> exclude) {
    LOG.debug("start expire snapshots older than {}, the exclude is {}", olderThan, exclude);
    final AtomicInteger toDeleteFiles = new AtomicInteger(0);
    final AtomicInteger deleteFiles = new AtomicInteger(0);
    Set<String> parentDirectory = new HashSet<>();
    arcticInternalTable.expireSnapshots()
        .retainLast(1)
        .expireOlderThan(olderThan)
        .deleteWith(file -> {
          try {
            String filePath = TableFileUtil.getUriPath(file);
            if (!exclude.contains(filePath) && !exclude.contains(new Path(filePath).getParent().toString())) {
              arcticInternalTable.io().deleteFile(file);
            }
            parentDirectory.add(new Path(file).getParent().toString());
            deleteFiles.incrementAndGet();
          } catch (Throwable t) {
            LOG.warn("failed to delete file " + file, t);
          } finally {
            toDeleteFiles.incrementAndGet();
          }
        })
        .cleanExpiredFiles(true)
        .commit();
    if (arcticInternalTable.io().supportFileSystemOperations()) {
      parentDirectory.forEach(parent -> TableFileUtil.deleteEmptyDirectory(arcticInternalTable.io(), parent, exclude));
    }
    LOG.info("to delete {} files, success delete {} files", toDeleteFiles.get(), deleteFiles.get());
  }

  public static Snapshot getClosestExpireSnapshot(UnkeyedTable changeTable, long ttl) {
    if (changeTable.snapshots() == null) {
      return null;
    }
    return Streams.stream(changeTable.snapshots())
        .filter(snapshot -> snapshot.timestampMillis() <= ttl)
        .min(Comparator.comparingLong(snapshot -> Math.abs(ttl - snapshot.timestampMillis())))
        .orElse(null);
  }

  public static List<DataFile> getClosestExpireDataFiles(UnkeyedTable changeTable, Snapshot closestExpireSnapshot) {
    List<DataFile> changeTTLDataFiles = new ArrayList<>();
    long recentExpireSnapshotId = closestExpireSnapshot.snapshotId();
    try (CloseableIterable<FileScanTask> fileScanTasks = changeTable.newScan()
        .useSnapshot(recentExpireSnapshotId)
        .planFiles()) {
      fileScanTasks.forEach(fileScanTask -> changeTTLDataFiles.add(fileScanTask.file()));
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to close table scan of " + changeTable.name(), e);
    }
    return changeTTLDataFiles;
  }

  public static void deleteChangeFile(KeyedTable keyedTable, List<DataFile> changeDataFiles, long sequenceNumber) {
    if (CollectionUtils.isEmpty(changeDataFiles)) {
      return;
    }

    StructLikeMap<Long> partitionMaxTransactionId = TablePropertyUtil.getPartitionOptimizedSequence(keyedTable);
    if (MapUtils.isEmpty(partitionMaxTransactionId)) {
      LOG.info("table {} not contains max transaction id", keyedTable.id());
      return;
    }

    Map<String, List<DataFile>> partitionDataFileMap = changeDataFiles.stream()
        .collect(Collectors.groupingBy(changeDataFile ->
            keyedTable.spec().partitionToPath(changeDataFile.partition()), Collectors.toList()));

    List<DataFile> changeDeleteFiles = new ArrayList<>();
    if (keyedTable.baseTable().spec().isUnpartitioned()) {
      List<DataFile> partitionDataFiles =
          partitionDataFileMap.get(keyedTable.spec().partitionToPath(changeDataFiles.get(0).partition()));

      Long optimizedSequence = partitionMaxTransactionId.get(TablePropertyUtil.EMPTY_STRUCT);
      if (CollectionUtils.isNotEmpty(partitionDataFiles)) {
        changeDeleteFiles.addAll(partitionDataFiles.stream()
            .filter(dataFile -> sequenceNumber <= optimizedSequence)
            .collect(Collectors.toList()));
      }
    } else {
      partitionMaxTransactionId.forEach((key, value) -> {
        List<DataFile> partitionDataFiles =
            partitionDataFileMap.get(keyedTable.baseTable().spec().partitionToPath(key));

        if (CollectionUtils.isNotEmpty(partitionDataFiles)) {
          changeDeleteFiles.addAll(partitionDataFiles.stream()
              .filter(dataFile -> sequenceNumber <= value)
              .collect(Collectors.toList()));
        }
      });
    }
    tryClearChangeFiles(keyedTable, changeDeleteFiles);
  }

  public static void tryClearChangeFiles(KeyedTable keyedTable, List<DataFile> changeFiles) {
    try {
      if (keyedTable.primaryKeySpec().primaryKeyExisted()) {
        for (int startIndex = 0; startIndex < changeFiles.size(); startIndex += DATA_FILE_LIST_SPLIT) {
          int end = Math.min(startIndex + DATA_FILE_LIST_SPLIT, changeFiles.size());
          List<DataFile> tableFiles = changeFiles.subList(startIndex, end);
          LOG.info("{} delete {} change files", keyedTable.id(), tableFiles.size());
          if (!tableFiles.isEmpty()) {
            DeleteFiles changeDelete = keyedTable.changeTable().newDelete();
            changeFiles.forEach(changeDelete::deleteFile);
            changeDelete.commit();
          }
          LOG.info("{} change committed, delete {} files, complete {}/{}", keyedTable.id(),
              tableFiles.size(), end, changeFiles.size());
        }
      }
    } catch (Throwable t) {
      LOG.error(keyedTable.id() + " failed to delete change files, ignore", t);
    }
  }
}
