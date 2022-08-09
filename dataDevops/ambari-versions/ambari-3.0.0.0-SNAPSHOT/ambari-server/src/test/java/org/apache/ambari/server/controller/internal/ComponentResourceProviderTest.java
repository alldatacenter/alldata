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

import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.agent.stomp.MetadataHolder;
import org.apache.ambari.server.agent.stomp.TopologyHolder;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.controller.AmbariManagementControllerImplTest;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ServiceComponentRequest;
import org.apache.ambari.server.controller.ServiceComponentResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelperInitializer;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.topology.STOMPComponentsDeleteHandler;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Injector;
import com.google.inject.Provider;

/**
 * Tests for the component resource provider.
 */
public class ComponentResourceProviderTest {
  private static final long CLUSTER_ID = 100;
  private static final String CLUSTER_NAME = "Cluster100";
  private static final String SERVICE_NAME = "Service100";

  @Before
  public void clearAuthentication() {
    AuthorizationHelperInitializer.viewInstanceDAOReturningNull();
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
    Resource.Type type = Resource.Type.Component;

    MaintenanceStateHelper maintenanceStateHelper = createNiceMock(MaintenanceStateHelper.class);
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    ServiceComponentFactory serviceComponentFactory = createNiceMock(ServiceComponentFactory.class);
    ServiceComponent serviceComponent = createNiceMock(ServiceComponent.class);
    StackId stackId = createNiceMock(StackId.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);

    expect(managementController.getClusters()).andReturn(clusters);
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo);
    expect(managementController.getServiceComponentFactory()).andReturn(serviceComponentFactory);

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(cluster.getService("Service100")).andReturn(service).anyTimes();
    expect(cluster.getClusterId()).andReturn(2L).anyTimes();

    expect(service.getDesiredStackId()).andReturn(stackId).anyTimes();
    expect(service.getName()).andReturn("Service100").anyTimes();

    expect(stackId.getStackName()).andReturn("HDP").anyTimes();
    expect(stackId.getStackVersion()).andReturn("99").anyTimes();

    expect(ambariMetaInfo.isValidServiceComponent("HDP", "99", "Service100", "Component100")).andReturn(true).anyTimes();

    expect(componentInfo.getName()).andReturn("Component100").anyTimes();
    expect(componentInfo.isRecoveryEnabled()).andReturn(true).anyTimes();
    expect(ambariMetaInfo.getComponent("HDP", "99", "Service100", "Component100")).andReturn(componentInfo).anyTimes();

    expect(serviceComponentFactory.createNew(service, "Component100")).andReturn(serviceComponent);

    // replay
    replay(managementController, response, clusters, cluster, service, stackId, ambariMetaInfo,
        serviceComponentFactory, serviceComponent, componentInfo);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = new ComponentResourceProvider(managementController,
        maintenanceStateHelper);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    // Service 1: create a map of properties for the request
    Map<String, Object> properties = new LinkedHashMap<>();

    // add properties to the request map
    properties.put(ComponentResourceProvider.CLUSTER_NAME, "Cluster100");
    properties.put(ComponentResourceProvider.SERVICE_NAME, "Service100");
    properties.put(ComponentResourceProvider.COMPONENT_NAME, "Component100");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    // verify
    verify(managementController, response, clusters, cluster, service, stackId, ambariMetaInfo,
        serviceComponentFactory, serviceComponent);
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
    Resource.Type type = Resource.Type.Component;

    MaintenanceStateHelper maintenanceStateHelper = createNiceMock(MaintenanceStateHelper.class);
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent serviceComponent1 = createNiceMock(ServiceComponent.class);
    ServiceComponent serviceComponent2 = createNiceMock(ServiceComponent.class);
    ServiceComponent serviceComponent3 = createNiceMock(ServiceComponent.class);
    StackId stackId = new StackId("FOO-1.0");
    final ComponentInfo componentInfo1 = createNiceMock(ComponentInfo.class);
    final ComponentInfo componentInfo2 = createNiceMock(ComponentInfo.class);
    Map <String, Integer> serviceComponentStateCountMap = new HashMap<>();
    serviceComponentStateCountMap.put("startedCount", 1);
    serviceComponentStateCountMap.put("installedCount", 0);
    serviceComponentStateCountMap.put("installedAndMaintenanceOffCount", 0);
    serviceComponentStateCountMap.put("installFailedCount", 0);
    serviceComponentStateCountMap.put("initCount", 0);
    serviceComponentStateCountMap.put("unknownCount", 1);
    serviceComponentStateCountMap.put("totalCount", 2);

    Map<String, ServiceComponent> serviceComponentMap = new HashMap<>();
    serviceComponentMap.put("Component101", serviceComponent1);
    serviceComponentMap.put("Component102", serviceComponent2);
    serviceComponentMap.put("Component103", serviceComponent3);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters);
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo);
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(serviceComponent1.getName()).andReturn("Component100");
    expect(serviceComponent1.getDesiredStackId()).andReturn(stackId).anyTimes();
    expect(serviceComponent2.getName()).andReturn("Component101");
    expect(serviceComponent2.getDesiredStackId()).andReturn(stackId).anyTimes();
    expect(serviceComponent3.getName()).andReturn("Component102");
    expect(serviceComponent3.getDesiredStackId()).andReturn(stackId).anyTimes();

    expect(cluster.getServices()).andReturn(Collections.singletonMap("Service100", service)).anyTimes();

    expect(service.getServiceComponents()).andReturn(serviceComponentMap).anyTimes();

    expect(serviceComponent1.convertToResponse()).andReturn(
      new ServiceComponentResponse(100L, "Cluster100", "Service100", "Component100", stackId, "", serviceComponentStateCountMap,
              true /* recovery enabled */, "Component100 Client", null, null));
    expect(serviceComponent2.convertToResponse()).andReturn(
      new ServiceComponentResponse(100L, "Cluster100", "Service100", "Component101", stackId, "", serviceComponentStateCountMap,
              false /* recovery not enabled */, "Component101 Client", null, null));
    expect(serviceComponent3.convertToResponse()).andReturn(
      new ServiceComponentResponse(100L, "Cluster100", "Service100", "Component102", stackId, "", serviceComponentStateCountMap,
              true /* recovery enabled */, "Component102 Client", "1.1", RepositoryVersionState.CURRENT));

    expect(ambariMetaInfo.getComponent("FOO", "1.0", null, "Component100")).andReturn(
        componentInfo1);
    expect(ambariMetaInfo.getComponent("FOO", "1.0", null, "Component101")).andReturn(
        componentInfo2);
    expect(ambariMetaInfo.getComponent("FOO", "1.0", null, "Component102")).andReturn(
        componentInfo1);

    expect(componentInfo1.getCategory()).andReturn("MASTER").anyTimes();
    expect(componentInfo2.getCategory()).andReturn("SLAVE").anyTimes();

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, service,
        serviceComponent1, serviceComponent2, serviceComponent3,
      componentInfo1, componentInfo2);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = new ComponentResourceProvider(managementController,
        maintenanceStateHelper);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(ComponentResourceProvider.CLUSTER_NAME);
    propertyIds.add(ComponentResourceProvider.COMPONENT_NAME);
    propertyIds.add(ComponentResourceProvider.CATEGORY);
    propertyIds.add(ComponentResourceProvider.TOTAL_COUNT);
    propertyIds.add(ComponentResourceProvider.STARTED_COUNT);
    propertyIds.add(ComponentResourceProvider.INSTALLED_COUNT);
    propertyIds.add(ComponentResourceProvider.INSTALLED_AND_MAINTENANCE_OFF_COUNT);
    propertyIds.add(ComponentResourceProvider.INSTALL_FAILED_COUNT);
    propertyIds.add(ComponentResourceProvider.INIT_COUNT);
    propertyIds.add(ComponentResourceProvider.UNKNOWN_COUNT);
    propertyIds.add(ComponentResourceProvider.RECOVERY_ENABLED);
    propertyIds.add(ComponentResourceProvider.DESIRED_VERSION);
    propertyIds.add(ComponentResourceProvider.REPOSITORY_STATE);

    Predicate predicate = new PredicateBuilder()
      .property(ComponentResourceProvider.CLUSTER_NAME)
      .equals("Cluster100")
      .and()
      .property(ComponentResourceProvider.CATEGORY)
      .equals("MASTER").toPredicate();

    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(2, resources.size());
    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(
          ComponentResourceProvider.CLUSTER_NAME);
      Assert.assertEquals("Cluster100", clusterName);
      Assert.assertEquals("MASTER", resource.getPropertyValue(
          ComponentResourceProvider.CATEGORY));
      Assert.assertEquals(2, resource.getPropertyValue(
        ComponentResourceProvider.TOTAL_COUNT));
      Assert.assertEquals(1, resource.getPropertyValue(
        ComponentResourceProvider.STARTED_COUNT));
      Assert.assertEquals(0, resource.getPropertyValue(
        ComponentResourceProvider.INSTALLED_COUNT));
      Assert.assertEquals(0, resource.getPropertyValue(
        ComponentResourceProvider.INSTALLED_AND_MAINTENANCE_OFF_COUNT));
      Assert.assertEquals(0, resource.getPropertyValue(
          ComponentResourceProvider.INSTALL_FAILED_COUNT));
      Assert.assertEquals(0, resource.getPropertyValue(
          ComponentResourceProvider.INIT_COUNT));
      Assert.assertEquals(1, resource.getPropertyValue(
          ComponentResourceProvider.UNKNOWN_COUNT));
      Assert.assertEquals(String.valueOf(true), resource.getPropertyValue(
        ComponentResourceProvider.RECOVERY_ENABLED));

      if (resource.getPropertyValue(
          ComponentResourceProvider.COMPONENT_NAME).equals("Component102")) {
        Assert.assertNotNull(resource.getPropertyValue(ComponentResourceProvider.REPOSITORY_STATE));
        Assert.assertNotNull(resource.getPropertyValue(ComponentResourceProvider.DESIRED_VERSION));
        Assert.assertEquals(RepositoryVersionState.CURRENT, resource.getPropertyValue(ComponentResourceProvider.REPOSITORY_STATE));
        Assert.assertEquals("1.1", resource.getPropertyValue(ComponentResourceProvider.DESIRED_VERSION));
      } else {
        Assert.assertNull(resource.getPropertyValue(ComponentResourceProvider.REPOSITORY_STATE));
        Assert.assertNull(resource.getPropertyValue(ComponentResourceProvider.DESIRED_VERSION));
      }
    }

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, service, serviceComponent1,
        serviceComponent2, serviceComponent3, componentInfo1, componentInfo2);
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
    Resource.Type type = Resource.Type.Component;

    MaintenanceStateHelper maintenanceStateHelper = createNiceMock(MaintenanceStateHelper.class);
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    Service service = createNiceMock(Service.class);
    ComponentInfo component1Info = createNiceMock(ComponentInfo.class);
    ComponentInfo component2Info = createNiceMock(ComponentInfo.class);
    ComponentInfo component3Info = createNiceMock(ComponentInfo.class);

    ServiceComponent serviceComponent1 = createNiceMock(ServiceComponent.class);
    ServiceComponent serviceComponent2 = createNiceMock(ServiceComponent.class);
    ServiceComponent serviceComponent3 = createNiceMock(ServiceComponent.class);
    ServiceComponentHost serviceComponentHost = createNiceMock(ServiceComponentHost.class);
    RequestStatusResponse requestStatusResponse = createNiceMock(RequestStatusResponse.class);
    StackId stackId = new StackId("stackName-1");

    Map<String, ServiceComponent> serviceComponentMap = new HashMap<>();
    serviceComponentMap.put("Component101", serviceComponent1);
    serviceComponentMap.put("Component102", serviceComponent2);
    serviceComponentMap.put("Component103", serviceComponent3);

    Map <String, Integer> serviceComponentStateCountMap = new HashMap<>();
    serviceComponentStateCountMap.put("startedCount", 0);
    serviceComponentStateCountMap.put("installedCount", 1);
    serviceComponentStateCountMap.put("installedAndMaintenanceOffCount", 0);
    serviceComponentStateCountMap.put("installFailedCount", 0);
    serviceComponentStateCountMap.put("initCount", 0);
    serviceComponentStateCountMap.put("unknownCount", 0);
    serviceComponentStateCountMap.put("totalCount", 1);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getEffectiveMaintenanceState(
        capture(EasyMock.newCapture()))).andReturn(MaintenanceState.OFF).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();

    expect(cluster.getService("Service100")).andReturn(service).anyTimes();
    expect(service.getName()).andReturn("Service100").anyTimes();
    expect(service.getServiceComponent("Component101")).andReturn(serviceComponent1).anyTimes();
    expect(service.getServiceComponent("Component102")).andReturn(serviceComponent1).anyTimes();
    expect(service.getServiceComponent("Component103")).andReturn(serviceComponent2).anyTimes();

    expect(serviceComponent1.getName()).andReturn("Component101").anyTimes();
    expect(serviceComponent1.getDesiredStackId()).andReturn(stackId).anyTimes();
    expect(serviceComponent2.getName()).andReturn("Component102").anyTimes();
    expect(serviceComponent2.getDesiredStackId()).andReturn(stackId).anyTimes();
    expect(serviceComponent3.getName()).andReturn("Component103").anyTimes();
    expect(serviceComponent3.getDesiredStackId()).andReturn(stackId).anyTimes();

    expect(cluster.getServices()).andReturn(Collections.singletonMap("Service100", service)).anyTimes();
    expect(cluster.getClusterId()).andReturn(2L).anyTimes();

    expect(service.getServiceComponents()).andReturn(serviceComponentMap).anyTimes();

    expect(ambariMetaInfo.getComponent("stackName", "1", "Service100", "Component101")).andReturn(component1Info).atLeastOnce();
    expect(ambariMetaInfo.getComponent("stackName", "1", "Service100", "Component102")).andReturn(component2Info).atLeastOnce();
    expect(ambariMetaInfo.getComponent("stackName", "1", "Service100", "Component103")).andReturn(component3Info).atLeastOnce();
    expect(component1Info.getCategory()).andReturn(null);
    expect(component2Info.getCategory()).andReturn(null);
    expect(component3Info.getCategory()).andReturn(null);

    expect(serviceComponent1.convertToResponse()).andReturn(
      new ServiceComponentResponse(100L, "Cluster100", "Service100", "Component101", stackId, "", serviceComponentStateCountMap,
              false /* recovery not enabled */, "Component101 Client", null, null));
    expect(serviceComponent2.convertToResponse()).andReturn(
      new ServiceComponentResponse(100L, "Cluster100", "Service100", "Component102", stackId, "", serviceComponentStateCountMap,
              false /* recovery not enabled */, "Component102 Client", null, null));
    expect(serviceComponent3.convertToResponse()).andReturn(
      new ServiceComponentResponse(100L, "Cluster100", "Service100", "Component103", stackId, "", serviceComponentStateCountMap,
              false /* recovery not enabled */, "Component103 Client", null, null));
    expect(serviceComponent1.getDesiredState()).andReturn(State.INSTALLED).anyTimes();
    expect(serviceComponent2.getDesiredState()).andReturn(State.INSTALLED).anyTimes();
    expect(serviceComponent3.getDesiredState()).andReturn(State.INSTALLED).anyTimes();

    expect(serviceComponentHost.getState()).andReturn(State.INSTALLED).anyTimes();

    Map<String, ServiceComponentHost> serviceComponentHosts = Collections.singletonMap("Host100", serviceComponentHost);

    expect(serviceComponent1.getServiceComponentHosts()).andReturn(serviceComponentHosts).anyTimes();
    expect(serviceComponent2.getServiceComponentHosts()).andReturn(serviceComponentHosts).anyTimes();
    expect(serviceComponent3.getServiceComponentHosts()).andReturn(serviceComponentHosts).anyTimes();

    expect(maintenanceStateHelper.isOperationAllowed(anyObject(Resource.Type.class), anyObject(Service.class))).andReturn(true).anyTimes();

    Capture<Map<String, String>> requestPropertiesCapture = EasyMock.newCapture();
    Capture<Map<State, List<Service>>> changedServicesCapture = EasyMock.newCapture();
    Capture<Map<State, List<ServiceComponent>>> changedCompsCapture = EasyMock.newCapture();
    Capture<Map<String, Map<State, List<ServiceComponentHost>>>> changedScHostsCapture = EasyMock.newCapture();
    Capture<Map<String, String>> requestParametersCapture = EasyMock.newCapture();
    Capture<Collection<ServiceComponentHost>> ignoredScHostsCapture = EasyMock.newCapture();
    Capture<Cluster> clusterCapture = EasyMock.newCapture();

    expect(managementController.createAndPersistStages(capture(clusterCapture), capture(requestPropertiesCapture), capture(requestParametersCapture), capture(changedServicesCapture), capture(changedCompsCapture), capture(changedScHostsCapture), capture(ignoredScHostsCapture), anyBoolean(), anyBoolean()
    )).andReturn(requestStatusResponse);

    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");


    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, service, component1Info,
        component2Info, component3Info, serviceComponent1, serviceComponent2, serviceComponent3,
        serviceComponentHost, requestStatusResponse, maintenanceStateHelper);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = new ComponentResourceProvider(managementController,
        maintenanceStateHelper);

    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(ComponentResourceProvider.RECOVERY_ENABLED, String.valueOf(true) /* recovery enabled */);
    properties.put(ComponentResourceProvider.STATE, "STARTED");
    properties.put(ComponentResourceProvider.CLUSTER_NAME, "Cluster100");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, mapRequestProps);

    // update the cluster named Cluster100
    Predicate predicate = new PredicateBuilder().property(ComponentResourceProvider.CLUSTER_NAME).
        equals("Cluster100").toPredicate();
    provider.updateResources(request, predicate);

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, service, component1Info,
        component2Info, component3Info, serviceComponent1, serviceComponent2, serviceComponent3,
        serviceComponentHost, requestStatusResponse, maintenanceStateHelper);
  }

  @Test
  public void testSuccessDeleteResourcesAsAdministrator() throws Exception {
    testSuccessDeleteResources(TestAuthenticationFactory.createAdministrator(), State.INSTALLED);
  }

  @Test
  public void testSuccessDeleteResourcesAsClusterAdministrator() throws Exception {
    testSuccessDeleteResources(TestAuthenticationFactory.createClusterAdministrator(), State.INSTALLED);
  }


  @Test(expected = AuthorizationException.class)
  public void testDeleteResourcesAsServiceAdministrator() throws Exception {
    testSuccessDeleteResources(TestAuthenticationFactory.createServiceAdministrator(), State.INSTALLED);
  }

  @Test(expected = SystemException.class)
  public void testDeleteResourcesWithStartedHostComponentState() throws Exception {
    testSuccessDeleteResources(TestAuthenticationFactory.createAdministrator(), State.STARTED);
  }

  private void testSuccessDeleteResources(Authentication authentication, State hostComponentState) throws Exception {
    Resource.Type type = Resource.Type.Component;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    MaintenanceStateHelper maintenanceStateHelper = createNiceMock(MaintenanceStateHelper.class);

    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    ServiceComponent serviceComponent = createNiceMock(ServiceComponent.class);
    ServiceComponentHost serviceComponentHost = createNiceMock(ServiceComponentHost.class);
    StackId stackId = createNiceMock(StackId.class);

    Map<String, ServiceComponentHost> serviceComponentHosts = new HashMap<>();
    serviceComponentHosts.put("", serviceComponentHost);

    expect(managementController.getClusters()).andReturn(clusters);
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo);

    expect(clusters.getCluster(CLUSTER_NAME)).andReturn(cluster);
    expect(cluster.getService(SERVICE_NAME)).andReturn(service);
    expect(cluster.getClusterId()).andReturn(CLUSTER_ID).anyTimes();

    expect(service.getServiceComponent("Component100")).andReturn(serviceComponent);

    expect(serviceComponent.getDesiredState()).andReturn(State.STARTED);
    expect(serviceComponent.getServiceComponentHosts()).andReturn(serviceComponentHosts).anyTimes();

    expect(serviceComponentHost.getDesiredState()).andReturn(hostComponentState);


    service.deleteServiceComponent(eq("Component100"), anyObject(DeleteHostComponentStatusMetaData.class));
    expectLastCall().once();
    // replay

    replay(managementController, clusters, cluster, service, stackId, ambariMetaInfo,
           serviceComponent, serviceComponentHost, maintenanceStateHelper);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = getComponentProvider(managementController,
        maintenanceStateHelper);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider)provider).addObserver(observer);


    Predicate predicate = new PredicateBuilder()
                .property(ComponentResourceProvider.CLUSTER_NAME)
                .equals("Cluster100")
                .and()
                .property(ComponentResourceProvider.SERVICE_NAME)
                .equals("Service100")
                .and()
                .property(ComponentResourceProvider.COMPONENT_NAME)
                .equals("Component100").toPredicate();

    provider.deleteResources(new RequestImpl(null, null, null, null), predicate);

    // verify
    verify(managementController, service);
  }
  public static ComponentResourceProvider getComponentProvider(
      AmbariManagementController managementController,
      MaintenanceStateHelper maintenanceStateHelper)
      throws NoSuchFieldException, IllegalAccessException {
    ComponentResourceProvider provider = new ComponentResourceProvider(managementController,
        maintenanceStateHelper);

    Field STOMPComponentsDeleteHandlerField = ComponentResourceProvider.class.getDeclaredField("STOMPComponentsDeleteHandler");
    STOMPComponentsDeleteHandlerField.setAccessible(true);
    STOMPComponentsDeleteHandler STOMPComponentsDeleteHandler = new STOMPComponentsDeleteHandler();
    STOMPComponentsDeleteHandlerField.set(provider, STOMPComponentsDeleteHandler);

    Field topologyHolderProviderField = STOMPComponentsDeleteHandler.class.getDeclaredField("m_topologyHolder");
    topologyHolderProviderField.setAccessible(true);
    Provider<TopologyHolder> m_topologyHolder = createMock(Provider.class);
    topologyHolderProviderField.set(STOMPComponentsDeleteHandler, m_topologyHolder);

    TopologyHolder topologyHolder = createNiceMock(TopologyHolder.class);

    expect(m_topologyHolder.get()).andReturn(topologyHolder).anyTimes();

    Field metadataHolderProviderField = STOMPComponentsDeleteHandler.class.getDeclaredField("metadataHolder");
    metadataHolderProviderField.setAccessible(true);
    Provider<MetadataHolder> m_metadataHolder = createMock(Provider.class);
    metadataHolderProviderField.set(STOMPComponentsDeleteHandler, m_metadataHolder);

    MetadataHolder metadataHolder = createNiceMock(MetadataHolder.class);

    expect(m_metadataHolder.get()).andReturn(metadataHolder).anyTimes();

    replay(m_metadataHolder, metadataHolder, m_topologyHolder, topologyHolder);
    return provider;
  }

  @Test
  public void testDeleteResourcesWithEmptyClusterComponentNamesAsAdministrator() throws Exception {
    testDeleteResourcesWithEmptyClusterComponentNames(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testDeleteResourcesWithEmptyClusterComponentNamesAsClusterAdministrator() throws Exception {
    testDeleteResourcesWithEmptyClusterComponentNames(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResourcesWithEmptyClusterComponentNamesAsServiceAdministrator() throws Exception {
    testDeleteResourcesWithEmptyClusterComponentNames(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testDeleteResourcesWithEmptyClusterComponentNames(Authentication authentication) throws Exception {
    Resource.Type type = Resource.Type.Component;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    MaintenanceStateHelper maintenanceStateHelper = createNiceMock(MaintenanceStateHelper.class);

    Clusters clusters = createNiceMock(Clusters.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();

    replay(managementController, clusters, ambariMetaInfo, maintenanceStateHelper);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = new ComponentResourceProvider(managementController,
        maintenanceStateHelper);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider)provider).addObserver(observer);

    Predicate predicate1 = new PredicateBuilder()
                .property(ComponentResourceProvider.SERVICE_NAME)
                .equals("Service100")
                .and()
                .property(ComponentResourceProvider.COMPONENT_NAME)
                .equals("Component100").toPredicate();

    try {
      provider.deleteResources(new RequestImpl(null, null, null, null), predicate1);
      Assert.fail("Expected IllegalArgumentException exception.");
    } catch (IllegalArgumentException e) {
      //expected
    }

    Predicate predicate2 = new PredicateBuilder()
                .property(ComponentResourceProvider.CLUSTER_NAME)
                .equals("Cluster100")
                .and()
                .property(ComponentResourceProvider.SERVICE_NAME)
                .equals("Service100")
                .and().toPredicate();

    try {
      provider.deleteResources(new RequestImpl(null, null, null, null), predicate2);
      Assert.fail("Expected IllegalArgumentException exception.");
    } catch (IllegalArgumentException e) {
      //expected
    }

    // verify
    verify(managementController);
  }

  @Test
  public void testUpdateAutoStartAsAdministrator() throws Exception {
    testUpdateAutoStart(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testUpdateAutoStartAsClusterAdministrator() throws Exception {
    testUpdateAutoStart(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testUpdateAutoStartAsServiceAdministrator() throws Exception {
    testUpdateAutoStart(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateAutoStartAsClusterUser() throws Exception {
    testUpdateAutoStart(TestAuthenticationFactory.createClusterUser());
  }

  /**
   * Perform steps to test updating the Auto-Start property (ServiceComponentInfo/recovery_enabled)
   * of a service.
   *
   * @param authentication the authentication and authorization details of the acting user
   * @throws Exception
   */
  private void testUpdateAutoStart(Authentication authentication) throws Exception {
    Resource.Type type = Resource.Type.Component;

    MaintenanceStateHelper maintenanceStateHelper = createMock(MaintenanceStateHelper.class);
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createMock(Cluster.class);
    AmbariMetaInfo ambariMetaInfo = createMock(AmbariMetaInfo.class);
    Service service = createMock(Service.class);
    ComponentInfo component1Info = createMock(ComponentInfo.class);

    ServiceComponent serviceComponent1 = createMock(ServiceComponent.class);
    ServiceComponentHost serviceComponentHost = createMock(ServiceComponentHost.class);
    RequestStatusResponse requestStatusResponse = createNiceMock(RequestStatusResponse.class);
    StackId stackId = new StackId("stackName-1");

    Map<String, ServiceComponent> serviceComponentMap = new HashMap<>();
    serviceComponentMap.put("Component101", serviceComponent1);

    Map <String, Integer> serviceComponentStateCountMap = new HashMap<>();
    serviceComponentStateCountMap.put("startedCount", 0);
    serviceComponentStateCountMap.put("installedCount", 1);
    serviceComponentStateCountMap.put("installedAndMaintenanceOffCount", 0);
    serviceComponentStateCountMap.put("installFailedCount", 0);
    serviceComponentStateCountMap.put("initCount", 0);
    serviceComponentStateCountMap.put("unknownCount", 0);
    serviceComponentStateCountMap.put("totalCount", 1);


    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getEffectiveMaintenanceState(
        capture(EasyMock.newCapture()))).andReturn(MaintenanceState.OFF).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();

    expect(cluster.getResourceId()).andReturn(4l).atLeastOnce();
    expect(cluster.getServices()).andReturn(Collections.singletonMap("Service100", service)).anyTimes();
    expect(cluster.getClusterId()).andReturn(2L).anyTimes();

    expect(cluster.getService("Service100")).andReturn(service).anyTimes();
    expect(service.getName()).andReturn("Service100").anyTimes();
    expect(service.getServiceComponent("Component101")).andReturn(serviceComponent1).anyTimes();

    expect(serviceComponent1.getName()).andReturn("Component101").atLeastOnce();
    expect(serviceComponent1.isRecoveryEnabled()).andReturn(false).atLeastOnce();
    expect(serviceComponent1.getDesiredStackId()).andReturn(stackId).anyTimes();
    serviceComponent1.setRecoveryEnabled(true);
    expectLastCall().once();

    expect(service.getServiceComponents()).andReturn(serviceComponentMap).anyTimes();

    expect(ambariMetaInfo.getComponent("stackName", "1", "Service100", "Component101")).andReturn(component1Info).atLeastOnce();
    expect(component1Info.getCategory()).andReturn(null);

    expect(serviceComponent1.convertToResponse()).andReturn(
        new ServiceComponentResponse(100L, "Cluster100", "Service100", "Component101", stackId, "", serviceComponentStateCountMap,
            false /* recovery not enabled */, "Component101 Client", null, null));
    expect(serviceComponent1.getDesiredState()).andReturn(State.INSTALLED).anyTimes();

    expect(serviceComponentHost.getState()).andReturn(State.INSTALLED).anyTimes();

    Map<String, ServiceComponentHost> serviceComponentHosts = Collections.singletonMap("Host100", serviceComponentHost);

    expect(serviceComponent1.getServiceComponentHosts()).andReturn(serviceComponentHosts).anyTimes();

    expect(maintenanceStateHelper.isOperationAllowed(anyObject(Resource.Type.class), anyObject(Service.class))).andReturn(true).anyTimes();

    Capture<Map<String, String>> requestPropertiesCapture = EasyMock.newCapture();
    Capture<Map<State, List<Service>>> changedServicesCapture = EasyMock.newCapture();
    Capture<Map<State, List<ServiceComponent>>> changedCompsCapture = EasyMock.newCapture();
    Capture<Map<String, Map<State, List<ServiceComponentHost>>>> changedScHostsCapture = EasyMock.newCapture();
    Capture<Map<String, String>> requestParametersCapture = EasyMock.newCapture();
    Capture<Collection<ServiceComponentHost>> ignoredScHostsCapture = EasyMock.newCapture();
    Capture<Cluster> clusterCapture = EasyMock.newCapture();

    expect(managementController.createAndPersistStages(capture(clusterCapture), capture(requestPropertiesCapture), capture(requestParametersCapture), capture(changedServicesCapture), capture(changedCompsCapture), capture(changedScHostsCapture), capture(ignoredScHostsCapture), anyBoolean(), anyBoolean()
    )).andReturn(requestStatusResponse);

    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");

    // replay
    replay(managementController, clusters, cluster, ambariMetaInfo, service, component1Info,
        serviceComponent1, serviceComponentHost, requestStatusResponse, maintenanceStateHelper);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = new ComponentResourceProvider(managementController,
        maintenanceStateHelper);

    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(ComponentResourceProvider.RECOVERY_ENABLED, String.valueOf(true) /* recovery enabled */);

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, mapRequestProps);

    // update the cluster named Cluster100
    Predicate predicate = new PredicateBuilder().property(ComponentResourceProvider.CLUSTER_NAME).
        equals("Cluster100").toPredicate();
    provider.updateResources(request, predicate);

    // verify
    verify(managementController, clusters, cluster, ambariMetaInfo, service, component1Info,
        serviceComponent1, serviceComponentHost, requestStatusResponse, maintenanceStateHelper);
  }

  @Test
  public void testGetComponents() throws Exception {
    // member state mocks
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    ComponentInfo componentInfo = createNiceMock(ComponentInfo.class);
    ServiceComponent component = createNiceMock(ServiceComponent.class);
    ServiceComponentResponse response = createNiceMock(ServiceComponentResponse.class);

    // requests
    ServiceComponentRequest request1 = new ServiceComponentRequest("cluster1", "service1", "component1",
        null, String.valueOf(true /* recovery enabled */));

    Set<ServiceComponentRequest> setRequests = new HashSet<>();
    setRequests.add(request1);

    // expectations
    // constructor init
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();

    expect(stackId.getStackName()).andReturn("stackName").anyTimes();
    expect(stackId.getStackVersion()).andReturn("1").anyTimes();

    // getComponents
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(cluster.getService("service1")).andReturn(service);
    expect(service.getName()).andReturn("service1").anyTimes();
    expect(service.getServiceComponent("component1")).andReturn(component);

    expect(ambariMetaInfo.getComponent("stackName", "1", "service1", "component1")).andReturn(componentInfo);
    expect(componentInfo.getCategory()).andReturn(null);

    expect(component.getDesiredStackId()).andReturn(stackId).anyTimes();
    expect(component.convertToResponse()).andReturn(response);
    // replay mocks
    replay(clusters, cluster, service, componentInfo, component, response, ambariMetaInfo, stackId, managementController);

    //test
    Set<ServiceComponentResponse> setResponses = getComponentResourceProvider(managementController).getComponents(setRequests);

    // assert and verify
    assertEquals(1, setResponses.size());
    assertTrue(setResponses.contains(response));

    verify(clusters, cluster, service, componentInfo, component, response, ambariMetaInfo, stackId, managementController);
  }

  /**
   * Ensure that ServiceComponentNotFoundException is handled where there are multiple requests as would be the
   * case when an OR predicate is provided in the query.
   */
  @Test
  public void testGetComponents_OR_Predicate_ServiceComponentNotFoundException() throws Exception {
    // member state mocks
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    StackId stackId = createNiceMock(StackId.class);

    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    ComponentInfo component3Info = createNiceMock(ComponentInfo.class);
    ComponentInfo component4Info = createNiceMock(ComponentInfo.class);
    ServiceComponent component1 = createNiceMock(ServiceComponent.class);
    ServiceComponent component2 = createNiceMock(ServiceComponent.class);
    ServiceComponentResponse response1 = createNiceMock(ServiceComponentResponse.class);
    ServiceComponentResponse response2 = createNiceMock(ServiceComponentResponse.class);

    // requests
    ServiceComponentRequest request1 = new ServiceComponentRequest("cluster1", "service1", "component1",
        null, String.valueOf(true /* recovery enabled */));
    ServiceComponentRequest request2 = new ServiceComponentRequest("cluster1", "service1", "component2",
        null, String.valueOf(true /* recovery enabled */));
    ServiceComponentRequest request3 = new ServiceComponentRequest("cluster1", "service1", "component3",
        null, String.valueOf(true /* recovery enabled */));
    ServiceComponentRequest request4 = new ServiceComponentRequest("cluster1", "service1", "component4",
        null, String.valueOf(true /* recovery enabled */));
    ServiceComponentRequest request5 = new ServiceComponentRequest("cluster1", "service2", null, null,
              String.valueOf(true /* recovery enabled */));

    Set<ServiceComponentRequest> setRequests = new HashSet<>();
    setRequests.add(request1);
    setRequests.add(request2);
    setRequests.add(request3);
    setRequests.add(request4);
    setRequests.add(request5);

    // expectations
    // constructor init
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();

    expect(stackId.getStackName()).andReturn("stackName").anyTimes();
    expect(stackId.getStackVersion()).andReturn("1").anyTimes();

    // getComponents
    expect(clusters.getCluster("cluster1")).andReturn(cluster).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(stackId).anyTimes();
    expect(cluster.getService("service1")).andReturn(service).anyTimes();
    expect(cluster.getService("service2")).andThrow(new ObjectNotFoundException("service2"));

    expect(ambariMetaInfo.getComponent("stackName", "1", "service1", "component3")).andReturn(component3Info);
    expect(ambariMetaInfo.getComponent("stackName", "1", "service1", "component4")).andReturn(component4Info);

    expect(component3Info.getCategory()).andReturn(null);
    expect(component4Info.getCategory()).andReturn(null);

    expect(service.getName()).andReturn("service1").anyTimes();
    expect(service.getServiceComponent("component1")).andThrow(new ServiceComponentNotFoundException("cluster1", "service1", "component1"));
    expect(service.getServiceComponent("component2")).andThrow(new ServiceComponentNotFoundException("cluster1", "service1", "component2"));
    expect(service.getServiceComponent("component3")).andReturn(component1);
    expect(service.getServiceComponent("component4")).andReturn(component2);

    expect(component1.convertToResponse()).andReturn(response1);
    expect(component1.getDesiredStackId()).andReturn(stackId).anyTimes();
    expect(component2.convertToResponse()).andReturn(response2);
    expect(component2.getDesiredStackId()).andReturn(stackId).anyTimes();
    // replay mocks
    replay(clusters, cluster, service, component3Info, component4Info, component1,  component2, response1,
        response2, ambariMetaInfo, stackId, managementController);

    //test
    Set<ServiceComponentResponse> setResponses = getComponentResourceProvider(managementController).getComponents(setRequests);

    // assert and verify
    assertEquals(2, setResponses.size());
    assertTrue(setResponses.contains(response1));
    assertTrue(setResponses.contains(response2));

    verify(clusters, cluster, service, component3Info, component4Info, component1, component2, response1,
        response2, ambariMetaInfo, stackId, managementController);
  }

  public static ComponentResourceProvider getComponentResourceProvider(AmbariManagementController managementController)
          throws AmbariException {
    Resource.Type type = Resource.Type.Component;
    MaintenanceStateHelper maintenanceStateHelper = createNiceMock(MaintenanceStateHelper.class);
    expect(maintenanceStateHelper.isOperationAllowed(anyObject(Resource.Type.class),
            anyObject(Service.class))).andReturn(true).anyTimes();
    expect(maintenanceStateHelper.isOperationAllowed(anyObject(Resource.Type.class),
            anyObject(ServiceComponentHost.class))).andReturn(true).anyTimes();
    replay(maintenanceStateHelper);

    return new ComponentResourceProvider(managementController, maintenanceStateHelper);
  }

  /**
   * Ensure that ServiceComponentNotFoundException is propagated in case where there is a single request.
   */
  @Test
  public void testGetComponents_ServiceComponentNotFoundException() throws Exception {
    // member state mocks
    Injector injector = createStrictMock(Injector.class);
    Capture<AmbariManagementController> controllerCapture = EasyMock.newCapture();
    Clusters clusters = createNiceMock(Clusters.class);
    MaintenanceStateHelper maintHelper = createNiceMock(MaintenanceStateHelper.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);

    // requests
    ServiceComponentRequest request1 = new ServiceComponentRequest("cluster1", "service1", "component1",
        null, String.valueOf(true /* recovery enabled */));

    Set<ServiceComponentRequest> setRequests = new HashSet<>();
    setRequests.add(request1);

    // expectations
    // constructor init
    AmbariManagementControllerImplTest.constructorInit(injector, controllerCapture, null, maintHelper,
        createNiceMock(KerberosHelper.class), null, null);

    // getComponents
    expect(clusters.getCluster("cluster1")).andReturn(cluster);
    expect(cluster.getService("service1")).andReturn(service);
    expect(service.getServiceComponent("component1")).andThrow(
        new ServiceComponentNotFoundException("cluster1", "service1", "component1"));
    // replay mocks
    replay(maintHelper, injector, clusters, cluster, service);

    //test
    AmbariManagementController controller = new AmbariManagementControllerImpl(null, clusters, injector);

    // assert that exception is thrown in case where there is a single request
    try {
      getComponentResourceProvider(controller).getComponents(setRequests);
      fail("expected ServiceComponentNotFoundException");
    } catch (ServiceComponentNotFoundException e) {
      // expected
    }

    assertSame(controller, controllerCapture.getValue());
    verify(injector, clusters, cluster, service);
  }

  public static void createComponents(AmbariManagementController controller, Set<ServiceComponentRequest> requests)
      throws AmbariException, AuthorizationException {
    ComponentResourceProvider provider = getComponentResourceProvider(controller);
    provider.createComponents(requests);
  }

  public static Set<ServiceComponentResponse> getComponents(AmbariManagementController controller,
                                                 Set<ServiceComponentRequest> requests) throws AmbariException {
    ComponentResourceProvider provider = getComponentResourceProvider(controller);
    return provider.getComponents(requests);
  }

  public static RequestStatusResponse updateComponents(AmbariManagementController controller,
                                                     Set<ServiceComponentRequest> requests,
                                                     Map<String, String> requestProperties, boolean runSmokeTest)
      throws AmbariException, AuthorizationException {
    ComponentResourceProvider provider = getComponentResourceProvider(controller);
    return provider.updateComponents(requests, requestProperties, runSmokeTest);
  }
}
