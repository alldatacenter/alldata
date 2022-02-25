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
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

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
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ServiceRequest;
import org.apache.ambari.server.controller.ServiceResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.utilities.state.DefaultServiceCalculatedState;
import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.serveraction.kerberos.KerberosAdminAuthenticationException;
import org.apache.ambari.server.serveraction.kerberos.KerberosMissingAdminCredentialsException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.topology.STOMPComponentsDeleteHandler;
import org.apache.ambari.spi.RepositoryType;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.common.collect.ImmutableMap;

/**
 * ServiceResourceProvider tests.
 */
public class ServiceResourceProviderTest {

  @BeforeClass
  public static void resetDefaultServiceCalculatedState() throws NoSuchFieldException, IllegalAccessException {
    Field clustersProviderField = DefaultServiceCalculatedState.class.getDeclaredField("clustersProvider");
    clustersProviderField.setAccessible(true);
    clustersProviderField.set(null, null);
  }

  @Before
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testCreateResourcesAsAdministrator() throws Exception{
    testCreateResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testCreateResourcesAsClusterAdministrator() throws Exception{
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesAsServiceAdministrator() throws Exception{
    testCreateResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testCreateResources(Authentication authentication) throws Exception{
    AmbariManagementController managementController = createNiceMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    StackId stackId = new StackId("HDP-2.5");
    ServiceFactory serviceFactory = createNiceMock(ServiceFactory.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    ServiceInfo serviceInfo = createNiceMock(ServiceInfo.class);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();

    expect(cluster.addService(eq("Service100"),
        EasyMock.anyObject(RepositoryVersionEntity.class))).andReturn(service);

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();

    expect(cluster.getService("Service100")).andReturn(null);
    expect(cluster.getDesiredStackVersion()).andReturn(stackId).anyTimes();
    expect(cluster.getClusterId()).andReturn(2L).anyTimes();

    expect(ambariMetaInfo.isValidService( (String) anyObject(), (String) anyObject(), (String) anyObject())).andReturn(true);
    expect(ambariMetaInfo.getService((String)anyObject(), (String)anyObject(), (String)anyObject())).andReturn(serviceInfo).anyTimes();

    // replay
    replay(managementController, clusters, cluster, service,
        ambariMetaInfo, serviceFactory, serviceInfo);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = getServiceProvider(managementController, true, null);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    // Service 1: create a map of properties for the request
    Map<String, Object> properties = new LinkedHashMap<>();

    // add properties to the request map
    properties.put(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID, "Service100");
    properties.put(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID, "INIT");
    properties.put(ServiceResourceProvider.SERVICE_DESIRED_STACK_PROPERTY_ID, "HDP-1.1");
    properties.put(ServiceResourceProvider.SERVICE_DESIRED_REPO_VERSION_ID_PROPERTY_ID, "1");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    // verify
    verify(managementController, clusters, cluster, service,
        ambariMetaInfo, serviceFactory, serviceInfo);
  }

  @Test
  public void testGetResourcesAsAdministrator() throws Exception{
    testGetResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testGetResourcesAsClusterAdministrator() throws Exception{
    testGetResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testGetResourcesAsServiceAdministrator() throws Exception{
    testGetResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testGetResources(Authentication authentication) throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service0 = createNiceMock(Service.class);
    Service service1 = createNiceMock(Service.class);
    Service service2 = createNiceMock(Service.class);
    Service service3 = createNiceMock(Service.class);
    Service service4 = createNiceMock(Service.class);
    ServiceResponse serviceResponse0 = createNiceMock(ServiceResponse.class);
    ServiceResponse serviceResponse1 = createNiceMock(ServiceResponse.class);
    ServiceResponse serviceResponse2 = createNiceMock(ServiceResponse.class);
    ServiceResponse serviceResponse3 = createNiceMock(ServiceResponse.class);
    ServiceResponse serviceResponse4 = createNiceMock(ServiceResponse.class);

    StackId stackId = createNiceMock(StackId.class);
    ServiceFactory serviceFactory = createNiceMock(ServiceFactory.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);

    Map<String, Service> allResponseMap = new HashMap<>();
    allResponseMap.put("Service100", service0);
    allResponseMap.put("Service101", service1);
    allResponseMap.put("Service102", service2);
    allResponseMap.put("Service103", service3);
    allResponseMap.put("Service104", service4);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getHostComponents(EasyMock.anyObject())).
        andReturn(Collections.emptySet()).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();

    expect(cluster.getServices()).andReturn(allResponseMap).anyTimes();
    expect(cluster.getService("Service102")).andReturn(service2);

    expect(service0.convertToResponse()).andReturn(serviceResponse0).anyTimes();
    expect(service1.convertToResponse()).andReturn(serviceResponse1).anyTimes();
    expect(service2.convertToResponse()).andReturn(serviceResponse2).anyTimes();
    expect(service3.convertToResponse()).andReturn(serviceResponse3).anyTimes();
    expect(service4.convertToResponse()).andReturn(serviceResponse4).anyTimes();

    expect(service0.getName()).andReturn("Service100").anyTimes();
    expect(service1.getName()).andReturn("Service101").anyTimes();
    expect(service2.getName()).andReturn("Service102").anyTimes();
    expect(service3.getName()).andReturn("Service103").anyTimes();
    expect(service4.getName()).andReturn("Service104").anyTimes();

    expect(service0.getDesiredState()).andReturn(State.INIT);
    expect(service1.getDesiredState()).andReturn(State.INSTALLED);
    expect(service2.getDesiredState()).andReturn(State.INIT);
    expect(service3.getDesiredState()).andReturn(State.INSTALLED);
    expect(service4.getDesiredState()).andReturn(State.INIT);

    expect(serviceResponse0.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(serviceResponse0.getServiceName()).andReturn("Service100").anyTimes();
    expect(serviceResponse1.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(serviceResponse1.getServiceName()).andReturn("Service101").anyTimes();
    expect(serviceResponse2.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(serviceResponse2.getServiceName()).andReturn("Service102").anyTimes();
    expect(serviceResponse3.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(serviceResponse3.getServiceName()).andReturn("Service103").anyTimes();
    expect(serviceResponse4.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(serviceResponse4.getServiceName()).andReturn("Service104").anyTimes();

    // replay
    replay(managementController, clusters, cluster,
        service0, service1, service2, service3, service4,
        serviceResponse0, serviceResponse1, serviceResponse2, serviceResponse3, serviceResponse4,
        ambariMetaInfo, stackId, serviceFactory);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = getServiceProvider(managementController);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID);

    // create the request
    Predicate predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").toPredicate();
    Request request = PropertyHelper.getReadRequest("ServiceInfo");
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(5, resources.size());
    Set<String> names = new HashSet<>();
    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals("Cluster100", clusterName);
      names.add((String) resource.getPropertyValue(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID));
    }
    // Make sure that all of the response objects got moved into resources
    for (Service service : allResponseMap.values() ) {
      Assert.assertTrue(names.contains(service.getName()));
    }

    // get service named Service102
    predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").and().
        property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals("Service102").toPredicate();
    request = PropertyHelper.getReadRequest("ServiceInfo");
    resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals("Service102", resources.iterator().next().getPropertyValue(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID));

    // get services where state == "INIT"
    predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").and().
        property(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID).equals("INIT").toPredicate();
    request = PropertyHelper.getReadRequest(propertyIds);
    resources = provider.getResources(request, predicate);

    Assert.assertEquals(3, resources.size());
    names = new HashSet<>();
    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals("Cluster100", clusterName);
      names.add((String) resource.getPropertyValue(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID));
    }

    // verify
    verify(managementController, clusters, cluster,
        service0, service1, service2, service3, service4,
        serviceResponse0, serviceResponse1, serviceResponse2, serviceResponse3, serviceResponse4,
        ambariMetaInfo, stackId, serviceFactory);
  }

  @Test
  public void testGetResources_KerberosSpecificProperties() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service0 = createNiceMock(Service.class);
    ServiceResponse serviceResponse0 = createNiceMock(ServiceResponse.class);

    StackId stackId = createNiceMock(StackId.class);
    ServiceFactory serviceFactory = createNiceMock(ServiceFactory.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    KerberosHelper kerberosHeper = createStrictMock(KerberosHelper.class);

    Map<String, Service> allResponseMap = new HashMap<>();
    allResponseMap.put("KERBEROS", service0);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getHostComponents(EasyMock.anyObject())).
        andReturn(Collections.emptySet()).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();

    expect(cluster.getServices()).andReturn(allResponseMap).anyTimes();
    expect(cluster.getService("KERBEROS")).andReturn(service0);

    expect(service0.convertToResponse()).andReturn(serviceResponse0).anyTimes();

    expect(service0.getName()).andReturn("Service100").anyTimes();

    expect(serviceResponse0.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(serviceResponse0.getServiceName()).andReturn("KERBEROS").anyTimes();

    kerberosHeper.validateKDCCredentials(cluster);

    // replay
    replay(managementController, clusters, cluster, service0, serviceResponse0,
        ambariMetaInfo, stackId, serviceFactory, kerberosHeper);

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    ResourceProvider provider = getServiceProvider(managementController);
    // set kerberos helper on provider
    Class<?> c = provider.getClass();
    Field f = c.getDeclaredField("kerberosHelper");
    f.setAccessible(true);
    f.set(provider, kerberosHeper);

    // create the request
    Predicate predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").and().
        property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals("KERBEROS").toPredicate();
    Request request = PropertyHelper.getReadRequest("ServiceInfo", "Services");
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      Assert.assertEquals("Cluster100", resource.getPropertyValue(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID));
      Assert.assertEquals("KERBEROS", resource.getPropertyValue(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID));
      Assert.assertEquals("OK", resource.getPropertyValue("Services/attributes/kdc_validation_result"));
      Assert.assertEquals("", resource.getPropertyValue("Services/attributes/kdc_validation_failure_details"));
    }

    // verify
    verify(managementController, clusters, cluster, service0, serviceResponse0,
        ambariMetaInfo, stackId, serviceFactory, kerberosHeper);
  }

  @Test
  public void testGetResources_KerberosSpecificProperties_NoKDCValidation() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service0 = createNiceMock(Service.class);
    ServiceResponse serviceResponse0 = createNiceMock(ServiceResponse.class);

    StackId stackId = createNiceMock(StackId.class);
    ServiceFactory serviceFactory = createNiceMock(ServiceFactory.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    KerberosHelper kerberosHelper = createStrictMock(KerberosHelper.class);

    Map<String, Service> allResponseMap = new HashMap<>();
    allResponseMap.put("KERBEROS", service0);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getHostComponents(EasyMock.anyObject())).
        andReturn(Collections.emptySet()).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();

    expect(cluster.getServices()).andReturn(allResponseMap).anyTimes();
    expect(cluster.getService("KERBEROS")).andReturn(service0);

    expect(service0.convertToResponse()).andReturn(serviceResponse0).anyTimes();

    expect(service0.getName()).andReturn("Service100").anyTimes();

    expect(serviceResponse0.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(serviceResponse0.getServiceName()).andReturn("KERBEROS").anyTimes();

    // The following call should NOT be made
    // kerberosHelper.validateKDCCredentials(cluster);

    // replay
    replay(managementController, clusters, cluster, service0, serviceResponse0,
        ambariMetaInfo, stackId, serviceFactory, kerberosHelper);

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    ResourceProvider provider = getServiceProvider(managementController);
    // set kerberos helper on provider
    Class<?> c = provider.getClass();
    Field f = c.getDeclaredField("kerberosHelper");
    f.setAccessible(true);
    f.set(provider, kerberosHelper);

    // create the request
    Predicate predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").and().
        property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals("KERBEROS").toPredicate();
    Request request = PropertyHelper.getReadRequest("ServiceInfo");
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      Assert.assertEquals("Cluster100", resource.getPropertyValue(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID));
      Assert.assertEquals("KERBEROS", resource.getPropertyValue(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID));
    }

    // verify
    verify(managementController, clusters, cluster, service0, serviceResponse0,
        ambariMetaInfo, stackId, serviceFactory, kerberosHelper);
  }

  @Test
  public void testGetResources_KerberosSpecificProperties_KDCInvalidCredentials() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service0 = createNiceMock(Service.class);
    ServiceResponse serviceResponse0 = createNiceMock(ServiceResponse.class);

    StackId stackId = createNiceMock(StackId.class);
    ServiceFactory serviceFactory = createNiceMock(ServiceFactory.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    KerberosHelper kerberosHeper = createStrictMock(KerberosHelper.class);

    Map<String, Service> allResponseMap = new HashMap<>();
    allResponseMap.put("KERBEROS", service0);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getHostComponents(EasyMock.anyObject())).
        andReturn(Collections.emptySet()).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();

    expect(cluster.getServices()).andReturn(allResponseMap).anyTimes();
    expect(cluster.getService("KERBEROS")).andReturn(service0);

    expect(service0.convertToResponse()).andReturn(serviceResponse0).anyTimes();

    expect(service0.getName()).andReturn("Service100").anyTimes();

    expect(serviceResponse0.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(serviceResponse0.getServiceName()).andReturn("KERBEROS").anyTimes();

    kerberosHeper.validateKDCCredentials(cluster);
    expectLastCall().andThrow(new KerberosAdminAuthenticationException("Invalid KDC administrator credentials."));

    // replay
    replay(managementController, clusters, cluster, service0, serviceResponse0,
        ambariMetaInfo, stackId, serviceFactory, kerberosHeper);

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    ResourceProvider provider = getServiceProvider(managementController);
    // set kerberos helper on provider
    Class<?> c = provider.getClass();
    Field f = c.getDeclaredField("kerberosHelper");
    f.setAccessible(true);
    f.set(provider, kerberosHeper);

    // create the request
    Predicate predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").and().
        property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals("KERBEROS").toPredicate();
    Request request = PropertyHelper.getReadRequest("ServiceInfo", "Services");
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      Assert.assertEquals("Cluster100", resource.getPropertyValue(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID));
      Assert.assertEquals("KERBEROS", resource.getPropertyValue(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID));
      Assert.assertEquals("INVALID_CREDENTIALS", resource.getPropertyValue("Services/attributes/kdc_validation_result"));
      Assert.assertEquals("Invalid KDC administrator credentials.", resource.getPropertyValue("Services/attributes/kdc_validation_failure_details"));
    }

    // verify
    verify(managementController, clusters, cluster, service0, serviceResponse0,
        ambariMetaInfo, stackId, serviceFactory, kerberosHeper);
  }

  @Test
  public void testGetResources_KerberosSpecificProperties_KDCMissingCredentials() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service0 = createNiceMock(Service.class);
    ServiceResponse serviceResponse0 = createNiceMock(ServiceResponse.class);

    StackId stackId = createNiceMock(StackId.class);
    ServiceFactory serviceFactory = createNiceMock(ServiceFactory.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    KerberosHelper kerberosHeper = createStrictMock(KerberosHelper.class);

    Map<String, Service> allResponseMap = new HashMap<>();
    allResponseMap.put("KERBEROS", service0);

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();
    expect(managementController.getHostComponents(EasyMock.anyObject())).
        andReturn(Collections.emptySet()).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();

    expect(cluster.getServices()).andReturn(allResponseMap).anyTimes();
    expect(cluster.getService("KERBEROS")).andReturn(service0);

    expect(service0.convertToResponse()).andReturn(serviceResponse0).anyTimes();

    expect(service0.getName()).andReturn("Service100").anyTimes();

    expect(serviceResponse0.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(serviceResponse0.getServiceName()).andReturn("KERBEROS").anyTimes();

    kerberosHeper.validateKDCCredentials(cluster);
    expectLastCall().andThrow(new KerberosMissingAdminCredentialsException("Missing KDC administrator credentials."));

    // replay
    replay(managementController, clusters, cluster, service0, serviceResponse0,
        ambariMetaInfo, stackId, serviceFactory, kerberosHeper);

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    ResourceProvider provider = getServiceProvider(managementController);
    // set kerberos helper on provider
    Class<?> c = provider.getClass();
    Field f = c.getDeclaredField("kerberosHelper");
    f.setAccessible(true);
    f.set(provider, kerberosHeper);

    // create the request
    Predicate predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").and().
        property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals("KERBEROS").toPredicate();
    Request request = PropertyHelper.getReadRequest("ServiceInfo", "Services");
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      Assert.assertEquals("Cluster100", resource.getPropertyValue(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID));
      Assert.assertEquals("KERBEROS", resource.getPropertyValue(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID));
      Assert.assertEquals("MISSING_CREDENTIALS", resource.getPropertyValue("Services/attributes/kdc_validation_result"));
      Assert.assertEquals("Missing KDC administrator credentials.", resource.getPropertyValue("Services/attributes/kdc_validation_failure_details"));
    }

    // verify
    verify(managementController, clusters, cluster, service0, serviceResponse0,
        ambariMetaInfo, stackId, serviceFactory, kerberosHeper);
  }

  @Test
  public void testUpdateResourcesAsAdministrator() throws Exception{
    testUpdateResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testUpdateResourcesAsClusterAdministrator() throws Exception{
    testUpdateResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testUpdateResourcesAsServiceAdministrator() throws Exception{
    testUpdateResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testUpdateResources(Authentication authentication) throws Exception{
    MaintenanceStateHelper maintenanceStateHelper = createNiceMock(MaintenanceStateHelper.class);
    RepositoryVersionDAO repositoryVersionDAO = createNiceMock(RepositoryVersionDAO.class);
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service0 = createNiceMock(Service.class);
    ServiceFactory serviceFactory = createNiceMock(ServiceFactory.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    RequestStageContainer requestStages = createNiceMock(RequestStageContainer.class);
    RequestStatusResponse requestStatusResponse = createNiceMock(RequestStatusResponse.class);
    RoleCommandOrder rco = createNiceMock(RoleCommandOrder.class);
    StackId stackId = createNiceMock(StackId.class);
    ServiceInfo serviceInfo = createNiceMock(ServiceInfo.class);

    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();

    expect(cluster.getClusterId()).andReturn(2L).anyTimes();
    expect(cluster.getService("Service102")).andReturn(service0);

    expect(service0.getDesiredState()).andReturn(State.INSTALLED).anyTimes();
    expect(service0.getServiceComponents()).andReturn(Collections.emptyMap()).anyTimes();

    expect(stackId.getStackId()).andReturn("HDP-2.5").anyTimes();
    expect(stackId.getStackName()).andReturn("HDP").anyTimes();
    expect(stackId.getStackVersion()).andReturn("2.5").anyTimes();
    expect(service0.getDesiredStackId()).andReturn(stackId).anyTimes();
    expect(service0.getName()).andReturn("Service102").anyTimes();
    expect(serviceInfo.isCredentialStoreSupported()).andReturn(true).anyTimes();
    expect(serviceInfo.isCredentialStoreEnabled()).andReturn(false).anyTimes();
    expect(ambariMetaInfo.getService("HDP", "2.5", "Service102")).andReturn(serviceInfo).anyTimes();

    Capture<Map<String, String>> requestPropertiesCapture = newCapture();
    Capture<Map<State, List<Service>>> changedServicesCapture = newCapture();
    Capture<Map<State, List<ServiceComponent>>> changedCompsCapture = newCapture();
    Capture<Map<String, Map<State, List<ServiceComponentHost>>>> changedScHostsCapture = newCapture();
    Capture<Map<String, String>> requestParametersCapture = newCapture();
    Capture<Collection<ServiceComponentHost>> ignoredScHostsCapture = newCapture();
    Capture<Cluster> clusterCapture = newCapture();

    expect(managementController.addStages((RequestStageContainer) isNull(), capture(clusterCapture), capture(requestPropertiesCapture),
        capture(requestParametersCapture), capture(changedServicesCapture), capture(changedCompsCapture),
        capture(changedScHostsCapture), capture(ignoredScHostsCapture), anyBoolean(), anyBoolean(), anyBoolean()
    )).andReturn(requestStages);
    requestStages.persist();
    expect(requestStages.getRequestStatusResponse()).andReturn(requestStatusResponse);
    expect(maintenanceStateHelper.isOperationAllowed(anyObject(Resource.Type.class), anyObject(Service.class))).andReturn(true).anyTimes();

    expect(service0.getCluster()).andReturn(cluster).anyTimes();
    expect(managementController.getRoleCommandOrder(cluster)).andReturn(rco).
    anyTimes();
    expect(rco.getTransitiveServices(eq(service0), eq(RoleCommand.START))).
    andReturn(Collections.emptySet()).anyTimes();

    // replay
    replay(managementController, clusters, cluster, rco, maintenanceStateHelper,
        repositoryVersionDAO, service0, serviceFactory, ambariMetaInfo, requestStages,
        requestStatusResponse, stackId, serviceInfo);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ServiceResourceProvider provider = getServiceProvider(managementController,
        maintenanceStateHelper, repositoryVersionDAO);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID, "STARTED");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, mapRequestProps);

    // update the service named Service102
    Predicate  predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").
        and().property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals("Service102").toPredicate();
    provider.updateResources(request, predicate);

    // verify
    verify(managementController, clusters, cluster, maintenanceStateHelper,
        service0, serviceFactory, ambariMetaInfo, requestStages, requestStatusResponse, stackId, serviceInfo);
  }

  @Test
  public void testReconfigureClientsFlagAsAdministrator() throws Exception {
    testReconfigureClientsFlag(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testReconfigureClientsFlagAsClusterAdministrator() throws Exception {
    testReconfigureClientsFlag(TestAuthenticationFactory.createAdministrator("clusterAdmin"));
  }

  @Test
  public void testReconfigureClientsFlagAsServiceAdministrator() throws Exception {
    testReconfigureClientsFlag(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testReconfigureClientsFlag(Authentication authentication) throws Exception {
    MaintenanceStateHelper maintenanceStateHelper = createNiceMock(MaintenanceStateHelper.class);
    RepositoryVersionDAO repositoryVersionDAO = createNiceMock(RepositoryVersionDAO.class);
    AmbariManagementController managementController1 = createMock(AmbariManagementController.class);
    AmbariManagementController managementController2 = createMock
        (AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service0 = createNiceMock(Service.class);
    ServiceResponse serviceResponse0 = createNiceMock(ServiceResponse.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    RequestStageContainer requestStages1 = createNiceMock(RequestStageContainer.class);
    RequestStageContainer requestStages2 = createNiceMock(RequestStageContainer.class);

    RequestStatusResponse response1 = createNiceMock(RequestStatusResponse.class);
    RequestStatusResponse response2 = createNiceMock(RequestStatusResponse
      .class);
    RoleCommandOrder rco = createNiceMock(RoleCommandOrder.class);

    StackId stackId = createNiceMock(StackId.class);
    ServiceInfo serviceInfo = createNiceMock(ServiceInfo.class);

    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");

    // set expectations
    expect(managementController1.getHostComponents(EasyMock.anyObject())).
        andReturn(Collections.emptySet()).anyTimes();
    expect(managementController2.getHostComponents(EasyMock.anyObject())).
        andReturn(Collections.emptySet()).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();

    expect(managementController1.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController1.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();

    expect(managementController2.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController2.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();

    expect(cluster.getClusterId()).andReturn(2L).anyTimes();
    expect(cluster.getService("Service102")).andReturn(service0).anyTimes();

    expect(stackId.getStackId()).andReturn("HDP-2.5").anyTimes();
    expect(stackId.getStackName()).andReturn("HDP").anyTimes();
    expect(stackId.getStackVersion()).andReturn("2.5").anyTimes();
    expect(service0.getDesiredStackId()).andReturn(stackId).anyTimes();
    expect(service0.getName()).andReturn("Service102").anyTimes();
    expect(serviceInfo.isCredentialStoreSupported()).andReturn(true).anyTimes();
    expect(serviceInfo.isCredentialStoreEnabled()).andReturn(false).anyTimes();
    expect(ambariMetaInfo.getService("HDP", "2.5", "Service102")).andReturn(serviceInfo).anyTimes();

    expect(service0.convertToResponse()).andReturn(serviceResponse0).anyTimes();
    expect(service0.getDesiredState()).andReturn(State.INSTALLED).anyTimes();
    expect(service0.getServiceComponents()).andReturn(Collections.emptyMap()).anyTimes();

    expect(serviceResponse0.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(serviceResponse0.getServiceName()).andReturn("Service102").anyTimes();

    Capture<Map<String, String>> requestPropertiesCapture = newCapture();
    Capture<Map<State, List<Service>>> changedServicesCapture = newCapture();
    Capture<Map<State, List<ServiceComponent>>> changedCompsCapture = newCapture();
    Capture<Map<String, Map<State, List<ServiceComponentHost>>>> changedScHostsCapture = newCapture();
    Capture<Map<String, String>> requestParametersCapture = newCapture();
    Capture<Collection<ServiceComponentHost>> ignoredScHostsCapture = newCapture();
    Capture<Cluster> clusterCapture = newCapture();

    expect(managementController1.addStages((RequestStageContainer) isNull(), capture(clusterCapture), capture(requestPropertiesCapture),
        capture(requestParametersCapture), capture(changedServicesCapture), capture(changedCompsCapture),
        capture(changedScHostsCapture), capture(ignoredScHostsCapture), anyBoolean(), anyBoolean(), anyBoolean()
    )).andReturn(requestStages1);

    expect(managementController2.addStages((RequestStageContainer) isNull(), capture(clusterCapture), capture(requestPropertiesCapture),
        capture(requestParametersCapture), capture(changedServicesCapture), capture(changedCompsCapture),
        capture(changedScHostsCapture), capture(ignoredScHostsCapture), anyBoolean(), anyBoolean(), anyBoolean()
    )).andReturn(requestStages2);

    requestStages1.persist();
    expect(requestStages1.getRequestStatusResponse()).andReturn(response1);

    requestStages2.persist();
    expect(requestStages2.getRequestStatusResponse()).andReturn(response2);

    expect(maintenanceStateHelper.isOperationAllowed(anyObject(Resource.Type.class), anyObject(Service.class))).andReturn(true).anyTimes();

    expect(service0.getCluster()).andReturn(cluster).anyTimes();
    expect(managementController1.getRoleCommandOrder(cluster)).andReturn(rco).
    anyTimes();
    expect(managementController2.getRoleCommandOrder(cluster)).andReturn(rco).
    anyTimes();
    expect(rco.getTransitiveServices(eq(service0), eq(RoleCommand.START))).
    andReturn(Collections.emptySet()).anyTimes();

    // replay
    replay(managementController1, response1, managementController2, requestStages1, requestStages2,
        response2, clusters, cluster, service0, serviceResponse0, ambariMetaInfo, rco,
        maintenanceStateHelper, repositoryVersionDAO, stackId, serviceInfo);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ServiceResourceProvider provider1 = getServiceProvider(managementController1,
        maintenanceStateHelper, repositoryVersionDAO);

    ServiceResourceProvider provider2 = getServiceProvider(managementController2,
        maintenanceStateHelper, repositoryVersionDAO);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID,
      "STARTED");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, mapRequestProps);

    // update the service named Service102
    Predicate  predicate1 = new PredicateBuilder().property
      (ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").
      and().property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).
      equals("Service102").and().property("params/reconfigure_client").
      equals("true").toPredicate();

    Predicate  predicate2 = new PredicateBuilder().property
      (ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").
      and().property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).
      equals("Service102").and().property("params/reconfigure_client").equals
      ("false").toPredicate();

    provider1.updateResources(request, predicate1);
    provider2.updateResources(request, predicate2);

    // verify
    verify(managementController1, response1, managementController2, requestStages1, requestStages2, response2,
        clusters, cluster, service0, serviceResponse0, ambariMetaInfo, maintenanceStateHelper, stackId, serviceInfo);
  }

  @Test
  public void testDeleteResourcesAsAdministrator() throws Exception{
    testDeleteResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testDeleteResourcesAsClusterAdministrator() throws Exception{
    testDeleteResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResourcesAsServiceAdministrator() throws Exception{
    testDeleteResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testDeleteResources(Authentication authentication) throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);

    String serviceName = "Service100";

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(cluster.getClusterId()).andReturn(2L).anyTimes();
    expect(cluster.getService(serviceName)).andReturn(service).anyTimes();
    expect(service.getDesiredState()).andReturn(State.INSTALLED).anyTimes();
    expect(service.getName()).andReturn(serviceName).anyTimes();
    expect(service.getServiceComponents()).andReturn(new HashMap<>());
    expect(service.getCluster()).andReturn(cluster);
    cluster.deleteService(eq(serviceName), anyObject(DeleteHostComponentStatusMetaData.class));

    // replay
    replay(managementController, clusters, cluster, service);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = getServiceProvider(managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider)provider).addObserver(observer);

    // delete the service named Service100
    Predicate  predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").and()
        .property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals(serviceName).toPredicate();
    provider.deleteResources(new RequestImpl(null, null, null, null), predicate);


    ResourceProviderEvent lastEvent = observer.getLastEvent();
    Assert.assertNotNull(lastEvent);
    Assert.assertEquals(Resource.Type.Service, lastEvent.getResourceType());
    Assert.assertEquals(ResourceProviderEvent.Type.Delete, lastEvent.getType());
    Assert.assertEquals(predicate, lastEvent.getPredicate());
    Assert.assertNull(lastEvent.getRequest());

    // verify
    verify(managementController, clusters, cluster, service);
  }

  @Test
  public void testDeleteResourcesBadServiceState() throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);

    String serviceName = "Service100";

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(cluster.getClusterId()).andReturn(2L).anyTimes();
    expect(cluster.getService(serviceName)).andReturn(service).anyTimes();
    expect(service.getDesiredState()).andReturn(State.STARTED).anyTimes();
    expect(service.getName()).andReturn(serviceName).anyTimes();
    expect(service.getServiceComponents()).andReturn(new HashMap<>());
    expect(service.getCluster()).andReturn(cluster);
    cluster.deleteService(eq(serviceName), anyObject(DeleteHostComponentStatusMetaData.class));

    // replay
    replay(managementController, clusters, cluster, service);

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    ResourceProvider provider = getServiceProvider(managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider)provider).addObserver(observer);

    // delete the service named Service100
    Predicate  predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").and()
        .property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals(serviceName).toPredicate();
    provider.deleteResources(new RequestImpl(null, null, null, null), predicate);


    ResourceProviderEvent lastEvent = observer.getLastEvent();
    Assert.assertNotNull(lastEvent);
    Assert.assertEquals(Resource.Type.Service, lastEvent.getResourceType());
    Assert.assertEquals(ResourceProviderEvent.Type.Delete, lastEvent.getType());
    Assert.assertEquals(predicate, lastEvent.getPredicate());
    Assert.assertNull(lastEvent.getRequest());

    // verify
    verify(managementController, clusters, cluster, service);
  }

  @Test
  public void testDeleteResourcesBadComponentState() throws Exception{
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);
    ServiceComponent sc = createNiceMock(ServiceComponent.class);
    Map<String, ServiceComponent> scMap = new HashMap<>();
    scMap.put("Component100", sc);
    State componentState = State.STARTED;
    ServiceComponentHost sch = createNiceMock(ServiceComponentHost.class);
    Map<String, ServiceComponentHost> schMap = new HashMap<>();
    schMap.put("Host1", sch);
    State schState = State.STARTED;

    String serviceName = "Service100";

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();
    expect(cluster.getService(serviceName)).andReturn(service).anyTimes();
    expect(service.getDesiredState()).andReturn(State.INSTALLED).anyTimes();
    expect(service.getName()).andReturn(serviceName).anyTimes();
    expect(service.getServiceComponents()).andReturn(scMap);
    expect(sc.getDesiredState()).andReturn(componentState).anyTimes();
    expect(sc.getName()).andReturn("Component100").anyTimes();
    expect(sc.canBeRemoved()).andReturn(componentState.isRemovableState()).anyTimes();
    expect(sc.getServiceComponentHosts()).andReturn(schMap).anyTimes();
    expect(sch.getDesiredState()).andReturn(schState).anyTimes();
    expect(sch.canBeRemoved()).andReturn(schState.isRemovableState()).anyTimes();

    // replay
    replay(managementController, clusters, cluster, service, sc, sch);

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    ResourceProvider provider = getServiceProvider(managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider)provider).addObserver(observer);

    // delete the service named Service100
    Predicate  predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").and()
        .property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals(serviceName).toPredicate();

    try {
      provider.deleteResources(new RequestImpl(null, null, null, null), predicate);
      Assert.fail("Expected exception deleting a service in a non-removable state.");
    } catch (SystemException e) {
      // expected
    }
  }

  /*
  If the host components of a service are in a removable state, the service should be removable even if it's state is non-removable
 */
  @Test
  public void testDeleteResourcesStoppedHostComponentState() throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service = createNiceMock(Service.class);

    //
    // Data structure for holding ServiceComponent information
    //
    class TestComponent {
      public String Name;
      public ServiceComponent Component;
      public State DesiredState;

      public TestComponent(String name, ServiceComponent component, State desiredState) {
        Name = name;
        Component = component;
        DesiredState = desiredState;
      }
    }

    //
    // Set up three components in STARTED state.
    //
    TestComponent component1 = new TestComponent("Component100", createNiceMock(ServiceComponent.class), State.STARTED);
    TestComponent component2 = new TestComponent("Component101", createNiceMock(ServiceComponent.class), State.STARTED);
    TestComponent component3 = new TestComponent("Component102", createNiceMock(ServiceComponent.class), State.STARTED);

    Map<String, ServiceComponent> scMap = new HashMap<>();
    scMap.put(component1.Name, component1.Component);
    scMap.put(component2.Name, component2.Component);
    scMap.put(component3.Name, component3.Component);

    Map<String, ServiceComponentHost> schMap1 = new HashMap<>();
    ServiceComponentHost sch1 = createNiceMock(ServiceComponentHost.class);
    schMap1.put("Host1", sch1);

    Map<String, ServiceComponentHost> schMap2 = new HashMap<>();
    ServiceComponentHost sch2 = createNiceMock(ServiceComponentHost.class);
    schMap2.put("Host2", sch2);

    Map<String, ServiceComponentHost> schMap3 = new HashMap<>();
    ServiceComponentHost sch3 = createNiceMock(ServiceComponentHost.class);
    schMap3.put("Host3", sch3);

    String clusterName = "Cluster100";
    String serviceName = "Service100";

    // set expectations
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster(clusterName)).andReturn(cluster).anyTimes();
    expect(cluster.getService(serviceName)).andReturn(service).anyTimes();
    expect(service.getDesiredState()).andReturn(State.STARTED).anyTimes();  // Service is in a non-removable state
    expect(service.getName()).andReturn(serviceName).anyTimes();
    expect(service.getServiceComponents()).andReturn(scMap).anyTimes();
    expect(component1.Component.getDesiredState()).andReturn(component1.DesiredState).anyTimes();
    expect(component2.Component.getDesiredState()).andReturn(component2.DesiredState).anyTimes();
    expect(component3.Component.getDesiredState()).andReturn(component3.DesiredState).anyTimes();
    expect(component1.Component.canBeRemoved()).andReturn(component1.DesiredState.isRemovableState()).anyTimes();
    expect(component2.Component.canBeRemoved()).andReturn(component2.DesiredState.isRemovableState()).anyTimes();
    expect(component3.Component.canBeRemoved()).andReturn(component3.DesiredState.isRemovableState()).anyTimes();
    expect(component1.Component.getServiceComponentHosts()).andReturn(schMap1).anyTimes();
    expect(component2.Component.getServiceComponentHosts()).andReturn(schMap2).anyTimes();
    expect(component3.Component.getServiceComponentHosts()).andReturn(schMap3).anyTimes();

    // Put the SCH in INSTALLED state so that the service can be deleted,
    // no matter what state the service component is in.
    State sch1State = State.INSTALLED;
    expect(sch1.getDesiredState()).andReturn(sch1State).anyTimes();
    expect(sch1.canBeRemoved()).andReturn(sch1State.isRemovableState()).anyTimes();

    State sch2State = State.INSTALLED;
    expect(sch2.getDesiredState()).andReturn(sch2State).anyTimes();
    expect(sch2.canBeRemoved()).andReturn(sch2State.isRemovableState()).anyTimes();

    State sch3State = State.INSTALLED;
    expect(sch3.getDesiredState()).andReturn(sch3State).anyTimes();
    expect(sch3.canBeRemoved()).andReturn(sch3State.isRemovableState()).anyTimes();

    expect(service.getCluster()).andReturn(cluster);
    cluster.deleteService(eq(serviceName), anyObject(DeleteHostComponentStatusMetaData.class));

    // replay
    replay(managementController, clusters, cluster, service,
            component1.Component, component2.Component, component3.Component, sch1, sch2, sch3);

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    ResourceProvider provider = getServiceProvider(managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider)provider).addObserver(observer);

    // delete the service named Service100
    Predicate  predicate = new PredicateBuilder().property(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID).equals(clusterName).and()
            .property(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID).equals(serviceName).toPredicate();
    provider.deleteResources(new RequestImpl(null, null, null, null), predicate);


    ResourceProviderEvent lastEvent = observer.getLastEvent();
    Assert.assertNotNull(lastEvent);
    Assert.assertEquals(Resource.Type.Service, lastEvent.getResourceType());
    Assert.assertEquals(ResourceProviderEvent.Type.Delete, lastEvent.getType());
    Assert.assertEquals(predicate, lastEvent.getPredicate());
    Assert.assertNull(lastEvent.getRequest());

    // verify
    verify(managementController, clusters, cluster, service,
            component1.Component, component2.Component, component3.Component, sch1, sch2, sch3);
  }

  @Test
  public void testCheckPropertyIds() throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    MaintenanceStateHelper maintenanceStateHelperMock = createNiceMock(MaintenanceStateHelper.class);
    RepositoryVersionDAO repositoryVersionDAO = createNiceMock(RepositoryVersionDAO.class);
    replay(maintenanceStateHelperMock, repositoryVersionDAO);

    AbstractResourceProvider provider = new ServiceResourceProvider(managementController,
        maintenanceStateHelperMock, repositoryVersionDAO);

    Set<String> unsupported = provider.checkPropertyIds(
        Collections.singleton(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID));

    Assert.assertTrue(unsupported.isEmpty());

    // note that key is not in the set of known property ids.  We allow it if its parent is a known property.
    // this allows for Map type properties where we want to treat the entries as individual properties
    String subKey = PropertyHelper.getPropertyId(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID, "key");
    unsupported = provider.checkPropertyIds(Collections.singleton(subKey));
    Assert.assertTrue(unsupported.isEmpty());

    unsupported = provider.checkPropertyIds(Collections.singleton("bar"));
    Assert.assertEquals(1, unsupported.size());
    Assert.assertTrue(unsupported.contains("bar"));

    for (String propertyId : provider.getPKPropertyIds()) {
      unsupported = provider.checkPropertyIds(Collections.singleton(propertyId));
      Assert.assertTrue(unsupported.isEmpty());
    }
  }

  @Test
  public void testCreateWithNoRepositoryId() throws Exception {
    AmbariManagementController managementController = createNiceMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service1 = createNiceMock(Service.class);
    Service service2 = createNiceMock(Service.class);

    RepositoryVersionEntity repoVersion = createNiceMock(RepositoryVersionEntity.class);
    expect(repoVersion.getId()).andReturn(500L).anyTimes();
    expect(service1.getDesiredRepositoryVersion()).andReturn(repoVersion).atLeastOnce();

    StackId stackId = new StackId("HDP-2.5");
    ServiceFactory serviceFactory = createNiceMock(ServiceFactory.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    ServiceInfo serviceInfo = createNiceMock(ServiceInfo.class);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();

    expect(cluster.addService(eq("Service200"), EasyMock.anyObject(RepositoryVersionEntity.class))).andReturn(service2);

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();

    expect(cluster.getServices()).andReturn(
        ImmutableMap.<String, Service>builder()
          .put("Service100", service1).build()).atLeastOnce();

    expect(cluster.getDesiredStackVersion()).andReturn(stackId).anyTimes();
    expect(cluster.getClusterId()).andReturn(2L).anyTimes();

    expect(ambariMetaInfo.isValidService( (String) anyObject(), (String) anyObject(), (String) anyObject())).andReturn(true);
    expect(ambariMetaInfo.getService((String)anyObject(), (String)anyObject(), (String)anyObject())).andReturn(serviceInfo).anyTimes();

    // replay
    replay(managementController, clusters, cluster, service1, service2,
        ambariMetaInfo, serviceFactory, serviceInfo, repoVersion);

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    Capture<Long> pkCapture = Capture.newInstance();
    ResourceProvider provider = getServiceProvider(managementController, true, pkCapture);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    // Service 1: create a map of properties for the request
    Map<String, Object> properties = new LinkedHashMap<>();

    // add properties to the request map
    properties.put(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID, "Service200");
    properties.put(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID, "INIT");
    properties.put(ServiceResourceProvider.SERVICE_DESIRED_STACK_PROPERTY_ID, "HDP-1.1");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    // verify
    verify(managementController, clusters, cluster, service1, service2,
        ambariMetaInfo, serviceFactory, serviceInfo);

    Assert.assertTrue(pkCapture.hasCaptured());
    Assert.assertEquals(Long.valueOf(500L), pkCapture.getValue());
  }

  @Test
  public void testCreateWithNoRepositoryIdAndPatch() throws Exception {
    AmbariManagementController managementController = createNiceMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service1 = createNiceMock(Service.class);
    Service service2 = createNiceMock(Service.class);

    RepositoryVersionEntity repoVersion = createNiceMock(RepositoryVersionEntity.class);
    expect(repoVersion.getId()).andReturn(500L).anyTimes();
    expect(repoVersion.getParentId()).andReturn(600L).anyTimes();
    expect(repoVersion.getType()).andReturn(RepositoryType.PATCH).anyTimes();
    expect(service1.getDesiredRepositoryVersion()).andReturn(repoVersion).atLeastOnce();

    StackId stackId = new StackId("HDP-2.5");
    ServiceFactory serviceFactory = createNiceMock(ServiceFactory.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    ServiceInfo serviceInfo = createNiceMock(ServiceInfo.class);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();

    expect(cluster.addService(eq("Service200"), EasyMock.anyObject(RepositoryVersionEntity.class))).andReturn(service2);

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();

    expect(cluster.getServices()).andReturn(
        ImmutableMap.<String, Service>builder()
          .put("Service100", service1).build()).atLeastOnce();

    expect(cluster.getDesiredStackVersion()).andReturn(stackId).anyTimes();
    expect(cluster.getClusterId()).andReturn(2L).anyTimes();

    expect(ambariMetaInfo.isValidService( (String) anyObject(), (String) anyObject(), (String) anyObject())).andReturn(true);
    expect(ambariMetaInfo.getService((String)anyObject(), (String)anyObject(), (String)anyObject())).andReturn(serviceInfo).anyTimes();

    // replay
    replay(managementController, clusters, cluster, service1, service2,
        ambariMetaInfo, serviceFactory, serviceInfo, repoVersion);

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    Capture<Long> pkCapture = Capture.newInstance();
    ResourceProvider provider = getServiceProvider(managementController, true, pkCapture);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    // Service 1: create a map of properties for the request
    Map<String, Object> properties = new LinkedHashMap<>();

    // add properties to the request map
    properties.put(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID, "Service200");
    properties.put(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID, "INIT");
    properties.put(ServiceResourceProvider.SERVICE_DESIRED_STACK_PROPERTY_ID, "HDP-1.1");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    // verify
    verify(managementController, clusters, cluster, service1, service2,
        ambariMetaInfo, serviceFactory, serviceInfo);

    Assert.assertTrue(pkCapture.hasCaptured());
    Assert.assertEquals(Long.valueOf(600L), pkCapture.getValue());
  }

  @Test
  public void testCreateWithNoRepositoryIdAndMultiBase() throws Exception {
    AmbariManagementController managementController = createNiceMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Service service1 = createNiceMock(Service.class);
    Service service2 = createNiceMock(Service.class);
    Service service3 = createNiceMock(Service.class);

    RepositoryVersionEntity repoVersion1 = createNiceMock(RepositoryVersionEntity.class);
    expect(repoVersion1.getId()).andReturn(500L).anyTimes();
    expect(service1.getDesiredRepositoryVersion()).andReturn(repoVersion1).atLeastOnce();

    RepositoryVersionEntity repoVersion2 = createNiceMock(RepositoryVersionEntity.class);
    expect(repoVersion2.getId()).andReturn(600L).anyTimes();
    expect(service2.getDesiredRepositoryVersion()).andReturn(repoVersion2).atLeastOnce();

    StackId stackId = new StackId("HDP-2.5");
    ServiceFactory serviceFactory = createNiceMock(ServiceFactory.class);
    AmbariMetaInfo ambariMetaInfo = createNiceMock(AmbariMetaInfo.class);
    ServiceInfo serviceInfo = createNiceMock(ServiceInfo.class);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(ambariMetaInfo).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();

    expect(cluster.getServices()).andReturn(
        ImmutableMap.<String, Service>builder()
          .put("Service100", service1)
          .put("Service200", service2).build()).atLeastOnce();

    expect(cluster.getDesiredStackVersion()).andReturn(stackId).anyTimes();
    expect(cluster.getClusterId()).andReturn(2L).anyTimes();

    // replay
    replay(managementController, clusters, cluster, service1, service2, service3,
        ambariMetaInfo, serviceFactory, serviceInfo, repoVersion1, repoVersion2);

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    Capture<Long> pkCapture = Capture.newInstance();
    ResourceProvider provider = getServiceProvider(managementController, true, pkCapture);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    // Service 1: create a map of properties for the request
    Map<String, Object> properties = new LinkedHashMap<>();

    // add properties to the request map
    properties.put(ServiceResourceProvider.SERVICE_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(ServiceResourceProvider.SERVICE_SERVICE_NAME_PROPERTY_ID, "Service200");
    properties.put(ServiceResourceProvider.SERVICE_SERVICE_STATE_PROPERTY_ID, "INIT");
    properties.put(ServiceResourceProvider.SERVICE_DESIRED_STACK_PROPERTY_ID, "HDP-1.1");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    try {
      provider.createResources(request);
      Assert.fail("Expected an exception when more than one base version was found");
    } catch (IllegalArgumentException expected) {
      // !!! expected
    }

    // verify
    verify(managementController, clusters, cluster, service1, service2, service3,
        ambariMetaInfo, serviceFactory, serviceInfo);

  }

  private static ServiceResourceProvider getServiceProvider(AmbariManagementController managementController,
      boolean mockFindByStack, Capture<Long> pkCapture) throws AmbariException, NoSuchFieldException, IllegalAccessException {
    MaintenanceStateHelper maintenanceStateHelperMock = createNiceMock(MaintenanceStateHelper.class);
    RepositoryVersionDAO repositoryVersionDAO = createNiceMock(RepositoryVersionDAO.class);
    expect(maintenanceStateHelperMock.isOperationAllowed(anyObject(Resource.Type.class), anyObject(Service.class))).andReturn(true).anyTimes();
    expect(maintenanceStateHelperMock.isOperationAllowed(anyObject(Resource.Type.class), anyObject(ServiceComponentHost.class))).andReturn(true).anyTimes();

    if (mockFindByStack) {
      RepositoryVersionEntity repositoryVersion = createNiceMock(RepositoryVersionEntity.class);

      if (null != pkCapture) {
        expect(repositoryVersionDAO.findByPK(capture(pkCapture))).andReturn(repositoryVersion).atLeastOnce();
      } else {
        expect(repositoryVersionDAO.findByPK(EasyMock.anyLong())).andReturn(repositoryVersion).atLeastOnce();
      }

      expect(repositoryVersion.getStackId()).andReturn(new StackId("HDP-2.2")).anyTimes();
      replay(repositoryVersion);
    }

    replay(maintenanceStateHelperMock, repositoryVersionDAO);
    return getServiceProvider(managementController, maintenanceStateHelperMock, repositoryVersionDAO);
  }

  /**
   * This factory method creates default MaintenanceStateHelper mock.
   * It's useful in most cases (when we don't care about Maintenance State)
   */
  public static ServiceResourceProvider getServiceProvider(AmbariManagementController managementController)
      throws AmbariException, NoSuchFieldException, IllegalAccessException {
    return getServiceProvider(managementController, false, null);
  }

  /**
   * This factory method allows to define custom MaintenanceStateHelper mock.
   */
  public static ServiceResourceProvider getServiceProvider(
      AmbariManagementController managementController,
      MaintenanceStateHelper maintenanceStateHelper, RepositoryVersionDAO repositoryVersionDAO)
      throws NoSuchFieldException, IllegalAccessException {
    Resource.Type type = Resource.Type.Service;
    ServiceResourceProvider serviceResourceProvider =
        new ServiceResourceProvider(managementController, maintenanceStateHelper, repositoryVersionDAO);

    Field STOMPComponentsDeleteHandlerField = ServiceResourceProvider.class.getDeclaredField("STOMPComponentsDeleteHandler");
    STOMPComponentsDeleteHandlerField.setAccessible(true);
    STOMPComponentsDeleteHandler STOMPComponentsDeleteHandler = createNiceMock(STOMPComponentsDeleteHandler.class);
    STOMPComponentsDeleteHandlerField.set(serviceResourceProvider, STOMPComponentsDeleteHandler);
    replay(STOMPComponentsDeleteHandler);
    return serviceResourceProvider;
  }

  public static void createServices(AmbariManagementController controller,
      RepositoryVersionDAO repositoryVersionDAO, Set<ServiceRequest> requests)
      throws AmbariException, AuthorizationException, NoSuchFieldException, IllegalAccessException {
    MaintenanceStateHelper maintenanceStateHelperMock = createNiceMock(MaintenanceStateHelper.class);
    ServiceResourceProvider provider = getServiceProvider(controller, maintenanceStateHelperMock, repositoryVersionDAO);
    provider.createServices(requests);
  }

  public static Set<ServiceResponse> getServices(AmbariManagementController controller,
                                                 Set<ServiceRequest> requests)
      throws AmbariException, NoSuchFieldException, IllegalAccessException {
    ServiceResourceProvider provider = getServiceProvider(controller);
    return provider.getServices(requests);
  }

  public static RequestStatusResponse updateServices(AmbariManagementController controller,
                                                     Set<ServiceRequest> requests,
                                                     Map<String, String> requestProperties, boolean runSmokeTest,
                                                     boolean reconfigureClients)
      throws AmbariException, AuthorizationException, NoSuchFieldException, IllegalAccessException {
    return updateServices(controller, requests, requestProperties, runSmokeTest, reconfigureClients, null);
  }

  /**
   * Allows to set maintenanceStateHelper. For use when there is anything to test
   * with maintenance mode.
   */
  public static RequestStatusResponse updateServices(AmbariManagementController controller,
                                                     Set<ServiceRequest> requests,
                                                     Map<String, String> requestProperties, boolean runSmokeTest,
                                                     boolean reconfigureClients,
                                                     MaintenanceStateHelper maintenanceStateHelper)
      throws AmbariException, AuthorizationException, NoSuchFieldException, IllegalAccessException {
    ServiceResourceProvider provider;
    if (maintenanceStateHelper != null) {
      provider = getServiceProvider(controller, maintenanceStateHelper, null);
    } else {
      provider = getServiceProvider(controller);
    }

    RequestStageContainer request = provider.updateServices(null, requests, requestProperties, runSmokeTest, reconfigureClients, true);
    request.persist();
    return request.getRequestStatusResponse();
  }

  public static RequestStatusResponse deleteServices(AmbariManagementController controller, Set<ServiceRequest> requests)
      throws AmbariException, AuthorizationException, NoSuchFieldException, IllegalAccessException {
    ServiceResourceProvider provider = getServiceProvider(controller);
    return provider.deleteServices(requests);
  }

}
