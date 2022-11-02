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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.annotations.UpgradeCheckInfo;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.models.HostComponentSummary;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.State;
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
 * Checks that service components are installed.
 */
@Singleton
@UpgradeCheckInfo(
    group = UpgradeCheckGroup.LIVELINESS,
    order = 2.0f,
    required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED })
public class ComponentsInstallationCheck extends ClusterCheck {

  static final UpgradeCheckDescription COMPONENTS_INSTALLATION = new UpgradeCheckDescription("COMPONENTS_INSTALLATION",
      UpgradeCheckType.SERVICE,
      "All service components must be installed",
      new ImmutableMap.Builder<String, String>()
        .put(UpgradeCheckDescription.DEFAULT,
            "The following Services must be reinstalled: {{fails}}. Try to reinstall the service components in INSTALL_FAILED state.").build());

  /**
   * Constructor.
   */
  public ComponentsInstallationCheck() {
    super(COMPONENTS_INSTALLATION);
  }

  @Override
  public UpgradeCheckResult perform(UpgradeCheckRequest request) throws AmbariException {
    UpgradeCheckResult result = new UpgradeCheckResult(this);

    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    Set<String> failedServiceNames = new HashSet<>();

    // Preq-req check should fail if any service component is in INSTALL_FAILED state
    Set<String> installFailedHostComponents = new HashSet<>();

    Set<String> servicesInUpgrade = checkHelperProvider.get().getServicesInUpgrade(request);
    for (String serviceName : servicesInUpgrade) {
      final Service service = cluster.getService(serviceName);
      // Skip service if it is in maintenance mode
      if (service.getMaintenanceState() == MaintenanceState.ON) {
        continue;
      }

      Map<String, ServiceComponent> serviceComponents = service.getServiceComponents();
      for (Map.Entry<String, ServiceComponent> component : serviceComponents.entrySet()) {
        ServiceComponent serviceComponent = component.getValue();
        if (serviceComponent.isVersionAdvertised()) {
          List<HostComponentSummary> hostComponentSummaries = HostComponentSummary.getHostComponentSummaries(
              service.getName(), serviceComponent.getName());

          for (HostComponentSummary hcs : hostComponentSummaries) {
            // Skip host if it is in maintenance mode
            Host host = clustersProvider.get().getHost(hcs.getHostName());
            if (host.getMaintenanceState(cluster.getClusterId()) != MaintenanceState.ON) {
              if (hcs.getCurrentState() == State.INSTALL_FAILED) {

                result.getFailedDetail().add(hcs);

                failedServiceNames.add(service.getName());
                installFailedHostComponents.add(MessageFormat.format("[{0}:{1} on {2}]",
                    service.getName(), serviceComponent.getName(), hcs.getHostName()));
              }
            }
          }
        }
      }
    }

    if(!installFailedHostComponents.isEmpty()) {
      String message = MessageFormat.format("Service components in INSTALL_FAILED state: {0}.",
          StringUtils.join(installFailedHostComponents, ", "));

      result.setFailedOn(new LinkedHashSet<>(failedServiceNames));
      result.setStatus(UpgradeCheckStatus.FAIL);
      result.setFailReason(
          "Found service components in INSTALL_FAILED state. Please re-install these components. " + message);
    }

    return result;
  }
}
