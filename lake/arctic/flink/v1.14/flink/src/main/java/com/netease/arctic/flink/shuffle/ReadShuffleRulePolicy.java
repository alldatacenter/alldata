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

import com.netease.arctic.table.DistributionHashMode;
import org.apache.flink.api.common.functions.Partitioner;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.table.data.RowData;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Random;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * Shuffle RowData with same key to same subtask, to make sure cdc data with same key in order.
 */
public class ReadShuffleRulePolicy implements ShuffleRulePolicy<RowData, ShuffleKey> {
  private static final Logger LOG = LoggerFactory.getLogger(ReadShuffleRulePolicy.class);

  private final ShuffleHelper helper;

  private final DistributionHashMode distributionHashMode;

  public ReadShuffleRulePolicy(ShuffleHelper helper) {
    this(helper, DistributionHashMode.autoSelect(helper.isPrimaryKeyExist(), helper.isPartitionKeyExist()));
  }

  public ReadShuffleRulePolicy(ShuffleHelper helper,
                               DistributionHashMode distributionHashMode) {
    this.helper = helper;
    this.distributionHashMode = distributionHashMode;
    Preconditions.checkArgument(distributionHashMode != DistributionHashMode.AUTO);
  }

  @Override
  public KeySelector<RowData, ShuffleKey> generateKeySelector() {
    return new PrimaryKeySelector();
  }

  @Override
  public Partitioner<ShuffleKey> generatePartitioner() {
    return new RoundRobinPartitioner(distributionHashMode, helper);
  }

  @Override
  public DistributionHashMode getPolicyType() {
    return distributionHashMode;
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
    private final ShuffleHelper helper;
    private final DistributionHashMode distributionHashMode;
    private Random random = null;

    RoundRobinPartitioner(DistributionHashMode distributionHashMode, ShuffleHelper helper) {
      this.distributionHashMode = distributionHashMode;
      this.helper = helper;
      if (!distributionHashMode.isSupportPartition() && !distributionHashMode.isSupportPrimaryKey()) {
        random = new Random();
      }
    }

    @Override
    public int partition(ShuffleKey key, int numPartitions) {
      if (helper != null) {
        helper.open();
      }
      checkNotNull(key);
      RowData row = checkNotNull(key.getRow());

      Integer pkHashCode = null;
      if (distributionHashMode.isSupportPrimaryKey()) {
        pkHashCode = helper.hashKeyValue(row);
      }
      // shuffle by arctic partition for partitioned table
      Integer partitionHashCode = null;
      if (distributionHashMode.isSupportPartition()) {
        partitionHashCode = helper.hashPartitionValue(row);
      }
      if (pkHashCode != null && partitionHashCode != null) {
        return Math.abs(Objects.hash(pkHashCode, partitionHashCode)) % numPartitions;
      } else if (pkHashCode != null) {
        return pkHashCode % numPartitions;
      } else if (partitionHashCode != null) {
        return partitionHashCode % numPartitions;
      } else {
        return random.nextInt(numPartitions);
      }
    }
  }
}
