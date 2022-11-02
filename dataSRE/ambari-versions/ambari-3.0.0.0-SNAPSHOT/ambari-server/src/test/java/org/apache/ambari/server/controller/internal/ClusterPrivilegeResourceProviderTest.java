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
import static org.easymock.EasyMock.expectLastCall;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.GroupDAO;
import org.apache.ambari.server.orm.dao.MemberDAO;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.PrincipalDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.ResourceDAO;
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
import org.apache.ambari.server.security.SecurityHelper;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.view.ViewInstanceHandlerList;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * ClusterPrivilegeResourceProvider tests.
 */
public class ClusterPrivilegeResourceProviderTest extends EasyMockSupport {

  @Before
  public void resetGlobalMocks() {
    resetAll();
  }

  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testCreateResources_Administrator() throws Exception {
    createResourcesTest(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResources_NonAdministrator() throws Exception {
    createResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L));
  }

  @Test
  public void testGetResources_Administrator() throws Exception {
    getResourcesTest(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResources_NonAdministrator() throws Exception {
    getResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L));
  }

  @Test
  public void testGetResource_Administrator_Self() throws Exception {
    getResourceTest(TestAuthenticationFactory.createAdministrator("admin"), "admin");
  }

  @Test
  public void testGetResource_Administrator_Other() throws Exception {
    getResourceTest(TestAuthenticationFactory.createAdministrator("admin"), "User1");
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResource_NonAdministrator_Self() throws Exception {
    getResourceTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User1");
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResource_NonAdministrator_Other() throws Exception {
    getResourceTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User10");
  }

  @Test
  public void testUpdateResources_Administrator_Self() throws Exception {
    updateResourcesTest(TestAuthenticationFactory.createAdministrator("admin"), "admin");
  }

  @Test
  public void testUpdateResources_Administrator_Other() throws Exception {
    updateResourcesTest(TestAuthenticationFactory.createAdministrator("admin"), "User1");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_NonAdministrator_Self() throws Exception {
    updateResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User1");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_NonAdministrator_Other() throws Exception {
    updateResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User10");
  }

  @Test
  public void testDeleteResources_Administrator() throws Exception {
    deleteResourcesTest(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResources_NonAdministrator() throws Exception {
    deleteResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L));
  }


  private void createResourcesTest(Authentication authentication) throws Exception {
    Injector injector = createInjector();

    PrincipalEntity principalEntity = createMockPrincipalEntity(2L, createMockPrincipalTypeEntity("USER"));

    ResourceTypeEntity clusterResourceTypeEntity = createMockResourceTypeEntity(ResourceType.CLUSTER);
    ResourceEntity clusterResourceEntity = createMockResourceEntity(1L, clusterResourceTypeEntity);
    PermissionEntity permissionEntity = createMockPermissionEntity("CLUSTER.OPERATOR", "Cluster Operator", clusterResourceTypeEntity);
    PrivilegeEntity privilegeEntity = createMockPrivilegeEntity(2, clusterResourceEntity, principalEntity, permissionEntity);
    ClusterEntity clusterEntity = createMockClusterEntity("c1", clusterResourceEntity);
    UserEntity userEntity = createMockUserEntity(principalEntity, "User1");

    Set<PrivilegeEntity> privilegeEntities = new HashSet<>();
    privilegeEntities.add(privilegeEntity);

    expect(principalEntity.getPrivileges()).andReturn(privilegeEntities).atLeastOnce();

    PrivilegeDAO privilegeDAO = injector.getInstance(PrivilegeDAO.class);
    expect(privilegeDAO.exists(anyObject(PrivilegeEntity.class))).andReturn(false).atLeastOnce();
    privilegeDAO.create(anyObject(PrivilegeEntity.class));
    expectLastCall().once();

    UserDAO userDAO = injector.getInstance(UserDAO.class);
    expect(userDAO.findUserByName("User1")).andReturn(userEntity).atLeastOnce();

    PrincipalDAO principalDAO = injector.getInstance(PrincipalDAO.class);
    expect(principalDAO.findById(2L)).andReturn(principalEntity).atLeastOnce();
    expect(principalDAO.merge(principalEntity)).andReturn(principalEntity).once();

    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    expect(clusterDAO.findByName("c1")).andReturn(clusterEntity).atLeastOnce();

        ResourceDAO resourceDAO = injector.getInstance(ResourceDAO.class);
    expect(resourceDAO.findById(1L)).andReturn(clusterResourceEntity).atLeastOnce();

    PermissionDAO permissionDAO = injector.getInstance(PermissionDAO.class);
    expect(permissionDAO.findPermissionByNameAndType("CLUSTER.OPERATOR", clusterResourceTypeEntity))
        .andReturn(permissionEntity)
        .atLeastOnce();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(PrivilegeResourceProvider.PERMISSION_NAME, "CLUSTER.OPERATOR");
    properties.put(PrivilegeResourceProvider.PRINCIPAL_NAME, "User1");
    properties.put(PrivilegeResourceProvider.PRINCIPAL_TYPE, "USER");
    properties.put(ClusterPrivilegeResourceProvider.CLUSTER_NAME, "c1");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    ResourceProvider provider = getResourceProvider(injector);
    provider.createResources(request);

    verifyAll();
  }

  private void getResourcesTest(Authentication authentication) throws Exception {
    Injector injector = createInjector();

    ResourceTypeEntity resourceTypeEntity = createMockResourceTypeEntity(ResourceType.CLUSTER);
    ResourceEntity resourceEntity = createMockResourceEntity(20L, resourceTypeEntity);
    PrincipalTypeEntity principalTypeEntity = createMockPrincipalTypeEntity("USER");
    PrincipalEntity principalEntity = createMockPrincipalEntity(20L, principalTypeEntity);
    PermissionEntity permissionEntity = createMockPermissionEntity("CLUSTER.ADMINISTRATOR", "Cluster Administrator", resourceTypeEntity);
    PrivilegeEntity privilegeEntity = createMockPrivilegeEntity(1, resourceEntity, principalEntity, permissionEntity);
    ClusterEntity clusterEntity = createMockClusterEntity("c1", resourceEntity);
    UserEntity userEntity = createMockUserEntity(principalEntity, "joe");

    List<PrincipalEntity> principalEntities = new LinkedList<>();
    principalEntities.add(principalEntity);

    List<UserEntity> userEntities = new LinkedList<>();
    userEntities.add(userEntity);

    List<PrivilegeEntity> privilegeEntities = new LinkedList<>();
    privilegeEntities.add(privilegeEntity);

    PrivilegeDAO privilegeDAO = injector.getInstance(PrivilegeDAO.class);
    expect(privilegeDAO.findAll()).andReturn(privilegeEntities);

    List<ClusterEntity> clusterEntities = new LinkedList<>();
    clusterEntities.add(clusterEntity);

    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    expect(clusterDAO.findAll()).andReturn(clusterEntities);

    UserDAO userDAO = injector.getInstance(UserDAO.class);
    expect(userDAO.findUsersByPrincipal(principalEntities)).andReturn(userEntities);

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = getResourceProvider(injector);
    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), null);

    Assert.assertEquals(1, resources.size());

    Resource resource = resources.iterator().next();

    Assert.assertEquals("CLUSTER.ADMINISTRATOR", resource.getPropertyValue(AmbariPrivilegeResourceProvider.PERMISSION_NAME));
    Assert.assertEquals("Cluster Administrator", resource.getPropertyValue(AmbariPrivilegeResourceProvider.PERMISSION_LABEL));
    Assert.assertEquals("joe", resource.getPropertyValue(AmbariPrivilegeResourceProvider.PRINCIPAL_NAME));
    Assert.assertEquals("USER", resource.getPropertyValue(AmbariPrivilegeResourceProvider.PRINCIPAL_TYPE));

    verifyAll();
  }

  private void getResourceTest(Authentication authentication, String requestedUsername) throws Exception {
    Injector injector = createInjector();

    ResourceTypeEntity resourceTypeEntity = createMockResourceTypeEntity(ResourceType.CLUSTER);
    ResourceEntity resourceEntity = createMockResourceEntity(20L, resourceTypeEntity);
    PrincipalTypeEntity principalTypeEntity = createMockPrincipalTypeEntity("USER");
    PrincipalEntity principalEntity = createMockPrincipalEntity(20L, principalTypeEntity);
    PermissionEntity permissionEntity = createMockPermissionEntity("CLUSTER.ADMINISTRATOR", "Cluster Administrator", resourceTypeEntity);
    PrivilegeEntity privilegeEntity = createMockPrivilegeEntity(1, resourceEntity, principalEntity, permissionEntity);
    ClusterEntity clusterEntity = createMockClusterEntity("c1", resourceEntity);
    UserEntity userEntity = createMockUserEntity(principalEntity, requestedUsername);

    List<PrincipalEntity> principalEntities = new LinkedList<>();
    principalEntities.add(principalEntity);

    List<UserEntity> userEntities = new LinkedList<>();
    userEntities.add(userEntity);

    List<PrivilegeEntity> privilegeEntities = new LinkedList<>();
    privilegeEntities.add(privilegeEntity);

    PrivilegeDAO privilegeDAO = injector.getInstance(PrivilegeDAO.class);
    expect(privilegeDAO.findAll()).andReturn(privilegeEntities);

    List<ClusterEntity> clusterEntities = new LinkedList<>();
    clusterEntities.add(clusterEntity);

    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    expect(clusterDAO.findAll()).andReturn(clusterEntities);

    UserDAO userDAO = injector.getInstance(UserDAO.class);
    expect(userDAO.findUsersByPrincipal(principalEntities)).andReturn(userEntities);

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = getResourceProvider(injector);
    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), null);

    Assert.assertEquals(1, resources.size());

    Resource resource = resources.iterator().next();

    Assert.assertEquals("CLUSTER.ADMINISTRATOR", resource.getPropertyValue(AmbariPrivilegeResourceProvider.PERMISSION_NAME));
    Assert.assertEquals("Cluster Administrator", resource.getPropertyValue(AmbariPrivilegeResourceProvider.PERMISSION_LABEL));
    Assert.assertEquals(requestedUsername, resource.getPropertyValue(AmbariPrivilegeResourceProvider.PRINCIPAL_NAME));
    Assert.assertEquals("USER", resource.getPropertyValue(AmbariPrivilegeResourceProvider.PRINCIPAL_TYPE));

    verifyAll();
  }

  private void updateResourcesTest(Authentication authentication, String requestedUsername) throws Exception {
    Injector injector = createInjector();

    ResourceTypeEntity resourceTypeEntity = createMockResourceTypeEntity(ResourceType.CLUSTER);
    PermissionEntity permissionEntity = createMockPermissionEntity("CLUSTER.ADMINISTRATOR", "Cluster Administrator", resourceTypeEntity);

    PermissionDAO permissionDAO = injector.getInstance(PermissionDAO.class);
    expect(permissionDAO.findPermissionByNameAndType("CLUSTER.OPERATOR", resourceTypeEntity)).andReturn(permissionEntity);

    ResourceEntity resourceEntity = createMockResourceEntity(2L, resourceTypeEntity);
    ClusterEntity clusterEntity = createMockClusterEntity("c1", resourceEntity);

    List<ClusterEntity> clusterEntities = new LinkedList<>();
    clusterEntities.add(clusterEntity);

    PrincipalTypeEntity principalTypeEntity = createMockPrincipalTypeEntity("USER");
    PrincipalEntity principalEntity = createMockPrincipalEntity(2L, principalTypeEntity);
    UserEntity userEntity = createMockUserEntity(principalEntity, requestedUsername);
    PrivilegeEntity privilegeEntity = createMockPrivilegeEntity(1, resourceEntity, principalEntity, permissionEntity);

    List<PrivilegeEntity> privilegeEntities = new ArrayList<>();
    privilegeEntities.add(privilegeEntity);

    UserDAO userDAO = injector.getInstance(UserDAO.class);
    expect(userDAO.findUserByName(requestedUsername)).andReturn(userEntity).atLeastOnce();

    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    expect(clusterDAO.findAll()).andReturn(clusterEntities);

    ResourceDAO resourceDAO = injector.getInstance(ResourceDAO.class);
    expect(resourceDAO.findById(2L)).andReturn(resourceEntity).atLeastOnce();

    PrincipalDAO principalDAO = injector.getInstance(PrincipalDAO.class);
    expect(principalDAO.findById(2L)).andReturn(principalEntity).atLeastOnce();

    PrivilegeDAO privilegeDAO = injector.getInstance(PrivilegeDAO.class);
    expect(privilegeDAO.findByResourceId(2L)).andReturn(privilegeEntities).atLeastOnce();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(PrivilegeResourceProvider.PERMISSION_NAME, "CLUSTER.OPERATOR");
    properties.put(PrivilegeResourceProvider.PRINCIPAL_NAME, requestedUsername);
    properties.put(PrivilegeResourceProvider.PRINCIPAL_TYPE, "USER");

    Request request = PropertyHelper.getUpdateRequest(properties, null);

    ResourceProvider provider = getResourceProvider(injector);
    provider.updateResources(request, null);

    verifyAll();
  }

  private void deleteResourcesTest(Authentication authentication) throws Exception {
    Injector injector = createInjector();

    PrincipalEntity principalEntity1 = createMockPrincipalEntity(1L, createMockPrincipalTypeEntity("USER"));

    ResourceTypeEntity clusterResourceTypeEntity = createMockResourceTypeEntity(ResourceType.CLUSTER);
    ResourceEntity clusterResourceEntity = createMockResourceEntity(1L, clusterResourceTypeEntity);
    PermissionEntity permissionEntity = createMockPermissionEntity("CLUSTER.OPERATOR", "Cluster Operator", clusterResourceTypeEntity);

    PrivilegeEntity privilegeEntity1 = createMockPrivilegeEntity(1, clusterResourceEntity, principalEntity1, permissionEntity);

    Set<PrivilegeEntity> privilege1Entities = new HashSet<>();
    privilege1Entities.add(privilegeEntity1);

    expect(principalEntity1.getPrivileges()).andReturn(privilege1Entities).atLeastOnce();

    PrivilegeDAO privilegeDAO = injector.getInstance(PrivilegeDAO.class);
    expect(privilegeDAO.findById(1)).andReturn(privilegeEntity1).atLeastOnce();
    privilegeDAO.remove(privilegeEntity1);
    expectLastCall().atLeastOnce();

    PrincipalDAO principalDAO = injector.getInstance(PrincipalDAO.class);
    expect(principalDAO.merge(principalEntity1)).andReturn(principalEntity1).atLeastOnce();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = getResourceProvider(injector);
    provider.deleteResources(new RequestImpl(null, null, null, null), createPredicate(1L));

    verifyAll();
  }


  private ResourceEntity createMockResourceEntity(Long id, ResourceTypeEntity resourceTypeEntity) {
    ResourceEntity resourceEntity = createMock(ResourceEntity.class);
    expect(resourceEntity.getId()).andReturn(id).anyTimes();
    expect(resourceEntity.getResourceType()).andReturn(resourceTypeEntity).anyTimes();
    return resourceEntity;
  }

  private ResourceTypeEntity createMockResourceTypeEntity(ResourceType resourceType) {
    ResourceTypeEntity resourceTypeEntity = createMock(ResourceTypeEntity.class);
    expect(resourceTypeEntity.getId()).andReturn(resourceType.getId()).anyTimes();
    expect(resourceTypeEntity.getName()).andReturn(resourceType.name()).anyTimes();
    return resourceTypeEntity;
  }

  private PermissionEntity createMockPermissionEntity(String name, String label, ResourceTypeEntity resourceTypeEntity) {
    PermissionEntity permissionEntity = createMock(PermissionEntity.class);
    expect(permissionEntity.getPermissionName()).andReturn(name).anyTimes();
    expect(permissionEntity.getPermissionLabel()).andReturn(label).anyTimes();
    expect(permissionEntity.getResourceType()).andReturn(resourceTypeEntity).anyTimes();
    return permissionEntity;
  }

  private PrincipalTypeEntity createMockPrincipalTypeEntity(String typeName) {
    PrincipalTypeEntity principalTypeEntity = createMock(PrincipalTypeEntity.class);
    expect(principalTypeEntity.getName()).andReturn(typeName).anyTimes();
    return principalTypeEntity;
  }

  private PrincipalEntity createMockPrincipalEntity(Long id, PrincipalTypeEntity principalTypeEntity) {
    PrincipalEntity principalEntity = createMock(PrincipalEntity.class);
    expect(principalEntity.getId()).andReturn(id).anyTimes();
    expect(principalEntity.getPrincipalType()).andReturn(principalTypeEntity).anyTimes();
    return principalEntity;
  }

  private PrivilegeEntity createMockPrivilegeEntity(Integer id, ResourceEntity resourceEntity, PrincipalEntity principalEntity, PermissionEntity permissionEntity) {
    PrivilegeEntity privilegeEntity = createMock(PrivilegeEntity.class);
    expect(privilegeEntity.getId()).andReturn(id).anyTimes();
    expect(privilegeEntity.getResource()).andReturn(resourceEntity).anyTimes();
    expect(privilegeEntity.getPrincipal()).andReturn(principalEntity).anyTimes();
    expect(privilegeEntity.getPermission()).andReturn(permissionEntity).anyTimes();
    return privilegeEntity;
  }

  private ClusterEntity createMockClusterEntity(String clusterName, ResourceEntity resourceEntity) {
    ClusterEntity clusterEntity = createMock(ClusterEntity.class);
    expect(clusterEntity.getClusterName()).andReturn(clusterName).anyTimes();
    expect(clusterEntity.getResource()).andReturn(resourceEntity).anyTimes();
    return clusterEntity;
  }

  private UserEntity createMockUserEntity(PrincipalEntity principalEntity, String username) {
    UserEntity userEntity = createMock(UserEntity.class);
    expect(userEntity.getPrincipal()).andReturn(principalEntity).anyTimes();
    expect(userEntity.getUserName()).andReturn(username).anyTimes();
    return userEntity;
  }


  private Predicate createPredicate(Long id) {
    return new PredicateBuilder()
        .property(ClusterPrivilegeResourceProvider.PRIVILEGE_ID)
        .equals(id)
        .toPredicate();
  }

  private ResourceProvider getResourceProvider(Injector injector) {
    PrivilegeResourceProvider.init(injector.getInstance(PrivilegeDAO.class),
        injector.getInstance(UserDAO.class),
        injector.getInstance(GroupDAO.class),
        injector.getInstance(PrincipalDAO.class),
        injector.getInstance(PermissionDAO.class),
        injector.getInstance(ResourceDAO.class));
    ClusterPrivilegeResourceProvider.init(injector.getInstance(ClusterDAO.class));
    return new ClusterPrivilegeResourceProvider();
  }

  private Injector createInjector() throws Exception {
    return Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(SecurityHelper.class).toInstance(createNiceMock(SecurityHelper.class));
        bind(ViewInstanceDAO.class).toInstance(createNiceMock(ViewInstanceDAO.class));
        bind(ViewInstanceHandlerList.class).toInstance(createNiceMock(ViewInstanceHandlerList.class));
        bind(MemberDAO.class).toInstance(createNiceMock(MemberDAO.class));

        bind(PrivilegeDAO.class).toInstance(createMock(PrivilegeDAO.class));
        bind(PrincipalDAO.class).toInstance(createMock(PrincipalDAO.class));
        bind(PermissionDAO.class).toInstance(createMock(PermissionDAO.class));
        bind(UserDAO.class).toInstance(createMock(UserDAO.class));
        bind(GroupDAO.class).toInstance(createMock(GroupDAO.class));
        bind(ResourceDAO.class).toInstance(createMock(ResourceDAO.class));
        bind(ClusterDAO.class).toInstance(createMock(ClusterDAO.class));
      }
    });
  }
}

