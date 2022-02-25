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
import org.apache.ambari.server.controller.internal.ClusterResourceProvider;
import org.apache.ambari.server.controller.internal.PrivilegeResourceProvider;
import org.apache.ambari.server.controller.internal.ViewPrivilegeResourceProvider;
import org.apache.ambari.server.security.authorization.ResourceType;

import io.swagger.annotations.ApiModelProperty;

/**
 * Response schema for endpoint {@link ViewPrivilegeService#getPrivileges(HttpHeaders, UriInfo, String, String, String)}
 */
public class ViewPrivilegeResponse extends PrivilegeResponse implements ApiModel {

  /**
   * Hide cluster name from the response schema
   * @return cluster name
   */
  @Override
  @ApiModelProperty(name = ClusterResourceProvider.CLUSTER_NAME, hidden = true)
  public String getClusterName() {
    return clusterName;
  }

  /**
   * Hide resource type from the response schema
   * @return resource type
   */
  @Override
  @ApiModelProperty(name = PrivilegeResourceProvider.TYPE_PROPERTY_ID, hidden = true)
  public ResourceType getType() {
    return type;
  }

  public interface ViewPrivilegeResponseWrapper extends ApiModel {
    @ApiModelProperty(name = ViewPrivilegeResourceProvider.PRIVILEGE_INFO)
    @SuppressWarnings("unused")
    ViewPrivilegeResponse getViewPrivilegeResponse();
  }

}
