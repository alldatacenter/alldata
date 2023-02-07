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
import org.apache.drill.yarn.core.ContainerRequestSpec;
import org.apache.hadoop.yarn.api.records.Resource;

public abstract class AbstractScheduler implements Scheduler {
  private static final Log LOG = LogFactory.getLog(AbstractScheduler.class);
  private final String name;
  private final String type;
  protected TaskSpec taskSpec;
  protected int priority;
  protected int failCount;
  protected TaskManager taskManager;
  protected SchedulerState state;
  protected boolean isTracked;

  public AbstractScheduler(String type, String name) {
    this.type = type;
    this.name = name;
    taskManager = new AbstractTaskManager();
  }

  public void setTaskManager(TaskManager taskManager) {
    this.taskManager = taskManager;
  }

  @Override
  public void registerState(SchedulerState state) {
    this.state = state;
  }

  @Override
  public void setPriority(int priority) {
    this.priority = priority;
    taskSpec.containerSpec.priority = priority;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public TaskManager getTaskManager() {
    return taskManager;
  }

  @Override
  public void change(int delta) {
    resize(getTarget() + delta);
  }

  protected void addTasks(int n) {
    LOG.info( "[" + getName( ) + "] - Adding " + n + " tasks" );
    for (int i = 0; i < n; i++) {
      state.start(new Task(this, taskSpec));
    }
  }

  @Override
  public boolean isTracked() {
    return isTracked;
  }

  @Override
  public ContainerRequestSpec getResource() {
    return taskSpec.containerSpec;
  }

  @Override
  public void limitContainerSize(Resource maxResource) throws AMException {
    if (taskSpec.containerSpec.memoryMb > maxResource.getMemory()) {
      LOG.warn(taskSpec.name + " requires " + taskSpec.containerSpec.memoryMb
          + " MB but the maximum YARN container size is "
          + maxResource.getMemory() + " MB");
      taskSpec.containerSpec.memoryMb = maxResource.getMemory();
    }
    if (taskSpec.containerSpec.vCores > maxResource.getVirtualCores()) {
      LOG.warn(taskSpec.name + " requires " + taskSpec.containerSpec.vCores
          + " vcores but the maximum YARN container size is "
          + maxResource.getVirtualCores() + " vcores");
      taskSpec.containerSpec.vCores = maxResource.getVirtualCores();
    }
  }

  @Override
  public int getRequestTimeoutSec() { return 0; }
}
