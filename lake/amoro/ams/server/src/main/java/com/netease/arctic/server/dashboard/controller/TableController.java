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

package com.netease.arctic.server.dashboard.controller;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.Constants;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.CatalogLoader;
import com.netease.arctic.hive.HiveTableProperties;
import com.netease.arctic.hive.catalog.ArcticHiveCatalog;
import com.netease.arctic.hive.utils.HiveTableUtil;
import com.netease.arctic.hive.utils.UpgradeHiveTableUtil;
import com.netease.arctic.server.catalog.IcebergCatalogImpl;
import com.netease.arctic.server.catalog.InternalIcebergCatalogImpl;
import com.netease.arctic.server.catalog.MixedHiveCatalogImpl;
import com.netease.arctic.server.catalog.ServerCatalog;
import com.netease.arctic.server.dashboard.ServerTableDescriptor;
import com.netease.arctic.server.dashboard.ServerTableProperties;
import com.netease.arctic.server.dashboard.model.AMSColumnInfo;
import com.netease.arctic.server.dashboard.model.AMSDataFileInfo;
import com.netease.arctic.server.dashboard.model.AMSPartitionField;
import com.netease.arctic.server.dashboard.model.AMSTransactionsOfTable;
import com.netease.arctic.server.dashboard.model.DDLInfo;
import com.netease.arctic.server.dashboard.model.FilesStatistics;
import com.netease.arctic.server.dashboard.model.HiveTableInfo;
import com.netease.arctic.server.dashboard.model.OptimizingProcessInfo;
import com.netease.arctic.server.dashboard.model.PartitionBaseInfo;
import com.netease.arctic.server.dashboard.model.PartitionFileBaseInfo;
import com.netease.arctic.server.dashboard.model.ServerTableMeta;
import com.netease.arctic.server.dashboard.model.TableBasicInfo;
import com.netease.arctic.server.dashboard.model.TableMeta;
import com.netease.arctic.server.dashboard.model.TableOperation;
import com.netease.arctic.server.dashboard.model.TableStatistics;
import com.netease.arctic.server.dashboard.model.TransactionsOfTable;
import com.netease.arctic.server.dashboard.model.UpgradeHiveMeta;
import com.netease.arctic.server.dashboard.model.UpgradeRunningInfo;
import com.netease.arctic.server.dashboard.model.UpgradeStatus;
import com.netease.arctic.server.dashboard.response.OkResponse;
import com.netease.arctic.server.dashboard.response.PageResult;
import com.netease.arctic.server.dashboard.utils.AmsUtil;
import com.netease.arctic.server.dashboard.utils.CommonUtil;
import com.netease.arctic.server.dashboard.utils.TableStatCollector;
import com.netease.arctic.server.optimizing.OptimizingProcessMeta;
import com.netease.arctic.server.optimizing.OptimizingTaskMeta;
import com.netease.arctic.server.table.ServerTableIdentifier;
import com.netease.arctic.server.table.TableService;
import com.netease.arctic.server.utils.Configurations;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.table.UnkeyedTable;
import io.javalin.http.Context;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.util.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Table controller.
 */
public class TableController {
  private static final Logger LOG = LoggerFactory.getLogger(TableController.class);
  private static final long UPGRADE_INFO_EXPIRE_INTERVAL = 60 * 60 * 1000;

  private final TableService tableService;
  private final ServerTableDescriptor tableDescriptor;
  private final Configurations serviceConfig;
  private final ConcurrentHashMap<TableIdentifier, UpgradeRunningInfo> upgradeRunningInfo = new ConcurrentHashMap<>();
  private final ScheduledExecutorService tableUpgradeExecutor;

  public TableController(
      TableService tableService,
      ServerTableDescriptor tableDescriptor,
      Configurations serviceConfig) {
    this.tableService = tableService;
    this.tableDescriptor = tableDescriptor;
    this.serviceConfig = serviceConfig;
    this.tableUpgradeExecutor = Executors.newScheduledThreadPool(
        0,
        new ThreadFactoryBuilder()
            .setDaemon(false)
            .setNameFormat("ASYNC-HIVE-TABLE-UPGRADE-%d").build());
  }

  /**
   * get table detail.
   *
   * @param ctx - context for handling the request and response
   */
  public void getTableDetail(Context ctx) {

    String catalog = ctx.pathParam("catalog");
    String database = ctx.pathParam("db");
    String tableMame = ctx.pathParam("table");

    Preconditions.checkArgument(
        StringUtils.isNotBlank(catalog) && StringUtils.isNotBlank(database) && StringUtils.isNotBlank(tableMame),
        "catalog.database.tableName can not be empty in any element");
    Preconditions.checkState(tableService.catalogExist(catalog), "invalid catalog!");

    ArcticTable table = tableService.loadTable(ServerTableIdentifier.of(catalog, database, tableMame));
    // set basic info
    TableBasicInfo tableBasicInfo = getTableBasicInfo(table);
    ServerTableMeta serverTableMeta = getServerTableMeta(table);
    long tableSize = 0;
    long tableFileCnt = 0;
    Map<String, Object> baseMetrics = Maps.newHashMap();
    FilesStatistics baseFilesStatistics = tableBasicInfo.getBaseStatistics().getTotalFilesStat();
    Map<String, String> baseSummary = tableBasicInfo.getBaseStatistics().getSummary();
    baseMetrics.put("lastCommitTime", AmsUtil.longOrNull(baseSummary.get("visibleTime")));
    baseMetrics.put("totalSize", AmsUtil.byteToXB(baseFilesStatistics.getTotalSize()));
    baseMetrics.put("fileCount", baseFilesStatistics.getFileCnt());
    baseMetrics.put("averageFileSize", AmsUtil.byteToXB(baseFilesStatistics.getAverageSize()));
    baseMetrics.put("baseWatermark", AmsUtil.longOrNull(serverTableMeta.getBaseWatermark()));
    tableSize += baseFilesStatistics.getTotalSize();
    tableFileCnt += baseFilesStatistics.getFileCnt();
    serverTableMeta.setBaseMetrics(baseMetrics);

    Map<String, Object> changeMetrics = Maps.newHashMap();
    if (tableBasicInfo.getChangeStatistics() != null) {
      FilesStatistics changeFilesStatistics = tableBasicInfo.getChangeStatistics().getTotalFilesStat();
      Map<String, String> changeSummary = tableBasicInfo.getChangeStatistics().getSummary();
      changeMetrics.put("lastCommitTime", AmsUtil.longOrNull(changeSummary.get("visibleTime")));
      changeMetrics.put("totalSize", AmsUtil.byteToXB(changeFilesStatistics.getTotalSize()));
      changeMetrics.put("fileCount", changeFilesStatistics.getFileCnt());
      changeMetrics.put("averageFileSize", AmsUtil.byteToXB(changeFilesStatistics.getAverageSize()));
      changeMetrics.put("tableWatermark", AmsUtil.longOrNull(serverTableMeta.getTableWatermark()));
      tableSize += changeFilesStatistics.getTotalSize();
      tableFileCnt += changeFilesStatistics.getFileCnt();
    } else {
      changeMetrics.put("lastCommitTime", null);
      changeMetrics.put("totalSize", null);
      changeMetrics.put("fileCount", null);
      changeMetrics.put("averageFileSize", null);
      changeMetrics.put("tableWatermark", null);
    }
    serverTableMeta.setChangeMetrics(changeMetrics);
    Set<TableFormat> tableFormats =
        com.netease.arctic.utils.CatalogUtil.tableFormats(tableService.getCatalogMeta(catalog));
    Preconditions.checkArgument(tableFormats.size() == 1, "Catalog support only one table format now.");
    TableFormat tableFormat = tableFormats.iterator().next();
    Map<String, Object> tableSummary = new HashMap<>();
    tableSummary.put("size", AmsUtil.byteToXB(tableSize));
    tableSummary.put("file", tableFileCnt);
    tableSummary.put("averageFile", AmsUtil.byteToXB(tableFileCnt == 0 ? 0 : tableSize / tableFileCnt));
    tableSummary.put("tableFormat", AmsUtil.formatString(tableFormat.name()));
    serverTableMeta.setTableSummary(tableSummary);
    ctx.json(OkResponse.of(serverTableMeta));
  }

  /**
   * get hive table detail.
   *
   * @param ctx - context for handling the request and response
   */
  public void getHiveTableDetail(Context ctx) {
    String catalog = ctx.pathParam("catalog");
    String db = ctx.pathParam("db");
    String table = ctx.pathParam("table");
    Preconditions.checkArgument(
        StringUtils.isNotBlank(catalog) && StringUtils.isNotBlank(db) && StringUtils.isNotBlank(table),
        "catalog.database.tableName can not be empty in any element");
    Preconditions.checkArgument(
        tableService.getServerCatalog(catalog) instanceof MixedHiveCatalogImpl,
        "catalog {} is not a mixed hive catalog, so not support load hive tables", catalog);

    // getRuntime table from catalog
    MixedHiveCatalogImpl arcticHiveCatalog = (MixedHiveCatalogImpl) tableService.getServerCatalog(catalog);

    TableIdentifier tableIdentifier = TableIdentifier.of(catalog, db, table);
    HiveTableInfo hiveTableInfo;
    Table hiveTable = HiveTableUtil.loadHmsTable(arcticHiveCatalog.getHiveClient(), tableIdentifier);
    List<AMSColumnInfo> schema = transformHiveSchemaToAMSColumnInfo(hiveTable.getSd().getCols());
    List<AMSColumnInfo> partitionColumnInfos = transformHiveSchemaToAMSColumnInfo(hiveTable.getPartitionKeys());
    hiveTableInfo = new HiveTableInfo(tableIdentifier, TableMeta.TableType.HIVE, schema, partitionColumnInfos,
        new HashMap<>(), hiveTable.getCreateTime());
    ctx.json(OkResponse.of(hiveTableInfo));
  }

  /**
   * upgrade hive table to arctic.
   *
   * @param ctx - context for handling the request and response
   */
  public void upgradeHiveTable(Context ctx) {
    String catalog = ctx.pathParam("catalog");
    String db = ctx.pathParam("db");
    String table = ctx.pathParam("table");
    Preconditions.checkArgument(
        StringUtils.isNotBlank(catalog) && StringUtils.isNotBlank(db) && StringUtils.isNotBlank(table),
        "catalog.database.tableName can not be empty in any element");
    UpgradeHiveMeta upgradeHiveMeta = ctx.bodyAsClass(UpgradeHiveMeta.class);

    ArcticHiveCatalog arcticHiveCatalog
        = (ArcticHiveCatalog) CatalogLoader.load(String.join("/",
        AmsUtil.getAMSThriftAddress(serviceConfig, Constants.THRIFT_TABLE_SERVICE_NAME),
        catalog));

    tableUpgradeExecutor.execute(() -> {
      TableIdentifier tableIdentifier = TableIdentifier.of(catalog, db, table);
      upgradeRunningInfo.put(tableIdentifier, new UpgradeRunningInfo());
      try {
        UpgradeHiveTableUtil.upgradeHiveTable(arcticHiveCatalog, TableIdentifier.of(catalog, db, table),
            upgradeHiveMeta.getPkList()
                .stream()
                .map(UpgradeHiveMeta.PrimaryKeyField::getFieldName)
                .collect(Collectors.toList()), upgradeHiveMeta.getProperties());
        upgradeRunningInfo.get(tableIdentifier).setStatus(UpgradeStatus.SUCCESS.toString());
      } catch (Throwable t) {
        LOG.error("Failed to upgrade hive table to arctic ", t);
        upgradeRunningInfo.get(tableIdentifier).setErrorMessage(AmsUtil.getStackTrace(t));
        upgradeRunningInfo.get(tableIdentifier).setStatus(UpgradeStatus.FAILED.toString());
      } finally {
        tableUpgradeExecutor.schedule(
            () -> upgradeRunningInfo.remove(tableIdentifier),
            UPGRADE_INFO_EXPIRE_INTERVAL,
            TimeUnit.MILLISECONDS);
      }
    });
    ctx.json(OkResponse.ok());
  }

  public void getUpgradeStatus(Context ctx) {
    String catalog = ctx.pathParam("catalog");
    String db = ctx.pathParam("db");
    String table = ctx.pathParam("table");
    UpgradeRunningInfo info = upgradeRunningInfo.containsKey(TableIdentifier.of(catalog, db, table)) ?
        upgradeRunningInfo.get(TableIdentifier.of(catalog, db, table)) :
        new UpgradeRunningInfo(UpgradeStatus.NONE.toString());
    ctx.json(OkResponse.of(info));
  }

  /**
   * get table properties for upgrading hive to arctic.
   *
   * @param ctx - context for handling the request and response
   */
  public void getUpgradeHiveTableProperties(Context ctx) throws IllegalAccessException {
    Map<String, String> keyValues = new TreeMap<>();
    Map<String, String> tableProperties =
        AmsUtil.getNotDeprecatedAndNotInternalStaticFields(TableProperties.class);
    tableProperties.keySet().stream()
        .filter(key -> !key.endsWith("_DEFAULT"))
        .forEach(
            key -> keyValues
                .put(tableProperties.get(key), tableProperties.get(key + "_DEFAULT")));
    ServerTableProperties.HIDDEN_EXPOSED.forEach(keyValues::remove);
    Map<String, String> hiveProperties =
        AmsUtil.getNotDeprecatedAndNotInternalStaticFields(HiveTableProperties.class);

    hiveProperties.keySet().stream()
        .filter(key -> HiveTableProperties.EXPOSED.contains(hiveProperties.get(key)))
        .filter(key -> !key.endsWith("_DEFAULT"))
        .forEach(
            key -> keyValues
                .put(hiveProperties.get(key), hiveProperties.get(key + "_DEFAULT")));
    ctx.json(OkResponse.of(keyValues));
  }

  /**
   * get list of optimizing processes.
   *
   * @param ctx - context for handling the request and response
   */
  public void getOptimizingProcesses(Context ctx) {

    String catalog = ctx.pathParam("catalog");
    String db = ctx.pathParam("db");
    String table = ctx.pathParam("table");
    Integer page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
    Integer pageSize = ctx.queryParamAsClass("pageSize", Integer.class).getOrDefault(20);

    int offset = (page - 1) * pageSize;
    int limit = pageSize;
    Preconditions.checkArgument(offset >= 0, "offset[%s] must >= 0", offset);
    Preconditions.checkArgument(limit >= 0, "limit[%s] must >= 0", limit);
    Preconditions.checkState(tableService.tableExist(new com.netease.arctic.ams.api.TableIdentifier(catalog, db,
        table)), "no such table");

    List<OptimizingProcessMeta> processMetaList = tableDescriptor.getOptimizingProcesses(catalog, db, table);
    int total = processMetaList.size();

    processMetaList = tableDescriptor.getOptimizingProcesses(catalog, db, table).stream()
        .skip(offset)
        .limit(limit)
        .collect(Collectors.toList());

    Map<Long, List<OptimizingTaskMeta>> optimizingTasks = tableDescriptor.getOptimizingTasks(processMetaList).stream()
        .collect(Collectors.groupingBy(OptimizingTaskMeta::getProcessId));

    List<OptimizingProcessInfo> result = processMetaList.stream()
        .map(p -> OptimizingProcessInfo.build(p, optimizingTasks.get(p.getProcessId())))
        .collect(Collectors.toList());

    ctx.json(OkResponse.of(PageResult.of(result, total)));
  }

  /**
   * get list of transactions.
   *
   * @param ctx - context for handling the request and response
   */
  public void getTableTransactions(Context ctx) {
    String catalogName = ctx.pathParam("catalog");
    String db = ctx.pathParam("db");
    String tableName = ctx.pathParam("table");
    Integer page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
    Integer pageSize = ctx.queryParamAsClass("pageSize", Integer.class).getOrDefault(20);

    List<TransactionsOfTable> transactionsOfTables =
        tableDescriptor.getTransactions(ServerTableIdentifier.of(catalogName, db, tableName));
    int offset = (page - 1) * pageSize;
    PageResult<AMSTransactionsOfTable> pageResult = PageResult.of(transactionsOfTables,
        offset, pageSize, AmsUtil::toTransactionsOfTable);
    ctx.json(OkResponse.of(pageResult));
  }

  /**
   * get detail of transaction.
   *
   * @param ctx - context for handling the request and response
   */
  public void getTransactionDetail(Context ctx) {
    String catalogName = ctx.pathParam("catalog");
    String db = ctx.pathParam("db");
    String tableName = ctx.pathParam("table");
    String transactionId = ctx.pathParam("transactionId");
    Integer page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
    Integer pageSize = ctx.queryParamAsClass("pageSize", Integer.class).getOrDefault(20);

    List<AMSDataFileInfo> result = tableDescriptor.getTransactionDetail(ServerTableIdentifier.of(catalogName, db,
        tableName), Long.parseLong(transactionId));
    int offset = (page - 1) * pageSize;
    PageResult<AMSDataFileInfo> amsPageResult = PageResult.of(result,
        offset, pageSize);
    ctx.json(OkResponse.of(amsPageResult));
  }

  /**
   * get partition list.
   *
   * @param ctx - context for handling the request and response
   */
  public void getTablePartitions(Context ctx) {
    String catalog = ctx.pathParam("catalog");
    String db = ctx.pathParam("db");
    String table = ctx.pathParam("table");
    Integer page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
    Integer pageSize = ctx.queryParamAsClass("pageSize", Integer.class).getOrDefault(20);

    ArcticTable arcticTable = tableService.loadTable(ServerTableIdentifier.of(catalog, db, table));
    List<PartitionBaseInfo> partitionBaseInfos = tableDescriptor.getTablePartition(arcticTable);
    int offset = (page - 1) * pageSize;
    PageResult<PartitionBaseInfo> amsPageResult = PageResult.of(partitionBaseInfos,
        offset, pageSize);
    ctx.json(OkResponse.of(amsPageResult));
  }

  /**
   * get file list of some partition.
   *
   * @param ctx - context for handling the request and response
   */
  public void getPartitionFileListInfo(Context ctx) {
    String catalog = ctx.pathParam("catalog");
    String db = ctx.pathParam("db");
    String table = ctx.pathParam("table");
    String partition = ctx.pathParam("partition");

    Integer page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
    Integer pageSize = ctx.queryParamAsClass("pageSize", Integer.class).getOrDefault(20);
    ArcticTable arcticTable = tableService.loadTable(ServerTableIdentifier.of(catalog, db, table));
    List<PartitionFileBaseInfo> partitionFileBaseInfos = tableDescriptor.getTableFile(arcticTable, partition);
    int offset = (page - 1) * pageSize;
    PageResult<PartitionFileBaseInfo> amsPageResult = PageResult.of(partitionFileBaseInfos,
        offset, pageSize);
    ctx.json(OkResponse.of(amsPageResult));
  }

  /**
   * get table operations.
   *
   * @param ctx - context for handling the request and response
   */
  public void getTableOperations(Context ctx) {
    String catalogName = ctx.pathParam("catalog");
    String db = ctx.pathParam("db");
    String tableName = ctx.pathParam("table");

    Integer page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
    Integer pageSize = ctx.queryParamAsClass("pageSize", Integer.class).getOrDefault(20);
    int offset = (page - 1) * pageSize;

    List<DDLInfo> ddlInfoList = tableDescriptor.getTableOperations(ServerTableIdentifier.of(catalogName, db,
        tableName));
    Collections.reverse(ddlInfoList);
    PageResult<TableOperation> amsPageResult = PageResult.of(ddlInfoList,
        offset, pageSize, TableOperation::buildFromDDLInfo);
    ctx.json(OkResponse.of(amsPageResult));
  }

  /**
   * get table list of catalog.db.
   *
   * @param ctx - context for handling the request and response
   */
  public void getTableList(Context ctx) {
    String catalog = ctx.pathParam("catalog");
    String db = ctx.pathParam("db");
    String keywords = ctx.queryParam("keywords");
    Preconditions.checkArgument(
        StringUtils.isNotBlank(catalog) && StringUtils.isNotBlank(db),
        "catalog.database can not be empty in any element");

    List<com.netease.arctic.ams.api.TableIdentifier> tableIdentifiers = tableService.listTables(catalog, db);
    ServerCatalog serverCatalog = tableService.getServerCatalog(catalog);
    List<TableMeta> tables = new ArrayList<>();

    if (serverCatalog instanceof IcebergCatalogImpl || serverCatalog instanceof InternalIcebergCatalogImpl) {
      tableIdentifiers.forEach(e -> tables.add(new TableMeta(
          e.getTableName(),
          TableMeta.TableType.ICEBERG.toString())));
    } else if (serverCatalog instanceof MixedHiveCatalogImpl) {
      tableIdentifiers.forEach(e -> tables.add(new TableMeta(e.getTableName(), TableMeta.TableType.ARCTIC.toString())));
      List<String> hiveTables = HiveTableUtil.getAllHiveTables(
          ((MixedHiveCatalogImpl) serverCatalog).getHiveClient(),
          db);
      Set<String> arcticTables =
          tableIdentifiers.stream()
              .map(com.netease.arctic.ams.api.TableIdentifier::getTableName)
              .collect(Collectors.toSet());
      hiveTables.stream().filter(e -> !arcticTables.contains(e)).forEach(e -> tables.add(new TableMeta(
          e,
          TableMeta.TableType.HIVE.toString())));
    } else {
      tableIdentifiers.forEach(e -> tables.add(new TableMeta(e.getTableName(), TableMeta.TableType.ARCTIC.toString())));
    }
    ctx.json(OkResponse.of(tables.stream().filter(t -> StringUtils.isBlank(keywords) ||
        t.getName().contains(keywords)).collect(Collectors.toList())));
  }

  /**
   * get databases of some catalog.
   *
   * @param ctx - context for handling the request and response
   */
  public void getDatabaseList(Context ctx) {
    String catalog = ctx.pathParam("catalog");
    String keywords = ctx.queryParam("keywords");

    List<String> dbList = tableService.listDatabases(catalog).stream()
        .filter(item -> StringUtils.isBlank(keywords) || item.contains(keywords))
        .collect(Collectors.toList());
    ctx.json(OkResponse.of(dbList));
  }

  /**
   * get list of catalogs.
   *
   * @param ctx - context for handling the request and response
   */
  public void getCatalogs(Context ctx) {
    List<CatalogMeta> catalogs = tableService.listCatalogMetas();
    ctx.json(OkResponse.of(catalogs));
  }

  /**
   * get single page query token.
   *
   * @param ctx - context for handling the request and response
   */
  public void getTableDetailTabToken(Context ctx) {
    String catalog = ctx.pathParam("catalog");
    String db = ctx.pathParam("db");
    String table = ctx.pathParam("table");

    String signCal = CommonUtil.generateTablePageToken(catalog, db, table);
    ctx.json(OkResponse.of(signCal));
  }

  private TableBasicInfo getTableBasicInfo(ArcticTable table) {
    try {
      TableBasicInfo tableBasicInfo = new TableBasicInfo();
      tableBasicInfo.setTableIdentifier(table.id());
      TableStatistics changeInfo = null;
      TableStatistics baseInfo;

      if (table.isUnkeyedTable()) {
        UnkeyedTable unkeyedTable = table.asUnkeyedTable();
        baseInfo = new TableStatistics();
        TableStatCollector.fillTableStatistics(baseInfo, unkeyedTable, table);
      } else if (table.isKeyedTable()) {
        KeyedTable keyedTable = table.asKeyedTable();
        if (!PrimaryKeySpec.noPrimaryKey().equals(keyedTable.primaryKeySpec())) {
          changeInfo = TableStatCollector.collectChangeTableInfo(keyedTable);
        }
        baseInfo = TableStatCollector.collectBaseTableInfo(keyedTable);
      } else {
        throw new IllegalStateException("unknown type of table");
      }

      tableBasicInfo.setChangeStatistics(changeInfo);
      tableBasicInfo.setBaseStatistics(baseInfo);
      tableBasicInfo.setTableStatistics(TableStatCollector.union(changeInfo, baseInfo));

      long createTime
          = PropertyUtil.propertyAsLong(table.properties(), TableProperties.TABLE_CREATE_TIME,
          TableProperties.TABLE_CREATE_TIME_DEFAULT);
      if (createTime != TableProperties.TABLE_CREATE_TIME_DEFAULT) {
        if (tableBasicInfo.getTableStatistics() != null) {
          if (tableBasicInfo.getTableStatistics().getSummary() == null) {
            tableBasicInfo.getTableStatistics().setSummary(new HashMap<>());
          } else {
            LOG.warn("{} summary is null", table.id());
          }
          tableBasicInfo.getTableStatistics().getSummary()
              .put("createTime", String.valueOf(createTime));
        } else {
          LOG.warn("{} table statistics is null {}", table.id(), tableBasicInfo);
        }
      }
      return tableBasicInfo;
    } catch (Throwable t) {
      LOG.error("{} failed to build table basic info", table.id(), t);
      throw t;
    }
  }

  private ServerTableMeta getServerTableMeta(ArcticTable table) {
    ServerTableMeta serverTableMeta = new ServerTableMeta();
    serverTableMeta.setTableType(table.format().toString());
    serverTableMeta.setTableIdentifier(table.id());
    serverTableMeta.setBaseLocation(table.location());
    fillTableProperties(serverTableMeta, table.properties());
    serverTableMeta.setPartitionColumnList(table
        .spec()
        .fields()
        .stream()
        .map(item -> AMSPartitionField.buildFromPartitionSpec(table.spec().schema(), item))
        .collect(Collectors.toList()));
    serverTableMeta.setSchema(table
        .schema()
        .columns()
        .stream()
        .map(AMSColumnInfo::buildFromNestedField)
        .collect(Collectors.toList()));

    serverTableMeta.setFilter(null);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Table " + table.name() + " is keyedTable: {}", table instanceof KeyedTable);
    }
    if (table.isKeyedTable()) {
      KeyedTable kt = table.asKeyedTable();
      if (kt.primaryKeySpec() != null) {
        serverTableMeta.setPkList(kt
            .primaryKeySpec()
            .fields()
            .stream()
            .map(item -> AMSColumnInfo.buildFromPartitionSpec(table.spec().schema(), item))
            .collect(Collectors.toList()));
      }
    }
    if (serverTableMeta.getPkList() == null) {
      serverTableMeta.setPkList(new ArrayList<>());
    }
    return serverTableMeta;
  }

  private void fillTableProperties(
      ServerTableMeta serverTableMeta,
      Map<String, String> tableProperties) {
    Map<String, String> properties = com.google.common.collect.Maps.newHashMap(tableProperties);
    serverTableMeta.setTableWatermark(properties.remove(TableProperties.WATERMARK_TABLE));
    serverTableMeta.setBaseWatermark(properties.remove(TableProperties.WATERMARK_BASE_STORE));
    serverTableMeta.setCreateTime(PropertyUtil.propertyAsLong(properties, TableProperties.TABLE_CREATE_TIME,
        TableProperties.TABLE_CREATE_TIME_DEFAULT));
    properties.remove(TableProperties.TABLE_CREATE_TIME);

    TableProperties.READ_PROTECTED_PROPERTIES.forEach(properties::remove);
    serverTableMeta.setProperties(properties);
  }

  private List<AMSColumnInfo> transformHiveSchemaToAMSColumnInfo(List<FieldSchema> fields) {
    return fields.stream()
        .map(f -> {
          AMSColumnInfo columnInfo = new AMSColumnInfo();
          columnInfo.setField(f.getName());
          columnInfo.setType(f.getType());
          columnInfo.setComment(f.getComment());
          return columnInfo;
        }).collect(Collectors.toList());
  }
}
