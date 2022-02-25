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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.models.HostComponentSummary;
import org.apache.ambari.server.state.CheckHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.repository.ClusterVersionSummary;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.spi.ClusterInformation;
import org.apache.ambari.spi.RepositoryType;
import org.apache.ambari.spi.RepositoryVersion;
import org.apache.ambari.spi.upgrade.UpgradeCheckRequest;
import org.apache.ambari.spi.upgrade.UpgradeCheckResult;
import org.apache.ambari.spi.upgrade.UpgradeCheckStatus;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Provider;


/**
 * Unit tests for ComponentsInstallationCheck
 *
 */
@RunWith(PowerMockRunner.class)               // Allow mocking static methods
@PrepareForTest(HostComponentSummary.class)   // This class has a static method that will be mocked
public class ComponentsInstallationCheckTest {
  private final Clusters clusters = Mockito.mock(Clusters.class);
  private AmbariMetaInfo ambariMetaInfo = Mockito.mock(AmbariMetaInfo.class);

  @Mock
  private ClusterVersionSummary m_clusterVersionSummary;

  @Mock
  private VersionDefinitionXml m_vdfXml;

  @Mock
  private RepositoryVersion m_repositoryVersion;

  @Mock
  private RepositoryVersionEntity m_repositoryVersionEntity;

  private MockCheckHelper m_checkHelper = new MockCheckHelper();

  final Map<String, Service> m_services = new HashMap<>();

  /**
   *
   */
  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);

    m_services.clear();

    StackId stackId = new StackId("HDP", "2.2");
    String version = "2.2.0.0-1234";

    Mockito.when(m_repositoryVersion.getId()).thenReturn(1L);
    Mockito.when(m_repositoryVersion.getRepositoryType()).thenReturn(RepositoryType.STANDARD);
    Mockito.when(m_repositoryVersion.getStackId()).thenReturn(stackId.toString());
    Mockito.when(m_repositoryVersion.getVersion()).thenReturn(version);

    Mockito.when(m_repositoryVersionEntity.getType()).thenReturn(RepositoryType.STANDARD);
    Mockito.when(m_repositoryVersionEntity.getVersion()).thenReturn(version);
    Mockito.when(m_repositoryVersionEntity.getStackId()).thenReturn(stackId);
    Mockito.when(m_repositoryVersionEntity.getRepositoryXml()).thenReturn(m_vdfXml);
    Mockito.when(m_vdfXml.getClusterSummary(Mockito.any(Cluster.class), Mockito.any(AmbariMetaInfo.class))).thenReturn(m_clusterVersionSummary);
    Mockito.when(m_clusterVersionSummary.getAvailableServiceNames()).thenReturn(m_services.keySet());

    final AmbariMetaInfo metaInfo = Mockito.mock(AmbariMetaInfo.class);

    m_checkHelper.setMetaInfoProvider(new Provider<AmbariMetaInfo>() {
      @Override
      public AmbariMetaInfo get() {
        return metaInfo;
      }
    });

    m_checkHelper.m_clusters = clusters;
    Mockito.when(m_checkHelper.m_repositoryVersionDAO.findByPK(Mockito.anyLong())).thenReturn(m_repositoryVersionEntity);
  }

  @Test
  public void testPerform() throws Exception {
    PowerMockito.mockStatic(HostComponentSummary.class);

    final ComponentsInstallationCheck componentsInstallationCheck = new ComponentsInstallationCheck();
    componentsInstallationCheck.clustersProvider = new Provider<Clusters>() {

      @Override
      public Clusters get() {
        return clusters;
      }
    };

    componentsInstallationCheck.ambariMetaInfo = new Provider<AmbariMetaInfo>() {
      @Override
      public AmbariMetaInfo get() {
        return ambariMetaInfo;
      }
    };

      componentsInstallationCheck.checkHelperProvider = new Provider<CheckHelper>() {
      @Override
      public CheckHelper get() {
        return m_checkHelper;
      }
    };

    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(cluster.getCurrentStackVersion()).thenReturn(new StackId("HDP", "2.2"));
    Mockito.when(clusters.getCluster("cluster")).thenReturn(cluster);

    final Service hdfsService = Mockito.mock(Service.class);
    final Service tezService = Mockito.mock(Service.class);
    final Service amsService = Mockito.mock(Service.class);
    Mockito.when(hdfsService.getMaintenanceState()).thenReturn(MaintenanceState.OFF);
    Mockito.when(tezService.getMaintenanceState()).thenReturn(MaintenanceState.OFF);
    Mockito.when(amsService.getMaintenanceState()).thenReturn(MaintenanceState.OFF);

    m_services.put("HDFS", hdfsService);
    m_services.put("TEZ", tezService);
    m_services.put("AMBARI_METRICS", amsService);

    Mockito.when(hdfsService.getName()).thenReturn("HDFS");
    Mockito.when(tezService.getName()).thenReturn("TEZ");
    Mockito.when(amsService.getName()).thenReturn("AMBARI_METRICS");

    Mockito.when(hdfsService.isClientOnlyService()).thenReturn(false);
    Mockito.when(tezService.isClientOnlyService()).thenReturn(true);
    Mockito.when(amsService.isClientOnlyService()).thenReturn(false);

    Mockito.when(cluster.getServices()).thenReturn(m_services);

    Mockito.when(cluster.getService("HDFS")).thenReturn(hdfsService);
    Mockito.when(cluster.getService("TEZ")).thenReturn(tezService);
    Mockito.when(cluster.getService("AMBARI_METRICS")).thenReturn(amsService);

    Mockito.when(ambariMetaInfo.getComponent(Mockito.anyString(), Mockito.anyString(),
        Mockito.anyString(), Mockito.anyString())).thenAnswer(new Answer<ComponentInfo>() {
      @Override
      public ComponentInfo answer(InvocationOnMock invocation) throws Throwable {
        ComponentInfo anyInfo = Mockito.mock(ComponentInfo.class);
        if (invocation.getArguments().length > 3 && "DATANODE".equals(invocation.getArguments()[3])) {
          Mockito.when(anyInfo.getCardinality()).thenReturn("1+");
        } else {
          Mockito.when(anyInfo.getCardinality()).thenReturn(null);
        }

        return anyInfo;
      }
    });

    // Put Components inside Services
    // HDFS
    Map<String, ServiceComponent> hdfsComponents = new HashMap<>();

    ServiceComponent nameNode = Mockito.mock(ServiceComponent.class);
    Mockito.when(nameNode.getName()).thenReturn("NAMENODE");
    Mockito.when(nameNode.isClientComponent()).thenReturn(false);
    Mockito.when(nameNode.isVersionAdvertised()).thenReturn(true);
    Mockito.when(nameNode.isMasterComponent()).thenReturn(true);

    ServiceComponent dataNode = Mockito.mock(ServiceComponent.class);
    Mockito.when(dataNode.getName()).thenReturn("DATANODE");
    Mockito.when(dataNode.isClientComponent()).thenReturn(false);
    Mockito.when(dataNode.isVersionAdvertised()).thenReturn(true);
    Mockito.when(dataNode.isMasterComponent()).thenReturn(false);

    ServiceComponent zkfc = Mockito.mock(ServiceComponent.class);
    Mockito.when(zkfc.getName()).thenReturn("ZKFC");
    Mockito.when(zkfc.isClientComponent()).thenReturn(false);
    Mockito.when(zkfc.isVersionAdvertised()).thenReturn(false);
    Mockito.when(zkfc.isMasterComponent()).thenReturn(false);

    hdfsComponents.put("NAMENODE", nameNode);
    hdfsComponents.put("DATANODE", dataNode);
    hdfsComponents.put("ZKFC", zkfc);

    Mockito.when(hdfsService.getServiceComponents()).thenReturn(hdfsComponents);

    // TEZ
    Map<String, ServiceComponent> tezComponents = new HashMap<>();

    ServiceComponent tezClient = Mockito.mock(ServiceComponent.class);
    Mockito.when(tezClient.getName()).thenReturn("TEZ_CLIENT");
    Mockito.when(tezClient.isClientComponent()).thenReturn(true);
    Mockito.when(tezClient.isVersionAdvertised()).thenReturn(true);

    tezComponents.put("TEZ_CLIENT", tezClient);

    Mockito.when(tezService.getServiceComponents()).thenReturn(tezComponents);

    // AMS
    Map<String, ServiceComponent> amsComponents = new HashMap<>();

    ServiceComponent metricsCollector = Mockito.mock(ServiceComponent.class);
    Mockito.when(metricsCollector.getName()).thenReturn("METRICS_COLLECTOR");
    Mockito.when(metricsCollector.isClientComponent()).thenReturn(false);
    Mockito.when(metricsCollector.isVersionAdvertised()).thenReturn(false);

    ServiceComponent metricsMonitor = Mockito.mock(ServiceComponent.class);
    Mockito.when(metricsMonitor.getName()).thenReturn("METRICS_MONITOR");
    Mockito.when(metricsMonitor.isClientComponent()).thenReturn(false);
    Mockito.when(metricsMonitor.isVersionAdvertised()).thenReturn(false);

    amsComponents.put("METRICS_COLLECTOR", metricsCollector);
    amsComponents.put("METRICS_MONITOR", metricsMonitor);

    Mockito.when(amsService.getServiceComponents()).thenReturn(amsComponents);

    final HostComponentSummary hcsNameNode = Mockito.mock(HostComponentSummary.class);
    final HostComponentSummary hcsDataNode1 = Mockito.mock(HostComponentSummary.class);
    final HostComponentSummary hcsDataNode2 = Mockito.mock(HostComponentSummary.class);
    final HostComponentSummary hcsDataNode3 = Mockito.mock(HostComponentSummary.class);
    final HostComponentSummary hcsZKFC = Mockito.mock(HostComponentSummary.class);
    final HostComponentSummary hcsTezClient = Mockito.mock(HostComponentSummary.class);
    final HostComponentSummary hcsMetricsCollector = Mockito.mock(HostComponentSummary.class);
    final HostComponentSummary hcsMetricsMonitor = Mockito.mock(HostComponentSummary.class);

    List<HostComponentSummary> allHostComponentSummaries = new ArrayList<>();
    allHostComponentSummaries.add(hcsNameNode);
    allHostComponentSummaries.add(hcsDataNode1);
    allHostComponentSummaries.add(hcsDataNode2);
    allHostComponentSummaries.add(hcsDataNode3);
    allHostComponentSummaries.add(hcsZKFC);
    allHostComponentSummaries.add(hcsTezClient);
    allHostComponentSummaries.add(hcsMetricsCollector);
    allHostComponentSummaries.add(hcsMetricsMonitor);

    final Map<String, Host> hosts = new HashMap<>();
    final Host host1 = Mockito.mock(Host.class);
    final Host host2 = Mockito.mock(Host.class);
    final Host host3 = Mockito.mock(Host.class);
    Mockito.when(host1.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);
    Mockito.when(host2.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);
    Mockito.when(host3.getMaintenanceState(1L)).thenReturn(MaintenanceState.OFF);
    hosts.put("host1", host1);
    hosts.put("host2", host2);
    hosts.put("host3", host3);
    Mockito.when(hcsNameNode.getHostName()).thenReturn("host1");
    Mockito.when(hcsDataNode1.getHostName()).thenReturn("host1");
    Mockito.when(hcsDataNode2.getHostName()).thenReturn("host2");
    Mockito.when(hcsDataNode3.getHostName()).thenReturn("host3");
    Mockito.when(hcsZKFC.getHostName()).thenReturn("host1");
    Mockito.when(hcsTezClient.getHostName()).thenReturn("host2");
    Mockito.when(hcsMetricsCollector.getHostName()).thenReturn("host3");
    Mockito.when(hcsMetricsMonitor.getHostName()).thenReturn("host3");

    // Mock the static method
    Mockito.when(HostComponentSummary.getHostComponentSummaries("HDFS", "NAMENODE")).thenReturn(Arrays.asList(hcsNameNode));
    Mockito.when(HostComponentSummary.getHostComponentSummaries("HDFS", "DATANODE")).thenReturn(Arrays.asList(hcsDataNode1, hcsDataNode2, hcsDataNode3));
    Mockito.when(HostComponentSummary.getHostComponentSummaries("HDFS", "ZKFC")).thenReturn(Arrays.asList(hcsZKFC));
    Mockito.when(HostComponentSummary.getHostComponentSummaries("TEZ", "TEZ_CLIENT")).thenReturn(Arrays.asList(hcsTezClient));
    Mockito.when(HostComponentSummary.getHostComponentSummaries("AMBARI_METRICS", "METRICS_COLLECTOR")).thenReturn(Arrays.asList(hcsMetricsCollector));
    Mockito.when(HostComponentSummary.getHostComponentSummaries("AMBARI_METRICS", "METRICS_MONITOR")).thenReturn(Arrays.asList(hcsMetricsMonitor));
    for (String hostName : hosts.keySet()) {
      Mockito.when(clusters.getHost(hostName)).thenReturn(hosts.get(hostName));
    }

    // Case 1. Initialize with good values
    for (HostComponentSummary hcs : allHostComponentSummaries) {
      Mockito.when(hcs.getDesiredState()).thenReturn(State.INSTALLED);
      Mockito.when(hcs.getCurrentState()).thenReturn(State.STARTED);
    }

    ClusterInformation clusterInformation = new ClusterInformation("cluster", false, null, null, null);
    UpgradeCheckRequest request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING,
        m_repositoryVersion, null, null);

    Mockito.when(hcsTezClient.getCurrentState()).thenReturn(State.INSTALLED);

    UpgradeCheckResult check = componentsInstallationCheck.perform(request);
    Assert.assertEquals(UpgradeCheckStatus.PASS, check.getStatus());
    Assert.assertTrue(check.getFailedDetail().isEmpty());

    // Case 2. Ensure that AMS is ignored even if their current state is not INSTALLED
    Mockito.when(hcsMetricsCollector.getCurrentState()).thenReturn(State.INSTALL_FAILED);
    Mockito.when(hcsMetricsMonitor.getCurrentState()).thenReturn(State.INSTALL_FAILED);
    check = new UpgradeCheckResult(null, null);
    check = componentsInstallationCheck.perform(request);
    Assert.assertEquals(UpgradeCheckStatus.PASS, check.getStatus());
    Assert.assertTrue(check.getFailedDetail().isEmpty());

    // Case 3: Change TEZ client state to INSTALL_FAILED, should fail
    Mockito.when(hcsTezClient.getCurrentState()).thenReturn(State.INSTALL_FAILED);
    check = new UpgradeCheckResult(null, null);
    check = componentsInstallationCheck.perform(request);
    Assert.assertEquals(UpgradeCheckStatus.FAIL, check.getStatus());
    Assert.assertTrue(check.getFailReason().indexOf("Service components in INSTALL_FAILED state") > -1);
    Assert.assertEquals(1, check.getFailedDetail().size());

    // Case 4: Change TEZ client state to INSTALL_FAILED and place TEZ in Maintenance mode, should succeed
    Mockito.when(tezService.getMaintenanceState()).thenReturn(MaintenanceState.ON);
    Mockito.when(hcsTezClient.getCurrentState()).thenReturn(State.INSTALL_FAILED);
    check = new UpgradeCheckResult(null, null);
    check = componentsInstallationCheck.perform(request);
    Assert.assertEquals(UpgradeCheckStatus.PASS, check.getStatus());
    Assert.assertTrue(check.getFailedDetail().isEmpty());

    // Case 5: Change TEZ client state to INSTALL_FAILED and place host2 in Maintenance mode, should succeed
    Mockito.when(tezService.getMaintenanceState()).thenReturn(MaintenanceState.OFF);
    Mockito.when(host2.getMaintenanceState(1L)).thenReturn(MaintenanceState.ON);
    Mockito.when(hcsTezClient.getCurrentState()).thenReturn(State.INSTALL_FAILED);
    check = new UpgradeCheckResult(null, null);
    check = componentsInstallationCheck.perform(request);
    Assert.assertEquals(UpgradeCheckStatus.PASS, check.getStatus());
    Assert.assertTrue(check.getFailedDetail().isEmpty());
  }
}
