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
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.cache.ConfigGroupHostMapping;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupConfigMappingEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupHostMappingEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.host.HostFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

public class ConfigGroupDAOTest {
  private Injector injector;
  private ConfigGroupDAO configGroupDAO;
  private ClusterDAO clusterDAO;
  private ConfigGroupConfigMappingDAO configGroupConfigMappingDAO;
  private ConfigGroupHostMappingDAO configGroupHostMappingDAO;
  private HostDAO hostDAO;
  private ResourceTypeDAO resourceTypeDAO;
  private StackDAO stackDAO;
  private HostFactory hostFactory;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    // required to populate the database with stacks
    injector.getInstance(AmbariMetaInfo.class);

    stackDAO = injector.getInstance(StackDAO.class);
    clusterDAO = injector.getInstance(ClusterDAO.class);
    configGroupDAO = injector.getInstance(ConfigGroupDAO.class);
    configGroupConfigMappingDAO = injector.getInstance
      (ConfigGroupConfigMappingDAO.class);
    configGroupHostMappingDAO = injector.getInstance
      (ConfigGroupHostMappingDAO.class);
    hostDAO = injector.getInstance(HostDAO.class);
    resourceTypeDAO = injector.getInstance(ResourceTypeDAO.class);
    hostFactory = injector.getInstance(HostFactory.class);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  private ConfigGroupEntity createConfigGroup(String clusterName,
         String groupName, String tag, String desc, List<HostEntity> hosts,
         List<ClusterConfigEntity> configs) throws Exception {
    Clusters clusters = injector.getInstance(Clusters.class);
    StackEntity stackEntity = stackDAO.find("HDP", "0.1");

    clusters.addCluster(clusterName, new StackId(stackEntity));
    ClusterEntity clusterEntity = clusterDAO.findByName(clusterName);

    ConfigGroupEntity configGroupEntity = new ConfigGroupEntity();

    configGroupEntity.setClusterEntity(clusterEntity);
    configGroupEntity.setClusterId(clusterEntity.getClusterId());
    configGroupEntity.setGroupName(groupName);
    configGroupEntity.setDescription(desc);
    configGroupEntity.setTag(tag);

    configGroupDAO.create(configGroupEntity);

    if (hosts != null && !hosts.isEmpty()) {
      List<ConfigGroupHostMappingEntity> hostMappingEntities = new ArrayList<>();

      for (HostEntity host : hosts) {
        host.setClusterEntities(Arrays.asList(clusterEntity));
        hostDAO.create(host);

        ConfigGroupHostMappingEntity hostMappingEntity = new ConfigGroupHostMappingEntity();
        hostMappingEntity.setHostId(host.getHostId());
        hostMappingEntity.setHostEntity(host);
        hostMappingEntity.setConfigGroupEntity(configGroupEntity);
        hostMappingEntity.setConfigGroupId(configGroupEntity.getGroupId());
        hostMappingEntities.add(hostMappingEntity);
        configGroupHostMappingDAO.create(hostMappingEntity);
      }
      configGroupEntity.setConfigGroupHostMappingEntities(hostMappingEntities);
      configGroupDAO.merge(configGroupEntity);
    }

    if (configs != null && !configs.isEmpty()) {
      List<ConfigGroupConfigMappingEntity> configMappingEntities = new
        ArrayList<>();

      for (ClusterConfigEntity config : configs) {
        config.setClusterEntity(clusterEntity);
        config.setClusterId(clusterEntity.getClusterId());
        clusterDAO.createConfig(config);

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
    return configGroupEntity;
  }

  @Test
  public void testCreatePlaneJaneCG() throws Exception {
    ConfigGroupEntity configGroupEntity = createConfigGroup("c1", "hdfs-1",
      "HDFS", "some description", null, null);

    Long clusterId = clusterDAO.findByName("c1").getClusterId();

    Assert.assertNotNull(configGroupEntity);
    Assert.assertEquals("c1", configGroupEntity.getClusterEntity().getClusterName());
    Assert.assertEquals(clusterId, configGroupEntity.getClusterEntity()
      .getClusterId());
    Assert.assertEquals("hdfs-1", configGroupEntity.getGroupName());
    Assert.assertEquals("HDFS", configGroupEntity.getTag());
    Assert.assertEquals("some description", configGroupEntity.getDescription());
  }

  @Test
  public void testFindByTag() throws Exception {
    createConfigGroup("c1", "hdfs-1", "HDFS", "some description", null, null);

    List<ConfigGroupEntity> configGroupEntities = configGroupDAO.findAllByTag
      ("HDFS");

    Long clusterId = clusterDAO.findByName("c1").getClusterId();

    Assert.assertNotNull(configGroupEntities);
    ConfigGroupEntity configGroupEntity = configGroupEntities.get(0);
    Assert.assertNotNull(configGroupEntity);
    Assert.assertEquals("c1", configGroupEntity.getClusterEntity().getClusterName());
    Assert.assertEquals(clusterId, configGroupEntity.getClusterEntity()
      .getClusterId());
    Assert.assertEquals("hdfs-1", configGroupEntity.getGroupName());
    Assert.assertEquals("HDFS", configGroupEntity.getTag());
    Assert.assertEquals("some description", configGroupEntity.getDescription());
  }

  @Test
  public void testFindByName() throws Exception {
    createConfigGroup("c1", "hdfs-1", "HDFS", "some description", null, null);

    ConfigGroupEntity configGroupEntity = configGroupDAO.findByName("hdfs-1");

    Long clusterId = clusterDAO.findByName("c1").getClusterId();

    Assert.assertNotNull(configGroupEntity);
    Assert.assertEquals("c1", configGroupEntity.getClusterEntity().getClusterName());
    Assert.assertEquals(clusterId, configGroupEntity.getClusterEntity()
      .getClusterId());
    Assert.assertEquals("hdfs-1", configGroupEntity.getGroupName());
    Assert.assertEquals("HDFS", configGroupEntity.getTag());
    Assert.assertEquals("some description", configGroupEntity.getDescription());
  }

  @Test
  public void testFindByHost() throws Exception {
    List<HostEntity> hosts = new ArrayList<>();
    // Partially constructed HostEntity that will persisted in {@link createConfigGroup}
    HostEntity hostEntity = new HostEntity();
    hostEntity.setHostName("h1");
    hostEntity.setOsType("centOS");

    hosts.add(hostEntity);
    ConfigGroupEntity configGroupEntity =
      createConfigGroup("c1", "hdfs-1", "HDFS", "some description", hosts, null);

    Assert.assertNotNull(hostEntity.getHostId());

    Assert.assertNotNull(configGroupEntity);
    Assert.assertTrue(configGroupEntity.getConfigGroupHostMappingEntities()
      .size() > 0);
    Assert.assertNotNull(configGroupEntity
      .getConfigGroupHostMappingEntities().iterator().next());

    Set<ConfigGroupHostMapping> hostMappingEntities = configGroupHostMappingDAO.findByHostId(hostEntity.getHostId());

    Assert.assertNotNull(hostMappingEntities);

    for (ConfigGroupHostMapping hostMappingEntity : hostMappingEntities) {
      Assert.assertEquals(hostEntity.getHostId(), hostMappingEntity.getHostId());
      Assert.assertEquals("centOS", hostMappingEntity.getHost().getOsType());
    }
  }

  @Test
  public void testFindConfigsByGroup() throws Exception {
    StackEntity stackEntity = stackDAO.find("HDP", "0.1");

    ClusterConfigEntity configEntity = new ClusterConfigEntity();
    configEntity.setType("core-site");
    configEntity.setTag("version1");
    configEntity.setData("someData");
    configEntity.setAttributes("someAttributes");
    configEntity.setStack(stackEntity);

    List<ClusterConfigEntity> configEntities = new
      ArrayList<>();
    configEntities.add(configEntity);

    ConfigGroupEntity configGroupEntity =
      createConfigGroup("c1", "hdfs-1", "HDFS", "some description", null,
        configEntities);

    Assert.assertNotNull(configGroupEntity);
    Assert.assertTrue(configGroupEntity.getConfigGroupConfigMappingEntities()
      .size() > 0);

    List<ConfigGroupConfigMappingEntity> configMappingEntities =
      configGroupConfigMappingDAO.findByGroup(configGroupEntity.getGroupId());

    Assert.assertNotNull(configEntities);
    Assert.assertEquals("core-site", configEntities.get(0).getType());
    Assert.assertEquals("version1", configEntities.get(0).getTag());
    Assert.assertEquals("someData", configEntities.get(0).getData());
    Assert.assertEquals("someAttributes", configEntities.get(0).getAttributes());
  }
}
