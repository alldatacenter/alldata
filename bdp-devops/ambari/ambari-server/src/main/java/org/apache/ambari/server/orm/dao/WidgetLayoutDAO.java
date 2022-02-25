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
import org.apache.ambari.server.orm.entities.WidgetLayoutEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class WidgetLayoutDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;

  /**
   * DAO utilities for dealing mostly with {@link TypedQuery} results.
   */
  @Inject
  private DaoUtils daoUtils;

  @RequiresSession
  public WidgetLayoutEntity findById(Long id) {
    return entityManagerProvider.get().find(WidgetLayoutEntity.class, id);
  }

  @RequiresSession
  public List<WidgetLayoutEntity> findByCluster(long clusterId) {
    TypedQuery<WidgetLayoutEntity> query = entityManagerProvider.get()
            .createNamedQuery("WidgetLayoutEntity.findByCluster", WidgetLayoutEntity.class);
    query.setParameter("clusterId", clusterId);

    return daoUtils.selectList(query);
  }

  @RequiresSession
  public List<WidgetLayoutEntity> findBySectionName(String sectionName) {
    TypedQuery<WidgetLayoutEntity> query = entityManagerProvider.get()
            .createNamedQuery("WidgetLayoutEntity.findBySectionName", WidgetLayoutEntity.class);
    query.setParameter("sectionName", sectionName);

    return daoUtils.selectList(query);
  }

  @RequiresSession
  public List<WidgetLayoutEntity> findByName(Long clusterId, String layoutName, String userName) {
    TypedQuery<WidgetLayoutEntity> query = entityManagerProvider.get()
      .createNamedQuery("WidgetLayoutEntity.findByName", WidgetLayoutEntity.class);
    query.setParameter("clusterId", clusterId);
    query.setParameter("layoutName", layoutName);
    query.setParameter("userName", userName);

    return daoUtils.selectList(query);
  }

  @RequiresSession
  public List<WidgetLayoutEntity> findAll() {
    TypedQuery<WidgetLayoutEntity> query = entityManagerProvider.get()
            .createNamedQuery("WidgetLayoutEntity.findAll", WidgetLayoutEntity.class);

    return daoUtils.selectList(query);
  }

  @Transactional
  public void create(WidgetLayoutEntity widgetLayoutEntity) {
    entityManagerProvider.get().persist(widgetLayoutEntity);
  }

  @Transactional
  public void createWithFlush(WidgetLayoutEntity widgetLayoutEntity) {
    EntityManager entityManager = entityManagerProvider.get();
    entityManager.persist(widgetLayoutEntity);
    entityManager.flush();
    entityManager.refresh(widgetLayoutEntity);
  }

  @Transactional
  public WidgetLayoutEntity merge(WidgetLayoutEntity widgetLayoutEntity) {
    return entityManagerProvider.get().merge(widgetLayoutEntity);
  }

  @Transactional
  public WidgetLayoutEntity mergeWithFlush(WidgetLayoutEntity widgetLayoutEntity) {
    EntityManager entityManager = entityManagerProvider.get();
    widgetLayoutEntity = entityManager.merge(widgetLayoutEntity);
    entityManager.flush();
    entityManager.refresh(widgetLayoutEntity);
    return widgetLayoutEntity;
  }

  @Transactional
  public void remove(WidgetLayoutEntity widgetLayoutEntity) {
    entityManagerProvider.get().remove(merge(widgetLayoutEntity));
  }

  @Transactional
  public void removeByPK(Long id) {
    entityManagerProvider.get().remove(findById(id));
  }

  @Transactional
  public void refresh(WidgetLayoutEntity widgetLayoutEntity) {
    entityManagerProvider.get().refresh(widgetLayoutEntity);
  }
}
