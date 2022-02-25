/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller.internal;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.ambari.server.configuration.AmbariServerConfiguration;
import org.apache.ambari.server.controller.UserPrivilegeResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.hooks.HookContextFactory;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.ldap.service.AmbariLdapConfigurationProvider;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.GroupDAO;
import org.apache.ambari.server.orm.dao.MemberDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.security.encryption.Encryptor;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.EasyMockSupport;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import junit.framework.Assert;

/**
 * UserPrivilegeResourceProvider tests.
 */
public class UserPrivilegeResourceProviderTest extends EasyMockSupport {

  @Test(expected = SystemException.class)
  public void testCreateResources() throws Exception {
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createClusterAdministrator("user1", 2L));
    UserPrivilegeResourceProvider resourceProvider = new UserPrivilegeResourceProvider();
    resourceProvider.createResources(createNiceMock(Request.class));
  }

  @Test
  public void testGetResources_Administrator() throws Exception {
    getResourcesTest(TestAuthenticationFactory.createAdministrator("admin"), "User1");
  }

  @Test
  public void testGetResources_NonAdministrator_Self() throws Exception {
    getResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User1");
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResources_NonAdministrator_Other() throws Exception {
    getResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User10");
  }

  @Test(expected = SystemException.class)
  public void testUpdateResources() throws Exception {
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createClusterAdministrator("user1", 2L));
    UserPrivilegeResourceProvider resourceProvider = new UserPrivilegeResourceProvider();
    resourceProvider.updateResources(createNiceMock(Request.class), createNiceMock(Predicate.class));
  }

  @Test(expected = SystemException.class)
  public void testDeleteResources() throws Exception {
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createClusterAdministrator("user1", 2L));
    UserPrivilegeResourceProvider resourceProvider = new UserPrivilegeResourceProvider();
    resourceProvider.deleteResources(createNiceMock(Request.class), createNiceMock(Predicate.class));
  }

  @Test
  public void testToResource_AMBARI() {
    PermissionEntity permissionEntity = createMock(PermissionEntity.class);
    expect(permissionEntity.getPermissionName()).andReturn("ADMINISTRATOR").atLeastOnce();
    expect(permissionEntity.getPermissionLabel()).andReturn("Ambari Administrator").atLeastOnce();

    PrincipalTypeEntity principalTypeEntity = createMock(PrincipalTypeEntity.class);
    expect(principalTypeEntity.getName()).andReturn("USER").atLeastOnce();

    PrincipalEntity principalEntity = createMock(PrincipalEntity.class);
    expect(principalEntity.getPrincipalType()).andReturn(principalTypeEntity).atLeastOnce();

    ResourceTypeEntity resourceTypeEntity = createMock(ResourceTypeEntity.class);
    expect(resourceTypeEntity.getName()).andReturn("AMBARI").atLeastOnce();

    ResourceEntity resourceEntity = createMock(ResourceEntity.class);
    expect(resourceEntity.getResourceType()).andReturn(resourceTypeEntity).atLeastOnce();

    PrivilegeEntity privilegeEntity = createMock(PrivilegeEntity.class);
    expect(privilegeEntity.getId()).andReturn(1).atLeastOnce();
    expect(privilegeEntity.getPermission()).andReturn(permissionEntity).atLeastOnce();
    expect(privilegeEntity.getPrincipal()).andReturn(principalEntity).atLeastOnce();
    expect(privilegeEntity.getResource()).andReturn(resourceEntity).atLeastOnce();

    UserEntity userEntity = createMock(UserEntity.class);
    expect(userEntity.getUserName()).andReturn("jdoe").atLeastOnce();

    UserDAO userDAO = createMock(UserDAO.class);
    expect(userDAO.findUserByPrincipal(anyObject(PrincipalEntity.class))).andReturn(userEntity).anyTimes();

    ClusterDAO clusterDAO = createMock(ClusterDAO.class);
    GroupDAO groupDAO = createMock(GroupDAO.class);
    ViewInstanceDAO viewInstanceDAO = createMock(ViewInstanceDAO.class);

    Users users = createNiceMock(Users.class);

    replayAll();

    UserPrivilegeResourceProvider.init(userDAO, clusterDAO, groupDAO, viewInstanceDAO, users);
    UserPrivilegeResourceProvider provider = new UserPrivilegeResourceProvider();
    UserPrivilegeResponse response = provider.getResponse(privilegeEntity, "jdoe");
    Resource resource = provider.toResource(response, provider.getPropertyIds());

    Assert.assertEquals(ResourceType.AMBARI.name(), resource.getPropertyValue(UserPrivilegeResourceProvider.TYPE));

    verifyAll();
  }

  @Test
  public void testToResource_CLUSTER() {
    PermissionEntity permissionEntity = createMock(PermissionEntity.class);
    expect(permissionEntity.getPermissionName()).andReturn("CLUSTER.ADMINISTRATOR").atLeastOnce();
    expect(permissionEntity.getPermissionLabel()).andReturn("Cluster Administrator").atLeastOnce();

    PrincipalTypeEntity principalTypeEntity = createMock(PrincipalTypeEntity.class);
    expect(principalTypeEntity.getName()).andReturn("USER").atLeastOnce();

    PrincipalEntity principalEntity = createMock(PrincipalEntity.class);
    expect(principalEntity.getPrincipalType()).andReturn(principalTypeEntity).atLeastOnce();

    ClusterEntity clusterEntity = createMock(ClusterEntity.class);
    expect(clusterEntity.getClusterName()).andReturn("TestCluster").atLeastOnce();

    ResourceTypeEntity resourceTypeEntity = createMock(ResourceTypeEntity.class);
    expect(resourceTypeEntity.getName()).andReturn("CLUSTER").atLeastOnce();

    ResourceEntity resourceEntity = createMock(ResourceEntity.class);
    expect(resourceEntity.getId()).andReturn(1L).atLeastOnce();
    expect(resourceEntity.getResourceType()).andReturn(resourceTypeEntity).atLeastOnce();

    PrivilegeEntity privilegeEntity = createMock(PrivilegeEntity.class);
    expect(privilegeEntity.getId()).andReturn(1).atLeastOnce();
    expect(privilegeEntity.getPermission()).andReturn(permissionEntity).atLeastOnce();
    expect(privilegeEntity.getPrincipal()).andReturn(principalEntity).atLeastOnce();
    expect(privilegeEntity.getResource()).andReturn(resourceEntity).atLeastOnce();

    UserEntity userEntity = createMock(UserEntity.class);
    expect(userEntity.getUserName()).andReturn("jdoe").atLeastOnce();

    ClusterDAO clusterDAO = createMock(ClusterDAO.class);
    expect(clusterDAO.findByResourceId(1L)).andReturn(clusterEntity).atLeastOnce();

    GroupDAO groupDAO = createMock(GroupDAO.class);
    ViewInstanceDAO viewInstanceDAO = createMock(ViewInstanceDAO.class);

    UserDAO userDAO = createMock(UserDAO.class);
    expect(userDAO.findUserByPrincipal(anyObject(PrincipalEntity.class))).andReturn(userEntity).anyTimes();

    Users users = createNiceMock(Users.class);

    replayAll();

    UserPrivilegeResourceProvider.init(userDAO, clusterDAO, groupDAO, viewInstanceDAO, users);
    UserPrivilegeResourceProvider provider = new UserPrivilegeResourceProvider();
    UserPrivilegeResponse response = provider.getResponse(privilegeEntity, "jdoe");
    Resource resource = provider.toResource(response, provider.getPropertyIds());

    Assert.assertEquals("TestCluster", resource.getPropertyValue(ClusterPrivilegeResourceProvider.CLUSTER_NAME));
    Assert.assertEquals(ResourceType.CLUSTER.name(), resource.getPropertyValue(UserPrivilegeResourceProvider.TYPE));

    verifyAll();
  }

  @Test
  public void testToResource_VIEW() {
    PermissionEntity permissionEntity = createMock(PermissionEntity.class);
    expect(permissionEntity.getPermissionName()).andReturn("CLUSTER.ADMINISTRATOR").atLeastOnce();
    expect(permissionEntity.getPermissionLabel()).andReturn("Cluster Administrator").atLeastOnce();

    PrincipalTypeEntity principalTypeEntity = createMock(PrincipalTypeEntity.class);
    expect(principalTypeEntity.getName()).andReturn("USER").atLeastOnce();

    PrincipalEntity principalEntity = createMock(PrincipalEntity.class);
    expect(principalEntity.getPrincipalType()).andReturn(principalTypeEntity).atLeastOnce();

    ViewEntity viewEntity = createMock(ViewEntity.class);
    expect(viewEntity.getCommonName()).andReturn("TestView").atLeastOnce();
    expect(viewEntity.getVersion()).andReturn("1.2.3.4").atLeastOnce();

    ViewInstanceEntity viewInstanceEntity = createMock(ViewInstanceEntity.class);
    expect(viewInstanceEntity.getViewEntity()).andReturn(viewEntity).atLeastOnce();
    expect(viewInstanceEntity.getName()).andReturn("Test View").atLeastOnce();

    ResourceTypeEntity resourceTypeEntity = createMock(ResourceTypeEntity.class);
    expect(resourceTypeEntity.getName()).andReturn("VIEW").atLeastOnce();

    ResourceEntity resourceEntity = createMock(ResourceEntity.class);
    expect(resourceEntity.getId()).andReturn(1L).atLeastOnce();
    expect(resourceEntity.getResourceType()).andReturn(resourceTypeEntity).atLeastOnce();

    PrivilegeEntity privilegeEntity = createMock(PrivilegeEntity.class);
    expect(privilegeEntity.getId()).andReturn(1).atLeastOnce();
    expect(privilegeEntity.getPermission()).andReturn(permissionEntity).atLeastOnce();
    expect(privilegeEntity.getPrincipal()).andReturn(principalEntity).atLeastOnce();
    expect(privilegeEntity.getResource()).andReturn(resourceEntity).atLeastOnce();

    UserEntity userEntity = createMock(UserEntity.class);
    expect(userEntity.getUserName()).andReturn("jdoe").atLeastOnce();

    ClusterDAO clusterDAO = createMock(ClusterDAO.class);
    GroupDAO groupDAO = createMock(GroupDAO.class);

    ViewInstanceDAO viewInstanceDAO = createMock(ViewInstanceDAO.class);
    expect(viewInstanceDAO.findByResourceId(1L)).andReturn(viewInstanceEntity).atLeastOnce();

    UserDAO userDAO = createMock(UserDAO.class);
    expect(userDAO.findUserByPrincipal(anyObject(PrincipalEntity.class))).andReturn(userEntity).anyTimes();

    Users users = createNiceMock(Users.class);

    replayAll();

    UserPrivilegeResourceProvider.init(userDAO, clusterDAO, groupDAO, viewInstanceDAO, users);
    UserPrivilegeResourceProvider provider = new UserPrivilegeResourceProvider();
    UserPrivilegeResponse response = provider.getResponse(privilegeEntity, "jdoe");
    Resource resource = provider.toResource(response, provider.getPropertyIds());

    Assert.assertEquals("Test View", resource.getPropertyValue(ViewPrivilegeResourceProvider.INSTANCE_NAME));
    Assert.assertEquals("TestView", resource.getPropertyValue(ViewPrivilegeResourceProvider.VIEW_NAME));
    Assert.assertEquals("1.2.3.4", resource.getPropertyValue(ViewPrivilegeResourceProvider.VERSION));
    Assert.assertEquals(ResourceType.VIEW.name(), resource.getPropertyValue(UserPrivilegeResourceProvider.TYPE));

    verifyAll();
  }

  @Test
  public void testToResource_SpecificVIEW() {
    PermissionEntity permissionEntity = createMock(PermissionEntity.class);
    expect(permissionEntity.getPermissionName()).andReturn("CLUSTER.ADMINISTRATOR").atLeastOnce();
    expect(permissionEntity.getPermissionLabel()).andReturn("Cluster Administrator").atLeastOnce();

    PrincipalTypeEntity principalTypeEntity = createMock(PrincipalTypeEntity.class);
    expect(principalTypeEntity.getName()).andReturn("USER").atLeastOnce();

    PrincipalEntity principalEntity = createMock(PrincipalEntity.class);
    expect(principalEntity.getPrincipalType()).andReturn(principalTypeEntity).atLeastOnce();

    ViewEntity viewEntity = createMock(ViewEntity.class);
    expect(viewEntity.getCommonName()).andReturn("TestView").atLeastOnce();
    expect(viewEntity.getVersion()).andReturn("1.2.3.4").atLeastOnce();

    ViewInstanceEntity viewInstanceEntity = createMock(ViewInstanceEntity.class);
    expect(viewInstanceEntity.getViewEntity()).andReturn(viewEntity).atLeastOnce();
    expect(viewInstanceEntity.getName()).andReturn("Test View").atLeastOnce();

    ResourceTypeEntity resourceTypeEntity = createMock(ResourceTypeEntity.class);
    expect(resourceTypeEntity.getName()).andReturn("TestView{1.2.3.4}").atLeastOnce();

    ResourceEntity resourceEntity = createMock(ResourceEntity.class);
    expect(resourceEntity.getId()).andReturn(1L).atLeastOnce();
    expect(resourceEntity.getResourceType()).andReturn(resourceTypeEntity).atLeastOnce();

    PrivilegeEntity privilegeEntity = createMock(PrivilegeEntity.class);
    expect(privilegeEntity.getId()).andReturn(1).atLeastOnce();
    expect(privilegeEntity.getPermission()).andReturn(permissionEntity).atLeastOnce();
    expect(privilegeEntity.getPrincipal()).andReturn(principalEntity).atLeastOnce();
    expect(privilegeEntity.getResource()).andReturn(resourceEntity).atLeastOnce();

    UserEntity userEntity = createMock(UserEntity.class);
    expect(userEntity.getUserName()).andReturn("jdoe").atLeastOnce();

    ClusterDAO clusterDAO = createMock(ClusterDAO.class);
    GroupDAO groupDAO = createMock(GroupDAO.class);

    ViewInstanceDAO viewInstanceDAO = createMock(ViewInstanceDAO.class);
    expect(viewInstanceDAO.findByResourceId(1L)).andReturn(viewInstanceEntity).atLeastOnce();

    UserDAO userDAO = createMock(UserDAO.class);
    expect(userDAO.findUserByPrincipal(anyObject(PrincipalEntity.class))).andReturn(userEntity).anyTimes();

    Users users = createNiceMock(Users.class);

    replayAll();

    UserPrivilegeResourceProvider.init(userDAO, clusterDAO, groupDAO, viewInstanceDAO, users);
    UserPrivilegeResourceProvider provider = new UserPrivilegeResourceProvider();
    UserPrivilegeResponse response = provider.getResponse(privilegeEntity, "jdoe");
    Resource resource = provider.toResource(response, provider.getPropertyIds());

    Assert.assertEquals("Test View", resource.getPropertyValue(ViewPrivilegeResourceProvider.INSTANCE_NAME));
    Assert.assertEquals("TestView", resource.getPropertyValue(ViewPrivilegeResourceProvider.VIEW_NAME));
    Assert.assertEquals("1.2.3.4", resource.getPropertyValue(ViewPrivilegeResourceProvider.VERSION));
    Assert.assertEquals(ResourceType.VIEW.name(), resource.getPropertyValue(UserPrivilegeResourceProvider.TYPE));

    verifyAll();
  }

  @Test
  public void testToResource_SpecificVIEW_WithClusterInheritedPermission() throws Exception {
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createClusterAdministrator("jdoe", 2L));

    Injector injector = createInjector();

    final UserPrivilegeResourceProvider resourceProvider = new UserPrivilegeResourceProvider();
    final UserDAO userDAO = injector.getInstance(UserDAO.class);
    final GroupDAO groupDAO = injector.getInstance(GroupDAO.class);
    final ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    final ViewInstanceDAO viewInstanceDAO = injector.getInstance(ViewInstanceDAO.class);
    final PrivilegeDAO privilegeDAO = injector.getInstance(PrivilegeDAO.class);
    final MemberDAO memberDAO = injector.getInstance(MemberDAO.class);


    final PrincipalTypeEntity rolePrincipalTypeEntity = createMock(PrincipalTypeEntity.class);
    expect(rolePrincipalTypeEntity.getName()).andReturn("ROLE").atLeastOnce();

    final PrincipalEntity rolePrincipalEntity = createMock(PrincipalEntity.class);
    expect(rolePrincipalEntity.getPrincipalType()).andReturn(rolePrincipalTypeEntity).atLeastOnce();

    final PermissionEntity permissionEntity = createMock(PermissionEntity.class);
    expect(permissionEntity.getPrincipal()).andReturn(rolePrincipalEntity).atLeastOnce();
    expect(permissionEntity.getPermissionName()).andReturn("CLUSTER.ADMINISTRATOR").atLeastOnce();
    expect(permissionEntity.getPermissionLabel()).andReturn("Cluster Administrator").atLeastOnce();

    final PrincipalTypeEntity principalTypeEntity = createMock(PrincipalTypeEntity.class);
    expect(principalTypeEntity.getName()).andReturn("USER").atLeastOnce();

    final PrincipalEntity principalEntity = createMock(PrincipalEntity.class);
    expect(principalEntity.getPrincipalType()).andReturn(principalTypeEntity).atLeastOnce();

    final ViewEntity viewEntity = createMock(ViewEntity.class);
    expect(viewEntity.getCommonName()).andReturn("TestView").atLeastOnce();
    expect(viewEntity.getVersion()).andReturn("1.2.3.4").atLeastOnce();

    final ResourceTypeEntity resourceTypeEntity = createMock(ResourceTypeEntity.class);
    expect(resourceTypeEntity.getName()).andReturn("TestView{1.2.3.4}").atLeastOnce();

    final ResourceEntity resourceEntity = createMock(ResourceEntity.class);
    expect(resourceEntity.getId()).andReturn(1L).anyTimes();
    expect(resourceEntity.getResourceType()).andReturn(resourceTypeEntity).anyTimes();

    final ViewInstanceEntity viewInstanceEntity = createMock(ViewInstanceEntity.class);
    expect(viewInstanceEntity.getViewEntity()).andReturn(viewEntity).atLeastOnce();
    expect(viewInstanceEntity.getName()).andReturn("Test View").atLeastOnce();

    final PrivilegeEntity explicitPrivilegeEntity = createMock(PrivilegeEntity.class);
    expect(explicitPrivilegeEntity.getId()).andReturn(1).atLeastOnce();
    expect(explicitPrivilegeEntity.getPermission()).andReturn(permissionEntity).atLeastOnce();
    expect(explicitPrivilegeEntity.getPrincipal()).andReturn(principalEntity).atLeastOnce();
    expect(explicitPrivilegeEntity.getResource()).andReturn(resourceEntity).atLeastOnce();

    final PrivilegeEntity implicitPrivilegeEntity = createMock(PrivilegeEntity.class);
    expect(implicitPrivilegeEntity.getId()).andReturn(2).atLeastOnce();
    expect(implicitPrivilegeEntity.getPermission()).andReturn(permissionEntity).atLeastOnce();
    expect(implicitPrivilegeEntity.getPrincipal()).andReturn(rolePrincipalEntity).atLeastOnce();
    expect(implicitPrivilegeEntity.getResource()).andReturn(resourceEntity).atLeastOnce();

    UserEntity userEntity = createMock(UserEntity.class);
    expect(userEntity.getUserName()).andReturn("jdoe").atLeastOnce();
    expect(userEntity.getPrincipal()).andReturn(principalEntity).atLeastOnce();

    expect(viewInstanceDAO.findByResourceId(1L)).andReturn(viewInstanceEntity).atLeastOnce();

    expect(userDAO.findUserByName("jdoe")).andReturn(userEntity).anyTimes();
    expect(userDAO.findUserByPrincipal(anyObject(PrincipalEntity.class))).andReturn(userEntity).anyTimes();
    expect(userDAO.findAll()).andReturn(Collections.emptyList()).anyTimes();

    final Users users = injector.getInstance(Users.class);

    List<PrincipalEntity> rolePrincipals = new LinkedList<>();
    rolePrincipals.add(rolePrincipalEntity);

    List<PrincipalEntity> userPrincipals = new LinkedList<>();
    userPrincipals.add(principalEntity);

    expect(privilegeDAO.findAllByPrincipal(userPrincipals)).
        andReturn(Collections.singletonList(explicitPrivilegeEntity))
        .once();
    // Implicit privileges...
    expect(privilegeDAO.findAllByPrincipal(rolePrincipals)).
        andReturn(Collections.singletonList(implicitPrivilegeEntity))
        .once();
    expect(memberDAO.findAllMembersByUser(userEntity)).
        andReturn(Collections.emptyList())
        .atLeastOnce();

    replayAll();

    final Set<String> propertyIds = new HashSet<>();
    propertyIds.add(UserPrivilegeResourceProvider.USER_NAME);
    final Predicate predicate = new PredicateBuilder()
        .property(UserPrivilegeResourceProvider.USER_NAME)
        .equals("jdoe")
        .toPredicate();
    TestAuthenticationFactory.createClusterAdministrator("jdoe", 2L);
    Request request = PropertyHelper.getReadRequest(propertyIds);

    UserPrivilegeResourceProvider.init(userDAO, clusterDAO, groupDAO, viewInstanceDAO, users);
    UserPrivilegeResourceProvider provider = new UserPrivilegeResourceProvider();
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      String userName = (String) resource.getPropertyValue(UserPrivilegeResourceProvider.USER_NAME);
      Assert.assertEquals("jdoe", userName);
    }

    verifyAll();
  }

  //  @SuppressWarnings("serial")
  private void getResourcesTest(Authentication authentication, String requestedUsername) throws Exception {
    Injector injector = createInjector();

    final UserPrivilegeResourceProvider resourceProvider = new UserPrivilegeResourceProvider();
    final UserDAO userDAO = injector.getInstance(UserDAO.class);
    final GroupDAO groupDAO = injector.getInstance(GroupDAO.class);
    final ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    final ViewInstanceDAO viewInstanceDAO = injector.getInstance(ViewInstanceDAO.class);
    final PrivilegeDAO privilegeDAO = injector.getInstance(PrivilegeDAO.class);
    final MemberDAO memberDAO = injector.getInstance(MemberDAO.class);

    final UserEntity userEntity = createNiceMock(UserEntity.class);
    final PrincipalEntity principalEntity = createNiceMock(PrincipalEntity.class);
    final PrivilegeEntity privilegeEntity = createNiceMock(PrivilegeEntity.class);
    final PermissionEntity permissionEntity = createNiceMock(PermissionEntity.class);
    final PrincipalTypeEntity principalTypeEntity = createNiceMock(PrincipalTypeEntity.class);
    final ResourceEntity resourceEntity = createNiceMock(ResourceEntity.class);
    final ResourceTypeEntity resourceTypeEntity = createNiceMock(ResourceTypeEntity.class);

    final Users users = injector.getInstance(Users.class);

    List<PrincipalEntity> userPrincipals = new LinkedList<>();
    userPrincipals.add(principalEntity);

    expect(privilegeDAO.findAllByPrincipal(userPrincipals)).
        andReturn(Collections.singletonList(privilegeEntity))
        .atLeastOnce();
    expect(memberDAO.findAllMembersByUser(userEntity)).
        andReturn(Collections.emptyList())
        .atLeastOnce();
    expect(userDAO.findAll()).andReturn(Collections.emptyList()).anyTimes();
    expect(userDAO.findUserByName(requestedUsername)).andReturn(userEntity).anyTimes();
    expect(userEntity.getPrincipal()).andReturn(principalEntity).anyTimes();
    expect(userEntity.getMemberEntities()).andReturn(Collections.emptySet()).anyTimes();
    expect(privilegeEntity.getPermission()).andReturn(permissionEntity).anyTimes();
    expect(privilegeEntity.getPrincipal()).andReturn(principalEntity).anyTimes();
    expect(principalEntity.getPrincipalType()).andReturn(principalTypeEntity).anyTimes();
    expect(principalTypeEntity.getName()).andReturn(PrincipalTypeEntity.USER_PRINCIPAL_TYPE_NAME).anyTimes();
    expect(principalEntity.getPrivileges()).andReturn(new HashSet<PrivilegeEntity>() {
      {
        add(privilegeEntity);
      }
    }).anyTimes();
    expect(userDAO.findUserByPrincipal(anyObject(PrincipalEntity.class))).andReturn(userEntity).anyTimes();
    expect(userEntity.getUserName()).andReturn(requestedUsername).anyTimes();
    expect(privilegeEntity.getResource()).andReturn(resourceEntity).anyTimes();
    expect(resourceEntity.getResourceType()).andReturn(resourceTypeEntity).anyTimes();
    expect(resourceTypeEntity.getName()).andReturn(ResourceType.AMBARI.name());
    expect(viewInstanceDAO.findAll()).andReturn(new ArrayList<>()).anyTimes();

    replayAll();

    UserPrivilegeResourceProvider.init(userDAO, clusterDAO, groupDAO, viewInstanceDAO, users);

    final Set<String> propertyIds = new HashSet<>();
    propertyIds.add(UserPrivilegeResourceProvider.USER_NAME);

    final Predicate predicate = new PredicateBuilder()
        .property(UserPrivilegeResourceProvider.USER_NAME)
        .equals(requestedUsername)
        .toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);

    // Set the authenticated user to a administrator
    SecurityContextHolder.getContext().setAuthentication(authentication);

    Set<Resource> resources = resourceProvider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      String userName = (String) resource.getPropertyValue(UserPrivilegeResourceProvider.USER_NAME);
      Assert.assertEquals(requestedUsername, userName);
    }

    verifyAll();
  }

  private Injector createInjector() {
    return Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(PasswordEncoder.class).toInstance(createNiceMock(PasswordEncoder.class));
        bind(HookService.class).toInstance(createMock(HookService.class));
        bind(HookContextFactory.class).toInstance(createMock(HookContextFactory.class));

        bind(UserDAO.class).toInstance(createNiceMock(UserDAO.class));
        bind(GroupDAO.class).toInstance(createNiceMock(GroupDAO.class));
        bind(ClusterDAO.class).toInstance(createNiceMock(ClusterDAO.class));
        bind(ViewInstanceDAO.class).toInstance(createNiceMock(ViewInstanceDAO.class));
        bind(PrivilegeDAO.class).toInstance(createMock(PrivilegeDAO.class));
        bind(MemberDAO.class).toInstance(createMock(MemberDAO.class));
        bind(new TypeLiteral<Encryptor<AmbariServerConfiguration>>() {}).annotatedWith(Names.named("AmbariServerConfigurationEncryptor")).toInstance(Encryptor.NONE);
        bind(AmbariLdapConfigurationProvider.class).toInstance(createMock(AmbariLdapConfigurationProvider.class));
      }
    });
  }

}
