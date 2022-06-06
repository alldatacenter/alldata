/*
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

import org.apache.ambari.server.controller.internal.ClusterResourceProvider;
import org.apache.ambari.server.controller.internal.PrivilegeResourceProvider;
import org.apache.ambari.server.controller.internal.ViewPrivilegeResourceProvider;
import org.apache.ambari.server.controller.internal.ViewResourceProvider;
import org.apache.ambari.server.orm.entities.PrincipalTypeEntity.PrincipalType;
import org.apache.ambari.server.security.authorization.ResourceType;

import io.swagger.annotations.ApiModelProperty;


public abstract class PrivilegeResponse implements ApiModel {
  protected String permissionLabel;
  protected Integer privilegeId;
  protected String permissionName;
  protected PrincipalType principalType;
  protected String principalName;
  protected ResourceType type;
  protected String clusterName;
  protected String viewName;
  protected String version;
  protected String instanceName;


  /**
   * Returns permission label
   * @return permission label
   */
  @ApiModelProperty(name = PrivilegeResourceProvider.PERMISSION_LABEL_PROPERTY_ID)
  public String getPermissionLabel() {
    return permissionLabel;
  }

  /**
   * Returns principal name
   * @return principal name
   */
  @ApiModelProperty(name = PrivilegeResourceProvider.PRINCIPAL_NAME_PROPERTY_ID)
  public String getPrincipalName() {
    return principalName;
  }

  /**
   * Sets principal name
   * @param principalName  principal name
   */
  public void setPrincipalName(String principalName) {
    this.principalName = principalName;
  }

  /**
   * Returns privilege id
   * @return  privilege id
   */
  @ApiModelProperty(name = PrivilegeResourceProvider.PRIVILEGE_ID_PROPERTY_ID)
  public Integer getPrivilegeId() {
    return privilegeId;
  }

  /**
   * Returns permission name
   * @return permission name
   */
  @ApiModelProperty(name = PrivilegeResourceProvider.PERMISSION_NAME_PROPERTY_ID)
  public String getPermissionName() {
    return permissionName;
  }

  /**
   * Returns principal type
   * @return principal type
   */
  @ApiModelProperty(name = PrivilegeResourceProvider.PRINCIPAL_TYPE_PROPERTY_ID)
  public PrincipalType getPrincipalType() {
    return principalType;
  }

  /**
   * Returns resource type
   * @return resource type
   */
  @ApiModelProperty(name = PrivilegeResourceProvider.TYPE_PROPERTY_ID)
  public ResourceType getType() {
    return type;
  }

  /**
   * Sets resource type
   * @param type resource type
   */
  public void setType(ResourceType type) {
    this.type = type;
  }

  /**
   * Returns cluster name
   * @return cluster name
   */
  @ApiModelProperty(name = ClusterResourceProvider.CLUSTER_NAME)
  public String getClusterName() {
    return clusterName;
  }

  /**
   * Sets cluster name
   * @param clusterName cluster name
   */
  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  /**
   * Returns view name
   * @return view name
   */
  @ApiModelProperty(name = ViewResourceProvider.VIEW_NAME_PROPERTY_ID)
  public String getViewName() {
    return viewName;
  }

  /**
   * Sets view name
   * @param viewName  view name
   */
  public void setViewName(String viewName) {
    this.viewName = viewName;
  }

  /**
   * Returns view version
   * @return view version
   */
  @ApiModelProperty(name = PrivilegeResourceProvider.VERSION_PROPERTY_ID)
  public String getVersion() {
    return version;
  }

  /**
   * Sets view version
   * @param version view version
   */
  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * Returns view instance name
   * @return view instance name
   */
  @ApiModelProperty(name = ViewPrivilegeResourceProvider.INSTANCE_NAME_PROPERTY_ID)
  public String getInstanceName() {
    return instanceName;
  }

  public void setInstanceName(String instanceName) {
    this.instanceName = instanceName;
  }

}
