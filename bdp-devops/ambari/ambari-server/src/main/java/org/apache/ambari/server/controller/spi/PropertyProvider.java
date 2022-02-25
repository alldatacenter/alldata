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

import java.util.Set;

/**
 * The property provider is used to plug in various property sources into a
 * resource provider.  The property provider is able to populate, or partially
 * populate a given resource object with property values.
 */
public interface PropertyProvider {

  /**
   * Populate the given set of resource with any properties that this property
   * provider can provide and return a populated set of resources.  The provider
   * may drop resources from the original set if it determines that the don't
   * meet the conditions of the predicate.
   *
   * @param resources  the resources to be populated
   * @param request    the request object which defines the desired set of properties
   * @param predicate  the predicate object which filters which resources are returned
   *
   * @return the populated set of resources
   *
   * @throws SystemException thrown if resources cannot be populated
   */
  Set<Resource> populateResources(Set<Resource> resources, Request request, Predicate predicate)
      throws SystemException;

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
