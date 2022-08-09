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
import static org.easymock.EasyMock.expectLastCall;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.mpack.MpackManagerFactory;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.entities.UserAuthenticationEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authentication.AmbariUserDetails;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.UserAuthenticationType;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.testutils.PartialNiceMockBinder;
import org.apache.velocity.exception.ResourceNotFoundException;
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
 * UserAuthenticationSourceResourceProviderTest tests.
 */
public class UserAuthenticationSourceResourceProviderTest extends EasyMockSupport {

  private static final long CREATE_TIME = Calendar.getInstance().getTime().getTime();
  private static final long UPDATE_TIME = Calendar.getInstance().getTime().getTime();

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

  @Test
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

  @Test
  public void testGetResource_NonAdministrator_Self() throws Exception {
    getResourceTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User1");
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResource_NonAdministrator_Other() throws Exception {
    getResourceTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User100");
  }

  @Test
  public void testUpdateResources_SetPassword_Administrator_Self() throws Exception {
    updateResources_SetAuthenticationKey(TestAuthenticationFactory.createAdministrator("admin"), "User100", null);
  }

  @Test
  public void testUpdateResources_SetPassword_Administrator_Other() throws Exception {
    updateResources_SetAuthenticationKey(TestAuthenticationFactory.createAdministrator("admin"), "User100", null);
  }

  @Test
  public void testUpdateResources_SetPassword_NonAdministrator_Self() throws Exception {
    updateResources_SetAuthenticationKey(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User1", null);
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_SetPassword_NonAdministrator_Other() throws Exception {
    updateResources_SetAuthenticationKey(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User100", null);
  }

  @Test
  public void testUpdateResources_SetPassword_VerifyLocal_Success() throws Exception {
    updateResources_SetAuthenticationKey(TestAuthenticationFactory.createAdministrator(), "User100", "local");
  }

  @Test(expected = ResourceNotFoundException.class)
  public void testUpdateResources_SetPassword_VerifyLocal_Fail() throws Exception {
    updateResources_SetAuthenticationKey(TestAuthenticationFactory.createAdministrator(), "User100", "KERBEROS");
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
      protected void configure() {
        PartialNiceMockBinder.newBuilder(UserAuthenticationSourceResourceProviderTest.this)
            .addAmbariMetaInfoBinding().addLdapBindings().build().configure(binder());

        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(AmbariMetaInfo.class).toInstance(createMock(AmbariMetaInfo.class));
        bind(Clusters.class).toInstance(createNiceMock(Clusters.class));
        bind(StackManagerFactory.class).toInstance(createNiceMock(StackManagerFactory.class));
        bind(Users.class).toInstance(createMock(Users.class));
        bind(MpackManagerFactory.class).toInstance(createNiceMock(MpackManagerFactory.class));
      }
    });
  }


  private void createResourcesTest(Authentication authentication) throws Exception {
    Injector injector = createInjector();

    UserEntity userEntity100 = createNiceMock(UserEntity.class);
    UserEntity userEntity200 = createNiceMock(UserEntity.class);

    Users users = injector.getInstance(Users.class);
    expect(users.getUserEntity("User100")).andReturn(userEntity100).once();
    expect(users.getUserEntity("User200")).andReturn(userEntity200).once();
    users.addAuthentication(userEntity100, UserAuthenticationType.LOCAL, "my_password_100_1234");
    expectLastCall().once();
    users.addAuthentication(userEntity200, UserAuthenticationType.LOCAL, "my_password_200_1234");
    expectLastCall().once();

    // replay
    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    ResourceProvider provider = getResourceProvider(injector);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    Map<String, Object> properties;

    properties = new LinkedHashMap<>();
    properties.put(UserAuthenticationSourceResourceProvider.AUTHENTICATION_USER_NAME_PROPERTY_ID, "User100");
    properties.put(UserAuthenticationSourceResourceProvider.AUTHENTICATION_AUTHENTICATION_TYPE_PROPERTY_ID, "local");
    properties.put(UserAuthenticationSourceResourceProvider.AUTHENTICATION_KEY_PROPERTY_ID, "my_password_100_1234");
    propertySet.add(properties);

    properties = new LinkedHashMap<>();
    properties.put(UserAuthenticationSourceResourceProvider.AUTHENTICATION_USER_NAME_PROPERTY_ID, "User200");
    properties.put(UserAuthenticationSourceResourceProvider.AUTHENTICATION_AUTHENTICATION_TYPE_PROPERTY_ID, "local");
    properties.put(UserAuthenticationSourceResourceProvider.AUTHENTICATION_KEY_PROPERTY_ID, "my_password_200_1234");
    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    // verify
    verifyAll();
  }

  private void getResourcesTest(Authentication authentication) throws Exception {
    Injector injector = createInjector();

    Users users = injector.getInstance(Users.class);
    Map<String, UserAuthenticationEntity> entities = new HashMap<>();

    entities.put("User1", createMockUserAuthenticationEntity("User1"));

    if ("admin".equals(authentication.getName())) {
      entities.put("User10", createMockUserAuthenticationEntity("User10"));
      entities.put("User100", createMockUserAuthenticationEntity("User100"));
      entities.put("admin", createMockUserAuthenticationEntity("admin"));

      expect(users.getUserAuthenticationEntities((String)null, null)).andReturn(entities.values()).once();
    } else {
      expect(users.getUserAuthenticationEntities("user1", null)).andReturn(entities.values()).once();
    }

    replayAll();

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = getResourceProvider(injector);

    Set<String> propertyIds = new HashSet<>();
    propertyIds.add(UserAuthenticationSourceResourceProvider.AUTHENTICATION_USER_NAME_PROPERTY_ID);
    propertyIds.add(UserAuthenticationSourceResourceProvider.AUTHENTICATION_AUTHENTICATION_TYPE_PROPERTY_ID);
    propertyIds.add(UserAuthenticationSourceResourceProvider.AUTHENTICATION_KEY_PROPERTY_ID);
    propertyIds.add(UserAuthenticationSourceResourceProvider.AUTHENTICATION_CREATED_PROPERTY_ID);
    propertyIds.add(UserAuthenticationSourceResourceProvider.AUTHENTICATION_UPDATED_PROPERTY_ID);

    Request request = PropertyHelper.getReadRequest(propertyIds);

    Set<Resource> resources = provider.getResources(request, null);

    Assert.assertEquals(entities.size(), resources.size());
    for (Resource resource : resources) {
      String userName = (String) resource.getPropertyValue(UserAuthenticationSourceResourceProvider.AUTHENTICATION_USER_NAME_PROPERTY_ID);
      Assert.assertTrue(entities.containsKey(userName));

      // This value should never come back...
      Assert.assertNull(resource.getPropertyValue(UserAuthenticationSourceResourceProvider.AUTHENTICATION_KEY_PROPERTY_ID));
    }

    verifyAll();
  }

  private void getResourceTest(Authentication authentication, String requestedUsername) throws Exception {
    Injector injector = createInjector();

    List<UserAuthenticationEntity> entities = new ArrayList<>();
    entities.add(createMockUserAuthenticationEntity(requestedUsername));

    Users users = injector.getInstance(Users.class);
    expect(users.getUserAuthenticationEntities(requestedUsername, null)).andReturn(entities).once();

    replayAll();

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = getResourceProvider(injector);

    Set<String> propertyIds = new HashSet<>();
    propertyIds.add(UserAuthenticationSourceResourceProvider.AUTHENTICATION_USER_NAME_PROPERTY_ID);
    propertyIds.add(UserAuthenticationSourceResourceProvider.AUTHENTICATION_KEY_PROPERTY_ID);

    Request request = PropertyHelper.getReadRequest(propertyIds);

    Set<Resource> resources = provider.getResources(request, createPredicate(requestedUsername, null));

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      String userName = (String) resource.getPropertyValue(UserAuthenticationSourceResourceProvider.AUTHENTICATION_USER_NAME_PROPERTY_ID);
      Assert.assertEquals(requestedUsername, userName);

      // This value should never come back...
      Assert.assertNull(resource.getPropertyValue(UserAuthenticationSourceResourceProvider.AUTHENTICATION_KEY_PROPERTY_ID));
    }

    verifyAll();
  }

  private void updateResources_SetAuthenticationKey(Authentication authentication, String requestedUsername, String authenticationType) throws Exception {
    Injector injector = createInjector();

    UserAuthenticationEntity userAuthenticationEntity = createMockUserAuthenticationEntity(requestedUsername);

    boolean isSelf = authentication.getName().equalsIgnoreCase(requestedUsername);

    List<UserAuthenticationEntity> userAuthenticationEntities = new ArrayList<>();
    userAuthenticationEntities.add(userAuthenticationEntity);

    UserEntity userEntity = createMock(UserEntity.class);
    expect(userEntity.getAuthenticationEntities()).andReturn(userAuthenticationEntities).once();
    if (isSelf) {
      expect(userEntity.getUserId()).andReturn(((AmbariUserDetails) authentication.getPrincipal()).getUserId()).once();
    } else {
      expect(userEntity.getUserId()).andReturn(AuthorizationHelper.getAuthenticatedId() + 100).once();
    }

    Users users = injector.getInstance(Users.class);
    expect(users.getUserEntity(requestedUsername)).andReturn(userEntity).once();
    users.modifyAuthentication(userAuthenticationEntity, "old_password", "new_password", isSelf);
    expectLastCall().once();

    replayAll();

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = getResourceProvider(injector);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(UserAuthenticationSourceResourceProvider.AUTHENTICATION_OLD_KEY_PROPERTY_ID, "old_password");
    properties.put(UserAuthenticationSourceResourceProvider.AUTHENTICATION_KEY_PROPERTY_ID, "new_password");

    if (authenticationType != null) {
      properties.put(UserAuthenticationSourceResourceProvider.AUTHENTICATION_AUTHENTICATION_TYPE_PROPERTY_ID, authenticationType);
    }

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    provider.updateResources(request, createPredicate(requestedUsername, userAuthenticationEntity.getUserAuthenticationId()));

    verifyAll();
  }

  private void deleteResourcesTest(Authentication authentication, String requestedUsername) throws Exception {
    Injector injector = createInjector();

    Users users = injector.getInstance(Users.class);
    users.removeAuthentication(requestedUsername, 1L);
    expectLastCall().atLeastOnce();

    // replay
    replayAll();

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = getResourceProvider(injector);

    provider.deleteResources(new RequestImpl(null, null, null, null), createPredicate(requestedUsername, 1L));

    // verify
    verifyAll();
  }


  private Predicate createPredicate(String requestedUsername, Long sourceId) {
    Predicate predicate1 = new PredicateBuilder()
        .property(UserAuthenticationSourceResourceProvider.AUTHENTICATION_USER_NAME_PROPERTY_ID)
        .equals(requestedUsername)
        .toPredicate();

    if (sourceId == null) {
      return predicate1;
    } else {
      Predicate predicate2 = new PredicateBuilder()
          .property(UserAuthenticationSourceResourceProvider.AUTHENTICATION_AUTHENTICATION_SOURCE_ID_PROPERTY_ID)
          .equals(sourceId.toString())
          .toPredicate();
      return new AndPredicate(predicate1, predicate2);
    }
  }

  private UserAuthenticationEntity createMockUserAuthenticationEntity(String username) {
    UserAuthenticationEntity entity = createMock(UserAuthenticationEntity.class);
    UserEntity userEntity = createMock(UserEntity.class);
    expect(entity.getAuthenticationType()).andReturn(UserAuthenticationType.LOCAL).anyTimes();
    expect(entity.getAuthenticationKey()).andReturn("this is a secret").anyTimes();
    expect(entity.getCreateTime()).andReturn(CREATE_TIME).anyTimes();
    expect(entity.getUpdateTime()).andReturn(UPDATE_TIME).anyTimes();
    expect(entity.getUserAuthenticationId()).andReturn(100L).anyTimes();
    expect(entity.getUser()).andReturn(userEntity).anyTimes();

    expect(userEntity.getUserName()).andReturn(username).anyTimes();
    return entity;
  }

  private ResourceProvider getResourceProvider(Injector injector) {
    UserAuthenticationSourceResourceProvider resourceProvider = new UserAuthenticationSourceResourceProvider();

    injector.injectMembers(resourceProvider);
    return resourceProvider;
  }
}