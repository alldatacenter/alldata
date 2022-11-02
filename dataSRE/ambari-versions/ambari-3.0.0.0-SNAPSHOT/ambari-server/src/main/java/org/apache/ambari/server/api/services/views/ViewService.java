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

import java.util.Collections;

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
import org.apache.ambari.server.controller.ViewResponse;
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
 * Service responsible for view resource requests.
 */
@Path("/views/")
@Api(value = "Views", description = "Endpoint for view specific operations")
public class ViewService extends BaseService {

  /**
   * Handles: GET  /views
   * Get all views.
   *
   * @param headers  http headers
   * @param ui       uri info
   *
   * @return view collection resource representation
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all views", nickname = "getViews", notes = "Returns details of all views.", response = ViewResponse.class, responseContainer = "List")
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "Views/*", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION, defaultValue = "Views/view_name.asc", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
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
  public Response getViews(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET, createViewResource(null));
  }

  /**
   * Handles: GET /views/{viewID}
   * Get a specific view.
   *
   * @param headers    http headers
   * @param ui         uri info
   * @param viewName   view id
   *
   * @return view instance representation
   */
  @GET
  @Path("{viewName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get single view", response = ViewResponse.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "Views/*", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_OR_HOST_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
  })
  public Response getView(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                          @ApiParam(value = "view name", required = true) @PathParam("viewName") String viewName) {

    return handleRequest(headers, body, ui, Request.Type.GET, createViewResource(viewName));
  }

  /**
   * Handles: POST /views/{viewID}
   * Create a specific view.
   *
   * @param headers    http headers
   * @param ui         uri info
   * @param viewName   view id
   *
   * @return information regarding the created view
   */
  @POST @ApiIgnore // until documented
  @Path("{viewName}")
  @Produces(MediaType.TEXT_PLAIN)
  public Response createView(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("viewName") String viewName) {
    return handleRequest(headers, body, ui, Request.Type.POST, createViewResource(viewName));
  }

  /**
   * Handles: PUT /views/{viewID}
   * Update a specific view.
   *
   * @param headers   http headers
   * @param ui        uri info
   * @param viewName  view id
   *
   * @return information regarding the updated view
   */
  @PUT @ApiIgnore // until documented
  @Path("{viewName}")
  @Produces(MediaType.TEXT_PLAIN)
  public Response updateView(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @ApiParam(value = "view name", required = true) @PathParam("viewName") String viewName) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createViewResource(viewName));
  }

  /**
   * Handles: DELETE /views/{viewID}
   * Delete a specific view.
   *
   * @param headers   http headers
   * @param ui        uri info
   * @param viewName  view id
   *
   * @return information regarding the deleted view
   */
  @DELETE @ApiIgnore // until documented
  @Path("{viewName}")
  @Produces(MediaType.TEXT_PLAIN)
  public Response deleteView(@Context HttpHeaders headers, @Context UriInfo ui,
                             @ApiParam(value = "view name", required = true) @PathParam("viewName") String viewName) {

    return handleRequest(headers, null, ui, Request.Type.DELETE, createViewResource(viewName));
  }

  // ----- helper methods ----------------------------------------------------

  /**
   * Create a view resource.
   *
   * @param viewName view name
   *
   * @return a view resource instance
   */
  private ResourceInstance createViewResource(String viewName) {
    return createResource(Resource.Type.View,
        Collections.singletonMap(Resource.Type.View, viewName));
  }
}
