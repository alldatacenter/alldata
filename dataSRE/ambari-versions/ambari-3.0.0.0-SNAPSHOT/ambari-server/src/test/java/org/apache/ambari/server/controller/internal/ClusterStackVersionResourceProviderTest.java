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

 import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.File;
import java.io.FileInputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.agent.CommandRepository;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.agent.ExecutionCommand.KeyNames;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ExecuteActionRequest;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ResourceProviderFactory;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepoDefinitionEntity;
import org.apache.ambari.server.orm.entities.RepoOsEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.stack.upgrade.Direction;
import org.apache.ambari.server.stack.upgrade.RepositoryVersionHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.ServiceOsSpecific;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.apache.ambari.server.state.repository.ClusterVersionSummary;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.ambari.spi.RepositoryType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;


 /**
 * ClusterStackVersionResourceProvider tests.
 */
public class ClusterStackVersionResourceProviderTest {

  public static final int MAX_TASKS_PER_STAGE = 2;
  private Injector injector;
  private AmbariMetaInfo ambariMetaInfo;
  private RepositoryVersionDAO repositoryVersionDAOMock;
  private ConfigHelper configHelper;
  private Configuration configuration;
  private StageFactory stageFactory;

  private HostVersionDAO hostVersionDAO;
  private HostComponentStateDAO hostComponentStateDAO;

  private Clusters clusters;
  private ActionManager actionManager;
  private AmbariManagementController managementController;

   public static final List<RepoOsEntity> REPO_OS_ENTITIES = new ArrayList<>();

   static {
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
     REPO_OS_ENTITIES.add(repoOsEntity);
   }

   public static final List<RepoOsEntity> REPO_OS_NOT_MANAGED = new ArrayList<>();

   static {
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
     repoOsEntity.setAmbariManaged(false);
     repoOsEntity.addRepoDefinition(repoDefinitionEntity1);
     repoOsEntity.addRepoDefinition(repoDefinitionEntity2);
     REPO_OS_NOT_MANAGED.add(repoOsEntity);
   }

  @Before
  public void setup() throws Exception {
    // Create instances of mocks
    repositoryVersionDAOMock = createNiceMock(RepositoryVersionDAO.class);
    hostVersionDAO = createNiceMock(HostVersionDAO.class);
    hostComponentStateDAO = createNiceMock(HostComponentStateDAO.class);

    configHelper = createNiceMock(ConfigHelper.class);
    InMemoryDefaultTestModule inMemoryModule = new InMemoryDefaultTestModule();
    Properties properties = inMemoryModule.getProperties();
    properties.setProperty(Configuration.AGENT_PACKAGE_PARALLEL_COMMANDS_LIMIT.getKey(),
            String.valueOf(MAX_TASKS_PER_STAGE));
    configuration = new Configuration(properties);
    stageFactory = createNiceMock(StageFactory.class);

    clusters = createNiceMock(Clusters.class);
    actionManager = createNiceMock(ActionManager.class);
    managementController = createMock(AmbariManagementController.class);

    // Initialize injector
    injector = Guice.createInjector(Modules.override(inMemoryModule).with(new MockModule()));
    injector.getInstance(GuiceJpaInitializer.class);
    ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);

    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testCreateResourcesAsAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator());
  }

   @Test
   public void testCreateResourcesAsClusterAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator());
  }

   @Test(expected = AuthorizationException.class)
   public void testCreateResourcesAsClusterOperator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterOperator());
  }

  private void testCreateResources(Authentication authentication) throws Exception {
    Cluster cluster = createNiceMock(Cluster.class);
    Map<String, String> hostLevelParams = new HashMap<>();
    StackId stackId = new StackId("HDP", "2.0.1");

    StackEntity stackEntity = new StackEntity();
    stackEntity.setStackName("HDP");
    stackEntity.setStackVersion("2.1.1");

    RepositoryVersionEntity repoVersion = new RepositoryVersionEntity();
    repoVersion.setId(1l);
    repoVersion.addRepoOsEntities(REPO_OS_ENTITIES);
    repoVersion.setStack(stackEntity);

    final String hostWithoutVersionableComponents = "host2";

    List<Host> hostsNeedingInstallCommands = new ArrayList<>();
    Map<String, Host> hostsForCluster = new HashMap<>();
    int hostCount = 10;
    for (int i = 0; i < hostCount; i++) {
      String hostname = "host" + i;
      Host host = createNiceMock(hostname, Host.class);
      expect(host.getHostName()).andReturn(hostname).anyTimes();
      expect(host.getOsFamily()).andReturn("redhat6").anyTimes();
      expect(host.getMaintenanceState(EasyMock.anyLong())).andReturn(
          MaintenanceState.OFF).anyTimes();
      expect(host.getAllHostVersions()).andReturn(
          Collections.emptyList()).anyTimes();

      replay(host);
      hostsForCluster.put(hostname, host);

      if (!StringUtils.equals(hostWithoutVersionableComponents, hostname)) {
        hostsNeedingInstallCommands.add(host);
      }
    }

    final ServiceComponentHost schDatanode = createMock(ServiceComponentHost.class);
    expect(schDatanode.getServiceName()).andReturn("HDFS").anyTimes();
    expect(schDatanode.getServiceComponentName()).andReturn("DATANODE").anyTimes();
    final ServiceComponentHost schNamenode = createMock(ServiceComponentHost.class);
    expect(schNamenode.getServiceName()).andReturn("HDFS").anyTimes();
    expect(schNamenode.getServiceComponentName()).andReturn("NAMENODE").anyTimes();
    final ServiceComponentHost schAMS = createMock(ServiceComponentHost.class);
    expect(schAMS.getServiceName()).andReturn("AMBARI_METRICS").anyTimes();
    expect(schAMS.getServiceComponentName()).andReturn("METRICS_COLLECTOR").anyTimes();
    // First host contains versionable components
    final List<ServiceComponentHost> schsH1 = Lists.newArrayList(schDatanode, schNamenode, schAMS);
    // Second host does not contain versionable components
    final List<ServiceComponentHost> schsH2 = Lists.newArrayList(schAMS);

    ServiceOsSpecific.Package hdfsPackage = new ServiceOsSpecific.Package();
    hdfsPackage.setName("hdfs");
    List<ServiceOsSpecific.Package> packages = Collections.singletonList(hdfsPackage);

    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    Map<String, Map<String, String>> hostConfigTags = new HashMap<>();
    expect(configHelper.getEffectiveDesiredTags(anyObject(ClusterImpl.class), anyObject(String.class))).andReturn(hostConfigTags);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getAuthName()).andReturn("admin").anyTimes();
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(managementController.getJdkResourceUrl()).andReturn("/JdkResourceUrl").anyTimes();
    expect(managementController.getPackagesForServiceHost(anyObject(ServiceInfo.class),
            EasyMock.<Map<String, String>>anyObject(), anyObject(String.class))).
            andReturn(packages).anyTimes();
    expect(managementController.findConfigurationTagsWithOverrides(anyObject(Cluster.class), EasyMock.anyString()))
      .andReturn(new HashMap<String, Map<String, String>>()).anyTimes();

    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster);
    expect(clusters.getHostsForCluster(anyObject(String.class))).andReturn(
        hostsForCluster).anyTimes();

    String clusterName = "Cluster100";
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(cluster.getHosts()).andReturn(hostsForCluster.values()).atLeastOnce();
    expect(cluster.getServices()).andReturn(new HashMap<>()).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(stackId);
    expect(cluster.getServiceComponentHosts(anyObject(String.class))).andAnswer(new IAnswer<List<ServiceComponentHost>>() {
      @Override
      public List<ServiceComponentHost> answer() throws Throwable {
        String hostname = (String) EasyMock.getCurrentArguments()[0];
            if (hostname.equals(hostWithoutVersionableComponents)) {
          return schsH2;
        } else {
          return schsH1;
        }
      }
    }).anyTimes();

    expect(cluster.transitionHostsToInstalling(
        anyObject(RepositoryVersionEntity.class), anyObject(VersionDefinitionXml.class),
        eq(false))).andReturn(hostsNeedingInstallCommands).atLeastOnce();

    ExecutionCommand executionCommand = createNiceMock(ExecutionCommand.class);
    ExecutionCommandWrapper executionCommandWrapper = createNiceMock(ExecutionCommandWrapper.class);

    expect(executionCommandWrapper.getExecutionCommand()).andReturn(executionCommand).anyTimes();

    Stage stage = createNiceMock(Stage.class);
    expect(stage.getExecutionCommandWrapper(anyObject(String.class), anyObject(String.class))).
            andReturn(executionCommandWrapper).anyTimes();

    expect(executionCommand.getHostLevelParams()).andReturn(hostLevelParams).anyTimes();

    Map<Role, Float> successFactors = new HashMap<>();
    expect(stage.getSuccessFactors()).andReturn(successFactors).atLeastOnce();

    // Check that we create proper stage count
    expect(stageFactory.createNew(anyLong(), anyObject(String.class),
            anyObject(String.class), anyLong(),
            anyObject(String.class), anyObject(String.class),
            anyObject(String.class))).andReturn(stage).
            times((int) Math.ceil(hostCount / MAX_TASKS_PER_STAGE));

    expect(
            repositoryVersionDAOMock.findByStackAndVersion(
                    anyObject(StackId.class),
                    anyObject(String.class))).andReturn(repoVersion);

    expect(actionManager.getRequestTasks(anyLong())).andReturn(Collections.emptyList()).anyTimes();

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterId(1l);
    clusterEntity.setClusterName(clusterName);

    StageUtils.setTopologyManager(injector.getInstance(TopologyManager.class));
    StageUtils.setConfiguration(injector.getInstance(Configuration.class));

    ResourceProvider provider = createProvider(managementController);
    injector.injectMembers(provider);

    // replay
    replay(managementController, response, clusters,
            cluster, repositoryVersionDAOMock, configHelper, schDatanode, schNamenode, schAMS, actionManager,
            executionCommand, executionCommandWrapper,stage, stageFactory);


    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    Map<String, Object> properties = new LinkedHashMap<>();

    // add properties to the request map
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID, "2.2.0.1-885");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_STACK_PROPERTY_ID, "HDP");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID, "2.1.1");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    RequestStatus status = provider.createResources(request);
    Assert.assertNotNull(status);

    // verify
    verify(managementController, response, clusters, stageFactory, stage);

    // check that the success factor was populated in the stage
    Float successFactor = successFactors.get(Role.INSTALL_PACKAGES);
    Assert.assertEquals(Float.valueOf(0.85f), successFactor);
  }

  @Experimental(feature=ExperimentalFeature.PATCH_UPGRADES)
  public void testCreateResourcesForPatch() throws Exception {
    Resource.Type type = Resource.Type.ClusterStackVersion;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    StackId stackId = new StackId("HDP", "2.0.1");

    File f = new File("src/test/resources/hbase_version_test.xml");

    RepositoryVersionEntity repoVersion = new RepositoryVersionEntity();
    repoVersion.setId(1l);
    repoVersion.addRepoOsEntities(REPO_OS_ENTITIES);
    repoVersion.setVersionXml(IOUtils.toString(new FileInputStream(f)));
    repoVersion.setVersionXsd("version_definition.xsd");
    repoVersion.setType(RepositoryType.PATCH);

    ambariMetaInfo.getComponent("HDP", "2.1.1", "HBASE", "HBASE_MASTER").setVersionAdvertised(true);


    Map<String, Host> hostsForCluster = new HashMap<>();
    int hostCount = 10;
    for (int i = 0; i < hostCount; i++) {
      String hostname = "host" + i;
      Host host = createNiceMock(hostname, Host.class);
      expect(host.getHostName()).andReturn(hostname).anyTimes();
      expect(host.getOsFamily()).andReturn("redhat6").anyTimes();
      expect(host.getMaintenanceState(EasyMock.anyLong())).andReturn(
          MaintenanceState.OFF).anyTimes();
      expect(host.getAllHostVersions()).andReturn(
          Collections.emptyList()).anyTimes();

      replay(host);
      hostsForCluster.put(hostname, host);
    }

    Service hdfsService = createNiceMock(Service.class);
    Service hbaseService = createNiceMock(Service.class);
    expect(hdfsService.getName()).andReturn("HDFS").anyTimes();
    expect(hbaseService.getName()).andReturn("HBASE").anyTimes();

    expect(hdfsService.getServiceComponents()).andReturn(new HashMap<>());
    expect(hbaseService.getServiceComponents()).andReturn(new HashMap<>());

    Map<String, Service> serviceMap = new HashMap<>();
    serviceMap.put("HDFS", hdfsService);
    serviceMap.put("HBASE", hbaseService);

    final ServiceComponentHost schDatanode = createMock(ServiceComponentHost.class);
    expect(schDatanode.getServiceName()).andReturn("HDFS").anyTimes();
    expect(schDatanode.getServiceComponentName()).andReturn("DATANODE").anyTimes();

    final ServiceComponentHost schNamenode = createMock(ServiceComponentHost.class);
    expect(schNamenode.getServiceName()).andReturn("HDFS").anyTimes();
    expect(schNamenode.getServiceComponentName()).andReturn("NAMENODE").anyTimes();

    final ServiceComponentHost schAMS = createMock(ServiceComponentHost.class);
    expect(schAMS.getServiceName()).andReturn("AMBARI_METRICS").anyTimes();
    expect(schAMS.getServiceComponentName()).andReturn("METRICS_COLLECTOR").anyTimes();

    final ServiceComponentHost schHBM = createMock(ServiceComponentHost.class);
    expect(schHBM.getServiceName()).andReturn("HBASE").anyTimes();
    expect(schHBM.getServiceComponentName()).andReturn("HBASE_MASTER").anyTimes();

    // First host contains versionable components
    final List<ServiceComponentHost> schsH1 = Arrays.asList(schDatanode, schNamenode, schAMS);

    // Second host does not contain versionable components
    final List<ServiceComponentHost> schsH2 = Arrays.asList(schAMS);

    // Third host only has hbase
    final List<ServiceComponentHost> schsH3 = Arrays.asList(schHBM);

    ServiceOsSpecific.Package hdfsPackage = new ServiceOsSpecific.Package();
    hdfsPackage.setName("hdfs");

    List<ServiceOsSpecific.Package> packages = Collections.singletonList(hdfsPackage);

    ActionManager actionManager = createNiceMock(ActionManager.class);

    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider csvResourceProvider = createNiceMock(ClusterStackVersionResourceProvider.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Map<String, Map<String, String>> hostConfigTags = new HashMap<>();
    expect(configHelper.getEffectiveDesiredTags(anyObject(ClusterImpl.class), anyObject(String.class))).andReturn(hostConfigTags);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getAuthName()).andReturn("admin").anyTimes();
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(managementController.getJdkResourceUrl()).andReturn("/JdkResourceUrl").anyTimes();
    expect(managementController.getPackagesForServiceHost(anyObject(ServiceInfo.class),
            EasyMock.anyObject(), anyObject(String.class))).
            andReturn(packages).times(1); // only one host has the versionable component

    expect(resourceProviderFactory.getHostResourceProvider(
            eq(managementController))).andReturn(csvResourceProvider).anyTimes();

    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster);
    expect(clusters.getHostsForCluster(anyObject(String.class))).andReturn(
        hostsForCluster).anyTimes();

    String clusterName = "Cluster100";
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(cluster.getHosts()).andReturn(hostsForCluster.values()).atLeastOnce();
    expect(cluster.getServices()).andReturn(serviceMap).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(stackId);
    expect(cluster.getServiceComponentHosts(anyObject(String.class))).andAnswer(new IAnswer<List<ServiceComponentHost>>() {
      @Override
      public List<ServiceComponentHost> answer() throws Throwable {
        String hostname = (String) EasyMock.getCurrentArguments()[0];
        if (hostname.equals("host2")) {
          return schsH2;
        } else if (hostname.equals("host3")) {
          return schsH3;
        } else {
          return schsH1;
        }
      }
    }).anyTimes();

    ExecutionCommand executionCommand = createNiceMock(ExecutionCommand.class);
    ExecutionCommandWrapper executionCommandWrapper = createNiceMock(ExecutionCommandWrapper.class);

    expect(executionCommandWrapper.getExecutionCommand()).andReturn(executionCommand).anyTimes();

    Stage stage = createNiceMock(Stage.class);
    expect(stage.getExecutionCommandWrapper(anyObject(String.class), anyObject(String.class))).
            andReturn(executionCommandWrapper).anyTimes();

    Map<Role, Float> successFactors = new HashMap<>();
    expect(stage.getSuccessFactors()).andReturn(successFactors).atLeastOnce();

    // Check that we create proper stage count
    expect(stageFactory.createNew(anyLong(), anyObject(String.class),
            anyObject(String.class), anyLong(),
            anyObject(String.class), anyObject(String.class),
            anyObject(String.class))).andReturn(stage).
            times((int) Math.ceil(hostCount / MAX_TASKS_PER_STAGE));

    expect(
            repositoryVersionDAOMock.findByStackAndVersion(
                    anyObject(StackId.class),
                    anyObject(String.class))).andReturn(repoVersion);

    Capture<org.apache.ambari.server.actionmanager.Request> c = Capture.newInstance();
    Capture<ExecuteActionRequest> ear = Capture.newInstance();

    actionManager.sendActions(capture(c), capture(ear));
    expectLastCall().atLeastOnce();
    expect(actionManager.getRequestTasks(anyLong())).andReturn(Collections.emptyList()).anyTimes();

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterId(1l);
    clusterEntity.setClusterName(clusterName);

    TopologyManager topologyManager = injector.getInstance(TopologyManager.class);
    StageUtils.setTopologyManager(topologyManager);

    // replay
    replay(managementController, response, clusters, hdfsService, hbaseService, resourceProviderFactory, csvResourceProvider,
            cluster, repositoryVersionDAOMock, configHelper, schDatanode, schNamenode, schAMS, schHBM, actionManager,
            executionCommand, executionCommandWrapper,stage, stageFactory);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    injector.injectMembers(provider);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    Map<String, Object> properties = new LinkedHashMap<>();

    // add properties to the request map
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID, "2.2.0.1-885");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_STACK_PROPERTY_ID, "HDP");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID, "2.1.1");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    RequestStatus status = provider.createResources(request);
    Assert.assertNotNull(status);

    // verify
    verify(managementController, response, clusters, stageFactory, stage);

    // check that the success factor was populated in the stage
    Float successFactor = successFactors.get(Role.INSTALL_PACKAGES);
    Assert.assertEquals(Float.valueOf(0.85f), successFactor);
  }

   @Test
   public void testCreateResourcesWithRepoDefinitionAsAdministrator() throws Exception {
     testCreateResourcesWithRepoDefinition(TestAuthenticationFactory.createAdministrator());
   }

   @Test
   public void testCreateResourcesWithRepoDefinitionAsClusterAdministrator() throws Exception {
     testCreateResourcesWithRepoDefinition(TestAuthenticationFactory.createClusterAdministrator());
   }

   @Test(expected = AuthorizationException.class)
   public void testCreateResourcesWithRepoDefinitionAsClusterOperator() throws Exception {
     testCreateResourcesWithRepoDefinition(TestAuthenticationFactory.createClusterOperator());
   }

   private void testCreateResourcesWithRepoDefinition(Authentication authentication) throws Exception {
    Cluster cluster = createNiceMock(Cluster.class);
    StackId stackId = new StackId("HDP", "2.0.1");

    File f = new File("src/test/resources/hbase_version_test.xml");

    StackEntity stackEntity = new StackEntity();
    stackEntity.setStackName("HDP");
    stackEntity.setStackVersion("2.1.1");

    RepositoryVersionEntity repoVersion = new RepositoryVersionEntity();
    repoVersion.setId(1l);
     repoVersion.addRepoOsEntities(REPO_OS_ENTITIES);
    repoVersion.setVersionXml(IOUtils.toString(new FileInputStream(f)));
    repoVersion.setVersionXsd("version_definition.xsd");
    repoVersion.setType(RepositoryType.STANDARD);
    repoVersion.setStack(stackEntity);

    ambariMetaInfo.getComponent("HDP", "2.1.1", "HBASE", "HBASE_MASTER").setVersionAdvertised(true);

    final String hostWithoutVersionableComponents = "host3";
    List<Host> hostsNeedingInstallCommands = new ArrayList<>();
    Map<String, Host> hostsForCluster = new HashMap<>();
    int hostCount = 10;
    for (int i = 0; i < hostCount; i++) {
      String hostname = "host" + i;
      Host host = createNiceMock(hostname, Host.class);
      expect(host.getHostName()).andReturn(hostname).anyTimes();
      expect(host.getOsFamily()).andReturn("redhat6").anyTimes();
      expect(host.getMaintenanceState(EasyMock.anyLong())).andReturn(
          MaintenanceState.OFF).anyTimes();
      expect(host.getAllHostVersions()).andReturn(
          Collections.emptyList()).anyTimes();

      replay(host);
      hostsForCluster.put(hostname, host);


      if (!StringUtils.equals(hostWithoutVersionableComponents, hostname)) {
        hostsNeedingInstallCommands.add(host);
      }
    }

    Service hdfsService = createNiceMock(Service.class);
    Service hbaseService = createNiceMock(Service.class);
    expect(hdfsService.getName()).andReturn("HDFS").anyTimes();
    expect(hbaseService.getName()).andReturn("HBASE").anyTimes();

    expect(hdfsService.getServiceComponents()).andReturn(new HashMap<>());
    expect(hbaseService.getServiceComponents()).andReturn(new HashMap<>());

    Map<String, Service> serviceMap = new HashMap<>();
    serviceMap.put("HDFS", hdfsService);
    serviceMap.put("HBASE", hbaseService);

    final ServiceComponentHost schDatanode = createMock(ServiceComponentHost.class);
    expect(schDatanode.getServiceName()).andReturn("HDFS").anyTimes();
    expect(schDatanode.getServiceComponentName()).andReturn("DATANODE").anyTimes();

    final ServiceComponentHost schNamenode = createMock(ServiceComponentHost.class);
    expect(schNamenode.getServiceName()).andReturn("HDFS").anyTimes();
    expect(schNamenode.getServiceComponentName()).andReturn("NAMENODE").anyTimes();

    final ServiceComponentHost schHBM = createMock(ServiceComponentHost.class);
    expect(schHBM.getServiceName()).andReturn("HBASE").anyTimes();
    expect(schHBM.getServiceComponentName()).andReturn("HBASE_MASTER").anyTimes();

    // First host contains versionable components
    final List<ServiceComponentHost> schsH1 = Arrays.asList(schDatanode, schNamenode);

    // Second host contains versionable components
    final List<ServiceComponentHost> schsH2 = Arrays.asList(schDatanode);

    // Third host only has hbase
    final List<ServiceComponentHost> schsH3 = Arrays.asList(schHBM);

    ServiceOsSpecific.Package hdfsPackage = new ServiceOsSpecific.Package();
    hdfsPackage.setName("hdfs");

    List<ServiceOsSpecific.Package> packages = Collections.singletonList(hdfsPackage);

    ActionManager actionManager = createNiceMock(ActionManager.class);

    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider csvResourceProvider = createNiceMock(ResourceProvider.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Map<String, Map<String, String>> hostConfigTags = new HashMap<>();
    expect(configHelper.getEffectiveDesiredTags(anyObject(ClusterImpl.class), anyObject(String.class))).andReturn(hostConfigTags);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getAuthName()).andReturn("admin").anyTimes();
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(managementController.getJdkResourceUrl()).andReturn("/JdkResourceUrl").anyTimes();
    expect(managementController.getPackagesForServiceHost(anyObject(ServiceInfo.class),
            EasyMock.anyObject(), anyObject(String.class))).
            andReturn(packages).anyTimes(); // only one host has the versionable component

    expect(resourceProviderFactory.getHostResourceProvider(
            eq(managementController))).andReturn(csvResourceProvider).anyTimes();

    expect(managementController.findConfigurationTagsWithOverrides(anyObject(Cluster.class), EasyMock.anyString()))
      .andReturn(new HashMap<String, Map<String, String>>()).anyTimes();


    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster);
    expect(clusters.getHostsForCluster(anyObject(String.class))).andReturn(
        hostsForCluster).anyTimes();

    String clusterName = "Cluster100";
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(cluster.getHosts()).andReturn(hostsForCluster.values()).atLeastOnce();
    expect(cluster.getServices()).andReturn(serviceMap).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(stackId);
    expect(cluster.getServiceComponentHosts(anyObject(String.class))).andAnswer(new IAnswer<List<ServiceComponentHost>>() {
      @Override
      public List<ServiceComponentHost> answer() throws Throwable {
        String hostname = (String) EasyMock.getCurrentArguments()[0];
            if (hostname.equals("host2")) {
          return schsH2;
        } else if (hostname.equals("host3")) {
          return schsH3;
        } else {
          return schsH1;
        }
      }
    }).anyTimes();

    expect(cluster.transitionHostsToInstalling(anyObject(RepositoryVersionEntity.class),
        anyObject(VersionDefinitionXml.class), eq(false))).andReturn(hostsNeedingInstallCommands).atLeastOnce();

//    ExecutionCommand executionCommand = createNiceMock(ExecutionCommand.class);
    ExecutionCommand executionCommand = new ExecutionCommand();
    ExecutionCommandWrapper executionCommandWrapper = createNiceMock(ExecutionCommandWrapper.class);

//    expect(executionCommand.getHostLevelParams()).andReturn(new HashMap<String, String>()).atLeastOnce();
    expect(executionCommandWrapper.getExecutionCommand()).andReturn(executionCommand).anyTimes();

    Stage stage = createNiceMock(Stage.class);
    expect(stage.getExecutionCommandWrapper(anyObject(String.class), anyObject(String.class))).
            andReturn(executionCommandWrapper).anyTimes();

    Map<Role, Float> successFactors = new HashMap<>();
    expect(stage.getSuccessFactors()).andReturn(successFactors).atLeastOnce();

    // Check that we create proper stage count
    expect(stageFactory.createNew(anyLong(), anyObject(String.class),
            anyObject(String.class), anyLong(),
            anyObject(String.class), anyObject(String.class),
            anyObject(String.class))).andReturn(stage).
            times((int) Math.ceil(hostCount / MAX_TASKS_PER_STAGE));

    expect(
            repositoryVersionDAOMock.findByStackAndVersion(
                    anyObject(StackId.class),
                    anyObject(String.class))).andReturn(repoVersion);

    Capture<org.apache.ambari.server.actionmanager.Request> c = Capture.newInstance();
    Capture<ExecuteActionRequest> ear = Capture.newInstance();

    actionManager.sendActions(capture(c), capture(ear));
    expectLastCall().atLeastOnce();
    expect(actionManager.getRequestTasks(anyLong())).andReturn(Collections.emptyList()).anyTimes();

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterId(1l);
    clusterEntity.setClusterName(clusterName);

    StageUtils.setTopologyManager(injector.getInstance(TopologyManager.class));
    StageUtils.setConfiguration(injector.getInstance(Configuration.class));

    // replay
    replay(managementController, response, clusters, hdfsService, hbaseService, resourceProviderFactory, csvResourceProvider,
            cluster, repositoryVersionDAOMock, configHelper, schDatanode, schNamenode, schHBM, actionManager,
            executionCommandWrapper,stage, stageFactory);

    ResourceProvider provider = createProvider(managementController);
    injector.injectMembers(provider);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    Map<String, Object> properties = new LinkedHashMap<>();

    // add properties to the request map
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID, "2.2.0.1-885");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_STACK_PROPERTY_ID, "HDP");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID, "2.1.1");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    RequestStatus status = provider.createResources(request);
    Assert.assertNotNull(status);

    // verify
    verify(managementController, response, clusters, stageFactory, stage);

    // check that the success factor was populated in the stage
    Float successFactor = successFactors.get(Role.INSTALL_PACKAGES);
    Assert.assertEquals(Float.valueOf(0.85f), successFactor);

    Assert.assertTrue(executionCommand.getRoleParams().containsKey(KeyNames.PACKAGE_LIST));
    Assert.assertTrue(executionCommand.getRoleParams().containsKey("stack_id"));
  }

   @Test
   public void testCreateResourcesWithNonManagedOSAsAdministrator() throws Exception {
     testCreateResourcesWithNonManagedOS(TestAuthenticationFactory.createAdministrator());
   }

   @Test
   public void testCreateResourcesWithNonManagedOSAsClusterAdministrator() throws Exception {
     testCreateResourcesWithNonManagedOS(TestAuthenticationFactory.createClusterAdministrator());
   }

   @Test(expected = AuthorizationException.class)
   public void testCreateResourcesWithNonManagedOSAsClusterOperator() throws Exception {
     testCreateResourcesWithNonManagedOS(TestAuthenticationFactory.createClusterOperator());
   }

   private void testCreateResourcesWithNonManagedOS(Authentication authentication) throws Exception {

    Cluster cluster = createNiceMock(Cluster.class);
    StackId stackId = new StackId("HDP", "2.0.1");

    File f = new File("src/test/resources/hbase_version_test.xml");

    StackEntity stackEntity = new StackEntity();
    stackEntity.setStackName("HDP");
    stackEntity.setStackVersion("2.1.1");

    RepositoryVersionEntity repoVersion = new RepositoryVersionEntity();
    repoVersion.setId(1l);
     repoVersion.addRepoOsEntities(REPO_OS_NOT_MANAGED);
    repoVersion.setVersionXml(IOUtils.toString(new FileInputStream(f)));
    repoVersion.setVersionXsd("version_definition.xsd");
    repoVersion.setType(RepositoryType.STANDARD);
    repoVersion.setStack(stackEntity);

    ambariMetaInfo.getComponent("HDP", "2.1.1", "HBASE", "HBASE_MASTER").setVersionAdvertised(true);

    final String hostWithoutVersionableComponents = "host3";
    List<Host> hostsNeedingInstallCommands = new ArrayList<>();

    Map<String, Host> hostsForCluster = new HashMap<>();
    int hostCount = 10;
    for (int i = 0; i < hostCount; i++) {
      String hostname = "host" + i;
      Host host = createNiceMock(hostname, Host.class);
      expect(host.getHostName()).andReturn(hostname).anyTimes();
      expect(host.getOsFamily()).andReturn("redhat6").anyTimes();
      expect(host.getMaintenanceState(EasyMock.anyLong())).andReturn(
          MaintenanceState.OFF).anyTimes();
      expect(host.getAllHostVersions()).andReturn(
          Collections.emptyList()).anyTimes();

      replay(host);
      hostsForCluster.put(hostname, host);

      if (!StringUtils.equals(hostWithoutVersionableComponents, hostname)) {
        hostsNeedingInstallCommands.add(host);
      }
    }

    Service hdfsService = createNiceMock(Service.class);
    Service hbaseService = createNiceMock(Service.class);
    expect(hdfsService.getName()).andReturn("HDFS").anyTimes();
    expect(hbaseService.getName()).andReturn("HBASE").anyTimes();

    expect(hdfsService.getServiceComponents()).andReturn(new HashMap<>());
    expect(hbaseService.getServiceComponents()).andReturn(new HashMap<>());

    Map<String, Service> serviceMap = new HashMap<>();
    serviceMap.put("HDFS", hdfsService);
    serviceMap.put("HBASE", hbaseService);

    final ServiceComponentHost schDatanode = createMock(ServiceComponentHost.class);
    expect(schDatanode.getServiceName()).andReturn("HDFS").anyTimes();
    expect(schDatanode.getServiceComponentName()).andReturn("DATANODE").anyTimes();

    final ServiceComponentHost schNamenode = createMock(ServiceComponentHost.class);
    expect(schNamenode.getServiceName()).andReturn("HDFS").anyTimes();
    expect(schNamenode.getServiceComponentName()).andReturn("NAMENODE").anyTimes();

    final ServiceComponentHost schHBM = createMock(ServiceComponentHost.class);
    expect(schHBM.getServiceName()).andReturn("HBASE").anyTimes();
    expect(schHBM.getServiceComponentName()).andReturn("HBASE_MASTER").anyTimes();

    // First host contains versionable components
    final List<ServiceComponentHost> schsH1 = Arrays.asList(schDatanode, schNamenode);

    // Second host contains versionable components
    final List<ServiceComponentHost> schsH2 = Arrays.asList(schDatanode);

    // Third host only has hbase
    final List<ServiceComponentHost> schsH3 = Arrays.asList(schHBM);

    ServiceOsSpecific.Package hdfsPackage = new ServiceOsSpecific.Package();
    hdfsPackage.setName("hdfs");

    List<ServiceOsSpecific.Package> packages = Collections.singletonList(hdfsPackage);

    ActionManager actionManager = createNiceMock(ActionManager.class);

    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProvider csvResourceProvider = createNiceMock(ResourceProvider.class);

    Map<String, Map<String, String>> hostConfigTags = new HashMap<>();
    expect(configHelper.getEffectiveDesiredTags(anyObject(ClusterImpl.class), anyObject(String.class))).andReturn(hostConfigTags);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getAuthName()).andReturn("admin").anyTimes();
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(managementController.getJdkResourceUrl()).andReturn("/JdkResourceUrl").anyTimes();
    expect(managementController.getPackagesForServiceHost(anyObject(ServiceInfo.class),
            EasyMock.anyObject(), anyObject(String.class))).
            andReturn(packages).anyTimes(); // only one host has the versionable component

    expect(managementController.findConfigurationTagsWithOverrides(anyObject(Cluster.class), EasyMock.anyString()))
    .andReturn(new HashMap<String, Map<String, String>>()).anyTimes();

    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster);
    expect(clusters.getHostsForCluster(anyObject(String.class))).andReturn(
        hostsForCluster).anyTimes();

    String clusterName = "Cluster100";
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(cluster.getHosts()).andReturn(hostsForCluster.values()).atLeastOnce();
    expect(cluster.getServices()).andReturn(serviceMap).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(stackId);
    expect(cluster.getServiceComponentHosts(anyObject(String.class))).andAnswer(new IAnswer<List<ServiceComponentHost>>() {
      @Override
      public List<ServiceComponentHost> answer() throws Throwable {
        String hostname = (String) EasyMock.getCurrentArguments()[0];
        if (hostname.equals("host2")) {
          return schsH2;
        } else if (hostname.equals("host3")) {
          return schsH3;
        } else {
          return schsH1;
        }
      }
    }).anyTimes();

    expect(cluster.transitionHostsToInstalling(anyObject(RepositoryVersionEntity.class),
        anyObject(VersionDefinitionXml.class), eq(false))).andReturn(hostsNeedingInstallCommands).atLeastOnce();

    ExecutionCommand executionCommand = new ExecutionCommand();
    ExecutionCommandWrapper executionCommandWrapper = createNiceMock(ExecutionCommandWrapper.class);

    expect(executionCommandWrapper.getExecutionCommand()).andReturn(executionCommand).anyTimes();

    Stage stage = createNiceMock(Stage.class);
    expect(stage.getExecutionCommandWrapper(anyObject(String.class), anyObject(String.class))).
            andReturn(executionCommandWrapper).anyTimes();

    Map<Role, Float> successFactors = new HashMap<>();
    expect(stage.getSuccessFactors()).andReturn(successFactors).atLeastOnce();

    // Check that we create proper stage count
    expect(stageFactory.createNew(anyLong(), anyObject(String.class),
            anyObject(String.class), anyLong(),
            anyObject(String.class), anyObject(String.class),
            anyObject(String.class))).andReturn(stage).
            times((int) Math.ceil(hostCount / MAX_TASKS_PER_STAGE));

    expect(repositoryVersionDAOMock.findByStackAndVersion(anyObject(StackId.class),
        anyObject(String.class))).andReturn(repoVersion);

    Capture<org.apache.ambari.server.actionmanager.Request> c = Capture.newInstance();
    Capture<ExecuteActionRequest> ear = Capture.newInstance();

    actionManager.sendActions(capture(c), capture(ear));
    expectLastCall().atLeastOnce();
    expect(actionManager.getRequestTasks(anyLong())).andReturn(Collections.emptyList()).anyTimes();

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterId(1l);
    clusterEntity.setClusterName(clusterName);

    StageUtils.setTopologyManager(injector.getInstance(TopologyManager.class));
    StageUtils.setConfiguration(injector.getInstance(Configuration.class));

    // replay
    replay(managementController, response, clusters, hdfsService, hbaseService, csvResourceProvider,
            cluster, repositoryVersionDAOMock, configHelper, schDatanode, schNamenode, schHBM, actionManager,
            executionCommandWrapper,stage, stageFactory);

    ResourceProvider provider = createProvider(managementController);
    injector.injectMembers(provider);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    Map<String, Object> properties = new LinkedHashMap<>();

    // add properties to the request map
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID, "2.2.0.1-885");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_STACK_PROPERTY_ID, "HDP");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID, "2.1.1");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    RequestStatus status = provider.createResources(request);
    Assert.assertNotNull(status);

    // verify
    verify(managementController, response, clusters, stageFactory, stage);

    // check that the success factor was populated in the stage
    Float successFactor = successFactors.get(Role.INSTALL_PACKAGES);
    Assert.assertEquals(Float.valueOf(0.85f), successFactor);

    Assert.assertNotNull(executionCommand.getRepositoryFile());
    Assert.assertEquals(2, executionCommand.getRepositoryFile().getRepositories().size());

    for (CommandRepository.Repository repo : executionCommand.getRepositoryFile().getRepositories()) {
      Assert.assertFalse(repo.isAmbariManaged());
    }

  }

   @Test
   public void testCreateResourcesMixedAsAdministrator() throws Exception {
     testCreateResourcesMixed(TestAuthenticationFactory.createAdministrator());
   }

   @Test
   public void testCreateResourcesMixedAsClusterAdministrator() throws Exception {
     testCreateResourcesMixed(TestAuthenticationFactory.createClusterAdministrator());
   }

   @Test(expected = AuthorizationException.class)
   public void testCreateResourcesMixedAsClusterOperator() throws Exception {
     testCreateResourcesMixed(TestAuthenticationFactory.createClusterOperator());
   }

   private void testCreateResourcesMixed(Authentication authentication) throws Exception {
    Cluster cluster = createNiceMock(Cluster.class);
    Map<String, String> hostLevelParams = new HashMap<>();
    StackId stackId = new StackId("HDP", "2.0.1");

    File f = new File("src/test/resources/hbase_version_test.xml");
    String xml = IOUtils.toString(new FileInputStream(f));
    // munge it
    xml = xml.replace("<package-version>2_3_4_0_3396</package-version>", "");

    StackEntity stack = new StackEntity();
    stack.setStackName("HDP");

    RepositoryVersionEntity repoVersion = new RepositoryVersionEntity();
    repoVersion.setStack(stack);
    repoVersion.setId(1l);
     repoVersion.addRepoOsEntities(REPO_OS_ENTITIES);
    repoVersion.setVersionXml(xml);
    repoVersion.setVersionXsd("version_definition.xsd");
    repoVersion.setType(RepositoryType.STANDARD);


    Map<String, Host> hostsForCluster = new HashMap<>();
    int hostCount = 10;
    for (int i = 0; i < hostCount; i++) {
      String hostname = "host" + i;

      List<HostVersionEntity> hostVersions = new ArrayList<>();

      HostVersionEntity hostVersion = createNiceMock(HostVersionEntity.class);
      expect(hostVersion.getRepositoryVersion()).andReturn(repoVersion);

      hostVersions.add(hostVersion);
      if (i == 2) {
        // !!! make it look like there is already a versioned installed that is less than the one being installed
        RepositoryVersionEntity badRve = new RepositoryVersionEntity();
        badRve.setStack(stack);
        badRve.setVersion("2.2.1.0-1000");

        HostVersionEntity badHostVersion = createNiceMock(HostVersionEntity.class);
        expect(badHostVersion.getRepositoryVersion()).andReturn(badRve);
        hostVersions.add(badHostVersion);
        replay(badHostVersion);
      }
      Host host = createNiceMock(hostname, Host.class);
      expect(host.getHostName()).andReturn(hostname).anyTimes();
      expect(host.getOsFamily()).andReturn("redhat6").anyTimes();
      expect(host.getMaintenanceState(EasyMock.anyLong())).andReturn(
          MaintenanceState.OFF).anyTimes();
      expect(host.getAllHostVersions()).andReturn(hostVersions).anyTimes();

      replay(host, hostVersion);
      hostsForCluster.put(hostname, host);
    }

    final ServiceComponentHost schDatanode = createMock(ServiceComponentHost.class);
    expect(schDatanode.getServiceName()).andReturn("HDFS").anyTimes();
    expect(schDatanode.getServiceComponentName()).andReturn("DATANODE").anyTimes();
    final ServiceComponentHost schNamenode = createMock(ServiceComponentHost.class);
    expect(schNamenode.getServiceName()).andReturn("HDFS").anyTimes();
    expect(schNamenode.getServiceComponentName()).andReturn("NAMENODE").anyTimes();
    final ServiceComponentHost schAMS = createMock(ServiceComponentHost.class);
    expect(schAMS.getServiceName()).andReturn("AMBARI_METRICS").anyTimes();
    expect(schAMS.getServiceComponentName()).andReturn("METRICS_COLLECTOR").anyTimes();
    // First host contains versionable components
    final List<ServiceComponentHost> schsH1 = Lists.newArrayList(schDatanode, schNamenode, schAMS);
    // Second host does not contain versionable components
    final List<ServiceComponentHost> schsH2 = Lists.newArrayList(schAMS);

    ServiceOsSpecific.Package hdfsPackage = new ServiceOsSpecific.Package();
    hdfsPackage.setName("hdfs");
    List<ServiceOsSpecific.Package> packages = Collections.singletonList(hdfsPackage);

    ActionManager actionManager = createNiceMock(ActionManager.class);

    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    Map<String, Map<String, String>> hostConfigTags = new HashMap<>();
    expect(configHelper.getEffectiveDesiredTags(anyObject(ClusterImpl.class), anyObject(String.class))).andReturn(hostConfigTags);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getAuthName()).andReturn("admin").anyTimes();
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(managementController.getJdkResourceUrl()).andReturn("/JdkResourceUrl").anyTimes();
    expect(managementController.getPackagesForServiceHost(anyObject(ServiceInfo.class),
            EasyMock.<Map<String, String>>anyObject(), anyObject(String.class))).
            andReturn(packages).anyTimes();

    expect(managementController.findConfigurationTagsWithOverrides(anyObject(Cluster.class), EasyMock.anyString()))
      .andReturn(new HashMap<String, Map<String, String>>()).anyTimes();

    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster);
    expect(clusters.getHostsForCluster(anyObject(String.class))).andReturn(
        hostsForCluster).anyTimes();

    String clusterName = "Cluster100";
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(cluster.getHosts()).andReturn(hostsForCluster.values()).atLeastOnce();
    expect(cluster.getServices()).andReturn(new HashMap<>()).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(stackId);
    expect(cluster.getServiceComponentHosts(anyObject(String.class))).andAnswer(new IAnswer<List<ServiceComponentHost>>() {
      @Override
      public List<ServiceComponentHost> answer() throws Throwable {
        String hostname = (String) EasyMock.getCurrentArguments()[0];
        if (hostname.equals("host2")) {
          return schsH2;
        } else {
          return schsH1;
        }
      }
    }).anyTimes();

    // now the important expectations - that the cluster transition methods were
    // called correctly
    expect(cluster.transitionHostsToInstalling(anyObject(RepositoryVersionEntity.class),
        anyObject(VersionDefinitionXml.class), eq(false))).andReturn(Collections.emptyList()).anyTimes();


    ExecutionCommand executionCommand = createNiceMock(ExecutionCommand.class);
    ExecutionCommandWrapper executionCommandWrapper = createNiceMock(ExecutionCommandWrapper.class);

    expect(executionCommandWrapper.getExecutionCommand()).andReturn(executionCommand).anyTimes();

    Stage stage = createNiceMock(Stage.class);
    expect(stage.getExecutionCommandWrapper(anyObject(String.class), anyObject(String.class))).
            andReturn(executionCommandWrapper).anyTimes();

    expect(executionCommand.getHostLevelParams()).andReturn(hostLevelParams).anyTimes();

    Map<Role, Float> successFactors = new HashMap<>();
    expect(stage.getSuccessFactors()).andReturn(successFactors).atLeastOnce();

    // Check that we create proper stage count
    expect(stageFactory.createNew(anyLong(), anyObject(String.class),
        anyObject(String.class), anyLong(),
        anyObject(String.class), anyObject(String.class),
        anyObject(String.class))).andReturn(stage).
        times((int) Math.ceil(hostCount / MAX_TASKS_PER_STAGE));

    expect(repositoryVersionDAOMock.findByStackAndVersion(anyObject(StackId.class),
        anyObject(String.class))).andReturn(repoVersion);

    expect(actionManager.getRequestTasks(anyLong())).andReturn(Collections.emptyList()).anyTimes();

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterId(1l);
    clusterEntity.setClusterName(clusterName);

    TopologyManager topologyManager = injector.getInstance(TopologyManager.class);
    StageUtils.setTopologyManager(topologyManager);
    StageUtils.setConfiguration(injector.getInstance(Configuration.class));



    // replay
    replay(managementController, response, clusters,
            cluster, repositoryVersionDAOMock, configHelper, schDatanode, schNamenode, schAMS, actionManager,
            executionCommand, executionCommandWrapper,stage, stageFactory);

    ResourceProvider provider = createProvider(managementController);
    injector.injectMembers(provider);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    Map<String, Object> properties = new LinkedHashMap<>();

    // add properties to the request map
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID, "2.2.0.1-885");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_STACK_PROPERTY_ID, "HDP");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID, "2.1.1");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    try {
      provider.createResources(request);
      Assert.fail("Expecting the create to fail due to an already installed version");
    } catch (IllegalArgumentException iae) {
      // !!! expected
    }

  }

   @Test
   public void testCreateResourcesExistingUpgradeAsAdministrator() throws Exception {
     testCreateResourcesExistingUpgrade(TestAuthenticationFactory.createAdministrator());
   }

   @Test
   public void testCreateResourcesExistingUpgradeAsClusterAdministrator() throws Exception {
     testCreateResourcesExistingUpgrade(TestAuthenticationFactory.createClusterAdministrator());
   }

   @Test(expected = AuthorizationException.class)
   public void testCreateResourcesExistingUpgradeAsClusterOperator() throws Exception {
     testCreateResourcesExistingUpgrade(TestAuthenticationFactory.createClusterOperator());
   }

  /**
   * Tests that forcing the host versions into
   * {@link RepositoryVersionState#INSTALLED}
   *
   * @throws Exception
   */
  @Test
  public void testCreateResourcesInInstalledState() throws Exception {
    Cluster cluster = createNiceMock(Cluster.class);
    StackId stackId = new StackId("HDP", "2.2.0");
    String repoVersion = "2.2.0.1-885";

    File f = new File("src/test/resources/hbase_version_test.xml");

    StackEntity stackEntity = new StackEntity();
    stackEntity.setStackName(stackId.getStackName());
    stackEntity.setStackVersion(stackId.getStackVersion());

    RepositoryVersionEntity repoVersionEntity = new RepositoryVersionEntity();
    repoVersionEntity.setId(1l);
    repoVersionEntity.addRepoOsEntities(REPO_OS_ENTITIES);
    repoVersionEntity.setVersionXml(IOUtils.toString(new FileInputStream(f)));
    repoVersionEntity.setVersionXsd("version_definition.xsd");
    repoVersionEntity.setType(RepositoryType.STANDARD);
    repoVersionEntity.setVersion(repoVersion);
    repoVersionEntity.setStack(stackEntity);

    List<Host> hostsNeedingInstallCommands = new ArrayList<>();
    Map<String, Host> hostsForCluster = new HashMap<>();
    List<HostVersionEntity> hostVersionEntitiesMergedWithNotRequired = new ArrayList<>();
    int hostCount = 10;

    for (int i = 0; i < hostCount; i++) {
      String hostname = "host" + i;
      Host host = createNiceMock(hostname, Host.class);
      expect(host.getHostName()).andReturn(hostname).anyTimes();
      expect(host.getOsFamily()).andReturn("redhat6").anyTimes();
      expect(host.getMaintenanceState(EasyMock.anyLong())).andReturn(MaintenanceState.OFF).anyTimes();

      // ensure that 2 hosts don't have versionable components so they
      // transition correct into the not required state
      if (i < hostCount - 2) {
        expect(host.hasComponentsAdvertisingVersions(eq(stackId))).andReturn(true).atLeastOnce();
        hostsNeedingInstallCommands.add(host);
        expect(host.getAllHostVersions()).andReturn(Collections.emptyList()).anyTimes();
      } else {
        expect(host.hasComponentsAdvertisingVersions(eq(stackId))).andReturn(false).atLeastOnce();

        // mock out the host versions so that we can test hosts being
        // transitioned into NOT_REQUIRED
        HostVersionEntity hostVersionEntity = EasyMock.createNiceMock(HostVersionEntity.class);
        expect(hostVersionEntity.getRepositoryVersion()).andReturn(repoVersionEntity).atLeastOnce();
        replay(hostVersionEntity);

        hostVersionEntitiesMergedWithNotRequired.add(hostVersionEntity);
        expect(host.getAllHostVersions()).andReturn(hostVersionEntitiesMergedWithNotRequired).anyTimes();
      }

      replay(host);

      hostsForCluster.put(hostname, host);
    }

    Service hdfsService = createNiceMock(Service.class);
    expect(hdfsService.getName()).andReturn("HDFS").anyTimes();
    expect(hdfsService.getServiceComponents()).andReturn(new HashMap<>());

    Map<String, Service> serviceMap = new HashMap<>();
    serviceMap.put("HDFS", hdfsService);

    final ServiceComponentHost schDatanode = createMock(ServiceComponentHost.class);
    expect(schDatanode.getServiceName()).andReturn("HDFS").anyTimes();
    expect(schDatanode.getServiceComponentName()).andReturn("DATANODE").anyTimes();

    final List<ServiceComponentHost> serviceComponentHosts = Arrays.asList(schDatanode);

    ServiceOsSpecific.Package hdfsPackage = new ServiceOsSpecific.Package();
    hdfsPackage.setName("hdfs");

    List<ServiceOsSpecific.Package> packages = Collections.singletonList(hdfsPackage);

    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider csvResourceProvider = createNiceMock(ResourceProvider.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getAuthName()).andReturn("admin").anyTimes();
    expect(managementController.getJdkResourceUrl()).andReturn("/JdkResourceUrl").anyTimes();
    expect(managementController.getPackagesForServiceHost(anyObject(ServiceInfo.class),
        EasyMock.anyObject(), anyObject(String.class))).andReturn(
            packages).anyTimes(); // only one host has the versionable component

    expect(resourceProviderFactory.getHostResourceProvider(eq(managementController))).andReturn(csvResourceProvider).anyTimes();

    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster);
    expect(clusters.getHostsForCluster(anyObject(String.class))).andReturn(
        hostsForCluster).anyTimes();

    String clusterName = "Cluster100";
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(cluster.getHosts()).andReturn(hostsForCluster.values()).atLeastOnce();
    expect(cluster.getServices()).andReturn(serviceMap).anyTimes();
    expect(cluster.getServiceComponentHosts(anyObject(String.class))).andReturn(serviceComponentHosts).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(stackId).atLeastOnce();

    expect(repositoryVersionDAOMock.findByStackAndVersion(anyObject(StackId.class),
        anyObject(String.class))).andReturn(repoVersionEntity);

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterId(1l);
    clusterEntity.setClusterName(clusterName);


    // now the important expectations - that the cluster transition methods were
    // called correctly
    expect(cluster.transitionHostsToInstalling(repoVersionEntity,
        repoVersionEntity.getRepositoryXml(), true)).andReturn(
            hostsNeedingInstallCommands).once();

    // replay
    replay(managementController, response, clusters, hdfsService, resourceProviderFactory,
        csvResourceProvider, cluster, repositoryVersionDAOMock, configHelper, schDatanode,
        stageFactory, hostVersionDAO);

    ResourceProvider provider = createProvider(managementController);
    injector.injectMembers(provider);

    // add the property map to a set for the request. add more maps for multiple
    // creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    Map<String, Object> properties = new LinkedHashMap<>();

    // add properties to the request map
    properties.put(
        ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID,
        clusterName);

    properties.put(
        ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID,
        repoVersion);

    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_STACK_PROPERTY_ID,
        stackId.getStackName());

    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID,
        stackId.getStackVersion());

    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_FORCE, "true");

    propertySet.add(properties);

    // set the security auth
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createAdministrator());

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    RequestStatus status = provider.createResources(request);
    Assert.assertNotNull(status);

    // verify
    verify(managementController, response, clusters, cluster, hostVersionDAO);
   }

  @Test
  public void testCreateResourcesPPC() throws Exception {
    Cluster cluster = createNiceMock(Cluster.class);
    Map<String, String> hostLevelParams = new HashMap<>();
    StackId stackId = new StackId("HDP", "2.0.1");

    RepositoryVersionHelper rvh = new RepositoryVersionHelper();

    RepositoryVersionEntity repoVersion = createNiceMock(RepositoryVersionEntity.class);
    expect(repoVersion.getId()).andReturn(1L).anyTimes();
    expect(repoVersion.getStackId()).andReturn(new StackId("HDP-2.1.1")).anyTimes();
    expect(repoVersion.isLegacy()).andReturn(false).anyTimes();
    expect(repoVersion.getStackName()).andReturn("HDP").anyTimes();


    List<RepoOsEntity> oss = new ArrayList<>();
    RepoDefinitionEntity repoDefinitionEntity1 = new RepoDefinitionEntity();
    repoDefinitionEntity1.setRepoID("HDP-UTILS-1.1.0.20");
    repoDefinitionEntity1.setBaseUrl("http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos-ppc7/2.x/updates/2.2.0.0");
    repoDefinitionEntity1.setRepoName("HDP-UTILS");
    RepoDefinitionEntity repoDefinitionEntity2 = new RepoDefinitionEntity();
    repoDefinitionEntity2.setRepoID("HDP-2.2");
    repoDefinitionEntity2.setBaseUrl("http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos-ppc7/2.x/updates/2.2.0.0");
    repoDefinitionEntity2.setRepoName("HDP");
    RepoOsEntity repoOsEntity = new RepoOsEntity();
    repoOsEntity.setFamily("redhat-ppc7");
    repoOsEntity.setAmbariManaged(true);
    repoOsEntity.addRepoDefinition(repoDefinitionEntity1);
    repoOsEntity.addRepoDefinition(repoDefinitionEntity2);
    oss.add(repoOsEntity);
    expect(repoVersion.getRepoOsEntities()).andReturn(oss).anyTimes();
    expect(repoVersion.getType()).andReturn(RepositoryType.STANDARD).anyTimes();

    Map<String, Host> hostsForCluster = new HashMap<>();
    int hostCount = 2;
    for (int i = 0; i < hostCount; i++) {
      String hostname = "host" + i;
      Host host = createNiceMock(hostname, Host.class);
      expect(host.getHostName()).andReturn(hostname).anyTimes();
      expect(host.getOsFamily()).andReturn("redhat-ppc7").anyTimes();
      expect(host.getMaintenanceState(EasyMock.anyLong())).andReturn(
          MaintenanceState.OFF).anyTimes();
      expect(host.getAllHostVersions()).andReturn(
          Collections.<HostVersionEntity>emptyList()).anyTimes();
      expect(host.getHostAttributes()).andReturn(
          ImmutableMap.<String, String>builder()
            .put("os_family", "redhat-ppc")
            .put("os_release_version", "7.2")
            .build()
          ).anyTimes();
      replay(host);
      hostsForCluster.put(hostname, host);
    }

    final ServiceComponentHost schDatanode = createMock(ServiceComponentHost.class);
    expect(schDatanode.getServiceName()).andReturn("HDFS").anyTimes();
    expect(schDatanode.getServiceComponentName()).andReturn("DATANODE").anyTimes();
    final ServiceComponentHost schNamenode = createMock(ServiceComponentHost.class);
    expect(schNamenode.getServiceName()).andReturn("HDFS").anyTimes();
    expect(schNamenode.getServiceComponentName()).andReturn("NAMENODE").anyTimes();
    final ServiceComponentHost schAMS = createMock(ServiceComponentHost.class);
    expect(schAMS.getServiceName()).andReturn("AMBARI_METRICS").anyTimes();
    expect(schAMS.getServiceComponentName()).andReturn("METRICS_COLLECTOR").anyTimes();
    // First host contains versionable components
    final List<ServiceComponentHost> schsH1 = Lists.newArrayList(schDatanode, schNamenode, schAMS);
    // Second host does not contain versionable components
    final List<ServiceComponentHost> schsH2 = Lists.newArrayList(schAMS);

    ServiceOsSpecific.Package hdfsPackage = new ServiceOsSpecific.Package();
    hdfsPackage.setName("hdfs");
    List<ServiceOsSpecific.Package> packages = Collections.singletonList(hdfsPackage);

    ActionManager actionManager = createNiceMock(ActionManager.class);

    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider csvResourceProvider = createNiceMock(ClusterStackVersionResourceProvider.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Map<String, Map<String, String>> hostConfigTags = new HashMap<>();
    expect(configHelper.getEffectiveDesiredTags(anyObject(ClusterImpl.class), anyObject(String.class))).andReturn(hostConfigTags);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getAuthName()).andReturn("admin").anyTimes();
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(managementController.getJdkResourceUrl()).andReturn("/JdkResourceUrl").anyTimes();
    expect(managementController.getPackagesForServiceHost(anyObject(ServiceInfo.class),
            (Map<String, String>) anyObject(List.class), anyObject(String.class))).
            andReturn(packages).anyTimes();

    expect(managementController.findConfigurationTagsWithOverrides(anyObject(Cluster.class), EasyMock.anyString()))
      .andReturn(new HashMap<String, Map<String, String>>()).anyTimes();


    expect(resourceProviderFactory.getHostResourceProvider(
            eq(managementController))).andReturn(csvResourceProvider).anyTimes();

    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster);
    expect(clusters.getHostsForCluster(anyObject(String.class))).andReturn(
        hostsForCluster).anyTimes();

    String clusterName = "Cluster100";
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(cluster.getHosts()).andReturn(hostsForCluster.values()).atLeastOnce();
    expect(cluster.getServices()).andReturn(new HashMap<String, Service>()).anyTimes();
    expect(cluster.getCurrentStackVersion()).andReturn(stackId);
    expect(cluster.getServiceComponentHosts(anyObject(String.class))).andAnswer(new IAnswer<List<ServiceComponentHost>>() {
      @Override
      public List<ServiceComponentHost> answer() throws Throwable {
        String hostname = (String) EasyMock.getCurrentArguments()[0];
        if (hostname.equals("host2")) {
          return schsH2;
        } else {
          return schsH1;
        }
      }
    }).anyTimes();
    expect(cluster.transitionHostsToInstalling(
        anyObject(RepositoryVersionEntity.class),
        anyObject(VersionDefinitionXml.class),
        EasyMock.anyBoolean())).andReturn(new ArrayList<>(hostsForCluster.values())).anyTimes();


    ExecutionCommand executionCommand = createNiceMock(ExecutionCommand.class);
    ExecutionCommandWrapper executionCommandWrapper = createNiceMock(ExecutionCommandWrapper.class);

    expect(executionCommandWrapper.getExecutionCommand()).andReturn(executionCommand).anyTimes();

    Stage stage = createNiceMock(Stage.class);
    expect(stage.getExecutionCommandWrapper(anyObject(String.class), anyObject(String.class))).
            andReturn(executionCommandWrapper).anyTimes();

    expect(executionCommand.getHostLevelParams()).andReturn(hostLevelParams).anyTimes();

    Map<Role, Float> successFactors = new HashMap<>();
    expect(stage.getSuccessFactors()).andReturn(successFactors).atLeastOnce();

    // Check that we create proper stage count
    expect(stageFactory.createNew(anyLong(), anyObject(String.class),
            anyObject(String.class), anyLong(),
            anyObject(String.class), anyObject(String.class),
            anyObject(String.class))).andReturn(stage).
            times((int) Math.ceil(hostCount / MAX_TASKS_PER_STAGE));

    expect(
            repositoryVersionDAOMock.findByStackAndVersion(
                    anyObject(StackId.class),
                    anyObject(String.class))).andReturn(repoVersion);

    expect(actionManager.getRequestTasks(anyLong())).andReturn(Collections.<HostRoleCommand>emptyList()).anyTimes();

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterId(1l);
    clusterEntity.setClusterName(clusterName);

    StageUtils.setTopologyManager(injector.getInstance(TopologyManager.class));
    StageUtils.setConfiguration(injector.getInstance(Configuration.class));

    // replay
    replay(managementController, response, clusters, resourceProviderFactory, csvResourceProvider,
            cluster, repoVersion, repositoryVersionDAOMock, configHelper, schDatanode, schNamenode, schAMS, actionManager,
            executionCommand, executionCommandWrapper,stage, stageFactory);

    ResourceProvider provider = createProvider(managementController);
    injector.injectMembers(provider);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    Map<String, Object> properties = new LinkedHashMap<>();

    // add properties to the request map
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID, "2.2.0.1-885");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_STACK_PROPERTY_ID, "HDP");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID, "2.1.1");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    RequestStatus status = provider.createResources(request);
    Assert.assertNotNull(status);

    // verify
    verify(managementController, response, clusters, stageFactory, stage);

    // check that the success factor was populated in the stage
    Float successFactor = successFactors.get(Role.INSTALL_PACKAGES);
    Assert.assertEquals(Float.valueOf(0.85f), successFactor);
  }

  @Test
  public void testGetSorted() throws Exception {
    Cluster cluster = createNiceMock(Cluster.class);
    StackId stackId = new StackId("HDP", "2.2.0");

    StackEntity stackEntity = new StackEntity();
    stackEntity.setStackName(stackId.getStackName());
    stackEntity.setStackVersion(stackId.getStackVersion());

    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider csvResourceProvider = createNiceMock(
        ClusterStackVersionResourceProvider.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster);

    expect(cluster.getClusterId()).andReturn(1L).anyTimes();

    String[] versionStrings = new String[] {
      "2.1.0.0-15",  // idx 0, sorted to 0
      "2.1.0.5-17",  // idx 1, sorted to 2
      "2.1.1.5-19",  // idx 2, sorted to 3
      "2.1.0.3-14",  // idx 3, sorted to 1
      "2.1.1.5-74"   // idx 4, sorted to 4
    };

    List<RepositoryVersionEntity> repoVersionList = new ArrayList<>();
    for (int i = 0; i < versionStrings.length; i++) {
      Long id = new Long(i);

      RepositoryVersionEntity repoVersion = createNiceMock(RepositoryVersionEntity.class);
      expect(repoVersion.getVersion()).andReturn(versionStrings[i]).anyTimes();
      expect(repoVersion.getStack()).andReturn(stackEntity).anyTimes();
      expect(repoVersion.getId()).andReturn(id).anyTimes();
      expect(repositoryVersionDAOMock.findByPK(id)).andReturn(repoVersion).anyTimes();

      repoVersionList.add(repoVersion);

      replay(repoVersion);
    }

    expect(repositoryVersionDAOMock.findAll()).andReturn(repoVersionList).atLeastOnce();

    expect(hostVersionDAO.findHostVersionByClusterAndRepository(
        anyLong(), anyObject(RepositoryVersionEntity.class))).andReturn(Collections.<HostVersionEntity>emptyList()).anyTimes();

    // replay
    replay(response, clusters, resourceProviderFactory,
        csvResourceProvider, cluster, repositoryVersionDAOMock, configHelper,
        stageFactory, hostVersionDAO);

    ResourceProvider provider = createProvider(managementController);
    injector.injectMembers(provider);

//    Field field = ClusterStackVersionResourceProvider.class.getDeclaredField("clusters");
//    field.setAccessible(true);
//    field.set(null, new Provider<Clusters>() {
//      @Override
//      public Clusters get() {
//        return clusters;
//      }
//    });

    // set the security auth
    SecurityContextHolder.getContext().setAuthentication(
        TestAuthenticationFactory.createAdministrator());

    Set<String> ids = Sets.newHashSet(
        ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_STATE_PROPERTY_ID,
        ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID,
        ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID);

    // get cluster named Cluster100
    Predicate predicate = new PredicateBuilder()
        .property(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100")
        .toPredicate();

    // create the request
    Request request = PropertyHelper.getReadRequest(ids);

    Set<Resource> responses = provider.getResources(request, predicate);
    Assert.assertNotNull(responses);

    // verify
    verify(response, clusters, cluster, hostVersionDAO);

    Assert.assertEquals(5, responses.size());

    int i = 0;
    // see the string array above.  this is the order matching the sorted strings
    long[] orders = new long[] { 0, 3, 1, 2, 4 };
    for (Resource res : responses) {
      Assert.assertEquals(orders[i], res.getPropertyValue(
          ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID));

      i++;
    }


  }

   private void testCreateResourcesExistingUpgrade(Authentication authentication) throws Exception {
    Cluster cluster = createNiceMock(Cluster.class);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();

    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster);

    UpgradeEntity upgrade = new UpgradeEntity();
    upgrade.setDirection(Direction.UPGRADE);

    expect(cluster.getClusterName()).andReturn("c1").atLeastOnce();
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(cluster.getUpgradeInProgress()).andReturn(upgrade).once();

    // replay
    replay(managementController, clusters, cluster);

    ResourceProvider provider = createProvider(managementController);
    injector.injectMembers(provider);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    Map<String, Object> properties = new LinkedHashMap<>();

    // add properties to the request map
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID, "2.2.0.1-885");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_STACK_PROPERTY_ID, "HDP");
    properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID, "2.1.1");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    try {
      provider.createResources(request);
      Assert.fail("Expecting the create to fail due to an already installed version");
    } catch (IllegalArgumentException iae) {
      // !!! expected
      Assert.assertEquals("Cluster c1 upgrade is in progress.  Cannot install packages.", iae.getMessage());
    }

    verify(cluster);

  }

   @Test
   public void testCreateResourcesPatch() throws Exception {

     Cluster cluster = createNiceMock(Cluster.class);
     StackId stackId = new StackId("HDP", "2.0.1");

     StackEntity fromStack = new StackEntity();
     fromStack.setStackName(stackId.getStackName());
     fromStack.setStackVersion(stackId.getStackVersion());

     RepositoryVersionEntity fromRepo = new RepositoryVersionEntity();
     fromRepo.setId(0L);
     fromRepo.addRepoOsEntities(REPO_OS_NOT_MANAGED);
     fromRepo.setVersionXsd("version_definition.xsd");
     fromRepo.setType(RepositoryType.STANDARD);
     fromRepo.setStack(fromStack);
     fromRepo.setVersion("2.0.1.0-1234");


     StackEntity stackEntity = new StackEntity();
     stackEntity.setStackName("HDP");
     stackEntity.setStackVersion("2.1.1");

     String hbaseVersionTestXML = "\n" +
       "<repository-version xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
       "  xsi:noNamespaceSchemaLocation=\"version_definition.xsd\">\n" +
       "  \n" +
       "  <release>\n" +
       "    <type>PATCH</type>\n" +
       "    <stack-id>HDP-2.3</stack-id>\n" +
       "    <version>2.3.4.0</version>\n" +
       "    <build>3396</build>\n" +
       "    <compatible-with>2.3.2.[0-9]</compatible-with>\n" +
       "    <release-notes>http://docs.hortonworks.com/HDPDocuments/HDP2/HDP-2.3.4/</release-notes>\n" +
       "  </release>\n" +
       "  \n" +
       "  <manifest>\n" +
       "    <service id=\"HBASE-112\" name=\"HBASE\" version=\"1.1.2\" version-id=\"2_3_4_0-3396\" />\n" +
       "  </manifest>\n" +
       "  \n" +
       "  <available-services>\n" +
       "    <service idref=\"HBASE-112\" />\n" +
       "  </available-services>\n" +
       "  \n" +
       "  <repository-info>\n" +
       "    <os family=\"redhat6\">\n" +
       "      <package-version>2_3_4_0_3396</package-version>\n" +
       "      <repo>\n" +
       "        <baseurl>http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.3.4.0</baseurl>\n" +
       "        <repoid>HDP-2.3</repoid>\n" +
       "        <reponame>HDP</reponame>\n" +
       "        <unique>true</unique>\n" +
       "      </repo>\n" +
       "      <repo>\n" +
       "        <baseurl>http://public-repo-1.hortonworks.com/HDP-UTILS-1.1.0.20/repos/centos6</baseurl>\n" +
       "        <repoid>HDP-UTILS-1.1.0.20</repoid>\n" +
       "        <reponame>HDP-UTILS</reponame>\n" +
       "        <unique>false</unique>\n" +
       "      </repo>\n" +
       "    </os>\n" +
       "  </repository-info>\n" +
       "  \n" +
       "  <upgrade>\n" +
       "    <configuration type=\"hdfs-site\">\n" +
       "      <set key=\"foo\" value=\"bar\" />\n" +
       "    </configuration>\n" +
       "  </upgrade>\n" +
       "</repository-version>";

     String xmlString = hbaseVersionTestXML;
     // hack to remove ZK
     xmlString = xmlString.replace("<service idref=\"ZOOKEEPER-346\" />", "");


     RepositoryVersionEntity repoVersion = new RepositoryVersionEntity();
     repoVersion.setId(1l);
     repoVersion.addRepoOsEntities(REPO_OS_NOT_MANAGED);
     repoVersion.setVersionXml(xmlString);
     repoVersion.setVersionXsd("version_definition.xsd");
     repoVersion.setType(RepositoryType.PATCH);
     repoVersion.setStack(stackEntity);

     ambariMetaInfo.getComponent("HDP", "2.1.1", "HBASE", "HBASE_MASTER").setVersionAdvertised(true);
     ambariMetaInfo.getComponent("HDP", "2.0.1", "HBASE", "HBASE_MASTER").setVersionAdvertised(true);

     final String hostWithoutVersionableComponents = "host3";
     List<Host> hostsNeedingInstallCommands = new ArrayList<>();

     Map<String, Host> hostsForCluster = new HashMap<>();
     int hostCount = 10;
     for (int i = 0; i < hostCount; i++) {
       String hostname = "host" + i;
       Host host = createNiceMock(hostname, Host.class);
       expect(host.getHostName()).andReturn(hostname).anyTimes();
       expect(host.getOsFamily()).andReturn("redhat6").anyTimes();
       expect(host.getMaintenanceState(EasyMock.anyLong())).andReturn(
           MaintenanceState.OFF).anyTimes();
       expect(host.getAllHostVersions()).andReturn(
           Collections.<HostVersionEntity>emptyList()).anyTimes();
       expect(host.getHostAttributes()).andReturn(new HashMap<String, String>()).anyTimes();
       replay(host);
       hostsForCluster.put(hostname, host);

       if (StringUtils.equals(hostWithoutVersionableComponents, hostname)) {
         hostsNeedingInstallCommands.add(host);
       }
     }

     Service hdfsService = createNiceMock(Service.class);
     expect(hdfsService.getName()).andReturn("HDFS").anyTimes();
     expect(hdfsService.getServiceComponents()).andReturn(new HashMap<String, ServiceComponent>());
     expect(hdfsService.getDesiredRepositoryVersion()).andReturn(fromRepo).anyTimes();

     Service hbaseService = createNiceMock(Service.class);
     expect(hbaseService.getName()).andReturn("HBASE").anyTimes();
     expect(hbaseService.getServiceComponents()).andReturn(new HashMap<String, ServiceComponent>());
     expect(hbaseService.getDesiredRepositoryVersion()).andReturn(fromRepo).anyTimes();

     Map<String, Service> serviceMap = new HashMap<>();
     serviceMap.put("HDFS", hdfsService);
     serviceMap.put("HBASE", hbaseService);

     final ServiceComponentHost schDatanode = createMock(ServiceComponentHost.class);
     expect(schDatanode.getServiceName()).andReturn("HDFS").anyTimes();
     expect(schDatanode.getServiceComponentName()).andReturn("DATANODE").anyTimes();

     final ServiceComponentHost schNamenode = createMock(ServiceComponentHost.class);
     expect(schNamenode.getServiceName()).andReturn("HDFS").anyTimes();
     expect(schNamenode.getServiceComponentName()).andReturn("NAMENODE").anyTimes();

     final ServiceComponentHost schHBM = createMock(ServiceComponentHost.class);
     expect(schHBM.getServiceName()).andReturn("HBASE").anyTimes();
     expect(schHBM.getServiceComponentName()).andReturn("HBASE_MASTER").anyTimes();

     // First host contains versionable components
     final List<ServiceComponentHost> schsH1 = Arrays.asList(schDatanode, schNamenode);

     // Second host contains versionable components
     final List<ServiceComponentHost> schsH2 = Arrays.asList(schDatanode);

     // Third host only has hbase
     final List<ServiceComponentHost> schsH3 = Arrays.asList(schHBM);

     ServiceOsSpecific.Package hdfsPackage = new ServiceOsSpecific.Package();
     hdfsPackage.setName("hdfs");

     List<ServiceOsSpecific.Package> packages = Collections.singletonList(hdfsPackage);

     ActionManager actionManager = createNiceMock(ActionManager.class);

     RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
     ResourceProvider csvResourceProvider = createNiceMock(ResourceProvider.class);

     Map<String, Map<String, String>> hostConfigTags = new HashMap<>();
     expect(configHelper.getEffectiveDesiredTags(anyObject(ClusterImpl.class), anyObject(String.class))).andReturn(hostConfigTags);

     expect(managementController.getClusters()).andReturn(clusters).anyTimes();
     expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
     expect(managementController.getAuthName()).andReturn("admin").anyTimes();
     expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
     expect(managementController.getJdkResourceUrl()).andReturn("/JdkResourceUrl").anyTimes();
     expect(managementController.getPackagesForServiceHost(anyObject(ServiceInfo.class),
         EasyMock.<Map<String, String>>anyObject(), anyObject(String.class))).
     andReturn(packages).anyTimes(); // only one host has the versionable component

     expect(managementController.findConfigurationTagsWithOverrides(anyObject(Cluster.class), EasyMock.anyString()))
     .andReturn(new HashMap<String, Map<String, String>>()).anyTimes();

     expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster).anyTimes();
     expect(clusters.getHostsForCluster(anyObject(String.class))).andReturn(
         hostsForCluster).anyTimes();

     String clusterName = "Cluster100";
     expect(cluster.getClusterId()).andReturn(1L).anyTimes();
     expect(cluster.getHosts()).andReturn(hostsForCluster.values()).atLeastOnce();
     expect(cluster.getServices()).andReturn(serviceMap).anyTimes();
     expect(cluster.getCurrentStackVersion()).andReturn(stackId);
     expect(cluster.getServiceComponentHosts(anyObject(String.class))).andAnswer(new IAnswer<List<ServiceComponentHost>>() {
       @Override
       public List<ServiceComponentHost> answer() throws Throwable {
         String hostname = (String) EasyMock.getCurrentArguments()[0];
         if (hostname.equals("host2")) {
           return schsH2;
         } else if (hostname.equals("host3")) {
           return schsH3;
         } else {
           return schsH1;
         }
       }
     }).anyTimes();

     expect(cluster.transitionHostsToInstalling(anyObject(RepositoryVersionEntity.class),
         anyObject(VersionDefinitionXml.class), eq(false))).andReturn(hostsNeedingInstallCommands).atLeastOnce();

     ExecutionCommand executionCommand = new ExecutionCommand();
     ExecutionCommandWrapper executionCommandWrapper = createNiceMock(ExecutionCommandWrapper.class);

     expect(executionCommandWrapper.getExecutionCommand()).andReturn(executionCommand).anyTimes();

     Stage stage = createNiceMock(Stage.class);
     expect(stage.getExecutionCommandWrapper(anyObject(String.class), anyObject(String.class))).
     andReturn(executionCommandWrapper).anyTimes();

     Map<Role, Float> successFactors = new HashMap<>();
     expect(stage.getSuccessFactors()).andReturn(successFactors).atLeastOnce();

     // Check that we create proper stage count
     expect(stageFactory.createNew(anyLong(), anyObject(String.class),
         anyObject(String.class), anyLong(),
         anyObject(String.class), anyObject(String.class),
         anyObject(String.class))).andReturn(stage).
     times((int) Math.ceil(hostCount / MAX_TASKS_PER_STAGE));

     expect(repositoryVersionDAOMock.findByStackAndVersion(anyObject(StackId.class),
         anyObject(String.class))).andReturn(repoVersion);

     Capture<org.apache.ambari.server.actionmanager.Request> c = Capture.newInstance();
     Capture<ExecuteActionRequest> ear = Capture.newInstance();

     actionManager.sendActions(capture(c), capture(ear));
     expectLastCall().atLeastOnce();
     expect(actionManager.getRequestTasks(anyLong())).andReturn(Collections.<HostRoleCommand>emptyList()).anyTimes();

     ClusterEntity clusterEntity = new ClusterEntity();
     clusterEntity.setClusterId(1l);
     clusterEntity.setClusterName(clusterName);

     StageUtils.setTopologyManager(injector.getInstance(TopologyManager.class));
     StageUtils.setConfiguration(injector.getInstance(Configuration.class));

     // replay
     replay(managementController, response, clusters, hdfsService, hbaseService, csvResourceProvider,
         cluster, repositoryVersionDAOMock, configHelper, schDatanode, schNamenode, schHBM, actionManager,
         executionCommandWrapper,stage, stageFactory);

     ResourceProvider provider = createProvider(managementController);
     injector.injectMembers(provider);

     // add the property map to a set for the request.  add more maps for multiple creates
     Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

     Map<String, Object> properties = new LinkedHashMap<>();

     // add properties to the request map
     properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
     properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_REPOSITORY_VERSION_PROPERTY_ID, "2.2.0.1-885");
     properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_STACK_PROPERTY_ID, "HDP");
     properties.put(ClusterStackVersionResourceProvider.CLUSTER_STACK_VERSION_VERSION_PROPERTY_ID, "2.1.1");

     propertySet.add(properties);

     // create the request
     Request request = PropertyHelper.getCreateRequest(propertySet, null);

     SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

     RequestStatus status = provider.createResources(request);
     Assert.assertNotNull(status);

     // check that the success factor was populated in the stage
     Float successFactor = successFactors.get(Role.INSTALL_PACKAGES);
     Assert.assertEquals(Float.valueOf(0.85f), successFactor);

     Assert.assertNotNull(executionCommand.getRepositoryFile());
     Assert.assertNotNull(executionCommand.getRoleParameters());
     Map<String, Object> roleParams = executionCommand.getRoleParameters();
     Assert.assertTrue(roleParams.containsKey(KeyNames.CLUSTER_VERSION_SUMMARY));
     Assert.assertEquals(ClusterVersionSummary.class,
         roleParams.get(KeyNames.CLUSTER_VERSION_SUMMARY).getClass());

     Assert.assertEquals(2, executionCommand.getRepositoryFile().getRepositories().size());
     for (CommandRepository.Repository repo : executionCommand.getRepositoryFile().getRepositories()) {
       Assert.assertFalse(repo.isAmbariManaged());
     }

   }

   private ClusterStackVersionResourceProvider createProvider(AmbariManagementController amc) {
     ResourceProviderFactory factory = injector.getInstance(ResourceProviderFactory.class);
     AbstractControllerResourceProvider.init(factory);

     Resource.Type type = Type.ClusterStackVersion;
     return (ClusterStackVersionResourceProvider) AbstractControllerResourceProvider.getResourceProvider(type,
         amc);
   }

  private class MockModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(RepositoryVersionDAO.class).toInstance(repositoryVersionDAOMock);
      bind(ConfigHelper.class).toInstance(configHelper);
      bind(Configuration.class).toInstance(configuration);
      bind(StageFactory.class).toInstance(stageFactory);
      bind(HostVersionDAO.class).toInstance(hostVersionDAO);
      bind(HostComponentStateDAO.class).toInstance(hostComponentStateDAO);
      bind(Clusters.class).toInstance(clusters);
      bind(ActionManager.class).toInstance(actionManager);
      bind(AmbariManagementController.class).toInstance(managementController);
    }
  }


}
