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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.apache.ambari.server.state.SecurityType;
import org.junit.Before;
import org.junit.Test;


/**
 * Blueprint unit tests.
 */
public class BlueprintImplTest {
  private static final Map<String, Map<String, Map<String, String>>> EMPTY_ATTRIBUTES = new HashMap<>();
  private static final Map<String, Map<String, String>> EMPTY_PROPERTIES = new HashMap<>();
  private static final Configuration EMPTY_CONFIGURATION = new Configuration(EMPTY_PROPERTIES, EMPTY_ATTRIBUTES);

  Stack stack = createNiceMock(Stack.class);
  Setting setting = createNiceMock(Setting.class);
  HostGroup group1 = createMock(HostGroup.class);
  HostGroup group2 = createMock(HostGroup.class);
  Set<HostGroup> hostGroups = new HashSet<>();
  Set<String> group1Components = new HashSet<>();
  Set<String> group2Components = new HashSet<>();
  Map<String, Map<String, String>> properties = new HashMap<>();
  Map<String, String> hdfsProps = new HashMap<>();
  Configuration configuration = new Configuration(properties, EMPTY_ATTRIBUTES, EMPTY_CONFIGURATION);
  org.apache.ambari.server.configuration.Configuration serverConfig;

  @Before
  public void setup() throws NoSuchFieldException, IllegalAccessException {
    properties.put("hdfs-site", hdfsProps);
    hdfsProps.put("foo", "val");
    hdfsProps.put("bar", "val");
    hdfsProps.put("dfs.nameservices", "val");
    Map<String, String> category1Props = new HashMap<>();
    properties.put("category1", category1Props);
    category1Props.put("prop1", "val");

    hostGroups.add(group1);
    hostGroups.add(group2);
    group1Components.add("c1");
    group1Components.add("c2");

    group2Components.add("c1");
    group2Components.add("c3");

    expect(stack.isPasswordProperty("HDFS", "hdfs-site", "foo")).andReturn(false).anyTimes();
    expect(stack.isPasswordProperty("HDFS", "hdfs-site", "bar")).andReturn(false).anyTimes();
    expect(stack.isPasswordProperty("HDFS", "hdfs-site", "some_password")).andReturn(true).anyTimes();
    expect(stack.isPasswordProperty("HDFS", "category1", "prop1")).andReturn(false).anyTimes();
    expect(stack.isPasswordProperty("SERVICE2", "category2", "prop2")).andReturn(false).anyTimes();
    expect(stack.getServiceForComponent("c1")).andReturn("HDFS").anyTimes();
    expect(stack.getServiceForComponent("c2")).andReturn("HDFS").anyTimes();
    expect(stack.getServiceForComponent("c3")).andReturn("SERVICE2").anyTimes();
    expect(group1.getName()).andReturn("group1").anyTimes();
    expect(group2.getName()).andReturn("group2").anyTimes();
    expect(group1.getConfiguration()).andReturn(EMPTY_CONFIGURATION).anyTimes();
    expect(group1.getComponentNames()).andReturn(group1Components).anyTimes();
    expect(group2.getComponentNames()).andReturn(group2Components).anyTimes();

    Collection<Stack.ConfigProperty> requiredHDFSProperties = new HashSet<>();
    requiredHDFSProperties.add(new Stack.ConfigProperty("hdfs-site", "foo", null));
    requiredHDFSProperties.add(new Stack.ConfigProperty("hdfs-site", "bar", null));
    requiredHDFSProperties.add(new Stack.ConfigProperty("hdfs-site", "some_password", null));
    requiredHDFSProperties.add(new Stack.ConfigProperty("category1", "prop1", null));

    Collection<Stack.ConfigProperty> requiredService2Properties = new HashSet<>();
    requiredService2Properties.add(new Stack.ConfigProperty("category2", "prop2", null));
    expect(stack.getRequiredConfigurationProperties("HDFS")).andReturn(requiredHDFSProperties).anyTimes();
    expect(stack.getRequiredConfigurationProperties("SERVICE2")).andReturn(requiredService2Properties).anyTimes();

    serverConfig = setupConfigurationWithGPLLicense(true);
  }

  @Test
  public void testValidateConfigurations__basic_positive() throws Exception {
    expect(group1.getCardinality()).andReturn("1").atLeastOnce();
    expect(group1.getComponents()).andReturn(Arrays.asList(new Component("c1"), new Component("c2"))).atLeastOnce();
    expect(group2.getCardinality()).andReturn("1").atLeastOnce();
    expect(group2.getComponents()).andReturn(Arrays.asList(new Component("c1"), new Component("c3"))).atLeastOnce();
    expect(group2.getConfiguration()).andReturn(EMPTY_CONFIGURATION).atLeastOnce();

    replay(stack, group1, group2, serverConfig);

    Map<String, String> category2Props = new HashMap<>();
    properties.put("category2", category2Props);
    category2Props.put("prop2", "val");

    SecurityConfiguration securityConfiguration = SecurityConfiguration.withReference("testRef");
    Blueprint blueprint = new BlueprintImpl("test", hostGroups, stack, configuration, securityConfiguration);
    blueprint.validateRequiredProperties();
    BlueprintEntity entity = blueprint.toEntity();

    verify(stack, group1, group2, serverConfig);
    assertTrue(entity.getSecurityType() == SecurityType.KERBEROS);
    assertTrue(entity.getSecurityDescriptorReference().equals("testRef"));
  }

  @Test
  public void testValidateConfigurations__hostGroupConfig() throws Exception {
    Map<String, Map<String, String>> group2Props = new HashMap<>();
    Map<String, String> group2Category2Props = new HashMap<>();
    group2Props.put("category2", group2Category2Props);
    group2Category2Props.put("prop2", "val");

    // set config for group2 which contains a required property
    Configuration group2Configuration = new Configuration(group2Props, EMPTY_ATTRIBUTES, configuration);
    expect(group2.getConfiguration()).andReturn(group2Configuration).atLeastOnce();

    expect(group1.getCardinality()).andReturn("1").atLeastOnce();
    expect(group1.getComponents()).andReturn(Arrays.asList(new Component("NAMENODE"))).atLeastOnce();
    expect(group2.getCardinality()).andReturn("1").atLeastOnce();
    expect(group2.getComponents()).andReturn(Arrays.asList(new Component("NAMENODE"))).atLeastOnce();
    Map<String, String> category2Props = new HashMap<>();
    properties.put("category2", category2Props);
    category2Props.put("prop2", "val");
    group1Components.add("NAMENODE");
    group2Components.add("NAMENODE");
    Map<String, String> hdfsProps = new HashMap<>();
    properties.put("hdfs-site", hdfsProps);
    hdfsProps.put("foo", "val");
    hdfsProps.put("bar", "val");
    Map<String, String> hadoopProps = new HashMap<>();
    properties.put("hadoop-env", hadoopProps);
    hadoopProps.put("dfs_ha_initial_namenode_active", "%HOSTGROUP:group1%");
    hadoopProps.put("dfs_ha_initial_namenode_standby", "%HOSTGROUP:group2%");
    replay(stack, group1, group2, serverConfig);
    Blueprint blueprint = new BlueprintImpl("test", hostGroups, stack, configuration, null);
    blueprint.validateRequiredProperties();
    BlueprintEntity entity = blueprint.toEntity();
    verify(stack, group1, group2, serverConfig);
    assertTrue(entity.getSecurityType() == SecurityType.NONE);
    assertTrue(entity.getSecurityDescriptorReference() == null);
  }
  @Test
  public void testValidateConfigurations__hostGroupConfigForNameNodeHAPositive() throws Exception {
    Map<String, Map<String, String>> group2Props = new HashMap<>();
    Map<String, String> group2Category2Props = new HashMap<>();
    group2Props.put("category2", group2Category2Props);
    group2Category2Props.put("prop2", "val");
    // set config for group2 which contains a required property
    Configuration group2Configuration = new Configuration(group2Props, EMPTY_ATTRIBUTES, configuration);
    expect(group2.getConfiguration()).andReturn(group2Configuration).atLeastOnce();


    expect(group1.getCardinality()).andReturn("1").atLeastOnce();
    expect(group1.getComponents()).andReturn(Arrays.asList(new Component("NAMENODE"),new Component("ZKFC"))).atLeastOnce();
    expect(group2.getCardinality()).andReturn("1").atLeastOnce();
    expect(group2.getComponents()).andReturn(Arrays.asList(new Component("NAMENODE"),new Component("ZKFC"))).atLeastOnce();
    Map<String, String> category2Props = new HashMap<>();
    properties.put("category2", category2Props);
    category2Props.put("prop2", "val");
    group1Components.add("NAMENODE");
    group1Components.add("ZKFC");
    group2Components.add("NAMENODE");
    group2Components.add("ZKFC");
    Map<String, String> hdfsProps = new HashMap<>();
    properties.put("hdfs-site", hdfsProps);
    hdfsProps.put("foo", "val");
    hdfsProps.put("bar", "val");
    hdfsProps.put("dfs.nameservices", "val");
    Map<String, String> hadoopProps = new HashMap<>();
    properties.put("hadoop-env", hadoopProps);
    hadoopProps.put("dfs_ha_initial_namenode_active", "%HOSTGROUP::group1%");
    hadoopProps.put("dfs_ha_initial_namenode_standby", "%HOSTGROUP::group2%");
    replay(stack, group1, group2, serverConfig);

    Blueprint blueprint = new BlueprintImpl("test", hostGroups, stack, configuration, null);
    blueprint.validateRequiredProperties();
    BlueprintEntity entity = blueprint.toEntity();

    verify(stack, group1, group2, serverConfig);
    assertTrue(entity.getSecurityType() == SecurityType.NONE);
    assertTrue(entity.getSecurityDescriptorReference() == null);
  }

  @Test(expected= IllegalArgumentException.class)
  public void testValidateConfigurations__hostGroupConfigForNameNodeHAInCorrectHostGroups() throws Exception {
    Map<String, Map<String, String>> group2Props = new HashMap<>();
    Map<String, String> group2Category2Props = new HashMap<>();
    group2Props.put("category2", group2Category2Props);
    group2Category2Props.put("prop2", "val");
    // set config for group2 which contains a required property
    Configuration group2Configuration = new Configuration(group2Props, EMPTY_ATTRIBUTES, configuration);
    expect(group2.getConfiguration()).andReturn(group2Configuration).atLeastOnce();
    expect(group1.getCardinality()).andReturn("1").atLeastOnce();
    expect(group1.getComponents()).andReturn(Arrays.asList(new Component("NAMENODE"),new Component("ZKFC"))).atLeastOnce();
    expect(group2.getCardinality()).andReturn("1").atLeastOnce();
    expect(group2.getComponents()).andReturn(Arrays.asList(new Component("NAMENODE"),new Component("ZKFC"))).atLeastOnce();
    Map<String, String> category2Props = new HashMap<>();
    properties.put("category2", category2Props);
    category2Props.put("prop2", "val");
    group1Components.add("NAMENODE");
    group1Components.add("ZKFC");
    group2Components.add("NAMENODE");
    group2Components.add("ZKFC");
    expect(stack.getServiceForComponent("NAMENODE")).andReturn("SERVICE2").atLeastOnce();
    expect(stack.getServiceForComponent("ZKFC")).andReturn("SERVICE2").atLeastOnce();
    Map<String, String> hdfsProps = new HashMap<>();
    properties.put("hdfs-site", hdfsProps);
    hdfsProps.put("foo", "val");
    hdfsProps.put("bar", "val");
    hdfsProps.put("dfs.nameservices", "val");
    Map<String, String> hadoopProps = new HashMap<>();
    properties.put("hadoop-env", hadoopProps);
    hadoopProps.put("dfs_ha_initial_namenode_active", "%HOSTGROUP::group2%");
    hadoopProps.put("dfs_ha_initial_namenode_standby", "%HOSTGROUP::group3%");
    replay(stack, group1, group2, serverConfig);
    Blueprint blueprint = new BlueprintImpl("test", hostGroups, stack, configuration, null);
    blueprint.validateRequiredProperties();
    verify(stack, group1, group2, serverConfig);
  }
  @Test(expected= IllegalArgumentException.class)
  public void testValidateConfigurations__hostGroupConfigForNameNodeHAMappedSameHostGroup() throws Exception {
    Map<String, Map<String, String>> group2Props = new HashMap<>();
    Map<String, String> group2Category2Props = new HashMap<>();
    group2Props.put("category2", group2Category2Props);
    group2Category2Props.put("prop2", "val");
    // set config for group2 which contains a required property
    Configuration group2Configuration = new Configuration(group2Props, EMPTY_ATTRIBUTES, configuration);
    expect(group2.getConfiguration()).andReturn(group2Configuration).atLeastOnce();
    expect(group1.getCardinality()).andReturn("1").atLeastOnce();
    expect(group1.getComponents()).andReturn(Arrays.asList(new Component("NAMENODE"),new Component("ZKFC"))).atLeastOnce();
    expect(group2.getCardinality()).andReturn("1").atLeastOnce();
    expect(group2.getComponents()).andReturn(Arrays.asList(new Component("NAMENODE"),new Component("ZKFC"))).atLeastOnce();
    Map<String, String> category2Props = new HashMap<>();
    properties.put("category2", category2Props);
    category2Props.put("prop2", "val");
    group1Components.add("NAMENODE");
    group1Components.add("ZKFC");
    group2Components.add("NAMENODE");
    group2Components.add("ZKFC");
    expect(stack.getServiceForComponent("NAMENODE")).andReturn("SERVICE2").atLeastOnce();
    expect(stack.getServiceForComponent("ZKFC")).andReturn("SERVICE2").atLeastOnce();
    Map<String, String> hdfsProps = new HashMap<>();
    properties.put("hdfs-site", hdfsProps);
    hdfsProps.put("foo", "val");
    hdfsProps.put("bar", "val");
    hdfsProps.put("dfs.nameservices", "val");
    Map<String, String> hadoopProps = new HashMap<>();
    properties.put("hadoop-env", hadoopProps);
    hadoopProps.put("dfs_ha_initial_namenode_active", "%HOSTGROUP::group2%");
    hadoopProps.put("dfs_ha_initial_namenode_standby", "%HOSTGROUP::group2%");
    replay(stack, group1, group2, serverConfig);
    Blueprint blueprint = new BlueprintImpl("test", hostGroups, stack, configuration, null);
    blueprint.validateRequiredProperties();
    verify(stack, group1, group2, serverConfig);
  }
  @Test(expected = InvalidTopologyException.class)
  public void testValidateConfigurations__secretReference() throws InvalidTopologyException,
      GPLLicenseNotAcceptedException, NoSuchFieldException, IllegalAccessException {
    Map<String, Map<String, String>> group2Props = new HashMap<>();
    Map<String, String> group2Category2Props = new HashMap<>();

    group2Props.put("category2", group2Category2Props);
    group2Category2Props.put("prop2", "val");
    hdfsProps.put("secret", "SECRET:hdfs-site:1:test");
    replay(stack, group1, group2, serverConfig);

    Blueprint blueprint = new BlueprintImpl("test", hostGroups, stack, configuration, null);
    blueprint.validateRequiredProperties();
    verify(stack, group1, group2, serverConfig);
  }

  @Test(expected = GPLLicenseNotAcceptedException.class)
  public void testValidateConfigurations__gplIsNotAllowedCodecsProperty() throws InvalidTopologyException,
      GPLLicenseNotAcceptedException, NoSuchFieldException, IllegalAccessException {
    Map<String, Map<String, String>> lzoProperties = new HashMap<>();
    lzoProperties.put("core-site", new HashMap<String, String>(){{
      put(BlueprintValidatorImpl.CODEC_CLASSES_PROPERTY_NAME, "OtherCodec, " + BlueprintValidatorImpl.LZO_CODEC_CLASS);
    }});
    Configuration lzoUsageConfiguration = new Configuration(lzoProperties, EMPTY_ATTRIBUTES, EMPTY_CONFIGURATION);

    serverConfig = setupConfigurationWithGPLLicense(false);
    replay(stack, group1, group2, serverConfig);

    Blueprint blueprint = new BlueprintImpl("test", hostGroups, stack, lzoUsageConfiguration, null);
    blueprint.validateRequiredProperties();
    verify(stack, group1, group2, serverConfig);
  }

  @Test(expected = GPLLicenseNotAcceptedException.class)
  public void testValidateConfigurations__gplIsNotAllowedLZOProperty() throws InvalidTopologyException,
      GPLLicenseNotAcceptedException, NoSuchFieldException, IllegalAccessException {
    Map<String, Map<String, String>> lzoProperties = new HashMap<>();
    lzoProperties.put("core-site", new HashMap<String, String>(){{
      put(BlueprintValidatorImpl.LZO_CODEC_CLASS_PROPERTY_NAME, BlueprintValidatorImpl.LZO_CODEC_CLASS);
    }});
    Configuration lzoUsageConfiguration = new Configuration(lzoProperties, EMPTY_ATTRIBUTES, EMPTY_CONFIGURATION);

    serverConfig = setupConfigurationWithGPLLicense(false);
    replay(stack, group1, group2, serverConfig);

    Blueprint blueprint = new BlueprintImpl("test", hostGroups, stack, lzoUsageConfiguration, null);
    blueprint.validateRequiredProperties();
    verify(stack, group1, group2, serverConfig);
  }

  @Test
  public void testValidateConfigurations__gplISAllowed() throws InvalidTopologyException,
      GPLLicenseNotAcceptedException, NoSuchFieldException, IllegalAccessException {
    Map<String, Map<String, String>> lzoProperties = new HashMap<>();
    lzoProperties.put("core-site", new HashMap<String, String>(){{
      put(BlueprintValidatorImpl.LZO_CODEC_CLASS_PROPERTY_NAME, BlueprintValidatorImpl.LZO_CODEC_CLASS);
      put(BlueprintValidatorImpl.CODEC_CLASSES_PROPERTY_NAME, "OtherCodec, " + BlueprintValidatorImpl.LZO_CODEC_CLASS);
    }});
    Configuration lzoUsageConfiguration = new Configuration(lzoProperties, EMPTY_ATTRIBUTES, EMPTY_CONFIGURATION);

    expect(group2.getConfiguration()).andReturn(EMPTY_CONFIGURATION).atLeastOnce();
    replay(stack, group1, group2, serverConfig);

    Blueprint blueprint = new BlueprintImpl("test", hostGroups, stack, lzoUsageConfiguration, null);
    blueprint.validateRequiredProperties();
    verify(stack, group1, group2, serverConfig);
  }

  @Test
  public void testAutoSkipFailureEnabled() {
    Blueprint blueprint = new BlueprintImpl("test", hostGroups, stack, configuration, null, setting);
    HashMap<String, String> skipFailureSetting = new HashMap<>();
    skipFailureSetting.put(Setting.SETTING_NAME_SKIP_FAILURE, "true");
    expect(setting.getSettingValue(Setting.SETTING_NAME_DEPLOYMENT_SETTINGS)).andReturn(Collections.singleton(skipFailureSetting));
    replay(stack, setting);

    assertTrue(blueprint.shouldSkipFailure());
    verify(stack, setting);
  }

  @Test
  public void testAutoSkipFailureDisabled() {
    Blueprint blueprint = new BlueprintImpl("test", hostGroups, stack, configuration, null, setting);
    HashMap<String, String> skipFailureSetting = new HashMap<>();
    skipFailureSetting.put(Setting.SETTING_NAME_SKIP_FAILURE, "false");
    expect(setting.getSettingValue(Setting.SETTING_NAME_DEPLOYMENT_SETTINGS)).andReturn(Collections.singleton(skipFailureSetting));
    replay(stack, setting);

    assertFalse(blueprint.shouldSkipFailure());
    verify(stack, setting);
  }

  public static org.apache.ambari.server.configuration.Configuration setupConfigurationWithGPLLicense(boolean isGPLAllowed)
      throws NoSuchFieldException, IllegalAccessException {
    org.apache.ambari.server.configuration.Configuration serverConfig =
        mock(org.apache.ambari.server.configuration.Configuration.class);
    expect(serverConfig.getGplLicenseAccepted()).andReturn(isGPLAllowed).atLeastOnce();

    Field field = BlueprintValidatorImpl.class.getDeclaredField("configuration");
    field.setAccessible(true);
    field.set(null, serverConfig);
    return serverConfig;
  }

  //todo: ensure coverage for these existing tests

  //  private void validateEntity(BlueprintEntity entity, boolean containsConfig) {
//    assertEquals(BLUEPRINT_NAME, entity.getBlueprintName());
//
//    StackEntity stackEntity = entity.getStack();
//    assertEquals("test-stack-name", stackEntity.getStackName());
//    assertEquals("test-stack-version", stackEntity.getStackVersion());
//
//    Collection<HostGroupEntity> hostGroupEntities = entity.getHostGroups();
//
//    assertEquals(2, hostGroupEntities.size());
//    for (HostGroupEntity hostGroup : hostGroupEntities) {
//      assertEquals(BLUEPRINT_NAME, hostGroup.getBlueprintName());
//      assertNotNull(hostGroup.getBlueprintEntity());
//      Collection<HostGroupComponentEntity> componentEntities = hostGroup.getComponents();
//      if (hostGroup.getName().equals("group1")) {
//        assertEquals("1", hostGroup.getCardinality());
//        assertEquals(2, componentEntities.size());
//        Iterator<HostGroupComponentEntity> componentIterator = componentEntities.iterator();
//        String name = componentIterator.next().getName();
//        assertTrue(name.equals("component1") || name.equals("component2"));
//        String name2 = componentIterator.next().getName();
//        assertFalse(name.equals(name2));
//        assertTrue(name2.equals("component1") || name2.equals("component2"));
//      } else if (hostGroup.getName().equals("group2")) {
//        assertEquals("2", hostGroup.getCardinality());
//        assertEquals(1, componentEntities.size());
//        HostGroupComponentEntity componentEntity = componentEntities.iterator().next();
//        assertEquals("component1", componentEntity.getName());
//
//        if (containsConfig) {
//          Collection<HostGroupConfigEntity> configurations = hostGroup.getConfigurations();
//          assertEquals(1, configurations.size());
//          HostGroupConfigEntity hostGroupConfigEntity = configurations.iterator().next();
//          assertEquals(BLUEPRINT_NAME, hostGroupConfigEntity.getBlueprintName());
//          assertSame(hostGroup, hostGroupConfigEntity.getHostGroupEntity());
//          assertEquals("core-site", hostGroupConfigEntity.getType());
//          Map<String, String> properties = gson.<Map<String, String>>fromJson(
//              hostGroupConfigEntity.getConfigData(), Map.class);
//          assertEquals(1, properties.size());
//          assertEquals("anything", properties.get("my.custom.hg.property"));
//        }
//      } else {
//        fail("Unexpected host group name");
//      }
//    }
//    Collection<BlueprintConfigEntity> configurations = entity.getConfigurations();
//    if (containsConfig) {
//      assertEquals(1, configurations.size());
//      BlueprintConfigEntity blueprintConfigEntity = configurations.iterator().next();
//      assertEquals(BLUEPRINT_NAME, blueprintConfigEntity.getBlueprintName());
//      assertSame(entity, blueprintConfigEntity.getBlueprintEntity());
//      assertEquals("core-site", blueprintConfigEntity.getType());
//      Map<String, String> properties = gson.<Map<String, String>>fromJson(
//          blueprintConfigEntity.getConfigData(), Map.class);
//      assertEquals(2, properties.size());
//      assertEquals("480", properties.get("fs.trash.interval"));
//      assertEquals("8500", properties.get("ipc.client.idlethreshold"));
//    } else {
//      assertEquals(0, configurations.size());
//    }
//  }



  //  @Test
//  public void testCreateResource_Validate__Cardinality__ExternalComponent() throws Exception {
//
//    Set<Map<String, Object>> setProperties = getTestProperties();
//    setConfigurationProperties(setProperties);
//    ((Set<Map<String, String>>) setProperties.iterator().next().get("configurations")).
//        add(Collections.singletonMap("global/hive_database", "Existing MySQL Database"));
//
//    Iterator iter = ((HashSet<Map<String, HashSet<Map<String, String>>>>) setProperties.iterator().next().
//        get(BlueprintResourceProvider.HOST_GROUP_PROPERTY_ID)).
//        iterator().next().get("components").iterator();
//    iter.next();
//    iter.remove();
//
//    AmbariManagementController managementController = createMock(AmbariManagementController.class);
//    Capture<Set<StackServiceRequest>> stackServiceRequestCapture = EasyMock.newCapture();
//    Capture<Set<StackServiceComponentRequest>> serviceComponentRequestCapture = EasyMock.newCapture();
//    Capture<StackConfigurationRequest> stackConfigurationRequestCapture = EasyMock.newCapture();
//    Capture<StackLevelConfigurationRequest> stackLevelConfigurationRequestCapture = EasyMock.newCapture();
//    Request request = createMock(Request.class);
//    StackServiceResponse stackServiceResponse = createMock(StackServiceResponse.class);
//    StackServiceComponentResponse stackServiceComponentResponse = createNiceMock(StackServiceComponentResponse.class);
//    StackServiceComponentResponse stackServiceComponentResponse2 = createNiceMock(StackServiceComponentResponse.class);
//    Set<StackServiceComponentResponse> setServiceComponents = new HashSet<StackServiceComponentResponse>();
//    setServiceComponents.add(stackServiceComponentResponse);
//    setServiceComponents.add(stackServiceComponentResponse2);
//
//    Map<String, ServiceInfo> services = new HashMap<String, ServiceInfo>();
//    ServiceInfo service = new ServiceInfo();
//    service.setName("test-service");
//    services.put("test-service", service);
//
//    List<ComponentInfo> serviceComponents = new ArrayList<ComponentInfo>();
//    ComponentInfo component1 = new ComponentInfo();
//    component1.setName("component1");
//    ComponentInfo component2 = new ComponentInfo();
//    component2.setName("MYSQL_SERVER");
//    serviceComponents.add(component1);
//    serviceComponents.add(component2);
//
//    Capture<BlueprintEntity> entityCapture = EasyMock.newCapture();
//
//    // set expectations
//    expect(blueprintFactory.createBlueprint(setProperties.iterator().next())).andReturn(blueprint).once();
//    expect(blueprint.validateRequiredProperties()).andReturn(Collections.<String, Map<String, Collection<String>>>emptyMap()).once();
//    expect(blueprint.toEntity()).andReturn(entity);
//    expect(blueprint.getName()).andReturn(BLUEPRINT_NAME).atLeastOnce();
//    expect(managementController.getStackServices(capture(stackServiceRequestCapture))).andReturn(
//        Collections.<StackServiceResponse>singleton(stackServiceResponse));
//    expect(stackServiceResponse.getServiceName()).andReturn("test-service").anyTimes();
//    expect(stackServiceResponse.getStackName()).andReturn("test-stack-name").anyTimes();
//    expect(stackServiceResponse.getStackVersion()).andReturn("test-stack-version").anyTimes();
//    expect(stackServiceResponse.getExcludedConfigTypes()).andReturn(Collections.<String>emptySet());
//
//    expect(managementController.getStackComponents(capture(serviceComponentRequestCapture))).andReturn(setServiceComponents).anyTimes();
//    expect(stackServiceComponentResponse.getCardinality()).andReturn("2").anyTimes();
//    expect(stackServiceComponentResponse.getComponentName()).andReturn("component1").anyTimes();
//    expect(stackServiceComponentResponse.getServiceName()).andReturn("test-service").anyTimes();
//    expect(stackServiceComponentResponse.getStackName()).andReturn("test-stack-name").anyTimes();
//    expect(stackServiceComponentResponse.getStackVersion()).andReturn("test-stack-version").anyTimes();
//    expect(stackServiceComponentResponse2.getCardinality()).andReturn("1").anyTimes();
//    expect(stackServiceComponentResponse2.getComponentName()).andReturn("MYSQL_SERVER").anyTimes();
//    expect(stackServiceComponentResponse2.getServiceName()).andReturn("test-service").anyTimes();
//    expect(stackServiceComponentResponse2.getStackName()).andReturn("test-stack-name").anyTimes();
//    expect(stackServiceComponentResponse2.getStackVersion()).andReturn("test-stack-version").anyTimes();
//
//    expect(managementController.getStackConfigurations(Collections.singleton(capture(stackConfigurationRequestCapture)))).
//        andReturn(Collections.<StackConfigurationResponse>emptySet());
//    expect(managementController.getStackLevelConfigurations(Collections.singleton(capture(stackLevelConfigurationRequestCapture)))).
//    andReturn(Collections.<StackConfigurationResponse>emptySet());
//
//    expect(metaInfo.getComponentDependencies("test-stack-name", "test-stack-version", "test-service", "MYSQL_SERVER")).
//        andReturn(Collections.<DependencyInfo>emptyList()).anyTimes();
//    expect(metaInfo.getComponentDependencies("test-stack-name", "test-stack-version", "test-service", "component1")).
//        andReturn(Collections.<DependencyInfo>emptyList()).anyTimes();
//
//    expect(request.getProperties()).andReturn(setProperties);
//    expect(request.getRequestInfoProperties()).andReturn(Collections.<String, String>emptyMap());
//    expect(dao.findByName(BLUEPRINT_NAME)).andReturn(null);
//    expect(metaInfo.getServices("test-stack-name", "test-stack-version")).andReturn(services).anyTimes();
//    expect(metaInfo.getComponentsByService("test-stack-name", "test-stack-version", "test-service")).
//        andReturn(serviceComponents).anyTimes();
//    expect(metaInfo.getComponentToService("test-stack-name", "test-stack-version", "component1")).
//        andReturn("test-service").anyTimes();
//    expect(metaInfo.getComponentToService("test-stack-name", "test-stack-version", "component2")).
//        andReturn("test-service").anyTimes();
//    expect(metaInfo.getService("test-stack-name", "test-stack-version", "test-service")).andReturn(service).anyTimes();
//    dao.create(capture(entityCapture));
//
//    replay(dao, metaInfo, request, managementController, stackServiceResponse,
//        stackServiceComponentResponse, stackServiceComponentResponse2);
//    // end expectations
//
//    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
//        Resource.Type.Blueprint,
//        PropertyHelper.getPropertyIds(Resource.Type.Blueprint),
//        PropertyHelper.getKeyPropertyIds(Resource.Type.Blueprint),
//        managementController);
//
//    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();
//    ((ObservableResourceProvider)provider).addObserver(observer);
//
//    provider.createResources(request);
//
//    ResourceProviderEvent lastEvent = observer.getLastEvent();
//    assertNotNull(lastEvent);
//    assertEquals(Resource.Type.Blueprint, lastEvent.getResourceType());
//    assertEquals(ResourceProviderEvent.Type.Create, lastEvent.getType());
//    assertEquals(request, lastEvent.getRequest());
//    assertNull(lastEvent.getPredicate());
//
//    verify(dao, metaInfo, request, managementController, stackServiceResponse,
//        stackServiceComponentResponse, stackServiceComponentResponse2);
//  }

//  @Test
//   public void testCreateResource_Validate__Cardinality__MultipleDependencyInstances() throws AmbariException, ResourceAlreadyExistsException,
//      SystemException, UnsupportedPropertyException, NoSuchParentResourceException {
//
//    Set<Map<String, Object>> setProperties = getTestProperties();
//    setConfigurationProperties(setProperties);
//
//    AmbariManagementController managementController = createMock(AmbariManagementController.class);
//    Capture<Set<StackServiceRequest>> stackServiceRequestCapture = EasyMock.newCapture();
//    Capture<Set<StackServiceComponentRequest>> serviceComponentRequestCapture = EasyMock.newCapture();
//    Capture<StackConfigurationRequest> stackConfigurationRequestCapture = EasyMock.newCapture();
//    Capture<StackLevelConfigurationRequest> stackLevelConfigurationRequestCapture = EasyMock.newCapture();
//    Request request = createMock(Request.class);
//    StackServiceResponse stackServiceResponse = createMock(StackServiceResponse.class);
//    StackServiceComponentResponse stackServiceComponentResponse = createNiceMock(StackServiceComponentResponse.class);
//    StackServiceComponentResponse stackServiceComponentResponse2 = createNiceMock(StackServiceComponentResponse.class);
//    Set<StackServiceComponentResponse> setServiceComponents = new HashSet<StackServiceComponentResponse>();
//    setServiceComponents.add(stackServiceComponentResponse);
//    setServiceComponents.add(stackServiceComponentResponse2);
//
//    DependencyInfo dependencyInfo = new DependencyInfo();
//    AutoDeployInfo autoDeployInfo = new AutoDeployInfo();
//    autoDeployInfo.setEnabled(false);
//    dependencyInfo.setAutoDeploy(autoDeployInfo);
//    dependencyInfo.setScope("cluster");
//    dependencyInfo.setName("test-service/component1");
//
//    Map<String, ServiceInfo> services = new HashMap<String, ServiceInfo>();
//    ServiceInfo service = new ServiceInfo();
//    service.setName("test-service");
//    services.put("test-service", service);
//
//    List<ComponentInfo> serviceComponents = new ArrayList<ComponentInfo>();
//    ComponentInfo component1 = new ComponentInfo();
//    component1.setName("component1");
//    ComponentInfo component2 = new ComponentInfo();
//    component2.setName("component2");
//    serviceComponents.add(component1);
//    serviceComponents.add(component2);
//
//    Capture<BlueprintEntity> entityCapture = EasyMock.newCapture();
//
//    // set expectations
//    expect(managementController.getStackServices(capture(stackServiceRequestCapture))).andReturn(
//        Collections.<StackServiceResponse>singleton(stackServiceResponse));
//    expect(stackServiceResponse.getServiceName()).andReturn("test-service").anyTimes();
//    expect(stackServiceResponse.getStackName()).andReturn("test-stack-name").anyTimes();
//    expect(stackServiceResponse.getStackVersion()).andReturn("test-stack-version").anyTimes();
//    expect(stackServiceResponse.getExcludedConfigTypes()).andReturn(Collections.<String>emptySet());
//
//    expect(managementController.getStackComponents(capture(serviceComponentRequestCapture))).andReturn(setServiceComponents).anyTimes();
//    expect(stackServiceComponentResponse.getCardinality()).andReturn("2").anyTimes();
//    expect(stackServiceComponentResponse.getComponentName()).andReturn("component1").anyTimes();
//    expect(stackServiceComponentResponse.getServiceName()).andReturn("test-service").anyTimes();
//    expect(stackServiceComponentResponse.getStackName()).andReturn("test-stack-name").anyTimes();
//    expect(stackServiceComponentResponse.getStackVersion()).andReturn("test-stack-version").anyTimes();
//    expect(stackServiceComponentResponse2.getCardinality()).andReturn("1").anyTimes();
//    expect(stackServiceComponentResponse2.getComponentName()).andReturn("component2").anyTimes();
//    expect(stackServiceComponentResponse2.getServiceName()).andReturn("test-service").anyTimes();
//    expect(stackServiceComponentResponse2.getStackName()).andReturn("test-stack-name").anyTimes();
//    expect(stackServiceComponentResponse2.getStackVersion()).andReturn("test-stack-version").anyTimes();
//
//    expect(managementController.getStackConfigurations(Collections.singleton(capture(stackConfigurationRequestCapture)))).
//        andReturn(Collections.<StackConfigurationResponse>emptySet());
//    expect(managementController.getStackLevelConfigurations(Collections.singleton(capture(stackLevelConfigurationRequestCapture)))).
//        andReturn(Collections.<StackConfigurationResponse>emptySet());
//
//    expect(metaInfo.getComponentDependencies("test-stack-name", "test-stack-version", "test-service", "component2")).
//        andReturn(Collections.<DependencyInfo>singletonList(dependencyInfo)).anyTimes();
//    expect(metaInfo.getComponentDependencies("test-stack-name", "test-stack-version", "test-service", "component1")).
//        andReturn(Collections.<DependencyInfo>emptyList()).anyTimes();
//
//    expect(request.getProperties()).andReturn(setProperties);
//    expect(request.getRequestInfoProperties()).andReturn(Collections.<String, String>emptyMap());
//    expect(dao.findByName(BLUEPRINT_NAME)).andReturn(null);
//    expect(metaInfo.getServices("test-stack-name", "test-stack-version")).andReturn(services).anyTimes();
//    expect(metaInfo.getComponentsByService("test-stack-name", "test-stack-version", "test-service")).
//        andReturn(serviceComponents).anyTimes();
//    expect(metaInfo.getComponentToService("test-stack-name", "test-stack-version", "component1")).
//        andReturn("test-service").anyTimes();
//    expect(metaInfo.getComponentToService("test-stack-name", "test-stack-version", "component2")).
//        andReturn("test-service").anyTimes();
//    expect(metaInfo.getService("test-stack-name", "test-stack-version", "test-service")).andReturn(service).anyTimes();
//    dao.create(capture(entityCapture));
//
//    replay(dao, metaInfo, request, managementController, stackServiceResponse,
//        stackServiceComponentResponse, stackServiceComponentResponse2);
//    // end expectations
//
//    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
//        Resource.Type.Blueprint,
//        PropertyHelper.getPropertyIds(Resource.Type.Blueprint),
//        PropertyHelper.getKeyPropertyIds(Resource.Type.Blueprint),
//        managementController);
//
//    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();
//    ((ObservableResourceProvider)provider).addObserver(observer);
//
//    provider.createResources(request);
//
//    ResourceProviderEvent lastEvent = observer.getLastEvent();
//    assertNotNull(lastEvent);
//    assertEquals(Resource.Type.Blueprint, lastEvent.getResourceType());
//    assertEquals(ResourceProviderEvent.Type.Create, lastEvent.getType());
//    assertEquals(request, lastEvent.getRequest());
//    assertNull(lastEvent.getPredicate());
//
//    verify(dao, metaInfo, request, managementController, stackServiceResponse,
//        stackServiceComponentResponse, stackServiceComponentResponse2);
//  }

//  @Test
//  public void testCreateResource_Validate__Cardinality__AutoCommit() throws AmbariException, ResourceAlreadyExistsException,
//      SystemException, UnsupportedPropertyException, NoSuchParentResourceException {
//
//    Set<Map<String, Object>> setProperties = getTestProperties();
//    setConfigurationProperties(setProperties);
//
//    // remove component2 from BP
//    Iterator iter = ((HashSet<Map<String, HashSet<Map<String, String>>>>) setProperties.iterator().next().
//        get(BlueprintResourceProvider.HOST_GROUP_PROPERTY_ID)).
//        iterator().next().get("components").iterator();
//    iter.next();
//    iter.remove();
//
//    AmbariManagementController managementController = createMock(AmbariManagementController.class);
//    Capture<Set<StackServiceRequest>> stackServiceRequestCapture = EasyMock.newCapture();
//    Capture<Set<StackServiceComponentRequest>> serviceComponentRequestCapture = EasyMock.newCapture();
//    Capture<StackConfigurationRequest> stackConfigurationRequestCapture = EasyMock.newCapture();
//    Capture<StackLevelConfigurationRequest> stackLevelConfigurationRequestCapture = EasyMock.newCapture();
//    Request request = createMock(Request.class);
//    StackServiceResponse stackServiceResponse = createMock(StackServiceResponse.class);
//    StackServiceComponentResponse stackServiceComponentResponse = createNiceMock(StackServiceComponentResponse.class);
//    StackServiceComponentResponse stackServiceComponentResponse2 = createNiceMock(StackServiceComponentResponse.class);
//    Set<StackServiceComponentResponse> setServiceComponents = new HashSet<StackServiceComponentResponse>();
//    setServiceComponents.add(stackServiceComponentResponse);
//    setServiceComponents.add(stackServiceComponentResponse2);
//
//    DependencyInfo dependencyInfo = new DependencyInfo();
//    AutoDeployInfo autoDeployInfo = new AutoDeployInfo();
//    autoDeployInfo.setEnabled(true);
//    autoDeployInfo.setCoLocate("test-service/component1");
//    dependencyInfo.setAutoDeploy(autoDeployInfo);
//    dependencyInfo.setScope("cluster");
//    dependencyInfo.setName("test-service/component2");
//
//    Map<String, ServiceInfo> services = new HashMap<String, ServiceInfo>();
//    ServiceInfo service = new ServiceInfo();
//    service.setName("test-service");
//    services.put("test-service", service);
//
//    List<ComponentInfo> serviceComponents = new ArrayList<ComponentInfo>();
//    ComponentInfo component1 = new ComponentInfo();
//    component1.setName("component1");
//    ComponentInfo component2 = new ComponentInfo();
//    component2.setName("component2");
//    serviceComponents.add(component1);
//    serviceComponents.add(component2);
//
//    Capture<BlueprintEntity> entityCapture = EasyMock.newCapture();
//
//    // set expectations
//    expect(managementController.getStackServices(capture(stackServiceRequestCapture))).andReturn(
//        Collections.<StackServiceResponse>singleton(stackServiceResponse));
//    expect(stackServiceResponse.getServiceName()).andReturn("test-service").anyTimes();
//    expect(stackServiceResponse.getStackName()).andReturn("test-stack-name").anyTimes();
//    expect(stackServiceResponse.getStackVersion()).andReturn("test-stack-version").anyTimes();
//    expect(stackServiceResponse.getExcludedConfigTypes()).andReturn(Collections.<String>emptySet());
//
//    expect(managementController.getStackComponents(capture(serviceComponentRequestCapture))).andReturn(setServiceComponents).anyTimes();
//    expect(stackServiceComponentResponse.getCardinality()).andReturn("2").anyTimes();
//    expect(stackServiceComponentResponse.getComponentName()).andReturn("component1").anyTimes();
//    expect(stackServiceComponentResponse.getServiceName()).andReturn("test-service").anyTimes();
//    expect(stackServiceComponentResponse.getStackName()).andReturn("test-stack-name").anyTimes();
//    expect(stackServiceComponentResponse.getStackVersion()).andReturn("test-stack-version").anyTimes();
//    expect(stackServiceComponentResponse2.getCardinality()).andReturn("1").anyTimes();
//    expect(stackServiceComponentResponse2.getComponentName()).andReturn("component2").anyTimes();
//    expect(stackServiceComponentResponse2.getServiceName()).andReturn("test-service").anyTimes();
//    expect(stackServiceComponentResponse2.getStackName()).andReturn("test-stack-name").anyTimes();
//    expect(stackServiceComponentResponse2.getStackVersion()).andReturn("test-stack-version").anyTimes();
//
//    expect(managementController.getStackConfigurations(Collections.singleton(capture(stackConfigurationRequestCapture)))).
//        andReturn(Collections.<StackConfigurationResponse>emptySet());
//    expect(managementController.getStackLevelConfigurations(Collections.singleton(capture(stackLevelConfigurationRequestCapture)))).
//        andReturn(Collections.<StackConfigurationResponse>emptySet());
//
//    expect(metaInfo.getComponentDependencies("test-stack-name", "test-stack-version", "test-service", "component2")).
//        andReturn(Collections.<DependencyInfo>emptyList()).anyTimes();
//    expect(metaInfo.getComponentDependencies("test-stack-name", "test-stack-version", "test-service", "component1")).
//        andReturn(Collections.<DependencyInfo>singletonList(dependencyInfo)).anyTimes();
//
//    expect(request.getProperties()).andReturn(setProperties);
//    expect(request.getRequestInfoProperties()).andReturn(Collections.<String, String>emptyMap());
//    expect(dao.findByName(BLUEPRINT_NAME)).andReturn(null);
//    expect(metaInfo.getServices("test-stack-name", "test-stack-version")).andReturn(services).anyTimes();
//    expect(metaInfo.getComponentsByService("test-stack-name", "test-stack-version", "test-service")).
//        andReturn(serviceComponents).anyTimes();
//    expect(metaInfo.getComponentToService("test-stack-name", "test-stack-version", "component1")).
//        andReturn("test-service").anyTimes();
//    expect(metaInfo.getComponentToService("test-stack-name", "test-stack-version", "component2")).
//        andReturn("test-service").anyTimes();
//    expect(metaInfo.getService("test-stack-name", "test-stack-version", "test-service")).andReturn(service).anyTimes();
//    dao.create(capture(entityCapture));
//
//    replay(dao, metaInfo, request, managementController, stackServiceResponse,
//        stackServiceComponentResponse, stackServiceComponentResponse2);
//    // end expectations
//
//    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
//        Resource.Type.Blueprint,
//        PropertyHelper.getPropertyIds(Resource.Type.Blueprint),
//        PropertyHelper.getKeyPropertyIds(Resource.Type.Blueprint),
//        managementController);
//
//    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();
//    ((ObservableResourceProvider)provider).addObserver(observer);
//
//    provider.createResources(request);
//
//    ResourceProviderEvent lastEvent = observer.getLastEvent();
//    assertNotNull(lastEvent);
//    assertEquals(Resource.Type.Blueprint, lastEvent.getResourceType());
//    assertEquals(ResourceProviderEvent.Type.Create, lastEvent.getType());
//    assertEquals(request, lastEvent.getRequest());
//    assertNull(lastEvent.getPredicate());
//
//    verify(dao, metaInfo, request, managementController, stackServiceResponse,
//        stackServiceComponentResponse, stackServiceComponentResponse2);
//  }

//  @Test
//  public void testCreateResource_Validate__Cardinality__Fail() throws AmbariException, ResourceAlreadyExistsException,
//      SystemException, UnsupportedPropertyException, NoSuchParentResourceException {
//
//    Set<Map<String, Object>> setProperties = getTestProperties();
//    setConfigurationProperties(setProperties);
//
//    Iterator iter = ((HashSet<Map<String, HashSet<Map<String, String>>>>) setProperties.iterator().next().
//        get(BlueprintResourceProvider.HOST_GROUP_PROPERTY_ID)).
//        iterator().next().get("components").iterator();
//    iter.next();
//    iter.remove();
//
//    AmbariManagementController managementController = createMock(AmbariManagementController.class);
//    Capture<Set<StackServiceRequest>> stackServiceRequestCapture = EasyMock.newCapture();
//    Capture<Set<StackServiceComponentRequest>> serviceComponentRequestCapture = EasyMock.newCapture();
//    Capture<StackConfigurationRequest> stackConfigurationRequestCapture = EasyMock.newCapture();
//    Capture<StackLevelConfigurationRequest> stackLevelConfigurationRequestCapture = EasyMock.newCapture();
//    Request request = createMock(Request.class);
//    StackServiceResponse stackServiceResponse = createMock(StackServiceResponse.class);
//    StackServiceComponentResponse stackServiceComponentResponse = createNiceMock(StackServiceComponentResponse.class);
//    StackServiceComponentResponse stackServiceComponentResponse2 = createNiceMock(StackServiceComponentResponse.class);
//    Set<StackServiceComponentResponse> setServiceComponents = new HashSet<StackServiceComponentResponse>();
//    setServiceComponents.add(stackServiceComponentResponse);
//    setServiceComponents.add(stackServiceComponentResponse2);
//
//    Map<String, ServiceInfo> services = new HashMap<String, ServiceInfo>();
//    ServiceInfo service = new ServiceInfo();
//    service.setName("test-service");
//    services.put("test-service", service);
//
//    List<ComponentInfo> serviceComponents = new ArrayList<ComponentInfo>();
//    ComponentInfo component1 = new ComponentInfo();
//    component1.setName("component1");
//    ComponentInfo component2 = new ComponentInfo();
//    component2.setName("MYSQL_SERVER");
//    serviceComponents.add(component1);
//    serviceComponents.add(component2);
//
//    // set expectations
//    expect(managementController.getStackServices(capture(stackServiceRequestCapture))).andReturn(
//        Collections.<StackServiceResponse>singleton(stackServiceResponse));
//    expect(stackServiceResponse.getServiceName()).andReturn("test-service").anyTimes();
//    expect(stackServiceResponse.getStackName()).andReturn("test-stack-name").anyTimes();
//    expect(stackServiceResponse.getStackVersion()).andReturn("test-stack-version").anyTimes();
//    expect(stackServiceResponse.getExcludedConfigTypes()).andReturn(Collections.<String>emptySet());
//
//    expect(managementController.getStackComponents(capture(serviceComponentRequestCapture))).andReturn(setServiceComponents).anyTimes();
//    expect(stackServiceComponentResponse.getCardinality()).andReturn("2").anyTimes();
//    expect(stackServiceComponentResponse.getComponentName()).andReturn("component1").anyTimes();
//    expect(stackServiceComponentResponse.getServiceName()).andReturn("test-service").anyTimes();
//    expect(stackServiceComponentResponse.getStackName()).andReturn("test-stack-name").anyTimes();
//    expect(stackServiceComponentResponse.getStackVersion()).andReturn("test-stack-version").anyTimes();
//    expect(stackServiceComponentResponse2.getCardinality()).andReturn("1").anyTimes();
//    expect(stackServiceComponentResponse2.getComponentName()).andReturn("MYSQL_SERVER").anyTimes();
//    expect(stackServiceComponentResponse2.getServiceName()).andReturn("test-service").anyTimes();
//    expect(stackServiceComponentResponse2.getStackName()).andReturn("test-stack-name").anyTimes();
//    expect(stackServiceComponentResponse2.getStackVersion()).andReturn("test-stack-version").anyTimes();
//
//    expect(managementController.getStackConfigurations(Collections.singleton(capture(stackConfigurationRequestCapture)))).
//        andReturn(Collections.<StackConfigurationResponse>emptySet());
//    expect(managementController.getStackLevelConfigurations(Collections.singleton(capture(stackLevelConfigurationRequestCapture)))).
//        andReturn(Collections.<StackConfigurationResponse>emptySet());
//
//    expect(metaInfo.getComponentDependencies("test-stack-name", "test-stack-version", "test-service", "MYSQL_SERVER")).
//        andReturn(Collections.<DependencyInfo>emptyList()).anyTimes();
//    expect(metaInfo.getComponentDependencies("test-stack-name", "test-stack-version", "test-service", "component1")).
//        andReturn(Collections.<DependencyInfo>emptyList()).anyTimes();
//
//    expect(request.getProperties()).andReturn(setProperties);
//    expect(request.getRequestInfoProperties()).andReturn(Collections.<String, String>emptyMap());
//    expect(dao.findByName(BLUEPRINT_NAME)).andReturn(null);
//    expect(metaInfo.getServices("test-stack-name", "test-stack-version")).andReturn(services).anyTimes();
//    expect(metaInfo.getComponentsByService("test-stack-name", "test-stack-version", "test-service")).
//        andReturn(serviceComponents).anyTimes();
//    expect(metaInfo.getComponentToService("test-stack-name", "test-stack-version", "component1")).
//        andReturn("test-service").anyTimes();
//    expect(metaInfo.getComponentToService("test-stack-name", "test-stack-version", "component2")).
//        andReturn("test-service").anyTimes();
//    expect(metaInfo.getService("test-stack-name", "test-stack-version", "test-service")).andReturn(service).anyTimes();
//
//    replay(dao, metaInfo, request, managementController, stackServiceResponse,
//        stackServiceComponentResponse, stackServiceComponentResponse2);
//    // end expectations
//
//    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
//        Resource.Type.Blueprint,
//        PropertyHelper.getPropertyIds(Resource.Type.Blueprint),
//        PropertyHelper.getKeyPropertyIds(Resource.Type.Blueprint),
//        managementController);
//
//    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();
//    ((ObservableResourceProvider)provider).addObserver(observer);
//
//    try {
//      provider.createResources(request);
//      fail("Expected validation failure for MYSQL_SERVER");
//    } catch (IllegalArgumentException e) {
//      // expected
//    }
//
//    verify(dao, metaInfo, request, managementController, stackServiceResponse,
//        stackServiceComponentResponse, stackServiceComponentResponse2);
//  }

//  @Test
//  public void testCreateResource_Validate__AmbariServerComponent() throws AmbariException, ResourceAlreadyExistsException,
//      SystemException, UnsupportedPropertyException, NoSuchParentResourceException
//  {
//    Request request = createMock(Request.class);
//    AmbariManagementController managementController = createMock(AmbariManagementController.class);
//    Capture<Set<StackServiceRequest>> stackServiceRequestCapture = EasyMock.newCapture();
//
//    Map<String, ServiceInfo> services = new HashMap<String, ServiceInfo>();
//    ServiceInfo service = new ServiceInfo();
//    service.setName("test-service");
//    services.put("test-service", service);
//
//    List<ComponentInfo> serviceComponents = new ArrayList<ComponentInfo>();
//    ComponentInfo component1 = new ComponentInfo();
//    component1.setName("component1");
//    ComponentInfo component2 = new ComponentInfo();
//    component2.setName("component2");
//    serviceComponents.add(component1);
//    serviceComponents.add(component2);
//
//
//    Set<Map<String, Object>> setProperties = getTestProperties();
//    ((HashSet<Map<String, String>>) ((HashSet<Map<String, Object>>) setProperties.iterator().next().get(
//        BlueprintResourceProvider.HOST_GROUP_PROPERTY_ID)).iterator().next().get("components")).
//        iterator().next().put("name", "AMBARI_SERVER");
//
//    Capture<BlueprintEntity> entityCapture = EasyMock.newCapture();
//
//    // set expectations
//    expect(managementController.getStackServices(capture(stackServiceRequestCapture))).andReturn(
//        Collections.<StackServiceResponse>emptySet());
//    expect(request.getProperties()).andReturn(setProperties);
//    expect(request.getRequestInfoProperties()).andReturn(Collections.<String, String>emptyMap());
//    expect(dao.findByName(BLUEPRINT_NAME)).andReturn(null);
//    expect(metaInfo.getServices("test-stack-name", "test-stack-version")).andReturn(services).anyTimes();
//    expect(metaInfo.getComponentsByService("test-stack-name", "test-stack-version", "test-service")).
//        andReturn(serviceComponents).anyTimes();
//    expect(metaInfo.getComponentToService("test-stack-name", "test-stack-version", "component1")).
//        andReturn("test-service").anyTimes();
//    expect(metaInfo.getService("test-stack-name", "test-stack-version", "test-service")).andReturn(service).anyTimes();
//
//    dao.create(capture(entityCapture));
//
//    replay(dao, metaInfo, request, managementController);
//    // end expectations
//
//    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
//        Resource.Type.Blueprint,
//        PropertyHelper.getPropertyIds(Resource.Type.Blueprint),
//        PropertyHelper.getKeyPropertyIds(Resource.Type.Blueprint),
//        managementController);
//
//    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();
//    ((ObservableResourceProvider)provider).addObserver(observer);
//
//    provider.createResources(request);
//
//    ResourceProviderEvent lastEvent = observer.getLastEvent();
//    assertNotNull(lastEvent);
//    assertEquals(Resource.Type.Blueprint, lastEvent.getResourceType());
//    assertEquals(ResourceProviderEvent.Type.Create, lastEvent.getType());
//    assertEquals(request, lastEvent.getRequest());
//    assertNull(lastEvent.getPredicate());
//
//    verify(dao, metaInfo, request, managementController);
//  }


}
