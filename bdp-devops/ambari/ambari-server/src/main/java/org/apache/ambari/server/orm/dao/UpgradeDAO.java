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

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.orm.entities.UpgradeGroupEntity;
import org.apache.ambari.server.orm.entities.UpgradeItemEntity;
import org.apache.ambari.server.stack.upgrade.Direction;
import org.apache.ambari.spi.RepositoryType;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * Manages the UpgradeEntity and UpgradeItemEntity classes
 */
@Singleton
public class UpgradeDAO {

  @Inject
  private Provider<EntityManager> entityManagerProvider;

  @Inject
  private DaoUtils daoUtils;

  /**
   * Get all items.
   * @return List of all of the UpgradeEntity items.
   */
  @RequiresSession
  public List<UpgradeEntity> findAll() {
    TypedQuery<UpgradeEntity> query = entityManagerProvider.get().createNamedQuery(
        "UpgradeEntity.findAll", UpgradeEntity.class);

    return daoUtils.selectList(query);
  }

  /**
   * @param clusterId the cluster id
   * @return the list of upgrades initiated for the cluster
   */
  @RequiresSession
  public List<UpgradeEntity> findUpgrades(long clusterId) {
    TypedQuery<UpgradeEntity> query = entityManagerProvider.get().createNamedQuery(
        "UpgradeEntity.findAllForCluster", UpgradeEntity.class);

    query.setParameter("clusterId", Long.valueOf(clusterId));

    return daoUtils.selectList(query);
  }

  /**
   * Finds a specific upgrade
   * @param upgradeId the id
   * @return the entity, or {@code null} if not found
   */
  @RequiresSession
  public UpgradeEntity findUpgrade(long upgradeId) {
    TypedQuery<UpgradeEntity> query = entityManagerProvider.get().createNamedQuery(
        "UpgradeEntity.findUpgrade", UpgradeEntity.class);

    query.setParameter("upgradeId", Long.valueOf(upgradeId));

    return daoUtils.selectSingle(query);
  }

  @RequiresSession
  public UpgradeEntity findUpgradeByRequestId(Long requestId) {
    TypedQuery<UpgradeEntity> query = entityManagerProvider.get().createNamedQuery(
        "UpgradeEntity.findUpgradeByRequestId", UpgradeEntity.class);

    query.setParameter("requestId", requestId);

    return daoUtils.selectSingle(query);
  }

  /**
   * Creates the upgrade entity in the database
   * @param entity the entity
   */
  @Transactional
  public void create(UpgradeEntity entity) {
    EntityManager entityManager = entityManagerProvider.get();
    entityManager.persist(entity);
  }

  /**
   * Removes all upgrades associated with the cluster.
   * @param clusterId the cluster id
   */
  @Transactional
  public void removeAll(long clusterId) {
    List<UpgradeEntity> entities = findUpgrades(clusterId);

    for (UpgradeEntity entity : entities) {
      entityManagerProvider.get().remove(entity);
    }
  }

  /**
   * @param groupId the group id
   * @return the group, or {@code null} if not found
   */
  @RequiresSession
  public UpgradeGroupEntity findUpgradeGroup(Long groupId) {

    TypedQuery<UpgradeGroupEntity> query = entityManagerProvider.get().createQuery(
        "SELECT p FROM UpgradeGroupEntity p WHERE p.upgradeGroupId = :groupId", UpgradeGroupEntity.class);
    query.setParameter("groupId", groupId);

    return daoUtils.selectSingle(query);
  }

  /**
   * @param requestId the request id
   * @param stageId the stage id
   * @return the upgrade entity, or {@code null} if not found
   */
  @RequiresSession
  public UpgradeItemEntity findUpgradeItemByRequestAndStage(Long requestId, Long stageId) {
    TypedQuery<UpgradeItemEntity> query = entityManagerProvider.get().createQuery(
        "SELECT p FROM UpgradeItemEntity p WHERE p.stageId = :stageId AND p.upgradeGroupEntity.upgradeEntity.requestId = :requestId",
        UpgradeItemEntity.class);
    query.setParameter("requestId", requestId);
    query.setParameter("stageId", stageId);

    return daoUtils.selectSingle(query);
  }

  /**
   * @param clusterId
   *          the cluster id
   * @param direction
   *          the direction (not {@code null}).
   * @return the upgrade entity, or {@code null} if not found
   */
  @RequiresSession
  public UpgradeEntity findLastUpgradeForCluster(long clusterId, Direction direction) {
    TypedQuery<UpgradeEntity> query = entityManagerProvider.get().createNamedQuery(
        "UpgradeEntity.findLatestForClusterInDirection", UpgradeEntity.class);
    query.setMaxResults(1);
    query.setParameter("clusterId", clusterId);
    query.setParameter("direction", direction);

    return daoUtils.selectSingle(query);
  }

  /**
   * @param clusterId
   *          the cluster id
   * @return the upgrade entity, or {@code null} if not found
   */
  @RequiresSession
  public UpgradeEntity findLastUpgradeOrDowngradeForCluster(long clusterId) {
    TypedQuery<UpgradeEntity> query = entityManagerProvider.get().createNamedQuery(
        "UpgradeEntity.findLatestForCluster", UpgradeEntity.class);
    query.setMaxResults(1);
    query.setParameter("clusterId", clusterId);

    return daoUtils.selectSingle(query);
  }

  /**
   * Gets the only revertable upgrade if one exists. By definition, only the
   * most recent {@code RepositoryType#PATCH} or {@code RepositoryType#MAINT}
   * upgrade which doesn't have a downgrade already is revertable.
   *
   * @param clusterId
   *          the cluster id
   * @return the upgrade which can be reverted, or {@code null} if not found
   */
  @RequiresSession
  public UpgradeEntity findRevertable(long clusterId) {
    TypedQuery<UpgradeEntity> query = entityManagerProvider.get().createNamedQuery(
        "UpgradeEntity.findRevertable", UpgradeEntity.class);
    query.setMaxResults(1);
    query.setParameter("clusterId", clusterId);

    return daoUtils.selectSingle(query);
  }

  /**
   * Gets the only revertable upgrade if one exists. By definition, only the
   * most recent {@code RepositoryType#PATCH} or {@code RepositoryType#MAINT}
   * upgrade which doesn't have a downgrade already is revertable.
   * <p>
   * This method tries to use some fancy SQL to do the work instead of relying
   * on columns to be set correctly.
   *
   * @param clusterId
   *          the cluster id
   * @return the upgrade which can be reverted, or {@code null} if not found
   */
  @RequiresSession
  public UpgradeEntity findRevertableUsingJPQL(long clusterId) {
    TypedQuery<UpgradeEntity> query = entityManagerProvider.get().createNamedQuery(
        "UpgradeEntity.findRevertableUsingJPQL", UpgradeEntity.class);
    query.setMaxResults(1);
    query.setParameter("clusterId", clusterId);
    query.setParameter("revertableTypes", RepositoryType.REVERTABLE);

    return daoUtils.selectSingle(query);
  }

  @Transactional
  public UpgradeEntity merge(UpgradeEntity upgradeEntity) {
    return entityManagerProvider.get().merge(upgradeEntity);
  }
}
