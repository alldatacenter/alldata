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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.controller.ServiceResponse;
import org.apache.ambari.server.controller.internal.DeleteHostComponentStatusMetaData;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.ClusterServiceDAO;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.commons.collections.MapUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

public class ServiceTest {

  private Clusters clusters;
  private Cluster cluster;
  private String clusterName;
  private Injector injector;
  private ServiceFactory serviceFactory;
  private ServiceComponentFactory serviceComponentFactory;
  private ServiceComponentHostFactory serviceComponentHostFactory;
  private OrmTestHelper ormTestHelper;
  private ConfigFactory configFactory;

  private final String STACK_VERSION = "0.1";
  private final String REPO_VERSION = "0.1-1234";
  private final StackId STACK_ID = new StackId("HDP", STACK_VERSION);
  private RepositoryVersionEntity repositoryVersion;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    serviceFactory = injector.getInstance(ServiceFactory.class);
    serviceComponentFactory = injector.getInstance(ServiceComponentFactory.class);
    serviceComponentHostFactory = injector.getInstance(ServiceComponentHostFactory.class);
    configFactory = injector.getInstance(ConfigFactory.class);

    ormTestHelper = injector.getInstance(OrmTestHelper.class);
    repositoryVersion = ormTestHelper.getOrCreateRepositoryVersion(STACK_ID, REPO_VERSION);

    clusterName = "foo";
    clusters.addCluster(clusterName, STACK_ID);
    cluster = clusters.getCluster(clusterName);
    Assert.assertNotNull(cluster);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  public void testCanBeRemoved() throws Exception{
    Service service = cluster.addService("HDFS", repositoryVersion);

    for (State state : State.values()) {
      service.setDesiredState(state);
      // service does not have any components, so it can be removed,
      // even if the service is in non-removable state.
      org.junit.Assert.assertTrue(service.canBeRemoved());
    }

    ServiceComponent component = service.addServiceComponent("NAMENODE");

    // component can be removed
    component.setDesiredState(State.INSTALLED);

    for (State state : State.values()) {
      service.setDesiredState(state);
      // should always be true if the sub component can be removed
      org.junit.Assert.assertTrue(service.canBeRemoved());
    }

    // can remove a STARTED component as whether a service can be removed
    // is ultimately decided based on if the host components can be removed
    component.setDesiredState(State.INSTALLED);
    addHostToCluster("h1", service.getCluster().getClusterName());
    ServiceComponentHost sch = serviceComponentHostFactory.createNew(component, "h1");
    component.addServiceComponentHost(sch);
    sch.setDesiredState(State.STARTED);
    sch.setState(State.STARTED);

    for (State state : State.values()) {
      service.setDesiredState(state);
      // should always be false if the sub component can not be removed
      org.junit.Assert.assertFalse(service.canBeRemoved());
    }

    sch.setDesiredState(State.INSTALLED);
    sch.setState(State.INSTALLED);
  }

  @Test
  public void testGetAndSetServiceInfo() throws AmbariException {
    String serviceName = "HDFS";
    Service s = serviceFactory.createNew(cluster, serviceName, repositoryVersion);
    cluster.addService(s);

    Service service = cluster.getService(serviceName);
    Assert.assertNotNull(service);

    StackId desiredStackId = new StackId("HDP-1.2.0");
    String desiredVersion = "1.2.0-1234";

    RepositoryVersionEntity desiredRepositoryVersion = ormTestHelper.getOrCreateRepositoryVersion(
        desiredStackId, desiredVersion);

    service.setDesiredRepositoryVersion(desiredRepositoryVersion);
    Assert.assertEquals(desiredStackId, service.getDesiredStackId());

    service.setDesiredState(State.INSTALLING);
    Assert.assertEquals(State.INSTALLING, service.getDesiredState());

    // FIXME todo use DAO to verify persisted object maps to inmemory state

  }


  @Test
  public void testAddGetDeleteServiceComponents() throws AmbariException {
    String serviceName = "HDFS";
    Service s = serviceFactory.createNew(cluster, serviceName, repositoryVersion);
    cluster.addService(s);

    Service service = cluster.getService(serviceName);

    Assert.assertNotNull(service);
    Assert.assertEquals(serviceName, service.getName());
    Assert.assertEquals(cluster.getClusterId(),
            service.getCluster().getClusterId());
    Assert.assertEquals(cluster.getClusterName(),
            service.getCluster().getClusterName());
    Assert.assertEquals(State.INIT, service.getDesiredState());
    Assert.assertFalse(
            service.getDesiredStackId().getStackId().isEmpty());

    Assert.assertTrue(s.getServiceComponents().isEmpty());

    ServiceComponent sc1 =
        serviceComponentFactory.createNew(s, "NAMENODE");
    ServiceComponent sc2 =
        serviceComponentFactory.createNew(s, "DATANODE1");
    ServiceComponent sc3 =
        serviceComponentFactory.createNew(s, "DATANODE2");

    Map<String, ServiceComponent> comps = new
      HashMap<>();
    comps.put(sc1.getName(), sc1);
    comps.put(sc2.getName(), sc2);

    s.addServiceComponents(comps);

    Assert.assertEquals(2, s.getServiceComponents().size());
    Assert.assertNotNull(s.getServiceComponent(sc1.getName()));
    Assert.assertNotNull(s.getServiceComponent(sc2.getName()));

    try {
      s.getServiceComponent(sc3.getName());
      fail("Expected error when looking for invalid component");
    } catch (Exception e) {
      // Expected
    }

    s.addServiceComponent(sc3);

    ServiceComponent sc4 = s.addServiceComponent("HDFS_CLIENT");
    Assert.assertNotNull(s.getServiceComponent(sc4.getName()));
    Assert.assertEquals(State.INIT,
        s.getServiceComponent("HDFS_CLIENT").getDesiredState());
    Assert.assertTrue(sc4.isClientComponent());
    Assert.assertEquals(4, s.getServiceComponents().size());

    Assert.assertNotNull(s.getServiceComponent(sc3.getName()));
    Assert.assertEquals(sc3.getName(),
        s.getServiceComponent(sc3.getName()).getName());
    Assert.assertEquals(s.getName(),
        s.getServiceComponent(sc3.getName()).getServiceName());
    Assert.assertEquals(cluster.getClusterName(),
        s.getServiceComponent(sc3.getName()).getClusterName());

    sc4.setDesiredState(State.INSTALLING);
    Assert.assertEquals(State.INSTALLING,
        s.getServiceComponent("HDFS_CLIENT").getDesiredState());

    // delete service component
    s.deleteServiceComponent("NAMENODE", new DeleteHostComponentStatusMetaData());

    assertEquals(3, s.getServiceComponents().size());
  }

  @Test
  public void testGetAndSetConfigs() {
    // FIXME add unit tests for configs once impl done
    /*
      public Map<String, Config> getDesiredConfigs();
      public void updateDesiredConfigs(Map<String, Config> configs);
     */
  }


  @Test
  public void testConvertToResponse() throws AmbariException {
    String serviceName = "HDFS";
    Service s = serviceFactory.createNew(cluster, serviceName, repositoryVersion);
    cluster.addService(s);
    Service service = cluster.getService(serviceName);
    Assert.assertNotNull(service);

    ServiceResponse r = s.convertToResponse();
    Assert.assertEquals(s.getName(), r.getServiceName());
    Assert.assertEquals(s.getCluster().getClusterName(), r.getClusterName());
    Assert.assertEquals(s.getDesiredStackId().getStackId(), r.getDesiredStackId());
    Assert.assertEquals(s.getDesiredState().toString(), r.getDesiredState());

    StackId desiredStackId = new StackId("HDP-1.2.0");
    String desiredVersion = "1.2.0-1234";

    RepositoryVersionEntity desiredRepositoryVersion = ormTestHelper.getOrCreateRepositoryVersion(
        desiredStackId, desiredVersion);

    service.setDesiredRepositoryVersion(desiredRepositoryVersion);
    service.setDesiredState(State.INSTALLING);
    r = s.convertToResponse();
    Assert.assertEquals(s.getName(), r.getServiceName());
    Assert.assertEquals(s.getCluster().getClusterName(), r.getClusterName());
    Assert.assertEquals(s.getDesiredStackId().getStackId(), r.getDesiredStackId());
    Assert.assertEquals(s.getDesiredState().toString(), r.getDesiredState());
    // FIXME add checks for configs

    StringBuilder sb = new StringBuilder();
    s.debugDump(sb);
    // TODO better checks?
    Assert.assertFalse(sb.toString().isEmpty());

  }

  @Test
  public void testServiceMaintenance() throws Exception {
    String serviceName = "HDFS";
    Service s = serviceFactory.createNew(cluster, serviceName, repositoryVersion);
    cluster.addService(s);

    Service service = cluster.getService(serviceName);
    Assert.assertNotNull(service);

    ClusterServiceDAO dao = injector.getInstance(ClusterServiceDAO.class);
    ClusterServiceEntity entity = dao.findByClusterAndServiceNames(clusterName, serviceName);
    Assert.assertNotNull(entity);
    Assert.assertEquals(MaintenanceState.OFF, entity.getServiceDesiredStateEntity().getMaintenanceState());
    Assert.assertEquals(MaintenanceState.OFF, service.getMaintenanceState());

    service.setMaintenanceState(MaintenanceState.ON);
    Assert.assertEquals(MaintenanceState.ON, service.getMaintenanceState());

    entity = dao.findByClusterAndServiceNames(clusterName, serviceName);
    Assert.assertNotNull(entity);
    Assert.assertEquals(MaintenanceState.ON, entity.getServiceDesiredStateEntity().getMaintenanceState());
  }

  /**
   * Tests the kerberosEnabledTest value set in the HDFS metainfo file (stacks/HDP/0.1/services/HDFS/metainfo.xml):
   * <pre>
   * {
   *   "or": [
   *     {
   *       "equals": [
   *         "core-site/hadoop.security.authentication",
   *         "kerberos"
   *       ]
   *     },
   *     {
   *       "equals": [
   *         "hdfs-site/hadoop.security.authentication",
   *         "kerberos"
   *       ]
   *     }
   *   ]
   * }
   * </pre>
   */
  @Test
  public void testServiceKerberosEnabledTest() throws Exception {
    String serviceName = "HDFS";
    Service s = serviceFactory.createNew(cluster, serviceName, repositoryVersion);
    cluster.addService(s);

    Service service = cluster.getService(serviceName);
    Assert.assertNotNull(service);


    Map<String, Map<String, String>> map = new HashMap<>();

    Assert.assertFalse(service.isKerberosEnabled(null));

    Assert.assertFalse(service.isKerberosEnabled(map));

    map.put("core-site", Collections.singletonMap("hadoop.security.authentication", "none"));
    map.put("hdfs-site", Collections.singletonMap("hadoop.security.authentication", "none"));
    Assert.assertFalse(service.isKerberosEnabled(map));

    map.put("core-site", Collections.singletonMap("hadoop.security.authentication", "kerberos"));
    map.put("hdfs-site", Collections.singletonMap("hadoop.security.authentication", "none"));
    Assert.assertTrue(service.isKerberosEnabled(map));

    map.put("core-site", Collections.singletonMap("hadoop.security.authentication", "none"));
    map.put("hdfs-site", Collections.singletonMap("hadoop.security.authentication", "kerberos"));
    Assert.assertTrue(service.isKerberosEnabled(map));

    map.put("core-site", Collections.singletonMap("hadoop.security.authentication", "kerberos"));
    map.put("hdfs-site", Collections.singletonMap("hadoop.security.authentication", "kerberos"));
    Assert.assertTrue(service.isKerberosEnabled(map));

  }

  @Test
  public void testClusterConfigsUnmappingOnDeleteAllServiceConfigs() throws AmbariException {
    String serviceName = "HDFS";
    Service s = serviceFactory.createNew(cluster, serviceName, repositoryVersion);
    cluster.addService(s);

    Service service = cluster.getService(serviceName);

    // add some cluster and service configs
    Config config1 = configFactory.createNew(cluster, "hdfs-site", "version1",
        new HashMap<String, String>() {{ put("a", "b"); }}, new HashMap<>());
    cluster.addDesiredConfig("admin", Collections.singleton(config1));

    Config config2 = configFactory.createNew(cluster, "hdfs-site", "version2",
        new HashMap<String, String>() {{ put("a", "b"); }}, new HashMap<>());
    cluster.addDesiredConfig("admin", Collections.singleton(config2));


    // check all cluster configs are mapped
    Collection<ClusterConfigEntity> clusterConfigEntities = cluster.getClusterEntity().getClusterConfigEntities();
    for (ClusterConfigEntity clusterConfigEntity : clusterConfigEntities) {
      assertFalse(clusterConfigEntity.isUnmapped());
    }

    ((ServiceImpl)service).deleteAllServiceConfigs();

    // check all cluster configs are unmapped
    clusterConfigEntities = cluster.getClusterEntity().getClusterConfigEntities();
    for (ClusterConfigEntity clusterConfigEntity : clusterConfigEntities) {
      assertTrue(clusterConfigEntity.isUnmapped());
    }
  }

  @Test
  public void testDeleteAllServiceConfigGroups() throws AmbariException {
    String serviceName = "HDFS";
    Service s = serviceFactory.createNew(cluster, serviceName, repositoryVersion);
    cluster.addService(s);

    Service service = cluster.getService(serviceName);

    ConfigGroup configGroup1 = createNiceMock(ConfigGroup.class);
    ConfigGroup configGroup2 = createNiceMock(ConfigGroup.class);

    expect(configGroup1.getId()).andReturn(1L).anyTimes();
    expect(configGroup2.getId()).andReturn(2L).anyTimes();

    expect(configGroup1.getServiceName()).andReturn(serviceName).anyTimes();
    expect(configGroup2.getServiceName()).andReturn(serviceName).anyTimes();

    expect(configGroup1.getClusterName()).andReturn(cluster.getClusterName()).anyTimes();
    expect(configGroup2.getClusterName()).andReturn(cluster.getClusterName()).anyTimes();

    replay(configGroup1, configGroup2);

    cluster.addConfigGroup(configGroup1);
    cluster.addConfigGroup(configGroup2);

    ((ServiceImpl)service).deleteAllServiceConfigGroups();

    assertTrue(MapUtils.isEmpty(cluster.getConfigGroupsByServiceName(serviceName)));

    verify(configGroup1, configGroup2);
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
}
