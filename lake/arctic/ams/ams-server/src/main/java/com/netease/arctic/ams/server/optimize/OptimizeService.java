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
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.netease.arctic.AmsClient;
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
import com.netease.arctic.catalog.CatalogLoader;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.utils.CatalogUtil;
import com.netease.arctic.utils.CompatiblePropertyUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.iceberg.relocated.com.google.common.annotations.VisibleForTesting;
import org.apache.iceberg.util.PropertyUtil;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class OptimizeService extends IJDBCService implements IOptimizeService {
  private static final Logger LOG = LoggerFactory.getLogger(OptimizeService.class);

  private ScheduledTasks<TableIdentifier, OptimizeCheckTask> checkTasks;

  private final BlockingQueue<TableOptimizeItem> toCommitTables = new ArrayBlockingQueue<>(1000);

  private final ConcurrentHashMap<TableIdentifier, TableOptimizeItem> cachedTables = new ConcurrentHashMap<>();
  private final Set<TableIdentifier> invalidTables = Collections.synchronizedSet(new HashSet<>());

  private final OptimizeQueueService optimizeQueueService;
  private final IMetaService metaService;
  private final AmsClient metastoreClient;
  private volatile boolean inited = false;

  public OptimizeService() {
    super();
    optimizeQueueService = ServiceContainer.getOptimizeQueueService();
    metaService = ServiceContainer.getMetaService();
    metastoreClient = ServiceContainer.getTableMetastoreHandler();
    init();
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
    LOG.info("OptimizeService inited {}", inited);
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
          cachedTables.keySet(),
          identifier -> checkInterval,
          OptimizeCheckTask::new,
          false);
      LOG.info("Schedule Optimize Checker finished with {} valid tables", cachedTables.keySet().size());
    } catch (Throwable t) {
      LOG.error("unexpected error when checkOptimizeCheckTasks", t);
    }
  }

  @Override
  public List<TableIdentifier> listCachedTables() {
    return new ArrayList<>(cachedTables.keySet());
  }

  @Override
  public List<TableIdentifier> refreshAndListTables() {
    LOG.info("refresh tables");
    if (!inited) {
      LOG.info("OptimizeService init not completed, not refresh");
      return new ArrayList<>(cachedTables.keySet());
    }
    Set<TableIdentifier> tableIdentifiers =
        new HashSet<>(com.netease.arctic.ams.server.utils.CatalogUtil.loadTablesFromCatalog());
    List<TableIdentifier> toAddTables = tableIdentifiers.stream()
        .filter(t -> !cachedTables.containsKey(t))
        .collect(Collectors.toList());
    List<TableIdentifier> toRemoveTables = cachedTables.keySet().stream()
        .filter(t -> !tableIdentifiers.contains(t))
        .collect(Collectors.toList());

    addNewTables(toAddTables);
    clearRemovedTables(toRemoveTables);

    return new ArrayList<>(cachedTables.keySet());
  }

  private void clearTableCache(TableIdentifier tableIdentifier) {
    TableOptimizeItem tableItem = cachedTables.remove(tableIdentifier);
    optimizeQueueService.release(tableIdentifier);
    try {
      tableItem.optimizeTasksClear(false);
    } catch (Throwable t) {
      LOG.debug("failed to delete " + tableIdentifier + " optimize task, ignore", t);
    }
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

  @VisibleForTesting
  void addTableIntoCache(TableOptimizeItem arcticTableItem, Map<String, String> properties,
                         boolean persistRuntime) {
    cachedTables.put(arcticTableItem.getTableIdentifier(), arcticTableItem);
    try {
      String groupName = CompatiblePropertyUtil.propertyAsString(properties,
          TableProperties.SELF_OPTIMIZING_GROUP, TableProperties.SELF_OPTIMIZING_GROUP_DEFAULT);
      optimizeQueueService.bind(arcticTableItem.getTableIdentifier(), groupName);
    } catch (InvalidObjectException e) {
      LOG.debug("failed to bind " + arcticTableItem.getTableIdentifier() + " and queue ", e);
    }
    if (persistRuntime) {
      try {
        insertTableOptimizeRuntime(arcticTableItem.getTableOptimizeRuntime());
      } catch (Throwable t) {
        LOG.debug("failed to insert " + arcticTableItem.getTableIdentifier() + " runtime, ignore", t);
      }
    }
  }

  @Override
  public TableOptimizeItem getTableOptimizeItem(TableIdentifier tableIdentifier) throws NoSuchObjectException {
    TableOptimizeItem tableOptimizeItem = cachedTables.get(tableIdentifier);
    if (tableOptimizeItem == null) {
      refreshAndListTables();
      TableOptimizeItem reloadTableOptimizeItem = cachedTables.get(tableIdentifier);
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

  private void loadTables() {
    LOG.info("init load tables");
    // load table when server start, only load table metadata
    Map<TableIdentifier, List<OptimizeTaskItem>> optimizeTasks = loadOptimizeTasks();
    Map<TableIdentifier, TableOptimizeRuntime> tableOptimizeRuntimes = loadTableOptimizeRuntimes();

    // load tables from catalog
    Set<TableIdentifier> tableIdentifiers = com.netease.arctic.ams.server.utils.CatalogUtil.loadTablesFromCatalog();

    Map<String, ArcticCatalog> icebergCatalogMap = new HashMap<>();
    for (TableIdentifier tableIdentifier : tableIdentifiers) {
      List<OptimizeTaskItem> tableOptimizeTasks = optimizeTasks.remove(tableIdentifier);
      try {
        TableMetadata tableMetadata = buildTableMetadata(icebergCatalogMap, tableIdentifier);
        TableOptimizeItem arcticTableItem = new TableOptimizeItem(null, tableMetadata);
        TableOptimizeRuntime oldTableOptimizeRuntime = tableOptimizeRuntimes.remove(tableIdentifier);
        arcticTableItem.initTableOptimizeRuntime(oldTableOptimizeRuntime)
            .initOptimizeTasks(tableOptimizeTasks);
        addTableIntoCache(arcticTableItem, tableMetadata.getProperties(), oldTableOptimizeRuntime == null);
      } catch (Throwable t) {
        LOG.error("failed to load  " + tableIdentifier, t);
        invalidTables.add(tableIdentifier);
      }
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

  private TableMetadata buildTableMetadata(Map<String, ArcticCatalog> icebergCatalogMap,
                                           TableIdentifier tableIdentifier) {
    TableMetadata tableMetadata = new TableMetadata();
    if (icebergCatalogMap.get(tableIdentifier.getCatalog()) != null) {
      ArcticTable arcticTable = icebergCatalogMap.get(tableIdentifier.getCatalog()).loadTable(tableIdentifier);
      tableMetadata.setTableIdentifier(tableIdentifier);
      tableMetadata.setProperties(arcticTable.properties());
    } else {
      ArcticCatalog arcticCatalog =
          CatalogLoader.load(ServiceContainer.getTableMetastoreHandler(), tableIdentifier.getCatalog());
      if (CatalogUtil.isIcebergCatalog(arcticCatalog)) {
        ArcticTable arcticTable = arcticCatalog.loadTable(tableIdentifier);
        tableMetadata.setTableIdentifier(tableIdentifier);
        tableMetadata.setProperties(arcticTable.properties());
        icebergCatalogMap.put(tableIdentifier.getCatalog(), arcticCatalog);
      } else {
        tableMetadata = metaService.loadTableMetadata(tableIdentifier);
      }
    }
    return tableMetadata;
  }

  private void initOptimizeTasksIntoOptimizeQueue() {
    ArrayListMultimap<Integer, OptimizeTaskItem> multiMap = ArrayListMultimap.create();
    cachedTables.values().stream().flatMap(t -> t.getOptimizeTasks().stream())
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

  @Override
  public void addNewTables(List<TableIdentifier> toAddTables) {
    if (CollectionUtils.isEmpty(toAddTables)) {
      return;
    }
    if (!inited) {
      LOG.info("OptimizeService init not completed, can't add new tables");
      return;
    }

    Map<String, ArcticCatalog> icebergCatalogMap = new HashMap<>();
    int success = 0;
    for (TableIdentifier toAddTable : toAddTables) {
      try {
        TableMetadata tableMetadata = buildTableMetadata(icebergCatalogMap, toAddTable);
        ArcticCatalog catalog = CatalogLoader.load(metastoreClient, toAddTable.getCatalog());
        ArcticTable arcticTable = catalog.loadTable(toAddTable);
        TableOptimizeItem newTableItem = new TableOptimizeItem(arcticTable, tableMetadata);
        long createTime = PropertyUtil.propertyAsLong(tableMetadata.getProperties(), TableProperties.TABLE_CREATE_TIME,
            TableProperties.TABLE_CREATE_TIME_DEFAULT);
        newTableItem.getTableOptimizeRuntime().setOptimizeStatusStartTime(createTime);
        addTableIntoCache(newTableItem, arcticTable.properties(), true);
        // remove recover table if it is present
        invalidTables.remove(toAddTable);
        success++;
      } catch (Throwable t) {
        // avoid printing too many error logs
        if (!invalidTables.contains(toAddTable)) {
          LOG.error("failed to load  " + toAddTable, t);
          invalidTables.add(toAddTable);
        }
      }
    }
    LOG.info("try add {} new tables, success {}, {}", toAddTables.size(), success, toAddTables);
  }

  @Override
  public void clearRemovedTables(List<TableIdentifier> toRemoveTables) {
    if (CollectionUtils.isEmpty(toRemoveTables)) {
      return;
    }
    if (!inited) {
      LOG.info("OptimizeService init not completed, can't add new tables");
      return;
    }
    toRemoveTables.forEach(this::clearTableCache);
    LOG.info("clear tables[{}] {}", toRemoveTables.size(), toRemoveTables);
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