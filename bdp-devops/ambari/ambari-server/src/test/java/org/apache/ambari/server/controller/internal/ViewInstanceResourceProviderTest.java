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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.view.ViewRegistry;
import org.apache.ambari.server.view.configuration.ViewConfig;
import org.apache.ambari.view.ViewDefinition;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class ViewInstanceResourceProviderTest {

  private static final ViewRegistry viewregistry = createMock(ViewRegistry.class);

  @Before
  public void before() {
    ViewRegistry.initInstance(viewregistry);
    reset(viewregistry);
  }

  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testToResource() throws Exception {
    ViewInstanceResourceProvider provider = new ViewInstanceResourceProvider();
    Set<String> propertyIds = new HashSet<>();
    propertyIds.add(ViewInstanceResourceProvider.PROPERTIES);
    propertyIds.add(ViewInstanceResourceProvider.CLUSTER_HANDLE);
    ViewInstanceEntity viewInstanceEntity = createNiceMock(ViewInstanceEntity.class);
    ViewEntity viewEntity = createNiceMock(ViewEntity.class);
    expect(viewInstanceEntity.getViewEntity()).andReturn(viewEntity).anyTimes();

    Map<String, String> propertyMap = new HashMap<>();

    propertyMap.put("par1", "val1");
    propertyMap.put("par2", "val2");

    expect(viewInstanceEntity.getPropertyMap()).andReturn(propertyMap);

    expect(viewInstanceEntity.getData()).andReturn(Collections.emptyList()).anyTimes();

    expect(viewregistry.checkAdmin()).andReturn(true);
    expect(viewregistry.checkAdmin()).andReturn(false);

    expect(viewInstanceEntity.getClusterHandle()).andReturn(1L);

    replay(viewregistry, viewEntity, viewInstanceEntity);

    // as admin
    Resource resource = provider.toResource(viewInstanceEntity, propertyIds);
    Map<String, Map<String, Object>> properties = resource.getPropertiesMap();
    assertEquals(2, properties.size());
    Map<String, Object> props = properties.get("ViewInstanceInfo");
    assertNotNull(props);
    assertEquals(1, props.size());
    assertEquals(1L, props.get("cluster_handle"));

    props = properties.get("ViewInstanceInfo/properties");
    assertNotNull(props);
    assertEquals(2, props.size());
    assertEquals("val1", props.get("par1"));
    assertEquals("val2", props.get("par2"));

    // as non-admin
    resource = provider.toResource(viewInstanceEntity, propertyIds);
    properties = resource.getPropertiesMap();
    props = properties.get("ViewInstanceInfo/properties");
    assertNull(props);

    verify(viewregistry, viewEntity, viewInstanceEntity);
  }

  @Test
  public void testCreateResourcesAsAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesAsClusterAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  private void testCreateResources(Authentication authentication) throws Exception {
    ViewInstanceResourceProvider provider = new ViewInstanceResourceProvider();

    Set<Map<String, Object>> properties = new HashSet<>();

    Map<String, Object> propertyMap = new HashMap<>();

    propertyMap.put(ViewInstanceResourceProvider.VIEW_NAME, "V1");
    propertyMap.put(ViewInstanceResourceProvider.VERSION, "1.0.0");
    propertyMap.put(ViewInstanceResourceProvider.INSTANCE_NAME, "I1");
    propertyMap.put(ViewInstanceResourceProvider.PROPERTIES + "/test_property", "test_value");

    properties.add(propertyMap);

    ViewInstanceEntity viewInstanceEntity = new ViewInstanceEntity();
    viewInstanceEntity.setViewName("V1{1.0.0}");
    viewInstanceEntity.setName("I1");

    ViewInstanceEntity viewInstanceEntity2 = new ViewInstanceEntity();
    viewInstanceEntity2.setViewName("V1{1.0.0}");
    viewInstanceEntity2.setName("I1");

    ViewEntity viewEntity = new ViewEntity();
    viewEntity.setStatus(ViewDefinition.ViewStatus.DEPLOYED);
    viewEntity.setName("V1{1.0.0}");

    viewInstanceEntity.setViewEntity(viewEntity);
    viewInstanceEntity2.setViewEntity(viewEntity);

    expect(viewregistry.instanceExists(viewInstanceEntity)).andReturn(false);
    expect(viewregistry.instanceExists(viewInstanceEntity2)).andReturn(false);
    expect(viewregistry.getDefinition("V1", "1.0.0")).andReturn(viewEntity).anyTimes();
    expect(viewregistry.getDefinition("V1", null)).andReturn(viewEntity).anyTimes();

    Capture<Map<String, String>> captureProperties = EasyMock.newCapture();

    viewregistry.setViewInstanceProperties(eq(viewInstanceEntity), capture(captureProperties),
        anyObject(ViewConfig.class), anyObject(ClassLoader.class));

    Capture<ViewInstanceEntity> instanceEntityCapture = EasyMock.newCapture();
    viewregistry.installViewInstance(capture(instanceEntityCapture));
    expectLastCall().anyTimes();

    expect(viewregistry.checkAdmin()).andReturn(true);
    expect(viewregistry.checkAdmin()).andReturn(false);

    replay(viewregistry);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    // as admin
    provider.createResources(PropertyHelper.getCreateRequest(properties, null));
    assertEquals(viewInstanceEntity, instanceEntityCapture.getValue());
    Map<String, String> props = captureProperties.getValue();
    assertEquals(1, props.size());
    assertEquals("test_value", props.get("test_property"));

    // as non-admin
    provider.createResources(PropertyHelper.getCreateRequest(properties, null));
    assertEquals(viewInstanceEntity2, instanceEntityCapture.getValue());
    props = viewInstanceEntity2.getPropertyMap();
    assertTrue(props.isEmpty());

    verify(viewregistry);
  }

  @Test
  public void testCreateResources_existingInstance() throws Exception {
    ViewInstanceResourceProvider provider = new ViewInstanceResourceProvider();

    Set<Map<String, Object>> properties = new HashSet<>();

    Map<String, Object> propertyMap = new HashMap<>();

    propertyMap.put(ViewInstanceResourceProvider.VIEW_NAME, "V1");
    propertyMap.put(ViewInstanceResourceProvider.VERSION, "1.0.0");
    propertyMap.put(ViewInstanceResourceProvider.INSTANCE_NAME, "I1");

    properties.add(propertyMap);

    ViewInstanceEntity viewInstanceEntity = new ViewInstanceEntity();
    viewInstanceEntity.setViewName("V1{1.0.0}");
    viewInstanceEntity.setName("I1");

    ViewEntity viewEntity = new ViewEntity();
    viewEntity.setStatus(ViewDefinition.ViewStatus.DEPLOYED);
    viewEntity.setName("V1{1.0.0}");

    viewInstanceEntity.setViewEntity(viewEntity);

    expect(viewregistry.instanceExists(viewInstanceEntity)).andReturn(true);
    expect(viewregistry.getDefinition("V1", "1.0.0")).andReturn(viewEntity).anyTimes();
    expect(viewregistry.getDefinition("V1", null)).andReturn(viewEntity);

    expect(viewregistry.checkAdmin()).andReturn(true);

    replay(viewregistry);

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    try {
      provider.createResources(PropertyHelper.getCreateRequest(properties, null));
      fail("Expected ResourceAlreadyExistsException.");
    } catch (ResourceAlreadyExistsException e) {
      // expected
    }

    verify(viewregistry);
  }

  @Test
  public void testCreateResources_viewNotLoaded() throws Exception {
    ViewInstanceResourceProvider provider = new ViewInstanceResourceProvider();

    Set<Map<String, Object>> properties = new HashSet<>();

    Map<String, Object> propertyMap = new HashMap<>();

    propertyMap.put(ViewInstanceResourceProvider.VIEW_NAME, "V1");
    propertyMap.put(ViewInstanceResourceProvider.VERSION, "1.0.0");
    propertyMap.put(ViewInstanceResourceProvider.INSTANCE_NAME, "I1");

    properties.add(propertyMap);

    ViewEntity viewEntity = new ViewEntity();
    viewEntity.setName("V1{1.0.0}");
    viewEntity.setStatus(ViewDefinition.ViewStatus.DEPLOYING);
    ViewInstanceEntity viewInstanceEntity = new ViewInstanceEntity();
    viewInstanceEntity.setViewName("V1{1.0.0}");
    viewInstanceEntity.setName("I1");
    viewInstanceEntity.setViewEntity(viewEntity);

    expect(viewregistry.getDefinition("V1", "1.0.0")).andReturn(viewEntity).anyTimes();
    expect(viewregistry.getDefinition("V1", null)).andReturn(viewEntity);

    expect(viewregistry.checkAdmin()).andReturn(true);

    replay(viewregistry);

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    try {
      provider.createResources(PropertyHelper.getCreateRequest(properties, null));
      fail("Expected IllegalStateException.");
    } catch (IllegalStateException e) {
      // expected
    }

    verify(viewregistry);
  }



  @Test
  public void testUpdateResources_viewNotLoaded() throws Exception {
    ViewInstanceResourceProvider provider = new ViewInstanceResourceProvider();

    Set<Map<String, Object>> properties = new HashSet<>();

    Map<String, Object> propertyMap = new HashMap<>();

    propertyMap.put(ViewInstanceResourceProvider.ICON_PATH, "path");

    properties.add(propertyMap);

    PredicateBuilder predicateBuilder = new PredicateBuilder();
    Predicate predicate =
        predicateBuilder.property(ViewInstanceResourceProvider.VIEW_NAME).equals("V1").toPredicate();
    ViewEntity viewEntity = new ViewEntity();
    viewEntity.setName("V1{1.0.0}");
    viewEntity.setStatus(ViewDefinition.ViewStatus.DEPLOYING);
    ViewInstanceEntity viewInstanceEntity = new ViewInstanceEntity();
    viewInstanceEntity.setViewName("V1{1.0.0}");
    viewInstanceEntity.setName("I1");
    viewInstanceEntity.setViewEntity(viewEntity);

    expect(viewregistry.getDefinitions()).andReturn(Collections.singleton(viewEntity));

    replay(viewregistry);

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    provider.updateResources(PropertyHelper.getCreateRequest(properties, null), predicate);

    Assert.assertNull(viewInstanceEntity.getIcon());

    verify(viewregistry);
  }

  @Test
  public void testDeleteResourcesAsAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResourcesAsClusterAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  private void testDeleteResources(Authentication authentication) throws Exception {
    ViewInstanceResourceProvider provider = new ViewInstanceResourceProvider();

    PredicateBuilder predicateBuilder = new PredicateBuilder();
    Predicate predicate =
        predicateBuilder.property(ViewInstanceResourceProvider.VIEW_NAME).equals("V1").toPredicate();

    ViewEntity viewEntity = new ViewEntity();
    viewEntity.setName("V1{1.0.0}");
    viewEntity.setStatus(ViewDefinition.ViewStatus.DEPLOYING);
    ViewInstanceEntity viewInstanceEntity = new ViewInstanceEntity();
    viewInstanceEntity.setViewName("V1{1.0.0}");
    viewInstanceEntity.setName("I1");
    viewInstanceEntity.setViewEntity(viewEntity);

    expect(viewregistry.getDefinitions()).andReturn(Collections.singleton(viewEntity));

    replay(viewregistry);

    SecurityContextHolder.getContext().setAuthentication(authentication);
    provider.deleteResources(new RequestImpl(null, null, null, null), predicate);

    verify(viewregistry);
  }
}
