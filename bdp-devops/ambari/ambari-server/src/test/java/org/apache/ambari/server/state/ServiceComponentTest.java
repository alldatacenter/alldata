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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.controller.ServiceComponentResponse;
import org.apache.ambari.server.controller.internal.DeleteHostComponentStatusMetaData;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

public class ServiceComponentTest {

  private Clusters clusters;
  private Cluster cluster;
  private Service service;
  private String clusterName;
  private String serviceName;
  private Injector injector;
  private ServiceFactory serviceFactory;
  private ServiceComponentFactory serviceComponentFactory;
  private ServiceComponentHostFactory serviceComponentHostFactory;
  private OrmTestHelper helper;
  private HostDAO hostDAO;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    serviceFactory = injector.getInstance(ServiceFactory.class);
    serviceComponentFactory = injector.getInstance(
        ServiceComponentFactory.class);
    serviceComponentHostFactory = injector.getInstance(
        ServiceComponentHostFactory.class);
    helper = injector.getInstance(OrmTestHelper.class);
    hostDAO = injector.getInstance(HostDAO.class);

    clusterName = "foo";
    serviceName = "HDFS";

    StackId stackId = new StackId("HDP-0.1");

    helper.createStack(stackId);

    clusters.addCluster(clusterName, stackId);
    cluster = clusters.getCluster(clusterName);

    cluster.setDesiredStackVersion(stackId);
    Assert.assertNotNull(cluster);

    RepositoryVersionEntity repositoryVersion = helper.getOrCreateRepositoryVersion(stackId,
        stackId.getStackVersion());

    Service s = serviceFactory.createNew(cluster, serviceName, repositoryVersion);
    cluster.addService(s);
    service = cluster.getService(serviceName);
    Assert.assertNotNull(service);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  public void testCreateServiceComponent() throws AmbariException {
    String componentName = "DATANODE2";
    ServiceComponent component = serviceComponentFactory.createNew(service,
        componentName);
    service.addServiceComponent(component);

    ServiceComponent sc = service.getServiceComponent(componentName);
    Assert.assertNotNull(sc);

    Assert.assertEquals(componentName, sc.getName());
    Assert.assertEquals(serviceName, sc.getServiceName());
    Assert.assertEquals(cluster.getClusterId(),
        sc.getClusterId());
    Assert.assertEquals(cluster.getClusterName(),
        sc.getClusterName());
    Assert.assertEquals(State.INIT, sc.getDesiredState());
    Assert.assertFalse(
        sc.getDesiredStackId().getStackId().isEmpty());
  }


  @Test
  public void testGetAndSetServiceComponentInfo() throws AmbariException {
    String componentName = "NAMENODE";
    ServiceComponent component = serviceComponentFactory.createNew(service,
        componentName);
    service.addServiceComponent(component);

    ServiceComponent sc = service.getServiceComponent(componentName);
    Assert.assertNotNull(sc);


    sc.setDesiredState(State.INSTALLED);
    Assert.assertEquals(State.INSTALLED, sc.getDesiredState());

    StackId newStackId = new StackId("HDP-1.2.0");
    RepositoryVersionEntity repositoryVersion = helper.getOrCreateRepositoryVersion(newStackId,
        newStackId.getStackVersion());

    sc.setDesiredRepositoryVersion(repositoryVersion);
    Assert.assertEquals(newStackId.toString(), sc.getDesiredStackId().getStackId());

    ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO =
        injector.getInstance(ServiceComponentDesiredStateDAO.class);

    ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findByName(
        cluster.getClusterId(), serviceName, componentName);

    ServiceComponent sc1 = serviceComponentFactory.createExisting(service,
        serviceComponentDesiredStateEntity);
    Assert.assertNotNull(sc1);
    Assert.assertEquals(State.INSTALLED, sc1.getDesiredState());
    Assert.assertEquals("HDP-1.2.0",
        sc1.getDesiredStackId().getStackId());

  }

  @Test
  public void testGetAndSetConfigs() {
    // FIXME add unit tests for configs once impl done
    /*
      public Map<String, Config> getDesiredConfigs();
      public void updateDesiredConfigs(Map<String, Config> configs);
     */
  }

  private void addHostToCluster(String hostname,
      String clusterName) throws AmbariException {
    clusters.addHost(hostname);
    Host h = clusters.getHost(hostname);
    h.setIPv4(hostname + "ipv4");
    h.setIPv6(hostname + "ipv6");

    Map<String, String> hostAttributes = new HashMap<>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6.3");
    h.setHostAttributes(hostAttributes);

    clusters.mapHostToCluster(hostname, clusterName);
  }

  @Test
  public void testAddAndGetServiceComponentHosts() throws AmbariException {
    String componentName = "NAMENODE";
    ServiceComponent component = serviceComponentFactory.createNew(service, componentName);
    service.addServiceComponent(component);

    ServiceComponent sc = service.getServiceComponent(componentName);
    Assert.assertNotNull(sc);

    Assert.assertTrue(sc.getServiceComponentHosts().isEmpty());

    try {
      serviceComponentHostFactory.createNew(sc, "h1");
      fail("Expected error for invalid host");
    } catch (Exception e) {
      // Expected
    }

    addHostToCluster("h1", service.getCluster().getClusterName());
    addHostToCluster("h2", service.getCluster().getClusterName());
    addHostToCluster("h3", service.getCluster().getClusterName());

    HostEntity hostEntity1 = hostDAO.findByName("h1");
    assertNotNull(hostEntity1);

    ServiceComponentHost sch1 = sc.addServiceComponentHost("h1");
    ServiceComponentHost sch2 = sc.addServiceComponentHost("h2");
    assertNotNull(sch1);
    assertNotNull(sch2);

    try {
      sc.addServiceComponentHost("h2");
      fail("Expected error for dups");
    } catch (Exception e) {
      // Expected
    }

    Assert.assertEquals(2, sc.getServiceComponentHosts().size());

    ServiceComponentHost schCheck = sc.getServiceComponentHost("h2");
    Assert.assertNotNull(schCheck);
    Assert.assertEquals("h2", schCheck.getHostName());

    sc.addServiceComponentHost("h3");
    Assert.assertNotNull(sc.getServiceComponentHost("h3"));

    sch1.setState(State.STARTING);
    sch1.setDesiredState(State.STARTED);

    HostComponentDesiredStateDAO desiredStateDAO = injector.getInstance(
        HostComponentDesiredStateDAO.class);
    HostComponentStateDAO liveStateDAO = injector.getInstance(
        HostComponentStateDAO.class);


    HostComponentDesiredStateEntity desiredStateEntity =
        desiredStateDAO.findByIndex(
          cluster.getClusterId(),
          serviceName,
          componentName,
          hostEntity1.getHostId()
        );

    HostComponentStateEntity stateEntity = liveStateDAO.findByIndex(cluster.getClusterId(),
        serviceName, componentName, hostEntity1.getHostId());

    ServiceComponentHost sch = serviceComponentHostFactory.createExisting(sc,
        stateEntity, desiredStateEntity);
    Assert.assertNotNull(sch);
    Assert.assertEquals(State.STARTING, sch.getState());
    Assert.assertEquals(State.STARTED, sch.getDesiredState());
    Assert.assertEquals(service.getDesiredRepositoryVersion().getVersion(),
        sch.getServiceComponent().getDesiredVersion());
  }

  @Test
  public void testConvertToResponse() throws AmbariException {
    String componentName = "NAMENODE";
    ServiceComponent component = serviceComponentFactory.createNew(service, componentName);
    service.addServiceComponent(component);

    addHostToCluster("h1", service.getCluster().getClusterName());
    addHostToCluster("h2", service.getCluster().getClusterName());
    addHostToCluster("h3", service.getCluster().getClusterName());
    ServiceComponentHost sch =
      serviceComponentHostFactory.createNew(component, "h1");
    ServiceComponentHost sch2 =
      serviceComponentHostFactory.createNew(component, "h2");
    ServiceComponentHost sch3 =
      serviceComponentHostFactory.createNew(component, "h3");
    sch.setState(State.INSTALLED);
    sch2.setState(State.INSTALLED);
    sch3.setState(State.INSTALLED);

    Map<String, ServiceComponentHost> compHosts =
      new HashMap<>();
    compHosts.put("h1", sch);
    compHosts.put("h2", sch2);
    compHosts.put("h3", sch3);
    component.addServiceComponentHosts(compHosts);
    Assert.assertEquals(3, component.getServiceComponentHosts().size());

    component.getServiceComponentHost("h2").setMaintenanceState(MaintenanceState.ON);
    sch3.setMaintenanceState(MaintenanceState.ON);

    ServiceComponent sc = service.getServiceComponent(componentName);
    Assert.assertNotNull(sc);
    sc.setDesiredState(State.INSTALLED);

    ServiceComponentResponse r = sc.convertToResponse();
    Assert.assertEquals(sc.getClusterName(), r.getClusterName());
    Assert.assertEquals(sc.getClusterId(), r.getClusterId().longValue());
    Assert.assertEquals(sc.getName(), r.getComponentName());
    Assert.assertEquals(sc.getServiceName(), r.getServiceName());
    Assert.assertEquals(sc.getDesiredStackId().getStackId(), r.getDesiredStackId());
    Assert.assertEquals(sc.getDesiredState().toString(), r.getDesiredState());

    int totalCount = r.getServiceComponentStateCount().get("totalCount");
    int startedCount = r.getServiceComponentStateCount().get("startedCount");
    int installedCount = r.getServiceComponentStateCount().get("installedCount");
    int installedAndMaintenanceOffCount = r.getServiceComponentStateCount().get("installedAndMaintenanceOffCount");
    Assert.assertEquals(3, totalCount);
    Assert.assertEquals(0, startedCount);
    Assert.assertEquals(3, installedCount);
    Assert.assertEquals(1, installedAndMaintenanceOffCount);

    // TODO check configs
    // r.getConfigVersions()

    // TODO test debug dump
    StringBuilder sb = new StringBuilder();
    sc.debugDump(sb);
    Assert.assertFalse(sb.toString().isEmpty());
  }

  @Test
  public void testCanBeRemoved() throws Exception {
    String componentName = "NAMENODE";
    ServiceComponent component = serviceComponentFactory.createNew(service,
                                                                   componentName);
    addHostToCluster("h1", service.getCluster().getClusterName());
    ServiceComponentHost sch = serviceComponentHostFactory.createNew(component, "h1");
    component.addServiceComponentHost(sch);

    for (State state : State.values()) {
      component.setDesiredState(state);

      for (State hcState : State.values()) {
        sch.setDesiredState(hcState);
        sch.setState(hcState);

        if (hcState.isRemovableState()) {
          org.junit.Assert.assertTrue(component.canBeRemoved());
        } else {
          org.junit.Assert.assertFalse(component.canBeRemoved());
        }
      }
    }
  }


  @Test
  public void testServiceComponentRemove() throws AmbariException {
    ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO = injector.getInstance(
        ServiceComponentDesiredStateDAO.class);

    String componentName = "NAMENODE";
    ServiceComponent component = serviceComponentFactory.createNew(service, componentName);
    service.addServiceComponent(component);

    ServiceComponent sc = service.getServiceComponent(componentName);
    Assert.assertNotNull(sc);

    sc.setDesiredState(State.STARTED);
    Assert.assertEquals(State.STARTED, sc.getDesiredState());

    ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findByName(
        cluster.getClusterId(), serviceName, componentName);

    Assert.assertNotNull(serviceComponentDesiredStateEntity);

    Assert.assertTrue(sc.getServiceComponentHosts().isEmpty());

    addHostToCluster("h1", service.getCluster().getClusterName());
    addHostToCluster("h2", service.getCluster().getClusterName());

    HostEntity hostEntity1 = hostDAO.findByName("h1");
    assertNotNull(hostEntity1);

    ServiceComponentHost sch1 =
        serviceComponentHostFactory.createNew(sc, "h1");
    ServiceComponentHost sch2 =
        serviceComponentHostFactory.createNew(sc, "h2");

    Map<String, ServiceComponentHost> compHosts =
      new HashMap<>();
    compHosts.put("h1", sch1);
    compHosts.put("h2", sch2);
    sc.addServiceComponentHosts(compHosts);

    sch1.setState(State.STARTED);
    sch2.setState(State.STARTED);

    // delete the SC
    DeleteHostComponentStatusMetaData deleteMetaData = new DeleteHostComponentStatusMetaData();
    sc.delete(deleteMetaData);
    Assert.assertNotNull("Delete must fail as some SCH are in STARTED state", deleteMetaData.getAmbariException());

    sch1.setState(State.INSTALLED);
    sch2.setState(State.INSTALL_FAILED);
    sc.delete(new DeleteHostComponentStatusMetaData());

    // verify history is gone, too
    serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findByName(
        cluster.getClusterId(), serviceName, componentName);

    Assert.assertNull(serviceComponentDesiredStateEntity);
 }

  @Test
  public void testVersionCreation() throws Exception {
    ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO = injector.getInstance(
        ServiceComponentDesiredStateDAO.class);

    String componentName = "NAMENODE";
    ServiceComponent component = serviceComponentFactory.createNew(service, componentName);
    service.addServiceComponent(component);

    ServiceComponent sc = service.getServiceComponent(componentName);
    Assert.assertNotNull(sc);

    sc.setDesiredState(State.INSTALLED);
    Assert.assertEquals(State.INSTALLED, sc.getDesiredState());

    ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findByName(
        cluster.getClusterId(), serviceName, componentName);

    StackDAO stackDAO = injector.getInstance(StackDAO.class);
    StackEntity stackEntity = stackDAO.find("HDP", "2.2.0");

    RepositoryVersionEntity rve = new RepositoryVersionEntity(stackEntity, "HDP-2.2.0",
        "2.2.0.1-1111", new ArrayList<>());

    RepositoryVersionDAO repositoryDAO = injector.getInstance(RepositoryVersionDAO.class);
    repositoryDAO.create(rve);

    sc.setDesiredRepositoryVersion(rve);

    Assert.assertEquals(rve, sc.getDesiredRepositoryVersion());

    Assert.assertEquals(new StackId("HDP", "2.2.0"), sc.getDesiredStackId());

    Assert.assertEquals("HDP-2.2.0", sc.getDesiredStackId().getStackId());

    Assert.assertNotNull(serviceComponentDesiredStateEntity);

    ServiceComponentVersionEntity version = new ServiceComponentVersionEntity();
    version.setState(RepositoryVersionState.CURRENT);
    version.setRepositoryVersion(rve);
    version.setUserName("user");
    serviceComponentDesiredStateEntity.addVersion(version);

    serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.merge(
        serviceComponentDesiredStateEntity);

    serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findByName(
        cluster.getClusterId(), serviceName, componentName);

    assertEquals(1, serviceComponentDesiredStateEntity.getVersions().size());
    ServiceComponentVersionEntity persistedVersion = serviceComponentDesiredStateEntity.getVersions().iterator().next();

    assertEquals(RepositoryVersionState.CURRENT, persistedVersion.getState());
  }

  @Test
  public void testVersionRemoval() throws Exception {
    ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO = injector.getInstance(
        ServiceComponentDesiredStateDAO.class);

    String componentName = "NAMENODE";
    ServiceComponent component = serviceComponentFactory.createNew(service, componentName);
    service.addServiceComponent(component);

    ServiceComponent sc = service.getServiceComponent(componentName);
    Assert.assertNotNull(sc);

    sc.setDesiredState(State.INSTALLED);
    Assert.assertEquals(State.INSTALLED, sc.getDesiredState());

    ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findByName(
        cluster.getClusterId(), serviceName, componentName);

    StackDAO stackDAO = injector.getInstance(StackDAO.class);
    StackEntity stackEntity = stackDAO.find("HDP", "2.2.0");

    RepositoryVersionEntity rve = new RepositoryVersionEntity(stackEntity, "HDP-2.2.0",
        "2.2.0.1-1111", new ArrayList<>());

    RepositoryVersionDAO repositoryDAO = injector.getInstance(RepositoryVersionDAO.class);
    repositoryDAO.create(rve);

    sc.setDesiredRepositoryVersion(rve);

    StackId stackId = sc.getDesiredStackId();
    Assert.assertEquals(new StackId("HDP", "2.2.0"), stackId);

    Assert.assertEquals("HDP-2.2.0", sc.getDesiredStackId().getStackId());

    Assert.assertNotNull(serviceComponentDesiredStateEntity);

    ServiceComponentVersionEntity version = new ServiceComponentVersionEntity();
    version.setState(RepositoryVersionState.CURRENT);
    version.setRepositoryVersion(rve);
    version.setUserName("user");
    serviceComponentDesiredStateEntity.addVersion(version);

    serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.merge(
        serviceComponentDesiredStateEntity);

    serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findByName(
        cluster.getClusterId(), serviceName, componentName);

    assertEquals(1, serviceComponentDesiredStateEntity.getVersions().size());
    ServiceComponentVersionEntity persistedVersion = serviceComponentDesiredStateEntity.getVersions().iterator().next();

    assertEquals(RepositoryVersionState.CURRENT, persistedVersion.getState());

    sc.delete(new DeleteHostComponentStatusMetaData());

    serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findByName(
        cluster.getClusterId(), serviceName, componentName);
    Assert.assertNull(serviceComponentDesiredStateEntity);


    // verify versions are gone, too
    List<ServiceComponentVersionEntity> list = serviceComponentDesiredStateDAO.findVersions(cluster.getClusterId(), serviceName, componentName);
    assertEquals(0, list.size());
  }


  @Test
  public void testUpdateStates() throws Exception {
    ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO = injector.getInstance(
        ServiceComponentDesiredStateDAO.class);

    String componentName = "NAMENODE";

    ServiceComponent component = serviceComponentFactory.createNew(service, componentName);

    StackId newStackId = new StackId("HDP-2.2.0");
    RepositoryVersionEntity repositoryVersion = helper.getOrCreateRepositoryVersion(newStackId,
        newStackId.getStackVersion());

    component.setDesiredRepositoryVersion(repositoryVersion);

    service.addServiceComponent(component);

    ServiceComponent sc = service.getServiceComponent(componentName);
    Assert.assertNotNull(sc);

    ServiceComponentDesiredStateEntity entity = serviceComponentDesiredStateDAO.findByName(cluster.getClusterId(), serviceName, componentName);

    RepositoryVersionEntity repoVersion2201 = helper.getOrCreateRepositoryVersion(
        component.getDesiredStackId(), "2.2.0.1");

    RepositoryVersionEntity repoVersion2202 = helper.getOrCreateRepositoryVersion(
        component.getDesiredStackId(), "2.2.0.2");

    addHostToCluster("h1", clusterName);
    addHostToCluster("h2", clusterName);

    sc.setDesiredState(State.INSTALLED);
    Assert.assertEquals(State.INSTALLED, sc.getDesiredState());

    ServiceComponentHost sch1 = sc.addServiceComponentHost("h1");
    ServiceComponentHost sch2 = sc.addServiceComponentHost("h2");

    // !!! case 1: component desired is UNKNOWN, mix of h-c versions
    sc.setDesiredRepositoryVersion(repositoryVersion);
    sch1.setVersion("2.2.0.1");
    sch2.setVersion("2.2.0.2");
    sc.updateRepositoryState("2.2.0.2");
    entity = serviceComponentDesiredStateDAO.findByName(cluster.getClusterId(), serviceName, componentName);
    assertEquals(RepositoryVersionState.OUT_OF_SYNC, entity.getRepositoryState());

    // !!! case 2: component desired is UNKNOWN, all h-c same version
    sc.setDesiredRepositoryVersion(repositoryVersion);
    sch1.setVersion("2.2.0.1");
    sch2.setVersion("2.2.0.1");
    sc.updateRepositoryState("2.2.0.1");
    entity = serviceComponentDesiredStateDAO.findByName(cluster.getClusterId(), serviceName, componentName);
    assertEquals(RepositoryVersionState.OUT_OF_SYNC, entity.getRepositoryState());

    // !!! case 3: component desired is known, any component reports different version
    sc.setDesiredRepositoryVersion(repoVersion2201);
    sch1.setVersion("2.2.0.1");
    sch2.setVersion("2.2.0.2");
    sc.updateRepositoryState("2.2.0.2");
    entity = serviceComponentDesiredStateDAO.findByName(cluster.getClusterId(), serviceName, componentName);
    assertEquals(RepositoryVersionState.OUT_OF_SYNC, entity.getRepositoryState());

    // !!! case 4: component desired is known, component reports same as desired, mix of h-c versions
    sc.setDesiredRepositoryVersion(repoVersion2201);
    sch1.setVersion("2.2.0.1");
    sch2.setVersion("2.2.0.2");
    sc.updateRepositoryState("2.2.0.1");
    entity = serviceComponentDesiredStateDAO.findByName(cluster.getClusterId(), serviceName, componentName);
    assertEquals(RepositoryVersionState.OUT_OF_SYNC, entity.getRepositoryState());

    // !!! case 5: component desired is known, component reports same as desired, all h-c the same
    sc.setDesiredRepositoryVersion(repoVersion2201);
    sch1.setVersion("2.2.0.1");
    sch2.setVersion("2.2.0.1");
    sc.updateRepositoryState("2.2.0.1");
    entity = serviceComponentDesiredStateDAO.findByName(cluster.getClusterId(), serviceName, componentName);
    assertEquals(RepositoryVersionState.CURRENT, entity.getRepositoryState());
  }
}
