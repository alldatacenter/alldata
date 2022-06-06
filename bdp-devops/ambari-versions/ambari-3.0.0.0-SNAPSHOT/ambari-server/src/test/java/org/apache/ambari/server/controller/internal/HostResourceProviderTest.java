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

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.HostNotFoundException;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionDBAccessorImpl;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactoryImpl;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.actionmanager.StageFactoryImpl;
import org.apache.ambari.server.agent.ComponentRecoveryReport;
import org.apache.ambari.server.agent.RecoveryConfigHelper;
import org.apache.ambari.server.agent.RecoveryReport;
import org.apache.ambari.server.agent.stomp.HostLevelParamsHolder;
import org.apache.ambari.server.agent.stomp.TopologyHolder;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.controller.AbstractRootServiceResponseFactory;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.HostRequest;
import org.apache.ambari.server.controller.HostResponse;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.controller.ResourceProviderFactory;
import org.apache.ambari.server.controller.RootServiceResponseFactory;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.hooks.users.UserHookService;
import org.apache.ambari.server.metadata.CachedRoleCommandOrderProvider;
import org.apache.ambari.server.metadata.RoleCommandOrderProvider;
import org.apache.ambari.server.mpack.MpackManagerFactory;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.StageDAO;
import org.apache.ambari.server.scheduler.ExecutionScheduler;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelperInitializer;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.security.encryption.CredentialStoreServiceImpl;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostConfig;
import org.apache.ambari.server.state.HostHealthStatus;
import org.apache.ambari.server.state.HostHealthStatus.HealthStatus;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostImpl;
import org.apache.ambari.server.testutils.PartialNiceMockBinder;
import org.apache.ambari.server.topology.PersistedState;
import org.apache.ambari.server.topology.PersistedStateImpl;
import org.apache.ambari.server.topology.TopologyManager;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.persist.UnitOfWork;

/**
 * HostResourceProvider tests.
 */
public class HostResourceProviderTest extends EasyMockSupport {

  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testCreateResourcesAsAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testCreateResourcesAsClusterAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesAsServiceAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testCreateResources(Authentication authentication) throws Exception {
    AuthorizationHelperInitializer.viewInstanceDAOReturningNull();
    Resource.Type type = Resource.Type.Host;
    Injector injector = createInjector();

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);
    Capture<String> rackChangeAffectedClusterName = EasyMock.newCapture();
    managementController.registerRackChange(capture(rackChangeAffectedClusterName));
    EasyMock.expectLastCall().once();
    expect(managementController.getBlueprintProvisioningStates(anyLong(), anyLong()))
        .andReturn(Collections.EMPTY_MAP).anyTimes();


    Clusters clusters = injector.getInstance(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    Host host = createMock(Host.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider hostResourceProvider = getHostProvider(injector);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    expect(cluster.getClusterId()).andReturn(2L).anyTimes();
    expect(cluster.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(cluster.getResourceId()).andReturn(4L).anyTimes();
    expect(cluster.getDesiredConfigs()).andReturn(new HashMap<>()).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).atLeastOnce();
    expect(clusters.getHost("Host100")).andReturn(host).atLeastOnce();
    clusters.updateHostWithClusterAndAttributes(EasyMock.anyObject(), EasyMock.anyObject());
    EasyMock.expectLastCall().once();


    expect(host.getRackInfo()).andReturn("/default-rack").anyTimes();
    expect(host.getHostId()).andReturn(1L).anyTimes();
    expect(host.getHostName()).andReturn("Host100").anyTimes();
    expect(host.getIPv4()).andReturn("0.0.0.0").anyTimes();
    Capture<String> newRack = EasyMock.newCapture();
    host.setRackInfo(capture(newRack));
    EasyMock.expectLastCall().once();

    expect(managementController.getClusters()).andReturn(clusters).atLeastOnce();

    expect(resourceProviderFactory.getHostResourceProvider(
        eq(managementController))).andReturn(hostResourceProvider).anyTimes();

    // replay
    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    Map<String, Object> properties = new LinkedHashMap<>();

    // add properties to the request map
    properties.put(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(HostResourceProvider.HOST_HOST_NAME_PROPERTY_ID, "Host100");
    properties.put(HostResourceProvider.HOST_RACK_INFO_PROPERTY_ID, "/test-rack");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    // verify
    verifyAll();

    // the registerRackChange should be called on AmbariManagementController for Cluster100 cluster
    assertEquals("Cluster100", rackChangeAffectedClusterName.getValue());

    // new rack info of the host should be /test-rack
    assertEquals("/test-rack", newRack.getValue());
  }

  @Test
  public void testGetResourcesAsAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testGetResourcesAsClusterAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testGetResourcesAsServiceAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testGetResources(Authentication authentication) throws Exception {
    AuthorizationHelperInitializer.viewInstanceDAOReturningNull();
    Resource.Type type = Resource.Type.Host;
    Injector injector = createInjector();
    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = injector.getInstance(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    Host host100 = createMockHost("Host100", "Cluster100", null, "HEALTHY", "RECOVERABLE", null);
    Host host101 = createMockHost("Host101", "Cluster100", null, "HEALTHY", "RECOVERABLE", null);
    Host host102 = createMockHost("Host102", "Cluster100", null, "HEALTHY", "RECOVERABLE", null);
    HostHealthStatus healthStatus = createNiceMock(HostHealthStatus.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider hostResourceProvider = getHostProvider(injector);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    List<Host> hosts = new LinkedList<>();
    hosts.add(host100);
    hosts.add(host101);
    hosts.add(host102);

    Set<Cluster> clusterSet = new HashSet<>();
    clusterSet.add(cluster);

    // set expectations
    expect(host100.getMaintenanceState(2)).andReturn(MaintenanceState.OFF).anyTimes();
    expect(host101.getMaintenanceState(2)).andReturn(MaintenanceState.OFF).anyTimes();
    expect(host102.getMaintenanceState(2)).andReturn(MaintenanceState.OFF).anyTimes();

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getHostComponents(EasyMock.anyObject()))
        .andReturn(Collections.emptySet()).anyTimes();
    expect(resourceProviderFactory.getHostResourceProvider(
            eq(managementController))).
        andReturn(hostResourceProvider).anyTimes();

    expect(clusters.getHosts()).andReturn(hosts).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(clusters.getClustersForHost("Host100")).andReturn(clusterSet).anyTimes();
    expect(clusters.getClustersForHost("Host101")).andReturn(clusterSet).anyTimes();
    expect(clusters.getClustersForHost("Host102")).andReturn(clusterSet).anyTimes();

    expect(cluster.getClusterId()).andReturn(2L).anyTimes();
    expect(cluster.getDesiredConfigs()).andReturn(new HashMap<>()).anyTimes();

    expect(healthStatus.getHealthStatus()).andReturn(HostHealthStatus.HealthStatus.HEALTHY).anyTimes();
    expect(healthStatus.getHealthReport()).andReturn("HEALTHY").anyTimes();

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_HOST_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_MAINTENANCE_STATE_PROPERTY_ID);

    Predicate predicate = buildPredicate("Cluster100", null);
    Request request = PropertyHelper.getReadRequest(propertyIds);

    // replay
    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);


    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(3, resources.size());
    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals("Cluster100", clusterName);
      MaintenanceState maintenanceState = (MaintenanceState)resource.getPropertyValue(HostResourceProvider.HOST_MAINTENANCE_STATE_PROPERTY_ID);
      Assert.assertEquals(MaintenanceState.OFF, maintenanceState);
    }

    // verify
    verifyAll();
  }

  @Test
  public void testGetResources_Status_NoCluster() throws Exception {
    Resource.Type type = Resource.Type.Host;

    Injector injector = createInjector();
    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = injector.getInstance(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    HostHealthStatus healthStatus = createNiceMock(HostHealthStatus.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);

    Host host100 = createMockHost("Host100", "Cluster100", null, "HEALTHY", "RECOVERABLE", null);

    HostResponse hostResponse1 = createNiceMock(HostResponse.class);

    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider hostResourceProvider = getHostProvider(injector);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Set<Cluster> clusterSet = new HashSet<>();
    clusterSet.add(cluster);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("Cluster100", "Service100", "Component100", "Component 100",
        "Host100", "Host100", "STARTED", "", null, null, null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("Cluster100", "Service100", "Component102", "Component 102",
        "Host100", "Host100", "STARTED", "", null, null, null, null);
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("Cluster100", "Service100", "Component103", "Component 103",
        "Host100", "Host100", "STARTED", "", null, null, null, null);

    Set<ServiceComponentHostResponse> responses = new HashSet<>();
    responses.add(shr1);
    responses.add(shr2);
    responses.add(shr3);

    // set expectations
    expect(host100.getMaintenanceState(2)).andReturn(MaintenanceState.OFF).anyTimes();

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getHostComponents(EasyMock.anyObject()))
        .andReturn(responses).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(clusters.getClustersForHost("Host100")).andReturn(clusterSet).anyTimes();
    expect(clusters.getHosts()).andReturn(Arrays.asList(host100)).anyTimes();

    expect(cluster.getClusterId()).andReturn(2L).anyTimes();
    expect(cluster.getDesiredConfigs()).andReturn(new HashMap<>()).anyTimes();

    expect(hostResponse1.getClusterName()).andReturn("").anyTimes();
    expect(hostResponse1.getHostname()).andReturn("Host100").anyTimes();
    expect(hostResponse1.getStatus()).andReturn(HealthStatus.HEALTHY.name()).anyTimes();
    expect(hostResponse1.getHealthReport()).andReturn("HEALTHY").anyTimes();


    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
        (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.getCategory()).andReturn("MASTER").anyTimes();

    expect(resourceProviderFactory.getHostResourceProvider(
        eq(managementController))).andReturn(hostResourceProvider).anyTimes();

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_HOST_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_HOST_STATUS_PROPERTY_ID);

    Predicate predicate = buildPredicate("Cluster100", null);
    Request request = PropertyHelper.getReadRequest(propertyIds);

    // replay
    replayAll();

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals("Cluster100", clusterName);
      String status = (String) resource.getPropertyValue(HostResourceProvider.HOST_HOST_STATUS_PROPERTY_ID);
      Assert.assertEquals("HEALTHY", status);
    }

    // verify
    verifyAll();
  }

  @Test
  public void testGetResources_Status_Healthy() throws Exception {
    Resource.Type type = Resource.Type.Host;

    Injector injector = createInjector();
    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = injector.getInstance(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    HostHealthStatus healthStatus = createNiceMock(HostHealthStatus.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);

    Host host100 = createMockHost("Host100", "Cluster100", null, "HEALTHY", "RECOVERABLE", null);

    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider hostResourceProvider = getHostProvider(injector);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Set<Cluster> clusterSet = new HashSet<>();
    clusterSet.add(cluster);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("Cluster100", "Service100", "Component100", "Component 100",
        "Host100", "Host100", "STARTED", "", null, null, null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("Cluster100", "Service100", "Component102", "Component 102",
        "Host100", "Host100", "STARTED", "", null, null, null, null);
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("Cluster100", "Service100", "Component103", "Component 103",
        "Host100", "Host100", "STARTED", "", null, null, null, null);

    Set<ServiceComponentHostResponse> responses = new HashSet<>();
    responses.add(shr1);
    responses.add(shr2);
    responses.add(shr3);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getHostComponents(EasyMock.anyObject()))
        .andReturn(responses).anyTimes();

    expect(host100.getMaintenanceState(2)).andReturn(MaintenanceState.OFF).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(clusters.getClustersForHost("Host100")).andReturn(clusterSet).anyTimes();
    expect(clusters.getHosts()).andReturn(Arrays.asList(host100)).anyTimes();

    expect(cluster.getClusterId()).andReturn(2L).anyTimes();
    expect(cluster.getDesiredConfigs()).andReturn(new HashMap<>()).anyTimes();

    expect(healthStatus.getHealthStatus()).andReturn(HostHealthStatus.HealthStatus.HEALTHY).anyTimes();
    expect(healthStatus.getHealthReport()).andReturn("HEALTHY").anyTimes();

    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
        (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.getCategory()).andReturn("MASTER").anyTimes();

    expect(resourceProviderFactory.getHostResourceProvider(
        eq(managementController))).andReturn(hostResourceProvider).anyTimes();

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_HOST_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_HOST_STATUS_PROPERTY_ID);

    Predicate predicate = buildPredicate("Cluster100", null);
    Request request = PropertyHelper.getReadRequest(propertyIds);

    // replay
    replayAll();

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);


    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals("Cluster100", clusterName);
      String status = (String) resource.getPropertyValue(HostResourceProvider.HOST_HOST_STATUS_PROPERTY_ID);
      Assert.assertEquals("HEALTHY", status);
    }

    // verify
    verifyAll();
  }

  @Test
  public void testGetResources_Status_Unhealthy() throws Exception {
    Resource.Type type = Resource.Type.Host;

    Injector injector = createInjector();
    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = injector.getInstance(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);
    HostResponse hostResponse1 = createNiceMock(HostResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider hostResourceProvider = getHostProvider(injector);

    Host host100 = createMockHost("Host100", "Cluster100", null, "UNHEALTHY", "RECOVERABLE", null);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Set<Cluster> clusterSet = new HashSet<>();
    clusterSet.add(cluster);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("Cluster100", "Service100", "Component100", "Component 100",
        "Host100", "Host100", "STARTED", "", null, null, null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("Cluster100", "Service100", "Component102", "Component 102",
        "Host100", "Host100", "INSTALLED", "", null, null, null, null);
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("Cluster100", "Service100", "Component103", "Component 103",
        "Host100", "Host100", "STARTED", "", null, null, null, null);

    Set<ServiceComponentHostResponse> responses = new HashSet<>();
    responses.add(shr1);
    responses.add(shr2);
    responses.add(shr3);

    // set expectations
    expect(host100.getMaintenanceState(2)).andReturn(MaintenanceState.OFF).anyTimes();

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getHostComponents(EasyMock.anyObject()))
        .andReturn(responses).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(clusters.getClustersForHost("Host100")).andReturn(clusterSet).anyTimes();
    expect(clusters.getHosts()).andReturn(Arrays.asList(host100)).anyTimes();

    expect(cluster.getClusterId()).andReturn(2L).anyTimes();
    expect(cluster.getDesiredConfigs()).andReturn(new HashMap<>()).anyTimes();

    expect(hostResponse1.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(hostResponse1.getHostname()).andReturn("Host100").anyTimes();
    expect(hostResponse1.getStatus()).andReturn(HealthStatus.UNHEALTHY.name()).anyTimes();
    expect(hostResponse1.getHealthReport()).andReturn("HEALTHY").anyTimes();

    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
        (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();

    expect(componentInfo.getCategory()).andReturn("MASTER").anyTimes();

    expect(resourceProviderFactory.getHostResourceProvider(
        eq(managementController))).andReturn(hostResourceProvider).anyTimes();

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_HOST_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_HOST_STATUS_PROPERTY_ID);

    Predicate predicate = buildPredicate("Cluster100", null);
    Request request = PropertyHelper.getReadRequest(propertyIds);

    // replay
    replayAll();

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals("Cluster100", clusterName);
      String status = (String) resource.getPropertyValue(HostResourceProvider.HOST_HOST_STATUS_PROPERTY_ID);
      Assert.assertEquals("UNHEALTHY", status);
    }

    // verify
    verifyAll();
  }

  @Test
  public void testGetResources_Status_Unknown() throws Exception {
    Resource.Type type = Resource.Type.Host;

    Injector injector = createInjector();
    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = injector.getInstance(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    HostResponse hostResponse1 = createNiceMock(HostResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider hostResourceProvider = getHostProvider(injector);
    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Host host100 = createMockHost("Host100", "Cluster100", null, "UNKNOWN", "RECOVERABLE", null);

    Set<Cluster> clusterSet = new HashSet<>();
    clusterSet.add(cluster);

    // set expectations
    expect(host100.getMaintenanceState(2)).andReturn(MaintenanceState.OFF).anyTimes();

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(clusters.getClustersForHost("Host100")).andReturn(clusterSet).anyTimes();
    expect(clusters.getHosts()).andReturn(Arrays.asList(host100)).anyTimes();

    expect(cluster.getClusterId()).andReturn(2L).anyTimes();
    expect(cluster.getDesiredConfigs()).andReturn(new HashMap<>()).anyTimes();

    expect(hostResponse1.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(hostResponse1.getHostname()).andReturn("Host100").anyTimes();
    expect(hostResponse1.getStatus()).andReturn(HealthStatus.UNKNOWN.name()).anyTimes();
    expect(hostResponse1.getHealthReport()).andReturn("UNKNOWN").anyTimes();
    expect(resourceProviderFactory.getHostResourceProvider(
        eq(managementController))).andReturn(hostResourceProvider).anyTimes();

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_HOST_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_HOST_STATUS_PROPERTY_ID);

    Predicate predicate = buildPredicate("Cluster100", null);
    Request request = PropertyHelper.getReadRequest(propertyIds);

    // replay
    replayAll();

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals("Cluster100", clusterName);
      String status = (String) resource.getPropertyValue(HostResourceProvider.HOST_HOST_STATUS_PROPERTY_ID);
      Assert.assertEquals("UNKNOWN", status);
    }

    // verify
    verifyAll();
  }

  @Test
  public void testGetRecoveryReportAsAdministrator() throws Exception {
    testGetRecoveryReport(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testGetRecoveryReportAsClusterAdministrator() throws Exception {
    testGetRecoveryReport(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testGetRecoveryReportAsServiceAdministrator() throws Exception {
    testGetRecoveryReport(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testGetRecoveryReport(Authentication authentication) throws Exception {
    Resource.Type type = Resource.Type.Host;

    Injector injector = createInjector();
    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = injector.getInstance(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider hostResourceProvider = getHostProvider(injector);

    RecoveryReport rr = new RecoveryReport();
    rr.setSummary("RECOVERABLE");
    List<ComponentRecoveryReport> compRecReports = new ArrayList<>();
    ComponentRecoveryReport compRecReport = new ComponentRecoveryReport();
    compRecReport.setLimitReached(Boolean.FALSE);
    compRecReport.setName("DATANODE");
    compRecReport.setNumAttempts(2);
    compRecReports.add(compRecReport);
    rr.setComponentReports(compRecReports);

    Host host100 = createMockHost("Host100", "Cluster100", null, "HEALTHY", "RECOVERABLE", rr);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Set<Cluster> clusterSet = new HashSet<>();
    clusterSet.add(cluster);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("Cluster100", "Service100", "Component100", "Component 100",
        "Host100", "Host100", "STARTED", "", null, null, null, null);

    Set<ServiceComponentHostResponse> responses = new HashSet<>();
    responses.add(shr1);

    // set expectations
    expect(host100.getMaintenanceState(2)).andReturn(MaintenanceState.OFF).anyTimes();

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getHostComponents(EasyMock.anyObject()))
        .andReturn(responses).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(clusters.getClustersForHost("Host100")).andReturn(clusterSet).anyTimes();
    expect(clusters.getHosts()).andReturn(Arrays.asList(host100)).anyTimes();
    expect(cluster.getClusterId()).andReturn(2L).anyTimes();
    expect(cluster.getDesiredConfigs()).andReturn(new HashMap<>()).anyTimes();

    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
        (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();
    expect(componentInfo.getCategory()).andReturn("SLAVE").anyTimes();
    expect(resourceProviderFactory.getHostResourceProvider(
        eq(managementController))).andReturn(hostResourceProvider).anyTimes();


    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_HOST_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_RECOVERY_REPORT_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_RECOVERY_SUMMARY_PROPERTY_ID);

    Predicate predicate = buildPredicate("Cluster100", null);
    Request request = PropertyHelper.getReadRequest(propertyIds);

    // replay
    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals("Cluster100", clusterName);
      String recovery = (String) resource.getPropertyValue(HostResourceProvider.HOST_RECOVERY_SUMMARY_PROPERTY_ID);
      Assert.assertEquals("RECOVERABLE", recovery);
      RecoveryReport recRep = (RecoveryReport) resource.getPropertyValue(HostResourceProvider.HOST_RECOVERY_REPORT_PROPERTY_ID);
      Assert.assertEquals("RECOVERABLE", recRep.getSummary());
      Assert.assertEquals(1, recRep.getComponentReports().size());
      Assert.assertEquals(2, recRep.getComponentReports().get(0).getNumAttempts());
    }

    // verify
    verifyAll();
  }

  @Test
  public void testGetResources_Status_Alert() throws Exception {
    Resource.Type type = Resource.Type.Host;

    Injector injector = createInjector();
    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = injector.getInstance(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    HostHealthStatus healthStatus = createNiceMock(HostHealthStatus.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);
    HostResponse hostResponse1 = createNiceMock(HostResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider hostResourceProvider = getHostProvider(injector);

    Host host100 = createMockHost("Host100", "Cluster100", null, "ALERT", "RECOVERABLE", null);

    AbstractControllerResourceProvider.init(resourceProviderFactory);
    Set<Cluster> clusterSet = new HashSet<>();
    clusterSet.add(cluster);

    ServiceComponentHostResponse shr1 = new ServiceComponentHostResponse("Cluster100", "Service100", "Component100", "Component 100",
        "Host100", "Host100", "STARTED", "", null, null, null, null);
    ServiceComponentHostResponse shr2 = new ServiceComponentHostResponse("Cluster100", "Service100", "Component102", "Component 102",
        "Host100", "Host100", "INSTALLED", "", null, null, null, null);
    ServiceComponentHostResponse shr3 = new ServiceComponentHostResponse("Cluster100", "Service100", "Component103", "Component 103",
        "Host100", "Host100", "STARTED", "", null, null, null, null);

    Set<ServiceComponentHostResponse> responses = new HashSet<>();
    responses.add(shr1);
    responses.add(shr2);
    responses.add(shr3);

    // set expectations
    expect(host100.getMaintenanceState(2)).andReturn(MaintenanceState.OFF).anyTimes();

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getHostComponents(EasyMock.anyObject()))
        .andReturn(responses).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(clusters.getClustersForHost("Host100")).andReturn(clusterSet).anyTimes();
    expect(clusters.getHosts()).andReturn(Arrays.asList(host100)).anyTimes();
    expect(cluster.getClusterId()).andReturn(2L).anyTimes();
    expect(cluster.getDesiredConfigs()).andReturn(new HashMap<>()).anyTimes();
    expect(hostResponse1.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(hostResponse1.getHostname()).andReturn("Host100").anyTimes();
    expect(hostResponse1.getStatus()).andReturn(HealthStatus.ALERT.name()).anyTimes();
    expect(hostResponse1.getHealthReport()).andReturn("HEALTHY").anyTimes();
    expect(ambariMetaInfo.getComponent((String) anyObject(), (String) anyObject(),
        (String) anyObject(), (String) anyObject())).andReturn(componentInfo).anyTimes();
    expect(componentInfo.getCategory()).andReturn("SLAVE").anyTimes();
    expect(resourceProviderFactory.getHostResourceProvider(
        eq(managementController))).andReturn(hostResourceProvider).anyTimes();


    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_HOST_NAME_PROPERTY_ID);
    propertyIds.add(HostResourceProvider.HOST_HOST_STATUS_PROPERTY_ID);

    Predicate predicate = buildPredicate("Cluster100", null);
    Request request = PropertyHelper.getReadRequest(propertyIds);

    // replay
    replayAll();

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);


    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals("Cluster100", clusterName);
      String status = (String) resource.getPropertyValue(HostResourceProvider.HOST_HOST_STATUS_PROPERTY_ID);
      Assert.assertEquals("ALERT", status);
    }

    // verify
    verifyAll();
  }

  @Test
  public void testUpdateDesiredConfigAsAdministrator() throws Exception {
    testUpdateDesiredConfig(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testUpdateDesiredConfigAsClusterAdministrator() throws Exception {
    testUpdateDesiredConfig(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testUpdateDesiredConfigAsServiceAdministrator() throws Exception {
    testUpdateDesiredConfig(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateDesiredConfigAsServiceOperator() throws Exception {
    testUpdateDesiredConfig(TestAuthenticationFactory.createServiceOperator());
  }

  private void testUpdateDesiredConfig(Authentication authentication) throws Exception {

    Injector injector = createInjector();
    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = injector.getInstance(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    HostResponse hostResponse1 = createNiceMock(HostResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider hostResourceProvider = getHostProvider(injector);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Set<Cluster> clusterSet = new HashSet<>();
    clusterSet.add(cluster);

    Host host100 = createMockHost("Host100", "Cluster100", null, "HEALTHY", "RECOVERABLE", null);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getHostComponents(EasyMock.anyObject()))
        .andReturn(Collections.emptySet()).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(clusters.getClustersForHost("Host100")).andReturn(clusterSet).anyTimes();
    expect(clusters.getHosts()).andReturn(Arrays.asList(host100)).anyTimes();
    expect(clusters.getHostsForCluster("Cluster100")).andReturn(Collections.singletonMap("Host100", host100)).anyTimes();
    expect(clusters.getHost("Host100")).andReturn(host100).anyTimes();
    clusters.mapAndPublishHostsToCluster(Collections.singleton("Host100"), "Cluster100");
    expectLastCall().anyTimes();
    expect(cluster.getClusterId()).andReturn(2L).anyTimes();
    expect(cluster.getResourceId()).andReturn(4L).anyTimes();
    expect(cluster.getDesiredConfigs()).andReturn(new HashMap<>()).anyTimes();
    expect(hostResponse1.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(hostResponse1.getHostname()).andReturn("Host100").anyTimes();
    expect(hostResponse1.getHealthReport()).andReturn("HEALTHY").anyTimes();
    expect(resourceProviderFactory.getHostResourceProvider(
        eq(managementController))).andReturn(hostResourceProvider).anyTimes();

    // replay
    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(HostResourceProvider.HOST_HOST_NAME_PROPERTY_ID, "Host100");
    properties.put(PropertyHelper.getPropertyId("Hosts.desired_config", "type"), "global");
    properties.put(PropertyHelper.getPropertyId("Hosts.desired_config", "tag"), "version1");
    properties.put(PropertyHelper.getPropertyId("Hosts.desired_config.properties", "a"), "b");
    properties.put(PropertyHelper.getPropertyId("Hosts.desired_config.properties", "x"), "y");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    Predicate predicate = buildPredicate("Cluster100", "Host100");

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.Host,
        managementController);

    provider.updateResources(request, predicate);

    // verify
    verifyAll();
  }

  @Test
  public void testUpdateResourcesAsAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testUpdateResourcesAsClusterAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResourcesAsServiceAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testUpdateResources(Authentication authentication) throws Exception {
    AuthorizationHelperInitializer.viewInstanceDAOReturningNull();
    Resource.Type type = Resource.Type.Host;
    Injector injector = createInjector();
    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = injector.getInstance(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    HostResponse hostResponse1 = createNiceMock(HostResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider hostResourceProvider = getHostProvider(injector);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Set<Cluster> clusterSet = new HashSet<>();
    clusterSet.add(cluster);

    Host host100 = createMockHost("Host100", "Cluster100", null, "HEALTHY", "RECOVERABLE", null);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getHostComponents(EasyMock.anyObject()))
        .andReturn(Collections.emptySet()).anyTimes();
    managementController.registerRackChange("Cluster100");
    expectLastCall().anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(clusters.getClustersForHost("Host100")).andReturn(clusterSet).anyTimes();
    expect(clusters.getHost("Host100")).andReturn(host100).anyTimes();
    expect(clusters.getHostsForCluster("Cluster100")).andReturn(Collections.singletonMap("Host100", host100)).anyTimes();
    clusters.mapAndPublishHostsToCluster(Collections.singleton("Host100"), "Cluster100");
    expectLastCall().anyTimes();
    expect(cluster.getClusterId()).andReturn(2L).anyTimes();
    expect(cluster.getResourceId()).andReturn(4L).anyTimes();
    expect(cluster.getDesiredConfigs()).andReturn(new HashMap<>()).anyTimes();
    expect(hostResponse1.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(hostResponse1.getHostname()).andReturn("Host100").anyTimes();
    expect(hostResponse1.getHealthReport()).andReturn("HEALTHY").anyTimes();
    expect(resourceProviderFactory.getHostResourceProvider(
        eq(managementController))).andReturn(hostResourceProvider).anyTimes();

    // replay
    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(HostResourceProvider.HOST_RACK_INFO_PROPERTY_ID, "rack info");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    Predicate predicate = buildPredicate("Cluster100", "Host100");
    provider.updateResources(request, predicate);

    // verify
    verifyAll();
  }

  @Test
  public void testDeleteResourcesAsAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testDeleteResourcesAsClusterAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResourcesAsServiceAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testDeleteResources(Authentication authentication) throws Exception {
    Injector injector = createInjector();
    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = injector.getInstance(Clusters.class);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    Cluster cluster = createMock(Cluster.class);
    Host host1 = createNiceMock(Host.class);
    ResourceProvider provider = getHostProvider(injector);
    HostHealthStatus healthStatus = createNiceMock(HostHealthStatus.class);
    TopologyManager topologyManager = createNiceMock(TopologyManager.class);
    HostResourceProvider.setTopologyManager(topologyManager);

    Set<Cluster> clusterSet = new HashSet<>();

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getHosts()).andReturn(Arrays.asList(host1)).anyTimes();
    expect(clusters.getHost("Host100")).andReturn(host1).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(clusters.getClustersForHost("Host100")).andReturn(clusterSet).anyTimes();
    expect(cluster.getServiceComponentHosts("Host100")).andReturn(Collections.emptyList());
    expect(cluster.getClusterId()).andReturn(100L).anyTimes();
    expect(cluster.getDesiredConfigs()).andReturn(new HashMap<>()).anyTimes();
    clusters.deleteHost("Host100");
    clusters.publishHostsDeletion(Collections.singleton(1L), Collections.singleton("Host100"));
    expect(host1.getHostName()).andReturn("Host100").anyTimes();
    expect(host1.getHostId()).andReturn(1L).anyTimes();
    expect(healthStatus.getHealthStatus()).andReturn(HostHealthStatus.HealthStatus.HEALTHY).anyTimes();
    expect(healthStatus.getHealthReport()).andReturn("HEALTHY").anyTimes();
    expect(topologyManager.getRequests(Collections.emptyList())).andReturn(Collections.emptyList()).anyTimes();

    // replay
    replayAll();

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();
    ((ObservableResourceProvider) provider).addObserver(observer);

    Predicate predicate = buildPredicate("Cluster100", "Host100");
    provider.deleteResources(new RequestImpl(null, null, null, null), predicate);

    ResourceProviderEvent lastEvent = observer.getLastEvent();
    Assert.assertNotNull(lastEvent);
    Assert.assertEquals(Resource.Type.Host, lastEvent.getResourceType());
    Assert.assertEquals(ResourceProviderEvent.Type.Delete, lastEvent.getType());
    Assert.assertEquals(predicate, lastEvent.getPredicate());
    Assert.assertNull(lastEvent.getRequest());

    // verify
    verifyAll();
  }

  public static HostResourceProvider getHostProvider(AmbariManagementController managementController)
      throws NoSuchFieldException, IllegalAccessException {
    Resource.Type type = Resource.Type.Host;
    TopologyHolder topologyHolder = EasyMock.createNiceMock(TopologyHolder.class);
    RecoveryConfigHelper recoveryConfigHelper = EasyMock.createNiceMock(RecoveryConfigHelper.class);
    HostLevelParamsHolder hostLevelParamsHolder = EasyMock.createNiceMock(HostLevelParamsHolder.class);

    HostResourceProvider hostResourceProvider = new HostResourceProvider(
        managementController);

    replay(topologyHolder, recoveryConfigHelper, hostLevelParamsHolder);

    Field topologyHolderField = HostResourceProvider.class.getDeclaredField("topologyHolder");
    topologyHolderField.setAccessible(true);
    topologyHolderField.set(hostResourceProvider, topologyHolder);

    Field recoveryConfigHelperField = HostResourceProvider.class.getDeclaredField("recoveryConfigHelper");
    recoveryConfigHelperField.setAccessible(true);
    recoveryConfigHelperField.set(hostResourceProvider, recoveryConfigHelper);

    Field hostLevelParamsHolderField = HostResourceProvider.class.getDeclaredField("hostLevelParamsHolder");
    hostLevelParamsHolderField.setAccessible(true);
    hostLevelParamsHolderField.set(hostResourceProvider, hostLevelParamsHolder);

    return hostResourceProvider;
  }

  @Test
  public void testGetHostsAsAdministrator() throws Exception {
    testGetHosts(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testGetHostsAsClusterAdministrator() throws Exception {
    testGetHosts(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testGetHostsAsServiceAdministrator() throws Exception {
    testGetHosts(TestAuthenticationFactory.createServiceAdministrator());
  }
  @Test
  public void testGetHostsAsServiceOperator() throws Exception {
    testGetHosts(TestAuthenticationFactory.createServiceOperator());
  }

  private void testGetHosts(Authentication authentication) throws Exception {
    // member state mocks
    Injector injector = createInjector();

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = injector.getInstance(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    Host host = createNiceMock(Host.class);
    HostResponse response = createNiceMock(HostResponse.class);

    Set<Cluster> setCluster = Collections.singleton(cluster);
    Map<String, DesiredConfig> desiredConfigs = new HashMap<>();
    Map<String, HostConfig> desiredHostConfigs = new HashMap<>();

    // requests
    HostRequest request1 = new HostRequest("host1", "cluster1");

    Set<HostRequest> setRequests = new HashSet<>();
    setRequests.add(request1);

    // expectations
    expect(managementController.getClusters()).andReturn(clusters).atLeastOnce();

    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(clusters.getHost("host1")).andReturn(host);
    expect(clusters.getClustersForHost("host1")).andReturn(setCluster);
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(cluster.getClusterId()).andReturn(2L).anyTimes();
    expect(cluster.getDesiredConfigs()).andReturn(desiredConfigs);
    expect(host.getDesiredHostConfigs(cluster, desiredConfigs)).andReturn(desiredHostConfigs);
    expect(host.getHostName()).andReturn("host1").anyTimes();
    expect(host.convertToResponse()).andReturn(response);
    response.setClusterName("cluster1");

    // replay mocks
    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    //test
    Set<HostResponse> setResponses = getHosts(managementController, setRequests);

    // assert and verify
    assertEquals(1, setResponses.size());
    assertTrue(setResponses.contains(response));

    verifyAll();
  }

  /**
   * Ensure that HostNotFoundException is propagated in case where there is a single request.
   */
  @Test(expected = HostNotFoundException.class)
  public void testGetHosts___HostNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createInjector();

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = injector.getInstance(Clusters.class);
    Cluster cluster = createMock(Cluster.class);

    // requests
    HostRequest request1 = new HostRequest("host1", "cluster1");
    Set<HostRequest> setRequests = Collections.singleton(request1);

    // expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(clusters.getHost("host1")).andThrow(new HostNotFoundException("host1"));

    // replay mocks
    replayAll();

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());
    getHosts(managementController, setRequests);

    verifyAll();
  }

  /**
   * Ensure that HostNotFoundException is propagated in case where there is a single request.
   */
  @Test(expected = HostNotFoundException.class)
  public void testGetHosts___HostNotFoundException_HostNotAssociatedWithCluster() throws Exception {
    // member state mocks
    Injector injector = createInjector();

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = injector.getInstance(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    Host host = createNiceMock(Host.class);

    // requests
    HostRequest request1 = new HostRequest("host1", "cluster1");
    Set<HostRequest> setRequests = Collections.singleton(request1);

    // expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(clusters.getHost("host1")).andReturn(host);
    expect(cluster.getDesiredConfigs()).andReturn(new HashMap<>()).anyTimes();
    expect(host.getHostName()).andReturn("host1").anyTimes();
    // because cluster is not in set will result in HostNotFoundException
    expect(clusters.getClustersForHost("host1")).andReturn(Collections.emptySet());

    // replay mocks
    replayAll();

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());
    getHosts(managementController, setRequests);

    verifyAll();
  }


  /**
   * Ensure that HostNotFoundException is handled where there are multiple requests as would be the
   * case when an OR predicate is provided in the query.
   */
  @Test
  public void testGetHosts___OR_Predicate_HostNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createInjector();

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = injector.getInstance(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    Host host1 = createNiceMock(Host.class);
    Host host2 = createNiceMock(Host.class);
    HostResponse response = createNiceMock(HostResponse.class);
    HostResponse response2 = createNiceMock(HostResponse.class);

    // requests
    HostRequest request1 = new HostRequest("host1", "cluster1");
    HostRequest request2 = new HostRequest("host2", "cluster1");
    HostRequest request3 = new HostRequest("host3", "cluster1");
    HostRequest request4 = new HostRequest("host4", "cluster1");

    Set<HostRequest> setRequests = new HashSet<>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);
    setRequests.add(request4);

    // expectations

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();

    expect(clusters.getCluster("cluster1")).andReturn(cluster).times(4);

    expect(clusters.getHost("host1")).andReturn(host1);
    expect(host1.getHostName()).andReturn("host1").anyTimes();
    expect(clusters.getClustersForHost("host1")).andReturn(Collections.singleton(cluster));
    expect(host1.convertToResponse()).andReturn(response);
    response.setClusterName("cluster1");

    expect(clusters.getHost("host2")).andReturn(host2);
    expect(host2.getHostName()).andReturn("host2").anyTimes();
    expect(clusters.getClustersForHost("host2")).andReturn(Collections.singleton(cluster));
    expect(host2.convertToResponse()).andReturn(response2);
    response2.setClusterName("cluster1");

    expect(clusters.getHost("host3")).andThrow(new HostNotFoundException("host3"));
    expect(clusters.getHost("host4")).andThrow(new HostNotFoundException("host4"));

    expect(cluster.getClusterId()).andReturn(2L).anyTimes();
    expect(cluster.getDesiredConfigs()).andReturn(new HashMap<>()).anyTimes();

    // replay mocks
    replayAll();

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    //test
    Set<HostResponse> setResponses = getHosts(managementController, setRequests);

    // assert and verify
    assertEquals(2, setResponses.size());
    assertTrue(setResponses.contains(response));
    assertTrue(setResponses.contains(response2));

    verifyAll();
  }

  public static void createHosts(AmbariManagementController controller, Set<HostRequest> requests)
      throws AmbariException, AuthorizationException, NoSuchFieldException, IllegalAccessException {
    HostResourceProvider provider = getHostProvider(controller);
    Set<Map<String, Object>> properties = new HashSet<>();

    for (HostRequest request : requests) {
      Map<String, Object> requestProperties = new HashMap<>();
      requestProperties.put(HostResourceProvider.HOST_HOST_NAME_PROPERTY_ID, request.getHostname());
      requestProperties.put(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID, request.getClusterName());
      if (null != request.getRackInfo()) {
        requestProperties.put(HostResourceProvider.HOST_RACK_INFO_PROPERTY_ID, UUID.randomUUID().toString());
      }
      properties.add(requestProperties);
    }

    provider.createHosts(PropertyHelper.getCreateRequest(properties, Collections.emptyMap()));
  }

  public static Set<HostResponse> getHosts(AmbariManagementController controller,
                                           Set<HostRequest> requests)
      throws AmbariException, NoSuchFieldException, IllegalAccessException {
    HostResourceProvider provider = getHostProvider(controller);
    return provider.getHosts(requests);
  }

  public static void deleteHosts(AmbariManagementController controller, Set<HostRequest> requests)
      throws AmbariException, NoSuchFieldException, IllegalAccessException {
    TopologyManager topologyManager = EasyMock.createNiceMock(TopologyManager.class);
    expect(topologyManager.getRequests(Collections.emptyList())).andReturn(Collections.emptyList()).anyTimes();

    replay(topologyManager);

    HostResourceProvider provider = getHostProvider(controller);
    HostResourceProvider.setTopologyManager(topologyManager);
    provider.deleteHosts(requests, false);
  }

  public static DeleteStatusMetaData deleteHosts(AmbariManagementController controller,
                                                 Set<HostRequest> requests, boolean dryRun)
      throws AmbariException, NoSuchFieldException, IllegalAccessException {
    TopologyManager topologyManager = EasyMock.createNiceMock(TopologyManager.class);
    expect(topologyManager.getRequests(Collections.emptyList())).andReturn(Collections.emptyList()).anyTimes();

    replay(topologyManager);

    HostResourceProvider provider = getHostProvider(controller);
    HostResourceProvider.setTopologyManager(topologyManager);
    return provider.deleteHosts(requests, dryRun);
  }

  public static void updateHosts(AmbariManagementController controller, Set<HostRequest> requests)
      throws AmbariException, AuthorizationException, NoSuchFieldException, IllegalAccessException {
    HostResourceProvider provider = getHostProvider(controller);
    provider.updateHosts(requests);
  }

  private Injector createInjector() throws Exception {
    Injector injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        PartialNiceMockBinder.newBuilder().addConfigsBindings().addFactoriesInstallBinding().addPasswordEncryptorBindings().addLdapBindings().build().configure(binder());

        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(ActionDBAccessor.class).to(ActionDBAccessorImpl.class);
        bind(ExecutionScheduler.class).toInstance(createNiceMock(ExecutionScheduler.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(Clusters.class).toInstance(createMock(Clusters.class));
        bind(AmbariManagementController.class).toInstance(createMock(AmbariManagementController.class));
        bind(AmbariMetaInfo.class).toInstance(createNiceMock(AmbariMetaInfo.class));
        bind(Gson.class).toInstance(new Gson());
        bind(MaintenanceStateHelper.class).toInstance(createNiceMock(MaintenanceStateHelper.class));
        bind(KerberosHelper.class).toInstance(createNiceMock(KerberosHelper.class));
        bind(UnitOfWork.class).toInstance(createNiceMock(UnitOfWork.class));
        bind(PersistedState.class).to(PersistedStateImpl.class);
        bind(HostRoleCommandFactory.class).to(HostRoleCommandFactoryImpl.class);
        bind(RoleCommandOrderProvider.class).to(CachedRoleCommandOrderProvider.class);
        bind(PasswordEncoder.class).toInstance(new StandardPasswordEncoder());
        bind(HookService.class).to(UserHookService.class);
        bind(StageFactory.class).to(StageFactoryImpl.class);
        bind(AbstractRootServiceResponseFactory.class).to(RootServiceResponseFactory.class);
        bind(CredentialStoreService.class).to(CredentialStoreServiceImpl.class);
        bind(AuditLogger.class).toInstance(createMock(AuditLogger.class));
        bind(StageDAO.class).toInstance(createNiceMock(StageDAO.class));
        bind(HostLevelParamsHolder.class).toInstance(createNiceMock(HostLevelParamsHolder.class));
        bind(TopologyHolder.class).toInstance(createNiceMock(TopologyHolder.class));
        bind(RecoveryConfigHelper.class).toInstance(createNiceMock(RecoveryConfigHelper.class));
        bind(MpackManagerFactory.class).toInstance(createNiceMock(MpackManagerFactory.class));

        install(new FactoryModuleBuilder().build(StackManagerFactory.class));
        install(new FactoryModuleBuilder().implement(
            ServiceComponentHost.class, ServiceComponentHostImpl.class).build(
            ServiceComponentHostFactory.class));
      }
    });

    StageDAO stageDAO = injector.getInstance(StageDAO.class);
    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    reset(stageDAO, ambariMetaInfo);
    return injector;
  }

  private HostResourceProvider getHostProvider(Injector injector) throws NoSuchFieldException, IllegalAccessException {
    HostResourceProvider provider = getHostProvider(injector.getInstance(AmbariManagementController.class));
    injector.injectMembers(provider);
    return provider;
  }

  private Host createMockHost(String hostName, String clusterName, Map<String, HostConfig> desiredConfigs,
                              String status, String recoverySummary, RecoveryReport recoveryReport) {
    Host host = createMock(Host.class);
    HostHealthStatus hostHealthStatus = new HostHealthStatus(HealthStatus.HEALTHY, "");
    HostResponse hostResponse = new HostResponse(hostName, clusterName, null, 1, 1, null,
        "centos6", 1024, null, 1, 1, null, null, null, hostHealthStatus, HostState.HEALTHY, status);

    hostResponse.setRecoverySummary(recoverySummary);
    hostResponse.setRecoveryReport(recoveryReport);
    expect(host.convertToResponse()).andReturn(hostResponse).anyTimes();

    try {
      expect(host.getDesiredHostConfigs(EasyMock.anyObject(),
          EasyMock.anyObject())).andReturn(desiredConfigs).anyTimes();

    } catch (AmbariException e) {
      Assert.fail(e.getMessage());
    }
    expect(host.getHostName()).andReturn(hostName).anyTimes();
    expect(host.getHostId()).andReturn(1L).anyTimes();
    expect(host.getRackInfo()).andReturn("rackInfo").anyTimes();
    host.setRackInfo(EasyMock.anyObject());
    expectLastCall().anyTimes();
    return host;
  }

  private Predicate buildPredicate(String clusterName, String hostName) {
    PredicateBuilder builder = new PredicateBuilder();
    if (clusterName != null && hostName != null) {
      return builder.property(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID).equals(clusterName)
      .and().property(HostResourceProvider.HOST_HOST_NAME_PROPERTY_ID).equals(hostName).toPredicate();
    }

    return clusterName != null ?
            builder.property(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID).equals(clusterName).toPredicate() :
            builder.property(HostResourceProvider.HOST_HOST_NAME_PROPERTY_ID).equals(hostName).toPredicate();
  }
}
