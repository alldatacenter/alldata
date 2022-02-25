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
package org.apache.ambari.server.state.services;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.dao.StageDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.orm.entities.StageEntityPK;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.stack.upgrade.Direction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Tests {@link org.apache.ambari.server.state.services.RetryUpgradeActionService}.
 */
public class RetryUpgradeActionServiceTest {

  private Injector injector;

  private Clusters clusters;
  private RepositoryVersionDAO repoVersionDAO;
  private UpgradeDAO upgradeDAO;
  private RequestDAO requestDAO;
  private StageDAO stageDAO;
  private HostRoleCommandDAO hostRoleCommandDAO;
  private OrmTestHelper helper;

  // Instance variables shared by all tests
  String clusterName = "c1";
  Cluster cluster;
  StackId stack220 = new StackId("HDP-2.2.0");
  StackEntity stackEntity220;
  Long upgradeRequestId = 1L;
  Long stageId = 1L;

  @Before
  public void before() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    clusters = injector.getInstance(Clusters.class);
    repoVersionDAO = injector.getInstance(RepositoryVersionDAO.class);
    upgradeDAO = injector.getInstance(UpgradeDAO.class);
    requestDAO = injector.getInstance(RequestDAO.class);
    stageDAO = injector.getInstance(StageDAO.class);
    hostRoleCommandDAO = injector.getInstance(HostRoleCommandDAO.class);
    helper = injector.getInstance(OrmTestHelper.class);
    stackEntity220 = helper.createStack(stack220);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  /**
   * Test the gauva service allows retrying certain failed actions during a stack upgrade.
   * Case 1: No cluster => no-op
   * Case 2: Cluster and valid timeout, but no active upgrade => no-op
   * Case 3: Cluster with an active upgrade, but no HOLDING_FAILED|HOLDING_TIMEDOUT commands => no-op
   * Case 4: Cluster with an active upgrade that contains a failed task in HOLDING_FAILED that
   * does NOT meet conditions to be retried => no-op
   * Case 5: Cluster with an active upgrade that contains a failed task in HOLDING_FAILED that
   * DOES meet conditions to be retried and has values for start time and original start time => retries the task
   * * Case 6: Cluster with an active upgrade that contains a failed task in HOLDING_TIMEDOUT that
   * DOES meet conditions to be retriedand does not have values for start time or original start time => retries the task
   * Case 7: Cluster with an active upgrade that contains a failed task in HOLDING_FAILED that
   * was already retried and has now expired => no-op
   * Case 8: Cluster with an active upgrade that contains a failed task in HOLDING_FAILED, but it is a critical task
   * during Finalize Cluster, which should not be retried => no-op
   * @throws Exception
   */
  @Test
  public void test() throws Exception {
    int timeoutMins = 1;
    RetryUpgradeActionService service = injector.getInstance(RetryUpgradeActionService.class);
    service.startUp();

    // Case 1: No cluster
    service.runOneIteration();

    // Case 2: Cluster and valid timeout, but no active upgrade
    createCluster();
    service.setMaxTimeout(timeoutMins);
    service.runOneIteration();

    // Case 3: Cluster with an active upgrade, but no HOLDING_FAILED|HOLDING_TIMEDOUT commands.
    prepareUpgrade();

    // Run the service
    service.runOneIteration();

    // Assert all commands in PENDING
    List<HostRoleCommandEntity> commands = hostRoleCommandDAO.findAll();
    Assert.assertTrue(!commands.isEmpty());
    for (HostRoleCommandEntity hrc : commands) {
      if (hrc.getStatus() == HostRoleStatus.PENDING) {
        Assert.fail("Did not expect any HostRoleCommands to be PENDING");
      }
    }

    // Case 4: Cluster with an active upgrade that contains a failed task in HOLDING_FAILED that does NOT meet conditions to be retried.
    StageEntityPK primaryKey = new StageEntityPK();
    primaryKey.setRequestId(upgradeRequestId);
    primaryKey.setStageId(stageId);

    StageEntity stageEntity = stageDAO.findByPK(primaryKey);

    HostRoleCommandEntity hrc2 = new HostRoleCommandEntity();
    hrc2.setStage(stageEntity);
    hrc2.setStatus(HostRoleStatus.HOLDING_FAILED);
    hrc2.setRole(Role.ZOOKEEPER_SERVER);
    hrc2.setRoleCommand(RoleCommand.RESTART);
    hrc2.setRetryAllowed(false);
    hrc2.setAutoSkipOnFailure(false);
    stageEntity.getHostRoleCommands().add(hrc2);

    hostRoleCommandDAO.create(hrc2);
    stageDAO.merge(stageEntity);

    // Run the service
    service.runOneIteration();

    commands = hostRoleCommandDAO.findAll();
    Assert.assertTrue(!commands.isEmpty() && commands.size() == 2);
    for (HostRoleCommandEntity hrc : commands) {
      if (hrc.getStatus() == HostRoleStatus.PENDING) {
        Assert.fail("Did not expect any HostRoleCommands to be PENDING");
      }
    }

    // Case 5: Cluster with an active upgrade that contains a failed task in HOLDING_FAILED that DOES meet conditions to be retried.
    long now = System.currentTimeMillis();
    hrc2.setRetryAllowed(true);
    hrc2.setOriginalStartTime(now);
    hostRoleCommandDAO.merge(hrc2);

    // Run the service
    service.runOneIteration();

    // Ensure that task 2 transitioned from HOLDING_FAILED to PENDING
    Assert.assertEquals(HostRoleStatus.PENDING, hostRoleCommandDAO.findByPK(hrc2.getTaskId()).getStatus());

    // Case 6: Cluster with an active upgrade that contains a failed task in HOLDING_FAILED that DOES meet conditions to be retried.
    hrc2.setStatus(HostRoleStatus.HOLDING_TIMEDOUT);
    hrc2.setRetryAllowed(true);
    hrc2.setOriginalStartTime(-1L);
    hrc2.setStartTime(-1L);
    hrc2.setLastAttemptTime(-1L);
    hrc2.setEndTime(-1L);
    hrc2.setAttemptCount((short) 0);
    hostRoleCommandDAO.merge(hrc2);

    // Run the service
    service.runOneIteration();

    // Ensure that task 2 transitioned from HOLDING_TIMEDOUT to PENDING
    Assert.assertEquals(HostRoleStatus.PENDING, hostRoleCommandDAO.findByPK(hrc2.getTaskId()).getStatus());

    // Case 7: Cluster with an active upgrade that contains a failed task in HOLDING_FAILED that was already retried and has now expired.
    now = System.currentTimeMillis();
    hrc2.setOriginalStartTime(now - (timeoutMins * 60000) - 1);
    hrc2.setStatus(HostRoleStatus.HOLDING_FAILED);
    hostRoleCommandDAO.merge(hrc2);

    // Run the service
    service.runOneIteration();

    Assert.assertEquals(HostRoleStatus.HOLDING_FAILED, hostRoleCommandDAO.findByPK(hrc2.getTaskId()).getStatus());

    // Case 8: Cluster with an active upgrade that contains a failed task in HOLDING_FAILED, but it is a critical task
    // during Finalize Cluster, which should not be retried.
    now = System.currentTimeMillis();
    hrc2.setOriginalStartTime(now);
    hrc2.setStatus(HostRoleStatus.HOLDING_FAILED);
    hrc2.setCustomCommandName("org.apache.ambari.server.serveraction.upgrades.FinalizeUpgradeAction");
    hostRoleCommandDAO.merge(hrc2);

    // Run the service
    service.runOneIteration();

    Assert.assertEquals(HostRoleStatus.HOLDING_FAILED, hostRoleCommandDAO.findByPK(hrc2.getTaskId()).getStatus());
  }

  /**
   * Create a cluster for stack HDP 2.2.0
   * @throws AmbariException
   */
  private void createCluster() throws AmbariException {

    clusters.addCluster(clusterName, stack220);
    cluster = clusters.getCluster("c1");

    RepositoryVersionEntity repoVersionEntity = new RepositoryVersionEntity();
    repoVersionEntity.setDisplayName("Initial Version");
    repoVersionEntity.addRepoOsEntities(new ArrayList<>());
    repoVersionEntity.setStack(stackEntity220);
    repoVersionEntity.setVersion("2.2.0.0");
    repoVersionDAO.create(repoVersionEntity);

    helper.getOrCreateRepositoryVersion(stack220, stack220.getStackVersion());
  }

  /**
   * Create a new repo version, plus the request and stage objects needed for a ROLLING stack upgrade.
   * @throws AmbariException
   */
  private void prepareUpgrade() throws AmbariException {
    RepositoryVersionEntity repoVersionEntity = new RepositoryVersionEntity();
    repoVersionEntity.setDisplayName("Version to Upgrade To");
    repoVersionEntity.addRepoOsEntities(new ArrayList<>());
    repoVersionEntity.setStack(stackEntity220);
    repoVersionEntity.setVersion("2.2.0.1");
    repoVersionDAO.create(repoVersionEntity);

    helper.getOrCreateRepositoryVersion(stack220, stack220.getStackVersion());

    RequestEntity requestEntity = new RequestEntity();
    requestEntity.setRequestId(upgradeRequestId);
    requestEntity.setClusterId(cluster.getClusterId());
    requestDAO.create(requestEntity);

    // Create the stage and add it to the request
    StageEntity stageEntity = new StageEntity();
    stageEntity.setRequest(requestEntity);
    stageEntity.setClusterId(cluster.getClusterId());
    stageEntity.setRequestId(upgradeRequestId);
    stageEntity.setStageId(stageId);
    requestEntity.setStages(Collections.singletonList(stageEntity));
    stageDAO.create(stageEntity);
    requestDAO.merge(requestEntity);

    UpgradeEntity upgrade = new UpgradeEntity();
    upgrade.setId(1L);
    upgrade.setRequestEntity(requestEntity);
    upgrade.setClusterId(cluster.getClusterId());
    upgrade.setUpgradePackage("some-name");
    upgrade.setUpgradePackStackId(new StackId((String) null));
    upgrade.setUpgradeType(UpgradeType.ROLLING);
    upgrade.setDirection(Direction.UPGRADE);
    upgrade.setRepositoryVersion(repoVersionEntity);
    upgradeDAO.create(upgrade);

    cluster.setUpgradeEntity(upgrade);

    // Create the task and add it to the stage
    HostRoleCommandEntity hrc1 = new HostRoleCommandEntity();

    hrc1.setStage(stageEntity);
    hrc1.setStatus(HostRoleStatus.COMPLETED);
    hrc1.setRole(Role.ZOOKEEPER_SERVER);
    hrc1.setRoleCommand(RoleCommand.RESTART);

    stageEntity.setHostRoleCommands(new ArrayList<>());
    stageEntity.getHostRoleCommands().add(hrc1);
    hostRoleCommandDAO.create(hrc1);
    stageDAO.merge(stageEntity);
  }
}
