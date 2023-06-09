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

import com.netease.arctic.ams.api.TableIdentifier;

public class CacheSnapshotInfo {
  private TableIdentifier tableIdentifier;
  private Long snapshotId;
  private Long snapshotSequence;
  private Long parentSnapshotId;
  private String action;
  private String innerTable;
  private String producer;
  private Long commitTime;
  private Long fileSize;
  private Integer fileCount;


  public TableIdentifier getTableIdentifier() {
    return tableIdentifier;
  }

  public void setTableIdentifier(TableIdentifier tableIdentifier) {
    this.tableIdentifier = tableIdentifier;
  }

  public Long getSnapshotId() {
    return snapshotId;
  }

  public void setSnapshotId(Long snapshotId) {
    this.snapshotId = snapshotId;
  }

  public Long getParentSnapshotId() {
    return parentSnapshotId;
  }

  public void setParentSnapshotId(Long parentSnapshotId) {
    this.parentSnapshotId = parentSnapshotId;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getInnerTable() {
    return innerTable;
  }

  public void setInnerTable(String innerTable) {
    this.innerTable = innerTable;
  }

  public String getProducer() {
    return producer;
  }

  public void setProducer(String producer) {
    this.producer = producer;
  }

  public Long getCommitTime() {
    return commitTime;
  }

  public void setCommitTime(Long commitTime) {
    this.commitTime = commitTime;
  }

  public Long getFileSize() {
    return fileSize;
  }

  public void setFileSize(Long fileSize) {
    this.fileSize = fileSize;
  }

  public Integer getFileCount() {
    return fileCount;
  }

  public void setFileCount(Integer fileCount) {
    this.fileCount = fileCount;
  }

  public Long getSnapshotSequence() {
    return snapshotSequence;
  }

  public void setSnapshotSequence(Long snapshotSequence) {
    this.snapshotSequence = snapshotSequence;
  }
}
