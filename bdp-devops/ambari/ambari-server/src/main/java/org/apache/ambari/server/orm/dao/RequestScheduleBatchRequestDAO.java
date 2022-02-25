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
import org.apache.ambari.server.orm.entities.RequestScheduleBatchRequestEntity;
import org.apache.ambari.server.orm.entities.RequestScheduleBatchRequestEntityPK;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class RequestScheduleBatchRequestDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  @RequiresSession
  public RequestScheduleBatchRequestEntity findByPk
    (RequestScheduleBatchRequestEntityPK batchRequestEntity) {

    return entityManagerProvider.get()
      .find(RequestScheduleBatchRequestEntity.class, batchRequestEntity);
  }

  @RequiresSession
  public List<RequestScheduleBatchRequestEntity> findByScheduleId(Long scheduleId) {
    TypedQuery<RequestScheduleBatchRequestEntity> query = entityManagerProvider
      .get().createNamedQuery("findByScheduleId",
        RequestScheduleBatchRequestEntity.class);

    query.setParameter("id", scheduleId);
    try {
      return query.getResultList();
    } catch (NoResultException ignored) {
    }
    return null;
  }

  @RequiresSession
  public List<RequestScheduleBatchRequestEntity> findAll() {
    return daoUtils.selectAll(entityManagerProvider.get(), RequestScheduleBatchRequestEntity.class);
  }

  @Transactional
  public void create(RequestScheduleBatchRequestEntity batchRequestEntity) {
    entityManagerProvider.get().persist(batchRequestEntity);
  }

  @Transactional
  public RequestScheduleBatchRequestEntity merge(RequestScheduleBatchRequestEntity batchRequestEntity) {

    return entityManagerProvider.get().merge(batchRequestEntity);
  }

  @Transactional
  public void refresh(RequestScheduleBatchRequestEntity batchRequestEntity) {
    entityManagerProvider.get().refresh(batchRequestEntity);
  }

  @Transactional
  public void remove(RequestScheduleBatchRequestEntity batchRequestEntity) {
    entityManagerProvider.get().remove(merge(batchRequestEntity));
  }

  @Transactional
  public void removeByPk(RequestScheduleBatchRequestEntityPK
                             batchRequestEntityPK) {
    entityManagerProvider.get().remove(findByPk(batchRequestEntityPK));
  }

  @Transactional
  public void removeByScheduleId(Long scheduleId) {
    TypedQuery<Long> query = entityManagerProvider.get().createQuery
      ("DELETE FROM RequestScheduleBatchRequestEntity batchreq WHERE " +
        "batchreq.scheduleId = ?1", Long.class);

    daoUtils.executeUpdate(query, scheduleId);
    // Flush to current transaction required in order to avoid Eclipse link
    // from re-ordering delete
    entityManagerProvider.get().flush();
  }
}
