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

package org.apache.uniffle.coordinator.strategy.assignment;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.hadoop.conf.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.uniffle.common.PartitionRange;
import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.coordinator.ServerNode;
import org.apache.uniffle.coordinator.SimpleClusterManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BasicAssignmentStrategyTest {

  Set<String> tags = Sets.newHashSet("test");
  private SimpleClusterManager clusterManager;
  private BasicAssignmentStrategy strategy;
  private int shuffleNodesMax = 7;

  @BeforeEach
  public void setUp() throws Exception {
    CoordinatorConf ssc = new CoordinatorConf();
    ssc.setInteger(CoordinatorConf.COORDINATOR_SHUFFLE_NODES_MAX, shuffleNodesMax);
    clusterManager = new SimpleClusterManager(ssc, new Configuration());
    strategy = new BasicAssignmentStrategy(clusterManager, ssc);
  }

  @AfterEach
  public void tearDown() throws IOException {
    clusterManager.clear();
    clusterManager.close();
  }

  @Test
  public void testAssign() {
    for (int i = 0; i < 20; ++i) {
      clusterManager.add(new ServerNode(String.valueOf(i), "127.0.0." + i, 0, 0, 0,
          20 - i, 0, tags, true));
    }

    PartitionRangeAssignment pra = strategy.assign(100, 10, 2, tags, -1, -1);
    SortedMap<PartitionRange, List<ServerNode>> assignments = pra.getAssignments();
    assertEquals(10, assignments.size());

    for (int i = 0; i < 100; i += 10) {
      assertTrue(assignments.containsKey(new PartitionRange(i, i + 10)));
    }

    int i = 0;
    Iterator<List<ServerNode>> ite = assignments.values().iterator();
    while (ite.hasNext()) {
      List<ServerNode> cur = ite.next();
      assertEquals(2, cur.size());
      assertEquals(String.valueOf(i % shuffleNodesMax), cur.get(0).getId());
      i++;
      assertEquals(String.valueOf(i % shuffleNodesMax), cur.get(1).getId());
      i++;
    }
  }

  @Test
  public void testRandomAssign() {
    for (int i = 0; i < 20; ++i) {
      clusterManager.add(new ServerNode(String.valueOf(i), "127.0.0." + i, 0, 0, 0,
          0, 0, tags, true));
    }
    PartitionRangeAssignment pra = strategy.assign(100, 10, 2, tags, -1, -1);
    SortedMap<PartitionRange, List<ServerNode>> assignments = pra.getAssignments();
    Set<ServerNode> serverNodes1 = Sets.newHashSet();
    for (Map.Entry<PartitionRange, List<ServerNode>> assignment : assignments.entrySet()) {
      serverNodes1.addAll(assignment.getValue());
    }

    pra = strategy.assign(100, 10, 2, tags, -1, -1);
    assignments = pra.getAssignments();
    Set<ServerNode> serverNodes2 = Sets.newHashSet();
    for (Map.Entry<PartitionRange, List<ServerNode>> assignment : assignments.entrySet()) {
      serverNodes2.addAll(assignment.getValue());
    }

    // test for the random node pick, there is a little possibility failed
    assertFalse(serverNodes1.containsAll(serverNodes2));
  }

  @Test
  public void testAssignWithDifferentNodeNum() {
    final ServerNode sn1 = new ServerNode("sn1", "", 0, 0, 0,
        20, 0, tags, true);
    final ServerNode sn2 = new ServerNode("sn2", "", 0, 0, 0,
        10, 0, tags, true);
    final ServerNode sn3 = new ServerNode("sn3", "", 0, 0, 0,
        0, 0, tags, true);

    clusterManager.add(sn1);
    PartitionRangeAssignment pra = strategy.assign(100, 10, 2, tags, -1, -1);
    // nodeNum < replica
    assertNull(pra.getAssignments());

    // nodeNum = replica
    clusterManager.add(sn2);
    pra = strategy.assign(100, 10, 2, tags, -1, -1);
    SortedMap<PartitionRange, List<ServerNode>> assignments = pra.getAssignments();
    Set<ServerNode> serverNodes = Sets.newHashSet();
    for (Map.Entry<PartitionRange, List<ServerNode>> assignment : assignments.entrySet()) {
      serverNodes.addAll(assignment.getValue());
    }
    assertEquals(2, serverNodes.size());
    assertTrue(serverNodes.contains(sn1));
    assertTrue(serverNodes.contains(sn2));

    // nodeNum > replica & nodeNum < shuffleNodesMax
    clusterManager.add(sn3);
    pra = strategy.assign(100, 10, 2, tags, -1, -1);
    assignments = pra.getAssignments();
    serverNodes = Sets.newHashSet();
    for (Map.Entry<PartitionRange, List<ServerNode>> assignment : assignments.entrySet()) {
      serverNodes.addAll(assignment.getValue());
    }
    assertEquals(3, serverNodes.size());
    assertTrue(serverNodes.contains(sn1));
    assertTrue(serverNodes.contains(sn2));
    assertTrue(serverNodes.contains(sn3));
  }

  @Test
  public void testAssignmentShuffleNodesNum() {
    Set<String> serverTags = Sets.newHashSet("tag-1");

    for (int i = 0; i < 20; ++i) {
      clusterManager.add(new ServerNode("t1-" + i, "127.0.0." + i, 0, 0, 0,
          20 - i, 0, serverTags, true));
    }

    /**
     * case1: user specify the illegal shuffle node num(<0)
     * it will use the default shuffle nodes num when having enough servers.
     */
    PartitionRangeAssignment pra = strategy.assign(100, 10, 1, serverTags, -1, -1);
    assertEquals(
        shuffleNodesMax,
        pra.getAssignments()
            .values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet())
            .size()
    );

    /**
     * case2: user specify the illegal shuffle node num(==0)
     * it will use the default shuffle nodes num when having enough servers.
     */
    pra = strategy.assign(100, 10, 1, serverTags, 0, -1);
    assertEquals(
        shuffleNodesMax,
        pra.getAssignments()
            .values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet())
            .size()
    );

    /**
     * case3: user specify the illegal shuffle node num(>default max limitation)
     * it will use the default shuffle nodes num when having enough servers
     */
    pra = strategy.assign(100, 10, 1, serverTags, shuffleNodesMax + 10, -1);
    assertEquals(
        shuffleNodesMax,
        pra.getAssignments()
            .values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet())
            .size()
    );

    /**
     * case4: user specify the legal shuffle node num,
     * it will use the customized shuffle nodes num when having enough servers
     */
    pra = strategy.assign(100, 10, 1, serverTags, shuffleNodesMax - 1, -1);
    assertEquals(
        shuffleNodesMax - 1,
        pra.getAssignments()
            .values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet())
            .size()
    );

    /**
     * case5: user specify the legal shuffle node num, but cluster don't have enough servers,
     * it will return the remaining servers.
     */
    serverTags = Sets.newHashSet("tag-2");
    for (int i = 0; i < shuffleNodesMax - 1; ++i) {
      clusterManager.add(new ServerNode("t2-" + i, "", 0, 0, 0,
          20 - i, 0, serverTags, true));
    }
    pra = strategy.assign(100, 10, 1, serverTags, shuffleNodesMax, -1);
    assertEquals(
        shuffleNodesMax - 1,
        pra.getAssignments()
            .values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet())
            .size()
    );
  }

  @Test
  public void testWithContinuousSelectPartitionStrategy() throws Exception {
    CoordinatorConf ssc = new CoordinatorConf();
    ssc.set(CoordinatorConf.COORDINATOR_SELECT_PARTITION_STRATEGY,
        AbstractAssignmentStrategy.SelectPartitionStrategyName.CONTINUOUS);
    ssc.setInteger(CoordinatorConf.COORDINATOR_SHUFFLE_NODES_MAX, shuffleNodesMax);
    clusterManager = new SimpleClusterManager(ssc, new Configuration());
    strategy = new BasicAssignmentStrategy(clusterManager, ssc);
    List<Long> list = Lists.newArrayList(20L, 20L, 20L, 20L, 20L, 20L, 20L, 20L, 20L, 20L,
        20L, 20L, 20L, 20L, 20L, 20L, 20L, 20L, 20L, 20L);
    updateServerResource(list);
    PartitionRangeAssignment assignment = strategy.assign(100, 1, 2, tags, 5, 20);
    List<Long> expect = Lists.newArrayList(40L, 40L, 40L, 40L, 40L);
    valid(expect, assignment.getAssignments());

    assignment = strategy.assign(28, 1, 2, tags, 5, 20);
    expect = Lists.newArrayList(11L, 12L, 12L, 11L, 10L);
    valid(expect, assignment.getAssignments());

    assignment = strategy.assign(29, 1, 2, tags, 5, 4);
    expect = Lists.newArrayList(11L, 12L, 12L, 12L, 11L);
    valid(expect, assignment.getAssignments());

    assignment = strategy.assign(29, 2, 2, tags, 5, 4);
    expect = Lists.newArrayList(12L, 12L, 12L, 12L, 12L);
    valid(expect, assignment.getAssignments());
  }

  void updateServerResource(List<Long> resources) {
    for (int i = 0; i < resources.size(); i++) {
      ServerNode node = new ServerNode(
          String.valueOf((char)('a' + i)),
          "127.0.0." + i,
          0,
          10L,
          5L,
          resources.get(i),
          5,
          tags,
          true);
      clusterManager.add(node);
    }
  }

  private void valid(List<Long> expect, SortedMap<PartitionRange, List<ServerNode>> partitionToServerNodes) {
    // Unable to match exactly, the order of the server is disordered
    int actualPartitionNum = 0;
    Set<ServerNode> serverNodes = Sets.newHashSet();
    for (Map.Entry<PartitionRange, List<ServerNode>> entry : partitionToServerNodes.entrySet()) {
      PartitionRange range = entry.getKey();
      actualPartitionNum += (range.getEnd() - range.getStart() + 1) * entry.getValue().size();
      serverNodes.addAll(entry.getValue());
    }

    long expectPartitionNum = expect.stream().mapToLong(Long::longValue).sum();
    assertEquals(expect.size(), serverNodes.size());
    assertEquals(expectPartitionNum, actualPartitionNum);
  }
}
