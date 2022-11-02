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

import java.util.Collection;
import java.util.List;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.ldap.service.AmbariLdapConfigurationProvider;
import org.apache.ambari.server.orm.entities.UserAuthenticationEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.ClientSecurityType;
import org.apache.ambari.server.security.authentication.AccountDisabledException;
import org.apache.ambari.server.security.authentication.AmbariAuthenticationProvider;
import org.apache.ambari.server.security.authentication.AmbariUserAuthentication;
import org.apache.ambari.server.security.authentication.AmbariUserDetails;
import org.apache.ambari.server.security.authentication.AmbariUserDetailsImpl;
import org.apache.ambari.server.security.authentication.InvalidUsernamePasswordCombinationException;
import org.apache.ambari.server.security.authentication.TooManyLoginFailuresException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.LdapUserDetails;

import com.google.inject.Inject;


/**
 * Provides LDAP user authorization logic for Ambari Server
 */
public class AmbariLdapAuthenticationProvider extends AmbariAuthenticationProvider {
  private static final String SYSTEM_PROPERTY_DISABLE_ENDPOINT_IDENTIFICATION = "com.sun.jndi.ldap.object.disableEndpointIdentification";
  private static Logger LOG = LoggerFactory.getLogger(AmbariLdapAuthenticationProvider.class);

  final AmbariLdapConfigurationProvider ldapConfigurationProvider;

  private AmbariLdapAuthoritiesPopulator authoritiesPopulator;

  private ThreadLocal<LdapServerProperties> ldapServerProperties = new ThreadLocal<>();
  private ThreadLocal<LdapAuthenticationProvider> providerThreadLocal = new ThreadLocal<>();
  private ThreadLocal<String> ldapUserSearchFilterThreadLocal = new ThreadLocal<>();

  @Inject
  public AmbariLdapAuthenticationProvider(Users users, Configuration configuration, AmbariLdapConfigurationProvider ldapConfigurationProvider,
                                          AmbariLdapAuthoritiesPopulator authoritiesPopulator) {
    super(users, configuration);
    this.ldapConfigurationProvider = ldapConfigurationProvider;
    this.authoritiesPopulator = authoritiesPopulator;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    if (isLdapEnabled()) {
      if (authentication.getName() == null) {
        LOG.info("Authentication failed: no username provided");
        throw new InvalidUsernamePasswordCombinationException("");
      }

      String username = authentication.getName().trim();

      if (authentication.getCredentials() == null) {
        LOG.info("Authentication failed: no credentials provided: {}", username);
        throw new InvalidUsernamePasswordCombinationException(username);
      }

      try {
        Authentication auth = loadLdapAuthenticationProvider(username).authenticate(authentication);
        UserEntity userEntity = getUserEntity(auth);

        if (userEntity == null) {
          // TODO: If we were automatically importing accounts from the LDAP server, we should
          // TODO: probably do it here.
          LOG.debug("user not found ('{}')", username);
          throw new InvalidUsernamePasswordCombinationException(username);
        } else {
          Users users = getUsers();

          // Ensure the user is allowed to login....
          try {
            users.validateLogin(userEntity, username);
          } catch (AccountDisabledException | TooManyLoginFailuresException e) {
            if (getConfiguration().showLockedOutUserMessage()) {
              throw e;
            } else {
              // Do not give away information about the existence or status of a user
              throw new InvalidUsernamePasswordCombinationException(username, false, e);
            }
          }

          AmbariUserDetails userDetails = new AmbariUserDetailsImpl(users.getUser(userEntity), null, users.getUserAuthorities(userEntity));
          return new AmbariUserAuthentication(null, userDetails, true);
        }
      } catch (AuthenticationException e) {
        LOG.debug("Got exception during LDAP authentication attempt", e);
        // Try to help in troubleshooting
        Throwable cause = e.getCause();
        if ((cause != null) && (cause != e)) {
          // Below we check the cause of an AuthenticationException to see what the actual cause is
          // and then send an appropriate message to the caller.
          if (cause instanceof org.springframework.ldap.CommunicationException) {
            if (LOG.isDebugEnabled()) {
              LOG.warn("Failed to communicate with the LDAP server: " + cause.getMessage(), e);
            } else {
              LOG.warn("Failed to communicate with the LDAP server: " + cause.getMessage());
            }
          } else if (cause instanceof org.springframework.ldap.AuthenticationException) {
            LOG.warn("Looks like LDAP manager credentials (that are used for " +
                "connecting to LDAP server) are invalid.", e);
          }
        }
        throw new InvalidUsernamePasswordCombinationException(username, e);
      } catch (IncorrectResultSizeDataAccessException multipleUsersFound) {
        String message = ldapConfigurationProvider.get().isLdapAlternateUserSearchEnabled()
            ? String.format("Login Failed: Please append your domain to your username and try again.  Example: %s@domain", username)
            : "Login Failed: More than one user with that username found, please work with your Ambari Administrator to adjust your LDAP configuration";

        throw new DuplicateLdapUserFoundAuthenticationException(message);
      }
    } else {
      return null;
    }
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }

  /**
   * Reloads LDAP Context Source and depending objects if properties were changed
   *
   * @return corresponding LDAP authentication provider
   */
  LdapAuthenticationProvider loadLdapAuthenticationProvider(String userName) {
    boolean ldapConfigPropertiesChanged = reloadLdapServerProperties();

    String ldapUserSearchFilter = getLdapUserSearchFilter(userName);

    if (ldapConfigPropertiesChanged || !ldapUserSearchFilter.equals(ldapUserSearchFilterThreadLocal.get())) {

      LOG.info("Either LDAP Properties or user search filter changed - rebuilding Context");
      LdapContextSource springSecurityContextSource = new LdapContextSource();
      List<String> ldapUrls = ldapServerProperties.get().getLdapUrls();
      springSecurityContextSource.setUrls(ldapUrls.toArray(new String[ldapUrls.size()]));
      springSecurityContextSource.setBase(ldapServerProperties.get().getBaseDN());

      if (!ldapServerProperties.get().isAnonymousBind()) {
        springSecurityContextSource.setUserDn(ldapServerProperties.get().getManagerDn());
        springSecurityContextSource.setPassword(ldapServerProperties.get().getManagerPassword());
      }

      if (ldapServerProperties.get().isUseSsl() && ldapServerProperties.get().isDisableEndpointIdentification()) {
        System.setProperty(SYSTEM_PROPERTY_DISABLE_ENDPOINT_IDENTIFICATION, "true");
        LOG.info("Disabled endpoint identification");
      } else {
        System.clearProperty(SYSTEM_PROPERTY_DISABLE_ENDPOINT_IDENTIFICATION);
        LOG.info("Removed endpoint identification disabling");
      }

      try {
        springSecurityContextSource.afterPropertiesSet();
      } catch (Exception e) {
        LOG.error("LDAP Context Source not loaded ", e);
        throw new UsernameNotFoundException("LDAP Context Source not loaded", e);
      }

      //TODO change properties
      String userSearchBase = ldapServerProperties.get().getUserSearchBase();
      FilterBasedLdapUserSearch userSearch = new FilterBasedLdapUserSearch(userSearchBase, ldapUserSearchFilter, springSecurityContextSource);

      AmbariLdapBindAuthenticator bindAuthenticator = new AmbariLdapBindAuthenticator(springSecurityContextSource, ldapConfigurationProvider.get());
      bindAuthenticator.setUserSearch(userSearch);

      LdapAuthenticationProvider authenticationProvider = new LdapAuthenticationProvider(bindAuthenticator, authoritiesPopulator);
      providerThreadLocal.set(authenticationProvider);
    }

    ldapUserSearchFilterThreadLocal.set(ldapUserSearchFilter);

    return providerThreadLocal.get();
  }


  /**
   * Check if LDAP authentication is enabled in server properties
   *
   * @return true if enabled
   */
  boolean isLdapEnabled() {
    return getConfiguration().getClientSecurityType() == ClientSecurityType.LDAP;
  }

  /**
   * Reloads LDAP Server properties from configuration
   *
   * @return true if properties were reloaded
   */
  private boolean reloadLdapServerProperties() {
    LdapServerProperties properties = ldapConfigurationProvider.get().getLdapServerProperties();
    if (!properties.equals(ldapServerProperties.get())) {
      LOG.info("Reloading properties");
      ldapServerProperties.set(properties);
      return true;
    }
    return false;
  }


  private String getLdapUserSearchFilter(String userName) {
    return ldapServerProperties.get()
        .getUserSearchFilter(ldapConfigurationProvider.get().isLdapAlternateUserSearchEnabled() && AmbariLdapUtils.isUserPrincipalNameFormat(userName));
  }

  /**
   * Gets the {@link UserEntity} related to the authentication information
   * <p>
   * First the DN is retrieved from the user authentication information and a {@link UserAuthenticationEntity}
   * is queried for where the type value is LDAP and key value case-insensitively matches the DN.
   * If a record is found, the related {@link UserEntity} is returned.
   * <p>
   * Else, a {@link UserEntity} with the user name is queried. If one is found and it has a
   * {@link UserAuthenticationEntity} where the type value is LDAP and key is empty, the related
   * {@link UserEntity} is returned
   * <p>
   * Else, <code>null</code> is returned.
   *
   * @param authentication the user's authentication data
   * @return a {@link UserEntity}
   */
  private UserEntity getUserEntity(Authentication authentication) {
    UserEntity userEntity = null;

    // Find user with the matching DN
    String dn = getUserDN(authentication);
    if (!StringUtils.isEmpty(dn)) {
      userEntity = getUserEntityForDN(dn);
    }

    // If a user was not found with the exact authentication properties (LDAP/dn), look up the user
    // using the configured LDAP username attribute and ensure that user has an empty-keyed LDAP
    // authentication entity record.
    if (userEntity == null) {
      String userName = AuthorizationHelper.resolveLoginAliasToUserName(authentication.getName());
      userEntity = getUsers().getUserEntity(userName);

      if (userEntity != null) {
        Collection<UserAuthenticationEntity> authenticationEntities = getAuthenticationEntities(userEntity, UserAuthenticationType.LDAP);
        UserEntity _userEntity = userEntity; // Hold on to the user entity value for now.
        userEntity = null;  // Guilty until proven innocent

        if (!CollectionUtils.isEmpty(authenticationEntities)) {
          for (UserAuthenticationEntity entity : authenticationEntities) {
            if (StringUtils.isEmpty(entity.getAuthenticationKey())) {
              // Proven innocent!
              userEntity = _userEntity;
              break;
            }
          }
        }
      }
    }

    return userEntity;
  }

  /**
   * Given a DN from the LDAP server, find the owning UserEntity.
   * <p>
   * DNs are case sensitive. Internally they are execpted to be stored as the bytes of the lowercase
   * string.
   * <p>
   * DN's are expected to be unique across all {@link UserAuthenticationEntity} records for type
   * UserAuthenticationType.LDAP.
   *
   * @param dn the DN to search for
   * @return a {@link UserEntity}, if found
   */
  private UserEntity getUserEntityForDN(String dn) {
    Collection<UserAuthenticationEntity> authenticationEntities = getAuthenticationEntities(UserAuthenticationType.LDAP, StringUtils.lowerCase(dn));
    return ((authenticationEntities == null) || (authenticationEntities.size() != 1))
        ? null
        : authenticationEntities.iterator().next().getUser();
  }

  /**
   * Given the authentication object, attempt to retrieve the user's DN value from it.
   *
   * @param authentication the authentication data
   * @return the relative DN; else <code>null</code> if not available
   */
  private String getUserDN(Authentication authentication) {
    Object objectPrincipal = (authentication == null) ? null : authentication.getPrincipal();
    if (objectPrincipal instanceof LdapUserDetails) {
      return ((LdapUserDetails) objectPrincipal).getDn();
    }

    return null;
  }
}
