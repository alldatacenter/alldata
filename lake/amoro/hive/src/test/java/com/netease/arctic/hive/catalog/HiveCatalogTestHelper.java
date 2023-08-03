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

package com.netease.arctic.hive.catalog;

import com.google.common.collect.Maps;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.CatalogTestHelper;
import com.netease.arctic.catalog.CatalogTestHelpers;
import com.netease.arctic.catalog.MixedTables;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;

import java.util.Map;

import static org.apache.iceberg.CatalogUtil.ICEBERG_CATALOG_TYPE;
import static org.apache.iceberg.CatalogUtil.ICEBERG_CATALOG_TYPE_HIVE;

public class HiveCatalogTestHelper implements CatalogTestHelper {

  private final TableFormat tableFormat;
  private final Configuration hiveConf;

  public HiveCatalogTestHelper(TableFormat tableFormat, Configuration hiveConf) {
    Preconditions.checkArgument(tableFormat.equals(TableFormat.ICEBERG) ||
        tableFormat.equals(TableFormat.MIXED_HIVE), "Cannot support table format:" + tableFormat);
    this.tableFormat = tableFormat;
    this.hiveConf = hiveConf;
  }

  @Override
  public TableFormat tableFormat() {
    return tableFormat;
  }

  @Override
  public CatalogMeta buildCatalogMeta(String baseDir) {
    Map<String, String> properties = Maps.newHashMap();
    return CatalogTestHelpers.buildHiveCatalogMeta(TEST_CATALOG_NAME,
        properties, hiveConf, tableFormat);
  }

  @Override
  public Catalog buildIcebergCatalog(CatalogMeta catalogMeta) {
    Map<String, String> catalogProperties = Maps.newHashMap(catalogMeta.getCatalogProperties());
    catalogProperties.put(ICEBERG_CATALOG_TYPE, ICEBERG_CATALOG_TYPE_HIVE);
    return org.apache.iceberg.CatalogUtil.buildIcebergCatalog(TEST_CATALOG_NAME,
        catalogProperties, hiveConf);
  }

  @Override
  public MixedTables buildMixedTables(CatalogMeta catalogMeta) {
    if (!TableFormat.MIXED_HIVE.equals(tableFormat)) {
      throw new UnsupportedOperationException("Cannot build mixed-tables for table format:" + tableFormat);
    }
    return new MixedHiveTables(catalogMeta);
  }

  @Override
  public String toString() {
    return tableFormat.toString();
  }
}
