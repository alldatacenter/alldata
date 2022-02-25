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
import java.util.Map;

import org.apache.ambari.server.state.SecurityType;

import io.swagger.annotations.ApiModelProperty;

/**
 * Request / response schema for blueprint API Swagger documentation generation. The interface only serves documentation
 * generation purposes, it is not meant to be implemented.
 */
public interface BlueprintSwagger extends ApiModel {

  @ApiModelProperty(name = "Blueprints")
  BlueprintInfo getBlueprintInfo();

  @ApiModelProperty(name = "configurations")
  List<Map<String, Object>> getConfigurations();

  @ApiModelProperty(name = "host_groups")
  List<HostGroupInfo> getHostGroups();

  interface BlueprintInfo {
    @ApiModelProperty(name = "blueprint_name")
    String getBlueprintName();

    @ApiModelProperty(name = "stack_name")
    String getStackName();

    @ApiModelProperty(name = "stack_version")
    String getStackVersion();

    @ApiModelProperty(name = "security")
    SecurityInfo getSecurity();
  }

  interface SecurityInfo {
    @ApiModelProperty(name = "security_type")
    SecurityType getSecurityType();

    @ApiModelProperty(name = "kerberos_descriptor")
    Map<String, Object> getKerberosDescriptor();

    @ApiModelProperty(name = "kerberos_descriptor_reference")
    String getKerberosDescriptorReference();
  }

  interface HostGroupInfo {
    @ApiModelProperty(name = "name")
    String getHostGroupName();

    @ApiModelProperty(name = "cardinality")
    int getCardinality();

    @ApiModelProperty(name = "components")
    List<ComponentInfo> getComponents();

    @ApiModelProperty(name = "configurations")
    List<Map<String, Object>> getConfigurations();

  }

  interface ComponentInfo {
    @ApiModelProperty(name = "name")
    String getComponentName();

    @ApiModelProperty(name = "provision_action")
    String getProvisionAction();
  }

}
