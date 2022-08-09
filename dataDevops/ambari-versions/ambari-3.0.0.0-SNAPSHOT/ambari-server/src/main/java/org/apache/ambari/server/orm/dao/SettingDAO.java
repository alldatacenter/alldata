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
import org.apache.ambari.server.orm.entities.SettingEntity;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class SettingDAO {

  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  /**
   * Find a setting with the given name.
   *
   * @param name - name of setting.
   * @return  a matching setting or null
   */
  @RequiresSession
  public SettingEntity findByName(String name) {
    if (StringUtils.isBlank(name)) {
      return null;
    }
    TypedQuery<SettingEntity> query = entityManagerProvider.get()
            .createNamedQuery("settingByName", SettingEntity.class);
    query.setParameter("name", name);
    return daoUtils.selectOne(query);
  }

  /**
   * Find all settings.
   *
   * @return all setting instances.
   */
  @RequiresSession
  public List<SettingEntity> findAll() {
    return daoUtils.selectAll(entityManagerProvider.get(), SettingEntity.class);
  }

  /**
   * Create a new setting entity.
   *
   * @param entity - entity to be created
   */
  @Transactional
  public void create(SettingEntity entity) {
    entityManagerProvider.get().persist(entity);
  }

  /**
   * Update setting instance.
   *
   * @param entity - entity to be updated.
   * @return - updated entity.
   */
  @Transactional
  public SettingEntity merge(SettingEntity entity) {
    return entityManagerProvider.get().merge(entity);
  }

  /**
   * Delete setting with given name.
   *
   * @param name - name of setting to be deleted.
   */
  @Transactional
  public void removeByName(String name) {
    SettingEntity entity = findByName(name);
    if (entity!= null) {
      entityManagerProvider.get().remove(entity);
    }
  }
}
