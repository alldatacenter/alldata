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
import com.netease.arctic.table.TableIdentifier;

// TODO remove after web changed in 0.3.1
public class BaseMajorCompactRecord {
  protected TableIdentifier tableIdentifier;
  protected CompactRangeType compactRange;
  protected long recordId;
  protected long visibleTime;
  protected long commitTime;
  protected long planTime;
  protected long duration;
  protected FilesStatistics totalFilesStatBeforeCompact;
  protected FilesStatistics totalFilesStatAfterCompact;
  protected OptimizeType optimizeType;

  public BaseMajorCompactRecord() {
  }

  public TableIdentifier getTableIdentifier() {
    return tableIdentifier;
  }

  public void setTableIdentifier(TableIdentifier tableIdentifier) {
    this.tableIdentifier = tableIdentifier;
  }

  public long getRecordId() {
    return recordId;
  }

  public void setRecordId(long recordId) {
    this.recordId = recordId;
  }

  public long getVisibleTime() {
    return visibleTime;
  }

  public void setVisibleTime(long visibleTime) {
    this.visibleTime = visibleTime;
  }

  public long getCommitTime() {
    return commitTime;
  }

  public void setCommitTime(long commitTime) {
    this.commitTime = commitTime;
  }

  public long getPlanTime() {
    return planTime;
  }

  public void setPlanTime(long planTime) {
    this.planTime = planTime;
  }

  public long getDuration() {
    return duration;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }

  public FilesStatistics getTotalFilesStatBeforeCompact() {
    return totalFilesStatBeforeCompact;
  }

  public void setTotalFilesStatBeforeCompact(
          FilesStatistics totalFilesStatBeforeCompact) {
    this.totalFilesStatBeforeCompact = totalFilesStatBeforeCompact;
  }

  public FilesStatistics getTotalFilesStatAfterCompact() {
    return totalFilesStatAfterCompact;
  }

  public void setTotalFilesStatAfterCompact(
          FilesStatistics totalFilesStatAfterCompact) {
    this.totalFilesStatAfterCompact = totalFilesStatAfterCompact;
  }


  public CompactRangeType getCompactRange() {
    return compactRange;
  }

  public void setCompactRange(CompactRangeType compactRange) {
    this.compactRange = compactRange;
  }

  public OptimizeType getOptimizeType() {
    return optimizeType;
  }

  public void setOptimizeType(OptimizeType optimizeType) {
    this.optimizeType = optimizeType;
  }
}
