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
package org.apache.ambari.server.state.alerts;


import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.alert.AlertDefinition;
import org.apache.ambari.server.state.alert.AlertDefinitionHash;
import org.apache.ambari.server.state.alert.Scope;
import org.apache.ambari.server.state.alert.SourceType;
import org.apache.commons.codec.binary.Hex;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

import junit.framework.TestCase;

/**
 * Tests for {@link AlertDefinitionHash}.
 */
@Category({ category.AlertTest.class})
public class AlertDefinitionHashTest extends TestCase {

  private AlertDefinitionHash m_hash;
  private Clusters m_mockClusters;
  private Cluster m_mockCluster;
  private AlertDefinitionDAO m_mockDao;
  private Injector m_injector;

  private static final String CLUSTERNAME = "cluster1";
  private static final String HOSTNAME = "c6401.ambari.apache.org";

  private List<AlertDefinitionEntity> m_agentDefinitions;
  private AlertDefinitionEntity m_hdfsService;
  AlertDefinitionEntity m_hdfsHost;
  private ConfigHelper m_configHelper;

  /**
   *
   */
  @Override
  @Before
  @SuppressWarnings("unchecked")
  protected void setUp() throws Exception {
    super.setUp();

    m_injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    m_mockClusters = m_injector.getInstance(Clusters.class);
    m_mockCluster = m_injector.getInstance(Cluster.class);
    m_mockDao = m_injector.getInstance(AlertDefinitionDAO.class);

    // add HDFS/NN
    List<ServiceComponentHost> serviceComponentHosts = new ArrayList<>();
    ServiceComponentHost sch = EasyMock.createNiceMock(ServiceComponentHost.class);
    expect(sch.getServiceName()).andReturn("HDFS").anyTimes();
    expect(sch.getServiceComponentName()).andReturn("NAMENODE").anyTimes();
    expect(sch.getHostName()).andReturn(HOSTNAME).anyTimes();
    EasyMock.replay(sch);
    serviceComponentHosts.add(sch);

    // add HDFS/DN
    sch = EasyMock.createNiceMock(ServiceComponentHost.class);
    expect(sch.getServiceName()).andReturn("HDFS").anyTimes();
    expect(sch.getServiceComponentName()).andReturn("DATANODE").anyTimes();
    expect(sch.getHostName()).andReturn(HOSTNAME).anyTimes();
    EasyMock.replay(sch);
    serviceComponentHosts.add(sch);

    Map<String, ServiceComponentHost> mapComponentHosts = new HashMap<>();
    ServiceComponentHost host = EasyMock.createNiceMock(ServiceComponentHost.class);
    expect(host.getHostName()).andReturn(HOSTNAME).anyTimes();
    mapComponentHosts.put(HOSTNAME, host);

    Map<String, ServiceComponent> serviceComponents = new HashMap<>();
    ServiceComponent namenode = EasyMock.createNiceMock(ServiceComponent.class);
    expect(namenode.getServiceComponentHosts()).andReturn(mapComponentHosts).anyTimes();
    expect(namenode.isMasterComponent()).andReturn(true).anyTimes();
    serviceComponents.put("NAMENODE", namenode);

    // create HDFS for the cluster
    Map<String, Service> services = new HashMap<>();
    String hdfsName = "HDFS";
    Service hdfs = EasyMock.createNiceMock(Service.class);
    expect(hdfs.getName()).andReturn("HDFS").anyTimes();
    expect(hdfs.getServiceComponents()).andReturn(serviceComponents).anyTimes();
    services.put(hdfsName, hdfs);

    // replay
    EasyMock.replay(hdfs, host, namenode);

    // Clusters mock
    expect(m_mockClusters.getCluster((String) anyObject())).andReturn(
        m_mockCluster).atLeastOnce();

    expect(m_mockClusters.getClusterById(EasyMock.anyInt())).andReturn(
        m_mockCluster).atLeastOnce();

    Map<String, Host> clusterHosts = new HashMap<>();
    clusterHosts.put(HOSTNAME, null);

    expect(m_mockClusters.getHostsForCluster(EasyMock.eq(CLUSTERNAME))).andReturn(
        clusterHosts).anyTimes();

    // cluster mock
    expect(m_mockCluster.getClusterId()).andReturn(Long.valueOf(1)).anyTimes();
    expect(m_mockCluster.getClusterName()).andReturn(CLUSTERNAME).anyTimes();
    expect(m_mockCluster.getServices()).andReturn(services).anyTimes();
    expect(
        m_mockCluster.getServiceComponentHosts(EasyMock.anyObject(String.class))).andReturn(
        serviceComponentHosts).anyTimes();

    m_hdfsService = new AlertDefinitionEntity();
    m_hdfsService.setDefinitionId(1L);
    m_hdfsService.setClusterId(1L);
    m_hdfsService.setHash(UUID.randomUUID().toString());
    m_hdfsService.setServiceName("HDFS");
    m_hdfsService.setComponentName("NAMENODE");
    m_hdfsService.setScope(Scope.SERVICE);
    m_hdfsService.setScheduleInterval(1);

    m_hdfsHost = new AlertDefinitionEntity();
    m_hdfsHost.setDefinitionId(2L);
    m_hdfsHost.setClusterId(1L);
    m_hdfsHost.setHash(UUID.randomUUID().toString());
    m_hdfsHost.setServiceName("HDFS");
    m_hdfsHost.setComponentName("DATANODE");
    m_hdfsHost.setScope(Scope.HOST);
    m_hdfsHost.setScheduleInterval(1);

    AlertDefinitionEntity agentScoped = new AlertDefinitionEntity();
    agentScoped.setDefinitionId(3L);
    agentScoped.setClusterId(1L);
    agentScoped.setHash(UUID.randomUUID().toString());
    agentScoped.setServiceName("AMBARI");
    agentScoped.setComponentName("AMBARI_AGENT");
    agentScoped.setScope(Scope.HOST);
    agentScoped.setScheduleInterval(1);

    EasyMock.expect(
        m_mockDao.findByServiceMaster(EasyMock.anyInt(),
            (Set<String>) EasyMock.anyObject())).andReturn(
        Collections.singletonList(m_hdfsService)).anyTimes();

    EasyMock.expect(
        m_mockDao.findByServiceComponent(EasyMock.anyInt(),
            EasyMock.anyObject(String.class), EasyMock.anyObject(String.class))).andReturn(
        Collections.singletonList(m_hdfsHost)).anyTimes();

    m_agentDefinitions = new ArrayList<>();
    m_agentDefinitions.add(agentScoped);
    EasyMock.expect(m_mockDao.findAgentScoped(EasyMock.anyInt())).andReturn(
        m_agentDefinitions).anyTimes();

    EasyMock.replay(m_mockClusters, m_mockCluster, m_mockDao);
    m_hash = m_injector.getInstance(AlertDefinitionHash.class);

    // configHelper mock
    m_configHelper = m_injector.getInstance(ConfigHelper.class);
    EasyMock.expect(m_configHelper.getEffectiveDesiredTags((Cluster) anyObject(), EasyMock.anyString())).andReturn(new HashMap<>()).anyTimes();
    EasyMock.expect(m_configHelper.getEffectiveConfigProperties((Cluster) anyObject(), (Map<String, Map<String, String>>) anyObject())).andReturn(new HashMap<>()).anyTimes();
    EasyMock.replay(m_configHelper);
  }

  /**
   *
   */
  @Override
  @After
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Test method for {@link org.apache.ambari.server.state.alert.AlertDefinitionHash#getHash(java.lang.String, java.lang.String)}.
   */
  @Test
  public void testGetHash() {
    String hash = m_hash.getHash(CLUSTERNAME, HOSTNAME);
    assertNotNull(hash);
    assertNotSame(AlertDefinitionHash.NULL_MD5_HASH, hash);
    assertEquals(hash, m_hash.getHash(CLUSTERNAME, HOSTNAME));
  }

  /**
   * Test method for {@link org.apache.ambari.server.state.alert.AlertDefinitionHash#getAlertDefinitions(java.lang.String, java.lang.String)}.
   */
  @Test
  public void testGetAlertDefinitions() {
    List<AlertDefinition> definitions = m_hash.getAlertDefinitions(
        CLUSTERNAME, HOSTNAME);

    assertEquals(3, definitions.size());
  }

  /**
   * Test {@link AlertDefinitionHash#invalidateAll()}.
   */
  @Test
  public void testInvalidateAll() {
    String hash = m_hash.getHash(CLUSTERNAME, HOSTNAME);
    assertNotNull(hash);

    m_hash.invalidateAll();

    String newHash = m_hash.getHash(CLUSTERNAME, HOSTNAME);
    assertEquals(hash, newHash);

    m_hash.invalidateAll();

    // add a new alert definition, forcing new hash
    AlertDefinitionEntity agentScoped = new AlertDefinitionEntity();
    agentScoped.setDefinitionId(System.currentTimeMillis());
    agentScoped.setClusterId(1L);
    agentScoped.setHash(UUID.randomUUID().toString());
    agentScoped.setServiceName("AMBARI");
    agentScoped.setComponentName("AMBARI_AGENT");
    agentScoped.setScope(Scope.HOST);
    agentScoped.setScheduleInterval(1);

    m_agentDefinitions.add(agentScoped);

    newHash = m_hash.getHash(CLUSTERNAME, HOSTNAME);
    assertNotSame(hash, newHash);
  }

  /**
   * Test {@link AlertDefinitionHash#isHashCached(String,String)}.
   */
  @Test
  public void testIsHashCached() {
    assertFalse(m_hash.isHashCached(CLUSTERNAME, HOSTNAME));
    String hash = m_hash.getHash(CLUSTERNAME, HOSTNAME);
    assertNotNull(hash);
    assertTrue(m_hash.isHashCached(CLUSTERNAME, HOSTNAME));

    m_hash.invalidate(HOSTNAME);
    assertFalse(m_hash.isHashCached(CLUSTERNAME, HOSTNAME));
    hash = m_hash.getHash(CLUSTERNAME, HOSTNAME);
    assertNotNull(hash);
    assertTrue(m_hash.isHashCached(CLUSTERNAME, HOSTNAME));

    m_hash.invalidateAll();
    assertFalse(m_hash.isHashCached(CLUSTERNAME, HOSTNAME));
    hash = m_hash.getHash(CLUSTERNAME, HOSTNAME);
    assertNotNull(hash);
    assertTrue(m_hash.isHashCached(CLUSTERNAME, HOSTNAME));
  }

  /**
   * Test {@link AlertDefinitionHash#invalidateHosts(AlertDefinitionEntity)}.
   */
  @Test
  public void testInvalidateHosts() {
    assertFalse(m_hash.isHashCached(CLUSTERNAME, HOSTNAME));
    String hash = m_hash.getHash(CLUSTERNAME, HOSTNAME);
    assertNotNull(hash);
    assertTrue(m_hash.isHashCached(CLUSTERNAME, HOSTNAME));

    Set<String> invalidatedHosts = m_hash.invalidateHosts(m_hdfsHost);
    assertFalse(m_hash.isHashCached(CLUSTERNAME, HOSTNAME));
    assertNotNull(invalidatedHosts);
    assertEquals(1, invalidatedHosts.size());
    assertTrue(invalidatedHosts.contains(HOSTNAME));
  }

  /**
   *
   */
  @Test
  public void testInvalidateHost() {
    assertFalse(m_hash.isHashCached(CLUSTERNAME, HOSTNAME));
    assertFalse(m_hash.isHashCached("foo", HOSTNAME));

    String hash = m_hash.getHash(CLUSTERNAME, HOSTNAME);
    assertNotNull(hash);
    assertTrue(m_hash.isHashCached(CLUSTERNAME, HOSTNAME));
    assertFalse(m_hash.isHashCached("foo", HOSTNAME));

    // invalidate the fake cluster and ensure the original cluster still
    // contains a cached valie
    m_hash.invalidate("foo", HOSTNAME);
    assertTrue(m_hash.isHashCached(CLUSTERNAME, HOSTNAME));
    assertFalse(m_hash.isHashCached("foo", HOSTNAME));

    m_hash.invalidateAll();
    assertFalse(m_hash.isHashCached(CLUSTERNAME, HOSTNAME));
    assertFalse(m_hash.isHashCached("foo", HOSTNAME));
  }

  @Test
  public void testAggregateIgnored() {
    Set<String> associatedHosts = m_hash.getAssociatedHosts(m_mockCluster,
        SourceType.AGGREGATE, "definitionName", "HDFS", null);

    assertEquals(0, associatedHosts.size());

    associatedHosts = m_hash.getAssociatedHosts(m_mockCluster, SourceType.PORT,
        "definitionName", "HDFS", null);

    assertEquals(1, associatedHosts.size());
  }

  @Test
  public void testHashingAlgorithm() throws Exception {
    List<String> uuids = new ArrayList<>();
    uuids.add(m_hdfsService.getHash());
    uuids.add(m_hdfsHost.getHash());

    for (AlertDefinitionEntity entity : m_agentDefinitions) {
      uuids.add(entity.getHash());
    }

    Collections.sort(uuids);

    MessageDigest digest = MessageDigest.getInstance("MD5");
    for (String uuid : uuids) {
      digest.update(uuid.getBytes());
    }

    byte[] hashBytes = digest.digest();
    String expected = Hex.encodeHexString(hashBytes);

    assertEquals(expected, m_hash.getHash(CLUSTERNAME, HOSTNAME));
  }

  /**
   *
   */
  private class MockModule implements Module {
    /**
     *
     */
    @Override
    public void configure(Binder binder) {
      Cluster cluster = EasyMock.createNiceMock(Cluster.class);
      EasyMock.expect(cluster.getAllConfigs()).andReturn(
        new ArrayList<>()).anyTimes();

      binder.bind(Clusters.class).toInstance(
          EasyMock.createNiceMock(Clusters.class));

      binder.bind(Cluster.class).toInstance(cluster);

      binder.bind(AlertDefinitionDAO.class).toInstance(
          EasyMock.createNiceMock(AlertDefinitionDAO.class));

      binder.bind(ConfigHelper.class).toInstance(
          EasyMock.createNiceMock(ConfigHelper.class));
    }
  }
}
