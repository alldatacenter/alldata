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

import com.netease.arctic.TestAms;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import org.apache.iceberg.CatalogProperties;
import org.apache.iceberg.TestCatalogUtil;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Map;

public class TestCatalogLoader {

  private static final String TEST_CATALOG_NAME = "test";
  @ClassRule
  public static TestAms TEST_AMS = new TestAms();

  @Test
  public void testLoadIcebergHadoopCatalog() {
    Map<String, String> properties = Maps.newHashMap();
    properties.put(CatalogMetaProperties.KEY_WAREHOUSE, "/temp");
    CatalogMeta catalogMeta = CatalogTestHelpers.buildCatalogMeta(TEST_CATALOG_NAME,
        CatalogMetaProperties.CATALOG_TYPE_HADOOP, properties, TableFormat.ICEBERG);
    TEST_AMS.getAmsHandler().createCatalog(catalogMeta);
    ArcticCatalog loadCatalog = CatalogLoader.load(getCatalogUrl(TEST_CATALOG_NAME));
    Assert.assertEquals(TEST_CATALOG_NAME, loadCatalog.name());
    Assert.assertTrue(loadCatalog instanceof BasicIcebergCatalog);
    TEST_AMS.getAmsHandler().dropCatalog(TEST_CATALOG_NAME);
  }

  @Test
  public void testLoadIcebergCustomCatalog() {
    Map<String, String> properties = Maps.newHashMap();
    properties.put(CatalogProperties.CATALOG_IMPL, TestCatalogUtil.TestCatalog.class.getName());
    CatalogMeta catalogMeta = CatalogTestHelpers.buildCatalogMeta(TEST_CATALOG_NAME,
        CatalogMetaProperties.CATALOG_TYPE_CUSTOM, properties, TableFormat.ICEBERG);
    TEST_AMS.getAmsHandler().createCatalog(catalogMeta);
    ArcticCatalog loadCatalog = CatalogLoader.load(getCatalogUrl(TEST_CATALOG_NAME));
    Assert.assertEquals(TEST_CATALOG_NAME, loadCatalog.name());
    Assert.assertTrue(loadCatalog instanceof BasicIcebergCatalog);
    TEST_AMS.getAmsHandler().dropCatalog(TEST_CATALOG_NAME);
  }

  @Test
  public void testLoadMixedIcebergCatalog() {
    Map<String, String> properties = Maps.newHashMap();
    properties.put(CatalogMetaProperties.KEY_WAREHOUSE, "/temp");
    CatalogMeta catalogMeta = CatalogTestHelpers.buildCatalogMeta(TEST_CATALOG_NAME,
        CatalogMetaProperties.CATALOG_TYPE_AMS, properties, TableFormat.MIXED_ICEBERG);
    TEST_AMS.getAmsHandler().createCatalog(catalogMeta);
    ArcticCatalog loadCatalog = CatalogLoader.load(getCatalogUrl(TEST_CATALOG_NAME));
    Assert.assertEquals(TEST_CATALOG_NAME, loadCatalog.name());
    Assert.assertTrue(loadCatalog instanceof BasicArcticCatalog);
    TEST_AMS.getAmsHandler().dropCatalog(TEST_CATALOG_NAME);
  }

  @Test
  public void testLoadOldAMSCatalog() {
    Map<String, String> properties = Maps.newHashMap();
    CatalogMeta catalogMeta = CatalogTestHelpers.buildCatalogMeta(TEST_CATALOG_NAME,
        CatalogMetaProperties.CATALOG_TYPE_HADOOP, properties);
    TEST_AMS.getAmsHandler().createCatalog(catalogMeta);
    ArcticCatalog loadCatalog = CatalogLoader.load(getCatalogUrl(TEST_CATALOG_NAME));
    Assert.assertEquals(TEST_CATALOG_NAME, loadCatalog.name());
    Assert.assertTrue(loadCatalog instanceof BasicArcticCatalog);
    TEST_AMS.getAmsHandler().dropCatalog(TEST_CATALOG_NAME);
  }

  @Test
  public void testLoadNotExistedCatalog() {
    Assert.assertThrows("catalog not found, please check catalog name", IllegalArgumentException.class,
        () -> CatalogLoader.load(getCatalogUrl(TEST_CATALOG_NAME)));
  }

  @Test
  public void testLoadCatalogWithErrorFormat() {
    Map<String, String> properties = Maps.newHashMap();
    CatalogMeta catalogMeta = CatalogTestHelpers.buildCatalogMeta(TEST_CATALOG_NAME,
        CatalogMetaProperties.CATALOG_TYPE_HADOOP, properties, TableFormat.MIXED_ICEBERG);
    TEST_AMS.getAmsHandler().createCatalog(catalogMeta);
    Assert.assertThrows("failed when load catalog test", IllegalStateException.class,
        () -> CatalogLoader.load(getCatalogUrl(TEST_CATALOG_NAME)));
    TEST_AMS.getAmsHandler().dropCatalog(TEST_CATALOG_NAME);
  }

  private String getCatalogUrl(String catalogName) {
    return TEST_AMS.getServerUrl() + "/" + catalogName;
  }
}
