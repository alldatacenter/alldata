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
 * See the License for the specific language governing privileges and
 * limitations under the License.
 */
package org.apache.ambari.server.controller.internal;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.GroupEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.authorization.RoleAuthorization;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

/**
 * Resource provider for cluster privileges.
 */
public class ClusterPrivilegeResourceProvider extends PrivilegeResourceProvider<ClusterEntity>{

  /**
   * Data access object used to obtain privilege entities.
   */
  protected static ClusterDAO clusterDAO;

  protected static final String CLUSTER_NAME_PROPERTY_ID = "cluster_name";

  protected static final String CLUSTER_NAME = PrivilegeResourceProvider.PRIVILEGE_INFO + PropertyHelper.EXTERNAL_PATH_SEP + CLUSTER_NAME_PROPERTY_ID;

  /**
   * The property ids for a privilege resource.
   */
  private static final Set<String> propertyIds = Sets.newHashSet(
      CLUSTER_NAME,
      PRIVILEGE_ID,
      PERMISSION_NAME,
      PERMISSION_NAME,
      PERMISSION_LABEL,
      PRINCIPAL_NAME,
      PRINCIPAL_TYPE);

  /**
   * The key property ids for a privilege resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.Cluster, CLUSTER_NAME)
      .put(Resource.Type.ClusterPrivilege, PRIVILEGE_ID)
      .build();

  // ----- Constructors ------------------------------------------------------

  /**
   * Construct an ClusterPrivilegeResourceProvider.
   */
  public ClusterPrivilegeResourceProvider() {
    super(propertyIds, keyPropertyIds, Resource.Type.ClusterPrivilege);

    EnumSet<RoleAuthorization> requiredAuthorizations = EnumSet.of(RoleAuthorization.AMBARI_ASSIGN_ROLES);
    setRequiredCreateAuthorizations(requiredAuthorizations);
    setRequiredDeleteAuthorizations(requiredAuthorizations);
    setRequiredGetAuthorizations(requiredAuthorizations);
    setRequiredUpdateAuthorizations(requiredAuthorizations);
  }


  // ----- ClusterPrivilegeResourceProvider ---------------------------------

  /**
   * Static initialization.
   *
   * @param dao  the cluster data access object
   */
  public static void init(ClusterDAO dao) {
    clusterDAO = dao;
  }


  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  public Map<Resource.Type, String> getKeyPropertyIds() {
    return keyPropertyIds;
  }


  // ----- PrivilegeResourceProvider -----------------------------------------

  @Override
  public Map<Long, ClusterEntity> getResourceEntities(Map<String, Object> properties) {

    String clusterName = (String) properties.get(CLUSTER_NAME);

    if (clusterName == null) {
      Map<Long, ClusterEntity> resourceEntities = new HashMap<>();

      List<ClusterEntity> clusterEntities = clusterDAO.findAll();

      for (ClusterEntity clusterEntity : clusterEntities) {
        resourceEntities.put(clusterEntity.getResource().getId(), clusterEntity);
      }
      return resourceEntities;
    }
    ClusterEntity clusterEntity = clusterDAO.findByName(clusterName);
    return Collections.singletonMap(clusterEntity.getResource().getId(), clusterEntity);
  }

  @Override
  public Long getResourceEntityId(Predicate predicate) {
    final String clusterName = getQueryParameterValue(CLUSTER_NAME, predicate).toString();
    final ClusterEntity clusterEntity = clusterDAO.findByName(clusterName);
    return clusterEntity.getResource().getId();
  }


  // ----- helper methods ----------------------------------------------------

  @Override
  protected Resource toResource(PrivilegeEntity privilegeEntity,
                                Map<Long, UserEntity> userEntities,
                                Map<Long, GroupEntity> groupEntities,
                                Map<Long, PermissionEntity> roleEntities,
                                Map<Long, ClusterEntity> resourceEntities,
                                Set<String> requestedIds) {

    Resource resource = super.toResource(privilegeEntity, userEntities, groupEntities, roleEntities, resourceEntities, requestedIds);
    if (resource != null) {
      ClusterEntity clusterEntity = resourceEntities.get(privilegeEntity.getResource().getId());
      setResourceProperty(resource, CLUSTER_NAME, clusterEntity.getClusterName(), requestedIds);
    }
    return resource;
  }

  @Override
  protected PermissionEntity getPermission(String permissionName, ResourceEntity resourceEntity) throws AmbariException {
    return super.getPermission(permissionName, resourceEntity);
  }
}

