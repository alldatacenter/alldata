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

package org.apache.ambari.server.events.listeners.upgrade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.controller.internal.DeleteHostComponentStatusMetaData;
import org.apache.ambari.server.events.ServiceComponentInstalledEvent;
import org.apache.ambari.server.events.ServiceComponentUninstalledEvent;
import org.apache.ambari.server.events.ServiceInstalledEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.UnitOfWork;

public class HostVersionOutOfSyncListenerTest {
  private final String stackId = "HDP-2.2.0";
  private final String yetAnotherStackId = "HDP-2.1.1";

  private final String CURRENT_VERSION = "2.2.0-2086";

  private Injector injector;

  @Inject
  private Clusters clusters;
  private Cluster c1;

  @Inject
  private OrmTestHelper helper;

  @Inject
  private HostVersionDAO hostVersionDAO;

  @Inject
  private ServiceComponentHostFactory serviceComponentHostFactory;

  @Inject
  private AmbariEventPublisher m_eventPublisher;


  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    EventBusSynchronizer.synchronizeAmbariEventPublisher(injector);
    injector.injectMembers(this);

    StackId stackId = new StackId(this.stackId);

    helper.createStack(stackId);

    clusters.addCluster("c1", stackId);
    c1 = clusters.getCluster("c1");
    addHost("h1");

    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());
    clusters.mapHostToCluster("h1", "c1");
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }


  /***
   * Shared between several test cases.
   * @param INSTALLED_VERSION Version to treat as INSTALLED
   * @param stackId Stack Id to use
   * @throws AmbariException
   */
  private RepositoryVersionEntity createClusterAndHosts(String INSTALLED_VERSION, StackId stackId)
      throws AmbariException {
    Host h1 = clusters.getHost("h1");
    h1.setState(HostState.HEALTHY);

    addHost("h2");
    clusters.mapHostToCluster("h2", "c1");
    addHost("h3");
    clusters.mapHostToCluster("h3", "c1");

    // create the new repo version
    RepositoryVersionEntity repositoryVersionEntity = helper.getOrCreateRepositoryVersion(stackId,
        INSTALLED_VERSION);

    c1.setCurrentStackVersion(stackId);
    assertRepoVersionState(INSTALLED_VERSION, RepositoryVersionState.INSTALLING);

    assertRepoVersionState(CURRENT_VERSION, RepositoryVersionState.CURRENT);

    // Add ZK service with only ZOOKEEPER_SERVER
    List<String> hostList = new ArrayList<>();
    hostList.add("h1");
    hostList.add("h2");
    hostList.add("h3");
    Map<String, List<Integer>> zkTopology = new HashMap<>();
    List<Integer> zkServerHosts = Arrays.asList(0, 1, 2);
    zkTopology.put("ZOOKEEPER_SERVER", new ArrayList<>(zkServerHosts));
    addService(c1, hostList, zkTopology, "ZOOKEEPER", repositoryVersionEntity);

    // install new version
    helper.createHostVersion("h1", repositoryVersionEntity, RepositoryVersionState.INSTALLED);
    helper.createHostVersion("h2", repositoryVersionEntity, RepositoryVersionState.INSTALLED);
    helper.createHostVersion("h3", repositoryVersionEntity, RepositoryVersionState.INSTALLED);

    assertRepoVersionState(INSTALLED_VERSION, RepositoryVersionState.INSTALLED);
    assertRepoVersionState(CURRENT_VERSION, RepositoryVersionState.CURRENT);

    // Add new host and verify that it has all host versions present
    List<HostVersionEntity> h2Versions = hostVersionDAO.findAll();

    // Check before adding service
    for (HostVersionEntity hostVersionEntity : h2Versions) {
      if (hostVersionEntity.getRepositoryVersion().getVersion().equals(INSTALLED_VERSION)) {
        assertEquals(hostVersionEntity.getState(), RepositoryVersionState.INSTALLED);
      }
    }

    return repositoryVersionEntity;
  }

  /***
   * Adds yet another repo version to existing cluster
   * Shared between several test cases.
   * @param INSTALLED_VERSION Version to add as INSTALLED
   * @param stackId Stack Id to use
   * @throws AmbariException
   */
  private RepositoryVersionEntity addRepoVersion(String INSTALLED_VERSION, StackId stackId) throws AmbariException {
    // Register and install new version
    RepositoryVersionEntity repositoryVersionEntity = helper.getOrCreateRepositoryVersion(stackId,
            INSTALLED_VERSION);
    helper.createHostVersion("h1", repositoryVersionEntity, RepositoryVersionState.INSTALLED);
    assertRepoVersionState(INSTALLED_VERSION, RepositoryVersionState.INSTALLED);

    // Add new host and verify that it has all host versions present
    List<HostVersionEntity> h2Versions = hostVersionDAO.findAll();

    // Check before adding service
    for (HostVersionEntity hostVersionEntity : h2Versions) {
      if (hostVersionEntity.getRepositoryVersion().getVersion().equals(INSTALLED_VERSION)) {
        assertEquals(hostVersionEntity.getState(), RepositoryVersionState.INSTALLED);
      }
    }

    return repositoryVersionEntity;
  }

  /**
   * When a service is added to a cluster, all non-CURRENT host versions on
   * all affected hosts (where host new components are installed)
   * should transition to OUT_OF_SYNC state
   */
  @Test
  public void testOnServiceEvent() throws AmbariException {
    String INSTALLED_VERSION = "2.2.0-1000";
    String INSTALLED_VERSION_2 = "2.1.1-2000";
    StackId stackId = new StackId(this.stackId);
    StackId yaStackId = new StackId(yetAnotherStackId);

    // get new hosts installed with the first repo
    RepositoryVersionEntity repositoryVersion = createClusterAndHosts(INSTALLED_VERSION, stackId);

    // register the new repo
    addRepoVersion(INSTALLED_VERSION_2, yaStackId);

    assertRepoVersionState(INSTALLED_VERSION, RepositoryVersionState.INSTALLED);
    assertRepoVersionState(INSTALLED_VERSION_2, RepositoryVersionState.INSTALLED);
    assertRepoVersionState(CURRENT_VERSION, RepositoryVersionState.CURRENT);

    //Add HDFS service
    List<String> hostList = new ArrayList<>();
    hostList.add("h1");
    hostList.add("h2");
    hostList.add("h3");
    Map<String, List<Integer>> hdfsTopology = new HashMap<>();
    hdfsTopology.put("NAMENODE", Collections.singletonList(0));
    hdfsTopology.put("SECONDARY_NAMENODE", Collections.singletonList(1));
    List<Integer> datanodeHosts = Arrays.asList(0, 1);
    hdfsTopology.put("DATANODE", new ArrayList<>(datanodeHosts));
    addService(c1, hostList, hdfsTopology, "HDFS", repositoryVersion);

    // Check result
    Set<String> changedHosts = new HashSet<>();
    changedHosts.add("h1");
    changedHosts.add("h2");

    List<HostVersionEntity> hostVersions = hostVersionDAO.findAll();

    for (HostVersionEntity hostVersionEntity : hostVersions) {
      if (hostVersionEntity.getRepositoryVersion().getVersion().equals(INSTALLED_VERSION) ||
              hostVersionEntity.getRepositoryVersion().getVersion().equals(INSTALLED_VERSION_2)) {
        if (changedHosts.contains(hostVersionEntity.getHostName())) {
          assertEquals(hostVersionEntity.getState(), RepositoryVersionState.OUT_OF_SYNC);
        } else {
          assertEquals(hostVersionEntity.getState(), RepositoryVersionState.INSTALLED);
        }
      }
    }


    assertRepoVersionState(INSTALLED_VERSION, RepositoryVersionState.OUT_OF_SYNC);
    assertRepoVersionState(INSTALLED_VERSION_2, RepositoryVersionState.OUT_OF_SYNC);
    assertRepoVersionState(CURRENT_VERSION, RepositoryVersionState.CURRENT);
  }


  /**
   * When a service with components that don't advertise their versions
   * is added to a cluster, all non-CURRENT host versions on
   * all affected hosts (where host new components are installed)
   * should NOT transition to OUT_OF_SYNC state
   */
  @Test
  public void testOnServiceEvent_component_does_not_advertise_version() throws AmbariException {
    String INSTALLED_VERSION = "2.2.0-1000";
    StackId stackId = new StackId(this.stackId);

    RepositoryVersionEntity repositoryVersion = createClusterAndHosts(INSTALLED_VERSION, stackId);

    //Add Ganglia service
    List<String> hostList = new ArrayList<>();
    hostList.add("h1");
    hostList.add("h2");
    hostList.add("h3");
    Map<String, List<Integer>> hdfsTopology = new HashMap<>();
    hdfsTopology.put("GANGLIA_SERVER", Collections.singletonList(0));
    List<Integer> monitorHosts = Arrays.asList(0, 1);
    hdfsTopology.put("GANGLIA_MONITOR", new ArrayList<>(monitorHosts));
    addService(c1, hostList, hdfsTopology, "GANGLIA", repositoryVersion);

    // Check result
    Set<String> changedHosts = new HashSet<>();
    changedHosts.add("h1");
    changedHosts.add("h2");

    List<HostVersionEntity> hostVersions = hostVersionDAO.findAll();

    // Host version should not transition to OUT_OF_SYNC state
    assertRepoVersionState(INSTALLED_VERSION, RepositoryVersionState.INSTALLED);
    for (HostVersionEntity hostVersionEntity : hostVersions) {
      if (hostVersionEntity.getRepositoryVersion().getVersion().equals(INSTALLED_VERSION)) {
        assertEquals(hostVersionEntity.getState(), RepositoryVersionState.INSTALLED);
      }
    }
  }

  /**
   * When a new service is added to a cluster with components, all INSTALLED host versions on
   * all affected hosts (where host new components are installed)
   * should transition to OUT_OF_SYNC state.
   */
  @Test
  public void testOnServiceComponentEvent() throws AmbariException {
    String INSTALLED_VERSION = "2.2.0-1000";
    String INSTALLED_VERSION_2 = "2.1.1-2000";
    StackId stackId = new StackId(this.stackId);
    StackId yaStackId = new StackId(yetAnotherStackId);

    createClusterAndHosts(INSTALLED_VERSION, stackId);
    addRepoVersion(INSTALLED_VERSION_2, yaStackId);

    assertRepoVersionState(INSTALLED_VERSION, RepositoryVersionState.INSTALLED);
    assertRepoVersionState(INSTALLED_VERSION_2, RepositoryVersionState.INSTALLED);

    //Add ZOOKEEPER_CLIENT component
    List<String> hostList = new ArrayList<>();
    hostList.add("h1");
    hostList.add("h2");
    hostList.add("h3");
    addServiceComponent(c1, hostList, "ZOOKEEPER", "ZOOKEEPER_CLIENT");

    // Check result
    Set<String> changedHosts = new HashSet<>();
    changedHosts.add("h1");
    changedHosts.add("h2");
    changedHosts.add("h3");

    assertRepoVersionState(INSTALLED_VERSION, RepositoryVersionState.OUT_OF_SYNC);

    List<HostVersionEntity> hostVersions = hostVersionDAO.findAll();

    for (HostVersionEntity hostVersionEntity : hostVersions) {
      RepositoryVersionEntity repoVersion = hostVersionEntity.getRepositoryVersion();

      if (repoVersion.getVersion().equals(INSTALLED_VERSION_2)) {
        assertEquals(RepositoryVersionState.INSTALLED, hostVersionEntity.getState());
      } else if (repoVersion.getVersion().equals(INSTALLED_VERSION)) {
        assertTrue(changedHosts.contains(hostVersionEntity.getHostName()));
        assertEquals(RepositoryVersionState.OUT_OF_SYNC, hostVersionEntity.getState());
      }
    }
  }


  /**
   * When a host is added to a cluster that has non-CURRENT cluster_version records,
   * then Ambari needs to insert host_verion record for each one of
   * those stack versions with a state of OUT_OF_SYNC
   */
  @Test
  public void testOnHostEvent() throws AmbariException {
    // Configuring single-node cluster with 2 repo versions
    Host h1 = clusters.getHost("h1");
    h1.setState(HostState.HEALTHY);

    StackId stackId = new StackId(this.stackId);
    RepositoryVersionEntity repositoryVersionEntity = helper.getOrCreateRepositoryVersion(stackId,"2.2.0-1000");
    RepositoryVersionEntity repositoryVersionEntity2 = helper.getOrCreateRepositoryVersion(stackId,"2.2.0-2000");
    c1.setCurrentStackVersion(stackId);

    assertRepoVersionState("2.2.0-1000", RepositoryVersionState.INSTALLING);
    assertRepoVersionState("2.2.0-2086", RepositoryVersionState.CURRENT);

    helper.createHostVersion("h1", repositoryVersionEntity, RepositoryVersionState.INSTALLED);
    helper.createHostVersion("h1", repositoryVersionEntity2, RepositoryVersionState.INSTALLED);

    assertRepoVersionState("2.2.0-1000", RepositoryVersionState.INSTALLED);
    assertRepoVersionState("2.2.0-2000", RepositoryVersionState.INSTALLED);
    assertRepoVersionState("2.2.0-2086", RepositoryVersionState.CURRENT);

    // Add new host and verify that it has all host versions present
    addHost("h2");
    clusters.mapHostToCluster("h2", "c1");

    List<HostVersionEntity> h2Versions = hostVersionDAO.findByHost("h2");

    for (HostVersionEntity hostVersionEntity : h2Versions) {
      if (hostVersionEntity.getRepositoryVersion().toString().equals("2.2.0-2086")) {
        assertEquals(hostVersionEntity.getState(), RepositoryVersionState.CURRENT);
      } else {
        assertEquals(hostVersionEntity.getState(), RepositoryVersionState.OUT_OF_SYNC);
      }
    }
  }

  /**
   * Tests that when a host is removed, the {@link org.apache.ambari.server.events.HostsRemovedEvent} fires and
   * eventually calls to recalculate the cluster state.
   */
  @Test
  public void testOnHostRemovedEvent() throws AmbariException {
    // add the 2nd host
    addHost("h2");
    clusters.mapHostToCluster("h2", "c1");
    Host host = clusters.getHost("h2");
    Long hostId = host.getHostId();
    host.setState(HostState.HEALTHY);

    StackId stackId = new StackId(this.stackId);
    RepositoryVersionEntity repositoryVersionEntity = helper.getOrCreateRepositoryVersion(stackId,
        "2.2.9-9999");

    c1.setCurrentStackVersion(stackId);

    assertRepoVersionState("2.2.0", RepositoryVersionState.CURRENT);
    assertRepoVersionState("2.2.9-9999", RepositoryVersionState.INSTALLING);

    HostVersionEntity hv1 = helper.createHostVersion("h1", repositoryVersionEntity,
        RepositoryVersionState.INSTALLED);
    HostVersionEntity hv2 = helper.createHostVersion("h2", repositoryVersionEntity,
        RepositoryVersionState.INSTALLED);

    // do an initial calculate to make sure the new repo is installing
    assertRepoVersionState("2.2.0", RepositoryVersionState.CURRENT);
    assertRepoVersionState("2.2.9-9999", RepositoryVersionState.INSTALLED);

    // make it seems like we upgraded, but 1 host still hasn't finished
    hv1.setState(RepositoryVersionState.INSTALLED);
    hv2.setState(RepositoryVersionState.INSTALLING);
    hostVersionDAO.merge(hv1);
    hostVersionDAO.merge(hv2);

    // recalculate and ensure that the cluster is UPGRADING
    assertRepoVersionState("2.2.0", RepositoryVersionState.CURRENT);
    assertRepoVersionState("2.2.9-9999", RepositoryVersionState.INSTALLING);

    // delete the host that was UPGRADING, and DON'T call recalculate; let the
    // event handle it
    injector.getInstance(UnitOfWork.class).begin();
    clusters.deleteHost("h2");
    clusters.publishHostsDeletion(Collections.singleton(hostId), Collections.singleton("h2"));
    injector.getInstance(UnitOfWork.class).end();
    assertRepoVersionState("2.2.0", RepositoryVersionState.CURRENT);
    assertRepoVersionState("2.2.9-9999", RepositoryVersionState.INSTALLED);
  }

  @Test
  public void testComponentHostVersionNotRequired() throws Exception {
    String clusterName = UUID.randomUUID().toString();
    String host1 = "host1";
    String host2 = "host2";
    String host3 = "host3";

    List<String> allHosts = Lists.newArrayList(host1, host2, host3);

    // create cluster and hosts
    StackId stackId = new StackId(this.stackId);
    clusters.addCluster(clusterName, stackId);
    c1 = clusters.getCluster(clusterName);
    addHost(host1);
    addHost(host2);
    addHost(host3);

    // create repo version
    RepositoryVersionEntity repo = helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());

    clusters.mapHostToCluster(host1, clusterName);
    clusters.mapHostToCluster(host2, clusterName);
    clusters.mapHostToCluster(host3, clusterName);

    helper.createHostVersion(host1, repo, RepositoryVersionState.INSTALLED);
    helper.createHostVersion(host2, repo, RepositoryVersionState.INSTALLED);
    helper.createHostVersion(host3, repo, RepositoryVersionState.INSTALLED);

    // add host1 with versionable component + non-versionable
    // add host2 with versionable component
    // add host3 with non-versionable component

    Map<String, List<Integer>> topology = new ImmutableMap.Builder<String, List<Integer>>()
        .put("NAMENODE", Lists.newArrayList(0))
        .put("DATANODE", Lists.newArrayList(1))
        .build();
    addService(c1, allHosts, topology, "HDFS", repo);

    topology = new ImmutableMap.Builder<String, List<Integer>>()
        .put("GANGLIA_SERVER", Lists.newArrayList(0))
        .put("GANGLIA_MONITOR", Lists.newArrayList(2))
        .build();
    addService(c1, allHosts, topology, "GANGLIA", repo);

    List<HostVersionEntity> hostVersions = hostVersionDAO.findAll();
    assertEquals(3, hostVersions.size());

    // assert host1 is OUT_OF_SYNC
    // assert host2 is OUT_OF_SYNC
    // assert host3 is NOT_REQUIRED
    for (HostVersionEntity hve : hostVersions) {
      if (hve.getHostName().equals(host3)) {
        assertEquals(RepositoryVersionState.NOT_REQUIRED, hve.getState());
      } else {
        assertEquals(RepositoryVersionState.OUT_OF_SYNC, hve.getState());
      }
    }

    // add versionable component to host3
    addServiceComponent(c1, Collections.singletonList(host3), "HDFS", "DATANODE");

    // assert host3 is OUT_OF_SYNC
    hostVersions = hostVersionDAO.findAll();
    for (HostVersionEntity hve : hostVersions) {
      assertEquals(RepositoryVersionState.OUT_OF_SYNC, hve.getState());
    }

    // remove versionable component from host3
    List<ServiceComponentHost> hostComponents = c1.getServiceComponentHosts(host3);
    for (ServiceComponentHost sch : hostComponents) {
      if (sch.getServiceName().equals("HDFS")) {
        sch.delete(new DeleteHostComponentStatusMetaData());

        StackId clusterStackId = c1.getDesiredStackVersion();

        ServiceComponentUninstalledEvent event = new ServiceComponentUninstalledEvent(
            c1.getClusterId(), clusterStackId.getStackName(), clusterStackId.getStackVersion(),
            "HDFS", "DATANODE", sch.getHostName(), false, false, -1l);

        m_eventPublisher.publish(event);
      }
    }

    // assert host3 is back to NOT_REQUIRED
    hostVersions = hostVersionDAO.findAll();
    for (HostVersionEntity hve : hostVersions) {
      if (hve.getHostName().equals(host3)) {
        assertEquals(RepositoryVersionState.NOT_REQUIRED, hve.getState());
      } else {
        assertEquals(RepositoryVersionState.OUT_OF_SYNC, hve.getState());
      }
    }
  }

  private void addHost(String hostname) throws AmbariException {
    clusters.addHost(hostname);

    Host host1 = clusters.getHost(hostname);
    host1.setIPv4("ipv4");
    host1.setIPv6("ipv6");

    Map<String, String> hostAttributes = new HashMap<>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6.4");
    host1.setHostAttributes(hostAttributes);
  }

  private void addService(Cluster cl, List<String> hostList, Map<String, List<Integer>> topology,
      String serviceName, RepositoryVersionEntity repositoryVersionEntity) throws AmbariException {
    StackId stackIdObj = new StackId(stackId);
    cl.setDesiredStackVersion(stackIdObj);
    cl.addService(serviceName, repositoryVersionEntity);

    for (Map.Entry<String, List<Integer>> component : topology.entrySet()) {

      String componentName = component.getKey();
      cl.getService(serviceName).addServiceComponent(componentName);

      for (Integer hostIndex : component.getValue()) {
        cl.getService(serviceName)
                .getServiceComponent(componentName)
                .addServiceComponentHost(
                        serviceComponentHostFactory.createNew(cl.getService(serviceName)
                                .getServiceComponent(componentName), hostList.get(hostIndex)));
      }

      ServiceInstalledEvent event = new ServiceInstalledEvent(cl.getClusterId(),
          stackIdObj.getStackName(), stackIdObj.getStackVersion(), serviceName);
      m_eventPublisher.publish(event);
    }
  }

  private void addServiceComponent(Cluster cl, List<String> hostList,
                                   String serviceName, String componentName) throws AmbariException {
    StackId stackIdObj = new StackId(stackId);
    Service service = cl.getService(serviceName);

    if (!service.getServiceComponents().containsKey(componentName)) {
      service.addServiceComponent(componentName);
    }

    ServiceComponent component = service.getServiceComponent(componentName);

    for(String hostName : hostList) {
      component.addServiceComponentHost(serviceComponentHostFactory.createNew(cl.getService(serviceName)
          .getServiceComponent(componentName), hostName));
      ServiceComponentInstalledEvent event = new ServiceComponentInstalledEvent(cl.getClusterId(),
          stackIdObj.getStackName(), stackIdObj.getStackVersion(),
          serviceName, componentName, hostName, false /* recovery not enabled */, false);
      m_eventPublisher.publish(event);
    }
  }

  private void assertRepoVersionState(String version, RepositoryVersionState state) {

    for (Host host : c1.getHosts()) {
      for (HostVersionEntity hostVersionEntity : host.getAllHostVersions()) {
        if (hostVersionEntity.getRepositoryVersion().getVersion().equals("version")) {
          assertEquals(state, hostVersionEntity.getState());
        }
      }
    }
  }
}
