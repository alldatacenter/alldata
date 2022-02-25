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

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.BaseService;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.controller.ViewUrlResponseSwagger;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.http.HttpStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Service responsible for view resource requests.
 */
@Path("/view/urls")
@Api(value = "Views", description = "Endpoint for view specific operations")
public class ViewUrlsService extends BaseService {

  private static final String VIEW_URL_INFO_TYPE = "org.apache.ambari.server.controller.ViewUrlResponseSwagger";

  /**
   * Get the list of all registered view URLs
   * @param headers
   * @param ui

   * @return collections of all view urls and any instances registered against them
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all view URLs", response = ViewUrlResponseSwagger.class, responseContainer = "List")
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "ViewUrlInfo/*", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
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
  public Response getViewUrls(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET, createViewUrlResource(null));
  }

  /**
   * Create a new View URL
   * @param body
   * @param headers
   * @param ui
   * @param urlName
   * @return
   */
  @POST
  @Path("{urlName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Create view URL")
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = VIEW_URL_INFO_TYPE, paramType = PARAM_TYPE_BODY)
  })
  @ApiResponses({
          @ApiResponse(code = HttpStatus.SC_CREATED, message = MSG_SUCCESSFUL_OPERATION),
          @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
          @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
          @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response createUrl(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                            @PathParam("urlName") String urlName) {
    return handleRequest(headers, body, ui, Request.Type.POST, createViewUrlResource(urlName));
  }

  /**
   * Update a view URL
   * @param body
   * @param headers
   * @param ui
   * @param urlName
   * @return
   */
  @PUT
  @Path("{urlName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Update view URL")
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = VIEW_URL_INFO_TYPE, paramType = PARAM_TYPE_BODY)
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_ACCEPTED, message = MSG_REQUEST_ACCEPTED),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_OR_HOST_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response updateUrl(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                            @PathParam("urlName") String urlName) {
    return handleRequest(headers, body, ui, Request.Type.PUT, createViewUrlResource(urlName));
  }

  /**
   * Get information about a single view URL
   * @param headers
   * @param ui
   * @param urlName
   * @return
   */
  @GET
  @Path("{urlName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get single view URL", nickname = "getViewUrl", response = ViewUrlResponseSwagger.class)
  @ApiImplicitParams({
          @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "ViewUrlInfo/*", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
  })
  public Response getUrl(@Context HttpHeaders headers, @Context UriInfo ui,
                         @PathParam("urlName") String urlName) {
    return handleRequest(headers, null, ui, Request.Type.GET, createViewUrlResource(urlName));
  }

  /**
   * Remove a view URL
   * @param body
   * @param headers
   * @param ui
   * @param urlName
   * @return
   */
  @DELETE
  @Path("{urlName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Delete view URL")
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_OR_HOST_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deleteUrl(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                            @PathParam("urlName") String urlName) {
    return handleRequest(headers, body, ui, Request.Type.DELETE, createViewUrlResource(urlName));
  }

  // ----- helper methods ----------------------------------------------------

  /**
   * Create a view URL resource.
   *
   * @param urlName Name of the URL
   *
   * @return a view URL resource instance
   */
  private ResourceInstance createViewUrlResource(final String urlName) {
    return createResource(Resource.Type.ViewURL, Collections.singletonMap(Resource.Type.ViewURL, urlName));
  }
}
