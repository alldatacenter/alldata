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
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.ClusterArtifactResponse;
import org.apache.ambari.server.controller.ClusterResponse.ClusterResponseWrapper;
import org.apache.ambari.server.controller.internal.ClusterResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.state.Clusters;
import org.apache.http.HttpStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Service responsible for cluster resource requests.
 */
@Path("/clusters")
@Api(value = "Clusters", description = "Endpoint for cluster-specific operations")
public class ClusterService extends BaseService {

  private static final String CLUSTER_REQUEST_TYPE = "org.apache.ambari.server.api.services.ClusterRequestSwagger";
  private static final String ARTIFACT_REQUEST_TYPE = "org.apache.ambari.server.controller.ClusterArtifactRequest";

  /**
   * The clusters utilities.
   */
  private final Clusters clusters;


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a ClusterService.
   */
  public ClusterService() {
    clusters = AmbariServer.getController().getClusters();
  }

  /**
   * Construct a ClusterService.
   *
   * @param clusters  the clusters utilities
   */
  protected ClusterService(Clusters clusters) {
    this.clusters = clusters;
  }


  // ----- ClusterService ----------------------------------------------------

  /**
   * Handles: GET /clusters/{clusterID}
   * Get a specific cluster.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param clusterName  cluster id
   *
   * @return cluster instance representation
   */
  @GET
  @Path("{clusterName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Returns information about a specific cluster", response = ClusterResponseWrapper.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY,
      defaultValue = ClusterResourceProvider.ALL_PROPERTIES),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response getCluster(String body, @Context HttpHeaders headers, @Context UriInfo ui,
    @ApiParam(required = true) @PathParam("clusterName") String clusterName
  ) {
    ResourceInstance resource = createClusterResource(clusterName);
    return handleRequest(headers, body, ui, Request.Type.GET, resource);
  }

  /**
   * Handles: GET  /clusters
   * Get all clusters.
   *
   * @param headers  http headers
   * @param ui       uri info
   *
   * @return cluster collection resource representation
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Returns all clusters",
    response = ClusterResponseWrapper.class, responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
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
  public Response getClusters(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    ResourceInstance resource = createClusterResource(null);
    return handleRequest(headers, body, ui, Request.Type.GET, resource);
  }

  /**
   * Handles: POST /clusters/{clusterID}
   * Create a specific cluster.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param clusterName  cluster id
   *
   * @return information regarding the created cluster
   */
   @POST
   @Path("{clusterName}")
   @Produces(MediaType.TEXT_PLAIN)
   @ApiOperation(value = "Creates a cluster")
   @ApiImplicitParams({
     @ApiImplicitParam(dataType = CLUSTER_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
   })
   @ApiResponses({
     @ApiResponse(code = HttpStatus.SC_CREATED, message = MSG_SUCCESSFUL_OPERATION),
     @ApiResponse(code = HttpStatus.SC_ACCEPTED, message = MSG_REQUEST_ACCEPTED),
     @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
     @ApiResponse(code = HttpStatus.SC_CONFLICT, message = MSG_RESOURCE_ALREADY_EXISTS),
     @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
     @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
     @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
   })
   public Response createCluster(String body, @Context HttpHeaders headers, @Context UriInfo ui,
     @ApiParam(required = true) @PathParam("clusterName") String clusterName
   ) {
     ResourceInstance resource = createClusterResource(clusterName);
     return handleRequest(headers, body, ui, Request.Type.POST, resource);
  }

  /**
   * Handles: PUT /clusters/{clusterID}
   * Update a specific cluster.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param clusterName  cluster id
   *
   * @return information regarding the updated cluster
   */
  @PUT
  @Path("{clusterName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Updates a cluster")
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = CLUSTER_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
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
  public Response updateCluster(String body, @Context HttpHeaders headers, @Context UriInfo ui,
    @ApiParam(required = true) @PathParam("clusterName") String clusterName
  ) {
    ResourceInstance resource = createClusterResource(clusterName);
    return handleRequest(headers, body, ui, Request.Type.PUT, resource);
  }

  /**
   * Handles: DELETE /clusters/{clusterID}
   * Delete a specific cluster.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param clusterName  cluster id
   *
   * @return information regarding the deleted cluster
   */
  @DELETE
  @Path("{clusterName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Deletes a cluster")
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deleteCluster(@Context HttpHeaders headers, @Context UriInfo ui,
    @ApiParam(required = true) @PathParam("clusterName") String clusterName
  ) {
    ResourceInstance resource = createClusterResource(clusterName);
    return handleRequest(headers, null, ui, Request.Type.DELETE, resource);
  }

  /**
   * Handles: GET /clusters/{clusterID}/artifacts
   * Get all artifacts associated with the cluster.
   *
   * @param body         request body
   * @param headers      http headers
   * @param ui           uri info
   * @param clusterName  cluster name
   *
   * @return artifact collection resource representation
   */
  @GET
  @Path("{clusterName}/artifacts")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Returns all artifacts associated with the cluster",
    response = ClusterArtifactResponse.class, responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
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
  public Response getClusterArtifacts(String body, @Context HttpHeaders headers, @Context UriInfo ui,
    @ApiParam(required = true) @PathParam("clusterName") String clusterName
  ) {
    ResourceInstance resource = createArtifactResource(clusterName, null);
    return handleRequest(headers, body, ui, Request.Type.GET, resource);
  }

  /**
   * Handles: GET /clusters/{clusterID}/artifacts/{artifactName}
   * Get an artifact resource instance.
   *
   * @param body          request body
   * @param headers       http headers
   * @param ui            uri info
   * @param clusterName   cluster name
   * @param artifactName  artifact name
   *
   * @return  artifact instance resource representation
   */
  @GET
  @Path("{clusterName}/artifacts/{artifactName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get the details of a cluster artifact",
    response = ClusterArtifactResponse.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getClusterArtifact(String body, @Context HttpHeaders headers, @Context UriInfo ui,
    @ApiParam(required = true) @PathParam("clusterName") String clusterName,
    @ApiParam(required = true) @PathParam("artifactName") String artifactName
  ) {
    ResourceInstance resource = createArtifactResource(clusterName, artifactName);
    return handleRequest(headers, body, ui, Request.Type.GET, resource);
  }

  /**
   * Handles: POST /clusters/{clusterID}/artifacts/{artifactName}
   * Create a cluster artifact.
   *
   * @param body          request body
   * @param headers       http headers
   * @param ui            uri info
   * @param clusterName   cluster name
   * @param artifactName  artifact name
   */
  @POST
  @Path("{clusterName}/artifacts/{artifactName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Creates a cluster artifact")
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
  public Response createClusterArtifact(String body, @Context HttpHeaders headers, @Context UriInfo ui,
    @ApiParam(required = true) @PathParam("clusterName") String clusterName,
    @ApiParam(required = true) @PathParam("artifactName") String artifactName
  ) {
    ResourceInstance resource = createArtifactResource(clusterName, artifactName);
    return handleRequest(headers, body, ui, Request.Type.POST, resource);
  }

  /**
   * Handles: PUT /clusters/{clusterID}/artifacts
   * Update all artifacts matching the provided predicate.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param clusterName  cluster name
   *
   * @return information regarding the updated artifacts
   */
  @PUT
  @Path("{clusterName}/artifacts")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Updates multiple artifacts")
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = ARTIFACT_REQUEST_TYPE, paramType = PARAM_TYPE_BODY, allowMultiple = true)
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
  public Response updateClusterArtifacts(String body, @Context HttpHeaders headers, @Context UriInfo ui,
    @ApiParam(required = true) @PathParam("clusterName") String clusterName
  ) {
    ResourceInstance resource = createArtifactResource(clusterName, null);
    return handleRequest(headers, body, ui, Request.Type.PUT, resource);
  }

  /**
   * Handles: PUT /clusters/{clusterID}/artifacts/{artifactName}
   * Update a specific artifact.
   *
   * @param headers       http headers
   * @param ui            uri info
   * @param clusterName   cluster name
   * @param artifactName  artifactName
   *
   * @return information regarding the updated artifact
   */
  @PUT
  @Path("{clusterName}/artifacts/{artifactName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Updates a single artifact")
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
  public Response updateClusterArtifact(String body, @Context HttpHeaders headers, @Context UriInfo ui,
    @ApiParam(required = true) @PathParam("clusterName") String clusterName,
    @ApiParam(required = true) @PathParam("artifactName") String artifactName
  ) {
    ResourceInstance resource = createArtifactResource(clusterName, artifactName);
    return handleRequest(headers, body, ui, Request.Type.PUT, resource);
  }

  /**
   * Handles: DELETE /clusters/{clusterID}/artifacts/{artifactName}
   * Delete a specific artifact.
   *
   * @param headers       http headers
   * @param ui            uri info
   * @param clusterName   cluster name
   * @param artifactName  artifactName
   *
   * @return information regarding the deleted artifact
   */
  @DELETE
  @Path("{clusterName}/artifacts/{artifactName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Deletes a single artifact")
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deleteClusterArtifact(String body, @Context HttpHeaders headers, @Context UriInfo ui,
    @ApiParam(required = true) @PathParam("clusterName") String clusterName,
    @ApiParam(required = true) @PathParam("artifactName") String artifactName
  ) {
    ResourceInstance resource = createArtifactResource(clusterName, artifactName);
    return handleRequest(headers, body, ui, Request.Type.DELETE, resource);
  }

  /**
   * Handles: DELETE /clusters/{clusterID}/artifacts
   * Delete all artifacts matching the provided predicate.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param clusterName  cluster name
   *
   * @return information regarding the deleted artifacts
   */
  @DELETE
  @Path("{clusterName}/artifacts")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Deletes all artifacts of a cluster that match the provided predicate")
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deleteClusterArtifacts(String body, @Context HttpHeaders headers, @Context UriInfo ui,
    @ApiParam(required = true) @PathParam("clusterName") String clusterName
  ) {
    ResourceInstance resource = createArtifactResource(clusterName, null);
    return handleRequest(headers, body, ui, Request.Type.DELETE, resource);
  }

  /**
   * Get the hosts sub-resource
   *
   * @param request      the request
   * @param clusterName  cluster id
   *
   * @return the hosts service
   */
  @Path("{clusterName}/hosts")
  public HostService getHostHandler(@Context javax.ws.rs.core.Request request, @PathParam("clusterName") String clusterName) {
    return new HostService(clusterName);
  }

  /**
   * Get the services sub-resource
   *
   * @param request      the request
   * @param clusterName  cluster id
   *
   * @return the services service
   */
  @Path("{clusterName}/services")
  public ServiceService getServiceHandler(@Context javax.ws.rs.core.Request request, @ApiParam @PathParam("clusterName") String clusterName) {
    return new ServiceService(clusterName);
  }

  /**
   * Gets the configurations sub-resource.
   *
   * @param request      the request
   * @param clusterName  the cluster name
   *
   * @return the configuration service
   */
  @Path("{clusterName}/configurations")
  public ConfigurationService getConfigurationHandler(@Context javax.ws.rs.core.Request request, @PathParam("clusterName") String clusterName) {
    return new ConfigurationService(clusterName);
  }

  /**
   * Gets the requests sub-resource.
   *
   * @param request      the request
   * @param clusterName  the cluster name
   *
   * @return the requests service
   */
  @Path("{clusterName}/requests")
  public RequestService getRequestHandler(@Context javax.ws.rs.core.Request request, @PathParam("clusterName") String clusterName) {
    return new RequestService(clusterName);
  }

  /**
   * Get the host component resource without specifying the parent host component.
   * Allows accessing host component resources across hosts.
   *
   * @param request      the request
   * @param clusterName  the cluster name
   *
   * @return  the host component service with no parent set
   */
  @Path("{clusterName}/host_components")
  public HostComponentService getHostComponentHandler(@Context javax.ws.rs.core.Request request, @PathParam("clusterName") String clusterName) {
    return new HostComponentService(clusterName, null);
  }

  /**
   * Get the host Kerberos identity resource without specifying the parent host component.
   * Allows accessing host Kerberos identity resources across hosts.
   *
   * @param request      the request
   * @param clusterName  the cluster name
   *
   * @return  the host component service with no parent set
   */
  @Path("{clusterName}/kerberos_identities")
  public HostKerberosIdentityService getHostKerberosIdentityHandler(@Context javax.ws.rs.core.Request request, @PathParam("clusterName") String clusterName) {
    return new HostKerberosIdentityService(clusterName, null);
  }

  /**
   * Get the component resource without specifying the parent service.
   * Allows accessing component resources across services.
   *
   * @param request      the request
   * @param clusterName  the cluster name
   *
   * @return  the host component service with no parent set
   */
  @Path("{clusterName}/components")
  public ComponentService getComponentHandler(@Context javax.ws.rs.core.Request request, @PathParam("clusterName") String clusterName) {
    return new ComponentService(clusterName, null);
  }

  /**
   * Gets the workflows sub-resource.
   *
   * @param request      the request
   * @param clusterName  the cluster name
   *
   * @return  the workflow service
   */
  @Path("{clusterName}/workflows")
  public WorkflowService getWorkflowHandler(@Context javax.ws.rs.core.Request request, @PathParam("clusterName") String clusterName) {
    return new WorkflowService(clusterName);
  }

  /**
   * Gets the config group service
   *
   * @param request      the request
   * @param clusterName  the cluster name
   *
   * @return  the config group service
   */
  @Path("{clusterName}/config_groups")
  public ConfigGroupService getConfigGroupService(@Context javax.ws.rs.core.Request request, @PathParam("clusterName") String clusterName) {
    return new ConfigGroupService(clusterName);
  }

  /**
   * Gets the request schedule service
   *
   * @param request      the request
   * @param clusterName  the cluster name
   *
   * @return  the request schedule service
   */
  @Path("{clusterName}/request_schedules")
  public RequestScheduleService getRequestScheduleService
                             (@Context javax.ws.rs.core.Request request, @PathParam ("clusterName") String clusterName) {
    return new RequestScheduleService(clusterName);
  }

  /**
   * Gets the alert definition service
   *
   * @param request      the request
   * @param clusterName  the cluster name
   *
   * @return  the alert definition service
   */
  @Path("{clusterName}/alert_definitions")
  public AlertDefinitionService getAlertDefinitionService(
      @Context javax.ws.rs.core.Request request, @PathParam("clusterName") String clusterName) {
    return new AlertDefinitionService(clusterName);
  }

  /**
   * Gets the alert group service.
   *
   * @param request
   *          the request.
   * @param clusterName
   *          the cluster name.
   * @return the alert group service.
   */
  @Path("{clusterName}/alert_groups")
  public AlertGroupService getAlertGroups(
      @Context javax.ws.rs.core.Request request,
      @PathParam("clusterName") String clusterName) {
    return new AlertGroupService(clusterName);
  }

  /**
   * Gets the privilege service
   *
   * @param request
   *          the request
   * @param clusterName
   *          the cluster name
   *
   * @return the privileges service
   */
  @Path("{clusterName}/privileges")
  public PrivilegeService getPrivilegeService(@Context javax.ws.rs.core.Request request, @PathParam ("clusterName") String clusterName) {
    return new ClusterPrivilegeService(clusterName);
  }

  /**
   * Gets the alert definition service
   *
   * @param request      the request
   * @param clusterName  the cluster name
   *
   * @return  the alert definition service
   */
  @Path("{clusterName}/alerts")
  public AlertService getAlertService(
      @Context javax.ws.rs.core.Request request, @PathParam("clusterName") String clusterName) {
    return new AlertService(clusterName, null, null);
  }

  /**
   * Gets the alert history service
   *
   * @param request
   *          the request
   * @param clusterName
   *          the cluster name
   *
   * @return the alert history service
   */
  @Path("{clusterName}/alert_history")
  public AlertHistoryService getAlertHistoryService(
      @Context javax.ws.rs.core.Request request,
      @PathParam("clusterName") String clusterName) {
    return new AlertHistoryService(clusterName, null, null);
  }

  /**
   * Gets the alert notice service
   *
   * @param request
   *          the request
   * @param clusterName
   *          the cluster name
   *
   * @return the alert notice service
   */
  @Path("{clusterName}/alert_notices")
  public AlertNoticeService getAlertNoticeService(
      @Context javax.ws.rs.core.Request request,
      @PathParam("clusterName") String clusterName) {
    return new AlertNoticeService(clusterName);
  }

  /**
   * Gets the cluster stack versions service.
   *
   * @param request
   *          the request
   * @param clusterName
   *          the cluster name
   *
   * @return the cluster stack versions service
   */
  @Path("{clusterName}/stack_versions")
  public ClusterStackVersionService getClusterStackVersionService(@Context javax.ws.rs.core.Request request,
      @PathParam("clusterName") String clusterName) {
    return new ClusterStackVersionService(clusterName);
  }

  /**
   * Gets the services for upgrades.
   *
   * @param request the request
   * @param clusterName the cluster name
   *
   * @return the upgrade services
   */
  @Path("{clusterName}/upgrades")
  public UpgradeService getUpgradeService(
      @Context javax.ws.rs.core.Request request,
      @PathParam("clusterName") String clusterName) {
    return new UpgradeService(clusterName);
  }

  /**
   * Gets a list of upgrade summaries.
   *
   * @param request the request
   * @param clusterName the cluster name
   *
   * @return the upgrade summary service
   */
  @Path("{clusterName}/upgrade_summary")
  public UpgradeSummaryService getUpgradeSummaryService(
      @Context javax.ws.rs.core.Request request,
      @PathParam("clusterName") String clusterName) {
    return new UpgradeSummaryService(clusterName);
  }
  
  /**
   * Gets the pre-upgrade checks service.
   *
   * @param request the request
   * @param clusterName the cluster name
   *
   * @return the pre-upgrade checks service.
   */
  @Path("{clusterName}/rolling_upgrades_check")
  public PreUpgradeCheckService getPreUpgradeCheckService(@Context javax.ws.rs.core.Request request, @PathParam("clusterName") String clusterName) {
    return new PreUpgradeCheckService(clusterName);
  }

  /**
   * Gets the widget layout service
   */
  @Path("{clusterName}/widget_layouts")
  public WidgetLayoutService getWidgetLayoutService(@Context javax.ws.rs.core.Request request,
                                                    @PathParam ("clusterName") String clusterName) {

    return new WidgetLayoutService(clusterName);
  }

  /**
   * Gets the widget service
   */
  @Path("{clusterName}/widgets")
  public WidgetService getWidgetService(@Context javax.ws.rs.core.Request request,
                                                    @PathParam ("clusterName") String clusterName) {

    return new WidgetService(clusterName);
  }

  /**
   * Gets the credentials service.
   *
   * @param request          the request.
   * @param clusterName         the cluster name.
   * @return the credentials service.
   */
  @Path("{clusterName}/credentials")
  public CredentialService getCredentials(
      @Context javax.ws.rs.core.Request request,
      @PathParam("clusterName") String clusterName) {
    return new CredentialService(clusterName);
  }

  /**
   * Handles: GET /clusters/{clusterID}/kerberos_descriptor
   * Gets the composite Kerberos descriptor associated with the cluster.
   *
   * @param request     the request.
   * @param clusterName the cluster name.
   * @return composite Kerberos descriptor resource representation
   */
  @Path("{clusterName}/kerberos_descriptors")
  public ClusterKerberosDescriptorService getCompositeKerberosDescriptor(
      @Context javax.ws.rs.core.Request request,
      @PathParam("clusterName") String clusterName) {
    return new ClusterKerberosDescriptorService(clusterName);
  }

  /**
   * Gets the Logging Service
   *
   * @param request the request
   * @param clusterName the cluster name
   *
   * @return a new instance of the LoggingService
   */
  @Path("{clusterName}/logging")
  public LoggingService getLogging(@Context javax.ws.rs.core.Request request,
                                   @PathParam("clusterName") String clusterName) {
    return AmbariServer.getController().getLoggingService(clusterName);
  }

  // ----- helper methods ----------------------------------------------------

  /**
   * Create a cluster resource instance.
   *
   * @param clusterName cluster name
   *
   * @return a cluster resource instance
   */
  ResourceInstance createClusterResource(String clusterName) {
    return createResource(Resource.Type.Cluster,
        Collections.singletonMap(Resource.Type.Cluster, clusterName));
  }

  /**
   * Create an artifact resource instance.
   *
   * @param clusterName  cluster name
   * @param artifactName artifact name
   *
   * @return an artifact resource instance
   */
  ResourceInstance createArtifactResource(String clusterName, String artifactName) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, clusterName);
    mapIds.put(Resource.Type.Artifact, artifactName);

    return createResource(Resource.Type.Artifact, mapIds);
  }
}
