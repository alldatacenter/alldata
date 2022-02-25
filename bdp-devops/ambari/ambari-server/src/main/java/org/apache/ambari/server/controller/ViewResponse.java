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

import org.apache.ambari.server.api.services.views.ViewService;
import org.apache.ambari.server.controller.internal.ViewResourceProvider;

import io.swagger.annotations.ApiModelProperty;
/**
 * Response schema for endpoint {@link ViewService#getViews(String, HttpHeaders, UriInfo)}
 */
public class ViewResponse implements ApiModel {
  private ViewInfo viewInfo;

  /**
   *
   * @param viewInfo {@link ViewInfo}
   */
  public ViewResponse(ViewInfo viewInfo) {
    this.viewInfo = viewInfo;
  }

  /**
   * Returns view information wrapper class instance
   * @return {@link #viewInfo}
   */
  @ApiModelProperty(name = ViewResourceProvider.VIEW_INFO)
  public ViewInfo getViewInfo() {
    return viewInfo;
  }

  /**
   * static wrapper class for view information
   */
  private class ViewInfo implements ApiModel{
    private String viewName;

    /**
     *
     * @param viewName view name
     */
    public ViewInfo(String viewName) {
      this.viewName = viewName;
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
     * @param viewName view name
     */
    public void setViewName(String viewName) {
      this.viewName = viewName;
    }
  }
}
