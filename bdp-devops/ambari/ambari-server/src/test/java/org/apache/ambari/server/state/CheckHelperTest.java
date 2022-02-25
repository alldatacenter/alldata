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

package org.apache.ambari.server.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ambari.annotations.UpgradeCheckInfo;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.checks.ClusterCheck;
import org.apache.ambari.server.checks.MockCheckHelper;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.repository.ClusterVersionSummary;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.spi.ClusterInformation;
import org.apache.ambari.spi.RepositoryType;
import org.apache.ambari.spi.RepositoryVersion;
import org.apache.ambari.spi.upgrade.UpgradeCheck;
import org.apache.ambari.spi.upgrade.UpgradeCheckDescription;
import org.apache.ambari.spi.upgrade.UpgradeCheckRequest;
import org.apache.ambari.spi.upgrade.UpgradeCheckResult;
import org.apache.ambari.spi.upgrade.UpgradeCheckStatus;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.inject.Provider;

import junit.framework.Assert;


/**
 * Tests the {@link CheckHelper} class
 * Makes sure that people don't forget to add new checks to registry.
 */
@RunWith(MockitoJUnitRunner.class)
public class CheckHelperTest {

  private final Clusters clusters = Mockito.mock(Clusters.class);

  private MockCheck m_mockCheck;

  private UpgradeCheckDescription m_mockUpgradeCheckDescription = Mockito.mock(UpgradeCheckDescription.class);

  @Mock
  private ClusterVersionSummary m_clusterVersionSummary;

  @Mock
  private VersionDefinitionXml m_vdfXml;

  @Mock
  private RepositoryVersion m_repositoryVersion;

  @Mock
  private RepositoryVersionEntity m_repositoryVersionEntity;

  @Mock
  private Object m_mockPerform;

  @Mock
  private RepositoryVersionDAO repositoryVersionDao;

  final Set<String> m_services = new HashSet<>();

  @Before
  public void setup() throws Exception {
    m_mockCheck = new MockCheck();

    Mockito.when(m_mockPerform.toString()).thenReturn("Perform!");

    m_services.clear();
    Mockito.when(m_repositoryVersion.getId()).thenReturn(1L);
    Mockito.when(m_repositoryVersion.getRepositoryType()).thenReturn(RepositoryType.STANDARD);
    Mockito.when(repositoryVersionDao.findByPK(Mockito.anyLong())).thenReturn(m_repositoryVersionEntity);
    Mockito.when(m_repositoryVersionEntity.getRepositoryXml()).thenReturn(m_vdfXml);
    Mockito.when(m_vdfXml.getClusterSummary(Mockito.any(Cluster.class), Mockito.any(AmbariMetaInfo.class))).thenReturn(m_clusterVersionSummary);
    Mockito.when(m_clusterVersionSummary.getAvailableServiceNames()).thenReturn(m_services);
  }

  /**
   * Sunny case when applicable.
   */
  @Test
  public void testPreUpgradeCheck() throws Exception {
    final CheckHelper helper = new CheckHelper();
    helper.clustersProvider = () -> clusters;
    helper.repositoryVersionDaoProvider = () -> repositoryVersionDao;

    Configuration configuration = EasyMock.createNiceMock(Configuration.class);
    List<UpgradeCheck> updateChecksRegistry = new ArrayList<>();

    EasyMock.expect(configuration.isUpgradePrecheckBypass()).andReturn(false);
    EasyMock.replay(configuration);
    updateChecksRegistry.add(m_mockCheck);

    ClusterInformation clusterInformation = new ClusterInformation("cluster", false, null, null, null);
    UpgradeCheckRequest request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING, m_repositoryVersion, null, null);

    helper.performChecks(request, updateChecksRegistry, configuration);

    Assert.assertEquals(UpgradeCheckStatus.PASS, request.getResult(m_mockUpgradeCheckDescription));
  }

  /**
   * Checks can be ignored, even if they are expected to fail.
   */
  @Test
  public void testPreUpgradeCheckNotApplicable() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);

    m_services.add("KAFKA");

    Mockito.when(cluster.getServices()).thenReturn(new HashMap<>());
    Mockito.when(cluster.getClusterId()).thenReturn(1L);
    Mockito.when(clusters.getCluster("cluster")).thenReturn(cluster);

    final CheckHelper helper = new CheckHelper();
    helper.clustersProvider = () -> clusters;
    helper.repositoryVersionDaoProvider = () -> repositoryVersionDao;

    Configuration configuration = EasyMock.createNiceMock(Configuration.class);
    List<UpgradeCheck> updateChecksRegistry = new ArrayList<>();

    EasyMock.expect(configuration.isUpgradePrecheckBypass()).andReturn(false);
    EasyMock.replay(configuration);
    updateChecksRegistry.add(m_mockCheck);

    ClusterInformation clusterInformation = new ClusterInformation("cluster", false, null, null, null);
    UpgradeCheckRequest request = new UpgradeCheckRequest(clusterInformation,
        UpgradeType.NON_ROLLING, m_repositoryVersion, null, null);

    helper.performChecks(request, updateChecksRegistry, configuration);

    Assert.assertEquals(null, request.getResult(m_mockUpgradeCheckDescription));

    request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING, m_repositoryVersion, null, null);
  }

  /**
   * Check that throwing an exception still fails.
   */
  @Test
  public void testPreUpgradeCheckThrowsException() throws Exception {
    final CheckHelper helper = new CheckHelper();
    helper.clustersProvider = () -> clusters;
    helper.repositoryVersionDaoProvider = () -> repositoryVersionDao;

    Configuration configuration = EasyMock.createNiceMock(Configuration.class);
    List<UpgradeCheck> updateChecksRegistry = new ArrayList<>();

    EasyMock.expect(configuration.isUpgradePrecheckBypass()).andReturn(false);
    EasyMock.replay(configuration);
    updateChecksRegistry.add(m_mockCheck);

    // this will cause an exception
    Mockito.when(m_mockPerform.toString()).thenThrow(new RuntimeException());

    ClusterInformation clusterInformation = new ClusterInformation("cluster", false, null, null, null);
    UpgradeCheckRequest request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING, m_repositoryVersion, null, null);

    helper.performChecks(request, updateChecksRegistry, configuration);

    Assert.assertEquals(UpgradeCheckStatus.FAIL, request.getResult(m_mockUpgradeCheckDescription));
  }

  /**
   * Test that applicable tests that fail when configured to bypass failures results in a status of {@see UpgradeCheckStatus.BYPASS}
   */
  @Test
  public void testPreUpgradeCheckBypassesFailure() throws Exception {
    final CheckHelper helper = new CheckHelper();
    helper.clustersProvider = () -> clusters;
    helper.repositoryVersionDaoProvider = () -> repositoryVersionDao;

    Configuration configuration = EasyMock.createNiceMock(Configuration.class);
    List<UpgradeCheck> updateChecksRegistry = new ArrayList<>();

    EasyMock.expect(configuration.isUpgradePrecheckBypass()).andReturn(true);
    EasyMock.replay(configuration);
    updateChecksRegistry.add(m_mockCheck);

    // this will cause an exception, triggering the bypass
    Mockito.when(m_mockPerform.toString()).thenThrow(new RuntimeException());

    ClusterInformation clusterInformation = new ClusterInformation("cluster", false, null, null, null);
    UpgradeCheckRequest request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING, m_repositoryVersion, null, null);

    helper.performChecks(request, updateChecksRegistry, configuration);

    Assert.assertEquals(UpgradeCheckStatus.BYPASS, request.getResult(m_mockUpgradeCheckDescription));
  }

  @Test
  public void testPreUpgradeCheckClusterMissing() throws Exception {
    final Cluster cluster = Mockito.mock(Cluster.class);
    final Service service = Mockito.mock(Service.class);

    m_services.add("KAFKA");

    Mockito.when(cluster.getServices()).thenReturn(new HashMap<>());
    Mockito.when(cluster.getClusterId()).thenReturn(1L);

    Mockito.when(clusters.getCluster(Mockito.anyString())).thenReturn(cluster);

    final MockCheckHelper helper = new MockCheckHelper();
    helper.m_clusters = clusters;
    helper.m_repositoryVersionDAO = repositoryVersionDao;
    helper.clustersProvider = () -> clusters;

    final AmbariMetaInfo metaInfo = Mockito.mock(AmbariMetaInfo.class);
    helper.metaInfoProvider = new Provider<AmbariMetaInfo>() {
      @Override
      public AmbariMetaInfo get() {
        return metaInfo;
      }
    };

    Configuration configuration = EasyMock.createNiceMock(Configuration.class);
    List<UpgradeCheck> updateChecksRegistry = new ArrayList<>();

    EasyMock.expect(configuration.isUpgradePrecheckBypass()).andReturn(false);
    EasyMock.replay(configuration);
    updateChecksRegistry.add(m_mockCheck);

    // this will cause an exception, triggering the fail
    Mockito.when(m_mockPerform.toString()).thenThrow(new RuntimeException());

    ClusterInformation clusterInformation = new ClusterInformation("cluster", false, null, null, null);
    UpgradeCheckRequest request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING, m_repositoryVersion, null, null);

    helper.performChecks(request, updateChecksRegistry, configuration);

    Assert.assertEquals(UpgradeCheckStatus.FAIL, request.getResult(m_mockUpgradeCheckDescription));
  }

  @UpgradeCheckInfo(
      required = { UpgradeType.ROLLING })
  class MockCheck extends ClusterCheck {

    protected MockCheck() {
      super(m_mockUpgradeCheckDescription);

      clustersProvider = new Provider<Clusters>() {

        @Override
        public Clusters get() {
          return clusters;
        }
      };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getApplicableServices() {
      return m_services;
    }

    @Override
    public UpgradeCheckResult perform(UpgradeCheckRequest request)
        throws AmbariException {
      m_mockPerform.toString();
      return new UpgradeCheckResult(this);
    }
  }
}