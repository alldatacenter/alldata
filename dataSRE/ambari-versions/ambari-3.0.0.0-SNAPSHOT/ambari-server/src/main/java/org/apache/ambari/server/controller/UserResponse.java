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
package org.apache.ambari.server.controller;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import org.apache.ambari.server.controller.internal.UserResourceProvider;
import org.apache.ambari.server.security.authorization.UserAuthenticationType;

import io.swagger.annotations.ApiModelProperty;

/**
 * Represents a user maintenance request.
 */
public class UserResponse implements ApiModel {

  private final String userName;
  private final String displayName;
  private final String localUserName;
  private final UserAuthenticationType authenticationType;
  private final boolean isLdapUser;
  private final boolean isActive;
  private final boolean isAdmin;
  private Set<String> groups = Collections.emptySet();

  private final Date createTime;
  private final Integer consecutiveFailures;

  public UserResponse(String userName, String displayName, String localUserName, UserAuthenticationType userType, boolean isLdapUser, boolean isActive, boolean isAdmin, Integer consecutiveFailures, Date createTime) {
    this.userName = userName;
    this.displayName = displayName;
    this.localUserName = localUserName;
    this.authenticationType = userType;
    this.isLdapUser = isLdapUser;
    this.isActive = isActive;
    this.isAdmin = isAdmin;
    this.consecutiveFailures = consecutiveFailures;
    this.createTime = createTime;
  }

  @ApiModelProperty(name = UserResourceProvider.USERNAME_PROPERTY_ID)
  public String getUsername() {
    return userName;
  }

  @ApiModelProperty(name = UserResourceProvider.DISPLAY_NAME_PROPERTY_ID)
  public String getDisplayName() {
    return displayName;
  }

  @ApiModelProperty(name = UserResourceProvider.LOCAL_USERNAME_PROPERTY_ID)
  public String getLocalUsername() {
    return localUserName;
  }

  @ApiModelProperty(name = UserResourceProvider.GROUPS_PROPERTY_ID)
  public Set<String> getGroups() {
    return groups;
  }

  public void setGroups(Set<String> groups) {
    this.groups = groups;
  }

  /**
   * @return the isLdapUser
   */
  @ApiModelProperty(name = UserResourceProvider.LDAP_USER_PROPERTY_ID)
  public boolean isLdapUser() {
    return isLdapUser;
  }

  @ApiModelProperty(name = UserResourceProvider.ACTIVE_PROPERTY_ID)
  public boolean isActive() {
    return isActive;
  }

  @ApiModelProperty(name = UserResourceProvider.ADMIN_PROPERTY_ID)
  public boolean isAdmin() {
    return isAdmin;
  }

  @ApiModelProperty(name = UserResourceProvider.USER_TYPE_PROPERTY_ID)
  public UserAuthenticationType getAuthenticationType() {
    return authenticationType;
  }

  @ApiModelProperty(name = UserResourceProvider.CONSECUTIVE_FAILURES_PROPERTY_ID)
  public Integer getConsecutiveFailures() {
    return consecutiveFailures;
  }

  @ApiModelProperty(name = UserResourceProvider.CREATE_TIME_PROPERTY_ID)
  public Date getCreateTime() {
    return createTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UserResponse that = (UserResponse) o;

    if (userName != null ? !userName.equals(that.userName) : that.userName != null) {
      return false;
    }
    return authenticationType == that.authenticationType;

  }

  @Override
  public int hashCode() {
    int result = userName != null ? userName.hashCode() : 0;
    result = 31 * result + (authenticationType != null ? authenticationType.hashCode() : 0);
    return result;
  }

  /**
   * Interface to help correct Swagger documentation generation
   */
  public interface UserResponseSwagger {
    @ApiModelProperty(name = UserResourceProvider.USER_RESOURCE_CATEGORY)
    UserResponse getUserResponse();
  }
}
