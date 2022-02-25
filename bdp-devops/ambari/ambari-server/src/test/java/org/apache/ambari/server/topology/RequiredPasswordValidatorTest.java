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

import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.expect;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.topology.validators.RequiredPasswordValidator;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Unit tests for RequiredPasswordValidator.
 */
public class RequiredPasswordValidatorTest extends EasyMockSupport {

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock
  private ClusterTopology topology;

  @Mock
  private Blueprint blueprint;

  @Mock
  private Stack stack;

  @Mock
  private HostGroup group1;

  @Mock
  private HostGroup group2;

  private static Configuration stackDefaults;
  private static Configuration bpClusterConfig;
  private static Configuration topoClusterConfig;
  private static Configuration bpGroup1Config;
  private static Configuration bpGroup2Config;
  private static Configuration topoGroup1Config;
  private static Configuration topoGroup2Config;

  private static final Map<String, HostGroup> hostGroups = new HashMap<>();
  private static final Map<String, HostGroupInfo> hostGroupInfo = new HashMap<>();

  private static final Collection<String> group1Components = new HashSet<>();
  private static final Collection<String> group2Components = new HashSet<>();
  private static final Collection<String> service1Components = new HashSet<>();
  private static final Collection<String> service2Components = new HashSet<>();
  private static final Collection<String> service3Components = new HashSet<>();

  private static final Collection<Stack.ConfigProperty> service1RequiredPwdConfigs = new HashSet<>();
  private static final Collection<Stack.ConfigProperty> service2RequiredPwdConfigs = new HashSet<>();
  private static final Collection<Stack.ConfigProperty> service3RequiredPwdConfigs = new HashSet<>();

  @TestSubject
  private RequiredPasswordValidator validator = new RequiredPasswordValidator();


  @Before
  public void setup() {

    stackDefaults = new Configuration(new HashMap<>(),
      new HashMap<>());

    bpClusterConfig = new Configuration(new HashMap<>(),
      new HashMap<>(), stackDefaults);

    topoClusterConfig = new Configuration(new HashMap<>(),
      new HashMap<>(), bpClusterConfig);

    bpGroup1Config = new Configuration(new HashMap<>(),
      new HashMap<>(), topoClusterConfig);

    bpGroup2Config = new Configuration(new HashMap<>(),
      new HashMap<>(), topoClusterConfig);

    topoGroup1Config = new Configuration(new HashMap<>(),
      new HashMap<>(), bpGroup1Config);

    topoGroup2Config = new Configuration(new HashMap<>(),
      new HashMap<>(), bpGroup2Config);

    service1RequiredPwdConfigs.clear();
    service2RequiredPwdConfigs.clear();
    service3RequiredPwdConfigs.clear();

    hostGroups.put("group1", group1);
    hostGroups.put("group2", group2);

    group1Components.add("component1");
    group1Components.add("component2");
    group1Components.add("component3");

    group2Components.add("component1");
    group2Components.add("component4");

    service1Components.add("component1");
    service1Components.add("component2");
    service2Components.add("component3");
    service3Components.add("component4");

    HostGroupInfo hostGroup1Info = new HostGroupInfo("group1");
    hostGroup1Info.setConfiguration(topoGroup1Config);
    HostGroupInfo hostGroup2Info = new HostGroupInfo("group2");
    hostGroup2Info.setConfiguration(topoGroup2Config);
    hostGroupInfo.put("group1", hostGroup1Info);
    hostGroupInfo.put("group2", hostGroup2Info);

    expect(topology.getConfiguration()).andReturn(topoClusterConfig).anyTimes();
    expect(topology.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(topology.getHostGroupInfo()).andReturn(hostGroupInfo).anyTimes();

    expect(blueprint.getHostGroups()).andReturn(hostGroups).anyTimes();
    expect(blueprint.getHostGroup("group1")).andReturn(group1).anyTimes();
    expect(blueprint.getHostGroup("group2")).andReturn(group2).anyTimes();
    expect(blueprint.getStack()).andReturn(stack).anyTimes();

    expect(group1.getComponentNames()).andReturn(group1Components).anyTimes();
    expect(group2.getComponentNames()).andReturn(group2Components).anyTimes();
    expect(group1.getComponents("service1")).andReturn(Arrays.asList("component1", "component2")).anyTimes();
    expect(group1.getComponents("service2")).andReturn(Arrays.asList("component3")).anyTimes();
    expect(group1.getComponents("service3")).andReturn(Collections.emptySet()).anyTimes();
    expect(group2.getComponents("service1")).andReturn(Arrays.asList("component1")).anyTimes();
    expect(group2.getComponents("service2")).andReturn(Collections.emptySet()).anyTimes();
    expect(group2.getComponents("service3")).andReturn(Arrays.asList("component4")).anyTimes();

    expect(stack.getServiceForComponent("component1")).andReturn("service1").anyTimes();
    expect(stack.getServiceForComponent("component2")).andReturn("service1").anyTimes();
    expect(stack.getServiceForComponent("component3")).andReturn("service2").anyTimes();
    expect(stack.getServiceForComponent("component4")).andReturn("service3").anyTimes();

    expect(stack.getRequiredConfigurationProperties("service1", PropertyInfo.PropertyType.PASSWORD)).andReturn(service1RequiredPwdConfigs).anyTimes();
    expect(stack.getRequiredConfigurationProperties("service2", PropertyInfo.PropertyType.PASSWORD)).andReturn(service2RequiredPwdConfigs).anyTimes();
    expect(stack.getRequiredConfigurationProperties("service3", PropertyInfo.PropertyType.PASSWORD)).andReturn(service3RequiredPwdConfigs).anyTimes();

  }

  @After
  public void tearDown() {
    verifyAll();
    resetAll();
  }


  @Test
  public void testValidate_noRequiredProps__noDefaultPwd() throws Exception {
    // GIVEN
    // no required pwd properties so shouldn't throw an exception
    expect(topology.getDefaultPassword()).andReturn(null);
    replayAll();

    // WHEN
    validator.validate(topology);
  }

  @Test
  public void testValidate_noRequiredProps__defaultPwd() throws Exception {
    // GIVEN
    expect(topology.getDefaultPassword()).andReturn("pwd");
    replayAll();

    // WHEN
    validator.validate(topology);

  }

  @Test(expected = InvalidTopologyException.class)
  public void testValidate_missingPwd__NoDefaultPwd() throws Exception {
    expect(topology.getDefaultPassword()).andReturn(null);
    replayAll();

    Stack.ConfigProperty pwdProp = new Stack.ConfigProperty("test-type", "pwdProp", null);
    service1RequiredPwdConfigs.add(pwdProp);


    validator.validate(topology);
  }

  @Test
  public void testValidate_missingPwd__defaultPwd() throws Exception {
    expect(topology.getDefaultPassword()).andReturn("default-pwd");
    replayAll();

    Stack.ConfigProperty pwdProp = new Stack.ConfigProperty("test-type", "pwdProp", null);
    service1RequiredPwdConfigs.add(pwdProp);

    // default value should be set
    validator.validate(topology);

    assertEquals(1, topoClusterConfig.getProperties().size());
    assertEquals("default-pwd", topoClusterConfig.getProperties().get("test-type").get("pwdProp"));
  }

  @Test
  public void testValidate_pwdPropertyInTopoGroupConfig__NoDefaultPwd() throws Exception {
    expect(topology.getDefaultPassword()).andReturn(null);
    replayAll();

    Stack.ConfigProperty pwdProp = new Stack.ConfigProperty("test-type", "pwdProp", null);
    service3RequiredPwdConfigs.add(pwdProp);
    // group2 has a component from service 3
    topoGroup2Config.getProperties().put("test-type", Collections.singletonMap("pwdProp", "secret"));

    validator.validate(topology);
  }

  @Test
  public void testValidate_pwdPropertyInTopoClusterConfig__NoDefaultPwd() throws Exception {
    expect(topology.getDefaultPassword()).andReturn(null);
    replayAll();

    Stack.ConfigProperty pwdProp = new Stack.ConfigProperty("test-type", "pwdProp", null);
    service3RequiredPwdConfigs.add(pwdProp);
    // group2 has a component from service 3
    topoClusterConfig.getProperties().put("test-type", Collections.singletonMap("pwdProp", "secret"));

    validator.validate(topology);
  }

  @Test
  public void testValidate_pwdPropertyInBPGroupConfig__NoDefaultPwd() throws Exception {
    expect(topology.getDefaultPassword()).andReturn(null);
    replayAll();

    Stack.ConfigProperty pwdProp = new Stack.ConfigProperty("test-type", "pwdProp", null);
    service3RequiredPwdConfigs.add(pwdProp);
    // group2 has a component from service 3
    bpGroup2Config.getProperties().put("test-type", Collections.singletonMap("pwdProp", "secret"));


    validator.validate(topology);
  }

  @Test
  public void testValidate_pwdPropertyInBPClusterConfig__NoDefaultPwd() throws Exception {
    expect(topology.getDefaultPassword()).andReturn(null);
    replayAll();

    Stack.ConfigProperty pwdProp = new Stack.ConfigProperty("test-type", "pwdProp", null);
    service3RequiredPwdConfigs.add(pwdProp);
    // group2 has a component from service 3
    bpClusterConfig.getProperties().put("test-type", Collections.singletonMap("pwdProp", "secret"));


    validator.validate(topology);
  }

  @Test(expected = InvalidTopologyException.class)
  public void testValidate_pwdPropertyInStackConfig__NoDefaultPwd() throws Exception {
    expect(topology.getDefaultPassword()).andReturn(null);
    replayAll();

    Stack.ConfigProperty pwdProp = new Stack.ConfigProperty("test-type", "pwdProp", null);
    service3RequiredPwdConfigs.add(pwdProp);
    // group2 has a component from service 3
    stackDefaults.getProperties().put("test-type", Collections.singletonMap("pwdProp", "secret"));


    // because stack config is ignored for validation, an exception should be thrown
    validator.validate(topology);
  }

  @Test
  public void testValidate_twoRequiredPwdOneSpecified__defaultPwd() throws Exception {
    expect(topology.getDefaultPassword()).andReturn("default-pwd");
    replayAll();

    Stack.ConfigProperty pwdProp = new Stack.ConfigProperty("test-type", "pwdProp", null);
    Stack.ConfigProperty pwdProp2 = new Stack.ConfigProperty("test2-type", "pwdProp2", null);
    service1RequiredPwdConfigs.add(pwdProp);
    service3RequiredPwdConfigs.add(pwdProp2);

    topoClusterConfig.getProperties().put("test2-type", Collections.singletonMap("pwdProp2", "secret"));

    // default value should be set
    validator.validate(topology);

    assertEquals(2, topoClusterConfig.getProperties().size());
    assertEquals("default-pwd", topoClusterConfig.getProperties().get("test-type").get("pwdProp"));
    assertEquals("secret", topoClusterConfig.getProperties().get("test2-type").get("pwdProp2"));
  }

  @Test
  public void testValidate_twoRequiredPwdTwoSpecified__noDefaultPwd() throws Exception {
    expect(topology.getDefaultPassword()).andReturn("default-pwd");
    replayAll();

    Stack.ConfigProperty pwdProp = new Stack.ConfigProperty("test-type", "pwdProp", null);
    Stack.ConfigProperty pwdProp2 = new Stack.ConfigProperty("test2-type", "pwdProp2", null);
    service1RequiredPwdConfigs.add(pwdProp);
    service3RequiredPwdConfigs.add(pwdProp2);

    topoClusterConfig.getProperties().put("test2-type", Collections.singletonMap("pwdProp2", "secret2"));
    topoClusterConfig.getProperties().put("test-type", Collections.singletonMap("pwdProp", "secret1"));

    // default value should be set
    validator.validate(topology);

    assertEquals(2, topoClusterConfig.getProperties().size());
    assertEquals("secret1", topoClusterConfig.getProperties().get("test-type").get("pwdProp"));
    assertEquals("secret2", topoClusterConfig.getProperties().get("test2-type").get("pwdProp2"));
  }

  @Test
  public void testValidate_multipleMissingPwd__defaultPwd() throws Exception {
    expect(topology.getDefaultPassword()).andReturn("default-pwd");
    replayAll();

    Stack.ConfigProperty pwdProp = new Stack.ConfigProperty("test-type", "pwdProp", null);
    Stack.ConfigProperty pwdProp2 = new Stack.ConfigProperty("test2-type", "pwdProp2", null);
    service1RequiredPwdConfigs.add(pwdProp);
    service3RequiredPwdConfigs.add(pwdProp2);

    // default value should be set
    validator.validate(topology);

    assertEquals(2, topoClusterConfig.getProperties().size());
    assertEquals("default-pwd", topoClusterConfig.getProperties().get("test-type").get("pwdProp"));
    assertEquals("default-pwd", topoClusterConfig.getProperties().get("test2-type").get("pwdProp2"));
  }

}
