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

package org.apache.ambari.server.security.authorization.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;

import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.security.authorization.AmbariGrantedAuthority;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class InternalAuthenticationToken implements Authentication {
  private static final long serialVersionUID = 1L;
  
  private static final String INTERNAL_NAME = "internal";
  private static final PrivilegeEntity ADMIN_PRIV_ENTITY = new PrivilegeEntity();
  static{
    createAdminPrivilegeEntity(ADMIN_PRIV_ENTITY);
  }

  // used in ClustersImpl, checkPermissions
  private static final Collection<? extends GrantedAuthority> AUTHORITIES =
    Collections.singleton(new AmbariGrantedAuthority(ADMIN_PRIV_ENTITY));
  private static final User INTERNAL_USER = new User(INTERNAL_NAME, "empty", AUTHORITIES);

  private String token;
  private boolean authenticated = false;


  /**
   * Sets up a privilege entity to be one that an administrative user would have.
   */
  private static void createAdminPrivilegeEntity(PrivilegeEntity entity) {
    PermissionEntity pe = new PermissionEntity();
    pe.setId(PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION);
    pe.setPermissionName(PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION_NAME);
    pe.addAuthorizations(EnumSet.allOf(RoleAuthorization.class));
    entity.setPermission(pe);
    
    ResourceEntity resource = new ResourceEntity();
    resource.setId(1L);
    
    ResourceTypeEntity rte = new ResourceTypeEntity();
    rte.setId(ResourceType.AMBARI.getId());
    rte.setName(ResourceType.AMBARI.name());
    resource.setResourceType(rte);
    entity.setResource(resource);
  }

  public InternalAuthenticationToken(String tokenString) {
    this.token = tokenString;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return AUTHORITIES;
  }

  @Override
  public String getCredentials() {
    return token;
  }

  @Override
  public Object getDetails() {
    return null;
  }

  @Override
  public Object getPrincipal() {
    return INTERNAL_USER;
  }

  @Override
  public boolean isAuthenticated() {
    return authenticated;
  }

  @Override
  public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
    this.authenticated = isAuthenticated;
  }
  @Override
  public String getName() {
    return INTERNAL_NAME;
  }
}
