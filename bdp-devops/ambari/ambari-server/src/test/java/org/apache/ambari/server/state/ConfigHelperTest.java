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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariCustomCommandExecutionHelper;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.events.publishers.STOMPUpdatePublisher;
import org.apache.ambari.server.mpack.MpackManagerFactory;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.security.SecurityHelper;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.testutils.PartialNiceMockBinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.Transactional;

import junit.framework.Assert;


@RunWith(Enclosed.class)
public class ConfigHelperTest {
  public static class RunWithInMemoryDefaultTestModule {
    private final static Logger LOG = LoggerFactory.getLogger(ConfigHelperTest.class);
    private static Clusters clusters;
    private static Injector injector;
    private static String clusterName;
    private static Cluster cluster;
    private static ConfigGroupFactory configGroupFactory;
    private static ConfigHelper configHelper;
    private static AmbariManagementController managementController;
    private static AmbariMetaInfo metaInfo;
    private static ConfigFactory configFactory;

    @Before
    public void setup() throws Exception {
      // Set the authenticated user
      // TODO: remove this or replace the authenticated user to test authorization rules
      SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator("admin"));

      injector = Guice.createInjector(new InMemoryDefaultTestModule());
      injector.getInstance(GuiceJpaInitializer.class);
      clusters = injector.getInstance(Clusters.class);
      configGroupFactory = injector.getInstance(ConfigGroupFactory.class);
      configHelper = injector.getInstance(ConfigHelper.class);
      managementController = injector.getInstance(AmbariManagementController.class);
      metaInfo = injector.getInstance(AmbariMetaInfo.class);
      configFactory = injector.getInstance(ConfigFactory.class);

      StackId stackId = new StackId("HDP-2.0.6");
      OrmTestHelper helper = injector.getInstance(OrmTestHelper.class);
      helper.createStack(stackId);

      RepositoryVersionEntity repositoryVersion = helper.getOrCreateRepositoryVersion(stackId, "2.0.6");

      clusterName = "c1";
      clusters.addCluster(clusterName, stackId);
      cluster = clusters.getCluster(clusterName);
      Assert.assertNotNull(cluster);
      clusters.addHost("h1");
      clusters.addHost("h2");
      clusters.addHost("h3");
      clusters.getHosts().forEach(h -> clusters.updateHostMappings(h));

      Assert.assertNotNull(clusters.getHost("h1"));
      Assert.assertNotNull(clusters.getHost("h2"));
      Assert.assertNotNull(clusters.getHost("h3"));

      // core-site
      ConfigurationRequest cr = new ConfigurationRequest();
      cr.setClusterName(clusterName);
      cr.setType("core-site");
      cr.setVersionTag("version1");
      cr.setProperties(new HashMap<String, String>() {{
        put("ipc.client.connect.max.retries", "30");
        put("fs.trash.interval", "30");
      }});
      cr.setPropertiesAttributes(new HashMap<String, Map<String, String>>() {{
        Map<String, String> attrs = new HashMap<>();
        attrs.put("ipc.client.connect.max.retries", "1");
        attrs.put("fs.trash.interval", "2");
        put("attribute1", attrs);
      }});

      final ClusterRequest clusterRequest1 =
          new ClusterRequest(cluster.getClusterId(), clusterName,
              cluster.getDesiredStackVersion().getStackVersion(), null);

      clusterRequest1.setDesiredConfig(Collections.singletonList(cr));
      managementController.updateClusters(new HashSet<ClusterRequest>() {{
        add(clusterRequest1);
      }}, null);

      // flume-conf

      ConfigurationRequest cr2 = new ConfigurationRequest();
      cr2.setClusterName(clusterName);
      cr2.setType("flume-conf");
      cr2.setVersionTag("version1");

      cluster.addService("FLUME", repositoryVersion);
      cluster.addService("OOZIE", repositoryVersion);
      cluster.addService("HDFS", repositoryVersion);

      final ClusterRequest clusterRequest2 =
          new ClusterRequest(cluster.getClusterId(), clusterName,
              cluster.getDesiredStackVersion().getStackVersion(), null);

      clusterRequest2.setDesiredConfig(Collections.singletonList(cr2));
      managementController.updateClusters(new HashSet<ClusterRequest>() {{
        add(clusterRequest2);
      }}, null);

      // global
      cr.setType("global");
      cr.setVersionTag("version1");
      cr.setProperties(new HashMap<String, String>() {{
        put("dfs_namenode_name_dir", "/hadoop/hdfs/namenode");
        put("namenode_heapsize", "1024");
      }});
      cr.setPropertiesAttributes(new HashMap<String, Map<String, String>>() {{
        Map<String, String> attrs = new HashMap<>();
        attrs.put("dfs_namenode_name_dir", "3");
        attrs.put("namenode_heapsize", "4");
        put("attribute2", attrs);
      }});

      final ClusterRequest clusterRequest3 =
          new ClusterRequest(cluster.getClusterId(), clusterName,
              cluster.getDesiredStackVersion().getStackVersion(), null);

      clusterRequest3.setDesiredConfig(Collections.singletonList(cr));
      managementController.updateClusters(new HashSet<ClusterRequest>() {{
        add(clusterRequest3);
      }}, null);

      // oozie-site
      ConfigurationRequest cr4 = new ConfigurationRequest();
      cr4.setClusterName(clusterName);
      cr4.setType("oozie-site");
      cr4.setVersionTag("version1");
      cr4.setProperties(new HashMap<String, String>() {{
        put("oozie.authentication.type", "simple");
        put("oozie.service.HadoopAccessorService.kerberos.enabled", "false");
      }});
      cr4.setPropertiesAttributes(null);

      final ClusterRequest clusterRequest4 =
          new ClusterRequest(cluster.getClusterId(), clusterName,
              cluster.getDesiredStackVersion().getStackVersion(), null);

      clusterRequest4.setDesiredConfig(Collections.singletonList(cr4));
      managementController.updateClusters(new HashSet<ClusterRequest>() {{
        add(clusterRequest4);
      }}, null);

      // ams-site
      ConfigurationRequest cr5 = new ConfigurationRequest();
      cr5.setClusterName(clusterName);
      cr5.setType("ams-site");
      cr5.setVersionTag("version1");
      cr5.setProperties(new HashMap<String, String>() {{
        put("timeline.service.operating.mode", "embedded");
        put("timeline.service.fifo.enabled", "false");
      }});
      cr5.setPropertiesAttributes(null);

      final ClusterRequest clusterRequest5 =
        new ClusterRequest(cluster.getClusterId(), clusterName,
          cluster.getDesiredStackVersion().getStackVersion(), null);

      clusterRequest5.setDesiredConfig(Collections.singletonList(cr5));
      managementController.updateClusters(new HashSet<ClusterRequest>() {{
        add(clusterRequest5);
      }}, null);

      // hdfs-site/hadoop.caller.context.enabled
      ConfigurationRequest cr6 = new ConfigurationRequest();
      cr6.setClusterName(clusterName);
      cr6.setType("hdfs-site");
      cr6.setVersionTag("version1");
      cr6.setProperties(new HashMap<String, String>() {{
        put("hadoop.caller.context.enabled", "true");
      }});
      cr6.setPropertiesAttributes(null);

      final ClusterRequest clusterRequest6 =
              new ClusterRequest(cluster.getClusterId(), clusterName,
                      cluster.getDesiredStackVersion().getStackVersion(), null);

      clusterRequest6.setDesiredConfig(Collections.singletonList(cr6));
      managementController.updateClusters(new HashSet<ClusterRequest>() {{
        add(clusterRequest6);
      }}, null);

      // hdfs-site/hadoop.caller.context.enabled
      ConfigurationRequest cr7 = new ConfigurationRequest();
      cr7.setClusterName(clusterName);
      cr7.setType("hdfs-site");
      cr7.setVersionTag("version2");
      cr7.setProperties(new HashMap<String, String>() {{
        put("hadoop.caller.context.enabled", "false");
      }});
      cr7.setPropertiesAttributes(null);

      final ClusterRequest clusterRequest7 =
              new ClusterRequest(cluster.getClusterId(), clusterName,
                      cluster.getDesiredStackVersion().getStackVersion(), null);

      clusterRequest7.setDesiredConfig(Collections.singletonList(cr7));
      managementController.updateClusters(new HashSet<ClusterRequest>() {{
        add(clusterRequest7);
      }}, null);

    }

    @After
    public void tearDown() throws AmbariException, SQLException {
      H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);

      // Clear the authenticated user
      SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Transactional
    Long addConfigGroup(String name, String tag, List<String> hosts,
                        List<Config> configs) throws AmbariException {

      Map<Long, Host> hostMap = new HashMap<>();
      Map<String, Config> configMap = new HashMap<>();

      Long hostId = 1L;
      for (String hostname : hosts) {
        Host host = clusters.getHost(hostname);
        hostMap.put(hostId, host);
        hostId++;
      }

      for (Config config : configs) {
        configMap.put(config.getType(), config);
      }

      ConfigGroup configGroup = configGroupFactory.createNew(cluster, null, name,
          tag, "", configMap, hostMap);
      LOG.info("Config group created with tag " + tag);
      configGroup.setTag(tag);

      cluster.addConfigGroup(configGroup);

      return configGroup.getId();
    }

    void applyConfig(Map<String, String> properties, String configType, String configTag) throws Exception {
      ConfigurationRequest cr = new ConfigurationRequest();
      cr.setClusterName(clusterName);
      cr.setType(configType);
      cr.setVersionTag(configTag);
      cr.setProperties(properties);

      final ClusterRequest clusterRequest =
          new ClusterRequest(cluster.getClusterId(), clusterName,
              cluster.getDesiredStackVersion().getStackVersion(), null);

      clusterRequest.setDesiredConfig(Collections.singletonList(cr));
      managementController.updateClusters(new HashSet<ClusterRequest>() {{
        add(clusterRequest);
      }}, null);
    }

    @Test
    public void testProcessHiddenAttribute() throws Exception {
      StackInfo stackInfo = metaInfo.getStack("HDP", "2.0.5");
      Map<String, Map<String, Map<String, String>>> configAttributes = new HashMap<>();
      configAttributes.put("hive-site", stackInfo.getDefaultConfigAttributesForConfigType("hive-site"));

      Map<String, Map<String, String>> originalConfig_hiveClient = createHiveConfig();

      Map<String, Map<String, String>> expectedConfig_hiveClient = new HashMap<String, Map<String, String>>() {{
        put("hive-site", new HashMap<String, String>() {{
          put("javax.jdo.option.ConnectionDriverName", "oracle");
          put("hive.metastore.warehouse.dir", "/tmp");
        }});
      }};

      ConfigHelper.processHiddenAttribute(originalConfig_hiveClient, configAttributes, "HIVE_CLIENT", false);
      Assert.assertEquals(expectedConfig_hiveClient, originalConfig_hiveClient);

      Map<String, Map<String, String>> originalConfig_hiveServer = createHiveConfig();
      Map<String, Map<String, String>> expectedConfig_hiveServer = createHiveConfig();

      ConfigHelper.processHiddenAttribute(originalConfig_hiveServer, configAttributes, "HIVE_SERVER", false);
      Assert.assertEquals(expectedConfig_hiveServer, originalConfig_hiveServer);

      Map<String, Map<String, String>> originalConfig_hiveServer1 = createHiveConfig();
      Map<String, Map<String, String>> expectedConfig_hiveServer1 = expectedConfig_hiveClient;

      // config download removes hidden properties without respecting of component
      ConfigHelper.processHiddenAttribute(originalConfig_hiveServer1, configAttributes, "HIVE_SERVER", true);
      Assert.assertEquals(expectedConfig_hiveServer1, originalConfig_hiveServer1);
    }

    private Map<String, Map<String, String>> createHiveConfig() {
      return new HashMap<String, Map<String, String>>() {{
        put("hive-site", new HashMap<String, String>() {{
          put("javax.jdo.option.ConnectionDriverName", "oracle");
          put("javax.jdo.option.ConnectionPassword", "1");
          put("hive.metastore.warehouse.dir", "/tmp");
        }});
      }};
    }

    @Test
    public void testEffectiveTagsForHost() throws Exception {

      //Setup
      ConfigurationRequest cr5 = new ConfigurationRequest();
      cr5.setClusterName(clusterName);
      cr5.setType("ams-env");
      cr5.setVersionTag("version1");
      cr5.setProperties(new HashMap<String, String>() {{
        put("metrics_collector_log_dir", "/var/log/ambari-metrics-collector");
        put("metrics_collector_pid_dir", "/var/run/ambari-metrics-collector");
      }});
      cr5.setPropertiesAttributes(null);

      final ClusterRequest clusterRequest6 =
        new ClusterRequest(cluster.getClusterId(), clusterName,
          cluster.getDesiredStackVersion().getStackVersion(), null);

      clusterRequest6.setDesiredConfig(Collections.singletonList(cr5));
      managementController.updateClusters(new HashSet<ClusterRequest>() {{
        add(clusterRequest6);
      }}, null);

      Map<String, String> properties = new HashMap<>();
      properties.put("a", "b");
      properties.put("c", "d");

      final Config config = configFactory.createNew(cluster, "ams-env", "version122", properties, null);
      Long groupId = addConfigGroup("g1", "t1", new ArrayList<String>() {{
        add("h1");
      }}, new ArrayList<Config>() {{
        add(config);
      }});

      Assert.assertNotNull(groupId);

      Map<String, Map<String, String>> configTags = configHelper
          .getEffectiveDesiredTags(cluster, "h1");

      Assert.assertNotNull(configTags);
      Map<String, String> tagsWithOverrides = configTags.get("ams-env");
      Assert.assertNotNull(tagsWithOverrides);
      Assert.assertTrue(tagsWithOverrides.containsKey(ConfigHelper.CLUSTER_DEFAULT_TAG));
      Assert.assertEquals("version1", tagsWithOverrides.get(ConfigHelper.CLUSTER_DEFAULT_TAG));
      Assert.assertTrue(tagsWithOverrides.containsKey(groupId.toString()));
      Assert.assertEquals("version122", tagsWithOverrides.get(groupId.toString()));
    }

    @Test
    public void testEffectivePropertiesWithOverrides() throws Exception {

      //Setup
      // core-site
      ConfigurationRequest cr = new ConfigurationRequest();
      cr.setClusterName(clusterName);
      cr.setType("core-site2");
      cr.setVersionTag("version1");
      cr.setProperties(new HashMap<String, String>() {{
        put("ipc.client.connect.max.retries", "30");
        put("fs.trash.interval", "30");
      }});
      cr.setPropertiesAttributes(new HashMap<String, Map<String, String>>() {{
        Map<String, String> attrs = new HashMap<>();
        attrs.put("ipc.client.connect.max.retries", "1");
        attrs.put("fs.trash.interval", "2");
        put("attribute1", attrs);
      }});

      final ClusterRequest clusterRequest1 =
        new ClusterRequest(cluster.getClusterId(), clusterName,
          cluster.getDesiredStackVersion().getStackVersion(), null);

      clusterRequest1.setDesiredConfig(Collections.singletonList(cr));
      managementController.updateClusters(new HashSet<ClusterRequest>() {{
        add(clusterRequest1);
      }}, null);

      // global
      cr.setType("global2");
      cr.setVersionTag("version1");
      cr.setProperties(new HashMap<String, String>() {{
        put("dfs_namenode_name_dir", "/hadoop/hdfs/namenode");
        put("namenode_heapsize", "1024");
      }});
      cr.setPropertiesAttributes(new HashMap<String, Map<String, String>>() {{
        Map<String, String> attrs = new HashMap<>();
        attrs.put("dfs_namenode_name_dir", "3");
        attrs.put("namenode_heapsize", "4");
        put("attribute2", attrs);
      }});

      final ClusterRequest clusterRequest3 =
        new ClusterRequest(cluster.getClusterId(), clusterName,
          cluster.getDesiredStackVersion().getStackVersion(), null);

      clusterRequest3.setDesiredConfig(Collections.singletonList(cr));
      managementController.updateClusters(new HashSet<ClusterRequest>() {{
        add(clusterRequest3);
      }}, null);

      Map<String, String> properties = new HashMap<>();
      properties.put("a", "b");
      properties.put("c", "d");
      final Config config1 = configFactory.createNew(cluster, "core-site2", "version122", properties, null);

      Map<String, String> properties2 = new HashMap<>();
      properties2.put("namenode_heapsize", "1111");
      final Config config2 = configFactory.createNew(cluster, "global2", "version122", properties2, null);

      Long groupId = addConfigGroup("g2", "t1", new ArrayList<String>() {{
        add("h1");
      }}, new ArrayList<Config>() {{
        add(config1);
        add(config2);
      }});

      Assert.assertNotNull(groupId);

      Map<String, Map<String, String>> propertyMap = configHelper
          .getEffectiveConfigProperties(cluster,
              configHelper.getEffectiveDesiredTags(cluster, "h1"));

      Assert.assertNotNull(propertyMap);
      Assert.assertTrue(propertyMap.containsKey("global2"));
      Map<String, String> globalProps = propertyMap.get("global2");
      Assert.assertEquals("1111", globalProps.get("namenode_heapsize"));
      Assert.assertEquals("/hadoop/hdfs/namenode", globalProps.get("dfs_namenode_name_dir"));
      Assert.assertTrue(propertyMap.containsKey("core-site"));
      Map<String, String> coreProps = propertyMap.get("core-site2");
      Assert.assertTrue(coreProps.containsKey("a"));
      Assert.assertTrue(coreProps.containsKey("c"));
      Assert.assertEquals("30", coreProps.get("ipc.client.connect.max.retries"));
    }

    @Test
    public void testEffectivePropertiesAttributesWithOverrides() throws Exception {

      //Another version of core-site & global.
      // core-site3
      ConfigurationRequest crr = new ConfigurationRequest();
      crr.setClusterName(clusterName);
      crr.setType("core-site3");
      crr.setVersionTag("version1");
      crr.setProperties(new HashMap<String, String>() {{
        put("ipc.client.connect.max.retries", "30");
        put("fs.trash.interval", "30");
      }});
      crr.setPropertiesAttributes(new HashMap<String, Map<String, String>>() {{
        Map<String, String> attrs = new HashMap<>();
        attrs.put("ipc.client.connect.max.retries", "1");
        attrs.put("fs.trash.interval", "2");
        put("attribute1", attrs);
      }});

      final ClusterRequest clusterRequestDup =
        new ClusterRequest(cluster.getClusterId(), clusterName,
          cluster.getDesiredStackVersion().getStackVersion(), null);

      clusterRequestDup.setDesiredConfig(Collections.singletonList(crr));
      managementController.updateClusters(new HashSet<ClusterRequest>() {{
        add(clusterRequestDup);
      }}, null);

      // global3
      crr.setType("global3");
      crr.setVersionTag("version1");
      crr.setProperties(new HashMap<String, String>() {{
        put("dfs_namenode_name_dir", "/hadoop/hdfs/namenode");
        put("namenode_heapsize", "1024");
      }});
      crr.setPropertiesAttributes(new HashMap<String, Map<String, String>>() {{
        Map<String, String> attrs = new HashMap<>();
        attrs.put("dfs_namenode_name_dir", "3");
        attrs.put("namenode_heapsize", "4");
        put("attribute2", attrs);
      }});

      final ClusterRequest clusterRequestGlobalDup =
        new ClusterRequest(cluster.getClusterId(), clusterName,
          cluster.getDesiredStackVersion().getStackVersion(), null);

      clusterRequestGlobalDup.setDesiredConfig(Collections.singletonList(crr));
      managementController.updateClusters(new HashSet<ClusterRequest>() {{
        add(clusterRequestGlobalDup);
      }}, null);


      Map<String, String> attributes = new HashMap<>();
      attributes.put("fs.trash.interval", "11");
      attributes.put("b", "y");
      Map<String, Map<String, String>> config1Attributes = new HashMap<>();
      config1Attributes.put("attribute1", attributes);

      final Config config1 = configFactory.createNew(cluster, "core-site3", "version122",
        new HashMap<>(), config1Attributes);

      attributes = new HashMap<>();
      attributes.put("namenode_heapsize", "z");
      attributes.put("c", "q");
      Map<String, Map<String, String>> config2Attributes = new HashMap<>();
      config2Attributes.put("attribute2", attributes);

      final Config config2 = configFactory.createNew(cluster, "global3", "version122",
        new HashMap<>(), config2Attributes);

      Long groupId = addConfigGroup("g3", "t1", new ArrayList<String>() {{
        add("h3");
      }}, new ArrayList<Config>() {{
        add(config1);
        add(config2);
      }});

      Assert.assertNotNull(groupId);

      Map<String, Map<String, Map<String, String>>> effectiveAttributes = configHelper
          .getEffectiveConfigAttributes(cluster,
              configHelper.getEffectiveDesiredTags(cluster, "h3"));

      Assert.assertNotNull(effectiveAttributes);
      Assert.assertEquals(8, effectiveAttributes.size());

      Assert.assertTrue(effectiveAttributes.containsKey("global3"));
      Map<String, Map<String, String>> globalAttrs = effectiveAttributes.get("global3");
      Assert.assertEquals(1, globalAttrs.size());
      Assert.assertTrue(globalAttrs.containsKey("attribute2"));
      Map<String, String> attribute2Occurances = globalAttrs.get("attribute2");
      Assert.assertEquals(3, attribute2Occurances.size());
      Assert.assertTrue(attribute2Occurances.containsKey("namenode_heapsize"));
      Assert.assertEquals("z", attribute2Occurances.get("namenode_heapsize"));
      Assert.assertTrue(attribute2Occurances.containsKey("dfs_namenode_name_dir"));
      Assert.assertEquals("3", attribute2Occurances.get("dfs_namenode_name_dir"));
      Assert.assertTrue(attribute2Occurances.containsKey("c"));
      Assert.assertEquals("q", attribute2Occurances.get("c"));

      Assert.assertTrue(effectiveAttributes.containsKey("core-site3"));
      Map<String, Map<String, String>> coreAttrs = effectiveAttributes.get("core-site3");
      Assert.assertEquals(1, coreAttrs.size());
      Assert.assertTrue(coreAttrs.containsKey("attribute1"));
      Map<String, String> attribute1Occurances = coreAttrs.get("attribute1");
      Assert.assertEquals(3, attribute1Occurances.size());
      Assert.assertTrue(attribute1Occurances.containsKey("ipc.client.connect.max.retries"));
      Assert.assertEquals("1", attribute1Occurances.get("ipc.client.connect.max.retries"));
      Assert.assertTrue(attribute1Occurances.containsKey("fs.trash.interval"));
      Assert.assertEquals("11", attribute1Occurances.get("fs.trash.interval"));
      Assert.assertTrue(attribute1Occurances.containsKey("b"));
      Assert.assertEquals("y", attribute1Occurances.get("b"));
    }

    @Test
    public void testCloneAttributesMap() throws Exception {
      // init
      Map<String, Map<String, String>> targetAttributesMap = new HashMap<>();
      Map<String, String> attributesValues = new HashMap<>();
      attributesValues.put("a", "1");
      attributesValues.put("b", "2");
      attributesValues.put("f", "3");
      attributesValues.put("q", "4");
      targetAttributesMap.put("attr", attributesValues);
      Map<String, Map<String, String>> sourceAttributesMap = new HashMap<>();
      attributesValues = new HashMap<>();
      attributesValues.put("a", "5");
      attributesValues.put("f", "6");
      sourceAttributesMap.put("attr", attributesValues);
      attributesValues = new HashMap<>();
      attributesValues.put("f", "7");
      attributesValues.put("q", "8");
      sourceAttributesMap.put("attr1", attributesValues);

      // eval
      configHelper.cloneAttributesMap(sourceAttributesMap, targetAttributesMap);

      // verification
      Assert.assertEquals(2, targetAttributesMap.size());
      Assert.assertTrue(targetAttributesMap.containsKey("attr"));
      Assert.assertTrue(targetAttributesMap.containsKey("attr1"));
      Map<String, String> attributes = targetAttributesMap.get("attr");
      Assert.assertEquals(4, attributes.size());
      Assert.assertEquals("5", attributes.get("a"));
      Assert.assertEquals("2", attributes.get("b"));
      Assert.assertEquals("6", attributes.get("f"));
      Assert.assertEquals("4", attributes.get("q"));
      attributes = targetAttributesMap.get("attr1");
      Assert.assertEquals(2, attributes.size());
      Assert.assertEquals("7", attributes.get("f"));
      Assert.assertEquals("8", attributes.get("q"));
    }

    @Test
    public void testCloneAttributesMapSourceIsNull() throws Exception {
      // init
      Map<String, Map<String, String>> targetAttributesMap = new HashMap<>();
      Map<String, String> attributesValues = new HashMap<>();
      attributesValues.put("a", "1");
      attributesValues.put("b", "2");
      attributesValues.put("f", "3");
      attributesValues.put("q", "4");
      targetAttributesMap.put("attr", attributesValues);
      Map<String, Map<String, String>> sourceAttributesMap = null;

      // eval
      configHelper.cloneAttributesMap(sourceAttributesMap, targetAttributesMap);

      // verification
      // No exception should be thrown
      // targetMap should not be changed
      Assert.assertEquals(1, targetAttributesMap.size());
      Assert.assertTrue(targetAttributesMap.containsKey("attr"));
      Map<String, String> attributes = targetAttributesMap.get("attr");
      Assert.assertEquals(4, attributes.size());
      Assert.assertEquals("1", attributes.get("a"));
      Assert.assertEquals("2", attributes.get("b"));
      Assert.assertEquals("3", attributes.get("f"));
      Assert.assertEquals("4", attributes.get("q"));
    }

    @Test
    public void testCloneAttributesMapTargetIsNull() throws Exception {
      // init
      Map<String, Map<String, String>> targetAttributesMap = null;
      Map<String, Map<String, String>> sourceAttributesMap = new HashMap<>();
      Map<String, String> attributesValues = new HashMap<>();
      attributesValues.put("a", "5");
      attributesValues.put("f", "6");
      sourceAttributesMap.put("attr", attributesValues);
      attributesValues = new HashMap<>();
      attributesValues.put("f", "7");
      attributesValues.put("q", "8");
      sourceAttributesMap.put("attr1", attributesValues);

      // eval
      configHelper.cloneAttributesMap(sourceAttributesMap, targetAttributesMap);

      // verification
      // No exception should be thrown
      // sourceMap should not be changed
      Assert.assertEquals(2, sourceAttributesMap.size());
      Assert.assertTrue(sourceAttributesMap.containsKey("attr"));
      Assert.assertTrue(sourceAttributesMap.containsKey("attr1"));
      Map<String, String> attributes = sourceAttributesMap.get("attr");
      Assert.assertEquals(2, attributes.size());
      Assert.assertEquals("5", attributes.get("a"));
      Assert.assertEquals("6", attributes.get("f"));
      attributes = sourceAttributesMap.get("attr1");
      Assert.assertEquals(2, attributes.size());
      Assert.assertEquals("7", attributes.get("f"));
      Assert.assertEquals("8", attributes.get("q"));
    }

    @Test
    public void testMergeAttributes() throws Exception {
      Map<String, Map<String, String>> persistedAttributes = new HashMap<>();
      Map<String, String> persistedFinalAttrs = new HashMap<>();
      persistedFinalAttrs.put("a", "true");
      persistedFinalAttrs.put("c", "true");
      persistedFinalAttrs.put("d", "true");
      persistedAttributes.put("final", persistedFinalAttrs);
      Map<String, Map<String, String>> confGroupAttributes = new HashMap<>();
      Map<String, String> confGroupFinalAttrs = new HashMap<>();
      confGroupFinalAttrs.put("b", "true");
      confGroupAttributes.put("final", confGroupFinalAttrs);
      Map<String, String> confGroupProperties = new HashMap<>();
      confGroupProperties.put("a", "any");
      confGroupProperties.put("b", "any");
      confGroupProperties.put("c", "any");

      Config overrideConfig = configFactory.createNew(cluster, "type", null,
          confGroupProperties, confGroupAttributes);

      Map<String, Map<String, String>> result
          = configHelper.overrideAttributes(overrideConfig, persistedAttributes);

      Assert.assertNotNull(result);
      Assert.assertEquals(1, result.size());
      Map<String, String> finalResultAttributes = result.get("final");
      Assert.assertNotNull(finalResultAttributes);
      Assert.assertEquals(2, finalResultAttributes.size());
      Assert.assertEquals("true", finalResultAttributes.get("b"));
      Assert.assertEquals("true", finalResultAttributes.get("d"));
    }

    @Test
    public void testMergeAttributesWithNoAttributeOverrides() throws Exception {
      Map<String, Map<String, String>> persistedAttributes = new HashMap<>();
      Map<String, String> persistedFinalAttrs = new HashMap<>();
      persistedFinalAttrs.put("a", "true");
      persistedFinalAttrs.put("c", "true");
      persistedFinalAttrs.put("d", "true");
      persistedAttributes.put("final", persistedFinalAttrs);
      Map<String, Map<String, String>> confGroupAttributes = new HashMap<>();
      Map<String, String> confGroupProperties = new HashMap<>();
      confGroupProperties.put("a", "any");
      confGroupProperties.put("b", "any");
      confGroupProperties.put("c", "any");

      Config overrideConfig = configFactory.createNew(cluster, "type", null,
          confGroupProperties, confGroupAttributes);

      Map<String, Map<String, String>> result
          = configHelper.overrideAttributes(overrideConfig, persistedAttributes);

      Assert.assertNotNull(result);
      Assert.assertEquals(1, result.size());
      Map<String, String> finalResultAttributes = result.get("final");
      Assert.assertNotNull(finalResultAttributes);
      Assert.assertEquals(1, finalResultAttributes.size());
      Assert.assertEquals("true", finalResultAttributes.get("d"));
    }

    @Test
    public void testMergeAttributesWithNullAttributes() throws Exception {
      Map<String, Map<String, String>> persistedAttributes = new HashMap<>();
      Map<String, String> persistedFinalAttrs = new HashMap<>();
      persistedFinalAttrs.put("a", "true");
      persistedFinalAttrs.put("c", "true");
      persistedFinalAttrs.put("d", "true");
      persistedAttributes.put("final", persistedFinalAttrs);
      Map<String, String> confGroupProperties = new HashMap<>();
      confGroupProperties.put("a", "any");
      confGroupProperties.put("b", "any");
      confGroupProperties.put("c", "any");

      Config overrideConfig = configFactory.createNew(cluster, "type", null,
          confGroupProperties, null);

      Map<String, Map<String, String>> result
          = configHelper.overrideAttributes(overrideConfig, persistedAttributes);

      Assert.assertNotNull(result);
      Assert.assertEquals(1, result.size());
      Map<String, String> finalResultAttributes = result.get("final");
      Assert.assertNotNull(finalResultAttributes);
      Assert.assertEquals(3, finalResultAttributes.size());
      Assert.assertEquals("true", finalResultAttributes.get("a"));
      Assert.assertEquals("true", finalResultAttributes.get("c"));
      Assert.assertEquals("true", finalResultAttributes.get("d"));
    }

    @Test
    public void testFilterInvalidPropertyValues() {
      Map<PropertyInfo, String> properties = new HashMap<>();
      PropertyInfo prop1 = new PropertyInfo();
      prop1.setName("1");
      PropertyInfo prop2 = new PropertyInfo();
      prop1.setName("2");
      PropertyInfo prop3 = new PropertyInfo();
      prop1.setName("3");
      PropertyInfo prop4 = new PropertyInfo();
      prop1.setName("4");

      properties.put(prop1, "/tmp");
      properties.put(prop2, "null");
      properties.put(prop3, "");
      properties.put(prop4, null);

      Set<String> resultSet = configHelper.filterInvalidPropertyValues(properties, "testlist");
      Assert.assertEquals(1, resultSet.size());
      Assert.assertEquals(resultSet.iterator().next(), "/tmp");
    }

    @Test
    public void testMergeAttributesWithNullProperties() throws Exception {
      Map<String, Map<String, String>> persistedAttributes = new HashMap<>();
      Map<String, String> persistedFinalAttrs = new HashMap<>();
      persistedFinalAttrs.put("a", "true");
      persistedFinalAttrs.put("c", "true");
      persistedFinalAttrs.put("d", "true");
      persistedAttributes.put("final", persistedFinalAttrs);
      Map<String, Map<String, String>> confGroupAttributes = new HashMap<>();
      Map<String, String> confGroupFinalAttrs = new HashMap<>();
      confGroupFinalAttrs.put("b", "true");
      confGroupAttributes.put("final", confGroupFinalAttrs);

      Config overrideConfig = configFactory.createNew(cluster, "type", "version122",
        new HashMap<>(), confGroupAttributes);

      Map<String, Map<String, String>> result
          = configHelper.overrideAttributes(overrideConfig, persistedAttributes);

      Assert.assertNotNull(result);
      Assert.assertEquals(1, result.size());
      Map<String, String> finalResultAttributes = result.get("final");
      Assert.assertNotNull(finalResultAttributes);
      Assert.assertEquals(4, finalResultAttributes.size());
      Assert.assertEquals("true", finalResultAttributes.get("a"));
      Assert.assertEquals("true", finalResultAttributes.get("b"));
      Assert.assertEquals("true", finalResultAttributes.get("c"));
      Assert.assertEquals("true", finalResultAttributes.get("d"));
    }

    @Test
    public void testUpdateConfigType() throws Exception {
      Config currentConfig = cluster.getDesiredConfigByType("core-site");
      Map<String, String> properties = currentConfig.getProperties();
      // Attributes exist
      Map<String, Map<String, String>> propertiesAttributes = currentConfig.getPropertiesAttributes();
      Assert.assertNotNull(propertiesAttributes);
      Assert.assertEquals(1, propertiesAttributes.size());
      Assert.assertTrue(propertiesAttributes.containsKey("attribute1"));
      // Config tag before update
      Assert.assertEquals("version1", currentConfig.getTag());
      // Properties before update
      Assert.assertEquals("30", properties.get("fs.trash.interval"));
      // Property and attribute exist
      Assert.assertTrue(properties.containsKey("ipc.client.connect.max.retries"));
      Assert.assertTrue(propertiesAttributes.get("attribute1").containsKey("ipc.client.connect.max.retries"));


      Map<String, String> updates = new HashMap<>();
      updates.put("new-property", "new-value");
      updates.put("fs.trash.interval", "updated-value");
      Collection<String> removals = Collections.singletonList("ipc.client.connect.max.retries");
      configHelper.updateConfigType(cluster, cluster.getCurrentStackVersion(), managementController,
          "core-site", updates, removals, "admin", "Test note");


      Config updatedConfig = cluster.getDesiredConfigByType("core-site");
      // Attributes aren't lost
      propertiesAttributes = updatedConfig.getPropertiesAttributes();
      Assert.assertNotNull(propertiesAttributes);
      Assert.assertEquals(1, propertiesAttributes.size());
      Assert.assertTrue(propertiesAttributes.containsKey("attribute1"));
      // Config tag updated
      Assert.assertFalse("version1".equals(updatedConfig.getTag()));
      // Property added
      properties = updatedConfig.getProperties();
      Assert.assertTrue(properties.containsKey("new-property"));
      Assert.assertEquals("new-value", properties.get("new-property"));
      // Property updated
      Assert.assertTrue(properties.containsKey("fs.trash.interval"));
      Assert.assertEquals("updated-value", properties.get("fs.trash.interval"));
      Assert.assertEquals("2", propertiesAttributes.get("attribute1").get("fs.trash.interval"));
      // Property and attribute removed
      Assert.assertFalse(properties.containsKey("ipc.client.connect.max.retries"));
      Assert.assertFalse(propertiesAttributes.get("attribute1").containsKey("ipc.client.connect.max.retries"));
    }

    @Test
    public void testUpdateConfigTypeNoPropertyAttributes() throws Exception {
      Config currentConfig = cluster.getDesiredConfigByType("oozie-site");
      Map<String, String> properties = currentConfig.getProperties();
      // Config tag before update
      Assert.assertEquals("version1", currentConfig.getTag());
      // Properties before update
      Assert.assertEquals("simple", properties.get("oozie.authentication.type"));
      Assert.assertEquals("false", properties.get("oozie.service.HadoopAccessorService.kerberos.enabled"));

      Map<String, String> updates = new HashMap<>();
      updates.put("oozie.authentication.type", "kerberos");
      updates.put("oozie.service.HadoopAccessorService.kerberos.enabled", "true");

      configHelper.updateConfigType(cluster, cluster.getCurrentStackVersion(), managementController,
          "oozie-site", updates, null, "admin", "Test " + "note");

      Config updatedConfig = cluster.getDesiredConfigByType("oozie-site");
      // Config tag updated
      Assert.assertFalse("version1".equals(updatedConfig.getTag()));
      // Property added
      properties = updatedConfig.getProperties();
      Assert.assertTrue(properties.containsKey("oozie.authentication.type"));
      Assert.assertEquals("kerberos", properties.get("oozie.authentication.type"));
      // Property updated
      Assert.assertTrue(properties.containsKey("oozie.service.HadoopAccessorService.kerberos.enabled"));
      Assert.assertEquals("true", properties.get("oozie.service.HadoopAccessorService.kerberos.enabled"));
    }

    @Test
    public void testUpdateConfigTypeRemovals() throws Exception {
      Config currentConfig = cluster.getDesiredConfigByType("ams-site");
      Map<String, String> properties = currentConfig.getProperties();
      // Config tag before update
      Assert.assertEquals("version1", currentConfig.getTag());
      // Properties before update
      Assert.assertEquals("embedded", properties.get("timeline.service.operating.mode"));
      Assert.assertEquals("false", properties.get("timeline.service.fifo.enabled"));

      List<String> removals = new ArrayList<>();
      removals.add("timeline.service.operating.mode");

      configHelper.updateConfigType(cluster, cluster.getCurrentStackVersion(), managementController,
          "ams-site", null, removals, "admin", "Test note");

      Config updatedConfig = cluster.getDesiredConfigByType("ams-site");
      // Config tag updated
      Assert.assertFalse("version1".equals(updatedConfig.getTag()));
      // Property removed
      properties = updatedConfig.getProperties();
      Assert.assertFalse(properties.containsKey("timeline.service.operating.mode"));
      // Property unchanged
      Assert.assertTrue(properties.containsKey("timeline.service.fifo.enabled"));
      Assert.assertEquals("false", properties.get("timeline.service.fifo.enabled"));
    }

    @Test
    public void testCalculateIsStaleConfigs() throws Exception {

      Map<String, HostConfig> schReturn = new HashMap<>();
      HostConfig hc = new HostConfig();
      // Put a different version to check for change
      hc.setDefaultVersionTag("version2");
      schReturn.put("flume-conf", hc);

      ServiceComponent sc = createNiceMock(ServiceComponent.class);

      // set up mocks
      ServiceComponentHost sch = createNiceMock(ServiceComponentHost.class);
      expect(sc.getDesiredStackId()).andReturn(cluster.getDesiredStackVersion()).anyTimes();

      // set up expectations
      expect(sch.getActualConfigs()).andReturn(schReturn).times(6);
      expect(sch.getHostName()).andReturn("h1").anyTimes();
      expect(sch.getClusterId()).andReturn(cluster.getClusterId()).anyTimes();
      expect(sch.getServiceName()).andReturn("FLUME").anyTimes();
      expect(sch.getServiceComponentName()).andReturn("FLUME_HANDLER").anyTimes();
      expect(sch.getServiceComponent()).andReturn(sc).anyTimes();

      replay(sc, sch);
      // Cluster level config changes
      Assert.assertTrue(configHelper.isStaleConfigs(sch, null));

      HostConfig hc2 = new HostConfig();
      hc2.setDefaultVersionTag("version1");
      schReturn.put("flume-conf", hc2);
      // invalidate cache to test new sch
      // Cluster level same configs
      Assert.assertFalse(configHelper.isStaleConfigs(sch, null));

      // Cluster level same configs but group specific configs for host have been updated
      List<String> hosts = new ArrayList<>();
      hosts.add("h1");
      List<Config> configs = new ArrayList<>();

      Config configImpl = configFactory.createNew(cluster, "flume-conf", "FLUME1",
        new HashMap<>(), null);

      configs.add(configImpl);
      addConfigGroup("configGroup1", "FLUME", hosts, configs);

      // config group added for host - expect staleness
      Assert.assertTrue(configHelper.isStaleConfigs(sch, null));

      HostConfig hc3 = new HostConfig();
      hc3.setDefaultVersionTag("version1");
      hc3.getConfigGroupOverrides().put(1l, "FLUME1");
      schReturn.put("flume-conf", hc3);

      // version1 and FLUME1 - stale=false
      Assert.assertFalse(configHelper.isStaleConfigs(sch, null));

      HostConfig hc4 = new HostConfig();
      hc4.setDefaultVersionTag("version1");
      hc4.getConfigGroupOverrides().put(1l, "FLUME2");
      schReturn.put("flume-conf", hc4);

      // version1 and FLUME2 - stale=true
      Assert.assertTrue(configHelper.isStaleConfigs(sch, null));

      HostConfig hc5 = new HostConfig();
      hc5.setDefaultVersionTag("version3");
      hc5.getConfigGroupOverrides().put(1l, "FLUME1");
      schReturn.put("flume-conf", hc5);

      // version3 and FLUME1 - stale=true
      Assert.assertTrue(configHelper.isStaleConfigs(sch, null));

      verify(sch);
  }

  @Test
  public void testCalculateRefreshCommands() throws Exception {

    Map<String, HostConfig> schReturn = new HashMap<>();
    HostConfig hc = new HostConfig();
    // Put a different version to check for change
    hc.setDefaultVersionTag("version1");
    schReturn.put("hdfs-site", hc);

    ServiceComponent sc = createNiceMock(ServiceComponent.class);

    // set up mocks
    ServiceComponentHost sch = createNiceMock(ServiceComponentHost.class);
    expect(sc.getDesiredStackId()).andReturn(cluster.getDesiredStackVersion()).anyTimes();

    // set up expectations
    expect(sch.getActualConfigs()).andReturn(schReturn).anyTimes();
    expect(sch.getHostName()).andReturn("h1").anyTimes();
    expect(sch.getClusterId()).andReturn(cluster.getClusterId()).anyTimes();
    expect(sch.getServiceName()).andReturn("HDFS").anyTimes();
    expect(sch.getServiceComponentName()).andReturn("NAMENODE").anyTimes();
    expect(sch.getServiceComponent()).andReturn(sc).anyTimes();

    replay(sc, sch);

    Assert.assertTrue(configHelper.isStaleConfigs(sch, null));
    String refreshConfigsCommand = configHelper.getRefreshConfigsCommand(cluster, sch);
    Assert.assertEquals("reload_configs", refreshConfigsCommand);
    verify(sch);
  }

    @Test
    @SuppressWarnings("unchecked")
    public void testFindChangedKeys() throws AmbariException, AuthorizationException, NoSuchMethodException,
        InvocationTargetException, IllegalAccessException {
      final String configType = "hdfs-site";
      final String newPropertyName = "new.property";

      ConfigurationRequest newProperty = new ConfigurationRequest();
      newProperty.setClusterName(clusterName);
      newProperty.setType(configType);
      newProperty.setVersionTag("version3");
      newProperty.setProperties(new HashMap<String, String>() {{
        put("hadoop.caller.context.enabled", "false");
        put(newPropertyName, "true");
      }});
      newProperty.setPropertiesAttributes(null);

      final ClusterRequest clusterRequestNewProperty =
          new ClusterRequest(cluster.getClusterId(), clusterName,
              cluster.getDesiredStackVersion().getStackVersion(), null);

      clusterRequestNewProperty.setDesiredConfig(Collections.singletonList(newProperty));
      managementController.updateClusters(new HashSet<ClusterRequest>() {{
        add(clusterRequestNewProperty);
      }}, null);


      ConfigurationRequest removedNewProperty = new ConfigurationRequest();
      removedNewProperty.setClusterName(clusterName);
      removedNewProperty.setType(configType);
      removedNewProperty.setVersionTag("version4");
      removedNewProperty.setProperties(new HashMap<String, String>() {{
        put("hadoop.caller.context.enabled", "false");
      }});
      removedNewProperty.setPropertiesAttributes(null);

      final ClusterRequest clusterRequestRemovedNewProperty =
          new ClusterRequest(cluster.getClusterId(), clusterName,
              cluster.getDesiredStackVersion().getStackVersion(), null);

      clusterRequestRemovedNewProperty.setDesiredConfig(Collections.singletonList(removedNewProperty));
      managementController.updateClusters(new HashSet<ClusterRequest>() {{
        add(clusterRequestRemovedNewProperty);
      }}, null);

      ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);
      Method method = ConfigHelper.class.getDeclaredMethod("findChangedKeys",
          Cluster.class, String.class, Collection.class, Collection.class);
      method.setAccessible(true);

      // property value was changed
      Collection<String> value = (Collection<String>) method.invoke(configHelper, cluster, configType,
          Collections.singletonList("version1"), Collections.singletonList("version2"));
      assertTrue(value.size() == 1);
      assertEquals(configType + "/hadoop.caller.context.enabled", value.iterator().next());

      // property was added
      value = (Collection<String>) method.invoke(configHelper, cluster, configType,
          Collections.singletonList("version2"), Collections.singletonList("version3"));
      assertTrue(value.size() == 1);
      assertEquals(configType + "/" + newPropertyName, value.iterator().next());

      // property was removed
      value = (Collection<String>) method.invoke(configHelper, cluster, configType,
          Collections.singletonList("version3"), Collections.singletonList("version4"));
      assertTrue(value.size() == 1);
      assertEquals(configType + "/" + newPropertyName, value.iterator().next());
    }

  }

  public static class RunWithCustomModule {
    private Injector injector;

    @Before
    public void setup() throws Exception {
      injector = Guice.createInjector(new AbstractModule() {

        @Override
        protected void configure() {
          final AmbariMetaInfo mockMetaInfo = createNiceMock(AmbariMetaInfo.class);
          final ClusterController clusterController = createStrictMock(ClusterController.class);

          PartialNiceMockBinder.newBuilder().addAmbariMetaInfoBinding().addFactoriesInstallBinding().addLdapBindings().build().configure(binder());

          bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
          bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
          bind(SecurityHelper.class).toInstance(createNiceMock(SecurityHelper.class));
          bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
          bind(AmbariCustomCommandExecutionHelper.class).toInstance(createNiceMock(AmbariCustomCommandExecutionHelper.class));
          bind(AmbariMetaInfo.class).toInstance(mockMetaInfo);
          bind(Clusters.class).toInstance(createNiceMock(Clusters.class));
          bind(ClusterController.class).toInstance(clusterController);
          bind(StackManagerFactory.class).toInstance(createNiceMock(StackManagerFactory.class));
          bind(HostRoleCommandDAO.class).toInstance(createNiceMock(HostRoleCommandDAO.class));
          bind(STOMPUpdatePublisher.class).toInstance(createNiceMock(STOMPUpdatePublisher.class));
          bind(MpackManagerFactory.class).toInstance(createNiceMock(MpackManagerFactory.class));

        }
      });

      // Set the authenticated user
      // TODO: remove this or replace the authenticated user to test authorization rules
      SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator("admin"));
    }

    @After
    public void teardown() {
      // Clear the authenticated user
      SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Test
    public void testGetServicePropertiesSimpleInvocation() throws Exception {
      Cluster mockCluster = createStrictMock(Cluster.class);
      StackId mockStackVersion = createStrictMock(StackId.class);
      AmbariMetaInfo mockAmbariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
      Service mockService = createStrictMock(Service.class);
      ServiceInfo mockServiceInfo = createStrictMock(ServiceInfo.class);

      PropertyInfo mockPropertyInfo1 = createStrictMock(PropertyInfo.class);
      PropertyInfo mockPropertyInfo2 = createStrictMock(PropertyInfo.class);

      List<PropertyInfo> serviceProperties = Arrays.asList(mockPropertyInfo1, mockPropertyInfo2);

      expect(mockCluster.getService("SERVICE")).andReturn(mockService).once();
      expect(mockService.getDesiredStackId()).andReturn(mockStackVersion).once();
      expect(mockStackVersion.getStackName()).andReturn("HDP").once();
      expect(mockStackVersion.getStackVersion()).andReturn("2.2").once();

      expect(mockAmbariMetaInfo.getService("HDP", "2.2", "SERVICE")).andReturn(mockServiceInfo).once();

      expect(mockServiceInfo.getProperties()).andReturn(serviceProperties).once();

      replay(mockAmbariMetaInfo, mockCluster, mockService, mockStackVersion, mockServiceInfo, mockPropertyInfo1, mockPropertyInfo2);

      mockAmbariMetaInfo.init();

      Set<PropertyInfo> result = injector.getInstance(ConfigHelper.class)
          .getServiceProperties(mockCluster, "SERVICE");

      Assert.assertNotNull(result);
      Assert.assertEquals(2, result.size());

      verify(mockAmbariMetaInfo, mockCluster, mockStackVersion, mockServiceInfo, mockPropertyInfo1, mockPropertyInfo2);
    }

    @Test
    public void testGetServicePropertiesDoNoRemoveExcluded() throws Exception {
      StackId mockStackVersion = createStrictMock(StackId.class);
      AmbariMetaInfo mockAmbariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
      ServiceInfo mockServiceInfo = createStrictMock(ServiceInfo.class);

      PropertyInfo mockPropertyInfo1 = createStrictMock(PropertyInfo.class);
      PropertyInfo mockPropertyInfo2 = createStrictMock(PropertyInfo.class);

      List<PropertyInfo> serviceProperties = Arrays.asList(mockPropertyInfo1, mockPropertyInfo2);

      expect(mockStackVersion.getStackName()).andReturn("HDP").once();
      expect(mockStackVersion.getStackVersion()).andReturn("2.2").once();

      expect(mockAmbariMetaInfo.getService("HDP", "2.2", "SERVICE")).andReturn(mockServiceInfo).once();

      expect(mockServiceInfo.getProperties()).andReturn(serviceProperties).once();

      replay(mockAmbariMetaInfo, mockStackVersion, mockServiceInfo, mockPropertyInfo1, mockPropertyInfo2);

      mockAmbariMetaInfo.init();

      Set<PropertyInfo> result = injector.getInstance(ConfigHelper.class)
          .getServiceProperties(mockStackVersion, "SERVICE", false);

      Assert.assertNotNull(result);
      Assert.assertEquals(2, result.size());

      verify(mockAmbariMetaInfo, mockStackVersion, mockServiceInfo, mockPropertyInfo1, mockPropertyInfo2);
    }

    @Test
    public void testGetServicePropertiesRemoveExcluded() throws Exception {
      StackId mockStackVersion = createStrictMock(StackId.class);
      AmbariMetaInfo mockAmbariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
      ServiceInfo mockServiceInfo = createStrictMock(ServiceInfo.class);

      PropertyInfo mockPropertyInfo1 = createStrictMock(PropertyInfo.class);
      PropertyInfo mockPropertyInfo2 = createStrictMock(PropertyInfo.class);

      List<PropertyInfo> serviceProperties = Arrays.asList(mockPropertyInfo1, mockPropertyInfo2);

      expect(mockStackVersion.getStackName()).andReturn("HDP").once();
      expect(mockStackVersion.getStackVersion()).andReturn("2.2").once();

      expect(mockAmbariMetaInfo.getService("HDP", "2.2", "SERVICE")).andReturn(mockServiceInfo).once();

      expect(mockServiceInfo.getProperties()).andReturn(serviceProperties).once();
      expect(mockServiceInfo.getExcludedConfigTypes()).andReturn(Collections.singleton("excluded-type")).once();

      expect(mockPropertyInfo1.getFilename()).andReturn("included-type.xml").times(2);

      expect(mockPropertyInfo2.getFilename()).andReturn("excluded-type.xml").once();

      replay(mockAmbariMetaInfo, mockStackVersion, mockServiceInfo, mockPropertyInfo1, mockPropertyInfo2);

      mockAmbariMetaInfo.init();

      Set<PropertyInfo> result = injector.getInstance(ConfigHelper.class)
          .getServiceProperties(mockStackVersion, "SERVICE", true);

      Assert.assertNotNull(result);
      Assert.assertEquals(1, result.size());
      Assert.assertEquals("included-type.xml", result.iterator().next().getFilename());

      verify(mockAmbariMetaInfo, mockStackVersion, mockServiceInfo, mockPropertyInfo1, mockPropertyInfo2);
    }
  }

  public static class RunWithoutModules {
    @Test
    public void nullsAreEqual() {
      assertTrue(ConfigHelper.valuesAreEqual(null, null));
    }

    @Test
    public void equalStringsAreEqual() {
      assertTrue(ConfigHelper.valuesAreEqual("asdf", "asdf"));
      assertTrue(ConfigHelper.valuesAreEqual("qwerty", "qwerty"));
    }

    @Test
    public void nullIsNotEqualWithNonNull() {
      assertFalse(ConfigHelper.valuesAreEqual(null, "asdf"));
      assertFalse(ConfigHelper.valuesAreEqual("asdf", null));
    }

    @Test
    public void equalNumbersInDifferentFormsAreEqual() {
      assertTrue(ConfigHelper.valuesAreEqual("1.234", "1.2340"));
      assertTrue(ConfigHelper.valuesAreEqual("12.34", "1.234e1"));
      assertTrue(ConfigHelper.valuesAreEqual("123L", "123l"));
      assertTrue(ConfigHelper.valuesAreEqual("-1.234", "-1.2340"));
      assertTrue(ConfigHelper.valuesAreEqual("-12.34", "-1.234e1"));
      assertTrue(ConfigHelper.valuesAreEqual("-123L", "-123l"));
      assertTrue(ConfigHelper.valuesAreEqual("1f", "1.0f"));
      assertTrue(ConfigHelper.valuesAreEqual("0", "000"));

      // these are treated as different by NumberUtils (due to different types not being equal)
      assertTrue(ConfigHelper.valuesAreEqual("123", "123L"));
      assertTrue(ConfigHelper.valuesAreEqual("0", "0.0"));
    }

    @Test
    public void differentNumbersAreNotEqual() {
      assertFalse(ConfigHelper.valuesAreEqual("1.234", "1.2341"));
      assertFalse(ConfigHelper.valuesAreEqual("123L", "124L"));
      assertFalse(ConfigHelper.valuesAreEqual("-1.234", "1.234"));
      assertFalse(ConfigHelper.valuesAreEqual("-123L", "123L"));
      assertFalse(ConfigHelper.valuesAreEqual("-1.234", "-1.2341"));
      assertFalse(ConfigHelper.valuesAreEqual("-123L", "-124L"));
    }
  }
}
