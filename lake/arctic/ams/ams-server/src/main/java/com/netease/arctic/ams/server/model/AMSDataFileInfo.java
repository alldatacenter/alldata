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

import com.netease.arctic.ams.server.utils.AmsUtils;

public class AMSDataFileInfo {
  String path;
  String partition;
  String type;
  long fileSize;
  String size;
  long commitTime; // 13-bit timestamp
  String file;
  String operation;

  public AMSDataFileInfo() {

  }

  public AMSDataFileInfo(String path, String partition, String type, long fileSize, long commitTime, String operation) {
    this.partition = partition;
    this.type = type;
    this.commitTime = commitTime;
    this.operation = operation;
    setPath(path);
    setFileSize(fileSize);
  }

  public long getFileSize() {
    return fileSize;
  }

  public void setFileSize(long fileSize) {
    this.fileSize = fileSize;
    this.size = AmsUtils.byteToXB(fileSize);
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
    this.file = AmsUtils.getFileName(path);
  }

  public String getPartition() {
    return partition;
  }

  public void setPartition(String partition) {
    this.partition = partition;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getSize() {
    return size;
  }

  public long getCommitTime() {
    return commitTime;
  }

  public void setCommitTime(long commitTime) {
    this.commitTime = commitTime;
  }

  public String getFile() {
    return this.file;
  }

  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }
}
