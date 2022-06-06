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

import org.apache.ambari.server.controller.internal.ServiceConfigVersionResourceProvider;

import io.swagger.annotations.ApiModelProperty;

public class ServiceConfigVersionRequest {
  private String clusterName;
  private String serviceName;
  private Long version;
  private Long createTime;
  private Long applyTime;
  private String userName;
  private String note;
  private Boolean isCurrent;

  public ServiceConfigVersionRequest() {
  }

  public ServiceConfigVersionRequest(String clusterName, String serviceName, Long version, Long createTime, Long applyTime, String userName, Boolean isCurrent) {
    this.clusterName = clusterName;
    this.serviceName = serviceName;
    this.version = version;
    this.createTime = createTime;
    this.applyTime = applyTime;
    this.userName = userName;
    this.isCurrent = isCurrent;
  }

  @ApiModelProperty(name = ServiceConfigVersionResourceProvider.SERVICE_NAME_PROPERTY_ID)
  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  @ApiModelProperty(name = ServiceConfigVersionResourceProvider.SERVICE_CONFIG_VERSION_PROPERTY_ID)
  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  @ApiModelProperty(name = ServiceConfigVersionResourceProvider.CREATE_TIME_PROPERTY_ID)
  public Long getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Long createTime) {
    this.createTime = createTime;
  }

  @ApiModelProperty(name = ServiceConfigVersionResourceProvider.APPLIED_TIME_PROPERTY_ID)
  public Long getApplyTime() {
    return applyTime;
  }

  public void setApplyTime(Long applyTime) {
    this.applyTime = applyTime;
  }

  @ApiModelProperty(name = ServiceConfigVersionResourceProvider.USER_PROPERTY_ID)
  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  @ApiModelProperty(name = ServiceConfigVersionResourceProvider.CLUSTER_NAME_PROPERTY_ID)
  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  @ApiModelProperty(name = ServiceConfigVersionResourceProvider.SERVICE_CONFIG_VERSION_NOTE_PROPERTY_ID)
  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  @ApiModelProperty(name = ServiceConfigVersionResourceProvider.IS_CURRENT_PROPERTY_ID)
  public Boolean getIsCurrent() {
    return isCurrent;
  }

  public void setIsCurrent(Boolean isCurrent) {
    this.isCurrent = isCurrent;
  }

  @Override
  public String toString() {
    return "ServiceConfigVersionRequest{" +
        "clusterName='" + clusterName + '\'' +
        ", serviceName='" + serviceName + '\'' +
        ", version=" + version +
        ", createTime=" + createTime +
        ", applyTime=" + applyTime +
        ", userName='" + userName + '\'' +
        ", note='" + note + '\'' +
        ", isCurrent=" + isCurrent +
        '}';
  }
}
