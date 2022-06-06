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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Request;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.agent.CommandRepository;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.ComponentResourceProviderTest;
import org.apache.ambari.server.controller.internal.RequestOperationLevel;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.controller.internal.ServiceResourceProviderTest;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.events.AgentConfigsUpdateEvent;
import org.apache.ambari.server.metadata.ActionMetadata;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.RepoDefinitionEntity;
import org.apache.ambari.server.orm.entities.RepoOsEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.stack.upgrade.RepositoryVersionHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.UserGroupInfo;
import org.apache.ambari.server.state.ValueAttributesInfo;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.collections.MapUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;


public class AmbariCustomCommandExecutionHelperTest {
  private static final String REQUEST_CONTEXT_PROPERTY = "context";

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock(type = MockType.NICE)
  private ActionManager actionManager;

  @Mock(type = MockType.NICE)
  private HostRoleCommand hostRoleCommand;

  @Mock(type = MockType.NICE)
  private ConfigHelper configHelper;

  private Injector injector;
  private Clusters clusters;
  private AmbariManagementController ambariManagementController;
  private Capture<Request> requestCapture = EasyMock.newCapture();
  private static final String OVERRIDDEN_SERVICE_CHECK_TIMEOUT_VALUE = "550";


  @Before
  public void setup() throws Exception {
    EasyMock.reset(actionManager, hostRoleCommand, configHelper);

    InMemoryDefaultTestModule module = new InMemoryDefaultTestModule(){
      @Override
      protected void configure() {
        getProperties().setProperty(Configuration.AGENT_SERVICE_CHECK_TASK_TIMEOUT.getKey(),
          OVERRIDDEN_SERVICE_CHECK_TIMEOUT_VALUE);
        super.configure();
        bind(ActionManager.class).toInstance(actionManager);
        bind(ConfigHelper.class).toInstance(configHelper);
      }
    };

    injector = Guice.createInjector(module);
    injector.getInstance(GuiceJpaInitializer.class);
    ambariManagementController = injector.getInstance(AmbariManagementController.class);
    clusters = injector.getInstance(Clusters.class);

    EasyMock.expect(configHelper.getPropertyValuesWithPropertyType(EasyMock.anyObject(StackId.class),
        EasyMock.anyObject(PropertyInfo.PropertyType.class),
        EasyMock.anyObject(Cluster.class),
        EasyMock.anyObject(Map.class))).andReturn(Collections.EMPTY_SET);

    EasyMock.expect(configHelper.getHostActualConfigs(EasyMock.anyLong())).andReturn(
        new AgentConfigsUpdateEvent(null, new TreeMap<>())).anyTimes();

    EasyMock.replay(configHelper);

    StageUtils.setTopologyManager(injector.getInstance(TopologyManager.class));
    StageUtils.setConfiguration(injector.getInstance(Configuration.class));

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());
    createClusterFixture("c1", new StackId("HDP-2.0.6"), "2.0.6-1234", "c1");

    EasyMock.verify(configHelper);
    EasyMock.reset(configHelper);

    EasyMock.expect(hostRoleCommand.getTaskId()).andReturn(1L);
    EasyMock.expect(hostRoleCommand.getStageId()).andReturn(1L);
    EasyMock.expect(hostRoleCommand.getRoleCommand()).andReturn(RoleCommand.CUSTOM_COMMAND);
    EasyMock.expect(hostRoleCommand.getRole()).andReturn(Role.AMBARI_SERVER_ACTION);
    EasyMock.expect(hostRoleCommand.getStatus()).andReturn(HostRoleStatus.PENDING);

    EasyMock.expect(actionManager.getNextRequestId()).andReturn(1L).anyTimes();
    EasyMock.expect(actionManager.getRequestTasks(1L)).andReturn(Collections.singletonList(hostRoleCommand));

    StackInfo stackInfo = new StackInfo();
    stackInfo.setName("HDP");
    stackInfo.setVersion("2.0.6");
    StackId stackId = new StackId(stackInfo);
    Map<String, DesiredConfig> desiredConfigMap = new HashMap<>();
    Map<PropertyInfo, String> userProperties = new HashMap<>();
    Map<PropertyInfo, String> groupProperties = new HashMap<>();
    PropertyInfo userProperty = new PropertyInfo();
    userProperty.setFilename("zookeeper-env.xml");
    userProperty.setName("zookeeper-user");
    userProperty.setValue("zookeeperUser");
    PropertyInfo groupProperty = new PropertyInfo();
    groupProperty.setFilename("zookeeper-env.xml");
    groupProperty.setName("zookeeper-group");
    groupProperty.setValue("zookeeperGroup");
    ValueAttributesInfo valueAttributesInfo = new ValueAttributesInfo();
    valueAttributesInfo.setType("user");
    Set<UserGroupInfo> userGroupEntries = new HashSet<>();
    UserGroupInfo userGroupInfo = new UserGroupInfo();
    userGroupInfo.setType("zookeeper-env");
    userGroupInfo.setName("zookeeper-group");
    userGroupEntries.add(userGroupInfo);
    valueAttributesInfo.setUserGroupEntries(userGroupEntries);
    userProperty.setPropertyValueAttributes(valueAttributesInfo);
    userProperties.put(userProperty, "zookeeperUser");
    groupProperties.put(groupProperty, "zookeeperGroup");
    Map<String, Set<String>> userGroupsMap = new HashMap<>();
    userGroupsMap.put("zookeeperUser", new HashSet<>(Arrays.asList("zookeeperGroup")));
    Cluster cluster = clusters.getCluster("c1");
    EasyMock.expect(configHelper.getPropertiesWithPropertyType(
      stackId, PropertyInfo.PropertyType.USER, cluster, desiredConfigMap)).andReturn(userProperties).anyTimes();
    EasyMock.expect(configHelper.getPropertiesWithPropertyType(
      stackId, PropertyInfo.PropertyType.GROUP, cluster, desiredConfigMap)).andReturn(groupProperties).anyTimes();
    EasyMock.expect(configHelper.createUserGroupsMap(stackId, cluster, desiredConfigMap)).andReturn(userGroupsMap).anyTimes();

    actionManager.sendActions(EasyMock.capture(requestCapture), EasyMock.anyObject(ExecuteActionRequest.class));
    EasyMock.expectLastCall();

  }

  @After
  public void teardown() throws AmbariException, SQLException {
    SecurityContextHolder.getContext().setAuthentication(null);
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  public void testRefreshQueueCustomCommand() throws Exception {
    Map<String, String> requestProperties = new HashMap<String, String>() {
      {
        put(REQUEST_CONTEXT_PROPERTY, "Refresh YARN Capacity Scheduler");
        put("command", "REFRESHQUEUES");
      }
    };

    ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1", "REFRESHQUEUES",
        new HashMap<String, String>() {
          {
            put("forceRefreshConfigTagsBeforeExecution", "true");
          }
        }, false);
    actionRequest.getResourceFilters().add(new RequestResourceFilter("YARN", "RESOURCEMANAGER", Collections.singletonList("c1-c6401")));

    EasyMock.replay(hostRoleCommand, actionManager, configHelper);

    ambariManagementController.createAction(actionRequest, requestProperties);

    Request request = requestCapture.getValue();
    Assert.assertNotNull(request);
    Assert.assertNotNull(request.getStages());
    Assert.assertEquals(1, request.getStages().size());
    Stage stage = request.getStages().iterator().next();

    Assert.assertEquals(1, stage.getHosts().size());

    List<ExecutionCommandWrapper> commands = stage.getExecutionCommands("c1-c6401");
    Assert.assertEquals(1, commands.size());

    ExecutionCommand command = commands.get(0).getExecutionCommand();
    Assert.assertNotNull(command.getHostLevelParams());
    Assert.assertTrue(command.getHostLevelParams().containsKey(ExecutionCommand.KeyNames.USER_GROUPS));
    Assert.assertEquals("{\"zookeeperUser\":[\"zookeeperGroup\"]}", command.getHostLevelParams().get(ExecutionCommand.KeyNames.USER_GROUPS));
    Assert.assertEquals(false, command.isOverrideConfigs());
    Assert.assertNull(command.getRepositoryFile());
  }

  @Test
  public void testHostsFilterHealthy() throws Exception {

    Map<String, String> requestProperties = new HashMap<String, String>() {
      {
        put("context" , "Restart all components for GANGLIA");
        put("operation_level/level", "SERVICE");
        put("operation_level/service_name", "GANGLIA");
        put("operation_level/cluster_name", "c1");
      }
    };

    ExecuteActionRequest actionRequest = new ExecuteActionRequest(
       "c1", "RESTART", null,
       Arrays.asList(
           new RequestResourceFilter("GANGLIA", "GANGLIA_SERVER", Collections.singletonList("c1-c6401")),
           new RequestResourceFilter("GANGLIA", "GANGLIA_MONITOR", Collections.singletonList("c1-c6401")),
           new RequestResourceFilter("GANGLIA", "GANGLIA_MONITOR", Collections.singletonList("c1-c6402"))
       ),
       new RequestOperationLevel(Resource.Type.Service, "c1", "GANGLIA", null, null),
      new HashMap<>(), false);

    EasyMock.replay(hostRoleCommand, actionManager, configHelper);

    ambariManagementController.createAction(actionRequest, requestProperties);

    Request request = requestCapture.getValue();
    Assert.assertNotNull(request);
    Assert.assertNotNull(request.getStages());
    Assert.assertEquals(1, request.getStages().size());
    Stage stage = request.getStages().iterator().next();

     // Check if was generated command, one for each host
    Assert.assertEquals(2, stage.getHostRoleCommands().size());
  }

  @Test
  public void testHostsFilterUnhealthyHost() throws Exception {
    // Set custom status to host
    clusters.getHost("c1-c6402").setState(HostState.HEARTBEAT_LOST);
    Map<String, String> requestProperties = new HashMap<String, String>() {
      {
        put("context", "Restart all components for GANGLIA");
        put("operation_level/level", "SERVICE");
        put("operation_level/service_name", "GANGLIA");
        put("operation_level/cluster_name", "c1");
      }
    };

    ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1", "RESTART", null,
        Arrays.asList(
            new RequestResourceFilter("GANGLIA", "GANGLIA_SERVER", Collections.singletonList("c1-c6401")),
            new RequestResourceFilter("GANGLIA", "GANGLIA_MONITOR", Collections.singletonList("c1-c6401")),
            new RequestResourceFilter("GANGLIA", "GANGLIA_MONITOR", Collections.singletonList("c1-c6402"))),
        new RequestOperationLevel(Resource.Type.Service, "c1", "GANGLIA", null, null),
      new HashMap<>(), false);

    EasyMock.replay(hostRoleCommand, actionManager, configHelper);

    ambariManagementController.createAction(actionRequest, requestProperties);

    Request request = requestCapture.getValue();
    Assert.assertNotNull(request);
    Assert.assertNotNull(request.getStages());
    Assert.assertEquals(1, request.getStages().size());
    Stage stage = request.getStages().iterator().next();

    // Check if was generated command for one health host
    Assert.assertEquals(1, stage.getHostRoleCommands().size());
  }

  @Test
  public void testHostsFilterUnhealthyComponent() throws Exception {
    // Set custom status to host
    clusters.getCluster("c1").getService("GANGLIA").getServiceComponent(
        "GANGLIA_MONITOR").getServiceComponentHost("c1-c6402").setState(State.UNKNOWN);

    Map<String, String> requestProperties = new HashMap<String, String>() {
      {
        put("context", "Restart all components for GANGLIA");
        put("operation_level/level", "SERVICE");
        put("operation_level/service_name", "GANGLIA");
        put("operation_level/cluster_name", "c1");
      }
    };

    ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1", "RESTART", null,
        Arrays.asList(
            new RequestResourceFilter("GANGLIA", "GANGLIA_SERVER", Collections.singletonList("c1-c6401")),
            new RequestResourceFilter("GANGLIA", "GANGLIA_MONITOR", Collections.singletonList("c1-c6401")),
            new RequestResourceFilter("GANGLIA", "GANGLIA_MONITOR", Collections.singletonList("c1-c6402"))),
        new RequestOperationLevel(Resource.Type.Host, "c1", "GANGLIA", null, null),
      new HashMap<>(), false);

    EasyMock.replay(hostRoleCommand, actionManager, configHelper);

    ambariManagementController.createAction(actionRequest, requestProperties);

    Request request = requestCapture.getValue();
    Assert.assertNotNull(request);
    Assert.assertNotNull(request.getStages());
    Assert.assertEquals(1, request.getStages().size());
    Stage stage = request.getStages().iterator().next();

    // Check if was generated command for one health host
    Assert.assertEquals(1, stage.getHostRoleCommands().size());
  }

  /**
   * Tests that trying to run a service check when there are no available hosts
   * will throw an exception.
   */
  @Test(expected = AmbariException.class)
  public void testNoCandidateHostThrowsException() throws Exception {
    long clusterId = clusters.getCluster("c1").getClusterId();

    // put host into MM
    clusters.getHost("c6402").setMaintenanceState(clusterId, MaintenanceState.ON);

    // ensure that service check is added for ZOOKEEPER
    injector.getInstance(ActionMetadata.class).addServiceCheckAction("ZOOKEEPER");

    Map<String, String> requestProperties = new HashMap<String, String>() {
      {
        put("context", "Service Check ZooKeeper");
        put("operation_level/level", "SERVICE");
        put("operation_level/service_name", "ZOOKEEPER");
        put("operation_level/cluster_name", "c1");
      }
    };

    // create the service check on the host in MM
    ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1",
        "ZOOKEEPER_QUORUM_SERVICE_CHECK",
        null, Collections.singletonList(new RequestResourceFilter("ZOOKEEPER", "ZOOKEEPER_CLIENT",
        Collections.singletonList("c6402"))),

        new RequestOperationLevel(Resource.Type.Service, "c1", "ZOOKEEPER", null, null),
      new HashMap<>(), false);

    EasyMock.replay(hostRoleCommand, actionManager, configHelper);
    ambariManagementController.createAction(actionRequest, requestProperties);
    Assert.fail(
        "Expected an exception since there are no hosts which can run the ZK service check");
  }

  /**
   * Tests that client-only services like TEZ are not run on hosts which are in
   * MM. The client-only service is a special path since a component is
   * typically not specified in the request.
   */
  @Test(expected = AmbariException.class)
  public void testServiceCheckMaintenanceModeWithMissingComponentName() throws Exception {
    long clusterId = clusters.getCluster("c1").getClusterId();

    // put host into MM
    clusters.getHost("c6402").setMaintenanceState(clusterId, MaintenanceState.ON);

    // ensure that service check is added for ZOOKEEPER
    injector.getInstance(ActionMetadata.class).addServiceCheckAction("ZOOKEEPER");

    // !!! use a null operation level to have us guess at the component
    Map<String, String> requestProperties = new HashMap<String, String>() {
      {
        put("context", "Service Check ZooKeeper");
        put("operation_level/level", null);
        put("operation_level/service_name", "ZOOKEEPER");
        put("operation_level/cluster_name", "c1");
      }
    };

    // create the service check on the host in MM, passing in null for the
    // component name
    ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1",
        "ZOOKEEPER_QUORUM_SERVICE_CHECK", null, Collections.singletonList(new RequestResourceFilter("ZOOKEEPER", null,
        Collections.singletonList("c6402"))),

        new RequestOperationLevel(Resource.Type.Service, "c1", "ZOOKEEPER", null, null),
      new HashMap<>(), false);

    EasyMock.replay(hostRoleCommand, actionManager, configHelper);
    ambariManagementController.createAction(actionRequest, requestProperties);
    Assert.fail("Expected an exception since there are no hosts which can run the ZK service check");
  }

  @Test(expected = AmbariException.class)
  public void testServiceCheckComponentWithEmptyHosts() throws Exception {

    AmbariCustomCommandExecutionHelper ambariCustomCommandExecutionHelper = injector.getInstance(AmbariCustomCommandExecutionHelper.class);

    List<RequestResourceFilter> requestResourceFilter = new ArrayList<RequestResourceFilter>() {{
      add(new RequestResourceFilter("FLUME", null, null));
    }};
    ActionExecutionContext actionExecutionContext = new ActionExecutionContext("c1", "SERVICE_CHECK", requestResourceFilter);
    Stage stage = EasyMock.niceMock(Stage.class);

    ambariCustomCommandExecutionHelper.addExecutionCommandsToStage(actionExecutionContext, stage, new HashMap<>(), null);

    Assert.fail("Expected an exception since there are no hosts which can run the Flume service check");
  }

  @Test
  public void testServiceCheckWithOverriddenTimeout() throws Exception {
    AmbariCustomCommandExecutionHelper ambariCustomCommandExecutionHelper = injector.getInstance(AmbariCustomCommandExecutionHelper.class);

    Cluster c1 = clusters.getCluster("c1");
    Service s = c1.getService("ZOOKEEPER");
    ServiceComponent sc = s.getServiceComponent("ZOOKEEPER_CLIENT");
    Assert.assertTrue(sc.getServiceComponentHosts().keySet().size() > 1);

    // There are multiple hosts with ZK Client.
    List<RequestResourceFilter> requestResourceFilter = new ArrayList<RequestResourceFilter>() {{
      add(new RequestResourceFilter("ZOOKEEPER", "ZOOKEEPER_CLIENT", Arrays.asList(new String[]{"c1-c6401"})));
    }};
    ActionExecutionContext actionExecutionContext = new ActionExecutionContext("c1", "SERVICE_CHECK", requestResourceFilter);
    Stage stage = EasyMock.niceMock(Stage.class);
    ExecutionCommandWrapper execCmdWrapper = EasyMock.niceMock(ExecutionCommandWrapper.class);
    ExecutionCommand execCmd = EasyMock.niceMock(ExecutionCommand.class);
    Capture<Map<String,String>> timeOutCapture = EasyMock.newCapture();

    EasyMock.expect(stage.getClusterName()).andReturn("c1");

    EasyMock.expect(stage.getExecutionCommandWrapper(EasyMock.eq("c1-c6401"), EasyMock.anyString())).andReturn(execCmdWrapper);
    EasyMock.expect(execCmdWrapper.getExecutionCommand()).andReturn(execCmd);
    execCmd.setCommandParams(EasyMock.capture(timeOutCapture));
    EasyMock.expectLastCall();

    HashSet<String> localComponents = new HashSet<>();
    EasyMock.expect(execCmd.getLocalComponents()).andReturn(localComponents).anyTimes();
    EasyMock.replay(configHelper, stage, execCmdWrapper, execCmd);

    ambariCustomCommandExecutionHelper.addExecutionCommandsToStage(actionExecutionContext, stage, new HashMap<>(), null);
    Map<String, String> configMap = timeOutCapture.getValues().get(0);
    for (Map.Entry<String, String> config : configMap.entrySet()) {
      if (config.getKey().equals(ExecutionCommand.KeyNames.COMMAND_TIMEOUT)) {
        Assert.assertEquals("Service check timeout should be equal to populated in configs",
          OVERRIDDEN_SERVICE_CHECK_TIMEOUT_VALUE,
          config.getValue());
        return;
      }
    }
    Assert.fail("Expected \"command_timeout\" config not found in execution command configs");
  }

  /**
   * Perform a Service Check for ZOOKEEPER/ZOOKEEPER_CLIENT without specifying a host to run in the request.
   * This should cause Ambari to randomly pick one of the ZOOKEEPER_CLIENT hosts.
   * The current logic first excludes hosts in maintenance mode or that are not healthy (i.e., not heartbeating).
   * From that candidate list, if any hosts have 0 IN-PROGRESS tasks, it randomly picks from that set.
   * Otherwise, it picks from all candidate hosts.
   * @throws Exception
   */
  @Test
  public void testServiceCheckPicksRandomHost() throws Exception {
    AmbariCustomCommandExecutionHelper ambariCustomCommandExecutionHelper = injector.getInstance(AmbariCustomCommandExecutionHelper.class);

    Cluster c1 = clusters.getCluster("c1");
    Service s = c1.getService("ZOOKEEPER");
    ServiceComponent sc = s.getServiceComponent("ZOOKEEPER_CLIENT");
    Assert.assertTrue(sc.getServiceComponentHosts().keySet().size() > 1);

    // There are multiple hosts with ZK Client.
    List<RequestResourceFilter> requestResourceFilter = new ArrayList<RequestResourceFilter>() {{
      add(new RequestResourceFilter("ZOOKEEPER", "ZOOKEEPER_CLIENT", null));
    }};
    ActionExecutionContext actionExecutionContext = new ActionExecutionContext("c1", "SERVICE_CHECK", requestResourceFilter);
    Stage stage = EasyMock.niceMock(Stage.class);
    ExecutionCommandWrapper execCmdWrapper = EasyMock.niceMock(ExecutionCommandWrapper.class);
    ExecutionCommand execCmd = EasyMock.niceMock(ExecutionCommand.class);

    EasyMock.expect(stage.getClusterName()).andReturn("c1");
    //
    EasyMock.expect(stage.getExecutionCommandWrapper(EasyMock.eq("c1-c6401"), EasyMock.anyString())).andReturn(execCmdWrapper);
    EasyMock.expect(stage.getExecutionCommandWrapper(EasyMock.eq("c1-c6402"), EasyMock.anyString())).andReturn(execCmdWrapper);
    EasyMock.expect(execCmdWrapper.getExecutionCommand()).andReturn(execCmd);

    HashSet<String> localComponents = new HashSet<>();
    EasyMock.expect(execCmd.getLocalComponents()).andReturn(localComponents).anyTimes();
    EasyMock.replay(configHelper, stage, execCmdWrapper, execCmd);

    ambariCustomCommandExecutionHelper.addExecutionCommandsToStage(actionExecutionContext, stage, new HashMap<>(), null);
  }

  @Test
  public void testIsTopologyRefreshRequired() throws Exception {
    AmbariCustomCommandExecutionHelper helper = injector.getInstance(AmbariCustomCommandExecutionHelper.class);

    EasyMock.expect(configHelper.getHostActualConfigs(EasyMock.anyLong())).andReturn(
        new AgentConfigsUpdateEvent(null, new TreeMap<>())).anyTimes();

    EasyMock.replay(configHelper);

    createClusterFixture("c2", new StackId("HDP-2.1.1"), "2.1.1.0-1234", "c2");

    EasyMock.verify(configHelper);

    Assert.assertTrue(helper.isTopologyRefreshRequired("START", "c2", "HDFS"));
    Assert.assertTrue(helper.isTopologyRefreshRequired("RESTART", "c2", "HDFS"));
    Assert.assertFalse(helper.isTopologyRefreshRequired("STOP", "c2", "HDFS"));
  }

  @Test
  public void testAvailableServicesMapContainsVersions() throws Exception {

    Map<String, String> requestProperties = new HashMap<String, String>() {
      {
        put(REQUEST_CONTEXT_PROPERTY, "Refresh YARN Capacity Scheduler");
        put("command", "REFRESHQUEUES");
      }
    };
    ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1", "REFRESHQUEUES",
            new HashMap<String, String>() {
              {
                put("forceRefreshConfigTags", "capacity-scheduler");
              }
            }, false);
    actionRequest.getResourceFilters().add(new RequestResourceFilter("YARN", "RESOURCEMANAGER", Collections.singletonList("c1-c6401")));
    EasyMock.replay(hostRoleCommand, actionManager, configHelper);

    ambariManagementController.createAction(actionRequest, requestProperties);

    Request request = requestCapture.getValue();
    Stage stage = request.getStages().iterator().next();
    List<ExecutionCommandWrapper> commands = stage.getExecutionCommands("c1-c6401");
    ExecutionCommand command = commands.get(0).getExecutionCommand();

    // ZK is the only service that is versionable
    Assert.assertFalse(MapUtils.isEmpty(command.getComponentVersionMap()));
    Assert.assertEquals(1, command.getComponentVersionMap().size());
    Assert.assertTrue(command.getComponentVersionMap().containsKey("ZOOKEEPER"));
    Assert.assertNull(command.getRepositoryFile());
  }

  /**
   * Tests that if a component's repository is not resolved, then the repo
   * version map does not get populated.
   *
   * @throws Exception
   */
  @Test
  public void testAvailableServicesMapIsEmptyWhenRepositoriesNotResolved() throws Exception {

    // set all repos to resolve=false to verify that we don't get a
    // component version map
    RepositoryVersionDAO repositoryVersionDAO = injector.getInstance(RepositoryVersionDAO.class);
    List<RepositoryVersionEntity> repoVersions = repositoryVersionDAO.findAll();
    for (RepositoryVersionEntity repoVersion : repoVersions) {
      repoVersion.setResolved(false);
      repositoryVersionDAO.merge(repoVersion);
    }

    Map<String, String> requestProperties = new HashMap<String, String>() {
      {
        put(REQUEST_CONTEXT_PROPERTY, "Refresh YARN Capacity Scheduler");
        put("command", "REFRESHQUEUES");
      }
    };

    ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1", "REFRESHQUEUES",
        new HashMap<String, String>() {
          {
            put("forceRefreshConfigTags", "capacity-scheduler");
          }
        }, false);

    actionRequest.getResourceFilters().add(new RequestResourceFilter("YARN", "RESOURCEMANAGER",
        Collections.singletonList("c1-c6401")));

    EasyMock.replay(hostRoleCommand, actionManager, configHelper);

    ambariManagementController.createAction(actionRequest, requestProperties);
    Request request = requestCapture.getValue();
    Stage stage = request.getStages().iterator().next();
    List<ExecutionCommandWrapper> commands = stage.getExecutionCommands("c1-c6401");
    ExecutionCommand command = commands.get(0).getExecutionCommand();

    Assert.assertTrue(MapUtils.isEmpty(command.getComponentVersionMap()));
    Assert.assertNull(command.getRepositoryFile());
  }

  @Test
  public void testCommandRepository() throws Exception {
    Cluster cluster = clusters.getCluster("c1");
    Service serviceYARN = cluster.getService("YARN");
    Service serviceZK = cluster.getService("ZOOKEEPER");
    ServiceComponent componentRM = serviceYARN.getServiceComponent("RESOURCEMANAGER");
    ServiceComponent componentZKC = serviceZK.getServiceComponent("ZOOKEEPER_CLIENT");
    Host host = clusters.getHost("c1-c6401");

    StackDAO stackDAO = injector.getInstance(StackDAO.class);
    RepositoryVersionDAO repoVersionDAO = injector.getInstance(RepositoryVersionDAO.class);
    ServiceComponentDesiredStateDAO componentDAO = injector.getInstance(ServiceComponentDesiredStateDAO.class);
    RepositoryVersionHelper repoVersionHelper = injector.getInstance(RepositoryVersionHelper.class);

    CommandRepository commandRepo = repoVersionHelper.getCommandRepository(cluster, componentRM, host);
    Assert.assertEquals(2, commandRepo.getRepositories().size());


    List<RepoOsEntity> operatingSystems = new ArrayList<>();
    RepoDefinitionEntity repoDefinitionEntity1 = new RepoDefinitionEntity();
    repoDefinitionEntity1.setRepoID("new-id");
    repoDefinitionEntity1.setBaseUrl("http://foo");
    repoDefinitionEntity1.setRepoName("HDP");
    RepoOsEntity repoOsEntity = new RepoOsEntity();
    repoOsEntity.setFamily("redhat6");
    repoOsEntity.setAmbariManaged(true);
    repoOsEntity.addRepoDefinition(repoDefinitionEntity1); operatingSystems .add(repoOsEntity);

    StackEntity stackEntity = stackDAO.find(cluster.getDesiredStackVersion().getStackName(),
        cluster.getDesiredStackVersion().getStackVersion());

    RepositoryVersionEntity repositoryVersion = new RepositoryVersionEntity(stackEntity,
        "2.1.1.1-1234", "2.1.1.1-1234", operatingSystems);
    repositoryVersion = repoVersionDAO.merge(repositoryVersion);

    // add a repo version associated with a component
    ServiceComponentDesiredStateEntity componentEntity = componentDAO.findByName(cluster.getClusterId(),
        serviceYARN.getName(), componentRM.getName());

    ServiceComponentVersionEntity componentVersionEntity = new ServiceComponentVersionEntity();
    componentVersionEntity.setRepositoryVersion(repositoryVersion);
    componentVersionEntity.setUserName("admin");

    componentEntity.setDesiredRepositoryVersion(repositoryVersion);
    componentEntity.addVersion(componentVersionEntity);
    componentDAO.merge(componentEntity);

    // !!! make sure the override is set
    commandRepo = repoVersionHelper.getCommandRepository(cluster, componentRM, host);

    Assert.assertEquals(1, commandRepo.getRepositories().size());
    CommandRepository.Repository repo = commandRepo.getRepositories().iterator().next();
    Assert.assertEquals("http://foo", repo.getBaseUrl());

    // verify that ZK has no repositories, since we haven't defined a repo version for ZKC
    commandRepo = repoVersionHelper.getCommandRepository(cluster, componentZKC, host);
    Assert.assertEquals(2, commandRepo.getRepositories().size());
  }

  /**
   * Tests that when {@link ActionExecutionContext#isFutureCommand()} is set, invalid
   * hosts/components are still scheduled.
   *
   * @throws Exception
   */
  @Test
  public void testFutureCommandsPassValidation() throws Exception {
    // make sure the component isn't added to the cluster yet
    String serviceName = "YARN";
    String componentName = "YARN_FOO";
    Cluster cluster = clusters.getCluster("c1");
    Service service = cluster.getService(serviceName);
    try {
      service.getServiceComponent(componentName);
      Assert.fail("For this test to work, the YARN_FOO component must not be in the cluster yet");
    } catch (AmbariException ambariException) {
      // expected
    }

    AmbariCustomCommandExecutionHelper ambariCustomCommandExecutionHelper = injector.getInstance(AmbariCustomCommandExecutionHelper.class);

    List<RequestResourceFilter> requestResourceFilter = new ArrayList<RequestResourceFilter>() {{
        add(new RequestResourceFilter(serviceName, componentName,
            Collections.singletonList("c1-c6401")));
    }};

    ActionExecutionContext actionExecutionContext = new ActionExecutionContext("c1", "RESTART", requestResourceFilter);
    actionExecutionContext.setIsFutureCommand(true);

    Stage stage = EasyMock.niceMock(Stage.class);
    ExecutionCommandWrapper execCmdWrapper = EasyMock.niceMock(ExecutionCommandWrapper.class);
    ExecutionCommand execCmd = EasyMock.niceMock(ExecutionCommand.class);

    EasyMock.expect(stage.getClusterName()).andReturn("c1");

    EasyMock.expect(stage.getExecutionCommandWrapper(EasyMock.eq("c1-c6401"), EasyMock.anyString())).andReturn(execCmdWrapper);
    EasyMock.expect(execCmdWrapper.getExecutionCommand()).andReturn(execCmd);
    EasyMock.expectLastCall();

    HashSet<String> localComponents = new HashSet<>();
    EasyMock.expect(execCmd.getLocalComponents()).andReturn(localComponents).anyTimes();
    EasyMock.replay(configHelper, stage, execCmdWrapper, execCmd);

    ambariCustomCommandExecutionHelper.addExecutionCommandsToStage(actionExecutionContext, stage, new HashMap<>(), null);

    EasyMock.verify(configHelper, stage, execCmdWrapper, execCmd);
  }

  private void createClusterFixture(String clusterName, StackId stackId,
    String respositoryVersion, String hostPrefix) throws AmbariException, AuthorizationException, NoSuchFieldException, IllegalAccessException {

    String hostC6401 = hostPrefix + "-c6401";
    String hostC6402 = hostPrefix + "-c6402";

    OrmTestHelper ormTestHelper = injector.getInstance(OrmTestHelper.class);
    RepositoryVersionEntity repositoryVersion = ormTestHelper.getOrCreateRepositoryVersion(stackId,
        respositoryVersion);

    createCluster(clusterName, stackId.getStackId());

    addHost(hostC6401, clusterName);
    addHost(hostC6402, clusterName);

    clusters.updateHostMappings(clusters.getHost(hostC6401));
    clusters.updateHostMappings(clusters.getHost(hostC6402));

    Cluster cluster = clusters.getCluster(clusterName);
    Assert.assertNotNull(cluster);

    createService(clusterName, "YARN", repositoryVersion);
    createService(clusterName, "GANGLIA", repositoryVersion);
    createService(clusterName, "ZOOKEEPER", repositoryVersion);
    createService(clusterName, "FLUME", repositoryVersion);

    createServiceComponent(clusterName, "YARN", "RESOURCEMANAGER", State.INIT);
    createServiceComponent(clusterName, "YARN", "NODEMANAGER", State.INIT);
    createServiceComponent(clusterName, "GANGLIA", "GANGLIA_SERVER", State.INIT);
    createServiceComponent(clusterName, "GANGLIA", "GANGLIA_MONITOR", State.INIT);
    createServiceComponent(clusterName, "ZOOKEEPER", "ZOOKEEPER_CLIENT", State.INIT);

    // this component should be not installed on any host
    createServiceComponent(clusterName, "FLUME", "FLUME_HANDLER", State.INIT);

    createServiceComponentHost(clusterName, "YARN", "RESOURCEMANAGER", hostC6401, null);
    createServiceComponentHost(clusterName, "YARN", "NODEMANAGER", hostC6401, null);
    createServiceComponentHost(clusterName, "GANGLIA", "GANGLIA_SERVER", hostC6401, State.INIT);
    createServiceComponentHost(clusterName, "GANGLIA", "GANGLIA_MONITOR", hostC6401, State.INIT);
    createServiceComponentHost(clusterName, "ZOOKEEPER", "ZOOKEEPER_CLIENT", hostC6401, State.INIT);

    createServiceComponentHost(clusterName, "YARN", "NODEMANAGER", hostC6402, null);
    createServiceComponentHost(clusterName, "GANGLIA", "GANGLIA_MONITOR", hostC6402, State.INIT);
    createServiceComponentHost(clusterName, "ZOOKEEPER", "ZOOKEEPER_CLIENT", hostC6402, State.INIT);
  }
  private void addHost(String hostname, String clusterName) throws AmbariException {
    clusters.addHost(hostname);
    setOsFamily(clusters.getHost(hostname), "redhat", "6.3");
    clusters.getHost(hostname).setState(HostState.HEALTHY);
    if (null != clusterName) {
      clusters.mapHostToCluster(hostname, clusterName);
    }
  }
  private void setOsFamily(Host host, String osFamily, String osVersion) {
    Map<String, String> hostAttributes = new HashMap<>();
    hostAttributes.put("os_family", osFamily);
    hostAttributes.put("os_release_version", osVersion);

    host.setHostAttributes(hostAttributes);
  }

  private void createCluster(String clusterName, String stackVersion) throws AmbariException, AuthorizationException {
    ClusterRequest r = new ClusterRequest(null, clusterName, State.INSTALLED.name(),
        SecurityType.NONE, stackVersion, null);
    ambariManagementController.createCluster(r);
  }

  private void createService(String clusterName, String serviceName,
      RepositoryVersionEntity repositoryVersion) throws AmbariException, AuthorizationException, NoSuchFieldException, IllegalAccessException {

    ServiceRequest r1 = new ServiceRequest(clusterName, serviceName,
        repositoryVersion.getId(), null, "false");

    Set<ServiceRequest> requests = new HashSet<>();
    requests.add(r1);

    ServiceResourceProviderTest.createServices(ambariManagementController,
        injector.getInstance(RepositoryVersionDAO.class), requests);
  }

  private void createServiceComponent(String clusterName,
      String serviceName, String componentName, State desiredState)
      throws AmbariException, AuthorizationException {
    String dStateStr = null;
    if (desiredState != null) {
      dStateStr = desiredState.toString();
    }
    ServiceComponentRequest r = new ServiceComponentRequest(clusterName,
        serviceName, componentName, dStateStr);
    Set<ServiceComponentRequest> requests =
      new HashSet<>();
    requests.add(r);
    ComponentResourceProviderTest.createComponents(ambariManagementController, requests);
  }

  private void createServiceComponentHost(String clusterName, String serviceName, String componentName, String hostname, State desiredState)
      throws AmbariException, AuthorizationException {
    String dStateStr = null;
    if (desiredState != null) {
      dStateStr = desiredState.toString();
    }
    ServiceComponentHostRequest r = new ServiceComponentHostRequest(clusterName,
        serviceName, componentName, hostname, dStateStr);
    Set<ServiceComponentHostRequest> requests =
      new HashSet<>();
    requests.add(r);
    ambariManagementController.createHostComponents(requests);
  }

}
