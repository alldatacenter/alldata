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

package org.apache.ambari.server.view;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.internal.AbstractResourceProvider;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;

/**
 * External view sub-resource provider.
 */
public class ViewExternalSubResourceProvider extends AbstractResourceProvider {

  /**
   * View external sub resource property id constants.
   */
  private static final String VIEW_NAME_PROPERTY_ID     = "view_name";
  private static final String VIEW_VERSION_PROPERTY_ID  = "version";
  private static final String INSTANCE_NAME_PROPERTY_ID = "instance_name";
  private static final String RESOURCE_NAME_PROPERTY_ID = "name";

  /**
   * The resource type.
   */
  private final Resource.Type type;

  /**
   * The names of the external resources for the view.
   */
  private final Set<String> resourceNames = new HashSet<>();

  /**
   * The set of key property ids.
   */
  private final Set<String> pkPropertyIds;

  /**
   * The associated view definition.
   */
  private final ViewEntity viewDefinition;


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a view sub-resource provider.
   *
   * @param type            the resource type
   * @param viewDefinition  the associated view definition
   */
  public ViewExternalSubResourceProvider(Resource.Type type, ViewEntity viewDefinition) {
    super(_getPropertyIds(), _getKeyPropertyIds(type));

    this.type           = type;
    this.pkPropertyIds  = new HashSet<>(getKeyPropertyIds().values());
    this.viewDefinition = viewDefinition;
  }

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException, UnsupportedPropertyException,
      ResourceAlreadyExistsException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Not supported!");
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> resourceSet = new HashSet<>();

    Set<ViewInstanceEntity> instanceDefinitions = new HashSet<>();

    Set<Map<String, Object>> propertyMaps = getPropertyMaps(predicate);
    int size = propertyMaps.size();

    Collection<ViewInstanceEntity> viewInstanceDefinitions = viewDefinition.getInstances();
    if (size == 0) {
      instanceDefinitions.addAll(viewInstanceDefinitions);
    } else {
      for (Map<String, Object> propertyMap : propertyMaps) {
        String instanceName = (String) propertyMap.get(INSTANCE_NAME_PROPERTY_ID);
        if (instanceName == null) {
          instanceDefinitions.addAll(viewInstanceDefinitions);
          break;
        } else {
          instanceDefinitions.add(viewDefinition.getInstanceDefinition(instanceName));
        }
      }
    }

    for (ViewInstanceEntity viewInstanceDefinition : instanceDefinitions) {
      for (String resourceName : resourceNames) {
        ResourceImpl resource = new ResourceImpl(type);
        resource.setProperty(VIEW_NAME_PROPERTY_ID, viewDefinition.getCommonName());
        resource.setProperty(VIEW_VERSION_PROPERTY_ID, viewDefinition.getVersion());
        resource.setProperty(INSTANCE_NAME_PROPERTY_ID, viewInstanceDefinition.getName());
        resource.setProperty("name", resourceName);
        resourceSet.add(resource);
      }
    }

    return resourceSet;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Not supported!");
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Not supported!");
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    return Collections.emptySet();
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Register an external sub-resource name.
   *
   * @param resourceName  the resource name
   */
  public void addResourceName(String resourceName) {
    resourceNames.add(resourceName);
  }

  // get the key property ids for the resource
  private static Map<Resource.Type, String> _getKeyPropertyIds(Resource.Type type) {

    Map<Resource.Type, String> keyPropertyIds = new HashMap<>();

    keyPropertyIds.put(Resource.Type.View, VIEW_NAME_PROPERTY_ID);
    keyPropertyIds.put(Resource.Type.ViewVersion, VIEW_VERSION_PROPERTY_ID);
    keyPropertyIds.put(Resource.Type.ViewInstance, INSTANCE_NAME_PROPERTY_ID);
    keyPropertyIds.put(type, RESOURCE_NAME_PROPERTY_ID);

    return keyPropertyIds;
  }

  // get the property ids for the resource
  private static Set<String> _getPropertyIds()  {
    Set<String> propertyIds = new HashSet<>();
    propertyIds.add(INSTANCE_NAME_PROPERTY_ID);
    propertyIds.add(VIEW_NAME_PROPERTY_ID);
    propertyIds.add(VIEW_VERSION_PROPERTY_ID);
    propertyIds.add(RESOURCE_NAME_PROPERTY_ID);

    return propertyIds;
  }
}
