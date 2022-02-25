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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.predicate.ArrayPredicate;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.RequestStatusMetaData;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.utils.RetryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract resource provider implementation that maps to an Ambari management controller.
 */
public abstract class AbstractResourceProvider extends BaseProvider implements ResourceProvider, ObservableResourceProvider {

  /**
   * Key property mapping by resource type.
   */
  protected final Map<Resource.Type, String> keyPropertyIds;

  /**
   * Observers of this observable resource provider.
   */
  private final Set<ResourceProviderObserver> observers = new HashSet<>();

  protected static final Logger LOG = LoggerFactory.getLogger(AbstractResourceProvider.class);
  protected final static String PROPERTIES_ATTRIBUTES_REGEX = "properties_attributes/[a-zA-Z][a-zA-Z._-]*$";
  private static final Pattern propertiesAttributesPattern = Pattern.compile(".*/" + PROPERTIES_ATTRIBUTES_REGEX);


  // ----- Constructors ------------------------------------------------------

  /**
   * Create a  new resource provider.
   *
   * @param propertyIds     the property ids
   * @param keyPropertyIds  the key property ids
   */
  protected AbstractResourceProvider(Set<String> propertyIds,
                                     Map<Resource.Type, String> keyPropertyIds) {
    super(propertyIds);
    this.keyPropertyIds = keyPropertyIds;
  }


  // ----- ResourceProvider --------------------------------------------------

  @Override
  public Map<Resource.Type, String> getKeyPropertyIds() {
    return keyPropertyIds;
  }


  // ----- ObservableResourceProvider ----------------------------------------

  @Override
  public void updateObservers(ResourceProviderEvent event) {
    for (ResourceProviderObserver observer : observers) {
      observer.update(event);
    }
  }

  @Override
  public void addObserver(ResourceProviderObserver observer) {
    observers.add(observer);
  }


  // ----- utility methods ---------------------------------------------------

  /**
   * Get the set of property ids that uniquely identify the resources
   * of this provider.
   *
   * @return the set of primary key properties
   */
  protected abstract Set<String> getPKPropertyIds();

  /**
   * Notify all listeners of a creation event.
   *
   * @param type     the type of the resources being created
   * @param request  the request used to create the resources
   */
  protected void notifyCreate(Resource.Type type, Request request) {
    updateObservers(new ResourceProviderEvent(type, ResourceProviderEvent.Type.Create, request, null));
  }

  /**
   * Notify all listeners of a update event.
   *
   * @param type       the type of the resources being updated
   * @param request    the request used to update the resources
   * @param predicate  the predicate used to update the resources
   */
  protected void notifyUpdate(Resource.Type type, Request request, Predicate predicate) {
    updateObservers(new ResourceProviderEvent(type, ResourceProviderEvent.Type.Update, request, predicate));
  }

  /**
   * Notify all listeners of a delete event.
   *
   * @param type       the type of the resources being deleted
   * @param predicate  the predicate used to delete the resources
   */
  protected void notifyDelete(Resource.Type type, Predicate predicate) {
    updateObservers(new ResourceProviderEvent(type, ResourceProviderEvent.Type.Delete, null, predicate));
  }

  /**
   * Get a set of properties from the given predicate.  The returned set of
   * property/value mappings is required to generate delete or get requests
   * to the back end which does not deal with predicates.  Note that the
   * single predicate can result in multiple backend requests.
   *
   * @param givenPredicate           the predicate
   *
   * @return the set of properties used to build request objects
   */
  protected Set<Map<String, Object>> getPropertyMaps(Predicate givenPredicate) {

    SimplifyingPredicateVisitor visitor = new SimplifyingPredicateVisitor(this);
    PredicateHelper.visit(givenPredicate, visitor);
    List<Predicate> predicates = visitor.getSimplifiedPredicates();

    Set<Map<String, Object>> propertyMaps = new HashSet<>();

    for (Predicate predicate : predicates) {
      propertyMaps.add(PredicateHelper.getProperties(predicate));
    }
    return propertyMaps;
  }

  /**
   * Get a set of properties from the given property map and predicate.  The
   * returned set of property/value mappings is required to generate update
   * requests to the back end which does not deal with Predicates.  Note that
   * the single property map & predicate can result in multiple backend requests.
   *
   * @param requestPropertyMap  the request update properties; may not be null
   * @param givenPredicate      the predicate
   *
   * @return the set of properties used to build request objects
   */
  protected Set<Map<String, Object>> getPropertyMaps(Map<String, Object> requestPropertyMap, Predicate givenPredicate)
      throws UnsupportedPropertyException, SystemException, NoSuchResourceException, NoSuchParentResourceException {

    Set<Map<String, Object>> propertyMaps = new HashSet<>();

    // If the predicate specifies a unique resource then we can simply return a single
    // property map for the update.  Otherwise we need to do a get with the given predicate
    // to get the set of property maps for the resources that need to be updated.
    if (specifiesUniqueResource(givenPredicate)) {
      Map<String, Object> propertyMap = new HashMap<>(PredicateHelper.getProperties(givenPredicate));
      propertyMap.putAll(requestPropertyMap);
      propertyMaps.add(propertyMap);
    } else {
      for (Resource resource : getResources(givenPredicate)) {
        Map<String, Object> propertyMap = new HashMap<>(PropertyHelper.getProperties(resource));
        propertyMap.putAll(requestPropertyMap);
        propertyMaps.add(propertyMap);
      }
    }
    return propertyMaps;
  }

  /**
   * Get a request status
   *
   * @return the request status
   */
  protected RequestStatus getRequestStatus(RequestStatusResponse response, Set<Resource> associatedResources) {
    return getRequestStatus(response, associatedResources, null);
  }

  protected RequestStatus getRequestStatus(RequestStatusResponse response, Set<Resource> associatedResources, RequestStatusMetaData requestStatusMetaData) {
    if (response != null){
      Resource requestResource = new ResourceImpl(Resource.Type.Request);
      if (response.getMessage() != null){
        requestResource.setProperty(PropertyHelper.getPropertyId("Requests", "message"), response.getMessage());
      }
      requestResource.setProperty(PropertyHelper.getPropertyId("Requests", "id"), response.getRequestId());
      requestResource.setProperty(PropertyHelper.getPropertyId("Requests", "status"), "Accepted");
      return new RequestStatusImpl(requestResource, associatedResources, requestStatusMetaData);
    }
    return new RequestStatusImpl(null, associatedResources, requestStatusMetaData);
  }

  /**
   * Get a request status
   *
   * @return the request status
   */
  protected RequestStatus getRequestStatus(RequestStatusResponse response) {
    return getRequestStatus(response, null) ;
  }

  /**
   * Extracting given query_parameter value from the predicate
   * @param queryParameterId  query parameter id
   * @param predicate         predicate
   * @return the query parameter
   */
  protected static Object getQueryParameterValue(String queryParameterId, Predicate predicate) {

    Object result = null;

    if (predicate instanceof EqualsPredicate) {
      EqualsPredicate equalsPredicate = (EqualsPredicate) predicate;
      if (queryParameterId.equals(equalsPredicate.getPropertyId())) {
        return equalsPredicate.getValue();
      }
    } else if (predicate instanceof ArrayPredicate) {
      ArrayPredicate arrayPredicate  = (ArrayPredicate) predicate;
      for (Predicate predicateItem : arrayPredicate.getPredicates()) {
        result = getQueryParameterValue(queryParameterId, predicateItem);
        if (result != null) {
          return result;
        }
      }

    }
    return result;
  }

  /**
   * Invoke a command against the Ambari backend to create resources and map
   * any {@link AmbariException} to the types appropriate for the
   * {@link ResourceProvider} interface.
   *
   * @param command  the command to invoke
   * @param <T>      the type of the response
   *
   * @return the response
   *
   * @throws SystemException                thrown if a system exception occurred
   * @throws ResourceAlreadyExistsException thrown if a resource already exists
   * @throws NoSuchParentResourceException  thrown if a parent of a resource doesn't exist
   */
  protected <T> T createResources(Command<T> command)
      throws SystemException, ResourceAlreadyExistsException, NoSuchParentResourceException {
    try {
      return invokeWithRetry(command);
    } catch (ParentObjectNotFoundException e) {
      throw new NoSuchParentResourceException(e.getMessage(), e);
    } catch (DuplicateResourceException e) {
      throw new ResourceAlreadyExistsException(e.getMessage());
    } catch (AmbariException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Caught AmbariException when creating a resource", e);
      }
      throw new SystemException("An internal system exception occurred: " + e.getMessage(), e);
    }
  }

  /**
   * Invoke a command against the Ambari backend to get resources and map
   * any {@link AmbariException} to the types appropriate for the
   * {@link ResourceProvider} interface.
   *
   * @param command  the command to invoke
   * @param <T>      the type of the response
   *
   * @return the response
   *
   * @throws SystemException                thrown if a system exception occurred
   * @throws NoSuchParentResourceException  thrown if a parent of a resource doesn't exist
   */
  protected <T> T getResources (Command<T> command)
      throws SystemException, NoSuchResourceException, NoSuchParentResourceException {
    try {
      return command.invoke();
    } catch (ParentObjectNotFoundException e) {
      throw new NoSuchParentResourceException(e.getMessage(), e);
    } catch (ObjectNotFoundException e) {
      throw new NoSuchResourceException("The requested resource doesn't exist: " + e.getMessage(), e);
    }  catch (AmbariException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Caught AmbariException when getting a resource", e);
      }
      throw new SystemException("An internal system exception occurred: " + e.getMessage(), e);
    }
  }

  /**
   * Invoke a command against the Ambari backend to modify resources and map
   * any {@link AmbariException} to the types appropriate for the
   * {@link ResourceProvider} interface.
   *
   * @param command  the command to invoke
   * @param <T>      the type of the response
   *
   * @return the response
   *
   * @throws SystemException                thrown if a system exception occurred
   * @throws NoSuchParentResourceException  thrown if a parent of a resource doesn't exist
   */
  protected <T> T modifyResources (Command<T> command)
      throws SystemException, NoSuchResourceException, NoSuchParentResourceException {
    try {
      return invokeWithRetry(command);
    } catch (ParentObjectNotFoundException e) {
      throw new NoSuchParentResourceException(e.getMessage(), e);
    } catch (ObjectNotFoundException e) {
      throw new NoSuchResourceException("The specified resource doesn't exist: " + e.getMessage(), e);
    }  catch (AmbariException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Caught AmbariException when modifying a resource", e);
      }
      throw new SystemException("An internal system exception occurred: " + e.getMessage(), e);
    }
  }

  /**
   * Helper method to get a configuration request, if one exists.
   * @param parentCategory  the parent category name.  Checks for a property
   *    whose category is the parent and marked as a desired config.
   * @param properties  the properties on the request.
   */
  public static List<ConfigurationRequest> getConfigurationRequests(String parentCategory, Map<String, Object> properties) {

    List<ConfigurationRequest> configs = new LinkedList<>();

    String desiredConfigKey = parentCategory + "/desired_config";
    // Multiple configs to be updated
    if (properties.containsKey(desiredConfigKey)
      && properties.get(desiredConfigKey) instanceof Set) {

      Set<Map<String, Object>> configProperties =
        (Set<Map<String, Object>>) properties.get(desiredConfigKey);
      for (Map<String, Object> value: configProperties) {
        ConfigurationRequest newConfig = new ConfigurationRequest();

        for (Entry<String, Object> e : value.entrySet()) {
          String propName =
            PropertyHelper.getPropertyName(desiredConfigKey + '/' + e.getKey());
          String absCatategory =
            PropertyHelper.getPropertyCategory(desiredConfigKey + '/' + e.getKey());
          parseProperties(newConfig, absCatategory, propName, e.getValue() == null ? null : e.getValue().toString());
        }
        configs.add(newConfig);
      }
      return configs;
    }

    ConfigurationRequest config = null;
    // as a convenience, allow consumers to specify name/value overrides in this
    // call instead of forcing a cluster call to do that work
    for (Entry<String, Object> entry : properties.entrySet()) {
      String absCategory = PropertyHelper.getPropertyCategory(entry.getKey());
      String propName = PropertyHelper.getPropertyName(entry.getKey());

      if (absCategory != null && absCategory.startsWith(desiredConfigKey)) {
        config = (null == config) ? new ConfigurationRequest() : config;
        if(entry.getValue() != null) {
          parseProperties(config, absCategory, propName, entry.getValue().toString());
        }
      }
    }
    if (config != null) {
      configs.add(config);
    }
    return configs;
  }

  public static void parseProperties(ConfigurationRequest config, String absCategory, String propName, String propValue) {
    if (propName.equals("type"))
      config.setType(propValue);
    else if (propName.equals("tag"))
      config.setVersionTag(propValue);
    else if (propName.equals("selected")) {
      config.setSelected(Boolean.parseBoolean(propValue));
    }
    else if (propName.equals("service_config_version_note")) {
      config.setServiceConfigVersionNote(propValue);
    }
    else if (absCategory.endsWith("/properties")) {
      config.getProperties().put(propName, propValue);
    }
    else if (propertiesAttributesPattern.matcher(absCategory).matches()) {
      String attributeName = absCategory.substring(absCategory.lastIndexOf('/') + 1);
      Map<String, Map<String, String>> configAttributesMap = config.getPropertiesAttributes();
      if (null == configAttributesMap) {
        configAttributesMap = new HashMap<>();
        config.setPropertiesAttributes(configAttributesMap);
      }
      Map<String, String> attributesMap = configAttributesMap.get(attributeName);
      if (null == attributesMap) {
        attributesMap = new HashMap<>();
        configAttributesMap.put(attributeName, attributesMap);
      }
      attributesMap.put(propName, propValue);
    }
  }

  // get the resources (id fields only) for the given predicate.
  private Set<Resource> getResources(Predicate givenPredicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    // TODO : Check for case where we should pass the request out to the cluster controller.
    return getResources(PropertyHelper.getReadRequest(getPKPropertyIds()), givenPredicate);
  }

  // determine whether or not the given predicate specifies a unique resource for this provider.
  private boolean specifiesUniqueResource(Predicate predicate) {
    SimplifyingPredicateVisitor visitor = new SimplifyingPredicateVisitor(this);
    PredicateHelper.visit(predicate, visitor);
    List<Predicate> predicates = visitor.getSimplifiedPredicates();

    return predicates.size() == 1 && PredicateHelper.getPropertyIds(predicate).containsAll(getPKPropertyIds());
  }

  //invoke command with retry support in case of database fail
  private <T> T invokeWithRetry(Command<T> command) throws AmbariException, AuthorizationException {
    RetryHelper.clearAffectedClusters();
    int retryAttempts = RetryHelper.getOperationsRetryAttempts();
    do {

      try {
        return command.invoke();
      } catch (Exception e) {
        if (RetryHelper.isDatabaseException(e)) {

          RetryHelper.invalidateAffectedClusters();

          if (retryAttempts > 0) {
            LOG.error("Ignoring database exception to perform operation retry, attempts remaining: " + retryAttempts, e);
            retryAttempts--;
          } else {
            RetryHelper.clearAffectedClusters();
            throw e;
          }
        } else {
          RetryHelper.clearAffectedClusters();
          throw e;
        }
      }

    } while (true);
  }


  // ----- Inner interface ---------------------------------------------------

  /**
   * Command to invoke against the Ambari backend.
   *
   * @param <T>  the response type
   */
  protected interface Command<T> {
    /**
     * Invoke this command.
     *
     * @return  the response
     *
     * @throws AmbariException thrown if a problem occurred during invocation
     */
    T invoke() throws AmbariException, AuthorizationException;
  }
}
