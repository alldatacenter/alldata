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

package com.netease.arctic.flink.read;

import com.netease.arctic.flink.read.hybrid.split.ArcticSplit;
import com.netease.arctic.flink.read.hybrid.split.ChangelogSplit;
import com.netease.arctic.scan.ArcticFileScanTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is a group of the partitions and nodes of the arctic table, it can plan different nodes and different partitions
 * into different {@link ArcticSplit}.
 */
public class PartitionAndNodeGroup {
  AtomicInteger splitCount = new AtomicInteger();
  Collection<ArcticFileScanTask> insertTasks;
  Collection<ArcticFileScanTask> deleteTasks;

  public PartitionAndNodeGroup insertFileScanTask(Set<ArcticFileScanTask> insertTasks) {
    this.insertTasks = insertTasks;
    return this;
  }

  public PartitionAndNodeGroup deleteFileScanTask(Set<ArcticFileScanTask> deleteTasks) {
    this.deleteTasks = deleteTasks;
    return this;
  }

  public PartitionAndNodeGroup splitCount(AtomicInteger splitCount) {
    this.splitCount = splitCount;
    return this;
  }

  List<ArcticSplit> planSplits() {
    Map<String, Map<Long, Node>> nodes = new HashMap<>();
    plan(true, nodes);
    plan(false, nodes);

    List<ArcticSplit> splits = new ArrayList<>();

    nodes.values()
        .forEach(indexNodes ->
            indexNodes.values()
                .forEach(node ->
                    splits.add(
                        new ChangelogSplit(node.inserts, node.deletes, splitCount.incrementAndGet()))));
    return splits;
  }

  /**
   * Split the collection of {@link ArcticFileScanTask} into different groups.
   *
   * @param insert if plan insert files or not
   * @param nodes  the key of nodes is partition info which the file located, the value of nodes is hashmap of
   *               arctic tree node id and {@link Node}
   */
  private void plan(boolean insert, Map<String, Map<Long, Node>> nodes) {
    Collection<ArcticFileScanTask> tasks = insert ? insertTasks : deleteTasks;
    if (tasks == null) {
      return;
    }

    tasks.forEach(task -> {
      String partitionKey = task.file().partition().toString();
      Long nodeId = task.file().node().getId();
      Map<Long, Node> indexNodes = nodes.getOrDefault(partitionKey, new HashMap<>());
      Node node = indexNodes.getOrDefault(nodeId, new Node());
      if (insert) {
        node.addInsert(task);
      } else {
        node.addDelete(task);
      }
      indexNodes.put(nodeId, node);
      nodes.put(partitionKey, indexNodes);
    });
  }

  private static class Node {
    List<ArcticFileScanTask> inserts = new ArrayList<>(1);
    List<ArcticFileScanTask> deletes = new ArrayList<>(1);

    void addInsert(ArcticFileScanTask task) {
      inserts.add(task);
    }

    void addDelete(ArcticFileScanTask task) {
      deletes.add(task);
    }
  }

}
