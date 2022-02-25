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
import org.apache.ambari.server.orm.entities.ViewURLEntity;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * View Instance Data Access Object.
 */
@Singleton
public class ViewURLDAO {
  /**
   * JPA entity manager
   */
  @Inject
  private Provider<EntityManager> entityManagerProvider;

  /**
   * Find all view instances.
   *
   * @return all views or an empty List
   */
  @RequiresSession
  public List<ViewURLEntity> findAll() {
    TypedQuery<ViewURLEntity> query = entityManagerProvider.get().
        createNamedQuery("allViewUrls", ViewURLEntity.class);

    return query.getResultList();
  }

  /**
   * Find URL by name
   * @param urlName
   * @return
     */
  @RequiresSession
  public Optional<ViewURLEntity> findByName(String urlName) {
    TypedQuery<ViewURLEntity> query = entityManagerProvider.get().
            createNamedQuery("viewUrlByName", ViewURLEntity.class);
    query.setParameter("urlName", urlName);
    try {
      return Optional.of(query.getSingleResult());
    } catch (Exception e){
      return Optional.absent();
    }
  }

  /**
   * Find URL by suffix
   *
   * @param urlSuffix
   *          the suffix to get the URL by
   * @return <code>Optional.absent()</code> if no view URL with the given suffix;
   *         otherwise an appropriate <code>Optional</code> instance holding the
   *         fetched view URL
   */
  @RequiresSession
  public Optional<ViewURLEntity> findBySuffix(String urlSuffix) {
    TypedQuery<ViewURLEntity> query = entityManagerProvider.get().createNamedQuery("viewUrlBySuffix", ViewURLEntity.class);
    query.setParameter("urlSuffix", urlSuffix);
    try {
      return Optional.of(query.getSingleResult());
    } catch (NoResultException e) {
      return Optional.absent();
    }
  }

  /**
   * Save a URL entity
   * @param urlEntity
     */
  @Transactional
  public void save(ViewURLEntity urlEntity) {
    entityManagerProvider.get().persist(urlEntity);
    // Reverse mappings are not automatically flushed for some reason
    entityManagerProvider.get().flush();
  }

  /**
   * Update and merge a URL entity
   * @param entity
     */
  @Transactional
  public void update(ViewURLEntity entity) {
    entityManagerProvider.get().merge(entity);
    entityManagerProvider.get().flush();

  }

  /**
   * Remove a URL entity
   * @param urlEntity
     */
  @Transactional
  public void delete(ViewURLEntity urlEntity) {
    entityManagerProvider.get().remove(urlEntity);
    entityManagerProvider.get().flush();
  }
}
