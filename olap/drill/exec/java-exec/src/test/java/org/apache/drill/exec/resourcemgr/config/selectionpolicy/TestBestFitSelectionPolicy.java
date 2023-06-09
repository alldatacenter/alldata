/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.resourcemgr.config.selectionpolicy;

import org.apache.drill.categories.ResourceManagerTest;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.resourcemgr.NodeResources;
import org.apache.drill.exec.resourcemgr.config.QueryQueueConfig;
import org.apache.drill.exec.resourcemgr.config.RMCommonDefaults;
import org.apache.drill.exec.resourcemgr.config.ResourcePool;
import org.apache.drill.exec.resourcemgr.config.exception.QueueSelectionException;
import org.apache.drill.test.BaseTest;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(ResourceManagerTest.class)
public final class TestBestFitSelectionPolicy extends BaseTest {

  private static QueueSelectionPolicy selectionPolicy;

  private static final QueryContext queryContext = mock(QueryContext.class);

  private static final NodeResources queryMaxResources = new NodeResources(1500*1024L*1024L, 2);

  private static final List<Long> poolMemory = new ArrayList<>();

  @BeforeClass
  public static void testSetup() {
    selectionPolicy = QueueSelectionPolicyFactory.createSelectionPolicy
      (RMCommonDefaults.ROOT_POOL_DEFAULT_QUEUE_SELECTION_POLICY);
    when(queryContext.getQueryId()).thenReturn(UserBitShared.QueryId.getDefaultInstance());
  }

  @After
  public void afterTestSetup() {
    poolMemory.clear();
  }

  private void testCommonHelper(long expectedPoolMem) throws Exception {
    List<ResourcePool> inputPools = new ArrayList<>();
    ResourcePool expectedPool = null;

    for (Long poolMemory : poolMemory) {
      final ResourcePool testPool = mock(ResourcePool.class);
      final QueryQueueConfig testPoolQueue = mock(QueryQueueConfig.class);
      when(testPool.getQueryQueue()).thenReturn(testPoolQueue);
      when(testPoolQueue.getMaxQueryMemoryInMBPerNode()).thenReturn(poolMemory);
      inputPools.add(testPool);

      if (poolMemory == expectedPoolMem) {
        expectedPool = testPool;
      }
    }

    ResourcePool selectedPool = selectionPolicy.selectQueue(inputPools, queryContext, queryMaxResources);
    assertEquals("Selected Pool and expected pool is different", expectedPool, selectedPool);
  }

  @Test(expected = QueueSelectionException.class)
  public void testWithNoPool() throws Exception {
    testCommonHelper(0);
  }

  @Test
  public void testWithSinglePool() throws Exception {
    poolMemory.add(1000L);
    testCommonHelper(1000);
  }

  @Test
  public void testWithMultiplePoolWithGreaterMaxNodeMemory() throws Exception {
    poolMemory.add(2500L);
    poolMemory.add(2000L);
    testCommonHelper(2000);
  }

  @Test
  public void testWithMultiplePoolWithLesserMaxNodeMemory() throws Exception {
    poolMemory.add(700L);
    poolMemory.add(500L);
    testCommonHelper(700);
  }

  @Test
  public void testMixOfPoolLess_Greater_MaxNodeMemory() throws Exception {
    poolMemory.add(1000L);
    poolMemory.add(2000L);
    testCommonHelper(2000);
  }

  @Test
  public void testMixOfPoolLess_Greater_EqualMaxNodeMemory() throws Exception {
    poolMemory.add(1000L);
    poolMemory.add(2000L);
    poolMemory.add(1500L);
    testCommonHelper(1500);
  }
}
