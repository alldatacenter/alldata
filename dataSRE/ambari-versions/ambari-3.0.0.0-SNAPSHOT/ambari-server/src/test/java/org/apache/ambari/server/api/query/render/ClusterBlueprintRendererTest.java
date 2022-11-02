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

package org.apache.ambari.server.api.query.render;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.ambari.server.api.query.QueryInfo;
import org.apache.ambari.server.api.resources.ClusterResourceDefinition;
import org.apache.ambari.server.api.resources.HostComponentResourceDefinition;
import org.apache.ambari.server.api.resources.HostResourceDefinition;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.KerberosHelperImpl;
import org.apache.ambari.server.controller.internal.ArtifactResourceProvider;
import org.apache.ambari.server.controller.internal.BlueprintExportType;
import org.apache.ambari.server.controller.internal.ClusterControllerImpl;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.cluster.ClustersImpl;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.topology.AmbariContext;
import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.Component;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.HostGroup;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 * ClusterBlueprintRenderer unit tests.
 */
@SuppressWarnings("unchecked")
@RunWith(PowerMockRunner.class)
@PrepareForTest({AmbariContext.class, AmbariServer.class})
public class ClusterBlueprintRendererTest {

  private static final ClusterTopology topology = createNiceMock(ClusterTopology.class);
  private static final ClusterController clusterController = createNiceMock(ClusterControllerImpl.class);

  private static final AmbariContext ambariContext = createNiceMock(AmbariContext.class);
  private static final Cluster cluster = createNiceMock(Cluster.class);
  private static final Clusters clusters = createNiceMock(ClustersImpl.class);
  private static final AmbariManagementController controller = createNiceMock(AmbariManagementControllerImpl.class);
  private static final KerberosHelper kerberosHelper = createNiceMock(KerberosHelperImpl.class);
  private static final KerberosDescriptor kerberosDescriptor = createNiceMock(KerberosDescriptor.class);

  private static final Blueprint blueprint = createNiceMock(Blueprint.class);
  private static final Stack stack = createNiceMock(Stack.class);
  private static final HostGroup group1 = createNiceMock(HostGroup.class);
  private static final HostGroup group2 = createNiceMock(HostGroup.class);

  private static final Configuration emptyConfiguration = new Configuration(new HashMap<>(), new HashMap<>());

  private static final Map<String, Map<String, String>> clusterProps = new HashMap<>();
  private static final Map<String, Map<String, Map<String, String>>> clusterAttributes =
    new HashMap<>();

  private static final Configuration clusterConfig = new Configuration(clusterProps, clusterAttributes);
  private final ClusterBlueprintRenderer minimalRenderer = new ClusterBlueprintRenderer(BlueprintExportType.MINIMAL);
  private final ClusterBlueprintRenderer fullRenderer = new ClusterBlueprintRenderer(BlueprintExportType.FULL);

  @Before
  public void setup() throws Exception {

    Map<String, String> clusterTypeProps = new HashMap<>();
    clusterProps.put("test-type-one", clusterTypeProps);
    clusterTypeProps.put("propertyOne", "valueOne");

    Map<String, Map<String, String>> clusterTypeAttributes = new HashMap<>();
    clusterAttributes.put("test-type-one", clusterTypeAttributes);
    Map<String, String> clusterAttributeProps = new HashMap<>();
    clusterAttributeProps.put("propertyOne", "true");
    clusterTypeAttributes.put("final", clusterAttributeProps);

    Collection<Component> group1Components = Arrays.asList(
        new Component("JOBTRACKER"), new Component("TASKTRACKER"), new Component("NAMENODE"), new Component("DATANODE"), new Component("AMBARI_SERVER"));

    Collection<Component> group2Components = Arrays.asList(new Component("TASKTRACKER"), new Component("DATANODE"));

    Map<String, HostGroup> hostGroups = new HashMap<>();
    hostGroups.put("host_group_1", group1);
    hostGroups.put("host_group_2", group2);

    HostGroupInfo group1Info = new HostGroupInfo("host_group_1");
    group1Info.addHost("host1");
    group1Info.setConfiguration(emptyConfiguration);
    HostGroupInfo group2Info = new HostGroupInfo("host_group_2");
    Map<String, HostGroupInfo> groupInfoMap = new HashMap<>();
    group2Info.addHosts(Arrays.asList("host2", "host3"));
    group2Info.setConfiguration(emptyConfiguration);
    groupInfoMap.put("host_group_1", group1Info);
    groupInfoMap.put("host_group_2", group2Info);

    expect(topology.isNameNodeHAEnabled()).andReturn(false).anyTimes();
    expect(topology.getConfiguration()).andReturn(clusterConfig).anyTimes();
    expect(topology.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(topology.getHostGroupInfo()).andReturn(groupInfoMap).anyTimes();
    expect(blueprint.getStack()).andReturn(stack).anyTimes();
    expect(blueprint.getHostGroups()).andReturn(hostGroups).anyTimes();
    expect(blueprint.getHostGroup("host_group_1")).andReturn(group1).anyTimes();
    expect(blueprint.getHostGroup("host_group_2")).andReturn(group2).anyTimes();
    expect(blueprint.getServices()).andReturn(ImmutableSet.of("HDFS", "YARN")).anyTimes();
    expect(stack.getName()).andReturn("HDP").anyTimes();
    expect(stack.getVersion()).andReturn("1.3.3").anyTimes();
    expect(stack.getConfiguration()).andReturn(Configuration.newEmpty()).anyTimes();
    expect(group1.getName()).andReturn("host_group_1").anyTimes();
    expect(group2.getName()).andReturn("host_group_2").anyTimes();
    expect(group1.getComponents()).andReturn(group1Components).anyTimes();
    expect(group2.getComponents()).andReturn(group2Components).anyTimes();

    expect(topology.getAmbariContext()).andReturn(ambariContext).anyTimes();
    expect(topology.getClusterId()).andReturn(1L).anyTimes();
    PowerMock.mockStatic(AmbariServer.class);
    expect(AmbariServer.getController()).andReturn(controller).anyTimes();
    PowerMock.replay(AmbariServer.class);
    expect(clusters.getCluster("clusterName")).andReturn(cluster).anyTimes();
    expect(controller.getKerberosHelper()).andReturn(kerberosHelper).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(kerberosHelper.getKerberosDescriptor(cluster, false)).andReturn(kerberosDescriptor).anyTimes();
    Set<String> properties = new HashSet<>();
    properties.add("core-site/hadoop.security.auth_to_local");
    expect(kerberosDescriptor.getAllAuthToLocalProperties()).andReturn(properties).anyTimes();
    expect(ambariContext.getClusterName(1L)).andReturn("clusterName").anyTimes();
    replay(topology, blueprint, stack, group1, group2, ambariContext, clusters, controller, kerberosHelper, cluster, kerberosDescriptor);
  }

  private void setupMocksForKerberosEnabledCluster() throws Exception {

    AmbariContext ambariContext = createNiceMock(AmbariContext.class);
    expect(ambariContext.getClusterName(anyLong())).andReturn("clusterName").anyTimes();

    PowerMock.mockStatic(AmbariContext.class);
    expect(AmbariContext.getClusterController()).andReturn(clusterController).anyTimes();
    expect(AmbariContext.getController()).andReturn(controller).anyTimes();

    reset(topology);

    HostGroupInfo group1Info = new HostGroupInfo("host_group_1");
    group1Info.addHost("host1");
    group1Info.setConfiguration(emptyConfiguration);
    HostGroupInfo group2Info = new HostGroupInfo("host_group_2");
    Map<String, HostGroupInfo> groupInfoMap = new HashMap<>();
    group2Info.addHosts(Arrays.asList("host2", "host3"));
    group2Info.setConfiguration(emptyConfiguration);
    groupInfoMap.put("host_group_1", group1Info);
    groupInfoMap.put("host_group_2", group2Info);

    expect(topology.isNameNodeHAEnabled()).andReturn(false).anyTimes();
    expect(topology.getConfiguration()).andReturn(clusterConfig).anyTimes();
    expect(topology.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(topology.getHostGroupInfo()).andReturn(groupInfoMap).anyTimes();
    expect(topology.getClusterId()).andReturn(1L).anyTimes();
    expect(topology.getAmbariContext()).andReturn(ambariContext).anyTimes();
    expect(topology.isClusterKerberosEnabled()).andReturn(true).anyTimes();

    ResourceProvider resourceProvider = createStrictMock(ResourceProvider.class);
    expect(clusterController.ensureResourceProvider(Resource.Type.Artifact)).andReturn(resourceProvider).once();

    Resource resource = createStrictMock(Resource.class);
    Set<Resource> result = Collections.singleton(resource);

    expect(resourceProvider.getResources(anyObject(Request.class), anyObject(Predicate.class))).andReturn(result).once();

    Map<String, Map<String, Object>> resourcePropertiesMap = new HashMap<>();
    resourcePropertiesMap.put(ArtifactResourceProvider.ARTIFACT_DATA_PROPERTY, Collections.emptyMap());
    Map<String, Object> propertiesMap = new HashMap<>();
    propertiesMap.put("testProperty", "testValue");
    resourcePropertiesMap.put(ArtifactResourceProvider.ARTIFACT_DATA_PROPERTY + "/properties", propertiesMap);

    expect(resource.getPropertiesMap()).andReturn(resourcePropertiesMap).once();

    PowerMock.replay(AmbariContext.class);
    replay(ambariContext, topology, clusterController, resource, resourceProvider);
  }


  @After
  public void tearDown() {
    verify(topology, blueprint, stack, group1, group2, ambariContext, clusters, controller, kerberosHelper, cluster, kerberosDescriptor);
    reset(topology, blueprint, stack, group1, group2, ambariContext, clusters, controller, kerberosHelper, cluster, kerberosDescriptor);
  }

  @Test
  public void testFinalizeProperties__instance() {
    QueryInfo rootQuery = new QueryInfo(new ClusterResourceDefinition(), new HashSet<>());
    TreeNode<QueryInfo> queryTree = new TreeNodeImpl<>(null, rootQuery, "Cluster");
    rootQuery.getProperties().add("foo/bar");
    rootQuery.getProperties().add("prop1");

    QueryInfo hostInfo = new QueryInfo(new HostResourceDefinition(), new HashSet<>());
    queryTree.addChild(hostInfo, "Host");

    QueryInfo hostComponentInfo = new QueryInfo(new HostComponentResourceDefinition(), new HashSet<>());
    queryTree.getChild("Host").addChild(hostComponentInfo, "HostComponent");

    TreeNode<Set<String>> propertyTree = fullRenderer.finalizeProperties(queryTree, false);

    Set<String> rootProperties = propertyTree.getObject();
    assertEquals(2, rootProperties.size());
    assertNotNull(propertyTree.getChild("Host"));
    assertTrue(propertyTree.getChild("Host").getObject().isEmpty());
    assertNotNull(propertyTree.getChild("Host/HostComponent"));
    assertEquals(1, propertyTree.getChild("Host/HostComponent").getObject().size());
    assertTrue(propertyTree.getChild("Host/HostComponent").getObject().contains("HostRoles/component_name"));
  }

  @Test
  public void clusterWithDefaultSettings() {
    Stack stack = stackForSettingsTest();
    TreeNode<Resource> clusterNode = clusterWith(stack,
      stack.getComponents(),
      defaultCredentialStoreSettings(),
      defaultRecoverySettings());

    Collection<Map<String, Object>> settings = fullRenderer.getSettings(clusterNode, stack);
    assertEquals(Lists.newArrayList(
      ImmutableMap.of(ClusterBlueprintRenderer.SERVICE_SETTINGS, ImmutableSet.of()),
      ImmutableMap.of(ClusterBlueprintRenderer.COMPONENT_SETTINGS, ImmutableSet.of())
    ), settings);

    assertEquals(ImmutableList.of(), minimalRenderer.getSettings(clusterNode, stack));
  }

  @Test
  public void clusterWithCustomSettings() {
    Stack stack = stackForSettingsTest();
    TreeNode<Resource> clusterNode = clusterWith(stack,
      stack.getComponents(),
      customCredentialStoreSettingFor(stack, "service1", "service2"),
      customRecoverySettingsFor(stack, "component1", "component2"));

    Collection<Map<String, Object>> settings = fullRenderer.getSettings(clusterNode, stack);
    assertEquals(Lists.newArrayList(
      ImmutableMap.of(ClusterBlueprintRenderer.SERVICE_SETTINGS, ImmutableSet.of(
        ImmutableMap.of(
          "name", "service1",
          ClusterBlueprintRenderer.CREDENTIAL_STORE_ENABLED, ClusterBlueprintRenderer.FALSE
        ),
        ImmutableMap.of(
          "name", "service2",
          ClusterBlueprintRenderer.CREDENTIAL_STORE_ENABLED, ClusterBlueprintRenderer.TRUE
        )
      )),
      ImmutableMap.of(ClusterBlueprintRenderer.COMPONENT_SETTINGS, ImmutableSet.of(
        ImmutableMap.of("name", "component1", ClusterBlueprintRenderer.RECOVERY_ENABLED, ClusterBlueprintRenderer.FALSE),
        ImmutableMap.of("name", "component2", ClusterBlueprintRenderer.RECOVERY_ENABLED, ClusterBlueprintRenderer.TRUE)
      ))
    ), settings);

    assertEquals(settings, minimalRenderer.getSettings(clusterNode, stack));
  }

  @Test
  public void clusterWithRecoveryDisabled() {
    Stack stack = stackForSettingsTest();
    TreeNode<Resource> clusterNode = clusterWith(stack,
      stack.getComponents(),
      defaultCredentialStoreSettings(),
      customRecoverySettingsFor(stack, "component1"));

    Collection<Map<String, Object>> settings = fullRenderer.getSettings(clusterNode, stack);

    assertEquals(Lists.newArrayList(
      ImmutableMap.of(ClusterBlueprintRenderer.SERVICE_SETTINGS, ImmutableSet.of()),
      ImmutableMap.of(ClusterBlueprintRenderer.COMPONENT_SETTINGS, ImmutableSet.of(
        ImmutableMap.of("name", "component1", ClusterBlueprintRenderer.RECOVERY_ENABLED, ClusterBlueprintRenderer.FALSE)
      ))
    ), settings);
    assertEquals(Lists.newArrayList(
      ImmutableMap.of(ClusterBlueprintRenderer.COMPONENT_SETTINGS, ImmutableSet.of(
        ImmutableMap.of("name", "component1", ClusterBlueprintRenderer.RECOVERY_ENABLED, ClusterBlueprintRenderer.FALSE)
      ))
    ), minimalRenderer.getSettings(clusterNode, stack));
  }

  private static TreeNode<Resource> clusterWith(Stack stack,
    Map<String, Collection<String>> componentsByService,
    Map<String, Map<String, String>> serviceSettings,
    Map<String, Map<String, Map<String, String>>> componentSettings
  ) {
    TreeNode<Resource> clusterTree = new ResultImpl(true).getResultTree();

    TreeNode<Resource> servicesTree = clusterTree.addChild(null, "services");
    servicesTree.setProperty("isCollection", "true");

    for (Map.Entry<String, Collection<String>> serviceEntry : componentsByService.entrySet()) {
      String serviceName = serviceEntry.getKey();
      Optional<ServiceInfo> serviceInfo = stack.getServiceInfo(serviceName);
      if (serviceInfo.isPresent()) {
        Resource serviceResource = new ResourceImpl(Resource.Type.Service);
        serviceResource.setProperty("ServiceInfo/service_name", serviceName);
        serviceResource.setProperty("ServiceInfo/credential_store_supported", serviceInfo.get().isCredentialStoreSupported());
        serviceResource.setProperty("ServiceInfo/credential_store_enabled",
          serviceSettings
            .getOrDefault(serviceName, ImmutableMap.of())
            .getOrDefault(ClusterBlueprintRenderer.CREDENTIAL_STORE_ENABLED, String.valueOf(serviceInfo.get().isCredentialStoreEnabled())));
        TreeNode<Resource> serviceTree = servicesTree.addChild(serviceResource, serviceName);
        TreeNode<Resource> componentsTree = serviceTree.addChild(null, "components");
        componentsTree.setProperty("isCollection", "true");

        for (String componentName : serviceEntry.getValue()) {
          ComponentInfo componentInfo = serviceInfo.get().getComponentByName(componentName);
          Resource componentResource = new ResourceImpl(Resource.Type.Component);
          componentResource.setProperty("ServiceComponentInfo/component_name", componentName);
          componentResource.setProperty("ServiceComponentInfo/cluster_name", "testCluster");
          componentResource.setProperty("ServiceComponentInfo/service_name", serviceName);
          componentResource.setProperty("ServiceComponentInfo/recovery_enabled",
            componentSettings
              .getOrDefault(serviceName, ImmutableMap.of())
              .getOrDefault(componentName, ImmutableMap.of())
              .getOrDefault(ClusterBlueprintRenderer.RECOVERY_ENABLED, String.valueOf(componentInfo.isRecoveryEnabled())));
          componentsTree.addChild(componentResource, componentName);
        }
      }
    }

    return clusterTree;
  }

  private static Map<String, Map<String, String>> defaultCredentialStoreSettings() {
    return ImmutableMap.of();
  }

  private static Map<String, Map<String, String>> customCredentialStoreSettingFor(Stack stack, String... services) {
    Map<String, Map<String, String>> result = new LinkedHashMap<>();
    for (String service : services) {
      ServiceInfo serviceInfo = stack.getServiceInfo(service).orElseThrow(IllegalStateException::new);
      result.put(service, ImmutableMap.of(
        ClusterBlueprintRenderer.CREDENTIAL_STORE_ENABLED, String.valueOf(!serviceInfo.isCredentialStoreEnabled())
      ));
    }
    return result;
  }

  private static Map<String, Map<String, Map<String, String>>> defaultRecoverySettings() {
    return ImmutableMap.of();
  }

  private static Map<String, Map<String, Map<String, String>>> customRecoverySettingsFor(Stack stack, String... components) {
    Map<String, Map<String, Map<String, String>>> result = new LinkedHashMap<>();
    for (String component : components) {
      ComponentInfo componentInfo = stack.getComponentInfo(component);
      String service = stack.getServiceForComponent(component);
      result
        .computeIfAbsent(service, __ -> new HashMap<>())
        .put(component, ImmutableMap.of(
          ClusterBlueprintRenderer.RECOVERY_ENABLED, String.valueOf(!componentInfo.isRecoveryEnabled())
        ));
    }
    return result;
  }

  private static Stack stackForSettingsTest() {
    Stack stack = createNiceMock(Stack.class);

    ServiceInfo service1 = createNiceMock(ServiceInfo.class);
    expect(service1.isCredentialStoreEnabled()).andReturn(true).anyTimes();
    expect(stack.getServiceInfo("service1")).andReturn(Optional.of(service1)).anyTimes();

    ComponentInfo component1 = createNiceMock(ComponentInfo.class);
    expect(component1.isRecoveryEnabled()).andReturn(true).anyTimes();
    expect(service1.getComponentByName("component1")).andReturn(component1).anyTimes();
    expect(stack.getServiceForComponent("component1")).andReturn("service1").anyTimes();
    expect(stack.getComponentInfo("component1")).andReturn(component1).anyTimes();

    ComponentInfo component2 = createNiceMock(ComponentInfo.class);
    expect(component2.isRecoveryEnabled()).andReturn(false).anyTimes();
    expect(service1.getComponentByName("component2")).andReturn(component2).anyTimes();
    expect(stack.getServiceForComponent("component2")).andReturn("service1").anyTimes();
    expect(stack.getComponentInfo("component2")).andReturn(component2).anyTimes();

    expect(service1.getComponents()).andReturn(ImmutableList.of(component1, component2)).anyTimes();

    ServiceInfo service2 = createNiceMock(ServiceInfo.class);
    expect(service2.isCredentialStoreEnabled()).andReturn(false).anyTimes();
    expect(stack.getServiceInfo("service2")).andReturn(Optional.of(service2)).anyTimes();
    ComponentInfo component3 = createNiceMock(ComponentInfo.class);
    expect(component3.isRecoveryEnabled()).andReturn(false).anyTimes();
    expect(service2.getComponentByName("component3")).andReturn(component3).anyTimes();
    expect(stack.getServiceForComponent("component3")).andReturn("service2").anyTimes();
    expect(stack.getComponentInfo("component3")).andReturn(component3).anyTimes();

    expect(service2.getComponents()).andReturn(ImmutableList.of(component3)).anyTimes();

    expect(stack.getComponents()).andReturn(ImmutableMap.of(
      "service1", ImmutableSet.of("component1", "component2"),
      "service2", ImmutableSet.of("component3")
    )).anyTimes();

    replay(stack, service1, service2, component1, component2, component3);

    return stack;
  }

  @Test
  public void testFinalizeProperties__instance_noComponentNode() {
    QueryInfo rootQuery = new QueryInfo(new ClusterResourceDefinition(), new HashSet<>());
    TreeNode<QueryInfo> queryTree = new TreeNodeImpl<>(null, rootQuery, "Cluster");
    rootQuery.getProperties().add("foo/bar");
    rootQuery.getProperties().add("prop1");

    TreeNode<Set<String>> propertyTree = fullRenderer.finalizeProperties(queryTree, false);

    Set<String> rootProperties = propertyTree.getObject();
    assertEquals(2, rootProperties.size());
    assertNotNull(propertyTree.getChild("Host"));
    assertTrue(propertyTree.getChild("Host").getObject().isEmpty());
    assertNotNull(propertyTree.getChild("Host/HostComponent"));
    assertEquals(1, propertyTree.getChild("Host/HostComponent").getObject().size());
    assertTrue(propertyTree.getChild("Host/HostComponent").getObject().contains("HostRoles/component_name"));
  }

  @Test
  public void testFinalizeResult_kerberos() throws Exception{

    setupMocksForKerberosEnabledCluster();

    Result result = new ResultImpl(true);
    createClusterResultTree(result.getResultTree());

    ClusterBlueprintRenderer renderer = new TestBlueprintRenderer(topology, BlueprintExportType.FULL);
    Result blueprintResult = renderer.finalizeResult(result);

    TreeNode<Resource> blueprintTree = blueprintResult.getResultTree();
    assertNull(blueprintTree.getStringProperty("isCollection"));
    assertEquals(1, blueprintTree.getChildren().size());

    TreeNode<Resource> blueprintNode = blueprintTree.getChildren().iterator().next();
    assertEquals(0, blueprintNode.getChildren().size());
    Resource blueprintResource = blueprintNode.getObject();
    Map<String, Map<String, Object>> properties = blueprintResource.getPropertiesMap();

    assertEquals("HDP", properties.get("Blueprints").get("stack_name"));
    assertEquals("1.3.3", properties.get("Blueprints").get("stack_version"));

    Map<String, Object> securityProperties = (Map<String, Object>) properties.get("Blueprints").get("security");
    assertEquals("KERBEROS", securityProperties.get("type"));
    assertNotNull(((Map<String, Object>) securityProperties.get("kerberos_descriptor")).get("properties"));
  }

  @Test
  public void testFinalizeResult() throws Exception{

    Result result = new ResultImpl(true);
    createClusterResultTree(result.getResultTree());

    ClusterBlueprintRenderer renderer = new TestBlueprintRenderer(topology, BlueprintExportType.FULL);
    Result blueprintResult = renderer.finalizeResult(result);

    TreeNode<Resource> blueprintTree = blueprintResult.getResultTree();
    assertNull(blueprintTree.getStringProperty("isCollection"));
    assertEquals(1, blueprintTree.getChildren().size());

    TreeNode<Resource> blueprintNode = blueprintTree.getChildren().iterator().next();
    assertEquals(0, blueprintNode.getChildren().size());
    Resource blueprintResource = blueprintNode.getObject();
    Map<String, Map<String, Object>> properties = blueprintResource.getPropertiesMap();

    assertEquals("HDP", properties.get("Blueprints").get("stack_name"));
    assertEquals("1.3.3", properties.get("Blueprints").get("stack_version"));

    Collection<Map<String, Object>> host_groups = (Collection<Map<String, Object>>) properties.get("").get("host_groups");
    assertEquals(2, host_groups.size());

    for (Map<String, Object> hostGroupProperties : host_groups) {
      String host_group_name = (String) hostGroupProperties.get("name");
      if (host_group_name.equals("host_group_1")) {
        assertEquals("1", hostGroupProperties.get("cardinality"));

        Collection<Map<String, String>> components = (Collection<Map<String, String>>) hostGroupProperties.get("components");
        // 4 specified components and ambari server
        assertEquals(5, components.size());

        Set<String> expectedValues = ImmutableSet.of("JOBTRACKER", "TASKTRACKER", "NAMENODE", "DATANODE", "AMBARI_SERVER");
        Set<String> actualValues = new HashSet<>();

        for (Map<String, String> componentProperties : components) {
          assertEquals(1, componentProperties.size());
          actualValues.add(componentProperties.get("name"));
        }
        assertEquals(expectedValues, actualValues);
      } else if (host_group_name.equals("host_group_2")) {
        // cardinality is 2 because 2 hosts share same topology
        assertEquals("2", hostGroupProperties.get("cardinality"));

        Collection<Map<String, String>> components = (Collection<Map<String, String>>) hostGroupProperties.get("components");
        assertEquals(2, components.size());

        Set<String> expectedValues = ImmutableSet.of("TASKTRACKER", "DATANODE");
        Set<String> actualValues = new HashSet<>();

        for (Map<String, String> componentProperties : components) {
          assertEquals(1, componentProperties.size());
          actualValues.add(componentProperties.get("name"));
        }
        assertEquals(expectedValues, actualValues);
      }
    }
  }

  @Test
  public void testFinalizeResultWithAttributes() throws Exception{
    ServiceInfo hdfsService = new ServiceInfo();
    hdfsService.setName("HDFS");
    ServiceInfo mrService = new ServiceInfo();
    mrService.setName("MAPREDUCE");

    Result result = new ResultImpl(true);
    Map<String, Object> testDesiredConfigMap =
      new HashMap<>();

    DesiredConfig testDesiredConfig =
      new DesiredConfig();

    testDesiredConfig.setTag("test-tag-one");
    testDesiredConfigMap.put("test-type-one", testDesiredConfig);

    createClusterResultTree(result.getResultTree(), testDesiredConfigMap);

    ClusterBlueprintRenderer renderer = new TestBlueprintRenderer(topology, BlueprintExportType.FULL);
    Result blueprintResult = renderer.finalizeResult(result);

    TreeNode<Resource> blueprintTree = blueprintResult.getResultTree();
    assertNull(blueprintTree.getStringProperty("isCollection"));
    assertEquals(1, blueprintTree.getChildren().size());

    TreeNode<Resource> blueprintNode = blueprintTree.getChildren().iterator().next();
    assertEquals(0, blueprintNode.getChildren().size());
    Resource blueprintResource = blueprintNode.getObject();
    Map<String, Map<String, Object>> properties = blueprintResource.getPropertiesMap();

    assertEquals("HDP", properties.get("Blueprints").get("stack_name"));
    assertEquals("1.3.3", properties.get("Blueprints").get("stack_version"));

    Collection<Map<String, Object>> host_groups = (Collection<Map<String, Object>>) properties.get("").get("host_groups");
    assertEquals(2, host_groups.size());

    for (Map<String, Object> hostGroupProperties : host_groups) {
      String host_group_name = (String) hostGroupProperties.get("name");
      if (host_group_name.equals("host_group_1")) {
        assertEquals("1", hostGroupProperties.get("cardinality"));

        Collection<Map<String, String>> components = (Collection<Map<String, String>>) hostGroupProperties.get("components");
        // 4 specified components and ambari server
        assertEquals(5, components.size());

        Set<String> expectedValues = ImmutableSet.of("JOBTRACKER", "TASKTRACKER", "NAMENODE", "DATANODE", "AMBARI_SERVER");
        Set<String> actualValues = new HashSet<>();

        for (Map<String, String> componentProperties : components) {
          assertEquals(1, componentProperties.size());
          actualValues.add(componentProperties.get("name"));
        }
        assertEquals(expectedValues, actualValues);
      } else if (host_group_name.equals("host_group_2")) {
        // cardinality is 2 because 2 hosts share same topology
        assertEquals("2", hostGroupProperties.get("cardinality"));

        Collection<Map<String, String>> components = (Collection<Map<String, String>>) hostGroupProperties.get("components");
        assertEquals(2, components.size());

        Set<String> expectedValues = ImmutableSet.of("TASKTRACKER", "DATANODE");
        Set<String> actualValues = new HashSet<>();

        for (Map<String, String> componentProperties : components) {
          assertEquals(1, componentProperties.size());
          actualValues.add(componentProperties.get("name"));
        }
        assertEquals(expectedValues, actualValues);
      }
    }

    List<Map<String, Map<String, Map<String, ?>>>> configurationsResult =
      (List<Map<String, Map<String, Map<String, ?>>>>)blueprintResource.getPropertyValue("configurations");

    assertEquals("Incorrect number of config maps added",
      1, configurationsResult.size());

    Map<String, Map<String, ?>> configMap =
      configurationsResult.iterator().next().get("test-type-one");

    assertNotNull("Expected config map was not included", configMap);

    assertEquals("Incorrect number of maps added under expected type",
      2, configMap.size());

    assertTrue("Expected properties map was not found",
      configMap.containsKey("properties"));
    assertTrue("Expected properties_attributes map was not found",
      configMap.containsKey("properties_attributes"));

    Map<String, ?> propertiesResult =
      configMap.get("properties");
    assertEquals("Incorrect number of config properties found",
      1, propertiesResult.size());

    Map<String, ?> attributesResult =
      configMap.get("properties_attributes");
    assertEquals("Incorrect number of config attributes found",
      1, attributesResult.size());

    // verify the correct properties were added to the exported Blueprint
    assertEquals("Incorrect property value included",
      "valueOne", propertiesResult.get("propertyOne"));

    // verify that the expected attributes were added to the exported Blueprint
    assertNotNull("Expected attribute not found in exported Blueprint",
      attributesResult.get("final"));

    assertTrue("Attribute type map was not included",
      attributesResult.get("final") instanceof Map);

    Map<String, ?> finalMap =
      (Map<String, ?>)attributesResult.get("final");

    assertEquals("Attribute value is not correct",
        "true", finalMap.get("propertyOne"));

  }

  @Test
  public void testClusterRendererDefaults() {
    assertFalse("ClusterBlueprintRenderer should not require property provider input",
      fullRenderer.requiresPropertyProviderInput());
  }

  //todo: collection resource

  private void createClusterResultTree(TreeNode<Resource> resultTree) throws Exception {
    createClusterResultTree(resultTree, null);
  }

  private void createClusterResultTree(TreeNode<Resource> resultTree, final Map<String, Object> desiredConfig) throws Exception{
    Resource clusterResource = new ResourceImpl(Resource.Type.Cluster) {
      @Override
      public Map<String, Map<String, Object>> getPropertiesMap() {
        Map<String, Map<String, Object>> originalMap =
          super.getPropertiesMap();

        if (desiredConfig == null) {
          // override the properties map for simpler testing
          originalMap.put("Clusters/desired_configs", Collections.emptyMap());
        } else {
          // allow for unit tests to customize this, needed for attributes export testing
          originalMap.put("Clusters/desired_configs", desiredConfig);
        }


        return originalMap;
      }

    };

    clusterResource.setProperty("Clusters/cluster_name", "testCluster");
    clusterResource.setProperty("Clusters/version", "HDP-1.3.3");

    TreeNode<Resource> clusterTree = resultTree.addChild(clusterResource, "Cluster:1");

    // add empty services resource for basic unit testing
    Resource servicesResource = new ResourceImpl(Resource.Type.Service);
    clusterTree.addChild(servicesResource, "services");


    Resource configurationsResource = new ResourceImpl(Resource.Type.Configuration);
    TreeNode<Resource> configurations = clusterTree.addChild(configurationsResource, "configurations");
    Resource resourceOne = new ResourceImpl(Resource.Type.Configuration) {
      @Override
      public Map<String, Map<String, Object>> getPropertiesMap() {
        Map<String, Map<String, Object>> originalMap =
        super.getPropertiesMap();

        // return null for properties, to simulate upgrade case
        originalMap.put("properties", null);

        return originalMap;
      }
    };

    resourceOne.setProperty("type", "mapreduce-log4j");

    configurations.addChild(resourceOne, "resourceOne");


    Resource resourceTwo = new ResourceImpl(Resource.Type.Configuration) {
      @Override
      public Map<String, Map<String, Object>> getPropertiesMap() {
        Map<String, Map<String, Object>> originalMap =
          super.getPropertiesMap();

        // return test properties, to simulate valid configuration entry
        originalMap.put("properties", Collections.singletonMap("propertyOne", "valueOne"));
        originalMap.put("properties_attributes", Collections.singletonMap("final", Collections.singletonMap("propertyOne", "true")));

        return originalMap;
      }
    };

    resourceTwo.setProperty("type", "test-type-one");
    resourceTwo.setProperty("tag", "test-tag-one");

    configurations.addChild(resourceTwo, "resourceTwo");

    Resource blueprintOne = new ResourceImpl(Resource.Type.Blueprint);
    blueprintOne.setProperty("Blueprints/blueprint_name", "blueprint-testCluster");
    clusterTree.addChild(blueprintOne, "Blueprints");

    TreeNode<Resource> hostsTree = clusterTree.addChild(null, "hosts");
    hostsTree.setProperty("isCollection", "true");

    // host 1 : ambari host
    Resource hostResource = new ResourceImpl(Resource.Type.Host);
    hostResource.setProperty("Hosts/host_name", getLocalHostName());
    TreeNode<Resource> hostTree = hostsTree.addChild(hostResource, "Host:1");

    TreeNode<Resource> hostComponentsTree = hostTree.addChild(null, "host_components");
    hostComponentsTree.setProperty("isCollection", "true");

    // host 1 components
    Resource nnComponentResource = new ResourceImpl(Resource.Type.HostComponent);
    nnComponentResource.setProperty("HostRoles/component_name", "NAMENODE");

    Resource dnComponentResource = new ResourceImpl(Resource.Type.HostComponent);
    dnComponentResource.setProperty("HostRoles/component_name", "DATANODE");

    Resource jtComponentResource = new ResourceImpl(Resource.Type.HostComponent);
    jtComponentResource.setProperty("HostRoles/component_name", "JOBTRACKER");

    Resource ttComponentResource = new ResourceImpl(Resource.Type.HostComponent);
    ttComponentResource.setProperty("HostRoles/component_name", "TASKTRACKER");

    hostComponentsTree.addChild(nnComponentResource, "HostComponent:1");
    hostComponentsTree.addChild(dnComponentResource, "HostComponent:2");
    hostComponentsTree.addChild(jtComponentResource, "HostComponent:3");
    hostComponentsTree.addChild(ttComponentResource, "HostComponent:4");

    // host 2
    Resource host2Resource = new ResourceImpl(Resource.Type.Host);
    host2Resource.setProperty("Hosts/host_name", "testHost2");
    TreeNode<Resource> host2Tree = hostsTree.addChild(host2Resource, "Host:2");

    TreeNode<Resource> host2ComponentsTree = host2Tree.addChild(null, "host_components");
    host2ComponentsTree.setProperty("isCollection", "true");

    // host 2 components
    host2ComponentsTree.addChild(dnComponentResource, "HostComponent:1");
    host2ComponentsTree.addChild(ttComponentResource, "HostComponent:2");

    // host 3 : same topology as host 2
    Resource host3Resource = new ResourceImpl(Resource.Type.Host);
    host3Resource.setProperty("Hosts/host_name", "testHost3");
    TreeNode<Resource> host3Tree = hostsTree.addChild(host3Resource, "Host:3");

    TreeNode<Resource> host3ComponentsTree = host3Tree.addChild(null, "host_components");
    host3ComponentsTree.setProperty("isCollection", "true");

    // host 3 components
    host3ComponentsTree.addChild(dnComponentResource, "HostComponent:1");
    host3ComponentsTree.addChild(ttComponentResource, "HostComponent:2");
  }

  private String getLocalHostName() throws UnknownHostException {
    return InetAddress.getLocalHost().getHostName();
  }

  private static class TestBlueprintRenderer extends ClusterBlueprintRenderer {

    private ClusterTopology topology;

    TestBlueprintRenderer(ClusterTopology topology, BlueprintExportType exportType) {
      super(exportType);
      this.topology = topology;
    }

    @Override
    protected ClusterTopology createClusterTopology(TreeNode<Resource> clusterNode) {
      return topology;
    }
  }
}
