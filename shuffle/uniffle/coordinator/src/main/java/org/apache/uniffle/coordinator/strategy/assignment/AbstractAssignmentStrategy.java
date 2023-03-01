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

import java.util.List;
import java.util.SortedMap;

import org.apache.uniffle.common.PartitionRange;
import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.coordinator.ServerNode;
import org.apache.uniffle.coordinator.strategy.host.BasicHostAssignmentStrategy;
import org.apache.uniffle.coordinator.strategy.host.HostAssignmentStrategy;
import org.apache.uniffle.coordinator.strategy.host.MustDiffHostAssignmentStrategy;
import org.apache.uniffle.coordinator.strategy.host.PreferDiffHostAssignmentStrategy;
import org.apache.uniffle.coordinator.strategy.partition.ContinuousSelectPartitionStrategy;
import org.apache.uniffle.coordinator.strategy.partition.RoundSelectPartitionStrategy;
import org.apache.uniffle.coordinator.strategy.partition.SelectPartitionStrategy;

import static org.apache.uniffle.coordinator.CoordinatorConf.COORDINATOR_ASSIGNMENT_HOST_STRATEGY;
import static org.apache.uniffle.coordinator.CoordinatorConf.COORDINATOR_SELECT_PARTITION_STRATEGY;

public abstract class AbstractAssignmentStrategy implements AssignmentStrategy {
  protected final CoordinatorConf conf;
  private HostAssignmentStrategy hostAssignmentStrategy;
  private SelectPartitionStrategy selectPartitionStrategy;

  public AbstractAssignmentStrategy(CoordinatorConf conf) {
    this.conf = conf;
    loadHostAssignmentStrategy();
    loadSelectPartitionStrategy();
  }

  private void loadSelectPartitionStrategy() {
    SelectPartitionStrategyName selectPartitionStrategyName =
        conf.get(COORDINATOR_SELECT_PARTITION_STRATEGY);
    if (selectPartitionStrategyName == SelectPartitionStrategyName.ROUND) {
      selectPartitionStrategy = new RoundSelectPartitionStrategy();
    } else if (selectPartitionStrategyName == SelectPartitionStrategyName.CONTINUOUS) {
      selectPartitionStrategy = new ContinuousSelectPartitionStrategy();
    } else {
      throw new RuntimeException("Unsupported partition assignment strategy:" + selectPartitionStrategyName);
    }
  }

  private void loadHostAssignmentStrategy() {
    HostAssignmentStrategyName hostAssignmentStrategyName = conf.get(COORDINATOR_ASSIGNMENT_HOST_STRATEGY);
    if (hostAssignmentStrategyName == HostAssignmentStrategyName.MUST_DIFF) {
      hostAssignmentStrategy = new MustDiffHostAssignmentStrategy();
    } else if (hostAssignmentStrategyName == HostAssignmentStrategyName.PREFER_DIFF) {
      hostAssignmentStrategy = new PreferDiffHostAssignmentStrategy();
    } else if (hostAssignmentStrategyName == HostAssignmentStrategyName.NONE) {
      hostAssignmentStrategy = new BasicHostAssignmentStrategy();
    } else {
      throw new RuntimeException("Unsupported partition assignment strategy:" + hostAssignmentStrategyName);
    }
  }

  protected List<ServerNode> getCandidateNodes(List<ServerNode> allNodes, int expectNum) {
    return hostAssignmentStrategy.assign(allNodes, expectNum);
  }

  protected SortedMap<PartitionRange, List<ServerNode>> getPartitionAssignment(
      int totalPartitionNum, int partitionNumPerRange, int replica, List<ServerNode> candidatesNodes,
      int estimateTaskConcurrency) {
    return selectPartitionStrategy.assign(totalPartitionNum, partitionNumPerRange, replica,
        candidatesNodes, estimateTaskConcurrency);
  }

  public enum HostAssignmentStrategyName {
    MUST_DIFF,
    PREFER_DIFF,
    NONE
  }

  public enum SelectPartitionStrategyName {
    ROUND,
    CONTINUOUS
  }
}
