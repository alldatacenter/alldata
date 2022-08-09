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

import org.apache.ambari.server.api.services.users.UserAuthorizationService;

import io.swagger.annotations.ApiModelProperty;

/**
 * Response schema for endpoint {@link UserAuthorizationService#getAuthorizations(HttpHeaders, UriInfo, String)}
 */
public class UserAuthorizationResponse implements ApiModel {

  private final String authorizationId;
  private final String authorizationName;
  private final String resourceType;
  private final String userName;
  private String clusterName;
  private String viewName;
  private String viewVersion;
  private String viewInstanceName;


  /**
   *
   * @param authorizationId      authorization id
   * @param authorizationName    authorization name
   * @param clusterName          cluster name
   * @param resourceType         resource type
   * @param userName             user name
   */
  public UserAuthorizationResponse(String authorizationId, String authorizationName,
                               String clusterName, String resourceType, String userName) {
    this.authorizationId = authorizationId;
    this.authorizationName = authorizationName;
    this.clusterName = clusterName;
    this.resourceType = resourceType;
    this.userName = userName;
  }

  /**
   *
   * @param authorizationId     authorization id
   * @param authorizationName   authorization name
   * @param resourceType        resource type
   * @param userName            user name
   * @param viewName            view name
   * @param viewVersion         view version
   * @param viewInstanceName    view instance name
   */
  public UserAuthorizationResponse(String authorizationId, String authorizationName,
                               String resourceType, String userName, String viewName,
                               String viewVersion, String viewInstanceName) {
    this.authorizationId = authorizationId;
    this.authorizationName = authorizationName;
    this.resourceType = resourceType;
    this.userName = userName;
    this.viewName = viewName;
    this.viewVersion = viewVersion;
    this.viewInstanceName = viewInstanceName;
  }

  /**
   * Returns authorization id
   * @return authorization id
   */
  @ApiModelProperty(name = "AuthorizationInfo/authorization_id")
  public String getAuthorizationId() {
    return authorizationId;
  }

  /**
   * Returns authorization name
   * @return authorization name
   */
  @ApiModelProperty(name = "AuthorizationInfo/authorization_name")
  public String getAuthorizationName() {
    return authorizationName;
  }

  /**
   * Returns resource type
   * @return resource type
   */
  @ApiModelProperty(name = "AuthorizationInfo/resource_type")
  public String getResourceType() {
    return resourceType;
  }

  /**
   * Returns view version
   * @return view version
   */
  @ApiModelProperty(name = "AuthorizationInfo/view_version")
  public String getViewVersion() {
    return viewVersion;
  }

  /**
   * Returns view instance name
   * @return view instance name
   */
  @ApiModelProperty(name = "AuthorizationInfo/view_instance_name")
  public String getViewInstanceName() {
    return viewInstanceName;
  }

  /**
   * Returns user name
   * @return user name
   */
  @ApiModelProperty(name = "AuthorizationInfo/user_name",required = true)
  public String getUserName() {
    return userName;
  }

  /**
   * Returns cluster name
   * @return cluster name
   */
  @ApiModelProperty(name = "AuthorizationInfo/cluster_name")
  public String getClusterName() {
    return clusterName;
  }

  /**
   * Returns view name
   * @return view name
   */
  @ApiModelProperty(name = "AuthorizationInfo/view_name")
  public String getViewName() {
    return viewName;
  }

}
