package com.netease.arctic.ams.server.maintainer.command;

import com.netease.arctic.catalog.CatalogManager;
import com.netease.arctic.table.TableIdentifier;
import org.apache.commons.lang3.StringUtils;

import java.util.stream.Collectors;

public class ShowCall implements CallCommand {

  private Namespaces namespaces;
  private CatalogManager catalogManager;

  public ShowCall(Namespaces namespaces, CatalogManager catalogManager) {
    this.namespaces = namespaces;
    this.catalogManager = catalogManager;
  }

  @Override
  public String call(Context context) {
    switch (this.namespaces) {
      case CATALOGS:
        return String.join("\n", catalogManager.catalogs());
      case DATABASES:
        if (StringUtils.isEmpty(context.getCatalog())) {
          throw new RuntimeException("there is no catalog be set");
        }
        return String.join("\n", catalogManager.getArcticCatalog(context.getCatalog()).listDatabases());
      case TABLES:
        if (StringUtils.isEmpty(context.getCatalog())) {
          throw new RuntimeException("there is no catalog be set");
        } else if (StringUtils.isEmpty(context.getDb())) {
          throw new RuntimeException("there is no database be set");
        } else {
          return catalogManager.getArcticCatalog(context.getCatalog()).listTables(context.getDb())
              .stream()
              .map(TableIdentifier::getTableName)
              .collect(Collectors.joining("\n"));
        }
      default:
        throw new UnsupportedOperationException("not support show operation named:" + this.namespaces);
    }
  }

  public enum Namespaces {
    CATALOGS,
    DATABASES,
    TABLES
  }
}
