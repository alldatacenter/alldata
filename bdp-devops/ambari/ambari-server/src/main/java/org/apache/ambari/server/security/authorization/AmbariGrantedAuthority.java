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

package org.apache.ambari.server.security.authorization;

import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.springframework.security.core.GrantedAuthority;

/**
 * Authority granted for Ambari privileges.
 */
public class AmbariGrantedAuthority implements GrantedAuthority {
  /**
   * The Ambari privilege.
   */
  private final PrivilegeEntity privilegeEntity;


  // ----- Constructors ------------------------------------------------------

  public AmbariGrantedAuthority(PrivilegeEntity privilegeEntity) {
    this.privilegeEntity = privilegeEntity;
  }


  // ----- GrantedAuthority --------------------------------------------------

  @Override
  public String getAuthority() {

    ResourceEntity resource = privilegeEntity.getResource();

    Long   resourceId            = resource.getId();
    String resourceTypeQualifier = resource.getResourceType().getName().toUpperCase() + ".";
    String privilegeName         = privilegeEntity.getPermission().getPermissionName() + "@" + resourceId;

    return privilegeName.startsWith(resourceTypeQualifier) ? privilegeName  : resourceTypeQualifier + privilegeName ;
  }


  // ----- AmbariGrantedAuthority --------------------------------------------

  /**
   * Get the associated privilege.
   *
   * @return the privilege
   */
  public PrivilegeEntity getPrivilegeEntity() {
    return privilegeEntity;
  }


  // ----- Object overrides --------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AmbariGrantedAuthority that = (AmbariGrantedAuthority) o;

    return !(privilegeEntity != null ? !privilegeEntity.equals(that.privilegeEntity) : that.privilegeEntity != null);
  }

  @Override
  public int hashCode() {
    return privilegeEntity != null ? privilegeEntity.hashCode() : 0;
  }
}
