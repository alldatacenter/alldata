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
package org.apache.ambari.server.controller.test;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.controller.utilities.BufferedThreadPoolExecutorCompletionService;
import org.apache.ambari.server.controller.utilities.ScalingThreadPoolExecutor;
import org.junit.Test;

import junit.framework.Assert;

public class BufferedThreadPoolExecutorCompletionServiceTest {

  private void longOp() throws InterruptedException {
    Thread.sleep(700);
    System.out.println("Completed " + Thread.currentThread());
  }

  /**
   * Tests that when unbounded queue provided to executor, only
   * {@link ThreadPoolExecutor#getCorePoolSize()} threads are launched
   *
   * @throws InterruptedException
   */
  @Test
  public void testOnlyCorePoolThreadsLaunchedForUnboundedQueue() throws InterruptedException {
    int CORE_POOL_SIZE = 2;
    int MAX_POOL_SIZE = 5;
    int TASKS_COUNT = 8;
    LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, 30000, TimeUnit.MILLISECONDS, queue);
    BufferedThreadPoolExecutorCompletionService<Runnable> service = new BufferedThreadPoolExecutorCompletionService<>(threadPoolExecutor);
    for (int tc = 0; tc < TASKS_COUNT; tc++) {
      service.submit(new Runnable() {
        @Override
        public void run() {
          try {
            longOp();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }, null);
    }

    // While waiting for tasks completion, check how many threads are being used
    Thread.sleep(500);
    Assert.assertEquals(CORE_POOL_SIZE, threadPoolExecutor.getActiveCount());
    for (int tc = 0; tc < TASKS_COUNT; tc++) {
      Future<Runnable> take = service.take();
      Assert.assertTrue(take.isDone());
      Assert.assertTrue("No more than CORE_POOL_SIZE threads should be launched", threadPoolExecutor.getActiveCount() <= CORE_POOL_SIZE);
    }
    threadPoolExecutor.shutdown();
  }

  /**
   * Tests that when load is more than core-pool-size and less than
   * max-pool-size, the number of threads scales up.
   *
   * @throws InterruptedException
   */
  @Test
  public void testLessThanMaxPoolSizeThreadsLaunched() throws InterruptedException {
    int CORE_POOL_SIZE = 2;
    int MAX_POOL_SIZE = 10;
    int TASKS_COUNT = 8;
    LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(CORE_POOL_SIZE);
    ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, 30000, TimeUnit.MILLISECONDS, queue);
    BufferedThreadPoolExecutorCompletionService<Runnable> service = new BufferedThreadPoolExecutorCompletionService<>(threadPoolExecutor);
    for (int tc = 0; tc < TASKS_COUNT; tc++) {
      service.submit(new Runnable() {
        @Override
        public void run() {
          try {
            longOp();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }, null);
    }

    // While waiting for tasks completion, check how many threads are being used
    Thread.sleep(500);
    Assert.assertEquals(TASKS_COUNT - CORE_POOL_SIZE, threadPoolExecutor.getActiveCount());
    for (int tc = 0; tc < TASKS_COUNT; tc++) {
      Future<Runnable> take = service.take();
      Assert.assertTrue(take.isDone());
      Assert.assertTrue("No more than TASKS_COUNT threads should be launched", threadPoolExecutor.getActiveCount() <= TASKS_COUNT);
    }
    threadPoolExecutor.shutdown();
  }

  /**
   * Tests that when load is more than max-pool-size, the number of threads
   * scales up.
   *
   * @throws InterruptedException
   */
  @Test
  public void testMaxPoolSizeThreadsLaunched() throws InterruptedException {
    int CORE_POOL_SIZE = 2;
    int MAX_POOL_SIZE = 10;
    int TASKS_COUNT = 24;
    LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(CORE_POOL_SIZE);
    ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, 30000, TimeUnit.MILLISECONDS, queue);
    BufferedThreadPoolExecutorCompletionService<Runnable> service = new BufferedThreadPoolExecutorCompletionService<>(threadPoolExecutor);
    for (int tc = 0; tc < TASKS_COUNT; tc++) {
      service.submit(new Runnable() {
        @Override
        public void run() {
          try {
            longOp();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }, null);
    }

    // While waiting for tasks completion, check how many threads are being used
    Thread.sleep(500);
    Assert.assertEquals(MAX_POOL_SIZE, threadPoolExecutor.getActiveCount());
    for (int tc = 0; tc < TASKS_COUNT; tc++) {
      Future<Runnable> take = service.take();
      Assert.assertTrue(take.isDone());
      Assert.assertTrue("No more than MAX_POOL_SIZE threads should be launched", threadPoolExecutor.getActiveCount() <= MAX_POOL_SIZE);
    }
    threadPoolExecutor.shutdown();
  }

  /**
   * Tests that when load is more than max-pool-size, the number of threads
   * scales up.
   *
   * @throws InterruptedException
   */
  @Test
  public void testScalingThreadPoolExecutor() throws InterruptedException {
    int CORE_POOL_SIZE = 2;
    int MAX_POOL_SIZE = 10;
    int TASKS_COUNT = 24;
    ThreadPoolExecutor threadPoolExecutor = new ScalingThreadPoolExecutor(CORE_POOL_SIZE,
        MAX_POOL_SIZE, 30000, TimeUnit.MILLISECONDS, CORE_POOL_SIZE);
    BufferedThreadPoolExecutorCompletionService<Runnable> service = new BufferedThreadPoolExecutorCompletionService<>(threadPoolExecutor);
    for (int tc = 0; tc < TASKS_COUNT; tc++) {
      service.submit(new Runnable() {
        @Override
        public void run() {
          try {
            longOp();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }, null);
    }

    // While waiting for tasks completion, check how many threads are being used
    Thread.sleep(500);
    Assert.assertEquals(MAX_POOL_SIZE, threadPoolExecutor.getActiveCount());
    for (int tc = 0; tc < TASKS_COUNT; tc++) {
      Future<Runnable> take = service.take();
      Assert.assertTrue(take.isDone());
      Assert.assertTrue("No more than MAX_POOL_SIZE threads should be launched", threadPoolExecutor.getActiveCount() <= MAX_POOL_SIZE);
    }
    threadPoolExecutor.shutdown();
  }
}
