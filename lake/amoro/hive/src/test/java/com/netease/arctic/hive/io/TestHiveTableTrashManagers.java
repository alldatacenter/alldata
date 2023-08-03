/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.hive.io;

import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.hive.TestHMS;
import com.netease.arctic.hive.catalog.HiveCatalogTestHelper;
import com.netease.arctic.hive.catalog.HiveTableTestHelper;
import com.netease.arctic.io.TableTrashManagers;
import com.netease.arctic.io.TestTableTrashManagers;
import com.netease.arctic.table.TableIdentifier;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TestHiveTableTrashManagers extends TestTableTrashManagers {

  @ClassRule
  public static TestHMS TEST_HMS = new TestHMS();

  public TestHiveTableTrashManagers(
      boolean keyedTable,
      boolean partitionedTable) {
    super(
        new HiveCatalogTestHelper(TableFormat.MIXED_HIVE, TEST_HMS.getHiveConf()),
        new HiveTableTestHelper(keyedTable, partitionedTable));
  }

  @Parameterized.Parameters(name = "keyedTable = {0}, partitionedTable = {1}")
  public static Object[][] parameters() {
    return new Object[][] {
        {true, true},
        {true, false},
        {false, true},
        {false, false}};
  }

  protected String getTableTrashLocation(TableIdentifier id) {
    return String.format("file:/%s/%s.db/%s/%s", TEST_HMS.getWareHouseLocation(), id.getDatabase(), id.getTableName(),
        TableTrashManagers.DEFAULT_TRASH_DIR);
  }
}
