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
package org.apache.ambari.server.agent;

import java.util.Map;


/**
 * Represents a status for a Host Component
 */
public class ComponentStatus {
  private String componentName;
  private String msg;
  private String status;

  private String sendExecCmdDet = "False";

  private String serviceName;
  private Long clusterId;
  private String stackVersion;
  private Map<String, Map<String, String>> configurationTags;
  private Map<String, Object> extra;

  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public String getMessage() {
    return msg;
  }

  public void setMessage(String msg) {
    this.msg = msg;
  }

  public String getStatus() {
    return status;
  }

  public void setSendExecCmdDet(String sendExecCmdDet) {
    this.sendExecCmdDet = sendExecCmdDet;
  }

  public String getSendExecCmdDet() {
    return this.sendExecCmdDet;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getStackVersion() {
    return stackVersion;
  }

  public void setStackVersion(String stackVersion) {
    this.stackVersion = stackVersion;
  }

  public String getMsg() {
    return msg;
  }

  public void setMsg(String msg) {
    this.msg = msg;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public Long getClusterId() {
    return clusterId;
  }

  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  /**
   * @param tags the config tags that match this status
   */
  public void setConfigTags(Map<String, Map<String,String>> tags) {
    configurationTags = tags;
  }

  /**
   * @return the config tags that match this command, or <code>null</code>
   * if none are present
   */
  public Map<String, Map<String,String>> getConfigTags() {
    return configurationTags;
  }

  /**
   * Sets extra information coming from the status.
   */
  public void setExtra(Map<String, Object> info) {
    extra = info;
  }

  /**
   * Gets extra information coming from the status.
   */
  public Map<String, Object> getExtra() {
    return extra;
  }

  @Override
  public String toString() {
    return "ComponentStatus [componentName=" + componentName + ", msg=" + msg
        + ", status=" + status
        + ", serviceName=" + serviceName + ", clusterId=" + clusterId
        + ", stackVersion=" + stackVersion + ", configurationTags="
        + configurationTags + ", extra=" + extra + "]";
  }
}
