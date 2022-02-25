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
 * Holder for page request information used when pagination is requested.
 */
public interface PageRequest {

  /**
   * Get the starting point for the page being requested.
   *
   * @return the starting point
   */
  StartingPoint getStartingPoint();

  /**
   * Get the desired page size.
   *
   * @return the page size; -1 means from the starting point to the end of the resource set
   */
  int getPageSize();

  /**
   * Get the offset (zero based) of the resource that should be used as the start or end
   * of the page.
   *
   * @return the offset
   */
  int getOffset();

  /**
   * Return the predicate that identifies the single resource to be used
   * as the start or end of the page.
   *
   * @return the associated predicate
   */
  Predicate getPredicate();

  /**
   * The desired starting point of the page being requested.
   */
  enum StartingPoint {
    Beginning,      // start the page from the beginning of the resource set
    End,            // end the page at the end of the resource set
    OffsetStart,    // start the page from the associated offset point
    OffsetEnd,      // end the page at the associated offset point
    PredicateStart, // start the page from the resource identified by the associated predicate
    PredicateEnd    // end the page at the resource identified by the associated predicate
    }
}
