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
import java.util.Arrays;
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.StackId;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;


/**
 * {@link org.apache.ambari.server.orm.dao.HostVersionDAO} unit tests.
 */
public class HostVersionDAOTest {

  private static Injector injector;
  private ResourceTypeDAO resourceTypeDAO;
  private ClusterDAO clusterDAO;
  private StackDAO stackDAO;
  private HostDAO hostDAO;
  private HostVersionDAO hostVersionDAO;
  private OrmTestHelper helper;

  private final static StackId HDP_22_STACK = new StackId("HDP", "2.2.0");
  private final static StackId BAD_STACK = new StackId("BADSTACK", "1.0");

  private final static String repoVersion_2200 = "2.2.0.0-1";
  private final static String repoVersion_2201 = "2.2.0.1-2";
  private final static String repoVersion_2202 = "2.2.0.2-3";

  @Before
  public void before() {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    H2DatabaseCleaner.resetSequences(injector);
    injector.getInstance(GuiceJpaInitializer.class);

    resourceTypeDAO = injector.getInstance(ResourceTypeDAO.class);
    clusterDAO = injector.getInstance(ClusterDAO.class);
    stackDAO = injector.getInstance(StackDAO.class);
    hostDAO = injector.getInstance(HostDAO.class);
    hostVersionDAO = injector.getInstance(HostVersionDAO.class);
    helper = injector.getInstance(OrmTestHelper.class);

    // required to populate the database with stacks
    injector.getInstance(AmbariMetaInfo.class);

    createDefaultData();
  }

  /**
   * Helper function to bootstrap some basic data about clusters, cluster version, host, and host versions.
   */
  private void createDefaultData() {
    StackEntity stackEntity = stackDAO.find(HDP_22_STACK.getStackName(), HDP_22_STACK.getStackVersion());
    Assert.assertNotNull(stackEntity);

    // Create the cluster
    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(ResourceType.CLUSTER.getId());
    resourceTypeEntity.setName(ResourceType.CLUSTER.name());
    resourceTypeEntity = resourceTypeDAO.merge(resourceTypeEntity);

    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setResourceType(resourceTypeEntity);

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterName("test_cluster1");
    clusterEntity.setClusterInfo("test_cluster_info1");
    clusterEntity.setResource(resourceEntity);
    clusterEntity.setDesiredStack(stackEntity);

    clusterDAO.create(clusterEntity);

    RepositoryVersionEntity repoVersionEntity = helper.getOrCreateRepositoryVersion(HDP_22_STACK, repoVersion_2200);

    // Create the hosts
    HostEntity host1 = new HostEntity();
    HostEntity host2 = new HostEntity();
    HostEntity host3 = new HostEntity();

    host1.setHostName("test_host1");
    host2.setHostName("test_host2");
    host3.setHostName("test_host3");
    host1.setIpv4("192.168.0.1");
    host2.setIpv4("192.168.0.2");
    host3.setIpv4("192.168.0.3");

    List<HostEntity> hostEntities = new ArrayList<>();
    hostEntities.add(host1);
    hostEntities.add(host2);
    hostEntities.add(host3);

    // Both sides of relation should be set when modifying in runtime
    host1.setClusterEntities(Arrays.asList(clusterEntity));
    host2.setClusterEntities(Arrays.asList(clusterEntity));
    host3.setClusterEntities(Arrays.asList(clusterEntity));

    hostDAO.create(host1);
    hostDAO.create(host2);
    hostDAO.create(host3);

    clusterEntity.setHostEntities(hostEntities);
    clusterDAO.merge(clusterEntity);

    // Create the Host Versions
    HostVersionEntity hostVersionEntity1 = new HostVersionEntity(host1, repoVersionEntity, RepositoryVersionState.CURRENT);
    HostVersionEntity hostVersionEntity2 = new HostVersionEntity(host2, repoVersionEntity, RepositoryVersionState.INSTALLED);
    HostVersionEntity hostVersionEntity3 = new HostVersionEntity(host3, repoVersionEntity, RepositoryVersionState.INSTALLED);

    hostVersionDAO.create(hostVersionEntity1);
    hostVersionDAO.create(hostVersionEntity2);
    hostVersionDAO.create(hostVersionEntity3);
  }

  /**
   * Helper function to bootstrap additional data on top of the default data.
   */
  private void addMoreVersions() {
    ClusterEntity clusterEntity = clusterDAO.findByName("test_cluster1");

    RepositoryVersionEntity repositoryVersionEnt_2_2_0_1 = helper.getOrCreateRepositoryVersion(HDP_22_STACK, repoVersion_2201);


    HostEntity[] hostEntities = clusterEntity.getHostEntities().toArray(new HostEntity[clusterEntity.getHostEntities().size()]);
    // Must sort by host name in ascending order to ensure that state is accurately set later on.
    Arrays.sort(hostEntities);

    // For each of the hosts, add a host version
    for (HostEntity host : hostEntities) {
      HostVersionEntity hostVersionEntity = new HostVersionEntity(host, repositoryVersionEnt_2_2_0_1, RepositoryVersionState.INSTALLED);
      hostVersionDAO.create(hostVersionEntity);
    }

    // For each of the hosts, add one more host version
    RepositoryVersionEntity repositoryVersionEnt_2_2_0_2 = helper.getOrCreateRepositoryVersion(HDP_22_STACK, repoVersion_2202);
    for (int i = 0; i < hostEntities.length; i++) {
      RepositoryVersionState desiredState = null;
      if (i % 3 == 0) {
        desiredState = RepositoryVersionState.INSTALLED;
      }
      if (i % 3 == 1) {
        desiredState = RepositoryVersionState.INSTALLING;
      }
      if (i % 3 == 2) {
        desiredState = RepositoryVersionState.INSTALL_FAILED;
      }


      HostVersionEntity hostVersionEntity = new HostVersionEntity(hostEntities[i], repositoryVersionEnt_2_2_0_2, desiredState);
      hostVersionDAO.create(hostVersionEntity);
    }
  }

  /**
   * Test the {@link HostVersionDAO#findAll()} method.
   */
  @Test
  public void testFindAll() {
    Assert.assertEquals(3, hostVersionDAO.findAll().size());
  }

  /**
   * Test the {@link HostVersionDAO#findByHost(String)} method.
   */
  @Test
  public void testFindByHost() {
    Assert.assertEquals(1, hostVersionDAO.findByHost("test_host1").size());
    Assert.assertEquals(1, hostVersionDAO.findByHost("test_host2").size());
    Assert.assertEquals(1, hostVersionDAO.findByHost("test_host3").size());

    addMoreVersions();

    Assert.assertEquals(3, hostVersionDAO.findByHost("test_host1").size());
    Assert.assertEquals(3, hostVersionDAO.findByHost("test_host2").size());
    Assert.assertEquals(3, hostVersionDAO.findByHost("test_host3").size());
  }

  /**
   * Test the {@link HostVersionDAO#findByClusterStackAndVersion(String, org.apache.ambari.server.state.StackId, String)} method.
   */
  @Test
  public void testFindByClusterStackAndVersion() {
    Assert.assertEquals(3, hostVersionDAO.findByClusterStackAndVersion("test_cluster1", HDP_22_STACK, repoVersion_2200).size());
    Assert.assertEquals(3, hostVersionDAO.findAll().size());

    addMoreVersions();

    Assert.assertEquals(3, hostVersionDAO.findByClusterStackAndVersion("test_cluster1", HDP_22_STACK, repoVersion_2201).size());
    Assert.assertEquals(3, hostVersionDAO.findByClusterStackAndVersion("test_cluster1", HDP_22_STACK, repoVersion_2202).size());
    Assert.assertEquals(9, hostVersionDAO.findAll().size());
  }

  /**
   * Test the {@link HostVersionDAO#findByClusterAndHost(String, String)} method.
   */
  @Test
  public void testFindByClusterAndHost() {
    Assert.assertEquals(1, hostVersionDAO.findByClusterAndHost("test_cluster1", "test_host1").size());
    Assert.assertEquals(1, hostVersionDAO.findByClusterAndHost("test_cluster1", "test_host2").size());
    Assert.assertEquals(1, hostVersionDAO.findByClusterAndHost("test_cluster1", "test_host3").size());

    addMoreVersions();

    Assert.assertEquals(3, hostVersionDAO.findByClusterAndHost("test_cluster1", "test_host1").size());
    Assert.assertEquals(3, hostVersionDAO.findByClusterAndHost("test_cluster1", "test_host2").size());
    Assert.assertEquals(3, hostVersionDAO.findByClusterAndHost("test_cluster1", "test_host3").size());
  }

  /**
   * Test the {@link HostVersionDAO#findByCluster(String)} method.
   */
  @Test
  public void testFindByCluster() {
    Assert.assertEquals(3, hostVersionDAO.findByCluster("test_cluster1").size());

    addMoreVersions();

    Assert.assertEquals(9, hostVersionDAO.findByCluster("test_cluster1").size());
  }

  /**
   * Test the {@link HostVersionDAO#findByClusterHostAndState(String, String, org.apache.ambari.server.state.RepositoryVersionState)} method.
   */
  @Test
  public void testFindByClusterHostAndState() {
    Assert.assertEquals(1, hostVersionDAO.findByClusterHostAndState("test_cluster1", "test_host1", RepositoryVersionState.CURRENT).size());
    Assert.assertEquals(0, hostVersionDAO.findByClusterHostAndState("test_cluster1", "test_host1", RepositoryVersionState.INSTALLED).size());
    Assert.assertEquals(0, hostVersionDAO.findByClusterHostAndState("test_cluster1", "test_host2", RepositoryVersionState.INSTALLING).size());
    Assert.assertEquals(0, hostVersionDAO.findByClusterHostAndState("test_cluster1", "test_host3", RepositoryVersionState.INSTALL_FAILED).size());

    addMoreVersions();

    Assert.assertEquals(2, hostVersionDAO.findByClusterHostAndState("test_cluster1", "test_host1", RepositoryVersionState.INSTALLED).size());
    Assert.assertEquals(2, hostVersionDAO.findByClusterHostAndState("test_cluster1", "test_host2", RepositoryVersionState.INSTALLED).size());
    Assert.assertEquals(2, hostVersionDAO.findByClusterHostAndState("test_cluster1", "test_host3", RepositoryVersionState.INSTALLED).size());

    Assert.assertEquals(1, hostVersionDAO.findByClusterHostAndState("test_cluster1", "test_host1", RepositoryVersionState.CURRENT).size());
    Assert.assertEquals(1, hostVersionDAO.findByClusterHostAndState("test_cluster1", "test_host2", RepositoryVersionState.INSTALLING).size());
    Assert.assertEquals(1, hostVersionDAO.findByClusterHostAndState("test_cluster1", "test_host3", RepositoryVersionState.INSTALL_FAILED).size());
  }

  /**
   * Test the {@link HostVersionDAO#findByClusterStackVersionAndHost(String, StackId, String, String)} method.
   */
  @Test
  public void testFindByClusterStackVersionAndHost() {
    HostEntity host1 = hostDAO.findByName("test_host1");
    HostEntity host2 = hostDAO.findByName("test_host2");
    HostEntity host3 = hostDAO.findByName("test_host3");

    HostVersionEntity hostVersionEntity1 = new HostVersionEntity(host1,
        helper.getOrCreateRepositoryVersion(HDP_22_STACK, repoVersion_2200), RepositoryVersionState.CURRENT);
    hostVersionEntity1.setId(1L);
    HostVersionEntity hostVersionEntity2 = new HostVersionEntity(host2,
        helper.getOrCreateRepositoryVersion(HDP_22_STACK, repoVersion_2200), RepositoryVersionState.INSTALLED);
    hostVersionEntity2.setId(2L);
    HostVersionEntity hostVersionEntity3 = new HostVersionEntity(host3,
        helper.getOrCreateRepositoryVersion(HDP_22_STACK, repoVersion_2200), RepositoryVersionState.INSTALLED);
    hostVersionEntity3.setId(3L);

    hostVersionEntity1.equals(hostVersionDAO.findByClusterStackVersionAndHost("test_cluster1", HDP_22_STACK, repoVersion_2200, "test_host1"));
    Assert.assertEquals(hostVersionEntity1, hostVersionDAO.findByClusterStackVersionAndHost("test_cluster1", HDP_22_STACK, repoVersion_2200, "test_host1"));
    Assert.assertEquals(hostVersionEntity2, hostVersionDAO.findByClusterStackVersionAndHost("test_cluster1", HDP_22_STACK, repoVersion_2200, "test_host2"));
    Assert.assertEquals(hostVersionEntity3, hostVersionDAO.findByClusterStackVersionAndHost("test_cluster1", HDP_22_STACK, repoVersion_2200, "test_host3"));

    // Test non-existent objects
    Assert.assertEquals(null, hostVersionDAO.findByClusterStackVersionAndHost("non_existent_cluster", HDP_22_STACK, repoVersion_2200, "test_host3"));
    Assert.assertEquals(null, hostVersionDAO.findByClusterStackVersionAndHost("test_cluster1", BAD_STACK, repoVersion_2200, "test_host3"));
    Assert.assertEquals(null, hostVersionDAO.findByClusterStackVersionAndHost("test_cluster1", HDP_22_STACK, "non_existent_version", "test_host3"));
    Assert.assertEquals(null, hostVersionDAO.findByClusterStackVersionAndHost("test_cluster1", HDP_22_STACK, "non_existent_version", "non_existent_host"));

    addMoreVersions();

    // Expected
    HostVersionEntity hostVersionEntity1LastExpected = new HostVersionEntity(host1,
        helper.getOrCreateRepositoryVersion(HDP_22_STACK, repoVersion_2202), RepositoryVersionState.INSTALLED);
    HostVersionEntity hostVersionEntity2LastExpected = new HostVersionEntity(host2,
        helper.getOrCreateRepositoryVersion(HDP_22_STACK, repoVersion_2202), RepositoryVersionState.INSTALLING);
    HostVersionEntity hostVersionEntity3LastExpected = new HostVersionEntity(host3,
        helper.getOrCreateRepositoryVersion(HDP_22_STACK, repoVersion_2202), RepositoryVersionState.INSTALL_FAILED);

    // Actual
    HostVersionEntity hostVersionEntity1LastActual = hostVersionDAO.findByClusterStackVersionAndHost("test_cluster1", HDP_22_STACK, repoVersion_2202, "test_host1");
    HostVersionEntity hostVersionEntity2LastActual = hostVersionDAO.findByClusterStackVersionAndHost("test_cluster1", HDP_22_STACK, repoVersion_2202, "test_host2");
    HostVersionEntity hostVersionEntity3LastActual = hostVersionDAO.findByClusterStackVersionAndHost("test_cluster1", HDP_22_STACK, repoVersion_2202, "test_host3");

    // Trying to Mock the actual objects to override the getId() method will not work because the class that mockito creates
    // is still a Mockito wrapper. Instead, take advantage of an overloaded constructor that ignores the Id.
    Assert.assertEquals(hostVersionEntity1LastExpected, new HostVersionEntity(hostVersionEntity1LastActual));
    Assert.assertEquals(hostVersionEntity2LastExpected, new HostVersionEntity(hostVersionEntity2LastActual));
    Assert.assertEquals(hostVersionEntity3LastExpected, new HostVersionEntity(hostVersionEntity3LastActual));
  }

  @Test
  public void testDuplicates() throws Exception {
    HostEntity host1 = hostDAO.findByName("test_host1");

    RepositoryVersionEntity repoVersion = helper.getOrCreateRepositoryVersion(HDP_22_STACK, repoVersion_2200);

    HostVersionEntity hostVersionEntity1 = new HostVersionEntity(host1, repoVersion, RepositoryVersionState.CURRENT);

    try {
      hostVersionDAO.create(hostVersionEntity1);
      Assert.fail("Each host can have a relationship to a repo version, but cannot have more than one for the same repo");
    } catch (Exception e) {
      // expected
    }

  }

  @After
  public void after() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
    injector = null;
  }
}
