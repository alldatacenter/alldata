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

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.services.views.ViewPermissionService;
import org.apache.ambari.server.controller.internal.ViewPermissionResourceProvider;

import io.swagger.annotations.ApiModelProperty;



/**
 * Response schema for endpoint {@link ViewPermissionService#getPermissions(HttpHeaders, UriInfo, String, String)}
 */
public class ViewPermissionResponse implements ApiModel {

  private final ViewPermissionInfo viewPermissionInfo;

  /**
   *
   * @param viewPermissionInfo view permission info
   */
  public ViewPermissionResponse(ViewPermissionInfo viewPermissionInfo) {
    this.viewPermissionInfo = viewPermissionInfo;
  }

  /**
   * Returns {@link ViewPermissionInfo} instance that hold all view permission information
   * @return {@link ViewPermissionInfo}
   */
  @ApiModelProperty(name = ViewPermissionResourceProvider.PERMISSION_INFO)
  public ViewPermissionInfo getViewPermissionInfo() {
    return viewPermissionInfo;
  }

  /**
   * static wrapper class that holds all view permission information
   */
  public static class ViewPermissionInfo {
    private final String viewName;
    private final String version;
    private final Integer permissionId;
    private final String permissionName;
    private final String resourceName;

    /**
     *
     * @param viewName            view name
     * @param version             view version
     * @param permissionId        permission id
     * @param permissionName      permission name
     * @param resourceName        resource name
     */
    public ViewPermissionInfo(String viewName, String version, Integer permissionId, String permissionName, String resourceName) {
      this.viewName = viewName;
      this.version = version;
      this.permissionId = permissionId;
      this.permissionName = permissionName;
      this.resourceName = resourceName;
    }

    /**
     * Returns view name
     * @return view name
     */
    @ApiModelProperty(name = ViewPermissionResourceProvider.VIEW_NAME_PROPERTY_ID)
    public String getViewName() {
      return viewName;
    }

    /**
     * Returns view version
     * @return view version
     */
    @ApiModelProperty(name = ViewPermissionResourceProvider.VERSION_PROPERTY_ID)
    public String getVersion() {
      return version;
    }

    /**
     * Returns permission id
     * @return permission id
     */
    @ApiModelProperty(name = ViewPermissionResourceProvider.PERMISSION_ID_PROPERTY_ID)
    public Integer getPermissionId() {
      return permissionId;
    }

    /**
     * Returns permission name
     * @return permission name
     */
    @ApiModelProperty(name = ViewPermissionResourceProvider.PERMISSION_NAME_PROPERTY_ID)
    public String getPermissionName() {
      return permissionName;
    }

    /**
     * Returns resource name
     * @return resource names
     */
    @ApiModelProperty(name = ViewPermissionResourceProvider.RESOURCE_NAME_PROPERTY_ID)
    public String getResourceName() {
      return resourceName;
    }
  }
}
