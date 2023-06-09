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

import com.netease.arctic.ams.api.OptimizeType;

import javax.annotation.Nullable;

public class TaskConfig {
  private final OptimizeType optimizeType;
  private final String partition;
  private final String commitGroup;
  private final String planGroup;
  private final long createTime;

  private final boolean moveFilesToHiveLocation;
  @Nullable
  private final String customHiveSubdirectory;
  @Nullable
  private final Long toSequence;
  @Nullable
  private final Long fromSequence;

  private TaskConfig(OptimizeType optimizeType, String partition, String commitGroup, String planGroup, long createTime,
                     boolean moveFilesToHiveLocation, @Nullable String customHiveSubdirectory,
                     @Nullable Long toSequence, @Nullable Long fromSequence) {
    this.optimizeType = optimizeType;
    this.partition = partition;
    this.commitGroup = commitGroup;
    this.planGroup = planGroup;
    this.createTime = createTime;
    this.moveFilesToHiveLocation = moveFilesToHiveLocation;
    this.customHiveSubdirectory = customHiveSubdirectory;
    this.toSequence = toSequence;
    this.fromSequence = fromSequence;
  }

  public TaskConfig(OptimizeType optimizeType, String partition, String commitGroup, String planGroup, long createTime,
                    boolean moveFilesToHiveLocation, @Nullable String customHiveSubdirectory) {
    this(optimizeType, partition, commitGroup, planGroup, createTime, moveFilesToHiveLocation, customHiveSubdirectory,
        null, null);
  }

  public TaskConfig(OptimizeType optimizeType, String partition, String commitGroup, String planGroup, long createTime,
                    @Nullable Long toSequence, @Nullable Long fromSequence) {
    this(optimizeType, partition, commitGroup, planGroup, createTime, false, null,
        toSequence, fromSequence);
  }

  public TaskConfig(OptimizeType optimizeType, String partition, String commitGroup, String planGroup,
                    long createTime) {
    this(optimizeType, partition, commitGroup, planGroup, createTime, false, null,
        null, null);
  }

  public OptimizeType getOptimizeType() {
    return optimizeType;
  }

  public String getPartition() {
    return partition;
  }

  @javax.annotation.Nullable
  public Long getToSequence() {
    return toSequence;
  }

  @javax.annotation.Nullable
  public Long getFromSequence() {
    return fromSequence;
  }

  public String getCommitGroup() {
    return commitGroup;
  }

  public String getPlanGroup() {
    return planGroup;
  }

  public long getCreateTime() {
    return createTime;
  }

  @Nullable
  public String getCustomHiveSubdirectory() {
    return customHiveSubdirectory;
  }

  public boolean isMoveFilesToHiveLocation() {
    return moveFilesToHiveLocation;
  }

  @Override
  public String toString() {
    return "TaskConfig{" +
        "optimizeType=" + optimizeType +
        ", partition='" + partition + '\'' +
        ", commitGroup='" + commitGroup + '\'' +
        ", planGroup='" + planGroup + '\'' +
        ", createTime=" + createTime +
        ", moveFilesToHiveLocation=" + moveFilesToHiveLocation +
        ", customHiveSubdirectory='" + customHiveSubdirectory + '\'' +
        ", maxChangeSequence=" + toSequence +
        ", minChangeSequence=" + fromSequence +
        '}';
  }
}
