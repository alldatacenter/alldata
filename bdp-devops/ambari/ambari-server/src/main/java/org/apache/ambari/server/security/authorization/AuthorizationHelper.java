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
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.RoleAuthorizationEntity;
import org.apache.ambari.server.security.authentication.AmbariProxiedUserDetailsImpl;
import org.apache.ambari.server.security.authentication.AmbariUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Provides utility methods for authentication functionality
 */
@Singleton
public class AuthorizationHelper {
  private final static Logger LOG = LoggerFactory.getLogger(AuthorizationHelper.class);

  @Inject
  static Provider<PrivilegeDAO> privilegeDAOProvider;

  @Inject
  static Provider<ViewInstanceDAO> viewInstanceDAOProvider;

  /**
   * Gets the name of the logged-in proxy user, if any.
   *
   * @param authentication
   * @return the name of the logged-in proxy user
   */
  public static String getProxyUserName(Authentication authentication) {
    if (authentication==null){
      return null;
    }
    Object userDetails = authentication.getPrincipal();
    if (userDetails instanceof AmbariProxiedUserDetailsImpl) {
      AmbariProxiedUserDetailsImpl ambariProxiedUserDetails = (AmbariProxiedUserDetailsImpl) userDetails;
      return ambariProxiedUserDetails.getProxyUserDetails().getUsername();
    }
    return null;
  }

  /**
   * Gets the name of the logged-in proxy user, if any.
   *
   * @return the name of the logged-in proxy user
   */
  public static String getProxyUserName() {
    SecurityContext securityContext = SecurityContextHolder.getContext();
    Authentication auth = securityContext.getAuthentication();
    return getProxyUserName(auth);
  }

  /**
   * Converts collection of RoleEntities to collection of GrantedAuthorities
   */
  public Collection<GrantedAuthority> convertPrivilegesToAuthorities(Collection<PrivilegeEntity> privilegeEntities) {
    Set<GrantedAuthority> authorities = new HashSet<>(privilegeEntities.size());

    for (PrivilegeEntity privilegeEntity : privilegeEntities) {
      authorities.add(new AmbariGrantedAuthority(privilegeEntity));
    }

    return authorities;
  }

  /**
   * Gets the name of the logged in user.  Thread-safe due to use of thread-local.
   *
   * @return the name of the logged in user, or <code>null</code> if none set.
   */
  public static String getAuthenticatedName() {
    return getAuthenticatedName(null);
  }

  /**
   * Gets the name of the logged-in user, if any.  Thread-safe due to use of
   * thread-local.
   *
   * @param defaultUsername the value if there is no logged-in user
   * @return the name of the logged-in user, or the default
   */
  public static String getAuthenticatedName(String defaultUsername) {
    SecurityContext securityContext = SecurityContextHolder.getContext();

    Authentication auth = securityContext.getAuthentication();

    return (null == auth) ? defaultUsername : auth.getName();
  }

  /**
   * Gets the ID of the logged-in user.  Thread-safe due to use of
   * thread-local.
   *
   * @return the ID of the logged-in user
   */
  public static int getAuthenticatedId() {
    SecurityContext securityContext = SecurityContextHolder.getContext();

    Authentication authentication = securityContext.getAuthentication();
    Object principal = (authentication == null) ? null : authentication.getPrincipal();

    if (principal instanceof AmbariUserDetails) {
      return ((AmbariUserDetails) principal).getUserId();
    }

    return -1;
  }

  /**
   * Determines if the authenticated user (from application's security context) is authorized to
   * perform an operation on the specific resource by matching the authenticated user's
   * authorizations with the one indicated.
   *
   * @param resourceType          a resource type being acted upon
   * @param resourceId            the privilege resource id (or adminresource.id) of the relevant resource
   * @param requiredAuthorization the required authorization
   * @return true if authorized; otherwise false
   * @see #isAuthorized(Authentication, ResourceType, Long, Set)
   */
  public static boolean isAuthorized(ResourceType resourceType, Long resourceId,
                                     RoleAuthorization requiredAuthorization) {
    return isAuthorized(getAuthentication(), resourceType, resourceId, EnumSet.of(requiredAuthorization));
  }

  /**
   * Determines if the authenticated user (from application's security context) is authorized to
   * perform an operation on the specific resource by matching the authenticated user's
   * authorizations with one from the provided set of authorizations.
   *
   * @param resourceType           a resource type being acted upon
   * @param resourceId             the privilege resource id (or adminresource.id) of the relevant resource
   * @param requiredAuthorizations a set of requirements for which one match will allow authorization
   * @return true if authorized; otherwise false
   * @see #isAuthorized(Authentication, ResourceType, Long, Set)
   */
  public static boolean isAuthorized(ResourceType resourceType, Long resourceId,
                                     Set<RoleAuthorization> requiredAuthorizations) {
    return isAuthorized(getAuthentication(), resourceType, resourceId, requiredAuthorizations);
  }

  /**
   * Determines if the specified authenticated user is authorized to perform an operation on the
   * specific resource by matching the authenticated user's authorizations with the one indicated.
   *
   * @param authentication        the authenticated user and associated access privileges
   * @param resourceType          a resource type being acted upon
   * @param resourceId            the privilege resource id (or adminresource.id) of the relevant resource
   * @param requiredAuthorization the required authorization
   * @return true if authorized; otherwise false
   * @see #isAuthorized(Authentication, ResourceType, Long, Set)
   */
  public static boolean isAuthorized(Authentication authentication, ResourceType resourceType,
                                     Long resourceId, RoleAuthorization requiredAuthorization) {
    return isAuthorized(authentication, resourceType, resourceId, EnumSet.of(requiredAuthorization));
  }

  /**
   * Determines if the specified authenticated user is authorized to perform an operation on the
   * the specific resource by matching the authenticated user's authorizations with one from the
   * provided set of authorizations.
   * <p/>
   * The specified resource type is a high-level resource such as {@link ResourceType#AMBARI Ambari},
   * a {@link ResourceType#CLUSTER cluster}, or a {@link ResourceType#VIEW view}.
   * <p/>
   * The specified resource id is the (admin)resource id referenced by a specific resource instance
   * such as a cluster or view.
   *
   * @param authentication         the authenticated user and associated access privileges
   * @param resourceType           a resource type being acted upon
   * @param resourceId             the privilege resource id (or adminresource.id) of the relevant resource
   * @param requiredAuthorizations a set of requirements for which one match will allow authorization
   * @return true if authorized; otherwise false
   */
  public static boolean isAuthorized(Authentication authentication, ResourceType resourceType,
                                     Long resourceId, Set<RoleAuthorization> requiredAuthorizations) {
    if ((requiredAuthorizations == null) || requiredAuthorizations.isEmpty()) {
      return true;
    } else if (authentication == null) {
      return false;
    } else {
      // Iterate through the set of required authorizations to see if at least one match is found.
      // If the user has at least one authorization that exists in the set of required authorizations,
      // that user is authorized to perform the operation.
      for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
        AmbariGrantedAuthority ambariGrantedAuthority = (AmbariGrantedAuthority) grantedAuthority;
        PrivilegeEntity privilegeEntity = ambariGrantedAuthority.getPrivilegeEntity();
        ResourceEntity privilegeResource = privilegeEntity.getResource();
        ResourceType privilegeResourceType = ResourceType.translate(privilegeResource.getResourceType().getName());
        boolean resourceOK;

        if (ResourceType.AMBARI == privilegeResourceType) {
          // This resource type indicates administrative access
          resourceOK = true;
        } else if ((resourceType == null) || (resourceType == privilegeResourceType)) {
          resourceOK = (resourceId == null) || resourceId.equals(privilegeResource.getId());
        } else {
          resourceOK = false;
        }

        // The the authority is for the relevant resource, see if one of the authorizations matches
        // one of the required authorizations...
        if (resourceOK) {
          PermissionEntity permission = privilegeEntity.getPermission();
          Collection<RoleAuthorizationEntity> userAuthorizations = (permission == null)
              ? null
              : permission.getAuthorizations();

          if (userAuthorizations != null) {
            for (RoleAuthorizationEntity userAuthorization : userAuthorizations) {
              try {
                if (requiredAuthorizations.contains(RoleAuthorization.translate(userAuthorization.getAuthorizationId()))) {
                  return true;
                }
              } catch (IllegalArgumentException e) {
                LOG.warn("Invalid authorization name, '{}'... ignoring.", userAuthorization.getAuthorizationId());
              }
            }
          }
        }
      }

      return false;
    }
  }

  /**
   * Determines if the authenticated user (from application's security context) is authorized to
   * perform an operation on the the specific resource by matching the authenticated user's
   * authorizations with one from the provided set of authorizations.
   * <p/>
   * If not authorized, an {@link AuthorizationException} will be thrown.
   *
   * @param resourceType           a resource type being acted upon
   * @param resourceId             the privilege resource id (or adminresource.id) of the relevant resource
   * @param requiredAuthorizations a set of requirements for which one match will allow authorization
   * @throws AuthorizationException if authorization is not granted
   * @see #isAuthorized(ResourceType, Long, Set)
   */
  public static void verifyAuthorization(ResourceType resourceType,
                                         Long resourceId,
                                         Set<RoleAuthorization> requiredAuthorizations)
      throws AuthorizationException {
    if (!isAuthorized(resourceType, resourceId, requiredAuthorizations)) {
      throw new AuthorizationException();
    }
  }

  /**
   * Determines if the specified authenticated user is authorized to perform an operation on the
   * the specific resource by matching the authenticated user's authorizations with one from the
   * provided set of authorizations.
   * <p/>
   * If not authorized, an {@link AuthorizationException} will be thrown.
   *
   * @param authentication         the authenticated user and associated access privileges
   * @param resourceType           a resource type being acted upon
   * @param resourceId             the privilege resource id (or adminresource.id) of the relevant resource
   * @param requiredAuthorizations a set of requirements for which one match will allow authorization
   * @throws AuthorizationException if authorization is not granted
   * @see #isAuthorized(Authentication, ResourceType, Long, Set)
   */
  public static void verifyAuthorization(Authentication authentication,
                                         ResourceType resourceType,
                                         Long resourceId,
                                         Set<RoleAuthorization> requiredAuthorizations)
      throws AuthorizationException {
    if (!isAuthorized(authentication, resourceType, resourceId, requiredAuthorizations)) {
      throw new AuthorizationException();
    }
  }

  /**
   * Retrieves the authenticated user and authorization details from the application's security context.
   *
   * @return the authenticated user and associated access privileges; or null if not available
   */
  public static Authentication getAuthentication() {
    SecurityContext context = SecurityContextHolder.getContext();
    return (context == null) ? null : context.getAuthentication();
  }

  /**
   * There are cases when users log-in with a login name that is
   * define in LDAP and which do not correspond to the user name stored
   * locally in ambari. These external login names act as an alias to
   * ambari users name. This method stores in the current http session a mapping
   * of alias user name to local ambari user name to make possible resolving
   * login alias to ambari user name.
   * @param ambariUserName ambari user name for which the alias is to be stored in the session
   * @param loginAlias the alias for the ambari user name.
   */
  public static void addLoginNameAlias(String ambariUserName, String loginAlias) {
    ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attr != null) {
      LOG.info("Adding login alias '{}' for user name '{}'", loginAlias, ambariUserName);
      attr.setAttribute(loginAlias, ambariUserName, RequestAttributes.SCOPE_SESSION);
    }
  }

  /**
   * Looks up the provided loginAlias in the current http session and return the ambari
   * user name that the alias is defined for.
   * @param loginAlias the login alias to resolve to ambari user name
   * @return the ambari user name if the alias is found otherwise returns the passed in loginAlias
   */
  public static String resolveLoginAliasToUserName(String loginAlias) {
    ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attr != null && attr.getAttribute(loginAlias, RequestAttributes.SCOPE_SESSION) != null) {
      return (String)attr.getAttribute(loginAlias, RequestAttributes.SCOPE_SESSION);
    }

    return loginAlias;
  }

  /**
   * Retrieve authorization names based on the details of the authenticated user
   * @param authentication the authenticated user and associated access privileges
   * @return human readable role authorizations
   */
  public static List<String> getAuthorizationNames(Authentication authentication) {
    List<String> authorizationNames = Lists.newArrayList();
    if (authentication.getAuthorities() != null) {
      for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
        AmbariGrantedAuthority ambariGrantedAuthority = (AmbariGrantedAuthority) grantedAuthority;

        PrivilegeEntity privilegeEntity = ambariGrantedAuthority.getPrivilegeEntity();
        Collection<RoleAuthorizationEntity> roleAuthorizationEntities =
          privilegeEntity.getPermission().getAuthorizations();
        for (RoleAuthorizationEntity entity : roleAuthorizationEntities) {
          authorizationNames.add(entity.getAuthorizationName());
        }
      }
    }
    return authorizationNames;
  }

}
