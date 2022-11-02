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
package org.apache.ambari.server.stack.upgrade.orchestrate;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.agent.stomp.AgentConfigsHolder;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.KerberosDetails;
import org.apache.ambari.server.controller.internal.DeleteHostComponentStatusMetaData;
import org.apache.ambari.server.controller.internal.UpgradeResourceProvider;
import org.apache.ambari.server.events.AgentConfigsUpdateEvent;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.ServiceConfigDAO;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.ServiceConfigEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.serveraction.kerberos.KDCType;
import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.stack.MasterHostResolver;
import org.apache.ambari.server.stack.StackManagerMock;
import org.apache.ambari.server.stack.upgrade.ConfigUpgradeChangeDefinition;
import org.apache.ambari.server.stack.upgrade.ConfigUpgradeChangeDefinition.ConfigurationKeyValue;
import org.apache.ambari.server.stack.upgrade.ConfigUpgradePack;
import org.apache.ambari.server.stack.upgrade.ConfigureTask;
import org.apache.ambari.server.stack.upgrade.Direction;
import org.apache.ambari.server.stack.upgrade.ExecuteTask;
import org.apache.ambari.server.stack.upgrade.Grouping;
import org.apache.ambari.server.stack.upgrade.HostOrderGrouping;
import org.apache.ambari.server.stack.upgrade.HostOrderItem;
import org.apache.ambari.server.stack.upgrade.HostOrderItem.HostOrderActionType;
import org.apache.ambari.server.stack.upgrade.ManualTask;
import org.apache.ambari.server.stack.upgrade.SecurityCondition;
import org.apache.ambari.server.stack.upgrade.ServiceCheckGrouping;
import org.apache.ambari.server.stack.upgrade.StopGrouping;
import org.apache.ambari.server.stack.upgrade.Task;
import org.apache.ambari.server.stack.upgrade.UpgradePack;
import org.apache.ambari.server.stack.upgrade.UpgradeScope;
import org.apache.ambari.server.stageplanner.RoleGraphFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.UpgradeState;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.ambari.spi.ClusterInformation;
import org.apache.ambari.spi.RepositoryType;
import org.apache.ambari.spi.upgrade.OrchestrationOptions;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.apache.commons.io.FileUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.util.Modules;

/**
 * Tests the {@link UpgradeHelper} class
 */
public class UpgradeHelperTest extends EasyMockSupport {

  private static final StackId STACK_ID_HDP_211 = new StackId("HDP-2.1.1");
  private static final StackId STACK_ID_HDP_220 = new StackId("HDP-2.2.0");
  private static final String UPGRADE_VERSION = "2.2.1.0-1234";
  private static final String DOWNGRADE_VERSION = "2.2.0.0-1234";

  private Injector injector;
  private AmbariMetaInfo ambariMetaInfo;
  private StackManagerMock stackManagerMock;
  private OrmTestHelper helper;
  private MasterHostResolver m_masterHostResolver;
  private UpgradeHelper m_upgradeHelper;
  private ConfigHelper m_configHelper;
  private AmbariManagementController m_managementController;
  private Gson m_gson = new Gson();

  private RepositoryVersionEntity repositoryVersion2110;
  private RepositoryVersionEntity repositoryVersion2200;
  private RepositoryVersionEntity repositoryVersion2210;
  private HostsType namenodeHosts = HostsType.highAvailability("h1", "h2", newLinkedHashSet(Arrays.asList("h1", "h2")));

  @Rule
  public TemporaryFolder tmpFolder = new TemporaryFolder();

  /**
   * Because test cases need to share config mocks, put common ones in this function.
   * @throws Exception
   */
  private void setConfigMocks() throws Exception {
    // configure the mock to return data given a specific placeholder
    m_configHelper = EasyMock.createNiceMock(ConfigHelper.class);
    expect(m_configHelper.getPlaceholderValueFromDesiredConfigurations(
        EasyMock.anyObject(Cluster.class), EasyMock.eq("{{foo/bar}}"))).andReturn("placeholder-rendered-properly").anyTimes();
    expect(m_configHelper.getEffectiveDesiredTags(
        EasyMock.anyObject(Cluster.class), EasyMock.anyObject(String.class))).andReturn(new HashMap<>()).anyTimes();
    expect(m_configHelper.getHostActualConfigs(
        EasyMock.anyLong())).andReturn(new AgentConfigsUpdateEvent(null, Collections.emptySortedMap())).anyTimes();
    expect(m_configHelper.getChangedConfigTypes(anyObject(Cluster.class), anyObject(ServiceConfigEntity.class),
        anyLong(), anyLong(), anyString())).andReturn(Collections.emptyMap()).anyTimes();
  }

  @Before
  public void before() throws Exception {
    setConfigMocks();
    // Most test cases can replay the common config mocks. If any test case needs custom ones, it can re-initialize m_configHelper;
    replay(m_configHelper);

    final InMemoryDefaultTestModule injectorModule = new InMemoryDefaultTestModule() {
      @Override
      protected void configure() {
        super.configure();
      }
    };

    MockModule mockModule = new MockModule();

    // create an injector which will inject the mocks
    injector = Guice.createInjector(Modules.override(injectorModule).with(mockModule));
    H2DatabaseCleaner.resetSequences(injector);
    injector.getInstance(GuiceJpaInitializer.class);

    EventBusSynchronizer.synchronizeAmbariEventPublisher(injector);
    EventBusSynchronizer.synchronizeAlertEventPublisher(injector);

    helper = injector.getInstance(OrmTestHelper.class);
    ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    stackManagerMock = (StackManagerMock) ambariMetaInfo.getStackManager();
    m_upgradeHelper = injector.getInstance(UpgradeHelper.class);
    m_masterHostResolver = EasyMock.createMock(MasterHostResolver.class);
    m_managementController = injector.getInstance(AmbariManagementController.class);

    repositoryVersion2110 = helper.getOrCreateRepositoryVersion(STACK_ID_HDP_211, "2.1.1.0-1234");
    repositoryVersion2200 = helper.getOrCreateRepositoryVersion(STACK_ID_HDP_220, DOWNGRADE_VERSION);
    repositoryVersion2210 = helper.getOrCreateRepositoryVersion(STACK_ID_HDP_220, UPGRADE_VERSION);

    // Set the authenticated user
    // TODO: remove this or replace the authenticated user to test authorization rules
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);

    // Clear the authenticated user
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testSuggestUpgradePack() throws Exception{
    final String clusterName = "c1";
    final StackId sourceStackId = new StackId("HDP", "2.1.1");
    final StackId targetStackId = new StackId("HDP", "2.2.0");
    final Direction upgradeDirection = Direction.UPGRADE;
    final UpgradeType upgradeType = UpgradeType.ROLLING;

    makeCluster();
    try {
      String preferredUpgradePackName = "upgrade_test";
      UpgradePack up = m_upgradeHelper.suggestUpgradePack(clusterName, sourceStackId, targetStackId, upgradeDirection, upgradeType, preferredUpgradePackName);
      assertEquals(upgradeType, up.getType());
    } catch (AmbariException e){
      assertTrue(false);
    }
  }

  @Test
  public void testSuggestUpgradePackFromSourceStack() throws Exception {
    final String clusterName = "c1";
    final StackId sourceStackId = new StackId("HDP", "2.1.1");
    final StackId targetStackId = new StackId("HDP", "2.2.0");
    final Direction upgradeDirection = Direction.UPGRADE;
    final UpgradeType upgradeType = UpgradeType.ROLLING;

    makeCluster();
    try {
      UpgradePack up = m_upgradeHelper.suggestUpgradePack(clusterName, sourceStackId, targetStackId, upgradeDirection, upgradeType, null);
      assertEquals(upgradeType, up.getType());
    } catch (AmbariException e) {
      e.printStackTrace();
      fail("unexpected exception suggesting upgrade pack");
    }
  }

  @Test
  public void testUpgradeOrchestration() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("foo", "bar");
    assertTrue(upgrades.isEmpty());

    upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");

    // set the display names of the service and component in the target stack
    // to make sure that we can correctly render display strings during the
    // upgrade
    ServiceInfo si = ambariMetaInfo.getService("HDP", "2.2.0", "ZOOKEEPER");
    si.setDisplayName("Zk");

    ComponentInfo ci = si.getComponentByName("ZOOKEEPER_SERVER");
    ci.setDisplayName("ZooKeeper1 Server2");

    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = getMockUpgradeContext(cluster, Direction.UPGRADE, UpgradeType.ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(7, groups.size());

    assertEquals("PRE_CLUSTER", groups.get(0).name);
    assertEquals("ZOOKEEPER", groups.get(1).name);
    assertEquals("CORE_MASTER", groups.get(2).name);
    assertEquals("CORE_SLAVES", groups.get(3).name);
    assertEquals("HIVE", groups.get(4).name);
    assertEquals("OOZIE", groups.get(5).name);

    UpgradeGroupHolder holder = groups.get(2);
    boolean found = false;
    for (StageWrapper sw : holder.items) {
      if (sw.getTasksJson().contains("Upgrading your database")) {
        found = true;
      }
    }
    assertTrue("Expected to find replaced text for Upgrading", found);

    UpgradeGroupHolder group = groups.get(1);
    // check that the display name is being used
    assertTrue(group.items.get(1).getText().contains("ZooKeeper1 Server2"));
    assertEquals(group.items.get(5).getText(), "Service Check Zk");

    group = groups.get(3);
    assertEquals(8, group.items.size());
    StageWrapper sw = group.items.get(3);
    assertEquals("Validate Partial Upgrade", sw.getText());
    assertEquals(1, sw.getTasks().size());
    assertEquals(1, sw.getTasks().get(0).getTasks().size());
    Task t = sw.getTasks().get(0).getTasks().get(0);
    assertEquals(ManualTask.class, t.getClass());
    ManualTask mt = (ManualTask) t;
    assertTrue(mt.messages.get(0).contains("DataNode and NodeManager"));
    assertNotNull(mt.structuredOut);
    assertTrue(mt.structuredOut.contains("DATANODE"));
    assertTrue(mt.structuredOut.contains("NODEMANAGER"));

    UpgradeGroupHolder postGroup = groups.get(6);
    assertEquals("POST_CLUSTER", postGroup.name);
    assertEquals("Finalize Upgrade", postGroup.title);
    assertEquals(3, postGroup.items.size());
    assertEquals("Confirm Finalize", postGroup.items.get(0).getText());
    assertEquals("Execute HDFS Finalize", postGroup.items.get(1).getText());
    assertEquals("Save Cluster State", postGroup.items.get(2).getText());
    assertEquals(StageWrapper.Type.SERVER_SIDE_ACTION, postGroup.items.get(2).getType());

    assertEquals(4, groups.get(0).items.size());
    assertEquals(6, groups.get(1).items.size());
    assertEquals(9, groups.get(2).items.size());
    assertEquals(8, groups.get(3).items.size());

    // Do stacks cleanup
    stackManagerMock.invalidateCurrentPaths();
    ambariMetaInfo.init();
  }

  @Experimental(feature=ExperimentalFeature.PATCH_UPGRADES)
  @Test
  public void testPartialUpgradeOrchestration() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("foo", "bar");
    assertTrue(upgrades.isEmpty());

    upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");

    // set the display names of the service and component in the target stack
    // to make sure that we can correctly render display strings during the
    // upgrade
    ServiceInfo si = ambariMetaInfo.getService("HDP", "2.2.0", "ZOOKEEPER");
    si.setDisplayName("Zk");

    ComponentInfo ci = si.getComponentByName("ZOOKEEPER_SERVER");
    ci.setDisplayName("ZooKeeper1 Server2");

    assertTrue(upgrades.containsKey("upgrade_test_partial"));
    UpgradePack upgrade = upgrades.get("upgrade_test_partial");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    Set<String> services = Collections.singleton("ZOOKEEPER");

    UpgradeContext context = getMockUpgradeContext(cluster, Direction.UPGRADE, UpgradeType.ROLLING,
        repositoryVersion2210, RepositoryType.PATCH, services);

    List<Grouping> groupings = upgrade.getGroups(Direction.UPGRADE);
    assertEquals(8, groupings.size());
    assertEquals(UpgradeScope.COMPLETE, groupings.get(6).scope);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(3, groups.size());

    assertEquals("PRE_CLUSTER", groups.get(0).name);
    assertEquals("ZOOKEEPER", groups.get(1).name);
    assertEquals("POST_CLUSTER", groups.get(2).name);

    UpgradeGroupHolder group = groups.get(1);
    // check that the display name is being used
    assertTrue(group.items.get(1).getText().contains("ZooKeeper1 Server2"));
    assertEquals("Service Check Zk", group.items.get(6).getText());

    UpgradeGroupHolder postGroup = groups.get(2);
    assertEquals("POST_CLUSTER", postGroup.name);
    assertEquals("Finalize Upgrade", postGroup.title);
    assertEquals(2, postGroup.items.size());
    assertEquals("Confirm Finalize", postGroup.items.get(0).getText());
    assertEquals("Save Cluster State", postGroup.items.get(1).getText());
    assertEquals(StageWrapper.Type.SERVER_SIDE_ACTION, postGroup.items.get(1).getType());

    assertEquals(2, groups.get(0).items.size());
    assertEquals(7, groups.get(1).items.size());
    assertEquals(2, groups.get(2).items.size());

    // Do stacks cleanup
    stackManagerMock.invalidateCurrentPaths();
    ambariMetaInfo.init();
  }

  @Test
  public void testCompleteUpgradeOrchestration() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("foo", "bar");
    assertTrue(upgrades.isEmpty());

    upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");

    // set the display names of the service and component in the target stack
    // to make sure that we can correctly render display strings during the
    // upgrade
    ServiceInfo si = ambariMetaInfo.getService("HDP", "2.2.0", "ZOOKEEPER");
    si.setDisplayName("Zk");

    ComponentInfo ci = si.getComponentByName("ZOOKEEPER_SERVER");
    ci.setDisplayName("ZooKeeper1 Server2");

    assertTrue(upgrades.containsKey("upgrade_test_partial"));
    UpgradePack upgrade = upgrades.get("upgrade_test_partial");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = getMockUpgradeContext(cluster, Direction.UPGRADE, UpgradeType.ROLLING,
        repositoryVersion2210, RepositoryType.STANDARD, Collections.singleton("ZOOKEEPER"));

    List<Grouping> groupings = upgrade.getGroups(Direction.UPGRADE);
    assertEquals(8, groupings.size());
    assertEquals(UpgradeScope.COMPLETE, groupings.get(6).scope);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(4, groups.size());

    assertEquals("PRE_CLUSTER", groups.get(0).name);
    assertEquals("ZOOKEEPER", groups.get(1).name);
    assertEquals("ALL_HOSTS", groups.get(2).name);
    assertEquals("POST_CLUSTER", groups.get(3).name);

    UpgradeGroupHolder group = groups.get(1);
    // check that the display name is being used
    assertTrue(group.items.get(1).getText().contains("ZooKeeper1 Server2"));
    assertEquals("Service Check Zk", group.items.get(5).getText());

    UpgradeGroupHolder postGroup = groups.get(3);
    assertEquals("POST_CLUSTER", postGroup.name);
    assertEquals("Finalize Upgrade", postGroup.title);
    assertEquals(2, postGroup.items.size());
    assertEquals("Confirm Finalize", postGroup.items.get(0).getText());
    assertEquals("Save Cluster State", postGroup.items.get(1).getText());
    assertEquals(StageWrapper.Type.SERVER_SIDE_ACTION, postGroup.items.get(1).getType());

    assertEquals(2, groups.get(0).items.size());
    assertEquals(6, groups.get(1).items.size());
    assertEquals(1, groups.get(2).items.size());

    // Do stacks cleanup
    stackManagerMock.invalidateCurrentPaths();
    ambariMetaInfo.init();
  }

  @Test
  public void testUpgradeServerActionOrchestration() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");

    ServiceInfo si = ambariMetaInfo.getService("HDP", "2.1.1", "ZOOKEEPER");
    si.setDisplayName("Zk");
    ComponentInfo ci = si.getComponentByName("ZOOKEEPER_SERVER");
    ci.setDisplayName("ZooKeeper1 Server2");

    assertTrue(upgrades.containsKey("upgrade_server_action_test"));
    UpgradePack upgrade = upgrades.get("upgrade_server_action_test");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = getMockUpgradeContext(cluster, Direction.UPGRADE, UpgradeType.ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(1, groups.size());
    UpgradeGroupHolder group = groups.get(0);
    assertEquals("CLUSTER_SERVER_ACTIONS", group.name);
    List<StageWrapper> stageWrappers = group.items;
    assertEquals(6, stageWrappers.size());
    assertEquals("Pre Upgrade", stageWrappers.get(0).getText());
    assertEquals("Pre Upgrade Zookeeper", stageWrappers.get(1).getText());
    assertEquals("Configuring", stageWrappers.get(2).getText());
    assertEquals("Configuring HDFS", stageWrappers.get(3).getText());
    assertEquals("Calculating Properties", stageWrappers.get(4).getText());
    assertEquals("Calculating HDFS Properties", stageWrappers.get(5).getText());

    // Do stacks cleanup
    stackManagerMock.invalidateCurrentPaths();
    ambariMetaInfo.init();
  }

  /**
   * Tests that hosts in MM are not included in the upgrade.
   *
   * @throws Exception
   */
  @Test
  public void testUpgradeOrchestrationWithHostsInMM() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("foo", "bar");
    assertTrue(upgrades.isEmpty());

    upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");

    ServiceInfo si = ambariMetaInfo.getService("HDP", "2.1.1", "ZOOKEEPER");
    si.setDisplayName("Zk");

    ComponentInfo ci = si.getComponentByName("ZOOKEEPER_SERVER");
    ci.setDisplayName("ZooKeeper1 Server2");

    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    assertNotNull(upgrade);

    // turn on MM for the first host
    Cluster cluster = makeCluster();
    Host hostInMaintenanceMode = cluster.getHosts().iterator().next();
    hostInMaintenanceMode.setMaintenanceState(cluster.getClusterId(), MaintenanceState.ON);

    UpgradeContext context = getMockUpgradeContextNoReplay(cluster, Direction.UPGRADE,
        UpgradeType.ROLLING, repositoryVersion2210);

    // use a "real" master host resolver here so that we can actually test MM
    MasterHostResolver masterHostResolver = new MasterHostResolver(cluster, m_configHelper, context);
    expect(context.getResolver()).andReturn(masterHostResolver).anyTimes();
    replay(context);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);
    assertEquals(7, groups.size());

    for (UpgradeGroupHolder group : groups) {
      for (StageWrapper stageWrapper : group.items) {
        Set<String> hosts = stageWrapper.getHosts();
        assertFalse(hosts.contains(hostInMaintenanceMode.getHostName()));
      }
    }

    // Do stacks cleanup
    stackManagerMock.invalidateCurrentPaths();
    ambariMetaInfo.init();
  }

  /**
   * Verify that a Rolling Upgrades restarts the NameNodes in the following order: standby, active.
   * @throws Exception
   */
  @Test
  public void testNamenodeOrder() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = getMockUpgradeContext(cluster, Direction.UPGRADE, UpgradeType.ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(7, groups.size());

    UpgradeGroupHolder mastersGroup = groups.get(2);
    assertEquals("CORE_MASTER", mastersGroup.name);

    List<String> orderedNameNodes = new LinkedList<>();
    for (StageWrapper sw : mastersGroup.items) {
      if (sw.getType().equals(StageWrapper.Type.RESTART) && sw.getText().toLowerCase().contains("NameNode".toLowerCase())) {
        for (TaskWrapper tw : sw.getTasks()) {
          for (String hostName : tw.getHosts()) {
            orderedNameNodes.add(hostName);
          }
        }
      }
    }

    assertEquals(2, orderedNameNodes.size());
    // Order is standby, then active.
    assertEquals("h2", orderedNameNodes.get(0));
    assertEquals("h1", orderedNameNodes.get(1));
  }

  @Test
  public void testNamenodeFederationOrder() throws Exception {
    namenodeHosts = HostsType.federated(
      Arrays.asList(
        new HostsType.HighAvailabilityHosts("h1", Arrays.asList("h2", "h3")),
        new HostsType.HighAvailabilityHosts("h4", singletonList("h5"))),
      newLinkedHashSet(Arrays.asList("h1", "h2", "h3", "h4", "h5")));

    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = getMockUpgradeContext(cluster, Direction.UPGRADE, UpgradeType.ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(7, groups.size());

    UpgradeGroupHolder mastersGroup = groups.get(2);
    assertEquals("CORE_MASTER", mastersGroup.name);

    List<String> orderedNameNodes = new LinkedList<>();
    for (StageWrapper sw : mastersGroup.items) {
      if (sw.getType().equals(StageWrapper.Type.RESTART) && sw.getText().toLowerCase().contains("NameNode".toLowerCase())) {
        for (TaskWrapper tw : sw.getTasks()) {
          for (String hostName : tw.getHosts()) {
            orderedNameNodes.add(hostName);
          }
        }
      }
    }
    assertEquals(Arrays.asList("h2", "h3", "h1", "h5", "h4"), orderedNameNodes);
  }

  @Test
  public void testUpgradeOrchestrationWithNoHeartbeat() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("foo", "bar");
    assertTrue(upgrades.isEmpty());

    upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster(false);

    Clusters clusters = injector.getInstance(Clusters.class);
    Host h4 = clusters.getHost("h4");
    h4.setState(HostState.HEARTBEAT_LOST);

    List<ServiceComponentHost> schs = cluster.getServiceComponentHosts("h4");
    assertEquals(1, schs.size());
    assertEquals(HostState.HEARTBEAT_LOST, schs.get(0).getHostState());

    UpgradeContext context = getMockUpgradeContext(cluster, Direction.UPGRADE, UpgradeType.ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(7, groups.size());

    assertEquals("PRE_CLUSTER", groups.get(0).name);
    assertEquals("ZOOKEEPER", groups.get(1).name);
    assertEquals("CORE_MASTER", groups.get(2).name);
    assertEquals("CORE_SLAVES", groups.get(3).name);
    assertEquals("HIVE", groups.get(4).name);
    assertEquals("OOZIE", groups.get(5).name);

    UpgradeGroupHolder postGroup = groups.get(6);
    assertEquals("POST_CLUSTER", postGroup.name);
    assertEquals("Finalize Upgrade", postGroup.title);
    assertEquals(3, postGroup.items.size());
    assertEquals("Confirm Finalize", postGroup.items.get(0).getText());
    assertEquals("Execute HDFS Finalize", postGroup.items.get(1).getText());
    assertEquals("Save Cluster State", postGroup.items.get(2).getText());
    assertEquals(StageWrapper.Type.SERVER_SIDE_ACTION, postGroup.items.get(2).getType());

    assertEquals(6, groups.get(1).items.size());
    assertEquals(9, groups.get(2).items.size());
    assertEquals(7, groups.get(3).items.size());
  }

  @Test
  public void testDowngradeOrchestration() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = getMockUpgradeContext(cluster, Direction.DOWNGRADE,
        UpgradeType.ROLLING, repositoryVersion2200);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(7, groups.size());

    assertEquals("PRE_CLUSTER", groups.get(0).name);
    assertEquals("OOZIE", groups.get(1).name);
    assertEquals("HIVE", groups.get(2).name);
    assertEquals("CORE_SLAVES", groups.get(3).name);
    assertEquals("CORE_MASTER", groups.get(4).name);
    assertEquals("ZOOKEEPER", groups.get(5).name);


    UpgradeGroupHolder postGroup = groups.get(6);
    assertEquals("POST_CLUSTER", postGroup.name);
    assertEquals("Finalize Downgrade", postGroup.title);
    assertEquals(3, postGroup.items.size());
    assertEquals("Confirm Finalize", postGroup.items.get(0).getText());
    assertEquals("Execute HDFS Finalize", postGroup.items.get(1).getText());
    assertEquals("Save Cluster State", postGroup.items.get(2).getText());
    assertEquals(StageWrapper.Type.SERVER_SIDE_ACTION, postGroup.items.get(2).getType());

    assertEquals(4, groups.get(0).items.size());
    assertEquals(8, groups.get(1).items.size());
    assertEquals(6, groups.get(2).items.size());
    assertEquals(6, groups.get(3).items.size());
    assertEquals(8, groups.get(4).items.size());
  }

  @Test
  public void testBuckets() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_bucket_test"));
    UpgradePack upgrade = upgrades.get("upgrade_bucket_test");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = getMockUpgradeContext(cluster, Direction.UPGRADE, UpgradeType.ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(1, groups.size());
    UpgradeGroupHolder group = groups.iterator().next();

    // Pre:
    //   Manual task = 1
    //   2x - Execute task on all 3 = 6

    // Post:
    //   Execute task on all 3 = 3
    //   2x - Manual task = 2
    //   3x - Execute task on all 3 = 9

    // Service Check = 1
    assertEquals(22, group.items.size());
  }

  @Test
  public void testManualTaskPostProcessing() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();
    UpgradeContext context = getMockUpgradeContext(cluster, Direction.UPGRADE, UpgradeType.ROLLING);
    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(7, groups.size());

    // grab the manual task out of ZK which has placeholder text
    UpgradeGroupHolder zookeeperGroup = groups.get(1);
    assertEquals("ZOOKEEPER", zookeeperGroup.name);
    ManualTask manualTask = (ManualTask) zookeeperGroup.items.get(0).getTasks().get(
        0).getTasks().get(0);

    assertEquals(1, manualTask.messages.size());
    assertEquals("This is a manual task with a placeholder of placeholder-rendered-properly",
        manualTask.messages.get(0));
  }

  @Test
  public void testConditionalDeleteTask() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    ConfigUpgradePack cup = ambariMetaInfo.getConfigUpgradePack("HDP", "2.1.1");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = getMockUpgradeContext(cluster, Direction.UPGRADE, UpgradeType.ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(7, groups.size());

    // grab the configure task out of Hive
    UpgradeGroupHolder hiveGroup = groups.get(4);
    assertEquals("HIVE", hiveGroup.name);
    ConfigureTask configureTask = (ConfigureTask) hiveGroup.items.get(1).getTasks().get(0).getTasks().get(0);

    // now change the thrift port to http to have the 2nd condition invoked
    Map<String, String> hiveConfigs = new HashMap<>();
    hiveConfigs.put("hive.server2.transport.mode", "http");
    hiveConfigs.put("hive.server2.thrift.port", "10001");
    hiveConfigs.put("condition", "1");

    ConfigurationRequest configurationRequest = new ConfigurationRequest();
    configurationRequest.setClusterName(cluster.getClusterName());
    configurationRequest.setType("hive-site");
    configurationRequest.setVersionTag("version2");
    configurationRequest.setProperties(hiveConfigs);

    final ClusterRequest clusterRequest = new ClusterRequest(
        cluster.getClusterId(), cluster.getClusterName(),
        cluster.getDesiredStackVersion().getStackVersion(), null);

    clusterRequest.setDesiredConfig(singletonList(configurationRequest));
    m_managementController.updateClusters(new HashSet<ClusterRequest>() {
      {
        add(clusterRequest);
      }
    }, null);

    Map<String, String> configProperties = configureTask.getConfigurationChanges(cluster, cup);
    assertFalse(configProperties.isEmpty());
    assertEquals(configProperties.get(ConfigureTask.PARAMETER_CONFIG_TYPE), "hive-site");

    String configurationJson = configProperties.get(ConfigureTask.PARAMETER_TRANSFERS);
    assertNotNull(configurationJson);

    List<ConfigUpgradeChangeDefinition.Transfer> transfers = m_gson.fromJson(configurationJson,
            new TypeToken<List<ConfigUpgradeChangeDefinition.Transfer>>() { }.getType());

    assertEquals(6, transfers.size());
    assertEquals("copy-key", transfers.get(0).fromKey);
    assertEquals("copy-key-to", transfers.get(0).toKey);

    assertEquals("move-key", transfers.get(1).fromKey);
    assertEquals("move-key-to", transfers.get(1).toKey);

    assertEquals("delete-key", transfers.get(2).deleteKey);
    assertEquals("delete-http-1", transfers.get(3).deleteKey);
    assertEquals("delete-http-2", transfers.get(4).deleteKey);
    assertEquals("delete-http-3", transfers.get(5).deleteKey);


  }

  @Test
  public void testConfigTaskConditionMet() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    ConfigUpgradePack cup = ambariMetaInfo.getConfigUpgradePack("HDP", "2.1.1");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = getMockUpgradeContext(cluster, Direction.UPGRADE, UpgradeType.ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade,
        context);

    assertEquals(7, groups.size());

    // grab the configure task out of Hive
    UpgradeGroupHolder hiveGroup = groups.get(4);
    assertEquals("HIVE", hiveGroup.name);

    //Condition is met
    ConfigureTask configureTask = (ConfigureTask) hiveGroup.items.get(2).getTasks().get(
        0).getTasks().get(0);
    Map<String, String> configProperties = configureTask.getConfigurationChanges(cluster, cup);

    assertFalse(configProperties.isEmpty());
    assertEquals(configProperties.get(ConfigureTask.PARAMETER_CONFIG_TYPE), "hive-site");

    assertTrue(configProperties.containsKey(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS));
    assertTrue(configProperties.containsKey(ConfigureTask.PARAMETER_REPLACEMENTS));
    assertTrue(configProperties.containsKey(ConfigureTask.PARAMETER_TRANSFERS));

    String configurationJson = configProperties.get(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS);
    String transferJson = configProperties.get(ConfigureTask.PARAMETER_TRANSFERS);
    String replacementJson = configProperties.get(ConfigureTask.PARAMETER_REPLACEMENTS);
    assertNotNull(configurationJson);
    assertNotNull(transferJson);
    assertNotNull(replacementJson);

    //if conditions for sets...
    List<ConfigUpgradeChangeDefinition.ConfigurationKeyValue> keyValuePairs = m_gson.fromJson(configurationJson,
        new TypeToken<List<ConfigUpgradeChangeDefinition.ConfigurationKeyValue>>() {
        }.getType());
    assertEquals("setKeyOne", keyValuePairs.get(0).key);
    assertEquals("1", keyValuePairs.get(0).value);

    assertEquals("setKeyTwo", keyValuePairs.get(1).key);
    assertEquals("2", keyValuePairs.get(1).value);

    assertEquals("setKeyThree", keyValuePairs.get(2).key);
    assertEquals("3", keyValuePairs.get(2).value);

    assertEquals("setKeyFour", keyValuePairs.get(3).key);
    assertEquals("4", keyValuePairs.get(3).value);

    //if conditions for transfer
    List<ConfigUpgradeChangeDefinition.Transfer> transfers = m_gson.fromJson(transferJson,
        new TypeToken<List<ConfigUpgradeChangeDefinition.Transfer>>() {
        }.getType());

    assertEquals("copy-key-one", transfers.get(0).fromKey);
    assertEquals("copy-to-key-one", transfers.get(0).toKey);

    assertEquals("copy-key-two", transfers.get(1).fromKey);
    assertEquals("copy-to-key-two", transfers.get(1).toKey);

    assertEquals("copy-key-three", transfers.get(2).fromKey);
    assertEquals("copy-to-key-three", transfers.get(2).toKey);

    assertEquals("copy-key-four", transfers.get(3).fromKey);
    assertEquals("copy-to-key-four", transfers.get(3).toKey);

    assertEquals("move-key-one", transfers.get(4).fromKey);
    assertEquals("move-to-key-one", transfers.get(4).toKey);

    assertEquals("move-key-two", transfers.get(5).fromKey);
    assertEquals("move-to-key-two", transfers.get(5).toKey);

    assertEquals("move-key-three", transfers.get(6).fromKey);
    assertEquals("move-to-key-three", transfers.get(6).toKey);

    assertEquals("move-key-four", transfers.get(7).fromKey);
    assertEquals("move-to-key-four", transfers.get(7).toKey);

    assertEquals("delete-key-one", transfers.get(8).deleteKey);
    assertEquals("delete-key-two", transfers.get(9).deleteKey);
    assertEquals("delete-key-three", transfers.get(10).deleteKey);
    assertEquals("delete-key-four", transfers.get(11).deleteKey);

    //if conditions for replace
    List<ConfigUpgradeChangeDefinition.Replace> replacements = m_gson.fromJson(replacementJson,
        new TypeToken<List<ConfigUpgradeChangeDefinition.Replace>>() {
        }.getType());
    assertEquals("replace-key-one", replacements.get(0).key);
    assertEquals("abc", replacements.get(0).find);
    assertEquals("abc-replaced", replacements.get(0).replaceWith);
    assertEquals("replace-key-two", replacements.get(1).key);
    assertEquals("efg", replacements.get(1).find);
    assertEquals("efg-replaced", replacements.get(1).replaceWith);
    assertEquals("replace-key-three", replacements.get(2).key);
    assertEquals("ijk", replacements.get(2).find);
    assertEquals("ijk-replaced", replacements.get(2).replaceWith);
    assertEquals("replace-key-four", replacements.get(3).key);
    assertEquals("lmn", replacements.get(3).find);
    assertEquals("lmn-replaced", replacements.get(3).replaceWith);
  }

  @Test
  public void testConfigTaskConditionSkipped() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    ConfigUpgradePack cup = ambariMetaInfo.getConfigUpgradePack("HDP", "2.1.1");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = getMockUpgradeContext(cluster, Direction.UPGRADE, UpgradeType.ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade,
        context);

    assertEquals(7, groups.size());

    UpgradeGroupHolder hiveGroup = groups.get(4);
    assertEquals("HIVE", hiveGroup.name);

    //Condition is not met, so no config operations should be present in the configureTask...
    ConfigureTask configureTask = (ConfigureTask) hiveGroup.items.get(3).getTasks().get(0).getTasks().get(0);
    Map<String, String> configProperties = configureTask.getConfigurationChanges(cluster, cup);

    assertFalse(configProperties.isEmpty());
    assertEquals(configProperties.get(ConfigureTask.PARAMETER_CONFIG_TYPE), "hive-site");

    assertTrue(configProperties.containsKey(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS));
    assertTrue(configProperties.containsKey(ConfigureTask.PARAMETER_REPLACEMENTS));
    assertTrue(configProperties.containsKey(ConfigureTask.PARAMETER_TRANSFERS));

    String configurationJson = configProperties.get(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS);
    String transferJson = configProperties.get(ConfigureTask.PARAMETER_TRANSFERS);

    String replacementJson = configProperties.get(ConfigureTask.PARAMETER_REPLACEMENTS);
    assertNotNull(configurationJson);
    assertNotNull(transferJson);
    assertNotNull(replacementJson);

    List<ConfigUpgradeChangeDefinition.ConfigurationKeyValue> keyValuePairs = m_gson.fromJson(configurationJson,
        new TypeToken<List<ConfigUpgradeChangeDefinition.ConfigurationKeyValue>>() {
        }.getType());
    assertTrue(keyValuePairs.isEmpty());

    List<ConfigUpgradeChangeDefinition.Replace> replacements = m_gson.fromJson(replacementJson,
        new TypeToken<List<ConfigUpgradeChangeDefinition.Replace>>() {
        }.getType());
    assertTrue(replacements.isEmpty());

    List<ConfigUpgradeChangeDefinition.Transfer> transfers = m_gson.fromJson(transferJson,
        new TypeToken<List<ConfigUpgradeChangeDefinition.Transfer>>() {
        }.getType());
    assertTrue(transfers.isEmpty());
  }

  /**
   * Tests that {@link ConfigurationKeyValue} pairs on a {@link ConfigureTask}
   * are correctly returned based on the if-conditions.
   *
   * @throws Exception
   */
  @Test
  public void testConfigureTask() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    ConfigUpgradePack cup = ambariMetaInfo.getConfigUpgradePack("HDP", "2.1.1");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = getMockUpgradeContext(cluster, Direction.UPGRADE, UpgradeType.ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade,
        context);

    assertEquals(7, groups.size());

    // grab the first configure task out of Hive
    UpgradeGroupHolder hiveGroup = groups.get(4);
    assertEquals("HIVE", hiveGroup.name);
    ConfigureTask configureTask = (ConfigureTask) hiveGroup.items.get(1).getTasks().get(0).getTasks().get(0);

    Map<String, String> configProperties = configureTask.getConfigurationChanges(cluster, cup);
    assertFalse(configProperties.isEmpty());
    assertEquals(configProperties.get(ConfigureTask.PARAMETER_CONFIG_TYPE), "hive-site");

    // now set the property in the if-check in the set element so that we have a match
    Map<String, String> hiveConfigs = new HashMap<>();
    hiveConfigs.put("fooKey", "THIS-BETTER-CHANGE");
    hiveConfigs.put("ifFooKey", "ifFooValue");

    ConfigurationRequest configurationRequest = new ConfigurationRequest();
    configurationRequest.setClusterName(cluster.getClusterName());
    configurationRequest.setType("hive-site");
    configurationRequest.setVersionTag("version2");
    configurationRequest.setProperties(hiveConfigs);

    final ClusterRequest clusterRequest = new ClusterRequest(
        cluster.getClusterId(), cluster.getClusterName(),
        cluster.getDesiredStackVersion().getStackVersion(), null);

    clusterRequest.setDesiredConfig(singletonList(configurationRequest));
    m_managementController.updateClusters(new HashSet<ClusterRequest>() {
      {
        add(clusterRequest);
      }
    }, null);

    // the configure task should now return different properties to set based on
    // the if-condition checks
    configProperties = configureTask.getConfigurationChanges(cluster, cup);
    assertFalse(configProperties.isEmpty());
    assertEquals( configProperties.get(ConfigureTask.PARAMETER_CONFIG_TYPE), "hive-site");

    String configurationJson = configProperties.get(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS);
    assertNotNull(configurationJson);

    List<ConfigUpgradeChangeDefinition.ConfigurationKeyValue> keyValuePairs = m_gson.fromJson(
        configurationJson,
        new TypeToken<List<ConfigUpgradeChangeDefinition.ConfigurationKeyValue>>() {
        }.getType());

    assertEquals("fooKey", keyValuePairs.get(0).key);
    assertEquals("fooValue", keyValuePairs.get(0).value);
  }

  /**
   * Tests that the regex replacement is working for configurations.
   *
   * @throws Exception
   */
  @Test
  public void testConfigureRegexTask() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    ConfigUpgradePack cup = ambariMetaInfo.getConfigUpgradePack("HDP", "2.1.1");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = getMockUpgradeContext(cluster, Direction.UPGRADE, UpgradeType.ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade,context);
    assertEquals(7, groups.size());

    // grab the regex task out of Hive
    UpgradeGroupHolder hiveGroup = groups.get(4);
    assertEquals("HIVE", hiveGroup.name);
    ConfigureTask configureTask = (ConfigureTask) hiveGroup.items.get(5).getTasks().get(0).getTasks().get(0);
    assertEquals("hdp_2_1_1_regex_replace", configureTask.getId());

    // now set the property in the if-check in the set element so that we have a match
    Map<String, String> hiveConfigs = new HashMap<>();
    StringBuilder builder = new StringBuilder();
    builder.append("1-foo-2");
    builder.append(System.lineSeparator());
    builder.append("1-bar-2");
    builder.append(System.lineSeparator());
    builder.append("3-foo-4");
    builder.append(System.lineSeparator());
    builder.append("1-foobar-2");
    builder.append(System.lineSeparator());
    hiveConfigs.put("regex-replace-key-one", builder.toString());

    ConfigurationRequest configurationRequest = new ConfigurationRequest();
    configurationRequest.setClusterName(cluster.getClusterName());
    configurationRequest.setType("hive-site");
    configurationRequest.setVersionTag("version2");
    configurationRequest.setProperties(hiveConfigs);

    final ClusterRequest clusterRequest = new ClusterRequest(
        cluster.getClusterId(), cluster.getClusterName(),
        cluster.getDesiredStackVersion().getStackVersion(), null);

    clusterRequest.setDesiredConfig(singletonList(configurationRequest));
    m_managementController.updateClusters(new HashSet<ClusterRequest>() {
      {
        add(clusterRequest);
      }
    }, null);

    // the configure task should now return different properties to set based on
    // the if-condition checks
    Map<String, String> configProperties = configureTask.getConfigurationChanges(cluster, cup);
    assertFalse(configProperties.isEmpty());
    assertEquals(configProperties.get(ConfigureTask.PARAMETER_CONFIG_TYPE), "hive-site");

    String configurationJson = configProperties.get(ConfigureTask.PARAMETER_REPLACEMENTS);
    assertNotNull(configurationJson);

    List<ConfigUpgradeChangeDefinition.Replace> replacements = m_gson.fromJson(
        configurationJson,
        new TypeToken<List<ConfigUpgradeChangeDefinition.Replace>>() {}.getType());

    assertEquals("1-foo-2" + System.lineSeparator(), replacements.get(0).find);
    assertEquals("REPLACED", replacements.get(0).replaceWith);
    assertEquals("3-foo-4" + System.lineSeparator(), replacements.get(1).find);
    assertEquals("REPLACED", replacements.get(1).replaceWith);
    assertEquals(2, replacements.size());
  }

  @Test
  public void testConfigureTaskWithMultipleConfigurations() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    ConfigUpgradePack cup = ambariMetaInfo.getConfigUpgradePack("HDP", "2.1.1");
    assertNotNull(upgrade);
    Cluster cluster = makeCluster();

    UpgradeContext context = getMockUpgradeContext(cluster, Direction.UPGRADE, UpgradeType.ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(7, groups.size());

    // grab the configure task out of Hive
    UpgradeGroupHolder hiveGroup = groups.get(4);
    assertEquals("HIVE", hiveGroup.name);
    ConfigureTask configureTask = (ConfigureTask) hiveGroup.items.get(1).getTasks().get(0).getTasks().get(0);

    Map<String, String> configProperties = configureTask.getConfigurationChanges(cluster, cup);
    assertFalse(configProperties.isEmpty());
    assertEquals(configProperties.get(ConfigureTask.PARAMETER_CONFIG_TYPE), "hive-site");

    String configurationJson = configProperties.get(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS);
    String transferJson = configProperties.get(ConfigureTask.PARAMETER_TRANSFERS);
    assertNotNull(configurationJson);
    assertNotNull(transferJson);

    List<ConfigUpgradeChangeDefinition.ConfigurationKeyValue> keyValuePairs = m_gson.fromJson(configurationJson,
        new TypeToken<List<ConfigUpgradeChangeDefinition.ConfigurationKeyValue>>() {
        }.getType());

    List<ConfigUpgradeChangeDefinition.Transfer> transfers = m_gson.fromJson(transferJson,
        new TypeToken<List<ConfigUpgradeChangeDefinition.Transfer>>() {
        }.getType());

    assertEquals("fooKey", keyValuePairs.get(0).key);
    assertEquals("fooValue", keyValuePairs.get(0).value);
    assertEquals("fooKey2", keyValuePairs.get(1).key);
    assertEquals("fooValue2", keyValuePairs.get(1).value);
    assertEquals("fooKey3", keyValuePairs.get(2).key);
    assertEquals("fooValue3", keyValuePairs.get(2).value);

    assertEquals("copy-key", transfers.get(0).fromKey);
    assertEquals("copy-key-to", transfers.get(0).toKey);

    assertEquals("move-key", transfers.get(1).fromKey);
    assertEquals("move-key-to", transfers.get(1).toKey);
  }

  @Test
  public void testServiceCheckUpgradeStages() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.2.0");
    assertTrue(upgrades.containsKey("upgrade_test_checks"));
    UpgradePack upgrade = upgrades.get("upgrade_test_checks");
    assertNotNull(upgrade);

    // HBASE and PIG have service checks, but not TEZ.
    Set<String> additionalServices = new HashSet<String>() {{ add("HBASE"); add("PIG"); add("TEZ"); add("AMBARI_METRICS"); }};
    Cluster c = makeCluster(true, additionalServices, "");

    int numServiceChecksExpected = 0;
    Collection<Service> services = c.getServices().values();
    for(Service service : services) {
      ServiceInfo si = ambariMetaInfo.getService(c.getCurrentStackVersion().getStackName(),
          c.getCurrentStackVersion().getStackVersion(), service.getName());
      if (null == si.getCommandScript()) {
        continue;
      }
      if (service.getName().equalsIgnoreCase("TEZ")) {
        assertTrue("Expect Tez to not have any service checks", false);
      }

      // Expect AMS to not run any service checks because it is excluded
      if (service.getName().equalsIgnoreCase("AMBARI_METRICS")) {
        continue;
      }
      numServiceChecksExpected++;
    }

    UpgradeContext context = getMockUpgradeContext(c, Direction.UPGRADE, UpgradeType.ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(8, groups.size());

    UpgradeGroupHolder holder = groups.get(4);
    assertEquals(holder.name, "SERVICE_CHECK_1");
    assertEquals(7, holder.items.size());
    int numServiceChecksActual = 0;
    for (StageWrapper sw : holder.items) {
      for(Service service : services) {
        Pattern p = Pattern.compile(".*" + service.getName(), Pattern.CASE_INSENSITIVE);
        Matcher matcher = p.matcher(sw.getText());
        if (matcher.matches()) {
          numServiceChecksActual++;
          continue;
        }
      }
    }

    assertEquals(numServiceChecksActual, numServiceChecksExpected);

    // grab the manual task out of ZK which has placeholder text
    UpgradeGroupHolder zookeeperGroup = groups.get(1);
    assertEquals("ZOOKEEPER", zookeeperGroup.name);
    ManualTask manualTask = (ManualTask) zookeeperGroup.items.get(0).getTasks().get(
        0).getTasks().get(0);

    assertEquals(1, manualTask.messages.size());
    assertEquals(
        "This is a manual task with a placeholder of placeholder-rendered-properly",
        manualTask.messages.get(0));

    UpgradeGroupHolder clusterGroup = groups.get(3);
    assertEquals(clusterGroup.name, "HBASE");
    assertEquals(clusterGroup.title, "Update HBase Configuration");
    assertEquals(1, clusterGroup.items.size());
    StageWrapper stage = clusterGroup.items.get(0);
    assertEquals(stage.getText(), "Update HBase Configuration");
  }

  @Test
  public void testServiceCheckDowngradeStages() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test_checks"));
    UpgradePack upgrade = upgrades.get("upgrade_test_checks");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = getMockUpgradeContext(cluster, Direction.DOWNGRADE,
        UpgradeType.ROLLING, repositoryVersion2200);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(6, groups.size());

    // grab the manual task out of ZK which has placeholder text

    UpgradeGroupHolder zookeeperGroup = groups.get(4);
    assertEquals("ZOOKEEPER", zookeeperGroup.name);
    ManualTask manualTask = (ManualTask) zookeeperGroup.items.get(0).getTasks().get(
        0).getTasks().get(0);

    assertEquals(1, manualTask.messages.size());
    assertEquals(
        "This is a manual task with a placeholder of placeholder-rendered-properly",
        manualTask.messages.get(0));
  }

  @Test
  public void testUpgradeOrchestrationFullTask() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");

    // set the display names of the service and component in the target stack
    // to make sure that we can correctly render display strings during the
    // upgrade
    ServiceInfo si = ambariMetaInfo.getService("HDP", "2.2.0", "ZOOKEEPER");
    si.setDisplayName("Zk");

    ComponentInfo ci = si.getComponentByName("ZOOKEEPER_SERVER");
    ci.setDisplayName("ZooKeeper1 Server2");

    assertTrue(upgrades.containsKey("upgrade_to_new_stack"));
    UpgradePack upgrade = upgrades.get("upgrade_to_new_stack");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = getMockUpgradeContext(cluster, Direction.UPGRADE, UpgradeType.ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(6, groups.size());

    assertEquals("PRE_CLUSTER", groups.get(0).name);
    assertEquals("ZOOKEEPER", groups.get(1).name);
    assertEquals("CORE_MASTER", groups.get(2).name);
    assertEquals("CORE_SLAVES", groups.get(3).name);
    assertEquals("HIVE", groups.get(4).name);

    UpgradeGroupHolder holder = groups.get(2);
    boolean found = false;
    for (StageWrapper sw : holder.items) {
      if (sw.getTasksJson().contains("Upgrading your database")) {
        found = true;
      }
    }
    assertTrue("Expected to find replaced text for Upgrading", found);

    UpgradeGroupHolder group = groups.get(1);
    // check that the display name is being used
    assertTrue(group.items.get(1).getText().contains("ZooKeeper1 Server2"));
    assertEquals(group.items.get(4).getText(), "Service Check Zk");

    group = groups.get(3);
    assertEquals(8, group.items.size());
    StageWrapper sw = group.items.get(3);
    assertEquals("Validate Partial Upgrade", sw.getText());
    assertEquals(1, sw.getTasks().size());
    assertEquals(1, sw.getTasks().get(0).getTasks().size());
    Task t = sw.getTasks().get(0).getTasks().get(0);
    assertEquals(ManualTask.class, t.getClass());
    ManualTask mt = (ManualTask) t;
    assertTrue(mt.messages.get(0).contains("DataNode and NodeManager"));
    assertNotNull(mt.structuredOut);
    assertTrue(mt.structuredOut.contains("DATANODE"));
    assertTrue(mt.structuredOut.contains("NODEMANAGER"));

    UpgradeGroupHolder postGroup = groups.get(5);
    assertEquals(postGroup.name, "POST_CLUSTER");
    assertEquals(postGroup.title, "Finalize Upgrade");
    assertEquals(4, postGroup.items.size());
    assertEquals("Confirm Finalize", postGroup.items.get(0).getText());
    assertEquals("Execute HDFS Finalize", postGroup.items.get(1).getText());
    assertEquals("Save Cluster State", postGroup.items.get(2).getText());
    assertEquals(StageWrapper.Type.SERVER_SIDE_ACTION, postGroup.items.get(2).getType());
    assertEquals("Run On All 2.2.1.0-1234", postGroup.items.get(3).getText());

    assertEquals(1, postGroup.items.get(3).getTasks().size());
    Set<String> hosts = postGroup.items.get(3).getTasks().get(0).getHosts();
    assertNotNull(hosts);
    assertEquals(4, hosts.size());

    assertEquals(4, groups.get(0).items.size());
    assertEquals(5, groups.get(1).items.size());
    assertEquals(9, groups.get(2).items.size());
    assertEquals(8, groups.get(3).items.size());

    // Do stacks cleanup
    stackManagerMock.invalidateCurrentPaths();
    ambariMetaInfo.init();
  }


  private Cluster makeCluster() throws AmbariException, AuthorizationException {
    return makeCluster(true);
  }


  /**
   * Create an HA cluster
   * @throws AmbariException
   */
  private Cluster makeCluster(boolean clean) throws AmbariException, AuthorizationException {
    return makeCluster(clean, new HashSet<>(), "");
  }

  /**
   * Create an HA cluster
   * @throws AmbariException
   */
  private Cluster makeCluster(boolean clean, Set<String> additionalServices, String yamlFileName)
          throws AmbariException, AuthorizationException {
    Clusters clusters = injector.getInstance(Clusters.class);
    ServiceFactory serviceFactory = injector.getInstance(ServiceFactory.class);

    String clusterName = "c1";

    String repositoryVersionString = "2.1.1-1234";
    StackId stackId = new StackId("HDP-2.1.1");

    clusters.addCluster(clusterName, stackId);
    Cluster c = clusters.getCluster(clusterName);

    RepositoryVersionEntity repositoryVersion = helper.getOrCreateRepositoryVersion(stackId,
        repositoryVersionString);

    helper.getOrCreateRepositoryVersion(STACK_ID_HDP_220, "2.2.0");

    helper.getOrCreateRepositoryVersion(STACK_ID_HDP_220, UPGRADE_VERSION);

    for (int i = 0; i < 4; i++) {
      String hostName = "h" + (i+1);
      clusters.addHost(hostName);
      Host host = clusters.getHost(hostName);

      Map<String, String> hostAttributes = new HashMap<>();
      hostAttributes.put("os_family", "redhat");
      hostAttributes.put("os_release_version", "6");

      host.setHostAttributes(hostAttributes);

      clusters.mapHostToCluster(hostName, clusterName);
    }

    // !!! add services
    c.addService(serviceFactory.createNew(c, "HDFS", repositoryVersion));
    c.addService(serviceFactory.createNew(c, "YARN", repositoryVersion));
    c.addService(serviceFactory.createNew(c, "ZOOKEEPER", repositoryVersion));
    c.addService(serviceFactory.createNew(c, "HIVE", repositoryVersion));
    c.addService(serviceFactory.createNew(c, "OOZIE", repositoryVersion));

    Service s = c.getService("HDFS");
    ServiceComponent sc = s.addServiceComponent("NAMENODE");
    sc.addServiceComponentHost("h1");
    sc.addServiceComponentHost("h2");
    sc = s.addServiceComponent("DATANODE");
    sc.addServiceComponentHost("h2");
    sc.addServiceComponentHost("h3");
    ServiceComponentHost sch = sc.addServiceComponentHost("h4");

    s = c.getService("ZOOKEEPER");
    sc = s.addServiceComponent("ZOOKEEPER_SERVER");
    sc.addServiceComponentHost("h1");
    sc.addServiceComponentHost("h2");
    sc.addServiceComponentHost("h3");

    s = c.getService("YARN");
    sc = s.addServiceComponent("RESOURCEMANAGER");
    sc.addServiceComponentHost("h2");

    sc = s.addServiceComponent("NODEMANAGER");
    sc.addServiceComponentHost("h1");
    sc.addServiceComponentHost("h3");

    s = c.getService("HIVE");
    sc = s.addServiceComponent("HIVE_SERVER");
    sc.addServiceComponentHost("h2");

    s = c.getService("OOZIE");
    // Oozie Server HA
    sc = s.addServiceComponent("OOZIE_SERVER");
    sc.addServiceComponentHost("h2");
    sc.addServiceComponentHost("h3");
    sc = s.addServiceComponent("OOZIE_CLIENT");
    sc.addServiceComponentHost("h1");
    sc.addServiceComponentHost("h2");
    sc.addServiceComponentHost("h3");

    // set some desired configs
    Map<String, String> hiveConfigs = new HashMap<>();
    hiveConfigs.put("hive.server2.transport.mode", "binary");
    hiveConfigs.put("hive.server2.thrift.port", "10001");

    ConfigurationRequest configurationRequest = new ConfigurationRequest();
    configurationRequest.setClusterName(clusterName);
    configurationRequest.setType("hive-site");
    configurationRequest.setVersionTag("version1");
    configurationRequest.setProperties(hiveConfigs);

    final ClusterRequest clusterRequest = new ClusterRequest(c.getClusterId(),
        clusterName, c.getDesiredStackVersion().getStackVersion(), null);

    clusterRequest.setDesiredConfig(singletonList(configurationRequest));
    m_managementController.updateClusters(new HashSet<ClusterRequest>() {
      {
        add(clusterRequest);
      }
    }, null);

    HostsType type = HostsType.normal("h1", "h2", "h3");
    expect(m_masterHostResolver.getMasterAndHosts("ZOOKEEPER", "ZOOKEEPER_SERVER")).andReturn(type).anyTimes();
    expect(m_masterHostResolver.getMasterAndHosts("ZOOKEEPER", "ZOOKEEPER_CLIENT")).andReturn(type).anyTimes();
    expect(m_masterHostResolver.getMasterAndHosts("HDFS", "NAMENODE")).andReturn(namenodeHosts).anyTimes();

    if (clean) {
      type = HostsType.normal("h2", "h3", "h4");
    } else {
      type = HostsType.normal("h2", "h3");
      type.unhealthy = singletonList(sch);
    }
    expect(m_masterHostResolver.getMasterAndHosts("HDFS", "DATANODE")).andReturn(type).anyTimes();

    type = HostsType.normal("h2");
    expect(m_masterHostResolver.getMasterAndHosts("YARN", "RESOURCEMANAGER")).andReturn(type).anyTimes();

    type = HostsType.normal(Sets.newLinkedHashSet());
    expect(m_masterHostResolver.getMasterAndHosts("YARN", "APP_TIMELINE_SERVER")).andReturn(type).anyTimes();

    type = HostsType.normal("h1", "h3");
    expect(m_masterHostResolver.getMasterAndHosts("YARN", "NODEMANAGER")).andReturn(type).anyTimes();

    expect(m_masterHostResolver.getMasterAndHosts("HIVE", "HIVE_SERVER")).andReturn(
        type).anyTimes();

    type = HostsType.normal("h2", "h3");
    expect(m_masterHostResolver.getMasterAndHosts("OOZIE", "OOZIE_SERVER")).andReturn(type).anyTimes();

    type = HostsType.normal("h1", "h2", "h3");
    expect(m_masterHostResolver.getMasterAndHosts("OOZIE", "OOZIE_CLIENT")).andReturn(type).anyTimes();

    expect(m_masterHostResolver.getCluster()).andReturn(c).anyTimes();
    expect(m_masterHostResolver
            .getValueFromDesiredConfigurations("cluster-env", "rack_yaml_file_path"))
            .andReturn(yamlFileName).anyTimes();
    for(String service : additionalServices) {
      c.addService(service, repositoryVersion);
      if (service.equals("HBASE")) {
        type = HostsType.normal("h1", "h2");
        expect(m_masterHostResolver.getMasterAndHosts("HBASE", "HBASE_MASTER")).andReturn(type).anyTimes();
      }
    }

    replay(m_masterHostResolver);

    return c;
  }

  /**
   * Test that multiple execute tasks with an annotation of synchronized="true" each run in their own stage.
   */
  @Test
  public void testUpgradeWithMultipleTasksInOwnStage() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");

    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    assertNotNull(upgrade);
    assertTrue(upgrade.getType() == UpgradeType.ROLLING);

    List<Grouping> upgradePackGroups = upgrade.getGroups(Direction.UPGRADE);

    boolean foundService = false;
    for (Grouping group : upgradePackGroups) {
      if (group.title.equals("Oozie")) {
        foundService = true;
      }
    }
    assertTrue(foundService);

    Cluster cluster = makeCluster();

    UpgradeContext context = getMockUpgradeContext(cluster, Direction.UPGRADE, UpgradeType.ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    // The upgrade pack has 2 tasks for Oozie in the pre-upgrade group.
    // The first task runs on "all", i.e., both Oozie Servers, whereas the
    // second task runs on "any", i.e., exactly one.
    int numPrepareStages = 0;
    for (UpgradeGroupHolder group : groups) {
      if (group.name.equals("OOZIE")) {
        assertTrue(group.items.size() > 0);
        for (StageWrapper sw : group.items) {

          if (sw.getText().equalsIgnoreCase("Preparing Oozie Server on h2 (Batch 1 of 2)") ||
              sw.getText().equalsIgnoreCase("Preparing Oozie Server on h3 (Batch 2 of 2)")) {
            numPrepareStages++;
            List<TaskWrapper> taskWrappers = sw.getTasks();
            assertEquals(1, taskWrappers.size());
            List<Task> tasks = taskWrappers.get(0).getTasks();
            assertEquals(1, taskWrappers.get(0).getHosts().size());
            assertEquals(1, tasks.size());

            ExecuteTask task = (ExecuteTask) tasks.get(0);
            assertTrue("scripts/oozie_server.py".equalsIgnoreCase(task.script));
            assertTrue("stop".equalsIgnoreCase(task.function));
          }

          if (sw.getText().equalsIgnoreCase("Preparing Oozie Server on h2")) {
            numPrepareStages++;
            List<TaskWrapper> taskWrappers = sw.getTasks();
            assertEquals(1, taskWrappers.size());
            List<Task> tasks = taskWrappers.get(0).getTasks();
            assertEquals(1, taskWrappers.get(0).getHosts().size());
            assertEquals(1, tasks.size());

            ExecuteTask task = (ExecuteTask) tasks.get(0);
            assertTrue("scripts/oozie_server_upgrade.py".equalsIgnoreCase(task.script));
            assertTrue("upgrade_oozie_database_and_sharelib".equalsIgnoreCase(task.function));
          }
        }
      }
    }
    assertEquals(3, numPrepareStages);
  }

  @Test
  public void testDowngradeAfterPartialUpgrade() throws Exception {

    Clusters clusters = injector.getInstance(Clusters.class);
    ServiceFactory serviceFactory = injector.getInstance(ServiceFactory.class);

    String clusterName = "c1";

    String version = "2.1.1.0-1234";
    StackId stackId = new StackId("HDP-2.1.1");
    clusters.addCluster(clusterName, stackId);
    Cluster c = clusters.getCluster(clusterName);

    RepositoryVersionEntity repositoryVersion = helper.getOrCreateRepositoryVersion(stackId, version);

    for (int i = 0; i < 2; i++) {
      String hostName = "h" + (i+1);
      clusters.addHost(hostName);
      Host host = clusters.getHost(hostName);

      Map<String, String> hostAttributes = new HashMap<>();
      hostAttributes.put("os_family", "redhat");
      hostAttributes.put("os_release_version", "6");

      host.setHostAttributes(hostAttributes);

      clusters.mapHostToCluster(hostName, clusterName);
    }

    // !!! add services
    c.addService(serviceFactory.createNew(c, "HDFS", repositoryVersion));

    Service s = c.getService("HDFS");
    ServiceComponent sc = s.addServiceComponent("NAMENODE");
    sc.addServiceComponentHost("h1");
    sc.addServiceComponentHost("h2");

    List<ServiceComponentHost> schs = c.getServiceComponentHosts("HDFS", "NAMENODE");
    assertEquals(2, schs.size());

    HostsType type = HostsType.highAvailability("h1", "h2", new LinkedHashSet<>(emptySet()));

    expect(m_masterHostResolver.getMasterAndHosts("ZOOKEEPER", "ZOOKEEPER_SERVER")).andReturn(null).anyTimes();
    expect(m_masterHostResolver.getMasterAndHosts("HDFS", "NAMENODE")).andReturn(type).anyTimes();
    expect(m_masterHostResolver.getCluster()).andReturn(c).anyTimes();
    replay(m_masterHostResolver);

    UpgradeContext context = getMockUpgradeContext(c, Direction.DOWNGRADE, UpgradeType.ROLLING,
        repositoryVersion2200);

    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_direction"));
    UpgradePack upgrade = upgrades.get("upgrade_direction");
    assertNotNull(upgrade);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);
    assertEquals(2, groups.size());

    UpgradeGroupHolder group = groups.get(0);
    assertEquals(1, group.items.size());
    assertEquals("PRE_POST_CLUSTER", group.name);

    group = groups.get(1);
    assertEquals("POST_CLUSTER", group.name);
    assertEquals(3, group.items.size());


    StageWrapper stage = group.items.get(1);
    assertEquals("NameNode Finalize", stage.getText());
    assertEquals(1, stage.getTasks().size());
    TaskWrapper task = stage.getTasks().get(0);
    assertEquals(1, task.getHosts().size());
  }

  @Test
  public void testResolverWithFailedUpgrade() throws Exception {
    Clusters clusters = injector.getInstance(Clusters.class);
    ServiceFactory serviceFactory = injector.getInstance(ServiceFactory.class);

    String clusterName = "c1";

    StackId stackId = new StackId("HDP-2.1.1");
    clusters.addCluster(clusterName, stackId);
    Cluster c = clusters.getCluster(clusterName);

    for (int i = 0; i < 2; i++) {
      String hostName = "h" + (i+1);
      clusters.addHost(hostName);
      Host host = clusters.getHost(hostName);

      Map<String, String> hostAttributes = new HashMap<>();
      hostAttributes.put("os_family", "redhat");
      hostAttributes.put("os_release_version", "6");

      host.setHostAttributes(hostAttributes);

      clusters.mapHostToCluster(hostName, clusterName);
    }

    // !!! add services
    c.addService(serviceFactory.createNew(c, "ZOOKEEPER", repositoryVersion2110));

    Service s = c.getService("ZOOKEEPER");
    ServiceComponent sc = s.addServiceComponent("ZOOKEEPER_SERVER");

    ServiceComponentHost sch1 = sc.addServiceComponentHost("h1");
    sch1.setVersion(repositoryVersion2110.getVersion());

    ServiceComponentHost sch2 = sc.addServiceComponentHost("h2");
    sch2.setVersion(repositoryVersion2110.getVersion());

    List<ServiceComponentHost> schs = c.getServiceComponentHosts("ZOOKEEPER", "ZOOKEEPER_SERVER");
    assertEquals(2, schs.size());

    UpgradeContext context = getMockUpgradeContextNoReplay(c, Direction.UPGRADE,
        UpgradeType.HOST_ORDERED, repositoryVersion2110);

    MasterHostResolver resolver = new MasterHostResolver(c, m_configHelper, context);
    expect(context.getResolver()).andReturn(resolver).anyTimes();
    replay(context);

    HostsType ht = resolver.getMasterAndHosts("ZOOKEEPER", "ZOOKEEPER_SERVER");
    assertEquals(0, ht.getHosts().size());

    // !!! if one of them is failed, it should be scheduled
    sch2.setUpgradeState(UpgradeState.FAILED);

    ht = resolver.getMasterAndHosts("ZOOKEEPER", "ZOOKEEPER_SERVER");

    assertEquals(1, ht.getHosts().size());
    assertEquals("h2", ht.getHosts().iterator().next());
  }

  /**
   * Test that MasterHostResolver is case-insensitive even if configs have hosts in upper case for NameNode.
   * @throws Exception
   */
  @Test
  public void testResolverCaseInsensitive() throws Exception {
    Clusters clusters = injector.getInstance(Clusters.class);
    ServiceFactory serviceFactory = injector.getInstance(ServiceFactory.class);

    String clusterName = "c1";
    String version = "2.1.1.0-1234";

    StackId stackId = new StackId("HDP-2.1.1");
    clusters.addCluster(clusterName, stackId);
    Cluster c = clusters.getCluster(clusterName);

    RepositoryVersionEntity repositoryVersion211 = helper.getOrCreateRepositoryVersion(stackId,
        version);

    for (int i = 0; i < 2; i++) {
      String hostName = "h" + (i+1);
      clusters.addHost(hostName);
      Host host = clusters.getHost(hostName);

      Map<String, String> hostAttributes = new HashMap<>();
      hostAttributes.put("os_family", "redhat");
      hostAttributes.put("os_release_version", "6");

      host.setHostAttributes(hostAttributes);

      clusters.mapHostToCluster(hostName, clusterName);
    }

    // Add services
    c.addService(serviceFactory.createNew(c, "HDFS", repositoryVersion211));

    Service s = c.getService("HDFS");
    ServiceComponent sc = s.addServiceComponent("NAMENODE");
    sc.addServiceComponentHost("h1");
    sc.addServiceComponentHost("h2");

    List<ServiceComponentHost> schs = c.getServiceComponentHosts("HDFS", "NAMENODE");
    assertEquals(2, schs.size());

    setConfigMocks();
    expect(m_configHelper.getValueFromDesiredConfigurations(c, "hdfs-site", "dfs.internal.nameservices")).andReturn("ha").anyTimes();
    expect(m_configHelper.getValueFromDesiredConfigurations(c, "hdfs-site", "dfs.ha.namenodes.ha")).andReturn("nn1,nn2").anyTimes();
    expect(m_configHelper.getValueFromDesiredConfigurations(c, "hdfs-site", "dfs.http.policy")).andReturn("HTTP_ONLY").anyTimes();

    // Notice that these names are all caps.
    expect(m_configHelper.getValueFromDesiredConfigurations(c, "hdfs-site", "dfs.namenode.http-address.ha.nn1")).andReturn("H1:50070").anyTimes();
    expect(m_configHelper.getValueFromDesiredConfigurations(c, "hdfs-site", "dfs.namenode.http-address.ha.nn2")).andReturn("H2:50070").anyTimes();
    replay(m_configHelper);

    UpgradeContext context = getMockUpgradeContextNoReplay(c, Direction.UPGRADE,
        UpgradeType.NON_ROLLING, repositoryVersion211);

    // use a "real" master host resolver here so that we can actually test MM
    MasterHostResolver mhr = new MockMasterHostResolver(c, m_configHelper, context);

    expect(context.getResolver()).andReturn(mhr).anyTimes();
    replay(context);


    HostsType ht = mhr.getMasterAndHosts("HDFS", "NAMENODE");
    assertNotNull(ht.getMasters());
    assertNotNull(ht.getSecondaries());
    assertEquals(2, ht.getHosts().size());

    // Should be stored in lowercase.
    assertTrue(ht.getHosts().contains("h1"));
    assertTrue(ht.getHosts().contains("h1"));
  }

  @Test
  public void testResolverBadJmx() throws Exception {
    Clusters clusters = injector.getInstance(Clusters.class);
    ServiceFactory serviceFactory = injector.getInstance(ServiceFactory.class);

    String clusterName = "c1";
    String version = "2.1.1.0-1234";

    StackId stackId = new StackId("HDP-2.1.1");
    clusters.addCluster(clusterName, stackId);
    Cluster c = clusters.getCluster(clusterName);

    RepositoryVersionEntity repositoryVersion211 = helper.getOrCreateRepositoryVersion(stackId, version);

    for (int i = 0; i < 2; i++) {
      String hostName = "h" + (i+1);
      clusters.addHost(hostName);
      Host host = clusters.getHost(hostName);

      Map<String, String> hostAttributes = new HashMap<>();
      hostAttributes.put("os_family", "redhat");
      hostAttributes.put("os_release_version", "6");

      host.setHostAttributes(hostAttributes);

      clusters.mapHostToCluster(hostName, clusterName);
    }

    // Add services
    c.addService(serviceFactory.createNew(c, "HDFS", repositoryVersion211));

    Service s = c.getService("HDFS");
    ServiceComponent sc = s.addServiceComponent("NAMENODE");
    sc.addServiceComponentHost("h1");
    sc.addServiceComponentHost("h2");

    List<ServiceComponentHost> schs = c.getServiceComponentHosts("HDFS", "NAMENODE");
    assertEquals(2, schs.size());

    setConfigMocks();
    expect(m_configHelper.getValueFromDesiredConfigurations(c, "hdfs-site", "dfs.internal.nameservices")).andReturn("ha").anyTimes();
    expect(m_configHelper.getValueFromDesiredConfigurations(c, "hdfs-site", "dfs.ha.namenodes.ha")).andReturn("nn1,nn2").anyTimes();
    expect(m_configHelper.getValueFromDesiredConfigurations(c, "hdfs-site", "dfs.http.policy")).andReturn("HTTP_ONLY").anyTimes();

    // Notice that these names are all caps.
    expect(m_configHelper.getValueFromDesiredConfigurations(c, "hdfs-site", "dfs.namenode.http-address.ha.nn1")).andReturn("H1:50070").anyTimes();
    expect(m_configHelper.getValueFromDesiredConfigurations(c, "hdfs-site", "dfs.namenode.http-address.ha.nn2")).andReturn("H2:50070").anyTimes();
    replay(m_configHelper);

    UpgradeContext context = getMockUpgradeContextNoReplay(c, Direction.UPGRADE,
        UpgradeType.NON_ROLLING, repositoryVersion211);

    // use a "real" master host resolver here so that we can actually test MM
    MasterHostResolver mhr = new BadMasterHostResolver(c, m_configHelper, context);

    expect(context.getResolver()).andReturn(mhr).anyTimes();
    replay(context);

    HostsType ht = mhr.getMasterAndHosts("HDFS", "NAMENODE");
    assertNotNull(ht.getMasters());
    assertNotNull(ht.getSecondaries());
    assertEquals(2, ht.getHosts().size());

    // Should be stored in lowercase.
    assertTrue(ht.getHosts().contains("h1"));
    assertTrue(ht.getHosts().contains("h2"));
  }


  /**
   * Tests that advanced {@link Grouping} instances like {@link StopGrouping}
   * work with rolling upgrade packs.
   *
   * @throws Exception
   */
  @Test
  public void testRollingUpgradesCanUseAdvancedGroupings() throws Exception {
    final String clusterName = "c1";
    final StackId sourceStackId = new StackId("HDP", "2.1.1");
    final StackId targetStackId = new StackId("HDP", "2.2.0");
    final Direction upgradeDirection = Direction.UPGRADE;
    final UpgradeType upgradeType = UpgradeType.ROLLING;

    Cluster cluster = makeCluster();

    // grab the right pack
    String preferredUpgradePackName = "upgrade_grouping_rolling";
    UpgradePack upgradePack = m_upgradeHelper.suggestUpgradePack(clusterName, sourceStackId,
        targetStackId, upgradeDirection, upgradeType, preferredUpgradePackName);

    assertEquals(upgradeType, upgradePack.getType());

    // get an upgrade
    UpgradeContext context = getMockUpgradeContext(cluster, Direction.UPGRADE, UpgradeType.ROLLING,
        repositoryVersion2210, RepositoryType.STANDARD, Collections.singleton("ZOOKEEPER"));

    List<Grouping> groupings = upgradePack.getGroups(Direction.UPGRADE);
    assertEquals(2, groupings.size());
    assertEquals("STOP_ZOOKEEPER", groupings.get(0).name);
    assertEquals("RESTART_ZOOKEEPER", groupings.get(1).name);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgradePack, context);

    assertEquals(2, groups.size());

    assertEquals("STOP_ZOOKEEPER", groups.get(0).name);
    assertEquals("RESTART_ZOOKEEPER", groups.get(1).name);

    // STOP_ZOOKEEPER GROUP
    UpgradeGroupHolder group = groups.get(0);

    // Check that the upgrade framework properly expanded the STOP grouping into
    // STOP tasks
    assertEquals("Stopping ZooKeeper Server on h1 (Batch 1 of 3)", group.items.get(0).getText());
  }

  @Test
  public void testOrchestrationNoServerSideOnDowngrade() throws Exception {
    Clusters clusters = injector.getInstance(Clusters.class);
    ServiceFactory serviceFactory = injector.getInstance(ServiceFactory.class);

    String clusterName = "c1";

    String version = "2.1.1.0-1234";
    StackId stackId = new StackId("HDP-2.1.1");
    StackId stackId2 = new StackId("HDP-2.2.0");
    clusters.addCluster(clusterName, stackId);
    Cluster c = clusters.getCluster(clusterName);

    RepositoryVersionEntity repoVersion211 = helper.getOrCreateRepositoryVersion(stackId,
        version);

    RepositoryVersionEntity repoVersion220 = helper.getOrCreateRepositoryVersion(stackId2, "2.2.0");

    for (int i = 0; i < 2; i++) {
      String hostName = "h" + (i+1);
      clusters.addHost(hostName);
      Host host = clusters.getHost(hostName);

      Map<String, String> hostAttributes = new HashMap<>();
      hostAttributes.put("os_family", "redhat");
      hostAttributes.put("os_release_version", "6");

      host.setHostAttributes(hostAttributes);

      clusters.mapHostToCluster(hostName, clusterName);
    }

    // !!! add storm
    c.addService(serviceFactory.createNew(c, "STORM", repoVersion211));

    Service s = c.getService("STORM");
    ServiceComponent sc = s.addServiceComponent("NIMBUS");
    ServiceComponentHost sch1 = sc.addServiceComponentHost("h1");
    ServiceComponentHost sch2 = sc.addServiceComponentHost("h2");

    UpgradePack upgradePack = new UpgradePack() {
      @Override
      public List<Grouping> getGroups(Direction direction) {

        Grouping g = new Grouping();

        OrderService orderService = new OrderService();
        orderService.serviceName = "STORM";
        orderService.components = singletonList("NIMBUS");

        g.name = "GROUP1";
        g.title = "Nimbus Group";
        g.services.add(orderService);

        return Lists.newArrayList(g);
      }

      @Override
      public Map<String, Map<String, ProcessingComponent>> getTasks() {
        ManualTask mt = new ManualTask();
        mt.messages = Lists.newArrayList("My New Message");

        ProcessingComponent pc = new ProcessingComponent();
        pc.name = "NIMBUS_MESSAGE";
        pc.preTasks = Lists.newArrayList(mt);

        return Collections.singletonMap("STORM", Collections.singletonMap("NIMBUS", pc));
      }

    };

    UpgradeContext context = getMockUpgradeContextNoReplay(c, Direction.UPGRADE,
        UpgradeType.NON_ROLLING, repoVersion220);

    // use a "real" master host resolver here so that we can actually test MM
    MasterHostResolver masterHostResolver = new MasterHostResolver(c, m_configHelper, context);

    expect(context.getResolver()).andReturn(masterHostResolver).anyTimes();
    replay(context);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgradePack, context);

    assertEquals(1, groups.size());

    sch1.setVersion(repoVersion211.getVersion());
    sch2.setVersion(repoVersion211.getVersion());

    context = getMockUpgradeContextNoReplay(c, Direction.DOWNGRADE, UpgradeType.NON_ROLLING,
        repoVersion211);

    // use a "real" master host resolver here so that we can actually test MM
    masterHostResolver = new MasterHostResolver(c, m_configHelper, context);

    expect(context.getResolver()).andReturn(masterHostResolver).anyTimes();
    replay(context);

    groups = m_upgradeHelper.createSequence(upgradePack, context);

    assertTrue(groups.isEmpty());
  }

  @Test
  public void testMultipleServerTasks() throws Exception {

    // !!! make a two node cluster with just ZK
    Clusters clusters = injector.getInstance(Clusters.class);
    ServiceFactory serviceFactory = injector.getInstance(ServiceFactory.class);

    String clusterName = "c1";

    String version = "2.1.1.0-1234";
    StackId stackId = new StackId("HDP-2.1.1");
    StackId stackId2 = new StackId("HDP-2.2.0");

    clusters.addCluster(clusterName, stackId);
    Cluster c = clusters.getCluster(clusterName);

    RepositoryVersionEntity repositoryVersion = helper.getOrCreateRepositoryVersion(stackId,
        version);

    RepositoryVersionEntity repoVersion220 = helper.getOrCreateRepositoryVersion(stackId2, "2.2.0");

    helper.getOrCreateRepositoryVersion(stackId2, UPGRADE_VERSION);

    for (int i = 0; i < 2; i++) {
      String hostName = "h" + (i+1);
      clusters.addHost(hostName);
      Host host = clusters.getHost(hostName);

      Map<String, String> hostAttributes = new HashMap<>();
      hostAttributes.put("os_family", "redhat");
      hostAttributes.put("os_release_version", "6");
      host.setHostAttributes(hostAttributes);

      clusters.mapHostToCluster(hostName, clusterName);
    }

    // !!! add services
    c.addService(serviceFactory.createNew(c, "ZOOKEEPER", repositoryVersion));

    Service s = c.getService("ZOOKEEPER");
    ServiceComponent sc = s.addServiceComponent("ZOOKEEPER_SERVER");
    sc.addServiceComponentHost("h1");
    sc.addServiceComponentHost("h2");

    sc = s.addServiceComponent("ZOOKEEPER_CLIENT");
    sc.addServiceComponentHost("h1");
    sc.addServiceComponentHost("h2");

    EasyMock.reset(m_masterHostResolver);

    expect(m_masterHostResolver.getCluster()).andReturn(c).anyTimes();

    HostsType type = HostsType.normal("h1", "h2");
    expect(m_masterHostResolver.getMasterAndHosts("ZOOKEEPER", "ZOOKEEPER_SERVER")).andReturn(type).anyTimes();

    type = HostsType.normal("h1", "h2");
    expect(m_masterHostResolver.getMasterAndHosts("ZOOKEEPER", "ZOOKEEPER_CLIENT")).andReturn(type).anyTimes();

    expect(m_masterHostResolver.getValueFromDesiredConfigurations("cluster-env", "rack_yaml_file_path")).andReturn("")
            .anyTimes();
    replay(m_masterHostResolver);

    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");

    ServiceInfo si = ambariMetaInfo.getService("HDP", "2.1.1", "ZOOKEEPER");
    si.setDisplayName("Zk");
    ComponentInfo ci = si.getComponentByName("ZOOKEEPER_SERVER");
    ci.setDisplayName("ZooKeeper1 Server2");

    UpgradePack upgrade = upgrades.get("upgrade_multi_server_tasks");
    assertNotNull(upgrade);

    UpgradeContext context = getMockUpgradeContext(c, Direction.UPGRADE, UpgradeType.NON_ROLLING,
        repoVersion220);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(2, groups.size());


    // zk server as a colocated grouping first.  XML says to run a manual, 2 configs, and an execute
    UpgradeGroupHolder group1 = groups.get(0);
    assertEquals(7, group1.items.size());

    // Stage 1.  manual, 2 configs, execute
    assertEquals(4, group1.items.get(0).getTasks().size());
    TaskWrapper taskWrapper = group1.items.get(0).getTasks().get(0);
    assertEquals(1, taskWrapper.getTasks().size());
    assertEquals(Task.Type.MANUAL, taskWrapper.getTasks().get(0).getType());

    taskWrapper = group1.items.get(0).getTasks().get(1);
    assertEquals(1, taskWrapper.getTasks().size());
    assertEquals(Task.Type.CONFIGURE, taskWrapper.getTasks().get(0).getType());

    taskWrapper = group1.items.get(0).getTasks().get(2);
    assertEquals(1, taskWrapper.getTasks().size());
    assertEquals(Task.Type.CONFIGURE, taskWrapper.getTasks().get(0).getType());

    taskWrapper = group1.items.get(0).getTasks().get(3);
    assertEquals(1, taskWrapper.getTasks().size());
    assertEquals(Task.Type.EXECUTE, taskWrapper.getTasks().get(0).getType());

    // Stage 2. restart for h1
    assertEquals(1, group1.items.get(1).getTasks().size());
    taskWrapper = group1.items.get(1).getTasks().get(0);
    assertEquals(1, taskWrapper.getTasks().size());
    assertEquals(Task.Type.RESTART, taskWrapper.getTasks().get(0).getType());
    assertTrue(taskWrapper.getHosts().contains("h1"));

    // Stage 3. service check
    assertEquals(1, group1.items.get(2).getTasks().size());
    taskWrapper = group1.items.get(2).getTasks().get(0);
    assertEquals(1, taskWrapper.getTasks().size());
    assertEquals(Task.Type.SERVICE_CHECK, taskWrapper.getTasks().get(0).getType());

    // stage 4. manual step for validation
    assertEquals(1, group1.items.get(3).getTasks().size());
    taskWrapper = group1.items.get(3).getTasks().get(0);
    assertEquals(1, taskWrapper.getTasks().size());
    assertEquals(Task.Type.MANUAL, taskWrapper.getTasks().get(0).getType());

    // Stage 5. repeat execute as it's not a server-side task.  no configure or manual tasks
    assertEquals(1, group1.items.get(4).getTasks().size());
    taskWrapper = group1.items.get(4).getTasks().get(0);
    assertEquals(1, taskWrapper.getTasks().size());
    assertEquals(Task.Type.EXECUTE, taskWrapper.getTasks().get(0).getType());

    // Stage 6. restart for h2.
    assertEquals(1, group1.items.get(5).getTasks().size());
    taskWrapper = group1.items.get(5).getTasks().get(0);
    assertEquals(1, taskWrapper.getTasks().size());
    assertEquals(Task.Type.RESTART, taskWrapper.getTasks().get(0).getType());
    assertTrue(taskWrapper.getHosts().contains("h2"));

    // Stage 7. service check
    assertEquals(1, group1.items.get(6).getTasks().size());
    taskWrapper = group1.items.get(6).getTasks().get(0);
    assertEquals(1, taskWrapper.getTasks().size());
    assertEquals(Task.Type.SERVICE_CHECK, taskWrapper.getTasks().get(0).getType());


    // zk client
    UpgradeGroupHolder group2 = groups.get(1);
    assertEquals(5, group2.items.size());

    // Stage 1. Configure
    assertEquals(1, group2.items.get(0).getTasks().size());
    taskWrapper = group2.items.get(0).getTasks().get(0);
    assertEquals(1, taskWrapper.getTasks().size());
    assertEquals(Task.Type.CONFIGURE, taskWrapper.getTasks().get(0).getType());

    // Stage 2. Custom class
    assertEquals(1, group2.items.get(1).getTasks().size());
    taskWrapper = group2.items.get(1).getTasks().get(0);
    assertEquals(1, taskWrapper.getTasks().size());
    assertEquals(Task.Type.SERVER_ACTION, taskWrapper.getTasks().get(0).getType());

    // Stage 3. Restart client on h1
    assertEquals(1, group2.items.get(2).getTasks().size());
    taskWrapper = group2.items.get(2).getTasks().get(0);
    assertEquals(1, taskWrapper.getTasks().size());
    assertEquals(Task.Type.RESTART, taskWrapper.getTasks().get(0).getType());

    // Stage 4. Restart client on h2 (no configure or custom class)
    assertEquals(1, group2.items.get(3).getTasks().size());
    taskWrapper = group2.items.get(3).getTasks().get(0);
    assertEquals(1, taskWrapper.getTasks().size());
    assertEquals(Task.Type.RESTART, taskWrapper.getTasks().get(0).getType());

    // Stage 5. service check
    assertEquals(1, group2.items.get(4).getTasks().size());
    taskWrapper = group2.items.get(4).getTasks().get(0);
    assertEquals(1, taskWrapper.getTasks().size());
    assertEquals(Task.Type.SERVICE_CHECK, taskWrapper.getTasks().get(0).getType());

  }




  /**
   * Tests {@link UpgradeType#HOST_ORDERED}, specifically that the orchestration
   * can properly expand the single {@link HostOrderGrouping} and create the
   * correct stages based on the dependencies of the components.
   *
   * @throws Exception
   */
  @Test
  public void testHostGroupingOrchestration() throws Exception {
    Clusters clusters = injector.getInstance(Clusters.class);
    ServiceFactory serviceFactory = injector.getInstance(ServiceFactory.class);

    String clusterName = "c1";

    String version = "2.1.1.0-1234";
    StackId stackId = new StackId("HDP-2.1.1");
    StackId stackId2 = new StackId("HDP-2.2.0");
    clusters.addCluster(clusterName, stackId);
    Cluster c = clusters.getCluster(clusterName);

    RepositoryVersionEntity repoVersion211 = helper.getOrCreateRepositoryVersion(stackId, version);

    RepositoryVersionEntity repoVersion220 = helper.getOrCreateRepositoryVersion(stackId2, "2.2.0");

    // create 2 hosts
    for (int i = 0; i < 2; i++) {
      String hostName = "h" + (i+1);
      clusters.addHost(hostName);
      Host host = clusters.getHost(hostName);

      Map<String, String> hostAttributes = new HashMap<>();
      hostAttributes.put("os_family", "redhat");
      hostAttributes.put("os_release_version", "6");

      host.setHostAttributes(hostAttributes);

      clusters.mapHostToCluster(hostName, clusterName);
    }

    // add ZK Server to both hosts, and then Nimbus to only 1 - this will test
    // how the HOU breaks out dependencies into stages
    c.addService(serviceFactory.createNew(c, "ZOOKEEPER", repoVersion211));
    c.addService(serviceFactory.createNew(c, "HBASE", repoVersion211));
    Service zookeeper = c.getService("ZOOKEEPER");
    Service hbase = c.getService("HBASE");
    ServiceComponent zookeeperServer = zookeeper.addServiceComponent("ZOOKEEPER_SERVER");
    ServiceComponentHost zookeeperServer1 = zookeeperServer.addServiceComponentHost("h1");
    ServiceComponentHost zookeeperServer2 = zookeeperServer.addServiceComponentHost("h2");
    ServiceComponent hbaseMaster = hbase.addServiceComponent("HBASE_MASTER");
    ServiceComponentHost hbaseMaster1 = hbaseMaster.addServiceComponentHost("h1");

    // !!! make a custom grouping
    HostOrderItem hostItem = new HostOrderItem(HostOrderActionType.HOST_UPGRADE,
        Lists.newArrayList("h1", "h2"));

    HostOrderItem checkItem = new HostOrderItem(HostOrderActionType.SERVICE_CHECK,
        Lists.newArrayList("ZOOKEEPER", "HBASE"));

    Grouping g = new HostOrderGrouping();
    ((HostOrderGrouping) g).setHostOrderItems(Lists.newArrayList(hostItem, checkItem));
    g.title = "Some Title";

    UpgradePack upgradePack = new UpgradePack();

    // !!! set the groups directly; allow the logic in getGroups(Direction) to happen
    Field field = UpgradePack.class.getDeclaredField("groups");
    field.setAccessible(true);
    field.set(upgradePack, Lists.newArrayList(g));

    field = UpgradePack.class.getDeclaredField("type" );
    field.setAccessible(true);
    field.set(upgradePack, UpgradeType.HOST_ORDERED);

    UpgradeContext context = getMockUpgradeContextNoReplay(c, Direction.UPGRADE,
        UpgradeType.HOST_ORDERED, repoVersion220);

    MasterHostResolver resolver = new MasterHostResolver(c, m_configHelper, context);
    expect(context.getResolver()).andReturn(resolver).anyTimes();
    replay(context);


    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgradePack, context);

    assertEquals(1, groups.size());

    UpgradeGroupHolder holder = groups.get(0);
    assertEquals(9, holder.items.size());

    for (int i = 0; i < 7; i++) {
      StageWrapper w = holder.items.get(i);
      if (i == 0 || i == 4) {
        assertEquals(StageWrapper.Type.STOP, w.getType());
      } else if (i == 1 || i == 5) {
        assertEquals(StageWrapper.Type.SERVER_SIDE_ACTION, w.getType());
        assertEquals(1, w.getTasks().size());
        assertEquals(1, w.getTasks().get(0).getTasks().size());
        Task t = w.getTasks().get(0).getTasks().get(0);
        assertEquals(ManualTask.class, t.getClass());
        ManualTask mt = (ManualTask) t;
        assertNotNull(mt.structuredOut);
        assertTrue(mt.structuredOut.contains("type"));
        assertTrue(mt.structuredOut.contains(HostOrderItem.HostOrderActionType.HOST_UPGRADE.toString()));
        assertTrue(mt.structuredOut.contains("host"));
        assertTrue(mt.structuredOut.contains(i == 1 ? "h1" : "h2"));
      } else {
        assertEquals(StageWrapper.Type.RESTART, w.getType());
      }
    }

    assertEquals(StageWrapper.Type.SERVICE_CHECK, holder.items.get(7).getType());
    assertEquals(StageWrapper.Type.SERVICE_CHECK, holder.items.get(8).getType());

    // !!! test downgrade when all host components have failed
    zookeeperServer1.setVersion(repoVersion211.getVersion());
    zookeeperServer2.setVersion(repoVersion211.getVersion());
    hbaseMaster1.setVersion(repoVersion211.getVersion());

    context = getMockUpgradeContextNoReplay(c, Direction.DOWNGRADE, UpgradeType.HOST_ORDERED,
        repoVersion211);

    resolver = new MasterHostResolver(c, m_configHelper, context);
    expect(context.getResolver()).andReturn(resolver).anyTimes();
    replay(context);

    groups = m_upgradeHelper.createSequence(upgradePack, context);

    assertEquals(1, groups.size());
    assertEquals(2, groups.get(0).items.size());

    // !!! test downgrade when one of the hosts had failed
    zookeeperServer1.setVersion(repoVersion211.getVersion());
    zookeeperServer2.setVersion(repoVersion220.getVersion());
    hbaseMaster1.setVersion(repoVersion211.getVersion());

    context = getMockUpgradeContextNoReplay(c, Direction.DOWNGRADE, UpgradeType.HOST_ORDERED,
        repoVersion211);

    resolver = new MasterHostResolver(c, m_configHelper, context);
    expect(context.getResolver()).andReturn(resolver).anyTimes();
    replay(context);

    groups = m_upgradeHelper.createSequence(upgradePack, context);

    assertEquals(1, groups.size());
    assertEquals(5, groups.get(0).items.size());
  }

  /**
   * Tests that the {@link SecurityCondition} element correctly restricts the groups in
   * an upgrade.
   *
   * @throws Exception
   */
  @Test
  public void testUpgradeConditions() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.2.0");
    assertTrue(upgrades.containsKey("upgrade_test_conditions"));
    UpgradePack upgrade = upgrades.get("upgrade_test_conditions");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    KerberosDetails kerberosDetails = createNiceMock(KerberosDetails.class);
    expect(kerberosDetails.getKdcType()).andReturn(KDCType.NONE).atLeastOnce();
    replay(kerberosDetails);

    UpgradeContext context = getMockUpgradeContext(cluster, Direction.UPGRADE, UpgradeType.ROLLING, false);
    expect(context.getKerberosDetails()).andReturn(kerberosDetails).atLeastOnce();
    replay(context);

    // initially, no conditions should be met, so only 1 group should be
    // available
    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);
    assertEquals(1, groups.size());

    // from that 1 group, only 1 task is condition-less
    List<StageWrapper> stageWrappers = groups.get(0).items;
    assertEquals(1, stageWrappers.size());
    assertEquals(1, stageWrappers.get(0).getTasks().size());

    // set the configuration property and try again
    Map<String, String> fooConfigs = new HashMap<>();
    fooConfigs.put("foo-property", "foo-value");
    ConfigurationRequest configurationRequest = new ConfigurationRequest();
    configurationRequest.setClusterName(cluster.getClusterName());
    configurationRequest.setType("foo-site");
    configurationRequest.setVersionTag("version1");
    configurationRequest.setProperties(fooConfigs);

    final ClusterRequest clusterRequest = new ClusterRequest(cluster.getClusterId(),
        cluster.getClusterName(), cluster.getDesiredStackVersion().getStackVersion(), null);

    clusterRequest.setDesiredConfig(singletonList(configurationRequest));
    m_managementController.updateClusters(Sets.newHashSet(clusterRequest), null);

    // the config condition should now be set
    groups = m_upgradeHelper.createSequence(upgrade, context);
    assertEquals(2, groups.size());
    assertEquals("ZOOKEEPER_CONFIG_CONDITION_TEST", groups.get(0).name);

    // now change the cluster security so the other conditions come back too
    cluster.setSecurityType(SecurityType.KERBEROS);

    groups = m_upgradeHelper.createSequence(upgrade, context);
    assertEquals(5, groups.size());

    EasyMock.verify(kerberosDetails);
  }

  @Test
  public void testUpgradeOrchestrationWithRack() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("foo", "bar");
    assertTrue(upgrades.isEmpty());
    upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    ServiceInfo serviceInfo = ambariMetaInfo.getService("HDP", "2.2.0", "ZOOKEEPER");
    serviceInfo.setDisplayName("Zk");
    ComponentInfo componentInfo = serviceInfo.getComponentByName("ZOOKEEPER_SERVER");
    componentInfo.setDisplayName("ZooKeeper1 Server2");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    assertNotNull(upgrade);
    File file = tmpFolder.newFile("rack_config.yaml");
    FileUtils.writeStringToFile(file, "racks:\n" +
            "  racka-1:\n" +
            "    hostGroups:\n" +
            "    - hosts:\n" +
            "      - h2\n" +
            "      - h4\n" +
            "    - hosts:\n" +
            "      - h3\n" +
            "      - h5\n" +
            "  rackb-22:\n" +
            "    hosts:\n" +
            "    - h1\n" +
            "    - " + StageUtils.getHostName() + "\n", Charset.defaultCharset(), false);
    Cluster cluster = makeCluster(true, new HashSet<>(), file.getAbsolutePath());
    UpgradeContext context = getMockUpgradeContext(cluster, Direction.UPGRADE, UpgradeType.ROLLING);
    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);
    assertEquals(7, groups.size());
    assertEquals("PRE_CLUSTER", groups.get(0).name);
    assertEquals("ZOOKEEPER", groups.get(1).name);
    assertEquals("CORE_MASTER", groups.get(2).name);
    assertEquals("CORE_SLAVES", groups.get(3).name);
    assertEquals("HIVE", groups.get(4).name);
    assertEquals("OOZIE", groups.get(5).name);
    UpgradeGroupHolder holder = groups.get(2);
    boolean found = false;
    for (StageWrapper sw : holder.items) {
      if (sw.getTasksJson().contains("Upgrading your database")) {
        found = true;
      }
    }
    assertTrue("Expected to find replaced text for Upgrading", found);

    UpgradeGroupHolder group = groups.get(1);
    assertTrue(group.items.get(1).getText().contains("ZooKeeper1 Server2"));
    assertEquals(group.items.get(5).getText(), "Service Check Zk");
    group = groups.get(3);
    assertEquals(8, group.items.size());
    StageWrapper sw = group.items.get(3);
    assertEquals("Validate Partial Upgrade", sw.getText());
    assertEquals(1, sw.getTasks().size());
    assertEquals(1, sw.getTasks().get(0).getTasks().size());
    Task task = sw.getTasks().get(0).getTasks().get(0);
    assertEquals(ManualTask.class, task.getClass());
    ManualTask mt = (ManualTask) task;
    assertTrue(mt.messages.get(0).contains("DataNode and NodeManager"));
    assertNotNull(mt.structuredOut);
    assertTrue(mt.structuredOut.contains("DATANODE"));
    assertTrue(mt.structuredOut.contains("NODEMANAGER"));
    UpgradeGroupHolder postGroup = groups.get(6);
    assertEquals("POST_CLUSTER", postGroup.name);
    assertEquals("Finalize Upgrade", postGroup.title);
    assertEquals(3, postGroup.items.size());
    assertEquals("Confirm Finalize", postGroup.items.get(0).getText());
    assertEquals("Execute HDFS Finalize", postGroup.items.get(1).getText());
    assertEquals("Save Cluster State", postGroup.items.get(2).getText());
    assertEquals(StageWrapper.Type.SERVER_SIDE_ACTION, postGroup.items.get(2).getType());
    assertEquals(4, groups.get(0).items.size());
    assertEquals(6, groups.get(1).items.size());
    assertEquals(9, groups.get(2).items.size());
    assertEquals(8, groups.get(3).items.size());
    stackManagerMock.invalidateCurrentPaths();
    ambariMetaInfo.init();
  }

  @Test
  public void testUpgradeOrchestrationWithRackError() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("foo", "bar");
    assertTrue(upgrades.isEmpty());
    upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    ServiceInfo serviceInfo = ambariMetaInfo.getService("HDP", "2.2.0", "ZOOKEEPER");
    serviceInfo.setDisplayName("Zk");
    ComponentInfo componentInfo = serviceInfo.getComponentByName("ZOOKEEPER_SERVER");
    componentInfo.setDisplayName("ZooKeeper1 Server2");
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    assertNotNull(upgrade);
    File file = tmpFolder.newFile("rack_config.yaml");
    FileUtils.writeStringToFile(file, "racks:\n" +
            "  racka-1:\n" +
            "    hostGroups:\n" +
            "    - hosts:\n" +
            "      - h4\n" +
            "    - hosts:\n" +
            "      - h5\n", Charset.defaultCharset(), false);
    Cluster cluster = makeCluster(true, new HashSet<>(), file.getAbsolutePath());
    UpgradeContext context = getMockUpgradeContext(cluster, Direction.UPGRADE, UpgradeType.ROLLING);
    try {
      m_upgradeHelper.createSequence(upgrade, context);
    } catch (RuntimeException e) {
      assertTrue(e.getCause().getMessage().contains("Rack mapping is not present for host name"));
    }
  }


  /**
   * Tests merging configurations between existing and new stack values on
   * upgrade.
   *
   * @throws Exception
   */
  @Test
  public void testMergeConfigurations() throws Exception {
    RepositoryVersionEntity repoVersion211 = createNiceMock(RepositoryVersionEntity.class);
    RepositoryVersionEntity repoVersion220 = createNiceMock(RepositoryVersionEntity.class);

    StackId stack211 = new StackId("HDP-2.1.1");
    StackId stack220 = new StackId("HDP-2.2.0");

    String version211 = "2.1.1.0-1234";
    String version220 = "2.2.0.0-1234";

    expect(repoVersion211.getStackId()).andReturn(stack211).atLeastOnce();
    expect(repoVersion211.getVersion()).andReturn(version211).atLeastOnce();

    expect(repoVersion220.getStackId()).andReturn(stack220).atLeastOnce();
    expect(repoVersion220.getVersion()).andReturn(version220).atLeastOnce();

    Map<String, Map<String, String>> stack211Configs = new HashMap<>();
    Map<String, String> stack211FooType = new HashMap<>();
    Map<String, String> stack211BarType = new HashMap<>();
    Map<String, String> stack211BazType = new HashMap<>();
    stack211Configs.put("foo-site", stack211FooType);
    stack211Configs.put("bar-site", stack211BarType);
    stack211Configs.put("baz-site", stack211BazType);
    stack211FooType.put("1", "one");
    stack211FooType.put("1A", "one-A");
    stack211BarType.put("2", "two");
    stack211BazType.put("3", "three");

    Map<String, Map<String, String>> stack220Configs = new HashMap<>();
    Map<String, String> stack220FooType = new HashMap<>();
    Map<String, String> stack220BazType = new HashMap<>();
    stack220Configs.put("foo-site", stack220FooType);
    stack220Configs.put("baz-site", stack220BazType);
    stack220FooType.put("1", "one-new");
    stack220FooType.put("1A1", "one-A-one");
    stack220BazType.put("3", "three-new");

    Map<String, String> existingFooType = new HashMap<>();
    Map<String, String> existingBarType = new HashMap<>();
    Map<String, String> existingBazType = new HashMap<>();

    ClusterConfigEntity fooConfigEntity = createNiceMock(ClusterConfigEntity.class);
    ClusterConfigEntity barConfigEntity = createNiceMock(ClusterConfigEntity.class);
    ClusterConfigEntity bazConfigEntity = createNiceMock(ClusterConfigEntity.class);

    expect(fooConfigEntity.getType()).andReturn("foo-site");
    expect(barConfigEntity.getType()).andReturn("bar-site");
    expect(bazConfigEntity.getType()).andReturn("baz-site");

    Config fooConfig = createNiceMock(Config.class);
    Config barConfig = createNiceMock(Config.class);
    Config bazConfig = createNiceMock(Config.class);

    existingFooType.put("1", "one");
    existingFooType.put("1A", "one-A");
    existingBarType.put("2", "two");
    existingBazType.put("3", "three-changed");

    expect(fooConfig.getType()).andReturn("foo-site").atLeastOnce();
    expect(barConfig.getType()).andReturn("bar-site").atLeastOnce();
    expect(bazConfig.getType()).andReturn("baz-site").atLeastOnce();
    expect(fooConfig.getProperties()).andReturn(existingFooType);
    expect(barConfig.getProperties()).andReturn(existingBarType);
    expect(bazConfig.getProperties()).andReturn(existingBazType);

    Map<String, DesiredConfig> desiredConfigurations = new HashMap<>();
    desiredConfigurations.put("foo-site", null);
    desiredConfigurations.put("bar-site", null);
    desiredConfigurations.put("baz-site", null);

    Service zookeeper = createNiceMock(Service.class);
    expect(zookeeper.getName()).andReturn("ZOOKEEPER").atLeastOnce();
    expect(zookeeper.getServiceComponents()).andReturn(
      new HashMap<>()).once();
    zookeeper.setDesiredRepositoryVersion(repoVersion220);
    expectLastCall().once();

    Cluster cluster = createNiceMock(Cluster.class);
    expect(cluster.getCurrentStackVersion()).andReturn(stack211).atLeastOnce();
    expect(cluster.getDesiredStackVersion()).andReturn(stack220);
    expect(cluster.getDesiredConfigs()).andReturn(desiredConfigurations);
    expect(cluster.getDesiredConfigByType("foo-site")).andReturn(fooConfig);
    expect(cluster.getDesiredConfigByType("bar-site")).andReturn(barConfig);
    expect(cluster.getDesiredConfigByType("baz-site")).andReturn(bazConfig);
    expect(cluster.getService("ZOOKEEPER")).andReturn(zookeeper);
    expect(cluster.getDesiredConfigByType("foo-type")).andReturn(fooConfig);
    expect(cluster.getDesiredConfigByType("bar-type")).andReturn(barConfig);
    expect(cluster.getDesiredConfigByType("baz-type")).andReturn(bazConfig);

    // setup the config helper for placeholder resolution
    @SuppressWarnings("unchecked")
    Provider<ConfigHelper> configHelperProvider = EasyMock.createNiceMock(Provider.class);
    ConfigHelper configHelper = EasyMock.createNiceMock(ConfigHelper.class);

    expect(configHelperProvider.get()).andStubReturn(configHelper);

    expect(configHelper.getDefaultProperties(stack211, "ZOOKEEPER")).andReturn(
        stack211Configs).anyTimes();

    expect(configHelper.getDefaultProperties(stack220, "ZOOKEEPER")).andReturn(
        stack220Configs).anyTimes();

    Capture<Map<String, Map<String, String>>> expectedConfigurationsCapture = EasyMock.newCapture();

    expect(configHelper.createConfigTypes(EasyMock.anyObject(Cluster.class),
        EasyMock.anyObject(StackId.class), EasyMock.anyObject(AmbariManagementController.class),
        EasyMock.capture(expectedConfigurationsCapture), EasyMock.anyObject(String.class),
        EasyMock.anyObject(String.class))).andReturn(true);

    EasyMock.replay(configHelperProvider, configHelper);

    // mock the service config DAO and replay it
    ServiceConfigEntity zookeeperServiceConfig = createNiceMock(ServiceConfigEntity.class);
    expect(zookeeperServiceConfig.getClusterConfigEntities()).andReturn(
        Lists.newArrayList(fooConfigEntity, barConfigEntity, bazConfigEntity));

    ServiceConfigDAO serviceConfigDAOMock;
    serviceConfigDAOMock = EasyMock.createNiceMock(ServiceConfigDAO.class);

    List<ServiceConfigEntity> latestServiceConfigs = Lists.newArrayList(zookeeperServiceConfig);
    expect(serviceConfigDAOMock.getLastServiceConfigsForService(EasyMock.anyLong(),
        eq("ZOOKEEPER"))).andReturn(latestServiceConfigs).once();

    replay(serviceConfigDAOMock);

    Map<String, UpgradePack> upgradePacks = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    UpgradePack upgradePack = upgradePacks.get("upgrade_to_new_stack");

    UpgradeContext context = createNiceMock(UpgradeContext.class);
    expect(context.getCluster()).andReturn(cluster).atLeastOnce();
    expect(context.getType()).andReturn(UpgradeType.ROLLING).atLeastOnce();
    expect(context.getDirection()).andReturn(Direction.UPGRADE).atLeastOnce();
    expect(context.getRepositoryVersion()).andReturn(repoVersion220).anyTimes();
    expect(context.getSupportedServices()).andReturn(Sets.newHashSet("ZOOKEEPER")).atLeastOnce();
    expect(context.getSourceRepositoryVersion(EasyMock.anyString())).andReturn(repoVersion211).atLeastOnce();
    expect(context.getTargetRepositoryVersion(EasyMock.anyString())).andReturn(repoVersion220).atLeastOnce();
    expect(context.getOrchestrationType()).andReturn(RepositoryType.STANDARD).anyTimes();
    expect(context.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(context.getHostRoleCommandFactory()).andStubReturn(injector.getInstance(HostRoleCommandFactory.class));
    expect(context.getRoleGraphFactory()).andStubReturn(injector.getInstance(RoleGraphFactory.class));
    expect(context.getUpgradePack()).andReturn(upgradePack).atLeastOnce();

    replayAll();

    UpgradeHelper upgradeHelper = injector.getInstance(UpgradeHelper.class);
    upgradeHelper.m_serviceConfigDAO = serviceConfigDAOMock;
    upgradeHelper.m_configHelperProvider = configHelperProvider;
    upgradeHelper.updateDesiredRepositoriesAndConfigs(context);

    Map<String, Map<String, String>> expectedConfigurations = expectedConfigurationsCapture.getValue();
    Map<String, String> expectedFooType = expectedConfigurations.get("foo-site");
    Map<String, String> expectedBarType = expectedConfigurations.get("bar-site");
    Map<String, String> expectedBazType = expectedConfigurations.get("baz-site");

    // As the upgrade pack did not have any Flume updates, its configs should
    // not be updated.
    assertEquals(3, expectedConfigurations.size());

    // the really important values are one-new and three-changed; one-new
    // indicates that the new stack value is changed since it was not customized
    // while three-changed represents that the customized value was preserved
    // even though the stack value changed
    assertEquals("one-new", expectedFooType.get("1"));
    assertEquals("one-A", expectedFooType.get("1A"));
    assertEquals("two", expectedBarType.get("2"));
    assertEquals("three-changed", expectedBazType.get("3"));
  }

  @Test
  public void testMergeConfigurationsWithClusterEnv() throws Exception {
    Cluster cluster = makeCluster(true);

    StackId oldStack = cluster.getDesiredStackVersion();
    StackId newStack = new StackId("HDP-2.5.0");

    ConfigFactory cf = injector.getInstance(ConfigFactory.class);

    Config clusterEnv = cf.createNew(cluster, "cluster-env", "version1",
        ImmutableMap.<String, String>builder().put("a", "b").build(),
        Collections.emptyMap());

    Config zooCfg = cf.createNew(cluster, "zoo.cfg", "version1",
        ImmutableMap.<String, String>builder().put("c", "d").build(),
        Collections.emptyMap());

    cluster.addDesiredConfig("admin", Sets.newHashSet(clusterEnv, zooCfg));

    Map<String, Map<String, String>> stackMap = new HashMap<>();
    stackMap.put("cluster-env", new HashMap<>());
    stackMap.put("hive-site", new HashMap<>());

    final Map<String, String> clusterEnvMap = new HashMap<>();

    Capture<Cluster> captureCluster = Capture.newInstance();
    Capture<StackId> captureStackId = Capture.newInstance();
    Capture<AmbariManagementController> captureAmc = Capture.newInstance();

    Capture<Map<String, Map<String, String>>> cap = new Capture<Map<String, Map<String, String>>>() {
      @Override
      public void setValue(Map<String, Map<String, String>> value) {
        if (value.containsKey("cluster-env")) {
          clusterEnvMap.putAll(value.get("cluster-env"));
        }
      }
    };

    Capture<String> captureUsername = Capture.newInstance();
    Capture<String> captureNote = Capture.newInstance();

    EasyMock.reset(m_configHelper);
    expect(m_configHelper.getDefaultProperties(oldStack, "HIVE")).andReturn(stackMap).atLeastOnce();
    expect(m_configHelper.getDefaultProperties(newStack, "HIVE")).andReturn(stackMap).atLeastOnce();
    expect(m_configHelper.getDefaultProperties(oldStack, "ZOOKEEPER")).andReturn(stackMap).atLeastOnce();
    expect(m_configHelper.getDefaultProperties(newStack, "ZOOKEEPER")).andReturn(stackMap).atLeastOnce();
    expect(m_configHelper.createConfigTypes(
        EasyMock.capture(captureCluster),
        EasyMock.capture(captureStackId),
        EasyMock.capture(captureAmc),
        EasyMock.capture(cap),

        EasyMock.capture(captureUsername),
        EasyMock.capture(captureNote))).andReturn(true);

    replay(m_configHelper);

    RepositoryVersionEntity repoVersionEntity = helper.getOrCreateRepositoryVersion(new StackId("HDP-2.5.0"), "2.5.0-1234");

    Map<String, Object> upgradeRequestMap = new HashMap<>();
    upgradeRequestMap.put(UpgradeResourceProvider.UPGRADE_DIRECTION, Direction.UPGRADE.name());
    upgradeRequestMap.put(UpgradeResourceProvider.UPGRADE_REPO_VERSION_ID, repoVersionEntity.getId().toString());
    upgradeRequestMap.put(UpgradeResourceProvider.UPGRADE_PACK, "upgrade_test_HDP-250");
    upgradeRequestMap.put(UpgradeResourceProvider.UPGRADE_SKIP_PREREQUISITE_CHECKS, Boolean.TRUE.toString());

    UpgradeContextFactory contextFactory = injector.getInstance(UpgradeContextFactory.class);
    UpgradeContext context = contextFactory.create(cluster, upgradeRequestMap);

    UpgradeHelper upgradeHelper = injector.getInstance(UpgradeHelper.class);
    upgradeHelper.updateDesiredRepositoriesAndConfigs(context);

    assertNotNull(clusterEnvMap);
    assertTrue(clusterEnvMap.containsKey("a"));

    // Do stacks cleanup
    stackManagerMock.invalidateCurrentPaths();
    ambariMetaInfo.init();
  }

  @Test
  public void testSequentialServiceChecks() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test_checks"));
    UpgradePack upgrade = upgrades.get("upgrade_test_checks");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();
    cluster.deleteService("HDFS", new DeleteHostComponentStatusMetaData());
    cluster.deleteService("YARN", new DeleteHostComponentStatusMetaData());

    UpgradeContext context = getMockUpgradeContext(cluster, Direction.UPGRADE,
        UpgradeType.ROLLING, repositoryVersion2110);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);
    assertEquals(5, groups.size());

    UpgradeGroupHolder serviceCheckGroup = groups.get(2);
    assertEquals(ServiceCheckGrouping.class, serviceCheckGroup.groupClass);
    assertEquals(3, serviceCheckGroup.items.size());

    StageWrapper wrapper = serviceCheckGroup.items.get(0);
    assertEquals(ServiceCheckGrouping.ServiceCheckStageWrapper.class, wrapper.getClass());
    assertTrue(wrapper.getText().contains("ZooKeeper"));

    // Do stacks cleanup
    stackManagerMock.invalidateCurrentPaths();
    ambariMetaInfo.init();
  }

  @Test
  public void testSequentialServiceChecksWithServiceCheckFailure() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test_checks"));
    UpgradePack upgrade = upgrades.get("upgrade_test_checks");
    assertNotNull(upgrade);

    // !!! fake skippable so we don't affect other tests
    for (Grouping g : upgrade.getAllGroups()) {
      if (g.name.equals("SERVICE_CHECK_1") || g.name.equals("SERVICE_CHECK_2")) {
        g.skippable = true;
      }
    }

    Cluster cluster = makeCluster();
    cluster.deleteService("HDFS", new DeleteHostComponentStatusMetaData());
    cluster.deleteService("YARN", new DeleteHostComponentStatusMetaData());

    UpgradeContext context = getMockUpgradeContext(cluster, Direction.UPGRADE, UpgradeType.ROLLING, repositoryVersion2110,
        RepositoryType.STANDARD, cluster.getServices().keySet(), m_masterHostResolver, false);
    expect(context.isServiceCheckFailureAutoSkipped()).andReturn(Boolean.TRUE).atLeastOnce();

    replay(context);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);
    assertEquals(5, groups.size());

    UpgradeGroupHolder serviceCheckGroup = groups.get(2);
    assertEquals(ServiceCheckGrouping.class, serviceCheckGroup.groupClass);
    assertEquals(4, serviceCheckGroup.items.size());

    StageWrapper wrapper = serviceCheckGroup.items.get(0);
    assertEquals(ServiceCheckGrouping.ServiceCheckStageWrapper.class, wrapper.getClass());
    assertTrue(wrapper.getText().contains("ZooKeeper"));

    wrapper = serviceCheckGroup.items.get(serviceCheckGroup.items.size()-1);
    assertTrue(wrapper.getText().equals("Verifying Skipped Failures"));

    // Do stacks cleanup
    stackManagerMock.invalidateCurrentPaths();
    ambariMetaInfo.init();
  }


  @Test
  public void testPrematureServiceChecks() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.containsKey("upgrade_test_checks"));
    UpgradePack upgrade = upgrades.get("upgrade_test_checks");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();
    cluster.deleteService("HDFS", new DeleteHostComponentStatusMetaData());
    cluster.deleteService("YARN", new DeleteHostComponentStatusMetaData());
    cluster.deleteService("ZOOKEEPER", new DeleteHostComponentStatusMetaData());

    UpgradeContext context = getMockUpgradeContext(cluster, Direction.UPGRADE,
        UpgradeType.ROLLING, repositoryVersion2110);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    assertEquals(3, groups.size());

    for (UpgradeGroupHolder holder : groups) {
      assertFalse(ServiceCheckGrouping.class.equals(holder.groupClass));
    }

    // Do stacks cleanup
    stackManagerMock.invalidateCurrentPaths();
    ambariMetaInfo.init();
  }

  /**
   * Tests that components added during the upgrade are scheduled for restart on
   * their future hosts.
   *
   * @throws Exception
   */
  @Test
  public void testAddComponentsDuringUpgrade() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");

    assertTrue(upgrades.containsKey("upgrade_test_add_component"));
    UpgradePack upgrade = upgrades.get("upgrade_test_add_component");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = getMockUpgradeContext(cluster, Direction.UPGRADE, UpgradeType.NON_ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    // 3 groups - stop, add component, restart
    assertEquals(3, groups.size());

    // ensure that the stop did not have extra hosts added to it
    UpgradeGroupHolder group = groups.get(0);
    assertEquals("STOP_HIVE", group.name);
    List<StageWrapper> stageWrappers = group.items;
    assertEquals(2, stageWrappers.size());

    // ensure that the restart has the future hosts
    group = groups.get(2);
    assertEquals("RESTART_HIVE", group.name);
    stageWrappers = group.items;
    assertEquals(4, stageWrappers.size());

    // Do stacks cleanup
    stackManagerMock.invalidateCurrentPaths();
    ambariMetaInfo.init();
  }

  @Test
  public void testParallelClients() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");

    assertTrue(upgrades.containsKey("upgrade_test_parallel_client"));
    UpgradePack upgrade = upgrades.get("upgrade_test_parallel_client");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    Service s = cluster.getService("ZOOKEEPER");
    ServiceComponent sc = s.addServiceComponent("ZOOKEEPER_CLIENT");
    sc.addServiceComponentHost("h1");
    sc.addServiceComponentHost("h2");
    sc.addServiceComponentHost("h3");

    UpgradeContext context = getMockUpgradeContext(cluster, Direction.UPGRADE, UpgradeType.NON_ROLLING);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);

    Optional<UpgradeGroupHolder> optional = groups.stream()
        .filter(g -> g.name.equals("ZK_CLIENTS"))
        .findAny();
    assertTrue(optional.isPresent());

    UpgradeGroupHolder holder = optional.get();

    assertEquals(3, holder.items.size());
    assertEquals(StageWrapper.Type.RESTART, holder.items.get(0).getType());
    assertEquals(StageWrapper.Type.SERVICE_CHECK, holder.items.get(1).getType());
    assertEquals(StageWrapper.Type.RESTART, holder.items.get(2).getType());

    // !!! this is a known issue - tasks wrappers should only wrap one task
    TaskWrapper taskWrapper = holder.items.get(0).getTasks().get(0);
    assertEquals(1, taskWrapper.getHosts().size());
    String host1 = taskWrapper.getHosts().iterator().next();

    taskWrapper = holder.items.get(1).getTasks().get(0);
    assertEquals(1, taskWrapper.getHosts().size());
    String host2 = taskWrapper.getHosts().iterator().next();
    assertEquals(host1, host2);

    taskWrapper = holder.items.get(2).getTasks().get(0);
    assertEquals(2, taskWrapper.getHosts().size());
  }

  @Test
  public void testOrchestrationOptions() throws Exception {

    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.2.0");
    assertTrue(upgrades.containsKey("upgrade_from_211"));

    UpgradePack upgrade = upgrades.get("upgrade_from_211");
    assertNotNull(upgrade);

    Cluster cluster = makeCluster();

    UpgradeContext context = getMockUpgradeContext(cluster, Direction.UPGRADE, UpgradeType.ROLLING, false);

    SimpleOrchestrationOptions options = new SimpleOrchestrationOptions(1);

    expect(context.getOrchestrationOptions()).andReturn(options).anyTimes();
    replay(context);

    List<UpgradeGroupHolder> groups = m_upgradeHelper.createSequence(upgrade, context);
    groups = groups.stream().filter(g -> g.name.equals("CORE_SLAVES")).collect(Collectors.toList());
    assertEquals(1, groups.size());

    List<StageWrapper> restarts = groups.get(0).items.stream().filter(sw ->
        sw.getType() == StageWrapper.Type.RESTART && sw.getText().contains("DataNode"))
        .collect(Collectors.toList());

    assertEquals("Expecting wrappers for each of 3 hosts", 3, restarts.size());

    options.m_count = 2;
    groups = m_upgradeHelper.createSequence(upgrade, context);
    groups = groups.stream().filter(g -> g.name.equals("CORE_SLAVES")).collect(Collectors.toList());
    assertEquals(1, groups.size());

    restarts = groups.get(0).items.stream().filter(sw ->
        sw.getType() == StageWrapper.Type.RESTART && sw.getText().contains("DataNode"))
        .collect(Collectors.toList());

    assertEquals("Expecting wrappers for each", 2, restarts.size());
  }

  /**
   * Builds a mock upgrade context using the following parameters:
   * <ul>
   * <li>{@link #repositoryVersion2110}
   * <li>{@link RepositoryType#STANDARD}
   * <li>All cluster services
   * <li>The mock master host resolve, {@link #m_masterHostResolver}
   * </ul>
   *
   * @param cluster
   *          the cluster for the upgrade
   * @param direction
   *          the upgrade direction
   * @param type
   *          the type of upgrade
   * @return a mock {@link UpgradeContext} which has already been replayed and
   *         is ready to use.
   */
  private UpgradeContext getMockUpgradeContext(Cluster cluster, Direction direction, UpgradeType type){
    return getMockUpgradeContext(cluster, direction, type, repositoryVersion2210);
  }

  /**
   * Builds a mock upgrade context using the following parameters:
   * <ul>
   * <li>{@link #repositoryVersion2110}
   * <li>{@link RepositoryType#STANDARD}
   * <li>All cluster services
   * <li>The mock master host resolve, {@link #m_masterHostResolver}
   * </ul>
   *
   * @param cluster
   *          the cluster for the upgrade
   * @param direction
   *          the upgrade direction
   * @param type
   *          the type of upgrade
   * @param replay
   *          {@code true} to replay the mock object before returning it,
   *          {@code false} otherwise.
   * @return a mock {@link UpgradeContext}
   */
  private UpgradeContext getMockUpgradeContext(Cluster cluster, Direction direction, UpgradeType type, boolean replay){
    return getMockUpgradeContext(cluster, direction, type, repositoryVersion2210,
        RepositoryType.STANDARD, cluster.getServices().keySet(), m_masterHostResolver, replay);
  }

  /**
   * Builds a mock upgrade context using the following parameters:
   * <ul>
   * <li>{@link RepositoryType#STANDARD}
   * <li>All cluster services
   * <li>The mock master host resolve, {@link #m_masterHostResolver}
   * </ul>
   *
   * @param cluster
   *          the cluster for the upgrade
   * @param direction
   *          the upgrade direction
   * @param type
   *          the type of upgrade
   * @param repositoryVersion
   *          the repository version to use for the upgrade or downgrade.
   * @return a mock {@link UpgradeContext} which has already been replayed and
   *         is ready to use.
   */
  private UpgradeContext getMockUpgradeContext(Cluster cluster, Direction direction,
      UpgradeType type, RepositoryVersionEntity repositoryVersion) {
    Set<String> allServices = cluster.getServices().keySet();
    return getMockUpgradeContext(cluster, direction, type, repositoryVersion,
        RepositoryType.STANDARD, allServices);
  }

  /**
   * Builds a mock upgrade context using the following parameters:
   * <ul>
   * <li>The mock master host resolve, {@link #m_masterHostResolver}
   * </ul>
   *
   * @param cluster
   *          the cluster for the upgrade
   * @param direction
   *          the upgrade direction
   * @param type
   *          the type of upgrade
   * @param repositoryVersion
   *          the repository version to use for the upgrade or downgrade.
   * @param repositoryType
   *          the type of repository for the upgrade (patch, standard, etc).
   * @param services
   *          the services participating in the upgrade. This should typically
   *          be all services, unless the {@link RepositoryType#PATCH} is used.
   * @return a mock {@link UpgradeContext} which has already been replayed and
   *         is ready to use.
   */
  private UpgradeContext getMockUpgradeContext(Cluster cluster, Direction direction,
      UpgradeType type, RepositoryVersionEntity repositoryVersion, RepositoryType repositoryType,
      Set<String> services) {
    return getMockUpgradeContext(cluster, direction, type, repositoryVersion,
        repositoryType, services, m_masterHostResolver, true);
  }

  /**
   * Builds a mock upgrade context using the following parameters:
   * <ul>
   * <li>{@link RepositoryType#STANDARD}
   * <li>All cluster services
   * </ul>
   *
   * @param cluster
   *          the cluster for the upgrade
   * @param direction
   *          the upgrade direction
   * @param type
   *          the type of upgrade
   * @param repositoryVersion
   *          the repository version to use for the upgrade or downgrade.
   * @return a mock {@link UpgradeContext} which has already been replayed and
   *         is ready to use.
   */
  private UpgradeContext getMockUpgradeContextNoReplay(Cluster cluster, Direction direction,
      UpgradeType type, RepositoryVersionEntity repositoryVersion) {
    Set<String> allServices = cluster.getServices().keySet();

    return getMockUpgradeContext(cluster, direction, type, repositoryVersion,
        RepositoryType.STANDARD, allServices, null, false);
  }

  /**
   * Builds a mock upgrade context using only the supplied method arguments.
   *
   * @param cluster
   *          the cluster for the upgrade
   * @param direction
   *          the upgrade direction
   * @param type
   *          the type of upgrade
   * @param repositoryVersion
   *          the repository version to use for the upgrade or downgrade.
   * @param repositoryType
   *          the type of repository for the upgrade (patch, standard, etc).
   * @param services
   *          the services participating in the upgrade. This should typically
   *          be all services, unless the {@link RepositoryType#PATCH} is used
   * @param resolver
   *          the master hsot resolver to use when determining how to
   *          orchestrate hosts.
   * @param replay
   *          {@code true} to replay the mock object before returning it,
   *          {@code false} otherwise.
   * @return a mock {@link UpgradeContext} which has already been replayed and
   *         is ready to use.
   */
  private UpgradeContext getMockUpgradeContext(Cluster cluster, Direction direction,
      UpgradeType type, RepositoryVersionEntity repositoryVersion, final RepositoryType repositoryType,
      Set<String> services, MasterHostResolver resolver, boolean replay) {
    UpgradeContext context = EasyMock.createNiceMock(UpgradeContext.class);
    expect(context.getCluster()).andReturn(cluster).anyTimes();
    expect(context.getType()).andReturn(type).anyTimes();
    expect(context.getDirection()).andReturn(direction).anyTimes();
    expect(context.getRepositoryVersion()).andReturn(repositoryVersion).anyTimes();
    expect(context.getSupportedServices()).andReturn(services).anyTimes();
    expect(context.getOrchestrationType()).andReturn(repositoryType).anyTimes();
    expect(context.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(context.getHostRoleCommandFactory()).andStubReturn(
        injector.getInstance(HostRoleCommandFactory.class));
    expect(context.getRoleGraphFactory()).andStubReturn(
        injector.getInstance(RoleGraphFactory.class));

    // only set this if supplied
    if (null != resolver) {
      expect(context.getResolver()).andReturn(resolver).anyTimes();
    }

    final Map<String, RepositoryVersionEntity> targetRepositoryVersions = new HashMap<>();
    for( String serviceName : services ){
      targetRepositoryVersions.put(serviceName, repositoryVersion);
    }

    final Capture<String> repoVersionServiceName = EasyMock.newCapture();
    expect(
        context.getTargetRepositoryVersion(EasyMock.capture(repoVersionServiceName))).andAnswer(
            new IAnswer<RepositoryVersionEntity>() {
              @Override
              public RepositoryVersionEntity answer() {
                return targetRepositoryVersions.get(repoVersionServiceName.getValue());
              }
            }).anyTimes();

    final Capture<String> serviceNameSupported = EasyMock.newCapture();
    expect(context.isServiceSupported(EasyMock.capture(serviceNameSupported))).andAnswer(
        new IAnswer<Boolean>() {
          @Override
          public Boolean answer() {
            return targetRepositoryVersions.containsKey(serviceNameSupported.getValue());
          }
        }).anyTimes();


    final Map<String, String> serviceNames = new HashMap<>();
    final Capture<String> serviceDisplayNameArg1 = EasyMock.newCapture();
    final Capture<String> serviceDisplayNameArg2 = EasyMock.newCapture();

    context.setServiceDisplay(EasyMock.capture(serviceDisplayNameArg1), EasyMock.capture(serviceDisplayNameArg2));
    expectLastCall().andAnswer(
        new IAnswer<Object>() {
          @Override
          public Object answer() {
            serviceNames.put(serviceDisplayNameArg1.getValue(), serviceDisplayNameArg2.getValue());
            return null;
          }
        }).anyTimes();


    final Map<String, String> componentNames = new HashMap<>();
    final Capture<String> componentDisplayNameArg1 = EasyMock.newCapture();
    final Capture<String> componentDisplayNameArg2 = EasyMock.newCapture();
    final Capture<String> componentDisplayNameArg3 = EasyMock.newCapture();

    context.setComponentDisplay(EasyMock.capture(componentDisplayNameArg1),
        EasyMock.capture(componentDisplayNameArg2), EasyMock.capture(componentDisplayNameArg3));

    expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() {
        componentNames.put(
            componentDisplayNameArg1.getValue() + ":" + componentDisplayNameArg2.getValue(),
            componentDisplayNameArg3.getValue());
        return null;
      }
    }).anyTimes();

    final Capture<String> getServiceDisplayArgument1 = EasyMock.newCapture();
    expect(
        context.getServiceDisplay(EasyMock.capture(getServiceDisplayArgument1))).andAnswer(
            new IAnswer<String>() {
              @Override
              public String answer() {
                return serviceNames.get(getServiceDisplayArgument1.getValue());
              }
            }).anyTimes();

    final Capture<String> getComponentDisplayArgument1 = EasyMock.newCapture();
    final Capture<String> getComponentDisplayArgument2 = EasyMock.newCapture();
    expect(context.getComponentDisplay(EasyMock.capture(getComponentDisplayArgument1),
        EasyMock.capture(getComponentDisplayArgument2))).andAnswer(new IAnswer<String>() {
          @Override
          public String answer() {
            return componentNames.get(getComponentDisplayArgument1.getValue() + ":"
                + getComponentDisplayArgument2.getValue());
          }
        }).anyTimes();

    final Capture<UpgradeScope> isScopedCapture = EasyMock.newCapture();
    expect(context.isScoped(EasyMock.capture(isScopedCapture))).andStubAnswer(
        new IAnswer<Boolean>() {
          @Override
          public Boolean answer() throws Throwable {
            UpgradeScope scope = isScopedCapture.getValue();
            if (scope == UpgradeScope.ANY) {
              return true;
            }

            if (scope == UpgradeScope.PARTIAL) {
              return repositoryType != RepositoryType.STANDARD;
            }

            return repositoryType == RepositoryType.STANDARD;
          }
        });

    if (replay) {
      replay(context);
    }

    return context;
  }

  /**
   * Extend {@link org.apache.ambari.server.stack.MasterHostResolver} in order
   * to overwrite the JMX methods.
   */
  private class MockMasterHostResolver extends MasterHostResolver {

    public MockMasterHostResolver(Cluster cluster, ConfigHelper configHelper, UpgradeContext context) {
      super(cluster, configHelper, context);
    }

    /**
     * Mock the call to get JMX Values.
     * @param hostname host name
     * @param port port number
     * @param beanName if asQuery is false, then search for this bean name
     * @param attributeName if asQuery is false, then search for this attribute name
     * @param asQuery whether to search bean or query
     * @param encrypted true if using https instead of http.
     * @return
     */
    @Override
    public String queryJmxBeanValue(String hostname, int port, String beanName, String attributeName,
                                    boolean asQuery, boolean encrypted) {

      if (beanName.equalsIgnoreCase("Hadoop:service=NameNode,name=NameNodeStatus") && attributeName.equalsIgnoreCase("State") && asQuery) {
        switch (hostname) {
          case "H1":
            return Status.ACTIVE.toString();
          case "H2":
            return Status.STANDBY.toString();
          case "H3":
            return Status.ACTIVE.toString();
          case "H4":
            return Status.STANDBY.toString();

          default:
            return "UNKNOWN_NAMENODE_STATUS_FOR_THIS_HOST";
        }
      }
      return  "NOT_MOCKED";
    }
  }

  private class MockModule implements Module {

    @Override
    public void configure(Binder binder) {
      binder.install(new FactoryModuleBuilder().build(UpgradeContextFactory.class));
      binder.bind(ConfigHelper.class).toInstance(m_configHelper);
      binder.bind(AgentConfigsHolder.class).toInstance(createNiceMock(AgentConfigsHolder.class));
    }
  }

  private static class BadMasterHostResolver extends MasterHostResolver {

    public BadMasterHostResolver(Cluster cluster, ConfigHelper configHelper, UpgradeContext context) {
      super(cluster, configHelper, context);
    }

    @Override
    protected String queryJmxBeanValue(String hostname, int port, String beanName,
        String attributeName, boolean asQuery, boolean encrypted) {
      return null;
    }

  }

  private static class SimpleOrchestrationOptions implements OrchestrationOptions {
    private int m_count;

    private SimpleOrchestrationOptions(int count) {
      m_count = count;
    }

    @Override
    public int getConcurrencyCount(ClusterInformation cluster, String service, String component) {
      return m_count;
    }
  }
}
