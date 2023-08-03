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

import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.TableTestHelper;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.catalog.CatalogTestHelper;
import com.netease.arctic.catalog.TableTestBase;
import com.netease.arctic.table.TableIdentifier;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.KEY_WAREHOUSE;

@RunWith(Parameterized.class)
public class TestTableTrashManagers extends TableTestBase {

  public TestTableTrashManagers(CatalogTestHelper catalogTestHelper,
      TableTestHelper tableTestHelper) {
    super(catalogTestHelper, tableTestHelper);
  }

  @Parameterized.Parameters(name = "{0}, {2}")
  public static Object[][] parameters() {
    return new Object[][] {
        {new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
         new BasicTableTestHelper(true, true)},
        {new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
         new BasicTableTestHelper(true, false)},
        {new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
         new BasicTableTestHelper(false, true)},
        {new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
         new BasicTableTestHelper(false, false)}};
  }



  @Test
  public void testGetTrashLocation() {
    TableIdentifier id = TableTestHelper.TEST_TABLE_ID;
    Assert.assertEquals("/table/location/.trash",
        TableTrashManagers.getTrashLocation(id, "/table/location", null));
    Assert.assertEquals(String.format("/tmp/xxx/%s/%s/%s/.trash", id.getCatalog(), id.getDatabase(), id.getTableName()),
        TableTrashManagers.getTrashLocation(id, "/table/location", "/tmp/xxx"));
    Assert.assertEquals(String.format("/tmp/xxx/%s/%s/%s/.trash", id.getCatalog(), id.getDatabase(), id.getTableName()),
        TableTrashManagers.getTrashLocation(id, "/table/location", "/tmp/xxx/"));
  }

  @Test
  public void testGetTrashParentLocation() {
    TableIdentifier id = TableTestHelper.TEST_TABLE_ID;
    Assert.assertEquals(String.format("/tmp/xxx/%s/%s/%s", id.getCatalog(), id.getDatabase(), id.getTableName()),
        TableTrashManagers.getTrashParentLocation(id, "/tmp/xxx"));
    Assert.assertEquals(String.format("/tmp/xxx/%s/%s/%s", id.getCatalog(), id.getDatabase(), id.getTableName()),
        TableTrashManagers.getTrashParentLocation(id, "/tmp/xxx/"));
  }

  protected String getTableTrashLocation(TableIdentifier id) {
    String catalogDir = getCatalogMeta().getCatalogProperties().get(KEY_WAREHOUSE);
    return String.format("%s/%s/%s/%s", catalogDir, id.getDatabase(), id.getTableName(),
        TableTrashManagers.DEFAULT_TRASH_DIR);
  }

}
