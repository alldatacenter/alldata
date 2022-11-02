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
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.RequestScheduleEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class RequestScheduleDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;

  @RequiresSession
  public RequestScheduleEntity findById(Long id) {
    return entityManagerProvider.get().find(RequestScheduleEntity.class, id);
  }

  @RequiresSession
  public List<RequestScheduleEntity> findByStatus(String status) {
    TypedQuery<RequestScheduleEntity> query = entityManagerProvider.get()
      .createNamedQuery("reqScheduleByStatus", RequestScheduleEntity.class);
    query.setParameter("status", status);
    try {
      return query.getResultList();
    } catch (NoResultException ignored) {
      return null;
    }
  }

  @RequiresSession
  public List<RequestScheduleEntity> findAll() {
    TypedQuery<RequestScheduleEntity> query = entityManagerProvider.get()
      .createNamedQuery("allReqSchedules", RequestScheduleEntity.class);

    try {
      return query.getResultList();
    } catch (NoResultException ignored) {
      return null;
    }
  }

  @Transactional
  public void create(RequestScheduleEntity requestScheduleEntity) {
    entityManagerProvider.get().persist(requestScheduleEntity);
  }

  @Transactional
  public RequestScheduleEntity merge(RequestScheduleEntity requestScheduleEntity) {
    return entityManagerProvider.get().merge(requestScheduleEntity);
  }

  @Transactional
  public void remove(RequestScheduleEntity requestScheduleEntity) {
    entityManagerProvider.get().remove(merge(requestScheduleEntity));
  }

  @Transactional
  public void removeByPK(Long id) {
    entityManagerProvider.get().remove(findById(id));
  }

  @Transactional
  public void refresh(RequestScheduleEntity requestScheduleEntity) {
    entityManagerProvider.get().refresh(requestScheduleEntity);
  }
}
