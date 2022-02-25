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

import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.entities.PermissionEntity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

/**
 * Resource provider for permission instances.
 */
public class PermissionResourceProvider extends AbstractResourceProvider {

  /**
   * Data access object used to obtain permission entities.
   */
  private static PermissionDAO permissionDAO;

  /**
   * Permission property id constants.
   */
  public static final String PERMISSION_ID_PROPERTY_ID   = "PermissionInfo/permission_id";
  public static final String PERMISSION_NAME_PROPERTY_ID = "PermissionInfo/permission_name";
  public static final String PERMISSION_LABEL_PROPERTY_ID = "PermissionInfo/permission_label";
  public static final String RESOURCE_NAME_PROPERTY_ID   = "PermissionInfo/resource_name";
  public static final String SORT_ORDER_PROPERTY_ID   = "PermissionInfo/sort_order";


  /**
   * The key property ids for a permission resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.Permission, PERMISSION_ID_PROPERTY_ID)
      .build();

  /**
   * The property ids for a permission resource.
   */
  private static final Set<String> propertyIds = Sets.newHashSet(
      PERMISSION_ID_PROPERTY_ID,
      PERMISSION_NAME_PROPERTY_ID,
      PERMISSION_LABEL_PROPERTY_ID,
      RESOURCE_NAME_PROPERTY_ID,
      SORT_ORDER_PROPERTY_ID);


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a permission resource provider.
   */
  public PermissionResourceProvider() {
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

    Set<Resource> resources    = new HashSet<>();
    Set<String>   requestedIds = getRequestPropertyIds(request, predicate);

    for(PermissionEntity permissionEntity : permissionDAO.findAll()){

      resources.add(toResource(permissionEntity, requestedIds));
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

  // convert the given permission entity to a resource
  private Resource toResource(PermissionEntity entity, Set<String> requestedIds) {
    Resource resource = new ResourceImpl(Resource.Type.Permission);

    setResourceProperty(resource, PERMISSION_ID_PROPERTY_ID, entity.getId(), requestedIds);
    setResourceProperty(resource, PERMISSION_NAME_PROPERTY_ID, entity.getPermissionName(), requestedIds);
    setResourceProperty(resource, PERMISSION_LABEL_PROPERTY_ID, entity.getPermissionLabel(), requestedIds);
    setResourceProperty(resource, RESOURCE_NAME_PROPERTY_ID, entity.getResourceType().getName(), requestedIds);
    setResourceProperty(resource, SORT_ORDER_PROPERTY_ID, entity.getSortOrder(), requestedIds);

    return resource;
  }
}
