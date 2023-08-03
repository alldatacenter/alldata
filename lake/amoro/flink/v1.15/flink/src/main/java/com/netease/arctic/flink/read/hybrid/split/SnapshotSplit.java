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

/**
 * A snapshot split generated during planning base table.
 */
public class SnapshotSplit extends ArcticSplit {
  private static final long serialVersionUID = 1L;
  private final int taskIndex;
  private final Collection<ArcticFileScanTask> insertScanTasks;
  private int insertFileOffset;
  private long insertRecordOffset;
  private DataTreeNode dataTreeNode;

  public SnapshotSplit(
      Collection<ArcticFileScanTask> insertScanTasks,
      int taskIndex) {
    Preconditions.checkArgument(insertScanTasks.size() > 0);
    this.insertScanTasks = insertScanTasks;
    this.taskIndex = taskIndex;
    PrimaryKeyedFile file = insertScanTasks.stream().findFirst().get().file();
    this.dataTreeNode = DataTreeNode.of(file.node().mask(), file.node().index());
  }

  @Override
  public String splitId() {
    return MoreObjects.toStringHelper(this)
        .add("insertTasks", FileScanTaskUtil.toString(insertScanTasks))
        .toString();
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

  public Collection<ArcticFileScanTask> insertTasks() {
    return insertScanTasks;
  }

  @Override
  public void updateOffset(Object[] offsets) {
    Preconditions.checkArgument(offsets.length == 2);
    insertFileOffset = (int) offsets[0];
    insertRecordOffset = (long) offsets[1];
  }

  @Override
  public ArcticSplit copy() {
    return new SnapshotSplit(insertScanTasks, taskIndex);
  }

  public int insertFileOffset() {
    return insertFileOffset;
  }

  public long insertRecordOffset() {
    return insertRecordOffset;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("insertTasks", FileScanTaskUtil.toString(insertScanTasks))
        .add("dataTreeNode", dataTreeNode.toString())
        .toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SnapshotSplit)) {
      return false;
    }
    SnapshotSplit other = (SnapshotSplit) obj;
    return splitId().equals(other.splitId()) &&
        insertFileOffset == other.insertFileOffset &&
        insertRecordOffset == other.insertRecordOffset &&
        taskIndex == other.taskIndex;
  }
}
