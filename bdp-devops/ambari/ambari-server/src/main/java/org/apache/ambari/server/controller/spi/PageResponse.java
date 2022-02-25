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
 * A response for a single page of resources when pagination is used.
 */
public interface PageResponse {
  /**
   * Get the iterable set of resources for the page.
   *
   * @return the iterable set of resources
   */
  Iterable<Resource> getIterable();

  /**
   * Get the offset of the first resource of the page.
   *
   * @return the offset
   */
  int getOffset();

  /**
   * Get the last resource before this page.
   *
   * @return the last resource before this page; null if this is the first page
   */
  Resource getPreviousResource();

  /**
   * Get the next resource after this page.
   *
   * @return the next resource after this page; null if this is the last page
   */
  Resource getNextResource();

  /**
   * Get the count of total resources without account for paging request.
   * @return total count
   */
  Integer getTotalResourceCount();
}
