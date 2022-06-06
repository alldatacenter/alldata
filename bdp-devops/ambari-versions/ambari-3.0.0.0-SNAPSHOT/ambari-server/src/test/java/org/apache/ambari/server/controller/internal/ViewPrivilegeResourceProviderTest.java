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
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.GroupDAO;
import org.apache.ambari.server.orm.dao.MemberDAO;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.PrincipalDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.ResourceDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.dao.ViewDAO;
import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewEntityTest;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntityTest;
import org.apache.ambari.server.security.SecurityHelper;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.view.ViewInstanceHandlerList;
import org.apache.ambari.server.view.ViewRegistry;
import org.apache.ambari.server.view.ViewRegistryTest;
import org.apache.ambari.view.ViewDefinition;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * ViewPrivilegeResourceProvider tests.
 */
public class ViewPrivilegeResourceProviderTest {
  private final static PrivilegeDAO privilegeDAO = createStrictMock(PrivilegeDAO.class);
  private final static UserDAO userDAO = createStrictMock(UserDAO.class);
  private final static GroupDAO groupDAO = createStrictMock(GroupDAO.class);
  private final static PrincipalDAO principalDAO = createStrictMock(PrincipalDAO.class);
  private final static PermissionDAO permissionDAO = createStrictMock(PermissionDAO.class);
  private final static ResourceDAO resourceDAO = createStrictMock(ResourceDAO.class);
  private static final ViewDAO viewDAO = createMock(ViewDAO.class);
  private static final ViewInstanceDAO viewInstanceDAO = createNiceMock(ViewInstanceDAO.class);
  private static final MemberDAO memberDAO = createNiceMock(MemberDAO.class);
  private static final ResourceTypeDAO resourceTypeDAO = createNiceMock(ResourceTypeDAO.class);
  private static final SecurityHelper securityHelper = createNiceMock(SecurityHelper.class);
  private static final ViewInstanceHandlerList handlerList = createNiceMock(ViewInstanceHandlerList.class);

  @BeforeClass
  public static void initClass() {
    PrivilegeResourceProvider.init(privilegeDAO, userDAO, groupDAO, principalDAO, permissionDAO, resourceDAO);
  }

  @Before
  public void resetGlobalMocks() {

    ViewRegistry.initInstance(ViewRegistryTest.getRegistry(viewDAO, viewInstanceDAO, userDAO,
        memberDAO, privilegeDAO, permissionDAO, resourceDAO, resourceTypeDAO, securityHelper, handlerList, null, null, null));
    reset(privilegeDAO, userDAO, groupDAO, principalDAO, permissionDAO, resourceDAO, handlerList);
  }

  @Test
  public void testGetResources() throws Exception {

    ViewEntity viewDefinition = ViewEntityTest.getViewEntity();
    ViewInstanceEntity viewInstanceDefinition = ViewInstanceEntityTest.getViewInstanceEntity();

    viewDefinition.addInstanceDefinition(viewInstanceDefinition);
    viewInstanceDefinition.setViewEntity(viewDefinition);
    viewDefinition.setStatus(ViewDefinition.ViewStatus.DEPLOYED);

    ViewRegistry registry = ViewRegistry.getInstance();

    registry.addDefinition(viewDefinition);

    registry.addInstanceDefinition(viewDefinition, viewInstanceDefinition);


    List<PrivilegeEntity> privilegeEntities = new LinkedList<>();

    PrivilegeEntity privilegeEntity = createNiceMock(PrivilegeEntity.class);
    ResourceEntity resourceEntity = createNiceMock(ResourceEntity.class);
    UserEntity userEntity = createNiceMock(UserEntity.class);
    PrincipalEntity principalEntity = createNiceMock(PrincipalEntity.class);
    PrincipalTypeEntity principalTypeEntity = createNiceMock(PrincipalTypeEntity.class);
    PermissionEntity permissionEntity = createNiceMock(PermissionEntity.class);

    List<PrincipalEntity> principalEntities = new LinkedList<>();
    principalEntities.add(principalEntity);

    List<UserEntity> userEntities = new LinkedList<>();
    userEntities.add(userEntity);

    privilegeEntities.add(privilegeEntity);

    expect(privilegeDAO.findAll()).andReturn(privilegeEntities);
    expect(privilegeEntity.getResource()).andReturn(resourceEntity).anyTimes();
    expect(privilegeEntity.getPrincipal()).andReturn(principalEntity).anyTimes();
    expect(privilegeEntity.getPermission()).andReturn(permissionEntity).anyTimes();
    expect(resourceEntity.getId()).andReturn(20L).anyTimes();
    expect(principalEntity.getId()).andReturn(20L).anyTimes();
    expect(userEntity.getPrincipal()).andReturn(principalEntity).anyTimes();
    expect(userEntity.getUserName()).andReturn("joe").anyTimes();
    expect(permissionEntity.getPermissionName()).andReturn("VIEW.USER").anyTimes();
    expect(permissionEntity.getPermissionLabel()).andReturn("View User").anyTimes();
    expect(principalEntity.getPrincipalType()).andReturn(principalTypeEntity).anyTimes();
    expect(principalTypeEntity.getName()).andReturn("USER").anyTimes();

    expect(permissionDAO.findById(PermissionEntity.VIEW_USER_PERMISSION)).andReturn(permissionEntity);

    expect(userDAO.findUsersByPrincipal(principalEntities)).andReturn(userEntities);

    replay(privilegeDAO, userDAO, groupDAO, principalDAO, permissionDAO, resourceDAO, privilegeEntity, resourceEntity,
        userEntity, principalEntity, permissionEntity, principalTypeEntity);

    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator("admin"));

    PrivilegeResourceProvider provider = new ViewPrivilegeResourceProvider();
    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), null);

    Assert.assertEquals(1, resources.size());

    Resource resource = resources.iterator().next();

    Assert.assertEquals("VIEW.USER", resource.getPropertyValue(AmbariPrivilegeResourceProvider.PERMISSION_NAME));
    Assert.assertEquals("View User", resource.getPropertyValue(AmbariPrivilegeResourceProvider.PERMISSION_LABEL));
    Assert.assertEquals("joe", resource.getPropertyValue(AmbariPrivilegeResourceProvider.PRINCIPAL_NAME));
    Assert.assertEquals("USER", resource.getPropertyValue(AmbariPrivilegeResourceProvider.PRINCIPAL_TYPE));

    verify(privilegeDAO, userDAO, groupDAO, principalDAO, permissionDAO, resourceDAO, privilegeEntity, resourceEntity,
        userEntity, principalEntity, permissionEntity, principalTypeEntity);
  }
}

