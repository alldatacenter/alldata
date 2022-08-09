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

import java.util.HashMap;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.services.users.ActiveWidgetLayoutService;

import io.swagger.annotations.ApiModelProperty;

/**
 * Response schema for endpoint {@link ActiveWidgetLayoutService#getServices(String, HttpHeaders, UriInfo, String)}
 */
public class ActiveWidgetLayoutResponse implements ApiModel {

  private final Long id;
  private final String clusterName;
  private final String displayName;
  private final String layoutName;
  private final String scope;
  private final String sectionName;
  private final String userName;
  private List<HashMap<String, WidgetResponse>> widgets;

  /**
   *
   * @param id
   * @param clusterName   cluster name
   * @param displayName   display name
   * @param layoutName    layout name
   * @param sectionName   section name
   * @param scope         scope
   * @param userName      user name
   * @param widgets       widgets
   */
  public ActiveWidgetLayoutResponse(Long id, String clusterName, String displayName, String layoutName, String sectionName, String scope, String userName, List<HashMap<String, WidgetResponse>> widgets) {
    this.id = id;
    this.clusterName = clusterName;
    this.displayName = displayName;
    this.layoutName = layoutName;
    this.sectionName = sectionName;
    this.scope = scope;
    this.userName = userName;
    this.widgets = widgets;
  }

  /**
   * Returns id for the widget layout
   * @return widget layout id
   */
  @ApiModelProperty(name = "WidgetLayoutInfo/id", hidden=true)
  public Long getId() {
    return id;
  }

  /**
   * Returns cluster name for the widget layout
   * @return cluster name
   */
  @ApiModelProperty(name = "WidgetLayoutInfo/cluster_name")
  public String getClusterName() {
    return clusterName;
  }

  /**
   * Returns display name for the widget layout
   * @return  display name
   */
  @ApiModelProperty(name = "WidgetLayoutInfo/display_name")
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Returns layout name
   * @return  layout name
   */
  @ApiModelProperty(name = "WidgetLayoutInfo/layout_name")
  public String getLayoutName() {
    return layoutName;
  }

  /**
   * Returns scope
   * @return scope
   */
  @ApiModelProperty(name = "WidgetLayoutInfo/scope")
  public String getScope() {
    return scope;
  }

  /**
   * Returns section name
   * @return section name
   */
  @ApiModelProperty(name = "WidgetLayoutInfo/section_name")
  public String getSectionName() {
    return sectionName;
  }

  /**
   * Returns user name
   * @return user name
   */
  @ApiModelProperty(name = "WidgetLayoutInfo/user_name")
  public String getUserName() {
    return userName;
  }

  /**
   * Returns widgets of the layout
   * @return  widgets
   */
  @ApiModelProperty(name = "WidgetLayoutInfo/widgets")
  public List<HashMap<String, WidgetResponse>> getWidgets() {
    return widgets;
  }

  public void setWidgets(List<HashMap<String, WidgetResponse>> widgets) {
    this.widgets = widgets;
  }
}
