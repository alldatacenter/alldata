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
package org.apache.drill.exec.physical.impl.partitionsender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.OperatorStats;
import org.apache.drill.exec.ops.QueryCancelledException;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.testing.ControlsInjector;
import org.apache.drill.exec.testing.ControlsInjectorFactory;
import org.apache.drill.exec.testing.CountDownLatchInjection;

import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decorator class to hide multiple Partitioner existence from the caller since
 * this class involves multithreaded processing of incoming batches as well as
 * flushing it needs special handling of OperatorStats - stats since stats are
 * not suitable for use in multithreaded environment The algorithm to figure out
 * processing versus wait time is based on following formula: totalWaitTime =
 * totalAllPartitionersProcessingTime - max(sum(processingTime) by partitioner)
 */
public final class PartitionerDecorator {
  private static final Logger logger = LoggerFactory.getLogger(PartitionerDecorator.class);
  private static final ControlsInjector injector = ControlsInjectorFactory.getInjector(PartitionerDecorator.class);

  private final List<Partitioner> partitioners;
  private final OperatorStats stats;
  private final ExecutorService executor;
  private final FragmentContext context;
  private final Thread thread;
  private final boolean enableParallelTaskExecution;

  PartitionerDecorator(List<Partitioner> partitioners, OperatorStats stats, FragmentContext context) {
    this(partitioners, stats, context, partitioners.size() > 1);
  }

  PartitionerDecorator(List<Partitioner> partitioners, OperatorStats stats, FragmentContext context, boolean enableParallelTaskExecution) {
    this.partitioners = partitioners;
    this.stats = stats;
    this.context = context;
    this.enableParallelTaskExecution = enableParallelTaskExecution;
    executor =  enableParallelTaskExecution ?  context.getExecutor() : MoreExecutors.newDirectExecutorService();
    thread = Thread.currentThread();
  }

  /**
   * partitionBatch - decorator method to call real Partitioner(s) to process incoming batch
   * uses either threading or not threading approach based on number Partitioners
   * @param incoming
   * @throws ExecutionException
   */
  public void partitionBatch(final RecordBatch incoming) throws ExecutionException {
    executeMethodLogic(new PartitionBatchHandlingClass(incoming));
  }

  /**
   * flushOutgoingBatches - decorator to call real Partitioner(s) flushOutgoingBatches
   * @param isLastBatch
   * @param schemaChanged
   * @throws ExecutionException
   */
  public void flushOutgoingBatches(final boolean isLastBatch, final boolean schemaChanged) throws ExecutionException {
    executeMethodLogic(new FlushBatchesHandlingClass(isLastBatch, schemaChanged));
  }

  /**
   * decorator method to call multiple Partitioners initialize()
   */
  public void initialize() {
    for (Partitioner part : partitioners ) {
      part.initialize();
    }
  }

  /**
   * decorator method to call multiple Partitioners clear()
   */
  public void clear() {
    for (Partitioner part : partitioners ) {
      part.clear();
    }
  }

  /**
   * Helper method to get PartitionOutgoingBatch based on the index
   * since we may have more then one Partitioner
   * As number of Partitioners should be very small AND this method it used very rarely,
   * so it is OK to loop in order to find right partitioner
   * @param index - index of PartitionOutgoingBatch
   * @return PartitionOutgoingBatch
   */
  public PartitionOutgoingBatch getOutgoingBatches(int index) {
    for (Partitioner part : partitioners ) {
      PartitionOutgoingBatch outBatch = part.getOutgoingBatch(index);
      if ( outBatch != null ) {
        return outBatch;
      }
    }
    return null;
  }

  List<Partitioner> getPartitioners() {
    return partitioners;
  }

  /**
   * Helper to execute the different methods wrapped into same logic
   * @param iface
   * @throws ExecutionException
   */
  @VisibleForTesting
  void executeMethodLogic(final GeneralExecuteIface iface) throws ExecutionException {
    // To simulate interruption of main fragment thread and interrupting the partition threads, create a
    // CountDownInject latch. Partitioner threads await on the latch and main fragment thread counts down or
    // interrupts waiting threads. This makes sure that we are actually interrupting the blocked partitioner threads.
    try (CountDownLatchInjection testCountDownLatch = injector.getLatch(context.getExecutionControls(), "partitioner-sender-latch")) {
      testCountDownLatch.initialize(1);
      final AtomicInteger count = new AtomicInteger();
      List<PartitionerTask> partitionerTasks = new ArrayList<>(partitioners.size());
      ExecutionException executionException = null;
      // start waiting on main stats to adjust by sum(max(processing)) at the end
      startWait();
      try {
        partitioners.forEach(partitioner -> createAndExecute(iface, testCountDownLatch, count, partitionerTasks, partitioner));
        // Wait for main fragment interruption.
        injector.injectInterruptiblePause(context.getExecutionControls(), "wait-for-fragment-interrupt", logger);
        testCountDownLatch.countDown();
      } catch (InterruptedException e) {
        logger.warn("fragment thread interrupted", e);
        throw new QueryCancelledException();
      } catch (RejectedExecutionException e) {
        logger.warn("Failed to execute partitioner tasks. Execution service down?", e);
        executionException = new ExecutionException(e);
      } finally {
        await(count, partitionerTasks);
        stopWait();
        processPartitionerTasks(partitionerTasks, executionException);
      }
    }
  }

  private void createAndExecute(GeneralExecuteIface iface, CountDownLatchInjection testCountDownLatch, AtomicInteger count,
      List<PartitionerTask> partitionerTasks, Partitioner partitioner) {
    PartitionerTask partitionerTask = new PartitionerTask(this, iface, partitioner, count, testCountDownLatch);
    executor.execute(partitionerTask);
    partitionerTasks.add(partitionerTask);
    count.incrementAndGet();
  }

  /**
   * Wait for completion of all partitioner tasks.
   * @param count current number of task not yet completed
   * @param partitionerTasks list of partitioner tasks submitted for execution
   */
  private void await(AtomicInteger count, List<PartitionerTask> partitionerTasks) {
    boolean cancelled = false;
    while (count.get() > 0) {
      if (context.getExecutorState().shouldContinue() || cancelled) {
        LockSupport.park();
      } else {
        logger.warn("Cancelling fragment {} partitioner tasks...", context.getFragIdString());
        partitionerTasks.forEach(partitionerTask -> partitionerTask.cancel(true));
        cancelled = true;
      }
    }
  }

  private void startWait() {
    if (enableParallelTaskExecution) {
      stats.startWait();
    }
  }

  private void stopWait() {
    if (enableParallelTaskExecution) {
      stats.stopWait();
    }
  }

  private void processPartitionerTasks(List<PartitionerTask> partitionerTasks, ExecutionException executionException) throws ExecutionException {
    long maxProcessTime = 0l;
    for (PartitionerTask partitionerTask : partitionerTasks) {
      ExecutionException e = partitionerTask.getException();
      if (e != null) {
        if (executionException == null) {
          executionException = e;
        } else {
          executionException.getCause().addSuppressed(e.getCause());
        }
      }
      if (executionException == null) {
        final OperatorStats localStats = partitionerTask.getStats();
        // find out max Partitioner processing time
        if (enableParallelTaskExecution) {
          long currentProcessingNanos = localStats.getProcessingNanos();
          maxProcessTime = (currentProcessingNanos > maxProcessTime) ? currentProcessingNanos : maxProcessTime;
        } else {
          maxProcessTime += localStats.getWaitNanos();
        }
        stats.mergeMetrics(localStats);
      }
    }
    if (executionException != null) {
      throw executionException;
    }
    // scale down main stats wait time based on calculated processing time
    // since we did not wait for whole duration of above execution
    if (enableParallelTaskExecution) {
      stats.adjustWaitNanos(-maxProcessTime);
    } else {
      stats.adjustWaitNanos(maxProcessTime);
    }
  }

  /**
   * Helper interface to generalize functionality executed in the thread
   * since it is absolutely the same for partitionBatch and flushOutgoingBatches
   * protected is for testing purposes
   */
  protected interface GeneralExecuteIface {
    void execute(Partitioner partitioner) throws IOException;
  }

  /**
   * Class to handle running partitionBatch method
   *
   */
  private static class PartitionBatchHandlingClass implements GeneralExecuteIface {

    private final RecordBatch incoming;

    PartitionBatchHandlingClass(RecordBatch incoming) {
      this.incoming = incoming;
    }

    @Override
    public void execute(Partitioner part) throws IOException {
      part.partitionBatch(incoming);
    }
  }

  /**
   * Class to handle running flushOutgoingBatches method
   *
   */
  private static class FlushBatchesHandlingClass implements GeneralExecuteIface {

    private final boolean isLastBatch;
    private final boolean schemaChanged;

    public FlushBatchesHandlingClass(boolean isLastBatch, boolean schemaChanged) {
      this.isLastBatch = isLastBatch;
      this.schemaChanged = schemaChanged;
    }

    @Override
    public void execute(Partitioner part) throws IOException {
      part.flushOutgoingBatches(isLastBatch, schemaChanged);
    }
  }

  /**
   * Helper class to wrap Runnable with cancellation and waiting for completion support
   *
   */
  private static class PartitionerTask implements Runnable {

    private enum STATE {
      NEW,
      COMPLETING,
      NORMAL,
      EXCEPTIONAL,
      CANCELLED,
      INTERRUPTING,
      INTERRUPTED
    }

    private final AtomicReference<STATE> state;
    private final AtomicReference<Thread> runner;
    private final PartitionerDecorator partitionerDecorator;
    private final AtomicInteger count;

    private final GeneralExecuteIface iface;
    private final Partitioner partitioner;
    private final CountDownLatchInjection testCountDownLatch;

    private volatile ExecutionException exception;

    public PartitionerTask(PartitionerDecorator partitionerDecorator, GeneralExecuteIface iface, Partitioner partitioner, AtomicInteger count, CountDownLatchInjection testCountDownLatch) {
      state = new AtomicReference<>(STATE.NEW);
      runner = new AtomicReference<>();
      this.partitionerDecorator = partitionerDecorator;
      this.iface = iface;
      this.partitioner = partitioner;
      this.count = count;
      this.testCountDownLatch = testCountDownLatch;
    }

    @Override
    public void run() {
      final Thread thread = Thread.currentThread();
      if (runner.compareAndSet(null, thread)) {
        final String name = thread.getName();
        thread.setName(String.format("Partitioner-%s-%d", partitionerDecorator.thread.getName(), thread.getId()));
        final OperatorStats localStats = partitioner.getStats();
        localStats.clear();
        localStats.startProcessing();
        ExecutionException executionException = null;
        try {
          // Test only - Pause until interrupted by fragment thread
          testCountDownLatch.await();
          if (state.get() == STATE.NEW) {
            iface.execute(partitioner);
          }
        } catch (InterruptedException e) {
          if (state.compareAndSet(STATE.NEW, STATE.INTERRUPTED)) {
            logger.warn("Partitioner Task interrupted during the run", e);
          }
        } catch (Throwable t) {
          executionException = new ExecutionException(t);
        } finally {
          if (state.compareAndSet(STATE.NEW, STATE.COMPLETING)) {
            if (executionException == null) {
              localStats.stopProcessing();
              state.lazySet(STATE.NORMAL);
            } else {
              exception = executionException;
              state.lazySet(STATE.EXCEPTIONAL);
            }
          }
          if (count.decrementAndGet() == 0) {
            LockSupport.unpark(partitionerDecorator.thread);
          }
          thread.setName(name);
          while (state.get() == STATE.INTERRUPTING) {
            Thread.yield();
          }
          // Clear interrupt flag
          Thread.interrupted();
        }
      }
    }

    void cancel(boolean mayInterruptIfRunning) {
      Preconditions.checkState(Thread.currentThread() == partitionerDecorator.thread,
          String.format("PartitionerTask can be cancelled only from the main %s thread", partitionerDecorator.thread.getName()));
      if (runner.compareAndSet(null, partitionerDecorator.thread)) {
        if (partitionerDecorator.executor instanceof ThreadPoolExecutor) {
          ((ThreadPoolExecutor)partitionerDecorator.executor).remove(this);
        }
        count.decrementAndGet();
      } else {
        if (mayInterruptIfRunning) {
          if (state.compareAndSet(STATE.NEW, STATE.INTERRUPTING)) {
            try {
              runner.get().interrupt();
            } finally {
              state.lazySet(STATE.INTERRUPTED);
            }
          }
        } else {
          state.compareAndSet(STATE.NEW, STATE.CANCELLED);
        }
      }
    }

    public ExecutionException getException() {
      return exception;
    }

    public OperatorStats getStats() {
      return partitioner.getStats();
    }
  }
}
