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

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.stack.upgrade.RepositoryVersionHelper;
import org.apache.ambari.server.stack.upgrade.UpgradePack;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
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
import org.mockito.runners.MockitoJUnitRunner;

import com.google.inject.Provider;

/**
 * Unit tests for HostsMasterMaintenanceCheck
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class HostsMasterMaintenanceCheckTest {
  private final Clusters clusters = Mockito.mock(Clusters.class);
  private final RepositoryVersionDAO repositoryVersionDAO = Mockito.mock(RepositoryVersionDAO.class);
  private final RepositoryVersionHelper repositoryVersionHelper = Mockito.mock(RepositoryVersionHelper.class);
  private final AmbariMetaInfo ambariMetaInfo = Mockito.mock(AmbariMetaInfo.class);

  @Mock
  private ClusterVersionSummary m_clusterVersionSummary;

  @Mock
  private VersionDefinitionXml m_vdfXml;

  @Mock
  private RepositoryVersion m_repositoryVersion;

  @Mock
  private RepositoryVersionEntity m_repositoryVersionEntity;

  final Map<String, Service> m_services = new HashMap<>();

  @Before
  public void setup() throws Exception {
    m_services.clear();

    Mockito.when(m_repositoryVersion.getRepositoryType()).thenReturn(RepositoryType.STANDARD);
    Mockito.when(m_repositoryVersionEntity.getType()).thenReturn(RepositoryType.STANDARD);
    Mockito.when(m_repositoryVersionEntity.getRepositoryXml()).thenReturn(m_vdfXml);
    Mockito.when(m_vdfXml.getClusterSummary(Mockito.any(Cluster.class), Mockito.any(AmbariMetaInfo.class))).thenReturn(m_clusterVersionSummary);
    Mockito.when(m_clusterVersionSummary.getAvailableServiceNames()).thenReturn(m_services.keySet());
  }

  @Test
  public void testPerform() throws Exception {
    Mockito.when(m_repositoryVersion.getVersion()).thenReturn("1.0.0.0-1234");
    Mockito.when(m_repositoryVersion.getStackId()).thenReturn(new StackId("HDP", "1.0").getStackId());
    Mockito.when(m_repositoryVersionEntity.getVersion()).thenReturn("1.0.0.0-1234");
    Mockito.when(m_repositoryVersionEntity.getStackId()).thenReturn(new StackId("HDP", "1.0"));

    final String upgradePackName = "upgrade_pack";
    final HostsMasterMaintenanceCheck hostsMasterMaintenanceCheck = new HostsMasterMaintenanceCheck();
    hostsMasterMaintenanceCheck.clustersProvider = new Provider<Clusters>() {

      @Override
      public Clusters get() {
        return clusters;
      }
    };
    hostsMasterMaintenanceCheck.repositoryVersionDaoProvider = new Provider<RepositoryVersionDAO>() {
      @Override
      public RepositoryVersionDAO get() {
        return repositoryVersionDAO;
      }
    };
    hostsMasterMaintenanceCheck.repositoryVersionHelper = new Provider<RepositoryVersionHelper>() {
      @Override
      public RepositoryVersionHelper get() {
        return repositoryVersionHelper;
      }
    };
    hostsMasterMaintenanceCheck.ambariMetaInfo = new Provider<AmbariMetaInfo>() {
      @Override
      public AmbariMetaInfo get() {
        return ambariMetaInfo;
      }
    };

    final Cluster cluster = Mockito.mock(Cluster.class);
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(clusters.getCluster("cluster")).thenReturn(cluster);
    Mockito.when(cluster.getDesiredStackVersion()).thenReturn(new StackId("HDP", "1.0"));
    Mockito.when(repositoryVersionHelper.getUpgradePackageName(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), (UpgradeType) Mockito.anyObject())).thenReturn(null);

    ClusterInformation clusterInformation = new ClusterInformation("cluster", false, null, null, null);
    UpgradeCheckRequest checkRequest = new UpgradeCheckRequest(clusterInformation,
        UpgradeType.ROLLING, m_repositoryVersion, null, null);

    UpgradeCheckResult result = hostsMasterMaintenanceCheck.perform(checkRequest);
    Assert.assertEquals(UpgradeCheckStatus.FAIL, result.getStatus());

    Mockito.when(repositoryVersionHelper.getUpgradePackageName(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), (UpgradeType) Mockito.anyObject())).thenReturn(upgradePackName);
    Mockito.when(ambariMetaInfo.getUpgradePacks(Mockito.anyString(), Mockito.anyString())).thenReturn(new HashMap<>());

    result = hostsMasterMaintenanceCheck.perform(checkRequest);
    Assert.assertEquals(UpgradeCheckStatus.FAIL, result.getStatus());

    final Map<String, UpgradePack> upgradePacks = new HashMap<>();
    final UpgradePack upgradePack = Mockito.mock(UpgradePack.class);
    Mockito.when(upgradePack.getName()).thenReturn(upgradePackName);
    upgradePacks.put(upgradePack.getName(), upgradePack);
    Mockito.when(ambariMetaInfo.getUpgradePacks(Mockito.anyString(), Mockito.anyString())).thenReturn(upgradePacks);
    Mockito.when(upgradePack.getTasks()).thenReturn(new HashMap<>());
    Mockito.when(cluster.getServices()).thenReturn(new HashMap<>());
    Mockito.when(clusters.getHostsForCluster(Mockito.anyString())).thenReturn(new HashMap<>());

    result = hostsMasterMaintenanceCheck.perform(checkRequest);
    Assert.assertEquals(UpgradeCheckStatus.PASS, result.getStatus());
  }
}
