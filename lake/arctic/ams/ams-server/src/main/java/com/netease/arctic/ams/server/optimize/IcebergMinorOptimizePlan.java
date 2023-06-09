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
import com.netease.arctic.ams.server.model.TableOptimizeRuntime;
import com.netease.arctic.ams.server.model.TaskConfig;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.utils.CompatiblePropertyUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class IcebergMinorOptimizePlan extends AbstractIcebergOptimizePlan {
  private static final Logger LOG = LoggerFactory.getLogger(IcebergMinorOptimizePlan.class);

  // partition -> FileScanTask
  private final Map<String, List<FileScanTask>> partitionSmallDataFilesTask = new HashMap<>();
  private final Map<String, List<FileScanTask>> partitionBigDataFilesTask = new HashMap<>();
  // partition -> need optimize delete files
  private final Map<String, Set<DeleteFile>> partitionDeleteFiles = new HashMap<>();
  // delete file path -> related data files
  private final Map<String, Set<FileScanTask>> deleteDataFileMap = new HashMap<>();
  // cache partitionSmallFileCnt
  private final Map<String, Integer> partitionSmallFileCnt = new HashMap<>();

  public IcebergMinorOptimizePlan(ArcticTable arcticTable, TableOptimizeRuntime tableOptimizeRuntime,
                                  List<FileScanTask> fileScanTasks,
                                  int queueId, long currentTime, long currentSnapshotId) {
    super(arcticTable, tableOptimizeRuntime, fileScanTasks, queueId, currentTime, currentSnapshotId);
  }

  @Override
  protected void addOptimizeFiles() {
    long smallFileSize = getSmallFileSize(arcticTable.properties());
    for (FileScanTask fileScanTask : fileScanTasks) {
      StructLike partition = fileScanTask.file().partition();
      String partitionPath = arcticTable.asUnkeyedTable().spec().partitionToPath(partition);
      allPartitions.add(partitionPath);
      // add DataFile info
      if (fileScanTask.file().fileSizeInBytes() <= smallFileSize) {
        // collect small data file info
        List<FileScanTask> smallFileScanTasks =
            partitionSmallDataFilesTask.computeIfAbsent(partitionPath, c -> new ArrayList<>());
        smallFileScanTasks.add(fileScanTask);
      } else {
        // collect need optimize delete file info
        if (fileScanTask.deletes().size() > 1) {
          List<FileScanTask> bigFileScanTasks =
              partitionBigDataFilesTask.computeIfAbsent(partitionPath, c -> new ArrayList<>());
          bigFileScanTasks.add(fileScanTask);
        }

        // add DeleteFile info
        for (DeleteFile deleteFile : fileScanTask.deletes()) {
          if (fileScanTask.deletes().size() > 1) {
            Set<DeleteFile> deleteFiles =
                partitionDeleteFiles.computeIfAbsent(partitionPath, c -> new HashSet<>());
            deleteFiles.add(deleteFile);
          }

          String deletePath = deleteFile.path().toString();
          Set<FileScanTask> fileScanTasks =
              deleteDataFileMap.computeIfAbsent(deletePath, c -> new HashSet<>());
          fileScanTasks.add(fileScanTask);
        }
      }
    }
  }

  @Override
  protected boolean partitionNeedPlan(String partitionToPath) {
    int smallFileCount = getPartitionSmallFileCount(partitionToPath);

    int smallFileCountThreshold = CompatiblePropertyUtil.propertyAsInt(arcticTable.properties(),
        TableProperties.SELF_OPTIMIZING_MINOR_TRIGGER_FILE_CNT,
        TableProperties.SELF_OPTIMIZING_MINOR_TRIGGER_FILE_CNT_DEFAULT);
    // partition has greater than 12 small files to optimize(include data files and delete files)
    if (smallFileCount >= smallFileCountThreshold) {
      LOG.info("{} ==== need iceberg minor optimize plan, partition is {}, small file count is {}, threshold is {}",
          tableId(), partitionToPath, smallFileCount, smallFileCountThreshold);
      return true;
    }

    LOG.debug("{} ==== don't need {} optimize plan, skip partition {}", tableId(), getOptimizeType(), partitionToPath);
    return false;
  }

  @Override
  protected long getPartitionWeight(String partitionToPath) {
    return getPartitionSmallFileCount(partitionToPath);
  }

  private int getPartitionSmallFileCount(String partitionToPath) {
    Integer cached = partitionSmallFileCnt.get(partitionToPath);
    if (cached != null) {
      return cached;
    }
    List<FileScanTask> smallDataFileTask = partitionSmallDataFilesTask.getOrDefault(partitionToPath, new ArrayList<>());

    final long smallFileSize = getSmallFileSize(arcticTable.properties());
    Set<DeleteFile> smallDeleteFile = partitionDeleteFiles.getOrDefault(partitionToPath, new HashSet<>()).stream()
        .filter(deleteFile -> deleteFile.fileSizeInBytes() <= smallFileSize)
        .collect(Collectors.toSet());
    for (FileScanTask task : smallDataFileTask) {
      smallDeleteFile.addAll(task.deletes());
    }
    int smallFileCount = smallDataFileTask.size() + smallDeleteFile.size();
    partitionSmallFileCnt.put(partitionToPath, smallFileCount);
    return smallFileCount;
  }

  @Override
  protected OptimizeType getOptimizeType() {
    return OptimizeType.Minor;
  }

  public boolean hasFileToOptimize() {
    return !partitionBigDataFilesTask.isEmpty() || !partitionSmallDataFilesTask.isEmpty();
  }

  @Override
  protected List<BasicOptimizeTask> collectTask(String partition) {
    List<BasicOptimizeTask> collector = new ArrayList<>();
    String commitGroup = UUID.randomUUID().toString();
    long createTime = System.currentTimeMillis();

    TaskConfig taskPartitionConfig = new TaskConfig(OptimizeType.Minor, partition, commitGroup, planGroup, createTime);

    collector.addAll(collectSmallDataFileTask(partition, taskPartitionConfig));
    collector.addAll(collectDeleteFileTask(partition, taskPartitionConfig));

    return collector;
  }

  private List<BasicOptimizeTask> collectSmallDataFileTask(String partition, TaskConfig taskPartitionConfig) {
    List<BasicOptimizeTask> collector = new ArrayList<>();
    List<FileScanTask> smallFileScanTasks = partitionSmallDataFilesTask.get(partition);
    if (CollectionUtils.isEmpty(smallFileScanTasks)) {
      return collector;
    }

    smallFileScanTasks = filterRepeatFileScanTask(smallFileScanTasks);

    List<List<FileScanTask>> packedList = binPackFileScanTask(smallFileScanTasks);

    if (CollectionUtils.isNotEmpty(packedList)) {
      for (List<FileScanTask> fileScanTasks : packedList) {
        List<DataFile> dataFiles = new ArrayList<>();
        List<DeleteFile> eqDeleteFiles = new ArrayList<>();
        List<DeleteFile> posDeleteFiles = new ArrayList<>();
        getOptimizeFile(fileScanTasks, dataFiles, eqDeleteFiles, posDeleteFiles);

        // only return tasks with at least 2 files
        int totalFileCnt = dataFiles.size() + eqDeleteFiles.size() + posDeleteFiles.size();
        if (totalFileCnt > 1) {
          collector.add(buildOptimizeTask(dataFiles, Collections.emptyList(),
              eqDeleteFiles, posDeleteFiles, taskPartitionConfig));
        }
      }
    }

    return collector;
  }

  private List<BasicOptimizeTask> collectDeleteFileTask(String partition, TaskConfig taskPartitionConfig) {
    List<BasicOptimizeTask> collector = new ArrayList<>();
    Set<DeleteFile> needOptimizeDeleteFiles = partitionDeleteFiles.get(partition);
    if (CollectionUtils.isEmpty(needOptimizeDeleteFiles)) {
      return collector;
    }

    List<FileScanTask> allNeedOptimizeTask = new ArrayList<>();
    for (DeleteFile needOptimizeDeleteFile : needOptimizeDeleteFiles) {
      allNeedOptimizeTask.addAll(deleteDataFileMap.get(needOptimizeDeleteFile.path().toString()));
    }

    allNeedOptimizeTask = filterRepeatFileScanTask(allNeedOptimizeTask);
    List<List<FileScanTask>> packedList = binPackFileScanTask(allNeedOptimizeTask);

    if (CollectionUtils.isNotEmpty(packedList)) {
      for (List<FileScanTask> fileScanTasks : packedList) {
        List<DataFile> dataFiles = new ArrayList<>();
        List<DeleteFile> eqDeleteFiles = new ArrayList<>();
        List<DeleteFile> posDeleteFiles = new ArrayList<>();
        getOptimizeFile(fileScanTasks, dataFiles, eqDeleteFiles, posDeleteFiles);

        int totalFileCnt = dataFiles.size() + eqDeleteFiles.size() + posDeleteFiles.size();
        Preconditions.checkArgument(totalFileCnt > 1, "task only have " + totalFileCnt + " files");

        collector.add(buildOptimizeTask(Collections.emptyList(), dataFiles,
            eqDeleteFiles, posDeleteFiles, taskPartitionConfig));
      }
    }

    return collector;
  }
}
