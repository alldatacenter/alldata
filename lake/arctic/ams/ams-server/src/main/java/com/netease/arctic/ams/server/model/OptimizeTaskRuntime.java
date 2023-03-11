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

package com.netease.arctic.ams.server.model;

import com.netease.arctic.ams.api.ErrorMessage;
import com.netease.arctic.ams.api.JobId;
import com.netease.arctic.ams.api.OptimizeStatus;
import com.netease.arctic.ams.api.OptimizeTaskId;

import java.nio.ByteBuffer;
import java.util.List;

public class OptimizeTaskRuntime implements Cloneable {
  public static final long INVALID_TIME = 0;
  private OptimizeTaskId optimizeTaskId;
  private OptimizeStatus status = OptimizeStatus.Init;
  private long pendingTime = INVALID_TIME;
  private long executeTime = INVALID_TIME;
  private long preparedTime = INVALID_TIME;
  private long reportTime = INVALID_TIME;
  private long commitTime = INVALID_TIME;
  private long costTime = INVALID_TIME;

  private JobId jobId;
  private String attemptId;

  private int retry = 0;
  private ErrorMessage errorMessage = null;

  private long newFileSize;
  private int newFileCnt;
  private List<ByteBuffer> targetFiles;

  public OptimizeTaskRuntime() {
  }

  public OptimizeTaskRuntime(OptimizeTaskId optimizeTaskId) {
    this.optimizeTaskId = optimizeTaskId;
  }

  public OptimizeTaskId getOptimizeTaskId() {
    return optimizeTaskId;
  }

  public void setOptimizeTaskId(OptimizeTaskId optimizeTaskId) {
    this.optimizeTaskId = optimizeTaskId;
  }

  public OptimizeStatus getStatus() {
    return status;
  }

  public void setStatus(OptimizeStatus status) {
    this.status = status;
  }

  public int getRetry() {
    return retry;
  }

  public void setRetry(int retry) {
    this.retry = retry;
  }

  public ErrorMessage getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(ErrorMessage errorMessage) {
    this.errorMessage = errorMessage;
  }

  public long getPreparedTime() {
    return preparedTime;
  }

  public void setPreparedTime(long preparedTime) {
    this.preparedTime = preparedTime;
  }

  public long getCommitTime() {
    return commitTime;
  }

  public void setCommitTime(long commitTime) {
    this.commitTime = commitTime;
  }

  public long getExecuteTime() {
    return executeTime;
  }

  public void setExecuteTime(long executeTime) {
    this.executeTime = executeTime;
  }

  public long getPendingTime() {
    return pendingTime;
  }

  public void setPendingTime(long pendingTime) {
    this.pendingTime = pendingTime;
  }

  public JobId getJobId() {
    return jobId;
  }

  public void setJobId(JobId jobId) {
    this.jobId = jobId;
  }

  public long getReportTime() {
    return reportTime;
  }

  public void setReportTime(long reportTime) {
    this.reportTime = reportTime;
  }

  public long getNewFileSize() {
    return newFileSize;
  }

  public void setNewFileSize(long newFileSize) {
    this.newFileSize = newFileSize;
  }

  public int getNewFileCnt() {
    return newFileCnt;
  }

  public void setNewFileCnt(int newFileCnt) {
    this.newFileCnt = newFileCnt;
  }

  public String getFailReason() {
    return errorMessage == null ? null : errorMessage.getFailReason();
  }

  public long getFailTime() {
    return errorMessage == null ? 0 : errorMessage.getFailTime();
  }

  public String getAttemptId() {
    return attemptId;
  }

  public void setAttemptId(String attemptId) {
    this.attemptId = attemptId;
  }

  public long getCostTime() {
    return costTime;
  }

  public void setCostTime(long costTime) {
    this.costTime = costTime;
  }

  public List<ByteBuffer> getTargetFiles() {
    return targetFiles;
  }

  public void setTargetFiles(List<ByteBuffer> targetFiles) {
    this.targetFiles = targetFiles;
  }

  @Override
  public String toString() {
    return "BaseOptimizeRuntime{" +
        "optimizeTaskId=" + optimizeTaskId +
        ", status=" + status +
        ", pendingTime=" + pendingTime +
        ", executeTime=" + executeTime +
        ", preparedTime=" + preparedTime +
        ", reportTime=" + reportTime +
        ", commitTime=" + commitTime +
        ", costTime=" + costTime +
        ", jobId=" + jobId +
        ", retry=" + retry +
        ", errorMessage=" + errorMessage +
        ", newFileSize=" + newFileSize +
        ", newFileCnt=" + newFileCnt +
        ", attemptId='" + attemptId +
        ", targetFiles=" + targetFiles +
        '}';
  }

  @Override
  public OptimizeTaskRuntime clone() {
    try {
      return (OptimizeTaskRuntime) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }
}
