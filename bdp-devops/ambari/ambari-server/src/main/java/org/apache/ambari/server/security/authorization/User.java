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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.ambari.server.orm.entities.MemberEntity;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.UserAuthenticationEntity;
import org.apache.ambari.server.orm.entities.UserEntity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


/**
 * Describes user of web-services
 */
@ApiModel
public class User {
  final private int userId;
  final private String userName;
  final private Date createTime;
  final private boolean active;
  final private Collection<String> groups;
  final private Collection<AuthenticationMethod> authenticationMethods;
  final private boolean admin;

  public User(UserEntity userEntity) {
    userId = userEntity.getUserId();
    userName = userEntity.getUserName();
    createTime = new Date(userEntity.getCreateTime());
    active = userEntity.getActive();

    groups = new ArrayList<>();
    for (MemberEntity memberEntity : userEntity.getMemberEntities()) {
      groups.add(memberEntity.getGroup().getGroupName());
    }

    authenticationMethods = new ArrayList<>();
    List<UserAuthenticationEntity> authenticationEntities = userEntity.getAuthenticationEntities();
    for (UserAuthenticationEntity authenticationEntity : authenticationEntities) {
      authenticationMethods.add(new AuthenticationMethod(authenticationEntity.getAuthenticationType(), authenticationEntity.getAuthenticationKey()));
    }

    boolean admin = false;
    for (PrivilegeEntity privilegeEntity : userEntity.getPrincipal().getPrivileges()) {
      if (privilegeEntity.getPermission().getPermissionName().equals(PermissionEntity.AMBARI_ADMINISTRATOR_PERMISSION_NAME)) {
        admin = true;
        break;
      }
    }
    this.admin = admin;
  }

  @ApiModelProperty(hidden = true)
  public int getUserId() {
    return userId;
  }

  @ApiModelProperty(name = "Users/user_name",required = true, access = "public", notes = "username containing only lowercase letters")
  public String getUserName() {
    return userName;
  }

  @ApiModelProperty(hidden = true)
  public Date getCreateTime() {
    return createTime;
  }

  @ApiModelProperty(name = "Users/active")
  public boolean isActive() {
    return active;
  }

  @ApiModelProperty(name = "Users/admin")
  public boolean isAdmin() {
    return admin;
  }

  @ApiModelProperty(name = "Users/groups")
  public Collection<String> getGroups() {
    return groups;
  }

  @ApiModelProperty(name = "Users/authentication_methods")
  public Collection<AuthenticationMethod> getAuthenticationMethods() {
    return authenticationMethods;
  }

  @ApiModelProperty(name = "Users/ldap_user")
  public boolean isLdapUser() {
    for (AuthenticationMethod authenticationMethod : authenticationMethods) {
      if (authenticationMethod.getAuthenticationType() == UserAuthenticationType.LDAP) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return userName;
  }

}
