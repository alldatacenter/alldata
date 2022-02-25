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

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactoryImpl;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.AmbariServerConfiguration;
import org.apache.ambari.server.controller.AbstractRootServiceResponseFactory;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.events.AgentConfigsUpdateEvent;
import org.apache.ambari.server.hooks.HookContextFactory;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.ldap.service.AmbariLdapConfigurationProvider;
import org.apache.ambari.server.metadata.CachedRoleCommandOrderProvider;
import org.apache.ambari.server.metadata.RoleCommandOrderProvider;
import org.apache.ambari.server.mpack.MpackManagerFactory;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.RoleAuthorizationEntity;
import org.apache.ambari.server.scheduler.ExecutionScheduler;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.security.encryption.CredentialStoreServiceImpl;
import org.apache.ambari.server.security.encryption.Encryptor;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContextFactory;
import org.apache.ambari.server.stageplanner.RoleGraphFactory;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.topology.PersistedState;
import org.apache.ambari.server.topology.PersistedStateImpl;
import org.apache.ambari.server.topology.tasks.ConfigureClusterTaskFactory;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;


/**
 * UserAuthorizationResourceProvider tests.
 */
public class UserAuthorizationResourceProviderTest extends EasyMockSupport {

  @Before
  public void setup() throws Exception {
    resetAll();
  }

  @After
  public void cleanup() {
    SecurityContextHolder.getContext().setAuthentication(null);
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
  public void testCreateResources() throws Exception {
    Injector injector = createInjector();

    replayAll();
    // Set the authenticated user to a non-administrator
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createClusterAdministrator("user1", 2L));
    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);
    UserAuthorizationResourceProvider provider = new UserAuthorizationResourceProvider(managementController);
    provider.createResources(createNiceMock(Request.class));
    verifyAll();
  }

  @Test(expected = SystemException.class)
  public void testUpdateResources() throws Exception {
    Injector injector = createInjector();

    replayAll();
    // Set the authenticated user to a non-administrator
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createClusterAdministrator("user1", 2L));
    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);
    UserAuthorizationResourceProvider provider = new UserAuthorizationResourceProvider(managementController);
    provider.updateResources(createNiceMock(Request.class), null);
    verifyAll();
  }

  @Test(expected = SystemException.class)
  public void testDeleteResources() throws Exception {
    Injector injector = createInjector();

    replayAll();
    // Set the authenticated user to a non-administrator
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createClusterAdministrator("user1", 2L));
    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);
    UserAuthorizationResourceProvider provider = new UserAuthorizationResourceProvider(managementController);
    provider.deleteResources(createNiceMock(Request.class), null);
    verifyAll();
  }

  private void getResourcesTest(Authentication authentication, String requestedUsername) throws Exception {
    Injector injector = createInjector();

    Resource clusterResource = createMock(Resource.class);
    expect(clusterResource.getPropertyValue(UserPrivilegeResourceProvider.PERMISSION_NAME))
        .andReturn("CLUSTER.DO_SOMETHING")
        .anyTimes();
    expect(clusterResource.getPropertyValue(UserPrivilegeResourceProvider.TYPE))
        .andReturn("CLUSTER")
        .anyTimes();
    expect(clusterResource.getPropertyValue(UserPrivilegeResourceProvider.CLUSTER_NAME))
        .andReturn("Cluster Name")
        .anyTimes();

    Resource viewResource = createMock(Resource.class);
    expect(viewResource.getPropertyValue(UserPrivilegeResourceProvider.PERMISSION_NAME))
        .andReturn("VIEW.DO_SOMETHING")
        .anyTimes();
    expect(viewResource.getPropertyValue(UserPrivilegeResourceProvider.TYPE))
        .andReturn("VIEW")
        .anyTimes();
    expect(viewResource.getPropertyValue(UserPrivilegeResourceProvider.VIEW_NAME))
        .andReturn("View Name")
        .anyTimes();
    expect(viewResource.getPropertyValue(UserPrivilegeResourceProvider.VIEW_VERSION))
        .andReturn("View Version")
        .anyTimes();
    expect(viewResource.getPropertyValue(UserPrivilegeResourceProvider.INSTANCE_NAME))
        .andReturn("View Instance Name")
        .anyTimes();

    Resource adminResource = createMock(Resource.class);
    expect(adminResource.getPropertyValue(UserPrivilegeResourceProvider.PERMISSION_NAME))
        .andReturn("ADMIN.DO_SOMETHING")
        .anyTimes();
    expect(adminResource.getPropertyValue(UserPrivilegeResourceProvider.TYPE))
        .andReturn("ADMIN")
        .anyTimes();
    expect(adminResource.getPropertyValue(UserPrivilegeResourceProvider.CLUSTER_NAME))
        .andReturn(null)
        .anyTimes();

    Resource emptyResource = createMock(Resource.class);
    expect(emptyResource.getPropertyValue(UserPrivilegeResourceProvider.PERMISSION_NAME))
        .andReturn("EMPTY.DO_SOMETHING")
        .anyTimes();
    expect(emptyResource.getPropertyValue(UserPrivilegeResourceProvider.TYPE))
        .andReturn("ADMIN")
        .anyTimes();
    expect(emptyResource.getPropertyValue(UserPrivilegeResourceProvider.CLUSTER_NAME))
        .andReturn(null)
        .anyTimes();

    Resource nullResource = createMock(Resource.class);
    expect(nullResource.getPropertyValue(UserPrivilegeResourceProvider.PERMISSION_NAME))
        .andReturn("NULL.DO_SOMETHING")
        .anyTimes();
    expect(nullResource.getPropertyValue(UserPrivilegeResourceProvider.TYPE))
        .andReturn("ADMIN")
        .anyTimes();
    expect(nullResource.getPropertyValue(UserPrivilegeResourceProvider.CLUSTER_NAME))
        .andReturn(null)
        .anyTimes();

    Set<Resource> userPrivilegeResources = new HashSet<>();
    userPrivilegeResources.add(clusterResource);
    userPrivilegeResources.add(viewResource);
    userPrivilegeResources.add(adminResource);
    userPrivilegeResources.add(emptyResource);
    userPrivilegeResources.add(nullResource);

    ResourceProvider userPrivilegeProvider = createMock(ResourceProvider.class);
    expect(userPrivilegeProvider.getResources(anyObject(Request.class), anyObject(Predicate.class)))
        .andReturn(userPrivilegeResources);

    ClusterController clusterController = createMock(ClusterController.class);
    expect(clusterController.ensureResourceProvider(Resource.Type.UserPrivilege))
        .andReturn(userPrivilegeProvider)
        .anyTimes();

    ResourceTypeEntity clusterResourceTypeEntity = createMock(ResourceTypeEntity.class);
    expect(clusterResourceTypeEntity.getId()).andReturn(1).anyTimes();

    ResourceTypeEntity viewResourceTypeEntity = createMock(ResourceTypeEntity.class);
    expect(viewResourceTypeEntity.getId()).andReturn(2).anyTimes();

    ResourceTypeEntity adminResourceTypeEntity = createMock(ResourceTypeEntity.class);
    expect(adminResourceTypeEntity.getId()).andReturn(3).anyTimes();

    ResourceTypeDAO resourceTypeDAO = injector.getInstance(ResourceTypeDAO.class);
    expect(resourceTypeDAO.findByName("CLUSTER")).andReturn(clusterResourceTypeEntity).anyTimes();
    expect(resourceTypeDAO.findByName("VIEW")).andReturn(viewResourceTypeEntity).anyTimes();
    expect(resourceTypeDAO.findByName("ADMIN")).andReturn(adminResourceTypeEntity).anyTimes();

    RoleAuthorizationEntity clusterRoleAuthorizationEntity = createMock(RoleAuthorizationEntity.class);
    expect(clusterRoleAuthorizationEntity.getAuthorizationId()).andReturn("CLUSTER.DO_SOMETHING").anyTimes();
    expect(clusterRoleAuthorizationEntity.getAuthorizationName()).andReturn("CLUSTER DO_SOMETHING").anyTimes();

    RoleAuthorizationEntity viewRoleAuthorizationEntity = createMock(RoleAuthorizationEntity.class);
    expect(viewRoleAuthorizationEntity.getAuthorizationId()).andReturn("VIEW.DO_SOMETHING").anyTimes();
    expect(viewRoleAuthorizationEntity.getAuthorizationName()).andReturn("VIEW DO_SOMETHING").anyTimes();

    RoleAuthorizationEntity adminRoleAuthorizationEntity = createMock(RoleAuthorizationEntity.class);
    expect(adminRoleAuthorizationEntity.getAuthorizationId()).andReturn("ADMIN.DO_SOMETHING").anyTimes();
    expect(adminRoleAuthorizationEntity.getAuthorizationName()).andReturn("ADMIN DO_SOMETHING").anyTimes();

    Collection<RoleAuthorizationEntity> clusterPermissionAuthorizations = Collections.singleton(clusterRoleAuthorizationEntity);
    Collection<RoleAuthorizationEntity> viewPermissionAuthorizations = Collections.singleton(viewRoleAuthorizationEntity);
    Collection<RoleAuthorizationEntity> adminPermissionAuthorizations = Collections.singleton(adminRoleAuthorizationEntity);

    PermissionEntity clusterPermissionEntity = createMock(PermissionEntity.class);
    expect(clusterPermissionEntity.getAuthorizations())
        .andReturn(clusterPermissionAuthorizations)
        .anyTimes();

    PermissionEntity viewPermissionEntity = createMock(PermissionEntity.class);
    expect(viewPermissionEntity.getAuthorizations())
        .andReturn(viewPermissionAuthorizations)
        .anyTimes();

    PermissionEntity adminPermissionEntity = createMock(PermissionEntity.class);
    expect(adminPermissionEntity.getAuthorizations())
        .andReturn(adminPermissionAuthorizations)
        .anyTimes();

    PermissionEntity emptyPermissionEntity = createMock(PermissionEntity.class);
    expect(emptyPermissionEntity.getAuthorizations())
        .andReturn(Collections.emptyList())
        .anyTimes();

    PermissionEntity nullPermissionEntity = createMock(PermissionEntity.class);
    expect(nullPermissionEntity.getAuthorizations())
        .andReturn(null)
        .anyTimes();

    PermissionDAO permissionDAO = injector.getInstance(PermissionDAO.class);
    expect(permissionDAO.findPermissionByNameAndType("CLUSTER.DO_SOMETHING", clusterResourceTypeEntity))
        .andReturn(clusterPermissionEntity)
        .anyTimes();
    expect(permissionDAO.findPermissionByNameAndType("VIEW.DO_SOMETHING", viewResourceTypeEntity))
        .andReturn(viewPermissionEntity)
        .anyTimes();
    expect(permissionDAO.findPermissionByNameAndType("ADMIN.DO_SOMETHING", adminResourceTypeEntity))
        .andReturn(adminPermissionEntity)
        .anyTimes();
    expect(permissionDAO.findPermissionByNameAndType("EMPTY.DO_SOMETHING", adminResourceTypeEntity))
        .andReturn(emptyPermissionEntity)
        .anyTimes();
    expect(permissionDAO.findPermissionByNameAndType("NULL.DO_SOMETHING", adminResourceTypeEntity))
        .andReturn(nullPermissionEntity)
        .anyTimes();

    replayAll();

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    // Set the authenticated user to a administrator
    SecurityContextHolder.getContext().setAuthentication(authentication);

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);

    UserAuthorizationResourceProvider.init(permissionDAO, resourceTypeDAO);
    UserAuthorizationResourceProvider provider = new UserAuthorizationResourceProvider(managementController);
    setClusterController(provider, clusterController);

    Predicate predicate = new PredicateBuilder()
        .property(UserAuthorizationResourceProvider.USERNAME_PROPERTY_ID).equals(requestedUsername)
        .toPredicate();

    Set<Resource> resources = provider.getResources(PropertyHelper.getReadRequest(), predicate);

    Assert.assertEquals(3, resources.size());

    LinkedList<String> expectedIds = new LinkedList<>();
    expectedIds.add("CLUSTER.DO_SOMETHING");
    expectedIds.add("VIEW.DO_SOMETHING");
    expectedIds.add("ADMIN.DO_SOMETHING");

    for (Resource resource : resources) {
      String authorizationId = (String) resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_ID_PROPERTY_ID);

      switch (authorizationId) {
        case "CLUSTER.DO_SOMETHING":
          Assert.assertEquals("CLUSTER DO_SOMETHING", resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_NAME_PROPERTY_ID));
          Assert.assertEquals("CLUSTER", resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_RESOURCE_TYPE_PROPERTY_ID));
          Assert.assertEquals("Cluster Name", resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_CLUSTER_NAME_PROPERTY_ID));
          Assert.assertNull(resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_VIEW_NAME_PROPERTY_ID));
          Assert.assertNull(resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_VIEW_VERSION_PROPERTY_ID));
          Assert.assertNull(resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_VIEW_INSTANCE_NAME_PROPERTY_ID));
          break;
        case "VIEW.DO_SOMETHING":
          Assert.assertEquals("VIEW DO_SOMETHING", resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_NAME_PROPERTY_ID));
          Assert.assertEquals("VIEW", resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_RESOURCE_TYPE_PROPERTY_ID));
          Assert.assertEquals("View Name", resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_VIEW_NAME_PROPERTY_ID));
          Assert.assertEquals("View Version", resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_VIEW_VERSION_PROPERTY_ID));
          Assert.assertEquals("View Instance Name", resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_VIEW_INSTANCE_NAME_PROPERTY_ID));
          Assert.assertNull(resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_CLUSTER_NAME_PROPERTY_ID));
          break;
        case "ADMIN.DO_SOMETHING":
          Assert.assertEquals("ADMIN DO_SOMETHING", resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_NAME_PROPERTY_ID));
          Assert.assertEquals("ADMIN", resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_RESOURCE_TYPE_PROPERTY_ID));
          Assert.assertNull(resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_CLUSTER_NAME_PROPERTY_ID));
          Assert.assertNull(resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_VIEW_NAME_PROPERTY_ID));
          Assert.assertNull(resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_VIEW_VERSION_PROPERTY_ID));
          Assert.assertNull(resource.getPropertyValue(UserAuthorizationResourceProvider.AUTHORIZATION_VIEW_INSTANCE_NAME_PROPERTY_ID));
          break;
      }

      expectedIds.remove();
    }

    Assert.assertEquals(0, expectedIds.size());

    verifyAll();
  }

  private void setClusterController(UserAuthorizationResourceProvider provider, ClusterController clusterController) throws Exception {
    Class<?> c = provider.getClass();
    Field f = c.getDeclaredField("clusterController");
    f.setAccessible(true);
    f.set(provider, clusterController);
  }

  private Injector createInjector() throws Exception {
    return Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        install(new FactoryModuleBuilder().build(UpgradeContextFactory.class));
        install(new FactoryModuleBuilder().build(RoleGraphFactory.class));
        install(new FactoryModuleBuilder().build(ConfigureClusterTaskFactory.class));

        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(ActionDBAccessor.class).toInstance(createNiceMock(ActionDBAccessor.class));
        bind(ExecutionScheduler.class).toInstance(createNiceMock(ExecutionScheduler.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(AmbariMetaInfo.class).toInstance(createMock(AmbariMetaInfo.class));
        bind(ActionManager.class).toInstance(createNiceMock(ActionManager.class));
        bind(org.apache.ambari.server.actionmanager.RequestFactory.class).toInstance(createNiceMock(org.apache.ambari.server.actionmanager.RequestFactory.class));
        bind(RequestExecutionFactory.class).toInstance(createNiceMock(RequestExecutionFactory.class));
        bind(StageFactory.class).toInstance(createNiceMock(StageFactory.class));
        bind(Clusters.class).toInstance(createNiceMock(Clusters.class));
        bind(AbstractRootServiceResponseFactory.class).toInstance(createNiceMock(AbstractRootServiceResponseFactory.class));
        bind(StackManagerFactory.class).toInstance(createNiceMock(StackManagerFactory.class));
        bind(ConfigFactory.class).toInstance(createNiceMock(ConfigFactory.class));
        bind(ConfigGroupFactory.class).toInstance(createNiceMock(ConfigGroupFactory.class));
        bind(ServiceFactory.class).toInstance(createNiceMock(ServiceFactory.class));
        bind(ServiceComponentFactory.class).toInstance(createNiceMock(ServiceComponentFactory.class));
        bind(ServiceComponentHostFactory.class).toInstance(createNiceMock(ServiceComponentHostFactory.class));
        bind(PasswordEncoder.class).toInstance(createNiceMock(PasswordEncoder.class));
        bind(KerberosHelper.class).toInstance(createNiceMock(KerberosHelper.class));
        bind(Users.class).toInstance(createMock(Users.class));
        bind(AmbariManagementController.class).to(AmbariManagementControllerImpl.class);
        bind(RoleCommandOrderProvider.class).to(CachedRoleCommandOrderProvider.class);
        bind(CredentialStoreService.class).to(CredentialStoreServiceImpl.class);
        bind(UserDAO.class).toInstance(createMock(UserDAO.class));
        bind(ResourceTypeDAO.class).toInstance(createMock(ResourceTypeDAO.class));
        bind(PermissionDAO.class).toInstance(createMock(PermissionDAO.class));
        bind(HostRoleCommandDAO.class).toInstance(createMock(HostRoleCommandDAO.class));
        bind(HookContextFactory.class).toInstance(createMock(HookContextFactory.class));
        bind(HookService.class).toInstance(createMock(HookService.class));
        bind(HostRoleCommandFactory.class).to(HostRoleCommandFactoryImpl.class);
        bind(PersistedState.class).to(PersistedStateImpl.class);
        bind(MpackManagerFactory.class).toInstance(createNiceMock(MpackManagerFactory.class));
        bind(AmbariLdapConfigurationProvider.class).toInstance(createMock(AmbariLdapConfigurationProvider.class));

        bind(new TypeLiteral<Encryptor<AgentConfigsUpdateEvent>>() {}).annotatedWith(Names.named("AgentConfigEncryptor")).toInstance(Encryptor.NONE);
        bind(new TypeLiteral<Encryptor<AmbariServerConfiguration>>() {}).annotatedWith(Names.named("AmbariServerConfigurationEncryptor")).toInstance(Encryptor.NONE);
      }
    });
  }
}
