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

import com.google.common.collect.Lists;
import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.TableTestHelper;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.catalog.CatalogTestHelper;
import com.netease.arctic.server.exception.AlreadyExistsException;
import com.netease.arctic.server.exception.IllegalMetadataException;
import com.netease.arctic.server.exception.ObjectNotExistsException;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static com.netease.arctic.TableTestHelper.TEST_DB_NAME;
import static com.netease.arctic.catalog.CatalogTestHelper.TEST_CATALOG_NAME;

@RunWith(Parameterized.class)
public class TestDatabaseService extends AMSTableTestBase {

  @Parameterized.Parameters(name = "{0}, {1}")
  public static Object[] parameters() {
    return new Object[][] {{new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
                            new BasicTableTestHelper(true, true)}};
  }

  public TestDatabaseService(CatalogTestHelper catalogTestHelper, TableTestHelper tableTestHelper) {
    super(catalogTestHelper, tableTestHelper);
  }

  @Test
  public void testCreateAndDropDatabase() {
    // test create database
    tableService().createDatabase(TEST_CATALOG_NAME, TEST_DB_NAME);

    // test create duplicate database
    Assert.assertThrows(AlreadyExistsException.class,
        () -> tableService().createDatabase(TEST_CATALOG_NAME, TEST_DB_NAME));

    // test list database
    Assert.assertEquals(Lists.newArrayList(TEST_DB_NAME),
        tableService().listDatabases(TEST_CATALOG_NAME));

    // test drop database
    tableService().dropDatabase(TEST_CATALOG_NAME, TEST_DB_NAME);
    Assert.assertEquals(Lists.newArrayList(),
        tableService().listDatabases(TEST_CATALOG_NAME));

    // test drop unknown database
    Assert.assertThrows(ObjectNotExistsException.class,
        () -> tableService().dropDatabase(TEST_CATALOG_NAME, TEST_DB_NAME));

    // test create database in not existed catalog
    Assert.assertThrows(ObjectNotExistsException.class,
        () -> tableService().createDatabase("unknown", TEST_DB_NAME));

    // test drop database in not existed catalog
    Assert.assertThrows(ObjectNotExistsException.class,
        () -> tableService().dropDatabase("unknown", TEST_DB_NAME));
  }

  @Test
  public void testDropDatabaseWithTable() {
    Assume.assumeTrue(catalogTestHelper().tableFormat().equals(TableFormat.MIXED_ICEBERG));
    tableService().createDatabase(TEST_CATALOG_NAME, TEST_DB_NAME);
    createTable();
    Assert.assertThrows(IllegalMetadataException.class, () -> tableService().dropDatabase(TEST_CATALOG_NAME,
        TEST_DB_NAME));
    dropTable();
    tableService().dropDatabase(TEST_CATALOG_NAME, TEST_DB_NAME);
  }
}
