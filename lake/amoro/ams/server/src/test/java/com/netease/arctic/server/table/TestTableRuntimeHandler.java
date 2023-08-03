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

import com.clearspring.analytics.util.Lists;
import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.TableTestHelper;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.catalog.CatalogTestHelper;
import com.netease.arctic.hive.catalog.HiveCatalogTestHelper;
import com.netease.arctic.hive.catalog.HiveTableTestHelper;
import com.netease.arctic.server.optimizing.OptimizingStatus;
import com.netease.arctic.server.utils.Configurations;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableProperties;
import org.apache.iceberg.util.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.List;

@RunWith(Parameterized.class)
public class TestTableRuntimeHandler extends AMSTableTestBase {

  private DefaultTableService tableService;

  @Parameterized.Parameters(name = "{0}, {1}")
  public static Object[] parameters() {
    return new Object[][] {{new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
                            new BasicTableTestHelper(true, true)},
                           {new BasicCatalogTestHelper(TableFormat.ICEBERG),
                            new BasicTableTestHelper(false, true)},
                           {new HiveCatalogTestHelper(TableFormat.MIXED_HIVE, TEST_HMS.getHiveConf()),
                            new HiveTableTestHelper(true, true)}};
  }

  public TestTableRuntimeHandler(
      CatalogTestHelper catalogTestHelper,
      TableTestHelper tableTestHelper) {
    super(catalogTestHelper, tableTestHelper, false);
  }

  @Test
  public void testInitialize() {
    tableService = new DefaultTableService(new Configurations());
    TestHandler handler = new TestHandler();
    tableService.addHandlerChain(handler);
    tableService.initialize();
    createDatabase();
    createTable();
    ServerTableIdentifier createTableId = tableService().listManagedTables().get(0);
    Assert.assertEquals(1, handler.getAddedTables().size());
    validateArcticTable(handler.getAddedTables().get(0).first());
    validateTableRuntime(handler.getAddedTables().get(0).second());
    tableService.dispose();
    Assert.assertTrue(handler.isDisposed());

    // initialize with a history table
    tableService = new DefaultTableService(new Configurations());
    handler = new TestHandler();
    tableService.addHandlerChain(handler);
    tableService.initialize();
    Assert.assertEquals(1, handler.getInitTables().size());
    Assert.assertEquals(createTableId.getId().longValue(), handler.getInitTables().get(0).getTableId());

    // test change properties
    tableService().loadTable(createTableId).updateProperties()
        .set(TableProperties.ENABLE_ORPHAN_CLEAN, "true").commit();
    tableService().getRuntime(createTableId).refresh(tableService.loadTable(serverTableIdentifier()));
    Assert.assertEquals(1, handler.getConfigChangedTables().size());
    validateTableRuntime(handler.getConfigChangedTables().get(0).first());
    Assert.assertTrue(handler.getConfigChangedTables().get(0).first().getTableConfiguration().isCleanOrphanEnabled());
    Assert.assertFalse(handler.getConfigChangedTables().get(0).second().isCleanOrphanEnabled());

    // drop table
    dropTable();
    Assert.assertEquals(1, handler.getRemovedTables().size());

    dropDatabase();
    tableService.dispose();
    tableService = null;
  }

  protected DefaultTableService tableService() {
    if (tableService != null) {
      return tableService;
    } else {
      return super.tableService();
    }
  }

  static class TestHandler extends RuntimeHandlerChain {

    private final List<TableRuntimeMeta> initTables = Lists.newArrayList();
    private final List<Pair<TableRuntime, OptimizingStatus>> statusChangedTables = Lists.newArrayList();
    private final List<Pair<TableRuntime, TableConfiguration>> configChangedTables = Lists.newArrayList();
    private final List<Pair<ArcticTable, TableRuntime>> addedTables = Lists.newArrayList();
    private final List<TableRuntime> removedTables = Lists.newArrayList();
    private boolean disposed = false;

    @Override
    protected void handleStatusChanged(
        TableRuntime tableRuntime, OptimizingStatus originalStatus) {
      statusChangedTables.add(Pair.of(tableRuntime, originalStatus));
    }

    @Override
    protected void handleConfigChanged(
        TableRuntime tableRuntime, TableConfiguration originalConfig) {
      configChangedTables.add(Pair.of(tableRuntime, originalConfig));
    }

    @Override
    protected void handleTableAdded(
        ArcticTable table, TableRuntime tableRuntime) {
      addedTables.add(Pair.of(table, tableRuntime));
    }

    @Override
    protected void handleTableRemoved(TableRuntime tableRuntime) {
      removedTables.add(tableRuntime);
    }

    @Override
    protected void initHandler(List<TableRuntimeMeta> tableRuntimeMetaList) {
      initTables.addAll(tableRuntimeMetaList);
    }

    @Override
    protected void doDispose() {
      disposed = true;
    }

    public List<TableRuntimeMeta> getInitTables() {
      return initTables;
    }

    public List<Pair<TableRuntime, OptimizingStatus>> getStatusChangedTables() {
      return statusChangedTables;
    }

    public List<Pair<TableRuntime, TableConfiguration>> getConfigChangedTables() {
      return configChangedTables;
    }

    public List<Pair<ArcticTable, TableRuntime>> getAddedTables() {
      return addedTables;
    }

    public List<TableRuntime> getRemovedTables() {
      return removedTables;
    }

    public boolean isDisposed() {
      return disposed;
    }
  }
}
