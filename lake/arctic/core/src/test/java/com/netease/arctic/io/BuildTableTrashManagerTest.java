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

package com.netease.arctic.io;

import com.netease.arctic.ams.api.properties.TableFormat;
import com.netease.arctic.catalog.TableTestBase;
import com.netease.arctic.table.TableIdentifier;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.KEY_WAREHOUSE;

@RunWith(Parameterized.class)
public class BuildTableTrashManagerTest extends TableTestBase {

  public BuildTableTrashManagerTest(TableFormat testFormat, boolean keyedTable, boolean partitionedTable) {
    super(testFormat, keyedTable, partitionedTable);
  }

  @Parameterized.Parameters(name = "testFormat = {0}, keyedTable = {1}, partitionedTable = {2}")
  public static Object[][] parameters() {
    return new Object[][] {
        {TableFormat.MIXED_ICEBERG, true, true},
        {TableFormat.MIXED_ICEBERG, true, false},
        {TableFormat.MIXED_ICEBERG, false, true},
        {TableFormat.MIXED_ICEBERG, false, false}};
  }

  @Test
  public void build() {
    TableIdentifier id = getArcticTable().id();
    TableTrashManager trashManager = TableTrashManagers.build(getArcticTable());
    Assert.assertEquals(id, trashManager.tableId());
    Assert.assertEquals(getTableTrashLocation(id), trashManager.getTrashLocation());
  }

  private String getTableTrashLocation(TableIdentifier id) {
    String catalogDir = getCatalogMeta().getCatalogProperties().get(KEY_WAREHOUSE);
    return String.format("%s/%s/%s/%s", catalogDir, id.getDatabase(), id.getTableName(),
        TableTrashManagers.DEFAULT_TRASH_DIR);
  }

}
