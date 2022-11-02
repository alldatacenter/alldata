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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.internal.CalculatedStatus;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.orm.entities.StageEntityPK;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * RequestDAO unit tests
 */
public class RequestDAOTest {
  private Injector injector;
  private ClusterDAO clusterDAO;
  private StageDAO stageDAO;
  private HostRoleCommandDAO hostRoleCommandDAO;
  private HostDAO hostDAO;
  private RequestDAO requestDAO;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.getInstance(AmbariMetaInfo.class);

    clusterDAO = injector.getInstance(ClusterDAO.class);
    stageDAO = injector.getInstance(StageDAO.class);
    hostRoleCommandDAO = injector.getInstance(HostRoleCommandDAO.class);
    hostDAO = injector.getInstance(HostDAO.class);
    requestDAO = injector.getInstance(RequestDAO.class);

  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }


  @Test
  public void testFindAll() throws Exception {
    RequestDAO dao = injector.getInstance(RequestDAO.class);
    Set<Long> set = Collections.emptySet();
    List<RequestEntity> list = dao.findByPks(set);
    Assert.assertEquals(0, list.size());
  }

  @Test
  public void testFindAllRequestIds() throws Exception {
    RequestDAO dao = injector.getInstance(RequestDAO.class);
    List<Long> list = dao.findAllRequestIds(0, true);
    Assert.assertEquals(0, list.size());
  }


  @Test
  public void testCalculatedStatus() throws Exception {
    createGraph();

    RequestEntity requestEntity = requestDAO.findByPK(100L);

    // !!! accepted value
    CalculatedStatus calc1 = CalculatedStatus.statusFromStageEntities(
        requestEntity.getStages());

    // !!! aggregated value
    Map<Long, HostRoleCommandStatusSummaryDTO> map = hostRoleCommandDAO.findAggregateCounts(100L);
    CalculatedStatus calc2 = CalculatedStatus.statusFromStageSummary(map,
        map.keySet());

    Assert.assertEquals(HostRoleStatus.IN_PROGRESS, calc1.getStatus());
    Assert.assertEquals(calc1.getStatus(), calc2.getStatus());
    Assert.assertEquals(calc1.getPercent(), calc2.getPercent(), 0.01d);

    // !!! simulate an upgrade group
    Set<Long> group = new HashSet<>();
    group.add(2L);
    group.add(3L);
    group.add(4L);

    // !!! accepted
    List<StageEntity> stages = new ArrayList<>();
    StageEntityPK primaryKey = new StageEntityPK();
    primaryKey.setRequestId(requestEntity.getRequestId());
    primaryKey.setStageId(2L);

    StageEntity stage = stageDAO.findByPK(primaryKey);
    Assert.assertNotNull(stage);
    stages.add(stage);

    primaryKey.setStageId(3L);
    stage = stageDAO.findByPK(primaryKey);
    Assert.assertNotNull(stage);
    stages.add(stage);

    primaryKey.setStageId(4L);
    stage = stageDAO.findByPK(primaryKey);
    Assert.assertNotNull(stage);
    stages.add(stage);

    CalculatedStatus calc3 = CalculatedStatus.statusFromStageEntities(stages);

    // !!! aggregated
    CalculatedStatus calc4 = CalculatedStatus.statusFromStageSummary(map, group);

    Assert.assertEquals(100d, calc3.getPercent(), 0.01d);
    Assert.assertEquals(HostRoleStatus.COMPLETED, calc3.getStatus());
    Assert.assertEquals(calc3.getPercent(), calc4.getPercent(), 0.01d);
    Assert.assertEquals(calc3.getStatus(), calc4.getStatus());
  }

  private void createGraph() {
    OrmTestHelper helper = injector.getInstance(OrmTestHelper.class);
    helper.createDefaultData();


    Long requestId = Long.valueOf(100L);
    String hostName = "test_host1";

    ResourceTypeEntity resourceTypeEntity =  new ResourceTypeEntity();
    resourceTypeEntity.setId(ResourceType.CLUSTER.getId());
    resourceTypeEntity.setName(ResourceType.CLUSTER.name());

    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setResourceType(resourceTypeEntity);

    ClusterEntity clusterEntity = clusterDAO.findByName("test_cluster1");

    RequestEntity requestEntity = new RequestEntity();
    requestEntity.setRequestId(requestId);
    requestEntity.setClusterId(clusterEntity.getClusterId());
    requestEntity.setStages(new ArrayList<>());
    requestDAO.create(requestEntity);

    HostEntity host = hostDAO.findByName(hostName);
    host.setHostRoleCommandEntities(new ArrayList<>());

    long stageId = 1L;

    stageId = createStages(stageId, 3, host, requestEntity, HostRoleStatus.COMPLETED, false);
    stageId = createStages(stageId, 1, host, requestEntity, HostRoleStatus.FAILED, true);
    stageId = createStages(stageId, 1, host, requestEntity, HostRoleStatus.IN_PROGRESS, false);
    stageId = createStages(stageId, 3, host, requestEntity, HostRoleStatus.PENDING, false);

    requestDAO.merge(requestEntity);
  }

  private long createStages(long startStageId, int count,
      HostEntity he, RequestEntity re, HostRoleStatus status, boolean skipStage) {
    long stageId = startStageId;

    ClusterEntity clusterEntity = clusterDAO.findByName("test_cluster1");

    for (int i = 0; i < count; i++) {
      StageEntity stageEntity = new StageEntity();
      stageEntity.setClusterId(clusterEntity.getClusterId());
      stageEntity.setRequest(re);
      stageEntity.setStageId(stageId);
      stageEntity.setHostRoleCommands(new ArrayList<>());
      stageEntity.setSkippable(skipStage);
      stageDAO.create(stageEntity);

      re.getStages().add(stageEntity);

      HostRoleCommandEntity commandEntity = new HostRoleCommandEntity();
      commandEntity.setRequestId(re.getRequestId());
      commandEntity.setStageId(stageId);
      commandEntity.setRoleCommand(RoleCommand.INSTALL);
      commandEntity.setStatus(status);
      commandEntity.setRole(Role.DATANODE);
      commandEntity.setHostEntity(he);
      commandEntity.setStage(stageEntity);
      hostRoleCommandDAO.create(commandEntity);

      he.getHostRoleCommandEntities().add(commandEntity);
      he = hostDAO.merge(he);

      stageEntity.getHostRoleCommands().add(commandEntity);
      stageDAO.merge(stageEntity);

      stageId++;
    }

    return stageId;
  }

}
