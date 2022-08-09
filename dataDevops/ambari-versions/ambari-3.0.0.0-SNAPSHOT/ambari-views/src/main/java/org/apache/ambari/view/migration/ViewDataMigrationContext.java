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

import org.apache.ambari.view.DataStore;
import org.apache.ambari.view.ViewInstanceDefinition;

import java.util.Map;

/**
 * Interface for view data migration context class. Provides access
 * to the information about origin(source) and current(target) instances.
 * Also provides utility methods for copying persistence entities and
 * instance data.
 */
public interface ViewDataMigrationContext {
  /**
   * Get the current(target) instance data version.
   *
   * @return the data version of current instance
   */
  int getCurrentDataVersion();

  /**
   * Get the instance definition of current instance.
   *
   * @return the instance definition of current instance
   */
  ViewInstanceDefinition getCurrentInstanceDefinition();

  /**
   * Get persistence entities of the current view instance.
   *
   * @return the mapping of entity class name to the class objects,
   * loaded by the classloader of current view version.
   */
  Map<String, Class> getCurrentEntityClasses();

  /**
   * Get a data store for current view persistence entities.
   *
   * @return a data store of current view instance
   */
  DataStore getCurrentDataStore();

  /**
   * Save an instance data value for the given key and given user
   * to current instance.
   *
   * @param user   the user (owner of instance data)
   * @param key    the key
   * @param value  the value
   */
  void putCurrentInstanceData(String user, String key, String value);

  /**
   * Get the current instance data in the mapping of user owning data to the key-value data.
   *
   * @return mapping of the data owner to the current instance data entries
   */
  Map<String, Map<String, String>> getCurrentInstanceDataByUser();

  /**
   * Get the origin(source) instance data version.
   *
   * @return the data version of origin instance
   */
  int getOriginDataVersion();

  /**
   * Get the instance definition of origin instance.
   *
   * @return the instance definition of origin instance
   */
  ViewInstanceDefinition getOriginInstanceDefinition();

  /**
   * Get persistence entities of the origin view instance.
   *
   * @return the mapping of entity class name to the class objects,
   * loaded by the classloader of origin view version.
   */
  Map<String, Class> getOriginEntityClasses();

  /**
   * Get a data store for origin view persistence entities.
   *
   * @return a data store of origin view instance
   */
  DataStore getOriginDataStore();

  /**
   * Save an instance data value for the given key and given user
   * to origin instance.
   *
   * @param user   the user (owner of instance data)
   * @param key    the key
   * @param value  the value
   */
  void putOriginInstanceData(String user, String key, String value);

  /**
   * Get the origin instance data in the mapping of user owning data to the key-value data.
   *
   * @return mapping of the data owner to the origin instance data entries
   */
  Map<String, Map<String, String>> getOriginInstanceDataByUser();

  /**
   * Utility method provides ability to copy all entity objects from origin to current
   * DataStore. Copies all fields as is without any processing.
   *
   * @param originEntityClass    class object of origin (migration source) instance
   * @param currentEntityClass   class object of current (migration target) instance
   */
  void copyAllObjects(Class originEntityClass, Class currentEntityClass) throws ViewDataMigrationException;

  /**
   * Utility method provides ability to copy all entity objects from origin to current
   * DataStore. Entity converter can be provided to do any required migration.
   *
   * @param originEntityClass    class object of origin (migration source) instance
   * @param currentEntityClass   class object of current (migration target) instance
   * @param entityConverter      object responsible of converting every single instance.
   */
  void copyAllObjects(Class originEntityClass, Class currentEntityClass, EntityConverter entityConverter)
      throws ViewDataMigrationException;

  /**
   * Utility method that copies all instance data from origin to current
   * instance without changing.
   */
  void copyAllInstanceData();
}
