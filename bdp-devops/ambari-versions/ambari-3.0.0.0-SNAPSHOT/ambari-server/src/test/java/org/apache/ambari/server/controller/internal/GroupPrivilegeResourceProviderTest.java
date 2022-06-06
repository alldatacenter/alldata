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
import static org.easymock.EasyMock.expect;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.ambari.server.configuration.AmbariServerConfiguration;
import org.apache.ambari.server.controller.GroupPrivilegeResponse;
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
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.GroupEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
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
 * GroupPrivilegeResourceProvider tests.
 */
public class GroupPrivilegeResourceProviderTest extends EasyMockSupport{

  @Test(expected = SystemException.class)
  public void testCreateResources() throws Exception {
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createClusterAdministrator("user1", 2L));
    GroupPrivilegeResourceProvider resourceProvider = new GroupPrivilegeResourceProvider();
    resourceProvider.createResources(createNiceMock(Request.class));
  }

  @Test
  public void testGetResources_Administrator() throws Exception {
    getResourcesTest(TestAuthenticationFactory.createAdministrator("admin"), "Group1");
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResources_NonAdministrator() throws Exception {
    getResourcesTest(TestAuthenticationFactory.createClusterAdministrator("user1", 2L), "Group1");
  }

  @Test(expected = SystemException.class)
  public void testUpdateResources() throws Exception {
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createClusterAdministrator("user1", 2L));
    GroupPrivilegeResourceProvider resourceProvider = new GroupPrivilegeResourceProvider();
    resourceProvider.updateResources(createNiceMock(Request.class), createNiceMock(Predicate.class));
  }

  @Test(expected = SystemException.class)
  public void testDeleteResources() throws Exception {
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createClusterAdministrator("user1", 2L));
    GroupPrivilegeResourceProvider resourceProvider = new GroupPrivilegeResourceProvider();
    resourceProvider.deleteResources(new RequestImpl(null, null, null, null), createNiceMock(Predicate.class));
  }

  @Test
  public void testToResource_AMBARI() {
    PermissionEntity permissionEntity = createMock(PermissionEntity.class);
    expect(permissionEntity.getPermissionName()).andReturn("ADMINISTRATOR").atLeastOnce();
    expect(permissionEntity.getPermissionLabel()).andReturn("Ambari Administrator").atLeastOnce();

    PrincipalTypeEntity principalTypeEntity = createMock(PrincipalTypeEntity.class);
    expect(principalTypeEntity.getName()).andReturn("GROUP").atLeastOnce();

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

    GroupEntity groupEntity = createMock(GroupEntity.class);
    expect(groupEntity.getGroupName()).andReturn("group1").atLeastOnce();

    GroupDAO groupDAO = createMock(GroupDAO.class);
    expect(groupDAO.findGroupByPrincipal(anyObject(PrincipalEntity.class))).andReturn(groupEntity).anyTimes();

    ClusterDAO clusterDAO = createMock(ClusterDAO.class);
    ViewInstanceDAO viewInstanceDAO = createMock(ViewInstanceDAO.class);
    Users users = createNiceMock(Users.class);

    replayAll();

    GroupPrivilegeResourceProvider.init(clusterDAO, groupDAO, viewInstanceDAO, users);
    GroupPrivilegeResourceProvider provider = new GroupPrivilegeResourceProvider();
    GroupPrivilegeResponse response = provider.getResponse(privilegeEntity, "group1");
    Resource resource = provider.toResource(response, provider.getPropertyIds());

    Assert.assertEquals(ResourceType.AMBARI.name(), resource.getPropertyValue(GroupPrivilegeResourceProvider.TYPE));

    verifyAll();
  }

  @Test
  public void testToResource_CLUSTER() {
    PermissionEntity permissionEntity = createMock(PermissionEntity.class);
    expect(permissionEntity.getPermissionName()).andReturn("CLUSTER.ADMINISTRATOR").atLeastOnce();
    expect(permissionEntity.getPermissionLabel()).andReturn("Cluster Administrator").atLeastOnce();

    PrincipalTypeEntity principalTypeEntity = createMock(PrincipalTypeEntity.class);
    expect(principalTypeEntity.getName()).andReturn("GROUP").atLeastOnce();

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

    GroupEntity groupEntity = createMock(GroupEntity.class);
    expect(groupEntity.getGroupName()).andReturn("group1").atLeastOnce();

    ClusterDAO clusterDAO = createMock(ClusterDAO.class);
    expect(clusterDAO.findByResourceId(1L)).andReturn(clusterEntity).atLeastOnce();

    ViewInstanceDAO viewInstanceDAO = createMock(ViewInstanceDAO.class);

    GroupDAO groupDAO = createMock(GroupDAO.class);
    expect(groupDAO.findGroupByPrincipal(anyObject(PrincipalEntity.class))).andReturn(groupEntity).anyTimes();
    Users users = createNiceMock(Users.class);

    replayAll();

    GroupPrivilegeResourceProvider.init(clusterDAO, groupDAO, viewInstanceDAO, users);
    GroupPrivilegeResourceProvider provider = new GroupPrivilegeResourceProvider();
    GroupPrivilegeResponse response = provider.getResponse(privilegeEntity, "group1");
    Resource resource = provider.toResource(response, provider.getPropertyIds());

    Assert.assertEquals("TestCluster", resource.getPropertyValue(ClusterPrivilegeResourceProvider.CLUSTER_NAME));
    Assert.assertEquals(ResourceType.CLUSTER.name(), resource.getPropertyValue(GroupPrivilegeResourceProvider.TYPE));

    verifyAll();
  }

  @Test
  public void testToResource_VIEW() {
    PermissionEntity permissionEntity = createMock(PermissionEntity.class);
    expect(permissionEntity.getPermissionName()).andReturn("CLUSTER.ADMINISTRATOR").atLeastOnce();
    expect(permissionEntity.getPermissionLabel()).andReturn("Cluster Administrator").atLeastOnce();

    PrincipalTypeEntity principalTypeEntity = createMock(PrincipalTypeEntity.class);
    expect(principalTypeEntity.getName()).andReturn("GROUP").atLeastOnce();

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

    GroupEntity groupEntity = createMock(GroupEntity.class);
    expect(groupEntity.getGroupName()).andReturn("group1").atLeastOnce();

    ClusterDAO clusterDAO = createMock(ClusterDAO.class);

    ViewInstanceDAO viewInstanceDAO = createMock(ViewInstanceDAO.class);
    expect(viewInstanceDAO.findByResourceId(1L)).andReturn(viewInstanceEntity).atLeastOnce();

    GroupDAO groupDAO = createMock(GroupDAO.class);
    expect(groupDAO.findGroupByPrincipal(anyObject(PrincipalEntity.class))).andReturn(groupEntity).anyTimes();

    Users users = createNiceMock(Users.class);

    replayAll();

    GroupPrivilegeResourceProvider.init(clusterDAO, groupDAO, viewInstanceDAO, users);
    GroupPrivilegeResourceProvider provider = new GroupPrivilegeResourceProvider();
    GroupPrivilegeResponse response = provider.getResponse(privilegeEntity, "group1");
    Resource resource = provider.toResource(response, provider.getPropertyIds());

    Assert.assertEquals("Test View", resource.getPropertyValue(ViewPrivilegeResourceProvider.INSTANCE_NAME));
    Assert.assertEquals("TestView", resource.getPropertyValue(ViewPrivilegeResourceProvider.VIEW_NAME));
    Assert.assertEquals("1.2.3.4", resource.getPropertyValue(ViewPrivilegeResourceProvider.VERSION));
    Assert.assertEquals(ResourceType.VIEW.name(), resource.getPropertyValue(GroupPrivilegeResourceProvider.TYPE));

    verifyAll();
  }

  @Test
  public void testToResource_SpecificVIEW() {
    PermissionEntity permissionEntity = createMock(PermissionEntity.class);
    expect(permissionEntity.getPermissionName()).andReturn("CLUSTER.ADMINISTRATOR").atLeastOnce();
    expect(permissionEntity.getPermissionLabel()).andReturn("Cluster Administrator").atLeastOnce();

    PrincipalTypeEntity principalTypeEntity = createMock(PrincipalTypeEntity.class);
    expect(principalTypeEntity.getName()).andReturn("GROUP").atLeastOnce();

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

    GroupEntity groupEntity = createMock(GroupEntity.class);
    expect(groupEntity.getGroupName()).andReturn("group1").atLeastOnce();

    ClusterDAO clusterDAO = createMock(ClusterDAO.class);

    ViewInstanceDAO viewInstanceDAO = createMock(ViewInstanceDAO.class);
    expect(viewInstanceDAO.findByResourceId(1L)).andReturn(viewInstanceEntity).atLeastOnce();

    GroupDAO groupDAO = createMock(GroupDAO.class);
    expect(groupDAO.findGroupByPrincipal(anyObject(PrincipalEntity.class))).andReturn(groupEntity).anyTimes();
    Users users = createNiceMock(Users.class);

    replayAll();

    GroupPrivilegeResourceProvider.init(clusterDAO, groupDAO, viewInstanceDAO, users);
    GroupPrivilegeResourceProvider provider = new GroupPrivilegeResourceProvider();
    GroupPrivilegeResponse response = provider.getResponse(privilegeEntity, "group1");
    Resource resource = provider.toResource(response, provider.getPropertyIds());

    Assert.assertEquals("Test View", resource.getPropertyValue(ViewPrivilegeResourceProvider.INSTANCE_NAME));
    Assert.assertEquals("TestView", resource.getPropertyValue(ViewPrivilegeResourceProvider.VIEW_NAME));
    Assert.assertEquals("1.2.3.4", resource.getPropertyValue(ViewPrivilegeResourceProvider.VERSION));
    Assert.assertEquals(ResourceType.VIEW.name(), resource.getPropertyValue(GroupPrivilegeResourceProvider.TYPE));

    verifyAll();
  }

  private void getResourcesTest(Authentication authentication, String requestedGroupName) throws Exception {
    final GroupPrivilegeResourceProvider resourceProvider = new GroupPrivilegeResourceProvider();
    final GroupDAO groupDAO = createNiceMock(GroupDAO.class);
    final ClusterDAO clusterDAO = createNiceMock(ClusterDAO.class);
    final ViewInstanceDAO viewInstanceDAO = createNiceMock(ViewInstanceDAO.class);
    final GroupEntity groupEntity = createNiceMock(GroupEntity.class);
    final PrincipalEntity principalEntity = createNiceMock(PrincipalEntity.class);
    final PrivilegeEntity privilegeEntity = createNiceMock(PrivilegeEntity.class);
    final PermissionEntity permissionEntity = createNiceMock(PermissionEntity.class);
    final PrincipalTypeEntity principalTypeEntity = createNiceMock(PrincipalTypeEntity.class);
    final ResourceEntity resourceEntity = createNiceMock(ResourceEntity.class);
    final ResourceTypeEntity resourceTypeEntity = createNiceMock(ResourceTypeEntity.class);
    final PrivilegeDAO privilegeDAO = createMock(PrivilegeDAO.class);

    final Injector injector = Guice.createInjector(new AbstractModule() {
                                                     @Override
                                                     protected void configure() {
                                                       bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
                                                       bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
                                                       bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
                                                       bind(PasswordEncoder.class).toInstance(createNiceMock(PasswordEncoder.class));
                                                       bind(HookService.class).toInstance(createMock(HookService.class));
                                                       bind(HookContextFactory.class).toInstance(createMock(HookContextFactory.class));
                                                       bind(new TypeLiteral<Encryptor<AmbariServerConfiguration>>() {}).annotatedWith(Names.named("AmbariServerConfigurationEncryptor")).toInstance(Encryptor.NONE);
                                                       bind(AmbariLdapConfigurationProvider.class).toInstance(createMock(AmbariLdapConfigurationProvider.class));

                                                       bind(GroupDAO.class).toInstance(groupDAO);
                                                       bind(ClusterDAO.class).toInstance(clusterDAO);
                                                       bind(ViewInstanceDAO.class).toInstance(viewInstanceDAO);
                                                       bind(GroupEntity.class).toInstance(groupEntity);
                                                       bind(PrincipalEntity.class).toInstance(principalEntity);
                                                       bind(PrivilegeEntity.class).toInstance(privilegeEntity);
                                                       bind(PermissionEntity.class).toInstance(permissionEntity);
                                                       bind(PrincipalTypeEntity.class).toInstance(principalTypeEntity);
                                                       bind(ResourceEntity.class).toInstance(resourceEntity);
                                                       bind(ResourceTypeEntity.class).toInstance(resourceTypeEntity);
                                                       bind(PrivilegeDAO.class).toInstance(privilegeDAO);
                                                     }
                                                   }
    );

    final Users users = injector.getInstance(Users.class);

    List<PrincipalEntity> groupPrincipals = new LinkedList<>();
    groupPrincipals.add(principalEntity);

    expect(privilegeDAO.findAllByPrincipal(groupPrincipals)).
        andReturn(Collections.singletonList(privilegeEntity))
        .once();
    expect(groupDAO.findGroupByName(requestedGroupName)).andReturn(groupEntity).atLeastOnce();
    expect(groupEntity.getPrincipal()).andReturn(principalEntity).atLeastOnce();
    expect(privilegeEntity.getPermission()).andReturn(permissionEntity).atLeastOnce();
    expect(privilegeEntity.getPrincipal()).andReturn(principalEntity).atLeastOnce();
    expect(principalEntity.getPrincipalType()).andReturn(principalTypeEntity).atLeastOnce();
    expect(principalTypeEntity.getName()).andReturn(PrincipalTypeEntity.GROUP_PRINCIPAL_TYPE_NAME).atLeastOnce();
    expect(groupDAO.findGroupByPrincipal(anyObject(PrincipalEntity.class))).andReturn(groupEntity).atLeastOnce();
    expect(groupEntity.getGroupName()).andReturn(requestedGroupName).atLeastOnce();
    expect(privilegeEntity.getResource()).andReturn(resourceEntity).atLeastOnce();
    expect(resourceEntity.getResourceType()).andReturn(resourceTypeEntity).atLeastOnce();
    expect(resourceTypeEntity.getName()).andReturn(ResourceType.AMBARI.name());

    replayAll();

    GroupPrivilegeResourceProvider.init(clusterDAO, groupDAO, viewInstanceDAO, users);

    final Set<String> propertyIds = new HashSet<>();
    propertyIds.add(GroupPrivilegeResourceProvider.GROUP_NAME);

    final Predicate predicate = new PredicateBuilder()
        .property(GroupPrivilegeResourceProvider.GROUP_NAME)
        .equals(requestedGroupName)
        .toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);

    // Set the authenticated group to a administrator
    SecurityContextHolder.getContext().setAuthentication(authentication);

    Set<Resource> resources = resourceProvider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      String groupName = (String) resource.getPropertyValue(GroupPrivilegeResourceProvider.GROUP_NAME);
      Assert.assertEquals(requestedGroupName, groupName);
    }

    verifyAll();
  }
}
