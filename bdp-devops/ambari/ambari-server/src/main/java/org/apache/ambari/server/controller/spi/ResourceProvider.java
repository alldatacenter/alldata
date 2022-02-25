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
package org.apache.ambari.server.controller.spi;

import java.util.Map;
import java.util.Set;

/**
 * The resource provider allows for the plugging in of a back end data store
 * for a resource type.  The resource provider is associated with a specific
 * resource type and can be queried for a list of resources of that type.
 * The resource provider plugs into and is used by the
 * {@link ClusterController cluster controller} to obtain a list of resources
 * for a given request.
 */
public interface ResourceProvider {

  /**
   * Create the resources defined by the properties in the given request object.
   *
   *
   * @param request  the request object which defines the set of properties
   *                 for the resources to be created
   *
   * @return the request status
   *
   * @throws SystemException an internal system exception occurred
   * @throws UnsupportedPropertyException the request contains unsupported property ids
   * @throws ResourceAlreadyExistsException attempted to create a resource which already exists
   * @throws NoSuchParentResourceException a parent resource of the resource to create doesn't exist
   */
  RequestStatus createResources(Request request)
      throws SystemException,
      UnsupportedPropertyException,
      ResourceAlreadyExistsException,
      NoSuchParentResourceException;

  /**
   * Get a set of {@link Resource resources} based on the given request and predicate
   * information.
   * </p>
   * Note that it is not required for this resource provider to completely filter
   * the set of resources based on the given predicate.  It may not be possible
   * since some of the properties involved may be provided by another
   * {@link PropertyProvider provider}.  This partial filtering is allowed because
   * the predicate will always be applied by the calling cluster controller.  The
   * predicate is made available at this level so that some pre-filtering can be done
   * as an optimization.
   * </p>
   * A simple implementation of a resource provider may choose to just return all of
   * the resources of a given type and allow the calling cluster controller to filter
   * based on the predicate.
   *
   *
   * @param request    the request object which defines the desired set of properties
   * @param predicate  the predicate object which can be used to filter which
   *                   resources are returned
   * @return a set of resources based on the given request and predicate information
   *
   * @throws SystemException an internal system exception occurred
   * @throws UnsupportedPropertyException the request contains unsupported property ids
   * @throws NoSuchResourceException the requested resource instance doesn't exist
   * @throws NoSuchParentResourceException a parent resource of the requested resource doesn't exist
   */
  Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException,
      UnsupportedPropertyException,
      NoSuchResourceException,
      NoSuchParentResourceException;

  /**
   * Update the resources selected by the given predicate with the properties
   * from the given request object.
   *
   *
   *
   * @param request    the request object which defines the set of properties
   *                   for the resources to be updated
   * @param predicate  the predicate object which can be used to filter which
   *                   resources are updated
   *
   * @return the request status
   *
   * @throws SystemException an internal system exception occurred
   * @throws UnsupportedPropertyException the request contains unsupported property ids
   * @throws NoSuchResourceException the resource instance to be updated doesn't exist
   * @throws NoSuchParentResourceException a parent resource of the resource doesn't exist
   */
  RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException,
      UnsupportedPropertyException,
      NoSuchResourceException,
      NoSuchParentResourceException;

  /**
   * Delete the resources selected by the given predicate.
   *
   * @param request   the request object which defines the set of properties
   *                  for the resources to be updated
   * @param predicate the predicate object which can be used to filter which
   *                  resources are deleted
   *
   * @return the request status
   *
   * @throws SystemException an internal system exception occurred
   * @throws UnsupportedPropertyException the request contains unsupported property ids
   * @throws NoSuchResourceException the resource instance to be deleted doesn't exist
   * @throws NoSuchParentResourceException a parent resource of the resource doesn't exist
   */
  RequestStatus deleteResources(Request request, Predicate predicate)
      throws SystemException,
      UnsupportedPropertyException,
      NoSuchResourceException,
      NoSuchParentResourceException;

  /**
   * Get the key property ids for the resource type associated with this resource
   * provider.  The key properties are those that uniquely identify the resource.
   *</p>
   * For example, the resource 'HostComponent' is uniquely identified by
   * its associated 'Cluster', 'Host' and 'Component' resources.  The key property ids
   * for a 'HostComponent' resource includes the property ids of the foreign key
   * references from the 'HostComponent' to 'Cluster', 'Host' and 'Component' resources.
   *
   * @return a map of key property ids
   */
  Map<Resource.Type, String> getKeyPropertyIds();

  /**
   * Check whether the set of given property ids is supported by this resource
   * provider.
   *
   * @return a subset of the given property id set containing any property ids not
   *         supported by this resource provider.  An empty return set indicates
   *         that all of the given property ids are supported.
   */
  Set<String> checkPropertyIds(Set<String> propertyIds);
}
