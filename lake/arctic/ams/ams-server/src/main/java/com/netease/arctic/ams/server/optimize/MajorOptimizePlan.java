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

package com.netease.arctic.ams.server.optimize;

import com.netease.arctic.ams.api.OptimizeType;
import com.netease.arctic.ams.server.model.BasicOptimizeTask;
import com.netease.arctic.ams.server.model.FileTree;
import com.netease.arctic.ams.server.model.TableOptimizeRuntime;
import com.netease.arctic.ams.server.model.TaskConfig;
import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.utils.CompatiblePropertyUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.util.BinPacking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MajorOptimizePlan extends AbstractArcticOptimizePlan {
  private static final Logger LOG = LoggerFactory.getLogger(MajorOptimizePlan.class);
  // cache partition base file count
  private final Map<String, Integer> partitionBaseFileCount = new HashMap<>();

  public MajorOptimizePlan(ArcticTable arcticTable, TableOptimizeRuntime tableOptimizeRuntime,
                           List<FileScanTask> baseFileScanTasks,
                           int queueId, long currentTime,
                           long baseSnapshotId) {
    super(arcticTable, tableOptimizeRuntime, Collections.emptyList(), baseFileScanTasks,
        queueId, currentTime, TableOptimizeRuntime.INVALID_SNAPSHOT_ID, baseSnapshotId);
  }

  @Override
  protected OptimizeType getOptimizeType() {
    return OptimizeType.Major;
  }

  @Override
  public boolean partitionNeedPlan(String partitionToPath) {
    // check small data file count
    if (checkBaseFileCount(partitionToPath)) {
      return true;
    }

    // check major optimize interval
    if (checkOptimizeInterval(partitionToPath)) {
      return true;
    }

    LOG.debug("{} ==== don't need {} optimize plan, skip partition {}", tableId(), getOptimizeType(), partitionToPath);
    return false;
  }

  @Override
  protected PartitionWeight getPartitionWeight(String partition) {
    return new MajorPartitionWeight(checkOptimizeInterval(partition), getBaseFileCount(partition));
  }

  protected int getBaseFileCount(String partition) {
    Integer cached = partitionBaseFileCount.get(partition);
    if (cached != null) {
      return cached;
    }

    int baseFileCount = getBaseFilesFromFileTree(partition).size();
    partitionBaseFileCount.put(partition, baseFileCount);
    return baseFileCount;
  }

  protected static class MajorPartitionWeight implements PartitionWeight {

    private final boolean reachInterval;

    private final int baseFileCount;

    public MajorPartitionWeight(boolean reachInterval, int baseFileCount) {
      this.reachInterval = reachInterval;
      this.baseFileCount = baseFileCount;
    }

    @Override
    public int compareTo(PartitionWeight o) {
      MajorPartitionWeight that = (MajorPartitionWeight) o;
      int compare = Boolean.compare(that.reachInterval, this.reachInterval);
      if (compare != 0) {
        return compare;
      }
      return Integer.compare(that.baseFileCount, this.baseFileCount);
    }
  }

  @Override
  protected List<BasicOptimizeTask> collectTask(String partition) {
    List<BasicOptimizeTask> result;
    FileTree treeRoot = partitionFileTree.get(partition);
    if (treeRoot == null) {
      return Collections.emptyList();
    }
    if (arcticTable.isUnkeyedTable()) {
      result = collectUnKeyedTableTasks(partition);
    } else {
      result = collectKeyedTableTasks(partition);
    }

    return result;
  }

  @Override
  protected long getMaxOptimizeInterval() {
    return CompatiblePropertyUtil.propertyAsLong(arcticTable.properties(),
        TableProperties.SELF_OPTIMIZING_MAJOR_TRIGGER_INTERVAL,
        TableProperties.SELF_OPTIMIZING_MAJOR_TRIGGER_INTERVAL_DEFAULT);
  }

  @Override
  protected long getLatestOptimizeTime(String partition) {
    return tableOptimizeRuntime.getLatestMajorOptimizeTime(partition);
  }

  protected boolean checkBaseFileCount(String partition) {
    return getBaseFileCount(partition) >= CompatiblePropertyUtil.propertyAsInt(arcticTable.properties(),
        TableProperties.SELF_OPTIMIZING_MAJOR_TRIGGER_FILE_CNT,
        TableProperties.SELF_OPTIMIZING_MAJOR_TRIGGER_FILE_CNT_DEFAULT);
  }

  @Override
  protected boolean baseFileShouldOptimize(DataFile baseFile, String partition) {
    return isSmallFile(baseFile);
  }

  protected boolean isSmallFile(DataFile dataFile) {
    return dataFile.fileSizeInBytes() < getSmallFileSize(arcticTable.properties());
  }

  protected TaskConfig getTaskConfig(String partition) {
    return new TaskConfig(getOptimizeType(), partition, UUID.randomUUID().toString(), planGroup,
        System.currentTimeMillis());
  }

  private List<BasicOptimizeTask> collectUnKeyedTableTasks(String partition) {
    List<BasicOptimizeTask> collector = new ArrayList<>();
    TaskConfig taskPartitionConfig = getTaskConfig(partition);

    List<DataFile> baseFiles = getBaseFilesFromFileTree(partition);
    List<DeleteFile> posDeleteFiles = getPosDeleteFilesFromFileTree(partition);
    if (nodeTaskNeedBuild(partition, posDeleteFiles, baseFiles)) {
      // for unkeyed table, tasks can be bin-packed
      long taskSize = CompatiblePropertyUtil.propertyAsLong(arcticTable.properties(),
          TableProperties.SELF_OPTIMIZING_TARGET_SIZE,
          TableProperties.SELF_OPTIMIZING_TARGET_SIZE_DEFAULT);
      Long sum = baseFiles.stream().map(DataFile::fileSizeInBytes).reduce(0L, Long::sum);
      int taskCnt = (int) (sum / taskSize) + 1;
      List<List<DataFile>> packed = new BinPacking.ListPacker<DataFile>(taskSize, taskCnt, true)
          .pack(baseFiles, DataFile::fileSizeInBytes);
      for (List<DataFile> files : packed) {
        if (CollectionUtils.isNotEmpty(files)) {
          collector.add(buildOptimizeTask(null,
              Collections.emptyList(), Collections.emptyList(), files, posDeleteFiles, taskPartitionConfig));
        }
      }
    }

    return collector;
  }

  private List<BasicOptimizeTask> collectKeyedTableTasks(String partition) {
    FileTree treeRoot = partitionFileTree.get(partition);
    if (treeRoot == null) {
      return Collections.emptyList();
    }
    List<BasicOptimizeTask> collector = new ArrayList<>();
    TaskConfig taskPartitionConfig = getTaskConfig(partition);
    List<FileTree> subTrees = new ArrayList<>();
    // split tasks
    treeRoot.splitFileTree(subTrees, new SplitIfNoFileExists());
    for (FileTree subTree : subTrees) {
      List<DataFile> baseFiles = new ArrayList<>();
      subTree.collectBaseFiles(baseFiles);
      if (!baseFiles.isEmpty()) {
        List<DeleteFile> posDeleteFiles = new ArrayList<>();
        subTree.collectPosDeleteFiles(posDeleteFiles);
        List<DataTreeNode> sourceNodes = Collections.singletonList(subTree.getNode());
        if (nodeTaskNeedBuild(partition, posDeleteFiles, baseFiles)) {
          collector.add(buildOptimizeTask(sourceNodes,
              Collections.emptyList(), Collections.emptyList(), baseFiles, posDeleteFiles, taskPartitionConfig));
        }
      }
    }

    return collector;
  }

  /**
   * check whether node task need to build
   * 
   * @param partition partition
   * @param posDeleteFiles pos-delete files in node
   * @param baseFiles base files in node
   * @return whether the node task need to build. If true, build task, otherwise skip.
   */
  protected boolean nodeTaskNeedBuild(String partition, List<DeleteFile> posDeleteFiles, List<DataFile> baseFiles) {
    return baseFiles.size() >= 2;
  }
}
