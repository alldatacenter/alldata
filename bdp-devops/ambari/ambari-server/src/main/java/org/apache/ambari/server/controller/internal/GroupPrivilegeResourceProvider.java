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
package org.apache.ambari.server.controller.internal;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.GroupPrivilegeResponse;
import org.apache.ambari.server.controller.PrivilegeResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.GroupDAO;
import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.GroupEntity;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.security.authorization.Users;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

/**
 * Resource provider for group privilege resources.
 */
@StaticallyInject
public class GroupPrivilegeResourceProvider extends ReadOnlyResourceProvider {

  protected static final String GROUP_NAME_PROPERTY_ID = "group_name";

  protected static final String PRIVILEGE_ID = PrivilegeResourceProvider.PRIVILEGE_ID;
  protected static final String PERMISSION_NAME = PrivilegeResourceProvider.PERMISSION_NAME;
  protected static final String PERMISSION_LABEL = PrivilegeResourceProvider.PERMISSION_LABEL;
  protected static final String PRINCIPAL_NAME = PrivilegeResourceProvider.PRINCIPAL_NAME;
  protected static final String PRINCIPAL_TYPE = PrivilegeResourceProvider.PRINCIPAL_TYPE;
  protected static final String VIEW_NAME = ViewPrivilegeResourceProvider.VIEW_NAME;
  protected static final String VIEW_VERSION = ViewPrivilegeResourceProvider.VERSION;
  protected static final String INSTANCE_NAME = ViewPrivilegeResourceProvider.INSTANCE_NAME;
  protected static final String CLUSTER_NAME = ClusterPrivilegeResourceProvider.CLUSTER_NAME;
  protected static final String TYPE = AmbariPrivilegeResourceProvider.TYPE;
  protected static final String GROUP_NAME = PrivilegeResourceProvider.PRIVILEGE_INFO + PropertyHelper.EXTERNAL_PATH_SEP + GROUP_NAME_PROPERTY_ID;

  /**
   * Data access object used to obtain cluster entities.
   */
  @Inject
  private static ClusterDAO clusterDAO;

  /**
   * Data access object used to obtain group entities.
   */
  @Inject
  private static GroupDAO groupDAO;

  /**
   * Data access object used to obtain view instance entities.
   */
  @Inject
  private static ViewInstanceDAO viewInstanceDAO;

  /**
   * Users (helper) object used to obtain privilege entities.
   */
  @Inject
  private static Users users;

  /**
   * The property ids for a privilege resource.
   */
  private static final Set<String> propertyIds = Sets.newHashSet(
      PRIVILEGE_ID,
      PERMISSION_NAME,
      PERMISSION_LABEL,
      PRINCIPAL_NAME,
      PRINCIPAL_TYPE,
      VIEW_NAME,
      VIEW_VERSION,
      INSTANCE_NAME,
      CLUSTER_NAME,
      TYPE,
      GROUP_NAME);

  /**
   * Static initialization.
   *  @param clusterDAO      the cluster data access object
   * @param groupDAO        the group data access object
   * @param viewInstanceDAO the view instance data access object
   * @param users           the users helper instance
   */
  public static void init(ClusterDAO clusterDAO, GroupDAO groupDAO,
                          ViewInstanceDAO viewInstanceDAO, Users users) {
    GroupPrivilegeResourceProvider.clusterDAO = clusterDAO;
    GroupPrivilegeResourceProvider.groupDAO = groupDAO;
    GroupPrivilegeResourceProvider.viewInstanceDAO = viewInstanceDAO;
    GroupPrivilegeResourceProvider.users = users;
  }

  @SuppressWarnings("serial")
  private static final Set<String> pkPropertyIds = ImmutableSet.<String>builder()
    .add(PRIVILEGE_ID)
    .build();

  /**
   * The key property ids for a privilege resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.Group, GROUP_NAME)
      .put(Resource.Type.GroupPrivilege, PRIVILEGE_ID)
      .build();


  /**
   * Constructor.
   */
  public GroupPrivilegeResourceProvider() {
    super(Resource.Type.GroupPrivilege, propertyIds, keyPropertyIds, null);

    EnumSet<RoleAuthorization> requiredAuthorizations = EnumSet.of(RoleAuthorization.AMBARI_ASSIGN_ROLES);
    setRequiredCreateAuthorizations(requiredAuthorizations);
    setRequiredDeleteAuthorizations(requiredAuthorizations);
    setRequiredGetAuthorizations(requiredAuthorizations);
    setRequiredUpdateAuthorizations(requiredAuthorizations);
  }

  // ----- PrivilegeResourceProvider -----------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {
    final Set<Resource> resources = new HashSet<>();
    final Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    // Ensure that the authenticated user has authorization to get this information
    if (!AuthorizationHelper.isAuthorized(ResourceType.AMBARI, null, RoleAuthorization.AMBARI_MANAGE_GROUPS)) {
      throw new AuthorizationException();
    }

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      final String groupName = (String) propertyMap.get(GROUP_NAME);

      if (groupName != null) {
        GroupEntity groupEntity = groupDAO.findGroupByName(groupName);

        if (groupEntity == null) {
          throw new SystemException("Group " + groupName + " was not found");
        }

        final Collection<PrivilegeEntity> privileges = users.getGroupPrivileges(groupEntity);

        for (PrivilegeEntity privilegeEntity : privileges) {
          GroupPrivilegeResponse response = getResponse(privilegeEntity, groupName);
          resources.add(toResource(response, requestedIds));
        }
      }
    }

    return resources;
  }

  /**
   * Returns the response for the group privilege that should be returned for the group privilege REST endpoint
   * @param privilegeEntity the privilege data
   * @param groupName    the group name
   * @return group privilege response
   */
  protected GroupPrivilegeResponse getResponse(PrivilegeEntity privilegeEntity, String groupName) {
    String permissionLabel = privilegeEntity.getPermission().getPermissionLabel();
    String permissionName =  privilegeEntity.getPermission().getPermissionName();
    String principalTypeName = privilegeEntity.getPrincipal().getPrincipalType().getName();
    GroupPrivilegeResponse groupPrivilegeResponse = new GroupPrivilegeResponse(groupName, permissionLabel , permissionName,
      privilegeEntity.getId(), PrincipalTypeEntity.PrincipalType.valueOf(principalTypeName));

    if (principalTypeName.equals(PrincipalTypeEntity.GROUP_PRINCIPAL_TYPE_NAME)) {
      final GroupEntity groupEntity = groupDAO.findGroupByPrincipal(privilegeEntity.getPrincipal());
      groupPrivilegeResponse.setPrincipalName(groupEntity.getGroupName());
    }


    String typeName = privilegeEntity.getResource().getResourceType().getName();
    ResourceType resourceType = ResourceType.translate(typeName);

    if(resourceType != null) {
      switch (resourceType) {
        case AMBARI:
          // there is nothing special to add for this case
          break;
        case CLUSTER:
          final ClusterEntity clusterEntity = clusterDAO.findByResourceId(privilegeEntity.getResource().getId());
          groupPrivilegeResponse.setClusterName(clusterEntity.getClusterName());
          break;
        case VIEW:
          final ViewInstanceEntity viewInstanceEntity = viewInstanceDAO.findByResourceId(privilegeEntity.getResource().getId());
          final ViewEntity viewEntity = viewInstanceEntity.getViewEntity();

          groupPrivilegeResponse.setViewName(viewEntity.getCommonName());
          groupPrivilegeResponse.setVersion(viewEntity.getVersion());
          groupPrivilegeResponse.setInstanceName(viewInstanceEntity.getName());
          break;
      }

      groupPrivilegeResponse.setType(resourceType);
    }

    return groupPrivilegeResponse;
  }

  /**
   * Translate the Response into a Resource
   * @param response        {@link PrivilegeResponse}
   * @param requestedIds    the relevant request ids
   * @return a resource
   */
  protected Resource toResource(GroupPrivilegeResponse response, Set<String> requestedIds) {
    final ResourceImpl resource = new ResourceImpl(Resource.Type.GroupPrivilege);

    setResourceProperty(resource, GROUP_NAME, response.getGroupName(), requestedIds);
    setResourceProperty(resource, PRIVILEGE_ID, response.getPrivilegeId(), requestedIds);
    setResourceProperty(resource, PERMISSION_NAME, response.getPermissionName(), requestedIds);
    setResourceProperty(resource, PERMISSION_LABEL, response.getPermissionLabel(), requestedIds);
    setResourceProperty(resource, PRINCIPAL_TYPE, response.getPrincipalType().name(), requestedIds);
    if (response.getPrincipalName() != null) {
      setResourceProperty(resource, PRINCIPAL_NAME, response.getPrincipalName(), requestedIds);
    }

    if (response.getType() != null) {
      setResourceProperty(resource, TYPE, response.getType().name(), requestedIds);
      switch (response.getType()) {
        case CLUSTER:
          setResourceProperty(resource, CLUSTER_NAME, response.getClusterName(), requestedIds);
          break;
        case VIEW:
          setResourceProperty(resource, VIEW_NAME, response.getViewName(), requestedIds);
          setResourceProperty(resource, VIEW_VERSION, response.getVersion(), requestedIds);
          setResourceProperty(resource, INSTANCE_NAME, response.getInstanceName(), requestedIds);
          break;
      }

    }

    return resource;
  }
}
