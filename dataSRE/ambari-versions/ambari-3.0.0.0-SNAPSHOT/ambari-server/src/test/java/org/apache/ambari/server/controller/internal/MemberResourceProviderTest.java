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
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.ResourceProviderFactory;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * MemberResourceProvider tests.
 */
public class MemberResourceProviderTest {

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
    Resource.Type type = Resource.Type.Member;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    expect(resourceProviderFactory.getMemberResourceProvider(eq(managementController)))
        .andReturn(new MemberResourceProvider(managementController)).anyTimes();

    managementController.createMembers(AbstractResourceProviderTest.Matcher.getMemberRequestSet("engineering", "joe"));
    expectLastCall().atLeastOnce();

    // replay
    replay(managementController, response, resourceProviderFactory);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    Map<String, Object> properties = new LinkedHashMap<>();

    // add properties to the request map
    properties.put(MemberResourceProvider.MEMBER_GROUP_NAME_PROPERTY_ID, "engineering");
    properties.put(MemberResourceProvider.MEMBER_USER_NAME_PROPERTY_ID, "joe");

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

  private void testGetResources(Authentication authentication) throws Exception {
    Resource.Type type = Resource.Type.Member;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    expect(resourceProviderFactory.getMemberResourceProvider(eq(managementController)))
        .andReturn(new MemberResourceProvider(managementController)).anyTimes();

    expect(managementController.getMembers(AbstractResourceProviderTest.Matcher.getMemberRequestSet(null, null)))
        .andReturn(Collections.emptySet())
        .atLeastOnce();

    // replay
    replay(managementController, response, resourceProviderFactory);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    // create the request
    Request request = PropertyHelper.getReadRequest(null, null);
    Predicate predicate = new PredicateBuilder().property(GroupResourceProvider.GROUP_GROUPNAME_PROPERTY_ID).
        equals("engineering").toPredicate();

    provider.getResources(request, predicate);

    // verify
    verify(managementController, response);
  }

  @Test
  public void testUpdateResources_Administrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_ClusterAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createClusterAdministrator("User1", 2L));
  }

  private void testUpdateResources(Authentication authentication) throws Exception {
    Resource.Type type = Resource.Type.Member;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    // set expectations
    expect(resourceProviderFactory.getMemberResourceProvider(eq(managementController)))
        .andReturn(new MemberResourceProvider(managementController)).anyTimes();

    managementController.updateMembers(AbstractResourceProviderTest.Matcher.getMemberRequestSet("engineering", "joe"));
    expectLastCall().atLeastOnce();

    // replay
    replay(managementController, response, resourceProviderFactory);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(MemberResourceProvider.MEMBER_GROUP_NAME_PROPERTY_ID, "engineering");
    properties.put(MemberResourceProvider.MEMBER_USER_NAME_PROPERTY_ID, "joe");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    PredicateBuilder builder = new PredicateBuilder();
    builder.property(MemberResourceProvider.MEMBER_GROUP_NAME_PROPERTY_ID).equals("engineering");
    Predicate predicate = builder.toPredicate();
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
    Resource.Type type = Resource.Type.Member;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);
    ResourceProviderFactory resourceProviderFactory = createNiceMock(ResourceProviderFactory.class);

    AbstractControllerResourceProvider.init(resourceProviderFactory);

    // set expectations
    expect(resourceProviderFactory.getMemberResourceProvider(eq(managementController)))
        .andReturn(new MemberResourceProvider(managementController)).anyTimes();

    managementController.deleteMembers(AbstractResourceProviderTest.Matcher.getMemberRequestSet("engineering", null));
    expectLastCall().atLeastOnce();

    // replay
    replay(managementController, response, resourceProviderFactory);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    PredicateBuilder builder = new PredicateBuilder();
    builder.property(MemberResourceProvider.MEMBER_GROUP_NAME_PROPERTY_ID).equals("engineering");
    Predicate predicate = builder.toPredicate();
    provider.deleteResources(new RequestImpl(null, null, null, null), predicate);

    // verify
    verify(managementController, response);
  }
}
