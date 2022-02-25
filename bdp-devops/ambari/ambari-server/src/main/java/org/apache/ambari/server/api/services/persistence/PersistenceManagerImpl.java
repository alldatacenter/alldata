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

package org.apache.ambari.server.api.services.persistence;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.NamedPropertySet;
import org.apache.ambari.server.api.services.RequestBody;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.Schema;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;

/**
 * Persistence Manager implementation.
 */
public class PersistenceManagerImpl implements PersistenceManager {

  /**
   * Cluster Controller reference.
   */
  private ClusterController m_controller;

  /**
   * Constructor.
   *
   * @param controller  the cluster controller
   */
  public PersistenceManagerImpl(ClusterController controller) {
    m_controller = controller;
  }

  @Override
  public RequestStatus create(ResourceInstance resource, RequestBody requestBody)
          throws UnsupportedPropertyException,
          SystemException,
          ResourceAlreadyExistsException,
          NoSuchParentResourceException {

    if (!AuthorizationHelper.isAuthorized(ResourceType.CLUSTER, null,
      EnumSet.of(RoleAuthorization.CLUSTER_MANAGE_USER_PERSISTED_DATA))) {
      throw new AuthorizationException("The authenticated user does not have authorization " +
        "to create/store user persisted data.");
    }

    if (resource != null) {

      Map<Resource.Type, String> mapResourceIds = resource.getKeyValueMap();
      Resource.Type type = resource.getResourceDefinition().getType();
      Schema schema = m_controller.getSchema(type);

      Set<NamedPropertySet> setProperties = requestBody.getNamedPropertySets();
      if (setProperties.isEmpty()) {
        requestBody.addPropertySet(new NamedPropertySet("", new HashMap<>()));
      }

      for (NamedPropertySet propertySet : setProperties) {
        for (Map.Entry<Resource.Type, String> entry : mapResourceIds.entrySet()) {
          Map<String, Object> mapProperties = propertySet.getProperties();
          String property = schema.getKeyPropertyId(entry.getKey());
          if (!mapProperties.containsKey(property)) {
            mapProperties.put(property, entry.getValue());
          }
        }
      }
      return m_controller.createResources(type, createControllerRequest(requestBody));
    } else {
      throw new NoSuchParentResourceException("Resource is null");
    }
  }

  @Override
  public RequestStatus update(ResourceInstance resource, RequestBody requestBody)
      throws UnsupportedPropertyException, SystemException, NoSuchParentResourceException, NoSuchResourceException {

    Map<Resource.Type, String> mapResourceIds = resource.getKeyValueMap();
    Resource.Type type = resource.getResourceDefinition().getType();
    Schema schema = m_controller.getSchema(type);

    Set<NamedPropertySet> setProperties = requestBody.getNamedPropertySets();

    for (NamedPropertySet propertySet : setProperties) {
      for (Map.Entry<Resource.Type, String> entry : mapResourceIds.entrySet()) {
        if (entry.getValue() != null) {
          Map<String, Object> mapProperties = propertySet.getProperties();
          String property = schema.getKeyPropertyId(entry.getKey());
          if (!mapProperties.containsKey(property)) {
            mapProperties.put(property, entry.getValue());
          }
        }
      }
    }

    return m_controller.updateResources(type, createControllerRequest(requestBody), resource.getQuery().getPredicate());
  }

  @Override
  public RequestStatus delete(ResourceInstance resource, RequestBody requestBody)
      throws UnsupportedPropertyException, SystemException, NoSuchParentResourceException, NoSuchResourceException {
    //todo: need to account for multiple resources and user predicate
    return m_controller.deleteResources(resource.getResourceDefinition().getType(),
        createControllerRequest(requestBody), resource.getQuery().getPredicate());

  }

  protected Request createControllerRequest(RequestBody body) {
    return PropertyHelper.getCreateRequest(body.getPropertySets(), body.getRequestInfoProperties());
  }
}
