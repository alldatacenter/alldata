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

import com.netease.arctic.ams.api.OptimizeTask;

public class BasicOptimizeTask extends OptimizeTask {
  public static final int INVALID_SEQUENCE = -1;

  protected String taskCommitGroup;
  protected String taskPlanGroup;
  protected String partition;

  protected long baseFileSize;
  protected long insertFileSize;
  protected long deleteFileSize;
  protected long posDeleteFileSize;

  protected int baseFileCnt;
  protected int insertFileCnt;
  protected int deleteFileCnt;
  protected int posDeleteFileCnt;

  protected int queueId = -1;
  protected long createTime;

  private long toSequence = INVALID_SEQUENCE;
  private long fromSequence = INVALID_SEQUENCE;

  public BasicOptimizeTask() {
  }

  public String getTaskCommitGroup() {
    return taskCommitGroup;
  }

  public void setTaskCommitGroup(String taskCommitGroup) {
    this.taskCommitGroup = taskCommitGroup;
  }

  public String getTaskPlanGroup() {
    return taskPlanGroup;
  }

  public void setTaskPlanGroup(String taskPlanGroup) {
    this.taskPlanGroup = taskPlanGroup;
  }

  public long getToSequence() {
    return toSequence;
  }

  public void setToSequence(long toSequence) {
    this.toSequence = toSequence;
  }

  public long getFromSequence() {
    return fromSequence;
  }

  public void setFromSequence(long fromSequence) {
    this.fromSequence = fromSequence;
  }

  public String getPartition() {
    return partition;
  }

  public void setPartition(String partition) {
    this.partition = partition;
  }

  public long getBaseFileSize() {
    return baseFileSize;
  }

  public void setBaseFileSize(long baseFileSize) {
    this.baseFileSize = baseFileSize;
  }

  public long getInsertFileSize() {
    return insertFileSize;
  }

  public void setInsertFileSize(long insertFileSize) {
    this.insertFileSize = insertFileSize;
  }

  public long getDeleteFileSize() {
    return deleteFileSize;
  }

  public void setDeleteFileSize(long deleteFileSize) {
    this.deleteFileSize = deleteFileSize;
  }

  public long getCreateTime() {
    return createTime;
  }

  public void setCreateTime(long createTime) {
    this.createTime = createTime;
  }

  public int getQueueId() {
    return queueId;
  }

  public void setQueueId(int queueId) {
    this.queueId = queueId;
  }

  public int getBaseFileCnt() {
    return baseFileCnt;
  }

  public void setBaseFileCnt(int baseFileCnt) {
    this.baseFileCnt = baseFileCnt;
  }

  public int getInsertFileCnt() {
    return insertFileCnt;
  }

  public void setInsertFileCnt(int insertFileCnt) {
    this.insertFileCnt = insertFileCnt;
  }

  public int getDeleteFileCnt() {
    return deleteFileCnt;
  }

  public void setDeleteFileCnt(int deleteFileCnt) {
    this.deleteFileCnt = deleteFileCnt;
  }

  public long getPosDeleteFileSize() {
    return posDeleteFileSize;
  }

  public void setPosDeleteFileSize(long posDeleteFileSize) {
    this.posDeleteFileSize = posDeleteFileSize;
  }

  public int getPosDeleteFileCnt() {
    return posDeleteFileCnt;
  }

  public void setPosDeleteFileCnt(int posDeleteFileCnt) {
    this.posDeleteFileCnt = posDeleteFileCnt;
  }

  @Override
  public String toString() {
    return "BaseOptimizeTask{" +
        "taskCommitGroup='" + taskCommitGroup + '\'' +
        ", taskPlanGroup='" + taskPlanGroup + '\'' +
        ", partition='" + partition + '\'' +
        ", baseFileSize=" + baseFileSize +
        ", insertFileSize=" + insertFileSize +
        ", deleteFileSize=" + deleteFileSize +
        ", posDeleteFileSize=" + posDeleteFileSize +
        ", baseFileCnt=" + baseFileCnt +
        ", insertFileCnt=" + insertFileCnt +
        ", deleteFileCnt=" + deleteFileCnt +
        ", posDeleteFileCnt=" + posDeleteFileCnt +
        ", queueId=" + queueId +
        ", createTime=" + createTime +
        ", toSequence=" + toSequence +
        ", fromSequence=" + fromSequence +
        "} " + superToString();
  }

  private String superToString() {
    // to fix insertFiles/deleteFiles/baseFiles too long
    StringBuilder sb = new StringBuilder("OptimizeTask(");
    boolean first = true;

    sb.append("taskId:");
    if (this.taskId == null) {
      sb.append("null");
    } else {
      sb.append(this.taskId);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("tableIdentifier:");
    if (this.tableIdentifier == null) {
      sb.append("null");
    } else {
      sb.append(this.tableIdentifier);
    }
    first = false;
    if (isSetInsertFiles()) {
      if (!first) sb.append(", ");
      sb.append("insertFiles:");
      if (this.insertFiles == null) {
        sb.append("null");
      } else {
        sb.append(this.insertFiles.size());
      }
      first = false;
    }
    if (isSetDeleteFiles()) {
      if (!first) sb.append(", ");
      sb.append("deleteFiles:");
      if (this.deleteFiles == null) {
        sb.append("null");
      } else {
        sb.append(this.deleteFiles.size());
      }
      first = false;
    }
    if (isSetBaseFiles()) {
      if (!first) sb.append(", ");
      sb.append("baseFiles:");
      if (this.baseFiles == null) {
        sb.append("null");
      } else {
        sb.append(this.baseFiles.size());
      }
      first = false;
    }
    if (isSetPosDeleteFiles()) {
      if (!first) sb.append(", ");
      sb.append("posDeleteFiles:");
      if (this.posDeleteFiles == null) {
        sb.append("null");
      } else {
        sb.append(this.posDeleteFiles.size());
      }
      first = false;
    }
    if (isSetSourceNodes()) {
      if (!first) sb.append(", ");
      sb.append("sourceNodes:");
      if (this.sourceNodes == null) {
        sb.append("null");
      } else {
        sb.append(this.sourceNodes);
      }
      first = false;
    }
    if (isSetProperties()) {
      if (!first) sb.append(", ");
      sb.append("properties:");
      if (this.properties == null) {
        sb.append("null");
      } else {
        sb.append(this.properties);
      }
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }
}
