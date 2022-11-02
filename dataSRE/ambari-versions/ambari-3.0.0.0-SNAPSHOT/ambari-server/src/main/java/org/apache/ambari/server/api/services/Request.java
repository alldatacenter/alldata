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

package org.apache.ambari.server.api.services;

import java.util.List;
import java.util.Map;

import org.apache.ambari.server.api.query.render.Renderer;
import org.apache.ambari.server.api.resources.ResourceDefinition;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.spi.PageRequest;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.SortRequest;
import org.apache.ambari.server.controller.spi.TemporalInfo;

/**
 * Provides information on the current request.
 */
public interface Request {

  /**
   * Enum of request types.
   */
  enum Type {
    GET,
    POST,
    PUT,
    DELETE,
    QUERY_POST
  }

  /**
   * Process the request.
   *
   * @return the result
   */
  Result process();

  /**
   * Obtain the resource definition which corresponds to the resource being operated on by the request.
   * The resource definition provides information about the resource type;
   *
   * @return the associated {@link ResourceDefinition}
   */
  ResourceInstance getResource();

  /**
   * Obtain the URI of this request.
   *
   * @return the request uri
   */
  String getURI();

  /**
   * Obtain the http request type.  Type is one of {@link Type}.
   *
   * @return the http request type
   */
  Type getRequestType();

  /**
   * Obtain the api version of the request.  The api version is specified in the request URI.
   *
   * @return the api version of the request
   */
  int getAPIVersion();

  /**
   * Obtain the query predicate that was built from the user provided predicate fields in the query string.
   * If multiple predicates are supplied, then they will be combined using the appropriate logical grouping
   * predicate such as 'AND'.
   *
   * @return the user defined predicate
   */
  Predicate getQueryPredicate();

  /**
   * Obtain the partial response fields and associated temporal information which were provided
   * in the query string of the request uri.
   *
   * @return map of partial response propertyId to temporal information
   */
  Map<String, TemporalInfo> getFields();

  /**
   * Obtain the request body data.
   */
  RequestBody getBody();

  /**
   * Obtain the http headers associated with the request.
   *
   * @return the http headers
   */
  Map<String, List<String>> getHttpHeaders();

  /**
   * Obtain the pagination request information.
   *
   * @return the page request
   */
  PageRequest getPageRequest();

  /**
   * Obtain information to order the results by.
   *
   * @return the order request
   */
  SortRequest getSortRequest();

  /**
   * Obtain the renderer for the request.
   *
   * @return renderer instance
   */
  Renderer getRenderer();

  /**
   * Returns the remote address for the request
   * @return the remote address for the request
   */
  String getRemoteAddress();

}
