/**
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
package org.apache.ambari.server.state.stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.stack.upgrade.ClusterGrouping;
import org.apache.ambari.server.stack.upgrade.ConfigureTask;
import org.apache.ambari.server.stack.upgrade.Direction;
import org.apache.ambari.server.stack.upgrade.ExecuteStage;
import org.apache.ambari.server.stack.upgrade.Grouping;
import org.apache.ambari.server.stack.upgrade.HostOrderGrouping;
import org.apache.ambari.server.stack.upgrade.ParallelScheduler;
import org.apache.ambari.server.stack.upgrade.RestartGrouping;
import org.apache.ambari.server.stack.upgrade.RestartTask;
import org.apache.ambari.server.stack.upgrade.ServiceCheckGrouping;
import org.apache.ambari.server.stack.upgrade.StopGrouping;
import org.apache.ambari.server.stack.upgrade.Task;
import org.apache.ambari.server.stack.upgrade.UpdateStackGrouping;
import org.apache.ambari.server.stack.upgrade.UpgradePack;
import org.apache.ambari.server.stack.upgrade.UpgradePack.PrerequisiteCheckConfig;
import org.apache.ambari.server.stack.upgrade.UpgradePack.ProcessingComponent;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Tests for the upgrade pack
 */
@Category({ category.StackUpgradeTest.class})
public class UpgradePackTest {

  private Injector injector;
  private AmbariMetaInfo ambariMetaInfo;

  private static final Logger LOG = LoggerFactory.getLogger(UpgradePackTest.class);

  @Before
  public void before() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  /**
   * Tests that boolean values are property serialized in the upgrade pack.
   *
   * @throws Exception
   */
  @Test
  public void testIsDowngradeAllowed() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.2.0");
    assertTrue(upgrades.size() > 0);

    String upgradePackWithoutDowngrade = "upgrade_test_no_downgrade";
    boolean foundAtLeastOnePackWithoutDowngrade = false;

    for (String key : upgrades.keySet()) {
      UpgradePack upgradePack = upgrades.get(key);
      if (upgradePack.getName().equals(upgradePackWithoutDowngrade)) {
        foundAtLeastOnePackWithoutDowngrade = true;
        assertFalse(upgradePack.isDowngradeAllowed());
        continue;
      }

      assertTrue(upgradePack.isDowngradeAllowed());
    }

    assertTrue(foundAtLeastOnePackWithoutDowngrade);
  }

  @Test
  public void testExistence() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("foo", "bar");
    assertTrue(upgrades.isEmpty());

    upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.size() > 0);
    assertTrue(upgrades.containsKey("upgrade_test"));
  }

  @Test
  public void testUpgradeParsing() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.size() > 0);
    assertTrue(upgrades.containsKey("upgrade_test"));
    UpgradePack upgrade = upgrades.get("upgrade_test");
    assertEquals("2.2.*.*", upgrade.getTarget());

    Map<String, List<String>> expectedStages = new LinkedHashMap<String, List<String>>() {{
      put("ZOOKEEPER", Arrays.asList("ZOOKEEPER_SERVER"));
      put("HDFS", Arrays.asList("NAMENODE", "DATANODE"));
    }};

    // !!! test the tasks
    int i = 0;
    for (Entry<String, List<String>> entry : expectedStages.entrySet()) {
      assertTrue(upgrade.getTasks().containsKey(entry.getKey()));
      assertEquals(i++, indexOf(upgrade.getTasks(), entry.getKey()));

      // check that the number of components matches
      assertEquals(entry.getValue().size(), upgrade.getTasks().get(entry.getKey()).size());

      // check component ordering
      int j = 0;
      for (String comp : entry.getValue()) {
        assertEquals(j++, indexOf(upgrade.getTasks().get(entry.getKey()), comp));
      }
    }

    // !!! test specific tasks
    assertTrue(upgrade.getTasks().containsKey("HDFS"));
    assertTrue(upgrade.getTasks().get("HDFS").containsKey("NAMENODE"));

    ProcessingComponent pc = upgrade.getTasks().get("HDFS").get("NAMENODE");
    assertNotNull(pc.preTasks);
    assertNotNull(pc.postTasks);
    assertNotNull(pc.tasks);
    assertNotNull(pc.preDowngradeTasks);
    assertNotNull(pc.postDowngradeTasks);
    assertEquals(1, pc.tasks.size());

    assertEquals(3, pc.preDowngradeTasks.size());
    assertEquals(1, pc.postDowngradeTasks.size());

    assertEquals(Task.Type.RESTART, pc.tasks.get(0).getType());
    assertEquals(RestartTask.class, pc.tasks.get(0).getClass());


    assertTrue(upgrade.getTasks().containsKey("ZOOKEEPER"));
    assertTrue(upgrade.getTasks().get("ZOOKEEPER").containsKey("ZOOKEEPER_SERVER"));

    pc = upgrade.getTasks().get("HDFS").get("DATANODE");
    assertNotNull(pc.preDowngradeTasks);
    assertEquals(0, pc.preDowngradeTasks.size());
    assertNotNull(pc.postDowngradeTasks);
    assertEquals(1, pc.postDowngradeTasks.size());


    pc = upgrade.getTasks().get("ZOOKEEPER").get("ZOOKEEPER_SERVER");
    assertNotNull(pc.preTasks);
    assertEquals(1, pc.preTasks.size());
    assertNotNull(pc.postTasks);
    assertEquals(1, pc.postTasks.size());
    assertNotNull(pc.tasks);
    assertEquals(1, pc.tasks.size());

    pc = upgrade.getTasks().get("YARN").get("NODEMANAGER");
    assertNotNull(pc.preTasks);
    assertEquals(2, pc.preTasks.size());
    Task t = pc.preTasks.get(1);
    assertEquals(ConfigureTask.class, t.getClass());
    ConfigureTask ct = (ConfigureTask) t;
    // check that the Configure task successfully parsed id
    assertEquals("hdp_2_1_1_nm_pre_upgrade", ct.getId());
    assertFalse(ct.supportsPatch);
  }

  @Test
  public void testGroupOrdersForRolling() {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.size() > 0);
    assertTrue(upgrades.containsKey("upgrade_test_checks"));
    UpgradePack upgrade = upgrades.get("upgrade_test_checks");

    PrerequisiteCheckConfig prerequisiteCheckConfig = upgrade.getPrerequisiteCheckConfig();
    assertNotNull(prerequisiteCheckConfig);
    assertNotNull(prerequisiteCheckConfig.globalProperties);
    assertTrue(prerequisiteCheckConfig.getGlobalProperties().containsKey("global-property-1"));
    assertEquals("global-value-1", prerequisiteCheckConfig.getGlobalProperties().get("global-property-1"));
    assertNotNull(prerequisiteCheckConfig.prerequisiteCheckProperties);
    assertEquals(2, prerequisiteCheckConfig.prerequisiteCheckProperties.size());
    assertNotNull(prerequisiteCheckConfig.getCheckProperties(
        "org.apache.ambari.server.checks.ServicesMapReduceDistributedCacheCheck"));
    assertTrue(prerequisiteCheckConfig.getCheckProperties(
        "org.apache.ambari.server.checks.ServicesMapReduceDistributedCacheCheck").containsKey("dfs-protocols-regex"));
    assertEquals("^([^:]*dfs|wasb|ecs):.*", prerequisiteCheckConfig.getCheckProperties(
        "org.apache.ambari.server.checks.ServicesMapReduceDistributedCacheCheck").get("dfs-protocols-regex"));
    assertNotNull(prerequisiteCheckConfig.getCheckProperties(
        "org.apache.ambari.server.checks.ServicesTezDistributedCacheCheck"));
    assertTrue(prerequisiteCheckConfig.getCheckProperties(
        "org.apache.ambari.server.checks.ServicesTezDistributedCacheCheck").containsKey("dfs-protocols-regex"));
    assertEquals("^([^:]*dfs|wasb|ecs):.*", prerequisiteCheckConfig.getCheckProperties(
        "org.apache.ambari.server.checks.ServicesTezDistributedCacheCheck").get("dfs-protocols-regex"));


    List<String> expected_up = Arrays.asList(
        "PRE_CLUSTER",
        "ZOOKEEPER",
        "CORE_MASTER",
        "SERVICE_CHECK_1",
        "CORE_SLAVES",
        "SERVICE_CHECK_2",
        "OOZIE",
        "POST_CLUSTER");

    List<String> expected_down = Arrays.asList(
        "PRE_CLUSTER",
        "OOZIE",
        "CORE_SLAVES",
        "SERVICE_CHECK_2",
        "CORE_MASTER",
        "SERVICE_CHECK_1",
        "ZOOKEEPER",
        "POST_CLUSTER");

    Grouping serviceCheckGroup = null;

    int i = 0;
    List<Grouping> groups = upgrade.getGroups(Direction.UPGRADE);
    for (Grouping g : groups) {
      assertEquals(expected_up.get(i), g.name);
      i++;

      if (g.name.equals("SERVICE_CHECK_1")) {
        serviceCheckGroup = g;
      }
    }

    List<String> expected_priority = Arrays.asList("HDFS", "HBASE", "YARN");

    assertNotNull(serviceCheckGroup);
    assertEquals(ServiceCheckGrouping.class, serviceCheckGroup.getClass());
    ServiceCheckGrouping scg = (ServiceCheckGrouping) serviceCheckGroup;

    Set<String> priorities = scg.getPriorities();
    assertEquals(3, priorities.size());

    i = 0;
    for (String s : priorities) {
      assertEquals(expected_priority.get(i++), s);
    }


    i = 0;
    groups = upgrade.getGroups(Direction.DOWNGRADE);
    for (Grouping g : groups) {
      assertEquals(expected_down.get(i), g.name);
      i++;
    }

  }


  // TODO AMBARI-12698, add the Downgrade case
  @Test
  public void testGroupOrdersForNonRolling() {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.size() > 0);
    assertTrue(upgrades.containsKey("upgrade_test_nonrolling"));
    UpgradePack upgrade = upgrades.get("upgrade_test_nonrolling");

    List<String> expected_up = Arrays.asList(
      "PRE_CLUSTER",
      "Stop High-Level Daemons",
      "Backups",
      "Stop Low-Level Daemons",
      "UPDATE_DESIRED_REPOSITORY_ID",
      "ALL_HOST_OPS",
      "ZOOKEEPER",
      "HDFS",
      "MR and YARN",
      "POST_CLUSTER");

    List<String> expected_down = Arrays.asList(
      "Restore Backups",
      "UPDATE_DESIRED_REPOSITORY_ID",
      "ALL_HOST_OPS",
      "ZOOKEEPER",
      "HDFS",
      "MR and YARN",
      "POST_CLUSTER");


    Iterator<String> itr_up = expected_up.iterator();
    List<Grouping> upgrade_groups = upgrade.getGroups(Direction.UPGRADE);
    for (Grouping g : upgrade_groups) {
      assertEquals(true, itr_up.hasNext());
      assertEquals(itr_up.next(), g.name);
    }

    Iterator<String> itr_down = expected_down.iterator();
    List<Grouping> downgrade_groups = upgrade.getGroups(Direction.DOWNGRADE);
    for (Grouping g : downgrade_groups) {
      assertEquals(true, itr_down.hasNext());
      assertEquals(itr_down.next(), g.name);
    }
  }


  @Test
  public void testDirectionForRolling() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.size() > 0);
    assertTrue(upgrades.containsKey("upgrade_direction"));
    UpgradePack upgrade = upgrades.get("upgrade_direction");
    assertTrue(upgrade.getType() == UpgradeType.ROLLING);

    List<Grouping> groups = upgrade.getGroups(Direction.UPGRADE);
    assertEquals(4, groups.size());
    Grouping group = groups.get(2);
    assertEquals(ClusterGrouping.class, group.getClass());
    ClusterGrouping cluster_group = (ClusterGrouping) group;
    assertEquals("Run on All", group.title);

    cluster_group = (ClusterGrouping) groups.get(3);
    List<ExecuteStage> stages = cluster_group.executionStages;
    assertEquals(3, stages.size());
    assertNotNull(stages.get(0).intendedDirection);
    assertEquals(Direction.DOWNGRADE, stages.get(0).intendedDirection);

    groups = upgrade.getGroups(Direction.DOWNGRADE);
    assertEquals(3, groups.size());
    // there are two clustergroupings at the end
    group = groups.get(1);
    assertEquals(ClusterGrouping.class, group.getClass());
    assertEquals("Run on All", group.title);

    group = groups.get(2);
    assertEquals(ClusterGrouping.class, group.getClass());
    assertEquals("Finalize Upgrade", group.title);
  }

  @Test
  public void testSkippableFailures() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    Set<String> keys = upgrades.keySet();
    for (String key : keys) {
      Assert.assertFalse(upgrades.get(key).isComponentFailureAutoSkipped());
      Assert.assertFalse(upgrades.get(key).isServiceCheckFailureAutoSkipped());
    }

    upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.2.0");
    UpgradePack upgradePack = upgrades.get("upgrade_test_skip_failures");
    Assert.assertTrue(upgradePack.isComponentFailureAutoSkipped());
    Assert.assertTrue(upgradePack.isServiceCheckFailureAutoSkipped());
  }

  /**
   * Tests that the XML for not auto skipping skippable failures works.
   *
   * @throws Exception
   */
  @Test
  public void testNoAutoSkipFailure() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.2.0");
    UpgradePack upgradePack = upgrades.get("upgrade_test_skip_failures");

    List<Grouping> groups = upgradePack.getGroups(Direction.UPGRADE);
    for (Grouping grouping : groups) {
      if (grouping.name.equals("SKIPPABLE_BUT_NOT_AUTO_SKIPPABLE")) {
        Assert.assertFalse(grouping.supportsAutoSkipOnFailure);
      } else {
        Assert.assertTrue(grouping.supportsAutoSkipOnFailure);
      }
    }
  }

  @Test
  public void testDirectionForNonRolling() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    assertTrue(upgrades.size() > 0);
    assertTrue(upgrades.containsKey("upgrade_test_nonrolling"));
    UpgradePack upgrade = upgrades.get("upgrade_test_nonrolling");
    assertTrue(upgrade.getType() == UpgradeType.NON_ROLLING);

    List<Grouping> groups = upgrade.getGroups(Direction.UPGRADE);
    assertEquals(10, groups.size());

    Grouping group = null;
    ClusterGrouping clusterGroup = null;
    UpdateStackGrouping updateStackGroup = null;
    StopGrouping stopGroup = null;
    RestartGrouping restartGroup = null;

    group = groups.get(0);
    assertEquals(ClusterGrouping.class, group.getClass());
    clusterGroup = (ClusterGrouping) group;
    assertEquals("Prepare Upgrade", clusterGroup.title);
    assertNull(clusterGroup.parallelScheduler);

    group = groups.get(1);
    assertEquals(StopGrouping.class, group.getClass());
    stopGroup = (StopGrouping) group;
    assertEquals("Stop Daemons for High-Level Services", stopGroup.title);
    assertNotNull(stopGroup.parallelScheduler);
    assertEquals(ParallelScheduler.DEFAULT_MAX_DEGREE_OF_PARALLELISM, stopGroup.parallelScheduler.maxDegreeOfParallelism);

    group = groups.get(2);
    assertEquals(ClusterGrouping.class, group.getClass());
    clusterGroup = (ClusterGrouping) group;
    assertEquals("Take Backups", clusterGroup.title);
    assertNull(clusterGroup.parallelScheduler);

    group = groups.get(3);
    assertEquals(StopGrouping.class, group.getClass());
    stopGroup = (StopGrouping) group;
    assertEquals("Stop Daemons for Low-Level Services", stopGroup.title);
    assertNotNull(stopGroup.parallelScheduler);
    assertEquals(ParallelScheduler.DEFAULT_MAX_DEGREE_OF_PARALLELISM, stopGroup.parallelScheduler.maxDegreeOfParallelism);

    group = groups.get(4);
    assertEquals(UpdateStackGrouping.class, group.getClass());
    updateStackGroup = (UpdateStackGrouping) group;
    assertEquals("Update Desired Stack Id", updateStackGroup.title);
    assertNull(updateStackGroup.parallelScheduler);

    group = groups.get(5);
    assertEquals(ClusterGrouping.class, group.getClass());
    clusterGroup = (ClusterGrouping) group;
    assertEquals("Set Version On All Hosts", clusterGroup.title);
    assertNull(clusterGroup.parallelScheduler);

    group = groups.get(6);
    assertEquals(RestartGrouping.class, group.getClass());
    restartGroup = (RestartGrouping) group;
    assertEquals("Zookeeper", restartGroup.title);
    assertNull(restartGroup.parallelScheduler);

    group = groups.get(7);
    assertEquals(RestartGrouping.class, group.getClass());
    restartGroup = (RestartGrouping) group;
    assertEquals("HDFS", restartGroup.title);
    assertNotNull(restartGroup.parallelScheduler);
    assertEquals(2, restartGroup.parallelScheduler.maxDegreeOfParallelism);

    group = groups.get(8);
    assertEquals(RestartGrouping.class, group.getClass());
    restartGroup = (RestartGrouping) group;
    assertEquals("MR and YARN", restartGroup.title);
    assertNotNull(restartGroup.parallelScheduler);
    assertEquals(ParallelScheduler.DEFAULT_MAX_DEGREE_OF_PARALLELISM, restartGroup.parallelScheduler.maxDegreeOfParallelism);

    group = groups.get(9);
    assertEquals(ClusterGrouping.class, group.getClass());
    clusterGroup = (ClusterGrouping) group;
    assertEquals("Finalize {{direction.text.proper}}", clusterGroup.title);
    assertNull(clusterGroup.parallelScheduler);
  }


  /**
   * Tests that the service level XML merges correctly for 2.0.5/HDFS/HDP/2.2.0.
   *
   * @throws Exception
   */
  @Test
  public void testServiceLevelUpgradePackMerge() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.2.0");
    assertTrue(upgrades.containsKey("upgrade_test_15388"));

    UpgradePack upgradePack = upgrades.get("upgrade_test_15388");

    List<String> checks = upgradePack.getPrerequisiteChecks();
    assertEquals(11, checks.size());
    assertTrue(checks.contains("org.apache.ambari.server.checks.FooCheck"));

    List<Grouping> groups = upgradePack.getGroups(Direction.UPGRADE);
    assertEquals(8, groups.size());
    Grouping group = groups.get(0);
    assertEquals(ClusterGrouping.class, group.getClass());
    ClusterGrouping cluster_group = (ClusterGrouping) group;
    assertEquals("Pre {{direction.text.proper}}", cluster_group.title);

    List<ExecuteStage> stages = cluster_group.executionStages;
    assertEquals(5, stages.size());
    ExecuteStage stage = stages.get(3);
    assertEquals("Backup FOO", stage.title);

    group = groups.get(2);
    assertEquals("Core Masters", group.title);
    List<UpgradePack.OrderService> services = group.services;
    assertEquals(3, services.size());
    UpgradePack.OrderService service = services.get(2);
    assertEquals("HBASE", service.serviceName);

    group = groups.get(3);
    assertEquals("Core Slaves", group.title);
    services = group.services;
    assertEquals(3, services.size());
    service = services.get(1);
    assertEquals("HBASE", service.serviceName);

    group = groups.get(4);
    assertEquals(ServiceCheckGrouping.class, group.getClass());
    ServiceCheckGrouping scGroup = (ServiceCheckGrouping) group;
    Set<String> priorityServices = scGroup.getPriorities();
    assertEquals(4, priorityServices.size());
    Iterator<String> serviceIterator = priorityServices.iterator();
    assertEquals("ZOOKEEPER", serviceIterator.next());
    assertEquals("HBASE", serviceIterator.next());

    group = groups.get(5);
    assertEquals("Hive", group.title);

    group = groups.get(6);
    assertEquals("Foo", group.title);
    services = group.services;
    assertEquals(2, services.size());
    service = services.get(1);
    assertEquals("FOO2", service.serviceName);

    Map<String, Map<String, ProcessingComponent>> tasks = upgradePack.getTasks();
    assertTrue(tasks.containsKey("HBASE"));

    // !!! generalized upgrade pack shouldn't be in this
    boolean found = false;
    for (Grouping grouping : upgradePack.getAllGroups()) {
      if (grouping.name.equals("GANGLIA_UPGRADE")) {
        found = true;
        break;
      }
    }
    assertFalse(found);

    // !!! test merge of a generalized upgrade pack
    upgradePack = upgrades.get("upgrade_test_conditions");
    assertNotNull(upgradePack);
    for (Grouping grouping : upgradePack.getAllGroups()) {
      if (grouping.name.equals("GANGLIA_UPGRADE")) {
        found = true;
        break;
      }
    }
    assertTrue(found);
  }


  @Test
  public void testPackWithHostGroup() {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.2.0");
    UpgradePack upgradePack = upgrades.get("upgrade_test_host_ordered");

    assertNotNull(upgradePack);
    assertEquals(upgradePack.getType(), UpgradeType.HOST_ORDERED);
    assertEquals(3, upgradePack.getAllGroups().size());

    assertEquals(HostOrderGrouping.class, upgradePack.getAllGroups().get(0).getClass());
    assertEquals(Grouping.class, upgradePack.getAllGroups().get(1).getClass());
  }

  @Test
  public void testDowngradeComponentTasks() throws Exception {
    Map<String, UpgradePack> upgrades = ambariMetaInfo.getUpgradePacks("HDP", "2.1.1");
    UpgradePack upgradePack = upgrades.get("upgrade_component_tasks_test");
    assertNotNull(upgradePack);

    Map<String, Map<String, ProcessingComponent>> components = upgradePack.getTasks();
    assertTrue(components.containsKey("ZOOKEEPER"));
    assertTrue(components.containsKey("HDFS"));

    Map<String, ProcessingComponent> zkMap = components.get("ZOOKEEPER");
    Map<String, ProcessingComponent> hdfsMap = components.get("HDFS");

    assertTrue(zkMap.containsKey("ZOOKEEPER_SERVER"));
    assertTrue(zkMap.containsKey("ZOOKEEPER_CLIENT"));
    assertTrue(hdfsMap.containsKey("NAMENODE"));
    assertTrue(hdfsMap.containsKey("DATANODE"));
    assertTrue(hdfsMap.containsKey("HDFS_CLIENT"));
    assertTrue(hdfsMap.containsKey("JOURNALNODE"));

    ProcessingComponent zkServer = zkMap.get("ZOOKEEPER_SERVER");
    ProcessingComponent zkClient = zkMap.get("ZOOKEEPER_CLIENT");
    ProcessingComponent hdfsNN = hdfsMap.get("NAMENODE");
    ProcessingComponent hdfsDN = hdfsMap.get("DATANODE");
    ProcessingComponent hdfsClient = hdfsMap.get("HDFS_CLIENT");
    ProcessingComponent hdfsJN = hdfsMap.get("JOURNALNODE");

    // ZK server has only pretasks defined, with pre-downgrade being a copy of pre-upgrade
    assertNotNull(zkServer.preTasks);
    assertNotNull(zkServer.preDowngradeTasks);
    assertNull(zkServer.postTasks);
    assertNull(zkServer.postDowngradeTasks);
    assertEquals(1, zkServer.preTasks.size());
    assertEquals(1, zkServer.preDowngradeTasks.size());

    // ZK client has only post-tasks defined, with post-downgrade being a copy of pre-upgrade
    assertNull(zkClient.preTasks);
    assertNull(zkClient.preDowngradeTasks);
    assertNotNull(zkClient.postTasks);
    assertNotNull(zkClient.postDowngradeTasks);
    assertEquals(1, zkClient.postTasks.size());
    assertEquals(1, zkClient.postDowngradeTasks.size());

    // NN has only pre-tasks defined, with an empty pre-downgrade
    assertNotNull(hdfsNN.preTasks);
    assertNotNull(hdfsNN.preDowngradeTasks);
    assertNull(hdfsNN.postTasks);
    assertNull(hdfsNN.postDowngradeTasks);
    assertEquals(1, hdfsNN.preTasks.size());
    assertEquals(0, hdfsNN.preDowngradeTasks.size());

    // DN has only post-tasks defined, with post-downgrade being empty
    assertNull(hdfsDN.preTasks);
    assertNull(hdfsDN.preDowngradeTasks);
    assertNotNull(hdfsDN.postTasks);
    assertNotNull(hdfsDN.postDowngradeTasks);
    assertEquals(1, hdfsDN.postTasks.size());
    assertEquals(0, hdfsDN.postDowngradeTasks.size());

    // HDFS client has only post and post-downgrade tasks
    assertNull(hdfsClient.preTasks);
    assertNotNull(hdfsClient.preDowngradeTasks);
    assertNull(hdfsClient.postTasks);
    assertNotNull(hdfsClient.postDowngradeTasks);
    assertEquals(1, hdfsClient.preDowngradeTasks.size());
    assertEquals(1, hdfsClient.postDowngradeTasks.size());

    // JN has differing tasks for pre and post downgrade
    assertNotNull(hdfsJN.preTasks);
    assertNotNull(hdfsJN.preDowngradeTasks);
    assertNotNull(hdfsJN.postTasks);
    assertNotNull(hdfsJN.postDowngradeTasks);
    assertEquals(1, hdfsJN.preTasks.size());
    assertEquals(2, hdfsJN.preDowngradeTasks.size());
    assertEquals(1, hdfsJN.postTasks.size());
    assertEquals(2, hdfsJN.postDowngradeTasks.size());

    // make sure all ids are accounted for

    Set<String> allIds = Sets.newHashSet("some_id", "some_id1", "some_id2", "some_id3", "some_id4", "some_id5");

    @SuppressWarnings("unchecked")
    Set<List<Task>> allTasks = Sets.newHashSet(hdfsJN.preTasks, hdfsJN.preDowngradeTasks,
        hdfsJN.postTasks, hdfsJN.postDowngradeTasks);

    for (List<Task> list : allTasks) {
      for (Task t : list) {
        assertEquals(ConfigureTask.class, t.getClass());

        ConfigureTask ct = (ConfigureTask) t;
        assertTrue(allIds.contains(ct.id));

        allIds.remove(ct.id);
      }
    }

    assertTrue(allIds.isEmpty());
  }


  private int indexOf(Map<String, ?> map, String keyToFind) {
    int result = -1;

    int i = 0;
    for (Entry<String, ?> entry : map.entrySet()) {
      if (entry.getKey().equals(keyToFind)) {
        return i;
      }
      i++;
    }

    return result;
  }
}
