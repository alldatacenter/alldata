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
import org.apache.ambari.server.orm.entities.TopologyRequestEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

public class TopologyRequestDAOTest {
  private Injector injector;
  private TopologyRequestDAO requestDAO;
  OrmTestHelper helper;
  Long clusterId;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    requestDAO = injector.getInstance(TopologyRequestDAO.class);
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
    TopologyHostGroupEntity hostGroupEntity = new TopologyHostGroupEntity();
    hostGroupEntity.setName("hg1");
    hostGroupEntity.setGroupAttributes("hg-attributes");
    hostGroupEntity.setGroupProperties("hg-properties");
    hostGroupEntity.setTopologyRequestEntity(requestEntity);
    requestEntity.setTopologyHostGroupEntities(Collections.singletonList(hostGroupEntity));
    TopologyHostInfoEntity hostInfoEntity = new TopologyHostInfoEntity();
    hostInfoEntity.setTopologyHostGroupEntity(hostGroupEntity);
    hostGroupEntity.setTopologyHostInfoEntities(Collections.singletonList(hostInfoEntity));
    hostInfoEntity.setFqdn("fqdn");
    hostInfoEntity.setHostCount(12);
    hostInfoEntity.setPredicate("predicate");

    requestDAO.create(requestEntity);
  }

  private void testRequestEntity(List<TopologyRequestEntity> requestEntities) {
    Assert.assertEquals(1, requestEntities.size());
    TopologyRequestEntity requestEntity = requestEntities.iterator().next();
    Assert.assertEquals("a1", requestEntity.getAction());
    Assert.assertEquals("bp1", requestEntity.getBlueprintName());
    Assert.assertEquals("attributes", requestEntity.getClusterAttributes());
    Assert.assertEquals("properties", requestEntity.getClusterProperties());
    Assert.assertEquals("description", requestEntity.getDescription());
    Collection<TopologyHostGroupEntity> hostGroupEntities = requestEntity.getTopologyHostGroupEntities();
    Assert.assertEquals(1, hostGroupEntities.size());
    TopologyHostGroupEntity hostGroupEntity = hostGroupEntities.iterator().next();
    Assert.assertEquals("hg1", hostGroupEntity.getName());
    Assert.assertEquals("hg-attributes", hostGroupEntity.getGroupAttributes());
    Assert.assertEquals("hg-properties", hostGroupEntity.getGroupProperties());
    Assert.assertEquals(requestEntity.getId(), hostGroupEntity.getRequestId());
    Collection<TopologyHostInfoEntity> infoEntities = hostGroupEntity.getTopologyHostInfoEntities();
    Assert.assertEquals(1, infoEntities.size());
    TopologyHostInfoEntity infoEntity = infoEntities.iterator().next();
    Assert.assertEquals("hg1", hostGroupEntity.getName());
    Assert.assertEquals(hostGroupEntity.getId(), infoEntity.getGroupId());
    Assert.assertEquals("fqdn", infoEntity.getFqdn());
    Assert.assertEquals(12, infoEntity.getHostCount().intValue());
    Assert.assertEquals("predicate", infoEntity.getPredicate());
  }

  @Test
  public void testAndFindAll() throws Exception {
    create();
    testRequestEntity(requestDAO.findAll());
  }

  @Test
  public void testFindByClusterId() throws Exception {
    create();
    testRequestEntity(requestDAO.findByClusterId(clusterId));
  }

  @Test
  public void testRemoveAll() throws Exception {
    // Given
    create();

    // When
    requestDAO.removeAll(clusterId);


    // Then
    List<TopologyRequestEntity> requestEntities = requestDAO.findByClusterId(clusterId);
    Assert.assertEquals("All topology request entities associated with cluster should be removed !", 0, requestEntities.size());
  }
}
