/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
import org.apache.ambari.server.orm.entities.MpackEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class MpackDAO {
  protected final static Logger LOG = LoggerFactory.getLogger(MpackDAO.class);

  /**
   * JPA entity manager
   */
  @Inject
  Provider<EntityManager> m_entityManagerProvider;

  /**
   * DAO utilities for dealing mostly with {@link TypedQuery} results.
   */
  @Inject
  private DaoUtils m_daoUtils;

  /**
   * Persists a new mpack
   */
  @Transactional
  public Long create(MpackEntity mpackEntity) {
    m_entityManagerProvider.get().persist(mpackEntity);
    return mpackEntity.getId();
  }

  /**
   * Gets an mpack with the specified ID.
   *
   * @param id
   *          the ID of the mpack to retrieve.
   * @return the mpack or {@code null} if none exists.
   */
  @RequiresSession
  public MpackEntity findById(long id) {
    return m_entityManagerProvider.get().find(MpackEntity.class, id);
  }

  /**
   * Gets mpacks with specified mpack name and mpack version.
   *
   * @param mpackName
   * @param mpackVersion
   * @return the mpack or {@code null} if none exists.
   */
  @RequiresSession
  public List<MpackEntity> findByNameVersion(String mpackName, String mpackVersion) {
    TypedQuery<MpackEntity> query = m_entityManagerProvider.get().createNamedQuery("MpackEntity.findByNameVersion", MpackEntity.class);
    query.setParameter("mpackName", mpackName);
    query.setParameter("mpackVersion", mpackVersion);
    return m_daoUtils.selectList(query);
  }

  /**
   * Gets all mpacks stored in the database across all clusters.
   *
   * @return all mpacks or an empty list if none exist (never {@code null}).
   */
  @RequiresSession
  public List<MpackEntity> findAll() {
    TypedQuery<MpackEntity> query = m_entityManagerProvider.get().createNamedQuery(
            "MpackEntity.findAll", MpackEntity.class);
    return m_daoUtils.selectList(query);
  }

  @Transactional
  public void removeById(Long id) {
    m_entityManagerProvider.get().remove(findById(id));
  }

}
