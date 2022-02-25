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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * PermissionResourceProvider tests.
 */
public class PermissionResourceProviderTest {

  private final static PermissionDAO dao = createStrictMock(PermissionDAO.class);

  @BeforeClass
  public static void initClass() {
    PermissionResourceProvider.init(dao);
  }

  @Before
  public void resetGlobalMocks() {
    reset(dao);
  }

  @Test (expected = UnsupportedOperationException.class)
  public void testCreateResources() throws Exception {
    PermissionResourceProvider provider = new PermissionResourceProvider();
    Request request = createNiceMock(Request.class);
    provider.createResources(request);
  }

  @Test
  public void testGetResources() throws Exception {
    List<PermissionEntity> permissionEntities = new LinkedList<>();

    PermissionEntity permissionEntity = createNiceMock(PermissionEntity.class);
    ResourceTypeEntity resourceTypeEntity = createNiceMock(ResourceTypeEntity.class);

    permissionEntities.add(permissionEntity);

    expect(dao.findAll()).andReturn(permissionEntities);
    expect(permissionEntity.getId()).andReturn(99);
    expect(permissionEntity.getPermissionName()).andReturn("AMBARI.ADMINISTRATOR");
    expect(permissionEntity.getPermissionLabel()).andReturn("Ambari Administrator");
    expect(permissionEntity.getSortOrder()).andReturn(1);
    expect(permissionEntity.getResourceType()).andReturn(resourceTypeEntity);
    expect(resourceTypeEntity.getName()).andReturn("AMBARI");

    replay(dao, permissionEntity, resourceTypeEntity);
    PermissionResourceProvider provider = new PermissionResourceProvider();
    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), null);
    // built in permissions
    Assert.assertEquals(1, resources.size());
    Resource resource = resources.iterator().next();

    Assert.assertEquals(99, resource.getPropertyValue(PermissionResourceProvider.PERMISSION_ID_PROPERTY_ID));
    Assert.assertEquals("AMBARI.ADMINISTRATOR", resource.getPropertyValue(PermissionResourceProvider.PERMISSION_NAME_PROPERTY_ID));
    Assert.assertEquals("Ambari Administrator", resource.getPropertyValue(PermissionResourceProvider.PERMISSION_LABEL_PROPERTY_ID));
    Assert.assertEquals("AMBARI", resource.getPropertyValue(PermissionResourceProvider.RESOURCE_NAME_PROPERTY_ID));
    Assert.assertEquals(1, resource.getPropertyValue(PermissionResourceProvider.SORT_ORDER_PROPERTY_ID));
    verify(dao, permissionEntity, resourceTypeEntity);
  }

  @Test (expected = UnsupportedOperationException.class)
  public void testUpdateResources() throws Exception {
    PermissionResourceProvider provider = new PermissionResourceProvider();
    Request request = createNiceMock(Request.class);
    provider.updateResources(request, null);
  }

  @Test (expected = UnsupportedOperationException.class)
  public void testDeleteResources() throws Exception {
    PermissionResourceProvider provider = new PermissionResourceProvider();
    provider.deleteResources(new RequestImpl(null, null, null, null), null);
  }
}
