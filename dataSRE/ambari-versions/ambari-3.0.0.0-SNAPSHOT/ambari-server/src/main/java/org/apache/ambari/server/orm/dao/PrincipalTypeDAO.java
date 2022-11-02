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
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * Principal Type Data Access Object.
 */
@Singleton
public class PrincipalTypeDAO {
  /**
   * JPA entity manager
   */
  @Inject
  Provider<EntityManager> entityManagerProvider;

  /**
   * Utilities.
   */
  @Inject
  DaoUtils daoUtils;

  /**
   * Find a principal type with the given id.
   *
   * @param id  type id
   *
   * @return  a matching principal type  or null
   */
  @RequiresSession
  public PrincipalTypeEntity findById(Integer id) {
    return entityManagerProvider.get().find(PrincipalTypeEntity.class, id);
  }

  /**
   * Find a principal type entity with the given name.
   *
   * @param name  principal type name
   *
   * @return  a matching principal type entity or null
   */
  @RequiresSession
  public PrincipalTypeEntity findByName(String name) {
    TypedQuery<PrincipalTypeEntity> query = entityManagerProvider.get().createNamedQuery("PrincipalTypeEntity.findByName", PrincipalTypeEntity.class);
    query.setParameter("name", name);
    return daoUtils.selectSingle(query);
  }

  /**
   * Find all principal types.
   *
   * @return all principal types or an empty List
   */
  @RequiresSession
  public List<PrincipalTypeEntity> findAll() {
    TypedQuery<PrincipalTypeEntity> query = entityManagerProvider.get().createQuery("SELECT principalType FROM PrincipalTypeEntity principalType", PrincipalTypeEntity.class);
    return daoUtils.selectList(query);
  }

  /**
   * Make an instance managed and persistent.
   *
   * @param entity  entity to store
   */
  @Transactional
  public void create(PrincipalTypeEntity entity) {
    entityManagerProvider.get().persist(entity);
  }

  @Transactional
  public PrincipalTypeEntity merge(PrincipalTypeEntity entity) {
    return entityManagerProvider.get().merge(entity);
  }

  /**
   * Remove the entity instance.
   *
   * @param entity entity to remove
   */
  @Transactional
  public void remove(PrincipalTypeEntity entity) {
    entityManagerProvider.get().remove(entity);
  }

  /**
   * Creates and returns principal type if it wasn't persisted yet.
   *
   * @param principalType id of principal type
   * @return principal type
   */
  @RequiresSession
  public PrincipalTypeEntity ensurePrincipalTypeCreated(int principalType) {
    PrincipalTypeEntity principalTypeEntity = findById(principalType);
    if (principalTypeEntity == null) {
      principalTypeEntity = new PrincipalTypeEntity();
      principalTypeEntity.setId(principalType);
      switch (principalType) {
        case PrincipalTypeEntity.USER_PRINCIPAL_TYPE:
          principalTypeEntity.setName(PrincipalTypeEntity.USER_PRINCIPAL_TYPE_NAME);
          break;
        case PrincipalTypeEntity.GROUP_PRINCIPAL_TYPE:
          principalTypeEntity.setName(PrincipalTypeEntity.GROUP_PRINCIPAL_TYPE_NAME);
          break;
        case PrincipalTypeEntity.ROLE_PRINCIPAL_TYPE:
          principalTypeEntity.setName(PrincipalTypeEntity.ROLE_PRINCIPAL_TYPE_NAME);
          break;
        default:
          throw new IllegalArgumentException("Unknown principal type ID=" + principalType);
      }
      create(principalTypeEntity);
    }
    return principalTypeEntity;
  }
}

