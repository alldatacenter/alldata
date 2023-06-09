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

package com.netease.arctic.optimizer.flink;

import org.apache.iceberg.ContentFile;

public class TaskStat {
  private final long startTime;
  private long endTime = 0;
  private int inputFileCnt;
  private long inputTotalSize;
  private int outputFileCnt;
  private long outputTotalSize;

  public TaskStat() {
    this.startTime = System.currentTimeMillis();
  }

  public boolean finished() {
    return endTime != 0;
  }

  public void finish() {
    this.endTime = System.currentTimeMillis();
  }

  public int getInputFileCnt() {
    return inputFileCnt;
  }

  public long getInputTotalSize() {
    return inputTotalSize;
  }

  public int getOutputFileCnt() {
    return outputFileCnt;
  }

  public long getOutputTotalSize() {
    return outputTotalSize;
  }

  public long getDuration() {
    if (finished()) {
      return this.endTime - this.startTime;
    } else {
      return System.currentTimeMillis() - this.startTime;
    }
  }

  public long getStartTime() {
    return startTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public void recordInputFiles(Iterable<ContentFile<?>> files) {
    if (files == null) {
      return;
    }
    for (ContentFile<?> file : files) {
      this.inputFileCnt++;
      this.inputTotalSize += file.fileSizeInBytes();
    }
  }

  public void recordOutFiles(Iterable<? extends ContentFile<?>> files) {
    if (files == null) {
      return;
    }
    for (ContentFile<?> file : files) {
      this.outputFileCnt++;
      this.outputTotalSize += file.fileSizeInBytes();
    }
  }

  @Override
  public String toString() {
    return "TaskStat{" +
        "startTime=" + startTime +
        ", endTime=" + endTime +
        ", inputFileCnt=" + inputFileCnt +
        ", inputTotalSize=" + inputTotalSize +
        ", outputFileCnt=" + outputFileCnt +
        ", outputTotalSize=" + outputTotalSize +
        '}';
  }
}
