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
package org.apache.drill.exec.store;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.drill.common.collections.Collectors;
import org.apache.drill.common.exceptions.UserException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.drill.shaded.guava.com.google.common.util.concurrent.MoreExecutors;
import org.apache.drill.shaded.guava.com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Allows parallel executions of tasks in a simplified way. Also maintains and
 * reports timings of task completion.
 * <p>
 * TODO: look at switching to fork join.
 *
 * @param <V>
 *          The time value that will be returned when the task is executed.
 */
public abstract class TimedCallable<V> implements Callable<V> {
  private static final Logger logger = LoggerFactory.getLogger(TimedCallable.class);

  private static long TIMEOUT_PER_RUNNABLE_IN_MSECS = 15000;

  private volatile long startTime = 0;
  private volatile long executionTime = -1;

  private static class FutureMapper<V> implements Function<Future<V>, V> {
    int count;
    Throwable throwable = null;

    private void setThrowable(Throwable t) {
      if (throwable == null) {
        throwable = t;
      } else {
        throwable.addSuppressed(t);
      }
    }

    @Override
    public V apply(Future<V> future) {
      Preconditions.checkState(future.isDone());
      if (!future.isCancelled()) {
        try {
          count++;
          return future.get();
        } catch (InterruptedException e) {
          // there is no wait as we are getting result from the completed/done future
          logger.error("Unexpected exception", e);
          throw UserException.internalError(e)
              .message("Unexpected exception")
              .build(logger);
        } catch (ExecutionException e) {
          setThrowable(e.getCause());
        }
      } else {
        setThrowable(new CancellationException());
      }
      return null;
    }
  }

  private static class Statistics<V> implements Consumer<TimedCallable<V>> {
    final long start = System.nanoTime();
    final Stopwatch watch = Stopwatch.createStarted();
    long totalExecution;
    long maxExecution;
    int count;
    int startedCount;
    private int doneCount;
    // measure thread creation times
    long earliestStart;
    long latestStart;
    long totalStart;

    @Override
    public void accept(TimedCallable<V> task) {
      count++;
      long threadStart = task.getStartTime(TimeUnit.NANOSECONDS) - start;
      if (threadStart >= 0) {
        startedCount++;
        earliestStart = Math.min(earliestStart, threadStart);
        latestStart = Math.max(latestStart, threadStart);
        totalStart += threadStart;
        long executionTime = task.getExecutionTime(TimeUnit.NANOSECONDS);
        if (executionTime != -1) {
          doneCount++;
          totalExecution += executionTime;
          maxExecution = Math.max(maxExecution, executionTime);
        } else {
          logger.info("Task {} started at {} did not finish", task, threadStart);
        }
      } else {
        logger.info("Task {} never commenced execution", task);
      }
    }

    Statistics<V> collect(final List<TimedCallable<V>> tasks) {
      totalExecution = maxExecution = 0;
      count = startedCount = doneCount = 0;
      earliestStart = Long.MAX_VALUE;
      latestStart = totalStart = 0;
      tasks.forEach(this);
      return this;
    }

    void log(final String activity, final Logger logger, int parallelism) {
      if (startedCount > 0) {
        logger.debug("{}: started {} out of {} using {} threads. (start time: min {} ms, avg {} ms, max {} ms).",
            activity, startedCount, count, parallelism,
            TimeUnit.NANOSECONDS.toMillis(earliestStart),
            TimeUnit.NANOSECONDS.toMillis(totalStart) / startedCount,
            TimeUnit.NANOSECONDS.toMillis(latestStart));
      } else {
        logger.debug("{}: started {} out of {} using {} threads.", activity, startedCount, count, parallelism);
      }

      if (doneCount > 0) {
        logger.debug("{}: completed {} out of {} using {} threads (execution time: total {} ms, avg {} ms, max {} ms).",
            activity, doneCount, count, parallelism, watch.elapsed(TimeUnit.MILLISECONDS),
            TimeUnit.NANOSECONDS.toMillis(totalExecution) / doneCount, TimeUnit.NANOSECONDS.toMillis(maxExecution));
      } else {
        logger.debug("{}: completed {} out of {} using {} threads", activity, doneCount, count, parallelism);
      }
    }
  }

  @Override
  public final V call() throws Exception {
    long start = System.nanoTime();
    startTime = start;
    try {
      logger.debug("Started execution of '{}' task at {} ms", this, TimeUnit.MILLISECONDS.convert(start, TimeUnit.NANOSECONDS));
      return runInner();
    } catch (InterruptedException e) {
      logger.warn("Task '{}' interrupted", this, e);
      throw e;
    } finally {
      long time = System.nanoTime() - start;
      if (logger.isWarnEnabled()) {
        long timeMillis = TimeUnit.MILLISECONDS.convert(time, TimeUnit.NANOSECONDS);
        if (timeMillis > TIMEOUT_PER_RUNNABLE_IN_MSECS) {
          logger.warn("Task '{}' execution time {} ms exceeds timeout {} ms.", this, timeMillis, TIMEOUT_PER_RUNNABLE_IN_MSECS);
        } else {
          logger.debug("Task '{}' execution time is {} ms", this, timeMillis);
        }
      }
      executionTime = time;
    }
  }

  protected abstract V runInner() throws Exception;

  private long getStartTime(TimeUnit unit) {
    return unit.convert(startTime, TimeUnit.NANOSECONDS);
  }

  private long getExecutionTime(TimeUnit unit) {
    return unit.convert(executionTime, TimeUnit.NANOSECONDS);
  }


  /**
   * Execute the list of runnables with the given parallelization.  At end, return values and report completion time
   * stats to provided logger. Each runnable is allowed a certain timeout. If the timeout exceeds, existing/pending
   * tasks will be cancelled and a {@link UserException} is thrown.
   * @param activity Name of activity for reporting in logger.
   * @param logger The logger to use to report results.
   * @param tasks List of callable that should be executed and timed.  If this list has one item, task will be
   *                  completed in-thread. Each callable must handle {@link InterruptedException}s.
   * @param parallelism  The number of threads that should be run to complete this task.
   * @return The list of outcome objects.
   * @throws IOException All exceptions are coerced to IOException since this was build for storage system tasks initially.
   */
  public static <V> List<V> run(final String activity, final Logger logger, final List<TimedCallable<V>> tasks, int parallelism) throws IOException {
    Preconditions.checkArgument(!Preconditions.checkNotNull(tasks).isEmpty(), "list of tasks is empty");
    Preconditions.checkArgument(parallelism > 0);
    parallelism = Math.min(parallelism, tasks.size());
    final ExecutorService threadPool = parallelism == 1 ? MoreExecutors.newDirectExecutorService()
        : Executors.newFixedThreadPool(parallelism, new ThreadFactoryBuilder().setNameFormat(activity + "-%d").build());
    final long timeout = TIMEOUT_PER_RUNNABLE_IN_MSECS * ((tasks.size() - 1)/parallelism + 1);
    final FutureMapper<V> futureMapper = new FutureMapper<>();
    final Statistics<V> statistics = logger.isDebugEnabled() ? new Statistics<>() : null;
    try {
      return Collectors.toList(threadPool.invokeAll(tasks, timeout, TimeUnit.MILLISECONDS), futureMapper);
    } catch (InterruptedException e) {
      final String errMsg = String.format("Interrupted while waiting for activity '%s' tasks to be done.", activity);
      logger.error(errMsg, e);
      throw UserException.resourceError(e)
          .message(errMsg)
          .build(logger);
    } catch (RejectedExecutionException e) {
      final String errMsg = String.format("Failure while submitting activity '%s' tasks for execution.", activity);
      logger.error(errMsg, e);
      throw UserException.internalError(e)
          .message(errMsg)
          .build(logger);
    } finally {
      List<Runnable> notStartedTasks = threadPool.shutdownNow();
      if (!notStartedTasks.isEmpty()) {
        logger.error("{} activity '{}' tasks never commenced execution.", notStartedTasks.size(), activity);
      }
      try {
        // Wait for 5s for currently running threads to terminate. Above call (threadPool.shutdownNow()) interrupts
        // any running threads. If the tasks are handling the interrupts properly they should be able to
        // wrap up and terminate. If not waiting for 5s here gives a chance to identify and log any potential
        // thread leaks.
        if (!threadPool.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
          logger.error("Detected run away tasks in activity '{}'.", activity);
        }
      } catch (final InterruptedException e) {
        logger.warn("Interrupted while waiting for pending threads in activity '{}' to terminate.", activity);
      }

      if (statistics != null) {
        statistics.collect(tasks).log(activity, logger, parallelism);
      }
      if (futureMapper.count != tasks.size()) {
        final String errMsg = String.format("Waited for %d ms, but only %d tasks for '%s' are complete." +
            " Total number of tasks %d, parallelism %d.", timeout, futureMapper.count, activity, tasks.size(), parallelism);
        logger.error(errMsg, futureMapper.throwable);
        throw UserException.resourceError(futureMapper.throwable)
            .message(errMsg)
            .build(logger);
      }
      if (futureMapper.throwable != null) {
        throw (futureMapper.throwable instanceof IOException) ?
            (IOException)futureMapper.throwable : new IOException(futureMapper.throwable);
      }
    }
  }
}
