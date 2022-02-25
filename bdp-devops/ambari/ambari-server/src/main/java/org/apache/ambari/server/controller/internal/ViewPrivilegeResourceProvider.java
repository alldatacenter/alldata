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

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.entities.GroupEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.view.ViewRegistry;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

/**
 * Resource provider for view privilege resources.
 */
public class ViewPrivilegeResourceProvider extends PrivilegeResourceProvider<ViewInstanceEntity> {

  public static final String VIEW_NAME_PROPERTY_ID = "view_name";
  public static final String VERSION_PROPERTY_ID = "version";
  public static final String INSTANCE_NAME_PROPERTY_ID = "instance_name";

  public static final String VIEW_NAME = PRIVILEGE_INFO + PropertyHelper.EXTERNAL_PATH_SEP + VIEW_NAME_PROPERTY_ID;
  public static final String VERSION = PRIVILEGE_INFO + PropertyHelper.EXTERNAL_PATH_SEP + VERSION_PROPERTY_ID;
  public static final String INSTANCE_NAME = PRIVILEGE_INFO + PropertyHelper.EXTERNAL_PATH_SEP + INSTANCE_NAME_PROPERTY_ID;

  /**
   * The property ids for a privilege resource.
   */
  private static final Set<String> propertyIds = Sets.newHashSet(
      VIEW_NAME,
      VERSION,
      INSTANCE_NAME,
      PRIVILEGE_ID,
      PERMISSION_NAME,
      PERMISSION_LABEL,
      PRINCIPAL_NAME,
      PRINCIPAL_TYPE);

  /**
   * The key property ids for a privilege resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.View, VIEW_NAME)
      .put(Resource.Type.ViewVersion, VERSION)
      .put(Resource.Type.ViewInstance, INSTANCE_NAME)
      .put(Resource.Type.ViewPrivilege, PRIVILEGE_ID)
      .build();

  /**
   * The built-in VIEW.USER permission.
   */
  private final PermissionEntity viewUsePermission;


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct an ViewPrivilegeResourceProvider.
   */
  public ViewPrivilegeResourceProvider() {
    super(propertyIds, keyPropertyIds, Resource.Type.ViewPrivilege);
    viewUsePermission = permissionDAO.findById(PermissionEntity.VIEW_USER_PERMISSION);

    EnumSet<RoleAuthorization> requiredAuthorizations = EnumSet.of(RoleAuthorization.AMBARI_MANAGE_VIEWS);
    setRequiredCreateAuthorizations(requiredAuthorizations);
    setRequiredDeleteAuthorizations(requiredAuthorizations);
    setRequiredGetAuthorizations(requiredAuthorizations);
    setRequiredUpdateAuthorizations(requiredAuthorizations);
  }


  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  public Map<Resource.Type, String> getKeyPropertyIds() {
    return keyPropertyIds;
  }


  // ----- PrivilegeResourceProvider -----------------------------------------

  @Override
  public Map<Long, ViewInstanceEntity> getResourceEntities(Map<String, Object> properties) throws AmbariException {
    ViewRegistry viewRegistry = ViewRegistry.getInstance();

    String viewName     = (String) properties.get(VIEW_NAME);
    String viewVersion  = (String) properties.get(VERSION);
    String instanceName = (String) properties.get(INSTANCE_NAME);

    if (viewName != null && viewVersion != null && instanceName != null) {
      ViewInstanceEntity viewInstanceEntity =
          viewRegistry.getInstanceDefinition(viewName, viewVersion, instanceName);

      if (viewInstanceEntity == null) {
        throw new AmbariException("View instance " + instanceName + " of " + viewName + viewVersion + " was not found");
      }

      ViewEntity view = viewInstanceEntity.getViewEntity();

      return view.isDeployed() ?
          Collections.singletonMap(viewInstanceEntity.getResource().getId(), viewInstanceEntity) :
          Collections.emptyMap();
    }

    Set<ViewEntity> viewEntities = new HashSet<>();

    if (viewVersion != null) {
      ViewEntity viewEntity = viewRegistry.getDefinition(viewName, viewVersion);
      if (viewEntity != null) {
        viewEntities.add(viewEntity);
      }
    } else {
      for (ViewEntity viewEntity : viewRegistry.getDefinitions()) {
        if (viewName == null || viewEntity.getCommonName().equals(viewName)) {
          viewEntities.add(viewEntity);
        }
      }
    }

    Map<Long, ViewInstanceEntity> resourceEntities = new HashMap<>();

    for (ViewEntity viewEntity : viewEntities) {
      if (viewEntity.isDeployed()) {
        for (ViewInstanceEntity viewInstanceEntity : viewEntity.getInstances()) {
          resourceEntities.put(viewInstanceEntity.getResource().getId(), viewInstanceEntity);
        }
      }
    }
    return resourceEntities;
  }

  @Override
  public Long getResourceEntityId(Predicate predicate) {
    final ViewRegistry viewRegistry = ViewRegistry.getInstance();

    final String viewName     = getQueryParameterValue(VIEW_NAME, predicate).toString();
    final String viewVersion  = getQueryParameterValue(VERSION, predicate).toString();
    final String instanceName = getQueryParameterValue(INSTANCE_NAME, predicate).toString();

    final ViewInstanceEntity viewInstanceEntity = viewRegistry.getInstanceDefinition(viewName, viewVersion, instanceName);

    if (viewInstanceEntity != null) {

      ViewEntity view = viewInstanceEntity.getViewEntity();

      return view.isDeployed() ? viewInstanceEntity.getResource().getId() : null;
    }
    return null;
  }


  // ----- helper methods ----------------------------------------------------

  @Override
  protected boolean checkResourceTypes(PrivilegeEntity entity) throws AmbariException {
    return super.checkResourceTypes(entity) ||
        entity.getPermission().getResourceType().getId().equals(ResourceType.VIEW.getId());
  }

  @Override
  protected Resource toResource(PrivilegeEntity privilegeEntity,
                                Map<Long, UserEntity> userEntities,
                                Map<Long, GroupEntity> groupEntities,
                                Map<Long, PermissionEntity> roleEntities,
                                Map<Long, ViewInstanceEntity> resourceEntities,
                                Set<String> requestedIds) {
    Resource resource = super.toResource(privilegeEntity, userEntities, groupEntities, roleEntities, resourceEntities, requestedIds);
    if (resource != null) {

      ViewInstanceEntity viewInstanceEntity = resourceEntities.get(privilegeEntity.getResource().getId());
      ViewEntity         viewEntity         = viewInstanceEntity.getViewEntity();

      if (!viewEntity.isDeployed()) {
        return null;
      }

      setResourceProperty(resource, VIEW_NAME, viewEntity.getCommonName(), requestedIds);
      setResourceProperty(resource, VERSION, viewEntity.getVersion(), requestedIds);
      setResourceProperty(resource, INSTANCE_NAME, viewInstanceEntity.getName(), requestedIds);
    }
    return resource;
  }

  @Override
  protected PermissionEntity getPermission(String permissionName, ResourceEntity resourceEntity) throws AmbariException {
    return (permissionName.equals(PermissionEntity.VIEW_USER_PERMISSION_NAME)) ?
        viewUsePermission : super.getPermission(permissionName, resourceEntity);
  }
}

