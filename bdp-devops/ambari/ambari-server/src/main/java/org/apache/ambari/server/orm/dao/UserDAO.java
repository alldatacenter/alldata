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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.UserEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class UserDAO {

  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  @RequiresSession
  public UserEntity findByPK(Integer userPK) {
    return entityManagerProvider.get().find(UserEntity.class, userPK);
  }

  @RequiresSession
  public List<UserEntity> findAll() {
    TypedQuery<UserEntity> query = entityManagerProvider.get().createQuery("SELECT user_entity FROM UserEntity user_entity", UserEntity.class);
    return daoUtils.selectList(query);
  }

  @RequiresSession
  public UserEntity findUserByName(String userName) {
    TypedQuery<UserEntity> query = entityManagerProvider.get().createNamedQuery("userByName", UserEntity.class);
    query.setParameter("username", userName.toLowerCase());
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Find the user entities for the given list of admin principal entities.
   *
   * @param principalList the list of principal entities
   * @return the matching list of user entities
   */
  @RequiresSession
  public List<UserEntity> findUsersByPrincipal(List<PrincipalEntity> principalList) {
    if (principalList == null || principalList.isEmpty()) {
      return Collections.emptyList();
    }
    TypedQuery<UserEntity> query = entityManagerProvider.get().createQuery("SELECT user_entity FROM UserEntity user_entity WHERE user_entity.principal IN :principalList", UserEntity.class);
    query.setParameter("principalList", principalList);
    return daoUtils.selectList(query);
  }

  /**
   * Find the user entity for the given admin principal entity.
   *
   * @param principal the principal entity
   * @return the matching user entity
   */
  @RequiresSession
  public UserEntity findUserByPrincipal(PrincipalEntity principal) {
    if (principal == null) {
      return null;
    }
    final TypedQuery<UserEntity> query = entityManagerProvider.get().createQuery("SELECT user_entity FROM UserEntity user_entity WHERE user_entity.principal.id=:principalId", UserEntity.class);
    query.setParameter("principalId", principal.getId());
    return daoUtils.selectSingle(query);
  }


  @Transactional
  public void create(UserEntity user) {
    create(new HashSet<>(Collections.singleton(user)));
  }

  @Transactional
  public void create(Set<UserEntity> users) {
    for (UserEntity user : users) {
      entityManagerProvider.get().persist(user);
    }
  }

  @Transactional
  public UserEntity merge(UserEntity user) {
    return entityManagerProvider.get().merge(user);
  }

  @Transactional
  public void merge(Set<UserEntity> users) {
    for (UserEntity user : users) {
      entityManagerProvider.get().merge(user);
    }
  }

  @Transactional
  public void remove(UserEntity user) {
    entityManagerProvider.get().remove(merge(user));
    entityManagerProvider.get().getEntityManagerFactory().getCache().evictAll();
  }

  @Transactional
  public void remove(Set<UserEntity> users) {
    for (UserEntity userEntity : users) {
      entityManagerProvider.get().remove(entityManagerProvider.get().merge(userEntity));
    }
  }

  @Transactional
  public void removeByPK(Integer userPK) {
    remove(findByPK(userPK));
  }
}
