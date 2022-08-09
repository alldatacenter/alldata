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

import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.services.users.ActiveWidgetLayoutService;

import io.swagger.annotations.ApiModelProperty;


/**
 * Wrapper class for request body schema of {@link ActiveWidgetLayoutService#updateServices(String, HttpHeaders, UriInfo, String)}
 */
public class ActiveWidgetLayoutRequest implements ApiModel {
   private List<WidgetLayoutIdWrapper> widgetLayouts;

  /**
   * Returns all widget layouts
   * @return widget layouts
   */
  @ApiModelProperty(name = "WidgetLayouts")
  public List<WidgetLayoutIdWrapper> getWidgetLayouts() {
    return widgetLayouts;
  }

  private class WidgetLayoutIdWrapper {
    private Long id;

    /**
     * Returns widget layout id
     * @return {@link #id}
     */
    public Long getId() {
      return id;
    }

    /**
     * Sets widget layout id
     * @param id
     */
    public void setId(Long id) {
      this.id = id;
    }

  }
}
