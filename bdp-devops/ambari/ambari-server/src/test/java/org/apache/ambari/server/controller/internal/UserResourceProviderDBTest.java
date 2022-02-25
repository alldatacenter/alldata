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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replay;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ResourceProviderFactory;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * Tests creation, retrieval and deletion of users using an in-memory database.
 * Also tests user creation and retrieval using usernames that differ only by case.
 * Verifies that usernames are stored as provided.
 */
@PrepareForTest({AuthorizationHelper.class})
public class UserResourceProviderDBTest {
  private static Injector injector;
  private static AmbariManagementController amc;
  private static Resource.Type userType = Resource.Type.User;
  private static UserResourceProvider userResourceProvider;
  private static UserAuthenticationSourceResourceProvider userAuthenticationSourceResourceProvider;
  private static String JDBC_IN_MEMORY_URL_CREATE =
      String.format("jdbc:derby:memory:myDB/%s;create=true", Configuration.DEFAULT_DERBY_SCHEMA);
  private static String JDBC_IN_MEMORY_URL_DROP =
      String.format("jdbc:derby:memory:myDB/%s;drop=true", Configuration.DEFAULT_DERBY_SCHEMA);

  /**
   * Sets up the in-memory database for the test suite.
   */
  @Before
  public void setupInMemoryDB() {
    InMemoryDefaultTestModule testModule = new InMemoryDefaultTestModule();

    Properties properties = testModule.getProperties();
    properties.setProperty(Configuration.SERVER_JDBC_URL.getKey(), JDBC_IN_MEMORY_URL_CREATE);
    properties.setProperty(Configuration.SERVER_JDBC_DRIVER.getKey(), Configuration.JDBC_IN_MEMORY_DRIVER);
    injector = Guice.createInjector(testModule);

    injector.getInstance(PersistService.class).start();

    amc = injector.getInstance(AmbariManagementController.class);

    userResourceProvider = new UserResourceProvider(amc);
    injector.injectMembers(userResourceProvider);

    userAuthenticationSourceResourceProvider = new UserAuthenticationSourceResourceProvider();
    injector.injectMembers(userAuthenticationSourceResourceProvider);


    ResourceProviderFactory factory = createMock(ResourceProviderFactory.class);
    expect(factory.getUserAuthenticationSourceResourceProvider()).andReturn(userAuthenticationSourceResourceProvider).anyTimes();
    replay(factory);
    AbstractControllerResourceProvider.init(factory);

  }

  /**
   * Closes the JPA connection after executing the test suite.
   */
  @After
  public void teardownInMemoryDB() throws AmbariException, SQLException {
    if (injector != null) {
      H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
    }
  }

  /**
   * Creates a user, retrieves it and verifies that the username matches the one that was
   * created. Deletes the created user and verifies that the username was deleted.
   *
   * @throws Exception
   */
  @Test
  public void createUserTest() throws Exception {
    Authentication authentication = TestAuthenticationFactory.createAdministrator();
    SecurityContextHolder.getContext().setAuthentication(authentication);

    // create a new user viewUser
    Map<String, Object> requestProperties = new HashMap<>();
    requestProperties.put(UserResourceProvider.USER_USERNAME_PROPERTY_ID, "viewUser");
    requestProperties.put(UserResourceProvider.USER_PASSWORD_PROPERTY_ID, "password");
    requestProperties.put(UserResourceProvider.USER_ADMIN_PROPERTY_ID, false);
    requestProperties.put(UserResourceProvider.USER_ACTIVE_PROPERTY_ID, true);

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProperties), null);
    RequestStatus requestStatus = userResourceProvider.createResources(request);
    assertNotNull(requestStatus);

    // verify the created username
    Request getRequest = PropertyHelper.getReadRequest(new HashSet<>(Collections.singleton("Users")));
    Predicate predicate = new PredicateBuilder()
        .property(UserResourceProvider.USER_USERNAME_PROPERTY_ID).equals("viewUser").toPredicate();
    Set<Resource> resources = userResourceProvider.getResources(getRequest, predicate);
    assertEquals(resources.size(), 1);
    Resource resource = resources.iterator().next();

    String userName = resource.getPropertyValue(UserResourceProvider.USER_USERNAME_PROPERTY_ID).toString();
    assertEquals("viewuser", userName);

    // delete the created username
    requestStatus = userResourceProvider.deleteResources(request, predicate);
    assertNotNull(requestStatus);

    // verify that the username was deleted
    resources = userResourceProvider.getResources(getRequest, null);
    assertEquals(resources.size(), 0);
  }

  /**
   * Creates a username in all lowercase. Attempt to add another user whose username differs only
   * by case to the previously added user. Verifies that the user cannot be added.
   *
   * @throws Exception
   */
  @Test
  public void createExistingUserTest() throws Exception {
    Authentication authentication = TestAuthenticationFactory.createAdministrator();
    SecurityContextHolder.getContext().setAuthentication(authentication);

        /* add a new user */
    Map<String, Object> requestProperties = new HashMap<>();
    requestProperties.put(UserResourceProvider.USER_USERNAME_PROPERTY_ID, "abcd");
    requestProperties.put(UserResourceProvider.USER_PASSWORD_PROPERTY_ID, "password");
    requestProperties.put(UserResourceProvider.USER_ADMIN_PROPERTY_ID, false);
    requestProperties.put(UserResourceProvider.USER_ACTIVE_PROPERTY_ID, true);

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProperties), null);
    RequestStatus requestStatus = userResourceProvider.createResources(request);
    assertNotNull(requestStatus);

        /* try with uppercase version of an existing user */
    requestProperties.put(UserResourceProvider.USER_USERNAME_PROPERTY_ID, "ABCD");
    request = PropertyHelper.getCreateRequest(Collections.singleton(requestProperties), null);
    try {
      requestStatus = userResourceProvider.createResources(request);
      assertTrue("Should fail with user exists", false);
    } catch (Exception ex) {
      assertTrue(ex.getMessage().contains("already exists"));
    }

    // delete the created username
    Predicate predicate = new PredicateBuilder()
        .property(UserResourceProvider.USER_USERNAME_PROPERTY_ID).equals("abcd").toPredicate();
    requestStatus = userResourceProvider.deleteResources(request, predicate);
    assertNotNull(requestStatus);

    // verify that the username was deleted
    Request getRequest = PropertyHelper.getReadRequest(new HashSet<>(Arrays.asList("Users")));
    Set<Resource> resources = userResourceProvider.getResources(getRequest, null);
    assertEquals(resources.size(), 0);
  }

  /**
   * Creates a user and retrieves the user using the same username but in lowercase. Verifies
   * that the retrieval is successful and that the retrieved username is the same as the one
   * that was used during creation.
   *
   * @throws Exception
   */
  @Test
  public void getExistingUser() throws Exception {
    Authentication authentication = TestAuthenticationFactory.createAdministrator();
    SecurityContextHolder.getContext().setAuthentication(authentication);

    // create a new user viewUser
    Map<String, Object> requestProperties = new HashMap<>();
    requestProperties.put(UserResourceProvider.USER_USERNAME_PROPERTY_ID, "viewUser");
    requestProperties.put(UserResourceProvider.USER_PASSWORD_PROPERTY_ID, "password");
    requestProperties.put(UserResourceProvider.USER_ADMIN_PROPERTY_ID, false);
    requestProperties.put(UserResourceProvider.USER_ACTIVE_PROPERTY_ID, true);

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProperties), null);
    RequestStatus requestStatus = userResourceProvider.createResources(request);
    assertNotNull(requestStatus);

    // verify the created username
    Request getRequest = PropertyHelper.getReadRequest(new HashSet<>(Arrays.asList("Users")));
    Predicate predicate = new PredicateBuilder()
        .property(UserResourceProvider.USER_USERNAME_PROPERTY_ID).equals("viewuser").toPredicate();
    Set<Resource> resources = userResourceProvider.getResources(getRequest, predicate);
    assertEquals(resources.size(), 1);
    Resource resource = resources.iterator().next();

    String userName = resource.getPropertyValue(UserResourceProvider.USER_USERNAME_PROPERTY_ID).toString();
    assertEquals("viewuser", userName);

    // delete the created username
    requestStatus = userResourceProvider.deleteResources(request, predicate);
    assertNotNull(requestStatus);

    // verify that the username was deleted
    resources = userResourceProvider.getResources(getRequest, null);
    assertEquals(resources.size(), 0);
  }

  /**
   * Adds an array of users, retrieves the users and verifies that the usernames do not differ
   * from the ones that were used during creation.
   *
   * @throws Exception
   */
  @Test
  public void getAllUserTest() throws Exception {
    Authentication authentication = TestAuthenticationFactory.createAdministrator();
    SecurityContextHolder.getContext().setAuthentication(authentication);

    List<String> userNames = Arrays.asList("user1", "uSer2", "User3", "useR4");
    List<String> lowercaseUserNames = new ArrayList<>();

    for (String username : userNames) {
      lowercaseUserNames.add(username.toLowerCase());
    }

    for (String userName : userNames) {
      Map<String, Object> requestProperties = new HashMap<>();
      requestProperties.put(UserResourceProvider.USER_USERNAME_PROPERTY_ID, userName);
      requestProperties.put(UserResourceProvider.USER_PASSWORD_PROPERTY_ID, "password");
      requestProperties.put(UserResourceProvider.USER_ADMIN_PROPERTY_ID, false);
      requestProperties.put(UserResourceProvider.USER_ACTIVE_PROPERTY_ID, true);

      Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProperties), null);
      RequestStatus requestStatus = userResourceProvider.createResources(request);
      assertNotNull(requestStatus);
    }

    // verify the created username
    Request getRequest = PropertyHelper.getReadRequest(Collections.singleton(("Users")));
    Set<Resource> resources = userResourceProvider.getResources(getRequest, null);
    for (Resource resource : resources) {
      System.out.println("Resource: " + resource.getPropertyValue(UserResourceProvider.USER_USERNAME_PROPERTY_ID).toString());
    }
    for (String s: lowercaseUserNames) {
      System.out.println("LC UN: " + s);
    }
    assertEquals(lowercaseUserNames.size(), resources.size());
    for (Resource resource : resources) {
      String userName = resource.getPropertyValue(UserResourceProvider.USER_USERNAME_PROPERTY_ID).toString();
      assertTrue(lowercaseUserNames.contains(userName));
    }

    // delete the users
    for (String userName : userNames) {
      Predicate predicate = new PredicateBuilder()
          .property(UserResourceProvider.USER_USERNAME_PROPERTY_ID).equals(userName).toPredicate();
      RequestStatus requestStatus = userResourceProvider.deleteResources(null /* not used */, predicate);
      assertNotNull(requestStatus);
    }

    // verify that the username was deleted
    resources = userResourceProvider.getResources(getRequest, null);
    assertEquals(resources.size(), 0);
  }
}
