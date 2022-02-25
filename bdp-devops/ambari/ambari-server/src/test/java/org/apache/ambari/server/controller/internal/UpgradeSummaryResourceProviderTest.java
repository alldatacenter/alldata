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
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.dao.StageDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.RepoOsEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.stack.upgrade.Direction;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.persist.Transactional;
import com.google.inject.util.Modules;

import junit.framework.Assert;

/**
 * UpgradeSummaryResourceProvider tests.
 */
public class UpgradeSummaryResourceProviderTest {

  private HostDAO hostDAO;
  private StackDAO stackDAO;
  private RepositoryVersionDAO repoVersionDAO;
  private UpgradeDAO upgradeDAO;
  private RequestDAO requestDAO;
  private StageDAO stageDAO;
  private HostRoleCommandDAO hrcDAO;

  @Inject
  private UpgradeHelper m_upgradeHelper;

  private Injector injector;
  private Clusters clusters;
  private OrmTestHelper helper;
  private AmbariManagementController amc;

  private String clusterName = "c1";

  @Before
  public void before() throws Exception {
    m_upgradeHelper = createNiceMock(UpgradeHelper.class);

    // Create an injector which will inject the mocks
    injector = Guice.createInjector(Modules.override(new InMemoryDefaultTestModule()).with(new MockModule()));
    injector.getInstance(GuiceJpaInitializer.class);
    helper = injector.getInstance(OrmTestHelper.class);
    amc = injector.getInstance(AmbariManagementController.class);

    Field field = AmbariServer.class.getDeclaredField("clusterController");
    field.setAccessible(true);
    field.set(null, amc);

    hostDAO = injector.getInstance(HostDAO.class);
    stackDAO = injector.getInstance(StackDAO.class);
    repoVersionDAO = injector.getInstance(RepositoryVersionDAO.class);
    upgradeDAO = injector.getInstance(UpgradeDAO.class);
    requestDAO = injector.getInstance(RequestDAO.class);
    stageDAO = injector.getInstance(StageDAO.class);
    hrcDAO = injector.getInstance(HostRoleCommandDAO.class);
  }

  @After
  public void after() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
    injector = null;
  }

  /**
   * Create a Cluster called c1 on HDP 2.2.0 with a single ZOOKEEPER_SERVER on Host h1
   */
  public void createCluster() throws Exception {
    StackEntity stackEntity = stackDAO.find("HDP", "2.2.0");

    List<RepoOsEntity> osRedhat6 = new ArrayList<>();
    RepoOsEntity repoOsEntity = new RepoOsEntity();
    repoOsEntity.setFamily("redhat6");
    repoOsEntity.setAmbariManaged(true);
    osRedhat6.add(repoOsEntity);

    RepositoryVersionEntity repoVersionEntity = new RepositoryVersionEntity();
    repoVersionEntity.setDisplayName("For Stack Version 2.2.0");
    repoVersionEntity.addRepoOsEntities(osRedhat6);
    repoVersionEntity.setStack(stackEntity);
    repoVersionEntity.setVersion("2.2.0.0");
    repoVersionDAO.create(repoVersionEntity);

    clusters = injector.getInstance(Clusters.class);

    StackId stackId = new StackId("HDP-2.2.0");
    clusters.addCluster(clusterName, stackId);
    Cluster cluster = clusters.getCluster("c1");

    helper.getOrCreateRepositoryVersion(stackId, "2.2.0.1-1234");

    clusters.addHost("h1");
    Host host = clusters.getHost("h1");
    Map<String, String> hostAttributes = new HashMap<>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6.4");
    host.setHostAttributes(hostAttributes);
    host.setState(HostState.HEALTHY);

    clusters.mapHostToCluster("h1", "c1");

    // add a single ZOOKEEPER server
    Service service = cluster.addService("ZOOKEEPER", repoVersionEntity);

    ServiceComponent component = service.addServiceComponent("ZOOKEEPER_SERVER");
    ServiceComponentHost sch = component.addServiceComponentHost("h1");
    sch.setVersion("2.2.0.0");

    component = service.addServiceComponent("ZOOKEEPER_CLIENT");
    sch = component.addServiceComponentHost("h1");
    sch.setVersion("2.2.0.0");
  }

  /**
   * Create a request, stage, and completed task.
   * @param cluster
   * @param upgradeRequestId
   * @param stageId
   */
  @Transactional
  void createCommands(Cluster cluster, Long upgradeRequestId, Long stageId) {
    HostEntity h1 = hostDAO.findByName("h1");

    RequestEntity requestEntity = requestDAO.findByPK(upgradeRequestId);

    // Create the stage and add it to the request
    StageEntity stageEntity = new StageEntity();
    stageEntity.setRequest(requestEntity);
    stageEntity.setClusterId(cluster.getClusterId());
    stageEntity.setRequestId(upgradeRequestId);
    stageEntity.setStageId(stageId);
    requestEntity.setStages(Collections.singletonList(stageEntity));
    stageDAO.create(stageEntity);
    requestDAO.merge(requestEntity);

    // Create the task and add it to the stage
    HostRoleCommandEntity hrc1 = new HostRoleCommandEntity();

    hrc1.setStage(stageEntity);
    hrc1.setStatus(HostRoleStatus.COMPLETED);
    hrc1.setRole(Role.ZOOKEEPER_SERVER);
    hrc1.setRoleCommand(RoleCommand.RESTART);
    hrc1.setHostEntity(h1);

    stageEntity.setHostRoleCommands(new ArrayList<>());
    stageEntity.getHostRoleCommands().add(hrc1);
    h1.getHostRoleCommandEntities().add(hrc1);

    hrcDAO.create(hrc1);
    hostDAO.merge(h1);
  }

  /**
   * Test UpgradeSummaryResourceProvider on several cases.
   * 1. Incorrect cluster name throws exception
   * 2. Upgrade with no tasks.
   * 3. Construct Upgrade with a single COMPLETED task. Resource should not have a failed reason.
   * 4. Append a failed task to the Upgrade. Resource should have a failed reason.
   * @throws Exception
   */
  @Test
  public void testGetUpgradeSummary() throws Exception {
    createCluster();

    Cluster cluster = clusters.getCluster(clusterName);
    ResourceProvider upgradeSummaryResourceProvider = createProvider(amc);

    // Case 1: Incorrect cluster name throws exception
    Request requestResource = PropertyHelper.getReadRequest();
    Predicate pBogus = new PredicateBuilder().property(UpgradeSummaryResourceProvider.UPGRADE_SUMMARY_CLUSTER_NAME).equals("bogus name").toPredicate();
    try {
      Set<Resource> resources = upgradeSummaryResourceProvider.getResources(requestResource, pBogus);
      assertTrue("Expected exception to be thrown", false);
    } catch (Exception e) {
      ;
    }

    // Case 2: Upgrade with no tasks.
    Long upgradeRequestId = 1L;

    Predicate p1 = new PredicateBuilder().property(UpgradeSummaryResourceProvider.UPGRADE_SUMMARY_CLUSTER_NAME).equals(clusterName).toPredicate();
    Predicate p2 = new PredicateBuilder().property(UpgradeSummaryResourceProvider.UPGRADE_SUMMARY_REQUEST_ID).equals(upgradeRequestId.toString()).toPredicate();
    Predicate p1And2 = new AndPredicate(p1, p2);

    Set<Resource> resources = upgradeSummaryResourceProvider.getResources(requestResource, p1And2);
    assertEquals(0, resources.size());

    RequestEntity requestEntity = new RequestEntity();
    requestEntity.setRequestId(1L);
    requestEntity.setClusterId(cluster.getClusterId());
    requestEntity.setStatus(HostRoleStatus.PENDING);
    requestEntity.setStages(new ArrayList<>());
    requestDAO.create(requestEntity);

    UpgradeEntity upgrade = new UpgradeEntity();
    upgrade.setRequestEntity(requestEntity);
    upgrade.setClusterId(cluster.getClusterId());
    upgrade.setId(1L);
    upgrade.setUpgradePackage("some-name");
    upgrade.setUpgradePackStackId(new StackId((String) null));
    upgrade.setUpgradeType(UpgradeType.ROLLING);
    upgrade.setDirection(Direction.UPGRADE);

    RepositoryVersionEntity repositoryVersion2201 = injector.getInstance(
        RepositoryVersionDAO.class).findByStackNameAndVersion("HDP", "2.2.0.1-1234");

    upgrade.setRepositoryVersion(repositoryVersion2201);
    upgradeDAO.create(upgrade);

    // Resource used to make assertions.
    Resource r;

    resources = upgradeSummaryResourceProvider.getResources(requestResource, p1And2);
    assertEquals(1, resources.size());
    r = resources.iterator().next();
    Assert.assertNull(r.getPropertyValue(UpgradeSummaryResourceProvider.UPGRADE_SUMMARY_FAIL_REASON));

    // Case 3: Construct Upgrade with a single COMPLETED task. Resource should not have a failed reason.
    Long currentStageId = 1L;
    createCommands(cluster, upgradeRequestId, currentStageId);

    resources = upgradeSummaryResourceProvider.getResources(requestResource, p1And2);
    assertEquals(1, resources.size());
    r = resources.iterator().next();
    Assert.assertNull(r.getPropertyValue(UpgradeSummaryResourceProvider.UPGRADE_SUMMARY_FAIL_REASON));

    // Case 4: Append a failed task to the Upgrade. Resource should have a failed reason.
    requestEntity = requestDAO.findByPK(upgradeRequestId);
    HostEntity h1 = hostDAO.findByName("h1");

    StageEntity nextStage = new StageEntity();
    nextStage.setRequest(requestEntity);
    nextStage.setClusterId(cluster.getClusterId());
    nextStage.setRequestId(upgradeRequestId);
    nextStage.setStageId(++currentStageId);
    requestEntity.getStages().add(nextStage);
    stageDAO.create(nextStage);
    requestDAO.merge(requestEntity);

    // Create the task and add it to the stage
    HostRoleCommandEntity hrc2 = new HostRoleCommandEntity();

    hrc2.setStage(nextStage);
    // Important that it's on its own stage with a FAILED status.
    hrc2.setStatus(HostRoleStatus.FAILED);
    hrc2.setRole(Role.ZOOKEEPER_SERVER);
    hrc2.setRoleCommand(RoleCommand.RESTART);
    hrc2.setCommandDetail("Restart ZOOKEEPER_SERVER");
    hrc2.setHostEntity(h1);

    nextStage.setHostRoleCommands(new ArrayList<>());
    nextStage.getHostRoleCommands().add(hrc2);
    h1.getHostRoleCommandEntities().add(hrc2);

    hrcDAO.create(hrc2);
    hostDAO.merge(h1);
    hrc2.setRequestId(upgradeRequestId);
    hrc2.setStageId(nextStage.getStageId());
    hrcDAO.merge(hrc2);

    Resource failedTask = new ResourceImpl(Resource.Type.Task);
    expect(m_upgradeHelper.getTaskResource(anyString(), anyLong(), anyLong(), anyLong())).andReturn(failedTask).anyTimes();
    replay(m_upgradeHelper);

    resources = upgradeSummaryResourceProvider.getResources(requestResource, p1And2);
    assertEquals(1, resources.size());
    r = resources.iterator().next();
    assertEquals("Failed calling Restart ZOOKEEPER_SERVER on host h1", r.getPropertyValue(UpgradeSummaryResourceProvider.UPGRADE_SUMMARY_FAIL_REASON));
  }

  /**
   * @param amc
   * @return the provider
   */
  private UpgradeSummaryResourceProvider createProvider(AmbariManagementController amc) {
    return new UpgradeSummaryResourceProvider(amc);
  }

  /**
   * Mock module that will bind UpgradeHelper to a mock instance.
   */
  private class MockModule implements Module {
    @Override
    public void configure(Binder binder) {
      binder.bind(UpgradeHelper.class).toInstance(m_upgradeHelper);
    }
  }
}
