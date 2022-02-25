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
 * The request object carries the properties or property ids required to
 * satisfy a resource request.  The request object also contains any
 * temporal (date range) information, if any, for each requested property.
 */
public interface Request {
  /**
   * Constant for request info property which contains the raw request body.
   */
  String REQUEST_INFO_BODY_PROPERTY = "RAW_REQUEST_BODY";

  String DIRECTIVE_DRY_RUN = "dry_run";

  /**
   * Get the set of property ids being requested.  Used for requests to get
   * resources.  An empty set signifies that all supported properties should
   * be returned (i.e. select * ).
   *
   * @return the set of property ids being requested
   */
  Set<String> getPropertyIds();

  /**
   * Get the property values of the request.  Used
   * for requests to update or create resources.  Each value
   * in the set is a map of properties for a resource being
   * created/updated.  Each map contains property values keyed
   * by property ids.
   *
   * @return the set of properties being requested
   */
  Set<Map<String, Object>> getProperties();

  /**
   * Get any request info properties of the request.  These are optional properties
   * that are specific to the request but not related to any resource.
   */
  Map<String, String> getRequestInfoProperties();

  /**
   * Get the {@link TemporalInfo temporal information} for the given property
   * id for this request, if any.
   *
   * @param id the property id
   * @return the temporal information for the given property id; null if noe exists
   */
  TemporalInfo getTemporalInfo(String id);

  /**
   * Obtain the pagination request information. This structure can be used for
   * the concrete {@link ResourceProvider} to instruct the caller that the
   * result set has already been paginated.
   *
   * @return the page request information.
   */
  PageRequest getPageRequest();

  /**
   * Obtain information to order the results by. This structure can be used for
   * the concrete {@link ResourceProvider} to instruct the caller that the
   * result set has already been sorted.
   *
   * @return the sort request information.
   */
  SortRequest getSortRequest();

  /**
   * Is this a dry run request.
   * @return true - if request is dry run request, false otherwise.
   */
  boolean isDryRunRequest();
}
