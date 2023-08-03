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

import com.netease.arctic.data.DataFileType;
import com.netease.arctic.server.dashboard.utils.AmsUtil;

public class PartitionFileBaseInfo {
  private String commitId;
  private DataFileType fileType;
  private Long commitTime;
  private String size;
  private String partitionName;
  private String path;
  private String file;
  private long fileSize;

  public PartitionFileBaseInfo() {
  }

  public PartitionFileBaseInfo(String commitId, DataFileType fileType, Long commitTime,
                               String partitionName, String path, long fileSize) {
    this.commitId = commitId;
    this.fileType = fileType;
    this.commitTime = commitTime;
    this.partitionName = partitionName;
    setPath(path);
    setFileSize(fileSize);
  }

  public String getCommitId() {
    return commitId;
  }

  public void setCommitId(String commitId) {
    this.commitId = commitId;
  }

  public DataFileType getFileType() {
    return fileType;
  }

  public void setFileType(DataFileType fileType) {
    this.fileType = fileType;
  }

  public Long getCommitTime() {
    return commitTime;
  }

  public void setCommitTime(Long commitTime) {
    this.commitTime = commitTime;
  }

  public String getSize() {
    return size;
  }

  public String getPartitionName() {
    return partitionName;
  }

  public void setPartitionName(String partitionName) {
    this.partitionName = partitionName;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
    this.file = AmsUtil.getFileName(path);
  }

  public String getFile() {
    return file;
  }

  public long getFileSize() {
    return fileSize;
  }

  public void setFileSize(long fileSize) {
    this.fileSize = fileSize;
    this.size = AmsUtil.byteToXB(fileSize);
  }
}
