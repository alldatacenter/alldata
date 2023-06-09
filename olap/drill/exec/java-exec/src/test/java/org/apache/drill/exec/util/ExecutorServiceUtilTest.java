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
package org.apache.drill.exec.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.drill.exec.util.concurrent.ExecutorServiceUtil;
import org.apache.drill.test.DrillTest;
import org.junit.Test;

/** Tests for validating the Drill executor service utility class */
public final class ExecutorServiceUtilTest extends DrillTest {

  @Test
  public void testSuccessfulExecution() {
    final int numThreads = 2;
    final int numTasks = 20;
    ExecutorService service = Executors.newFixedThreadPool(numThreads);
    List<RequestContainer> requests = new ArrayList<>(numTasks);

    // Set the test parameters (using the default values)
    TestParams params = new TestParams();

    // Launch the tasks
    for (int idx = 0; idx < numTasks; idx++) {
      CallableTask task = new CallableTask(params);
      Future<TaskResult> future = ExecutorServiceUtil.submit(service, task);

      requests.add(new RequestContainer(future, task));
    }

    int numSuccess  = 0;

    // Wait for the tasks to finish
    for (int idx = 0; idx < numTasks; idx++) {
      RequestContainer request = requests.get(idx);

      try {
        TaskResult result = request.future.get();
        assertNotNull(result);

        if (result.isSuccess()) {
          ++numSuccess;
        }
      } catch (Exception e) {
        // NOOP
      }
    }

    assertEquals(numTasks, numSuccess);
  }

  @Test
  public void testFailedExecution() {
    final int numThreads = 2;
    final int numTasks = 20;
    ExecutorService service = Executors.newFixedThreadPool(numThreads);
    List<RequestContainer> requests = new ArrayList<>(numTasks);

    // Set the test parameters
    TestParams params        = new TestParams();
    params.generateException = true;

    // Launch the tasks
    for (int idx = 0; idx < numTasks; idx++) {
      CallableTask task = new CallableTask(params);
      Future<TaskResult> future = ExecutorServiceUtil.submit(service, task);

      requests.add(new RequestContainer(future, task));
    }

    int numSuccess = 0;
    int numFailures = 0;

    // Wait for the tasks to finish
    for (int idx = 0; idx < numTasks; idx++) {
      RequestContainer request = requests.get(idx);

      try {
        TaskResult result = request.future.get();
        assertNotNull(result);

        if (result.isSuccess()) {
          ++numSuccess;
        }
      } catch (Exception e) {
        assertTrue(request.task.result.isFailed());
        ++numFailures;
      }
    }

    assertEquals(0, numSuccess);
    assertEquals(numTasks, numFailures);
  }

  @Test
  public void testMixedExecution() {
    final int numThreads = 2;
    final int numTasks = 20;
    ExecutorService service = Executors.newFixedThreadPool(numThreads);
    List<RequestContainer> requests = new ArrayList<>(numTasks);

    // Set the test parameters
    TestParams successParams = new TestParams();
    TestParams failedParams = new TestParams();
    failedParams.generateException = true;

    int expNumFailedTasks = 0;
    int expNumSuccessTasks = 0;

    // Launch the tasks
    for (int idx = 0; idx < numTasks; idx++) {
      CallableTask task;

      if (idx % 2 == 0) {
        task = new CallableTask(successParams);
        ++expNumSuccessTasks;
      } else {
        task = new CallableTask(failedParams);
        ++expNumFailedTasks;
      }

      Future<TaskResult> future = ExecutorServiceUtil.submit(service, task);
      requests.add(new RequestContainer(future, task));
    }

    int numSuccess = 0;
    int numFailures = 0;

    // Wait for the tasks to finish
    for (int idx = 0; idx < numTasks; idx++) {
      RequestContainer request = requests.get(idx);

      try {
        TaskResult result = request.future.get();
        assertNotNull(result);

        if (result.isSuccess()) {
          ++numSuccess;
        }
      } catch (Exception e) {
        assertTrue(request.task.result.isFailed());
        ++numFailures;
      }
    }

    assertEquals(expNumSuccessTasks, numSuccess);
    assertEquals(expNumFailedTasks, numFailures);
  }

  @Test
  public void testCancelExecution() {
    final int numThreads = 2;
    ExecutorService service = Executors.newFixedThreadPool(numThreads);
    RequestContainer request;

    // Set the test parameters
    TestParams params = new TestParams();
    params.controller = new TaskExecutionController();

    // Launch the task
    CallableTask task = new CallableTask(params);
    Future<TaskResult> future = ExecutorServiceUtil.submit(service, task);
    request = new RequestContainer(future, task);

    // Allow the task to start
    params.controller.start();
    params.controller.hasStarted();

    // Allow the task to exit but with a delay so that we can test the blocking nature of "cancel"
    params.controller.delayMillisOnExit = 50;
    params.controller.exit();

    // Cancel the task
    boolean result = request.future.cancel(true);

    if (result) {
      // We were able to cancel this task; let's make sure that it is done now that the current thread is
      // unblocked
      assertTrue(task.result.isCancelled());

    } else {
      // Cancellation could't happen most probably because this thread got context switched for
      // for a long time (should be rare); let's make sure the task is done and successful
      assertTrue(task.result.isSuccess());
    }
  }



// ----------------------------------------------------------------------------
// Internal Classes
// ----------------------------------------------------------------------------

  @SuppressWarnings("unused")
  private static final class TaskResult {

    private enum ExecutionStatus {
      NOT_STARTED,
      RUNNING,
      SUCCEEDED,
      FAILED,
      CANCELLED
    }

    private ExecutionStatus status;

    TaskResult() {
      status = ExecutionStatus.NOT_STARTED;
    }

    private boolean isSuccess() {
      return status.equals(ExecutionStatus.SUCCEEDED);
    }

    private boolean isFailed() {
      return status.equals(ExecutionStatus.FAILED);
    }

    private boolean isCancelled() {
      return status.equals(ExecutionStatus.CANCELLED);
    }

    private boolean isFailedOrCancelled() {
      return status.equals(ExecutionStatus.CANCELLED)
        || status.equals(ExecutionStatus.FAILED);
    }
  }

  @SuppressWarnings("unused")
  private static final class TaskExecutionController {
    private volatile boolean canStart = false;
    private volatile boolean canExit = false;
    private volatile boolean started = false;
    private volatile boolean exited = false;
    private volatile int delayMillisOnExit = 0;
    private final Object monitor = new Object();

    private void canStart() {
      synchronized(monitor) {
        while (!canStart) {
          try {
            monitor.wait();
          } catch (InterruptedException ie) {
            // NOOP
          }
        }
        started = true;
        monitor.notify();
      }
    }

    private void canExit() {
      synchronized(monitor) {
        while (!canExit) {
          try {
            monitor.wait();
          } catch (InterruptedException ie) {
            // NOOP
          }
        }
      }

      // Wait requested delay time before exiting
      for (int i = 0; i < delayMillisOnExit; i++) {
        try {
          Thread.sleep(1); // sleep 1 ms
        } catch (InterruptedException ie) {
          // NOOP
        }
      }

      synchronized(monitor) {
        exited = true;
        monitor.notify();
      }
    }

    private void start() {
      synchronized(monitor) {
        canStart = true;
        monitor.notify();
      }
    }

    private void exit() {
      synchronized(monitor) {
        canExit = true;
        monitor.notify();
      }
    }

    private void hasStarted() {
      synchronized(monitor) {
        while (!started) {
          try {
            monitor.wait();
          } catch (InterruptedException ie) {
            // NOOP
          }
        }
      }
    }

    private void hasExited() {
      synchronized(monitor) {
        while (!exited) {
          try {
            monitor.wait();
          } catch (InterruptedException ie) {
            // NOOP
          }
        }
      }
    }

  }

  private static final class TestParams {
    private final int waitTimeMillis = 2;
    private boolean generateException = false;
    private TaskExecutionController controller = null;
  }

  private static final class CallableTask implements Callable<TaskResult> {
    private final TaskResult result = new TaskResult();
    private final TestParams params;

    private CallableTask(TestParams params) {
      this.params = params;
    }

    @Override
    public TaskResult call() throws Exception {

      beforeStart();

      result.status = TaskResult.ExecutionStatus.RUNNING;
      boolean interrupted = false;
      Exception exc = null;

      try {
        for (int i = 0; i < params.waitTimeMillis; i++) {
          try {
            Thread.sleep(1); // sleep 1 ms
          } catch (InterruptedException ie) {
            interrupted = true;
          }
        }

        if (params.generateException) {
          throw new RuntimeException("Test emulated exception..");
        }

      } catch (Exception e) {
        exc = e;
        throw e;

      } finally {
        beforeExit();

        if (interrupted) {
          result.status = TaskResult.ExecutionStatus.CANCELLED;
        } else if (exc != null) {
          result.status = TaskResult.ExecutionStatus.FAILED;
        } else {
          result.status = TaskResult.ExecutionStatus.SUCCEEDED;
        }
      }
      return result;
    }

    private void beforeStart() {
      if (params.controller != null) {
        params.controller.canStart();
      }
    }

    private void beforeExit() {
      if (params.controller != null) {
        params.controller.canExit();
      }
    }
  }

  private static final class RequestContainer {
    private final Future<TaskResult> future;
    private final CallableTask task;

    private RequestContainer(Future<TaskResult> future, CallableTask task) {
      this.future = future;
      this.task   = task;
    }
  }

}