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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.PartitionRange;
import org.apache.uniffle.coordinator.ClusterManager;
import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.coordinator.ServerNode;

public class BasicAssignmentStrategy extends AbstractAssignmentStrategy {

  private static final Logger LOG = LoggerFactory.getLogger(BasicAssignmentStrategy.class);

  private ClusterManager clusterManager;

  public BasicAssignmentStrategy(ClusterManager clusterManager, CoordinatorConf conf) {
    super(conf);
    this.clusterManager = clusterManager;
  }

  @Override
  public PartitionRangeAssignment assign(int totalPartitionNum, int partitionNumPerRange,
      int replica, Set<String> requiredTags, int requiredShuffleServerNumber, int estimateTaskConcurrency) {
    int shuffleNodesMax = clusterManager.getShuffleNodesMax();
    int expectedShuffleNodesNum = shuffleNodesMax;
    if (requiredShuffleServerNumber < shuffleNodesMax && requiredShuffleServerNumber > 0) {
      expectedShuffleNodesNum = requiredShuffleServerNumber;
    }
    List<ServerNode> servers = getRequiredServers(requiredTags, expectedShuffleNodesNum);
    if (servers.isEmpty() || servers.size() < replica) {
      return new PartitionRangeAssignment(null);
    }

    SortedMap<PartitionRange, List<ServerNode>> assignments =
        getPartitionAssignment(totalPartitionNum, partitionNumPerRange, replica, servers, estimateTaskConcurrency);

    return new PartitionRangeAssignment(assignments);
  }

  private List<ServerNode> getRequiredServers(Set<String> requiredTags, int expectedNum) {
    List<ServerNode> servers = clusterManager.getServerList(requiredTags);
    // shuffle server update the status according to heartbeat, if every server is in initial status,
    // random the order of list to avoid always pick same nodes
    Collections.shuffle(servers);
    Collections.sort(servers);
    if (expectedNum > servers.size()) {
      LOG.warn("Can't get expected servers [" + expectedNum + "] and found only [" + servers.size() + "]");
      return servers;
    }

    return getCandidateNodes(servers, expectedNum);
  }
}
