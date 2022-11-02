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

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.entities.UserAuthenticationEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.authorization.UserAuthenticationType;
import org.apache.ambari.server.security.authorization.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.inject.Inject;

/**
 * AmbariLocalAuthenticationProvider is an {@link org.springframework.security.authentication.AuthenticationProvider}
 * implementation used to authenticate users using username and password details from the local Ambari database.
 * <p>
 * Users will fail to authenticate, even if they supply the correct credentials if the account is locked out
 * by being disabled or locked due to too many consecutive failure.
 */
public class AmbariLocalAuthenticationProvider extends AmbariAuthenticationProvider {
  private static final Logger LOG = LoggerFactory.getLogger(AmbariLocalAuthenticationProvider.class);

  private PasswordEncoder passwordEncoder;

  @Inject
  public AmbariLocalAuthenticationProvider(Users users, PasswordEncoder passwordEncoder, Configuration configuration) {
    super(users, configuration);
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    if (authentication.getName() == null) {
      LOG.info("Authentication failed: no username provided");
      throw new InvalidUsernamePasswordCombinationException("");
    }

    String userName = authentication.getName().trim();

    if (authentication.getCredentials() == null) {
      LOG.info("Authentication failed: no credentials provided: {}", userName);
      throw new InvalidUsernamePasswordCombinationException(userName);
    }

    Users users = getUsers();

    UserEntity userEntity = users.getUserEntity(userName);

    if (userEntity == null) {
      LOG.info("User not found: {}", userName);
      throw new InvalidUsernamePasswordCombinationException(userName);
    }

    UserAuthenticationEntity authenticationEntity = getAuthenticationEntity(userEntity, UserAuthenticationType.LOCAL);
    if (authenticationEntity != null) {
      String password = authenticationEntity.getAuthenticationKey();
      String presentedPassword = authentication.getCredentials().toString();

      if (passwordEncoder.matches(presentedPassword, password)) {
        // The user was  authenticated, return the authenticated user object
        LOG.debug("Authentication succeeded - a matching username and password were found: {}", userName);

        try {
          users.validateLogin(userEntity, userName);
        }
        catch (AccountDisabledException | TooManyLoginFailuresException e) {
          if (getConfiguration().showLockedOutUserMessage()) {
            throw e;
          } else {
            // Do not give away information about the existence or status of a user
            throw new InvalidUsernamePasswordCombinationException(userName, false, e);
          }
        }

        AmbariUserDetails userDetails = new AmbariUserDetailsImpl(users.getUser(userEntity), password, users.getUserAuthorities(userEntity));
        return new AmbariUserAuthentication(password, userDetails, true);
      }
    }

    // The user was not authenticated, fail
    LOG.debug("Authentication failed: password does not match stored value: {}", userName);
    throw new InvalidUsernamePasswordCombinationException(userName);
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }
}
