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

import static java.util.Arrays.asList;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupConfigMappingEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.ServiceConfigEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.StackId;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class ServiceConfigDAOTest {
  private static final StackId HDP_01 = new StackId("HDP", "0.1");
  private static final StackId HDP_02 = new StackId("HDP", "0.2");

  private Injector injector;
  private ServiceConfigDAO serviceConfigDAO;
  private ClusterDAO clusterDAO;
  private ResourceTypeDAO resourceTypeDAO;
  private StackDAO stackDAO;
  private ConfigGroupDAO configGroupDAO;
  private ConfigGroupConfigMappingDAO configGroupConfigMappingDAO;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    // required to load stack information into the DB
    injector.getInstance(AmbariMetaInfo.class);

    clusterDAO = injector.getInstance(ClusterDAO.class);
    stackDAO = injector.getInstance(StackDAO.class);
    serviceConfigDAO = injector.getInstance(ServiceConfigDAO.class);
    resourceTypeDAO = injector.getInstance(ResourceTypeDAO.class);
    configGroupDAO = injector.getInstance(ConfigGroupDAO.class);
    configGroupConfigMappingDAO = injector.getInstance(ConfigGroupConfigMappingDAO.class);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  private ServiceConfigEntity createServiceConfig(String serviceName,
         String userName, Long version, Long serviceConfigId,
         Long createTimestamp, List<ClusterConfigEntity> clusterConfigEntities)
    throws Exception {
    ServiceConfigEntity serviceConfigEntity = prepareServiceConfig(serviceName,
        userName, version, serviceConfigId, createTimestamp, clusterConfigEntities);
    serviceConfigDAO.create(serviceConfigEntity);

    return serviceConfigEntity;
  }

  private ServiceConfigEntity createServiceConfigWithGroup(String serviceName,
                                                  String userName, Long version, Long serviceConfigId,
                                                  Long createTimestamp, List<ClusterConfigEntity> clusterConfigEntities, Long groupId)
      throws Exception {
    ServiceConfigEntity serviceConfigEntity = prepareServiceConfig(serviceName,
        userName, version, serviceConfigId, createTimestamp, clusterConfigEntities);
    serviceConfigEntity.setGroupId(groupId);
    serviceConfigDAO.create(serviceConfigEntity);

    return serviceConfigEntity;
  }


  private ServiceConfigEntity prepareServiceConfig(String serviceName,
                                                  String userName, Long version, Long serviceConfigId,
                                                  Long createTimestamp, List<ClusterConfigEntity> clusterConfigEntities)
      throws Exception {

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

    ClusterEntity  clusterEntity = clusterDAO.findByName("c1");
    if (clusterEntity == null) {
      StackEntity stackEntity = stackDAO.find(HDP_01.getStackName(),
          HDP_01.getStackVersion());

      clusterEntity = new ClusterEntity();
      clusterEntity.setClusterName("c1");
      clusterEntity.setResource(resourceEntity);
      clusterEntity.setDesiredStack(stackEntity);

      clusterDAO.create(clusterEntity);
    }

    ServiceConfigEntity serviceConfigEntity = new ServiceConfigEntity();
    serviceConfigEntity.setServiceName(serviceName);
    serviceConfigEntity.setUser(userName);
    serviceConfigEntity.setVersion(version);
    serviceConfigEntity.setServiceConfigId(serviceConfigId);
    serviceConfigEntity.setClusterId(clusterEntity.getClusterId());
    serviceConfigEntity.setCreateTimestamp(createTimestamp);
    serviceConfigEntity.setClusterConfigEntities(clusterConfigEntities);
    serviceConfigEntity.setClusterEntity(clusterEntity);
    serviceConfigEntity.setStack(clusterEntity.getDesiredStack());
    return serviceConfigEntity;
  }


  @Test
  public void testCreateServiceConfigVersion() throws Exception {

    ServiceConfigEntity serviceConfigEntity =
      createServiceConfig("HDFS", "admin", 1L, 1L, 1111L, null);

    Long clusterId = clusterDAO.findByName("c1").getClusterId();

    Assert.assertNotNull(serviceConfigEntity);
    Assert.assertEquals("c1", serviceConfigEntity.getClusterEntity().getClusterName());
    Assert.assertEquals(clusterId, serviceConfigEntity.getClusterEntity()
      .getClusterId());
    Assert.assertEquals("HDFS", serviceConfigEntity.getServiceName());
    Assert.assertEquals(Long.valueOf(1111L), serviceConfigEntity.getCreateTimestamp());
    Assert.assertEquals("admin", serviceConfigEntity.getUser());
    Assert.assertEquals(Long.valueOf(1), serviceConfigEntity.getVersion());
    Assert.assertTrue(serviceConfigEntity.getClusterConfigEntities().isEmpty());
    Assert.assertNotNull(serviceConfigEntity.getServiceConfigId());
  }

  @Test
  public void testFindServiceConfigEntity() throws Exception {
    ServiceConfigEntity sce =
      createServiceConfig("HDFS", "admin", 1L, 1L, 1111L, null);

    ServiceConfigEntity serviceConfigEntity = serviceConfigDAO.find(sce.getServiceConfigId());

    Long clusterId = clusterDAO.findByName("c1").getClusterId();

    Assert.assertNotNull(serviceConfigEntity);
    Assert.assertEquals("c1", serviceConfigEntity.getClusterEntity().getClusterName());
    Assert.assertEquals(clusterId, serviceConfigEntity.getClusterEntity()
      .getClusterId());
    Assert.assertEquals("HDFS", serviceConfigEntity.getServiceName());
    Assert.assertEquals(Long.valueOf(1111L), serviceConfigEntity.getCreateTimestamp());
    Assert.assertEquals("admin", serviceConfigEntity.getUser());
    Assert.assertEquals(Long.valueOf(1), serviceConfigEntity.getVersion());
    Assert.assertTrue(serviceConfigEntity.getClusterConfigEntities().isEmpty());
    Assert.assertNotNull(serviceConfigEntity.getServiceConfigId());
  }

  @Test
  public void testFindByServiceAndVersion() throws Exception {

    createServiceConfig("HDFS", "admin", 1L, 1L, 1111L, null);

    ServiceConfigEntity serviceConfigEntity =
      serviceConfigDAO.findByServiceAndVersion("HDFS", 1L);

    Long clusterId = clusterDAO.findByName("c1").getClusterId();

    Assert.assertNotNull(serviceConfigEntity);
    Assert.assertEquals("c1", serviceConfigEntity.getClusterEntity().getClusterName());
    Assert.assertEquals(clusterId, serviceConfigEntity.getClusterEntity()
      .getClusterId());
    Assert.assertEquals("HDFS", serviceConfigEntity.getServiceName());
    Assert.assertEquals(Long.valueOf(1111L), serviceConfigEntity.getCreateTimestamp());
    Assert.assertEquals("admin", serviceConfigEntity.getUser());
    Assert.assertEquals(Long.valueOf(1), serviceConfigEntity.getVersion());
    Assert.assertTrue(serviceConfigEntity.getClusterConfigEntities().isEmpty());
    Assert.assertNotNull(serviceConfigEntity.getServiceConfigId());
  }

  @Test
  public void testFindMaxVersions() throws Exception {
    createServiceConfig("HDFS", "admin", 1L, 1L, 1111L, null);
    createServiceConfig("HDFS", "admin", 2L, 2L, 2222L, null);
    createServiceConfig("YARN", "admin", 1L, 3L, 3333L, null);

    long hdfsVersion = serviceConfigDAO.findNextServiceConfigVersion(
        clusterDAO.findByName("c1").getClusterId(), "HDFS");

    long yarnVersion = serviceConfigDAO.findNextServiceConfigVersion(
        clusterDAO.findByName("c1").getClusterId(), "YARN");

    Assert.assertEquals(3, hdfsVersion);
    Assert.assertEquals(2, yarnVersion);
  }

  @Test
  public void testGetLastServiceConfigs() throws Exception {
    createServiceConfig("HDFS", "admin", 1L, 1L, 1111L, null);
    createServiceConfig("HDFS", "admin", 2L, 2L, 2222L, null);
    createServiceConfig("YARN", "admin", 1L, 3L, 3333L, null);

    List<ServiceConfigEntity> serviceConfigEntities =
      serviceConfigDAO.getLastServiceConfigs(clusterDAO.findByName("c1").getClusterId());

    Assert.assertNotNull(serviceConfigEntities);
    Assert.assertEquals(2, serviceConfigEntities.size());

    Long clusterId = clusterDAO.findByName("c1").getClusterId();

    for (ServiceConfigEntity sce: serviceConfigEntities) {
      if ("HDFS".equals(sce.getServiceName())) {
        Assert.assertEquals("c1", sce.getClusterEntity().getClusterName());
        Assert.assertEquals(clusterId, sce.getClusterEntity()
          .getClusterId());
        Assert.assertEquals(Long.valueOf(2222L), sce.getCreateTimestamp());
        Assert.assertEquals(Long.valueOf(2), sce.getVersion());
        Assert.assertTrue(sce.getClusterConfigEntities().isEmpty());
        Assert.assertNotNull(sce.getServiceConfigId());
      }
      if ("YARN".equals(sce.getServiceName())) {
        Assert.assertEquals("c1", sce.getClusterEntity().getClusterName());
        Assert.assertEquals(clusterId, sce.getClusterEntity()
          .getClusterId());
        Assert.assertEquals(Long.valueOf(3333L), sce.getCreateTimestamp());
        Assert.assertEquals(Long.valueOf(1), sce.getVersion());
        Assert.assertTrue(sce.getClusterConfigEntities().isEmpty());
        Assert.assertNotNull(sce.getServiceConfigId());
      }

      Assert.assertEquals("admin", sce.getUser());
    }
  }

  @Test
  public void testGetLastServiceConfigsForService() throws Exception {
    String serviceName = "HDFS";
    Clusters clusters = injector.getInstance(Clusters.class);
    clusters.addCluster("c1", HDP_01);
    ConfigGroupEntity configGroupEntity1 = new ConfigGroupEntity();
    ClusterEntity clusterEntity = clusterDAO.findByName("c1");
    configGroupEntity1.setClusterEntity(clusterEntity);
    configGroupEntity1.setClusterId(clusterEntity.getClusterId());
    configGroupEntity1.setGroupName("group1");
    configGroupEntity1.setDescription("group1_desc");
    configGroupEntity1.setTag("HDFS");
    configGroupEntity1.setServiceName("HDFS");
    configGroupDAO.create(configGroupEntity1);
    ConfigGroupEntity group1 = configGroupDAO.findByName("group1");
    ConfigGroupEntity configGroupEntity2 = new ConfigGroupEntity();
    configGroupEntity2.setClusterEntity(clusterEntity);
    configGroupEntity2.setClusterId(clusterEntity.getClusterId());
    configGroupEntity2.setGroupName("group2");
    configGroupEntity2.setDescription("group2_desc");
    configGroupEntity2.setTag("HDFS");
    configGroupEntity2.setServiceName("HDFS");
    configGroupDAO.create(configGroupEntity2);
    ConfigGroupEntity group2 = configGroupDAO.findByName("group2");
    createServiceConfig(serviceName, "admin", 1L, 1L, 1111L, null);
    createServiceConfig(serviceName, "admin", 2L, 2L, 1010L, null);
    createServiceConfigWithGroup(serviceName, "admin", 3L, 3L, 2222L, null, group1.getGroupId());
    createServiceConfigWithGroup(serviceName, "admin", 5L, 5L, 3333L, null, group2.getGroupId());
    createServiceConfigWithGroup(serviceName, "admin", 4L, 4L, 3330L, null, group2.getGroupId());

    List<ServiceConfigEntity> serviceConfigEntities = serviceConfigDAO
        .getLastServiceConfigsForService(clusterDAO.findByName("c1").getClusterId(), serviceName);
    Assert.assertNotNull(serviceConfigEntities);
    Assert.assertEquals(3, serviceConfigEntities.size());

    for (ServiceConfigEntity sce : serviceConfigEntities) {
      if (sce.getGroupId() != null && sce.getGroupId().equals(group2.getGroupId())) {
        // Group ID with the highest version should be selected
        Assert.assertEquals(sce.getVersion(), Long.valueOf(5L));
      }
    }
  }


  @Test
  public void testGetLastServiceConfig() throws Exception {
    createServiceConfig("HDFS", "admin", 1L, 1L, 1111L, null);
    createServiceConfig("HDFS", "admin", 2L, 2L, 2222L, null);
    createServiceConfig("YARN", "admin", 1L, 3L, 3333L, null);

    Long clusterId = clusterDAO.findByName("c1").getClusterId();

    ServiceConfigEntity serviceConfigEntity =
      serviceConfigDAO.getLastServiceConfig(clusterId, "HDFS");

    Assert.assertNotNull(serviceConfigEntity);
    Assert.assertEquals("c1", serviceConfigEntity.getClusterEntity().getClusterName());
    Assert.assertEquals(clusterId, serviceConfigEntity.getClusterEntity()
      .getClusterId());
    Assert.assertEquals("HDFS", serviceConfigEntity.getServiceName());
    Assert.assertEquals(Long.valueOf(2222L), serviceConfigEntity.getCreateTimestamp());
    Assert.assertEquals("admin", serviceConfigEntity.getUser());
    Assert.assertEquals(Long.valueOf(2), serviceConfigEntity.getVersion());
    Assert.assertTrue(serviceConfigEntity.getClusterConfigEntities().isEmpty());
    Assert.assertNotNull(serviceConfigEntity.getServiceConfigId());
  }

  @Test
  public void testGetServiceConfigs() throws Exception {
    createServiceConfig("HDFS", "admin", 1L, 1L, 1111L, null);
    createServiceConfig("HDFS", "admin", 2L, 2L, 2222L, null);
    createServiceConfig("YARN", "admin", 1L, 3L, 3333L, null);

    Long clusterId = clusterDAO.findByName("c1").getClusterId();

    List<ServiceConfigEntity> serviceConfigEntities =
      serviceConfigDAO.getServiceConfigs(clusterId);

    Assert.assertNotNull(serviceConfigEntities);
    Assert.assertEquals(3, serviceConfigEntities.size());

    for (ServiceConfigEntity sce: serviceConfigEntities) {
      if ("HDFS".equals(sce.getServiceName()) && (sce.getVersion() == 1)) {
        Assert.assertEquals("c1", sce.getClusterEntity().getClusterName());
        Assert.assertEquals(clusterId, sce.getClusterEntity()
          .getClusterId());
        Assert.assertEquals(Long.valueOf(1111L), sce.getCreateTimestamp());
        Assert.assertTrue(sce.getClusterConfigEntities().isEmpty());
        Assert.assertNotNull(sce.getServiceConfigId());
      } else if ("HDFS".equals(sce.getServiceName()) && (sce.getVersion() == 2)) {
        Assert.assertEquals("c1", sce.getClusterEntity().getClusterName());
        Assert.assertEquals(clusterId, sce.getClusterEntity()
          .getClusterId());
        Assert.assertEquals(Long.valueOf(2222L), sce.getCreateTimestamp());
        Assert.assertTrue(sce.getClusterConfigEntities().isEmpty());
        Assert.assertNotNull(sce.getServiceConfigId());
      } else if ("YARN".equals(sce.getServiceName())) {
        Assert.assertEquals("c1", sce.getClusterEntity().getClusterName());
        Assert.assertEquals(clusterId, sce.getClusterEntity()
          .getClusterId());
        Assert.assertEquals(Long.valueOf(3333L), sce.getCreateTimestamp());
        Assert.assertEquals(Long.valueOf(1), sce.getVersion());
        Assert.assertTrue(sce.getClusterConfigEntities().isEmpty());
        Assert.assertNotNull(sce.getServiceConfigId());

      } else {
        Assert.fail();
      }
      Assert.assertEquals("admin", sce.getUser());
    }
  }

  @Test
  public void testGetAllServiceConfigs() throws Exception {
    ServiceConfigEntity serviceConfigEntity = null;
    serviceConfigEntity = createServiceConfig("HDFS", "admin", 1L, 1L, 10L, null);
    serviceConfigEntity = createServiceConfig("HDFS", "admin", 2L, 2L, 20L, null);
    serviceConfigEntity = createServiceConfig("HDFS", "admin", 3L, 3L, 30L, null);
    serviceConfigEntity = createServiceConfig("YARN", "admin", 1L, 4L, 40L, null);

    long clusterId = serviceConfigEntity.getClusterId();

    List<ServiceConfigEntity> serviceConfigs = serviceConfigDAO.getServiceConfigsForServiceAndStack(
        clusterId, HDP_01, "HDFS");

    Assert.assertEquals(3, serviceConfigs.size());

    serviceConfigs = serviceConfigDAO.getServiceConfigsForServiceAndStack(clusterId, HDP_01, "YARN");
    Assert.assertEquals(1, serviceConfigs.size());
    
    serviceConfigs = serviceConfigDAO.getServiceConfigsForServiceAndStack(clusterId, HDP_02, "HDFS");
    Assert.assertEquals(0, serviceConfigs.size());
  }

  @Test
  public void testGetLatestServiceConfigs() throws Exception {
    ServiceConfigEntity serviceConfigEntity = null;
    serviceConfigEntity = createServiceConfig("HDFS", "admin", 1L, 1L, 10L, null);
    serviceConfigEntity = createServiceConfig("HDFS", "admin", 2L, 2L, 20L, null);
    serviceConfigEntity = createServiceConfig("HDFS", "admin", 3L, 3L, 30L, null);
    serviceConfigEntity = createServiceConfig("YARN", "admin", 1L, 4L, 40L, null);

    StackEntity stackEntity = stackDAO.find(HDP_02.getStackName(),
        HDP_02.getStackVersion());

    ClusterEntity clusterEntity = serviceConfigEntity.getClusterEntity();
    clusterEntity.setDesiredStack(stackEntity);
    clusterDAO.merge(clusterEntity);

    ConfigGroupEntity configGroupEntity1 = new ConfigGroupEntity();
    configGroupEntity1.setClusterEntity(clusterEntity);
    configGroupEntity1.setClusterId(clusterEntity.getClusterId());
    configGroupEntity1.setGroupName("group1");
    configGroupEntity1.setDescription("group1_desc");
    configGroupEntity1.setTag("HDFS");
    configGroupEntity1.setServiceName("HDFS");
    configGroupDAO.create(configGroupEntity1);
    ConfigGroupEntity group1 = configGroupDAO.findByName("group1");
    createServiceConfigWithGroup("HDFS", "admin", 3L, 8L, 2222L, null, group1.getGroupId());
    // create some for HDP 0.2
    serviceConfigEntity = createServiceConfig("HDFS", "admin", 4L, 5L, 50L, null);
    serviceConfigEntity = createServiceConfig("HDFS", "admin", 5L, 6L, 60L, null);
    serviceConfigEntity = createServiceConfig("YARN", "admin", 2L, 7L, 70L, null);

    long clusterId = serviceConfigEntity.getClusterId();

    List<ServiceConfigEntity> serviceConfigs = serviceConfigDAO.getLatestServiceConfigs(clusterId, HDP_01);
    Assert.assertEquals(3, serviceConfigs.size());
    configGroupDAO.remove(configGroupEntity1);

    serviceConfigs = serviceConfigDAO.getLatestServiceConfigs(clusterId, HDP_02);
    Assert.assertEquals(2, serviceConfigs.size());
  }

  @Test
  public void testConfiguration() throws Exception{
    initClusterEntities();
    ClusterEntity clusterEntity = clusterDAO.findByName("c1");

    Assert.assertTrue(!clusterEntity.getClusterConfigEntities().isEmpty());

    Assert.assertEquals(5, clusterEntity.getClusterConfigEntities().size());
  }

  /**
   * Tests the ability to find the latest configuration by stack, regardless of
   * whether that configuration is enabled.
   *
   * @throws Exception
   */
  @Test
  public void testGetLatestClusterConfigsByStack() throws Exception {
    initClusterEntities();

    ClusterEntity clusterEntity = clusterDAO.findByName("c1");

    // there should be 3 configs in HDP-0.1 for this cluster, none selected
    List<ClusterConfigEntity> clusterConfigEntities = clusterDAO.getLatestConfigurations(clusterEntity.getClusterId(), HDP_01);
    Assert.assertEquals(1, clusterConfigEntities.size());

    ClusterConfigEntity entity = clusterConfigEntities.get(0);
    Assert.assertEquals("version3", entity.getTag());
    Assert.assertEquals("oozie-site", entity.getType());
    Assert.assertFalse(entity.isSelected());

    // there should be 2 configs in HDP-0.2 for this cluster, the latest being
    // selected
    clusterConfigEntities = clusterDAO.getLatestConfigurations(clusterEntity.getClusterId(), HDP_02);
    Assert.assertEquals(1, clusterConfigEntities.size());

    entity = clusterConfigEntities.get(0);
    Assert.assertEquals("version5", entity.getTag());
    Assert.assertEquals("oozie-site", entity.getType());
    Assert.assertTrue(entity.isSelected());
  }

  @Test
  public void testGetLatestClusterConfigsWithTypes() throws Exception {
    initClusterEntities();
    ClusterEntity clusterEntity = clusterDAO.findByName("c1");
    List<ClusterConfigEntity> entities = clusterDAO.getLatestConfigurationsWithTypes(clusterEntity.getClusterId(), HDP_01, asList("oozie-site"));
    Assert.assertEquals(1, entities.size());
    entities = clusterDAO.getLatestConfigurationsWithTypes(clusterEntity.getClusterId(), HDP_01, asList("no-such-type"));
    Assert.assertTrue(entities.isEmpty());
    entities = clusterDAO.getLatestConfigurationsWithTypes(clusterEntity.getClusterId(), HDP_01, Collections.emptyList());
    Assert.assertTrue(entities.isEmpty());
  }

  /**
   * Tests getting latest and enabled configurations when there is a
   * configuration group. Configurations for configuration groups are not
   * "selected" as they are merged in with the selected configuration. This can
   * cause problems if searching simply for the "latest" since it will pickup
   * the wrong configuration.
   *
   */
  @Test
  public void testGetClusterConfigsByStackCG() throws Exception {
    initClusterEntitiesWithConfigGroups();
    ClusterEntity clusterEntity = clusterDAO.findByName("c1");

    List<ConfigGroupEntity> configGroupEntities = configGroupDAO.findAllByTag("OOZIE");

    Long clusterId = clusterDAO.findByName("c1").getClusterId();

    Assert.assertNotNull(configGroupEntities);
    ConfigGroupEntity configGroupEntity = configGroupEntities.get(0);
    Assert.assertNotNull(configGroupEntity);
    Assert.assertEquals("c1", configGroupEntity.getClusterEntity().getClusterName());
    Assert.assertEquals(clusterId, configGroupEntity.getClusterEntity().getClusterId());
    Assert.assertEquals("oozie_server", configGroupEntity.getGroupName());
    Assert.assertEquals("OOZIE", configGroupEntity.getTag());
    Assert.assertEquals("oozie server", configGroupEntity.getDescription());

    // all 3 are HDP-0.1, but only the 2nd one is enabled
    List<ClusterConfigEntity> clusterConfigEntities = clusterDAO.getEnabledConfigsByStack(
        clusterEntity.getClusterId(), HDP_01);

    Assert.assertEquals(1, clusterConfigEntities.size());

    ClusterConfigEntity configEntity = clusterConfigEntities.get(0);
    Assert.assertEquals("version2", configEntity.getTag());
    Assert.assertEquals("oozie-site", configEntity.getType());
    Assert.assertTrue(configEntity.isSelected());

    // this should still return the 2nd one since the 3rd one has never been
    // selected as its only for configuration groups
    clusterConfigEntities = clusterDAO.getLatestConfigurations(clusterEntity.getClusterId(),
        HDP_01);

    configEntity = clusterConfigEntities.get(0);
    Assert.assertEquals("version2", configEntity.getTag());
    Assert.assertEquals("oozie-site", configEntity.getType());
    Assert.assertTrue(configEntity.isSelected());
  }


  /**
   * Tests that when there are multiple configurations for a stack, only the
   * selected ones get returned.
   *
   * @throws Exception
   */
  @Test
  public void testGetEnabledClusterConfigByStack() throws Exception {
    Clusters clusters = injector.getInstance(Clusters.class);
    clusters.addCluster("c1", HDP_01);

    Cluster cluster = clusters.getCluster("c1");

    initClusterEntities();

    Collection<ClusterConfigEntity> latestConfigs = clusterDAO.getEnabledConfigsByStack(
        cluster.getClusterId(), HDP_02);

    Assert.assertEquals(1, latestConfigs.size());
    for (ClusterConfigEntity e : latestConfigs) {
      Assert.assertEquals("version5", e.getTag());
      Assert.assertEquals("oozie-site", e.getType());
    }
  }

  /**
   * When the last configuration of a given configuration type to be stored into
   * the clusterconfig table is for a configuration group, that configuration is
   * not enabled. Therefore, it should be skipped when getting the enabled
   * configurations for a stack.
   */
  @Test
  public void testGetLatestClusterConfigByStackCG() throws Exception {
    Clusters clusters = injector.getInstance(Clusters.class);
    clusters.addCluster("c1", HDP_01);

    Cluster cluster = clusters.getCluster("c1");

    initClusterEntitiesWithConfigGroups();

    Collection<ClusterConfigEntity> latestConfigs = clusterDAO.getEnabledConfigsByStack(
        cluster.getClusterId(), HDP_01);

    Assert.assertEquals(1, latestConfigs.size());
    for (ClusterConfigEntity e : latestConfigs) {
      Assert.assertEquals("version2", e.getTag());
      Assert.assertEquals("oozie-site", e.getType());
    }
  }

  @Test
  public void testGetLastServiceConfigsForServiceWhenAConfigGroupIsDeleted() throws Exception {
    Clusters clusters = injector.getInstance(Clusters.class);
    clusters.addCluster("c1", HDP_01);
    initClusterEntitiesWithConfigGroups();
    ConfigGroupEntity configGroupEntity1 = new ConfigGroupEntity();
    ClusterEntity clusterEntity = clusterDAO.findByName("c1");
    Long clusterId = clusterEntity.getClusterId();
    configGroupEntity1.setClusterEntity(clusterEntity);
    configGroupEntity1.setClusterId(clusterEntity.getClusterId());
    configGroupEntity1.setGroupName("toTestDeleteGroup_OOZIE");
    configGroupEntity1.setDescription("toTestDeleteGroup_OOZIE_DESC");
    configGroupEntity1.setTag("OOZIE");
    configGroupEntity1.setServiceName("OOZIE");
    configGroupDAO.create(configGroupEntity1);
    ConfigGroupEntity testDeleteGroup_OOZIE = configGroupDAO.findByName("toTestDeleteGroup_OOZIE");
    createServiceConfigWithGroup("OOZIE", "", 2L, 2L, System.currentTimeMillis(), null,
        testDeleteGroup_OOZIE.getGroupId());
    Collection<ServiceConfigEntity> serviceConfigEntityList = serviceConfigDAO.getLastServiceConfigsForService(clusterId,
        "OOZIE");
    Assert.assertEquals(2, serviceConfigEntityList.size());
    configGroupDAO.remove(configGroupEntity1);
    serviceConfigEntityList = serviceConfigDAO.getLastServiceConfigsForService(clusterId, "OOZIE");
    Assert.assertEquals(1, serviceConfigEntityList.size());
  }

  /**
   * Createa a cluster with 5 configurations for Oozie. Each configuration will
   * have a tag of "version" plus a count. 3 configs will be for
   * {@link #HDP_01}, and 2 will be for {@link #HDP_02}. Only the most recent
   * configuration, {@code version5}, will be enabled.
   *
   * @throws Exception
   */
  private void initClusterEntities() throws Exception{
    String userName = "admin";

    ServiceConfigEntity oozieServiceConfigEntity = createServiceConfig("OOZIE", userName, 1L, 1L, System.currentTimeMillis(), null);
    ClusterEntity  clusterEntity = oozieServiceConfigEntity.getClusterEntity();

    Long clusterId = clusterEntity.getClusterId();

    if(null == clusterId){
      clusterId = 1L;
      clusterEntity.setClusterId(clusterId);
      clusterEntity = clusterDAO.merge(clusterEntity);
    }

    StackEntity stackEntityHDP01 = stackDAO.find(HDP_01.getStackName(),HDP_01.getStackVersion());
    StackEntity stackEntityHDP02 = stackDAO.find(HDP_02.getStackName(),HDP_02.getStackVersion());

    String oozieSite = "oozie-site";

    // create 5 Oozie Configs, with only the latest from HDP-0.2 being enabled
    int configsToCreate = 5;
    for (int i = 1; i <= configsToCreate; i++) {
      Thread.sleep(1);
      ClusterConfigEntity entity = new ClusterConfigEntity();
      entity.setClusterEntity(clusterEntity);
      entity.setClusterId(clusterEntity.getClusterId());
      entity.setType(oozieSite);
      entity.setVersion(Long.valueOf(i));
      entity.setTag("version"+i);
      entity.setTimestamp(new Date().getTime());

      // set selected to true to get the last selected timestamp populated
      entity.setSelected(true);

      // now set it to false
      entity.setSelected(false);

      entity.setStack(stackEntityHDP01);
      if (i >= 4) {
        entity.setStack(stackEntityHDP02);
        if (i == configsToCreate) {
          entity.setSelected(true);
        }
      }

      entity.setData("");
      clusterDAO.createConfig(entity);
      clusterEntity.getClusterConfigEntities().add(entity);
      clusterDAO.merge(clusterEntity);
    }
  }

  /**
   * Createa a cluster with 3 configurations for Oozie in the {@link #HDP_01}
   * stack. Only {@code version2}, will be enabled. {@code version3} will be for
   * a new configuration group.
   *
   * @throws Exception
   */
  private void initClusterEntitiesWithConfigGroups() throws Exception{
    String userName = "admin";

    ServiceConfigEntity oozieServiceConfigEntity = createServiceConfig("OOZIE", userName, 1L, 1L, System.currentTimeMillis(), null);
    ClusterEntity  clusterEntity = oozieServiceConfigEntity.getClusterEntity();

    Long clusterId = clusterEntity.getClusterId();

    if(null == clusterId){
      clusterId = 1L;
      clusterEntity.setClusterId(clusterId);
      clusterEntity = clusterDAO.merge(clusterEntity);
    }

    StackEntity stackEntityHDP01 = stackDAO.find(HDP_01.getStackName(),HDP_01.getStackVersion());
    String oozieSite = "oozie-site";

    // create 2 configurations for HDP-0.1
    int count = 2;
    for (int i = 1; i <= count; i++) {
      Thread.sleep(1);
      ClusterConfigEntity entity = new ClusterConfigEntity();
      entity.setClusterEntity(clusterEntity);
      entity.setClusterId(clusterEntity.getClusterId());
      entity.setType(oozieSite);
      entity.setVersion(Long.valueOf(i));
      entity.setTag("version"+i);
      entity.setTimestamp(new Date().getTime());
      entity.setStack(stackEntityHDP01);
      entity.setData("");
      entity.setSelected(false);

      if (i == count) {
        entity.setSelected(true);
      }

      clusterDAO.createConfig(entity);
      clusterEntity.getClusterConfigEntities().add(entity);
      clusterDAO.merge(clusterEntity);
    }

    ResourceTypeEntity resourceTypeEntity = resourceTypeDAO.findById(ResourceType.CLUSTER.getId());
    if (resourceTypeEntity == null) {
      resourceTypeEntity = new ResourceTypeEntity();
      resourceTypeEntity.setId(ResourceType.CLUSTER.getId());
      resourceTypeEntity.setName(ResourceType.CLUSTER.name());
      resourceTypeEntity = resourceTypeDAO.merge(resourceTypeEntity);
    }

    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setResourceType(resourceTypeEntity);

    // create a configuration group for oozie
    ConfigGroupEntity configGroupEntity = new ConfigGroupEntity();
    configGroupEntity.setClusterEntity(clusterEntity);
    configGroupEntity.setClusterId(clusterEntity.getClusterId());
    configGroupEntity.setGroupName("oozie_server");
    configGroupEntity.setDescription("oozie server");
    configGroupEntity.setTag("OOZIE");
    configGroupDAO.create(configGroupEntity);

    // create a new configuration for oozie, for the config group
    ClusterConfigEntity configEntityForGroup = new ClusterConfigEntity();
    configEntityForGroup.setSelected(false);
    configEntityForGroup.setType("oozie-site");
    configEntityForGroup.setTag("version3");
    configEntityForGroup.setData("someData");
    configEntityForGroup.setAttributes("someAttributes");
    configEntityForGroup.setStack(stackEntityHDP01);

    List<ClusterConfigEntity> configEntitiesForGroup = new ArrayList<>();
    configEntitiesForGroup.add(configEntityForGroup);
    List<ConfigGroupConfigMappingEntity> configMappingEntities = new ArrayList<>();

    for (ClusterConfigEntity config : configEntitiesForGroup) {
      config.setClusterEntity(clusterEntity);
      config.setClusterId(clusterEntity.getClusterId());
      clusterDAO.createConfig(config);

      Thread.sleep(1);
      ConfigGroupConfigMappingEntity configMappingEntity = new
        ConfigGroupConfigMappingEntity();
      configMappingEntity.setClusterId(clusterEntity.getClusterId());
      configMappingEntity.setClusterConfigEntity(config);
      configMappingEntity.setConfigGroupEntity(configGroupEntity);
      configMappingEntity.setConfigGroupId(configGroupEntity.getGroupId());
      configMappingEntity.setVersionTag(config.getTag());
      configMappingEntity.setConfigType(config.getType());
      configMappingEntity.setTimestamp(System.currentTimeMillis());
      configMappingEntities.add(configMappingEntity);
      configGroupConfigMappingDAO.create(configMappingEntity);
    }

    configGroupEntity.setConfigGroupConfigMappingEntities(configMappingEntities);
    configGroupDAO.merge(configGroupEntity);
  }
}
