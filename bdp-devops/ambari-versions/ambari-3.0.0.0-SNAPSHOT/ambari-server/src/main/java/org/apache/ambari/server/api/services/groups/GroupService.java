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
package org.apache.ambari.server.api.services.groups;

import java.util.Collections;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.BaseService;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.controller.GroupResponse;
import org.apache.ambari.server.controller.spi.Resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Service responsible for user groups requests.
 */
@Path("/groups/")
@Api(value = "Groups", description = "Endpoint for group specific operations")
public class GroupService extends BaseService {
  /**
   * Gets all groups.
   * Handles: GET /groups requests.
   *
   * @param headers    http headers
   * @param ui         uri info
   */
  @GET
  @Produces("text/plain")
  @ApiOperation(value = "Get all groups", nickname = "GroupService#getGroups", notes = "Returns details of all groups.", response = GroupResponse.class, responseContainer = "List")
  @ApiImplicitParams({
    @ApiImplicitParam(name = "fields", value = "Filter group details", defaultValue = "Groups/*", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "sortBy", value = "Sort groups (asc | desc)", defaultValue = "Groups/group_name.asc", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "page_size", value = "The number of resources to be returned for the paged response.", defaultValue = "10", dataType = "integer", paramType = "query"),
    @ApiImplicitParam(name = "from", value = "The starting page resource (inclusive). Valid values are :offset | \"start\"", defaultValue = "0", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "to", value = "The ending page resource (inclusive). Valid values are :offset | \"end\"", dataType = "string", paramType = "query")
  })
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful retrieval of all group entries", response = GroupResponse.class, responseContainer = "List")}
  )
  public Response getGroups(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET, createGroupResource(null));
  }

  /**
   * Gets a single group.
   * Handles: GET /groups/{groupName} requests.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param groupName    the group name
   * @return information regarding the specified group
   */
  @GET
  @Path("{groupName}")
  @Produces("text/plain")
  @ApiOperation(value = "Get group", nickname = "GroupService#getGroup", notes = "Returns group details.", response = GroupResponse.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = "fields", value = "Filter group details", defaultValue = "Groups", dataType = "string", paramType = "query")
  })
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful retrieval of group resource", response = GroupResponse.class)}
  )
  public Response getGroup(@Context HttpHeaders headers, @Context UriInfo ui,
                           @ApiParam(value = "group name", required = true) @PathParam("groupName") String groupName) {
    return handleRequest(headers, null, ui, Request.Type.GET, createGroupResource(groupName));
  }

  /**
   * Creates a group.
   * Handles: POST /groups requests.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @return information regarding the created group
   */
   @POST
   @Produces("text/plain")
   @ApiOperation(value = "Create new group", nickname = "GroupService#createGroup", notes = "Creates group resource.")
   @ApiImplicitParams({
     @ApiImplicitParam(name = "body", value = "input parameters in json form", required = true, dataType = "org.apache.ambari.server.controller.GroupRequest", paramType = "body")
   })
   @ApiResponses(value = {
     @ApiResponse(code = 200, message = "successful operation"),
     @ApiResponse(code = 500, message = "Server Error")}
   )
   public Response createGroup(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.POST, createGroupResource(null));
  }

  /**
   * Creates a group.
   * Handles: POST /groups/{groupName} requests.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param groupName    the group name
   * @return information regarding the created group
   *
   * @deprecated Use requests to /groups instead.
   */
   @POST @ApiIgnore // deprecated
   @Deprecated
   @Path("{groupName}")
   @Produces("text/plain")
   public Response createGroup(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                 @PathParam("groupName") String groupName) {
    return handleRequest(headers, body, ui, Request.Type.POST, createGroupResource(groupName));
  }

  /**
   * Deletes a group.
   * Handles:  DELETE /groups/{groupName} requests.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param groupName    the group name
   * @return information regarding the deleted group
   */
  @DELETE
  @Path("{groupName}")
  @Produces("text/plain")
  @ApiOperation(value = "Delete group", nickname = "GroupService#deleteGroup", notes = "Delete group resource.")
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful operation"),
    @ApiResponse(code = 500, message = "Server Error")}
  )
  public Response deleteGroup(@Context HttpHeaders headers, @Context UriInfo ui,
                              @ApiParam(value = "group name", required = true) @PathParam("groupName") String groupName) {
    return handleRequest(headers, null, ui, Request.Type.DELETE, createGroupResource(groupName));
  }

  /**
   * Create a group resource instance.
   *
   * @param groupName group name
   *
   * @return a group resource instance
   */
  private ResourceInstance createGroupResource(String groupName) {
    return createResource(Resource.Type.Group,
        Collections.singletonMap(Resource.Type.Group, groupName));
  }
}
