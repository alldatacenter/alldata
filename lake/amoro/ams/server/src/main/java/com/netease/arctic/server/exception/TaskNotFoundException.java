package com.netease.arctic.server.exception;

import com.netease.arctic.ams.api.OptimizingTaskId;

public class TaskNotFoundException extends ArcticRuntimeException {
  private OptimizingTaskId taskId;

  public TaskNotFoundException(OptimizingTaskId taskId) {
    super("Task " + taskId + " not found.");
    this.taskId = taskId;
  }

  public OptimizingTaskId getTaskId() {
    return taskId;
  }
}
