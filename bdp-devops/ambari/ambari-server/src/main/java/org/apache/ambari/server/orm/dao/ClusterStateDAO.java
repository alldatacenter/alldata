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
import org.apache.ambari.server.orm.entities.ClusterStateEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class ClusterStateDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  @RequiresSession
  public ClusterStateEntity findByPK(long clusterId) {
    return entityManagerProvider.get().find(ClusterStateEntity.class, clusterId);
  }

  @RequiresSession
  public List<ClusterStateEntity> findAll() {
    return daoUtils.selectAll(entityManagerProvider.get(), ClusterStateEntity.class);
  }

  @Transactional
  public void refresh(ClusterStateEntity clusterStateEntity) {
    entityManagerProvider.get().refresh(clusterStateEntity);
  }

  @Transactional
  public void create(ClusterStateEntity clusterStateEntity) {
    entityManagerProvider.get().persist(clusterStateEntity);
  }

  @Transactional
  public ClusterStateEntity merge(ClusterStateEntity clusterStateEntity) {
    return entityManagerProvider.get().merge(clusterStateEntity);
  }

  @Transactional
  public void remove(ClusterStateEntity clusterStateEntity) {
    entityManagerProvider.get().remove(merge(clusterStateEntity));
  }

  @Transactional
  public void removeByPK(long clusterId) {
    remove(findByPK(clusterId));
  }

}
