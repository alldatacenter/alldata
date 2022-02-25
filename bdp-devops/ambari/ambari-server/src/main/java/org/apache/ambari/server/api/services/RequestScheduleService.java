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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.RequestScheduleResponseSwagger;
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
 * Service responsible for management of a batch of requests with attached
 * schedule
 */
@Api(value = "RequestSchedules", description = "Endpoint for request schedule specific operations")
public class RequestScheduleService extends BaseService {

  public static final String REQUEST_SCHEDULE_REQUEST_TYPE = "org.apache.ambari.server.controller" +
          ".RequestScheduleRequestSwagger";

  /**
   * Parent cluster name.
   */
  private String m_clusterName;

  /**
   * Constructor
   * @param m_clusterName
   */
  public RequestScheduleService(String m_clusterName) {
    this.m_clusterName = m_clusterName;
  }

  /**
   * Handles URL: /clusters/{clusterId}/request_schedules
   * Get all the scheduled requests for a cluster.
   *
   * @param headers
   * @param ui
   * @return
   */
  @GET
  @Path("") // This is needed if class level path is not present otherwise no Swagger docs will be generated for this method
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all request schedules", response = RequestScheduleResponseSwagger.class, responseContainer =
          RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "RequestSchedule/*",
            dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_OR_HOST_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
  })
  public Response getRequestSchedules(String body,
                                      @Context HttpHeaders headers,
                                      @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET,
      createRequestSchedule(m_clusterName, null));
  }

  /**
   * Handles URL: /clusters/{clusterId}/request_schedules/{requestScheduleId}
   * Get details on a specific request schedule
   *
   * @return
   */
  @GET
  @Path("{requestScheduleId}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get request schedule", response = RequestScheduleResponseSwagger.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "RequestSchedule/*", dataType =
            DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_OR_HOST_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
  })
  public Response getRequestSchedule(String body,
                                     @Context HttpHeaders headers,
                                     @Context UriInfo ui,
                                     @PathParam("requestScheduleId") String requestScheduleId) {
    return handleRequest(headers, body, ui, Request.Type.GET,
      createRequestSchedule(m_clusterName, requestScheduleId));
  }

  /**
   * Handles POST /clusters/{clusterId}/request_schedules
   * Create a new request schedule
   *
   * @param body
   * @param headers
   * @param ui
   * @return
   */
  @POST
  @Path("") // This is needed if class level path is not present otherwise no Swagger docs will be generated for this method
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Create new request schedule")
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = REQUEST_SCHEDULE_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
  })
  @ApiResponses(value = {
    @ApiResponse(code = HttpStatus.SC_CREATED, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_ACCEPTED, message = MSG_REQUEST_ACCEPTED),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response createRequestSchedule(String body,
                                        @Context HttpHeaders headers,
                                        @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.POST,
      createRequestSchedule(m_clusterName, null));
  }


  /**
   * Handles URL: /clusters/{clusterId}/request_schedules/{requestScheduleId}
   * Get details on a specific request schedule
   *
   * @return
   */

  @PUT
  @Path("{requestScheduleId}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Updates a scheduled request, usually used to pause running scheduled requests or to resume them.",
    notes = "Changes the state of an existing request. Usually used to pause running scheduled requests or to resume them.",
    nickname = "RequestSchedules#updateRequestSchedule"
  )
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = REQUEST_SCHEDULE_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
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
  public Response updateRequestSchedule(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                 @ApiParam @PathParam("requestScheduleId") String requestScheduleId) {
    return handleRequest(headers, body, ui, Request.Type.PUT, createRequestSchedule(m_clusterName, requestScheduleId));
  }

  /**
   * Handles DELETE /clusters/{clusterId}/request_schedules/{requestScheduleId}
   * Delete a request schedule
   *
   * @param headers
   * @param ui
   * @param requestScheduleId
   * @return
   */
  @DELETE
  @Path("{requestScheduleId}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Delete a request schedule", notes = "Changes status from COMPLETED to DISABLED")
  @ApiResponses({
          @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
          @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_OR_HOST_NOT_FOUND),
          @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
          @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
          @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deleteRequestSchedule(@Context HttpHeaders headers,
                                        @Context UriInfo ui,
                                        @PathParam("requestScheduleId") String requestScheduleId) {
    return handleRequest(headers, null, ui, Request.Type.DELETE,
      createRequestSchedule(m_clusterName, requestScheduleId));
  }

  /**
   * Create a request schedule resource instance
   * @param clusterName
   * @param requestScheduleId
   * @return
   */
  private ResourceInstance createRequestSchedule(String clusterName,
                                                 String requestScheduleId) {
    Map<Resource.Type,String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, clusterName);
    mapIds.put(Resource.Type.RequestSchedule, requestScheduleId);

    return createResource(Resource.Type.RequestSchedule, mapIds);
  }

}
