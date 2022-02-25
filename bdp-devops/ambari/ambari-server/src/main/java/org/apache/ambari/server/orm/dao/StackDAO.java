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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.StackId;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * The {@link StackDAO} class is used to manage the persistence and retrieval of
 * {@link StackEntity} instances.
 */
@Singleton
public class StackDAO {

  /**
   * JPA entity manager
   */
  @Inject
  private Provider<EntityManager> entityManagerProvider;

  /**
   * DAO utilities for dealing mostly with {@link TypedQuery} results.
   */
  @Inject
  private DaoUtils daoUtils;

  /**
   * Gets a stack with the specified ID.
   *
   * @param stackId
   *          the ID of the stack to retrieve.
   * @return the stack or {@code null} if none exists.
   */
  @RequiresSession
  public StackEntity findById(long stackId) {
    return entityManagerProvider.get().find(StackEntity.class, stackId);
  }

  /**
   * Gets all of the defined stacks.
   *
   * @return all of the stacks loaded from resources or an empty list (never
   *         {@code null}).
   */
  @RequiresSession
  public List<StackEntity> findAll() {
    TypedQuery<StackEntity> query = entityManagerProvider.get().createNamedQuery(
        "StackEntity.findAll", StackEntity.class);

    return daoUtils.selectList(query);
  }

  /**
   * Gets the stack that matches the specified name and version.
   *
   * @return the stack matching the specified name and version or {@code null}
   *         if none.
   */
  @RequiresSession
  public StackEntity find(String stackName, String stackVersion) {
    TypedQuery<StackEntity> query = entityManagerProvider.get().createNamedQuery(
        "StackEntity.findByNameAndVersion", StackEntity.class);

    query.setParameter("stackName", stackName);
    query.setParameter("stackVersion", stackVersion);

    return daoUtils.selectOne(query);
  }

  /**
   * Gets the stack that matches the specified stack ID by name and version.
   *
   * @param stackId
   *          the stack ID to find (not {@code null}).
   * @return the stack matching the specified name and version or {@code null}
   *         if none.
   */
  @RequiresSession
  public StackEntity find(StackId stackId) {
    return find(stackId.getStackName(), stackId.getStackVersion());
  }

  /**
   * Persists a new stack instance.
   *
   * @param stack
   *          the stack to persist (not {@code null}).
   */
  @Transactional
  public void create(StackEntity stack)
      throws AmbariException {
    EntityManager entityManager = entityManagerProvider.get();
    entityManager.persist(stack);
  }

  /**
   * Refresh the state of the stack instance from the database.
   *
   * @param stack
   *          the stack to refresh (not {@code null}).
   */
  @Transactional
  public void refresh(StackEntity stack) {
    entityManagerProvider.get().refresh(stack);
  }

  /**
   * Merge the speicified stack with the existing stack in the database.
   *
   * @param stack
   *          the stack to merge (not {@code null}).
   * @return the updated stack with merged content (never {@code null}).
   */
  @Transactional
  public StackEntity merge(StackEntity stack) {
    return entityManagerProvider.get().merge(stack);
  }

  /**
   * Creates or updates the specified entity. This method will check
   * {@link StackEntity#getStackId()} in order to determine whether the entity
   * should be created or merged.
   *
   * @param stack
   *          the stack to create or update (not {@code null}).
   */
  public void createOrUpdate(StackEntity stack)
      throws AmbariException {
    if (null == stack.getStackId()) {
      create(stack);
    } else {
      merge(stack);
    }
  }

  /**
   * Removes the specified stack and all related clusters, services and
   * components.
   *
   * @param stack
   *          the stack to remove.
   */
  @Transactional
  public void remove(StackEntity stack) {
    EntityManager entityManager = entityManagerProvider.get();
    stack = findById(stack.getStackId());
    if (null != stack) {
      entityManager.remove(stack);
    }
  }

  /**
   * Removes the specified stack based on mpackid.
   *
   * @param mpackId
   *
   */
  @Transactional
  public void removeByMpack(Long mpackId) {
    entityManagerProvider.get().remove(findByMpack(mpackId));
  }

  /**
   * Gets the stack that matches the specified mpackid.
   *
   * @return the stack matching the specified mpackid or {@code null}
   *         if none.
   */
  public StackEntity findByMpack(Long mpackId) {
    TypedQuery<StackEntity> query = entityManagerProvider.get().createNamedQuery(
            "StackEntity.findByMpack", StackEntity.class);
    query.setParameter("mpackId", mpackId);

    return daoUtils.selectOne(query);
  }
}
