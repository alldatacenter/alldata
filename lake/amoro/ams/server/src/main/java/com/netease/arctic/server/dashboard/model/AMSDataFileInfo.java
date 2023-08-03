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

import com.netease.arctic.ams.api.PartitionFieldData;
import com.netease.arctic.server.dashboard.utils.AmsUtil;
import com.netease.arctic.utils.ConvertStructUtil;
import org.apache.iceberg.FileContent;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.StructLike;

import java.util.List;

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

  public AMSDataFileInfo(String path, PartitionSpec partitionSpec, StructLike partitionStruct, FileContent type,
      long fileSize, long commitTime,
      String operation) {
    this.partition = partitionToPath(ConvertStructUtil.partitionFields(partitionSpec, partitionStruct));
    this.type = getIcebergFileType(type);
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
    this.size = AmsUtil.byteToXB(fileSize);
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
    this.file = AmsUtil.getFileName(path);
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

  private String getIcebergFileType(FileContent fileContent) {
    switch (fileContent) {
      case DATA:
        return "data";
      case EQUALITY_DELETES:
        return "eq-deletes";
      case POSITION_DELETES:
        return "pos-deletes";
      default:
        throw new UnsupportedOperationException("unknown fileContent " + fileContent);
    }
  }

  private String partitionToPath(List<PartitionFieldData> partitionFieldDataList) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < partitionFieldDataList.size(); i++) {
      if (i > 0) {
        sb.append("/");
      }
      sb.append(partitionFieldDataList.get(i).getName()).append("=")
          .append(partitionFieldDataList.get(i).getValue());
    }
    return sb.toString();
  }
}
