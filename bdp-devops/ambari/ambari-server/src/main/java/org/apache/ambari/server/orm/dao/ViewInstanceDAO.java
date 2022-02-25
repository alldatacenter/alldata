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
import org.apache.ambari.server.orm.entities.ViewInstanceDataEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * View Instance Data Access Object.
 */
@Singleton
public class ViewInstanceDAO {
  /**
   * JPA entity manager
   */
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  /**
   * Find a view with the given names.
   *
   * @param viewName      name of view
   * @param instanceName  name of the instance
   *
   * @return  a matching view instance or null
   */
  @RequiresSession
  public ViewInstanceEntity findByName(String viewName, String instanceName) {
    TypedQuery<ViewInstanceEntity> query = entityManagerProvider.get().createQuery(
      "SELECT instance FROM ViewInstanceEntity instance WHERE instance.viewName = ?1 AND instance.name = ?2",
      ViewInstanceEntity.class);
    return daoUtils.selectSingle(query, viewName, instanceName);
  }

  /**
   * Find a view instance by given resource id.
   *
   * @param resourceId resource id
   * @return a matching view instance or null
   */
  @RequiresSession
  public ViewInstanceEntity findByResourceId(long resourceId) {
    TypedQuery<ViewInstanceEntity> query = entityManagerProvider.get().createNamedQuery("viewInstanceByResourceId", ViewInstanceEntity.class);
    query.setParameter("resourceId", resourceId);
    try {
      return query.getSingleResult();
    } catch (NoResultException ignored) {
      return null;
    }
  }

  /**
   * Find all view instances.
   *
   * @return all views or an empty List
   */
  @RequiresSession
  public List<ViewInstanceEntity> findAll() {
    TypedQuery<ViewInstanceEntity> query = entityManagerProvider.get().
        createNamedQuery("allViewInstances", ViewInstanceEntity.class);

    return query.getResultList();
  }

  /**
   * Gets the associated {@link ResourceEntity} for a given instance.
   *
   * @param viewName
   *          the name of the view
   * @param instanceName
   *          the name of the view instance
   *
   * @return the associated resource entity or {@code null}.
   */
  @RequiresSession
  public ResourceEntity findResourceForViewInstance(String viewName,
      String instanceName) {
    TypedQuery<ResourceEntity> query = entityManagerProvider.get().createNamedQuery(
        "getResourceIdByViewInstance", ResourceEntity.class);

    query.setParameter("viewName", viewName);
    query.setParameter("instanceName", instanceName);

    return daoUtils.selectOne(query);
  }

  /**
   * Refresh the state of the instance from the database,
   * overwriting changes made to the entity, if any.
   *
   * @param viewInstanceEntity  entity to refresh
   */
  @Transactional
  public void refresh(ViewInstanceEntity viewInstanceEntity) {
    entityManagerProvider.get().refresh(viewInstanceEntity);
  }

  /**
   * Make an instance managed and persistent.
   *
   * @param viewInstanceEntity  entity to persist
   */
  @Transactional
  public void create(ViewInstanceEntity viewInstanceEntity) {
    entityManagerProvider.get().persist(viewInstanceEntity);
  }

  /**
   * Merge the state of the given entity into the current persistence context.
   *
   * @param viewInstanceEntity  entity to merge
   * @return the merged entity
   */
  @Transactional
  public ViewInstanceEntity merge(ViewInstanceEntity viewInstanceEntity) {
    return entityManagerProvider.get().merge(viewInstanceEntity);
  }

  /**
   * Remove the entity instance.
   *
   * @param viewInstanceEntity  entity to remove
   */
  @Transactional
  public void remove(ViewInstanceEntity viewInstanceEntity) {
    entityManagerProvider.get().remove(merge(viewInstanceEntity));
  }

  /**
   * Merge the state of the given entity data into the current persistence context.
   *
   * @param viewInstanceDataEntity  entity to merge
   * @return the merged entity
   */
  @Transactional
  public ViewInstanceDataEntity mergeData(ViewInstanceDataEntity viewInstanceDataEntity) {
    return entityManagerProvider.get().merge(viewInstanceDataEntity);
  }

  /**
   * Remove the entity instance data.
   *
   * @param viewInstanceDataEntity  entity to remove
   */
  @Transactional
  public void removeData(ViewInstanceDataEntity viewInstanceDataEntity) {
    entityManagerProvider.get().remove(mergeData(viewInstanceDataEntity));
  }
}
