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

import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostHealthStatus;
import org.apache.ambari.server.state.HostHealthStatus.HealthStatus;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.spi.ClusterInformation;
import org.apache.ambari.spi.RepositoryType;
import org.apache.ambari.spi.upgrade.UpgradeCheckRequest;
import org.apache.ambari.spi.upgrade.UpgradeCheckResult;
import org.apache.ambari.spi.upgrade.UpgradeCheckStatus;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.inject.Provider;

/**
 * Unit tests for HostsHeartbeatCheck
 *
 */
public class HostsHeartbeatCheckTest {
  private final Clusters clusters = Mockito.mock(Clusters.class);

  final RepositoryVersionEntity m_repositoryVersion = Mockito.mock(RepositoryVersionEntity.class);

  /**
   *
   */
  @Before
  public void setup() throws Exception {
    Mockito.when(m_repositoryVersion.getType()).thenReturn(RepositoryType.STANDARD);
    Mockito.when(m_repositoryVersion.getVersion()).thenReturn("2.2.0.0-1234");
    Mockito.when(m_repositoryVersion.getStackId()).thenReturn(new StackId("HDP", "2.2"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testPerform() throws Exception {
    final HostsHeartbeatCheck hostHeartbeatCheck = new HostsHeartbeatCheck();
    hostHeartbeatCheck.clustersProvider = new Provider<Clusters>() {

      @Override
      public Clusters get() {
        return clusters;
      }
    };

    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(cluster.getCurrentStackVersion()).thenReturn(new StackId("HDP", "2.2"));
    Mockito.when(clusters.getCluster("cluster")).thenReturn(cluster);

    final List<Host> hosts = new ArrayList<>();
    final Host host1 = Mockito.mock(Host.class);
    final Host host2 = Mockito.mock(Host.class);
    final Host host3 = Mockito.mock(Host.class);

    final HostHealthStatus status1 = Mockito.mock(HostHealthStatus.class);
    final HostHealthStatus status2 = Mockito.mock(HostHealthStatus.class);
    final HostHealthStatus status3 = Mockito.mock(HostHealthStatus.class);

    Mockito.when(host1.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);
    Mockito.when(host2.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);
    Mockito.when(host3.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);

    Mockito.when(host1.getHealthStatus()).thenReturn(status1);
    Mockito.when(host2.getHealthStatus()).thenReturn(status2);
    Mockito.when(host3.getHealthStatus()).thenReturn(status3);

    Mockito.when(status1.getHealthStatus()).thenReturn(HealthStatus.HEALTHY);
    Mockito.when(status2.getHealthStatus()).thenReturn(HealthStatus.HEALTHY);
    Mockito.when(status3.getHealthStatus()).thenReturn(HealthStatus.UNKNOWN);

    hosts.add(host1);
    hosts.add(host2);
    hosts.add(host3);

    Mockito.when(cluster.getHosts()).thenReturn(hosts);

    ClusterInformation clusterInformation = new ClusterInformation("cluster", false, null, null, null);
    UpgradeCheckRequest request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING, null, null, null);

    UpgradeCheckResult check = hostHeartbeatCheck.perform(request);
    Assert.assertEquals(UpgradeCheckStatus.FAIL, check.getStatus());
    Assert.assertFalse(check.getFailedDetail().isEmpty());

    // put the unhealthy host into MM which will allow this check to pass
    check = new UpgradeCheckResult(null, null);
    Mockito.when(host3.getMaintenanceState(1L)).thenReturn(MaintenanceState.ON);
    check = hostHeartbeatCheck.perform(request);
    Assert.assertEquals(UpgradeCheckStatus.PASS, check.getStatus());
    Assert.assertTrue(check.getFailedDetail().isEmpty());

    // make the host healthy and take it out of MM to produce a PASS result
    Mockito.when(status3.getHealthStatus()).thenReturn(HealthStatus.HEALTHY);
    check = new UpgradeCheckResult(null, null);
    Mockito.when(host3.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);
    check =hostHeartbeatCheck.perform(request);
    Assert.assertEquals(UpgradeCheckStatus.PASS, check.getStatus());
    Assert.assertTrue(check.getFailedDetail().isEmpty());
  }
}
