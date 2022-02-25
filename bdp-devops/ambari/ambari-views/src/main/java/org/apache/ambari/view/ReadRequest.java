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

import java.util.Set;

/**
 * Information passed to a read request.
 */
public interface ReadRequest {
  /**
   * Get the requested property ids.
   *
   * @return the property ids
   */
  public Set<String> getPropertyIds();

  /**
   * Get the predicate.
   *
   * @return the predicate; may be null
   */
  public String getPredicate();

  /**
   * Get the temporal information for the given property id for this
   * request, if any.
   *
   * @param id the property id
   *
   * @return the temporal information for the given property id; null if
   *         none exists
   */
  public TemporalInfo getTemporalInfo(String id);


  /**
   * Temporal query data.
   */
  public interface TemporalInfo {
    /**
     * Get the start of the requested time range.  The time is given in
     * seconds since the Unix epoch.
     *
     * @return the start time in seconds
     */
    Long getStartTime();

    /**
     * Get the end of the requested time range.  The time is given in
     * seconds since the Unix epoch.
     *
     * @return the end time in seconds
     */
    Long getEndTime();

    /**
     * Get the requested time between each data point of the temporal
     * data.  The time is given in seconds.
     *
     * @return the step time in seconds
     */
    Long getStep();
  }
}
