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
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.newCapture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactoryImpl;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.AmbariServerConfiguration;
import org.apache.ambari.server.controller.AbstractRootServiceResponseFactory;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.ResourceProviderFactory;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.events.AgentConfigsUpdateEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.hooks.HookContext;
import org.apache.ambari.server.hooks.HookContextFactory;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.ldap.service.AmbariLdapConfigurationProvider;
import org.apache.ambari.server.metadata.CachedRoleCommandOrderProvider;
import org.apache.ambari.server.metadata.RoleCommandOrderProvider;
import org.apache.ambari.server.mpack.MpackManagerFactory;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.GroupDAO;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.MemberDAO;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.PrincipalDAO;
import org.apache.ambari.server.orm.dao.PrincipalTypeDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.ResourceDAO;
import org.apache.ambari.server.orm.dao.UserAuthenticationDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.MemberEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.UserAuthenticationEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.scheduler.ExecutionScheduler;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.UserAuthenticationType;
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
import org.apache.ambari.server.view.ViewRegistry;
import org.apache.commons.lang.StringUtils;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMockSupport;
import org.easymock.IAnswer;
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
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;

/**
 * UserResourceProvider tests.
 */
public class UserResourceProviderTest extends EasyMockSupport {

  private static final long CREATE_TIME = Calendar.getInstance().getTime().getTime();

  @Before
  public void resetMocks() {
    resetAll();
  }

  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testCreateResources_Administrator() throws Exception {
    Map<String, Object> resource = new HashMap<>();
    resource.put(UserResourceProvider.USER_USERNAME_PROPERTY_ID, "User100");
    resource.put(UserResourceProvider.USER_LOCAL_USERNAME_PROPERTY_ID, "user100");
    resource.put(UserResourceProvider.USER_DISPLAY_NAME_PROPERTY_ID, "User 100");

    createResourcesTest(TestAuthenticationFactory.createAdministrator(), Collections.singleton(resource));
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResources_NonAdministrator() throws Exception {
    Map<String, Object> resource = new HashMap<>();
    resource.put(UserResourceProvider.USER_USERNAME_PROPERTY_ID, "User100");
    resource.put(UserResourceProvider.USER_LOCAL_USERNAME_PROPERTY_ID, "user100");
    resource.put(UserResourceProvider.USER_DISPLAY_NAME_PROPERTY_ID, "User 100");

    createResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), Collections.singleton(resource));
  }

  @Test
  public void testCreateResources_Multiple() throws Exception {
    Map<String, Object> resource1 = new HashMap<>();
    resource1.put(UserResourceProvider.USER_USERNAME_PROPERTY_ID, "User100");
    Map<String, Object> resource2 = new HashMap<>();
    resource2.put(UserResourceProvider.USER_USERNAME_PROPERTY_ID, "User200");

    HashSet<Map<String, Object>> resourceProperties = new HashSet<>();
    resourceProperties.add(resource1);
    resourceProperties.add(resource2);

    createResourcesTest(TestAuthenticationFactory.createAdministrator("admin"), resourceProperties);
  }

  /**
   * Test setting a user's local password when creating the account. This is for backward compatibility
   * to maintain the REST API V1 contract.
   */
  @Test
  public void testCreateResources_SetPassword() throws Exception {
    Map<String, Object> resource = new HashMap<>();
    resource.put(UserResourceProvider.USER_USERNAME_PROPERTY_ID, "User100");
    resource.put(UserResourceProvider.USER_PASSWORD_PROPERTY_ID, "password100");

    createResourcesTest(TestAuthenticationFactory.createAdministrator("admin"), Collections.singleton(resource));
  }

  /**
   * Test give a user Ambari administrative rights by assigning the user to the AMBARI.ADMINISTRATOR role
   * when creating the account. This is for backward compatibility to maintain the REST API V1 contract.
   */
  @Test
  public void testCreateResources_SetAdmin() throws Exception {
    Map<String, Object> resource = new HashMap<>();
    resource.put(UserResourceProvider.USER_USERNAME_PROPERTY_ID, "User100");
    resource.put(UserResourceProvider.USER_ADMIN_PROPERTY_ID, true);

    createResourcesTest(TestAuthenticationFactory.createAdministrator("admin"), Collections.singleton(resource));
  }

  @Test
  public void testCreateResources_SetInactive() throws Exception {
    Map<String, Object> resource = new HashMap<>();
    resource.put(UserResourceProvider.USER_USERNAME_PROPERTY_ID, "User100");
    resource.put(UserResourceProvider.USER_ACTIVE_PROPERTY_ID, false);

    createResourcesTest(TestAuthenticationFactory.createAdministrator("admin"), Collections.singleton(resource));
  }

  @Test
  public void testGetResources_Administrator() throws Exception {
    getResourcesTest(TestAuthenticationFactory.createAdministrator("admin"), null);
  }

  @Test
  public void testGetResources_NonAdministrator() throws Exception {
    getResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), null);
  }

  @Test
  public void testGetResource_Administrator_Self() throws Exception {
    getResourcesTest(TestAuthenticationFactory.createAdministrator("admin"), "admin");
  }

  @Test
  public void testGetResource_Administrator_Other() throws Exception {
    getResourcesTest(TestAuthenticationFactory.createAdministrator("admin"), "User1");
  }

  @Test
  public void testGetResource_NonAdministrator_Self() throws Exception {
    getResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User1");
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResource_NonAdministrator_Other() throws Exception {
    getResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User100");
  }

  @Test
  public void testUpdateResources_UpdateAdmin_Administrator_Self() throws Exception {
    testUpdateResources_UpdateAdmin(TestAuthenticationFactory.createAdministrator("admin"), "admin");
  }

  @Test
  public void testUpdateResources_UpdateAdmin_Administrator_Other() throws Exception {
    testUpdateResources_UpdateAdmin(TestAuthenticationFactory.createAdministrator("admin"), "User100");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_UpdateAdmin_NonAdministrator_Self() throws Exception {
    testUpdateResources_UpdateAdmin(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User1");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_UpdateAdmin_NonAdministrator_Other() throws Exception {
    testUpdateResources_UpdateAdmin(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User100");
  }

  @Test
  public void testUpdateResources_UpdateActive_Administrator_Self() throws Exception {
    testUpdateResources_UpdateActive(TestAuthenticationFactory.createAdministrator("admin"), "admin");
  }

  @Test
  public void testUpdateResources_UpdateActive_Administrator_Other() throws Exception {
    testUpdateResources_UpdateActive(TestAuthenticationFactory.createAdministrator("admin"), "User100");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_UpdateActive_NonAdministrator_Self() throws Exception {
    testUpdateResources_UpdateActive(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User1");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_UpdateActive_NonAdministrator_Other() throws Exception {
    testUpdateResources_UpdateActive(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User100");
  }

  @Test
  public void testUpdateResources_UpdateDisplayName_Administrator_Self() throws Exception {
    testUpdateResources_UpdateDisplayName(TestAuthenticationFactory.createAdministrator("admin"), "admin");
  }

  @Test
  public void testUpdateResources_UpdateDisplayName_Administrator_Other() throws Exception {
    testUpdateResources_UpdateDisplayName(TestAuthenticationFactory.createAdministrator("admin"), "User100");
  }

  @Test
  public void testUpdateResources_UpdateDisplayName_NonAdministrator_Self() throws Exception {
    testUpdateResources_UpdateDisplayName(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User1");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_UpdateDisplayName_NonAdministrator_Other() throws Exception {
    testUpdateResources_UpdateDisplayName(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User100");
  }

  @Test
  public void testUpdateResources_UpdateLocalUserName_Administrator_Self() throws Exception {
    testUpdateResources_UpdateLocalUserName(TestAuthenticationFactory.createAdministrator("admin"), "admin");
  }

  @Test
  public void testUpdateResources_UpdateLocalUserName_Administrator_Other() throws Exception {
    testUpdateResources_UpdateLocalUserName(TestAuthenticationFactory.createAdministrator("admin"), "User100");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_UpdateLocalUserName_NonAdministrator_Self() throws Exception {
    testUpdateResources_UpdateLocalUserName(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User1");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_UpdateLocalUserName_NonAdministrator_Other() throws Exception {
    testUpdateResources_UpdateLocalUserName(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User100");
  }

  @Test
  public void testUpdateResources_UpdatePassword_Administrator_Self() throws Exception {
    testUpdateResources_UpdatePassword(TestAuthenticationFactory.createAdministrator("admin"), "admin");
  }

  @Test
  public void testUpdateResources_UpdatePassword_Administrator_Other() throws Exception {
    testUpdateResources_UpdatePassword(TestAuthenticationFactory.createAdministrator("admin"), "User100");
  }

  @Test
  public void testUpdateResources_UpdatePassword_NonAdministrator_Self() throws Exception {
    testUpdateResources_UpdatePassword(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User1");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_UpdatePassword_NonAdministrator_Other() throws Exception {
    testUpdateResources_UpdatePassword(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User100");
  }

  @Test
  public void testUpdateResources_CreatePassword_Administrator_Self() throws Exception {
    testUpdateResources_CreatePassword(TestAuthenticationFactory.createAdministrator("admin"), "admin");
  }

  @Test
  public void testUpdateResources_CreatePassword_Administrator_Other() throws Exception {
    testUpdateResources_CreatePassword(TestAuthenticationFactory.createAdministrator("admin"), "User100");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_CreatePassword_NonAdministrator_Self() throws Exception {
    testUpdateResources_CreatePassword(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User1");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_CreatePassword_NonAdministrator_Other() throws Exception {
    testUpdateResources_CreatePassword(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User100");
  }

  @Test
  public void testDeleteResource_Administrator_Self() throws Exception {
    deleteResourcesTest(TestAuthenticationFactory.createAdministrator("admin"), "admin");
  }

  @Test
  public void testDeleteResource_Administrator_Other() throws Exception {
    deleteResourcesTest(TestAuthenticationFactory.createAdministrator("admin"), "User100");
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResource_NonAdministrator_Self() throws Exception {
    deleteResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User1");
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResource_NonAdministrator_Other() throws Exception {
    deleteResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User100");
  }

  private Injector createInjector() throws Exception {
    return Guice.createInjector(new AbstractModule() {
      @Override
      protected <T> Provider<T> getProvider(Class<T> type) {
        return super.getProvider(type);
      }

      @Override
      protected void configure() {
        install(new FactoryModuleBuilder().build(UpgradeContextFactory.class));
        install(new FactoryModuleBuilder().build(RoleGraphFactory.class));
        install(new FactoryModuleBuilder().build(ConfigureClusterTaskFactory.class));

        bind(EntityManager.class).toInstance(createMock(EntityManager.class));
        bind(DBAccessor.class).toInstance(createMock(DBAccessor.class));
        bind(ActionDBAccessor.class).toInstance(createMock(ActionDBAccessor.class));
        bind(ExecutionScheduler.class).toInstance(createMock(ExecutionScheduler.class));
        bind(OsFamily.class).toInstance(createMock(OsFamily.class));
        bind(AmbariMetaInfo.class).toInstance(createMock(AmbariMetaInfo.class));
        bind(ActionManager.class).toInstance(createMock(ActionManager.class));
        bind(RequestFactory.class).toInstance(createMock(RequestFactory.class));
        bind(RequestExecutionFactory.class).toInstance(createMock(RequestExecutionFactory.class));
        bind(StageFactory.class).toInstance(createMock(StageFactory.class));
        bind(Clusters.class).toInstance(createMock(Clusters.class));
        bind(AbstractRootServiceResponseFactory.class).toInstance(createMock(AbstractRootServiceResponseFactory.class));
        bind(StackManagerFactory.class).toInstance(createMock(StackManagerFactory.class));
        bind(ConfigFactory.class).toInstance(createMock(ConfigFactory.class));
        bind(ConfigGroupFactory.class).toInstance(createMock(ConfigGroupFactory.class));
        bind(ServiceFactory.class).toInstance(createMock(ServiceFactory.class));
        bind(ServiceComponentFactory.class).toInstance(createMock(ServiceComponentFactory.class));
        bind(ServiceComponentHostFactory.class).toInstance(createMock(ServiceComponentHostFactory.class));
        bind(PasswordEncoder.class).toInstance(createMock(PasswordEncoder.class));
        bind(KerberosHelper.class).toInstance(createMock(KerberosHelper.class));
        bind(AmbariManagementController.class).toInstance(createMock(AmbariManagementController.class));
        bind(RoleCommandOrderProvider.class).to(CachedRoleCommandOrderProvider.class);
        bind(CredentialStoreService.class).to(CredentialStoreServiceImpl.class);
        bind(HostRoleCommandDAO.class).toInstance(createMock(HostRoleCommandDAO.class));
        bind(HookService.class).toInstance(createMock(HookService.class));
        bind(HookContextFactory.class).toInstance(createMock(HookContextFactory.class));
        bind(HostRoleCommandFactory.class).to(HostRoleCommandFactoryImpl.class);
        bind(PersistedState.class).to(PersistedStateImpl.class);
        bind(UserDAO.class).toInstance(createMock(UserDAO.class));

        bind(UserAuthenticationDAO.class).toInstance(createMock(UserAuthenticationDAO.class));
        bind(GroupDAO.class).toInstance(createMock(GroupDAO.class));
        bind(MemberDAO.class).toInstance(createMock(MemberDAO.class));
        bind(PrincipalDAO.class).toInstance(createMock(PrincipalDAO.class));
        bind(PermissionDAO.class).toInstance(createMock(PermissionDAO.class));
        bind(PrivilegeDAO.class).toInstance(createMock(PrivilegeDAO.class));
        bind(ResourceDAO.class).toInstance(createMock(ResourceDAO.class));
        bind(PrincipalTypeDAO.class).toInstance(createMock(PrincipalTypeDAO.class));
        bind(MpackManagerFactory.class).toInstance(createNiceMock(MpackManagerFactory.class));
        bind(AmbariLdapConfigurationProvider.class).toInstance(createMock(AmbariLdapConfigurationProvider.class));

        bind(new TypeLiteral<Encryptor<AgentConfigsUpdateEvent>>() {}).annotatedWith(Names.named("AgentConfigEncryptor")).toInstance(Encryptor.NONE);
        bind(new TypeLiteral<Encryptor<AmbariServerConfiguration>>() {}).annotatedWith(Names.named("AmbariServerConfigurationEncryptor")).toInstance(Encryptor.NONE);
      }
    });
  }


  private void createResourcesTest(Authentication authentication, Set<Map<String, Object>> resourceProperties) throws Exception {
    Injector injector = createInjector();
    UserDAO userDAO = injector.getInstance(UserDAO.class);
    Capture<? extends UserEntity> userEntityCapture = newCapture(CaptureType.ALL);

    Map<String, Map<String, Object>> expectedUsers = new HashMap<>();

    for (Map<String, Object> properties : resourceProperties) {
      String username = (String) properties.get(UserResourceProvider.USER_USERNAME_PROPERTY_ID);

      if (!StringUtils.isEmpty(username)) {
        Assert.assertFalse("User names must be unique for this test case", expectedUsers.containsKey(username.toLowerCase()));

        expect(userDAO.findUserByName(username)).andReturn(null).times(2);
        userDAO.create(capture(userEntityCapture));
        expectLastCall().once();

        PrincipalTypeEntity principalTypeEntity = createMock(PrincipalTypeEntity.class);

        PrincipalTypeDAO principalTypeDAO = injector.getInstance(PrincipalTypeDAO.class);
        expect(principalTypeDAO.findById(PrincipalTypeEntity.USER_PRINCIPAL_TYPE)).andReturn(principalTypeEntity).once();

        PrincipalDAO principalDAO = injector.getInstance(PrincipalDAO.class);
        principalDAO.create(anyObject(PrincipalEntity.class));
        expectLastCall().andAnswer(new IAnswer<Object>() {
          @Override
          public Object answer() throws Throwable {
            Object[] args = getCurrentArguments();

            ((PrincipalEntity) args[0]).setId(1L);
            return null;
          }
        }).once();


        HookContextFactory hookContextFactory = injector.getInstance(HookContextFactory.class);
        expect(hookContextFactory.createUserHookContext(username)).andReturn(null).once();

        HookService hookService = injector.getInstance(HookService.class);
        expect(hookService.execute(anyObject(HookContext.class))).andReturn(true).once();


        if (properties.get(UserResourceProvider.USER_PASSWORD_PROPERTY_ID) != null) {
          ResourceProviderFactory factory = createMock(ResourceProviderFactory.class);
          ResourceProvider resourceProvider = createMock(ResourceProvider.class);
          RequestStatus status = createMock(RequestStatus.class);
          expect(resourceProvider.createResources(anyObject(Request.class))).andReturn(status).once();
          expect(factory.getUserAuthenticationSourceResourceProvider()).andReturn(resourceProvider).once();

          AbstractControllerResourceProvider.init(factory);
        }

        if (properties.get(UserResourceProvider.USER_ADMIN_PROPERTY_ID) != null) {
          Boolean isAdmin = Boolean.TRUE.equals(properties.get(UserResourceProvider.USER_ADMIN_PROPERTY_ID));

          if (isAdmin) {
            PermissionEntity permissionEntity = createMock(PermissionEntity.class);
            PermissionDAO permissionDAO = injector.getInstance(PermissionDAO.class);
            expect(permissionDAO.findAmbariAdminPermission()).andReturn(permissionEntity).once();

            ResourceEntity resourceEntity = createMock(ResourceEntity.class);
            ResourceDAO resourceDAO = injector.getInstance(ResourceDAO.class);
            expect(resourceDAO.findAmbariResource()).andReturn(resourceEntity).once();

            PrivilegeDAO privilegeDAO = injector.getInstance(PrivilegeDAO.class);
            privilegeDAO.create(anyObject(PrivilegeEntity.class));
            expectLastCall().andAnswer(new IAnswer<Object>() {
              @Override
              public Object answer() throws Throwable {
                Object[] args = getCurrentArguments();

                ((PrivilegeEntity) args[0]).setId(1);
                return null;
              }
            }).once();

            expect(principalDAO.merge(anyObject(PrincipalEntity.class))).andReturn(null).once();

            expect(userDAO.merge(anyObject(UserEntity.class))).andReturn(null).once();
          }
        }

        expectedUsers.put(username.toLowerCase(), properties);
      }
    }

    // replay
    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    ResourceProvider provider = getResourceProvider(injector);

    // create the request
    Request request = PropertyHelper.getCreateRequest(resourceProperties, null);

    provider.createResources(request);

    // verify
    verifyAll();

    List<? extends UserEntity> capturedUserEntities = userEntityCapture.getValues();
    Assert.assertEquals(expectedUsers.size(), capturedUserEntities.size());

    for (UserEntity userEntity : capturedUserEntities) {
      String userName = userEntity.getUserName();
      Map<String, Object> userProperties = expectedUsers.get(userName);

      Assert.assertNotNull(userProperties);

      String username = (String) userProperties.get(UserResourceProvider.USER_USERNAME_PROPERTY_ID);
      String displayName = (String) userProperties.get(UserResourceProvider.USER_DISPLAY_NAME_PROPERTY_ID);
      String localUsername = (String) userProperties.get(UserResourceProvider.USER_LOCAL_USERNAME_PROPERTY_ID);
      Boolean isActive = (userProperties.containsKey(UserResourceProvider.USER_ACTIVE_PROPERTY_ID))
          ? !Boolean.FALSE.equals(userProperties.get(UserResourceProvider.USER_ACTIVE_PROPERTY_ID))
          : Boolean.TRUE;

      Assert.assertEquals(username.toLowerCase(), userEntity.getUserName());
      Assert.assertEquals(StringUtils.defaultIfEmpty(localUsername, username), userEntity.getLocalUsername());
      Assert.assertEquals(StringUtils.defaultIfEmpty(displayName, username), userEntity.getDisplayName());
      Assert.assertEquals(isActive, userEntity.getActive());
    }
  }

  private void getResourcesTest(Authentication authentication, String requestedUsername) throws Exception {
    Injector injector = createInjector();

    String username = requestedUsername;
    if (username == null) {
      if (!"admin".equals(authentication.getName())) {
        username = authentication.getName();
      }
    }

    UserDAO userDAO = injector.getInstance(UserDAO.class);

    PrincipalEntity userPrincipalEntity = createMock(PrincipalEntity.class);
    expect(userPrincipalEntity.getPrivileges()).andReturn(null).anyTimes();

    if (username == null) {
      UserEntity userEntity1 = createMockUserEntity("User1");
      expect(userEntity1.getPrincipal()).andReturn(userPrincipalEntity).once();

      UserEntity userEntity10 = createMockUserEntity("User10");
      expect(userEntity10.getPrincipal()).andReturn(userPrincipalEntity).once();

      UserEntity userEntity100 = createMockUserEntity("User100");
      expect(userEntity100.getPrincipal()).andReturn(userPrincipalEntity).once();

      UserEntity userEntityAdmin = createMockUserEntity("admin");
      expect(userEntityAdmin.getPrincipal()).andReturn(userPrincipalEntity).once();

      List<UserEntity> allUsers = Arrays.asList(userEntity1, userEntity10, userEntity100, userEntityAdmin);

      expect(userDAO.findAll()).andReturn(allUsers).once();
    } else {
      UserEntity userEntity = createMockUserEntity(username);
      expect(userEntity.getPrincipal()).andReturn(userPrincipalEntity).once();

      expect(userDAO.findUserByName(username)).andReturn(userEntity).once();
    }

    replayAll();

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = getResourceProvider(injector);

    Set<String> propertyIds = new HashSet<>();
    propertyIds.add(UserResourceProvider.USER_USERNAME_PROPERTY_ID);
    propertyIds.add(UserResourceProvider.USER_PASSWORD_PROPERTY_ID);

    Request request = PropertyHelper.getReadRequest(propertyIds);

    Set<Resource> resources = provider.getResources(request, (requestedUsername == null) ? null : createPredicate(requestedUsername));

    if (username == null) {
      List<String> expectedList = Arrays.asList("User1", "User10", "User100", "admin");
      Assert.assertEquals(4, resources.size());
      for (Resource resource : resources) {
        String userName = (String) resource.getPropertyValue(UserResourceProvider.USER_USERNAME_PROPERTY_ID);
        Assert.assertTrue(expectedList.contains(userName));
      }
    } else {
      Assert.assertEquals(1, resources.size());
      for (Resource resource : resources) {
        Assert.assertEquals(username, resource.getPropertyValue(UserResourceProvider.USER_USERNAME_PROPERTY_ID));
      }
    }

    verifyAll();
  }

  private void testUpdateResources_UpdateAdmin(Authentication authentication, String requestedUsername) throws Exception {
    updateResourcesTest(authentication, requestedUsername, Collections.<String, Object>singletonMap(UserResourceProvider.USER_ADMIN_PROPERTY_ID, true), false);
  }

  private void testUpdateResources_UpdateActive(Authentication authentication, String requestedUsername) throws Exception {
    updateResourcesTest(authentication, requestedUsername, Collections.<String, Object>singletonMap(UserResourceProvider.USER_ACTIVE_PROPERTY_ID, false), false);
  }

  private void testUpdateResources_UpdateDisplayName(Authentication authentication, String requestedUsername) throws Exception {
    updateResourcesTest(authentication, requestedUsername, Collections.<String, Object>singletonMap(UserResourceProvider.USER_DISPLAY_NAME_PROPERTY_ID, "Updated Display Name"), false);
  }

  private void testUpdateResources_UpdateLocalUserName(Authentication authentication, String requestedUsername) throws Exception {
    updateResourcesTest(authentication, requestedUsername, Collections.<String, Object>singletonMap(UserResourceProvider.USER_LOCAL_USERNAME_PROPERTY_ID, "updated_username"), false);
  }

  private void testUpdateResources_UpdatePassword(Authentication authentication, String requestedUsername) throws Exception {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(UserResourceProvider.USER_OLD_PASSWORD_PROPERTY_ID, "old_password");
    properties.put(UserResourceProvider.USER_PASSWORD_PROPERTY_ID, "new_password");

    updateResourcesTest(authentication, requestedUsername, properties, true);
  }

  private void testUpdateResources_CreatePassword(Authentication authentication, String requestedUsername) throws Exception {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(UserResourceProvider.USER_OLD_PASSWORD_PROPERTY_ID, "old_password");
    properties.put(UserResourceProvider.USER_PASSWORD_PROPERTY_ID, "new_password");

    updateResourcesTest(authentication, requestedUsername, properties, false);
  }

  private void updateResourcesTest(Authentication authentication, String requestedUsername, Map<String, Object> updates, boolean passwordAlreadyExists) throws Exception {
    Injector injector = createInjector();

    Capture<Request> requestCapture = newCapture(CaptureType.FIRST);
    Capture<Predicate> predicateCapture = newCapture(CaptureType.FIRST);
    boolean hasUpdates = false;

    ResourceProviderFactory factory = createMock(ResourceProviderFactory.class);

    UserEntity userEntity = createMock(UserEntity.class);
    expect(userEntity.getUserName()).andReturn(requestedUsername).anyTimes();
    expect(userEntity.getActive()).andReturn(true).anyTimes();
    expect(userEntity.getDisplayName()).andReturn(requestedUsername).anyTimes();
    expect(userEntity.getLocalUsername()).andReturn(requestedUsername).anyTimes();

    if (updates.containsKey(UserResourceProvider.USER_DISPLAY_NAME_PROPERTY_ID)) {
      userEntity.setDisplayName((String) updates.get(UserResourceProvider.USER_DISPLAY_NAME_PROPERTY_ID));
      expectLastCall().once();
      hasUpdates = true;
    }

    if (updates.containsKey(UserResourceProvider.USER_LOCAL_USERNAME_PROPERTY_ID)) {
      userEntity.setLocalUsername((String) updates.get(UserResourceProvider.USER_LOCAL_USERNAME_PROPERTY_ID));
      expectLastCall().once();
      hasUpdates = true;
    }

    if (updates.containsKey(UserResourceProvider.USER_ACTIVE_PROPERTY_ID)) {
      userEntity.setActive((Boolean) updates.get(UserResourceProvider.USER_ACTIVE_PROPERTY_ID));
      expectLastCall().once();
      hasUpdates = true;
    }

    UserDAO userDAO = injector.getInstance(UserDAO.class);
    expect(userDAO.findUserByName(requestedUsername)).andReturn(userEntity).once();

    if (hasUpdates) {
      expect(userDAO.merge(userEntity)).andReturn(userEntity).once();
    }

    if (updates.get(UserResourceProvider.USER_ADMIN_PROPERTY_ID) != null) {
      Boolean isAdmin = Boolean.TRUE.equals(updates.get(UserResourceProvider.USER_ADMIN_PROPERTY_ID));

      if (isAdmin) {
        PermissionEntity permissionEntity = createMock(PermissionEntity.class);
        PermissionDAO permissionDAO = injector.getInstance(PermissionDAO.class);
        expect(permissionDAO.findAmbariAdminPermission()).andReturn(permissionEntity).once();

        ResourceEntity resourceEntity = createMock(ResourceEntity.class);
        ResourceDAO resourceDAO = injector.getInstance(ResourceDAO.class);
        expect(resourceDAO.findAmbariResource()).andReturn(resourceEntity).once();

        PrivilegeDAO privilegeDAO = injector.getInstance(PrivilegeDAO.class);
        privilegeDAO.create(anyObject(PrivilegeEntity.class));
        expectLastCall().andAnswer(new IAnswer<Object>() {
          @Override
          public Object answer() throws Throwable {
            Object[] args = getCurrentArguments();

            ((PrivilegeEntity) args[0]).setId(1);
            return null;
          }
        }).once();

        PrincipalDAO principalDAO = injector.getInstance(PrincipalDAO.class);
        expect(principalDAO.merge(anyObject(PrincipalEntity.class))).andReturn(null).once();

        PrincipalEntity principalEntity = createMock(PrincipalEntity.class);
        expect(principalEntity.getPrivileges()).andReturn(new HashSet<PrivilegeEntity>()).anyTimes();

        expect(userEntity.getPrincipal()).andReturn(principalEntity).anyTimes();

        expect(userDAO.merge(anyObject(UserEntity.class))).andReturn(null).once();
      }
    }

    if (updates.containsKey(UserResourceProvider.USER_PASSWORD_PROPERTY_ID)) {
      if(passwordAlreadyExists) {
        UserAuthenticationEntity authenticationEntity = createMock(UserAuthenticationEntity.class);
        expect(authenticationEntity.getUserAuthenticationId()).andReturn(100L).anyTimes();
        expect(authenticationEntity.getAuthenticationType()).andReturn(UserAuthenticationType.LOCAL).anyTimes();

        expect(userEntity.getAuthenticationEntities()).andReturn(Collections.singletonList(authenticationEntity)).once();
      }
      else {
        expect(userEntity.getAuthenticationEntities()).andReturn(Collections.<UserAuthenticationEntity>emptyList()).once();
      }

      RequestStatus status = createMock(RequestStatus.class);

      ResourceProvider resourceProvider = createMock(ResourceProvider.class);

      if(passwordAlreadyExists) {
        expect(resourceProvider.updateResources(capture(requestCapture), capture(predicateCapture))).andReturn(status).once();
      }
      else {
        expect(resourceProvider.createResources(capture(requestCapture))).andReturn(status).once();
      }

      expect(factory.getUserAuthenticationSourceResourceProvider()).andReturn(resourceProvider).once();
    }

    AmbariEventPublisher publisher = createNiceMock(AmbariEventPublisher.class);

    replayAll();

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    ViewRegistry.initInstance(new ViewRegistry(publisher));
    AbstractControllerResourceProvider.init(factory);
    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = getResourceProvider(injector);

    // create the request
    Request request = PropertyHelper.getUpdateRequest(updates, null);

    provider.updateResources(request, createPredicate(requestedUsername));

    verifyAll();

    if (updates.containsKey(UserResourceProvider.USER_PASSWORD_PROPERTY_ID)) {
      // Verify that the correct request was issued to update update the user's password...
      Request capturedRequest = requestCapture.getValue();
      Set<Map<String, Object>> capturedProperties = capturedRequest.getProperties();
      Map<String, Object> properties = capturedProperties.iterator().next();
      Assert.assertNotNull(capturedProperties);
      if(passwordAlreadyExists) {
        Assert.assertEquals(updates.get(UserResourceProvider.USER_PASSWORD_PROPERTY_ID), properties.get(UserAuthenticationSourceResourceProvider.AUTHENTICATION_KEY_PROPERTY_ID));
        Assert.assertEquals(updates.get(UserResourceProvider.USER_OLD_PASSWORD_PROPERTY_ID), properties.get(UserAuthenticationSourceResourceProvider.AUTHENTICATION_OLD_KEY_PROPERTY_ID));
      }
      else {
        Assert.assertEquals(updates.get(UserResourceProvider.USER_PASSWORD_PROPERTY_ID), properties.get(UserAuthenticationSourceResourceProvider.AUTHENTICATION_KEY_PROPERTY_ID));
        Assert.assertEquals(UserAuthenticationType.LOCAL.name(), properties.get(UserAuthenticationSourceResourceProvider.AUTHENTICATION_AUTHENTICATION_TYPE_PROPERTY_ID));
        Assert.assertEquals(requestedUsername, properties.get(UserAuthenticationSourceResourceProvider.AUTHENTICATION_USER_NAME_PROPERTY_ID));
      }

      if(passwordAlreadyExists) {
        Predicate capturedPredicate = predicateCapture.getValue();
        Assert.assertEquals(AndPredicate.class, capturedPredicate.getClass());
        AndPredicate andPredicate = (AndPredicate) capturedPredicate;
        Predicate[] predicates = andPredicate.getPredicates();
        Assert.assertEquals(2, predicates.length);
        for (Predicate p : predicates) {
          Assert.assertEquals(EqualsPredicate.class, p.getClass());
          EqualsPredicate equalsPredicate = (EqualsPredicate) p;

          if (UserAuthenticationSourceResourceProvider.AUTHENTICATION_USER_NAME_PROPERTY_ID.equals(equalsPredicate.getPropertyId())) {
            Assert.assertEquals(requestedUsername, equalsPredicate.getValue());
          } else if (UserAuthenticationSourceResourceProvider.AUTHENTICATION_AUTHENTICATION_SOURCE_ID_PROPERTY_ID.equals(equalsPredicate.getPropertyId())) {
            Assert.assertEquals("100", equalsPredicate.getValue());
          }
        }
      }
      else {
        Assert.assertFalse(predicateCapture.hasCaptured());
      }
    }
  }

  private void deleteResourcesTest(Authentication authentication, String requestedUsername) throws Exception {
    Injector injector = createInjector();

    UserEntity userEntity = createMockUserEntity(requestedUsername);

    List<PrincipalEntity> adminPrincipals = Collections.singletonList(createMock(PrincipalEntity.class));

    List<UserEntity> adminUserEntities = new ArrayList<>();
    adminUserEntities.add(createMockUserEntity("some admin"));
    if ("admin".equals(requestedUsername)) {
      adminUserEntities.add(userEntity);
    }

    UserDAO userDAO = injector.getInstance(UserDAO.class);
    expect(userDAO.findUserByName(requestedUsername)).andReturn(userEntity).once();
    (expect(userDAO.findUsersByPrincipal(adminPrincipals))).andReturn(adminUserEntities).once();
    userDAO.remove(userEntity);
    expectLastCall().atLeastOnce();

    PrincipalDAO principalDAO = injector.getInstance(PrincipalDAO.class);
    expect(principalDAO.findByPermissionId(PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION)).andReturn(adminPrincipals).once();

    // replay
    replayAll();

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = getResourceProvider(injector);

    provider.deleteResources(new RequestImpl(null, null, null, null), createPredicate(requestedUsername));

    // verify
    verifyAll();
  }


  private Predicate createPredicate(String requestedUsername) {
    return new PredicateBuilder()
        .property(UserResourceProvider.USER_USERNAME_PROPERTY_ID)
        .equals(requestedUsername)
        .toPredicate();
  }

  private UserEntity createMockUserEntity(String username) {
    UserEntity userEntity = createMock(UserEntity.class);
    expect(userEntity.getUserId()).andReturn(username.hashCode()).anyTimes();
    expect(userEntity.getUserName()).andReturn(username).anyTimes();
    expect(userEntity.getLocalUsername()).andReturn(username).anyTimes();
    expect(userEntity.getDisplayName()).andReturn(username).anyTimes();
    expect(userEntity.getActive()).andReturn(true).anyTimes();
    expect(userEntity.getCreateTime()).andReturn(CREATE_TIME).anyTimes();
    expect(userEntity.getConsecutiveFailures()).andReturn(0).anyTimes();
    expect(userEntity.getAuthenticationEntities()).andReturn(Collections.<UserAuthenticationEntity>emptyList()).anyTimes();
    expect(userEntity.getMemberEntities()).andReturn(Collections.<MemberEntity>emptySet()).anyTimes();
    return userEntity;
  }

  private ResourceProvider getResourceProvider(Injector injector) {
    UserResourceProvider resourceProvider = new UserResourceProvider(injector.getInstance(AmbariManagementController.class));

    injector.injectMembers(resourceProvider);
    return resourceProvider;
  }
}