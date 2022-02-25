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

package org.apache.ambari.server.controller.internal;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createStrictMock;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.reset;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.security.encryption.CredentialStoreType;
import org.apache.ambari.server.state.quicklinksprofile.QuickLinksProfileBuilderTest;
import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.BlueprintFactory;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.apache.ambari.server.topology.InvalidTopologyTemplateException;
import org.apache.ambari.server.topology.TopologyRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.Sets;

/**
 * Unit tests for ProvisionClusterRequest.
 */
@SuppressWarnings("unchecked")
public class ProvisionClusterRequestTest {

  private static final String CLUSTER_NAME = "cluster_name";
  private static final String BLUEPRINT_NAME = "blueprint_name";

  private static final BlueprintFactory blueprintFactory = createStrictMock(BlueprintFactory.class);
  private static final Blueprint blueprint = createNiceMock(Blueprint.class);
  private static final ResourceProvider hostResourceProvider = createMock(ResourceProvider.class);
  private static final Configuration blueprintConfig = new Configuration(
      Collections.emptyMap(),
      Collections.emptyMap());

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    reset(blueprintFactory, blueprint, hostResourceProvider);
    ProvisionClusterRequest.init(blueprintFactory);
    // set host resource provider field
    Class clazz = BaseClusterRequest.class;
    Field f = clazz.getDeclaredField("hostResourceProvider");
    f.setAccessible(true);
    f.set(null, hostResourceProvider);

    expect(blueprintFactory.getBlueprint(BLUEPRINT_NAME)).andReturn(blueprint).once();
    expect(blueprint.getConfiguration()).andReturn(blueprintConfig).anyTimes();
    expect(hostResourceProvider.checkPropertyIds(Collections.singleton("Hosts/host_name"))).
        andReturn(Collections.emptySet()).once();

    replay(blueprintFactory, blueprint, hostResourceProvider);
  }

  @After
  public void tearDown() {
    verify(blueprintFactory, blueprint, hostResourceProvider);
  }

  @Test
  public void testHostNameSpecified() throws Exception {
    // reset host resource provider expectations to none since we are not specifying a host predicate
    reset(hostResourceProvider);
    replay(hostResourceProvider);
    Map<String, Object> properties = createBlueprintRequestPropertiesNameOnly(CLUSTER_NAME, BLUEPRINT_NAME);

    ProvisionClusterRequest provisionClusterRequest = new ProvisionClusterRequest(properties, null);

    assertEquals(CLUSTER_NAME, provisionClusterRequest.getClusterName());
    assertEquals(TopologyRequest.Type.PROVISION, provisionClusterRequest.getType());
    assertEquals(String.format("Provision Cluster '%s'", CLUSTER_NAME) , provisionClusterRequest.getDescription());
    assertSame(blueprint, provisionClusterRequest.getBlueprint());
    Map<String, HostGroupInfo> hostGroupInfo = provisionClusterRequest.getHostGroupInfo();
    assertEquals(1, hostGroupInfo.size());

    // group1
    // host info
    HostGroupInfo group1Info = hostGroupInfo.get("group1");
    assertEquals("group1", group1Info.getHostGroupName());
    assertEquals(1, group1Info.getHostNames().size());
    assertTrue(group1Info.getHostNames().contains("host1.mydomain.com"));
    assertEquals(1, group1Info.getRequestedHostCount());
    assertNull(group1Info.getPredicate());
    // configuration
    Configuration group1Configuration = group1Info.getConfiguration();
    assertNull(group1Configuration.getParentConfiguration());
    assertEquals(1, group1Configuration.getProperties().size());
    Map<String, String> group1TypeProperties = group1Configuration.getProperties().get("foo-type");
    assertEquals(2, group1TypeProperties.size());
    assertEquals("prop1Value", group1TypeProperties.get("hostGroup1Prop1"));
    assertEquals("prop2Value", group1TypeProperties.get("hostGroup1Prop2"));
    assertTrue(group1Configuration.getAttributes().isEmpty());

    // cluster scoped configuration
    Configuration clusterScopeConfiguration = provisionClusterRequest.getConfiguration();
    assertSame(blueprintConfig, clusterScopeConfiguration.getParentConfiguration());
    assertEquals(1, clusterScopeConfiguration.getProperties().size());
    Map<String, String> clusterScopedProperties = clusterScopeConfiguration.getProperties().get("someType");
    assertEquals(1, clusterScopedProperties.size());
    assertEquals("someValue", clusterScopedProperties.get("property1"));
    // attributes
    Map<String, Map<String, Map<String, String>>> clusterScopedAttributes = clusterScopeConfiguration.getAttributes();
    assertEquals(1, clusterScopedAttributes.size());
    Map<String, Map<String, String>> clusterScopedTypeAttributes = clusterScopedAttributes.get("someType");
    assertEquals(1, clusterScopedTypeAttributes.size());
    Map<String, String> clusterScopedTypePropertyAttributes = clusterScopedTypeAttributes.get("attribute1");
    assertEquals(1, clusterScopedTypePropertyAttributes.size());
    assertEquals("someAttributePropValue", clusterScopedTypePropertyAttributes.get("property1"));
  }

  @Test
  public void testHostCountSpecified() throws Exception {
    // reset host resource provider expectations to none since we are not specifying a host predicate
    reset(hostResourceProvider);
    replay(hostResourceProvider);
    Map<String, Object> properties = createBlueprintRequestPropertiesCountOnly(CLUSTER_NAME, BLUEPRINT_NAME);

    ProvisionClusterRequest provisionClusterRequest = new ProvisionClusterRequest(properties, null);

    assertEquals(CLUSTER_NAME, provisionClusterRequest.getClusterName());
    assertEquals(TopologyRequest.Type.PROVISION, provisionClusterRequest.getType());
    assertEquals(String.format("Provision Cluster '%s'", CLUSTER_NAME) , provisionClusterRequest.getDescription());
    assertSame(blueprint, provisionClusterRequest.getBlueprint());
    Map<String, HostGroupInfo> hostGroupInfo = provisionClusterRequest.getHostGroupInfo();
    assertEquals(1, hostGroupInfo.size());

    // group2
    HostGroupInfo group2Info = hostGroupInfo.get("group2");
    assertEquals("group2", group2Info.getHostGroupName());
    assertTrue(group2Info.getHostNames().isEmpty());
    assertEquals(5, group2Info.getRequestedHostCount());
    assertNull(group2Info.getPredicate());
    // configuration
    Configuration group2Configuration = group2Info.getConfiguration();
    assertNull(group2Configuration.getParentConfiguration());
    assertEquals(1, group2Configuration.getProperties().size());
    Map<String, String> group2TypeProperties = group2Configuration.getProperties().get("foo-type");
    assertEquals(1, group2TypeProperties.size());
    assertEquals("prop1Value", group2TypeProperties.get("hostGroup2Prop1"));
    //attributes
    Map<String, Map<String, Map<String, String>>> group2Attributes = group2Configuration.getAttributes();
    assertEquals(1, group2Attributes.size());
    Map<String, Map<String, String>> group2Type1Attributes = group2Attributes.get("foo-type");
    assertEquals(1, group2Type1Attributes.size());
    Map<String, String> group2Type1Prop1Attributes = group2Type1Attributes.get("attribute1");
    assertEquals(1, group2Type1Prop1Attributes.size());
    assertEquals("attribute1Prop10-value", group2Type1Prop1Attributes.get("hostGroup2Prop10"));

    // cluster scoped configuration
    Configuration clusterScopeConfiguration = provisionClusterRequest.getConfiguration();
    assertSame(blueprintConfig, clusterScopeConfiguration.getParentConfiguration());
    assertEquals(1, clusterScopeConfiguration.getProperties().size());
    Map<String, String> clusterScopedProperties = clusterScopeConfiguration.getProperties().get("someType");
    assertEquals(1, clusterScopedProperties.size());
    assertEquals("someValue", clusterScopedProperties.get("property1"));
    // attributes
    Map<String, Map<String, Map<String, String>>> clusterScopedAttributes = clusterScopeConfiguration.getAttributes();
    assertEquals(1, clusterScopedAttributes.size());
    Map<String, Map<String, String>> clusterScopedTypeAttributes = clusterScopedAttributes.get("someType");
    assertEquals(1, clusterScopedTypeAttributes.size());
    Map<String, String> clusterScopedTypePropertyAttributes = clusterScopedTypeAttributes.get("attribute1");
    assertEquals(1, clusterScopedTypePropertyAttributes.size());
    assertEquals("someAttributePropValue", clusterScopedTypePropertyAttributes.get("property1"));
  }

  @Test
  public void testMultipleGroups() throws Exception {
    Map<String, Object> properties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);
    ProvisionClusterRequest provisionClusterRequest = new ProvisionClusterRequest(properties, null);

    assertEquals(CLUSTER_NAME, provisionClusterRequest.getClusterName());
    assertEquals(TopologyRequest.Type.PROVISION, provisionClusterRequest.getType());
    assertEquals(String.format("Provision Cluster '%s'", CLUSTER_NAME) , provisionClusterRequest.getDescription());
    assertSame(blueprint, provisionClusterRequest.getBlueprint());
    Map<String, HostGroupInfo> hostGroupInfo = provisionClusterRequest.getHostGroupInfo();
    assertEquals(2, hostGroupInfo.size());

    // group1
    // host info
    HostGroupInfo group1Info = hostGroupInfo.get("group1");
    assertEquals("group1", group1Info.getHostGroupName());
    assertEquals(1, group1Info.getHostNames().size());
    assertTrue(group1Info.getHostNames().contains("host1.mydomain.com"));
    assertEquals(1, group1Info.getRequestedHostCount());
    assertNull(group1Info.getPredicate());
    // configuration
    Configuration group1Configuration = group1Info.getConfiguration();
    assertNull(group1Configuration.getParentConfiguration());
    assertEquals(1, group1Configuration.getProperties().size());
    Map<String, String> group1TypeProperties = group1Configuration.getProperties().get("foo-type");
    assertEquals(2, group1TypeProperties.size());
    assertEquals("prop1Value", group1TypeProperties.get("hostGroup1Prop1"));
    assertEquals("prop2Value", group1TypeProperties.get("hostGroup1Prop2"));
    assertTrue(group1Configuration.getAttributes().isEmpty());

    // group2
    HostGroupInfo group2Info = hostGroupInfo.get("group2");
    assertEquals("group2", group2Info.getHostGroupName());
    assertTrue(group2Info.getHostNames().isEmpty());
    assertEquals(5, group2Info.getRequestedHostCount());
    assertNotNull(group2Info.getPredicate());
    // configuration
    Configuration group2Configuration = group2Info.getConfiguration();
    assertNull(group2Configuration.getParentConfiguration());
    assertEquals(1, group2Configuration.getProperties().size());
    Map<String, String> group2TypeProperties = group2Configuration.getProperties().get("foo-type");
    assertEquals(1, group2TypeProperties.size());
    assertEquals("prop1Value", group2TypeProperties.get("hostGroup2Prop1"));
    //attributes
    Map<String, Map<String, Map<String, String>>> group2Attributes = group2Configuration.getAttributes();
    assertEquals(1, group2Attributes.size());
    Map<String, Map<String, String>> group2Type1Attributes = group2Attributes.get("foo-type");
    assertEquals(1, group2Type1Attributes.size());
    Map<String, String> group2Type1Prop1Attributes = group2Type1Attributes.get("attribute1");
    assertEquals(1, group2Type1Prop1Attributes.size());
    assertEquals("attribute1Prop10-value", group2Type1Prop1Attributes.get("hostGroup2Prop10"));

    // cluster scoped configuration
    Configuration clusterScopeConfiguration = provisionClusterRequest.getConfiguration();
    assertSame(blueprintConfig, clusterScopeConfiguration.getParentConfiguration());
    assertEquals(1, clusterScopeConfiguration.getProperties().size());
    Map<String, String> clusterScopedProperties = clusterScopeConfiguration.getProperties().get("someType");
    assertEquals(1, clusterScopedProperties.size());
    assertEquals("someValue", clusterScopedProperties.get("property1"));
    // attributes
    Map<String, Map<String, Map<String, String>>> clusterScopedAttributes = clusterScopeConfiguration.getAttributes();
    assertEquals(1, clusterScopedAttributes.size());
    Map<String, Map<String, String>> clusterScopedTypeAttributes = clusterScopedAttributes.get("someType");
    assertEquals(1, clusterScopedTypeAttributes.size());
    Map<String, String> clusterScopedTypePropertyAttributes = clusterScopedTypeAttributes.get("attribute1");
    assertEquals(1, clusterScopedTypePropertyAttributes.size());
    assertEquals("someAttributePropValue", clusterScopedTypePropertyAttributes.get("property1"));
  }

  @Test(expected= InvalidTopologyTemplateException.class)
  public void test_NoHostGroupInfo() throws Exception {
    Map<String, Object> properties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);
    ((Collection)properties.get("host_groups")).clear();

    // reset default host resource provider expectations to none
    reset(hostResourceProvider);
    replay(hostResourceProvider);
    // should result in an exception
    new ProvisionClusterRequest(properties, null);
  }

  @Test
  public void test_Creditentials() throws Exception {
    Map<String, Object> properties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);
    HashMap<String, String> credentialHashMap = new HashMap<>();
    credentialHashMap.put("alias", "testAlias");
    credentialHashMap.put("principal", "testPrincipal");
    credentialHashMap.put("key", "testKey");
    credentialHashMap.put("type", "temporary");
    Set<Map<String, String>> credentialsSet = new HashSet<>();
    credentialsSet.add(credentialHashMap);
    properties.put("credentials", credentialsSet);

    ProvisionClusterRequest provisionClusterRequest = new ProvisionClusterRequest(properties, null);

    assertEquals(provisionClusterRequest.getCredentialsMap().get("testAlias").getAlias(), "testAlias");
    assertEquals(provisionClusterRequest.getCredentialsMap().get("testAlias").getPrincipal(), "testPrincipal");
    assertEquals(provisionClusterRequest.getCredentialsMap().get("testAlias").getKey(), "testKey");
    assertEquals(provisionClusterRequest.getCredentialsMap().get("testAlias").getType().name(), "TEMPORARY");
  }


  @Test
  public void test_CreditentialsInvalidType() throws Exception {
    expectedException.expect(InvalidTopologyTemplateException.class);
    expectedException.expectMessage("credential.type [TESTTYPE] is invalid. acceptable values: " +
        Arrays.toString(CredentialStoreType.values()));

    Map<String, Object> properties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);
    HashMap<String, String> credentialHashMap = new HashMap<>();
    credentialHashMap.put("alias", "testAlias");
    credentialHashMap.put("principal", "testPrincipal");
    credentialHashMap.put("key", "testKey");
    credentialHashMap.put("type", "testType");
    Set<Map<String, String>> credentialsSet = new HashSet<>();
    credentialsSet.add(credentialHashMap);
    properties.put("credentials", credentialsSet);

    ProvisionClusterRequest provisionClusterRequest = new ProvisionClusterRequest(properties, null);
  }

  @Test(expected= InvalidTopologyTemplateException.class)
  public void test_GroupInfoMissingName() throws Exception {
    Map<String, Object> properties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);
    ((Collection<Map<String, Object>>)properties.get("host_groups")).iterator().next().remove("name");

    // reset default host resource provider expectations to none
    reset(hostResourceProvider);
    replay(hostResourceProvider);
    // should result in an exception
    new ProvisionClusterRequest(properties, null);
  }

  @Test(expected= InvalidTopologyTemplateException.class)
  public void test_NoHostsInfo() throws Exception {
    Map<String, Object> properties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);
    ((Collection<Map<String, Object>>)properties.get("host_groups")).iterator().next().remove("hosts");

    // reset default host resource provider expectations to none
    reset(hostResourceProvider);
    replay(hostResourceProvider);
    // should result in an exception
    new ProvisionClusterRequest(properties, null);
  }

  @Test(expected = InvalidTopologyTemplateException.class)
  public void test_NoHostNameOrHostCount() throws Exception {
    Map<String, Object> properties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);
    // remove fqdn property for a group that contains fqdn not host_count
    for (Map<String, Object> groupProps : (Collection<Map<String, Object>>) properties.get("host_groups")) {
      Collection<Map<String, Object>> hostInfo = (Collection<Map<String, Object>>) groupProps.get("hosts");
      Map<String, Object> next = hostInfo.iterator().next();
      if (next.containsKey("fqdn")) {
        next.remove("fqdn");
        break;
      }
    }

    // reset default host resource provider expectations to none
    reset(hostResourceProvider);
    replay(hostResourceProvider);
    // should result in an exception
    new ProvisionClusterRequest(properties, null);
  }


  @Test(expected = InvalidTopologyTemplateException.class)
  public void testInvalidPredicateProperty() throws Exception {
    reset(hostResourceProvider);
    // checkPropertyIds() returns invalid property names
    expect(hostResourceProvider.checkPropertyIds(Collections.singleton("Hosts/host_name"))).
      andReturn(Collections.singleton("Hosts/host_name"));
    replay(hostResourceProvider);

    // should result in an exception due to invalid property in host predicate
    new ProvisionClusterRequest(createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME), null);
  }

  @Test(expected = InvalidTopologyTemplateException.class)
  public void testHostNameAndCountSpecified() throws Exception {
    // reset host resource provider expectations to none since we are not specifying a host predicate
    reset(hostResourceProvider);
    replay(hostResourceProvider);

    Map<String, Object> properties = createBlueprintRequestPropertiesNameOnly(CLUSTER_NAME, BLUEPRINT_NAME);
    ((Map) ((List) properties.get("host_groups")).iterator().next()).put("host_count", "5");
    // should result in an exception due to both host name and host count being specified
    new ProvisionClusterRequest(properties, null);
  }

  @Test(expected = InvalidTopologyTemplateException.class)
  public void testHostNameAndPredicateSpecified() throws Exception {
    // reset host resource provider expectations to none since we are not specifying a host predicate
    reset(hostResourceProvider);
    replay(hostResourceProvider);

    Map<String, Object> properties = createBlueprintRequestPropertiesNameOnly(CLUSTER_NAME, BLUEPRINT_NAME);
    ((Map) ((List) properties.get("host_groups")).iterator().next()).put("host_predicate", "Hosts/host_name=myTestHost");
    // should result in an exception due to both host name and host count being specified
    new ProvisionClusterRequest(properties, null);
  }

  @Test
  public void testQuickLinksProfile_NoDataInRequest() throws Exception {
    Map<String, Object> properties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);
    ProvisionClusterRequest request = new ProvisionClusterRequest(properties, null);
    assertNull("No quick links profile is expected", request.getQuickLinksProfileJson());
  }

  @Test
  public void testQuickLinksProfile_OnlyGlobalFilterDataInRequest() throws Exception {
    Map<String, Object> properties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);

    properties.put(ProvisionClusterRequest.QUICKLINKS_PROFILE_FILTERS_PROPERTY,
        Sets.newHashSet(QuickLinksProfileBuilderTest.filter(null, null, true)));

    ProvisionClusterRequest request = new ProvisionClusterRequest(properties, null);
    assertEquals("Quick links profile doesn't match expected",
        "{\"filters\":[{\"visible\":true}]}",
        request.getQuickLinksProfileJson());
  }

  @Test
  public void testQuickLinksProfile_OnlyServiceFilterDataInRequest() throws Exception {
    Map<String, Object> properties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);

    Map<String, String> filter = QuickLinksProfileBuilderTest.filter(null, null, true);
    Map<String, Object> hdfs = QuickLinksProfileBuilderTest.service("HDFS", null, Sets.newHashSet(filter));
    Set<Map<String, Object>> services = Sets.newHashSet(hdfs);
    properties.put(ProvisionClusterRequest.QUICKLINKS_PROFILE_SERVICES_PROPERTY, services);

    ProvisionClusterRequest request = new ProvisionClusterRequest(properties, null);
    assertEquals("Quick links profile doesn't match expected",
        "{\"services\":[{\"name\":\"HDFS\",\"filters\":[{\"visible\":true}]}]}",
        request.getQuickLinksProfileJson());
  }

  @Test
  public void testQuickLinksProfile_BothGlobalAndServiceLevelFilters() throws Exception {
    Map<String, Object> properties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);

    properties.put(ProvisionClusterRequest.QUICKLINKS_PROFILE_FILTERS_PROPERTY,
        Sets.newHashSet(QuickLinksProfileBuilderTest.filter(null, null, true)));

    Map<String, String> filter = QuickLinksProfileBuilderTest.filter(null, null, true);
    Map<String, Object> hdfs = QuickLinksProfileBuilderTest.service("HDFS", null, Sets.newHashSet(filter));
    Set<Map<String, Object>> services = Sets.newHashSet(hdfs);
    properties.put(ProvisionClusterRequest.QUICKLINKS_PROFILE_SERVICES_PROPERTY, services);

    ProvisionClusterRequest request = new ProvisionClusterRequest(properties, null);
    System.out.println(request.getQuickLinksProfileJson());
    assertEquals("Quick links profile doesn't match expected",
        "{\"filters\":[{\"visible\":true}],\"services\":[{\"name\":\"HDFS\",\"filters\":[{\"visible\":true}]}]}",
        request.getQuickLinksProfileJson());
  }

  @Test(expected = InvalidTopologyTemplateException.class)
  public void testQuickLinksProfile_InvalidRequestData() throws Exception {
    Map<String, Object> properties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);

    properties.put(ProvisionClusterRequest.QUICKLINKS_PROFILE_SERVICES_PROPERTY, "Hello World!");

    ProvisionClusterRequest request = new ProvisionClusterRequest(properties, null);
  }

  public static Map<String, Object> createBlueprintRequestProperties(String clusterName, String blueprintName) {
    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID, clusterName);
    properties.put(ClusterResourceProvider.BLUEPRINT, blueprintName);

    Collection<Map<String, Object>> hostGroups = new ArrayList<>();
    properties.put("host_groups", hostGroups);

    // host group 1
    Map<String, Object> hostGroup1Properties = new HashMap<>();
    hostGroups.add(hostGroup1Properties);
    hostGroup1Properties.put("name", "group1");
    Collection<Map<String, String>> hostGroup1Hosts = new ArrayList<>();
    hostGroup1Properties.put("hosts", hostGroup1Hosts);
    Map<String, String> hostGroup1HostProperties = new HashMap<>();
    hostGroup1HostProperties.put("fqdn", "host1.myDomain.com");
    hostGroup1Hosts.add(hostGroup1HostProperties);
    // host group 1 scoped configuration
    // version 1 configuration syntax
    Collection<Map<String, String>> hostGroup1Configurations = new ArrayList<>();
    hostGroup1Properties.put("configurations", hostGroup1Configurations);
    Map<String, String> hostGroup1Configuration1 = new HashMap<>();
    hostGroup1Configuration1.put("foo-type/hostGroup1Prop1", "prop1Value");
    hostGroup1Configuration1.put("foo-type/hostGroup1Prop2", "prop2Value");
    hostGroup1Configurations.add(hostGroup1Configuration1);

    // host group 2
    Map<String, Object> hostGroup2Properties = new HashMap<>();
    hostGroups.add(hostGroup2Properties);
    hostGroup2Properties.put("name", "group2");
    hostGroup2Properties.put("host_count", "5");
    hostGroup2Properties.put("host_predicate", "Hosts/host_name=myTestHost");

    // host group 2 scoped configuration
    // version 2 configuration syntax
    Collection<Map<String, String>> hostGroup2Configurations = new ArrayList<>();
    hostGroup2Properties.put("configurations", hostGroup2Configurations);
    Map<String, String> hostGroup2Configuration1 = new HashMap<>();
    hostGroup2Configuration1.put("foo-type/properties/hostGroup2Prop1", "prop1Value");
    hostGroup2Configuration1.put("foo-type/properties_attributes/attribute1/hostGroup2Prop10", "attribute1Prop10-value");
    hostGroup2Configurations.add(hostGroup2Configuration1);

    // cluster scoped configuration
    Collection<Map<String, String>> clusterConfigurations = new ArrayList<>();
    properties.put("configurations", clusterConfigurations);

    Map<String, String> clusterConfigurationProperties = new HashMap<>();
    clusterConfigurations.add(clusterConfigurationProperties);
    clusterConfigurationProperties.put("someType/properties/property1", "someValue");
    clusterConfigurationProperties.put("someType/properties_attributes/attribute1/property1", "someAttributePropValue");

    return properties;
  }

  public static Map<String, Object> createBlueprintRequestPropertiesNameOnly(String clusterName, String blueprintName) {
    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID, clusterName);
    properties.put(ClusterResourceProvider.BLUEPRINT, blueprintName);

    Collection<Map<String, Object>> hostGroups = new ArrayList<>();
    properties.put("host_groups", hostGroups);

    // host group 1
    Map<String, Object> hostGroup1Properties = new HashMap<>();
    hostGroups.add(hostGroup1Properties);
    hostGroup1Properties.put("name", "group1");
    Collection<Map<String, String>> hostGroup1Hosts = new ArrayList<>();
    hostGroup1Properties.put("hosts", hostGroup1Hosts);
    Map<String, String> hostGroup1HostProperties = new HashMap<>();
    hostGroup1HostProperties.put("fqdn", "host1.myDomain.com");
    hostGroup1Hosts.add(hostGroup1HostProperties);
    // host group 1 scoped configuration
    // version 1 configuration syntax
    Collection<Map<String, String>> hostGroup1Configurations = new ArrayList<>();
    hostGroup1Properties.put("configurations", hostGroup1Configurations);
    Map<String, String> hostGroup1Configuration1 = new HashMap<>();
    hostGroup1Configuration1.put("foo-type/hostGroup1Prop1", "prop1Value");
    hostGroup1Configuration1.put("foo-type/hostGroup1Prop2", "prop2Value");
    hostGroup1Configurations.add(hostGroup1Configuration1);

    // cluster scoped configuration
    Collection<Map<String, String>> clusterConfigurations = new ArrayList<>();
    properties.put("configurations", clusterConfigurations);

    Map<String, String> clusterConfigurationProperties = new HashMap<>();
    clusterConfigurations.add(clusterConfigurationProperties);
    clusterConfigurationProperties.put("someType/properties/property1", "someValue");
    clusterConfigurationProperties.put("someType/properties_attributes/attribute1/property1", "someAttributePropValue");

    return properties;
  }

  public static Map<String, Object> createBlueprintRequestPropertiesCountOnly(String clusterName, String blueprintName) {
    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID, clusterName);
    properties.put(ClusterResourceProvider.BLUEPRINT, blueprintName);

    Collection<Map<String, Object>> hostGroups = new ArrayList<>();
    properties.put("host_groups", hostGroups);

    // host group 2
    Map<String, Object> hostGroup2Properties = new HashMap<>();
    hostGroups.add(hostGroup2Properties);
    hostGroup2Properties.put("name", "group2");
    // count with no predicate
    hostGroup2Properties.put("host_count", "5");

    // host group 2 scoped configuration
    // version 2 configuration syntax
    Collection<Map<String, String>> hostGroup2Configurations = new ArrayList<>();
    hostGroup2Properties.put("configurations", hostGroup2Configurations);
    Map<String, String> hostGroup2Configuration1 = new HashMap<>();
    hostGroup2Configuration1.put("foo-type/properties/hostGroup2Prop1", "prop1Value");
    hostGroup2Configuration1.put("foo-type/properties_attributes/attribute1/hostGroup2Prop10", "attribute1Prop10-value");
    hostGroup2Configurations.add(hostGroup2Configuration1);

    // cluster scoped configuration
    Collection<Map<String, String>> clusterConfigurations = new ArrayList<>();
    properties.put("configurations", clusterConfigurations);

    Map<String, String> clusterConfigurationProperties = new HashMap<>();
    clusterConfigurations.add(clusterConfigurationProperties);
    clusterConfigurationProperties.put("someType/properties/property1", "someValue");
    clusterConfigurationProperties.put("someType/properties_attributes/attribute1/property1", "someAttributePropValue");

    return properties;
  }
}
