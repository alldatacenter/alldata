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

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.annotations.UpgradeCheckInfo;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.CheckHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.repository.ClusterVersionSummary;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.spi.ClusterInformation;
import org.apache.ambari.spi.RepositoryType;
import org.apache.ambari.spi.RepositoryVersion;
import org.apache.ambari.spi.upgrade.UpgradeCheckDescription;
import org.apache.ambari.spi.upgrade.UpgradeCheckGroup;
import org.apache.ambari.spi.upgrade.UpgradeCheckRequest;
import org.apache.ambari.spi.upgrade.UpgradeCheckResult;
import org.apache.ambari.spi.upgrade.UpgradeCheckType;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Provider;

import junit.framework.Assert;

/**
 * Unit tests for ClusterCheck
 */
public class ClusterCheckTest extends EasyMockSupport {
  @Mock
  private Clusters clusters;

  /**
   * Used to mock out what services will be provided to us by the VDF/cluster.
   */
  @Mock
  private ClusterVersionSummary m_clusterVersionSummary;

  /**
   *
   */
  @Mock
  private VersionDefinitionXml m_vdfXml;

  private static final UpgradeCheckDescription m_description = new UpgradeCheckDescription(
      "Test Check", UpgradeCheckType.CLUSTER, "Test Check", "Test Failure Reason");

  private MockCheckHelper m_mockCheckHelper = new MockCheckHelper();

  @Before
  public void setup() throws Exception {
    injectMocks(this);
  }

  @Test
  public void testFormatEntityList() {
    ClusterCheck check = new TestCheckImpl(UpgradeCheckType.HOST);

    Assert.assertEquals("", check.formatEntityList(null));

    final LinkedHashSet<String> failedOn = new LinkedHashSet<>();
    Assert.assertEquals("", check.formatEntityList(failedOn));

    failedOn.add("host1");
    Assert.assertEquals("host1", check.formatEntityList(failedOn));

    failedOn.add("host2");
    Assert.assertEquals("host1 and host2", check.formatEntityList(failedOn));

    failedOn.add("host3");
    Assert.assertEquals("host1, host2 and host3", check.formatEntityList(failedOn));

    check = new TestCheckImpl(UpgradeCheckType.CLUSTER);
    Assert.assertEquals("host1, host2 and host3", check.formatEntityList(failedOn));

    check = new TestCheckImpl(UpgradeCheckType.SERVICE);
    Assert.assertEquals("host1, host2 and host3", check.formatEntityList(failedOn));

    check = new TestCheckImpl(null);
    Assert.assertEquals("host1, host2 and host3", check.formatEntityList(failedOn));
  }

  @Test
  public void testIsApplicable() throws Exception{
    final String clusterName = "c1";
    final Cluster cluster = createMock(Cluster.class);


    Map<String, Service> services = new HashMap<String, Service>(){{
      put("SERVICE1", null);
      put("SERVICE2", null);
      put("SERVICE3", null);
    }};

    Set<String> oneServiceList = Sets.newHashSet("SERVICE1");
    Set<String> atLeastOneServiceList = Sets.newHashSet("SERVICE1", "MISSING_SERVICE");
    Set<String> allServicesList = Sets.newHashSet("SERVICE1", "SERVICE2");
    Set<String> missingServiceList = Sets.newHashSet("MISSING_SERVICE");

    expect(clusters.getCluster(anyString())).andReturn(cluster).atLeastOnce();
    expect(cluster.getServices()).andReturn(services).atLeastOnce();

    RepositoryVersion repositoryVersion = createNiceMock(RepositoryVersion.class);
    expect(repositoryVersion.getId()).andReturn(1L).anyTimes();
    expect(repositoryVersion.getRepositoryType()).andReturn(RepositoryType.STANDARD).anyTimes();

    RepositoryVersionEntity repositoryVersionEntity = createNiceMock(RepositoryVersionEntity.class);
    expect(repositoryVersionEntity.getType()).andReturn(RepositoryType.STANDARD).anyTimes();
    expect(repositoryVersionEntity.getRepositoryXml()).andReturn(m_vdfXml).atLeastOnce();
    expect(m_vdfXml.getClusterSummary(EasyMock.anyObject(Cluster.class),
        EasyMock.anyObject(AmbariMetaInfo.class))).andReturn(m_clusterVersionSummary).atLeastOnce();

    expect(m_clusterVersionSummary.getAvailableServiceNames()).andReturn(
        allServicesList).atLeastOnce();

    final AmbariMetaInfo ami = createNiceMock(AmbariMetaInfo.class);

    m_mockCheckHelper.setMetaInfoProvider(new Provider<AmbariMetaInfo>() {
      @Override
      public AmbariMetaInfo get() {
        return ami;
      }
    });


    Mockito.when(m_mockCheckHelper.m_repositoryVersionDAO.findByPK(Mockito.anyLong())).thenReturn(
        repositoryVersionEntity);

    replayAll();

    TestCheckImpl check = new TestCheckImpl(UpgradeCheckType.SERVICE);
    check.checkHelperProvider = new Provider<CheckHelper>() {
      @Override
      public CheckHelper get() {
        return m_mockCheckHelper;
      }
    };

    ClusterInformation clusterInformation = new ClusterInformation(clusterName, false, null, null, null);
    UpgradeCheckRequest request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING,
        repositoryVersion, null, null);

    // case, where we need at least one service to be present
    check.setApplicableServices(oneServiceList);
    Assert.assertTrue(m_mockCheckHelper.getApplicableChecks(request, Lists.newArrayList(check)).size() == 1);

    check.setApplicableServices(atLeastOneServiceList);
    Assert.assertTrue(m_mockCheckHelper.getApplicableChecks(request, Lists.newArrayList(check)).size() == 1);

    check.setApplicableServices(missingServiceList);
    Assert.assertTrue(m_mockCheckHelper.getApplicableChecks(request, Lists.newArrayList(check)).size() == 0);
  }

  /**
   * Tests that even though the services are installed, the check doesn't match
   * since it's for a service not in the PATCH.
   *
   * @throws Exception
   */
  @Test
  public void testIsApplicableForPatch() throws Exception {
    final String clusterName = "c1";
    final Cluster cluster = createMock(Cluster.class);

    Map<String, Service> services = new HashMap<String, Service>() {
      {
        put("SERVICE1", null);
        put("SERVICE2", null);
        put("SERVICE3", null);
      }
    };

    Set<String> oneServiceList = Sets.newHashSet("SERVICE1");

    expect(clusters.getCluster(anyString())).andReturn(cluster).atLeastOnce();
    expect(cluster.getServices()).andReturn(services).atLeastOnce();

    RepositoryVersion repositoryVersion = createNiceMock(RepositoryVersion.class);
    expect(repositoryVersion.getId()).andReturn(1L).anyTimes();
    expect(repositoryVersion.getRepositoryType()).andReturn(RepositoryType.STANDARD).anyTimes();

    RepositoryVersionEntity repositoryVersionEntity = createNiceMock(RepositoryVersionEntity.class);
    expect(repositoryVersionEntity.getType()).andReturn(RepositoryType.STANDARD).anyTimes();
    expect(repositoryVersionEntity.getRepositoryXml()).andReturn(m_vdfXml).atLeastOnce();
    expect(m_vdfXml.getClusterSummary(EasyMock.anyObject(Cluster.class), EasyMock.anyObject(AmbariMetaInfo.class))).andReturn(
        m_clusterVersionSummary).atLeastOnce();

    // the cluster summary will only return 1 service for the upgrade, even
    // though this cluster has 2 services installed
    expect(m_clusterVersionSummary.getAvailableServiceNames()).andReturn(
        oneServiceList).atLeastOnce();

    m_mockCheckHelper.m_clusters = clusters;
    Mockito.when(m_mockCheckHelper.m_repositoryVersionDAO.findByPK(Mockito.anyLong())).thenReturn(
        repositoryVersionEntity);

    final AmbariMetaInfo ami = createNiceMock(AmbariMetaInfo.class);

    m_mockCheckHelper.setMetaInfoProvider(new Provider<AmbariMetaInfo>() {
      @Override
      public AmbariMetaInfo get() {
        return ami;
      }
    });

    replayAll();

    TestCheckImpl check = new TestCheckImpl(UpgradeCheckType.SERVICE);
    check.checkHelperProvider = new Provider<CheckHelper>() {
      @Override
      public CheckHelper get() {
        return m_mockCheckHelper;
      }
    };

    ClusterInformation clusterInformation = new ClusterInformation(clusterName, false, null, null, null);
    UpgradeCheckRequest request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING,
        repositoryVersion, null, null);

    // since the check is for SERVICE2, it should not match even though its
    // installed since the repository is only for SERVICE1
    check.setApplicableServices(Sets.newHashSet("SERVICE2"));
    Assert.assertTrue(m_mockCheckHelper.getApplicableChecks(request, Lists.newArrayList(check)).size() == 0);

    // ok, so now change the check to match against SERVICE1
    check.setApplicableServices(Sets.newHashSet("SERVICE1"));
    Assert.assertTrue(m_mockCheckHelper.getApplicableChecks(request, Lists.newArrayList(check)).size() == 1);
  }

  @UpgradeCheckInfo(
      group = UpgradeCheckGroup.DEFAULT,
      order = 1.0f,
      required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED })
  private class TestCheckImpl extends ClusterCheck {
    private UpgradeCheckType m_type;
    private Set<String> m_applicableServices = Sets.newHashSet();

    TestCheckImpl(UpgradeCheckType type) {
      super(null);
      m_type = type;

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
    public UpgradeCheckDescription getCheckDescription() {
      return m_description;
    }

    @Override
    public UpgradeCheckResult perform(UpgradeCheckRequest request)
        throws AmbariException {
      return new UpgradeCheckResult(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getApplicableServices() {
      return m_applicableServices;
    }

    void setApplicableServices(Set<String> applicableServices) {
      m_applicableServices = applicableServices;
    }
  }

  @UpgradeCheckInfo(group = UpgradeCheckGroup.DEFAULT, order = 1.0f, required = { UpgradeType.ROLLING })
  private class RollingTestCheckImpl extends ClusterCheck {
    private UpgradeCheckType m_type;

    RollingTestCheckImpl(UpgradeCheckType type) {
      super(null);
      m_type = type;

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
    public UpgradeCheckDescription getCheckDescription() {
      return m_description;
    }

    @Override
    public UpgradeCheckResult perform(UpgradeCheckRequest request)
        throws AmbariException {
      return new UpgradeCheckResult(this);
    }
  }

  @UpgradeCheckInfo(group = UpgradeCheckGroup.DEFAULT, order = 1.0f)
  private class NotRequiredCheckTest extends ClusterCheck {

    private UpgradeCheckType m_type;

    NotRequiredCheckTest(UpgradeCheckType type) {
      super(null);
      m_type = type;

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
    public UpgradeCheckDescription getCheckDescription() {
      return m_description;
    }

    @Override
    public UpgradeCheckResult perform(UpgradeCheckRequest request)
        throws AmbariException {
      return new UpgradeCheckResult(this);
    }
  }
}
