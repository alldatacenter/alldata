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

package com.netease.arctic.flink.read.hybrid.assigner;

import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.data.PrimaryKeyedFile;
import com.netease.arctic.flink.read.hybrid.split.ArcticSplit;
import com.netease.arctic.flink.read.hybrid.split.ArcticSplitState;
import com.netease.arctic.scan.ArcticFileScanTask;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.apache.flink.util.FlinkRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * According to Mark,Index TreeNodes and subtaskId assigning a split to special subtask to read.
 */
public class ShuffleSplitAssigner implements SplitAssigner {
  private static final Logger LOG = LoggerFactory.getLogger(ShuffleSplitAssigner.class);

  private static final long POLL_TIMEOUT = 200;
  private final SplitEnumeratorContext<ArcticSplit> enumeratorContext;

  private int totalParallelism;
  private int totalSplitNum;
  private Long currentMaskOfTreeNode;
  private final Object lock = new Object();

  /**
   * Key is the partition data and file index of the arctic file, Value is flink application subtaskId.
   */
  private final Map<Long, Integer> partitionIndexSubtaskMap;
  /**
   * Key is subtaskId, Value is the queue of unAssigned arctic splits.
   */
  private final Map<Integer, PriorityBlockingQueue<ArcticSplit>> subtaskSplitMap;

  private CompletableFuture<Void> availableFuture;


  public ShuffleSplitAssigner(
      SplitEnumeratorContext<ArcticSplit> enumeratorContext) {
    this.enumeratorContext = enumeratorContext;
    this.totalParallelism = enumeratorContext.currentParallelism();
    this.partitionIndexSubtaskMap = new ConcurrentHashMap<>();
    this.subtaskSplitMap = new ConcurrentHashMap<>();
  }

  public ShuffleSplitAssigner(
      SplitEnumeratorContext<ArcticSplit> enumeratorContext, Collection<ArcticSplitState> splitStates,
      long[] shuffleSplitRelation) {
    this.enumeratorContext = enumeratorContext;
    this.partitionIndexSubtaskMap = new ConcurrentHashMap<>();
    this.subtaskSplitMap = new ConcurrentHashMap<>();
    deserializePartitionIndex(shuffleSplitRelation);
    splitStates.forEach(state -> onDiscoveredSplits(Collections.singleton(state.toSourceSplit())));
  }

  @Override
  public Split getNext() {
    throw new UnsupportedOperationException("ShuffleSplitAssigner couldn't support this operation.");
  }

  @Override
  public Split getNext(int subtaskId) {
    return getNextSplit(subtaskId).map(Split::of).orElseGet(isEmpty() ? Split::unavailable : Split::subtaskUnavailable);
  }

  private Optional<ArcticSplit> getNextSplit(int subTaskId) {
    int currentParallelism = enumeratorContext.currentParallelism();
    if (totalParallelism != currentParallelism) {
      throw new FlinkRuntimeException(
          String.format(
              "Source parallelism has been changed, before parallelism is %s, now is %s",
              totalParallelism, currentParallelism));
    }
    if (subtaskSplitMap.containsKey(subTaskId)) {
      PriorityBlockingQueue<ArcticSplit> queue = subtaskSplitMap.get(subTaskId);

      ArcticSplit arcticSplit = null;
      try {
        arcticSplit = queue.poll(POLL_TIMEOUT, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        LOG.warn("interruptedException", e);
      }
      if (arcticSplit == null) {
        LOG.debug("Subtask {}, couldn't retrieve arctic source split in the queue.", subTaskId);
        return Optional.empty();
      } else {
        LOG.info("get next arctic split taskIndex {}, totalSplitNum {}, arcticSplit {}.",
            arcticSplit.taskIndex(), totalSplitNum, arcticSplit);
        return Optional.of(arcticSplit);
      }
    } else {
      LOG.debug("Subtask {}, it's an idle subtask due to the empty queue with this subtask.", subTaskId);
      return Optional.empty();
    }
  }

  @Override
  public void onDiscoveredSplits(Collection<ArcticSplit> splits) {
    splits.forEach(this::putArcticIntoQueue);
    // only complete pending future if new splits are discovered
    completeAvailableFuturesIfNeeded();
  }

  @Override
  public void onUnassignedSplits(Collection<ArcticSplit> splits) {
    onDiscoveredSplits(splits);
  }

  void putArcticIntoQueue(final ArcticSplit split) {
    List<DataTreeNode> exactlyTreeNodes = getExactlyTreeNodes(split);

    PrimaryKeyedFile file = findAnyFileInArcticSplit(split);

    for (DataTreeNode node : exactlyTreeNodes) {
      long partitionIndexKey = Math.abs(file.partition().toString().hashCode() + node.index());
      int subtaskId = partitionIndexSubtaskMap.computeIfAbsent(
          partitionIndexKey, key -> (partitionIndexSubtaskMap.size() + 1) % totalParallelism);
      LOG.info("partition = {}, (mask, index) = ({}, {}), subtaskId = {}",
          file.partition().toString(), node.mask(), node.index(), subtaskId);

      PriorityBlockingQueue<ArcticSplit> queue = subtaskSplitMap.getOrDefault(subtaskId, new PriorityBlockingQueue<>());
      ArcticSplit copiedSplit = split.copy();
      copiedSplit.modifyTreeNode(node);
      LOG.info("put split into queue: {}", copiedSplit);
      queue.add(copiedSplit);
      totalSplitNum = totalSplitNum + 1;
      subtaskSplitMap.put(subtaskId, queue);
    }
  }

  @Override
  public Collection<ArcticSplitState> state() {
    List<ArcticSplitState> arcticSplitStates = new ArrayList<>();
    subtaskSplitMap.forEach((key, value) ->
        arcticSplitStates.addAll(
            value.stream()
                .map(ArcticSplitState::new)
                .collect(Collectors.toList())));

    return arcticSplitStates;
  }

  @Override
  public synchronized CompletableFuture<Void> isAvailable() {
    if (availableFuture == null) {
      availableFuture = new CompletableFuture<>();
    }
    return availableFuture;
  }

  public boolean isEmpty() {
    if (subtaskSplitMap.isEmpty()) {
      return true;
    }
    for (Map.Entry<Integer, PriorityBlockingQueue<ArcticSplit>> entry : subtaskSplitMap.entrySet()) {
      if (!entry.getValue().isEmpty()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void close() throws IOException {
    subtaskSplitMap.clear();
    partitionIndexSubtaskMap.clear();
  }

  public long[] serializePartitionIndex() {
    int prefixParams = 3;
    long[] shuffleSplitRelation = new long[partitionIndexSubtaskMap.size() * 2 + prefixParams];
    shuffleSplitRelation[0] = totalParallelism;
    shuffleSplitRelation[1] = totalSplitNum;
    shuffleSplitRelation[2] = currentMaskOfTreeNode == null ? -1 : currentMaskOfTreeNode;

    int i = prefixParams;
    for (Map.Entry<Long, Integer> entry : partitionIndexSubtaskMap.entrySet()) {
      shuffleSplitRelation[i++] = entry.getKey();
      shuffleSplitRelation[i++] = entry.getValue();
    }
    return shuffleSplitRelation;
  }

  void deserializePartitionIndex(long[] shuffleSplitRelation) {
    int prefixParams = 3;
    this.totalParallelism = (int) shuffleSplitRelation[0];
    this.totalSplitNum = (int) shuffleSplitRelation[1];
    this.currentMaskOfTreeNode = shuffleSplitRelation[2] == -1 ? null : shuffleSplitRelation[2];

    for (int i = prefixParams; i < shuffleSplitRelation.length; i++) {
      partitionIndexSubtaskMap.put(shuffleSplitRelation[i], (int) shuffleSplitRelation[++i]);
    }
  }

  /**
   * <p>
   * |mask=0          o
   * |             /     \
   * |mask=1     o        o
   * |         /   \    /   \
   * |mask=3  o     o  o     o
   * <p>
   * Different data files may locate in different layers when multi snapshots are committed, so arctic source reading
   * should consider emitting the records and keeping ordering. According to the dataTreeNode of the arctic split and
   * the currentMaskOfTreeNode, return the exact tree node list which may move up or go down layers in the arctic tree.
   * </p>
   *
   * @param arcticSplit arctic split.
   * @return the exact tree node list.
   */
  public List<DataTreeNode> getExactlyTreeNodes(ArcticSplit arcticSplit) {
    DataTreeNode dataTreeNode = arcticSplit.dataTreeNode();
    long mask = dataTreeNode.mask();

    synchronized (lock) {
      if (currentMaskOfTreeNode == null) {
        currentMaskOfTreeNode = mask;
      }
    }

    return scanTreeNode(dataTreeNode);
  }

  private List<DataTreeNode> scanTreeNode(DataTreeNode dataTreeNode) {
    long mask = dataTreeNode.mask();
    if (mask == currentMaskOfTreeNode) {
      return Collections.singletonList(dataTreeNode);
    } else if (mask > currentMaskOfTreeNode) {
      // move up one layer
      return scanTreeNode(dataTreeNode.parent());
    } else {
      // go down one layer
      List<DataTreeNode> allNodes = new ArrayList<>();
      allNodes.addAll(scanTreeNode(dataTreeNode.left()));
      allNodes.addAll(scanTreeNode(dataTreeNode.right()));
      return allNodes;
    }
  }

  /**
   * In one arctic split, the partitions, mask and index of the files are the same.
   *
   * @param arcticSplit arctic source split
   * @return anyone primary keyed file in the arcticSplit.
   */
  private PrimaryKeyedFile findAnyFileInArcticSplit(ArcticSplit arcticSplit) {
    AtomicReference<PrimaryKeyedFile> file = new AtomicReference<>();
    if (arcticSplit.isChangelogSplit()) {
      List<ArcticFileScanTask> arcticSplits = new ArrayList<>(arcticSplit.asChangelogSplit().insertTasks());
      arcticSplits.addAll(arcticSplit.asChangelogSplit().deleteTasks());
      arcticSplits.stream().findFirst().ifPresent(task -> file.set(task.file()));
      if (file.get() != null) {
        return file.get();
      }
    }

    List<ArcticFileScanTask> arcticSplits = new ArrayList<>(arcticSplit.asSnapshotSplit().insertTasks());
    arcticSplits.stream().findFirst().ifPresent(task -> file.set(task.file()));
    if (file.get() != null) {
      return file.get();
    }
    throw new FlinkRuntimeException("Couldn't find a primaryKeyedFile.");
  }

  private synchronized void completeAvailableFuturesIfNeeded() {
    if (availableFuture != null && !isEmpty()) {
      availableFuture.complete(null);
    }
    availableFuture = null;
  }
}
