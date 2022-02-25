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

import static org.apache.ambari.server.controller.AmbariManagementControllerImpl.CLUSTER_PHASE_INITIAL_INSTALL;
import static org.apache.ambari.server.controller.AmbariManagementControllerImpl.CLUSTER_PHASE_PROPERTY;
import static org.apache.ambari.server.controller.internal.HostComponentResourceProvider.ALL_COMPONENTS;
import static org.apache.ambari.server.controller.internal.HostComponentResourceProvider.shouldSkipInstallTaskForComponent;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ResourceProviderFactory;
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.UpgradeState;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Injector;

/**
 * HostComponentResourceProvider tests.
 */
public class HostComponentResourceProviderTest {
  @Before
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
    Resource.Type type = Resource.Type.HostComponent;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    Injector injector = createNiceMock(Injector.class);
    HostComponentResourceProvider hostComponentResourceProvider =
        new HostComponentResourceProvider(managementController);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    managementController.createHostComponents(
        AbstractResourceProviderTest.Matcher.getHostComponentRequestSet(
            "Cluster100", "Service100", "Component100", "Host100", null, null));

    expect(resourceProviderFactory.getHostComponentResourceProvider(
        eq(managementController))).
        andReturn(hostComponentResourceProvider).anyTimes();


    // replay
    replay(managementController, response, resourceProviderFactory);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    // Service 1: create a map of properties for the request
    Map<String, Object> properties = new LinkedHashMap<>();

    // add properties to the request map
    properties.put(HostComponentResourceProvider.CLUSTER_NAME, "Cluster100");
    properties.put(HostComponentResourceProvider.SERVICE_NAME, "Service100");
    properties.put(HostComponentResourceProvider.COMPONENT_NAME, "Component100");
    properties.put(HostComponentResourceProvider.HOST_NAME, "Host100");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    // verify
    verify(managementController, response, resourceProviderFactory);
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
    Resource.Type type = Resource.Type.HostComponent;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    ResourceProvider hostComponentResourceProvider = createNiceMock(HostComponentResourceProvider.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    Set<ServiceComponentHostResponse> allResponse = new HashSet<>();
    StackId stackId = new StackId("HDP-0.1");
    StackId stackId2 = new StackId("HDP-0.2");

    String repositoryVersion2 = "0.2-1234";

    allResponse.add(new ServiceComponentHostResponse(
        "Cluster100", "Service100", "Component100", "Component 100", "Host100", "Host100",
        State.INSTALLED.toString(), stackId.getStackId(), State.STARTED.toString(),
        stackId2.getStackId(), repositoryVersion2, null));

    allResponse.add(new ServiceComponentHostResponse(
        "Cluster100", "Service100", "Component101", "Component 101", "Host100", "Host100",
        State.INSTALLED.toString(), stackId.getStackId(), State.STARTED.toString(),
        stackId2.getStackId(), repositoryVersion2, null));

    allResponse.add(new ServiceComponentHostResponse(
        "Cluster100", "Service100", "Component102", "Component 102", "Host100", "Host100",
        State.INSTALLED.toString(), stackId.getStackId(), State.STARTED.toString(),
        stackId2.getStackId(), repositoryVersion2, null));

    Map<String, String> expectedNameValues = new HashMap<>();
    expectedNameValues.put(
        HostComponentResourceProvider.CLUSTER_NAME, "Cluster100");
    expectedNameValues.put(
        HostComponentResourceProvider.STATE, State.INSTALLED.toString());
    expectedNameValues.put(
        HostComponentResourceProvider.VERSION, repositoryVersion2);
    expectedNameValues.put(
        HostComponentResourceProvider.DESIRED_REPOSITORY_VERSION, repositoryVersion2);
    expectedNameValues.put(
        HostComponentResourceProvider.DESIRED_STATE, State.STARTED.toString());
    expectedNameValues.put(
        HostComponentResourceProvider.DESIRED_STACK_ID, stackId2.getStackId());
    expectedNameValues.put(
        HostComponentResourceProvider.UPGRADE_STATE, UpgradeState.NONE.name());


    // set expectations
    expect(resourceProviderFactory.getHostComponentResourceProvider(
        eq(managementController))).
        andReturn(hostComponentResourceProvider).anyTimes();

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(HostComponentResourceProvider.CLUSTER_NAME);
    propertyIds.add(HostComponentResourceProvider.COMPONENT_NAME);
    propertyIds.add(HostComponentResourceProvider.STATE);
    propertyIds.add(HostComponentResourceProvider.VERSION);
    propertyIds.add(HostComponentResourceProvider.DESIRED_REPOSITORY_VERSION);
    propertyIds.add(HostComponentResourceProvider.DESIRED_STATE);
    propertyIds.add(HostComponentResourceProvider.DESIRED_STACK_ID);

    Predicate predicate = new PredicateBuilder().property(
        HostComponentResourceProvider.CLUSTER_NAME).equals("Cluster100").toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);

    Set<Resource> hostsComponentResources = new HashSet<>();

    Resource hostsComponentResource1 = new ResourceImpl(Resource.Type.HostComponent);
    hostsComponentResource1.setProperty(HostComponentResourceProvider.CLUSTER_NAME, "Cluster100");
    hostsComponentResource1.setProperty(HostComponentResourceProvider.HOST_NAME, "Host100");
    hostsComponentResource1.setProperty(HostComponentResourceProvider.SERVICE_NAME, "Service100");
    hostsComponentResource1.setProperty(HostComponentResourceProvider.COMPONENT_NAME, "Component100");
    hostsComponentResource1.setProperty(HostComponentResourceProvider.STATE, State.INSTALLED.name());
    hostsComponentResource1.setProperty(HostComponentResourceProvider.DESIRED_STATE, State.STARTED.name());
    hostsComponentResource1.setProperty(
        HostComponentResourceProvider.VERSION, repositoryVersion2);
    hostsComponentResource1.setProperty(HostComponentResourceProvider.DESIRED_STACK_ID, stackId2.getStackId());
    hostsComponentResource1.setProperty(HostComponentResourceProvider.UPGRADE_STATE, UpgradeState.NONE.name());
    hostsComponentResource1.setProperty(HostComponentResourceProvider.DESIRED_REPOSITORY_VERSION, repositoryVersion2);

    Resource hostsComponentResource2 = new ResourceImpl(Resource.Type.HostComponent);
    hostsComponentResource2.setProperty(HostComponentResourceProvider.CLUSTER_NAME, "Cluster100");
    hostsComponentResource2.setProperty(HostComponentResourceProvider.HOST_NAME, "Host100");
    hostsComponentResource2.setProperty(HostComponentResourceProvider.SERVICE_NAME, "Service100");
    hostsComponentResource2.setProperty(HostComponentResourceProvider.COMPONENT_NAME, "Component101");
    hostsComponentResource2.setProperty(HostComponentResourceProvider.STATE, State.INSTALLED.name());
    hostsComponentResource2.setProperty(HostComponentResourceProvider.DESIRED_STATE, State.STARTED.name());
    hostsComponentResource2.setProperty(
        HostComponentResourceProvider.VERSION, repositoryVersion2);
    hostsComponentResource2.setProperty(HostComponentResourceProvider.DESIRED_STACK_ID, stackId2.getStackId());
    hostsComponentResource2.setProperty(HostComponentResourceProvider.UPGRADE_STATE, UpgradeState.NONE.name());
    hostsComponentResource2.setProperty(HostComponentResourceProvider.DESIRED_REPOSITORY_VERSION, repositoryVersion2);

    Resource hostsComponentResource3 = new ResourceImpl(Resource.Type.HostComponent);
    hostsComponentResource3.setProperty(HostComponentResourceProvider.CLUSTER_NAME, "Cluster100");
    hostsComponentResource3.setProperty(HostComponentResourceProvider.HOST_NAME, "Host100");
    hostsComponentResource3.setProperty(HostComponentResourceProvider.SERVICE_NAME, "Service100");
    hostsComponentResource3.setProperty(HostComponentResourceProvider.COMPONENT_NAME, "Component102");
    hostsComponentResource3.setProperty(HostComponentResourceProvider.STATE, State.INSTALLED.name());
    hostsComponentResource3.setProperty(HostComponentResourceProvider.DESIRED_STATE, State.STARTED.name());
    hostsComponentResource3.setProperty(
        HostComponentResourceProvider.VERSION, repositoryVersion2);
    hostsComponentResource3.setProperty(HostComponentResourceProvider.DESIRED_STACK_ID, stackId2.getStackId());
    hostsComponentResource3.setProperty(HostComponentResourceProvider.UPGRADE_STATE, UpgradeState.NONE.name());
    hostsComponentResource3.setProperty(HostComponentResourceProvider.DESIRED_REPOSITORY_VERSION, repositoryVersion2);

    hostsComponentResources.add(hostsComponentResource1);
    hostsComponentResources.add(hostsComponentResource2);
    hostsComponentResources.add(hostsComponentResource3);

    expect(hostComponentResourceProvider.getResources(eq(request), eq(predicate))).andReturn(hostsComponentResources).anyTimes();

    // replay
    replay(managementController, resourceProviderFactory, hostComponentResourceProvider);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);


    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(3, resources.size());
    Set<String> names = new HashSet<>();
    for (Resource resource : resources) {
      for (String key : expectedNameValues.keySet()) {
        Assert.assertEquals(expectedNameValues.get(key), resource.getPropertyValue(key));
      }
      names.add((String) resource.getPropertyValue(
          HostComponentResourceProvider.COMPONENT_NAME));
    }
    // Make sure that all of the response objects got moved into resources
    for (ServiceComponentHostResponse response : allResponse) {
      Assert.assertTrue(names.contains(response.getComponentName()));
    }

    // verify
    verify(managementController, resourceProviderFactory, hostComponentResourceProvider);
  }

  @Test
  public void testUpdateResourcesAsAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testUpdateResourcesAsClusterAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testUpdateResourcesAsServiceAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testUpdateResources(Authentication authentication) throws Exception {
    Resource.Type type = Resource.Type.HostComponent;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    HostVersionDAO hostVersionDAO = createMock(HostVersionDAO.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    Injector injector = createNiceMock(Injector.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    ServiceComponentHost componentHost = createNiceMock(ServiceComponentHost.class);
    RequestStageContainer stageContainer = createNiceMock(RequestStageContainer.class);
    MaintenanceStateHelper maintenanceStateHelper = createNiceMock(MaintenanceStateHelper.class);


    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");

    Set<ServiceComponentHostResponse> nameResponse = new HashSet<>();
    nameResponse.add(new ServiceComponentHostResponse(
        "Cluster102", "Service100", "Component100", "Component 100", "Host100", "Host100",
        "INSTALLED", "", "", "", "", null));

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.findServiceName(cluster, "Component100")).andReturn("Service100").anyTimes();
    expect(clusters.getCluster("Cluster102")).andReturn(cluster).anyTimes();
    expect(cluster.getClusterId()).andReturn(2L).anyTimes();
    expect(cluster.getService("Service100")).andReturn(service).anyTimes();
    expect(service.getServiceComponent("Component100")).andReturn(component).anyTimes();
    expect(component.getServiceComponentHost("Host100")).andReturn(componentHost).anyTimes();
    expect(component.getName()).andReturn("Component100").anyTimes();
    expect(componentHost.getState()).andReturn(State.INSTALLED).anyTimes();
    expect(response.getMessage()).andReturn("response msg").anyTimes();
    expect(response.getRequestId()).andReturn(1000L);

    //Cluster is default type.  Maintenance mode is not being tested here so the default is returned.
    expect(maintenanceStateHelper.isOperationAllowed(Resource.Type.Cluster, componentHost)).andReturn(true).anyTimes();

    expect(managementController.getHostComponents(
        EasyMock.anyObject())).andReturn(nameResponse).once();

    Map<String, Map<State, List<ServiceComponentHost>>> changedHosts = new HashMap<>();
    List<ServiceComponentHost> changedComponentHosts = new ArrayList<>();
    changedComponentHosts.add(componentHost);
    changedHosts.put("Component100", Collections.singletonMap(State.STARTED, changedComponentHosts));

    expect(managementController.addStages(null, cluster, mapRequestProps, null, null, null, changedHosts,
        Collections.emptyList(), false, false, false, false)).andReturn(stageContainer).once();

    stageContainer.persist();
    expect(stageContainer.getRequestStatusResponse()).andReturn(response).once();

    TestHostComponentResourceProvider provider =
        new TestHostComponentResourceProvider(PropertyHelper.getPropertyIds(type),
            PropertyHelper.getKeyPropertyIds(type),
            managementController, injector);
    provider.setFieldValue("maintenanceStateHelper", maintenanceStateHelper);
    provider.setFieldValue("hostVersionDAO", hostVersionDAO);

    expect(resourceProviderFactory.getHostComponentResourceProvider(
        eq(managementController))).
        andReturn(provider).anyTimes();

    // replay
    replay(managementController, response, resourceProviderFactory, clusters, cluster, service,
        component, componentHost, stageContainer, maintenanceStateHelper);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(HostComponentResourceProvider.STATE, "STARTED");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, mapRequestProps);

    // update the cluster named Cluster102
    Predicate predicate = new PredicateBuilder().property(
        HostComponentResourceProvider.CLUSTER_NAME).equals("Cluster102").and().
        property(HostComponentResourceProvider.STATE).equals("INSTALLED").and().
        property(HostComponentResourceProvider.COMPONENT_NAME).equals("Component100").toPredicate();
    RequestStatus requestStatus = provider.updateResources(request, predicate);
    Resource responseResource = requestStatus.getRequestResource();
    assertEquals("response msg", responseResource.getPropertyValue(PropertyHelper.getPropertyId("Requests", "message")));
    assertEquals(1000L, responseResource.getPropertyValue(PropertyHelper.getPropertyId("Requests", "id")));
    assertEquals("Accepted", responseResource.getPropertyValue(PropertyHelper.getPropertyId("Requests", "status")));
    assertTrue(requestStatus.getAssociatedResources().isEmpty());

    // verify
    verify(managementController, response, resourceProviderFactory, stageContainer);
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
    Resource.Type type = Resource.Type.HostComponent;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    DeleteStatusMetaData deleteStatusMetaData = createNiceMock(DeleteStatusMetaData.class);
    Injector injector = createNiceMock(Injector.class);

    HostComponentResourceProvider provider =
        new HostComponentResourceProvider(managementController);

    // set expectations
    expect(managementController.deleteHostComponents(
        AbstractResourceProviderTest.Matcher.getHostComponentRequestSet(
            null, null, "Component100", "Host100", null, null))).andReturn(deleteStatusMetaData);

    // replay
    replay(managementController, deleteStatusMetaData);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    provider.addObserver(observer);

    Predicate predicate = new PredicateBuilder().
        property(HostComponentResourceProvider.COMPONENT_NAME).equals("Component100").and().
        property(HostComponentResourceProvider.HOST_NAME).equals("Host100").toPredicate();
    provider.deleteResources(new RequestImpl(null, null, null, null), predicate);


    ResourceProviderEvent lastEvent = observer.getLastEvent();
    Assert.assertNotNull(lastEvent);
    Assert.assertEquals(Resource.Type.HostComponent, lastEvent.getResourceType());
    Assert.assertEquals(ResourceProviderEvent.Type.Delete, lastEvent.getType());
    Assert.assertEquals(predicate, lastEvent.getPredicate());
    Assert.assertNull(lastEvent.getRequest());

    // verify
    verify(managementController, deleteStatusMetaData);
  }

  @Test
  public void testCheckPropertyIds() throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Injector injector = createNiceMock(Injector.class);

    HostComponentResourceProvider provider =
        new HostComponentResourceProvider(managementController);

    Set<String> unsupported = provider.checkPropertyIds(Collections.singleton(PropertyHelper.getPropertyId("HostRoles", "cluster_name")));
    Assert.assertTrue(unsupported.isEmpty());

    // note that key is not in the set of known property ids.  We allow it if its parent is a known property.
    // this allows for Map type properties where we want to treat the entries as individual properties
    Assert.assertTrue(provider.checkPropertyIds(Collections.singleton(PropertyHelper.getPropertyId("HostRoles/service_name", "key"))).isEmpty());

    unsupported = provider.checkPropertyIds(Collections.singleton("bar"));
    Assert.assertEquals(1, unsupported.size());
    Assert.assertTrue(unsupported.contains("bar"));

    unsupported = provider.checkPropertyIds(Collections.singleton(PropertyHelper.getPropertyId("HostRoles", "component_name")));
    Assert.assertTrue(unsupported.isEmpty());

    unsupported = provider.checkPropertyIds(Collections.singleton("HostRoles"));
    Assert.assertTrue(unsupported.isEmpty());

    unsupported = provider.checkPropertyIds(Collections.singleton("config"));
    Assert.assertTrue(unsupported.isEmpty());

    unsupported = provider.checkPropertyIds(Collections.singleton("config/unknown_property"));
    Assert.assertTrue(unsupported.isEmpty());
  }

  @Test
  public void testUpdateResourcesNothingToUpdate() throws Exception {
    Authentication authentication = TestAuthenticationFactory.createServiceAdministrator();
    Resource.Type type = Resource.Type.HostComponent;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    HostVersionDAO hostVersionDAO = createMock(HostVersionDAO.class);
//    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);
    Injector injector = createNiceMock(Injector.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    ServiceComponentHost componentHost = createNiceMock(ServiceComponentHost.class);
    RequestStageContainer stageContainer = createNiceMock(RequestStageContainer.class);
    MaintenanceStateHelper maintenanceStateHelper = createNiceMock(MaintenanceStateHelper.class);


    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");

    Set<ServiceComponentHostResponse> nameResponse = new HashSet<>();
    nameResponse.add(new ServiceComponentHostResponse(
        "Cluster102", "Service100", "Component100", "Component 100", "Host100", "Host100",
        "INSTALLED", "", "", "", "", null));

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.findServiceName(cluster, "Component100")).andReturn("Service100").anyTimes();
    expect(clusters.getCluster("Cluster102")).andReturn(cluster).anyTimes();
    expect(cluster.getClusterId()).andReturn(2L).anyTimes();
    expect(cluster.getService("Service100")).andReturn(service).anyTimes();
    expect(service.getServiceComponent("Component100")).andReturn(component).anyTimes();
    expect(component.getServiceComponentHost("Host100")).andReturn(componentHost).anyTimes();
    expect(component.getName()).andReturn("Component100").anyTimes();
    expect(componentHost.getState()).andReturn(State.INSTALLED).anyTimes();

    //Cluster is default type.  Maintenance mode is not being tested here so the default is returned.
    expect(maintenanceStateHelper.isOperationAllowed(Resource.Type.Cluster, componentHost)).andReturn(true).anyTimes();

    expect(managementController.getHostComponents(
        EasyMock.anyObject())).andReturn(Collections.emptySet()).once();

    Map<String, Map<State, List<ServiceComponentHost>>> changedHosts = new HashMap<>();
    List<ServiceComponentHost> changedComponentHosts = new ArrayList<>();
    changedComponentHosts.add(componentHost);
    changedHosts.put("Component100", Collections.singletonMap(State.STARTED, changedComponentHosts));

    TestHostComponentResourceProvider provider =
        new TestHostComponentResourceProvider(PropertyHelper.getPropertyIds(type),
            PropertyHelper.getKeyPropertyIds(type),
            managementController, injector);
    provider.setFieldValue("maintenanceStateHelper", maintenanceStateHelper);
    provider.setFieldValue("hostVersionDAO", hostVersionDAO);

    expect(resourceProviderFactory.getHostComponentResourceProvider(
        eq(managementController))).
        andReturn(provider).anyTimes();

    // replay
    replay(managementController, resourceProviderFactory, clusters, cluster, service,
        component, componentHost, stageContainer, maintenanceStateHelper);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(HostComponentResourceProvider.STATE, "STARTED");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, mapRequestProps);

    // update the cluster named Cluster102
    Predicate predicate = new PredicateBuilder().property(
        HostComponentResourceProvider.CLUSTER_NAME).equals("Cluster102").and().
        property(HostComponentResourceProvider.STATE).equals("INSTALLED").and().
        property(HostComponentResourceProvider.COMPONENT_NAME).equals("Component100").toPredicate();


    try {
      provider.updateResources(request, predicate);
      fail("Expected exception when no resources are found to be updatable");
    } catch (NoSuchResourceException e) {
      // !!! expected
    }


    // verify
    verify(managementController, resourceProviderFactory, stageContainer);
  }

  @Test
  public void doesNotSkipInstallTaskForClient() {
    String component = "SOME_COMPONENT";
    assertFalse(shouldSkipInstallTaskForComponent(component, true, new RequestInfoBuilder().skipInstall(component).build()));
    assertFalse(shouldSkipInstallTaskForComponent(component, true, new RequestInfoBuilder().skipInstall(ALL_COMPONENTS).build()));
  }

  @Test
  public void doesNotSkipInstallTaskForOtherPhase() {
    String component = "SOME_COMPONENT";
    RequestInfoBuilder requestInfoBuilder = new RequestInfoBuilder().phase("INSTALL");
    assertFalse(shouldSkipInstallTaskForComponent(component, false, requestInfoBuilder.skipInstall(component).build()));
    assertFalse(shouldSkipInstallTaskForComponent(component, false, requestInfoBuilder.skipInstall(ALL_COMPONENTS).build()));
  }

  @Test
  public void doesNotSkipInstallTaskForExplicitException() {
    String component = "SOME_COMPONENT";
    RequestInfoBuilder requestInfoBuilder = new RequestInfoBuilder().skipInstall(ALL_COMPONENTS).doNotSkipInstall(component);
    assertFalse(shouldSkipInstallTaskForComponent(component, false, requestInfoBuilder.build()));
  }

  @Test
  public void skipsInstallTaskIfRequested() {
    String component = "SOME_COMPONENT";
    RequestInfoBuilder requestInfoBuilder = new RequestInfoBuilder().skipInstall(component);
    assertTrue(shouldSkipInstallTaskForComponent(component, false, requestInfoBuilder.build()));
  }

  @Test
  public void skipsInstallTaskForAll() {
    RequestInfoBuilder requestInfoBuilder = new RequestInfoBuilder().skipInstall(ALL_COMPONENTS);
    assertTrue(shouldSkipInstallTaskForComponent("ANY_COMPONENT", false, requestInfoBuilder.build()));
  }

  @Test
  public void doesNotSkipInstallOfPrefixedComponent() {
    String prefix = "HIVE_SERVER", component = prefix + "_INTERACTIVE";
    Map<String, String> requestInfo = new RequestInfoBuilder().skipInstall(component).build();
    assertTrue(shouldSkipInstallTaskForComponent(component, false, requestInfo));
    assertFalse(shouldSkipInstallTaskForComponent(prefix, false, requestInfo));
  }

  private static class RequestInfoBuilder {

    private String phase = CLUSTER_PHASE_INITIAL_INSTALL;
    private final Collection<String> skipInstall = new LinkedList<>();
    private final Collection<String> doNotSkipInstall = new LinkedList<>();

    public RequestInfoBuilder skipInstall(String... components) {
      skipInstall.clear();
      skipInstall.addAll(Arrays.asList(components));
      return this;
    }

    public RequestInfoBuilder doNotSkipInstall(String... components) {
      doNotSkipInstall.clear();
      doNotSkipInstall.addAll(Arrays.asList(components));
      return this;
    }

    public RequestInfoBuilder phase(String phase) {
      this.phase = phase;
      return this;
    }

    public Map<String, String> build() {
      Map<String, String> info = new HashMap<>();
      if (phase != null) {
        info.put(CLUSTER_PHASE_PROPERTY, phase);
      }
      HostComponentResourceProvider.addProvisionActionProperties(skipInstall, doNotSkipInstall, info);
      return info;
    }
  }

  // Used to directly call updateHostComponents on the resource provider.
  // This exists as a temporary solution as a result of moving updateHostComponents from
  // AmbariManagentControllerImpl to HostComponentResourceProvider.
  public static RequestStatusResponse updateHostComponents(AmbariManagementController controller,
                                                           Injector injector,
                                                           Set<ServiceComponentHostRequest> requests,
                                                           Map<String, String> requestProperties,
                                                           boolean runSmokeTest) throws Exception {
    Resource.Type type = Resource.Type.HostComponent;
    TestHostComponentResourceProvider provider =
        new TestHostComponentResourceProvider(PropertyHelper.getPropertyIds(type),
            PropertyHelper.getKeyPropertyIds(type),
            controller, injector);

    provider.setFieldValue("maintenanceStateHelper", injector.getInstance(MaintenanceStateHelper.class));
    provider.setFieldValue("hostVersionDAO", injector.getInstance(HostVersionDAO.class));

    RequestStageContainer requestStages = provider.updateHostComponents(null, requests, requestProperties,
        runSmokeTest, false, false);
    requestStages.persist();
    return requestStages.getRequestStatusResponse();
  }

  private static class TestHostComponentResourceProvider extends HostComponentResourceProvider {

    /**
     * Create a  new resource provider for the given management controller.
     *
     * @param propertyIds          the property ids
     * @param keyPropertyIds       the key property ids
     * @param managementController the management controller
     */
    public TestHostComponentResourceProvider(Set<String> propertyIds, Map<Resource.Type, String> keyPropertyIds,
                                             AmbariManagementController managementController, Injector injector) throws Exception {
      super(managementController);
    }

    public void setFieldValue(String fieldName, Object fieldValue) throws Exception {
      Class<?> c = getClass().getSuperclass();
      Field f = c.getDeclaredField(fieldName);
      f.setAccessible(true);
      f.set(this, fieldValue);
    }

  }
}
