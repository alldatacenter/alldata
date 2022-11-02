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

import org.apache.ambari.server.api.services.views.ViewPrivilegeService;
import org.apache.ambari.server.controller.internal.PrivilegeResourceProvider;
import org.apache.ambari.server.controller.internal.ViewPrivilegeResourceProvider;

import io.swagger.annotations.ApiModelProperty;

/**
 * Request body schema for endpoint {@link ViewPrivilegeService#createPrivilege(String, HttpHeaders, UriInfo, String, String, String)} )}
 */
public class ViewPrivilegeRequest extends ViewPrivilegeResponse implements ApiModel {

  /**
   * Hide privilege id in request body schema
   * @return privilege id
   */
  @Override
  @ApiModelProperty(name = PrivilegeResourceProvider.PRIVILEGE_ID_PROPERTY_ID, hidden = true)
  public Integer getPrivilegeId() {
    return privilegeId;
  }

  /**
   * Hide permission label in request body schema
   * @return permission label
   */
  @Override
  @ApiModelProperty(name = PrivilegeResourceProvider.PERMISSION_LABEL_PROPERTY_ID, hidden = true)
  public String getPermissionLabel() {
    return permissionLabel;
  }

  /**
   * Hide view name in request body schema
   * @return view name
   */
  @Override
  @ApiModelProperty(name = ViewPrivilegeResourceProvider.VIEW_NAME_PROPERTY_ID, hidden = true)
  public String getViewName() {
    return viewName;
  }

  /**
   * Hide view version in request body schema
   * @return view version
   */
  @Override
  @ApiModelProperty(name = ViewPrivilegeResourceProvider.VERSION_PROPERTY_ID, hidden = true)
  public String getVersion() {
    return version;
  }

  /**
   * Hide view instance name in request body schema
   * @return view instance name
   */
  @Override
  @ApiModelProperty(name = ViewPrivilegeResourceProvider.INSTANCE_NAME_PROPERTY_ID, hidden = true)
  public String getInstanceName() {
    return instanceName;
  }


}
