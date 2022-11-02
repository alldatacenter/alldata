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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.PermissionDAO;
import org.apache.ambari.server.orm.dao.RoleAuthorizationDAO;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.RoleAuthorizationEntity;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * A write-only resource provider for securely stored credentials
 */
@StaticallyInject
public class RoleAuthorizationResourceProvider extends ReadOnlyResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(RoleAuthorizationResourceProvider.class);

  // ----- Property ID constants ---------------------------------------------

  public static final String AUTHORIZATION_ID_PROPERTY_ID = PropertyHelper.getPropertyId("AuthorizationInfo", "authorization_id");
  public static final String PERMISSION_ID_PROPERTY_ID = PropertyHelper.getPropertyId("AuthorizationInfo", "permission_id");
  public static final String AUTHORIZATION_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("AuthorizationInfo", "authorization_name");

  private static final Set<String> PK_PROPERTY_IDS;
  private static final Set<String> PROPERTY_IDS;
  private static final Map<Type, String> KEY_PROPERTY_IDS;

  static {
    Set<String> set;
    set = new HashSet<>();
    set.add(AUTHORIZATION_ID_PROPERTY_ID);
    set.add(PERMISSION_ID_PROPERTY_ID);
    PK_PROPERTY_IDS = Collections.unmodifiableSet(set);

    set = new HashSet<>();
    set.add(AUTHORIZATION_ID_PROPERTY_ID);
    set.add(PERMISSION_ID_PROPERTY_ID);
    set.add(AUTHORIZATION_NAME_PROPERTY_ID);
    PROPERTY_IDS = Collections.unmodifiableSet(set);

    HashMap<Type, String> map = new HashMap<>();
    map.put(Type.Permission, PERMISSION_ID_PROPERTY_ID);
    map.put(Type.RoleAuthorization, AUTHORIZATION_ID_PROPERTY_ID);
    KEY_PROPERTY_IDS = Collections.unmodifiableMap(map);
  }

  /**
   * Data access object used to obtain authorization entities.
   */
  @Inject
  private static RoleAuthorizationDAO roleAuthorizationDAO;

  /**
   * Data access object used to obtain permission entities.
   */
  @Inject
  private static PermissionDAO permissionDAO;

  /**
   * Create a new resource provider.
   */
  public RoleAuthorizationResourceProvider(AmbariManagementController managementController) {
    super(Type.RoleAuthorization, PROPERTY_IDS, KEY_PROPERTY_IDS, managementController);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources = new HashSet<>();

    Set<Map<String, Object>> propertyMaps;

    if (predicate == null) {
      // The request must be from /
      propertyMaps = Collections.singleton(Collections.<String, Object>emptyMap());
    } else {
      propertyMaps = getPropertyMaps(predicate);
    }

    if (propertyMaps != null) {
      for (Map<String, Object> propertyMap : propertyMaps) {
        Object object = propertyMap.get(PERMISSION_ID_PROPERTY_ID);
        Collection<RoleAuthorizationEntity> authorizationEntities;
        Integer permissionId;

        if (object instanceof String) {
          try {
            permissionId = Integer.valueOf((String) object);
          } catch (NumberFormatException e) {
            LOG.warn(PERMISSION_ID_PROPERTY_ID + " is not a valid integer value", e);
            throw new NoSuchResourceException("The requested resource doesn't exist: Authorization not found, " + predicate, e);
          }
        } else if (object instanceof Number) {
          permissionId = ((Number) object).intValue();
        } else {
          permissionId = null;
        }

        if (permissionId == null) {
          authorizationEntities = roleAuthorizationDAO.findAll();
        } else {
          PermissionEntity permissionEntity = permissionDAO.findById(permissionId);

          if (permissionEntity == null) {
            authorizationEntities = null;
          } else {
            authorizationEntities = permissionEntity.getAuthorizations();
          }
        }

        if (authorizationEntities != null) {
          String authorizationId = (String) propertyMap.get(AUTHORIZATION_ID_PROPERTY_ID);

          if (!StringUtils.isEmpty(authorizationId)) {
            // Filter the entities
            Iterator<RoleAuthorizationEntity> iterator = authorizationEntities.iterator();
            while (iterator.hasNext()) {
              if (!authorizationId.equals(iterator.next().getAuthorizationId())) {
                iterator.remove();
              }
            }
          }

          for (RoleAuthorizationEntity entity : authorizationEntities) {
            resources.add(toResource(permissionId, entity, requestedIds));
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
   * Creates a new resource from the given RoleAuthorizationEntity and set of requested ids.
   *
   * @param entity       the RoleAuthorizationEntity
   * @param requestedIds the properties to include in the resulting resource instance
   * @return a resource
   */
  private Resource toResource(Integer permissionId, RoleAuthorizationEntity entity, Set<String> requestedIds) {
    Resource resource = new ResourceImpl(Type.RoleAuthorization);
    setResourceProperty(resource, AUTHORIZATION_ID_PROPERTY_ID, entity.getAuthorizationId(), requestedIds);
    if (permissionId != null) {
      setResourceProperty(resource, PERMISSION_ID_PROPERTY_ID, permissionId, requestedIds);
    }
    setResourceProperty(resource, AUTHORIZATION_NAME_PROPERTY_ID, entity.getAuthorizationName(), requestedIds);
    return resource;
  }
}
