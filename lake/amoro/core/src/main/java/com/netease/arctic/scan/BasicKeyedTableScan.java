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

import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.data.DefaultKeyedFile;
import com.netease.arctic.scan.expressions.BasicPartitionEvaluator;
import com.netease.arctic.table.BasicKeyedTable;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.utils.TablePropertyUtil;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.TableScan;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.relocated.com.google.common.collect.ListMultimap;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.relocated.com.google.common.collect.Multimaps;
import org.apache.iceberg.util.BinPacking;
import org.apache.iceberg.util.PropertyUtil;
import org.apache.iceberg.util.StructLikeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Basic implementation of {@link KeyedTableScan}, including the merge-on-read plan logical
 */
public class BasicKeyedTableScan implements KeyedTableScan {
  private static final Logger LOG = LoggerFactory.getLogger(BasicKeyedTableScan.class);

  private final BasicKeyedTable table;
  List<NodeFileScanTask> splitTasks = new ArrayList<>();
  private final Map<StructLike, List<NodeFileScanTask>> fileScanTasks = new HashMap<>();
  private final int lookBack;
  private final long openFileCost;
  private final long splitSize;
  private Double splitTaskByDeleteRatio;
  private Expression expression;

  public BasicKeyedTableScan(BasicKeyedTable table) {
    this.table = table;
    openFileCost = PropertyUtil.propertyAsLong(table.properties(),
        TableProperties.SPLIT_OPEN_FILE_COST, TableProperties.SPLIT_OPEN_FILE_COST_DEFAULT);
    splitSize = PropertyUtil.propertyAsLong(table.properties(),
        TableProperties.SPLIT_SIZE, TableProperties.SPLIT_SIZE_DEFAULT);
    lookBack = PropertyUtil.propertyAsInt(table.properties(),
        TableProperties.SPLIT_LOOKBACK, TableProperties.SPLIT_LOOKBACK_DEFAULT);
  }

  /**
   * Config this scan with filter by the {@link Expression}.
   * For Change Table, only filters related to partition will take effect.
   *
   * @param expr a filter expression
   * @return scan based on this with results filtered by the expression
   */
  @Override
  public KeyedTableScan filter(Expression expr) {
    if (expression == null) {
      expression = expr;
    } else {
      expression = Expressions.and(expr, expression);
    }
    return this;
  }

  @Override
  public CloseableIterable<CombinedScanTask> planTasks() {
    // base file
    CloseableIterable<ArcticFileScanTask> baseFileList;
    baseFileList = planBaseFiles();

    // change file
    CloseableIterable<ArcticFileScanTask> changeFileList;
    if (table.primaryKeySpec().primaryKeyExisted()) {
      changeFileList = planChangeFiles();
    } else {
      changeFileList = CloseableIterable.empty();
    }

    // 1. group files by partition
    Map<StructLike, Collection<ArcticFileScanTask>> partitionedFiles =
        groupFilesByPartition(changeFileList, baseFileList);
    LOG.info("planning table {} need plan partition size {}", table.id(), partitionedFiles.size());
    partitionedFiles.forEach(this::partitionPlan);
    LOG.info("planning table {} partitionPlan end", table.id());
    // 2.split node task (FileScanTask -> FileScanTask List)
    split();
    LOG.info("planning table {} split end", table.id());
    // 3.combine node task (FileScanTask List -> CombinedScanTask)
    return combineNode(CloseableIterable.withNoopClose(splitTasks),
        splitSize, lookBack, openFileCost);
  }

  @Override
  public KeyedTableScan enableSplitTaskByDeleteRatio(double splitTaskByDeleteRatio) {
    this.splitTaskByDeleteRatio = splitTaskByDeleteRatio;
    return this;
  }

  private CloseableIterable<ArcticFileScanTask> planBaseFiles() {
    TableScan scan = table.baseTable().newScan();
    if (this.expression != null) {
      scan = scan.filter(this.expression);
    }
    CloseableIterable<FileScanTask> fileScanTasks = scan.planFiles();
    return CloseableIterable.transform(
        fileScanTasks,
        fileScanTask -> new BasicArcticFileScanTask(DefaultKeyedFile.parseBase(fileScanTask.file()),
            fileScanTask.deletes(), fileScanTask.spec(), expression));
  }

  private CloseableIterable<ArcticFileScanTask> planChangeFiles() {
    StructLikeMap<Long> partitionOptimizedSequence = TablePropertyUtil.getPartitionOptimizedSequence(table);
    StructLikeMap<Long> legacyPartitionMaxTransactionId = TablePropertyUtil.getLegacyPartitionMaxTransactionId(table);
    Expression partitionExpressions = Expressions.alwaysTrue();
    if (expression != null) {
      //Only push down filters related to partition
      partitionExpressions = new BasicPartitionEvaluator(table.spec()).project(expression);
    }

    ChangeTableIncrementalScan changeTableScan = table.changeTable().newScan()
        .fromSequence(partitionOptimizedSequence)
        .fromLegacyTransaction(legacyPartitionMaxTransactionId);

    changeTableScan = (ChangeTableIncrementalScan) changeTableScan.filter(partitionExpressions);

    return CloseableIterable.transform(changeTableScan.planFiles(), s -> (ArcticFileScanTask) s);
  }

  private void split() {
    fileScanTasks.forEach((structLike, fileScanTasks1) -> {
      for (NodeFileScanTask task : fileScanTasks1) {
        if (task.dataTasks().size() < 2) {
          splitTasks.add(task);
          continue;
        }

        if (splitTaskByDeleteRatio != null) {
          long deleteWeight = task.arcticEquityDeletes().stream().mapToLong(s -> s.file().fileSizeInBytes())
              .map(s -> s + openFileCost)
              .sum();

          long dataWeight = task.dataTasks().stream().mapToLong(s -> s.file().fileSizeInBytes())
              .map(s -> s + openFileCost)
              .sum();
          double deleteRatio = deleteWeight * 1.0 / dataWeight;

          if (deleteRatio < splitTaskByDeleteRatio) {
            long targetSize = Math.min(new Double(deleteWeight / splitTaskByDeleteRatio).longValue(), splitSize);
            split(task, targetSize);
            continue;
          }
        }

        if (task.cost() <= splitSize) {
          splitTasks.add(task);
          continue;
        }
        split(task, splitSize);
      }
    });
  }

  private void split(NodeFileScanTask task, long targetSize) {
    CloseableIterable<NodeFileScanTask> tasksIterable =
        splitNode(CloseableIterable.withNoopClose(task.dataTasks()),
            task.arcticEquityDeletes(), targetSize, lookBack, openFileCost);
    splitTasks.addAll(Lists.newArrayList(tasksIterable));
  }

  public CloseableIterable<NodeFileScanTask> splitNode(
      CloseableIterable<ArcticFileScanTask> splitFiles,
      List<ArcticFileScanTask> deleteFiles,
      long splitSize, int lookback, long openFileCost) {
    Function<ArcticFileScanTask, Long> weightFunc = task -> Math.max(task.file().fileSizeInBytes(), openFileCost);
    return CloseableIterable.transform(
        CloseableIterable.combine(
            new BinPacking.PackingIterable<>(splitFiles, splitSize, lookback, weightFunc, true),
            splitFiles),
        datafiles -> packingTask(datafiles, deleteFiles));
  }

  private NodeFileScanTask packingTask(List<ArcticFileScanTask> datafiles, List<ArcticFileScanTask> deleteFiles) {
    // TODO Optimization: Add files in batch
    return new NodeFileScanTask(Stream.concat(datafiles.stream(), deleteFiles.stream()).collect(Collectors.toList()));
  }

  public CloseableIterable<CombinedScanTask> combineNode(
      CloseableIterable<NodeFileScanTask> splitFiles,
      long splitSize, int lookback, long openFileCost) {
    Function<NodeFileScanTask, Long> weightFunc = file -> Math.max(file.cost(), openFileCost);
    return CloseableIterable.transform(
        CloseableIterable.combine(
            new BinPacking.PackingIterable<>(splitFiles, splitSize, lookback, weightFunc, true),
            splitFiles),
        BaseCombinedScanTask::new);
  }

  /**
   * Construct tree node task according to partition
   * 1. Put all files into the node they originally belonged to
   * 2. Find all data nodes, traverse, and find the delete that intersects them
   */
  private void partitionPlan(StructLike partition, Collection<ArcticFileScanTask> keyedTableTasks) {
    Map<DataTreeNode, NodeFileScanTask> nodeFileScanTaskMap = new HashMap<>();
    // planfiles() cannot guarantee the uniqueness of the file,
    // so Set<path> here is used to remove duplicate files
    Set<String> pathSets = new HashSet<>();
    keyedTableTasks.forEach(task -> {
      if (!pathSets.contains(task.file().path().toString())) {
        pathSets.add(task.file().path().toString());
        DataTreeNode treeNode = task.file().node();
        NodeFileScanTask nodeFileScanTask = nodeFileScanTaskMap.getOrDefault(treeNode, new NodeFileScanTask(treeNode));
        nodeFileScanTask.addFile(task);
        nodeFileScanTaskMap.put(treeNode, nodeFileScanTask);
      }
    });

    nodeFileScanTaskMap.forEach((treeNode, nodeFileScanTask) -> {
      if (!nodeFileScanTask.isDataNode()) {
        return;
      }

      nodeFileScanTaskMap.forEach((treeNode1, nodeFileScanTask1) -> {
        if (!treeNode1.equals(treeNode) && (treeNode1.isSonOf(treeNode) || treeNode.isSonOf(treeNode1))) {
          List<ArcticFileScanTask> deletes = nodeFileScanTask1.arcticEquityDeletes().stream()
              .filter(file -> file.file().node().equals(treeNode1))
              .collect(Collectors.toList());

          nodeFileScanTask.addTasks(deletes);
        }
      });
    });

    List<NodeFileScanTask> fileScanTaskList = new ArrayList<>();
    nodeFileScanTaskMap.forEach((treeNode, nodeFileScanTask) -> {
      if (!nodeFileScanTask.isDataNode()) {
        return;
      }
      fileScanTaskList.add(nodeFileScanTask);
    });

    fileScanTasks.put(partition, fileScanTaskList);
  }

  public Map<StructLike, Collection<ArcticFileScanTask>> groupFilesByPartition(
      CloseableIterable<ArcticFileScanTask> changeTasks,
      CloseableIterable<ArcticFileScanTask> baseTasks) {
    ListMultimap<StructLike, ArcticFileScanTask> filesGroupedByPartition
        = Multimaps.newListMultimap(Maps.newHashMap(), Lists::newArrayList);

    try {
      changeTasks.forEach(task -> filesGroupedByPartition.put(task.file().partition(), task));
      baseTasks.forEach(task -> filesGroupedByPartition.put(task.file().partition(), task));
      return filesGroupedByPartition.asMap();
    } finally {
      try {
        changeTasks.close();
        baseTasks.close();
      } catch (IOException e) {
        LOG.warn("Failed to close table scan of {} ", table.id(), e);
      }
    }
  }
}
