/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.topology;

import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorBlueprintProcessor;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.internal.ConfigurationTopologyException;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.serveraction.kerberos.KerberosInvalidConfigurationException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.StackId;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Maps;

/**
 * ClusterConfigurationRequest unit tests
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({AmbariContext.class})
public class ClusterConfigurationRequestTest {

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock(type = MockType.NICE)
  private Blueprint blueprint;

  @Mock(type = MockType.NICE)
  private AmbariContext ambariContext;

  @Mock(type = MockType.NICE)
  private Cluster cluster;

  @Mock(type = MockType.NICE)
  private Clusters clusters;

  @Mock(type = MockType.NICE)
  private ClusterTopology topology;

  @Mock(type = MockType.NICE)
  private StackAdvisorBlueprintProcessor stackAdvisorBlueprintProcessor;

  @Mock(type = MockType.NICE)
  private Stack stack;

  @Mock(type = MockType.NICE)
  private AmbariManagementController controller;

  @Mock(type = MockType.NICE)
  private KerberosHelper kerberosHelper;

  @Mock(type = MockType.NICE)
  private ConfigHelper configHelper;

  private final String STACK_NAME = "testStack";
  private final String STACK_VERSION = "1";
  private final Map<String, Map<String, String>> stackProperties = new HashMap<>();
  private final Map<String, String> defaultClusterEnvProperties = new HashMap<>();

  @Before
  public void setup() {
    stackProperties.put(ConfigHelper.CLUSTER_ENV, defaultClusterEnvProperties);
  }

  /**
   * testConfigType config type should be in updatedConfigTypes, as no custom property in Blueprint
   * ==> Kerberos config property should be updated
   * @throws Exception
   */
  @Test
  public void testProcessWithKerberos_UpdateKererosConfigProperty_WithNoCustomValue() throws Exception {

    Capture<? extends Set<String>> captureUpdatedConfigTypes = testProcessWithKerberos(null, "defaultTestValue", null);

    Set<String> updatedConfigTypes = captureUpdatedConfigTypes.getValue();
    assertEquals(2, updatedConfigTypes.size());
  }

  /**
   * testConfigType config type should be in updatedConfigTypes, as testProperty in Blueprint is equal to stack
   * default ==> Kerberos config property should be updated
   * @throws Exception
   */
  @Test
  public void testProcessWithKerberos_UpdateKererosConfigProperty_WithCustomValueEqualToStackDefault() throws
    Exception {

    Capture<? extends Set<String>> captureUpdatedConfigTypes = testProcessWithKerberos("defaultTestValue",
      "defaultTestValue", null);

    Set<String> updatedConfigTypes = captureUpdatedConfigTypes.getValue();
    assertEquals(2, updatedConfigTypes.size());

  }

  /**
   * testConfigType config type shouldn't be in updatedConfigTypes, as testProperty in Blueprint is different that
   * stack default (custom value) ==> Kerberos config property shouldn't be updated
   * @throws Exception
   */
  @Test
  public void testProcessWithKerberos_DontUpdateKererosConfigProperty_WithCustomValueDifferentThanStackDefault() throws
    Exception {

    Capture<? extends Set<String>> captureUpdatedConfigTypes = testProcessWithKerberos("testPropertyValue",
      "defaultTestValue", null);

    Set<String> updatedConfigTypes = captureUpdatedConfigTypes.getValue();
    assertEquals(1, updatedConfigTypes.size());
  }

  /**
   * testConfigType config type shouldn't be in updatedConfigTypes, as testProperty in Blueprint is a custom value
   * (no default value in stack for testProperty)
   * ==> Kerberos config property shouldn't be updated
   * @throws Exception
   */
  @Test
  public void testProcessWithKerberos_DontUpdateKererosConfigProperty_WithCustomValueNoStackDefault() throws Exception {

    Capture<? extends Set<String>> captureUpdatedConfigTypes = testProcessWithKerberos("testPropertyValue", null, null);

    Set<String> updatedConfigTypes = captureUpdatedConfigTypes.getValue();
    assertEquals(1, updatedConfigTypes.size());
  }

  @Test
  public void testProcessWithKerberos_DontUpdateKererosConfigProperty_WithKerberosConfigSameAsDefault() throws
    Exception {
    Map<String, Map<String, String>> kerberosConfig = new HashMap<>();
    Map<String, String> properties = new HashMap<>();
    properties.put("testProperty", "defaultTestValue");
    kerberosConfig.put("testConfigType", properties);

    Capture<? extends Set<String>> captureUpdatedConfigTypes = testProcessWithKerberos(null, "defaultTestValue", kerberosConfig);

    Set<String> updatedConfigTypes = captureUpdatedConfigTypes.getValue();
    assertEquals(1, updatedConfigTypes.size());
  }

  @Test
  public void testProcessWithKerberos_DontUpdateKererosConfigProperty_WithOrphanedKerberosConfigType() throws
    Exception {
    Map<String, Map<String, String>> kerberosConfig = new HashMap<>();
    Map<String, String> properties = new HashMap<>();
    properties.put("testProperty", "KERBEROStestValue");
    kerberosConfig.put("orphanedTestConfigType", properties);

    Capture<? extends Set<String>> captureUpdatedConfigTypes = testProcessWithKerberos(null, "defaultTestValue", kerberosConfig);

    Set<String> updatedConfigTypes = captureUpdatedConfigTypes.getValue();
    assertEquals(1, updatedConfigTypes.size());
  }

  private Capture<? extends Set<String>> testProcessWithKerberos(String blueprintPropertyValue, String
    stackPropertyValue,  Map<String, Map<String, String>> kerberosConfig) throws AmbariException, KerberosInvalidConfigurationException,
    ConfigurationTopologyException {


    Map<String, Map<String, String>> existingConfig = new HashMap<>();
    Configuration stackDefaultConfig = new Configuration(existingConfig,
      new HashMap<>());
    if (stackPropertyValue != null) {
      stackDefaultConfig.setProperty("testConfigType", "testProperty", stackPropertyValue);
    }

    Configuration blueprintConfig = new Configuration(stackDefaultConfig.getFullProperties(),
      new HashMap<>());
    if (blueprintPropertyValue != null) {
      blueprintConfig.setProperty("testConfigType", "testProperty", blueprintPropertyValue);
    }

    PowerMock.mockStatic(AmbariContext.class);
    AmbariContext.getController();
    expectLastCall().andReturn(controller).anyTimes();

    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(controller.getKerberosHelper()).andReturn(kerberosHelper).times(1);

    expect(clusters.getCluster("testCluster")).andReturn(cluster).anyTimes();

    expect(blueprint.getStack()).andReturn(stack).anyTimes();
    expect(stack.getName()).andReturn(STACK_NAME).anyTimes();
    expect(stack.getVersion()).andReturn(STACK_VERSION).anyTimes();
    expect(stack.getServiceForConfigType("testConfigType")).andReturn("KERBEROS").anyTimes();
    expect(stack.getAllConfigurationTypes(anyString())).andReturn(Collections.singletonList("testConfigType")
    ).anyTimes();
    expect(stack.getExcludedConfigurationTypes(anyString())).andReturn(Collections.emptySet()).anyTimes();
    expect(stack.getConfigurationPropertiesWithMetadata(anyString(), anyString())).andReturn(Collections.emptyMap()).anyTimes();

    Set<String> services = new HashSet<>();
    services.add("HDFS");
    services.add("KERBEROS");
    services.add("ZOOKEPER");
    expect(blueprint.getServices()).andReturn(services).anyTimes();
    expect(stack.getConfiguration(services)).andReturn(stackDefaultConfig).once();

    List<String> hdfsComponents = new ArrayList<>();
    hdfsComponents.add("NAMENODE");
    List<String> kerberosComponents = new ArrayList<>();
    kerberosComponents.add("KERBEROS_CLIENT");
    List<String> zookeeperComponents = new ArrayList<>();
    zookeeperComponents.add("ZOOKEEPER_SERVER");

    expect(blueprint.getComponents("HDFS")).andReturn(hdfsComponents).anyTimes();
    expect(blueprint.getComponents("KERBEROS")).andReturn(kerberosComponents).anyTimes();
    expect(blueprint.getComponents("ZOOKEPER")).andReturn(zookeeperComponents).anyTimes();

    expect(topology.getAmbariContext()).andReturn(ambariContext).anyTimes();
    expect(topology.getConfigRecommendationStrategy()).andReturn(ConfigRecommendationStrategy.NEVER_APPLY).anyTimes();
    expect(topology.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(blueprint.isValidConfigType("testConfigType")).andReturn(true).anyTimes();

    expect(topology.getConfiguration()).andReturn(blueprintConfig).anyTimes();
    expect(topology.getHostGroupInfo()).andReturn(Collections.emptyMap()).anyTimes();
    expect(topology.getClusterId()).andReturn(Long.valueOf(1)).anyTimes();
    expect(topology.getHostGroupsForComponent(anyString())).andReturn(Collections.emptyList())
      .anyTimes();

    expect(ambariContext.getConfigHelper()).andReturn(configHelper).anyTimes();
    expect(ambariContext.getClusterName(Long.valueOf(1))).andReturn("testCluster").anyTimes();
    expect(ambariContext.createConfigurationRequests(EasyMock.anyObject())).andReturn(Collections
      .emptyList()).anyTimes();

    expect(configHelper.getDefaultStackProperties(
        EasyMock.eq(new StackId(STACK_NAME, STACK_VERSION)))).andReturn(stackProperties).anyTimes();

    if (kerberosConfig == null) {
      kerberosConfig = new HashMap<>();
      Map<String, String> properties = new HashMap<>();
      properties.put("testProperty", "KERBEROStestValue");
      kerberosConfig.put("testConfigType", properties);
     }
    expect(kerberosHelper.ensureHeadlessIdentities(anyObject(Cluster.class), EasyMock.anyObject(),
      EasyMock.anyObject())).andReturn(true).once();
    expect(kerberosHelper.getServiceConfigurationUpdates(anyObject(Cluster.class), EasyMock.anyObject(),
      EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.anyObject(), anyBoolean(), eq(false))).andReturn(kerberosConfig).once();

    Capture<? extends String> captureClusterName = newCapture(CaptureType.ALL);
    Capture<? extends Set<String>> captureUpdatedConfigTypes = newCapture(CaptureType.ALL);
    ambariContext.waitForConfigurationResolution(capture(captureClusterName), capture
      (captureUpdatedConfigTypes));
    expectLastCall();

    PowerMock.replay(stack, blueprint, topology, controller, clusters, kerberosHelper,
        ambariContext, AmbariContext.class, configHelper);

    ClusterConfigurationRequest clusterConfigurationRequest = new ClusterConfigurationRequest(
      ambariContext, topology, false, stackAdvisorBlueprintProcessor, true);
    clusterConfigurationRequest.process();

    verify(blueprint, topology, ambariContext, controller, kerberosHelper, configHelper);


    String clusterName = captureClusterName.getValue();
    assertEquals("testCluster", clusterName);
    return captureUpdatedConfigTypes;
  }

  @Test
  public void testProcessClusterConfigRequestDontIncludeKererosConfigs() throws Exception {

    Map<String, Map<String, String>> existingConfig = new HashMap<>();
    Configuration stackConfig = new Configuration(existingConfig,
      new HashMap<>());

    PowerMock.mockStatic(AmbariContext.class);
    AmbariContext.getController();
    expectLastCall().andReturn(controller).anyTimes();

    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("testCluster")).andReturn(cluster).anyTimes();

    expect(blueprint.getStack()).andReturn(stack).anyTimes();
    expect(stack.getName()).andReturn(STACK_NAME).anyTimes();
    expect(stack.getVersion()).andReturn(STACK_VERSION).anyTimes();
    expect(stack.getAllConfigurationTypes(anyString())).andReturn(Collections.singletonList("testConfigType")).anyTimes();
    expect(stack.getExcludedConfigurationTypes(anyString())).andReturn(Collections.emptySet()).anyTimes();
    expect(stack.getConfigurationPropertiesWithMetadata(anyString(), anyString())).andReturn(Collections.emptyMap()).anyTimes();

    Set<String> services = new HashSet<>();
    services.add("HDFS");
    services.add("KERBEROS");
    services.add("ZOOKEPER");
    expect(blueprint.getServices()).andReturn(services).anyTimes();

    List<String> hdfsComponents = new ArrayList<>();
    hdfsComponents.add("NAMENODE");
    List<String> kerberosComponents = new ArrayList<>();
    kerberosComponents.add("KERBEROS_CLIENT");
    List<String> zookeeperComponents = new ArrayList<>();
    zookeeperComponents.add("ZOOKEEPER_SERVER");

    expect(blueprint.getComponents("HDFS")).andReturn(hdfsComponents).anyTimes();
    expect(blueprint.getComponents("KERBEROS")).andReturn(kerberosComponents).anyTimes();
    expect(blueprint.getComponents("ZOOKEPER")).andReturn(zookeeperComponents).anyTimes();

    expect(topology.getAmbariContext()).andReturn(ambariContext).anyTimes();
    expect(topology.getConfigRecommendationStrategy()).andReturn(ConfigRecommendationStrategy.NEVER_APPLY).anyTimes();
    expect(topology.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(topology.getConfiguration()).andReturn(stackConfig).anyTimes();
    expect(topology.getHostGroupInfo()).andReturn(Collections.emptyMap()).anyTimes();
    expect(topology.getClusterId()).andReturn(Long.valueOf(1)).anyTimes();

    expect(ambariContext.getConfigHelper()).andReturn(configHelper).anyTimes();
    expect(ambariContext.getClusterName(Long.valueOf(1))).andReturn("testCluster").anyTimes();
    expect(ambariContext.createConfigurationRequests(EasyMock.anyObject())).andReturn(Collections
      .emptyList()).anyTimes();

    expect(configHelper.getDefaultStackProperties(
        EasyMock.eq(new StackId(STACK_NAME, STACK_VERSION)))).andReturn(stackProperties).anyTimes();

    PowerMock.replay(stack, blueprint, topology, controller, clusters, ambariContext,
        AmbariContext.class, configHelper);

    ClusterConfigurationRequest clusterConfigurationRequest = new ClusterConfigurationRequest(
      ambariContext, topology, false, stackAdvisorBlueprintProcessor);
    clusterConfigurationRequest.process();

    verify(blueprint, topology, ambariContext, controller, configHelper);

  }

  @Test
  public void testProcessClusterConfigRequestRemoveUnusedConfigTypes() throws Exception {
    // GIVEN
    Configuration configuration = createConfigurations();
    Set<String> services = new HashSet<>();
    services.add("HDFS");
    services.add("RANGER");
    Map<String, HostGroupInfo> hostGroupInfoMap = Maps.newHashMap();
    HostGroupInfo hg1 = new HostGroupInfo("hg1");
    hg1.setConfiguration(createConfigurationsForHostGroup());
    hostGroupInfoMap.put("hg1", hg1);

    expect(topology.getAmbariContext()).andReturn(ambariContext).anyTimes();
    expect(topology.getConfiguration()).andReturn(configuration).anyTimes();
    expect(topology.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(topology.getHostGroupInfo()).andReturn(hostGroupInfoMap);
    expect(blueprint.getStack()).andReturn(stack).anyTimes();
    expect(blueprint.getServices()).andReturn(services).anyTimes();

    expect(blueprint.isValidConfigType("hdfs-site")).andReturn(true).anyTimes();
    expect(blueprint.isValidConfigType("admin-properties")).andReturn(true).anyTimes();
    expect(blueprint.isValidConfigType("yarn-site")).andReturn(false).anyTimes();
    expect(blueprint.isValidConfigType("cluster-env")).andReturn(true).anyTimes();
    expect(blueprint.isValidConfigType("global")).andReturn(true).anyTimes();

    expect(ambariContext.getConfigHelper()).andReturn(configHelper).anyTimes();

    expect(configHelper.getDefaultStackProperties(
        EasyMock.eq(new StackId(STACK_NAME, STACK_VERSION)))).andReturn(stackProperties).anyTimes();

    EasyMock.replay(stack, blueprint, topology, ambariContext, configHelper);
    // WHEN
    new ClusterConfigurationRequest(ambariContext, topology, false, stackAdvisorBlueprintProcessor);
    // THEN
    assertFalse("YARN service not present in topology config thus 'yarn-site' config type should be removed from config.", configuration.getFullProperties().containsKey("yarn-site"));
    assertTrue("HDFS service is present in topology host group config thus 'hdfs-site' config type should be left in the config.", configuration.getFullAttributes().containsKey("hdfs-site"));
    assertTrue("'cluster-env' config type should not be removed from configuration.", configuration.getFullProperties().containsKey("cluster-env"));
    assertTrue("'global' config type should not be removed from configuration.", configuration.getFullProperties().containsKey("global"));

    assertFalse("SPARK service not present in topology host group config thus 'spark-env' config type should be removed from config.", hg1.getConfiguration().getFullAttributes().containsKey("spark-env"));
    assertTrue("HDFS service is present in topology host group config thus 'hdfs-site' config type should be left in the config.", hg1.getConfiguration().getFullAttributes().containsKey("hdfs-site"));
    verify(stack, blueprint, topology, ambariContext, configHelper);
  }

  @Test
  public void testProcessClusterConfigRequestWithOnlyHostGroupConfigRemoveUnusedConfigTypes() throws Exception {
    // Given
    Map<String, Map<String, String>> config = Maps.newHashMap();
    config.put("cluster-env", new HashMap<>());
    config.put("global", new HashMap<>());
    Map<String, Map<String, Map<String, String>>> attributes = Maps.newHashMap();

    Configuration configuration = new Configuration(config, attributes);

    Set<String> services = new HashSet<>();
    services.add("HDFS");
    services.add("RANGER");
    Map<String, HostGroupInfo> hostGroupInfoMap = Maps.newHashMap();
    HostGroupInfo hg1 = new HostGroupInfo("hg1");
    hg1.setConfiguration(createConfigurationsForHostGroup());
    hostGroupInfoMap.put("hg1", hg1);

    expect(topology.getAmbariContext()).andReturn(ambariContext).anyTimes();
    expect(topology.getConfiguration()).andReturn(configuration).anyTimes();
    expect(topology.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(topology.getHostGroupInfo()).andReturn(hostGroupInfoMap);
    expect(blueprint.getStack()).andReturn(stack).anyTimes();
    expect(blueprint.getServices()).andReturn(services).anyTimes();

    expect(blueprint.isValidConfigType("hdfs-site")).andReturn(true).anyTimes();
    expect(blueprint.isValidConfigType("cluster-env")).andReturn(true).anyTimes();
    expect(blueprint.isValidConfigType("global")).andReturn(true).anyTimes();

    expect(ambariContext.getConfigHelper()).andReturn(configHelper).anyTimes();

    expect(configHelper.getDefaultStackProperties(
        EasyMock.eq(new StackId(STACK_NAME, STACK_VERSION)))).andReturn(stackProperties).anyTimes();

    EasyMock.replay(stack, blueprint, topology, ambariContext, configHelper);

    // When

    new ClusterConfigurationRequest(ambariContext, topology, false, stackAdvisorBlueprintProcessor);

    // Then
    assertTrue("'cluster-env' config type should not be removed from configuration.", configuration.getFullProperties().containsKey("cluster-env"));
    assertTrue("'global' config type should not be removed from configuration.", configuration.getFullProperties().containsKey("global"));

    assertFalse("SPARK service not present in topology host group config thus 'spark-env' config type should be removed from config.", hg1.getConfiguration().getFullAttributes().containsKey("spark-env"));
    assertTrue("HDFS service is present in topology host group config thus 'hdfs-site' config type should be left in the config.", hg1.getConfiguration().getFullAttributes().containsKey("hdfs-site"));
    verify(stack, blueprint, topology, ambariContext, configHelper);

  }

  private Configuration createConfigurations() {
    Map<String, Map<String, String>> firstLevelConfig = Maps.newHashMap();
    firstLevelConfig.put("hdfs-site", new HashMap<>());
    firstLevelConfig.put("yarn-site", new HashMap<>());
    firstLevelConfig.put("cluster-env", new HashMap<>());
    firstLevelConfig.put("global", new HashMap<>());

    Map<String, Map<String, Map<String, String>>> firstLevelAttributes = Maps.newHashMap();
    firstLevelAttributes.put("hdfs-site", new HashMap<>());

    Map<String, Map<String, String>> secondLevelConfig = Maps.newHashMap();
    secondLevelConfig.put("admin-properties", new HashMap<>());
    Map<String, Map<String, Map<String, String>>> secondLevelAttributes = Maps.newHashMap();
    secondLevelAttributes.put("admin-properties", new HashMap<>());


    Configuration secondLevelConf = new Configuration(secondLevelConfig, secondLevelAttributes);
    return new Configuration(firstLevelConfig, firstLevelAttributes, secondLevelConf);
  }

  private Configuration createConfigurationsForHostGroup() {
    Map<String, Map<String, String>> firstLevelConfig = Maps.newHashMap();
    firstLevelConfig.put("hdfs-site", new HashMap<>());
    firstLevelConfig.put("spark-env", new HashMap<>());
    firstLevelConfig.put("cluster-env", new HashMap<>());
    firstLevelConfig.put("global", new HashMap<>());

    Map<String, Map<String, Map<String, String>>> firstLevelAttributes = Maps.newHashMap();
    firstLevelAttributes.put("hdfs-site", new HashMap<>());

    Map<String, Map<String, String>> secondLevelConfig = Maps.newHashMap();
    secondLevelConfig.put("admin-properties", new HashMap<>());
    Map<String, Map<String, Map<String, String>>> secondLevelAttributes = Maps.newHashMap();
    secondLevelAttributes.put("admin-properties", new HashMap<>());


    Configuration secondLevelConf = new Configuration(secondLevelConfig, secondLevelAttributes);
    return new Configuration(firstLevelConfig, firstLevelAttributes, secondLevelConf);
  }


}
