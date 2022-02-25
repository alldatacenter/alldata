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

package org.apache.ambari.server.topology.addservice;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toMap;
import static org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest.StackAdvisorRequestType.HOST_GROUPS;
import static org.apache.ambari.server.controller.internal.UnitUpdaterTest.configProperty;
import static org.apache.ambari.server.testutils.TestCollectionUtils.map;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorHelper;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.api.services.stackadvisor.validations.ValidationResponse;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.topology.ConfigRecommendationStrategy;
import org.apache.ambari.server.topology.Configuration;
import org.apache.commons.lang3.tuple.Pair;
import org.easymock.EasyMockRunner;
import org.easymock.IExpectationSetters;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Injector;

@RunWith(EasyMockRunner.class)
public class StackAdvisorAdapterTest {

  @Mock
  private AmbariManagementController managementController;

  @Mock
  private StackAdvisorHelper stackAdvisorHelper;

  @Mock
  private org.apache.ambari.server.configuration.Configuration serverConfig;

  @Mock
  private Injector injector;

  @Mock
  private Stack stack;

  @TestSubject
  private StackAdvisorAdapter adapter = new StackAdvisorAdapter();

  private static final Map<String, Set<String>> COMPONENT_HOST_MAP = ImmutableMap.<String, Set<String>>builder()
    .put("NAMENODE", ImmutableSet.of("c7401", "c7402"))
    .put("DATANODE", ImmutableSet.of("c7403", "c7404", "c7405", "c7406"))
    .put("HDFS_CLIENT", ImmutableSet.of("c7403", "c7404", "c7405", "c7406"))
    .put("ZOOKEEPER_SERVER", ImmutableSet.of("c7401", "c7402"))
    .put("ZOOKEEPER_CLIENT", ImmutableSet.of("c7401", "c7402", "c7403", "c7404", "c7405", "c7406"))
    .build();

  private static final Map<String, Map<String, Set<String>>> SERVICE_COMPONENT_HOST_MAP_1 = ImmutableMap.of(
    "HDFS", ImmutableMap.of(
      "NAMENODE", ImmutableSet.of("c7401", "c7402"),
      "DATANODE", ImmutableSet.of("c7403", "c7404", "c7405", "c7406"),
      "HDFS_CLIENT", ImmutableSet.of("c7403", "c7404", "c7405", "c7406")),
    "ZOOKEEPER", ImmutableMap.of(
      "ZOOKEEPER_SERVER", ImmutableSet.of("c7401", "c7402"),
      "ZOOKEEPER_CLIENT", ImmutableSet.of("c7401", "c7402", "c7403", "c7404", "c7405", "c7406")));

  private static final Map<String, Map<String, Set<String>>> SERVICE_COMPONENT_HOST_MAP_2 = ImmutableMap.<String, Map<String, Set<String>>>builder()
    .putAll(SERVICE_COMPONENT_HOST_MAP_1)
    .put("HIVE", emptyMap())
    .put("SPARK2", emptyMap())
    .build();

  private static final Map<String, Set<String>> HOST_COMPONENT_MAP = ImmutableMap.<String, Set<String>>builder()
    .put("c7401", ImmutableSet.of("NAMENODE", "ZOOKEEPER_SERVER", "ZOOKEEPER_CLIENT"))
    .put("c7402", ImmutableSet.of("NAMENODE", "ZOOKEEPER_SERVER", "ZOOKEEPER_CLIENT"))
    .put("c7403", ImmutableSet.of("DATANODE", "HDFS_CLIENT", "ZOOKEEPER_CLIENT"))
    .put("c7404", ImmutableSet.of("DATANODE", "HDFS_CLIENT", "ZOOKEEPER_CLIENT"))
    .put("c7405", ImmutableSet.of("DATANODE", "HDFS_CLIENT", "ZOOKEEPER_CLIENT"))
    .put("c7406", ImmutableSet.of("DATANODE", "HDFS_CLIENT", "ZOOKEEPER_CLIENT"))
    .build();

  private final AddServiceInfo.Builder addServiceInfoBuilder = new AddServiceInfo.Builder()
    .setClusterName("c1");

  @Test
  public void getHostComponentMap() {
    assertEquals(HOST_COMPONENT_MAP, StackAdvisorAdapter.getHostComponentMap(COMPONENT_HOST_MAP));
  }

  @Test
  public void getComponentHostMap() {
    assertEquals(COMPONENT_HOST_MAP, StackAdvisorAdapter.getComponentHostMap(SERVICE_COMPONENT_HOST_MAP_2));
  }

  @Test
  public void getRecommendedLayout() {
    Map<String, Set<String>> hostGroups = ImmutableMap.of(
      "host_group1", ImmutableSet.of("c7401", "c7402"),
      "host_group2", ImmutableSet.of("c7403", "c7404", "c7405", "c7406"));

    Map<String, Set<String>> hostGroupComponents = ImmutableMap.of(
      "host_group1", ImmutableSet.of("NAMENODE", "ZOOKEEPER_SERVER", "ZOOKEEPER_CLIENT"),
      "host_group2", ImmutableSet.of("DATANODE", "HDFS_CLIENT", "ZOOKEEPER_CLIENT"));

    Map<String, String> serviceToComponent = ImmutableMap.<String, String>builder()
      .put("NAMENODE", "HDFS")
      .put("DATANODE", "HDFS")
      .put("HDFS_CLIENT", "HDFS")
      .put("ZOOKEEPER_SERVER", "ZOOKEEPER")
      .put("ZOOKEEPER_CLIENT", "ZOOKEEPER")
      .build();

    assertEquals(SERVICE_COMPONENT_HOST_MAP_1,
      StackAdvisorAdapter.getRecommendedLayout(hostGroups, hostGroupComponents, serviceToComponent::get));
  }

  @Test
  public void mergeDisjunctMaps() {
    Map<String, String> map1 = ImmutableMap.of("key1", "value1", "key2", "value2");
    Map<String, String> map2 = ImmutableMap.of("key3", "value3", "key4", "value4");
    assertEquals(
      ImmutableMap.of("key1", "value1", "key2", "value2", "key3", "value3", "key4", "value4"),
      StackAdvisorAdapter.mergeDisjunctMaps(map1, map2));
  }

  @Test(expected = IllegalArgumentException.class)
  public void mergeDisjunctMaps_invalidInput() {
    Map<String, String> map1 = ImmutableMap.of("key1", "value1", "key2", "value2");
    Map<String, String> map2 = ImmutableMap.of("key2", "value2", "key3", "value3");
    StackAdvisorAdapter.mergeDisjunctMaps(map1, map2);
  }

  @Test
  public void keepNewServicesOnly() {
    Map<String, Map<String, Set<String>>> newServices = ImmutableMap.of(
      "KAFKA", emptyMap(),
      "PIG", emptyMap());

    Map<String, Map<String, Set<String>>> expectedNewServiceRecommendations = ImmutableMap.of(
      "KAFKA", ImmutableMap.of("KAFKA_BROKER", ImmutableSet.of("c7405")),
      "PIG", ImmutableMap.of("PIG_CLIENT", ImmutableSet.of("c7405", "c7406")));

    Map<String, Map<String, Set<String>>> recommendations = new HashMap<>(SERVICE_COMPONENT_HOST_MAP_1);
    recommendations.putAll(expectedNewServiceRecommendations);

    Map<String, Map<String, Set<String>>> newServiceRecommendations = StackAdvisorAdapter.keepNewServicesOnly(recommendations, newServices);
    assertEquals(expectedNewServiceRecommendations, newServiceRecommendations);
  }

  @Before
  public void setUp() throws Exception {
    Cluster cluster = mock(Cluster.class);
    expect(cluster.getHostNames()).andReturn(ImmutableSet.of("c7401", "c7402"));
    expect(cluster.getServices()).andReturn(ImmutableMap.of(
      "HDFS",
      service("HDFS", ImmutableMap.of("NAMENODE", ImmutableSet.of("c7401"), "HDFS_CLIENT", ImmutableSet.of("c7401", "c7402"))),
      "ZOOKEEPER",
      service("ZOOKEEPER", ImmutableMap.of("ZOOKEEPER_SERVER", ImmutableSet.of("c7401"), "ZOOKEEPER_CLIENT", ImmutableSet.of("c7401", "c7402"))),
      "MAPREDUCE2",
      service("MAPREDUCE2", ImmutableMap.of("HISTORYSERVER", ImmutableSet.of("c7401")))));
    Clusters clusters = mock(Clusters.class);
    expect(clusters.getCluster(anyString())).andReturn(cluster).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    replay(clusters, cluster, managementController);

    expect(serverConfig.getGplLicenseAccepted()).andReturn(Boolean.FALSE).anyTimes();
    @SuppressWarnings("unchecked")
    IExpectationSetters iExpectationSetters = expect(serverConfig.getAddServiceHostGroupStrategyClass()).andReturn((Class) GroupByComponentsStrategy.class).anyTimes();
    replay(serverConfig);

    expect(injector.getInstance(GroupByComponentsStrategy.class)).andReturn(new GroupByComponentsStrategy()).anyTimes();
    replay(injector);

    RecommendationResponse.BlueprintClusterBinding binding = RecommendationResponse.BlueprintClusterBinding.fromHostGroupHostMap(
      ImmutableMap.of(
        "hostgroup-1", ImmutableSet.of("c7401"),
        "hostgroup-2", ImmutableSet.of("c7402")));
    RecommendationResponse.Blueprint blueprint = new RecommendationResponse.Blueprint();
    blueprint.setHostGroups(RecommendationResponse.HostGroup.fromHostGroupComponents(
      ImmutableMap.of(
        "hostgroup-1", ImmutableSet.of("NAMENODE", "HDFS_CLIENT", "ZOOKEEPER_SERVER", "ZOOKEEPER_CLIENT", "HISTORYSERVER"),
        "hostgroup-2", ImmutableSet.of("HDFS_CLIENT", "ZOOKEEPER_CLIENT", "KAFKA_BROKER"))
    ));
    RecommendationResponse layoutResponse = createRecommendation(blueprint, binding);

    RecommendationResponse.Blueprint configBlueprint = new RecommendationResponse.Blueprint();
    RecommendationResponse.BlueprintConfigurations kafkaBroker = RecommendationResponse.BlueprintConfigurations.create(
      ImmutableMap.of("log.dirs", "/kafka-logs", "offsets.topic.replication.factor", "1"),
      ImmutableMap.of("maximum", ImmutableMap.of("offsets.topic.replication.factor", "10")));
    RecommendationResponse.BlueprintConfigurations spark2Defaults = RecommendationResponse.BlueprintConfigurations.create(
      ImmutableMap.of("spark.yarn.queue", "default"), null);
    RecommendationResponse.BlueprintConfigurations mapredSite = RecommendationResponse.BlueprintConfigurations.create(
      ImmutableMap.of("mapreduce.map.memory.mb", "682", "mapreduce.reduce.memory.mb", "1364"),
      ImmutableMap.of(
        "minimum", ImmutableMap.of("mapreduce.map.memory.mb", "682", "mapreduce.reduce.memory.mb", "682"),
        "maximum" , ImmutableMap.of("mapreduce.map.memory.mb", "2046", "mapreduce.reduce.memory.mb", "2046")));
    configBlueprint.setConfigurations(map(
      "kafka-broker", kafkaBroker,
      "spark2-defaults", spark2Defaults,
      "mapred-site", mapredSite
    ));
    RecommendationResponse configResponse = createRecommendation(configBlueprint, binding);

    expect(stackAdvisorHelper.recommend(anyObject())).andAnswer(() -> {
      StackAdvisorRequest request = (StackAdvisorRequest) getCurrentArguments()[0];
      assertNotNull(request.getHosts());
      assertNotNull(request.getServices());
      assertNotNull(request.getStackName());
      assertNotNull(request.getStackVersion());
      assertNotNull(request.getConfigurations());
      assertNotNull(request.getHostComponents());
      assertNotNull(request.getComponentHostsMap());
      assertNotNull(request.getHostGroupBindings());
      assertNotNull(request.getLdapConfig());
      assertNotNull(request.getRequestType());
      return request.getRequestType() == HOST_GROUPS ? layoutResponse : configResponse;
    });

    ValidationResponse validationResponse = new ValidationResponse();
    validationResponse.setItems(emptySet());
    expect(stackAdvisorHelper.validate(anyObject())).andReturn(validationResponse);

    replay(stackAdvisorHelper);

    expect(stack.getStackId()).andReturn(new StackId("HDP", "3.0")).anyTimes();
    ImmutableMap<String, String> serviceComponentMap = ImmutableMap.<String, String>builder()
      .put("KAFKA_BROKER", "KAFKA")
      .put("NAMENODE", "HDFS")
      .put("HDFS_CLIENT", "HDFS")
      .put("ZOOKEEPER_SERVER", "ZOOKEEPER")
      .put("ZOOKEEPER_CLIENT", "ZOOKEEPER")
      .put("HISTORYSERVER", "MAPREDUCE2")
      .build();
    expect(stack.getServiceForComponent(anyString())).andAnswer(() -> serviceComponentMap.get(getCurrentArguments()[0])).anyTimes();
    ImmutableMap<String, String> configTypeServiceMap = ImmutableMap.<String, String>builder()
      .put("kafka-broker", "KAFKA")
      .put("spark2-defaults", "SPARK2")
      .put("mapred-site", "MAPREDUCE2")
      .build();
    expect(stack.getServiceForConfigType(anyString())).andAnswer(() -> configTypeServiceMap.get(getCurrentArguments()[0])).anyTimes();
    expect(stack.getConfigurationPropertiesWithMetadata("OOZIE", "oozie-env")).andReturn(
      ImmutableMap.of(
        "mapreduce.map.memory.mb", configProperty("mapreduce.map.memory.mb", "MB"),
        "mapreduce.reduce.memory.mb", configProperty("mapreduce.reduce.memory.mb", "MB"))).anyTimes();
    replay(stack);
  }

  private static RecommendationResponse createRecommendation(RecommendationResponse.Blueprint blueprint,
                                                                            RecommendationResponse.BlueprintClusterBinding binding) {
    RecommendationResponse response = new RecommendationResponse();
    RecommendationResponse.Recommendation recommendation = new RecommendationResponse.Recommendation();
    response.setRecommendations(recommendation);
    recommendation.setBlueprint(blueprint);
    recommendation.setBlueprintClusterBinding(binding);
    return response;
  }

  private static Service service(String name, ImmutableMap<String,ImmutableSet<String>> componentHostMap) {
    Service service = mock(Service.class);
    expect(service.getName()).andReturn(name).anyTimes();
    Map<String, ServiceComponent> serviceComponents = componentHostMap.entrySet().stream()
      .map(entry -> {
        ServiceComponent component = mock(ServiceComponent.class);
        expect(component.getName()).andReturn(entry.getKey()).anyTimes();
        expect(component.getServiceComponentsHosts()).andReturn(entry.getValue()).anyTimes();
        replay(component);
        return Pair.of(entry.getKey(), component);
      })
      .collect(toMap(Pair::getKey, Pair::getValue));
    expect(service.getServiceComponents()).andReturn(serviceComponents).anyTimes();
    replay(service);
    return service;
  }

  @Test
  public void recommendLayout() {
    Map<String, Map<String, Set<String>>> newServices = ImmutableMap.of(
      "KAFKA",
      ImmutableMap.of("KAFKA_BROKER", emptySet()));

    AddServiceInfo info = addServiceInfoBuilder
      .setStack(stack)
      .setConfig(Configuration.newEmpty())
      .setNewServices(newServices)
      .build();
    AddServiceInfo infoWithRecommendations = adapter.recommendLayout(info);

    Map<String, Map<String, Set<String>>> expectedNewLayout = ImmutableMap.of(
      "KAFKA",
      ImmutableMap.of("KAFKA_BROKER", ImmutableSet.of("c7402"))
    );

    assertEquals(expectedNewLayout, infoWithRecommendations.newServices());
  }

  @Test
  public void recommendConfigurations_noLayoutInfo() {
    Map<String, Map<String, Set<String>>> newServices = ImmutableMap.of(
      "KAFKA", ImmutableMap.of(
        "KAFKA_BROKER", ImmutableSet.of("c7401")),
      "SPARK2", ImmutableMap.of(
        "SPARK2_JOBHISTORYSERVER", ImmutableSet.of("c7402"),
        "SPARK2_CLIENT", ImmutableSet.of("c7403", "c7404")),
      "OOZIE", ImmutableMap.of(
        "OOZIE_SERVER", ImmutableSet.of("c7401"),
        "OOZIE_CLIENT", ImmutableSet.of("c7403", "c7404")));

    Configuration stackConfig = Configuration.newEmpty();
    Configuration clusterConfig = new Configuration(
      map("oozie-env", map("oozie_heapsize", "1024", "oozie_permsize", "256")),
      emptyMap());
    Configuration userConfig = Configuration.newEmpty();
    userConfig.setParentConfiguration(clusterConfig);
    clusterConfig.setParentConfiguration(stackConfig);

    AddServiceRequest request = request(ConfigRecommendationStrategy.ALWAYS_APPLY);
    AddServiceInfo info = addServiceInfoBuilder
      .setRequest(request)
      .setStack(stack)
      .setConfig(userConfig)
      .setNewServices(newServices)
      .build(); // No LayoutRecommendationInfo
    AddServiceInfo infoWithConfig = adapter.recommendConfigurations(info);

    Configuration recommendedConfig = infoWithConfig.getConfig();
    assertSame(userConfig, recommendedConfig.getParentConfiguration());
    assertSame(clusterConfig, userConfig.getParentConfiguration());
    assertSame(stackConfig, clusterConfig.getParentConfiguration());

    // Yarn/Mapred config is excpected to be removed as it does not belong to newly added services
    assertEquals(
      ImmutableMap.of(
        "kafka-broker", ImmutableMap.of(
          "log.dirs", "/kafka-logs",
          "offsets.topic.replication.factor", "1"),
        "spark2-defaults", ImmutableMap.of(
          "spark.yarn.queue", "default"),
        "oozie-env", ImmutableMap.of(
          "oozie_heapsize", "1024m",  // unit updates should happen
          "oozie_permsize", "256m")),
      recommendedConfig.getProperties());

    assertEquals(
      ImmutableMap.of(
        "kafka-broker", ImmutableMap.of(
          "maximum", ImmutableMap.of("offsets.topic.replication.factor", "10"))),
      recommendedConfig.getAttributes());
  }

  @Test
  public void recommendConfigurations_alwaysApply() {
    Map<String, Map<String, Set<String>>> newServices = ImmutableMap.of(
      "KAFKA", ImmutableMap.of(
        "KAFKA_BROKER", ImmutableSet.of("c7401")),
      "SPARK2", ImmutableMap.of(
        "SPARK2_JOBHISTORYSERVER", ImmutableSet.of("c7402"),
        "SPARK2_CLIENT", ImmutableSet.of("c7403", "c7404")),
      "OOZIE", ImmutableMap.of(
        "OOZIE_SERVER", ImmutableSet.of("c7401"),
        "OOZIE_CLIENT", ImmutableSet.of("c7403", "c7404")));

    Configuration stackConfig = Configuration.newEmpty();
    Configuration clusterConfig = new Configuration(
      map("oozie-env", map("oozie_heapsize", "1024", "oozie_permsize", "256")),
      emptyMap());
    Configuration userConfig = Configuration.newEmpty();
    userConfig.setParentConfiguration(clusterConfig);
    clusterConfig.setParentConfiguration(stackConfig);

    LayoutRecommendationInfo layoutRecommendationInfo = new LayoutRecommendationInfo(new HashMap<>(), new HashMap<>()); // contents doesn't matter for the test
    AddServiceRequest request = request(ConfigRecommendationStrategy.ALWAYS_APPLY);
    AddServiceInfo info = addServiceInfoBuilder
      .setRequest(request)
      .setStack(stack)
      .setConfig(userConfig)
      .setNewServices(newServices)
      .setRecommendationInfo(layoutRecommendationInfo)
      .build();
    AddServiceInfo infoWithConfig = adapter.recommendConfigurations(info);

    Configuration recommendedConfig = infoWithConfig.getConfig();
    assertSame(userConfig, recommendedConfig.getParentConfiguration());
    assertSame(clusterConfig, userConfig.getParentConfiguration());
    assertSame(stackConfig, clusterConfig.getParentConfiguration());

    // Yarn/Mapred config is excpected to be removed as it does not belong to newly added services
    assertEquals(
      ImmutableMap.of(
        "kafka-broker", ImmutableMap.of(
          "log.dirs", "/kafka-logs",
          "offsets.topic.replication.factor", "1"),
        "spark2-defaults", ImmutableMap.of(
          "spark.yarn.queue", "default"),
        "oozie-env", ImmutableMap.of(
          "oozie_heapsize", "1024m",  // unit updates should happen
          "oozie_permsize", "256m")),
      recommendedConfig.getProperties());

    assertEquals(
      ImmutableMap.of(
        "kafka-broker", ImmutableMap.of(
          "maximum", ImmutableMap.of("offsets.topic.replication.factor", "10"))),
      recommendedConfig.getAttributes());
  }

  @Test
  public void recommendConfigurations_alwaysDoNotOverrideCustomValues() {
    Map<String, Map<String, Set<String>>> newServices = ImmutableMap.of(
      "KAFKA", ImmutableMap.of(
        "KAFKA_BROKER", ImmutableSet.of("c7401")),
      "SPARK2", ImmutableMap.of(
        "SPARK2_JOBHISTORYSERVER", ImmutableSet.of("c7402"),
        "SPARK2_CLIENT", ImmutableSet.of("c7403", "c7404")),
      "OOZIE", ImmutableMap.of(
        "OOZIE_SERVER", ImmutableSet.of("c7401"),
        "OOZIE_CLIENT", ImmutableSet.of("c7403", "c7404")));

    Configuration stackConfig = Configuration.newEmpty();
    Configuration clusterConfig = new Configuration(
      map("oozie-env", map("oozie_heapsize", "1024", "oozie_permsize", "256")),
      emptyMap());
    Configuration userConfig = Configuration.newEmpty();
    userConfig.setParentConfiguration(clusterConfig);
    clusterConfig.setParentConfiguration(stackConfig);

    LayoutRecommendationInfo layoutRecommendationInfo = new LayoutRecommendationInfo(new HashMap<>(), new HashMap<>()); // contents doesn't matter for the test
    AddServiceRequest request = request(ConfigRecommendationStrategy.ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES);
    AddServiceInfo info = addServiceInfoBuilder
      .setRequest(request)
      .setStack(stack)
      .setConfig(userConfig)
      .setNewServices(newServices)
      .setRecommendationInfo(layoutRecommendationInfo)
      .build();
    AddServiceInfo infoWithConfig = adapter.recommendConfigurations(info);

    assertSame(userConfig, infoWithConfig.getConfig()); // user config stays top priority
    Configuration recommendedConfig = userConfig.getParentConfiguration();
    assertSame(clusterConfig, recommendedConfig.getParentConfiguration());
    assertSame(stackConfig, clusterConfig.getParentConfiguration());

    // Yarn/Mapred config is excpected to be removed as it does not belong to newly added services
    assertEquals(
      ImmutableMap.of(
        "kafka-broker", ImmutableMap.of(
          "log.dirs", "/kafka-logs",
          "offsets.topic.replication.factor", "1"),
        "spark2-defaults", ImmutableMap.of(
          "spark.yarn.queue", "default")),
      recommendedConfig.getProperties());

    // the result of unit updates always happen in the top level config
    assertEquals(
      ImmutableMap.of(
        "oozie-env", ImmutableMap.of(
          "oozie_heapsize", "1024m",
          "oozie_permsize", "256m")),
      userConfig.getProperties());

    assertEquals(
      ImmutableMap.of(
        "kafka-broker", ImmutableMap.of(
          "maximum", ImmutableMap.of("offsets.topic.replication.factor", "10"))),
      recommendedConfig.getAttributes());
  }

  @Test
  public void recommendConfigurations_neverApply() {
    Map<String, Map<String, Set<String>>> newServices = ImmutableMap.of(
      "KAFKA", ImmutableMap.of(
        "KAFKA_BROKER", ImmutableSet.of("c7401")),
      "SPARK2", ImmutableMap.of(
        "SPARK2_JOBHISTORYSERVER", ImmutableSet.of("c7402"),
        "SPARK2_CLIENT", ImmutableSet.of("c7403", "c7404")),
      "OOZIE", ImmutableMap.of(
        "OOZIE_SERVER", ImmutableSet.of("c7401"),
        "OOZIE_CLIENT", ImmutableSet.of("c7403", "c7404")));

    Configuration stackConfig = Configuration.newEmpty();
    Configuration clusterConfig = new Configuration(
      map("oozie-env", map("oozie_heapsize", "1024", "oozie_permsize", "256")),
      emptyMap());
    Configuration userConfig = Configuration.newEmpty();
    userConfig.setParentConfiguration(clusterConfig);
    clusterConfig.setParentConfiguration(stackConfig);

    LayoutRecommendationInfo layoutRecommendationInfo = new LayoutRecommendationInfo(new HashMap<>(), new HashMap<>()); // contents doesn't matter for the test
    AddServiceRequest request = request(ConfigRecommendationStrategy.NEVER_APPLY);
    AddServiceInfo info = addServiceInfoBuilder
      .setRequest(request)
      .setStack(stack)
      .setConfig(userConfig)
      .setNewServices(newServices)
      .setRecommendationInfo(layoutRecommendationInfo)
      .build();
    AddServiceInfo infoWithConfig = adapter.recommendConfigurations(info);

    // No recommended config, no stack config
    assertSame(userConfig, infoWithConfig.getConfig());
    assertSame(clusterConfig, userConfig.getParentConfiguration());
    assertNotNull(clusterConfig.getParentConfiguration());

    // the result of unit updates always happen in the top level config
    assertEquals(
      ImmutableMap.of(
        "oozie-env", ImmutableMap.of(
          "oozie_heapsize", "1024m",
          "oozie_permsize", "256m")),
      userConfig.getProperties());
  }

  @Test
  public void recommendConfigurations_onlyStackDefaultsApply() {
    Map<String, Map<String, Set<String>>> newServices = ImmutableMap.of(
      "KAFKA", ImmutableMap.of(
        "KAFKA_BROKER", ImmutableSet.of("c7401")),
      "SPARK2", ImmutableMap.of(
        "SPARK2_JOBHISTORYSERVER", ImmutableSet.of("c7402"),
        "SPARK2_CLIENT", ImmutableSet.of("c7403", "c7404")),
      "OOZIE", ImmutableMap.of(
        "OOZIE_SERVER", ImmutableSet.of("c7401"),
        "OOZIE_CLIENT", ImmutableSet.of("c7403", "c7404")));

    Configuration stackConfig = new Configuration(
      ImmutableMap.of("kafka-broker", ImmutableMap.of("log.dirs", "/kafka-logs-stackdefault")),
      ImmutableMap.of());
    Configuration clusterConfig = new Configuration(
      ImmutableMap.of("oozie-env", ImmutableMap.of("oozie_heapsize", "1024", "oozie_permsize", "256")),
      emptyMap());
    Configuration userConfig = Configuration.newEmpty();
    userConfig.setParentConfiguration(clusterConfig);
    clusterConfig.setParentConfiguration(stackConfig);

    LayoutRecommendationInfo layoutRecommendationInfo = new LayoutRecommendationInfo(new HashMap<>(), new HashMap<>()); // contents doesn't matter for the test
    AddServiceRequest request = request(ConfigRecommendationStrategy.ONLY_STACK_DEFAULTS_APPLY);
    AddServiceInfo info = addServiceInfoBuilder
      .setRequest(request)
      .setStack(stack)
      .setConfig(userConfig)
      .setNewServices(newServices)
      .setRecommendationInfo(layoutRecommendationInfo)
      .build();
    AddServiceInfo infoWithConfig = adapter.recommendConfigurations(info);
    Configuration recommendedConfig = infoWithConfig.getConfig().getParentConfiguration();

    // No recommended config
    assertSame(userConfig, infoWithConfig.getConfig()); // user config is top level in this case
    assertSame(clusterConfig, recommendedConfig.getParentConfiguration());
    assertSame(stackConfig, clusterConfig.getParentConfiguration());

    assertEquals(
      ImmutableMap.of(
        "kafka-broker", ImmutableMap.of(
          "log.dirs", "/kafka-logs")),
      recommendedConfig.getProperties());

    // the result of unit updates always happen in the top level config
    assertEquals(
      ImmutableMap.of(
        "oozie-env", ImmutableMap.of(
          "oozie_heapsize", "1024m",
          "oozie_permsize", "256m")),
      userConfig.getProperties());
  }

  @Test
  public void removeNonStackConfigRecommendations() {
    Map<String, Map<String, String>> stackProperties = ImmutableMap.of(
      "kafka-broker", ImmutableMap.of(
        "log.dirs", "/kafka-logs",
        "offsets.topic.replication.factor", "1"),
      "spark2-defaults", ImmutableMap.of(
        "spark.yarn.queue", "default"));

    Map<String, Map<String, Map<String, String>>> stackAttributes = ImmutableMap.of(
      "oozie-env",
      ImmutableMap.of(
        "miniumum",
        ImmutableMap.of("oozie_heapsize", "1024", "oozie_permsize", "256")));

    Configuration stackConfig = new Configuration(stackProperties, stackAttributes);

    Map<String, RecommendationResponse.BlueprintConfigurations> recommendedConfigs =
      map(
        "hdfs-site", RecommendationResponse.BlueprintConfigurations.create(
          map("dfs.namenode.name.dir", "/hadoop/hdfs/namenode"),
          map("visible", ImmutableMap.of("dfs.namenode.name.dir", "false"))),
        "oozie-env", RecommendationResponse.BlueprintConfigurations.create(
          map("oozie_heapsize", "2048"),
          new HashMap<>()),
        "spark2-defaults", RecommendationResponse.BlueprintConfigurations.create(
          map("spark.yarn.queue", "spark2"),
          new HashMap<>()));

    Map<String, RecommendationResponse.BlueprintConfigurations> recommendedConfigsForStackDefaults =
      ImmutableMap.of(
        "oozie-env", RecommendationResponse.BlueprintConfigurations.create(
          ImmutableMap.of("oozie_heapsize", "2048"),
          ImmutableMap.of()),
        "spark2-defaults", RecommendationResponse.BlueprintConfigurations.create(
          ImmutableMap.of("spark.yarn.queue", "spark2"),
          ImmutableMap.of()));

    StackAdvisorAdapter.removeNonStackConfigRecommendations(stackConfig, recommendedConfigs);

    assertEquals(recommendedConfigsForStackDefaults, recommendedConfigs);
  }

  @Test
  public void getLayoutRecommendationInfo() {
    Map<String, Map<String, Set<String>>> newServices = ImmutableMap.of(
      "KAFKA", ImmutableMap.of(
        "KAFKA_BROKER", ImmutableSet.of("c7401")),
      "SPARK2", ImmutableMap.of(
        "SPARK2_JOBHISTORYSERVER", ImmutableSet.of("c7402"),
        "SPARK2_CLIENT", ImmutableSet.of("c7403", "c7404")),
      "OOZIE", ImmutableMap.of(
        "OOZIE_SERVER", ImmutableSet.of("c7401"),
        "OOZIE_CLIENT", ImmutableSet.of("c7403", "c7404")));

    AddServiceRequest request = request(ConfigRecommendationStrategy.ALWAYS_APPLY);
    AddServiceInfo info = addServiceInfoBuilder
      .setRequest(request)
      .setStack(stack)
      .setConfig(Configuration.newEmpty())
      .setNewServices(newServices)
      .build(); // No LayoutReommendationInfo -> needs to be calculated

    LayoutRecommendationInfo layoutRecommendationInfo = adapter.getLayoutRecommendationInfo(info);
    layoutRecommendationInfo.getAllServiceLayouts();

    assertEquals(
      ImmutableMap.of(
        "host_group_1", ImmutableSet.of("c7401"),
        "host_group_2", ImmutableSet.of("c7402"),
        "host_group_3", ImmutableSet.of("c7403", "c7404")),
      layoutRecommendationInfo.getHostGroups());

    assertEquals(
      ImmutableMap.<String, Map<String, Set<String>>>builder()
        .put("KAFKA", ImmutableMap.of(
          "KAFKA_BROKER", ImmutableSet.of("c7401")))
        .put("SPARK2", ImmutableMap.of(
          "SPARK2_JOBHISTORYSERVER", ImmutableSet.of("c7402"),
          "SPARK2_CLIENT", ImmutableSet.of("c7403", "c7404")))
        .put("OOZIE", ImmutableMap.of(
          "OOZIE_SERVER", ImmutableSet.of("c7401"),
          "OOZIE_CLIENT", ImmutableSet.of("c7403", "c7404")))
        .put("HDFS", ImmutableMap.of(
          "NAMENODE", ImmutableSet.of("c7401"),
          "HDFS_CLIENT", ImmutableSet.of("c7401", "c7402")))
        .put("ZOOKEEPER", ImmutableMap.of(
          "ZOOKEEPER_SERVER", ImmutableSet.of("c7401"),
          "ZOOKEEPER_CLIENT", ImmutableSet.of("c7401", "c7402")))
        .put("MAPREDUCE2", ImmutableMap.of(
          "HISTORYSERVER", ImmutableSet.of("c7401")))
        .build(),
      layoutRecommendationInfo.getAllServiceLayouts());
  }

  private static AddServiceRequest request(ConfigRecommendationStrategy strategy) {
    return new AddServiceRequest(null, strategy, null, null, null, null, null, null, null, null, null);
  }

}
