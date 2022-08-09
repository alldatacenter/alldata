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

import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.KeyValueEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class KeyValueDAO {

  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  @RequiresSession
  public KeyValueEntity findByKey(String key) {
    return entityManagerProvider.get().find(KeyValueEntity.class, key);
  }

  @RequiresSession
  public Collection<KeyValueEntity> findAll() {
    TypedQuery<KeyValueEntity> query =
        entityManagerProvider.get().createQuery("SELECT keyValue FROM KeyValueEntity keyValue", KeyValueEntity.class);
    return daoUtils.selectList(query);
  }

  @Transactional
  public void refresh(KeyValueEntity keyValueEntity) {
    entityManagerProvider.get().refresh(keyValueEntity);
  }

  @Transactional
  public void create(KeyValueEntity keyValueEntity) {
    entityManagerProvider.get().persist(keyValueEntity);
  }

  @Transactional
  public KeyValueEntity merge(KeyValueEntity keyValueEntity) {
    return entityManagerProvider.get().merge(keyValueEntity);
  }

  @Transactional
  public void remove(KeyValueEntity keyValueEntity) {
    entityManagerProvider.get().remove(merge(keyValueEntity));
  }

  @Transactional
  public void removeByHostName(String key) {
    remove(findByKey(key));
  }

}
