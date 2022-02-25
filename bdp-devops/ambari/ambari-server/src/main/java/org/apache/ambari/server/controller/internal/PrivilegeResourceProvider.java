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
 * See the License for the specific language governing privileges and
 * limitations under the License.
 */

package org.apache.ambari.server.controller.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.GroupDAO;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.PrincipalDAO;
import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.ResourceDAO;
import org.apache.ambari.server.orm.dao.UserDAO;
import org.apache.ambari.server.orm.entities.GroupEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.commons.lang.StringUtils;

/**
 * Abstract resource provider for privilege resources.
 */
public abstract class PrivilegeResourceProvider<T> extends AbstractAuthorizedResourceProvider {

  /**
   * Data access object used to obtain privilege entities.
   */
  private static PrivilegeDAO privilegeDAO;

  /**
   * Data access object used to obtain user entities.
   */
  private static UserDAO userDAO;

  /**
   * Data access object used to obtain group entities.
   */
  private static GroupDAO groupDAO;

  /**
   * Data access object used to obtain principal entities.
   */
  private static PrincipalDAO principalDAO;

  /**
   * Data access object used to obtain permission entities.
   */
  protected static PermissionDAO permissionDAO;

  /**
   * Data access object used to obtain resource entities.
   */
  private static ResourceDAO resourceDAO;

  public static final String PRIVILEGE_INFO = "PrivilegeInfo";

  public static final String PRIVILEGE_ID_PROPERTY_ID    = "privilege_id";
  public static final String PERMISSION_NAME_PROPERTY_ID = "permission_name";
  public static final String PERMISSION_LABEL_PROPERTY_ID = "permission_label";
  public static final String PRINCIPAL_NAME_PROPERTY_ID  = "principal_name";
  public static final String PRINCIPAL_TYPE_PROPERTY_ID  = "principal_type";
  public static final String VERSION_PROPERTY_ID  = "version";
  public static final String TYPE_PROPERTY_ID  = "type";

  public static final String PRIVILEGE_ID = PRIVILEGE_INFO + PropertyHelper.EXTERNAL_PATH_SEP + PRIVILEGE_ID_PROPERTY_ID;
  public static final String PERMISSION_NAME = PRIVILEGE_INFO + PropertyHelper.EXTERNAL_PATH_SEP + PERMISSION_NAME_PROPERTY_ID;
  public static final String PERMISSION_LABEL = PRIVILEGE_INFO + PropertyHelper.EXTERNAL_PATH_SEP + PERMISSION_LABEL_PROPERTY_ID;
  public static final String PRINCIPAL_NAME = PRIVILEGE_INFO + PropertyHelper.EXTERNAL_PATH_SEP + PRINCIPAL_NAME_PROPERTY_ID;
  public static final String PRINCIPAL_TYPE = PRIVILEGE_INFO + PropertyHelper.EXTERNAL_PATH_SEP + PRINCIPAL_TYPE_PROPERTY_ID;

  /**
   * The privilege resource type.
   */
  private final Resource.Type resourceType;


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a privilege resource provider.
   */
  public PrivilegeResourceProvider(Set<String> propertyIds,
                                   Map<Resource.Type, String> keyPropertyIds,
                                   Resource.Type resourceType) {
    super(resourceType, propertyIds, keyPropertyIds);
    this.resourceType = resourceType;
  }


  // ----- PrivilegeResourceProvider ----------------------------------------

  /**
   * Static initialization.
   *
   * @param privDAO  the privilege data access object
   * @param usrDAO   the user data access object
   * @param grpDAO   the group data access object
   * @param prinDAO  the principal data access object
   * @param permDAO  the permission data access object
   * @param resDAO   the resource data access object
   */
  public static void init(PrivilegeDAO privDAO, UserDAO usrDAO, GroupDAO grpDAO, PrincipalDAO prinDAO,
                          PermissionDAO permDAO, ResourceDAO resDAO) {
    privilegeDAO  = privDAO;
    userDAO       = usrDAO;
    groupDAO      = grpDAO;
    principalDAO  = prinDAO;
    permissionDAO = permDAO;
    resourceDAO   = resDAO;
  }

  /**
   * Get the entities for the owning resources from the given properties.
   *
   * @param properties the set of properties
   *
   * @return the entities
   * @throws AmbariException if resource entities were not found
   */
  public abstract Map<Long, T> getResourceEntities(Map<String, Object> properties) throws AmbariException;

  /**
   * Get the id for the resource specified by predicate.
   *
   * @param predicate predicate
   *
   * @return the resource id
   */
  public abstract Long getResourceEntityId(Predicate predicate);


  // ----- ResourceProvider --------------------------------------------------

  @Override
  public RequestStatus createResourcesAuthorized(Request request)
      throws SystemException, UnsupportedPropertyException,
      ResourceAlreadyExistsException, NoSuchParentResourceException {
    for (Map<String, Object> properties : request.getProperties()) {
      createResources(getCreateCommand(properties));
    }
    notifyCreate(resourceType, request);

    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    Set<Resource> resources    = new HashSet<>();
    Set<String>   requestedIds = getRequestPropertyIds(request, predicate);
    Set<Long>     resourceIds  = new HashSet<>();

    Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);

    if (propertyMaps.isEmpty()) {
      propertyMaps.add(Collections.emptyMap());
    }

    for (Map<String, Object> properties : propertyMaps) {
      Map<Long, T> resourceEntities;
      try {
        resourceEntities = getResourceEntities(properties);
      } catch (AmbariException e) {
        throw new SystemException("Could not get resource list from request", e);
      }

      resourceIds.addAll(resourceEntities.keySet());

      Set<PrivilegeEntity> entitySet = new HashSet<>();
      List<PrincipalEntity> userPrincipals = new LinkedList<>();
      List<PrincipalEntity> groupPrincipals = new LinkedList<>();
      List<PrincipalEntity> rolePrincipals = new LinkedList<>();

      List<PrivilegeEntity> entities = privilegeDAO.findAll();

      for(PrivilegeEntity privilegeEntity : entities){
        if (resourceIds.contains(privilegeEntity.getResource().getId())) {
          PrincipalEntity principal = privilegeEntity.getPrincipal();
          String principalType = principal.getPrincipalType().getName();

          entitySet.add(privilegeEntity);

          if(PrincipalTypeEntity.USER_PRINCIPAL_TYPE_NAME.equals(principalType)) {
            userPrincipals.add(principal);
          }
          else if(PrincipalTypeEntity.GROUP_PRINCIPAL_TYPE_NAME.equals(principalType)) {
            groupPrincipals.add(principal);
          }
          else if(PrincipalTypeEntity.ROLE_PRINCIPAL_TYPE_NAME.equals(principalType)) {
            rolePrincipals.add(principal);
          }
        }
      }

      Map<Long, UserEntity> userEntities = new HashMap<>();
      if(!userPrincipals.isEmpty()) {
        List<UserEntity> userList = userDAO.findUsersByPrincipal(userPrincipals);
        for (UserEntity userEntity : userList) {
          userEntities.put(userEntity.getPrincipal().getId(), userEntity);
        }
      }

      Map<Long, GroupEntity> groupEntities = new HashMap<>();
      if(!groupPrincipals.isEmpty()) {
        List<GroupEntity> groupList = groupDAO.findGroupsByPrincipal(groupPrincipals);
        for (GroupEntity groupEntity : groupList) {
          groupEntities.put(groupEntity.getPrincipal().getId(), groupEntity);
        }
      }

      Map<Long, PermissionEntity> roleEntities = new HashMap<>();
      if (!rolePrincipals.isEmpty()){
        List<PermissionEntity> roleList = permissionDAO.findPermissionsByPrincipal(rolePrincipals);
        for (PermissionEntity roleEntity : roleList) {
          roleEntities.put(roleEntity.getPrincipal().getId(), roleEntity);
        }
      }

      for(PrivilegeEntity privilegeEntity : entitySet){
        Resource resource = toResource(privilegeEntity, userEntities, groupEntities, roleEntities, resourceEntities, requestedIds);
        if (resource != null && (predicate == null || predicate.evaluate(resource))) {
          resources.add(resource);
        }
      }
    }

    return resources;
  }

  @Override
  public RequestStatus updateResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    modifyResources(getUpdateCommand(request, predicate));
    notifyUpdate(resourceType, request, predicate);
    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    modifyResources(getDeleteCommand(predicate));
    notifyDelete(resourceType, predicate);
    return getRequestStatus(null);
  }


  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(getKeyPropertyIds().values());
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Check to see if the given privilege entity's permission is allowable for the
   * resource type.
   *
   * @param entity  the privilege entity
   *
   * @throws AmbariException if the the privilege permission is not allowable for the resource type
   */
  protected boolean checkResourceTypes(PrivilegeEntity entity) throws AmbariException {
    Integer resourceType           = entity.getResource().getResourceType().getId();
    Integer permissionResourceType = entity.getPermission().getResourceType().getId();

    return resourceType.equals(permissionResourceType);
  }

  /**
   * Convert the given privilege entity into a Resource.
   *
   * @param privilegeEntity   the privilege entity to be converted
   * @param userEntities      the map of user entities keyed by resource id
   * @param groupEntities     the map of group entities keyed by resource id
   * @param roleEntities      the map of role entities keyed by resource id
   * @param resourceEntities  the map of resource entities keyed by resource id
   * @param requestedIds      the requested property ids
   *
   * @return the resource
   */
  protected Resource toResource(PrivilegeEntity privilegeEntity,
                                Map<Long, UserEntity> userEntities,
                                Map<Long, GroupEntity> groupEntities,
                                Map<Long, PermissionEntity> roleEntities,
                                Map<Long, T> resourceEntities,
                                Set<String> requestedIds) {
    Resource resource = new ResourceImpl(resourceType);

    PrincipalEntity principal = privilegeEntity.getPrincipal();
    String principalTypeName = null;
    String resourcePropertyName = null;

    if(principal != null) {
      PrincipalTypeEntity principalType = principal.getPrincipalType();

      if (principalType != null) {
        Long principalId = principal.getId();

        principalTypeName = principalType.getName();

        if (StringUtils.equalsIgnoreCase(PrincipalTypeEntity.GROUP_PRINCIPAL_TYPE_NAME, principalTypeName)) {
          GroupEntity groupEntity = groupEntities.get(principalId);
          if (groupEntity != null) {
            resourcePropertyName = groupEntity.getGroupName();
          }
        } else if (StringUtils.equalsIgnoreCase(PrincipalTypeEntity.ROLE_PRINCIPAL_TYPE_NAME, principalTypeName)) {
          PermissionEntity roleEntity = roleEntities.get(principalId);
          if (roleEntity != null) {
            resourcePropertyName = roleEntity.getPermissionName();
          }
        } else if (StringUtils.equalsIgnoreCase(PrincipalTypeEntity.USER_PRINCIPAL_TYPE_NAME, principalTypeName)) {
          UserEntity userEntity = userEntities.get(principalId);
          if (userEntity != null) {
            resourcePropertyName = userEntity.getUserName();
          }
        }
      }
    }

    setResourceProperty(resource, PRIVILEGE_ID, privilegeEntity.getId(), requestedIds);
    setResourceProperty(resource, PERMISSION_NAME, privilegeEntity.getPermission().getPermissionName(), requestedIds);
    setResourceProperty(resource, PERMISSION_LABEL, privilegeEntity.getPermission().getPermissionLabel(), requestedIds);
    setResourceProperty(resource, PRINCIPAL_NAME, resourcePropertyName, requestedIds);
    setResourceProperty(resource, PRINCIPAL_TYPE, principalTypeName, requestedIds);

    return resource;
  }

  /**
   * Convert the given map of properties to a privilege entity for the resource
   * identified by the given id.
   *
   * @param properties  the property map
   * @param resourceId  the resource id
   *
   * @return the new privilege entity
   */
  protected PrivilegeEntity toEntity(Map<String, Object> properties, Long resourceId)
      throws AmbariException {
    PrivilegeEntity entity         = new PrivilegeEntity();
    String          permissionName = (String) properties.get(PERMISSION_NAME);
    ResourceEntity  resourceEntity = resourceDAO.findById(resourceId);
    PermissionEntity permission = getPermission(permissionName, resourceEntity);
    if (permission == null) {
      throw new AmbariException("Can't find a permission named " + permissionName +
          " for the resource.");
    }
    entity.setPermission(permission);
    entity.setResource(resourceEntity);

    String principalName = (String) properties.get(PRINCIPAL_NAME);
    String principalType = (String) properties.get(PRINCIPAL_TYPE);
    if (StringUtils.equalsIgnoreCase(PrincipalTypeEntity.GROUP_PRINCIPAL_TYPE_NAME, principalType)) {
      GroupEntity groupEntity = groupDAO.findGroupByName(principalName);
      if (groupEntity != null) {
        entity.setPrincipal(principalDAO.findById(groupEntity.getPrincipal().getId()));
      }
    } else if (StringUtils.equalsIgnoreCase(PrincipalTypeEntity.ROLE_PRINCIPAL_TYPE_NAME, principalType)) {
      PermissionEntity permissionEntity = permissionDAO.findByName(principalName);
      if (permissionEntity != null) {
        entity.setPrincipal(principalDAO.findById(permissionEntity.getPrincipal().getId()));
      }
    } else if (StringUtils.equalsIgnoreCase(PrincipalTypeEntity.USER_PRINCIPAL_TYPE_NAME, principalType)) {
      UserEntity userEntity = userDAO.findUserByName(principalName);
      if (userEntity != null) {
        entity.setPrincipal(principalDAO.findById(userEntity.getPrincipal().getId()));
      }
    } else {
      throw new AmbariException("Unknown principal type " + principalType);
    }
    if (entity.getPrincipal() == null) {
      throw new AmbariException("Could not find " + principalType + " named " + principalName);
    }
    return entity;
  }

  // Get a permission with the given permission name for the given resource.
  protected PermissionEntity getPermission(String permissionName, ResourceEntity resourceEntity)
      throws AmbariException {

    return permissionDAO.findPermissionByNameAndType(permissionName, resourceEntity.getResourceType());
  }

  // Create a create command with the given properties map.
  private Command<Void> getCreateCommand(final Map<String, Object> properties) {
    return new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {

        // for a create there should only be one resource ...
        Set<Long> resourceIds = getResourceEntities(properties).keySet();
        Long      resourceId  = resourceIds.iterator().next();

        PrivilegeEntity entity = toEntity(properties, resourceId);

        if (entity.getPrincipal() == null) {
          throw new AmbariException("Can't find principal " + properties.get(PRINCIPAL_TYPE) +
              " " + properties.get(PRINCIPAL_NAME) + " for privilege.");
        }
        if (privilegeDAO.exists(entity)) {
            throw new DuplicateResourceException("The privilege already exists.");
        }
        if (!checkResourceTypes(entity)) {
          throw new AmbariException("Can't grant " + entity.getPermission().getResourceType().getName() +
              " permission on a " + entity.getResource().getResourceType().getName() + " resource.");
        }
        privilegeDAO.create(entity);
        entity.getPrincipal().getPrivileges().add(entity);
        principalDAO.merge(entity.getPrincipal());
        return null;
      }
    };
  }

  // Create a delete command with the given predicate.
  private Command<Void> getDeleteCommand(final Predicate predicate) {
    return new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        try {
          for (Map<String, Object> resource : getPropertyMaps(predicate)) {
            if (resource.get(PRIVILEGE_ID) == null) {
              throw new AmbariException("Privilege ID should be provided for this request");
            }
            PrivilegeEntity entity = privilegeDAO.findById(Integer.valueOf(resource.get(PRIVILEGE_ID).toString()));
            if (entity != null) {
              if (!checkResourceTypes(entity)) {
                throw new AmbariException("Can't remove " + entity.getPermission().getResourceType().getName() +
                    " permission from a " + entity.getResource().getResourceType().getName() + " resource.");
              }
              entity.getPrincipal().getPrivileges().remove(entity);
              principalDAO.merge(entity.getPrincipal());
              privilegeDAO.remove(entity);
            }
          }
        } catch (Exception e) {
          throw new AmbariException("Caught exception deleting privilege.", e);
        }
        return null;
      }
    };
  }

  private Command<Void> getUpdateCommand(final Request request, final Predicate predicate) {
    return new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        Long resource = null;
        final List<PrivilegeEntity> requiredEntities = new ArrayList<>();
        for (Map<String, Object> properties: request.getProperties()) {
          Set<Long> resourceIds = getResourceEntities(properties).keySet();
          Long      resourceId  = resourceIds.iterator().next();

          if (resource != null && ! resourceId.equals(resource)) {
            throw new AmbariException("Can't update privileges of multiple resources in one request");
          }
          resource = resourceId;

          PrivilegeEntity entity = toEntity(properties, resourceId);
          requiredEntities.add(entity);
        }
        if (resource == null) {
          // request body is empty, use predicate instead
          resource = getResourceEntityId(predicate);
          // if the predicate does not identify a single resource or the resource is not available for update
          if (resource == null) {
            return null;
          }
        }
        final List<PrivilegeEntity> currentPrivileges = privilegeDAO.findByResourceId(resource);

        for (PrivilegeEntity requiredPrivilege: requiredEntities) {
          boolean isInBothLists = false;
          for (PrivilegeEntity currentPrivilege: currentPrivileges) {
            if (requiredPrivilege.getPermission().getPermissionName().equals(currentPrivilege.getPermission().getPermissionName()) &&
                    requiredPrivilege.getPrincipal().getId().equals(currentPrivilege.getPrincipal().getId())) {
              isInBothLists = true;
              break;
            }
          }
          if (!isInBothLists) {
            if (!checkResourceTypes(requiredPrivilege)) {
              throw new AmbariException("Can't grant " + requiredPrivilege.getPermission().getResourceType().getName() +
                  " permission on a " + requiredPrivilege.getResource().getResourceType().getName() + " resource.");
            }

            privilegeDAO.create(requiredPrivilege);
            requiredPrivilege.getPrincipal().getPrivileges().add(requiredPrivilege);
            principalDAO.merge(requiredPrivilege.getPrincipal());
          }
        }
        for (PrivilegeEntity currentPrivilege: currentPrivileges) {
          boolean isInBothLists = false;
          for (PrivilegeEntity requiredPrivilege: requiredEntities) {
            if (requiredPrivilege.getPermission().getPermissionName().equals(currentPrivilege.getPermission().getPermissionName()) &&
                    requiredPrivilege.getPrincipal().getId().equals(currentPrivilege.getPrincipal().getId())) {
              isInBothLists = true;
              break;
            }
          }
          if (!isInBothLists) {
            if (!checkResourceTypes(currentPrivilege)) {
              throw new AmbariException("Can't remove " + currentPrivilege.getPermission().getResourceType().getName() +
                  " permission from a " + currentPrivilege.getResource().getResourceType().getName() + " resource.");
            }
            currentPrivilege.getPrincipal().getPrivileges().remove(currentPrivilege);
            principalDAO.merge(currentPrivilege.getPrincipal());
            privilegeDAO.remove(currentPrivilege);
          }
        }
        return null;
      }
    };
  }
}

