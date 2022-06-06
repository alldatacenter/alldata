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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.entities.TopologyHostGroupEntity;
import org.apache.ambari.server.orm.entities.TopologyHostInfoEntity;
import org.apache.ambari.server.orm.entities.TopologyHostRequestEntity;
import org.apache.ambari.server.orm.entities.TopologyHostTaskEntity;
import org.apache.ambari.server.orm.entities.TopologyLogicalRequestEntity;
import org.apache.ambari.server.orm.entities.TopologyLogicalTaskEntity;
import org.apache.ambari.server.orm.entities.TopologyRequestEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

public class TopologyLogicalRequestDAOTest {
  private Injector injector;
  private TopologyRequestDAO requestDAO;
  private TopologyLogicalRequestDAO logicalRequestDAO;
  private TopologyHostGroupDAO hostGroupDAO;
  OrmTestHelper helper;
  Long clusterId;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    requestDAO = injector.getInstance(TopologyRequestDAO.class);
    logicalRequestDAO = injector.getInstance(TopologyLogicalRequestDAO.class);
    hostGroupDAO = injector.getInstance(TopologyHostGroupDAO.class);
    helper = injector.getInstance(OrmTestHelper.class);
    clusterId = helper.createCluster();
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  private void create() {
    TopologyRequestEntity requestEntity = new TopologyRequestEntity();
    requestEntity.setAction("a1");
    requestEntity.setBlueprintName("bp1");
    requestEntity.setClusterAttributes("attributes");
    requestEntity.setClusterProperties("properties");
    requestEntity.setClusterId(clusterId);
    requestEntity.setDescription("description");
    requestDAO.create(requestEntity);
    List<TopologyRequestEntity> requestEntities = requestDAO.findAll();
    Assert.assertEquals(1, requestEntities.size());
    requestEntity = requestEntities.iterator().next();

    TopologyHostGroupEntity hostGroupEntity = new TopologyHostGroupEntity();
    hostGroupEntity.setName("hg1");
    hostGroupEntity.setGroupProperties("test");
    hostGroupEntity.setGroupAttributes("test");
    hostGroupEntity.setTopologyRequestEntity(requestEntity);

    TopologyHostInfoEntity hostInfoEntity = new TopologyHostInfoEntity();
    hostInfoEntity.setHostCount(1);
    hostInfoEntity.setPredicate("test");
    hostInfoEntity.setFqdn("fqdn");
    hostInfoEntity.setTopologyHostGroupEntity(hostGroupEntity);
    hostGroupDAO.create(hostGroupEntity);
    List<TopologyHostGroupEntity> hostGroupEntities = hostGroupDAO.findAll();
    Assert.assertEquals(1, hostGroupEntities.size());
    hostGroupEntity = hostGroupEntities.iterator().next();

    TopologyLogicalRequestEntity logicalRequestEntity = new TopologyLogicalRequestEntity();
    logicalRequestEntity.setId(1L);
    logicalRequestEntity.setDescription("description");
    logicalRequestEntity.setTopologyRequestEntity(requestEntity);
    logicalRequestEntity.setTopologyRequestId(requestEntity.getId());

    TopologyHostRequestEntity hostRequestEntity = new TopologyHostRequestEntity();
    hostGroupEntity.setId(1L);
    hostRequestEntity.setHostName("h1");
    hostRequestEntity.setStageId(1L);
    hostRequestEntity.setTopologyLogicalRequestEntity(logicalRequestEntity);
    hostRequestEntity.setTopologyHostGroupEntity(hostGroupEntity);

    TopologyHostTaskEntity hostTaskEntity = new TopologyHostTaskEntity();
    hostTaskEntity.setType("type");
    hostTaskEntity.setTopologyHostRequestEntity(hostRequestEntity);

    TopologyLogicalTaskEntity logicalTaskEntity = new TopologyLogicalTaskEntity();
    logicalTaskEntity.setComponentName("NAMENODE");
    logicalTaskEntity.setHostRoleCommandEntity(null);
    logicalTaskEntity.setTopologyHostTaskEntity(hostTaskEntity);


    hostGroupEntity.setTopologyHostRequestEntities(Collections.singletonList(hostRequestEntity));

    hostRequestEntity.setTopologyHostTaskEntities(Collections.singletonList(hostTaskEntity));
    hostRequestEntity.setTopologyHostGroupEntity(hostGroupEntity);
    hostTaskEntity.setTopologyLogicalTaskEntities(Collections.singletonList(logicalTaskEntity));
    logicalRequestEntity.setTopologyHostRequestEntities(Collections.singletonList(hostRequestEntity));

    logicalRequestDAO.create(logicalRequestEntity);
  }

  @Test
  @Ignore
  public void testFindAll() throws Exception {
    create();
    List<TopologyLogicalRequestEntity> logicalRequestEntities = logicalRequestDAO.findAll();
    Assert.assertEquals(1, logicalRequestEntities.size());

    TopologyLogicalRequestEntity logicalRequestEntity = logicalRequestEntities.iterator().next();
    Assert.assertNotNull(logicalRequestEntity.getTopologyRequestId());
    Assert.assertEquals(Long.valueOf(1), logicalRequestEntity.getId());
    Assert.assertEquals("description", logicalRequestEntity.getDescription());
    Assert.assertNotNull(logicalRequestEntity.getTopologyRequestEntity());

    Collection<TopologyHostRequestEntity> hostRequestEntities = logicalRequestEntity.getTopologyHostRequestEntities();
    Assert.assertEquals(1, hostRequestEntities.size());
    TopologyHostRequestEntity hostRequestEntity = hostRequestEntities.iterator().next();
    Assert.assertNotNull(hostRequestEntity.getTopologyHostGroupEntity());
    Assert.assertEquals(hostRequestEntity.getTopologyHostGroupEntity().getId(), hostRequestEntity.getHostGroupId());

    Collection<TopologyHostTaskEntity> taskEntities = hostRequestEntity.getTopologyHostTaskEntities();
    Assert.assertEquals(1, taskEntities.size());
    TopologyHostTaskEntity taskEntity = taskEntities.iterator().next();
    Assert.assertNotNull(taskEntity.getTopologyHostRequestEntity());
    Assert.assertNotNull(taskEntity.getTopologyLogicalTaskEntities());
    Assert.assertEquals(1, taskEntity.getTopologyLogicalTaskEntities().size());
    Assert.assertNotNull(taskEntity.getTopologyLogicalTaskEntities().iterator().next().getTopologyHostTaskEntity());
  }
}
