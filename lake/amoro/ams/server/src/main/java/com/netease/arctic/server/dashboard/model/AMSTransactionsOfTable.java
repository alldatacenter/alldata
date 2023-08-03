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

package com.netease.arctic.server.dashboard.model;

import java.util.Objects;


public class AMSTransactionsOfTable {
  private String transactionId;
  private int fileCount;
  private String fileSize;
  private long commitTime;
  private String snapshotId;


  public AMSTransactionsOfTable() {
  }

  public AMSTransactionsOfTable(String transactionId, int fileCount, String fileSize, long commitTime) {
    this.transactionId = transactionId;
    this.fileCount = fileCount;
    this.fileSize = fileSize;
    this.commitTime = commitTime;
    this.snapshotId = this.transactionId;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }

  public int getFileCount() {
    return fileCount;
  }

  public void setFileCount(int fileCount) {
    this.fileCount = fileCount;
  }

  public String getFileSize() {
    return fileSize;
  }

  public void setFileSize(String fileSize) {
    this.fileSize = fileSize;
  }

  public long getCommitTime() {
    return commitTime;
  }

  public void setCommitTime(long commitTime) {
    this.commitTime = commitTime;
  }

  public String getSnapshotId() {
    return snapshotId;
  }

  public void setSnapshotId(String snapshotId) {
    this.snapshotId = snapshotId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AMSTransactionsOfTable that = (AMSTransactionsOfTable) o;
    return transactionId == that.transactionId &&
            fileCount == that.fileCount &&
            fileSize == that.fileSize &&
            commitTime == that.commitTime &&
            snapshotId == that.snapshotId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(transactionId, fileCount, fileSize, commitTime, snapshotId);
  }
}
