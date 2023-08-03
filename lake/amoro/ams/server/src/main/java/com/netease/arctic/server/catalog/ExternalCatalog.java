package com.netease.arctic.server.catalog;

import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.server.persistence.mapper.TableMetaMapper;
import com.netease.arctic.server.table.ServerTableIdentifier;

public abstract class ExternalCatalog extends ServerCatalog {

  protected ExternalCatalog(CatalogMeta metadata) {
    super(metadata);
  }

  public void syncTable(String database, String tableName) {
    ServerTableIdentifier tableIdentifier =
            ServerTableIdentifier.of(getMetadata().getCatalogName(), database, tableName);
    doAs(TableMetaMapper.class, mapper -> mapper.insertTable(tableIdentifier));
  }

  public ServerTableIdentifier getServerTableIdentifier(String database, String tableName) {
    return getAs(TableMetaMapper.class, mapper -> mapper.selectTableIdentifier(getMetadata().getCatalogName(),
        database, tableName));
  }

  public void disposeTable(String database, String tableName) {
    doAs(TableMetaMapper.class, mapper -> mapper.deleteTableIdByName(getMetadata().getCatalogName(), database,
        tableName));
  }
}
