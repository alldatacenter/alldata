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

import java.util.Set;

import org.apache.ambari.annotations.UpgradeCheckInfo;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
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
 * Checks to ensure that services are not in maintenance mode.
 */
@Singleton
@UpgradeCheckInfo(
    group = UpgradeCheckGroup.MAINTENANCE_MODE,
    order = 6.0f,
    required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED })
public class ServicesMaintenanceModeCheck extends ClusterCheck {

  static final UpgradeCheckDescription SERVICES_MAINTENANCE_MODE = new UpgradeCheckDescription("SERVICES_MAINTENANCE_MODE",
      UpgradeCheckType.SERVICE,
      "No services can be in Maintenance Mode",
      new ImmutableMap.Builder<String, String>()
        .put(UpgradeCheckDescription.DEFAULT,
            "The following Services must not be in Maintenance Mode: {{fails}}.").build());

  /**
   * Constructor.
   */
  public ServicesMaintenanceModeCheck() {
    super(SERVICES_MAINTENANCE_MODE);
  }

  @Override
  public UpgradeCheckResult perform(UpgradeCheckRequest request) throws AmbariException {
    UpgradeCheckResult result = new UpgradeCheckResult(this);

    final Cluster cluster = clustersProvider.get().getCluster(request.getClusterName());
    Set<String> servicesInUpgrade = checkHelperProvider.get().getServicesInUpgrade(request);

    for (String serviceName : servicesInUpgrade) {
      final Service service = cluster.getService(serviceName);
      if (!service.isClientOnlyService() && service.getMaintenanceState() == MaintenanceState.ON) {
        result.getFailedOn().add(service.getName());
      }
    }

    if (!result.getFailedOn().isEmpty()) {
      result.setStatus(UpgradeCheckStatus.FAIL);
      result.setFailReason(getFailReason(result, request));
    }

    return result;
  }
}
