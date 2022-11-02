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


import java.util.List;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.authentication.AbstractLdapAuthenticator;
import org.springframework.security.ldap.search.LdapUserSearch;


/**
 * An authenticator which binds as a user and checks if user should get ambari
 * admin authorities according to LDAP group membership
 */
public class AmbariLdapBindAuthenticator extends AbstractLdapAuthenticator {
  private static final Logger LOG = LoggerFactory.getLogger(AmbariLdapBindAuthenticator.class);
  private static final String AMBARI_ADMIN_LDAP_ATTRIBUTE_KEY = "ambari_admin";

  private final AmbariLdapConfiguration ldapConfiguration;


  public AmbariLdapBindAuthenticator(BaseLdapPathContextSource contextSource, AmbariLdapConfiguration ldapConfiguration) {
    super(contextSource);
    this.ldapConfiguration = ldapConfiguration;;
  }

  @Override
  public DirContextOperations authenticate(Authentication authentication) {

    if (!(authentication instanceof UsernamePasswordAuthenticationToken)) {
      LOG.info("Unexpected authentication token type encountered ({}) - failing authentication.", authentication.getClass().getName());
      throw new BadCredentialsException("Unexpected authentication token type encountered.");
    }

    DirContextOperations user = authenticate((UsernamePasswordAuthenticationToken) authentication);

    LdapServerProperties ldapServerProperties = ldapConfiguration.getLdapServerProperties();
    if (StringUtils.isNotEmpty(ldapServerProperties.getAdminGroupMappingRules())) {
      setAmbariAdminAttr(user, ldapServerProperties);
    }

    // Users stored locally in ambari are matched against LDAP users by the ldap attribute configured to be used as user name.
    // (e.g. uid, sAMAccount -> ambari user name )
    String ldapUserName = user.getStringAttribute(ldapServerProperties.getUsernameAttribute());
    String loginName = authentication.getName(); // user login name the user has logged in

    if (ldapUserName == null) {
      LOG.warn("The user data does not contain a value for {}.", ldapServerProperties.getUsernameAttribute());
    } else if (ldapUserName.isEmpty()) {
      LOG.warn("The user data contains an empty value for {}.", ldapServerProperties.getUsernameAttribute());
    } else if (!ldapUserName.equals(loginName)) {
      // if authenticated user name is different from ldap user name than user has logged in
      // with a login name that is different (e.g. user principal name) from the ambari user name stored in
      // ambari db. In this case add the user login name  as login alias for ambari user name.
      LOG.info("User with {}='{}' logged in with login alias '{}'", ldapServerProperties.getUsernameAttribute(), ldapUserName, loginName);

      // If the ldap username needs to be processed (like converted to all lowercase characters),
      // process it before setting it in the session via AuthorizationHelper#addLoginNameAlias
      String processedLdapUserName;
      if (ldapServerProperties.isForceUsernameToLowercase()) {
        processedLdapUserName = ldapUserName.toLowerCase();
        LOG.info("Forcing ldap username to be lowercase characters: {} ==> {}", ldapUserName, processedLdapUserName);
      } else {
        processedLdapUserName = ldapUserName;
      }

      AuthorizationHelper.addLoginNameAlias(processedLdapUserName, loginName);
    }

    return user;
  }

  /**
   * Authenticates a user with a configured LDAP server using the user's username and password.
   * <p>
   * To authenticate a user:
   * <ol>
   * <li>
   * The LDAP server is queried for the relevant user object where the
   * supplied username matches the configured LDAP attribute that represents the user's username
   * <ul><li>Example: (&(uid=user1)(objectClass=posixAccount))</li></ul>
   * </li>
   * <li>
   * If found, the distinguished name (DN) of the user object is obtained from returned data and then
   * used, along with the supplied password to perform an LDAP bind (see {@link #bind(DirContextOperations, String)})
   * </li>
   * </ol>
   * <p>
   * Failure to authenticate will result in a {@link BadCredentialsException} to be thrown.
   *
   * @param authentication the credentials to use for authentication
   * @return the authenticated user details
   * @see #bind(DirContextOperations, String)
   */
  private DirContextOperations authenticate(UsernamePasswordAuthenticationToken authentication) {
    DirContextOperations user = null;

    String username = authentication.getName();
    Object credentials = authentication.getCredentials();
    String password = (credentials instanceof String) ? (String) credentials : null;

    if (StringUtils.isEmpty(username)) {
      LOG.debug("Empty username encountered - failing authentication.");
      throw new BadCredentialsException("Empty username encountered.");
    }

    LOG.debug("Authenticating {}", username);

    if (StringUtils.isEmpty(password)) {
      LOG.debug("Empty password encountered - failing authentication.");
      throw new BadCredentialsException("Empty password encountered.");
    }

    LdapUserSearch userSearch = getUserSearch();
    if (userSearch == null) {
      LOG.debug("The user search facility has not been set - failing authentication.");
      throw new BadCredentialsException("The user search facility has not been set.");
    } else {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Searching for user with username {}: {}", username, userSearch);
      }

      // Find the user data where the supplied username matches the value of the configured LDAP
      // attribute for the user's username. If a user is found, use the DN fro the returned data
      // and the supplied password to attempt authentication.
      DirContextOperations userFromSearch = userSearch.searchForUser(username);

      if (userFromSearch == null) {
        LOG.debug("LDAP user object not found for {}", username);
      } else {
        LOG.debug("Found LDAP user for {}: {}", username, userFromSearch.getDn());
        user = bind(userFromSearch, password);

        // If trace enabled, log the user's LDAP attributes.
        if (LOG.isTraceEnabled()) {
          Attributes attributes = user.getAttributes();
          if (attributes != null) {
            StringBuilder builder = new StringBuilder();
            NamingEnumeration<String> ids = attributes.getIDs();
            try {
              while (ids.hasMore()) {
                String id = ids.next();
                builder.append("\n\t");
                builder.append(attributes.get(id));
              }
            } catch (NamingException e) {
              // Ignore this...
            }
            LOG.trace("User Attributes: {}", builder);
          } else {
            LOG.trace("User Attributes: not available");
          }
        }
      }
    }

    // If a user was not authenticated, thrown a BadCredentialsException, else return the user data
    if (user == null) {
      LOG.debug("Invalid credentials for {} - failing authentication.", username);
      throw new BadCredentialsException("Invalid credentials.");
    } else {
      LOG.debug("Successfully authenticated {}", username);
    }

    return user;
  }

  /**
   * Attempt to authenticate a user with the configured LDAP server by performing an LDAP bind.
   * <p>
   * Using the distinguished name provided in the supplied user data and the supplied password,
   * attempt to authenticate with the configured LDAP server. If authentication is successful, use the
   * attributes from the supplied user data rather than the attributes associated with the bound context
   * because some scenarios result in missing data within the bound context due to LDAP server implementations.
   * <p>
   * If authentication is not successful, throw a {@link BadCredentialsException}.
   *
   * @param user     the user data containing the relevant DN and associated attributes
   * @param password the password
   * @return the authenticated user details
   * @throws BadCredentialsException if authentication fails
   */
  private DirContextOperations bind(DirContextOperations user, String password) {
    ContextSource contextSource = getContextSource();

    if (contextSource == null) {
      String message = "Missing ContextSource - failing authentication.";
      LOG.debug(message);
      throw new InternalAuthenticationServiceException(message);
    }

    if (!(contextSource instanceof BaseLdapPathContextSource)) {
      String message = String.format("Unexpected ContextSource type (%s) - failing authentication.", contextSource.getClass().getName());
      LOG.debug(message);
      throw new InternalAuthenticationServiceException(message);
    }

    BaseLdapPathContextSource baseLdapPathContextSource = (BaseLdapPathContextSource) contextSource;
    Name userDistinguishedName = user.getDn();
    Name fullDn = AmbariLdapUtils.getFullDn(userDistinguishedName, baseLdapPathContextSource.getBaseLdapName());

    LOG.debug("Attempting to bind as {}", fullDn);

    DirContext dirContext = null;

    try {
      // Perform the authentication.  The result is not used because it is expected that the supplied
      // user data has all of the attributes for the authenticated user. If authentication fails, it
      // expected that the supplied user data will be destroyed or orphaned.
      dirContext = baseLdapPathContextSource.getContext(fullDn.toString(), password);

      // Build a new DirContextAdapter using the attributes from the passed in user details since it
      // is expected these details will be more complete of querying for them from the bound context.
      // Some LDAP server implementations will no return all attributes to the bound context due to
      // the filter being used in the query.
      return new DirContextAdapter(user.getAttributes(), userDistinguishedName, baseLdapPathContextSource.getBaseLdapName());
    } catch (org.springframework.ldap.AuthenticationException e) {
      String message = String.format("Failed to bind as %s - %s", user.getDn().toString(), e.getMessage());
      if (LOG.isTraceEnabled()) {
        LOG.trace(message, e);
      } else if (LOG.isDebugEnabled()) {
        LOG.debug(message);
      }
      throw new BadCredentialsException("The username or password is incorrect.");
    } finally {
      LdapUtils.closeContext(dirContext);
    }
  }

  /**
   * Checks weather user is a member of ambari administrators group in LDAP. If
   * yes, sets user's ambari_admin attribute to true
   *
   * @param user the user details
   * @return the updated user details
   */
  private DirContextOperations setAmbariAdminAttr(DirContextOperations user, LdapServerProperties ldapServerProperties) {
    String baseDn = ldapServerProperties.getBaseDN().toLowerCase();
    String groupBase = ldapServerProperties.getGroupBase().toLowerCase();
    final String groupNamingAttribute =
        ldapServerProperties.getGroupNamingAttr();
    final String adminGroupMappingMemberAttr = ldapServerProperties.getAdminGroupMappingMemberAttr();

    //If groupBase is set incorrectly or isn't set - search in BaseDn
    int indexOfBaseDn = groupBase.indexOf(baseDn);
    groupBase = indexOfBaseDn <= 0 ? "" : groupBase.substring(0, indexOfBaseDn - 1);

    String memberValue = StringUtils.isNotEmpty(adminGroupMappingMemberAttr)
        ? user.getStringAttribute(adminGroupMappingMemberAttr) : user.getNameInNamespace();
    LOG.debug("LDAP login - set '{}' as member attribute for adminGroupMappingRules", memberValue);

    String setAmbariAdminAttrFilter = resolveAmbariAdminAttrFilter(ldapServerProperties, memberValue);
    LOG.debug("LDAP login - set admin attr filter: {}", setAmbariAdminAttrFilter);

    AttributesMapper attributesMapper = attrs -> attrs.get(groupNamingAttribute).get();

    LdapTemplate ldapTemplate = new LdapTemplate((getContextSource()));
    ldapTemplate.setIgnorePartialResultException(true);
    ldapTemplate.setIgnoreNameNotFoundException(true);

    @SuppressWarnings("unchecked")
    List<String> ambariAdminGroups = ldapTemplate.search(groupBase, setAmbariAdminAttrFilter, attributesMapper);

    //user has admin role granted, if user is a member of at least 1 group,
    // which matches the rules in configuration
    if (ambariAdminGroups.size() > 0) {
      user.setAttributeValue(AMBARI_ADMIN_LDAP_ATTRIBUTE_KEY, true);
    }

    return user;
  }

  private String resolveAmbariAdminAttrFilter(LdapServerProperties ldapServerProperties, String memberValue) {
    String groupMembershipAttr = ldapServerProperties.getGroupMembershipAttr();
    String groupObjectClass = ldapServerProperties.getGroupObjectClass();
    String adminGroupMappingRules =
        ldapServerProperties.getAdminGroupMappingRules();
    final String groupNamingAttribute =
        ldapServerProperties.getGroupNamingAttr();
    String groupSearchFilter = ldapServerProperties.getGroupSearchFilter();

    String setAmbariAdminAttrFilter;
    if (StringUtils.isEmpty(groupSearchFilter)) {
      String adminGroupMappingRegex = createAdminGroupMappingRegex(adminGroupMappingRules, groupNamingAttribute);
      setAmbariAdminAttrFilter = String.format("(&(%s=%s)(objectclass=%s)(|%s))",
          groupMembershipAttr,
          memberValue,
          groupObjectClass,
          adminGroupMappingRegex);
    } else {
      setAmbariAdminAttrFilter = String.format("(&(%s=%s)%s)",
          groupMembershipAttr,
          memberValue,
          groupSearchFilter);
    }
    return setAmbariAdminAttrFilter;
  }

  private String createAdminGroupMappingRegex(String adminGroupMappingRules, String groupNamingAttribute) {
    String[] adminGroupMappingRegexs = adminGroupMappingRules.split(",");
    StringBuilder builder = new StringBuilder("");
    for (String adminGroupMappingRegex : adminGroupMappingRegexs) {
      builder.append(String.format("(%s=%s)", groupNamingAttribute, adminGroupMappingRegex));
    }
    return builder.toString();
  }

}
