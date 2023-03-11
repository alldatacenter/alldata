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

package com.netease.arctic.flink.shuffle;

import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.table.DistributionHashMode;
import org.apache.flink.api.common.functions.Partitioner;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.table.data.RowData;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.flink.util.Preconditions.checkArgument;
import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * After the primary key value hash is modulated based on concurrency,
 * the row is routed to different subtask write
 * <p>
 */
public class RoundRobinShuffleRulePolicy implements ShuffleRulePolicy<RowData, ShuffleKey> {
  private static final Logger LOG = LoggerFactory.getLogger(RoundRobinShuffleRulePolicy.class);

  private final ShuffleHelper helper;

  private final int downStreamOperatorParallelism;

  private final int fileSplit;

  private int factor = -1;

  private Map<Integer, Set<DataTreeNode>> subtaskTreeNodes;

  private final DistributionHashMode distributionHashMode;

  public RoundRobinShuffleRulePolicy(int downStreamOperatorParallelism,
                                     int fileSplit) {
    this(null, downStreamOperatorParallelism, fileSplit);
  }

  public RoundRobinShuffleRulePolicy(ShuffleHelper helper,
                                     int downStreamOperatorParallelism,
                                     int fileSplit) {
    this(helper, downStreamOperatorParallelism, fileSplit,
        DistributionHashMode.autoSelect(helper.isPrimaryKeyExist(), helper.isPartitionKeyExist()));
  }

  public RoundRobinShuffleRulePolicy(ShuffleHelper helper,
                                     int downStreamOperatorParallelism,
                                     int fileSplit, DistributionHashMode distributionHashMode) {
    this.helper = helper;
    this.downStreamOperatorParallelism = downStreamOperatorParallelism;
    this.fileSplit = fileSplit;
    this.distributionHashMode = distributionHashMode;
    Preconditions.checkArgument(distributionHashMode != DistributionHashMode.NONE);
    Preconditions.checkArgument(distributionHashMode != DistributionHashMode.AUTO);
  }

  @Override
  public KeySelector<RowData, ShuffleKey> generateKeySelector() {
    return new PrimaryKeySelector();
  }

  @Override
  public Partitioner<ShuffleKey> generatePartitioner() {
    getSubtaskTreeNodes();
    return new RoundRobinPartitioner(downStreamOperatorParallelism, factor, distributionHashMode, helper);
  }

  @Override
  public DistributionHashMode getPolicyType() {
    return distributionHashMode;
  }

  @Override
  public Map<Integer, Set<DataTreeNode>> getSubtaskTreeNodes() {
    if (this.subtaskTreeNodes == null) {
      this.subtaskTreeNodes = initSubtaskFactorMap(this.downStreamOperatorParallelism);
      return this.subtaskTreeNodes;
    }
    return this.subtaskTreeNodes;
  }

  /**
   * get factor sequence and writer subtask id mapping relationship
   * Key：subtask id
   * Value：treeNodes
   *
   * @return
   */
  private Map<Integer, Set<DataTreeNode>> initSubtaskFactorMap(final int writerParallelism) {
    Map<Integer, Set<DataTreeNode>> subtaskTreeNodes = new HashMap<>(writerParallelism);
    if (distributionHashMode.isSupportPrimaryKey()) {
      factor = fileSplit;
      // every writer may accept all node data for partitioned table
      if (distributionHashMode.isSupportPartition()) {
        IntStream.range(0, writerParallelism).forEach(subtaskId -> {
          subtaskTreeNodes.put(subtaskId, IntStream.range(0, factor).mapToObj(index ->
              DataTreeNode.of(factor - 1, index)).collect(Collectors.toSet()));
        });
      } else {
        if (factor < writerParallelism) {
          int actualDepth = getActualDepth(writerParallelism);
          factor = (int) Math.pow(2, actualDepth - 1);
        }
        final int finalMask = factor - 1;

        IntStream.range(0, factor).forEach(sequence -> {
          int subtaskId = getSubtaskId(sequence, writerParallelism);
          if (!subtaskTreeNodes.containsKey(subtaskId)) {
            Set<DataTreeNode> treeNodes = new HashSet<>();
            treeNodes.add(DataTreeNode.of(finalMask, sequence));
            subtaskTreeNodes.put(subtaskId, treeNodes);
          } else {
            subtaskTreeNodes.get(subtaskId).add(DataTreeNode.of(finalMask, sequence));
          }
        });
      }
    } else {
      IntStream.range(0, writerParallelism).forEach(subtaskId -> {
        subtaskTreeNodes.put(subtaskId, Sets.newHashSet(DataTreeNode.of(0, 0)));
      });
    }
    subtaskTreeNodes.forEach(
        (subtaskId, treeNodes) -> LOG.info("subtaskId={}, treeNodes={}.", subtaskId, treeNodes));
    return subtaskTreeNodes;
  }

  private static int getActualDepth(int numPartitions) {
    return (int) Math.ceil(Math.log(numPartitions) / Math.log(2)) + 1;
  }

  private static int getSubtaskId(int sequence, int parallelism) {
    return sequence % parallelism;
  }

  /**
   * return ShuffleKey
   */
  static class PrimaryKeySelector implements KeySelector<RowData, ShuffleKey> {
    @Override
    public ShuffleKey getKey(RowData value) throws Exception {
      return new ShuffleKey(value);
    }
  }

  /**
   * Circular polling feed a streamRecord into a special factor node
   */
  static class RoundRobinPartitioner implements Partitioner<ShuffleKey> {
    private final int downStreamOperatorParallelism;
    private final int factor;
    private final ShuffleHelper helper;
    private final DistributionHashMode distributionHashMode;

    RoundRobinPartitioner(int downStreamOperatorParallelism, int factor,
                          DistributionHashMode distributionHashMode, ShuffleHelper helper) {
      this.downStreamOperatorParallelism = downStreamOperatorParallelism;
      this.factor = factor;
      this.distributionHashMode = distributionHashMode;
      this.helper = helper;
    }

    @Override
    public int partition(ShuffleKey key, int numPartitions) {
      if (helper != null) {
        helper.open();
      }
      checkNotNull(key);
      RowData row = checkNotNull(key.getRow());

      checkArgument(
          numPartitions == this.downStreamOperatorParallelism,
          String.format(
              "shuffle arctic record numPartition:%s is diff with writer parallelism:%s.",
              numPartitions,
              this.downStreamOperatorParallelism));
      Integer factorIndex = null;
      if (distributionHashMode.isSupportPrimaryKey()) {
        long pkHashCode = helper.hashKeyValue(row);
        factorIndex = (int) (pkHashCode % this.factor);
      }
      // shuffle by arctic tree node and partition for partitioned table
      Integer partitionHashCode = null;
      if (distributionHashMode.isSupportPartition()) {
        partitionHashCode = helper.hashPartitionValue(row);
      }
      if (factorIndex != null && partitionHashCode != null) {
        return Math.abs(Objects.hash(factorIndex, partitionHashCode)) % numPartitions;
      } else if (factorIndex != null) {
        return factorIndex % numPartitions;
      } else if (partitionHashCode != null) {
        return partitionHashCode % numPartitions;
      } else {
        return 0;
      }
    }
  }
}
