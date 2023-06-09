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
import com.netease.arctic.ams.api.OptimizeType;
import com.netease.arctic.ams.api.properties.OptimizeTaskProperties;
import com.netease.arctic.ams.server.model.BasicOptimizeTask;
import com.netease.arctic.ams.server.model.OptimizeTaskRuntime;
import com.netease.arctic.data.file.FileNameGenerator;
import com.netease.arctic.hive.HMSClientPool;
import com.netease.arctic.hive.table.SupportHive;
import com.netease.arctic.hive.utils.HivePartitionUtil;
import com.netease.arctic.hive.utils.HiveTableUtil;
import com.netease.arctic.hive.utils.TableTypeUtil;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.utils.SerializationUtils;
import com.netease.arctic.utils.TableFileUtils;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.types.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SupportHiveCommit extends BasicOptimizeCommit {
  private static final Logger LOG = LoggerFactory.getLogger(SupportHiveCommit.class);

  protected Consumer<OptimizeTaskItem> updateTargetFiles;

  public SupportHiveCommit(
      ArcticTable arcticTable,
      Map<String, List<OptimizeTaskItem>> optimizeTasksToCommit,
      Consumer<OptimizeTaskItem> updateTargetFiles) {
    super(arcticTable, optimizeTasksToCommit);
    Preconditions.checkArgument(TableTypeUtil.isHive(arcticTable), "The table not support hive");
    this.updateTargetFiles = updateTargetFiles;
  }

  @Override
  public boolean commit(long baseSnapshotId) throws Exception {
    LOG.info("{} get tasks to support hive commit for partitions {}", arcticTable.id(),
        optimizeTasksToCommit.keySet());
    HMSClientPool hiveClient = ((SupportHive) arcticTable).getHMSClient();
    Map<String, String> partitionPathMap = new HashMap<>();
    Types.StructType partitionSchema = arcticTable.isUnkeyedTable() ?
        arcticTable.asUnkeyedTable().spec().partitionType() :
        arcticTable.asKeyedTable().baseTable().spec().partitionType();

    optimizeTasksToCommit.forEach((partition, optimizeTaskItems) -> {
      // if major optimize task don't contain pos-delete files in a partition, can rewrite or move data files
      // to hive location
      if (isPartitionMajorOptimizeSupportHive(partition, optimizeTaskItems)) {
        for (OptimizeTaskItem optimizeTaskItem : optimizeTaskItems) {
          OptimizeTaskRuntime optimizeRuntime = optimizeTaskItem.getOptimizeRuntime();
          List<DataFile> targetFiles = optimizeRuntime.getTargetFiles().stream()
              .map(fileByte -> (DataFile) SerializationUtils.toContentFile(fileByte))
              .collect(Collectors.toList());
          long maxTransactionId = targetFiles.stream()
              .mapToLong(dataFile -> FileNameGenerator.parseTransactionId(dataFile.path().toString()))
              .max()
              .orElse(0L);

          List<ByteBuffer> newTargetFiles = new ArrayList<>(targetFiles.size());
          for (DataFile targetFile : targetFiles) {
            if (partitionPathMap.get(partition) == null) {
              List<String> partitionValues =
                  HivePartitionUtil.partitionValuesAsList(targetFile.partition(), partitionSchema);
              String partitionPath;
              if (arcticTable.spec().isUnpartitioned()) {
                try {
                  Table hiveTable = ((SupportHive) arcticTable).getHMSClient().run(client ->
                      client.getTable(arcticTable.id().getDatabase(), arcticTable.id().getTableName()));
                  partitionPath = hiveTable.getSd().getLocation();
                } catch (Exception e) {
                  LOG.error("Get hive table failed", e);
                  break;
                }
              } else {
                String hiveSubdirectory = arcticTable.isKeyedTable() ?
                    HiveTableUtil.newHiveSubdirectory(maxTransactionId) : HiveTableUtil.newHiveSubdirectory();

                Partition p = HivePartitionUtil.getPartition(hiveClient, arcticTable, partitionValues);
                if (p == null) {
                  partitionPath = HiveTableUtil.newHiveDataLocation(((SupportHive) arcticTable).hiveLocation(),
                      arcticTable.spec(), targetFile.partition(), hiveSubdirectory);
                } else {
                  partitionPath = p.getSd().getLocation();
                }
              }
              partitionPathMap.put(partition, partitionPath);
            }

            DataFile finalDataFile = moveTargetFiles(targetFile, partitionPathMap.get(partition));
            newTargetFiles.add(SerializationUtils.toByteBuffer(finalDataFile));
          }

          optimizeRuntime.setTargetFiles(newTargetFiles);
          updateTargetFiles.accept(optimizeTaskItem);
        }
      }
    });

    return super.commit(baseSnapshotId);
  }

  protected boolean isPartitionMajorOptimizeSupportHive(String partition, List<OptimizeTaskItem> optimizeTaskItems) {
    for (OptimizeTaskItem optimizeTaskItem : optimizeTaskItems) {
      BasicOptimizeTask optimizeTask = optimizeTaskItem.getOptimizeTask();
      boolean isMajorTaskSupportHive = optimizeTask.getTaskId().getType() == OptimizeType.Major &&
          optimizeTask.getProperties().containsKey(OptimizeTaskProperties.MOVE_FILES_TO_HIVE_LOCATION);
      if (!isMajorTaskSupportHive) {
        LOG.info("{} is not major task support hive for partitions {}", arcticTable.id(), partition);
        return false;
      }
    }

    LOG.info("{} is major task support hive for partitions {}", arcticTable.id(), partition);
    return true;
  }

  private DataFile moveTargetFiles(DataFile targetFile, String hiveLocation) {
    String oldFilePath = targetFile.path().toString();
    String newFilePath = TableFileUtils.getNewFilePath(hiveLocation, oldFilePath);

    if (!arcticTable.io().exists(newFilePath)) {
      if (!arcticTable.io().exists(hiveLocation)) {
        LOG.debug("{} hive location {} does not exist and need to mkdir before rename", arcticTable.id(), hiveLocation);
        arcticTable.io().mkdirs(hiveLocation);
      }
      arcticTable.io().rename(oldFilePath, newFilePath);
      LOG.debug("{} move file from {} to {}", arcticTable.id(), oldFilePath, newFilePath);
    }

    // org.apache.iceberg.BaseFile.set
    ((StructLike) targetFile).set(1, newFilePath);
    return targetFile;
  }
}
