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

import java.util.Date;

import org.apache.ambari.server.controller.internal.UserAuthenticationSourceResourceProvider;
import org.apache.ambari.server.security.authorization.UserAuthenticationType;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import io.swagger.annotations.ApiModelProperty;

/**
 * Represents a user authentication source.
 */
public class UserAuthenticationSourceResponse implements ApiModel {

  private final String userName;
  private final Long sourceId;
  private final UserAuthenticationType authenticationType;
  private final String key;

  private final Date createTime;
  private final Date updateTime;

  public UserAuthenticationSourceResponse(String userName, Long sourceId, UserAuthenticationType authenticationType, String key, Date createTime, Date updateTime) {
    this.userName = userName;
    this.sourceId = sourceId;
    this.authenticationType = authenticationType;
    this.key = key;
    this.createTime = createTime;
    this.updateTime = updateTime;
  }

  /**
   * Returns user name
   *
   * @return user name
   */
  @ApiModelProperty(name = UserAuthenticationSourceResourceProvider.USER_NAME_PROPERTY_ID, required = true)
  public String getUserName() {
    return userName;
  }


  @ApiModelProperty(name = UserAuthenticationSourceResourceProvider.AUTHENTICATION_SOURCE_ID_PROPERTY_ID, required = true)
  public Long getSourceId() {
    return sourceId;
  }

  @ApiModelProperty(name = UserAuthenticationSourceResourceProvider.AUTHENTICATION_TYPE_PROPERTY_ID, required = true)
  public UserAuthenticationType getAuthenticationType() {
    return authenticationType;
  }

  @ApiModelProperty(name = UserAuthenticationSourceResourceProvider.KEY_PROPERTY_ID)
  public String getKey() {
    return key;
  }

  @ApiModelProperty(name = UserAuthenticationSourceResourceProvider.CREATED_PROPERTY_ID)
  public Date getCreateTime() {
    return createTime;
  }

  @ApiModelProperty(name = UserAuthenticationSourceResourceProvider.UPDATED_PROPERTY_ID)
  public Date getUpdateTime() {
    return updateTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (o == null || getClass() != o.getClass()) {
      return false;
    } else {
      UserAuthenticationSourceResponse that = (UserAuthenticationSourceResponse) o;

      EqualsBuilder equalsBuilder = new EqualsBuilder();
      equalsBuilder.append(userName, that.userName);
      equalsBuilder.append(sourceId, that.sourceId);
      equalsBuilder.append(authenticationType, that.authenticationType);
      equalsBuilder.append(key, that.key);
      equalsBuilder.append(createTime, that.createTime);
      equalsBuilder.append(updateTime, that.updateTime);
      return equalsBuilder.isEquals();
    }
  }

  @Override
  public int hashCode() {
    HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
    hashCodeBuilder.append(userName);
    hashCodeBuilder.append(sourceId);
    hashCodeBuilder.append(authenticationType);
    hashCodeBuilder.append(key);
    hashCodeBuilder.append(createTime);
    hashCodeBuilder.append(updateTime);
    return hashCodeBuilder.toHashCode();
  }

  /**
   * Interface to help correct Swagger documentation generation
   */
  public interface UserAuthenticationSourceResponseSwagger {
    @ApiModelProperty(name = UserAuthenticationSourceResourceProvider.AUTHENTICATION_SOURCE_RESOURCE_CATEGORY)
    @SuppressWarnings("unused")
    UserAuthenticationSourceResponse getUserAuthenticationSourceResponse();
  }
}
