/**
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
package org.apache.ambari.server.serveraction.upgrades;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.alerts.AmbariPerformanceRunnable;
import org.apache.ambari.server.events.listeners.upgrade.StackVersionListener;
import org.apache.ambari.server.stack.MasterHostResolver;
import org.apache.ambari.server.stack.upgrade.AddComponentTask;
import org.apache.ambari.server.stack.upgrade.ExecuteHostType;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContext;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.State;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Lists;
import com.google.gson.Gson;

/**
 * Tests {@link AddComponentAction}.
 */
/**
 * Tests {@link AmbariPerformanceRunnable}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ AddComponentAction.class, MasterHostResolver.class })
public class AddComponentActionTest extends EasyMockSupport {

  private static final String CANDIDATE_SERVICE = "FOO-SERVICE";
  private static final String CANDIDATE_COMPONENT = "FOO-COMPONENT";
  private static final String NEW_SERVICE = CANDIDATE_SERVICE;
  private static final String NEW_COMPONENT = "FOO-NEW-COMPONENT";
  private static final String CLUSTER_NAME = "c1";

  private final Map<String, String> m_commandParams = new HashMap<>();

  private final Clusters m_mockClusters = createNiceMock(Clusters.class);
  private final Cluster m_mockCluster = createNiceMock(Cluster.class);
  private final Service m_mockCandidateService = createNiceMock(Service.class);
  private final ServiceComponent m_mockCandidateServiceComponent = createNiceMock(ServiceComponent.class);

  private final UpgradeContext m_mockUpgradeContext = createNiceMock(UpgradeContext.class);

  private final String CANDIDATE_HOST_NAME = "c6401.ambari.apache.org";
  private final Host m_mockHost = createStrictMock(Host.class);
  private final Collection<Host> m_candidateHosts = Lists.newArrayList(m_mockHost);

  private AddComponentAction m_action;

  @Before
  public void before() throws Exception {
    PowerMock.mockStatic(MasterHostResolver.class);
    expect(MasterHostResolver.getCandidateHosts(m_mockCluster, ExecuteHostType.ALL,
        CANDIDATE_SERVICE, CANDIDATE_COMPONENT)).andReturn(m_candidateHosts).once();
    PowerMock.replay(MasterHostResolver.class);

    m_action = PowerMock.createNicePartialMock(AddComponentAction.class, "getUpgradeContext",
        "createCommandReport", "getClusters", "getGson");

    ExecutionCommand executionCommand = createNiceMock(ExecutionCommand.class);
    expect(executionCommand.getCommandParams()).andReturn(m_commandParams).once();
    m_action.setExecutionCommand(executionCommand);

    expect(m_action.getClusters()).andReturn(m_mockClusters).atLeastOnce();
    expect(m_action.getUpgradeContext(m_mockCluster)).andReturn(m_mockUpgradeContext).once();
    expect(m_action.getGson()).andReturn(new Gson()).once();

    AddComponentTask addComponentTask = new AddComponentTask();
    addComponentTask.service = NEW_SERVICE;
    addComponentTask.component = NEW_COMPONENT;
    addComponentTask.hostService = CANDIDATE_SERVICE;
    addComponentTask.hostComponent = CANDIDATE_COMPONENT;
    addComponentTask.hosts = ExecuteHostType.ALL;

    String addComponentTaskJson = addComponentTask.toJson();
    m_commandParams.put("clusterName", CLUSTER_NAME);
    m_commandParams.put(AddComponentTask.PARAMETER_SERIALIZED_ADD_COMPONENT_TASK,
        addComponentTaskJson);

    expect(m_mockClusters.getCluster(CLUSTER_NAME)).andReturn(m_mockCluster).once();
  }

  @After
  public void after() throws Exception {
    PowerMock.verify(m_action);
  }

  /**
   * Tests that adding a component during upgrade invokes the correct methods.
   *
   * @throws Exception
   */
  @Test
  public void testAddComponentDuringUpgrade() throws Exception {
    expect(m_mockCluster.getService(NEW_SERVICE)).andReturn(m_mockCandidateService).once();
    expect(m_mockCandidateService.getServiceComponent(NEW_COMPONENT)).andThrow(new ServiceComponentNotFoundException(CLUSTER_NAME, NEW_SERVICE, NEW_COMPONENT));
    expect(m_mockCandidateService.addServiceComponent(NEW_COMPONENT)).andReturn(m_mockCandidateServiceComponent).once();

    expect(m_mockHost.getHostName()).andReturn(CANDIDATE_HOST_NAME).atLeastOnce();

    m_mockCandidateServiceComponent.setDesiredState(State.INSTALLED);
    expectLastCall().once();

    Map<String, ServiceComponentHost> existingSCHs = new HashMap<>();
    expect(m_mockCandidateServiceComponent.getServiceComponentHosts()).andReturn(existingSCHs).once();

    ServiceComponentHost mockServiceComponentHost = createNiceMock(ServiceComponentHost.class);
    expect(m_mockCandidateServiceComponent.addServiceComponentHost(CANDIDATE_HOST_NAME)).andReturn(mockServiceComponentHost).once();
    mockServiceComponentHost.setState(State.INSTALLED);
    expectLastCall().once();

    mockServiceComponentHost.setDesiredState(State.INSTALLED);
    expectLastCall().once();

    mockServiceComponentHost.setVersion(StackVersionListener.UNKNOWN_VERSION);
    expectLastCall().once();

    PowerMock.replay(m_action);
    replayAll();

    m_action.execute(null);

    verifyAll();
  }

  /**
   * Tests that we fail without any candidates.
   *
   * @throws Exception
   */
  @Test
  public void testAddComponentDuringUpgradeFailsWithNoCandidates() throws Exception {
    PowerMock.replay(m_action);
    replayAll();

    m_candidateHosts.clear();

    m_action.execute(null);

    verifyAll();
  }

  /**
   * Tests that we fail when the candidateg service isn't installed in the
   * cluster.
   *
   * @throws Exception
   */
  @Test
  public void testAddComponentWhereServiceIsNotInstalled() throws Exception {
    expect(m_mockCluster.getService(NEW_SERVICE)).andThrow(
        new ServiceNotFoundException(CLUSTER_NAME, CANDIDATE_SERVICE)).once();

    PowerMock.replay(m_action);
    replayAll();

    m_action.execute(null);

    verifyAll();
  }

}
