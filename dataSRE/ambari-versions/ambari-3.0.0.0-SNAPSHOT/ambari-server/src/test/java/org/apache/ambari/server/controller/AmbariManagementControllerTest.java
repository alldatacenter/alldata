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

package org.apache.ambari.server.controller;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.text.MessageFormat;
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
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.StackAccessException;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.ActionType;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Request;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.actionmanager.TargetHostType;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.ClusterStackVersionResourceProviderTest;
import org.apache.ambari.server.controller.internal.ComponentResourceProviderTest;
import org.apache.ambari.server.controller.internal.DeleteStatusMetaData;
import org.apache.ambari.server.controller.internal.HostComponentResourceProviderTest;
import org.apache.ambari.server.controller.internal.HostResourceProviderTest;
import org.apache.ambari.server.controller.internal.RequestOperationLevel;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.controller.internal.ServiceResourceProviderTest;
import org.apache.ambari.server.controller.internal.TaskResourceProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.customactions.ActionDefinition;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.ExecutionCommandDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.dao.TopologyHostInfoDAO;
import org.apache.ambari.server.orm.dao.WidgetDAO;
import org.apache.ambari.server.orm.dao.WidgetLayoutDAO;
import org.apache.ambari.server.orm.entities.ExecutionCommandEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.WidgetEntity;
import org.apache.ambari.server.orm.entities.WidgetLayoutEntity;
import org.apache.ambari.server.orm.entities.WidgetLayoutUserWidgetEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.serveraction.ServerAction;
import org.apache.ambari.server.stack.StackManagerMock;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostComponentAdminState;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.ServiceOsSpecific;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostInstallEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostOpSucceededEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostServerActionEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartedEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStopEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStoppedEvent;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.collections.CollectionUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class AmbariManagementControllerTest {

  private static final Logger LOG =
      LoggerFactory.getLogger(AmbariManagementControllerTest.class);

  private static final String STACK_NAME = "HDP";

  private static final String SERVICE_NAME_YARN = "YARN";
  private static final String COMPONENT_NAME_NODEMANAGER = "NODEMANAGER";
  private static final String SERVICE_NAME_HBASE = "HBASE";
  private static final String COMPONENT_NAME_REGIONSERVER = "HBASE_REGIONSERVER";
  private static final String COMPONENT_NAME_DATANODE = "DATANODE";
  private static final String SERVICE_NAME_HIVE = "HIVE";
  private static final String COMPONENT_NAME_HIVE_METASTORE = "HIVE_METASTORE";
  private static final String COMPONENT_NAME_HIVE_SERVER = "HIVE_SERVER";
  private static final String STACK_VERSION = "0.2";
  private static final String NEW_STACK_VERSION = "2.0.6";
  private static final String OS_TYPE = "centos5";
  private static final String REPO_ID = "HDP-1.1.1.16";
  private static final String REPO_NAME = "HDP";
  private static final String PROPERTY_NAME = "hbase.regionserver.msginterval";
  private static final String SERVICE_NAME = "HDFS";
  private static final String FAKE_SERVICE_NAME = "FAKENAGIOS";
  private static final int STACK_VERSIONS_CNT = 17;
  private static final int REPOS_CNT = 3;
  private static final int STACK_PROPERTIES_CNT = 103;
  private static final int STACK_COMPONENTS_CNT = 5;
  private static final int OS_CNT = 2;

  private static final String NON_EXT_VALUE = "XXX";
  private static final String INCORRECT_BASE_URL = "http://incorrect.url";

  private static final String COMPONENT_NAME = "NAMENODE";

  private static final String REQUEST_CONTEXT_PROPERTY = "context";

  private static AmbariManagementController controller;
  private static Clusters clusters;
  private ActionDBAccessor actionDB;
  private static Injector injector;
  private ServiceFactory serviceFactory;
  private ServiceComponentFactory serviceComponentFactory;
  private ServiceComponentHostFactory serviceComponentHostFactory;
  private static AmbariMetaInfo ambariMetaInfo;
  private EntityManager entityManager;
  private ConfigHelper configHelper;
  private ConfigGroupFactory configGroupFactory;
  private OrmTestHelper helper;
  private StageFactory stageFactory;
  private HostDAO hostDAO;
  private TopologyHostInfoDAO topologyHostInfoDAO;
  private HostRoleCommandDAO hostRoleCommandDAO;
  private StackManagerMock stackManagerMock;
  private RepositoryVersionDAO repositoryVersionDAO;

  RepositoryVersionEntity repositoryVersion01;
  RepositoryVersionEntity repositoryVersion02;
  RepositoryVersionEntity repositoryVersion120;
  RepositoryVersionEntity repositoryVersion201;
  RepositoryVersionEntity repositoryVersion206;
  RepositoryVersionEntity repositoryVersion207;
  RepositoryVersionEntity repositoryVersion208;
  RepositoryVersionEntity repositoryVersion220;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @BeforeClass
  public static void beforeClass() throws Exception {
    InMemoryDefaultTestModule module = new InMemoryDefaultTestModule();
    injector = Guice.createInjector(module);
    H2DatabaseCleaner.resetSequences(injector);
    injector.getInstance(GuiceJpaInitializer.class);
    ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();
    clusters = injector.getInstance(Clusters.class);
    controller = injector.getInstance(AmbariManagementController.class);
    TopologyManager topologyManager = injector.getInstance(TopologyManager.class);
    StageUtils.setTopologyManager(topologyManager);
    Configuration configuration = injector.getInstance(Configuration.class);
    StageUtils.setConfiguration(configuration);
    ActionManager.setTopologyManager(topologyManager);

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());
  }

  @Before
  public void setup() throws Exception {
    EventBusSynchronizer.synchronizeAmbariEventPublisher(injector);

    entityManager = injector.getProvider(EntityManager.class).get();
    actionDB = injector.getInstance(ActionDBAccessor.class);
    serviceFactory = injector.getInstance(ServiceFactory.class);
    serviceComponentFactory = injector.getInstance(
        ServiceComponentFactory.class);
    serviceComponentHostFactory = injector.getInstance(
        ServiceComponentHostFactory.class);
    configHelper = injector.getInstance(ConfigHelper.class);
    configGroupFactory = injector.getInstance(ConfigGroupFactory.class);
    helper = injector.getInstance(OrmTestHelper.class);
    stageFactory = injector.getInstance(StageFactory.class);
    hostDAO = injector.getInstance(HostDAO.class);
    topologyHostInfoDAO = injector.getInstance(TopologyHostInfoDAO.class);
    hostRoleCommandDAO = injector.getInstance(HostRoleCommandDAO.class);
    stackManagerMock = (StackManagerMock) ambariMetaInfo.getStackManager();
    EasyMock.replay(injector.getInstance(AuditLogger.class));

    repositoryVersion01 = helper.getOrCreateRepositoryVersion(
        new StackId("HDP-0.1"), "0.1-1234");

    repositoryVersion02 = helper.getOrCreateRepositoryVersion(
        new StackId("HDP-0.2"), "0.2-1234");

    repositoryVersion120 = helper.getOrCreateRepositoryVersion(
        new StackId("HDP-1.2.0"), "1.2.0-1234");

    repositoryVersion201 = helper.getOrCreateRepositoryVersion(
        new StackId("HDP-2.0.1"), "2.0.1-1234");

    repositoryVersion206 = helper.getOrCreateRepositoryVersion(
        new StackId("HDP-2.0.6"), "2.0.6-1234");

    repositoryVersion207 = helper.getOrCreateRepositoryVersion(
        new StackId("HDP-2.0.7"), "2.0.7-1234");

    repositoryVersion208 = helper.getOrCreateRepositoryVersion(
        new StackId("HDP-2.0.8"), "2.0.8-1234");

    repositoryVersion220 = helper.getOrCreateRepositoryVersion(
        new StackId("HDP-2.2.0"), "2.2.0-1234");

    repositoryVersionDAO = injector.getInstance(RepositoryVersionDAO.class);

    for (Host host : clusters.getHosts()) {
      clusters.updateHostMappings(host);
    }
  }

  @After
  public void teardown() {
    actionDB = null;
    EasyMock.reset(injector.getInstance(AuditLogger.class));
  }

  @AfterClass
  public static void afterClass() throws Exception {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  private static String getUniqueName() {
    return UUID.randomUUID().toString();
  }


  private void setOsFamily(Host host, String osFamily, String osVersion) {
    Map<String, String> hostAttributes = new HashMap<>();
    hostAttributes.put("os_family", osFamily);
    hostAttributes.put("os_release_version", osVersion);

    host.setHostAttributes(hostAttributes);
  }

  private void addHost(String hostname) throws Exception {
    addHostToCluster(hostname, null);
  }

  private void addHostToCluster(String hostname, String clusterName) throws Exception {

    if (!clusters.hostExists(hostname)) {
      clusters.addHost(hostname);
      setOsFamily(clusters.getHost(hostname), "redhat", "6.3");
      clusters.getHost(hostname).setState(HostState.HEALTHY);
    }

    if (null != clusterName) {
      clusters.mapHostToCluster(hostname, clusterName);
    }
    clusters.updateHostMappings(clusters.getHost(hostname));
  }

  private void deleteHost(String hostname) throws Exception {
    clusters.deleteHost(hostname);
  }



  /**
   * Creates a Cluster object, along with its corresponding ClusterVersion based on the stack.
   * @param clusterName Cluster name
   * @throws Exception
   */
  private void createCluster(String clusterName) throws Exception{
    RepositoryVersionDAO repoDAO = injector.getInstance(RepositoryVersionDAO.class);
    ClusterRequest r = new ClusterRequest(null, clusterName, State.INSTALLED.name(), SecurityType.NONE, "HDP-0.1", null);
    controller.createCluster(r);
  }

  private void createService(String clusterName, String serviceName, State desiredState) throws Exception, AuthorizationException {
    createService(clusterName, serviceName, repositoryVersion02, desiredState);
  }

  private void createService(String clusterName, String serviceName,
      RepositoryVersionEntity repositoryVersion, State desiredState)
      throws Exception, AuthorizationException {
    String dStateStr = null;
    if (desiredState != null) {
      dStateStr = desiredState.toString();
    }

    ServiceRequest r1 = new ServiceRequest(clusterName, serviceName,
        repositoryVersion.getId(), dStateStr,
        null);

    Set<ServiceRequest> requests = new HashSet<>();
    requests.add(r1);

    ServiceResourceProviderTest.createServices(controller, repositoryVersionDAO, requests);
  }

  private void createServiceComponent(String clusterName,
      String serviceName, String componentName, State desiredState)
      throws Exception, AuthorizationException {
    String dStateStr = null;
    if (desiredState != null) {
      dStateStr = desiredState.toString();
    }
    ServiceComponentRequest r = new ServiceComponentRequest(clusterName,
        serviceName, componentName, dStateStr);
    Set<ServiceComponentRequest> requests =
      new HashSet<>();
    requests.add(r);
    ComponentResourceProviderTest.createComponents(controller, requests);
  }

  private void createServiceComponentHost(String clusterName,
      String serviceName, String componentName, String hostname,
      State desiredState) throws Exception, AuthorizationException {
    String dStateStr = null;
    if (desiredState != null) {
      dStateStr = desiredState.toString();
    }
    ServiceComponentHostRequest r = new ServiceComponentHostRequest(clusterName,
        serviceName, componentName, hostname, dStateStr);
    Set<ServiceComponentHostRequest> requests =
      new HashSet<>();
    requests.add(r);
    controller.createHostComponents(requests);
  }

  private void deleteServiceComponentHost(String clusterName,
                                          String serviceName, String componentName, String hostname,
                                          State desiredState) throws Exception, AuthorizationException {
    String dStateStr = null;
    if (desiredState != null) {
      dStateStr = desiredState.toString();
    }
    ServiceComponentHostRequest r = new ServiceComponentHostRequest(clusterName,
        serviceName, componentName, hostname, dStateStr);
    Set<ServiceComponentHostRequest> requests =
      new HashSet<>();
    requests.add(r);
    controller.deleteHostComponents(requests);
  }

  private Long createConfigGroup(Cluster cluster, String serviceName, String name, String tag,
                              List<String> hosts, List<Config> configs)
                              throws Exception {

    Map<Long, Host> hostMap = new HashMap<>();
    Map<String, Config> configMap = new HashMap<>();

    for (String hostname : hosts) {
      Host host = clusters.getHost(hostname);
      HostEntity hostEntity = hostDAO.findByName(hostname);
      hostMap.put(hostEntity.getHostId(), host);
    }

    for (Config config : configs) {
      configMap.put(config.getType(), config);
    }

    ConfigGroup configGroup = configGroupFactory.createNew(cluster, serviceName, name,
      tag, "", configMap, hostMap);

    configGroup.setServiceName(serviceName);

    cluster.addConfigGroup(configGroup);

    return configGroup.getId();
  }

  private long stopService(String clusterName, String serviceName,
      boolean runSmokeTests, boolean reconfigureClients) throws
      Exception, AuthorizationException {
    ServiceRequest r = new ServiceRequest(clusterName, serviceName, null, State.INSTALLED.toString(), null);
    Set<ServiceRequest> requests = new HashSet<>();
    requests.add(r);
    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");
    RequestStatusResponse resp = ServiceResourceProviderTest.updateServices(controller, requests,
      mapRequestProps, runSmokeTests, reconfigureClients);

    Assert.assertEquals(State.INSTALLED,
      clusters.getCluster(clusterName).getService(serviceName)
        .getDesiredState());

    // manually change live state to stopped as no running action manager
    for (ServiceComponent sc :
      clusters.getCluster(clusterName).getService(serviceName)
        .getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        sch.setState(State.INSTALLED);
      }
    }

    return resp.getRequestId();
  }

  private long stopServiceComponentHosts(String clusterName,
      String serviceName) throws Exception {
    Cluster c = clusters.getCluster(clusterName);
    Service s = c.getService(serviceName);
    Set<ServiceComponentHostRequest> requests = new
      HashSet<>();
    for (ServiceComponent sc : s.getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        ServiceComponentHostRequest schr = new ServiceComponentHostRequest
          (clusterName, serviceName, sc.getName(),
            sch.getHostName(), State.INSTALLED.name());
        requests.add(schr);
      }
    }
    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");
    RequestStatusResponse resp = HostComponentResourceProviderTest.updateHostComponents(controller, injector, requests,
        mapRequestProps, false);

    // manually change live state to started as no running action manager
    for (ServiceComponent sc :
      clusters.getCluster(clusterName).getService(serviceName)
        .getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        sch.setState(State.INSTALLED);
      }
    }
    return resp.getRequestId();
  }

  private long startService(String clusterName, String serviceName,
                            boolean runSmokeTests, boolean reconfigureClients) throws
      Exception, AuthorizationException {
    return startService(clusterName, serviceName, runSmokeTests, reconfigureClients, null);
  }


  private long startService(String clusterName, String serviceName,
                            boolean runSmokeTests, boolean reconfigureClients,
                            MaintenanceStateHelper maintenanceStateHelper) throws
      Exception, AuthorizationException {
    ServiceRequest r = new ServiceRequest(clusterName, serviceName, repositoryVersion02.getId(),
        State.STARTED.toString(), null);
    Set<ServiceRequest> requests = new HashSet<>();
    requests.add(r);
    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");
    RequestStatusResponse resp = ServiceResourceProviderTest.updateServices(controller, requests,
        mapRequestProps, runSmokeTests, reconfigureClients, maintenanceStateHelper);

    Assert.assertEquals(State.STARTED,
        clusters.getCluster(clusterName).getService(serviceName)
            .getDesiredState());

    if (resp != null) {
      // manually change live state to stopped as no running action manager
      List<HostRoleCommand> commands = actionDB.getRequestTasks(resp.getRequestId());
      for (HostRoleCommand cmd : commands) {
        String scName = cmd.getRole().toString();
        if (!scName.endsWith("CHECK")) {
          Cluster cluster = clusters.getCluster(clusterName);
          String hostname = cmd.getHostName();
          for (Service s : cluster.getServices().values()) {
            if (s.getServiceComponents().containsKey(scName) &&
              !s.getServiceComponent(scName).isClientComponent()) {
              s.getServiceComponent(scName).getServiceComponentHost(hostname).
                setState(State.STARTED);
              break;
            }
          }
        }
      }
      return resp.getRequestId();
    } else {
      return -1;
    }
  }


  private long installService(String clusterName, String serviceName,
                              boolean runSmokeTests, boolean reconfigureClients)
      throws Exception, AuthorizationException {
    return installService(clusterName, serviceName, runSmokeTests, reconfigureClients, null, null);
  }


  /**
   * Allows to set maintenanceStateHelper. For use when there is anything to test
   * with maintenance mode.
   */
  private long installService(String clusterName, String serviceName,
                              boolean runSmokeTests, boolean reconfigureClients,
                              MaintenanceStateHelper maintenanceStateHelper,
                              Map<String, String> mapRequestPropsInput)
      throws Exception, AuthorizationException {

    ServiceRequest r = new ServiceRequest(clusterName, serviceName, repositoryVersion02.getId(),
        State.INSTALLED.toString(), null);

    Set<ServiceRequest> requests = new HashSet<>();
    requests.add(r);

    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");
    if(mapRequestPropsInput != null) {
      mapRequestProps.putAll(mapRequestPropsInput);
    }

    RequestStatusResponse resp = ServiceResourceProviderTest.updateServices(controller, requests,
        mapRequestProps, runSmokeTests, reconfigureClients, maintenanceStateHelper);

    Assert.assertEquals(State.INSTALLED,
        clusters.getCluster(clusterName).getService(serviceName)
            .getDesiredState());

    if (resp != null) {
      // manually change live state to stopped as no running action manager
      List<HostRoleCommand> commands = actionDB.getRequestTasks(resp.getRequestId());
      for (HostRoleCommand cmd : commands) {
        clusters.getCluster(clusterName).getService(serviceName).getServiceComponent(cmd.getRole().name())
            .getServiceComponentHost(cmd.getHostName()).setState(State.INSTALLED);
      }
      return resp.getRequestId();
    } else {
      return -1;
    }
  }


  private boolean checkExceptionType(Throwable e, Class<? extends Exception> exceptionClass) {
    return e != null && (exceptionClass.isAssignableFrom(e.getClass()) || checkExceptionType(e.getCause(), exceptionClass));
  }

  @Test
  public void testCreateClusterSimple() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    Set<ClusterResponse> r =
        controller.getClusters(Collections.singleton(
            new ClusterRequest(null, cluster1, null, null)));
    Assert.assertEquals(1, r.size());
    ClusterResponse c = r.iterator().next();
    Assert.assertEquals(cluster1, c.getClusterName());

    try {
      createCluster(cluster1);
      fail("Duplicate cluster creation should fail");
    } catch (Exception e) {
      // Expected
    }
  }

  @Test
  public void testCreateClusterWithHostMapping() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();

    String host1 = getUniqueName();
    String host2 = getUniqueName();

    Set<String> hostNames = new HashSet<>();
    hostNames.add(host1);
    hostNames.add(host2);
    ClusterRequest r = new ClusterRequest(null, cluster1, "HDP-0.1", hostNames);

    try {
      controller.createCluster(r);
      fail("Expected create cluster to fail for invalid hosts");
    } catch (Exception e) {
      // Expected
    }

    try {
      clusters.getCluster(cluster1);
      fail("Expected to fail for non created cluster");
    } catch (ClusterNotFoundException e) {
      // Expected
    }

    clusters.addHost(host1);
    clusters.addHost(host2);
    setOsFamily(clusters.getHost(host1), "redhat", "6.3");
    setOsFamily(clusters.getHost(host2), "redhat", "6.3");

    controller.createCluster(r);
    Assert.assertNotNull(clusters.getCluster(cluster1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateClusterWithInvalidRequest1() throws Exception {
    ClusterRequest r = new ClusterRequest(null, null, null, null);
    controller.createCluster(r);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateClusterWithInvalidRequest2() throws Exception {
    ClusterRequest r = new ClusterRequest(1L, null, null, null);
    controller.createCluster(r);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateClusterWithInvalidRequest3() throws Exception {
    ClusterRequest r = new ClusterRequest(null, getUniqueName(), null, null);
    controller.createCluster(r);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateClusterWithInvalidRequest4() throws Exception {
    ClusterRequest r = new ClusterRequest(null, null, State.INSTALLING.name(), null, "HDP-1.2.0", null);
    controller.createCluster(r);
    controller.updateClusters(Collections.singleton(r), null);
  }

  @Test
  public void testCreateServicesSimple() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    String serviceName = "HDFS";
    createService(cluster1, serviceName, repositoryVersion02, State.INIT);

    Service s =
        clusters.getCluster(cluster1).getService(serviceName);
    Assert.assertNotNull(s);
    Assert.assertEquals(serviceName, s.getName());
    Assert.assertEquals(cluster1, s.getCluster().getClusterName());

    ServiceRequest req = new ServiceRequest(cluster1, "HDFS", repositoryVersion02.getId(), null, null);

    Set<ServiceResponse> r =
        ServiceResourceProviderTest.getServices(controller, Collections.singleton(req));
    Assert.assertEquals(1, r.size());
    ServiceResponse resp = r.iterator().next();
    Assert.assertEquals(serviceName, resp.getServiceName());
    Assert.assertEquals(cluster1, resp.getClusterName());
    Assert.assertEquals(State.INIT.toString(), resp.getDesiredState());
    Assert.assertEquals("HDP-0.2", resp.getDesiredStackId());
  }

  @Test
  public void testCreateServicesWithInvalidRequest() throws Exception, AuthorizationException {
    // invalid request
    // dups in requests
    // multi cluster updates

    Set<ServiceRequest> set1 = new HashSet<>();

    try {
      set1.clear();
      ServiceRequest rInvalid = new ServiceRequest(null, null, null, null, null);
      set1.add(rInvalid);
      ServiceResourceProviderTest.createServices(controller, repositoryVersionDAO, set1);
      fail("Expected failure for invalid requests");
    } catch (Exception e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceRequest rInvalid = new ServiceRequest("foo", null, null, null, null);
      set1.add(rInvalid);
      ServiceResourceProviderTest.createServices(controller, repositoryVersionDAO, set1);
      fail("Expected failure for invalid requests");
    } catch (Exception e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceRequest rInvalid = new ServiceRequest("foo", "bar", null, null, null);
      set1.add(rInvalid);
      ServiceResourceProviderTest.createServices(controller, repositoryVersionDAO, set1);
      fail("Expected failure for invalid cluster");
    } catch (Exception e) {
      // Expected
      Assert.assertTrue(checkExceptionType(e, ClusterNotFoundException.class));
    }

    String cluster1 = getUniqueName();
    String cluster2 = getUniqueName();


    clusters.addCluster(cluster1, new StackId("HDP-0.1"));
    clusters.addCluster(cluster2, new StackId("HDP-0.1"));

    try {
      set1.clear();
      ServiceRequest valid1 = new ServiceRequest(cluster1, "HDFS", null, null, null);
      ServiceRequest valid2 = new ServiceRequest(cluster1, "HDFS", null, null, null);
      set1.add(valid1);
      set1.add(valid2);
      ServiceResourceProviderTest.createServices(controller, repositoryVersionDAO, set1);
      fail("Expected failure for invalid requests");
    } catch (Exception e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceRequest valid1 = new ServiceRequest(cluster1, "bar", repositoryVersion02.getId(), State.STARTED.toString(), null);
      set1.add(valid1);
      ServiceResourceProviderTest.createServices(controller, repositoryVersionDAO, set1);
      fail("Expected failure for invalid service");
    } catch (Exception e) {
      // Expected
    }


    try {
      set1.clear();
      ServiceRequest valid1 = new ServiceRequest(cluster1, "HDFS", repositoryVersion02.getId(), State.STARTED.toString(), null);
      ServiceRequest valid2 = new ServiceRequest(cluster2, "HDFS", repositoryVersion02.getId(), State.STARTED.toString(), null);
      set1.add(valid1);
      set1.add(valid2);
      ServiceResourceProviderTest.createServices(controller, repositoryVersionDAO, set1);
      fail("Expected failure for multiple clusters");
    } catch (Exception e) {
      // Expected
    }

    Assert.assertNotNull(clusters.getCluster(cluster1));
    Assert.assertEquals(0, clusters.getCluster(cluster1).getServices().size());

    set1.clear();
    ServiceRequest valid = new ServiceRequest(cluster1, "HDFS", repositoryVersion02.getId(), null, null);
    set1.add(valid);
    ServiceResourceProviderTest.createServices(controller, repositoryVersionDAO, set1);

    try {
      set1.clear();
      ServiceRequest valid1 = new ServiceRequest(cluster1, "HDFS", repositoryVersion02.getId(), State.STARTED.toString(), null);
      ServiceRequest valid2 = new ServiceRequest(cluster1, "HDFS", repositoryVersion02.getId(), State.STARTED.toString(), null);
      set1.add(valid1);
      set1.add(valid2);
      ServiceResourceProviderTest.createServices(controller, repositoryVersionDAO, set1);
      fail("Expected failure for existing service");
    } catch (Exception e) {
      // Expected
    }

    Assert.assertEquals(1, clusters.getCluster(cluster1).getServices().size());

  }

  @Test
  public void testCreateServiceWithInvalidInfo() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    String serviceName = "HDFS";
    try {
      createService(cluster1, serviceName, State.INSTALLING);
      fail("Service creation should fail for invalid state");
    } catch (Exception e) {
      // Expected
    }
    try {
      clusters.getCluster(cluster1).getService(serviceName);
      fail("Service creation should have failed");
    } catch (Exception e) {
      // Expected
    }
    try {
      createService(cluster1, serviceName, State.INSTALLED);
      fail("Service creation should fail for invalid initial state");
    } catch (Exception e) {
      // Expected
    }

    createService(cluster1, serviceName, null);

    String serviceName2 = "MAPREDUCE";
    createService(cluster1, serviceName2, State.INIT);

    ServiceRequest r = new ServiceRequest(cluster1, null, null, null, null);
    Set<ServiceResponse> response = ServiceResourceProviderTest.getServices(controller, Collections.singleton(r));
    Assert.assertEquals(2, response.size());

    for (ServiceResponse svc : response) {
      Assert.assertTrue(svc.getServiceName().equals(serviceName)
          || svc.getServiceName().equals(serviceName2));
      Assert.assertEquals("HDP-0.2", svc.getDesiredStackId());
      Assert.assertEquals(State.INIT.toString(), svc.getDesiredState());
    }
  }

  @Test
  public void testCreateServicesMultiple() throws Exception, AuthorizationException {
    Set<ServiceRequest> set1 = new HashSet<>();

    String cluster1 = getUniqueName();

    clusters.addCluster(cluster1, new StackId("HDP-0.1"));

    ServiceRequest valid1 = new ServiceRequest(cluster1, "HDFS", repositoryVersion01.getId(), null, null);
    ServiceRequest valid2 = new ServiceRequest(cluster1, "MAPREDUCE", repositoryVersion01.getId(), null, null);
    set1.add(valid1);
    set1.add(valid2);
    ServiceResourceProviderTest.createServices(controller, repositoryVersionDAO, set1);

    try {
      valid1 = new ServiceRequest(cluster1, "PIG", repositoryVersion01.getId(), null, null);
      valid2 = new ServiceRequest(cluster1, "MAPREDUCE", 4L, null, null);
      set1.add(valid1);
      set1.add(valid2);
      ServiceResourceProviderTest.createServices(controller, repositoryVersionDAO, set1);
      fail("Expected failure for invalid services");
    } catch (Exception e) {
      // Expected
      Assert.assertTrue(checkExceptionType(e, DuplicateResourceException.class));
    }

    Assert.assertNotNull(clusters.getCluster(cluster1));
    Assert.assertEquals(2, clusters.getCluster(cluster1).getServices().size());
    Assert.assertNotNull(clusters.getCluster(cluster1).getService("HDFS"));
    Assert.assertNotNull(clusters.getCluster(cluster1).getService("MAPREDUCE"));
  }

  @Test
  public void testCreateServiceComponentSimple() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);

    String componentName = "NAMENODE";
    try {
      createServiceComponent(cluster1, serviceName, componentName,
          State.INSTALLING);
      fail("ServiceComponent creation should fail for invalid state");
    } catch (Exception e) {
      // Expected
    }
    try {
      clusters.getCluster(cluster1).getService(serviceName)
          .getServiceComponent(componentName);
      fail("ServiceComponent creation should have failed");
    } catch (Exception e) {
      // Expected
    }

    createServiceComponent(cluster1, serviceName, componentName,
        State.INIT);
    Assert.assertNotNull(clusters.getCluster(cluster1)
        .getService(serviceName).getServiceComponent(componentName));

    ServiceComponentRequest r =
        new ServiceComponentRequest(cluster1, serviceName, null, null);
    Set<ServiceComponentResponse> response = ComponentResourceProviderTest.getComponents(controller, Collections.singleton(r));
    Assert.assertEquals(1, response.size());

    ServiceComponentResponse sc = response.iterator().next();
    Assert.assertEquals(State.INIT.toString(), sc.getDesiredState());
    Assert.assertEquals(componentName, sc.getComponentName());
    Assert.assertEquals(cluster1, sc.getClusterName());
    Assert.assertEquals(serviceName, sc.getServiceName());
  }

  @Test
  public void testCreateServiceComponentWithInvalidRequest()
      throws Exception, AuthorizationException {
    // multiple clusters
    // dup objects
    // existing components
    // invalid request params
    // invalid service
    // invalid cluster
    String cluster1 = getUniqueName();
    String cluster2 = getUniqueName();

    Set<ServiceComponentRequest> set1 = new HashSet<>();

    try {
      set1.clear();
      ServiceComponentRequest rInvalid =
          new ServiceComponentRequest(null, null, null, null);
      set1.add(rInvalid);
      ComponentResourceProviderTest.createComponents(controller, set1);
      fail("Expected failure for invalid requests");
    } catch (Exception e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentRequest rInvalid =
          new ServiceComponentRequest(cluster1, null, null, null);
      set1.add(rInvalid);
      ComponentResourceProviderTest.createComponents(controller, set1);
      fail("Expected failure for invalid requests");
    } catch (Exception e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentRequest rInvalid =
          new ServiceComponentRequest(cluster1, "s1", null, null);
      set1.add(rInvalid);
      ComponentResourceProviderTest.createComponents(controller, set1);
      fail("Expected failure for invalid requests");
    } catch (Exception e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentRequest rInvalid =
          new ServiceComponentRequest(cluster1, "s1", "sc1", null);
      set1.add(rInvalid);
      ComponentResourceProviderTest.createComponents(controller, set1);
      fail("Expected failure for invalid cluster");
    } catch (ParentObjectNotFoundException e) {
      // Expected
    }

    clusters.addCluster(cluster1, new StackId("HDP-0.1"));
    clusters.addCluster(cluster2, new StackId("HDP-0.1"));

    try {
      set1.clear();
      ServiceComponentRequest rInvalid =
          new ServiceComponentRequest(cluster1, "HDFS", "NAMENODE", null);
      set1.add(rInvalid);
      ComponentResourceProviderTest.createComponents(controller, set1);
      fail("Expected failure for invalid service");
    } catch (ParentObjectNotFoundException e) {
      // Expected
    }

    Cluster c1 = clusters.getCluster(cluster1);
    StackId stackId = new StackId("HDP-0.1");
    c1.setDesiredStackVersion(stackId);

    RepositoryVersionEntity repositoryVersion = helper.getOrCreateRepositoryVersion(stackId,
        stackId.getStackVersion());

    Service s1 = serviceFactory.createNew(c1, "HDFS", repositoryVersion);
    Service s2 = serviceFactory.createNew(c1, "MAPREDUCE", repositoryVersion);
    c1.addService(s1);
    c1.addService(s2);

    set1.clear();
    ServiceComponentRequest valid1 =
        new ServiceComponentRequest(cluster1, "HDFS", "NAMENODE", null);
    ServiceComponentRequest valid2 =
        new ServiceComponentRequest(cluster1, "MAPREDUCE", "JOBTRACKER", null);
    ServiceComponentRequest valid3 =
        new ServiceComponentRequest(cluster1, "MAPREDUCE", "TASKTRACKER", null);
    set1.add(valid1);
    set1.add(valid2);
    set1.add(valid3);
    ComponentResourceProviderTest.createComponents(controller, set1);

    try {
      set1.clear();
      ServiceComponentRequest rInvalid1 =
          new ServiceComponentRequest(cluster1, "HDFS", "HDFS_CLIENT", null);
      ServiceComponentRequest rInvalid2 =
          new ServiceComponentRequest(cluster1, "HDFS", "HDFS_CLIENT", null);
      set1.add(rInvalid1);
      set1.add(rInvalid2);
      ComponentResourceProviderTest.createComponents(controller, set1);
      fail("Expected failure for dups in requests");
    } catch (Exception e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentRequest rInvalid1 =
          new ServiceComponentRequest(cluster1, "HDFS", "HDFS_CLIENT", null);
      ServiceComponentRequest rInvalid2 =
          new ServiceComponentRequest(cluster2, "HDFS", "HDFS_CLIENT", null);
      set1.add(rInvalid1);
      set1.add(rInvalid2);
      ComponentResourceProviderTest.createComponents(controller, set1);
      fail("Expected failure for multiple clusters");
    } catch (Exception e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentRequest rInvalid =
          new ServiceComponentRequest(cluster1, "HDFS", "NAMENODE", null);
      set1.add(rInvalid);
      ComponentResourceProviderTest.createComponents(controller, set1);
      fail("Expected failure for already existing component");
    } catch (Exception e) {
      // Expected
    }


    Assert.assertEquals(1, s1.getServiceComponents().size());
    Assert.assertNotNull(s1.getServiceComponent("NAMENODE"));
    Assert.assertEquals(2, s2.getServiceComponents().size());
    Assert.assertNotNull(s2.getServiceComponent("JOBTRACKER"));
    Assert.assertNotNull(s2.getServiceComponent("TASKTRACKER"));

  }


  @Test
  @Ignore
  //TODO this test becomes unstable after this patch, not reproducible locally but fails in apache jenkins jobs
  //investigate and reenable
  public void testGetExecutionCommandWithClusterEnvForRetry() throws Exception {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    Cluster cluster = clusters.getCluster(cluster1);
    clusters.getCluster(cluster1)
        .setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";
    createServiceComponent(cluster1, serviceName, componentName1,
                           State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2,
                           State.INIT);
    createServiceComponent(cluster1, serviceName, componentName3,
                           State.INIT);

    String host1 = getUniqueName();
    String host2 = getUniqueName();

    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);

    Map<String, String> configs = new HashMap<>();
    configs.put("a", "b");
    configs.put("command_retry_enabled", "true");
    configs.put("command_retry_max_time_in_sec", "5");
    configs.put("commands_to_retry", "INSTALL");

    ConfigurationRequest cr1;
    cr1 = new ConfigurationRequest(cluster1, "cluster-env","version1",
                                   configs, null);

    ClusterRequest crReq = new ClusterRequest(cluster.getClusterId(), cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr1));
    controller.updateClusters(Collections.singleton(crReq), null);

    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");

    createServiceComponentHost(cluster1, serviceName, componentName2,
                               host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
                               host2, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
                               host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
                               host2, null);

    // issue an install command, expect retry is enabled
    ServiceComponentHostRequest
        schr =
        new ServiceComponentHostRequest(cluster1, "HDFS", "DATANODE", host2, "INSTALLED");
    Map<String, String> requestProps = new HashMap<>();
    requestProps.put("phase", "INITIAL_INSTALL");
    RequestStatusResponse rsr = updateHostComponents(Collections.singleton(schr), requestProps, false);

    List<Stage> stages = actionDB.getAllStages(rsr.getRequestId());
    Assert.assertEquals(1, stages.size());
    Stage stage = stages.iterator().next();
    List<ExecutionCommandWrapper> execWrappers = stage.getExecutionCommands(host2);
    Assert.assertEquals(1, execWrappers.size());
    ExecutionCommandWrapper execWrapper = execWrappers.iterator().next();
    ExecutionCommand ec = execWrapper.getExecutionCommand();
    Map<String, Map<String, String>> configurations = ec.getConfigurations();
    assertNotNull(configurations);
    assertEquals(1, configurations.size());
    assertTrue(configurations.containsKey("cluster-env"));
    assertTrue(ec.getCommandParams().containsKey("max_duration_for_retries"));
    assertEquals("5", ec.getCommandParams().get("max_duration_for_retries"));
    assertTrue(ec.getCommandParams().containsKey("command_retry_enabled"));
    assertEquals("true", ec.getCommandParams().get("command_retry_enabled"));

    for (ServiceComponentHost sch : clusters.getCluster(cluster1).getServiceComponentHosts(host2)) {
      sch.setState(State.INSTALLED);
    }

    // issue an start command but no retry as phase is only INITIAL_INSTALL
    schr = new ServiceComponentHostRequest(cluster1, "HDFS", "DATANODE", host2, "STARTED");
    rsr = updateHostComponents(Collections.singleton(schr), requestProps, false);
    stages = actionDB.getAllStages(rsr.getRequestId());
    Assert.assertEquals(1, stages.size());
    stage = stages.iterator().next();
    execWrappers = stage.getExecutionCommands(host2);
    Assert.assertEquals(1, execWrappers.size());
    execWrapper = execWrappers.iterator().next();
    ec = execWrapper.getExecutionCommand();
    configurations = ec.getConfigurations();
    assertNotNull(configurations);
    assertEquals(1, configurations.size());
    assertTrue(configurations.containsKey("cluster-env"));
    assertTrue(ec.getCommandParams().containsKey("max_duration_for_retries"));
    assertEquals("5", ec.getCommandParams().get("max_duration_for_retries"));
    assertTrue(ec.getCommandParams().containsKey("command_retry_enabled"));
    assertEquals("false", ec.getCommandParams().get("command_retry_enabled"));

    configs.put("command_retry_enabled", "true");
    configs.put("command_retry_max_time_in_sec", "12");
    configs.put("commands_to_retry", "START");

    cr1 = new ConfigurationRequest(cluster1, "cluster-env","version2",
                                   configs, null);
    crReq = new ClusterRequest(cluster.getClusterId(), cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr1));
    controller.updateClusters(Collections.singleton(crReq), null);

    // issue an start command and retry is expected
    requestProps.put("phase", "INITIAL_START");
    schr = new ServiceComponentHostRequest(cluster1, "HDFS", "DATANODE", host2, "STARTED");
    rsr = updateHostComponents(Collections.singleton(schr), requestProps, false);
    stages = actionDB.getAllStages(rsr.getRequestId());
    Assert.assertEquals(1, stages.size());
    stage = stages.iterator().next();
    execWrappers = stage.getExecutionCommands(host2);
    Assert.assertEquals(1, execWrappers.size());
    execWrapper = execWrappers.iterator().next();
    ec = execWrapper.getExecutionCommand();
    configurations = ec.getConfigurations();
    assertNotNull(configurations);
    assertEquals(1, configurations.size());
    assertTrue(configurations.containsKey("cluster-env"));
    assertTrue(ec.getCommandParams().containsKey("max_duration_for_retries"));
    assertEquals("12", ec.getCommandParams().get("max_duration_for_retries"));
    assertTrue(ec.getCommandParams().containsKey("command_retry_enabled"));
    assertEquals("true", ec.getCommandParams().get("command_retry_enabled"));

    // issue an start command and retry is expected but bad cluster-env
    configs.put("command_retry_enabled", "asdf");
    configs.put("command_retry_max_time_in_sec", "-5");
    configs.put("commands_to_retry2", "START");

    cr1 = new ConfigurationRequest(cluster1, "cluster-env","version3",
                                   configs, null);
    crReq = new ClusterRequest(cluster.getClusterId(), cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr1));
    controller.updateClusters(Collections.singleton(crReq), null);

    requestProps.put("phase", "INITIAL_START");
    schr = new ServiceComponentHostRequest(cluster1, "HDFS", "DATANODE", host2, "STARTED");
    rsr = updateHostComponents(Collections.singleton(schr), requestProps, false);
    stages = actionDB.getAllStages(rsr.getRequestId());
    Assert.assertEquals(1, stages.size());
    stage = stages.iterator().next();
    execWrappers = stage.getExecutionCommands(host2);
    Assert.assertEquals(1, execWrappers.size());
    execWrapper = execWrappers.iterator().next();
    ec = execWrapper.getExecutionCommand();
    configurations = ec.getConfigurations();
    assertNotNull(configurations);
    assertEquals(1, configurations.size());
    assertTrue(configurations.containsKey("cluster-env"));
    assertTrue(ec.getCommandParams().containsKey("max_duration_for_retries"));
    assertEquals("0", ec.getCommandParams().get("max_duration_for_retries"));
    assertTrue(ec.getCommandParams().containsKey("command_retry_enabled"));
    assertEquals("false", ec.getCommandParams().get("command_retry_enabled"));
  }


  @Test
  public void testGetExecutionCommand() throws Exception {
    String cluster1 = getUniqueName();
    final String host1 = getUniqueName();

    createServiceComponentHostSimple(cluster1, host1, getUniqueName());

    String serviceName = "HDFS";

    Cluster cluster = clusters.getCluster(cluster1);
    Service s1 = cluster.getService(serviceName);

    // Create and attach config
    Map<String, String> configs = new HashMap<>();
    configs.put("a", "b");

    Map<String, String> hadoopEnvConfigs = new HashMap<>();
    hadoopEnvConfigs.put("hdfs_user", "myhdfsuser");
    hadoopEnvConfigs.put("hdfs_group", "myhdfsgroup");

    ConfigurationRequest cr1,cr2, cr3;

    cr1 = new ConfigurationRequest(cluster1, "core-site","version1",
                                   configs, null);
    cr2 = new ConfigurationRequest(cluster1, "hdfs-site","version1",
                                   configs, null);
    cr3 = new ConfigurationRequest(cluster1, "hadoop-env","version1",
      hadoopEnvConfigs, null);

    ClusterRequest crReq = new ClusterRequest(cluster.getClusterId(), cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr1));
    controller.updateClusters(Collections.singleton(crReq), null);
    crReq = new ClusterRequest(cluster.getClusterId(), cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr2));
    controller.updateClusters(Collections.singleton(crReq), null);
    crReq = new ClusterRequest(cluster.getClusterId(), cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr3));
    controller.updateClusters(Collections.singleton(crReq), null);



    // Install
    installService(cluster1, serviceName, false, false);

    ExecutionCommand ec =
        controller.getExecutionCommand(cluster,
                                       s1.getServiceComponent("NAMENODE").getServiceComponentHost(host1),
                                       RoleCommand.START);
    assertEquals("1-0", ec.getCommandId());
    assertEquals(cluster1, ec.getClusterName());
    Map<String, Map<String, String>> configurations = ec.getConfigurations();
    assertNotNull(configurations);
    assertEquals(0, configurations.size());
    assertTrue(ec.getCommandParams().containsKey("max_duration_for_retries"));
    assertEquals("0", ec.getCommandParams().get("max_duration_for_retries"));
    assertTrue(ec.getCommandParams().containsKey("command_retry_enabled"));
    assertEquals("false", ec.getCommandParams().get("command_retry_enabled"));
    assertFalse(ec.getCommandParams().containsKey("custom_folder"));

    ec = controller.getExecutionCommand(cluster,
                                        s1.getServiceComponent("DATANODE").getServiceComponentHost(host1),
                                        RoleCommand.START);
    assertEquals(cluster1, ec.getClusterName());
    assertNotNull(ec.getCommandParams());
    assertNotNull(ec.getHostLevelParams());
    assertTrue(ec.getHostLevelParams().containsKey(ExecutionCommand.KeyNames.USER_LIST));
    assertEquals("[\"myhdfsuser\"]", ec.getHostLevelParams().get(ExecutionCommand.KeyNames.USER_LIST));
    assertTrue(ec.getHostLevelParams().containsKey(ExecutionCommand.KeyNames.GROUP_LIST));
    assertEquals("[\"myhdfsgroup\"]", ec.getHostLevelParams().get(ExecutionCommand.KeyNames.GROUP_LIST));
    assertTrue(ec.getHostLevelParams().containsKey(ExecutionCommand.KeyNames.USER_GROUPS));
    assertEquals("{\"myhdfsuser\":[\"myhdfsgroup\"]}", ec.getHostLevelParams().get(ExecutionCommand.KeyNames.USER_GROUPS));
  }

  @Test
  public void testCreateServiceComponentMultiple() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();
    String cluster2 = getUniqueName();

    clusters.addCluster(cluster1, new StackId("HDP-0.2"));
    clusters.addCluster(cluster2, new StackId("HDP-0.2"));

    Cluster c1 = clusters.getCluster(cluster1);
    StackId stackId = new StackId("HDP-0.2");

    RepositoryVersionEntity repositoryVersion = helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());

    Service s1 = serviceFactory.createNew(c1, "HDFS", repositoryVersion);
    Service s2 = serviceFactory.createNew(c1, "MAPREDUCE", repositoryVersion);
    c1.addService(s1);
    c1.addService(s2);

    Set<ServiceComponentRequest> set1 = new HashSet<>();
    ServiceComponentRequest valid1 =
        new ServiceComponentRequest(cluster1, "HDFS", "NAMENODE", null);
    ServiceComponentRequest valid2 =
        new ServiceComponentRequest(cluster1, "MAPREDUCE", "JOBTRACKER", null);
    ServiceComponentRequest valid3 =
        new ServiceComponentRequest(cluster1, "MAPREDUCE", "TASKTRACKER", null);
    set1.add(valid1);
    set1.add(valid2);
    set1.add(valid3);
    ComponentResourceProviderTest.createComponents(controller, set1);

    Assert.assertEquals(1, c1.getService("HDFS").getServiceComponents().size());
    Assert.assertEquals(2, c1.getService("MAPREDUCE").getServiceComponents().size());
    Assert.assertNotNull(c1.getService("HDFS")
        .getServiceComponent("NAMENODE"));
    Assert.assertNotNull(c1.getService("MAPREDUCE")
        .getServiceComponent("JOBTRACKER"));
    Assert.assertNotNull(c1.getService("MAPREDUCE")
        .getServiceComponent("TASKTRACKER"));
  }

  @Test
  public void testCreateServiceComponentHostSimple1() throws Exception {
    String cluster1 = getUniqueName();
    String host1 = getUniqueName();
    String host2 = getUniqueName();
    createServiceComponentHostSimple(cluster1, host1, host2);
  }

  private void createServiceComponentHostSimple(String clusterName, String host1,
      String host2) throws Exception, AuthorizationException {

    createCluster(clusterName);
    clusters.getCluster(clusterName)
        .setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "HDFS";
    createService(clusterName, serviceName, repositoryVersion01, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(clusterName, serviceName, componentName1,
        State.INIT);
    createServiceComponent(clusterName, serviceName, componentName2,
        State.INIT);
    createServiceComponent(clusterName, serviceName, componentName3,
        State.INIT);

    try {
      createServiceComponentHost(clusterName, serviceName, componentName1,
          host1, State.INIT);
      fail("ServiceComponentHost creation should fail for invalid host"
          + " as host not mapped to cluster");
    } catch (Exception e) {
      // Expected
    }

    addHostToCluster(host1, clusterName);
    addHostToCluster(host2, clusterName);

    try {
      createServiceComponentHost(clusterName, serviceName, componentName1,
          host1, State.INSTALLING);
      fail("ServiceComponentHost creation should fail for invalid state");
    } catch (Exception e) {
      // Expected
    }

    try {
      clusters.getCluster(clusterName).getService(serviceName)
          .getServiceComponent(componentName1).getServiceComponentHost(host1);
      fail("ServiceComponentHost creation should have failed earlier");
    } catch (Exception e) {
      // Expected
    }

    // null service should work
    createServiceComponentHost(clusterName, null, componentName1,
        host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
        host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName2,
        host2, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
        host1, null);
    createServiceComponentHost(clusterName, serviceName, componentName3,
        host2, null);

    try {
      createServiceComponentHost(clusterName, serviceName, componentName1,
          host1, null);
      fail("ServiceComponentHost creation should fail as duplicate");
    } catch (Exception e) {
      // Expected
    }

    Assert.assertNotNull(clusters.getCluster(clusterName)
        .getService(serviceName)
        .getServiceComponent(componentName1)
        .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(clusterName)
        .getService(serviceName)
        .getServiceComponent(componentName2)
        .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(clusterName)
        .getService(serviceName)
        .getServiceComponent(componentName2)
        .getServiceComponentHost(host2));
    Assert.assertNotNull(clusters.getCluster(clusterName)
        .getService(serviceName)
        .getServiceComponent(componentName3)
        .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(clusterName)
        .getService(serviceName)
        .getServiceComponent(componentName3)
        .getServiceComponentHost(host2));

    ServiceComponentHostRequest r =
        new ServiceComponentHostRequest(clusterName, serviceName,
            componentName2, null, null);

    Set<ServiceComponentHostResponse> response =
        controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(2, response.size());

  }

  @Test
  public void testCreateServiceComponentHostMultiple()
      throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    createServiceComponent(cluster1, serviceName, componentName1,
        State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2,
        State.INIT);
    String host1 = getUniqueName();
    String host2 = getUniqueName();
    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);

    Set<ServiceComponentHostRequest> set1 =
      new HashSet<>();
    ServiceComponentHostRequest r1 =
        new ServiceComponentHostRequest(cluster1, serviceName,
            componentName1, host1, State.INIT.toString());
    ServiceComponentHostRequest r2 =
        new ServiceComponentHostRequest(cluster1, serviceName,
            componentName2, host1, State.INIT.toString());
    ServiceComponentHostRequest r3 =
        new ServiceComponentHostRequest(cluster1, serviceName,
            componentName1, host2, State.INIT.toString());
    ServiceComponentHostRequest r4 =
        new ServiceComponentHostRequest(cluster1, serviceName,
            componentName2, host2, State.INIT.toString());

    set1.add(r1);
    set1.add(r2);
    set1.add(r3);
    set1.add(r4);
    controller.createHostComponents(set1);

    Assert.assertEquals(2,
      clusters.getCluster(cluster1).getServiceComponentHosts(host1).size());
    Assert.assertEquals(2,
      clusters.getCluster(cluster1).getServiceComponentHosts(host2).size());

    Assert.assertNotNull(clusters.getCluster(cluster1)
        .getService(serviceName).getServiceComponent(componentName1)
        .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(cluster1)
        .getService(serviceName).getServiceComponent(componentName1)
        .getServiceComponentHost(host2));
    Assert.assertNotNull(clusters.getCluster(cluster1)
        .getService(serviceName).getServiceComponent(componentName2)
        .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(cluster1)
        .getService(serviceName).getServiceComponent(componentName2)
        .getServiceComponentHost(host2));
  }

  @Test(expected=AmbariException.class)
  public void testCreateServiceComponentHostExclusiveAmbariException()
      throws Exception {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "EXCLUSIVE_DEPENDENCY_COMPONENT";
    createServiceComponent(cluster1, serviceName, componentName1,
        State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2,
        State.INIT);
    createServiceComponent(cluster1, serviceName, componentName3,
        State.INIT);
    String host1 = getUniqueName();
    String host2 = getUniqueName();
    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);

    Set<ServiceComponentHostRequest> set1 =
      new HashSet<>();
    ServiceComponentHostRequest r1 =
        new ServiceComponentHostRequest(cluster1, serviceName,
            componentName1, host1, State.INIT.toString());
    ServiceComponentHostRequest r2 =
        new ServiceComponentHostRequest(cluster1, serviceName,
            componentName3, host1, State.INIT.toString());
    ServiceComponentHostRequest r3 =
        new ServiceComponentHostRequest(cluster1, serviceName,
            componentName2, host1, State.INIT.toString());

    set1.add(r1);
    set1.add(r2);
    set1.add(r3);

    controller.createHostComponents(set1);
  }

  @Test
  public void testCreateServiceComponentHostWithInvalidRequest()
      throws Exception, AuthorizationException {
    // multiple clusters
    // dup objects
    // existing components
    // invalid request params
    // invalid service
    // invalid cluster
    // invalid component
    // invalid host

    Set<ServiceComponentHostRequest> set1 =
      new HashSet<>();

    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid =
          new ServiceComponentHostRequest(null, null, null, null, null);
      set1.add(rInvalid);
      controller.createHostComponents(set1);
      fail("Expected failure for invalid requests");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid =
          new ServiceComponentHostRequest("foo", null, null, null, null);
      set1.add(rInvalid);
      controller.createHostComponents(set1);
      fail("Expected failure for invalid requests");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid =
          new ServiceComponentHostRequest("foo", "HDFS", null, null, null);
      set1.add(rInvalid);
      controller.createHostComponents(set1);
      fail("Expected failure for invalid requests");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid =
          new ServiceComponentHostRequest("foo", "HDFS", "NAMENODE", null, null);
      set1.add(rInvalid);
      controller.createHostComponents(set1);
      fail("Expected failure for invalid requests");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    String host1 = getUniqueName();
    String host2 = getUniqueName();
    String host3 = getUniqueName();

    String clusterFoo = getUniqueName();
    String cluster1 = getUniqueName();
    String cluster2 = getUniqueName();

    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid =
          new ServiceComponentHostRequest(clusterFoo, "HDFS", "NAMENODE", host1, null);
      set1.add(rInvalid);
      controller.createHostComponents(set1);
      fail("Expected failure for invalid cluster");
    } catch (ParentObjectNotFoundException e) {
      // Expected
    }

    clusters.addCluster(clusterFoo, new StackId("HDP-0.2"));
    clusters.addCluster(cluster1, new StackId("HDP-0.2"));
    clusters.addCluster(cluster2, new StackId("HDP-0.2"));
    Cluster foo = clusters.getCluster(clusterFoo);
    Cluster c1 = clusters.getCluster(cluster1);
    Cluster c2 = clusters.getCluster(cluster2);




    StackId stackId = new StackId("HDP-0.2");
    RepositoryVersionEntity repositoryVersion = helper.getOrCreateRepositoryVersion(stackId,
        stackId.getStackVersion());

    foo.setDesiredStackVersion(stackId);
    foo.setCurrentStackVersion(stackId);

    stackId = new StackId("HDP-0.2");
    c1.setDesiredStackVersion(stackId);
    c1.setCurrentStackVersion(stackId);

    stackId = new StackId("HDP-0.2");
    c2.setDesiredStackVersion(stackId);
    c2.setCurrentStackVersion(stackId);

    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid =
          new ServiceComponentHostRequest(clusterFoo, "HDFS", "NAMENODE", host1, null);
      set1.add(rInvalid);
      controller.createHostComponents(set1);
      fail("Expected failure for invalid service");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    Service s1 = serviceFactory.createNew(foo, "HDFS", repositoryVersion);
    foo.addService(s1);
    Service s2 = serviceFactory.createNew(c1, "HDFS", repositoryVersion);
    c1.addService(s2);
    Service s3 = serviceFactory.createNew(c2, "HDFS", repositoryVersion);
    c2.addService(s3);


    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid =
          new ServiceComponentHostRequest(clusterFoo, "HDFS", "NAMENODE", host1, null);
      set1.add(rInvalid);
      controller.createHostComponents(set1);
      fail("Expected failure for invalid service");
    } catch (Exception e) {
      // Expected
    }

    ServiceComponent sc1 = serviceComponentFactory.createNew(s1, "NAMENODE");
    s1.addServiceComponent(sc1);
    ServiceComponent sc2 = serviceComponentFactory.createNew(s2, "NAMENODE");
    s2.addServiceComponent(sc2);
    ServiceComponent sc3 = serviceComponentFactory.createNew(s3, "NAMENODE");
    s3.addServiceComponent(sc3);


    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid =
          new ServiceComponentHostRequest(clusterFoo, "HDFS", "NAMENODE", host1, null);
      set1.add(rInvalid);
      controller.createHostComponents(set1);
      fail("Expected failure for invalid host");
    } catch (Exception e) {
      // Expected
    }

    clusters.addHost(host1);
    Host h1 = clusters.getHost(host1);
    h1.setIPv4("ipv41");
    h1.setIPv6("ipv61");
    setOsFamily(h1, "redhat", "6.3");
    clusters.addHost(host2);
    Host h2 = clusters.getHost(host2);
    h2.setIPv4("ipv42");
    h2.setIPv6("ipv62");
    setOsFamily(h2, "redhat", "6.3");
    clusters.addHost(host3);
    Host h3 = clusters.getHost(host3);
    h3.setIPv4("ipv43");
    h3.setIPv6("ipv63");
    setOsFamily(h3, "redhat", "6.3");

    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid =
          new ServiceComponentHostRequest(clusterFoo, "HDFS", "NAMENODE", host1, null);
      set1.add(rInvalid);
      controller.createHostComponents(set1);
      fail("Expected failure for invalid host cluster mapping");
    } catch (Exception e) {
      // Expected
    }

    Set<String> hostnames = new HashSet<>();
    hostnames.add(host1);
    hostnames.add(host2);
    hostnames.add(host3);
    clusters.mapAndPublishHostsToCluster(hostnames, clusterFoo);
    clusters.mapAndPublishHostsToCluster(hostnames, cluster1);
    clusters.mapAndPublishHostsToCluster(hostnames, cluster2);
    clusters.updateHostMappings(clusters.getHost(host1));
    clusters.updateHostMappings(clusters.getHost(host2));
    clusters.updateHostMappings(clusters.getHost(host3));
    set1.clear();
    ServiceComponentHostRequest valid =
        new ServiceComponentHostRequest(clusterFoo, "HDFS", "NAMENODE", host1, null);
    set1.add(valid);
    controller.createHostComponents(set1);

    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid1 =
          new ServiceComponentHostRequest(clusterFoo, "HDFS", "NAMENODE", host2, null);
      ServiceComponentHostRequest rInvalid2 =
          new ServiceComponentHostRequest(clusterFoo, "HDFS", "NAMENODE", host2, null);
      set1.add(rInvalid1);
      set1.add(rInvalid2);
      controller.createHostComponents(set1);
      fail("Expected failure for dup requests");
    } catch (DuplicateResourceException e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid1 =
          new ServiceComponentHostRequest(cluster1, "HDFS", "NAMENODE", host2,
              null);
      ServiceComponentHostRequest rInvalid2 =
          new ServiceComponentHostRequest(cluster2, "HDFS", "NAMENODE", host3,
              null);
      set1.add(rInvalid1);
      set1.add(rInvalid2);
      controller.createHostComponents(set1);
      fail("Expected failure for multiple clusters");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      set1.clear();
      ServiceComponentHostRequest rInvalid1 =
          new ServiceComponentHostRequest(clusterFoo, "HDFS", "NAMENODE", host1,
              null);
      ServiceComponentHostRequest rInvalid2 =
          new ServiceComponentHostRequest(clusterFoo, "HDFS", "NAMENODE", host2,
              null);
      set1.add(rInvalid1);
      set1.add(rInvalid2);
      controller.createHostComponents(set1);
      fail("Expected failure for already existing");
    } catch (DuplicateResourceException e) {
      // Expected
    }

    Assert.assertEquals(1, foo.getServiceComponentHosts(host1).size());
    Assert.assertEquals(0, foo.getServiceComponentHosts(host2).size());
    Assert.assertEquals(0, foo.getServiceComponentHosts(host3).size());

    set1.clear();
    ServiceComponentHostRequest valid1 =
        new ServiceComponentHostRequest(cluster1, "HDFS", "NAMENODE", host1,
            null);
    set1.add(valid1);
    controller.createHostComponents(set1);

    set1.clear();
    ServiceComponentHostRequest valid2 =
        new ServiceComponentHostRequest(cluster2, "HDFS", "NAMENODE", host1,
            null);
    set1.add(valid2);
    controller.createHostComponents(set1);

    Assert.assertEquals(1, foo.getServiceComponentHosts(host1).size());
    Assert.assertEquals(1, c1.getServiceComponentHosts(host1).size());
    Assert.assertEquals(1, c2.getServiceComponentHosts(host1).size());

  }

  @Test
  public void testCreateHostSimple() throws Exception {
    String cluster1 = getUniqueName();
    String host1 = getUniqueName();
    String host2 = getUniqueName();


    HostRequest r1 = new HostRequest(host1, null);
    r1.toString();

    Set<HostRequest> requests = new HashSet<>();
    requests.add(r1);
    try {
      HostResourceProviderTest.createHosts(controller, requests);
      fail("Create host should fail for non-bootstrapped host");
    } catch (Exception e) {
      // Expected
    }

    clusters.addHost(host1);
    clusters.addHost(host2);
    setOsFamily(clusters.getHost(host1), "redhat", "5.9");
    setOsFamily(clusters.getHost(host2), "redhat", "5.9");

    HostRequest request = new HostRequest(host2, "foo");
    requests.add(request);

    try {
      HostResourceProviderTest.createHosts(controller, requests);
      fail("Create host should fail for invalid clusters");
    } catch (Exception e) {
      // Expected
    }

    request.setClusterName(cluster1);

    clusters.addCluster(cluster1, new StackId("HDP-0.1"));
    Cluster c = clusters.getCluster(cluster1);
    StackId stackId = new StackId("HDP-0.1");
    c.setDesiredStackVersion(stackId);
    c.setCurrentStackVersion(stackId);
    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());

    HostResourceProviderTest.createHosts(controller, requests);

    Assert.assertNotNull(clusters.getHost(host1));
    Assert.assertNotNull(clusters.getHost(host2));

    Assert.assertEquals(0, clusters.getClustersForHost(host1).size());
    Assert.assertEquals(1, clusters.getClustersForHost(host2).size());

  }

  @Test
  public void testCreateHostMultiple() throws Exception {
    String host1 = getUniqueName();
    String host2 = getUniqueName();
    String host3 = getUniqueName();
    String cluster1 = getUniqueName();

    clusters.addHost(host1);
    clusters.addHost(host2);
    clusters.addHost(host3);
    clusters.addCluster(cluster1, new StackId("HDP-0.1"));
    Cluster c = clusters.getCluster(cluster1);
    StackId stackID = new StackId("HDP-0.1");
    c.setDesiredStackVersion(stackID);
    c.setCurrentStackVersion(stackID);
    helper.getOrCreateRepositoryVersion(stackID, stackID.getStackVersion());

    setOsFamily(clusters.getHost(host1), "redhat", "5.9");
    setOsFamily(clusters.getHost(host2), "redhat", "5.9");
    setOsFamily(clusters.getHost(host3), "redhat", "5.9");

    HostRequest r1 = new HostRequest(host1, cluster1);
    HostRequest r2 = new HostRequest(host2, cluster1);
    HostRequest r3 = new HostRequest(host3, null);

    Set<HostRequest> set1 = new HashSet<>();
    set1.add(r1);
    set1.add(r2);
    set1.add(r3);
    HostResourceProviderTest.createHosts(controller, set1);

    Assert.assertEquals(1, clusters.getClustersForHost(host1).size());
    Assert.assertEquals(1, clusters.getClustersForHost(host2).size());
    Assert.assertEquals(0, clusters.getClustersForHost(host3).size());
  }

  @Test
  public void testCreateHostWithInvalidRequests() throws Exception {
    // unknown host
    // invalid clusters
    // duplicate host
    String host1 = getUniqueName();
    String cluster1 = getUniqueName();

    Set<HostRequest> set1 = new HashSet<>();

    try {
      set1.clear();
      HostRequest rInvalid =
          new HostRequest(host1, null);
      set1.add(rInvalid);
      HostResourceProviderTest.createHosts(controller, set1);
      fail("Expected failure for invalid host");
    } catch (Exception e) {
      // Expected
    }

    clusters.addHost(host1);

    try {
      set1.clear();
      HostRequest rInvalid =
          new HostRequest(host1, cluster1);
      set1.add(rInvalid);
      HostResourceProviderTest.createHosts(controller, set1);
      fail("Expected failure for invalid cluster");
    } catch (Exception e) {
      // Expected
    }

    clusters.addCluster(cluster1, new StackId("HDP-0.1"));

    try {
      set1.clear();
      HostRequest rInvalid1 =
          new HostRequest(host1, cluster1);
      rInvalid1.setRackInfo(UUID.randomUUID().toString());
      HostRequest rInvalid2 =
          new HostRequest(host1, cluster1);
      set1.add(rInvalid1);
      set1.add(rInvalid2);
      HostResourceProviderTest.createHosts(controller, set1);
      fail("Expected failure for dup requests");
    } catch (Exception e) {
      // Expected
    }
  }

  /**
   * Create a cluster with a service, and verify that the request tasks have the correct output log and error log paths.
   */
  @Test
  public void testRequestStatusLogs() throws Exception {
    String cluster1 = getUniqueName();

    createServiceComponentHostSimple(cluster1, getUniqueName(), getUniqueName());

    String serviceName = "HDFS";

    Cluster cluster = clusters.getCluster(cluster1);
    for (Host h : clusters.getHosts()) {
      // Simulate each agent registering and setting the prefix path on its host
      h.setPrefix(Configuration.PREFIX_DIR);
    }

    Map<String, Config> configs = new HashMap<>();
    Map<String, String> properties = new HashMap<>();
    Map<String, Map<String, String>> propertiesAttributes = new HashMap<>();

    ConfigFactory configFactory = injector.getInstance(ConfigFactory.class);
    Config c1 = configFactory.createNew(cluster, "hdfs-site", "v1",  properties, propertiesAttributes);
    configs.put(c1.getType(), c1);

    ServiceRequest r = new ServiceRequest(cluster1, serviceName, repositoryVersion02.getId(),
        State.INSTALLED.toString(), null);

    Set<ServiceRequest> requests = new HashSet<>();
    requests.add(r);

    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");

    RequestStatusResponse trackAction =
        ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true, false);

    List<ShortTaskStatus> taskStatuses = trackAction.getTasks();
    Assert.assertFalse(taskStatuses.isEmpty());
    for (ShortTaskStatus task : taskStatuses) {
      Assert.assertEquals("Task output logs don't match", Configuration.PREFIX_DIR + "/output-" + task.getTaskId() + ".txt", task.getOutputLog());
      Assert.assertEquals("Task error logs don't match", Configuration.PREFIX_DIR + "/errors-" + task.getTaskId() + ".txt", task.getErrorLog());
    }
  }

  @Test
  public void testInstallAndStartService() throws Exception {
    String cluster1 = getUniqueName();
    String host1 = getUniqueName();
    String host2 = getUniqueName();

    createServiceComponentHostSimple(cluster1, host1, host2);

    String serviceName = "HDFS";

    Cluster cluster = clusters.getCluster(cluster1);

    Map<String, Config> configs = new HashMap<>();
    Map<String, String> properties = new HashMap<>();
    Map<String, Map<String, String>> propertiesAttributes = new HashMap<>();
    properties.put("a", "a1");
    properties.put("b", "b1");

    ConfigFactory configFactory = injector.getInstance(ConfigFactory.class);
    Config c1 = configFactory.createNew(cluster, "hdfs-site", "v1", properties, propertiesAttributes);
    properties.put("c", cluster1);
    properties.put("d", "d1");

    Config c2 = configFactory.createNew(cluster, "core-site", "v1", properties, propertiesAttributes);
    configFactory.createNew(cluster, "foo-site", "v1", properties, propertiesAttributes);

    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");

    configs.put(c1.getType(), c1);
    configs.put(c2.getType(), c2);

    ServiceRequest r = new ServiceRequest(cluster1, serviceName, repositoryVersion02.getId(),
        State.INSTALLED.toString(), null);

    Set<ServiceRequest> requests = new HashSet<>();
    requests.add(r);

    RequestStatusResponse trackAction =
        ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true, false);
    Assert.assertEquals(State.INSTALLED,
        clusters.getCluster(cluster1).getService(serviceName)
        .getDesiredState());
    for (ServiceComponent sc :
      clusters.getCluster(cluster1).getService(serviceName)
      .getServiceComponents().values()) {
      Assert.assertEquals(State.INSTALLED, sc.getDesiredState());
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        Assert.assertEquals(State.INSTALLED, sch.getDesiredState());
        Assert.assertEquals(State.INIT, sch.getState());
      }
    }

    List<ShortTaskStatus> taskStatuses = trackAction.getTasks();
    Assert.assertEquals(5, taskStatuses.size());

    boolean foundH1NN = false;
    boolean foundH1DN = false;
    boolean foundH2DN = false;
    boolean foundH1CLT = false;
    boolean foundH2CLT = false;

    for (ShortTaskStatus taskStatus : taskStatuses) {
      LOG.debug("Task dump :{}", taskStatus);
      Assert.assertEquals(RoleCommand.INSTALL.toString(),
          taskStatus.getCommand());
      Assert.assertEquals(HostRoleStatus.PENDING.toString(),
          taskStatus.getStatus());
      if (taskStatus.getHostName().equals(host1)) {
        if (Role.NAMENODE.toString().equals(taskStatus.getRole())) {
          foundH1NN = true;
        } else if (Role.DATANODE.toString().equals(taskStatus.getRole())) {
          foundH1DN = true;
        } else if (Role.HDFS_CLIENT.toString().equals(taskStatus.getRole())) {
          foundH1CLT = true;
        } else {
          fail("Found invalid role for host h1");
        }
      } else if (taskStatus.getHostName().equals(host2)) {
        if (Role.DATANODE.toString().equals(taskStatus.getRole())) {
          foundH2DN = true;
        } else if (Role.HDFS_CLIENT.toString().equals(taskStatus.getRole())) {
          foundH2CLT = true;
        } else {
          fail("Found invalid role for host h2");
        }
      } else {
        fail("Found invalid host in task list");
      }
    }
    Assert.assertTrue(foundH1DN && foundH1NN && foundH2DN
        && foundH1CLT && foundH2CLT);

    // TODO validate stages?
    List<Stage> stages = actionDB.getAllStages(trackAction.getRequestId());
    Assert.assertEquals(1, stages.size());

    for (Stage stage : stages) {
      LOG.info("Stage Details for Install Service"
          + ", stageId="+ stage.getStageId()
          + ", actionId=" + stage.getActionId());

      for (String host : stage.getHosts()) {
        for (ExecutionCommandWrapper ecw : stage.getExecutionCommands(host)) {
          Assert.assertNotNull(ecw.getExecutionCommand().getRepositoryFile());
        }
      }
    }

    org.apache.ambari.server.controller.spi.Request request = PropertyHelper.getReadRequest(
        TaskResourceProvider.TASK_CLUSTER_NAME_PROPERTY_ID,
        TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID,
        TaskResourceProvider.TASK_STAGE_ID_PROPERTY_ID);

    Predicate predicate = new PredicateBuilder().property(
        TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID).equals(
            trackAction.getRequestId()).toPredicate();

    List<HostRoleCommandEntity> entities = hostRoleCommandDAO.findAll(request, predicate);
    Assert.assertEquals(5, entities.size());

    // !!! pick any entity to make sure a request brings back only one
    Long taskId = entities.get(0).getTaskId();
    predicate = new PredicateBuilder().property(
        TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID).equals(
            trackAction.getRequestId()).and().property(
                TaskResourceProvider.TASK_ID_PROPERTY_ID).equals(taskId).toPredicate();

    entities = hostRoleCommandDAO.findAll(request, predicate);
    Assert.assertEquals(1, entities.size());

    // manually change live state to installed as no running action manager
    for (ServiceComponent sc :
      clusters.getCluster(cluster1).getService(serviceName)
      .getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        sch.setState(State.INSTALLED);
      }
    }

    r = new ServiceRequest(cluster1, serviceName, repositoryVersion02.getId(), State.STARTED.toString(),
        null);
    requests.clear();
    requests.add(r);
    trackAction = ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true,
      false);

    Assert.assertEquals(State.STARTED,
        clusters.getCluster(cluster1).getService(serviceName)
        .getDesiredState());
    for (ServiceComponent sc :
      clusters.getCluster(cluster1).getService(serviceName)
      .getServiceComponents().values()) {
      if (sc.getName().equals("HDFS_CLIENT")) {
        Assert.assertEquals(State.INSTALLED, sc.getDesiredState());
      } else {
        Assert.assertEquals(State.STARTED, sc.getDesiredState());
      }
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        if (sch.getServiceComponentName().equals("HDFS_CLIENT")) {
          Assert.assertEquals(State.INSTALLED, sch.getDesiredState());
        } else {
          Assert.assertEquals(State.STARTED, sch.getDesiredState());
        }
      }
    }

    // TODO validate stages?
    stages = actionDB.getAllStages(trackAction.getRequestId());
    Assert.assertEquals(2, stages.size());

    StringBuilder sb = new StringBuilder();
    clusters.debugDump(sb);
    LOG.info("Cluster Dump: " + sb);

    for (ServiceComponent sc :
      clusters.getCluster(cluster1).getService(serviceName)
      .getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        if (sc.isClientComponent()) {
          sch.setState(State.INSTALLED);
        } else {
          sch.setState(State.INSTALL_FAILED);
        }
      }
    }

    r = new ServiceRequest(cluster1, serviceName, repositoryVersion02.getId(), State.INSTALLED.toString(),
        null);
    requests.clear();
    requests.add(r);
    trackAction = ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true,
      false);

    Assert.assertEquals(State.INSTALLED,
        clusters.getCluster(cluster1).getService(serviceName)
        .getDesiredState());
    for (ServiceComponent sc :
      clusters.getCluster(cluster1).getService(serviceName)
      .getServiceComponents().values()) {
      Assert.assertEquals(State.INSTALLED, sc.getDesiredState());
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        Assert.assertEquals(State.INSTALLED, sch.getDesiredState());
      }
    }

    // TODO validate stages?
    stages = actionDB.getAllStages(trackAction.getRequestId());

    Assert.assertEquals(1, stages.size());

  }

  @Test
  public void testGetClusters() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();

    clusters.addCluster(cluster1, new StackId("HDP-0.1"));

    Cluster c1 = clusters.getCluster(cluster1);

    StackId stackId = new StackId("HDP-0.1");
    c1.setDesiredStackVersion(stackId);
    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());

    ClusterRequest r = new ClusterRequest(null, null, null, null);
    Set<ClusterResponse> resp = controller.getClusters(Collections.singleton(r));

    // !!! many tests are creating clusters, just make sure we have at least one
    Assert.assertFalse(resp.isEmpty());

    boolean found = false;
    for (ClusterResponse cr : resp) {
      if (cr.getClusterName().equals(cluster1)) {
        Assert.assertEquals(c1.getClusterId(), cr.getClusterId());
        Assert.assertEquals(c1.getDesiredStackVersion().getStackId(), cr.getDesiredStackVersion());
        found = true;
        break;
      }
    }

    Assert.assertTrue(found);
  }

  @Test
  public void testGetClustersWithFilters() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();
    String cluster2 = getUniqueName();
    String cluster3 = getUniqueName();
    String cluster4 = getUniqueName();

    clusters.addCluster(cluster1, new StackId("HDP-0.1"));
    clusters.addCluster(cluster2, new StackId("HDP-0.1"));
    clusters.addCluster(cluster3, new StackId("HDP-1.2.0"));
    clusters.addCluster(cluster4, new StackId("HDP-0.1"));

    ClusterRequest r = new ClusterRequest(null, null, null, null);
    Set<ClusterResponse> resp = controller.getClusters(Collections.singleton(r));
    Assert.assertTrue(resp.size() >= 4);

    r = new ClusterRequest(null, cluster1, null, null);
    resp = controller.getClusters(Collections.singleton(r));
    Assert.assertEquals(1, resp.size());
    Cluster c1 = clusters.getCluster(cluster1);
    Assert.assertEquals(c1.getClusterId(), resp.iterator().next().getClusterId());

    r = new ClusterRequest(null, null, "HDP-0.1", null);
    resp = controller.getClusters(Collections.singleton(r));
    Assert.assertTrue(resp.size() >= 3);

    r = new ClusterRequest(null, null, null, null);
    resp = controller.getClusters(Collections.singleton(r));
    Assert.assertTrue("Stack ID request is invalid and expect them all", resp.size() > 3);
  }

  @Test
  public void testGetServices() throws Exception {
    String cluster1 = getUniqueName();

    StackId stackId = new StackId("HDP-0.1");
    RepositoryVersionEntity repositoryVersion = helper.getOrCreateRepositoryVersion(stackId,
        stackId.getStackVersion());

    clusters.addCluster(cluster1, stackId);
    Cluster c1 = clusters.getCluster(cluster1);
    Service s1 = serviceFactory.createNew(c1, "HDFS", repositoryVersion);

    c1.addService(s1);
    s1.setDesiredState(State.INSTALLED);

    ServiceRequest r = new ServiceRequest(cluster1, null, null, null, null);
    Set<ServiceResponse> resp = ServiceResourceProviderTest.getServices(controller, Collections.singleton(r));

    ServiceResponse resp1 = resp.iterator().next();

    Assert.assertEquals(s1.getClusterId(), resp1.getClusterId().longValue());
    Assert.assertEquals(s1.getCluster().getClusterName(), resp1.getClusterName());
    Assert.assertEquals(s1.getName(), resp1.getServiceName());
    Assert.assertEquals("HDP-0.1", s1.getDesiredStackId().getStackId());
    Assert.assertEquals(s1.getDesiredStackId().getStackId(), resp1.getDesiredStackId());
    Assert.assertEquals(State.INSTALLED.toString(), resp1.getDesiredState());

  }

  @Test
  public void testGetServicesWithFilters() throws Exception {
    String cluster1 = getUniqueName();
    String cluster2 = getUniqueName();

    StackId stackId = new StackId("HDP-0.2");
    RepositoryVersionEntity repositoryVersion = helper.getOrCreateRepositoryVersion(stackId,
        stackId.getStackVersion());

    clusters.addCluster(cluster1, stackId);
    clusters.addCluster(cluster2, stackId);
    Cluster c1 = clusters.getCluster(cluster1);
    Cluster c2 = clusters.getCluster(cluster2);
    c1.setDesiredStackVersion(stackId);
    c2.setDesiredStackVersion(stackId);

    Service s1 = serviceFactory.createNew(c1, "HDFS", repositoryVersion);
    Service s2 = serviceFactory.createNew(c1, "MAPREDUCE", repositoryVersion);
    Service s3 = serviceFactory.createNew(c1, "HBASE", repositoryVersion);
    Service s4 = serviceFactory.createNew(c2, "HIVE", repositoryVersion);
    Service s5 = serviceFactory.createNew(c2, "ZOOKEEPER", repositoryVersion);

    c1.addService(s1);
    c1.addService(s2);
    c1.addService(s3);
    c2.addService(s4);
    c2.addService(s5);

    s1.setDesiredState(State.INSTALLED);
    s2.setDesiredState(State.INSTALLED);
    s4.setDesiredState(State.INSTALLED);

    ServiceRequest r = new ServiceRequest(null, null, null, null, null);
    Set<ServiceResponse> resp;

    try {
      ServiceResourceProviderTest.getServices(controller, Collections.singleton(r));
      fail("Expected failure for invalid request");
    } catch (Exception e) {
      // Expected
    }

    r = new ServiceRequest(c1.getClusterName(), null, null, null, null);
    resp = ServiceResourceProviderTest.getServices(controller, Collections.singleton(r));
    Assert.assertEquals(3, resp.size());

    r = new ServiceRequest(c1.getClusterName(), s2.getName(), null, null, null);
    resp = ServiceResourceProviderTest.getServices(controller, Collections.singleton(r));
    Assert.assertEquals(1, resp.size());
    Assert.assertEquals(s2.getName(), resp.iterator().next().getServiceName());

    try {
      r = new ServiceRequest(c2.getClusterName(), s1.getName(), null, null, null);
      ServiceResourceProviderTest.getServices(controller, Collections.singleton(r));
      fail("Expected failure for invalid service");
    } catch (Exception e) {
      // Expected
    }

    r = new ServiceRequest(c1.getClusterName(), null, null, "INSTALLED", null);
    resp = ServiceResourceProviderTest.getServices(controller, Collections.singleton(r));
    Assert.assertEquals(2, resp.size());

    r = new ServiceRequest(c2.getClusterName(), null, null, "INIT", null);
    resp = ServiceResourceProviderTest.getServices(controller, Collections.singleton(r));
    Assert.assertEquals(1, resp.size());

    ServiceRequest r1, r2, r3;
    r1 = new ServiceRequest(c1.getClusterName(), null, null, "INSTALLED", null);
    r2 = new ServiceRequest(c2.getClusterName(), null, null, "INIT", null);
    r3 = new ServiceRequest(c2.getClusterName(), null, null, "INIT", null);

    Set<ServiceRequest> reqs = new HashSet<>();
    reqs.addAll(Arrays.asList(r1, r2, r3));
    resp = ServiceResourceProviderTest.getServices(controller, reqs);
    Assert.assertEquals(3, resp.size());

  }


  @Test
  public void testGetServiceComponents() throws Exception {
    String cluster1 = getUniqueName();

    StackId stackId = new StackId("HDP-0.2");
    RepositoryVersionEntity repositoryVersion = helper.getOrCreateRepositoryVersion(stackId,
        stackId.getStackVersion());

    clusters.addCluster(cluster1, stackId);
    Cluster c1 = clusters.getCluster(cluster1);
    c1.setDesiredStackVersion(stackId);
    Service s1 = serviceFactory.createNew(c1, "HDFS", repositoryVersion);
    c1.addService(s1);
    s1.setDesiredState(State.INSTALLED);
    ServiceComponent sc1 = serviceComponentFactory.createNew(s1, "DATANODE");
    s1.addServiceComponent(sc1);
    sc1.setDesiredState(State.UNINSTALLED);

    ServiceComponentRequest r = new ServiceComponentRequest(cluster1,
       s1.getName(), sc1.getName(), null);

    Set<ServiceComponentResponse> resps = ComponentResourceProviderTest.getComponents(controller, Collections.singleton(r));
    Assert.assertEquals(1, resps.size());

    ServiceComponentResponse resp = resps.iterator().next();

    Assert.assertEquals(c1.getClusterName(), resp.getClusterName());
    Assert.assertEquals(sc1.getName(), resp.getComponentName());
    Assert.assertEquals(s1.getName(), resp.getServiceName());
    Assert.assertEquals("HDP-0.2", resp.getDesiredStackId());
    Assert.assertEquals(sc1.getDesiredState().toString(),
        resp.getDesiredState());
    Assert.assertEquals(c1.getClusterId(), resp.getClusterId().longValue());

  }


  @Test
  public void testGetServiceComponentsWithFilters() throws Exception {
    String cluster1 = getUniqueName();
    String cluster2 = getUniqueName();

    StackId stackId = new StackId("HDP-0.2");
    RepositoryVersionEntity repositoryVersion = helper.getOrCreateRepositoryVersion(stackId,
        stackId.getStackVersion());

    clusters.addCluster(cluster1, stackId);
    clusters.addCluster(cluster2, stackId);
    Cluster c1 = clusters.getCluster(cluster1);
    Cluster c2 = clusters.getCluster(cluster2);

    Service s1 = serviceFactory.createNew(c1, "HDFS", repositoryVersion);
    Service s2 = serviceFactory.createNew(c1, "MAPREDUCE", repositoryVersion);
    Service s3 = serviceFactory.createNew(c1, "HBASE", repositoryVersion);
    Service s4 = serviceFactory.createNew(c2, "HIVE", repositoryVersion);
    Service s5 = serviceFactory.createNew(c2, "ZOOKEEPER", repositoryVersion);

    c1.addService(s1);
    c1.addService(s2);
    c1.addService(s3);
    c2.addService(s4);
    c2.addService(s5);

    s1.setDesiredState(State.INSTALLED);
    s2.setDesiredState(State.INSTALLED);
    s4.setDesiredState(State.INSTALLED);

    ServiceComponent sc1 = serviceComponentFactory.createNew(s1, "DATANODE");
    ServiceComponent sc2 = serviceComponentFactory.createNew(s1, "NAMENODE");
    ServiceComponent sc3 = serviceComponentFactory.createNew(s3, "HBASE_REGIONSERVER");
    ServiceComponent sc4 = serviceComponentFactory.createNew(s4, "HIVE_SERVER");
    ServiceComponent sc5 = serviceComponentFactory.createNew(s4, "HIVE_CLIENT");
    ServiceComponent sc6 = serviceComponentFactory.createNew(s4, "MYSQL_SERVER");
    ServiceComponent sc7 = serviceComponentFactory.createNew(s5, "ZOOKEEPER_SERVER");
    ServiceComponent sc8 = serviceComponentFactory.createNew(s5, "ZOOKEEPER_CLIENT");

    s1.addServiceComponent(sc1);
    s1.addServiceComponent(sc2);
    s3.addServiceComponent(sc3);
    s4.addServiceComponent(sc4);
    s4.addServiceComponent(sc5);
    s4.addServiceComponent(sc6);
    s5.addServiceComponent(sc7);
    s5.addServiceComponent(sc8);

    sc1.setDesiredState(State.UNINSTALLED);
    sc3.setDesiredState(State.UNINSTALLED);
    sc5.setDesiredState(State.UNINSTALLED);
    sc6.setDesiredState(State.UNINSTALLED);
    sc7.setDesiredState(State.UNINSTALLED);
    sc8.setDesiredState(State.UNINSTALLED);

    ServiceComponentRequest r = new ServiceComponentRequest(null, null,
        null, null);

    try {
      ComponentResourceProviderTest.getComponents(controller, Collections.singleton(r));
      fail("Expected failure for invalid cluster");
    } catch (Exception e) {
      // Expected
    }

    // all comps per cluster
    r = new ServiceComponentRequest(c1.getClusterName(),
        null, null, null);
    Set<ServiceComponentResponse> resps = ComponentResourceProviderTest.getComponents(controller, Collections.singleton(r));
    Assert.assertEquals(3, resps.size());

    // all comps per cluster filter on state
    r = new ServiceComponentRequest(c2.getClusterName(),
        null, null, State.UNINSTALLED.toString());
    resps = ComponentResourceProviderTest.getComponents(controller, Collections.singleton(r));
    Assert.assertEquals(4, resps.size());

    // all comps for given service
    r = new ServiceComponentRequest(c2.getClusterName(),
        s5.getName(), null, null);
    resps = ComponentResourceProviderTest.getComponents(controller, Collections.singleton(r));
    Assert.assertEquals(2, resps.size());

    // all comps for given service filter by state
    r = new ServiceComponentRequest(c2.getClusterName(),
        s4.getName(), null, State.INIT.toString());
    resps = ComponentResourceProviderTest.getComponents(controller, Collections.singleton(r));
    Assert.assertEquals(1, resps.size());
    Assert.assertEquals(sc4.getName(),
        resps.iterator().next().getComponentName());

    // get single given comp
    r = new ServiceComponentRequest(c2.getClusterName(),
        null, sc5.getName(), State.INIT.toString());
    resps = ComponentResourceProviderTest.getComponents(controller, Collections.singleton(r));
    Assert.assertEquals(1, resps.size());
    Assert.assertEquals(sc5.getName(),
        resps.iterator().next().getComponentName());

    // get single given comp and given svc
    r = new ServiceComponentRequest(c2.getClusterName(),
        s4.getName(), sc5.getName(), State.INIT.toString());
    resps = ComponentResourceProviderTest.getComponents(controller, Collections.singleton(r));
    Assert.assertEquals(1, resps.size());
    Assert.assertEquals(sc5.getName(),
        resps.iterator().next().getComponentName());


    ServiceComponentRequest r1, r2, r3;
    Set<ServiceComponentRequest> reqs = new HashSet<>();
    r1 = new ServiceComponentRequest(c2.getClusterName(),
        null, null, State.UNINSTALLED.toString());
    r2 = new ServiceComponentRequest(c1.getClusterName(),
        null, null, null);
    r3 = new ServiceComponentRequest(c1.getClusterName(),
        null, null, State.INIT.toString());
    reqs.addAll(Arrays.asList(r1, r2, r3));
    resps = ComponentResourceProviderTest.getComponents(controller, reqs);
    Assert.assertEquals(7, resps.size());
  }

  @Test
  public void testGetServiceComponentHosts() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();
    String host1 = getUniqueName();

    Cluster c1 = setupClusterWithHosts(cluster1, "HDP-0.1", Lists.newArrayList(host1), "centos5");
    RepositoryVersionEntity repositoryVersion = repositoryVersion01;

    Service s1 = serviceFactory.createNew(c1, "HDFS", repositoryVersion);
    c1.addService(s1);
    ServiceComponent sc1 = serviceComponentFactory.createNew(s1, "DATANODE");
    s1.addServiceComponent(sc1);
    sc1.setDesiredState(State.UNINSTALLED);
    ServiceComponentHost sch1 = serviceComponentHostFactory.createNew(sc1, host1);
    sc1.addServiceComponentHost(sch1);
    sch1.setDesiredState(State.INSTALLED);
    sch1.setState(State.INSTALLING);

    /*sch1.updateActualConfigs(new HashMap<String, Map<String,String>>() {{
      put("global", new HashMap<String,String>() {{ put("tag", "version1"); }});
    }});*/


    ServiceComponentHostRequest r =
        new ServiceComponentHostRequest(c1.getClusterName(),
            null, null, null, null);
    Set<ServiceComponentHostResponse> resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(1, resps.size());

    ServiceComponentHostResponse resp =
        resps.iterator().next();

    Assert.assertEquals(c1.getClusterName(), resp.getClusterName());
    Assert.assertEquals(sc1.getName(), resp.getComponentName());
    Assert.assertEquals(s1.getName(), resp.getServiceName());
    Assert.assertEquals(sch1.getHostName(), resp.getHostname());
    Assert.assertEquals(sch1.getDesiredState().toString(),
        resp.getDesiredState());
    Assert.assertEquals(sch1.getState().toString(),
        resp.getLiveState());
    Assert.assertEquals(repositoryVersion.getStackId(),
        sch1.getServiceComponent().getDesiredStackId());
  }

  @Test
  @Ignore
  //TODO Should be rewritten after stale configs calculate workflow change.
  public void testGetServiceComponentHostsWithStaleConfigFilter() throws Exception, AuthorizationException {

    final String host1 = getUniqueName();
    final String host2 = getUniqueName();
    String cluster1 = getUniqueName();
    Cluster c = setupClusterWithHosts(cluster1, "HDP-2.0.5",
        new ArrayList<String>() {{
          add(host1);
          add(host2);
        }},
        "centos5");

    Long clusterId = c.getClusterId();
    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(cluster1, serviceName, componentName1,
        State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2,
        State.INIT);
    createServiceComponent(cluster1, serviceName, componentName3,
        State.INIT);

    createServiceComponentHost(cluster1, serviceName, componentName1,
        host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
        host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
        host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
        host2, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
        host2, null);

    // Install
    installService(cluster1, serviceName, false, false);

    // Create and attach config
    Map<String, String> configs = new HashMap<>();
    configs.put("a", "b");

    ConfigurationRequest cr1;
    cr1 = new ConfigurationRequest(cluster1, "hdfs-site", "version1",
        configs, null);
    ClusterRequest crReq = new ClusterRequest(clusterId, cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr1));
    controller.updateClusters(Collections.singleton(crReq), null);

    // Start
    startService(cluster1, serviceName, false, false);

    //Update actual config
    HashMap<String, Map<String, String>> actualConfig = new HashMap<String, Map<String, String>>() {{
      put("hdfs-site", new HashMap<String, String>() {{
        put("tag", "version1");
      }});
    }};
    HashMap<String, Map<String, String>> actualConfigOld = new
        HashMap<String, Map<String, String>>() {{
          put("hdfs-site", new HashMap<String, String>() {{
            put("tag", "version0");
          }});
        }};

    Service s1 = clusters.getCluster(cluster1).getService(serviceName);
    /*s1.getServiceComponent(componentName1).getServiceComponentHost(host1).updateActualConfigs(actualConfig);
    s1.getServiceComponent(componentName2).getServiceComponentHost(host1).updateActualConfigs(actualConfig);
    s1.getServiceComponent(componentName3).getServiceComponentHost(host1).updateActualConfigs(actualConfigOld);
    s1.getServiceComponent(componentName2).getServiceComponentHost(host2).updateActualConfigs(actualConfigOld);
    s1.getServiceComponent(componentName3).getServiceComponentHost(host2).updateActualConfigs(actualConfig);*/

    ServiceComponentHostRequest r =
        new ServiceComponentHostRequest(cluster1, null, null, null, null);
    Set<ServiceComponentHostResponse> resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(5, resps.size());

    //Get all host components with stale config = true
    r = new ServiceComponentHostRequest(cluster1, null, null, null, null);
    r.setStaleConfig("true");
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(2, resps.size());

    //Get all host components with stale config = false
    r = new ServiceComponentHostRequest(cluster1, null, null, null, null);
    r.setStaleConfig("false");
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(3, resps.size());

    //Get all host components with stale config = false and hostname filter
    r = new ServiceComponentHostRequest(cluster1, null, null, host1, null);
    r.setStaleConfig("false");
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(2, resps.size());

    //Get all host components with stale config = false and hostname filter
    r = new ServiceComponentHostRequest(cluster1, null, null, host2, null);
    r.setStaleConfig("true");
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(1, resps.size());
  }

  @Test
  public void testServiceComponentHostsWithDecommissioned() throws Exception {

    final String host1 = getUniqueName();
    final String host2 = getUniqueName();
    String cluster1 = getUniqueName();

    setupClusterWithHosts(cluster1, "HDP-2.0.7",
        new ArrayList<String>() {{
          add(host1);
          add(host2);
        }},
        "centos5");
    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(cluster1, serviceName, componentName1,
        State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2,
        State.INIT);
    createServiceComponent(cluster1, serviceName, componentName3,
        State.INIT);

    createServiceComponentHost(cluster1, serviceName, componentName1,
        host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
        host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
        host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
        host2, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
        host2, null);

    // Install
    installService(cluster1, serviceName, false, false);

    // Start
    startService(cluster1, serviceName, false, false);

    Service s1 = clusters.getCluster(cluster1).getService(serviceName);
    s1.getServiceComponent(componentName2).getServiceComponentHost(host1).
        setComponentAdminState(HostComponentAdminState.DECOMMISSIONED);
    s1.getServiceComponent(componentName2).getServiceComponentHost(host2).
        setComponentAdminState(HostComponentAdminState.INSERVICE);

    ServiceComponentHostRequest r =
        new ServiceComponentHostRequest(cluster1, null, null, null, null);
    Set<ServiceComponentHostResponse> resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(5, resps.size());

    //Get all host components with decommissiond = true
    r = new ServiceComponentHostRequest(cluster1, null, null, null, null);
    r.setAdminState("DECOMMISSIONED");
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(1, resps.size());

    //Get all host components with decommissioned = false
    r = new ServiceComponentHostRequest(cluster1, null, null, null, null);
    r.setAdminState("INSERVICE");
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(1, resps.size());

    //Get all host components with decommissioned = some random string
    r = new ServiceComponentHostRequest(cluster1, null, null, null, null);
    r.setAdminState("INSTALLED");
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(0, resps.size());

    //Update adminState
    r = new ServiceComponentHostRequest(cluster1, "HDFS", "DATANODE", host2, null);
    r.setAdminState("DECOMMISSIONED");
    try {
      updateHostComponents(Collections.singleton(r), new HashMap<>(), false);
      Assert.fail("Must throw exception when decommission attribute is updated.");
    } catch (IllegalArgumentException ex) {
      Assert.assertTrue(ex.getMessage().contains("Property adminState cannot be modified through update"));
    }
  }

  @Test
  public void testHbaseDecommission() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    clusters.getCluster(cluster1).setDesiredStackVersion(new StackId("HDP-2.0.7"));
    String serviceName = "HBASE";
    createService(cluster1, serviceName, repositoryVersion207, null);
    String componentName1 = "HBASE_MASTER";
    String componentName2 = "HBASE_REGIONSERVER";

    createServiceComponent(cluster1, serviceName, componentName1, State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2, State.INIT);

    final String host1 = getUniqueName();
    final String host2 = getUniqueName();

    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);

    createServiceComponentHost(cluster1, serviceName, componentName1, host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName1, host2, null);
    createServiceComponentHost(cluster1, serviceName, componentName2, host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2, host2, null);

    RequestOperationLevel level = new RequestOperationLevel(
            Resource.Type.HostComponent, cluster1, null, null, null);

    // Install
    installService(cluster1, serviceName, false, false);

    // Start
    startService(cluster1, serviceName, false, false);

    Cluster cluster = clusters.getCluster(cluster1);
    Service s = cluster.getService(serviceName);
    Assert.assertEquals(State.STARTED, s.getDesiredState());
    ServiceComponentHost scHost = s.getServiceComponent("HBASE_REGIONSERVER").getServiceComponentHost(host2);
    Assert.assertEquals(HostComponentAdminState.INSERVICE, scHost.getComponentAdminState());

    // Decommission one RS
    Map<String, String> params = new HashMap<String, String>() {{
      put("excluded_hosts", host2);
      put("align_maintenance_state", "true");
    }};
    RequestResourceFilter resourceFilter = new RequestResourceFilter("HBASE", "HBASE_MASTER", null);
    List<RequestResourceFilter> resourceFilters = new ArrayList<>();
    resourceFilters.add(resourceFilter);

    ExecuteActionRequest request = new ExecuteActionRequest(cluster1,
      "DECOMMISSION", null, resourceFilters, level, params, false);

    Map<String, String> requestProperties = new HashMap<>();
    requestProperties.put(REQUEST_CONTEXT_PROPERTY, "Called from a test");

    RequestStatusResponse response = controller.createAction(request,
        requestProperties);

    List<HostRoleCommand> storedTasks = actionDB.getRequestTasks(response.getRequestId());
    ExecutionCommand execCmd = storedTasks.get(0).getExecutionCommandWrapper
        ().getExecutionCommand();
    Assert.assertNotNull(storedTasks);
    Assert.assertEquals(1, storedTasks.size());
    Assert.assertEquals(HostComponentAdminState.DECOMMISSIONED, scHost.getComponentAdminState());
    Assert.assertEquals(MaintenanceState.ON, scHost.getMaintenanceState());
    HostRoleCommand command = storedTasks.get(0);
    Assert.assertTrue("DECOMMISSION".equals(command.getCustomCommandName()));
    Assert.assertTrue(("DECOMMISSION, Excluded: " + host2).equals(command.getCommandDetail()));
    Map<String, String> cmdParams = command.getExecutionCommandWrapper().getExecutionCommand().getCommandParams();
    Assert.assertTrue(cmdParams.containsKey("mark_draining_only"));
    Assert.assertEquals("false", cmdParams.get("mark_draining_only"));
    Assert.assertEquals(Role.HBASE_MASTER, command.getRole());
    Assert.assertEquals(RoleCommand.CUSTOM_COMMAND, command.getRoleCommand());
    Assert.assertEquals("DECOMMISSION", execCmd.getCommandParams().get("custom_command"));
    Assert.assertEquals(host2, execCmd.getCommandParams().get("all_decommissioned_hosts"));
    assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());

    // RS stops
    s.getServiceComponent("HBASE_REGIONSERVER").getServiceComponentHost(host2).setState(State.INSTALLED);

    // Remove RS from draining
    params = new
        HashMap<String, String>() {{
          put("excluded_hosts", host2);
          put("mark_draining_only", "true");
          put("slave_type", "HBASE_REGIONSERVER");
          put("align_maintenance_state", "true");
        }};
    resourceFilter = new RequestResourceFilter("HBASE", "HBASE_MASTER", null);
    ArrayList<RequestResourceFilter> filters = new ArrayList<>();
    filters.add(resourceFilter);
    request = new ExecuteActionRequest(cluster1, "DECOMMISSION", null,
            filters, level, params, false);

    response = controller.createAction(request, requestProperties);

    storedTasks = actionDB.getRequestTasks(response.getRequestId());
    execCmd = storedTasks.get(0).getExecutionCommandWrapper
        ().getExecutionCommand();
    Assert.assertNotNull(storedTasks);
    Assert.assertEquals(1, storedTasks.size());
    Assert.assertEquals(HostComponentAdminState.DECOMMISSIONED, scHost.getComponentAdminState());
    Assert.assertEquals(MaintenanceState.ON, scHost.getMaintenanceState());
    command = storedTasks.get(0);
    Assert.assertEquals("DECOMMISSION", execCmd.getCommandParams().get("custom_command"));
    Assert.assertEquals(host2, execCmd.getCommandParams().get("all_decommissioned_hosts"));
    Assert.assertTrue("DECOMMISSION".equals(command.getCustomCommandName()));
    Assert.assertTrue(("DECOMMISSION, Excluded: " + host2).equals(command.getCommandDetail()));
    cmdParams = command.getExecutionCommandWrapper().getExecutionCommand().getCommandParams();
    Assert.assertTrue(cmdParams.containsKey("mark_draining_only"));
    Assert.assertEquals("true", cmdParams.get("mark_draining_only"));
    assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());

    //Recommission
    params = new HashMap<String, String>() {{
      put("included_hosts", host2);
    }};
    request = new ExecuteActionRequest(cluster1, "DECOMMISSION", null,
      resourceFilters, level, params, false);

    response = controller.createAction(request,
        requestProperties);

    storedTasks = actionDB.getRequestTasks(response.getRequestId());
    execCmd = storedTasks.get(0).getExecutionCommandWrapper
        ().getExecutionCommand();
    Assert.assertNotNull(storedTasks);
    Assert.assertEquals(1, storedTasks.size());
    Assert.assertEquals(HostComponentAdminState.INSERVICE, scHost.getComponentAdminState());
    Assert.assertEquals(MaintenanceState.ON, scHost.getMaintenanceState());
    command = storedTasks.get(0);
    Assert.assertTrue("DECOMMISSION".equals(command.getCustomCommandName()));
    Assert.assertTrue(("DECOMMISSION, Included: " + host2).equals(command.getCommandDetail()));
    cmdParams = command.getExecutionCommandWrapper().getExecutionCommand().getCommandParams();
    Assert.assertTrue(cmdParams.containsKey("mark_draining_only"));
    Assert.assertEquals("false", cmdParams.get("mark_draining_only"));
    assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());

    Assert.assertTrue(cmdParams.containsKey("excluded_hosts"));
    Assert.assertEquals("", cmdParams.get("excluded_hosts"));
    Assert.assertEquals(Role.HBASE_MASTER, command.getRole());
    Assert.assertEquals(RoleCommand.CUSTOM_COMMAND, command.getRoleCommand());
    Assert.assertEquals("DECOMMISSION", execCmd.getCommandParams().get("custom_command"));
  }

  private Cluster setupClusterWithHosts(String clusterName, String stackId, List<String> hosts,
                                        String osType) throws Exception, AuthorizationException {
    ClusterRequest r = new ClusterRequest(null, clusterName, stackId, null);
    controller.createCluster(r);
    Cluster c1 = clusters.getCluster(clusterName);
    for (String host : hosts) {
      addHostToCluster(host, clusterName);
    }
    for (Host host : clusters.getHosts()) {
      clusters.updateHostMappings(host);
    }
    return c1;
  }

  @Test
  public void testGetServiceComponentHostsWithFilters() throws Exception, AuthorizationException {
    final String cluster1 = getUniqueName();
    final String host1 = getUniqueName();
    final String host2 = getUniqueName();
    final String host3 = getUniqueName();

    Cluster c1 = setupClusterWithHosts(cluster1, "HDP-0.2",
        new ArrayList<String>() {{
          add(host1);
          add(host2);
          add(host3);
        }},
        "centos5");

    RepositoryVersionEntity repositoryVersion = repositoryVersion02;

    Service s1 = serviceFactory.createNew(c1, "HDFS", repositoryVersion);
    Service s2 = serviceFactory.createNew(c1, "MAPREDUCE", repositoryVersion);
    Service s3 = serviceFactory.createNew(c1, "HBASE", repositoryVersion);

    c1.addService(s1);
    c1.addService(s2);
    c1.addService(s3);

    s1.setDesiredState(State.INSTALLED);
    s2.setDesiredState(State.INSTALLED);

    ServiceComponent sc1 = serviceComponentFactory.createNew(s1, "DATANODE");
    ServiceComponent sc2 = serviceComponentFactory.createNew(s1, "NAMENODE");
    ServiceComponent sc3 = serviceComponentFactory.createNew(s3,
        "HBASE_REGIONSERVER");

    s1.addServiceComponent(sc1);
    s1.addServiceComponent(sc2);
    s3.addServiceComponent(sc3);

    sc1.setDesiredState(State.UNINSTALLED);
    sc3.setDesiredState(State.UNINSTALLED);

    ServiceComponentHost sch1 = serviceComponentHostFactory.createNew(sc1, host1);
    ServiceComponentHost sch2 = serviceComponentHostFactory.createNew(sc1, host2);
    ServiceComponentHost sch3 = serviceComponentHostFactory.createNew(sc1, host3);
    ServiceComponentHost sch4 = serviceComponentHostFactory.createNew(sc2, host1);
    ServiceComponentHost sch5 = serviceComponentHostFactory.createNew(sc2, host2);
    ServiceComponentHost sch6 = serviceComponentHostFactory.createNew(sc3, host3);

    sc1.addServiceComponentHost(sch1);
    sc1.addServiceComponentHost(sch2);
    sc1.addServiceComponentHost(sch3);
    sc2.addServiceComponentHost(sch4);
    sc2.addServiceComponentHost(sch5);
    sc3.addServiceComponentHost(sch6);

    sch1.setDesiredState(State.INSTALLED);
    sch2.setDesiredState(State.INIT);
    sch4.setDesiredState(State.INSTALLED);
    sch5.setDesiredState(State.UNINSTALLED);

    ServiceComponentHostRequest r =
        new ServiceComponentHostRequest(null, null, null, null, null);

    try {
      controller.getHostComponents(Collections.singleton(r));
      fail("Expected failure for invalid cluster");
    } catch (Exception e) {
      // Expected
    }

    // all across cluster
    r = new ServiceComponentHostRequest(c1.getClusterName(), null,
        null, null, null);
    Set<ServiceComponentHostResponse> resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(6, resps.size());

    // all for service
    r = new ServiceComponentHostRequest(c1.getClusterName(), s1.getName(),
        null, null, null);
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(5, resps.size());

    // all for component
    r = new ServiceComponentHostRequest(c1.getClusterName(), null,
        sc3.getName(), null, null);
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(1, resps.size());

    // all for host
    r = new ServiceComponentHostRequest(c1.getClusterName(), null,
        null, host2, null);
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(2, resps.size());

    // all across cluster with state filter
    r = new ServiceComponentHostRequest(c1.getClusterName(), null,
        null, null, State.UNINSTALLED.toString());
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(1, resps.size());

    // all for service with state filter
    r = new ServiceComponentHostRequest(c1.getClusterName(), s1.getName(),
        null, null, State.INIT.toString());
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(2, resps.size());

    // all for component with state filter
    r = new ServiceComponentHostRequest(c1.getClusterName(), null,
        sc3.getName(), null, State.INSTALLED.toString());
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(0, resps.size());

    // all for host with state filter
    r = new ServiceComponentHostRequest(c1.getClusterName(), null,
        null, host2, State.INIT.toString());
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(1, resps.size());

    // for service and host
    r = new ServiceComponentHostRequest(c1.getClusterName(), s3.getName(),
        null, host1, null);
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(0, resps.size());

    // single sch - given service and host and component
    r = new ServiceComponentHostRequest(c1.getClusterName(), s3.getName(),
        sc3.getName(), host3, State.INSTALLED.toString());
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(0, resps.size());

    // single sch - given service and host and component
    r = new ServiceComponentHostRequest(c1.getClusterName(), s3.getName(),
        sc3.getName(), host3, null);
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(1, resps.size());

    ServiceComponentHostRequest r1, r2, r3;
    r1 = new ServiceComponentHostRequest(c1.getClusterName(), null,
        null, host3, null);
    r2 = new ServiceComponentHostRequest(c1.getClusterName(), s3.getName(),
        sc3.getName(), host2, null);
    r3 = new ServiceComponentHostRequest(c1.getClusterName(), null,
        null, host2, null);
    Set<ServiceComponentHostRequest> reqs =
      new HashSet<>();
    reqs.addAll(Arrays.asList(r1, r2, r3));
    resps = controller.getHostComponents(reqs);
    Assert.assertEquals(4, resps.size());
  }

  @Test
  public void testGetHosts() throws Exception, AuthorizationException {
    final String cluster1 = getUniqueName();
    final String cluster2 = getUniqueName();
    final String host1 = getUniqueName();
    final String host2 = getUniqueName();
    final String host3 = getUniqueName();
    final String host4 = getUniqueName();

    setupClusterWithHosts(cluster1, "HDP-0.2",
        new ArrayList<String>() {{
          add(host1);
          add(host2);
        }},
        "centos5");

    setupClusterWithHosts(cluster2, "HDP-0.2",
        new ArrayList<String>() {{
          add(host3);
        }},
        "centos5");
    clusters.addHost(host4);
    setOsFamily(clusters.getHost(host4), "redhat", "5.9");

    Map<String, String> attrs = new HashMap<>();
    attrs.put("a1", "b1");
    clusters.getHost(host3).setHostAttributes(attrs);
    attrs.put("a2", "b2");
    clusters.getHost(host4).setHostAttributes(attrs);

    HostRequest r = new HostRequest(null, null);

    Set<HostResponse> resps = HostResourceProviderTest.getHosts(controller, Collections.singleton(r));

    Set<String> foundHosts = new HashSet<>();

    for (HostResponse resp : resps) {
      if (resp.getHostname().equals(host1)) {
        Assert.assertEquals(cluster1, resp.getClusterName());
        Assert.assertEquals(2, resp.getHostAttributes().size());
        Assert.assertEquals(MaintenanceState.OFF, resp.getMaintenanceState());
        foundHosts.add(resp.getHostname());
      } else if (resp.getHostname().equals(host2)) {
        Assert.assertEquals(cluster1, resp.getClusterName());
        Assert.assertEquals(2, resp.getHostAttributes().size());
        Assert.assertEquals(MaintenanceState.OFF, resp.getMaintenanceState());
        foundHosts.add(resp.getHostname());
      } else if (resp.getHostname().equals(host3)) {
        Assert.assertEquals(cluster2, resp.getClusterName());
        Assert.assertEquals(3, resp.getHostAttributes().size());
        Assert.assertEquals(MaintenanceState.OFF, resp.getMaintenanceState());
        foundHosts.add(resp.getHostname());
      } else if (resp.getHostname().equals(host4)) {
        //todo: why wouldn't this be null?
        Assert.assertEquals("", resp.getClusterName());
        Assert.assertEquals(4, resp.getHostAttributes().size());
        Assert.assertEquals(null, resp.getMaintenanceState());
        foundHosts.add(resp.getHostname());
      }
    }

    Assert.assertEquals(4, foundHosts.size());

    r = new HostRequest(host1, null);
    resps = HostResourceProviderTest.getHosts(controller, Collections.singleton(r));
    Assert.assertEquals(1, resps.size());
    HostResponse resp = resps.iterator().next();
    Assert.assertEquals(host1, resp.getHostname());
    Assert.assertEquals(cluster1, resp.getClusterName());
    Assert.assertEquals(MaintenanceState.OFF, resp.getMaintenanceState());
    Assert.assertEquals(2, resp.getHostAttributes().size());

  }

  @Test
  public void testServiceUpdateBasic() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    String serviceName = "HDFS";

    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");

    clusters.getCluster(cluster1).setDesiredStackVersion(
        new StackId("HDP-0.2"));
    createService(cluster1, serviceName, State.INIT);

    Service s =
        clusters.getCluster(cluster1).getService(serviceName);
    Assert.assertNotNull(s);
    Assert.assertEquals(serviceName, s.getName());
    Assert.assertEquals(State.INIT, s.getDesiredState());
    Assert.assertEquals(cluster1, s.getCluster().getClusterName());

    Set<ServiceRequest> reqs = new HashSet<>();
    ServiceRequest r;

    try {
      r = new ServiceRequest(cluster1, serviceName, repositoryVersion02.getId(),
          State.INSTALLING.toString(), null);
      reqs.clear();
      reqs.add(r);
      ServiceResourceProviderTest.updateServices(controller, reqs, mapRequestProps, true, false);
      fail("Expected fail for invalid state transition");
    } catch (Exception e) {
      // Expected
    }

    r = new ServiceRequest(cluster1, serviceName, repositoryVersion02.getId(), State.INSTALLED.toString(),
        null);
    reqs.clear();
    reqs.add(r);
    RequestStatusResponse trackAction = ServiceResourceProviderTest.updateServices(controller, reqs,
        mapRequestProps, true, false);
    Assert.assertNull(trackAction);
  }

  @Test
  public void testServiceUpdateInvalidRequest() throws Exception, AuthorizationException {
    // multiple clusters
    // dup services
    // multiple diff end states

    String cluster1 = getUniqueName();
    createCluster(cluster1);
    String cluster2 = getUniqueName();
    createCluster(cluster2);
    String serviceName1 = "HDFS";

    createService(cluster1, serviceName1, null);
    String serviceName2 = "HBASE";
    String serviceName3 = "HBASE";

    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");

    try {
      createService(cluster2, serviceName3, repositoryVersion01, null);
      fail("Expected fail for invalid service for stack 0.1");
    } catch (Exception e) {
      // Expected
    }

    clusters.getCluster(cluster1).setDesiredStackVersion(
        new StackId("HDP-0.2"));
    clusters.getCluster(cluster2).setDesiredStackVersion(
        new StackId("HDP-0.2"));
    createService(cluster1, serviceName2, null);
    createService(cluster2, serviceName3, null);

    Set<ServiceRequest> reqs = new HashSet<>();
    ServiceRequest req1, req2;
    try {
      reqs.clear();
      req1 = new ServiceRequest(cluster1, serviceName1, repositoryVersion02.getId(),
          State.INSTALLED.toString(), null);
      req2 = new ServiceRequest(cluster2, serviceName2, repositoryVersion02.getId(),
          State.INSTALLED.toString(), null);
      reqs.add(req1);
      reqs.add(req2);
      ServiceResourceProviderTest.updateServices(controller, reqs, mapRequestProps, true, false);
      fail("Expected failure for multi cluster update");
    } catch (Exception e) {
      // Expected
    }

    try {
      reqs.clear();
      req1 = new ServiceRequest(cluster1, serviceName1, repositoryVersion02.getId(),
          State.INSTALLED.toString(), null);
      req2 = new ServiceRequest(cluster1, serviceName1, repositoryVersion02.getId(),
          State.INSTALLED.toString(), null);
      reqs.add(req1);
      reqs.add(req2);
      ServiceResourceProviderTest.updateServices(controller, reqs, mapRequestProps, true, false);
      fail("Expected failure for dups services");
    } catch (Exception e) {
      // Expected
    }

    clusters.getCluster(cluster1).getService(serviceName2)
        .setDesiredState(State.INSTALLED);

    try {
      reqs.clear();
      req1 = new ServiceRequest(cluster1, serviceName1, repositoryVersion02.getId(),
          State.INSTALLED.toString(), null);
      req2 = new ServiceRequest(cluster1, serviceName2, repositoryVersion02.getId(),
          State.STARTED.toString(), null);
      reqs.add(req1);
      reqs.add(req2);
      ServiceResourceProviderTest.updateServices(controller, reqs, mapRequestProps, true, false);
      fail("Expected failure for different states");
    } catch (Exception e) {
      // Expected
    }

  }

  @Ignore("Something fishy with the stacks here that's causing the RCO to be loaded incorrectly")
  public void testServiceUpdateRecursive() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();

    createCluster(cluster1);
    clusters.getCluster(cluster1)
        .setDesiredStackVersion(new StackId("HDP-0.2"));
    String serviceName1 = "HDFS";
    createService(cluster1, serviceName1, repositoryVersion02, null);

    String serviceName2 = "HBASE";
    createService(cluster1, serviceName2, repositoryVersion02, null);

    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HBASE_MASTER";
    String componentName4 = "HDFS_CLIENT";

    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");

    createServiceComponent(cluster1, serviceName1, componentName1,
        State.INIT);
    createServiceComponent(cluster1, serviceName1, componentName2,
        State.INIT);
    createServiceComponent(cluster1, serviceName2, componentName3,
        State.INIT);
    createServiceComponent(cluster1, serviceName1, componentName4,
        State.INIT);
    String host1 = getUniqueName();
    String host2 = getUniqueName();
    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);

    Set<ServiceComponentHostRequest> set1 =
      new HashSet<>();
    ServiceComponentHostRequest r1 =
        new ServiceComponentHostRequest(cluster1, serviceName1,
            componentName1, host1, State.INIT.toString());
    ServiceComponentHostRequest r2 =
        new ServiceComponentHostRequest(cluster1, serviceName1,
            componentName2, host1, State.INIT.toString());
    ServiceComponentHostRequest r3 =
        new ServiceComponentHostRequest(cluster1, serviceName1,
            componentName1, host2, State.INIT.toString());
    ServiceComponentHostRequest r4 =
        new ServiceComponentHostRequest(cluster1, serviceName1,
            componentName2, host2, State.INIT.toString());
    ServiceComponentHostRequest r5 =
        new ServiceComponentHostRequest(cluster1, serviceName2,
            componentName3, host1, State.INIT.toString());
    ServiceComponentHostRequest r6 =
        new ServiceComponentHostRequest(cluster1, serviceName1,
            componentName4, host2, State.INIT.toString());

    set1.add(r1);
    set1.add(r2);
    set1.add(r3);
    set1.add(r4);
    set1.add(r5);
    set1.add(r6);
    controller.createHostComponents(set1);

    Cluster c1 = clusters.getCluster(cluster1);
    Service s1 = c1.getService(serviceName1);
    Service s2 = c1.getService(serviceName2);
    ServiceComponent sc1 = s1.getServiceComponent(componentName1);
    ServiceComponent sc2 = s1.getServiceComponent(componentName2);
    ServiceComponent sc3 = s2.getServiceComponent(componentName3);
    ServiceComponent sc4 = s1.getServiceComponent(componentName4);
    ServiceComponentHost sch1 = sc1.getServiceComponentHost(host1);
    ServiceComponentHost sch2 = sc2.getServiceComponentHost(host1);
    ServiceComponentHost sch3 = sc1.getServiceComponentHost(host2);
    ServiceComponentHost sch4 = sc2.getServiceComponentHost(host2);
    ServiceComponentHost sch5 = sc3.getServiceComponentHost(host1);
    ServiceComponentHost sch6 = sc4.getServiceComponentHost(host2);

    s1.setDesiredState(State.INSTALLED);
    s2.setDesiredState(State.INSTALLED);
    sc1.setDesiredState(State.STARTED);
    sc2.setDesiredState(State.INIT);
    sc3.setDesiredState(State.STARTED);
    sc4.setDesiredState(State.INSTALLED);
    sch1.setDesiredState(State.INSTALLED);
    sch2.setDesiredState(State.INSTALLED);
    sch3.setDesiredState(State.INSTALLED);
    sch4.setDesiredState(State.INSTALLED);
    sch5.setDesiredState(State.INSTALLED);
    sch6.setDesiredState(State.INSTALLED);
    sch1.setState(State.INSTALLED);
    sch2.setState(State.INSTALLED);
    sch3.setState(State.INSTALLED);
    sch4.setState(State.INSTALLED);
    sch5.setState(State.INSTALLED);
    sch6.setState(State.INSTALLED);

    Set<ServiceRequest> reqs = new HashSet<>();
    ServiceRequest req1, req2;
    try {
      req1 = new ServiceRequest(cluster1, serviceName1, repositoryVersion02.getId(),
          State.STARTED.toString(), null);
      reqs.add(req1);
      ServiceResourceProviderTest.updateServices(controller, reqs, mapRequestProps, true, false);
      fail("Expected failure for invalid state update");
    } catch (Exception e) {
      // Expected
    }

    s1.setDesiredState(State.INSTALLED);
    s2.setDesiredState(State.INSTALLED);
    sc1.setDesiredState(State.STARTED);
    sc2.setDesiredState(State.INSTALLED);
    sc3.setDesiredState(State.STARTED);
    sch1.setDesiredState(State.INSTALLED);
    sch2.setDesiredState(State.INSTALLED);
    sch3.setDesiredState(State.INSTALLED);
    sch4.setDesiredState(State.INSTALLED);
    sch5.setDesiredState(State.INSTALLED);
    sch1.setState(State.INIT);
    sch2.setState(State.INSTALLED);
    sch3.setState(State.INIT);
    sch4.setState(State.INSTALLED);
    sch5.setState(State.INSTALLED);

    try {
      reqs.clear();
      req1 = new ServiceRequest(cluster1, serviceName1, repositoryVersion02.getId(),
          State.STARTED.toString(), null);
      reqs.add(req1);
      ServiceResourceProviderTest.updateServices(controller, reqs, mapRequestProps, true, false);
      fail("Expected failure for invalid state update");
    } catch (Exception e) {
      // Expected
    }

    s1.setDesiredState(State.INSTALLED);
    s2.setDesiredState(State.INSTALLED);
    sc1.setDesiredState(State.STARTED);
    sc2.setDesiredState(State.INSTALLED);
    sc3.setDesiredState(State.STARTED);

    sch1.setDesiredState(State.STARTED);
    sch2.setDesiredState(State.STARTED);
    sch3.setDesiredState(State.STARTED);
    sch4.setDesiredState(State.STARTED);
    sch5.setDesiredState(State.STARTED);

    sch1.setState(State.INSTALLED);
    sch2.setState(State.INSTALLED);
    sch3.setState(State.INSTALLED);
    sch4.setState(State.STARTED);
    sch5.setState(State.INSTALLED);

    reqs.clear();
    req1 = new ServiceRequest(cluster1, serviceName1, repositoryVersion02.getId(),
        State.STARTED.toString(), null);
    req2 = new ServiceRequest(cluster1, serviceName2, repositoryVersion02.getId(),
        State.STARTED.toString(), null);
    reqs.add(req1);
    reqs.add(req2);
    RequestStatusResponse trackAction = ServiceResourceProviderTest.updateServices(controller, reqs,
      mapRequestProps, true, false);

    Assert.assertEquals(State.STARTED, s1.getDesiredState());
    Assert.assertEquals(State.STARTED, s2.getDesiredState());
    Assert.assertEquals(State.STARTED, sc1.getDesiredState());
    Assert.assertEquals(State.STARTED, sc2.getDesiredState());
    Assert.assertEquals(State.STARTED, sc3.getDesiredState());
    Assert.assertEquals(State.INSTALLED, sc4.getDesiredState());
    Assert.assertEquals(State.STARTED, sch1.getDesiredState());
    Assert.assertEquals(State.STARTED, sch2.getDesiredState());
    Assert.assertEquals(State.STARTED, sch3.getDesiredState());
    Assert.assertEquals(State.STARTED, sch4.getDesiredState());
    Assert.assertEquals(State.STARTED, sch5.getDesiredState());
    Assert.assertEquals(State.INSTALLED, sch6.getDesiredState());
    Assert.assertEquals(State.INSTALLED, sch1.getState());
    Assert.assertEquals(State.INSTALLED, sch2.getState());
    Assert.assertEquals(State.INSTALLED, sch3.getState());
    Assert.assertEquals(State.STARTED, sch4.getState());
    Assert.assertEquals(State.INSTALLED, sch5.getState());
    Assert.assertEquals(State.INSTALLED, sch6.getState());

    long requestId = trackAction.getRequestId();
    List<Stage> stages = actionDB.getAllStages(requestId);

    for (Stage stage : stages) {
      LOG.debug("Stage dump: {}", stage);
    }

    Assert.assertTrue(!stages.isEmpty());
    Assert.assertEquals(3, stages.size());

    // expected
    // sch1 to start
    // sch2 to start
    // sch3 to start
    // sch5 to start
    Stage stage1 = null, stage2 = null, stage3 = null;
    for (Stage s : stages) {
      if (s.getStageId() == 0) { stage1 = s; }
      if (s.getStageId() == 1) { stage2 = s; }
      if (s.getStageId() == 2) { stage3 = s; }
    }

    Assert.assertEquals(2, stage1.getExecutionCommands(host1).size());
    Assert.assertEquals(1, stage1.getExecutionCommands(host2).size());
    Assert.assertEquals(1, stage2.getExecutionCommands(host1).size());

    Assert.assertNotNull(stage1.getExecutionCommandWrapper(host1, "NAMENODE"));
    Assert.assertNotNull(stage1.getExecutionCommandWrapper(host1, "DATANODE"));
    Assert.assertNotNull(stage1.getExecutionCommandWrapper(host2, "NAMENODE"));
    Assert.assertNotNull(stage2.getExecutionCommandWrapper(host1, "HBASE_MASTER"));
    Assert.assertNull(stage1.getExecutionCommandWrapper(host2, "DATANODE"));
    Assert.assertNotNull(stage3.getExecutionCommandWrapper(host1, "HBASE_SERVICE_CHECK"));
    Assert.assertNotNull(stage2.getExecutionCommandWrapper(host2, "HDFS_SERVICE_CHECK"));

    Type type = new TypeToken<Map<String, String>>() {}.getType();


    for (Stage s : stages) {
      for (List<ExecutionCommandWrapper> list : s.getExecutionCommands().values()) {
        for (ExecutionCommandWrapper ecw : list) {
          if (ecw.getExecutionCommand().getRole().contains("SERVICE_CHECK")) {
            Map<String, String> hostParams = StageUtils.getGson().fromJson(s.getHostParamsStage(), type);
            Assert.assertNotNull(hostParams);
            Assert.assertTrue(hostParams.size() > 0);
            Assert.assertTrue(hostParams.containsKey("stack_version"));
            Assert.assertEquals(hostParams.get("stack_version"), c1.getDesiredStackVersion().getStackVersion());
          }
        }
      }
    }

    // manually set live state
    sch1.setState(State.STARTED);
    sch2.setState(State.STARTED);
    sch3.setState(State.STARTED);
    sch4.setState(State.STARTED);
    sch5.setState(State.STARTED);

    // test no-op
    reqs.clear();
    req1 = new ServiceRequest(cluster1, serviceName1, repositoryVersion02.getId(),
        State.STARTED.toString(), null);
    req2 = new ServiceRequest(cluster1, serviceName2, repositoryVersion02.getId(),
        State.STARTED.toString(), null);
    reqs.add(req1);
    reqs.add(req2);
    trackAction = ServiceResourceProviderTest.updateServices(controller, reqs, mapRequestProps, true,
      false);
    Assert.assertNull(trackAction);

  }

  @Test
  public void testServiceComponentUpdateRecursive() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();

    createCluster(cluster1);
    String serviceName1 = "HDFS";
    createService(cluster1, serviceName1, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";
    createServiceComponent(cluster1, serviceName1, componentName1,
        State.INIT);
    createServiceComponent(cluster1, serviceName1, componentName2,
        State.INIT);
    createServiceComponent(cluster1, serviceName1, componentName3,
        State.INIT);

    String host1 = getUniqueName();
    String host2 = getUniqueName();

    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);

    Set<ServiceComponentHostRequest> set1 =
      new HashSet<>();
    ServiceComponentHostRequest r1 =
        new ServiceComponentHostRequest(cluster1, serviceName1,
            componentName1, host1, State.INIT.toString());
    ServiceComponentHostRequest r2 =
        new ServiceComponentHostRequest(cluster1, serviceName1,
            componentName2, host1, State.INIT.toString());
    ServiceComponentHostRequest r3 =
        new ServiceComponentHostRequest(cluster1, serviceName1,
            componentName1, host2, State.INIT.toString());
    ServiceComponentHostRequest r4 =
        new ServiceComponentHostRequest(cluster1, serviceName1,
            componentName2, host2, State.INIT.toString());
    ServiceComponentHostRequest r5 =
        new ServiceComponentHostRequest(cluster1, serviceName1,
            componentName3, host1, State.INIT.toString());

    set1.add(r1);
    set1.add(r2);
    set1.add(r3);
    set1.add(r4);
    set1.add(r5);
    controller.createHostComponents(set1);

    Cluster c1 = clusters.getCluster(cluster1);
    Service s1 = c1.getService(serviceName1);
    ServiceComponent sc1 = s1.getServiceComponent(componentName1);
    ServiceComponent sc2 = s1.getServiceComponent(componentName2);
    ServiceComponent sc3 = s1.getServiceComponent(componentName3);
    ServiceComponentHost sch1 = sc1.getServiceComponentHost(host1);
    ServiceComponentHost sch2 = sc2.getServiceComponentHost(host1);
    ServiceComponentHost sch3 = sc1.getServiceComponentHost(host2);
    ServiceComponentHost sch4 = sc2.getServiceComponentHost(host2);
    ServiceComponentHost sch5 = sc3.getServiceComponentHost(host1);

    s1.setDesiredState(State.INSTALLED);
    sc1.setDesiredState(State.INIT);
    sc2.setDesiredState(State.INIT);
    sc3.setDesiredState(State.STARTED);
    sch1.setDesiredState(State.INSTALLED);
    sch2.setDesiredState(State.INSTALLED);
    sch3.setDesiredState(State.STARTED);
    sch4.setDesiredState(State.INSTALLED);
    sch5.setDesiredState(State.INSTALLED);
    sch1.setState(State.INSTALLED);
    sch2.setState(State.INSTALLED);
    sch3.setState(State.STARTED);
    sch4.setState(State.INSTALLED);
    sch5.setState(State.UNKNOWN);

    Set<ServiceComponentRequest> reqs =
      new HashSet<>();
    ServiceComponentRequest req1, req2, req3;

    // confirm an UNKOWN doesn't fail
    req1 = new ServiceComponentRequest(cluster1, serviceName1,
        sc3.getName(), State.INSTALLED.toString());
    reqs.add(req1);
    ComponentResourceProviderTest.updateComponents(controller, reqs, Collections.emptyMap(), true);
    try {
      reqs.clear();
      req1 = new ServiceComponentRequest(cluster1, serviceName1,
          sc1.getName(), State.INIT.toString());
      reqs.add(req1);
      ComponentResourceProviderTest.updateComponents(controller, reqs, Collections.emptyMap(), true);
      fail("Expected failure for invalid state update");
    } catch (Exception e) {
      // Expected
    }

    s1.setDesiredState(State.INSTALLED);
    sc1.setDesiredState(State.STARTED);
    sc2.setDesiredState(State.INSTALLED);
    sc3.setDesiredState(State.STARTED);
    sch1.setDesiredState(State.INIT);
    sch2.setDesiredState(State.INIT);
    sch3.setDesiredState(State.INIT);
    sch4.setDesiredState(State.INIT);
    sch5.setDesiredState(State.INIT);
    sch1.setState(State.INIT);
    sch2.setState(State.INSTALLED);
    sch3.setState(State.INIT);
    sch4.setState(State.INSTALLED);
    sch5.setState(State.INSTALLED);

    try {
      reqs.clear();
      req1 = new ServiceComponentRequest(cluster1, serviceName1,
          sc1.getName(), State.STARTED.toString());
      reqs.add(req1);
      ComponentResourceProviderTest.updateComponents(controller, reqs, Collections.emptyMap(), true);
      fail("Expected failure for invalid state update");
    } catch (Exception e) {
      // Expected
    }

    s1.setDesiredState(State.INSTALLED);
    sc1.setDesiredState(State.STARTED);
    sc2.setDesiredState(State.INIT);
    sc3.setDesiredState(State.STARTED);
    sch1.setDesiredState(State.INIT);
    sch2.setDesiredState(State.INIT);
    sch3.setDesiredState(State.INIT);
    sch4.setDesiredState(State.INIT);
    sch5.setDesiredState(State.INIT);
    sch1.setState(State.STARTED);
    sch2.setState(State.INIT);
    sch3.setState(State.INSTALLED);
    sch4.setState(State.STARTED);
    sch5.setState(State.INIT);

    reqs.clear();
    req1 = new ServiceComponentRequest(cluster1, serviceName1,
        sc1.getName(), State.INSTALLED.toString());
    req2 = new ServiceComponentRequest(cluster1, serviceName1,
        sc2.getName(), State.INSTALLED.toString());
    req3 = new ServiceComponentRequest(cluster1, serviceName1,
        sc3.getName(), State.INSTALLED.toString());
    reqs.add(req1);
    reqs.add(req2);
    reqs.add(req3);
    RequestStatusResponse trackAction = ComponentResourceProviderTest.updateComponents(controller, reqs, Collections.emptyMap(), true);

    Assert.assertEquals(State.INSTALLED, s1.getDesiredState());
    Assert.assertEquals(State.INSTALLED, sc1.getDesiredState());
    Assert.assertEquals(State.INSTALLED, sc2.getDesiredState());
    Assert.assertEquals(State.INSTALLED, sc3.getDesiredState());
    Assert.assertEquals(State.INSTALLED, sch1.getDesiredState());
    Assert.assertEquals(State.INSTALLED, sch2.getDesiredState());
    Assert.assertEquals(State.INSTALLED, sch3.getDesiredState());
    Assert.assertEquals(State.INSTALLED, sch4.getDesiredState());
    Assert.assertEquals(State.INSTALLED, sch5.getDesiredState());
    Assert.assertEquals(State.STARTED, sch1.getState());
    Assert.assertEquals(State.INIT, sch2.getState());
    Assert.assertEquals(State.INSTALLED, sch3.getState());
    Assert.assertEquals(State.STARTED, sch4.getState());
    Assert.assertEquals(State.INIT, sch5.getState());

    long requestId = trackAction.getRequestId();
    List<Stage> stages = actionDB.getAllStages(requestId);
    Assert.assertTrue(!stages.isEmpty());

    // FIXME check stage count

    for (Stage stage : stages) {
      LOG.debug("Stage dump: {}", stage);
    }

    // FIXME verify stages content - execution commands, etc

    // maually set live state
    sch1.setState(State.INSTALLED);
    sch2.setState(State.INSTALLED);
    sch3.setState(State.INSTALLED);
    sch4.setState(State.INSTALLED);
    sch5.setState(State.INSTALLED);

    // test no-op
    reqs.clear();
    req1 = new ServiceComponentRequest(cluster1, serviceName1,
        sc1.getName(), State.INSTALLED.toString());
    req2 = new ServiceComponentRequest(cluster1, serviceName1,
        sc2.getName(), State.INSTALLED.toString());
    reqs.add(req1);
    reqs.add(req2);
    trackAction = ComponentResourceProviderTest.updateComponents(controller, reqs, Collections.emptyMap(), true);
    Assert.assertNull(trackAction);
  }

  @Test
  public void testServiceComponentHostUpdateRecursive() throws Exception {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    String serviceName1 = "HDFS";
    createService(cluster1, serviceName1, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";
    createServiceComponent(cluster1, serviceName1, componentName1,
        State.INIT);
    createServiceComponent(cluster1, serviceName1, componentName2,
        State.INIT);
    createServiceComponent(cluster1, serviceName1, componentName3,
        State.INIT);
    String host1 = getUniqueName();
    String host2 = getUniqueName();
    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);


    Set<ServiceComponentHostRequest> set1 =
      new HashSet<>();
    ServiceComponentHostRequest r1 =
        new ServiceComponentHostRequest(cluster1, serviceName1,
            componentName1, host1, State.INIT.toString());
    ServiceComponentHostRequest r2 =
        new ServiceComponentHostRequest(cluster1, serviceName1,
            componentName2, host1, State.INIT.toString());
    ServiceComponentHostRequest r3 =
        new ServiceComponentHostRequest(cluster1, serviceName1,
            componentName1, host2, State.INIT.toString());
    ServiceComponentHostRequest r4 =
        new ServiceComponentHostRequest(cluster1, serviceName1,
            componentName2, host2, State.INIT.toString());
    ServiceComponentHostRequest r5 =
        new ServiceComponentHostRequest(cluster1, serviceName1,
            componentName3, host1, State.INIT.toString());

    set1.add(r1);
    set1.add(r2);
    set1.add(r3);
    set1.add(r4);
    set1.add(r5);
    controller.createHostComponents(set1);

    Cluster c1 = clusters.getCluster(cluster1);
    Service s1 = c1.getService(serviceName1);
    ServiceComponent sc1 = s1.getServiceComponent(componentName1);
    ServiceComponent sc2 = s1.getServiceComponent(componentName2);
    ServiceComponent sc3 = s1.getServiceComponent(componentName3);
    ServiceComponentHost sch1 = sc1.getServiceComponentHost(host1);
    ServiceComponentHost sch2 = sc2.getServiceComponentHost(host1);
    ServiceComponentHost sch3 = sc1.getServiceComponentHost(host2);
    ServiceComponentHost sch4 = sc2.getServiceComponentHost(host2);
    ServiceComponentHost sch5 = sc3.getServiceComponentHost(host1);

    s1.setDesiredState(State.INIT);
    sc1.setDesiredState(State.INIT);
    sc2.setDesiredState(State.INIT);
    sc3.setDesiredState(State.INIT);
    sch1.setDesiredState(State.INIT);
    sch2.setDesiredState(State.INIT);
    sch3.setDesiredState(State.INIT);
    sch4.setDesiredState(State.INSTALLED);
    sch5.setDesiredState(State.INSTALLED);
    sch1.setState(State.INIT);
    sch2.setState(State.INSTALL_FAILED);
    sch3.setState(State.INIT);
    sch4.setState(State.INSTALLED);
    sch5.setState(State.INSTALLED);

    ServiceComponentHostRequest req1, req2, req3, req4, req5;
    Set<ServiceComponentHostRequest> reqs =
      new HashSet<>();

    //todo: I had to comment this portion of the test out for now because I had to modify
    //todo: the transition validation code for the new advanced provisioning
    //todo: work which causes a failure here due to lack of an exception.
//    try {
//      reqs.clear();
//      req1 = new ServiceComponentHostRequest(cluster1, serviceName1,
//          componentName1, host1,
//          State.STARTED.toString());
//      reqs.add(req1);
//      updateHostComponents(reqs, Collections.<String, String>emptyMap(), true);
//      fail("Expected failure for invalid transition");
//    } catch (Exception e) {
//      // Expected
//    }

    try {
      reqs.clear();
      req1 = new ServiceComponentHostRequest(cluster1, serviceName1,
          componentName1, host1,
          State.INSTALLED.toString());
      req2 = new ServiceComponentHostRequest(cluster1, serviceName1,
          componentName1, host2,
          State.INSTALLED.toString());
      req3 = new ServiceComponentHostRequest(cluster1, serviceName1,
          componentName2, host1,
          State.INSTALLED.toString());
      req4 = new ServiceComponentHostRequest(cluster1, serviceName1,
          componentName2, host2,
          State.INSTALLED.toString());
      req5 = new ServiceComponentHostRequest(cluster1, serviceName1,
          componentName3, host1,
          State.STARTED.toString());
      reqs.add(req1);
      reqs.add(req2);
      reqs.add(req3);
      reqs.add(req4);
      reqs.add(req5);
      updateHostComponents(reqs, Collections.emptyMap(), true);
      // Expected, now client components with STARTED status will be ignored
    } catch (Exception e) {
      fail("Failure for invalid states");
    }

    reqs.clear();
    req1 = new ServiceComponentHostRequest(cluster1, null,
        componentName1, host1, State.INSTALLED.toString());
    req2 = new ServiceComponentHostRequest(cluster1, serviceName1,
        componentName1, host2, State.INSTALLED.toString());
    req3 = new ServiceComponentHostRequest(cluster1, null,
        componentName2, host1, State.INSTALLED.toString());
    req4 = new ServiceComponentHostRequest(cluster1, serviceName1,
        componentName2, host2, State.INSTALLED.toString());
    req5 = new ServiceComponentHostRequest(cluster1, serviceName1,
        componentName3, host1, State.INSTALLED.toString());
    reqs.add(req1);
    reqs.add(req2);
    reqs.add(req3);
    reqs.add(req4);
    reqs.add(req5);
    RequestStatusResponse trackAction = updateHostComponents(reqs,
        Collections.emptyMap(), true);
    Assert.assertNotNull(trackAction);

    long requestId = trackAction.getRequestId();

    Assert.assertFalse(actionDB.getAllStages(requestId).isEmpty());
    List<Stage> stages = actionDB.getAllStages(requestId);
    // FIXME check stage count

    for (Stage stage : stages) {
      LOG.debug("Stage dump: {}", stage);
    }

    // FIXME verify stages content - execution commands, etc

    // manually set live state
    sch1.setState(State.INSTALLED);
    sch2.setState(State.INSTALLED);
    sch3.setState(State.INSTALLED);
    sch4.setState(State.INSTALLED);
    sch5.setState(State.INSTALLED);

    // test no-op
    reqs.clear();
    req1 = new ServiceComponentHostRequest(cluster1, serviceName1,
        componentName1, host1,
        State.INSTALLED.toString());
    req2 = new ServiceComponentHostRequest(cluster1, serviceName1,
        componentName1, host2,
        State.INSTALLED.toString());
    reqs.add(req1);
    reqs.add(req2);
    trackAction = updateHostComponents(reqs, Collections.emptyMap(), true);
    Assert.assertNull(trackAction);
  }

  @Test
  public void testCreateCustomActions() throws Exception {
    final String cluster1 = getUniqueName();
    // !!! weird, but the assertions are banking on alphabetical order
    final String host1 = "a" + getUniqueName();
    final String host2 = "b" + getUniqueName();
    final String host3 = "c" + getUniqueName();

    setupClusterWithHosts(cluster1, "HDP-2.0.6",
        new ArrayList<String>() {{
          add(host1);
          add(host2);
          add(host3);
        }},
        "centos6");

    Cluster cluster = clusters.getCluster(cluster1);
    cluster.setDesiredStackVersion(new StackId("HDP-2.0.6"));
    cluster.setCurrentStackVersion(new StackId("HDP-2.0.6"));

    ConfigFactory cf = injector.getInstance(ConfigFactory.class);
    Config config1 = cf.createNew(cluster, "global", "version1",
        new HashMap<String, String>() {{
          put("key1", "value1");
        }}, new HashMap<>());

    Config config2 = cf.createNew(cluster, "core-site", "version1",
        new HashMap<String, String>() {{
          put("key1", "value1");
        }}, new HashMap<>());

    Config config3 = cf.createNew(cluster, "yarn-site", "version1",
        new HashMap<String, String>() {{
          put("test.password", "supersecret");
        }}, new HashMap<>());

    RepositoryVersionEntity repositoryVersion = repositoryVersion206;

    Service hdfs = cluster.addService("HDFS", repositoryVersion);
    Service mapred = cluster.addService("YARN", repositoryVersion);

    hdfs.addServiceComponent(Role.HDFS_CLIENT.name());
    hdfs.addServiceComponent(Role.NAMENODE.name());
    hdfs.addServiceComponent(Role.DATANODE.name());

    mapred.addServiceComponent(Role.RESOURCEMANAGER.name());

    hdfs.getServiceComponent(Role.HDFS_CLIENT.name()).addServiceComponentHost(host1);
    hdfs.getServiceComponent(Role.NAMENODE.name()).addServiceComponentHost(host1);
    hdfs.getServiceComponent(Role.DATANODE.name()).addServiceComponentHost(host1);
    hdfs.getServiceComponent(Role.DATANODE.name()).addServiceComponentHost(host2);

    String actionDef1 = getUniqueName();
    String actionDef2 = getUniqueName();


    ActionDefinition a1 = new ActionDefinition(actionDef1, ActionType.SYSTEM,
        "test,[optional1]", "", "", "Does file exist", TargetHostType.SPECIFIC, Short.valueOf("100"), null);
    controller.getAmbariMetaInfo().addActionDefinition(a1);
    controller.getAmbariMetaInfo().addActionDefinition(new ActionDefinition(
        actionDef2, ActionType.SYSTEM, "", "HDFS", "DATANODE", "Does file exist",
        TargetHostType.ALL, Short.valueOf("1000"), null));

    Map<String, String> params = new HashMap<String, String>() {{
      put("test", "test");
      put("pwd", "SECRET:yarn-site:1:test.password");
    }};

    Map<String, String> requestProperties = new HashMap<>();
    requestProperties.put(REQUEST_CONTEXT_PROPERTY, "Called from a test");
    requestProperties.put("datanode", "abc");

    ArrayList<String> hosts = new ArrayList<String>() {{add(host1);}};
    RequestResourceFilter resourceFilter = new RequestResourceFilter("HDFS", "DATANODE", hosts);
    List<RequestResourceFilter> resourceFilters = new ArrayList<>();
    resourceFilters.add(resourceFilter);

    ExecuteActionRequest actionRequest = new ExecuteActionRequest(cluster1, null, actionDef1, resourceFilters, null, params, false);
    RequestStatusResponse response = controller.createAction(actionRequest, requestProperties);
    assertEquals(1, response.getTasks().size());
    ShortTaskStatus taskStatus = response.getTasks().get(0);
    Assert.assertEquals(host1, taskStatus.getHostName());

    List<HostRoleCommand> storedTasks = actionDB.getRequestTasks(response.getRequestId());
    Stage stage = actionDB.getAllStages(response.getRequestId()).get(0);
    Assert.assertNotNull(stage);

    Assert.assertEquals(1, storedTasks.size());
    HostRoleCommand task = storedTasks.get(0);
    Assert.assertEquals(RoleCommand.ACTIONEXECUTE, task.getRoleCommand());
    Assert.assertEquals(actionDef1, task.getRole().name());
    Assert.assertEquals(host1, task.getHostName());
    ExecutionCommand cmd = task.getExecutionCommandWrapper().getExecutionCommand();
    // h1 has only DATANODE, NAMENODE, CLIENT sch's
    Assert.assertEquals(host1, cmd.getHostname());
    Assert.assertFalse(cmd.getLocalComponents().isEmpty());
    Assert.assertTrue(cmd.getLocalComponents().contains(Role.DATANODE.name()));
    Assert.assertTrue(cmd.getLocalComponents().contains(Role.NAMENODE.name()));
    Assert.assertTrue(cmd.getLocalComponents().contains(Role.HDFS_CLIENT.name()));
    Assert.assertFalse(cmd.getLocalComponents().contains(Role.RESOURCEMANAGER.name()));
    Type type = new TypeToken<Map<String, String>>(){}.getType();
    Map<String, String> hostParametersStage = StageUtils.getGson().fromJson(stage.getHostParamsStage(), type);
    Map<String, String> commandParametersStage = StageUtils.getGson().fromJson(stage.getCommandParamsStage(), type);

    Assert.assertTrue(commandParametersStage.containsKey("test"));
    Assert.assertTrue(commandParametersStage.containsKey("pwd"));
    Assert.assertEquals(commandParametersStage.get("pwd"), "supersecret");
    Assert.assertEquals("HDFS", cmd.getServiceName());
    Assert.assertEquals("DATANODE", cmd.getComponentName());
    Assert.assertNotNull(hostParametersStage.get("jdk_location"));
    Assert.assertEquals("900", cmd.getCommandParams().get("command_timeout"));
    Assert.assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());

    // !!! test that the action execution helper is using the right timeout
    a1.setDefaultTimeout((short) 1800);
    actionRequest = new ExecuteActionRequest(cluster1, null, actionDef1, resourceFilters, null, params, false);
    response = controller.createAction(actionRequest, requestProperties);

    List<HostRoleCommand> storedTasks1 = actionDB.getRequestTasks(response.getRequestId());
    cmd = storedTasks1.get(0).getExecutionCommandWrapper().getExecutionCommand();
    Assert.assertEquals("1800", cmd.getCommandParams().get("command_timeout"));

    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("HDFS", "", null);
    resourceFilters.add(resourceFilter);
    actionRequest = new ExecuteActionRequest(cluster1, null, actionDef2, resourceFilters, null, params, false);
    response = controller.createAction(actionRequest, requestProperties);
    assertEquals(2, response.getTasks().size());

    final List<HostRoleCommand> storedTasks2 = actionDB.getRequestTasks(response.getRequestId());
    task = storedTasks2.get(1);
    Assert.assertEquals(RoleCommand.ACTIONEXECUTE, task.getRoleCommand());
    Assert.assertEquals(actionDef2, task.getRole().name());
    HashSet<String> expectedHosts = new HashSet<String>() {{
      add(host2);
      add(host1);
    }};
    HashSet<String> actualHosts = new HashSet<String>() {{
      add(storedTasks2.get(1).getHostName());
      add(storedTasks2.get(0).getHostName());
    }};
    Assert.assertEquals(expectedHosts, actualHosts);

    cmd = task.getExecutionCommandWrapper().getExecutionCommand();
    commandParametersStage = StageUtils.getGson().fromJson(stage.getCommandParamsStage(), type);

    Assert.assertTrue(commandParametersStage.containsKey("test"));
    Assert.assertTrue(commandParametersStage.containsKey("pwd"));
    Assert.assertEquals(commandParametersStage.get("pwd"), "supersecret");
    Assert.assertEquals("HDFS", cmd.getServiceName());
    Assert.assertEquals("DATANODE", cmd.getComponentName());
    Assert.assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());
    // h2 has only DATANODE sch
    Assert.assertEquals(host2, cmd.getHostname());
    Assert.assertFalse(cmd.getLocalComponents().isEmpty());
    Assert.assertTrue(cmd.getLocalComponents().contains(Role.DATANODE.name()));
    Assert.assertFalse(cmd.getLocalComponents().contains(Role.NAMENODE.name()));
    Assert.assertFalse(cmd.getLocalComponents().contains(Role.HDFS_CLIENT.name()));
    Assert.assertFalse(cmd.getLocalComponents().contains(Role.RESOURCEMANAGER.name()));

    hosts = new ArrayList<String>() {{add(host3);}};
    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("HDFS", "", hosts);
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest(cluster1, null, actionDef1, resourceFilters, null, params, false);
    response = controller.createAction(actionRequest, requestProperties);
    assertEquals(1, response.getTasks().size());
    taskStatus = response.getTasks().get(0);
    Assert.assertEquals(host3, taskStatus.getHostName());
    Assert.assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());
  }

  @Test
  public void testComponentCategorySentWithRestart() throws Exception, AuthorizationException {
    final String cluster1 = getUniqueName();
    final String host1 = getUniqueName();

    setupClusterWithHosts(cluster1, "HDP-2.0.7",
      new ArrayList<String>() {{
        add(host1);
      }},
      "centos5");

    Cluster cluster = clusters.getCluster(cluster1);
    cluster.setDesiredStackVersion(new StackId("HDP-2.0.7"));
    cluster.setCurrentStackVersion(new StackId("HDP-2.0.7"));

    ConfigFactory cf = injector.getInstance(ConfigFactory.class);
    Config config1 = cf.createNew(cluster, "global", "version1",
      new HashMap<String, String>() {{
        put("key1", "value1");
      }}, new HashMap<>());

    Config config2 = cf.createNew(cluster, "core-site", "version1",
      new HashMap<String, String>() {{
        put("key1", "value1");
      }}, new HashMap<>());

    RepositoryVersionEntity repositoryVersion = repositoryVersion207;

    Service hdfs = cluster.addService("HDFS", repositoryVersion);

    hdfs.addServiceComponent(Role.HDFS_CLIENT.name());
    hdfs.addServiceComponent(Role.NAMENODE.name());
    hdfs.addServiceComponent(Role.DATANODE.name());

    hdfs.getServiceComponent(Role.HDFS_CLIENT.name()).addServiceComponentHost(host1);
    hdfs.getServiceComponent(Role.NAMENODE.name()).addServiceComponentHost(host1);
    hdfs.getServiceComponent(Role.DATANODE.name()).addServiceComponentHost(host1);

    installService(cluster1, "HDFS", false, false);

    startService(cluster1, "HDFS", false, false);

    Cluster c = clusters.getCluster(cluster1);
    Service s = c.getService("HDFS");

    Assert.assertEquals(State.STARTED, s.getDesiredState());
    for (ServiceComponent sc : s.getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        if (sc.isClientComponent()) {
          Assert.assertEquals(State.INSTALLED, sch.getDesiredState());
        } else {
          Assert.assertEquals(State.STARTED, sch.getDesiredState());
        }
      }
    }

    Map<String, String> params = new HashMap<String, String>() {{
      put("test", "test");
    }};
    RequestResourceFilter resourceFilter = new RequestResourceFilter(
      "HDFS",
      "HDFS_CLIENT",
      new ArrayList<String>() {{ add(host1); }});
    ExecuteActionRequest actionRequest = new ExecuteActionRequest(cluster1,
      "RESTART", params, false);
    actionRequest.getResourceFilters().add(resourceFilter);

    Map<String, String> requestProperties = new HashMap<>();
    requestProperties.put(REQUEST_CONTEXT_PROPERTY, "Called from a test");
    requestProperties.put("hdfs_client", "abc");

    RequestStatusResponse response = controller.createAction(actionRequest, requestProperties);

    List<Stage> stages = actionDB.getAllStages(response.getRequestId());
    Assert.assertNotNull(stages);

    HostRoleCommand hrc = null;
    Type type = new TypeToken<Map<String, String>>(){}.getType();
    for (Stage stage : stages) {
      for (HostRoleCommand cmd : stage.getOrderedHostRoleCommands()) {
        if (cmd.getRole().equals(Role.HDFS_CLIENT)) {
          hrc = cmd;
        }
        Map<String, String> hostParamStage = StageUtils.getGson().fromJson(stage.getHostParamsStage(), type);
        Assert.assertTrue(hostParamStage.containsKey(ExecutionCommand.KeyNames.DB_DRIVER_FILENAME));
        Assert.assertTrue(hostParamStage.containsKey(ExecutionCommand.KeyNames.MYSQL_JDBC_URL));
        Assert.assertTrue(hostParamStage.containsKey(ExecutionCommand.KeyNames.ORACLE_JDBC_URL));
      }
    }
    Assert.assertNotNull(hrc);
    Assert.assertEquals("RESTART HDFS/HDFS_CLIENT", hrc.getCommandDetail());
    Map<String, String> roleParams = hrc.getExecutionCommandWrapper()
      .getExecutionCommand().getRoleParams();

    Assert.assertNotNull(roleParams);
    Assert.assertEquals("CLIENT", roleParams.get(ExecutionCommand.KeyNames.COMPONENT_CATEGORY));
    Assert.assertTrue(hrc.getExecutionCommandWrapper().getExecutionCommand().getCommandParams().containsKey("hdfs_client"));
    Assert.assertEquals("abc", hrc.getExecutionCommandWrapper().getExecutionCommand().getCommandParams().get("hdfs_client"));
    Assert.assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());
  }

  @SuppressWarnings("serial")
  @Test
  public void testCreateActionsFailures() throws Exception {
    final String cluster1 = getUniqueName();
    final String host1 = getUniqueName();

    setupClusterWithHosts(cluster1, "HDP-2.0.7",
        new ArrayList<String>() {{
          add(host1);
        }},
        "centos5");

    Cluster cluster = clusters.getCluster(cluster1);
    cluster.setDesiredStackVersion(new StackId("HDP-2.0.7"));
    cluster.setCurrentStackVersion(new StackId("HDP-2.0.7"));

    RepositoryVersionEntity repositoryVersion = repositoryVersion207;

    ConfigFactory cf = injector.getInstance(ConfigFactory.class);
    Config config1 = cf.createNew(cluster, "global", "version1",
        new HashMap<String, String>() {{
          put("key1", "value1");
        }}, new HashMap<>());

    Config config2 = cf.createNew(cluster, "core-site", "version1",
        new HashMap<String, String>() {{
          put("key1", "value1");
        }}, new HashMap<>());

    cluster.addConfig(config1);
    cluster.addConfig(config2);
    cluster.addDesiredConfig("_test", Collections.singleton(config1));
    cluster.addDesiredConfig("_test", Collections.singleton(config2));

    Service hdfs = cluster.addService("HDFS", repositoryVersion);
    Service hive = cluster.addService("HIVE", repositoryVersion);

    hdfs.addServiceComponent(Role.HDFS_CLIENT.name());
    hdfs.addServiceComponent(Role.NAMENODE.name());
    hdfs.addServiceComponent(Role.DATANODE.name());

    hive.addServiceComponent(Role.HIVE_SERVER.name());

    hdfs.getServiceComponent(Role.HDFS_CLIENT.name()).addServiceComponentHost(host1);
    hdfs.getServiceComponent(Role.NAMENODE.name()).addServiceComponentHost(host1);
    hdfs.getServiceComponent(Role.DATANODE.name()).addServiceComponentHost(host1);

    Map<String, String> params = new HashMap<String, String>() {{
      put("test", "test");
    }};

    RequestResourceFilter resourceFilter = new RequestResourceFilter("HDFS", null, null);

    ExecuteActionRequest actionRequest = new ExecuteActionRequest(cluster1, "NON_EXISTENT_CHECK", params, false);
    actionRequest.getResourceFilters().add(resourceFilter);

    Map<String, String> requestProperties = new HashMap<>();
    requestProperties.put(REQUEST_CONTEXT_PROPERTY, "Called from a test");

    expectActionCreationErrorWithMessage(actionRequest, requestProperties, "Unsupported action");

    //actionRequest = new ExecuteActionRequest(cluster1, "NON_EXISTENT_SERVICE_CHECK", "HDFS", params);
    //expectActionCreationErrorWithMessage(actionRequest, requestProperties, "Unsupported action");

    actionRequest = new ExecuteActionRequest(cluster1, "DECOMMISSION_DATANODE", params, false);
    actionRequest.getResourceFilters().add(resourceFilter);

    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
      "Unsupported action DECOMMISSION_DATANODE for Service: HDFS and Component: null");

    //actionRequest = new ExecuteActionRequest(cluster1, "DECOMMISSION", "HDFS", params);
    //expectActionCreationErrorWithMessage(actionRequest, requestProperties, "Unsupported action DECOMMISSION for Service: HDFS and Component: null");

    resourceFilter = new RequestResourceFilter("HDFS", "HDFS_CLIENT", null);
    List<RequestResourceFilter> resourceFilters = new ArrayList<>();
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest(cluster1, "DECOMMISSION", null, resourceFilters, null, params, false);

    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Unsupported action DECOMMISSION for Service: HDFS and Component: HDFS_CLIENT");

    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("HDFS", null, null);
    resourceFilters.add(resourceFilter);
    actionRequest = new ExecuteActionRequest(cluster1, null, "DECOMMISSION_DATANODE", resourceFilters, null, params, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Action DECOMMISSION_DATANODE does not exist");

    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("YARN", "RESOURCEMANAGER", null);
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest(cluster1, "DECOMMISSION", null, resourceFilters, null, params, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Service not found, clusterName=" + cluster1 + ", serviceName=YARN");

    Map<String, String> params2 = new HashMap<String, String>() {{
      put("included_hosts", "h1,h2");
      put("excluded_hosts", "h1,h3");
    }};

    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("HDFS", "NAMENODE", null);
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest(cluster1, "DECOMMISSION", null, resourceFilters, null, params2, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Same host cannot be specified for inclusion as well as exclusion. Hosts: [h1]");

    params2 = new HashMap<String, String>() {{
      put("included_hosts", " h1,h2");
      put("excluded_hosts", "h4, h3");
      put("slave_type", "HDFS_CLIENT");
    }};
    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("HDFS", "NAMENODE", null);
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest(cluster1, "DECOMMISSION", null, resourceFilters, null, params2, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Component HDFS_CLIENT is not supported for decommissioning.");

    List<String> hosts = new ArrayList<>();
    hosts.add("h6");
    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("HDFS", "NAMENODE", hosts);
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest(cluster1, "DECOMMISSION", null, resourceFilters, null, params2, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Decommission command cannot be issued with target host(s) specified.");

    hdfs.getServiceComponent(Role.DATANODE.name()).getServiceComponentHost(host1).setState(State.INSTALLED);
    params2 = new HashMap<String, String>() {{
      put("excluded_hosts", host1);
    }};
    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("HDFS", "NAMENODE", null);
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest(cluster1, "DECOMMISSION", null, resourceFilters, null, params2, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Component DATANODE on host " + host1 + " cannot be decommissioned as its not in STARTED state");

    params2 = new HashMap<String, String>() {{
      put("excluded_hosts", "h1 ");
      put("mark_draining_only", "true");
    }};
    actionRequest = new ExecuteActionRequest(cluster1, "DECOMMISSION", null, resourceFilters, null, params2, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "mark_draining_only is not a valid parameter for NAMENODE");

    String actionDef1 = getUniqueName();
    String actionDef2 = getUniqueName();
    String actionDef3 = getUniqueName();
    String actionDef4 = getUniqueName();

    controller.getAmbariMetaInfo().addActionDefinition(new ActionDefinition(
        actionDef1, ActionType.SYSTEM, "test,dirName", "", "", "Does file exist",
        TargetHostType.SPECIFIC, Short.valueOf("100"), null));

    controller.getAmbariMetaInfo().addActionDefinition(new ActionDefinition(
        actionDef2, ActionType.SYSTEM, "", "HDFS", "DATANODE", "Does file exist",
        TargetHostType.ANY, Short.valueOf("100"), null));

    controller.getAmbariMetaInfo().addActionDefinition(new ActionDefinition(
            "update_repo", ActionType.SYSTEM, "", "HDFS", "DATANODE", "Does file exist",
            TargetHostType.ANY, Short.valueOf("100"), null));

    controller.getAmbariMetaInfo().addActionDefinition(new ActionDefinition(
        actionDef3, ActionType.SYSTEM, "", "MAPREDUCE", "MAPREDUCE_CLIENT", "Does file exist",
        TargetHostType.ANY, Short.valueOf("100"), null));

    controller.getAmbariMetaInfo().addActionDefinition(new ActionDefinition(
        actionDef4, ActionType.SYSTEM, "", "HIVE", "", "Does file exist",
        TargetHostType.ANY, Short.valueOf("100"), null));

    actionRequest = new ExecuteActionRequest(cluster1, null, actionDef1, null, null, null, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Action " + actionDef1 + " requires input 'test' that is not provided");

    actionRequest = new ExecuteActionRequest(cluster1, null, actionDef1, null, null, params, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Action " + actionDef1 + " requires input 'dirName' that is not provided");

    params.put("dirName", "dirName");
    actionRequest = new ExecuteActionRequest(cluster1, null, actionDef1, null, null, params, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Action " + actionDef1 + " requires explicit target host(s)");

    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("HIVE", null, null);
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest(cluster1, null, actionDef2, resourceFilters, null, params, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Action " + actionDef2 + " targets service HIVE that does not match with expected HDFS");

    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("HDFS", "HDFS_CLIENT", null);
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest(cluster1, null, actionDef2, resourceFilters, null, params, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Action " + actionDef2 + " targets component HDFS_CLIENT that does not match with expected DATANODE");

    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("HDFS2", "HDFS_CLIENT", null);
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest(cluster1, null, actionDef1, resourceFilters, null, params, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Service not found, clusterName=" + cluster1 + ", serviceName=HDFS2");

    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("HDFS", "HDFS_CLIENT2", null);
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest(cluster1, null, actionDef1, resourceFilters, null, params, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "ServiceComponent not found, clusterName=" + cluster1 + ", serviceName=HDFS, serviceComponentName=HDFS_CLIENT2");

    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("", "HDFS_CLIENT2", null);
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest(cluster1, null, actionDef1, resourceFilters, null, params, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Action " + actionDef1 + " targets component HDFS_CLIENT2 without specifying the target service");

    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("", "", null);
    resourceFilters.add(resourceFilter);

    // targets a service that is not a member of the stack (e.g. MR not in HDP-2)
    actionRequest = new ExecuteActionRequest(cluster1, null, actionDef3, resourceFilters, null, params, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Service not found, clusterName=" + cluster1 + ", serviceName=MAPREDUCE");

    hosts = new ArrayList<>();
    hosts.add("h6");
    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("HDFS", "", hosts);
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest(cluster1, null, actionDef2, resourceFilters, null, params, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Request specifies host h6 but it is not a valid host based on the target service=HDFS and component=DATANODE");

    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("HIVE", "", null);
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest(cluster1, null, actionDef4, resourceFilters, null, params, false);
    expectActionCreationErrorWithMessage(actionRequest, requestProperties,
        "Suitable hosts not found, component=, service=HIVE, cluster=" + cluster1 + ", actionName=" + actionDef4);

  }

  private void expectActionCreationErrorWithMessage(ExecuteActionRequest actionRequest,
                                                    Map<String, String> requestProperties,
                                                    String message) {
    try {
      RequestStatusResponse response = controller.createAction(actionRequest, requestProperties);
      Assert.fail("createAction should fail");
    } catch (Exception ex) {
      LOG.info(ex.getMessage());
      if (!ex.getMessage().contains(message)) {
        fail(String.format("Expected '%s' to contain '%s'", ex.getMessage(), message));
      }
    }
  }

  @SuppressWarnings("serial")
  @Test
  public void testCreateServiceCheckActions() throws Exception {
    final String cluster1 = getUniqueName();
    final String host1 = getUniqueName();
    final String host2 = getUniqueName();

    setupClusterWithHosts(cluster1, "HDP-0.1",
        new ArrayList<String>() {{
          add(host1);
          add(host2);
        }},
        "centos5");

    Cluster cluster = clusters.getCluster(cluster1);
    cluster.setDesiredStackVersion(new StackId("HDP-0.1"));
    cluster.setCurrentStackVersion(new StackId("HDP-0.1"));

    RepositoryVersionEntity repositoryVersion = repositoryVersion01;

    ConfigFactory cf = injector.getInstance(ConfigFactory.class);
    Config config1 = cf.createNew(cluster, "global", "version1",
        new HashMap<String, String>(){{ put("key1", "value1"); }}, new HashMap<>());
    config1.setPropertiesAttributes(new HashMap<String, Map<String, String>>(){{ put("attr1", new HashMap<>()); }});

    Config config2 = cf.createNew(cluster, "core-site", "version1",
        new HashMap<String, String>(){{ put("key1", "value1"); }}, new HashMap<>());
    config2.setPropertiesAttributes(new HashMap<String, Map<String, String>>(){{ put("attr2", new HashMap<>()); }});

    cluster.addDesiredConfig("_test", Collections.singleton(config1));
    cluster.addDesiredConfig("_test", Collections.singleton(config2));

    Service hdfs = cluster.addService("HDFS", repositoryVersion);
    Service mapReduce = cluster.addService("MAPREDUCE", repositoryVersion);

    hdfs.addServiceComponent(Role.HDFS_CLIENT.name());
    mapReduce.addServiceComponent(Role.MAPREDUCE_CLIENT.name());

    hdfs.getServiceComponent(Role.HDFS_CLIENT.name()).addServiceComponentHost(host1);
    mapReduce.getServiceComponent(Role.MAPREDUCE_CLIENT.name()).addServiceComponentHost(host2);

    Map<String, String> params = new HashMap<String, String>() {{
      put("test", "test");
    }};
    ExecuteActionRequest actionRequest = new ExecuteActionRequest(cluster1, Role.HDFS_SERVICE_CHECK.name(), params, false);
    RequestResourceFilter resourceFilter = new RequestResourceFilter("HDFS", null, null);
    actionRequest.getResourceFilters().add(resourceFilter);

    Map<String, String> requestProperties = new HashMap<>();
    requestProperties.put(REQUEST_CONTEXT_PROPERTY, "Called from a test");

    RequestStatusResponse response = controller.createAction(actionRequest, requestProperties);

    assertEquals(1, response.getTasks().size());
    ShortTaskStatus task = response.getTasks().get(0);

    List<HostRoleCommand> storedTasks = actionDB.getRequestTasks(response.getRequestId());
    Stage stage = actionDB.getAllStages(response.getRequestId()).get(0);

    //Check configs not stored with execution command
    ExecutionCommandDAO executionCommandDAO = injector.getInstance(ExecutionCommandDAO.class);
    ExecutionCommandEntity commandEntity = executionCommandDAO.findByPK(task.getTaskId());

    Gson gson = new Gson();
    ExecutionCommand executionCommand = gson.fromJson(new StringReader(
        new String(commandEntity.getCommand())), ExecutionCommand.class);

    assertTrue(executionCommand.getConfigurations() == null || executionCommand.getConfigurations().isEmpty());

    assertEquals(1, storedTasks.size());
    HostRoleCommand hostRoleCommand = storedTasks.get(0);

    assertEquals("SERVICE_CHECK HDFS", hostRoleCommand.getCommandDetail());
    assertNull(hostRoleCommand.getCustomCommandName());

    assertEquals(task.getTaskId(), hostRoleCommand.getTaskId());
    assertNotNull(actionRequest.getResourceFilters());
    RequestResourceFilter requestResourceFilter = actionRequest.getResourceFilters().get(0);
    assertEquals(resourceFilter.getServiceName(), hostRoleCommand.getExecutionCommandWrapper().getExecutionCommand().getServiceName());
    assertEquals(actionRequest.getClusterName(), hostRoleCommand.getExecutionCommandWrapper().getExecutionCommand().getClusterName());
    assertEquals(actionRequest.getCommandName(), hostRoleCommand.getExecutionCommandWrapper().getExecutionCommand().getRole());
    assertEquals(Role.HDFS_CLIENT.name(), hostRoleCommand.getEvent().getEvent().getServiceComponentName());
    assertEquals(actionRequest.getParameters(), hostRoleCommand.getExecutionCommandWrapper().getExecutionCommand().getRoleParams());
    assertNotNull(hostRoleCommand.getExecutionCommandWrapper().getExecutionCommand().getConfigurations());
    assertEquals(0, hostRoleCommand.getExecutionCommandWrapper().getExecutionCommand().getConfigurations().size());
    assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), stage.getRequestContext());
    assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());

    actionRequest = new ExecuteActionRequest(cluster1, Role.MAPREDUCE_SERVICE_CHECK.name(), null, false);
    resourceFilter = new RequestResourceFilter("MAPREDUCE", null, null);
    actionRequest.getResourceFilters().add(resourceFilter);

    injector.getInstance(ActionMetadata.class).addServiceCheckAction("MAPREDUCE");
    response = controller.createAction(actionRequest, requestProperties);

    assertEquals(1, response.getTasks().size());

    List<HostRoleCommand> tasks = actionDB.getRequestTasks(response.getRequestId());

    assertEquals(1, tasks.size());

    requestProperties.put(REQUEST_CONTEXT_PROPERTY, null);
    response = controller.createAction(actionRequest, requestProperties);

    assertEquals(1, response.getTasks().size());
    assertEquals("", response.getRequestContext());
  }



  @Test
  public void testUpdateConfigForRunningService() throws Exception {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    clusters.getCluster(cluster1)
            .setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");

    createServiceComponent(cluster1, serviceName, componentName1,
            State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2,
            State.INIT);
    createServiceComponent(cluster1, serviceName, componentName3,
            State.INIT);

    String host1 = getUniqueName();
    String host2 = getUniqueName();

    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);


    // null service should work
    createServiceComponentHost(cluster1, null, componentName1,
            host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
            host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
            host2, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
            host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
            host2, null);

    Assert.assertNotNull(clusters.getCluster(cluster1)
            .getService(serviceName)
            .getServiceComponent(componentName1)
            .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(cluster1)
            .getService(serviceName)
            .getServiceComponent(componentName2)
            .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(cluster1)
            .getService(serviceName)
            .getServiceComponent(componentName2)
            .getServiceComponentHost(host2));
    Assert.assertNotNull(clusters.getCluster(cluster1)
            .getService(serviceName)
            .getServiceComponent(componentName3)
            .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(cluster1)
            .getService(serviceName)
            .getServiceComponent(componentName3)
            .getServiceComponentHost(host2));

    // Install
    ServiceRequest r = new ServiceRequest(cluster1, serviceName, repositoryVersion01.getId(),
        State.INSTALLED.toString(), null);
    Set<ServiceRequest> requests = new HashSet<>();
    requests.add(r);

    ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true, false);
    Assert.assertEquals(State.INSTALLED,
            clusters.getCluster(cluster1).getService(serviceName)
                    .getDesiredState());

    // manually change live state to installed as no running action manager
    for (ServiceComponent sc :
            clusters.getCluster(cluster1).getService(serviceName)
                    .getServiceComponents().values()) {
        for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
            sch.setState(State.INSTALLED);
        }
    }

    // Start
    r = new ServiceRequest(cluster1, serviceName, repositoryVersion01.getId(),
        State.STARTED.toString(), null);
    requests.clear();
    requests.add(r);
    ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true, false);

    // manually change live state to started as no running action manager
    for (ServiceComponent sc :
            clusters.getCluster(cluster1).getService(serviceName)
                    .getServiceComponents().values()) {
        for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
            sch.setState(State.STARTED);
        }
    }

    Assert.assertEquals(State.STARTED,
            clusters.getCluster(cluster1).getService(serviceName)
                    .getDesiredState());
    for (ServiceComponent sc :
            clusters.getCluster(cluster1).getService(serviceName)
                    .getServiceComponents().values()) {
        if (sc.getName().equals("HDFS_CLIENT")) {
            Assert.assertEquals(State.INSTALLED, sc.getDesiredState());
        } else {
            Assert.assertEquals(State.STARTED, sc.getDesiredState());
        }
        for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
            if (sch.getServiceComponentName().equals("HDFS_CLIENT")) {
                Assert.assertEquals(State.INSTALLED, sch.getDesiredState());
            } else {
                Assert.assertEquals(State.STARTED, sch.getDesiredState());
            }
        }
    }

    Map<String, String> configs = new HashMap<>();
    configs.put("a", "b");

    ConfigurationRequest cr1, cr2, cr3, cr4, cr5, cr6, cr7, cr8;
    cr1 = new ConfigurationRequest(cluster1, "typeA","v1", configs, null);
    cr2 = new ConfigurationRequest(cluster1, "typeB","v1", configs, null);
    cr3 = new ConfigurationRequest(cluster1, "typeC","v1", configs, null);
    cr4 = new ConfigurationRequest(cluster1, "typeD","v1", configs, null);
    cr5 = new ConfigurationRequest(cluster1, "typeA","v2", configs, null);
    cr6 = new ConfigurationRequest(cluster1, "typeB","v2", configs, null);
    cr7 = new ConfigurationRequest(cluster1, "typeC","v2", configs, null);
    cr8 = new ConfigurationRequest(cluster1, "typeE","v1", configs, null);
    controller.createConfiguration(cr1);
    controller.createConfiguration(cr2);
    controller.createConfiguration(cr3);
    controller.createConfiguration(cr4);
    controller.createConfiguration(cr5);
    controller.createConfiguration(cr6);
    controller.createConfiguration(cr7);
    controller.createConfiguration(cr8);

    Cluster cluster = clusters.getCluster(cluster1);
    Service s = cluster.getService(serviceName);
    ServiceComponent sc1 = s.getServiceComponent(componentName1);
    ServiceComponent sc2 = s.getServiceComponent(componentName2);
    ServiceComponentHost sch1 = sc1.getServiceComponentHost(host1);

    Set<ServiceComponentHostRequest> schReqs =
      new HashSet<>();
    Set<ServiceComponentRequest> scReqs =
      new HashSet<>();
    Set<ServiceRequest> sReqs = new HashSet<>();
    Map<String, String> configVersions = new HashMap<>();

    // update configs at SCH and SC level
    configVersions.clear();
    configVersions.put("typeA", "v1");
    configVersions.put("typeB", "v1");
    configVersions.put("typeC", "v1");
    schReqs.clear();
    schReqs.add(new ServiceComponentHostRequest(cluster1, serviceName,
            componentName1, host1, null));
    Assert.assertNull(updateHostComponents(schReqs, Collections.emptyMap(), true));

    configVersions.clear();
    configVersions.put("typeC", "v1");
    configVersions.put("typeD", "v1");
    scReqs.clear();
    scReqs.add(new ServiceComponentRequest(cluster1, serviceName, componentName2, null));
    Assert.assertNull(ComponentResourceProviderTest.updateComponents(controller, scReqs, Collections.emptyMap(), true));

    // update configs at service level
    configVersions.clear();
    configVersions.put("typeA", "v2");
    configVersions.put("typeC", "v2");
    configVersions.put("typeE", "v1");
    sReqs.clear();
    sReqs.add(new ServiceRequest(cluster1, serviceName, repositoryVersion01.getId(), null, null));
    Assert.assertNull(ServiceResourceProviderTest.updateServices(controller, sReqs, mapRequestProps, true, false));


    // update configs at SCH level
    configVersions.clear();
    configVersions.put("typeA", "v1");
    configVersions.put("typeB", "v1");
    configVersions.put("typeC", "v1");
    schReqs.clear();
    schReqs.add(new ServiceComponentHostRequest(cluster1, serviceName,
            componentName1, host1, null));
    Assert.assertNull(updateHostComponents(schReqs, Collections.emptyMap(), true));

    // update configs at SC level
    configVersions.clear();
    configVersions.put("typeC", "v2");
    configVersions.put("typeD", "v1");
    scReqs.clear();
    scReqs.add(new ServiceComponentRequest(cluster1, serviceName,
            componentName1, null));
    Assert.assertNull(ComponentResourceProviderTest.updateComponents(controller, scReqs, Collections.emptyMap(), true));

  }

  @Test
  public void testConfigUpdates() throws Exception {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    clusters.getCluster(cluster1)
        .setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");

    createServiceComponent(cluster1, serviceName, componentName1,
        State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2,
        State.INIT);
    createServiceComponent(cluster1, serviceName, componentName3,
        State.INIT);

    String host1 = getUniqueName();
    String host2 = getUniqueName();

    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);


    // null service should work
    createServiceComponentHost(cluster1, null, componentName1,
        host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
        host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
        host2, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
        host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
        host2, null);

    Assert.assertNotNull(clusters.getCluster(cluster1)
        .getService(serviceName)
        .getServiceComponent(componentName1)
        .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(cluster1)
        .getService(serviceName)
        .getServiceComponent(componentName2)
        .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(cluster1)
        .getService(serviceName)
        .getServiceComponent(componentName2)
        .getServiceComponentHost(host2));
    Assert.assertNotNull(clusters.getCluster(cluster1)
        .getService(serviceName)
        .getServiceComponent(componentName3)
        .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(cluster1)
        .getService(serviceName)
        .getServiceComponent(componentName3)
        .getServiceComponentHost(host2));

    Map<String, String> configs = new HashMap<>();
    configs.put("a", "b");

    Map<String, Map<String, String>> configAttributes = new HashMap<>();
    configAttributes.put("final", new HashMap<>());
    configAttributes.get("final").put("a", "true");

    ConfigurationRequest cr1, cr2, cr3, cr4, cr5, cr6, cr7, cr8;
    cr1 = new ConfigurationRequest(cluster1, "typeA","v1", configs, configAttributes);
    cr2 = new ConfigurationRequest(cluster1, "typeB","v1", configs, configAttributes);
    cr3 = new ConfigurationRequest(cluster1, "typeC","v1", configs, configAttributes);
    cr4 = new ConfigurationRequest(cluster1, "typeD","v1", configs, configAttributes);
    cr5 = new ConfigurationRequest(cluster1, "typeA","v2", configs, configAttributes);
    cr6 = new ConfigurationRequest(cluster1, "typeB","v2", configs, configAttributes);
    cr7 = new ConfigurationRequest(cluster1, "typeC","v2", configs, configAttributes);
    cr8 = new ConfigurationRequest(cluster1, "typeE","v1", configs, configAttributes);
    controller.createConfiguration(cr1);
    controller.createConfiguration(cr2);
    controller.createConfiguration(cr3);
    controller.createConfiguration(cr4);
    controller.createConfiguration(cr5);
    controller.createConfiguration(cr6);
    controller.createConfiguration(cr7);
    controller.createConfiguration(cr8);

    Cluster cluster = clusters.getCluster(cluster1);
    Service s = cluster.getService(serviceName);
    ServiceComponent sc1 = s.getServiceComponent(componentName1);
    ServiceComponent sc2 = s.getServiceComponent(componentName2);
    ServiceComponentHost sch1 = sc1.getServiceComponentHost(host1);

    Set<ServiceComponentHostRequest> schReqs =
      new HashSet<>();
    Set<ServiceComponentRequest> scReqs =
      new HashSet<>();
    Set<ServiceRequest> sReqs = new HashSet<>();
    Map<String, String> configVersions = new HashMap<>();

    // update configs at SCH and SC level
    configVersions.clear();
    configVersions.put("typeA", "v1");
    configVersions.put("typeB", "v1");
    configVersions.put("typeC", "v1");
    schReqs.clear();
    schReqs.add(new ServiceComponentHostRequest(cluster1, serviceName,
        componentName1, host1, null));
    Assert.assertNull(updateHostComponents(schReqs, Collections.emptyMap(), true));

    configVersions.clear();
    configVersions.put("typeC", "v1");
    configVersions.put("typeD", "v1");
    scReqs.clear();
    scReqs.add(new ServiceComponentRequest(cluster1, serviceName,
        componentName2, null));
    Assert.assertNull(ComponentResourceProviderTest.updateComponents(controller, scReqs, Collections.emptyMap(), true));

    // update configs at service level
    configVersions.clear();
    configVersions.put("typeA", "v2");
    configVersions.put("typeC", "v2");
    configVersions.put("typeE", "v1");
    sReqs.clear();
    sReqs.add(new ServiceRequest(cluster1, serviceName, repositoryVersion01.getId(), null, null));
    Assert.assertNull(ServiceResourceProviderTest.updateServices(controller, sReqs, mapRequestProps, true, false));

    // update configs at SCH level
    configVersions.clear();
    configVersions.put("typeA", "v1");
    configVersions.put("typeB", "v1");
    configVersions.put("typeC", "v1");
    schReqs.clear();
    schReqs.add(new ServiceComponentHostRequest(cluster1, serviceName,
        componentName1, host1, null));
    Assert.assertNull(updateHostComponents(schReqs, Collections.emptyMap(), true));

    // update configs at SC level
    configVersions.clear();
    configVersions.put("typeC", "v2");
    configVersions.put("typeD", "v1");
    scReqs.clear();
    scReqs.add(new ServiceComponentRequest(cluster1, serviceName,
        componentName1, null));
    Assert.assertNull(ComponentResourceProviderTest.updateComponents(controller, scReqs, Collections.emptyMap(), true));

  }

  @Test
  public void testReConfigureService() throws Exception {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    clusters.getCluster(cluster1)
      .setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");

    createServiceComponent(cluster1, serviceName, componentName1,
      State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2,
      State.INIT);
    createServiceComponent(cluster1, serviceName, componentName3,
      State.INIT);

    String host1 = getUniqueName();
    String host2 = getUniqueName();

    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);


    // null service should work
    createServiceComponentHost(cluster1, null, componentName1,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
      host2, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
      host2, null);

    // Install
    ServiceRequest r = new ServiceRequest(cluster1, serviceName, repositoryVersion01.getId(),
      State.INSTALLED.toString());
    Set<ServiceRequest> requests = new HashSet<>();
    requests.add(r);

    ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true, false);
    Assert.assertEquals(State.INSTALLED,
      clusters.getCluster(cluster1).getService(serviceName)
        .getDesiredState());

    // manually change live state to installed as no running action manager
    for (ServiceComponent sc :
      clusters.getCluster(cluster1).getService(serviceName)
        .getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        sch.setState(State.INSTALLED);
      }
    }

    // Create and attach config
    Map<String, String> configs = new HashMap<>();
    configs.put("a", "b");

    ConfigurationRequest cr1,cr2,cr3;
    cr1 = new ConfigurationRequest(cluster1, "core-site","version1",
      configs, null);
    cr2 = new ConfigurationRequest(cluster1, "hdfs-site","version1",
      configs, null);
    cr3 = new ConfigurationRequest(cluster1, "core-site","version122",
      configs, null);
    controller.createConfiguration(cr1);
    controller.createConfiguration(cr2);
    controller.createConfiguration(cr3);

    Cluster cluster = clusters.getCluster(cluster1);
    Service s = cluster.getService(serviceName);
    ServiceComponent sc1 = s.getServiceComponent(componentName1);
    ServiceComponent sc2 = s.getServiceComponent(componentName2);
    ServiceComponentHost sch1 = sc1.getServiceComponentHost(host1);

    Set<ServiceComponentHostRequest> schReqs =
      new HashSet<>();
    Set<ServiceComponentRequest> scReqs =
      new HashSet<>();
    Set<ServiceRequest> sReqs = new HashSet<>();
    Map<String, String> configVersions = new HashMap<>();

    // SCH level
    configVersions.clear();
    configVersions.put("core-site", "version1");
    configVersions.put("hdfs-site", "version1");
    schReqs.clear();
    schReqs.add(new ServiceComponentHostRequest(cluster1, serviceName,
      componentName1, host1, null));
    Assert.assertNull(updateHostComponents(schReqs, Collections.emptyMap(), true));

    // Reconfigure SCH level
    configVersions.clear();
    configVersions.put("core-site", "version122");
    schReqs.clear();
    schReqs.add(new ServiceComponentHostRequest(cluster1, serviceName,
      componentName1, host1, null));
    Assert.assertNull(updateHostComponents(schReqs, Collections.emptyMap(), true));

    // Clear Entity Manager
    entityManager.clear();

    //SC Level
    configVersions.clear();
    configVersions.put("core-site", "version1");
    configVersions.put("hdfs-site", "version1");
    scReqs.add(new ServiceComponentRequest(cluster1, serviceName,
      componentName2, null));
    Assert.assertNull(ComponentResourceProviderTest.updateComponents(controller, scReqs, Collections.emptyMap(), true));

    scReqs.add(new ServiceComponentRequest(cluster1, serviceName,
      componentName1, null));
    Assert.assertNull(ComponentResourceProviderTest.updateComponents(controller, scReqs, Collections.emptyMap(), true));

    // Reconfigure SC level
    configVersions.clear();
    configVersions.put("core-site", "version122");

    scReqs.clear();
    scReqs.add(new ServiceComponentRequest(cluster1, serviceName,
      componentName2, null));
    Assert.assertNull(ComponentResourceProviderTest.updateComponents(controller, scReqs, Collections.emptyMap(), true));

    scReqs.clear();
    scReqs.add(new ServiceComponentRequest(cluster1, serviceName,
      componentName1, null));
    Assert.assertNull(ComponentResourceProviderTest.updateComponents(controller, scReqs, Collections.emptyMap(), true));

    entityManager.clear();

    // S level
    configVersions.clear();
    configVersions.put("core-site", "version1");
    configVersions.put("hdfs-site", "version1");
    sReqs.clear();
    sReqs.add(new ServiceRequest(cluster1, serviceName, repositoryVersion01.getId(), null));
    Assert.assertNull(ServiceResourceProviderTest.updateServices(controller, sReqs, mapRequestProps, true, false));

    // Reconfigure S Level
    configVersions.clear();
    configVersions.put("core-site", "version122");

    sReqs.clear();
    sReqs.add(new ServiceRequest(cluster1, serviceName, repositoryVersion01.getId(), null));
    Assert.assertNull(ServiceResourceProviderTest.updateServices(controller, sReqs, mapRequestProps, true, false));

    entityManager.clear();

  }

  @Test
  @Ignore // not actual because all configs/attributes/tags were moved to STOMP configurations topic
  public void testReConfigureServiceClient() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    Cluster cluster = clusters.getCluster(cluster1);
    cluster.setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName1 = "HDFS";
    String serviceName2 = "MAPREDUCE";
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";
    String componentName4 = "JOBTRACKER";
    String componentName5 = "TASKTRACKER";
    String componentName6 = "MAPREDUCE_CLIENT";

    createService(cluster1, serviceName1, repositoryVersion01, null);
    createService(cluster1, serviceName2, repositoryVersion01, null);

    createServiceComponent(cluster1, serviceName1, componentName1,
      State.INIT);
    createServiceComponent(cluster1, serviceName1, componentName2,
      State.INIT);
    createServiceComponent(cluster1, serviceName1, componentName3,
      State.INIT);
    createServiceComponent(cluster1, serviceName2, componentName4,
      State.INIT);
    createServiceComponent(cluster1, serviceName2, componentName5,
      State.INIT);
    createServiceComponent(cluster1, serviceName2, componentName6,
      State.INIT);

    String host1 = getUniqueName();
    String host2 = getUniqueName();
    String host3 = getUniqueName();

    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);
    addHostToCluster(host3, cluster1);

    createServiceComponentHost(cluster1, serviceName1, componentName1,
      host1, null);
    createServiceComponentHost(cluster1, serviceName1, componentName2,
      host1, null);
    createServiceComponentHost(cluster1, serviceName2, componentName4,
      host1, null);
    createServiceComponentHost(cluster1, serviceName2, componentName5,
      host1, null);
    createServiceComponentHost(cluster1, serviceName1, componentName2,
      host2, null);
    createServiceComponentHost(cluster1, serviceName1, componentName3,
      host2, null);
    createServiceComponentHost(cluster1, serviceName2, componentName6,
      host2, null);
    createServiceComponentHost(cluster1, serviceName1, componentName3,
      host3, null);
    createServiceComponentHost(cluster1, serviceName2, componentName6,
      host3, null);

    // Create and attach config
    Map<String, String> configs = new HashMap<>();
    configs.put("a", "b");
    Map<String, String> configs2 = new HashMap<>();
    configs2.put("c", "d");
    Map<String, String> configs3 = new HashMap<>();

    ConfigurationRequest cr1,cr2,cr3,cr4;
    cr1 = new ConfigurationRequest(cluster1, "core-site","version1",
      configs, null);
    cr2 = new ConfigurationRequest(cluster1, "hdfs-site","version1",
      configs, null);
    cr4 = new ConfigurationRequest(cluster1, "kerberos-env", "version1",
      configs3, null);

    ConfigFactory cf = injector.getInstance(ConfigFactory.class);
    Config config1 = cf.createNew(cluster, "kerberos-env", "version1",
      new HashMap<>(), new HashMap<>());

    ClusterRequest crReq = new ClusterRequest(cluster.getClusterId(), cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr1));
    controller.updateClusters(Collections.singleton(crReq), null);
    crReq = new ClusterRequest(cluster.getClusterId(), cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr2));
    controller.updateClusters(Collections.singleton(crReq), null);
    crReq = new ClusterRequest(cluster.getClusterId(), cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr4));
    controller.updateClusters(Collections.singleton(crReq), null);

    // Install
    long requestId1 = installService(cluster1, serviceName1, true, false);

    List<Stage> stages = actionDB.getAllStages(requestId1);

    installService(cluster1, serviceName2, false, false);

    // Start
    startService(cluster1, serviceName1, true, false);
    startService(cluster1, serviceName2, true, false);

    // Reconfigure
    cr3 = new ConfigurationRequest(cluster1, "core-site","version122",
        configs2, null);
    crReq = new ClusterRequest(cluster.getClusterId(), cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr3));
    controller.updateClusters(Collections.singleton(crReq), null);

    // Stop HDFS & MAPREDUCE
    stopService(cluster1, serviceName1, false, false);
    stopService(cluster1, serviceName2, false, false);

    // Start
    long requestId2 = startService(cluster1, serviceName1, true, true);
    long requestId3 = startService(cluster1, serviceName2, true, true);

    stages = new ArrayList<>();
    stages.addAll(actionDB.getAllStages(requestId2));
    stages.addAll(actionDB.getAllStages(requestId3));
    HostRoleCommand hdfsCmdHost3 = null;
    HostRoleCommand hdfsCmdHost2 = null;
    HostRoleCommand mapRedCmdHost2 = null;
    HostRoleCommand mapRedCmdHost3 = null;
    for (Stage stage : stages) {
      List<HostRoleCommand> hrcs = stage.getOrderedHostRoleCommands();

      for (HostRoleCommand hrc : hrcs) {
        LOG.debug("role: {}", hrc.getRole());
        if (hrc.getRole().toString().equals("HDFS_CLIENT")) {
          if (hrc.getHostName().equals(host3)) {
            hdfsCmdHost3 = hrc;
          } else if (hrc.getHostName().equals(host2)) {
            hdfsCmdHost2 = hrc;
          }
        }
        if (hrc.getRole().toString().equals("MAPREDUCE_CLIENT")) {
          if (hrc.getHostName().equals(host2)) {
            mapRedCmdHost2 = hrc;
          } else if (hrc.getHostName().equals(host3)) {
            mapRedCmdHost3 = hrc;
          }
        }
      }
    }
    Assert.assertNotNull(hdfsCmdHost3);
    Assert.assertNotNull(hdfsCmdHost2);
    ExecutionCommand execCmd = hdfsCmdHost3.getExecutionCommandWrapper()
      .getExecutionCommand();
    // Check if MapReduce client is reinstalled
    Assert.assertNotNull(mapRedCmdHost2);
    Assert.assertNotNull(mapRedCmdHost3);

    /*
     * Test for lost host
     */
    // Stop HDFS & MAPREDUCE
    stopService(cluster1, serviceName1, false, false);
    stopService(cluster1, serviceName2, false, false);

    clusters.getHost(host2).setState(HostState.HEARTBEAT_LOST);

    // Start MAPREDUCE, HDFS is started as a dependency
    requestId3 = startService(cluster1, serviceName2, true, true);
    stages = actionDB.getAllStages(requestId3);
    HostRoleCommand clientWithHostDown = null;
    for (Stage stage : stages) {
      for (HostRoleCommand hrc : stage.getOrderedHostRoleCommands()) {
        if (hrc.getRole().toString().equals("MAPREDUCE_CLIENT") && hrc
          .getHostName().equals(host2)) {
          clientWithHostDown = hrc;
        }
      }
    }
    Assert.assertNull(clientWithHostDown);

    Assert.assertEquals(State.STARTED, clusters.getCluster(cluster1).
      getService("MAPREDUCE").getServiceComponent("TASKTRACKER").
      getServiceComponentHost(host1).getState());
    Assert.assertEquals(State.STARTED, clusters.getCluster(cluster1).
      getService("HDFS").getServiceComponent("NAMENODE").
      getServiceComponentHost(host1).getState());
    Assert.assertEquals(State.STARTED, clusters.getCluster(cluster1).
      getService("HDFS").getServiceComponent("DATANODE").
      getServiceComponentHost(host1).getState());
  }

  @Test
  public void testReconfigureClientWithServiceStarted() throws Exception {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    Cluster cluster = clusters.getCluster(cluster1);
    cluster.setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(cluster1, serviceName, componentName1,
      State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2,
      State.INIT);
    createServiceComponent(cluster1, serviceName, componentName3,
      State.INIT);

    String host1 = getUniqueName();
    String host2 = getUniqueName();

    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);

    createServiceComponentHost(cluster1, serviceName, componentName1,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
      host2, null);

    // Create and attach config
    Map<String, String> configs = new HashMap<>();
    configs.put("a", "b");
    Map<String, String> configs2 = new HashMap<>();
    configs2.put("c", "d");

    ConfigurationRequest cr1,cr2,cr3;
    cr1 = new ConfigurationRequest(cluster1, "core-site","version1",
      configs, null);
    cr2 = new ConfigurationRequest(cluster1, "hdfs-site","version1",
      configs, null);

    ClusterRequest crReq = new ClusterRequest(cluster.getClusterId(), cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr1));
    controller.updateClusters(Collections.singleton(crReq), null);
    crReq = new ClusterRequest(cluster.getClusterId(), cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr2));
    controller.updateClusters(Collections.singleton(crReq), null);

    installService(cluster1, serviceName, false, false);
    startService(cluster1, serviceName, false, false);

    Cluster c = clusters.getCluster(cluster1);
    Service s = c.getService(serviceName);
    // Stop Sch only
    stopServiceComponentHosts(cluster1, serviceName);
    Assert.assertEquals(State.STARTED, s.getDesiredState());
    for (ServiceComponent sc : s.getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        Assert.assertEquals(State.INSTALLED, sch.getDesiredState());
      }
    }

    // Reconfigure
    cr3 = new ConfigurationRequest(cluster1, "core-site","version122",
      configs2, null);
    crReq = new ClusterRequest(cluster.getClusterId(), cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr3));
    controller.updateClusters(Collections.singleton(crReq), null);

    long id = startService(cluster1, serviceName, false, true);
    List<Stage> stages = actionDB.getAllStages(id);
    HostRoleCommand clientHrc = null;
    for (Stage stage : stages) {
      for (HostRoleCommand hrc : stage.getOrderedHostRoleCommands()) {
        if (hrc.getHostName().equals(host2) && hrc.getRole().toString()
          .equals("HDFS_CLIENT")) {
          clientHrc = hrc;
        }
      }
    }
    Assert.assertNotNull(clientHrc);
  }

  @Test
  public void testClientServiceSmokeTests() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    String serviceName = "PIG";
    createService(cluster1, serviceName, repositoryVersion01, null);
    String componentName1 = "PIG";
    createServiceComponent(cluster1, serviceName, componentName1, State.INIT);

    String host1 = getUniqueName();
    String host2 = getUniqueName();

    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);

    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");

    // null service should work
    createServiceComponentHost(cluster1, null, componentName1,
        host1, null);
    createServiceComponentHost(cluster1, null, componentName1,
        host2, null);

    ServiceRequest r = new ServiceRequest(cluster1, serviceName, repositoryVersion01.getId(),
        State.INSTALLED.toString());
    Set<ServiceRequest> requests = new HashSet<>();
    requests.add(r);

    RequestStatusResponse trackAction =
        ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true, false);
    Assert.assertEquals(State.INSTALLED,
        clusters.getCluster(cluster1).getService(serviceName)
        .getDesiredState());
    for (ServiceComponent sc :
      clusters.getCluster(cluster1).getService(serviceName)
      .getServiceComponents().values()) {
      Assert.assertEquals(State.INSTALLED, sc.getDesiredState());
      Assert.assertFalse(sc.isRecoveryEnabled()); // default value of recoveryEnabled
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        Assert.assertEquals(State.INSTALLED, sch.getDesiredState());
        Assert.assertEquals(State.INIT, sch.getState());
      }
    }

    List<ShortTaskStatus> taskStatuses = trackAction.getTasks();
    Assert.assertEquals(2, taskStatuses.size());

    List<Stage> stages = actionDB.getAllStages(trackAction.getRequestId());
    Assert.assertEquals(1, stages.size());
    Assert.assertEquals("Called from a test", stages.get(0).getRequestContext());

    for (ServiceComponent sc :
      clusters.getCluster(cluster1).getService(serviceName)
          .getServiceComponents().values()) {
      sc.setRecoveryEnabled(true);
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        sch.setState(State.INSTALLED);
      }
    }

    r = new ServiceRequest(cluster1, serviceName, repositoryVersion01.getId(), State.STARTED.toString());
    requests.clear();
    requests.add(r);

    injector.getInstance(ActionMetadata.class).addServiceCheckAction("PIG");
    trackAction = ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true, false);
    Assert.assertNotNull(trackAction);
    Assert.assertEquals(State.INSTALLED,
        clusters.getCluster(cluster1).getService(serviceName)
        .getDesiredState());
    for (ServiceComponent sc :
      clusters.getCluster(cluster1).getService(serviceName)
          .getServiceComponents().values()) {
      Assert.assertEquals(State.INSTALLED, sc.getDesiredState());
      Assert.assertTrue(sc.isRecoveryEnabled());
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        Assert.assertEquals(State.INSTALLED, sch.getDesiredState());
        Assert.assertEquals(State.INSTALLED, sch.getState());
      }
    }

    stages = actionDB.getAllStages(trackAction.getRequestId());
    for (Stage s : stages) {
      LOG.info("Stage dump : " + s);
    }
    Assert.assertEquals(1, stages.size());

    taskStatuses = trackAction.getTasks();
    Assert.assertEquals(1, taskStatuses.size());
    Assert.assertEquals(Role.PIG_SERVICE_CHECK.toString(),
        taskStatuses.get(0).getRole());
  }

  @Test
  public void testSkipTaskOnUnhealthyHosts() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    clusters.getCluster(cluster1)
        .setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";

    createServiceComponent(cluster1, serviceName, componentName1,
        State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2,
        State.INIT);

    String host1 = getUniqueName();
    String host2 = getUniqueName();
    String host3 = getUniqueName();

    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);
    addHostToCluster(host3, cluster1);

    createServiceComponentHost(cluster1, serviceName, componentName1,
        host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
        host2, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
        host3, null);

    // Install
    installService(cluster1, serviceName, false, false);

    // h1=HEALTHY, h2=HEARTBEAT_LOST, h3=WAITING_FOR_HOST_STATUS_UPDATES
    clusters.getHost(host1).setState(HostState.HEALTHY);
    clusters.getHost(host2).setState(HostState.HEALTHY);
    clusters.getHost(host3).setState(HostState.HEARTBEAT_LOST);

    long requestId = startService(cluster1, serviceName, true, false);
    List<HostRoleCommand> commands = actionDB.getRequestTasks(requestId);
    Assert.assertEquals(3, commands.size());
    int commandCount = 0;
    for (HostRoleCommand command : commands) {
      if (command.getRoleCommand() == RoleCommand.START) {
        Assert.assertTrue(command.getHostName().equals(host1) || command.getHostName().equals(host2));
        commandCount++;
      }
    }
    Assert.assertEquals("Expect only two task.", 2, commandCount);

    stopService(cluster1, serviceName, false, false);

    // h1=HEARTBEAT_LOST, h2=HEARTBEAT_LOST, h3=HEALTHY
    clusters.getHost(host1).setState(HostState.HEARTBEAT_LOST);
    clusters.getHost(host2).setState(HostState.HEARTBEAT_LOST);
    clusters.getHost(host3).setState(HostState.HEALTHY);

    requestId = startService(cluster1, serviceName, true, false);
    commands = actionDB.getRequestTasks(requestId);
    commandCount = 0;
    for (HostRoleCommand command : commands) {
      if (command.getRoleCommand() == RoleCommand.START) {
        Assert.assertTrue(command.getHostName().equals(host3));
        commandCount++;
      }
    }
    Assert.assertEquals("Expect only one task.", 1, commandCount);

    stopService(cluster1, serviceName, false, false);

    // h1=HEALTHY, h2=HEALTHY, h3=HEALTHY
    clusters.getHost(host1).setState(HostState.HEALTHY);
    clusters.getHost(host2).setState(HostState.HEALTHY);
    clusters.getHost(host3).setState(HostState.HEALTHY);

    requestId = startService(cluster1, serviceName, true, false);
    commands = actionDB.getRequestTasks(requestId);
    commandCount = 0;
    for (HostRoleCommand command : commands) {
      if (command.getRoleCommand() == RoleCommand.START) {
        Assert.assertTrue(command.getHostName().equals(host3) ||
            command.getHostName().equals(host2) ||
            command.getHostName().equals(host1));
        commandCount++;
      }
    }
    Assert.assertEquals("Expect all three task.", 3, commandCount);

    // h1=HEALTHY, h2=HEARTBEAT_LOST, h3=HEALTHY
    clusters.getHost(host2).setState(HostState.HEARTBEAT_LOST);
    requestId = stopService(cluster1, serviceName, false, false);
    commands = actionDB.getRequestTasks(requestId);
    Assert.assertEquals(2, commands.size());
    commandCount = 0;
    for (HostRoleCommand command : commands) {
      if (command.getRoleCommand() == RoleCommand.STOP) {
        Assert.assertTrue(command.getHostName().equals(host3) ||
            command.getHostName().equals(host1));
        commandCount++;
      }
    }
    Assert.assertEquals("Expect only two task.", 2, commandCount);

    // Force a sch into INSTALL_FAILED
    Cluster cluster = clusters.getCluster(cluster1);
    Service s = cluster.getService(serviceName);
    ServiceComponent sc3 = s.getServiceComponent(componentName2);
    for (ServiceComponentHost sch : sc3.getServiceComponentHosts().values()) {
      if (sch.getHostName().equals(host3)) {
        sch.setState(State.INSTALL_FAILED);
      }
    }

    // h1=HEALTHY, h2=HEALTHY, h3=HEARTBEAT_LOST
    clusters.getHost(host3).setState(HostState.HEARTBEAT_LOST);
    clusters.getHost(host2).setState(HostState.HEALTHY);
    requestId = installService(cluster1, serviceName, false, false);
    Assert.assertEquals(-1, requestId);

    // All healthy, INSTALL should succeed
    clusters.getHost(host3).setState(HostState.HEALTHY);
    requestId = installService(cluster1, serviceName, false, false);
    commands = actionDB.getRequestTasks(requestId);
    Assert.assertEquals(1, commands.size());
    commandCount = 0;
    for (HostRoleCommand command : commands) {
      if (command.getRoleCommand() == RoleCommand.INSTALL) {
        Assert.assertTrue(command.getHostName().equals(host3));
        commandCount++;
      }
    }
    Assert.assertEquals("Expect only one task.", 1, commandCount);
  }

  @Test
  public void testServiceCheckWhenHostIsUnhealthy() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    clusters.getCluster(cluster1).setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(cluster1, serviceName, componentName1,
        State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2,
        State.INIT);
    createServiceComponent(cluster1, serviceName, componentName3,
        State.INIT);

    String host1 = getUniqueName();
    String host2 = getUniqueName();
    String host3 = getUniqueName();

    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);
    addHostToCluster(host3, cluster1);

    createServiceComponentHost(cluster1, serviceName, componentName1,
        host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
        host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
        host2, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
        host3, null);

    // Install
    installService(cluster1, serviceName, false, false);
    clusters.getHost(host3).setState(HostState.UNHEALTHY);
    clusters.getHost(host2).setState(HostState.HEALTHY);

    // Start
    long requestId = startService(cluster1, serviceName, true, false);
    List<HostRoleCommand> commands = actionDB.getRequestTasks(requestId);
    int commandCount = 0;
    for(HostRoleCommand command : commands) {
      if(command.getRoleCommand() == RoleCommand.SERVICE_CHECK &&
          command.getRole() == Role.HDFS_SERVICE_CHECK) {
        Assert.assertTrue(command.getHostName().equals(host2));
        commandCount++;
      }
    }
    Assert.assertEquals("Expect only one service check.", 1, commandCount);

    stopService(cluster1, serviceName, false, false);

    clusters.getHost(host3).setState(HostState.HEALTHY);
    clusters.getHost(host2).setState(HostState.HEARTBEAT_LOST);

    requestId = startService(cluster1, serviceName, true, false);
    commands = actionDB.getRequestTasks(requestId);
    commandCount = 0;
    for(HostRoleCommand command : commands) {
      if(command.getRoleCommand() == RoleCommand.SERVICE_CHECK &&
          command.getRole() == Role.HDFS_SERVICE_CHECK) {
        Assert.assertTrue(command.getHostName().equals(host3));
        commandCount++;
      }
    }
    Assert.assertEquals("Expect only one service check.", 1, commandCount);

    RequestResourceFilter resourceFilter = new RequestResourceFilter("HDFS", null, null);
    ExecuteActionRequest actionRequest = new ExecuteActionRequest(cluster1, Role.HDFS_SERVICE_CHECK.name(), null, false);
    actionRequest.getResourceFilters().add(resourceFilter);
    Map<String, String> requestProperties = new HashMap<>();

    RequestStatusResponse response = controller.createAction(actionRequest, requestProperties);
    commands = actionDB.getRequestTasks(response.getRequestId());
    commandCount = 0;
    for(HostRoleCommand command : commands) {
      if(command.getRoleCommand() == RoleCommand.SERVICE_CHECK &&
          command.getRole() == Role.HDFS_SERVICE_CHECK) {
        Assert.assertTrue(command.getHostName().equals(host3));
        commandCount++;
      }
    }
    Assert.assertEquals("Expect only one service check.", 1, commandCount);

    // When both are unhealthy then it should raise an exception.
    clusters.getHost(host3).setState(HostState.WAITING_FOR_HOST_STATUS_UPDATES);
    clusters.getHost(host2).setState(HostState.INIT);
    try {
      controller.createAction(actionRequest, requestProperties);
      assertTrue("Exception should have been raised.", false);
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("there were no healthy eligible hosts"));
    }
  }

  @Test
  public void testReInstallForInstallFailedClient() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    clusters.getCluster(cluster1)
      .setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(cluster1, serviceName, componentName1,
      State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2,
      State.INIT);
    createServiceComponent(cluster1, serviceName, componentName3,
      State.INIT);

    String host1 = getUniqueName();
    String host2 = getUniqueName();
    String host3 = getUniqueName();

    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);
    addHostToCluster(host3, cluster1);

    createServiceComponentHost(cluster1, serviceName, componentName1,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
      host2, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
      host3, null);

    // Install
    installService(cluster1, serviceName, false, false);

    // Mark client as install failed.
    Cluster cluster = clusters.getCluster(cluster1);
    Service s = cluster.getService(serviceName);
    ServiceComponent sc3 = s.getServiceComponent(componentName3);
    for(ServiceComponentHost sch : sc3.getServiceComponentHosts().values()) {
      if (sch.getHostName().equals(host3)) {
        sch.setState(State.INSTALL_FAILED);
      }
    }

    // Start
    long requestId = startService(cluster1, serviceName, false, true);
    List<Stage> stages = actionDB.getAllStages(requestId);
    HostRoleCommand clientReinstallCmd = null;
    for (Stage stage : stages) {
      for (HostRoleCommand hrc : stage.getOrderedHostRoleCommands()) {
        if (hrc.getHostName().equals(host3) && hrc.getRole().toString()
          .equals("HDFS_CLIENT")) {
          clientReinstallCmd = hrc;
          break;
        }
      }
    }
    Assert.assertNotNull(clientReinstallCmd);
  }

  @Test
  public void testReInstallClientComponent() throws Exception {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    clusters.getCluster(cluster1)
      .setDesiredStackVersion(new StackId("HDP-2.0.6"));
    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(cluster1, serviceName, componentName1,
      State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2,
      State.INIT);
    createServiceComponent(cluster1, serviceName, componentName3,
      State.INIT);

    String host1 = getUniqueName();
    String host2 = getUniqueName();
    String host3 = getUniqueName();

    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);
    addHostToCluster(host3, cluster1);

    createServiceComponentHost(cluster1, serviceName, componentName1,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
      host2, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
      host3, null);

    // Install
    installService(cluster1, serviceName, false, false);

    // Reinstall SCH
    ServiceComponentHostRequest schr = new ServiceComponentHostRequest
      (cluster1, serviceName, componentName3, host3, State.INSTALLED.name());
    Set<ServiceComponentHostRequest> setReqs = new
      HashSet<>();
    setReqs.add(schr);
    RequestStatusResponse resp = updateHostComponents(setReqs,
      Collections.emptyMap(), false);

    Assert.assertNotNull(resp);
    Assert.assertTrue(resp.getRequestId() > 0);
    List<Stage> stages = actionDB.getAllStages(resp.getRequestId());
    HostRoleCommand clientReinstallCmd = null;
    for (Stage stage : stages) {
      for (HostRoleCommand hrc : stage.getOrderedHostRoleCommands()) {
        if (hrc.getHostName().equals(host3) && hrc.getRole().toString()
          .equals("HDFS_CLIENT")) {
          clientReinstallCmd = hrc;
          break;
        }
      }
    }
    Assert.assertNotNull(clientReinstallCmd);
  }

  @Test
  public void testReInstallClientComponentFromServiceChange() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    clusters.getCluster(cluster1)
      .setDesiredStackVersion(new StackId("HDP-2.0.6"));
    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);
    String componentName = "HDFS_CLIENT";

    createServiceComponent(cluster1, serviceName, componentName,
      State.INIT);

    String host1 = getUniqueName();
    String host2 = getUniqueName();

    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);

    createServiceComponentHost(cluster1, serviceName, componentName,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName,
      host2, null);

    // Install
    installService(cluster1, serviceName, false, false);

    // Start Service
    ServiceRequest sr = new ServiceRequest(
      cluster1, serviceName, repositoryVersion206.getId(), State.STARTED.name());
    Set<ServiceRequest> setReqs = new HashSet<>();
    setReqs.add(sr);
    RequestStatusResponse resp = ServiceResourceProviderTest.updateServices(controller,
      setReqs, Collections.emptyMap(), false, true);

    Assert.assertNotNull(resp);
    Assert.assertTrue(resp.getRequestId() > 0);

    List<Stage> stages = actionDB.getAllStages(resp.getRequestId());
    Map<String, Role> hostsToRoles = new HashMap<>();
    for (Stage stage : stages) {
      for (HostRoleCommand hrc : stage.getOrderedHostRoleCommands()) {
          hostsToRoles.put(hrc.getHostName(), hrc.getRole());
      }
    }

    Map<String, Role> expectedHostsToRoles = new HashMap<>();
    expectedHostsToRoles.put(host1, Role.HDFS_CLIENT);
    expectedHostsToRoles.put(host2, Role.HDFS_CLIENT);
    Assert.assertEquals(expectedHostsToRoles, hostsToRoles);
  }

  @Test
  public void testDecommissonDatanodeAction() throws Exception {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    Cluster cluster = clusters.getCluster(cluster1);
    cluster.setDesiredStackVersion(new StackId("HDP-2.0.7"));
    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(cluster1, serviceName, componentName1,
      State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2,
      State.INIT);
    createServiceComponent(cluster1, serviceName, componentName3,
      State.INIT);

    // !!! for whatever reason, the assertions are alphabetical to hostnames
    final String host1 = "d" + getUniqueName();
    final String host2 = "e" + getUniqueName();

    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);

    createServiceComponentHost(cluster1, serviceName, componentName1,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
      host2, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
      host2, null);

    RequestOperationLevel level = new RequestOperationLevel(
            Resource.Type.HostComponent, cluster1, null, null, null);

    // Install
    installService(cluster1, serviceName, false, false);

    // Create and attach config
    Map<String, String> configs = new HashMap<>();
    configs.put("a", "b");

    ConfigurationRequest cr1;
    cr1 = new ConfigurationRequest(cluster1, "hdfs-site","version1",
      configs, null);
    ClusterRequest crReq = new ClusterRequest(cluster.getClusterId(), cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr1));
    controller.updateClusters(Collections.singleton(crReq), null);

    // Start
    startService(cluster1, serviceName, false, false);

    cluster = clusters.getCluster(cluster1);
    Service s = cluster.getService(serviceName);
    Assert.assertEquals(State.STARTED, s.getDesiredState());
    ServiceComponentHost scHost = s.getServiceComponent("DATANODE").getServiceComponentHost(host2);
    Assert.assertEquals(HostComponentAdminState.INSERVICE, scHost.getComponentAdminState());

    // Decommission one datanode
    Map<String, String> params = new HashMap<String, String>(){{
      put("test", "test");
      put("excluded_hosts", host2);
      put("align_maintenance_state", "true");
    }};
    RequestResourceFilter resourceFilter = new RequestResourceFilter("HDFS", "NAMENODE", null);
    ArrayList<RequestResourceFilter> filters = new ArrayList<>();
    filters.add(resourceFilter);
    ExecuteActionRequest request = new ExecuteActionRequest(cluster1,
            "DECOMMISSION", null, filters, level, params, false);

    Map<String, String> requestProperties = new HashMap<>();
    requestProperties.put(REQUEST_CONTEXT_PROPERTY, "Called from a test");

    RequestStatusResponse response = controller.createAction(request,
      requestProperties);

    List<HostRoleCommand> storedTasks = actionDB.getRequestTasks(response.getRequestId());
    ExecutionCommand execCmd = storedTasks.get(0).getExecutionCommandWrapper
      ().getExecutionCommand();
    Assert.assertNotNull(storedTasks);
    Assert.assertEquals(1, storedTasks.size());
    Assert.assertEquals(HostComponentAdminState.DECOMMISSIONED, scHost.getComponentAdminState());
    Assert.assertEquals(MaintenanceState.ON, scHost.getMaintenanceState());
    HostRoleCommand command =  storedTasks.get(0);
    Assert.assertEquals(Role.NAMENODE, command.getRole());
    Assert.assertEquals(RoleCommand.CUSTOM_COMMAND, command.getRoleCommand());
    Assert.assertEquals("DECOMMISSION", execCmd.getCommandParams().get("custom_command"));
    Assert.assertEquals(host2, execCmd.getCommandParams().get("all_decommissioned_hosts"));
    Assert.assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());

    // Decommission the other datanode
    params = new HashMap<String, String>(){{
      put("test", "test");
      put("excluded_hosts", host1);
      put("align_maintenance_state", "true");
    }};
    resourceFilter = new RequestResourceFilter("HDFS", "NAMENODE", null);
    filters = new ArrayList<>();
    filters.add(resourceFilter);

    request = new ExecuteActionRequest(cluster1, "DECOMMISSION",
            null, filters, level, params, false);

    response = controller.createAction(request,
        requestProperties);

    storedTasks = actionDB.getRequestTasks(response.getRequestId());
    execCmd = storedTasks.get(0).getExecutionCommandWrapper
        ().getExecutionCommand();
    Map<String, String> cmdParams = execCmd.getCommandParams();
    Assert.assertTrue(cmdParams.containsKey("update_files_only"));
    Assert.assertTrue(cmdParams.get("update_files_only").equals("false"));
    Assert.assertNotNull(storedTasks);
    Assert.assertEquals(1, storedTasks.size());
    Assert.assertEquals(HostComponentAdminState.DECOMMISSIONED, scHost.getComponentAdminState());
    Assert.assertEquals(MaintenanceState.ON, scHost.getMaintenanceState());
    Assert.assertEquals("DECOMMISSION", execCmd.getCommandParams().get("custom_command"));
    Assert.assertTrue(execCmd.getCommandParams().get("all_decommissioned_hosts").contains(host1));
    Assert.assertTrue(execCmd.getCommandParams().get("all_decommissioned_hosts").contains(host2));
    Assert.assertTrue(execCmd.getCommandParams().get("all_decommissioned_hosts").equals(host1+","+host2) ||
      execCmd.getCommandParams().get("all_decommissioned_hosts").equals(host2+","+host1));
    Assert.assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());

    // Recommission the other datanode  (while adding NameNode HA)
    createServiceComponentHost(cluster1, serviceName, componentName1,
        host2, null);
    ServiceComponentHostRequest r = new ServiceComponentHostRequest(cluster1, serviceName,
        componentName1, host2, State.INSTALLED.toString());
    Set<ServiceComponentHostRequest> requests = new HashSet<>();
    requests.add(r);
    updateHostComponents(requests, Collections.emptyMap(), true);
    s.getServiceComponent(componentName1).getServiceComponentHost(host2).setState(State.INSTALLED);
    r = new ServiceComponentHostRequest(cluster1, serviceName,
        componentName1, host2, State.STARTED.toString());
    requests.clear();
    requests.add(r);
    updateHostComponents(requests, Collections.emptyMap(), true);
    s.getServiceComponent(componentName1).getServiceComponentHost(host2).setState(State.STARTED);

    params = new HashMap<String, String>(){{
      put("test", "test");
      put("included_hosts", host1 + " , " + host2);
      put("align_maintenance_state", "true");
    }};
    resourceFilter = new RequestResourceFilter("HDFS", "NAMENODE", null);
    filters = new ArrayList<>();
    filters.add(resourceFilter);
    request = new ExecuteActionRequest(cluster1, "DECOMMISSION", null,
            filters, level, params, false);

    response = controller.createAction(request,
        requestProperties);

    storedTasks = actionDB.getRequestTasks(response.getRequestId());
    Assert.assertNotNull(storedTasks);
    scHost = s.getServiceComponent("DATANODE").getServiceComponentHost(host2);
    Assert.assertEquals(HostComponentAdminState.INSERVICE, scHost.getComponentAdminState());
    Assert.assertEquals(MaintenanceState.OFF, scHost.getMaintenanceState());
    execCmd = storedTasks.get(0).getExecutionCommandWrapper().getExecutionCommand();
    Assert.assertNotNull(storedTasks);
    Assert.assertEquals(2, storedTasks.size());
    int countRefresh = 0;
    for(HostRoleCommand hrc : storedTasks) {
      Assert.assertEquals("DECOMMISSION", hrc.getCustomCommandName());
      // hostname order is not guaranteed
      Assert.assertTrue(hrc.getCommandDetail().contains("DECOMMISSION, Included: "));
      Assert.assertTrue(hrc.getCommandDetail().contains(host1));
      Assert.assertTrue(hrc.getCommandDetail().contains(host2));
      cmdParams = hrc.getExecutionCommandWrapper().getExecutionCommand().getCommandParams();
      if(!cmdParams.containsKey("update_files_only")
          || !cmdParams.get("update_files_only").equals("true")) {
        countRefresh++;
      }
      Assert.assertEquals("", cmdParams.get("all_decommissioned_hosts"));
    }
    Assert.assertEquals(2, countRefresh);

    // Slave components will have admin state as INSERVICE even if the state in DB is null
    scHost.setComponentAdminState(null);
    Assert.assertEquals(HostComponentAdminState.INSERVICE, scHost.getComponentAdminState());
    Assert.assertEquals(MaintenanceState.OFF, scHost.getMaintenanceState());
    Assert.assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());
  }

  @Test
  public void testResourceFiltersWithCustomActions() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();
    final String host1 = getUniqueName();
    final String host2 = getUniqueName();
    final String host3 = getUniqueName();

    setupClusterWithHosts(cluster1, "HDP-2.0.6",
      new ArrayList<String>() {{
        add(host1);
        add(host2);
        add(host3);
      }},
      "centos6");

    Cluster cluster = clusters.getCluster(cluster1);
    cluster.setDesiredStackVersion(new StackId("HDP-2.0.6"));
    cluster.setCurrentStackVersion(new StackId("HDP-2.0.6"));

    RepositoryVersionEntity repositoryVersion = repositoryVersion206;

    ConfigFactory cf = injector.getInstance(ConfigFactory.class);
    cf.createNew(cluster, "global", "version1",
      new HashMap<String, String>() {{
        put("key1", "value1");
      }}, new HashMap<>());

    cf.createNew(cluster, "core-site", "version1",
      new HashMap<String, String>() {{
        put("key1", "value1");
      }}, new HashMap<>());

    Service hdfs = cluster.addService("HDFS", repositoryVersion);
    Service mapred = cluster.addService("YARN", repositoryVersion);

    hdfs.addServiceComponent(Role.HDFS_CLIENT.name());
    hdfs.addServiceComponent(Role.NAMENODE.name());
    hdfs.addServiceComponent(Role.DATANODE.name());

    mapred.addServiceComponent(Role.RESOURCEMANAGER.name());

    hdfs.getServiceComponent(Role.HDFS_CLIENT.name()).addServiceComponentHost(host1);
    hdfs.getServiceComponent(Role.NAMENODE.name()).addServiceComponentHost(host1);
    hdfs.getServiceComponent(Role.DATANODE.name()).addServiceComponentHost(host1);
    hdfs.getServiceComponent(Role.DATANODE.name()).addServiceComponentHost(host2);

    String action1 = getUniqueName();

    controller.getAmbariMetaInfo().addActionDefinition(new ActionDefinition(
      action1, ActionType.SYSTEM, "", "HDFS", "", "Some custom action.",
      TargetHostType.ALL, Short.valueOf("10010"), null));

    Map<String, String> params = new HashMap<String, String>() {{
      put("test", "test");
    }};

    Map<String, String> requestProperties = new HashMap<>();
    requestProperties.put(REQUEST_CONTEXT_PROPERTY, "Called from a test");

    List<RequestResourceFilter> resourceFilters = new ArrayList<>();
    ArrayList<String> hosts = new ArrayList<String>() {{ add(host2); }};
    RequestResourceFilter resourceFilter1 = new RequestResourceFilter("HDFS", "DATANODE", hosts);

    hosts = new ArrayList<String>() {{ add(host1); }};
    RequestResourceFilter resourceFilter2 = new RequestResourceFilter("HDFS", "NAMENODE", hosts);

    resourceFilters.add(resourceFilter1);
    resourceFilters.add(resourceFilter2);

    ExecuteActionRequest actionRequest = new ExecuteActionRequest(cluster1, null, action1, resourceFilters, null, params, false);
    RequestStatusResponse response = null;
    try {
      controller.createAction(actionRequest, requestProperties);
    } catch (Exception ae) {
      LOG.info("Expected exception.", ae);
      Assert.assertTrue(ae.getMessage().contains("Custom action definition only " +
        "allows one resource filter to be specified"));
    }
    resourceFilters.remove(resourceFilter1);
    actionRequest = new ExecuteActionRequest(cluster1, null, action1, resourceFilters, null, params, false);
    response = controller.createAction(actionRequest, requestProperties);

    assertEquals(1, response.getTasks().size());
    HostRoleCommand nnCommand = null;

    for (HostRoleCommand hrc : actionDB.getRequestTasks(response.getRequestId())) {
      if (hrc.getHostName().equals(host1)) {
        nnCommand = hrc;
      }
    }

    Assert.assertNotNull(nnCommand);
    ExecutionCommand cmd = nnCommand.getExecutionCommandWrapper().getExecutionCommand();
    Assert.assertEquals(action1, cmd.getRole());
    Assert.assertEquals("10010", cmd.getCommandParams().get("command_timeout"));
    Type type = new TypeToken<Map<String, String>>(){}.getType();
    for (Stage stage : actionDB.getAllStages(response.getRequestId())){
      Map<String, String> commandParamsStage = StageUtils.getGson().fromJson(stage.getCommandParamsStage(), type);
      Assert.assertTrue(commandParamsStage.containsKey("test"));
    }
    Assert.assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());
  }

  @Test
  public void testResourceFiltersWithCustomCommands() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();
    final String host1 = getUniqueName();
    final String host2 = getUniqueName();
    final String host3 = getUniqueName();

    setupClusterWithHosts(cluster1, "HDP-2.0.6",
      new ArrayList<String>() {{
        add(host1);
        add(host2);
        add(host3);
      }},
      "centos6");

    Cluster cluster = clusters.getCluster(cluster1);
    cluster.setDesiredStackVersion(new StackId("HDP-2.0.6"));
    cluster.setCurrentStackVersion(new StackId("HDP-2.0.6"));

    RepositoryVersionEntity repositoryVersion = repositoryVersion206;

    ConfigFactory cf = injector.getInstance(ConfigFactory.class);
    Config config1 = cf.createNew(cluster, "global", "version1",
      new HashMap<String, String>() {{
        put("key1", "value1");
      }}, new HashMap<>());

    Config config2 = cf.createNew(cluster, "core-site", "version1",
      new HashMap<String, String>() {{
        put("key1", "value1");
      }}, new HashMap<>());

    Service hdfs = cluster.addService("HDFS", repositoryVersion);
    Service mapred = cluster.addService("YARN", repositoryVersion);

    hdfs.addServiceComponent(Role.HDFS_CLIENT.name());
    hdfs.addServiceComponent(Role.NAMENODE.name());
    hdfs.addServiceComponent(Role.DATANODE.name());

    mapred.addServiceComponent(Role.RESOURCEMANAGER.name());

    hdfs.getServiceComponent(Role.HDFS_CLIENT.name()).addServiceComponentHost(host1);
    hdfs.getServiceComponent(Role.NAMENODE.name()).addServiceComponentHost(host1);
    hdfs.getServiceComponent(Role.DATANODE.name()).addServiceComponentHost(host1);
    hdfs.getServiceComponent(Role.DATANODE.name()).addServiceComponentHost(host2);

    mapred.getServiceComponent(Role.RESOURCEMANAGER.name()).addServiceComponentHost(host2);

    Map<String, String> params = new HashMap<String, String>() {{
      put("test", "test");
    }};

    Map<String, String> requestProperties = new HashMap<>();
    requestProperties.put(REQUEST_CONTEXT_PROPERTY, "Called from a test");
    requestProperties.put("command_retry_enabled", "true");
    requestProperties.put("log_output", "false");

    // Test multiple restarts
    List<RequestResourceFilter> resourceFilters = new ArrayList<>();
    RequestResourceFilter resourceFilter = new RequestResourceFilter("HDFS",
      Role.DATANODE.name(), new ArrayList<String>() {{ add(host1); add(host2); }});
    resourceFilters.add(resourceFilter);
    resourceFilter = new RequestResourceFilter("YARN",
      Role.RESOURCEMANAGER.name(), new ArrayList<String>() {{ add(host2); }});
    resourceFilters.add(resourceFilter);

    ExecuteActionRequest request = new ExecuteActionRequest(cluster1, "RESTART", null, resourceFilters, null, params, false);

    RequestStatusResponse response = controller.createAction(request, requestProperties);
    Assert.assertEquals(3, response.getTasks().size());
    List<HostRoleCommand> storedTasks = actionDB.getRequestTasks(response.getRequestId());

    Assert.assertNotNull(storedTasks);
    int expectedRestartCount = 0;
    for (HostRoleCommand hrc : storedTasks) {
      Assert.assertEquals("RESTART", hrc.getCustomCommandName());

      Map<String, String> cParams = hrc.getExecutionCommandWrapper().getExecutionCommand().getCommandParams();
      Assert.assertEquals("Expect retry to be set", true, cParams.containsKey("command_retry_enabled"));
      Assert.assertEquals("Expect max duration to be set", true, cParams.containsKey("max_duration_for_retries"));
      Assert.assertEquals("Expect max duration to be 600", "600", cParams.get("max_duration_for_retries"));
      Assert.assertEquals("Expect retry to be true", "true", cParams.get("command_retry_enabled"));
      Assert.assertEquals("Expect log_output to be set", true, cParams.containsKey("log_output"));
      Assert.assertEquals("Expect log_output to be false", "false", cParams.get("log_output"));

      if (hrc.getHostName().equals(host1) && hrc.getRole().equals(Role.DATANODE)) {
        expectedRestartCount++;
      } else if(hrc.getHostName().equals(host2)) {
        if (hrc.getRole().equals(Role.DATANODE)) {
          expectedRestartCount++;
        } else if (hrc.getRole().equals(Role.RESOURCEMANAGER)) {
          expectedRestartCount++;
        }
      }
    }

    Assert.assertEquals("Restart 2 datanodes and 1 Resourcemanager.", 3,
        expectedRestartCount);
    Assert.assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());

    requestProperties.put("max_duration_for_retries", "423");
    response = controller.createAction(request, requestProperties);
    Assert.assertEquals(3, response.getTasks().size());
    storedTasks = actionDB.getRequestTasks(response.getRequestId());

    Assert.assertNotNull(storedTasks);
    for (HostRoleCommand hrc : storedTasks) {
      Assert.assertEquals("RESTART", hrc.getCustomCommandName());

      Map<String, String> cParams = hrc.getExecutionCommandWrapper().getExecutionCommand().getCommandParams();
      Assert.assertEquals("Expect retry to be set", true, cParams.containsKey("command_retry_enabled"));
      Assert.assertEquals("Expect max duration to be set", true, cParams.containsKey("max_duration_for_retries"));
      Assert.assertEquals("Expect max duration to be 423", "423", cParams.get("max_duration_for_retries"));
      Assert.assertEquals("Expect retry to be true", "true", cParams.get("command_retry_enabled"));
    }

    requestProperties.remove("max_duration_for_retries");
    requestProperties.remove("command_retry_enabled");
    response = controller.createAction(request, requestProperties);
    Assert.assertEquals(3, response.getTasks().size());
    storedTasks = actionDB.getRequestTasks(response.getRequestId());

    Assert.assertNotNull(storedTasks);
    for (HostRoleCommand hrc : storedTasks) {
      Assert.assertEquals("RESTART", hrc.getCustomCommandName());

      Map<String, String> cParams = hrc.getExecutionCommandWrapper().getExecutionCommand().getCommandParams();
      Assert.assertEquals("Expect retry to be set", false, cParams.containsKey("command_retry_enabled"));
      Assert.assertEquals("Expect max duration to be set", false, cParams.containsKey("max_duration_for_retries"));
    }

    // Test service checks - specific host
    resourceFilters.clear();
    resourceFilter = new RequestResourceFilter("HDFS", null,
      new ArrayList<String>() {{ add(host1); }});
    resourceFilters.add(resourceFilter);
    request = new ExecuteActionRequest(cluster1, Role.HDFS_SERVICE_CHECK.name(),
      null, resourceFilters, null, null, false);
    response = controller.createAction(request, requestProperties);

    Assert.assertEquals(1, response.getTasks().size());
    storedTasks = actionDB.getRequestTasks(response.getRequestId());
    Assert.assertNotNull(storedTasks);
    Assert.assertEquals(Role.HDFS_SERVICE_CHECK.name(),
        storedTasks.get(0).getRole().name());
    Assert.assertEquals(host1, storedTasks.get(0).getHostName());
    Assert.assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());

    Assert.assertEquals(State.STARTED, cluster.getService("HDFS").getServiceComponent(Role.DATANODE.name()).getServiceComponentHost(host1).getDesiredState());
    Assert.assertEquals(State.STARTED, cluster.getService("HDFS").getServiceComponent(Role.DATANODE.name()).getServiceComponentHost(host2).getDesiredState());
    Assert.assertEquals(State.STARTED, cluster.getService("YARN").getServiceComponent(Role.RESOURCEMANAGER.name()).getServiceComponentHost(host2).getDesiredState());
  }


  @Test
  public void testConfigsAttachedToServiceChecks() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    Cluster cluster = clusters.getCluster(cluster1);
    cluster.setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(cluster1, serviceName, componentName1,
      State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2,
      State.INIT);
    createServiceComponent(cluster1, serviceName, componentName3,
      State.INIT);

    String host1 = getUniqueName();
    String host2 = getUniqueName();

    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);


    // null service should work
    createServiceComponentHost(cluster1, null, componentName1,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
      host2, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
      host2, null);

    // Create and attach config
    Map<String, String> configs = new HashMap<>();
    configs.put("a", "b");

    ConfigurationRequest cr1,cr2;
    cr1 = new ConfigurationRequest(cluster1, "core-site","version1",
      configs, null);
    cr2 = new ConfigurationRequest(cluster1, "hdfs-site","version1",
      configs, null);

    ClusterRequest crReq = new ClusterRequest(cluster.getClusterId(), cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr1));
    controller.updateClusters(Collections.singleton(crReq), null);
    crReq = new ClusterRequest(cluster.getClusterId(), cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr2));
    controller.updateClusters(Collections.singleton(crReq), null);

    // Install
    installService(cluster1, serviceName, false, false);
    // Start
    long requestId = startService(cluster1, serviceName, true, false);

    List<Stage> stages = actionDB.getAllStages(requestId);
    boolean serviceCheckFound = false;
    for (Stage stage : stages) {
      for (HostRoleCommand hrc : stage.getOrderedHostRoleCommands()) {
        if (hrc.getRole().equals(Role.HDFS_SERVICE_CHECK)) {
          serviceCheckFound = true;
        }
      }
    }

    Type type = new TypeToken<Map<String, String>>(){}.getType();
    for (Stage stage : actionDB.getAllStages(requestId)){
      Map<String, String> hostParamsStage = StageUtils.getGson().fromJson(stage.getHostParamsStage(), type);
      Assert.assertNotNull(hostParamsStage.get("jdk_location"));
    }

    Assert.assertEquals(true, serviceCheckFound);
  }

  @Test
  @Ignore("Unsuported feature !")
  public void testConfigsAttachedToServiceNotCluster() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    clusters.getCluster(cluster1).setDesiredStackVersion(new StackId("HDP-0.1"));

    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(cluster1, serviceName, componentName1,
      State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2,
      State.INIT);
    createServiceComponent(cluster1, serviceName, componentName3,
      State.INIT);

    String host1 = getUniqueName();
    String host2 = getUniqueName();

    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);


    // null service should work
    createServiceComponentHost(cluster1, null, componentName1,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
      host2, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
      host2, null);

    // Create and attach config
    Map<String, String> configs = new HashMap<>();
    configs.put("a", "b");

    ConfigurationRequest cr1,cr2;
    cr1 = new ConfigurationRequest(cluster1, "core-site","version1",
      configs, null);
    cr2 = new ConfigurationRequest(cluster1, "hdfs-site","version1",
      configs, null);

    // create, but don't assign
    controller.createConfiguration(cr1);
    controller.createConfiguration(cr2);

    Map<String,String> configVersions = new HashMap<String,String>() {{
      put("core-site", "version1");
      put("hdfs-site", "version1");
    }};
    ServiceRequest sr = new ServiceRequest(cluster1, serviceName, repositoryVersion01.getId(), null);
    ServiceResourceProviderTest.updateServices(controller, Collections.singleton(sr), new HashMap<>(), false, false);

    // Install
    installService(cluster1, serviceName, false, false);
    // Start
    long requestId = startService(cluster1, serviceName, true, false);

    Assert.assertEquals(0, clusters.getCluster(cluster1).getDesiredConfigs().size());

    List<Stage> stages = actionDB.getAllStages(requestId);
    boolean serviceCheckFound = false;
    for (Stage stage : stages) {
      for (HostRoleCommand hrc : stage.getOrderedHostRoleCommands()) {
        if (hrc.getRole().equals(Role.HDFS_SERVICE_CHECK)) {
          serviceCheckFound = true;
        }
      }
    }
    Assert.assertEquals(true, serviceCheckFound);
  }

  @Test
  public void testHostLevelParamsSentWithCommands() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    String serviceName = "PIG";
    createService(cluster1, serviceName, repositoryVersion01, null);
    String componentName1 = "PIG";
    createServiceComponent(cluster1, serviceName, componentName1,
      State.INIT);

    String host1 = getUniqueName();
    String host2 = getUniqueName();

    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);

    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");

    // null service should work
    createServiceComponentHost(cluster1, null, componentName1,
      host1, null);
    createServiceComponentHost(cluster1, null, componentName1,
      host2, null);



    ServiceRequest r = new ServiceRequest(cluster1, serviceName, repositoryVersion01.getId(),
        State.INSTALLED.toString());
    Set<ServiceRequest> requests = new HashSet<>();
    requests.add(r);

    RequestStatusResponse trackAction =
      ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true, false);
    Assert.assertEquals(State.INSTALLED,
      clusters.getCluster(cluster1).getService(serviceName)
        .getDesiredState());

    List<Stage> stages = actionDB.getAllStages(trackAction.getRequestId());
    Type type = new TypeToken<Map<String, String>>(){}.getType();

    for (Stage stage : stages){
      Map<String, String> params = StageUtils.getGson().fromJson(stage.getHostParamsStage(), type);
      Assert.assertEquals("0.1", params.get("stack_version"));
      Assert.assertNotNull(params.get("jdk_location"));
      Assert.assertNotNull(params.get("db_name"));
      Assert.assertNotNull(params.get("mysql_jdbc_url"));
      Assert.assertNotNull(params.get("oracle_jdbc_url"));
    }

    ExecutionCommand executionCommand = stages.get(0).getOrderedHostRoleCommands().get(
        0).getExecutionCommandWrapper().getExecutionCommand();

    Map<String, String> paramsCmd = executionCommand.getHostLevelParams();
    Assert.assertNotNull(executionCommand.getRepositoryFile());
    Assert.assertNotNull(paramsCmd.get("clientsToUpdateConfigs"));
  }

  @Test
  @Ignore // not actual because all configs/attributes/tags were moved to STOMP configurations topic
  public void testConfigGroupOverridesWithHostActions() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    Cluster cluster = clusters.getCluster(cluster1);
    cluster.setDesiredStackVersion(new StackId("HDP-2.0.6"));
    String serviceName1 = "HDFS";
    String serviceName2 = "MAPREDUCE2";
    createService(cluster1, serviceName1, repositoryVersion206, null);
    createService(cluster1, serviceName2, repositoryVersion206, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";
    String componentName4 = "HISTORYSERVER";

    createServiceComponent(cluster1, serviceName1, componentName1, State.INIT);
    createServiceComponent(cluster1, serviceName1, componentName2, State.INIT);
    createServiceComponent(cluster1, serviceName1, componentName3, State.INIT);
    createServiceComponent(cluster1, serviceName2, componentName4, State.INIT);

    final String host1 = getUniqueName();
    final String host2 = getUniqueName();
    final String host3 = getUniqueName();

    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);
    addHostToCluster(host3, cluster1);

    createServiceComponentHost(cluster1, serviceName1, componentName1, host1, null);
    createServiceComponentHost(cluster1, serviceName1, componentName2, host2, null);
    createServiceComponentHost(cluster1, serviceName1, componentName3, host2, null);
    createServiceComponentHost(cluster1, serviceName1, componentName3, host3, null);
    createServiceComponentHost(cluster1, serviceName2, componentName4, host3, null);

    // Create and attach config
    Map<String, String> configs = new HashMap<>();
    configs.put("a", "b");

    ConfigurationRequest cr1,cr2,cr3;
    cr1 = new ConfigurationRequest(cluster1, "core-site", "version1", configs, null);
    cr2 = new ConfigurationRequest(cluster1, "hdfs-site", "version1", configs, null);
    cr3 = new ConfigurationRequest(cluster1, "mapred-site", "version1", configs, null);

    ClusterRequest crReq = new ClusterRequest(cluster.getClusterId(), cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr1));
    controller.updateClusters(Collections.singleton(crReq), null);
    crReq = new ClusterRequest(cluster.getClusterId(), cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr2));
    controller.updateClusters(Collections.singleton(crReq), null);
    crReq = new ClusterRequest(cluster.getClusterId(), cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr3));
    controller.updateClusters(Collections.singleton(crReq), null);

    String group1 = getUniqueName();
    String tag1 = getUniqueName();
    String group2 = getUniqueName();
    String tag2 = getUniqueName();

    ConfigFactory configFactory = injector.getInstance(ConfigFactory.class);

    // Create Config group for core-site
    configs = new HashMap<>();
    configs.put("a", "c");
    cluster = clusters.getCluster(cluster1);
    final Config config = configFactory.createReadOnly("core-site", "version122", configs, null);
    Long groupId = createConfigGroup(cluster, serviceName1, group1, tag1,
      new ArrayList<String>() {{ add(host1); }},
      new ArrayList<Config>() {{ add(config); }});

    Assert.assertNotNull(groupId);

    // Create Config group for mapred-site
    configs = new HashMap<>();
    configs.put("a", "c");

    final Config config2 =  configFactory.createReadOnly("mapred-site", "version122", configs, null);
    groupId = createConfigGroup(cluster, serviceName2, group2, tag2,
      new ArrayList<String>() {{ add(host1); }},
      new ArrayList<Config>() {{ add(config2); }});

    Assert.assertNotNull(groupId);

    // Install
    Long requestId = installService(cluster1, serviceName1, false, false);
    HostRoleCommand namenodeInstall = null;
    HostRoleCommand clientInstall = null;
    HostRoleCommand slaveInstall = null;
    for (Stage stage : actionDB.getAllStages(requestId)) {
      for (HostRoleCommand hrc : stage.getOrderedHostRoleCommands()) {
        if (hrc.getRole().equals(Role.NAMENODE) && hrc.getHostName().equals(host1)) {
          namenodeInstall = hrc;
        } else if (hrc.getRole().equals(Role.HDFS_CLIENT) && hrc.getHostName()
            .equals(host3)) {
          clientInstall = hrc;
        } else if (hrc.getRole().equals(Role.DATANODE) && hrc.getHostName()
            .equals(host2)) {
          slaveInstall = hrc;
        }
      }
    }

    Assert.assertNotNull(namenodeInstall);
    Assert.assertNotNull(clientInstall);
    Assert.assertNotNull(slaveInstall);
    Assert.assertTrue(namenodeInstall.getExecutionCommandWrapper()
      .getExecutionCommand().getConfigurations().get("core-site").containsKey("a"));
    Assert.assertEquals("c", namenodeInstall.getExecutionCommandWrapper()
      .getExecutionCommand().getConfigurations().get("core-site").get("a"));

    // Slave and client should not have the override
    Assert.assertTrue(clientInstall.getExecutionCommandWrapper()
      .getExecutionCommand().getConfigurations().get("core-site").containsKey("a"));
    Assert.assertEquals("b", clientInstall.getExecutionCommandWrapper()
      .getExecutionCommand().getConfigurations().get("core-site").get("a"));
    Assert.assertTrue(slaveInstall.getExecutionCommandWrapper()
      .getExecutionCommand().getConfigurations().get("core-site").containsKey("a"));
    Assert.assertEquals("b", slaveInstall.getExecutionCommandWrapper()
      .getExecutionCommand().getConfigurations().get("core-site").get("a"));

    startService(cluster1, serviceName1, false, false);

    requestId = installService(cluster1, serviceName2, false, false);
    HostRoleCommand mapredInstall = null;
    for (Stage stage : actionDB.getAllStages(requestId)) {
      for (HostRoleCommand hrc : stage.getOrderedHostRoleCommands()) {
        if (hrc.getRole().equals(Role.HISTORYSERVER) && hrc.getHostName()
            .equals(host3)) {
          mapredInstall = hrc;
        }
      }
    }
    Assert.assertNotNull(mapredInstall);
    // Config group not associated with host
    Assert.assertEquals("b", mapredInstall.getExecutionCommandWrapper()
      .getExecutionCommand().getConfigurations().get("mapred-site").get("a"));

    // Associate the right host
    ConfigGroup configGroup = cluster.getConfigGroups().get(groupId);
    configGroup.setHosts(new HashMap<Long, Host>() {{ put(3L,
      clusters.getHost(host3)); }});

    requestId = startService(cluster1, serviceName2, false, false);
    mapredInstall = null;
    for (Stage stage : actionDB.getAllStages(requestId)) {
      for (HostRoleCommand hrc : stage.getOrderedHostRoleCommands()) {
        if (hrc.getRole().equals(Role.HISTORYSERVER) && hrc.getHostName()
            .equals(host3)) {
          mapredInstall = hrc;
        }
      }
    }
    Assert.assertNotNull(mapredInstall);
    Assert.assertEquals("c", mapredInstall.getExecutionCommandWrapper()
      .getExecutionCommand().getConfigurations().get("mapred-site").get("a"));

  }

  @Test
  public void testConfigGroupOverridesWithDecommissionDatanode() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    Cluster cluster = clusters.getCluster(cluster1);
    cluster.setDesiredStackVersion(new StackId("HDP-2.0.7"));
    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(cluster1, serviceName, componentName1,
        State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2,
        State.INIT);
    createServiceComponent(cluster1, serviceName, componentName3,
        State.INIT);

    final String host1 = getUniqueName();
    final String host2 = getUniqueName();

    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);

    createServiceComponentHost(cluster1, serviceName, componentName1,
        host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
        host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
        host2, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
        host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
        host2, null);

    // Install
    installService(cluster1, serviceName, false, false);

    // Create and attach config
    Map<String, String> configs = new HashMap<>();
    configs.put("a", "b");

    ConfigurationRequest cr1, cr2;
    cr1 = new ConfigurationRequest(cluster1, "hdfs-site", "version1",
        configs, null);
    ClusterRequest crReq = new ClusterRequest(cluster.getClusterId(), cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr1));
    controller.updateClusters(Collections.singleton(crReq), null);

    // Start
    startService(cluster1, serviceName, false, false);

    // Create Config group for hdfs-site
    configs = new HashMap<>();
    configs.put("a", "c");

    String group1 = getUniqueName();
    String tag1 = getUniqueName();

    ConfigFactory configFactory = injector.getInstance(ConfigFactory.class);
    final Config config = configFactory.createReadOnly("hdfs-site", "version122", configs, null);
    Long groupId = createConfigGroup(clusters.getCluster(cluster1), serviceName, group1, tag1,
        new ArrayList<String>() {{
          add(host1);
          add(host2);
        }},
        new ArrayList<Config>() {{
          add(config);
        }}
    );

    Assert.assertNotNull(groupId);

    cluster = clusters.getCluster(cluster1);
    Service s = cluster.getService(serviceName);
    Assert.assertEquals(State.STARTED, s.getDesiredState());

    Map<String, String> params = new HashMap<String, String>() {{
      put("test", "test");
      put("excluded_hosts", host1);
    }};
    RequestResourceFilter resourceFilter = new RequestResourceFilter("HDFS", "NAMENODE", null);
    ExecuteActionRequest request = new ExecuteActionRequest(cluster1, "DECOMMISSION", params, false);
    request.getResourceFilters().add(resourceFilter);

    Map<String, String> requestProperties = new HashMap<>();
    requestProperties.put(REQUEST_CONTEXT_PROPERTY, "Called from a test");

    RequestStatusResponse response = controller.createAction(request,
        requestProperties);

    List<HostRoleCommand> storedTasks = actionDB.getRequestTasks(response.getRequestId());
    ExecutionCommand execCmd = storedTasks.get(0).getExecutionCommandWrapper
        ().getExecutionCommand();
    Assert.assertNotNull(storedTasks);
    Assert.assertEquals(1, storedTasks.size());
    HostRoleCommand command =  storedTasks.get(0);
    Assert.assertEquals(Role.NAMENODE, command.getRole());
    Assert.assertEquals(RoleCommand.CUSTOM_COMMAND, command.getRoleCommand());
    Assert.assertEquals("DECOMMISSION", execCmd.getCommandParams().get("custom_command"));
    Assert.assertEquals(host1, execCmd.getCommandParams().get("all_decommissioned_hosts"));
    Assert.assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());
  }

  @Test
  @Ignore // not actual because all configs/attributes/tags were moved to STOMP configurations topic
  public void testConfigGroupOverridesWithServiceCheckActions() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    Cluster cluster = clusters.getCluster(cluster1);
    cluster.setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(cluster1, serviceName, componentName1,
      State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2,
      State.INIT);
    createServiceComponent(cluster1, serviceName, componentName3,
      State.INIT);

    final String host1 = getUniqueName();
    final String host2 = getUniqueName();

    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);

    // null service should work
    createServiceComponentHost(cluster1, null, componentName1,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
      host2, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
      host2, null);

    // Create and attach config
    Map<String, String> configs = new HashMap<>();
    configs.put("a", "b");

    ConfigurationRequest cr1,cr2;
    cr1 = new ConfigurationRequest(cluster1, "core-site","version1",
      configs, null);
    cr2 = new ConfigurationRequest(cluster1, "hdfs-site","version1",
      configs, null);

    ClusterRequest crReq = new ClusterRequest(cluster.getClusterId(), cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr1));
    controller.updateClusters(Collections.singleton(crReq), null);
    crReq = new ClusterRequest(cluster.getClusterId(), cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr2));
    controller.updateClusters(Collections.singleton(crReq), null);

    // Install
    installService(cluster1, serviceName, false, false);

    String group1 = getUniqueName();
    String tag1 = getUniqueName();

    // Create Config group for hdfs-site
    configs = new HashMap<>();
    configs.put("a", "c");

    ConfigFactory configFactory = injector.getInstance(ConfigFactory.class);
    final Config config = configFactory.createReadOnly("hdfs-site", "version122", configs, null);
    Long groupId = createConfigGroup(clusters.getCluster(cluster1), serviceName, group1, tag1,
      new ArrayList<String>() {{ add(host1); add(host2); }},
      new ArrayList<Config>() {{ add(config); }});

    Assert.assertNotNull(groupId);

    // Start
    long requestId = startService(cluster1, serviceName, true, false);
    HostRoleCommand smokeTestCmd = null;
    List<Stage> stages = actionDB.getAllStages(requestId);
    for (Stage stage : stages) {
      for (HostRoleCommand hrc : stage.getOrderedHostRoleCommands()) {
        if (hrc.getRole().equals(Role.HDFS_SERVICE_CHECK)) {
          smokeTestCmd = hrc;
        }
      }
    }
    Assert.assertNotNull(smokeTestCmd);
    Assert.assertEquals("c", smokeTestCmd.getExecutionCommandWrapper()
      .getExecutionCommand().getConfigurations().get("hdfs-site").get("a"));
  }

  @Test
  public void testGetStacks() throws Exception {

    HashSet<String> availableStacks = new HashSet<>();
    for (StackInfo stackInfo: ambariMetaInfo.getStacks()){
      availableStacks.add(stackInfo.getName());
    }

    StackRequest request = new StackRequest(null);
    Set<StackResponse> responses = controller.getStacks(Collections.singleton(request));
    Assert.assertEquals(availableStacks.size(), responses.size());

    StackRequest requestWithParams = new StackRequest(STACK_NAME);
    Set<StackResponse> responsesWithParams = controller.getStacks(Collections.singleton(requestWithParams));
    Assert.assertEquals(1, responsesWithParams.size());
    for (StackResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getStackName(), STACK_NAME);

    }

    StackRequest invalidRequest = new StackRequest(NON_EXT_VALUE);
    try {
      controller.getStacks(Collections.singleton(invalidRequest));
    } catch (StackAccessException e) {
      // do nothing
    }
  }

  @Test
  public void testGetStackVersions() throws Exception {


    StackVersionRequest request = new StackVersionRequest(STACK_NAME, null);
    Set<StackVersionResponse> responses = controller.getStackVersions(Collections.singleton(request));
    Assert.assertEquals(STACK_VERSIONS_CNT, responses.size());

    StackVersionRequest requestWithParams = new StackVersionRequest(STACK_NAME, STACK_VERSION);
    Set<StackVersionResponse> responsesWithParams = controller.getStackVersions(Collections.singleton(requestWithParams));
    Assert.assertEquals(1, responsesWithParams.size());
    for (StackVersionResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getStackVersion(), STACK_VERSION);
    }

    StackVersionRequest invalidRequest = new StackVersionRequest(STACK_NAME, NON_EXT_VALUE);
    try {
      controller.getStackVersions(Collections.singleton(invalidRequest));
    } catch (StackAccessException e) {
      // do nothing
    }

    // test that a stack response has upgrade packs
    requestWithParams = new StackVersionRequest(STACK_NAME, "2.1.1");
    responsesWithParams = controller.getStackVersions(Collections.singleton(requestWithParams));

    Assert.assertEquals(1, responsesWithParams.size());
    StackVersionResponse resp = responsesWithParams.iterator().next();
    assertNotNull(resp.getUpgradePacks());
    assertTrue(resp.getUpgradePacks().size() > 0);
    assertTrue(resp.getUpgradePacks().contains("upgrade_test"));
  }

  @Test
  public void testGetStackVersionActiveAttr() throws Exception {

    for (StackInfo stackInfo: ambariMetaInfo.getStacks(STACK_NAME)) {
      if (stackInfo.getVersion().equalsIgnoreCase(STACK_VERSION)) {
        stackInfo.setActive(true);
      }
    }

    StackVersionRequest requestWithParams = new StackVersionRequest(STACK_NAME, STACK_VERSION);
    Set<StackVersionResponse> responsesWithParams = controller.getStackVersions(Collections.singleton(requestWithParams));
    Assert.assertEquals(1, responsesWithParams.size());
    for (StackVersionResponse responseWithParams: responsesWithParams) {
      Assert.assertTrue(responseWithParams.isActive());
    }
  }

  @Test
  public void testGetRepositories() throws Exception {

    RepositoryRequest request = new RepositoryRequest(STACK_NAME, STACK_VERSION, OS_TYPE, null, REPO_NAME);
    Set<RepositoryResponse> responses = controller.getRepositories(Collections.singleton(request));
    Assert.assertEquals(REPOS_CNT, responses.size());

    RepositoryRequest requestWithParams = new RepositoryRequest(STACK_NAME, STACK_VERSION, OS_TYPE, REPO_ID, REPO_NAME);
    requestWithParams.setClusterVersionId(525L);
    Set<RepositoryResponse> responsesWithParams = controller.getRepositories(Collections.singleton(requestWithParams));
    Assert.assertEquals(1, responsesWithParams.size());
    for (RepositoryResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getRepoId(), REPO_ID);
      Assert.assertEquals(525L, responseWithParams.getClusterVersionId().longValue());
    }

    RepositoryRequest invalidRequest = new RepositoryRequest(STACK_NAME, STACK_VERSION, OS_TYPE, NON_EXT_VALUE, REPO_NAME);
    try {
      controller.getRepositories(Collections.singleton(invalidRequest));
    } catch (StackAccessException e) {
      // do nothing
    }
  }


  @Test
  public void testGetStackServices() throws Exception {
    StackServiceRequest request = new StackServiceRequest(STACK_NAME, NEW_STACK_VERSION, null);
    Set<StackServiceResponse> responses = controller.getStackServices(Collections.singleton(request));
    Assert.assertEquals(12, responses.size());


    StackServiceRequest requestWithParams = new StackServiceRequest(STACK_NAME, NEW_STACK_VERSION, SERVICE_NAME);
    Set<StackServiceResponse> responsesWithParams = controller.getStackServices(Collections.singleton(requestWithParams));
    Assert.assertEquals(1, responsesWithParams.size());
    for (StackServiceResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getServiceName(), SERVICE_NAME);
      Assert.assertTrue(responseWithParams.getConfigTypes().size() > 0);
    }


    StackServiceRequest invalidRequest = new StackServiceRequest(STACK_NAME, NEW_STACK_VERSION, NON_EXT_VALUE);
    try {
      controller.getStackServices(Collections.singleton(invalidRequest));
    } catch (StackAccessException e) {
      // do nothing
    }
  }

  @Test
  public void testConfigInComponent() throws Exception {
    StackServiceRequest requestWithParams = new StackServiceRequest(STACK_NAME, "2.0.6", "YARN");
    Set<StackServiceResponse> responsesWithParams = controller.getStackServices(Collections.singleton(requestWithParams));

    Assert.assertEquals(1, responsesWithParams.size());

    for (StackServiceResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getServiceName(), "YARN");
      Assert.assertTrue(responseWithParams.getConfigTypes().containsKey("capacity-scheduler"));
    }
  }

  @Test
  public void testGetStackConfigurations() throws Exception {
    StackConfigurationRequest request = new StackConfigurationRequest(STACK_NAME, STACK_VERSION, SERVICE_NAME, null);
    Set<StackConfigurationResponse> responses = controller.getStackConfigurations(Collections.singleton(request));
    Assert.assertEquals(STACK_PROPERTIES_CNT, responses.size());


    StackConfigurationRequest requestWithParams = new StackConfigurationRequest(STACK_NAME, STACK_VERSION, SERVICE_NAME, PROPERTY_NAME);
    Set<StackConfigurationResponse> responsesWithParams = controller.getStackConfigurations(Collections.singleton(requestWithParams));
    Assert.assertEquals(1, responsesWithParams.size());
    for (StackConfigurationResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getPropertyName(), PROPERTY_NAME);

    }

    StackConfigurationRequest invalidRequest = new StackConfigurationRequest(STACK_NAME, STACK_VERSION, SERVICE_NAME, NON_EXT_VALUE);
    try {
      controller.getStackConfigurations(Collections.singleton(invalidRequest));
    } catch (StackAccessException e) {
      // do nothing
    }
  }


  @Test
  public void testGetStackComponents() throws Exception {
    StackServiceComponentRequest request = new StackServiceComponentRequest(STACK_NAME, STACK_VERSION, SERVICE_NAME, null);
    Set<StackServiceComponentResponse> responses = controller.getStackComponents(Collections.singleton(request));
    Assert.assertEquals(STACK_COMPONENTS_CNT, responses.size());


    StackServiceComponentRequest requestWithParams = new StackServiceComponentRequest(STACK_NAME, STACK_VERSION, SERVICE_NAME, COMPONENT_NAME);
    Set<StackServiceComponentResponse> responsesWithParams = controller.getStackComponents(Collections.singleton(requestWithParams));
    Assert.assertEquals(1, responsesWithParams.size());
    for (StackServiceComponentResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getComponentName(), COMPONENT_NAME);

    }

    StackServiceComponentRequest invalidRequest = new StackServiceComponentRequest(STACK_NAME, STACK_VERSION, SERVICE_NAME, NON_EXT_VALUE);
    try {
      controller.getStackComponents(Collections.singleton(invalidRequest));
    } catch (StackAccessException e) {
      // do nothing
    }
  }

  @Test
  public void testGetStackOperatingSystems() throws Exception {
    OperatingSystemRequest request = new OperatingSystemRequest(STACK_NAME, STACK_VERSION, null);
    Set<OperatingSystemResponse> responses = controller.getOperatingSystems(Collections.singleton(request));
    Assert.assertEquals(OS_CNT, responses.size());


    OperatingSystemRequest requestWithParams = new OperatingSystemRequest(STACK_NAME, STACK_VERSION, OS_TYPE);
    Set<OperatingSystemResponse> responsesWithParams = controller.getOperatingSystems(Collections.singleton(requestWithParams));
    Assert.assertEquals(1, responsesWithParams.size());
    for (OperatingSystemResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getOsType(), OS_TYPE);

    }

    OperatingSystemRequest invalidRequest = new OperatingSystemRequest(STACK_NAME, STACK_VERSION, NON_EXT_VALUE);
    try {
      controller.getOperatingSystems(Collections.singleton(invalidRequest));
    } catch (StackAccessException e) {
      // do nothing
    }
  }

  @Test
  public void testGetStackOperatingSystemsWithRepository() throws Exception {
    RepositoryVersionDAO dao = injector.getInstance(RepositoryVersionDAO.class);
    StackDAO stackDAO = injector.getInstance(StackDAO.class);
    StackEntity stackEntity = stackDAO.find(STACK_NAME, STACK_VERSION);
    assertNotNull(stackEntity);

    RepositoryVersionEntity versionEntity = dao.create(stackEntity, "0.2.2", "HDP-0.2", ClusterStackVersionResourceProviderTest.REPO_OS_ENTITIES);

    OperatingSystemRequest request = new OperatingSystemRequest(STACK_NAME, STACK_VERSION, null);
    Set<OperatingSystemResponse> responses = controller.getOperatingSystems(Collections.singleton(request));
    Assert.assertEquals(OS_CNT, responses.size());

    OperatingSystemRequest requestWithParams = new OperatingSystemRequest(STACK_NAME, STACK_VERSION, OS_TYPE);
    requestWithParams.setVersionDefinitionId(versionEntity.getId().toString());

    Set<OperatingSystemResponse> responsesWithParams = controller.getOperatingSystems(Collections.singleton(requestWithParams));
    Assert.assertEquals(1, responsesWithParams.size());

  }

  @Test
  public void testStackServiceCheckSupported() throws Exception {
    StackServiceRequest hdfsServiceRequest = new StackServiceRequest(
        STACK_NAME, "2.0.8", SERVICE_NAME);

    Set<StackServiceResponse> responses = controller.getStackServices(Collections.singleton(hdfsServiceRequest));
    Assert.assertEquals(1, responses.size());

    StackServiceResponse response = responses.iterator().next();
    assertTrue(response.isServiceCheckSupported());

    StackServiceRequest fakeServiceRequest = new StackServiceRequest(
        STACK_NAME, "2.0.8", FAKE_SERVICE_NAME);

    responses = controller.getStackServices(Collections.singleton(fakeServiceRequest));
    Assert.assertEquals(1, responses.size());

    response = responses.iterator().next();
    assertFalse(response.isServiceCheckSupported());
  }

  @Test
  public void testStackServiceComponentCustomCommands() throws Exception {
    StackServiceComponentRequest namenodeRequest = new StackServiceComponentRequest(
        STACK_NAME, NEW_STACK_VERSION, SERVICE_NAME, COMPONENT_NAME);

    Set<StackServiceComponentResponse> responses = controller.getStackComponents(Collections.singleton(namenodeRequest));
    Assert.assertEquals(1, responses.size());

    StackServiceComponentResponse response = responses.iterator().next();
    assertNotNull(response.getCustomCommands());
    assertEquals(2, response.getCustomCommands().size());
    assertEquals("DECOMMISSION", response.getCustomCommands().get(0));
    assertEquals("REBALANCEHDFS", response.getCustomCommands().get(1));

    StackServiceComponentRequest journalNodeRequest = new StackServiceComponentRequest(
        STACK_NAME, NEW_STACK_VERSION, SERVICE_NAME, "JOURNALNODE");

    responses = controller.getStackComponents(Collections.singleton(journalNodeRequest));
    Assert.assertEquals(1, responses.size());

    response = responses.iterator().next();
    assertNotNull(response.getCustomCommands());
    assertEquals(0, response.getCustomCommands().size());
  }

  @Test
  public void testDecommissionAllowed() throws Exception{
    StackServiceComponentRequest requestWithParams = new StackServiceComponentRequest(STACK_NAME, NEW_STACK_VERSION, SERVICE_NAME_HBASE, COMPONENT_NAME_REGIONSERVER);
    Set<StackServiceComponentResponse> responsesWithParams = controller.getStackComponents(Collections.singleton(requestWithParams));
    for (StackServiceComponentResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getComponentName(), COMPONENT_NAME_REGIONSERVER);
      Assert.assertTrue(responseWithParams.isDecommissionAlllowed());
    }
  }

  @Test
  public void testDecommissionAllowedInheritance() throws Exception{
    //parent has it, child doesn't
    StackServiceComponentRequest requestWithParams = new StackServiceComponentRequest(STACK_NAME, NEW_STACK_VERSION, SERVICE_NAME, COMPONENT_NAME_DATANODE);
    Set<StackServiceComponentResponse> responsesWithParams = controller.getStackComponents(Collections.singleton(requestWithParams));
    for (StackServiceComponentResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getComponentName(), COMPONENT_NAME_DATANODE);
      Assert.assertTrue(responseWithParams.isDecommissionAlllowed());
    }
  }

  @Test
  public void testDecommissionAllowedOverwrite() throws Exception{
    StackServiceComponentRequest requestWithParams = new StackServiceComponentRequest(STACK_NAME, "2.0.5", SERVICE_NAME_YARN, COMPONENT_NAME_NODEMANAGER);
    Set<StackServiceComponentResponse> responsesWithParams = controller.getStackComponents(Collections.singleton(requestWithParams));

    //parent has it
    for (StackServiceComponentResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getComponentName(), COMPONENT_NAME_NODEMANAGER);
      Assert.assertFalse(responseWithParams.isDecommissionAlllowed());
    }

    requestWithParams = new StackServiceComponentRequest(STACK_NAME, NEW_STACK_VERSION, SERVICE_NAME_YARN, COMPONENT_NAME_NODEMANAGER);
    responsesWithParams = controller.getStackComponents(Collections.singleton(requestWithParams));
    //parent has it, child overwrites it
    for (StackServiceComponentResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getComponentName(), COMPONENT_NAME_NODEMANAGER);
      Assert.assertTrue(responseWithParams.isDecommissionAlllowed());
    }
  }

  @Test
  public void testRassignAllowed() throws Exception{
    StackServiceComponentRequest requestWithParams = new StackServiceComponentRequest(STACK_NAME, "2.0.5", SERVICE_NAME, COMPONENT_NAME);
    Set<StackServiceComponentResponse> responsesWithParams = controller.getStackComponents(Collections.singleton(requestWithParams));
    for (StackServiceComponentResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getComponentName(), COMPONENT_NAME);
      Assert.assertTrue(responseWithParams.isReassignAlllowed());
    }

    requestWithParams = new StackServiceComponentRequest(STACK_NAME, "2.0.5", SERVICE_NAME, COMPONENT_NAME_DATANODE);
    responsesWithParams = controller.getStackComponents(Collections.singleton(requestWithParams));
    for (StackServiceComponentResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getComponentName(), COMPONENT_NAME_DATANODE);
      Assert.assertFalse(responseWithParams.isReassignAlllowed());
    }
  }

  @Test
  public void testReassignAllowedInheritance() throws Exception{
    //parent has it, child doesn't
    StackServiceComponentRequest requestWithParams = new StackServiceComponentRequest(STACK_NAME, NEW_STACK_VERSION, SERVICE_NAME_HIVE, COMPONENT_NAME_HIVE_METASTORE);
    Set<StackServiceComponentResponse> responsesWithParams = controller.getStackComponents(Collections.singleton(requestWithParams));
    for (StackServiceComponentResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getComponentName(), COMPONENT_NAME_HIVE_METASTORE);
      Assert.assertTrue(responseWithParams.isReassignAlllowed());
    }
  }

  @Test
  public void testReassignAllowedOverwrite() throws Exception{
    StackServiceComponentRequest requestWithParams = new StackServiceComponentRequest(STACK_NAME, "2.0.5", SERVICE_NAME_HIVE, COMPONENT_NAME_HIVE_SERVER);
    Set<StackServiceComponentResponse> responsesWithParams = controller.getStackComponents(Collections.singleton(requestWithParams));

    //parent has it
    for (StackServiceComponentResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getComponentName(), COMPONENT_NAME_HIVE_SERVER);
      Assert.assertTrue(responseWithParams.isReassignAlllowed());
    }

    requestWithParams = new StackServiceComponentRequest(STACK_NAME, NEW_STACK_VERSION, SERVICE_NAME_HIVE, COMPONENT_NAME_HIVE_SERVER);
    responsesWithParams = controller.getStackComponents(Collections.singleton(requestWithParams));
    //parent has it, child overwrites it
    for (StackServiceComponentResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getComponentName(), COMPONENT_NAME_HIVE_SERVER);
      Assert.assertFalse(responseWithParams.isReassignAlllowed());
    }
  }

  @Test
  public void testBulkCommandsInheritence() throws Exception{
    //HDP 2.0.6 inherit HDFS configurations from HDP 2.0.5
    StackServiceComponentRequest requestWithParams = new StackServiceComponentRequest(STACK_NAME, NEW_STACK_VERSION, SERVICE_NAME, COMPONENT_NAME_DATANODE);
    Set<StackServiceComponentResponse> responsesWithParams = controller.getStackComponents(Collections.singleton(requestWithParams));
    Assert.assertEquals(1, responsesWithParams.size());
    for (StackServiceComponentResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getComponentName(), COMPONENT_NAME_DATANODE);
      Assert.assertEquals(responseWithParams.getBulkCommandsDisplayName(), "DataNodes");
      Assert.assertEquals(responseWithParams.getBulkCommandsMasterComponentName(), "NAMENODE");
    }
  }

  @Test
  public void testBulkCommandsChildStackOverride() throws Exception{
    //Both HDP 2.0.6 and HDP 2.0.5 has HBase configurations
    StackServiceComponentRequest requestWithParams = new StackServiceComponentRequest(STACK_NAME, "2.0.5", SERVICE_NAME_HBASE, COMPONENT_NAME_REGIONSERVER);
    Set<StackServiceComponentResponse> responsesWithParams = controller.getStackComponents(Collections.singleton(requestWithParams));
    Assert.assertEquals(1, responsesWithParams.size());
    for (StackServiceComponentResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getBulkCommandsDisplayName(), "Region Servers");
      Assert.assertEquals(responseWithParams.getBulkCommandsMasterComponentName(), "HBASE_MASTER");
    }

    requestWithParams = new StackServiceComponentRequest(STACK_NAME, NEW_STACK_VERSION, SERVICE_NAME_HBASE, COMPONENT_NAME_REGIONSERVER);
    responsesWithParams = controller.getStackComponents(Collections.singleton(requestWithParams));
    Assert.assertEquals(1, responsesWithParams.size());
    for (StackServiceComponentResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getBulkCommandsDisplayName(), "HBase Region Servers");
      Assert.assertEquals(responseWithParams.getBulkCommandsMasterComponentName(), "HBASE_MASTER");
    }
  }

  @Test
  public void testUpdateClusterUpgradabilityCheck() throws Exception, AuthorizationException {
    String cluster1 = getUniqueName();
    StackId currentStackId = new StackId("HDP-0.2");

    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");

    createCluster(cluster1);
    Cluster c = clusters.getCluster(cluster1);
    c.setDesiredStackVersion(currentStackId);
    ClusterRequest r = new ClusterRequest(c.getClusterId(), cluster1, "HDP-0.3", null);
    try {
      controller.updateClusters(Collections.singleton(r), mapRequestProps);
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().contains("Illegal request to upgrade to"));
    }

    StackId unsupportedStackId = new StackId("HDP-2.2.0");
    c.setDesiredStackVersion(unsupportedStackId);
    c.setCurrentStackVersion(unsupportedStackId);
    c.refresh();
    r = new ClusterRequest(c.getClusterId(), cluster1, "HDP-0.2", null);
    try {
      controller.updateClusters(Collections.singleton(r), mapRequestProps);
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().contains("Upgrade is not allowed from"));
    }
  }

  private void validateGeneratedStages(List<Stage> stages, int expectedStageCount, ExpectedUpgradeTasks expectedTasks) {
    Assert.assertEquals(expectedStageCount, stages.size());
    int prevRoleOrder = -1;
    for (Stage stage : stages) {
      int currRoleOrder = -1;
      for (HostRoleCommand command : stage.getOrderedHostRoleCommands()) {
        if(command.getRole() == Role.AMBARI_SERVER_ACTION) {
          Assert.assertTrue(command.toString(), expectedTasks.isTaskExpected(command.getRole()));
          currRoleOrder = expectedTasks.getRoleOrder(command.getRole());
          ExecutionCommand execCommand = command.getExecutionCommandWrapper().getExecutionCommand();
          Assert.assertTrue(
              execCommand.getRoleParams().containsKey(ServerAction.ACTION_NAME));
          Assert.assertEquals(RoleCommand.EXECUTE, execCommand.getRoleCommand());
        } else {
          Assert.assertTrue(command.toString(), expectedTasks.isTaskExpected(command.getRole(), command.getHostName()));
          currRoleOrder = expectedTasks.getRoleOrder(command.getRole());
          ExecutionCommand execCommand = command.getExecutionCommandWrapper().getExecutionCommand();
          Assert.assertTrue(execCommand.getCommandParams().containsKey("source_stack_version"));
          Assert.assertTrue(execCommand.getCommandParams().containsKey("target_stack_version"));
          Assert.assertEquals(RoleCommand.UPGRADE, execCommand.getRoleCommand());
        }
      }

      List<HostRoleCommand> commands = stage.getOrderedHostRoleCommands();
      Assert.assertTrue(commands.size() > 0);
      Role role = commands.get(0).getRole();
      for (HostRoleCommand command : commands) {
        Assert.assertTrue("All commands must be for the same role", role.equals(command.getRole()));
      }

      Assert.assertTrue("Roles must be in order", currRoleOrder > prevRoleOrder);
      prevRoleOrder = currRoleOrder;
    }
  }

  class ExpectedUpgradeTasks {
    private static final int ROLE_COUNT = 25;
    private static final String DEFAULT_HOST = "default_host";
    private ArrayList<Map<String, Boolean>> expectedList;
    private Map<Role, Integer> roleToIndex;

    public ExpectedUpgradeTasks(List<String> hosts) {
      roleToIndex = new HashMap<>();
      expectedList = new ArrayList<>(ROLE_COUNT);

      fillRoleToIndex();
      fillExpectedHosts(hosts);
    }

    public void expectTask(Role role, String host) {
      expectedList.get(roleToIndex.get(role)).put(host, true);
    }

    public void expectTask(Role role) {
      Assert.assertEquals(Role.AMBARI_SERVER_ACTION, role);
      expectTask(role, DEFAULT_HOST);
    }

    public boolean isTaskExpected(Role role, String host) {
      return expectedList.get(roleToIndex.get(role)).get(host);
    }

    public boolean isTaskExpected(Role role) {
      Assert.assertEquals(Role.AMBARI_SERVER_ACTION, role);
      return isTaskExpected(role, DEFAULT_HOST);
    }

    public int getRoleOrder(Role role) {
      return roleToIndex.get(role);
    }

    public void resetAll() {
      for (Role role : roleToIndex.keySet()) {
        Map<String, Boolean> hostState = expectedList.get(roleToIndex.get(role));
        for (String host : hostState.keySet()) {
          hostState.put(host, false);
        }
      }
    }

    private void fillExpectedHosts(List<String> hosts) {
      for (int index = 0; index < ROLE_COUNT; index++) {
        Map<String, Boolean> hostState = new HashMap<>();
        for (String host : hosts) {
          hostState.put(host, false);
        }
        expectedList.add(hostState);
      }
    }

    private void fillRoleToIndex() {
      roleToIndex.put(Role.NAMENODE, 0);
      roleToIndex.put(Role.SECONDARY_NAMENODE, 1);
      roleToIndex.put(Role.DATANODE, 2);
      roleToIndex.put(Role.HDFS_CLIENT, 3);
      roleToIndex.put(Role.JOBTRACKER, 4);
      roleToIndex.put(Role.TASKTRACKER, 5);
      roleToIndex.put(Role.MAPREDUCE_CLIENT, 6);
      roleToIndex.put(Role.ZOOKEEPER_SERVER, 7);
      roleToIndex.put(Role.ZOOKEEPER_CLIENT, 8);
      roleToIndex.put(Role.HBASE_MASTER, 9);

      roleToIndex.put(Role.HBASE_REGIONSERVER, 10);
      roleToIndex.put(Role.HBASE_CLIENT, 11);
      roleToIndex.put(Role.HIVE_SERVER, 12);
      roleToIndex.put(Role.HIVE_METASTORE, 13);
      roleToIndex.put(Role.HIVE_CLIENT, 14);
      roleToIndex.put(Role.HCAT, 15);
      roleToIndex.put(Role.OOZIE_SERVER, 16);
      roleToIndex.put(Role.OOZIE_CLIENT, 17);
      roleToIndex.put(Role.WEBHCAT_SERVER, 18);
      roleToIndex.put(Role.PIG, 19);

      roleToIndex.put(Role.SQOOP, 20);
      roleToIndex.put(Role.GANGLIA_SERVER, 21);
      roleToIndex.put(Role.GANGLIA_MONITOR, 22);
      roleToIndex.put(Role.AMBARI_SERVER_ACTION, 23);
    }
  }

  @Test
  public void testServiceStopWhileStopping() throws Exception {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    clusters.getCluster(cluster1)
      .setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";
    createServiceComponent(cluster1, serviceName, componentName1,
      State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2,
      State.INIT);
    createServiceComponent(cluster1, serviceName, componentName3,
      State.INIT);

    final String host1 = getUniqueName();
    final String host2 = getUniqueName();

    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);

    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");

    // null service should work
    createServiceComponentHost(cluster1, null, componentName1,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
      host2, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
      host2, null);

    Assert.assertNotNull(clusters.getCluster(cluster1)
      .getService(serviceName)
      .getServiceComponent(componentName1)
      .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(cluster1)
      .getService(serviceName)
      .getServiceComponent(componentName2)
      .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(cluster1)
      .getService(serviceName)
      .getServiceComponent(componentName2)
      .getServiceComponentHost(host2));
    Assert.assertNotNull(clusters.getCluster(cluster1)
      .getService(serviceName)
      .getServiceComponent(componentName3)
      .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(cluster1)
      .getService(serviceName)
      .getServiceComponent(componentName3)
      .getServiceComponentHost(host2));

    // Install
    ServiceRequest r = new ServiceRequest(cluster1, serviceName, repositoryVersion01.getId(), State.INSTALLED.toString());
    Set<ServiceRequest> requests = new HashSet<>();
    requests.add(r);

    ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true, false);
    Assert.assertEquals(State.INSTALLED,
      clusters.getCluster(cluster1).getService(serviceName)
        .getDesiredState());

    // manually change live state to installed as no running action manager
    for (ServiceComponent sc :
      clusters.getCluster(cluster1).getService(serviceName)
        .getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        sch.setState(State.INSTALLED);
      }
    }

    // Start
    r = new ServiceRequest(cluster1, serviceName, repositoryVersion01.getId(), State.STARTED.toString());
    requests.clear();
    requests.add(r);
    ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true, false);

    // manually change live state to started as no running action manager
    for (ServiceComponent sc :
      clusters.getCluster(cluster1).getService(serviceName)
        .getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        if (!sch.getServiceComponentName().equals("HDFS_CLIENT")) {
          sch.setState(State.STARTED);
        }
      }
    }

    Assert.assertEquals(State.STARTED,
      clusters.getCluster(cluster1).getService(serviceName)
        .getDesiredState());

    // Set Current state to stopping
    clusters.getCluster(cluster1).getService(serviceName).setDesiredState
      (State.STOPPING);
    for (ServiceComponent sc :
      clusters.getCluster(cluster1).getService(serviceName)
        .getServiceComponents().values()) {

      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        if (!sch.getServiceComponentName().equals("HDFS_CLIENT")) {
          Assert.assertEquals(State.STARTED, sch.getDesiredState());
          sch.setState(State.STOPPING);
        } else if (sch.getServiceComponentName().equals("DATANODE")) {
          ServiceComponentHostRequest r1 = new ServiceComponentHostRequest
            (cluster1, serviceName, sch.getServiceComponentName(),
              sch.getHostName(), State.INSTALLED.name());
          Set<ServiceComponentHostRequest> reqs1 = new
            HashSet<>();
          reqs1.add(r1);
          updateHostComponents(reqs1, Collections.emptyMap(), true);
          Assert.assertEquals(State.INSTALLED, sch.getDesiredState());
        }
      }
    }

    // Stop all services
    r = new ServiceRequest(cluster1, serviceName, repositoryVersion01.getId(), State.INSTALLED.toString());
    requests.clear();
    requests.add(r);
    ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true, false);

    for (ServiceComponent sc :
      clusters.getCluster(cluster1).getService(serviceName)
        .getServiceComponents().values()) {

      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        if (!sch.getServiceComponentName().equals("HDFS_CLIENT")) {
          Assert.assertEquals(State.INSTALLED, sch.getDesiredState());
        }
      }
    }
  }

  @Test
  public void testGetTasksByRequestId() throws Exception {
    ActionManager am = injector.getInstance(ActionManager.class);

    final long requestId1 = am.getNextRequestId();
    final long requestId2 = am.getNextRequestId();
    final long requestId3 = am.getNextRequestId();
    final String cluster1 = getUniqueName();
    final String hostName1 = getUniqueName();
    final String context = "Test invocation";

    StackId stackID = new StackId("HDP-0.1");
    clusters.addCluster(cluster1, stackID);
    Cluster c = clusters.getCluster(cluster1);
    Long clusterId = c.getClusterId();

    helper.getOrCreateRepositoryVersion(stackID, stackID.getStackVersion());
    clusters.addHost(hostName1);
    setOsFamily(clusters.getHost(hostName1), "redhat", "5.9");

    clusters.mapAndPublishHostsToCluster(new HashSet<String>(){
      {add(hostName1);}}, cluster1);


    List<Stage> stages = new ArrayList<>();
    stages.add(stageFactory.createNew(requestId1, "/a1", cluster1, clusterId, context,
        "", ""));
    stages.get(0).setStageId(1);
    stages.get(0).addHostRoleExecutionCommand(hostName1, Role.HBASE_MASTER,
            RoleCommand.START,
            new ServiceComponentHostStartEvent(Role.HBASE_MASTER.toString(),
                    hostName1, System.currentTimeMillis()),
            cluster1, "HBASE", false, false);

    stages.add(stageFactory.createNew(requestId1, "/a2", cluster1, clusterId, context,
      "", ""));
    stages.get(1).setStageId(2);
    stages.get(1).addHostRoleExecutionCommand(hostName1, Role.HBASE_CLIENT,
            RoleCommand.START,
            new ServiceComponentHostStartEvent(Role.HBASE_CLIENT.toString(),
                    hostName1, System.currentTimeMillis()), cluster1, "HBASE", false, false);

    stages.add(stageFactory.createNew(requestId1, "/a3", cluster1, clusterId, context,
      "", ""));
    stages.get(2).setStageId(3);
    stages.get(2).addHostRoleExecutionCommand(hostName1, Role.HBASE_CLIENT,
            RoleCommand.START,
            new ServiceComponentHostStartEvent(Role.HBASE_CLIENT.toString(),
                    hostName1, System.currentTimeMillis()), cluster1, "HBASE", false, false);

    Request request = new Request(stages, "", clusters);
    actionDB.persistActions(request);

    stages.clear();
    stages.add(stageFactory.createNew(requestId2, "/a4", cluster1, clusterId, context,
      "", ""));
    stages.get(0).setStageId(4);
    stages.get(0).addHostRoleExecutionCommand(hostName1, Role.HBASE_CLIENT,
            RoleCommand.START,
            new ServiceComponentHostStartEvent(Role.HBASE_CLIENT.toString(),
                    hostName1, System.currentTimeMillis()), cluster1, "HBASE", false, false);

    stages.add(stageFactory.createNew(requestId2, "/a5", cluster1, clusterId, context,
      "", ""));
    stages.get(1).setStageId(5);
    stages.get(1).addHostRoleExecutionCommand(hostName1, Role.HBASE_CLIENT,
            RoleCommand.START,
            new ServiceComponentHostStartEvent(Role.HBASE_CLIENT.toString(),
                    hostName1, System.currentTimeMillis()), cluster1, "HBASE", false, false);

    request = new Request(stages, "", clusters);
    actionDB.persistActions(request);

    // Add a stage to execute a task as server-side action on the Ambari server
    ServiceComponentHostServerActionEvent serviceComponentHostServerActionEvent =
        new ServiceComponentHostServerActionEvent(Role.AMBARI_SERVER_ACTION.toString(), null, System.currentTimeMillis());
    stages.clear();
    stages.add(stageFactory.createNew(requestId3, "/a6", cluster1, clusterId, context,
      "", ""));
    stages.get(0).setStageId(6);
    stages.get(0).addServerActionCommand("some.action.class.name", null, Role.AMBARI_SERVER_ACTION,
        RoleCommand.EXECUTE, cluster1, serviceComponentHostServerActionEvent, null, null, null, null, false, false);
    assertEquals("_internal_ambari", stages.get(0).getOrderedHostRoleCommands().get(0).getHostName());
    request = new Request(stages, "", clusters);
    actionDB.persistActions(request);

    org.apache.ambari.server.controller.spi.Request spiRequest = PropertyHelper.getReadRequest(
        TaskResourceProvider.TASK_CLUSTER_NAME_PROPERTY_ID,
        TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID,
        TaskResourceProvider.TASK_STAGE_ID_PROPERTY_ID);

    // request ID 1 has 3 tasks
    Predicate predicate = new PredicateBuilder().property(
        TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID).equals(requestId1).toPredicate();

    List<HostRoleCommandEntity> entities = hostRoleCommandDAO.findAll(spiRequest, predicate);
    Assert.assertEquals(3, entities.size());

    Long taskId = entities.get(0).getTaskId();

    // request just a task by ID
    predicate = new PredicateBuilder().property(
        TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID).equals(requestId1).and().property(
            TaskResourceProvider.TASK_ID_PROPERTY_ID).equals(taskId).toPredicate();

    entities = hostRoleCommandDAO.findAll(spiRequest, predicate);
    Assert.assertEquals(1, entities.size());

    // request ID 2 has 2 tasks
    predicate = new PredicateBuilder().property(
        TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID).equals(requestId2).toPredicate();

    entities = hostRoleCommandDAO.findAll(spiRequest, predicate);
    Assert.assertEquals(2, entities.size());

    // a single task from request 1 and all tasks from request 2 will total 3
    predicate = new PredicateBuilder().property(
        TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID).equals(requestId1).and().property(
            TaskResourceProvider.TASK_ID_PROPERTY_ID).equals(taskId).or().property(
                TaskResourceProvider.TASK_REQUEST_ID_PROPERTY_ID).equals(requestId2).toPredicate();

    entities = hostRoleCommandDAO.findAll(spiRequest, predicate);
    Assert.assertEquals(3, entities.size());
  }

  @Test
  public void testUpdateHostComponentsBadState() throws Exception {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    clusters.getCluster(cluster1)
      .setDesiredStackVersion(new StackId("HDP-0.1"));
    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";
    createServiceComponent(cluster1, serviceName, componentName1,
      State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2,
      State.INIT);
    createServiceComponent(cluster1, serviceName, componentName3,
      State.INIT);

    final String host1 = getUniqueName();
    final String host2 = getUniqueName();

    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);

    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");

    // null service should work
    createServiceComponentHost(cluster1, null, componentName1,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2,
      host2, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
      host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName3,
      host2, null);

    Assert.assertNotNull(clusters.getCluster(cluster1).getService(serviceName)
      .getServiceComponent(componentName1)
      .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(cluster1).getService(serviceName)
      .getServiceComponent(componentName2)
      .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(cluster1).getService(serviceName)
      .getServiceComponent(componentName2)
      .getServiceComponentHost(host2));
    Assert.assertNotNull(clusters.getCluster(cluster1).getService(serviceName)
      .getServiceComponent(componentName3)
      .getServiceComponentHost(host1));
    Assert.assertNotNull(clusters.getCluster(cluster1).getService(serviceName)
      .getServiceComponent(componentName3)
      .getServiceComponentHost(host2));



    // Install
    ServiceRequest r = new ServiceRequest(cluster1, serviceName, repositoryVersion01.getId(), State.INSTALLED.toString());
    Set<ServiceRequest> requests = new HashSet<>();
    requests.add(r);

    ServiceResourceProviderTest.updateServices(controller, requests, mapRequestProps, true, false);
    Assert.assertEquals(State.INSTALLED,
      clusters.getCluster(cluster1).getService(serviceName)
        .getDesiredState());

    // set host components on host1 to INSTALLED
    for (ServiceComponentHost sch : clusters.getCluster(cluster1).getServiceComponentHosts(host1)) {
      sch.setState(State.INSTALLED);
    }

    // set the host components on host2 to UNKNOWN state to simulate a lost host
    for (ServiceComponentHost sch : clusters.getCluster(cluster1).getServiceComponentHosts(host2)) {
      sch.setState(State.UNKNOWN);
    }

    // issue an installed state request without failure
    ServiceComponentHostRequest schr = new ServiceComponentHostRequest(cluster1, "HDFS", "DATANODE", host2, "INSTALLED");
    Map<String, String> requestProps = new HashMap<>();
    requestProps.put("datanode", "dn_value");
    requestProps.put("namenode", "nn_value");
    RequestStatusResponse rsr = updateHostComponents(Collections.singleton(schr), requestProps, false);

    List<Stage> stages = actionDB.getAllStages(rsr.getRequestId());
    Assert.assertEquals(1, stages.size());
    Stage stage = stages.iterator().next();
    List<ExecutionCommandWrapper> execWrappers = stage.getExecutionCommands(host2);
    Assert.assertEquals(1, execWrappers.size());
    ExecutionCommandWrapper execWrapper = execWrappers.iterator().next();
    Assert.assertTrue(execWrapper.getExecutionCommand().getCommandParams().containsKey("datanode"));
    Assert.assertFalse(execWrapper.getExecutionCommand().getCommandParams().containsKey("namendode"));



    // set the host components on host2 to UNKNOWN state to simulate a lost host
    for (ServiceComponentHost sch : clusters.getCluster(cluster1).getServiceComponentHosts(host2)) {
      Assert.assertEquals(State.UNKNOWN, sch.getState());
    }
  }

  @Test
  public void testServiceUpdateRecursiveBadHostComponent() throws Exception {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    clusters.getCluster(cluster1)
        .setDesiredStackVersion(new StackId("HDP-0.2"));

    String serviceName1 = "HDFS";
    createService(cluster1, serviceName1, null);

    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(cluster1, serviceName1, componentName1, State.INIT);
    createServiceComponent(cluster1, serviceName1, componentName2, State.INIT);
    createServiceComponent(cluster1, serviceName1, componentName3, State.INIT);
    String host1 = getUniqueName();
    addHostToCluster(host1, cluster1);

    Set<ServiceComponentHostRequest> set1 = new HashSet<>();
    ServiceComponentHostRequest r1 = new ServiceComponentHostRequest(cluster1, serviceName1,
        componentName1, host1, State.INIT.toString());
    ServiceComponentHostRequest r2 = new ServiceComponentHostRequest(cluster1, serviceName1,
        componentName2, host1, State.INIT.toString());
    ServiceComponentHostRequest r3 = new ServiceComponentHostRequest(cluster1, serviceName1,
        componentName3, host1, State.INIT.toString());

    set1.add(r1);
    set1.add(r2);
    set1.add(r3);
    controller.createHostComponents(set1);

    Cluster c1 = clusters.getCluster(cluster1);
    Service s1 = c1.getService(serviceName1);

    ServiceComponent sc1 = s1.getServiceComponent(componentName1);
    ServiceComponent sc2 = s1.getServiceComponent(componentName2);
    ServiceComponent sc3 = s1.getServiceComponent(componentName3);
    ServiceComponentHost sch1 = sc1.getServiceComponentHost(host1);
    ServiceComponentHost sch2 = sc2.getServiceComponentHost(host1);
    ServiceComponentHost sch3 = sc3.getServiceComponentHost(host1);

    s1.setDesiredState(State.INSTALLED);
    sc1.setDesiredState(State.STARTED);
    sc2.setDesiredState(State.INIT);
    sc3.setDesiredState(State.INSTALLED);
    sch1.setDesiredState(State.INSTALLED);
    sch2.setDesiredState(State.INSTALLED);
    sch3.setDesiredState(State.INSTALLED);
    sch1.setState(State.INSTALLED);
    sch2.setState(State.UNKNOWN);
    sch3.setState(State.INSTALLED);

    // an UNKOWN failure will throw an exception
    ServiceRequest req = new ServiceRequest(cluster1, serviceName1, repositoryVersion02.getId(),
        State.INSTALLED.toString());
    ServiceResourceProviderTest.updateServices(controller, Collections.singleton(req), Collections.emptyMap(), true, false);
  }

  @Test
  public void testUpdateStacks() throws Exception {

    StackInfo stackInfo = ambariMetaInfo.getStack(STACK_NAME, STACK_VERSION);

    for (RepositoryInfo repositoryInfo: stackInfo.getRepositories()) {
      assertFalse(INCORRECT_BASE_URL.equals(repositoryInfo.getBaseUrl()));
      repositoryInfo.setBaseUrl(INCORRECT_BASE_URL);
      assertTrue(INCORRECT_BASE_URL.equals(repositoryInfo.getBaseUrl()));
    }

    stackManagerMock.invalidateCurrentPaths();
    controller.updateStacks();

    stackInfo = ambariMetaInfo.getStack(STACK_NAME, STACK_VERSION);

    for (RepositoryInfo repositoryInfo: stackInfo.getRepositories()) {
      assertFalse(INCORRECT_BASE_URL.equals(repositoryInfo.getBaseUrl()));
    }
  }

  @Test
  public void testDeleteHostComponentInVariousStates() throws Exception {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    clusters.getCluster(cluster1).setDesiredStackVersion(new StackId("HDP-1.3.1"));
    String hdfs = "HDFS";
    String mapred = "MAPREDUCE";
    createService(cluster1, hdfs, null);
    createService(cluster1, mapred, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";
    String componentName4 = "JOBTRACKER";
    String componentName5 = "TASKTRACKER";
    String componentName6 = "MAPREDUCE_CLIENT";

    createServiceComponent(cluster1, hdfs, componentName1, State.INIT);
    createServiceComponent(cluster1, hdfs, componentName2, State.INIT);
    createServiceComponent(cluster1, hdfs, componentName3, State.INIT);
    createServiceComponent(cluster1, mapred, componentName4, State.INIT);
    createServiceComponent(cluster1, mapred, componentName5, State.INIT);
    createServiceComponent(cluster1, mapred, componentName6, State.INIT);

    String host1 = getUniqueName();

    addHostToCluster(host1, cluster1);

    createServiceComponentHost(cluster1, hdfs, componentName1, host1, null);
    createServiceComponentHost(cluster1, hdfs, componentName2, host1, null);
    createServiceComponentHost(cluster1, hdfs, componentName3, host1, null);
    createServiceComponentHost(cluster1, mapred, componentName4, host1, null);
    createServiceComponentHost(cluster1, mapred, componentName5, host1, null);
    createServiceComponentHost(cluster1, mapred, componentName6, host1, null);

    // Install
    installService(cluster1, hdfs, false, false);
    installService(cluster1, mapred, false, false);

    Cluster cluster = clusters.getCluster(cluster1);
    Service s1 = cluster.getService(hdfs);
    Service s2 = cluster.getService(mapred);
    ServiceComponent sc1 = s1.getServiceComponent(componentName1);
    sc1.getServiceComponentHosts().values().iterator().next().setState(State.STARTED);

    Set<ServiceComponentHostRequest> schRequests = new HashSet<>();
    // delete HC
    schRequests.clear();
    schRequests.add(new ServiceComponentHostRequest(cluster1, hdfs, componentName1, host1, null));
    try {
      controller.deleteHostComponents(schRequests);
      Assert.fail("Expect failure while deleting.");
    } catch (Exception ex) {
      Assert.assertTrue(ex.getMessage().contains(
          "Current host component state prohibiting component removal"));
    }

    sc1.getServiceComponentHosts().values().iterator().next().setDesiredState(State.STARTED);
    sc1.getServiceComponentHosts().values().iterator().next().setState(State.UNKNOWN);
    ServiceComponent sc2 = s1.getServiceComponent(componentName2);
    sc2.getServiceComponentHosts().values().iterator().next().setState(State.INIT);
    ServiceComponent sc3 = s1.getServiceComponent(componentName3);
    sc3.getServiceComponentHosts().values().iterator().next().setState(State.INSTALL_FAILED);
    ServiceComponent sc4 = s2.getServiceComponent(componentName4);
    sc4.getServiceComponentHosts().values().iterator().next().setDesiredState(State.INSTALLED);
    sc4.getServiceComponentHosts().values().iterator().next().setState(State.DISABLED);
    ServiceComponent sc5 = s2.getServiceComponent(componentName5);
    sc5.getServiceComponentHosts().values().iterator().next().setState(State.INSTALLED);
    ServiceComponent sc6 = s2.getServiceComponent(componentName6);
    sc6.getServiceComponentHosts().values().iterator().next().setState(State.INIT);

    schRequests.clear();
    schRequests.add(new ServiceComponentHostRequest(cluster1, hdfs, componentName1, host1, null));
    schRequests.add(new ServiceComponentHostRequest(cluster1, hdfs, componentName2, host1, null));
    schRequests.add(new ServiceComponentHostRequest(cluster1, hdfs, componentName3, host1, null));
    schRequests.add(new ServiceComponentHostRequest(cluster1, mapred, componentName4, host1, null));
    schRequests.add(new ServiceComponentHostRequest(cluster1, mapred, componentName5, host1, null));
    schRequests.add(new ServiceComponentHostRequest(cluster1, mapred, componentName6, host1, null));
    DeleteStatusMetaData deleteStatusMetaData = controller.deleteHostComponents(schRequests);
    Assert.assertEquals(0, deleteStatusMetaData.getExceptionForKeys().size());
  }

  @Test
  public void testDeleteHostWithComponent() throws Exception {
    String cluster1 = getUniqueName();

    createCluster(cluster1);

    Cluster cluster = clusters.getCluster(cluster1);
    cluster.setDesiredStackVersion(new StackId("HDP-0.1"));

    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(cluster1, serviceName, componentName1, State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2, State.INIT);
    createServiceComponent(cluster1, serviceName, componentName3, State.INIT);

    String host1 = getUniqueName();  // Host will belong to the cluster and contain components

    addHostToCluster(host1, cluster1);

    // Add components to host1
    createServiceComponentHost(cluster1, serviceName, componentName1, host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2, host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName3, host1, null);

    // Install
    installService(cluster1, serviceName, false, false);

    // Treat host components on host1 as up and healthy
    Map<String, ServiceComponentHost> hostComponents = cluster.getService(serviceName).getServiceComponent(componentName1).getServiceComponentHosts();
    for (Map.Entry<String, ServiceComponentHost> entry : hostComponents.entrySet()) {
      ServiceComponentHost cHost = entry.getValue();
      cHost.handleEvent(new ServiceComponentHostInstallEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis(), cluster.getDesiredStackVersion().getStackId()));
      cHost.handleEvent(new ServiceComponentHostOpSucceededEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis()));
    }
    hostComponents = cluster.getService(serviceName).getServiceComponent(componentName2).getServiceComponentHosts();
    for (Map.Entry<String, ServiceComponentHost> entry : hostComponents.entrySet()) {
      ServiceComponentHost cHost = entry.getValue();
      cHost.handleEvent(new ServiceComponentHostInstallEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis(), cluster.getDesiredStackVersion().getStackId()));
      cHost.handleEvent(new ServiceComponentHostOpSucceededEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis()));
    }

    // Case 1: Attempt delete when some components are STARTED
    Set<HostRequest> requests = new HashSet<>();
    requests.clear();
    requests.add(new HostRequest(host1, cluster1));

    Service s = cluster.getService(serviceName);
    s.getServiceComponent("DATANODE").getServiceComponentHost(host1).setState(State.STARTED);
    try {
      HostResourceProviderTest.deleteHosts(controller, requests, false);
      fail("Expect failure deleting hosts when components exist and have not been stopped.");
    } catch (Exception e) {
      LOG.info("Exception is - " + e.getMessage());
      Assert.assertTrue(e.getMessage().contains("these components are not in the removable state:"));
    }

    // Case 2: Attempt delete dryRun = true
    DeleteStatusMetaData data = null;

    LOG.info("Test dry run of delete with all host components");
    s.getServiceComponent("DATANODE").getServiceComponentHost(host1).setState(State.INSTALLED);
    try {
      data = HostResourceProviderTest.deleteHosts(controller, requests, true);
      Assert.assertTrue(data.getDeletedKeys().size() == 1);
    } catch (Exception e) {
      LOG.info("Exception is - " + e.getMessage());
      fail("Do not expect failure deleting hosts when components exist and are stopped.");
    }

    // Case 3: Attempt delete dryRun = false
    LOG.info("Test successful delete with all host components");
    s.getServiceComponent("DATANODE").getServiceComponentHost(host1).setState(State.INSTALLED);
    try {
      data = HostResourceProviderTest.deleteHosts(controller, requests, false);
      Assert.assertNotNull(data);
      Assert.assertTrue(4 == data.getDeletedKeys().size());
      Assert.assertTrue(0 == data.getExceptionForKeys().size());
    } catch (Exception e) {
      LOG.info("Exception is - " + e.getMessage());
      fail("Do not expect failure deleting hosts when components exist and are stopped.");
    }
    // Verify host does not exist
    try {
      clusters.getHost(host1);
      Assert.fail("Expected a HostNotFoundException.");
    } catch (HostNotFoundException e) {
      // expected
    }
  }

  @Test
  public void testDeleteHost() throws Exception {
    String cluster1 = getUniqueName();

    createCluster(cluster1);

    Cluster cluster = clusters.getCluster(cluster1);
    cluster.setDesiredStackVersion(new StackId("HDP-0.1"));

    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(cluster1, serviceName, componentName1, State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2, State.INIT);
    createServiceComponent(cluster1, serviceName, componentName3, State.INIT);

    String host1 = getUniqueName();  // Host will belong to the cluster and contain components
    String host2 = getUniqueName();  // Host will belong to the cluster and not contain any components

    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);
    String host3 = getUniqueName();  // Host is not registered

    // Add components to host1
    createServiceComponentHost(cluster1, serviceName, componentName1, host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2, host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName3, host1, null);

    // Install
    installService(cluster1, serviceName, false, false);

    // Treat host components on host1 as up and healthy
    Map<String, ServiceComponentHost> hostComponents = cluster.getService(serviceName).getServiceComponent(componentName1).getServiceComponentHosts();
    for (Map.Entry<String, ServiceComponentHost> entry : hostComponents.entrySet()) {
      ServiceComponentHost cHost = entry.getValue();
      cHost.handleEvent(new ServiceComponentHostInstallEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis(), cluster.getDesiredStackVersion().getStackId()));
      cHost.handleEvent(new ServiceComponentHostOpSucceededEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis()));
    }
    hostComponents = cluster.getService(serviceName).getServiceComponent(componentName2).getServiceComponentHosts();
    for (Map.Entry<String, ServiceComponentHost> entry : hostComponents.entrySet()) {
      ServiceComponentHost cHost = entry.getValue();
      cHost.handleEvent(new ServiceComponentHostInstallEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis(), cluster.getDesiredStackVersion().getStackId()));
      cHost.handleEvent(new ServiceComponentHostOpSucceededEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis()));
    }

    Set<HostRequest> requests = new HashSet<>();
    requests.clear();
    requests.add(new HostRequest(host1, cluster1));

    // Case 1: Delete host that is still part of cluster, but do not specify the cluster_name in the request
    Set<ServiceComponentHostRequest> schRequests = new HashSet<>();
    // Disable HC for non-clients
    schRequests.add(new ServiceComponentHostRequest(cluster1, serviceName, componentName1, host1, "DISABLED"));
    schRequests.add(new ServiceComponentHostRequest(cluster1, serviceName, componentName2, host1, "DISABLED"));
    updateHostComponents(schRequests, new HashMap<>(), false);

    // Delete HC
    schRequests.clear();
    schRequests.add(new ServiceComponentHostRequest(cluster1, serviceName, componentName1, host1, null));
    schRequests.add(new ServiceComponentHostRequest(cluster1, serviceName, componentName2, host1, null));
    schRequests.add(new ServiceComponentHostRequest(cluster1, serviceName, componentName3, host1, null));
    controller.deleteHostComponents(schRequests);

    Assert.assertEquals(0, cluster.getServiceComponentHosts(host1).size());

    Assert.assertNull(topologyHostInfoDAO.findByHostname(host1));

    Long firstHostId = clusters.getHost(host1).getHostId();

    // Deletion without specifying cluster should be successful
    requests.clear();
    requests.add(new HostRequest(host1, null));
    try {
      HostResourceProviderTest.deleteHosts(controller, requests);
    } catch (Exception e) {
      fail("Did not expect an error deleting the host from the cluster. Error: " + e.getMessage());
    }
    // Verify host is no longer part of the cluster
    Assert.assertFalse(clusters.getHostsForCluster(cluster1).containsKey(host1));
    Assert.assertFalse(clusters.getClustersForHost(host1).contains(cluster));
    Assert.assertNull(topologyHostInfoDAO.findByHostname(host1));

    // verify there are no host role commands for the host
    List<HostRoleCommandEntity> tasks = hostRoleCommandDAO.findByHostId(firstHostId);
    assertEquals(0, tasks.size());

    // Case 2: Delete host that is still part of the cluster, and specify the cluster_name in the request
    requests.clear();
    requests.add(new HostRequest(host2, cluster1));
    try {
      HostResourceProviderTest.deleteHosts(controller, requests);
    } catch (Exception e) {
      fail("Did not expect an error deleting the host from the cluster. Error: " + e.getMessage());
    }
    // Verify host is no longer part of the cluster
    Assert.assertFalse(clusters.getHostsForCluster(cluster1).containsKey(host2));
    Assert.assertFalse(clusters.getClustersForHost(host2).contains(cluster));
    Assert.assertNull(topologyHostInfoDAO.findByHostname(host2));

    // Case 3: Attempt to delete a host that has already been deleted
    requests.clear();
    requests.add(new HostRequest(host1, null));
    try {
      HostResourceProviderTest.deleteHosts(controller, requests);
      Assert.fail("Expected a HostNotFoundException trying to remove a host that was already deleted.");
    } catch (HostNotFoundException e) {
      // expected
    }

    // Verify host does not exist
    try {
      clusters.getHost(host1);
      Assert.fail("Expected a HostNotFoundException.");
    } catch (HostNotFoundException e) {
      // expected
    }

    // Case 4: Attempt to delete a host that was never added to the cluster
    requests.clear();
    requests.add(new HostRequest(host3, null));
    try {
      HostResourceProviderTest.deleteHosts(controller, requests);
      Assert.fail("Expected a HostNotFoundException trying to remove a host that was never added.");
    } catch (HostNotFoundException e) {
      // expected
    }
  }

  @Test
  public void testGetRootServices() throws Exception {

    RootServiceRequest request = new RootServiceRequest(null);
    Set<RootServiceResponse> responses = controller.getRootServices(Collections.singleton(request));
    Assert.assertEquals(RootService.values().length, responses.size());

    RootServiceRequest requestWithParams = new RootServiceRequest(RootService.AMBARI.toString());
    Set<RootServiceResponse> responsesWithParams = controller.getRootServices(Collections.singleton(requestWithParams));
    Assert.assertEquals(1, responsesWithParams.size());
    for (RootServiceResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getServiceName(), RootService.AMBARI.toString());
    }

    RootServiceRequest invalidRequest = new RootServiceRequest(NON_EXT_VALUE);
    try {
      controller.getRootServices(Collections.singleton(invalidRequest));
    } catch (ObjectNotFoundException e) {
      // do nothing
    }
  }

  @Test
  public void testGetRootServiceComponents() throws Exception {

    RootServiceComponentRequest request = new RootServiceComponentRequest(RootService.AMBARI.toString(), null);
    Set<RootServiceComponentResponse> responses = controller.getRootServiceComponents(Collections.singleton(request));
    Assert.assertEquals(RootService.AMBARI.getComponents().length, responses.size());

    RootServiceComponentRequest requestWithParams = new RootServiceComponentRequest(
        RootService.AMBARI.toString(),
        RootService.AMBARI.getComponents()[0].toString());

    Set<RootServiceComponentResponse> responsesWithParams = controller.getRootServiceComponents(Collections.singleton(requestWithParams));
    Assert.assertEquals(1, responsesWithParams.size());
    for (RootServiceComponentResponse responseWithParams: responsesWithParams) {
      Assert.assertEquals(responseWithParams.getComponentName(), RootService.AMBARI.getComponents()[0].toString());
    }

    RootServiceComponentRequest invalidRequest = new RootServiceComponentRequest(NON_EXT_VALUE, NON_EXT_VALUE);
    try {
      controller.getRootServiceComponents(Collections.singleton(invalidRequest));
    } catch (ObjectNotFoundException e) {
      // do nothing
    }
  }

  @Test
  public void testDeleteComponentsOnHost() throws Exception {
    String cluster1 = getUniqueName();

    createCluster(cluster1);

    Cluster cluster = clusters.getCluster(cluster1);
    cluster.setDesiredStackVersion(new StackId("HDP-0.1"));

    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(cluster1, serviceName, componentName1, State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2, State.INIT);
    createServiceComponent(cluster1, serviceName, componentName3, State.INIT);

    String host1 = getUniqueName();

    addHostToCluster(host1, cluster1);

    createServiceComponentHost(cluster1, null, componentName1, host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2, host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName3, host1, null);

    // Install
    installService(cluster1, serviceName, false, false);

    // make them believe they are up
    Map<String, ServiceComponentHost> hostComponents = cluster.getService(serviceName).getServiceComponent(componentName1).getServiceComponentHosts();
    for (Map.Entry<String, ServiceComponentHost> entry : hostComponents.entrySet()) {
      ServiceComponentHost cHost = entry.getValue();
      cHost.handleEvent(new ServiceComponentHostInstallEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis(), cluster.getDesiredStackVersion().getStackId()));
      cHost.handleEvent(new ServiceComponentHostOpSucceededEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis()));
    }
    hostComponents = cluster.getService(serviceName).getServiceComponent(componentName2).getServiceComponentHosts();
    for (Map.Entry<String, ServiceComponentHost> entry : hostComponents.entrySet()) {
      ServiceComponentHost cHost = entry.getValue();
      cHost.handleEvent(new ServiceComponentHostInstallEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis(), cluster.getDesiredStackVersion().getStackId()));
      cHost.handleEvent(new ServiceComponentHostOpSucceededEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis()));
    }


    ServiceComponentHost sch = cluster.getService(serviceName).getServiceComponent(componentName2).getServiceComponentHost(host1);
    Assert.assertNotNull(sch);

    sch.handleEvent(new ServiceComponentHostStartEvent(sch.getServiceComponentName(), sch.getHostName(), System.currentTimeMillis()));
    sch.handleEvent(new ServiceComponentHostStartedEvent (sch.getServiceComponentName(), sch.getHostName(), System.currentTimeMillis()));

    Set<ServiceComponentHostRequest> schRequests = new HashSet<>();
    schRequests.add(new ServiceComponentHostRequest(cluster1, null, null, host1, null));

    DeleteStatusMetaData deleteStatusMetaData = controller.deleteHostComponents(schRequests);
    Assert.assertEquals(1, deleteStatusMetaData.getExceptionForKeys().size());
    Assert.assertEquals(1, cluster.getServiceComponentHosts(host1).size());

    sch.handleEvent(new ServiceComponentHostStopEvent(sch.getServiceComponentName(), sch.getHostName(), System.currentTimeMillis()));
    sch.handleEvent(new ServiceComponentHostStoppedEvent (sch.getServiceComponentName(), sch.getHostName(), System.currentTimeMillis()));

    controller.deleteHostComponents(schRequests);

    Assert.assertEquals(0, cluster.getServiceComponentHosts(host1).size());
  }

  @Test
  public void testExecutionCommandConfiguration() throws Exception {
    Map<String, Map<String, String>> config = new HashMap<>();
    config.put("type1", new HashMap<>());
    config.put("type3", new HashMap<>());
    config.get("type3").put("name1", "neverchange");
    configHelper.applyCustomConfig(config, "type1", "name1", "value11", false);
    Assert.assertEquals("value11", config.get("type1").get("name1"));

    config.put("type1", new HashMap<>());
    configHelper.applyCustomConfig(config, "type1", "name1", "value12", false);
    Assert.assertEquals("value12", config.get("type1").get("name1"));

    configHelper.applyCustomConfig(config, "type2", "name2", "value21", false);
    Assert.assertEquals("value21", config.get("type2").get("name2"));

    configHelper.applyCustomConfig(config, "type2", "name2", "", true);
    Assert.assertEquals("", config.get("type2").get("DELETED_name2"));
    Assert.assertEquals("neverchange", config.get("type3").get("name1"));

    Map<String, String> persistedClusterConfig = new HashMap<>();
    persistedClusterConfig.put("name1", "value11");
    persistedClusterConfig.put("name3", "value31");
    persistedClusterConfig.put("name4", "value41");
    Map<String, String> override = new HashMap<>();
    override.put("name1", "value12");
    override.put("name2", "value21");
    override.put("DELETED_name3", "value31");
    Map<String, String> mergedConfig = configHelper.getMergedConfig
      (persistedClusterConfig, override);
    Assert.assertEquals(3, mergedConfig.size());
    Assert.assertFalse(mergedConfig.containsKey("name3"));
    Assert.assertEquals("value12", mergedConfig.get("name1"));
    Assert.assertEquals("value21", mergedConfig.get("name2"));
    Assert.assertEquals("value41", mergedConfig.get("name4"));
  }

  @Test
  public void testApplyConfigurationWithTheSameTag() throws AuthorizationException {

    final String cluster1 = getUniqueName();

    String tag = "version1";
    String type = "core-site";
    Exception exception = null;
    try {
      AmbariManagementController amc = injector.getInstance(AmbariManagementController.class);
      Clusters clusters = injector.getInstance(Clusters.class);
      Gson gson = new Gson();

      clusters.addHost("host1");
      clusters.addHost("host2");
      clusters.addHost("host3");
      Host host = clusters.getHost("host1");
      setOsFamily(host, "redhat", "6.3");
      host = clusters.getHost("host2");
      setOsFamily(host, "redhat", "6.3");
      host = clusters.getHost("host3");
      setOsFamily(host, "redhat", "6.3");

      ClusterRequest clusterRequest = new ClusterRequest(null, cluster1, "HDP-1.2.0", null);
      amc.createCluster(clusterRequest);

      Set<ServiceRequest> serviceRequests = new HashSet<>();
      serviceRequests.add(new ServiceRequest(cluster1, "HDFS", repositoryVersion120.getId(), null));

      ServiceResourceProviderTest.createServices(amc, repositoryVersionDAO, serviceRequests);

      Type confType = new TypeToken<Map<String, String>>() {
      }.getType();

      ConfigurationRequest configurationRequest = new ConfigurationRequest(cluster1, type, tag,
          gson.fromJson("{ \"fs.default.name\" : \"localhost:8020\"}", confType), null);
      amc.createConfiguration(configurationRequest);

      amc.createConfiguration(configurationRequest);
    } catch (Exception e) {
      exception = e;
    }

    assertNotNull(exception);
    String exceptionMessage = MessageFormat.format("Configuration with tag ''{0}'' exists for ''{1}''",
        tag, type);
    assertEquals(exceptionMessage, exception.getMessage());
  }

  @Test
  public void testDeleteClusterCreateHost() throws Exception {

    String STACK_ID = "HDP-2.0.1";

    String CLUSTER_NAME = getUniqueName();
    String HOST1 = getUniqueName();
    String HOST2 = getUniqueName();

    Clusters clusters = injector.getInstance(Clusters.class);

    clusters.addHost(HOST1);
    Host host = clusters.getHost(HOST1);
    setOsFamily(host, "redhat", "6.3");
    clusters.getHost(HOST1).setState(HostState.HEALTHY);
    clusters.updateHostMappings(host);

    clusters.addHost(HOST2);

    host = clusters.getHost(HOST2);
    clusters.updateHostMappings(host);
    setOsFamily(host, "redhat", "6.3");

    AmbariManagementController amc = injector.getInstance(AmbariManagementController.class);

    ClusterRequest cr = new ClusterRequest(null, CLUSTER_NAME, STACK_ID, null);
    amc.createCluster(cr);

    long clusterId = clusters.getCluster(CLUSTER_NAME).getClusterId();

    ConfigurationRequest configRequest = new ConfigurationRequest(CLUSTER_NAME, "global", "version1",
        new HashMap<String, String>() {{ put("a", "b"); }}, null);
    ClusterRequest ur = new ClusterRequest(clusterId, CLUSTER_NAME, STACK_ID, null);
    ur.setDesiredConfig(Collections.singletonList(configRequest));
    amc.updateClusters(Collections.singleton(ur), new HashMap<>());

    // add some hosts
    Set<HostRequest> hrs = new HashSet<>();
    hrs.add(new HostRequest(HOST1, CLUSTER_NAME));
    HostResourceProviderTest.createHosts(amc, hrs);

    Set<ServiceRequest> serviceRequests = new HashSet<>();
    serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "HDFS", repositoryVersion201.getId(), null));
    serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "MAPREDUCE2", repositoryVersion201.getId(), null));
    serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "YARN", repositoryVersion201.getId(), null));

    ServiceResourceProviderTest.createServices(amc, repositoryVersionDAO, serviceRequests);

    Set<ServiceComponentRequest> serviceComponentRequests = new HashSet<>();
    serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "HDFS", "NAMENODE", null));
    serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "HDFS", "SECONDARY_NAMENODE", null));
    serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "HDFS", "DATANODE", null));
    serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "MAPREDUCE2", "HISTORYSERVER", null));
    serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "YARN", "RESOURCEMANAGER", null));
    serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "YARN", "NODEMANAGER", null));
    serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "HDFS", "HDFS_CLIENT", null));

    ComponentResourceProviderTest.createComponents(amc, serviceComponentRequests);

    Set<ServiceComponentHostRequest> componentHostRequests = new HashSet<>();
    componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, "HDFS", "DATANODE", HOST1, null));
    componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, "HDFS", "NAMENODE", HOST1, null));
    componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, "HDFS", "SECONDARY_NAMENODE", HOST1, null));
    componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, "MAPREDUCE2", "HISTORYSERVER", HOST1, null));
    componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, "YARN", "RESOURCEMANAGER", HOST1, null));
    componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, "YARN", "NODEMANAGER", HOST1, null));
    componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, "HDFS", "HDFS_CLIENT", HOST1, null));

    amc.createHostComponents(componentHostRequests);

    RequestResourceFilter resourceFilter = new RequestResourceFilter("HDFS", null, null);
    ExecuteActionRequest ar = new ExecuteActionRequest(CLUSTER_NAME, Role.HDFS_SERVICE_CHECK.name(), null, false);
    ar.getResourceFilters().add(resourceFilter);
    amc.createAction(ar, null);


    // change mind, delete the cluster
    amc.deleteCluster(cr);

    assertNotNull(clusters.getHost(HOST1));
    assertNotNull(clusters.getHost(HOST2));

    HostDAO dao = injector.getInstance(HostDAO.class);

    assertNotNull(dao.findByName(HOST1));
    assertNotNull(dao.findByName(HOST2));

  }

  @Test
  @Ignore
  public void testDisableAndDeleteStates() throws Exception {
    Map<String,String> mapRequestProps = new HashMap<>();

    String cluster1 = getUniqueName();

    AmbariManagementController amc = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = injector.getInstance(Clusters.class);
    Gson gson = new Gson();

    String host1 = getUniqueName();
    String host2 = getUniqueName();
    String host3 = getUniqueName();

    clusters.addHost(host1);
    clusters.addHost(host2);
    clusters.addHost(host3);
    Host host = clusters.getHost("host1");
    setOsFamily(host, "redhat", "5.9");
    clusters.updateHostMappings(host);
    host = clusters.getHost("host2");
    setOsFamily(host, "redhat", "5.9");
    clusters.updateHostMappings(host);
    host = clusters.getHost("host3");
    setOsFamily(host, "redhat", "5.9");
    clusters.updateHostMappings(host);

    ClusterRequest clusterRequest = new ClusterRequest(null, cluster1, "HDP-1.2.0", null);
    amc.createCluster(clusterRequest);

    Set<ServiceRequest> serviceRequests = new HashSet<>();
    serviceRequests.add(new ServiceRequest(cluster1, "HDFS", repositoryVersion120.getId(), null));
    serviceRequests.add(new ServiceRequest(cluster1, "HIVE", repositoryVersion120.getId(), null));

    ServiceResourceProviderTest.createServices(amc, repositoryVersionDAO, serviceRequests);

    Type confType = new TypeToken<Map<String, String>>() {}.getType();

    ConfigurationRequest configurationRequest = new ConfigurationRequest(cluster1, "core-site", "version1",
        gson.fromJson("{ \"fs.default.name\" : \"localhost:8020\"}", confType), null);
    amc.createConfiguration(configurationRequest);

    configurationRequest = new ConfigurationRequest(cluster1, "hdfs-site", "version1",
        gson.fromJson("{ \"dfs.datanode.data.dir.perm\" : \"750\"}", confType), null);
    amc.createConfiguration(configurationRequest);

    configurationRequest = new ConfigurationRequest(cluster1, "global", "version1",
        gson.fromJson("{ \"hive.server2.enable.doAs\" : \"true\"}", confType), null);
    amc.createConfiguration(configurationRequest);

    Assert.assertTrue(clusters.getCluster(cluster1).getDesiredConfigs().containsKey("hive-site"));

    serviceRequests.clear();
    serviceRequests.add(new ServiceRequest(cluster1, "HDFS", repositoryVersion120.getId(), null));

    ServiceResourceProviderTest.updateServices(amc, serviceRequests, mapRequestProps, true, false);

    Set<ServiceComponentRequest> serviceComponentRequests = new HashSet<>();
    serviceComponentRequests.add(new ServiceComponentRequest(cluster1, "HDFS", "NAMENODE", null));
    serviceComponentRequests.add(new ServiceComponentRequest(cluster1, "HDFS", "SECONDARY_NAMENODE", null));
    serviceComponentRequests.add(new ServiceComponentRequest(cluster1, "HDFS", "DATANODE", null));
    serviceComponentRequests.add(new ServiceComponentRequest(cluster1, "HDFS", "HDFS_CLIENT", null));

    ComponentResourceProviderTest.createComponents(amc, serviceComponentRequests);

    Set<HostRequest> hostRequests = new HashSet<>();
    hostRequests.add(new HostRequest(host1, cluster1));
    hostRequests.add(new HostRequest(host2, cluster1));
    hostRequests.add(new HostRequest(host3, cluster1));

    HostResourceProviderTest.createHosts(amc, hostRequests);

    Set<ServiceComponentHostRequest> componentHostRequests = new HashSet<>();
    componentHostRequests.add(new ServiceComponentHostRequest(cluster1, null, "DATANODE", host1, null));
    componentHostRequests.add(new ServiceComponentHostRequest(cluster1, null, "NAMENODE", host1, null));
    componentHostRequests.add(new ServiceComponentHostRequest(cluster1, null, "SECONDARY_NAMENODE", host1, null));
    componentHostRequests.add(new ServiceComponentHostRequest(cluster1, null, "DATANODE", host2, null));
    componentHostRequests.add(new ServiceComponentHostRequest(cluster1, null, "DATANODE", host3, null));


    amc.createHostComponents(componentHostRequests);

    serviceRequests.clear();
    serviceRequests.add(new ServiceRequest(cluster1, "HDFS", repositoryVersion120.getId(), "INSTALLED"));
    ServiceResourceProviderTest.updateServices(amc, serviceRequests, mapRequestProps, true, false);

    Cluster cluster = clusters.getCluster(cluster1);
    Map<String, ServiceComponentHost> namenodes = cluster.getService("HDFS").getServiceComponent("NAMENODE").getServiceComponentHosts();
    org.junit.Assert.assertEquals(1, namenodes.size());

    ServiceComponentHost componentHost = namenodes.get(host1);

    Map<String, ServiceComponentHost> hostComponents = cluster.getService("HDFS").getServiceComponent("DATANODE").getServiceComponentHosts();
    for (Map.Entry<String, ServiceComponentHost> entry : hostComponents.entrySet()) {
      ServiceComponentHost cHost = entry.getValue();
      cHost.handleEvent(new ServiceComponentHostInstallEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis(), "HDP-1.2.0"));
      cHost.handleEvent(new ServiceComponentHostOpSucceededEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis()));
    }
    hostComponents = cluster.getService("HDFS").getServiceComponent("NAMENODE").getServiceComponentHosts();
    for (Map.Entry<String, ServiceComponentHost> entry : hostComponents.entrySet()) {
      ServiceComponentHost cHost = entry.getValue();
      cHost.handleEvent(new ServiceComponentHostInstallEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis(), "HDP-1.2.0"));
      cHost.handleEvent(new ServiceComponentHostOpSucceededEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis()));
    }
    hostComponents = cluster.getService("HDFS").getServiceComponent("SECONDARY_NAMENODE").getServiceComponentHosts();
    for (Map.Entry<String, ServiceComponentHost> entry : hostComponents.entrySet()) {
      ServiceComponentHost cHost = entry.getValue();
      cHost.handleEvent(new ServiceComponentHostInstallEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis(), "HDP-1.2.0"));
      cHost.handleEvent(new ServiceComponentHostOpSucceededEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis()));
    }

    componentHostRequests.clear();
    componentHostRequests.add(new ServiceComponentHostRequest(cluster1, null, "NAMENODE", host1, "DISABLED"));

    updateHostComponents(amc, componentHostRequests, mapRequestProps, true);

    Assert.assertEquals(State.DISABLED, componentHost.getState());

    componentHostRequests.clear();
    componentHostRequests.add(new ServiceComponentHostRequest(cluster1, null, "NAMENODE", host1, "INSTALLED"));

    updateHostComponents(amc, componentHostRequests, mapRequestProps, true);

    Assert.assertEquals(State.INSTALLED, componentHost.getState());

    componentHostRequests.clear();
    componentHostRequests.add(new ServiceComponentHostRequest(cluster1, null, "NAMENODE", host1, "DISABLED"));

    updateHostComponents(amc, componentHostRequests, mapRequestProps, true);

    Assert.assertEquals(State.DISABLED, componentHost.getState());

    componentHostRequests.clear();
    componentHostRequests.add(new ServiceComponentHostRequest(cluster1, null, "NAMENODE", host2, null));

    amc.createHostComponents(componentHostRequests);

    componentHostRequests.clear();
    componentHostRequests.add(new ServiceComponentHostRequest(cluster1, null, "NAMENODE", host2, "INSTALLED"));

    updateHostComponents(amc, componentHostRequests, mapRequestProps, true);

    namenodes = cluster.getService("HDFS").getServiceComponent("NAMENODE").getServiceComponentHosts();
    Assert.assertEquals(2, namenodes.size());

    componentHost = namenodes.get(host2);
    componentHost.handleEvent(new ServiceComponentHostInstallEvent(componentHost.getServiceComponentName(), componentHost.getHostName(), System.currentTimeMillis(), "HDP-1.2.0"));
    componentHost.handleEvent(new ServiceComponentHostOpSucceededEvent(componentHost.getServiceComponentName(), componentHost.getHostName(), System.currentTimeMillis()));

    serviceRequests.clear();
    serviceRequests.add(new ServiceRequest(cluster1, "HDFS", repositoryVersion120.getId(), "STARTED"));

    RequestStatusResponse response = ServiceResourceProviderTest.updateServices(amc, serviceRequests,
        mapRequestProps, true, false);
    for (ShortTaskStatus shortTaskStatus : response.getTasks()) {
      assertFalse(host1.equals(shortTaskStatus.getHostName()) && "NAMENODE".equals(shortTaskStatus.getRole()));
    }

    componentHostRequests.clear();
    componentHostRequests.add(new ServiceComponentHostRequest(cluster1, null, "NAMENODE", host1, null));

    amc.deleteHostComponents(componentHostRequests);

    namenodes = cluster.getService("HDFS").getServiceComponent("NAMENODE").getServiceComponentHosts();
    org.junit.Assert.assertEquals(1, namenodes.size());

    // testing the behavior for runSmokeTest flag
    // piggybacking on this test to avoid setting up the mock cluster
    testRunSmokeTestFlag(mapRequestProps, amc, serviceRequests);

    // should be able to add the host component back
    componentHostRequests.clear();
    componentHostRequests.add(new ServiceComponentHostRequest(cluster1, null, "NAMENODE", host1, null));
    amc.createHostComponents(componentHostRequests);
    namenodes = cluster.getService("HDFS").getServiceComponent("NAMENODE").getServiceComponentHosts();
    assertEquals(2, namenodes.size());

    // make INSTALLED again
    componentHost = namenodes.get(host1);
    componentHost.handleEvent(new ServiceComponentHostInstallEvent(componentHost.getServiceComponentName(), componentHost.getHostName(), System.currentTimeMillis(), "HDP-1.2.0"));
    componentHost.handleEvent(new ServiceComponentHostOpSucceededEvent(componentHost.getServiceComponentName(), componentHost.getHostName(), System.currentTimeMillis()));
    componentHostRequests.clear();
    componentHostRequests.add(new ServiceComponentHostRequest(cluster1, null, "NAMENODE", host1, "INSTALLED"));
    updateHostComponents(amc, componentHostRequests, mapRequestProps, true);
    assertEquals(State.INSTALLED, namenodes.get(host1).getState());

    // make unknown
    ServiceComponentHost sch = null;
    for (ServiceComponentHost tmp : cluster.getServiceComponentHosts(host2)) {
      if (tmp.getServiceComponentName().equals("DATANODE")) {
        tmp.setState(State.UNKNOWN);
        sch = tmp;
      }
    }
    assertNotNull(sch);

    // make disabled
    componentHostRequests.clear();
    componentHostRequests.add(new ServiceComponentHostRequest(cluster1, null, "DATANODE", host2, "DISABLED"));
    updateHostComponents(amc, componentHostRequests, mapRequestProps, false);
    org.junit.Assert.assertEquals(State.DISABLED, sch.getState());

    // State should not be changed if componentHostRequests are empty
    componentHostRequests.clear();
    mapRequestProps.put(RequestOperationLevel.OPERATION_CLUSTER_ID,cluster1);
    updateHostComponents(amc, componentHostRequests, mapRequestProps, false);
    org.junit.Assert.assertEquals(State.DISABLED, sch.getState());
    mapRequestProps.clear();

    // ServiceComponentHost remains in disabled after service stop
    assertEquals(sch.getServiceComponentName(),"DATANODE");
    serviceRequests.clear();
    serviceRequests.add(new ServiceRequest(cluster1, "HDFS", repositoryVersion120.getId(), "INSTALLED"));
    ServiceResourceProviderTest.updateServices(amc, serviceRequests,
      mapRequestProps, true, false);
    assertEquals(State.DISABLED, sch.getState());

    // ServiceComponentHost remains in disabled after service start
    serviceRequests.clear();
    serviceRequests.add(new ServiceRequest(cluster1, "HDFS", repositoryVersion120.getId(), "STARTED"));
    ServiceResourceProviderTest.updateServices(amc, serviceRequests,
      mapRequestProps, true, false);
    assertEquals(State.DISABLED, sch.getState());

    // confirm delete
    componentHostRequests.clear();
    componentHostRequests.add(new ServiceComponentHostRequest(cluster1, null, "DATANODE", host2, null));
    amc.deleteHostComponents(componentHostRequests);

    sch = null;
    for (ServiceComponentHost tmp : cluster.getServiceComponentHosts(host2)) {
      if (tmp.getServiceComponentName().equals("DATANODE")) {
        sch = tmp;
      }
    }
    org.junit.Assert.assertNull(sch);

    /*
    *Test remove service
    */
    serviceRequests.clear();
    serviceRequests.add(new ServiceRequest(cluster1, "HDFS", repositoryVersion120.getId(), "INSTALLED"));
    ServiceResourceProviderTest.updateServices(amc, serviceRequests, mapRequestProps, true, false);
    serviceRequests.clear();
    serviceRequests.add(new ServiceRequest(cluster1, null, null, null, null));
    org.junit.Assert.assertEquals(2, ServiceResourceProviderTest.getServices(amc, serviceRequests).size());
    serviceRequests.clear();
    serviceRequests.add(new ServiceRequest(cluster1, "HDFS", repositoryVersion120.getId(), null));
    serviceRequests.add(new ServiceRequest(cluster1, "HIVE", repositoryVersion120.getId(), null));
    ServiceResourceProviderTest.deleteServices(amc, serviceRequests);
    serviceRequests.clear();
    serviceRequests.add(new ServiceRequest(cluster1, null, null, null, null));
    org.junit.Assert.assertEquals(0, ServiceResourceProviderTest.getServices(amc, serviceRequests).size());

    /*
    *Test add service again
    */
    serviceRequests.clear();
    serviceRequests.add(new ServiceRequest(cluster1, "HDFS", repositoryVersion120.getId(), null));

    ServiceResourceProviderTest.createServices(amc, repositoryVersionDAO, serviceRequests);

    org.junit.Assert.assertEquals(1, ServiceResourceProviderTest.getServices(amc, serviceRequests).size());
    //Create new configs
    configurationRequest = new ConfigurationRequest(cluster1, "core-site", "version2",
        gson.fromJson("{ \"fs.default.name\" : \"localhost:8020\"}", confType), null);
    amc.createConfiguration(configurationRequest);
    configurationRequest = new ConfigurationRequest(cluster1, "hdfs-site", "version2",
        gson.fromJson("{ \"dfs.datanode.data.dir.perm\" : \"750\"}", confType), null);
    amc.createConfiguration(configurationRequest);
    configurationRequest = new ConfigurationRequest(cluster1, "global", "version2",
        gson.fromJson("{ \"hbase_hdfs_root_dir\" : \"/apps/hbase/\"}", confType), null);
    amc.createConfiguration(configurationRequest);
    //Add configs to service
    serviceRequests.clear();
    serviceRequests.add(new ServiceRequest(cluster1, "HDFS", repositoryVersion120.getId(), null));
    ServiceResourceProviderTest.updateServices(amc, serviceRequests, mapRequestProps, true, false);
    //Crate service components
    serviceComponentRequests = new HashSet<>();
    serviceComponentRequests.add(new ServiceComponentRequest(cluster1, "HDFS", "NAMENODE", null));
    serviceComponentRequests.add(new ServiceComponentRequest(cluster1, "HDFS", "SECONDARY_NAMENODE", null));
    serviceComponentRequests.add(new ServiceComponentRequest(cluster1, "HDFS", "DATANODE", null));
    serviceComponentRequests.add(new ServiceComponentRequest(cluster1, "HDFS", "HDFS_CLIENT", null));
    ComponentResourceProviderTest.createComponents(amc, serviceComponentRequests);

    //Create ServiceComponentHosts
    componentHostRequests = new HashSet<>();
    componentHostRequests.add(new ServiceComponentHostRequest(cluster1, null, "DATANODE", "host1", null));
    componentHostRequests.add(new ServiceComponentHostRequest(cluster1, null, "NAMENODE", "host1", null));
    componentHostRequests.add(new ServiceComponentHostRequest(cluster1, null, "SECONDARY_NAMENODE", host1, null));
    componentHostRequests.add(new ServiceComponentHostRequest(cluster1, null, "DATANODE", "host2", null));
    componentHostRequests.add(new ServiceComponentHostRequest(cluster1, null, "DATANODE", "host3", null));
    amc.createHostComponents(componentHostRequests);


    namenodes = cluster.getService("HDFS").getServiceComponent("NAMENODE").getServiceComponentHosts();
    org.junit.Assert.assertEquals(1, namenodes.size());
    Map<String, ServiceComponentHost> datanodes = cluster.getService("HDFS").getServiceComponent("DATANODE").getServiceComponentHosts();
    org.junit.Assert.assertEquals(3, datanodes.size());
    Map<String, ServiceComponentHost> namenodes2 = cluster.getService("HDFS").getServiceComponent("SECONDARY_NAMENODE").getServiceComponentHosts();
    org.junit.Assert.assertEquals(1, namenodes2.size());

  }

  @Test
  public void testScheduleSmokeTest() throws Exception {

    final String HOST1 = getUniqueName();
    final String OS_TYPE = "centos5";
    final String STACK_ID = "HDP-2.0.1";
    final String CLUSTER_NAME = getUniqueName();
    final String HDFS_SERVICE_CHECK_ROLE = "HDFS_SERVICE_CHECK";
    final String MAPREDUCE2_SERVICE_CHECK_ROLE = "MAPREDUCE2_SERVICE_CHECK";
    final String YARN_SERVICE_CHECK_ROLE = "YARN_SERVICE_CHECK";

    Map<String,String> mapRequestProps = Collections.emptyMap();


    AmbariManagementController amc = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = injector.getInstance(Clusters.class);

    clusters.addHost(HOST1);
    Host host = clusters.getHost(HOST1);
    setOsFamily(host, "redhat", "5.9");
    clusters.getHost(HOST1).setState(HostState.HEALTHY);
    clusters.updateHostMappings(host);

    ClusterRequest clusterRequest = new ClusterRequest(null, CLUSTER_NAME, STACK_ID, null);
    amc.createCluster(clusterRequest);

    Set<ServiceRequest> serviceRequests = new HashSet<>();
    serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "HDFS", repositoryVersion201.getId(), null));
    serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "MAPREDUCE2", repositoryVersion201.getId(), null));
    serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "YARN", repositoryVersion201.getId(), null));

    ServiceResourceProviderTest.createServices(amc, repositoryVersionDAO, serviceRequests);

    Set<ServiceComponentRequest> serviceComponentRequests = new HashSet<>();
    serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "HDFS", "NAMENODE", null));
    serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "HDFS", "SECONDARY_NAMENODE", null));
    serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "HDFS", "DATANODE", null));
    serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "MAPREDUCE2", "HISTORYSERVER", null));
    serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "YARN", "RESOURCEMANAGER", null));
    serviceComponentRequests.add(new ServiceComponentRequest(CLUSTER_NAME, "YARN", "NODEMANAGER", null));

    ComponentResourceProviderTest.createComponents(amc, serviceComponentRequests);

    Set<HostRequest> hostRequests = new HashSet<>();
    hostRequests.add(new HostRequest(HOST1, CLUSTER_NAME));

    HostResourceProviderTest.createHosts(amc, hostRequests);

    for (Host clusterHost : clusters.getHosts()) {
      clusters.updateHostMappings(clusterHost);
    }

    Set<ServiceComponentHostRequest> componentHostRequests = new HashSet<>();
    componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, null, "DATANODE", HOST1, null));
    componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, null, "NAMENODE", HOST1, null));
    componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, null, "SECONDARY_NAMENODE", HOST1, null));
    componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, null, "HISTORYSERVER", HOST1, null));
    componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, null, "RESOURCEMANAGER", HOST1, null));
    componentHostRequests.add(new ServiceComponentHostRequest(CLUSTER_NAME, null, "NODEMANAGER", HOST1, null));

    amc.createHostComponents(componentHostRequests);

    //Install services
    serviceRequests.clear();
    serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "HDFS", repositoryVersion201.getId(), State.INSTALLED.name()));
    serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "MAPREDUCE2", repositoryVersion201.getId(), State.INSTALLED.name()));
    serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "YARN", repositoryVersion201.getId(), State.INSTALLED.name()));

    ServiceResourceProviderTest.updateServices(amc, serviceRequests, mapRequestProps, true, false);

    Cluster cluster = clusters.getCluster(CLUSTER_NAME);

    for (String serviceName : cluster.getServices().keySet() ) {

      for(String componentName: cluster.getService(serviceName).getServiceComponents().keySet()) {

        Map<String, ServiceComponentHost> serviceComponentHosts = cluster.getService(serviceName).getServiceComponent(componentName).getServiceComponentHosts();

        for (Map.Entry<String, ServiceComponentHost> entry : serviceComponentHosts.entrySet()) {
          ServiceComponentHost cHost = entry.getValue();
          cHost.handleEvent(new ServiceComponentHostInstallEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis(), STACK_ID));
          cHost.handleEvent(new ServiceComponentHostOpSucceededEvent(cHost.getServiceComponentName(), cHost.getHostName(), System.currentTimeMillis()));
        }
      }
    }

    //Start services
    serviceRequests.clear();
    serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "HDFS", repositoryVersion201.getId(), State.STARTED.name()));
    serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "MAPREDUCE2", repositoryVersion201.getId(), State.STARTED.name()));
    serviceRequests.add(new ServiceRequest(CLUSTER_NAME, "YARN", repositoryVersion201.getId(), State.STARTED.name()));

    RequestStatusResponse response = ServiceResourceProviderTest.updateServices(amc, serviceRequests,
        mapRequestProps, true, false);

    Collection<?> hdfsSmokeTasks = CollectionUtils.select(response.getTasks(), new RolePredicate(HDFS_SERVICE_CHECK_ROLE));
    //Ensure that smoke test task was created for HDFS
    org.junit.Assert.assertEquals(1, hdfsSmokeTasks.size());

    Collection<?> mapreduce2SmokeTasks = CollectionUtils.select(response.getTasks(), new RolePredicate(MAPREDUCE2_SERVICE_CHECK_ROLE));
    //Ensure that smoke test task was created for MAPREDUCE2
    org.junit.Assert.assertEquals(1, mapreduce2SmokeTasks.size());

    Collection<?> yarnSmokeTasks = CollectionUtils.select(response.getTasks(), new RolePredicate(YARN_SERVICE_CHECK_ROLE));
    //Ensure that smoke test task was created for YARN
    org.junit.Assert.assertEquals(1, yarnSmokeTasks.size());
  }

  @Test
  public void testGetServices2() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();
    Clusters clusters = createNiceMock(Clusters.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    ServiceResponse response = createNiceMock(ServiceResponse.class);

    // requests
    ServiceRequest request1 = new ServiceRequest("cluster1", "service1", null, null, null);

    Set<ServiceRequest> setRequests = new HashSet<>();
    setRequests.add(request1);

    // expectations
    // constructor init
    AmbariManagementControllerImplTest.constructorInit(injector, controllerCapture, null, maintHelper,
        createStrictMock(KerberosHelper.class), null, null);

    // getServices
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(cluster.getService("service1")).andReturn(service);

    expect(service.convertToResponse()).andReturn(response);
    // replay mocks
    replay(maintHelper, injector, clusters, cluster, service, response);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    Set<ServiceResponse> setResponses = ServiceResourceProviderTest.getServices(controller, setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(1, setResponses.size());
    assertTrue(setResponses.contains(response));

    verify(injector, clusters, cluster, service, response);
  }

  /**
   * Ensure that ServiceNotFoundException is propagated in case where there is a single request.
   */
  @Test
  public void testGetServices___ServiceNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();
    Clusters clusters = createNiceMock(Clusters.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);
    Cluster cluster = createNiceMock(Cluster.class);

    // requests
    ServiceRequest request1 = new ServiceRequest("cluster1", "service1", null, null, null);
    Set<ServiceRequest> setRequests = new HashSet<>();
    setRequests.add(request1);

    // expectations
    // constructor init
    AmbariManagementControllerImplTest.constructorInit(injector, controllerCapture, null, maintHelper,
        createStrictMock(KerberosHelper.class), null, null);

    // getServices
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(cluster.getService("service1")).andThrow(new ServiceNotFoundException("custer1", "service1"));

    // replay mocks
    replay(maintHelper, injector, clusters, cluster);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);

    // assert that exception is thrown in case where there is a single request
    try {
      ServiceResourceProviderTest.getServices(controller, setRequests);
      fail("expected ServiceNotFoundException");
    } catch (ServiceNotFoundException e) {
      // expected
    }

    assertSame(controller, controllerCapture.getValue());
    verify(injector, clusters, cluster);
  }

  /**
   * Ensure that ServiceNotFoundException is handled where there are multiple requests as would be the
   * case when an OR predicate is provided in the query.
   */
  @Test
  public void testGetServices___OR_Predicate_ServiceNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();
    Clusters clusters = createNiceMock(Clusters.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Service service1 = createNiceMock(Service.class);
    Service service2 = createNiceMock(Service.class);
    ServiceResponse response = createNiceMock(ServiceResponse.class);
    ServiceResponse response2 = createNiceMock(ServiceResponse.class);

    // requests
    ServiceRequest request1 = new ServiceRequest("cluster1", "service1", null, null, null);
    ServiceRequest request2 = new ServiceRequest("cluster1", "service2", null, null, null);
    ServiceRequest request3 = new ServiceRequest("cluster1", "service3", null, null, null);
    ServiceRequest request4 = new ServiceRequest("cluster1", "service4", null, null, null);

    Set<ServiceRequest> setRequests = new HashSet<>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);
    setRequests.add(request4);

    // expectations
    // constructor init
    AmbariManagementControllerImplTest.constructorInit(injector, controllerCapture, null, maintHelper,
        createStrictMock(KerberosHelper.class), null, null);

    // getServices
    expect(clusters.getCluster("cluster1")).andReturn(cluster).times(4);
    expect(cluster.getService("service1")).andReturn(service1);
    expect(cluster.getService("service2")).andThrow(new ServiceNotFoundException("cluster1", "service2"));
    expect(cluster.getService("service3")).andThrow(new ServiceNotFoundException("cluster1", "service3"));
    expect(cluster.getService("service4")).andReturn(service2);

    expect(service1.convertToResponse()).andReturn(response);
    expect(service2.convertToResponse()).andReturn(response2);
    // replay mocks
    replay(maintHelper, injector, clusters, cluster, service1, service2,
      response, response2);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);
    Set<ServiceResponse> setResponses = ServiceResourceProviderTest.getServices(controller, setRequests);

    // assert and verify
    assertSame(controller, controllerCapture.getValue());
    assertEquals(2, setResponses.size());
    assertTrue(setResponses.contains(response));
    assertTrue(setResponses.contains(response2));

    verify(injector, clusters, cluster, service1, service2, response, response2);
  }

  private void testRunSmokeTestFlag(Map<String, String> mapRequestProps,
                                    AmbariManagementController amc,
                                    Set<ServiceRequest> serviceRequests)
      throws Exception, AuthorizationException {
    RequestStatusResponse response;//Starting HDFS service. No run_smoke_test flag is set, smoke

    String cluster1 = getUniqueName();

    //Stopping HDFS service
    serviceRequests.clear();
    serviceRequests.add(new ServiceRequest(cluster1, "HDFS", repositoryVersion02.getId(), "INSTALLED"));
    ServiceResourceProviderTest.updateServices(amc, serviceRequests, mapRequestProps, false,
        false);

    //Starting HDFS service. No run_smoke_test flag is set, smoke
    // test(HDFS_SERVICE_CHECK) won't run
    boolean runSmokeTest = false;
    serviceRequests.clear();
    serviceRequests.add(new ServiceRequest(cluster1, "HDFS", repositoryVersion02.getId(), "STARTED"));
    response = ServiceResourceProviderTest.updateServices(amc, serviceRequests, mapRequestProps,
        runSmokeTest, false);

    List<ShortTaskStatus> taskStatuses = response.getTasks();
    boolean smokeTestRequired = false;
    for (ShortTaskStatus shortTaskStatus : taskStatuses) {
      if (shortTaskStatus.getRole().equals(Role.HDFS_SERVICE_CHECK.toString())) {
        smokeTestRequired= true;
      }
    }
    assertFalse(smokeTestRequired);

    //Stopping HDFS service
    serviceRequests.clear();
    serviceRequests.add(new ServiceRequest(cluster1, "HDFS", repositoryVersion02.getId(), "INSTALLED"));
    ServiceResourceProviderTest.updateServices(amc, serviceRequests, mapRequestProps, false,
        false);

    //Starting HDFS service again.
    //run_smoke_test flag is set, smoke test will be run
    runSmokeTest = true;
    serviceRequests.clear();
    serviceRequests.add(new ServiceRequest(cluster1, "HDFS", repositoryVersion02.getId(), "STARTED"));
    response = ServiceResourceProviderTest.updateServices(amc, serviceRequests, mapRequestProps,
        runSmokeTest, false);

    taskStatuses = response.getTasks();
    smokeTestRequired = false;
    for (ShortTaskStatus shortTaskStatus : taskStatuses) {
      if (shortTaskStatus.getRole().equals(Role.HDFS_SERVICE_CHECK.toString())) {
        smokeTestRequired= true;
      }
    }
    assertTrue(smokeTestRequired);
  }

  private class RolePredicate implements org.apache.commons.collections.Predicate {

    private String role;

    public RolePredicate(String role) {
      this.role = role;
    }

    @Override
    public boolean evaluate(Object obj) {
      ShortTaskStatus task = (ShortTaskStatus)obj;
      return task.getRole().equals(role);
    }
  }

  @Test
  public void testReinstallClientSchSkippedInMaintenance() throws Exception {
    String cluster1 = getUniqueName();
    final String host1 = getUniqueName();
    final String host2 = getUniqueName();
    final String host3 = getUniqueName();

    Cluster c1 = setupClusterWithHosts(cluster1, "HDP-1.2.0",
      new ArrayList<String>() {{
        add(host1);
        add(host2);
        add(host3);
      }},
      "centos5");

    RepositoryVersionEntity repositoryVersion = repositoryVersion120;

    Service hdfs = c1.addService("HDFS", repositoryVersion);
    createServiceComponent(cluster1, "HDFS", "NAMENODE", State.INIT);
    createServiceComponent(cluster1, "HDFS", "DATANODE", State.INIT);
    createServiceComponent(cluster1, "HDFS", "HDFS_CLIENT", State.INIT);

    createServiceComponentHost(cluster1, "HDFS", "NAMENODE", host1, State.INIT);
    createServiceComponentHost(cluster1, "HDFS", "DATANODE", host1, State.INIT);
    createServiceComponentHost(cluster1, "HDFS", "HDFS_CLIENT", host1, State.INIT);
    createServiceComponentHost(cluster1, "HDFS", "HDFS_CLIENT", host2, State.INIT);
    createServiceComponentHost(cluster1, "HDFS", "HDFS_CLIENT", host3, State.INIT);

    installService(cluster1, "HDFS", false, false);

    clusters.getHost(host3).setMaintenanceState(c1.getClusterId(), MaintenanceState.ON);

    Long id = startService(cluster1, "HDFS", false ,true);

    Assert.assertNotNull(id);
    List<Stage> stages = actionDB.getAllStages(id);
    Assert.assertNotNull(stages);
    HostRoleCommand hrc1 = null;
    HostRoleCommand hrc2 = null;
    HostRoleCommand hrc3 = null;
    for (Stage s : stages) {
      for (HostRoleCommand hrc : s.getOrderedHostRoleCommands()) {
        if (hrc.getRole().equals(Role.HDFS_CLIENT) && hrc.getHostName().equals(host1)) {
          hrc1 = hrc;
        } else if (hrc.getRole().equals(Role.HDFS_CLIENT) && hrc.getHostName().equals(host2)) {
          hrc2 = hrc;
        } else if (hrc.getRole().equals(Role.HDFS_CLIENT) && hrc.getHostName().equals(host3)) {
          hrc3 = hrc;
        }
      }
    }

    Assert.assertNotNull(hrc1);
    Assert.assertNotNull(hrc2);
    Assert.assertNull(hrc3);
  }

  @Test
  public void setMonitoringServicesRestartRequired() throws Exception {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    Cluster cluster = clusters.getCluster(cluster1);
    StackId stackId = new StackId("HDP-2.0.8");
    cluster.setDesiredStackVersion(stackId);
    cluster.setCurrentStackVersion(stackId);

    String hdfsService = "HDFS";
    String fakeMonitoringService = "FAKENAGIOS";
    createService(cluster1, hdfsService, repositoryVersion208, null);
    createService(cluster1, fakeMonitoringService, repositoryVersion208, null);

    String namenode = "NAMENODE";
    String datanode = "DATANODE";
    String hdfsClient = "HDFS_CLIENT";
    String fakeServer = "FAKE_MONITORING_SERVER";

    createServiceComponent(cluster1, hdfsService, namenode, State.INIT);
    createServiceComponent(cluster1, hdfsService, datanode, State.INIT);
    createServiceComponent(cluster1, fakeMonitoringService, fakeServer, State.INIT);

    String host1 = getUniqueName();

    addHostToCluster(host1, cluster1);
    createServiceComponentHost(cluster1, hdfsService, namenode, host1, null);
    createServiceComponentHost(cluster1, hdfsService, datanode, host1, null);
    createServiceComponentHost(cluster1, fakeMonitoringService, fakeServer, host1,
      null);


    ServiceComponentHost monitoringServiceComponentHost = null;
    for (ServiceComponentHost sch : cluster.getServiceComponentHosts(host1)) {
      if (sch.getServiceComponentName().equals(fakeServer)) {
        monitoringServiceComponentHost = sch;
      }
    }

    assertFalse(monitoringServiceComponentHost.isRestartRequired());

    createServiceComponent(cluster1, hdfsService, hdfsClient,
      State.INIT);

    createServiceComponentHost(cluster1, hdfsService, hdfsClient, host1, null);

    assertTrue(monitoringServiceComponentHost.isRestartRequired());
  }

  @Test
  public void setRestartRequiredAfterChangeService() throws Exception {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    Cluster cluster = clusters.getCluster(cluster1);
    StackId stackId = new StackId("HDP-2.0.7");
    cluster.setDesiredStackVersion(stackId);
    cluster.setCurrentStackVersion(stackId);

    String hdfsService = "HDFS";
    String zookeeperService = "ZOOKEEPER";
    createService(cluster1, hdfsService, repositoryVersion207, null);
    createService(cluster1, zookeeperService, repositoryVersion207, null);

    String namenode = "NAMENODE";
    String datanode = "DATANODE";
    String hdfsClient = "HDFS_CLIENT";
    String zookeeperServer = "ZOOKEEPER_SERVER";
    String zookeeperClient = "ZOOKEEPER_CLIENT";

    createServiceComponent(cluster1, hdfsService, namenode,
      State.INIT);
    createServiceComponent(cluster1, hdfsService, datanode,
      State.INIT);
    createServiceComponent(cluster1, zookeeperService, zookeeperServer,
      State.INIT);
    createServiceComponent(cluster1, zookeeperService, zookeeperClient,
      State.INIT);

    String host1 = getUniqueName();
    String host2 = getUniqueName();

    addHostToCluster(host1, cluster1);
    createServiceComponentHost(cluster1, hdfsService, namenode, host1, null);
    createServiceComponentHost(cluster1, hdfsService, datanode, host1, null);
    createServiceComponentHost(cluster1, zookeeperService, zookeeperServer, host1,
      null);
    createServiceComponentHost(cluster1, zookeeperService, zookeeperClient, host1,
      null);

    ServiceComponentHost zookeeperSch = null;
    for (ServiceComponentHost sch : cluster.getServiceComponentHosts(host1)) {
      if (sch.getServiceComponentName().equals(zookeeperServer)) {
        zookeeperSch = sch;
      }
    }
    assertFalse(zookeeperSch.isRestartRequired());

    addHostToCluster(host2, cluster1);
    createServiceComponentHost(cluster1, zookeeperService, zookeeperClient, host2, null);

    assertFalse(zookeeperSch.isRestartRequired());  //No restart required if adding host

    createServiceComponentHost(cluster1, zookeeperService, zookeeperServer, host2, null);

    assertTrue(zookeeperSch.isRestartRequired());  //Add zk server required restart

    deleteServiceComponentHost(cluster1, zookeeperService, zookeeperServer, host2, null);
    deleteServiceComponentHost(cluster1, zookeeperService, zookeeperClient, host2, null);
    deleteHost(host2);

    assertTrue(zookeeperSch.isRestartRequired());   //Restart if removing host!
  }

  @Test
  public void testRestartIndicatorsAndSlaveFilesUpdateAtComponentsDelete() throws Exception {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    Cluster cluster = clusters.getCluster(cluster1);
    StackId stackId = new StackId("HDP-2.0.7");
    cluster.setDesiredStackVersion(stackId);
    cluster.setCurrentStackVersion(stackId);

    String hdfsService = "HDFS";
    String zookeeperService = "ZOOKEEPER";
    createService(cluster1, hdfsService, null);
    createService(cluster1, zookeeperService, null);

    String namenode = "NAMENODE";
    String datanode = "DATANODE";
    String zookeeperServer = "ZOOKEEPER_SERVER";
    String zookeeperClient = "ZOOKEEPER_CLIENT";

    createServiceComponent(cluster1, hdfsService, namenode,
        State.INIT);
    createServiceComponent(cluster1, hdfsService, datanode,
        State.INIT);
    createServiceComponent(cluster1, zookeeperService, zookeeperServer,
        State.INIT);
    createServiceComponent(cluster1, zookeeperService, zookeeperClient,
        State.INIT);

    String host1 = getUniqueName();
    String host2 = getUniqueName();

    addHostToCluster(host1, cluster1);
    createServiceComponentHost(cluster1, hdfsService, namenode, host1, null);
    createServiceComponentHost(cluster1, hdfsService, datanode, host1, null);
    createServiceComponentHost(cluster1, zookeeperService, zookeeperServer, host1,
        null);
    createServiceComponentHost(cluster1, zookeeperService, zookeeperClient, host1,
        null);

    ServiceComponentHost nameNodeSch = null;
    for (ServiceComponentHost sch : cluster.getServiceComponentHosts(host1)) {
      if (sch.getServiceComponentName().equals(namenode)) {
        nameNodeSch = sch;
      }
    }

    assertFalse(nameNodeSch.isRestartRequired());

    addHostToCluster(host2, cluster1);

    createServiceComponentHost(cluster1, hdfsService, datanode, host2, null);
    assertFalse(nameNodeSch.isRestartRequired());  //No restart required if adding host

    deleteServiceComponentHost(cluster1, hdfsService, datanode, host2, null);
    deleteHost(host2);

    assertFalse(nameNodeSch.isRestartRequired());   //NameNode doesn't need to be restarted!

    List<Long> requestIDs = actionDB.getRequestsByStatus(null, 1, false);
    Request request = actionDB.getRequest(requestIDs.get(0));
    assertEquals("Update Include/Exclude Files for [HDFS]", request.getRequestContext());
    assertEquals(false, request.isExclusive());
    Type type = new TypeToken<Map<String, String>>(){}.getType();
    Map<String, String> requestParams = StageUtils.getGson().fromJson(request.getInputs(), type);
    assertEquals(2, requestParams.size());
    assertEquals("true", requestParams.get("is_add_or_delete_slave_request"));
    assertEquals("true", requestParams.get("update_files_only"));
    assertEquals(1, request.getResourceFilters().size());
    RequestResourceFilter resourceFilter = request.getResourceFilters().get(0);
    assertEquals(resourceFilter.getServiceName(), hdfsService);
    assertEquals(resourceFilter.getComponentName(), namenode);
    assertEquals(resourceFilter.getHostNames(), new ArrayList<String>());
  }

  @Test
  public void testMaintenanceState() throws Exception {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    clusters.getCluster(cluster1).setDesiredStackVersion(
        new StackId("HDP-1.2.0"));

    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);

    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(cluster1, serviceName, componentName1, State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2, State.INIT);
    createServiceComponent(cluster1, serviceName, componentName3, State.INIT);

    String host1 = getUniqueName();
    String host2 = getUniqueName();

    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);

    createServiceComponentHost(cluster1, serviceName, componentName1, host1,
        null);
    createServiceComponentHost(cluster1, serviceName, componentName2, host1,
        null);
    createServiceComponentHost(cluster1, serviceName, componentName2, host2,
        null);

    Map<String, String> requestProperties = new HashMap<>();
    requestProperties.put("context", "Called from a test");

    Cluster cluster = clusters.getCluster(cluster1);
    Service service = cluster.getService(serviceName);
    Map<String, Host> hosts = clusters.getHostsForCluster(cluster1);

    MaintenanceStateHelper maintenanceStateHelper = MaintenanceStateHelperTest.getMaintenanceStateHelperInstance(clusters);

    // test updating a service
    ServiceRequest sr = new ServiceRequest(cluster1, serviceName, repositoryVersion120.getId(), null);
    sr.setMaintenanceState(MaintenanceState.ON.name());
    ServiceResourceProviderTest.updateServices(controller,
        Collections.singleton(sr), requestProperties, false, false,
        maintenanceStateHelper);
    Assert.assertEquals(MaintenanceState.ON, service.getMaintenanceState());

    // check the host components implied state vs desired state
    for (ServiceComponent sc : service.getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        Assert.assertEquals(MaintenanceState.IMPLIED_FROM_SERVICE,
            controller.getEffectiveMaintenanceState(sch));
        Assert.assertEquals(MaintenanceState.OFF, sch.getMaintenanceState());
      }
    }

    // reset
    sr.setMaintenanceState(MaintenanceState.OFF.name());
    ServiceResourceProviderTest.updateServices(controller,
        Collections.singleton(sr), requestProperties, false, false,
        maintenanceStateHelper);
    Assert.assertEquals(MaintenanceState.OFF, service.getMaintenanceState());

    // check the host components implied state vs desired state
    for (ServiceComponent sc : service.getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        Assert.assertEquals(MaintenanceState.OFF,
            controller.getEffectiveMaintenanceState(sch));
        Assert.assertEquals(MaintenanceState.OFF, sch.getMaintenanceState());
      }
    }

    // passivate a host
    HostRequest hr = new HostRequest(host1, cluster1);
    hr.setMaintenanceState(MaintenanceState.ON.name());
    HostResourceProviderTest.updateHosts(controller, Collections.singleton(hr)
    );

    Host host = hosts.get(host1);
    Assert.assertEquals(MaintenanceState.ON,
        host.getMaintenanceState(cluster.getClusterId()));

    // check the host components implied state vs desired state, only for
    // affected hosts
    for (ServiceComponent sc : service.getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        MaintenanceState implied = controller.getEffectiveMaintenanceState(sch);
        if (sch.getHostName().equals(host1)) {
          Assert.assertEquals(MaintenanceState.IMPLIED_FROM_HOST, implied);
        } else {
          Assert.assertEquals(MaintenanceState.OFF, implied);
        }
        Assert.assertEquals(MaintenanceState.OFF, sch.getMaintenanceState());
      }
    }

    // reset
    hr.setMaintenanceState(MaintenanceState.OFF.name());
    HostResourceProviderTest.updateHosts(controller, Collections.singleton(hr)
    );

    host = hosts.get(host1);
    Assert.assertEquals(MaintenanceState.OFF,
        host.getMaintenanceState(cluster.getClusterId()));

    // check the host components active state vs desired state
    for (ServiceComponent sc : service.getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        Assert.assertEquals(MaintenanceState.OFF,
            controller.getEffectiveMaintenanceState(sch));
        Assert.assertEquals(MaintenanceState.OFF, sch.getMaintenanceState());
      }
    }

    // passivate several hosts
    HostRequest hr1 = new HostRequest(host1, cluster1);
    hr1.setMaintenanceState(MaintenanceState.ON.name());
    HostRequest hr2 = new HostRequest(host2, cluster1);
    hr2.setMaintenanceState(MaintenanceState.ON.name());
    Set<HostRequest> set = new HashSet<>();
    set.add(hr1);
    set.add(hr2);
    HostResourceProviderTest.updateHosts(controller, set
    );

    host = hosts.get(host1);
    Assert.assertEquals(MaintenanceState.ON,
        host.getMaintenanceState(cluster.getClusterId()));
    host = hosts.get(host2);
    Assert.assertEquals(MaintenanceState.ON,
        host.getMaintenanceState(cluster.getClusterId()));

    // reset
    hr1 = new HostRequest(host1, cluster1);
    hr1.setMaintenanceState(MaintenanceState.OFF.name());
    hr2 = new HostRequest(host2, cluster1);
    hr2.setMaintenanceState(MaintenanceState.OFF.name());
    set = new HashSet<>();
    set.add(hr1);
    set.add(hr2);

    HostResourceProviderTest.updateHosts(controller, set
    );
    host = hosts.get(host1);
    Assert.assertEquals(MaintenanceState.OFF,
        host.getMaintenanceState(cluster.getClusterId()));
    host = hosts.get(host2);
    Assert.assertEquals(MaintenanceState.OFF,
        host.getMaintenanceState(cluster.getClusterId()));

    // only do one SCH
    ServiceComponentHost targetSch = service.getServiceComponent(componentName2).getServiceComponentHosts().get(
        host2);
    Assert.assertNotNull(targetSch);
    targetSch.setMaintenanceState(MaintenanceState.ON);

    // check the host components active state vs desired state
    Assert.assertEquals(MaintenanceState.ON,
        controller.getEffectiveMaintenanceState(targetSch));

    // update the service
    service.setMaintenanceState(MaintenanceState.ON);
    Assert.assertEquals(MaintenanceState.ON,
        controller.getEffectiveMaintenanceState(targetSch));

    // make SCH active
    targetSch.setMaintenanceState(MaintenanceState.OFF);
    Assert.assertEquals(MaintenanceState.IMPLIED_FROM_SERVICE,
        controller.getEffectiveMaintenanceState(targetSch));

    // update the service
    service.setMaintenanceState(MaintenanceState.OFF);
    Assert.assertEquals(MaintenanceState.OFF,
        controller.getEffectiveMaintenanceState(targetSch));

    host = hosts.get(host2);
    // update host
    host.setMaintenanceState(cluster.getClusterId(), MaintenanceState.ON);
    Assert.assertEquals(MaintenanceState.IMPLIED_FROM_HOST,
        controller.getEffectiveMaintenanceState(targetSch));

    targetSch.setMaintenanceState(MaintenanceState.ON);
    Assert.assertEquals(MaintenanceState.ON,
        controller.getEffectiveMaintenanceState(targetSch));

    // check the host components active state vs desired state
    for (ServiceComponent sc : service.getServiceComponents().values()) {
      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        Assert.assertEquals(State.INIT, sch.getState());
      }
    }

    long id1 = installService(cluster1, serviceName, false, false,
        maintenanceStateHelper, null);

    List<HostRoleCommand> hdfsCmds = actionDB.getRequestTasks(id1);
    Assert.assertNotNull(hdfsCmds);

    HostRoleCommand datanodeCmd = null;

    for (HostRoleCommand cmd : hdfsCmds) {
      if (cmd.getRole().equals(Role.DATANODE)) {
        datanodeCmd = cmd;
      }
    }

    Assert.assertNotNull(datanodeCmd);

    // verify passive sch was skipped
    for (ServiceComponent sc : service.getServiceComponents().values()) {
      if (!sc.getName().equals(componentName2)) {
        continue;
      }

      for (ServiceComponentHost sch : sc.getServiceComponentHosts().values()) {
        Assert.assertEquals(sch == targetSch ? State.INIT : State.INSTALLED,
            sch.getState());
      }
    }
  }

  @Test
  public void testCredentialStoreRelatedAPICallsToUpdateSettings() throws Exception {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    clusters.getCluster(cluster1).setDesiredStackVersion(
        new StackId("HDP-2.2.0"));

    String service1Name = "HDFS";
    String service2Name = "STORM";
    String service3Name = "ZOOKEEPER";
    createService(cluster1, service1Name, repositoryVersion220, null);
    createService(cluster1, service2Name, repositoryVersion220, null);
    createService(cluster1, service3Name, repositoryVersion220, null);
    String component1Name = "NAMENODE";
    String component2Name = "DRPC_SERVER";
    String component3Name = "ZOOKEEPER_SERVER";
    createServiceComponent(cluster1, service1Name, component1Name, State.INIT);
    createServiceComponent(cluster1, service2Name, component2Name, State.INIT);
    createServiceComponent(cluster1, service3Name, component3Name, State.INIT);
    String host1 = getUniqueName();
    addHostToCluster(host1, cluster1);
    createServiceComponentHost(cluster1, service1Name, component1Name, host1, null);
    createServiceComponentHost(cluster1, service2Name, component2Name, host1, null);
    createServiceComponentHost(cluster1, service3Name, component3Name, host1, null);

    Map<String, String> requestProperties = new HashMap<>();
    requestProperties.put("context", "Called from a test");

    Cluster cluster = clusters.getCluster(cluster1);
    Service service1 = cluster.getService(service1Name);

    MaintenanceStateHelper
        maintenanceStateHelper =
        MaintenanceStateHelperTest.getMaintenanceStateHelperInstance(clusters);

    // test updating a service
    ServiceRequest sr = new ServiceRequest(cluster1, service1Name, repositoryVersion220.getId(), null);
    sr.setCredentialStoreEnabled("true");

    ServiceResourceProviderTest.updateServices(controller,
                                               Collections.singleton(sr), requestProperties, false, false,
                                               maintenanceStateHelper);
    Assert.assertTrue(service1.isCredentialStoreEnabled());
    Assert.assertTrue(service1.isCredentialStoreSupported());
    Assert.assertFalse(service1.isCredentialStoreRequired());

    ServiceRequest sr2 = new ServiceRequest(cluster1, service2Name, repositoryVersion220.getId(), null);
    sr2.setCredentialStoreEnabled("true");
    try {
      ServiceResourceProviderTest.updateServices(controller,
                                                 Collections.singleton(sr2), requestProperties, false, false,
                                                 maintenanceStateHelper);
      Assert.assertTrue("Expected exception not thrown - service does not support cred store", true);
    }catch(IllegalArgumentException iaex) {
      Assert.assertTrue(iaex.getMessage(), iaex.getMessage().contains(
          "Invalid arguments, cannot enable credential store as it is not supported by the service. Service=STORM"));
    }

    ServiceRequest sr3 = new ServiceRequest(cluster1, service3Name, repositoryVersion220.getId(), null);
    sr3.setCredentialStoreEnabled("false");
    try {
      ServiceResourceProviderTest.updateServices(controller,
                                                 Collections.singleton(sr3), requestProperties, false, false,
                                                 maintenanceStateHelper);
      Assert.assertTrue("Expected exception not thrown - service does not support disabling of cred store", true);
    }catch(IllegalArgumentException iaex) {
      Assert.assertTrue(iaex.getMessage(), iaex.getMessage().contains(
          "Invalid arguments, cannot disable credential store as it is required by the service. Service=ZOOKEEPER"));
    }

    ServiceRequest sr4 = new ServiceRequest(cluster1, service3Name, repositoryVersion220.getId(), null);
    sr4.setCredentialStoreSupported("true");
    try {
      ServiceResourceProviderTest.updateServices(controller,
                                                 Collections.singleton(sr4), requestProperties, false, false,
                                                 maintenanceStateHelper);
      Assert.assertTrue("Expected exception not thrown - service does not support updating cred store support", true);
    }catch(IllegalArgumentException iaex) {
      Assert.assertTrue(iaex.getMessage(), iaex.getMessage().contains(
          "Invalid arguments, cannot update credential_store_supported as it is set only via service definition. Service=ZOOKEEPER"));
    }
  }

  @Test
  public void testPassiveSkipServices() throws Exception {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    clusters.getCluster(cluster1)
        .setDesiredStackVersion(new StackId("HDP-0.1"));

    String serviceName1 = "HDFS";
    String serviceName2 = "MAPREDUCE";
    createService(cluster1, serviceName1, null);
    createService(cluster1, serviceName2, null);

    String componentName1_1 = "NAMENODE";
    String componentName1_2 = "DATANODE";
    String componentName1_3 = "HDFS_CLIENT";
    createServiceComponent(cluster1, serviceName1, componentName1_1,
        State.INIT);
    createServiceComponent(cluster1, serviceName1, componentName1_2,
        State.INIT);
    createServiceComponent(cluster1, serviceName1, componentName1_3,
        State.INIT);

    String componentName2_1 = "JOBTRACKER";
    String componentName2_2 = "TASKTRACKER";
    createServiceComponent(cluster1, serviceName2, componentName2_1,
        State.INIT);
    createServiceComponent(cluster1, serviceName2, componentName2_2,
        State.INIT);

    String host1 = getUniqueName();
    String host2 = getUniqueName();

    addHostToCluster(host1, cluster1);
    addHostToCluster(host2, cluster1);

    createServiceComponentHost(cluster1, serviceName1, componentName1_1, host1, null);
    createServiceComponentHost(cluster1, serviceName1, componentName1_2, host1, null);
    createServiceComponentHost(cluster1, serviceName1, componentName1_2, host2, null);

    createServiceComponentHost(cluster1, serviceName2, componentName2_1, host1, null);
    createServiceComponentHost(cluster1, serviceName2, componentName2_2, host2, null);

    MaintenanceStateHelper maintenanceStateHelper =
            MaintenanceStateHelperTest.getMaintenanceStateHelperInstance(clusters);

    installService(cluster1, serviceName1, false, false, maintenanceStateHelper, null);
    installService(cluster1, serviceName2, false, false, maintenanceStateHelper, null);

    startService(cluster1, serviceName1, false, false, maintenanceStateHelper);
    startService(cluster1, serviceName2, false, false, maintenanceStateHelper);

    Map<String, String> requestProperties = new HashMap<>();
    requestProperties.put("context", "Called from a test");

    Cluster cluster = clusters.getCluster(cluster1);

    for (Service service : cluster.getServices().values()) {
      Assert.assertEquals(State.STARTED, service.getDesiredState());
    }

    Service service2 = cluster.getService(serviceName2);
    service2.setMaintenanceState(MaintenanceState.ON);

    Set<ServiceRequest> srs = new HashSet<>();
    srs.add(new ServiceRequest(cluster1, serviceName1, repositoryVersion01.getId(), State.INSTALLED.name()));
    srs.add(new ServiceRequest(cluster1, serviceName2, repositoryVersion01.getId(), State.INSTALLED.name()));
    RequestStatusResponse rsr = ServiceResourceProviderTest.updateServices(controller, srs,
            requestProperties, false, false, maintenanceStateHelper);

    for (ShortTaskStatus sts : rsr.getTasks()) {
      String role = sts.getRole();
      Assert.assertFalse(role.equals(componentName2_1));
      Assert.assertFalse(role.equals(componentName2_2));
    }

    for (Service service : cluster.getServices().values()) {
      if (service.getName().equals(serviceName2)) {
        Assert.assertEquals(State.STARTED, service.getDesiredState());
      } else {
        Assert.assertEquals(State.INSTALLED, service.getDesiredState());
      }
    }

    service2.setMaintenanceState(MaintenanceState.OFF);
    ServiceResourceProviderTest.updateServices(controller, srs, requestProperties,
            false, false, maintenanceStateHelper);
    for (Service service : cluster.getServices().values()) {
      Assert.assertEquals(State.INSTALLED, service.getDesiredState());
    }

    startService(cluster1, serviceName1, false, false, maintenanceStateHelper);
    startService(cluster1, serviceName2, false, false, maintenanceStateHelper);

    // test host
    Host h1 = clusters.getHost(host1);
    h1.setMaintenanceState(cluster.getClusterId(), MaintenanceState.ON);

    srs = new HashSet<>();
    srs.add(new ServiceRequest(cluster1, serviceName1, repositoryVersion01.getId(), State.INSTALLED.name()));
    srs.add(new ServiceRequest(cluster1, serviceName2, repositoryVersion01.getId(), State.INSTALLED.name()));

    rsr = ServiceResourceProviderTest.updateServices(controller, srs, requestProperties,
            false, false, maintenanceStateHelper);

    for (ShortTaskStatus sts : rsr.getTasks()) {
      Assert.assertFalse(sts.getHostName().equals(host1));
    }

    h1.setMaintenanceState(cluster.getClusterId(), MaintenanceState.OFF);
    startService(cluster1, serviceName2, false, false, maintenanceStateHelper);

    service2.setMaintenanceState(MaintenanceState.ON);

    ServiceRequest sr = new ServiceRequest(cluster1, serviceName2, repositoryVersion01.getId(), State.INSTALLED.name());
    rsr = ServiceResourceProviderTest.updateServices(controller,
        Collections.singleton(sr), requestProperties, false, false, maintenanceStateHelper);

    Assert.assertTrue("Service start request defaults to Cluster operation level," +
                    "command does not create tasks",
        rsr == null || rsr.getTasks().size() == 0);

  }

  @Test
  public void testIsAttributeMapsEqual() {
    AmbariManagementControllerImpl controllerImpl = null;
    if (controller instanceof AmbariManagementControllerImpl){
      controllerImpl = (AmbariManagementControllerImpl)controller;
    }
    Map<String, Map<String, String>> requestConfigAttributes = new HashMap<>();
    Map<String, Map<String, String>> clusterConfigAttributes = new HashMap<>();
    Assert.assertTrue(controllerImpl.isAttributeMapsEqual(requestConfigAttributes, clusterConfigAttributes));
    requestConfigAttributes.put("final", new HashMap<>());
    requestConfigAttributes.get("final").put("c", "true");
    clusterConfigAttributes.put("final", new HashMap<>());
    clusterConfigAttributes.get("final").put("c", "true");
    Assert.assertTrue(controllerImpl.isAttributeMapsEqual(requestConfigAttributes, clusterConfigAttributes));
    clusterConfigAttributes.put("final2", new HashMap<>());
    clusterConfigAttributes.get("final2").put("a", "true");
    Assert.assertFalse(controllerImpl.isAttributeMapsEqual(requestConfigAttributes, clusterConfigAttributes));
    requestConfigAttributes.put("final2", new HashMap<>());
    requestConfigAttributes.get("final2").put("a", "false");
    Assert.assertFalse(controllerImpl.isAttributeMapsEqual(requestConfigAttributes, clusterConfigAttributes));
  }

  @Test
  public void testEmptyConfigs() throws Exception {
    String cluster1 = getUniqueName();
    createCluster(cluster1);
    Cluster cluster =  clusters.getCluster(cluster1);
    cluster.setDesiredStackVersion(new StackId("HDP-0.1"));

    ClusterRequest cr = new ClusterRequest(cluster.getClusterId(), cluster.getClusterName(), null, null);

    // test null map with no prior
    cr.setDesiredConfig(Collections.singletonList(
        new ConfigurationRequest(cluster1, "typeA", "v1", null, null)));
    controller.updateClusters(Collections.singleton(cr), new HashMap<>());
    Config config = cluster.getDesiredConfigByType("typeA");
    Assert.assertNull(config);

    // test empty map with no prior
    cr.setDesiredConfig(Collections.singletonList(
        new ConfigurationRequest(cluster1, "typeA", "v1", new HashMap<>(), new HashMap<>())));
    controller.updateClusters(Collections.singleton(cr), new HashMap<>());
    config = cluster.getDesiredConfigByType("typeA");
    Assert.assertNotNull(config);

    // test empty properties on a new version
    cr.setDesiredConfig(Collections.singletonList(
        new ConfigurationRequest(cluster1, "typeA", "v2", new HashMap<>(), new HashMap<>())));
    controller.updateClusters(Collections.singleton(cr), new HashMap<>());
    config = cluster.getDesiredConfigByType("typeA");
    Assert.assertNotNull(config);
    Assert.assertEquals(Integer.valueOf(0), Integer.valueOf(config.getProperties().size()));

    // test new version
    Map<String, String> map = new HashMap<>();
    map.clear();
    map.put("c", "d");
    Map<String, Map<String, String>> attributesMap = new HashMap<>();
    attributesMap.put("final", new HashMap<>());
    attributesMap.get("final").put("c", "true");
    cr.setDesiredConfig(Collections.singletonList(
        new ConfigurationRequest(cluster1, "typeA", "v3", map, attributesMap)));
    controller.updateClusters(Collections.singleton(cr), new HashMap<>());
    config = cluster.getDesiredConfigByType("typeA");
    Assert.assertNotNull(config);
    Assert.assertTrue(config.getProperties().containsKey("c"));

    // test reset to v2
    cr.setDesiredConfig(Collections.singletonList(
        new ConfigurationRequest(cluster1, "typeA", "v2", new HashMap<>(), new HashMap<>())));
    controller.updateClusters(Collections.singleton(cr), new HashMap<>());
    config = cluster.getDesiredConfigByType("typeA");
    Assert.assertEquals("v2", config.getTag());
    Assert.assertNotNull(config);
    Assert.assertEquals(Integer.valueOf(0), Integer.valueOf(config.getProperties().size()));

    // test v2, but with properties
    cr.setDesiredConfig(Collections.singletonList(
        new ConfigurationRequest(cluster1, "typeA", "v2", new HashMap<String, String>() {{ put("a", "b"); }},
            new HashMap<String, Map<String,String>>(){{put("final", new HashMap<String, String>(){{put("a", "true");}});
          }
        })));
    try {
      controller.updateClusters(Collections.singleton(cr), new HashMap<>());
      Assert.fail("Expect failure when creating a config that exists");
    } catch (Exception e) {
      // expected
    }
  }

  @Test
  public void testCreateCustomActionNoCluster() throws Exception {
    String hostname1 = getUniqueName();
    String hostname2 = getUniqueName();
    addHost(hostname1);
    addHost(hostname2);

    String action1 = getUniqueName();

    ambariMetaInfo.addActionDefinition(new ActionDefinition(action1, ActionType.SYSTEM,
        "", "", "", "action def description", TargetHostType.ANY,
        Short.valueOf("60"), null));

    Map<String, String> requestProperties = new HashMap<>();
    requestProperties.put(REQUEST_CONTEXT_PROPERTY, "Called from a test");

    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("some_custom_param", "abc");

    // !!! target single host
    List<String> hosts = Arrays.asList(hostname1);
    RequestResourceFilter resourceFilter = new RequestResourceFilter(null, null, hosts);
    List<RequestResourceFilter> resourceFilters = new ArrayList<>();
    resourceFilters.add(resourceFilter);

    ExecuteActionRequest actionRequest = new ExecuteActionRequest(null, null,
        action1, resourceFilters, null, requestParams, false);
    RequestStatusResponse response = controller.createAction(actionRequest, requestProperties);
    assertEquals(1, response.getTasks().size());
    ShortTaskStatus taskStatus = response.getTasks().get(0);
    Assert.assertEquals(hostname1, taskStatus.getHostName());

    Stage stage = actionDB.getAllStages(response.getRequestId()).get(0);
    Assert.assertNotNull(stage);
    Assert.assertEquals(-1L, stage.getClusterId());

    List<HostRoleCommand> storedTasks = actionDB.getRequestTasks(response.getRequestId());
    Assert.assertEquals(1, storedTasks.size());
    HostRoleCommand task = storedTasks.get(0);
    Assert.assertEquals(RoleCommand.ACTIONEXECUTE, task.getRoleCommand());
    Assert.assertEquals(action1, task.getRole().name());
    Assert.assertEquals(hostname1, task.getHostName());

    ExecutionCommand cmd = task.getExecutionCommandWrapper().getExecutionCommand();
    Type type = new TypeToken<Map<String, String>>(){}.getType();
    Map<String, String> commandParamsStage = StageUtils.getGson().fromJson(stage.getCommandParamsStage(), type);
    Assert.assertTrue(commandParamsStage.containsKey("some_custom_param"));
    Assert.assertEquals(null, cmd.getServiceName());
    Assert.assertEquals(null, cmd.getComponentName());
    Assert.assertTrue(cmd.getLocalComponents().isEmpty());

    Assert.assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());

    // !!! target two hosts

    hosts = Arrays.asList(hostname1, hostname2);
    resourceFilter = new RequestResourceFilter(null, null, hosts);
    resourceFilters = new ArrayList<>();
    resourceFilters.add(resourceFilter);

    actionRequest = new ExecuteActionRequest(null, null,
        action1, resourceFilters, null, requestParams, false);
    response = controller.createAction(actionRequest, requestProperties);
    assertEquals(2, response.getTasks().size());
    boolean host1Found = false;
    boolean host2Found = false;
    for (ShortTaskStatus sts : response.getTasks()) {
      if (sts.getHostName().equals(hostname1)) {
        host1Found = true;
      } else if (sts.getHostName().equals(hostname2)) {
        host2Found = true;
      }
    }
    Assert.assertTrue(host1Found);
    Assert.assertTrue(host2Found);

    stage = actionDB.getAllStages(response.getRequestId()).get(0);
    Assert.assertNotNull(stage);
    Assert.assertEquals(-1L, stage.getClusterId());

    storedTasks = actionDB.getRequestTasks(response.getRequestId());
    Assert.assertEquals(2, storedTasks.size());
    task = storedTasks.get(0);
    Assert.assertEquals(RoleCommand.ACTIONEXECUTE, task.getRoleCommand());
    Assert.assertEquals(action1, task.getRole().name());
    // order is not guaranteed
    Assert.assertTrue(hostname1.equals(task.getHostName()) || hostname2.equals(task.getHostName()));

    cmd = task.getExecutionCommandWrapper().getExecutionCommand();
    commandParamsStage = StageUtils.getGson().fromJson(stage.getCommandParamsStage(), type);
    Assert.assertTrue(commandParamsStage.containsKey("some_custom_param"));
    Assert.assertEquals(null, cmd.getServiceName());
    Assert.assertEquals(null, cmd.getComponentName());
    Assert.assertTrue(cmd.getLocalComponents().isEmpty());

    Assert.assertEquals(requestProperties.get(REQUEST_CONTEXT_PROPERTY), response.getRequestContext());
  }

  @Test
  @Ignore
  //TODO Should be rewritten after stale configs calculate workflow change.
  public void testConfigAttributesStaleConfigFilter() throws Exception, AuthorizationException {

    final String host1 = getUniqueName();
    final String host2 = getUniqueName();
    String cluster1 = getUniqueName();
    Cluster c = setupClusterWithHosts(cluster1, "HDP-2.0.5", new ArrayList<String>() {
      {
        add(host1);
        add(host2);
      }
    }, "centos5");

    Long clusterId = c.getClusterId();

    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(cluster1, serviceName, componentName1, State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2, State.INIT);
    createServiceComponent(cluster1, serviceName, componentName3, State.INIT);

    createServiceComponentHost(cluster1, serviceName, componentName1, host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2, host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName3, host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2, host2, null);
    createServiceComponentHost(cluster1, serviceName, componentName3, host2, null);

    // Install
    installService(cluster1, serviceName, false, false);

    // Create and attach config
    // hdfs-site will not have config-attributes
    Map<String, String> hdfsConfigs = new HashMap<>();
    hdfsConfigs.put("a", "b");
    Map<String, Map<String, String>> hdfsConfigAttributes = new HashMap<String, Map<String, String>>() {
      {
        put("final", new HashMap<String, String>() {{put("a", "true");}});
      }
    };

    ConfigurationRequest cr1 = new ConfigurationRequest(cluster1, "hdfs-site", "version1", hdfsConfigs, hdfsConfigAttributes);
    ClusterRequest crReq1 = new ClusterRequest(clusterId, cluster1, null, null);
    crReq1.setDesiredConfig(Collections.singletonList(cr1));

    controller.updateClusters(Collections.singleton(crReq1), null);

    // Start
    startService(cluster1, serviceName, false, false);

    // Update actual config
    HashMap<String, Map<String, String>> actualConfig = new HashMap<String, Map<String, String>>() {
      {
        put("hdfs-site", new HashMap<String, String>() {{put("tag", "version1");}});
      }
    };
    HashMap<String, Map<String, String>> actualConfigOld = new HashMap<String, Map<String, String>>() {
      {
        put("hdfs-site", new HashMap<String, String>() {{put("tag", "version0");}});
      }
    };

    Service s1 = clusters.getCluster(cluster1).getService(serviceName);
    /*s1.getServiceComponent(componentName1).getServiceComponentHost(host1).updateActualConfigs(actualConfig);
    s1.getServiceComponent(componentName2).getServiceComponentHost(host1).updateActualConfigs(actualConfig);
    s1.getServiceComponent(componentName3).getServiceComponentHost(host1).updateActualConfigs(actualConfigOld);
    s1.getServiceComponent(componentName2).getServiceComponentHost(host2).updateActualConfigs(actualConfigOld);
    s1.getServiceComponent(componentName3).getServiceComponentHost(host2).updateActualConfigs(actualConfig);*/

    ServiceComponentHostRequest r = new ServiceComponentHostRequest(cluster1, null, null, null, null);
    Set<ServiceComponentHostResponse> resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(5, resps.size());

    // Get all host components with stale config = true
    r = new ServiceComponentHostRequest(cluster1, null, null, null, null);
    r.setStaleConfig("true");
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(2, resps.size());

    // Get all host components with stale config = false
    r = new ServiceComponentHostRequest(cluster1, null, null, null, null);
    r.setStaleConfig("false");
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(3, resps.size());

    // Get all host components with stale config = false and hostname filter
    r = new ServiceComponentHostRequest(cluster1, null, null, host1, null);
    r.setStaleConfig("false");
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(2, resps.size());

    // Get all host components with stale config = false and hostname filter
    r = new ServiceComponentHostRequest(cluster1, null, null, host2, null);
    r.setStaleConfig("true");
    resps = controller.getHostComponents(Collections.singleton(r));
    Assert.assertEquals(1, resps.size());
  }

  @Test
  public void testSecretReferences() throws Exception, AuthorizationException {

    final String host1 = getUniqueName();
    final String host2 = getUniqueName();
    String cluster1 = getUniqueName();

    Cluster cl = setupClusterWithHosts(cluster1, "HDP-2.0.5", new ArrayList<String>() {
      {
        add(host1);
        add(host2);
      }
    }, "centos5");

    Long clusterId = cl.getClusterId();

    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    String componentName3 = "HDFS_CLIENT";

    createServiceComponent(cluster1, serviceName, componentName1, State.INIT);
    createServiceComponent(cluster1, serviceName, componentName2, State.INIT);
    createServiceComponent(cluster1, serviceName, componentName3, State.INIT);

    createServiceComponentHost(cluster1, serviceName, componentName1, host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2, host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName3, host1, null);
    createServiceComponentHost(cluster1, serviceName, componentName2, host2, null);
    createServiceComponentHost(cluster1, serviceName, componentName3, host2, null);

    // Install
    installService(cluster1, serviceName, false, false);

    ClusterRequest crReq;
    ConfigurationRequest cr;

    cr = new ConfigurationRequest(cluster1,
        "hdfs-site",
        "version1",
        new HashMap<String, String>(){{
          put("test.password", "first");
          put("test.password.empty", "");
        }},
      new HashMap<>()
    );
    crReq = new ClusterRequest(clusterId, cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr));

    controller.updateClusters(Collections.singleton(crReq), null);
    // update config with secret reference
    cr = new ConfigurationRequest(cluster1,
        "hdfs-site",
        "version2",
        new HashMap<String, String>(){{
          put("test.password", "SECRET:hdfs-site:1:test.password");
          put("new", "new");//need this to mark config as "changed"
        }},
      new HashMap<>()
    );
    crReq = new ClusterRequest(clusterId, cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr));
    controller.updateClusters(Collections.singleton(crReq), null);
    // change password to new value
    cr = new ConfigurationRequest(cluster1,
        "hdfs-site",
        "version3",
        new HashMap<String, String>(){{
          put("test.password", "brandNewPassword");
        }},
      new HashMap<>()
    );
    crReq = new ClusterRequest(clusterId, cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr));
    controller.updateClusters(Collections.singleton(crReq), null);
    // wrong secret reference
    cr = new ConfigurationRequest(cluster1,
        "hdfs-site",
        "version3",
        new HashMap<String, String>(){{
          put("test.password", "SECRET:hdfs-site:666:test.password");
        }},
      new HashMap<>()
    );
    crReq = new ClusterRequest(clusterId, cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr));
    try {
      controller.updateClusters(Collections.singleton(crReq), null);
      fail("Request need to be failed with wrong secret reference");
    } catch (Exception e){

    }
    // reference to config which does not contain requested property
    cr = new ConfigurationRequest(cluster1,
        "hdfs-site",
        "version4",
        new HashMap<String, String>(){{
          put("foo", "bar");
        }},
      new HashMap<>()
    );
    crReq = new ClusterRequest(clusterId, cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr));
    controller.updateClusters(Collections.singleton(crReq), null);
    cr = new ConfigurationRequest(cluster1,
        "hdfs-site",
        "version5",
        new HashMap<String, String>(){{
          put("test.password", "SECRET:hdfs-site:4:test.password");
          put("new", "new");
        }},
      new HashMap<>()
    );
    crReq = new ClusterRequest(clusterId, cluster1, null, null);
    crReq.setDesiredConfig(Collections.singletonList(cr));
    try {
      controller.updateClusters(Collections.singleton(crReq), null);
      fail("Request need to be failed with wrong secret reference");
    } catch (Exception e) {
      assertEquals("Error when parsing secret reference. Cluster: " + cluster1 + " ConfigType: hdfs-site ConfigVersion: 4 does not contain property 'test.password'",
          e.getMessage());
    }
    cl.getAllConfigs();
    assertEquals(cl.getAllConfigs().size(), 4);

    Config v1 = cl.getConfigByVersion("hdfs-site", 1l);
    Config v2 = cl.getConfigByVersion("hdfs-site", 2l);
    Config v3 = cl.getConfigByVersion("hdfs-site", 3l);
    Config v4 = cl.getConfigByVersion("hdfs-site", 4l);

    assertEquals(v1.getProperties().get("test.password"), "first");
    assertEquals(v2.getProperties().get("test.password"), "first");
    assertEquals(v3.getProperties().get("test.password"), "brandNewPassword");
    assertFalse(v4.getProperties().containsKey("test.password"));

    // check if we have masked secret in responce
    final ConfigurationRequest configRequest = new ConfigurationRequest(cluster1, "hdfs-site", null, null, null);
    configRequest.setIncludeProperties(true);
    Set<ConfigurationResponse> requestedConfigs = controller.getConfigurations(new HashSet<ConfigurationRequest>() {{
      add(configRequest);
    }});
    for(ConfigurationResponse resp : requestedConfigs) {
      String secretName = "SECRET:hdfs-site:"+ resp.getVersion() +":test.password";
      if(resp.getConfigs().containsKey("test.password")) {
        assertEquals(resp.getConfigs().get("test.password"), secretName);
      }
      if(resp.getConfigs().containsKey("test.password.empty")) {
        assertEquals(resp.getConfigs().get("test.password.empty"), "");
      }
    }
  }

  @Test
  public void testTargetedProcessCommand() throws Exception {
    final String host1 = getUniqueName();
    String cluster1 = getUniqueName();

    Cluster cluster = setupClusterWithHosts(cluster1, "HDP-2.0.5", Arrays.asList(host1), "centos5");
    String serviceName = "HDFS";
    createService(cluster1, serviceName, null);
    String componentName1 = "NAMENODE";

    createServiceComponent(cluster1, serviceName, componentName1, State.INIT);

    createServiceComponentHost(cluster1, serviceName, componentName1, host1, null);

    // Install
    installService(cluster1, serviceName, false, false);

    // Create and attach config
    // hdfs-site will not have config-attributes
    Map<String, String> hdfsConfigs = new HashMap<>();
    hdfsConfigs.put("a", "b");
    Map<String, Map<String, String>> hdfsConfigAttributes = new HashMap<String, Map<String, String>>() {
      {
        put("final", new HashMap<String, String>() {{put("a", "true");}});
      }
    };

    ConfigurationRequest cr1 = new ConfigurationRequest(cluster1, "hdfs-site", "version1", hdfsConfigs, hdfsConfigAttributes);
    ClusterRequest crReq1 = new ClusterRequest(cluster.getClusterId(), cluster1, null, null);
    crReq1.setDesiredConfig(Collections.singletonList(cr1));

    controller.updateClusters(Collections.singleton(crReq1), null);

    // Start
    startService(cluster1, serviceName, false, false);

    ServiceComponentHostRequest req = new ServiceComponentHostRequest(cluster1, serviceName,
        componentName1, host1, "INSTALLED");

    Map<String, String> requestProperties = new HashMap<>();
    requestProperties.put("namenode", "p1");
    RequestStatusResponse resp = updateHostComponents(Collections.singleton(req), requestProperties, false);

    // succeed in creating a task
    assertNotNull(resp);

    // manually change live state to stopped as no running action manager
    for (ServiceComponentHost sch :
      clusters.getCluster(cluster1).getServiceComponentHosts(host1)) {
        sch.setState(State.INSTALLED);
    }

    // no new commands since no targeted info
    resp = updateHostComponents(Collections.singleton(req), new HashMap<>(), false);
    assertNull(resp);

    // role commands added for targeted command
    resp = updateHostComponents(Collections.singleton(req), requestProperties, false);
    assertNotNull(resp);

  }

  @Test
  public void testGetPackagesForServiceHost() throws Exception {
    ServiceInfo service = ambariMetaInfo.getStack("HDP", "2.0.1").getService("HIVE");
    HashMap<String, String> hostParams = new HashMap<>();

    Map<String, ServiceOsSpecific.Package> packages = new HashMap<>();
    String [] packageNames = {"hive", "mysql-connector-java", "mysql", "mysql-server", "mysql-client"};
    for (String packageName : packageNames) {
      ServiceOsSpecific.Package pkg = new ServiceOsSpecific.Package();
      pkg.setName(packageName);
      packages.put(packageName, pkg);
    }

    List<ServiceOsSpecific.Package> rhel5Packages = controller.getPackagesForServiceHost(service, hostParams, "redhat5");
    List<ServiceOsSpecific.Package> expectedRhel5 = Arrays.asList(
            packages.get("hive"),
            packages.get("mysql-connector-java"),
            packages.get("mysql"),
            packages.get("mysql-server")
    );

    List<ServiceOsSpecific.Package> sles11Packages = controller.getPackagesForServiceHost(service, hostParams, "suse11");
    List<ServiceOsSpecific.Package> expectedSles11 = Arrays.asList(
            packages.get("hive"),
            packages.get("mysql-connector-java"),
            packages.get("mysql"),
            packages.get("mysql-client")
    );
    assertThat(rhel5Packages, is(expectedRhel5));
    assertThat(sles11Packages, is(expectedSles11));
  }

  @Test
  public void testServiceWidgetCreationOnServiceCreate() throws Exception {
    String cluster1 = getUniqueName();
    ClusterRequest r = new ClusterRequest(null, cluster1,
      State.INSTALLED.name(), SecurityType.NONE, "OTHER-2.0", null);
    controller.createCluster(r);
    String serviceName = "HBASE";
    clusters.getCluster(cluster1).setDesiredStackVersion(new StackId("OTHER-2.0"));

    RepositoryVersionEntity repositoryVersion = helper.getOrCreateRepositoryVersion(
        new StackId("OTHER-2.0"), "2.0-1234");

    createService(cluster1, serviceName, repositoryVersion, State.INIT);

    Service s = clusters.getCluster(cluster1).getService(serviceName);
    Assert.assertNotNull(s);
    Assert.assertEquals(serviceName, s.getName());
    Assert.assertEquals(cluster1, s.getCluster().getClusterName());

    WidgetDAO widgetDAO = injector.getInstance(WidgetDAO.class);
    WidgetLayoutDAO widgetLayoutDAO = injector.getInstance(WidgetLayoutDAO.class);
    List<WidgetEntity> widgetEntities = widgetDAO.findAll();
    List<WidgetLayoutEntity> layoutEntities = widgetLayoutDAO.findAll();

    Assert.assertNotNull(widgetEntities);
    Assert.assertFalse(widgetEntities.isEmpty());
    Assert.assertNotNull(layoutEntities);
    Assert.assertFalse(layoutEntities.isEmpty());

    WidgetEntity candidateVisibleEntity = null;
    for (WidgetEntity entity : widgetEntities) {
      if (entity.getWidgetName().equals("OPEN_CONNECTIONS")) {
        candidateVisibleEntity = entity;
      }
    }
    Assert.assertNotNull(candidateVisibleEntity);
    Assert.assertEquals("GRAPH", candidateVisibleEntity.getWidgetType());
    Assert.assertEquals("ambari", candidateVisibleEntity.getAuthor());
    Assert.assertEquals("CLUSTER", candidateVisibleEntity.getScope());
    Assert.assertNotNull(candidateVisibleEntity.getMetrics());
    Assert.assertNotNull(candidateVisibleEntity.getProperties());
    Assert.assertNotNull(candidateVisibleEntity.getWidgetValues());

    WidgetLayoutEntity candidateLayoutEntity = null;
    for (WidgetLayoutEntity entity : layoutEntities) {
      if (entity.getLayoutName().equals("default_hbase_layout")) {
        candidateLayoutEntity = entity;
      }
    }
    Assert.assertNotNull(candidateLayoutEntity);
    List<WidgetLayoutUserWidgetEntity> layoutUserWidgetEntities =
      candidateLayoutEntity.getListWidgetLayoutUserWidgetEntity();
    Assert.assertNotNull(layoutUserWidgetEntities);
    Assert.assertEquals(4, layoutUserWidgetEntities.size());
    Assert.assertEquals("RS_READS_WRITES", layoutUserWidgetEntities.get(0).getWidget().getWidgetName());
    Assert.assertEquals("OPEN_CONNECTIONS", layoutUserWidgetEntities.get(1).getWidget().getWidgetName());
    Assert.assertEquals("FILES_LOCAL", layoutUserWidgetEntities.get(2).getWidget().getWidgetName());
    Assert.assertEquals("UPDATED_BLOCKED_TIME", layoutUserWidgetEntities.get(3).getWidget().getWidgetName());
    Assert.assertEquals("HBASE_SUMMARY", layoutUserWidgetEntities.get(0).getWidget().getDefaultSectionName());

    File widgetsFile  = ambariMetaInfo.getCommonWidgetsDescriptorFile();
    assertNotNull(widgetsFile);
    assertEquals("src/test/resources/widgets.json", widgetsFile.getPath());
    assertTrue(widgetsFile.exists());

    candidateLayoutEntity = null;
    for (WidgetLayoutEntity entity : layoutEntities) {
      if (entity.getLayoutName().equals("default_system_heatmap")) {
        candidateLayoutEntity = entity;
        break;
      }
    }
    Assert.assertNotNull(candidateLayoutEntity);
    Assert.assertEquals("ambari", candidateVisibleEntity.getAuthor());
    Assert.assertEquals("CLUSTER", candidateVisibleEntity.getScope());
  }

  // this is a temporary measure as a result of moving updateHostComponents from AmbariManagementController
  // to HostComponentResourceProvider.  Eventually the tests should be moved out of this class.
  private RequestStatusResponse updateHostComponents(Set<ServiceComponentHostRequest> requests,
                                                     Map<String, String> requestProperties,
                                                     boolean runSmokeTest) throws Exception {

    return updateHostComponents(controller, requests, requestProperties, runSmokeTest);
  }

  private RequestStatusResponse updateHostComponents(AmbariManagementController controller,
                                                     Set<ServiceComponentHostRequest> requests,
                                                     Map<String, String> requestProperties,
                                                     boolean runSmokeTest) throws Exception {

    return HostComponentResourceProviderTest.updateHostComponents(
        controller, injector, requests, requestProperties, runSmokeTest);
  }
}


