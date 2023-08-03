package com.netease.arctic.server.catalog;

import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.catalog.MixedTables;
import com.netease.arctic.server.persistence.mapper.TableMetaMapper;
import com.netease.arctic.server.table.TableMetadata;
import com.netease.arctic.table.ArcticTable;

public class MixedCatalogImpl extends InternalCatalog {

  private final MixedTables tables;

  protected MixedCatalogImpl(CatalogMeta metadata) {
    super(metadata);
    this.tables = new MixedTables(metadata);
  }

  protected MixedCatalogImpl(CatalogMeta metadata, MixedTables tables) {
    super(metadata);
    this.tables = tables;
  }

  @Override
  public void updateMetadata(CatalogMeta metadata) {
    super.updateMetadata(metadata);
    this.tables.refreshCatalogMeta(getMetadata());
  }

  @Override
  public ArcticTable loadTable(String database, String tableName) {
    TableMetadata tableMetadata = getAs(TableMetaMapper.class, mapper ->
        mapper.selectTableMetaByName(getMetadata().getCatalogName(), database, tableName));
    return tableMetadata == null ? null : tables.loadTableByMeta(tableMetadata.buildTableMeta());
  }

  protected MixedTables tables() {
    return tables;
  }
}
