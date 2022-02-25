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

import java.util.Collection;

/**
 * View data store.
 */
public interface DataStore {

  /**
   * Save the given entity to persistent storage.  The entity must be declared as an
   * {@code <entity>} in the {@code <persistence>} element of the view.xml.
   *
   * @param entity  the entity to be persisted.
   *
   * @throws PersistenceException thrown if the given entity can not be persisted
   */
  public void store(Object entity) throws PersistenceException;

  /**
   * Remove the given entity from persistent storage.
   *
   * @param entity  the entity to be removed.
   *
   * @throws PersistenceException thrown if the given entity can not be removed
   */
  public void remove(Object entity) throws PersistenceException;

  /**
   * Find the entity of the given class type that is uniquely identified by the
   * given primary key.
   *
   * @param clazz       the entity class
   * @param primaryKey  the primary key
   * @param <T>         the entity type
   *
   * @return the entity; null if the entity can't be found
   *
   * @throws PersistenceException thrown if an error occurs trying to find the entity
   */
  public <T> T find(Class<T> clazz, Object primaryKey) throws PersistenceException;

  /**
   * Find all the entities for the given where clause.  Specifying null for the where
   * clause should return all entities of the given class type.
   *
   * @param clazz        the entity class
   * @param whereClause  the where clause; may be null
   * @param <T>          the entity type
   *
   * @return all of the entities for the given where clause; empty collection if no
   *         entities can be found
   *
   * @throws PersistenceException
   */
  public <T> Collection<T> findAll(Class<T> clazz, String whereClause) throws PersistenceException;
}
