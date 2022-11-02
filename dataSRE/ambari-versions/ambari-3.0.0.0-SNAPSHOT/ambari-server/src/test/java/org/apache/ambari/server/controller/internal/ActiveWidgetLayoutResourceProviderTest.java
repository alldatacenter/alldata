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
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactoryImpl;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AbstractRootServiceResponseFactory;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.hooks.HookContextFactory;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.metadata.CachedRoleCommandOrderProvider;
import org.apache.ambari.server.metadata.RoleCommandOrderProvider;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.dao.WidgetDAO;
import org.apache.ambari.server.orm.dao.WidgetLayoutDAO;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.orm.entities.WidgetLayoutEntity;
import org.apache.ambari.server.scheduler.ExecutionScheduler;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.security.encryption.CredentialStoreServiceImpl;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContextFactory;
import org.apache.ambari.server.stageplanner.RoleGraphFactory;
import org.apache.ambari.server.state.Cluster;
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
import org.easymock.Capture;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;

/**
 * ActiveWidgetLayout tests
 */
@Ignore // need to fix StackOverflowError
public class ActiveWidgetLayoutResourceProviderTest extends EasyMockSupport {

  @Before
  public void before() {
    resetAll();
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
  public void testCreateResources_Administrator() throws Exception {
    createResourcesTest(TestAuthenticationFactory.createAdministrator("admin"), "User1");
  }

  @Test(expected = SystemException.class)
  public void testCreateResources_NonAdministrator_Self() throws Exception {
    createResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User1");
  }

  @Test(expected = SystemException.class)
  public void testCreateResources_NonAdministrator_Other() throws Exception {
    createResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User10");
  }

  @Test
  public void testUpdateResources_Administrator() throws Exception {
    updateResourcesTest(TestAuthenticationFactory.createAdministrator("admin"), "User1");
  }

  @Test
  public void testUpdateResources_NonAdministrator_Self() throws Exception {
    updateResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User1");
  }

  @Test
  public void testUpdateResources_NoUserName_Self() throws Exception {
    updateResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User1", false);
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_NonAdministrator_Other() throws Exception {
    updateResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User10");
  }

  @Test(expected = SystemException.class)
  public void testDeleteResources_Administrator() throws Exception {
    deleteResourcesTest(TestAuthenticationFactory.createAdministrator("admin"), "User1");
  }

  @Test(expected = SystemException.class)
  public void testDeleteResources_NonAdministrator_Self() throws Exception {
    deleteResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User1");
  }

  @Test(expected = SystemException.class)
  public void testDeleteResources_NonAdministrator_Other() throws Exception {
    deleteResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User10");
  }

  private void getResourcesTest(Authentication authentication, String requestedUsername) throws Exception {
    Injector injector = createInjector();

    UserEntity userEntity = createMockUserEntity(requestedUsername);

    UserDAO userDAO = injector.getInstance(UserDAO.class);
    expect(userDAO.findUserByName(requestedUsername)).andReturn(userEntity).atLeastOnce();

    WidgetLayoutDAO widgetLayoutDAO = injector.getInstance(WidgetLayoutDAO.class);
    expect(widgetLayoutDAO.findById(1L)).andReturn(createMockWidgetLayout(1L, requestedUsername)).atLeastOnce();
    expect(widgetLayoutDAO.findById(2L)).andReturn(createMockWidgetLayout(2L, requestedUsername)).atLeastOnce();

    Cluster cluster = createNiceMock(Cluster.class);
    expect(cluster.getClusterName()).andReturn("c1").atLeastOnce();
    expect(cluster.getResourceId()).andReturn(4L).anyTimes();

    Clusters clusters = injector.getInstance(Clusters.class);
    expect(clusters.getClusterById(2L)).andReturn(cluster).atLeastOnce();

    replayAll();

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);

    ResourceProvider provider = getResourceProvider(injector, managementController);

    Set<String> propertyIds = PropertyHelper.getPropertyIds(Resource.Type.ActiveWidgetLayout);

    Request request = PropertyHelper.getReadRequest(propertyIds);

    Set<Resource> resources = provider.getResources(request, createPredicate(requestedUsername));

    Assert.assertEquals(2, resources.size());
    for (Resource resource : resources) {

      Long id = (Long) resource.getPropertyValue(ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_ID_PROPERTY_ID);

      Assert.assertEquals("section" + id, resource.getPropertyValue(ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_SECTION_NAME_PROPERTY_ID));
      Assert.assertEquals("CLUSTER", resource.getPropertyValue(ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_SCOPE_PROPERTY_ID));
      Assert.assertEquals(requestedUsername, resource.getPropertyValue(ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_USERNAME_PROPERTY_ID));
      Assert.assertEquals("display name" + id, resource.getPropertyValue(ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_DISPLAY_NAME_PROPERTY_ID));
      Assert.assertEquals("layout name" + id, resource.getPropertyValue(ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_LAYOUT_NAME_PROPERTY_ID));

      Assert.assertEquals("[]", resource.getPropertyValue(ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_WIDGETS_PROPERTY_ID).toString());
    }

    verifyAll();
  }

  private void createResourcesTest(Authentication authentication, String requestedUsername) throws Exception {
    Injector injector = createInjector();

    replayAll();

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);

    ResourceProvider provider = getResourceProvider(injector, managementController);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    Map<String, Object> properties = new LinkedHashMap<>();

    // add properties to the request map
    properties.put(ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_USERNAME_PROPERTY_ID, requestedUsername);

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    verifyAll();
  }

  private void updateResourcesTest(Authentication authentication, String requestedUsername) throws Exception {
    updateResourcesTest(authentication, requestedUsername, true);
  }

  private void updateResourcesTest(Authentication authentication, String requestedUsername, boolean setUserName) throws Exception {
    Injector injector = createInjector();

    Capture<? extends String> widgetLayoutJsonCapture = newCapture();

    UserEntity userEntity = createMockUserEntity(requestedUsername);
    userEntity.setActiveWidgetLayouts(capture(widgetLayoutJsonCapture));
    expectLastCall().once();

    UserDAO userDAO = injector.getInstance(UserDAO.class);
    expect(userDAO.findUserByName(requestedUsername)).andReturn(userEntity).atLeastOnce();
    expect(userDAO.merge(userEntity)).andReturn(userEntity).atLeastOnce();

    WidgetLayoutDAO widgetLayoutDAO = injector.getInstance(WidgetLayoutDAO.class);
    expect(widgetLayoutDAO.findById(1L)).andReturn(createMockWidgetLayout(1L, requestedUsername)).atLeastOnce();
    expect(widgetLayoutDAO.findById(2L)).andReturn(createMockWidgetLayout(2L, requestedUsername)).atLeastOnce();

    replayAll();

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);

    Set<Map<String, String>> widgetLayouts = new HashSet<>();
    HashMap<String, String> layout;

    layout = new HashMap<>();
    layout.put("id", "1");
    widgetLayouts.add(layout);

    layout = new HashMap<>();
    layout.put("id", "2");
    widgetLayouts.add(layout);

    HashMap<String, Object> requestProps = new HashMap<>();
    requestProps.put(ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT, widgetLayouts);
    if (setUserName) {
      requestProps.put(ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_USERNAME_PROPERTY_ID, requestedUsername);
    }

    Request request = PropertyHelper.getUpdateRequest(requestProps, null);

    ResourceProvider provider = getResourceProvider(injector, managementController);

    provider.updateResources(request, createPredicate(requestedUsername));

    verifyAll();

    String json = widgetLayoutJsonCapture.getValue();
    Assert.assertNotNull(json);

    Set capturedWidgetLayouts = new Gson().fromJson(json, widgetLayouts.getClass());
    Assert.assertEquals(widgetLayouts, capturedWidgetLayouts);
  }

  private void deleteResourcesTest(Authentication authentication, String requestedUsername) throws Exception {
    Injector injector = createInjector();

    UserEntity userEntity = createMockUserEntity(requestedUsername);

    UserDAO userDAO = injector.getInstance(UserDAO.class);
    expect(userDAO.findUserByName(requestedUsername)).andReturn(userEntity).atLeastOnce();

    replayAll();

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);

    ResourceProvider provider = getResourceProvider(injector, managementController);

    provider.deleteResources(new RequestImpl(null, null, null, null), createPredicate(requestedUsername));

    verifyAll();
  }

  private ResourceProvider getResourceProvider(Injector injector, AmbariManagementController managementController) throws Exception {
    ActiveWidgetLayoutResourceProvider.init(injector.getInstance(UserDAO.class),
        injector.getInstance(WidgetDAO.class),
        injector.getInstance(WidgetLayoutDAO.class),
        new Gson());

    return AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.ActiveWidgetLayout,
        managementController);
  }

  private Predicate createPredicate(String requestedUsername) {
    return new PredicateBuilder()
        .property(ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_USERNAME_PROPERTY_ID)
        .equals(requestedUsername)
        .toPredicate();
  }

  private WidgetLayoutEntity createMockWidgetLayout(Long id, String username) {
    WidgetLayoutEntity widgetLayoutEntity = createMock(WidgetLayoutEntity.class);
    expect(widgetLayoutEntity.getId()).andReturn(id).anyTimes();
    expect(widgetLayoutEntity.getUserName()).andReturn(username).anyTimes();
    expect(widgetLayoutEntity.getLayoutName()).andReturn("layout name" + id).anyTimes();
    expect(widgetLayoutEntity.getSectionName()).andReturn("section" + id).anyTimes();
    expect(widgetLayoutEntity.getScope()).andReturn("CLUSTER").anyTimes();
    expect(widgetLayoutEntity.getDisplayName()).andReturn("display name" + id).anyTimes();
    expect(widgetLayoutEntity.getClusterId()).andReturn(2L).anyTimes();
    expect(widgetLayoutEntity.getListWidgetLayoutUserWidgetEntity()).andReturn(Collections.emptyList()).anyTimes();
    return widgetLayoutEntity;
  }

  private UserEntity createMockUserEntity(String username) {
    UserEntity userEntity = createMock(UserEntity.class);
    expect(userEntity.getUserId()).andReturn(username.hashCode()).anyTimes();
    expect(userEntity.getUserName()).andReturn(username).anyTimes();
    expect(userEntity.getActiveWidgetLayouts()).andReturn("[{\"id\":\"1\"},{\"id\":\"2\"}]").anyTimes();

    return userEntity;
  }

  private Injector createInjector() throws Exception {
    return Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        install(new FactoryModuleBuilder().build(UpgradeContextFactory.class));
        install(new FactoryModuleBuilder().build(RoleGraphFactory.class));
        install(new FactoryModuleBuilder().build(ConfigureClusterTaskFactory.class));

        bind(PersistedState.class).to(PersistedStateImpl.class);
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
        bind(WidgetLayoutDAO.class).toInstance(createMock(WidgetLayoutDAO.class));
        bind(HostRoleCommandDAO.class).toInstance(createMock(HostRoleCommandDAO.class));
        bind(HookContextFactory.class).toInstance(createMock(HookContextFactory.class));
        bind(HookService.class).toInstance(createMock(HookService.class));
        bind(HostRoleCommandFactory.class).to(HostRoleCommandFactoryImpl.class);
      }
    });
  }
}
