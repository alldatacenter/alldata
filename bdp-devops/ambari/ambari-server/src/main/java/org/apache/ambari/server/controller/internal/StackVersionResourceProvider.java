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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.StackVersionRequest;
import org.apache.ambari.server.controller.StackVersionResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.StackEntity;

import com.google.inject.Inject;

@StaticallyInject
public class StackVersionResourceProvider extends ReadOnlyResourceProvider {
  public static final String RESPONSE_KEY = "Versions";
  public static final String ALL_PROPERTIES = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "*";
  public static final String STACK_VERSION_PROPERTY_ID     = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "stack_version";
  public static final String STACK_NAME_PROPERTY_ID        = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "stack_name";
  public static final String STACK_MIN_VERSION_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "min_upgrade_version";
  public static final String STACK_ACTIVE_PROPERTY_ID      = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "active";
  public static final String STACK_VALID_PROPERTY_ID      = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "valid";
  public static final String STACK_ERROR_SET      = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP +"stack-errors";
  public static final String STACK_CONFIG_TYPES            = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "config_types";
  public static final String STACK_PARENT_PROPERTY_ID      = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "parent_stack_version";
  public static final String UPGRADE_PACKS_PROPERTY_ID = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "upgrade_packs";
  public static final String STACK_MIN_JDK     = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "min_jdk";
  public static final String STACK_MAX_JDK     = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "max_jdk";
  public static final String MPACK_RESOURCE_ID     = RESPONSE_KEY + PropertyHelper.EXTERNAL_PATH_SEP + "id";

  public static final Set<String> PROPERTY_IDS = new HashSet<>();

  @Inject
  protected static StackDAO stackDAO;

  private static Set<String> pkPropertyIds = new HashSet<>(
    Arrays.asList(new String[]{STACK_NAME_PROPERTY_ID, STACK_VERSION_PROPERTY_ID, MPACK_RESOURCE_ID}));

  /**
   * The key property ids for a mpack resource.
   */
  public static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<>();

  static {
    // properties
    PROPERTY_IDS.add(MPACK_RESOURCE_ID);
    PROPERTY_IDS.add(STACK_VERSION_PROPERTY_ID);
    PROPERTY_IDS.add(STACK_NAME_PROPERTY_ID);
    PROPERTY_IDS.add(STACK_MIN_VERSION_PROPERTY_ID);
    PROPERTY_IDS.add(STACK_ACTIVE_PROPERTY_ID);
    PROPERTY_IDS.add(STACK_VALID_PROPERTY_ID);
    PROPERTY_IDS.add(STACK_ERROR_SET);
    PROPERTY_IDS.add(STACK_CONFIG_TYPES);
    PROPERTY_IDS.add(STACK_PARENT_PROPERTY_ID);
    PROPERTY_IDS.add(UPGRADE_PACKS_PROPERTY_ID);
    PROPERTY_IDS.add(STACK_MIN_JDK);
    PROPERTY_IDS.add(STACK_MAX_JDK);

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.Mpack, MPACK_RESOURCE_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.Stack, STACK_NAME_PROPERTY_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.StackVersion, STACK_VERSION_PROPERTY_ID);

  }

  StackVersionResourceProvider(AmbariManagementController controller) {
    super(Resource.Type.StackVersion, PROPERTY_IDS, KEY_PROPERTY_IDS, controller);
  }


  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    final Set<StackVersionRequest> requests = new HashSet<>();
    Set<Resource> resources = new HashSet<>();

    if (predicate == null) {
      requests.add(getRequest(Collections.emptyMap()));
    } else {
      Map<String, Object> propertyMap = new HashMap<>(PredicateHelper.getProperties(predicate));
      if (propertyMap.containsKey(MPACK_RESOURCE_ID)) {
        Resource resource = new ResourceImpl(Resource.Type.StackVersion);
        Long mpackId = Long.valueOf((String) propertyMap.get(MPACK_RESOURCE_ID));
        StackEntity stackEntity = stackDAO.findByMpack(mpackId);
        requests.add(new StackVersionRequest(stackEntity.getStackName(), stackEntity.getStackVersion()));
        resource.setProperty(STACK_NAME_PROPERTY_ID,
                (String)stackEntity.getStackName());

        resource.setProperty(STACK_VERSION_PROPERTY_ID,
                (String)stackEntity.getStackVersion());

        resource.setProperty(MPACK_RESOURCE_ID, mpackId);

        resources.add(resource);

      } else {
        for (Map<String, Object> propertyMap1:
             getPropertyMaps(predicate)) {
          requests.add(getRequest(propertyMap1));
        }

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Set<StackVersionResponse> responses = getResources(new Command<Set<StackVersionResponse>>() {
      @Override
      public Set<StackVersionResponse> invoke() throws AmbariException {
        return getManagementController().getStackVersions(requests);
      }
    });


    for (StackVersionResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.StackVersion);

      setResourceProperty(resource, STACK_NAME_PROPERTY_ID,
          response.getStackName(), requestedIds);

      setResourceProperty(resource, STACK_VERSION_PROPERTY_ID,
          response.getStackVersion(), requestedIds);

      setResourceProperty(resource, STACK_ACTIVE_PROPERTY_ID,
          response.isActive(), requestedIds);

      setResourceProperty(resource, STACK_VALID_PROPERTY_ID,
          response.isValid(), requestedIds);

      setResourceProperty(resource, STACK_ERROR_SET,
          response.getErrors(), requestedIds);

      setResourceProperty(resource, STACK_PARENT_PROPERTY_ID,
        response.getParentVersion(), requestedIds);

      setResourceProperty(resource, STACK_CONFIG_TYPES,
          response.getConfigTypes(), requestedIds);

      setResourceProperty(resource, UPGRADE_PACKS_PROPERTY_ID,
          response.getUpgradePacks(), requestedIds);

      setResourceProperty(resource, STACK_MIN_JDK,
              response.getMinJdk(), requestedIds);

      setResourceProperty(resource, STACK_MAX_JDK,
              response.getMaxJdk(), requestedIds);

        resources.add(resource);
      }
      }
    }

      return resources;
    }

  private StackVersionRequest getRequest(Map<String, Object> properties) {
    return new StackVersionRequest(
        (String) properties.get(STACK_NAME_PROPERTY_ID),
        (String) properties.get(STACK_VERSION_PROPERTY_ID));
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }

}
