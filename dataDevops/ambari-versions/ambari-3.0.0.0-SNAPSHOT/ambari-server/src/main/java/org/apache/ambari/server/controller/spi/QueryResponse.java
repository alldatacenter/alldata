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
 * Resource query response.  Used for resource queries
 * that may include the paging and/or sorting of the
 * returned resources.
 */
public interface QueryResponse {

  /**
   * Get the set of resources returned in the response.
   *
   * @return the set of resources
   */
  Set<Resource> getResources();

  /**
   * Determine whether or not the response is sorted.
   *
   * @return {@code true} if the response is sorted
   */
  boolean isSortedResponse();

  /**
   * Determine whether or not the response is paginated.
   *
   * @return {@code true} if the response is paginated
   */
  boolean isPagedResponse();

  /**
   * Get the the total number of resources for the query result.
   * May be different than the size of the resource set for a
   * paged response.
   *
   * @return total the total number of resources in the query result
   */
  int getTotalResourceCount();
}
