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
package org.apache.ambari.server.orm.dao;

import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

/**
 * CRUD DAO.
 *
 * @param <E> entity class
 * @param <K> primary key class
 */
public class CrudDAO<E, K> {
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  private Class<E> entityClass;

  /**
   * Constructor.
   *
   * @param entityClass entity class
   */
  public CrudDAO(Class<E> entityClass) {
    this.entityClass = entityClass;
  }

  /**
   * Retrieves entity by primary key.
   *
   * @param pk primary key
   * @return null if there is no suitable entity
   */
  @RequiresSession
  public E findByPK(K pk) {
    return entityManagerProvider.get().find(entityClass, pk);
  }

  /**
   * Retrieves all entities.
   *
   * @return list of all entities
   */
  @RequiresSession
  public List<E> findAll() {
    final TypedQuery<E> query = entityManagerProvider.get().createQuery("SELECT entity FROM " + entityClass.getSimpleName() + " entity", entityClass);
    return daoUtils.selectList(query);
  }

  /**
   * Retrieves the maximum ID from the entities.
   *
   * @param idColName name of the column that corresponds to the ID.
   * @return maximum ID, or 0 if none exist.
   */
  @RequiresSession
  public Long findMaxId(String idColName) {
    final TypedQuery<Long> query = entityManagerProvider.get().createQuery("SELECT MAX(entity." + idColName + ") FROM "
        + entityClass.getSimpleName() + " entity", Long.class);
    // May be null if no results.
    Long result = daoUtils.selectOne(query);
    return result == null ? 0 : result;
  }

  /**
   * Creates entity.
   *
   * @param entity entity to create
   */
  @Transactional
  protected void create(E entity) {
    entityManagerProvider.get().persist(entity);
  }

  /**
   * Updates entity.
   *
   * @param entity entity to update
   * @return updated entity
   */
  @Transactional
  public E merge(E entity) {
    return entityManagerProvider.get().merge(entity);
  }

  /**
   * Refreshes entity.
   *
   * @param entity entity to refresh
   */
  @Transactional
  public void refresh(E entity) {
    entityManagerProvider.get().refresh(entity);
  }

  /**
   * Deletes entity.
   *
   * @param entity entity to delete
   */
  @Transactional
  public void remove(E entity) {
    entityManagerProvider.get().remove(merge(entity));
    entityManagerProvider.get().getEntityManagerFactory().getCache().evictAll();
  }

  /**
   * Deletes entities.
   *
   * @param entities entities to delete
   */
  @Transactional
  public void remove(Collection<E> entities) {
    for (E entity : entities) {
      entityManagerProvider.get().remove(merge(entity));
    }
    entityManagerProvider.get().getEntityManagerFactory().getCache().evictAll();
  }

  /**
   * Deletes entity by PK.
   *
   * @param pk primary key
   */
  @Transactional
  public void removeByPK(K pk) {
    remove(findByPK(pk));
  }
}
