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


import static java.util.Arrays.asList;
import static org.apache.ambari.server.state.AlertState.CRITICAL;
import static org.apache.ambari.server.state.AlertState.WARNING;

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.annotations.UpgradeCheckInfo;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.spi.upgrade.UpgradeCheckDescription;
import org.apache.ambari.spi.upgrade.UpgradeCheckGroup;
import org.apache.ambari.spi.upgrade.UpgradeCheckRequest;
import org.apache.ambari.spi.upgrade.UpgradeCheckResult;
import org.apache.ambari.spi.upgrade.UpgradeCheckStatus;
import org.apache.ambari.spi.upgrade.UpgradeCheckType;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonProperty;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Checks that there are no WARNING/CRITICAL alerts on current cluster.
 * That is a potential problem when doing stack update.
 */
@Singleton
@UpgradeCheckInfo(
    group = UpgradeCheckGroup.DEFAULT,
    required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED })
public class HealthCheck extends ClusterCheck {

  private static final List<AlertState> ALERT_STATES = asList(WARNING, CRITICAL);

  @Inject
  Provider<AlertsDAO> alertsDAOProvider;

  static final UpgradeCheckDescription HEALTH = new UpgradeCheckDescription("HEALTH",
      UpgradeCheckType.CLUSTER,
      "Cluster Health",
      new ImmutableMap.Builder<String, String>()
        .put(UpgradeCheckDescription.DEFAULT,
            "The following issues have been detected on this cluster and should be addressed before upgrading: %s").build());

  /**
   * Constructor.
   */
  public HealthCheck() {
    super(HEALTH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UpgradeCheckResult perform(UpgradeCheckRequest request)
      throws AmbariException {
    UpgradeCheckResult result = new UpgradeCheckResult(this);

    AlertsDAO alertsDAO = alertsDAOProvider.get();
    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    List<AlertCurrentEntity> alerts = alertsDAO.findCurrentByCluster(cluster.getClusterId());

    List<String> errorMessages = new ArrayList<>();

    for (AlertCurrentEntity alert : alerts) {
      AlertHistoryEntity alertHistory = alert.getAlertHistory();
      AlertState alertState = alertHistory.getAlertState();
      if (ALERT_STATES.contains(alertState) && !alert.getMaintenanceState().equals(MaintenanceState.ON)) {
        String state = alertState.name();
        String label = alertHistory.getAlertDefinition().getLabel();
        String hostName = alertHistory.getHostName();

        if (hostName == null) {
          errorMessages.add(state + ": " + label);
        } else {
          errorMessages.add(state + ": " + label + ": " + hostName);
        }
        result.getFailedDetail().add(new AlertDetail(state, label, hostName));
      }
    }

    if (!errorMessages.isEmpty()) {
      result.getFailedOn().add(clusterName);
      result.setStatus(UpgradeCheckStatus.WARNING);
      String failReason = getFailReason(result, request);
      result.setFailReason(
          String.format(failReason, StringUtils.join(errorMessages, System.lineSeparator())));
    }

    return result;
  }

  /**
   * Used to represent specific detail about alert.
   */
  private static class AlertDetail {
    @JsonProperty("state")
    public String state;

    @JsonProperty("label")
    public String label;

    @JsonProperty("host_name")
    public String hostName;

    AlertDetail(String state, String label, String hostName) {
      this.state = state;
      this.label = label;
      this.hostName = hostName;
    }
  }
}
