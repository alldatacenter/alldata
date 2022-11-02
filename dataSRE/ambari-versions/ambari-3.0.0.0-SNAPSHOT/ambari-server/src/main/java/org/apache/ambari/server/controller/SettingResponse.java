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

import java.util.Objects;

import org.apache.ambari.server.controller.internal.SettingResourceProvider;

import io.swagger.annotations.ApiModelProperty;

public class SettingResponse {

  private final String name;
  private final String settingType;
  private final String content;
  private final String updatedBy;
  private final long updateTimestamp;

  public SettingResponse(String name, String settingType, String content, String updatedBy, long updateTimestamp) {
    this.name = name;
    this.settingType = settingType;
    this.content = content;
    this.updatedBy = updatedBy;
    this.updateTimestamp = updateTimestamp;
  }

  @ApiModelProperty(name = SettingResourceProvider.NAME)
  public String getName() {
    return name;
  }

  @ApiModelProperty(name = SettingResourceProvider.SETTING_TYPE)
  public String getSettingType() {
    return settingType;
  }

  @ApiModelProperty(name = SettingResourceProvider.CONTENT)
  public String getContent() {
    return content;
  }

  @ApiModelProperty(name = SettingResourceProvider.UPDATED_BY)
  public String getUpdatedBy() {
    return updatedBy;
  }

  @ApiModelProperty(name = SettingResourceProvider.UPDATE_TIMESTAMP)
  public long getUpdateTimestamp() {
    return updateTimestamp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SettingResponse other = (SettingResponse) o;
    return Objects.equals(name, other.name) &&
      Objects.equals(settingType, other.settingType) &&
      Objects.equals(content, other.content) &&
      Objects.equals(updatedBy, other.updatedBy) &&
      updateTimestamp == other.updateTimestamp;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, settingType, content, updatedBy, updateTimestamp);
  }

  public interface SettingResponseWrapper extends ApiModel {
    @ApiModelProperty(name = SettingResourceProvider.RESPONSE_KEY)
    @SuppressWarnings("unused")
    SettingResponse getSettingResponse();
  }
}
