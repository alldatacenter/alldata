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
package org.apache.ambari.server.state;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.ConfigGroupDAO;
import org.apache.ambari.server.orm.dao.ConfigGroupHostMappingDAO;
import org.apache.ambari.server.orm.entities.ConfigGroupConfigMappingEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupHostMappingEntity;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.Transactional;

import junit.framework.Assert;

public class ConfigGroupTest {

  private Clusters clusters;
  private Cluster cluster;
  private String clusterName;
  private Injector injector;
  private ConfigGroupFactory configGroupFactory;
  private ConfigFactory configFactory;
  private ConfigGroupDAO configGroupDAO;
  private ConfigGroupHostMappingDAO configGroupHostMappingDAO;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    configFactory = injector.getInstance(ConfigFactory.class);
    configGroupFactory = injector.getInstance(ConfigGroupFactory.class);
    configGroupDAO = injector.getInstance(ConfigGroupDAO.class);
    configGroupHostMappingDAO = injector.getInstance
      (ConfigGroupHostMappingDAO.class);

    StackId stackId = new StackId("HDP-0.1");
    OrmTestHelper helper = injector.getInstance(OrmTestHelper.class);
    helper.createStack(stackId);

    clusterName = "foo";
    clusters.addCluster(clusterName, stackId);
    cluster = clusters.getCluster(clusterName);
    Assert.assertNotNull(cluster);
    clusters.addHost("h1");
    clusters.addHost("h2");
    Assert.assertNotNull(clusters.getHost("h1"));
    Assert.assertNotNull(clusters.getHost("h2"));
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Transactional
  ConfigGroup createConfigGroup() throws AmbariException {
    // Create config without persisting and save group
    Map<String, String> properties = new HashMap<>();
    properties.put("a", "b");
    properties.put("c", "d");
    Map<String, Map<String, String>> propertiesAttributes = new HashMap<>();
    Map<String, String> attributes = new HashMap<>();
    attributes.put("a", "true");
    propertiesAttributes.put("final", attributes);
    Config config = configFactory.createNew(cluster, "hdfs-site", "testversion", properties, propertiesAttributes);

    Host host = clusters.getHost("h1");

    Map<String, Config> configs = new HashMap<>();
    Map<Long, Host> hosts = new HashMap<>();

    configs.put(config.getType(), config);
    hosts.put(host.getHostId(), host);

    ConfigGroup configGroup = configGroupFactory.createNew(cluster, "HDFS", "cg-test",
      "HDFS", "New HDFS configs for h1", configs, hosts);

    cluster.addConfigGroup(configGroup);
    return configGroup;
  }

  @Test
  public void testCreateNewConfigGroup() throws Exception {
    ConfigGroup configGroup = createConfigGroup();
    Assert.assertNotNull(configGroup);

    ConfigGroupEntity configGroupEntity = configGroupDAO.findByName("cg-test");
    Assert.assertNotNull(configGroupEntity);
    Assert.assertEquals("HDFS", configGroupEntity.getTag());
    ConfigGroupConfigMappingEntity configMappingEntity = configGroupEntity
      .getConfigGroupConfigMappingEntities().iterator().next();
    Assert.assertNotNull(configMappingEntity);
    Assert.assertEquals("hdfs-site", configMappingEntity.getConfigType());
    Assert.assertEquals("testversion", configMappingEntity.getVersionTag());
    Assert.assertNotNull(configMappingEntity.getClusterConfigEntity());
    Assert.assertTrue(configMappingEntity
      .getClusterConfigEntity().getData().contains("a"));
    Assert.assertEquals("{\"final\":{\"a\":\"true\"}}", configMappingEntity
        .getClusterConfigEntity().getAttributes());
    ConfigGroupHostMappingEntity hostMappingEntity = configGroupEntity
      .getConfigGroupHostMappingEntities().iterator().next();
    Assert.assertNotNull(hostMappingEntity);
    Assert.assertEquals("h1", hostMappingEntity.getHostname());
  }

  @Test
  @Transactional
  public void testUpdateConfigGroup() throws Exception {
    ConfigGroup configGroup = createConfigGroup();
    Assert.assertNotNull(configGroup);
    ConfigGroupEntity configGroupEntity = configGroupDAO.findById(configGroup.getId());
    Assert.assertNotNull(configGroupEntity);

    configGroup = configGroupFactory.createExisting(cluster, configGroupEntity);

    // Add new host
    Host host = clusters.getHost("h2");
    configGroup.addHost(host);
    Assert.assertEquals(2, configGroup.getHosts().values().size());

    // Create a new config
    Map<String, String> properties = new HashMap<>();
    properties.put("key1", "value1");
    Map<String, Map<String, String>> propertiesAttributes = new HashMap<>();
    Map<String, String> attributes = new HashMap<>();
    attributes.put("key1", "true");
    propertiesAttributes.put("final", attributes);

    Config config = configFactory.createNew(cluster, "test-site", "version100", properties, propertiesAttributes);
    Map<String, Config> newConfigurations = new HashMap<>(configGroup.getConfigurations());
    newConfigurations.put(config.getType(), config);

    configGroup.setConfigurations(newConfigurations);
    Assert.assertEquals(2, configGroup.getConfigurations().values().size());

    // re-request it and verify that the config was added
    configGroupEntity = configGroupDAO.findById(configGroup.getId());
    Assert.assertEquals(2, configGroupEntity.getConfigGroupConfigMappingEntities().size());

    configGroup.setName("NewName");
    configGroup.setDescription("NewDesc");
    configGroup.setTag("NewTag");

    // Save
    configGroupEntity = configGroupDAO.findByName("NewName");

    Assert.assertNotNull(configGroupEntity);
    Assert.assertEquals(2, configGroupEntity.getConfigGroupHostMappingEntities().size());
    Assert.assertEquals(2, configGroupEntity.getConfigGroupConfigMappingEntities().size());
    Assert.assertEquals("NewTag", configGroupEntity.getTag());
    Assert.assertEquals("NewDesc", configGroupEntity.getDescription());
    Assert.assertNotNull(cluster.getConfig("test-site", "version100"));

    ConfigGroupConfigMappingEntity configMappingEntity = null;
    Object[] array = configGroupEntity.getConfigGroupConfigMappingEntities().toArray();
    for(Object o: array) {
      if("test-site".equals(((ConfigGroupConfigMappingEntity)o).getConfigType())){
        configMappingEntity = (ConfigGroupConfigMappingEntity) o;
        break;
      }
    }
    Assert.assertNotNull(configMappingEntity);
    Assert.assertTrue(configMappingEntity
        .getClusterConfigEntity().getData().contains("{\"key1\":\"value1\"}"));
      Assert.assertEquals("{\"final\":{\"key1\":\"true\"}}", configMappingEntity
          .getClusterConfigEntity().getAttributes());
  }

  @Test
  public void testDeleteConfigGroup() throws Exception {
    ConfigGroup configGroup = createConfigGroup();
    Assert.assertNotNull(configGroup);
    Long id = configGroup.getId();

    configGroup.delete();

    Assert.assertNull(configGroupDAO.findById(id));
  }

  @Test
  public void testRemoveHost() throws Exception {
    ConfigGroup configGroup = createConfigGroup();
    Assert.assertNotNull(configGroup);
    Long id = configGroup.getId();

    configGroup = cluster.getConfigGroups().get(id);
    Assert.assertNotNull(configGroup);

    long hostId = clusters.getHost("h1").getHostId();

    clusters.unmapHostFromCluster("h1", clusterName);

    Assert.assertNull(clusters.getHostsForCluster(clusterName).get("h1"));
    // Assumes that 1L is the id of host h1, as specified in createConfigGroup
    Assert.assertNotNull(configGroupHostMappingDAO.findByHostId(hostId));
    Assert.assertTrue(configGroupHostMappingDAO.findByHostId(hostId).isEmpty());
    Assert.assertFalse(configGroup.getHosts().containsKey(hostId));
  }

  @Test
  public void testGetConfigGroup() throws Exception {
    ConfigGroup configGroup = createConfigGroup();
    Assert.assertNotNull(configGroup);
    Assert.assertNotNull(cluster.getConfigGroups().get(configGroup.getId()));

    ConfigGroupEntity configGroupEntity = configGroupDAO.findById(configGroup
      .getId());
    Collection<ConfigGroupConfigMappingEntity> configMappingEntities =
      configGroupEntity.getConfigGroupConfigMappingEntities();
    Collection<ConfigGroupHostMappingEntity> hostMappingEntities =
      configGroupEntity.getConfigGroupHostMappingEntities();

    Assert.assertEquals(configGroup.getId(), configGroupEntity.getGroupId());
    Assert.assertEquals(configGroup.getTag(), configGroupEntity.getTag());
    Assert.assertNotNull(configMappingEntities);
    Assert.assertNotNull(hostMappingEntities);
    Assert.assertEquals("h1", hostMappingEntities.iterator().next()
      .getHostname());
    ConfigGroupConfigMappingEntity configMappingEntity =
      configMappingEntities.iterator().next();
    Assert.assertEquals("hdfs-site", configMappingEntity.getConfigType());
    Assert.assertEquals("testversion", configMappingEntity.getVersionTag());
  }
}
