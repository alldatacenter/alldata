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

import static org.apache.ambari.server.controller.internal.SettingResourceProvider.SETTING_CONTENT_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.SettingResourceProvider.SETTING_NAME_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.SettingResourceProvider.SETTING_SETTING_TYPE_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.SettingResourceProvider.SETTING_UPDATED_BY_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.SettingResourceProvider.SETTING_UPDATE_TIMESTAMP_PROPERTY_ID;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.SettingDAO;
import org.apache.ambari.server.orm.entities.SettingEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.commons.lang.RandomStringUtils;
import org.easymock.Capture;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.common.collect.Lists;

public class SettingResourceProviderTest {
  IMocksControl mockControl;
  SettingDAO dao;
  SettingResourceProvider resourceProvider;


  @Before
  public void setUp() throws Exception {
    mockControl = createControl();
    dao = mockControl.createMock(SettingDAO.class);
    resourceProvider = new SettingResourceProvider();
    setPrivateField(resourceProvider, "dao", dao);
  }

  @After
  public void tearDown() {
    mockControl.verify();
    SecurityContextHolder.getContext().setAuthentication(null);
    mockControl.reset();
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResources_instance_noAuth() throws Exception {
    getResources_instance(newEntity("motd"), readRequest());
  }

  @Test
  public void testGetResources_instance_clusterUser() throws Exception {
    setupAuthenticationForClusterUser();
    String name = "motd";
    SettingEntity entity = newEntity(name);

    Set<Resource> response = getResources_instance(entity, readRequest());
    assertEquals(1, response.size());
    Resource resource = response.iterator().next();
    assertEqualsEntityAndResource(entity, resource);
  }

  @Test
  public void testGetResources_instance_admin() throws Exception {
    setupAuthenticationForAdmin();
    SettingEntity entity = newEntity("motd");
    Set<Resource> response = getResources_instance(entity, readRequest());
    assertEquals(1, response.size());
    Resource resource = response.iterator().next();
    assertEqualsEntityAndResource(entity, resource);
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResources_collection_noAuth() throws Exception {
    mockControl.replay();
    Request request = PropertyHelper.getReadRequest(
            SETTING_NAME_PROPERTY_ID,
            SETTING_CONTENT_PROPERTY_ID,
            SETTING_SETTING_TYPE_PROPERTY_ID,
            SETTING_UPDATED_BY_PROPERTY_ID,
            SETTING_UPDATE_TIMESTAMP_PROPERTY_ID);
    resourceProvider.getResources(request, null);
  }

  @Test
  public void testGetResources_collection_clusterUser() throws Exception {
    setupAuthenticationForClusterUser();

    SettingEntity entity1 = newEntity("motd");
    SettingEntity entity2 = newEntity("ldap");
    Request request = PropertyHelper.getReadRequest(
            SETTING_NAME_PROPERTY_ID,
            SETTING_CONTENT_PROPERTY_ID,
            SETTING_SETTING_TYPE_PROPERTY_ID,
            SETTING_UPDATED_BY_PROPERTY_ID,
            SETTING_UPDATE_TIMESTAMP_PROPERTY_ID);

    expect(dao.findAll()).andReturn(Lists.newArrayList(entity1, entity2));
    mockControl.replay();

    Set<Resource> response = resourceProvider.getResources(request, null);
    assertEquals(2, response.size());
    Map<Object, Resource> resourceMap = new HashMap<>();
    Iterator<Resource> resourceIterator = response.iterator();
    Resource nextResource = resourceIterator.next();
    resourceMap.put(nextResource.getPropertyValue(SETTING_NAME_PROPERTY_ID), nextResource);
    nextResource = resourceIterator.next();
    resourceMap.put(nextResource.getPropertyValue(SETTING_NAME_PROPERTY_ID), nextResource);
    assertEqualsEntityAndResource(entity1, resourceMap.get(entity1.getName()));
    assertEqualsEntityAndResource(entity2, resourceMap.get(entity2.getName()));
  }

  @Test
  public void testGetResources_collection_admin() throws Exception {
    setupAuthenticationForAdmin();

    SettingEntity entity1 = newEntity("motd");
    SettingEntity entity2 = newEntity("ldap");
    Request request = PropertyHelper.getReadRequest(
            SETTING_NAME_PROPERTY_ID,
            SETTING_CONTENT_PROPERTY_ID,
            SETTING_SETTING_TYPE_PROPERTY_ID,
            SETTING_UPDATED_BY_PROPERTY_ID,
            SETTING_UPDATE_TIMESTAMP_PROPERTY_ID);
    expect(dao.findAll()).andReturn(Lists.newArrayList(entity1, entity2));
    mockControl.replay();

    Set<Resource> response = resourceProvider.getResources(request, null);
    assertEquals(2, response.size());
    Map<Object, Resource> resourceMap = new HashMap<>();
    Iterator<Resource> resourceIterator = response.iterator();
    Resource nextResource = resourceIterator.next();
    resourceMap.put(nextResource.getPropertyValue(SETTING_NAME_PROPERTY_ID), nextResource);
    nextResource = resourceIterator.next();
    resourceMap.put(nextResource.getPropertyValue(SETTING_NAME_PROPERTY_ID), nextResource);
    assertEqualsEntityAndResource(entity1, resourceMap.get(entity1.getName()));
    assertEqualsEntityAndResource(entity2, resourceMap.get(entity2.getName()));
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResource_noAuth() throws Exception {
    mockControl.replay();
    resourceProvider.createResources(createRequest(newEntity("moted")));
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResource_clusterUser() throws Exception {
    setupAuthenticationForClusterUser();
    mockControl.replay();
    resourceProvider.createResources(createRequest(newEntity("motd")));
  }

  @Test
  public void testCreateResource_admin() throws Exception {
    setupAuthenticationForAdmin();
    SettingEntity entity = newEntity("motd");
    Capture<SettingEntity> entityCapture = Capture.newInstance();
    Request request = createRequest(entity);

    expect(dao.findByName(entity.getName())).andReturn(null);
    dao.create(capture(entityCapture));
    mockControl.replay();

    RequestStatus response = resourceProvider.createResources(request);
    assertEquals(RequestStatus.Status.Complete, response.getStatus());
    Set<Resource> associatedResources = response.getAssociatedResources();
    assertEquals(1, associatedResources.size());
    SettingEntity capturedEntity = entityCapture.getValue();
    assertEquals(entity.getName(), capturedEntity.getName());
    assertEquals(entity.getContent(), capturedEntity.getContent());
    assertEquals(entity.getSettingType(), capturedEntity.getSettingType());
    assertEquals(AuthorizationHelper.getAuthenticatedName(), capturedEntity.getUpdatedBy());
  }

  @Test(expected = ResourceAlreadyExistsException.class)
  public void testCreateDuplicateResource() throws Exception {
    setupAuthenticationForAdmin();
    SettingEntity entity = newEntity("motd");
    Request request = createRequest(entity);

    expect(dao.findByName(entity.getName())).andReturn(entity);
    mockControl.replay();
    resourceProvider.createResources(request);
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_noAuth() throws Exception {
    mockControl.replay();
    resourceProvider.updateResources(updateRequest(newEntity("motd")), null);
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_clusterUser() throws Exception {
    setupAuthenticationForClusterUser();
    mockControl.replay();
    resourceProvider.updateResources(updateRequest(newEntity("motd")), null);
  }

  @Test
  public void testUpdateResources_admin() throws Exception {
    setupAuthenticationForAdmin();
    String name = "motd";
    SettingEntity oldEntity = newEntity(name);
    SettingEntity updatedEntity = oldEntity.clone();
    updatedEntity.setContent("{text}");
    updatedEntity.setSettingType("new-type");

    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate = pb.begin().property(SETTING_NAME_PROPERTY_ID).equals(name).end().toPredicate();
    Capture<SettingEntity> capture = Capture.newInstance();

    expect(dao.findByName(name)).andReturn(oldEntity);
    expect(dao.merge(capture(capture))).andReturn(updatedEntity);
    mockControl.replay();

    RequestStatus response = resourceProvider.updateResources(updateRequest(updatedEntity), predicate);
    SettingEntity capturedEntity = capture.getValue();
    assertEquals(RequestStatus.Status.Complete, response.getStatus());
    assertEquals(updatedEntity.getId(), capturedEntity.getId());
    assertEquals(updatedEntity.getName(), capturedEntity.getName());
    assertEquals(updatedEntity.getSettingType(), capturedEntity.getSettingType());
    assertEquals(updatedEntity.getContent(), capturedEntity.getContent());
    assertEquals(AuthorizationHelper.getAuthenticatedName(), capturedEntity.getUpdatedBy());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResources_noAuth() throws Exception {
    mockControl.replay();
    resourceProvider.deleteResources(new RequestImpl(null, null, null, null), null);
  }


  @Test(expected = AuthorizationException.class)
  public void testDeleteResources_clusterUser() throws Exception {
    setupAuthenticationForClusterUser();
    mockControl.replay();
    resourceProvider.deleteResources(new RequestImpl(null, null, null, null), null);
  }

  @Test
  public void testDeleteResources() throws Exception {
    setupAuthenticationForAdmin();

    String name = "motd";
    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate = pb.begin().property(SETTING_NAME_PROPERTY_ID).equals(name).end().toPredicate();
    dao.removeByName(name);
    mockControl.replay();
    resourceProvider.deleteResources(new RequestImpl(null, null, null, null), predicate);
  }

  private Set<Resource> getResources_instance(SettingEntity entity, Request request) throws Exception {
    String name = entity.getName();
    PredicateBuilder pb = new PredicateBuilder();
    Predicate predicate = pb.begin().property(SETTING_NAME_PROPERTY_ID).equals(name).end().toPredicate();

    expect(dao.findByName(name)).andReturn(entity).anyTimes();
    mockControl.replay();
    return resourceProvider.getResources(request, predicate);
  }


  private Request readRequest() {
    return PropertyHelper.getReadRequest(
            SETTING_NAME_PROPERTY_ID,
            SETTING_CONTENT_PROPERTY_ID,
            SETTING_SETTING_TYPE_PROPERTY_ID,
            SETTING_UPDATED_BY_PROPERTY_ID,
            SETTING_UPDATE_TIMESTAMP_PROPERTY_ID);
  }

  private Request createRequest(SettingEntity entity) {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SETTING_NAME_PROPERTY_ID, entity.getName());
    properties.put(SETTING_CONTENT_PROPERTY_ID, entity.getContent());
    properties.put(SETTING_UPDATED_BY_PROPERTY_ID, entity.getUpdatedBy());
    properties.put(SETTING_SETTING_TYPE_PROPERTY_ID, entity.getSettingType());
    return PropertyHelper.getCreateRequest(Collections.singleton(properties), null);
  }

  private Request updateRequest(SettingEntity entity) {
    Map<String, Object> properties = new HashMap<>();
    properties.put(SETTING_NAME_PROPERTY_ID, entity.getName());
    properties.put(SETTING_CONTENT_PROPERTY_ID, entity.getContent());
    properties.put(SETTING_SETTING_TYPE_PROPERTY_ID, entity.getSettingType());
    return PropertyHelper.getUpdateRequest(properties, null);
  }

  private void setupAuthenticationForClusterUser() {
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createClusterUser());
  }

  private void setupAuthenticationForAdmin() {
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());
  }

  private SettingEntity newEntity(String name) {
    SettingEntity entity = new SettingEntity();
    entity.setName(name);
    entity.setContent(RandomStringUtils.randomAlphabetic(10));
    entity.setSettingType(RandomStringUtils.randomAlphabetic(5));
    entity.setUpdatedBy("ambari");
    entity.setUpdateTimestamp(System.currentTimeMillis());
    return entity;
  }

  private void assertEqualsEntityAndResource(SettingEntity entity, Resource resource) {
    assertEquals(entity.getName(), resource.getPropertyValue(SETTING_NAME_PROPERTY_ID));
    assertEquals(entity.getSettingType(), resource.getPropertyValue(SETTING_SETTING_TYPE_PROPERTY_ID));
    assertEquals(entity.getContent(), resource.getPropertyValue(SETTING_CONTENT_PROPERTY_ID));
    assertEquals(entity.getUpdatedBy(), resource.getPropertyValue(SETTING_UPDATED_BY_PROPERTY_ID));
    assertEquals(entity.getUpdateTimestamp(), resource.getPropertyValue(SETTING_UPDATE_TIMESTAMP_PROPERTY_ID));
  }

  private void setPrivateField(Object o, String field, Object value) throws Exception{
    Class<?> c = o.getClass();
    Field f = c.getDeclaredField(field);
    f.setAccessible(true);
    f.set(o, value);
  }
}
