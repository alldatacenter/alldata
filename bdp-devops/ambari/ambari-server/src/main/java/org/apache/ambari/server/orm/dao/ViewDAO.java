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
import org.apache.ambari.server.orm.entities.ViewEntity;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * View Data Access Object.
 */
@Singleton
public class ViewDAO {
  /**
   * JPA entity manager
   */
  @Inject
  Provider<EntityManager> entityManagerProvider;

  /**
   * Find a view with a given name.
   *
   * @param viewName name of view to find
   *
   * @return  a matching view or null
   */
  @RequiresSession
  public ViewEntity findByName(String viewName) {
    return entityManagerProvider.get().find(ViewEntity.class, viewName);
  }

  /**
   * Find a view with a given common name.
   *
   * @param viewCommonName common name of view to find
   *
   * @return  a matching view or null
   */
  @RequiresSession
  public List<ViewEntity> findByCommonName(String viewCommonName) {
    List<ViewEntity> list = Lists.newArrayList();
    if (viewCommonName != null) {
      for (ViewEntity viewEntity : findAll()) {
        if (viewCommonName.equals(viewEntity.getCommonName())) {
          list.add(viewEntity);
        }
      }
    }
    return list;
  }

  /**
   * Find all views.
   *
   * @return all views or an empty List
   */
  @RequiresSession
  public List<ViewEntity> findAll() {
    TypedQuery<ViewEntity> query = entityManagerProvider.get().
        createNamedQuery("allViews", ViewEntity.class);

    return query.getResultList();
  }

  /**
   * Refresh the state of the instance from the database,
   * overwriting changes made to the entity, if any.
   *
   * @param ViewEntity  entity to refresh
   */
  @Transactional
  public void refresh(ViewEntity ViewEntity) {
    entityManagerProvider.get().refresh(ViewEntity);
  }

  /**
   * Make an instance managed and persistent.
   *
   * @param ViewEntity  entity to store
   */
  @Transactional
  public void create(ViewEntity ViewEntity) {
    entityManagerProvider.get().persist(ViewEntity);
  }

  /**
   * Merge the state of the given entity into the current persistence context.
   *
   * @param ViewEntity  entity to merge
   * @return the merged entity
   */
  @Transactional
  public ViewEntity merge(ViewEntity ViewEntity) {
    return entityManagerProvider.get().merge(ViewEntity);
  }

  /**
   * Remove the entity instance.
   *
   * @param ViewEntity  entity to remove
   */
  @Transactional
  public void remove(ViewEntity ViewEntity) {
    entityManagerProvider.get().remove(merge(ViewEntity));
  }
}
