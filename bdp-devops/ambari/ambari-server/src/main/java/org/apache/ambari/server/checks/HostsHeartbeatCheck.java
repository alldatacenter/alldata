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
package org.apache.ambari.server.checks;

import java.util.Collection;

import org.apache.ambari.annotations.UpgradeCheckInfo;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostHealthStatus.HealthStatus;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.spi.upgrade.UpgradeCheckDescription;
import org.apache.ambari.spi.upgrade.UpgradeCheckGroup;
import org.apache.ambari.spi.upgrade.UpgradeCheckRequest;
import org.apache.ambari.spi.upgrade.UpgradeCheckResult;
import org.apache.ambari.spi.upgrade.UpgradeCheckStatus;
import org.apache.ambari.spi.upgrade.UpgradeCheckType;
import org.apache.ambari.spi.upgrade.UpgradeType;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;

/**
 * Checks that all hosts are heartbeating with the Ambari Server. If there is a
 * host which is not heartbeating, then it must be in maintenance mode to
 * prevent a failure of this check.
 * <p/>
 * This check will return {@link UpgradeCheckStatus#FAIL} if there are hosts not
 * heartbeating and not in maintenance mode.
 *
 * @see HostMaintenanceModeCheck
 */
@Singleton
@UpgradeCheckInfo(
    group = UpgradeCheckGroup.LIVELINESS,
    order = 1.0f,
    required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED })
public class HostsHeartbeatCheck extends ClusterCheck {

  static final UpgradeCheckDescription HOSTS_HEARTBEAT = new UpgradeCheckDescription("HOSTS_HEARTBEAT",
      UpgradeCheckType.HOST,
      "All hosts must be communicating with Ambari. Hosts which are not reachable should be placed in Maintenance Mode.",
      new ImmutableMap.Builder<String, String>()
        .put(UpgradeCheckDescription.DEFAULT,
            "There are hosts which are not communicating with Ambari.").build());

  /**
   * Constructor.
   */
  public HostsHeartbeatCheck() {
    super(HOSTS_HEARTBEAT);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UpgradeCheckResult perform(UpgradeCheckRequest request)
      throws AmbariException {
    UpgradeCheckResult result = new UpgradeCheckResult(this);

    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    Collection<Host> hosts = cluster.getHosts();

    for (Host host : hosts) {
      HealthStatus hostHealth = host.getHealthStatus().getHealthStatus();
      MaintenanceState maintenanceState = host.getMaintenanceState(cluster.getClusterId());
      switch (hostHealth) {
        case UNHEALTHY:
        case UNKNOWN:
          if (maintenanceState == MaintenanceState.OFF) {
            result.getFailedOn().add(host.getHostName());

            result.getFailedDetail().add(
                new HostDetail(host.getHostId(), host.getHostName()));
          }
          break;
        default:
          break;

      }
    }

    // for any hosts unhealthy and NOT in MM mode, fail this check
    if (!result.getFailedOn().isEmpty()) {
      result.setStatus(UpgradeCheckStatus.FAIL);
      result.setFailReason(getFailReason(result, request));
    }

    return result;
  }
}
