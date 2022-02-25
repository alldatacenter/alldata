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

package org.apache.ambari.server.controller.internal;

import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ExecuteActionRequest;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ResourceProviderFactory;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepoDefinitionEntity;
import org.apache.ambari.server.orm.entities.RepoOsEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.ServiceOsSpecific;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.utils.StageUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

/**
 * ClusterStackVersionResourceProvider tests.
 */
public class HostStackVersionResourceProviderTest {

  private Injector injector;
  private AmbariMetaInfo ambariMetaInfo;
  private RepositoryVersionDAO repositoryVersionDAOMock;
  private HostVersionDAO hostVersionDAOMock;
  private ConfigHelper configHelper;
  private AmbariManagementController managementController;
  private Clusters clusters;
  private Cluster cluster;
  private RequestStatusResponse response;
  private ResourceProviderFactory resourceProviderFactory;
  private ResourceProvider csvResourceProvider;
  private ActionManager actionManager;
  private Capture<org.apache.ambari.server.actionmanager.Request> requestCapture;
  private Capture<ExecuteActionRequest> executeActionRequestCapture;
  private HostVersionEntity hostVersionEntityMock;
  private RepositoryVersionEntity repoVersion;
  private Resource.Type type = Resource.Type.HostStackVersion;

  private List<RepoOsEntity> operatingSystemsEn = new ArrayList<>();

  {
    RepoDefinitionEntity repoDefinitionEntity1 = new RepoDefinitionEntity();
    repoDefinitionEntity1.setRepoID("HDP-UTILS-1.1.0.20");
    repoDefinitionEntity1.setBaseUrl("http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos5/2.x/updates/2.2.0.0");
    repoDefinitionEntity1.setRepoName("HDP-UTILS");
    RepoDefinitionEntity repoDefinitionEntity2 = new RepoDefinitionEntity();
    repoDefinitionEntity2.setRepoID("HDP-2.2");
    repoDefinitionEntity2.setBaseUrl("http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos5/2.x/updates/2.2.0.0");
    repoDefinitionEntity2.setRepoName("HDP");
    RepoOsEntity repoOsEntity = new RepoOsEntity();
    repoOsEntity.setFamily("redhat6");
    repoOsEntity.setAmbariManaged(true);
    repoOsEntity.addRepoDefinition(repoDefinitionEntity1);
    repoOsEntity.addRepoDefinition(repoDefinitionEntity2);
    operatingSystemsEn.add(repoOsEntity);
  }

  @Before
  public void setup() throws Exception {
    // Create instances of mocks
    repositoryVersionDAOMock = createNiceMock(RepositoryVersionDAO.class);
    hostVersionDAOMock = createNiceMock(HostVersionDAO.class);
    configHelper = createNiceMock(ConfigHelper.class);
    // Initialize injector
    InMemoryDefaultTestModule module = new InMemoryDefaultTestModule();
    injector = Guice.createInjector(Modules.override(module).with(new MockModule()));
    injector.getInstance(GuiceJpaInitializer.class);
    ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    managementController = createMock(AmbariManagementController.class);
    clusters = createNiceMock(Clusters.class);
    cluster = createNiceMock(Cluster.class);
    response = createNiceMock(RequestStatusResponse.class);
    resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    csvResourceProvider = createNiceMock(ClusterStackVersionResourceProvider.class);
    hostVersionEntityMock = createNiceMock(HostVersionEntity.class);
    actionManager = createNiceMock(ActionManager.class);
    repoVersion = new RepositoryVersionEntity();
    repoVersion.addRepoOsEntities(operatingSystemsEn);

    StackEntity stack = new StackEntity();
    stack.setStackName("HDP");
    stack.setStackVersion("2.0.1");
    repoVersion.setStack(stack);
    repoVersion.setVersion("2.2");
    repoVersion.setId(1L);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }


  @Test
  public void testGetResources() throws Exception {
    // add the property map to a set for the request.  add more maps for multiple creates
    String hostname = "host1";
    String clustername = "Cluster100";

    Predicate predicate = new PredicateBuilder().begin()
            .property(HostStackVersionResourceProvider.HOST_STACK_VERSION_HOST_NAME_PROPERTY_ID).equals(hostname)
            .and()
            .property(HostStackVersionResourceProvider.HOST_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID).equals(clustername)
            .end().and().begin()
            .property(HostStackVersionResourceProvider.HOST_STACK_VERSION_STATE_PROPERTY_ID).equals("INSTALLING")
            .or()
            .property(HostStackVersionResourceProvider.HOST_STACK_VERSION_STATE_PROPERTY_ID).equals("INSTALL_FAILED")
            .or()
            .property(HostStackVersionResourceProvider.HOST_STACK_VERSION_STATE_PROPERTY_ID).equals("OUT_OF_SYNC")
            .end().toPredicate();
    // create the request
    Request request = PropertyHelper.getCreateRequest(Collections.emptySet(), null);
    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
            type,
            managementController);

    expect(hostVersionDAOMock.findByClusterAndHost(clustername, hostname)).andReturn(Collections.singletonList(hostVersionEntityMock));
    expect(hostVersionEntityMock.getRepositoryVersion()).andReturn(repoVersion).anyTimes();
    expect(repositoryVersionDAOMock.findByStackAndVersion(isA(StackId.class), isA(String.class))).andReturn(null).anyTimes();
    expect(hostVersionEntityMock.getState()).andReturn(RepositoryVersionState.INSTALLING).anyTimes();

    replay(hostVersionDAOMock, hostVersionEntityMock, repositoryVersionDAOMock);
    Set<Resource> resources = provider.getResources(request, predicate);
    assertEquals(1, resources.size());
    verify(hostVersionDAOMock, hostVersionEntityMock, repositoryVersionDAOMock);
  }

  @Test
  public void testCreateResources() throws Exception {
    StackId stackId = new StackId("HDP", "2.0.1");

    final Host host1 = createNiceMock("host1", Host.class);
    expect(host1.getHostName()).andReturn("host1").anyTimes();
    expect(host1.getOsFamily()).andReturn("redhat6").anyTimes();
    replay(host1);
    Map<String, Host> hostsForCluster = Collections.singletonMap(host1.getHostName(), host1);

    ServiceComponentHost sch = createMock(ServiceComponentHost.class);
    List<ServiceComponentHost> schs = Collections.singletonList(sch);



    final ServiceOsSpecific.Package hivePackage = new ServiceOsSpecific.Package();
    hivePackage.setName("hive");
    final ServiceOsSpecific.Package mysqlPackage = new ServiceOsSpecific.Package();
    mysqlPackage.setName("mysql");
    mysqlPackage.setSkipUpgrade(Boolean.TRUE);
    List<ServiceOsSpecific.Package> packages = Arrays.asList(hivePackage, mysqlPackage);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Map<String, Map<String, String>> hostConfigTags = new HashMap<>();
    expect(configHelper.getEffectiveDesiredTags(anyObject(ClusterImpl.class), anyObject(String.class))).andReturn(hostConfigTags);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(managementController.getJdkResourceUrl()).andReturn("/JdkResourceUrl").anyTimes();
    expect(managementController.getPackagesForServiceHost(anyObject(ServiceInfo.class),
            EasyMock.anyObject(), anyObject(String.class))).andReturn(packages).anyTimes();

    expect(resourceProviderFactory.getHostResourceProvider(
        eq(managementController))).andReturn(csvResourceProvider).anyTimes();

    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster);
    expect(clusters.getHost(anyObject(String.class))).andReturn(host1);
    expect(cluster.getHosts()).andReturn(hostsForCluster.values()).atLeastOnce();
    expect(cluster.getServices()).andReturn(new HashMap<>()).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(stackId);
    expect(cluster.getServiceComponentHosts(anyObject(String.class))).andReturn(schs).anyTimes();

    expect(sch.getServiceName()).andReturn("HIVE").anyTimes();

    expect(
        repositoryVersionDAOMock.findByStackAndVersion(
            anyObject(StackId.class),
            anyObject(String.class))).andReturn(repoVersion);

    expect(
        hostVersionDAOMock.findByClusterStackVersionAndHost(
            anyObject(String.class), anyObject(StackId.class),
            anyObject(String.class), anyObject(String.class))).andReturn(
        hostVersionEntityMock);

    expect(hostVersionEntityMock.getState()).andReturn(RepositoryVersionState.INSTALL_FAILED).anyTimes();

    expect(actionManager.getRequestTasks(anyLong())).andReturn(Collections.emptyList()).anyTimes();

    StageUtils.setTopologyManager(injector.getInstance(TopologyManager.class));
    StageUtils.setConfiguration(injector.getInstance(Configuration.class));

    // replay
    replay(managementController, response, clusters, resourceProviderFactory, csvResourceProvider,
            cluster, repositoryVersionDAOMock, configHelper, sch, actionManager, hostVersionEntityMock, hostVersionDAOMock);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    injector.injectMembers(provider);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    Map<String, Object> properties = new LinkedHashMap<>();

    // add properties to the request map
    properties.put(HostStackVersionResourceProvider.HOST_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(HostStackVersionResourceProvider.HOST_STACK_VERSION_REPO_VERSION_PROPERTY_ID, "2.2.0.1-885");
    properties.put(HostStackVersionResourceProvider.HOST_STACK_VERSION_STACK_PROPERTY_ID, "HDP");
    properties.put(HostStackVersionResourceProvider.HOST_STACK_VERSION_VERSION_PROPERTY_ID, "2.0.1");
    properties.put(HostStackVersionResourceProvider.HOST_STACK_VERSION_HOST_NAME_PROPERTY_ID, "host1");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    // verify
    verify(managementController, response, clusters);
  }

  @Test
  public void testCreateResources_on_host_not_belonging_To_any_cluster() throws Exception {
    StackId stackId = new StackId("HDP", "2.0.1");

    final Host host1 = createNiceMock("host1", Host.class);
    expect(host1.getHostName()).andReturn("host1").anyTimes();
    expect(host1.getOsFamily()).andReturn("redhat6").anyTimes();
    replay(host1);
    Map<String, Host> hostsForCluster = new HashMap<>();

    ServiceComponentHost sch = createMock(ServiceComponentHost.class);

    final ServiceOsSpecific.Package hivePackage = new ServiceOsSpecific.Package();
    hivePackage.setName("hive");
    final ServiceOsSpecific.Package mysqlPackage = new ServiceOsSpecific.Package();
    mysqlPackage.setName("mysql");
    mysqlPackage.setSkipUpgrade(Boolean.TRUE);
    List<ServiceOsSpecific.Package> packages = Arrays.asList(hivePackage, mysqlPackage);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Map<String, Map<String, String>> hostConfigTags = new HashMap<>();
    expect(configHelper.getEffectiveDesiredTags(anyObject(ClusterImpl.class), anyObject(String.class))).andReturn(hostConfigTags);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(managementController.getJdkResourceUrl()).andReturn("/JdkResourceUrl").anyTimes();
    expect(managementController.getPackagesForServiceHost(anyObject(ServiceInfo.class),
      anyObject(Map.class), anyObject(String.class))).andReturn(packages).anyTimes();

    expect(resourceProviderFactory.getHostResourceProvider(
      eq(managementController))).andReturn(csvResourceProvider).anyTimes();

    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster);
    expect(clusters.getHost(anyObject(String.class))).andReturn(host1);
    expect(cluster.getHosts()).andReturn(hostsForCluster.values()).atLeastOnce();
    expect(cluster.getServices()).andReturn(new HashMap<>()).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(stackId);

    expect(
      repositoryVersionDAOMock.findByStackAndVersion(
        anyObject(StackId.class),
        anyObject(String.class))).andReturn(repoVersion);

    expect(actionManager.getRequestTasks(anyLong())).andReturn(Collections.emptyList()).anyTimes();
    requestCapture = newCapture();
    executeActionRequestCapture = newCapture();
    actionManager.sendActions(capture(requestCapture), capture(executeActionRequestCapture));

    StageUtils.setTopologyManager(injector.getInstance(TopologyManager.class));
    StageUtils.setConfiguration(injector.getInstance(Configuration.class));

    // replay
    replay(managementController, response, clusters, resourceProviderFactory, csvResourceProvider,
      cluster, repositoryVersionDAOMock, configHelper, sch, actionManager, hostVersionEntityMock, hostVersionDAOMock);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
      type,
      managementController);

    injector.injectMembers(provider);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    Map<String, Object> properties = new LinkedHashMap<>();

    // add properties to the request map
    properties.put(HostStackVersionResourceProvider.HOST_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(HostStackVersionResourceProvider.HOST_STACK_VERSION_REPO_VERSION_PROPERTY_ID, "2.2.0.1-885");
    properties.put(HostStackVersionResourceProvider.HOST_STACK_VERSION_STACK_PROPERTY_ID, "HDP");
    properties.put(HostStackVersionResourceProvider.HOST_STACK_VERSION_VERSION_PROPERTY_ID, "2.0.1");
    properties.put(HostStackVersionResourceProvider.HOST_STACK_VERSION_HOST_NAME_PROPERTY_ID, "host1");
    Set<Map<String, String>> components = new HashSet<>();
    Map<String, String> hiveMetastoreComponent = new HashMap<>();
    hiveMetastoreComponent.put("name", "HIVE_METASTORE");
    components.add(hiveMetastoreComponent);
    Map<String, String> hiveServerstoreComponent = new HashMap<>();
    hiveServerstoreComponent.put("name", "HIVE_SERVER");
    components.add(hiveServerstoreComponent);
    properties.put(HostStackVersionResourceProvider.HOST_STACK_VERSION_COMPONENT_NAMES_PROPERTY_ID, components);
    properties.put(HostStackVersionResourceProvider.HOST_STACK_VERSION_FORCE_INSTALL_ON_NON_MEMBER_HOST_PROPERTY_ID,
      "true");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    // verify
    verify(managementController, response, clusters);

    assertEquals(requestCapture.getValue().getStages().size(), 2);
    List<Stage> stages = new ArrayList<>(requestCapture.getValue().getStages());
    Stage installStage = stages.get(0);
    Stage selectStackStage = stages.get(1);
    String hostName = "host1";

    assertEquals(1, installStage.getExecutionCommands().size());
    assertEquals(hostName, installStage.getExecutionCommands().keySet().iterator().next());
    assertEquals(1, installStage.getExecutionCommands().get(hostName).size());
    assertEquals(true, installStage.getExecutionCommands().get(hostName).get(0).getExecutionCommand().isOverrideConfigs());

    assertEquals(1, selectStackStage.getExecutionCommands().size());
    assertEquals(hostName, selectStackStage.getExecutionCommands().keySet().iterator().next());
    assertEquals(1, selectStackStage.getExecutionCommands().get(hostName).size());
    assertEquals(false, selectStackStage.getExecutionCommands().get(hostName).get(0).getExecutionCommand().isOverrideConfigs());
  }

  @Test
  public void testCreateResources_in_out_of_sync_state() throws Exception {
    StackId stackId = new StackId("HDP", "2.0.1");

    final Host host1 = createNiceMock("host1", Host.class);
    expect(host1.getHostName()).andReturn("host1").anyTimes();
    expect(host1.getOsFamily()).andReturn("redhat6").anyTimes();
    replay(host1);
    Map<String, Host> hostsForCluster = Collections.singletonMap(host1.getHostName(), host1);

    ServiceComponentHost sch = createMock(ServiceComponentHost.class);
    List<ServiceComponentHost> schs = Collections.singletonList(sch);

    ServiceOsSpecific.Package hivePackage = new ServiceOsSpecific.Package();
    hivePackage.setName("hive");
    List<ServiceOsSpecific.Package> packages = Collections.singletonList(hivePackage);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Map<String, Map<String, String>> hostConfigTags = new HashMap<>();
    expect(configHelper.getEffectiveDesiredTags(anyObject(ClusterImpl.class), anyObject(String.class))).andReturn(hostConfigTags);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(managementController.getJdkResourceUrl()).andReturn("/JdkResourceUrl").anyTimes();
    expect(managementController.getPackagesForServiceHost(anyObject(ServiceInfo.class),
            EasyMock.anyObject(), anyObject(String.class))).andReturn(packages).anyTimes();

    expect(resourceProviderFactory.getHostResourceProvider(
            eq(managementController))).andReturn(csvResourceProvider).anyTimes();

    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster);
    expect(clusters.getHost(anyObject(String.class))).andReturn(host1);
    expect(cluster.getHosts()).andReturn(hostsForCluster.values()).atLeastOnce();
    expect(cluster.getServices()).andReturn(new HashMap<>()).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(stackId);
    expect(cluster.getServiceComponentHosts(anyObject(String.class))).andReturn(schs).anyTimes();

    expect(sch.getServiceName()).andReturn("HIVE").anyTimes();

    expect(
        repositoryVersionDAOMock.findByStackAndVersion(
            anyObject(StackId.class),
            anyObject(String.class))).andReturn(repoVersion);

    expect(
        hostVersionDAOMock.findByClusterStackVersionAndHost(
            anyObject(String.class), anyObject(StackId.class),
            anyObject(String.class), anyObject(String.class))).andReturn(
        hostVersionEntityMock);

    expect(hostVersionEntityMock.getState()).andReturn(RepositoryVersionState.OUT_OF_SYNC).anyTimes();

    expect(actionManager.getRequestTasks(anyLong())).andReturn(Collections.emptyList()).anyTimes();

    StageUtils.setTopologyManager(injector.getInstance(TopologyManager.class));
    StageUtils.setConfiguration(injector.getInstance(Configuration.class));

    // replay
    replay(managementController, response, clusters, resourceProviderFactory, csvResourceProvider,
            cluster, repositoryVersionDAOMock, configHelper, sch, actionManager, hostVersionEntityMock, hostVersionDAOMock);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
            type,
            managementController);

    injector.injectMembers(provider);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    Map<String, Object> properties = new LinkedHashMap<>();

    // add properties to the request map
    properties.put(HostStackVersionResourceProvider.HOST_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(HostStackVersionResourceProvider.HOST_STACK_VERSION_REPO_VERSION_PROPERTY_ID, "2.2.0.1-885");
    properties.put(HostStackVersionResourceProvider.HOST_STACK_VERSION_STACK_PROPERTY_ID, "HDP");
    properties.put(HostStackVersionResourceProvider.HOST_STACK_VERSION_VERSION_PROPERTY_ID, "2.0.1");
    properties.put(HostStackVersionResourceProvider.HOST_STACK_VERSION_HOST_NAME_PROPERTY_ID, "host1");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    // verify
    verify(managementController, response, clusters);
  }

  public class MockModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(RepositoryVersionDAO.class).toInstance(repositoryVersionDAOMock);
      bind(HostVersionDAO.class).toInstance(hostVersionDAOMock);
      bind(ConfigHelper.class).toInstance(configHelper);
    }
  }
}
