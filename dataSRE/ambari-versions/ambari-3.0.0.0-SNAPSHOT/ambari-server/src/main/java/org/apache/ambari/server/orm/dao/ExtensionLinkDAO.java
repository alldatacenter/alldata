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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.ExtensionLinkRequest;
import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.ExtensionLinkEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * The {@link ExtensionLinkDAO} class is used to manage the persistence and retrieval of
 * {@link ExtensionLinkEntity} instances.
 *
 * An extension version is like a stack version but it contains custom services.  Linking an extension
 * version to the current stack version allows the cluster to install the custom services contained in
 * the extension version.
 */
@Singleton
public class ExtensionLinkDAO {

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
   * Gets the extension links that match the specified stack name and version.
   *
   * @return the extension links  matching the specified stack name and version if any.
   */
  @RequiresSession
  public List<ExtensionLinkEntity> find(ExtensionLinkRequest request) {
    if (request.getLinkId() != null) {
      ExtensionLinkEntity entity = findById(Long.parseLong(request.getLinkId()));
      List<ExtensionLinkEntity> list = new ArrayList<>();
      list.add(entity);
      return list;
    }

    String stackName = request.getStackName();
    String stackVersion = request.getStackVersion();
    String extensionName = request.getExtensionName();
    String extensionVersion = request.getExtensionVersion();

    if (stackName != null && stackVersion != null) {
      if (extensionName != null) {
        if (extensionVersion != null) {
          ExtensionLinkEntity entity = findByStackAndExtension(stackName, stackVersion, extensionName, extensionVersion);
          List<ExtensionLinkEntity> list = new ArrayList<>();
          list.add(entity);
          return list;
        }
        return findByStackAndExtensionName(stackName, stackVersion, extensionName);
      }
      return findByStack(stackName, stackVersion);
    }
    if (extensionName != null && extensionVersion != null) {
      return findByExtension(extensionName, extensionVersion);
    }

    return findAll();
  }

  /**
   * Gets an extension link with the specified ID.
   *
   * @param linkId
   *          the ID of the extension link to retrieve.
   * @return the extension or {@code null} if none exists.
   */
  @RequiresSession
  public ExtensionLinkEntity findById(long linkId) {
    return entityManagerProvider.get().find(ExtensionLinkEntity.class, linkId);
  }

  /**
   * Gets all of the defined extension links.
   *
   * @return all of the extension links loaded from resources or an empty list (never
   *         {@code null}).
   */
  @RequiresSession
  public List<ExtensionLinkEntity> findAll() {
    TypedQuery<ExtensionLinkEntity> query = entityManagerProvider.get().createNamedQuery(
        "ExtensionLinkEntity.findAll", ExtensionLinkEntity.class);

    return daoUtils.selectList(query);
  }

  /**
   * Gets the extension links that match the specified extension name and version.
   *
   * @return the extension links matching the specified extension name and version if any.
   */
  @RequiresSession
  public List<ExtensionLinkEntity> findByExtension(String extensionName, String extensionVersion) {
    TypedQuery<ExtensionLinkEntity> query = entityManagerProvider.get().createNamedQuery(
        "ExtensionLinkEntity.findByExtension", ExtensionLinkEntity.class);

    query.setParameter("extensionName", extensionName);
    query.setParameter("extensionVersion", extensionVersion);

    return daoUtils.selectList(query);
  }

  /**
   * Gets the extension links that match the specified stack name and version.
   *
   * @return the extension links  matching the specified stack name and version if any.
   */
  @RequiresSession
  public List<ExtensionLinkEntity> findByStack(String stackName, String stackVersion) {
    TypedQuery<ExtensionLinkEntity> query = entityManagerProvider.get().createNamedQuery(
        "ExtensionLinkEntity.findByStack", ExtensionLinkEntity.class);

    query.setParameter("stackName", stackName);
    query.setParameter("stackVersion", stackVersion);

    return daoUtils.selectList(query);
  }

  /**
   * Gets the extension link that match the specified stack name, stack version and extension name.
   *
   * @return the extension link matching the specified stack name, stack version and extension name if any.
   */
  @RequiresSession
  public List<ExtensionLinkEntity> findByStackAndExtensionName(String stackName, String stackVersion, String extensionName) {
    TypedQuery<ExtensionLinkEntity> query = entityManagerProvider.get().createNamedQuery(
        "ExtensionLinkEntity.findByStackAndExtensionName", ExtensionLinkEntity.class);

    query.setParameter("stackName", stackName);
    query.setParameter("stackVersion", stackVersion);
    query.setParameter("extensionName", extensionName);

    return daoUtils.selectList(query);
  }

  /**
   * Gets the extension link that match the specified stack name, stack version, extension name and extension version.
   *
   * @return the extension link matching the specified stack name, stack version, extension name and extension version if any.
   */
  @RequiresSession
  public ExtensionLinkEntity findByStackAndExtension(String stackName, String stackVersion, String extensionName, String extensionVersion) {
    TypedQuery<ExtensionLinkEntity> query = entityManagerProvider.get().createNamedQuery(
        "ExtensionLinkEntity.findByStackAndExtension", ExtensionLinkEntity.class);

    query.setParameter("stackName", stackName);
    query.setParameter("stackVersion", stackVersion);
    query.setParameter("extensionName", extensionName);
    query.setParameter("extensionVersion", extensionVersion);

    return daoUtils.selectOne(query);
  }

  /**
   * Persists a new extension link instance.
   *
   * @param link
   *          the extension link to persist (not {@code null}).
   */
  @Transactional
  public void create(ExtensionLinkEntity link)
      throws AmbariException {
    EntityManager entityManager = entityManagerProvider.get();
    entityManager.persist(link);
  }

  /**
   * Refresh the state of the extension instance from the database.
   *
   * @param link
   *          the extension link to refresh (not {@code null}).
   */
  @Transactional
  public void refresh(ExtensionLinkEntity link) {
    entityManagerProvider.get().refresh(link);
  }

  /**
   * Merge the specified extension link with the existing extension link in the database.
   *
   * @param link
   *          the extension link to merge (not {@code null}).
   * @return the updated extension link with merged content (never {@code null}).
   */
  @Transactional
  public ExtensionLinkEntity merge(ExtensionLinkEntity link) {
    return entityManagerProvider.get().merge(link);
  }

  /**
   * Creates or updates the specified entity. This method will check
   * {@link ExtensionLinkEntity#getLinkId()} in order to determine whether the entity
   * should be created or merged.
   *
   * @param link the link to create or update (not {@code null}).
   */
  public void createOrUpdate(ExtensionLinkEntity link)
      throws AmbariException {
    if (null == link.getLinkId()) {
      create(link);
    } else {
      merge(link);
    }
  }

  /**
   * Removes the specified extension link
   *
   * @param link
   *          the extension link to remove.
   */
  @Transactional
  public void remove(ExtensionLinkEntity link) {
    EntityManager entityManager = entityManagerProvider.get();
    link = findById(link.getLinkId());
    if (null != link) {
      entityManager.remove(link);
    }
  }
}
