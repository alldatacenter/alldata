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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.spi.ClusterInformation;
import org.apache.ambari.spi.upgrade.UpgradeCheckRequest;
import org.apache.ambari.spi.upgrade.UpgradeCheckResult;
import org.apache.ambari.spi.upgrade.UpgradeCheckStatus;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Provider;


public class HealthCheckTest {
  private static final String CLUSTER_NAME = "cluster1";
  private static final long CLUSTER_ID = 1L;
  private static final String ALERT_HOSTNAME = "some hostname 1";
  private static final String ALERT_DEFINITION_LABEL = "label 1";
  private HealthCheck healthCheck;
  private AlertsDAO alertsDAO = mock(AlertsDAO.class);

  @Before
  public void setUp() throws Exception {
    final Clusters clusters = mock(Clusters.class);

    healthCheck = new HealthCheck();
    healthCheck.alertsDAOProvider = new Provider<AlertsDAO>() {
      @Override
      public AlertsDAO get() {
        return alertsDAO;
      }
    };

    healthCheck.clustersProvider = new Provider<Clusters>() {
      @Override
      public Clusters get() {
        return clusters;
      }
    };

    Cluster cluster = mock(Cluster.class);
    when(clusters.getCluster(CLUSTER_NAME)).thenReturn(cluster);
    when(cluster.getClusterId()).thenReturn(CLUSTER_ID);

  }

  @Test
  public void testWarningWhenNoAlertsExist() throws AmbariException {
    when(alertsDAO.findCurrentByCluster(eq(CLUSTER_ID))).thenReturn(Collections.emptyList());

    ClusterInformation clusterInformation = new ClusterInformation(CLUSTER_NAME, false, null, null, null);
    UpgradeCheckRequest request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING, null, null, null);

    UpgradeCheckResult result = healthCheck.perform(request);
    Assert.assertEquals(UpgradeCheckStatus.PASS, result.getStatus());
    Assert.assertTrue(result.getFailedDetail().isEmpty());
  }

  @Test
  public void testWarningWhenCriticalAlertExists() throws AmbariException {
    expectWarning(AlertState.CRITICAL);
  }

  @Test
  public void testWarningWhenWarningAlertExists() throws AmbariException {
    expectWarning(AlertState.WARNING);
  }

  private void expectWarning(AlertState alertState) throws AmbariException {
    AlertCurrentEntity alertCurrentEntity = new AlertCurrentEntity();
    AlertHistoryEntity criticalAlert = new AlertHistoryEntity();
    AlertDefinitionEntity alertDefinition = new AlertDefinitionEntity();

    criticalAlert.setAlertDefinition(alertDefinition);
    criticalAlert.setHostName(ALERT_HOSTNAME);
    criticalAlert.setAlertState(alertState);

    alertDefinition.setLabel(ALERT_DEFINITION_LABEL);

    alertCurrentEntity.setAlertHistory(criticalAlert);

    when(alertsDAO.findCurrentByCluster(eq(CLUSTER_ID))).thenReturn(asList(alertCurrentEntity));

    ClusterInformation clusterInformation = new ClusterInformation(CLUSTER_NAME, false, null, null, null);
    UpgradeCheckRequest request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING, null, null, null);

    UpgradeCheckResult result = healthCheck.perform(request);

    Assert.assertEquals(UpgradeCheckStatus.WARNING, result.getStatus());
    Assert.assertFalse(result.getFailedDetail().isEmpty());
  }
}