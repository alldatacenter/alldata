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

package org.apache.ambari.server.topology;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.state.AutoDeployInfo;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.DependencyConditionInfo;
import org.apache.ambari.server.state.DependencyInfo;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * BlueprintValidatorImpl unit tests.
 */
public class BlueprintValidatorImplTest {

  private final Map<String, HostGroup> hostGroups = new LinkedHashMap<>();
  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock(type = MockType.NICE)
  private Blueprint blueprint;

  @Mock(type = MockType.NICE)
  private Stack stack;

  @Mock(type = MockType.NICE)
  private HostGroup group1;

  @Mock(type = MockType.NICE)
  private HostGroup group2;

  @Mock(type = MockType.NICE)
  private DependencyInfo dependency1;
  @Mock(type = MockType.NICE)
  private DependencyInfo dependency2;

  @Mock(type = MockType.NICE)
  private ComponentInfo dependencyComponentInfo;
  @Mock(type = MockType.NICE)
  private DependencyConditionInfo dependencyConditionInfo1;
  @Mock(type = MockType.NICE)
  private DependencyConditionInfo dependencyConditionInfo2;

  private final Collection<String> group1Components = new ArrayList<>();
  private final Collection<String> group2Components = new ArrayList<>();
  private final Collection<String> services = new ArrayList<>();

  private Collection<DependencyInfo> dependencies1 = new ArrayList<>();
  private List<DependencyConditionInfo> dependenciesConditionInfos1 = new ArrayList<>();
  private AutoDeployInfo autoDeploy = new AutoDeployInfo();
  private Map<String, Map<String, String>> configProperties = new HashMap<>();
  private Configuration configuration = new Configuration(configProperties, Collections.emptyMap());


  @Before
  public void setup() {
    hostGroups.put("group1", group1);
    hostGroups.put("group2", group2);

    autoDeploy.setEnabled(true);
    autoDeploy.setCoLocate("service1/component2");

    expect(blueprint.getStack()).andReturn(stack).anyTimes();
    expect(blueprint.getHostGroups()).andReturn(hostGroups).anyTimes();
    expect(blueprint.getServices()).andReturn(services).anyTimes();

    expect(group1.getComponentNames()).andReturn(group1Components).anyTimes();
    expect(group1.getName()).andReturn("host-group-1").anyTimes();
    expect(group2.getComponentNames()).andReturn(group2Components).anyTimes();
    expect(group2.getName()).andReturn("host-group-2").anyTimes();

    expect(stack.getDependenciesForComponent("component1")).andReturn(dependencies1).anyTimes();
    expect(stack.getDependenciesForComponent("component2")).andReturn(dependencies1).anyTimes();
    expect(stack.getDependenciesForComponent("component3")).andReturn(dependencies1).anyTimes();
    expect(stack.getDependenciesForComponent("component4")).andReturn(dependencies1).anyTimes();

    expect(stack.getCardinality("component1")).andReturn(new Cardinality("1"));
    expect(stack.getCardinality("component2")).andReturn(new Cardinality("1+"));
    expect(stack.getCardinality("component3")).andReturn(new Cardinality("1+"));
    dependenciesConditionInfos1.add(dependencyConditionInfo1);
    dependenciesConditionInfos1.add(dependencyConditionInfo2);

    expect(blueprint.getConfiguration()).andReturn(configuration).anyTimes();
  }

  @After
  public void tearDown() {
    reset(blueprint, stack, group1, group2, dependency1, dependency2, dependencyConditionInfo1, dependencyConditionInfo2);
  }

  @Test
  public void testValidateTopology_basic() throws Exception {
    group1Components.add("component1");
    group1Components.add("component1");

    services.addAll(Arrays.asList("service1", "service2"));

    expect(stack.getComponents("service1")).andReturn(Collections.singleton("component1")).anyTimes();
    expect(stack.getComponents("service2")).andReturn(Collections.singleton("component2")).anyTimes();

    expect(blueprint.getHostGroupsForComponent("component1")).andReturn(Collections.singleton(group1)).anyTimes();
    expect(blueprint.getHostGroupsForComponent("component2")).andReturn(Arrays.asList(group1, group2)).anyTimes();

    replay(blueprint, stack, group1, group2, dependency1);
    BlueprintValidator validator = new BlueprintValidatorImpl(blueprint);
    validator.validateTopology();
  }

  @Test(expected = InvalidTopologyException.class)
  public void testValidateTopology_basic_negative() throws Exception {
    group1Components.add("component2");

    services.addAll(Collections.singleton("service1"));

    expect(stack.getComponents("service1")).andReturn(Arrays.asList("component1", "component2")).anyTimes();

    expect(blueprint.getHostGroupsForComponent("component1")).andReturn(Collections.emptyList()).anyTimes();
    expect(blueprint.getHostGroupsForComponent("component2")).andReturn(Arrays.asList(group1, group2)).anyTimes();

    replay(blueprint, stack, group1, group2, dependency1);
    BlueprintValidator validator = new BlueprintValidatorImpl(blueprint);
    validator.validateTopology();
  }

  @Test
  public void testValidateTopology_autoDeploy() throws Exception {
    group1Components.add("component2");
    services.addAll(Collections.singleton("service1"));

    expect(blueprint.getHostGroupsForComponent("component1")).andReturn(Collections.emptyList()).anyTimes();
    expect(blueprint.getHostGroupsForComponent("component2")).andReturn(Arrays.asList(group1, group2)).anyTimes();

    expect(stack.getComponents("service1")).andReturn(Arrays.asList("component1", "component2")).anyTimes();
    expect(stack.getAutoDeployInfo("component1")).andReturn(autoDeploy).anyTimes();

    expect(group1.addComponent("component1")).andReturn(true).once();

    replay(blueprint, stack, group1, group2, dependency1);
    BlueprintValidator validator = new BlueprintValidatorImpl(blueprint);
    validator.validateTopology();

    verify(group1);
  }

  @Test(expected=InvalidTopologyException.class)
  public void testValidateTopology_exclusiveDependency() throws Exception {
    group1Components.add("component2");
    group1Components.add("component3");
    dependencies1.add(dependency1);
    services.addAll(Collections.singleton("service1"));

    expect(blueprint.getHostGroupsForComponent("component1")).andReturn(Arrays.asList(group1, group2)).anyTimes();
    expect(blueprint.getHostGroupsForComponent("component2")).andReturn(Arrays.asList(group1, group2)).anyTimes();
    expect(blueprint.getHostGroupsForComponent("component3")).andReturn(Arrays.asList(group1, group2)).anyTimes();

    expect(stack.getComponents("service1")).andReturn(Arrays.asList("component1", "component2")).anyTimes();
    expect(stack.getComponents("service2")).andReturn(Collections.singleton("component3")).anyTimes();
    expect(stack.getAutoDeployInfo("component1")).andReturn(autoDeploy).anyTimes();

    AutoDeployInfo dependencyAutoDeploy = new AutoDeployInfo();
    dependencyAutoDeploy.setEnabled(true);
    dependencyAutoDeploy.setCoLocate("service1/component1");

    expect(dependency1.getScope()).andReturn("host").anyTimes();
    expect(dependency1.getType()).andReturn("exclusive").anyTimes();
    expect(dependency1.getAutoDeploy()).andReturn(dependencyAutoDeploy).anyTimes();
    expect(dependency1.getComponentName()).andReturn("component3").anyTimes();
    expect(dependency1.getServiceName()).andReturn("service1").anyTimes();
    expect(dependency1.getName()).andReturn("dependency1").anyTimes();

    expect(dependencyComponentInfo.isClient()).andReturn(true).anyTimes();
    expect(stack.getComponentInfo("component3")).andReturn(dependencyComponentInfo).anyTimes();

    replay(blueprint, stack, group1, group2, dependency1, dependencyComponentInfo);

    BlueprintValidator validator = new BlueprintValidatorImpl(blueprint);
    validator.validateTopology();

    verify(group1);
  }

  @Test
  public void testValidateTopology_autoDeploy_hasDependency() throws Exception {
    group1Components.add("component2");
    dependencies1.add(dependency1);
    services.addAll(Collections.singleton("service1"));

    expect(blueprint.getHostGroupsForComponent("component1")).andReturn(Collections.emptyList()).anyTimes();
    expect(blueprint.getHostGroupsForComponent("component2")).andReturn(Arrays.asList(group1, group2)).anyTimes();
    expect(blueprint.getHostGroupsForComponent("component3")).andReturn(Collections.emptyList()).anyTimes();

    expect(stack.getComponents("service1")).andReturn(Arrays.asList("component1", "component2")).anyTimes();
    expect(stack.getComponents("service2")).andReturn(Collections.singleton("component3")).anyTimes();
    expect(stack.getAutoDeployInfo("component1")).andReturn(autoDeploy).anyTimes();

    AutoDeployInfo dependencyAutoDeploy = new AutoDeployInfo();
    dependencyAutoDeploy.setEnabled(true);
    dependencyAutoDeploy.setCoLocate("service1/component1");

    expect(dependency1.getScope()).andReturn("host").anyTimes();
    expect(dependency1.getType()).andReturn("inclusive").anyTimes();
    expect(dependency1.getAutoDeploy()).andReturn(dependencyAutoDeploy).anyTimes();
    expect(dependency1.getComponentName()).andReturn("component3").anyTimes();
    expect(dependency1.getServiceName()).andReturn("service1").anyTimes();
    expect(dependency1.getName()).andReturn("dependency1").anyTimes();

    expect(dependencyComponentInfo.isClient()).andReturn(true).anyTimes();
    expect(stack.getComponentInfo("component3")).andReturn(dependencyComponentInfo).anyTimes();

    expect(group1.addComponent("component1")).andReturn(true).once();
    expect(group1.addComponent("component3")).andReturn(true).once();

    replay(blueprint, stack, group1, group2, dependency1, dependencyComponentInfo);

    BlueprintValidator validator = new BlueprintValidatorImpl(blueprint);
    validator.validateTopology();

    verify(group1);
  }

  @Test(expected=InvalidTopologyException.class)
  public void testValidateRequiredProperties_SqlaInHiveStackHdp22() throws Exception {
    Map<String, String> hiveEnvConfig = new HashMap<>();
    hiveEnvConfig.put("hive_database","Existing SQL Anywhere Database");
    configProperties.put("hive-env", hiveEnvConfig);

    group1Components.add("HIVE_METASTORE");

    services.addAll(Arrays.asList("HIVE"));

    org.apache.ambari.server.configuration.Configuration serverConfig =
        BlueprintImplTest.setupConfigurationWithGPLLicense(true);

    Configuration config = new Configuration(new HashMap<>(), new HashMap<>());
    expect(group1.getConfiguration()).andReturn(config).anyTimes();

    expect(stack.getComponents("HIVE")).andReturn(Collections.singleton("HIVE_METASTORE")).anyTimes();
    expect(stack.getVersion()).andReturn("2.2").once();
    expect(stack.getName()).andReturn("HDP").once();

    expect(blueprint.getHostGroupsForComponent("HIVE_METASTORE")).andReturn(Collections.singleton(group1)).anyTimes();

    replay(blueprint, stack, group1, group2, dependency1, serverConfig);
    BlueprintValidator validator = new BlueprintValidatorImpl(blueprint);
    validator.validateRequiredProperties();
  }

  @Test(expected=InvalidTopologyException.class)
  public void testValidateRequiredProperties_SqlaInOozieStackHdp22() throws Exception {
    Map<String, String> hiveEnvConfig = new HashMap<>();
    hiveEnvConfig.put("oozie_database","Existing SQL Anywhere Database");
    configProperties.put("oozie-env", hiveEnvConfig);

    group1Components.add("OOZIE_SERVER");

    services.addAll(Arrays.asList("OOZIE"));

    org.apache.ambari.server.configuration.Configuration serverConfig =
        BlueprintImplTest.setupConfigurationWithGPLLicense(true);

    Configuration config = new Configuration(new HashMap<>(), new HashMap<>());
    expect(group1.getConfiguration()).andReturn(config).anyTimes();

    expect(stack.getComponents("OOZIE")).andReturn(Collections.singleton("OOZIE_SERVER")).anyTimes();
    expect(stack.getVersion()).andReturn("2.2").once();
    expect(stack.getName()).andReturn("HDP").once();

    expect(blueprint.getHostGroupsForComponent("OOZIE_SERVER")).andReturn(Collections.singleton(group1)).anyTimes();

    replay(blueprint, stack, group1, group2, dependency1, serverConfig);
    BlueprintValidator validator = new BlueprintValidatorImpl(blueprint);
    validator.validateRequiredProperties();
  }

  @Test
  public void testShouldDependencyBeExcludedWenRelatedServiceIsNotInBlueprint() throws Exception {
    // GIVEN
    hostGroups.clear();
    hostGroups.put("group1", group1);

    group1Components.add("component-1");
    dependencies1.add(dependency1);
    services.addAll(Collections.singleton("service-1"));


    expect(blueprint.getHostGroupsForComponent("component-1")).andReturn(Arrays.asList(group1)).anyTimes();
    expect(blueprint.getName()).andReturn("blueprint-1").anyTimes();

    Cardinality cardinality = new Cardinality("1");

    expect(stack.getComponents("service-1")).andReturn(Arrays.asList("component-1")).anyTimes();
    expect(stack.getAutoDeployInfo("component-1")).andReturn(autoDeploy).anyTimes();
    expect(stack.getDependenciesForComponent("component-1")).andReturn(dependencies1).anyTimes();
    expect(stack.getCardinality("component-1")).andReturn(cardinality).anyTimes();


    AutoDeployInfo dependencyAutoDeploy = new AutoDeployInfo();
    dependencyAutoDeploy.setEnabled(true);
    dependencyAutoDeploy.setCoLocate("service1/component1");

    expect(dependency1.getScope()).andReturn("host").anyTimes();
    expect(dependency1.getAutoDeploy()).andReturn(dependencyAutoDeploy).anyTimes();
    expect(dependency1.getComponentName()).andReturn("component-d").anyTimes();
    expect(dependency1.getServiceName()).andReturn("service-d").anyTimes();
    expect(dependency1.getName()).andReturn("dependency-1").anyTimes();


    expect(dependencyComponentInfo.isClient()).andReturn(true).anyTimes();
    expect(stack.getComponentInfo("component-d")).andReturn(dependencyComponentInfo).anyTimes();

    replay(blueprint, stack, group1, group2, dependency1, dependencyComponentInfo);

    // WHEN
    BlueprintValidator validator = new BlueprintValidatorImpl(blueprint);
    validator.validateTopology();

    // THEN
    verify(group1);

  }
  @Test(expected=InvalidTopologyException.class)
  public void testShouldThrowErrorWhenDependentComponentIsNotInBlueprint() throws Exception {
    // GIVEN
    hostGroups.clear();
    hostGroups.put("group1", group1);

    group1Components.add("component-1");
    dependencies1.add(dependency1);
    services.addAll(Collections.singleton("service-1"));


    expect(blueprint.getHostGroupsForComponent("component-1")).andReturn(Arrays.asList(group1)).anyTimes();
    expect(blueprint.getName()).andReturn("blueprint-1").anyTimes();

    Cardinality cardinality = new Cardinality("1");

    expect(stack.getComponents("service-1")).andReturn(Arrays.asList("component-1")).anyTimes();
    expect(stack.getAutoDeployInfo("component-1")).andReturn(autoDeploy).anyTimes();
    expect(stack.getDependenciesForComponent("component-1")).andReturn(dependencies1).anyTimes();
    expect(stack.getCardinality("component-1")).andReturn(cardinality).anyTimes();


    AutoDeployInfo dependencyAutoDeploy = null;

    expect(dependency1.getScope()).andReturn("host").anyTimes();
    expect(dependency1.getType()).andReturn("inclusive").anyTimes();
    expect(dependency1.getAutoDeploy()).andReturn(dependencyAutoDeploy).anyTimes();
    expect(dependency1.getComponentName()).andReturn("component-d").anyTimes();
    expect(dependency1.getServiceName()).andReturn("service-d").anyTimes();
    expect(dependency1.getName()).andReturn("dependency-1").anyTimes();


    expect(stack.getComponentInfo("component-d")).andReturn(dependencyComponentInfo).anyTimes();

    replay(blueprint, stack, group1, group2, dependency1, dependencyComponentInfo);

    // WHEN
    BlueprintValidator validator = new BlueprintValidatorImpl(blueprint);
    validator.validateTopology();

    // THEN
    verify(group1);

  }
  @Test(expected=InvalidTopologyException.class)
  public void testWhenComponentIsConditionallyDependentAndOnlyOneOfTheConditionsIsSatisfied() throws Exception {
    // GIVEN
    hostGroups.clear();
    hostGroups.put("group1", group1);

    group1Components.add("component-1");
    dependencies1.add(dependency1);
    dependencies1.add(dependency2);
    services.addAll(Collections.singleton("service-1"));


    expect(blueprint.getHostGroupsForComponent("component-1")).andReturn(Arrays.asList(group1)).anyTimes();
    expect(blueprint.getName()).andReturn("blueprint-1").anyTimes();
    Map<String, Map<String, String>> properties = new HashMap<>();
    Map<String, String> typeProps = new HashMap<>();
    typeProps.put("yarn.resourcemanager.hostname", "testhost");
    properties.put("yarn-site", typeProps);

    Configuration clusterConfig = new Configuration(properties,
       Collections.emptyMap());

    Cardinality cardinality = new Cardinality("1");

    expect(stack.getComponents("service-1")).andReturn(Arrays.asList("component-1")).anyTimes();
    expect(stack.getAutoDeployInfo("component-1")).andReturn(autoDeploy).anyTimes();
    expect(stack.getDependenciesForComponent("component-1")).andReturn(dependencies1).anyTimes();
    expect(stack.getCardinality("component-1")).andReturn(cardinality).anyTimes();

    AutoDeployInfo dependencyAutoDeploy = null;

    expect(dependency1.getScope()).andReturn("host").anyTimes();
    expect(dependency1.getType()).andReturn("inclusive").anyTimes();
    expect(dependency1.getAutoDeploy()).andReturn(dependencyAutoDeploy).anyTimes();
    expect(dependency1.getComponentName()).andReturn("component-d").anyTimes();
    expect(dependency1.getServiceName()).andReturn("service-d").anyTimes();
    expect(dependency1.getName()).andReturn("dependency-1").anyTimes();
    expect(dependency1.hasDependencyConditions()).andReturn(true).anyTimes();
    expect(dependency1.getDependencyConditions()).andReturn(dependenciesConditionInfos1).anyTimes();
    expect(dependency2.getScope()).andReturn("host").anyTimes();
    expect(dependency2.getType()).andReturn("inclusive").anyTimes();
    expect(dependency2.getAutoDeploy()).andReturn(dependencyAutoDeploy).anyTimes();
    expect(dependency2.getComponentName()).andReturn("component-d").anyTimes();
    expect(dependency2.getServiceName()).andReturn("service-d").anyTimes();
    expect(dependency2.getName()).andReturn("dependency-2").anyTimes();
    expect(dependency2.hasDependencyConditions()).andReturn(false).anyTimes();

    expect(dependencyConditionInfo1.isResolved(EasyMock.anyObject())).andReturn(true).anyTimes();
    expect(dependencyConditionInfo2.isResolved(EasyMock.anyObject())).andReturn(false).anyTimes();


    expect(dependencyComponentInfo.isClient()).andReturn(false).anyTimes();
    expect(stack.getComponentInfo("component-d")).andReturn(dependencyComponentInfo).anyTimes();

    replay(blueprint, stack, group1, group2, dependency1, dependency2, dependencyComponentInfo,dependencyConditionInfo1,dependencyConditionInfo2);

    // WHEN
    BlueprintValidator validator = new BlueprintValidatorImpl(blueprint);
    validator.validateTopology();

    // THEN
    verify(group1);

  }
}
