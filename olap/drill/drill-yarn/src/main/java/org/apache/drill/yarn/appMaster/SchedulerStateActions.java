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

import org.apache.hadoop.yarn.api.records.Container;

/**
 * Represents the set of commands called by the cluster controller to manage the
 * state of tasks within a task group. Each task group is managed by a
 * scheduler.
 */

public interface SchedulerStateActions {
  /**
   * Returns the name of the scheduler associated with this task action group.
   *
   * @return The name of the scheduler associated with this task action group.
   */

  String getName();

  /**
   * Returns the scheduler associated with this task group.
   *
   * @return The scheduler associated with this task group.
   */

  Scheduler getScheduler();

  /**
   * Adjust the number of running tasks as needed to balance the number of
   * running tasks with the desired number. May result in no change it the
   * cluster is already in balance (or is in the process of achieving balance.)
   */

  void adjustTasks();

  /**
   * Request a container the first task that we wish to start.
   */

  boolean requestContainers(EventContext context, int maxRequests);

  /**
   * A container request has been granted. Match the container up with the first
   * task waiting for a container and launch the task.
   *
   * @param context
   * @param container
   */

  void containerAllocated(EventContext context, Container container);

  /**
   * Shut down this task group by canceling all tasks not already cancelled.
   *
   * @param context
   */

  void shutDown(EventContext context);

  /**
   * Determine if this task group is done. It is done when there are no active
   * tasks and the controller itself is shutting down. This latter check
   * differentiates the start state (when no tasks are active) from the end
   * state. The AM will not shut down until all task groups are done.
   *
   * @return True if this task group is done. False otherwise.
   */

  boolean isDone();

  int getTaskCount( );

  int getLiveCount();

  int getRequestCount( );

  void visitTaskModels( TaskVisitor visitor );

  void checkTasks(EventContext context, long curTime);

  void cancel(Task task);

  Task getTask(int id);
}
