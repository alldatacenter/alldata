package com.netease.arctic.ams.server.maintainer.command;

import com.netease.arctic.ams.api.NoSuchObjectException;
import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.catalog.CatalogManager;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class UseCall implements CallCommand {

  private CatalogManager catalogManager;
  private String namespace;

  public UseCall(String namespace, CatalogManager catalogManager) {
    this.namespace = namespace;
    this.catalogManager = catalogManager;
  }

  @Override
  public String call(Context context) {
    String[] nameParts = this.namespace.split("\\.");
    try {
      ArcticCatalog catalog = useCatalog(nameParts[0], context);
      if (nameParts.length == 2) {
        useDb(catalog, nameParts[1], context);
      }
    } catch (NoSuchObjectException e) {
      if (StringUtils.isEmpty(context.getCatalog())) {
        throw new RuntimeException("there is no catalog be set");
      }
      ArcticCatalog catalog = catalogManager.getArcticCatalog(context.getCatalog());
      useDb(catalog, nameParts[0], context);
    }
    return "OK";
  }

  private ArcticCatalog useCatalog(String catalogName, Context context) throws NoSuchObjectException {
    List<String> catalogs = catalogManager.catalogs();
    if (catalogs.contains(catalogName)) {
      ArcticCatalog catalog = catalogManager.getArcticCatalog(catalogName);
      context.setCatalog(catalogName);
      return catalog;
    } else {
      throw new NoSuchObjectException();
    }
  }

  private void useDb(ArcticCatalog catalog, String dbName, Context context) {
    if (!catalog.listDatabases().contains(dbName)) {
      throw new RuntimeException(String.format("there is no database named:%s of catalog:%s", this.namespace,
          context.getCatalog()));
    }
    context.setDb(dbName);
  }
}
