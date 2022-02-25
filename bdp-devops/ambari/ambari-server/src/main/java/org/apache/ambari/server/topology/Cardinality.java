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

package org.apache.ambari.server.topology;

/**
 * Component cardinality representation.
 */
public class Cardinality {
  String cardinality;
  int min = 0;
  int max = Integer.MAX_VALUE;
  int exact = -1;
  boolean isAll = false;

  public Cardinality(String cardinality) {
    this.cardinality = cardinality;
    if (cardinality != null && ! cardinality.isEmpty()) {
      if (cardinality.contains("+")) {
        min = Integer.parseInt(cardinality.split("\\+")[0]);
      } else if (cardinality.contains("-")) {
        String[] toks = cardinality.split("-");
        min = Integer.parseInt(toks[0]);
        max = Integer.parseInt(toks[1]);
      } else if (cardinality.equals("ALL")) {
        isAll = true;
      } else {
        exact = Integer.parseInt(cardinality);
      }
    }
  }

  /**
   * Determine if component is required for all host groups.
   *
   * @return true if cardinality is 'ALL', false otherwise
   */
  public boolean isAll() {
    return isAll;
  }

  /**
   * Determine if the given count satisfies the required cardinality.
   *
   * @param count  number of host groups containing component
   *
   * @return true id count satisfies the required cardinality, false otherwise
   */
  public boolean isValidCount(int count) {
    if (isAll) {
      return false;
    } else if (exact != -1) {
      return count == exact;
    } else return count >= min && count <= max;
  }

  /**
   * Determine if the cardinality count supports auto-deployment.
   * This determination is independent of whether the component is configured
   * to be auto-deployed.  This only indicates whether auto-deployment is
   * supported for the current cardinality.
   *
   * At this time, only cardinalities of ALL or where a count of 1 is valid are
   * supported.
   *
   * @return true if cardinality supports auto-deployment
   */
  public boolean supportsAutoDeploy() {
    return isValidCount(1) || isAll;
  }

  public String getValue() {
    return cardinality;
  }
}
