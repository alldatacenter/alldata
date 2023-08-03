package com.netease.arctic.server.catalog;

import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.TableIdentifier;
import com.netease.arctic.catalog.IcebergCatalogWrapper;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.utils.CatalogUtil;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class IcebergCatalogImpl extends ExternalCatalog {

  private final IcebergCatalogWrapper catalogWrapper;

  protected IcebergCatalogImpl(CatalogMeta metadata) {
    super(metadata);
    this.catalogWrapper = new IcebergCatalogWrapper(getMetadata(), Collections.emptyMap());
  }

  @Override
  public void updateMetadata(CatalogMeta metadata) {
    super.updateMetadata(metadata);
    this.catalogWrapper.refreshCatalogMeta(getMetadata());
  }

  @Override
  public boolean exist(String database) {
    return catalogWrapper.listDatabases().contains(database);
  }

  @Override
  public boolean exist(String database, String tableName) {
    return loadTable(database, tableName) != null;
  }

  @Override
  public List<String> listDatabases() {
    return catalogWrapper.listDatabases();
  }

  @Override
  public List<TableIdentifier> listTables() {
    return toAmsIdList(catalogWrapper.listTables());
  }

  @Override
  public List<TableIdentifier> listTables(String database) {
    return toAmsIdList(catalogWrapper.listTables(database));
  }

  public List<TableIdentifier> toAmsIdList(List<com.netease.arctic.table.TableIdentifier> identifierList) {
    return identifierList.stream().map(CatalogUtil::amsTaleId).collect(Collectors.toList());
  }

  @Override
  public ArcticTable loadTable(String database, String tableName) {
    return catalogWrapper.loadTable(com.netease.arctic.table.TableIdentifier.of(catalogWrapper.name(), database,
        tableName));
  }
}
