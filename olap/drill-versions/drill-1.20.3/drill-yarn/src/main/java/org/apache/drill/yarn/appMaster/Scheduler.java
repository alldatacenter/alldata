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

import org.apache.drill.yarn.core.ContainerRequestSpec;
import org.apache.drill.yarn.core.LaunchSpec;
import org.apache.hadoop.yarn.api.records.Resource;

/**
 * The scheduler describes the set of tasks to run. It provides the details
 * required to launch each task and optionally a specification of the containers
 * required to run the task.
 * <p>
 * Schedulers can manage batch task (which do their job and complete), or
 * persistent tasks (which run until terminated.)
 * <p>
 * The scheduler tracks task completion (for batch tasks) and task levels (for
 * persistent tasks.)
 */

public interface Scheduler {
  public interface TaskManager {
    int maxConcurrentAllocs();

    LaunchSpec getLaunchSpec(Task task);

    void allocated(EventContext context);

    boolean stop(Task task);

    void completed(EventContext context);

    boolean isLive(EventContext context);
  }

  /**
   * Controller-assigned priority for this scheduler. Used to differentiate
   * container requests by scheduler.
   *
   * @param priority
   */

  void setPriority(int priority);

  /**
   * Register the state object that tracks tasks launched by this scheduler.
   *
   * @param state
   */

  void registerState(SchedulerState state);

  String getName();

  String getType();

  /**
   * Whether tasks from this scheduler should incorporate app startup/shutdown
   * acknowledgements (acks) into the task lifecycle.
   */

  boolean isTracked();

  TaskManager getTaskManager();

  /**
   * Get the desired number of running tasks.
   *
   * @return The desired number of running tasks
   */
  int getTarget();

  /**
   * Increase (positive) or decrease (negative) the number of desired tasks by
   * the given amount.
   *
   * @param delta
   */
  void change(int delta);

  /**
   * Set the number of desired tasks to the given level.
   *
   * @param level
   * @return the actual resize level, which may be lower than the requested
   * level if the system cannot provide the requested level
   */

  int resize(int level);

  void completed(Task task);

  /**
   * Adjust the number of running tasks to better track the desired number.
   * Starts or stops tasks using the {@link SchedulerState} registered with
   * {@link #registerState(SchedulerState)}.
   */

  void adjust();

  /**
   * Return an estimate of progress given as a ratio of (work completed, total
   * work).
   *
   * @return Estimate of progress.
   */
  int[] getProgress();

  /**
   * If this is a batch scheduler, whether all tasks for the batch have
   * completed. If this is a persistent task scheduler, always returns false.
   *
   * @return true if the scheduler has more tasks to run, false if the
   * scheduler has no more tasks or manages a set of long-running tasks
   */
  boolean hasMoreTasks();

  /**
   * For reporting, get the YARN resources requested by processes in
   * this pool.
   * @return The request spec.
   */

  ContainerRequestSpec getResource( );

  void limitContainerSize(Resource maxResource) throws AMException;

  /**
   * Maximum amount of time to wait when cancelling a job in the REQUESTING
   * state. YARN will happily wait forever for a resource, this setting
   * forcibly cancels the request at timeout.
   *
   * @return the number of seconds to wait for timeout. 0 means no timeout
   */

  int getRequestTimeoutSec();

  /**
   * Informs the scheduler that a YARN resource request timed out. The scheduler
   * can either retry or (more productively) assume that the requested node is
   * not available and adjust its target size downward.
   */

  void requestTimedOut();
}
