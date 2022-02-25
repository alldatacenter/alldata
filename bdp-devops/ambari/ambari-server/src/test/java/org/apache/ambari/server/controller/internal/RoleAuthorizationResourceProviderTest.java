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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.reset;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.RoleAuthorizationDAO;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.RoleAuthorizationEntity;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;


/**
 * RoleAuthorizationResourceProvider tests.
 */
public class RoleAuthorizationResourceProviderTest extends EasyMockSupport {
  private static Injector injector;

  @Before
  public void setup() {
    reset();

    injector = Guice.createInjector(Modules.override(new InMemoryDefaultTestModule())
        .with(new AbstractModule() {
          @Override
          protected void configure() {
            AmbariManagementController managementController = createNiceMock(AmbariManagementController.class);

            bind(AmbariManagementController.class).toInstance(managementController);
            bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
            bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
            bind(PermissionDAO.class).toInstance(createStrictMock(PermissionDAO.class));
            bind(RoleAuthorizationDAO.class).toInstance(createStrictMock(RoleAuthorizationDAO.class));
          }
        }));
  }


  @Test
  public void testGetResources() throws Exception {
    RoleAuthorizationEntity roleAuthorizationEntity = createNiceMock(RoleAuthorizationEntity.class);
    expect(roleAuthorizationEntity.getAuthorizationId()).andReturn("TEST.DO_SOMETHING");
    expect(roleAuthorizationEntity.getAuthorizationName()).andReturn("Do Something");

    List<RoleAuthorizationEntity> authorizationEntities = new ArrayList<>();
    authorizationEntities.add(roleAuthorizationEntity);

    RoleAuthorizationDAO roleAuthorizationDAO = injector.getInstance(RoleAuthorizationDAO.class);
    expect(roleAuthorizationDAO.findAll()).andReturn(authorizationEntities);

    replayAll();

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);
    RoleAuthorizationResourceProvider provider = new RoleAuthorizationResourceProvider(managementController);
    injector.injectMembers(provider);

    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), null);

    Assert.assertEquals(1, resources.size());
    Resource resource = resources.iterator().next();

    Assert.assertEquals("TEST.DO_SOMETHING", resource.getPropertyValue(RoleAuthorizationResourceProvider.AUTHORIZATION_ID_PROPERTY_ID));
    Assert.assertEquals("Do Something", resource.getPropertyValue(RoleAuthorizationResourceProvider.AUTHORIZATION_NAME_PROPERTY_ID));
    Assert.assertNull(resource.getPropertyValue(RoleAuthorizationResourceProvider.PERMISSION_ID_PROPERTY_ID));

    verifyAll();
  }

  @Test
  public void testGetResourcesForPermission() throws Exception {
    RoleAuthorizationEntity roleAuthorizationEntity = createNiceMock(RoleAuthorizationEntity.class);
    expect(roleAuthorizationEntity.getAuthorizationId()).andReturn("TEST.DO_SOMETHING").once();
    expect(roleAuthorizationEntity.getAuthorizationName()).andReturn("Do Something").once();

    List<RoleAuthorizationEntity> authorizationEntities = new ArrayList<>();
    authorizationEntities.add(roleAuthorizationEntity);

    PermissionEntity permissionEntry = createStrictMock(PermissionEntity.class);
    expect(permissionEntry.getAuthorizations()).andReturn(authorizationEntities).once();

    PermissionDAO permissionDAO = injector.getInstance(PermissionDAO.class);
    expect(permissionDAO.findById(1)).andReturn(permissionEntry).once();

    replayAll();

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);
    RoleAuthorizationResourceProvider provider = new RoleAuthorizationResourceProvider(managementController);

    Predicate predicate = new PredicateBuilder()
        .property(RoleAuthorizationResourceProvider.PERMISSION_ID_PROPERTY_ID).equals("1")
        .toPredicate();

    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), predicate);

    Assert.assertEquals(1, resources.size());
    Resource resource = resources.iterator().next();

    Assert.assertEquals("TEST.DO_SOMETHING", resource.getPropertyValue(RoleAuthorizationResourceProvider.AUTHORIZATION_ID_PROPERTY_ID));
    Assert.assertEquals("Do Something", resource.getPropertyValue(RoleAuthorizationResourceProvider.AUTHORIZATION_NAME_PROPERTY_ID));
    Assert.assertEquals(1, resource.getPropertyValue(RoleAuthorizationResourceProvider.PERMISSION_ID_PROPERTY_ID));

    verifyAll();
  }

  @Test
  public void testGetResource() throws Exception {
    RoleAuthorizationEntity roleAuthorizationEntity1 = createNiceMock(RoleAuthorizationEntity.class);
    expect(roleAuthorizationEntity1.getAuthorizationId()).andReturn("TEST.DO_SOMETHING").anyTimes();
    expect(roleAuthorizationEntity1.getAuthorizationName()).andReturn("Do Something").anyTimes();

    RoleAuthorizationEntity roleAuthorizationEntity2 = createNiceMock(RoleAuthorizationEntity.class);
    expect(roleAuthorizationEntity2.getAuthorizationId()).andReturn("TEST.DO_SOMETHING_ELSE").anyTimes();
    expect(roleAuthorizationEntity2.getAuthorizationName()).andReturn("Do Something Else").anyTimes();

    List<RoleAuthorizationEntity> authorizationEntities = new ArrayList<>();
    authorizationEntities.add(roleAuthorizationEntity1);
    authorizationEntities.add(roleAuthorizationEntity2);

    PermissionEntity permissionEntry = createStrictMock(PermissionEntity.class);
    expect(permissionEntry.getAuthorizations()).andReturn(authorizationEntities).once();

    PermissionDAO permissionDAO = injector.getInstance(PermissionDAO.class);
    expect(permissionDAO.findById(1)).andReturn(permissionEntry).once();

    replayAll();

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);
    RoleAuthorizationResourceProvider provider = new RoleAuthorizationResourceProvider(managementController);

    Predicate predicate = new PredicateBuilder().begin()
        .property(RoleAuthorizationResourceProvider.AUTHORIZATION_ID_PROPERTY_ID).equals("TEST.DO_SOMETHING")
        .and()
        .property(RoleAuthorizationResourceProvider.PERMISSION_ID_PROPERTY_ID).equals("1")
        .end()
        .toPredicate();

    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), predicate);

    Assert.assertEquals(1, resources.size());
    Resource resource = resources.iterator().next();

    Assert.assertEquals("TEST.DO_SOMETHING", resource.getPropertyValue(RoleAuthorizationResourceProvider.AUTHORIZATION_ID_PROPERTY_ID));
    Assert.assertEquals("Do Something", resource.getPropertyValue(RoleAuthorizationResourceProvider.AUTHORIZATION_NAME_PROPERTY_ID));
    Assert.assertEquals(1, resource.getPropertyValue(RoleAuthorizationResourceProvider.PERMISSION_ID_PROPERTY_ID));

    verifyAll();
  }

  @Test(expected = org.apache.ambari.server.controller.spi.SystemException.class)
  public void testUpdateResources() throws Exception {
    replayAll();
    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);
    RoleAuthorizationResourceProvider provider = new RoleAuthorizationResourceProvider(managementController);
    provider.updateResources(createNiceMock(Request.class), null);
  }

  @Test(expected = org.apache.ambari.server.controller.spi.SystemException.class)
  public void testDeleteResources() throws Exception {
    replayAll();
    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);
    RoleAuthorizationResourceProvider provider = new RoleAuthorizationResourceProvider(managementController);
    provider.deleteResources(new RequestImpl(null, null, null, null), null);
  }
}
