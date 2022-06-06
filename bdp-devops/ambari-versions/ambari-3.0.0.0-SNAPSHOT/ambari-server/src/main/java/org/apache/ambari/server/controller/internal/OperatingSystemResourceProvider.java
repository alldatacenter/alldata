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
import org.apache.ambari.server.controller.OperatingSystemRequest;
import org.apache.ambari.server.controller.OperatingSystemResponse;
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
import com.google.common.collect.ImmutableSet;

public class OperatingSystemResourceProvider extends ReadOnlyResourceProvider {

  public static final String OPERATING_SYSTEM_STACK_NAME_PROPERTY_ID            = PropertyHelper.getPropertyId("OperatingSystems", "stack_name");
  public static final String OPERATING_SYSTEM_STACK_VERSION_PROPERTY_ID         = PropertyHelper.getPropertyId("OperatingSystems", "stack_version");
  public static final String OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID               = PropertyHelper.getPropertyId("OperatingSystems", "os_type");
  public static final String OPERATING_SYSTEM_REPOSITORY_VERSION_ID_PROPERTY_ID = PropertyHelper.getPropertyId("OperatingSystems", "repository_version_id");
  public static final String OPERATING_SYSTEM_VERSION_DEFINITION_ID_PROPERTY_ID = PropertyHelper.getPropertyId("OperatingSystems", "version_definition_id");
  public static final String OPERATING_SYSTEM_AMBARI_MANAGED_REPOS              = "OperatingSystems/ambari_managed_repositories";

  private static final Set<String> pkPropertyIds = ImmutableSet.of(
      OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID,
      OPERATING_SYSTEM_STACK_NAME_PROPERTY_ID,
      OPERATING_SYSTEM_STACK_VERSION_PROPERTY_ID);

  public static final Set<String> propertyIds = ImmutableSet.of(
      OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID,
      OPERATING_SYSTEM_STACK_NAME_PROPERTY_ID,
      OPERATING_SYSTEM_STACK_VERSION_PROPERTY_ID,
      OPERATING_SYSTEM_REPOSITORY_VERSION_ID_PROPERTY_ID,
      OPERATING_SYSTEM_VERSION_DEFINITION_ID_PROPERTY_ID,
      OPERATING_SYSTEM_AMBARI_MANAGED_REPOS);

  public static final Map<Type, String> keyPropertyIds = ImmutableMap.<Type, String>builder()
    .put(Resource.Type.OperatingSystem, OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID)
    .put(Resource.Type.Stack, OPERATING_SYSTEM_STACK_NAME_PROPERTY_ID)
    .put(Resource.Type.StackVersion, OPERATING_SYSTEM_STACK_VERSION_PROPERTY_ID)
    .put(Resource.Type.RepositoryVersion, OPERATING_SYSTEM_REPOSITORY_VERSION_ID_PROPERTY_ID)
    .put(Resource.Type.CompatibleRepositoryVersion, OPERATING_SYSTEM_REPOSITORY_VERSION_ID_PROPERTY_ID)
    .put(Resource.Type.VersionDefinition, OPERATING_SYSTEM_VERSION_DEFINITION_ID_PROPERTY_ID)
    .build();

  protected OperatingSystemResourceProvider(AmbariManagementController managementController) {
    super(Resource.Type.OperatingSystem, propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    final Set<OperatingSystemRequest> requests = new HashSet<>();

    if (predicate == null) {
      requests.add(getRequest(Collections.emptyMap()));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Set<OperatingSystemResponse> responses = getResources(new Command<Set<OperatingSystemResponse>>() {
      @Override
      public Set<OperatingSystemResponse> invoke() throws AmbariException {
        return getManagementController().getOperatingSystems(requests);
      }
    });

    Set<Resource> resources = new HashSet<>();

    for (OperatingSystemResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.OperatingSystem);

      setResourceProperty(resource, OPERATING_SYSTEM_STACK_NAME_PROPERTY_ID,
          response.getStackName(), requestedIds);

      setResourceProperty(resource, OPERATING_SYSTEM_STACK_VERSION_PROPERTY_ID,
          response.getStackVersion(), requestedIds);

      setResourceProperty(resource, OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID,
          response.getOsType(), requestedIds);

      setResourceProperty(resource, OPERATING_SYSTEM_AMBARI_MANAGED_REPOS, response.isAmbariManagedRepos(),
          requestedIds);

      if (response.getRepositoryVersionId() != null) {
        setResourceProperty(resource, OPERATING_SYSTEM_REPOSITORY_VERSION_ID_PROPERTY_ID,
            response.getRepositoryVersionId(), requestedIds);
      }

      if (response.getVersionDefinitionId() != null) {
        setResourceProperty(resource, OPERATING_SYSTEM_VERSION_DEFINITION_ID_PROPERTY_ID,
            response.getVersionDefinitionId(), requestedIds);
      }

      resources.add(resource);
    }

    return resources;
  }

  private OperatingSystemRequest getRequest(Map<String, Object> properties) {
    final OperatingSystemRequest request = new OperatingSystemRequest(
        (String) properties.get(OPERATING_SYSTEM_STACK_NAME_PROPERTY_ID),
        (String) properties.get(OPERATING_SYSTEM_STACK_VERSION_PROPERTY_ID),
        (String) properties.get(OPERATING_SYSTEM_OS_TYPE_PROPERTY_ID));

    if (properties.containsKey(OPERATING_SYSTEM_REPOSITORY_VERSION_ID_PROPERTY_ID)) {
      request.setRepositoryVersionId(Long.parseLong(properties.get(OPERATING_SYSTEM_REPOSITORY_VERSION_ID_PROPERTY_ID).toString()));
    }

    if (properties.containsKey(OPERATING_SYSTEM_VERSION_DEFINITION_ID_PROPERTY_ID)) {
      request.setVersionDefinitionId(properties.get(OPERATING_SYSTEM_VERSION_DEFINITION_ID_PROPERTY_ID).toString());
    }

    return request;
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

}
