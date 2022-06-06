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
import org.apache.ambari.server.controller.RootServiceRequest;
import org.apache.ambari.server.controller.RootServiceResponse;
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

public class RootServiceResourceProvider extends ReadOnlyResourceProvider {

  public static final String RESPONSE_KEY = "RootService";
  public static final String SERVICE_NAME = "service_name";
  public static final String SERVICE_NAME_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + SERVICE_NAME;

  /**
   * The key property ids for a RootService resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Type.RootService, SERVICE_NAME_PROPERTY_ID)
      .build();

  /**
   * The property ids for a RootService resource.
   */
  private static final Set<String> propertyIds = Sets.newHashSet(
      SERVICE_NAME_PROPERTY_ID);

  protected RootServiceResourceProvider(AmbariManagementController managementController) {
    super(Type.RootService, propertyIds, keyPropertyIds, managementController);
  }
  
  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    final Set<RootServiceRequest> requests = new HashSet<>();

    if (predicate == null) {
      requests.add(getRequest(Collections.emptyMap()));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Set<RootServiceResponse> responses = getResources(new Command<Set<RootServiceResponse>>() {
      @Override
      public Set<RootServiceResponse> invoke() throws AmbariException {
        return getManagementController().getRootServices(requests);
      }
    });

    Set<Resource> resources = new HashSet<>();

    for (RootServiceResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.RootService);
      setResourceProperty(resource, SERVICE_NAME_PROPERTY_ID, response.getServiceName(), requestedIds);
      resources.add(resource);
    }

    return resources;
  }

  private RootServiceRequest getRequest(Map<String, Object> properties) {
    return new RootServiceRequest((String) properties.get(SERVICE_NAME_PROPERTY_ID));
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }

}
