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

import java.text.MessageFormat;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.ambari.annotations.UpgradeCheckInfo;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.spi.RepositoryVersion;
import org.apache.ambari.spi.upgrade.UpgradeCheckDescription;
import org.apache.ambari.spi.upgrade.UpgradeCheckGroup;
import org.apache.ambari.spi.upgrade.UpgradeCheckRequest;
import org.apache.ambari.spi.upgrade.UpgradeCheckResult;
import org.apache.ambari.spi.upgrade.UpgradeCheckStatus;
import org.apache.ambari.spi.upgrade.UpgradeCheckType;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.apache.commons.lang.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;

/**
 * Checks if Install Packages needs to be re-run
 */
@Singleton
@UpgradeCheckInfo(
    group = UpgradeCheckGroup.DEFAULT,
    order = 3.0f,
    required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED })
public class InstallPackagesCheck extends ClusterCheck {

  static final UpgradeCheckDescription INSTALL_PACKAGES_CHECK = new UpgradeCheckDescription("INSTALL_PACKAGES_CHECK",
      UpgradeCheckType.CLUSTER,
      "Install packages must be re-run",
      new ImmutableMap.Builder<String, String>()
        .put(UpgradeCheckDescription.DEFAULT,
            "Re-run Install Packages before starting upgrade").build());

  /**
   * Constructor.
   */
  public InstallPackagesCheck() {
    super(INSTALL_PACKAGES_CHECK);
  }

  @Override
  public UpgradeCheckResult perform(UpgradeCheckRequest request) throws AmbariException {
    UpgradeCheckResult result = new UpgradeCheckResult(this);

    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    RepositoryVersion repositoryVersion = request.getTargetRepositoryVersion();

    final StackId targetStackId = new StackId(repositoryVersion.getStackId());

    if (!repositoryVersion.getVersion().matches("^\\d+(\\.\\d+)*\\-\\d+$")) {
      String message = MessageFormat.format(
          "The Repository Version {0} for Stack {1} must contain a \"-\" followed by a build number. "
              + "Make sure that another registered repository does not have the same repo URL or "
              + "shares the same build number. Next, try reinstalling the Repository Version.",
          repositoryVersion.getVersion(), targetStackId.getStackVersion());

      result.getFailedOn().add("Repository Version " + repositoryVersion.getVersion());
      result.setStatus(UpgradeCheckStatus.FAIL);
      result.setFailReason(message);
      return result;
    }

    final Set<HostDetail> failedHosts = new TreeSet<>();

    for (Host host : cluster.getHosts()) {
      if (host.getMaintenanceState(cluster.getClusterId()) != MaintenanceState.ON) {
        for (HostVersionEntity hve : hostVersionDaoProvider.get().findByHost(host.getHostName())) {
          if (StringUtils.equals(hve.getRepositoryVersion().getVersion(), repositoryVersion.getVersion())
              && hve.getState() == RepositoryVersionState.INSTALL_FAILED) {

            failedHosts.add(new HostDetail(host.getHostId(), host.getHostName()));
          }
        }
      }
    }

    if (!failedHosts.isEmpty()) {
      String message = MessageFormat.format("Hosts in cluster [{0},{1},{2},{3}] are in INSTALL_FAILED state because " +
              "Install Packages had failed. Please re-run Install Packages, if necessary place following hosts " +
              "in Maintenance mode: {4}", cluster.getClusterName(), targetStackId.getStackName(),
          targetStackId.getStackVersion(), repositoryVersion.getVersion(),
          StringUtils.join(failedHosts, ", "));

      LinkedHashSet<String> failedHostNames = failedHosts.stream().map(
          failedHost -> failedHost.hostName).collect(
              Collectors.toCollection(LinkedHashSet::new));

      result.setFailedOn(failedHostNames);
      result.setStatus(UpgradeCheckStatus.FAIL);
      result.setFailReason(message);
      result.getFailedDetail().addAll(failedHosts);

      return result;
    }

    result.setStatus(UpgradeCheckStatus.PASS);
    return result;
  }
}
