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
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.table.blocker.TableBlockerManager;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.util.PropertyUtil;
import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class MixedCatalogTest extends CatalogTestBase {

  public MixedCatalogTest(TableFormat tableFormat) {
    super(tableFormat);
  }

  @Parameterized.Parameters(name = "testFormat = {0}")
  public static Object[] parameters() {
    return new Object[] {TableFormat.MIXED_ICEBERG};
  }

  @Test
  public void testCreateUnkeyedTable() {
    getCatalog().createDatabase(TableTestHelpers.TEST_DB_NAME);
    UnkeyedTable createTable = getCatalog()
        .newTableBuilder(TableTestHelpers.TEST_TABLE_ID, getCreateTableSchema())
        .withPartitionSpec(getCreateTableSpec())
        .create()
        .asUnkeyedTable();

    Assert.assertEquals(getCreateTableSchema().asStruct(), createTable.schema().asStruct());
    Assert.assertEquals(getCreateTableSpec(), createTable.spec());
    Assert.assertEquals(TableTestHelpers.TEST_TABLE_ID, createTable.id());

    UnkeyedTable loadTable = getCatalog().loadTable(TableTestHelpers.TEST_TABLE_ID).asUnkeyedTable();
    Assert.assertEquals(getCreateTableSchema().asStruct(), loadTable.schema().asStruct());
    Assert.assertEquals(getCreateTableSpec(), loadTable.spec());
    Assert.assertEquals(TableTestHelpers.TEST_TABLE_ID, loadTable.id());

    getCatalog().dropTable(TableTestHelpers.TEST_TABLE_ID, true);
    getCatalog().dropDatabase(TableTestHelpers.TEST_DB_NAME);
  }

  @Test
  public void testCreateKeyedTable() {
    getCatalog().createDatabase(TableTestHelpers.TEST_DB_NAME);
    KeyedTable createTable = getCatalog()
        .newTableBuilder(TableTestHelpers.TEST_TABLE_ID, getCreateTableSchema())
        .withPartitionSpec(getCreateTableSpec())
        .withPrimaryKeySpec(TableTestHelpers.PRIMARY_KEY_SPEC)
        .create()
        .asKeyedTable();

    Assert.assertEquals(getCreateTableSchema().asStruct(), createTable.schema().asStruct());
    Assert.assertEquals(getCreateTableSpec(), createTable.spec());
    Assert.assertEquals(TableTestHelpers.TEST_TABLE_ID, createTable.id());
    Assert.assertEquals(TableTestHelpers.PRIMARY_KEY_SPEC, createTable.primaryKeySpec());

    Assert.assertEquals(getCreateTableSchema().asStruct(), createTable.baseTable().schema().asStruct());
    Assert.assertEquals(getCreateTableSpec(), createTable.baseTable().spec());

    Assert.assertEquals(getCreateTableSchema().asStruct(), createTable.changeTable().schema().asStruct());
    Assert.assertEquals(getCreateTableSpec(), createTable.changeTable().spec());

    KeyedTable loadTable = getCatalog().loadTable(TableTestHelpers.TEST_TABLE_ID).asKeyedTable();

    Assert.assertEquals(getCreateTableSchema().asStruct(), loadTable.schema().asStruct());
    Assert.assertEquals(getCreateTableSpec(), loadTable.spec());
    Assert.assertEquals(TableTestHelpers.TEST_TABLE_ID, loadTable.id());
    Assert.assertEquals(TableTestHelpers.PRIMARY_KEY_SPEC, loadTable.primaryKeySpec());

    Assert.assertEquals(getCreateTableSchema().asStruct(), loadTable.baseTable().schema().asStruct());
    Assert.assertEquals(getCreateTableSpec(), loadTable.baseTable().spec());

    Assert.assertEquals(getCreateTableSchema().asStruct(), loadTable.changeTable().schema().asStruct());
    Assert.assertEquals(getCreateTableSpec(), loadTable.changeTable().spec());

    getCatalog().dropTable(TableTestHelpers.TEST_TABLE_ID, true);
    getCatalog().dropDatabase(TableTestHelpers.TEST_DB_NAME);
  }

  @Test
  public void testCreateTableWithCatalogTableProperties() throws TException {
    CatalogMeta testCatalogMeta = TEST_AMS.getAmsHandler().getCatalog(TEST_CATALOG_NAME);
    TEST_AMS.getAmsHandler().updateMeta(testCatalogMeta,
        CatalogMetaProperties.TABLE_PROPERTIES_PREFIX + TableProperties.ENABLE_SELF_OPTIMIZING,
        "false");
    getCatalog().createDatabase(TableTestHelpers.TEST_DB_NAME);
    UnkeyedTable createTable = getCatalog()
        .newTableBuilder(TableTestHelpers.TEST_TABLE_ID, getCreateTableSchema())
        .withPartitionSpec(getCreateTableSpec())
        .create()
        .asUnkeyedTable();
    Assert.assertEquals(false, PropertyUtil.propertyAsBoolean(createTable.properties(),
        TableProperties.ENABLE_SELF_OPTIMIZING, TableProperties.ENABLE_SELF_OPTIMIZING_DEFAULT));
    getCatalog().dropTable(TableTestHelpers.TEST_TABLE_ID, true);
    getCatalog().dropDatabase(TableTestHelpers.TEST_DB_NAME);
  }

  @Test
  public void testRefreshCatalogProperties() throws TException {
    getCatalog().createDatabase(TableTestHelpers.TEST_DB_NAME);
    UnkeyedTable createTable = getCatalog()
        .newTableBuilder(TableTestHelpers.TEST_TABLE_ID, getCreateTableSchema())
        .withPartitionSpec(getCreateTableSpec())
        .create()
        .asUnkeyedTable();
    Assert.assertEquals(true, PropertyUtil.propertyAsBoolean(createTable.properties(),
        TableProperties.ENABLE_SELF_OPTIMIZING, TableProperties.ENABLE_SELF_OPTIMIZING_DEFAULT));
    getCatalog().dropTable(TableTestHelpers.TEST_TABLE_ID, true);

    CatalogMeta testCatalogMeta = TEST_AMS.getAmsHandler().getCatalog(TEST_CATALOG_NAME);
    TEST_AMS.getAmsHandler().updateMeta(testCatalogMeta,
        CatalogMetaProperties.TABLE_PROPERTIES_PREFIX + TableProperties.ENABLE_SELF_OPTIMIZING,
        "false");
    getCatalog().refresh();
    createTable = getCatalog()
        .newTableBuilder(TableTestHelpers.TEST_TABLE_ID, getCreateTableSchema())
        .withPartitionSpec(getCreateTableSpec())
        .create()
        .asUnkeyedTable();
    Assert.assertEquals(false, PropertyUtil.propertyAsBoolean(createTable.properties(),
        TableProperties.ENABLE_SELF_OPTIMIZING, TableProperties.ENABLE_SELF_OPTIMIZING_DEFAULT));

    getCatalog().dropTable(TableTestHelpers.TEST_TABLE_ID, true);
    getCatalog().dropDatabase(TableTestHelpers.TEST_DB_NAME);
  }
  
  @Test
  public void testGetTableBlockerManager() {
    TableBlockerManager tableBlockerManager = getCatalog().getTableBlockerManager(TableTestHelpers.TEST_TABLE_ID);
    Assert.assertEquals(TableTestHelpers.TEST_TABLE_ID, tableBlockerManager.tableIdentifier());
    Assert.assertTrue(tableBlockerManager.getBlockers().isEmpty());
    
  }

  protected Schema getCreateTableSchema() {
    return TableTestHelpers.TABLE_SCHEMA;
  }

  protected PartitionSpec getCreateTableSpec() {
    return TableTestHelpers.SPEC;
  }
}
