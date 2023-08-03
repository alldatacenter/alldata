package com.netease.arctic.server.table;

import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import com.netease.arctic.ams.api.BlockableOperation;
import com.netease.arctic.ams.api.Blocker;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.TableIdentifier;
import com.netease.arctic.server.ArcticManagementConf;
import com.netease.arctic.server.catalog.CatalogBuilder;
import com.netease.arctic.server.catalog.ExternalCatalog;
import com.netease.arctic.server.catalog.InternalCatalog;
import com.netease.arctic.server.catalog.ServerCatalog;
import com.netease.arctic.server.exception.AlreadyExistsException;
import com.netease.arctic.server.exception.IllegalMetadataException;
import com.netease.arctic.server.exception.ObjectNotExistsException;
import com.netease.arctic.server.optimizing.OptimizingStatus;
import com.netease.arctic.server.persistence.StatedPersistentBase;
import com.netease.arctic.server.persistence.mapper.CatalogMetaMapper;
import com.netease.arctic.server.persistence.mapper.TableMetaMapper;
import com.netease.arctic.server.table.blocker.TableBlocker;
import com.netease.arctic.server.utils.Configurations;
import com.netease.arctic.table.ArcticTable;
import org.apache.commons.lang.StringUtils;
import org.apache.iceberg.relocated.com.google.common.annotations.VisibleForTesting;
import org.apache.iceberg.relocated.com.google.common.base.MoreObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DefaultTableService extends StatedPersistentBase implements TableService {

  public static final Logger LOG = LoggerFactory.getLogger(DefaultTableService.class);
  private final long externalCatalogRefreshingInterval;
  private final long blockerTimeout;
  private final Map<String, InternalCatalog> internalCatalogMap = new ConcurrentHashMap<>();
  private final Map<String, ExternalCatalog> externalCatalogMap = new ConcurrentHashMap<>();
  @StateField
  private final Map<ServerTableIdentifier, TableRuntime> tableRuntimeMap = new ConcurrentHashMap<>();
  private RuntimeHandlerChain headHandler;
  private Timer tableExplorerTimer;

  private final CompletableFuture<Boolean> initialized = new CompletableFuture<>();
  private final Configurations serverConfiguration;

  public DefaultTableService(Configurations configuration) {
    this.externalCatalogRefreshingInterval =
        configuration.getLong(ArcticManagementConf.REFRESH_EXTERNAL_CATALOGS_INTERVAL);
    this.blockerTimeout = configuration.getLong(ArcticManagementConf.BLOCKER_TIMEOUT);
    this.serverConfiguration = configuration;
  }

  @Override
  public List<CatalogMeta> listCatalogMetas() {
    checkStarted();
    List<CatalogMeta> catalogs = internalCatalogMap.values().stream()
        .map(ServerCatalog::getMetadata).collect(Collectors.toList());
    catalogs.addAll(externalCatalogMap.values().stream()
        .map(ServerCatalog::getMetadata).collect(Collectors.toList()));
    return catalogs;
  }

  @Override
  public CatalogMeta getCatalogMeta(String catalogName) {
    checkStarted();
    ServerCatalog catalog = getServerCatalog(catalogName);
    return catalog.getMetadata();
  }

  @Override
  public boolean catalogExist(String catalogName) {
    checkStarted();
    return internalCatalogMap.containsKey(catalogName) || externalCatalogMap.containsKey(catalogName);
  }

  @Override
  public ServerCatalog getServerCatalog(String catalogName) {
    ServerCatalog catalog = Optional.ofNullable((ServerCatalog) internalCatalogMap.get(catalogName))
        .orElse(externalCatalogMap.get(catalogName));
    return Optional.ofNullable(catalog).orElseThrow(() -> new ObjectNotExistsException("Catalog " + catalogName));
  }

  @Override
  public void createCatalog(CatalogMeta catalogMeta) {
    checkStarted();
    if (catalogExist(catalogMeta.getCatalogName())) {
      throw new AlreadyExistsException("Catalog " + catalogMeta.getCatalogName());
    }
    doAsTransaction(
        () -> doAs(CatalogMetaMapper.class, mapper -> mapper.insertCatalog(catalogMeta)),
        () -> initServerCatalog(catalogMeta));
  }

  private void initServerCatalog(CatalogMeta catalogMeta) {
    ServerCatalog catalog = CatalogBuilder.buildServerCatalog(catalogMeta, serverConfiguration);
    if (catalog instanceof InternalCatalog) {
      internalCatalogMap.put(catalogMeta.getCatalogName(), (InternalCatalog) catalog);
    } else {
      externalCatalogMap.put(catalogMeta.getCatalogName(), (ExternalCatalog) catalog);
    }
  }

  @Override
  public void dropCatalog(String catalogName) {
    checkStarted();
    doAsExisted(
        CatalogMetaMapper.class,
        mapper -> mapper.deleteCatalog(catalogName),
        () -> new IllegalMetadataException("Catalog " + catalogName + " has more than one database or table"));
    internalCatalogMap.remove(catalogName);
    externalCatalogMap.remove(catalogName);
  }

  @Override
  public void updateCatalog(CatalogMeta catalogMeta) {
    checkStarted();
    ServerCatalog catalog = getServerCatalog(catalogMeta.getCatalogName());
    validateCatalogUpdate(catalog.getMetadata(), catalogMeta);
    doAs(CatalogMetaMapper.class, mapper -> mapper.updateCatalog(catalogMeta));
    catalog.updateMetadata(catalogMeta);
  }

  @Override
  public TableMetadata loadTableMetadata(TableIdentifier tableIdentifier) {
    validateTableExists(tableIdentifier);
    return Optional.ofNullable(getAs(TableMetaMapper.class, mapper ->
            mapper.selectTableMetaByName(tableIdentifier.getCatalog(),
                tableIdentifier.getDatabase(), tableIdentifier.getTableName())))
        .orElseThrow(() -> new ObjectNotExistsException(tableIdentifier));
  }

  @Override
  public void dropTableMetadata(TableIdentifier tableIdentifier, boolean deleteData) {
    checkStarted();
    validateTableExists(tableIdentifier);
    ServerTableIdentifier serverTableIdentifier = getInternalCatalog(tableIdentifier.getCatalog())
        .dropTable(tableIdentifier.getDatabase(), tableIdentifier.getTableName());
    Optional.ofNullable(tableRuntimeMap.remove(serverTableIdentifier))
        .ifPresent(tableRuntime -> {
          if (headHandler != null) {
            headHandler.fireTableRemoved(tableRuntime);
          }
          tableRuntime.dispose();
        });
  }

  @Override
  public void createTable(String catalogName, TableMetadata tableMetadata) {
    checkStarted();
    validateTableNotExists(tableMetadata.getTableIdentifier().getIdentifier());

    InternalCatalog catalog = getInternalCatalog(catalogName);
    ServerTableIdentifier tableIdentifier = catalog.createTable(tableMetadata);
    ArcticTable table = catalog.loadTable(tableIdentifier.getDatabase(), tableIdentifier.getTableName());
    TableRuntime tableRuntime = new TableRuntime(tableIdentifier, this, table.properties());
    tableRuntimeMap.put(tableIdentifier, tableRuntime);
    if (headHandler != null) {
      headHandler.fireTableAdded(table, tableRuntime);
    }
  }

  @Override
  public List<String> listDatabases(String catalogName) {
    checkStarted();
    return getServerCatalog(catalogName).listDatabases();
  }

  @Override
  public List<ServerTableIdentifier> listManagedTables() {
    checkStarted();
    return getAs(TableMetaMapper.class, TableMetaMapper::selectAllTableIdentifiers);
  }

  @Override
  public List<ServerTableIdentifier> listManagedTables(String catalogName) {
    checkStarted();
    return getAs(TableMetaMapper.class, mapper -> mapper.selectTableIdentifiersByCatalog(catalogName));
  }

  @Override
  public List<TableIdentifier> listTables(String catalogName, String dbName) {
    checkStarted();
    return getServerCatalog(catalogName).listTables(dbName);
  }

  @Override
  public void createDatabase(String catalogName, String dbName) {
    checkStarted();
    getInternalCatalog(catalogName).createDatabase(dbName);
  }

  @Override
  public void dropDatabase(String catalogName, String dbName) {
    checkStarted();
    getInternalCatalog(catalogName).dropDatabase(dbName);
  }

  @Override
  public ArcticTable loadTable(ServerTableIdentifier tableIdentifier) {
    checkStarted();
    return getServerCatalog(tableIdentifier.getCatalog())
        .loadTable(tableIdentifier.getDatabase(), tableIdentifier.getTableName());
  }

  @Override
  public List<TableMetadata> listTableMetas() {
    checkStarted();
    return getAs(TableMetaMapper.class, TableMetaMapper::selectTableMetas);
  }
 
  @Override
  public List<TableMetadata> listTableMetas(String catalogName, String database) {
    checkStarted();
    return getAs(TableMetaMapper.class, mapper -> mapper.selectTableMetasByDb(catalogName, database));
  }

  @Override
  public boolean tableExist(TableIdentifier tableIdentifier) {
    checkStarted();
    return getServerCatalog(tableIdentifier.getCatalog()).exist(
        tableIdentifier.getDatabase(),
        tableIdentifier.getTableName());
  }

  @Override
  public Blocker block(
      TableIdentifier tableIdentifier, List<BlockableOperation> operations,
      Map<String, String> properties) {
    checkStarted();
    return getAndCheckExist(getServerTableIdentifier(tableIdentifier))
        .block(operations, properties, blockerTimeout)
        .buildBlocker();
  }

  @Override
  public void releaseBlocker(TableIdentifier tableIdentifier, String blockerId) {
    checkStarted();
    TableRuntime tableRuntime = getRuntime(getServerTableIdentifier(tableIdentifier));
    if (tableRuntime != null) {
      tableRuntime.release(blockerId);
    }
  }

  @Override
  public long renewBlocker(TableIdentifier tableIdentifier, String blockerId) {
    checkStarted();
    TableRuntime tableRuntime = getAndCheckExist(getServerTableIdentifier(tableIdentifier));
    return tableRuntime.renew(blockerId, blockerTimeout);
  }

  @Override
  public List<Blocker> getBlockers(TableIdentifier tableIdentifier) {
    checkStarted();
    return getAndCheckExist(getServerTableIdentifier(tableIdentifier))
        .getBlockers().stream().map(TableBlocker::buildBlocker).collect(Collectors.toList());
  }

  private InternalCatalog getInternalCatalog(String catalogName) {
    return Optional.ofNullable(internalCatalogMap.get(catalogName)).orElseThrow(() ->
        new ObjectNotExistsException("Catalog " + catalogName));
  }

  @Override
  public void addHandlerChain(RuntimeHandlerChain handler) {
    checkNotStarted();
    if (headHandler == null) {
      headHandler = handler;
    } else {
      headHandler.appendNext(handler);
    }
  }

  @Override
  public void handleTableChanged(TableRuntime tableRuntime, OptimizingStatus originalStatus) {
    if (headHandler != null) {
      headHandler.fireStatusChanged(tableRuntime, originalStatus);
    }
  }

  @Override
  public void handleTableChanged(TableRuntime tableRuntime, TableConfiguration originalConfig) {
    if (headHandler != null) {
      headHandler.fireConfigChanged(tableRuntime, originalConfig);
    }
  }

  @Override
  public void initialize() {
    checkNotStarted();
    List<CatalogMeta> catalogMetas =
        getAs(CatalogMetaMapper.class, CatalogMetaMapper::getCatalogs);
    catalogMetas.forEach(this::initServerCatalog);

    List<TableRuntimeMeta> tableRuntimeMetaList =
        getAs(TableMetaMapper.class, TableMetaMapper::selectTableRuntimeMetas);
    tableRuntimeMetaList.forEach(tableRuntimeMeta -> {
      TableRuntime tableRuntime = tableRuntimeMeta.constructTableRuntime(this);
      tableRuntimeMap.put(tableRuntime.getTableIdentifier(), tableRuntime);
    });

    if (headHandler != null) {
      headHandler.initialize(tableRuntimeMetaList);
    }
    tableExplorerTimer = new Timer("ExternalTableExplorer", true);
    tableExplorerTimer.scheduleAtFixedRate(
        new TableExplorer(),
        0,
        externalCatalogRefreshingInterval);
    initialized.complete(true);
  }

  public TableRuntime getAndCheckExist(ServerTableIdentifier tableIdentifier) {
    if (tableIdentifier == null) {
      throw new ObjectNotExistsException(tableIdentifier);
    }
    TableRuntime tableRuntime = getRuntime(tableIdentifier);
    if (tableRuntime == null) {
      throw new ObjectNotExistsException(tableIdentifier);
    }
    return tableRuntime;
  }

  private ServerTableIdentifier getServerTableIdentifier(TableIdentifier id) {
    return getAs(
        TableMetaMapper.class,
        mapper -> mapper.selectTableIdentifier(id.getCatalog(), id.getDatabase(), id.getTableName()));
  }

  @Override
  public TableRuntime getRuntime(ServerTableIdentifier tableIdentifier) {
    checkStarted();
    return tableRuntimeMap.get(tableIdentifier);
  }

  @Override
  public boolean contains(ServerTableIdentifier tableIdentifier) {
    checkStarted();
    return tableRuntimeMap.containsKey(tableIdentifier);
  }

  public void dispose() {
    if (tableExplorerTimer != null) {
      tableExplorerTimer.cancel();
    }
    if (headHandler != null) {
      headHandler.dispose();
    }
  }

  @VisibleForTesting
  void exploreExternalCatalog() {
    for (ExternalCatalog externalCatalog : externalCatalogMap.values()) {
      try {
        Set<TableIdentity> tableIdentifiers = externalCatalog.listTables().stream()
            .map(TableIdentity::new)
            .collect(Collectors.toSet());
        Map<TableIdentity, ServerTableIdentifier> serverTableIdentifiers =
            getAs(
                TableMetaMapper.class,
                mapper -> mapper.selectTableIdentifiersByCatalog(externalCatalog.name())).stream()
                .collect(Collectors.toMap(TableIdentity::new, tableIdentifier -> tableIdentifier));
        Sets.difference(tableIdentifiers, serverTableIdentifiers.keySet())
            .forEach(tableIdentity -> {
              try {
                syncTable(externalCatalog, tableIdentity);
              } catch (Exception e) {
                LOG.error("TableExplorer sync table {} error", tableIdentity.toString(), e);
              }
            });
        Sets.difference(serverTableIdentifiers.keySet(), tableIdentifiers)
            .forEach(tableIdentity -> {
              try {
                disposeTable(externalCatalog, serverTableIdentifiers.get(tableIdentity));
              } catch (Exception e) {
                LOG.error("TableExplorer dispose table {} error", tableIdentity.toString(), e);
              }
            });
      } catch (Exception e) {
        LOG.error("TableExplorer run error", e);
      }
    }
  }

  private void validateTableIdentifier(TableIdentifier tableIdentifier) {
    if (StringUtils.isBlank(tableIdentifier.getTableName())) {
      throw new IllegalMetadataException("table name is blank");
    }
    if (StringUtils.isBlank(tableIdentifier.getCatalog())) {
      throw new IllegalMetadataException("catalog is blank");
    }
    if (StringUtils.isBlank(tableIdentifier.getDatabase())) {
      throw new IllegalMetadataException("database is blank");
    }
  }

  private void validateTableNotExists(TableIdentifier tableIdentifier) {
    validateTableIdentifier(tableIdentifier);
    if (tableExist(tableIdentifier)) {
      throw new AlreadyExistsException(tableIdentifier);
    }
  }

  private void validateTableExists(TableIdentifier tableIdentifier) {
    validateTableIdentifier(tableIdentifier);
    if (!tableExist(tableIdentifier)) {
      throw new ObjectNotExistsException(tableIdentifier);
    }
  }

  private void validateCatalogUpdate(CatalogMeta oldMeta, CatalogMeta newMeta) {
    if (!oldMeta.getCatalogType().equals(newMeta.getCatalogType())) {
      throw new IllegalMetadataException("Cannot update catalog type");
    }
  }

  private void checkStarted() {
    try {
      initialized.get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void checkNotStarted() {
    if (initialized.isDone()) {
      throw new IllegalStateException("Table service has started.");
    }
  }

  private class TableExplorer extends TimerTask {

    @Override
    public void run() {
      exploreExternalCatalog();
    }
  }

  private void syncTable(ExternalCatalog externalCatalog, TableIdentity tableIdentity) {
    invokeConsisitency(() -> doAsTransaction(
        () -> externalCatalog.syncTable(tableIdentity.getDatabase(), tableIdentity.getTableName()),
        () -> handleTableRuntimeAdded(externalCatalog, tableIdentity)
    ));
  }

  private void handleTableRuntimeAdded(ExternalCatalog externalCatalog, TableIdentity tableIdentity) {
    ServerTableIdentifier tableIdentifier =
        externalCatalog.getServerTableIdentifier(tableIdentity.getDatabase(), tableIdentity.getTableName());
    ArcticTable table = externalCatalog.loadTable(
        tableIdentifier.getDatabase(),
        tableIdentifier.getTableName());
    TableRuntime tableRuntime = new TableRuntime(tableIdentifier, this, table.properties());
    tableRuntimeMap.put(tableIdentifier, tableRuntime);
    if (headHandler != null) {
      headHandler.fireTableAdded(table, tableRuntime);
    }
  }

  private void disposeTable(ExternalCatalog externalCatalog, ServerTableIdentifier tableIdentifier) {
    externalCatalog.disposeTable(tableIdentifier.getDatabase(), tableIdentifier.getTableName());
    Optional.ofNullable(tableRuntimeMap.remove(tableIdentifier))
        .ifPresent(tableRuntime -> {
          if (headHandler != null) {
            headHandler.fireTableRemoved(tableRuntime);
          }
          tableRuntime.dispose();
        });
  }

  private static class TableIdentity {

    private final String database;
    private final String tableName;

    protected TableIdentity(TableIdentifier tableIdentifier) {
      this.database = tableIdentifier.getDatabase();
      this.tableName = tableIdentifier.getTableName();
    }

    protected TableIdentity(ServerTableIdentifier serverTableIdentifier) {
      this.database = serverTableIdentifier.getDatabase();
      this.tableName = serverTableIdentifier.getTableName();
    }

    public String getDatabase() {
      return database;
    }

    public String getTableName() {
      return tableName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      TableIdentity that = (TableIdentity) o;
      return Objects.equal(database, that.database) && Objects.equal(tableName, that.tableName);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(database, tableName);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("database", database)
          .add("tableName", tableName)
          .toString();
    }
  }
}
