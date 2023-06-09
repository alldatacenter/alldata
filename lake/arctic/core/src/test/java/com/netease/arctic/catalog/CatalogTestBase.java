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

import com.google.common.collect.Maps;
import com.netease.arctic.TableTestHelpers;
import com.netease.arctic.TestAms;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.MockArcticMetastoreServer;
import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import com.netease.arctic.ams.api.properties.TableFormat;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.catalog.Catalog;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Map;

import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.CATALOG_TYPE_AMS;
import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.CATALOG_TYPE_HADOOP;
import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.CATALOG_TYPE_HIVE;
import static org.apache.iceberg.CatalogUtil.ICEBERG_CATALOG_TYPE;
import static org.apache.iceberg.CatalogUtil.ICEBERG_CATALOG_TYPE_HADOOP;

public abstract class CatalogTestBase {

  protected static final String TEST_CATALOG_NAME = TableTestHelpers.TEST_CATALOG_NAME;
  @ClassRule
  public static TestAms TEST_AMS = new TestAms();
  private final TableFormat testFormat;
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  private ArcticCatalog catalog;
  private CatalogMeta catalogMeta;
  private Catalog icebergCatalog;

  public CatalogTestBase(TableFormat testFormat) {
    this.testFormat = testFormat;
  }

  public static MockArcticMetastoreServer.AmsHandler getAmsHandler() {
    return TEST_AMS.getAmsHandler();
  }

  @Before
  public void setupCatalog() throws IOException {
    catalogMeta = buildCatalogMeta();
    getAmsHandler().createCatalog(catalogMeta);
  }

  protected CatalogMeta buildCatalogMeta() throws IOException {
    Map<String, String> properties = Maps.newHashMap();
    properties.put(CatalogMetaProperties.KEY_WAREHOUSE, temp.newFolder().getPath());
    return CatalogTestHelpers.buildCatalogMeta(TEST_CATALOG_NAME, getCatalogType(),
        properties, testFormat);
  }

  private String getCatalogType() {
    switch (testFormat) {
      case ICEBERG:
        return CATALOG_TYPE_HADOOP;
      case MIXED_ICEBERG:
        return CATALOG_TYPE_AMS;
      case MIXED_HIVE:
        return CATALOG_TYPE_HIVE;
      default:
        throw new UnsupportedOperationException("Unsupported table format:" + testFormat);
    }
  }

  @After
  public void dropCatalog() {
    getAmsHandler().dropCatalog(TEST_CATALOG_NAME);
    catalog = null;
  }

  protected ArcticCatalog getCatalog() {
    if (catalog == null) {
      catalog = CatalogLoader.load(getCatalogUrl());
    }
    return catalog;
  }

  protected String getCatalogUrl() {
    return TEST_AMS.getServerUrl() + "/" + TEST_CATALOG_NAME;
  }

  protected CatalogMeta getCatalogMeta() {
    return catalogMeta;
  }

  protected TableFormat getTestFormat() {
    return testFormat;
  }

  protected Catalog getIcebergCatalog() {
    if (TableFormat.ICEBERG.equals(testFormat)) {
      if (icebergCatalog == null) {
        icebergCatalog = buildIcebergCatalog();
      }
      return icebergCatalog;
    } else {
      throw new UnsupportedOperationException("Cannot get iceberg catalog for table format:" + testFormat);
    }
  }

  protected Catalog buildIcebergCatalog() {
    Map<String, String> catalogProperties = Maps.newHashMap(catalogMeta.getCatalogProperties());
    catalogProperties.put(ICEBERG_CATALOG_TYPE, ICEBERG_CATALOG_TYPE_HADOOP);
    return org.apache.iceberg.CatalogUtil.buildIcebergCatalog(TEST_CATALOG_NAME,
        catalogProperties, new Configuration());
  }
}
