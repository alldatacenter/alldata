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
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.ConfigurationResponse;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.state.StackId;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Tests for the configuration resource provider.
 */
public class ConfigurationResourceProviderTest {

  @BeforeClass
  public static void setupAuthentication() {
    // Set authenticated user so that authorization checks will pass
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testCreateResources() throws Exception {

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    managementController.createConfiguration(AbstractResourceProviderTest.Matcher.getConfigurationRequest(
        "Cluster100", "type", "tag", new HashMap<>(), null));
    expectLastCall().andReturn(null);

    // replay
    replay(managementController, response);

    ConfigurationResourceProvider provider = new ConfigurationResourceProvider(
        managementController);

    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(ConfigurationResourceProvider.CLUSTER_NAME, "Cluster100");
    properties.put(ConfigurationResourceProvider.TAG, "tag");
    properties.put(ConfigurationResourceProvider.TYPE, "type");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    // verify
    verify(managementController, response);
  }

  @Test
  public void testCreateAttributesResources() throws Exception {

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    managementController.createConfiguration(AbstractResourceProviderTest.Matcher.getConfigurationRequest(
        "Cluster100", "type", "tag", new HashMap<String, String>(){
          {
            put("a", "b");
          }
        }, new HashMap<String, Map<String,String>>(){
          {
            put("final", new HashMap<String, String>(){
              {
                put("a", "true");
              }
            });
          }
        }));
    expectLastCall().andReturn(null);

    // replay
    replay(managementController, response);

    ConfigurationResourceProvider provider = new ConfigurationResourceProvider(
        managementController);

    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(ConfigurationResourceProvider.CLUSTER_NAME, "Cluster100");
    properties.put(ConfigurationResourceProvider.TAG, "tag");
    properties.put(ConfigurationResourceProvider.TYPE, "type");
    properties.put("properties/a", "b");
    properties.put("properties_attributes/final/a", "true");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    // verify
    verify(managementController, response);
  }

  @Test
  public void testGetResources() throws Exception {
    Resource.Type type = Resource.Type.Configuration;
    StackId stackId = new StackId("HDP", "0.1");

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    Set<ConfigurationResponse> allResponse = new HashSet<>();
    allResponse.add(new ConfigurationResponse("Cluster100", stackId, "type",
        "tag1", 1L, null, null));
    allResponse.add(new ConfigurationResponse("Cluster100", stackId, "type",
        "tag2", 2L, null, null));
    allResponse.add(new ConfigurationResponse("Cluster100", stackId, "type",
        "tag3", 3L, null, null));

    Set<ConfigurationResponse> orResponse = new HashSet<>();
    orResponse.add(new ConfigurationResponse("Cluster100", stackId, "type",
        "tag1", 1L, null, null));
    orResponse.add(new ConfigurationResponse("Cluster100", stackId, "type",
        "tag2", 2L, null, null));

    Capture<Set<ConfigurationRequest>> configRequestCapture1 = EasyMock.newCapture();
    Capture<Set<ConfigurationRequest>> configRequestCapture2 = EasyMock.newCapture();

    // set expectations
    //equals predicate
    expect(managementController.getConfigurations(
        capture(configRequestCapture1))).andReturn(allResponse).once();

    // OR predicate
    expect(managementController.getConfigurations(
        capture(configRequestCapture2))).andReturn(orResponse).once();

    // replay
    replay(managementController);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(ConfigurationResourceProvider.CLUSTER_NAME);
    propertyIds.add(ConfigurationResourceProvider.TAG);

    // equals predicate
    Predicate predicate = new PredicateBuilder().property(
        ConfigurationResourceProvider.CLUSTER_NAME).equals("Cluster100").toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Set<ConfigurationRequest> setRequest = configRequestCapture1.getValue();
    assertEquals(1, setRequest.size());
    ConfigurationRequest configRequest = setRequest.iterator().next();
    assertEquals("Cluster100", configRequest.getClusterName());
    assertNull(configRequest.getType());
    assertNull(configRequest.getVersionTag());

    Assert.assertEquals(3, resources.size());
    boolean containsResource1 = false;
    boolean containsResource2 = false;
    boolean containsResource3 = false;

    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(
          ConfigurationResourceProvider.CLUSTER_NAME);

      String stackIdProperty = (String) resource.getPropertyValue(ConfigurationResourceProvider.STACK_ID);

      Assert.assertEquals("Cluster100", clusterName);
      Assert.assertEquals(stackId.getStackId(), stackIdProperty);
      String tag = (String) resource.getPropertyValue(
          ConfigurationResourceProvider.TAG);

      if (tag.equals("tag1")) {
        containsResource1 = true;
      } else if (tag.equals("tag2")) {
        containsResource2 = true;
      } else if (tag.equals("tag3")) {
        containsResource3 = true;
      }
    }
    assertTrue(containsResource1);
    assertTrue(containsResource2);
    assertTrue(containsResource3);

    // OR predicate
    predicate = new PredicateBuilder().property(
        ConfigurationResourceProvider.TAG).equals("tag1").or().
        property(ConfigurationResourceProvider.TAG).equals("tag2").toPredicate();

    request = PropertyHelper.getReadRequest(propertyIds);
    resources = provider.getResources(request, predicate);

    setRequest = configRequestCapture2.getValue();
    assertEquals(2, setRequest.size());
    boolean containsTag1 = false;
    boolean containsTag2 = false;
    for (ConfigurationRequest cr : setRequest) {
      assertNull(cr.getClusterName());
      if (cr.getVersionTag().equals("tag1")) {
        containsTag1 = true;
      } else if (cr.getVersionTag().equals("tag2")) {
        containsTag2 = true;
      }
    }
    assertTrue(containsTag1);
    assertTrue(containsTag2);

    Assert.assertEquals(2, resources.size());
    containsResource1 = false;
    containsResource2 = false;

    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(
          ConfigurationResourceProvider.CLUSTER_NAME);
      Assert.assertEquals("Cluster100", clusterName);
      String tag = (String) resource.getPropertyValue(
          ConfigurationResourceProvider.TAG);

      if (tag.equals("tag1")) {
        containsResource1 = true;
      } else if (tag.equals("tag2")) {
        containsResource2 = true;
      }
    }
    assertTrue(containsResource1);
    assertTrue(containsResource2);

    // verify
    verify(managementController);
  }

  @Test
  public void testUpdateResources() throws Exception {
    Resource.Type type = Resource.Type.Configuration;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    // replay
    replay(managementController, response);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<>();

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    Predicate predicate = new PredicateBuilder().property(
        ConfigurationResourceProvider.TAG).equals("Configuration100").toPredicate();

    try {
      provider.updateResources(request, predicate);
      Assert.fail("Expected an UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // expected
    }

    // verify
    verify(managementController, response);
  }

  @Test
  public void testDeleteResources() throws Exception {
    Resource.Type type = Resource.Type.Configuration;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    // replay
    replay(managementController);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    Predicate predicate = new PredicateBuilder().property(
        ConfigurationResourceProvider.TAG).equals("Configuration100").toPredicate();
    try {
      provider.deleteResources(new RequestImpl(null, null, null, null), predicate);
      Assert.fail("Expected an UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // expected
    }

    // verify
    verify(managementController);
  }
}
