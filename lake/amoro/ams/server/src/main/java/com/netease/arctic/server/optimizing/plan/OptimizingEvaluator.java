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

package com.netease.arctic.server.optimizing.plan;

import com.netease.arctic.hive.table.SupportHive;
import com.netease.arctic.server.optimizing.scan.IcebergTableFileScanHelper;
import com.netease.arctic.server.optimizing.scan.KeyedTableFileScanHelper;
import com.netease.arctic.server.optimizing.scan.TableFileScanHelper;
import com.netease.arctic.server.optimizing.scan.UnkeyedTableFileScanHelper;
import com.netease.arctic.server.table.KeyedTableSnapshot;
import com.netease.arctic.server.table.TableRuntime;
import com.netease.arctic.server.table.TableSnapshot;
import com.netease.arctic.server.utils.IcebergTableUtil;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.utils.TableTypeUtil;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.relocated.com.google.common.base.MoreObjects;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.apache.iceberg.util.StructLikeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class OptimizingEvaluator {
  
  private static final Logger LOG = LoggerFactory.getLogger(OptimizingEvaluator.class);

  protected final ArcticTable arcticTable;
  protected final TableRuntime tableRuntime;
  protected final TableSnapshot currentSnapshot;
  protected boolean isInitialized = false;

  protected Map<String, PartitionEvaluator> partitionPlanMap = Maps.newHashMap();

  public OptimizingEvaluator(TableRuntime tableRuntime, ArcticTable table) {
    this.tableRuntime = tableRuntime;
    this.arcticTable = table;
    this.currentSnapshot = IcebergTableUtil.getSnapshot(table, tableRuntime);
  }

  public ArcticTable getArcticTable() {
    return arcticTable;
  }

  public TableRuntime getTableRuntime() {
    return tableRuntime;
  }

  protected void initEvaluator() {
    long startTime = System.currentTimeMillis();
    TableFileScanHelper tableFileScanHelper;
    if (TableTypeUtil.isIcebergTableFormat(arcticTable)) {
      tableFileScanHelper = new IcebergTableFileScanHelper(arcticTable.asUnkeyedTable(), currentSnapshot.snapshotId());
    } else {
      if (arcticTable.isUnkeyedTable()) {
        tableFileScanHelper =
            new UnkeyedTableFileScanHelper(arcticTable.asUnkeyedTable(), currentSnapshot.snapshotId());
      } else {
        tableFileScanHelper =
            new KeyedTableFileScanHelper(arcticTable.asKeyedTable(), ((KeyedTableSnapshot) currentSnapshot));
      }
    }
    tableFileScanHelper.withPartitionFilter(getPartitionFilter());
    initPartitionPlans(tableFileScanHelper);
    isInitialized = true;
    LOG.info("{} finish initEvaluator and get {} partitions necessary to optimize, cost {} ms",
        arcticTable.id(), partitionPlanMap.size(), System.currentTimeMillis() - startTime);
  }

  protected TableFileScanHelper.PartitionFilter getPartitionFilter() {
    return null;
  }

  private void initPartitionPlans(TableFileScanHelper tableFileScanHelper) {
    PartitionSpec partitionSpec = arcticTable.spec();
    for (TableFileScanHelper.FileScanResult fileScanResult : tableFileScanHelper.scan()) {
      StructLike partition = fileScanResult.file().partition();
      String partitionPath = partitionSpec.partitionToPath(partition);
      PartitionEvaluator evaluator = partitionPlanMap.computeIfAbsent(partitionPath, this::buildEvaluator);
      evaluator.addFile(fileScanResult.file(), fileScanResult.deleteFiles());
    }
    partitionProperty().forEach((partition, properties) -> {
      String partitionToPath = partitionSpec.partitionToPath(partition);
      PartitionEvaluator evaluator = partitionPlanMap.get(partitionToPath);
      if (evaluator != null) {
        evaluator.addPartitionProperties(properties);
      }
    });
    partitionPlanMap.values().removeIf(plan -> !plan.isNecessary());
  }

  private StructLikeMap<Map<String, String>> partitionProperty() {
    if (arcticTable.isKeyedTable()) {
      return arcticTable.asKeyedTable().baseTable().partitionProperty();
    } else {
      return arcticTable.asUnkeyedTable().partitionProperty();
    }
  }

  protected PartitionEvaluator buildEvaluator(String partitionPath) {
    if (TableTypeUtil.isIcebergTableFormat(arcticTable)) {
      return new CommonPartitionEvaluator(tableRuntime, partitionPath, System.currentTimeMillis());
    } else {
      if (com.netease.arctic.hive.utils.TableTypeUtil.isHive(arcticTable)) {
        String hiveLocation = (((SupportHive) arcticTable).hiveLocation());
        return new MixedHivePartitionPlan.MixedHivePartitionEvaluator(tableRuntime, partitionPath, hiveLocation,
            System.currentTimeMillis(), arcticTable.isKeyedTable());
      } else {
        return new MixedIcebergPartitionPlan.MixedIcebergPartitionEvaluator(tableRuntime, partitionPath,
            System.currentTimeMillis(), arcticTable.isKeyedTable());
      }
    }
  }

  public boolean isNecessary() {
    if (!isInitialized) {
      initEvaluator();
    }
    return !partitionPlanMap.isEmpty();
  }

  public PendingInput getPendingInput() {
    if (!isInitialized) {
      initEvaluator();
    }
    return new PendingInput(partitionPlanMap.values());
  }

  public static class PendingInput {

    private final Set<String> partitions = Sets.newHashSet();

    private int dataFileCount = 0;
    private long dataFileSize = 0;
    private int equalityDeleteFileCount = 0;
    private int positionalDeleteFileCount = 0;
    private long positionalDeleteBytes = 0L;
    private long equalityDeleteBytes = 0L;

    public PendingInput(Collection<PartitionEvaluator> evaluators) {
      for (PartitionEvaluator evaluator : evaluators) {
        partitions.add(evaluator.getPartition());
        dataFileCount += evaluator.getFragmentFileCount() + evaluator.getSegmentFileCount();
        dataFileSize += evaluator.getFragmentFileSize() + evaluator.getSegmentFileSize();
        positionalDeleteBytes += evaluator.getPosDeleteFileSize();
        positionalDeleteFileCount += evaluator.getPosDeleteFileCount();
        equalityDeleteBytes += evaluator.getEqualityDeleteFileSize();
        equalityDeleteFileCount += evaluator.getEqualityDeleteFileCount();
      }
    }

    public Set<String> getPartitions() {
      return partitions;
    }

    public int getDataFileCount() {
      return dataFileCount;
    }

    public long getDataFileSize() {
      return dataFileSize;
    }

    public int getEqualityDeleteFileCount() {
      return equalityDeleteFileCount;
    }

    public int getPositionalDeleteFileCount() {
      return positionalDeleteFileCount;
    }

    public long getPositionalDeleteBytes() {
      return positionalDeleteBytes;
    }

    public long getEqualityDeleteBytes() {
      return equalityDeleteBytes;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("partitions", partitions)
          .add("dataFileCount", dataFileCount)
          .add("dataFileSize", dataFileSize)
          .add("equalityDeleteFileCount", equalityDeleteFileCount)
          .add("positionalDeleteFileCount", positionalDeleteFileCount)
          .add("positionalDeleteBytes", positionalDeleteBytes)
          .add("equalityDeleteBytes", equalityDeleteBytes)
          .toString();
    }
  }
}
