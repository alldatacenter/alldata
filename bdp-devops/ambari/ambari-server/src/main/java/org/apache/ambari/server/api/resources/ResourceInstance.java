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

package org.apache.ambari.server.api.resources;

import java.util.Map;

import org.apache.ambari.server.api.query.Query;
import org.apache.ambari.server.controller.spi.Resource;

/**
 * Resource instance which contains request specific state.
 */
public interface ResourceInstance {

  /**
   * Set the values of the parent foreign keys.
   *
   * @param keyValueMap  map of all parent foreign keys. Map from resource type to id value.
   */
  void setKeyValueMap(Map<Resource.Type, String> keyValueMap);

  /**
   * Obtain the primary and foreign key properties for the resource.
   *
   * @return map of primary and foreign key values keyed by resource type
   */
  Map<Resource.Type, String> getKeyValueMap();

  /**
   * Return the query associated with the resource.
   * Each resource has one query.
   *
   * @return the associated query
   */
  Query getQuery();

  /**
   * Return the resource definition for this resource type.
   * All information in the definition is static and is specific to the resource type,
   * not the resource instance.
   *
   * @return  the associated resource definition
   */
  ResourceDefinition getResourceDefinition();

  /**
   * Return all sub-resource instances.
   * This will include all children of this resource as well
   * as any other resources referred to via a foreign key property.
   *
   * @return all sub-resource instances
   */
  Map<String, ResourceInstance> getSubResources();

  /**
   * Determine if resource is a collection resource.
   *
   * @return true if the resource is a collection resource; false otherwise
   */
  boolean isCollectionResource();
}
