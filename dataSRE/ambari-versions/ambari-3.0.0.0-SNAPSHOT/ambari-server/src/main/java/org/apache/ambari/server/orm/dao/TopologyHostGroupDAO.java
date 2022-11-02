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
import org.apache.ambari.server.orm.entities.TopologyHostGroupEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class TopologyHostGroupDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;

  @Inject
  DaoUtils daoUtils;

  @RequiresSession
  public TopologyHostGroupEntity findById(Long id) {
    return entityManagerProvider.get().find(TopologyHostGroupEntity.class, id);
  }

  @RequiresSession
  public TopologyHostGroupEntity findByRequestIdAndName(long topologyRequestId, String name) {
    TypedQuery<TopologyHostGroupEntity> query = entityManagerProvider.get().createNamedQuery(
        "TopologyHostGroupEntity.findByRequestIdAndName", TopologyHostGroupEntity.class);

    query.setParameter("requestId", topologyRequestId);
    query.setParameter("name", name);

    return query.getSingleResult();
  }

  @RequiresSession
  public List<TopologyHostGroupEntity> findAll() {
    return daoUtils.selectAll(entityManagerProvider.get(), TopologyHostGroupEntity.class);
  }

  @Transactional
  public void create(TopologyHostGroupEntity hostGroupEntity) {
    entityManagerProvider.get().persist(hostGroupEntity);
  }

  @Transactional
  public TopologyHostGroupEntity merge(TopologyHostGroupEntity hostGroupEntity) {
    return entityManagerProvider.get().merge(hostGroupEntity);
  }

  @Transactional
  public void remove(TopologyHostGroupEntity hostGroupEntity) {
    entityManagerProvider.get().remove(hostGroupEntity);
  }
}
