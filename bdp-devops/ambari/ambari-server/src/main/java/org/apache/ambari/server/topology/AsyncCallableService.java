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

package org.apache.ambari.server.topology;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

/**
 * Callable service implementation for executing tasks asynchronously.
 * The service repeatedly tries to execute the provided task till it successfully completes, or the provided timeout
 * interval is exceeded.
 *
 * @param <T> the type returned by the task to be executed
 */
public class AsyncCallableService<T> implements Callable<T> {

  private static final Logger LOG = LoggerFactory.getLogger(AsyncCallableService.class);

  // task execution is done on a separate thread provided by this executor
  private final ScheduledExecutorService executorService;

  // the task to be executed
  private final Callable<T> task;
  private final String taskName;

  // the total time the allowed for the task to be executed (retries will be happen within this timeframe in
  // milliseconds)
  private final long timeout;

  // the delay between two consecutive execution trials in milliseconds
  private final long retryDelay;
  private final Consumer<Throwable> onError;

  public AsyncCallableService(Callable<T> task, long timeout, long retryDelay, String taskName, Consumer<Throwable> onError) {
    this(task, timeout, retryDelay, taskName, Executors.newScheduledThreadPool(1), onError);
  }

  public AsyncCallableService(Callable<T> task, long timeout, long retryDelay, String taskName, ScheduledExecutorService executorService, Consumer<Throwable> onError) {
    Preconditions.checkArgument(retryDelay > 0, "retryDelay should be positive");

    this.task = task;
    this.executorService = executorService;
    this.timeout = timeout;
    this.retryDelay = retryDelay;
    this.taskName = taskName;
    this.onError = onError;
  }

  @Override
  public T call() throws Exception {
    long startTime = System.currentTimeMillis();
    long timeLeft = timeout;
    Future<T> future = executorService.submit(task);
    LOG.info("Task {} execution started at {}", taskName, startTime);

    Throwable lastError = null;
    while (true) {
      try {
        LOG.debug("Task {} waiting for result at most {} ms", taskName, timeLeft);
        T taskResult = future.get(timeLeft, TimeUnit.MILLISECONDS);
        LOG.info("Task {} successfully completed with result: {}", taskName, taskResult);
        return taskResult;
      } catch (TimeoutException e) {
        LOG.debug("Task {} timeout", taskName);
        if (lastError == null) {
          lastError = e;
        }
        timeLeft = 0;
      } catch (ExecutionException e) {
        Throwable cause = Throwables.getRootCause(e);
        if (!(cause instanceof RetryTaskSilently)) {
          LOG.info(String.format("Task %s exception during execution", taskName), cause);
        }
        lastError = cause;
        timeLeft = timeout - (System.currentTimeMillis() - startTime) - retryDelay;
      }

      if (timeLeft <= 0) {
        attemptToCancel(future);
        LOG.warn("Task {} timeout exceeded, no more retries", taskName);
        onError.accept(lastError);
        return null;
      }

      LOG.debug("Task {} retrying execution in {} milliseconds", taskName, retryDelay);
      future = executorService.schedule(task, retryDelay, TimeUnit.MILLISECONDS);
    }
  }

  private void attemptToCancel(Future<?> future) {
    LOG.debug("Task {} timeout exceeded, cancelling", taskName);
    if (!future.isDone() && future.cancel(true)) {
      LOG.debug("Task {} cancelled", taskName);
    } else {
      LOG.debug("Task {} already done", taskName);
    }
  }

  public static class RetryTaskSilently extends RuntimeException {
    // marker, throw if the task needs to be retried
    public RetryTaskSilently() {
      super();
    }
    public RetryTaskSilently(String message) {
      super(message);
    }
  }

}
