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

import java.util.List;

import org.apache.ambari.server.state.DependencyConditionInfo;

import io.swagger.annotations.ApiModelProperty;

/**
 * Response schema for endpoint {@link org.apache.ambari.server.api.services.StacksService#getServiceComponentDependency(
 *        String, javax.ws.rs.core.HttpHeaders, javax.ws.rs.core.UriInfo, String, String, String, String, String)}

 * The interface is not actually implemented, it only carries swagger annotations.
 */
public interface ComponentDependencyResponse extends ApiModel{

  @ApiModelProperty(name = "Dependencies")
  public ComponentDependencyResponseInfo getDependencyResponseInfo();

  public interface ComponentDependencyResponseInfo {
    @ApiModelProperty(name = "component_name")
    public String getComponentName();

    @ApiModelProperty(name = "conditions")
    public List<DependencyConditionInfo> getDependencyConditions();

    @ApiModelProperty(name = "dependent_component_name")
    public String getDependentComponentName();

    @ApiModelProperty(name = "dependent_service_name")
    public String getDependentServiceName();

    @ApiModelProperty(name = "scope")
    public String getScope();

    @ApiModelProperty(name = "service_name")
    public String getServiceName();

    @ApiModelProperty(name = "stack_name")
    public String getStackName();

    @ApiModelProperty(name = "stack_version")
    public String getStackVersion();
  }

}
