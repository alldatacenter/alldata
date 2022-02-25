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

import static org.apache.ambari.server.state.UpgradeState.IN_PROGRESS;
import static org.apache.ambari.server.state.UpgradeState.VERSION_MISMATCH;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.spi.ClusterInformation;
import org.apache.ambari.spi.upgrade.UpgradeCheckRequest;
import org.apache.ambari.spi.upgrade.UpgradeCheckResult;
import org.apache.ambari.spi.upgrade.UpgradeCheckStatus;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Provider;

/**
 * Checks VersionMismatchCheck pre-upgrade check. Includes tests that emulate both
 * clusters with and without host components in VERSION_MISMATCH upgrade state.
 */
public class VersionMismatchCheckTest {
  private static final String CLUSTER_NAME = "cluster1";
  private static final String FIRST_SERVICE_NAME = "service1";
  private static final String FIRST_SERVICE_COMPONENT_NAME = "component1";
  private static final String FIRST_SERVICE_COMPONENT_HOST_NAME = "host1";
  private VersionMismatchCheck versionMismatchCheck;
  private Map<String, ServiceComponentHost> firstServiceComponentHosts;

  @Before
  public void setUp() throws Exception {
    final Clusters clusters = mock(Clusters.class);
    versionMismatchCheck = new VersionMismatchCheck();
    versionMismatchCheck.clustersProvider = new Provider<Clusters>() {
      @Override
      public Clusters get() {
        return clusters;
      }
    };

    Cluster cluster = mock(Cluster.class);
    when(clusters.getCluster(CLUSTER_NAME)).thenReturn(cluster);

    Service firstService = mock(Service.class);
    Map<String, Service> services = ImmutableMap.of(FIRST_SERVICE_NAME, firstService);
    when(cluster.getServices()).thenReturn(services);

    ServiceComponent firstServiceComponent = mock(ServiceComponent.class);
    Map<String, ServiceComponent> components = ImmutableMap.of(FIRST_SERVICE_COMPONENT_NAME, firstServiceComponent);
    when(firstService.getServiceComponents()).thenReturn(components);

    ServiceComponentHost firstServiceComponentHost = mock(ServiceComponentHost.class);
    firstServiceComponentHosts = ImmutableMap.of(FIRST_SERVICE_COMPONENT_HOST_NAME, firstServiceComponentHost);
    when(firstServiceComponent.getServiceComponentHosts()).thenReturn(firstServiceComponentHosts);
  }

  @Test
  public void testWarningWhenHostWithVersionMismatchExists() throws Exception {
    when(firstServiceComponentHosts.get(FIRST_SERVICE_COMPONENT_HOST_NAME).getUpgradeState()).thenReturn(VERSION_MISMATCH);

    ClusterInformation clusterInformation = new ClusterInformation(CLUSTER_NAME, false, null, null, null);
    UpgradeCheckRequest request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING,
        null, null, null);

    UpgradeCheckResult check = versionMismatchCheck.perform(request);
    Assert.assertEquals(UpgradeCheckStatus.WARNING, check.getStatus());
  }

  @Test
  public void testWarningWhenHostWithVersionMismatchDoesNotExist() throws Exception {
    when(firstServiceComponentHosts.get(FIRST_SERVICE_COMPONENT_HOST_NAME).getUpgradeState()).thenReturn(IN_PROGRESS);

    ClusterInformation clusterInformation = new ClusterInformation(CLUSTER_NAME, false, null, null, null);
    UpgradeCheckRequest request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING,
        null, null, null);

    UpgradeCheckResult check = versionMismatchCheck.perform(request);
    Assert.assertEquals(UpgradeCheckStatus.PASS, check.getStatus());
  }
}
