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

package com.netease.arctic.server.optimizing;

import com.google.common.collect.Sets;
import com.netease.arctic.ams.api.OptimizingTask;
import com.netease.arctic.ams.api.OptimizingTaskId;
import com.netease.arctic.ams.api.OptimizingTaskResult;
import com.netease.arctic.optimizing.RewriteFilesInput;
import com.netease.arctic.optimizing.RewriteFilesOutput;
import com.netease.arctic.server.ArcticServiceConstants;
import com.netease.arctic.server.dashboard.utils.OptimizingUtil;
import com.netease.arctic.server.exception.DuplicateRuntimeException;
import com.netease.arctic.server.exception.IllegalTaskStateException;
import com.netease.arctic.server.exception.OptimizingClosedException;
import com.netease.arctic.server.optimizing.plan.TaskDescriptor;
import com.netease.arctic.server.persistence.StatedPersistentBase;
import com.netease.arctic.server.persistence.TaskFilesPersistence;
import com.netease.arctic.server.persistence.mapper.OptimizingMapper;
import com.netease.arctic.utils.SerializationUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TaskRuntime extends StatedPersistentBase {
  private long tableId;
  private String partition;
  private OptimizingTaskId taskId;
  @StateField
  private Status status = Status.PLANNED;
  private final TaskStatusMachine statusMachine = new TaskStatusMachine();
  @StateField
  private int retry = 0;
  @StateField
  private long startTime = ArcticServiceConstants.INVALID_TIME;
  @StateField
  private long endTime = ArcticServiceConstants.INVALID_TIME;
  @StateField
  private long costTime = 0;
  @StateField
  private OptimizingQueue.OptimizingThread optimizingThread;
  @StateField
  private String failReason;
  private TaskOwner owner;
  private RewriteFilesInput input;
  @StateField
  private RewriteFilesOutput output;
  @StateField
  private MetricsSummary summary;
  private Map<String, String> properties;

  private TaskRuntime() {
  }

  public TaskRuntime(
      OptimizingTaskId taskId,
      TaskDescriptor taskDescriptor,
      Map<String, String> properties) {
    this.taskId = taskId;
    this.partition = taskDescriptor.getPartition();
    this.input = taskDescriptor.getInput();
    this.summary = new MetricsSummary(input);
    this.tableId = taskDescriptor.getTableId();
    this.properties = properties;
  }

  public void complete(OptimizingQueue.OptimizingThread thread, OptimizingTaskResult result) {
    invokeConsisitency(() -> {
      validThread(thread);
      if (result.getErrorMessage() != null) {
        fail(result.getErrorMessage());
      } else {
        finish(TaskFilesPersistence.loadTaskOutput(result.getTaskOutput()));
      }
      owner.acceptResult(this);
      optimizingThread = null;
    });
  }

  private void finish(RewriteFilesOutput filesOutput) {
    invokeConsisitency(() -> {
      statusMachine.accept(Status.SUCCESS);
      summary.setNewFileCnt(OptimizingUtil.getFileCount(filesOutput));
      summary.setNewFileSize(OptimizingUtil.getFileSize(filesOutput));
      endTime = System.currentTimeMillis();
      costTime += endTime - startTime;
      output = filesOutput;
      persistTaskRuntime(this);
    });
  }

  void fail(String errorMessage) {
    invokeConsisitency(() -> {
      statusMachine.accept(Status.FAILED);
      failReason = errorMessage;
      endTime = System.currentTimeMillis();
      costTime += endTime - startTime;
      persistTaskRuntime(this);
    });
  }

  void reset(boolean incRetryCount) {
    invokeConsisitency(() -> {
      if (incRetryCount) {
        retry++;
      }
      statusMachine.accept(Status.PLANNED);
      doAs(OptimizingMapper.class, mapper ->
          mapper.updateTaskStatus(this, Status.PLANNED));
    });
  }

  void schedule(OptimizingQueue.OptimizingThread thread) {
    invokeConsisitency(() -> {
      statusMachine.accept(Status.SCHEDULED);
      optimizingThread = thread;
      startTime = System.currentTimeMillis();
      persistTaskRuntime(this);
    });
  }

  void ack(OptimizingQueue.OptimizingThread thread) {
    invokeConsisitency(() -> {
      validThread(thread);
      statusMachine.accept(Status.ACKED);
      startTime = System.currentTimeMillis();
      endTime = ArcticServiceConstants.INVALID_TIME;
      persistTaskRuntime(this);
    });
  }

  void tryCanceling() {
    invokeConsisitency(() -> {
      if (statusMachine.tryAccepting(Status.CANCELED)) {
        costTime = System.currentTimeMillis() - startTime;
        persistTaskRuntime(this);
      }
    });
  }


  public TaskRuntime claimOwnership(TaskOwner owner) {
    this.owner = owner;
    return this;
  }

  public boolean finished() {
    return this.status == Status.SUCCESS || this.status == Status.FAILED || this.status == Status.CANCELED;
  }

  protected void setInput(RewriteFilesInput input) {
    if (input == null) {
      throw new IllegalStateException("Optimizing input is null, id:" + taskId);
    }
    this.input = input;
  }

  public RewriteFilesInput getInput() {
    return input;
  }

  public RewriteFilesOutput getOutput() {
    return output;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public long getProcessId() {
    return taskId.getProcessId();
  }

  public OptimizingQueue.OptimizingThread getOptimizingThread() {
    return optimizingThread;
  }

  public OptimizingTask getOptimizingTask() {
    OptimizingTask optimizingTask = new OptimizingTask(taskId);
    optimizingTask.setTaskInput(SerializationUtil.simpleSerialize(input));
    optimizingTask.setProperties(properties);
    return optimizingTask;
  }

  public long getStartTime() {
    return startTime;
  }

  public OptimizingTaskId getTaskId() {
    return taskId;
  }

  public Status getStatus() {
    return status;
  }

  public int getRetry() {
    return retry;
  }

  public MetricsSummary getMetricsSummary() {
    return summary;
  }

  public String getPartition() {
    return partition;
  }

  public String getFailReason() {
    return failReason;
  }

  public long getCostTime() {
    if (endTime != ArcticServiceConstants.INVALID_TIME) {
      long elapse = System.currentTimeMillis() - startTime;
      return Math.max(0, elapse) + costTime;
    }
    return costTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public long getQuotaTime(long calculatingStartTime, long calculatingEndTime) {
    if (startTime == ArcticServiceConstants.INVALID_TIME) {
      return 0;
    }
    calculatingStartTime = Math.max(startTime, calculatingStartTime);
    calculatingEndTime = costTime == ArcticServiceConstants.INVALID_TIME ? calculatingEndTime : costTime + startTime;
    long lastingTime = calculatingEndTime - calculatingStartTime;
    return Math.max(0, lastingTime);
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public MetricsSummary getSummary() {
    return summary;
  }

  public void setSummary(MetricsSummary summary) {
    this.summary = summary;
  }

  public long getTableId() {
    return tableId;
  }

  private void validThread(OptimizingQueue.OptimizingThread thread) {
    if (!thread.equals(this.optimizingThread)) {
      throw new DuplicateRuntimeException("Task already acked by optimizer thread + " + thread);
    }
  }

  private void persistTaskRuntime(TaskRuntime taskRuntime) {
    doAs(OptimizingMapper.class, mapper -> mapper.updateTaskRuntime(taskRuntime));
  }

  public TaskQuota getCurrentQuota() {
    if (startTime == ArcticServiceConstants.INVALID_TIME || endTime == ArcticServiceConstants.INVALID_TIME) {
      throw new IllegalStateException("start time or end time is not correctly set");
    }
    return new TaskQuota(this);
  }

  public boolean isSuspending(long determineTime, long ackTimeout) {
    return status == TaskRuntime.Status.SCHEDULED &&
        determineTime - startTime > ackTimeout;
  }

  private static final Map<Status, Set<Status>> nextStatusMap = new HashMap<>();

  static {
    nextStatusMap.put(
        Status.PLANNED,
        Sets.newHashSet(
            Status.PLANNED,
            Status.SCHEDULED,
            Status.CANCELED));
    nextStatusMap.put(
        Status.SCHEDULED,
        Sets.newHashSet(
            Status.PLANNED,
            Status.SCHEDULED,
            Status.ACKED,
            Status.CANCELED));
    nextStatusMap.put(
        Status.ACKED,
        Sets.newHashSet(
            Status.PLANNED,
            Status.ACKED,
            Status.SUCCESS,
            Status.FAILED,
            Status.CANCELED));
    nextStatusMap.put(
        Status.FAILED,
        Sets.newHashSet(
            Status.PLANNED,
            Status.FAILED,
            Status.CANCELED));
    nextStatusMap.put(
        Status.SUCCESS,
        Sets.newHashSet(Status.SUCCESS));
    nextStatusMap.put(
        Status.CANCELED,
        Sets.newHashSet(Status.CANCELED));
  }

  private class TaskStatusMachine {

    private Set<Status> next;

    private TaskStatusMachine() {
      this.next = nextStatusMap.get(status);
    }

    public void accept(Status targetStatus) {
      if (owner.isClosed()) {
        throw new OptimizingClosedException(taskId.getProcessId());
      }
      next = nextStatusMap.get(status);
      if (!next.contains(targetStatus)) {
        throw new IllegalTaskStateException(taskId, status, targetStatus);
      }
      status = targetStatus;
      next = nextStatusMap.get(status);
    }

    public synchronized boolean tryAccepting(Status targetStatus) {
      if (owner.isClosed() || !next.contains(targetStatus)) {
        return false;
      }
      status = targetStatus;
      next = nextStatusMap.get(status);
      return true;
    }
  }

  public enum Status {
    PLANNED,
    SCHEDULED,
    ACKED,
    FAILED,
    SUCCESS,
    CANCELED // If Optimizing process failed, all tasks will be CANCELED except for SUCCESS tasks
  }

  public static class TaskQuota {

    private long processId;
    private int taskId;
    private int retryNum;
    private long startTime;
    private long endTime;
    private String failReason;
    private long tableId;

    public TaskQuota() {

    }

    public TaskQuota(TaskRuntime task) {
      this.startTime = task.getStartTime();
      this.endTime = task.getEndTime();
      this.processId = task.getTaskId().getProcessId();
      this.taskId = task.getTaskId().getTaskId();
      this.tableId = task.getTableId();
      this.retryNum = task.getRetry();
    }

    public long getStartTime() {
      return startTime;
    }

    public long getProcessId() {
      return processId;
    }

    public int getTaskId() {
      return taskId;
    }


    public int getRetryNum() {
      return retryNum;
    }

    public long getEndTime() {
      return endTime;
    }

    public String getFailReason() {
      return failReason;
    }

    public long getTableId() {
      return tableId;
    }

    public long getQuotaTime(long calculatingStartTime) {
      long lastingTime = endTime - Math.max(startTime, calculatingStartTime);
      return Math.max(0, lastingTime);
    }

    public boolean checkExpired(long validTime) {
      return endTime <= validTime;
    }
  }

  public interface TaskOwner {
    void acceptResult(TaskRuntime taskRuntime);

    boolean isClosed();
  }
}
