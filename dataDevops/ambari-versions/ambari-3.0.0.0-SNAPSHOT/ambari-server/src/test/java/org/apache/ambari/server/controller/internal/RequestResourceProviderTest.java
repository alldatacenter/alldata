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


import static org.apache.ambari.server.controller.internal.HostComponentResourceProvider.STALE_CONFIGS;
import static org.apache.ambari.server.security.TestAuthenticationFactory.createAdministrator;
import static org.apache.ambari.server.security.TestAuthenticationFactory.createViewUser;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.newCapture;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createNiceMock;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.reset;
import static org.powermock.api.easymock.PowerMock.resetAll;
import static org.powermock.api.easymock.PowerMock.verify;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.api.services.BaseRequest;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.ExecuteActionRequest;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.QueryResponse;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.customactions.ActionDefinition;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandStatusSummaryDTO;
import org.apache.ambari.server.orm.dao.RequestDAO;
import org.apache.ambari.server.orm.entities.RequestEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelperInitializer;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.HostGroup;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.apache.ambari.server.topology.HostRequest;
import org.apache.ambari.server.topology.LogicalRequest;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.topology.TopologyRequest;
import org.apache.ambari.server.utils.SecretReference;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * RequestResourceProvider tests.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ClusterControllerHelper.class})
public class RequestResourceProviderTest {

  private RequestDAO requestDAO;
  private HostRoleCommandDAO hrcDAO;
  private TopologyManager topologyManager;

  @Before
  public void before() throws Exception {

    requestDAO = createNiceMock(RequestDAO.class);
    hrcDAO = createNiceMock(HostRoleCommandDAO.class);
    topologyManager = createNiceMock(TopologyManager.class);

    reset(topologyManager);

    //todo: add assertions for topology manager interactions
    expect(topologyManager.getStageSummaries(EasyMock.<Long>anyObject())).andReturn(
      Collections.emptyMap()).anyTimes();

    expect(topologyManager.getRequests(EasyMock.anyObject())).andReturn(
      Collections.emptyList()).anyTimes();

    replay(topologyManager);


    // !!! don't mess with injectors for this test
    Field field = RequestResourceProvider.class.getDeclaredField("s_requestDAO");
    field.setAccessible(true);
    field.set(null, requestDAO);

    field = RequestResourceProvider.class.getDeclaredField("s_hostRoleCommandDAO");
    field.setAccessible(true);
    field.set(null, hrcDAO);

    field = RequestResourceProvider.class.getDeclaredField("topologyManager");
    field.setAccessible(true);
    field.set(null, topologyManager);

    field = SecretReference.class.getDeclaredField("gson");
    field.setAccessible(true);
    field.set(null, new Gson());

    AuthorizationHelperInitializer.viewInstanceDAOReturningNull();
  }

  @After
  public void cleanAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testCreateResources() throws Exception {
    Resource.Type type = Resource.Type.Request;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);


    // replay
    replay(managementController, response);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
      type,
      managementController);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    Map<String, Object> properties = new LinkedHashMap<>();

    // add properties to the request map
    properties.put(RequestResourceProvider.REQUEST_ID_PROPERTY_ID, "Request100");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    try {
      provider.createResources(request);
      Assert.fail("Expected an UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // expected
    }

    // verify
    verify(managementController, response);
  }

  @Test
  public void testGetResourcesWithRequestInfo() throws Exception {
    Resource.Type type = Resource.Type.Request;

    expect(requestDAO.findByPks(Collections.emptyList(), true)).andReturn(Collections.emptyList()).anyTimes();

    prepareGetAuthorizationExpectations();

    ActionManager actionManager = createNiceMock(ActionManager.class);

    Cluster cluster = createNiceMock(Cluster.class);
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();
    expect(cluster.getResourceId()).andReturn(4L).anyTimes();

    Clusters clusters = createNiceMock(Clusters.class);
    expect(clusters.getCluster("foo_cluster")).andReturn(cluster).anyTimes();

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    replay(managementController, clusters, cluster);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(type, managementController);

    Map<String, String> requestInfoProperties = new HashMap<>();
    Request request;
    Predicate predicate = new PredicateBuilder()
      .property(RequestResourceProvider.REQUEST_CLUSTER_NAME_PROPERTY_ID)
      .equals("foo_cluster")
      .and().property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID)
      .equals(null)
      .and().property(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID)
      .equals(null)
      .toPredicate();

    request = PropertyHelper.getReadRequest(new HashSet<>(),
      requestInfoProperties, null, null, null);

    expect(requestDAO.findAllRequestIds(BaseRequest.DEFAULT_PAGE_SIZE, false, 1L)).andReturn(Collections.emptyList()).anyTimes();
    replay(requestDAO);

    provider.getResources(request, predicate);
    verify(requestDAO);

    requestInfoProperties.put(BaseRequest.PAGE_SIZE_PROPERTY_KEY, "20");
    request = PropertyHelper.getReadRequest(new HashSet<>(),
      requestInfoProperties, null, null, null);
    provider.getResources(request, predicate);
    verify(requestDAO);

    reset(requestDAO);

    requestInfoProperties.put(BaseRequest.ASC_ORDER_PROPERTY_KEY, "true");
    request = PropertyHelper.getReadRequest(new HashSet<>(),
      requestInfoProperties, null, null, null);
    expect(requestDAO.findByPks(Collections.emptyList(), true)).andReturn(Collections.emptyList()).anyTimes();
    expect(requestDAO.findAllRequestIds(BaseRequest.DEFAULT_PAGE_SIZE, true, 1L)).andReturn(Collections.emptyList()).anyTimes();
    replay(requestDAO);

    provider.getResources(request, predicate);
    verify(requestDAO);
  }

  private void prepareGetAuthorizationExpectations() {
    prepareGetAuthorizationExpectations(true);
  }

  private void prepareGetAuthorizationExpectations(boolean allowedToAuthorize) {
    SecurityContextHolder.getContext().setAuthentication(allowedToAuthorize ? createAdministrator() : createViewUser(1L));
  }

  @Test
  public void testGetResources() throws Exception {
    Resource.Type type = Resource.Type.Request;

    String storedInputs = "{" +
        " \"hosts\": \"host1\"," +
        " \"check_execute_list\": \"last_agent_env_check,installed_packages,existing_repos,transparentHugePage\"," +
        " \"jdk_location\": \"http://ambari_server.home:8080/resources/\"," +
        " \"threshold\": \"20\"," +
        " \"password\": \"for your eyes only\"," +
        " \"foo_password\": \"for your eyes only\"," +
        " \"passwd\": \"for your eyes only\"," +
        " \"foo_passwd\": \"for your eyes only\"" +
        " }";
    String cleanedInputs = SecretReference.maskPasswordInPropertyMap(storedInputs);

    // Make sure SecretReference.maskPasswordInPropertyMap properly masked the password fields in cleanedInputs...
    Gson gson = new Gson();
    Map<String, String> map = gson.fromJson(cleanedInputs, new TypeToken<Map<String, String>>() {}.getType());
    for (Map.Entry<String, String> entry : map.entrySet()) {
      String name = entry.getKey();
      if (name.contains("password") || name.contains("passwd")) {
        Assert.assertEquals("SECRET", entry.getValue());
      }
      else {
        Assert.assertFalse("SECRET".equals(entry.getValue()));
      }
    }

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);
    RequestEntity requestMock = createNiceMock(RequestEntity.class);

    expect(requestMock.getRequestContext()).andReturn("this is a context").anyTimes();
    expect(requestMock.getRequestId()).andReturn(100L).anyTimes();
    expect(requestMock.getInputs()).andReturn(storedInputs).anyTimes();

    Capture<Collection<Long>> requestIdsCapture = newCapture();

    // set expectations
    expect(managementController.getActionManager()).andReturn(actionManager);
    expect(requestDAO.findByPks(capture(requestIdsCapture), eq(true))).andReturn(Collections.singletonList(requestMock)).anyTimes();
    expect(hrcDAO.findAggregateCounts((Long) anyObject())).andReturn(new HashMap<Long, HostRoleCommandStatusSummaryDTO>(){{
      put(1L, HostRoleCommandStatusSummaryDTO.create().inProgress(1));
    }}).anyTimes();

    prepareGetAuthorizationExpectations();

    // replay
    replay(managementController, actionManager, requestDAO, hrcDAO, requestMock);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
      type,
      managementController);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(RequestResourceProvider.REQUEST_ID_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_INPUTS_ID);

    Predicate predicate = new PredicateBuilder().property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("100").
      toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      Assert.assertEquals(100L, (long) (Long) resource.getPropertyValue(RequestResourceProvider.REQUEST_ID_PROPERTY_ID));
      Assert.assertEquals("IN_PROGRESS", resource.getPropertyValue(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID));
      Assert.assertEquals(cleanedInputs, resource.getPropertyValue(RequestResourceProvider.REQUEST_INPUTS_ID));
    }

    // verify
    verify(managementController, actionManager, requestDAO, hrcDAO);
  }

  @Test
  public void testGetResourcesWithRequestSchedule() throws Exception {
    Resource.Type type = Resource.Type.Request;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);

    RequestEntity requestMock = createNiceMock(RequestEntity.class);
    expect(requestMock.getRequestContext()).andReturn("this is a context").anyTimes();
    expect(requestMock.getRequestId()).andReturn(100L).anyTimes();
    expect(requestMock.getRequestScheduleId()).andReturn(11L).anyTimes();

    Capture<Collection<Long>> requestIdsCapture = newCapture();

    // set expectations
    expect(managementController.getActionManager()).andReturn(actionManager);
    expect(requestDAO.findByPks(capture(requestIdsCapture), eq(true))).andReturn(Collections.singletonList(requestMock)).anyTimes();
    expect(hrcDAO.findAggregateCounts((Long) anyObject())).andReturn(new HashMap<Long, HostRoleCommandStatusSummaryDTO>(){{
      put(1L, HostRoleCommandStatusSummaryDTO.create().inProgress(1));
    }}).anyTimes();

    prepareGetAuthorizationExpectations();

    // replay
    replay(managementController, actionManager, requestDAO, hrcDAO, requestMock);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
      type,
      managementController);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(RequestResourceProvider.REQUEST_ID_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_SOURCE_SCHEDULE);


    Predicate predicate = new PredicateBuilder().property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("100").
      toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      Assert.assertEquals(100L, (long) (Long) resource.getPropertyValue(RequestResourceProvider.REQUEST_ID_PROPERTY_ID));
      Assert.assertEquals("IN_PROGRESS", resource.getPropertyValue(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID));
      Assert.assertEquals(11L, (long) (Long) resource.getPropertyValue(RequestResourceProvider.REQUEST_SOURCE_SCHEDULE_ID));
    }

    // verify
    verify(managementController, actionManager, requestDAO, hrcDAO);
  }

  @Test
  public void testGetResourcesWithoutRequestSchedule() throws Exception {
    Resource.Type type = Resource.Type.Request;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);
    RequestEntity requestMock = createNiceMock(RequestEntity.class);

    expect(requestMock.getRequestContext()).andReturn("this is a context").anyTimes();
    expect(requestMock.getRequestId()).andReturn(100L).anyTimes();
    expect(requestMock.getRequestScheduleId()).andReturn(null).anyTimes();

    Capture<Collection<Long>> requestIdsCapture = newCapture();

    // set expectations
    expect(managementController.getActionManager()).andReturn(actionManager);
    expect(requestDAO.findByPks(capture(requestIdsCapture), eq(true))).andReturn(Collections.singletonList(requestMock)).anyTimes();
    expect(hrcDAO.findAggregateCounts((Long) anyObject())).andReturn(new HashMap<Long, HostRoleCommandStatusSummaryDTO>(){{
      put(1L, HostRoleCommandStatusSummaryDTO.create().inProgress(1));
    }}).anyTimes();

    prepareGetAuthorizationExpectations();

    // replay
    replay(managementController, actionManager, requestMock, requestDAO, hrcDAO);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
      type,
      managementController);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(RequestResourceProvider.REQUEST_ID_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_SOURCE_SCHEDULE);


    Predicate predicate = new PredicateBuilder().property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("100").
      toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      Assert.assertEquals(100L, (long) (Long) resource.getPropertyValue(RequestResourceProvider.REQUEST_ID_PROPERTY_ID));
      Assert.assertEquals("IN_PROGRESS", resource.getPropertyValue(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID));
      Assert.assertEquals(null, resource.getPropertyValue(RequestResourceProvider.REQUEST_SOURCE_SCHEDULE));
    }

    // verify
    verify(managementController, actionManager, requestMock, requestDAO, hrcDAO);
  }

  @Test
  public void testGetResourcesWithCluster() throws Exception {
    Resource.Type type = Resource.Type.Request;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);

    expect(cluster.getClusterId()).andReturn(50L).anyTimes();

    RequestEntity requestMock = createNiceMock(RequestEntity.class);
    expect(requestMock.getRequestContext()).andReturn("this is a context").anyTimes();
    expect(requestMock.getClusterId()).andReturn(50L).anyTimes();
    expect(requestMock.getRequestId()).andReturn(100L).anyTimes();

    Capture<Collection<Long>> requestIdsCapture = newCapture();

    // set expectations
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster("c1")).andReturn(cluster).anyTimes();
    expect(clusters.getCluster("bad-cluster")).andThrow(new AmbariException("bad cluster!")).anyTimes();
    expect(requestDAO.findByPks(capture(requestIdsCapture), eq(true))).andReturn(Collections.singletonList(requestMock));
    expect(hrcDAO.findAggregateCounts((Long) anyObject())).andReturn(new HashMap<Long, HostRoleCommandStatusSummaryDTO>(){{
      put(1L, HostRoleCommandStatusSummaryDTO.create().inProgress(1));
    }}).anyTimes();

    prepareGetAuthorizationExpectations();

    // replay
    replay(managementController, actionManager, clusters, cluster, requestMock, requestDAO, hrcDAO);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
      type,
      managementController);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(RequestResourceProvider.REQUEST_ID_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID);

    Predicate predicate = new PredicateBuilder().
      property(RequestResourceProvider.REQUEST_CLUSTER_NAME_PROPERTY_ID).equals("c1").and().
      property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("100").
      toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      Assert.assertEquals(100L, (long) (Long) resource.getPropertyValue(RequestResourceProvider.REQUEST_ID_PROPERTY_ID));
      Assert.assertEquals("IN_PROGRESS", resource.getPropertyValue(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID));
    }

    // try again with a bad cluster name
    predicate = new PredicateBuilder().
      property(RequestResourceProvider.REQUEST_CLUSTER_NAME_PROPERTY_ID).equals("bad-cluster").and().
      property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("100").
      toPredicate();
    try {
      provider.getResources(request, predicate);
    } catch (NoSuchParentResourceException e) {
      e.printStackTrace();
    }

    // verify
    verify(managementController, actionManager, clusters, cluster, requestMock, requestDAO, hrcDAO);
  }

  @Test
  public void testGetResourcesOrPredicate() throws Exception {
    Resource.Type type = Resource.Type.Request;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);

    RequestEntity requestMock = createNiceMock(RequestEntity.class);
    expect(requestMock.getRequestContext()).andReturn("this is a context").anyTimes();
    expect(requestMock.getRequestId()).andReturn(100L).anyTimes();

    RequestEntity requestMock1 = createNiceMock(RequestEntity.class);
    expect(requestMock1.getRequestContext()).andReturn("this is a context").anyTimes();
    expect(requestMock1.getRequestId()).andReturn(101L).anyTimes();

    Capture<Collection<Long>> requestIdsCapture = newCapture();

    // set expectations
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(requestDAO.findByPks(capture(requestIdsCapture), eq(true))).
      andReturn(Arrays.asList(requestMock, requestMock1)).anyTimes();
    expect(hrcDAO.findAggregateCounts((Long) anyObject())).andReturn(new HashMap<Long, HostRoleCommandStatusSummaryDTO>(){{
      put(1L, HostRoleCommandStatusSummaryDTO.create().inProgress(1));
    }}).anyTimes();

    prepareGetAuthorizationExpectations();

    // replay
    replay(managementController, actionManager, requestMock, requestMock1, requestDAO, hrcDAO);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
      type,
      managementController);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(RequestResourceProvider.REQUEST_ID_PROPERTY_ID);

    Predicate predicate = new PredicateBuilder().property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("100").
      or().property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("101").
      toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(2, resources.size());
    for (Resource resource : resources) {
      long id = (Long) resource.getPropertyValue(RequestResourceProvider.REQUEST_ID_PROPERTY_ID);
      Assert.assertTrue(id == 100L || id == 101L);
    }

    // verify
    verify(managementController, actionManager, requestMock, requestMock1, requestDAO, hrcDAO);
  }

  @Test
  public void testGetResourcesCompleted() throws Exception {
    Resource.Type type = Resource.Type.Request;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);

    RequestEntity requestMock0 = createNiceMock(RequestEntity.class);
    expect(requestMock0.getRequestContext()).andReturn("this is a context").anyTimes();
    expect(requestMock0.getRequestId()).andReturn(100L).anyTimes();

    RequestEntity requestMock1 = createNiceMock(RequestEntity.class);
    expect(requestMock1.getRequestContext()).andReturn("this is a context").anyTimes();
    expect(requestMock1.getRequestId()).andReturn(101L).anyTimes();

    Capture<Collection<Long>> requestIdsCapture = newCapture();

    // set expectations
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(requestDAO.findByPks(capture(requestIdsCapture), eq(true))).andReturn(Arrays.asList(requestMock0));
    expect(requestDAO.findByPks(capture(requestIdsCapture), eq(true))).andReturn(Arrays.asList(requestMock1));
    expect(hrcDAO.findAggregateCounts((Long) anyObject())).andReturn(new HashMap<Long, HostRoleCommandStatusSummaryDTO>(){{
      put(1L, HostRoleCommandStatusSummaryDTO.create().completed(2));
    }}).anyTimes();

    prepareGetAuthorizationExpectations();

    // replay
    replay(managementController, actionManager, requestMock0, requestMock1, requestDAO, hrcDAO);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
      type,
      managementController);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(RequestResourceProvider.REQUEST_ID_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_TASK_CNT_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_COMPLETED_TASK_CNT_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_FAILED_TASK_CNT_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_PROGRESS_PERCENT_ID);

    Predicate predicate = new PredicateBuilder().property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("100").or().
      property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("101").toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(2, resources.size());
    for (Resource resource : resources) {
      long id = (Long) resource.getPropertyValue(RequestResourceProvider.REQUEST_ID_PROPERTY_ID);
      Assert.assertTrue(id == 100L || id == 101L);
      Assert.assertEquals("COMPLETED", resource.getPropertyValue(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID));
      Assert.assertEquals(2, resource.getPropertyValue(RequestResourceProvider.REQUEST_TASK_CNT_ID));
      Assert.assertEquals(0, resource.getPropertyValue(RequestResourceProvider.REQUEST_FAILED_TASK_CNT_ID));
      Assert.assertEquals(2, resource.getPropertyValue(RequestResourceProvider.REQUEST_COMPLETED_TASK_CNT_ID));

      Assert.assertEquals(100.0, resource.getPropertyValue(RequestResourceProvider.REQUEST_PROGRESS_PERCENT_ID));
    }

    // verify
    verify(managementController, actionManager, requestMock0, requestMock1, requestDAO, hrcDAO);
  }

  @Test
  public void testGetResourcesInProgress() throws Exception {
    Resource.Type type = Resource.Type.Request;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);

    RequestEntity requestMock0 = createNiceMock(RequestEntity.class);
    expect(requestMock0.getRequestContext()).andReturn("this is a context").anyTimes();
    expect(requestMock0.getRequestId()).andReturn(100L).anyTimes();

    RequestEntity requestMock1 = createNiceMock(RequestEntity.class);
    expect(requestMock1.getRequestContext()).andReturn("this is a context").anyTimes();
    expect(requestMock1.getRequestId()).andReturn(101L).anyTimes();

    Capture<Collection<Long>> requestIdsCapture = EasyMock.newCapture();

    // set expectations
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(requestDAO.findByPks(capture(requestIdsCapture), eq(true))).andReturn(Arrays.asList(requestMock0));
    expect(requestDAO.findByPks(capture(requestIdsCapture), eq(true))).andReturn(Arrays.asList(requestMock1));

    // IN_PROGRESS and PENDING
    expect(hrcDAO.findAggregateCounts(100L)).andReturn(new HashMap<Long, HostRoleCommandStatusSummaryDTO>() {{
      put(1L, HostRoleCommandStatusSummaryDTO.create().inProgress(1).pending(1));
    }}).once();

    // IN_PROGRESS and QUEUED
    expect(hrcDAO.findAggregateCounts(101L)).andReturn(new HashMap<Long, HostRoleCommandStatusSummaryDTO>(){{
      put(1L, HostRoleCommandStatusSummaryDTO.create().inProgress(1).queued(1));
    }}).once();

    prepareGetAuthorizationExpectations();

    // replay
    replay(managementController, actionManager, requestMock0, requestMock1, requestDAO, hrcDAO);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
      type,
      managementController);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(RequestResourceProvider.REQUEST_ID_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_TASK_CNT_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_COMPLETED_TASK_CNT_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_FAILED_TASK_CNT_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_QUEUED_TASK_CNT_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_PROGRESS_PERCENT_ID);

    Predicate predicate = new PredicateBuilder().property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("100").or().
      property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("101").toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(2, resources.size());
    for (Resource resource : resources) {
      long id = (Long) resource.getPropertyValue(RequestResourceProvider.REQUEST_ID_PROPERTY_ID);
      Assert.assertTrue(id == 100L || id == 101L);
      Assert.assertEquals("IN_PROGRESS", resource.getPropertyValue(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID));
      Assert.assertEquals(2, resource.getPropertyValue(RequestResourceProvider.REQUEST_TASK_CNT_ID));
      Assert.assertEquals(0, resource.getPropertyValue(RequestResourceProvider.REQUEST_FAILED_TASK_CNT_ID));

      if (id == 100L) {
        Assert.assertEquals(0, resource.getPropertyValue(RequestResourceProvider.REQUEST_QUEUED_TASK_CNT_ID));
        int progressPercent = ((Double) resource.getPropertyValue(RequestResourceProvider.REQUEST_PROGRESS_PERCENT_ID)).intValue();
        Assert.assertEquals(17, progressPercent);
      } else {
        Assert.assertEquals(1, resource.getPropertyValue(RequestResourceProvider.REQUEST_QUEUED_TASK_CNT_ID));
        int progressPercent = ((Double) resource.getPropertyValue(RequestResourceProvider.REQUEST_PROGRESS_PERCENT_ID)).intValue();
        Assert.assertEquals(21, progressPercent);
      }
      Assert.assertEquals(0, resource.getPropertyValue(RequestResourceProvider.REQUEST_COMPLETED_TASK_CNT_ID));
    }

    // verify
    verify(managementController, actionManager, requestMock0, requestMock1, requestDAO, hrcDAO);
  }

  @Test
  public void testGetResourcesFailed() throws Exception {
    Resource.Type type = Resource.Type.Request;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);

    RequestEntity requestMock0 = createNiceMock(RequestEntity.class);
    expect(requestMock0.getRequestContext()).andReturn("this is a context").anyTimes();
    expect(requestMock0.getRequestId()).andReturn(100L).anyTimes();

    RequestEntity requestMock1 = createNiceMock(RequestEntity.class);
    expect(requestMock1.getRequestContext()).andReturn("this is a context").anyTimes();
    expect(requestMock1.getRequestId()).andReturn(101L).anyTimes();

    Capture<Collection<Long>> requestIdsCapture = EasyMock.newCapture();

    // set expectations
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(requestDAO.findByPks(capture(requestIdsCapture), eq(true))).andReturn(Arrays.asList(requestMock0));
    expect(requestDAO.findByPks(capture(requestIdsCapture), eq(true))).andReturn(Arrays.asList(requestMock1));

    // FAILED and COMPLETED
    expect(hrcDAO.findAggregateCounts(100L)).andReturn(new HashMap<Long, HostRoleCommandStatusSummaryDTO>(){{
      put(1L, HostRoleCommandStatusSummaryDTO.create().failed(1).completed(1));
    }}).once();

    // ABORTED and TIMEDOUT
    expect(hrcDAO.findAggregateCounts(101L)).andReturn(new HashMap<Long, HostRoleCommandStatusSummaryDTO>(){{
      put(1L, HostRoleCommandStatusSummaryDTO.create().aborted(1).timedout(1));
    }}).once();

    prepareGetAuthorizationExpectations();

    // replay
    replay(managementController, actionManager, requestMock0, requestMock1, requestDAO, hrcDAO);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
      type,
      managementController);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(RequestResourceProvider.REQUEST_ID_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_TASK_CNT_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_COMPLETED_TASK_CNT_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_FAILED_TASK_CNT_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_ABORTED_TASK_CNT_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_TIMED_OUT_TASK_CNT_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_PROGRESS_PERCENT_ID);

    Predicate predicate = new PredicateBuilder().property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("100").or().
      property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("101").toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(2, resources.size());
    for (Resource resource : resources) {
      long id = (Long) resource.getPropertyValue(RequestResourceProvider.REQUEST_ID_PROPERTY_ID);
      Assert.assertTrue(id == 100L || id == 101L);
      Assert.assertEquals(2, resource.getPropertyValue(RequestResourceProvider.REQUEST_TASK_CNT_ID));
      if (id == 100L) {
        Assert.assertEquals("FAILED", resource.getPropertyValue(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID));
        Assert.assertEquals(1, resource.getPropertyValue(RequestResourceProvider.REQUEST_FAILED_TASK_CNT_ID));
        Assert.assertEquals(0, resource.getPropertyValue(RequestResourceProvider.REQUEST_ABORTED_TASK_CNT_ID));
        Assert.assertEquals(0, resource.getPropertyValue(RequestResourceProvider.REQUEST_TIMED_OUT_TASK_CNT_ID));
      } else {
        Assert.assertEquals("TIMEDOUT", resource.getPropertyValue(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID));
        Assert.assertEquals(0, resource.getPropertyValue(RequestResourceProvider.REQUEST_FAILED_TASK_CNT_ID));
        Assert.assertEquals(1, resource.getPropertyValue(RequestResourceProvider.REQUEST_ABORTED_TASK_CNT_ID));
        Assert.assertEquals(1, resource.getPropertyValue(RequestResourceProvider.REQUEST_TIMED_OUT_TASK_CNT_ID));
      }
      Assert.assertEquals(2, resource.getPropertyValue(RequestResourceProvider.REQUEST_COMPLETED_TASK_CNT_ID));
      Assert.assertEquals(100.0, resource.getPropertyValue(RequestResourceProvider.REQUEST_PROGRESS_PERCENT_ID));
    }

    // verify
    verify(managementController, actionManager, requestMock0, requestMock1, requestDAO, hrcDAO);
  }

  @Test(expected = AuthorizationException.class)
  public void shouldThrowAuthorizationErrorInCaseTheAuthenticatedUserDoesNotHaveTheAppropriatePermissions() throws Exception {
    // Given
    prepareGetAuthorizationExpectations(false);
    final ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(Resource.Type.Request, createMock(AmbariManagementController.class));
    final Request request = PropertyHelper.getUpdateRequest(new LinkedHashMap<>(), null);
    final Predicate predicate = new PredicateBuilder().property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("100").toPredicate();

    // When
    provider.getResources(request, predicate);

    // Then: see expected exception
  }

  @Test
  public void testUpdateResources_CancelRequest() throws Exception {
    Resource.Type type = Resource.Type.Request;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);
    HostRoleCommand hostRoleCommand = createNiceMock(HostRoleCommand.class);
    Stage stage = createNiceMock(Stage.class);
    Clusters clusters = createNiceMock(Clusters.class);

    List<HostRoleCommand> hostRoleCommands = new LinkedList<>();
    hostRoleCommands.add(hostRoleCommand);

    Collection<Stage> stages = new HashSet<>();
    stages.add(stage);

    org.apache.ambari.server.actionmanager.Request requestMock =
      createNiceMock(org.apache.ambari.server.actionmanager.Request.class);
    expect(requestMock.getCommands()).andReturn(hostRoleCommands).anyTimes();
    expect(requestMock.getStages()).andReturn(stages).anyTimes();

    expect(stage.getOrderedHostRoleCommands()).andReturn(hostRoleCommands).anyTimes();

    Capture<Collection<Long>> requestIdsCapture = newCapture();

    // set expectations
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(actionManager.getRequests(capture(requestIdsCapture))).
      andReturn(Collections.singletonList(requestMock)).anyTimes();
    expect(hostRoleCommand.getStatus()).andReturn(HostRoleStatus.IN_PROGRESS).anyTimes();

    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    prepareGetAuthorizationExpectations();

    // replay
    replay(managementController, actionManager, hostRoleCommand, clusters, requestMock, response, stage);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
      type,
      managementController);

    // TEST CASE: Check update request validation (abort reason not specified)
    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<>();
    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);
    Predicate predicate = new PredicateBuilder().property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).
      equals("100").toPredicate();

    try {
      provider.updateResources(request, predicate);
      Assert.fail("Expected an java.lang.IllegalArgumentException: Abort reason can not be empty.");
    } catch (IllegalArgumentException e) {
      // expected
    }

    // Add abort reason to previous request
    properties.put(RequestResourceProvider.REQUEST_ABORT_REASON_PROPERTY_ID, "Some reason");

    // TEST CASE: Check update request validation (new state is not specified)
    request = PropertyHelper.getUpdateRequest(properties, null);
    try {
      provider.updateResources(request, predicate);
      Assert.fail("Expected an java.lang.IllegalArgumentException: null is wrong value.");
    } catch (IllegalArgumentException e) {
      // expected
    }

    // TEST CASE: Check update request validation (new state is wrong)
    properties.put(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID, "COMPLETED");
    request = PropertyHelper.getUpdateRequest(properties, null);
    try {
      provider.updateResources(request, predicate);
      Assert.fail("Expected an java.lang.IllegalArgumentException: COMPLETED is wrong value. " +
        "The only allowed value for updating request status is ABORTED");
    } catch (IllegalArgumentException e) {
      // expected
    }

    // TEST CASE: Check update request validation (request is in wrong state)
    // Put valid request status
    properties.put(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID, "ABORTED");
    for (HostRoleStatus status : HostRoleStatus.values()) {
      reset(hostRoleCommand);
      expect(hostRoleCommand.getStatus()).andReturn(status).anyTimes();
      replay(hostRoleCommand);
      request = PropertyHelper.getUpdateRequest(properties, null);
      if (status == HostRoleStatus.IN_PROGRESS ||
        status == HostRoleStatus.PENDING ||
        status == HostRoleStatus.HOLDING ||
        status == HostRoleStatus.HOLDING_FAILED ||
        status == HostRoleStatus.HOLDING_TIMEDOUT ||
        status == HostRoleStatus.COMPLETED ||
        status == HostRoleStatus.ABORTED ||
        status == HostRoleStatus.FAILED ||
        status == HostRoleStatus.TIMEDOUT ||
        status == HostRoleStatus.QUEUED ||
        status == HostRoleStatus.SKIPPED_FAILED) { // the only valid cases
        provider.updateResources(request, predicate);
      } else {  // In other cases, should error out
        try {
          provider.updateResources(request, predicate);
          Assert.fail("Expected an java.lang.IllegalArgumentException");
        } catch (IllegalArgumentException e) {
          // expected
        }
      }
    }
    // verify
    verify(managementController, response, stage);
  }

  @Test
  public void testDeleteResources() throws Exception {
    Resource.Type type = Resource.Type.Request;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    // replay
    replay(managementController);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
      type,
      managementController);

    Predicate predicate = new PredicateBuilder().property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).
      equals("Request100").toPredicate();
    try {
      provider.deleteResources(new RequestImpl(null, null, null, null), predicate);
      Assert.fail("Expected an UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // expected
    }

    // verify
    verify(managementController);
  }

  @Test
  public void testCreateResourcesForCommandsAsAdministrator() throws Exception {
    testCreateResourcesForCommands(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testCreateResourcesForCommandsAsClusterAdministrator() throws Exception {
    testCreateResourcesForCommands(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testCreateResourcesForCommandsAsServiceAdministrator() throws Exception {
    testCreateResourcesForCommands(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test
  public void testCreateResourcesForCommandsAsServiceOperator() throws Exception {
    testCreateResourcesForCommands(TestAuthenticationFactory.createServiceOperator());
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesForCommandsAsClusterUser() throws Exception {
    testCreateResourcesForCommands(TestAuthenticationFactory.createClusterUser());
  }

  private void testCreateResourcesForCommands(Authentication authentication) throws Exception {
    Resource.Type type = Resource.Type.Request;

    Capture<ExecuteActionRequest> actionRequest = newCapture();
    Capture<HashMap<String, String>> propertyMap = newCapture();

    Cluster cluster = createMock(Cluster.class);
    expect(cluster.getClusterId()).andReturn(2L).anyTimes();
    expect(cluster.getResourceId()).andReturn(4L).anyTimes();

    Clusters clusters = createMock(Clusters.class);
    expect(clusters.getCluster("c1")).andReturn(cluster).anyTimes();

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.createAction(capture(actionRequest), capture(propertyMap)))
      .andReturn(response).anyTimes();
    expect(response.getMessage()).andReturn("Message").anyTimes();

    // replay
    replay(cluster, clusters, managementController, response);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(RequestResourceProvider.REQUEST_CLUSTER_NAME_PROPERTY_ID, "c1");

    Set<Map<String, Object>> filterSet = new HashSet<>();
    Map<String, Object> filterMap = new HashMap<>();
    filterMap.put(RequestResourceProvider.SERVICE_ID, "HDFS");
    filterSet.add(filterMap);

    properties.put(RequestResourceProvider.REQUEST_RESOURCE_FILTER_ID, filterSet);

    propertySet.add(properties);

    Map<String, String> requestInfoProperties = new HashMap<>();
    requestInfoProperties.put(RequestResourceProvider.COMMAND_ID, "HDFS_SERVICE_CHECK");



    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, requestInfoProperties);
    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
      type,
      managementController);

    provider.createResources(request);
    ExecuteActionRequest capturedRequest = actionRequest.getValue();

    Assert.assertTrue(actionRequest.hasCaptured());
    Assert.assertTrue(capturedRequest.isCommand());
    Assert.assertEquals(null, capturedRequest.getActionName());
    Assert.assertEquals("HDFS_SERVICE_CHECK", capturedRequest.getCommandName());
    Assert.assertNotNull(capturedRequest.getResourceFilters());
    Assert.assertEquals(1, capturedRequest.getResourceFilters().size());
    RequestResourceFilter capturedResourceFilter = capturedRequest.getResourceFilters().get(0);
    Assert.assertEquals("HDFS", capturedResourceFilter.getServiceName());
    Assert.assertEquals(null, capturedResourceFilter.getComponentName());
    Assert.assertNotNull(capturedResourceFilter.getHostNames());
    Assert.assertEquals(0, capturedResourceFilter.getHostNames().size());
    Assert.assertEquals(1, actionRequest.getValue().getParameters().size());
  }

  @Test
  public void testCreateResourcesForCommandsWithParamsAsAdministrator() throws Exception {
    testCreateResourcesForCommandsWithParams(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testCreateResourcesForCommandsWithParamsAsClusterAdministrator() throws Exception {
    testCreateResourcesForCommandsWithParams(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testCreateResourcesForCommandsWithParamsAsServiceAdministrator() throws Exception {
    testCreateResourcesForCommandsWithParams(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test
  public void testCreateResourcesForCommandsWithParamsAsServiceOperator() throws Exception {
    testCreateResourcesForCommandsWithParams(TestAuthenticationFactory.createServiceOperator());
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesForCommandsWithParamsAsClusterUser() throws Exception {
    testCreateResourcesForCommandsWithParams(TestAuthenticationFactory.createClusterUser());
  }

  private void testCreateResourcesForCommandsWithParams(Authentication authentication) throws Exception {
    Resource.Type type = Resource.Type.Request;

    Capture<ExecuteActionRequest> actionRequest = newCapture();
    Capture<HashMap<String, String>> propertyMap = newCapture();

    Cluster cluster = createMock(Cluster.class);
    expect(cluster.getClusterId()).andReturn(2L).anyTimes();
    expect(cluster.getResourceId()).andReturn(4L).anyTimes();

    Clusters clusters = createMock(Clusters.class);
    expect(clusters.getCluster("c1")).andReturn(cluster).anyTimes();

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.createAction(capture(actionRequest), capture(propertyMap)))
      .andReturn(response).anyTimes();
    expect(response.getMessage()).andReturn("Message").anyTimes();
    // replay
    replay(cluster, clusters, managementController, response);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(RequestResourceProvider.REQUEST_CLUSTER_NAME_PROPERTY_ID, "c1");

    Set<Map<String, Object>> filterSet = new HashSet<>();
    Map<String, Object> filterMap = new HashMap<>();
    filterMap.put(RequestResourceProvider.SERVICE_ID, "HDFS");
    filterMap.put(RequestResourceProvider.HOSTS_ID, "host1,host2,host3");
    filterSet.add(filterMap);

    properties.put(RequestResourceProvider.REQUEST_RESOURCE_FILTER_ID, filterSet);

    propertySet.add(properties);

    Map<String, String> requestInfoProperties = new HashMap<>();

    requestInfoProperties.put("parameters/param1", "value1");
    requestInfoProperties.put("parameters/param2", "value2");

    String[] expectedHosts = new String[]{"host1", "host2", "host3"};
    Map<String, String> expectedParams = new HashMap<String, String>() {{
      put("param1", "value1");
      put("param2", "value2");
    }};

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, requestInfoProperties);
    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
      type,
      managementController);

    // Neither action nor commands are specified
    try {
      provider.createResources(request);
    } catch (UnsupportedOperationException ex) {
      Assert.assertTrue(ex.getMessage().contains("Either command or action must be specified"));
    }

    // Both action and command are specified
    requestInfoProperties.put(RequestResourceProvider.COMMAND_ID, "HDFS_SERVICE_CHECK");
    requestInfoProperties.put(RequestResourceProvider.ACTION_ID, "a1");
    request = PropertyHelper.getCreateRequest(propertySet, requestInfoProperties);
    try {
      provider.createResources(request);
    } catch (UnsupportedOperationException ex) {
      Assert.assertTrue(ex.getMessage().contains("Both command and action cannot be specified"));
    }
    requestInfoProperties.remove(RequestResourceProvider.ACTION_ID);
    request = PropertyHelper.getCreateRequest(propertySet, requestInfoProperties);

    provider.createResources(request);
    Assert.assertTrue(actionRequest.hasCaptured());
    ExecuteActionRequest capturedRequest = actionRequest.getValue();
    Assert.assertTrue(capturedRequest.isCommand());
    Assert.assertEquals(null, capturedRequest.getActionName());
    Assert.assertEquals("HDFS_SERVICE_CHECK", capturedRequest.getCommandName());
    Assert.assertEquals(1, capturedRequest.getResourceFilters().size());
    RequestResourceFilter capturedResourceFilter = capturedRequest.getResourceFilters().get(0);
    Assert.assertEquals("HDFS", capturedResourceFilter.getServiceName());
    Assert.assertEquals(null, capturedResourceFilter.getComponentName());
    Assert.assertEquals(3, capturedResourceFilter.getHostNames().size());
    Assert.assertArrayEquals(expectedHosts, capturedResourceFilter.getHostNames().toArray());
    Assert.assertEquals(3, capturedRequest.getParameters().size());
    for(String key : expectedParams.keySet()) {
      Assert.assertEquals(expectedParams.get(key), capturedRequest.getParameters().get(key));
    }
  }

  @Test
  public void testCreateResourcesForCommandWithHostPredicate() throws Exception {
    Resource.Type type = Resource.Type.Request;

    Capture<ExecuteActionRequest> actionRequest = EasyMock.newCapture();
    Capture<HashMap<String, String>> propertyMap = EasyMock.newCapture();
    Capture<Request> requestCapture = EasyMock.newCapture();
    Capture<Predicate> predicateCapture = EasyMock.newCapture();

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    Clusters clusters = createNiceMock(Clusters.class);

    expect(managementController.createAction(capture(actionRequest),
      capture(propertyMap))).andReturn(response).anyTimes();
    expect(response.getMessage()).andReturn("Message").anyTimes();
    expect(managementController.getClusters()).andReturn(clusters);

    ClusterControllerImpl controller = createNiceMock(ClusterControllerImpl.class);
    HostComponentProcessResourceProvider hostComponentProcessResourceProvider = createNiceMock(HostComponentProcessResourceProvider.class);
    PowerMock.mockStatic(ClusterControllerHelper.class);

    Resource resource = createNiceMock(Resource.class);
    Collection<Resource> resources = Collections.singleton(resource);
    Iterable<Resource> resourceIterable = new Iterable<Resource>() {
      @Override
      public Iterator<Resource> iterator() {
        return resources.iterator();
      }
    };

    expect(ClusterControllerHelper.getClusterController()).andReturn(controller);
    expect(controller.ensureResourceProvider(Resource.Type.HostComponent)).andReturn(hostComponentProcessResourceProvider);
    QueryResponse queryResponse = createNiceMock(QueryResponse.class);
    expect(controller.getResources(eq(Resource.Type.HostComponent), capture(requestCapture),
      capture(predicateCapture))).andReturn(queryResponse);
    expect(controller.getIterable(eq(Resource.Type.HostComponent), eq(queryResponse),
      (Request) anyObject(), (Predicate) anyObject(), eq(null), eq(null)))
      .andReturn(resourceIterable);

    // replay
    replayAll();

    SecurityContextHolder.getContext().setAuthentication(
      TestAuthenticationFactory.createAdministrator());

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(RequestResourceProvider.REQUEST_CLUSTER_NAME_PROPERTY_ID, "c1");

    Set<Map<String, Object>> filterSet = new HashSet<>();
    String predicateProperty = STALE_CONFIGS + "=true";
    Map<String, Object> filterMap = new HashMap<>();
    filterMap.put(RequestResourceProvider.HOSTS_PREDICATE, predicateProperty);
    filterSet.add(filterMap);

    properties.put(RequestResourceProvider.REQUEST_RESOURCE_FILTER_ID, filterSet);
    propertySet.add(properties);

    Map<String, String> requestInfoProperties = new HashMap<>();
    requestInfoProperties.put(RequestResourceProvider.COMMAND_ID, "RESTART");
    requestInfoProperties.put(RequestResourceProvider.REQUEST_CONTEXT_ID, "Restart All with Stale Configs");

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, requestInfoProperties);
    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
      type,
      managementController);

    provider.createResources(request);
    ExecuteActionRequest capturedRequest = actionRequest.getValue();

    Assert.assertTrue(requestCapture.hasCaptured());
    Assert.assertTrue(predicateCapture.hasCaptured());
    Map<String, Object> predicateProperties = PredicateHelper.getProperties(predicateCapture.getValue());
    String propertyIdToAssert = null;
    Object propertyValueToAssert = null;
    for (Map.Entry<String, Object> predicateEntry : predicateProperties.entrySet()) {
      if (predicateEntry.getKey().equals(STALE_CONFIGS)) {
        propertyIdToAssert = predicateEntry.getKey();
        propertyValueToAssert = predicateEntry.getValue();
      }
    }
    Assert.assertNotNull(propertyIdToAssert);
    Assert.assertEquals("true", propertyValueToAssert);
    Assert.assertTrue(capturedRequest.getResourceFilters().isEmpty());
    Assert.assertEquals(1, capturedRequest.getParameters().size());
    Assert.assertEquals("true", capturedRequest.getParameters().get(RequestResourceProvider.HAS_RESOURCE_FILTERS));
  }

  @Test
  public void testCreateResourcesForCommandsWithOpLvlAsAdministrator() throws Exception {
    testCreateResourcesForCommandsWithOpLvl(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testCreateResourcesForCommandsWithOpLvlAsClusterAdministrator() throws Exception {
    testCreateResourcesForCommandsWithOpLvl(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test
  public void testCreateResourcesForCommandsWithOpLvlAsServiceAdministrator() throws Exception {
    testCreateResourcesForCommandsWithOpLvl(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test
  public void testCreateResourcesForCommandsWithOpLvlAsServiceOperator() throws Exception {
    testCreateResourcesForCommandsWithOpLvl(TestAuthenticationFactory.createServiceOperator());
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesForCommandsWithOpLvlAsClusterUser() throws Exception {
    testCreateResourcesForCommandsWithOpLvl(TestAuthenticationFactory.createClusterUser());
  }

  private void testCreateResourcesForCommandsWithOpLvl(Authentication authentication) throws Exception {
    Resource.Type type = Resource.Type.Request;

    Capture<ExecuteActionRequest> actionRequest = newCapture();
    Capture<HashMap<String, String>> propertyMap = newCapture();

    Cluster cluster = createMock(Cluster.class);
    expect(cluster.getClusterId()).andReturn(2L).anyTimes();
    expect(cluster.getResourceId()).andReturn(4L).anyTimes();

    Clusters clusters = createMock(Clusters.class);
    expect(clusters.getCluster("c1")).andReturn(cluster).anyTimes();

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(managementController.createAction(capture(actionRequest), capture(propertyMap)))
      .andReturn(response).anyTimes();
    expect(response.getMessage()).andReturn("Message").anyTimes();

    // replay
    replay(cluster, clusters, managementController, response);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    Map<String, Object> properties = new LinkedHashMap<>();

    String c1 = "c1";
    String host_component = "HOST_COMPONENT";
    String service_id = "HDFS";
    String hostcomponent_id = "Namenode";
    String host_name = "host1";

    properties.put(RequestResourceProvider.REQUEST_CLUSTER_NAME_PROPERTY_ID, c1);

    Set<Map<String, Object>> filterSet = new HashSet<>();
    Map<String, Object> filterMap = new HashMap<>();
    filterMap.put(RequestResourceProvider.SERVICE_ID, service_id);
    filterMap.put(RequestResourceProvider.HOSTS_ID, host_name);
    filterSet.add(filterMap);

    properties.put(RequestResourceProvider.REQUEST_RESOURCE_FILTER_ID, filterSet);

    propertySet.add(properties);

    Map<String, String> requestInfoProperties = new HashMap<>();
    requestInfoProperties.put(RequestResourceProvider.COMMAND_ID, "RESTART");

    requestInfoProperties.put(RequestOperationLevel.OPERATION_LEVEL_ID,
      host_component);
    requestInfoProperties.put(RequestOperationLevel.OPERATION_CLUSTER_ID, c1);
    requestInfoProperties.put(RequestOperationLevel.OPERATION_SERVICE_ID,
      service_id);
    requestInfoProperties.put(RequestOperationLevel.OPERATION_HOSTCOMPONENT_ID,
      hostcomponent_id);
    requestInfoProperties.put(RequestOperationLevel.OPERATION_HOST_NAME,
      host_name);

    Request request = PropertyHelper.getCreateRequest(propertySet, requestInfoProperties);
    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
      type,
      managementController);

    requestInfoProperties.put(RequestOperationLevel.OPERATION_CLUSTER_ID, c1);

    // create request in a normal way (positive scenario)
    provider.createResources(request);
    Assert.assertTrue(actionRequest.hasCaptured());
    ExecuteActionRequest capturedRequest = actionRequest.getValue();
    RequestOperationLevel level = capturedRequest.getOperationLevel();
    Assert.assertEquals(level.getLevel().toString(), "HostComponent");
    Assert.assertEquals(level.getClusterName(), c1);
    Assert.assertEquals(level.getServiceName(), service_id);
    Assert.assertEquals(level.getHostComponentName(), hostcomponent_id);
    Assert.assertEquals(level.getHostName(), host_name);
  }

  @Test
  public void testCreateResourcesCheckHostForNonClusterAsAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator(), null, null, "check_host",
        EnumSet.of(RoleAuthorization.HOST_ADD_DELETE_HOSTS));
  }

  public void testCreateResourcesCheckHostForNonClusterAsClusterAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator(), null, null, "check_host",
        EnumSet.of(RoleAuthorization.HOST_ADD_DELETE_HOSTS));
  }

  public void testCreateResourcesCheckHostForNonClusterAsClusterOperator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterOperator(), null, null, "check_host",
        EnumSet.of(RoleAuthorization.HOST_ADD_DELETE_HOSTS));
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesCheckHostForNonClusterAsServiceAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceAdministrator(), null, null, "check_host",
        EnumSet.of(RoleAuthorization.HOST_ADD_DELETE_HOSTS));
  }

  @Test
  public void testCreateResourcesCheckHostForClusterAsAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator(), "c1", null, "check_host",
        EnumSet.of(RoleAuthorization.HOST_ADD_DELETE_HOSTS));
  }

  @Test
  public void testCreateResourcesCheckHostForClusterAsClusterAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator(), "c1", null, "check_host",
        EnumSet.of(RoleAuthorization.HOST_ADD_DELETE_HOSTS));
  }

  @Test
  public void testCreateResourcesCheckHostForClusterAsClusterOperator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterOperator(), "c1", null, "check_host",
        EnumSet.of(RoleAuthorization.HOST_ADD_DELETE_HOSTS));
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesCheckHostForClusterAsServiceAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceAdministrator(), "c1", null, "check_host",
        EnumSet.of(RoleAuthorization.HOST_ADD_DELETE_HOSTS));
  }

  @Test
  public void testCreateResourcesServiceCheckForClusterAsAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator(), "c1", "SOME_SERVICE_CHECK", null, null);
  }

  @Test
  public void testCreateResourcesServiceCheckForClusterAsClusterAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator(), "c1", "SOME_SERVICE_CHECK", null, null);
  }

  @Test
  public void testCreateResourcesServiceCheckForClusterAsClusterOperator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterOperator(), "c1", "SOME_SERVICE_CHECK", null, null);
  }

  @Test
  public void testCreateResourcesServiceCheckForClusterAsServiceAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceAdministrator(), "c1", "SOME_SERVICE_CHECK", null, null);
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesServiceCheckForClusterAsClusterUser() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterUser(), "c1", "SOME_SERVICE_CHECK", null, null);
  }
  @Test
  public void testCreateResourcesDecommissionForClusterAsAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator(), "c1", "SOME_SERVICE_CHECK", null, null);
  }

  @Test
  public void testCreateResourcesDecommissionForClusterAsClusterAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator(), "c1", "SOME_SERVICE_CHECK", null, null);
  }

  @Test
  public void testCreateResourcesDecommissionForClusterAsClusterOperator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterOperator(), "c1", "SOME_SERVICE_CHECK", null, null);
  }

  @Test
  public void testCreateResourcesDecommissionForClusterAsServiceAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceAdministrator(), "c1", "SOME_SERVICE_CHECK", null, null);
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesDecommissionForClusterAsClusterUser() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterUser(), "c1", "SOME_SERVICE_CHECK", null, null);
  }

  @Test
  public void testCreateResourcesCustomActionNoPrivsForNonClusterAsAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator(), null, null, "custom_action", null);
  }

  @Test
  public void testCreateResourcesCustomActionNoPrivsForNonClusterAsClusterAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator(), null, null, "custom_action", null);
  }

  @Test
  public void testCreateResourcesCustomActionNoPrivsForNonClusterAsClusterOperator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterOperator(), null, null, "custom_action", null);
  }

  @Test
  public void testCreateResourcesForNonClusterAsServiceAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceAdministrator(), null, null, "custom_action", null);
  }

  private void testCreateResources(Authentication authentication, String clusterName, String commandName, String actionName, Set<RoleAuthorization> permissions) throws Exception {
    Resource.Type type = Resource.Type.Request;

    Capture<ExecuteActionRequest> actionRequest = newCapture();
    Capture<HashMap<String, String>> propertyMap = newCapture();

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    AmbariMetaInfo metaInfo = createMock(AmbariMetaInfo.class);
    ActionDefinition actionDefinition = createMock(ActionDefinition.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    expect(managementController.createAction(capture(actionRequest), capture(propertyMap)))
      .andReturn(response).anyTimes();
    expect(managementController.getAmbariMetaInfo()).andReturn(metaInfo).anyTimes();
    expect(metaInfo.getActionDefinition(actionName)).andReturn(actionDefinition).anyTimes();
    expect(actionDefinition.getPermissions()).andReturn(permissions).anyTimes();
    expect(response.getMessage()).andReturn("Message").anyTimes();

    Cluster cluster = createMock(Cluster.class);
    Clusters clusters = createMock(Clusters.class);
    if(clusterName != null) {
      expect(cluster.getResourceId()).andReturn(4L).anyTimes();

      expect(clusters.getCluster(clusterName)).andReturn(cluster).anyTimes();

      expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    }

    // replay
    replay(managementController, metaInfo, actionDefinition, response, cluster, clusters);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    Map<String, Object> properties = new LinkedHashMap<>();

    Set<Map<String, Object>> filterSet = new HashSet<>();
    Map<String, Object> filterMap = new HashMap<>();
    filterMap.put(RequestResourceProvider.HOSTS_ID, "h1,h2");
    filterSet.add(filterMap);

    properties.put(RequestResourceProvider.REQUEST_RESOURCE_FILTER_ID, filterSet);
    properties.put(RequestResourceProvider.REQUEST_CLUSTER_NAME_PROPERTY_ID, clusterName);
    propertySet.add(properties);

    Map<String, String> requestInfoProperties = new HashMap<>();
    if(commandName != null) {
      requestInfoProperties.put(RequestResourceProvider.COMMAND_ID, commandName);
    }
    if(actionName != null) {
      requestInfoProperties.put(RequestResourceProvider.ACTION_ID, actionName);
    }

    SecurityContextHolder.getContext().setAuthentication(authentication);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, requestInfoProperties);
    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
      type,
      managementController);
    provider.createResources(request);
    ExecuteActionRequest capturedRequest = actionRequest.getValue();

    Assert.assertTrue(actionRequest.hasCaptured());

    if(actionName != null) {
      Assert.assertFalse("expected an action", capturedRequest.isCommand());
      Assert.assertEquals(actionName, capturedRequest.getActionName());
    }
    if(commandName != null) {
      Assert.assertTrue("expected a command", capturedRequest.isCommand());
      Assert.assertEquals(commandName, capturedRequest.getCommandName());
    }
    Assert.assertNotNull(capturedRequest.getResourceFilters());
    Assert.assertEquals(1, capturedRequest.getResourceFilters().size());
    RequestResourceFilter capturedResourceFilter = capturedRequest.getResourceFilters().get(0);
    Assert.assertEquals(null, capturedResourceFilter.getServiceName());
    Assert.assertEquals(null, capturedResourceFilter.getComponentName());
    Assert.assertNotNull(capturedResourceFilter.getHostNames());
    Assert.assertEquals(2, capturedResourceFilter.getHostNames().size());
    Assert.assertEquals(1, actionRequest.getValue().getParameters().size());
  }

  @Test
  public void testGetResourcesWithoutCluster() throws Exception {
    Resource.Type type = Resource.Type.Request;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);

    Clusters clusters = createNiceMock(Clusters.class);

    RequestEntity requestMock = createNiceMock(RequestEntity.class);
    expect(requestMock.getRequestContext()).andReturn("this is a context").anyTimes();
    expect(requestMock.getRequestId()).andReturn(100L).anyTimes();

    Capture<Collection<Long>> requestIdsCapture = newCapture();

    // set expectations
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster(anyObject(String.class))).andReturn(null).anyTimes();
    expect(requestDAO.findByPks(capture(requestIdsCapture), eq(true))).andReturn(Collections.singletonList(requestMock));
    expect(hrcDAO.findAggregateCounts((Long) anyObject())).andReturn(new HashMap<Long, HostRoleCommandStatusSummaryDTO>(){{
      put(1L, HostRoleCommandStatusSummaryDTO.create().inProgress(1));
    }}).anyTimes();

    prepareGetAuthorizationExpectations();

    // replay
    replay(managementController, actionManager, clusters, requestMock, requestDAO, hrcDAO);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
      type,
      managementController);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(RequestResourceProvider.REQUEST_ID_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID);

    Predicate predicate = new PredicateBuilder().
      property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("100").
      toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      Assert.assertEquals(100L, (long) (Long) resource.getPropertyValue(RequestResourceProvider.REQUEST_ID_PROPERTY_ID));
      Assert.assertEquals("IN_PROGRESS", resource.getPropertyValue(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID));
      Assert.assertNull(resource.getPropertyValue(RequestResourceProvider.REQUEST_CLUSTER_NAME_PROPERTY_ID));
    }

    // verify
    verify(managementController, actionManager, clusters, requestMock, requestDAO, hrcDAO);
  }

  @Test
  public void testRequestStatusWithNoTasks() throws Exception {
    Resource.Type type = Resource.Type.Request;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    ActionManager actionManager = createNiceMock(ActionManager.class);

    Clusters clusters = createNiceMock(Clusters.class);

    RequestEntity requestMock = createNiceMock(RequestEntity.class);
    expect(requestMock.getRequestContext()).andReturn("this is a context").anyTimes();
    expect(requestMock.getRequestId()).andReturn(100L).anyTimes();

    Capture<Collection<Long>> requestIdsCapture = newCapture();

    // set expectations
    expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getCluster(anyObject(String.class))).andReturn(null).anyTimes();
    expect(requestDAO.findByPks(capture(requestIdsCapture), eq(true))).andReturn(Collections.singletonList(requestMock));
    expect(hrcDAO.findAggregateCounts((Long) anyObject())).andReturn(
      Collections.emptyMap()).anyTimes();

    prepareGetAuthorizationExpectations();

    // replay
    replay(managementController, actionManager, clusters, requestMock, requestDAO, hrcDAO);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
      type,
      managementController);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(RequestResourceProvider.REQUEST_ID_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID);
    propertyIds.add(RequestResourceProvider.REQUEST_PROGRESS_PERCENT_ID);

    Predicate predicate = new PredicateBuilder().
      property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("100").
      toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      Assert.assertEquals(100L, (long) (Long) resource.getPropertyValue(RequestResourceProvider.REQUEST_ID_PROPERTY_ID));
      Assert.assertEquals("COMPLETED", resource.getPropertyValue(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID));
      Assert.assertEquals(100.0, resource.getPropertyValue(RequestResourceProvider.REQUEST_PROGRESS_PERCENT_ID));
    }

    // verify
    verify(managementController, actionManager, clusters, requestMock, requestDAO, hrcDAO);
  }

  /**
   * Tests that if there are no tasks, topology requests return status they get from the logical request.
   */
  @Test
  @PrepareForTest(AmbariServer.class)
  public void testGetLogicalRequestStatusWithNoTasks() throws Exception {
    Iterable<CalculatedStatus> statusList = ImmutableList.of(CalculatedStatus.COMPLETED, CalculatedStatus.PENDING, CalculatedStatus.ABORTED);
    for (CalculatedStatus calculatedStatus : statusList) {
      // Given
      resetAll();

      PowerMock.mockStatic(AmbariServer.class);
      AmbariManagementController managementController = createMock(AmbariManagementController.class);
      ActionManager actionManager = createNiceMock(ActionManager.class);
      Clusters clusters = createNiceMock(Clusters.class);
      Cluster cluster = createNiceMock(Cluster.class);
      RequestEntity requestMock = createNiceMock(RequestEntity.class);
      Blueprint blueprint = createNiceMock(Blueprint.class);
      ClusterTopology topology = createNiceMock(ClusterTopology.class);
      HostGroup hostGroup = createNiceMock(HostGroup.class);
      TopologyRequest topologyRequest = createNiceMock(TopologyRequest.class);
      LogicalRequest logicalRequest = createNiceMock(LogicalRequest.class);
      HostRequest hostRequest = createNiceMock(HostRequest.class);

      Long requestId = 100L;
      Long clusterId = 2L;
      String clusterName = "cluster1";
      String hostGroupName = "host_group_1";
      HostGroupInfo hostGroupInfo = new HostGroupInfo(hostGroupName);
      hostGroupInfo.setRequestedCount(1);
      Map<String, HostGroupInfo> hostGroupInfoMap = ImmutableMap.of(hostGroupName, hostGroupInfo);
      Collection<HostRequest> hostRequests = Collections.singletonList(hostRequest);
      Map<Long, HostRoleCommandStatusSummaryDTO> dtoMap = Collections.emptyMap();

      expect(AmbariServer.getController()).andReturn(managementController).anyTimes();
      expect(requestMock.getRequestContext()).andReturn("this is a context").anyTimes();
      expect(requestMock.getRequestId()).andReturn(requestId).anyTimes();
      expect(hostGroup.getName()).andReturn(hostGroupName).anyTimes();
      expect(blueprint.getHostGroup(hostGroupName)).andReturn(hostGroup).anyTimes();
      expect(topology.getClusterId()).andReturn(2L).anyTimes();
      expect(cluster.getClusterId()).andReturn(clusterId).anyTimes();
      expect(cluster.getClusterName()).andReturn(clusterName).anyTimes();
      expect(managementController.getActionManager()).andReturn(actionManager).anyTimes();
      expect(managementController.getClusters()).andReturn(clusters).anyTimes();
      expect(clusters.getCluster(eq(clusterName))).andReturn(cluster).anyTimes();
      expect(clusters.getClusterById(clusterId)).andReturn(cluster).anyTimes();
      Collection<Long> requestIds = anyObject();
      expect(requestDAO.findByPks(requestIds, eq(true))).andReturn(Lists.newArrayList(requestMock));
      expect(hrcDAO.findAggregateCounts((Long) anyObject())).andReturn(dtoMap).anyTimes();
      expect(topologyManager.getRequest(requestId)).andReturn(logicalRequest).anyTimes();
      expect(topologyManager.getRequests(eq(Collections.singletonList(requestId)))).andReturn(Collections.singletonList(logicalRequest)).anyTimes();
      expect(topologyManager.getStageSummaries(EasyMock.<Long>anyObject())).andReturn(dtoMap).anyTimes();

      expect(topologyRequest.getHostGroupInfo()).andReturn(hostGroupInfoMap).anyTimes();
      expect(topology.getBlueprint()).andReturn(blueprint).anyTimes();
      expect(blueprint.shouldSkipFailure()).andReturn(true).anyTimes();

      expect(logicalRequest.getHostRequests()).andReturn(hostRequests).anyTimes();
      expect(logicalRequest.constructNewPersistenceEntity()).andReturn(requestMock).anyTimes();
      expect(logicalRequest.calculateStatus()).andReturn(calculatedStatus).anyTimes();
      Optional<String> failureReason = calculatedStatus == CalculatedStatus.ABORTED
        ? Optional.of("some reason")
        : Optional.<String>absent();
      expect(logicalRequest.getFailureReason()).andReturn(failureReason).anyTimes();

      prepareGetAuthorizationExpectations();

      replayAll();

      Resource.Type type = Resource.Type.Request;
      ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController
      );

      Set<String> propertyIds = ImmutableSet.of(
        RequestResourceProvider.REQUEST_ID_PROPERTY_ID,
        RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID,
        RequestResourceProvider.REQUEST_PROGRESS_PERCENT_ID,
        RequestResourceProvider.REQUEST_CONTEXT_ID
      );

      Predicate predicate = new PredicateBuilder().
        property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals("100").
        toPredicate();

      Request request = PropertyHelper.getReadRequest(propertyIds);

      // When
      Set<Resource> resources = provider.getResources(request, predicate);

      // Then
      verifyAll();

      Assert.assertEquals(1, resources.size());
      Resource resource = Iterables.getOnlyElement(resources);
      Assert.assertEquals(requestId, resource.getPropertyValue(RequestResourceProvider.REQUEST_ID_PROPERTY_ID));
      Assert.assertEquals(calculatedStatus.getStatus().toString(), resource.getPropertyValue(RequestResourceProvider.REQUEST_STATUS_PROPERTY_ID));
      Assert.assertEquals(calculatedStatus.getPercent(), resource.getPropertyValue(RequestResourceProvider.REQUEST_PROGRESS_PERCENT_ID));

      Object requestContext = resource.getPropertyValue(RequestResourceProvider.REQUEST_CONTEXT_ID);
      Assert.assertNotNull(requestContext);
      Assert.assertTrue(!failureReason.isPresent() || requestContext.toString().contains(failureReason.get()));
    }
  }
}
