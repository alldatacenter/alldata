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

package com.netease.arctic.ams.server.utils;

import com.netease.arctic.AmsClient;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.ams.server.service.impl.CatalogMetadataService;
import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.catalog.BasicIcebergCatalog;
import com.netease.arctic.catalog.CatalogLoader;
import com.netease.arctic.table.TableIdentifier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * catalog util class. cache thrift objects
 */
public class CatalogUtil {
  
  private static final Logger LOG = LoggerFactory.getLogger(CatalogUtil.class);
  public static final ConcurrentHashMap<String, ArcticCatalog> CATALOG_CACHE = new ConcurrentHashMap<>();

  /**
   * add cache
   */
  public static ArcticCatalog getArcticCatalog(String thriftHost, Integer thriftPort, String name) {
    if (StringUtils.isBlank(thriftHost)) {
      thriftHost = "localhost";
    }

    if (CATALOG_CACHE.get(name) == null) {
      synchronized (CatalogUtil.class) {
        if (CATALOG_CACHE.get(name) == null) {
          String catalogThriftUrl = String.format("thrift://%s:%d/%s", thriftHost, thriftPort, name);
          ArcticCatalog catalog = CatalogLoader.load(catalogThriftUrl, new HashMap<>());
          CATALOG_CACHE.put(name, catalog);
          return catalog;
        }
      }
    }
    return CATALOG_CACHE.get(name);
  }

  public static ArcticCatalog getArcticCatalog(String name) {
    if (CATALOG_CACHE.get(name) == null) {
      synchronized (CatalogUtil.class) {
        if (CATALOG_CACHE.get(name) == null) {
          AmsClient client = ServiceContainer.getTableMetastoreHandler();
          ArcticCatalog catalog = CatalogLoader.load(client, name);
          CATALOG_CACHE.put(name, catalog);
          return catalog;
        }
      }
    }
    return CATALOG_CACHE.get(name);
  }

  public static void removeCatalogCache(String name) {
    synchronized (CatalogUtil.class) {
      CATALOG_CACHE.remove(name);
    }
  }

  public static boolean isIcebergCatalog(String name) {
    ArcticCatalog ac = getArcticCatalog(name);
    return ac instanceof BasicIcebergCatalog;
  }

  public static boolean isHiveCatalog(String name) {
    CatalogMetadataService catalogMetadataService = ServiceContainer.getCatalogMetadataService();
    Optional<CatalogMeta> opt = catalogMetadataService.getCatalog(name);
    return opt.isPresent() && CatalogMetaProperties.CATALOG_TYPE_HIVE.equalsIgnoreCase(opt.get().getCatalogType());
  }

  public static Set<TableIdentifier> loadTablesFromCatalog() {
    Set<TableIdentifier> tables = new HashSet<>();
    List<CatalogMeta> catalogMetas = ServiceContainer.getCatalogMetadataService().getCatalogs();
    catalogMetas.forEach(catalogMeta -> {
      try {
        ArcticCatalog arcticCatalog =
            CatalogLoader.load(ServiceContainer.getTableMetastoreHandler(), catalogMeta.getCatalogName());
        List<String> databases = arcticCatalog.listDatabases();
        for (String database : databases) {
          try {
            tables.addAll(arcticCatalog.listTables(database));
          } catch (Exception e1) {
            LOG.error("list tables for database {} error", database, e1);
          }
        }
      } catch (Exception e2) {
        LOG.error("load table for catalog {} error", catalogMeta.getCatalogName(), e2);
      }
    });

    return tables;
  }
}
