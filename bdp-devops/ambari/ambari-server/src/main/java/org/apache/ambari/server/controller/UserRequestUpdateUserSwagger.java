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

import io.swagger.annotations.ApiModelProperty;

/**
 * Interface to help correct Swagger documentation generation
 */
public interface UserRequestUpdateUserSwagger extends ApiModel {

  @ApiModelProperty(name = UserResourceProvider.USER_RESOURCE_CATEGORY)
  UpdateUserInfo getUpdateUserRequest();

  interface UpdateUserInfo {
    @ApiModelProperty(name = UserResourceProvider.OLD_PASSWORD_PROPERTY_ID)
    String getOldPassword();

    @ApiModelProperty(name = UserResourceProvider.PASSWORD_PROPERTY_ID)
    String getPassword();

    @ApiModelProperty(name = UserResourceProvider.ACTIVE_PROPERTY_ID)
    Boolean isActive();

    @ApiModelProperty(name = UserResourceProvider.ADMIN_PROPERTY_ID)
    Boolean isAdmin();

    @ApiModelProperty(name = UserResourceProvider.DISPLAY_NAME_PROPERTY_ID)
    String getDisplayName();

    @ApiModelProperty(name = UserResourceProvider.LOCAL_USERNAME_PROPERTY_ID)
    String getLocalUserName();
  }
}
