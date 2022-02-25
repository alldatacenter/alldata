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
import org.apache.ambari.server.controller.RequestPostResponse;
import org.apache.ambari.server.controller.RequestResponse;
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
 * Service responsible for request resource requests.
 */
@Path("/requests/")
@Api(value = "Requests", description = "Endpoint for request specific operations")
public class RequestService extends BaseService {

  private static final String REQUEST_POST_REQUEST_TYPE = "org.apache.ambari.server.controller.RequestPostRequest";
  private static final String REQUEST_PUT_REQUEST_TYPE = "org.apache.ambari.server.controller.RequestPutRequest";

  /**
   * Parent cluster name.
   */
  private String m_clusterName;


  public RequestService() {
  }
  
  /**
   * Constructor.
   *
   * @param clusterName cluster id
   */
  public RequestService(String clusterName) {
    m_clusterName = clusterName;
  }

  /**
   * Handles URL: /clusters/{clusterID}/requests/{requestID} or
   * /requests/{requestId}
   * Get a specific request.
   *
   * @param headers    http headers
   * @param ui         uri info
   * @param requestId  request id
   *
   * @return request resource representation
   */
  @GET
  @Path("{requestId}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get the details of a request",
      nickname = "RequestService#getRequest",
      response = RequestResponse.class)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION,
          defaultValue = "Requests/*",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getRequest(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @ApiParam @PathParam("requestId") String requestId) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createRequestResource(m_clusterName, requestId));
  }

  /**
   * Handles URL: /clusters/{clusterId}/requests or /requests
   * Get all requests for a cluster.
   *
   * @param headers http headers
   * @param ui      uri info
   *
   * @return request collection resource representation
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all requests. A predicate can be given to filter results.",
      nickname = "RequestService#getRequests",
      response = RequestResponse.class,
      responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION,
          defaultValue = "Requests/id",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION,
          defaultValue = "Requests/id.asc",
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
  public Response getRequests(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET,
        createRequestResource(m_clusterName, null));
  }

  /**
   * Gets the stage sub-resource.
   */
  @Path("{requestId}/stages")
  // TODO: find a way to handle this with Swagger (refactor or custom annotation?)
  public StageService getStageHandler(@ApiParam @PathParam("requestId") String requestId) {
    return new StageService(m_clusterName, requestId);
  }

  /**
   * Gets the tasks sub-resource.
   */
  @Path("{requestId}/tasks")
  // TODO: find a way to handle this with Swagger (refactor or custom annotation?)
  public TaskService getTaskHandler(@ApiParam @PathParam("requestId") String requestId) {
    return new TaskService(m_clusterName, requestId, null);
  }

  /**
   * Handles: PUT /clusters/{clusterId}/requests/{requestId} or /requests/{requestId}
   * Change state of existing requests. Usually used to cancel running requests
   *
   * @param body        http body
   * @param headers     http headers
   * @param ui          uri info
   * @return information regarding the updated requests
   */
  @PUT
  @Path("{requestId}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Updates a request, usually used to cancel running requests.",
      notes = "Changes the state of an existing request. Usually used to cancel running requests.",
      nickname = "RequestService#updateRequests"
  )
  @ApiImplicitParams({
      @ApiImplicitParam(dataType = REQUEST_PUT_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
  })
  @ApiResponses({
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_ACCEPTED, message = MSG_REQUEST_ACCEPTED),
      @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
      @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response updateRequests(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                 @ApiParam @PathParam("requestId") String requestId) {
    return handleRequest(headers, body, ui, Request.Type.PUT, createRequestResource(m_clusterName, requestId));
  }

  /**
   * Handles: POST /clusters/{clusterId}/requests or /requests
   * Create multiple requests.
   *
   * @param body        http body
   * @param headers     http headers
   * @param ui          uri info
   * @return information regarding the created requests
   */
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Creates one or more Requests",
      nickname = "RequestService#createRequests"
  )
  @ApiImplicitParams({
      @ApiImplicitParam(dataType = REQUEST_POST_REQUEST_TYPE, paramType = PARAM_TYPE_BODY, allowMultiple = true)
  })
  @ApiResponses({
      @ApiResponse(code = HttpStatus.SC_CREATED, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_ACCEPTED, message = MSG_REQUEST_ACCEPTED,
          response = RequestPostResponse.class),
      @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_CONFLICT, message = MSG_RESOURCE_ALREADY_EXISTS),
      @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
      @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response createRequests(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.POST, createRequestResource(m_clusterName, null));
  }

  /**
   * Create a request resource instance.
   *
   * @param clusterName  cluster name
   * @param requestId    request id
   *
   * @return a request resource instance
   */
  ResourceInstance createRequestResource(String clusterName, String requestId) {
    Map<Resource.Type,String> mapIds = new HashMap<>();
    
    if (null != clusterName)
      mapIds.put(Resource.Type.Cluster, clusterName);
    mapIds.put(Resource.Type.Request, requestId);

    return createResource(Resource.Type.Request, mapIds);
  }
}
