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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigMergeHelper;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.spi.ClusterInformation;
import org.apache.ambari.spi.RepositoryType;
import org.apache.ambari.spi.RepositoryVersion;
import org.apache.ambari.spi.upgrade.UpgradeCheckRequest;
import org.apache.ambari.spi.upgrade.UpgradeCheckResult;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.inject.Provider;
//
/**
 * Unit tests for ConfigurationMergeCheck
 */
public class ConfigurationMergeCheckTest {

  private static final String CONFIG_FILE = "hdfs-site.xml";
  private static final String CONFIG_TYPE = "hdfs-site";
  private static final String CONFIG_PROPERTY = "hdfs.property";

  private Clusters clusters = EasyMock.createMock(Clusters.class);
  private Map<String, String> m_configMap = new HashMap<>();

  private static final StackId stackId_1_0 = new StackId("HDP-1.0");

  final RepositoryVersion m_repositoryVersion = Mockito.mock(RepositoryVersion.class);
  final RepositoryVersionEntity m_repositoryVersionEntity = Mockito.mock(RepositoryVersionEntity.class);

  @Before
  public void before() throws Exception {
    Cluster cluster = EasyMock.createMock(Cluster.class);

    expect(cluster.getCurrentStackVersion()).andReturn(stackId_1_0).anyTimes();
    expect(clusters.getCluster((String) anyObject())).andReturn(cluster).anyTimes();

    expect(cluster.getServices()).andReturn(new HashMap<String, Service>() {{
      put("HDFS", EasyMock.createMock(Service.class));
    }}).anyTimes();


    m_configMap.put(CONFIG_PROPERTY, "1024m");
    Config config = EasyMock.createMock(Config.class);
    expect(config.getProperties()).andReturn(m_configMap).anyTimes();

    expect(cluster.getDesiredConfigByType(CONFIG_TYPE)).andReturn(config).anyTimes();

    StackId stackId = new StackId("HDP", "1.1");
    String version = "1.1.0.0-1234";

    Mockito.when(m_repositoryVersion.getId()).thenReturn(1L);
    Mockito.when(m_repositoryVersion.getRepositoryType()).thenReturn(RepositoryType.STANDARD);
    Mockito.when(m_repositoryVersion.getStackId()).thenReturn(stackId.toString());
    Mockito.when(m_repositoryVersion.getVersion()).thenReturn(version);

    Mockito.when(m_repositoryVersionEntity.getType()).thenReturn(RepositoryType.STANDARD);
    Mockito.when(m_repositoryVersionEntity.getVersion()).thenReturn(version);
    Mockito.when(m_repositoryVersionEntity.getStackId()).thenReturn(stackId);

    replay(clusters, cluster, config);
  }

  @Test
  public void testPerform() throws Exception {
    ConfigurationMergeCheck cmc = new ConfigurationMergeCheck();

    final RepositoryVersionDAO repositoryVersionDAO = EasyMock.createMock(RepositoryVersionDAO.class);
    expect(repositoryVersionDAO.findByStackNameAndVersion("HDP", "1.0")).andReturn(createFor("1.0")).anyTimes();
    expect(repositoryVersionDAO.findByStackNameAndVersion("HDP", "1.1.0.0-1234")).andReturn(createFor("1.1")).anyTimes();

    replay(repositoryVersionDAO);

    cmc.repositoryVersionDaoProvider = new Provider<RepositoryVersionDAO>() {
      @Override
      public RepositoryVersionDAO get() {
        return repositoryVersionDAO;
      }
    };

    cmc.clustersProvider = new Provider<Clusters>() {
      @Override
      public Clusters get() {
        return clusters;
      }
    };

    cmc.m_mergeHelper = new ConfigMergeHelper();
    Field field = ConfigMergeHelper.class.getDeclaredField("m_clusters");
    field.setAccessible(true);
    field.set(cmc.m_mergeHelper, cmc.clustersProvider);

    final AmbariMetaInfo ami = EasyMock.createMock(AmbariMetaInfo.class);


    field = ConfigMergeHelper.class.getDeclaredField("m_ambariMetaInfo");
    field.setAccessible(true);
    field.set(cmc.m_mergeHelper, new Provider<AmbariMetaInfo>() {
      @Override
      public AmbariMetaInfo get() {
        return ami;
      }
    });

    PropertyInfo pi10 = new PropertyInfo();
    pi10.setFilename(CONFIG_FILE);
    pi10.setName(CONFIG_PROPERTY);
    pi10.setValue("1024");

    PropertyInfo pi11 = new PropertyInfo();
    pi11.setFilename(CONFIG_FILE);
    pi11.setName(CONFIG_PROPERTY);
    pi11.setValue("1024");

    expect(ami.getServiceProperties("HDP", "1.0", "HDFS")).andReturn(
        Collections.singleton(pi10)).anyTimes();

    expect(ami.getServiceProperties("HDP", "1.1", "HDFS")).andReturn(
        Collections.singleton(pi11)).anyTimes();

    expect(ami.getStackProperties(anyObject(String.class), anyObject(String.class))).andReturn(
        Collections.emptySet()).anyTimes();

    replay(ami);

    ClusterInformation clusterInformation = new ClusterInformation("cluster", false, null, null, null);
    UpgradeCheckRequest request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING,
        m_repositoryVersion, null, null);

    UpgradeCheckResult check = cmc.perform(request);
    Assert.assertEquals("Expect no warnings", 0, check.getFailedOn().size());

    m_configMap.put(CONFIG_PROPERTY, "1025m");
    pi11.setValue("1026");
    check = cmc.perform(request);
    Assert.assertEquals("Expect warning when user-set has changed from new default",
        1, check.getFailedOn().size());
    Assert.assertEquals(1, check.getFailedDetail().size());
    ConfigurationMergeCheck.MergeDetail detail = (ConfigurationMergeCheck.MergeDetail) check.getFailedDetail().get(0);
    Assert.assertEquals("1025m", detail.current);
    Assert.assertEquals("1026m", detail.new_stack_value);
    Assert.assertEquals("1025m", detail.result_value);
    Assert.assertEquals(CONFIG_TYPE, detail.type);
    Assert.assertEquals(CONFIG_PROPERTY, detail.property);

    pi11.setName(CONFIG_PROPERTY + ".foo");
    check = cmc.perform(request);
    Assert.assertEquals("Expect no warning when user new stack is empty",
        0, check.getFailedOn().size());
    Assert.assertEquals(0, check.getFailedDetail().size());

    pi11.setName(CONFIG_PROPERTY);
    pi10.setName(CONFIG_PROPERTY + ".foo");
    check = cmc.perform(request);
    Assert.assertEquals("Expect warning when user old stack is empty, and value changed",
        1, check.getFailedOn().size());
    Assert.assertEquals(1, check.getFailedDetail().size());
    detail = (ConfigurationMergeCheck.MergeDetail) check.getFailedDetail().get(0);
    Assert.assertEquals("1025m", detail.current);
    Assert.assertEquals("1026m", detail.new_stack_value);
  }

  private RepositoryVersionEntity createFor(final String stackVersion) {
    RepositoryVersionEntity entity = new RepositoryVersionEntity();

    entity.setStack(new StackEntity() {
      @Override
      public String getStackVersion() {
        return stackVersion;
      }

      @Override
      public String getStackName() {
        return "HDP";
      }
    });

    return entity;
  }
}
