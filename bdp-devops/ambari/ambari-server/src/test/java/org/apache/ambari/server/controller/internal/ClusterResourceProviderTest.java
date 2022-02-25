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
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ClusterRequest;
import org.apache.ambari.server.controller.ClusterResponse;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.BlueprintFactory;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.apache.ambari.server.topology.SecurityConfiguration;
import org.apache.ambari.server.topology.SecurityConfigurationFactory;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.topology.TopologyRequestFactory;
import org.apache.ambari.server.utils.RetryHelper;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.eclipse.persistence.exceptions.DatabaseException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.gson.Gson;


/**
 * ClusterResourceProvider tests.
 */
public class ClusterResourceProviderTest {
  private static final String CLUSTER_NAME = "cluster_name";
  private static final String BLUEPRINT_NAME = "blueprint_name";

  private ClusterResourceProvider provider;

  private static final AmbariManagementController controller = createNiceMock(AmbariManagementController.class);
  private static final Request request = createNiceMock(Request.class);
  private static final TopologyManager topologyManager = createStrictMock(TopologyManager.class);
  private static final TopologyRequestFactory topologyFactory = createStrictMock(TopologyRequestFactory.class);
  private final static SecurityConfigurationFactory securityFactory = createMock(SecurityConfigurationFactory.class);
  private static final ProvisionClusterRequest topologyRequest = createNiceMock(ProvisionClusterRequest.class);
  private static final BlueprintFactory blueprintFactory = createStrictMock(BlueprintFactory.class);
  private static final Blueprint blueprint = createNiceMock(Blueprint.class);
  private static final RequestStatusResponse requestStatusResponse = createNiceMock(RequestStatusResponse.class);
  private static final Gson gson = new Gson();

  @Before
  public void setup() throws Exception{
    ClusterResourceProvider.init(topologyManager, topologyFactory, securityFactory, gson);
    ProvisionClusterRequest.init(blueprintFactory);
    provider = new ClusterResourceProvider(controller);

    expect(blueprintFactory.getBlueprint(BLUEPRINT_NAME)).andReturn(blueprint).anyTimes();
    expect(securityFactory.createSecurityConfigurationFromRequest(null, false)).andReturn(null).anyTimes();
  }

  @After
  public void tearDown() {
    reset(request, topologyManager, topologyFactory, topologyRequest, blueprintFactory, securityFactory,
      requestStatusResponse, blueprint);

    // Clear the security context
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  private void replayAll() {
    replay(request, topologyManager, topologyFactory, topologyRequest, blueprintFactory, securityFactory,
      requestStatusResponse, blueprint);
  }

  private void verifyAll() {
    verify(request, topologyManager, topologyFactory, topologyRequest, blueprintFactory, securityFactory,
      requestStatusResponse, blueprint);
  }

  @Test
  public void testCreateResource_blueprint_asAdministrator() throws Exception {
    testCreateResource_blueprint(TestAuthenticationFactory.createAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResource_blueprint__NonAdministrator() throws Exception {
    testCreateResource_blueprint(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testCreateResource_blueprint_With_ProvisionAction() throws Exception {
    Set<Map<String, Object>> requestProperties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);
    Map<String, Object> properties = requestProperties.iterator().next();
    properties.put(BaseClusterRequest.PROVISION_ACTION_PROPERTY, "INSTALL_ONLY");
    Map<String, String> requestInfoProperties = new HashMap<>();
    requestInfoProperties.put(Request.REQUEST_INFO_BODY_PROPERTY, "{}");

    // set expectations
    expect(request.getProperties()).andReturn(requestProperties).anyTimes();
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties).anyTimes();

    expect(securityFactory.createSecurityConfigurationFromRequest(EasyMock.anyObject(), anyBoolean())).andReturn(null)
      .once();
    expect(topologyFactory.createProvisionClusterRequest(properties, null)).andReturn(topologyRequest).once();
    expect(topologyManager.provisionCluster(topologyRequest)).andReturn(requestStatusResponse).once();
    expect(requestStatusResponse.getRequestId()).andReturn(5150L).anyTimes();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    RequestStatus requestStatus = provider.createResources(request);
    assertEquals(5150L, requestStatus.getRequestResource().getPropertyValue(PropertyHelper.getPropertyId("Requests", "id")));
    assertEquals(Resource.Type.Request, requestStatus.getRequestResource().getType());
    assertEquals("Accepted", requestStatus.getRequestResource().getPropertyValue(PropertyHelper.getPropertyId("Requests", "status")));

    verifyAll();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateResource_blueprint_withInvalidSecurityConfiguration() throws Exception {
    Set<Map<String, Object>> requestProperties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);
    Map<String, Object> properties = requestProperties.iterator().next();
    Map<String, String> requestInfoProperties = new HashMap<>();
    requestInfoProperties.put(Request.REQUEST_INFO_BODY_PROPERTY, "{\"security\" : {\n\"type\" : \"NONE\"," +
      "\n\"kerberos_descriptor_reference\" : " + "\"testRef\"\n}}");
    SecurityConfiguration blueprintSecurityConfiguration = SecurityConfiguration.withReference("testRef");
    SecurityConfiguration securityConfiguration = SecurityConfiguration.NONE;

    // set expectations
    expect(request.getProperties()).andReturn(requestProperties).anyTimes();
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties).anyTimes();

    expect(securityFactory.createSecurityConfigurationFromRequest(EasyMock.anyObject(), anyBoolean())).andReturn
      (securityConfiguration).once();
    expect(topologyFactory.createProvisionClusterRequest(properties, securityConfiguration)).andReturn(topologyRequest).once();
    expect(topologyRequest.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(blueprint.getSecurity()).andReturn(blueprintSecurityConfiguration).anyTimes();
    expect(requestStatusResponse.getRequestId()).andReturn(5150L).anyTimes();

    replayAll();
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());
    RequestStatus requestStatus = provider.createResources(request);
  }

  @Test
  public void testCreateResource_blueprint_withSecurityConfiguration() throws Exception {
    Set<Map<String, Object>> requestProperties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);
    Map<String, Object> properties = requestProperties.iterator().next();
    SecurityConfiguration securityConfiguration = SecurityConfiguration.withReference("testRef");

    Map<String, String> requestInfoProperties = new HashMap<>();
    requestInfoProperties.put(Request.REQUEST_INFO_BODY_PROPERTY, "{\"security\" : {\n\"type\" : \"KERBEROS\",\n\"kerberos_descriptor_reference\" : " +
      "\"testRef\"\n}}");

        // set expectations
    expect(request.getProperties()).andReturn(requestProperties).anyTimes();
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties).anyTimes();

    expect(topologyFactory.createProvisionClusterRequest(properties, securityConfiguration)).andReturn(topologyRequest).once();
    expect(securityFactory.createSecurityConfigurationFromRequest(EasyMock.anyObject(), anyBoolean())).andReturn
      (securityConfiguration).once();
    expect(topologyManager.provisionCluster(topologyRequest)).andReturn(requestStatusResponse).once();
    expect(requestStatusResponse.getRequestId()).andReturn(5150L).anyTimes();

    replayAll();
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());
    RequestStatus requestStatus = provider.createResources(request);
    assertEquals(5150L, requestStatus.getRequestResource().getPropertyValue(PropertyHelper.getPropertyId("Requests", "id")));
    assertEquals(Resource.Type.Request, requestStatus.getRequestResource().getType());
    assertEquals("Accepted", requestStatus.getRequestResource().getPropertyValue(PropertyHelper.getPropertyId("Requests", "status")));

    verifyAll();
  }

  @Test(expected=IllegalArgumentException.class)
  public void testCreateResource_blueprint__InvalidRequest() throws Exception {
    Set<Map<String, Object>> requestProperties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);
    Map<String, Object> properties = requestProperties.iterator().next();

    // set expectations
    expect(request.getProperties()).andReturn(requestProperties).anyTimes();
    // throw exception from topology request factory an assert that the correct exception is thrown from resource provider
    expect(topologyFactory.createProvisionClusterRequest(properties, null)).andThrow(new InvalidTopologyException
      ("test"));

    replayAll();
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());
    provider.createResources(request);
  }

  @Test
  public void testCreateResourcesAsAdministrator() throws Exception{
    testCreateResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesAsNonAdministrator() throws Exception{
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testCreateResourcesWithRetry() throws Exception {
    Clusters clusters = createMock(Clusters.class);
    EasyMock.replay(clusters);

    RetryHelper.init(clusters, 3);
    Resource.Type type = Resource.Type.Cluster;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    managementController.createCluster(
        AbstractResourceProviderTest.Matcher.getClusterRequest(null, "Cluster100", "HDP-0.1", null));
    expectLastCall().andThrow(new DatabaseException("test"){}).once().andVoid().atLeastOnce();

    // replay
    replay(managementController, response);

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider)provider).addObserver(observer);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    // Cluster 1: create a map of properties for the request
    Map<String, Object> properties = new LinkedHashMap<>();

    // add the cluster name to the properties map
    properties.put(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID, "Cluster100");

    // add the version to the properties map
    properties.put(ClusterResourceProvider.CLUSTER_VERSION_PROPERTY_ID, "HDP-0.1");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    ResourceProviderEvent lastEvent = observer.getLastEvent();
    Assert.assertNotNull(lastEvent);
    Assert.assertEquals(Resource.Type.Cluster, lastEvent.getResourceType());
    Assert.assertEquals(ResourceProviderEvent.Type.Create, lastEvent.getType());
    Assert.assertEquals(request, lastEvent.getRequest());
    Assert.assertNull(lastEvent.getPredicate());

    // verify
    verify(managementController, response);

    RetryHelper.init(clusters, 0);

  }

  @Test
  public void testGetResourcesAsAdministrator() throws Exception{
    testGetResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testGetResourcesAsNonAdministrator() throws Exception{
    testGetResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  public void testGetResources(Authentication authentication) throws Exception{
    Resource.Type type = Resource.Type.Cluster;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);

    Set<ClusterResponse> allResponse = new HashSet<>();
    allResponse.add(new ClusterResponse(100L, "Cluster100", State.INSTALLED, SecurityType.NONE, null, 0, null, null));
    allResponse.add(new ClusterResponse(101L, "Cluster101", State.INSTALLED, SecurityType.NONE, null, 0, null, null));
    allResponse.add(new ClusterResponse(102L, "Cluster102", State.INSTALLED, SecurityType.NONE, null, 0, null, null));
    allResponse.add(new ClusterResponse(103L, "Cluster103", State.INSTALLED, SecurityType.NONE, null, 0, null, null));
    allResponse.add(new ClusterResponse(104L, "Cluster104", State.INSTALLED, SecurityType.NONE, null, 0, null, null));

    Set<ClusterResponse> nameResponse = new HashSet<>();
    nameResponse.add(new ClusterResponse(102L, "Cluster102", State.INSTALLED, SecurityType.NONE, null, 0, null, null));

    Set<ClusterResponse> idResponse = new HashSet<>();
    idResponse.add(new ClusterResponse(103L, "Cluster103", State.INSTALLED, SecurityType.NONE, null, 0, null, null));

    // set expectations
    Capture<Set<ClusterRequest>> captureClusterRequests = EasyMock.newCapture();

    expect(managementController.getClusters(capture(captureClusterRequests))).andReturn(allResponse).once();
    expect(managementController.getClusters(capture(captureClusterRequests))).andReturn(nameResponse).once();
    expect(managementController.getClusters(capture(captureClusterRequests))).andReturn(idResponse).once();

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();

    // replay
    replay(managementController, clusters);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(ClusterResourceProvider.CLUSTER_ID_PROPERTY_ID);
    propertyIds.add(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID);

    // create the request
    Request request = PropertyHelper.getReadRequest(propertyIds);

    // get all ... no predicate
    Set<Resource> resources = provider.getResources(request, null);

    Assert.assertEquals(5, resources.size());
    for (Resource resource : resources) {
      Long id = (Long) resource.getPropertyValue(ClusterResourceProvider.CLUSTER_ID_PROPERTY_ID);
      String name = (String) resource.getPropertyValue(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals(name, "Cluster" + id);
    }

    // get cluster named Cluster102
    Predicate predicate =
        new PredicateBuilder().property(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID).equals("Cluster102").
            toPredicate();
    resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(102L, resources.iterator().next().
        getPropertyValue(ClusterResourceProvider.CLUSTER_ID_PROPERTY_ID));
    Assert.assertEquals("Cluster102", resources.iterator().next().
        getPropertyValue(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID));

    // get cluster with id == 103
    predicate =
        new PredicateBuilder().property(ClusterResourceProvider.CLUSTER_ID_PROPERTY_ID).equals(103L).toPredicate();
    resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    Assert.assertEquals(103L, resources.iterator().next().
        getPropertyValue(ClusterResourceProvider.CLUSTER_ID_PROPERTY_ID));
    Assert.assertEquals("Cluster103", resources.iterator().next().
        getPropertyValue(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID));

    // verify
    verify(managementController, clusters);
  }

  @Test
  public void testUpdateResourcesAsAdministrator() throws Exception{
    testUpdateResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testUpdateResourcesAsClusterAdministrator() throws Exception{
    testUpdateResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResourcesAsServiceOperator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createServiceOperator());
  }

  @Test
  public void testUpdateWithConfigurationAsAdministrator() throws Exception {
    testUpdateWithConfiguration(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testUpdateWithConfigurationAsClusterAdministrator() throws Exception {
    testUpdateWithConfiguration(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateWithConfigurationAsServiceOperator() throws Exception {
    testUpdateWithConfiguration(TestAuthenticationFactory.createServiceOperator());
  }

  @Test
  public void testDeleteResourcesAsAdministrator() throws Exception{
    testDeleteResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResourcesAsNonAdministrator() throws Exception{
    testDeleteResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  //todo: configuration properties are not being added to props
  private Set<Map<String, Object>> createBlueprintRequestProperties(String clusterName, String blueprintName) {
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();
    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID, clusterName);
    properties.put(ClusterResourceProvider.BLUEPRINT, blueprintName);
    propertySet.add(properties);

    Collection<Map<String, Object>> hostGroups = new ArrayList<>();
    Map<String, Object> hostGroupProperties = new HashMap<>();
    hostGroups.add(hostGroupProperties);
    hostGroupProperties.put("name", "group1");
    Collection<Map<String, String>> hostGroupHosts = new ArrayList<>();
    hostGroupProperties.put("hosts", hostGroupHosts);
    Map<String, String> hostGroupHostProperties = new HashMap<>();
    hostGroupHostProperties.put("fqdn", "host.domain");
    hostGroupHosts.add(hostGroupHostProperties);
    properties.put("host_groups", hostGroups);

    Map<String, String> mapGroupConfigProperties = new HashMap<>();
    mapGroupConfigProperties.put("myGroupProp", "awesomeValue");

    // blueprint core-site cluster configuration properties
    Map<String, String> blueprintCoreConfigProperties = new HashMap<>();
    blueprintCoreConfigProperties.put("property1", "value2");
    blueprintCoreConfigProperties.put("new.property", "new.property.value");

    Map<String, String> blueprintGlobalConfigProperties = new HashMap<>();
    blueprintGlobalConfigProperties.put("hive_database", "New MySQL Database");

    Map<String, String> oozieEnvConfigProperties = new HashMap<>();
    oozieEnvConfigProperties.put("property1","value2");
    Map<String, String> hbaseEnvConfigProperties = new HashMap<>();
    hbaseEnvConfigProperties.put("property1","value2");
    Map<String, String> falconEnvConfigProperties = new HashMap<>();
    falconEnvConfigProperties.put("property1","value2");

    return propertySet;
  }

  private void testCreateResource_blueprint(Authentication authentication) throws Exception {
    Set<Map<String, Object>> requestProperties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);
    Map<String, Object> properties = requestProperties.iterator().next();
    Map<String, String> requestInfoProperties = new HashMap<>();
    requestInfoProperties.put(Request.REQUEST_INFO_BODY_PROPERTY, "{}");

    // set expectations
    expect(request.getProperties()).andReturn(requestProperties).anyTimes();
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties).anyTimes();

    expect(securityFactory.createSecurityConfigurationFromRequest(EasyMock.anyObject(), anyBoolean())).andReturn(null)
        .once();
    expect(topologyFactory.createProvisionClusterRequest(properties, null)).andReturn(topologyRequest).once();
    expect(topologyManager.provisionCluster(topologyRequest)).andReturn(requestStatusResponse).once();
    expect(requestStatusResponse.getRequestId()).andReturn(5150L).anyTimes();

    replayAll();
    SecurityContextHolder.getContext().setAuthentication(authentication);

    RequestStatus requestStatus = provider.createResources(request);
    assertEquals(5150L, requestStatus.getRequestResource().getPropertyValue(PropertyHelper.getPropertyId("Requests", "id")));
    assertEquals(Resource.Type.Request, requestStatus.getRequestResource().getType());
    assertEquals("Accepted", requestStatus.getRequestResource().getPropertyValue(PropertyHelper.getPropertyId("Requests", "status")));

    verifyAll();
  }

  private void testCreateResources(Authentication authentication) throws Exception{
    Resource.Type type = Resource.Type.Cluster;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    managementController.createCluster(
        AbstractResourceProviderTest.Matcher.getClusterRequest(null, "Cluster100", "HDP-0.1", null));
    managementController.createCluster(
        AbstractResourceProviderTest.Matcher.getClusterRequest(99L, null, "HDP-0.1", null));

    // replay
    replay(managementController, response);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider)provider).addObserver(observer);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    // Cluster 1: create a map of properties for the request
    Map<String, Object> properties = new LinkedHashMap<>();

    // add the cluster name to the properties map
    properties.put(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID, "Cluster100");

    // add the version to the properties map
    properties.put(ClusterResourceProvider.CLUSTER_VERSION_PROPERTY_ID, "HDP-0.1");

    propertySet.add(properties);

    // Cluster 2: create a map of properties for the request
    properties = new LinkedHashMap<>();

    // add the cluster id to the properties map
    properties.put(ClusterResourceProvider.CLUSTER_ID_PROPERTY_ID, 99L);

    // add the version to the properties map
    properties.put(ClusterResourceProvider.CLUSTER_VERSION_PROPERTY_ID, "HDP-0.1");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    ResourceProviderEvent lastEvent = observer.getLastEvent();
    Assert.assertNotNull(lastEvent);
    Assert.assertEquals(Resource.Type.Cluster, lastEvent.getResourceType());
    Assert.assertEquals(ResourceProviderEvent.Type.Create, lastEvent.getType());
    Assert.assertEquals(request, lastEvent.getRequest());
    Assert.assertNull(lastEvent.getPredicate());

    // verify
    verify(managementController, response);
  }

  public void testUpdateResources(Authentication authentication) throws Exception{
    Resource.Type type = Resource.Type.Cluster;

    Clusters clusters = createMock(Clusters.class);

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    Set<ClusterResponse> nameResponse = new HashSet<>();
    nameResponse.add(new ClusterResponse(102L, "Cluster102", State.INIT, SecurityType.NONE, null, 0, null, null));

    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");

    // set expectations
    expect(managementController.getClusters(EasyMock.anyObject())).andReturn(nameResponse).once();
    expect(managementController.updateClusters(
        AbstractResourceProviderTest.Matcher.getClusterRequestSet(102L, "Cluster102", State.INSTALLED.name(), SecurityType.NONE, "HDP-0.1", null), eq(mapRequestProps))).
        andReturn(response).once();

    expect(managementController.updateClusters(
        AbstractResourceProviderTest.Matcher.getClusterRequestSet(103L, null, null, null, "HDP-0.1", null), eq(mapRequestProps))).
        andReturn(response).once();

    expect(managementController.getClusterUpdateResults(anyObject(ClusterRequest.class))).andReturn(null).anyTimes();

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();

    // replay
    replay(managementController, response, clusters);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider)provider).addObserver(observer);

    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(ClusterResourceProvider.CLUSTER_VERSION_PROPERTY_ID, "HDP-0.1");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, mapRequestProps);

    // update the cluster named Cluster102
    Predicate  predicate = new PredicateBuilder().property(
        ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID).equals("Cluster102").toPredicate();
    provider.updateResources(request, predicate);

    // update the cluster where id == 103
    predicate = new PredicateBuilder().property(
        ClusterResourceProvider.CLUSTER_ID_PROPERTY_ID).equals(103L).toPredicate();
    provider.updateResources(request, predicate);

    ResourceProviderEvent lastEvent = observer.getLastEvent();
    Assert.assertNotNull(lastEvent);
    Assert.assertEquals(Resource.Type.Cluster, lastEvent.getResourceType());
    Assert.assertEquals(ResourceProviderEvent.Type.Update, lastEvent.getType());
    Assert.assertEquals(request, lastEvent.getRequest());
    Assert.assertEquals(predicate, lastEvent.getPredicate());

    // verify
    verify(managementController, response, clusters);
  }

  public void testUpdateWithConfiguration(Authentication authentication) throws Exception {
    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    Set<ClusterResponse> nameResponse = new HashSet<>();
    nameResponse.add(new ClusterResponse(100L, "Cluster100", State.INSTALLED, SecurityType.NONE, null, 0, null, null));

    Map<String, String> mapRequestProps = new HashMap<>();
    mapRequestProps.put("context", "Called from a test");

    // set expectations
    expect(managementController.getClusters(EasyMock.anyObject())).andReturn(nameResponse).times(2);
    expect(managementController.updateClusters(Collections.singleton(EasyMock.anyObject(ClusterRequest.class)),
        eq(mapRequestProps))).andReturn(response).times(1);
    expect(managementController.getClusterUpdateResults(anyObject(ClusterRequest.class))).andReturn(null).anyTimes();

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();

    // replay
    replay(managementController, response, clusters);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(PropertyHelper.getPropertyId("Clusters.desired_config", "type"), "global");
    properties.put(PropertyHelper.getPropertyId("Clusters.desired_config", "tag"), "version1");
    properties.put(PropertyHelper.getPropertyId("Clusters.desired_config.properties", "a"), "b");
    properties.put(PropertyHelper.getPropertyId("Clusters.desired_config.properties", "x"), "y");


    Map<String, Object> properties2 = new LinkedHashMap<>();

    properties2.put(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties2.put(PropertyHelper.getPropertyId("Clusters.desired_config", "type"), "mapred-site");
    properties2.put(PropertyHelper.getPropertyId("Clusters.desired_config", "tag"), "versio99");
    properties2.put(PropertyHelper.getPropertyId("Clusters.desired_config.properties", "foo"), "A1");
    properties2.put(PropertyHelper.getPropertyId("Clusters.desired_config.properties", "bar"), "B2");

    Set<Map<String, Object>> propertySet = new HashSet<>();

    propertySet.add(properties);
    propertySet.add(properties2);

    // create the request
    Request request = new RequestImpl(null, propertySet, mapRequestProps, null);

    Predicate  predicate = new PredicateBuilder().property(
        ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").toPredicate();

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.Cluster,
        managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider)provider).addObserver(observer);

    provider.updateResources(request, predicate);

    ResourceProviderEvent lastEvent = observer.getLastEvent();
    Assert.assertNotNull(lastEvent);
    Assert.assertEquals(Resource.Type.Cluster, lastEvent.getResourceType());
    Assert.assertEquals(ResourceProviderEvent.Type.Update, lastEvent.getType());
    Assert.assertEquals(request, lastEvent.getRequest());
    Assert.assertEquals(predicate, lastEvent.getPredicate());

    // verify
    verify(managementController, response, clusters);
  }

  public void testDeleteResources(Authentication authentication) throws Exception{
    Resource.Type type = Resource.Type.Cluster;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    // set expectations
    managementController.deleteCluster(
        AbstractResourceProviderTest.Matcher.getClusterRequest(null, "Cluster102", null, null));
    managementController.deleteCluster(
        AbstractResourceProviderTest.Matcher.getClusterRequest(103L, null, null, null));

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();

    // replay
    replay(managementController, response, clusters);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();

    ((ObservableResourceProvider)provider).addObserver(observer);

    // delete the cluster named Cluster102
    Predicate  predicate = new PredicateBuilder().property(
        ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID).equals("Cluster102").toPredicate();
    provider.deleteResources(new RequestImpl(null, null, null, null), predicate);

    // delete the cluster where id == 103
    predicate = new PredicateBuilder().property(
        ClusterResourceProvider.CLUSTER_ID_PROPERTY_ID).equals(103L).toPredicate();
    provider.deleteResources(new RequestImpl(null, null, null, null), predicate);

    ResourceProviderEvent lastEvent = observer.getLastEvent();
    Assert.assertNotNull(lastEvent);
    Assert.assertEquals(Resource.Type.Cluster, lastEvent.getResourceType());
    Assert.assertEquals(ResourceProviderEvent.Type.Delete, lastEvent.getType());
    Assert.assertEquals(predicate, lastEvent.getPredicate());
    Assert.assertNull(lastEvent.getRequest());

    // verify
    verify(managementController, response, clusters);
  }

  @Test
  public void testCreateWithRepository() throws Exception {
    Authentication authentication = TestAuthenticationFactory.createAdministrator();

    Resource.Type type = Resource.Type.Cluster;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Capture<ClusterRequest> cap = Capture.newInstance();

    managementController.createCluster(capture(cap));
    expectLastCall();

    // replay
    replay(managementController);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    // Cluster 1: create a map of properties for the request
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(ClusterResourceProvider.CLUSTER_VERSION_PROPERTY_ID, "HDP-0.1");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    // verify
    verify(managementController);

    assertTrue(cap.hasCaptured());
    assertNotNull(cap.getValue());
  }

  @Test
  public void testCreateResource_blueprint_withRepoVersion() throws Exception {
    Authentication authentication = TestAuthenticationFactory.createAdministrator();

    Set<Map<String, Object>> requestProperties = createBlueprintRequestProperties(CLUSTER_NAME, BLUEPRINT_NAME);
    Map<String, Object> properties = requestProperties.iterator().next();
    properties.put(ProvisionClusterRequest.REPO_VERSION_PROPERTY, "2.1.1");

    Map<String, String> requestInfoProperties = new HashMap<>();
    requestInfoProperties.put(Request.REQUEST_INFO_BODY_PROPERTY, "{}");

    // set expectations
    expect(request.getProperties()).andReturn(requestProperties).anyTimes();
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties).anyTimes();

    expect(securityFactory.createSecurityConfigurationFromRequest(EasyMock.anyObject(), anyBoolean())).andReturn(null)
        .once();
    expect(topologyFactory.createProvisionClusterRequest(properties, null)).andReturn(topologyRequest).once();
    expect(topologyManager.provisionCluster(topologyRequest)).andReturn(requestStatusResponse).once();
    expect(requestStatusResponse.getRequestId()).andReturn(5150L).anyTimes();

    replayAll();
    SecurityContextHolder.getContext().setAuthentication(authentication);

    RequestStatus requestStatus = provider.createResources(request);
    assertEquals(5150L, requestStatus.getRequestResource().getPropertyValue(PropertyHelper.getPropertyId("Requests", "id")));
    assertEquals(Resource.Type.Request, requestStatus.getRequestResource().getType());
    assertEquals("Accepted", requestStatus.getRequestResource().getPropertyValue(PropertyHelper.getPropertyId("Requests", "status")));

    verifyAll();
  }

}
