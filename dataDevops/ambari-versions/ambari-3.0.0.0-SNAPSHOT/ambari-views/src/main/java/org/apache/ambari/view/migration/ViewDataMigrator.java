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

package org.apache.ambari.view.migration;

/**
 * Interface for data migration logic class.
 */
public interface ViewDataMigrator {
  /**
   * Called by the view framework before migration process. View can cancel migration by returning false.
   *
   * @return  true if view can do the migration, false otherwise.
   * @throws  ViewDataMigrationException   migration exception occurred.
   */
  boolean beforeMigration() throws ViewDataMigrationException;

  /**
   * Called by the view framework after migration finished.
   * View can do cleanup and additional migration procedures.
   *
   * @throws ViewDataMigrationException    migration exception occurred.
   */
  void afterMigration() throws ViewDataMigrationException;

  /**
   * Called by the view framework during migration process for every persistence entity
   * of the origin (source) instance.
   * View can migrate a single persistence entity in the implementation of this method.
   *
   * @param originEntityClass    class object of origin (migration source) instance
   * @param currentEntityClass   class object of current (migration target) instance
   *
   * @throws ViewDataMigrationException    migration exception occurred.
   */
  void migrateEntity(Class originEntityClass, Class currentEntityClass) throws ViewDataMigrationException;

  /**
   * Called by the view framework during migration process.
   * View can migrate the instance data in the implementation of this method.
   *
   * @throws ViewDataMigrationException    migration exception occurred.
   */
  void migrateInstanceData() throws ViewDataMigrationException;
}
