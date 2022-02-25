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

package org.apache.ambari.server.security.authentication;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.persistence.EntityManager;

import org.apache.ambari.server.configuration.AmbariServerConfiguration;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.hooks.HookContextFactory;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.ldap.service.AmbariLdapConfigurationProvider;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.security.encryption.Encryptor;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

public abstract class AbstractAuthenticationProviderTest extends EasyMockSupport {

  static final String TEST_USER_NAME = "userName";

  @Before
  public void setUp() throws Exception {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @After
  public void cleanUp() throws Exception {
    SecurityContextHolder.getContext().setAuthentication(null);
  }


  @Test
  public void testAuthenticationSuccess() {
    Injector injector = getInjector();

    UserEntity userEntity = getUserEntity(injector, TEST_USER_NAME, 9, true);

    Users users = injector.getInstance(Users.class);
    expect(users.getUserEntity(TEST_USER_NAME)).andReturn(userEntity).atLeastOnce();
    expect(users.getUserAuthorities(userEntity)).andReturn(null).atLeastOnce();

    Authentication authentication = getAuthentication(true, true);

    replayAll();

    AuthenticationProvider provider = getAuthenticationProvider(injector);
    Authentication result = provider.authenticate(authentication);

    verifyAll();

    assertNotNull(result);
    assertEquals(true, result.isAuthenticated());
    assertTrue(result instanceof AmbariUserAuthentication);

    validateAuthenticationResult((AmbariUserAuthentication) result);
  }

  @Test(expected = AmbariAuthenticationException.class)
  public void testAuthenticationWithIncorrectUserName() {
    Injector injector = getInjector();

    Authentication authentication = getAuthentication(false, true);

    Users users = injector.getInstance(Users.class);
    expect(users.getUserEntity(anyString())).andReturn(null).atLeastOnce();

    replayAll();

    AuthenticationProvider provider = getAuthenticationProvider(injector);
    provider.authenticate(authentication);
  }


  @Test(expected = AmbariAuthenticationException.class)
  public void testAuthenticationWithoutCredentials() {
    Injector injector = getInjector();

    UserEntity userEntity = getUserEntity(injector, TEST_USER_NAME, 0, true);

    Users users = injector.getInstance(Users.class);
    expect(users.getUserEntity(TEST_USER_NAME)).andReturn(userEntity).atLeastOnce();
    expect(users.getUserAuthorities(userEntity)).andReturn(null).atLeastOnce();

    Authentication authentication = createMock(Authentication.class);
    expect(authentication.getName()).andReturn(TEST_USER_NAME).atLeastOnce();
    expect(authentication.getCredentials()).andReturn(null).atLeastOnce();

    replayAll();

    AuthenticationProvider provider = getAuthenticationProvider(injector);
    provider.authenticate(authentication);
  }


  @Test(expected = AmbariAuthenticationException.class)
  public void testAuthenticationWithIncorrectCredential() {
    Injector injector = getInjector();

    UserEntity userEntity = getUserEntity(injector, TEST_USER_NAME, 0, true);

    Users users = injector.getInstance(Users.class);
    expect(users.getUserEntity(TEST_USER_NAME)).andReturn(userEntity).atLeastOnce();
    expect(users.getUserAuthorities(userEntity)).andReturn(null).atLeastOnce();

    Authentication authentication = getAuthentication(true, false);

    replayAll();

    AuthenticationProvider provider = getAuthenticationProvider(injector);
    provider.authenticate(authentication);
  }

  @Test(expected = TooManyLoginFailuresException.class)
  public void testUserIsLockedOutAfterConsecutiveFailures() {
    Injector injector = getInjector();

    // Force the user to have more than 10 consecutive failures
    UserEntity userEntity = getUserEntity(injector, TEST_USER_NAME, 11, true);

    Users users = injector.getInstance(Users.class);
    expect(users.getUserEntity(TEST_USER_NAME)).andReturn(userEntity).atLeastOnce();

    Authentication authentication = getAuthentication(true, true);

    replayAll();

    AmbariLocalAuthenticationProvider ambariLocalAuthenticationProvider = injector.getInstance(AmbariLocalAuthenticationProvider.class);
    ambariLocalAuthenticationProvider.authenticate(authentication);
  }

  @Test(expected = AccountDisabledException.class)
  public void testUserIsInactive() {
    Injector injector = getInjector();

    // Force the user to be inactive
    UserEntity userEntity = getUserEntity(injector, TEST_USER_NAME, 10, false);

    Users users = injector.getInstance(Users.class);
    expect(users.getUserEntity(TEST_USER_NAME)).andReturn(userEntity).atLeastOnce();

    Authentication authentication = getAuthentication(true, true);

    replayAll();

    AmbariLocalAuthenticationProvider ambariLocalAuthenticationProvider = injector.getInstance(AmbariLocalAuthenticationProvider.class);
    ambariLocalAuthenticationProvider.authenticate(authentication);
  }

  protected Injector getInjector() {
    final Users users = createMockBuilder(Users.class)
        .addMockedMethod("getUserEntity",  String.class)
        .addMockedMethod("getUserAuthorities", UserEntity.class)
        .createMock();

    return Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        Configuration configuration = createNiceMock(Configuration.class);
        expect(configuration.getMaxAuthenticationFailures()).andReturn(10).anyTimes();
        expect(configuration.showLockedOutUserMessage()).andReturn(true).anyTimes();

        bind(EntityManager.class).toInstance(createMock(EntityManager.class));
        bind(DBAccessor.class).toInstance(createMock(DBAccessor.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(HookService.class).toInstance(createMock(HookService.class));
        bind(HookContextFactory.class).toInstance(createMock(HookContextFactory.class));
        bind(new TypeLiteral<Encryptor<AmbariServerConfiguration>>() {}).annotatedWith(Names.named("AmbariServerConfigurationEncryptor")).toInstance(Encryptor.NONE);
        bind(AmbariLdapConfigurationProvider.class).toInstance(createMock(AmbariLdapConfigurationProvider.class));

        bind(Users.class).toInstance(users);
        bind(Configuration.class).toInstance(configuration);
      }
    }, getAdditionalModule());

  }

  protected abstract AuthenticationProvider getAuthenticationProvider(Injector injector);

  protected abstract Authentication getAuthentication(boolean correctUsername, boolean correctCredential);

  protected abstract UserEntity getUserEntity(Injector injector, String username, int consecutiveFailures, boolean active);

  protected abstract Module getAdditionalModule();

  protected abstract void validateAuthenticationResult(AmbariUserAuthentication result);

}
