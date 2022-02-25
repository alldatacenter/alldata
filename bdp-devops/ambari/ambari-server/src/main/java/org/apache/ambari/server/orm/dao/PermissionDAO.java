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
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.orm.RequiresSession;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * Permission Data Access Object.
 */
@Singleton
public class PermissionDAO {

  /**
   * JPA entity manager
   */
  @Inject
  Provider<EntityManager> entityManagerProvider;

  @Inject
  DaoUtils daoUtils;

  /**
   * Create permission.
   *
   * @param permissionEntity  entity to store
   */
  @Transactional
  public void create(PermissionEntity permissionEntity) {
    entityManagerProvider.get().persist(permissionEntity);
  }

  /**
   * Create or updates a permission.
   *
   * @param permissionEntity  entity to create or update
   */
  @Transactional
  public PermissionEntity merge(PermissionEntity permissionEntity) {
    return entityManagerProvider.get().merge(permissionEntity);
  }

  /**
   * Find a permission entity with the given id.
   *
   * @param id  type id
   *
   * @return  a matching permission entity or null
   */
  @RequiresSession
  public PermissionEntity findById(Integer id) {
    return entityManagerProvider.get().find(PermissionEntity.class, id);
  }

  /**
   * Find a permission entity with the given name.
   *
   * @param name  permission name
   *
   * @return  a matching permission entity or null
   */
  @RequiresSession
  public PermissionEntity findByName(String name) {
    TypedQuery<PermissionEntity> query = entityManagerProvider.get().createNamedQuery("PermissionEntity.findByName", PermissionEntity.class);
    query.setParameter("permissionName", name);
    return daoUtils.selectSingle(query);
  }

  /**
   * Find the permission entities for the given list of principals
   *
   * @param principalList  the list of principal entities
   *
   * @return the list of permissions (or roles) matching the query
   */
  @RequiresSession
  public List<PermissionEntity> findPermissionsByPrincipal(List<PrincipalEntity> principalList) {
    if (principalList == null || principalList.isEmpty()) {
      return Collections.emptyList();
    }
    TypedQuery<PermissionEntity> query = entityManagerProvider.get().createNamedQuery("PermissionEntity.findByPrincipals", PermissionEntity.class);
    query.setParameter("principalList", principalList);
    return daoUtils.selectList(query);
  }

  /**
   * Find all permission entities.
   *
   * @return all entities or an empty List
   */
  @RequiresSession
  public List<PermissionEntity> findAll() {
    TypedQuery<PermissionEntity> query = entityManagerProvider.get().createQuery("SELECT p FROM PermissionEntity p", PermissionEntity.class);
    return daoUtils.selectList(query);
  }

  /**
   * Find a permission entity by name and type.
   *
   * @param name         the permission name
   * @param resourceType the resource type
   *
   * @return a matching permission entity or null
   */
  @RequiresSession
  public PermissionEntity findPermissionByNameAndType(String name, ResourceTypeEntity resourceType) {
    if (name.equals(PermissionEntity.VIEW_USER_PERMISSION_NAME)) {
      // VIEW.USER permission should be available for any type of views
      return findViewUsePermission();
    }
    TypedQuery<PermissionEntity> query = entityManagerProvider.get().createQuery("SELECT p FROM PermissionEntity p WHERE p.permissionName=:permissionname AND p.resourceType=:resourcetype", PermissionEntity.class);
    query.setParameter("permissionname", name);
    query.setParameter("resourcetype", resourceType);
    return daoUtils.selectSingle(query);
  }

  /**
   * Find AMBARI.ADMINISTRATOR permission.
   *
   * @return a matching permission entity or null
   */
  @RequiresSession
  public PermissionEntity findAmbariAdminPermission() {
    return findById(PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION);
  }

  /**
   * Find VIEW.USER permission.
   *
   * @return a matching permission entity or null
   */
  @RequiresSession
  public PermissionEntity findViewUsePermission() {
    return findById(PermissionEntity.VIEW_USER_PERMISSION);
  }

  /**
   * Find CLUSTER.ADMINISTRATOR permission.
   *
   * @return a matching permission entity or null
   */
  @RequiresSession
  public PermissionEntity findClusterOperatePermission() {
    return findById(PermissionEntity.CLUSTER_ADMINISTRATOR_PERMISSION);
  }

  /**
   * Find CLUSTER.USER permission.
   *
   * @return a matching permission entity or null
   */
  @RequiresSession
  public PermissionEntity findClusterReadPermission() {
    return findById(PermissionEntity.CLUSTER_USER_PERMISSION);
  }
}
