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
import org.apache.drill.exec.resourcemgr.config.ResourcePool;
import org.apache.drill.exec.resourcemgr.config.exception.QueueSelectionException;
import org.apache.drill.test.BaseTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(ResourceManagerTest.class)
public final class TestDefaultSelectionPolicy extends BaseTest {

  private static QueueSelectionPolicy selectionPolicy;

  private static final QueryContext queryContext = mock(QueryContext.class);

  @BeforeClass
  public static void testSetup() {
    selectionPolicy = QueueSelectionPolicyFactory.createSelectionPolicy(
      QueueSelectionPolicy.SelectionPolicy.DEFAULT);
    when(queryContext.getQueryId()).thenReturn(UserBitShared.QueryId.getDefaultInstance());
  }

  @Test(expected = QueueSelectionException.class)
  public void testWithNoDefaultPool() throws Exception {
    List<ResourcePool> inputPools = new ArrayList<>();
    final ResourcePool testPool1 = mock(ResourcePool.class);
    when(testPool1.isDefaultPool()).thenReturn(false);
    final ResourcePool testPool2 = mock(ResourcePool.class);
    when(testPool2.isDefaultPool()).thenReturn(false);

    inputPools.add(testPool1);
    inputPools.add(testPool2);
    selectionPolicy.selectQueue(inputPools, queryContext, null);
  }

  @Test
  public void testWithSingleDefaultPool() throws Exception {
    List<ResourcePool> inputPools = new ArrayList<>();
    final ResourcePool testPool1 = mock(ResourcePool.class);
    when(testPool1.isDefaultPool()).thenReturn(true);
    inputPools.add(testPool1);

    final ResourcePool selectedPool = selectionPolicy.selectQueue(inputPools, queryContext, null);
    assertEquals("Selected Pool and expected pool is different",testPool1, selectedPool);
  }

  @Test
  public void testWithMultipleDefaultPool() throws Exception {
    List<ResourcePool> inputPools = new ArrayList<>();
    final ResourcePool testPool1 = mock(ResourcePool.class);
    when(testPool1.isDefaultPool()).thenReturn(true);
    final ResourcePool testPool2 = mock(ResourcePool.class);
    when(testPool2.isDefaultPool()).thenReturn(true);

    inputPools.add(testPool1);
    inputPools.add(testPool2);

    final ResourcePool selectedPool = selectionPolicy.selectQueue(inputPools, queryContext, null);
    assertEquals("Selected Pool and expected pool is different",testPool1, selectedPool);
  }

  @Test
  public void testMixOfDefaultAndNonDefaultPool() throws Exception {
    List<ResourcePool> inputPools = new ArrayList<>();
    final ResourcePool testPool1 = mock(ResourcePool.class);
    when(testPool1.isDefaultPool()).thenReturn(false);
    final ResourcePool testPool2 = mock(ResourcePool.class);
    when(testPool2.isDefaultPool()).thenReturn(true);

    inputPools.add(testPool1);
    inputPools.add(testPool2);

    final ResourcePool selectedPool = selectionPolicy.selectQueue(inputPools, queryContext, null);
    assertEquals("Selected Pool and expected pool is different",testPool2, selectedPool);
  }
}
