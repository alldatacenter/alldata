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

package com.netease.arctic.server.table;

import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.TableTestHelper;
import com.netease.arctic.ams.api.BlockableOperation;
import com.netease.arctic.ams.api.Blocker;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.ams.api.TableIdentifier;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.catalog.CatalogTestHelper;
import com.netease.arctic.hive.catalog.HiveCatalogTestHelper;
import com.netease.arctic.hive.catalog.HiveTableTestHelper;
import com.netease.arctic.server.ArcticManagementConf;
import com.netease.arctic.server.exception.AlreadyExistsException;
import com.netease.arctic.server.exception.BlockerConflictException;
import com.netease.arctic.server.exception.ObjectNotExistsException;
import com.netease.arctic.table.blocker.RenewableBlocker;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.netease.arctic.TableTestHelper.TEST_DB_NAME;
import static com.netease.arctic.catalog.CatalogTestHelper.TEST_CATALOG_NAME;

@RunWith(Parameterized.class)
public class TestTableService extends AMSTableTestBase {

  @Parameterized.Parameters(name = "{0}, {1}")
  public static Object[] parameters() {
    return new Object[][] {{new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
                            new BasicTableTestHelper(true, true)},
                           {new HiveCatalogTestHelper(TableFormat.MIXED_HIVE, TEST_HMS.getHiveConf()),
                            new HiveTableTestHelper(true, true)}};
  }

  public TestTableService(CatalogTestHelper catalogTestHelper, TableTestHelper tableTestHelper) {
    super(catalogTestHelper, tableTestHelper);
  }

  @Test
  public void testCreateAndDropTable() {
    tableService().createDatabase(TEST_CATALOG_NAME, TEST_DB_NAME);

    // test create table
    createTable();
    Assert.assertEquals(tableMeta(), tableService().loadTableMetadata(
        tableMeta().getTableIdentifier()).buildTableMeta());

    // test list tables
    List<TableIdentifier> tableIdentifierList = tableService().listTables(TEST_CATALOG_NAME,
        TEST_DB_NAME);
    Assert.assertEquals(1, tableIdentifierList.size());
    Assert.assertEquals(tableMeta().getTableIdentifier(), tableIdentifierList.get(0));

    // test list table metadata
    List<TableMetadata> tableMetadataList = tableService().listTableMetas();
    Assert.assertEquals(1, tableMetadataList.size());
    Assert.assertEquals(tableMeta(), tableMetadataList.get(0).buildTableMeta());

    tableMetadataList = tableService().listTableMetas(TEST_CATALOG_NAME, TEST_DB_NAME);
    Assert.assertEquals(1, tableMetadataList.size());
    Assert.assertEquals(tableMeta(), tableMetadataList.get(0).buildTableMeta());

    // test table exist
    Assert.assertTrue(tableService().tableExist(tableMeta().getTableIdentifier()));

    // test create duplicate table
    Assert.assertThrows(AlreadyExistsException.class, () -> tableService().createTable(TEST_CATALOG_NAME,
        tableMetadata()));

    // test create table with wrong catalog name
    Assert.assertThrows(ObjectNotExistsException.class, () -> {
      TableMetadata copyMetadata = new TableMetadata(serverTableIdentifier(), tableMeta(), catalogMeta());
      copyMetadata.getTableIdentifier().setCatalog("unknown");
      tableService().createTable(TEST_CATALOG_NAME, copyMetadata);
    });

    // test create table in not existed catalog
    Assert.assertThrows(ObjectNotExistsException.class, () -> {
      TableMetadata copyMetadata = new TableMetadata(serverTableIdentifier(), tableMeta(), catalogMeta());
      copyMetadata.getTableIdentifier().setCatalog("unknown");
      tableService().createTable("unknown", copyMetadata);
    });

    if (catalogTestHelper().tableFormat().equals(TableFormat.MIXED_ICEBERG)) {
      // test create table in not existed database
      Assert.assertThrows(
          ObjectNotExistsException.class,
          () -> {
            TableMetadata copyMetadata = new TableMetadata(serverTableIdentifier(), tableMeta(), catalogMeta());
            copyMetadata.getTableIdentifier().setDatabase("unknown");
            tableService().createTable(TEST_CATALOG_NAME, copyMetadata);
          });
    }

    // test drop table
    dropTable();
    Assert.assertEquals(0, tableService().listManagedTables().size());
    Assert.assertEquals(0, tableService().listTables(TEST_CATALOG_NAME, TEST_DB_NAME).size());
    Assert.assertEquals(0, tableService().listTableMetas().size());
    Assert.assertEquals(0, tableService().listTableMetas(TEST_CATALOG_NAME, TEST_DB_NAME).size());
    Assert.assertFalse(tableService().tableExist(tableMeta().getTableIdentifier()));

    // test drop not existed table
    Assert.assertThrows(ObjectNotExistsException.class,
        () -> tableService().dropTableMetadata(tableMeta().getTableIdentifier(), true));

    tableService().dropDatabase(TEST_CATALOG_NAME, TEST_DB_NAME);
  }

  @Test
  public void testBlockAndRelease() {
    createDatabase();
    createTable();
    TableIdentifier tableIdentifier = serverTableIdentifier().getIdentifier();

    List<BlockableOperation> operations = new ArrayList<>();
    operations.add(BlockableOperation.BATCH_WRITE);
    operations.add(BlockableOperation.OPTIMIZE);

    assertBlockerCnt(0);
    assertNotBlocked(BlockableOperation.OPTIMIZE);
    assertNotBlocked(BlockableOperation.BATCH_WRITE);

    Blocker block = tableService().block(tableIdentifier, operations, getProperties());
    assertBlocker(block, operations);
    assertBlockerCnt(1);
    assertBlocked(BlockableOperation.OPTIMIZE);
    assertBlocked(BlockableOperation.BATCH_WRITE);

    tableService().releaseBlocker(tableIdentifier, block.getBlockerId() + "");
    assertBlockerCnt(0);
    assertNotBlocked(BlockableOperation.OPTIMIZE);
    assertNotBlocked(BlockableOperation.BATCH_WRITE);

    dropTable();
    dropDatabase();
  }

  @Test
  public void testBlockConflict() {
    createDatabase();
    createTable();
    TableIdentifier tableIdentifier = serverTableIdentifier().getIdentifier();

    List<BlockableOperation> operations = new ArrayList<>();
    operations.add(BlockableOperation.BATCH_WRITE);
    operations.add(BlockableOperation.OPTIMIZE);

    assertBlockerCnt(0);
    assertNotBlocked(BlockableOperation.OPTIMIZE);
    assertNotBlocked(BlockableOperation.BATCH_WRITE);

    Blocker block = tableService().block(tableIdentifier, operations, getProperties());

    Assert.assertThrows("should be conflict", BlockerConflictException.class,
        () -> tableService().block(tableIdentifier, operations, getProperties()));

    assertBlocker(block, operations);
    assertBlockerCnt(1);
    assertBlocked(BlockableOperation.OPTIMIZE);
    assertBlocked(BlockableOperation.BATCH_WRITE);

    tableService().releaseBlocker(tableIdentifier, block.getBlockerId() + "");
    assertBlockerCnt(0);
    assertNotBlocked(BlockableOperation.OPTIMIZE);
    assertNotBlocked(BlockableOperation.BATCH_WRITE);

    dropTable();
    dropDatabase();
  }

  @Test
  public void testRenewBlocker() throws InterruptedException {
    createDatabase();
    createTable();
    TableIdentifier tableIdentifier = serverTableIdentifier().getIdentifier();

    List<BlockableOperation> operations = new ArrayList<>();
    operations.add(BlockableOperation.BATCH_WRITE);
    operations.add(BlockableOperation.OPTIMIZE);

    assertBlockerCnt(0);
    assertNotBlocked(BlockableOperation.OPTIMIZE);
    assertNotBlocked(BlockableOperation.BATCH_WRITE);

    Blocker block = tableService().block(tableIdentifier, operations, getProperties());
    Thread.sleep(1);

    tableService().renewBlocker(tableIdentifier, block.getBlockerId() + "");
    assertBlockerCnt(1);
    assertBlocked(BlockableOperation.OPTIMIZE);
    assertBlocked(BlockableOperation.BATCH_WRITE);

    assertBlocker(block, operations);
    assertBlockerCnt(1);
    assertBlocked(BlockableOperation.OPTIMIZE);
    assertBlocked(BlockableOperation.BATCH_WRITE);
    assertBlockerRenewed(tableService().getBlockers(tableIdentifier).get(0));

    tableService().releaseBlocker(tableIdentifier, block.getBlockerId() + "");
    assertBlockerCnt(0);
    assertNotBlocked(BlockableOperation.OPTIMIZE);
    assertNotBlocked(BlockableOperation.BATCH_WRITE);

    dropTable();
    dropDatabase();
  }

  @Test
  public void testAutoIncrementBlockerId() {
    createDatabase();
    createTable();
    TableIdentifier tableIdentifier = serverTableIdentifier().getIdentifier();

    List<BlockableOperation> operations = new ArrayList<>();
    operations.add(BlockableOperation.BATCH_WRITE);
    operations.add(BlockableOperation.OPTIMIZE);

    Blocker block = tableService().block(tableIdentifier, operations, getProperties());

    tableService().releaseBlocker(tableIdentifier, block.getBlockerId() + "");

    Blocker block2 = tableService().block(tableIdentifier, operations, getProperties());

    Assert.assertEquals(Long.parseLong(block2.getBlockerId()) - Long.parseLong(block.getBlockerId()), 1);

    tableService().releaseBlocker(tableIdentifier, block2.getBlockerId() + "");

    dropTable();
    dropDatabase();
  }

  private void assertBlocker(Blocker block, List<BlockableOperation> operations) {
    Assert.assertEquals(operations.size(), block.getOperations().size());
    operations.forEach(operation -> Assert.assertTrue(block.getOperations().contains(operation)));
    Assert.assertEquals(getProperties().size() + 3, block.getProperties().size());
    getProperties().forEach((key, value) -> Assert.assertEquals(block.getProperties().get(key), value));
    long timeout = ArcticManagementConf.BLOCKER_TIMEOUT.defaultValue();
    Assert.assertEquals(timeout + "", block.getProperties().get(RenewableBlocker.BLOCKER_TIMEOUT));

    Assert.assertEquals(timeout, Long.parseLong(block.getProperties().get(RenewableBlocker.EXPIRATION_TIME_PROPERTY)) -
        Long.parseLong(block.getProperties().get(RenewableBlocker.CREATE_TIME_PROPERTY)));
  }

  private void assertBlockerRenewed(Blocker block) {
    long timeout = ArcticManagementConf.BLOCKER_TIMEOUT.defaultValue();
    long actualTimeout = Long.parseLong(block.getProperties().get(RenewableBlocker.EXPIRATION_TIME_PROPERTY)) -
        Long.parseLong(block.getProperties().get(RenewableBlocker.CREATE_TIME_PROPERTY));
    Assert.assertTrue("actualTimeout is " + actualTimeout, actualTimeout > timeout);
  }

  private void assertNotBlocked(BlockableOperation operation) {
    Assert.assertFalse(isBlocked(operation));
    Assert.assertFalse(isTableRuntimeBlocked(operation));
  }

  private void assertBlocked(BlockableOperation operation) {
    Assert.assertTrue(isBlocked(operation));
    Assert.assertTrue(isTableRuntimeBlocked(operation));
  }

  private boolean isBlocked(BlockableOperation operation) {
    return tableService().getBlockers(serverTableIdentifier().getIdentifier()).stream()
        .anyMatch(blocker -> blocker.getOperations().contains(operation));
  }

  private boolean isTableRuntimeBlocked(BlockableOperation operation) {
    return tableService().getRuntime(serverTableIdentifier()).isBlocked(operation);
  }

  private void assertBlockerCnt(int i) {
    List<Blocker> blockers;
    blockers = tableService().getBlockers(serverTableIdentifier().getIdentifier());
    Assert.assertEquals(i, blockers.size());
  }
  
  private Map<String, String> getProperties() {
    Map<String, String> properties = new HashMap<>();
    properties.put("test_key", "test_value");
    properties.put("2", "");
    properties.put("3", null);
    return properties;
  }
}
