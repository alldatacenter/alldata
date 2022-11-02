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
package org.apache.ambari.server.controller.internal;

import java.text.NumberFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.UserRequest;
import org.apache.ambari.server.controller.UserResponse;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.ResourcePredicateEvaluator;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.entities.MemberEntity;
import org.apache.ambari.server.orm.entities.UserAuthenticationEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.security.authorization.UserAuthenticationType;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Resource provider for user resources.
 */
public class UserResourceProvider extends AbstractControllerResourceProvider implements ResourcePredicateEvaluator {

  private static final Logger LOG = LoggerFactory.getLogger(UserResourceProvider.class);

  // ----- Property ID constants ---------------------------------------------

  public static final String USER_RESOURCE_CATEGORY = "Users";

  // Users
  public static final String USERNAME_PROPERTY_ID = "user_name";
  public static final String DISPLAY_NAME_PROPERTY_ID = "display_name";
  public static final String LOCAL_USERNAME_PROPERTY_ID = "local_user_name";
  public static final String ACTIVE_PROPERTY_ID = "active";
  public static final String CREATE_TIME_PROPERTY_ID = "created";
  public static final String CONSECUTIVE_FAILURES_PROPERTY_ID = "consecutive_failures";
  public static final String ADMIN_PROPERTY_ID = "admin";
  public static final String GROUPS_PROPERTY_ID = "groups";

  public static final String USER_USERNAME_PROPERTY_ID = USER_RESOURCE_CATEGORY + "/" + USERNAME_PROPERTY_ID;
  public static final String USER_DISPLAY_NAME_PROPERTY_ID = USER_RESOURCE_CATEGORY + "/" + DISPLAY_NAME_PROPERTY_ID;
  public static final String USER_LOCAL_USERNAME_PROPERTY_ID = USER_RESOURCE_CATEGORY + "/" + LOCAL_USERNAME_PROPERTY_ID;
  public static final String USER_ACTIVE_PROPERTY_ID = USER_RESOURCE_CATEGORY + "/" + ACTIVE_PROPERTY_ID;
  public static final String USER_CREATE_TIME_PROPERTY_ID = USER_RESOURCE_CATEGORY + "/" + CREATE_TIME_PROPERTY_ID;
  public static final String USER_CONSECUTIVE_FAILURES_PROPERTY_ID = USER_RESOURCE_CATEGORY + "/" + CONSECUTIVE_FAILURES_PROPERTY_ID;
  public static final String USER_ADMIN_PROPERTY_ID = USER_RESOURCE_CATEGORY + "/" + ADMIN_PROPERTY_ID;
  public static final String USER_GROUPS_PROPERTY_ID = USER_RESOURCE_CATEGORY + "/" + GROUPS_PROPERTY_ID;

  /* *******************************************************
   * Deprecated properties, kept for backwards compatibility and to maintain API V1 contract.
   * These properties are related to a user's authentication resource.
   * ******************************************************* */
  @Deprecated
  public static final String PASSWORD_PROPERTY_ID = "password";
  @Deprecated
  public static final String OLD_PASSWORD_PROPERTY_ID = "old_password";
  @Deprecated
  public static final String LDAP_USER_PROPERTY_ID = "ldap_user";
  @Deprecated
  public static final String USER_TYPE_PROPERTY_ID = "user_type";

  @Deprecated
  public static final String USER_PASSWORD_PROPERTY_ID = USER_RESOURCE_CATEGORY + "/" + PASSWORD_PROPERTY_ID;
  @Deprecated
  public static final String USER_OLD_PASSWORD_PROPERTY_ID = USER_RESOURCE_CATEGORY + "/" + OLD_PASSWORD_PROPERTY_ID;
  @Deprecated
  public static final String USER_LDAP_USER_PROPERTY_ID = USER_RESOURCE_CATEGORY + "/" + LDAP_USER_PROPERTY_ID;
  @Deprecated
  public static final String USER_USER_TYPE_PROPERTY_ID = USER_RESOURCE_CATEGORY + "/" + USER_TYPE_PROPERTY_ID;
  /* ******************************************************* */

  /**
   * The key property ids for a User resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.User, USER_USERNAME_PROPERTY_ID)
      .build();

  private static final Set<String> propertyIds = Sets.newHashSet(
      USER_USERNAME_PROPERTY_ID,
      USER_DISPLAY_NAME_PROPERTY_ID,
      USER_LOCAL_USERNAME_PROPERTY_ID,
      USER_ACTIVE_PROPERTY_ID,
      USER_CREATE_TIME_PROPERTY_ID,
      USER_CONSECUTIVE_FAILURES_PROPERTY_ID,
      USER_GROUPS_PROPERTY_ID,
      USER_PASSWORD_PROPERTY_ID,
      USER_OLD_PASSWORD_PROPERTY_ID,
      USER_LDAP_USER_PROPERTY_ID,
      USER_USER_TYPE_PROPERTY_ID,
      USER_ADMIN_PROPERTY_ID
  );

  @Inject
  private Users users;

  /**
   * Create a new resource provider for the given management controller.
   */
  @AssistedInject
  UserResourceProvider(@Assisted AmbariManagementController managementController) {
    super(Resource.Type.User, propertyIds, keyPropertyIds, managementController);

    setRequiredCreateAuthorizations(EnumSet.of(RoleAuthorization.AMBARI_MANAGE_USERS));
    setRequiredDeleteAuthorizations(EnumSet.of(RoleAuthorization.AMBARI_MANAGE_USERS));
  }

  @Override
  public RequestStatus createResourcesAuthorized(Request request)
      throws SystemException,
      UnsupportedPropertyException,
      ResourceAlreadyExistsException,
      NoSuchParentResourceException {
    final Set<UserRequest> requests = new HashSet<>();
    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }

    createResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        try {
          createUsers(requests);
        } catch (AuthorizationException e) {
          throw new AmbariException(e.getMessage(), e);
        }
        return null;
      }
    });

    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<UserRequest> requests = new HashSet<>();

    if (predicate == null) {
      requests.add(getRequest(null));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }

    Set<UserResponse> responses = getResources(new Command<Set<UserResponse>>() {
      @Override
      public Set<UserResponse> invoke() throws AmbariException, AuthorizationException {
        return getUsers(requests);
      }
    });

    if (LOG.isDebugEnabled()) {
      LOG.debug("Found user responses matching get user request, userRequestSize={}, userResponseSize={}", requests.size(), responses.size());
    }

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources = new HashSet<>();

    for (UserResponse userResponse : responses) {
      ResourceImpl resource = new ResourceImpl(Resource.Type.User);

      setResourceProperty(resource, USER_USERNAME_PROPERTY_ID,
          userResponse.getUsername(), requestedIds);

      setResourceProperty(resource, USER_DISPLAY_NAME_PROPERTY_ID,
          userResponse.getDisplayName(), requestedIds);

      setResourceProperty(resource, USER_LOCAL_USERNAME_PROPERTY_ID,
          userResponse.getLocalUsername(), requestedIds);

      // This is deprecated but here for backwards compatibility
      setResourceProperty(resource, USER_LDAP_USER_PROPERTY_ID,
          userResponse.isLdapUser(), requestedIds);

      // This is deprecated but here for backwards compatibility
      setResourceProperty(resource, USER_USER_TYPE_PROPERTY_ID,
          userResponse.getAuthenticationType(), requestedIds);

      setResourceProperty(resource, USER_ACTIVE_PROPERTY_ID,
          userResponse.isActive(), requestedIds);

      setResourceProperty(resource, USER_GROUPS_PROPERTY_ID,
          userResponse.getGroups(), requestedIds);

      setResourceProperty(resource, USER_ADMIN_PROPERTY_ID,
          userResponse.isAdmin(), requestedIds);

      setResourceProperty(resource, USER_CONSECUTIVE_FAILURES_PROPERTY_ID,
          userResponse.getConsecutiveFailures(), requestedIds);

      setResourceProperty(resource, USER_CREATE_TIME_PROPERTY_ID,
          userResponse.getCreateTime(), requestedIds);

      resources.add(resource);
    }

    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<UserRequest> requests = new HashSet<>();

    for (Map<String, Object> propertyMap : getPropertyMaps(request.getProperties().iterator().next(), predicate)) {
      UserRequest req = getRequest(propertyMap);

      requests.add(req);
    }

    modifyResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException, AuthorizationException {
        updateUsers(requests);
        return null;
      }
    });

    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<UserRequest> requests = new HashSet<>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      UserRequest req = getRequest(propertyMap);

      requests.add(req);
    }

    modifyResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        deleteUsers(requests);
        return null;
      }
    });

    return getRequestStatus(null);
  }

  /**
   * ResourcePredicateEvaluator implementation. If property type is User/user_name,
   * we do a case insensitive comparison so that we can return the retrieved
   * username when it differs only in case with respect to the requested username.
   *
   * @param predicate the predicate
   * @param resource  the resource
   * @return
   */
  @Override
  public boolean evaluate(Predicate predicate, Resource resource) {
    if (predicate instanceof EqualsPredicate) {
      EqualsPredicate equalsPredicate = (EqualsPredicate) predicate;
      String propertyId = equalsPredicate.getPropertyId();
      if (propertyId.equals(USER_USERNAME_PROPERTY_ID)) {
        return equalsPredicate.evaluateIgnoreCase(resource);
      }
    }
    return predicate.evaluate(resource);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }

  private UserRequest getRequest(Map<String, Object> properties) {
    if (properties == null) {
      return new UserRequest(null);
    }

    UserRequest request = new UserRequest((String) properties.get(USER_USERNAME_PROPERTY_ID));

    request.setDisplayName((String) properties.get(USER_DISPLAY_NAME_PROPERTY_ID));
    request.setLocalUserName((String) properties.get(USER_LOCAL_USERNAME_PROPERTY_ID));

    request.setPassword((String) properties.get(USER_PASSWORD_PROPERTY_ID));
    request.setOldPassword((String) properties.get(USER_OLD_PASSWORD_PROPERTY_ID));

    if (null != properties.get(USER_ACTIVE_PROPERTY_ID)) {
      request.setActive(Boolean.valueOf(properties.get(USER_ACTIVE_PROPERTY_ID).toString()));
    }

    if (null != properties.get(USER_ADMIN_PROPERTY_ID)) {
      request.setAdmin(Boolean.valueOf(properties.get(USER_ADMIN_PROPERTY_ID).toString()));
    }

    if (null != properties.get(USER_CONSECUTIVE_FAILURES_PROPERTY_ID)) {
      request.setConsecutiveFailures(Integer.parseInt(properties.get(USER_CONSECUTIVE_FAILURES_PROPERTY_ID).toString()));
    }

    return request;
  }


  /**
   * Creates users.
   *
   * @param requests the request objects which define the user.
   * @throws AmbariException when the user cannot be created.
   */
  private void createUsers(Set<UserRequest> requests) throws AmbariException, AuthorizationException {
    // First check for obvious issues... then try to create the accounts.  This will help to avoid
    // some accounts being created and some not due to an issue with one or more of the users.
    for (UserRequest request : requests) {
      String username = request.getUsername();

      if (StringUtils.isEmpty(username)) {
        throw new AmbariException("Username must be supplied.");
      }

      if (users.getUser(username) != null) {
        String message;
        if (requests.size() == 1) {
          message = "The requested username already exists.";
        } else {
          message = "One or more of the requested usernames already exists.";
        }
        throw new DuplicateResourceException(message);
      }
    }

    for (UserRequest request : requests) {
      String username = request.getUsername();
      String displayName = StringUtils.defaultIfEmpty(request.getDisplayName(), username);
      String localUserName = StringUtils.defaultIfEmpty(request.getLocalUserName(), username);

      UserEntity userEntity = users.createUser(username, localUserName, displayName, request.isActive());
      if (userEntity != null) {
        if (Boolean.TRUE.equals(request.isAdmin())) {
          users.grantAdminPrivilege(userEntity);
        }

        // Setting a user's the password here is to allow for backward compatibility with pre-Ambari-3.0
        // versions so that the contract for REST API V1 is maintained.
        if (!StringUtils.isEmpty(request.getPassword())) {
          // This is performed as a user administrator since the  authorization check was done prior
          // to executing #createResourcesAuthorized.
          addOrUpdateLocalAuthenticationSource(true, userEntity, request.getPassword(), null);
        }
      }
    }
  }

  /**
   * Updates the users specified.
   *
   * @param requests the users to modify
   * @throws AmbariException          if the resources cannot be updated
   * @throws IllegalArgumentException if the authenticated user is not authorized to update all of
   *                                  the requested properties
   */
  private void updateUsers(Set<UserRequest> requests) throws AmbariException, AuthorizationException {
    boolean asUserAdministrator = AuthorizationHelper.isAuthorized(ResourceType.AMBARI, null,
        RoleAuthorization.AMBARI_MANAGE_USERS);
    String authenticatedUsername = AuthorizationHelper.getAuthenticatedName();

    for (final UserRequest request : requests) {
      String requestedUsername = request.getUsername();

      // An administrator can modify any user, else a user can only modify themself.
      if (!asUserAdministrator && (!authenticatedUsername.equalsIgnoreCase(requestedUsername))) {
        throw new AuthorizationException();
      }

      UserEntity userEntity = users.getUserEntity(requestedUsername);
      if (null == userEntity) {// Only an user with the privs to manage users can update a user's active status
        continue;
      }

      boolean hasUpdates = false;
      if (isValueChanged(request.isActive(), userEntity.getActive())) {
        // If this value is being set, make sure the authenticated user is an administrator before
        // allowing to change it. Only administrators should be able to change a user's active state
        if (!asUserAdministrator) {
          throw new AuthorizationException("The authenticated user is not authorized to update the requested user's active property");
        }

        hasUpdates = true;
      }

      // Only an user with the privs to manage users can update a user's local username
      if (isValueChanged(request.getLocalUserName(), userEntity.getLocalUsername())) {
        // If this value is being set, make sure the authenticated user is an administrator before
        // allowing to change it. Only administrators should be able to change a user's active state
        if (!asUserAdministrator) {
          throw new AuthorizationException("The authenticated user is not authorized to update the requested user's local username property");
        }

        hasUpdates = true;
      }

      hasUpdates = hasUpdates || isValueChanged(request.getDisplayName(), userEntity.getDisplayName());

      if (hasUpdates) {
        users.safelyUpdateUserEntity(userEntity,
            new Users.Command() {
              @Override
              public void perform(UserEntity userEntity) {
                if (isValueChanged(request.isActive(), userEntity.getActive())) {
                  userEntity.setActive(request.isActive());
                }

                if (isValueChanged(request.getLocalUserName(), userEntity.getLocalUsername())) {
                  userEntity.setLocalUsername(request.getLocalUserName());
                }

                if (isValueChanged(request.getDisplayName(), userEntity.getDisplayName())) {
                  userEntity.setDisplayName(request.getDisplayName());
                }
              }
            });
      }

      // Only an user with the privs to manage users can update a user's roles
      if (null != request.isAdmin()) {
        // If this value is being set, make sure the authenticated user is an administrator before
        // allowing to change it. Only administrators should be able to change a user's administrative
        // privileges
        if (!asUserAdministrator) {
          throw new AuthorizationException("The authenticated user is not authorized to update the requested resource property");
        }

        if (request.isAdmin()) {
          users.grantAdminPrivilege(userEntity);
        } else {
          users.revokeAdminPrivilege(userEntity);
        }
      }

      // Setting/Changing a user's password here is for backward compatibility to maintain API V1 contract
      if (request.getPassword() != null) {
        addOrUpdateLocalAuthenticationSource(asUserAdministrator, userEntity, request.getPassword(), request.getOldPassword());
      }

      if (request.getConsecutiveFailures() != null) {
        if (!asUserAdministrator) {
          throw new AuthorizationException("The authenticated user is not authorized to update the requested resource property");
        }
        users.safelyUpdateUserEntity(userEntity, user -> user.setConsecutiveFailures(request.getConsecutiveFailures()));
      }
    }
  }

  /**
   * Adds to updates a user's local authentication source by issuing a call to the {@link UserAuthenticationSourceResourceProvider}.
   * <p>
   * This is for backward compatibility to maintain the contract for Ambari's REST API version V1.
   *
   * @param asUserAdministrator true if the authenticated user have privs to manage user; false otherwise
   * @param subjectUserEntity   the user to update
   * @param password            the password to set, it is expected that this value is not <code>null</code>
   * @param oldPassword         the old/current password to use for verification is needed, this value may be <code>null</code>
   */
  private void addOrUpdateLocalAuthenticationSource(boolean asUserAdministrator, UserEntity subjectUserEntity, String password, String oldPassword)
      throws AuthorizationException, AmbariException {
    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(Resource.Type.UserAuthenticationSource,
        getManagementController());

    if (provider != null) {
      // Determine if the user already has an LOCAL authentication source setup...
      UserAuthenticationEntity userAuthenticationEntity = null;
      List<UserAuthenticationEntity> authenticationEntities = subjectUserEntity.getAuthenticationEntities();
      for (UserAuthenticationEntity authenticationEntity : authenticationEntities) {
        if (authenticationEntity.getAuthenticationType() == UserAuthenticationType.LOCAL) {
          userAuthenticationEntity = authenticationEntity;
          break;
        }
      }
      if (userAuthenticationEntity == null) {
        // a new authentication source needs to be create... only a privileged user can do this...
        if (!asUserAdministrator) {
          throw new AuthorizationException("The authenticated user is not authorized to create a local authentication source.");
        } else {
          Set<Map<String, Object>> propertiesSet = new HashSet<>();
          Map<String, Object> properties;
          properties = new LinkedHashMap<>();
          properties.put(UserAuthenticationSourceResourceProvider.AUTHENTICATION_USER_NAME_PROPERTY_ID, subjectUserEntity.getUserName());
          properties.put(UserAuthenticationSourceResourceProvider.AUTHENTICATION_AUTHENTICATION_TYPE_PROPERTY_ID, UserAuthenticationType.LOCAL.name());
          properties.put(UserAuthenticationSourceResourceProvider.AUTHENTICATION_KEY_PROPERTY_ID, password);
          propertiesSet.add(properties);

          try {
            provider.createResources(PropertyHelper.getCreateRequest(propertiesSet, null));
          } catch (Exception e) {
            throw new AmbariException(e.getMessage(), e);
          }
        }
      } else {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put(UserAuthenticationSourceResourceProvider.AUTHENTICATION_OLD_KEY_PROPERTY_ID, oldPassword);
        properties.put(UserAuthenticationSourceResourceProvider.AUTHENTICATION_KEY_PROPERTY_ID, password);

        Predicate predicate1 = new PredicateBuilder()
            .property(UserAuthenticationSourceResourceProvider.AUTHENTICATION_USER_NAME_PROPERTY_ID)
            .equals(subjectUserEntity.getUserName())
            .toPredicate();
        Predicate predicate2 = new PredicateBuilder()
            .property(UserAuthenticationSourceResourceProvider.AUTHENTICATION_AUTHENTICATION_SOURCE_ID_PROPERTY_ID)
            .equals(convertIdToString(userAuthenticationEntity.getUserAuthenticationId()))
            .toPredicate();

        try {
          provider.updateResources(PropertyHelper.getUpdateRequest(properties, null), new AndPredicate(predicate1, predicate2));
        } catch (Exception e) {
          throw new AmbariException(e.getMessage(), e);
        }
      }
    }
  }

  /**
   * Safely converts an id value to a string
   *
   * @param id the value to convert
   * @return a string representation of the id
   */
  private String convertIdToString(Long id) {
    if (id == null) {
      return null;
    } else {
      NumberFormat format = NumberFormat.getIntegerInstance();
      format.setGroupingUsed(false);
      return format.format(id);
    }
  }

  private boolean isValueChanged(Object newValue, Object currentValue) {
    return (newValue != null) && !newValue.equals(currentValue);
  }

  /**
   * Deletes the users specified.
   *
   * @param requests the users to delete
   * @throws AmbariException if the resources cannot be deleted
   */
  private void deleteUsers(Set<UserRequest> requests)
      throws AmbariException {

    for (UserRequest r : requests) {
      String username = r.getUsername();
      if (!StringUtils.isEmpty(username)) {

        if (LOG.isDebugEnabled()) {
          LOG.debug("Received a delete user request, username= {}", username);
        }

        users.removeUser(users.getUserEntity(username));
      }
    }
  }

  /**
   * Gets the users identified by the given request objects.
   *
   * @param requests the request objects
   * @return a set of user responses
   * @throws AmbariException if the users could not be read
   */
  private Set<UserResponse> getUsers(Set<UserRequest> requests)
      throws AmbariException, AuthorizationException {

    Set<UserResponse> responses = new HashSet<>();

    for (UserRequest r : requests) {

      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a getUsers request, userRequest={}", r.toString());
      }

      String requestedUsername = r.getUsername();
      String authenticatedUsername = AuthorizationHelper.getAuthenticatedName();

      // A user resource may be retrieved by an administrator or the same user.
      if (!AuthorizationHelper.isAuthorized(ResourceType.AMBARI, null, RoleAuthorization.AMBARI_MANAGE_USERS)) {
        if (null == requestedUsername) {
          // Since the authenticated user is not the administrator, force only that user's resource
          // to be returned
          requestedUsername = authenticatedUsername;
        } else if (!requestedUsername.equalsIgnoreCase(authenticatedUsername)) {
          // Since the authenticated user is not the administrator and is asking for a different user,
          // throw an AuthorizationException
          throw new AuthorizationException();
        }
      }

      // get them all
      if (null == requestedUsername) {
        for (UserEntity u : users.getAllUserEntities()) {
          responses.add(createUserResponse(u));
        }
      } else {

        UserEntity u = users.getUserEntity(requestedUsername);
        if (null == u) {
          if (requests.size() == 1) {
            // only throw exceptin if there is a single request
            // if there are multiple requests, this indicates an OR predicate
            throw new ObjectNotFoundException("Cannot find user '"
                + requestedUsername + "'");
          }
        } else {
          responses.add(createUserResponse(u));
        }
      }
    }

    return responses;
  }

  private UserResponse createUserResponse(UserEntity userEntity) {
    List<UserAuthenticationEntity> authenticationEntities = userEntity.getAuthenticationEntities();
    boolean isLdapUser = false;
    UserAuthenticationType userType = UserAuthenticationType.LOCAL;

    for (UserAuthenticationEntity authenticationEntity : authenticationEntities) {
      if (authenticationEntity.getAuthenticationType() == UserAuthenticationType.LDAP) {
        isLdapUser = true;
        userType = UserAuthenticationType.LDAP;
      } else if (authenticationEntity.getAuthenticationType() == UserAuthenticationType.PAM) {
        userType = UserAuthenticationType.PAM;
      }
    }

    Set<String> groups = new HashSet<>();
    for (MemberEntity memberEntity : userEntity.getMemberEntities()) {
      groups.add(memberEntity.getGroup().getGroupName());
    }

    boolean isAdmin = users.hasAdminPrivilege(userEntity);

    UserResponse userResponse = new UserResponse(userEntity.getUserName(),
        userEntity.getDisplayName(),
        userEntity.getLocalUsername(),
        userType,
        isLdapUser,
        userEntity.getActive(),
        isAdmin,
        userEntity.getConsecutiveFailures(),
        new Date(userEntity.getCreateTime()));
    userResponse.setGroups(groups);
    return userResponse;
  }

}
