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

package com.netease.arctic.catalog;

import com.netease.arctic.AmsClient;
import com.netease.arctic.PooledAmsClient;
import com.netease.arctic.ams.api.ArcticTableMetastore;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.Constants;
import com.netease.arctic.ams.api.NoSuchObjectException;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.ams.api.client.AmsClientPools;
import com.netease.arctic.ams.api.client.ArcticThriftUrl;
import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import com.netease.arctic.utils.CatalogUtil;
import org.apache.iceberg.CatalogProperties;
import org.apache.iceberg.common.DynConstructors;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.rest.RESTCatalog;
import org.apache.thrift.TException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.CATALOG_TYPE_AMS;
import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.CATALOG_TYPE_CUSTOM;
import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.CATALOG_TYPE_HADOOP;
import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.CATALOG_TYPE_HIVE;

/**
 * Catalogs, create catalog from arctic metastore thrift url.
 */
public class CatalogLoader {

  public static final String AMS_CATALOG_IMPL = BasicArcticCatalog.class.getName();
  public static final String ICEBERG_CATALOG_IMPL = BasicIcebergCatalog.class.getName();
  public static final String HIVE_CATALOG_IMPL = "com.netease.arctic.hive.catalog.ArcticHiveCatalog";

  public static final String ICEBERG_REST_CATALOG = RESTCatalog.class.getName();

  /**
   * Entrypoint for loading Catalog.
   *
   * @param catalogUrl arctic catalog url, thrift://arctic-ams-host:port/catalog_name
   * @param properties client side catalog configs
   * @return arctic catalog object
   */
  public static ArcticCatalog load(String catalogUrl, Map<String, String> properties) {
    ArcticThriftUrl url = ArcticThriftUrl.parse(catalogUrl, Constants.THRIFT_TABLE_SERVICE_NAME);
    if (url.catalogName() == null || url.catalogName().contains("/")) {
      throw new IllegalArgumentException("invalid catalog name " + url.catalogName());
    }

    return loadCatalog(catalogUrl, url.catalogName(), properties);
  }

  /**
   * Entrypoint for loading catalog.
   *
   * @param catalogUrl arctic catalog url, thrift://arctic-ams-host:port/catalog_name
   * @return arctic catalog object
   */
  public static ArcticCatalog load(String catalogUrl) {
    return load(catalogUrl, Maps.newHashMap());
  }

  /**
   * Entrypoint for loading catalog.
   *
   * @param client      arctic metastore client
   * @param catalogName arctic catalog name
   * @return arctic catalog object
   */
  public static ArcticCatalog load(AmsClient client, String catalogName) {
    return load(client, catalogName, Maps.newHashMap());
  }

  /**
   * Entrypoint for loading catalog
   *
   * @param client      arctic metastore client
   * @param catalogName arctic catalog name
   * @param props       client side catalog configs
   * @return arctic catalog object
   */
  public static ArcticCatalog load(AmsClient client, String catalogName, Map<String, String> props) {
    try {
      CatalogMeta catalogMeta = client.getCatalog(catalogName);
      String type = catalogMeta.getCatalogType();
      CatalogUtil.mergeCatalogProperties(catalogMeta, props);
      String catalogImpl;
      Set<TableFormat> tableFormats = CatalogUtil.tableFormats(catalogMeta);
      Preconditions.checkArgument(tableFormats.size() == 1,
          "Catalog support only one table format now.");
      TableFormat tableFormat = tableFormats.iterator().next();
      switch (type) {
        case CATALOG_TYPE_HADOOP:
          Preconditions.checkArgument(tableFormat.equals(TableFormat.ICEBERG),
              "Hadoop catalog support iceberg table only.");
          if (catalogMeta.getCatalogProperties().containsKey(CatalogMetaProperties.TABLE_FORMATS)) {
            catalogImpl = ICEBERG_CATALOG_IMPL;
          } else {
            // Compatibility with older versions
            catalogImpl = AMS_CATALOG_IMPL;
          }
          break;
        case CATALOG_TYPE_HIVE:
          if (tableFormat.equals(TableFormat.ICEBERG)) {
            catalogImpl = ICEBERG_CATALOG_IMPL;
          } else if (tableFormat.equals(TableFormat.MIXED_HIVE)) {
            catalogImpl = HIVE_CATALOG_IMPL;
          } else {
            throw new IllegalArgumentException("Hive Catalog support iceberg table and mixed hive table only");
          }
          break;
        case CATALOG_TYPE_AMS:
          if (tableFormat.equals(TableFormat.MIXED_ICEBERG)) {
            catalogImpl = AMS_CATALOG_IMPL;
          } else if (tableFormat.equals(TableFormat.ICEBERG)) {
            catalogMeta.putToCatalogProperties(CatalogProperties.WAREHOUSE_LOCATION, catalogName);
            catalogMeta.putToCatalogProperties(CatalogProperties.CATALOG_IMPL, ICEBERG_REST_CATALOG);
            catalogImpl = ICEBERG_CATALOG_IMPL;
          } else {
            throw new IllegalArgumentException("Internal Catalog support iceberg or mixed-iceberg table only");
          }

          break;
        case CATALOG_TYPE_CUSTOM:
          Preconditions.checkArgument(tableFormat.equals(TableFormat.ICEBERG),
              "Custom catalog support iceberg table only.");
          Preconditions.checkArgument(catalogMeta.getCatalogProperties().containsKey(CatalogProperties.CATALOG_IMPL),
              "Custom catalog properties must contains " + CatalogProperties.CATALOG_IMPL);
          catalogImpl = ICEBERG_CATALOG_IMPL;
          break;
        default:
          throw new IllegalStateException("unsupported catalog type:" + type);
      }
      ArcticCatalog catalog = buildCatalog(catalogImpl);
      catalog.initialize(client, catalogMeta, props);
      return catalog;
    } catch (NoSuchObjectException e1) {
      throw new IllegalArgumentException("catalog not found, please check catalog name", e1);
    } catch (Exception e) {
      throw new IllegalStateException("failed when load catalog " + catalogName, e);
    }
  }

  /**
   * Show catalog list in arctic metastore.
   *
   * @param metastoreUrl url of arctic metastore
   * @return catalog name list
   */
  public static List<String> catalogs(String metastoreUrl) {
    try {
      return ((ArcticTableMetastore.Iface) AmsClientPools.getClientPool(metastoreUrl).iface()).getCatalogs()
          .stream()
          .map(CatalogMeta::getCatalogName)
          .collect(Collectors.toList());
    } catch (TException e) {
      throw new IllegalStateException("failed when load catalogs", e);
    }
  }

  private static ArcticCatalog loadCatalog(
      String metaStoreUrl,
      String catalogName,
      Map<String, String> properties) {
    AmsClient client = new PooledAmsClient(metaStoreUrl);
    return load(client, catalogName, properties);
  }

  private static ArcticCatalog buildCatalog(String impl) {
    DynConstructors.Ctor<ArcticCatalog> ctor;
    try {
      ctor = DynConstructors.builder(ArcticCatalog.class).impl(impl).buildChecked();
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException(String.format(
          "Cannot initialize Catalog implementation %s: %s", impl, e.getMessage()), e);
    }
    try {
      return ctor.newInstance();
    } catch (ClassCastException e) {
      throw new IllegalArgumentException(
          String.format("Cannot initialize Catalog, %s does not implement Catalog.", impl), e);
    }
  }
}
