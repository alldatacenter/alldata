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

package com.netease.arctic.ams.server.maintainer.command;

import com.netease.arctic.TableTestHelpers;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import com.netease.arctic.ams.api.properties.TableFormat;
import com.netease.arctic.catalog.CatalogTestHelpers;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static com.netease.arctic.TableTestHelpers.TEST_TABLE_NAME;

public class TestShowCall extends CallCommandTestBase {

  @Test
  public void testShowCatalogs() {
    Context context = new Context();
    Assert.assertEquals(TEST_CATALOG_NAME, callFactory.generateShowCall(ShowCall.Namespaces.CATALOGS).call(context));

    Map<String, String> properties = Maps.newHashMap();
    properties.put(CatalogMetaProperties.KEY_WAREHOUSE, "/temp");
    CatalogMeta catalogMeta = CatalogTestHelpers.buildCatalogMeta("repair_catalog",
        CatalogMetaProperties.CATALOG_TYPE_HADOOP, properties, TableFormat.MIXED_ICEBERG);
    TEST_AMS.getAmsHandler().createCatalog(catalogMeta);
    Assert.assertEquals(
        TEST_CATALOG_NAME + "\nrepair_catalog",
        callFactory.generateShowCall(ShowCall.Namespaces.CATALOGS).call(context));
  }

  @Test
  public void testShowDatabases() throws TException {
    Context context = new Context();
    Assert.assertThrows(
        RuntimeException.class,
        () -> callFactory.generateShowCall(ShowCall.Namespaces.DATABASES).call(context));

    context.setCatalog(TEST_CATALOG_NAME);
    Assert.assertEquals(
        TableTestHelpers.TEST_DB_NAME,
        callFactory.generateShowCall(ShowCall.Namespaces.DATABASES).call(context));

    TEST_AMS.getAmsHandler().createDatabase(TEST_CATALOG_NAME, "repair_db");
    Assert.assertEquals(
        TableTestHelpers.TEST_DB_NAME + "\nrepair_db",
        callFactory.generateShowCall(ShowCall.Namespaces.DATABASES).call(context));
  }

  @Test
  public void testShowTables() {
    Context context = new Context();
    Assert.assertThrows(
        RuntimeException.class,
        () -> callFactory.generateShowCall(ShowCall.Namespaces.TABLES).call(context));

    context.setCatalog(TEST_CATALOG_NAME);
    Assert.assertThrows(
        RuntimeException.class,
        () -> callFactory.generateShowCall(ShowCall.Namespaces.TABLES).call(context));

    context.setDb(TableTestHelpers.TEST_DB_NAME);
    Assert.assertEquals(
        TEST_TABLE_NAME,
        callFactory.generateShowCall(ShowCall.Namespaces.TABLES).call(context));
  }
}
