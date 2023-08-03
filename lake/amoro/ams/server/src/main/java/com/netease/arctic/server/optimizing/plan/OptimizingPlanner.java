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

import com.clearspring.analytics.util.Lists;
import com.netease.arctic.hive.table.SupportHive;
import com.netease.arctic.server.ArcticServiceConstants;
import com.netease.arctic.server.optimizing.OptimizingType;
import com.netease.arctic.server.optimizing.scan.TableFileScanHelper;
import com.netease.arctic.server.table.KeyedTableSnapshot;
import com.netease.arctic.server.table.TableRuntime;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.utils.TableTypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OptimizingPlanner extends OptimizingEvaluator {
  private static final Logger LOG = LoggerFactory.getLogger(OptimizingPlanner.class);

  private static final long MAX_INPUT_FILE_SIZE_PER_THREAD = 5L * 1024 * 1024 * 1024;

  private final TableFileScanHelper.PartitionFilter partitionFilter;

  protected long processId;
  private final double availableCore;
  private final long planTime;
  private OptimizingType optimizingType;
  private final PartitionPlannerFactory partitionPlannerFactory;
  private List<TaskDescriptor> tasks;

  public OptimizingPlanner(TableRuntime tableRuntime, ArcticTable table, double availableCore) {
    super(tableRuntime, table);
    this.partitionFilter = tableRuntime.getPendingInput() == null ?
        null : tableRuntime.getPendingInput().getPartitions()::contains;
    this.availableCore = availableCore;
    this.planTime = System.currentTimeMillis();
    this.processId = Math.max(tableRuntime.getNewestProcessId() + 1, planTime);
    this.partitionPlannerFactory = new PartitionPlannerFactory(arcticTable, tableRuntime, planTime);
  }

  @Override
  protected PartitionEvaluator buildEvaluator(String partitionPath) {
    return partitionPlannerFactory.buildPartitionPlanner(partitionPath);
  }

  public Map<String, Long> getFromSequence() {
    return partitionPlanMap.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> ((AbstractPartitionPlan) e.getValue()).getFromSequence()));
  }

  public Map<String, Long> getToSequence() {
    return partitionPlanMap.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> ((AbstractPartitionPlan) e.getValue()).getToSequence()));
  }

  @Override
  protected TableFileScanHelper.PartitionFilter getPartitionFilter() {
    return partitionFilter;
  }

  public long getTargetSnapshotId() {
    return currentSnapshot.snapshotId();
  }

  public long getTargetChangeSnapshotId() {
    if (currentSnapshot instanceof KeyedTableSnapshot) {
      return ((KeyedTableSnapshot) currentSnapshot).changeSnapshotId();
    } else {
      return ArcticServiceConstants.INVALID_SNAPSHOT_ID;
    }
  }

  @Override
  public boolean isNecessary() {
    if (!super.isNecessary()) {
      return false;
    }
    return !planTasks().isEmpty();
  }

  public List<TaskDescriptor> planTasks() {
    if (this.tasks != null) {
      return this.tasks;
    }
    long startTime = System.nanoTime();

    if (!isInitialized) {
      initEvaluator();
    }
    if (!super.isNecessary()) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("{} === skip planning", tableRuntime.getTableIdentifier());
      }
      return cacheAndReturnTasks(Collections.emptyList());
    }

    List<PartitionEvaluator> evaluators = new ArrayList<>(partitionPlanMap.values());
    evaluators.sort(Comparator.comparing(PartitionEvaluator::getWeight));
    Collections.reverse(evaluators);

    double maxInputSize = MAX_INPUT_FILE_SIZE_PER_THREAD * availableCore;
    List<PartitionEvaluator> inputPartitions = Lists.newArrayList();
    long actualInputSize = 0;
    for (int i = 0; i < evaluators.size() && actualInputSize < maxInputSize; i++) {
      PartitionEvaluator evaluator = evaluators.get(i);
      inputPartitions.add(evaluator);
      if (actualInputSize + evaluator.getCost() < maxInputSize) {
        actualInputSize += evaluator.getCost();
      }
    }

    double avgThreadCost = actualInputSize / availableCore;
    List<TaskDescriptor> tasks = Lists.newArrayList();
    for (PartitionEvaluator evaluator : inputPartitions) {
      tasks.addAll(((AbstractPartitionPlan) evaluator).splitTasks((int) (actualInputSize / avgThreadCost)));
    }
    if (!tasks.isEmpty()) {
      if (evaluators.stream().anyMatch(evaluator -> evaluator.getOptimizingType() == OptimizingType.FULL)) {
        optimizingType = OptimizingType.FULL;
      } else if (evaluators.stream().anyMatch(evaluator -> evaluator.getOptimizingType() == OptimizingType.MAJOR)) {
        optimizingType = OptimizingType.MAJOR;
      } else {
        optimizingType = OptimizingType.MINOR;
      }
    }
    long endTime = System.nanoTime();
    LOG.info("{} finish plan, type = {}, get {} tasks, cost {} ns, {} ms", tableRuntime.getTableIdentifier(),
        getOptimizingType(), tasks.size(), endTime - startTime, (endTime - startTime) / 1_000_000);
    return cacheAndReturnTasks(tasks);
  }

  private List<TaskDescriptor> cacheAndReturnTasks(List<TaskDescriptor> tasks) {
    this.tasks = tasks;
    return this.tasks;
  }

  public long getPlanTime() {
    return planTime;
  }

  public OptimizingType getOptimizingType() {
    return optimizingType;
  }

  public long getProcessId() {
    return processId;
  }

  private static class PartitionPlannerFactory {
    private final ArcticTable arcticTable;
    private final TableRuntime tableRuntime;
    private final String hiveLocation;
    private final long planTime;

    public PartitionPlannerFactory(ArcticTable arcticTable, TableRuntime tableRuntime, long planTime) {
      this.arcticTable = arcticTable;
      this.tableRuntime = tableRuntime;
      this.planTime = planTime;
      if (com.netease.arctic.hive.utils.TableTypeUtil.isHive(arcticTable)) {
        this.hiveLocation = (((SupportHive) arcticTable).hiveLocation());
      } else {
        this.hiveLocation = null;
      }
    }

    public PartitionEvaluator buildPartitionPlanner(String partitionPath) {
      if (TableTypeUtil.isIcebergTableFormat(arcticTable)) {
        return new IcebergPartitionPlan(tableRuntime, arcticTable, partitionPath, planTime);
      } else {
        if (com.netease.arctic.hive.utils.TableTypeUtil.isHive(arcticTable)) {
          return new MixedHivePartitionPlan(tableRuntime, arcticTable, partitionPath, hiveLocation, planTime);
        } else {
          return new MixedIcebergPartitionPlan(tableRuntime, arcticTable, partitionPath, planTime);
        }
      }
    }
  }
}
