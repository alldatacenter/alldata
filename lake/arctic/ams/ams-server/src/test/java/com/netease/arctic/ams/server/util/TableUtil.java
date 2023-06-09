/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.server.util;

import com.google.common.collect.Maps;
import com.netease.arctic.TableTestBase;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.table.ArcticTable;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.catalog.Catalog;

import java.util.Map;
import java.util.Optional;

import static com.netease.arctic.ams.server.AmsTestBase.AMS_TEST_ICEBERG_CATALOG_NAME;
import static com.netease.arctic.ams.server.AmsTestBase.AMS_TEST_ICEBERG_DB_NAME;
import static com.netease.arctic.ams.server.AmsTestBase.icebergCatalog;
import static org.apache.iceberg.CatalogUtil.ICEBERG_CATALOG_TYPE;
import static org.apache.iceberg.CatalogUtil.ICEBERG_CATALOG_TYPE_HADOOP;

public class TableUtil extends TableTestBase {
  public static ArcticTable createIcebergTable(String tableName, Schema schema, Map<String, String> properties,
      PartitionSpec spec) {
    Optional<CatalogMeta> opt = ServiceContainer.getCatalogMetadataService().getCatalog(AMS_TEST_ICEBERG_CATALOG_NAME);
    if (!opt.isPresent()){
      throw new IllegalStateException("catalog " + AMS_TEST_ICEBERG_CATALOG_NAME + " not exist");
    }
    CatalogMeta catalogMeta = opt.get();
    Map<String, String> catalogProperties = Maps.newHashMap(catalogMeta.getCatalogProperties());
    catalogProperties.put(ICEBERG_CATALOG_TYPE, ICEBERG_CATALOG_TYPE_HADOOP);
    Catalog nativeIcebergTable = org.apache.iceberg.CatalogUtil.buildIcebergCatalog(AMS_TEST_ICEBERG_CATALOG_NAME,
        catalogProperties, new Configuration());
    nativeIcebergTable.createTable(
        org.apache.iceberg.catalog.TableIdentifier.of(AMS_TEST_ICEBERG_DB_NAME, tableName),
        schema, spec, null, properties);
    return icebergCatalog.loadTable(
        com.netease.arctic.table.TableIdentifier.of(
            AMS_TEST_ICEBERG_CATALOG_NAME,
            AMS_TEST_ICEBERG_DB_NAME,
            tableName));
  }
}
