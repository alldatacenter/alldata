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
import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.data.file.FileNameGenerator;
import com.netease.arctic.hive.table.SupportHive;
import com.netease.arctic.hive.utils.TableTypeUtil;
import com.netease.arctic.table.ArcticTable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileScanTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SupportHiveFullOptimizePlan extends FullOptimizePlan {
  private static final Logger LOG = LoggerFactory.getLogger(SupportHiveFullOptimizePlan.class);

  // hive location.
  protected final String hiveLocation;
  // files in locations don't need to major optimize
  protected final Set<String> excludeLocations = new HashSet<>();

  public SupportHiveFullOptimizePlan(ArcticTable arcticTable, TableOptimizeRuntime tableOptimizeRuntime,
                                     List<FileScanTask> baseFileScanTasks, int queueId, long currentTime,
                                     long baseSnapshotId) {
    super(arcticTable, tableOptimizeRuntime, baseFileScanTasks, queueId, currentTime, baseSnapshotId);

    Preconditions.checkArgument(TableTypeUtil.isHive(arcticTable), "The table not support hive");
    hiveLocation = ((SupportHive) arcticTable).hiveLocation();
    excludeLocations.add(hiveLocation);

    this.isCustomizeDir = true;
  }

  @Override
  protected boolean partitionNeedPlan(String partitionToPath) {

    List<DeleteFile> posDeleteFiles = getPosDeleteFilesFromFileTree(partitionToPath);
    List<DataFile> baseFiles = getBaseFilesFromFileTree(partitionToPath);
    Map<DataTreeNode, Long> nodeSmallFileCount = new HashMap<>();
    boolean nodeHaveTwoSmallFiles = false;
    boolean notInHiveFile = false;
    for (DataFile baseFile : baseFiles) {
      boolean inHive = baseFile.path().toString().contains(((SupportHive) arcticTable).hiveLocation());
      if (!inHive) {
        notInHiveFile = true;
        LOG.info("table {} has in not hive location files", arcticTable.id());
        break;
      } else if (baseFile.fileSizeInBytes() <= getSmallFileSize(arcticTable.properties())) {
        DataTreeNode node = FileNameGenerator.parseFileNodeFromFileName(baseFile.path().toString());
        if (nodeSmallFileCount.get(node) != null) {
          nodeHaveTwoSmallFiles = true;
          LOG.info("table {} has greater than 2 small files in (mask:{}, node :{}) in hive location",
              arcticTable.id(), node.mask(), node.index());
          break;
        } else {
          nodeSmallFileCount.put(node, 1L);
        }
      }
    }
    // check whether partition need plan by files info.
    // if partition has no pos-delete file, and there are files in not hive location or
    // small file count greater than 2 in hive location, partition need plan
    // if partition has pos-delete, partition need plan
    boolean partitionNeedPlan =
        CollectionUtils.isNotEmpty(posDeleteFiles) || nodeHaveTwoSmallFiles || notInHiveFile;

    // check position delete file total size
    if (checkPosDeleteTotalSize(partitionToPath) && partitionNeedPlan) {
      return true;
    }

    // check full optimize interval
    if (checkOptimizeInterval(partitionToPath) && partitionNeedPlan) {
      return true;
    }

    LOG.debug("{} ==== don't need {} optimize plan, skip partition {}, partitionNeedPlan is {}",
        tableId(), getOptimizeType(), partitionToPath, partitionNeedPlan);
    return false;
  }

  @Override
  protected boolean nodeTaskNeedBuild(List<DeleteFile> posDeleteFiles, List<DataFile> baseFiles) {
    return true;
  }
}
