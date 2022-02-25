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

package org.apache.ambari.server.actionmanager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.agent.AgentCommand.AgentCommandType;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.agent.ExecutionCommand.KeyNames;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.codehaus.jettison.json.JSONException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class ExecutionCommandWrapperTest {

  private static final String HOST1 = "dev01.ambari.apache.org";
  private static final String CLUSTER1 = "c1";
  private static final String CLUSTER_VERSION_TAG = "clusterVersion";
  private static final String SERVICE_VERSION_TAG = "serviceVersion";
  private static final String HOST_VERSION_TAG = "hostVersion";
  private static final String GLOBAL_CONFIG = "global";
  private static final String SERVICE_SITE_CONFIG = "service-site";
  private static final String SERVICE_SITE_NAME1 = "ssn1";
  private static final String SERVICE_SITE_NAME2 = "ssn2";
  private static final String SERVICE_SITE_NAME3 = "ssn3";
  private static final String SERVICE_SITE_NAME4 = "ssn4";
  private static final String SERVICE_SITE_NAME5 = "ssn5";
  private static final String SERVICE_SITE_NAME6 = "ssn6";
  private static final String SERVICE_SITE_VAL1 = "ssv1";
  private static final String SERVICE_SITE_VAL1_S = "ssv1_s";
  private static final String SERVICE_SITE_VAL2 = "ssv2";
  private static final String SERVICE_SITE_VAL2_H = "ssv2_h";
  private static final String SERVICE_SITE_VAL3 = "ssv3";
  private static final String SERVICE_SITE_VAL4 = "ssv4";
  private static final String SERVICE_SITE_VAL5 = "ssv5";
  private static final String SERVICE_SITE_VAL5_S = "ssv5_s";
  private static final String SERVICE_SITE_VAL6_H = "ssv6_h";
  private static final String GLOBAL_NAME1 = "gn1";
  private static final String GLOBAL_NAME2 = "gn2";
  private static final String GLOBAL_CLUSTER_VAL1 = "gcv1";
  private static final String GLOBAL_CLUSTER_VAL2 = "gcv2";
  private static final String GLOBAL_VAL1 = "gv1";

  private static Map<String, String> GLOBAL_CLUSTER;
  private static Map<String, String> SERVICE_SITE_CLUSTER;
  private static Map<String, String> SERVICE_SITE_SERVICE;
  private static Map<String, String> SERVICE_SITE_HOST;
  private static Map<String, Map<String, String>> CONFIG_ATTRIBUTES;

  private static Injector injector;
  private static Clusters clusters;
  private static ConfigFactory configFactory;
  private static ConfigHelper configHelper;
  private static StageFactory stageFactory;
  private static OrmTestHelper ormTestHelper;

  @BeforeClass
  public static void setup() throws AmbariException {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    configHelper = injector.getInstance(ConfigHelper.class);
    configFactory = injector.getInstance(ConfigFactory.class);
    stageFactory = injector.getInstance(StageFactory.class);
    ormTestHelper = injector.getInstance(OrmTestHelper.class);

    clusters = injector.getInstance(Clusters.class);
    clusters.addHost(HOST1);
    clusters.addCluster(CLUSTER1, new StackId("HDP-0.1"));
    clusters.mapHostToCluster(HOST1, CLUSTER1);

    Map<String, String> hostAttributes = new HashMap<>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6.4");
    Host host = clusters.getHost(HOST1);
    host.setHostAttributes(hostAttributes);

    Cluster cluster1 = clusters.getCluster(CLUSTER1);

    OrmTestHelper helper = injector.getInstance(OrmTestHelper.class);
    RepositoryVersionEntity repositoryVersion = helper.getOrCreateRepositoryVersion(cluster1);
    cluster1.addService("HDFS", repositoryVersion);

    SERVICE_SITE_CLUSTER = new HashMap<>();
    SERVICE_SITE_CLUSTER.put(SERVICE_SITE_NAME1, SERVICE_SITE_VAL1);
    SERVICE_SITE_CLUSTER.put(SERVICE_SITE_NAME2, SERVICE_SITE_VAL2);
    SERVICE_SITE_CLUSTER.put(SERVICE_SITE_NAME3, SERVICE_SITE_VAL3);
    SERVICE_SITE_CLUSTER.put(SERVICE_SITE_NAME4, SERVICE_SITE_VAL4);

    SERVICE_SITE_SERVICE = new HashMap<>();
    SERVICE_SITE_SERVICE.put(SERVICE_SITE_NAME1, SERVICE_SITE_VAL1_S);
    SERVICE_SITE_SERVICE.put(SERVICE_SITE_NAME5, SERVICE_SITE_VAL5_S);

    SERVICE_SITE_HOST = new HashMap<>();
    SERVICE_SITE_HOST.put(SERVICE_SITE_NAME2, SERVICE_SITE_VAL2_H);
    SERVICE_SITE_HOST.put(SERVICE_SITE_NAME6, SERVICE_SITE_VAL6_H);

    GLOBAL_CLUSTER = new HashMap<>();
    GLOBAL_CLUSTER.put(GLOBAL_NAME1, GLOBAL_CLUSTER_VAL1);
    GLOBAL_CLUSTER.put(GLOBAL_NAME2, GLOBAL_CLUSTER_VAL2);

    CONFIG_ATTRIBUTES = new HashMap<>();

    //Cluster level global config
    configFactory.createNew(cluster1, GLOBAL_CONFIG, CLUSTER_VERSION_TAG, GLOBAL_CLUSTER, CONFIG_ATTRIBUTES);

    //Cluster level service config
    configFactory.createNew(cluster1, SERVICE_SITE_CONFIG, CLUSTER_VERSION_TAG, SERVICE_SITE_CLUSTER, CONFIG_ATTRIBUTES);

    //Service level service config
    configFactory.createNew(cluster1, SERVICE_SITE_CONFIG, SERVICE_VERSION_TAG, SERVICE_SITE_SERVICE, CONFIG_ATTRIBUTES);

    //Host level service config
    configFactory.createNew(cluster1, SERVICE_SITE_CONFIG, HOST_VERSION_TAG, SERVICE_SITE_HOST, CONFIG_ATTRIBUTES);

    ActionDBAccessor db = injector.getInstance(ActionDBAccessorImpl.class);

    createTask(db, 1, 1, HOST1, CLUSTER1);

  }

  private static void createTask(ActionDBAccessor db, long requestId, long stageId, String hostName, String clusterName) throws AmbariException {

    Stage s = stageFactory.createNew(requestId, "/var/log", clusterName, 1L, "execution command wrapper test", "commandParamsStage", "hostParamsStage");
    s.setStageId(stageId);
    s.addHostRoleExecutionCommand(hostName, Role.NAMENODE,
        RoleCommand.START,
        new ServiceComponentHostStartEvent(Role.NAMENODE.toString(),
            hostName, System.currentTimeMillis()), clusterName, "HDFS", false, false);
    List<Stage> stages = new ArrayList<>();
    stages.add(s);
    Request request = new Request(stages, "clusterHostInfo", clusters);
    db.persistActions(request);
  }

  @Test
  public void testGetExecutionCommand() throws JSONException, AmbariException {

    ExecutionCommand executionCommand = new ExecutionCommand();

    executionCommand.setClusterName(CLUSTER1);
    executionCommand.setTaskId(1);
    executionCommand.setRequestAndStage(1, 1);
    executionCommand.setHostname(HOST1);
    executionCommand.setRole("NAMENODE");
    executionCommand.setRoleParams(Collections.emptyMap());
    executionCommand.setRoleCommand(RoleCommand.START);
    executionCommand.setServiceName("HDFS");
    executionCommand.setCommandType(AgentCommandType.EXECUTION_COMMAND);
    executionCommand.setCommandParams(Collections.emptyMap());

    String json = StageUtils.getGson().toJson(executionCommand, ExecutionCommand.class);

    ExecutionCommandWrapper execCommWrap = new ExecutionCommandWrapper(json);
    injector.injectMembers(execCommWrap);

    ExecutionCommand processedExecutionCommand = execCommWrap.getExecutionCommand();

    Assert.assertNotNull(processedExecutionCommand.getRepositoryFile());
  }

  @Test
  public void testGetMergedConfig() {
    Map<String, String> baseConfig = new HashMap<>();

    baseConfig.put(SERVICE_SITE_NAME1, SERVICE_SITE_VAL1);
    baseConfig.put(SERVICE_SITE_NAME2, SERVICE_SITE_VAL2);
    baseConfig.put(SERVICE_SITE_NAME3, SERVICE_SITE_VAL3);
    baseConfig.put(SERVICE_SITE_NAME4, SERVICE_SITE_VAL4);
    baseConfig.put(SERVICE_SITE_NAME5, SERVICE_SITE_VAL5);

    Map<String, String> overrideConfig = new HashMap<>();

    overrideConfig.put(SERVICE_SITE_NAME2, SERVICE_SITE_VAL2_H);
    overrideConfig.put(SERVICE_SITE_NAME6, SERVICE_SITE_VAL6_H);


    Map<String, String> mergedConfig = configHelper.getMergedConfig(baseConfig,
      overrideConfig);


    Set<String> configsKeys = new HashSet<>();
    configsKeys.addAll(baseConfig.keySet());
    configsKeys.addAll(overrideConfig.keySet());

    Assert.assertEquals(configsKeys.size(), mergedConfig.size());

    Assert.assertEquals(SERVICE_SITE_VAL1, mergedConfig.get(SERVICE_SITE_NAME1));
    Assert.assertEquals(SERVICE_SITE_VAL2_H, mergedConfig.get(SERVICE_SITE_NAME2));
    Assert.assertEquals(SERVICE_SITE_VAL3, mergedConfig.get(SERVICE_SITE_NAME3));
    Assert.assertEquals(SERVICE_SITE_VAL4, mergedConfig.get(SERVICE_SITE_NAME4));
    Assert.assertEquals(SERVICE_SITE_VAL5, mergedConfig.get(SERVICE_SITE_NAME5));
    Assert.assertEquals(SERVICE_SITE_VAL6_H, mergedConfig.get(SERVICE_SITE_NAME6));
  }

  /**
   * Test that the execution command wrapper properly sets the version
   * information when the cluster is in the INSTALLING state.
   *
   * @throws JSONException
   * @throws AmbariException
   */
  @Test
  public void testExecutionCommandHasVersionInfoWithoutCurrentClusterVersion()
      throws JSONException, AmbariException {
    Cluster cluster = clusters.getCluster(CLUSTER1);

    StackId stackId = cluster.getDesiredStackVersion();

    // set the repo version resolved state to verify that the version is not sent
    RepositoryVersionEntity repositoryVersion = ormTestHelper.getOrCreateRepositoryVersion(stackId, "0.1-0000");
    repositoryVersion.setResolved(false);
    ormTestHelper.repositoryVersionDAO.merge(repositoryVersion);

    Service service = cluster.getService("HDFS");
    service.setDesiredRepositoryVersion(repositoryVersion);

    // first try with an INSTALL command - this should not populate version info
    ExecutionCommand executionCommand = new ExecutionCommand();
    Map<String, String> commandParams = new HashMap<>();

    executionCommand.setClusterName(CLUSTER1);
    executionCommand.setTaskId(1);
    executionCommand.setRequestAndStage(1, 1);
    executionCommand.setHostname(HOST1);
    executionCommand.setRole("NAMENODE");
    executionCommand.setRoleParams(Collections.<String, String>emptyMap());
    executionCommand.setRoleCommand(RoleCommand.INSTALL);
    executionCommand.setServiceName("HDFS");
    executionCommand.setCommandType(AgentCommandType.EXECUTION_COMMAND);
    executionCommand.setCommandParams(commandParams);

    String json = StageUtils.getGson().toJson(executionCommand, ExecutionCommand.class);
    ExecutionCommandWrapper execCommWrap = new ExecutionCommandWrapper(json);
    injector.injectMembers(execCommWrap);

    ExecutionCommand processedExecutionCommand = execCommWrap.getExecutionCommand();
    commandParams = processedExecutionCommand.getCommandParams();
    Assert.assertFalse(commandParams.containsKey(KeyNames.VERSION));

    // now try with a START command, but still unresolved
    executionCommand = new ExecutionCommand();
    commandParams = new HashMap<>();

    executionCommand.setClusterName(CLUSTER1);
    executionCommand.setTaskId(1);
    executionCommand.setRequestAndStage(1, 1);
    executionCommand.setHostname(HOST1);
    executionCommand.setRole("NAMENODE");
    executionCommand.setRoleParams(Collections.<String, String> emptyMap());
    executionCommand.setRoleCommand(RoleCommand.START);
    executionCommand.setServiceName("HDFS");
    executionCommand.setCommandType(AgentCommandType.EXECUTION_COMMAND);
    executionCommand.setCommandParams(commandParams);

    json = StageUtils.getGson().toJson(executionCommand, ExecutionCommand.class);
    execCommWrap = new ExecutionCommandWrapper(json);
    injector.injectMembers(execCommWrap);

    processedExecutionCommand = execCommWrap.getExecutionCommand();
    commandParams = processedExecutionCommand.getCommandParams();
    Assert.assertFalse(commandParams.containsKey(KeyNames.VERSION));

    // now that the repositoryVersion is resolved, it should populate the version even
    // though the state is INSTALLING
    repositoryVersion.setResolved(true);
    ormTestHelper.repositoryVersionDAO.merge(repositoryVersion);
    execCommWrap = new ExecutionCommandWrapper(json);
    injector.injectMembers(execCommWrap);

    processedExecutionCommand = execCommWrap.getExecutionCommand();
    commandParams = processedExecutionCommand.getCommandParams();
    Assert.assertEquals("0.1-0000", commandParams.get(KeyNames.VERSION));
    }

  /**
   * Test that the execution command wrapper ignores repository file when there are none to use.
   */
  @Test
  public void testExecutionCommandNoRepositoryFile() throws Exception {
    Cluster cluster = clusters.getCluster(CLUSTER1);

    StackId stackId = cluster.getDesiredStackVersion();
    RepositoryVersionEntity repositoryVersion = ormTestHelper.getOrCreateRepositoryVersion(new StackId("HDP", "0.2"), "0.2-0000");
    repositoryVersion.setResolved(true); // has build number
    Service service = cluster.addService("HIVE", repositoryVersion);
    service.setDesiredRepositoryVersion(repositoryVersion);

    repositoryVersion.addRepoOsEntities(new ArrayList<>());

    ormTestHelper.repositoryVersionDAO.merge(repositoryVersion);

    // first try with an INSTALL command - this should not populate version info
    ExecutionCommand executionCommand = new ExecutionCommand();
    Map<String, String> commandParams = new HashMap<>();

    executionCommand.setClusterName(CLUSTER1);
    executionCommand.setTaskId(1);
    executionCommand.setRequestAndStage(1, 1);
    executionCommand.setHostname(HOST1);
    executionCommand.setRole("HIVE_SERVER");
    executionCommand.setRoleParams(Collections.<String, String>emptyMap());
    executionCommand.setRoleCommand(RoleCommand.INSTALL);
    executionCommand.setServiceName("HIVE");
    executionCommand.setCommandType(AgentCommandType.EXECUTION_COMMAND);
    executionCommand.setCommandParams(commandParams);

    String json = StageUtils.getGson().toJson(executionCommand, ExecutionCommand.class);
    ExecutionCommandWrapper execCommWrap = new ExecutionCommandWrapper(json);
    injector.injectMembers(execCommWrap);

    ExecutionCommand processedExecutionCommand = execCommWrap.getExecutionCommand();
    commandParams = processedExecutionCommand.getCommandParams();
    Assert.assertFalse(commandParams.containsKey(KeyNames.VERSION));

    // now try with a START command which should populate the version even
    // though the state is INSTALLING
    executionCommand = new ExecutionCommand();
    commandParams = new HashMap<>();

    executionCommand.setClusterName(CLUSTER1);
    executionCommand.setTaskId(1);
    executionCommand.setRequestAndStage(1, 1);
    executionCommand.setHostname(HOST1);
    executionCommand.setRole("HIVE_SERVER");
    executionCommand.setRoleParams(Collections.<String, String> emptyMap());
    executionCommand.setRoleCommand(RoleCommand.START);
    executionCommand.setServiceName("HIVE");
    executionCommand.setCommandType(AgentCommandType.EXECUTION_COMMAND);
    executionCommand.setCommandParams(commandParams);

    json = StageUtils.getGson().toJson(executionCommand, ExecutionCommand.class);
    execCommWrap = new ExecutionCommandWrapper(json);
    injector.injectMembers(execCommWrap);

    processedExecutionCommand = execCommWrap.getExecutionCommand();
    commandParams = processedExecutionCommand.getCommandParams();
    Assert.assertEquals("0.2-0000", commandParams.get(KeyNames.VERSION));
  }

  @AfterClass
  public static void tearDown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }
}
