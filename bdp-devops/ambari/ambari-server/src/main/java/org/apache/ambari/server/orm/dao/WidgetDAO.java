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
import org.apache.ambari.server.orm.entities.WidgetEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class WidgetDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;

  /**
   * DAO utilities for dealing mostly with {@link TypedQuery} results.
   */
  @Inject
  private DaoUtils daoUtils;

  @RequiresSession
  public WidgetEntity findById(Long id) {
    return entityManagerProvider.get().find(WidgetEntity.class, id);
  }

  @RequiresSession
  public List<WidgetEntity> findByCluster(long clusterId) {
    TypedQuery<WidgetEntity> query = entityManagerProvider.get()
            .createNamedQuery("WidgetEntity.findByCluster", WidgetEntity.class);
    query.setParameter("clusterId", clusterId);

    return daoUtils.selectList(query);
  }

  @RequiresSession
  public List<WidgetEntity> findBySectionName(String sectionName) {
    TypedQuery<WidgetEntity> query = entityManagerProvider.get()
            .createNamedQuery("WidgetEntity.findBySectionName", WidgetEntity.class);
    query.setParameter("sectionName", sectionName);

    return daoUtils.selectList(query);
  }

  @RequiresSession
  public List<WidgetEntity> findByName(Long clusterId, String widgetName, String author, String defaultSectionName) {
    TypedQuery<WidgetEntity> query = entityManagerProvider.get()
      .createNamedQuery("WidgetEntity.findByName", WidgetEntity.class);
    query.setParameter("clusterId", clusterId);
    query.setParameter("widgetName", widgetName);
    query.setParameter("author", author);
    query.setParameter("defaultSectionName", defaultSectionName);

    return daoUtils.selectList(query);
  }

  @RequiresSession
  public List<WidgetEntity> findByScopeOrAuthor(String author, String scope) {
    TypedQuery<WidgetEntity> query = entityManagerProvider.get()
            .createNamedQuery("WidgetEntity.findByScopeOrAuthor", WidgetEntity.class);
    query.setParameter("author", author);
    query.setParameter("scope", scope);

    return daoUtils.selectList(query);
  }

  @RequiresSession
  public List<WidgetEntity> findAll() {
    TypedQuery<WidgetEntity> query = entityManagerProvider.get()
            .createNamedQuery("WidgetEntity.findAll", WidgetEntity.class);

    return daoUtils.selectList(query);
  }

  @Transactional
  public void create(WidgetEntity widgetEntity) {
    entityManagerProvider.get().persist(widgetEntity);
  }

  @Transactional
  public WidgetEntity merge(WidgetEntity widgetEntity) {
    return entityManagerProvider.get().merge(widgetEntity);
  }

  @Transactional
  public void remove(WidgetEntity widgetEntity) {
    entityManagerProvider.get().remove(merge(widgetEntity));
  }

  @Transactional
  public void removeByPK(Long id) {
    entityManagerProvider.get().remove(findById(id));
  }

  @Transactional
  public void refresh(WidgetEntity widgetEntity) {
    entityManagerProvider.get().refresh(widgetEntity);
  }
}
