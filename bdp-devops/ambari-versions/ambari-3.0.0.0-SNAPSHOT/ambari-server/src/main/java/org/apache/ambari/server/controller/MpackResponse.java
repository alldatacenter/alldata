/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller;


import org.apache.ambari.server.controller.internal.MpackResourceProvider;
import org.apache.ambari.server.state.Mpack;


import io.swagger.annotations.ApiModelProperty;

/**
 * Represents a mpack response.
 */
public class MpackResponse {

  private Long id;
  private String mpackId;
  private String mpackName;
  private String mpackVersion;
  private String mpackUri;
  private Long registryId;
  private String stackId;
  private String description;
  private String displayName;

  public MpackResponse(Mpack mpack) {
    this.id = mpack.getResourceId();
    this.mpackId = mpack.getMpackId();
    this.mpackName = mpack.getName();
    this.mpackVersion = mpack.getVersion();
    this.mpackUri = mpack.getMpackUri();
    this.registryId = mpack.getRegistryId();
    this.description = mpack.getDescription();
    this.displayName = mpack.getDisplayName();
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getRegistryId() {
    return registryId;
  }

  public void setRegistryId(Long registryId) {
    this.registryId = registryId;
  }

  public String getMpackId() {
    return mpackId;
  }

  public void setMpackId(String mpackId) {
    this.mpackId = mpackId;
  }

  public String getMpackName() {
    return mpackName;
  }

  public void setMpackName(String mpackName) {
    this.mpackName = mpackName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getMpackVersion() {
    return mpackVersion;
  }

  public void setMpackVersion(String mpackVersion) {
    this.mpackVersion = mpackVersion;
  }

  public String getMpackUri() {
    return mpackUri;
  }

  public void setMpackUri(String mpackUri) {
    this.mpackUri = mpackUri;
  }

  public String getStackId() {
    return stackId;
  }

  public void setStackId(String stackId) {
    this.stackId = stackId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  @Override
  public int hashCode() {
    int result;
    result = 31 + getId().hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof MpackResponse)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    MpackResponse MpackResponse = (MpackResponse) obj;
    return getId().equals(MpackResponse.getId());
  }

  public interface MpackResponseWrapper extends ApiModel {
    @ApiModelProperty(name = MpackResourceProvider.RESPONSE_KEY)
    @SuppressWarnings("unused")
    MpackResponse getMpackResponse();
  }
}
