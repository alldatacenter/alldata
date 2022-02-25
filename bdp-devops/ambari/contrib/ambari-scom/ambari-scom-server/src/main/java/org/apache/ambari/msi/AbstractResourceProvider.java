/**
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

package org.apache.ambari.msi;

import org.apache.ambari.server.controller.internal.RequestStatusImpl;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * An abstract resource provider for a MSI defined cluster.
 */
public abstract class AbstractResourceProvider implements ResourceProvider {

  private final ClusterDefinition clusterDefinition;

  private final Resource.Type type;

  private final Set<String> propertyIds;


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a resource provider based on the given cluster definition.
   *
   * @param clusterDefinition  the cluster definition
   */
  public AbstractResourceProvider(Resource.Type type, ClusterDefinition clusterDefinition) {
    this.type              = type;
    this.clusterDefinition = clusterDefinition;

    Set<String> propertyIds = PropertyHelper.getPropertyIds(type);
    this.propertyIds = new HashSet<String>(propertyIds);
    this.propertyIds.addAll(PropertyHelper.getCategories(propertyIds));
  }


  // ----- ResourceProvider --------------------------------------------------

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException, UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Management operations are not supported");
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> resultSet = new HashSet<Resource>();

    for (Resource resource : getResources()) {
      if (predicate == null || predicate.evaluate(resource)) {
        ResourceImpl newResource = new ResourceImpl(resource);
        updateProperties(newResource, request, predicate);
        resultSet.add(newResource);
      }
    }
    return resultSet;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> resources = getResources(request, predicate);

    Integer requestId = -1;

    Iterator<Map<String,Object>> iterator = request.getProperties().iterator();
    if (iterator.hasNext()) {
      Map<String, Object> properties = iterator.next();

      for (Resource resource : resources) {
        requestId = updateProperties(resource, properties);
      }
    }
    return getRequestStatus(requestId == -1 ? null : requestId);
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Management operations are not supported");
  }

  @Override
  public Map<Resource.Type, String> getKeyPropertyIds() {
    return PropertyHelper.getKeyPropertyIds(type);
  }

  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    propertyIds = new HashSet<String>(propertyIds);
    propertyIds.removeAll(this.propertyIds);
    return propertyIds;
  }


  // ----- AbstractResourceProvider ------------------------------------------

  /**
   * Get the set of resources for this provider.
   *
   * @return the set of resources
   */
  abstract protected Set<Resource> getResources();

  /**
   * Update the resource with any properties handled by the resource provider.
   *
   * @param resource   the resource to update
   * @param request    the request
   * @param predicate  the predicate
   */
  public abstract void updateProperties(Resource resource, Request request, Predicate predicate);

  /**
   * Update the resource with the given properties.
   *
   * @param resource    the resource to update
   * @param properties  the properties
   *
   * @return the request id; -1 for none
   */
  public abstract int updateProperties(Resource resource, Map<String, Object> properties);

  /**
   * Get a request status
   *
   * @return the request status
   */
  protected RequestStatus getRequestStatus(Integer requestId) {
    if (requestId != null){
      Resource requestResource = new ResourceImpl(Resource.Type.Request);
      requestResource.setProperty(PropertyHelper.getPropertyId("Requests", "id"), requestId);
      requestResource.setProperty(PropertyHelper.getPropertyId("Requests", "status"), "InProgress");
      return new RequestStatusImpl(requestResource);
    }
    return new RequestStatusImpl(null);
  }


  // ----- accessors ---------------------------------------------------------

  /**
   * Get the configuration provider.
   *
   * @return the configuration provider
   */
  protected ClusterDefinition getClusterDefinition() {
    return clusterDefinition;
  }

  /**
   * Get the resource provider type.
   *
   * @return the type
   */
  public Resource.Type getType() {
    return type;
  }


// ----- helper methods ----------------------------------------------------

  /**
   * Get the set of property ids required to satisfy the given request.
   *
   * @param request              the request
   * @param predicate            the predicate
   *
   * @return the set of property ids needed to satisfy the request
   */
  protected Set<String> getRequestPropertyIds(Request request, Predicate predicate) {
    Set<String> propertyIds  = request.getPropertyIds();

    // if no properties are specified, then return them all
    if (propertyIds == null || propertyIds.isEmpty()) {
      return new HashSet<String>(this.propertyIds);
    }

    propertyIds = new HashSet<String>(propertyIds);

    if (predicate != null) {
      propertyIds.addAll(PredicateHelper.getPropertyIds(predicate));
    }
    return propertyIds;
  }

  /**
   * Check to see if the given set contains a property or category id that matches the given property id.
   *
   * @param ids         the set of property/category ids
   * @param propertyId  the property id
   *
   * @return true if the given set contains a property id or category that matches the given property id
   */
  protected static boolean contains(Set<String> ids, String propertyId) {
    boolean contains = ids.contains(propertyId);

    if (!contains) {
      String category = PropertyHelper.getPropertyCategory(propertyId);
      while (category != null && !contains) {
        contains = ids.contains(category);
        category = PropertyHelper.getPropertyCategory(category);
      }
    }
    return contains;
  }

  /**
   * Factory method for obtaining a resource provider based on a given type.
   *
   * @param type               the resource type
   * @param clusterDefinition  the cluster definition
   *
   * @return a new resource provider
   */
  public static ResourceProvider getResourceProvider(Resource.Type type,
                                                     ClusterDefinition clusterDefinition) {
    if (type.equals(Resource.Type.Cluster)) {
      return new ClusterProvider(clusterDefinition);
    } else if (type.equals(Resource.Type.Service)) {
      return new ServiceProvider(clusterDefinition);
    } else if (type.equals(Resource.Type.Component)) {
      return new ComponentProvider(clusterDefinition);
    } else if (type.equals(Resource.Type.Host)) {
      return new HostProvider(clusterDefinition);
    } else if (type.equals(Resource.Type.HostComponent)) {
      return new HostComponentProvider(clusterDefinition);
    } else if (type.equals(Resource.Type.Request)) {
      return new RequestProvider(clusterDefinition);
    } else if (type.equals(Resource.Type.Task)) {
      return new TaskProvider(clusterDefinition);
    } else if (type.equals(Resource.Type.Configuration)) {
      return new ConfigurationProvider(clusterDefinition);
    } else {
      return new NoOpProvider(type, clusterDefinition);
    }
  }
}
