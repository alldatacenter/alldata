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
import com.netease.arctic.io.RecoverableArcticFileIO;
import com.netease.arctic.io.TableTrashManager;
import com.netease.arctic.io.TableTrashManagers;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.table.blocker.TableBlockerManager;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.util.PropertyUtil;
import org.apache.thrift.TException;
import org.junit.After;
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
  }

  @Test
  public void testCreateTableWithNewCatalogProperties() throws TException {
    getCatalog().createDatabase(TableTestHelpers.TEST_DB_NAME);
    UnkeyedTable createTable = getCatalog()
        .newTableBuilder(TableTestHelpers.TEST_TABLE_ID, getCreateTableSchema())
        .withPartitionSpec(getCreateTableSpec())
        .create()
        .asUnkeyedTable();
    Assert.assertTrue(PropertyUtil.propertyAsBoolean(createTable.properties(),
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
    Assert.assertFalse(PropertyUtil.propertyAsBoolean(createTable.properties(),
        TableProperties.ENABLE_SELF_OPTIMIZING, TableProperties.ENABLE_SELF_OPTIMIZING_DEFAULT));
  }

  @Test
  public void testUnkeyedRecoverableFileIO() throws TException {
    getCatalog().createDatabase(TableTestHelpers.TEST_DB_NAME);
    UnkeyedTable createTable = getCatalog()
        .newTableBuilder(TableTestHelpers.TEST_TABLE_ID, getCreateTableSchema())
        .withPartitionSpec(getCreateTableSpec())
        .create()
        .asUnkeyedTable();
    Assert.assertFalse(createTable.io() instanceof RecoverableArcticFileIO);

    CatalogMeta testCatalogMeta = TEST_AMS.getAmsHandler().getCatalog(TEST_CATALOG_NAME);
    TEST_AMS.getAmsHandler().updateMeta(testCatalogMeta,
        CatalogMetaProperties.TABLE_PROPERTIES_PREFIX + TableProperties.ENABLE_TABLE_TRASH,
        "true");
    getCatalog().refresh();

    ArcticTable table = getCatalog().loadTable(TableTestHelpers.TEST_TABLE_ID);
    assertRecoverableFileIO(table);

    getCatalog().dropTable(TableTestHelpers.TEST_TABLE_ID, true);
    createTable = getCatalog()
        .newTableBuilder(TableTestHelpers.TEST_TABLE_ID, getCreateTableSchema())
        .withPartitionSpec(getCreateTableSpec())
        .create()
        .asUnkeyedTable();
    assertRecoverableFileIO(createTable);
  }

  @Test
  public void testKeyedRecoverableFileIO() throws TException {
    getCatalog().createDatabase(TableTestHelpers.TEST_DB_NAME);
    KeyedTable createTable = getCatalog()
        .newTableBuilder(TableTestHelpers.TEST_TABLE_ID, getCreateTableSchema())
        .withPartitionSpec(getCreateTableSpec())
        .withPrimaryKeySpec(TableTestHelpers.PRIMARY_KEY_SPEC)
        .create()
        .asKeyedTable();
    Assert.assertFalse(createTable.io() instanceof RecoverableArcticFileIO);
    Assert.assertFalse(createTable.changeTable().io() instanceof RecoverableArcticFileIO);
    Assert.assertFalse(createTable.baseTable().io() instanceof RecoverableArcticFileIO);

    CatalogMeta testCatalogMeta = TEST_AMS.getAmsHandler().getCatalog(TEST_CATALOG_NAME);
    TEST_AMS.getAmsHandler().updateMeta(testCatalogMeta,
        CatalogMetaProperties.TABLE_PROPERTIES_PREFIX + TableProperties.ENABLE_TABLE_TRASH,
        "true");
    getCatalog().refresh();

    ArcticTable table = getCatalog().loadTable(TableTestHelpers.TEST_TABLE_ID);
    assertRecoverableFileIO(table);
    assertRecoverableFileIO(table.asKeyedTable().changeTable());
    assertRecoverableFileIO(table.asKeyedTable().baseTable());

    getCatalog().dropTable(TableTestHelpers.TEST_TABLE_ID, true);
    createTable = getCatalog()
        .newTableBuilder(TableTestHelpers.TEST_TABLE_ID, getCreateTableSchema())
        .withPartitionSpec(getCreateTableSpec())
        .withPrimaryKeySpec(TableTestHelpers.PRIMARY_KEY_SPEC)
        .create()
        .asKeyedTable();
    assertRecoverableFileIO(createTable);
    assertRecoverableFileIO(table.asKeyedTable().changeTable());
    assertRecoverableFileIO(table.asKeyedTable().baseTable());
  }

  private void assertRecoverableFileIO(ArcticTable arcticTable) {
    Assert.assertTrue(arcticTable.io() instanceof RecoverableArcticFileIO);
    RecoverableArcticFileIO io = (RecoverableArcticFileIO) arcticTable.io();
    TableTrashManager expected = TableTrashManagers.build(arcticTable);
    Assert.assertEquals(expected.getTrashLocation(), io.getTrashManager().getTrashLocation());
    Assert.assertEquals(expected.tableId(), io.getTrashManager().tableId());
    Assert.assertEquals(TableProperties.TABLE_TRASH_FILE_PATTERN_DEFAULT, io.getTrashFilePattern());
  }

  @Test
  public void testGetTableBlockerManager() {
    getCatalog().createDatabase(TableTestHelpers.TEST_DB_NAME);
    KeyedTable createTable = getCatalog()
        .newTableBuilder(TableTestHelpers.TEST_TABLE_ID, getCreateTableSchema())
        .withPrimaryKeySpec(TableTestHelpers.PRIMARY_KEY_SPEC)
        .withPartitionSpec(getCreateTableSpec())
        .create()
        .asKeyedTable();
    TableBlockerManager tableBlockerManager = getCatalog().getTableBlockerManager(createTable.id());
    Assert.assertEquals(createTable.id(), tableBlockerManager.tableIdentifier());
    Assert.assertTrue(tableBlockerManager.getBlockers().isEmpty());
  }

  @After
  public void after() {
    getCatalog().dropTable(TableTestHelpers.TEST_TABLE_ID, true);
    getCatalog().dropDatabase(TableTestHelpers.TEST_DB_NAME);
  }

  protected Schema getCreateTableSchema() {
    return TableTestHelpers.TABLE_SCHEMA;
  }

  protected PartitionSpec getCreateTableSpec() {
    return TableTestHelpers.SPEC;
  }
}
