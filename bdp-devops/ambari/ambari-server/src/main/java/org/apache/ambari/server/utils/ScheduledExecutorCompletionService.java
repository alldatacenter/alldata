/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.utils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledExecutorCompletionService<V> extends ExecutorCompletionService<V> {
  private final ScheduledExecutorService scheduledExecutor;
  private final BlockingQueue<Future<V>> queue;

  private class QueueingFuture extends FutureTask<Void> {
    QueueingFuture(RunnableFuture<V> task) {
      super(task, null);
      this.task = task;
    }
    @Override
    protected void done() {
      queue.add(task);
    }
    private final Future<V> task;
  }

  public ScheduledExecutorCompletionService(ScheduledExecutorService scheduledExecutor, BlockingQueue<Future<V>> queue) {
    super(scheduledExecutor, queue);
    this.scheduledExecutor = scheduledExecutor;
    this.queue = queue;
  }

  public Future<V> schedule(Callable<V> task, long delay, TimeUnit unit) {
    if (task == null) throw new NullPointerException();
    RunnableFuture<V> f = newTaskFor(task);
    scheduledExecutor.schedule(new QueueingFuture(f), delay, unit);
    return f;
  }

  private RunnableFuture<V> newTaskFor(Callable<V> task) {
    return new FutureTask<V>(task);
  }
}
