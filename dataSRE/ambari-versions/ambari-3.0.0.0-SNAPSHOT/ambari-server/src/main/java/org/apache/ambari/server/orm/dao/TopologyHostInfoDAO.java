/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.orm.dao;

import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.TopologyHostInfoEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class TopologyHostInfoDAO {

  @Inject
  Provider<EntityManager> entityManagerProvider;

  /**
   * Looks for TopologyHostInfo by ID
   * @param hostInfoId ID of topology host info
   * @return Found entity or NULL
   */
  @RequiresSession
  public TopologyHostInfoEntity findById(long hostInfoId) {
    return entityManagerProvider.get().find(TopologyHostInfoEntity.class, hostInfoId);
  }

  @RequiresSession
  public List<TopologyHostInfoEntity> findByHost(HostEntity host) {
    TypedQuery<TopologyHostInfoEntity> query = entityManagerProvider.get().createQuery(
        "SELECT hostInfo FROM TopologyHostInfoEntity hostInfo where hostInfo.hostEntity=:host", TopologyHostInfoEntity.class);
    query.setParameter("host", host);
    try {
      return query.getResultList();
    } catch (NoResultException ignored) {
      return null;
    }
  }

  @RequiresSession
  public List<TopologyHostInfoEntity> findAll() {
    TypedQuery<TopologyHostInfoEntity> query = entityManagerProvider.get().createQuery("SELECT hostInfo FROM TopologyHostInfoEntity hostInfo", TopologyHostInfoEntity.class);
    try {
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  /**
   * Refreshes entity state from database
   * @param topologyHostInfoEntity entity to refresh
   */
  @Transactional
  public void refresh(TopologyHostInfoEntity topologyHostInfoEntity) {
    entityManagerProvider.get().refresh(topologyHostInfoEntity);
  }

  @Transactional
  public void create(TopologyHostInfoEntity topologyHostInfoEntity) {
    entityManagerProvider.get().persist(topologyHostInfoEntity);
  }

  @Transactional
  public TopologyHostInfoEntity merge(TopologyHostInfoEntity topologyHostInfoEntity) {
    return entityManagerProvider.get().merge(topologyHostInfoEntity);
  }

  @Transactional
  public void remove(TopologyHostInfoEntity topologyHostInfoEntity) {
    entityManagerProvider.get().remove(merge(topologyHostInfoEntity));
  }

  @Transactional
  public void removeByHost(HostEntity host) {
    for(TopologyHostInfoEntity e : findByHost(host)) {
      entityManagerProvider.get().remove(merge(e));
    }
  }

  public TopologyHostInfoEntity findByHostname(String hostName) {
    TypedQuery<TopologyHostInfoEntity> query = entityManagerProvider.get().createQuery(
      "SELECT hostInfo FROM TopologyHostInfoEntity hostInfo where hostInfo.fqdn=:hostName", TopologyHostInfoEntity.class);
    query.setParameter("hostName", hostName);
    try {
      return query.getSingleResult();
    } catch (NoResultException ignored) {
      return null;
    }
  }
}
