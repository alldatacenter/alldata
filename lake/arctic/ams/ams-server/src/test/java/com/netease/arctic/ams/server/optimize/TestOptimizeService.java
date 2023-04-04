/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.server.optimize;

import com.netease.arctic.ams.api.NoSuchObjectException;
import com.netease.arctic.ams.api.TableIdentifier;
import com.netease.arctic.ams.server.AmsTestBase;
import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.PrimaryKeySpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.types.Types;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.netease.arctic.ams.server.AmsTestBase.AMS_TEST_CATALOG_NAME;
import static com.netease.arctic.ams.server.AmsTestBase.AMS_TEST_DB_NAME;
import static com.netease.arctic.table.TableProperties.ENABLE_SELF_OPTIMIZING;

public class TestOptimizeService {
  private static TableIdentifier testOptimizeIdentifier;
  private static String tableName = "testOptimizeService";
  private static ArcticTable table;

  private static IOptimizeService service = ServiceContainer.getOptimizeService();

  @BeforeClass
  public static void before() {
    Schema schema = new Schema(
        Types.NestedField.required(1, "id", Types.IntegerType.get()),
        Types.NestedField.required(2, "data", Types.StringType.get())
    );
    PrimaryKeySpec PRIMARY_KEY_SPEC = PrimaryKeySpec.builderFor(schema)
        .addColumn("id").build();

    testOptimizeIdentifier = new TableIdentifier();
    testOptimizeIdentifier.catalog = AMS_TEST_CATALOG_NAME;
    testOptimizeIdentifier.database = AMS_TEST_DB_NAME;
    testOptimizeIdentifier.tableName = tableName;
    table = AmsTestBase.catalog.newTableBuilder(
        com.netease.arctic.table.TableIdentifier.of(testOptimizeIdentifier),
        schema).withPrimaryKeySpec(PRIMARY_KEY_SPEC).create();
  }

  @Test
  public void testStopAndStartOptimize() throws NoSuchObjectException {
    service.stopOptimize(com.netease.arctic.table.TableIdentifier.of(testOptimizeIdentifier));
    table.refresh();
    Assert.assertEquals("false", table.properties().get(ENABLE_SELF_OPTIMIZING));
    service.startOptimize(com.netease.arctic.table.TableIdentifier.of(testOptimizeIdentifier));
    table.refresh();
    Assert.assertEquals("true", table.properties().get(ENABLE_SELF_OPTIMIZING));
  }
}
