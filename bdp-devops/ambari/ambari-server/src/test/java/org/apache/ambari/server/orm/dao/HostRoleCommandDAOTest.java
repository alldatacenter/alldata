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
package org.apache.ambari.server.orm.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

/**
 * Tests {@link HostRoleCommandDAO}.
 */
public class HostRoleCommandDAOTest {

  private Injector m_injector;
  private ClusterDAO m_clusterDAO;
  private StageDAO m_stageDAO;
  private HostRoleCommandDAO m_hostRoleCommandDAO;
  private HostDAO m_hostDAO;
  private RequestDAO m_requestDAO;

  @Before
  public void setup() throws Exception {
    m_injector = Guice.createInjector(new InMemoryDefaultTestModule());
    m_injector.getInstance(GuiceJpaInitializer.class);
    m_injector.getInstance(AmbariMetaInfo.class);

    m_clusterDAO = m_injector.getInstance(ClusterDAO.class);
    m_stageDAO = m_injector.getInstance(StageDAO.class);
    m_hostRoleCommandDAO = m_injector.getInstance(HostRoleCommandDAO.class);
    m_hostDAO = m_injector.getInstance(HostDAO.class);
    m_requestDAO = m_injector.getInstance(RequestDAO.class);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabase(m_injector.getProvider(EntityManager.class).get());
  }

  /**
   * Tests finding all tasks between a range of stages.
   */
  @Test
  public void testFindTasksBetweenStages() {
    OrmTestHelper helper = m_injector.getInstance(OrmTestHelper.class);
    helper.createDefaultData();

    Long requestId = Long.valueOf(100L);
    ClusterEntity clusterEntity = m_clusterDAO.findByName("test_cluster1");

    RequestEntity requestEntity = new RequestEntity();
    requestEntity.setRequestId(requestId);
    requestEntity.setClusterId(clusterEntity.getClusterId());
    requestEntity.setStages(new ArrayList<>());
    m_requestDAO.create(requestEntity);

    AtomicLong stageId = new AtomicLong(1);
    HostEntity host = m_hostDAO.findByName("test_host1");
    host.setHostRoleCommandEntities(new ArrayList<>());

    createStage(stageId.getAndIncrement(), 3, host, requestEntity, HostRoleStatus.COMPLETED);
    createStage(stageId.getAndIncrement(), 2, host, requestEntity, HostRoleStatus.SKIPPED_FAILED);
    createStage(stageId.getAndIncrement(), 1, host, requestEntity, HostRoleStatus.ABORTED);

    List<HostRoleCommandEntity> tasks = m_hostRoleCommandDAO.findByStatusBetweenStages(requestId,
        HostRoleStatus.SKIPPED_FAILED, 1, 3);

    Assert.assertEquals(2, tasks.size());

    tasks = m_hostRoleCommandDAO.findByStatusBetweenStages(requestId, HostRoleStatus.SKIPPED_FAILED, 1, 1);
    Assert.assertEquals(0, tasks.size());
  }

  /**
   * Tests that setting the auto-skip feature of a {@link HostRoleCommandEntity}
   * is somewhat dependenant on the {@link StageEntity}'s support for it.
   */
  @Test
  public void testAutoSkipSupport() {
    OrmTestHelper helper = m_injector.getInstance(OrmTestHelper.class);
    helper.createDefaultData();

    Long requestId = Long.valueOf(100L);
    ClusterEntity clusterEntity = m_clusterDAO.findByName("test_cluster1");

    RequestEntity requestEntity = new RequestEntity();
    requestEntity.setRequestId(requestId);
    requestEntity.setClusterId(clusterEntity.getClusterId());
    requestEntity.setStages(new ArrayList<>());
    m_requestDAO.create(requestEntity);

    AtomicLong stageId = new AtomicLong(1);
    HostEntity host = m_hostDAO.findByName("test_host1");
    host.setHostRoleCommandEntities(new ArrayList<>());

    // start out with a stage that is skippable, supports auto skip, and has
    // auto skip tasks
    long stageIdAutoSkipAll = stageId.getAndIncrement();
    createStage(stageIdAutoSkipAll, 3, host, requestEntity, HostRoleStatus.PENDING, true,
        true, true);

    List<HostRoleCommandEntity> tasks = m_hostRoleCommandDAO.findByRequest(requestId);
    for (HostRoleCommandEntity task : tasks) {
      Assert.assertTrue(task.isFailureAutoSkipped());
    }

    // let's try a skippable stage that doesn't support auto skip
    long stageIdSkippableButNoAutoSkip = stageId.getAndIncrement();
    createStage(stageIdSkippableButNoAutoSkip, 3, host, requestEntity, HostRoleStatus.PENDING, true,
        false, true);

    tasks = m_hostRoleCommandDAO.findByRequest(requestId);
    for (HostRoleCommandEntity task : tasks) {
      StageEntity stage = task.getStage();
      if( stage.getStageId() == stageIdAutoSkipAll ){
        Assert.assertTrue(task.isFailureAutoSkipped());
      } else if( stage.getStageId() == stageIdSkippableButNoAutoSkip ){
        Assert.assertFalse(task.isFailureAutoSkipped());
      }
    }

    // ok, now unset them all
    m_hostRoleCommandDAO.updateAutomaticSkipOnFailure(requestId, false, false);
    tasks = m_hostRoleCommandDAO.findByRequest(requestId);
    for (HostRoleCommandEntity task : tasks) {
      Assert.assertFalse(task.isFailureAutoSkipped());
    }
  }

  /**
   * Creates a single stage with the specified number of commands.
   *
   * @param startStageId
   * @param count
   * @param hostEntity
   * @param requestEntity
   * @param status
   * @return
   */
  private void createStage(long startStageId, int count, HostEntity hostEntity,
      RequestEntity requestEntity, HostRoleStatus status) {
    createStage(startStageId, count, hostEntity, requestEntity, status, false, false, false);
  }

  /**
   * Creates a single stage with the specified number of commands.
   *
   * @param startStageId
   * @param count
   * @param hostEntity
   * @param requestEntity
   * @param status
   * @param skipStage
   * @param supportsAutoSkipOnFailure
   * @return
   */
  private void createStage(long startStageId, int count, HostEntity hostEntity,
      RequestEntity requestEntity, HostRoleStatus status, boolean skipStage,
      boolean supportsAutoSkipOnFailure, boolean autoSkipFailedCommandsInStage) {
    long stageId = startStageId;

    ClusterEntity clusterEntity = m_clusterDAO.findByName("test_cluster1");

    StageEntity stageEntity = new StageEntity();
    stageEntity.setClusterId(clusterEntity.getClusterId());
    stageEntity.setRequest(requestEntity);
    stageEntity.setStageId(stageId);
    stageEntity.setHostRoleCommands(new ArrayList<>());
    stageEntity.setSkippable(skipStage);
    stageEntity.setAutoSkipFailureSupported(supportsAutoSkipOnFailure);

    m_stageDAO.create(stageEntity);
    requestEntity.getStages().add(stageEntity);

    for (int i = 0; i < count; i++) {
      HostRoleCommandEntity commandEntity = new HostRoleCommandEntity();
      commandEntity.setRequestId(requestEntity.getRequestId());
      commandEntity.setStageId(stageId);
      commandEntity.setRoleCommand(RoleCommand.INSTALL);
      commandEntity.setStatus(status);
      commandEntity.setRole(Role.DATANODE);
      commandEntity.setHostEntity(hostEntity);
      commandEntity.setStage(stageEntity);
      commandEntity.setAutoSkipOnFailure(
          autoSkipFailedCommandsInStage && skipStage && supportsAutoSkipOnFailure);

      m_hostRoleCommandDAO.create(commandEntity);

      hostEntity.getHostRoleCommandEntities().add(commandEntity);
      hostEntity = m_hostDAO.merge(hostEntity);

      stageEntity.getHostRoleCommands().add(commandEntity);
      m_stageDAO.merge(stageEntity);
    }
  }
}
