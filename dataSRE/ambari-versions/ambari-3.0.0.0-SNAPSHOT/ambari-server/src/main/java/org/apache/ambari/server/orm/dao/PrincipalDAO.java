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

import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.PrincipalEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * Principal Data Access Object.
 */
@Singleton
public class PrincipalDAO {
  /**
   * JPA entity manager
   */
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  /**
   * Find a principal with the given id.
   *
   * @param id  type id
   *
   * @return  a matching principal type  or null
   */
  @RequiresSession
  public PrincipalEntity findById(Long id) {
    return entityManagerProvider.get().find(PrincipalEntity.class, id);
  }

  /**
   * Find all principals.
   *
   * @return all principals or an empty List
   */
  @RequiresSession
  public List<PrincipalEntity> findAll() {
    TypedQuery<PrincipalEntity> query = entityManagerProvider.get().createQuery("SELECT principal FROM PrincipalEntity principal", PrincipalEntity.class);
    return daoUtils.selectList(query);
  }

  /**
   * Find principals having specified permission.
   *
   * @param id permission id
   * @return all principals having specified permission
   */
  @RequiresSession
  public List<PrincipalEntity> findByPermissionId(Integer id) {
    TypedQuery<PrincipalEntity> query = entityManagerProvider.get().createNamedQuery("principalByPrivilegeId", PrincipalEntity.class);
    query.setParameter("permission_id", id);
    return daoUtils.selectList(query);
  }


  @RequiresSession
  public List<PrincipalEntity> findByPrincipalType(String name) {
    TypedQuery<PrincipalEntity> query = entityManagerProvider.get().createNamedQuery("principalByPrincipalType", PrincipalEntity.class);
    query.setParameter("principal_type", name);
    return  daoUtils.selectList(query);
  }

  /**
   * Make an instance managed and persistent.
   *
   * @param entity  entity to store
   */
  @Transactional
  public void create(PrincipalEntity entity) {
    create(Arrays.asList(entity));
  }

  /**
   * Make instances managed and persistent.
   *
   * @param entities entities to store
   */
  @Transactional
  public void create(List<PrincipalEntity> entities) {
    for (PrincipalEntity entity: entities) {
      entityManagerProvider.get().persist(entity);
    }
  }

  /**
   * Merge the given entity.
   *
   * @param entity  the entity
   *
   * @return the managed entity
   */
  @Transactional
  public PrincipalEntity merge(PrincipalEntity entity) {
    return entityManagerProvider.get().merge(entity);
  }

  /**
   * Remove the entity instance.
   *
   * @param entity  entity to remove
   */
  @Transactional
  public void remove(PrincipalEntity entity) {
    entityManagerProvider.get().remove(entity);
  }

}
