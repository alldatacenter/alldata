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

import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrivilegeResponse;
import org.apache.ambari.server.controller.UserAuthenticationSourceRequest;
import org.apache.ambari.server.controller.UserAuthenticationSourceResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.orm.entities.UserAuthenticationEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.security.authorization.UserAuthenticationType;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.exception.ResourceNotFoundException;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

/**
 * Resource provider for user authentication source resources.
 */
public class UserAuthenticationSourceResourceProvider extends AbstractAuthorizedResourceProvider {

  public static final String AUTHENTICATION_SOURCE_RESOURCE_CATEGORY = "AuthenticationSourceInfo";

  public static final String AUTHENTICATION_SOURCE_ID_PROPERTY_ID = "source_id";
  public static final String USER_NAME_PROPERTY_ID = "user_name";
  public static final String AUTHENTICATION_TYPE_PROPERTY_ID = "authentication_type";
  public static final String KEY_PROPERTY_ID = "key";
  public static final String OLD_KEY_PROPERTY_ID = "old_key";
  public static final String CREATED_PROPERTY_ID = "created";
  public static final String UPDATED_PROPERTY_ID = "updated";

  public static final String AUTHENTICATION_AUTHENTICATION_SOURCE_ID_PROPERTY_ID = AUTHENTICATION_SOURCE_RESOURCE_CATEGORY + "/" + AUTHENTICATION_SOURCE_ID_PROPERTY_ID;
  public static final String AUTHENTICATION_USER_NAME_PROPERTY_ID = AUTHENTICATION_SOURCE_RESOURCE_CATEGORY + "/" + USER_NAME_PROPERTY_ID;
  public static final String AUTHENTICATION_AUTHENTICATION_TYPE_PROPERTY_ID = AUTHENTICATION_SOURCE_RESOURCE_CATEGORY + "/" + AUTHENTICATION_TYPE_PROPERTY_ID;
  public static final String AUTHENTICATION_KEY_PROPERTY_ID = AUTHENTICATION_SOURCE_RESOURCE_CATEGORY + "/" + KEY_PROPERTY_ID;
  public static final String AUTHENTICATION_OLD_KEY_PROPERTY_ID = AUTHENTICATION_SOURCE_RESOURCE_CATEGORY + "/" + OLD_KEY_PROPERTY_ID;
  public static final String AUTHENTICATION_CREATED_PROPERTY_ID = AUTHENTICATION_SOURCE_RESOURCE_CATEGORY + "/" + CREATED_PROPERTY_ID;
  public static final String AUTHENTICATION_UPDATED_PROPERTY_ID = AUTHENTICATION_SOURCE_RESOURCE_CATEGORY + "/" + UPDATED_PROPERTY_ID;

  private static final Set<String> PK_PROPERTY_IDS = ImmutableSet.of(
      AUTHENTICATION_AUTHENTICATION_SOURCE_ID_PROPERTY_ID
  );
  private static final Set<String> PROPERTY_IDS = ImmutableSet.of(
      AUTHENTICATION_AUTHENTICATION_SOURCE_ID_PROPERTY_ID,
      AUTHENTICATION_USER_NAME_PROPERTY_ID,
      AUTHENTICATION_AUTHENTICATION_TYPE_PROPERTY_ID,
      AUTHENTICATION_KEY_PROPERTY_ID,
      AUTHENTICATION_OLD_KEY_PROPERTY_ID,
      AUTHENTICATION_CREATED_PROPERTY_ID,
      AUTHENTICATION_UPDATED_PROPERTY_ID
  );
  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = ImmutableMap.of(
      Resource.Type.User, AUTHENTICATION_USER_NAME_PROPERTY_ID,
      Resource.Type.UserAuthenticationSource, AUTHENTICATION_AUTHENTICATION_SOURCE_ID_PROPERTY_ID
  );

  @Inject
  private Users users;

  /**
   * Constructor.
   */
  public UserAuthenticationSourceResourceProvider() {
    super(Resource.Type.UserAuthenticationSource, PROPERTY_IDS, KEY_PROPERTY_IDS);

    EnumSet<RoleAuthorization> requiredAuthorizations = EnumSet.of(RoleAuthorization.AMBARI_MANAGE_USERS);
    setRequiredCreateAuthorizations(requiredAuthorizations);
    setRequiredDeleteAuthorizations(requiredAuthorizations);
    setRequiredGetAuthorizations(requiredAuthorizations);
    setRequiredUpdateAuthorizations(requiredAuthorizations);
  }

  // ----- PrivilegeResourceProvider -----------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return PK_PROPERTY_IDS;
  }

  @Override
  public RequestStatus createResourcesAuthorized(Request request)
      throws SystemException, UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {
    final Set<UserAuthenticationSourceRequest> requests = new HashSet<>();
    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }

    createResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        createUserAuthenticationSources(requests);
        return null;
      }
    });

    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<UserAuthenticationSourceRequest> requests = new HashSet<>();
    if (predicate == null) {
      requests.add(getRequest(null));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }

    Set<UserAuthenticationSourceResponse> responses = getResources(new Command<Set<UserAuthenticationSourceResponse>>() {
      @Override
      public Set<UserAuthenticationSourceResponse> invoke() throws AmbariException, AuthorizationException {
        return getUserAuthenticationSources(requests);
      }
    });

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources = new HashSet<>();

    for (UserAuthenticationSourceResponse response : responses) {
      resources.add(toResource(response, requestedIds));
    }

    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<UserAuthenticationSourceRequest> requests = new HashSet<>();

    for (Map<String, Object> propertyMap : getPropertyMaps(request.getProperties().iterator().next(), predicate)) {
      requests.add(getRequest(propertyMap));
    }

    modifyResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException, AuthorizationException {
        updateUserAuthenticationSources(requests);
        return null;
      }
    });

    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<UserAuthenticationSourceRequest> requests = new HashSet<>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(propertyMap));
    }

    modifyResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException, AuthorizationException {
        deleteUserAuthenticationSources(requests);
        return null;
      }
    });

    return getRequestStatus(null);
  }

  private UserAuthenticationSourceRequest getRequest(Map<String, Object> properties) {
    String username;
    Long sourceId;
    UserAuthenticationType authenticationType;
    String key;
    String oldKey;

    if (properties == null) {
      username = null;
      sourceId = null;
      authenticationType = null;
      key = null;
      oldKey = null;
    } else {
      String tmp;

      username = (String) properties.get(AUTHENTICATION_USER_NAME_PROPERTY_ID);
      key = (String) properties.get(AUTHENTICATION_KEY_PROPERTY_ID);
      oldKey = (String) properties.get(AUTHENTICATION_OLD_KEY_PROPERTY_ID);

      tmp = (String) properties.get(AUTHENTICATION_AUTHENTICATION_SOURCE_ID_PROPERTY_ID);

      if (StringUtils.isEmpty(tmp)) {
        sourceId = null;
      } else {
        sourceId = Long.parseLong(tmp);
      }

      tmp = (String) properties.get(AUTHENTICATION_AUTHENTICATION_TYPE_PROPERTY_ID);
      if (StringUtils.isEmpty(tmp)) {
        authenticationType = null;
      } else {
        authenticationType = UserAuthenticationType.valueOf(tmp.trim().toUpperCase());
      }
    }

    return new UserAuthenticationSourceRequest(username, sourceId, authenticationType, key, oldKey);
  }

  /**
   * Creates user authentication sources.
   *
   * @param requests the request objects which define the user authentication source.
   * @throws AmbariException when the user authentication source cannot be created.
   */
  private void createUserAuthenticationSources(Set<UserAuthenticationSourceRequest> requests) throws AmbariException {
    for (UserAuthenticationSourceRequest request : requests) {
      String username = request.getUsername();
      if (StringUtils.isEmpty(username)) {
        throw new AmbariException("Username must be supplied.");
      }

      UserAuthenticationType authenticationType = request.getAuthenticationType();
      if (authenticationType == null) {
        throw new AmbariException("A value authentication type must be supplied.");
      }

      UserEntity userEntity = users.getUserEntity(username);
      if (userEntity == null) {
        throw new AmbariException("There is no user with the supplied username");
      }

      users.addAuthentication(userEntity, authenticationType, request.getKey());
    }
  }

  /**
   * Gets the users authentication sources identified by the given request objects.
   *
   * @param requests the request objects
   * @return a set of user responses
   * @throws AmbariException if the user authentication sources could not be read
   */
  private Set<UserAuthenticationSourceResponse> getUserAuthenticationSources(Set<UserAuthenticationSourceRequest> requests)
      throws AmbariException, AuthorizationException {

    Set<UserAuthenticationSourceResponse> responses = new HashSet<>();

    for (UserAuthenticationSourceRequest request : requests) {

      String requestedUsername = request.getUsername();
      String authenticatedUsername = AuthorizationHelper.getAuthenticatedName();

      // A user authentication source resource may be retrieved by an administrator or the same user.
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

      Collection<UserAuthenticationEntity> authenticationEntities = users.getUserAuthenticationEntities(requestedUsername, request.getAuthenticationType());
      if (authenticationEntities != null) {
        for (UserAuthenticationEntity authenticationEntity : authenticationEntities) {
          responses.add(createUserAuthenticationSourceResponse(authenticationEntity));
        }
      }
    }

    return responses;
  }

  /**
   * Delete the users authentication sources identified by the given request objects.
   * <p>
   * It ia assumed that the user has previously been authorized to perform this operation.
   *
   * @param requests the request objects
   * @throws AmbariException if the user authentication sources could not be read
   */
  private void deleteUserAuthenticationSources(Set<UserAuthenticationSourceRequest> requests)
      throws AmbariException, AuthorizationException {

    for (UserAuthenticationSourceRequest r : requests) {
      String username = r.getUsername();
      Long sourceId = r.getSourceId();
      if (!StringUtils.isEmpty(username) && (sourceId != null)) {
        users.removeAuthentication(username, sourceId);
      }
    }
  }

  private void updateUserAuthenticationSources(Set<UserAuthenticationSourceRequest> requests) throws AuthorizationException, AmbariException {

    Integer authenticatedUserId = AuthorizationHelper.getAuthenticatedId();


    for (UserAuthenticationSourceRequest request : requests) {
      String requestedUsername = request.getUsername();

      UserEntity userEntity = users.getUserEntity(requestedUsername);
      if (null == userEntity) {
        continue;
      }

      boolean isSelf = authenticatedUserId.equals(userEntity.getUserId());
      /* **************************************************
       * Ensure that the authenticated user can change the password for the subject user. At least one
       * of the following must be true
       *  * The authenticate user is requesting to change his/her own password for a local authentication source
       *  * The authenticated user has permissions to manage users
       *  ************************************************** */
      if (!isSelf && !AuthorizationHelper.isAuthorized(ResourceType.AMBARI, null, RoleAuthorization.AMBARI_MANAGE_USERS)) {
        throw new AuthorizationException("You are not authorized perform this operation");
      }

      UserAuthenticationEntity userAuthenticationEntity = null;
      Long sourceId = request.getSourceId();

      if (sourceId != null) {
        List<UserAuthenticationEntity> authenticationEntities = userEntity.getAuthenticationEntities();
        // Find the relevant authentication entity...
        for (UserAuthenticationEntity authenticationEntity : authenticationEntities) {
          if (sourceId.equals(authenticationEntity.getUserAuthenticationId())) {
            userAuthenticationEntity = authenticationEntity;
            break;
          }
        }
      }

      if (userAuthenticationEntity == null) {
        // The requested authentication record was not found....
        throw new ResourceNotFoundException("The requested authentication source was not found.");
      }

      // If the authentication_type is set, use it to verify that the found authentication source matches it...
      if ((request.getAuthenticationType() != null) && (request.getAuthenticationType() != userAuthenticationEntity.getAuthenticationType())) {
        throw new ResourceNotFoundException("The requested authentication source was not found - mismatch on authentication type");
      }

      users.modifyAuthentication(userAuthenticationEntity, request.getOldKey(), request.getKey(), isSelf);
    }
  }

  private UserAuthenticationSourceResponse createUserAuthenticationSourceResponse(UserAuthenticationEntity entity) {
    return new UserAuthenticationSourceResponse(entity.getUser().getUserName(),
        entity.getUserAuthenticationId(),
        entity.getAuthenticationType(),
        entity.getAuthenticationKey(),
        new Date(entity.getCreateTime()),
        new Date(entity.getUpdateTime()));
  }


  /**
   * Translate the Response into a Resource
   *
   * @param response     {@link PrivilegeResponse}
   * @param requestedIds the relevant request ids
   * @return a resource
   */
  private Resource toResource(UserAuthenticationSourceResponse response, Set<String> requestedIds) {
    final ResourceImpl resource = new ResourceImpl(Resource.Type.UserAuthenticationSource);

    setResourceProperty(resource, AUTHENTICATION_USER_NAME_PROPERTY_ID, response.getUserName(), requestedIds);
    setResourceProperty(resource, AUTHENTICATION_AUTHENTICATION_SOURCE_ID_PROPERTY_ID, response.getSourceId(), requestedIds);
    setResourceProperty(resource, AUTHENTICATION_AUTHENTICATION_TYPE_PROPERTY_ID, response.getAuthenticationType().name(), requestedIds);
    setResourceProperty(resource, AUTHENTICATION_CREATED_PROPERTY_ID, response.getCreateTime(), requestedIds);
    setResourceProperty(resource, AUTHENTICATION_UPDATED_PROPERTY_ID, response.getUpdateTime(), requestedIds);

    // NOTE, AUTHENTICATION_KEY_PROPERTY_ID is not being returned here since we don't want to return
    // any sensitive information.  Once set that data should stay internal to Ambari.

    return resource;
  }
}