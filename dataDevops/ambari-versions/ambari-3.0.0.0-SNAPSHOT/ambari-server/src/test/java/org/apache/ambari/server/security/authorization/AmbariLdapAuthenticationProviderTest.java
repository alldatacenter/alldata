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
package org.apache.ambari.server.security.authorization;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.ldap.service.AmbariLdapConfigurationProvider;
import org.apache.ambari.server.orm.entities.UserAuthenticationEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.ClientSecurityType;
import org.apache.ambari.server.security.authentication.AmbariUserAuthentication;
import org.apache.ambari.server.security.authentication.InvalidUsernamePasswordCombinationException;
import org.easymock.EasyMockSupport;
import org.junit.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.LdapUserDetails;

public class AmbariLdapAuthenticationProviderTest extends EasyMockSupport {
  private static final String ALLOWED_USER_NAME = "allowedUser";
  private static final String ALLOWED_USER_DN = "uid=alloweduser,ou=people,dc=ambari,dc=apache,dc=org";

  @Test(expected = InvalidUsernamePasswordCombinationException.class)
  public void testBadCredential() throws Exception {
    Authentication authentication = new UsernamePasswordAuthenticationToken(ALLOWED_USER_NAME, "password");

    Configuration configuration = createMockConfiguration(ClientSecurityType.LDAP);

    Users users = createMock(Users.class);
    AmbariLdapConfigurationProvider ldapConfigurationProvider = createMock(AmbariLdapConfigurationProvider.class);
    AmbariLdapAuthoritiesPopulator authoritiesPopulator = createMock(AmbariLdapAuthoritiesPopulator.class);

    LdapAuthenticationProvider ldapAuthenticationProvider = createMock(LdapAuthenticationProvider.class);
    expect(ldapAuthenticationProvider.authenticate(authentication)).andThrow(new BadCredentialsException("")).once();

    AmbariLdapAuthenticationProvider authenticationProvider = createMockBuilder(AmbariLdapAuthenticationProvider.class)
        .withConstructor(users, configuration, ldapConfigurationProvider, authoritiesPopulator)
        .addMockedMethod("loadLdapAuthenticationProvider")
        .createMock();
    expect(authenticationProvider.loadLdapAuthenticationProvider(ALLOWED_USER_NAME)).andReturn(ldapAuthenticationProvider).once();

    replayAll();

    authenticationProvider.authenticate(authentication);
  }

  @Test
  public void testAuthenticate() throws Exception {
    Authentication authentication = new UsernamePasswordAuthenticationToken(ALLOWED_USER_NAME, "password");

    LdapUserDetails ldapUserDetails = createMock(LdapUserDetails.class);
    expect(ldapUserDetails.getDn()).andReturn(ALLOWED_USER_DN).atLeastOnce();

    Authentication authenticatedAuthentication = createMock(Authentication.class);
    expect(authenticatedAuthentication.getPrincipal()).andReturn(ldapUserDetails).atLeastOnce();

    Configuration configuration = createMockConfiguration(ClientSecurityType.LDAP);

    UserEntity userEntity = createMock(UserEntity.class);

    UserAuthenticationEntity userAuthenticationEntity = createMock(UserAuthenticationEntity.class);
    expect(userAuthenticationEntity.getUser()).andReturn(userEntity).atLeastOnce();

    User user = createMock(User.class);

    Users users = createMock(Users.class);
    expect(users.getUserAuthenticationEntities(UserAuthenticationType.LDAP, ALLOWED_USER_DN)).andReturn(Collections.singleton(userAuthenticationEntity)).atLeastOnce();
    users.validateLogin(userEntity, ALLOWED_USER_NAME);
    expectLastCall().atLeastOnce();
    expect(users.getUser(userEntity)).andReturn(user).atLeastOnce();
    expect(users.getUserAuthorities(userEntity)).andReturn(Collections.emptyList()).atLeastOnce();

    AmbariLdapConfigurationProvider ldapConfigurationProvider = createMock(AmbariLdapConfigurationProvider.class);

    AmbariLdapAuthoritiesPopulator authoritiesPopulator = createMock(AmbariLdapAuthoritiesPopulator.class);

    LdapAuthenticationProvider ldapAuthenticationProvider = createMock(LdapAuthenticationProvider.class);
    expect(ldapAuthenticationProvider.authenticate(authentication)).andReturn(authenticatedAuthentication).once();

    AmbariLdapAuthenticationProvider authenticationProvider = createMockBuilder(AmbariLdapAuthenticationProvider.class)
        .withConstructor(users, configuration, ldapConfigurationProvider, authoritiesPopulator)
        .addMockedMethod("loadLdapAuthenticationProvider")
        .createMock();
    expect(authenticationProvider.loadLdapAuthenticationProvider(ALLOWED_USER_NAME)).andReturn(ldapAuthenticationProvider).once();

    replayAll();

    Authentication result = authenticationProvider.authenticate(authentication);
    assertTrue(result instanceof AmbariUserAuthentication);
    assertTrue(result.isAuthenticated());

    verifyAll();
  }

  @Test
  public void testDisabled() throws Exception {
    Authentication authentication = new UsernamePasswordAuthenticationToken(ALLOWED_USER_NAME, "password");

    Configuration configuration = createMockConfiguration(ClientSecurityType.LOCAL);
    Users users = createMock(Users.class);
    AmbariLdapConfigurationProvider ldapConfigurationProvider = createMock(AmbariLdapConfigurationProvider.class);
    AmbariLdapAuthoritiesPopulator authoritiesPopulator = createMock(AmbariLdapAuthoritiesPopulator.class);

    AmbariLdapAuthenticationProvider authenticationProvider = createMockBuilder(AmbariLdapAuthenticationProvider.class)
        .withConstructor(users, configuration, ldapConfigurationProvider, authoritiesPopulator)
        .addMockedMethod("loadLdapAuthenticationProvider")
        .createMock();

    replayAll();

    Authentication result = authenticationProvider.authenticate(authentication);
    assertNull(result);

    verifyAll();
  }

  private Configuration createMockConfiguration(ClientSecurityType clientSecurityType) {
    Configuration configuration = createMock(Configuration.class);
    expect(configuration.getClientSecurityType()).andReturn(clientSecurityType).atLeastOnce();
    return configuration;
  }

}
