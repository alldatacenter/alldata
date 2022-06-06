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

import org.apache.ambari.server.controller.internal.UserAuthenticationSourceResourceProvider;
import org.apache.ambari.server.security.authorization.UserAuthenticationType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Represents a user authentication source request.
 */
@ApiModel
public class UserAuthenticationSourceRequest {
  private final String username;
  private final Long sourceId;
  private final UserAuthenticationType authenticationType;
  private final String key;
  private final String oldKey;

  public UserAuthenticationSourceRequest(String username, Long sourceId) {
    this(username, sourceId, null, null);

  }

  public UserAuthenticationSourceRequest(String username, Long sourceId, UserAuthenticationType authenticationType) {
    this(username, sourceId, authenticationType, null);
  }

  public UserAuthenticationSourceRequest(String username, Long sourceId, UserAuthenticationType authenticationType, String key) {
    this(username, sourceId, authenticationType, key, null);
  }

  public UserAuthenticationSourceRequest(String username, Long sourceId, UserAuthenticationType authenticationType, String key, String oldKey) {
    this.username = username;
    this.sourceId = sourceId;
    this.authenticationType = authenticationType;
    this.key = key;
    this.oldKey = oldKey;
  }

  @ApiModelProperty(name = UserAuthenticationSourceResourceProvider.USER_NAME_PROPERTY_ID, hidden = true)
  public String getUsername() {
    return username;
  }

  @ApiModelProperty(name = UserAuthenticationSourceResourceProvider.AUTHENTICATION_SOURCE_ID_PROPERTY_ID)
  public Long getSourceId() {
    return sourceId;
  }

  @ApiModelProperty(name = UserAuthenticationSourceResourceProvider.AUTHENTICATION_TYPE_PROPERTY_ID)
  public UserAuthenticationType getAuthenticationType() {
    return authenticationType;
  }

  @ApiModelProperty(name = UserAuthenticationSourceResourceProvider.KEY_PROPERTY_ID)
  public String getKey() {
    return key;
  }

  @ApiModelProperty(name = UserAuthenticationSourceResourceProvider.OLD_KEY_PROPERTY_ID)
  public String getOldKey() {
    return oldKey;
  }
}
