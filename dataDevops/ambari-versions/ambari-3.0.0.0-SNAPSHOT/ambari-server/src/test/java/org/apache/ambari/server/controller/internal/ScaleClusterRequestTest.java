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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createStrictMock;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.reset;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.BlueprintFactory;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.HostGroup;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.apache.ambari.server.topology.InvalidTopologyTemplateException;
import org.apache.ambari.server.topology.TopologyRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for ScaleClusterRequest.
 */
@SuppressWarnings("unchecked")
public class ScaleClusterRequestTest {

  private static final String CLUSTER_NAME = "cluster_name";
  private static final String BLUEPRINT_NAME = "blueprint_name";
  private static final String HOST1_NAME = "host1.test.com";
  private static final String HOST2_NAME = "host2.test.com";
  private static final String GROUP1_NAME = "group1";
  private static final String GROUP2_NAME = "group2";
  private static final String GROUP3_NAME = "group3";
  private static final String PREDICATE = "test/prop=foo";

  private static final BlueprintFactory blueprintFactory = createStrictMock(BlueprintFactory.class);
  private static final Blueprint blueprint = createNiceMock(Blueprint.class);
  private static final ResourceProvider hostResourceProvider = createMock(ResourceProvider.class);
  private static final HostGroup hostGroup1 = createNiceMock(HostGroup.class);
  private static final Configuration blueprintConfig = new Configuration(
      Collections.emptyMap(),
      Collections.emptyMap());

  @Before
  public void setUp() throws Exception {
    ScaleClusterRequest.init(blueprintFactory);
    // set host resource provider field
    Class clazz = BaseClusterRequest.class;
    Field f = clazz.getDeclaredField("hostResourceProvider");
    f.setAccessible(true);
    f.set(null, hostResourceProvider);

    expect(blueprintFactory.getBlueprint(BLUEPRINT_NAME)).andReturn(blueprint).anyTimes();
    expect(blueprint.getConfiguration()).andReturn(blueprintConfig).anyTimes();
    expect(blueprint.getHostGroup(GROUP1_NAME)).andReturn(hostGroup1).anyTimes();
    expect(blueprint.getHostGroup(GROUP2_NAME)).andReturn(hostGroup1).anyTimes();
    expect(blueprint.getHostGroup(GROUP3_NAME)).andReturn(hostGroup1).anyTimes();
    expect(blueprint.getName()).andReturn(BLUEPRINT_NAME).anyTimes();
    expect(hostResourceProvider.checkPropertyIds(Collections.singleton("test/prop"))).
        andReturn(Collections.emptySet()).once();

    replay(blueprintFactory, blueprint, hostResourceProvider, hostGroup1);
  }

  @After
  public void tearDown() {
    verify(blueprintFactory, blueprint, hostResourceProvider, hostGroup1);
    reset(blueprintFactory, blueprint, hostResourceProvider, hostGroup1);
  }

  @Test
  public void test_basic_hostName() throws Exception {
    Map<String, Object> props = createScaleClusterPropertiesGroup1_HostName(CLUSTER_NAME, BLUEPRINT_NAME);
    addSingleHostByName(props);
    addSingleHostByName(replaceWithPlainHostNameKey(props));
  }

  private void addSingleHostByName(Map<String, Object> props) throws InvalidTopologyTemplateException {
    // reset default host resource provider expectations to none since no host predicate is used
    reset(hostResourceProvider);
    replay(hostResourceProvider);

    ScaleClusterRequest scaleClusterRequest = new ScaleClusterRequest(Collections.singleton(props));

    assertEquals(TopologyRequest.Type.SCALE, scaleClusterRequest.getType());
    assertEquals(String.format("Scale Cluster '%s' (+%s hosts)", CLUSTER_NAME, "1"),
        scaleClusterRequest.getDescription());
    assertEquals(CLUSTER_NAME, scaleClusterRequest.getClusterName());
    assertSame(blueprint, scaleClusterRequest.getBlueprint());
    Map<String, HostGroupInfo> hostGroupInfo = scaleClusterRequest.getHostGroupInfo();
    assertEquals(1, hostGroupInfo.size());

    // group1
    // host info
    HostGroupInfo group1Info = hostGroupInfo.get(GROUP1_NAME);
    assertEquals(GROUP1_NAME, group1Info.getHostGroupName());
    assertEquals(1, group1Info.getHostNames().size());
    assertTrue(group1Info.getHostNames().contains(HOST1_NAME));
    assertEquals(1, group1Info.getRequestedHostCount());
    assertNull(group1Info.getPredicate());
  }

  @Test
  public void testMultipleHostNames() throws Exception {
    Set<Map<String, Object>> propertySet = new HashSet<>();
    propertySet.add(createScaleClusterPropertiesGroup1_HostName(CLUSTER_NAME, BLUEPRINT_NAME));
    propertySet.add(createScaleClusterPropertiesGroup1_HostName2(CLUSTER_NAME, BLUEPRINT_NAME));
    addMultipleHostsByName(propertySet);

    for (Map<String, Object> props : propertySet) {
      replaceWithPlainHostNameKey(props);
    }
    addMultipleHostsByName(propertySet);
  }

  private void addMultipleHostsByName(Set<Map<String, Object>> propertySet) throws InvalidTopologyTemplateException {
    // reset default host resource provider expectations to none since no host predicate is used
    reset(hostResourceProvider);
    replay(hostResourceProvider);

    ScaleClusterRequest scaleClusterRequest = new ScaleClusterRequest(propertySet);

    assertEquals(TopologyRequest.Type.SCALE, scaleClusterRequest.getType());
    assertEquals(String.format("Scale Cluster '%s' (+%s hosts)", CLUSTER_NAME, "2"),
        scaleClusterRequest.getDescription());
    assertEquals(CLUSTER_NAME, scaleClusterRequest.getClusterName());
    assertSame(blueprint, scaleClusterRequest.getBlueprint());
    Map<String, HostGroupInfo> hostGroupInfo = scaleClusterRequest.getHostGroupInfo();
    assertEquals(1, hostGroupInfo.size());

    // group1
    // host info
    HostGroupInfo group1Info = hostGroupInfo.get(GROUP1_NAME);
    assertEquals(GROUP1_NAME, group1Info.getHostGroupName());
    assertEquals(2, group1Info.getHostNames().size());
    assertTrue(group1Info.getHostNames().contains(HOST1_NAME));
    assertTrue(group1Info.getHostNames().contains(HOST2_NAME));
    assertEquals(2, group1Info.getRequestedHostCount());
    assertNull(group1Info.getPredicate());
  }

  @Test
  public void test_basic_hostCount() throws Exception {
    // reset default host resource provider expectations to none since no host predicate is used
    reset(hostResourceProvider);
    replay(hostResourceProvider);

    ScaleClusterRequest scaleClusterRequest = new ScaleClusterRequest(Collections.singleton(
        createScaleClusterPropertiesGroup1_HostCount(CLUSTER_NAME, BLUEPRINT_NAME)));

    assertEquals(TopologyRequest.Type.SCALE, scaleClusterRequest.getType());
    assertEquals(String.format("Scale Cluster '%s' (+%s hosts)", CLUSTER_NAME, "1"),
        scaleClusterRequest.getDescription());
    assertEquals(CLUSTER_NAME, scaleClusterRequest.getClusterName());
    assertSame(blueprint, scaleClusterRequest.getBlueprint());
    Map<String, HostGroupInfo> hostGroupInfo = scaleClusterRequest.getHostGroupInfo();
    assertEquals(1, hostGroupInfo.size());

    // group2
    // host info
    HostGroupInfo group2Info = hostGroupInfo.get(GROUP2_NAME);
    assertEquals(GROUP2_NAME, group2Info.getHostGroupName());
    assertEquals(0, group2Info.getHostNames().size());
    assertEquals(1, group2Info.getRequestedHostCount());
    assertNull(group2Info.getPredicate());
  }

  @Test
  public void test_basic_hostCount2() throws Exception {
    // reset default host resource provider expectations to none since no host predicate is used
    reset(hostResourceProvider);
    replay(hostResourceProvider);

    ScaleClusterRequest scaleClusterRequest = new ScaleClusterRequest(Collections.singleton(
        createScaleClusterPropertiesGroup1_HostCount2(CLUSTER_NAME, BLUEPRINT_NAME)));

    assertEquals(TopologyRequest.Type.SCALE, scaleClusterRequest.getType());
    assertEquals(String.format("Scale Cluster '%s' (+%s hosts)", CLUSTER_NAME, "2"),
        scaleClusterRequest.getDescription());
    assertEquals(CLUSTER_NAME, scaleClusterRequest.getClusterName());
    assertSame(blueprint, scaleClusterRequest.getBlueprint());
    Map<String, HostGroupInfo> hostGroupInfo = scaleClusterRequest.getHostGroupInfo();
    assertEquals(1, hostGroupInfo.size());

    // group2
    // host info
    HostGroupInfo group2Info = hostGroupInfo.get(GROUP3_NAME);
    assertEquals(GROUP3_NAME, group2Info.getHostGroupName());
    assertEquals(0, group2Info.getHostNames().size());
    assertEquals(2, group2Info.getRequestedHostCount());
    assertNull(group2Info.getPredicate());
  }

  @Test
  public void test_basic_hostCountAndPredicate() throws Exception {
    ScaleClusterRequest scaleClusterRequest = new ScaleClusterRequest(Collections.singleton(
        createScaleClusterPropertiesGroup1_HostCountAndPredicate(CLUSTER_NAME, BLUEPRINT_NAME)));

    assertEquals(TopologyRequest.Type.SCALE, scaleClusterRequest.getType());
    assertEquals(String.format("Scale Cluster '%s' (+%s hosts)", CLUSTER_NAME, "1"),
        scaleClusterRequest.getDescription());
    assertEquals(CLUSTER_NAME, scaleClusterRequest.getClusterName());
    assertSame(blueprint, scaleClusterRequest.getBlueprint());
    Map<String, HostGroupInfo> hostGroupInfo = scaleClusterRequest.getHostGroupInfo();
    assertEquals(1, hostGroupInfo.size());

    // group3
    // host info
    HostGroupInfo group3Info = hostGroupInfo.get(GROUP3_NAME);
    assertEquals(GROUP3_NAME, group3Info.getHostGroupName());
    assertEquals(0, group3Info.getHostNames().size());
    assertEquals(1, group3Info.getRequestedHostCount());
    assertEquals(PREDICATE, group3Info.getPredicateString());
  }

  @Test
  public void testMultipleHostGroups() throws Exception {
    Set<Map<String, Object>> propertySet = new HashSet<>();
    propertySet.add(createScaleClusterPropertiesGroup1_HostCountAndPredicate(CLUSTER_NAME, BLUEPRINT_NAME));
    propertySet.add(createScaleClusterPropertiesGroup1_HostCount(CLUSTER_NAME, BLUEPRINT_NAME));
    propertySet.add(createScaleClusterPropertiesGroup1_HostName(CLUSTER_NAME, BLUEPRINT_NAME));

    ScaleClusterRequest scaleClusterRequest = new ScaleClusterRequest(propertySet);

    assertEquals(TopologyRequest.Type.SCALE, scaleClusterRequest.getType());
    assertEquals(String.format("Scale Cluster '%s' (+%s hosts)", CLUSTER_NAME, "3"),
        scaleClusterRequest.getDescription());
    assertEquals(CLUSTER_NAME, scaleClusterRequest.getClusterName());
    assertSame(blueprint, scaleClusterRequest.getBlueprint());
    Map<String, HostGroupInfo> hostGroupInfo = scaleClusterRequest.getHostGroupInfo();
    assertEquals(3, hostGroupInfo.size());

    // group
    // host info
    HostGroupInfo group1Info = hostGroupInfo.get(GROUP1_NAME);
    assertEquals(GROUP1_NAME, group1Info.getHostGroupName());
    assertEquals(1, group1Info.getHostNames().size());
    assertTrue(group1Info.getHostNames().contains(HOST1_NAME));
    assertEquals(1, group1Info.getRequestedHostCount());
    assertNull(group1Info.getPredicate());

    // group2
    // host info
    HostGroupInfo group2Info = hostGroupInfo.get(GROUP2_NAME);
    assertEquals(GROUP2_NAME, group2Info.getHostGroupName());
    assertEquals(0, group2Info.getHostNames().size());
    assertEquals(1, group2Info.getRequestedHostCount());
    assertNull(group2Info.getPredicate());

    // group3
    // host info
    HostGroupInfo group3Info = hostGroupInfo.get(GROUP3_NAME);
    assertEquals(GROUP3_NAME, group3Info.getHostGroupName());
    assertEquals(0, group3Info.getHostNames().size());
    assertEquals(1, group3Info.getRequestedHostCount());
    assertEquals(PREDICATE, group3Info.getPredicateString());
  }



  @Test(expected = InvalidTopologyTemplateException.class)
  public void test_GroupInfoMissingName() throws Exception {
    Map<String, Object> properties = createScaleClusterPropertiesGroup1_HostName(CLUSTER_NAME, BLUEPRINT_NAME);
    // remove host group name
    properties.remove("host_group");

    // reset default host resource provider expectations to none
    reset(hostResourceProvider);
    replay(hostResourceProvider);
    // should result in an exception
    new ScaleClusterRequest(Collections.singleton(properties));
  }

  @Test(expected = InvalidTopologyTemplateException.class)
  public void test_NoHostNameOrHostCount() throws Exception {
    Map<String, Object> properties = createScaleClusterPropertiesGroup1_HostName(CLUSTER_NAME, BLUEPRINT_NAME);
    // remove host name
    properties.remove(HostResourceProvider.HOST_HOST_NAME_PROPERTY_ID);

    // reset default host resource provider expectations to none
    reset(hostResourceProvider);
    replay(hostResourceProvider);
    // should result in an exception because neither host name or host count are specified
    new ScaleClusterRequest(Collections.singleton(properties));
  }


  @Test(expected = InvalidTopologyTemplateException.class)
  public void testInvalidPredicateProperty() throws Exception {
    reset(hostResourceProvider);
    // checkPropertyIds() returns invalid property names
    expect(hostResourceProvider.checkPropertyIds(Collections.singleton("test/prop"))).
        andReturn(Collections.singleton("test/prop"));
    replay(hostResourceProvider);

    // should result in an exception due to invalid property in host predicate
    new ScaleClusterRequest(Collections.singleton(
        createScaleClusterPropertiesGroup1_HostCountAndPredicate(CLUSTER_NAME, BLUEPRINT_NAME)));
  }

  @Test(expected = InvalidTopologyTemplateException.class)
  public void testMultipleBlueprints() throws Exception {
    reset(hostResourceProvider);
    replay(hostResourceProvider);

    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();
    propertySet.add(createScaleClusterPropertiesGroup1_HostName(CLUSTER_NAME, BLUEPRINT_NAME));
    propertySet.add(createScaleClusterPropertiesGroup1_HostName2(CLUSTER_NAME, "OTHER_BLUEPRINT"));

    // should result in an exception due to different blueprints being specified
    new ScaleClusterRequest(propertySet);
  }

  public static Map<String, Object> createScaleClusterPropertiesGroup1_HostName(String clusterName, String blueprintName) {
    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID, clusterName);
    properties.put(HostResourceProvider.BLUEPRINT_PROPERTY_ID, blueprintName);
    properties.put(HostResourceProvider.HOST_GROUP_PROPERTY_ID, GROUP1_NAME);
    properties.put(HostResourceProvider.HOST_HOST_NAME_PROPERTY_ID, HOST1_NAME);

    return properties;
  }

  // include host name under "host_name" key instead of "Hosts/host_name"
  private static Map<String, Object> replaceWithPlainHostNameKey(Map<String, Object> properties) {
    Object value = properties.remove(HostResourceProvider.HOST_HOST_NAME_PROPERTY_ID);
    properties.put(HostResourceProvider.HOST_NAME_PROPERTY_ID, value);
    return properties;
  }

  public static Map<String, Object> createScaleClusterPropertiesGroup1_HostCount(String clusterName, String blueprintName) {
    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID, clusterName);
    properties.put(HostResourceProvider.BLUEPRINT_PROPERTY_ID, blueprintName);
    properties.put(HostResourceProvider.HOST_GROUP_PROPERTY_ID, GROUP2_NAME);
    properties.put(HostResourceProvider.HOST_COUNT_PROPERTY_ID, 1);

    return properties;
  }

  public static Map<String, Object> createScaleClusterPropertiesGroup1_HostCountAndPredicate(String clusterName, String blueprintName) {
    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID, clusterName);
    properties.put(HostResourceProvider.BLUEPRINT_PROPERTY_ID, blueprintName);
    properties.put(HostResourceProvider.HOST_GROUP_PROPERTY_ID, GROUP3_NAME);
    properties.put(HostResourceProvider.HOST_COUNT_PROPERTY_ID, 1);
    properties.put(HostResourceProvider.HOST_PREDICATE_PROPERTY_ID, PREDICATE);

    return properties;
  }

  public static Map<String, Object> createScaleClusterPropertiesGroup1_HostCount2(String clusterName, String blueprintName) {
    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID, clusterName);
    properties.put(HostResourceProvider.BLUEPRINT_PROPERTY_ID, blueprintName);
    properties.put(HostResourceProvider.HOST_GROUP_PROPERTY_ID, GROUP3_NAME);
    properties.put(HostResourceProvider.HOST_COUNT_PROPERTY_ID, 2);

    return properties;
  }

  public static Map<String, Object> createScaleClusterPropertiesGroup1_HostName2(String clusterName, String blueprintName) {
    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID, clusterName);
    properties.put(HostResourceProvider.BLUEPRINT_PROPERTY_ID, blueprintName);
    properties.put(HostResourceProvider.HOST_GROUP_PROPERTY_ID, GROUP1_NAME);
    properties.put(HostResourceProvider.HOST_HOST_NAME_PROPERTY_ID, HOST2_NAME);

    return properties;
  }
}
