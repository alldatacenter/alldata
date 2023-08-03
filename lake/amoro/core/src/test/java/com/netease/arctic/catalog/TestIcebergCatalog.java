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

import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.TableTestHelper;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import com.netease.arctic.io.RecoverableHadoopFileIO;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableProperties;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.thrift.TException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TestIcebergCatalog extends CatalogTestBase {

  @Parameterized.Parameters(name = "testFormat = {0}")
  public static Object[] parameters() {
    return new Object[] {new BasicCatalogTestHelper(TableFormat.ICEBERG)};
  }

  public TestIcebergCatalog(CatalogTestHelper catalogTestHelper) {
    super(catalogTestHelper);
  }

  @Before
  public void before() {
    if (!getCatalog().listDatabases().contains(TableTestHelper.TEST_DB_NAME)) {
      getCatalog().createDatabase(TableTestHelper.TEST_DB_NAME);
    }
  }

  @Test
  public void testLoadIcebergTable() {
    createIcebergTable();
    ArcticTable table = getCatalog().loadTable(TableTestHelper.TEST_TABLE_ID);
    Assert.assertTrue(table instanceof IcebergCatalogWrapper.BasicIcebergTable);
    Assert.assertTrue(table.isUnkeyedTable());
    Assert.assertEquals(BasicTableTestHelper.TABLE_SCHEMA.asStruct(), table.schema().asStruct());
  }

  @Test
  public void testRecoverableFileIO() throws TException {
    createIcebergTable();
    ArcticTable table = getCatalog().loadTable(TableTestHelper.TEST_TABLE_ID);
    Assert.assertFalse(table.io() instanceof RecoverableHadoopFileIO);

    CatalogMeta testCatalogMeta = TEST_AMS.getAmsHandler().getCatalog(CatalogTestHelper.TEST_CATALOG_NAME);
    TEST_AMS.getAmsHandler().updateMeta(
        testCatalogMeta,
        CatalogMetaProperties.TABLE_PROPERTIES_PREFIX + TableProperties.ENABLE_TABLE_TRASH,
        "true");
    getCatalog().refresh();

    table = getCatalog().loadTable(TableTestHelper.TEST_TABLE_ID);
    Assert.assertFalse(table.io() instanceof RecoverableHadoopFileIO);

    getCatalog().dropTable(TableTestHelper.TEST_TABLE_ID, true);
    createIcebergTable();
    table = getCatalog().loadTable(TableTestHelper.TEST_TABLE_ID);
    Assert.assertFalse(table.io() instanceof RecoverableHadoopFileIO);
  }

  @After
  public void after() {
    getCatalog().dropTable(TableTestHelper.TEST_TABLE_ID, true);
    getCatalog().dropDatabase(TableTestHelper.TEST_DB_NAME);
  }

  private void createIcebergTable() {
    Catalog nativeIcebergCatalog = getIcebergCatalog();
    nativeIcebergCatalog.createTable(
        TableIdentifier.of(TableTestHelper.TEST_DB_NAME, TableTestHelper.TEST_TABLE_NAME),
        BasicTableTestHelper.TABLE_SCHEMA);
  }
}
