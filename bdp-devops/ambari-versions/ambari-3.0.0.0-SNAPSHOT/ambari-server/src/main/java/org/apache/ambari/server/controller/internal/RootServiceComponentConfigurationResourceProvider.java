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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.RootServiceComponentConfiguration;
import org.apache.ambari.server.api.services.RootServiceComponentConfigurationService;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.utils.SecretReference;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Inject;

public class RootServiceComponentConfigurationResourceProvider extends AbstractAuthorizedResourceProvider {

  static final String RESOURCE_KEY = "Configuration";

  public static final String CONFIGURATION_CATEGORY_PROPERTY_ID = PropertyHelper.getPropertyId(RESOURCE_KEY, "category");
  public static final String CONFIGURATION_PROPERTIES_PROPERTY_ID = PropertyHelper.getPropertyId(RESOURCE_KEY, "properties");
  public static final String CONFIGURATION_PROPERTY_TYPES_PROPERTY_ID = PropertyHelper.getPropertyId(RESOURCE_KEY, "property_types");
  public static final String CONFIGURATION_COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(RESOURCE_KEY, "component_name");
  public static final String CONFIGURATION_SERVICE_NAME_PROPERTY_ID = PropertyHelper.getPropertyId(RESOURCE_KEY, "service_name");

  private static final Set<String> PROPERTIES;

  private static final Map<Resource.Type, String> PK_PROPERTY_MAP;

  private static final Set<String> PK_PROPERTY_IDS;

  static {
    Set<String> set = new HashSet<>();
    set.add(CONFIGURATION_SERVICE_NAME_PROPERTY_ID);
    set.add(CONFIGURATION_COMPONENT_NAME_PROPERTY_ID);
    set.add(CONFIGURATION_CATEGORY_PROPERTY_ID);
    set.add(CONFIGURATION_PROPERTIES_PROPERTY_ID);
    set.add(CONFIGURATION_PROPERTY_TYPES_PROPERTY_ID);

    PROPERTIES = Collections.unmodifiableSet(set);

    Map<Resource.Type, String> map = new HashMap<>();
    map.put(Resource.Type.RootService, CONFIGURATION_SERVICE_NAME_PROPERTY_ID);
    map.put(Resource.Type.RootServiceComponent, CONFIGURATION_COMPONENT_NAME_PROPERTY_ID);
    map.put(Resource.Type.RootServiceComponentConfiguration, CONFIGURATION_CATEGORY_PROPERTY_ID);

    PK_PROPERTY_MAP = Collections.unmodifiableMap(map);
    PK_PROPERTY_IDS = Collections.unmodifiableSet(new HashSet<>(PK_PROPERTY_MAP.values()));
  }

  @Inject
  private RootServiceComponentConfigurationHandlerFactory rootServiceComponentConfigurationHandlerFactory;

  public RootServiceComponentConfigurationResourceProvider() {
    super(Resource.Type.RootServiceComponentConfiguration, PROPERTIES, PK_PROPERTY_MAP);

    Set<RoleAuthorization> authorizations = EnumSet.of(RoleAuthorization.AMBARI_MANAGE_CONFIGURATION);
    setRequiredCreateAuthorizations(authorizations);
    setRequiredDeleteAuthorizations(authorizations);
    setRequiredUpdateAuthorizations(authorizations);
    setRequiredGetAuthorizations(authorizations);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return PK_PROPERTY_IDS;
  }

  @Override
  public RequestStatus createResourcesAuthorized(Request request)
      throws SystemException, UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {

    OperationStatusMetaData operationStatusMetadata = null;

    Map<String, String> requestInfoProperties = request.getRequestInfoProperties();
    if (requestInfoProperties.containsKey(RootServiceComponentConfigurationService.DIRECTIVE_OPERATION)) {
      String operationType = requestInfoProperties.get(RootServiceComponentConfigurationService.DIRECTIVE_OPERATION);
      Map<String, Object> operationParameters = getOperationParameters(requestInfoProperties);

      operationStatusMetadata = performOperation(null, null, null, request.getProperties(),
          false, operationType, operationParameters);
    } else {
      createOrAddProperties(null, null, null, request.getProperties(), true);
    }

    return getRequestStatus(null, null, operationStatusMetadata);
  }

  @Override
  protected Set<Resource> getResourcesAuthorized(Request request, Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    return getResources(new Command<Set<Resource>>() {
      @Override
      public Set<Resource> invoke() throws AmbariException {
        Set<Resource> resources = new HashSet<>();
        Set<String> requestedIds = getRequestPropertyIds(request, predicate);

        if (CollectionUtils.isEmpty(requestedIds)) {
          requestedIds = PROPERTIES;
        }

        if (predicate == null) {
          Set<Resource> _resources;
          try {
            _resources = getConfigurationResources(requestedIds, null);
          } catch (NoSuchResourceException e) {
            throw new AmbariException(e.getMessage(), e);
          }

          if (!CollectionUtils.isEmpty(_resources)) {
            resources.addAll(_resources);
          }
        } else {
          for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
            Set<Resource> _resources;
            try {
              _resources = getConfigurationResources(requestedIds, propertyMap);
            } catch (NoSuchResourceException e) {
              throw new AmbariException(e.getMessage(), e);
            }

            if (!CollectionUtils.isEmpty(_resources)) {
              resources.addAll(_resources);
            }
          }
        }

        return resources;
      }
    });
  }

  @Override
  protected RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate) throws SystemException,
      UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    String serviceName = (String) PredicateHelper.getProperties(predicate).get(CONFIGURATION_SERVICE_NAME_PROPERTY_ID);
    String componentName = (String) PredicateHelper.getProperties(predicate).get(CONFIGURATION_COMPONENT_NAME_PROPERTY_ID);
    String categoryName = (String) PredicateHelper.getProperties(predicate).get(CONFIGURATION_CATEGORY_PROPERTY_ID);

    RootServiceComponentConfigurationHandler handler = rootServiceComponentConfigurationHandlerFactory.getInstance(serviceName, componentName, categoryName);
    if (handler != null) {
      handler.removeComponentConfiguration(categoryName);
    } else {
      throw new SystemException(String.format("Configurations may not be updated for the %s component of the root service %s", componentName, serviceName));
    }

    return getRequestStatus(null);
  }

  @Override
  protected RequestStatus updateResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    String serviceName = (String) PredicateHelper.getProperties(predicate).get(CONFIGURATION_SERVICE_NAME_PROPERTY_ID);
    String componentName = (String) PredicateHelper.getProperties(predicate).get(CONFIGURATION_COMPONENT_NAME_PROPERTY_ID);
    String categoryName = (String) PredicateHelper.getProperties(predicate).get(CONFIGURATION_CATEGORY_PROPERTY_ID);

    OperationStatusMetaData operationStatusMetadata = null;

    Map<String, String> requestInfoProperties = request.getRequestInfoProperties();
    if (requestInfoProperties.containsKey(RootServiceComponentConfigurationService.DIRECTIVE_OPERATION)) {
      String operationType = requestInfoProperties.get(RootServiceComponentConfigurationService.DIRECTIVE_OPERATION);
      Map<String, Object> operationParameters = getOperationParameters(requestInfoProperties);

      operationStatusMetadata = performOperation(serviceName, componentName, categoryName, request.getProperties(),
          true, operationType, operationParameters);
    } else {
      createOrAddProperties(serviceName, componentName, categoryName, request.getProperties(), false);
    }

    return getRequestStatus(null, null, operationStatusMetadata);
  }

  private Resource toResource(String serviceName, String componentName, String categoryName, Map<String, String> properties, Map<String, String> propertyTypes, Set<String> requestedIds) {
    Resource resource = new ResourceImpl(Resource.Type.RootServiceComponentConfiguration);
    setResourceProperty(resource, CONFIGURATION_SERVICE_NAME_PROPERTY_ID, serviceName, requestedIds);
    setResourceProperty(resource, CONFIGURATION_COMPONENT_NAME_PROPERTY_ID, componentName, requestedIds);
    setResourceProperty(resource, CONFIGURATION_CATEGORY_PROPERTY_ID, categoryName, requestedIds);
    setResourceProperty(resource, CONFIGURATION_PROPERTIES_PROPERTY_ID, SecretReference.maskPasswordInPropertyMap(properties), requestedIds);
    setResourceProperty(resource, CONFIGURATION_PROPERTY_TYPES_PROPERTY_ID, propertyTypes, requestedIds);
    return resource;
  }

  /**
   * Retrieves groups of properties from the request data and create or updates them as needed.
   * <p>
   * Each group of properties is expected to have a category (<code>AmbariConfiguration/category</code>)
   * value and one or more property (<code>AmbariConfiguration/properties/property.name</code>) values.
   * If a category cannot be determined from the propery set, the default category value (passed in)
   * is used.  If a default category is set, it is assumed that it was parsed from the request predicate
   * (if available).
   *
   * @param defaultServiceName             the default service name to use if needed
   * @param defaultComponentName           the default component name to use if needed
   * @param defaultCategoryName            the default category to use if needed
   * @param requestProperties              a collection of property maps parsed from the request
   * @param removePropertiesIfNotSpecified <code>true</code> to remove existing properties that have not been specifed in the request;
   *                                       <code>false</code> append or update the existing set of properties with values from the request
   * @throws SystemException if an error occurs saving the configuration data
   */
  private void createOrAddProperties(String defaultServiceName, String defaultComponentName, String defaultCategoryName,
                                     Set<Map<String, Object>> requestProperties, boolean removePropertiesIfNotSpecified)
      throws SystemException {
    // set of resource properties (each entry in the set belongs to a different resource)
    if (requestProperties != null) {
      for (Map<String, Object> resourceProperties : requestProperties) {
        RequestDetails requestDetails = parseProperties(defaultServiceName, defaultComponentName, defaultCategoryName, resourceProperties);

        RootServiceComponentConfigurationHandler handler = rootServiceComponentConfigurationHandlerFactory.getInstance(requestDetails.serviceName, requestDetails.componentName, requestDetails.categoryName);
        if (handler != null) {
          try {
            handler.updateComponentCategory(requestDetails.categoryName, requestDetails.properties, removePropertiesIfNotSpecified);
          } catch (AmbariException | IllegalArgumentException e) {
            throw new SystemException(e.getMessage(), e.getCause());
          }
        } else {
          throw new SystemException(String.format("Configurations may not be updated for the %s component of the root service, %s", requestDetails.serviceName, requestDetails.componentName));
        }
      }
    }
  }

  /**
   * Performs the requested operation on the set of data for the specified configration data.
   *
   * @param defaultServiceName      the default service name to use if needed
   * @param defaultComponentName    the default component name to use if needed
   * @param defaultCategoryName     the default category to use if needed
   * @param requestProperties       a collection of property maps parsed from the request
   * @param mergeExistingProperties <code>true</code> to use the the set of existing properties along with the specified set of properties;
   *                                <code>false</code>  to use set of specified properties only
   * @param operationType           the operation to perform
   * @param operationParameters     parameters to supply the name operation
   * @return an {@link OperationStatusMetaData}
   * @throws SystemException if an error occurs while performing the operation
   */
  private OperationStatusMetaData performOperation(String defaultServiceName, String defaultComponentName, String defaultCategoryName,
                                                   Set<Map<String, Object>> requestProperties, boolean mergeExistingProperties,
                                                   String operationType, Map<String, Object> operationParameters)
      throws SystemException {

    OperationStatusMetaData metaData = new OperationStatusMetaData();

    // set of resource properties (each entry in the set belongs to a different resource)
    if (requestProperties != null) {
      for (Map<String, Object> resourceProperties : requestProperties) {
        RequestDetails requestDetails = parseProperties(defaultServiceName, defaultComponentName, defaultCategoryName, resourceProperties);

        RootServiceComponentConfigurationHandler handler = rootServiceComponentConfigurationHandlerFactory.getInstance(requestDetails.serviceName, requestDetails.componentName, requestDetails.categoryName);
        if (handler != null) {
          RootServiceComponentConfigurationHandler.OperationResult operationResult = handler.performOperation(requestDetails.categoryName, requestDetails.properties, mergeExistingProperties, operationType, operationParameters);
          if (operationResult == null) {
            throw new SystemException(String.format("An unexpected error has occurred while handling an operation for the %s component of the root service, %s", requestDetails.serviceName, requestDetails.componentName));
          }

          metaData.addResult(operationResult.getId(), operationResult.isSuccess(), operationResult.getMessage(), operationResult.getResponse());
        } else {
          throw new SystemException(String.format("Operations may not be performed on configurations for the %s component of the root service, %s", requestDetails.serviceName, requestDetails.componentName));
        }
      }
    }

    return metaData;
  }

  /**
   * Parse the property map from a request into a map of services to components to category names to maps of property names and values.
   *
   * @param defaultServiceName   the default service name to use if one is not found in the map of properties
   * @param defaultComponentName the default component name to use if one is not found in the map of properties
   * @param defaultCategoryName  the default category name to use if one is not found in the map of properties
   * @param resourceProperties   a map of properties from a request item   @return a map of category names to maps of name/value pairs
   * @throws SystemException if an issue with the data is determined
   */
  private RequestDetails parseProperties(String defaultServiceName, String defaultComponentName, String defaultCategoryName, Map<String, Object> resourceProperties) throws SystemException {
    String serviceName = defaultServiceName;
    String componentName = defaultComponentName;
    String categoryName = defaultCategoryName;
    Map<String, String> properties = new HashMap<>();

    for (Map.Entry<String, Object> entry : resourceProperties.entrySet()) {
      String propertyName = entry.getKey();

      if (CONFIGURATION_CATEGORY_PROPERTY_ID.equals(propertyName)) {
        if (entry.getValue() instanceof String) {
          categoryName = (String) entry.getValue();
        }
      } else if (CONFIGURATION_COMPONENT_NAME_PROPERTY_ID.equals(propertyName)) {
        if (entry.getValue() instanceof String) {
          componentName = (String) entry.getValue();
        }
      } else if (CONFIGURATION_SERVICE_NAME_PROPERTY_ID.equals(propertyName)) {
        if (entry.getValue() instanceof String) {
          serviceName = (String) entry.getValue();
        }
      } else {
        String propertyCategory = PropertyHelper.getPropertyCategory(entry.getKey());
        if ((propertyCategory != null) && propertyCategory.equals(CONFIGURATION_PROPERTIES_PROPERTY_ID)) {
          String name = PropertyHelper.getPropertyName(entry.getKey());
          Object value = entry.getValue();
          properties.put(name, (value == null) ? null : value.toString());
        }
      }
    }

    if (StringUtils.isEmpty(serviceName)) {
      throw new SystemException("The service name must be set");
    }

    if (StringUtils.isEmpty(componentName)) {
      throw new SystemException("The component name must be set");
    }

    if (StringUtils.isEmpty(categoryName)) {
      throw new SystemException("The configuration category must be set");
    }

    if (properties.isEmpty()) {
      throw new SystemException("The configuration properties must be set");
    }

    return new RequestDetails(serviceName, componentName, categoryName, properties);
  }

  /**
   * Creates a map of the operation parameters from the data in the request info map.
   * <p>
   * Operation parmaters are set under the "parameters" category.
   *
   * @param requestInfoProperties a map of request info properties
   * @return a map of operation request parameters
   */
  private Map<String, Object> getOperationParameters(Map<String, String> requestInfoProperties) {
    Map<String, Object> operationParameters = new HashMap<>();

    for (Map.Entry<String, String> entry : requestInfoProperties.entrySet()) {
      String propertyCategory = PropertyHelper.getPropertyCategory(entry.getKey());
      if ((propertyCategory != null) && propertyCategory.equals("parameters")) {
        String name = PropertyHelper.getPropertyName(entry.getKey());
        Object value = entry.getValue();
        operationParameters.put(name, (value == null) ? null : value.toString());
      }
    }

    return operationParameters;
  }


  /**
   * Retrieves the requested configuration resources
   *
   * @param requestedIds the requested properties ids
   * @param propertyMap  the request properties
   * @return a set of resources built from the found data
   * @throws NoSuchResourceException if the requested resource was not found
   */
  private Set<Resource> getConfigurationResources(Set<String> requestedIds, Map<String, Object> propertyMap) throws NoSuchResourceException {
    Set<Resource> resources = new HashSet<>();

    String serviceName = getStringProperty(propertyMap, CONFIGURATION_SERVICE_NAME_PROPERTY_ID);
    String componentName = getStringProperty(propertyMap, CONFIGURATION_COMPONENT_NAME_PROPERTY_ID);
    String categoryName = getStringProperty(propertyMap, CONFIGURATION_CATEGORY_PROPERTY_ID);

    RootServiceComponentConfigurationHandler handler = rootServiceComponentConfigurationHandlerFactory.getInstance(serviceName, componentName, categoryName);

    if (handler != null) {
      Map<String, RootServiceComponentConfiguration> configurations = handler.getComponentConfigurations(categoryName);

      if (configurations == null) {
        throw new NoSuchResourceException(categoryName);
      } else {
        for (Map.Entry<String, RootServiceComponentConfiguration> entry : configurations.entrySet()) {
          resources.add(toResource(serviceName, componentName, entry.getKey(), entry.getValue().getProperties(), entry.getValue().getPropertyTypes(), requestedIds));
        }
      }
    }

    return resources;
  }

  private String getStringProperty(Map<String, Object> propertyMap, String propertyId) {
    String value = null;

    if (propertyMap != null) {
      Object o = propertyMap.get(propertyId);
      if (o instanceof String) {
        value = (String) o;
      }
    }

    return value;
  }

  /**
   * RequestDetails is a container for details parsed from the request.
   */
  private class RequestDetails {
    final String serviceName;
    final String componentName;
    final String categoryName;
    final Map<String, String> properties;

    private RequestDetails(String serviceName, String componentName, String categoryName, Map<String, String> properties) {
      this.serviceName = serviceName;
      this.componentName = componentName;
      this.categoryName = categoryName;
      this.properties = properties;
    }
  }
}

