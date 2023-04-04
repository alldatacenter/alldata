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

package com.netease.arctic.ams.server.service.impl;

import com.netease.arctic.TableTestBase;
import com.netease.arctic.ams.api.BlockableOperation;
import com.netease.arctic.ams.api.NoSuchObjectException;
import com.netease.arctic.ams.api.OperationConflictException;
import com.netease.arctic.ams.server.config.ArcticMetaStoreConf;
import com.netease.arctic.ams.server.model.TableBlocker;
import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.blocker.RenewableBlocker;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.netease.arctic.ams.server.AmsTestBase.AMS_TEST_CATALOG_NAME;

public class TestTableBlockerService extends TableTestBase {
  private final TableIdentifier tableIdentifier = TableIdentifier.of(AMS_TEST_CATALOG_NAME, "test", "test");
  private final Map<String, String> properties = new HashMap<>();

  {
    properties.put("test_key", "test_value");
    properties.put("2", "");
    properties.put("3", null);
  }

  @Test
  public void testBlockAndRelease() throws OperationConflictException {
    List<BlockableOperation> operations = new ArrayList<>();
    operations.add(BlockableOperation.BATCH_WRITE);
    operations.add(BlockableOperation.OPTIMIZE);

    assertBlockerCnt(0);
    assertNotBlocked(BlockableOperation.OPTIMIZE);
    assertNotBlocked(BlockableOperation.BATCH_WRITE);

    TableBlocker block = ServiceContainer.getTableBlockerService().block(tableIdentifier, operations, properties);
    assertBlocker(block, operations);
    assertBlockerCnt(1);
    assertBlocked(BlockableOperation.OPTIMIZE);
    assertBlocked(BlockableOperation.BATCH_WRITE);

    ServiceContainer.getTableBlockerService().release(tableIdentifier, block.getBlockerId() + "");
    assertBlockerCnt(0);
    assertNotBlocked(BlockableOperation.OPTIMIZE);
    assertNotBlocked(BlockableOperation.BATCH_WRITE);
  }

  @Test
  public void testBlockConflict() throws OperationConflictException {
    List<BlockableOperation> operations = new ArrayList<>();
    operations.add(BlockableOperation.BATCH_WRITE);
    operations.add(BlockableOperation.OPTIMIZE);

    assertBlockerCnt(0);
    assertNotBlocked(BlockableOperation.OPTIMIZE);
    assertNotBlocked(BlockableOperation.BATCH_WRITE);

    TableBlocker block = ServiceContainer.getTableBlockerService().block(tableIdentifier, operations, properties);

    Assert.assertThrows("should be conflict", OperationConflictException.class,
        () -> ServiceContainer.getTableBlockerService().block(tableIdentifier, operations, properties));

    assertBlocker(block, operations);
    assertBlockerCnt(1);
    assertBlocked(BlockableOperation.OPTIMIZE);
    assertBlocked(BlockableOperation.BATCH_WRITE);

    ServiceContainer.getTableBlockerService().release(tableIdentifier, block.getBlockerId() + "");
    assertBlockerCnt(0);
    assertNotBlocked(BlockableOperation.OPTIMIZE);
    assertNotBlocked(BlockableOperation.BATCH_WRITE);
  }

  @Test
  public void testRenew() throws OperationConflictException, NoSuchObjectException {
    List<BlockableOperation> operations = new ArrayList<>();
    operations.add(BlockableOperation.BATCH_WRITE);
    operations.add(BlockableOperation.OPTIMIZE);

    assertBlockerCnt(0);
    assertNotBlocked(BlockableOperation.OPTIMIZE);
    assertNotBlocked(BlockableOperation.BATCH_WRITE);

    TableBlocker block = ServiceContainer.getTableBlockerService().block(tableIdentifier, operations, properties);

    ServiceContainer.getTableBlockerService().renew(tableIdentifier, block.getBlockerId() + "");
    assertBlockerCnt(1);
    assertBlocked(BlockableOperation.OPTIMIZE);
    assertBlocked(BlockableOperation.BATCH_WRITE);

    assertBlocker(block, operations);
    assertBlockerCnt(1);
    assertBlocked(BlockableOperation.OPTIMIZE);
    assertBlocked(BlockableOperation.BATCH_WRITE);
    assertBlockerRenewed(ServiceContainer.getTableBlockerService().getBlockers(tableIdentifier).get(0));

    ServiceContainer.getTableBlockerService().release(tableIdentifier, block.getBlockerId() + "");
    assertBlockerCnt(0);
    assertNotBlocked(BlockableOperation.OPTIMIZE);
    assertNotBlocked(BlockableOperation.BATCH_WRITE);
  }

  @Test
  public void testExpire() {
    TableBlocker tableBlocker = new TableBlocker();
    tableBlocker.setTableIdentifier(tableIdentifier);
    tableBlocker.setExpirationTime(System.currentTimeMillis() - 10);
    tableBlocker.setCreateTime(System.currentTimeMillis() - 20);
    tableBlocker.setOperations(Collections.singletonList(BlockableOperation.OPTIMIZE.name()));
    ServiceContainer.getTableBlockerService().insertTableBlocker(tableBlocker);

    TableBlocker tableBlocker2 = new TableBlocker();
    tableBlocker2.setTableIdentifier(tableIdentifier);
    tableBlocker2.setExpirationTime(System.currentTimeMillis() + 100000);
    tableBlocker2.setCreateTime(System.currentTimeMillis() - 20);
    tableBlocker2.setOperations(Collections.singletonList(BlockableOperation.BATCH_WRITE.name()));
    ServiceContainer.getTableBlockerService().insertTableBlocker(tableBlocker2);

    int deleted = ServiceContainer.getTableBlockerService().expireBlockers(tableIdentifier);
    Assert.assertEquals(1, deleted);

    ServiceContainer.getTableBlockerService().release(tableIdentifier, tableBlocker2.getBlockerId() + "");
    assertBlockerCnt(0);
  }

  @Test
  public void testClear() {
    TableBlocker tableBlocker = new TableBlocker();
    tableBlocker.setTableIdentifier(tableIdentifier);
    tableBlocker.setExpirationTime(System.currentTimeMillis() - 10);
    tableBlocker.setCreateTime(System.currentTimeMillis() - 20);
    tableBlocker.setOperations(Collections.singletonList(BlockableOperation.OPTIMIZE.name()));
    ServiceContainer.getTableBlockerService().insertTableBlocker(tableBlocker);

    TableBlocker tableBlocker2 = new TableBlocker();
    tableBlocker2.setTableIdentifier(tableIdentifier);
    tableBlocker2.setExpirationTime(System.currentTimeMillis() + 100000);
    tableBlocker2.setCreateTime(System.currentTimeMillis() - 20);
    tableBlocker2.setOperations(Collections.singletonList(BlockableOperation.BATCH_WRITE.name()));
    ServiceContainer.getTableBlockerService().insertTableBlocker(tableBlocker2);

    int deleted = ServiceContainer.getTableBlockerService().clearBlockers(tableIdentifier);
    Assert.assertEquals(2, deleted);
    assertBlockerCnt(0);
  }

  @Test
  public void testAutoIncrementBlockerId() throws OperationConflictException {
    List<BlockableOperation> operations = new ArrayList<>();
    operations.add(BlockableOperation.BATCH_WRITE);
    operations.add(BlockableOperation.OPTIMIZE);

    TableBlocker block = ServiceContainer.getTableBlockerService().block(tableIdentifier, operations, properties);

    ServiceContainer.getTableBlockerService().release(tableIdentifier, block.getBlockerId() + "");

    TableBlocker block2 = ServiceContainer.getTableBlockerService().block(tableIdentifier, operations, properties);

    Assert.assertEquals(block2.getBlockerId() - block.getBlockerId(), 1);

    ServiceContainer.getTableBlockerService().release(tableIdentifier, block2.getBlockerId() + "");
  }

  private void assertBlocker(TableBlocker block, List<BlockableOperation> operations) {
    Assert.assertEquals(operations.size(), block.getOperations().size());
    operations.forEach(operation -> Assert.assertTrue(block.getOperations().contains(operation.name())));
    Assert.assertEquals(properties.size() + 1, block.getProperties().size());
    properties.forEach((key, value) -> Assert.assertEquals(block.getProperties().get(key), value));
    long timeout = ArcticMetaStoreConf.BLOCKER_TIMEOUT.defaultValue();
    Assert.assertEquals(timeout + "", block.getProperties().get(RenewableBlocker.BLOCKER_TIMEOUT));

    Assert.assertEquals(timeout, block.getExpirationTime() - block.getCreateTime());
  }

  private void assertBlockerRenewed(TableBlocker block) {
    long timeout = ArcticMetaStoreConf.BLOCKER_TIMEOUT.defaultValue();
    Assert.assertTrue(block.getExpirationTime() - block.getCreateTime() > timeout);
  }

  private void assertNotBlocked(BlockableOperation optimize) {
    Assert.assertFalse(
        ServiceContainer.getTableBlockerService().isBlocked(tableIdentifier, optimize));
  }

  private void assertBlocked(BlockableOperation optimize) {
    Assert.assertTrue(
        ServiceContainer.getTableBlockerService().isBlocked(tableIdentifier, optimize));
  }

  private void assertBlockerCnt(int i) {
    List<TableBlocker> blockers;
    blockers = ServiceContainer.getTableBlockerService().getBlockers(tableIdentifier);
    Assert.assertEquals(i, blockers.size());
  }

}
