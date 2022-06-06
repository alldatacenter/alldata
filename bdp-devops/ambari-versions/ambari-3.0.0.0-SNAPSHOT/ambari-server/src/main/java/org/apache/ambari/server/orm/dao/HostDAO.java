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
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.StageEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class HostDAO {

  @Inject
  Provider<EntityManager> entityManagerProvider;

  /**
   * Looks for Host by ID
   * @param hostId ID of Host
   * @return Found entity or NULL
   */
  @RequiresSession
  public HostEntity findById(long hostId) {
    return entityManagerProvider.get().find(HostEntity.class, hostId);
  }

  @RequiresSession
  public HostEntity findByName(String hostName) {
    TypedQuery<HostEntity> query = entityManagerProvider.get().createNamedQuery(
        "HostEntity.findByHostName", HostEntity.class);
    query.setParameter("hostName", hostName);
    try {
      return query.getSingleResult();
    } catch (NoResultException ignored) {
      return null;
    }
  }

  @RequiresSession
  public List<HostEntity> findAll() {
    TypedQuery<HostEntity> query = entityManagerProvider.get().createQuery("SELECT host FROM HostEntity host", HostEntity.class);
    try {
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  @RequiresSession
  public List<HostEntity> findByStage(StageEntity stageEntity) {
    TypedQuery<HostEntity> query = entityManagerProvider.get().createQuery(
        "SELECT host FROM HostEntity host " +
            "WHERE host.hostName IN (" +
            "SELECT DISTINCT host.hostName " +
            "FROM HostEntity host " +
            "JOIN host.hostRoleCommandEntities command " +
            "JOIN command.stage stage " +
            "WHERE stage=:stageEntity)", HostEntity.class);
    query.setParameter("stageEntity", stageEntity);
    try {
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  /**
   * Refreshes entity state from database
   * @param hostEntity entity to refresh
   */
  @Transactional
  public void refresh(HostEntity hostEntity) {
    entityManagerProvider.get().refresh(hostEntity);
  }

  @Transactional
  public void create(HostEntity hostEntity) {
    entityManagerProvider.get().persist(hostEntity);
  }

  @Transactional
  public HostEntity merge(HostEntity hostEntity) {
    return entityManagerProvider.get().merge(hostEntity);
  }

  @Transactional
  public void remove(HostEntity hostEntity) {
    entityManagerProvider.get().remove(hostEntity);
  }

  public List<String> getHostNamesByHostIds(List<Long> hostIds) {
    List<String> hostNames = new ArrayList<>();
    if (hostIds != null) {
      for (Long hostId : hostIds) {
        HostEntity hostEntity = findById(hostId);
        hostNames.add(hostEntity.getHostName());
      }
    }
    return hostNames;
  }
}
