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

package com.netease.arctic.server.dashboard.utils;

import com.netease.arctic.ams.api.Constants;
import com.netease.arctic.server.dashboard.model.FilesStatistics;
import com.netease.arctic.server.dashboard.model.SnapshotInfo;
import com.netease.arctic.server.dashboard.model.TableStatistics;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.SnapshotSummary;
import org.apache.iceberg.Table;
import org.apache.iceberg.exceptions.NoSuchTableException;
import org.apache.iceberg.util.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class TableStatCollector {
  private static final Logger LOG = LoggerFactory.getLogger(TableStatCollector.class);

  @Nonnull
  public static TableStatistics collectChangeTableInfo(KeyedTable table) {
    try {
      UnkeyedTable internalTable = null;
      try {
        internalTable = table.changeTable();
      } catch (NoSuchTableException e) {
        LOG.warn("No related hive table found. " + table.id(), e);
      }
      TableStatistics tableInfo = new TableStatistics();
      fillTableStatistics(tableInfo, internalTable, table);
      return tableInfo;
    } catch (Exception e) {
      LOG.error("failed to collect change table info of " + table.id(), e);
      return initEmptyTableStatistics(new TableStatistics(), table.id());
    }
  }

  @Nonnull
  public static TableStatistics collectBaseTableInfo(KeyedTable table) {
    try {
      LOG.info("start collect {} table info of {}", Constants.INNER_TABLE_BASE, table.id());
      UnkeyedTable internalTable = null;
      try {
        internalTable = table.baseTable();
      } catch (org.apache.iceberg.exceptions.NoSuchTableException e) {
        LOG.warn("No related hive table found. " + table.id(), e);
      }
      TableStatistics tableInfo = new TableStatistics();
      fillTableStatistics(tableInfo, internalTable, table);
      return tableInfo;
    } catch (Exception e) {
      LOG.error("failed to collect base table info of " + table.id(), e);
      return initEmptyTableStatistics(new TableStatistics(), table.id());
    }
  }

  public static TableStatistics union(
      TableStatistics changeTableInfo,
      TableStatistics baseTableInfo) {
    if (baseTableInfo == null && changeTableInfo == null) {
      return null;
    }
    if (baseTableInfo == null) {
      return new TableStatistics(changeTableInfo);
    }
    if (changeTableInfo == null) {
      return new TableStatistics(baseTableInfo);
    }
    TableStatistics overview = new TableStatistics();
    Map<String, String> baseSummary = baseTableInfo.getSummary();
    Map<String, String> changeSummary = changeTableInfo.getSummary();
    Map<String, String> summary = new HashMap<>();
    PropertiesUtil.putNotNullProperties(summary, "visibleTime", changeSummary.get("visibleTime"));
    overview.setTableIdentifier(baseTableInfo.getTableIdentifier());
    PropertiesUtil.putNotNullProperties(summary, "snapshotCnt", baseSummary.get("snapshotCnt"));
    PropertiesUtil.putNotNullProperties(summary, "firstSnapshotCommitTime", baseSummary.get("firstSnapshotCommitTime"));
    overview.setSummary(summary);
    FilesStatistics changeFs = changeTableInfo.getTotalFilesStat();
    FilesStatistics baseFs = baseTableInfo.getTotalFilesStat();

    overview.setTotalFilesStat(new FilesStatistics.Builder()
        .addFilesStatistics(changeFs)
        .addFilesStatistics(baseFs)
        .build());
    return overview;
  }

  public static SnapshotInfo buildBaseTableSnapshotInfo(Table baseTable) {
    Snapshot currentSnapshot = baseTable.currentSnapshot();
    SnapshotInfo snapshotInfo = new SnapshotInfo();
    if (currentSnapshot != null) {
      fillSnapshotInfo(snapshotInfo, currentSnapshot);
    }
    return snapshotInfo;
  }

  private static void fillSnapshotInfo(SnapshotInfo info, @Nonnull Snapshot snapshot) {
    info.setOperation(snapshot.operation());
    info.setAddedFiles(
        PropertyUtil.propertyAsInt(snapshot.summary(), SnapshotSummary.ADDED_FILES_PROP, 0));
    info.setAddedFilesSize(PropertyUtil
        .propertyAsLong(snapshot.summary(), SnapshotSummary.ADDED_FILE_SIZE_PROP, 0));
    info.setAddedRecords(PropertyUtil
        .propertyAsLong(snapshot.summary(), SnapshotSummary.ADDED_RECORDS_PROP, 0));
    info.setRemovedFilesSize(PropertyUtil
        .propertyAsLong(snapshot.summary(), SnapshotSummary.REMOVED_FILE_SIZE_PROP, 0));
    int removedFiles = PropertyUtil.propertyAsInt(snapshot.summary(), SnapshotSummary.DELETED_FILES_PROP, 0) +
        PropertyUtil.propertyAsInt(snapshot.summary(), SnapshotSummary.REMOVED_DELETE_FILES_PROP, 0);
    info.setRemovedFiles(removedFiles);
    info.setRemovedRecords(
        PropertyUtil.propertyAsLong(snapshot.summary(), SnapshotSummary.DELETED_RECORDS_PROP, 0));
    info.setTotalSize(
        PropertyUtil.propertyAsLong(snapshot.summary(), SnapshotSummary.TOTAL_FILE_SIZE_PROP, 0));
    info.setSnapshotId(snapshot.snapshotId());
    info.setTotalFiles(PropertyUtil
        .propertyAsInt(snapshot.summary(), SnapshotSummary.TOTAL_DATA_FILES_PROP, 0));
    info.setTotalRecords(PropertyUtil
        .propertyAsLong(snapshot.summary(), SnapshotSummary.TOTAL_RECORDS_PROP, 0));
  }

  private static void fillTableSnapshotInfo(TableStatistics tableStatistics, UnkeyedTable internalTable) {
    if (internalTable.currentSnapshot() == null) {
      return;
    }

    Snapshot snapshot = internalTable.currentSnapshot();
    Map<String, String> summary = tableStatistics.getSummary();
    PropertiesUtil.putNotNullProperties(summary, "operation", snapshot.summary().getOrDefault("operation", ""));
    PropertiesUtil.putNotNullProperties(summary, "addedFiles", String.valueOf(PropertyUtil
        .propertyAsInt(snapshot.summary(), SnapshotSummary.ADDED_FILES_PROP, 0)));
    PropertiesUtil.putNotNullProperties(summary, "addedFilesSize", String.valueOf(PropertyUtil
        .propertyAsLong(snapshot.summary(), SnapshotSummary.ADDED_FILE_SIZE_PROP, 0)));
    PropertiesUtil.putNotNullProperties(summary, "addedRecords", String.valueOf(PropertyUtil
        .propertyAsLong(snapshot.summary(), SnapshotSummary.ADDED_RECORDS_PROP, 0)));
    PropertiesUtil.putNotNullProperties(summary, "removedFilesSize", String.valueOf(PropertyUtil
        .propertyAsLong(snapshot.summary(), SnapshotSummary.REMOVED_FILE_SIZE_PROP, 0)));
    PropertiesUtil.putNotNullProperties(summary, "removedFiles", String.valueOf(PropertyUtil
        .propertyAsInt(snapshot.summary(), SnapshotSummary.DELETED_FILES_PROP, 0)));
    PropertiesUtil.putNotNullProperties(summary, "removedRecords", String.valueOf(PropertyUtil
        .propertyAsLong(snapshot.summary(), SnapshotSummary.DELETED_RECORDS_PROP, 0)));
    PropertiesUtil.putNotNullProperties(summary, "totalSize", String.valueOf(PropertyUtil
        .propertyAsLong(snapshot.summary(), SnapshotSummary.TOTAL_FILE_SIZE_PROP, 0)));
    PropertiesUtil.putNotNullProperties(summary, "snapshotId", String.valueOf(snapshot.snapshotId()));
    PropertiesUtil.putNotNullProperties(summary, "totalFiles", String.valueOf(PropertyUtil
        .propertyAsInt(snapshot.summary(), SnapshotSummary.TOTAL_DATA_FILES_PROP, 0)));
    PropertiesUtil.putNotNullProperties(summary, "totalRecords", String.valueOf(PropertyUtil
        .propertyAsLong(snapshot.summary(), SnapshotSummary.TOTAL_RECORDS_PROP, 0)));
  }

  public static void fillTableStatistics(
      TableStatistics tableStatistics,
      UnkeyedTable arcticInternalTable,
      ArcticTable table) {
    if (arcticInternalTable == null) {
      initEmptyTableStatistics(tableStatistics, table.id());
      return;
    }
    tableStatistics.setTableIdentifier(table.id());

    // count current snapshot file count&size
    FilesStatisticsBuilder totalFileStatBuilder = new FilesStatisticsBuilder();
    Snapshot currentSnapshot = arcticInternalTable.currentSnapshot();
    if (currentSnapshot != null) {
      long addedFilesSize =
          PropertyUtil
              .propertyAsLong(currentSnapshot.summary(), SnapshotSummary.TOTAL_FILE_SIZE_PROP, 0);
      int addedFilesCnt =
          PropertyUtil.propertyAsInt(currentSnapshot.summary(), SnapshotSummary.TOTAL_DATA_FILES_PROP, 0) + PropertyUtil
              .propertyAsInt(currentSnapshot.summary(), org.apache.iceberg.SnapshotSummary.TOTAL_DELETE_FILES_PROP, 0);
      totalFileStatBuilder.addFiles(addedFilesSize, addedFilesCnt);
    }
    tableStatistics.setTotalFilesStat(totalFileStatBuilder.build());

    // list all history snapshot
    Iterable<Snapshot> snapshots = arcticInternalTable.snapshots();
    Snapshot firstSnapshot = null;
    Snapshot lastSnapshot = null;
    int snapshotCount = 0;
    for (Snapshot snapshot : snapshots) {
      snapshotCount++;
      lastSnapshot = snapshot;
      if (firstSnapshot == null) {
        firstSnapshot = snapshot;
      }
    }

    Map<String, String> summary = fillSummary(snapshotCount, lastSnapshot, firstSnapshot);
    tableStatistics.setSummary(summary);
    fillTableSnapshotInfo(tableStatistics, arcticInternalTable);
  }

  private static TableStatistics initEmptyTableStatistics(
      TableStatistics tableStatistics,
      TableIdentifier tableIdentifier) {
    tableStatistics.setSummary(new HashMap<>());
    tableStatistics.setTableIdentifier(tableIdentifier);
    tableStatistics.setTotalFilesStat(new FilesStatistics(0, 0L));
    return tableStatistics;
  }

  private static Map<String, String> fillSummary(
      int snapshotCount,
      Snapshot lastSnapshot,
      Snapshot firstSnapshot) {
    Map<String, String> summary = new HashMap<>();
    PropertiesUtil.putNotNullProperties(summary, "snapshotCnt", String.valueOf(snapshotCount));
    PropertiesUtil.putNotNullProperties(summary, "visibleTime",
        lastSnapshot == null ? null : String.valueOf(lastSnapshot.timestampMillis()));
    PropertiesUtil.putNotNullProperties(summary, "firstSnapshotCommitTime",
        firstSnapshot == null ? null : String.valueOf(firstSnapshot.timestampMillis()));
    return summary;
  }
}