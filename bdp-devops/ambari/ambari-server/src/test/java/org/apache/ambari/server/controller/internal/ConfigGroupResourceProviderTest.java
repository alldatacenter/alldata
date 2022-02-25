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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ConfigGroupRequest;
import org.apache.ambari.server.controller.ConfigGroupResponse;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.util.Modules;

public class ConfigGroupResourceProviderTest {

  private HostDAO hostDAO = null;

  @Before
  public void setup() throws Exception {
    // Clear authenticated user so that authorization checks will pass
    SecurityContextHolder.getContext().setAuthentication(null);

    hostDAO = createStrictMock(HostDAO.class);

    // Create injector after all mocks have been initialized
    Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));
  }

  private ConfigGroupResourceProvider getConfigGroupResourceProvider
      (AmbariManagementController managementController) throws NoSuchFieldException, IllegalAccessException {
    Resource.Type type = Resource.Type.ConfigGroup;

    ConfigGroupResourceProvider configGroupResourceProvider =
        (ConfigGroupResourceProvider) AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    Provider<ConfigHelper> configHelperProvider = createNiceMock(Provider.class);
    expect(configHelperProvider.get()).andReturn(createNiceMock(ConfigHelper.class));

    replay(configHelperProvider);

    Field m_configHelper = ConfigGroupResourceProvider.class.getDeclaredField("m_configHelper");
    m_configHelper.setAccessible(true);
    m_configHelper.set(configGroupResourceProvider, configHelperProvider);

    return configGroupResourceProvider;
  }


  private class MockModule implements Module {
    @Override
    public void configure(Binder binder) {
      binder.bind(HostDAO.class).toInstance(hostDAO);
    }
  }

  @Test
  public void testCreateConfigGroupAsAmbariAdministrator() throws Exception {
    testCreateConfigGroup(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testCreateConfigGroupAsClusterAdministrator() throws Exception {
    testCreateConfigGroup(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testCreateConfigGroupAsClusterOperator() throws Exception {
    testCreateConfigGroup(TestAuthenticationFactory.createClusterOperator());
  }

  @Test
  public void testCreateConfigGroupAsServiceAdministrator() throws Exception {
    testCreateConfigGroup(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateConfigGroupAsServiceOperator() throws Exception {
    testCreateConfigGroup(TestAuthenticationFactory.createServiceOperator());
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateConfigGroupAsClusterUser() throws Exception {
    testCreateConfigGroup(TestAuthenticationFactory.createClusterUser());
  }

  private void testCreateConfigGroup(Authentication authentication) throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Host h1 = createNiceMock(Host.class);
    Host h2 = createNiceMock(Host.class);
    HostEntity hostEntity1 = createMock(HostEntity.class);
    HostEntity hostEntity2 = createMock(HostEntity.class);
    ConfigGroupFactory configGroupFactory = createNiceMock(ConfigGroupFactory.class);
    ConfigGroup configGroup = createNiceMock(ConfigGroup.class);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(clusters.getHost("h1")).andReturn(h1);
    expect(clusters.getHost("h2")).andReturn(h2);
    expect(cluster.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(cluster.isConfigTypeExists(anyString())).andReturn(true).anyTimes();
    expect(managementController.getConfigGroupFactory()).andReturn(configGroupFactory);
    expect(managementController.getAuthName()).andReturn("admin").anyTimes();
    expect(hostDAO.findByName("h1")).andReturn(hostEntity1).atLeastOnce();
    expect(hostDAO.findByName("h2")).andReturn(hostEntity2).atLeastOnce();
    expect(hostEntity1.getHostId()).andReturn(1L).atLeastOnce();
    expect(hostEntity2.getHostId()).andReturn(2L).atLeastOnce();

    Capture<Cluster> clusterCapture = newCapture();
    Capture<String> serviceName = newCapture();
    Capture<String> captureName = newCapture();
    Capture<String> captureDesc = newCapture();
    Capture<String> captureTag = newCapture();
    Capture<Map<String, Config>> captureConfigs = newCapture();
    Capture<Map<Long, Host>> captureHosts = newCapture();

    expect(configGroupFactory.createNew(capture(clusterCapture), capture(serviceName),
        capture(captureName), capture(captureTag), capture(captureDesc),
        capture(captureConfigs), capture(captureHosts))).andReturn(configGroup);

    replay(managementController, clusters, cluster, configGroupFactory,
        configGroup, response, hostDAO, hostEntity1, hostEntity2);

    ResourceProvider provider = getConfigGroupResourceProvider
        (managementController);

    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();
    Map<String, Object> properties = new LinkedHashMap<>();

    Set<Map<String, Object>> hostSet = new HashSet<>();
    Map<String, Object> host1 = new HashMap<>();
    host1.put(ConfigGroupResourceProvider.HOST_NAME, "h1");
    hostSet.add(host1);
    Map<String, Object> host2 = new HashMap<>();
    host2.put(ConfigGroupResourceProvider.HOST_NAME, "h2");
    hostSet.add(host2);

    Set<Map<String, Object>> configSet = new HashSet<>();
    Map<String, String> configMap = new HashMap<>();
    Map<String, Object> configs = new HashMap<>();
    configs.put("type", "core-site");
    configs.put("tag", "version100");
    configMap.put("key1", "value1");
    configs.put("properties", configMap);
    configSet.add(configs);

    properties.put(ConfigGroupResourceProvider
        .CLUSTER_NAME, "Cluster100");
    properties.put(ConfigGroupResourceProvider.GROUP_NAME,
        "test-1");
    properties.put(ConfigGroupResourceProvider.TAG,
        "tag-1");
    properties.put(ConfigGroupResourceProvider.HOSTS,
        hostSet);
    properties.put(ConfigGroupResourceProvider.DESIRED_CONFIGS,
        configSet);

    propertySet.add(properties);

    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    provider.createResources(request);

    verify(managementController, clusters, cluster, configGroupFactory,
        configGroup, response, hostDAO, hostEntity1, hostEntity2);

    assertEquals("version100", captureConfigs.getValue().get("core-site")
        .getTag());
    assertTrue(captureHosts.getValue().containsKey(1L));
    assertTrue(captureHosts.getValue().containsKey(2L));
  }

  @Test
  public void testDuplicateNameConfigGroupAsAmbariAdministrator() throws Exception {
    testDuplicateNameConfigGroup(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testDuplicateNameConfigGroupAsClusterAdministrator() throws Exception {
    testDuplicateNameConfigGroup(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testDuplicateNameConfigGroupAsClusterOperator() throws Exception {
    testDuplicateNameConfigGroup(TestAuthenticationFactory.createClusterOperator());
  }

  @Test
  public void testDuplicateNameConfigGroupAsServiceAdministrator() throws Exception {
    testDuplicateNameConfigGroup(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDuplicateNameConfigGroupAsServiceOperator() throws Exception {
    testDuplicateNameConfigGroup(TestAuthenticationFactory.createServiceOperator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDuplicateNameConfigGroupAsClusterUser() throws Exception {
    testDuplicateNameConfigGroup(TestAuthenticationFactory.createClusterUser());
  }

  private void testDuplicateNameConfigGroup(Authentication authentication) throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    ConfigGroupFactory configGroupFactory = createNiceMock(ConfigGroupFactory.class);
    ConfigGroup configGroup = createNiceMock(ConfigGroup.class);
    Map<Long, ConfigGroup> configGroupMap = new HashMap<>();
    configGroupMap.put(1L, configGroup);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(managementController.getConfigGroupFactory()).andReturn
        (configGroupFactory).anyTimes();
    expect(managementController.getAuthName()).andReturn("admin").anyTimes();
    expect(cluster.getConfigGroups()).andReturn(configGroupMap);

    expect(configGroupFactory.createNew((Cluster) anyObject(), (String) anyObject(), (String) anyObject(),
        (String) anyObject(), (String) anyObject(), EasyMock.anyObject(),
        EasyMock.anyObject())).andReturn(configGroup).anyTimes();

    expect(configGroup.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(configGroup.getName()).andReturn("test-1").anyTimes();
    expect(configGroup.getTag()).andReturn("tag-1").anyTimes();

    replay(managementController, clusters, cluster, configGroupFactory,
        configGroup, response);

    ResourceProvider provider = getConfigGroupResourceProvider
        (managementController);

    Map<String, Object> properties = new LinkedHashMap<>();
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    properties.put(ConfigGroupResourceProvider
        .CLUSTER_NAME, "Cluster100");
    properties.put(ConfigGroupResourceProvider.GROUP_NAME,
        "test-1");
    properties.put(ConfigGroupResourceProvider.TAG,
        "tag-1");

    propertySet.add(properties);
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    Exception exception = null;
    try {
      provider.createResources(request);
    } catch (AuthorizationException e) {
      throw e;
    } catch (Exception e) {
      exception = e;
    }

    verify(managementController, clusters, cluster, configGroupFactory,
        configGroup, response);

    assertNotNull(exception);
    assertTrue(exception instanceof ResourceAlreadyExistsException);
  }

  @Test
  public void testUpdateConfigGroupWithWrongConfigType() throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ConfigHelper configHelper = createNiceMock(ConfigHelper.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Host h1 = createNiceMock(Host.class);
    Host h2 = createNiceMock(Host.class);
    HostEntity hostEntity1 = createMock(HostEntity.class);
    HostEntity hostEntity2 = createMock(HostEntity.class);

    final ConfigGroup configGroup = createNiceMock(ConfigGroup.class);
    ConfigGroupResponse configGroupResponse = createNiceMock
        (ConfigGroupResponse.class);

    expect(cluster.isConfigTypeExists("core-site")).andReturn(false).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAuthName()).andReturn("admin").anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(clusters.getHost("h1")).andReturn(h1);
    expect(clusters.getHost("h2")).andReturn(h2);
    expect(hostDAO.findByName("h1")).andReturn(hostEntity1).anyTimes();
    expect(hostDAO.findById(1L)).andReturn(hostEntity1).anyTimes();
    expect(hostDAO.findByName("h2")).andReturn(hostEntity2).anyTimes();
    expect(hostDAO.findById(2L)).andReturn(hostEntity2).anyTimes();
    expect(hostEntity1.getHostId()).andReturn(1L).atLeastOnce();
    expect(hostEntity2.getHostId()).andReturn(2L).atLeastOnce();
    expect(h1.getHostId()).andReturn(1L).anyTimes();
    expect(h2.getHostId()).andReturn(2L).anyTimes();

    expect(configGroup.getName()).andReturn("test-1").anyTimes();
    expect(configGroup.getId()).andReturn(25L).anyTimes();
    expect(configGroup.getTag()).andReturn("tag-1").anyTimes();

    expect(configGroup.convertToResponse()).andReturn(configGroupResponse).anyTimes();
    expect(configGroupResponse.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(configGroupResponse.getId()).andReturn(25L).anyTimes();

    expect(cluster.getConfigGroups()).andStubAnswer(new IAnswer<Map<Long, ConfigGroup>>() {
      @Override
      public Map<Long, ConfigGroup> answer() throws Throwable {
        Map<Long, ConfigGroup> configGroupMap = new HashMap<>();
        configGroupMap.put(configGroup.getId(), configGroup);
        return configGroupMap;
      }
    });

    replay(managementController, clusters, cluster,
        configGroup, response, configGroupResponse, configHelper, hostDAO, hostEntity1, hostEntity2, h1, h2);

    ResourceProvider provider = getConfigGroupResourceProvider
        (managementController);

    Map<String, Object> properties = new LinkedHashMap<>();

    Set<Map<String, Object>> hostSet = new HashSet<>();
    Map<String, Object> host1 = new HashMap<>();
    host1.put(ConfigGroupResourceProvider.HOST_NAME, "h1");
    hostSet.add(host1);
    Map<String, Object> host2 = new HashMap<>();
    host2.put(ConfigGroupResourceProvider.HOST_NAME, "h2");
    hostSet.add(host2);

    Set<Map<String, Object>> configSet = new HashSet<>();
    Map<String, String> configMap = new HashMap<>();
    Map<String, Object> configs = new HashMap<>();
    configs.put("type", "core-site");
    configs.put("tag", "version100");
    configMap.put("key1", "value1");
    configs.put("properties", configMap);
    configSet.add(configs);

    properties.put(ConfigGroupResourceProvider
        .CLUSTER_NAME, "Cluster100");
    properties.put(ConfigGroupResourceProvider.GROUP_NAME,
        "test-1");
    properties.put(ConfigGroupResourceProvider.TAG,
        "tag-1");
    properties.put(ConfigGroupResourceProvider.HOSTS,
        hostSet);
    properties.put(ConfigGroupResourceProvider.DESIRED_CONFIGS,
        configSet);

    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");

    Request request = PropertyHelper.getUpdateRequest(properties, mapRequestProps);

    Predicate predicate = new PredicateBuilder().property
        (ConfigGroupResourceProvider.CLUSTER_NAME).equals
        ("Cluster100").and().
        property(ConfigGroupResourceProvider.ID).equals
        (25L).toPredicate();

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    SystemException systemException = null;
    try {
      provider.updateResources(request, predicate);
    } catch (SystemException e) {
      systemException = e;
    }
    assertNotNull(systemException);
    verify(managementController, clusters, cluster,
        configGroup, response, configGroupResponse, configHelper, hostDAO, hostEntity1, hostEntity2, h1, h2);
  }

  @Test
  public void testUpdateConfigGroupAsAmbariAdministrator() throws Exception {
    testUpdateConfigGroup(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testUpdateConfigGroupAsClusterAdministrator() throws Exception {
    testUpdateConfigGroup(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testUpdateConfigGroupAsClusterOperator() throws Exception {
    testUpdateConfigGroup(TestAuthenticationFactory.createClusterOperator());
  }

  @Test
  public void testUpdateConfigGroupAsServiceAdministrator() throws Exception {
    testUpdateConfigGroup(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateConfigGroupAsServiceOperator() throws Exception {
    testUpdateConfigGroup(TestAuthenticationFactory.createServiceOperator());
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateConfigGroupAsClusterUser() throws Exception {
    testUpdateConfigGroup(TestAuthenticationFactory.createClusterUser());
  }

  private void testUpdateConfigGroup(Authentication authentication) throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ConfigHelper configHelper = createNiceMock(ConfigHelper.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Host h1 = createNiceMock(Host.class);
    Host h2 = createNiceMock(Host.class);
    HostEntity hostEntity1 = createMock(HostEntity.class);
    HostEntity hostEntity2 = createMock(HostEntity.class);

    final ConfigGroup configGroup = createNiceMock(ConfigGroup.class);
    ConfigGroupResponse configGroupResponse = createNiceMock
        (ConfigGroupResponse.class);

    expect(cluster.isConfigTypeExists("core-site")).andReturn(true).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAuthName()).andReturn("admin").anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(clusters.getHost("h1")).andReturn(h1);
    expect(clusters.getHost("h2")).andReturn(h2);
    expect(hostDAO.findByName("h1")).andReturn(hostEntity1).anyTimes();
    expect(hostDAO.findById(1L)).andReturn(hostEntity1).anyTimes();
    expect(hostDAO.findByName("h2")).andReturn(hostEntity2).anyTimes();
    expect(hostDAO.findById(2L)).andReturn(hostEntity2).anyTimes();
    expect(hostEntity1.getHostId()).andReturn(1L).atLeastOnce();
    expect(hostEntity2.getHostId()).andReturn(2L).atLeastOnce();
    expect(h1.getHostId()).andReturn(1L).anyTimes();
    expect(h2.getHostId()).andReturn(2L).anyTimes();
    expect(managementController.getConfigGroupUpdateResults((ConfigGroupRequest)anyObject())).
            andReturn(new ConfigGroupResponse(1L, "", "", "", "", new HashSet<>(), new HashSet<>())).atLeastOnce();

    expect(configGroup.getName()).andReturn("test-1").anyTimes();
    expect(configGroup.getId()).andReturn(25L).anyTimes();
    expect(configGroup.getTag()).andReturn("tag-1").anyTimes();

    expect(configGroup.convertToResponse()).andReturn(configGroupResponse).anyTimes();
    expect(configGroupResponse.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(configGroupResponse.getId()).andReturn(25L).anyTimes();

    expect(cluster.getConfigGroups()).andStubAnswer(new IAnswer<Map<Long, ConfigGroup>>() {
      @Override
      public Map<Long, ConfigGroup> answer() throws Throwable {
        Map<Long, ConfigGroup> configGroupMap = new HashMap<>();
        configGroupMap.put(configGroup.getId(), configGroup);
        return configGroupMap;
      }
    });

    replay(managementController, clusters, cluster,
        configGroup, response, configGroupResponse, configHelper, hostDAO, hostEntity1, hostEntity2, h1, h2);

    ResourceProvider provider = getConfigGroupResourceProvider
        (managementController);

    Map<String, Object> properties = new LinkedHashMap<>();

    Set<Map<String, Object>> hostSet = new HashSet<>();
    Map<String, Object> host1 = new HashMap<>();
    host1.put(ConfigGroupResourceProvider.HOST_NAME, "h1");
    hostSet.add(host1);
    Map<String, Object> host2 = new HashMap<>();
    host2.put(ConfigGroupResourceProvider.HOST_NAME, "h2");
    hostSet.add(host2);

    Set<Map<String, Object>> configSet = new HashSet<>();
    Map<String, String> configMap = new HashMap<>();
    Map<String, Object> configs = new HashMap<>();
    configs.put("type", "core-site");
    configs.put("tag", "version100");
    configMap.put("key1", "value1");
    configs.put("properties", configMap);
    configSet.add(configs);

    properties.put(ConfigGroupResourceProvider
        .CLUSTER_NAME, "Cluster100");
    properties.put(ConfigGroupResourceProvider.GROUP_NAME,
        "test-1");
    properties.put(ConfigGroupResourceProvider.TAG,
        "tag-1");
    properties.put(ConfigGroupResourceProvider.HOSTS,
        hostSet);
    properties.put(ConfigGroupResourceProvider.DESIRED_CONFIGS,
        configSet);

    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");

    Request request = PropertyHelper.getUpdateRequest(properties, mapRequestProps);

    Predicate predicate = new PredicateBuilder().property
        (ConfigGroupResourceProvider.CLUSTER_NAME).equals
        ("Cluster100").and().
        property(ConfigGroupResourceProvider.ID).equals
        (25L).toPredicate();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    provider.updateResources(request, predicate);

    verify(managementController, clusters, cluster,
        configGroup, response, configGroupResponse, configHelper, hostDAO, hostEntity1, hostEntity2, h1, h2);
  }

  @Test
  public void testGetConfigGroupAsAmbariAdministrator() throws Exception {
    testGetConfigGroup(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testGetConfigGroupAsClusterAdministrator() throws Exception {
    testGetConfigGroup(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testGetConfigGroupAsClusterOperator() throws Exception {
    testGetConfigGroup(TestAuthenticationFactory.createClusterOperator());
  }

  @Test
  public void testGetConfigGroupAsServiceAdministrator() throws Exception {
    testGetConfigGroup(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test
  public void testGetConfigGroupAsServiceOperator() throws Exception {
    testGetConfigGroup(TestAuthenticationFactory.createServiceOperator());
  }

  @Test
  public void testGetConfigGroupAsClusterUser() throws Exception {
    testGetConfigGroup(TestAuthenticationFactory.createClusterUser());
  }

  @SuppressWarnings("unchecked")
  private void testGetConfigGroup(Authentication authentication) throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Host h1 = createNiceMock(Host.class);
    final Long host1Id = 1L;
    List<Long> hostIds = new ArrayList<Long>() {{
      add(host1Id);
    }};
    List<String> hostNames = new ArrayList<String>() {{
      add("h1");
    }};
    HostEntity hostEntity1 = createMock(HostEntity.class);

    expect(hostDAO.getHostNamesByHostIds(hostIds)).andReturn(hostNames).atLeastOnce();
    expect(hostDAO.findByName("h1")).andReturn(hostEntity1).anyTimes();
    expect(hostEntity1.getHostId()).andReturn(host1Id).anyTimes();

    ConfigGroup configGroup1 = createNiceMock(ConfigGroup.class);
    ConfigGroup configGroup2 = createNiceMock(ConfigGroup.class);
    ConfigGroup configGroup3 = createNiceMock(ConfigGroup.class);
    ConfigGroup configGroup4 = createNiceMock(ConfigGroup.class);
    ConfigGroupResponse response1 = createNiceMock(ConfigGroupResponse.class);
    ConfigGroupResponse response2 = createNiceMock(ConfigGroupResponse.class);
    ConfigGroupResponse response3 = createNiceMock(ConfigGroupResponse.class);
    ConfigGroupResponse response4 = createNiceMock(ConfigGroupResponse.class);

    Map<Long, ConfigGroup> configGroupMap = new HashMap<>();
    configGroupMap.put(1L, configGroup1);
    configGroupMap.put(2L, configGroup2);
    configGroupMap.put(3L, configGroup3);
    configGroupMap.put(4L, configGroup4);

    Map<Long, ConfigGroup> configGroupByHostname = new HashMap<>();
    configGroupByHostname.put(4L, configGroup4);

    expect(configGroup1.convertToResponse()).andReturn(response1).anyTimes();
    expect(configGroup2.convertToResponse()).andReturn(response2).anyTimes();
    expect(configGroup3.convertToResponse()).andReturn(response3).anyTimes();
    expect(configGroup4.convertToResponse()).andReturn(response4).anyTimes();

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(cluster.getConfigGroups()).andReturn(configGroupMap).anyTimes();
    expect(cluster.getClusterName()).andReturn("Cluster100").anyTimes();

    expect(configGroup1.getName()).andReturn("g1").anyTimes();
    expect(configGroup2.getName()).andReturn("g2").anyTimes();
    expect(configGroup3.getName()).andReturn("g3").anyTimes();
    expect(configGroup4.getName()).andReturn("g4").anyTimes();
    expect(configGroup1.getTag()).andReturn("t1").anyTimes();
    expect(configGroup2.getTag()).andReturn("t2").anyTimes();
    expect(configGroup3.getTag()).andReturn("t3").anyTimes();
    expect(configGroup4.getTag()).andReturn("t4").anyTimes();

    Map<Long, Host> hostMap = new HashMap<>();
    hostMap.put(host1Id, h1);
    expect(configGroup4.getHosts()).andReturn(hostMap).anyTimes();


    expect(response1.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(response2.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(response3.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(response4.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(response1.getId()).andReturn(1L).anyTimes();
    expect(response2.getId()).andReturn(2L).anyTimes();
    expect(response3.getId()).andReturn(3L).anyTimes();
    expect(response4.getId()).andReturn(4L).anyTimes();
    expect(response2.getGroupName()).andReturn("g2").anyTimes();
    expect(response3.getTag()).andReturn("t3").anyTimes();
    expect(cluster.getConfigGroupsByHostname("h1")).andReturn(configGroupByHostname).anyTimes();

    Set<Map<String, Object>> hostObj = new HashSet<>();
    Map<String, Object> hostnames = new HashMap<>();
    hostnames.put("host_name", "h1");
    hostObj.add(hostnames);
    expect(response4.getHosts()).andReturn(hostObj).anyTimes();

    replay(managementController, clusters, cluster, hostDAO, hostEntity1,
        configGroup1, configGroup2, configGroup3, configGroup4, response1, response2, response3, response4);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider resourceProvider = getConfigGroupResourceProvider(managementController);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(ConfigGroupResourceProvider.CLUSTER_NAME);
    propertyIds.add(ConfigGroupResourceProvider.ID);

    // Read all
    Predicate predicate = new PredicateBuilder().property
        (ConfigGroupResourceProvider.CLUSTER_NAME)
        .equals("Cluster100").toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);

    Set<Resource> resources = resourceProvider.getResources(request, predicate);

    assertEquals(4, resources.size());

    // Read by id
    predicate = new PredicateBuilder().property(ConfigGroupResourceProvider
        .ID).equals(1L).and().property
        (ConfigGroupResourceProvider.CLUSTER_NAME)
        .equals("Cluster100").toPredicate();

    resources = resourceProvider.getResources(request, predicate);

    assertEquals(1, resources.size());
    assertEquals(1L, resources.iterator().next().getPropertyValue
        (ConfigGroupResourceProvider.ID));

    // Read by Name
    predicate = new PredicateBuilder().property(ConfigGroupResourceProvider
        .CLUSTER_NAME).equals("Cluster100").and()
        .property(ConfigGroupResourceProvider.GROUP_NAME)
        .equals("g2").toPredicate();

    resources = resourceProvider.getResources(request, predicate);

    assertEquals(1, resources.size());
    assertEquals("g2", resources.iterator().next().getPropertyValue
        (ConfigGroupResourceProvider.GROUP_NAME));

    // Read by tag
    predicate = new PredicateBuilder().property(ConfigGroupResourceProvider
        .CLUSTER_NAME).equals("Cluster100").and()
        .property(ConfigGroupResourceProvider.TAG)
        .equals("t3").toPredicate();

    resources = resourceProvider.getResources(request, predicate);

    assertEquals(1, resources.size());
    assertEquals("t3", resources.iterator().next().getPropertyValue
        (ConfigGroupResourceProvider.TAG));

    // Read by hostname (hosts=h1)
    predicate = new PredicateBuilder().property(ConfigGroupResourceProvider
        .CLUSTER_NAME).equals("Cluster100").and()
        .property(ConfigGroupResourceProvider.HOSTS)
        .equals("h1").toPredicate();

    resources = resourceProvider.getResources(request, predicate);

    assertEquals(1, resources.size());
    Set<Map<String, Object>> hostSet = (Set<Map<String, Object>>)
        resources.iterator().next()
            .getPropertyValue(ConfigGroupResourceProvider
                .HOSTS);
    assertEquals("h1", hostSet.iterator().next().get
        (ConfigGroupResourceProvider.HOST_NAME));

    // Read by hostname (hosts/host_name=h1)
    predicate = new PredicateBuilder().property(ConfigGroupResourceProvider
        .CLUSTER_NAME).equals("Cluster100").and()
        .property(ConfigGroupResourceProvider.HOSTS_HOST_NAME)
        .equals("h1").toPredicate();

    resources = resourceProvider.getResources(request, predicate);

    assertEquals(1, resources.size());
    hostSet = (Set<Map<String, Object>>)
        resources.iterator().next()
            .getPropertyValue(ConfigGroupResourceProvider
                .HOSTS);
    assertEquals("h1", hostSet.iterator().next().get
        (ConfigGroupResourceProvider.HOST_NAME));


    // Read by tag and hostname (hosts=h1) - Positive
    predicate = new PredicateBuilder().property(ConfigGroupResourceProvider
        .CLUSTER_NAME).equals("Cluster100").and()
        .property(ConfigGroupResourceProvider.TAG)
        .equals("t4").and().property(ConfigGroupResourceProvider
            .HOSTS).equals(host1Id).toPredicate();

    resources = resourceProvider.getResources(request, predicate);

    assertEquals(1, resources.size());
    hostSet = (Set<Map<String, Object>>)
        resources.iterator().next()
            .getPropertyValue(ConfigGroupResourceProvider
                .HOSTS);
    assertEquals("h1", hostSet.iterator().next().get
        (ConfigGroupResourceProvider.HOST_NAME));

    // Read by tag and hostname (hosts/host_name=h1) - Positive
    predicate = new PredicateBuilder().property(ConfigGroupResourceProvider
        .CLUSTER_NAME).equals("Cluster100").and()
        .property(ConfigGroupResourceProvider.TAG)
        .equals("t4").and().property(ConfigGroupResourceProvider
            .HOSTS_HOST_NAME).equals("h1").toPredicate();

    resources = resourceProvider.getResources(request, predicate);

    assertEquals(1, resources.size());
    hostSet = (Set<Map<String, Object>>)
        resources.iterator().next()
            .getPropertyValue(ConfigGroupResourceProvider
                .HOSTS);
    assertEquals("h1", hostSet.iterator().next().get
        (ConfigGroupResourceProvider.HOST_NAME));

    // Read by id
    predicate = new PredicateBuilder().property(ConfigGroupResourceProvider
        .ID).equals(11L).and().property
        (ConfigGroupResourceProvider.CLUSTER_NAME)
        .equals("Cluster100").toPredicate();

    NoSuchResourceException resourceException = null;
    try {
      resourceProvider.getResources(request, predicate);
    } catch (NoSuchResourceException ce) {
      resourceException = ce;
    }
    Assert.assertNotNull(resourceException);

    verify(managementController, clusters, cluster, hostDAO, hostEntity1,
        configGroup1, configGroup2, configGroup3, configGroup4, response1, response2, response3, response4);
  }

  @Test
  public void testDeleteConfigGroupAsAmbariAdministrator() throws Exception {
    testDeleteConfigGroup(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testDeleteConfigGroupAsClusterAdministrator() throws Exception {
    testDeleteConfigGroup(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testDeleteConfigGroupAsClusterOperator() throws Exception {
    testDeleteConfigGroup(TestAuthenticationFactory.createClusterOperator());
  }

  @Test
  public void testDeleteConfigGroupAsServiceAdministrator() throws Exception {
    testDeleteConfigGroup(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteConfigGroupAsServiceOperator() throws Exception {
    testDeleteConfigGroup(TestAuthenticationFactory.createServiceOperator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteConfigGroupAsClusterUser() throws Exception {
    testDeleteConfigGroup(TestAuthenticationFactory.createClusterUser());
  }

  private void testDeleteConfigGroup(Authentication authentication) throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    ConfigGroup configGroup = createNiceMock(ConfigGroup.class);

    expect(managementController.getAuthName()).andReturn("admin").anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(cluster.getConfigGroups()).andReturn(Collections.singletonMap(1L, configGroup));

    cluster.deleteConfigGroup(1L);

    replay(managementController, clusters, cluster, configGroup);

    ResourceProvider resourceProvider = getConfigGroupResourceProvider
        (managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider) resourceProvider).addObserver(observer);

    Predicate predicate = new PredicateBuilder().property
        (ConfigGroupResourceProvider.CLUSTER_NAME)
        .equals("Cluster100").and().property(ConfigGroupResourceProvider
            .ID).equals(1L).toPredicate();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    resourceProvider.deleteResources(new RequestImpl(null, null, null, null), predicate);

    ResourceProviderEvent lastEvent = observer.getLastEvent();
    Assert.assertNotNull(lastEvent);
    Assert.assertEquals(Resource.Type.ConfigGroup, lastEvent.getResourceType());
    Assert.assertEquals(ResourceProviderEvent.Type.Delete, lastEvent.getType());
    Assert.assertEquals(predicate, lastEvent.getPredicate());
    Assert.assertNull(lastEvent.getRequest());

    verify(managementController, clusters, cluster, configGroup);
  }

  @Test
  public void testGetConfigGroupRequest_populatesConfigAttributes() throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    ConfigGroupResourceProvider resourceProvider = getConfigGroupResourceProvider
        (managementController);

    Set<Map<String, String>> desiredConfigProperties = new HashSet<>();
    Map<String, String> desiredConfig1 = new HashMap<>();
    desiredConfig1.put("tag", "version2");
    desiredConfig1.put("type", "type1");
    desiredConfig1.put("properties/key1", "value1");
    desiredConfig1.put("properties/key2", "value2");
    desiredConfig1.put("properties_attributes/attr1/key1", "true");
    desiredConfig1.put("properties_attributes/attr1/key2", "false");
    desiredConfig1.put("properties_attributes/attr2/key1", "15");
    desiredConfigProperties.add(desiredConfig1);

    Map<String, Object> properties = new HashMap<>();
    properties.put("ConfigGroup/hosts", new HashMap<String, String>() {{
      put("host_name", "ambari1");
    }});
    properties.put("ConfigGroup/cluster_name", "c");
    properties.put("ConfigGroup/desired_configs", desiredConfigProperties);

    ConfigGroupRequest request = resourceProvider.getConfigGroupRequest(properties);

    assertNotNull(request);
    Map<String, Config> configMap = request.getConfigs();
    assertNotNull(configMap);
    assertEquals(1, configMap.size());
    assertTrue(configMap.containsKey("type1"));
    Config config = configMap.get("type1");
    assertEquals("type1", config.getType());
    Map<String, String> configProperties = config.getProperties();
    assertNotNull(configProperties);
    assertEquals(2, configProperties.size());
    assertEquals("value1", configProperties.get("key1"));
    assertEquals("value2", configProperties.get("key2"));
    Map<String, Map<String, String>> configAttributes = config.getPropertiesAttributes();
    assertNotNull(configAttributes);
    assertEquals(2, configAttributes.size());
    assertTrue(configAttributes.containsKey("attr1"));
    Map<String, String> attr1 = configAttributes.get("attr1");
    assertNotNull(attr1);
    assertEquals(2, attr1.size());
    assertEquals("true", attr1.get("key1"));
    assertEquals("false", attr1.get("key2"));
    assertTrue(configAttributes.containsKey("attr2"));
    Map<String, String> attr2 = configAttributes.get("attr2");
    assertNotNull(attr2);
    assertEquals(1, attr2.size());
    assertEquals("15", attr2.get("key1"));
  }
}
