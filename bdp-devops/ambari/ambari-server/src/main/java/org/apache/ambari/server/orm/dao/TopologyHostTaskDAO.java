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
package org.apache.ambari.server.orm.dao;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.TopologyHostTaskEntity;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class TopologyHostTaskDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;

  @Inject
  DaoUtils daoUtils;

  @RequiresSession
  public TopologyHostTaskEntity findById(Long id) {
    return entityManagerProvider.get().find(TopologyHostTaskEntity.class, id);
  }

  @RequiresSession
  public Collection<TopologyHostTaskEntity> findByHostRequest(Long id) {
    TypedQuery<TopologyHostTaskEntity> query = entityManagerProvider.get()
        .createNamedQuery("TopologyHostTaskEntity.findByHostRequest", TopologyHostTaskEntity.class);

    query.setParameter("hostRequestId", id);
    return daoUtils.selectList(query);
  }

  @RequiresSession
  public Set<Long> findHostRequestIdsByHostTaskIds(Set<Long> hostTaskIds) {
    EntityManager entityManager = entityManagerProvider.get();
    TypedQuery<Long> topologyHostTaskQuery =
            entityManager.createNamedQuery("TopologyLogicalTaskEntity.findHostRequestIdsByHostTaskIds", Long.class);

    topologyHostTaskQuery.setParameter("hostTaskIds", hostTaskIds);

    return Sets.newHashSet(daoUtils.selectList(topologyHostTaskQuery));
  }

  @RequiresSession
  public List<TopologyHostTaskEntity> findAll() {
    return daoUtils.selectAll(entityManagerProvider.get(), TopologyHostTaskEntity.class);
  }

  @Transactional
  public void create(TopologyHostTaskEntity requestEntity) {
    entityManagerProvider.get().persist(requestEntity);
  }

  @Transactional
  public TopologyHostTaskEntity merge(TopologyHostTaskEntity requestEntity) {
    return entityManagerProvider.get().merge(requestEntity);
  }

  @Transactional
  public void remove(TopologyHostTaskEntity requestEntity) {
    entityManagerProvider.get().remove(requestEntity);
  }
}

