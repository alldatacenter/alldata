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
package org.apache.ambari.server.serveraction.upgrades;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.orm.entities.UpgradeHistoryEntity;
import org.apache.ambari.server.serveraction.ServerAction;
import org.apache.ambari.server.stack.upgrade.ConfigUpgradeChangeDefinition.ConfigurationKeyValue;
import org.apache.ambari.server.stack.upgrade.ConfigUpgradeChangeDefinition.IfValueMatchType;
import org.apache.ambari.server.stack.upgrade.ConfigUpgradeChangeDefinition.Insert;
import org.apache.ambari.server.stack.upgrade.ConfigUpgradeChangeDefinition.InsertType;
import org.apache.ambari.server.stack.upgrade.ConfigUpgradeChangeDefinition.Replace;
import org.apache.ambari.server.stack.upgrade.ConfigUpgradeChangeDefinition.Transfer;
import org.apache.ambari.server.stack.upgrade.ConfigureTask;
import org.apache.ambari.server.stack.upgrade.PropertyKeyState;
import org.apache.ambari.server.stack.upgrade.TransferCoercionType;
import org.apache.ambari.server.stack.upgrade.TransferOperation;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Tests upgrade-related server side actions
 */
public class ConfigureActionTest {

  @Inject
  private Injector m_injector;

  @Inject
  private OrmTestHelper m_helper;

  @Inject
  private HostRoleCommandFactory hostRoleCommandFactory;

  @Inject
  private ServiceFactory serviceFactory;

  @Inject
  private ConfigHelper m_configHelper;

  @Inject
  private Clusters clusters;

  @Inject
  private ConfigFactory configFactory;

  @Inject
  private ConfigureAction action;

  @Inject
  private RequestDAO requestDAO;

  @Inject
  private UpgradeDAO upgradeDAO;

  @Inject
  private ServiceComponentFactory serviceComponentFactory;

  @Inject
  private ServiceComponentHostFactory serviceComponentHostFactory;

  private RepositoryVersionEntity repoVersion2110;
  private RepositoryVersionEntity repoVersion2111;
  private RepositoryVersionEntity repoVersion2200;

  private final Map<String, Map<String, String>> NO_ATTRIBUTES = new HashMap<>();

  @Before
  public void setup() throws Exception {
    m_injector = Guice.createInjector(new InMemoryDefaultTestModule());
    m_injector.getInstance(GuiceJpaInitializer.class);
    m_injector.injectMembers(this);

    repoVersion2110 = m_helper.getOrCreateRepositoryVersion(new StackId("HDP-2.1.1"), "2.1.1.0-1234");
    repoVersion2111 = m_helper.getOrCreateRepositoryVersion(new StackId("HDP-2.1.1"), "2.1.1.1-5678");
    repoVersion2200 = m_helper.getOrCreateRepositoryVersion(new StackId("HDP-2.2.0"), "2.2.0.0-1234");

    makeUpgradeCluster();
  }

  @After
  public void teardown() throws Exception {
    H2DatabaseCleaner.clearDatabase(m_injector.getProvider(EntityManager.class).get());
  }


  /**
   * Tests that a new configuration is created when upgrading across stack when
   * there is no existing configuration with the correct target stack.
   *
   * @throws Exception
   */
  @Test
  public void testNewConfigCreatedWhenUpgradingAcrossStacks() throws Exception {
    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    Map<String, String> properties = new HashMap<String, String>() {
      {
        put("initLimit", "10");
      }
    };

    Config config = createConfig(c, repoVersion2110, "zoo.cfg", "version2", properties);

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    List<ConfigurationKeyValue> configurations = new ArrayList<>();
    ConfigurationKeyValue keyValue = new ConfigurationKeyValue();
    configurations.add(keyValue);
    keyValue.key = "initLimit";
    keyValue.value = "11";

    createUpgrade(c, repoVersion2200);

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS, new Gson().toJson(configurations));

    ExecutionCommand executionCommand = getExecutionCommand(commandParams);
    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null,
        null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(
        executionCommand));

    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(3, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertFalse(StringUtils.equals("version2", config.getTag()));
    assertEquals("11", config.getProperties().get("initLimit"));
  }

  /**
   * Tests that if a configuration with the target stack already exists, then it
   * will be re-used instead of a new one created.
   *
   * @throws Exception
   */
  @Test
  public void testConfigurationWithTargetStackUsed() throws Exception {
    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    Map<String, String> properties = new HashMap<String, String>() {
      {
        put("initLimit", "10");
      }
    };

    Config config = createConfig(c, repoVersion2200, "zoo.cfg", "version2", properties);

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    List<ConfigurationKeyValue> configurations = new ArrayList<>();
    ConfigurationKeyValue keyValue = new ConfigurationKeyValue();
    configurations.add(keyValue);
    keyValue.key = "initLimit";
    keyValue.value = "11";

    createUpgrade(c, repoVersion2200);

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS, new Gson().toJson(configurations));

    ExecutionCommand executionCommand = getExecutionCommand(commandParams);
    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));

    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertEquals("version2", config.getTag());
    assertEquals("11", config.getProperties().get("initLimit"));
  }

  /**
   * Tests that DELETE "*" with edit preserving works correctly.
   *
   * @throws Exception
   */
  @Test
  public void testDeletePreserveChanges() throws Exception {
    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    // create a config for zoo.cfg with two values; one is a stack value and the
    // other is custom
    Map<String, String> properties = new HashMap<String, String>() {
      {
        put("tickTime", "2000");
        put("foo", "bar");
      }
    };

    Config config = createConfig(c, repoVersion2110, "zoo.cfg", "version2", properties);

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");

    createUpgrade(c, repoVersion2111);

    // delete all keys, preserving edits or additions
    List<Transfer> transfers = new ArrayList<>();
    Transfer transfer = new Transfer();
    transfer.operation = TransferOperation.DELETE;
    transfer.deleteKey = "*";
    transfer.preserveEdits = true;
    transfers.add(transfer);

    commandParams.put(ConfigureTask.PARAMETER_TRANSFERS, new Gson().toJson(transfers));

    ExecutionCommand executionCommand = getExecutionCommand(commandParams);
    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));

    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    // make sure there are now 3 versions after the merge
    assertEquals(3, c.getConfigsByType("zoo.cfg").size());
    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertFalse("version2".equals(config.getTag()));

    // time to check our values; there should only be 1 left since tickTime was
    // removed
    Map<String, String> map = config.getProperties();
    assertEquals("bar", map.get("foo"));
    assertFalse(map.containsKey("tickTime"));
  }

  @Test
  public void testConfigTransferCopy() throws Exception {
    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    Map<String, String> properties = new HashMap<String, String>() {
      {
        put("initLimit", "10");
        put("copyIt", "10");
        put("moveIt", "10");
        put("deleteIt", "10");
      }
    };

    Config config = createConfig(c, repoVersion2110, "zoo.cfg", "version2", properties);

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    List<ConfigurationKeyValue> configurations = new ArrayList<>();
    ConfigurationKeyValue keyValue = new ConfigurationKeyValue();
    configurations.add(keyValue);
    keyValue.key = "initLimit";
    keyValue.value = "11";

    createUpgrade(c, repoVersion2111);

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS, new Gson().toJson(configurations));

    // normal copy
    List<Transfer> transfers = new ArrayList<>();
    Transfer transfer = new Transfer();
    transfer.operation = TransferOperation.COPY;
    transfer.fromKey = "copyIt";
    transfer.toKey = "copyKey";
    transfers.add(transfer);

    // copy with default
    transfer = new Transfer();
    transfer.operation = TransferOperation.COPY;
    transfer.fromKey = "copiedFromMissingKeyWithDefault";
    transfer.toKey = "copiedToMissingKeyWithDefault";
    transfer.defaultValue = "defaultValue";
    transfers.add(transfer);

    // normal move
    transfer = new Transfer();
    transfer.operation = TransferOperation.MOVE;
    transfer.fromKey = "moveIt";
    transfer.toKey = "movedKey";
    transfers.add(transfer);

    // move with default
    transfer = new Transfer();
    transfer.operation = TransferOperation.MOVE;
    transfer.fromKey = "movedFromKeyMissingWithDefault";
    transfer.toKey = "movedToMissingWithDefault";
    transfer.defaultValue = "defaultValue2";
    transfer.mask = true;
    transfers.add(transfer);

    transfer = new Transfer();
    transfer.operation = TransferOperation.DELETE;
    transfer.deleteKey = "deleteIt";
    transfers.add(transfer);

    commandParams.put(ConfigureTask.PARAMETER_TRANSFERS, new Gson().toJson(transfers));

    ExecutionCommand executionCommand = getExecutionCommand(commandParams);
    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null,
        null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(
        executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(3, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertFalse("version2".equals(config.getTag()));

    Map<String, String> map = config.getProperties();
    assertEquals("11", map.get("initLimit"));
    assertEquals("10", map.get("copyIt"));
    assertTrue(map.containsKey("copyKey"));
    assertEquals(map.get("copyIt"), map.get("copyKey"));
    assertFalse(map.containsKey("moveIt"));
    assertTrue(map.containsKey("movedKey"));
    assertFalse(map.containsKey("deletedKey"));
    assertTrue(map.containsKey("copiedToMissingKeyWithDefault"));
    assertEquals("defaultValue", map.get("copiedToMissingKeyWithDefault"));
    assertTrue(map.containsKey("movedToMissingWithDefault"));
    assertEquals("defaultValue2", map.get("movedToMissingWithDefault"));

    transfers.clear();
    transfer = new Transfer();
    transfer.operation = TransferOperation.DELETE;
    transfer.deleteKey = "*";
    transfer.preserveEdits = true;
    transfer.keepKeys.add("copyKey");
    // The below key should be ignored/not added as it doesn't exist originally as part of transfer.
    transfer.keepKeys.add("keyNotExisting");
    // The 'null' passed as part of key should be ignored as part of transfer operation.
    transfer.keepKeys.add(null);


    transfers.add(transfer);
    commandParams.put(ConfigureTask.PARAMETER_TRANSFERS, new Gson().toJson(transfers));

    report = action.execute(null);
    assertNotNull(report);

    assertEquals(4, c.getConfigsByType("zoo.cfg").size());
    config = c.getDesiredConfigByType("zoo.cfg");
    map = config.getProperties();
    assertEquals(6, map.size());
    assertTrue(map.containsKey("initLimit")); // it just changed to 11 from 10
    assertTrue(map.containsKey("copyKey")); // is new
    // Below two keys should not have been added in the map.
    assertFalse(map.containsKey("keyNotExisting"));
    assertFalse(map.containsKey(null));
  }

  @Test
  public void testCoerceValueOnCopy() throws Exception {
    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    Map<String, String> properties = new HashMap<String, String>() {
      {
        put("zoo.server.csv", "c6401,c6402,  c6403");
      }
    };

    Config config = createConfig(c, repoVersion2110, "zoo.cfg", "version2", properties);

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    createUpgrade(c, repoVersion2111);

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");

    // copy with coerce
    List<Transfer> transfers = new ArrayList<>();
    Transfer transfer = new Transfer();
    transfer.operation = TransferOperation.COPY;
    transfer.coerceTo = TransferCoercionType.YAML_ARRAY;
    transfer.fromKey = "zoo.server.csv";
    transfer.toKey = "zoo.server.array";
    transfer.defaultValue = "['foo','bar']";
    transfers.add(transfer);

    commandParams.put(ConfigureTask.PARAMETER_TRANSFERS, new Gson().toJson(transfers));

    ExecutionCommand executionCommand = getExecutionCommand(commandParams);
    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(3, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertFalse("version2".equals(config.getTag()));

    Map<String, String> map = config.getProperties();
    assertEquals("c6401,c6402,  c6403", map.get("zoo.server.csv"));
    assertEquals("['c6401','c6402','c6403']", map.get("zoo.server.array"));
  }

  @Test
  public void testValueReplacement() throws Exception {
    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    Map<String, String> properties = new HashMap<String, String>() {
      {
        put("key_to_replace", "My New Cat");
        put("key_with_no_match", "WxyAndZ");
      }
    };

    Config config = createConfig(c, repoVersion2110, "zoo.cfg", "version2", properties);

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    createUpgrade(c, repoVersion2111);

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");

    // Replacement task
    List<Replace> replacements = new ArrayList<>();
    Replace replace = new Replace();
    replace.key = "key_to_replace";
    replace.find = "New Cat";
    replace.replaceWith = "Wet Dog";
    replacements.add(replace);

    replace = new Replace();
    replace.key = "key_with_no_match";
    replace.find = "abc";
    replace.replaceWith = "def";
    replacements.add(replace);

    commandParams.put(ConfigureTask.PARAMETER_REPLACEMENTS, new Gson().toJson(replacements));

    ExecutionCommand executionCommand = getExecutionCommand(commandParams);
    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(3, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertFalse("version2".equals(config.getTag()));

    assertEquals("My Wet Dog", config.getProperties().get("key_to_replace"));
    assertEquals("WxyAndZ", config.getProperties().get("key_with_no_match"));
  }

  /**
   * Tests that replacing a {@code null} value works.
   *
   * @throws Exception
   */
  @Test
  public void testValueReplacementWithMissingConfigurations() throws Exception {
    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    Map<String, String> properties = new HashMap<String, String>() {
      {
        put("existing", "This exists!");
        put("missing", null);
      }
    };

    Config config = createConfig(c, repoVersion2110, "zoo.cfg", "version2", properties);

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    createUpgrade(c, repoVersion2111);

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");

    // Replacement task
    List<Replace> replacements = new ArrayList<>();
    Replace replace = new Replace();
    replace.key = "missing";
    replace.find = "foo";
    replace.replaceWith = "bar";
    replacements.add(replace);

    commandParams.put(ConfigureTask.PARAMETER_REPLACEMENTS, new Gson().toJson(replacements));

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");
    executionCommand.setRoleParams(new HashMap<>());
    executionCommand.getRoleParams().put(ServerAction.ACTION_USER_NAME, "username");

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(3, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertEquals(null, config.getProperties().get("missing"));
  }

  @Test
  public void testMultipleKeyValuesPerTask() throws Exception {
    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    Map<String, String> properties = new HashMap<String, String>() {
      {
        put("fooKey", "barValue");
      }
    };

    Config config = createConfig(c, repoVersion2200, "zoo.cfg", "version2", properties);

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    // create several configurations
    List<ConfigurationKeyValue> configurations = new ArrayList<>();
    ConfigurationKeyValue fooKey2 = new ConfigurationKeyValue();
    configurations.add(fooKey2);
    fooKey2.key = "fooKey2";
    fooKey2.value = "barValue2";

    ConfigurationKeyValue fooKey3 = new ConfigurationKeyValue();
    configurations.add(fooKey3);
    fooKey3.key = "fooKey3";
    fooKey3.value = "barValue3";
    fooKey3.mask = true;

    createUpgrade(c, repoVersion2200);

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS, new Gson().toJson(configurations));

    ExecutionCommand executionCommand = getExecutionCommand(commandParams);
    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertEquals("barValue", config.getProperties().get("fooKey"));
    assertEquals("barValue2", config.getProperties().get("fooKey2"));
    assertEquals("barValue3", config.getProperties().get("fooKey3"));
    assertTrue(report.getStdOut().contains("******"));

  }

  @Test
  public void testAllowedSet() throws Exception {
    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    Map<String, String> properties = new HashMap<String, String>() {
      {
        put("set.key.1", "s1");
        put("set.key.2", "s2");
        put("set.key.3", "s3");
        put("set.key.4", "s4");
      }
    };

    Config config = createConfig(c, repoVersion2200, "zoo.cfg", "version2", properties);

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    // create several configurations
    List<ConfigurationKeyValue> configurations = new ArrayList<>();
    ConfigurationKeyValue fooKey1 = new ConfigurationKeyValue();
    configurations.add(fooKey1);
    fooKey1.key = "fooKey1";
    fooKey1.value = "barValue1";

    ConfigurationKeyValue fooKey2 = new ConfigurationKeyValue();
    configurations.add(fooKey2);
    fooKey2.key = "fooKey2";
    fooKey2.value = "barValue2";

    ConfigurationKeyValue fooKey3 = new ConfigurationKeyValue();
    configurations.add(fooKey3);
    fooKey3.key = "fooKey3";
    fooKey3.value = "barValue3";
    fooKey3.ifKey = "set.key.1";
    fooKey3.ifType = "zoo.cfg";
    fooKey3.ifValue = "s1";

    ConfigurationKeyValue fooKey4 = new ConfigurationKeyValue();
    configurations.add(fooKey4);
    fooKey4.key = "fooKey4";
    fooKey4.value = "barValue4";
    fooKey4.ifKey = "set.key.2";
    fooKey4.ifType = "zoo.cfg";
    fooKey4.ifKeyState= PropertyKeyState.PRESENT;

    ConfigurationKeyValue fooKey5 = new ConfigurationKeyValue();
    configurations.add(fooKey5);
    fooKey5.key = "fooKey5";
    fooKey5.value = "barValue5";
    fooKey5.ifKey = "abc";
    fooKey5.ifType = "zoo.cfg";
    fooKey5.ifKeyState= PropertyKeyState.ABSENT;

    createUpgrade(c, repoVersion2200);

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS, new Gson().toJson(configurations));

    ExecutionCommand executionCommand = getExecutionCommand(commandParams);
    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertEquals("barValue1", config.getProperties().get("fooKey1"));
    assertEquals("barValue2", config.getProperties().get("fooKey2"));
    assertEquals("barValue3", config.getProperties().get("fooKey3"));
    assertEquals("barValue4", config.getProperties().get("fooKey4"));
    assertEquals("barValue5", config.getProperties().get("fooKey5"));
    assertEquals("s1", config.getProperties().get("set.key.1"));
    assertEquals("s2", config.getProperties().get("set.key.2"));
    assertEquals("s3", config.getProperties().get("set.key.3"));
    assertEquals("s4", config.getProperties().get("set.key.4"));
  }

  @Test
  public void testDisallowedSet() throws Exception {
    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    Map<String, String> properties = new HashMap<String, String>() {
      {
        put("set.key.1", "s1");
        put("set.key.2", "s2");
        put("set.key.3", "s3");
        put("set.key.4", "s4");
      }
    };

    Config config = createConfig(c, repoVersion2200, "zoo.cfg", "version2", properties);

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    // create several configurations
    List<ConfigurationKeyValue> configurations = new ArrayList<>();
    ConfigurationKeyValue fooKey3 = new ConfigurationKeyValue();
    configurations.add(fooKey3);
    fooKey3.key = "fooKey3";
    fooKey3.value = "barValue3";
    fooKey3.ifKey = "set.key.1";
    fooKey3.ifType = "zoo.cfg";
    fooKey3.ifValue = "no-such-value";

    ConfigurationKeyValue fooKey4 = new ConfigurationKeyValue();
    configurations.add(fooKey4);
    fooKey4.key = "fooKey4";
    fooKey4.value = "barValue4";
    fooKey4.ifKey = "set.key.2";
    fooKey4.ifType = "zoo.cfg";
    fooKey4.ifKeyState= PropertyKeyState.ABSENT;

    ConfigurationKeyValue fooKey5 = new ConfigurationKeyValue();
    configurations.add(fooKey5);
    fooKey5.key = "fooKey5";
    fooKey5.value = "barValue5";
    fooKey5.ifKey = "abc";
    fooKey5.ifType = "zoo.cfg";
    fooKey5.ifKeyState= PropertyKeyState.PRESENT;

    createUpgrade(c, repoVersion2200);

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS, new Gson().toJson(configurations));

    ExecutionCommand executionCommand = getExecutionCommand(commandParams);
    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertEquals("s1", config.getProperties().get("set.key.1"));
    assertEquals("s2", config.getProperties().get("set.key.2"));
    assertEquals("s3", config.getProperties().get("set.key.3"));
    assertEquals("s4", config.getProperties().get("set.key.4"));
    assertFalse(config.getProperties().containsKey("fooKey3"));
    assertFalse(config.getProperties().containsKey("fooKey4"));
    assertFalse(config.getProperties().containsKey("fooKey5"));
  }

  @Test
  public void testAllowedReplacment() throws Exception {


    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    Map<String, String> properties = new HashMap<String, String>() {
      {
        put("replace.key.1", "r1");
        put("replace.key.2", "r2");
        put("replace.key.3", "r3a1");
        put("replace.key.4", "r4");
        put("replace.key.5", "r5");
      }
    };

    Config config = createConfig(c, repoVersion2200, "zoo.cfg", "version2", properties);

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    // create several configurations
    List<Replace> replacements = new ArrayList<>();
    Replace replace = new Replace();
    replace.key = "replace.key.3";
    replace.find = "a";
    replace.replaceWith = "A";
    replacements.add(replace);

    Replace replace2 = new Replace();
    replacements.add(replace2);
    replace2.key = "replace.key.4";
    replace2.find = "r";
    replace2.replaceWith = "R";
    replace2.ifKey = "replace.key.1";
    replace2.ifType = "zoo.cfg";
    replace2.ifValue = "r1";
    replacements.add(replace2);

    Replace replace3 = new Replace();
    replacements.add(replace3);
    replace3.key = "replace.key.2";
    replace3.find = "r";
    replace3.replaceWith = "R";
    replace3.ifKey = "replace.key.1";
    replace3.ifType = "zoo.cfg";
    replace3.ifKeyState = PropertyKeyState.PRESENT;
    replacements.add(replace3);

    Replace replace4 = new Replace();
    replacements.add(replace3);
    replace4.key = "replace.key.5";
    replace4.find = "r";
    replace4.replaceWith = "R";
    replace4.ifKey = "no.such.key";
    replace4.ifType = "zoo.cfg";
    replace4.ifKeyState = PropertyKeyState.ABSENT;
    replacements.add(replace4);

    createUpgrade(c, repoVersion2200);

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_REPLACEMENTS, new Gson().toJson(replacements));

    ExecutionCommand executionCommand = getExecutionCommand(commandParams);
    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertEquals("r1", config.getProperties().get("replace.key.1"));
    assertEquals("R2", config.getProperties().get("replace.key.2"));
    assertEquals("r3A1", config.getProperties().get("replace.key.3"));
    assertEquals("R4", config.getProperties().get("replace.key.4"));
    assertEquals("R5", config.getProperties().get("replace.key.5"));
  }

  @Test
  public void testDisallowedReplacment() throws Exception {
    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    Map<String, String> properties = new HashMap<String, String>() {
      {
        put("replace.key.1", "r1");
        put("replace.key.2", "r2");
        put("replace.key.3", "r3a1");
        put("replace.key.4", "r4");
        put("replace.key.5", "r5");
      }
    };

    Config config = createConfig(c, repoVersion2110, "zoo.cfg", "version2", properties);

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    // create several configurations
    List<Replace> replacements = new ArrayList<>();

    Replace replace2 = new Replace();
    replacements.add(replace2);
    replace2.key = "replace.key.4";
    replace2.find = "r";
    replace2.replaceWith = "R";
    replace2.ifKey = "replace.key.1";
    replace2.ifType = "zoo.cfg";
    replace2.ifValue = "not-this-value";
    replacements.add(replace2);

    Replace replace3 = new Replace();
    replacements.add(replace3);
    replace3.key = "replace.key.2";
    replace3.find = "r";
    replace3.replaceWith = "R";
    replace3.ifKey = "replace.key.1";
    replace3.ifType = "zoo.cfg";
    replace3.ifKeyState = PropertyKeyState.ABSENT;
    replacements.add(replace3);

    Replace replace4 = new Replace();
    replacements.add(replace3);
    replace4.key = "replace.key.5";
    replace4.find = "r";
    replace4.replaceWith = "R";
    replace4.ifKey = "no.such.key";
    replace4.ifType = "zoo.cfg";
    replace4.ifKeyState = PropertyKeyState.PRESENT;
    replacements.add(replace4);

    createUpgrade(c, repoVersion2200);

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_REPLACEMENTS, new Gson().toJson(replacements));

    ExecutionCommand executionCommand = getExecutionCommand(commandParams);
    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertEquals("r1", config.getProperties().get("replace.key.1"));
    assertEquals("r2", config.getProperties().get("replace.key.2"));
    assertEquals("r3a1", config.getProperties().get("replace.key.3"));
    assertEquals("r4", config.getProperties().get("replace.key.4"));
    assertEquals("r5", config.getProperties().get("replace.key.5"));
  }

  @Test
  public void testAllowedTransferCopy() throws Exception {
    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    Map<String, String> properties = new HashMap<String, String>() {
      {
        put("initLimit", "10");
        put("copy.key.1", "c1");
        put("copy.key.2", "c2");
      }
    };

    Config config = createConfig(c, repoVersion2110, "zoo.cfg", "version2", properties);

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    List<ConfigurationKeyValue> configurations = new ArrayList<>();
    ConfigurationKeyValue keyValue = new ConfigurationKeyValue();
    configurations.add(keyValue);
    keyValue.key = "initLimit";
    keyValue.value = "11";

    createUpgrade(c, repoVersion2200);

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS, new Gson().toJson(configurations));

    // normal copy
    List<Transfer> transfers = new ArrayList<>();
    Transfer transfer1 = new Transfer();
    transfer1.operation = TransferOperation.COPY;
    transfer1.fromKey = "copy.key.1";
    transfer1.toKey = "copy.to.key.1";
    transfers.add(transfer1);

    // copy with default
    Transfer transfer2 = new Transfer();
    transfer2.operation = TransferOperation.COPY;
    transfer2.fromKey = "copy.key.no.need.to.exit.1";
    transfer2.toKey = "copy.to.key.with.default.1";
    transfer2.defaultValue = "defaultValue";
    transfers.add(transfer2);

    Transfer transfer3 = new Transfer();
    transfer3.operation = TransferOperation.COPY;
    transfer3.fromKey = "copy.key.2";
    transfer3.toKey = "copy.to.key.2";
    transfer3.ifKey = "initLimit";
    transfer3.ifType = "zoo.cfg";
    transfer3.ifValue = "10";
    transfers.add(transfer3);

    Transfer transfer4 = new Transfer();
    transfer4.operation = TransferOperation.COPY;
    transfer4.fromKey = "copy.key.2";
    transfer4.toKey = "copy.to.key.3";
    transfer4.ifKey = "initLimit";
    transfer4.ifType = "zoo.cfg";
    transfer4.ifKeyState = PropertyKeyState.PRESENT;
    transfers.add(transfer4);

    Transfer transfer5 = new Transfer();
    transfer5.operation = TransferOperation.COPY;
    transfer5.fromKey = "copy.key.no.need.to.exist.2";
    transfer5.toKey = "copy.to.key.with.default.2";
    transfer5.defaultValue = "defaultValue2";
    transfer5.ifKey = "no.such.key";
    transfer5.ifType = "zoo.cfg";
    transfer5.ifKeyState = PropertyKeyState.ABSENT;
    transfers.add(transfer5);

    commandParams.put(ConfigureTask.PARAMETER_TRANSFERS, new Gson().toJson(transfers));

    ExecutionCommand executionCommand = getExecutionCommand(commandParams);
    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null,
        null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(
        executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(3, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertFalse("version2".equals(config.getTag()));

    Map<String, String> map = config.getProperties();
    assertEquals(8, map.size());
    assertEquals("11", map.get("initLimit"));
    assertEquals(map.get("copy.key.1"), map.get("copy.to.key.1"));
    assertTrue(!map.containsKey("copy.key.no.need.to.exit.1"));
    assertEquals("defaultValue", map.get("copy.to.key.with.default.1"));
    assertTrue(!map.containsKey("copy.key.no.need.to.exit.2"));
    assertEquals("defaultValue2", map.get("copy.to.key.with.default.2"));
    assertEquals(map.get("copy.key.2"), map.get("copy.to.key.2"));
    assertEquals(map.get("copy.key.2"), map.get("copy.to.key.3"));
  }

  @Test
  public void testDisallowedTransferCopy() throws Exception {
    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    Map<String, String> properties = new HashMap<String, String>() {
      {
        put("initLimit", "10");
        put("copy.key.1", "c1");
        put("copy.key.2", "c2");
      }
    };

    Config config = createConfig(c, repoVersion2110, "zoo.cfg", "version2", properties);

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    List<ConfigurationKeyValue> configurations = new ArrayList<>();
    ConfigurationKeyValue keyValue = new ConfigurationKeyValue();
    configurations.add(keyValue);
    keyValue.key = "initLimit";
    keyValue.value = "11";

    createUpgrade(c, repoVersion2111);

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS, new Gson().toJson(configurations));

    List<Transfer> transfers = new ArrayList<>();
    Transfer transfer = new Transfer();
    transfer.operation = TransferOperation.COPY;
    transfer.fromKey = "copy.key.2";
    transfer.toKey = "copy.to.key.2";
    transfer.ifKey = "initLimit";
    transfer.ifType = "zoo.cfg";
    transfer.ifValue = "not-the-real-value";
    transfers.add(transfer);

    transfer = new Transfer();
    transfer.operation = TransferOperation.COPY;
    transfer.fromKey = "copy.key.2";
    transfer.toKey = "copy.to.key.3";
    transfer.ifKey = "initLimit";
    transfer.ifType = "zoo.cfg";
    transfer.ifKeyState = PropertyKeyState.ABSENT;
    transfers.add(transfer);

    transfer = new Transfer();
    transfer.operation = TransferOperation.COPY;
    transfer.fromKey = "copy.key.no.need.to.exist.2";
    transfer.toKey = "copy.to.key.with.default.2";
    transfer.defaultValue = "defaultValue2";
    transfer.ifKey = "no.such.key";
    transfer.ifType = "zoo.cfg";
    transfer.ifKeyState = PropertyKeyState.PRESENT;
    transfers.add(transfer);

    commandParams.put(ConfigureTask.PARAMETER_TRANSFERS, new Gson().toJson(transfers));

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");
    executionCommand.setRoleParams(new HashMap<>());
    executionCommand.getRoleParams().put(ServerAction.ACTION_USER_NAME, "username");

    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null,
        null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(
        executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(3, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertFalse("version2".equals(config.getTag()));

    Map<String, String> map = config.getProperties();
    assertEquals(3, map.size());
    assertEquals("11", map.get("initLimit"));
    assertEquals("c1", map.get("copy.key.1"));
    assertEquals("c2", map.get("copy.key.2"));
  }

  @Test
  public void testAllowedTransferMove() throws Exception {
    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    Map<String, String> properties = new HashMap<String, String>() {
      {
        put("initLimit", "10");
        put("move.key.1", "m1");
        put("move.key.2", "m2");
        put("move.key.3", "m3");
        put("move.key.4", "m4");
      }
    };

    Config config = createConfig(c, repoVersion2110, "zoo.cfg", "version2", properties);

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    List<ConfigurationKeyValue> configurations = new ArrayList<>();
    ConfigurationKeyValue keyValue = new ConfigurationKeyValue();
    configurations.add(keyValue);
    keyValue.key = "initLimit";
    keyValue.value = "11";

    createUpgrade(c, repoVersion2111);

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS, new Gson().toJson(configurations));

    List<Transfer> transfers = new ArrayList<>();
    Transfer transfer1 = new Transfer();
    transfer1.operation = TransferOperation.MOVE;
    transfer1.fromKey = "move.key.1";
    transfer1.toKey = "move.to.key.1";
    transfers.add(transfer1);

    Transfer transfer2 = new Transfer();
    transfer2.operation = TransferOperation.MOVE;
    transfer2.fromKey = "move.key.2";
    transfer2.toKey = "move.to.key.2";
    transfer2.ifKey = "initLimit";
    transfer2.ifType = "zoo.cfg";
    transfer2.ifValue = "10";
    transfers.add(transfer2);

    Transfer transfer3 = new Transfer();
    transfer3.operation = TransferOperation.MOVE;
    transfer3.fromKey = "move.key.3";
    transfer3.toKey = "move.to.key.3";
    transfer3.ifKey = "initLimit";
    transfer3.ifType = "zoo.cfg";
    transfer3.ifKeyState = PropertyKeyState.PRESENT;
    transfers.add(transfer3);

    Transfer transfer4 = new Transfer();
    transfer4.operation = TransferOperation.MOVE;
    transfer4.fromKey = "move.key.4";
    transfer4.toKey = "move.to.key.4";
    transfer4.ifKey = "no.such.key";
    transfer4.ifType = "zoo.cfg";
    transfer4.ifKeyState = PropertyKeyState.ABSENT;
    transfers.add(transfer4);

    commandParams.put(ConfigureTask.PARAMETER_TRANSFERS, new Gson().toJson(transfers));

    ExecutionCommand executionCommand = getExecutionCommand(commandParams);
    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null,
        null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(
        executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(3, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertFalse("version2".equals(config.getTag()));

    Map<String, String> map = config.getProperties();
    assertEquals(5, map.size());
    String[] shouldNotExitKeys = new String[]{"move.key.1", "move.key.2", "move.key.3", "move.key.4"};
    String[] shouldExitKeys = new String[]{"move.to.key.1", "move.to.key.2", "move.to.key.3", "move.to.key.4"};
    for(String key: shouldNotExitKeys){
      assertFalse(map.containsKey(key));
    }

    for(String key: shouldExitKeys){
      assertTrue(map.containsKey(key));
    }
  }

  @Test
  public void testDisallowedTransferMove() throws Exception {
    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    Map<String, String> properties = new HashMap<String, String>() {
      {
        put("initLimit", "10");
        put("move.key.1", "m1");
        put("move.key.2", "m2");
        put("move.key.3", "m3");
        put("move.key.4", "m4");
      }
    };

    Config config = createConfig(c, repoVersion2110, "zoo.cfg", "version2", properties);

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    List<ConfigurationKeyValue> configurations = new ArrayList<>();
    ConfigurationKeyValue keyValue = new ConfigurationKeyValue();
    configurations.add(keyValue);
    keyValue.key = "initLimit";
    keyValue.value = "11";

    createUpgrade(c, repoVersion2111);

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS, new Gson().toJson(configurations));

    List<Transfer> transfers = new ArrayList<>();
    Transfer transfer2 = new Transfer();
    transfer2.operation = TransferOperation.MOVE;
    transfer2.fromKey = "move.key.2";
    transfer2.toKey = "move.to.key.2";
    transfer2.ifKey = "initLimit";
    transfer2.ifType = "zoo.cfg";
    transfer2.ifValue = "not-real-value";
    transfers.add(transfer2);

    Transfer transfer3 = new Transfer();
    transfer3.operation = TransferOperation.MOVE;
    transfer3.fromKey = "move.key.3";
    transfer3.toKey = "move.to.key.3";
    transfer3.ifKey = "initLimit";
    transfer3.ifType = "zoo.cfg";
    transfer3.ifKeyState = PropertyKeyState.ABSENT;
    transfers.add(transfer3);

    Transfer transfer4 = new Transfer();
    transfer4.operation = TransferOperation.MOVE;
    transfer4.fromKey = "move.key.4";
    transfer4.toKey = "move.to.key.4";
    transfer4.ifKey = "no.such.key";
    transfer4.ifType = "zoo.cfg";
    transfer4.ifKeyState = PropertyKeyState.PRESENT;
    transfers.add(transfer3);

    commandParams.put(ConfigureTask.PARAMETER_TRANSFERS, new Gson().toJson(transfers));

    ExecutionCommand executionCommand = getExecutionCommand(commandParams);
    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null,
        null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(
        executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(3, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertFalse("version2".equals(config.getTag()));

    Map<String, String> map = config.getProperties();
    assertEquals(5, map.size());

    String[] shouldExitKeys = new String[]{"move.key.1", "move.key.2", "move.key.3", "move.key.4"};
    String[] shouldNotExitKeys = new String[]{"move.to.key.1", "move.to.key.2", "move.to.key.3", "move.to.key.4"};
    for(String key: shouldNotExitKeys){
      assertFalse(map.containsKey(key));
    }

    for(String key: shouldExitKeys){
      assertTrue(map.containsKey(key));
    }
  }

  @Test
  public void testAllowedTransferDelete() throws Exception {
    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    Map<String, String> properties = new HashMap<String, String>() {
      {
        put("initLimit", "10");
        put("delete.key.1", "d1");
        put("delete.key.2", "d2");
        put("delete.key.3", "d3");
        put("delete.key.4", "d4");
      }
    };

    Config config = createConfig(c, repoVersion2110, "zoo.cfg", "version2", properties);

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    List<ConfigurationKeyValue> configurations = new ArrayList<>();
    ConfigurationKeyValue keyValue = new ConfigurationKeyValue();
    configurations.add(keyValue);
    keyValue.key = "initLimit";
    keyValue.value = "11";

    createUpgrade(c, repoVersion2111);

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS, new Gson().toJson(configurations));

    List<Transfer> transfers = new ArrayList<>();
    Transfer transfer1 = new Transfer();
    transfer1.operation = TransferOperation.DELETE;
    transfer1.deleteKey = "delete.key.1";
    transfers.add(transfer1);

    Transfer transfer2 = new Transfer();
    transfer2.operation = TransferOperation.DELETE;
    transfer2.deleteKey = "delete.key.2";
    transfer2.ifKey = "initLimit";
    transfer2.ifType = "zoo.cfg";
    transfer2.ifValue = "10";
    transfers.add(transfer2);

    Transfer transfer3 = new Transfer();
    transfer3.operation = TransferOperation.DELETE;
    transfer3.deleteKey = "delete.key.3";
    transfer3.ifKey = "initLimit";
    transfer3.ifType = "zoo.cfg";
    transfer3.ifKeyState = PropertyKeyState.PRESENT;
    transfers.add(transfer3);

    Transfer transfer4 = new Transfer();
    transfer4.operation = TransferOperation.DELETE;
    transfer4.deleteKey = "delete.key.4";
    transfer4.ifKey = "no.such.key";
    transfer4.ifType = "zoo.cfg";
    transfer4.ifKeyState = PropertyKeyState.ABSENT;
    transfers.add(transfer4);

    commandParams.put(ConfigureTask.PARAMETER_TRANSFERS, new Gson().toJson(transfers));

    ExecutionCommand executionCommand = getExecutionCommand(commandParams);
    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null,
        null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(
        executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(3, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertFalse("version2".equals(config.getTag()));

    Map<String, String> map = config.getProperties();
    assertEquals(1, map.size());
    assertEquals("11", map.get("initLimit"));
    String[] shouldNotExitKeys = new String[]{"delete.key.1","delete.key.2","delete.key.3","delete.key.4"};
    for(String key: shouldNotExitKeys){
      assertFalse(map.containsKey(key));
    }
  }

  @Test
  public void testDisallowedTransferDelete() throws Exception {
    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    Map<String, String> properties = new HashMap<String, String>() {
      {
        put("initLimit", "10");
        put("delete.key.1", "d1");
        put("delete.key.2", "d2");
        put("delete.key.3", "d3");
        put("delete.key.4", "d4");
      }
    };

    Config config = createConfig(c, repoVersion2110, "zoo.cfg", "version2", properties);

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    List<ConfigurationKeyValue> configurations = new ArrayList<>();
    ConfigurationKeyValue keyValue = new ConfigurationKeyValue();
    configurations.add(keyValue);
    keyValue.key = "initLimit";
    keyValue.value = "11";

    createUpgrade(c, repoVersion2111);

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");
    commandParams.put(ConfigureTask.PARAMETER_KEY_VALUE_PAIRS, new Gson().toJson(configurations));

    List<Transfer> transfers = new ArrayList<>();
    Transfer transfer2 = new Transfer();
    transfer2.operation = TransferOperation.DELETE;
    transfer2.deleteKey = "delete.key.2";
    transfer2.ifKey = "initLimit";
    transfer2.ifType = "zoo.cfg";
    transfer2.ifValue = "not.real.value";
    transfers.add(transfer2);

    Transfer transfer3 = new Transfer();
    transfer3.operation = TransferOperation.DELETE;
    transfer3.deleteKey = "delete.key.3";
    transfer3.ifKey = "initLimit";
    transfer3.ifType = "zoo.cfg";
    transfer3.ifKeyState = PropertyKeyState.ABSENT;
    transfers.add(transfer3);

    Transfer transfer4 = new Transfer();
    transfer4.operation = TransferOperation.DELETE;
    transfer4.deleteKey = "delete.key.4";
    transfer4.ifKey = "no.such.key";
    transfer4.ifType = "zoo.cfg";
    transfer4.ifKeyState = PropertyKeyState.PRESENT;
    transfers.add(transfer4);

    commandParams.put(ConfigureTask.PARAMETER_TRANSFERS, new Gson().toJson(transfers));

    ExecutionCommand executionCommand = getExecutionCommand(commandParams);
    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null,
        null, null);

    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(
        executionCommand));


    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(3, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertFalse("version2".equals(config.getTag()));

    Map<String, String> map = config.getProperties();
    assertEquals(5, map.size());
    assertEquals("11", map.get("initLimit"));
    String[] shouldExitKeys = new String[]{"delete.key.1","delete.key.2","delete.key.3","delete.key.4"};
    for(String key: shouldExitKeys){
      assertTrue(map.containsKey(key));
    }
  }

  /**
   * Tests using the {@code <insert/>} element in a configuration upgrade pack.
   *
   * @throws Exception
   */
  @Test
  public void testInsert() throws Exception {
    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    Map<String, String> properties = new HashMap<String, String>() {
      {
        put("key_to_append", "append");
        put("key_to_prepend", "prepend");
      }
    };

    Config config = createConfig(c, repoVersion2110, "zoo.cfg", "version2", properties);

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    createUpgrade(c, repoVersion2111);

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");

    // define the changes
    final String prependValue = "This should be on a newline";
    final String appendValue = " this will be after...";

    // insert tasks
    List<Insert> insertions = new ArrayList<>();

    Insert prepend = new Insert();
    prepend.insertType = InsertType.PREPEND;
    prepend.key = "key_to_prepend";
    prepend.value = prependValue;
    prepend.newlineBefore = false;
    prepend.newlineAfter = true;

    Insert append = new Insert();
    append.insertType = InsertType.APPEND;
    append.key = "key_to_append";
    append.value = appendValue;
    append.newlineBefore = false;
    append.newlineAfter = false;

    // add them to the list
    insertions.add(prepend);
    insertions.add(append);

    // just for fun, add them again - this will test their idempotence
    insertions.add(prepend);
    insertions.add(append);

    commandParams.put(ConfigureTask.PARAMETER_INSERTIONS, new Gson().toJson(insertions));

    ExecutionCommand executionCommand = getExecutionCommand(commandParams);
    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));
    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    assertEquals(3, c.getConfigsByType("zoo.cfg").size());

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);
    assertFalse("version2".equals(config.getTag()));

    // build the expected values
    String expectedPrepend = prependValue + System.lineSeparator() + "prepend";
    String expectedAppend = "append" + appendValue;

    assertEquals(expectedPrepend, config.getProperties().get("key_to_prepend"));
    assertEquals(expectedAppend, config.getProperties().get("key_to_append"));
  }

  /**
   * Tests using the {@code <insert/>} element in a configuration upgrade pack.
   *
   * @throws Exception
   */
  @Test
  public void testInsertWithCondition() throws Exception {
    Cluster c = clusters.getCluster("c1");
    assertEquals(1, c.getConfigsByType("zoo.cfg").size());

    final String lineSample = "This is before";
    Map<String, String> properties = new HashMap<String, String>() {
      {
        put("item_not_inserted", lineSample);
        put("item_inserted", lineSample);
        put("item_partial_inserted", lineSample);
        put("item_partial_not_inserted", lineSample);
        put("item_value_not_inserted", lineSample);
        put("item_partial_value_not_inserted", lineSample);
        put("item_value_not_not_inserted", lineSample);
        put("item_partial_value_not_not_inserted", lineSample);
      }
    };

    Config config = createConfig(c, repoVersion2110, "zoo.cfg", "version2", properties);

    c.addDesiredConfig("user", Collections.singleton(config));
    assertEquals(2, c.getConfigsByType("zoo.cfg").size());

    createUpgrade(c, repoVersion2111);

    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");
    commandParams.put(ConfigureTask.PARAMETER_CONFIG_TYPE, "zoo.cfg");


    final String appendValue = " this will be after...";

    // insert tasks
    List<Insert> insertions = new ArrayList<>();

    Insert in1 = new Insert();
    Insert in2 = new Insert();
    Insert in3 = new Insert();
    Insert in4 = new Insert();
    Insert in5 = new Insert();
    Insert in6 = new Insert();
    Insert in7 = new Insert();
    Insert in8 = new Insert();

    //expect: no changes
    in1.insertType = InsertType.APPEND;
    in1.key = "item_not_inserted";
    in1.value = appendValue;
    in1.newlineBefore = false;
    in1.newlineAfter = false;
    in1.ifType = "zoo.cfg";
    in1.ifKey = "item_not_inserted";
    in1.ifValue = "multiline";
    in1.ifValueMatchType = IfValueMatchType.EXACT;

    //expect: value appended to the property
    in2.insertType = InsertType.APPEND;
    in2.key = "item_inserted";
    in2.value = appendValue;
    in2.newlineBefore = false;
    in2.newlineAfter = false;
    in2.ifType = "zoo.cfg";
    in2.ifKey = "item_inserted";
    in2.ifValue = lineSample;
    in2.ifValueMatchType = IfValueMatchType.EXACT;

    //expect: value appended to the property
    in3.insertType = InsertType.APPEND;
    in3.key = "item_partial_inserted";
    in3.value = appendValue;
    in3.newlineBefore = false;
    in3.newlineAfter = false;
    in3.ifType = "zoo.cfg";
    in3.ifKey = "item_partial_inserted";
    in3.ifValue = "before";
    in3.ifValueMatchType = IfValueMatchType.PARTIAL;

    //expect: no changes
    in4.insertType = InsertType.APPEND;
    in4.key = "item_partial_not_inserted";
    in4.value = appendValue;
    in4.newlineBefore = false;
    in4.newlineAfter = false;
    in4.ifType = "zoo.cfg";
    in4.ifKey = "item_partial_not_inserted";
    in4.ifValue = "wrong word";
    in4.ifValueMatchType = IfValueMatchType.PARTIAL;

    //expect: value appended to the property
    in5.insertType = InsertType.APPEND;
    in5.key = "item_value_not_inserted";
    in5.value = appendValue;
    in5.newlineBefore = false;
    in5.newlineAfter = false;
    in5.ifType = "zoo.cfg";
    in5.ifKey = "item_value_not_inserted";
    in5.ifValue = "wrong word";
    in5.ifValueMatchType = IfValueMatchType.EXACT;
    in5.ifValueNotMatched = true;

    //expect: value appended to the property
    in6.insertType = InsertType.APPEND;
    in6.key = "item_partial_value_not_inserted";
    in6.value = appendValue;
    in6.newlineBefore = false;
    in6.newlineAfter = false;
    in6.ifType = "zoo.cfg";
    in6.ifKey = "item_partial_value_not_inserted";
    in6.ifValue = "wrong word";
    in6.ifValueMatchType = IfValueMatchType.PARTIAL;
    in6.ifValueNotMatched = true;

    //expect: no changes
    in7.insertType = InsertType.APPEND;
    in7.key = "item_value_not_not_inserted";
    in7.value = appendValue;
    in7.newlineBefore = false;
    in7.newlineAfter = false;
    in7.ifType = "zoo.cfg";
    in7.ifKey = "item_value_not_not_inserted";
    in7.ifValue = lineSample;
    in7.ifValueMatchType = IfValueMatchType.EXACT;
    in7.ifValueNotMatched = true;

    //expect: no changes
    in8.insertType = InsertType.APPEND;
    in8.key = "item_partial_value_not_not_inserted";
    in8.value = appendValue;
    in8.newlineBefore = false;
    in8.newlineAfter = false;
    in8.ifType = "zoo.cfg";
    in8.ifKey = "item_partial_value_not_not_inserted";
    in8.ifValue = "before";
    in8.ifValueMatchType = IfValueMatchType.PARTIAL;
    in8.ifValueNotMatched = true;

    insertions.add(in1);
    insertions.add(in2);
    insertions.add(in3);
    insertions.add(in4);
    insertions.add(in5);
    insertions.add(in6);
    insertions.add(in7);
    insertions.add(in8);

    commandParams.put(ConfigureTask.PARAMETER_INSERTIONS, new Gson().toJson(insertions));

    ExecutionCommand executionCommand = getExecutionCommand(commandParams);
    HostRoleCommand hostRoleCommand = hostRoleCommandFactory.create(null, null, null, null);
    hostRoleCommand.setExecutionCommandWrapper(new ExecutionCommandWrapper(executionCommand));
    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hostRoleCommand);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    config = c.getDesiredConfigByType("zoo.cfg");
    assertNotNull(config);

    // build the expected values
    String expectedAppend = lineSample + appendValue;

    assertEquals(expectedAppend, config.getProperties().get("item_inserted"));
    assertEquals(expectedAppend, config.getProperties().get("item_partial_inserted"));
    assertEquals(expectedAppend, config.getProperties().get("item_value_not_inserted"));
    assertEquals(expectedAppend, config.getProperties().get("item_partial_value_not_inserted"));

    assertTrue(lineSample.equalsIgnoreCase(config.getProperties().get("item_not_inserted")));
    assertTrue(lineSample.equalsIgnoreCase(config.getProperties().get("item_partial_not_inserted")));
    assertTrue(lineSample.equalsIgnoreCase(config.getProperties().get("item_value_not_not_inserted")));
    assertTrue(lineSample.equalsIgnoreCase(config.getProperties().get("item_partial_value_not_not_inserted")));
  }

  /**
   * Creates a cluster using {@link #repoVersion2110} with ZooKeeper installed.
   *
   * @throws Exception
   */
  private void makeUpgradeCluster() throws Exception {
    String clusterName = "c1";
    String hostName = "h1";
    Long hostId = 1L;

    clusters.addCluster(clusterName, repoVersion2110.getStackId());

    Cluster c = clusters.getCluster(clusterName);

    // add a host component
    clusters.addHost(hostName);
    Host host = clusters.getHost(hostName);
    Map<String, String> hostAttributes = new HashMap<>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6");
    host.setHostAttributes(hostAttributes);

    clusters.mapHostToCluster(hostName, clusterName);
    clusters.updateHostMappings(host);

    // !!! very important, otherwise the loops that walk the list of installed
    // service properties will not run!
    Service zk = installService(c, "ZOOKEEPER", repoVersion2110);
    addServiceComponent(c, zk, "ZOOKEEPER_SERVER");
    addServiceComponent(c, zk, "ZOOKEEPER_CLIENT");
    createNewServiceComponentHost(c, "ZOOKEEPER", "ZOOKEEPER_SERVER", hostName);
    createNewServiceComponentHost(c, "ZOOKEEPER", "ZOOKEEPER_CLIENT", hostName);

    Map<String, String> properties = new HashMap<String, String>() {
      {
        put("initLimit", "10");
      }
    };

    Config config = createConfig(c, repoVersion2110, "zoo.cfg", "version1", properties);

    c.addDesiredConfig("user", Collections.singleton(config));

    // verify that our configs are there
    String tickTime = m_configHelper.getPropertyValueFromStackDefinitions(c, "zoo.cfg", "tickTime");
    assertNotNull(tickTime);
  }

  /**
   * Installs a service in the cluster.
   *
   * @param cluster
   * @param serviceName
   * @return
   * @throws AmbariException
   */
  private Service installService(Cluster cluster, String serviceName,
      RepositoryVersionEntity repositoryVersion) throws AmbariException {
    Service service = null;

    try {
      service = cluster.getService(serviceName);
    } catch (ServiceNotFoundException e) {
      service = serviceFactory.createNew(cluster, serviceName, repositoryVersion);
      cluster.addService(service);
    }

    return service;
  }

  private ServiceComponent addServiceComponent(Cluster cluster, Service service,
      String componentName) throws AmbariException {
    ServiceComponent serviceComponent = null;
    try {
      serviceComponent = service.getServiceComponent(componentName);
    } catch (ServiceComponentNotFoundException e) {
      serviceComponent = serviceComponentFactory.createNew(service, componentName);
      service.addServiceComponent(serviceComponent);
      serviceComponent.setDesiredState(State.INSTALLED);
    }

    return serviceComponent;
  }

  private ServiceComponentHost createNewServiceComponentHost(Cluster cluster, String serviceName,
      String svcComponent, String hostName) throws AmbariException {
    Assert.assertNotNull(cluster.getConfigGroups());
    Service s = cluster.getService(serviceName);
    ServiceComponent sc = addServiceComponent(cluster, s, svcComponent);

    ServiceComponentHost sch = serviceComponentHostFactory.createNew(sc, hostName);

    sc.addServiceComponentHost(sch);
    sch.setDesiredState(State.INSTALLED);
    sch.setState(State.INSTALLED);
    return sch;
  }

  /**
   * Creates an upgrade and associates it with the cluster.
   */
  private UpgradeEntity createUpgrade(Cluster cluster, RepositoryVersionEntity repositoryVersion)
      throws Exception {

    // create some entities for the finalize action to work with for patch
    // history
    RequestEntity requestEntity = new RequestEntity();
    requestEntity.setClusterId(cluster.getClusterId());
    requestEntity.setRequestId(1L);
    requestEntity.setStartTime(System.currentTimeMillis());
    requestEntity.setCreateTime(System.currentTimeMillis());
    requestDAO.create(requestEntity);

    UpgradeEntity upgradeEntity = new UpgradeEntity();
    upgradeEntity.setId(1L);
    upgradeEntity.setClusterId(cluster.getClusterId());
    upgradeEntity.setRequestEntity(requestEntity);
    upgradeEntity.setUpgradePackage("");
    upgradeEntity.setUpgradePackStackId(new StackId((String) null));
    upgradeEntity.setRepositoryVersion(repositoryVersion);
    upgradeEntity.setUpgradeType(UpgradeType.NON_ROLLING);

    Map<String, Service> services = cluster.getServices();
    for (String serviceName : services.keySet()) {
      Service service = services.get(serviceName);
      Map<String, ServiceComponent> components = service.getServiceComponents();
      for (String componentName : components.keySet()) {
        UpgradeHistoryEntity history = new UpgradeHistoryEntity();
        history.setUpgrade(upgradeEntity);
        history.setServiceName(serviceName);
        history.setComponentName(componentName);
        history.setFromRepositoryVersion(service.getDesiredRepositoryVersion());
        history.setTargetRepositoryVersion(repositoryVersion);
        upgradeEntity.addHistory(history);
      }
    }

    upgradeDAO.create(upgradeEntity);
    cluster.setUpgradeEntity(upgradeEntity);
    return upgradeEntity;
  }

  private ExecutionCommand getExecutionCommand(Map<String, String> commandParams) {
    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setClusterName("c1");
    executionCommand.setCommandParams(commandParams);
    executionCommand.setRoleParams(new HashMap<>());
    executionCommand.getRoleParams().put(ServerAction.ACTION_USER_NAME, "username");

    return executionCommand;
  }

  private Config createConfig(Cluster cluster, RepositoryVersionEntity repoVersion, String type,
      String tag, Map<String, String> properties) {
    return configFactory.createNew(repoVersion.getStackId(), cluster, type, tag, properties,
        NO_ATTRIBUTES);
  }
}
