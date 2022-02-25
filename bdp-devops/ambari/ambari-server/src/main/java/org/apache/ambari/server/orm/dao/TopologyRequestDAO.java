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
import org.apache.ambari.server.orm.entities.TopologyRequestEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class TopologyRequestDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;

  @Inject
  DaoUtils daoUtils;

  @RequiresSession
  public TopologyRequestEntity findById(Long id) {
    return entityManagerProvider.get().find(TopologyRequestEntity.class, id);
  }

  @RequiresSession
  public List<TopologyRequestEntity> findByClusterId(long clusterId) {
    TypedQuery<TopologyRequestEntity> query = entityManagerProvider.get()
      .createNamedQuery("TopologyRequestEntity.findByClusterId", TopologyRequestEntity.class);

    query.setParameter("clusterId", clusterId);
    return daoUtils.selectList(query);
  }

  @RequiresSession
  public List<TopologyRequestEntity> findAll() {
    return daoUtils.selectAll(entityManagerProvider.get(), TopologyRequestEntity.class);
  }

  @RequiresSession
  public List<TopologyRequestEntity> findAllProvisionRequests() {
    return daoUtils.selectList(entityManagerProvider.get().createNamedQuery("TopologyRequestEntity.findProvisionRequests",
      TopologyRequestEntity.class));
  }

  @Transactional
  public void create(TopologyRequestEntity requestEntity) {
    entityManagerProvider.get().persist(requestEntity);
  }

  @Transactional
  public TopologyRequestEntity merge(TopologyRequestEntity requestEntity) {
    return entityManagerProvider.get().merge(requestEntity);
  }

  @Transactional
  public void remove(TopologyRequestEntity requestEntity) {
    entityManagerProvider.get().remove(requestEntity);
  }

  @Transactional
  public void removeByPK(Long requestId) {
    remove(findById(requestId));
  }

  /**
   * Removes all {@link TopologyRequestEntity} that are associated with the specified cluster ID.
   * @param clusterId the cluster ID.
   */
  @Transactional
  public void removeAll(Long clusterId) {
    List<TopologyRequestEntity> clusterTopologyRequests = findByClusterId(clusterId);
    for (TopologyRequestEntity topologyRequestEntity: clusterTopologyRequests)
      remove(topologyRequestEntity);
  }
}

