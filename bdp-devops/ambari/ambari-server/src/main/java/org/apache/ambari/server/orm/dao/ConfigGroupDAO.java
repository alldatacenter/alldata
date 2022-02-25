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
import org.apache.ambari.server.orm.entities.ConfigGroupEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
@RequiresSession
public class ConfigGroupDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;

  // DAO methods on config group

  /**
   * Find config group by its unique name
   * @param groupName
   * @return ConfigGroupEntity
   */
  @RequiresSession
  public ConfigGroupEntity findByName(String groupName) {
    TypedQuery<ConfigGroupEntity> query = entityManagerProvider.get()
      .createNamedQuery("configGroupByName", ConfigGroupEntity.class);
    query.setParameter("groupName", groupName);
    try {
      return query.getSingleResult();
    } catch (NoResultException ignored) {
      return null;
    }
  }

  /**
   * Find config group by its id
   * @param id
   * @return
   */
  @RequiresSession
  public ConfigGroupEntity findById(Long id) {
    return entityManagerProvider.get().find(ConfigGroupEntity.class, id);
  }

  @RequiresSession
  public List<ConfigGroupEntity> findAll() {
    TypedQuery<ConfigGroupEntity> query = entityManagerProvider.get()
      .createNamedQuery("allConfigGroups", ConfigGroupEntity.class);
    try {
      return query.getResultList();
    } catch (NoResultException ignored) {
    }
    return null;
  }

  /**
   * Find config groups by service name and so on
   * @param tag
   * @return
   */
  @RequiresSession
  public List<ConfigGroupEntity> findAllByTag(String tag) {
    TypedQuery<ConfigGroupEntity> query = entityManagerProvider.get()
      .createNamedQuery("configGroupsByTag", ConfigGroupEntity.class);
    query.setParameter("tagName", tag);
    try {
      return query.getResultList();
    } catch (NoResultException ignored) {
    }
    return null;
  }

  @Transactional
  public void create(ConfigGroupEntity configGroupEntity) {
    entityManagerProvider.get().persist(configGroupEntity);
  }

  @Transactional
  public ConfigGroupEntity merge(ConfigGroupEntity configGroupEntity) {
    return entityManagerProvider.get().merge(configGroupEntity);
  }

  @Transactional
  public void remove(ConfigGroupEntity configGroupEntity) {
    entityManagerProvider.get().remove(merge(configGroupEntity));
  }

  @Transactional
  public void removeByPK(Long id) {
    entityManagerProvider.get().remove(findById(id));
  }

  @Transactional
  public void refresh(ConfigGroupEntity configGroupEntity) {
    entityManagerProvider.get().refresh(configGroupEntity);
  }

  // DAO methods on associated objects

}
