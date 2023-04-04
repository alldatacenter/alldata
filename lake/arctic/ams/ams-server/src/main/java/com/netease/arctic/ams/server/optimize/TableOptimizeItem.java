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

package com.netease.arctic.ams.server.optimize;

import com.google.common.annotations.VisibleForTesting;
import com.netease.arctic.AmsClient;
import com.netease.arctic.ams.api.AlreadyExistsException;
import com.netease.arctic.ams.api.ErrorMessage;
import com.netease.arctic.ams.api.OptimizeRangeType;
import com.netease.arctic.ams.api.OptimizeStatus;
import com.netease.arctic.ams.api.OptimizeTaskId;
import com.netease.arctic.ams.api.OptimizeTaskStat;
import com.netease.arctic.ams.api.OptimizeType;
import com.netease.arctic.ams.server.mapper.InternalTableFilesMapper;
import com.netease.arctic.ams.server.mapper.OptimizeHistoryMapper;
import com.netease.arctic.ams.server.mapper.OptimizeTaskRuntimesMapper;
import com.netease.arctic.ams.server.mapper.OptimizeTasksMapper;
import com.netease.arctic.ams.server.mapper.TableOptimizeRuntimeMapper;
import com.netease.arctic.ams.server.mapper.TaskHistoryMapper;
import com.netease.arctic.ams.server.model.BasicOptimizeTask;
import com.netease.arctic.ams.server.model.CoreInfo;
import com.netease.arctic.ams.server.model.FilesStatistics;
import com.netease.arctic.ams.server.model.OptimizeHistory;
import com.netease.arctic.ams.server.model.OptimizeTaskRuntime;
import com.netease.arctic.ams.server.model.TableMetadata;
import com.netease.arctic.ams.server.model.TableOptimizeInfo;
import com.netease.arctic.ams.server.model.TableOptimizeRuntime;
import com.netease.arctic.ams.server.model.TableTaskHistory;
import com.netease.arctic.ams.server.service.IJDBCService;
import com.netease.arctic.ams.server.service.IQuotaService;
import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.ams.server.utils.FilesStatisticsBuilder;
import com.netease.arctic.ams.server.utils.TableStatCollector;
import com.netease.arctic.ams.server.utils.UnKeyedTableUtil;
import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.catalog.CatalogLoader;
import com.netease.arctic.data.file.ContentFileWithSequence;
import com.netease.arctic.data.file.FileNameGenerator;
import com.netease.arctic.hive.table.SupportHive;
import com.netease.arctic.hive.utils.TableTypeUtil;
import com.netease.arctic.scan.ChangeTableIncrementalScan;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.utils.CompatiblePropertyUtil;
import com.netease.arctic.utils.TablePropertyUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.SnapshotSummary;
import org.apache.iceberg.TableScan;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.util.PropertyUtil;
import org.apache.iceberg.util.StructLikeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class TableOptimizeItem extends IJDBCService {
  public static final Long META_EXPIRE_TIME = 60_000L;// 1min
  private static final Logger LOG = LoggerFactory.getLogger(TableOptimizeItem.class);

  private final TableIdentifier tableIdentifier;
  private volatile ArcticTable arcticTable;
  private TableOptimizeRuntime tableOptimizeRuntime;
  private FilesStatistics optimizeFileInfo;

  private final ReentrantLock tasksLock = new ReentrantLock();
  private final ReentrantLock tableLock = new ReentrantLock();
  private final ReentrantLock tasksCommitLock = new ReentrantLock();
  private final AtomicBoolean waitCommit = new AtomicBoolean(false);
  // flag to avoid concurrent plan
  private final AtomicBoolean planning = new AtomicBoolean(false);

  private final Map<OptimizeTaskId, OptimizeTaskItem> optimizeTasks = new LinkedHashMap<>();

  private volatile long metaRefreshTime;

  private final IQuotaService quotaService;
  private final AmsClient metastoreClient;
  private volatile double quotaCache;
  private volatile String groupNameCache;

  private BasicOptimizeCommit optimizeCommit;
  private long commitTime;
  private static final long INIT_COMMIT_TIME = 0L;

  /**
   * -1: not initialized
   * 0: not committed
   */
  private volatile long latestCommitTime = -1L;

  public TableOptimizeItem(ArcticTable arcticTable, TableMetadata tableMetadata) {
    this.arcticTable = arcticTable;
    this.metaRefreshTime = -1;
    this.tableOptimizeRuntime = new TableOptimizeRuntime(tableMetadata.getTableIdentifier());
    this.quotaCache = CompatiblePropertyUtil.propertyAsDouble(tableMetadata.getProperties(),
        TableProperties.SELF_OPTIMIZING_QUOTA,
        TableProperties.SELF_OPTIMIZING_QUOTA_DEFAULT);
    this.groupNameCache = CompatiblePropertyUtil.propertyAsString(tableMetadata.getProperties(),
        TableProperties.SELF_OPTIMIZING_GROUP,
        TableProperties.SELF_OPTIMIZING_GROUP_DEFAULT);
    this.tableIdentifier = tableMetadata.getTableIdentifier();
    this.metastoreClient = ServiceContainer.getTableMetastoreHandler();
    this.quotaService = ServiceContainer.getQuotaService();
  }

  @VisibleForTesting
  public TableOptimizeItem(ArcticTable arcticTable, TableMetadata tableMetadata, long metaRefreshTime) {
    this.arcticTable = arcticTable;
    this.metaRefreshTime = metaRefreshTime;
    this.tableOptimizeRuntime = new TableOptimizeRuntime(tableMetadata.getTableIdentifier());
    this.quotaCache = CompatiblePropertyUtil.propertyAsDouble(tableMetadata.getProperties(),
        TableProperties.SELF_OPTIMIZING_QUOTA,
        TableProperties.SELF_OPTIMIZING_QUOTA_DEFAULT);
    this.groupNameCache = CompatiblePropertyUtil.propertyAsString(tableMetadata.getProperties(),
        TableProperties.SELF_OPTIMIZING_GROUP,
        TableProperties.SELF_OPTIMIZING_GROUP_DEFAULT);
    this.tableIdentifier = tableMetadata.getTableIdentifier();
    this.metastoreClient = ServiceContainer.getTableMetastoreHandler();
    this.quotaService = ServiceContainer.getQuotaService();
  }

  /**
   * Initial optimize tasks.
   *
   * @param optimizeTasks -
   */
  public void initOptimizeTasks(List<OptimizeTaskItem> optimizeTasks) {
    if (CollectionUtils.isNotEmpty(optimizeTasks)) {
      optimizeTasks
          .forEach(task -> this.optimizeTasks.put(task.getOptimizeTask().getTaskId(), task));
    }
  }

  /**
   * Initial TableOptimizeRuntime.
   *
   * @param runtime -
   * @return this for chain
   */
  public TableOptimizeItem initTableOptimizeRuntime(TableOptimizeRuntime runtime) {
    if (runtime != null) {
      this.tableOptimizeRuntime = runtime;
    } else if (this.tableOptimizeRuntime == null) {
      this.tableOptimizeRuntime = new TableOptimizeRuntime(tableIdentifier);
    }
    return this;
  }

  /**
   * if all tasks are Prepared
   *
   * @return true if tasks is not empty and all Prepared
   */
  public boolean allTasksPrepared() {
    if (!optimizeTasks.isEmpty()) {
      return optimizeTasks.values().stream().allMatch(t -> t.getOptimizeStatus() == OptimizeStatus.Prepared);
    } else {
      return false;
    }
  }

  /**
   * try trigger commit if all tasks are Prepared.
   */
  public void tryTriggerCommit() {
    tasksLock.lock();
    try {
      if (waitCommit.get()) {
        return;
      }
      if (!allTasksPrepared() && !isRetryCommit()) {
        return;
      }
      boolean success = ServiceContainer.getOptimizeService().triggerOptimizeCommit(this);
      if (success) {
        waitCommit.set(true);
      }
    } finally {
      tasksLock.unlock();
    }
  }

  /**
   * Get table identifier.
   *
   * @return TableIdentifier
   */
  public TableIdentifier getTableIdentifier() {
    return tableIdentifier;
  }

  /**
   * Get Arctic Table, refresh if expired.
   *
   * @return ArcticTable
   */
  public ArcticTable getArcticTable() {
    if (arcticTable == null) {
      tryRefresh(false);
    }
    return arcticTable;
  }

  /**
   * Get arcticTable, refresh immediately or not.
   *
   * @param forceRefresh - refresh immediately
   * @return ArcticTable
   */
  public ArcticTable getArcticTable(boolean forceRefresh) {
    tryRefresh(forceRefresh);
    return arcticTable;
  }

  /**
   * If arctic table is KeyedTable.
   *
   * @return true/false
   */
  public boolean isKeyedTable() {
    if (arcticTable == null) {
      tryRefresh(false);
    }
    return arcticTable.isKeyedTable();
  }

  /**
   * Get cached quota, cache will be updated when arctic table refresh.
   *
   * @return quota
   */
  public double getQuotaCache() {
    return quotaCache;
  }

  public long getLatestCommitTime() {
    if (latestCommitTime == -1L) {
      latestCommitTime = ServiceContainer.getOptimizeService().getLatestCommitTime(tableIdentifier);
    }
    return latestCommitTime;
  }

  public String getGroupNameCache() {
    return groupNameCache;
  }

  private void tryRefresh(boolean force) {
    if (force || isMetaExpired() || arcticTable == null) {
      tableLock.lock();
      try {
        if (force || isMetaExpired() || arcticTable == null) {
          refresh();
        }
      } finally {
        tableLock.unlock();
      }
    }
  }

  private void refresh() {
    ArcticCatalog catalog = CatalogLoader.load(metastoreClient, tableIdentifier.getCatalog());
    this.arcticTable = catalog.loadTable(tableIdentifier);
    this.metaRefreshTime = System.currentTimeMillis();
    this.quotaCache = CompatiblePropertyUtil.propertyAsDouble(arcticTable.properties(),
        TableProperties.SELF_OPTIMIZING_QUOTA,
        TableProperties.SELF_OPTIMIZING_QUOTA_DEFAULT);
    this.groupNameCache = CompatiblePropertyUtil.propertyAsString(arcticTable.properties(),
        TableProperties.SELF_OPTIMIZING_GROUP,
        TableProperties.SELF_OPTIMIZING_GROUP_DEFAULT);
  }

  private int optimizeMaxRetry() {
    return CompatiblePropertyUtil
        .propertyAsInt(getArcticTable(false).properties(), TableProperties.SELF_OPTIMIZING_RETRY_NUMBER,
            TableProperties.SELF_OPTIMIZING_RETRY_NUMBER_DEFAULT);
  }

  private boolean isMetaExpired() {
    return System.currentTimeMillis() > metaRefreshTime + META_EXPIRE_TIME;
  }

  /**
   * Update optimize task result, Failed or Prepared.
   *
   * @param optimizeTaskStat - optimizeTaskStat
   */
  public void updateOptimizeTaskStat(OptimizeTaskStat optimizeTaskStat) {
    Objects.requireNonNull(optimizeTaskStat, "optimizeTaskStat can't be null");
    Objects.requireNonNull(optimizeTaskStat.getTaskId(), "optimizeTaskId can't be null");

    OptimizeTaskItem optimizeTaskItem = optimizeTasks.get(optimizeTaskStat.getTaskId());
    Preconditions.checkNotNull(optimizeTaskItem, "can't find optimize task " + optimizeTaskStat.getTaskId());
    LOG.info("{} task {} ==== updateMajorOptimizeTaskStat, commitGroup = {}, status = {}, attemptId={}",
        optimizeTaskItem.getTableIdentifier(), optimizeTaskItem.getOptimizeTask().getTaskId(),
        optimizeTaskItem.getOptimizeTask().getTaskCommitGroup(), optimizeTaskStat.getStatus(),
        optimizeTaskStat.getAttemptId());
    Preconditions.checkArgument(
        Objects.equals(optimizeTaskStat.getAttemptId(), optimizeTaskItem.getOptimizeRuntime().getAttemptId()),
        "wrong attemptId " + optimizeTaskStat.getAttemptId() + " valid attemptId " +
            optimizeTaskItem.getOptimizeRuntime().getAttemptId());
    switch (optimizeTaskStat.getStatus()) {
      case Failed:
        optimizeTaskItem.onFailed(optimizeTaskStat.getErrorMessage(), optimizeTaskStat.getCostTime());
        break;
      case Prepared:
        List<ByteBuffer> targetFiles = optimizeTaskStat.getFiles();
        long targetFileSize = optimizeTaskStat.getNewFileSize();
        // if minor optimize, insert files as base new files
        if (optimizeTaskItem.getOptimizeTask().getTaskId().getType() == OptimizeType.Minor &&
            !com.netease.arctic.utils.TableTypeUtil.isIcebergTableFormat(getArcticTable())) {
          if (optimizeTaskItem.getOptimizeTask().getInsertFiles() == null ||
              optimizeTaskItem.getOptimizeTask().getInsertFileCnt() !=
                  optimizeTaskItem.getOptimizeTask().getInsertFiles().size()) {
            optimizeTaskItem.setFiles();
          }
          // check whether insert files don't change, confirm data consistency
          if (optimizeTaskItem.getOptimizeTask().getInsertFiles() != null &&
              optimizeTaskItem.getOptimizeTask().getInsertFileCnt() !=
              optimizeTaskItem.getOptimizeTask().getInsertFiles().size()) {
            String errorMessage =
                String.format("table %s insert files changed in minor optimize task %s, can't prepared.",
                    optimizeTaskItem.getTableIdentifier(), optimizeTaskItem.getOptimizeTask().getTaskId().getTraceId());
            throw new IllegalStateException(errorMessage);
          }
          targetFiles.addAll(optimizeTaskItem.getOptimizeTask().getInsertFiles());
          targetFileSize = targetFileSize + optimizeTaskItem.getOptimizeTask().getInsertFileSize();
        }
        optimizeTaskItem.onPrepared(optimizeTaskStat.getReportTime(),
            targetFiles, targetFileSize, optimizeTaskStat.getCostTime());
        tryTriggerCommit();
        break;
      default:
        throw new IllegalArgumentException("unsupported status: " + optimizeTaskStat.getStatus());
    }
  }

  /**
   * Build current table optimize info.
   *
   * @return TableOptimizeInfo
   */
  public TableOptimizeInfo buildTableOptimizeInfo() {
    CoreInfo tableResourceInfo = quotaService.getTableResourceInfo(tableIdentifier, 3600 * 1000);
    double needCoreCount = tableResourceInfo.getNeedCoreCount();
    double realCoreCount = tableResourceInfo.getRealCoreCount();
    TableOptimizeInfo tableOptimizeInfo = new TableOptimizeInfo(tableIdentifier);
    TableOptimizeRuntime tableOptimizeRuntime = getTableOptimizeRuntime();
    tableOptimizeInfo.setOptimizeStatus(tableOptimizeRuntime.getOptimizeStatus().displayValue());
    tableOptimizeInfo.setDuration(System.currentTimeMillis() - tableOptimizeRuntime.getOptimizeStatusStartTime());
    tableOptimizeInfo.setQuota(needCoreCount);
    double value = realCoreCount / needCoreCount;
    tableOptimizeInfo.setQuotaOccupation(new BigDecimal(value).setScale(4, RoundingMode.HALF_UP).doubleValue());
    if (tableOptimizeRuntime.getOptimizeStatus() == TableOptimizeRuntime.OptimizeStatus.FullOptimizing) {
      List<BasicOptimizeTask> optimizeTasks =
          this.optimizeTasks.values().stream().map(OptimizeTaskItem::getOptimizeTask).collect(
              Collectors.toList());
      this.optimizeFileInfo = collectOptimizeFileInfo(optimizeTasks, OptimizeType.FullMajor);
    } else if (tableOptimizeRuntime.getOptimizeStatus() == TableOptimizeRuntime.OptimizeStatus.MajorOptimizing) {
      List<BasicOptimizeTask> optimizeTasks =
          this.optimizeTasks.values().stream().map(OptimizeTaskItem::getOptimizeTask).collect(
              Collectors.toList());
      this.optimizeFileInfo = collectOptimizeFileInfo(optimizeTasks, OptimizeType.Major);
    } else if (tableOptimizeRuntime.getOptimizeStatus() == TableOptimizeRuntime.OptimizeStatus.MinorOptimizing) {
      List<BasicOptimizeTask> optimizeTasks =
          this.optimizeTasks.values().stream().map(OptimizeTaskItem::getOptimizeTask).collect(
              Collectors.toList());
      this.optimizeFileInfo = collectOptimizeFileInfo(optimizeTasks, OptimizeType.Minor);
    }
    if (this.optimizeFileInfo != null) {
      tableOptimizeInfo.setFileCount(this.optimizeFileInfo.getFileCnt());
      tableOptimizeInfo.setFileSize(this.optimizeFileInfo.getTotalSize());
    }
    tableOptimizeInfo.setGroupName(groupNameCache);
    return tableOptimizeInfo;
  }

  /**
   * Refresh and update table optimize status.
   */
  public void updateTableOptimizeStatus() {
    if (!this.optimizeTasks.isEmpty()) {
      tasksLock.lock();
      try {
        if (!this.optimizeTasks.isEmpty()) {
          List<BasicOptimizeTask> optimizeTasks =
              this.optimizeTasks.values().stream().map(OptimizeTaskItem::getOptimizeTask).collect(
                  Collectors.toList());
          if (hasFullOptimizeTask()) {
            tryUpdateOptimizeInfo(
                TableOptimizeRuntime.OptimizeStatus.FullOptimizing, optimizeTasks, OptimizeType.FullMajor);
          } else if (hasMajorOptimizeTask()) {
            tryUpdateOptimizeInfo(
                TableOptimizeRuntime.OptimizeStatus.MajorOptimizing, optimizeTasks, OptimizeType.Major);
          } else {
            tryUpdateOptimizeInfo(
                TableOptimizeRuntime.OptimizeStatus.MinorOptimizing, optimizeTasks, OptimizeType.Minor);
          }
          return;
        }
      } finally {
        tasksLock.unlock();
      }
    }
    // if optimizeTasks is empty
    if (!CompatiblePropertyUtil
        .propertyAsBoolean(getArcticTable(false).properties(), TableProperties.ENABLE_SELF_OPTIMIZING,
            TableProperties.ENABLE_SELF_OPTIMIZING_DEFAULT)) {
      tryUpdateOptimizeInfo(TableOptimizeRuntime.OptimizeStatus.Idle, Collections.emptyList(), null);
      return;
    }
    if (planning.get()) {
      // if the table is planning, should not update the optimizing status
      return;
    }
    if (tableOptimizeRuntime.getOptimizeStatus() == TableOptimizeRuntime.OptimizeStatus.Pending) {
      // if the table is Pending, should not plan again
      return;
    }
    if (com.netease.arctic.utils.TableTypeUtil.isIcebergTableFormat(getArcticTable())) {
      arcticTable.asUnkeyedTable().refresh();
      Snapshot currentSnapshot = arcticTable.asUnkeyedTable().currentSnapshot();
      if (currentSnapshot == null) {
        tryUpdateOptimizeInfo(TableOptimizeRuntime.OptimizeStatus.Idle, Collections.emptyList(), null);
        return;
      }
      if (!tableChanged(currentSnapshot)) {
        tryUpdateOptimizeInfo(TableOptimizeRuntime.OptimizeStatus.Idle, Collections.emptyList(), null);
        return;
      }
      List<FileScanTask> fileScanTasks;
      TableScan tableScan = arcticTable.asUnkeyedTable().newScan();
      tableScan = tableScan.useSnapshot(currentSnapshot.snapshotId());
      try (CloseableIterable<FileScanTask> filesIterable = tableScan.planFiles()) {
        fileScanTasks = Lists.newArrayList(filesIterable);
      } catch (IOException e) {
        throw new UncheckedIOException("Failed to close table scan of " + tableIdentifier, e);
      }
      IcebergFullOptimizePlan fullPlan =
          getIcebergFullPlan(fileScanTasks, -1, System.currentTimeMillis(), currentSnapshot.snapshotId());
      OptimizePlanResult optimizePlanResult = fullPlan.plan();
      // pending for full optimize
      if (!optimizePlanResult.isEmpty()) {
        tryUpdateOptimizeInfo(TableOptimizeRuntime.OptimizeStatus.Pending, optimizePlanResult.getOptimizeTasks(),
            OptimizeType.FullMajor);
      } else {
        IcebergMinorOptimizePlan minorPlan =
            getIcebergMinorPlan(fileScanTasks, -1, System.currentTimeMillis(), currentSnapshot.snapshotId());
        optimizePlanResult = minorPlan.plan();
        // pending for minor optimize
        if (!optimizePlanResult.isEmpty()) {
          tryUpdateOptimizeInfo(
              TableOptimizeRuntime.OptimizeStatus.Pending, optimizePlanResult.getOptimizeTasks(), OptimizeType.Minor);
        } else {
          // idle state
          tryUpdateOptimizeInfo(TableOptimizeRuntime.OptimizeStatus.Idle, Collections.emptyList(), null);
        }
      }
    } else {
      Snapshot baseCurrentSnapshot;
      Snapshot changeCurrentSnapshot = null;
      ArcticTable arcticTable = getArcticTable();
      StructLikeMap<Long> partitionOptimizedSequence = null;
      StructLikeMap<Long> legacyPartitionMaxTransactionId = null;
      if (arcticTable.isKeyedTable()) {
        baseCurrentSnapshot = UnKeyedTableUtil.getCurrentSnapshot(arcticTable.asKeyedTable().baseTable());
        partitionOptimizedSequence = TablePropertyUtil.getPartitionOptimizedSequence(arcticTable.asKeyedTable());
        legacyPartitionMaxTransactionId =
            TablePropertyUtil.getLegacyPartitionMaxTransactionId(arcticTable.asKeyedTable());
        changeCurrentSnapshot = UnKeyedTableUtil.getCurrentSnapshot(arcticTable.asKeyedTable().changeTable());
      } else {
        baseCurrentSnapshot = UnKeyedTableUtil.getCurrentSnapshot(arcticTable.asUnkeyedTable());
      }

      List<FileScanTask> baseFiles = planBaseFiles(baseCurrentSnapshot);
      FullOptimizePlan fullPlan =
          getFullPlan(-1, System.currentTimeMillis(), baseFiles, baseCurrentSnapshot);
      OptimizePlanResult optimizePlanResult = OptimizePlanResult.EMPTY;
      if (fullPlan != null) {
        optimizePlanResult = fullPlan.plan();
      }
      // pending for full optimize
      if (!optimizePlanResult.isEmpty()) {
        tryUpdateOptimizeInfo(TableOptimizeRuntime.OptimizeStatus.Pending, optimizePlanResult.getOptimizeTasks(),
            OptimizeType.FullMajor);
      } else {
        MajorOptimizePlan majorPlan =
            getMajorPlan(-1, System.currentTimeMillis(), baseFiles, baseCurrentSnapshot);
        if (majorPlan != null) {
          optimizePlanResult = majorPlan.plan();
        }
        // pending for major optimize
        if (!optimizePlanResult.isEmpty()) {
          tryUpdateOptimizeInfo(
              TableOptimizeRuntime.OptimizeStatus.Pending, optimizePlanResult.getOptimizeTasks(), OptimizeType.Major);
        } else {
          if (isKeyedTable()) {
            if (!changeStoreChanged(changeCurrentSnapshot)) {
              tryUpdateOptimizeInfo(TableOptimizeRuntime.OptimizeStatus.Idle, Collections.emptyList(), null);
              return;
            }
            MinorOptimizePlan minorPlan =
                getMinorPlan(-1, System.currentTimeMillis(), baseFiles, baseCurrentSnapshot,
                    changeCurrentSnapshot, partitionOptimizedSequence, legacyPartitionMaxTransactionId);
            if (minorPlan != null) {
              optimizePlanResult = minorPlan.plan();
            }
            if (!optimizePlanResult.isEmpty()) {
              tryUpdateOptimizeInfo(TableOptimizeRuntime.OptimizeStatus.Pending,
                  optimizePlanResult.getOptimizeTasks(),
                  OptimizeType.Minor);
              return;
            }
          }
          // idle state
          tryUpdateOptimizeInfo(TableOptimizeRuntime.OptimizeStatus.Idle, Collections.emptyList(), null);
        }
      }
    }
  }

  public List<FileScanTask> planBaseFiles(Snapshot baseCurrentSnapshot) {
    if (baseCurrentSnapshot == null) {
      return Collections.emptyList();
    }
    UnkeyedTable baseTable;
    if (getArcticTable().isKeyedTable()) {
      baseTable = getArcticTable().asKeyedTable().baseTable();
    } else {
      baseTable = getArcticTable().asUnkeyedTable();
    }
    List<FileScanTask> baseFiles = new ArrayList<>();
    try (CloseableIterable<FileScanTask> fileScanTasks = baseTable.newScan()
        .useSnapshot(baseCurrentSnapshot.snapshotId())
        .planFiles()) {
      fileScanTasks.forEach(baseFiles::add);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to close table scan of " + baseTable.name(), e);
    }
    return baseFiles;
  }

  public void checkOptimizeGroup() {
    try {
      Set<TableIdentifier> tablesOfQueue =
          ServiceContainer.getOptimizeQueueService().getTablesOfQueue(this.groupNameCache);
      if (!tablesOfQueue.contains(this.tableIdentifier)) {
        ServiceContainer.getOptimizeQueueService().release(this.tableIdentifier);
        ServiceContainer.getOptimizeQueueService().bind(tableIdentifier, this.groupNameCache);
      }
    } catch (Exception e) {
      LOG.error("checkOptimizeGroup error", e);
    }
  }

  private boolean hasMajorOptimizeTask() {
    for (Map.Entry<OptimizeTaskId, OptimizeTaskItem> entry : optimizeTasks.entrySet()) {
      OptimizeTaskId key = entry.getKey();
      if (key.getType() == OptimizeType.Major) {
        return true;
      }
    }
    return false;
  }

  private boolean hasFullOptimizeTask() {
    for (Map.Entry<OptimizeTaskId, OptimizeTaskItem> entry : optimizeTasks.entrySet()) {
      OptimizeTaskId key = entry.getKey();
      if (key.getType() == OptimizeType.FullMajor) {
        return true;
      }
    }
    return false;
  }

  private FilesStatistics collectOptimizeFileInfo(Collection<BasicOptimizeTask> tasks, OptimizeType optimizeType) {
    FilesStatisticsBuilder builder = new FilesStatisticsBuilder();
    for (BasicOptimizeTask task : tasks) {
      if (task.getTaskId().getType().equals(optimizeType)) {
        builder.addFiles(task.getBaseFileSize(), task.getBaseFileCnt());
        builder.addFiles(task.getInsertFileSize(), task.getInsertFileCnt());
        builder.addFiles(task.getDeleteFileSize(), task.getDeleteFileCnt());
        builder.addFiles(task.getPosDeleteFileSize(), task.getPosDeleteFileCnt());
      }
    }
    return builder.build();
  }

  private void tryUpdateOptimizeInfo(TableOptimizeRuntime.OptimizeStatus optimizeStatus,
                                     Collection<BasicOptimizeTask> optimizeTasks, OptimizeType optimizeType) {
    if (tableOptimizeRuntime.getOptimizeStatus() != optimizeStatus) {
      tableOptimizeRuntime.setOptimizeStatus(optimizeStatus);
      tableOptimizeRuntime.setOptimizeStatusStartTime(System.currentTimeMillis());
      try {
        persistTableOptimizeRuntime();
      } catch (Throwable t) {
        LOG.warn("failed to persist tableOptimizeRuntime when update OptimizeStatus, ignore", t);
      }
      optimizeFileInfo = collectOptimizeFileInfo(optimizeTasks, optimizeType);
    }
    if (tableOptimizeRuntime.getOptimizeStatusStartTime() <= 0) {
      long createTime = PropertyUtil.propertyAsLong(getArcticTable().properties(), TableProperties.TABLE_CREATE_TIME,
          TableProperties.TABLE_CREATE_TIME_DEFAULT);
      if (createTime != tableOptimizeRuntime.getOptimizeStatusStartTime()) {
        tableOptimizeRuntime.setOptimizeStatusStartTime(createTime);
        persistTableOptimizeRuntime();
      }
    }
  }

  /**
   * Add new optimize tasks.
   *
   * @param newOptimizeTasks new optimize tasks
   * @throws AlreadyExistsException when task already exists
   */
  public void addNewOptimizeTasks(List<BasicOptimizeTask> newOptimizeTasks)
      throws AlreadyExistsException {
    // for rollback
    Set<OptimizeTaskId> addedOptimizeTaskIds = new HashSet<>();
    tasksLock.lock();
    try {
      for (BasicOptimizeTask optimizeTask : newOptimizeTasks) {
        OptimizeTaskRuntime optimizeRuntime = new OptimizeTaskRuntime(optimizeTask.getTaskId());
        OptimizeTaskItem optimizeTaskItem = new OptimizeTaskItem(optimizeTask, optimizeRuntime);
        if (optimizeTasks.putIfAbsent(optimizeTask.getTaskId(), optimizeTaskItem) != null) {
          throw new AlreadyExistsException(optimizeTask.getTaskId() + " already exists");
        }
        optimizeTaskItem.persistOptimizeTask();
        addedOptimizeTaskIds.add(optimizeTask.getTaskId());
        LOG.info("{} add new task {}", tableIdentifier, optimizeTask);
        // when minor optimize, there is no need to execute task not contains deleteFiles or not contains any dataFiles,
        // for no deleteFiles the inertFiles need to commit to base table
        // for no dataFiles the txId in base properties need to update
        boolean minorNotNeedExecute = optimizeTask.getDeleteFiles().isEmpty() ||
            (optimizeTask.getBaseFiles().isEmpty() && optimizeTask.getInsertFiles().isEmpty());
        if (minorNotNeedExecute && optimizeTask.getTaskId().getType().equals(OptimizeType.Minor) &&
            !com.netease.arctic.utils.TableTypeUtil.isIcebergTableFormat(arcticTable)) {
          optimizeTaskItem.onPrepared(System.currentTimeMillis(),
              optimizeTask.getInsertFiles(), optimizeTask.getInsertFileSize(), 0L);
        }
        optimizeTaskItem.clearFiles();
      }
      updateTableOptimizeStatus();
      tryTriggerCommit();
    } catch (Throwable t) {
      // rollback
      for (OptimizeTaskId addedOptimizeTaskId : addedOptimizeTaskIds) {
        OptimizeTaskItem removed = optimizeTasks.remove(addedOptimizeTaskId);
        if (removed != null) {
          removed.clearOptimizeTask();
        }
      }
      throw t;
    } finally {
      tasksLock.unlock();
    }
  }

  public void optimizeTasksClear(boolean refreshOptimizeStatus) {
    try (SqlSession sqlSession = getSqlSession(false)) {
      List<OptimizeTaskItem> tasks = new ArrayList<>(optimizeTasks.values());

      OptimizeTasksMapper optimizeTasksMapper =
          getMapper(sqlSession, OptimizeTasksMapper.class);
      InternalTableFilesMapper internalTableFilesMapper =
          getMapper(sqlSession, InternalTableFilesMapper.class);
      TableOptimizeRuntimeMapper tableOptimizeRuntimeMapper =
          getMapper(sqlSession, TableOptimizeRuntimeMapper.class);
      TaskHistoryMapper taskHistoryMapper =
          getMapper(sqlSession, TaskHistoryMapper.class);


      TableOptimizeRuntime oldTableRuntime = tableOptimizeRuntime.clone();
      try {
        // reset snapshot id in table runtime, because the tasks were cleared rather than committed.
        // So need to plan again.
        tableOptimizeRuntime.setCurrentSnapshotId(TableOptimizeRuntime.INVALID_SNAPSHOT_ID);
        tableOptimizeRuntime.setCurrentChangeSnapshotId(TableOptimizeRuntime.INVALID_SNAPSHOT_ID);
        // persist partition optimize time
        tableOptimizeRuntimeMapper.updateTableOptimizeRuntime(tableOptimizeRuntime);
      } catch (Throwable t) {
        tableOptimizeRuntime.restoreTableOptimizeRuntime(oldTableRuntime);
        LOG.error("failed to persist tableOptimizeRuntime after commit failed, ignore. " + getTableIdentifier(), t);
        sqlSession.rollback(true);
        return;
      }

      tasksLock.lock();
      List<OptimizeTaskItem> removedList = new ArrayList<>();
      try {
        long endTime = System.currentTimeMillis();
        tasks.stream().map(OptimizeTaskItem::getTaskId)
            .forEach(optimizeTaskId -> {
              OptimizeTaskItem removed = optimizeTasks.remove(optimizeTaskId);
              if (removed != null) {
                removedList.add(removed);
                optimizeTasksMapper.deleteOptimizeTask(optimizeTaskId.getTraceId());
                internalTableFilesMapper.deleteOptimizeTaskFile(optimizeTaskId);

                // set end time and cost time in optimize_task_history
                List<TableTaskHistory> taskHistoryList =
                    taskHistoryMapper.selectTaskHistoryByTraceId(optimizeTaskId.getTraceId());
                if (CollectionUtils.isNotEmpty(taskHistoryList)) {
                  TableTaskHistory taskHistory = taskHistoryList.get(0);
                  if (taskHistory != null && taskHistory.getEndTime() != 0) {
                    taskHistory.setEndTime(endTime);
                    taskHistory.setCostTime(endTime - taskHistory.getStartTime());
                    taskHistoryMapper.updateTaskHistory(taskHistory);
                  }
                }
              }
              LOG.info("{} removed", optimizeTaskId);
            });
      } catch (Throwable t) {
        for (OptimizeTaskItem optimizeTaskItem : removedList) {
          optimizeTasks.put(optimizeTaskItem.getTaskId(), optimizeTaskItem);
        }
        tableOptimizeRuntime.restoreTableOptimizeRuntime(oldTableRuntime);
        LOG.error("failed to remove optimize task after commit, ignore. " + getTableIdentifier(),
            t);
        sqlSession.rollback(true);
        return;
      } finally {
        tasksLock.unlock();
      }

      sqlSession.commit(true);
    }

    if (refreshOptimizeStatus) {
      updateTableOptimizeStatus();
    }
  }

  private void optimizeTasksCommitted(BasicOptimizeCommit optimizeCommit,
                                      long commitTime) {
    try (SqlSession sqlSession = getSqlSession(false)) {
      Map<String, List<OptimizeTaskItem>> tasks = optimizeCommit.getCommittedTasks();
      Map<String, OptimizeType> optimizeTypMap = optimizeCommit.getPartitionOptimizeType();

      // commit
      OptimizeTasksMapper optimizeTasksMapper =
          getMapper(sqlSession, OptimizeTasksMapper.class);
      OptimizeTaskRuntimesMapper optimizeTaskRuntimesMapper =
          getMapper(sqlSession, OptimizeTaskRuntimesMapper.class);
      InternalTableFilesMapper internalTableFilesMapper =
          getMapper(sqlSession, InternalTableFilesMapper.class);
      TableOptimizeRuntimeMapper tableOptimizeRuntimeMapper =
          getMapper(sqlSession, TableOptimizeRuntimeMapper.class);
      OptimizeHistoryMapper optimizeHistoryMapper =
          getMapper(sqlSession, OptimizeHistoryMapper.class);

      try {
        tasks.values().stream().flatMap(Collection::stream)
            .forEach(taskItem -> {
              OptimizeTaskRuntime newRuntime = taskItem.getOptimizeRuntime().clone();
              newRuntime.setCommitTime(commitTime);
              newRuntime.setStatus(OptimizeStatus.Committed);
              // after commit, task will be deleted, there is no need to update
              optimizeTaskRuntimesMapper.updateOptimizeTaskRuntime(newRuntime);
              taskItem.setOptimizeRuntime(newRuntime);
            });
      } catch (Exception e) {
        LOG.warn("failed to persist taskOptimizeRuntime after commit, ignore. " + getTableIdentifier(), e);
        sqlSession.rollback(true);
      }

      tasks.keySet().forEach(
          partition -> {
            OptimizeType optimizeType = optimizeTypMap.get(partition);
            switch (optimizeType) {
              case Minor:
                tableOptimizeRuntime.putLatestMinorOptimizeTime(partition, commitTime);
                break;
              case Major:
                tableOptimizeRuntime.putLatestMajorOptimizeTime(partition, commitTime);
                break;
              case FullMajor:
                tableOptimizeRuntime.putLatestFullOptimizeTime(partition, commitTime);
                break;
            }
          });

      try {
        // persist optimize task history
        OptimizeHistory record = buildOptimizeRecord(tasks, commitTime);
        optimizeHistoryMapper.insertOptimizeHistory(record);

        // update the latest commit time in memory
        latestCommitTime = Math.max(latestCommitTime, commitTime);
      } catch (Throwable t) {
        LOG.warn("failed to persist optimize history after commit, ignore. " + getTableIdentifier(), t);
        sqlSession.rollback(true);
      }

      try {
        // persist partition optimize time
        tableOptimizeRuntimeMapper.updateTableOptimizeRuntime(tableOptimizeRuntime);
      } catch (Throwable t) {
        LOG.warn("failed to persist tableOptimizeRuntime after commit, ignore. " + getTableIdentifier(), t);
        sqlSession.rollback(true);
      }

      tasksLock.lock();
      List<OptimizeTaskItem> removedList = new ArrayList<>();
      try {
        tasks.values().stream().flatMap(Collection::stream).map(OptimizeTaskItem::getTaskId)
            .forEach(optimizeTaskId -> {
              OptimizeTaskItem removed = optimizeTasks.remove(optimizeTaskId);
              if (removed != null) {
                removedList.add(removed);
                optimizeTasksMapper.deleteOptimizeTask(optimizeTaskId.getTraceId());
                internalTableFilesMapper.deleteOptimizeTaskFile(optimizeTaskId);
              }
              LOG.info("{} removed", optimizeTaskId);
            });
      } catch (Throwable t) {
        for (OptimizeTaskItem optimizeTaskItem : removedList) {
          optimizeTasks.put(optimizeTaskItem.getTaskId(), optimizeTaskItem);
        }
        LOG.warn("failed to remove optimize task after commit, ignore. " + getTableIdentifier(),
            t);
        sqlSession.rollback(true);
      } finally {
        tasksLock.unlock();
      }

      sqlSession.commit(true);
    }
    this.commitTime = INIT_COMMIT_TIME;
    updateTableOptimizeStatus();
  }

  private OptimizeHistory buildOptimizeRecord(Map<String, List<OptimizeTaskItem>> tasks, long commitTime) {
    OptimizeHistory record = new OptimizeHistory();
    record.setTableIdentifier(getTableIdentifier());
    record.setOptimizeRange(OptimizeRangeType.Partition);
    record.setCommitTime(commitTime);
    Long minPlanTime = tasks.entrySet().stream().flatMap(entry -> entry.getValue().stream())
        .map(OptimizeTaskItem::getOptimizeTask).map(BasicOptimizeTask::getCreateTime)
        .min(Long::compare).orElse(0L);
    record.setOptimizeType(tasks.entrySet().iterator().next().getValue().get(0)
        .getOptimizeTask().getTaskId().getType());
    record.setPlanTime(minPlanTime);
    record.setVisibleTime(commitTime);
    record.setDuration(record.getCommitTime() - record.getPlanTime());
    FilesStatisticsBuilder insertFb = new FilesStatisticsBuilder();
    FilesStatisticsBuilder deleteFb = new FilesStatisticsBuilder();
    FilesStatisticsBuilder baseFb = new FilesStatisticsBuilder();
    FilesStatisticsBuilder targetFb = new FilesStatisticsBuilder();
    FilesStatisticsBuilder posDeleteFb = new FilesStatisticsBuilder();
    tasks.values()
        .forEach(list -> list
            .forEach(t -> {
              BasicOptimizeTask task = t.getOptimizeTask();
              insertFb.addFiles(task.getInsertFileSize(), task.getInsertFileCnt());
              deleteFb.addFiles(task.getDeleteFileSize(), task.getDeleteFileCnt());
              baseFb.addFiles(task.getBaseFileSize(), task.getBaseFileCnt());
              posDeleteFb.addFiles(task.getPosDeleteFileSize(), task.getPosDeleteFileCnt());
              OptimizeTaskRuntime runtime = t.getOptimizeRuntime();
              targetFb.addFiles(runtime.getNewFileSize(), runtime.getNewFileCnt());
            }));
    record.setInsertFilesStatBeforeOptimize(insertFb.build());
    record.setDeleteFilesStatBeforeOptimize(deleteFb.build());
    record.setBaseFilesStatBeforeOptimize(baseFb.build());
    record.setPosDeleteFilesStatBeforeOptimize(posDeleteFb.build());

    FilesStatistics totalFs = new FilesStatisticsBuilder()
        .addFilesStatistics(record.getInsertFilesStatBeforeOptimize())
        .addFilesStatistics(record.getDeleteFilesStatBeforeOptimize())
        .addFilesStatistics(record.getBaseFilesStatBeforeOptimize())
        .addFilesStatistics(record.getPosDeleteFilesStatBeforeOptimize())
        .build();
    record.setTotalFilesStatBeforeOptimize(totalFs);
    record.setTotalFilesStatAfterOptimize(targetFb.build());

    record.setPartitionCnt(tasks.keySet().size());
    record.setPartitions(String.join(",", tasks.keySet()));
    if (isKeyedTable()) {
      KeyedTable keyedHiveTable = getArcticTable(true).asKeyedTable();
      record.setSnapshotInfo(TableStatCollector.buildBaseTableSnapshotInfo(keyedHiveTable.baseTable()));
      record.setPartitionOptimizedSequence(TablePropertyUtil.getPartitionOptimizedSequence(keyedHiveTable).toString());
    } else {
      getArcticTable(true);
      record.setSnapshotInfo(TableStatCollector.buildBaseTableSnapshotInfo(getArcticTable(true).asUnkeyedTable()));
    }
    return record;
  }

  /**
   * GetOptimizeTasksToExecute
   * include Init, Failed.
   *
   * @return List of OptimizeTaskItem
   */
  public List<OptimizeTaskItem> getOptimizeTasksToExecute() {
    // lock for conflict with add new tasks, because files with be removed from OptimizeTask after tasks added
    tasksLock.lock();
    try {
      return optimizeTasks.values().stream()
          .filter(taskItem -> taskItem.canExecute(this::optimizeMaxRetry))
          .sorted(Comparator.comparingLong(o -> o.getOptimizeTask().getCreateTime()))
          .collect(Collectors.toList());
    } finally {
      tasksLock.unlock();
    }
  }

  /**
   * If task execute timeout, set it to be Failed.
   */
  public void checkTaskExecuteTimeout() {
    tasksLock.lock();
    try {
      optimizeTasks.values().stream().filter(OptimizeTaskItem::executeTimeout)
          .forEach(task -> {
            task.onFailed(new ErrorMessage(System.currentTimeMillis(), "execute expired"),
                System.currentTimeMillis() - task.getOptimizeRuntime().getExecuteTime());
            LOG.error("{} execute timeout, change to Failed", task.getTaskId());
          });
    } finally {
      tasksLock.unlock();
    }
  }

  /**
   * If task execute failed after max retry, clear tasks.
   */
  public void clearFailedTasks() {
    tasksLock.lock();
    try {
      int maxRetry = optimizeMaxRetry();
      Optional<OptimizeTaskItem> failedTask =
          optimizeTasks.values().stream().filter(task ->
                  task.getOptimizeRuntime().getRetry() > maxRetry && OptimizeStatus.Failed == task.getOptimizeStatus())
              .findAny();

      // if table has failed task after max retry, clear all tasks
      if (failedTask.isPresent()) {
        LOG.warn("{} has execute task failed over {} times, the reason is {}",
            tableIdentifier, maxRetry, failedTask.get().getOptimizeRuntime().getFailReason());
        optimizeTasksClear(false);
      }
    } finally {
      tasksLock.unlock();
    }
  }

  /**
   * Get tasks which is ready to commit (only if all tasks in a table is ready).
   *
   * @return map partition -> tasks of partition
   */
  public Map<String, List<OptimizeTaskItem>> getOptimizeTasksToCommit() {
    tasksLock.lock();
    try {
      Map<String, List<OptimizeTaskItem>> collector = new HashMap<>();
      for (OptimizeTaskItem optimizeTaskItem : optimizeTasks.values()) {
        String partition = optimizeTaskItem.getOptimizeTask().getPartition();
        if (!optimizeTaskItem.canCommit()) {
          collector.clear();
          break;
        }
        optimizeTaskItem.setFiles();
        collector.computeIfAbsent(partition, p -> new ArrayList<>()).add(optimizeTaskItem);
      }
      return collector;
    } finally {
      tasksLock.unlock();
    }
  }

  public void setTableCanCommit() {
    waitCommit.set(false);
  }

  /**
   * Commit optimize tasks.
   *
   * @throws Exception -
   */
  public void commitOptimizeTasks() throws Exception {
    tasksCommitLock.lock();

    // check current base table snapshot whether changed when minor optimize
    if (isMinorOptimizing() && !com.netease.arctic.utils.TableTypeUtil.isIcebergTableFormat(getArcticTable())) {
      if (tableOptimizeRuntime.getCurrentSnapshotId() !=
          UnKeyedTableUtil.getSnapshotId(getArcticTable().asKeyedTable().baseTable())) {
        LOG.info("the latest snapshot has changed in base table {}, give up commit.", tableIdentifier);
        optimizeTasksClear(true);
      }
    }

    try {
      // If it is a retry task, do optimizeTasksCommitted()
      if (isRetryCommit()) {
        optimizeTasksCommitted(optimizeCommit, commitTime);
      }
      Map<String, List<OptimizeTaskItem>> tasksToCommit = getOptimizeTasksToCommit();
      long taskCount = tasksToCommit.values().stream().mapToLong(Collection::size).sum();
      if (MapUtils.isNotEmpty(tasksToCommit)) {
        LOG.info("{} get {} tasks of {} partitions to commit", tableIdentifier, taskCount, tasksToCommit.size());
        if (com.netease.arctic.utils.TableTypeUtil.isIcebergTableFormat(getArcticTable())) {
          optimizeCommit = new IcebergOptimizeCommit(getArcticTable(true), tasksToCommit);
        } else if (TableTypeUtil.isHive(getArcticTable())) {
          optimizeCommit = new SupportHiveCommit(getArcticTable(true),
              tasksToCommit, OptimizeTaskItem::persistTargetFiles);
        } else {
          optimizeCommit = new BasicOptimizeCommit(getArcticTable(true), tasksToCommit);
        }

        boolean committed = optimizeCommit.commit(tableOptimizeRuntime.getCurrentSnapshotId());
        if (committed) {
          commitTime = System.currentTimeMillis();
          optimizeTasksCommitted(optimizeCommit, commitTime);
        } else {
          LOG.warn("{} commit failed, clear optimize tasks", tableIdentifier);
          optimizeTasksClear(true);
        }
      } else {
        LOG.info("{} get no tasks to commit", tableIdentifier);
      }
    } finally {
      tasksCommitLock.unlock();
    }
  }

  /**
   * Check if the task currently being submitted needs to be retried
   * @return boolean
   */
  private boolean isRetryCommit() {
    return commitTime != INIT_COMMIT_TIME;
  }

  /**
   * Get all optimize tasks.
   *
   * @return list of all optimize tasks
   */
  public List<OptimizeTaskItem> getOptimizeTasks() {
    return new ArrayList<>(optimizeTasks.values());
  }

  /**
   * Get full optimize plan for arctic tables.
   *
   * @param queueId     -
   * @param currentTime -
   * @return -
   */
  public FullOptimizePlan getFullPlan(int queueId, long currentTime,
                                      List<FileScanTask> baseFiles, Snapshot baseSnapshot) {
    if (baseSnapshot == null) {
      LOG.debug("{} base table is empty, skip full optimize", tableIdentifier);
      return null;
    }
    if (getArcticTable() instanceof SupportHive) {
      return new SupportHiveFullOptimizePlan(getArcticTable(), tableOptimizeRuntime,
          baseFiles, queueId, currentTime, baseSnapshot.snapshotId());
    } else {
      return new FullOptimizePlan(getArcticTable(), tableOptimizeRuntime,
          baseFiles, queueId, currentTime, baseSnapshot.snapshotId());
    }
  }

  /**
   * Get major optimize plan for arctic tables.
   *
   * @param queueId     -
   * @param currentTime -
   * @return -
   */
  public MajorOptimizePlan getMajorPlan(int queueId, long currentTime,
                                        List<FileScanTask> baseFiles, Snapshot baseSnapshot) {
    if (baseSnapshot == null) {
      LOG.debug("{} base table is empty, skip major optimize", tableIdentifier);
      return null;
    }

    if (getArcticTable() instanceof SupportHive) {
      return new SupportHiveMajorOptimizePlan(getArcticTable(), tableOptimizeRuntime,
          baseFiles, queueId, currentTime, baseSnapshot.snapshotId());
    } else {
      return new MajorOptimizePlan(getArcticTable(), tableOptimizeRuntime,
          baseFiles, queueId, currentTime, baseSnapshot.snapshotId());
    }
  }

  /**
   * Get minor optimize plan for arctic tables.
   *
   * @param queueId     -
   * @param currentTime -
   * @return -
   */
  public MinorOptimizePlan getMinorPlan(int queueId, long currentTime,
                                        List<FileScanTask> baseFiles, Snapshot baseSnapshot, Snapshot changeSnapshot,
                                        StructLikeMap<Long> partitionOptimizedSequence,
                                        StructLikeMap<Long> legacyPartitionMaxTransactionId) {
    if (changeSnapshot == null) {
      LOG.debug("{} change table is empty, skip minor optimize", tableIdentifier);
      return null;
    }
    long changeSnapshotId = changeSnapshot.snapshotId();
    long maxSequence = getMaxSequenceLimit(changeSnapshot, partitionOptimizedSequence, legacyPartitionMaxTransactionId);
    if (maxSequence == Long.MIN_VALUE) {
      return null;
    }
    ChangeTableIncrementalScan changeTableIncrementalScan =
        getArcticTable().asKeyedTable().changeTable().newChangeScan()
            .fromSequence(partitionOptimizedSequence)
            .fromLegacyTransaction(legacyPartitionMaxTransactionId)
            .toSequence(maxSequence)
            .useSnapshot(changeSnapshotId);
    List<ContentFileWithSequence<?>> changeFiles;
    try (CloseableIterable<ContentFileWithSequence<?>> files = changeTableIncrementalScan.planFilesWithSequence()) {
      changeFiles = Lists.newArrayList(files);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to close table scan of " + getArcticTable().name(), e);
    }

    // if not optimize all files, it is difficult to get the accurate snapshot id, just set changeSnapshotId to -1 to 
    // trigger next optimize
    if (maxSequence != Long.MAX_VALUE) {
      changeSnapshotId = -1;
    }
    return new MinorOptimizePlan(getArcticTable(), tableOptimizeRuntime, baseFiles, changeFiles,
        queueId, currentTime, changeSnapshotId,
        baseSnapshot == null ? TableOptimizeRuntime.INVALID_SNAPSHOT_ID : baseSnapshot.snapshotId());
  }

  private long getMaxSequenceLimit(Snapshot changeSnapshot,
                                   StructLikeMap<Long> partitionOptimizedSequence,
                                   StructLikeMap<Long> legacyPartitionMaxTransactionId) {
    int totalFilesInSummary = PropertyUtil
        .propertyAsInt(changeSnapshot.summary(), SnapshotSummary.TOTAL_DATA_FILES_PROP, 0);
    int maxFileCntLimit = CompatiblePropertyUtil.propertyAsInt(getArcticTable().properties(),
        TableProperties.SELF_OPTIMIZING_MAX_FILE_CNT, TableProperties.SELF_OPTIMIZING_MAX_FILE_CNT_DEFAULT);
    // not scan files to improve performance
    if (totalFilesInSummary <= maxFileCntLimit) {
      return Long.MAX_VALUE;
    }
    // scan and get all change files grouped by sequence(snapshot)
    ChangeTableIncrementalScan changeTableIncrementalScan =
        getArcticTable().asKeyedTable().changeTable().newChangeScan()
            .fromSequence(partitionOptimizedSequence)
            .fromLegacyTransaction(legacyPartitionMaxTransactionId)
            .useSnapshot(changeSnapshot.snapshotId());
    Map<Long, SnapshotFileGroup> changeFilesGroupBySequence = new HashMap<>();
    try (CloseableIterable<ContentFileWithSequence<?>> files = changeTableIncrementalScan.planFilesWithSequence()) {
      for (ContentFileWithSequence<?> file : files) {
        SnapshotFileGroup fileGroup =
            changeFilesGroupBySequence.computeIfAbsent(file.getSequenceNumber(), key -> {
              long txId = FileNameGenerator.parseChangeTransactionId(file.path().toString(), file.getSequenceNumber());
              return new SnapshotFileGroup(file.getSequenceNumber(), txId);
            });
        fileGroup.addFile();
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to close table scan of " + getArcticTable().name(), e);
    }

    if (changeFilesGroupBySequence.isEmpty()) {
      LOG.debug("{} get no change files to optimize with partitionOptimizedSequence {}", tableIdentifier,
          partitionOptimizedSequence);
      return Long.MIN_VALUE;
    }

    long maxSequence =
        getMaxSequenceKeepingTxIdInOrder(new ArrayList<>(changeFilesGroupBySequence.values()), maxFileCntLimit);
    if (maxSequence == Long.MIN_VALUE) {
      LOG.warn("{} get no change files with self-optimizing.max-file-count={}, change it to a bigger value",
          tableIdentifier, maxFileCntLimit);
    } else if (maxSequence != Long.MAX_VALUE) {
      LOG.warn("{} not all change files optimized with self-optimizing.max-file-count={}, maxSequence={}",
          tableIdentifier, maxFileCntLimit, maxSequence);
    }
    return maxSequence;
  }

  /**
   * Select all the files whose sequence <= maxSequence as Selected-Files, seek the maxSequence to find as many
   * Selected-Files as possible, and also
   * - the cnt of these Selected-Files must <= maxFileCntLimit
   * - the max TransactionId of the Selected-Files must > the min TransactionId of all the left files
   *
   * @param snapshotFileGroups snapshotFileGroups
   * @param maxFileCntLimit    maxFileCntLimit
   * @return the max sequence of selected file, return Long.MAX_VALUE if all files should be selected,
   * Long.MIN_VALUE means no files should be selected
   */
  static long getMaxSequenceKeepingTxIdInOrder(List<SnapshotFileGroup> snapshotFileGroups, long maxFileCntLimit) {
    if (maxFileCntLimit <= 0 || snapshotFileGroups == null || snapshotFileGroups.isEmpty()) {
      return Long.MIN_VALUE;
    }
    // 1.sort sequence
    Collections.sort(snapshotFileGroups);
    // 2.find the max index where all file cnt <= maxFileCntLimit
    int index = -1;
    int fileCnt = 0;
    for (int i = 0; i < snapshotFileGroups.size(); i++) {
      fileCnt += snapshotFileGroups.get(i).getFileCnt();
      if (fileCnt <= maxFileCntLimit) {
        index = i;
      } else {
        break;
      }
    }
    // all files cnt <= maxFileCntLimit, return all files
    if (fileCnt <= maxFileCntLimit) {
      return Long.MAX_VALUE;
    }
    // if only check the first file groups, then the file cnt > maxFileCntLimit, no files should be selected
    if (index == -1) {
      return Long.MIN_VALUE;
    }

    // 3.wrap file group with the max TransactionId before and min TransactionId after
    List<SnapshotFileGroupWrapper> snapshotFileGroupWrappers = wrapMinMaxTransactionId(snapshotFileGroups);
    // 4.find the valid snapshotFileGroup
    while (true) {
      SnapshotFileGroupWrapper current = snapshotFileGroupWrappers.get(index);
      // check transaction id inorder: max transaction id before(inclusive) < min transaction id after
      if (Math.max(current.getFileGroup().getTransactionId(), current.getMaxTransactionIdBefore()) <
          current.getMinTransactionIdAfter()) {
        return current.getFileGroup().getSequence();
      }
      index--;
      if (index == -1) {
        return Long.MIN_VALUE;
      }
    }
  }

  private static List<SnapshotFileGroupWrapper> wrapMinMaxTransactionId(List<SnapshotFileGroup> snapshotFileGroups) {
    List<SnapshotFileGroupWrapper> wrappedList = new ArrayList<>();
    for (SnapshotFileGroup snapshotFileGroup : snapshotFileGroups) {
      wrappedList.add(new SnapshotFileGroupWrapper(snapshotFileGroup));
    }
    long maxValue = Long.MIN_VALUE;
    for (int i = 0; i < wrappedList.size(); i++) {
      SnapshotFileGroupWrapper wrapper = wrappedList.get(i);
      wrapper.setMaxTransactionIdBefore(maxValue);
      if (wrapper.getFileGroup().getTransactionId() > maxValue) {
        maxValue = wrapper.getFileGroup().getTransactionId();
      }
    }
    long minValue = Long.MAX_VALUE;
    for (int i = wrappedList.size() - 1; i >= 0; i--) {
      SnapshotFileGroupWrapper wrapper = wrappedList.get(i);
      wrapper.setMinTransactionIdAfter(minValue);
      if (wrapper.getFileGroup().getTransactionId() < minValue) {
        minValue = wrapper.getFileGroup().getTransactionId();
      }
    }
    return wrappedList;
  }

  /**
   * Get full optimize plan for iceberg tables.
   *
   * @param queueId     -
   * @param currentTime -
   * @return -
   */
  public IcebergFullOptimizePlan getIcebergFullPlan(List<FileScanTask> fileScanTasks,
                                                    int queueId,
                                                    long currentTime, long currentSnapshotId) {
    return new IcebergFullOptimizePlan(arcticTable, tableOptimizeRuntime, fileScanTasks, queueId, currentTime,
        currentSnapshotId);
  }

  /**
   * Get minor optimize plan for iceberg tables.
   *
   * @param queueId     -
   * @param currentTime -
   * @return -
   */
  public IcebergMinorOptimizePlan getIcebergMinorPlan(List<FileScanTask> fileScanTasks,
                                                      int queueId,
                                                      long currentTime, long currentSnapshotId) {
    return new IcebergMinorOptimizePlan(arcticTable, tableOptimizeRuntime, fileScanTasks, queueId, currentTime,
        currentSnapshotId);
  }

  /**
   * Get optimizeRuntime.
   *
   * @return -
   */
  public TableOptimizeRuntime getTableOptimizeRuntime() {
    return tableOptimizeRuntime;
  }

  /**
   * check whether table has optimize task
   * @return -
   */
  public boolean optimizeRunning() {
    return planning.get() || CollectionUtils.isNotEmpty(getOptimizeTasks());
  }

  public boolean startPlanIfNot() {
    return this.planning.compareAndSet(false, true);
  }

  public void finishPlan() {
    this.planning.set(false);
  }

  public boolean tableChanged(Snapshot snapshot) {
    if (snapshot == null) {
      return false;
    }
    long lastSnapshotId = tableOptimizeRuntime.getCurrentSnapshotId();
    LOG.debug("{} ==== currentSnapshotId={}, lastSnapshotId={}", tableIdentifier,
        snapshot.snapshotId(), lastSnapshotId);
    return snapshot.snapshotId() != lastSnapshotId;
  }

  public boolean changeStoreChanged(Snapshot snapshot) {
    if (snapshot == null) {
      return false;
    }
    long lastChangeSnapshotId = tableOptimizeRuntime.getCurrentChangeSnapshotId();
    LOG.debug("{} ==== currentChangeSnapshotId={}, lastChangeSnapshotId={}", tableIdentifier,
        snapshot.snapshotId(), lastChangeSnapshotId);
    return snapshot.snapshotId() != lastChangeSnapshotId;
  }

  /**
   * Persist after update table optimizeRuntime.
   */
  public void persistTableOptimizeRuntime() {
    try (SqlSession sqlSession = getSqlSession(true)) {
      TableOptimizeRuntimeMapper tableOptimizeRuntimeMapper =
          getMapper(sqlSession, TableOptimizeRuntimeMapper.class);
      tableOptimizeRuntimeMapper.updateTableOptimizeRuntime(tableOptimizeRuntime);
    }
  }

  private boolean isMinorOptimizing() {
    if (MapUtils.isEmpty(optimizeTasks)) {
      return false;
    }
    OptimizeTaskItem optimizeTaskItem = new ArrayList<>(optimizeTasks.values()).get(0);
    return optimizeTaskItem.getTaskId().getType() == OptimizeType.Minor;
  }

  /**
   * Files grouped by snapshot, but only with the file cnt.
   */
  static class SnapshotFileGroup implements Comparable<SnapshotFileGroup> {
    private final long sequence;
    private final long transactionId;
    private int fileCnt = 0;

    public SnapshotFileGroup(long sequence, long transactionId) {
      this.sequence = sequence;
      this.transactionId = transactionId;
    }

    public SnapshotFileGroup(long sequence, long transactionId, int fileCnt) {
      this.sequence = sequence;
      this.transactionId = transactionId;
      this.fileCnt = fileCnt;
    }

    public void addFile() {
      fileCnt++;
    }

    public long getTransactionId() {
      return transactionId;
    }

    public int getFileCnt() {
      return fileCnt;
    }

    public long getSequence() {
      return sequence;
    }

    @Override
    public int compareTo(SnapshotFileGroup o) {
      return Long.compare(this.sequence, o.sequence);
    }
  }

  /**
   * Wrap SnapshotFileGroup with max and min Transaction Id
   */
  private static class SnapshotFileGroupWrapper {
    private final SnapshotFileGroup fileGroup;
    // in the ordered file group list, the max transaction before this file group, Long.MIN_VALUE for the first
    private long maxTransactionIdBefore;
    // in the ordered file group list, the min transaction after this file group, Long.MAX_VALUE for the last
    private long minTransactionIdAfter;

    public SnapshotFileGroupWrapper(SnapshotFileGroup fileGroup) {
      this.fileGroup = fileGroup;
    }

    public SnapshotFileGroup getFileGroup() {
      return fileGroup;
    }

    public long getMaxTransactionIdBefore() {
      return maxTransactionIdBefore;
    }

    public void setMaxTransactionIdBefore(long maxTransactionIdBefore) {
      this.maxTransactionIdBefore = maxTransactionIdBefore;
    }

    public long getMinTransactionIdAfter() {
      return minTransactionIdAfter;
    }

    public void setMinTransactionIdAfter(long minTransactionIdAfter) {
      this.minTransactionIdAfter = minTransactionIdAfter;
    }
  }
}