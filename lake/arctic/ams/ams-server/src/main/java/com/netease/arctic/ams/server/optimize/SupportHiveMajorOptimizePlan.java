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

import com.google.common.base.Preconditions;
import com.netease.arctic.ams.server.model.TableOptimizeRuntime;
import com.netease.arctic.ams.server.model.TaskConfig;
import com.netease.arctic.hive.table.SupportHive;
import com.netease.arctic.hive.utils.TableTypeUtil;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.utils.CompatiblePropertyUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileScanTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SupportHiveMajorOptimizePlan extends MajorOptimizePlan {
  private static final Logger LOG = LoggerFactory.getLogger(SupportHiveMajorOptimizePlan.class);
  // files in locations don't need to major optimize
  private final String hiveLocation;
  private final Set<String> partitionsHasPosDelete = new HashSet<>();

  public SupportHiveMajorOptimizePlan(ArcticTable arcticTable, TableOptimizeRuntime tableOptimizeRuntime,
                                      List<FileScanTask> baseFileScanTasks,
                                      int queueId, long currentTime,
                                      long baseSnapshotId) {
    super(arcticTable, tableOptimizeRuntime, baseFileScanTasks,
        queueId, currentTime, baseSnapshotId);

    Preconditions.checkArgument(TableTypeUtil.isHive(arcticTable), "The table not support hive");
    this.hiveLocation = (((SupportHive) arcticTable).hiveLocation());
  }

  @Override
  public boolean partitionNeedPlan(String partitionToPath) {
    long current = System.currentTimeMillis();

    List<DeleteFile> posDeleteFiles = getPosDeleteFilesFromFileTree(partitionToPath);
    List<DataFile> baseFiles = getBaseFilesFromFileTree(partitionToPath);
    List<DataFile> smallFiles = filterSmallFiles(baseFiles);

    // check whether partition need plan by files info.
    // if partition has no pos-delete file, and there are files in not hive location, need plan
    // if partition has pos-delete, and there are small file count greater than 2 in not hive location, need plan
    boolean hasPos = CollectionUtils.isNotEmpty(posDeleteFiles) && smallFiles.size() >= 2;
    boolean noPos = CollectionUtils.isEmpty(posDeleteFiles) && CollectionUtils.isNotEmpty(baseFiles);
    boolean partitionNeedPlan = hasPos || noPos;
    if (partitionNeedPlan) {
      // check small data file count
      if (checkSmallFileCount(smallFiles)) {
        return true;
      }

      // check major optimize interval
      if (checkMajorOptimizeInterval(current, partitionToPath)) {
        return true;
      }
    }

    LOG.debug("{} ==== don't need {} optimize plan, skip partition {}, partitionNeedPlan is {}",
        tableId(), getOptimizeType(), partitionToPath, partitionNeedPlan);
    return false;
  }

  @Override
  protected boolean checkMajorOptimizeInterval(long current, String partitionToPath) {
    if (current - tableOptimizeRuntime.getLatestMajorOptimizeTime(partitionToPath) >=
        CompatiblePropertyUtil.propertyAsLong(arcticTable.properties(),
            TableProperties.SELF_OPTIMIZING_MAJOR_TRIGGER_INTERVAL,
            TableProperties.SELF_OPTIMIZING_MAJOR_TRIGGER_INTERVAL_DEFAULT)) {
      return true;
    }

    return false;
  }

  @Override
  protected boolean baseFileShouldOptimize(DataFile baseFile, String partition) {
    // if a partition has pos-delete file, only the small base files not in hive location should be optimized,
    // otherwise, all the files not in hive location should be optimized and move to hive location after optimize
    if (notMoveToHiveLocation(partition)) {
      return isSmallFile(baseFile) && notInHiveLocation(baseFile.path().toString());
    } else {
      return notInHiveLocation(baseFile.path().toString());
    }
  }
  
  private boolean notMoveToHiveLocation(String partition) {
    return partitionsHasPosDelete.contains(partition);
  }

  @Override
  protected void addBaseFilesIntoFileTree() {
    UnkeyedTable baseTable = getBaseTable();
    // get partitions has Pos-Delete
    baseFileScanTasks.forEach(task -> {
      DataFile baseFile = task.file();
      String partition = baseTable.spec().partitionToPath(baseFile.partition());
      if (partitionsHasPosDelete.contains(partition)) {
        return;
      }
      List<DeleteFile> deletes = task.deletes();
      if (!deletes.isEmpty()) {
        partitionsHasPosDelete.add(partition);
      }
    });
    super.addBaseFilesIntoFileTree();
  }

  @Override
  protected TaskConfig getTaskConfig(String partition) {
    return new TaskConfig(getOptimizeType(), partition, UUID.randomUUID().toString(), planGroup,
        System.currentTimeMillis(), !notMoveToHiveLocation(partition), null);
  }

  @Override
  protected boolean nodeTaskNeedBuild(String partition, List<DeleteFile> posDeleteFiles, List<DataFile> baseFiles) {
    if (notMoveToHiveLocation(partition)) {
      // if not move to hive location, no need to optimize for only 1 base file, to avoid continuous optimizing
      return baseFiles.size() >= 2;
    } else {
      return true;
    }
  }

  private List<DataFile> filterSmallFiles(List<DataFile> dataFileList) {
    // for support hive table, filter small files
    return dataFileList.stream().filter(this::isSmallFile).collect(Collectors.toList());
  }

  private boolean notInHiveLocation(String filePath) {
    return !filePath.contains(hiveLocation);
  }
}
