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

package com.netease.arctic.flink.read.hybrid.assigner;

import com.netease.arctic.flink.read.FlinkSplitPlanner;
import com.netease.arctic.flink.read.hybrid.split.ArcticSplit;
import com.netease.arctic.flink.read.hybrid.split.ArcticSplitState;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SplitAssignerAwaitingTest extends ShuffleSplitAssignerTest {

  @Test
  public void testEmpty() {
    ShuffleSplitAssigner splitAssigner = instanceSplitAssigner(1);
    Split split = splitAssigner.getNext(0);
    Assert.assertNotNull(split);
    Assert.assertEquals(Split.Status.UNAVAILABLE, split.status());
  }

  @Test
  public void testStaticAssign() {
    ShuffleSplitAssigner splitAssigner = instanceSplitAssigner(1);
    List<ArcticSplit> splitList = FlinkSplitPlanner.planFullTable(testKeyedTable, new AtomicInteger());

    splitAssigner.onDiscoveredSplits(splitList);
    assertSnapshot(splitAssigner, 7);
    assertAllSplits(splitAssigner, 7);

    splitAssigner.onUnassignedSplits(splitList.subList(0, 6));
    assertSnapshot(splitAssigner, 6);
    assertAllSplits(splitAssigner, 6);
  }

  @Test
  public void testContinueAssign() {
    ShuffleSplitAssigner assigner = instanceSplitAssigner(1);
    assertGetNext(assigner, Split.Status.UNAVAILABLE);

    List<ArcticSplit> splitList = FlinkSplitPlanner.planFullTable(testKeyedTable, new AtomicInteger());
    List<ArcticSplit> splits1 = splitList.subList(0, 1);
    assertAvailableFuture(assigner, () -> assigner.onDiscoveredSplits(splits1));
    List<ArcticSplit> splits2 = splitList.subList(1, 2);
    assertAvailableFuture(assigner, () -> assigner.onUnassignedSplits(splits2));

    assigner.onDiscoveredSplits(splitList.subList(2, 4));
    assertSnapshot(assigner, 2);
    assertAllSplits(assigner, 2);
    assertSnapshot(assigner, 0);
  }

  private void assertAllSplits(ShuffleSplitAssigner splitAssigner, int splitCount) {
    for (int i = 0; i < splitCount + 2; i++) {
      if (i < splitCount) {
        assertGetNext(splitAssigner, Split.Status.AVAILABLE);
      } else {
        assertGetNext(splitAssigner, Split.Status.UNAVAILABLE);
      }
    }
  }

  private void assertAvailableFuture(
      ShuffleSplitAssigner assigner, Runnable addSplitsRunnable) {
    // register callback
    AtomicBoolean futureCompleted = new AtomicBoolean();
    CompletableFuture<Void> future = assigner.isAvailable();
    future.thenAccept(ignored -> futureCompleted.set(true));
    // calling isAvailable again should return the same object reference
    // note that thenAccept will return a new future.
    // we want to assert the same instance on the assigner returned future
    Assert.assertSame(future, assigner.isAvailable());

    // now add some splits
    addSplitsRunnable.run();
    Assert.assertEquals(true, futureCompleted.get());

    for (int i = 0; i < 1; ++i) {
      assertGetNext(assigner, Split.Status.AVAILABLE);
    }
    assertGetNext(assigner, Split.Status.UNAVAILABLE);
    assertSnapshot(assigner, 0);
  }

  private void assertGetNext(ShuffleSplitAssigner assigner, Split.Status expectedStatus) {
    Split result = assigner.getNext(0);
    Assert.assertEquals(expectedStatus, result.status());
    switch (expectedStatus) {
      case AVAILABLE:
        Assert.assertNotNull(result.split());
        break;
      case UNAVAILABLE:
        Assert.assertNull(result.split());
        break;
      default:
        Assert.fail("Unknown status: " + expectedStatus);
    }
  }

  private void assertSnapshot(ShuffleSplitAssigner assigner, int splitCount) {
    Collection<ArcticSplitState> stateBeforeGet = assigner.state();
    Assert.assertEquals(splitCount, stateBeforeGet.size());
  }
}
