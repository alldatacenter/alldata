/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.netease.arctic.optimizer;

import com.netease.arctic.ams.api.OptimizeTask;

import java.io.Serializable;

/**
 * Task with attemptId.
 */
public class TaskWrapper implements Serializable {
  private OptimizeTask task;
  private int attemptId;

  public TaskWrapper(OptimizeTask task, int attemptId) {
    this.task = task;
    this.attemptId = attemptId;
  }

  public OptimizeTask getTask() {
    return task;
  }

  public void setTask(OptimizeTask task) {
    this.task = task;
  }

  public int getAttemptId() {
    return attemptId;
  }

  public void setAttemptId(int attemptId) {
    this.attemptId = attemptId;
  }

  @Override
  public String toString() {
    return "TaskWrapper{" +
        "task=" + task +
        ", attemptId='" + attemptId + '\'' +
        '}';
  }
}
