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
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.catalog.CatalogTestHelper;
import com.netease.arctic.hive.catalog.HiveCatalogTestHelper;
import com.netease.arctic.hive.catalog.HiveTableTestHelper;
import com.netease.arctic.server.exception.ObjectNotExistsException;
import com.netease.arctic.table.ArcticTable;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TestTableRuntimeManager extends AMSTableTestBase {

  @Parameterized.Parameters(name = "{0}, {1}")
  public static Object[] parameters() {
    return new Object[][] {{new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
                            new BasicTableTestHelper(true, true)},
                           {new BasicCatalogTestHelper(TableFormat.ICEBERG),
                            new BasicTableTestHelper(false, true)},
                           {new HiveCatalogTestHelper(TableFormat.MIXED_HIVE, TEST_HMS.getHiveConf()),
                            new HiveTableTestHelper(true, true)}};
  }

  public TestTableRuntimeManager(CatalogTestHelper catalogTestHelper,
      TableTestHelper tableTestHelper) {
    super(catalogTestHelper, tableTestHelper, true);
  }

  @Test
  public void testLoadTable() {
    ArcticTable arcticTable = tableService().loadTable(serverTableIdentifier());
    validateArcticTable(arcticTable);

    // test load not existed table
    Assert.assertThrows(ObjectNotExistsException.class, () -> tableService().loadTable(
        ServerTableIdentifier.of(null, "unknown", "unknown", "unknown")));
  }

  @Test
  public void testTableContains() {
    Assert.assertTrue(tableService().contains(serverTableIdentifier()));
    ServerTableIdentifier copyId = ServerTableIdentifier.of(null,
        serverTableIdentifier().getCatalog(), serverTableIdentifier().getDatabase(),
        serverTableIdentifier().getTableName());
    Assert.assertFalse(tableService().contains(copyId));
    copyId = ServerTableIdentifier.of(serverTableIdentifier().getId(),
        serverTableIdentifier().getCatalog(), serverTableIdentifier().getDatabase(),
        "unknown");
    Assert.assertFalse(tableService().contains(copyId));
  }

  @Test
  public void testTableRuntime() {
    TableRuntime tableRuntime = tableService().getRuntime(serverTableIdentifier());
    validateTableRuntime(tableRuntime);
  }
}
