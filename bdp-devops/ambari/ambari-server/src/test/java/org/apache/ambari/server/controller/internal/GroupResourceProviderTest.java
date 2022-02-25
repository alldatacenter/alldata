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
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.GroupRequest;
import org.apache.ambari.server.controller.GroupResponse;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * GroupResourceProvider tests.
 */
public class GroupResourceProviderTest {

  @Before
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testCreateResources_Administrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResources_ClusterAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator("User1", 2L));
  }

  private void testCreateResources(Authentication authentication) throws Exception {
    Resource.Type type = Resource.Type.Group;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    managementController.createGroups(AbstractResourceProviderTest.Matcher.getGroupRequestSet("engineering"));

    // replay
    replay(managementController, response);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    Map<String, Object> properties = new LinkedHashMap<>();

    // add properties to the request map
    properties.put(GroupResourceProvider.GROUP_GROUPNAME_PROPERTY_ID, "engineering");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    // verify
    verify(managementController, response);
  }

  @Test
  public void testGetResources_Administrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResources_ClusterAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createClusterAdministrator("User1", 2L));
  }

  public void testGetResources(Authentication authentication) throws Exception {
    Resource.Type type = Resource.Type.Group;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    Set<GroupResponse> allResponse = new HashSet<>();
    allResponse.add(new GroupResponse("engineering", false));

    // set expectations
    expect(managementController.getGroups(AbstractResourceProviderTest.Matcher.getGroupRequestSet("engineering"))).
        andReturn(allResponse).once();

    // replay
    replay(managementController);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(GroupResourceProvider.GROUP_GROUPNAME_PROPERTY_ID);

    Predicate predicate = new PredicateBuilder().property(GroupResourceProvider.GROUP_GROUPNAME_PROPERTY_ID).
        equals("engineering").toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      String groupName = (String) resource.getPropertyValue(GroupResourceProvider.GROUP_GROUPNAME_PROPERTY_ID);
      Assert.assertEquals("engineering", groupName);
    }

    // verify
    verify(managementController);
  }

  @Test
  public void testUpdateResources_Adminstrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_ClusterAdminstrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createClusterAdministrator("User1", 2L));
  }

  private void testUpdateResources(Authentication authentication) throws Exception {
    Resource.Type type = Resource.Type.Group;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    Set<GroupRequest> requests = AbstractResourceProviderTest.Matcher.getGroupRequestSet("engineering");

    // set expectations
    managementController.updateGroups(requests);

    // replay
    replay(managementController, response);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    Map<String, Object> properties = new LinkedHashMap<>();
    Request request = PropertyHelper.getUpdateRequest(properties, null);
    Predicate predicate = new PredicateBuilder().property(GroupResourceProvider.GROUP_GROUPNAME_PROPERTY_ID).
        equals("engineering").toPredicate();

    provider.updateResources(request, predicate);

    // verify
    verify(managementController, response);
  }

  @Test
  public void testDeleteResources_Administrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResources_ClusterAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createClusterAdministrator("User1", 2L));
  }

  private void testDeleteResources(Authentication authentication) throws Exception {
    Resource.Type type = Resource.Type.Group;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    // set expectations
    managementController.deleteGroups(AbstractResourceProviderTest.Matcher.getGroupRequestSet("engineering"));

    // replay
    replay(managementController, response);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    Predicate predicate = new PredicateBuilder().property(GroupResourceProvider.GROUP_GROUPNAME_PROPERTY_ID).
        equals("engineering").toPredicate();
    provider.deleteResources(new RequestImpl(null, null, null, null), predicate);

    // verify
    verify(managementController, response);
  }
}
