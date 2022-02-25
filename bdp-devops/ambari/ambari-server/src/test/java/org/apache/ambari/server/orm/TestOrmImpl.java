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

package org.apache.ambari.server.orm;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.RollbackException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterServiceDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.dao.StageDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.StageEntity;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class TestOrmImpl extends Assert {
  private static final Logger log = LoggerFactory.getLogger(TestOrmImpl.class);
  @Inject
  private Injector injector;
  @Inject
  private StackDAO stackDAO;
  @Inject
  private ResourceTypeDAO resourceTypeDAO;
  @Inject
  private ClusterDAO clusterDAO;
  @Inject
  private OrmTestHelper ormTestHelper;
  @Inject
  private ClusterServiceDAO clusterServiceDAO;
  @Inject
  private HostRoleCommandDAO hostRoleCommandDAO;
  @Inject
  private HostDAO hostDAO;
  @Inject
  private StageDAO stageDAO;
  @Inject
  private EntityManager entityManager;
  @Inject
  private RequestDAO requestDAO;

  @Before
  public void setup() {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);
    // required to load stack information into the DB
    injector.getInstance(AmbariMetaInfo.class);
    ormTestHelper.createDefaultData();
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  /**
   * persistence provider is responsible for returning empty collection if relation doesn't exists
   */
  @Test
  public void testEmptyPersistentCollection() {
    String testClusterName = "test_cluster2";


    // create an admin resource to represent this cluster
    ResourceTypeEntity resourceTypeEntity = resourceTypeDAO.findById(ResourceType.CLUSTER.getId());
    if (resourceTypeEntity == null) {
      resourceTypeEntity = new ResourceTypeEntity();
      resourceTypeEntity.setId(ResourceType.CLUSTER.getId());
      resourceTypeEntity.setName(ResourceType.CLUSTER.name());
      resourceTypeEntity = resourceTypeDAO.merge(resourceTypeEntity);
    }

    StackEntity stackEntity = stackDAO.find("HDP", "2.2.0");
    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setResourceType(resourceTypeEntity);

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterName(testClusterName);
    clusterEntity.setResource(resourceEntity);
    clusterEntity.setDesiredStack(stackEntity);

    clusterDAO.create(clusterEntity);
    clusterEntity = clusterDAO.findByName(clusterEntity.getClusterName());

    assertTrue("empty relation wasn't instantiated", clusterEntity.getHostEntities() != null);
  }

  /**
   * Transaction marked for rollback should not be allowed for commit
   * @throws Throwable
   */
  @Test(expected = RollbackException.class)
  public void testRollbackException() throws Throwable{
    ormTestHelper.performTransactionMarkedForRollback();
  }

  /**
   * Rollback test
   */
  @Test
  public void testSafeRollback() {
    String testClusterName = "don't save";

    EntityManager entityManager = ormTestHelper.getEntityManager();
    entityManager.getTransaction().begin();
    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterName(testClusterName);
    entityManager.persist(clusterEntity);
    entityManager.getTransaction().rollback();

    assertNull("transaction was not rolled back", clusterDAO.findByName(testClusterName));
  }

  /**
   * Test auto incremented field and custom query example
   */
  @Test
  public void testAutoIncrementedField() {
    Date currentTime = new Date();
    String serviceName = "MapReduce1";
    String clusterName = "test_cluster1";

    createService(currentTime, serviceName, clusterName);

    ClusterServiceEntity clusterServiceEntity = clusterServiceDAO.findByClusterAndServiceNames(clusterName, serviceName);

    clusterServiceDAO.remove(clusterServiceEntity);

    assertNull(clusterServiceDAO.findByClusterAndServiceNames(clusterName, serviceName));

  }

  private void createService(Date currentTime, String serviceName, String clusterName) {
    ClusterEntity cluster = clusterDAO.findByName(clusterName);

    ClusterServiceEntity clusterServiceEntity = new ClusterServiceEntity();
    clusterServiceEntity.setClusterEntity(cluster);
    clusterServiceEntity.setServiceName(serviceName);

    cluster.getClusterServiceEntities().add(clusterServiceEntity);

    clusterServiceDAO.create(clusterServiceEntity);
    clusterDAO.merge(cluster);

    clusterServiceEntity = clusterServiceDAO.findByClusterAndServiceNames(clusterName, serviceName);
    assertNotNull(clusterServiceEntity);

    clusterServiceDAO.merge(clusterServiceEntity);
  }

  /**
   * to clarify: are cascade operations allowed?
   */
  @Test
  public void testCascadeRemoveFail() {
    Date currentTime = new Date();
    String serviceName = "MapReduce2";
    String clusterName = "test_cluster1";

    createService(currentTime, serviceName, clusterName);

    ClusterServiceEntity clusterServiceEntity = clusterServiceDAO.findByClusterAndServiceNames(clusterName, serviceName);
    clusterServiceDAO.remove(clusterServiceEntity);

    Assert.assertNull(
        clusterServiceDAO.findByClusterAndServiceNames(clusterName,
            serviceName));
  }

  @Test
  public void testSortedCommands() {
    injector.getInstance(OrmTestHelper.class).createStageCommands();
    HostRoleCommandDAO hostRoleCommandDAO = injector.getInstance(HostRoleCommandDAO.class);
    HostDAO hostDAO = injector.getInstance(HostDAO.class);
    StageDAO stageDAO = injector.getInstance(StageDAO.class);

    List<HostRoleCommandEntity> list =
        hostRoleCommandDAO.findSortedCommandsByStageAndHost(
            stageDAO.findByActionId("1-1"), hostDAO.findByName("test_host1"));
    log.info("command '{}' - taskId '{}' ", list.get(0).getRoleCommand(),
        list.get(0).getTaskId());
    log.info("command '{}' - taskId '{}'", list.get(1).getRoleCommand(),
        list.get(1).getTaskId());
   assertTrue(list.get(0).getTaskId() < list.get(1).getTaskId());

  }

  @Test
  public void testFindHostsByStage() {
    ormTestHelper.createStageCommands();
    StageEntity stageEntity = stageDAO.findByActionId("1-1");
    log.info("StageEntity {} {}" + stageEntity.getRequestId() + " "
        + stageEntity.getStageId());
    List<HostEntity> hosts = hostDAO.findByStage(stageEntity);
    assertEquals(2, hosts.size());
  }

  @Test
  public void testAbortHostRoleCommands() {
    ormTestHelper.createStageCommands();
    int result = hostRoleCommandDAO.updateStatusByRequestId(
        1L, HostRoleStatus.ABORTED, Arrays.asList(HostRoleStatus.QUEUED,
        HostRoleStatus.IN_PROGRESS, HostRoleStatus.PENDING));
    //result always 1 in batch mode
    List<HostRoleCommandEntity> commandEntities = hostRoleCommandDAO.findByRequest(1L);
    int count = 0;
    for (HostRoleCommandEntity commandEntity : commandEntities) {
      if (commandEntity.getStatus() == HostRoleStatus.ABORTED) {
        count++;
      }
    }
    assertEquals("Exactly two commands should be in aborted state", 2, count);
  }

  @Test
  public void testFindStageByHostRole() {
    ormTestHelper.createStageCommands();
    List<HostRoleCommandEntity> list = hostRoleCommandDAO.findByHostRole("test_host1", 1L, 1L, Role.DATANODE.toString());
    assertEquals(1, list.size());
  }

  @Test
  public void testLastRequestId() {
    ormTestHelper.createStageCommands();
    RequestDAO requestDAO = injector.getInstance(RequestDAO.class);

    RequestEntity requestEntity = requestDAO.findByPK(1L);
    List<StageEntity> stageEntities = new ArrayList<>();

    StageEntity stageEntity = new StageEntity();
    stageEntity.setClusterId(clusterDAO.findByName("test_cluster1").getClusterId());
    stageEntity.setRequest(requestEntity);
    stageEntity.setStageId(2L);
    stageDAO.create(stageEntity);
    StageEntity stageEntity2 = new StageEntity();
    stageEntity2.setClusterId(clusterDAO.findByName("test_cluster1").getClusterId());
    stageEntity2.setRequest(requestEntity);
    stageEntity2.setRequestId(1L);
    stageEntity2.setStageId(3L);
    stageDAO.create(stageEntity2);

    stageEntities.add(stageEntity);
    stageEntities.add(stageEntity2);
    requestEntity.setStages(stageEntities);
    requestDAO.merge(requestEntity);
    assertEquals(1L, stageDAO.getLastRequestId());
  }

  @Test
  public void testConcurrentModification() throws InterruptedException {
    final StackEntity stackEntity = stackDAO.find("HDP", "2.2.0");

    // create an admin resource to represent this cluster
    ResourceTypeEntity resourceTypeEntity = resourceTypeDAO.findById(ResourceType.CLUSTER.getId());
    if (resourceTypeEntity == null) {
      resourceTypeEntity = new ResourceTypeEntity();
      resourceTypeEntity.setId(ResourceType.CLUSTER.getId());
      resourceTypeEntity.setName(ResourceType.CLUSTER.name());
      resourceTypeEntity = resourceTypeDAO.merge(resourceTypeEntity);
    }
    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setResourceType(resourceTypeEntity);

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterName("cluster1");
    clusterEntity.setResource(resourceEntity);
    clusterEntity.setDesiredStack(stackEntity);

    clusterDAO.create(clusterEntity);

    clusterEntity = clusterDAO.findById(clusterEntity.getClusterId());
    assertEquals("cluster1", clusterEntity.getClusterName());

    Thread thread = new Thread(){
      @Override
      public void run() {
        ClusterEntity clusterEntity1 = clusterDAO.findByName("cluster1");
        clusterEntity1.setClusterName("anotherName");
        clusterDAO.merge(clusterEntity1);

        clusterEntity1 = clusterDAO.findById(clusterEntity1.getClusterId());
        assertEquals("anotherName", clusterEntity1.getClusterName());

        entityManager.clear();
      }
    };

    thread.start();
    thread.join();

    entityManager.clear();

    clusterEntity = clusterDAO.findById(clusterEntity.getClusterId());
    assertEquals("anotherName", clusterEntity.getClusterName());

    thread = new Thread(){
      @Override
      public void run() {
        clusterDAO.removeByName("anotherName");
        entityManager.clear();
      }
    };

    thread.start();
    thread.join();

    entityManager.clear();
    assertNull(clusterDAO.findById(clusterEntity.getClusterId()));

    List<ClusterEntity> result = clusterDAO.findAll();

    final ResourceTypeEntity finalResourceTypeEntity = resourceTypeEntity;

    thread = new Thread(){
      @Override
      public void run() {
        ResourceEntity resourceEntity = new ResourceEntity();
        resourceEntity.setResourceType(finalResourceTypeEntity);
        ClusterEntity temp = new ClusterEntity();
        temp.setClusterName("temp_cluster");
        temp.setResource(resourceEntity);
        temp.setDesiredStack(stackEntity);
        clusterDAO.create(temp);
      }
    };

    thread.start();
    thread.join();

    assertEquals(result.size() + 1, (result = clusterDAO.findAll()).size());


    thread = new Thread(){
      @Override
      public void run() {
        ResourceEntity resourceEntity = new ResourceEntity();
        resourceEntity.setResourceType(finalResourceTypeEntity);
        ClusterEntity temp = new ClusterEntity();
        temp.setClusterName("temp_cluster2");
        temp.setResource(resourceEntity);
        temp.setDesiredStack(stackEntity);
        clusterDAO.create(temp);
      }
    };

    thread.start();
    thread.join();

    assertEquals(result.size() + 1, (clusterDAO.findAll()).size());

  }

}
