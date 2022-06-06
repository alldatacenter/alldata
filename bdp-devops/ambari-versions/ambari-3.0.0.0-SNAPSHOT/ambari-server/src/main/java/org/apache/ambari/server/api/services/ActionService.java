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
import org.apache.ambari.server.controller.ActionResponse;
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
 * Service responsible for action definition resource requests.
 */
@Path("/actions/")
@Api(value = "Actions", description = "Endpoint for action definition specific operations")
public class ActionService extends BaseService {

  private static final String ACTION_REQUEST_TYPE = "org.apache.ambari.server.controller.ActionRequestSwagger";

  /**
   * Handles: GET /actions/{actionName}
   * Get a specific action definition.
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param actionName action name
   * @return action definition instance representation
   */
  @GET
  @Path("{actionName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get the details of an action definition",
      nickname = "ActionService#getActionDefinition",
      response = ActionResponse.ActionResponseSwagger.class)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION,
          defaultValue = "Actions/*",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getActionDefinition(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @ApiParam(required = true) @PathParam("actionName") String actionName) {

    return handleRequest(headers, body, ui, Request.Type.GET, createActionDefinitionResource(actionName));
  }

  /**
   * Handles: GET  /actions
   * Get all action definitions.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return action definition collection resource representation
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all action definitions",
      nickname = "ActionService#getActionDefinitions",
      response = ActionResponse.ActionResponseSwagger.class,
      responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION,
          defaultValue = "Actions/action_name",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION,
          defaultValue = "Actions/action_name.asc",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getActionDefinitions(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET, createActionDefinitionResource(null));
  }

  /**
   * Handles: POST /actions/{actionName}
   * Create a specific action definition.
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param actionName  action name
   * @return information regarding the action definition being created
   */
   @POST
   @Path("{actionName}")
   @Produces(MediaType.TEXT_PLAIN)
   @ApiOperation(value = "Creates an action definition - Currently Not Supported",
       nickname = "ActionService#createActionDefinition"
   )
   @ApiImplicitParams({
       @ApiImplicitParam(dataType = ACTION_REQUEST_TYPE, paramType = "body", allowMultiple = false)
   })
   @ApiResponses({
       @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_REQUEST),
       @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
       @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
       @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
   })
   public Response createActionDefinition(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                 @ApiParam(required = true) @PathParam("actionName") String actionName) {

    return handleRequest(headers, body, ui, Request.Type.POST, createActionDefinitionResource(actionName));
  }

  /**
   * Handles: PUT /actions/{actionName}
   * Update a specific action definition.
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param actionName  action name
   * @return information regarding the updated action
   */
  @PUT
  @Path("{actionName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Updates an action definition - Currently Not Supported",
      nickname = "ActionService#updateActionDefinition"
  )
  @ApiImplicitParams({
      @ApiImplicitParam(dataType = ACTION_REQUEST_TYPE, paramType = "body")
  })
  @ApiResponses({
      @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_REQUEST),
      @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
      @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response updateActionDefinition(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @ApiParam(required = true) @PathParam("actionName") String actionName) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createActionDefinitionResource(actionName));
  }

  /**
   * Handles: DELETE /actions/{actionName}
   * Delete a specific action definition.
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param actionName  action name
   * @return information regarding the deleted action definition
   */
  @DELETE
  @Path("{actionName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Deletes an action definition - Currently Not Supported",
      nickname = "ActionService#deleteActionDefinition"
  )
  @ApiResponses({
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
      @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deleteActionDefinition(@Context HttpHeaders headers, @Context UriInfo ui,
                                @ApiParam(required = true) @PathParam("actionName") String actionName) {
    return handleRequest(headers, null, ui, Request.Type.DELETE, createActionDefinitionResource(actionName));
  }

  /**
   * Create a action definition resource instance.
   *
   * @param actionName action name
   *
   * @return a action definition resource instance
   */
  ResourceInstance createActionDefinitionResource(String actionName) {
    return createResource(Resource.Type.Action,
        Collections.singletonMap(Resource.Type.Action, actionName));
  }
}
