/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller.internal;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.RemoteAmbariClusterDAO;
import org.apache.ambari.server.orm.entities.RemoteAmbariClusterEntity;
import org.apache.ambari.server.orm.entities.RemoteAmbariClusterServiceEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.view.RemoteAmbariClusterRegistry;
import org.easymock.Capture;
import org.junit.After;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class RemoteClusterResourceProviderTest {

  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testToResource() throws Exception {
    RemoteClusterResourceProvider provider = new RemoteClusterResourceProvider();
    Set<String> propertyIds = new HashSet<>();
    propertyIds.add(RemoteClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(RemoteClusterResourceProvider.CLUSTER_URL_PROPERTY_ID);
    propertyIds.add(RemoteClusterResourceProvider.USERNAME_PROPERTY_ID);
    propertyIds.add(RemoteClusterResourceProvider.PASSWORD_PROPERTY_ID);
    propertyIds.add(RemoteClusterResourceProvider.SERVICES_PROPERTY_ID);

    RemoteAmbariClusterServiceEntity service1 = createNiceMock(RemoteAmbariClusterServiceEntity.class);
    expect(service1.getServiceName()).andReturn("service1").once();

    RemoteAmbariClusterServiceEntity service2 = createNiceMock(RemoteAmbariClusterServiceEntity.class);
    expect(service2.getServiceName()).andReturn("service2").once();

    List<RemoteAmbariClusterServiceEntity> serviceList = new ArrayList<>();
    serviceList.add(service1);
    serviceList.add(service2);

    RemoteAmbariClusterEntity entity = createNiceMock(RemoteAmbariClusterEntity.class);
    expect(entity.getName()).andReturn("test").once();
    expect(entity.getUrl()).andReturn("url").once();
    expect(entity.getUsername()).andReturn("user").once();
    expect(entity.getServices()).andReturn(serviceList).once();

    replay(service1, service2, entity);

    List<String> services = new ArrayList<>();
    services.add("service1");
    services.add("service2");

    Resource resource = provider.toResource(propertyIds, entity);
    assertEquals(resource.getPropertyValue(RemoteClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID), "test");
    assertEquals(resource.getPropertyValue(RemoteClusterResourceProvider.CLUSTER_URL_PROPERTY_ID), "url");
    assertEquals(resource.getPropertyValue(RemoteClusterResourceProvider.USERNAME_PROPERTY_ID), "user");
    assertEquals(resource.getPropertyValue(RemoteClusterResourceProvider.SERVICES_PROPERTY_ID), services);
    verify(service1, service2, entity);
  }

  @Test
  public void testCreateResourcesAsAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator());
  }

  static void setField(Field field, Object newValue) throws Exception {
    field.setAccessible(true);

    Field modifiersField = Field.class.getDeclaredField("modifiers");
    modifiersField.setAccessible(true);
    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    field.set(null, newValue);
  }

  private void testCreateResources(Authentication authentication) throws Exception {

    RemoteClusterResourceProvider provider = new RemoteClusterResourceProvider();

    RemoteAmbariClusterDAO clusterDAO = createMock(RemoteAmbariClusterDAO.class);
    setField(RemoteClusterResourceProvider.class.getDeclaredField("remoteAmbariClusterDAO"), clusterDAO);

    RemoteAmbariClusterRegistry clusterRegistry = createMock(RemoteAmbariClusterRegistry.class);
    setField(RemoteClusterResourceProvider.class.getDeclaredField("remoteAmbariClusterRegistry"), clusterRegistry);

    Set<Map<String, Object>> properties = new HashSet<>();
    Map<String, Object> propertyMap = new HashMap<>();
    propertyMap.put(RemoteClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID, "test");
    propertyMap.put(RemoteClusterResourceProvider.CLUSTER_URL_PROPERTY_ID, "url");
    propertyMap.put(RemoteClusterResourceProvider.USERNAME_PROPERTY_ID, "username");
    propertyMap.put(RemoteClusterResourceProvider.PASSWORD_PROPERTY_ID, "password");

    RemoteAmbariClusterServiceEntity service1 = createNiceMock(RemoteAmbariClusterServiceEntity.class);
    expect(service1.getServiceName()).andReturn("service1").once();

    RemoteAmbariClusterServiceEntity service2 = createNiceMock(RemoteAmbariClusterServiceEntity.class);
    expect(service2.getServiceName()).andReturn("service2").once();

    List<RemoteAmbariClusterServiceEntity> serviceList = new ArrayList<>();
    serviceList.add(service1);
    serviceList.add(service2);

    expect(clusterDAO.findByName("test")).andReturn(null).anyTimes();
    Capture<RemoteAmbariClusterEntity> clusterEntityCapture = newCapture();

    clusterRegistry.saveOrUpdate(capture(clusterEntityCapture),eq(false));

    replay(clusterRegistry, clusterDAO, service1, service2);

    properties.add(propertyMap);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    provider.createResources(PropertyHelper.getCreateRequest(properties, null));

    RemoteAmbariClusterEntity clusterEntity = clusterEntityCapture.getValue();
    assertEquals(clusterEntity.getName(), "test");
    assertEquals(clusterEntity.getPassword(), "password");
    assertEquals(clusterEntity.getUrl(), "url");
    assertEquals(clusterEntity.getUsername(), "username");

  }

  @Test
  public void testDeleteResources() throws Exception {
    RemoteClusterResourceProvider provider = new RemoteClusterResourceProvider();
    RemoteAmbariClusterDAO clusterDAO = createNiceMock(RemoteAmbariClusterDAO.class);
    RemoteAmbariClusterEntity clusterEntity = new RemoteAmbariClusterEntity();

    setField(RemoteClusterResourceProvider.class.getDeclaredField("remoteAmbariClusterDAO"), clusterDAO);

    EqualsPredicate equalsPredicate = new EqualsPredicate<>(RemoteClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID,"test");

    RemoteAmbariClusterRegistry clusterRegistry = createMock(RemoteAmbariClusterRegistry.class);
    setField(RemoteClusterResourceProvider.class.getDeclaredField("remoteAmbariClusterRegistry"), clusterRegistry);

    Set<Map<String, Object>> properties = new HashSet<>();
    Map<String, Object> propertyMap = new HashMap<>();
    propertyMap.put(RemoteClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID,"test");

    expect(clusterDAO.findByName("test")).andReturn(clusterEntity);
    clusterRegistry.delete(clusterEntity);

    replay(clusterDAO);

    properties.add(propertyMap);

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    provider.deleteResources(PropertyHelper.getCreateRequest(properties, null),equalsPredicate);

  }

}
