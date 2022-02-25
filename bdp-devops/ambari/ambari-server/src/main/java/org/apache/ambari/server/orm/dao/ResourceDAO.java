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
import org.apache.ambari.server.orm.entities.ResourceEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * Admin resource Data Access Object.
 */
@Singleton
public class ResourceDAO {
  /**
   * JPA entity manager
   */
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  /**
   * Find a resource with the given id.
   *
   * @param id  type id
   *
   * @return  a matching resource type  or null
   */
  @RequiresSession
  public ResourceEntity findById(Long id) {
    return entityManagerProvider.get().find(ResourceEntity.class, id);
  }

  /**
   * Find a resource with the given resource type id.
   *
   * @param id  type id
   *
   * @return  a matching resource or null
   */
  @RequiresSession
  public ResourceEntity findByResourceTypeId(Integer id) {
    TypedQuery<ResourceEntity> query = entityManagerProvider.get().createQuery(
        "SELECT resource FROM ResourceEntity resource WHERE resource.resourceType.id =:resourceTypeId",
        ResourceEntity.class);
    query.setParameter("resourceTypeId", id);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Find all resources.
   *
   * @return all resources or an empty List
   */
  @RequiresSession
  public List<ResourceEntity> findAll() {
    TypedQuery<ResourceEntity> query = entityManagerProvider.get().createQuery("SELECT resource FROM ResourceEntity resource", ResourceEntity.class);
    return daoUtils.selectList(query);
  }

  /**
   * Make an instance managed and persistent.
   *
   * @param entity  entity to store
   */
  @Transactional
  public void create(ResourceEntity entity) {
    entityManagerProvider.get().persist(entity);
  }


  @Transactional
  public ResourceEntity merge(ResourceEntity entity) {
    return entityManagerProvider.get().merge(entity);
  }

  /**
   * Finds root level resource.
   *
   * @return the matching resource or null
   */
  public ResourceEntity findAmbariResource() {
    return findById(ResourceEntity.AMBARI_RESOURCE_ID);
  }
}
