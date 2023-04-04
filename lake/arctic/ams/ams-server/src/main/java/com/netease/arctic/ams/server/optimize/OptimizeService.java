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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.netease.arctic.ams.api.ErrorMessage;
import com.netease.arctic.ams.api.InvalidObjectException;
import com.netease.arctic.ams.api.NoSuchObjectException;
import com.netease.arctic.ams.api.OptimizeStatus;
import com.netease.arctic.ams.api.OptimizeTaskId;
import com.netease.arctic.ams.api.OptimizeTaskStat;
import com.netease.arctic.ams.server.ArcticMetaStore;
import com.netease.arctic.ams.server.config.ArcticMetaStoreConf;
import com.netease.arctic.ams.server.mapper.OptimizeHistoryMapper;
import com.netease.arctic.ams.server.mapper.OptimizeTaskRuntimesMapper;
import com.netease.arctic.ams.server.mapper.OptimizeTasksMapper;
import com.netease.arctic.ams.server.mapper.TableOptimizeRuntimeMapper;
import com.netease.arctic.ams.server.model.BasicOptimizeTask;
import com.netease.arctic.ams.server.model.OptimizeHistory;
import com.netease.arctic.ams.server.model.OptimizeTaskRuntime;
import com.netease.arctic.ams.server.model.TableMetadata;
import com.netease.arctic.ams.server.model.TableOptimizeRuntime;
import com.netease.arctic.ams.server.service.IJDBCService;
import com.netease.arctic.ams.server.service.IMetaService;
import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.ams.server.service.impl.OptimizeQueueService;
import com.netease.arctic.ams.server.utils.OptimizeStatusUtil;
import com.netease.arctic.ams.server.utils.ScheduledTasks;
import com.netease.arctic.ams.server.utils.ThreadPool;
import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.utils.CatalogUtil;
import com.netease.arctic.utils.CompatiblePropertyUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.iceberg.relocated.com.google.common.annotations.VisibleForTesting;
import org.apache.iceberg.util.PropertyUtil;
import org.apache.iceberg.util.Tasks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class OptimizeService extends IJDBCService implements IOptimizeService {
  private static final Logger LOG = LoggerFactory.getLogger(OptimizeService.class);

  private ScheduledTasks<TableIdentifier, OptimizeCheckTask> checkTasks;

  private final BlockingQueue<TableOptimizeItem> toCommitTables = new ArrayBlockingQueue<>(1000);

  private final ConcurrentHashMap<TableIdentifier, TableOptimizeItem> optimizeTables = new ConcurrentHashMap<>();
  private final Set<TableIdentifier> unOptimizeTables = Collections.synchronizedSet(new HashSet<>());

  private final OptimizeQueueService optimizeQueueService;
  private final IMetaService metaService;
  private volatile ExecutorService tablesSvc;
  private volatile boolean inited = false;

  public OptimizeService() {
    super();
    optimizeQueueService = ServiceContainer.getOptimizeQueueService();
    metaService = ServiceContainer.getMetaService();
    tablesSvc = getProcessTablesThreadPool();
    init();
  }

  private synchronized ExecutorService getProcessTablesThreadPool() {
    if (tablesSvc == null || tablesSvc.isShutdown() || tablesSvc.isTerminated()) {
      tablesSvc = Executors.newFixedThreadPool(loadTablePoolSize());
    }
    return tablesSvc;
  }

  private void init() {
    try {
      new Thread(() -> {
        try {
          LOG.info("OptimizeService init...");
          loadTables();
          initOptimizeTasksIntoOptimizeQueue();
          ScheduledExecutorService refreshTableEsv = Executors.newSingleThreadScheduledExecutor(
              new ThreadFactoryBuilder()
                  .setDaemon(false)
                  .setNameFormat("Optimize Refresh Tables Thread")
                  .build()
          );
          refreshTableEsv.scheduleAtFixedRate(
              this::refreshAndListTables,
              60000L,
              ArcticMetaStore.conf.getLong(ArcticMetaStoreConf.OPTIMIZE_REFRESH_TABLES_INTERVAL, 60000L),
              TimeUnit.MILLISECONDS);
          LOG.info("OptimizeService init completed");
        } catch (Exception e) {
          LOG.error("OptimizeService init failed", e);
        } finally {
          inited = true;
        }
      }).start();
      LOG.info("OptimizeService init async");
    } catch (Exception e) {
      LOG.error("OptimizeService init failed", e);
    }
  }

  @Override
  public boolean isInited() {
    return inited;
  }

  @Override
  public synchronized void checkOptimizeCheckTasks(long checkInterval) {
    try {
      LOG.info("Schedule Optimize Checker");
      if (!inited) {
        LOG.info("OptimizeService init not completed, not check optimize task");
        return;
      }
      if (checkTasks == null) {
        checkTasks = new ScheduledTasks<>(ThreadPool.Type.OPTIMIZE_CHECK);
      }
      checkTasks.checkRunningTask(
          optimizeTables.keySet(),
          () -> 0L,
          () -> checkInterval,
          OptimizeCheckTask::new,
          false);
      LOG.info("Schedule Optimize Checker finished with {} optimizing tables", optimizeTables.size());
    } catch (Throwable t) {
      LOG.error("unexpected error when checkOptimizeCheckTasks", t);
    }
  }

  @Override
  public List<TableIdentifier> listCachedTables() {
    return new ArrayList<>(optimizeTables.keySet());
  }

  /**
   * The AMS optimization service should monitor all tables in the catalog for changes, including whether the table
   * exists and whether the table the status of self-optimizing enabled/disabled.
   * Refresh self-optimizing enabled{@link #optimizeTables} and disabled table{@link #unOptimizeTables} set.
   * @return valid table identifiers
   */
  @Override
  public List<TableIdentifier> refreshAndListTables() {
    LOG.info("refresh tables");
    if (!inited) {
      LOG.info("OptimizeService init not completed, not refresh");
      return new ArrayList<>(optimizeTables.keySet());
    }
    long startTimestamp = System.currentTimeMillis();
    Set<TableIdentifier> tableIdentifiers =
        new HashSet<>(com.netease.arctic.ams.server.utils.CatalogUtil.loadTablesFromCatalog());
    try {
      // brand-new tables
      List<TableIdentifier> toAddTables = tableIdentifiers.stream()
          .filter(t -> !getValidTables().contains(t))
          .collect(Collectors.toList());
      addNewTables(toAddTables);
      // removed tables
      List<TableIdentifier> toRemoveTables = getValidTables().stream()
          .filter(t -> !tableIdentifiers.contains(t))
          .collect(Collectors.toList());
      clearRemovedTables(toRemoveTables);

      // update optimizing tables set
      addOptimizingTables();
      // clean tasks when self-optimizing disabled
      clearUnOptimizingTables();
    } finally {
      tablesSvc.shutdown();
    }

    LOG.info("Refresh tables complete, cost {} ms", System.currentTimeMillis() - startTimestamp);

    return new ArrayList<>(optimizeTables.keySet());
  }

  /**
   * clean self-optimizing table and related tasks
   * @param tableIdentifier Arctic table identifier
   * @param pureDelete whether delete history records and table runtime in sysdb
   */
  private void clearOptimizeTable(TableIdentifier tableIdentifier, boolean pureDelete) {
    if (!optimizeTables.containsKey(tableIdentifier)) {
      return;
    }
    TableOptimizeItem tableItem = optimizeTables.remove(tableIdentifier);
    optimizeQueueService.release(tableIdentifier);
    try {
      tableItem.optimizeTasksClear(false);
    } catch (Throwable t) {
      LOG.debug("failed to delete " + tableIdentifier + " optimize task, ignore", t);
    }
    if (pureDelete) {
      try {
        deleteTableOptimizeRuntime(tableIdentifier);
      } catch (Throwable t) {
        LOG.debug("failed to delete  " + tableIdentifier + " runtime, ignore", t);
      }
      try {
        deleteOptimizeRecord(tableIdentifier);
        deleteOptimizeTaskHistory(tableIdentifier);
      } catch (Throwable t) {
        LOG.debug("failed to delete " + tableIdentifier + " optimize(task) history, ignore", t);
      }
    }
  }

  @VisibleForTesting
  void addTableIntoCache(TableOptimizeItem arcticTableItem, Map<String, String> properties,
                         boolean persistRuntime) {
    optimizeTables.put(arcticTableItem.getTableIdentifier(), arcticTableItem);
    try {
      String groupName = CompatiblePropertyUtil.propertyAsString(properties,
          TableProperties.SELF_OPTIMIZING_GROUP, TableProperties.SELF_OPTIMIZING_GROUP_DEFAULT);
      optimizeQueueService.bind(arcticTableItem.getTableIdentifier(), groupName);
    } catch (InvalidObjectException e) {
      LOG.debug("failed to bind " + arcticTableItem.getTableIdentifier() + " and queue ", e);
    }
    if (persistRuntime) {
      persistTableRuntime(arcticTableItem.getTableOptimizeRuntime());
    }
  }

  private void persistTableRuntime(TableOptimizeRuntime tableOptimizeRuntime) {
    try {
      insertTableOptimizeRuntime(tableOptimizeRuntime);
    } catch (Throwable t) {
      LOG.debug("failed to insert " + tableOptimizeRuntime.getTableIdentifier() + " runtime, ignore", t);
    }
  }

  @Override
  public TableOptimizeItem getTableOptimizeItem(TableIdentifier tableIdentifier) throws NoSuchObjectException {
    TableOptimizeItem tableOptimizeItem = optimizeTables.get(tableIdentifier);
    if (tableOptimizeItem == null) {
      refreshAndListTables();
      TableOptimizeItem reloadTableOptimizeItem = optimizeTables.get(tableIdentifier);
      if (reloadTableOptimizeItem == null) {
        throw new NoSuchObjectException("can't find table " + tableIdentifier);
      }
      return reloadTableOptimizeItem;
    }
    return tableOptimizeItem;
  }

  @Override
  public void handleOptimizeResult(OptimizeTaskStat optimizeTaskStat) throws NoSuchObjectException {
    getTableOptimizeItem(new TableIdentifier(optimizeTaskStat.getTableIdentifier()))
        .updateOptimizeTaskStat(optimizeTaskStat);
  }

  @Override
  public void startOptimize(TableIdentifier tableIdentifier) throws NoSuchObjectException {
    com.netease.arctic.ams.server.utils.CatalogUtil.getArcticCatalog(tableIdentifier.getCatalog())
        .loadTable(tableIdentifier)
        .updateProperties()
        .set(TableProperties.ENABLE_SELF_OPTIMIZING, "true")
        .commit();
  }

  @Override
  public void stopOptimize(TableIdentifier tableIdentifier) throws NoSuchObjectException {
    try {
      com.netease.arctic.ams.server.utils.CatalogUtil.getArcticCatalog(tableIdentifier.getCatalog())
          .loadTable(tableIdentifier)
          .updateProperties()
          .set(TableProperties.ENABLE_SELF_OPTIMIZING, "false")
          .commit();
    } finally {
      getTableOptimizeItem(tableIdentifier).optimizeTasksClear(true);
    }
  }

  private void loadTables() {
    LOG.info("init load tables");
    // load table when server start, only load table metadata
    Map<TableIdentifier, List<OptimizeTaskItem>> optimizeTasks = loadOptimizeTasks();
    Map<TableIdentifier, TableOptimizeRuntime> tableOptimizeRuntimes = loadTableOptimizeRuntimes();

    // load tables from catalog
    Set<TableIdentifier> tableIdentifiers = com.netease.arctic.ams.server.utils.CatalogUtil.loadTablesFromCatalog();

    try {
      parallelProcessTables(tableIdentifiers, tableIdentifier -> {
        List<OptimizeTaskItem> tableOptimizeTasks = optimizeTasks.remove(tableIdentifier);

        TableMetadata tableMetadata = buildTableMetadata(tableIdentifier);
        TableOptimizeItem arcticTableItem = new TableOptimizeItem(null, tableMetadata);
        TableOptimizeRuntime oldTableOptimizeRuntime = tableOptimizeRuntimes.remove(tableIdentifier);
        arcticTableItem.initTableOptimizeRuntime(oldTableOptimizeRuntime)
            .initOptimizeTasks(tableOptimizeTasks);
        if (CompatiblePropertyUtil.propertyAsBoolean(
            tableMetadata.getProperties(),
            TableProperties.ENABLE_SELF_OPTIMIZING,
            TableProperties.ENABLE_SELF_OPTIMIZING_DEFAULT)) {
          addTableIntoCache(arcticTableItem, tableMetadata.getProperties(), oldTableOptimizeRuntime == null);
        } else {
          unOptimizeTables.add(tableIdentifier);
          persistTableRuntime(arcticTableItem.getTableOptimizeRuntime());
        }
      });
    } finally {
      tablesSvc.shutdown();
    }

    if (!optimizeTasks.isEmpty()) {
      LOG.warn("clear optimize tasks {}", optimizeTasks.keySet());
      for (Map.Entry<TableIdentifier, List<OptimizeTaskItem>> entry : optimizeTasks.entrySet()) {
        for (OptimizeTaskItem task : entry.getValue()) {
          task.clearOptimizeTask();
        }
      }
    }

    if (!tableOptimizeRuntimes.isEmpty()) {
      LOG.warn("clear table runtime {}", tableOptimizeRuntimes.keySet());
      for (TableIdentifier tableIdentifier : tableOptimizeRuntimes.keySet()) {
        deleteTableOptimizeRuntime(tableIdentifier);
      }
    }
  }

  private TableMetadata buildTableMetadata(TableIdentifier tableIdentifier) {
    ArcticCatalog arcticCatalog =
        com.netease.arctic.ams.server.utils.CatalogUtil.getArcticCatalog(tableIdentifier.getCatalog());

    TableMetadata tableMetadata = new TableMetadata();
    if (CatalogUtil.isIcebergCatalog(arcticCatalog)) {
      ArcticTable arcticTable = arcticCatalog.loadTable(tableIdentifier);
      tableMetadata.setTableIdentifier(tableIdentifier);
      tableMetadata.setProperties(arcticTable.properties());
    } else {
      tableMetadata = metaService.loadTableMetadata(tableIdentifier);
    }
    return tableMetadata;
  }

  private void initOptimizeTasksIntoOptimizeQueue() {
    ArrayListMultimap<Integer, OptimizeTaskItem> multiMap = ArrayListMultimap.create();
    optimizeTables.values().stream().flatMap(t -> t.getOptimizeTasks().stream())
        .filter(t -> OptimizeStatusUtil.in(t.getOptimizeStatus(), OptimizeStatus.Pending))
        .forEach(t -> multiMap.put(t.getOptimizeTask().getQueueId(), t));
    for (Integer queueId : multiMap.keySet()) {
      List<OptimizeTaskItem> optimizeTaskItems = multiMap.get(queueId);
      optimizeTaskItems
          .sort(Comparator.comparingLong(o -> o.getOptimizeRuntime().getPendingTime()));
      for (OptimizeTaskItem task : optimizeTaskItems) {
        try {
          optimizeQueueService.submitTask(task);
        } catch (NoSuchObjectException | InvalidObjectException e) {
          LOG.error("failed to load task {} into optimizeQueue {}", task.getOptimizeTask(), queueId);
          task.onFailed(new ErrorMessage(System.currentTimeMillis(), e.getMessage()), 0L);
        }
      }
    }
  }

  private void addNewTables(List<TableIdentifier> toAddTables) {
    if (!inited) {
      LOG.info("OptimizeService init not completed, can't add new tables");
      return;
    }
    parallelProcessTables(toAddTables, this::addNewTable);
    LOG.info("add tables[{}] {}", toAddTables.size(), toAddTables);
  }

  @Override
  public void addNewTable(TableIdentifier toAddTable) {
    checkIfEnabledSelfOptimizing(com.netease.arctic.ams.server.utils.CatalogUtil
        .getArcticCatalog(toAddTable.getCatalog())
        .loadTable(toAddTable));
    LOG.info("add table {}", toAddTable);
  }

  private void addOptimizingTables() {
    if (!inited) {
      LOG.info("OptimizeService init not completed, can't add optimizing tables");
      return;
    }
    parallelProcessTables(unOptimizeTables, unOptimizeTable ->
        checkIfEnabledSelfOptimizing(com.netease.arctic.ams.server.utils.CatalogUtil
            .getArcticCatalog(unOptimizeTable.getCatalog())
            .loadTable(unOptimizeTable)));
  }

  private void parallelProcessTables(Iterable<TableIdentifier> tableIdentifiers,
      Tasks.Task<TableIdentifier, RuntimeException> task) {
    if (Iterables.isEmpty(tableIdentifiers)) {
      return;
    }

    Tasks.foreach(tableIdentifiers)
        .suppressFailureWhenFinished()
        .noRetry()
        .executeWith(getProcessTablesThreadPool())
        .run(task);
  }

  private void clearRemovedTables(List<TableIdentifier> toRemoveTables) {
    if (!inited) {
      LOG.info("OptimizeService init not completed, can't remove tables");
      return;
    }
    parallelProcessTables(toRemoveTables, this::clearRemovedTable);
    LOG.info("clear tables[{}] {}", toRemoveTables.size(), toRemoveTables);
  }

  @Override
  public void clearRemovedTable(TableIdentifier toRemoveTable) {
    clearOptimizeTable(toRemoveTable, true);
    unOptimizeTables.remove(toRemoveTable);
    LOG.info("clear table {}", toRemoveTable);
  }

  private void clearUnOptimizingTables() {
    if (!inited) {
      LOG.info("OptimizeService init not completed, can't update un-optimizing tables");
      return;
    }
    parallelProcessTables(optimizeTables.keySet(),
        optimizeTable -> checkIfDisabledSelfOptimizing(optimizeTables.get(optimizeTable).getArcticTable()));
  }

  private int loadTablePoolSize() {
    return Math.max(2, Runtime.getRuntime().availableProcessors());
  }

  private void checkIfEnabledSelfOptimizing(ArcticTable table) {
    TableIdentifier tableIdentifier = table.id();
    TableMetadata tableMetadata = buildTableMetadata(tableIdentifier);
    if (!CompatiblePropertyUtil.propertyAsBoolean(
        tableMetadata.getProperties(),
        TableProperties.ENABLE_SELF_OPTIMIZING,
        TableProperties.ENABLE_SELF_OPTIMIZING_DEFAULT)) {
      if (!unOptimizeTables.contains(tableIdentifier)) {
        unOptimizeTables.add(tableIdentifier);
        persistTableRuntime(new TableOptimizeRuntime(tableIdentifier));
      }
      return;
    }

    TableOptimizeItem newTableItem = new TableOptimizeItem(table, tableMetadata);
    long createTime =
        PropertyUtil.propertyAsLong(
            tableMetadata.getProperties(),
            TableProperties.TABLE_CREATE_TIME,
            TableProperties.TABLE_CREATE_TIME_DEFAULT);
    newTableItem.getTableOptimizeRuntime().setOptimizeStatusStartTime(createTime);
    addTableIntoCache(newTableItem, tableMetadata.getProperties(), true);
    LOG.info("Enable self-optimizing table: {}", tableIdentifier);
  }

  private void checkIfDisabledSelfOptimizing(ArcticTable table) {
    TableIdentifier tableIdentifier = table.id();
    if (CompatiblePropertyUtil.propertyAsBoolean(
        table.properties(),
        TableProperties.ENABLE_SELF_OPTIMIZING,
        TableProperties.ENABLE_SELF_OPTIMIZING_DEFAULT)) {
      unOptimizeTables.remove(tableIdentifier);
      return;
    }

    clearOptimizeTable(tableIdentifier, false);
    unOptimizeTables.add(tableIdentifier);
    LOG.info("Disable self-optimizing table: {}", tableIdentifier);
  }

  private Map<TableIdentifier, List<OptimizeTaskItem>> loadOptimizeTasks() {
    Map<TableIdentifier, List<OptimizeTaskItem>> results = new HashMap<>();

    List<BasicOptimizeTask> optimizeTasks = selectAllOptimizeTasks();

    for (BasicOptimizeTask optimizeTask : optimizeTasks) {
      initOptimizeTask(optimizeTask);
    }
    Map<OptimizeTaskId, OptimizeTaskRuntime> optimizeTaskRuntimes =
        selectAllOptimizeTaskRuntimes().stream()
            .collect(Collectors.toMap(OptimizeTaskRuntime::getOptimizeTaskId, r -> r));
    AtomicBoolean lostTaskRuntime = new AtomicBoolean(false);
    List<OptimizeTaskItem> optimizeTaskItems = optimizeTasks.stream()
        .map(t -> {
          OptimizeTaskRuntime optimizeTaskRuntime = optimizeTaskRuntimes.get(t.getTaskId());
          if (optimizeTaskRuntime == null) {
            lostTaskRuntime.set(true);
            LOG.error("can't find optimize task runtime in sysdb, tableIdentifier = {}, taskId = {}",
                t.getTableIdentifier(), t.getTaskId());
          }
          return new OptimizeTaskItem(t,
              optimizeTaskRuntimes.getOrDefault(t.getTaskId(), new OptimizeTaskRuntime(t.getTaskId())));
        })
        .collect(Collectors.toList());

    if (lostTaskRuntime.get()) {
      throw new IllegalStateException("sysdb error, lost some task runtimes, fix sysdb first");
    }

    for (OptimizeTaskItem optimizeTaskItem : optimizeTaskItems) {
      TableIdentifier tableIdentifier = optimizeTaskItem.getTableIdentifier();
      List<OptimizeTaskItem> optimizeTaskItemList = results.computeIfAbsent(tableIdentifier, e -> new ArrayList<>());
      optimizeTaskItemList.add(optimizeTaskItem);
    }
    return results;
  }

  private void initOptimizeTask(BasicOptimizeTask optimizeTask) {
    if (optimizeTask.getInsertFiles() == null) {
      optimizeTask.setInsertFiles(Collections.emptyList());
    }
    if (optimizeTask.getDeleteFiles() == null) {
      optimizeTask.setDeleteFiles(Collections.emptyList());
    }
    if (optimizeTask.getBaseFiles() == null) {
      optimizeTask.setBaseFiles(Collections.emptyList());
    }
    if (optimizeTask.getPosDeleteFiles() == null) {
      optimizeTask.setPosDeleteFiles(Collections.emptyList());
    }
  }

  private Map<TableIdentifier, TableOptimizeRuntime> loadTableOptimizeRuntimes() {
    Map<TableIdentifier, TableOptimizeRuntime> collector = new HashMap<>();
    List<TableOptimizeRuntime> tableOptimizeRuntimes = selectTableOptimizeRuntimes();
    for (TableOptimizeRuntime runtime : tableOptimizeRuntimes) {
      collector.put(runtime.getTableIdentifier(), runtime);
    }
    return collector;
  }

  private void deleteOptimizeRecord(TableIdentifier tableIdentifier) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      OptimizeHistoryMapper optimizeHistoryMapper =
          getMapper(sqlSession, OptimizeHistoryMapper.class);
      optimizeHistoryMapper.deleteOptimizeRecord(tableIdentifier);
    }
  }

  private void deleteOptimizeTaskHistory(TableIdentifier tableIdentifier) {
    ServiceContainer.getTableTaskHistoryService().deleteTaskHistory(tableIdentifier);
  }

  private Set<TableIdentifier> getValidTables() {
    return StreamSupport.stream(Iterables.concat(optimizeTables.keySet(), unOptimizeTables).spliterator(), false)
        .collect(Collectors.toSet());
  }

  @Override
  public List<OptimizeHistory> getOptimizeHistory(TableIdentifier identifier) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      OptimizeHistoryMapper optimizeHistoryMapper =
          getMapper(sqlSession, OptimizeHistoryMapper.class);

      return optimizeHistoryMapper.selectOptimizeHistory(identifier);
    }
  }

  @Override
  public Long getLatestCommitTime(TableIdentifier identifier) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      OptimizeHistoryMapper optimizeHistoryMapper =
          getMapper(sqlSession, OptimizeHistoryMapper.class);

      Timestamp latestCommitTime = optimizeHistoryMapper.latestCommitTime(identifier);
      if (latestCommitTime == null) {
        return 0L;
      }
      return latestCommitTime.getTime();
    }
  }

  @Override
  public long maxOptimizeHistoryId() {
    try (SqlSession sqlSession = getSqlSession(true)) {
      OptimizeHistoryMapper optimizeHistoryMapper =
          getMapper(sqlSession, OptimizeHistoryMapper.class);
      Long maxId = optimizeHistoryMapper.maxOptimizeHistoryId();
      return maxId == null ? 0 : maxId;
    }
  }

  @Override
  public boolean triggerOptimizeCommit(TableOptimizeItem tableOptimizeItem) {
    return toCommitTables.offer(tableOptimizeItem);
  }

  @Override
  public TableOptimizeItem takeTableToCommit() throws InterruptedException {
    return toCommitTables.take();
  }

  @Override
  public void expireOptimizeHistory(TableIdentifier tableIdentifier, long expireTime) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      OptimizeHistoryMapper optimizeHistoryMapper =
          getMapper(sqlSession, OptimizeHistoryMapper.class);
      optimizeHistoryMapper.expireOptimizeHistory(tableIdentifier, expireTime);
    }
  }

  private List<OptimizeTaskRuntime> selectAllOptimizeTaskRuntimes() {
    try (SqlSession sqlSession = getSqlSession(true)) {
      OptimizeTaskRuntimesMapper optimizeTaskRuntimesMapper =
          getMapper(sqlSession, OptimizeTaskRuntimesMapper.class);
      return optimizeTaskRuntimesMapper.selectAllOptimizeTaskRuntimes();
    }
  }

  private List<BasicOptimizeTask> selectAllOptimizeTasks() {
    try (SqlSession sqlSession = getSqlSession(true)) {
      OptimizeTasksMapper optimizeTasksMapper =
          getMapper(sqlSession, OptimizeTasksMapper.class);
      return optimizeTasksMapper.selectAllOptimizeTasks();
    }
  }

  // table runtime
  private void insertTableOptimizeRuntime(TableOptimizeRuntime tableOptimizeRuntime) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      TableOptimizeRuntimeMapper tableOptimizeRuntimeMapper =
          getMapper(sqlSession, TableOptimizeRuntimeMapper.class);
      tableOptimizeRuntimeMapper.insertTableOptimizeRuntime(tableOptimizeRuntime);
    }
  }

  private void deleteTableOptimizeRuntime(TableIdentifier tableIdentifier) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      TableOptimizeRuntimeMapper tableOptimizeRuntimeMapper =
          getMapper(sqlSession, TableOptimizeRuntimeMapper.class);
      tableOptimizeRuntimeMapper.deleteTableOptimizeRuntime(tableIdentifier);
    }
  }

  private List<TableOptimizeRuntime> selectTableOptimizeRuntimes() {
    try (SqlSession sqlSession = getSqlSession(true)) {
      TableOptimizeRuntimeMapper tableOptimizeRuntimeMapper =
          getMapper(sqlSession, TableOptimizeRuntimeMapper.class);
      return tableOptimizeRuntimeMapper.selectTableOptimizeRuntimes();
    }
  }
}