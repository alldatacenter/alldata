/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.security.authorization;

import static java.lang.Boolean.parseBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;

import org.apache.ambari.server.configuration.AmbariServerConfigurationKey;
import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class AmbariLdapBindAuthenticatorTest extends EasyMockSupport {
  
  private Injector injector;
  private AmbariLdapConfiguration ldapConfiguration;
  
  @Before
  public void init() throws Exception {
    injector = createInjector();
    ldapConfiguration = injector.getInstance(AmbariLdapConfiguration.class);
  }

  @Test
  public void testAuthenticateWithoutLogin() throws Exception {
    testAuthenticate("username", "username", false);
  }

  @Test
  public void testAuthenticateWithNullLDAPUsername() throws Exception {
    testAuthenticate("username", null, false);
  }

  @Test
  public void testAuthenticateWithLoginAliasDefault() throws Exception {
    testAuthenticate("username", "ldapUsername", false);
  }

  @Test
  public void testAuthenticateWithLoginAliasForceToLower() throws Exception {
    testAuthenticate("username", "ldapUsername", true);
  }

  @Test
  public void testAuthenticateBadPassword() throws Exception {
    String basePathString = "dc=apache,dc=org";
    String ldapUserRelativeDNString = String.format("uid=%s,ou=people,ou=dev", "ldapUsername");
    LdapName ldapUserRelativeDN = new LdapName(ldapUserRelativeDNString);
    String ldapUserDNString = String.format("%s,%s", ldapUserRelativeDNString, basePathString);
    LdapName basePath = LdapUtils.newLdapName(basePathString);

    LdapContextSource ldapCtxSource = createMock(LdapContextSource.class);
    expect(ldapCtxSource.getBaseLdapName())
        .andReturn(basePath)
        .atLeastOnce();
    expect(ldapCtxSource.getContext(ldapUserDNString, "password"))
        .andThrow(new org.springframework.ldap.AuthenticationException(null))
        .once();

    DirContextOperations searchedUserContext = createMock(DirContextOperations.class);
    expect(searchedUserContext.getDn())
        .andReturn(ldapUserRelativeDN)
        .atLeastOnce();

    FilterBasedLdapUserSearch userSearch = createMock(FilterBasedLdapUserSearch.class);
    expect(userSearch.searchForUser(anyString())).andReturn(searchedUserContext).once();

    setupDatabaseConfigurationExpectations(false, false);

    replayAll();


    AmbariLdapBindAuthenticator bindAuthenticator = new AmbariLdapBindAuthenticator(ldapCtxSource, ldapConfiguration);
    bindAuthenticator.setUserSearch(userSearch);

    try {
      bindAuthenticator.authenticate(new UsernamePasswordAuthenticationToken("username", "password"));
      fail("Expected thrown exception: org.springframework.security.authentication.BadCredentialsException");
    } catch (org.springframework.security.authentication.BadCredentialsException e) {
      // expected
    } catch (Throwable t) {
      fail("Expected thrown exception: org.springframework.security.authentication.BadCredentialsException\nEncountered thrown exception " + t.getClass().getName());
    }

    verifyAll();
  }

  private void testAuthenticate(String ambariUsername, String ldapUsername, boolean forceUsernameToLower) throws Exception {
    String basePathString = "dc=apache,dc=org";
    String ldapUserRelativeDNString = String.format("uid=%s,ou=people,ou=dev", ldapUsername);
    LdapName ldapUserRelativeDN = new LdapName(ldapUserRelativeDNString);
    String ldapUserDNString = String.format("%s,%s", ldapUserRelativeDNString, basePathString);
    LdapName basePath = LdapUtils.newLdapName(basePathString);

    @SuppressWarnings("unchecked")
    NamingEnumeration<SearchResult> adminGroups = createMock(NamingEnumeration.class);
    expect(adminGroups.hasMore())
        .andReturn(false)
        .atLeastOnce();
    adminGroups.close();
    expectLastCall().atLeastOnce();

    DirContextOperations boundUserContext = createMock(DirContextOperations.class);
    System.out.println(ldapUserDNString);
    expect(boundUserContext.search(eq("ou=groups"), eq("(&(member=" + ldapUserDNString + ")(objectclass=group)(|(cn=Ambari Administrators)))"), anyObject(SearchControls.class)))
        .andReturn(adminGroups)
        .atLeastOnce();
    boundUserContext.close();
    expectLastCall().atLeastOnce();
    
    LdapContextSource ldapCtxSource = createMock(LdapContextSource.class);
    expect(ldapCtxSource.getBaseLdapName())
        .andReturn(basePath)
        .atLeastOnce();
    expect(ldapCtxSource.getContext(ldapUserDNString, "password"))
        .andReturn(boundUserContext)
        .once();
    expect(ldapCtxSource.getReadOnlyContext())
        .andReturn(boundUserContext)
        .once();

    Attributes searchedAttributes = new BasicAttributes("uid", ldapUsername);

    DirContextOperations searchedUserContext = createMock(DirContextOperations.class);
    expect(searchedUserContext.getDn())
        .andReturn(ldapUserRelativeDN)
        .atLeastOnce();
    expect(searchedUserContext.getAttributes())
        .andReturn(searchedAttributes)
        .atLeastOnce();

    FilterBasedLdapUserSearch userSearch = createMock(FilterBasedLdapUserSearch.class);
    expect(userSearch.searchForUser(ambariUsername)).andReturn(searchedUserContext).once();

    ServletRequestAttributes servletRequestAttributes = createMock(ServletRequestAttributes.class);

    if (!StringUtils.isEmpty(ldapUsername) && !ambariUsername.equals(ldapUsername)) {
      servletRequestAttributes.setAttribute(eq(ambariUsername), eq(forceUsernameToLower ? ldapUsername.toLowerCase() : ldapUsername), eq(RequestAttributes.SCOPE_SESSION));
      expectLastCall().once();
    }

    setupDatabaseConfigurationExpectations(true, forceUsernameToLower);

    replayAll();

    RequestContextHolder.setRequestAttributes(servletRequestAttributes);

    AmbariLdapBindAuthenticator bindAuthenticator = new AmbariLdapBindAuthenticator(ldapCtxSource, ldapConfiguration);
    bindAuthenticator.setUserSearch(userSearch);
    DirContextOperations user = bindAuthenticator.authenticate(new UsernamePasswordAuthenticationToken(ambariUsername, "password"));

    verifyAll();

    String ldapUserNameAttribute = ldapConfiguration.getLdapServerProperties().getUsernameAttribute();
    assertEquals(ldapUsername, user.getStringAttribute(ldapUserNameAttribute));
  }
  
  private Injector createInjector() throws Exception {
    return Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        bind(AmbariLdapConfiguration.class).toInstance(createNiceMock(AmbariLdapConfiguration.class));
      }
    });
  }
  
  private void setupDatabaseConfigurationExpectations(boolean expectedDatabaseConfigCall, boolean forceUsernameToLowerCase) {
    final LdapServerProperties ldapServerProperties = getDefaultLdapServerProperties(forceUsernameToLowerCase);
    ldapServerProperties.setGroupObjectClass("group");
    if (expectedDatabaseConfigCall) {
      expect(ldapConfiguration.getLdapServerProperties()).andReturn(ldapServerProperties).anyTimes();
    }
  }
  
  private static LdapServerProperties getDefaultLdapServerProperties(boolean forceUsernameToLowerCase) {
    final LdapServerProperties ldapServerProperties = new LdapServerProperties();
    ldapServerProperties.setPrimaryUrl(AmbariServerConfigurationKey.SERVER_HOST.getDefaultValue() + ":" + AmbariServerConfigurationKey.SERVER_PORT.getDefaultValue());
    ldapServerProperties.setSecondaryUrl(AmbariServerConfigurationKey.SECONDARY_SERVER_HOST.getDefaultValue() + ":" + AmbariServerConfigurationKey.SECONDARY_SERVER_PORT.getDefaultValue());
    ldapServerProperties.setUseSsl(parseBoolean(AmbariServerConfigurationKey.USE_SSL.getDefaultValue()));
    ldapServerProperties.setAnonymousBind(parseBoolean(AmbariServerConfigurationKey.ANONYMOUS_BIND.getDefaultValue()));
    ldapServerProperties.setManagerDn(AmbariServerConfigurationKey.BIND_DN.getDefaultValue());
    ldapServerProperties.setManagerPassword(AmbariServerConfigurationKey.BIND_PASSWORD.getDefaultValue());
    ldapServerProperties.setBaseDN(AmbariServerConfigurationKey.USER_SEARCH_BASE.getDefaultValue());
    ldapServerProperties.setUsernameAttribute(AmbariServerConfigurationKey.USER_NAME_ATTRIBUTE.getDefaultValue());
    ldapServerProperties.setForceUsernameToLowercase(forceUsernameToLowerCase);
    ldapServerProperties.setUserBase(AmbariServerConfigurationKey.USER_BASE.getDefaultValue());
    ldapServerProperties.setUserObjectClass(AmbariServerConfigurationKey.USER_OBJECT_CLASS.getDefaultValue());
    ldapServerProperties.setDnAttribute(AmbariServerConfigurationKey.DN_ATTRIBUTE.getDefaultValue());
    ldapServerProperties.setGroupBase(AmbariServerConfigurationKey.GROUP_BASE.getDefaultValue());
    ldapServerProperties.setGroupObjectClass(AmbariServerConfigurationKey.GROUP_OBJECT_CLASS.getDefaultValue());
    ldapServerProperties.setGroupMembershipAttr(AmbariServerConfigurationKey.GROUP_MEMBER_ATTRIBUTE.getDefaultValue());
    ldapServerProperties.setGroupNamingAttr(AmbariServerConfigurationKey.GROUP_NAME_ATTRIBUTE.getDefaultValue());
    ldapServerProperties.setAdminGroupMappingRules(AmbariServerConfigurationKey.GROUP_MAPPING_RULES.getDefaultValue());
    ldapServerProperties.setAdminGroupMappingMemberAttr("");
    ldapServerProperties.setUserSearchFilter(AmbariServerConfigurationKey.USER_SEARCH_FILTER.getDefaultValue());
    ldapServerProperties.setAlternateUserSearchFilter(AmbariServerConfigurationKey.ALTERNATE_USER_SEARCH_FILTER.getDefaultValue());
    ldapServerProperties.setGroupSearchFilter(AmbariServerConfigurationKey.GROUP_SEARCH_FILTER.getDefaultValue());
    ldapServerProperties.setReferralMethod(AmbariServerConfigurationKey.REFERRAL_HANDLING.getDefaultValue());
    ldapServerProperties.setSyncUserMemberReplacePattern(AmbariServerConfigurationKey.USER_MEMBER_REPLACE_PATTERN.getDefaultValue());
    ldapServerProperties.setSyncGroupMemberReplacePattern(AmbariServerConfigurationKey.GROUP_MEMBER_REPLACE_PATTERN.getDefaultValue());
    ldapServerProperties.setSyncUserMemberFilter(AmbariServerConfigurationKey.USER_MEMBER_FILTER.getDefaultValue());
    ldapServerProperties.setSyncGroupMemberFilter(AmbariServerConfigurationKey.GROUP_MEMBER_FILTER.getDefaultValue());
    ldapServerProperties.setPaginationEnabled(parseBoolean(AmbariServerConfigurationKey.PAGINATION_ENABLED.getDefaultValue()));
    return ldapServerProperties;
  }
}
