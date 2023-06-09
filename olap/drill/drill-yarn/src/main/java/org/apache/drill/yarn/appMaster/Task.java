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

import java.util.HashMap;
import java.util.Map;

import org.apache.drill.yarn.core.ContainerRequestSpec;
import org.apache.drill.yarn.core.LaunchSpec;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.client.api.AMRMClient.ContainerRequest;

/**
 * AM-side state of individual containers. This class is mostly
 * a holder of state. Behavior is provided by the
 * {@link TaskState} subclasses.
 */

public class Task {
  /**
   * Tracking plugin state. A task can be untracked, or moves
   * though states<br>
   * NEW --> START_ACK --> END_ACK
   * <p>
   * Tracking state is separate from, but integrated with,
   * task state. This is because, due to latency, tracking
   * events may be slightly out of sync with YARN events.
   */

  public enum TrackingState
  {
    UNTRACKED( "N/A" ),
    NEW( "Waiting" ),
    START_ACK( "OK" ),
    END_ACK( "Deregistered" );

    private String displayName;

    private TrackingState( String displayName ) {
      this.displayName = displayName;
    }

    public String getDisplayName( ) { return displayName; }
  }

  public enum Disposition
  {
    CANCELLED, LAUNCH_FAILED, RUN_FAILED, COMPLETED, TOO_MANY_RETRIES, RETRIED
  }

  /**
   * Maximum amount of time to wait when canceling a job in the REQUESTING
   * state. YARN will happily wait forever for a resource, this setting allows
   * the user to request to cancel a task, give YARN a while to respond, then
   * forcibly cancel the job at timeout.
   */

  public static final long MAX_CANCELLATION_TIME = 10_000; // ms = 10s

  /**
   * Tasks receive a sequential internal task ID. Since all task
   * creation is single-threaded, no additional concurrency controls
   * are needed to protect this value.
   */

  private static volatile int taskCounter = 0;

  /**
   * Internal identifier for the task.
   */

  public final int taskId;


  public final Scheduler scheduler;

  /**
   * Identifies the type of container needed and the details of the task to run.
   */

  public TaskSpec taskSpec;

  /**
   * The scheduler group that manages this task.
   */

  public SchedulerStateImpl taskGroup;

  /**
   * Tracking state for an additional task tracker (such as using
   * ZooKeeper to track Drill-bits.)
   */

  protected TrackingState trackingState;

  /**
   * Tracks the container request between request and allocation. We must pass
   * the container request back to YARN to remove it once it is allocated.
   */

  public ContainerRequest containerRequest;

  /**
   * The YARN container assigned to this task. The container is set only during
   * the ALLOCATED, LAUNCHING, RUNNING and ENDING states.
   */

  public Container container;

  /**
   * Life-cycle state of this task.
   */

  protected TaskState state;

  /**
   * True if the application has requested that the resource request or
   * application run be cancelled. Cancelled tasks are not subject to retry.
   */

  protected boolean cancelled;

  /**
   * Disposition of a completed task: whether it was cancelled, succeeded or
   * failed.
   */

  public Disposition disposition;

  public Throwable error;

  public int tryCount;

  public ContainerStatus completionStatus;

  public long launchTime;
  public long stateStartTime;
  public long completionTime;

  long cancellationTime;

  public Map<String,Object> properties = new HashMap<>( );

  public Task(Scheduler scheduler, TaskSpec taskSpec) {
    taskId = ++taskCounter;
    this.scheduler = scheduler;
    this.taskSpec = taskSpec;
    state = TaskState.START;
    resetTrackingState();
  }

  /**
   * Special constructor to create a static copy of the current
   * task. The copy is placed in the completed tasks list.
   * @param task
   */

  private Task(Task task) {
    taskId = task.taskId;
    scheduler = task.scheduler;
    taskSpec = task.taskSpec;
    taskGroup = task.taskGroup;
    trackingState = task.trackingState;
    containerRequest = task.containerRequest;
    container = task.container;
    state = task.state;
    cancelled = task.cancelled;
    disposition = task.disposition;
    error = task.error;
    tryCount = task.tryCount;
    completionStatus = task.completionStatus;
    launchTime = task.launchTime;
    stateStartTime = task.stateStartTime;
    completionTime = task.completionTime;
    cancellationTime = task.cancellationTime;
    properties.putAll( task.properties );
  }

  public void resetTrackingState( ) {
    trackingState = scheduler.isTracked() ? TrackingState.NEW : TrackingState.UNTRACKED;
  }

  public int getId( ) { return taskId; }
  public ContainerRequestSpec getContainerSpec() { return taskSpec.containerSpec; }

  public LaunchSpec getLaunchSpec() { return taskSpec.launchSpec; }

  public TaskState getState() { return state; }

  public ContainerId getContainerId() {
    assert container != null;
    return container.getId();
  }

  public Container getContainer() {
    assert container != null;
    return container;
  }

  public int getTryCount() { return tryCount; }

  public boolean isFailed() {
    return disposition != null && disposition != Disposition.COMPLETED;
  }

  public Disposition getDisposition() { return disposition; }

  public SchedulerStateImpl getGroup() { return taskGroup; }

  public void setGroup(SchedulerStateImpl taskGroup) { this.taskGroup = taskGroup; }

  public boolean retryable() {
    return !cancelled && disposition != Disposition.COMPLETED;
  }

  public boolean isCancelled() { return cancelled; }

  /**
   * Reset the task state in preparation for a retry.
   * Note: state reset is done by the state class.
   */

  public void reset() {
    assert !cancelled;
    error = null;
    disposition = null;
    completionStatus = null;
    launchTime = 0;
    completionTime = 0;
    cancellationTime = 0;
    container = null;
    resetTrackingState();
  }

  public long uptime() {
    long endTime = completionTime;
    if (endTime == 0) {
      endTime = System.currentTimeMillis();
    }
    return endTime - launchTime;
  }

  public String getHostName() {
    if (container == null) {
      return null;
    }
    return container.getNodeId().getHost();
  }

  public TrackingState getTrackingState() {
    return trackingState;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("[id=")
       .append(taskId)
       .append(", type=");
    // Scheduler is unset in some unit tests.
    if (scheduler !=null ) {
       buf.append(scheduler.getName());
    }
    buf.append(", name=")
       .append(getName());
    if (container != null) {
      buf.append(", host=")
         .append(getHostName());
    }
    buf.append(", state=")
       .append(state.toString())
       .append("]");
    return buf.toString();
  }

  public boolean isLive() {
    return state == TaskState.RUNNING && !cancelled;
  }

  public void cancel() {
    cancelled = true;
    cancellationTime = System.currentTimeMillis();
  }

  public Task copy() {
    return new Task(this);
  }

  public String getName() {
    return taskSpec == null ? null : taskSpec.name;
  }

  /**
   * Label for this task displayed in log messages.
   *
   * @return
   */

  public String getLabel() {
    return toString( );
  }

  public void setTrackingState(TrackingState tState) {
    trackingState = tState;
  }
}