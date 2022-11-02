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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.ConfigurationResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.RoleAuthorization;

/**
 * Resource provider for configuration resources.
 */
public class ConfigurationResourceProvider extends AbstractControllerResourceProvider {

  private static final Pattern PROPERTIES_ATTRIBUTES_PATTERN = Pattern.compile("^"
      + PROPERTIES_ATTRIBUTES_REGEX);

  public static final String CONFIG = "Config";

  public static final String CLUSTER_NAME_PROPERTY_ID = "cluster_name";
  public static final String STACK_ID_PROPERTY_ID = "stack_id";
  public static final String TYPE_PROPERTY_ID = "type";
  public static final String TAG_PROPERTY_ID = "tag";
  public static final String VERSION_PROPERTY_ID = "version";
  public static final String PROPERTIES_PROPERTY_ID = "properties";
  public static final String PROPERTIES_ATTRIBUTES_PROPERTY_ID = "properties_attributes";

  // ----- Property ID constants ---------------------------------------------
  public static final String CLUSTER_NAME = CONFIG + PropertyHelper.EXTERNAL_PATH_SEP + CLUSTER_NAME_PROPERTY_ID;
  public static final String STACK_ID = CONFIG + PropertyHelper.EXTERNAL_PATH_SEP + STACK_ID_PROPERTY_ID;

  // !!! values are part of query strings and body post, so they
  // don't have defined categories (like Config)
  public static final String TYPE = PropertyHelper.getPropertyId(null, TYPE_PROPERTY_ID);
  public static final String TAG = PropertyHelper.getPropertyId(null, TAG_PROPERTY_ID);
  public static final String VERSION = PropertyHelper.getPropertyId(null, VERSION_PROPERTY_ID);

  /**
   * The property ids for a configuration resource.
   */
  private static final Set<String> PROPERTY_IDS = new HashSet<>();

  /**
   * The key property ids for a configuration resource.
   */
  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<>();

  static {
    // properties
    PROPERTY_IDS.add(CLUSTER_NAME);
    PROPERTY_IDS.add(STACK_ID);
    PROPERTY_IDS.add(TYPE);
    PROPERTY_IDS.add(TAG);
    PROPERTY_IDS.add(VERSION);

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.Configuration, TYPE);
    KEY_PROPERTY_IDS.put(Resource.Type.Cluster, CLUSTER_NAME);
  }

  /**
   * The primary key property ids for the configuration resource type.
   */
  private static final Set<String> pkPropertyIds =
    new HashSet<>(Arrays.asList(new String[]{
      CLUSTER_NAME,
      TYPE}));


  // ----- Constructors ------------------------------------------------------

  /**
   * Constructor
   *
   * @param managementController  the associated management controller
   */
  ConfigurationResourceProvider(AmbariManagementController managementController) {
    super(Resource.Type.Configuration, PROPERTY_IDS, KEY_PROPERTY_IDS, managementController);

    // creating configs requires authorizations based on the type of changes being performed, therefore
    // checks need to be performed inline.
    // update and delete are not supported for configs

    setRequiredGetAuthorizations(EnumSet.of(RoleAuthorization.CLUSTER_VIEW_CONFIGS));
  }


  // ----- ResourceProvider --------------------------------------------------

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException,
             UnsupportedPropertyException,
             ResourceAlreadyExistsException,
             NoSuchParentResourceException {

    for (Map<String, Object> map : request.getProperties()) {
      String cluster = (String) map.get(CLUSTER_NAME);
      String type = (String) map.get(TYPE);
      String tag  = (String) map.get(TAG);

      Map<String, String> configMap = new HashMap<>();
      Map<String, Map<String, String>> configAttributesMap = null;

      for (Entry<String, Object> entry : map.entrySet()) {
        String propertyCategory = PropertyHelper.getPropertyCategory(entry.getKey());
        if (propertyCategory != null && propertyCategory.equals("properties") && null != entry.getValue()) {
          configMap.put(PropertyHelper.getPropertyName(entry.getKey()), entry.getValue().toString());
        }
        if (propertyCategory != null
            && PROPERTIES_ATTRIBUTES_PATTERN.matcher(propertyCategory).matches()
            && null != entry.getValue()) {
          if (null == configAttributesMap) {
            configAttributesMap = new HashMap<>();
          }
          String attributeName = propertyCategory.substring(propertyCategory.lastIndexOf('/') + 1);
          Map<String, String> attributesMap = configAttributesMap.get(attributeName);
          if (attributesMap == null) {
            attributesMap = new HashMap<>();
            configAttributesMap.put(attributeName, attributesMap);
          }
          attributesMap.put(PropertyHelper.getPropertyName(entry.getKey()), entry.getValue().toString());
        }
      }

      final ConfigurationRequest configRequest = new ConfigurationRequest(cluster, type, tag, configMap, configAttributesMap);

      createResources(new Command<Void>() {
        @Override
        public Void invoke() throws AmbariException, AuthorizationException {
          getManagementController().createConfiguration(configRequest);
          return null;
        }
      });

    }
    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResourcesAuthorized(Request request, Predicate predicate)
    throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ConfigurationRequest> requests = new HashSet<>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequest(request, propertyMap));
    }

    Set<ConfigurationResponse> responses = getResources(new Command<Set<ConfigurationResponse>>() {
      @Override
      public Set<ConfigurationResponse> invoke() throws AmbariException {
        return getManagementController().getConfigurations(requests);
      }
    });

    Set<Resource> resources = new HashSet<>();
    for (ConfigurationResponse response : responses) {
      // don't use the StackId object here; we just want the stack ID string
      String stackId = response.getStackId().getStackId();

      Resource resource = new ResourceImpl(Resource.Type.Configuration);
      resource.setProperty(CLUSTER_NAME, response.getClusterName());
      resource.setProperty(STACK_ID, stackId);
      resource.setProperty(TYPE, response.getType());
      resource.setProperty(TAG, response.getVersionTag());
      resource.setProperty(VERSION, response.getVersion());

      if (null != response.getConfigs() && response.getConfigs().size() > 0) {
        Map<String, String> configs = response.getConfigs();

        for (Entry<String, String> entry : configs.entrySet()) {
          String id = PropertyHelper.getPropertyId("properties", entry.getKey());
          resource.setProperty(id, entry.getValue());
        }
      }
      if (null != response.getConfigAttributes() && response.getConfigAttributes().size() > 0) {
        Map<String, Map<String, String>> configAttributes = response.getConfigAttributes();
        for (Entry<String, Map<String, String>> configAttribute : configAttributes.entrySet()) {
          String id = PropertyHelper.getPropertyId("properties_attributes", configAttribute.getKey());
          resource.setProperty(id, configAttribute.getValue());
        }
      }
      resources.add(resource);
    }
    return resources;
  }

  /**
   * Throws an exception, as Configurations cannot be updated.
   */
  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Cannot update a Configuration resource.");
  }

  /**
   * Throws an exception, as Configurations cannot be deleted.
   */
  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Cannot delete a Configuration resource.");
  }

  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    propertyIds = super.checkPropertyIds(propertyIds);

    if (propertyIds.isEmpty()) {
      return propertyIds;
    }
    Set<String> unsupportedProperties = new HashSet<>();

    for (String propertyId : propertyIds) {

      // TODO : hack to allow for inconsistent property names
      // for example, the tag property can come here as Config/tag, /tag or tag
      if (!propertyId.equals("tag") && !propertyId.equals("type") &&
          !propertyId.equals("/tag") && !propertyId.equals("/type") &&
          !propertyId.equals("properties") && !propertyId.equals("properties_attributes")) {

        String propertyCategory = PropertyHelper.getPropertyCategory(propertyId);

        if (propertyCategory == null
            || !(propertyCategory.equals("properties") || PROPERTIES_ATTRIBUTES_PATTERN.matcher(
                propertyCategory).matches())) {
          unsupportedProperties.add(propertyId);
        }
      }
    }
    return unsupportedProperties;
  }

  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }


  // ----- utility methods ---------------------------------------------------

  /**
   * Get a configuration request object from the given map of properties.
   *
   * @param properties  the map of properties
   *
   * @return a configuration request
   */
  private ConfigurationRequest getRequest(Request request, Map<String, Object> properties) {
    String type = (String) properties.get(TYPE);
    String tag  = (String) properties.get(TAG);

    ConfigurationRequest configRequest = new ConfigurationRequest(
        (String) properties.get(CLUSTER_NAME),
        type, tag, new HashMap<>(), new HashMap<>());

    Set<String> requestedIds = request.getPropertyIds();
    if (requestedIds.contains("properties") || requestedIds.contains("*")) {
      configRequest.setIncludeProperties(true);
    }

    return configRequest;
  }
}
