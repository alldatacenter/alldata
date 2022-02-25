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

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.audit.event.LoginAuditEvent;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.PermissionHelper;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.utils.RequestUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * AmbariAuthenticationEventHandlerImpl is the default {@link AmbariAuthenticationEventHandler}
 * implementation.
 * <p>
 * This implementation tracks authentication attempts using the Ambari {@link AuditLogger} and
 * ensures that the relevant user's consecutive authentication failure count is properly tracked.
 * <p>
 * Upon an authentication failure, the user's consecutive authentication failure count is incremented
 * by <code>1</code> and upon a successful authentication, the user's consecutive authentication failure count
 * is reset to <code>0</code>.
 */
@Singleton
public class AmbariAuthenticationEventHandlerImpl implements AmbariAuthenticationEventHandler {
  private static final Logger LOG = LoggerFactory.getLogger(AmbariAuthenticationEventHandlerImpl.class);
  /**
   * Audit logger
   */
  @Inject
  private AuditLogger auditLogger;

  /**
   * PermissionHelper to help create audit entries
   */
  @Inject
  private PermissionHelper permissionHelper;

  @Inject
  private Users users;

  @Override
  public void onSuccessfulAuthentication(AmbariAuthenticationFilter filter, HttpServletRequest servletRequest, HttpServletResponse servletResponse, Authentication result) {
    String username = (result == null) ? null : result.getName();

    // Using the Ambari audit logger, log this event (if enabled)
    if (auditLogger.isEnabled()) {
      AuditEvent loginSucceededAuditEvent = LoginAuditEvent.builder()
          .withRemoteIp(RequestUtils.getRemoteAddress(servletRequest))
          .withUserName(username)
          .withProxyUserName(AuthorizationHelper.getProxyUserName(result))
          .withTimestamp(System.currentTimeMillis())
          .withRoles(permissionHelper.getPermissionLabels(result))
          .build();
      auditLogger.log(loginSucceededAuditEvent);
    }

    // Reset the user's consecutive authentication failure count to 0.
    if (!StringUtils.isEmpty(username)) {
      LOG.debug("Successfully authenticated {}", username);
      users.clearConsecutiveAuthenticationFailures(username);
    } else {
      LOG.warn("Successfully authenticated an unknown user");
    }
  }

  @Override
  public void onUnsuccessfulAuthentication(AmbariAuthenticationFilter filter, HttpServletRequest servletRequest, HttpServletResponse servletResponse, AmbariAuthenticationException cause) {
    String username;
    String message;
    String logMessage;
    Integer consecutiveFailures = null;
    boolean incrementFailureCount;

    if (cause == null) {
      username = null;
      message = "Unknown cause";
      incrementFailureCount = false;
    } else {
      username = cause.getUsername();
      message = cause.getLocalizedMessage();
      incrementFailureCount = cause.isCredentialFailure();
    }

    if (!StringUtils.isEmpty(username)) {
      // Only increment the authentication failure count if the authentication filter declares to
      // do so.
      if(incrementFailureCount && filter.shouldIncrementFailureCount()) {
        // Increment the user's consecutive authentication failure count.
        consecutiveFailures = users.incrementConsecutiveAuthenticationFailures(username);

        // If consecutiveFailures is NULL, then no user entry was found for the specified username.
        if (consecutiveFailures == null) {
          logMessage = String.format("Failed to authenticate %s: The user does not exist in the Ambari database", username);
        } else {
          logMessage = String.format("Failed to authenticate %s (attempt #%d): %s", username, consecutiveFailures, message);
        }
      }
      else {
        logMessage = String.format("Failed to authenticate %s: %s", username, message);
      }
    } else {
      logMessage = String.format("Failed to authenticate an unknown user: %s", message);
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug(logMessage, cause);
    } else {
      LOG.info(logMessage);
    }

    // Using the Ambari audit logger, log this event (if enabled)
    if (auditLogger.isEnabled()) {
      AuditEvent loginFailedAuditEvent = LoginAuditEvent.builder()
          .withRemoteIp(RequestUtils.getRemoteAddress(servletRequest))
          .withTimestamp(System.currentTimeMillis())
          .withReasonOfFailure(message)
          .withConsecutiveFailures(consecutiveFailures)
          .withUserName(username)
          .withProxyUserName(null)
          .build();
      auditLogger.log(loginFailedAuditEvent);
    }
  }

  @Override
  public void beforeAttemptAuthentication(AmbariAuthenticationFilter filter, ServletRequest servletRequest, ServletResponse servletResponse) {
    HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

    // Using the Ambari audit logger, log this event (if enabled)
    if (auditLogger.isEnabled() && filter.shouldApply(httpServletRequest) && (AuthorizationHelper.getAuthenticatedName() == null)) {
      AuditEvent loginFailedAuditEvent = LoginAuditEvent.builder()
          .withRemoteIp(RequestUtils.getRemoteAddress(httpServletRequest))
          .withTimestamp(System.currentTimeMillis())
          .withReasonOfFailure("Authentication required")
          .withUserName(null)
          .withProxyUserName(null)
          .build();
      auditLogger.log(loginFailedAuditEvent);
    }

  }
}
