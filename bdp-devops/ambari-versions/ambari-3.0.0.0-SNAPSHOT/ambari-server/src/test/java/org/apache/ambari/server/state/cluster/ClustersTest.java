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

package org.apache.ambari.server.state.cluster;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.agent.AgentEnv;
import org.apache.ambari.server.agent.HostInfo;
import org.apache.ambari.server.controller.internal.ProvisionClusterRequest;
import org.apache.ambari.server.events.HostRegisteredEvent;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.ClusterServiceDAO;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.TopologyRequestDAO;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.AgentVersion;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.host.HostRegistrationRequestEvent;
import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.apache.ambari.server.topology.HostRequest;
import org.apache.ambari.server.topology.LogicalRequest;
import org.apache.ambari.server.topology.PersistedState;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.topology.TopologyRequest;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

import junit.framework.Assert;

public class ClustersTest {

  private Clusters clusters;
  private Injector injector;
  @Inject
  private OrmTestHelper helper;
  @Inject
  private HostDAO hostDAO;

  @Inject
  private TopologyRequestDAO topologyRequestDAO;


  @Inject
  private PersistedState persistedState;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(Modules.override(new InMemoryDefaultTestModule()).with(new MockModule()));
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    injector.injectMembers(this);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  private void setOsFamily(Host host, String osFamily, String osVersion) {
	  Map<String, String> hostAttributes = new HashMap<>();
	  hostAttributes.put("os_family", osFamily);
	  hostAttributes.put("os_release_version", osVersion);

	  host.setHostAttributes(hostAttributes);
  }

  @Test
  public void testGetInvalidCluster() throws AmbariException {
    try {
      clusters.getCluster("foo");
      fail("Exception should be thrown on invalid get");
    }
    catch (ClusterNotFoundException e) {
      // Expected
    }

  }

  @Test
  public void testAddAndGetCluster() throws AmbariException {
    StackId stackId = new StackId("HDP-2.1.1");

    helper.createStack(stackId);

    String c1 = "foo";
    String c2 = "foo";
    clusters.addCluster(c1, stackId);

    try {
      clusters.addCluster(c1, stackId);
      fail("Exception should be thrown on invalid add");
    }
    catch (AmbariException e) {
      // Expected
    }

    try {
      clusters.addCluster(c2, stackId);
      fail("Exception should be thrown on invalid add");
    }
    catch (AmbariException e) {
      // Expected
    }

    c2 = "foo2";
    clusters.addCluster(c2, stackId);

    Assert.assertNotNull(clusters.getCluster(c1));
    Assert.assertNotNull(clusters.getCluster(c2));

    Assert.assertEquals(c1, clusters.getCluster(c1).getClusterName());
    Assert.assertEquals(c2, clusters.getCluster(c2).getClusterName());

    Map<String, Cluster> verifyClusters = clusters.getClusters();
    Assert.assertTrue(verifyClusters.containsKey(c1));
    Assert.assertTrue(verifyClusters.containsKey(c2));
    Assert.assertNotNull(verifyClusters.get(c1));
    Assert.assertNotNull(verifyClusters.get(c2));

    Cluster c = clusters.getCluster(c1);
    c.setClusterName("foobar");
    long cId = c.getClusterId();

    Cluster changed = clusters.getCluster("foobar");
    Assert.assertNotNull(changed);
    Assert.assertEquals(cId, changed.getClusterId());

    Assert.assertEquals("foobar",
        clusters.getClusterById(cId).getClusterName());

  }

  @Test
  public void testAddAndGetClusterWithSecurityType() throws AmbariException {
    StackId stackId = new StackId("HDP-2.1.1");

    helper.createStack(stackId);

    String c1 = "foo";
    SecurityType securityType = SecurityType.KERBEROS;
    clusters.addCluster(c1, stackId, securityType);

    Assert.assertNotNull(clusters.getCluster(c1));

    Assert.assertEquals(c1, clusters.getCluster(c1).getClusterName());
    Assert.assertEquals(securityType, clusters.getCluster(c1).getSecurityType());
  }

  @Test
  public void testAddAndGetHost() throws AmbariException {
    String h1 = "h1";
    String h2 = "h2";
    String h3 = "h3";

    clusters.addHost(h1);

    try {
      clusters.addHost(h1);
      fail("Expected exception on duplicate host entry");
    } catch (Exception e) {
      // Expected
    }

    clusters.addHost(h2);
    clusters.addHost(h3);

    List<Host> hosts = clusters.getHosts();
    Assert.assertEquals(3, hosts.size());

    Assert.assertNotNull(clusters.getHost(h1));
    Assert.assertNotNull(clusters.getHost(h2));
    Assert.assertNotNull(clusters.getHost(h3));

    Host h = clusters.getHost(h2);
    Assert.assertNotNull(h);

    try {
      clusters.getHost("foo");
      fail("Expected error for unknown host");
    } catch (HostNotFoundException e) {
      // Expected
    }

  }

  @Test
  public void testClusterHostMapping() throws AmbariException {
    String c1 = "c1";
    String c2 = "c2";
    String h1 = "h1";
    String h2 = "h2";
    String h3 = "h3";
    String h4 = "h4";

    try {
      clusters.mapHostToCluster(h1, c1);
      fail("Expected exception for invalid cluster/host");
    } catch (Exception e) {
      // Expected
    }

    StackId stackId = new StackId("HDP-0.1");

    helper.createStack(stackId);

    clusters.addCluster(c1, stackId);
    clusters.addCluster(c2, stackId);

    Cluster cluster1 = clusters.getCluster(c1);
    Cluster cluster2 = clusters.getCluster(c2);
    Assert.assertNotNull(clusters.getCluster(c1));
    Assert.assertNotNull(clusters.getCluster(c2));

    cluster1.setDesiredStackVersion(stackId);
    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());

    try {
      clusters.mapHostToCluster(h1, c1);
      fail("Expected exception for invalid host");
    } catch (Exception e) {
      // Expected
    }

    clusters.addHost(h1);
    clusters.addHost(h2);
    clusters.addHost(h3);
    Assert.assertNotNull(clusters.getHost(h1));
    setOsFamily(clusters.getHost(h1), "redhat", "6.4");
    setOsFamily(clusters.getHost(h2), "redhat", "5.9");
    setOsFamily(clusters.getHost(h3), "redhat", "6.4");

    try {
        clusters.getClustersForHost(h4);
        fail("Expected exception for invalid host");
    } catch (HostNotFoundException e) {
          // Expected
    }

    Set<Cluster> c = clusters.getClustersForHost(h3);
    Assert.assertEquals(0, c.size());

    clusters.mapHostToCluster(h1, c1);
    clusters.mapHostToCluster(h2, c1);

    try {
      clusters.mapHostToCluster(h1, c1);
      fail("Expected exception for duplicate");
    } catch (DuplicateResourceException e) {
      // expected
    }

    /* make sure 2 host mapping to same cluster are the same cluster objects */

    Cluster c3 = (Cluster) clusters.getClustersForHost(h1).toArray()[0];
    Cluster c4 = (Cluster) clusters.getClustersForHost(h2).toArray()[0];

    Assert.assertEquals(c3, c4);
    Set<String> hostnames = new HashSet<>();
    hostnames.add(h1);
    hostnames.add(h2);

    clusters.mapAndPublishHostsToCluster(hostnames, c2);

    c = clusters.getClustersForHost(h1);
    Assert.assertEquals(2, c.size());

    c = clusters.getClustersForHost(h2);
    Assert.assertEquals(2, c.size());


    // TODO write test for getHostsForCluster
    Map<String, Host> hostsForC1 = clusters.getHostsForCluster(c1);
    Assert.assertEquals(2, hostsForC1.size());
    Assert.assertTrue(hostsForC1.containsKey(h1));
    Assert.assertTrue(hostsForC1.containsKey(h2));
    Assert.assertNotNull(hostsForC1.get(h1));
    Assert.assertNotNull(hostsForC1.get(h2));
  }

  @Test
  public void testDebugDump() throws AmbariException {
    String c1 = "c1";
    String c2 = "c2";
    String h1 = "h1";
    String h2 = "h2";
    String h3 = "h3";

    StackId stackId = new StackId("HDP-0.1");

    helper.createStack(stackId);

    clusters.addCluster(c1, stackId);
    clusters.addCluster(c2, stackId);
    Cluster cluster1 = clusters.getCluster(c1);
    Cluster cluster2 = clusters.getCluster(c2);
    Assert.assertNotNull(clusters.getCluster(c1));
    Assert.assertNotNull(clusters.getCluster(c2));

    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());

    clusters.addHost(h1);
    clusters.addHost(h2);
    clusters.addHost(h3);
    setOsFamily(clusters.getHost(h1), "redhat", "6.4");
    setOsFamily(clusters.getHost(h2), "redhat", "5.9");
    setOsFamily(clusters.getHost(h3), "redhat", "6.4");
    clusters.mapHostToCluster(h1, c1);
    clusters.mapHostToCluster(h2, c1);

    StringBuilder sb = new StringBuilder();
    clusters.debugDump(sb);
    // TODO verify dump output?
  }

  @Test
  public void testDeleteCluster() throws Exception {
    String c1 = "c1";
    final String h1 = "h1";
    final String h2 = "h2";

    StackId stackId = new StackId("HDP-0.1");

    helper.createStack(stackId);

    clusters.addCluster(c1, stackId);

    Cluster cluster = clusters.getCluster(c1);

    cluster.setDesiredStackVersion(stackId);
    cluster.setCurrentStackVersion(stackId);

    RepositoryVersionEntity repositoryVersion = helper.getOrCreateRepositoryVersion(stackId,
        stackId.getStackVersion());

    final Config config1 = injector.getInstance(ConfigFactory.class).createNew(cluster, "t1", "1",
        new HashMap<String, String>() {{
          put("prop1", "val1");
        }}, new HashMap<>());

    Config config2 = injector.getInstance(ConfigFactory.class).createNew(cluster, "t1", "2",
        new HashMap<String, String>() {{
          put("prop2", "val2");
        }}, new HashMap<>());

    // cluster desired config
    cluster.addDesiredConfig("_test", Collections.singleton(config1));

    clusters.addHost(h1);
    clusters.addHost(h2);


    Host host1 = clusters.getHost(h1);
    Host host2 = clusters.getHost(h2);

    setOsFamily(host1, "centos", "5.9");
    setOsFamily(host2, "centos", "5.9");

    clusters.mapAndPublishHostsToCluster(new HashSet<String>() {
      {
        addAll(Arrays.asList(h1, h2));
      }
    }, c1);
    clusters.updateHostMappings(host1);
    clusters.updateHostMappings(host2);

    // host config override
    host1.addDesiredConfig(cluster.getClusterId(), true, "_test", config2);

    Service hdfs = cluster.addService("HDFS", repositoryVersion);

    Assert.assertNotNull(injector.getInstance(ClusterServiceDAO.class).findByClusterAndServiceNames(c1, "HDFS"));

    ServiceComponent nameNode = hdfs.addServiceComponent("NAMENODE");
    ServiceComponent dataNode = hdfs.addServiceComponent("DATANODE");

    ServiceComponent serviceCheckNode = hdfs.addServiceComponent("HDFS_CLIENT");

    ServiceComponentHost nameNodeHost = nameNode.addServiceComponentHost(h1);
    HostEntity nameNodeHostEntity = hostDAO.findByName(nameNodeHost.getHostName());
    Assert.assertNotNull(nameNodeHostEntity);

    ServiceComponentHost dataNodeHost = dataNode.addServiceComponentHost(h2);

    ServiceComponentHost serviceCheckNodeHost = serviceCheckNode.addServiceComponentHost(h2);
    serviceCheckNodeHost.setState(State.UNKNOWN);

    Assert.assertNotNull(injector.getInstance(HostComponentStateDAO.class).findByIndex(
      nameNodeHost.getClusterId(), nameNodeHost.getServiceName(),
      nameNodeHost.getServiceComponentName(), nameNodeHostEntity.getHostId()));

    Assert.assertNotNull(injector.getInstance(HostComponentDesiredStateDAO.class).findByIndex(
      nameNodeHost.getClusterId(),
      nameNodeHost.getServiceName(),
      nameNodeHost.getServiceComponentName(),
      nameNodeHostEntity.getHostId()
    ));
    Assert.assertEquals(2, injector.getProvider(EntityManager.class).get().createQuery("SELECT config FROM ClusterConfigEntity config").getResultList().size());
    Assert.assertEquals(1, injector.getProvider(EntityManager.class).get().createQuery("SELECT state FROM ClusterStateEntity state").getResultList().size());
    Assert.assertEquals(1, injector.getProvider(EntityManager.class).get().createQuery("SELECT config FROM ClusterConfigEntity config WHERE config.selected = 1").getResultList().size());

    // add topology request
    Blueprint bp = createNiceMock(Blueprint.class);
    expect(bp.getName()).andReturn("TestBluePrint").anyTimes();

    Configuration clusterConfig = new Configuration(
      Maps.newHashMap(),
      Maps.newHashMap()
      );

    Map<String, HostGroupInfo> hostGroups = Maps.newHashMap();

    ProvisionClusterRequest topologyRequest = createNiceMock(ProvisionClusterRequest.class);
    expect(topologyRequest.getType()).andReturn(TopologyRequest.Type.PROVISION).anyTimes();
    expect(topologyRequest.getBlueprint()).andReturn(bp).anyTimes();
    expect(topologyRequest.getClusterId()).andReturn(cluster.getClusterId()).anyTimes();
    expect(topologyRequest.getConfiguration()).andReturn(clusterConfig).anyTimes();
    expect(topologyRequest.getDescription()).andReturn("Test description").anyTimes();
    expect(topologyRequest.getHostGroupInfo()).andReturn(hostGroups).anyTimes();


    replay(bp, topologyRequest);

    persistedState.persistTopologyRequest(topologyRequest);

    Assert.assertEquals(1, topologyRequestDAO.findByClusterId(cluster.getClusterId()).size());

    clusters.deleteCluster(c1);

    Assert.assertEquals(2, hostDAO.findAll().size());
    Assert.assertNull(injector.getInstance(HostComponentStateDAO.class).findByIndex(
      nameNodeHost.getClusterId(), nameNodeHost.getServiceName(),
      nameNodeHost.getServiceComponentName(), nameNodeHostEntity.getHostId()));

    Assert.assertNull(injector.getInstance(HostComponentDesiredStateDAO.class).findByIndex(
      nameNodeHost.getClusterId(), nameNodeHost.getServiceName(),
      nameNodeHost.getServiceComponentName(), nameNodeHostEntity.getHostId()
    ));
    Assert.assertEquals(0, injector.getProvider(EntityManager.class).get().createQuery("SELECT config FROM ClusterConfigEntity config").getResultList().size());
    Assert.assertEquals(0, injector.getProvider(EntityManager.class).get().createQuery("SELECT state FROM ClusterStateEntity state").getResultList().size());
    Assert.assertEquals(0, topologyRequestDAO.findByClusterId(cluster.getClusterId()).size());
  }

  @Test
  public void testNullHostNamesInTopologyRequests() throws AmbariException {
    final String hostName = "myhost";
    final String clusterName = "mycluster";

    Cluster cluster = createCluster(clusterName);
    addHostToCluster(hostName, clusterName);
    addHostToCluster(hostName + "2", clusterName);
    addHostToCluster(hostName + "3", clusterName);

    createTopologyRequest(cluster, hostName);
    clusters.deleteHost(hostName);
    for(Host h : cluster.getHosts()) {
      if(hostName.equals(h.getHostName())) {
        Assert.fail("Host is expected to be deleted");
      }
    }
  }

  /**
   * Tests that {@link HostRegisteredEvent} properly updates the
   * {@link Clusters} in-memory mapping of hostIds to hosts.
   *
   * @throws AmbariException
   */
  @Test
  public void testHostRegistrationPopulatesIdMapping() throws Exception {
    String clusterName = UUID.randomUUID().toString();
    String hostName = UUID.randomUUID().toString();

    // required so that the event which does the work is executed synchornously
    EventBusSynchronizer.synchronizeAmbariEventPublisher(injector);

    Cluster cluster = createCluster(clusterName);
    Assert.assertNotNull(cluster);

    addHostToCluster(hostName, clusterName);
    Host host = clusters.getHost(hostName);
    Assert.assertNotNull(host);
    long currentTime = System.currentTimeMillis();

    HostRegistrationRequestEvent registrationEvent = new HostRegistrationRequestEvent(
        host.getHostName(),
        new AgentVersion(""), currentTime, new HostInfo(), new AgentEnv(), currentTime);

    host.handleEvent(registrationEvent);

    Long hostId = host.getHostId();
    Assert.assertNotNull(hostId);

    host = clusters.getHostById(hostId);
    Assert.assertNotNull(host);
  }

  private void createTopologyRequest(Cluster cluster, String hostName) {
    final String groupName = "MyHostGroup";

    // add topology request
    Blueprint bp = createNiceMock(Blueprint.class);
    expect(bp.getName()).andReturn("TestBluePrint").anyTimes();

    Configuration clusterConfig = new Configuration(
      Maps.newHashMap(),
      Maps.newHashMap()
    );

    Map<String, HostGroupInfo> hostGroups = new HashMap<>();
    HostGroupInfo hostGroupInfo = new HostGroupInfo(groupName);
    hostGroupInfo.setConfiguration(clusterConfig);
    hostGroupInfo.addHost(hostName);
    hostGroupInfo.addHost(hostName + "2");
    hostGroupInfo.addHost(hostName + "3");
    hostGroups.put(groupName, hostGroupInfo);

    ProvisionClusterRequest topologyRequest = createNiceMock(ProvisionClusterRequest.class);
    expect(topologyRequest.getType()).andReturn(TopologyRequest.Type.PROVISION).anyTimes();
    expect(topologyRequest.getBlueprint()).andReturn(bp).anyTimes();
    expect(topologyRequest.getClusterId()).andReturn(cluster.getClusterId()).anyTimes();
    expect(topologyRequest.getConfiguration()).andReturn(clusterConfig).anyTimes();
    expect(topologyRequest.getDescription()).andReturn("Test description").anyTimes();
    expect(topologyRequest.getHostGroupInfo()).andReturn(hostGroups).anyTimes();

    replay(bp, topologyRequest);

    persistedState.persistTopologyRequest(topologyRequest);

    createTopologyLogicalRequest(cluster, hostName);
  }

  private HostRequest createHostRequest(long hrId, String hostName) {
    HostRequest hr = createNiceMock(HostRequest.class);
    expect(hr.getId()).andReturn(hrId).anyTimes();
    expect(hr.getHostgroupName()).andReturn("MyHostGroup").anyTimes();
    expect(hr.getHostName()).andReturn(hostName).anyTimes();
    expect(hr.getStageId()).andReturn(1L);
    expect(hr.getTopologyTasks()).andReturn(Collections.emptyList());

    replay(hr);
    return hr;
  }

  private void createTopologyLogicalRequest(Cluster cluster, String hostName) {
    Collection<HostRequest> hostRequests = new ArrayList<>();
    hostRequests.add(createHostRequest(1L, null));
    hostRequests.add(createHostRequest(2L, hostName));
    hostRequests.add(createHostRequest(3L, null));
    hostRequests.add(createHostRequest(4L, hostName + "2"));
    hostRequests.add(createHostRequest(5L, null));
    hostRequests.add(createHostRequest(6L, hostName + "3"));

    Long requestId = topologyRequestDAO.findByClusterId(cluster.getClusterId()).get(0).getId();
    LogicalRequest logicalRequest = createNiceMock(LogicalRequest.class);
    expect(logicalRequest.getHostRequests()).andReturn(hostRequests).anyTimes();
    expect(logicalRequest.getRequestContext()).andReturn("Description").anyTimes();
    expect(logicalRequest.getRequestId()).andReturn(1L).anyTimes();
    replay(logicalRequest);

    persistedState.persistLogicalRequest(logicalRequest, requestId);
  }

  private void addHostToCluster(String hostName, String clusterName) throws AmbariException {
    clusters.addHost(hostName);

    Host host = clusters.getHost(hostName);
    setOsFamily(clusters.getHost(hostName), "centos", "5.9");

    Set<String> hostnames = new HashSet<>();
    hostnames.add(hostName);
    clusters.mapAndPublishHostsToCluster(hostnames, clusterName);
  }

  private Cluster createCluster(String clusterName) throws AmbariException {
    StackId stackId = new StackId("HDP-0.1");

    helper.createStack(stackId);

    clusters.addCluster(clusterName, stackId);

    return clusters.getCluster(clusterName);
  }

  private static class MockModule implements Module {
    @Override
    public void configure(Binder binder) {
      binder.bind(TopologyManager.class).toInstance(createNiceMock(TopologyManager.class));
    }
  }
}
