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

package org.apache.ambari.server.api.services.stackadvisor;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.controller.internal.ConfigurationTopologyException;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.state.ValueAttributesInfo;
import org.apache.ambari.server.topology.AdvisedConfiguration;
import org.apache.ambari.server.topology.BlueprintImpl;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.ConfigRecommendationStrategy;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.HostGroup;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;

public class StackAdvisorBlueprintProcessorTest {
  private StackAdvisorBlueprintProcessor underTest = new StackAdvisorBlueprintProcessor();

  private ClusterTopology clusterTopology = createMock(ClusterTopology.class);
  private BlueprintImpl blueprint = createMock(BlueprintImpl.class);
  private Stack stack = createMock(Stack.class);
  private HostGroup hostGroup = createMock(HostGroup.class);
  private Configuration configuration = createMock(Configuration.class);

  private static StackAdvisorHelper stackAdvisorHelper = createMock(StackAdvisorHelper.class);

  @BeforeClass
  public static void initClass() {
    StackAdvisorBlueprintProcessor.init(stackAdvisorHelper);
  }

  @Before
  public void setUp() {
    reset(clusterTopology, blueprint, stack, stackAdvisorHelper);
  }

  @Test
  public void testAdviseConfiguration() throws StackAdvisorException, ConfigurationTopologyException, AmbariException {
    // GIVEN
    Map<String, Map<String, String>> props = createProps();
    Map<String, AdvisedConfiguration> advisedConfigurations = new HashMap<>();
    expect(clusterTopology.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(clusterTopology.getHostGroupInfo()).andReturn(createHostGroupInfo()).anyTimes();
    expect(clusterTopology.getAdvisedConfigurations()).andReturn(advisedConfigurations).anyTimes();
    expect(clusterTopology.getConfiguration()).andReturn(configuration).anyTimes();
    expect(clusterTopology.isClusterKerberosEnabled()).andReturn(false).anyTimes();
    expect(clusterTopology.getConfigRecommendationStrategy()).andReturn(ConfigRecommendationStrategy.ALWAYS_APPLY).anyTimes();
    expect(blueprint.getStack()).andReturn(stack).anyTimes();
    expect(stack.getVersion()).andReturn("2.3").anyTimes();
    expect(stack.getName()).andReturn("HDP").anyTimes();
    expect(stack.getConfiguration(Arrays.asList("HDFS", "YARN", "HIVE"))).andReturn(createStackDefaults()).anyTimes();
    expect(blueprint.getServices()).andReturn(Arrays.asList("HDFS", "YARN", "HIVE")).anyTimes();
    expect(blueprint.getHostGroups()).andReturn(createHostGroupMap()).anyTimes();
    expect(blueprint.isValidConfigType("core-site")).andReturn(true).anyTimes();
    expect(hostGroup.getComponentNames()).andReturn(Arrays.asList("comp1", "comp2")).anyTimes();
    expect(stackAdvisorHelper.recommend(anyObject(StackAdvisorRequest.class))).andReturn(createRecommendationResponse());
    expect(configuration.getFullProperties()).andReturn(props).anyTimes();

    replay(clusterTopology, blueprint, stack, hostGroup, configuration, stackAdvisorHelper);
    // WHEN
    underTest.adviseConfiguration(clusterTopology, props);
    // THEN
    assertTrue(advisedConfigurations.get("core-site").getProperties().containsKey("dummyKey1"));
    assertTrue(advisedConfigurations.get("core-site").getProperties().containsKey("dummyKey3"));
    assertTrue(advisedConfigurations.get("core-site").getPropertyValueAttributes().containsKey("dummyKey2"));
    assertTrue(advisedConfigurations.get("core-site").getPropertyValueAttributes().containsKey("dummyKey3"));
    assertEquals("dummyValue", advisedConfigurations.get("core-site").getProperties().get("dummyKey1"));
    assertEquals(Boolean.toString(true), advisedConfigurations.get("core-site")
      .getPropertyValueAttributes().get("dummyKey2").getDelete());
  }

  @Test
  public void testAdviseConfigurationWithOnlyStackDefaultsApply() throws StackAdvisorException, ConfigurationTopologyException, AmbariException {
    // GIVEN
    Map<String, Map<String, String>> props = createProps();
    Map<String, AdvisedConfiguration> advisedConfigurations = new HashMap<>();
    expect(clusterTopology.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(clusterTopology.getHostGroupInfo()).andReturn(createHostGroupInfo()).anyTimes();
    expect(clusterTopology.getAdvisedConfigurations()).andReturn(advisedConfigurations).anyTimes();
    expect(clusterTopology.getConfiguration()).andReturn(configuration).anyTimes();
    expect(clusterTopology.isClusterKerberosEnabled()).andReturn(false).anyTimes();
    expect(clusterTopology.getConfigRecommendationStrategy()).andReturn(ConfigRecommendationStrategy.ONLY_STACK_DEFAULTS_APPLY);
    expect(blueprint.getStack()).andReturn(stack).anyTimes();
    expect(stack.getVersion()).andReturn("2.3").anyTimes();
    expect(stack.getName()).andReturn("HDP").anyTimes();
    expect(stack.getConfiguration(Arrays.asList("HDFS", "YARN", "HIVE"))).andReturn(createStackDefaults()).anyTimes();
    expect(blueprint.getServices()).andReturn(Arrays.asList("HDFS", "YARN", "HIVE")).anyTimes();
    expect(blueprint.getHostGroups()).andReturn(createHostGroupMap()).anyTimes();
    expect(blueprint.isValidConfigType("core-site")).andReturn(true).anyTimes();
    expect(hostGroup.getComponentNames()).andReturn(Arrays.asList("comp1", "comp2")).anyTimes();
    expect(stackAdvisorHelper.recommend(anyObject(StackAdvisorRequest.class))).andReturn(createRecommendationResponse());
    expect(configuration.getFullProperties()).andReturn(props).anyTimes();

    replay(clusterTopology, blueprint, stack, hostGroup, configuration, stackAdvisorHelper);
    // WHEN
    underTest.adviseConfiguration(clusterTopology, props);
    // THEN
    assertTrue(advisedConfigurations.get("core-site").getProperties().containsKey("dummyKey1"));
    assertFalse(advisedConfigurations.get("core-site").getProperties().containsKey("dummyKey3"));
    assertTrue(advisedConfigurations.get("core-site").getPropertyValueAttributes().containsKey("dummyKey2"));
    assertFalse(advisedConfigurations.get("core-site").getPropertyValueAttributes().containsKey("dummyKey3"));
    assertEquals("dummyValue", advisedConfigurations.get("core-site").getProperties().get("dummyKey1"));
    assertEquals(Boolean.toString(true), advisedConfigurations.get("core-site")
      .getPropertyValueAttributes().get("dummyKey2").getDelete());
  }

  @Test
  public void testAdviseConfigurationWithOnlyStackDefaultsApplyWhenNoUserInputForDefault() throws StackAdvisorException, ConfigurationTopologyException, AmbariException {
    // GIVEN
    Map<String, Map<String, String>> props = createProps();
    props.get("core-site").put("dummyKey3", "stackDefaultValue");
    Map<String, AdvisedConfiguration> advisedConfigurations = new HashMap<>();
    expect(clusterTopology.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(clusterTopology.getHostGroupInfo()).andReturn(createHostGroupInfo()).anyTimes();
    expect(clusterTopology.getAdvisedConfigurations()).andReturn(advisedConfigurations).anyTimes();
    expect(clusterTopology.getConfiguration()).andReturn(configuration).anyTimes();
    expect(clusterTopology.isClusterKerberosEnabled()).andReturn(false).anyTimes();
    expect(clusterTopology.getConfigRecommendationStrategy()).andReturn(ConfigRecommendationStrategy.ONLY_STACK_DEFAULTS_APPLY);
    expect(blueprint.getStack()).andReturn(stack).anyTimes();
    expect(stack.getVersion()).andReturn("2.3").anyTimes();
    expect(stack.getName()).andReturn("HDP").anyTimes();
    expect(stack.getConfiguration(Arrays.asList("HDFS", "YARN", "HIVE"))).andReturn(createStackDefaults()).anyTimes();
    expect(blueprint.getServices()).andReturn(Arrays.asList("HDFS", "YARN", "HIVE")).anyTimes();
    expect(blueprint.getHostGroups()).andReturn(createHostGroupMap()).anyTimes();
    expect(blueprint.isValidConfigType("core-site")).andReturn(true).anyTimes();
    expect(hostGroup.getComponentNames()).andReturn(Arrays.asList("comp1", "comp2")).anyTimes();
    expect(stackAdvisorHelper.recommend(anyObject(StackAdvisorRequest.class))).andReturn(createRecommendationResponse());
    expect(configuration.getFullProperties()).andReturn(props).anyTimes();

    replay(clusterTopology, blueprint, stack, hostGroup, configuration, stackAdvisorHelper);
    // WHEN
    underTest.adviseConfiguration(clusterTopology, props);
    // THEN
    assertTrue(advisedConfigurations.get("core-site").getProperties().containsKey("dummyKey1"));
    assertTrue(advisedConfigurations.get("core-site").getPropertyValueAttributes().containsKey("dummyKey2"));
    assertEquals("dummyValue", advisedConfigurations.get("core-site").getProperties().get("dummyKey1"));
    assertEquals(Boolean.toString(true), advisedConfigurations.get("core-site")
      .getPropertyValueAttributes().get("dummyKey2").getDelete());
  }

  @Test
  public void testAdviseConfigurationWith_ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES() throws StackAdvisorException,
      ConfigurationTopologyException, AmbariException {
    // GIVEN
    Map<String, Map<String, String>> props = createProps();
    Map<String, AdvisedConfiguration> advisedConfigurations = new HashMap<>();
    expect(clusterTopology.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(clusterTopology.getHostGroupInfo()).andReturn(createHostGroupInfo()).anyTimes();
    expect(clusterTopology.getAdvisedConfigurations()).andReturn(advisedConfigurations).anyTimes();
    expect(clusterTopology.getConfiguration()).andReturn(configuration).anyTimes();
    expect(clusterTopology.isClusterKerberosEnabled()).andReturn(false).anyTimes();
    expect(clusterTopology.getConfigRecommendationStrategy()).andReturn(ConfigRecommendationStrategy.ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES).anyTimes();
    expect(blueprint.getStack()).andReturn(stack).anyTimes();
    expect(stack.getVersion()).andReturn("2.3").anyTimes();
    expect(stack.getName()).andReturn("HDP").anyTimes();
    expect(stack.getConfiguration(Arrays.asList("HDFS", "YARN", "HIVE"))).andReturn(createStackDefaults()).anyTimes();
    expect(blueprint.getServices()).andReturn(Arrays.asList("HDFS", "YARN", "HIVE")).anyTimes();
    expect(blueprint.getHostGroups()).andReturn(createHostGroupMap()).anyTimes();
    expect(blueprint.isValidConfigType("core-site")).andReturn(true).anyTimes();
    expect(hostGroup.getComponentNames()).andReturn(Arrays.asList("comp1", "comp2")).anyTimes();
    expect(stackAdvisorHelper.recommend(anyObject(StackAdvisorRequest.class))).andReturn(createRecommendationResponse());
    expect(configuration.getFullProperties()).andReturn(props).anyTimes();

    replay(clusterTopology, blueprint, stack, hostGroup, configuration, stackAdvisorHelper);
    // WHEN
    underTest.adviseConfiguration(clusterTopology, props);
    // THEN
    assertTrue(advisedConfigurations.get("core-site").getProperties().containsKey("dummyKey1"));
    assertTrue(advisedConfigurations.get("core-site").getPropertyValueAttributes().containsKey("dummyKey2"));
    assertEquals("dummyValue", advisedConfigurations.get("core-site").getProperties().get("dummyKey1"));
    assertEquals(Boolean.toString(true), advisedConfigurations.get("core-site")
      .getPropertyValueAttributes().get("dummyKey2").getDelete());
  }

  @Test
  public void testAdviseConfigurationWhenConfigurationRecommendFails() throws StackAdvisorException, ConfigurationTopologyException, AmbariException {
    // GIVEN
    Map<String, Map<String, String>> props = createProps();
    Map<String, AdvisedConfiguration> advisedConfigurations = new HashMap<>();
    expect(clusterTopology.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(clusterTopology.getHostGroupInfo()).andReturn(createHostGroupInfo()).anyTimes();
    expect(clusterTopology.getAdvisedConfigurations()).andReturn(advisedConfigurations).anyTimes();
    expect(clusterTopology.getConfiguration()).andReturn(configuration).anyTimes();
    expect(clusterTopology.isClusterKerberosEnabled()).andReturn(false).anyTimes();
    expect(blueprint.getStack()).andReturn(stack).anyTimes();
    expect(stack.getVersion()).andReturn("2.3").anyTimes();
    expect(stack.getName()).andReturn("HDP").anyTimes();
    expect(blueprint.getHostGroups()).andReturn(createHostGroupMap()).anyTimes();
    expect(hostGroup.getComponentNames()).andReturn(Arrays.asList("comp1", "comp2")).anyTimes();
    expect(blueprint.getServices()).andReturn(Arrays.asList("HDFS", "YARN", "HIVE")).anyTimes();
    expect(stackAdvisorHelper.recommend(anyObject(StackAdvisorRequest.class))).andThrow(new StackAdvisorException("ex"));
    expect(configuration.getFullProperties()).andReturn(props);

    replay(clusterTopology, blueprint, stack, hostGroup, configuration, stackAdvisorHelper);
    // WHEN
    try {
      underTest.adviseConfiguration(clusterTopology, props);
      fail("Invalid state");
    } catch (ConfigurationTopologyException e) {
      assertEquals(StackAdvisorBlueprintProcessor.RECOMMENDATION_FAILED, e.getMessage());
    }
  }

  @Test
  public void testAdviseConfigurationWhenConfigurationRecommendHasInvalidResponse() throws StackAdvisorException, ConfigurationTopologyException, AmbariException {
    // GIVEN
    Map<String, Map<String, String>> props = createProps();
    Map<String, AdvisedConfiguration> advisedConfigurations = new HashMap<>();
    expect(clusterTopology.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(clusterTopology.getHostGroupInfo()).andReturn(createHostGroupInfo()).anyTimes();
    expect(clusterTopology.getAdvisedConfigurations()).andReturn(advisedConfigurations).anyTimes();
    expect(clusterTopology.getConfiguration()).andReturn(configuration).anyTimes();
    expect(clusterTopology.isClusterKerberosEnabled()).andReturn(false).anyTimes();
    expect(blueprint.getStack()).andReturn(stack).anyTimes();
    expect(stack.getVersion()).andReturn("2.3").anyTimes();
    expect(stack.getName()).andReturn("HDP").anyTimes();
    expect(blueprint.getServices()).andReturn(Arrays.asList("HDFS", "YARN", "HIVE")).anyTimes();
    expect(blueprint.getHostGroups()).andReturn(createHostGroupMap()).anyTimes();
    expect(hostGroup.getComponentNames()).andReturn(Arrays.asList("comp1", "comp2")).anyTimes();
    expect(stackAdvisorHelper.recommend(anyObject(StackAdvisorRequest.class))).andReturn(new RecommendationResponse());
    expect(configuration.getFullProperties()).andReturn(props);

    replay(clusterTopology, blueprint, stack, hostGroup, configuration, stackAdvisorHelper);
    // WHEN
    try {
      underTest.adviseConfiguration(clusterTopology, props);
      fail("Invalid state");
    } catch (ConfigurationTopologyException e) {
      assertEquals(StackAdvisorBlueprintProcessor.INVALID_RESPONSE, e.getMessage());
    }
  }

  private Map<String, Map<String, String>> createProps() {
    Map<String, Map<String, String>> props = Maps.newHashMap();
    Map<String, String> siteProps = Maps.newHashMap();
    siteProps.put("myprop", "myvalue");
    siteProps.put("dummyKey3", "userinput");
    props.put("core-site", siteProps);
    return props;
  }

  private Map<String, HostGroup> createHostGroupMap() {
    Map<String, HostGroup> hgMap = Maps.newHashMap();
    hgMap.put("hg1", hostGroup);
    hgMap.put("hg2", hostGroup);
    hgMap.put("hg3", hostGroup);
    return hgMap;
  }

  private Map<String, HostGroupInfo> createHostGroupInfo() {
    Map<String, HostGroupInfo> hostGroupInfoMap = new HashMap<>();
    HostGroupInfo hgi1 = new HostGroupInfo("hostGroup1");
    HostGroupInfo hgi2 = new HostGroupInfo("hostGroup2");
    hostGroupInfoMap.put("hg1", hgi1);
    hostGroupInfoMap.put("hg2", hgi2);
    return hostGroupInfoMap;
  }

  private Configuration createStackDefaults() {
    Map<String, Map<String, String>> stackDefaultProps =
      new HashMap<>();
    Map<String, String> coreSiteDefault = new HashMap<>();
    coreSiteDefault.put("dummyKey3", "stackDefaultValue");
    stackDefaultProps.put("core-site", coreSiteDefault);

    Map<String, Map<String, Map<String, String>>> stackDefaultAttributes =
      new HashMap<>();
    return new Configuration(stackDefaultProps, stackDefaultAttributes);
  }

  private RecommendationResponse createRecommendationResponse() {
    RecommendationResponse response = new RecommendationResponse();
    RecommendationResponse.Recommendation recommendations = new RecommendationResponse.Recommendation();
    RecommendationResponse.Blueprint blueprint = new RecommendationResponse.Blueprint();
    Map<String, RecommendationResponse.BlueprintConfigurations> blueprintConfigurationsMap =
      new HashMap<>();
    RecommendationResponse.BlueprintConfigurations blueprintConfig =
      new RecommendationResponse.BlueprintConfigurations();
    Map<String, String> properties = new HashMap<>();
    properties.put("dummyKey1", "dummyValue");
    properties.put("dummyKey3", "dummyValue-override");
    blueprintConfig.setProperties(properties);
    Map<String, ValueAttributesInfo> propAttributes = new HashMap<>();
    ValueAttributesInfo valueAttributesInfo1 = new ValueAttributesInfo();
    ValueAttributesInfo valueAttributesInfo2 = new ValueAttributesInfo();
    valueAttributesInfo1.setDelete("true");
    valueAttributesInfo2.setDelete("true");
    propAttributes.put("dummyKey2", valueAttributesInfo1);
    propAttributes.put("dummyKey3", valueAttributesInfo2);
    blueprintConfig.setPropertyAttributes(propAttributes);
    blueprintConfigurationsMap.put("core-site", blueprintConfig);
    blueprint.setConfigurations(blueprintConfigurationsMap);
    recommendations.setBlueprint(blueprint);
    response.setRecommendations(recommendations);
    return response;
  }


}
