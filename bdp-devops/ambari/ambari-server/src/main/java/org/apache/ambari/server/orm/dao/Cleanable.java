/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.ambari.server.orm.dao;

import org.apache.ambari.server.cleanup.TimeBasedCleanupPolicy;

/**
 * Interface to be implemented by all DAOs that support the cleanup functionality.
 * All implementing DAO are automatically configured in the cleanup process.
 *
 */
public interface Cleanable {

  /**
   * Performs the cleanup for the entiries the implementing DAO is responsible for.
   *
   * @param policy the policy with the parameters of the cleanup
   * @return the number of affected records if available
   */
  long cleanup(TimeBasedCleanupPolicy policy);
}
