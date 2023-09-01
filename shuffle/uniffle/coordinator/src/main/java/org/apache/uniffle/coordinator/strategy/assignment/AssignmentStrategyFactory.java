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

import org.apache.uniffle.coordinator.ClusterManager;
import org.apache.uniffle.coordinator.CoordinatorConf;

public class AssignmentStrategyFactory {

  private CoordinatorConf conf;
  private ClusterManager clusterManager;

  public AssignmentStrategyFactory(CoordinatorConf conf, ClusterManager clusterManager) {
    this.conf = conf;
    this.clusterManager = clusterManager;
  }

  public AssignmentStrategy getAssignmentStrategy() {
    StrategyName strategy = conf.get(CoordinatorConf.COORDINATOR_ASSIGNMENT_STRATEGY);
    if (StrategyName.BASIC == strategy) {
      return new BasicAssignmentStrategy(clusterManager, conf);
    } else if (StrategyName.PARTITION_BALANCE == strategy) {
      return new PartitionBalanceAssignmentStrategy(clusterManager, conf);
    } else {
      throw new UnsupportedOperationException("Unsupported assignment strategy.");
    }
  }

  public enum StrategyName {
    BASIC,
    PARTITION_BALANCE
  }

}
