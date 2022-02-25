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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.StackConfigurationDependencyRequest;
import org.apache.ambari.server.controller.StackConfigurationDependencyResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

public class StackConfigurationDependencyResourceProvider extends
    ReadOnlyResourceProvider {

  public static final String STACK_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackConfigurationDependency", "stack_name");

  public static final String STACK_VERSION_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackConfigurationDependency", "stack_version");

  public static final String SERVICE_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackConfigurationDependency", "service_name");

  public static final String PROPERTY_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackConfigurationDependency", "property_name");

  public static final String DEPENDENCY_NAME_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackConfigurationDependency", "dependency_name");

  public static final String DEPENDENCY_TYPE_PROPERTY_ID = PropertyHelper
      .getPropertyId("StackConfigurationDependency", "dependency_type");

  /**
   * The key property ids for a StackConfigurationDependency resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Type.Stack, STACK_NAME_PROPERTY_ID)
      .put(Type.StackVersion, STACK_VERSION_PROPERTY_ID)
      .put(Type.StackService, SERVICE_NAME_PROPERTY_ID)
      .put(Type.StackConfiguration, PROPERTY_NAME_PROPERTY_ID)
      .put(Type.StackLevelConfiguration, PROPERTY_NAME_PROPERTY_ID)
      .put(Type.StackConfigurationDependency, DEPENDENCY_NAME_PROPERTY_ID)
      .build();

  /**
   * The property ids for a StackConfigurationDependency resource.
   */
  private static final Set<String> propertyIds = Sets.newHashSet(
      STACK_NAME_PROPERTY_ID,
      STACK_VERSION_PROPERTY_ID,
      SERVICE_NAME_PROPERTY_ID,
      PROPERTY_NAME_PROPERTY_ID,
      DEPENDENCY_NAME_PROPERTY_ID,
      DEPENDENCY_TYPE_PROPERTY_ID);

  protected StackConfigurationDependencyResourceProvider(AmbariManagementController managementController) {
    super(Type.StackConfigurationDependency, propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public Set<Resource> getResources(final Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    final Set<StackConfigurationDependencyRequest> requests =
      new HashSet<>();

    if (predicate == null) {
      requests.add(getRequest(Collections.emptyMap()));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Set<StackConfigurationDependencyResponse> responses = getResources(new Command<Set<StackConfigurationDependencyResponse>>() {
      @Override
      public Set<StackConfigurationDependencyResponse> invoke() throws AmbariException {
        return getManagementController().getStackConfigurationDependencies(requests);
      }
    });

    Set<Resource> resources = new HashSet<>();

    for (StackConfigurationDependencyResponse response : responses) {
      Resource resource = new ResourceImpl(Type.StackConfigurationDependency);

      setResourceProperty(resource, STACK_NAME_PROPERTY_ID,
          response.getStackName(), requestedIds);

      setResourceProperty(resource, STACK_VERSION_PROPERTY_ID,
          response.getStackVersion(), requestedIds);

      setResourceProperty(resource, SERVICE_NAME_PROPERTY_ID,
          response.getServiceName(), requestedIds);

      setResourceProperty(resource, PROPERTY_NAME_PROPERTY_ID,
          response.getPropertyName(), requestedIds);

      setResourceProperty(resource, DEPENDENCY_NAME_PROPERTY_ID,
          response.getDependencyName(), requestedIds);

      setResourceProperty(resource, DEPENDENCY_TYPE_PROPERTY_ID,
          response.getDependencyType(), requestedIds);


      resources.add(resource);
    }

    return resources;
  }

  private StackConfigurationDependencyRequest getRequest(Map<String, Object> properties) {
    return new StackConfigurationDependencyRequest(
        (String) properties.get(STACK_NAME_PROPERTY_ID),
        (String) properties.get(STACK_VERSION_PROPERTY_ID),
        (String) properties.get(SERVICE_NAME_PROPERTY_ID),
        (String) properties.get(PROPERTY_NAME_PROPERTY_ID),
        (String) properties.get(DEPENDENCY_NAME_PROPERTY_ID));
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }

}
