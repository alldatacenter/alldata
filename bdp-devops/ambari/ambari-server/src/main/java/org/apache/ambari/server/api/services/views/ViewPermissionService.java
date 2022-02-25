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

package org.apache.ambari.server.api.services.views;

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

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.BaseService;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.controller.ViewPermissionResponse;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.http.HttpStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Service responsible for custom view permission resource requests.
 */
@Path("/views/{viewName}/versions/{version}/permissions")
@Api(value = "Views", description = "Endpoint for view specific operations")
public class ViewPermissionService extends BaseService {

  /**
   * Handles: GET  /permissions
   * Get all permissions.
   *
   * @param headers    http headers
   * @param ui         uri info
   * @param viewName   view id
   * @param version    version id
   *
   * @return permission collection resource representation
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all permissions for a view", response = ViewPermissionResponse.class, responseContainer = "List")
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "PermissionInfo/*", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
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
  public Response getPermissions(@Context HttpHeaders headers, @Context UriInfo ui,
                                 @ApiParam(value = "view name") @PathParam("viewName") String viewName,
                                 @ApiParam(value = "view version") @PathParam("version") String version) {
    return handleRequest(headers, null, ui, Request.Type.GET, createPermissionResource(
      viewName, version, null));
  }

  /**
   * Handles: GET /views/{viewName}/versions/{version}/permissions/{permissionID}
   * Get a specific permission.
   *
   * @param headers        http headers
   * @param ui             uri info
   * @param viewName       view id
   * @param version        version id
   * @param permissionId   permission id
   *
   * @return permission instance representation
   */
  @GET
  @Path("{permissionId}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get single view permission", response = ViewPermissionResponse.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "PermissionInfo/*", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
  })
  public Response getPermission(@Context HttpHeaders headers, @Context UriInfo ui,
                                @ApiParam(value = "view name") @PathParam("viewName") String viewName,
                                @ApiParam(value = "view version") @PathParam("version") String version,
                                @ApiParam(value = "permission id") @PathParam("permissionId") String permissionId) {

    return handleRequest(headers, null, ui, Request.Type.GET, createPermissionResource(
        viewName, version, permissionId));
  }

  /**
   * Handles: POST /views/{viewName}/versions/{version}/permissions/{permissionID}
   * Create a specific permission.
   *
   * @param headers        http headers
   * @param ui             uri info
   * @param viewName       view id
   * @param version        version id
   * @param permissionId   permission id
   *
   * @return information regarding the created permission
   */
  @POST @ApiIgnore // until documented, not supported
  @Path("{permissionId}")
  @Produces(MediaType.TEXT_PLAIN)
  public Response createPermission(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                   @ApiParam(value = "view name") @PathParam("viewName") String viewName,
                                   @ApiParam(value = "view version") @PathParam("version") String version,
                                   @ApiParam(value = "permission id") @PathParam("permissionId") String permissionId) {

    return handleRequest(headers, body, ui, Request.Type.POST, createPermissionResource(
        viewName, version, permissionId));
  }

  /**
   * Handles: PUT /views/{viewName}/versions/{version}/permissions/{permissionID}
   * Update a specific permission.
   *
   * @param headers       http headers
   * @param ui            uri info
   * @param viewName      view id
   * @param version       version id
   * @param permissionId  permission id
   * @return information regarding the updated permission
   */
  @PUT @ApiIgnore // until documented, not supported
  @Path("{permissionId}")
  @Produces(MediaType.TEXT_PLAIN)
  public Response updatePermission(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                   @ApiParam(value = "view name") @PathParam("viewName") String viewName,
                                   @ApiParam(value = "view version") @PathParam("version") String version,
                                   @ApiParam(value = "permission id") @PathParam("permissionId") String permissionId) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createPermissionResource(
        viewName, version, permissionId));
  }

  /**
   * Handles: DELETE /views/{viewName}/versions/{version}/permissions/{permissionID}
   * Delete a specific permission.
   *
   * @param headers       http headers
   * @param ui            uri info
   * @param viewName      view id
   * @param version       version id
   * @param permissionId  permission id
   *
   * @return information regarding the deleted permission
   */
  @DELETE @ApiIgnore // until documented, not supported
  @Path("{permissionId}")
  @Produces("text/plain")
  public Response deletePermission(@Context HttpHeaders headers, @Context UriInfo ui,
                                   @ApiParam(value = "view name") @PathParam("viewName") String viewName,
                                   @ApiParam(value = "view version") @PathParam("version") String version,
                                   @ApiParam(value = "permission id") @PathParam("permissionId") String permissionId) {

    return handleRequest(headers, null, ui, Request.Type.DELETE, createPermissionResource(
        viewName, version, permissionId));
  }

  // ----- helper methods ----------------------------------------------------

  /**
   * Create a permission resource.
   *
   * @param permissionId permission name
   *
   * @return a permission resource instance
   */
  protected ResourceInstance createPermissionResource(String viewName, String viewVersion, String permissionId) {
    Map<Resource.Type,String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.View, viewName);
    mapIds.put(Resource.Type.ViewVersion, viewVersion);
    mapIds.put(Resource.Type.ViewPermission, permissionId);

    return createResource(Resource.Type.ViewPermission, mapIds);
  }
}
