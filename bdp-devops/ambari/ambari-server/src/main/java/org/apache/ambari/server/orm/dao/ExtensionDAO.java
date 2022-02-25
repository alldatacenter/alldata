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
import org.apache.ambari.server.orm.entities.ExtensionEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * The {@link ExtensionDAO} class is used to manage the persistence and retrieval of
 * {@link ExtensionEntity} instances.
 *
 * An extension version is like a stack version but it contains custom services.  Linking an extension
 * version to the current stack version allows the cluster to install the custom services contained in
 * the extension version.
 */
@Singleton
public class ExtensionDAO {

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
   * Gets a extension with the specified ID.
   *
   * @param extensionId
   *          the ID of the extension to retrieve.
   * @return the extension or {@code null} if none exists.
   */
  @RequiresSession
  public ExtensionEntity findById(long extensionId) {
    return entityManagerProvider.get().find(ExtensionEntity.class, extensionId);
  }

  /**
   * Gets all of the defined extensions.
   *
   * @return all of the extensions loaded from resources or an empty list (never
   *         {@code null}).
   */
  @RequiresSession
  public List<ExtensionEntity> findAll() {
    TypedQuery<ExtensionEntity> query = entityManagerProvider.get().createNamedQuery(
        "ExtensionEntity.findAll", ExtensionEntity.class);

    return daoUtils.selectList(query);
  }

  /**
   * Gets the extension that matches the specified name and version.
   *
   * @return the extension matching the specified name and version or {@code null}
   *         if none.
   */
  @RequiresSession
  public ExtensionEntity find(String extensionName, String extensionVersion) {
    TypedQuery<ExtensionEntity> query = entityManagerProvider.get().createNamedQuery(
        "ExtensionEntity.findByNameAndVersion", ExtensionEntity.class);

    query.setParameter("extensionName", extensionName);
    query.setParameter("extensionVersion", extensionVersion);

    return daoUtils.selectOne(query);
  }

  /**
   * Persists a new extension instance.
   *
   * @param extension
   *          the extension to persist (not {@code null}).
   */
  @Transactional
  public void create(ExtensionEntity extension)
      throws AmbariException {
    EntityManager entityManager = entityManagerProvider.get();
    entityManager.persist(extension);
  }

  /**
   * Refresh the state of the extension instance from the database.
   *
   * @param extension
   *          the extension to refresh (not {@code null}).
   */
  @Transactional
  public void refresh(ExtensionEntity extension) {
    entityManagerProvider.get().refresh(extension);
  }

  /**
   * Merge the specified extension with the existing extension in the database.
   *
   * @param extension
   *          the extension to merge (not {@code null}).
   * @return the updated extension with merged content (never {@code null}).
   */
  @Transactional
  public ExtensionEntity merge(ExtensionEntity extension) {
    return entityManagerProvider.get().merge(extension);
  }

  /**
   * Creates or updates the specified entity. This method will check
   * {@link ExtensionEntity#getExtensionId()} in order to determine whether the entity
   * should be created or merged.
   *
   * @param extension
   *          the extension to create or update (not {@code null}).
   */
  public void createOrUpdate(ExtensionEntity extension)
      throws AmbariException {
    if (null == extension.getExtensionId()) {
      create(extension);
    } else {
      merge(extension);
    }
  }

  /**
   * Removes the specified extension and all related clusters, services and
   * components.
   *
   * @param extension
   *          the extension to remove.
   */
  @Transactional
  public void remove(ExtensionEntity extension) {
    EntityManager entityManager = entityManagerProvider.get();
    extension = findById(extension.getExtensionId());
    if (null != extension) {
      entityManager.remove(extension);
    }
  }
}
