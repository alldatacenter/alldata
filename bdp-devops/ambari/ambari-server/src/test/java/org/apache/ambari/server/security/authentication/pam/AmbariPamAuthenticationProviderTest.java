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
package org.apache.ambari.server.security.authentication.pam;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.util.Collections;
import java.util.Properties;

import javax.persistence.EntityManager;

import org.apache.ambari.server.configuration.AmbariServerConfiguration;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.hooks.HookContextFactory;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.ldap.service.AmbariLdapConfigurationProvider;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.UserAuthenticationEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.ClientSecurityType;
import org.apache.ambari.server.security.authentication.AccountDisabledException;
import org.apache.ambari.server.security.authentication.AmbariUserAuthentication;
import org.apache.ambari.server.security.authentication.TooManyLoginFailuresException;
import org.apache.ambari.server.security.authorization.User;
import org.apache.ambari.server.security.authorization.UserAuthenticationType;
import org.apache.ambari.server.security.authorization.UserName;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.security.encryption.Encryptor;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.libpam.PAM;
import org.jvnet.libpam.PAMException;
import org.jvnet.libpam.UnixUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import junit.framework.Assert;

public class AmbariPamAuthenticationProviderTest extends EasyMockSupport {

  private static final String TEST_USER_NAME = "userName";
  private static final String TEST_USER_PASS = "userPass";
  private static final String TEST_USER_INCORRECT_PASS = "userIncorrectPass";

  private Injector injector;

  @Before
  public void setup() {
    final Users users = createMockBuilder(Users.class)
        .addMockedMethod("getUserEntity", String.class)
        .addMockedMethod("getUserAuthorities", UserEntity.class)
        .addMockedMethod("createUser", String.class, String.class, String.class, Boolean.class)
        .addMockedMethod("addPamAuthentication", UserEntity.class, String.class)
        .addMockedMethod("getUser", UserEntity.class)
        .createMock();

    Properties properties = new Properties();
    properties.setProperty(Configuration.CLIENT_SECURITY.getKey(), ClientSecurityType.PAM.name());
    properties.setProperty(Configuration.PAM_CONFIGURATION_FILE.getKey(), "ambari-pam");
    properties.setProperty(Configuration.SHOW_LOCKED_OUT_USER_MESSAGE.getKey(), "true");
    properties.setProperty(Configuration.MAX_LOCAL_AUTHENTICATION_FAILURES.getKey(), "10");

    final Configuration configuration = new Configuration(properties);

    injector = Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(HookContextFactory.class).toInstance(createNiceMock(HookContextFactory.class));
        bind(HookService.class).toInstance(createNiceMock(HookService.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(PamAuthenticationFactory.class).toInstance(createMock(PamAuthenticationFactory.class));
        bind(PasswordEncoder.class).toInstance(new StandardPasswordEncoder());
        bind(Users.class).toInstance(users);
        bind(Configuration.class).toInstance(configuration);
        bind(new TypeLiteral<Encryptor<AmbariServerConfiguration>>() {}).annotatedWith(Names.named("AmbariServerConfigurationEncryptor")).toInstance(Encryptor.NONE);
        bind(AmbariLdapConfigurationProvider.class).toInstance(createMock(AmbariLdapConfigurationProvider.class));
      }
    });
  }

  @Test(expected = AuthenticationException.class)
  public void testBadCredential() throws Exception {

    PAM pam = createMock(PAM.class);
    expect(pam.authenticate(eq(TEST_USER_NAME), eq(TEST_USER_INCORRECT_PASS)))
        .andThrow(new PAMException())
        .once();
    pam.dispose();
    expectLastCall().once();

    PamAuthenticationFactory pamAuthenticationFactory = injector.getInstance(PamAuthenticationFactory.class);
    expect(pamAuthenticationFactory.createInstance(injector.getInstance(Configuration.class))).andReturn(pam).once();

    Users users = injector.getInstance(Users.class);
    expect(users.getUserEntity(TEST_USER_NAME)).andReturn(null).once();

    replayAll();

    Authentication authentication = new UsernamePasswordAuthenticationToken(TEST_USER_NAME, TEST_USER_INCORRECT_PASS);

    AmbariPamAuthenticationProvider authenticationProvider = injector.getInstance(AmbariPamAuthenticationProvider.class);
    authenticationProvider.authenticate(authentication);

    verifyAll();
  }

  @Test
  public void testAuthenticateExistingUser() throws Exception {

    UnixUser unixUser = createNiceMock(UnixUser.class);

    PAM pam = createMock(PAM.class);
    expect(pam.authenticate(eq(TEST_USER_NAME), eq(TEST_USER_PASS))).andReturn(unixUser).once();
    pam.dispose();
    expectLastCall().once();

    PamAuthenticationFactory pamAuthenticationFactory = injector.getInstance(PamAuthenticationFactory.class);
    expect(pamAuthenticationFactory.createInstance(injector.getInstance(Configuration.class))).andReturn(pam).once();

    UserEntity userEntity = combineUserEntity(true, true, 0);

    Users users = injector.getInstance(Users.class);
    expect(users.getUserEntity(TEST_USER_NAME)).andReturn(userEntity).once();
    expect(users.getUser(userEntity)).andReturn(new User(userEntity)).once();
    expect(users.getUserAuthorities(userEntity)).andReturn(null).once();

    replayAll();

    Authentication authentication = new UsernamePasswordAuthenticationToken(TEST_USER_NAME, TEST_USER_PASS);
    AmbariPamAuthenticationProvider authenticationProvider = injector.getInstance(AmbariPamAuthenticationProvider.class);

    Authentication result = authenticationProvider.authenticate(authentication);
    Assert.assertNotNull(result);
    Assert.assertEquals(true, result.isAuthenticated());
    Assert.assertTrue(result instanceof AmbariUserAuthentication);

    verifyAll();
  }

  @Test(expected = AccountDisabledException.class)
  public void testAuthenticateDisabledUser() throws Exception {

    UnixUser unixUser = createNiceMock(UnixUser.class);

    PAM pam = createMock(PAM.class);
    expect(pam.authenticate(eq(TEST_USER_NAME), eq(TEST_USER_PASS))).andReturn(unixUser).once();
    pam.dispose();
    expectLastCall().once();

    PamAuthenticationFactory pamAuthenticationFactory = injector.getInstance(PamAuthenticationFactory.class);
    expect(pamAuthenticationFactory.createInstance(injector.getInstance(Configuration.class))).andReturn(pam).once();

    UserEntity userEntity = combineUserEntity(true, false, 0);

    Users users = injector.getInstance(Users.class);
    expect(users.getUserEntity(TEST_USER_NAME)).andReturn(userEntity).once();

    replayAll();

    Authentication authentication = new UsernamePasswordAuthenticationToken(TEST_USER_NAME, TEST_USER_PASS);
    AmbariPamAuthenticationProvider authenticationProvider = injector.getInstance(AmbariPamAuthenticationProvider.class);
    authenticationProvider.authenticate(authentication);

    verifyAll();
  }

  @Test(expected = TooManyLoginFailuresException.class)
  public void testAuthenticateLockedUser() throws Exception {

    UnixUser unixUser = createNiceMock(UnixUser.class);

    PAM pam = createMock(PAM.class);
    expect(pam.authenticate(eq(TEST_USER_NAME), eq(TEST_USER_PASS))).andReturn(unixUser).once();
    pam.dispose();
    expectLastCall().once();

    PamAuthenticationFactory pamAuthenticationFactory = injector.getInstance(PamAuthenticationFactory.class);
    expect(pamAuthenticationFactory.createInstance(injector.getInstance(Configuration.class))).andReturn(pam).once();

    UserEntity userEntity = combineUserEntity(true, true, 11);

    Users users = injector.getInstance(Users.class);
    expect(users.getUserEntity(TEST_USER_NAME)).andReturn(userEntity).once();

    replayAll();

    Authentication authentication = new UsernamePasswordAuthenticationToken(TEST_USER_NAME, TEST_USER_PASS);
    AmbariPamAuthenticationProvider authenticationProvider = injector.getInstance(AmbariPamAuthenticationProvider.class);
    authenticationProvider.authenticate(authentication);

    verifyAll();
  }

  @Test
  public void testAuthenticateNewUser() throws Exception {

    UnixUser unixUser = createNiceMock(UnixUser.class);
    expect(unixUser.getUserName()).andReturn(TEST_USER_NAME.toLowerCase()).atLeastOnce();

    PAM pam = createMock(PAM.class);
    expect(pam.authenticate(eq(TEST_USER_NAME), eq(TEST_USER_PASS))).andReturn(unixUser).once();
    pam.dispose();
    expectLastCall().once();

    PamAuthenticationFactory pamAuthenticationFactory = injector.getInstance(PamAuthenticationFactory.class);
    expect(pamAuthenticationFactory.createInstance(injector.getInstance(Configuration.class))).andReturn(pam).once();

    UserEntity userEntity = combineUserEntity(false, true, 0);

    Users users = injector.getInstance(Users.class);
    expect(users.getUserEntity(TEST_USER_NAME)).andReturn(null).once();
    expect(users.createUser(TEST_USER_NAME, TEST_USER_NAME.toLowerCase(), TEST_USER_NAME, true)).andReturn(userEntity).once();
    users.addPamAuthentication(userEntity, TEST_USER_NAME.toLowerCase());
    expectLastCall().once();
    expect(users.getUser(userEntity)).andReturn(new User(userEntity)).once();
    expect(users.getUserAuthorities(userEntity)).andReturn(null).once();

    replayAll();

    Authentication authentication = new UsernamePasswordAuthenticationToken(TEST_USER_NAME, TEST_USER_PASS);
    AmbariPamAuthenticationProvider authenticationProvider = injector.getInstance(AmbariPamAuthenticationProvider.class);

    Authentication result = authenticationProvider.authenticate(authentication);
    Assert.assertNotNull(result);
    Assert.assertEquals(true, result.isAuthenticated());
    Assert.assertTrue(result instanceof AmbariUserAuthentication);

    verifyAll();
  }

  @Test
  public void testDisabled() throws Exception {

    Configuration configuration = injector.getInstance(Configuration.class);
    configuration.setClientSecurityType(ClientSecurityType.LOCAL);

    Authentication authentication = new UsernamePasswordAuthenticationToken(TEST_USER_NAME, TEST_USER_PASS);

    AmbariPamAuthenticationProvider authenticationProvider = injector.getInstance(AmbariPamAuthenticationProvider.class);
    Authentication auth = authenticationProvider.authenticate(authentication);
    Assert.assertTrue(auth == null);
  }

  private UserEntity combineUserEntity(boolean addAuthentication, Boolean active, Integer consecutiveFailures) {
    PrincipalEntity principalEntity = new PrincipalEntity();

    UserEntity userEntity = new UserEntity();
    userEntity.setUserId(1);
    userEntity.setUserName(UserName.fromString(TEST_USER_NAME).toString());
    userEntity.setLocalUsername(TEST_USER_NAME);
    userEntity.setPrincipal(principalEntity);
    userEntity.setActive(active);
    userEntity.setConsecutiveFailures(consecutiveFailures);

    if (addAuthentication) {
      UserAuthenticationEntity userAuthenticationEntity = new UserAuthenticationEntity();
      userAuthenticationEntity.setAuthenticationType(UserAuthenticationType.PAM);
      userAuthenticationEntity.setAuthenticationKey(TEST_USER_NAME);

      userEntity.setAuthenticationEntities(Collections.singletonList(userAuthenticationEntity));
    }
    return userEntity;
  }

}
