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
import org.apache.ambari.server.orm.entities.RoleSuccessCriteriaEntity;
import org.apache.ambari.server.orm.entities.RoleSuccessCriteriaEntityPK;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class RoleSuccessCriteriaDAO {

  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  @RequiresSession
  public RoleSuccessCriteriaEntity findByPK(RoleSuccessCriteriaEntityPK roleSuccessCriteriaEntityPK) {
    entityManagerProvider.get().clear();
    return entityManagerProvider.get().find(RoleSuccessCriteriaEntity.class, roleSuccessCriteriaEntityPK);
  }

  @RequiresSession
  public List<RoleSuccessCriteriaEntity> findAll() {
    return daoUtils.selectAll(entityManagerProvider.get(), RoleSuccessCriteriaEntity.class);
  }

  @Transactional
  public void create(RoleSuccessCriteriaEntity stageEntity) {
    entityManagerProvider.get().persist(stageEntity);
  }

  @Transactional
  public RoleSuccessCriteriaEntity merge(RoleSuccessCriteriaEntity stageEntity) {
    return entityManagerProvider.get().merge(stageEntity);
  }

  @Transactional
  public void remove(RoleSuccessCriteriaEntity stageEntity) {
    entityManagerProvider.get().remove(merge(stageEntity));
  }

  @Transactional
  public void removeByPK(RoleSuccessCriteriaEntityPK roleSuccessCriteriaEntityPK) {
    remove(findByPK(roleSuccessCriteriaEntityPK));
  }
}
