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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.BaseService;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.controller.GroupPrivilegeResponse;
import org.apache.ambari.server.controller.PrivilegeResponse;
import org.apache.ambari.server.controller.spi.Resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 *  Service responsible for group privilege resource requests.
 */
@Path("/groups/{groupName}/privileges")
@Api(value = "Groups", description = "Endpoint for group specific operations")
public class GroupPrivilegeService extends BaseService {

  /**
   * Handles: GET  /groups/{groupName}/privileges
   * Get all group privileges.
   * @param headers
   * @param ui
   * @param groupName
   * @return
   */
  @GET
  @Produces("text/plain")
  @ApiOperation(value = "Get all privileges", nickname = "GroupPrivilegeService#getPrivileges", notes = "Returns all privileges for group.", response = GroupPrivilegeResponse.class, responseContainer = "List")
  @ApiImplicitParams({
    @ApiImplicitParam(name = "fields", value = "Filter user privileges", defaultValue = "PrivilegeInfo/*", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "sortBy", value = "Sort user privileges (asc | desc)", defaultValue = "PrivilegeInfo/user_name.asc", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "page_size", value = "The number of resources to be returned for the paged response.", defaultValue = "10", dataType = "integer", paramType = "query"),
    @ApiImplicitParam(name = "from", value = "The starting page resource (inclusive). Valid values are :offset | \"start\"", defaultValue = "0", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "to", value = "The ending page resource (inclusive). Valid values are :offset | \"end\"", dataType = "string", paramType = "query")
  })

  public Response getPrivileges(@Context HttpHeaders headers, @Context UriInfo ui,
                                @ApiParam(value = "group name", required = true) @PathParam("groupName") String groupName) {
    return handleRequest(headers, null, ui, Request.Type.GET, createPrivilegeResource(groupName, null));
  }


  /**
   * Handles: GET /groups/{groupName}/privileges{privilegeID}
   * Get a specific privilege for a group.
   *
   * @param headers       http headers
   * @param ui            uri info
   * @param groupName     group name
   * @param privilegeId   privilege id
   * @return privilege instance representation
   */
  @GET
  @Path("{privilegeId}")
  @Produces("text/plain")
  @ApiOperation(value = "Get group privilege", nickname = "GroupPrivilegeService#getPrivilege", notes = "Returns group privilege details.", response = GroupPrivilegeResponse.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = "fields", value = "Filter group privilege details", defaultValue = "PrivilegeInfo/*", dataType = "string", paramType = "query")
  })
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful operation", response = PrivilegeResponse.class)}
  )
  public Response getPrivilege(@Context HttpHeaders headers, @Context UriInfo ui, @ApiParam(value = "group name", required = true) @PathParam ("groupName") String groupName,
                               @ApiParam(value = "privilege id", required = true) @PathParam("privilegeId") String privilegeId) {
    return handleRequest(headers, null, ui, Request.Type.GET, createPrivilegeResource(groupName, privilegeId));
  }



  protected ResourceInstance createPrivilegeResource(String groupName, String privilegeId) {
    final Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Group, groupName);
    mapIds.put(Resource.Type.GroupPrivilege, privilegeId);
    return createResource(Resource.Type.GroupPrivilege, mapIds);
  }
}