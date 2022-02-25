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

import java.util.Queue;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * An {@link ExecutorCompletionService} which takes a {@link ThreadPoolExecutor}
 * and uses its thread pool to execute tasks - buffering any tasks that
 * overflow. Such buffered tasks are later re-submitted to the executor when
 * finished tasks are polled or taken.
 * <p/>
 * This class overrides the {@link ThreadPoolExecutor}'s
 * {@link RejectedExecutionHandler} to collect overflowing tasks. When
 * {@link #poll(long, TimeUnit)} is invoked, this class will attempt to
 * re-submit any overflow tasks before waiting the specified amount of time.
 * This will prevent blocking on an empty completion queue since the parent
 * {@link ExecutorCompletionService} doesn't have any idea that there are tasks
 * waiting to be resubmitted.
 * <p/>
 * The {@link ScalingThreadPoolExecutor} can be used in conjunction with this
 * class to provide an efficient buffered, scaling thread pool implementation.
 *
 * @param <V>
 */
public class BufferedThreadPoolExecutorCompletionService<V> extends ExecutorCompletionService<V> {

  private ThreadPoolExecutor executor;
  private Queue<Runnable> overflowQueue;

  public BufferedThreadPoolExecutorCompletionService(ThreadPoolExecutor executor) {
    super(executor);
    this.executor = executor;
    this.overflowQueue = new LinkedBlockingQueue<>();
    this.executor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
      /**
       * Once the ThreadPoolExecutor is at full capacity, it starts to reject
       * submissions which are queued for later submission.
       */
      @Override
      public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        overflowQueue.add(r);
      }
    });
  }

  @Override
  public Future<V> take() throws InterruptedException {
    Future<V> take = super.take();
    if (!executor.isTerminating() && !overflowQueue.isEmpty() && executor.getActiveCount() < executor.getMaximumPoolSize()) {
      Runnable overflow = overflowQueue.poll();
      if (overflow != null) {
        executor.execute(overflow);
      }
    }
    return take;
  }

  @Override
  public Future<V> poll() {
    Future<V> poll = super.poll();
    if (!executor.isTerminating() && !overflowQueue.isEmpty() && executor.getActiveCount() < executor.getMaximumPoolSize()) {
      Runnable overflow = overflowQueue.poll();
      if (overflow != null) {
        executor.execute(overflow);
      }
    }
    return poll;
  }

  /**
   * {@inheritDoc}
   * <p/>
   * The goal of this method is to prevent blocking if there are tasks waiting
   * in the overflow queue. In the event that the overflow queue was populated,
   * we should not blindly wait on the parent
   * {@link ExecutorCompletionService#poll(long, TimeUnit)} method. Instead, we
   * should ensure that we have submitted at least one of our own tasks for
   * completion.
   */
  @Override
  public Future<V> poll(long timeout, TimeUnit unit) throws InterruptedException {
    // first poll for anything that's already completed and do a short-circuit return
    Future<V> poll = super.poll();
    if( null != poll ) {
      // there's something to return; that's great, but let's also see if we can
      // submit anything in the overflow queue back to the completion service
      if (!executor.isTerminating() && !overflowQueue.isEmpty()
          && executor.getActiveCount() < executor.getMaximumPoolSize()) {
        Runnable overflow = overflowQueue.poll();
        if (overflow != null) {
          executor.execute(overflow);
        }
      }

      // return the future
      return poll;
    }

    // nothing completed yet, so check active thread count - if there is nothing
    // working either, then that means that the parent completion service thinks
    // it's done - we should submit our own tasks
    if (executor.getActiveCount() == 0) {
      if (!executor.isTerminating() && !overflowQueue.isEmpty()) {
        Runnable overflow = overflowQueue.poll();
        if (overflow != null) {
          executor.execute(overflow);
        }
      }
    }

    // now that we've confirmed that either the parent completion service is
    // still working or we submitted our own task, we can poll with a timeout
    poll = super.poll(timeout, unit);
    return poll;
  }
}
