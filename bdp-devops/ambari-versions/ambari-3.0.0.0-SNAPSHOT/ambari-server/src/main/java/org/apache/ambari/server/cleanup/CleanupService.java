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
package org.apache.ambari.server.cleanup;

/**
 * Contract for services in charge for cleaning operational data.
 * @param <T> the type based on which the cleanup is done
 */
public interface CleanupService<T> {

  interface CleanupResult {
    /**
     * Returns the number of rows deleted by the cleanup
     * @return The total number of rows deleted by the cleanup
     */
    long getAffectedRows();

    /**
     * The cleanup process executes the specific cleanup operations via
     * {@link org.apache.ambari.server.orm.dao.Cleanable} implementations.
     * Some of these may fail during the cleanup process. This method returns
     * the number of failed clean ups.
     * @return The number of failed cleanups.
     */
    int getErrorCount();
  }

  /**
   * Triggers the cleanup for the given cleanup policy.
   *
   * @param cleanupPolicy the cleanup policy based on which the cleanup is executed.
   * @return the affected "rows"
   */
  CleanupResult cleanup(T cleanupPolicy);
}
