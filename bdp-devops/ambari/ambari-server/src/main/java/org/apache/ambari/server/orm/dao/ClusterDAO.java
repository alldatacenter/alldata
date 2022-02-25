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
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.StackId;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class ClusterDAO {

  @Inject
  private Provider<EntityManager> entityManagerProvider;

  @Inject
  private DaoUtils daoUtils;

  @Inject
  private StackDAO stackDAO;

  /**
   * Looks for Cluster by ID
   * @param id ID of Cluster
   * @return Found entity or NULL
   */
  @RequiresSession
  public ClusterEntity findById(long id) {
    return entityManagerProvider.get().find(ClusterEntity.class, id);
  }

  @RequiresSession
  public ClusterEntity findByName(String clusterName) {
    TypedQuery<ClusterEntity> query = entityManagerProvider.get().createNamedQuery("clusterByName", ClusterEntity.class);
    query.setParameter("clusterName", clusterName);
    try {
      return query.getSingleResult();
    } catch (NoResultException ignored) {
      return null;
    }
  }


  @RequiresSession
  public ClusterEntity findByResourceId(long resourceId) {
    TypedQuery<ClusterEntity> query = entityManagerProvider.get().createNamedQuery("clusterByResourceId", ClusterEntity.class);
    query.setParameter("resourceId", resourceId);
    try {
      return query.getSingleResult();
    } catch (NoResultException ignored) {
      return null;
    }
  }

  @RequiresSession
  public List<ClusterEntity> findAll() {
    TypedQuery<ClusterEntity> query = entityManagerProvider.get().createNamedQuery("allClusters", ClusterEntity.class);
    try {
      return query.getResultList();
    } catch (NoResultException ignored) {
    }
    return null;
  }

  @RequiresSession
  public ClusterConfigEntity findConfig(Long configEntityPK) {
    return entityManagerProvider.get().find(ClusterConfigEntity.class,
      configEntityPK);
  }

  @RequiresSession
  public ClusterConfigEntity findConfig(Long clusterId, String type, String tag) {
    CriteriaBuilder cb = entityManagerProvider.get().getCriteriaBuilder();
    CriteriaQuery<ClusterConfigEntity> cq = cb.createQuery(ClusterConfigEntity.class);
    Root<ClusterConfigEntity> config = cq.from(ClusterConfigEntity.class);
    cq.where(cb.and(
        cb.equal(config.get("clusterId"), clusterId)),
        cb.equal(config.get("type"), type),
        cb.equal(config.get("tag"), tag)
    );
    TypedQuery<ClusterConfigEntity> query = entityManagerProvider.get().createQuery(cq);
    return daoUtils.selectOne(query);
  }

  @RequiresSession
  public List<ClusterConfigEntity> getEnabledConfigsByTypes(Long clusterId, Collection<String> types) {
    TypedQuery<ClusterConfigEntity> query = entityManagerProvider.get().createNamedQuery(
        "ClusterConfigEntity.findEnabledConfigsByTypes",
      ClusterConfigEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("types", types);

    return daoUtils.selectList(query);
  }

  @RequiresSession
  public ClusterConfigEntity findConfig(Long clusterId, String type, Long version) {
    CriteriaBuilder cb = entityManagerProvider.get().getCriteriaBuilder();
    CriteriaQuery<ClusterConfigEntity> cq = cb.createQuery(ClusterConfigEntity.class);
    Root<ClusterConfigEntity> config = cq.from(ClusterConfigEntity.class);
    cq.where(cb.and(
        cb.equal(config.get("clusterId"), clusterId)),
      cb.equal(config.get("type"), type),
      cb.equal(config.get("version"), version)
    );
    TypedQuery<ClusterConfigEntity> query = entityManagerProvider.get().createQuery(cq);
    return daoUtils.selectOne(query);
  }

  /**
   * Gets the next version that will be created for a given
   * {@link ClusterConfigEntity}.
   *
   * @param clusterId
   *          the cluster that the service is a part of.
   * @param configType
   *          the name of the configuration type (not {@code null}).
   * @return the highest existing value of the version column + 1
   */
  @RequiresSession
  public Long findNextConfigVersion(long clusterId, String configType) {
    TypedQuery<Number> query = entityManagerProvider.get().createNamedQuery(
        "ClusterConfigEntity.findNextConfigVersion", Number.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("configType", configType);

    return daoUtils.selectSingle(query).longValue();
  }

  /**
   * Get all configurations for the specified cluster and stack. This will
   * return different versions of the same configuration type (cluster-env v1
   * and cluster-env v2) if they exist.
   *
   * @param clusterId
   *          the cluster (not {@code null}).
   * @param stackId
   *          the stack (not {@code null}).
   * @return all service configurations for the cluster and stack.
   */
  @RequiresSession
  public List<ClusterConfigEntity> getAllConfigurations(Long clusterId,
      StackId stackId) {

    StackEntity stackEntity = stackDAO.find(stackId.getStackName(),
        stackId.getStackVersion());

    TypedQuery<ClusterConfigEntity> query = entityManagerProvider.get().createNamedQuery(
        "ClusterConfigEntity.findAllConfigsByStack", ClusterConfigEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("stack", stackEntity);

    return daoUtils.selectList(query);
  }

  /**
   * Gets the latest configurations for a given stack for all of the
   * configurations of the specified cluster. This method does not take into
   * account the configuration being enabled, as the latest for a given stack
   * may not be "selected".
   *
   * @param clusterId
   *          the cluster that the service is a part of.
   * @param stackId
   *          the stack to get the latest configurations for (not {@code null}).
   * @return the latest configurations for the specified cluster and stack.
   */
  @RequiresSession
  public List<ClusterConfigEntity> getLatestConfigurations(long clusterId,
      StackId stackId) {
    StackEntity stackEntity = stackDAO.find(stackId.getStackName(),
        stackId.getStackVersion());

    TypedQuery<ClusterConfigEntity> query = entityManagerProvider.get().createNamedQuery(
        "ClusterConfigEntity.findLatestConfigsByStack",
        ClusterConfigEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("stack", stackEntity);

    return daoUtils.selectList(query);
  }

  /**
   * Gets the latest configurations for a given stack with any of the given config types.
   * This method does not take into account the configuration being enabled.
   */
  @RequiresSession
  public List<ClusterConfigEntity> getLatestConfigurationsWithTypes(long clusterId, StackId stackId, Collection<String> configTypes) {
    StackEntity stackEntity = stackDAO.find(stackId.getStackName(), stackId.getStackVersion());
    if (configTypes.isEmpty()) {
      return Collections.emptyList();
    }
    return daoUtils.selectList(
      entityManagerProvider.get()
      .createNamedQuery("ClusterConfigEntity.findLatestConfigsByStackWithTypes", ClusterConfigEntity.class)
      .setParameter("clusterId", clusterId)
      .setParameter("stack", stackEntity)
      .setParameter("types", configTypes));
  }

  /**
   * Gets the latest configurations for a given stack for all of the
   * configurations of the specified cluster.
   *
   * @param clusterId
   *          the cluster that the service is a part of.
   * @param stackId
   *          the stack to get the latest configurations for (not {@code null}).
   * @return the latest configurations for the specified cluster and stack.
   */
  @RequiresSession
  public List<ClusterConfigEntity> getEnabledConfigsByStack(long clusterId, StackId stackId) {
    StackEntity stackEntity = stackDAO.find(stackId.getStackName(), stackId.getStackVersion());

    TypedQuery<ClusterConfigEntity> query = entityManagerProvider.get().createNamedQuery(
        "ClusterConfigEntity.findEnabledConfigsByStack", ClusterConfigEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("stack", stackEntity);

    return daoUtils.selectList(query);
  }

  /**
   * Gets the latest configurations for the specified cluster.
   *
   * @param clusterId
   *          the cluster that the service is a part of.
   * @return the latest configurations for the specified cluster.
   */
  @RequiresSession
  public List<ClusterConfigEntity> getEnabledConfigs(long clusterId) {

    TypedQuery<ClusterConfigEntity> query = entityManagerProvider.get().createNamedQuery(
        "ClusterConfigEntity.findEnabledConfigs", ClusterConfigEntity.class);

    query.setParameter("clusterId", clusterId);

    return daoUtils.selectList(query);
  }

  /**
   * Gets the latest config in the given cluster by type name. Only a config
   * which is enabled can be returned.
   *
   * @param clusterId
   *          the ID of the cluster.
   * @param type
   *          the config type (not {@code null}).
   * @return a config, or {@code null} if there is none enabled.
   */
  @RequiresSession
  public ClusterConfigEntity findEnabledConfigByType(long clusterId, String type) {

    TypedQuery<ClusterConfigEntity> query = entityManagerProvider.get().createNamedQuery(
        "ClusterConfigEntity.findEnabledConfigByType", ClusterConfigEntity.class);

    query.setParameter("clusterId", clusterId);
    query.setParameter("type", type);

    return daoUtils.selectOne(query);
  }

  /**
   * Create Cluster entity in Database
   * @param clusterEntity entity to create
   */
  @Transactional
  public void create(ClusterEntity clusterEntity) {
    entityManagerProvider.get().persist(clusterEntity);
  }

  /**
   * Creates a cluster configuration in the DB.
   */
  @Transactional
  public void createConfig(ClusterConfigEntity entity) {
    entityManagerProvider.get().persist(entity);
  }

  /**
   * Remove a cluster configuration in the DB.
   */
  @Transactional
  public void removeConfig(ClusterConfigEntity entity) {
    entityManagerProvider.get().remove(entity);
  }

  /**
   * Retrieve entity data from DB
   *
   * @param clusterEntity
   *          entity to refresh
   */
  @Transactional
  public void refresh(ClusterEntity clusterEntity) {
    entityManagerProvider.get().refresh(clusterEntity);
  }

  /**
   * Merge the specified entity into the current persistence context.
   *
   * @param clusterEntity
   *          the entity to merge (not {@code null}).
   * @return the managed entity which was merged (never {@code null}).
   */
  public ClusterEntity merge(ClusterEntity clusterEntity) {
    return merge(clusterEntity, false);
  }

  /**
   * Merge the specified entity into the current persistence context, optionally
   * instructing the {@link EntityManager} to write any queued persist/merges
   * into the database immediately.
   *
   * @param clusterEntity
   *          the entity to merge (not {@code null}).
   * @param flush
   *          if {@code true} then {@link EntityManager#flush()} will be invoked
   *          immediately after the merge.
   * @return the managed entity which was merged (never {@code null}).
   */
  @Transactional
  public ClusterEntity merge(ClusterEntity clusterEntity, boolean flush) {
    EntityManager entityManager = entityManagerProvider.get();
    clusterEntity = entityManager.merge(clusterEntity);

    // force any queued persist/merges to be written to the database, including
    // the merge from above
    if (flush) {
      entityManager.flush();
    }

    return clusterEntity;
  }

  /**
   * Merge the specified entity into the current persistence context.
   *
   * @param clusterConfigEntity
   *          the entity to merge (not {@code null}).
   * @return the managed entity which was merged (never {@code null}).
   */
  @Transactional
  public ClusterConfigEntity merge(ClusterConfigEntity clusterConfigEntity) {
    return merge(clusterConfigEntity, false);
  }

  /**
   * Merge the specified entity into the current persistence context.
   *
   * @param clusterConfigEntity
   *          the entity to merge (not {@code null}).
   * @param flush
   *          if {@code true} then {@link EntityManager#flush()} will be invoked
   *          immediately after the merge.
   * @return the managed entity which was merged (never {@code null}).
   */
  @Transactional
  public ClusterConfigEntity merge(ClusterConfigEntity clusterConfigEntity, boolean flush) {
    EntityManager entityManager = entityManagerProvider.get();
    ClusterConfigEntity clusterConfigEntityRes = entityManager.merge(clusterConfigEntity);
    if(flush) {
      entityManager.flush();
    }
    return clusterConfigEntityRes;
  }

  @Transactional
  public void remove(ClusterEntity clusterEntity) {
    entityManagerProvider.get().remove(clusterEntity);
  }

  @Transactional
  public void removeByName(String clusterName) {
    remove(findByName(clusterName));
  }

  @Transactional
  public void removeByPK(long id) {
    remove(findById(id));
  }

  @RequiresSession
  public boolean isManaged(ClusterEntity entity) {
    return entityManagerProvider.get().contains(entity);
  }
}
