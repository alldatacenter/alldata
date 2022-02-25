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

package org.apache.ambari.server.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.authentication.AmbariUserAuthentication;
import org.apache.ambari.server.security.authentication.AmbariUserDetailsImpl;
import org.apache.ambari.server.security.authorization.AmbariGrantedAuthority;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.security.authorization.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class TestAuthenticationFactory {
  public static Authentication createAdministrator() {
    return createAdministrator("admin");
  }

  public static Authentication createAdministrator(String name) {
    return createAmbariUserAuthentication(1, name, Collections.singleton(createAdministratorGrantedAuthority()));
  }

  public static Authentication createClusterAdministrator() {
    return createClusterAdministrator("clusterAdmin", 4L);
  }

  public static Authentication createClusterOperator() {
    return createClusterOperator("clusterOp", 4L);
  }

  public static Authentication createClusterAdministrator(String name, Long clusterResourceId) {
    return createAmbariUserAuthentication(1, name, Collections.singleton(createClusterAdministratorGrantedAuthority(clusterResourceId)));
  }

  public static Authentication createClusterOperator(String name, Long clusterResourceId) {
    return createAmbariUserAuthentication(1, name, Collections.singleton(createClusterOperatorGrantedAuthority(clusterResourceId)));
  }

  public static Authentication createServiceAdministrator() {
    return createServiceAdministrator("serviceAdmin", 4L);
  }

  public static Authentication createServiceAdministrator(String name, Long clusterResourceId) {
    return createAmbariUserAuthentication(1, name, Collections.singleton(createServiceAdministratorGrantedAuthority(clusterResourceId)));
  }

  public static Authentication createServiceOperator() {
    return createServiceOperator("serviceOp", 4L);
  }

  public static Authentication createServiceOperator(String name, Long clusterResourceId) {
    return createAmbariUserAuthentication(1, name, Collections.singleton(createServiceOperatorGrantedAuthority(clusterResourceId)));
  }

  public static Authentication createClusterUser() {
    return createClusterUser("clusterUser", 4L);
  }

  public static Authentication createClusterUser(String name, Long clusterResourceId) {
    return createAmbariUserAuthentication(1, name, Collections.singleton(createClusterUserGrantedAuthority(clusterResourceId)));
  }

  public static Authentication createViewUser(Long viewResourceId) {
    return createViewUser("viewUser", viewResourceId);
  }

  public static Authentication createViewUser(String name, Long viewResourceId) {
    return createAmbariUserAuthentication(1, name, Collections.singleton(createViewUserGrantedAuthority(viewResourceId)));
  }

  public static Authentication createNoRoleUser() {
    return createNoRoleUser("noRoleUser", 4L);
  }

  public static Authentication createNoRoleUser(String name, Long clusterResourceId) {
    return createAmbariUserAuthentication(1, name, Collections.emptySet());
  }

  private static GrantedAuthority createAdministratorGrantedAuthority() {
    return new AmbariGrantedAuthority(createAdministratorPrivilegeEntity());
  }

  private static GrantedAuthority createClusterAdministratorGrantedAuthority(Long clusterResourceId) {
    return new AmbariGrantedAuthority(createClusterAdministratorPrivilegeEntity(clusterResourceId));
  }

  private static GrantedAuthority createClusterOperatorGrantedAuthority(Long clusterResourceId) {
    return new AmbariGrantedAuthority(createClusterOperatorPrivilegeEntity(clusterResourceId));
  }

  private static GrantedAuthority createServiceAdministratorGrantedAuthority(Long clusterResourceId) {
    return new AmbariGrantedAuthority(createServiceAdministratorPrivilegeEntity(clusterResourceId));
  }

  private static GrantedAuthority createServiceOperatorGrantedAuthority(Long clusterResourceId) {
    return new AmbariGrantedAuthority(createServiceOperatorPrivilegeEntity(clusterResourceId));
  }

  private static GrantedAuthority createClusterUserGrantedAuthority(Long clusterResourceId) {
    return new AmbariGrantedAuthority(createClusterUserPrivilegeEntity(clusterResourceId));
  }

  private static GrantedAuthority createViewUserGrantedAuthority(Long resourceId) {
    return new AmbariGrantedAuthority(createViewUserPrivilegeEntity(resourceId));
  }

  public static PrivilegeEntity createPrivilegeEntity(ResourceEntity resourceEntity, PermissionEntity permissionEntity, PrincipalEntity principalEntity) {
    PrivilegeEntity privilegeEntity = new PrivilegeEntity();
    privilegeEntity.setResource(resourceEntity);
    privilegeEntity.setPermission(permissionEntity);
    privilegeEntity.setPrincipal(principalEntity);
    return privilegeEntity;
  }

  private static PrivilegeEntity createAdministratorPrivilegeEntity() {
    return createPrivilegeEntity(createAmbariResourceEntity(), createAdministratorPermission(), null);
  }

  private static PrivilegeEntity createClusterAdministratorPrivilegeEntity(Long clusterResourceId) {
    return createPrivilegeEntity(createClusterResourceEntity(clusterResourceId), createClusterAdministratorPermission(), null);
  }

  private static PrivilegeEntity createClusterOperatorPrivilegeEntity(Long clusterResourceId) {
    return createPrivilegeEntity(createClusterResourceEntity(clusterResourceId), createClusterOperatorPermission(), null);
  }

  private static PrivilegeEntity createServiceAdministratorPrivilegeEntity(Long clusterResourceId) {
    return createPrivilegeEntity(createClusterResourceEntity(clusterResourceId), createServiceAdministratorPermission(), null);
  }

  private static PrivilegeEntity createServiceOperatorPrivilegeEntity(Long clusterResourceId) {
    return createPrivilegeEntity(createClusterResourceEntity(clusterResourceId), createServiceOperatorPermission(), null);
  }

  private static PrivilegeEntity createClusterUserPrivilegeEntity(Long clusterResourceId) {
    return createPrivilegeEntity(createClusterResourceEntity(clusterResourceId), createClusterUserPermission(), null);
  }

  private static PrivilegeEntity createViewUserPrivilegeEntity(Long resourceId) {
    return createPrivilegeEntity(createViewResourceEntity(resourceId), createViewUserPermission(), null);
  }

  public static PermissionEntity createAdministratorPermission() {
    PermissionEntity permissionEntity = new PermissionEntity();
    permissionEntity.setId(PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION);
    permissionEntity.setResourceType(createResourceTypeEntity(ResourceType.AMBARI));
    permissionEntity.setPrincipal(createPrincipalEntity(1L));
    permissionEntity.addAuthorizations(EnumSet.allOf(RoleAuthorization.class));
    return permissionEntity;
  }

  public static PermissionEntity createClusterAdministratorPermission() {
    PermissionEntity permissionEntity = new PermissionEntity();
    permissionEntity.setId(PermissionEntity.CLUSTER_ADMINISTRATOR_PERMISSION);
    permissionEntity.setResourceType(createResourceTypeEntity(ResourceType.CLUSTER));
    permissionEntity.setPrincipal(createPrincipalEntity(2L));
    permissionEntity.addAuthorizations(EnumSet.of(
        RoleAuthorization.SERVICE_VIEW_STATUS_INFO,
        RoleAuthorization.SERVICE_VIEW_OPERATIONAL_LOGS,
        RoleAuthorization.SERVICE_VIEW_METRICS,
        RoleAuthorization.SERVICE_VIEW_CONFIGS,
        RoleAuthorization.SERVICE_VIEW_ALERTS,
        RoleAuthorization.SERVICE_TOGGLE_MAINTENANCE,
        RoleAuthorization.SERVICE_TOGGLE_ALERTS,
        RoleAuthorization.SERVICE_START_STOP,
        RoleAuthorization.SERVICE_SET_SERVICE_USERS_GROUPS,
        RoleAuthorization.SERVICE_RUN_SERVICE_CHECK,
        RoleAuthorization.SERVICE_RUN_CUSTOM_COMMAND,
        RoleAuthorization.SERVICE_MOVE,
        RoleAuthorization.SERVICE_MODIFY_CONFIGS,
        RoleAuthorization.SERVICE_MANAGE_CONFIG_GROUPS,
        RoleAuthorization.SERVICE_MANAGE_AUTO_START,
        RoleAuthorization.SERVICE_MANAGE_ALERTS,
        RoleAuthorization.SERVICE_ENABLE_HA,
        RoleAuthorization.SERVICE_DECOMMISSION_RECOMMISSION,
        RoleAuthorization.SERVICE_COMPARE_CONFIGS,
        RoleAuthorization.SERVICE_ADD_DELETE_SERVICES,
        RoleAuthorization.HOST_VIEW_STATUS_INFO,
        RoleAuthorization.HOST_VIEW_METRICS,
        RoleAuthorization.HOST_VIEW_CONFIGS,
        RoleAuthorization.HOST_TOGGLE_MAINTENANCE,
        RoleAuthorization.HOST_ADD_DELETE_HOSTS,
        RoleAuthorization.HOST_ADD_DELETE_COMPONENTS,
        RoleAuthorization.CLUSTER_VIEW_STATUS_INFO,
        RoleAuthorization.CLUSTER_VIEW_STACK_DETAILS,
        RoleAuthorization.CLUSTER_VIEW_METRICS,
        RoleAuthorization.CLUSTER_VIEW_CONFIGS,
        RoleAuthorization.CLUSTER_VIEW_ALERTS,
        RoleAuthorization.CLUSTER_UPGRADE_DOWNGRADE_STACK,
        RoleAuthorization.CLUSTER_TOGGLE_KERBEROS,
        RoleAuthorization.CLUSTER_TOGGLE_ALERTS,
        RoleAuthorization.CLUSTER_RUN_CUSTOM_COMMAND,
        RoleAuthorization.CLUSTER_MODIFY_CONFIGS,
        RoleAuthorization.CLUSTER_MANAGE_USER_PERSISTED_DATA,
        RoleAuthorization.CLUSTER_MANAGE_CREDENTIALS,
        RoleAuthorization.CLUSTER_MANAGE_CONFIG_GROUPS,
        RoleAuthorization.CLUSTER_MANAGE_AUTO_START,
        RoleAuthorization.CLUSTER_MANAGE_ALERTS,
        RoleAuthorization.CLUSTER_MANAGE_ALERT_NOTIFICATIONS
    ));
    return permissionEntity;
  }

  public static PermissionEntity createClusterOperatorPermission() {
    PermissionEntity permissionEntity = new PermissionEntity();
    permissionEntity.setId(5);
    permissionEntity.setResourceType(createResourceTypeEntity(ResourceType.CLUSTER));
    permissionEntity.setPrincipal(createPrincipalEntity(3L));
    permissionEntity.addAuthorizations(EnumSet.of(
        RoleAuthorization.SERVICE_VIEW_STATUS_INFO,
        RoleAuthorization.SERVICE_VIEW_OPERATIONAL_LOGS,
        RoleAuthorization.SERVICE_VIEW_METRICS,
        RoleAuthorization.SERVICE_VIEW_CONFIGS,
        RoleAuthorization.SERVICE_VIEW_ALERTS,
        RoleAuthorization.SERVICE_TOGGLE_MAINTENANCE,
        RoleAuthorization.SERVICE_START_STOP,
        RoleAuthorization.SERVICE_RUN_SERVICE_CHECK,
        RoleAuthorization.SERVICE_RUN_CUSTOM_COMMAND,
        RoleAuthorization.SERVICE_MOVE,
        RoleAuthorization.SERVICE_MODIFY_CONFIGS,
        RoleAuthorization.SERVICE_MANAGE_CONFIG_GROUPS,
        RoleAuthorization.SERVICE_MANAGE_AUTO_START,
        RoleAuthorization.SERVICE_ENABLE_HA,
        RoleAuthorization.SERVICE_DECOMMISSION_RECOMMISSION,
        RoleAuthorization.SERVICE_COMPARE_CONFIGS,
        RoleAuthorization.HOST_VIEW_STATUS_INFO,
        RoleAuthorization.HOST_VIEW_METRICS,
        RoleAuthorization.HOST_VIEW_CONFIGS,
        RoleAuthorization.HOST_TOGGLE_MAINTENANCE,
        RoleAuthorization.HOST_ADD_DELETE_HOSTS,
        RoleAuthorization.HOST_ADD_DELETE_COMPONENTS,
        RoleAuthorization.CLUSTER_VIEW_STATUS_INFO,
        RoleAuthorization.CLUSTER_VIEW_STACK_DETAILS,
        RoleAuthorization.CLUSTER_VIEW_METRICS,
        RoleAuthorization.CLUSTER_VIEW_CONFIGS,
        RoleAuthorization.CLUSTER_VIEW_ALERTS,
        RoleAuthorization.CLUSTER_MANAGE_USER_PERSISTED_DATA,
        RoleAuthorization.CLUSTER_MANAGE_CREDENTIALS,
        RoleAuthorization.CLUSTER_MANAGE_CONFIG_GROUPS,
        RoleAuthorization.CLUSTER_MANAGE_AUTO_START
    ));
    return permissionEntity;
  }

  public static PermissionEntity createServiceAdministratorPermission() {
    PermissionEntity permissionEntity = new PermissionEntity();
    permissionEntity.setId(5);
    permissionEntity.setResourceType(createResourceTypeEntity(ResourceType.CLUSTER));
    permissionEntity.setPrincipal(createPrincipalEntity(4L));
    permissionEntity.addAuthorizations(EnumSet.of(
        RoleAuthorization.SERVICE_VIEW_STATUS_INFO,
        RoleAuthorization.SERVICE_VIEW_OPERATIONAL_LOGS,
        RoleAuthorization.SERVICE_VIEW_METRICS,
        RoleAuthorization.SERVICE_VIEW_CONFIGS,
        RoleAuthorization.SERVICE_VIEW_ALERTS,
        RoleAuthorization.SERVICE_TOGGLE_MAINTENANCE,
        RoleAuthorization.SERVICE_START_STOP,
        RoleAuthorization.SERVICE_RUN_SERVICE_CHECK,
        RoleAuthorization.SERVICE_RUN_CUSTOM_COMMAND,
        RoleAuthorization.SERVICE_MODIFY_CONFIGS,
        RoleAuthorization.SERVICE_MANAGE_CONFIG_GROUPS,
        RoleAuthorization.SERVICE_MANAGE_AUTO_START,
        RoleAuthorization.SERVICE_DECOMMISSION_RECOMMISSION,
        RoleAuthorization.SERVICE_COMPARE_CONFIGS,
        RoleAuthorization.HOST_VIEW_STATUS_INFO,
        RoleAuthorization.HOST_VIEW_METRICS,
        RoleAuthorization.HOST_VIEW_CONFIGS,
        RoleAuthorization.CLUSTER_VIEW_STATUS_INFO,
        RoleAuthorization.CLUSTER_VIEW_STACK_DETAILS,
        RoleAuthorization.CLUSTER_VIEW_METRICS,
        RoleAuthorization.CLUSTER_VIEW_CONFIGS,
        RoleAuthorization.CLUSTER_VIEW_ALERTS,
        RoleAuthorization.CLUSTER_MANAGE_USER_PERSISTED_DATA,
        RoleAuthorization.CLUSTER_MANAGE_CONFIG_GROUPS
    ));
    return permissionEntity;
  }

  public static PermissionEntity createServiceOperatorPermission() {
    PermissionEntity permissionEntity = new PermissionEntity();
    permissionEntity.setId(6);
    permissionEntity.setResourceType(createResourceTypeEntity(ResourceType.CLUSTER));
    permissionEntity.setPrincipal(createPrincipalEntity(5L));
    permissionEntity.addAuthorizations(EnumSet.of(
        RoleAuthorization.SERVICE_VIEW_STATUS_INFO,
        RoleAuthorization.SERVICE_VIEW_METRICS,
        RoleAuthorization.SERVICE_VIEW_CONFIGS,
        RoleAuthorization.SERVICE_VIEW_ALERTS,
        RoleAuthorization.SERVICE_TOGGLE_MAINTENANCE,
        RoleAuthorization.SERVICE_START_STOP,
        RoleAuthorization.SERVICE_RUN_SERVICE_CHECK,
        RoleAuthorization.SERVICE_RUN_CUSTOM_COMMAND,
        RoleAuthorization.SERVICE_DECOMMISSION_RECOMMISSION,
        RoleAuthorization.SERVICE_COMPARE_CONFIGS,
        RoleAuthorization.HOST_VIEW_STATUS_INFO,
        RoleAuthorization.HOST_VIEW_METRICS,
        RoleAuthorization.HOST_VIEW_CONFIGS,
        RoleAuthorization.CLUSTER_VIEW_STATUS_INFO,
        RoleAuthorization.CLUSTER_VIEW_STACK_DETAILS,
        RoleAuthorization.CLUSTER_VIEW_METRICS,
        RoleAuthorization.CLUSTER_VIEW_CONFIGS,
        RoleAuthorization.CLUSTER_VIEW_ALERTS,
        RoleAuthorization.CLUSTER_MANAGE_USER_PERSISTED_DATA
    ));
    return permissionEntity;
  }

  public static PermissionEntity createClusterUserPermission() {
    PermissionEntity permissionEntity = new PermissionEntity();
    permissionEntity.setId(PermissionEntity.CLUSTER_USER_PERMISSION);
    permissionEntity.setResourceType(createResourceTypeEntity(ResourceType.CLUSTER));
    permissionEntity.setPrincipal(createPrincipalEntity(6L));
    permissionEntity.addAuthorizations(EnumSet.of(
        RoleAuthorization.SERVICE_VIEW_STATUS_INFO,
        RoleAuthorization.SERVICE_VIEW_METRICS,
        RoleAuthorization.SERVICE_VIEW_CONFIGS,
        RoleAuthorization.SERVICE_VIEW_ALERTS,
        RoleAuthorization.SERVICE_COMPARE_CONFIGS,
        RoleAuthorization.HOST_VIEW_STATUS_INFO,
        RoleAuthorization.HOST_VIEW_METRICS,
        RoleAuthorization.HOST_VIEW_CONFIGS,
        RoleAuthorization.CLUSTER_VIEW_STATUS_INFO,
        RoleAuthorization.CLUSTER_VIEW_STACK_DETAILS,
        RoleAuthorization.CLUSTER_VIEW_METRICS,
        RoleAuthorization.CLUSTER_VIEW_CONFIGS,
        RoleAuthorization.CLUSTER_VIEW_ALERTS,
        RoleAuthorization.CLUSTER_MANAGE_USER_PERSISTED_DATA
    ));

    return permissionEntity;
  }

  public static PermissionEntity createViewUserPermission() {
    PermissionEntity permissionEntity = new PermissionEntity();
    permissionEntity.setId(PermissionEntity.VIEW_USER_PERMISSION);
    permissionEntity.setResourceType(createResourceTypeEntity(ResourceType.CLUSTER));
    permissionEntity.setPrincipal(createPrincipalEntity(7L));
    permissionEntity.addAuthorizations(EnumSet.of(RoleAuthorization.VIEW_USE));
    return permissionEntity;
  }

  private static ResourceEntity createAmbariResourceEntity() {
    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setId(null);
    resourceEntity.setResourceType(createResourceTypeEntity(ResourceType.AMBARI));
    return resourceEntity;
  }

  private static ResourceEntity createClusterResourceEntity(Long clusterResourceId) {
    return createResourceEntity(ResourceType.CLUSTER, clusterResourceId);
  }

  private static ResourceEntity createResourceEntity(ResourceType resourceType, Long resourceId) {
    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setId(resourceId);
    resourceEntity.setResourceType(createResourceTypeEntity(resourceType));
    return resourceEntity;
  }

  private static ResourceEntity createViewResourceEntity(Long resourceId) {
    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setId(resourceId);
    if (resourceId != null) {
      resourceEntity.setResourceType(createResourceTypeEntity(ResourceType.VIEW.name(), resourceId.intValue()));
    }
    return resourceEntity;
  }

  private static ResourceTypeEntity createResourceTypeEntity(ResourceType resourceType) {
    return createResourceTypeEntity(resourceType.name(), resourceType.getId());
  }

  private static ResourceTypeEntity createResourceTypeEntity(String resourceName, Integer resourceId) {
    ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity();
    resourceTypeEntity.setId(resourceId);
    resourceTypeEntity.setName(resourceName);
    return resourceTypeEntity;
  }

  private static PrincipalEntity createPrincipalEntity(Long principalId) {
    PrincipalEntity principalEntity = new PrincipalEntity();
    principalEntity.setId(principalId);
    principalEntity.setPrincipalType(createPrincipalTypeEntity());
    return principalEntity;
  }

  private static PrincipalTypeEntity createPrincipalTypeEntity() {
    PrincipalTypeEntity principalTypeEntity = new PrincipalTypeEntity();
    principalTypeEntity.setId(1);
    principalTypeEntity.setName("ROLE");
    return principalTypeEntity;
  }

  private static Authentication createAmbariUserAuthentication(int userId, String username, Set<GrantedAuthority> authorities) {
    PrincipalEntity principal = new PrincipalEntity();
    principal.setPrivileges(Collections.emptySet());

    UserEntity userEntity = new UserEntity();
    userEntity.setUserId(userId);
    userEntity.setUserName(username);
    userEntity.setPrincipal(principal);

    return new AmbariUserAuthentication(null, new AmbariUserDetailsImpl(new User(userEntity), null, authorities), true);
  }
}
