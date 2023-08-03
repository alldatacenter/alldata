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

package com.netease.arctic.server.dashboard;

import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.TableTestHelper;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.catalog.CatalogTestHelper;
import com.netease.arctic.hive.catalog.HiveCatalogTestHelper;
import com.netease.arctic.hive.catalog.HiveTableTestHelper;
import com.netease.arctic.server.dashboard.model.DDLInfo;
import com.netease.arctic.server.table.AMSTableTestBase;
import com.netease.arctic.table.ArcticTable;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.List;

@RunWith(Parameterized.class)
public class TestServerTableDescriptor extends AMSTableTestBase {

  @Parameterized.Parameters(name = "{0}, {1}")
  public static Object[] parameters() {
    return new Object[][] {{new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
        new BasicTableTestHelper(true, true)},
        {new HiveCatalogTestHelper(TableFormat.MIXED_HIVE, TEST_HMS.getHiveConf()),
            new HiveTableTestHelper(true, true)}};
  }

  public TestServerTableDescriptor(CatalogTestHelper catalogTestHelper,
                                   TableTestHelper tableTestHelper) {
    super(catalogTestHelper, tableTestHelper, true);
  }

  @Test
  public void getTableOperations() {
    ServerTableDescriptor serverTableDescriptor = new ServerTableDescriptor(tableService());
    ArcticTable arcticTable = tableService().loadTable(serverTableIdentifier());
    arcticTable.updateProperties().set("key", "value1").commit();
    List<DDLInfo> tableOperations = serverTableDescriptor.getTableOperations(serverTableIdentifier());
    Assert.assertEquals(1, tableOperations.size());
    DDLInfo ddlInfo = tableOperations.get(0);
    Assert.assertEquals(ddlInfo.getDdl(),
        "ALTER TABLE test_catalog.test_db.test_table SET TBLPROPERTIES ('key'='value1')");
    arcticTable.updateProperties().set("key", "value2").commit();
    tableOperations = serverTableDescriptor.getTableOperations(serverTableIdentifier());
    Assert.assertEquals(2, tableOperations.size());
  }
}