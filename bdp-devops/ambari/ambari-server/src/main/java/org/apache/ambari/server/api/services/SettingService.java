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
import org.apache.ambari.server.controller.SettingResponse.SettingResponseWrapper;
import org.apache.ambari.server.controller.internal.SettingResourceProvider;
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
 * Service responsible for setting resource requests.
 */
@Path("/settings")
@Api(value = "Settings", description = "Endpoint for settings-specific operations")
public class SettingService extends BaseService {

  private static final String DEFAULT_FIELDS_GET_SETTINGS = SettingResourceProvider.SETTING_NAME_PROPERTY_ID;
  private static final String DEFAULT_FIELDS_GET_SETTING = SettingResourceProvider.ALL_PROPERTIES;
  private static final String SETTING_REQUEST_TYPE = "org.apache.ambari.server.api.services.SettingRequestSwagger";

  /**
   * Construct a SettingService.
   */
  public SettingService() {

  }

  /**
   * Handles: GET  /settings
   * Get all clusters.
   *
   * @param headers  http headers
   * @param ui       uri info
   *
   * @return setting collection resource representation
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Returns all settings",
    response = SettingResponseWrapper.class, responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY,
      defaultValue =  DEFAULT_FIELDS_GET_SETTINGS),
    @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, allowableValues = QUERY_FROM_VALUES, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, allowableValues = QUERY_TO_VALUES, dataType = QUERY_TO_TYPE, paramType = PARAM_TYPE_QUERY),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response getSettings(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    ResourceInstance resource = createSettingResource(null);
    return handleRequest(headers, body, ui, Request.Type.GET, resource);
  }

  /**
   * Handles: GET /settings/{settingName}
   * Get a specific setting.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param settingName  settingName
   *
   * @return setting instance representation
   */
  @GET
  @Path("{settingName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Returns a specific setting",
    response = SettingResponseWrapper.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY,
      defaultValue = DEFAULT_FIELDS_GET_SETTING),
    @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, allowableValues = QUERY_FROM_VALUES, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, allowableValues = QUERY_TO_VALUES, dataType = QUERY_TO_TYPE, paramType = PARAM_TYPE_QUERY),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response getSetting(String body, @Context HttpHeaders headers, @Context UriInfo ui,
    @ApiParam(value = "setting name", required = true) @PathParam("settingName") String settingName
  ) {
    ResourceInstance resource = createSettingResource(settingName);
    return handleRequest(headers, body, ui, Request.Type.GET, resource);
  }

  /**
   * Handles: POST /settings
   * Create a specific setting.
   *
   * @param headers      http headers
   * @param ui           uri info
   *
   * @return information regarding the created setting
   */
   @POST
   @Produces(MediaType.TEXT_PLAIN)
   @ApiOperation(value = "Creates a setting")
   @ApiImplicitParams({
     @ApiImplicitParam(dataType = SETTING_REQUEST_TYPE, paramType = PARAM_TYPE_BODY, required = true)
   })
   @ApiResponses({
     @ApiResponse(code = HttpStatus.SC_CREATED, message = MSG_SUCCESSFUL_OPERATION),
     @ApiResponse(code = HttpStatus.SC_ACCEPTED, message = MSG_REQUEST_ACCEPTED),
     @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
     @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_NOT_FOUND),
     @ApiResponse(code = HttpStatus.SC_CONFLICT, message = MSG_RESOURCE_ALREADY_EXISTS),
     @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
     @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
     @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
   })
   public Response createSetting(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
     ResourceInstance resource = createSettingResource(null);
     return handleRequest(headers, body, ui, Request.Type.POST, resource);
  }

  /**
   * Handles: PUT /settings/{settingName}
   * Update a specific setting.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param settingName  setting name
   *
   * @return information regarding the updated setting
   */
  @PUT
  @Path("{settingName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Updates a setting")
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = SETTING_REQUEST_TYPE, paramType = PARAM_TYPE_BODY, required = true)
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response updateSetting(String body, @Context HttpHeaders headers, @Context UriInfo ui,
    @ApiParam(value = "setting name", required = true) @PathParam("settingName") String settingName
  ) {
    ResourceInstance resource = createSettingResource(settingName);
    return handleRequest(headers, body, ui, Request.Type.PUT, resource);
  }

  /**
   * Handles: DELETE /settings/{settingName}
   * Delete a specific setting.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param settingName  setting name
   *
   * @return information regarding the deleted setting
   */
  @DELETE
  @Path("{settingName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Deletes a setting")
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deleteSetting(@Context HttpHeaders headers, @Context UriInfo ui,
    @ApiParam(value = "setting name", required = true) @PathParam("settingName") String settingName
  ) {
    ResourceInstance resource = createSettingResource(settingName);
    return handleRequest(headers, null, ui, Request.Type.DELETE, resource);
  }

  // ----- helper methods ----------------------------------------------------

  /**
   * Create a setting resource instance.
   *
   * @param settingName setting name
   *
   * @return a setting resource instance
   */
  protected ResourceInstance createSettingResource(String settingName) {
    return createResource(Resource.Type.Setting,
        Collections.singletonMap(Resource.Type.Setting, settingName));
  }

}