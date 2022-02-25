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

package org.apache.ambari.server.security.authentication.kerberos;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.entities.UserAuthenticationEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.authentication.AccountDisabledException;
import org.apache.ambari.server.security.authentication.AmbariAuthenticationException;
import org.apache.ambari.server.security.authentication.AmbariUserDetailsImpl;
import org.apache.ambari.server.security.authentication.InvalidUsernamePasswordCombinationException;
import org.apache.ambari.server.security.authentication.TooManyLoginFailuresException;
import org.apache.ambari.server.security.authentication.UserNotFoundException;
import org.apache.ambari.server.security.authorization.User;
import org.apache.ambari.server.security.authorization.UserAuthenticationType;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.security.authentication.util.KerberosName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * AmbariAuthToLocalUserDetailsService is a {@link UserDetailsService} that translates
 * a Kerberos principal name into a local username that may be used when looking up
 * and Ambari user account.
 */
@Component
public class AmbariAuthToLocalUserDetailsService implements UserDetailsService {
  private static final Logger LOG = LoggerFactory.getLogger(AmbariAuthToLocalUserDetailsService.class);

  private final Configuration configuration;

  private final Users users;

  private final String authToLocalRules;

  /**
   * Constructor.
   * <p>
   * Given the Ambari {@link Configuration}, initializes the {@link KerberosName} class using
   * the <code>auth-to-local</code> rules from {@link AmbariKerberosAuthenticationProperties#getAuthToLocalRules()}.
   *
   * @param configuration the Ambari configuration data
   * @param users         the Ambari users access object
   */
  AmbariAuthToLocalUserDetailsService(Configuration configuration, Users users) {
    AmbariKerberosAuthenticationProperties properties = configuration.getKerberosAuthenticationProperties();
    String authToLocalRules = properties.getAuthToLocalRules();

    if (StringUtils.isEmpty(authToLocalRules)) {
      authToLocalRules = "DEFAULT";
    }

    this.configuration = configuration;
    this.users = users;
    this.authToLocalRules = authToLocalRules;
  }

  @Override
  public UserDetails loadUserByUsername(String principal) throws UsernameNotFoundException {
    String username;

    // First see if there is a Kerberos-related authentication record for some user.
    Collection<UserAuthenticationEntity> entities = users.getUserAuthenticationEntities(UserAuthenticationType.KERBEROS, principal);

    // Zero or one value is expected.. if not, that is an issue.
    // If no entries are returned, we have not yet seen this principal.  If no, perform an auth-to-local translation
    // to determine what the local username is.
    if (CollectionUtils.isEmpty(entities)) {
      username = translatePrincipalName(principal);
      if (username == null) {
        String message = String.format("Failed to translate %s to a local username during Kerberos authentication.", principal);
        LOG.warn(message);
        throw new UsernameNotFoundException(message);
      }

      LOG.info("Translated {} to {} using auth-to-local rules during Kerberos authentication.", principal, username);
      return createUser(username, principal);
    } else if (entities.size() == 1) {
      UserEntity userEntity = entities.iterator().next().getUser();
      LOG.trace("Found KERBEROS authentication method for {} using principal {}", userEntity.getUserName(), principal);
      return createUserDetails(userEntity);
    } else {
      throw new AmbariAuthenticationException("", "Unexpected error due to collisions on the principal name", false);
    }
  }

  /**
   * Given a username, finds an appropriate account in the Ambari database.
   *
   * @param username  a username
   * @param principal the user's principal
   * @return the user details of the found user, or <code>null</code> if an appropriate user was not found
   */
  private UserDetails createUser(String username, String principal) throws AuthenticationException {
    UserEntity userEntity = users.getUserEntity(username);

    if (userEntity == null) {
      LOG.info("User not found: {} (from {})", username, principal);
      throw new UserNotFoundException(username, String.format("Cannot find user using Kerberos ticket (%s).", principal));
    } else {
      // Check to see if the user is allowed to authenticate using KERBEROS or LDAP
      List<UserAuthenticationEntity> authenticationEntities = userEntity.getAuthenticationEntities();
      boolean hasKerberos = false;

      for (UserAuthenticationEntity entity : authenticationEntities) {
        UserAuthenticationType authenticationType = entity.getAuthenticationType();

        switch (authenticationType) {
          case KERBEROS:
            String key = entity.getAuthenticationKey();
            if (StringUtils.isEmpty(key) || key.equals(username)) {
              LOG.trace("Found KERBEROS authentication method for {} where no principal was set. Fixing...", username);
              // Fix this entry so that it contains the relevant principal..
              try {
                users.addKerberosAuthentication(userEntity, principal);
                users.removeAuthentication(userEntity, entity.getUserAuthenticationId());
              } catch (AmbariException e) {
                // This should not lead to an error... if so, log it and ignore.
                LOG.warn(String.format("Failed to create KERBEROS authentication method entry for %s with principal %s: %s", username, principal, e.getLocalizedMessage()), e);
              }
              hasKerberos = true;
            } else if (principal.equalsIgnoreCase(entity.getAuthenticationKey())) {
              LOG.trace("Found KERBEROS authentication method for {} using principal {}", username, principal);
              hasKerberos = true;
            }
            break;
        }

        if (hasKerberos) {
          break;
        }
      }

      // TODO: Determine if KERBEROS users can be automatically added
      if (!hasKerberos) {
        try {
          users.addKerberosAuthentication(userEntity, principal);
          LOG.trace("Added KERBEROS authentication method for {} using principal {}", username, principal);
        } catch (AmbariException e) {
          LOG.error(String.format("Failed to add the KERBEROS authentication method for %s: %s", principal, e.getLocalizedMessage()), e);
        }
      }
    }

    return createUserDetails(userEntity);
  }

  private UserDetails createUserDetails(UserEntity userEntity) {
    String username = userEntity.getUserName();

    // Ensure the user account is allowed to log in
    try {
      users.validateLogin(userEntity, username);
    } catch (AccountDisabledException | TooManyLoginFailuresException e) {
      if (configuration.showLockedOutUserMessage()) {
        throw e;
      } else {
        // Do not give away information about the existence or status of a user
        throw new InvalidUsernamePasswordCombinationException(username, false, e);
      }
    }

    return new AmbariUserDetailsImpl(new User(userEntity), null, users.getUserAuthorities(userEntity));
  }

  /**
   * Using the auth-to-local rules stored in <code>authentication.kerberos.auth_to_local.rules</code>
   * in the <code>ambari.properties</code> file, translate the supplied principal name to a local name.
   *
   * @param principalName the principal name to translate
   * @return a local username
   */
  public String translatePrincipalName(String principalName) {
    if (StringUtils.isNotEmpty(principalName) && principalName.contains("@")) {
      try {
        // Since KerberosName relies on a static variable to hold on to the auth-to-local rules,
        // attempt to protect access to the rule set by blocking other threads from changing the
        // rules out from under us during this operation.
        synchronized (KerberosName.class) {
          KerberosName.setRules(authToLocalRules);
          return new KerberosName(principalName).getShortName();
        }
      } catch (UserNotFoundException e) {
        throw new UsernameNotFoundException(e.getMessage(), e);
      } catch (IOException e) {
        String message = String.format("Failed to translate %s to a local username during Kerberos authentication: %s", principalName, e.getLocalizedMessage());
        LOG.warn(message);
        throw new UsernameNotFoundException(message, e);
      }
    } else {
      return principalName;
    }
  }
}
