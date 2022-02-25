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
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.apache.ambari.server.orm.entities.StackEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;


/**
 * Blueprint Data Access Object.
 */
@Singleton
public class BlueprintDAO {

  /**
   * JPA entity manager
   */
  @Inject
  Provider<EntityManager> entityManagerProvider;

  @Inject
  StackDAO stackDAO;

  /**
   * Find a blueprint with a given name.
   *
   * @param blueprint_name name of blueprint to find
   *
   * @return  a matching blueprint or null
   */
  @RequiresSession
  public BlueprintEntity findByName(String blueprint_name) {
    return entityManagerProvider.get().find(BlueprintEntity.class, blueprint_name);
  }

  /**
   * Find all blueprints.
   *
   * @return all blueprints or an empty List
   */
  @RequiresSession
  public List<BlueprintEntity> findAll() {
    TypedQuery<BlueprintEntity> query = entityManagerProvider.get().
        createNamedQuery("allBlueprints", BlueprintEntity.class);

    return query.getResultList();
  }

  /**
   * Refresh the state of the instance from the database,
   * overwriting changes made to the entity, if any.
   *
   * @param blueprintEntity  entity to refresh
   */
  @Transactional
  public void refresh(BlueprintEntity blueprintEntity) {
    ensureStackIdSet(blueprintEntity);
    entityManagerProvider.get().refresh(blueprintEntity);
  }

  /**
   * Make an instance managed and persistent.
   *
   * @param blueprintEntity  entity to persist
   */
  @Transactional
  public void create(BlueprintEntity blueprintEntity) {
    ensureStackIdSet(blueprintEntity);
    entityManagerProvider.get().persist(blueprintEntity);
  }

  /**
   * Merge the state of the given entity into the current persistence context.
   *
   * @param blueprintEntity  entity to merge
   * @return the merged entity
   */
  @Transactional
  public BlueprintEntity merge(BlueprintEntity blueprintEntity) {
    ensureStackIdSet(blueprintEntity);
    return entityManagerProvider.get().merge(blueprintEntity);
  }

  /**
   * Remove the entity instance.
   *
   * @param blueprintEntity  entity to remove
   */
  @Transactional
  public void remove(BlueprintEntity blueprintEntity) {
    ensureStackIdSet(blueprintEntity);
    entityManagerProvider.get().remove(merge(blueprintEntity));
  }

  /**
   * Remove entity instance by primary key
   * @param blueprint_name Primary key: blueprint name
   */
  @Transactional
  public void removeByName(String blueprint_name) {
    entityManagerProvider.get().remove(findByName(blueprint_name));
  }

  private void ensureStackIdSet(BlueprintEntity entity) {
    StackEntity stack = entity.getStack();
    if (stack != null && stack.getStackId() == null) {
      entity.setStack(stackDAO.find(stack.getStackName(), stack.getStackVersion()));
    }
  }
}
