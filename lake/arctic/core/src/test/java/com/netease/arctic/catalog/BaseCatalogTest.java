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

import com.netease.arctic.ams.api.properties.TableFormat;
import org.apache.iceberg.exceptions.AlreadyExistsException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class BaseCatalogTest extends CatalogTestBase {

  public BaseCatalogTest(TableFormat testFormat) {
    super(testFormat);
  }

  @Parameterized.Parameters(name = "testFormat = {0}")
  public static Object[] parameters() {
    return new Object[] {TableFormat.ICEBERG, TableFormat.MIXED_ICEBERG};
  }

  @Test
  public void testCreateAndDropDatabase() {
    String createDbName = "create_db";
    Assert.assertFalse(getCatalog().listDatabases().contains(createDbName));
    getCatalog().createDatabase(createDbName);
    Assert.assertTrue(getCatalog().listDatabases().contains(createDbName));
    getCatalog().dropDatabase(createDbName);
    Assert.assertFalse(getCatalog().listDatabases().contains(createDbName));
  }

  @Test
  public void testCreateDuplicateDatabase() {
    String createDbName = "create_db";
    Assert.assertFalse(getCatalog().listDatabases().contains(createDbName));
    getCatalog().createDatabase(createDbName);
    Assert.assertTrue(getCatalog().listDatabases().contains(createDbName));
    Assert.assertThrows(
        AlreadyExistsException.class,
        () -> getCatalog().createDatabase(createDbName));
    getCatalog().dropDatabase(createDbName);
  }
}
