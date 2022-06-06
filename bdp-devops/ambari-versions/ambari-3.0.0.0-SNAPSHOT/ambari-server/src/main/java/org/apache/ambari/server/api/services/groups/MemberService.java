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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.BaseService;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.controller.MemberResponse;
import org.apache.ambari.server.controller.spi.Resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Service responsible for user membership requests.
 */
@Path("/groups/{groupName}/members")
@Api(value = "Groups", description = "Endpoint for group specific operations")
public class MemberService extends BaseService {
  /**
   * Creates new members.
   * Handles: POST /groups/{groupname}/members requests.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param groupName    group name
   * @return information regarding the created member
   */
   @POST @ApiIgnore // until documented
   @Produces("text/plain")
   public Response createMember(String body, @Context HttpHeaders headers, @Context UriInfo ui, @PathParam("groupName") String groupName) {
    return handleRequest(headers, body, ui, Request.Type.POST, createMemberResource(groupName, null));
  }

  /**
   * Creates a new member.
   * Handles: POST /groups/{groupname}/members/{username} requests.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param groupName    group name
   * @param userName     the user name
   * @return information regarding the created member
   */
   @POST @ApiIgnore // until documented
   @Path("{userName}")
   @Produces("text/plain")
   public Response createMember(String body, @Context HttpHeaders headers, @Context UriInfo ui, @PathParam("groupName") String groupName,
                                 @PathParam("userName") String userName) {
    return handleRequest(headers, body, ui, Request.Type.POST, createMemberResource(groupName, userName));
  }

   /**
    * Deletes a member.
    * Handles:  DELETE /groups/{groupname}/members/{username} requests.
    *
    * @param headers      http headers
    * @param ui           uri info
    * @param groupName    group name
    * @param userName     the user name
    * @return information regarding the deleted group
    */
   @DELETE
   @Path("{userName}")
   @Produces("text/plain")
   @ApiOperation(value = "Delete group member", nickname = "MemberService#deleteMember", notes = "Delete member resource.")
   @ApiResponses(value = {
     @ApiResponse(code = 200, message = "Successful operation"),
     @ApiResponse(code = 500, message = "Server Error")}
   )
   public Response deleteMember(@Context HttpHeaders headers, @Context UriInfo ui, @ApiParam(value = "group name", required = true) @PathParam("groupName") String groupName,
                                @ApiParam(value = "user name", required = true) @PathParam("userName") String userName) {
     return handleRequest(headers, null, ui, Request.Type.DELETE, createMemberResource(groupName, userName));
   }

  /**
   * Gets all members.
   * Handles: GET /groups/{groupname}/members requests.
   *
   * @param headers    http headers
   * @param ui         uri info
   * @param groupName  group name
   * @return information regarding all members
   */
  @GET
  @Produces("text/plain")
  @ApiOperation(value = "Get all group members", nickname = "MemberService#getMembers", notes = "Returns details of all members.", response = MemberResponse.class, responseContainer = "List")
  @ApiImplicitParams({
    @ApiImplicitParam(name = "fields", value = "Filter member details", defaultValue = "MemberInfo/*", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "sortBy", value = "Sort members (asc | desc)", defaultValue = "MemberInfo/user_name.asc", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "page_size", value = "The number of resources to be returned for the paged response.", defaultValue = "10", dataType = "integer", paramType = "query"),
    @ApiImplicitParam(name = "from", value = "The starting page resource (inclusive). Valid values are :offset | \"start\"", defaultValue = "0", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "to", value = "The ending page resource (inclusive). Valid values are :offset | \"end\"", dataType = "string", paramType = "query")
  })
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful operation", response = MemberResponse.class, responseContainer = "List")}
  )
  public Response getMembers(@Context HttpHeaders headers, @Context UriInfo ui, @ApiParam(value = "group name", required = true) @PathParam("groupName") String groupName) {
    return handleRequest(headers, null, ui, Request.Type.GET, createMemberResource(groupName, null));
  }

  /**
   * Gets member.
   * Handles: GET /groups/{groupname}/members/{username} requests.
   *
   * @param headers    http headers
   * @param ui         uri info
   * @param groupName    group name
   * @param userName   the user name
   * @return information regarding the specific member
   */
  @GET
  @Path("{userName}")
  @Produces("text/plain")
  @ApiOperation(value = "Get group member", nickname = "MemberService#getMember", notes = "Returns member details.", response = MemberResponse.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = "fields", value = "Filter member details", defaultValue = "MemberInfo", dataType = "string", paramType = "query")
  })
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful operation", response = MemberResponse.class)}
  )
  public Response getMember(@Context HttpHeaders headers, @Context UriInfo ui, @ApiParam(value = "group name", required = true)  @PathParam("groupName") String groupName,
                            @ApiParam(value = "user name", required = true) @PathParam("userName") String userName) {
    return handleRequest(headers, null, ui, Request.Type.GET, createMemberResource(groupName, userName));
  }

  /**
   * Updates all members.
   * Handles: PUT /groups/{groupname}/members requests.
   *
   * @param headers    http headers
   * @param ui         uri info
   * @param groupName    group name
   * @return status of the request
   */
  @PUT
  @Produces("text/plain")
  @ApiOperation(value = "Update group members", nickname = "MemberService#updateMembers", notes = "Updates group member resources.", responseContainer = "List")
  @ApiImplicitParams({
    @ApiImplicitParam(name = "body", value = "input parameters in json form", required = true, dataType = "org.apache.ambari.server.controller.MemberRequest", paramType = "body")
  })
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful operation"),
    @ApiResponse(code = 500, message = "Server Error")}
  )
  public Response updateMembers(String body, @Context HttpHeaders headers, @Context UriInfo ui, @ApiParam(value = "group name", required = true)
                                 @PathParam("groupName") String groupName) {
    return handleRequest(headers, body, ui, Request.Type.PUT, createMemberResource(groupName, null));
  }

  /**
   * Create a member resource instance.
   *
   * @param groupName  group name
   * @param userName   user name
   *
   * @return a member resource instance
   */
  private ResourceInstance createMemberResource(String groupName, String userName) {
    final Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Group, groupName);
    mapIds.put(Resource.Type.Member, userName);
    return createResource(Resource.Type.Member, mapIds);
  }
}
