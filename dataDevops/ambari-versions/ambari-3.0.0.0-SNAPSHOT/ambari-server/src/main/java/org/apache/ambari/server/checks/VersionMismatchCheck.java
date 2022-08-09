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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ambari.annotations.UpgradeCheckInfo;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.UpgradeState;
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
 * Warns about host components whose upgrade state is VERSION_MISMATCH. Never triggers
 * fail. In failure description, lists actual and expected component versions.
 */
@Singleton
@UpgradeCheckInfo(
    group = UpgradeCheckGroup.COMPONENT_VERSION,
    order = 7.0f,
    required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED })
public class VersionMismatchCheck extends ClusterCheck {

  static final UpgradeCheckDescription VERSION_MISMATCH = new UpgradeCheckDescription("VERSION_MISMATCH",
      UpgradeCheckType.HOST,
      "All components must be reporting the expected version",
      new ImmutableMap.Builder<String, String>()
        .put(UpgradeCheckDescription.DEFAULT,
            "There are components which are not reporting the expected stack version: \n%s").build());

  public VersionMismatchCheck() {
    super(VERSION_MISMATCH);
  }

  @Override
  public UpgradeCheckResult perform(UpgradeCheckRequest request) throws AmbariException {
    UpgradeCheckResult result = new UpgradeCheckResult(this);

    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    Map<String, Service> services = cluster.getServices();
    List<String> errorMessages = new ArrayList<>();
    for (Service service : services.values()) {
      validateService(service, result, errorMessages);
    }

    if (!result.getFailedOn().isEmpty()) {
      result.setStatus(UpgradeCheckStatus.WARNING);
      String failReason = getFailReason(result, request);
      result.setFailReason(String.format(failReason, StringUtils.join(errorMessages, "\n")));
      result.setFailReason(StringUtils.join(errorMessages, "\n"));
    }

    return result;
  }

  /**
   * Iterates over all service components belonging to a service and validates them.
   * @param service
   * @param result
   * @param errorMessages
   * @throws AmbariException
   */
  private void validateService(Service service, UpgradeCheckResult result,
                               List<String> errorMessages) throws AmbariException {
    Map<String, ServiceComponent> serviceComponents = service.getServiceComponents();
    for (ServiceComponent serviceComponent : serviceComponents.values()) {
      validateServiceComponent(serviceComponent, result, errorMessages);
    }
  }

  /**
   * Iterates over all host components belonging to a service component and validates them.
   * @param serviceComponent
   * @param result
   * @param errorMessages
   */
  private void validateServiceComponent(ServiceComponent serviceComponent,
                                        UpgradeCheckResult result, List<String> errorMessages) {
    Map<String, ServiceComponentHost> serviceComponentHosts = serviceComponent.getServiceComponentHosts();
    for (ServiceComponentHost serviceComponentHost : serviceComponentHosts.values()) {
      validateServiceComponentHost(serviceComponent, serviceComponentHost,
          result, errorMessages);
    }
  }

  /**
   * Validates host component. If upgrade state of host component is VERSION_MISMATCH,
   * adds hostname to a Failed On map of prerequisite check, and adds all other
   * host component version details to errorMessages
   * @param serviceComponent
   * @param serviceComponentHost
   * @param result
   * @param errorMessages
   */
  private void validateServiceComponentHost(ServiceComponent serviceComponent,
                                            ServiceComponentHost serviceComponentHost,
                                            UpgradeCheckResult result,
                                            List<String> errorMessages) {
    if (serviceComponentHost.getUpgradeState().equals(UpgradeState.VERSION_MISMATCH)) {
      String hostName = serviceComponentHost.getHostName();
      String serviceComponentName = serviceComponentHost.getServiceComponentName();
      String desiredVersion = serviceComponent.getDesiredVersion();
      String actualVersion = serviceComponentHost.getVersion();

      String message = hostName + "/" + serviceComponentName
          + " desired version: " + desiredVersion
          + ", actual version: " + actualVersion;
      result.getFailedOn().add(hostName);
      errorMessages.add(message);
    }
  }
}
