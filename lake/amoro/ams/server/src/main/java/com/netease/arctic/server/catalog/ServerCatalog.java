package com.netease.arctic.server.catalog;

import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.TableIdentifier;
import com.netease.arctic.server.persistence.PersistentBase;
import com.netease.arctic.server.persistence.mapper.CatalogMetaMapper;
import com.netease.arctic.table.ArcticTable;

import java.util.List;

public abstract class ServerCatalog extends PersistentBase {

  private volatile CatalogMeta metadata;

  protected ServerCatalog(CatalogMeta metadata) {
    this.metadata = metadata;
  }

  public String name() {
    return metadata.getCatalogName();
  }


  public CatalogMeta getMetadata() {
    return metadata;
  }

  public void updateMetadata(CatalogMeta metadata) {
    doAs(CatalogMetaMapper.class, mapper -> mapper.updateCatalog(metadata));
    this.metadata = metadata;
  }

  public abstract boolean exist(String database);

  public abstract boolean exist(String database, String tableName);

  public abstract List<String> listDatabases();

  public abstract List<TableIdentifier> listTables();

  public abstract List<TableIdentifier> listTables(String database);

  public abstract ArcticTable loadTable(String database, String tableName);
}
