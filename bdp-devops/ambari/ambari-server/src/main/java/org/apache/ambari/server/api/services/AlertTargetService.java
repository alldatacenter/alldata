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

import org.apache.ambari.server.api.resources.AlertTargetResourceDefinition;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.AlertTargetSwagger;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.state.alert.AlertTarget;
import org.apache.http.HttpStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;



/**
 * The {@link AlertTargetService} handles CRUD operation requests for alert
 * targets.
 */
@Path("/alert_targets/")
@Api(value = "Alerts", description = "Endpoint for alert specific operations")
public class AlertTargetService extends BaseService {

  public static final String ALERT_TARGET_REQUEST_TYPE = "org.apache.ambari.server.controller.AlertTargetSwagger";

  /**
   * Handles GET /alert_targets
   * Get all alert targets.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return alert target resource representation
   */
  @GET
  @Produces("text/plain")
  @ApiOperation(value = "Returns all alert targets", response = AlertTargetSwagger.class, responseContainer = "List")
  @ApiImplicitParams({
          @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "AlertTarget/*", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
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
  public Response getTargets(@Context HttpHeaders headers,
      @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET,
        createAlertTargetResource(null));
  }

  /**
   * Handles GET /alert_targets/{targetId}
   * Get a specific alert target.
   *
   * @param headers   http headers
   * @param ui        uri info
   * @param targetId  alert target id
   * @return alert target resource representation
   */
  @GET
  @Path("{targetId}")
  @Produces("text/plain")
  @ApiOperation(value = "Returns a single alert target", response = AlertTargetSwagger.class, nickname = "getTarget")
  @ApiImplicitParams({
          @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "AlertTarget/*", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
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
  public Response getTargets(@Context HttpHeaders headers,
      @Context UriInfo ui, @ApiParam(value = "alert target id") @PathParam("targetId") Long targetId) {
    return handleRequest(headers, null, ui, Request.Type.GET,
        createAlertTargetResource(targetId));
  }

  /**
   * Handles POST /alert_targets
   * Create a specific alert target.
   *
   * @param body     http body
   * @param headers  http headers
   * @param ui       uri info
   *
   * @return alert target resource representation
   */
  @POST
  @Produces("text/plain")
  @ApiOperation(value = "Creates an alert target")
  @ApiImplicitParams({
          @ApiImplicitParam(dataType = ALERT_TARGET_REQUEST_TYPE, paramType = PARAM_TYPE_BODY),
          @ApiImplicitParam(name = AlertTargetResourceDefinition.VALIDATE_CONFIG_DIRECTIVE, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
          @ApiImplicitParam(name = AlertTargetResourceDefinition.OVERWRITE_DIRECTIVE, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses({
          @ApiResponse(code = HttpStatus.SC_CREATED, message = MSG_SUCCESSFUL_OPERATION),
          @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
          @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
          @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response createTarget(String body, @Context HttpHeaders headers,
      @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.POST,
        createAlertTargetResource(null));
  }

  /**
   * Handles PUT /alert_targets/{targetId}
   * Updates a specific alert target.
   *
   * @param body     http body
   * @param headers  http headers
   * @param ui       uri info
   * @param targetId alert target id
   *
   * @return information regarding updated alert target
   */
  @PUT
  @Path("{targetId}")
  @Produces("text/plain")
  @ApiOperation(value = "Updates an alert target")
  @ApiImplicitParams({
          @ApiImplicitParam(dataType = ALERT_TARGET_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
  })
  @ApiResponses({
          @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
          @ApiResponse(code = HttpStatus.SC_ACCEPTED, message = MSG_REQUEST_ACCEPTED),
          @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
          @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
          @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
          @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response updateTarget(String body, @Context HttpHeaders headers,
      @Context UriInfo ui, @PathParam("targetId") Long targetId) {
    return handleRequest(headers, body, ui, Request.Type.PUT,
        createAlertTargetResource(targetId));
  }

  /**
   * Handles DELETE /alert_target/{targetId}
   * Deletes a specific alert target.
   *
   * @param headers  http headers
   * @param ui       uri info
   * @param targetId alert target id
   *
   * @return alert target resource representation
   */
  @DELETE
  @Path("{targetId}")
  @Produces("text/plain")
  @ApiOperation(value = "Deletes an alert target")
  @ApiResponses({
          @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
          @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
          @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
          @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deleteTarget(String body, @Context HttpHeaders headers,
      @Context UriInfo ui, @PathParam("targetId") Long targetId) {
    return handleRequest(headers, body, ui, Request.Type.DELETE,
        createAlertTargetResource(targetId));
  }

  /**
   * Create a request capturing the target ID and resource type for an
   * {@link AlertTarget}.
   *
   * @param targetId
   *          the unique ID of the target to create the query for (not
   *          {@code null}).
   * @return the instance of the query.
   */
  private ResourceInstance createAlertTargetResource(Long targetId) {
    Map<Resource.Type, String> mapIds = new HashMap<>();

    mapIds.put(Resource.Type.AlertTarget,
        null == targetId ? null : targetId.toString());

    return createResource(Resource.Type.AlertTarget, mapIds);
  }
}
