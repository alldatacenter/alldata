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

import java.io.IOException;
import java.security.Principal;
import java.util.EnumSet;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.audit.event.AccessUnauthorizedAuditEvent;
import org.apache.ambari.server.audit.event.AuditEvent;
import org.apache.ambari.server.audit.event.LoginAuditEvent;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.security.AmbariEntryPoint;
import org.apache.ambari.server.security.authorization.internal.InternalAuthenticationToken;
import org.apache.ambari.server.utils.RequestUtils;
import org.apache.ambari.server.view.ViewRegistry;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AmbariAuthorizationFilter implements Filter {
  private static final String REALM_PARAM = "realm";
  private static final String DEFAULT_REALM = "AuthFilter";

  private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

  private static final Pattern STACK_ADVISOR_REGEX = Pattern.compile("/api/v[0-9]+/stacks/[^/]+/versions/[^/]+/(validations|recommendations).*");

  public static final String API_VERSION_PREFIX = "/api/v[0-9]+";
  public static final String VIEWS_CONTEXT_PATH_PREFIX = "/views/";

  private static final String VIEWS_CONTEXT_PATH_PATTERN = VIEWS_CONTEXT_PATH_PREFIX + "([^/]+)/([^/]+)/([^/]+)(.*)";
  private static final String VIEWS_CONTEXT_ALL_PATTERN = VIEWS_CONTEXT_PATH_PREFIX + ".*";
  private static final String API_USERS_ALL_PATTERN = API_VERSION_PREFIX + "/users.*";
  private static final String API_PRIVILEGES_ALL_PATTERN = API_VERSION_PREFIX + "/privileges.*";
  private static final String API_GROUPS_ALL_PATTERN = API_VERSION_PREFIX + "/groups.*";
  private static final String API_CLUSTERS_PATTERN = API_VERSION_PREFIX + "/clusters/(\\w+/?)?";
  private static final String API_WIDGET_LAYOUTS_PATTERN = API_VERSION_PREFIX + "/clusters/.*?/widget_layouts.*?";
  private static final String API_WIDGET_PATTERN = API_VERSION_PREFIX + "/clusters/.*?/widgets.*";
  private static final String API_CLUSTERS_ALL_PATTERN = API_VERSION_PREFIX + "/clusters.*";
  private static final String API_VIEWS_ALL_PATTERN = API_VERSION_PREFIX + "/views.*";
  private static final String API_PERSIST_ALL_PATTERN = API_VERSION_PREFIX + "/persist.*";
  private static final String API_LDAP_SYNC_EVENTS_ALL_PATTERN = API_VERSION_PREFIX + "/ldap_sync_events.*";
  private static final String API_CREDENTIALS_ALL_PATTERN = API_VERSION_PREFIX + "/clusters/.*?/credentials.*";
  private static final String API_CREDENTIALS_AMBARI_PATTERN = API_VERSION_PREFIX + "/clusters/.*?/credentials/ambari\\..*";
  private static final String API_CLUSTER_REQUESTS_ALL_PATTERN = API_VERSION_PREFIX + "/clusters/.*?/requests.*";
  private static final String API_CLUSTER_SERVICES_ALL_PATTERN = API_VERSION_PREFIX + "/clusters/.*?/services.*";
  private static final String API_CLUSTER_ALERT_ALL_PATTERN = API_VERSION_PREFIX + "/clusters/.*?/alert.*";
  private static final String API_CLUSTER_HOSTS_ALL_PATTERN = API_VERSION_PREFIX + "/clusters/.*?/hosts.*";
  private static final String API_CLUSTER_CONFIGURATIONS_ALL_PATTERN = API_VERSION_PREFIX + "/clusters/.*?/configurations.*";
  private static final String API_CLUSTER_COMPONENTS_ALL_PATTERN = API_VERSION_PREFIX + "/clusters/.*?/components.*";
  private static final String API_CLUSTER_HOST_COMPONENTS_ALL_PATTERN = API_VERSION_PREFIX + "/clusters/.*?/host_components.*";
  private static final String API_CLUSTER_CONFIG_GROUPS_ALL_PATTERN = API_VERSION_PREFIX + "/clusters/.*?/config_groups.*";
  private static final String API_STACK_VERSIONS_PATTERN = API_VERSION_PREFIX + "/stacks/.*?/versions/.*";
  private static final String API_HOSTS_ALL_PATTERN = API_VERSION_PREFIX + "/hosts.*";
  private static final String API_ALERT_TARGETS_ALL_PATTERN = API_VERSION_PREFIX + "/alert_targets.*";
  private static final String API_BOOTSTRAP_PATTERN_ALL = API_VERSION_PREFIX + "/bootstrap.*";
  private static final String API_REQUESTS_ALL_PATTERN = API_VERSION_PREFIX + "/requests.*";
  private static final String API_CLUSTERS_UPGRADES_PATTERN = API_VERSION_PREFIX + "/clusters/.*?/upgrades.*";

  protected static final String LOGIN_REDIRECT_BASE = "/#/login?targetURI=";

  /**
   * The Ambari authentication entry point
   */
  private final AmbariEntryPoint entryPoint;

  /**
   * Access to Ambari configuration data
   */
  private final Configuration configuration;

  /**
   * Access to user information
   */
  private final Users users;

  /**
   * The audit logger
   */
  private final AuditLogger auditLogger;

  /**
   * A Permission Helper used to provided inforamtion for the Audit Logger
   */
  private final PermissionHelper permissionHelper;

  /**
   * The realm to use for the basic http auth
   */
  private String realm;

  /**
   * Constructor.
   *
   * @param entryPoint       the authentication entrypoint
   * @param configuration    the Ambari configuration
   * @param users            Ambari user access
   * @param auditLogger      the Audit logger
   * @param permissionHelper the permission helper
   */
  public AmbariAuthorizationFilter(AmbariEntryPoint entryPoint,
                                   Configuration configuration,
                                   Users users,
                                   AuditLogger auditLogger,
                                   PermissionHelper permissionHelper) {
    this.entryPoint = entryPoint;
    this.configuration = configuration;
    this.users = users;
    this.auditLogger = auditLogger;
    this.permissionHelper = permissionHelper;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    realm = getParameterValue(filterConfig, REALM_PARAM, DEFAULT_REALM);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    String requestURI = httpRequest.getRequestURI();

    SecurityContext context = getSecurityContext();

    Authentication authentication = context.getAuthentication();

    AuditEvent auditEvent = null;

    //  If no explicit authenticated user is set, set it to the default user (if one is specified)
    if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
      Authentication defaultAuthentication = getDefaultAuthentication();
      if (defaultAuthentication != null) {
        context.setAuthentication(defaultAuthentication);
        authentication = defaultAuthentication;
      }
    }
    if (authentication == null || authentication instanceof AnonymousAuthenticationToken ||
        !authentication.isAuthenticated()) {
      String token = httpRequest.getHeader(INTERNAL_TOKEN_HEADER);
      if (token != null) {
        InternalAuthenticationToken internalAuthenticationToken = new InternalAuthenticationToken(token);
        context.setAuthentication(internalAuthenticationToken);
        if(auditLogger.isEnabled()) {
          LoginAuditEvent loginAuditEvent = LoginAuditEvent.builder()
            .withUserName(internalAuthenticationToken.getName())
            .withProxyUserName(AuthorizationHelper.getProxyUserName(internalAuthenticationToken))
            .withRemoteIp(RequestUtils.getRemoteAddress(httpRequest))
            .withRoles(permissionHelper.getPermissionLabels(authentication))
            .withTimestamp(System.currentTimeMillis()).build();
          auditLogger.log(loginAuditEvent);
        }
      } else {
        // for view access, we should redirect to the Ambari login
        if (requestURI.matches(VIEWS_CONTEXT_ALL_PATTERN)) {
          String queryString = httpRequest.getQueryString();
          String requestedURL = queryString == null ? requestURI : (requestURI + '?' + queryString);
          String redirectURL = httpResponse.encodeRedirectURL(LOGIN_REDIRECT_BASE + requestedURL);
          httpResponse.sendRedirect(redirectURL);
        } else {
          entryPoint.commence(httpRequest, httpResponse, new AuthenticationCredentialsNotFoundException("Missing authentication token"));
        }
        return;
      }
    } else if (!authorizationPerformedInternally(requestURI)) {
      boolean authorized = false;

      if (requestURI.matches(API_BOOTSTRAP_PATTERN_ALL)) {
        authorized = AuthorizationHelper.isAuthorized(authentication,
            ResourceType.CLUSTER,
            null,
            EnumSet.of(RoleAuthorization.HOST_ADD_DELETE_HOSTS));
      }
      else {
        for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
          if (grantedAuthority instanceof AmbariGrantedAuthority) {

            AmbariGrantedAuthority ambariGrantedAuthority = (AmbariGrantedAuthority) grantedAuthority;

            PrivilegeEntity privilegeEntity = ambariGrantedAuthority.getPrivilegeEntity();
            Integer permissionId = privilegeEntity.getPermission().getId();

            // admin has full access
            if (permissionId.equals(PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION)) {
              authorized = true;
              break;
            }

            // clusters require permission
            if (!"GET".equalsIgnoreCase(httpRequest.getMethod()) && requestURI.matches(API_CREDENTIALS_AMBARI_PATTERN)) {
              // Only the administrator can operate on credentials where the alias starts with "ambari."
              if (permissionId.equals(PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION)) {
                authorized = true;
                break;
              }
            } else if (requestURI.matches(API_CLUSTERS_ALL_PATTERN)) {
              if (permissionId.equals(PermissionEntity.CLUSTER_USER_PERMISSION) ||
                  permissionId.equals(PermissionEntity.CLUSTER_ADMINISTRATOR_PERMISSION)) {
                authorized = true;
                break;
              }
            } else if (STACK_ADVISOR_REGEX.matcher(requestURI).matches()) {
              //TODO permissions model doesn't manage stacks api, but we need access to stack advisor to save configs
              if (permissionId.equals(PermissionEntity.CLUSTER_USER_PERMISSION) ||
                  permissionId.equals(PermissionEntity.CLUSTER_ADMINISTRATOR_PERMISSION)) {
                authorized = true;
                break;
              }
            } else if (requestURI.matches(API_VIEWS_ALL_PATTERN)) {
              // views require permission
              if (permissionId.equals(PermissionEntity.VIEW_USER_PERMISSION)) {
                authorized = true;
                break;
              }
            }
          }
        }

        // Allow all GETs that are not LDAP sync events...
        authorized = authorized || (httpRequest.getMethod().equals("GET") && !requestURI.matches(API_LDAP_SYNC_EVENTS_ALL_PATTERN));
      }

      if (!authorized) {
        if(auditLogger.isEnabled()) {
          auditEvent = AccessUnauthorizedAuditEvent.builder()
            .withHttpMethodName(httpRequest.getMethod())
            .withRemoteIp(RequestUtils.getRemoteAddress(httpRequest))
            .withResourcePath(httpRequest.getRequestURI())
            .withUserName(AuthorizationHelper.getAuthenticatedName())
            .withProxyUserName(AuthorizationHelper.getProxyUserName())
            .withTimestamp(System.currentTimeMillis())
            .build();
          auditLogger.log(auditEvent);
        }

        httpResponse.setHeader("WWW-Authenticate", "Basic realm=\"" + realm + "\"");
        httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permissions to access this resource.");
        httpResponse.flushBuffer();
        return;
      }
    }
    if (AuthorizationHelper.getAuthenticatedName() != null) {
      httpResponse.setHeader("User", AuthorizationHelper.getAuthenticatedName());
      if (auditLogger.isEnabled() && httpResponse.getStatus() == HttpServletResponse.SC_FORBIDDEN) {
        auditEvent = AccessUnauthorizedAuditEvent.builder()
          .withHttpMethodName(httpRequest.getMethod())
          .withRemoteIp(RequestUtils.getRemoteAddress(httpRequest))
          .withResourcePath(httpRequest.getRequestURI())
          .withUserName(AuthorizationHelper.getAuthenticatedName())
          .withProxyUserName(AuthorizationHelper.getProxyUserName())
          .withTimestamp(System.currentTimeMillis())
          .build();
        auditLogger.log(auditEvent);
      }
    }
    chain.doFilter(request, response);
  }

  /**
   * Creates the default Authentication if a default user is configured
   *
   * @return an Authentication representing the default user
   */
  private Authentication getDefaultAuthentication() {
    Authentication defaultUser = null;

    if ((configuration != null) && (users != null)) {
      String username = configuration.getDefaultApiAuthenticatedUser();

      if (!StringUtils.isEmpty(username)) {
        final User user = users.getUser(username);

        if (user != null) {
          Principal principal = new Principal() {
            @Override
            public String getName() {
              return user.getUserName();
            }
          };

          defaultUser = new UsernamePasswordAuthenticationToken(principal, null,
              users.getUserAuthorities(user.getUserName()));
        }
      }
    }

    return defaultUser;
  }

  /**
   * Tests the URI to determine if authorization checks are performed internally or should be
   * performed in the filter.
   *
   * @param requestURI the request uri
   * @return true if handled internally; otherwise false
   */
  private boolean authorizationPerformedInternally(String requestURI) {
    return requestURI.matches(API_USERS_ALL_PATTERN) ||
        requestURI.matches(API_REQUESTS_ALL_PATTERN) ||
        requestURI.matches(API_GROUPS_ALL_PATTERN) ||
        requestURI.matches(API_CREDENTIALS_ALL_PATTERN) ||
        requestURI.matches(API_PRIVILEGES_ALL_PATTERN) ||
        requestURI.matches(API_CLUSTER_REQUESTS_ALL_PATTERN) ||
        requestURI.matches(API_CLUSTER_SERVICES_ALL_PATTERN) ||
        requestURI.matches(API_CLUSTER_ALERT_ALL_PATTERN) ||
        requestURI.matches(API_CLUSTERS_PATTERN) ||
        requestURI.matches(API_STACK_VERSIONS_PATTERN) ||
        requestURI.matches(API_VIEWS_ALL_PATTERN) ||
        requestURI.matches(VIEWS_CONTEXT_PATH_PATTERN) ||
        requestURI.matches(API_WIDGET_LAYOUTS_PATTERN) ||
        requestURI.matches(API_WIDGET_PATTERN) ||
        requestURI.matches(API_CLUSTER_HOSTS_ALL_PATTERN) ||
        requestURI.matches(API_CLUSTER_CONFIGURATIONS_ALL_PATTERN) ||
        requestURI.matches(API_CLUSTER_COMPONENTS_ALL_PATTERN) ||
        requestURI.matches(API_CLUSTER_HOST_COMPONENTS_ALL_PATTERN) ||
        requestURI.matches(API_CLUSTER_CONFIG_GROUPS_ALL_PATTERN) ||
        requestURI.matches(API_HOSTS_ALL_PATTERN) ||
        requestURI.matches(API_ALERT_TARGETS_ALL_PATTERN) ||
        requestURI.matches(API_PERSIST_ALL_PATTERN) ||
        requestURI.matches(API_CLUSTERS_UPGRADES_PATTERN);
  }

  @Override
  public void destroy() {
    // do nothing
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Get the parameter value from the given servlet filter configuration.
   *
   * @param filterConfig  the servlet configuration
   * @param parameterName the parameter name
   * @param defaultValue  the default value
   * @return the parameter value or the default value if not set
   */
  private static String getParameterValue(
      FilterConfig filterConfig, String parameterName, String defaultValue) {

    String value = filterConfig.getInitParameter(parameterName);
    if (value == null || value.length() == 0) {
      value = filterConfig.getServletContext().getInitParameter(parameterName);
    }
    return value == null || value.length() == 0 ? defaultValue : value;
  }

  SecurityContext getSecurityContext() {
    return SecurityContextHolder.getContext();
  }

  ViewRegistry getViewRegistry() {
    return ViewRegistry.getInstance();
  }
}
