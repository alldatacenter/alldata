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
import com.netease.arctic.io.ArcticFileIOs;
import com.netease.arctic.io.RecoverableHadoopFileIO;
import com.netease.arctic.io.TableTrashManagers;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.table.blocker.TableBlockerManager;
import com.netease.arctic.utils.ArcticTableUtil;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.util.PropertyUtil;
import org.apache.thrift.TException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TestMixedCatalog extends CatalogTestBase {

  public TestMixedCatalog(CatalogTestHelper catalogTestHelper) {
    super(catalogTestHelper);
  }

  @Parameterized.Parameters(name = "testFormat = {0}")
  public static Object[] parameters() {
    return new Object[] {new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG)};
  }


  @Before
  public void before() {
    if (!getCatalog().listDatabases().contains(TableTestHelper.TEST_DB_NAME)) {
      getCatalog().createDatabase(TableTestHelper.TEST_DB_NAME);
    }
  }

  protected void validateCreatedTable(ArcticTable table) throws TException  {
    Assert.assertEquals(getCreateTableSchema().asStruct(), table.schema().asStruct());
    Assert.assertEquals(getCreateTableSpec(), table.spec());
    Assert.assertEquals(TableTestHelper.TEST_TABLE_ID, table.id());
    if (table.isKeyedTable()) {
      KeyedTable keyedTable = (KeyedTable)table;
      Assert.assertEquals(BasicTableTestHelper.PRIMARY_KEY_SPEC, keyedTable.primaryKeySpec());
      Assert.assertEquals(getCreateTableSchema().asStruct(), keyedTable.baseTable().schema().asStruct());
      Assert.assertEquals(getCreateTableSpec(), keyedTable.baseTable().spec());
      Assert.assertEquals(getCreateTableSchema().asStruct(), keyedTable.changeTable().schema().asStruct());
      Assert.assertEquals(getCreateTableSpec(), keyedTable.changeTable().spec());
    }
  }

  @Test
  public void testCreateUnkeyedTable() throws TException {
    UnkeyedTable createTable = getCatalog()
        .newTableBuilder(TableTestHelper.TEST_TABLE_ID, getCreateTableSchema())
        .withPartitionSpec(getCreateTableSpec())
        .create()
        .asUnkeyedTable();
    validateCreatedTable(createTable);

    UnkeyedTable loadTable = getCatalog().loadTable(TableTestHelper.TEST_TABLE_ID).asUnkeyedTable();
    validateCreatedTable(loadTable);
  }

  @Test
  public void testCreateKeyedTable() throws TException {
    KeyedTable createTable = getCatalog()
        .newTableBuilder(TableTestHelper.TEST_TABLE_ID, getCreateTableSchema())
        .withPartitionSpec(getCreateTableSpec())
        .withPrimaryKeySpec(BasicTableTestHelper.PRIMARY_KEY_SPEC)
        .create()
        .asKeyedTable();
    validateCreatedTable(createTable);

    KeyedTable loadTable = getCatalog().loadTable(TableTestHelper.TEST_TABLE_ID).asKeyedTable();
    validateCreatedTable(loadTable);
  }

  @Test
  public void testCreateTableWithNewCatalogProperties() throws TException {
    UnkeyedTable createTable = getCatalog()
        .newTableBuilder(TableTestHelper.TEST_TABLE_ID, getCreateTableSchema())
        .withPartitionSpec(getCreateTableSpec())
        .create()
        .asUnkeyedTable();
    Assert.assertTrue(PropertyUtil.propertyAsBoolean(createTable.properties(),
        TableProperties.ENABLE_SELF_OPTIMIZING, TableProperties.ENABLE_SELF_OPTIMIZING_DEFAULT));
    getCatalog().dropTable(TableTestHelper.TEST_TABLE_ID, true);

    CatalogMeta testCatalogMeta = TEST_AMS.getAmsHandler().getCatalog(CatalogTestHelper.TEST_CATALOG_NAME);
    TEST_AMS.getAmsHandler().updateMeta(
        testCatalogMeta,
        CatalogMetaProperties.TABLE_PROPERTIES_PREFIX + TableProperties.ENABLE_SELF_OPTIMIZING,
        "false");
    getCatalog().refresh();
    createTable = getCatalog()
        .newTableBuilder(TableTestHelper.TEST_TABLE_ID, getCreateTableSchema())
        .withPartitionSpec(getCreateTableSpec())
        .create()
        .asUnkeyedTable();
    Assert.assertFalse(PropertyUtil.propertyAsBoolean(createTable.properties(),
        TableProperties.ENABLE_SELF_OPTIMIZING, TableProperties.ENABLE_SELF_OPTIMIZING_DEFAULT));
  }

  @Test
  public void testCreateTableWithNewCatalogLogProperties() throws TException {
    UnkeyedTable createTable = getCatalog()
        .newTableBuilder(TableTestHelper.TEST_TABLE_ID, getCreateTableSchema())
        .withPartitionSpec(getCreateTableSpec())
        .create()
        .asUnkeyedTable();
    Assert.assertTrue(PropertyUtil.propertyAsBoolean(createTable.properties(),
        TableProperties.ENABLE_SELF_OPTIMIZING, TableProperties.ENABLE_SELF_OPTIMIZING_DEFAULT));
    getCatalog().dropTable(TableTestHelper.TEST_TABLE_ID, true);

    CatalogMeta testCatalogMeta = TEST_AMS.getAmsHandler().getCatalog(CatalogTestHelper.TEST_CATALOG_NAME);
    TEST_AMS.getAmsHandler().updateMeta(
        testCatalogMeta,
        CatalogMetaProperties.TABLE_PROPERTIES_PREFIX + TableProperties.LOG_STORE_ADDRESS,
        "1.1.1.1");
    TEST_AMS.getAmsHandler().updateMeta(
        testCatalogMeta,
        CatalogMetaProperties.TABLE_PROPERTIES_PREFIX + TableProperties.LOG_STORE_MESSAGE_TOPIC,
        "test-topic");
    TEST_AMS.getAmsHandler().updateMeta(
        testCatalogMeta,
        CatalogMetaProperties.TABLE_PROPERTIES_PREFIX + TableProperties.ENABLE_SELF_OPTIMIZING,
        "false");
    getCatalog().refresh();
    createTable = getCatalog()
        .newTableBuilder(TableTestHelper.TEST_TABLE_ID, getCreateTableSchema())
        .withPartitionSpec(getCreateTableSpec())
        .withProperty(TableProperties.ENABLE_LOG_STORE, "true")
        .create()
        .asUnkeyedTable();
    Assert.assertFalse(PropertyUtil.propertyAsBoolean(createTable.properties(),
        TableProperties.ENABLE_SELF_OPTIMIZING, TableProperties.ENABLE_SELF_OPTIMIZING_DEFAULT));
  }

  @Test
  public void testUnkeyedRecoverableFileIO() throws TException {
    UnkeyedTable createTable = getCatalog()
        .newTableBuilder(TableTestHelper.TEST_TABLE_ID, getCreateTableSchema())
        .withPartitionSpec(getCreateTableSpec())
        .create()
        .asUnkeyedTable();
    Assert.assertFalse(createTable.io() instanceof RecoverableHadoopFileIO);

    CatalogMeta testCatalogMeta = TEST_AMS.getAmsHandler().getCatalog(CatalogTestHelper.TEST_CATALOG_NAME);
    TEST_AMS.getAmsHandler().updateMeta(
        testCatalogMeta,
        CatalogMetaProperties.TABLE_PROPERTIES_PREFIX + TableProperties.ENABLE_TABLE_TRASH,
        "true");
    getCatalog().refresh();

    ArcticTable table = getCatalog().loadTable(TableTestHelper.TEST_TABLE_ID);
    assertRecoverableFileIO(table);

    getCatalog().dropTable(TableTestHelper.TEST_TABLE_ID, true);
    createTable = getCatalog()
        .newTableBuilder(TableTestHelper.TEST_TABLE_ID, getCreateTableSchema())
        .withPartitionSpec(getCreateTableSpec())
        .create()
        .asUnkeyedTable();
    assertRecoverableFileIO(createTable);
  }

  @Test
  public void testKeyedRecoverableFileIO() throws TException {
    KeyedTable createTable = getCatalog()
        .newTableBuilder(TableTestHelper.TEST_TABLE_ID, getCreateTableSchema())
        .withPartitionSpec(getCreateTableSpec())
        .withPrimaryKeySpec(BasicTableTestHelper.PRIMARY_KEY_SPEC)
        .create()
        .asKeyedTable();
    Assert.assertFalse(createTable.io() instanceof RecoverableHadoopFileIO);
    Assert.assertFalse(createTable.changeTable().io() instanceof RecoverableHadoopFileIO);
    Assert.assertFalse(createTable.baseTable().io() instanceof RecoverableHadoopFileIO);

    CatalogMeta testCatalogMeta = TEST_AMS.getAmsHandler().getCatalog(CatalogTestHelper.TEST_CATALOG_NAME);
    TEST_AMS.getAmsHandler().updateMeta(
        testCatalogMeta,
        CatalogMetaProperties.TABLE_PROPERTIES_PREFIX + TableProperties.ENABLE_TABLE_TRASH,
        "true");
    getCatalog().refresh();

    ArcticTable table = getCatalog().loadTable(TableTestHelper.TEST_TABLE_ID);
    assertRecoverableFileIO(table);
    assertRecoverableFileIO(table.asKeyedTable().changeTable());
    assertRecoverableFileIO(table.asKeyedTable().baseTable());

    getCatalog().dropTable(TableTestHelper.TEST_TABLE_ID, true);
    createTable = getCatalog()
        .newTableBuilder(TableTestHelper.TEST_TABLE_ID, getCreateTableSchema())
        .withPartitionSpec(getCreateTableSpec())
        .withPrimaryKeySpec(BasicTableTestHelper.PRIMARY_KEY_SPEC)
        .create()
        .asKeyedTable();
    assertRecoverableFileIO(createTable);
    assertRecoverableFileIO(table.asKeyedTable().changeTable());
    assertRecoverableFileIO(table.asKeyedTable().baseTable());
  }

  private void assertRecoverableFileIO(ArcticTable arcticTable) {
    if (ArcticFileIOs.CLOSE_TRASH) {
      return;
    }

    Assert.assertTrue(arcticTable.io() instanceof RecoverableHadoopFileIO);
    RecoverableHadoopFileIO io = (RecoverableHadoopFileIO) arcticTable.io();

    String tableRootLocation = ArcticTableUtil.tableRootLocation(arcticTable);
    String trashLocation = TableTrashManagers.getTrashLocation(arcticTable.id(), tableRootLocation,
        arcticTable.properties().get(TableProperties.TABLE_TRASH_CUSTOM_ROOT_LOCATION));
    Assert.assertEquals(trashLocation, io.getTrashManager().getTrashLocation());
    Assert.assertEquals(arcticTable.id(), io.getTrashManager().tableId());
    Assert.assertEquals(TableProperties.TABLE_TRASH_FILE_PATTERN_DEFAULT, io.getTrashFilePattern());
  }

  @Test
  public void testGetTableBlockerManager() {
    KeyedTable createTable = getCatalog()
        .newTableBuilder(TableTestHelper.TEST_TABLE_ID, getCreateTableSchema())
        .withPrimaryKeySpec(BasicTableTestHelper.PRIMARY_KEY_SPEC)
        .withPartitionSpec(getCreateTableSpec())
        .create()
        .asKeyedTable();
    TableBlockerManager tableBlockerManager = getCatalog().getTableBlockerManager(createTable.id());
    Assert.assertEquals(createTable.id(), tableBlockerManager.tableIdentifier());
    Assert.assertTrue(tableBlockerManager.getBlockers().isEmpty());
  }

  @After
  public void after() {
    getCatalog().dropTable(TableTestHelper.TEST_TABLE_ID, true);
    getCatalog().dropDatabase(TableTestHelper.TEST_DB_NAME);
  }

  protected Schema getCreateTableSchema() {
    return BasicTableTestHelper.TABLE_SCHEMA;
  }

  protected PartitionSpec getCreateTableSpec() {
    return BasicTableTestHelper.SPEC;
  }
}
