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
package org.apache.ambari.server.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;

public class ManagedThreadPoolExecutorTest {

  @Test
  public void isStoppedAfterCreation() {
    ManagedThreadPoolExecutor executor = createExecutor(1);
    executor.execute(Assert::fail);
    assertFalse(executor.isRunning());
  }

  @Test
  public void canBeStartedAndStopped() {
    ManagedThreadPoolExecutor executor = createExecutor(1);
    executor.submit(() -> Boolean.TRUE);

    executor.start();
    assertTrue(executor.isRunning());
    executor.stop();
    assertFalse(executor.isRunning());
  }

  @Test
  public void retainsTasksUntilStarted() {
    final int taskCount = 60;
    final AtomicInteger counter = new AtomicInteger();
    ManagedThreadPoolExecutor executor = createExecutor(10);

    for (int i = 0; i < taskCount; ++i) {
      executor.execute(counter::incrementAndGet);
    }

    executor.start();
    Awaitility.await().atMost(2, TimeUnit.SECONDS)
      .until(() -> counter.get() == taskCount);
  }

  private static ManagedThreadPoolExecutor createExecutor(int poolSize) {
    return new ManagedThreadPoolExecutor(poolSize, poolSize,
      0L, TimeUnit.MILLISECONDS,
      new LinkedBlockingQueue<>());
  }
}
