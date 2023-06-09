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

import org.apache.drill.yarn.appMaster.Scheduler.TaskManager;

public class EventContext {
  public final AMYarnFacade yarn;
  public final ClusterControllerImpl controller;
  public SchedulerStateImpl group;
  public Task task;

  public EventContext(ClusterControllerImpl controller) {
    yarn = controller.getYarn();
    this.controller = controller;
  }

  public EventContext(ClusterController controller) {
    this((ClusterControllerImpl) controller);
  }

  public EventContext(ClusterControllerImpl controller, Task task) {
    this(controller);
    setTask(task);
  }

  /**
   * For testing only, omits the controller and YARN.
   *
   * @param task
   */

  public EventContext(Task task) {
    controller = null;
    yarn = null;
    this.task = task;
  }

  public void setTask(Task task) {
    this.task = task;
    group = task.getGroup();
  }

  public TaskState getState() {
    return task.state;
  }

  public void setGroup(SchedulerStateActions group) {
    this.group = (SchedulerStateImpl) group;
  }

  public TaskManager getTaskManager() {
    return group.getScheduler().getTaskManager();
  }
}