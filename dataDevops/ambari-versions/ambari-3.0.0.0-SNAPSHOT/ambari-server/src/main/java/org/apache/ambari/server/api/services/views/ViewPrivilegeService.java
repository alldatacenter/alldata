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
 * See the License for the specific language governing privileges and
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
import org.apache.ambari.server.controller.ViewPrivilegeResponse;
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
 *  Service responsible for view privilege resource requests.
 */
@Path("/views/{viewName}/versions/{version}/instances/{instanceName}/privileges")
@Api(tags = "Views", description = "Endpoint for view specific operations")
public class ViewPrivilegeService extends BaseService {

  public static final String PRIVILEGE_INFO_REQUEST_TYPE = "org.apache.ambari.server.controller.ViewPrivilegeResponse.ViewPrivilegeResponseWrapper";

  /**
   * Handles: GET  /views/{viewName}/versions/{version}/instances/{instanceName}/privileges
   * Get all privileges.
   *
   * @param headers       http headers
   * @param ui            uri info
   * @param viewName      view id
   * @param version       version id
   * @param instanceName  instance id
   *
   * @return privilege collection representation
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all view instance privileges", response = ViewPrivilegeResponse.ViewPrivilegeResponseWrapper.class, responseContainer = "List")
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "PrivilegeInfo/*", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
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
  public Response getPrivileges(@Context HttpHeaders headers, @Context UriInfo ui,
                                @ApiParam(value = "view name") @PathParam("viewName") String viewName,
                                @ApiParam(value = "view version") @PathParam("version") String version,
                                @ApiParam(value = "instance name") @PathParam("instanceName") String instanceName) {
    return handleRequest(headers, null, ui, Request.Type.GET, createPrivilegeResource(viewName, version, instanceName,null));
  }

  /**
   * Handles: GET /views/{viewName}/versions/{version}/instances/{instanceName}/privileges/{privilegeID}
   * Get a specific privilege.
   *
   * @param headers        http headers
   * @param ui             uri info
   * @param viewName       view id
   * @param version        version id
   * @param instanceName   instance id
   * @param privilegeId    privilege id
   *
   * @return privilege instance representation
   */
  @GET
  @Path("/{privilegeId}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get single view instance privilege", response = ViewPrivilegeResponse.ViewPrivilegeResponseWrapper.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "PrivilegeInfo/*", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_OR_HOST_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
  })
  public Response getPrivilege(@Context HttpHeaders headers, @Context UriInfo ui,
                               @ApiParam(value = "view name") @PathParam("viewName") String viewName,
                               @ApiParam(value = "view version") @PathParam("version") String version,
                               @ApiParam(value = "instance name") @PathParam("instanceName") String instanceName,
                               @ApiParam(value = "privilege id", required = true) @PathParam("privilegeId") String privilegeId) {

    return handleRequest(headers, null, ui, Request.Type.GET, createPrivilegeResource(viewName, version, instanceName,privilegeId));
  }

  /**
   * Handles: POST /views/{viewName}/versions/{version}/instances/{instanceName}/privileges
   * Create a privilege.
   *
   * @param body          request body
   * @param headers       http headers
   * @param ui            uri info
   * @param viewName      view id
   * @param version       version id
   * @param instanceName  instance id
   *
   * @return information regarding the created privilege
   */
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Create view instance privilege")
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = PRIVILEGE_INFO_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_CREATED, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response createPrivilege(String body, @Context HttpHeaders headers, @Context UriInfo ui,
    @ApiParam(value = "view name") @PathParam("viewName") String viewName,
    @ApiParam(value = "view version") @PathParam("version") String version,
    @ApiParam(value = "instance name") @PathParam("instanceName") String instanceName) {
    return handleRequest(headers, body, ui, Request.Type.POST,
        createPrivilegeResource(viewName, version, instanceName,null));
  }

  /**
   * Handles: PUT /views/{viewName}/versions/{version}/instances/{instanceName}/privileges/{privilegeID}
   * Update a specific privilege.
   *
   * @param headers       http headers
   * @param ui            uri info
   * @param viewName      view id
   * @param version       version id
   * @param instanceName  instance id
   * @param privilegeId   privilege id
   *
   * @return information regarding the updated privilege
   */
  @PUT @ApiIgnore // until documented
  // Remove comments when the below API call is fixed
  /*@Path("{privilegeId}")
  @Produces("text/plain")
  @ApiOperation(value = "Update view instance privilege", notes = "Update privilege resource for view instance.")
  @ApiImplicitParams({
    @ApiImplicitParam(name = "body", value = "input parameters in json form", required = true, dataType = "org.apache.ambari.server.controller.ViewPrivilegeRequest", paramType = "body")
  })
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful operation"),
    @ApiResponse(code = 500, message = "Server Error")}
  ) */
  public Response updatePrivilege(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                  @ApiParam(value = "view name") @PathParam("viewName") String viewName,
                                  @ApiParam(value = "view version") @PathParam("version") String version,
                                  @ApiParam(value = "instance name") @PathParam("instanceName") String instanceName,
                                  @ApiParam(value = "privilege id") @PathParam("privilegeId") String privilegeId) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createPrivilegeResource(viewName, version, instanceName, privilegeId));
  }

  /**
   * Handles: PUT /views/{viewName}/versions/{version}/instances/{instanceName}/privileges
   * Update a set of privileges for the resource.
   *
   * @param body          request body
   * @param headers       http headers
   * @param ui            uri info
   * @param viewName      view id
   * @param version       version id
   * @param instanceName  instance id
   *
   * @return information regarding the updated privileges
   */
  @PUT
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Update view instance privilege")
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = PRIVILEGE_INFO_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
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
  public Response updatePrivileges(String body, @Context HttpHeaders headers, @Context UriInfo ui,
    @ApiParam(value = "view name") @PathParam("viewName") String viewName,
    @ApiParam(value = "view version") @PathParam("version") String version,
    @ApiParam(value = "instance name") @PathParam("instanceName") String instanceName) {
    return handleRequest(headers, body, ui, Request.Type.PUT,
            createPrivilegeResource(viewName, version, instanceName,null));
  }

  /**
   * Handles: DELETE /views/{viewName}/versions/{version}/instances/{instanceName}/privileges
   * Delete privileges.
   *
   * @param body          request body
   * @param headers       http headers
   * @param ui            uri info
   * @param viewName      view id
   * @param version       version id
   * @param instanceName  instance id
   *
   * @return information regarding the deleted privileges
   */
  @DELETE
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Delete view instance privileges")
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_OR_HOST_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deletePrivileges(String body, @Context HttpHeaders headers, @Context UriInfo ui,
    @ApiParam(value = "view name") @PathParam("viewName") String viewName,
    @ApiParam(value = "view version") @PathParam("viewVersion") String version,
    @ApiParam(value = "instance name") @PathParam("instanceName") String instanceName) {
    return handleRequest(headers, body, ui, Request.Type.DELETE, createPrivilegeResource(viewName, version, instanceName,null));
  }

  /**
   * Handles: DELETE /views/{viewName}/versions/{version}/instances/{instanceName}/privileges/{privilegeID}
   * Delete a specific privilege.
   *
   * @param headers       http headers
   * @param ui            uri info
   * @param viewName      view id
   * @param version       version id
   * @param instanceName  instance id
   * @param privilegeId   privilege id
   *
   * @return information regarding the deleted privilege
   */
  @DELETE
  @Path("{privilegeId}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Delete privileges")
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_OR_HOST_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deletePrivilege(@Context HttpHeaders headers, @Context UriInfo ui,
    @ApiParam(value = "view name") @PathParam("viewName") String viewName,
    @ApiParam(value = "view version") @PathParam("version") String version,
    @ApiParam(value = "instance name") @PathParam("instanceName") String instanceName,
    @ApiParam(value = "privilege id") @PathParam("privilegeId") String privilegeId) {
    return handleRequest(headers, null, ui, Request.Type.DELETE, createPrivilegeResource(viewName, version, instanceName, privilegeId));
  }

  protected ResourceInstance createPrivilegeResource(String viewName, String viewVersion, String instanceName, String privilegeId) {
    Map<Resource.Type,String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.View, viewName);
    mapIds.put(Resource.Type.ViewVersion, viewVersion);
    mapIds.put(Resource.Type.ViewInstance, instanceName);
    mapIds.put(Resource.Type.ViewPrivilege, privilegeId);

    return createResource(Resource.Type.ViewPrivilege, mapIds);
  }
}

