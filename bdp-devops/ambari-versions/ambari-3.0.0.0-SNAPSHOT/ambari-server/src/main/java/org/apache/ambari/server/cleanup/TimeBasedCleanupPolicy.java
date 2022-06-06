/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.cleanup;

/**
 * Time and cluster id based cleanup policy
 */
public class TimeBasedCleanupPolicy {

  private String clusterName;
  private Long toDateInMillis;

  /**
   * Constructs an instance based on the given arguments.
   *
   * @param clusterName      the cluster name
   * @param toDateInMillis timestamp before that entities are purged.
   */
  public TimeBasedCleanupPolicy(String clusterName, Long toDateInMillis) {
    this.clusterName = clusterName;
    this.toDateInMillis = toDateInMillis;
  }

  /**
   * @return the cluster name
   */
  public String getClusterName() {
    return clusterName;
  }

  /**
   *
   * @return the timestamp before that entities are purged
   */
  public Long getToDateInMillis() {
    return toDateInMillis;
  }

  /**
   *
   * @return The used purge policy
   */
  public PurgePolicy getPurgePolicy() {
    return PurgePolicy.DELETE;
  }
}
