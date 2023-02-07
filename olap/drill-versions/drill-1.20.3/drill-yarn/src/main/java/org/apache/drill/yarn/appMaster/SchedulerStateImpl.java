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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.drill.yarn.core.DoYUtil;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerId;

/**
 * Manages a the set of tasks associated with a scheduler. The scheduler decides
 * which tasks to run or stop; the task group manages the life-cycle of the
 * tasks for the given scheduler.
 * <p>
 * Schedulers, and hence their groups, define a priority. When starting, higher
 * priority (lower priority value) groups run before lower priority groups.
 * Similarly, when shrinking the cluster, lower priority groups shrink before
 * higher priority groups.
 */

public final class SchedulerStateImpl
    implements SchedulerState, SchedulerStateActions {
  static final Log LOG = LogFactory.getLog(SchedulerStateImpl.class);

  private final Scheduler scheduler;

  private final ClusterControllerImpl controller;

  /**
   * Tracks the tasks to be started, but for which no work has yet been done.
   * (State == PENDING).
   */

  protected List<Task> pendingTasks = new LinkedList<>();

  /**
   * Tracks the tasks for which containers have been requested. (State ==
   * REQUESTED).
   */

  protected List<Task> allocatingTasks = new LinkedList<>();

  /**
   * Tracks running tasks: those that have been allocated containers and are
   * starting, running, failed or ended. We use a map for this because, during
   * these states, the task is identified by its container. (State == LAUNCHING,
   * RUNNING or ENDING).
   */

  protected Map<ContainerId, Task> activeContainers = new HashMap<>();

  public SchedulerStateImpl(ClusterControllerImpl controller,
      Scheduler scheduler) {
    this.controller = controller;
    this.scheduler = scheduler;
    scheduler.registerState(this);
  }

  @Override
  public String getName() {
    return scheduler.getName();
  }

  public int getMaxRetries() {
    return controller.getMaxRetries();
  }

  public int getStopTimeoutMs() {
    return controller.getStopTimeoutMs();
  }

  @Override
  public Scheduler getScheduler() { return scheduler; }

  /**
   * Define a new task in this group. Adds it to the pending queue so that a
   * container will be requested.
   *
   * @param task
   */

  @Override
  public void start(Task task) {
    assert task.getGroup() == null;
    task.setGroup(this);
    enqueuePendingRequest(task);
  }

  /**
   * Put a task into the queue waiting to send a container request to YARN.
   *
   * @param task
   */

  public void enqueuePendingRequest(Task task) {
    assert !activeContainers.containsValue(task);
    assert !allocatingTasks.contains(task);
    assert !pendingTasks.contains(task);
    pendingTasks.add(task);

    // Special initial-state notification

    EventContext context = new EventContext(controller, task);
    controller.fireLifecycleChange(TaskLifecycleListener.Event.CREATED,
        context);
  }

  public int maxCurrentRequests() {
    return this.scheduler.getTaskManager().maxConcurrentAllocs();
  }

  @Override
  public boolean requestContainers(EventContext context, int maxRequests) {
    if (pendingTasks.isEmpty()) {
      return false;
    }

    // Limit the maximum number of requests to the limit set by
    // the scheduler.

    maxRequests = Math.min(maxRequests, maxCurrentRequests());

    // Further limit requests to account for in-flight requests.

    maxRequests -= allocatingTasks.size( );

    // Request containers as long as there are pending tasks remaining.

    for (int i = 0; i < maxRequests && !pendingTasks.isEmpty(); i++) {
      context.setTask(pendingTasks.get(0));
      context.getState().requestContainer(context);
    }
    return true;
  }

  /**
   * Remove a task from the queue of tasks waiting to send a container request.
   * The caller must put the task into the proper next state: the allocating
   * queue or the completed task list.
   *
   * @param task
   */

  public void dequeuePendingRequest(Task task) {
    assert !activeContainers.containsValue(task);
    assert !allocatingTasks.contains(task);
    assert pendingTasks.contains(task);
    pendingTasks.remove(task);
  }

  /**
   * Put a task onto the queue awaiting an allocation response from YARN.
   *
   * @param task
   */

  public void enqueueAllocatingTask(Task task) {
    assert !activeContainers.containsValue(task);
    assert !allocatingTasks.contains(task);
    assert !pendingTasks.contains(task);
    allocatingTasks.add(task);
  }

  @Override
  public void containerAllocated(EventContext context, Container container) {
    if (activeContainers.containsKey(container.getId())) {
      LOG.error("Container allocated again: " + DoYUtil.labelContainer(container));
      return;
    }
    if (allocatingTasks.isEmpty()) {

      // Not sure why this happens. Maybe only in debug mode
      // due stopping execution one thread while the RM
      // heartbeat keeps sending our request over & over?
      // One known case: the user requests a container. While YARN is
      // considering the request, the user cancels the task.

      LOG.warn("Releasing unwanted container: " + DoYUtil.labelContainer(container) );
      context.yarn.releaseContainer(container);
      return;
    }
    context.setTask(allocatingTasks.get(0));
    context.getState().containerAllocated(context, container);
  }

  @Override
  public void checkTasks(EventContext context, long curTime) {

    // Iterate over tasks using a temporary list. The tick event may cause a timeout
    // that turns around and modifies these lists.

    List<Task> temp = new ArrayList<>( );
    temp.addAll( allocatingTasks );
    for (Task task : temp) {
      context.setTask(task);
      context.getState().tick(context, curTime);
    }
    temp.clear();
    temp.addAll( pendingTasks );
    for (Task task : temp) {
      context.setTask(task);
      context.getState().tick(context, curTime);
    }
    temp.clear();
    temp.addAll( activeContainers.values( ) );
    for (Task task : temp) {
      context.setTask(task);
      context.getState().tick(context, curTime);
    }
  }

  /**
   * Remove a task from the list of those waiting for a container allocation.
   * The allocation may be done, or cancelled. The caller is responsible for
   * moving the task to the next collection.
   *
   * @param task
   */

  public void dequeueAllocatingTask(Task task) {
    assert allocatingTasks.contains(task);
    allocatingTasks.remove(task);
  }

  /**
   * Mark that a task has become active and should be tracked by its container
   * ID. Prior to this, the task is not associated with a container.
   *
   * @param task
   */

  public void containerAllocated(Task task) {
    assert !activeContainers.containsValue(task);
    assert !allocatingTasks.contains(task);
    assert !pendingTasks.contains(task);
    activeContainers.put(task.getContainerId(), task);
    controller.containerAllocated(task);
  }

  /**
   * Mark that a task has completed: its container has expired or been revoked
   * or the task has completed: successfully or a failure, as given by the
   * task's disposition. The task can no longer be tracked by its container ID.
   * If this is the last active task for this group, mark the group itself as
   * completed.
   *
   * @param task
   */

  public void containerReleased(Task task) {
    assert activeContainers.containsKey(task.getContainerId());
    activeContainers.remove(task.getContainerId());
    controller.containerReleased(task);
  }

  /**
   * Mark that a task has completed successfully or a failure, as given by the
   * task's disposition. If this is the last active task for this group, mark
   * the group itself as completed.
   *
   * @param task
   */

  public void taskEnded(Task task) {
    scheduler.completed(task);
    controller.taskEnded(task);
    if (isDone()) {
      controller.taskGroupCompleted(this);
    }
    LOG.info(task.toString() + " - Task completed" );
  }

  /**
   * Mark that a task is about to be retried. Task still retains its state from
   * the current try.
   *
   * @param task
   */

  public void taskRetried(Task task) {
    controller.taskRetried(task);
  }

  @Override
  public void shutDown(EventContext context) {
    for (Task task : getStartingTasks()) {
      context.setTask(task);
      context.getState().cancel(context);
    }
    for (Task task : getActiveTasks()) {
      context.setTask(task);
      context.getState().cancel(context);
    }
  }

  /**
   * Report if this task group has any tasks in the active part of their
   * life-cycle: pending, allocating or active.
   *
   * @return
   */

  public boolean hasTasks() {
    return getTaskCount() != 0;
  }

  @Override
  public boolean isDone() {
    return !hasTasks() && !scheduler.hasMoreTasks();
  }

  @Override
  public void adjustTasks() {
    scheduler.adjust();
  }

  /**
   * Request a graceful stop of the task. Delegates to the task manager to do
   * the actual work.
   *
   * @return true if the graceful stop request was sent, false if not, or if
   *         this task type has no graceful stop
   */

  public boolean requestStop(Task task) {
    return scheduler.getTaskManager().stop(task);
  }

  @Override
  public int getTaskCount() {
    return pendingTasks.size() + allocatingTasks.size()
        + activeContainers.size();
  }

  @Override
  public int getCancelledTaskCount() {

    // TODO Crude first cut. This value should be maintained
    // as a count.

    int count = 0;
    for (Task task : pendingTasks) {
      if (task.isCancelled()) {
        count++;
      }
    }
    for (Task task : allocatingTasks) {
      if (task.isCancelled()) {
        count++;
      }
    }
    for (Task task : activeContainers.values()) {
      if (task.isCancelled()) {
        count++;
      }
    }
    return count;
  }

  @Override
  public List<Task> getStartingTasks() {
    List<Task> tasks = new ArrayList<>();
    for (Task task : pendingTasks) {
      if (!task.isCancelled()) {
        tasks.add(task);
      }
    }
    for (Task task : allocatingTasks) {
      if (!task.isCancelled()) {
        tasks.add(task);
      }
    }
    return tasks;
  }

  @Override
  public List<Task> getActiveTasks() {
    List<Task> tasks = new ArrayList<>();
    for (Task task : activeContainers.values()) {
      if (!task.isCancelled()) {
        tasks.add(task);
      }
    }
    return tasks;
  }

  @Override
  public void cancel(Task task) {
    EventContext context = new EventContext(controller, task);
    LOG.info( task.getLabel() + " Task cancelled" );
    context.getState().cancel(context);
  }

  @Override
  public int getLiveCount() {
    int count = 0;
    for (Task task : activeContainers.values()) {
      if (task.isLive()) {
        count++;
      }
    }
    return count;
  }

  @Override
  public void visitTaskModels(TaskVisitor visitor) {
    for (Task task : pendingTasks) {
      visitor.visit(task);
    }
    for (Task task : allocatingTasks) {
      visitor.visit(task);
    }
    for (Task task : activeContainers.values()) {
      visitor.visit(task);
    }
  }

  @Override
  public Task getTask(int id) {
    for (Task task : pendingTasks) {
      if (task.getId() == id) {
        return task;
      }
    }
    for (Task task : allocatingTasks) {
      if (task.getId() == id) {
        return task;
      }
    }
    for (Task task : activeContainers.values()) {
      if (task.getId() == id) {
        return task;
      }
    }
    return null;
  }

  @Override
  public int getRequestCount() {
    return allocatingTasks.size();
  }

  @Override
  public ClusterController getController( ) { return controller; }
}