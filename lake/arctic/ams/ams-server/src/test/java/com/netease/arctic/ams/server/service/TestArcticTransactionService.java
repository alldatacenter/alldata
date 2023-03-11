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

package com.netease.arctic.ams.server.service;

import com.netease.arctic.ams.api.TableIdentifier;
import com.netease.arctic.ams.server.AmsTestBase;
import com.netease.arctic.ams.server.ArcticMetaStore;
import com.netease.arctic.ams.server.service.impl.ArcticTransactionService;
import com.netease.arctic.ams.server.service.impl.CatalogMetadataService;
import com.netease.arctic.ams.server.service.impl.DDLTracerService;
import com.netease.arctic.ams.server.utils.JDBCSqlSessionFactoryProvider;
import com.netease.arctic.table.PrimaryKeySpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.types.Types;
import org.apache.thrift.TException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static com.netease.arctic.ams.server.AmsTestBase.AMS_TEST_CATALOG_NAME;
import static com.netease.arctic.ams.server.AmsTestBase.AMS_TEST_DB_NAME;

@PowerMockIgnore({"javax.management.*"})
@PrepareForTest({
    ServiceContainer.class,
    JDBCSqlSessionFactoryProvider.class,
    DDLTracerService.class,
    ArcticMetaStore.class,
    CatalogMetadataService.class
})
public class TestArcticTransactionService {

  private static String tableName = "testTxAllocateTable";

  @BeforeClass
  public static void setup() {
    Schema schema = new Schema(
        Types.NestedField.required(1, "id", Types.IntegerType.get())
    );
    PrimaryKeySpec primaryKeySpec = PrimaryKeySpec.builderFor(schema)
        .addColumn("id").build();
    AmsTestBase.catalog.newTableBuilder(
        com.netease.arctic.table.TableIdentifier.of(AMS_TEST_CATALOG_NAME, AMS_TEST_DB_NAME, tableName),
        schema).withPrimaryKeySpec(primaryKeySpec).create();
  }

  @Test
  public void testTransaction() throws InterruptedException {
    ArcticTransactionService transactionService = ServiceContainer.getArcticTransactionService();
    TableIdentifier identifier = new TableIdentifier(AMS_TEST_CATALOG_NAME, AMS_TEST_DB_NAME, tableName);
    transactionService.validTable(identifier);
    CountDownLatch latch = new CountDownLatch(3);
    Set<Long> txIds = new HashSet<>();
    for (int i = 0; i < 3; i++) {
      new Thread(() -> {
        try {
          long txId = transactionService.allocateTransactionId(identifier, "");
          txIds.add(txId);
          Assert.assertTrue(txId > 0);
        } catch (TException exception) {
          Assert.fail(exception.getMessage());
        } finally {
          latch.countDown();
        }
      }).start();

    }
    latch.await();
    Assert.assertEquals(3, txIds.size());
  }
}
