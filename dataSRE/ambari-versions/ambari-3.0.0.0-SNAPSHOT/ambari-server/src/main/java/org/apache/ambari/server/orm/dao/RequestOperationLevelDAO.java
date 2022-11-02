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
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.RequestOperationLevelEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class RequestOperationLevelDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;


  @RequiresSession
  public List<RequestOperationLevelEntity> findByHostId(Long hostId) {
    final TypedQuery<RequestOperationLevelEntity> query = entityManagerProvider.get()
        .createNamedQuery("requestOperationLevelByHostId", RequestOperationLevelEntity.class);
    query.setParameter("hostId", hostId);
    return daoUtils.selectList(query);
  }

  @RequiresSession
  public List<RequestOperationLevelEntity> findAll() {
    return daoUtils.selectAll(entityManagerProvider.get(), RequestOperationLevelEntity.class);
  }

  @Transactional
  public void refresh(RequestOperationLevelEntity requestOperationLevelEntity) {
    entityManagerProvider.get().refresh(requestOperationLevelEntity);
  }

  @Transactional
  public void create(RequestOperationLevelEntity requestOperationLevelEntity) {
    entityManagerProvider.get().persist(requestOperationLevelEntity);
  }

  @Transactional
  public RequestOperationLevelEntity merge(RequestOperationLevelEntity requestOperationLevelEntity) {
    return entityManagerProvider.get().merge(requestOperationLevelEntity);
  }

  @Transactional
  public void remove(RequestOperationLevelEntity requestOperationLevelEntity) {
    entityManagerProvider.get().remove(merge(requestOperationLevelEntity));
  }

  @Transactional
  public void removeByHostId(Long hostId) {
    Collection<RequestOperationLevelEntity> hostRequestOpLevels = this.findByHostId(hostId);
    for (RequestOperationLevelEntity hostRequestOpLevel : hostRequestOpLevels) {
      this.remove(hostRequestOpLevel);
    }
  }

}
