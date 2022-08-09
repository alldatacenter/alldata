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
import org.apache.ambari.server.controller.ClusterServiceArtifactResponse;
import org.apache.ambari.server.controller.ServiceResponse;
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
 * Service responsible for services resource requests.
 */
@Api(value = "Cluster Services", description = "Endpoint for service specific operations")
public class ServiceService extends BaseService {
  private static final String SERVICE_REQUEST_TYPE = "org.apache.ambari.server.controller.ServiceRequestSwagger";
  private static final String ARTIFACT_REQUEST_TYPE = "org.apache.ambari.server.controller.ClusterServiceArtifactRequest";

  /**
   * Parent cluster name.
   */
  private String m_clusterName;

  /**
   * Constructor.
   *
   * @param clusterName cluster id
   */
  public ServiceService(String clusterName) {
    m_clusterName = clusterName;
  }

  /**
   * Handles URL: /clusters/{clusterID}/services/{serviceID}
   * Get a specific service.
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param serviceName service id
   * @return service resource representation
   */
  @GET
  @Path("{serviceName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get the details of a service",
      nickname = "ServiceService#getService",
      notes = "Returns the details of a service.",
      response = ServiceResponse.ServiceResponseSwagger.class,
      responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "ServiceInfo/*",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getService(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @ApiParam @PathParam("serviceName") String serviceName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createServiceResource(m_clusterName, serviceName));
  }

  /**
   * Handles URL: /clusters/{clusterId}/services
   * Get all services for a cluster.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return service collection resource representation
   */
  @GET
  @Path("") // This is needed if class level path is not present otherwise no Swagger docs will be generated for this method
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all services",
      nickname = "ServiceService#getServices",
      notes = "Returns all services.",
      response = ServiceResponse.ServiceResponseSwagger.class,
      responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION,
          defaultValue = "ServiceInfo/service_name,ServiceInfo/cluster_name",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION,
          defaultValue = "ServiceInfo/service_name.asc,ServiceInfo/cluster_name.asc",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getServices(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET,
        createServiceResource(m_clusterName, null));
  }

  /**
   * Handles: POST /clusters/{clusterId}/services/{serviceId}
   * Create a specific service.
   *
   * @param body        http body
   * @param headers     http headers
   * @param ui          uri info
   * @param serviceName service id
   * @return information regarding the created service
   */
  @POST
  @Path("{serviceName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Creates a service",
      nickname = "ServiceService#createService"
  )
  @ApiImplicitParams({
      @ApiImplicitParam(dataType = SERVICE_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
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
  public Response createService(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @ApiParam @PathParam("serviceName") String serviceName) {
    return handleRequest(headers, body, ui, Request.Type.POST,
        createServiceResource(m_clusterName, serviceName));
  }

  /**
   * Handles: POST /clusters/{clusterId}/services
   * Create services, possibly more than one.
   *
   * @param body        http body
   * @param headers     http headers
   * @param ui          uri info
   * @return information regarding the created services
   */
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Creates services",
      nickname = "ServiceService#createServices"
  )
  @ApiImplicitParams({
      @ApiImplicitParam(dataType = SERVICE_REQUEST_TYPE, paramType = PARAM_TYPE_BODY, allowMultiple = true)
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
  public Response createServices(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.POST,
        createServiceResource(m_clusterName, null));
  }

  /**
   * Handles: PUT /clusters/{clusterId}/services/{serviceId}
   * Update a specific service.
   *
   * @param body        http body
   * @param headers     http headers
   * @param ui          uri info
   * @param serviceName service id
   * @return information regarding the updated service
   */
  @PUT
  @Path("{serviceName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Updates a service",
      nickname = "ServiceService#updateService"
  )
  @ApiImplicitParams({
      @ApiImplicitParam(dataType = SERVICE_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
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
  public Response updateService(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @ApiParam @PathParam("serviceName") String serviceName) {
    return handleRequest(headers, body, ui, Request.Type.PUT, createServiceResource(m_clusterName, serviceName));
  }

  /**
   * Handles: PUT /clusters/{clusterId}/services
   * Update multiple services.
   *
   * @param body        http body
   * @param headers     http headers
   * @param ui          uri info
   * @return information regarding the updated service
   */
  @PUT
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Updates multiple services",
      nickname = "ServiceService#updateServices"
  )
  @ApiImplicitParams({
      @ApiImplicitParam(dataType = SERVICE_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
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
  public Response updateServices(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createServiceResource(m_clusterName, null));
  }

  /**
   * Handles: DELETE /clusters/{clusterId}/services/{serviceId}
   * Delete a specific service.
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param serviceName service id
   * @return information regarding the deleted service
   */
  @DELETE
  @Path("{serviceName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Deletes a service",
      nickname = "ServiceService#deleteService"
  )
  @ApiResponses({
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
      @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deleteService(@Context HttpHeaders headers, @Context UriInfo ui,
                                @ApiParam(required = true) @PathParam("serviceName") String serviceName) {
    return handleRequest(headers, null, ui, Request.Type.DELETE, createServiceResource(m_clusterName, serviceName));
  }

  /**
   * Get the components sub-resource.
   *
   * @param serviceName service id
   * @return the components service
   */
  @Path("{serviceName}/components")
  // TODO: find a way to handle this with Swagger (refactor or custom annotation?)
  public ComponentService getComponentHandler(@PathParam("serviceName") String serviceName) {

    return new ComponentService(m_clusterName, serviceName);
  }

  /**
   * Gets the alerts sub-resource.
   */
  @Path("{serviceName}/alerts")
  // TODO: find a way to handle this with Swagger (refactor or custom annotation?)
  public AlertService getAlertHandler(
      @PathParam("serviceName") String serviceName) {
    return new AlertService(m_clusterName, serviceName, null);
  }

  /**
   * Handles: POST /clusters/{clusterId}/services/{serviceId}/artifacts/{artifactName}
   * Create a service artifact instance.
   *
   * @param body          http body
   * @param headers       http headers
   * @param ui            uri info
   * @param serviceName   service name
   * @param artifactName  artifact name
   *
   * @return information regarding the created artifact
   */
  @POST
  @Path("{serviceName}/artifacts/{artifactName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Creates a service artifact",
      nickname = "ServiceService#createArtifact"
  )
  @ApiImplicitParams({
      @ApiImplicitParam(dataType = ARTIFACT_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
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
  public Response createArtifact(String body,
                                 @Context HttpHeaders headers,
                                 @Context UriInfo ui,
                                 @ApiParam @PathParam("serviceName") String serviceName,
                                 @ApiParam @PathParam("artifactName") String artifactName) {

    return handleRequest(headers, body, ui, Request.Type.POST,
        createArtifactResource(m_clusterName, serviceName, artifactName));
  }

  /**
   * Handles: GET /clusters/{clusterId}/services/{serviceId}/artifacts
   * Get all service artifacts.
   *
   * @param body          http body
   * @param headers       http headers
   * @param ui            uri info
   * @param serviceName   service name
   *
   * @return artifact collection resource representation
   */
  @GET
  @Path("{serviceName}/artifacts")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all service artifacts",
      nickname = "ServiceService#getArtifacts",
      response = ClusterServiceArtifactResponse.class,
      responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION,
          defaultValue = "Artifacts/artifact_name",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION,
          defaultValue = "Artifacts/artifact_name",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getArtifacts(String body,
                              @Context HttpHeaders headers,
                              @Context UriInfo ui,
                              @PathParam("serviceName") String serviceName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createArtifactResource(m_clusterName, serviceName, null));
  }

  /**
   * Handles: GET /clusters/{clusterId}/services/{serviceId}/artifacts/{artifactName}
   * Gat a service artifact instance.
   *
   * @param body          http body
   * @param headers       http headers
   * @param ui            uri info
   * @param serviceName   service name
   * @param artifactName  artifact name
   *
   * @return artifact instance resource representation
   */
  @GET
  @Path("{serviceName}/artifacts/{artifactName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get the details of a service artifact",
      nickname = "ServiceService#getArtifact",
      response = ClusterServiceArtifactResponse.class,
      responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION,
          defaultValue = "Artifacts/artifact_name",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION,
          defaultValue = "Artifacts/artifact_name",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getArtifact(String body,
                                 @Context HttpHeaders headers,
                                 @Context UriInfo ui,
                                 @ApiParam @PathParam("serviceName") String serviceName,
                                 @ApiParam @PathParam("artifactName") String artifactName) {
    return handleRequest(headers, body, ui, Request.Type.GET,
        createArtifactResource(m_clusterName, serviceName, artifactName));
  }

  /**
   * Handles: PUT /clusters/{clusterID}/services/{serviceName}/artifacts
   * Update all artifacts matching the provided predicate.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param serviceName  service name
   *
   * @return information regarding the updated artifacts
   */
  @PUT
  @Path("{serviceName}/artifacts")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Updates multiple artifacts",
      nickname = "ServiceService#updateArtifacts"
  )
  @ApiImplicitParams({
      @ApiImplicitParam(dataType = ARTIFACT_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
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
  public Response updateArtifacts(String body,
                                  @Context HttpHeaders headers,
                                  @Context UriInfo ui,
                                  @ApiParam @PathParam("serviceName") String serviceName) {

    return handleRequest(headers, body, ui, Request.Type.PUT,
        createArtifactResource(m_clusterName, serviceName, null));
  }

  /**
   * Handles: PUT /clusters/{clusterID}/services/{serviceName}/artifacts/{artifactName}
   * Update a specific artifact.
   *
   * @param headers       http headers
   * @param ui            uri info
   * @param serviceName   service name
   * @param artifactName  artifact name
   *
   * @return information regarding the updated artifact
   */
  @PUT
  @Path("{serviceName}/artifacts/{artifactName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Updates a single artifact",
      nickname = "ServiceService#updateArtifact"
  )
  @ApiImplicitParams({
      @ApiImplicitParam(dataType = ARTIFACT_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
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
  public Response updateArtifact(String body,
                                 @Context HttpHeaders headers,
                                 @Context UriInfo ui,
                                 @ApiParam(required = true) @PathParam("serviceName") String serviceName,
                                 @ApiParam(required = true) @PathParam("artifactName") String artifactName) {

    return handleRequest(headers, body, ui, Request.Type.PUT,
        createArtifactResource(m_clusterName, serviceName, artifactName));
  }

  /**
   * Handles: DELETE /clusters/{clusterID}/services/{serviceName}/artifacts
   * Delete all artifacts matching the provided predicate.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param serviceName  service name
   *
   * @return information regarding the deleted artifacts
   */
  @DELETE
  @Path("{serviceName}/artifacts")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Deletes all artifacts of a service that match the provided predicate",
    nickname = "ServiceService#deleteArtifacts"
  )
  @ApiResponses({
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
      @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deleteArtifacts(String body,
                                  @Context HttpHeaders headers,
                                  @Context UriInfo ui,
                                  @ApiParam(required = true) @PathParam("serviceName") String serviceName) {

    return handleRequest(headers, body, ui, Request.Type.DELETE,
        createArtifactResource(m_clusterName, serviceName, null));
  }

  /**
   * Handles: DELETE /clusters/{clusterID}/services/{serviceName}/artifacts/{artifactName}
   * Delete a specific artifact.
   *
   * @param headers       http headers
   * @param ui            uri info
   * @param serviceName   service name
   * @param artifactName  artifact name
   *
   * @return information regarding the deleted artifact
   */
  @DELETE
  @Path("{serviceName}/artifacts/{artifactName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Deletes a single service artifact",
      nickname = "ServiceService#deleteArtifact"
  )
  @ApiResponses({
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
      @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deleteArtifact(String body,
                                 @Context HttpHeaders headers,
                                 @Context UriInfo ui,
                                 @ApiParam(required = true) @PathParam("serviceName") String serviceName,
                                 @ApiParam(required = true) @PathParam("artifactName") String artifactName) {

    return handleRequest(headers, body, ui, Request.Type.DELETE,
        createArtifactResource(m_clusterName, serviceName, artifactName));
  }

  /**
   * Gets the alert history service
   *
   * @param request
   *          the request
   * @param serviceName
   *          the service name
   *
   * @return the alert history service
   */
  @Path("{serviceName}/alert_history")
  // TODO: find a way to handle this with Swagger (refactor or custom annotation?)
  public AlertHistoryService getAlertHistoryService(
      @Context javax.ws.rs.core.Request request,
      @PathParam("serviceName") String serviceName) {

    return new AlertHistoryService(m_clusterName, serviceName, null);
  }

  /**
   * Create a service resource instance.
   *
   * @param clusterName  cluster name
   * @param serviceName  service name
   *
   * @return a service resource instance
   */
  ResourceInstance createServiceResource(String clusterName, String serviceName) {
    Map<Resource.Type,String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, clusterName);
    mapIds.put(Resource.Type.Service, serviceName);

    return createResource(Resource.Type.Service, mapIds);
  }

  /**
   * Create an artifact resource instance.
   *
   * @param clusterName   cluster name
   * @param serviceName   service name
   * @param artifactName  artifact name
   *
   * @return an artifact resource instance
   */
  ResourceInstance createArtifactResource(String clusterName, String serviceName, String artifactName) {
    Map<Resource.Type,String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, clusterName);
    mapIds.put(Resource.Type.Service, serviceName);
    mapIds.put(Resource.Type.Artifact, artifactName);

    return createResource(Resource.Type.Artifact, mapIds);
  }
}
