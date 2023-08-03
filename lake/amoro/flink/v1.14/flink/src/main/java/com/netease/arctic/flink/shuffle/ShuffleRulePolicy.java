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

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * Policy for shuffle a streamRecord by primary keys
 */
public interface ShuffleRulePolicy<IN, KEY> extends Serializable {

  /**
   * Generate KeySelector
   *
   * @return
   */
  KeySelector<IN, KEY> generateKeySelector();

  /**
   * Generate partitioner
   *
   * @return
   */
  Partitioner<KEY> generatePartitioner();

  /**
   * Get shuffle type.
   *
   * @return ShufflePolicyType
   */
  DistributionHashMode getPolicyType();

  /**
   * Get factor sequence and writer subtask id mapping relationship
   * Key：subtask id
   * Value：treeNodes
   *
   * @return
   */
  default Map<Integer, Set<DataTreeNode>> getSubtaskTreeNodes() {
    return null;
  }
}
