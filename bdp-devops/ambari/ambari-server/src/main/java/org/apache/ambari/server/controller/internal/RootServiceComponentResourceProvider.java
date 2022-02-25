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
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootServiceComponentRequest;
import org.apache.ambari.server.controller.RootServiceComponentResponse;
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

public class RootServiceComponentResourceProvider extends ReadOnlyResourceProvider {

  public static final String RESPONSE_KEY = "RootServiceComponents";
  public static final String ALL_PROPERTIES = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "*";

  public static final String SERVICE_NAME = "service_name";
  public static final String COMPONENT_NAME = "component_name";
  public static final String COMPONENT_VERSION = "component_version";
  public static final String PROPERTIES = "properties";
  public static final String SERVER_CLOCK = "server_clock";

  public static final String SERVICE_NAME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + SERVICE_NAME;
  public static final String COMPONENT_NAME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + COMPONENT_NAME;
  public static final String COMPONENT_VERSION_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + COMPONENT_VERSION;
  public static final String PROPERTIES_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + PROPERTIES;
  public static final String SERVER_CLOCK_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + SERVER_CLOCK;

  /**
   * The key property ids for a RootServiceComponent resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Type.RootService, SERVICE_NAME_PROPERTY_ID)
      .put(Type.RootServiceComponent, COMPONENT_NAME_PROPERTY_ID)
      .build();

  /**
   * The property ids for a RootServiceComponent resource.
   */
  private static final Set<String> propertyIds = Sets.newHashSet(
      SERVICE_NAME_PROPERTY_ID,
      COMPONENT_NAME_PROPERTY_ID,
      COMPONENT_VERSION_PROPERTY_ID,
      PROPERTIES_PROPERTY_ID,
      SERVER_CLOCK_PROPERTY_ID);

  protected RootServiceComponentResourceProvider(AmbariManagementController managementController) {
    super(Type.RootServiceComponent, propertyIds, keyPropertyIds, managementController);
  }
  
  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    final Set<RootServiceComponentRequest> requests = new HashSet<>();

    if (predicate == null) {
      requests.add(getRequest(Collections.emptyMap()));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Set<RootServiceComponentResponse> responses = getResources(new Command<Set<RootServiceComponentResponse>>() {
      @Override
      public Set<RootServiceComponentResponse> invoke() throws AmbariException {
        return getManagementController().getRootServiceComponents(requests);
      }
    });

    Set<Resource> resources = new HashSet<>();

    for (RootServiceComponentResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.RootServiceComponent);

      setResourceProperty(resource, SERVICE_NAME_PROPERTY_ID, response.getServiceName(), requestedIds);
      setResourceProperty(resource, COMPONENT_NAME_PROPERTY_ID, response.getComponentName(), requestedIds);
      setResourceProperty(resource, PROPERTIES_PROPERTY_ID, response.getProperties(), requestedIds);
      setResourceProperty(resource, COMPONENT_VERSION_PROPERTY_ID, response.getComponentVersion(), requestedIds);
      
      if (RootComponent.AMBARI_SERVER.name().equals(response.getComponentName())) {
        setResourceProperty(resource, SERVER_CLOCK_PROPERTY_ID, response.getServerClock(), requestedIds);
      }      

      resources.add(resource);
    }

    return resources;
  }

  private RootServiceComponentRequest getRequest(Map<String, Object> properties) {
    return new RootServiceComponentRequest((String) properties.get(SERVICE_NAME_PROPERTY_ID),
                                           (String) properties.get(COMPONENT_NAME_PROPERTY_ID));
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }

}
