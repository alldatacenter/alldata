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
package org.apache.drill.exec.server.rest.auth;

import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.rpc.security.AuthenticatorFactory;
import org.apache.drill.exec.rpc.security.plain.PlainFactory;
import org.apache.drill.exec.rpc.user.security.UserAuthenticationException;
import org.apache.drill.exec.rpc.user.security.UserAuthenticator;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.options.SystemOptionManager;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.UserIdentity;

import javax.security.auth.Subject;
import javax.servlet.ServletRequest;
import java.security.Principal;

/**
 * LoginService used when user authentication is enabled in Drillbit. It validates the user against the user
 * authenticator set in BOOT config.
 */
public class DrillRestLoginService implements LoginService {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DrillRestLoginService.class);

  private final DrillbitContext drillbitContext;

  private IdentityService identityService = new DefaultIdentityService();

  public DrillRestLoginService(final DrillbitContext drillbitContext) {
    this.drillbitContext = drillbitContext;
  }

  @Override
  public boolean validate(UserIdentity user) {
    // This is called for every request after authentication is complete to make sure the user is still valid.
    // Once a user is authenticated we assume that the user is still valid. This behavior is similar to ODBC/JDBC where
    // once a user is logged-in we don't recheck the credentials again in the same session.
    return true;
  }

  @Override
  public String getName() {
    return "DrillRestLoginService";
  }

  @Override
  public UserIdentity login(String username, Object credentials, ServletRequest request) {
    if (!(credentials instanceof String)) {
      return null;
    }

    try {
      // Authenticate WebUser locally using UserAuthenticator. If WebServer is started that guarantees the PLAIN
      // mechanism is configured and authenticator is also available
      final AuthenticatorFactory plainFactory = drillbitContext.getAuthProvider()
          .getAuthenticatorFactory(PlainFactory.SIMPLE_NAME);
      final UserAuthenticator userAuthenticator = ((PlainFactory) plainFactory).getAuthenticator();

      // Authenticate the user with configured Authenticator
      userAuthenticator.authenticate(username, credentials.toString());

      logger.info("WebUser {} logged in from {}:{}", username, request.getRemoteHost(), request.getRemotePort());

      final SystemOptionManager sysOptions = drillbitContext.getOptionManager();

      final boolean isAdmin = ImpersonationUtil.hasAdminPrivileges(username,
              ExecConstants.ADMIN_USERS_VALIDATOR.getAdminUsers(sysOptions),
              ExecConstants.ADMIN_USER_GROUPS_VALIDATOR.getAdminUserGroups(sysOptions));

      // Create the UserPrincipal corresponding to logged in user.
      final Principal userPrincipal = new DrillUserPrincipal(username, isAdmin);

      final Subject subject = new Subject();
      subject.getPrincipals().add(userPrincipal);
      subject.getPrivateCredentials().add(credentials);

      if (isAdmin) {
        subject.getPrincipals().addAll(DrillUserPrincipal.ADMIN_PRINCIPALS);
        return identityService.newUserIdentity(subject, userPrincipal, DrillUserPrincipal.ADMIN_USER_ROLES);
      } else {
        subject.getPrincipals().addAll(DrillUserPrincipal.NON_ADMIN_PRINCIPALS);
        return identityService.newUserIdentity(subject, userPrincipal, DrillUserPrincipal.NON_ADMIN_USER_ROLES);
      }
    } catch (final Exception e) {
      if (e instanceof UserAuthenticationException) {
        logger.debug("Authentication failed for WebUser '{}'", username, e);
      } else {
        logger.error("Unexpected failure occurred for WebUser {} during login.", username, e);
      }
      return null;
    }
  }

  @Override
  public IdentityService getIdentityService() {
    return identityService;
  }

  @Override
  public void setIdentityService(IdentityService identityService) {
    this.identityService = identityService;
  }

  /**
   * This gets called whenever a session is invalidated (because of user logout) or timed out.
   * @param user - logged in UserIdentity
   */
  @Override
  public void logout(UserIdentity user) {
    // no-op
    if (logger.isTraceEnabled()) {
      logger.trace("Web user {} logged out.", user.getUserPrincipal().getName());
    }
  }
}
