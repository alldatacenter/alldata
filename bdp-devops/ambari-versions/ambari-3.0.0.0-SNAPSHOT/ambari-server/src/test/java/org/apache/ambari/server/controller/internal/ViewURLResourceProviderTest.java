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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.ViewURLDAO;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.orm.entities.ViewURLEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.view.ViewRegistry;
import org.easymock.Capture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.common.base.Optional;

public class ViewURLResourceProviderTest {

  private static final ViewRegistry viewregistry = createMock(ViewRegistry.class);

  @Before
  public void before() throws Exception {
    ViewRegistry.initInstance(viewregistry);
    reset(viewregistry);
  }

  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testToResource() throws Exception {
    ViewURLResourceProvider provider = new ViewURLResourceProvider();
    Set<String> propertyIds = new HashSet<>();
    propertyIds.add(ViewURLResourceProvider.URL_NAME);
    propertyIds.add(ViewURLResourceProvider.URL_SUFFIX);
    ViewURLEntity viewURLEntity = createNiceMock(ViewURLEntity.class);

    ViewEntity viewEntity = createNiceMock(ViewEntity.class);
    ViewInstanceEntity viewInstanceEntity = createNiceMock(ViewInstanceEntity.class);
    expect(viewURLEntity.getUrlName()).andReturn("test").once();
    expect(viewURLEntity.getUrlSuffix()).andReturn("url").once();
    expect(viewURLEntity.getViewInstanceEntity()).andReturn(viewInstanceEntity).once();
    expect(viewInstanceEntity.getViewEntity()).andReturn(viewEntity).once();
    expect(viewEntity.getCommonName()).andReturn("FILES").once();
    expect(viewEntity.getVersion()).andReturn("1.0.0").once();
    expect(viewInstanceEntity.getName()).andReturn("test").once();

    replay(viewURLEntity, viewEntity, viewInstanceEntity);

    Resource resource = provider.toResource(viewURLEntity);
    assertEquals(resource.getPropertyValue(ViewURLResourceProvider.URL_NAME),"test");
    assertEquals(resource.getPropertyValue(ViewURLResourceProvider.URL_SUFFIX),"url");
    assertEquals(resource.getPropertyValue(ViewURLResourceProvider.VIEW_INSTANCE_NAME),"test");
    assertEquals(resource.getPropertyValue(ViewURLResourceProvider.VIEW_INSTANCE_VERSION),"1.0.0");
    assertEquals(resource.getPropertyValue(ViewURLResourceProvider.VIEW_INSTANCE_COMMON_NAME),"FILES");
    verify(viewURLEntity,viewInstanceEntity,viewEntity);
  }


  @Test
  public void testCreateResourcesAsAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator());
  }



  static void setDao(Field field, Object newValue) throws Exception {
    field.setAccessible(true);

    Field modifiersField = Field.class.getDeclaredField("modifiers");
    modifiersField.setAccessible(true);
    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    field.set(null, newValue);
  }

  private void testCreateResources(Authentication authentication) throws Exception {
    ViewInstanceEntity viewInstanceEntity = createNiceMock(ViewInstanceEntity.class);
    ViewEntity viewEntity = createNiceMock(ViewEntity.class);
    ViewURLResourceProvider provider = new ViewURLResourceProvider();

    ViewURLDAO viewURLDAO = createNiceMock(ViewURLDAO.class);
    setDao(ViewURLResourceProvider.class.getDeclaredField("viewURLDAO"), viewURLDAO);
    Set<Map<String, Object>> properties = new HashSet<>();
    Map<String, Object> propertyMap = new HashMap<>();
    propertyMap.put(ViewURLResourceProvider.URL_NAME,"test");
    propertyMap.put(ViewURLResourceProvider.URL_SUFFIX,"suffix");
    propertyMap.put(ViewURLResourceProvider.VIEW_INSTANCE_COMMON_NAME,"FILES");
    propertyMap.put(ViewURLResourceProvider.VIEW_INSTANCE_NAME,"test");
    propertyMap.put(ViewURLResourceProvider.VIEW_INSTANCE_VERSION,"1.0.0");

    expect(viewregistry.getInstanceDefinition("FILES","1.0.0","test")).andReturn(viewInstanceEntity);
    expect(viewregistry.getDefinition("FILES","1.0.0")).andReturn(viewEntity);
    expect(viewInstanceEntity.getViewEntity()).andReturn(viewEntity).once();
    expect(viewEntity.getCommonName()).andReturn("FILES").once();
    expect(viewEntity.isDeployed()).andReturn(true).once();
    expect(viewEntity.getVersion()).andReturn("1.0.0").once();
    expect(viewInstanceEntity.getName()).andReturn("test").once();
    expect(viewInstanceEntity.getViewUrl()).andReturn(null).once();
    expect(viewURLDAO.findByName("test")).andReturn(Optional.absent());
    expect(viewURLDAO.findBySuffix("suffix")).andReturn(Optional.absent());
    Capture<ViewURLEntity> urlEntityCapture = newCapture();
    viewURLDAO.save(capture(urlEntityCapture));
    viewregistry.updateViewInstance(viewInstanceEntity);
    viewregistry.updateView(viewInstanceEntity);
    replay(viewregistry,viewEntity,viewInstanceEntity,viewURLDAO);

    properties.add(propertyMap);

    SecurityContextHolder.getContext().setAuthentication(authentication);

    provider.createResources(PropertyHelper.getCreateRequest(properties, null));

    ViewURLEntity urlEntity = urlEntityCapture.getValue();
    assertEquals(urlEntity.getUrlName(),"test");
    assertEquals(urlEntity.getUrlSuffix(),"suffix");
    assertEquals(urlEntity.getViewInstanceEntity(),viewInstanceEntity);

  }

  @Test(expected = org.apache.ambari.server.controller.spi.SystemException.class)
  public void testCreateResources_existingUrlName() throws Exception {
    ViewInstanceEntity viewInstanceEntity = createNiceMock(ViewInstanceEntity.class);
    ViewEntity viewEntity = createNiceMock(ViewEntity.class);
    ViewURLResourceProvider provider = new ViewURLResourceProvider();

    ViewURLDAO viewURLDAO = createNiceMock(ViewURLDAO.class);
    setDao(ViewURLResourceProvider.class.getDeclaredField("viewURLDAO"), viewURLDAO);
    Set<Map<String, Object>> properties = new HashSet<>();
    Map<String, Object> propertyMap = new HashMap<>();
    propertyMap.put(ViewURLResourceProvider.URL_NAME,"test");
    propertyMap.put(ViewURLResourceProvider.URL_SUFFIX,"suffix");
    propertyMap.put(ViewURLResourceProvider.VIEW_INSTANCE_COMMON_NAME,"FILES");
    propertyMap.put(ViewURLResourceProvider.VIEW_INSTANCE_NAME,"test");
    propertyMap.put(ViewURLResourceProvider.VIEW_INSTANCE_VERSION,"1.0.0");

    expect(viewregistry.getInstanceDefinition("FILES","1.0.0","test")).andReturn(viewInstanceEntity);
    expect(viewregistry.getDefinition("FILES","1.0.0")).andReturn(viewEntity);
    expect(viewInstanceEntity.getViewEntity()).andReturn(viewEntity).once();
    expect(viewEntity.getCommonName()).andReturn("FILES").once();
    expect(viewEntity.isDeployed()).andReturn(true).once();
    expect(viewEntity.getVersion()).andReturn("1.0.0").once();
    expect(viewInstanceEntity.getName()).andReturn("test").once();
    expect(viewInstanceEntity.getViewUrl()).andReturn(null).once();
    expect(viewURLDAO.findByName("test")).andReturn(Optional.of(new ViewURLEntity()));
    replay(viewregistry,viewEntity,viewInstanceEntity,viewURLDAO);
    properties.add(propertyMap);
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());
    provider.createResources(PropertyHelper.getCreateRequest(properties, null));

  }

  @Test(expected = org.apache.ambari.server.controller.spi.SystemException.class)
  public void testCreateResources_existingUrlSuffix() throws Exception {
    ViewInstanceEntity viewInstanceEntity = createNiceMock(ViewInstanceEntity.class);
    ViewEntity viewEntity = createNiceMock(ViewEntity.class);
    ViewURLResourceProvider provider = new ViewURLResourceProvider();

    ViewURLDAO viewURLDAO = createNiceMock(ViewURLDAO.class);
    setDao(ViewURLResourceProvider.class.getDeclaredField("viewURLDAO"), viewURLDAO);
    Set<Map<String, Object>> properties = new HashSet<>();
    Map<String, Object> propertyMap = new HashMap<>();
    propertyMap.put(ViewURLResourceProvider.URL_NAME,"test");
    propertyMap.put(ViewURLResourceProvider.URL_SUFFIX,"suffix");
    propertyMap.put(ViewURLResourceProvider.VIEW_INSTANCE_COMMON_NAME,"FILES");
    propertyMap.put(ViewURLResourceProvider.VIEW_INSTANCE_NAME,"test");
    propertyMap.put(ViewURLResourceProvider.VIEW_INSTANCE_VERSION,"1.0.0");

    expect(viewregistry.getInstanceDefinition("FILES","1.0.0","test")).andReturn(viewInstanceEntity);
    expect(viewregistry.getDefinition("FILES","1.0.0")).andReturn(viewEntity);
    expect(viewInstanceEntity.getViewEntity()).andReturn(viewEntity).once();
    expect(viewEntity.getCommonName()).andReturn("FILES").once();
    expect(viewEntity.isDeployed()).andReturn(true).once();
    expect(viewEntity.getVersion()).andReturn("1.0.0").once();
    expect(viewInstanceEntity.getName()).andReturn("test").once();
    expect(viewInstanceEntity.getViewUrl()).andReturn(null).once();
    expect(viewURLDAO.findByName("test")).andReturn(Optional.absent());
    expect(viewURLDAO.findBySuffix("suffix")).andReturn(Optional.of(new ViewURLEntity()));
    replay(viewregistry,viewEntity,viewInstanceEntity,viewURLDAO);
    properties.add(propertyMap);
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());
    provider.createResources(PropertyHelper.getCreateRequest(properties, null));
  }


  @Test
  public void testUpdateResources() throws Exception {
    ViewInstanceEntity viewInstanceEntity = createNiceMock(ViewInstanceEntity.class);
    ViewEntity viewEntity = createNiceMock(ViewEntity.class);
    ViewURLResourceProvider provider = new ViewURLResourceProvider();
    ViewURLEntity viewURLEntity = createNiceMock(ViewURLEntity.class);

    ViewURLDAO viewURLDAO = createNiceMock(ViewURLDAO.class);
    setDao(ViewURLResourceProvider.class.getDeclaredField("viewURLDAO"), viewURLDAO);
    Set<Map<String, Object>> properties = new HashSet<>();
    Map<String, Object> propertyMap = new HashMap<>();
    propertyMap.put(ViewURLResourceProvider.URL_NAME,"test");
    propertyMap.put(ViewURLResourceProvider.URL_SUFFIX,"suffix2");

    expect(viewURLDAO.findByName("test")).andReturn(Optional.of(viewURLEntity));
    expect(viewURLEntity.getViewInstanceEntity()).andReturn(viewInstanceEntity).once();
    expect(viewURLEntity.getUrlName()).andReturn("test").once();
    expect(viewURLEntity.getUrlSuffix()).andReturn("suffix2").once();
    expect(viewURLEntity.getViewInstanceEntity()).andReturn(viewInstanceEntity).once();
    viewURLEntity.setUrlSuffix("suffix2");
    Capture<ViewURLEntity> urlEntityCapture = newCapture();
    viewURLDAO.update(capture(urlEntityCapture));
    viewregistry.updateViewInstance(viewInstanceEntity);
    viewregistry.updateView(viewInstanceEntity);
    replay(viewregistry,viewEntity,viewInstanceEntity,viewURLDAO,viewURLEntity);

    properties.add(propertyMap);

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    PredicateBuilder predicateBuilder = new PredicateBuilder();
    Predicate predicate =
            predicateBuilder.property(ViewURLResourceProvider.URL_NAME).equals("test").toPredicate();

    provider.updateResources(PropertyHelper.getCreateRequest(properties, null),predicate);

    ViewURLEntity urlEntity = urlEntityCapture.getValue();
    assertEquals(urlEntity.getUrlName(),"test");
    assertEquals(urlEntity.getUrlSuffix(),"suffix2");
    assertEquals(urlEntity.getViewInstanceEntity(),viewInstanceEntity);

  }

  @Test
  public void testDeleteResources() throws Exception {
    ViewInstanceEntity viewInstanceEntity = createNiceMock(ViewInstanceEntity.class);
    ViewEntity viewEntity = createNiceMock(ViewEntity.class);
    ViewURLResourceProvider provider = new ViewURLResourceProvider();
    ViewURLEntity viewURLEntity = createNiceMock(ViewURLEntity.class);
    ViewURLDAO viewURLDAO = createNiceMock(ViewURLDAO.class);
    EqualsPredicate<String> equalsPredicate = new EqualsPredicate<>(ViewURLResourceProvider.URL_NAME,"test");


    setDao(ViewURLResourceProvider.class.getDeclaredField("viewURLDAO"), viewURLDAO);
    Set<Map<String, Object>> properties = new HashSet<>();
    Map<String, Object> propertyMap = new HashMap<>();
    propertyMap.put(ViewURLResourceProvider.URL_NAME,"test");
    propertyMap.put(ViewURLResourceProvider.URL_SUFFIX,"suffix");

    expect(viewURLDAO.findByName("test")).andReturn(Optional.of(viewURLEntity));
    expect(viewURLEntity.getViewInstanceEntity()).andReturn(viewInstanceEntity).once();
    expect(viewURLEntity.getUrlName()).andReturn("test").once();
    expect(viewURLEntity.getUrlSuffix()).andReturn("suffix").once();
    expect(viewURLEntity.getViewInstanceEntity()).andReturn(viewInstanceEntity).once();
    viewURLEntity.setUrlSuffix("suffix");
    Capture<ViewURLEntity> urlEntityCapture = newCapture();
    viewInstanceEntity.clearUrl();
    viewURLEntity.clearEntity();
    viewURLDAO.delete(capture(urlEntityCapture));
    viewregistry.updateViewInstance(viewInstanceEntity);
    viewregistry.updateView(viewInstanceEntity);
    replay(viewregistry,viewEntity,viewInstanceEntity,viewURLDAO,viewURLEntity);

    properties.add(propertyMap);

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());

    provider.deleteResources(PropertyHelper.getCreateRequest(properties, null),equalsPredicate);

    ViewURLEntity urlEntity = urlEntityCapture.getValue();
    assertEquals(urlEntity.getUrlName(),"test");
    assertEquals(urlEntity.getUrlSuffix(),"suffix");
    assertEquals(urlEntity.getViewInstanceEntity(),viewInstanceEntity);

  }





}
