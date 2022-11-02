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
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.UserAuthenticationEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.authorization.UserAuthenticationType;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class UserAuthenticationDAO {

  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  @RequiresSession
  public UserAuthenticationEntity findByPK(Long pk) {
    return entityManagerProvider.get().find(UserAuthenticationEntity.class, pk);
  }

  @RequiresSession
  public List<UserAuthenticationEntity> findAll() {
    TypedQuery<UserAuthenticationEntity> query = entityManagerProvider.get().createNamedQuery("UserAuthenticationEntity.findAll", UserAuthenticationEntity.class);
    return daoUtils.selectList(query);
  }

  @RequiresSession
  public List<UserAuthenticationEntity> findByType(UserAuthenticationType authenticationType) {
    TypedQuery<UserAuthenticationEntity> query = entityManagerProvider.get().createNamedQuery("UserAuthenticationEntity.findByType", UserAuthenticationEntity.class);
    query.setParameter("authenticationType", authenticationType.name());
    return daoUtils.selectList(query);
  }

  @RequiresSession
  public List<UserAuthenticationEntity> findByTypeAndKey(UserAuthenticationType authenticationType, String key) {
    TypedQuery<UserAuthenticationEntity> query = entityManagerProvider.get().createNamedQuery("UserAuthenticationEntity.findByTypeAndKey", UserAuthenticationEntity.class);
    query.setParameter("authenticationType", authenticationType.name());
    query.setParameter("authenticationKey", key);
    return daoUtils.selectList(query);
  }

  public List<UserAuthenticationEntity> findByUser(UserEntity userEntity) {
    TypedQuery<UserAuthenticationEntity> query = entityManagerProvider.get().createNamedQuery("UserAuthenticationEntity.findByUser", UserAuthenticationEntity.class);
    query.setParameter("userId", userEntity.getUserId());
    return daoUtils.selectList(query);
  }

  @Transactional
  public void create(UserAuthenticationEntity entity) {
    entityManagerProvider.get().persist(entity);
  }

  @Transactional
  public void create(Set<UserAuthenticationEntity> entities) {
    for (UserAuthenticationEntity entity : entities) {
      entityManagerProvider.get().persist(entity);
    }
  }

  @Transactional
  public UserAuthenticationEntity merge(UserAuthenticationEntity entity) {
    return entityManagerProvider.get().merge(entity);
  }

  @Transactional
  public void merge(Set<UserAuthenticationEntity> entities) {
    for (UserAuthenticationEntity entity : entities) {
      entityManagerProvider.get().merge(entity);
    }
  }

  @Transactional
  public void remove(UserAuthenticationEntity entity) {
    entityManagerProvider.get().remove(entity);
  }

  @Transactional
  public void remove(Set<UserAuthenticationEntity> entities) {
    for (UserAuthenticationEntity entity : entities) {
      entityManagerProvider.get().remove(entity);
    }
  }

  @Transactional
  public void removeByPK(Long pk) {
    remove(findByPK(pk));
  }
}
