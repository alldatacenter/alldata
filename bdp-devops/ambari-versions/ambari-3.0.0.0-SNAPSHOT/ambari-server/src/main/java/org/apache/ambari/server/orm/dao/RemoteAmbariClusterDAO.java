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
import org.apache.ambari.server.orm.entities.RemoteAmbariClusterEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * Remote Ambari Cluster Data Access Object.
 */
@Singleton
public class RemoteAmbariClusterDAO {
  /**
   * JPA entity manager
   */
  @Inject
  private Provider<EntityManager> entityManagerProvider;
  @Inject
  private DaoUtils daoUtils;


  /**
   * Find all view instances.
   *
   * @return all views or an empty List
   */
  @RequiresSession
  public List<RemoteAmbariClusterEntity> findAll() {
    TypedQuery<RemoteAmbariClusterEntity> query = entityManagerProvider.get().
        createNamedQuery("allRemoteAmbariClusters", RemoteAmbariClusterEntity.class);

    return query.getResultList();
  }

  /**
   * Find Cluster by name
   * @param clusterName
   * @return
     */
  @RequiresSession
  public RemoteAmbariClusterEntity findByName(String clusterName) {
    TypedQuery<RemoteAmbariClusterEntity> query = entityManagerProvider.get().
            createNamedQuery("remoteAmbariClusterByName", RemoteAmbariClusterEntity.class);
    query.setParameter("clusterName", clusterName);
    return daoUtils.selectSingle(query);
  }

  /**
   * Find Cluster by Id
   * @param clusterId
   * @return
   */
  @RequiresSession
  public RemoteAmbariClusterEntity findById(Long clusterId) {
    TypedQuery<RemoteAmbariClusterEntity> query = entityManagerProvider.get().
      createNamedQuery("remoteAmbariClusterById", RemoteAmbariClusterEntity.class);
    query.setParameter("clusterId", clusterId);
    return daoUtils.selectSingle(query);
  }

  /**
   * Save a Cluster entity
   * @param entity
     */
  @Transactional
  public void save(RemoteAmbariClusterEntity entity) {
    entityManagerProvider.get().persist(entity);
  }

  /**
   * Update and merge a Remote Ambari Cluster entity
   * @param entity
     */
  @Transactional
  public void update(RemoteAmbariClusterEntity entity) {
    entityManagerProvider.get().merge(entity);

  }

  /**
   * Remove a cluster entity
   * @param clusterEntity
     */
  @Transactional
  public void delete(RemoteAmbariClusterEntity clusterEntity) {
    entityManagerProvider.get().remove(clusterEntity);
  }
}
