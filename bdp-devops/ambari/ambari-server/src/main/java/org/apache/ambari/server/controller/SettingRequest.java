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

public class SettingRequest {

  private final String name;
  private final String settingType;
  private final String content;

  public SettingRequest(String name, String settingType, String content) {
    this.name = name;
    this.settingType = settingType;
    this.content = content;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SettingRequest other = (SettingRequest) o;
    return Objects.equals(name, other.name) &&
      Objects.equals(settingType, other.settingType) &&
      Objects.equals(content, other.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, settingType, content);
  }

}
