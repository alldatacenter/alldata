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

import java.util.Map;

/**
 * A simplified meta of task, not include input/output files.
 */
public class OptimizingTaskMeta {
  private long processId;
  private int taskId;
  private int retryNum;
  private long tableId;
  private String partitionData;
  private long createTime;
  private long startTime;
  private long endTime;
  private long costTime;
  private TaskRuntime.Status status;
  private String failReason;
  private String optimizerToken;
  private int threadId;
  private MetricsSummary metricsSummary;
  private Map<String, String> properties;

  public OptimizingTaskMeta() {
  }

  public long getProcessId() {
    return processId;
  }

  public void setProcessId(long processId) {
    this.processId = processId;
  }

  public TaskRuntime.Status getStatus() {
    return status;
  }

  public void setStatus(TaskRuntime.Status status) {
    this.status = status;
  }

  public int getTaskId() {
    return taskId;
  }

  public void setTaskId(int taskId) {
    this.taskId = taskId;
  }

  public int getRetryNum() {
    return retryNum;
  }

  public void setRetryNum(int retryNum) {
    this.retryNum = retryNum;
  }

  public long getTableId() {
    return tableId;
  }

  public void setTableId(long tableId) {
    this.tableId = tableId;
  }

  public String getPartitionData() {
    return partitionData;
  }

  public void setPartitionData(String partitionData) {
    this.partitionData = partitionData;
  }

  public long getCreateTime() {
    return createTime;
  }

  public void setCreateTime(long createTime) {
    this.createTime = createTime;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public void setEndTime(long endTime) {
    this.endTime = endTime;
  }

  public long getCostTime() {
    return costTime;
  }

  public void setCostTime(long costTime) {
    this.costTime = costTime;
  }

  public String getFailReason() {
    return failReason;
  }

  public void setFailReason(String failReason) {
    this.failReason = failReason;
  }

  public String getOptimizerToken() {
    return optimizerToken;
  }

  public void setOptimizerToken(String optimizerToken) {
    this.optimizerToken = optimizerToken;
  }

  public int getThreadId() {
    return threadId;
  }

  public void setThreadId(int threadId) {
    this.threadId = threadId;
  }

  public MetricsSummary getMetricsSummary() {
    return metricsSummary;
  }

  public void setMetricsSummary(MetricsSummary metricsSummary) {
    this.metricsSummary = metricsSummary;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }
}
