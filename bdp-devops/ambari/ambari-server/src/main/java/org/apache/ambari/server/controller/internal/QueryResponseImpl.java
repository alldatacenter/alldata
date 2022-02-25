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

import java.util.Set;

import org.apache.ambari.server.controller.spi.QueryResponse;
import org.apache.ambari.server.controller.spi.Resource;

/**
 * Simple {@link org.apache.ambari.server.controller.spi.QueryResponse} implementation.
 */
public class QueryResponseImpl implements QueryResponse {

  /**
   * The set of resources returned by the query.
   */
  private final Set<Resource> resources;

  /**
   * {@code true} if the response is sorted.
   */
  private final boolean sortedResponse;

  /**
   * {@code true} if the response is paginated.
   */
  private final boolean pagedResponse;

  /**
   * The total number of resources returned by the query.
   */
  private final int totalResourceCount;


  // ----- Constructors ------------------------------------------------------

  /**
   * Constructor.
   *
   * @param resources the set of resources returned by the query.
   */
  public QueryResponseImpl(Set<Resource> resources) {
    this.resources          = resources;
    this.sortedResponse     = false;
    this.pagedResponse      = false;
    this.totalResourceCount = 0;
  }

  /**
   * Constructor.
   *
   * @param resources           the set of resources returned by the query.
   * @param sortedResponse      indicates whether or not the response is sorted
   * @param pagedResponse       indicates whether or not the response is paged
   * @param totalResourceCount  the total number of resources returned by the query
   */
  public QueryResponseImpl(Set<Resource> resources, boolean sortedResponse,
                           boolean pagedResponse, int totalResourceCount) {
    this.resources          = resources;
    this.sortedResponse     = sortedResponse;
    this.pagedResponse      = pagedResponse;
    this.totalResourceCount = totalResourceCount;
  }


  // ----- QueryResponse -----------------------------------------------------

  @Override
  public Set<Resource> getResources() {
    return resources;
  }

  @Override
  public boolean isSortedResponse() {
    return sortedResponse;
  }

  @Override
  public boolean isPagedResponse() {
    return pagedResponse;
  }

  @Override
  public int getTotalResourceCount() {
    return totalResourceCount;
  }
}
