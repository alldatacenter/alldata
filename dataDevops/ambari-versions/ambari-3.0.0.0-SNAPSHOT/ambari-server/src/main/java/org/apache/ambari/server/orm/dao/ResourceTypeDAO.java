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
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * Resource Type Data Access Object.
 */
@Singleton
public class ResourceTypeDAO {
  /**
   * JPA entity manager
   */
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  /**
   * Find a resource type with the given id.
   *
   * @param id  type id
   *
   * @return  a matching resource type or null
   */
  @RequiresSession
  public ResourceTypeEntity findById(Integer id) {
    return entityManagerProvider.get().find(ResourceTypeEntity.class, id);
  }

  /**
   * Find a resource type with the given name.
   *
   * @param name  type name
   *
   * @return  a matching resource type or null
   */
  @RequiresSession
  public ResourceTypeEntity findByName(String name) {
    TypedQuery<ResourceTypeEntity> query = entityManagerProvider.get().createQuery(
        "SELECT resourceType FROM ResourceTypeEntity resourceType WHERE resourceType.name = ?1",
        ResourceTypeEntity.class);
    return daoUtils.selectSingle(query, name);
  }

  /**
   * Find all resource types.
   *
   * @return all resource types or an empty List
   */
  @RequiresSession
  public List<ResourceTypeEntity> findAll() {
    TypedQuery<ResourceTypeEntity> query = entityManagerProvider.get().createQuery("SELECT resourceType FROM ResourceTypeEntity resourceType", ResourceTypeEntity.class);
    return daoUtils.selectList(query);
  }

  /**
   * Make an instance managed and persistent.
   *
   * @param entity  entity to store
   */
  @Transactional
  public void create(ResourceTypeEntity entity) {
    entityManagerProvider.get().persist(entity);
  }

  /**
   * Merge the given entity.
   *
   * @param entity  the entity
   *
   * @return the managed entity
   */
  @Transactional
  public ResourceTypeEntity merge(ResourceTypeEntity entity) {
    return entityManagerProvider.get().merge(entity);
  }

}

