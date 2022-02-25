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

import org.apache.ambari.server.controller.internal.UserResourceProvider;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Represents a user maintenance request.
 */
@ApiModel
public class UserRequest {
  private String userName;
  private String password;
  private String oldPassword;
  private Boolean active;
  private Boolean admin;

  private String displayName;
  private String localUserName;
  private Integer consecutiveFailures;

  public UserRequest(String name) {
    this.userName = name;
  }

  @ApiModelProperty(name = UserResourceProvider.USERNAME_PROPERTY_ID)
  public String getUsername() {
    return userName;
  }

  @ApiModelProperty(name = UserResourceProvider.PASSWORD_PROPERTY_ID)
  public String getPassword() {
    return password;
  }

  public void setPassword(String userPass) {
    password = userPass;
  }

  @ApiModelProperty(name = UserResourceProvider.OLD_PASSWORD_PROPERTY_ID)
  public String getOldPassword() {
    return oldPassword;
  }

  public void setOldPassword(String oldUserPass) {
    oldPassword = oldUserPass;
  }

  @ApiModelProperty(name = UserResourceProvider.ACTIVE_PROPERTY_ID)
  public Boolean isActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  @ApiModelProperty(name = UserResourceProvider.ADMIN_PROPERTY_ID)
  public Boolean isAdmin() {
    return admin;
  }

  public void setAdmin(Boolean admin) {
    this.admin = admin;
  }

  @ApiModelProperty(name = UserResourceProvider.DISPLAY_NAME_PROPERTY_ID)
  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  @ApiModelProperty(name = UserResourceProvider.LOCAL_USERNAME_PROPERTY_ID)
  public String getLocalUserName() {
    return localUserName;
  }

  public void setLocalUserName(String localUserName) {
    this.localUserName = localUserName;
  }

  @ApiModelProperty(name = UserResourceProvider.CONSECUTIVE_FAILURES_PROPERTY_ID)
  public Integer getConsecutiveFailures() {
    return consecutiveFailures;
  }

  public void setConsecutiveFailures(Integer consecutiveFailures) {
    this.consecutiveFailures = consecutiveFailures;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("User, username=").append(userName);
    return sb.toString();
  }
}
