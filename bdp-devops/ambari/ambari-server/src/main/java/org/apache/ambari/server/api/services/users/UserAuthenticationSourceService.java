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

package org.apache.ambari.server.api.services.users;

import static org.apache.ambari.server.controller.internal.UserAuthenticationSourceResourceProvider.AUTHENTICATION_AUTHENTICATION_SOURCE_ID_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.UserAuthenticationSourceResourceProvider.AUTHENTICATION_SOURCE_RESOURCE_CATEGORY;
import static org.apache.ambari.server.controller.internal.UserAuthenticationSourceResourceProvider.AUTHENTICATION_USER_NAME_PROPERTY_ID;

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

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.BaseService;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.controller.UserAuthenticationSourceResponse;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Service responsible for user authentication source resource requests.
 */
@Path("/users/{userName}/sources")
@Api(value = "User Authentication Sources", description = "Endpoint for user specific authentication source operations")
public class UserAuthenticationSourceService extends BaseService {

  private static final String CREATE_REQUEST_TYPE = "org.apache.ambari.server.controller.UserAuthenticationSourceRequestCreateSwagger";
  private static final String UPDATE_REQUEST_TYPE = "org.apache.ambari.server.controller.UserAuthenticationSourceRequestUpdateSwagger";
  private static final String AUTHENTICATION_SOURCE_DEFAULT_SORT = AUTHENTICATION_AUTHENTICATION_SOURCE_ID_PROPERTY_ID + ".asc";

  /**
   * Handles: GET  /users/{userName}/sources
   * Get all authentication sources for the user.
   *
   * @param headers  http headers
   * @param ui       uri info
   * @param userName user name
   * @return user resource instance representation
   */
  @GET
  @Produces("text/plain")
  @ApiOperation(value = "Get all authentication sources", response = UserAuthenticationSourceResponse.UserAuthenticationSourceResponseSwagger.class, responseContainer = "List")
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY, defaultValue = AUTHENTICATION_AUTHENTICATION_SOURCE_ID_PROPERTY_ID + "," + AUTHENTICATION_USER_NAME_PROPERTY_ID),
      @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY, defaultValue = AUTHENTICATION_SOURCE_DEFAULT_SORT),
      @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, allowableValues = QUERY_FROM_VALUES, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, allowableValues = QUERY_TO_VALUES, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
  })
  @ApiResponses({
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
      @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response getAuthenticationSources(@Context HttpHeaders headers, @Context UriInfo ui,
                                           @ApiParam(value = "user name", required = true) @PathParam("userName") String userName) {
    return handleRequest(headers, null, ui, Request.Type.GET, createResource(userName, null));
  }

  /**
   * Handles: GET /users/{userName}/sources/{sourceID}
   * Get a specific authentication source.
   *
   * @param headers  http headers
   * @param ui       uri info
   * @param userName user name
   * @param sourceId authentication source id
   * @return authentication source instance representation
   */
  @GET
  @Path("{sourceId}")
  @Produces("text/plain")
  @ApiOperation(value = "Get user authentication source", response = UserAuthenticationSourceResponse.UserAuthenticationSourceResponseSwagger.class)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY, defaultValue = AUTHENTICATION_SOURCE_RESOURCE_CATEGORY + "/*"),
  })
  @ApiResponses({
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
      @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response getAuthenticationSource(@Context HttpHeaders headers, @Context UriInfo ui,
                                          @ApiParam(value = "user name", required = true) @PathParam("userName") String userName,
                                          @ApiParam(value = "source id", required = true) @PathParam("sourceId") String sourceId) {
    return handleRequest(headers, null, ui, Request.Type.GET, createResource(userName, sourceId));
  }

  /**
   * Creates an authentication source.
   * Handles: POST /users/{userName}/sources
   *
   * @param headers  http headers
   * @param ui       uri info
   * @param userName user name
   * @return information regarding the created user
   */
  @POST
  @Produces("text/plain")
  @ApiOperation(value = "Create one or more new authentication sources for a user")
  @ApiImplicitParams({
      @ApiImplicitParam(dataType = CREATE_REQUEST_TYPE, paramType = PARAM_TYPE_BODY, allowMultiple = true)
  })
  @ApiResponses({
      @ApiResponse(code = HttpStatus.SC_CREATED, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_ACCEPTED, message = MSG_REQUEST_ACCEPTED),
      @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_CONFLICT, message = MSG_RESOURCE_ALREADY_EXISTS),
      @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
      @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response createAuthenticationSources(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                              @ApiParam(value = "user name", required = true) @PathParam("userName") String userName) {
    return handleRequest(headers, body, ui, Request.Type.POST, createResource(userName, null));
  }

  /**
   * Creates an authentication source.
   * Handles: PUT /users/{userName}/sources/{sourceId}
   *
   * @param headers  http headers
   * @param ui       uri info
   * @param userName user name
   * @param sourceId authentication source id
   * @return information regarding the created user
   */
  @PUT
  @Path("{sourceId}")
  @Produces("text/plain")
  @ApiOperation(value = "Updates an existing authentication source")
  @ApiImplicitParams({
      @ApiImplicitParam(dataType = UPDATE_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
  })
  @ApiResponses({
      @ApiResponse(code = HttpStatus.SC_ACCEPTED, message = MSG_REQUEST_ACCEPTED),
      @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_CONFLICT, message = MSG_RESOURCE_ALREADY_EXISTS),
      @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
      @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response updateAuthenticationSource(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                             @ApiParam(value = "user name", required = true) @PathParam("userName") String userName,
                                             @ApiParam(value = "source id", required = true) @PathParam("sourceId") String sourceId) {
    return handleRequest(headers, body, ui, Request.Type.PUT, createResource(userName, sourceId));
  }

  /**
   * Delete an authentication source.
   * Handles: DELETE /users/{userName}/sources/{sourceId}
   *
   * @param headers  http headers
   * @param ui       uri info
   * @param userName user name
   * @param sourceId authentication source id
   * @return information regarding the created user
   */
  @DELETE
  @Path("{sourceId}")
  @Produces("text/plain")
  @ApiOperation(value = "Deletes an existing authentication source")
  @ApiResponses({
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
      @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deleteAuthenticationSource(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                             @ApiParam(value = "user name", required = true) @PathParam("userName") String userName,
                                             @ApiParam(value = "source id", required = true) @PathParam("sourceId") String sourceId) {
    return handleRequest(headers, body, ui, Request.Type.DELETE, createResource(userName, sourceId));
  }

  protected ResourceInstance createResource(String userName, String sourceId) {
    final Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.User, StringUtils.lowerCase(userName));
    mapIds.put(Resource.Type.UserAuthenticationSource, sourceId);
    return createResource(Resource.Type.UserAuthenticationSource, mapIds);
  }
}
