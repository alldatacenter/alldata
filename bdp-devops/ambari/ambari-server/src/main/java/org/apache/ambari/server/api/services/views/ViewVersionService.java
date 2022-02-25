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
import org.apache.ambari.server.controller.ViewVersionResponse;
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
 * Service responsible for view version resource requests.
 */
@Path("/views/{viewName}/versions")
@Api(value = "Views", description = "Endpoint for view specific operations")
public class ViewVersionService extends BaseService {

  /**
   * Handles: GET  /views/{viewName}/versions
   * Get all views versions.
   *
   * @param headers  http headers
   * @param ui       uri info
   * @param viewName   view id
   *
   * @return view collection resource representation
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all versions for a view", response = ViewVersionResponse.class, responseContainer = "List")
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "ViewVersionInfo/*", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
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
  public Response getVersions(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                              @ApiParam(value = "view name") @PathParam("viewName") String viewName) {

    return handleRequest(headers, body, ui, Request.Type.GET, createResource(viewName, null));
  }

  /**
   * Handles: GET /views/{viewName}/versions/{version}
   * Get a specific view version.
   *
   * @param headers  http headers
   * @param ui       uri info
   * @param viewName   view id
   * @param version  version id
   *
   * @return view instance representation
   */
  @GET
  @Path("{version}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get single view version", response = ViewVersionResponse.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "ViewVersionInfo/*", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
  })
  @ApiResponses(value = {
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
  })
  public Response getVersion(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                              @ApiParam(value = "view name") @PathParam("viewName") String viewName,
                              @PathParam("version") String version) {

    return handleRequest(headers, body, ui, Request.Type.GET, createResource(viewName, version));
  }

  /**
   * Handles: POST /views/{viewName}/versions/{version}
   * Create a specific view version.
   *
   * @param headers    http headers
   * @param ui         uri info
   * @param viewName   view id
   * @param version    the version
   *
   * @return information regarding the created view
   */
  @POST @ApiIgnore // until documented, unsupported method
  @Path("{version}")
  @Produces(MediaType.TEXT_PLAIN)
  public Response createVersions(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                 @ApiParam(value = "view name") @PathParam("viewName") String viewName,
                                 @PathParam("version") String version) {

    return handleRequest(headers, body, ui, Request.Type.POST, createResource(viewName, version));
  }

  /**
   * Handles: PUT /views/{viewName}/versions/{version}
   * Update a specific view version.
   *
   * @param headers   http headers
   * @param ui        uri info
   * @param viewName  view id
   * @param version   the version
   *
   * @return information regarding the updated view
   */
  @PUT @ApiIgnore // until documented, unsupported method
  @Path("{version}")
  @Produces(MediaType.TEXT_PLAIN)
  public Response updateVersions(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                 @ApiParam(value = "view name") @PathParam("viewName") String viewName,
                                 @PathParam("version") String version) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createResource(viewName, version));
  }

  /**
   * Handles: DELETE /views/{viewName}/versions/{version}
   * Delete a specific view version.
   *
   * @param headers   http headers
   * @param ui        uri info
   * @param viewName   view id
   * @param version   version id
   *
   * @return information regarding the deleted view version
   */
  @DELETE @ApiIgnore // until documented, unsupported method
  @Path("{version}")
  @Produces(MediaType.TEXT_PLAIN)
  public Response deleteVersions(@Context HttpHeaders headers, @Context UriInfo ui,
                                 @PathParam("viewName") String viewName, @PathParam("version") String version) {

    return handleRequest(headers, null, ui, Request.Type.DELETE, createResource(viewName, version));
  }

  /**
   * Get the permissions sub-resource
   *
   * @param version  the version
   *
   * @return the permission service

  @Path("{version}/permissions")
  public ViewPermissionService getPermissionHandler(@PathParam("version") String version) {

    return new ViewPermissionService(viewName, version);
  }

  // ----- helper methods ----------------------------------------------------

  /**
   * Create a view resource.
   *
   * @param viewName view name
   *
   * @return a view resource instance
   */
  private ResourceInstance createResource(String viewName, String version) {
    Map<Resource.Type,String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.View, viewName);
    mapIds.put(Resource.Type.ViewVersion, version);
    return createResource(Resource.Type.ViewVersion, mapIds);
  }
}
