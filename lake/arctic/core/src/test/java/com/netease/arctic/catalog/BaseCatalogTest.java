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

import com.netease.arctic.TableTestHelpers;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import com.netease.arctic.ams.api.properties.TableFormat;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableProperties;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.exceptions.AlreadyExistsException;
import org.apache.iceberg.util.PropertyUtil;
import org.apache.thrift.TException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class BaseCatalogTest extends CatalogTestBase {

  public BaseCatalogTest(TableFormat testFormat) {
    super(testFormat);
  }

  @Parameterized.Parameters(name = "testFormat = {0}")
  public static Object[] parameters() {
    return new Object[] {TableFormat.ICEBERG, TableFormat.MIXED_ICEBERG};
  }

  @Test
  public void testCreateAndDropDatabase() {
    String createDbName = TableTestHelpers.TEST_DB_NAME;
    Assert.assertFalse(getCatalog().listDatabases().contains(createDbName));
    getCatalog().createDatabase(createDbName);
    Assert.assertTrue(getCatalog().listDatabases().contains(createDbName));
    getCatalog().dropDatabase(createDbName);
    Assert.assertFalse(getCatalog().listDatabases().contains(createDbName));
  }

  @Test
  public void testCreateDuplicateDatabase() {
    String createDbName = TableTestHelpers.TEST_DB_NAME;
    Assert.assertFalse(getCatalog().listDatabases().contains(createDbName));
    getCatalog().createDatabase(createDbName);
    Assert.assertTrue(getCatalog().listDatabases().contains(createDbName));
    Assert.assertThrows(
        AlreadyExistsException.class,
        () -> getCatalog().createDatabase(createDbName));
    getCatalog().dropDatabase(createDbName);
  }

  @Test
  public void testCreateTableWithCatalogTableProperties() throws TException {
    CatalogMeta testCatalogMeta = TEST_AMS.getAmsHandler().getCatalog(TEST_CATALOG_NAME);
    TEST_AMS.getAmsHandler().updateMeta(testCatalogMeta,
        CatalogMetaProperties.TABLE_PROPERTIES_PREFIX + TableProperties.ENABLE_SELF_OPTIMIZING,
        "false");
    getCatalog().createDatabase(TableTestHelpers.TEST_DB_NAME);
    createTestTable();
    ArcticTable createTable = getCatalog().loadTable(TableTestHelpers.TEST_TABLE_ID);
    Assert.assertEquals(false, PropertyUtil.propertyAsBoolean(createTable.properties(),
        TableProperties.ENABLE_SELF_OPTIMIZING, TableProperties.ENABLE_SELF_OPTIMIZING_DEFAULT));
  }

  @Test
  public void testLoadTableWithNewCatalogProperties() throws TException {
    getCatalog().createDatabase(TableTestHelpers.TEST_DB_NAME);
    createTestTable();
    ArcticTable createTable = getCatalog().loadTable(TableTestHelpers.TEST_TABLE_ID);

    Assert.assertTrue(PropertyUtil.propertyAsBoolean(createTable.properties(),
        TableProperties.ENABLE_SELF_OPTIMIZING, TableProperties.ENABLE_SELF_OPTIMIZING_DEFAULT));

    CatalogMeta testCatalogMeta = TEST_AMS.getAmsHandler().getCatalog(TEST_CATALOG_NAME);
    TEST_AMS.getAmsHandler().updateMeta(testCatalogMeta,
        CatalogMetaProperties.TABLE_PROPERTIES_PREFIX + TableProperties.ENABLE_SELF_OPTIMIZING,
        "false");
    getCatalog().refresh();
    ArcticTable loadTable = getCatalog().loadTable(createTable.id());
    Assert.assertFalse(PropertyUtil.propertyAsBoolean(loadTable.properties(),
        TableProperties.ENABLE_SELF_OPTIMIZING, TableProperties.ENABLE_SELF_OPTIMIZING_DEFAULT));
  }

  @After
  public void after() {
    getCatalog().dropTable(TableTestHelpers.TEST_TABLE_ID, true);
    if (getCatalog().listDatabases().contains(TableTestHelpers.TEST_DB_NAME)) {
      getCatalog().dropDatabase(TableTestHelpers.TEST_DB_NAME);
    }
  }

  protected void createTestTable() {
    switch (getTestFormat()) {
      case ICEBERG:
        getIcebergCatalog().createTable(
            TableIdentifier.of(TableTestHelpers.TEST_DB_NAME, TableTestHelpers.TEST_TABLE_NAME),
            TableTestHelpers.TABLE_SCHEMA);
        break;
      case MIXED_ICEBERG:
      case MIXED_HIVE:
        getCatalog()
            .newTableBuilder(TableTestHelpers.TEST_TABLE_ID, TableTestHelpers.TABLE_SCHEMA)
            .create();
        break;
      default:
        throw new UnsupportedOperationException("Unsupported table format:" + getTestFormat());
    }
  }
}
