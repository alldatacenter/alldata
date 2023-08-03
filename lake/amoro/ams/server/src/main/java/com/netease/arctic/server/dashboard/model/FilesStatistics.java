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

public class FilesStatistics {
  private int fileCnt;
  private long totalSize;
  private long averageSize;

  public static FilesStatistics build(int fileCnt, long totalSize) {
    return new FilesStatistics(fileCnt, totalSize);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private int fileCnt = 0;
    private long totalSize = 0;

    public Builder addFile(long fileSize) {
      this.totalSize += fileSize;
      this.fileCnt++;
      return this;
    }

    public Builder addFilesStatistics(FilesStatistics fs) {
      this.totalSize += fs.getTotalSize();
      this.fileCnt += fs.getFileCnt();
      return this;
    }

    public Builder addFiles(long totalSize, int fileCnt) {
      this.totalSize += totalSize;
      this.fileCnt += fileCnt;
      return this;
    }

    public FilesStatistics build() {
      return FilesStatistics.build(fileCnt, totalSize);
    }
  }

  public FilesStatistics() {
  }

  public FilesStatistics(Integer fileCnt, Long totalSize) {
    this.fileCnt = fileCnt;
    this.totalSize = totalSize;
    if (fileCnt != 0) {
      this.averageSize = totalSize / fileCnt;
    }
  }

  public int getFileCnt() {
    return fileCnt;
  }

  public void setFileCnt(int fileCnt) {
    this.fileCnt = fileCnt;
  }

  public long getTotalSize() {
    return totalSize;
  }

  public void setTotalSize(long totalSize) {
    this.totalSize = totalSize;
  }

  public long getAverageSize() {
    return averageSize;
  }

  public void setAverageSize(long averageSize) {
    this.averageSize = averageSize;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("fileCnt", fileCnt)
        .add("totalSize", totalSize)
        .add("averageSize", averageSize)
        .toString();
  }
}
