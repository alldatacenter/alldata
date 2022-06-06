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

package org.apache.ambari.view;

import java.util.Map;
import java.util.Set;

/**
 * Class used to access view sub-resources.
 *
 * @param <T> the type of the resource class provided by this ResourceProvider object.
 */
public interface ResourceProvider<T> {
  /**
   * Get a single resource from the given id.  The resource should be
   * populated with the given properties.
   *
   * @param resourceId  the id of the requested resource
   * @param properties  the set of requested property ids
   *
   * @throws SystemException an internal system exception occurred
   * @throws NoSuchResourceException a requested resource doesn't exist
   * @throws UnsupportedPropertyException the request contains unsupported property ids
   *
   * @return the resource
   */
  public T getResource(String resourceId, Set<String> properties) throws
      SystemException, NoSuchResourceException, UnsupportedPropertyException;

  /**
   * Get all of the resources.  The resources should be populated with
   * the given resources.
   *
   * @param request  the read request
   *
   * @throws SystemException an internal system exception occurred
   * @throws NoSuchResourceException a requested resource doesn't exist
   * @throws UnsupportedPropertyException the request contains unsupported property ids
   *
   * @return a set containing all the resources
   */
  public Set<T> getResources(ReadRequest request) throws
      SystemException, NoSuchResourceException, UnsupportedPropertyException;

  /**
   * Create a resource with the given id and given property values.
   *
   * @param resourceId  the id of the requested resource
   * @param properties  the map of property values to set on the new resource
   *
   * @throws SystemException an internal system exception occurred
   * @throws ResourceAlreadyExistsException attempted to create a resource which already exists
   * @throws NoSuchResourceException a parent resource doesn't exist
   * @throws UnsupportedPropertyException the request contains unsupported property ids
   */
  public void createResource(String resourceId, Map<String, Object> properties) throws
      SystemException, ResourceAlreadyExistsException,  NoSuchResourceException, UnsupportedPropertyException;

  /**
   * Update the resource identified by given resource id with the given property values.
   *
   * @param resourceId  the id of the requested resource
   * @param properties  the map of property values to update on the resource
   *
   * @throws SystemException an internal system exception occurred
   * @throws NoSuchResourceException a requested resource doesn't exist
   * @throws UnsupportedPropertyException the request contains unsupported property ids
   *
   * @return true if the resource was successfully updated
   */
  public boolean updateResource(String resourceId, Map<String, Object> properties) throws
      SystemException, NoSuchResourceException, UnsupportedPropertyException;

  /**
   * Delete the resource identified by the given resource id.
   *
   * @param resourceId  the id of the requested resource
   *
   * @throws SystemException an internal system exception occurred
   * @throws NoSuchResourceException the resource instance to be deleted doesn't exist
   * @throws UnsupportedPropertyException the request contains unsupported property ids
   *
   * @return true if the resource was successfully deleted
   */
  public boolean deleteResource(String resourceId) throws
      SystemException, NoSuchResourceException, UnsupportedPropertyException;
}
