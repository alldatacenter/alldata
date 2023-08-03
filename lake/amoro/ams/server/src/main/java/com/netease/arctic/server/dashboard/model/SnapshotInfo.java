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

import org.apache.iceberg.relocated.com.google.common.base.MoreObjects;

public class SnapshotInfo {
  private Long snapshotId;
  private String operation;
  private Long totalSize;
  private Integer totalFiles;
  private Long totalRecords;
  private Integer addedFiles;
  private Long addedFilesSize;
  private Long addedRecords;
  private Long removedFilesSize;
  private Integer removedFiles;
  private Long removedRecords;

  public Long getSnapshotId() {
    return snapshotId;
  }

  public void setSnapshotId(Long snapshotId) {
    this.snapshotId = snapshotId;
  }

  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  public Long getTotalSize() {
    return totalSize;
  }

  public void setTotalSize(Long totalSize) {
    this.totalSize = totalSize;
  }

  public Integer getTotalFiles() {
    return totalFiles;
  }

  public void setTotalFiles(Integer totalFiles) {
    this.totalFiles = totalFiles;
  }

  public Long getTotalRecords() {
    return totalRecords;
  }

  public void setTotalRecords(Long totalRecords) {
    this.totalRecords = totalRecords;
  }

  public Integer getAddedFiles() {
    return addedFiles;
  }

  public void setAddedFiles(Integer addedFiles) {
    this.addedFiles = addedFiles;
  }

  public Long getAddedFilesSize() {
    return addedFilesSize;
  }

  public void setAddedFilesSize(Long addedFilesSize) {
    this.addedFilesSize = addedFilesSize;
  }

  public Long getAddedRecords() {
    return addedRecords;
  }

  public void setAddedRecords(Long addedRecords) {
    this.addedRecords = addedRecords;
  }

  public Long getRemovedFilesSize() {
    return removedFilesSize;
  }

  public void setRemovedFilesSize(Long removedFilesSize) {
    this.removedFilesSize = removedFilesSize;
  }

  public Integer getRemovedFiles() {
    return removedFiles;
  }

  public void setRemovedFiles(Integer removedFiles) {
    this.removedFiles = removedFiles;
  }

  public Long getRemovedRecords() {
    return removedRecords;
  }

  public void setRemovedRecords(Long removedRecords) {
    this.removedRecords = removedRecords;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("snapshotId", snapshotId)
        .add("operation", operation)
        .add("totalSize", totalSize)
        .add("totalFiles", totalFiles)
        .add("totalRecords", totalRecords)
        .add("addedFiles", addedFiles)
        .add("addedFilesSize", addedFilesSize)
        .add("addedRecords", addedRecords)
        .add("removedFilesSize", removedFilesSize)
        .add("removedFiles", removedFiles)
        .add("removedRecords", removedRecords)
        .toString();
  }
}
