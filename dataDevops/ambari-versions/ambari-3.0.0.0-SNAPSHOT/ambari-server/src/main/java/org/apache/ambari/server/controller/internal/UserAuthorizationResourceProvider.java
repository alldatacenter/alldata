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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.UserAuthorizationResponse;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.RoleAuthorizationEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;

import com.google.inject.Inject;

/**
 * A write-only resource provider for securely stored credentials
 */
@StaticallyInject
public class UserAuthorizationResourceProvider extends ReadOnlyResourceProvider {

  // ----- Property ID constants ---------------------------------------------

  public static final String AUTHORIZATION_ID_PROPERTY_ID = PropertyHelper.getPropertyId("AuthorizationInfo", "authorization_id");
  public static final String USERNAME_PROPERTY_ID = PropertyHelper.getPropertyId("AuthorizationInfo", "user_name");
  public static final String AUTHORIZATION_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("AuthorizationInfo", "authorization_name");
  public static final String AUTHORIZATION_RESOURCE_TYPE_PROPERTY_ID = PropertyHelper.getPropertyId("AuthorizationInfo", "resource_type");
  public static final String AUTHORIZATION_CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("AuthorizationInfo", "cluster_name");
  public static final String AUTHORIZATION_VIEW_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("AuthorizationInfo", "view_name");
  public static final String AUTHORIZATION_VIEW_VERSION_PROPERTY_ID = PropertyHelper.getPropertyId("AuthorizationInfo", "view_version");
  public static final String AUTHORIZATION_VIEW_INSTANCE_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("AuthorizationInfo", "view_instance_name");

  private static final Set<String> PK_PROPERTY_IDS;
  private static final Set<String> PROPERTY_IDS;
  private static final Map<Type, String> KEY_PROPERTY_IDS;

  static {
    Set<String> set;
    set = new HashSet<>();
    set.add(AUTHORIZATION_ID_PROPERTY_ID);
    set.add(USERNAME_PROPERTY_ID);
    set.add(AUTHORIZATION_RESOURCE_TYPE_PROPERTY_ID);
    PK_PROPERTY_IDS = Collections.unmodifiableSet(set);

    set = new HashSet<>();
    set.add(AUTHORIZATION_ID_PROPERTY_ID);
    set.add(USERNAME_PROPERTY_ID);
    set.add(AUTHORIZATION_NAME_PROPERTY_ID);
    set.add(AUTHORIZATION_RESOURCE_TYPE_PROPERTY_ID);
    set.add(AUTHORIZATION_CLUSTER_NAME_PROPERTY_ID);
    set.add(AUTHORIZATION_VIEW_NAME_PROPERTY_ID);
    set.add(AUTHORIZATION_VIEW_VERSION_PROPERTY_ID);
    set.add(AUTHORIZATION_VIEW_INSTANCE_NAME_PROPERTY_ID);
    PROPERTY_IDS = Collections.unmodifiableSet(set);

    HashMap<Type, String> map = new HashMap<>();
    map.put(Type.User, USERNAME_PROPERTY_ID);
    map.put(Type.UserAuthorization, AUTHORIZATION_ID_PROPERTY_ID);
    KEY_PROPERTY_IDS = Collections.unmodifiableMap(map);
  }

  /**
   * Data access object used to obtain permission entities.
   */
  @Inject
  private static PermissionDAO permissionDAO;

  /**
   * Data access object used to obtain resource type entities.
   */
  @Inject
  private static ResourceTypeDAO resourceTypeDAO;

  /**
   * The ClusterController user to get access to other resource providers
   */
  private final ClusterController clusterController;

  /**
   * For testing purposes
   */
  public static void init(PermissionDAO permissionDAO, ResourceTypeDAO resourceTypeDAO) {
    UserAuthorizationResourceProvider.permissionDAO = permissionDAO;
    UserAuthorizationResourceProvider.resourceTypeDAO = resourceTypeDAO;
  }

  /**
   * Create a new resource provider.
   */
  public UserAuthorizationResourceProvider(AmbariManagementController managementController) {
    super(Type.UserAuthorization, PROPERTY_IDS, KEY_PROPERTY_IDS, managementController);

    clusterController = ClusterControllerHelper.getClusterController();
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources = new HashSet<>();

    // Use the UserPrivilegeProvider to get the set of privileges the user has. This set of privileges
    // is used to generate a composite set of authorizations the user has been granted.
    ResourceProvider userPrivilegeProvider = clusterController.ensureResourceProvider(Type.UserPrivilege);

    boolean isUserAdministrator = AuthorizationHelper.isAuthorized(ResourceType.AMBARI, null,
        RoleAuthorization.AMBARI_MANAGE_USERS);

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      String username = (String) propertyMap.get(USERNAME_PROPERTY_ID);

      // Ensure that the authenticated user has authorization to get this information
      if (!isUserAdministrator && !AuthorizationHelper.getAuthenticatedName().equalsIgnoreCase(username)) {
        throw new AuthorizationException();
      }

      Request internalRequest = createUserPrivilegeRequest();
      Predicate internalPredicate = createUserPrivilegePredicate(username);

      Set<Resource> internalResources = userPrivilegeProvider.getResources(internalRequest, internalPredicate);
      if (internalResources != null) {
        for (Resource internalResource : internalResources) {
          String permissionName = (String) internalResource.getPropertyValue(UserPrivilegeResourceProvider.PERMISSION_NAME);
          String resourceType = (String) internalResource.getPropertyValue(UserPrivilegeResourceProvider.TYPE);
          Collection<RoleAuthorizationEntity> authorizationEntities;
          ResourceTypeEntity resourceTypeEntity = resourceTypeDAO.findByName(resourceType);

          if (resourceTypeEntity != null) {
            PermissionEntity permissionEntity = permissionDAO.findPermissionByNameAndType(permissionName, resourceTypeEntity);

            if (permissionEntity == null) {
              authorizationEntities = null;
            } else {
              authorizationEntities = permissionEntity.getAuthorizations();
            }

            if (authorizationEntities != null) {
              // The details about the resource that the user has been granted access to are
              // different depending on the resource type specified in the privilege entity
              if ("VIEW".equals(resourceType)) {
                addViewResources(resources, username, resourceType, internalResource, authorizationEntities, requestedIds);
              } else {
                addClusterResources(resources, username, resourceType, internalResource, authorizationEntities, requestedIds);
              }
            }
          }
        }
      }
    }

    return resources;
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return PK_PROPERTY_IDS;
  }

  /**
   * Create a predicate to use to query for the user's set of privileges
   *
   * @param username the username of the relevant user
   * @return a predicate
   */
  private Predicate createUserPrivilegePredicate(String username) {
    return new EqualsPredicate<>(UserPrivilegeResourceProvider.USER_NAME, username);
  }

  /**
   * Create a request to use to query for the user's set of privileges
   *
   * @return a request
   */
  private Request createUserPrivilegeRequest() {
    Set<String> propertyIds = new HashSet<>();
    propertyIds.add(UserPrivilegeResourceProvider.PRIVILEGE_ID);
    propertyIds.add(UserPrivilegeResourceProvider.PERMISSION_NAME);
    propertyIds.add(UserPrivilegeResourceProvider.TYPE);
    propertyIds.add(UserPrivilegeResourceProvider.CLUSTER_NAME);
    propertyIds.add(UserPrivilegeResourceProvider.VIEW_NAME);
    propertyIds.add(UserPrivilegeResourceProvider.VIEW_VERSION);
    propertyIds.add(UserPrivilegeResourceProvider.INSTANCE_NAME);

    return new RequestImpl(propertyIds, null, null, null);
  }

  /**
   * Creates and adds resources to the results where each resource properly identities the cluster
   * to which the authorization data applies.
   * <p/>
   * Generates an AuthorizationInfo block containing the following fields:
   * <ul>
   * <li>authorization_id</li>
   * <li>authorization_name</li>
   * <li>cluster_name</li>
   * <li>resource_type</li>
   * <li>user_name</li>
   * </ul>
   *
   * @param resources             the set of resources to amend
   * @param username              the username
   * @param resourceType          the resource type (typically "CLUSTER" or "AMBARI")
   * @param privilegeResource     the privilege resource used for retrieving cluster-specific details
   * @param authorizationEntities relevant AuthorizationEntity values for this authorization
   * @param requestedIds          the properties to include in the resulting resource instance
   */
  private void addClusterResources(Set<Resource> resources, String username,
                                   String resourceType, Resource privilegeResource,
                                   Collection<RoleAuthorizationEntity> authorizationEntities,
                                   Set<String> requestedIds) {

    for (RoleAuthorizationEntity entity : authorizationEntities) {
      Resource resource = new ResourceImpl(Type.UserAuthorization);
      String clusterName = (String)privilegeResource.getPropertyValue(UserPrivilegeResourceProvider.CLUSTER_NAME);
      UserAuthorizationResponse userAuthorizationResponse = getResponse(entity.getAuthorizationId(),
        entity.getAuthorizationName(), clusterName, resourceType, username);
      setResourceProperty(resource, AUTHORIZATION_ID_PROPERTY_ID, userAuthorizationResponse.getAuthorizationId(), requestedIds);
      setResourceProperty(resource, USERNAME_PROPERTY_ID, username, requestedIds);
      setResourceProperty(resource, AUTHORIZATION_NAME_PROPERTY_ID, entity.getAuthorizationName(), requestedIds);
      setResourceProperty(resource, AUTHORIZATION_RESOURCE_TYPE_PROPERTY_ID, resourceType, requestedIds);
      setResourceProperty(resource, AUTHORIZATION_CLUSTER_NAME_PROPERTY_ID,
          privilegeResource.getPropertyValue(UserPrivilegeResourceProvider.CLUSTER_NAME),
          requestedIds);

      resources.add(resource);
    }
  }


  /**
   * Creates and adds resources to the results where each resource properly identities the view
   * to which the authorization data applies.
   * <p/>
   * Generates an AuthorizationInfo block containing the following fields:
   * <ul>
   * <li>authorization_id</li>
   * <li>authorization_name</li>
   * <li>resource_type</li>
   * <li>view_name</li>
   * <li>view_version</li>
   * <li>view_instance_name</li>
   * <li>user_name</li>
   * </ul>
   *
   * @param resources             the set of resources to amend
   * @param username              the username
   * @param resourceType          the resource type (typically "VIEW")
   * @param privilegeResource     the privilege resource used for retrieving view-specific details
   * @param authorizationEntities relevant AuthorizationEntity values for this authorization
   * @param requestedIds          the properties to include in the resulting resource instance
   */
  private void addViewResources(Set<Resource> resources, String username,
                                String resourceType, Resource privilegeResource,
                                Collection<RoleAuthorizationEntity> authorizationEntities,
                                Set<String> requestedIds) {
    for (RoleAuthorizationEntity entity : authorizationEntities) {
      Resource resource = new ResourceImpl(Type.UserAuthorization);
      String viewName = (String)privilegeResource.getPropertyValue(UserPrivilegeResourceProvider.VIEW_NAME);
      String viewVersion = (String)privilegeResource.getPropertyValue(UserPrivilegeResourceProvider.VIEW_VERSION);
      String viewInstanceName = (String)privilegeResource.getPropertyValue(UserPrivilegeResourceProvider.INSTANCE_NAME);
      UserAuthorizationResponse userAuthorizationResponse = getResponse(entity.getAuthorizationId(),
        entity.getAuthorizationName(), resourceType, username, viewName, viewVersion, viewInstanceName);
      setResourceProperty(resource, AUTHORIZATION_ID_PROPERTY_ID, userAuthorizationResponse.getAuthorizationId(), requestedIds);
      setResourceProperty(resource, USERNAME_PROPERTY_ID, userAuthorizationResponse.getUserName(), requestedIds);
      setResourceProperty(resource, AUTHORIZATION_NAME_PROPERTY_ID, userAuthorizationResponse.getAuthorizationName(), requestedIds);
      setResourceProperty(resource, AUTHORIZATION_RESOURCE_TYPE_PROPERTY_ID, userAuthorizationResponse.getResourceType(), requestedIds);
      setResourceProperty(resource, AUTHORIZATION_VIEW_NAME_PROPERTY_ID, userAuthorizationResponse.getViewName(),
          requestedIds);
      setResourceProperty(resource, AUTHORIZATION_VIEW_VERSION_PROPERTY_ID, userAuthorizationResponse.getViewVersion(),
          requestedIds);
      setResourceProperty(resource, AUTHORIZATION_VIEW_INSTANCE_NAME_PROPERTY_ID, userAuthorizationResponse.getViewInstanceName(),
          requestedIds);

      resources.add(resource);
    }
  }

  /**
   *  Returns user authorization response instance that represents the response schema
   *  for /users/{userName}/authorizations REST endpoint
   * @param authorizationId     authorization id
   * @param authorizationName   authorization name
   * @param clusterName         cluster name
   * @param resourceType        resource type
   * @param userName            user name
   * @return {@link UserAuthorizationResponse}
   */
  private UserAuthorizationResponse getResponse(String authorizationId, String authorizationName,
                                                                     String clusterName, String resourceType, String userName) {
    return new UserAuthorizationResponse(authorizationId, authorizationName, clusterName, resourceType, userName);

  }

  /**
   * Returns user authorization response instance that represents the response schema
   * for /users/{userName}/authorizations REST endpoint
   * @param authorizationId      authorization id
   * @param authorizationName    authorization name
   * @param resourceType         resource type
   * @param userName             user name
   * @param viewName             view name
   * @param viewVersion          view version
   * @param viewInstanceName     view instance name
   * @return  {@link UserAuthorizationResponse}
   */
  private UserAuthorizationResponse getResponse(String authorizationId, String authorizationName,
                                                                     String resourceType, String userName, String viewName,
                                                                     String viewVersion, String viewInstanceName) {
    return new UserAuthorizationResponse(authorizationId, authorizationName, resourceType, userName, viewName, viewVersion, viewInstanceName);

  }
}
