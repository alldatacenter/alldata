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

package com.netease.arctic.scan;

import com.netease.arctic.data.DataFileType;
import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.utils.FileScanTaskUtil;
import org.apache.iceberg.relocated.com.google.common.base.MoreObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of {@link KeyedTableScanTask} with files in one single {@link DataTreeNode}
 */
public class NodeFileScanTask implements KeyedTableScanTask {
  private static final Logger LOG = LoggerFactory.getLogger(NodeFileScanTask.class);

  private List<ArcticFileScanTask> baseTasks = new ArrayList<>();
  private List<ArcticFileScanTask> insertTasks = new ArrayList<>();
  private List<ArcticFileScanTask> deleteFiles = new ArrayList<>();
  private long cost = 0;
  private final long openFileCost = Long.valueOf(TableProperties.SPLIT_OPEN_FILE_COST_DEFAULT);
  private DataTreeNode treeNode;
  private long rowNums = 0;

  public NodeFileScanTask() {
  }

  public NodeFileScanTask(DataTreeNode treeNode) {
    this.treeNode = treeNode;
  }

  public NodeFileScanTask(List<ArcticFileScanTask> allTasks) {
    this.baseTasks = allTasks.stream()
        .filter(t -> t.file().type() == DataFileType.BASE_FILE)
        .collect(Collectors.toList());
    this.insertTasks = allTasks.stream()
        .filter(t -> t.file().type() == DataFileType.INSERT_FILE).collect(Collectors.toList());
    this.deleteFiles = allTasks.stream()
        .filter(t -> t.file().type() == DataFileType.EQ_DELETE_FILE).collect(Collectors.toList());

    Stream.concat(baseTasks.stream(), insertTasks.stream()).forEach(
        task -> {
          cost = cost + Math.max(task.file().fileSizeInBytes(), openFileCost);
          rowNums = rowNums + task.file().recordCount();
        }
    );
  }

  public void setTreeNode(DataTreeNode treeNode) {
    this.treeNode = treeNode;
  }

  public long cost() {
    return cost;
  }

  public long recordCount() {
    return rowNums;
  }

  @Override
  public List<ArcticFileScanTask> baseTasks() {
    return baseTasks;
  }

  @Override
  public List<ArcticFileScanTask> insertTasks() {
    return insertTasks;
  }

  @Override
  public List<ArcticFileScanTask> arcticEquityDeletes() {
    return deleteFiles;
  }

  @Override
  public List<ArcticFileScanTask> dataTasks() {
    return Stream.concat(baseTasks.stream(), insertTasks.stream())
        .collect(Collectors.toList());
  }

  public void addFile(ArcticFileScanTask task) {

    DataFileType fileType = task.fileType();
    if (fileType == null) {
      LOG.warn("file type is null");
      return;
    }
    if (fileType == DataFileType.BASE_FILE || fileType == DataFileType.INSERT_FILE) {
      cost = cost + Math.max(task.file().fileSizeInBytes(), openFileCost);
      rowNums = rowNums + task.file().recordCount();
    }
    switch (fileType) {
      case BASE_FILE:
        baseTasks.add(task);
        break;
      case INSERT_FILE:
        insertTasks.add(task);
        break;
      case EQ_DELETE_FILE:
        deleteFiles.add(task);
        break;
      default:
        LOG.warn("file type is {}, not add in node", fileType);
        // ignore the object
    }
  }

  public void addTasks(List<ArcticFileScanTask> files) {
    files.forEach(this::addFile);
  }

  public Boolean isDataNode() {
    return baseTasks.size() > 0 || insertTasks.size() > 0;
  }

  public Boolean isSameNode(long mask, long index) {
    return treeNode.mask() == mask && treeNode.index() == index;
  }

  public DataTreeNode treeNode() {
    return treeNode;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("\nbaseTasks", FileScanTaskUtil.toString(baseTasks))
        .add("\ninsertTasks", FileScanTaskUtil.toString(insertTasks))
        .add("\ndeleteFiles", FileScanTaskUtil.toString(deleteFiles))
        .toString();
  }
}
