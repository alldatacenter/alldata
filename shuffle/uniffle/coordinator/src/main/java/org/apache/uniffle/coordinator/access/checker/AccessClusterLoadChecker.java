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

package org.apache.uniffle.coordinator.access.checker;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.config.Reconfigurable;
import org.apache.uniffle.common.config.RssConf;
import org.apache.uniffle.common.util.Constants;
import org.apache.uniffle.coordinator.AccessManager;
import org.apache.uniffle.coordinator.ClusterManager;
import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.coordinator.ServerNode;
import org.apache.uniffle.coordinator.access.AccessCheckResult;
import org.apache.uniffle.coordinator.access.AccessInfo;
import org.apache.uniffle.coordinator.metric.CoordinatorMetrics;

import static org.apache.uniffle.common.util.Constants.ACCESS_INFO_REQUIRED_SHUFFLE_NODES_NUM;

/**
 * AccessClusterLoadChecker use the cluster load metrics including memory and healthy to
 * filter and count available nodes numbers and reject if the number do not reach the threshold.
 */
public class AccessClusterLoadChecker extends AbstractAccessChecker implements Reconfigurable {

  private static final Logger LOG = LoggerFactory.getLogger(AccessClusterLoadChecker.class);

  private final ClusterManager clusterManager;
  private final double memoryPercentThreshold;
  // The hard constraint number of available shuffle servers
  private final int availableServerNumThreshold;
  private volatile int defaultRequiredShuffleServerNumber;

  public AccessClusterLoadChecker(AccessManager accessManager) throws Exception {
    super(accessManager);
    clusterManager = accessManager.getClusterManager();
    CoordinatorConf conf = accessManager.getCoordinatorConf();
    this.memoryPercentThreshold = conf.getDouble(CoordinatorConf.COORDINATOR_ACCESS_LOADCHECKER_MEMORY_PERCENTAGE);
    this.availableServerNumThreshold = conf.getInteger(
        CoordinatorConf.COORDINATOR_ACCESS_LOADCHECKER_SERVER_NUM_THRESHOLD,
        -1
    );
    this.defaultRequiredShuffleServerNumber = conf.get(CoordinatorConf.COORDINATOR_SHUFFLE_NODES_MAX);
  }

  @Override
  public AccessCheckResult check(AccessInfo accessInfo) {
    Set<String> tags = accessInfo.getTags();
    List<ServerNode> servers = clusterManager.getServerList(tags);
    int size = (int) servers.stream().filter(ServerNode::isHealthy).filter(this::checkMemory).count();

    // If the hard constraint number exist, directly check it
    if (availableServerNumThreshold != -1 && size >= availableServerNumThreshold) {
      return new AccessCheckResult(true, Constants.COMMON_SUCCESS_MESSAGE);
    }

    // If the hard constraint is missing, check the available servers number meet the job's required server size
    if (availableServerNumThreshold == -1) {
      String requiredNodesNumRaw = accessInfo.getExtraProperties().get(ACCESS_INFO_REQUIRED_SHUFFLE_NODES_NUM);
      int requiredNodesNum = defaultRequiredShuffleServerNumber;
      if (StringUtils.isNotEmpty(requiredNodesNumRaw) && Integer.parseInt(requiredNodesNumRaw) > 0) {
        requiredNodesNum = Integer.parseInt(requiredNodesNumRaw);
      }
      if (size >= requiredNodesNum) {
        return new AccessCheckResult(true, Constants.COMMON_SUCCESS_MESSAGE);
      }
    }

    String msg = String.format("Denied by AccessClusterLoadChecker accessInfo[%s], "
            + "total %s nodes, %s available nodes, "
            + "memory percent threshold %s, available num threshold %s.",
        accessInfo, servers.size(), size, memoryPercentThreshold, availableServerNumThreshold);
    LOG.warn(msg);
    CoordinatorMetrics.counterTotalLoadDeniedRequest.inc();
    return new AccessCheckResult(false, msg);
  }

  private boolean checkMemory(ServerNode serverNode) {
    double availableMemory = (double) serverNode.getAvailableMemory();
    double total = (double) serverNode.getTotalMemory();
    double availablePercent = availableMemory / (total / 100.0);
    return Double.compare(availablePercent, memoryPercentThreshold) >= 0;
  }

  public ClusterManager getClusterManager() {
    return clusterManager;
  }

  public double getMemoryPercentThreshold() {
    return memoryPercentThreshold;
  }

  public int getAvailableServerNumThreshold() {
    return availableServerNumThreshold;
  }

  public void close() {
  }

  @Override
  public void reconfigure(RssConf conf) {
    int nodeMax = conf.get(CoordinatorConf.COORDINATOR_SHUFFLE_NODES_MAX);
    if (nodeMax != defaultRequiredShuffleServerNumber) {
      LOG.warn("Coordinator update new defaultRequiredShuffleServerNumber " + nodeMax);
      defaultRequiredShuffleServerNumber = nodeMax;
    }
  }

  @Override
  public boolean isPropertyReconfigurable(String property) {
    return CoordinatorConf.COORDINATOR_SHUFFLE_NODES_MAX.key().equals(property);
  }
}
