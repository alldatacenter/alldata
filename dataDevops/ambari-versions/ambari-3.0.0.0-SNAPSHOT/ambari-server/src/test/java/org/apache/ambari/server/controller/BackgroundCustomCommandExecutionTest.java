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

import static org.mockito.Matchers.any;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.Request;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.agent.AgentCommand.AgentCommandType;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.ComponentResourceProviderTest;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.controller.internal.ServiceResourceProviderTest;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

@RunWith(MockitoJUnitRunner.class)
public class BackgroundCustomCommandExecutionTest {
  private Injector injector;
  private AmbariManagementController controller;
  private Clusters clusters;

  private static final String REQUEST_CONTEXT_PROPERTY = "context";
  private static final String UPDATE_REPLICATION_PARAMS = "{\n" +
          "              \"replication_cluster_keys\": c7007.ambari.apache.org,c7008.ambari.apache.org,c7009.ambari.apache.org:2181:/hbase,\n" +
          "              \"replication_peers\": 1\n" +
          "            }";
  private static final String STOP_REPLICATION_PARAMS = "{\n" +
          "              \"replication_peers\": 1\n" +
          "            }";

  @Captor ArgumentCaptor<Request> requestCapture;
  @Mock ActionManager am;

  private final String STACK_VERSION = "2.0.6";
  private final String REPO_VERSION = "2.0.6-1234";
  private final StackId STACK_ID = new StackId("HDP", STACK_VERSION);
  private RepositoryVersionEntity m_repositoryVersion;

  @Before
  public void setup() throws Exception {
    Configuration configuration;
    TopologyManager topologyManager;

    InMemoryDefaultTestModule module = new InMemoryDefaultTestModule(){


      @Override
      protected void configure() {
        getProperties().put(Configuration.CUSTOM_ACTION_DEFINITION.getKey(), "src/main/resources/custom_action_definitions");
        super.configure();
        bind(ActionManager.class).toInstance(am);
      }
    };
    injector = Guice.createInjector(module);


    injector.getInstance(GuiceJpaInitializer.class);
    controller = injector.getInstance(AmbariManagementController.class);
    clusters = injector.getInstance(Clusters.class);
    configuration = injector.getInstance(Configuration.class);
    topologyManager = injector.getInstance(TopologyManager.class);
    OrmTestHelper ormTestHelper = injector.getInstance(OrmTestHelper.class);

    Assert.assertEquals("src/main/resources/custom_action_definitions", configuration.getCustomActionDefinitionPath());

    StageUtils.setTopologyManager(topologyManager);
    StageUtils.setConfiguration(configuration);

    // Set the authenticated user
    // TODO: remove this or replace the authenticated user to test authorization rules
    // Set the authenticated user
    // TODO: remove this or replace the authenticated user to test authorization rules
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    m_repositoryVersion = ormTestHelper.getOrCreateRepositoryVersion(STACK_ID, REPO_VERSION);
    Assert.assertNotNull(m_repositoryVersion);
  }
  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @SuppressWarnings("serial")
  @Test
  public void testRebalanceHdfsCustomCommand() {
    try {
      createClusterFixture();

      Map<String, String> requestProperties = new HashMap<String, String>() {
        {
          put(REQUEST_CONTEXT_PROPERTY, "Refresh YARN Capacity Scheduler");
          put("command", "REBALANCEHDFS");
          put("namenode" , "{\"threshold\":13}");//case is important here
        }
      };

      ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1",
          "REBALANCEHDFS", new HashMap<>(), false);
      actionRequest.getResourceFilters().add(new RequestResourceFilter("HDFS", "NAMENODE",Collections.singletonList("c6401")));

      controller.createAction(actionRequest, requestProperties);

      Mockito.verify(am, Mockito.times(1)).sendActions(requestCapture.capture(), any(ExecuteActionRequest.class));

      Request request = requestCapture.getValue();
      Assert.assertNotNull(request);
      Assert.assertNotNull(request.getStages());
      Assert.assertEquals(1, request.getStages().size());
      Stage stage = request.getStages().iterator().next();

      System.out.println(stage);

      Assert.assertEquals(1, stage.getHosts().size());

      List<ExecutionCommandWrapper> commands = stage.getExecutionCommands("c6401");
      Assert.assertEquals(1, commands.size());

      ExecutionCommand command = commands.get(0).getExecutionCommand();

      Assert.assertEquals(AgentCommandType.BACKGROUND_EXECUTION_COMMAND, command.getCommandType());
      Assert.assertEquals("{\"threshold\":13}", command.getCommandParams().get("namenode"));

    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  private void createClusterFixture() throws AmbariException, AuthorizationException, NoSuchFieldException, IllegalAccessException {
    createCluster("c1");
    addHost("c6401","c1");
    addHost("c6402","c1");
    addHost("c7007", "c1");
    clusters.updateHostMappings(clusters.getHost("c6401"));
    clusters.updateHostMappings(clusters.getHost("c6402"));
    clusters.updateHostMappings(clusters.getHost("c7007"));

    clusters.getCluster("c1");
    createService("c1", "HDFS", null);
    createService("c1", "HBASE", null);
    createServiceComponent("c1", "HDFS", "NAMENODE", State.INIT);
    createServiceComponent("c1", "HBASE", "HBASE_MASTER", State.INIT);
    createServiceComponentHost("c1", "HDFS", "NAMENODE", "c6401", null);
    createServiceComponentHost("c1", "HBASE", "HBASE_MASTER", "c7007", null);
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


  @SuppressWarnings("serial")
  @Test
  public void testUpdateHBaseReplicationCustomCommand()
          throws AuthorizationException, AmbariException, IllegalAccessException,
          NoSuchFieldException {
    createClusterFixture();
    Map<String, String> requestProperties = new HashMap<String, String>() {
      {
        put(REQUEST_CONTEXT_PROPERTY, "Enable Cross Cluster HBase Replication");
        put("command", "UPDATE_REPLICATION");
        put("parameters", UPDATE_REPLICATION_PARAMS);
      }
    };
    ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1",
            "UPDATE_REPLICATION", new HashMap<>(), false);
    actionRequest.getResourceFilters().add(new RequestResourceFilter("HBASE", "HBASE_MASTER",
            Collections.singletonList("c7007")));

    controller.createAction(actionRequest, requestProperties);

    Mockito.verify(am, Mockito.times(1))
            .sendActions(requestCapture.capture(), any(ExecuteActionRequest.class));

    Request request = requestCapture.getValue();
    Assert.assertNotNull(request);
    Assert.assertNotNull(request.getStages());
    Assert.assertEquals(1, request.getStages().size());
    Stage stage = request.getStages().iterator().next();

    Assert.assertEquals(1, stage.getHosts().size());

    List<ExecutionCommandWrapper> commands = stage.getExecutionCommands("c7007");
    Assert.assertEquals(1, commands.size());
    ExecutionCommand command = commands.get(0).getExecutionCommand();
    Assert.assertEquals(AgentCommandType.EXECUTION_COMMAND, command.getCommandType());
    Assert.assertEquals("UPDATE_REPLICATION", command.getCommandParams().get("custom_command"));

  }



  @SuppressWarnings("serial")
  @Test
  public void testStopHBaseReplicationCustomCommand()
          throws AuthorizationException, AmbariException, IllegalAccessException,
          NoSuchFieldException {
    createClusterFixture();
    Map<String, String> requestProperties = new HashMap<String, String>() {
      {
        put(REQUEST_CONTEXT_PROPERTY, "Disable Cross Cluster HBase Replication");
        put("command", "STOP_REPLICATION");
        put("parameters", STOP_REPLICATION_PARAMS);
      }
    };
    ExecuteActionRequest actionRequest = new ExecuteActionRequest("c1",
            "STOP_REPLICATION", new HashMap<>(), false);
    actionRequest.getResourceFilters().add(new RequestResourceFilter("HBASE", "HBASE_MASTER",
            Collections.singletonList("c7007")));

    controller.createAction(actionRequest, requestProperties);

    Mockito.verify(am, Mockito.times(1))
            .sendActions(requestCapture.capture(), any(ExecuteActionRequest.class));

    Request request = requestCapture.getValue();
    Assert.assertNotNull(request);
    Assert.assertNotNull(request.getStages());
    Assert.assertEquals(1, request.getStages().size());
    Stage stage = request.getStages().iterator().next();

    Assert.assertEquals(1, stage.getHosts().size());

    List<ExecutionCommandWrapper> commands = stage.getExecutionCommands("c7007");
    Assert.assertEquals(1, commands.size());
    ExecutionCommand command = commands.get(0).getExecutionCommand();
    Assert.assertEquals(AgentCommandType.EXECUTION_COMMAND, command.getCommandType());
    Assert.assertEquals("STOP_REPLICATION", command.getCommandParams().get("custom_command"));

  }




  private void createCluster(String clusterName) throws AmbariException, AuthorizationException {
    ClusterRequest r = new ClusterRequest(null, clusterName, State.INSTALLED.name(),
        SecurityType.NONE, STACK_ID.getStackId(), null);

    controller.createCluster(r);
  }

  private void createService(String clusterName,
      String serviceName, State desiredState) throws AmbariException, AuthorizationException, NoSuchFieldException, IllegalAccessException {
    String dStateStr = null;
    if (desiredState != null) {
      dStateStr = desiredState.toString();
    }
    ServiceRequest r1 = new ServiceRequest(clusterName, serviceName,
        m_repositoryVersion.getId(), dStateStr);

    Set<ServiceRequest> requests = new HashSet<>();
    requests.add(r1);

    ServiceResourceProviderTest.createServices(controller,
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
    ComponentResourceProviderTest.createComponents(controller, requests);
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
    controller.createHostComponents(requests);
  }

}
