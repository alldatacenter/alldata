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

package org.apache.ambari.view.pig.persistence;

import org.apache.ambari.view.pig.persistence.utils.FilteringStrategy;
import org.apache.ambari.view.pig.persistence.utils.Indexed;
import org.apache.ambari.view.pig.persistence.utils.ItemNotFound;

import java.util.List;

/**
 * Object storage interface
 */
public interface Storage {
  /**
   * Persist object to DB. It should be Indexed
   * @param obj object to save
   */
  void store(Indexed obj);

  /**
   * Load object
   * @param model bean class
   * @param id identifier
   * @param <T> bean class
   * @return bean instance
   * @throws ItemNotFound thrown if item with id was not found in DB
   */
  <T extends Indexed> T load(Class<T> model, int id) throws ItemNotFound;

  /**
   * Load all objects of given bean class
   * @param model bean class
   * @param filter filtering strategy (return only those objects that conform condition)
   * @param <T> bean class
   * @return list of filtered objects
   */
  <T extends Indexed> List<T> loadAll(Class<T> model, FilteringStrategy filter);

  /**
   * Load all objects of given bean class
   * @param model bean class
   * @param <T> bean class
   * @return list of all objects
   */
  <T extends Indexed> List<T> loadAll(Class<T> model);

  /**
   * Delete object
   * @param model bean class
   * @param id identifier
   */
  void delete(Class model, int id) throws ItemNotFound;

  /**
   * Check is object exists
   * @param model bean class
   * @param id identifier
   * @return true if exists
   */
  boolean exists(Class model, int id);
}
