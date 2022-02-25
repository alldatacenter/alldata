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
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ExtensionVersionRequest;
import org.apache.ambari.server.controller.ExtensionVersionResponse;
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

/**
 * An extension version is like a stack version but it contains custom services.  Linking an extension
 * version to the current stack version allows the cluster to install the custom services contained in
 * the extension version.
 */
@StaticallyInject
public class ExtensionVersionResourceProvider extends ReadOnlyResourceProvider {

  public static final String EXTENSION_VERSION_PROPERTY_ID     = PropertyHelper.getPropertyId("Versions", "extension_version");
  public static final String EXTENSION_NAME_PROPERTY_ID        = PropertyHelper.getPropertyId("Versions", "extension_name");
  public static final String EXTENSION_VALID_PROPERTY_ID      = PropertyHelper.getPropertyId("Versions", "valid");
  public static final String EXTENSION_ERROR_SET      = PropertyHelper.getPropertyId("Versions", "extension-errors");
  public static final String EXTENSION_PARENT_PROPERTY_ID      = PropertyHelper.getPropertyId("Versions", "parent_extension_version");

  /**
   * The key property ids for a ExtensionVersion resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Type.Extension, EXTENSION_NAME_PROPERTY_ID)
      .put(Type.ExtensionVersion, EXTENSION_VERSION_PROPERTY_ID)
      .build();

  /**
   * The property ids for a ExtensionVersion resource.
   */
  private static final Set<String> propertyIds = Sets.newHashSet(
      EXTENSION_VERSION_PROPERTY_ID,
      EXTENSION_NAME_PROPERTY_ID,
      EXTENSION_VALID_PROPERTY_ID,
      EXTENSION_ERROR_SET,
      EXTENSION_PARENT_PROPERTY_ID);

  protected ExtensionVersionResourceProvider(
      AmbariManagementController managementController) {
    super(Type.ExtensionVersion, propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    final Set<ExtensionVersionRequest> requests = new HashSet<>();

    if (predicate == null) {
      requests.add(getRequest(Collections.emptyMap()));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Set<ExtensionVersionResponse> responses = getResources(new Command<Set<ExtensionVersionResponse>>() {
      @Override
      public Set<ExtensionVersionResponse> invoke() throws AmbariException {
        return getManagementController().getExtensionVersions(requests);
      }
    });

    Set<Resource> resources = new HashSet<>();

    for (ExtensionVersionResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.ExtensionVersion);

      setResourceProperty(resource, EXTENSION_NAME_PROPERTY_ID,
          response.getExtensionName(), requestedIds);

      setResourceProperty(resource, EXTENSION_VERSION_PROPERTY_ID,
          response.getExtensionVersion(), requestedIds);

      setResourceProperty(resource, EXTENSION_VALID_PROPERTY_ID,
          response.isValid(), requestedIds);

      setResourceProperty(resource, EXTENSION_ERROR_SET,
          response.getErrors(), requestedIds);

      setResourceProperty(resource, EXTENSION_PARENT_PROPERTY_ID,
        response.getParentVersion(), requestedIds);

      resources.add(resource);
    }

    return resources;
  }

  private ExtensionVersionRequest getRequest(Map<String, Object> properties) {
    return new ExtensionVersionRequest(
        (String) properties.get(EXTENSION_NAME_PROPERTY_ID),
        (String) properties.get(EXTENSION_VERSION_PROPERTY_ID));
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }

}
