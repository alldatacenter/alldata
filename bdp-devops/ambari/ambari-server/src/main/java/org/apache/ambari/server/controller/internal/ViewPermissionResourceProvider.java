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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.ViewPermissionResponse;
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
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.view.ViewRegistry;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

/**
 * Resource provider for custom view permissions.
 */
public class ViewPermissionResourceProvider extends AbstractResourceProvider {

  /**
   * Data access object used to obtain permission entities.
   */
  private static PermissionDAO permissionDAO;

  public static final String PERMISSION_INFO = "PermissionInfo";

  public static final String VIEW_NAME_PROPERTY_ID = "view_name";
  public static final String VERSION_PROPERTY_ID = "version";
  public static final String PERMISSION_ID_PROPERTY_ID = "permission_id";
  public static final String PERMISSION_NAME_PROPERTY_ID = "permission_name";
  public static final String RESOURCE_NAME_PROPERTY_ID = "resource_name";

  public static final String VIEW_NAME = PERMISSION_INFO + PropertyHelper.EXTERNAL_PATH_SEP + VIEW_NAME_PROPERTY_ID;
  public static final String VERSION = PERMISSION_INFO + PropertyHelper.EXTERNAL_PATH_SEP + VERSION_PROPERTY_ID;
  public static final String PERMISSION_ID = PERMISSION_INFO + PropertyHelper.EXTERNAL_PATH_SEP + PERMISSION_ID_PROPERTY_ID;
  public static final String PERMISSION_NAME = PERMISSION_INFO + PropertyHelper.EXTERNAL_PATH_SEP + PERMISSION_NAME_PROPERTY_ID;
  public static final String RESOURCE_NAME = PERMISSION_INFO + PropertyHelper.EXTERNAL_PATH_SEP + RESOURCE_NAME_PROPERTY_ID;

  /**
   * The key property ids for a permission resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.View, VIEW_NAME)
      .put(Resource.Type.ViewVersion, VERSION)
      .put(Resource.Type.ViewPermission, PERMISSION_ID)
      .build();

  /**
   * The property ids for a permission resource.
   */
  private static final Set<String> propertyIds = Sets.newHashSet(
      VIEW_NAME,
      VERSION,
      PERMISSION_ID,
      PERMISSION_NAME,
      RESOURCE_NAME);


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a permission resource provider.
   */
  public ViewPermissionResourceProvider() {
    super(propertyIds, keyPropertyIds);
  }


  // ----- PermissionResourceProvider ----------------------------------------

  /**
   * Static initialization.
   *
   * @param dao  permission data access object
   */
  public static void init(PermissionDAO dao) {
    permissionDAO = dao;
  }


  // ----- ResourceProvider --------------------------------------------------

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException, UnsupportedPropertyException,
      ResourceAlreadyExistsException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    ViewRegistry  viewRegistry = ViewRegistry.getInstance();
    Set<Resource> resources    = new HashSet<>();
    Set<String>   requestedIds = getRequestPropertyIds(request, predicate);

    PermissionEntity viewUsePermission = permissionDAO.findViewUsePermission();
    for (Map<String, Object> propertyMap: getPropertyMaps(predicate)) {
      Object viewName = propertyMap.get(VIEW_NAME);
      Object viewVersion = propertyMap.get(VERSION);
      if (viewName != null && viewVersion != null) {
        ViewEntity viewEntity = viewRegistry.getDefinition(viewName.toString(), viewVersion.toString());

        // do not report permissions for views that are not loaded.
        if (viewEntity.isDeployed()) {
          ViewPermissionResponse viewPermissionResponse = getResponse(viewUsePermission, viewEntity.getResourceType(), viewEntity);
          resources.add(toResource(viewPermissionResponse, requestedIds));
        }
      }
    }

    for(PermissionEntity permissionEntity : permissionDAO.findAll()){
      ResourceTypeEntity resourceType = permissionEntity.getResourceType();

      ViewEntity viewEntity = viewRegistry.getDefinition(resourceType);

      if (viewEntity != null && viewEntity.isDeployed()) {
        ViewPermissionResponse viewPermissionResponse = getResponse(permissionEntity, resourceType, viewEntity);
        resources.add(toResource(viewPermissionResponse, requestedIds));
      }
    }

    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Not supported.");
  }

  @Override
  public Map<Resource.Type, String> getKeyPropertyIds() {
    return keyPropertyIds;
  }


  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Returns response schema instance for REST endpoint /views/{viewName}/versions/{version}/permissions
   * @param entity         permission entity {@link PermissionEntity}
   * @param resourceType   resource type {@link ResourceTypeEntity}
   * @param viewEntity     view entity {@link ViewEntity}
   * @return {@link ViewPermissionResponse}
   */
  private ViewPermissionResponse getResponse(PermissionEntity entity, ResourceTypeEntity resourceType, ViewEntity viewEntity) {

    String viewName = viewEntity.getCommonName();
    String version = viewEntity.getVersion();
    Integer permissionId = entity.getId();
    String permissionName = entity.getPermissionName();
    String resourceName = resourceType.getName();
    ViewPermissionResponse.ViewPermissionInfo viewPermissionInfo  = new ViewPermissionResponse.ViewPermissionInfo(viewName,version,
                                                                    permissionId, permissionName, resourceName);

    return new ViewPermissionResponse(viewPermissionInfo);
  }

  // convert the response to a resource
  private Resource toResource(ViewPermissionResponse viewPermissionResponse, Set<String> requestedIds) {

    Resource resource = new ResourceImpl(Resource.Type.ViewPermission);
    ViewPermissionResponse.ViewPermissionInfo viewPermissionInfo  = viewPermissionResponse.getViewPermissionInfo();
    setResourceProperty(resource, VIEW_NAME, viewPermissionInfo.getViewName(), requestedIds);
    setResourceProperty(resource, VERSION, viewPermissionInfo.getVersion(), requestedIds);

    setResourceProperty(resource, PERMISSION_ID, viewPermissionInfo.getPermissionId(), requestedIds);
    setResourceProperty(resource, PERMISSION_NAME, viewPermissionInfo.getPermissionName(), requestedIds);
    setResourceProperty(resource, RESOURCE_NAME, viewPermissionInfo.getResourceName(), requestedIds);

    return resource;
  }
}
