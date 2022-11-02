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
package org.apache.ambari.server.serveraction.upgrades;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.ServiceConfigVersionResponse;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.orm.entities.UpgradeHistoryEntity;
import org.apache.ambari.server.serveraction.ServerAction;
import org.apache.ambari.server.stack.upgrade.Direction;
import org.apache.ambari.server.stack.upgrade.UpgradePack;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.UpgradeState;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.apache.ambari.spi.RepositoryType;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.UnitOfWork;

/**
 * Tests upgrade-related server side actions
 */
public class UpgradeActionTest {
  private static final String clusterName = "c1";

  private static final String HDP_2_1_1_0 = "2.1.1.0-1";
  private static final String HDP_2_1_1_1 = "2.1.1.1-2";

  private static final String HDP_2_2_0_1 = "2.2.0.1-3";

  private static final StackId HDP_21_STACK = new StackId("HDP-2.1.1");
  private static final StackId HDP_22_STACK = new StackId("HDP-2.2.0");

  private RepositoryVersionEntity sourceRepositoryVersion;

  private Injector m_injector;

  private AmbariManagementController amc;
  @Inject
  private OrmTestHelper m_helper;
  @Inject
  private RepositoryVersionDAO repoVersionDAO;
  @Inject
  private Clusters clusters;
  @Inject
  private HostVersionDAO hostVersionDAO;
  @Inject
  private HostDAO hostDAO;
  @Inject
  private HostRoleCommandFactory hostRoleCommandFactory;
  @Inject
  private ServiceFactory serviceFactory;
  @Inject
  private ServiceComponentFactory serviceComponentFactory;
  @Inject
  private ServiceComponentHostFactory serviceComponentHostFactory;
  @Inject
  private RequestDAO requestDAO;
  @Inject
  private UpgradeDAO upgradeDAO;
  @Inject
  private StackDAO stackDAO;
  @Inject
  private AmbariMetaInfo ambariMetaInfo;
  @Inject
  private FinalizeUpgradeAction finalizeUpgradeAction;
  @Inject
  private ConfigFactory configFactory;

  @Inject
  private RepositoryVersionDAO repositoryVersionDAO;

  @Inject
  private HostComponentStateDAO hostComponentStateDAO;

  private RepositoryVersionEntity repositoryVersion2110;
  private RepositoryVersionEntity repositoryVersion2111;
  private RepositoryVersionEntity repositoryVersion2201;

  @Before
  public void setup() throws Exception {
    m_injector = Guice.createInjector(new InMemoryDefaultTestModule());
    EventBusSynchronizer.synchronizeAmbariEventPublisher(m_injector);
    m_injector.getInstance(GuiceJpaInitializer.class);
    m_injector.injectMembers(this);
    m_injector.getInstance(UnitOfWork.class).begin();

    // Initialize AmbariManagementController
    amc = m_injector.getInstance(AmbariManagementController.class);

    Field field = AmbariServer.class.getDeclaredField("clusterController");
    field.setAccessible(true);
    field.set(null, amc);

    repositoryVersion2110 = m_helper.getOrCreateRepositoryVersion(HDP_21_STACK, HDP_2_1_1_0);
    repositoryVersion2111 = m_helper.getOrCreateRepositoryVersion(HDP_21_STACK, HDP_2_1_1_1);
    repositoryVersion2201 = m_helper.getOrCreateRepositoryVersion(HDP_22_STACK, HDP_2_2_0_1);
  }

  @After
  public void teardown() throws Exception {
    m_injector.getInstance(UnitOfWork.class).end();
    H2DatabaseCleaner.clearDatabase(m_injector.getProvider(EntityManager.class).get());
  }

  private void makeDowngradeCluster(RepositoryVersionEntity sourceRepoVersion,
      RepositoryVersionEntity targetRepoVersion) throws Exception {
    String hostName = "h1";

    clusters.addCluster(clusterName, sourceRepoVersion.getStackId());

    // add a host component
    clusters.addHost(hostName);

    Host host = clusters.getHost(hostName);

    Map<String, String> hostAttributes = new HashMap<>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6");
    host.setHostAttributes(hostAttributes);

    HostVersionEntity entity = new HostVersionEntity();
    entity.setHostEntity(hostDAO.findByName(hostName));
    entity.setRepositoryVersion(targetRepoVersion);
    entity.setState(RepositoryVersionState.INSTALLING);
    hostVersionDAO.create(entity);
  }

  private Cluster createUpgradeCluster(
      RepositoryVersionEntity sourceRepoVersion, String hostName) throws Exception {

    clusters.addCluster(clusterName, sourceRepoVersion.getStackId());
    Cluster cluster = clusters.getCluster(clusterName);

    // add a host component
    clusters.addHost(hostName);

    Host host = clusters.getHost(hostName);

    Map<String, String> hostAttributes = new HashMap<>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6");
    host.setHostAttributes(hostAttributes);

    // without this, HostEntity will not have a relation to ClusterEntity
    clusters.mapHostToCluster(hostName, clusterName);

    HostVersionEntity entity = new HostVersionEntity(hostDAO.findByName(hostName),
        sourceRepoVersion, RepositoryVersionState.INSTALLED);

    hostVersionDAO.create(entity);

    return cluster;
  }

  private void createHostVersions(RepositoryVersionEntity targetRepoVersion,
      String hostName) throws AmbariException {
    Cluster c = clusters.getCluster(clusterName);

    // create a single host with the UPGRADED HostVersionEntity
    HostDAO hostDAO = m_injector.getInstance(HostDAO.class);

    HostVersionEntity entity = new HostVersionEntity(hostDAO.findByName(hostName),
        targetRepoVersion, RepositoryVersionState.INSTALLED);

    hostVersionDAO.create(entity);

    // verify the UPGRADED host versions were created successfully
    List<HostVersionEntity> hostVersions = hostVersionDAO.findHostVersionByClusterAndRepository(
        c.getClusterId(), targetRepoVersion);

    assertEquals(1, hostVersions.size());
    assertEquals(RepositoryVersionState.INSTALLED, hostVersions.get(0).getState());
  }

  private void makeCrossStackUpgradeClusterAndSourceRepo(StackId sourceStack, String sourceRepo,
                                                         String hostName)throws Exception {

    clusters.addCluster(clusterName, sourceStack);

    StackEntity stackEntitySource = stackDAO.find(sourceStack.getStackName(), sourceStack.getStackVersion());

    assertNotNull(stackEntitySource);

    Cluster c = clusters.getCluster(clusterName);
    c.setCurrentStackVersion(sourceStack);
    c.setDesiredStackVersion(sourceStack);

    // add a host component
    clusters.addHost(hostName);
    Host host = clusters.getHost(hostName);

    Map<String, String> hostAttributes = new HashMap<>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6");
    host.setHostAttributes(hostAttributes);

    clusters.mapHostToCluster(hostName, clusterName);

    clusters.updateHostMappings(host);

    // Create the starting repo version
    sourceRepositoryVersion = m_helper.getOrCreateRepositoryVersion(sourceStack, sourceRepo);
  }

  private void makeCrossStackUpgradeTargetRepo(StackId targetStack, String targetRepo, String hostName) throws Exception{
    StackEntity stackEntityTarget = stackDAO.find(targetStack.getStackName(), targetStack.getStackVersion());
    assertNotNull(stackEntityTarget);

    m_helper.getOrCreateRepositoryVersion(new StackId(stackEntityTarget), targetRepo);

    // Start upgrading the newer repo

    HostDAO hostDAO = m_injector.getInstance(HostDAO.class);

    HostVersionEntity entity = new HostVersionEntity();
    entity.setHostEntity(hostDAO.findByName(hostName));
    entity.setRepositoryVersion(repoVersionDAO.findByStackAndVersion(targetStack, targetRepo));
    entity.setState(RepositoryVersionState.INSTALLED);
    hostVersionDAO.create(entity);
  }

  /***
   * During an Express Upgrade that crosses a stack version, Ambari calls UpdateDesiredRepositoryAction
   * in order to change the stack and apply configs.
   * The configs that are applied must be saved with the username that is passed in the role params.
   */
  @Test
  public void testExpressUpgradeUpdateDesiredRepositoryAction() throws Exception {
    StackId sourceStack = HDP_21_STACK;
    StackId targetStack = HDP_22_STACK;
    String sourceRepo = HDP_2_1_1_0;
    String hostName = "h1";

    // Must be a NON_ROLLING upgrade that jumps stacks in order for it to apply config changes.
    // That upgrade pack has changes for ZK and NameNode.
    String upgradePackName = "upgrade_nonrolling_new_stack";

    Map<String, UpgradePack> packs = ambariMetaInfo.getUpgradePacks(sourceStack.getStackName(), sourceStack.getStackVersion());
    Assert.assertTrue(packs.containsKey(upgradePackName));

    makeCrossStackUpgradeClusterAndSourceRepo(sourceStack, sourceRepo, hostName);

    Cluster cluster = clusters.getCluster(clusterName);

    // Install ZK and HDFS with some components
    Service zk = installService(cluster, "ZOOKEEPER", repositoryVersion2110);
    addServiceComponent(cluster, zk, "ZOOKEEPER_SERVER");
    addServiceComponent(cluster, zk, "ZOOKEEPER_CLIENT");
    createNewServiceComponentHost(cluster, "ZOOKEEPER", "ZOOKEEPER_SERVER", "h1");
    createNewServiceComponentHost(cluster, "ZOOKEEPER", "ZOOKEEPER_CLIENT", "h1");

    Service hdfs = installService(cluster, "HDFS", repositoryVersion2110);
    addServiceComponent(cluster, hdfs, "NAMENODE");
    addServiceComponent(cluster, hdfs, "DATANODE");
    createNewServiceComponentHost(cluster, "HDFS", "NAMENODE", "h1");
    createNewServiceComponentHost(cluster, "HDFS", "DATANODE", "h1");

    makeCrossStackUpgradeTargetRepo(targetStack, repositoryVersion2201.getVersion(), hostName);
    createUpgrade(cluster, repositoryVersion2201);

    Assert.assertNotNull(repositoryVersion2201);

    // Create some configs
    createConfigs(cluster);
    Collection<Config> configs = cluster.getAllConfigs();
    Assert.assertFalse(configs.isEmpty());

    Map<String, String> commandParams = new HashMap<>();
    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    Map<String, String> roleParams = new HashMap<>();

    // User that is performing the config changes
    String userName = "admin";
    roleParams.put(ServerAction.ACTION_USER_NAME, userName);
    executionCommand.setRoleParams(roleParams);
    executionCommand.setClusterName(clusterName);

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));

    // Call the action to change the desired stack and apply the configs from the Config Pack called by the Upgrade Pack.
    UpdateDesiredRepositoryAction action = m_injector.getInstance(UpdateDesiredRepositoryAction.class);
    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    List<ServiceConfigVersionResponse> configVersionsBefore = cluster.getServiceConfigVersions();

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(HostRoleStatus.COMPLETED.name(), report.getStatus());

    List<ServiceConfigVersionResponse> configVersionsAfter = cluster.getServiceConfigVersions();
    Assert.assertFalse(configVersionsAfter.isEmpty());

    assertTrue(configVersionsAfter.size() - configVersionsBefore.size() >= 1);
  }

  @Test
  public void testFinalizeDowngrade() throws Exception {
    makeDowngradeCluster(repositoryVersion2110, repositoryVersion2111);

    Cluster cluster = clusters.getCluster(clusterName);

    createUpgrade(cluster, repositoryVersion2111);

    Map<String, String> commandParams = new HashMap<>();
    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName(clusterName);

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));

    finalizeUpgradeAction.setExecutionCommand(executionCommand);
    finalizeUpgradeAction.setHostRoleCommand(hostRoleCommand);

    CommandReport report = finalizeUpgradeAction.execute(null);
    assertNotNull(report);
    assertEquals(HostRoleStatus.COMPLETED.name(), report.getStatus());

    for (HostVersionEntity entity : hostVersionDAO.findByClusterAndHost(clusterName, "h1")) {
      if (StringUtils.equals(entity.getRepositoryVersion().getVersion(), repositoryVersion2110.getVersion())) {
        assertEquals(RepositoryVersionState.CURRENT, entity.getState());
      } else {
        assertEquals(RepositoryVersionState.INSTALLED, entity.getState());
      }
    }
  }

  @Test
  public void testFinalizeUpgrade() throws Exception {
    String hostName = "h1";

    createUpgradeCluster(repositoryVersion2110, hostName);
    createHostVersions(repositoryVersion2111, hostName);

    Cluster cluster = clusters.getCluster(clusterName);

    createUpgrade(cluster, repositoryVersion2111);

    // Finalize the upgrade
    Map<String, String> commandParams = new HashMap<>();
    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName(clusterName);

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));

    finalizeUpgradeAction.setExecutionCommand(executionCommand);
    finalizeUpgradeAction.setHostRoleCommand(hostRoleCommand);

    // this should fail since the host versions have not moved to current
    CommandReport report = finalizeUpgradeAction.execute(null);
    assertEquals(HostRoleStatus.FAILED.name(), report.getStatus());

    List<HostVersionEntity> hostVersions = hostVersionDAO.findHostVersionByClusterAndRepository(
        cluster.getClusterId(), repositoryVersion2111);

    for (HostVersionEntity hostVersion : hostVersions) {
      hostVersion.setState(RepositoryVersionState.CURRENT);
    }

    report = finalizeUpgradeAction.execute(null);
    assertEquals(HostRoleStatus.COMPLETED.name(), report.getStatus());

    hostVersions = hostVersionDAO.findHostVersionByClusterAndRepository(cluster.getClusterId(),
        repositoryVersion2111);

    for (HostVersionEntity hostVersion : hostVersions) {
      Collection<HostComponentStateEntity> hostComponentStates = hostComponentStateDAO.findByHost(hostVersion.getHostName());
      for (HostComponentStateEntity hostComponentStateEntity: hostComponentStates) {
       assertEquals(UpgradeState.NONE, hostComponentStateEntity.getUpgradeState());
      }
    }
  }

  /**
   * Tests that finalize still works when there are hosts which are already
   * {@link RepositoryVersionState#CURRENT}.
   *
   * @throws Exception
   */
  @Test
  public void testFinalizeWithHostsAlreadyCurrent() throws Exception {
    String hostName = "h1";

    createUpgradeCluster(repositoryVersion2110, hostName);
    createHostVersions(repositoryVersion2111, hostName);

    // move the old version from CURRENT to INSTALLED and the new version from
    // UPGRADED to CURRENT - this will simulate what happens when a host is
    // removed before finalization and all hosts transition to CURRENT
    List<HostVersionEntity> hostVersions = hostVersionDAO.findAll();
    for (HostVersionEntity hostVersion : hostVersions) {
      if (hostVersion.getState() == RepositoryVersionState.CURRENT) {
        hostVersion.setState(RepositoryVersionState.INSTALLED);
      } else {
        hostVersion.setState(RepositoryVersionState.CURRENT);
      }

      hostVersionDAO.merge(hostVersion);
    }

    // Verify the repo before calling Finalize
    Cluster cluster = clusters.getCluster(clusterName);

    createUpgrade(cluster, repositoryVersion2111);

    // Finalize the upgrade
    Map<String, String> commandParams = new HashMap<>();

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName(clusterName);

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));

    finalizeUpgradeAction.setExecutionCommand(executionCommand);
    finalizeUpgradeAction.setHostRoleCommand(hostRoleCommand);

    CommandReport report = finalizeUpgradeAction.execute(null);
    assertNotNull(report);
    assertEquals(HostRoleStatus.COMPLETED.name(), report.getStatus());
  }

  /**
   * Tests that all host versions are correct after upgrade. This test will
   * ensure that the prior CURRENT versions are moved to INSTALLED while not
   * touching any others.
   *
   * @throws Exception
   */
  @Test
  public void testHostVersionsAfterUpgrade() throws Exception {
    String hostName = "h1";
    Cluster cluster = createUpgradeCluster(repositoryVersion2110, hostName);
    createHostVersions(repositoryVersion2111, hostName);
    createHostVersions(repositoryVersion2201, hostName);

    // Install ZK with some components
    Service zk = installService(cluster, "ZOOKEEPER", repositoryVersion2110);
    addServiceComponent(cluster, zk, "ZOOKEEPER_SERVER");
    addServiceComponent(cluster, zk, "ZOOKEEPER_CLIENT");
    createNewServiceComponentHost(cluster, "ZOOKEEPER", "ZOOKEEPER_SERVER", hostName);
    createNewServiceComponentHost(cluster, "ZOOKEEPER", "ZOOKEEPER_CLIENT", hostName);

    List<HostVersionEntity> hostVersions = hostVersionDAO.findAll();
    assertEquals(3, hostVersions.size());

    // repo 2110 - CURRENT (upgrading from)
    // repo 2111 - CURRENT (all hosts reported in during upgrade)
    // repo 2201 - NOT_REQUIRED (different stack)
    for (HostVersionEntity hostVersion : hostVersions) {
      RepositoryVersionEntity hostRepoVersion = hostVersion.getRepositoryVersion();
      if (repositoryVersion2110.equals(hostRepoVersion)) {
        hostVersion.setState(RepositoryVersionState.CURRENT);
      } else if (repositoryVersion2111.equals(hostRepoVersion)) {
        hostVersion.setState(RepositoryVersionState.CURRENT);
      } else {
        hostVersion.setState(RepositoryVersionState.NOT_REQUIRED);
      }

      hostVersionDAO.merge(hostVersion);
    }

    // upgrade to 2111
    createUpgrade(cluster, repositoryVersion2111);

    // push all services to the correct repo version for finalize
    Map<String, Service> services = cluster.getServices();
    assertTrue(services.size() > 0);
    for (Service service : services.values()) {
      service.setDesiredRepositoryVersion(repositoryVersion2111);
    }

    // push all components to the correct version
    List<HostComponentStateEntity> hostComponentStates = hostComponentStateDAO.findByHost(hostName);
    for (HostComponentStateEntity hostComponentState : hostComponentStates) {
      hostComponentState.setVersion(repositoryVersion2111.getVersion());
      hostComponentStateDAO.merge(hostComponentState);
    }

    Map<String, String> commandParams = new HashMap<>();
    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName(clusterName);

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));

    finalizeUpgradeAction.setExecutionCommand(executionCommand);
    finalizeUpgradeAction.setHostRoleCommand(hostRoleCommand);

    // finalize
    CommandReport report = finalizeUpgradeAction.execute(null);
    assertNotNull(report);
    assertEquals(HostRoleStatus.COMPLETED.name(), report.getStatus());

    for (HostVersionEntity hostVersion : hostVersions) {
      RepositoryVersionEntity hostRepoVersion = hostVersion.getRepositoryVersion();
      if (repositoryVersion2110.equals(hostRepoVersion)) {
        assertEquals(RepositoryVersionState.INSTALLED, hostVersion.getState());
      } else if (repositoryVersion2111.equals(hostRepoVersion)) {
        assertEquals(RepositoryVersionState.CURRENT, hostVersion.getState());
      } else {
        assertEquals(RepositoryVersionState.NOT_REQUIRED, hostVersion.getState());
      }
    }
  }

  /**
   * Tests the case where a revert happens on a patch upgrade and a new service
   * has been added which causes the repository to go OUT_OF_SYNC.
   *
   * @throws Exception
   */
  @Test
  public void testHostVersionsOutOfSyncAfterRevert() throws Exception {
    String hostName = "h1";
    Cluster cluster = createUpgradeCluster(repositoryVersion2110, hostName);
    createHostVersions(repositoryVersion2111, hostName);

    // Install ZK with some components (HBase is installed later to test the
    // logic of revert)
    Service zk = installService(cluster, "ZOOKEEPER", repositoryVersion2110);
    addServiceComponent(cluster, zk, "ZOOKEEPER_SERVER");
    addServiceComponent(cluster, zk, "ZOOKEEPER_CLIENT");
    createNewServiceComponentHost(cluster, "ZOOKEEPER", "ZOOKEEPER_SERVER", hostName);
    createNewServiceComponentHost(cluster, "ZOOKEEPER", "ZOOKEEPER_CLIENT", hostName);

    List<HostVersionEntity> hostVersions = hostVersionDAO.findAll();
    assertEquals(2, hostVersions.size());

    // repo 2110 - CURRENT
    // repo 2111 - CURRENT (PATCH)
    for (HostVersionEntity hostVersion : hostVersions) {
      hostVersion.setState(RepositoryVersionState.CURRENT);

      hostVersionDAO.merge(hostVersion);
    }

    // convert the repository into a PATCH repo (which should have ZK and HBase
    // as available services)
    repositoryVersion2111.setParent(repositoryVersion2110);
    repositoryVersion2111.setType(RepositoryType.PATCH);
    repositoryVersion2111.setVersionXml(hostName);
    repositoryVersion2111.setVersionXsd("version_definition.xsd");

    File patchVdfFile = new File("src/test/resources/hbase_version_test.xml");
    repositoryVersion2111.setVersionXml(
        IOUtils.toString(new FileInputStream(patchVdfFile), Charset.defaultCharset()));

    repositoryVersion2111 = repositoryVersionDAO.merge(repositoryVersion2111);

    // pretend like we patched
    UpgradeEntity upgrade = createUpgrade(cluster, repositoryVersion2111);
    upgrade.setOrchestration(RepositoryType.PATCH);
    upgrade.setRevertAllowed(true);
    upgrade = upgradeDAO.merge(upgrade);

    // add a service on the parent repo to cause the OUT_OF_SYNC on revert
    Service hbase = installService(cluster, "HBASE", repositoryVersion2110);
    addServiceComponent(cluster, hbase, "HBASE_MASTER");
    createNewServiceComponentHost(cluster, "HBASE", "HBASE_MASTER", hostName);

    // revert the patch
    UpgradeEntity revert = createRevert(cluster, upgrade);
    assertEquals(RepositoryType.PATCH, revert.getOrchestration());

    // push all services to the revert repo version for finalize
    Map<String, Service> services = cluster.getServices();
    assertTrue(services.size() > 0);
    for (Service service : services.values()) {
      service.setDesiredRepositoryVersion(repositoryVersion2110);
    }

    // push all components to the revert version
    List<HostComponentStateEntity> hostComponentStates = hostComponentStateDAO.findByHost(hostName);
    for (HostComponentStateEntity hostComponentState : hostComponentStates) {
      hostComponentState.setVersion(repositoryVersion2110.getVersion());
      hostComponentStateDAO.merge(hostComponentState);
    }

    Map<String, String> commandParams = new HashMap<>();
    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName(clusterName);

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));

    finalizeUpgradeAction.setExecutionCommand(executionCommand);
    finalizeUpgradeAction.setHostRoleCommand(hostRoleCommand);

    // finalize
    CommandReport report = finalizeUpgradeAction.execute(null);
    assertNotNull(report);
    assertEquals(HostRoleStatus.COMPLETED.name(), report.getStatus());

    for (HostVersionEntity hostVersion : hostVersions) {
      RepositoryVersionEntity hostRepoVersion = hostVersion.getRepositoryVersion();
      if (repositoryVersion2110.equals(hostRepoVersion)) {
        assertEquals(RepositoryVersionState.CURRENT, hostVersion.getState());
      } else {
        assertEquals(RepositoryVersionState.OUT_OF_SYNC, hostVersion.getState());
      }
    }
  }

  private ServiceComponentHost createNewServiceComponentHost(Cluster cluster, String svc,
                                                             String svcComponent, String hostName) throws AmbariException {
    Assert.assertNotNull(cluster.getConfigGroups());
    Service s = installService(cluster, svc, sourceRepositoryVersion);
    ServiceComponent sc = addServiceComponent(cluster, s, svcComponent);

    ServiceComponentHost sch = serviceComponentHostFactory.createNew(sc, hostName);

    sc.addServiceComponentHost(sch);
    sch.setDesiredState(State.INSTALLED);
    sch.setState(State.INSTALLED);
    return sch;
  }

  private Service installService(Cluster cluster, String serviceName,
      RepositoryVersionEntity repositoryVersionEntity) throws AmbariException {
    Service service = null;

    try {
      service = cluster.getService(serviceName);
    } catch (ServiceNotFoundException e) {
      service = serviceFactory.createNew(cluster, serviceName, repositoryVersionEntity);
      cluster.addService(service);
    }

    return service;
  }

  private ServiceComponent addServiceComponent(Cluster cluster, Service service,
      String componentName) throws AmbariException {
    ServiceComponent serviceComponent = null;
    try {
      serviceComponent = service.getServiceComponent(componentName);
    } catch (ServiceComponentNotFoundException e) {
      serviceComponent = serviceComponentFactory.createNew(service, componentName);
      service.addServiceComponent(serviceComponent);
      serviceComponent.setDesiredState(State.INSTALLED);
    }

    return serviceComponent;
  }

  private void createConfigs(Cluster cluster) {
    Map<String, String> properties = new HashMap<>();
    Map<String, Map<String, String>> propertiesAttributes = new HashMap<>();
    properties.put("a", "a1");
    properties.put("b", "b1");

    configFactory.createNew(cluster, "zookeeper-env", "version-" + System.currentTimeMillis(),
        properties, propertiesAttributes);

    properties.put("zookeeper_a", "value_1");
    properties.put("zookeeper_b", "value_2");

    configFactory.createNew(cluster, "hdfs-site", "version-" + System.currentTimeMillis(),
        properties, propertiesAttributes);

    properties.put("hdfs_a", "value_3");
    properties.put("hdfs_b", "value_4");

    configFactory.createNew(cluster, "core-site", "version-" + System.currentTimeMillis(),
        properties, propertiesAttributes);

    configFactory.createNew(cluster, "foo-site", "version-" + System.currentTimeMillis(),
        properties, propertiesAttributes);
  }

  /**
   * Creates an upgrade and associates it with the cluster.
   */
  private UpgradeEntity createUpgrade(Cluster cluster, RepositoryVersionEntity repositoryVersion)
      throws Exception {

    // create some entities for the finalize action to work with for patch
    // history
    RequestEntity requestEntity = new RequestEntity();
    requestEntity.setClusterId(cluster.getClusterId());
    requestEntity.setRequestId(1L);
    requestEntity.setStartTime(System.currentTimeMillis());
    requestEntity.setCreateTime(System.currentTimeMillis());
    requestDAO.create(requestEntity);

    UpgradeEntity upgradeEntity = new UpgradeEntity();
    upgradeEntity.setId(1L);
    upgradeEntity.setClusterId(cluster.getClusterId());
    upgradeEntity.setRequestEntity(requestEntity);
    upgradeEntity.setUpgradePackage("");
    upgradeEntity.setUpgradePackStackId(new StackId((String) null));
    upgradeEntity.setRepositoryVersion(repositoryVersion);
    upgradeEntity.setUpgradeType(UpgradeType.NON_ROLLING);

    Map<String, Service> services = cluster.getServices();
    for (String serviceName : services.keySet()) {
      Service service = services.get(serviceName);
      Map<String, ServiceComponent> components = service.getServiceComponents();
      for (String componentName : components.keySet()) {
        UpgradeHistoryEntity history = new UpgradeHistoryEntity();
        history.setUpgrade(upgradeEntity);
        history.setServiceName(serviceName);
        history.setComponentName(componentName);
        history.setFromRepositoryVersion(service.getDesiredRepositoryVersion());
        history.setTargetRepositoryVersion(repositoryVersion);
        upgradeEntity.addHistory(history);
      }
    }

    upgradeDAO.create(upgradeEntity);
    cluster.setUpgradeEntity(upgradeEntity);
    return upgradeEntity;
  }

  /**
   * Creates a revert based on an existing upgrade.
   */
  private UpgradeEntity createRevert(Cluster cluster, UpgradeEntity upgradeToRevert)
      throws Exception {

    // create some entities for the finalize action to work with for patch
    // history
    RequestEntity requestEntity = new RequestEntity();
    requestEntity.setClusterId(cluster.getClusterId());
    requestEntity.setRequestId(2L);
    requestEntity.setStartTime(System.currentTimeMillis());
    requestEntity.setCreateTime(System.currentTimeMillis());
    requestDAO.create(requestEntity);

    UpgradeEntity revert = new UpgradeEntity();
    revert.setId(2L);
    revert.setDirection(Direction.DOWNGRADE);
    revert.setClusterId(cluster.getClusterId());
    revert.setRequestEntity(requestEntity);
    revert.setUpgradePackage("");
    revert.setUpgradePackStackId(new StackId((String) null));
    revert.setRepositoryVersion(upgradeToRevert.getRepositoryVersion());
    revert.setUpgradeType(upgradeToRevert.getUpgradeType());
    revert.setOrchestration(upgradeToRevert.getOrchestration());


    for (UpgradeHistoryEntity historyToRevert : upgradeToRevert.getHistory()) {
      UpgradeHistoryEntity history = new UpgradeHistoryEntity();
      history.setUpgrade(revert);
      history.setServiceName(historyToRevert.getServiceName());
      history.setComponentName(historyToRevert.getComponentName());
      history.setFromRepositoryVersion(upgradeToRevert.getRepositoryVersion());
      history.setTargetRepositoryVersion(historyToRevert.getFromReposistoryVersion());
      revert.addHistory(history);

    }
    upgradeDAO.create(revert);
    cluster.setUpgradeEntity(revert);
    return revert;
  }

}
