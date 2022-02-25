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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.GroupEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.security.authorization.GroupType;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

@Singleton
public class GroupDAO {
  @Inject
  Provider<EntityManager> entityManagerProvider;
  @Inject
  DaoUtils daoUtils;

  @RequiresSession
  public GroupEntity findByPK(Integer groupPK) {
    return entityManagerProvider.get().find(GroupEntity.class, groupPK);
  }

  @RequiresSession
  public List<GroupEntity> findAll() {
    final TypedQuery<GroupEntity> query = entityManagerProvider.get().createQuery("SELECT group_entity FROM GroupEntity group_entity", GroupEntity.class);
    return daoUtils.selectList(query);
  }

  @RequiresSession
  public GroupEntity findGroupByName(String groupName) {
    final TypedQuery<GroupEntity> query = entityManagerProvider.get().createNamedQuery("groupByName", GroupEntity.class);
    query.setParameter("groupname", groupName.toLowerCase());
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  @RequiresSession
  public GroupEntity findGroupByNameAndType(String groupName, GroupType groupType) {
    // do case insensitive compare
    TypedQuery<GroupEntity> query = entityManagerProvider.get().createQuery(
        "SELECT group_entity FROM GroupEntity group_entity WHERE group_entity.groupType=:type AND lower(group_entity.groupName)=lower(:name)", GroupEntity.class);
    query.setParameter("type", groupType);
    query.setParameter("name", groupName);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Find the group entities for the given list of principals
   *
   * @param principalList  the list of principal entities
   *
   * @return the list of groups matching the query
   */
  @RequiresSession
  public List<GroupEntity> findGroupsByPrincipal(List<PrincipalEntity> principalList) {
    if (principalList == null || principalList.isEmpty()) {
      return Collections.emptyList();
    }
    TypedQuery<GroupEntity> query = entityManagerProvider.get().createQuery("SELECT grp FROM GroupEntity grp WHERE grp.principal IN :principalList", GroupEntity.class);
    query.setParameter("principalList", principalList);
    return daoUtils.selectList(query);
  }

  /**
   * Find the group entity for the given admin principal entity.
   *
   * @param principal the principal entity
   *
   * @return the matching gropu entity
   */
  @RequiresSession
  public GroupEntity findGroupByPrincipal(PrincipalEntity principal) {
    if (principal == null) {
      return null;
    }
    final TypedQuery<GroupEntity> query = entityManagerProvider.get().createQuery("SELECT group_entity FROM GroupEntity group_entity WHERE group_entity.principal.id=:principalId", GroupEntity.class);
    query.setParameter("principalId", principal.getId());
    return daoUtils.selectSingle(query);
  }


  @Transactional
  public void create(GroupEntity group) {
    create(new HashSet<>(Arrays.asList(group)));
  }

  @Transactional
  public void create(Set<GroupEntity> groups) {
    for (GroupEntity group: groups) {
      group.setGroupName(group.getGroupName().toLowerCase());
      entityManagerProvider.get().persist(group);
    }
  }

  @Transactional
  public GroupEntity merge(GroupEntity group) {
    group.setGroupName(group.getGroupName().toLowerCase());
    return entityManagerProvider.get().merge(group);
  }

  @Transactional
  public void merge(Set<GroupEntity> groups) {
    for (GroupEntity group: groups) {
      group.setGroupName(group.getGroupName().toLowerCase());
      entityManagerProvider.get().merge(group);
    }
  }

  @Transactional
  public void remove(GroupEntity group) {
    entityManagerProvider.get().remove(merge(group));
    entityManagerProvider.get().getEntityManagerFactory().getCache().evictAll();
  }

  @Transactional
  public void remove(Set<GroupEntity> groups) {
    for (GroupEntity groupEntity: groups) {
      entityManagerProvider.get().remove(entityManagerProvider.get().merge(groupEntity));
    }
  }

  @Transactional
  public void removeByPK(Integer groupPK) {
    remove(findByPK(groupPK));
  }
}
