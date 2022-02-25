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
package org.apache.ambari.spi.upgrade;

import org.apache.ambari.spi.ClusterInformation;

/**
 * A provider may specify the orchestration options for parts of the upgrade.
 */
public interface OrchestrationOptions {

  /**
   * Gets the count of components that may be run in parallel for groupings.
   * 
   * @param cluster
   *          the cluster information containing topology and configurations
   * @param service
   *          the name of the service containing the component
   * @param component
   *          the name of the component
   *          
   * @return the number of slaves that may be run in parallel.  Returning a
   *          value less than 1 results in non-parallel behavior
   */
  int getConcurrencyCount(ClusterInformation cluster, String service, String component);
  
}
