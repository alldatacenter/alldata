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

package com.netease.arctic.flink.read.hybrid.split;

import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.data.PrimaryKeyedFile;
import com.netease.arctic.scan.ArcticFileScanTask;
import com.netease.arctic.utils.FileScanTaskUtil;
import org.apache.iceberg.relocated.com.google.common.base.MoreObjects;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;

import java.util.Collection;
import java.util.Optional;

/**
 * A changelog split generated during planning change table.
 */
public class ChangelogSplit extends ArcticSplit {
  private static final long serialVersionUID = 1L;
  private final int taskIndex;
  private final Collection<ArcticFileScanTask> insertScanTasks;
  private final Collection<ArcticFileScanTask> deleteScanTasks;
  private int insertFileOffset;
  private long insertRecordOffset;
  private int deleteFileOffset;
  private long deleteRecordOffset;
  private DataTreeNode dataTreeNode;

  public ChangelogSplit(
      Collection<ArcticFileScanTask> insertScanTasks,
      Collection<ArcticFileScanTask> deleteScanTasks,
      int taskIndex) {
    Preconditions.checkArgument(insertScanTasks.size() > 0 || deleteScanTasks.size() > 0);
    this.taskIndex = taskIndex;
    this.insertScanTasks = insertScanTasks;
    this.deleteScanTasks = deleteScanTasks;
    Optional<ArcticFileScanTask> task = insertScanTasks.stream().findFirst();
    PrimaryKeyedFile file = task.isPresent() ? task.get().file() : deleteScanTasks.stream().findFirst().get().file();
    this.dataTreeNode = file.node();
  }

  @Override
  public Integer taskIndex() {
    return taskIndex;
  }

  @Override
  public DataTreeNode dataTreeNode() {
    return dataTreeNode;
  }

  @Override
  public void modifyTreeNode(DataTreeNode expectedNode) {
    Preconditions.checkNotNull(expectedNode);
    this.dataTreeNode = expectedNode;
  }

  @Override
  public void updateOffset(Object[] offsets) {
    Preconditions.checkArgument(offsets.length == 4);
    insertFileOffset = (int) offsets[0];
    insertRecordOffset = (long) offsets[1];
    deleteFileOffset = (int) offsets[2];
    deleteRecordOffset = (long) offsets[3];
  }

  @Override
  public ArcticSplit copy() {
    return new ChangelogSplit(insertScanTasks, deleteScanTasks, taskIndex);
  }

  @Override
  public String splitId() {
    return MoreObjects.toStringHelper(this)
        .add("insertTasks", FileScanTaskUtil.toString(insertScanTasks))
        .add("arcticEquityDeletes", FileScanTaskUtil.toString(deleteScanTasks))
        .toString();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("insertTasks", FileScanTaskUtil.toString(insertScanTasks))
        .add("arcticEquityDeletes", FileScanTaskUtil.toString(deleteScanTasks))
        .add("dataTreeNode", dataTreeNode.toString())
        .toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ChangelogSplit)) {
      return false;
    }
    ChangelogSplit other = (ChangelogSplit) obj;
    return splitId().equals(other.splitId()) &&
        insertFileOffset == other.insertFileOffset &&
        insertRecordOffset == other.insertRecordOffset &&
        deleteFileOffset == other.deleteFileOffset &&
        deleteRecordOffset == other.deleteRecordOffset &&
        taskIndex == other.taskIndex;
  }

  public int insertFileOffset() {
    return insertFileOffset;
  }

  public long insertRecordOffset() {
    return insertRecordOffset;
  }

  public int deleteFileOffset() {
    return deleteFileOffset;
  }

  public long deleteRecordOffset() {
    return deleteRecordOffset;
  }

  public Collection<ArcticFileScanTask> insertTasks() {
    return insertScanTasks;
  }

  public Collection<ArcticFileScanTask> deleteTasks() {
    return deleteScanTasks;
  }
}
