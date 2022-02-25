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

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.HostStateEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class HostStateDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;


  @RequiresSession
  public HostStateEntity findByHostId(Long hostId) {
    return entityManagerProvider.get().find(HostStateEntity.class, hostId);
  }

  @RequiresSession
  public List<HostStateEntity> findAll() {
    return daoUtils.selectAll(entityManagerProvider.get(), HostStateEntity.class);
  }

  @Transactional
  public void refresh(HostStateEntity hostStateEntity) {
    entityManagerProvider.get().refresh(hostStateEntity);
  }

  @Transactional
  public void create(HostStateEntity hostStateEntity) {
    entityManagerProvider.get().persist(hostStateEntity);
  }

  @Transactional
  public HostStateEntity merge(HostStateEntity hostStateEntity) {
    return entityManagerProvider.get().merge(hostStateEntity);
  }

  @Transactional
  public void remove(HostStateEntity hostStateEntity) {
    entityManagerProvider.get().remove(merge(hostStateEntity));
  }

  @Transactional
  public void removeByHostId(Long hostId) {
    remove(findByHostId(hostId));
  }

}
