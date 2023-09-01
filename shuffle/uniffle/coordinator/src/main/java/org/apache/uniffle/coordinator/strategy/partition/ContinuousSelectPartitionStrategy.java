/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.coordinator.strategy.partition;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.collect.Lists;

import org.apache.uniffle.common.PartitionRange;
import org.apache.uniffle.coordinator.ServerNode;
import org.apache.uniffle.coordinator.util.CoordinatorUtils;

public class ContinuousSelectPartitionStrategy implements SelectPartitionStrategy {
  @Override
  public SortedMap<PartitionRange, List<ServerNode>> assign(
      int totalPartitionNum, int partitionNumPerRange, int replica,
      List<ServerNode> candidatesNodes, int estimateTaskConcurrency) {
    SortedMap<PartitionRange, List<ServerNode>> assignments = new TreeMap<>();
    int serverNum = candidatesNodes.size();
    List<List<PartitionRange>> rangesGroup = CoordinatorUtils.generateRangesGroup(totalPartitionNum,
        partitionNumPerRange, serverNum, estimateTaskConcurrency);

    for (int rc = 0; rc < replica; rc++) {
      for (int i = 0; i < rangesGroup.size(); i++) {
        ServerNode node = candidatesNodes.get((i + rc) % serverNum);
        List<PartitionRange> ranges = rangesGroup.get(i);
        ranges.forEach(range -> {
          List<ServerNode> serverNodes = assignments.computeIfAbsent(range, key -> Lists.newArrayList());
          serverNodes.add(node);
        });
      }
    }
    return assignments;
  }
}
