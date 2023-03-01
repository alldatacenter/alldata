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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

import org.apache.uniffle.common.PartitionRange;
import org.apache.uniffle.coordinator.ServerNode;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ContinuousSelectPartitionStrategyTest {
  private final Set<String> tags = Sets.newHashSet("test");

  @Test
  public void test() throws Exception {
    ContinuousSelectPartitionStrategy strategy = new ContinuousSelectPartitionStrategy();

    List<ServerNode> serverNodes = generateServerResource(Lists.newArrayList(20L, 20L, 20L, 20L, 20L));
    SortedMap<PartitionRange, List<ServerNode>> assignments = strategy.assign(100, 2, 2, serverNodes, 20);
    assertEquals(50, assignments.size());
    List<Long> expect = Lists.newArrayList(20L, 20L, 20L, 20L, 20L);
    valid(expect, assignments);

    assignments = strategy.assign(100, 2, 3, serverNodes, 20);
    assertEquals(50, assignments.size());
    expect = Lists.newArrayList(30L, 30L, 30L, 30L, 30L);
    valid(expect, assignments);

    assignments = strategy.assign(100, 2, 2, serverNodes, 4);
    assertEquals(50, assignments.size());
    expect = Lists.newArrayList(20L, 20L, 20L, 20L, 20L);
    valid(expect, assignments);

    assignments = strategy.assign(98, 2, 2, serverNodes, 20);
    assertEquals(49, assignments.size());
    expect = Lists.newArrayList(19L, 20L, 20L, 20L, 19L);
    valid(expect, assignments);

    assignments = strategy.assign(98, 2, 3, serverNodes, 20);
    assertEquals(49, assignments.size());
    expect = Lists.newArrayList(29L, 29L, 30L, 30L, 29L);
    valid(expect, assignments);

    assignments = strategy.assign(98, 2, 3, serverNodes, 4);
    assertEquals(49, assignments.size());
    expect = Lists.newArrayList(29L, 29L, 30L, 30L, 29L);
    valid(expect, assignments);

    assignments = strategy.assign(4, 2, 2, serverNodes, 4);
    assertEquals(2, assignments.size());
    expect = Lists.newArrayList(1L, 2L, 1L);
    valid(expect, assignments);
  }

  private List<ServerNode> generateServerResource(List<Long> resources) {
    List<ServerNode> serverNodes = Lists.newArrayList();
    for (int i = 0; i < resources.size(); i++) {
      ServerNode node = new ServerNode(
          String.valueOf((char) ('a' + i)),
          "127.0.0." + i,
          0,
          10L,
          5L,
          resources.get(i),
          5,
          tags,
          true);
      serverNodes.add(node);
    }
    return serverNodes;
  }

  private void valid(List<Long> expect, SortedMap<PartitionRange, List<ServerNode>> partitionToServerNodes) {
    SortedMap<ServerNode, Integer> serverToPartitionRangeNums = new TreeMap<>(Comparator.comparing(ServerNode::getId));
    partitionToServerNodes.values().stream().flatMap(Collection::stream).forEach(serverNode -> {
      int oldVal = serverToPartitionRangeNums.getOrDefault(serverNode, 0);
      serverToPartitionRangeNums.put(serverNode, oldVal + 1);
    });
    assertEquals(serverToPartitionRangeNums.size(), expect.size());

    int i = 0;
    for (Map.Entry<ServerNode, Integer> entry : serverToPartitionRangeNums.entrySet()) {
      int partitionNum = entry.getValue();
      assertEquals(expect.get(i), partitionNum);
      i++;
    }
  }
}
