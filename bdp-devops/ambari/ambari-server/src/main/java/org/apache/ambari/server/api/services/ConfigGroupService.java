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
package org.apache.ambari.server.api.services;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.ConfigGroupResponse;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.http.HttpStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Service responsible for management of Config Groups
 */
@Api(value = "Config Groups", description = "Endpoint for config-group-specific operations")
public class ConfigGroupService extends BaseService {

  private static final String CONFIG_GROUP_REQUEST_TYPE = "org.apache.ambari.server.controller.ConfigGroupRequest";

  /**
   * Parent cluster name.
   */
  private String m_clusterName;

  /**
   * Constructor
   * @param m_clusterName
   */
  public ConfigGroupService(String m_clusterName) {
    this.m_clusterName = m_clusterName;
  }

  /**
   * Handles URL: /clusters/{clusterId}/config_groups
   * Get all the config groups for a cluster.
   *
   * @param headers
   * @param ui
   * @return
   */
  @GET
  @Path("") // This is needed if class level path is not present otherwise no Swagger docs will be generated for this method
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Returns all config groups", response = ConfigGroupResponse.ConfigGroupWrapper.class, responseContainer =
          RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "ConfigGroup/*", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, allowableValues = QUERY_FROM_VALUES, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, allowableValues = QUERY_TO_VALUES, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
  })
  public Response getConfigGroups(String body, @Context HttpHeaders headers,
                                  @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET,
      createConfigGroupResource(m_clusterName, null));
  }

  /**
   * Handles URL: /clusters/{clusterId}/config_groups/{groupId}
   * Get details on a config group.
   *
   * @return
   */
  @GET
  @Path("{groupId}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Returns a single config group", response = ConfigGroupResponse.ConfigGroupWrapper.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "ConfigGroup/*", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
  })
  public Response getConfigGroup(String body, @Context HttpHeaders headers,
          @Context UriInfo ui, @PathParam("groupId") String groupId) {
    return handleRequest(headers, body, ui, Request.Type.GET,
        createConfigGroupResource(m_clusterName, groupId));
  }

  /**
   * Handles POST /clusters/{clusterId}/config_groups
   * Create a new config group
   *
   * @param body
   * @param headers
   * @param ui
   * @return
   */
  @POST
  @Path("") // This is needed if class level path is not present otherwise no Swagger docs will be generated for this method
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Creates a config group")
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = CONFIG_GROUP_REQUEST_TYPE, paramType = PARAM_TYPE_BODY),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_CREATED, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response createConfigGroup(String body, @Context HttpHeaders headers,
                                    @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.POST,
      createConfigGroupResource(m_clusterName, null));
  }

  /**
   * Handles PUT /clusters/{clusterId}/config_groups/{groupId}
   * Update a config group
   *
   * @param body
   * @param headers
   * @param ui
   * @param groupId
   * @return
   */
  @PUT
  @Path("{groupId}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Updates a config group")
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = CONFIG_GROUP_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_ACCEPTED, message = MSG_REQUEST_ACCEPTED),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response updateConfigGroup(String body, @Context HttpHeaders
    headers, @Context UriInfo ui, @PathParam("groupId") String groupId) {
    return handleRequest(headers, body, ui, Request.Type.PUT,
      createConfigGroupResource(m_clusterName, groupId));
  }

  /**
   * Handles DELETE /clusters/{clusterId}/config_groups/{groupId}
   * Delete a config group
   *
   * @param headers
   * @param ui
   * @param groupId
   * @return
   */
  @DELETE
  @Path("{groupId}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Deletes a config group")
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deleteConfigGroup(@Context HttpHeaders headers,
                                    @Context UriInfo ui,
                                    @PathParam("groupId") String groupId) {
    return handleRequest(headers, null, ui, Request.Type.DELETE,
      createConfigGroupResource(m_clusterName, groupId));
  }

  /**
   * Create a request resource instance.
   *
   * @param clusterName  cluster name
   * @param groupId    config group id
   *
   * @return a request resource instance
   */
  ResourceInstance createConfigGroupResource(String clusterName,
                                             String groupId) {
    Map<Resource.Type,String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, clusterName);
    mapIds.put(Resource.Type.ConfigGroup, groupId);

    return createResource(Resource.Type.ConfigGroup, mapIds);
  }
}
