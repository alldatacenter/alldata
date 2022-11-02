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

import static org.apache.ambari.server.controller.internal.UserResourceProvider.USER_RESOURCE_CATEGORY;
import static org.apache.ambari.server.controller.internal.UserResourceProvider.USER_USERNAME_PROPERTY_ID;

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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.BaseService;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.controller.UserResponse;
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
 * Service responsible for user requests.
 */
@Path("/users/")
@Api(value = "Users", description = "Endpoint for user specific operations")
public class UserService extends BaseService {

  private static final String UPDATE_USER_REQUEST_TYPE = "org.apache.ambari.server.controller.UserRequestUpdateUserSwagger";
  private static final String CREATE_USER_REQUEST_TYPE = "org.apache.ambari.server.controller.UserRequestCreateUserSwagger";
  private static final String CREATE_USERS_REQUEST_TYPE = "org.apache.ambari.server.controller.UserRequestCreateUsersSwagger";;
  private static final String USER_DEFAULT_SORT = USER_USERNAME_PROPERTY_ID + ".asc";

  /**
   * Gets all users.
   * Handles: GET /users requests.
   */
  @GET
  @Produces("text/plain")
  @ApiOperation(value = "Get all users", response = UserResponse.UserResponseSwagger.class, responseContainer = "List")
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY, defaultValue = USER_USERNAME_PROPERTY_ID),
      @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY, defaultValue = USER_DEFAULT_SORT),
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
  public Response getUsers(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET, createUserResource(null));
  }

  /**
   * Gets a single user.
   * Handles: GET /users/{username} requests
   *
   * @param headers  http headers
   * @param ui       uri info
   * @param userName the username
   * @return information regarding the created user
   */
  @GET
  @Path("{userName}")
  @Produces("text/plain")
  @ApiOperation(value = "Get single user", response = UserResponse.UserResponseSwagger.class)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY, defaultValue = USER_RESOURCE_CATEGORY + "/*"),
  })
  @ApiResponses({
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
      @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response getUser(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                          @ApiParam(value = "user name", required = true) @PathParam("userName") String userName) {
    return handleRequest(headers, body, ui, Request.Type.GET, createUserResource(userName));
  }

  /**
   * Creates a user.
   * Handles: POST /users
   *
   * @param headers http headers
   * @param ui      uri info
   * @return information regarding the created user
   */
  @POST
  @Produces("text/plain")
  @ApiOperation(value = "Creates one or more users in a single request")
  @ApiImplicitParams({
      @ApiImplicitParam(dataType = CREATE_USERS_REQUEST_TYPE, paramType = PARAM_TYPE_BODY, allowMultiple = true)
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
  public Response createUsers(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.POST, createUserResource(null));
  }

  /**
   * Creates a user.
   * Handles: POST /users/{username}
   *
   * @param headers  http headers
   * @param ui       uri info
   * @param userName the username
   * @return information regarding the created user
   */
  @POST
  @Path("{userName}")
  @Produces("text/plain")
  @ApiOperation(value = "Create new user")
  @ApiImplicitParams({
      @ApiImplicitParam(dataType = CREATE_USER_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
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
  public Response createUser(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @ApiParam(value = "user name", required = true) @PathParam("userName") String userName) {
    return handleRequest(headers, body, ui, Request.Type.POST, createUserResource(userName));
  }

  /**
   * Updates a specific user.
   * Handles: PUT /users/{userName}
   *
   * @param headers  http headers
   * @param ui       uri info
   * @param userName the username
   * @return information regarding the updated user
   */
  @PUT
  @Path("{userName}")
  @Produces("text/plain")
  @ApiOperation(value = "Update user details")
  @ApiImplicitParams({
      @ApiImplicitParam(dataType = UPDATE_USER_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
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
  public Response updateUser(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @ApiParam(value = "user name", required = true) @PathParam("userName") String userName) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createUserResource(userName));
  }

  /**
   * Deletes a user.
   * Handles:  DELETE /users/{userName}
   */
  @DELETE
  @Path("{userName}")
  @Produces("text/plain")
  @ApiOperation(value = "Delete single user")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successful operation"),
      @ApiResponse(code = 500, message = "Server Error")}
  )
  public Response deleteUser(@Context HttpHeaders headers, @Context UriInfo ui,
                             @ApiParam(value = "user name", required = true) @PathParam("userName") String userName) {
    return handleRequest(headers, null, ui, Request.Type.DELETE, createUserResource(userName));
  }

  /**
   * Create a user resource instance.
   *
   * @param userName user name
   * @return a user resource instance
   */
  private ResourceInstance createUserResource(String userName) {
    return createResource(Resource.Type.User,
        Collections.singletonMap(Resource.Type.User, userName));
  }
}
