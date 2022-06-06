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
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.BlueprintSwagger;
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
 * Service responsible for handling REST requests for the /blueprints endpoint.
 * This blueprint resource is a blueprint template meaning that it doesn't contain
 * any cluster specific information.  Updates are not permitted as blueprints are
 * immutable.
 */
@Path("/blueprints/")
@Api(value = "Blueprints", description = "Endpoint for blueprint specific operations")
public class BlueprintService extends BaseService {

  public static final String BLUEPRINT_REQUEST_TYPE = "org.apache.ambari.server.controller.BlueprintSwagger";
  /**
   * Handles: GET  /blueprints
   * Get all blueprints.
   *
   * @param headers  http headers
   * @param ui       uri info
   * @return blueprint collection resource representation
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all blueprints",
      nickname = "BlueprintService#getBlueprints",
      response = BlueprintSwagger.class,
      responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION,
          defaultValue = "Blueprints/blueprint_name",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION,
          defaultValue = "Blueprints/blueprint_name.asc",
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
  public Response getBlueprints(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET, createBlueprintResource(null));
  }

  /**
   * Handles: GET /blueprints/{blueprintID}
   * Get a specific blueprint.
   *
   * @param headers        http headers
   * @param ui             uri info
   * @param blueprintName  blueprint id
   * @return blueprint instance representation
   */
  @GET
  @Path("{blueprintName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get the details of a blueprint",
      nickname = "BlueprintService#getBlueprint",
      response = BlueprintSwagger.class,
      responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION,
          defaultValue = "Blueprints/*",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getBlueprint(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @ApiParam @PathParam("blueprintName") String blueprintName) {
  return handleRequest(headers, body, ui, Request.Type.GET, createBlueprintResource(blueprintName));
  }

  /**
   * Handles: POST /blueprints/{blueprintID}
   * Create a specific blueprint.
   *
   * @param headers       http headers
   * @param ui            uri info
   * @param blueprintName blueprint id
   * @return information regarding the created blueprint
   */
  @POST
  @Path("{blueprintName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Creates a blueprint",
      nickname = "BlueprintService#createBlueprint"
  )
  @ApiImplicitParams({
      @ApiImplicitParam(dataType = BLUEPRINT_REQUEST_TYPE, paramType = PARAM_TYPE_BODY, allowMultiple = false)
  })
  @ApiResponses({
      @ApiResponse(code = HttpStatus.SC_CREATED, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_ACCEPTED, message = MSG_REQUEST_ACCEPTED),
      @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_CONFLICT, message = MSG_RESOURCE_ALREADY_EXISTS),
      @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
      @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response createBlueprint(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                  @ApiParam @PathParam("blueprintName") String blueprintName) {

    return handleRequest(headers, body, ui, Request.Type.POST, createBlueprintResource(blueprintName));
  }

  /**
   * Handles: DELETE /blueprints/{blueprintID}
   * Delete a specific blueprint.
   *
   * @param headers       http headers
   * @param ui            uri info
   * @param blueprintName blueprint name
   * @return information regarding the deleted blueprint
   */
  @DELETE
  @Path("{blueprintName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Deletes a blueprint",
      nickname = "BlueprintService#deleteBlueprint"
  )
  @ApiResponses({
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
      @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deleteBlueprint(@Context HttpHeaders headers, @Context UriInfo ui,
                                @ApiParam @PathParam("blueprintName") String blueprintName) {
    return handleRequest(headers, null, ui, Request.Type.DELETE, createBlueprintResource(blueprintName));
  }

  /**
   * Handles: DELETE /blueprints
   * Delete a set of blueprints that match a predicate.
   *
   * @param headers       http headers
   * @param ui            uri info
   * @return information regarding the deleted blueprint
   */
  @DELETE
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Deletes multiple blueprints that match the predicate. Omitting the predicate will delete all " +
      "blueprints.",
      nickname = "BlueprintService#deleteBlueprints"
  )
  @ApiResponses({
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
      @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deleteBlueprints(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.DELETE, createBlueprintResource(null));
  }

  /**
   * Create a blueprint resource instance.
   *
   * @param blueprintName blueprint name
   *
   * @return a blueprint resource instance
   */
  ResourceInstance createBlueprintResource(String blueprintName) {
    return createResource(Resource.Type.Blueprint,
        Collections.singletonMap(Resource.Type.Blueprint, blueprintName));
  }
}
