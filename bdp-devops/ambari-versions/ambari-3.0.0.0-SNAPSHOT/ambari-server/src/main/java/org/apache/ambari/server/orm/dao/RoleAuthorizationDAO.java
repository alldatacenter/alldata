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
 * See the License for the specific language governing authorizations and
 * limitations under the License.
 */

package org.apache.ambari.server.orm.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.RoleAuthorizationEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * Authorization (definition) Data Access Object.
 */
@Singleton
public class RoleAuthorizationDAO {

  /**
   * JPA entity manager
   */
  @Inject
  Provider<EntityManager> entityManagerProvider;

  @Inject
  DaoUtils daoUtils;

  /**
   * Create a new role authorization.
   *
   * @param roleAuthorizationEntity  entity to store
   */
  @Transactional
  public void create(RoleAuthorizationEntity roleAuthorizationEntity) {
    entityManagerProvider.get().persist(roleAuthorizationEntity);
  }

  /**
   * Create or updates a role authorization.
   *
   * @param roleAuthorizationEntity  entity to create or update
   */
  @Transactional
  public RoleAuthorizationEntity merge(RoleAuthorizationEntity roleAuthorizationEntity) {
    return entityManagerProvider.get().merge(roleAuthorizationEntity);
  }

  /**
   * Find a authorization entity with the given id.
   *
   * @param id type id
   * @return a matching authorization entity or null
   */
  @RequiresSession
  public RoleAuthorizationEntity findById(String id) {
    return entityManagerProvider.get().find(RoleAuthorizationEntity.class, id);
  }

  /**
   * Find all authorization entities.
   *
   * @return all entities or an empty List
   */
  @RequiresSession
  public List<RoleAuthorizationEntity> findAll() {
    TypedQuery<RoleAuthorizationEntity> query = entityManagerProvider.get().createNamedQuery("findAll", RoleAuthorizationEntity.class);
    return daoUtils.selectList(query);
  }
}
