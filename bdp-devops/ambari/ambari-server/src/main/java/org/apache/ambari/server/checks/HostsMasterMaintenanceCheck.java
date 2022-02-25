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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.annotations.UpgradeCheckInfo;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.stack.upgrade.UpgradePack;
import org.apache.ambari.server.stack.upgrade.UpgradePack.ProcessingComponent;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.spi.RepositoryVersion;
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
 * Checks that all hosts in maintenance state do not have master components.
 */
@Singleton
@UpgradeCheckInfo(
    group = UpgradeCheckGroup.MAINTENANCE_MODE,
    order = 5.0f,
    required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED })
public class HostsMasterMaintenanceCheck extends ClusterCheck {

  static final String KEY_NO_UPGRADE_NAME = "no_upgrade_name";
  static final String KEY_NO_UPGRADE_PACK = "no_upgrade_pack";

  static final UpgradeCheckDescription HOSTS_MASTER_MAINTENANCE = new UpgradeCheckDescription("HOSTS_MASTER_MAINTENANCE",
      UpgradeCheckType.HOST,
      "Hosts in Maintenance Mode must not have any master components",
      new ImmutableMap.Builder<String, String>()
        .put(UpgradeCheckDescription.DEFAULT,
            "The following hosts must not be in in Maintenance Mode since they host Master components: {{fails}}.")
        .put(HostsMasterMaintenanceCheck.KEY_NO_UPGRADE_NAME,
            "Could not find suitable upgrade pack for %s %s to version {{version}}.")
        .put(HostsMasterMaintenanceCheck.KEY_NO_UPGRADE_PACK,
            "Could not find upgrade pack named %s.").build());

  /**
   * Constructor.
   */
  public HostsMasterMaintenanceCheck() {
    super(HOSTS_MASTER_MAINTENANCE);
  }

  @Override
  public UpgradeCheckResult perform(UpgradeCheckRequest request) throws AmbariException {
    UpgradeCheckResult result = new UpgradeCheckResult(this);

    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    RepositoryVersion repositoryVersion = request.getTargetRepositoryVersion();
    final StackId stackId = new StackId(repositoryVersion.getStackId());
    final Set<String> hostsWithMasterComponent = new HashSet<>();

    final String upgradePackName = repositoryVersionHelper.get().getUpgradePackageName(
        stackId.getStackName(), stackId.getStackVersion(), repositoryVersion.getVersion(), null);

    if (upgradePackName == null) {
      result.setStatus(UpgradeCheckStatus.FAIL);
      String fail = getFailReason(KEY_NO_UPGRADE_NAME, result, request);
      result.setFailReason(String.format(fail, stackId.getStackName(), stackId.getStackVersion()));
      return result;
    }

    final UpgradePack upgradePack = ambariMetaInfo.get().getUpgradePacks(stackId.getStackName(), stackId.getStackVersion()).get(upgradePackName);
    if (upgradePack == null) {
      result.setStatus(UpgradeCheckStatus.FAIL);
      String fail = getFailReason(KEY_NO_UPGRADE_PACK, result, request);
      result.setFailReason(String.format(fail, upgradePackName));
      return result;
    }

    final Set<String> componentsFromUpgradePack = new HashSet<>();
    for (Map<String, ProcessingComponent> task: upgradePack.getTasks().values()) {
      componentsFromUpgradePack.addAll(task.keySet());
    }

    for (Service service: cluster.getServices().values()) {
      for (ServiceComponent serviceComponent: service.getServiceComponents().values()) {
        if (serviceComponent.isMasterComponent() && componentsFromUpgradePack.contains(serviceComponent.getName())) {
          hostsWithMasterComponent.addAll(serviceComponent.getServiceComponentHosts().keySet());
        }
      }
    }

    final Map<String, Host> clusterHosts = clustersProvider.get().getHostsForCluster(clusterName);
    for (Map.Entry<String, Host> hostEntry : clusterHosts.entrySet()) {
      final Host host = hostEntry.getValue();
      if (host.getMaintenanceState(cluster.getClusterId()) == MaintenanceState.ON && hostsWithMasterComponent.contains(host.getHostName())) {
        result.getFailedOn().add(host.getHostName());

        result.getFailedDetail().add(
            new HostDetail(host.getHostId(), host.getHostName()));
      }
    }

    if (!result.getFailedOn().isEmpty()) {
      result.setStatus(UpgradeCheckStatus.FAIL);
      result.setFailReason(getFailReason(result, request));
    }

    return result;
  }
}
