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
package org.apache.drill.yarn.appMaster;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.drill.yarn.appMaster.Task.Disposition;
import org.apache.drill.yarn.core.DoYUtil;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerStatus;

/**
 * Represents the behaviors associated with each state in the lifecycle
 * of a task.
 * <p>
 * Startup process:
 * <dl>
 * <dt>START --> REQUESTING<dt>
 * <dd>New task sends a container request to YARN.</dd>
 * <dt>REQUESTING --> LAUNCHING<dt>
 * <dd>Container received from YARN, launching the tasks's process.</dd>
 * <dt>LAUNCHING --> RUNNING<dt>
 * <dd>Task launched and needs no start Ack.</dd>
 * <dt>LAUNCHING --> WAIT_START_ACK<dt>
 * <dd>Task launched and needs a start Ack.</dd>
 * <dt>WAIT_START_ACK --> RUNNING<dt>
 * <dd>Start Ack received.</dd>
 * </dl>
 * <p>
 * Shutdown process:
 * <dt>RUNNING --> WAIT_END_ACK | END<dt>
 * <dd>The resource manager reported task completion.</dd>
 * <dt>RUNNING --> ENDING<dt>
 * <dd>Request sent to the task for a graceful shutdown.</dd>
 * <dt>RUNNING --> KILLING<dt>
 * <dd>Request sent to the node manager to forcibly kill the task.</dd>
 * <dt>ENDING --> WAIT_END_ACK | END<dt>
 * <dd>The task gracefully exited as reported by the resource manager.</dd>
 * <dt>ENDING --> KILLING<dt>
 * <dd>The wait for graceful exit timed out, a forced kill message
 *     sent to the node manager.</dd>
 * <dt>KILLING --> WAIT_END_ACK | END<dt>
 * <dd>The task exited as reported by the resource manager.</dd>
 * <dt>END_ACK --> END<dt>
 * <dd>The end-ack is received or the wait timed out.</dd>
 * <dl>
 * <p>
 * This is a do-it-yourself enum. Java enums values are instances of a single
 * class. In this version, each enum value is the sole instance of a separate
 * class, allowing each state to have its own behavior.
 */

public abstract class TaskState {
  /**
   * Task that is newly created and needs a container allocated. No messages
   * have yet been sent to YARN for the task.
   */

  private static class StartState extends TaskState {
    protected StartState() { super(false, TaskLifecycleListener.Event.CREATED, true); }

    @Override
    public void requestContainer(EventContext context) {
      Task task = context.task;
      task.tryCount++;
      context.group.dequeuePendingRequest(task);
      if (task.cancelled) {
        taskStartFailed(context, Disposition.CANCELLED);
      } else {
        transition(context, REQUESTING);
        context.group.enqueueAllocatingTask(task);
        task.containerRequest = context.yarn
            .requestContainer(task.getContainerSpec());
      }
    }

    /**
     * Cancellation is trivial: just drop the task; no need to coordinate
     * with YARN.
     */

    @Override
    public void cancel(EventContext context) {
      Task task = context.task;
      assert !task.cancelled;
      context.group.dequeuePendingRequest(task);
      task.cancel();
      taskStartFailed(context, Disposition.CANCELLED);
    }
  }

  /**
   * Task for which a container request has been sent but not yet received.
   */

  private static class RequestingState extends TaskState {
    protected RequestingState() {
      super(false, TaskLifecycleListener.Event.CREATED, true);
    }

    /**
     * Handle REQUESING --> LAUNCHING. Indicates that we've asked YARN to start
     * the task on the allocated container.
     */

    @Override
    public void containerAllocated(EventContext context, Container container) {
      Task task = context.task;
      LOG.info(task.getLabel() + " - Received container: "
          + DoYUtil.describeContainer(container));
      context.group.dequeueAllocatingTask(task);

      // No matter what happens below, we don't want to ask for this
      // container again. The RM async API is a bit bizarre in this
      // regard: it will keep asking for container over and over until
      // we tell it to stop.

      context.yarn.removeContainerRequest(task.containerRequest);

      // The container is need both in the normal and in the cancellation
      // path, so set it here.

      task.container = container;
      if (task.cancelled) {
        context.yarn.releaseContainer(container);
        taskStartFailed(context, Disposition.CANCELLED);
        return;
      }
      task.error = null;
      task.completionStatus = null;
      transition(context, LAUNCHING);

      // The pool that manages this task wants to know that we have
      // a container. The task manager may want to do some task-
      // specific setup.

      context.group.containerAllocated(context.task);
      context.getTaskManager().allocated(context);

      // Go ahead and launch a task in the container using the launch
      // specification provided by the task group (pool).

      try {
        context.yarn.launchContainer(container, task.getLaunchSpec());
        task.launchTime = System.currentTimeMillis();
      } catch (YarnFacadeException e) {
        LOG.error("Container launch failed: " + task.getContainerId(), e);

        // This may not be the right response. RM may still think
        // we have the container if the above is a local failure.

        task.error = e;
        context.group.containerReleased(task);
        task.container = null;
        taskStartFailed(context, Disposition.LAUNCH_FAILED);
      }
    }

    /**
     * Cancel the container request. We must wait for the response from YARN to
     * do the actual cancellation. For now, just mark the task as cancelled.
     */

    @Override
    public void cancel(EventContext context) {
      Task task = context.task;
      context.task.cancel();
      LOG.info(task.getLabel() + " - Cancelled at user request");
      context.yarn.removeContainerRequest(task.containerRequest);
      context.group.dequeueAllocatingTask(task);
      task.disposition = Task.Disposition.CANCELLED;
      task.completionTime = System.currentTimeMillis();
      transition(context, END);
      context.group.taskEnded(context.task);
    }

    /**
     * The task is requesting a container. If the request takes too long,
     * cancel the request and shrink the target task count. This event
     * generally indicates that the user wants to run more tasks than
     * the cluster has capacity.
     */

    @Override
    public void tick(EventContext context, long curTime) {
      Task task = context.task;
      int timeoutSec = task.scheduler.getRequestTimeoutSec( );
      if (timeoutSec == 0) {
        return;
      }
      if (task.stateStartTime + timeoutSec * 1000 > curTime) {
        return;
      }
      LOG.info(task.getLabel() + " - Request timed out after + "
          + timeoutSec + " secs.");
      context.yarn.removeContainerRequest(task.containerRequest);
      context.group.dequeueAllocatingTask(task);
      task.disposition = Task.Disposition.LAUNCH_FAILED;
      task.completionTime = System.currentTimeMillis();
      transition(context, END);
      context.group.taskEnded(context.task);
      task.scheduler.requestTimedOut();
    }
  }

  /**
   * Task for which a container has been allocated and the task launch request
   * sent. Awaiting confirmation that the task is running.
   */

  private static class LaunchingState extends TaskState {
    protected LaunchingState() {
      super(true, TaskLifecycleListener.Event.ALLOCATED, true);
    }

    /**
     * Handle launch failure. Results in a LAUNCHING --> END transition or
     * restart.
     * <p>
     * This situation can occur, when debugging, if a timeout occurs after the
     * allocation message, such as when, sitting in the debugger on the
     * allocation event.
     */

    @Override
    public void launchFailed(EventContext context, Throwable t) {
      Task task = context.task;
      LOG.info(task.getLabel() + " - Container start failed");
      context.task.error = t;
      launchFailed(context);
    }

    /**
     * Handle LAUNCHING --> RUNNING/START_ACK. Indicates that YARN has confirmed
     * that the task is, indeed, running.
     */

    @Override
    public void containerStarted(EventContext context) {
      Task task = context.task;

      // If this task is tracked (that is, it is a Drillbit which
      // we monitor using ZK) then we have to decide if we've
      // seen the task in the tracker yet. If we have, then the
      // task is fully running. If we haven't, then we need to
      // wait for the start acknowledgement.

      if (task.trackingState == Task.TrackingState.NEW) {
        transition(context, WAIT_START_ACK);
      } else {
        transition(context, RUNNING);
      }
      task.error = null;

      // If someone came along and marked the task as cancelled,
      // we are now done waiting for YARN so we can immediately
      // turn around and kill the task. (Can't kill the task,
      // however, until YARN starts it, hence the need to wait
      // for YARN to start the task before killing it.)

      if (task.cancelled) {
        transition(context, KILLING);
        context.yarn.killContainer(task.getContainer());
      }
    }

    /**
     * Out-of-order start ACK, perhaps due to network latency. Handle by staying
     * in this state, but later jump directly<br>
     * LAUNCHING --> RUNNING
     */

    @Override
    public void startAck(EventContext context) {
      context.task.trackingState = Task.TrackingState.START_ACK;
    }

    @Override
    public void containerCompleted(EventContext context,
        ContainerStatus status) {
      // Seen on Mac when putting machine to sleep.
      // Handle by failing & retrying.
      completed(context, status);
      endOrAck(context);
    }

    @Override
    public void cancel(EventContext context) {
      context.task.cancel();
      context.yarn.killContainer(context.task.getContainer());
    }

    @Override
    public void tick(EventContext context, long curTime) {

      // If we are canceling the task, and YARN has not reported container
      // completion after some amount of time, just force failure.

      Task task = context.task;
      if (task.isCancelled()
          && task.cancellationTime + Task.MAX_CANCELLATION_TIME < curTime) {
        LOG.error(task.getLabel() + " - Launch timed out after "
            + Task.MAX_CANCELLATION_TIME / 1000 + " secs.");
        launchFailed(context);
      }
    }

    private void launchFailed(EventContext context) {
      Task task = context.task;
      task.completionTime = System.currentTimeMillis();

      // Not sure if releasing the container is needed...

      context.yarn.releaseContainer(task.container);
      context.group.containerReleased(task);
      task.container = null;
      taskStartFailed(context, Disposition.LAUNCH_FAILED);
    }
  }

  /**
   * Task has been launched, is tracked, but we've not yet received a start ack.
   */

  private static class WaitStartAckState extends TaskState {
    protected WaitStartAckState() {
      super(true, TaskLifecycleListener.Event.RUNNING, true);
    }

    @Override
    public void startAck(EventContext context) {
      context.task.trackingState = Task.TrackingState.START_ACK;
      transition(context, RUNNING);
    }

    @Override
    public void cancel(EventContext context) {
      RUNNING.cancel(context);
    }

    // @Override
    // public void containerStopped(EventContext context) {
    // transition(context, WAIT_COMPLETE );
    // }

    @Override
    public void containerCompleted(EventContext context,
        ContainerStatus status) {
      completed(context, status);
      taskTerminated(context);
    }

    // TODO: Timeout in this state.
  }

  /**
   * Task in the normal running state.
   */

  private static class RunningState extends TaskState {
    protected RunningState() {
      super(true, TaskLifecycleListener.Event.RUNNING, true);
    }

    /**
     * Normal task completion. Implements the RUNNING --> END transition.
     *
     * @param status
     */

    @Override
    public void containerCompleted(EventContext context,
        ContainerStatus status) {
      completed(context, status);
      endOrAck(context);
    }

    @Override
    public void cancel(EventContext context) {
      Task task = context.task;
      task.cancel();
      if (context.group.requestStop(task)) {
        transition(context, ENDING);
      } else {
        context.yarn.killContainer(task.container);
        transition(context, KILLING);
      }
    }

    /**
     * The task claims that it is complete, but we think it is running. Assume
     * that the task has started its own graceful shutdown (or the
     * equivalent).<br>
     * RUNNING --> ENDING
     */

    @Override
    public void completionAck(EventContext context) {
      context.task.trackingState = Task.TrackingState.END_ACK;
      transition(context, ENDING);
    }
  }

  /**
   * Task for which a termination request has been sent to the Drill-bit, but
   * confirmation has not yet been received from the Node Manager. (Not yet
   * supported in the Drill-bit.
   */

  public static class EndingState extends TaskState {
    protected EndingState() { super(true, TaskLifecycleListener.Event.RUNNING, false); }

    /*
     * Normal ENDING --> WAIT_COMPLETE transition, awaiting Resource Manager
     * confirmation.
     */

//    @Override
//    public void containerStopped(EventContext context) {
//      transition(context, WAIT_COMPLETE);
//    }

    /**
     * Normal ENDING --> WAIT_END_ACK | END transition.
     *
     * @param status
     */

    @Override
    public void containerCompleted(EventContext context,
        ContainerStatus status) {
      completed(context, status);
      endOrAck(context);
    }

    @Override
    public void cancel(EventContext context) {
      context.task.cancel();
    }

    /**
     * If the graceful stop process exceeds the maximum timeout, go ahead and
     * forcibly kill the process.
     */

    @Override
    public void tick(EventContext context, long curTime) {
      Task task = context.task;
      if (curTime - task.stateStartTime > task.taskGroup.getStopTimeoutMs()) {
        context.yarn.killContainer(task.container);
        transition(context, KILLING);
      }
    }

    @Override
    public void completionAck(EventContext context) {
      context.task.trackingState = Task.TrackingState.END_ACK;
    }
  }

  /**
   * Task for which a forced termination request has been sent to the Node
   * Manager, but a stop message has not yet been received.
   */

  public static class KillingState extends TaskState {
    protected KillingState() { super(true, TaskLifecycleListener.Event.RUNNING, false); }

    /*
     * Normal KILLING --> WAIT_COMPLETE transition, awaiting Resource Manager
     * confirmation.
     */

//    @Override
//    public void containerStopped(EventContext context) {
//      transition(context, WAIT_COMPLETE);
//    }

    /**
     * Normal KILLING --> WAIT_END_ACK | END transition.
     *
     * @param status
     */

    @Override
    public void containerCompleted(EventContext context,
        ContainerStatus status) {
      completed(context, status);
      endOrAck(context);
    }

    @Override
    public void cancel(EventContext context) {
      context.task.cancel();
    }

    @Override
    public void startAck(EventContext context) {
      // Better late than never... Happens during debugging sessions
      // when order of messages is scrambled.

      context.task.trackingState = Task.TrackingState.START_ACK;
    }

    @Override
    public void completionAck(EventContext context) {
      context.task.trackingState = Task.TrackingState.END_ACK;
    }

    @Override
    public void stopTaskFailed(EventContext context, Throwable t) {
      assert false;
      // What to do?
    }
  }

  /**
   * Task exited, but we are waiting for confirmation from Zookeeper that
   * the Drillbit registration has been removed. Required to associate
   * ZK registrations with Drillbits. Ensures that we don't try to
   * start a new Drillbit on a node until the previous Drillbit
   * completely shut down, including dropping out of ZK.
   */

  private static class WaitEndAckState extends TaskState {
    protected WaitEndAckState() {
      super(false, TaskLifecycleListener.Event.RUNNING, false);
    }

    @Override
    public void cancel(EventContext context) {
      context.task.cancel();
    }

    @Override
    public void completionAck(EventContext context) {
      context.task.trackingState = Task.TrackingState.END_ACK;
      taskTerminated(context);
    }

    /**
     * Periodically check if the process is still live. We are supposed to
     * receive events when the task becomes deregistered. But, we've seen
     * cases where the task hangs in this state forever. Try to resolve
     * the issue by polling periodically.
     */

    @Override
    public void tick(EventContext context, long curTime) {
      if(! context.getTaskManager().isLive(context)){
        taskTerminated(context);
      }
    }
  }

  /**
   * Task is completed or failed. The disposition field gives the details of the
   * completion type. The task is not active on YARN, but could be retried.
   */

  private static class EndState extends TaskState {
    protected EndState() {
      super(false, TaskLifecycleListener.Event.ENDED, false);
    }

    /*
     * Ignore out-of-order Node Manager completion notices.
     */

    // @Override
    // public void containerStopped(EventContext context) {
    // }

    @Override
    public void cancel(EventContext context) {
    }
  }

  private static final Log LOG = LogFactory.getLog(TaskState.class);

  public static final TaskState START = new StartState();
  public static final TaskState REQUESTING = new RequestingState();
  public static final TaskState LAUNCHING = new LaunchingState();
  public static final TaskState WAIT_START_ACK = new WaitStartAckState();
  public static final TaskState RUNNING = new RunningState();
  public static final TaskState ENDING = new EndingState();
  public static final TaskState KILLING = new KillingState();
  public static final TaskState WAIT_END_ACK = new WaitEndAckState();
  public static final TaskState END = new EndState();

  protected final boolean hasContainer;
  protected final TaskLifecycleListener.Event lifeCycleEvent;
  protected final String label;
  protected final boolean cancellable;

  public TaskState(boolean hasContainer, TaskLifecycleListener.Event lcEvent,
      boolean cancellable) {
    this.hasContainer = hasContainer;
    lifeCycleEvent = lcEvent;
    this.cancellable = cancellable;
    String name = toString();
    name = name.replace("State", "");
    name = name.replaceAll("([a-z]+)([A-Z])", "$1_$2");
    label = name.toUpperCase();
  }

  protected void endOrAck(EventContext context) {
    if (context.task.trackingState == Task.TrackingState.START_ACK) {
      transition(context, WAIT_END_ACK);
    } else {
      taskTerminated(context);
    }
  }

  public void requestContainer(EventContext context) {
    illegalState(context, "requestContainer");
  }

  /**
   * Resource Manager reports that the task has been allocated a container.
   *
   * @param context
   * @param container
   */

  public void containerAllocated(EventContext context, Container container) {
    illegalState(context, "containerAllocated");
  }

  /**
   * The launch of the container failed.
   *
   * @param context
   * @param t
   */

  public void launchFailed(EventContext context, Throwable t) {
    illegalState(context, "launchFailed");
  }

  /**
   * Node Manager reports that the task has started execution.
   *
   * @param context
   */

  public void containerStarted(EventContext context) {
    illegalState(context, "containerStarted");
  }

  /**
   * The monitoring plugin has detected that the task has confirmed that it is
   * fully started.
   */

  public void startAck(EventContext context) {
    illegalState(context, "startAck");
  }

  /**
   * The node manager request to stop a task failed.
   *
   * @param context
   * @param t
   */

  public void stopTaskFailed(EventContext context, Throwable t) {
    illegalState(context, "stopTaskFailed");
  }

  /**
   * The monitoring plugin has detected that the task has confirmed that it has
   * started shutdown.
   */

  public void completionAck(EventContext context) {
    illegalState(context, "completionAck");
  }

  /**
   * Node Manager reports that the task has stopped execution. We don't yet know
   * if this was a success or failure.
   *
   * @param context
   */

  public void containerStopped(EventContext context) {
    illegalState(context, "containerStopped");
  }

  /**
   * Resource Manager reports that the task has completed execution and provided
   * the completion status.
   *
   * @param context
   * @param status
   */

  public void containerCompleted(EventContext context, ContainerStatus status) {
    completed(context, status);
    illegalState(context, "containerCompleted");
  }

  /**
   * Cluster manager wishes to cancel this task.
   *
   * @param context
   */

  public void cancel(EventContext context) {
    illegalState(context, "cancel");
  }

  public void tick(EventContext context, long curTime) {
    // Ignore by default
  }

  /**
   * Implement a state transition, alerting any life cycle listeners and
   * updating the log file. Marks the start time of the new state in support of
   * states that implement a timeout.
   *
   * @param context
   * @param newState
   */

  protected void transition(EventContext context, TaskState newState) {
    TaskState oldState = context.task.state;
    LOG.info(context.task.getLabel() + " " + oldState.toString() + " --> "
        + newState.toString());
    context.task.state = newState;
    if (newState.lifeCycleEvent != oldState.lifeCycleEvent) {
      context.controller.fireLifecycleChange(newState.lifeCycleEvent, context);
    }
    context.task.stateStartTime = System.currentTimeMillis();
  }

  /**
   * Task failed when starting. No container has been allocated. The task
   * will go from:<br>
   * * --> END
   * <p>
   * If the run failed, and the task can be retried, it may
   * then move from<br>
   * END --> STARTING
   * @param context
   * @param disposition
   */

  protected void taskStartFailed(EventContext context,
      Disposition disposition) {

    // No container, so don't alert the task manager.

    assert context.task.container == null;

    context.getTaskManager().completed(context);
    taskEnded(context, disposition);
    retryTask(context);
  }

  /**
   * A running task terminated. It may have succeeded or failed,
   * this method will determine which.
   * <p>
   * Every task goes from:<br>
   * * --> END
   * <p>
   * If the run failed, and the task can be retried, it may
   * then move from<br>
   * END --> STARTING
   *
   * @param context
   */

  protected void taskTerminated(EventContext context) {
    Task task = context.task;

    // Give the task manager a peek at the completed task.
    // The task manager can override retry behavior. To
    // cancel a task that would otherwise be retried, call
    // cancel( ) on the task.

    context.getTaskManager().completed(context);
    context.group.containerReleased(task);
    assert task.completionStatus != null;
    if (task.completionStatus.getExitStatus() == 0) {
      taskEnded(context, Disposition.COMPLETED);
      context.group.taskEnded(context.task);
    } else {
      taskEnded(context, Disposition.RUN_FAILED);
      retryTask(context);
    }
  }

  /**
   * Implements the details of marking a task as ended. Note, this method
   * does not deregister the task with the scheduler state, we keep it
   * registered in case we decide to retry.
   *
   * @param context
   * @param disposition
   */

  private void taskEnded(EventContext context, Disposition disposition) {
    Task task = context.task;
    if (disposition == null) {
      assert task.disposition != null;
    } else {
      task.disposition = disposition;
    }
    task.completionTime = System.currentTimeMillis();
    transition(context, END);
  }

  /**
   * Retry a task. Requires that the task currently be in the END state to provide
   * clean state transitions. Will deregister the task if it cannot be retried
   * because the cluster is ending or the task has failed too many times.
   * Otherwise, starts the whole life cycle over again.
   *
   * @param context
   */

  private void retryTask(EventContext context) {
    Task task = context.task;
    assert task.state == END;
    if (!context.controller.isLive() || !task.retryable()) {
      context.group.taskEnded(task);
      return;
    }
    if (task.tryCount > task.taskGroup.getMaxRetries()) {
      LOG.error(task.getLabel() + " - Too many retries: " + task.tryCount);
      task.disposition = Disposition.TOO_MANY_RETRIES;
      context.group.taskEnded(task);
      return;
    }
    LOG.info(task.getLabel() + " - Retrying task, try " + task.tryCount);
    context.group.taskRetried(task);
    task.reset();
    transition(context, START);
    context.group.enqueuePendingRequest(task);
  }

  /**
   * An event is called in a state where it is not expected. Log it, ignore it
   * and hope it goes away.
   *
   * @param action
   */

  private void illegalState(EventContext context, String action) {
    // Intentionally assert: fails during debugging, soldiers on in production.

    assert false;
    LOG.error(context.task.getLabel() + " - Action " + action
        + " in wrong state: " + toString(),
        new IllegalStateException("Action in wrong state"));
  }

  protected void completed(EventContext context, ContainerStatus status) {
    Task task = context.task;
    String diag = status.getDiagnostics();
    LOG.trace(
        task.getLabel() + " Completed, exit status: " + status.getExitStatus()
            + (DoYUtil.isBlank(diag) ? "" : ": " + status.getDiagnostics()));
    task.completionStatus = status;
  }

  @Override
  public String toString() { return getClass().getSimpleName(); }

  public boolean hasContainer() { return hasContainer; }

  public String getLabel() { return label; }

  public boolean isCancellable() {
    return cancellable;
  }
}