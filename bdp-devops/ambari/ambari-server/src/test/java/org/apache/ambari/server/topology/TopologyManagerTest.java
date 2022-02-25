/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.topology;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.powermock.api.easymock.PowerMock.mockStatic;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ShortTaskStatus;
import org.apache.ambari.server.controller.internal.HostResourceProvider;
import org.apache.ambari.server.controller.internal.ProvisionClusterRequest;
import org.apache.ambari.server.controller.internal.ScaleClusterRequest;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.events.ClusterProvisionStartedEvent;
import org.apache.ambari.server.events.ClusterProvisionedEvent;
import org.apache.ambari.server.events.RequestFinishedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.SettingDAO;
import org.apache.ambari.server.orm.entities.SettingEntity;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.stack.NoSuchStackException;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.quicklinksprofile.QuickLinksProfile;
import org.apache.ambari.server.topology.tasks.ConfigureClusterTask;
import org.apache.ambari.server.topology.tasks.ConfigureClusterTaskFactory;
import org.apache.ambari.server.topology.validators.TopologyValidatorService;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.MockType;
import org.easymock.TestSubject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * TopologyManager unit tests
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( { TopologyManager.class })
public class TopologyManagerTest {

  private static final String CLUSTER_NAME = "test-cluster";
  private static final long CLUSTER_ID = 1;
  private static final String BLUEPRINT_NAME = "test-bp";
  private static final String STACK_NAME = "test-stack";
  private static final String STACK_VERSION = "test-stack-version";
  private static final String SAMPLE_QUICKLINKS_PROFILE_1 = "{\"filters\":[{\"visible\":true}],\"services\":[]}";
  private static final String SAMPLE_QUICKLINKS_PROFILE_2 =
      "{\"filters\":[],\"services\":[{\"name\":\"HDFS\",\"components\":[],\"filters\":[{\"visible\":true}]}]}";

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @TestSubject
  private TopologyManager topologyManager = new TopologyManager();

  @TestSubject
  private TopologyManager topologyManagerReplay = new TopologyManager();

  @Mock(type = MockType.NICE)
  private Blueprint blueprint;

  @Mock(type = MockType.NICE)
  private Stack stack;

  @Mock(type = MockType.NICE)
  private ProvisionClusterRequest request;

  private final PersistedTopologyRequest persistedTopologyRequest = new PersistedTopologyRequest(1, request);
  @Mock(type = MockType.STRICT)
  private LogicalRequestFactory logicalRequestFactory;
  @Mock(type = MockType.DEFAULT)
  private LogicalRequest logicalRequest;
  @Mock(type = MockType.NICE)
  private AmbariContext ambariContext;
  @Mock(type = MockType.NICE)
  private ConfigurationRequest configurationRequest;
  @Mock(type = MockType.NICE)
  private ConfigurationRequest configurationRequest2;
  @Mock(type = MockType.NICE)
  private ConfigurationRequest configurationRequest3;
  @Mock(type = MockType.NICE)
  private RequestStatusResponse requestStatusResponse;
  @Mock(type = MockType.STRICT)
  private ExecutorService executor;
  @Mock(type = MockType.NICE)
  private PersistedState persistedState;
  @Mock(type = MockType.NICE)
  private HostGroup group1;
  @Mock(type = MockType.NICE)
  private HostGroup group2;
  @Mock(type = MockType.STRICT)
  private SecurityConfigurationFactory securityConfigurationFactory;
  @Mock(type = MockType.STRICT)
  private CredentialStoreService credentialStoreService;
  @Mock(type = MockType.STRICT)
  private ClusterController clusterController;
  @Mock(type = MockType.STRICT)
  private ResourceProvider resourceProvider;
  @Mock(type = MockType.STRICT)
  private SettingDAO settingDAO;
  @Mock(type = MockType.NICE)
  private ClusterTopology clusterTopologyMock;
  @Mock(type = MockType.NICE)
  private ConfigureClusterTaskFactory configureClusterTaskFactory;
  @Mock(type = MockType.NICE)
  private ConfigureClusterTask configureClusterTask;
  @Mock(type = MockType.NICE)
  private AmbariEventPublisher eventPublisher;

  @Mock(type = MockType.STRICT)
  private Future mockFuture;

  @Mock
  private TopologyValidatorService topologyValidatorService;

  private final Configuration stackConfig = new Configuration(new HashMap<>(),
    new HashMap<>());
  private final Configuration bpConfiguration = new Configuration(new HashMap<>(),
    new HashMap<>(), stackConfig);
  private final Configuration topoConfiguration = new Configuration(new HashMap<>(),
    new HashMap<>(), bpConfiguration);
  private final Configuration bpGroup1Config = new Configuration(new HashMap<>(),
    new HashMap<>(), bpConfiguration);
  private final Configuration bpGroup2Config = new Configuration(new HashMap<>(),
    new HashMap<>(), bpConfiguration);
  //todo: topo config hierarchy is wrong: bpGroupConfigs should extend topo cluster config
  private final Configuration topoGroup1Config = new Configuration(new HashMap<>(),
    new HashMap<>(), bpGroup1Config);
  private final Configuration topoGroup2Config = new Configuration(new HashMap<>(),
    new HashMap<>(), bpGroup2Config);

  private HostGroupInfo group1Info = new HostGroupInfo("group1");
  private HostGroupInfo group2Info = new HostGroupInfo("group2");
  private Map<String, HostGroupInfo> groupInfoMap = new HashMap<>();

  private Collection<Component> group1Components = Arrays.asList(new Component("component1"), new Component("component2"), new Component("component3"));
  private Collection<Component> group2Components = Arrays.asList(new Component("component3"), new Component("component4"));

  private Map<String, Collection<String>> group1ServiceComponents = new HashMap<>();
  private Map<String, Collection<String>> group2ServiceComponents = new HashMap<>();

  private Map<String, Collection<String>> serviceComponents = new HashMap<>();

  private String predicate = "Hosts/host_name=foo";

  private List<TopologyValidator> topologyValidators = new ArrayList<>();

  private Capture<ClusterTopology> clusterTopologyCapture;
  private Capture<Map<String, Object>> configRequestPropertiesCapture;
  private Capture<Map<String, Object>> configRequestPropertiesCapture2;
  private Capture<Map<String, Object>> configRequestPropertiesCapture3;
  private Capture<ClusterRequest> updateClusterConfigRequestCapture;
  private Capture<Runnable> updateConfigTaskCapture;

  @Before
  public void setup() throws Exception {
    clusterTopologyCapture = newCapture();
    configRequestPropertiesCapture = newCapture();
    configRequestPropertiesCapture2 = newCapture();
    configRequestPropertiesCapture3 = newCapture();
    updateClusterConfigRequestCapture = newCapture();
    updateConfigTaskCapture = newCapture();

    topoConfiguration.setProperty("service1-site", "s1-prop", "s1-prop-value");
    topoConfiguration.setProperty("service2-site", "s2-prop", "s2-prop-value");
    topoConfiguration.setProperty("cluster-env", "g-prop", "g-prop-value");

    //clusterRequestCapture = EasyMock.newCapture();
    // group 1 has fqdn specified
    group1Info.addHost("host1");
    group1Info.setConfiguration(topoGroup1Config);
    // group 2 has host_count and host_predicate specified
    group2Info.setRequestedCount(2);
    group2Info.setPredicate(predicate);
    group2Info.setConfiguration(topoGroup2Config);

    groupInfoMap.put("group1", group1Info);
    groupInfoMap.put("group2", group2Info);

    Map<String, HostGroup> groupMap = new HashMap<>();
    groupMap.put("group1", group1);
    groupMap.put("group2", group2);

    serviceComponents.put("service1", Arrays.asList("component1", "component3"));
    serviceComponents.put("service2", Arrays.asList("component2", "component4"));

    group1ServiceComponents.put("service1", Arrays.asList("component1", "component3"));
    group1ServiceComponents.put("service2", Collections.singleton("component2"));
    group2ServiceComponents.put("service2",  Arrays.asList("component3", "component4"));

    expect(blueprint.getHostGroup("group1")).andReturn(group1).anyTimes();
    expect(blueprint.getHostGroup("group2")).andReturn(group2).anyTimes();
    expect(blueprint.getComponents("service1")).andReturn(Arrays.asList("component1", "component3")).anyTimes();
    expect(blueprint.getComponents("service2")).andReturn(Arrays.asList("component2", "component4")).anyTimes();
    expect(blueprint.getConfiguration()).andReturn(bpConfiguration).anyTimes();
    expect(blueprint.getHostGroups()).andReturn(groupMap).anyTimes();
    expect(blueprint.getHostGroupsForComponent("component1")).andReturn(Collections.singleton(group1)).anyTimes();
    expect(blueprint.getHostGroupsForComponent("component2")).andReturn(Collections.singleton(group1)).anyTimes();
    expect(blueprint.getHostGroupsForComponent("component3")).andReturn(Arrays.asList(group1, group2)).anyTimes();
    expect(blueprint.getHostGroupsForComponent("component4")).andReturn(Collections.singleton(group2)).anyTimes();
    expect(blueprint.getHostGroupsForService("service1")).andReturn(Arrays.asList(group1, group2)).anyTimes();
    expect(blueprint.getHostGroupsForService("service2")).andReturn(Arrays.asList(group1, group2)).anyTimes();
    expect(blueprint.getName()).andReturn(BLUEPRINT_NAME).anyTimes();
    expect(blueprint.getServices()).andReturn(Arrays.asList("service1", "service2")).anyTimes();
    expect(blueprint.getStack()).andReturn(stack).anyTimes();
    expect(blueprint.getRepositorySettings()).andReturn(new ArrayList<>()).anyTimes();
    // don't expect toEntity()

    expect(stack.getAllConfigurationTypes("service1")).andReturn(Arrays.asList("service1-site", "service1-env")).anyTimes();
    expect(stack.getAllConfigurationTypes("service2")).andReturn(Arrays.asList("service2-site", "service2-env")).anyTimes();
    expect(stack.getAutoDeployInfo("component1")).andReturn(null).anyTimes();
    expect(stack.getAutoDeployInfo("component2")).andReturn(null).anyTimes();
    expect(stack.getAutoDeployInfo("component3")).andReturn(null).anyTimes();
    expect(stack.getAutoDeployInfo("component4")).andReturn(null).anyTimes();
    expect(stack.getCardinality("component1")).andReturn(new Cardinality("1")).anyTimes();
    expect(stack.getCardinality("component2")).andReturn(new Cardinality("1")).anyTimes();
    expect(stack.getCardinality("component3")).andReturn(new Cardinality("1+")).anyTimes();
    expect(stack.getCardinality("component4")).andReturn(new Cardinality("1+")).anyTimes();
    expect(stack.getComponents()).andReturn(serviceComponents).anyTimes();
    expect(stack.getComponents("service1")).andReturn(serviceComponents.get("service1")).anyTimes();
    expect(stack.getComponents("service2")).andReturn(serviceComponents.get("service2")).anyTimes();
    expect(stack.getServiceForConfigType("service1-site")).andReturn("service1").anyTimes();
    expect(stack.getServiceForConfigType("service2-site")).andReturn("service2").anyTimes();
    expect(stack.getConfiguration()).andReturn(stackConfig).anyTimes();
    expect(stack.getName()).andReturn(STACK_NAME).anyTimes();
    expect(stack.getVersion()).andReturn(STACK_VERSION).anyTimes();
    expect(stack.getExcludedConfigurationTypes("service1")).andReturn(Collections.emptySet()).anyTimes();
    expect(stack.getExcludedConfigurationTypes("service2")).andReturn(Collections.emptySet()).anyTimes();

    expect(request.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(request.getClusterId()).andReturn(CLUSTER_ID).anyTimes();
    expect(request.getClusterName()).andReturn(CLUSTER_NAME).anyTimes();
    expect(request.getDescription()).andReturn("Provision Cluster Test").anyTimes();
    expect(request.getConfiguration()).andReturn(topoConfiguration).anyTimes();
    expect(request.getHostGroupInfo()).andReturn(groupInfoMap).anyTimes();
    expect(request.getConfigRecommendationStrategy()).andReturn(ConfigRecommendationStrategy.NEVER_APPLY).anyTimes();
    expect(request.getSecurityConfiguration()).andReturn(null).anyTimes();


    expect(group1.getBlueprintName()).andReturn(BLUEPRINT_NAME).anyTimes();
    expect(group1.getCardinality()).andReturn("test cardinality").anyTimes();
    expect(group1.containsMasterComponent()).andReturn(true).anyTimes();
    expect(group1.getComponents()).andReturn(group1Components).anyTimes();
    expect(group1.getComponents("service1")).andReturn(group1ServiceComponents.get("service1")).anyTimes();
    expect(group1.getComponents("service2")).andReturn(group1ServiceComponents.get("service1")).anyTimes();
    expect(group1.getConfiguration()).andReturn(topoGroup1Config).anyTimes();
    expect(group1.getName()).andReturn("group1").anyTimes();
    expect(group1.getServices()).andReturn(Arrays.asList("service1", "service2")).anyTimes();
    expect(group1.getStack()).andReturn(stack).anyTimes();

    expect(group2.getBlueprintName()).andReturn(BLUEPRINT_NAME).anyTimes();
    expect(group2.getCardinality()).andReturn("test cardinality").anyTimes();
    expect(group2.containsMasterComponent()).andReturn(false).anyTimes();
    expect(group2.getComponents()).andReturn(group2Components).anyTimes();
    expect(group2.getComponents("service1")).andReturn(group2ServiceComponents.get("service1")).anyTimes();
    expect(group2.getComponents("service2")).andReturn(group2ServiceComponents.get("service2")).anyTimes();
    expect(group2.getConfiguration()).andReturn(topoGroup2Config).anyTimes();
    expect(group2.getName()).andReturn("group2").anyTimes();
    expect(group2.getServices()).andReturn(Arrays.asList("service1", "service2")).anyTimes();
    expect(group2.getStack()).andReturn(stack).anyTimes();


    expect(logicalRequestFactory.createRequest(eq(1L), (TopologyRequest) anyObject(), capture(clusterTopologyCapture))).
        andReturn(logicalRequest).anyTimes();
    expect(logicalRequest.getRequestId()).andReturn(1L).anyTimes();
    expect(logicalRequest.getClusterId()).andReturn(CLUSTER_ID).anyTimes();
    expect(logicalRequest.getReservedHosts()).andReturn(Collections.singleton("host1")).anyTimes();
    expect(logicalRequest.getRequestStatus()).andReturn(requestStatusResponse).anyTimes();

    expect(ambariContext.getPersistedTopologyState()).andReturn(persistedState).anyTimes();
    //todo: don't ignore param
    ambariContext.createAmbariResources(isA(ClusterTopology.class), eq(CLUSTER_NAME), (SecurityType) isNull(), (String) isNull(), anyLong());
    expectLastCall().anyTimes();
    expect(ambariContext.getNextRequestId()).andReturn(1L).anyTimes();
    expect(ambariContext.isClusterKerberosEnabled(CLUSTER_ID)).andReturn(false).anyTimes();
    expect(ambariContext.getClusterId(CLUSTER_NAME)).andReturn(CLUSTER_ID).anyTimes();
    expect(ambariContext.getClusterName(CLUSTER_ID)).andReturn(CLUSTER_NAME).anyTimes();
    // cluster configuration task run() isn't executed by mock executor
    // so only INITIAL config
    expect(ambariContext.createConfigurationRequests(capture(configRequestPropertiesCapture))).
        andReturn(Collections.singletonList(configurationRequest)).anyTimes();
    expect(ambariContext.createConfigurationRequests(capture(configRequestPropertiesCapture2))).
        andReturn(Collections.singletonList(configurationRequest2)).anyTimes();
    expect(ambariContext.createConfigurationRequests(capture(configRequestPropertiesCapture3))).
        andReturn(Collections.singletonList(configurationRequest3)).anyTimes();

    ambariContext.setConfigurationOnCluster(capture(updateClusterConfigRequestCapture));
    expectLastCall().anyTimes();
    ambariContext.persistInstallStateForUI(CLUSTER_NAME, STACK_NAME, STACK_VERSION);
    expectLastCall().anyTimes();

    expect(clusterController.ensureResourceProvider(anyObject(Resource.Type.class))).andReturn(resourceProvider);

    expect(configureClusterTaskFactory.createConfigureClusterTask(anyObject(), anyObject(), anyObject())).andReturn(configureClusterTask);
    expect(configureClusterTask.getTimeout()).andReturn(1000L);
    expect(configureClusterTask.getRepeatDelay()).andReturn(50L);
    expect(executor.submit(anyObject(AsyncCallableService.class))).andReturn(mockFuture).anyTimes();

    expect(persistedState.persistTopologyRequest(request)).andReturn(persistedTopologyRequest).anyTimes();
    persistedState.persistLogicalRequest(logicalRequest, 1);
    expectLastCall().anyTimes();

    Class clazz = TopologyManager.class;

    Field f = clazz.getDeclaredField("executor");
    f.setAccessible(true);
    f.set(topologyManager, executor);

    EasyMockSupport.injectMocks(topologyManager);

    Field f2 = clazz.getDeclaredField("executor");
    f2.setAccessible(true);
    f2.set(topologyManagerReplay, executor);

    EasyMockSupport.injectMocks(topologyManagerReplay);

  }

  @After
  public void tearDown() {
    PowerMock.verify(System.class);
    verify(blueprint, stack, request, group1, group2, ambariContext, logicalRequestFactory,
        logicalRequest, configurationRequest, configurationRequest2, configurationRequest3,
        requestStatusResponse, executor, persistedState, clusterTopologyMock, mockFuture, settingDAO, eventPublisher);

    PowerMock.reset(System.class);
    reset(blueprint, stack, request, group1, group2, ambariContext, logicalRequestFactory,
        logicalRequest, configurationRequest, configurationRequest2, configurationRequest3,
        requestStatusResponse, executor, persistedState, clusterTopologyMock, mockFuture, settingDAO, eventPublisher);
  }

  @Test
  public void testProvisionCluster() throws Exception {
    expect(persistedState.getAllRequests()).andReturn(Collections.emptyMap()).anyTimes();
    replayAll();

    topologyManager.provisionCluster(request);
    //todo: assertions
  }

  @Test
  public void testBlueprintProvisioningStateEvent() throws Exception {
    expect(persistedState.getAllRequests()).andReturn(Collections.emptyMap()).anyTimes();
    eventPublisher.publish(anyObject(ClusterProvisionStartedEvent.class));
    expectLastCall().once();
    replayAll();

    topologyManager.provisionCluster(request);
  }

  @Test
  public void testAddKerberosClientAtTopologyInit() throws Exception {
    Map<ClusterTopology, List<LogicalRequest>> allRequests = new HashMap<>();
    List<LogicalRequest> requestList = new ArrayList<>();
    requestList.add(logicalRequest);
    expect(logicalRequest.hasPendingHostRequests()).andReturn(false).anyTimes();
    expect(logicalRequest.isFinished()).andReturn(false).anyTimes();
    allRequests.put(clusterTopologyMock, requestList);
    expect(requestStatusResponse.getTasks()).andReturn(Collections.emptyList()).anyTimes();
    expect(clusterTopologyMock.isClusterKerberosEnabled()).andReturn(true);
    expect(clusterTopologyMock.getClusterId()).andReturn(CLUSTER_ID).anyTimes();
    expect(clusterTopologyMock.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(persistedState.getAllRequests()).andReturn(allRequests).anyTimes();
    expect(persistedState.getProvisionRequest(CLUSTER_ID)).andReturn(logicalRequest).anyTimes();
    expect(ambariContext.isTopologyResolved(CLUSTER_ID)).andReturn(true).anyTimes();
    expect(group1.addComponent("KERBEROS_CLIENT")).andReturn(true).anyTimes();
    expect(group2.addComponent("KERBEROS_CLIENT")).andReturn(true).anyTimes();

    replayAll();

    topologyManager.provisionCluster(request);
    //todo: assertions
  }

  @Test
  public void testBlueprintRequestCompletion() throws Exception {
    List<ShortTaskStatus> tasks = new ArrayList<>();
    ShortTaskStatus t1 = new ShortTaskStatus();
    t1.setStatus(HostRoleStatus.COMPLETED.toString());
    tasks.add(t1);
    ShortTaskStatus t2 = new ShortTaskStatus();
    t2.setStatus(HostRoleStatus.COMPLETED.toString());
    tasks.add(t2);
    ShortTaskStatus t3 = new ShortTaskStatus();
    t3.setStatus(HostRoleStatus.COMPLETED.toString());
    tasks.add(t3);

    expect(requestStatusResponse.getTasks()).andReturn(tasks).anyTimes();
    expect(persistedState.getAllRequests()).andReturn(Collections.emptyMap()).anyTimes();
    expect(persistedState.getProvisionRequest(CLUSTER_ID)).andReturn(logicalRequest).anyTimes();
    expect(logicalRequest.isFinished()).andReturn(true).anyTimes();
    expect(logicalRequest.isSuccessful()).andReturn(true).anyTimes();
    eventPublisher.publish(anyObject(ClusterProvisionedEvent.class));
    expectLastCall().once();
    replayAll();
    topologyManager.provisionCluster(request);
    requestFinished();
    Assert.assertTrue(topologyManager.isClusterProvisionWithBlueprintFinished(CLUSTER_ID));
  }

  @Test
  public void testBlueprintRequestCompletion__Failure() throws Exception {
    List<ShortTaskStatus> tasks = new ArrayList<>();
    ShortTaskStatus t1 = new ShortTaskStatus();
    t1.setStatus(HostRoleStatus.FAILED.toString());
    tasks.add(t1);
    ShortTaskStatus t2 = new ShortTaskStatus();
    t2.setStatus(HostRoleStatus.COMPLETED.toString());
    tasks.add(t2);
    ShortTaskStatus t3 = new ShortTaskStatus();
    t3.setStatus(HostRoleStatus.COMPLETED.toString());
    tasks.add(t3);

    expect(requestStatusResponse.getTasks()).andReturn(tasks).anyTimes();
    expect(persistedState.getAllRequests()).andReturn(Collections.emptyMap()).anyTimes();
    expect(persistedState.getProvisionRequest(CLUSTER_ID)).andReturn(logicalRequest).anyTimes();
    expect(logicalRequest.isFinished()).andReturn(true).anyTimes();
    expect(logicalRequest.isSuccessful()).andReturn(false).anyTimes();
    replayAll();
    topologyManager.provisionCluster(request);
    requestFinished();
    Assert.assertTrue(topologyManager.isClusterProvisionWithBlueprintFinished(CLUSTER_ID));
  }

  @Test
  public void testBlueprintRequestCompletion__InProgress() throws Exception {
    List<ShortTaskStatus> tasks = new ArrayList<>();
    ShortTaskStatus t1 = new ShortTaskStatus();
    t1.setStatus(HostRoleStatus.IN_PROGRESS.toString());
    tasks.add(t1);
    ShortTaskStatus t2 = new ShortTaskStatus();
    t2.setStatus(HostRoleStatus.COMPLETED.toString());
    tasks.add(t2);
    ShortTaskStatus t3 = new ShortTaskStatus();
    t3.setStatus(HostRoleStatus.COMPLETED.toString());
    tasks.add(t3);

    expect(requestStatusResponse.getTasks()).andReturn(tasks).anyTimes();
    expect(persistedState.getAllRequests()).andReturn(Collections.emptyMap()).anyTimes();
    expect(persistedState.getProvisionRequest(CLUSTER_ID)).andReturn(logicalRequest).anyTimes();
    expect(logicalRequest.isFinished()).andReturn(false).anyTimes();
    replayAll();
    topologyManager.provisionCluster(request);
    requestFinished();
    Assert.assertFalse(topologyManager.isClusterProvisionWithBlueprintFinished(CLUSTER_ID));
  }

  @Test
  public void testBlueprintRequestCompletion__NoRequest() throws Exception {
    TopologyManager tm = new TopologyManager();
    tm.onRequestFinished(new RequestFinishedEvent(CLUSTER_ID, 1));
    Assert.assertFalse(tm.isClusterProvisionWithBlueprintTracked(CLUSTER_ID));
    replayAll();
  }

  @Test
  public void testBlueprintRequestCompletion__Replay() throws Exception {
    List<ShortTaskStatus> tasks = new ArrayList<>();
    ShortTaskStatus t1 = new ShortTaskStatus();
    t1.setStatus(HostRoleStatus.COMPLETED.toString());
    tasks.add(t1);
    ShortTaskStatus t2 = new ShortTaskStatus();
    t2.setStatus(HostRoleStatus.COMPLETED.toString());
    tasks.add(t2);
    ShortTaskStatus t3 = new ShortTaskStatus();
    t3.setStatus(HostRoleStatus.COMPLETED.toString());
    tasks.add(t3);

    Map<ClusterTopology,List<LogicalRequest>> allRequests = new HashMap<>();
    List<LogicalRequest> logicalRequests = new ArrayList<>();
    logicalRequests.add(logicalRequest);
    ClusterTopology clusterTopologyMock = EasyMock.createNiceMock(ClusterTopology.class);
    expect(clusterTopologyMock.getClusterId()).andReturn(CLUSTER_ID).anyTimes();

    expect(ambariContext.isTopologyResolved(EasyMock.anyLong())).andReturn(true).anyTimes();

    allRequests.put(clusterTopologyMock, logicalRequests);
    expect(persistedState.getAllRequests()).andReturn(allRequests).anyTimes();
    expect(persistedState.getProvisionRequest(CLUSTER_ID)).andReturn(logicalRequest).anyTimes();
    expect(logicalRequest.hasPendingHostRequests()).andReturn(true).anyTimes();
    expect(logicalRequest.getCompletedHostRequests()).andReturn(Collections.EMPTY_LIST).anyTimes();
    expect(logicalRequest.isFinished()).andReturn(true).anyTimes();
    expect(requestStatusResponse.getTasks()).andReturn(tasks).anyTimes();
    replayAll();
    EasyMock.replay(clusterTopologyMock);
    topologyManagerReplay.getRequest(1L); // calling ensureInitialized indirectly
    Assert.assertTrue(topologyManagerReplay.isClusterProvisionWithBlueprintFinished(CLUSTER_ID));
  }

  private void requestFinished() {
    topologyManager.onRequestFinished(new RequestFinishedEvent(CLUSTER_ID, 1));
  }

  private void replayAll() {
    replay(blueprint, stack, request, group1, group2, ambariContext, logicalRequestFactory,
            configurationRequest, configurationRequest2, configurationRequest3, executor,
            persistedState, clusterTopologyMock, securityConfigurationFactory, credentialStoreService,
            clusterController, resourceProvider, mockFuture, requestStatusResponse, logicalRequest, settingDAO,
            configureClusterTaskFactory, configureClusterTask, eventPublisher);
  }

  @Test(expected = InvalidTopologyException.class)
  public void testScaleHosts__alreadyExistingHost() throws InvalidTopologyTemplateException, InvalidTopologyException, AmbariException, NoSuchStackException {
    HashSet<Map<String, Object>> propertySet = new HashSet<>();
    Map<String,Object> properties = new TreeMap<>();
    properties.put(HostResourceProvider.HOST_HOST_NAME_PROPERTY_ID, "host1");
    properties.put(HostResourceProvider.HOST_GROUP_PROPERTY_ID, "group1");
    properties.put(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID, CLUSTER_NAME);
    properties.put(HostResourceProvider.BLUEPRINT_PROPERTY_ID, BLUEPRINT_NAME);
    propertySet.add(properties);
    BlueprintFactory bpfMock = EasyMock.createNiceMock(BlueprintFactory.class);
    EasyMock.expect(bpfMock.getBlueprint(BLUEPRINT_NAME)).andReturn(blueprint).anyTimes();
    ScaleClusterRequest.init(bpfMock);
    replay(bpfMock);
    expect(persistedState.getAllRequests()).andReturn(Collections.emptyMap()).anyTimes();
    replayAll();
    topologyManager.provisionCluster(request);
    topologyManager.scaleHosts(new ScaleClusterRequest(propertySet));
    Assert.fail("InvalidTopologyException should have been thrown");
  }

  @Test
  public void testProvisionCluster_QuickLinkProfileIsSavedTheFirstTime() throws Exception {
    expect(persistedState.getAllRequests()).andReturn(Collections.emptyMap()).anyTimes();

    // request has a quicklinks profile
    expect(request.getQuickLinksProfileJson()).andReturn(SAMPLE_QUICKLINKS_PROFILE_1).anyTimes();

    // this means no quicklinks profile exists before calling provisionCluster()
    expect(settingDAO.findByName(QuickLinksProfile.SETTING_NAME_QUICKLINKS_PROFILE)).andReturn(null);

    // expect that settingsDao saves the quick links profile with the right content
    final long timeStamp = System.currentTimeMillis();
    mockStatic(System.class);
    expect(System.currentTimeMillis()).andReturn(timeStamp);
    PowerMock.replay(System.class);
    final SettingEntity quickLinksProfile = createQuickLinksSettingEntity(SAMPLE_QUICKLINKS_PROFILE_1, timeStamp);
    settingDAO.create(eq(quickLinksProfile));

    replayAll();

    topologyManager.provisionCluster(request);
  }

  @Test
  public void testProvisionCluster_ExistingQuickLinkProfileIsOverwritten() throws Exception {
    expect(persistedState.getAllRequests()).andReturn(Collections.emptyMap()).anyTimes();

    // request has a quicklinks profile
    expect(request.getQuickLinksProfileJson()).andReturn(SAMPLE_QUICKLINKS_PROFILE_2).anyTimes();

    // existing quick links profile returned by dao
    final long timeStamp1 = System.currentTimeMillis();
    SettingEntity originalProfile = createQuickLinksSettingEntity(SAMPLE_QUICKLINKS_PROFILE_1, timeStamp1);
    expect(settingDAO.findByName(QuickLinksProfile.SETTING_NAME_QUICKLINKS_PROFILE)).andReturn(originalProfile);

    // expect that settingsDao overwrites the quick links profile with the new content
    mockStatic(System.class);
    final long timeStamp2 = timeStamp1 + 100;
    expect(System.currentTimeMillis()).andReturn(timeStamp2);
    PowerMock.replay(System.class);
    final SettingEntity newProfile = createQuickLinksSettingEntity(SAMPLE_QUICKLINKS_PROFILE_2, timeStamp2);
    expect(settingDAO.merge(newProfile)).andReturn(newProfile);

    replayAll();

    topologyManager.provisionCluster(request);
  }

  private SettingEntity createQuickLinksSettingEntity(String content, long timeStamp) {
    SettingEntity settingEntity = new SettingEntity();
    settingEntity.setName(QuickLinksProfile.SETTING_NAME_QUICKLINKS_PROFILE);
    settingEntity.setSettingType(QuickLinksProfile.SETTING_TYPE_AMBARI_SERVER);
    settingEntity.setContent(content);
    settingEntity.setUpdatedBy(AuthorizationHelper.getAuthenticatedName());
    settingEntity.setUpdateTimestamp(timeStamp);
    return settingEntity;
  }
}
