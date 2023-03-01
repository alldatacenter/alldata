/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.server;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShuffleTaskInfoTest {

  @BeforeEach
  public void setup() {
    ShuffleServerMetrics.register();
  }

  @AfterEach
  public void tearDown() {
    ShuffleServerMetrics.clear();
  }

  @Test
  public void hugePartitionConcurrentTest() throws InterruptedException {
    ShuffleTaskInfo shuffleTaskInfo = new ShuffleTaskInfo("hugePartitionConcurrentTest_appId");

    // case1
    int n = 10;
    final CyclicBarrier barrier = new CyclicBarrier(n);
    final CountDownLatch countDownLatch = new CountDownLatch(n);
    ExecutorService executorService = Executors.newFixedThreadPool(n);
    IntStream.range(0, n).forEach(i -> executorService.submit(() -> {
      try {
        barrier.await();
        shuffleTaskInfo.markHugePartition(i, i);
      } catch (Exception e) {
        // ignore
      } finally {
        countDownLatch.countDown();
      }
    }));
    countDownLatch.await();
    assertEquals(1, ShuffleServerMetrics.counterTotalAppWithHugePartitionNum.get());
    assertEquals(1, ShuffleServerMetrics.gaugeAppWithHugePartitionNum.get());
    assertEquals(n, ShuffleServerMetrics.counterTotalHugePartitionNum.get());
    assertEquals(n, ShuffleServerMetrics.gaugeHugePartitionNum.get());

    // case2
    ShuffleServerMetrics.clear();
    ShuffleServerMetrics.register();
    barrier.reset();
    CountDownLatch latch = new CountDownLatch(n);
    ShuffleTaskInfo taskInfo = new ShuffleTaskInfo("hugePartitionConcurrentTest_appId");
    IntStream.range(0, n).forEach(i -> executorService.submit(() -> {
      try {
        barrier.await();
        taskInfo.markHugePartition(1, 1);
      } catch (Exception e) {
        // ignore
      } finally {
        latch.countDown();
      }
    }));
    latch.await();
    assertEquals(1, ShuffleServerMetrics.counterTotalAppWithHugePartitionNum.get());
    assertEquals(1, ShuffleServerMetrics.gaugeAppWithHugePartitionNum.get());
    assertEquals(1, ShuffleServerMetrics.counterTotalHugePartitionNum.get());
    assertEquals(1, ShuffleServerMetrics.gaugeHugePartitionNum.get());

    executorService.shutdownNow();
  }

  @Test
  public void hugePartitionTest() {
    ShuffleTaskInfo shuffleTaskInfo = new ShuffleTaskInfo("hugePartition_appId");

    // case1
    assertFalse(shuffleTaskInfo.hasHugePartition());
    assertEquals(0, shuffleTaskInfo.getHugePartitionSize());

    // case2
    shuffleTaskInfo.markHugePartition(1, 1);
    shuffleTaskInfo.markHugePartition(1, 2);
    shuffleTaskInfo.markHugePartition(2, 2);
    assertTrue(shuffleTaskInfo.hasHugePartition());
    assertEquals(3, shuffleTaskInfo.getHugePartitionSize());
    assertEquals(1, ShuffleServerMetrics.gaugeAppWithHugePartitionNum.get());
    assertEquals(1, ShuffleServerMetrics.counterTotalAppWithHugePartitionNum.get());
  }

  @Test
  public void partitionSizeSummaryTest() {
    ShuffleTaskInfo shuffleTaskInfo = new ShuffleTaskInfo("partitionSizeSummaryTest_appId");
    // case1
    long size = shuffleTaskInfo.getPartitionDataSize(0, 0);
    assertEquals(0, size);

    // case2
    shuffleTaskInfo.addPartitionDataSize(0, 0, 1000);
    size = shuffleTaskInfo.getPartitionDataSize(0, 0);
    assertEquals(1000, size);

    // case3
    shuffleTaskInfo.addPartitionDataSize(0, 0, 500);
    size = shuffleTaskInfo.getPartitionDataSize(0, 0);
    assertEquals(1500, size);

    assertEquals(
        1500,
        shuffleTaskInfo.getTotalDataSize()
    );
  }
}
