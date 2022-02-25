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

import org.apache.ambari.server.controller.internal.RequestResourceProvider;

import io.swagger.annotations.ApiModelProperty;

/**
 * Request schema for endpoint {@link org.apache.ambari.server.api.services.RequestService#createRequests(String,
 *    javax.ws.rs.core.HttpHeaders, javax.ws.rs.core.UriInfo)}
 *
 * The interface is not actually implemented, it only carries swagger annotations.
 */
public interface RequestPostRequest extends ApiModel {

  String NOTES_ACTION_OR_COMMAND = "Either action or command must be specified, but not both";

  @ApiModelProperty(name = RequestResourceProvider.REQUEST_INFO)
  RequestInfo getRequestInfo();

  @ApiModelProperty(name = "Body")
  Body getBody();

  interface RequestInfo {
    @ApiModelProperty(name = RequestResourceProvider.ACTION_ID, notes = NOTES_ACTION_OR_COMMAND)
    String getAction();

    @ApiModelProperty(name = RequestResourceProvider.COMMAND_ID, notes = NOTES_ACTION_OR_COMMAND)
    String getCommand();

    @ApiModelProperty(name = "operation_level",
        notes = "Must be specified along with command.")
    OperationLevel getOperationLevel();

    @ApiModelProperty(name = "parameters")
    Map<String, Object> getParameters();
  }

  interface RequestResourceFilter {
    @ApiModelProperty(name = "service_name")
    String getServiceName();

    @ApiModelProperty(name = "component_name")
    String getComponentName();

    @ApiModelProperty(name = "hosts")
    String getHosts();

    @ApiModelProperty(name = "hosts_predicate")
    String getHostsPredicate();
  }

  interface OperationLevel {
    @ApiModelProperty(name = "level")
    String getLevel();

    @ApiModelProperty(name = "cluster_name")
    String getClusterName();
  }

  interface Body {
    @ApiModelProperty(name = RequestResourceProvider.REQUESTS)
    Request getRequest();
  }

  interface Request {
    @ApiModelProperty(name = "resource_filters")
    List<RequestResourceFilter> getResourceFilters();

    @ApiModelProperty(name = "cluster_name")
    String getClusterName();

    @ApiModelProperty(name = "exclusive")
    boolean isExclusive();
  }

}
