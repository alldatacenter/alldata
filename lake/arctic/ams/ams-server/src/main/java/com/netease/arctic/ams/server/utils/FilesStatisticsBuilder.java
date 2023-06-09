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

package com.netease.arctic.ams.server.utils;

import com.netease.arctic.ams.server.model.FilesStatistics;

public class FilesStatisticsBuilder {
  private int fileCnt = 0;
  private long totalSize = 0;

  public void addFile(long fileSize) {
    this.totalSize += fileSize;
    this.fileCnt++;
  }

  public FilesStatisticsBuilder addFilesStatistics(FilesStatistics fs) {
    this.totalSize += fs.getTotalSize();
    this.fileCnt += fs.getFileCnt();
    return this;
  }

  public void addFiles(long totalSize, int fileCnt) {
    this.totalSize += totalSize;
    this.fileCnt += fileCnt;
  }

  public FilesStatistics build() {
    return new FilesStatistics(fileCnt, totalSize);
  }

  public static FilesStatistics build(int fileCnt, long totalSize) {
    return new FilesStatistics(fileCnt, totalSize);
  }
}
