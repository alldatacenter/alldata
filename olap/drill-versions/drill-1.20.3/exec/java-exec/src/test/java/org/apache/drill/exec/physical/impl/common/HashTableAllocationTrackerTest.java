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
package org.apache.drill.exec.physical.impl.common;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;

import static org.apache.drill.exec.physical.impl.common.HashTable.BATCH_SIZE;

public class HashTableAllocationTrackerTest extends BaseTest {
  @Test
  public void testDoubleGetNextCall() {
    final HashTableConfig config = new HashTableConfig(100, true, .5f, Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList());
    final HashTableAllocationTracker tracker = new HashTableAllocationTracker(config);

    for (int counter = 0; counter < 100; counter++) {
      Assert.assertEquals(100, tracker.getNextBatchHolderSize(BATCH_SIZE));
    }
  }

  @Test(expected = IllegalStateException.class)
  public void testPrematureCommit() {
    final HashTableConfig config = new HashTableConfig(100, .5f, Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList());
    final HashTableAllocationTracker tracker = new HashTableAllocationTracker(config);

    tracker.commit(30);
  }

  @Test(expected = IllegalStateException.class)
  public void testDoubleCommit() {
    final HashTableConfig config = new HashTableConfig(100, .5f, Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList());
    final HashTableAllocationTracker tracker = new HashTableAllocationTracker(config);

    tracker.commit(30);
    tracker.commit(30);
  }

  @Test
  public void testOverAsking() {
    final HashTableConfig config = new HashTableConfig(100, .5f, Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList());
    final HashTableAllocationTracker tracker = new HashTableAllocationTracker(config);

    tracker.getNextBatchHolderSize(30);
  }

  /**
   * Test for when we do not know the final size of the hash table.
   */
  @Test
  public void testLifecycle1() {
    final HashTableConfig config = new HashTableConfig(100, .5f, Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList());
    final HashTableAllocationTracker tracker = new HashTableAllocationTracker(config);

    for (int counter = 0; counter < 100; counter++) {
      Assert.assertEquals(30, tracker.getNextBatchHolderSize(30));
      tracker.commit(30);
    }
  }

  /**
   * Test for when we know the final size of the hash table
   */
  @Test
  public void testLifecycle() {
    final HashTableConfig config = new HashTableConfig(100, true, .5f, Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList());
    final HashTableAllocationTracker tracker = new HashTableAllocationTracker(config);

    Assert.assertEquals(30, tracker.getNextBatchHolderSize(30));
    tracker.commit(30);
    Assert.assertEquals(30, tracker.getNextBatchHolderSize(30));
    tracker.commit(30);
    Assert.assertEquals(30, tracker.getNextBatchHolderSize(30));
    tracker.commit(30);
    Assert.assertEquals(10, tracker.getNextBatchHolderSize(30));
    tracker.commit(30);

    boolean caughtException = false;

    try {
      tracker.getNextBatchHolderSize(BATCH_SIZE);
    } catch (IllegalStateException ex) {
      caughtException = true;
    }

    Assert.assertTrue(caughtException);
  }
}
