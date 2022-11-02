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

/**
 * Extended resource provider interface that adds the ability to return a
 * {@link org.apache.ambari.server.controller.spi.QueryResponse} from a
 * resource query.
 *
 * If a resource provider supports paging and/or sorting of the resources
 * acquired through a query then it should implement
 * {@link org.apache.ambari.server.controller.spi.ExtendedResourceProvider}.
 */
public interface ExtendedResourceProvider extends ResourceProvider {
  /**
   * Query for resources that match the given predicate.
   *
   * @param request    the request object which defines the desired set of properties
   * @param predicate  the predicate object which can be used to filter which
   *                   resources are returned
   *
   * @return the response from the resource query
   *
   * @throws SystemException an internal system exception occurred
   * @throws UnsupportedPropertyException the request contains unsupported property ids
   * @throws NoSuchResourceException the requested resource instance doesn't exist
   * @throws NoSuchParentResourceException a parent resource of the requested resource doesn't exist
   */
  QueryResponse queryForResources(Request request, Predicate predicate)
      throws SystemException,
      UnsupportedPropertyException,
      NoSuchResourceException,
      NoSuchParentResourceException;
}
