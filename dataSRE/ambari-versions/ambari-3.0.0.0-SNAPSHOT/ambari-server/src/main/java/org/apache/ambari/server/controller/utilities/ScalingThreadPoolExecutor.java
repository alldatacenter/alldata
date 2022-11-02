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
package org.apache.ambari.server.controller.utilities;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * An {@link ExecutorService} that executes each submitted task using one of
 * possibly several pooled threads. It also scales up the number of threads in
 * the pool if the number of submissions exceeds the core size of the pool. The
 * pool can scale up to the specified maximum pool size.
 *
 * If the number of submissions exceeds the sum of the core and maximum size of
 * the thread pool, the submissions are then handled by the provided
 * {@link RejectedExecutionHandler}.
 *
 * If the overflowing submissions need to be handled,
 * {@link BufferedThreadPoolExecutorCompletionService} can be used to buffer up
 * overflowing submissions for later submission as threads become available.
 *
 * @see BufferedThreadPoolExecutorCompletionService
 */
public class ScalingThreadPoolExecutor extends ThreadPoolExecutor {

  /**
   * Constructor.
   *
   * @param corePoolSize
   *          the number of threads which will be considered as "core". If core
   *          thread timeout is not enabled, then this will be the minimum
   *          number of threads, always. With core timeout enabled, then this
   *          will be the number of threads spun up to handle incoming work
   *          regardless of worker queue size.
   * @param maximumPoolSize
   *          the maximum number of threads which can be spawned when an
   *          {@link ExecutorService} encounters a failure inserting into the
   *          work queue. These threads are not spawned to handle entries into
   *          the work queue; they are only spawned if the queue fills and a
   *          {@link RejectedExecutionHandler} is invoked.
   * @param keepAliveTime
   *          the TTL for core threads
   * @param unit
   *          the time unit for core threads
   * @param workerQueueSize
   *          the size of the worker queue. The threads specified by
   *          {@link #getMaximumPoolSize()} will only be created if the worker
   *          queue is exhausted.
   */
  public ScalingThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
      TimeUnit unit, int workerQueueSize) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, new LinkedBlockingQueue<>(workerQueueSize));
  }
}
