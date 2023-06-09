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

import java.util.List;

/**
 * The cluster state for tasks managed by a scheduler. Abstracts away the
 * details of managing tasks, allowing the scheduler to work only with overall
 * number of tasks.
 */

public interface SchedulerState {
  /**
   * The number of tasks in any active (non-ended) lifecycle state.
   *
   * @return The number of tasks in any active (non-ended) lifecycle state.
   */

  int getTaskCount();

  /**
   * The number of active tasks that have been cancelled, but have not yet
   * ended.
   *
   * @returnThe number of active tasks that have been cancelled, but have not yet
   * ended.
   */

  int getCancelledTaskCount();

  /**
   * Returns the list of tasks awaiting a container request to be sent to YARN
   * or for which a container request has been sent to YARN, but no container
   * allocation has yet been received. Such tasks are simple to cancel. The list
   * does not contain any tasks in this state which have previously been
   * cancelled.
   *
   * @return The list of tasks awaiting a container request to be sent to YARN
   * or for which a container request has been sent to YARN, but no container
   * allocation has yet been received.
   */

  List<Task> getStartingTasks();

  /**
   * Returns the list of active tasks that have not yet been cancelled. Active
   * tasks are any task for which a container has been assigned, but has not yet
   * received a RM container completion event.
   *
   * @return The list of active tasks that have not yet been cancelled.
   */

  List<Task> getActiveTasks();

  /**
   * Start the given task.
   *
   * @param task
   */

  void start(Task task);

  void cancel(Task task);

  ClusterController getController();
}
