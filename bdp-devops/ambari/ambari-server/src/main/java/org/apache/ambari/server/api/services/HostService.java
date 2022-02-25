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

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.HostResponse;
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
 * Service responsible for hosts resource requests.
 */
@Path("/hosts")
@Api(value = "Hosts", description = "Endpoint for host-specific operations")
public class HostService extends BaseService {

  private static final String UNKNOWN_HOSTS = "Attempt to add hosts that have not been registered";
  private static final String HOST_ALREADY_EXISTS = "Attempt to create a host which already exists";
  private static final String HOST_REQUEST_TYPE = "org.apache.ambari.server.controller.HostRequest";

  /**
   * Parent cluster id.
   */
  private final String m_clusterName;

  /**
   * Constructor.
   */
  public HostService() {
    this(null);
  }

  /**
   * Constructor.
   *
   * @param clusterName cluster id
   */
  public HostService(String clusterName) {
    m_clusterName = clusterName;
  }

  /**
   * Handles GET /clusters/{clusterID}/hosts/{hostID} and /hosts/{hostID}
   * Get a specific host.
   *
   * @param headers  http headers
   * @param ui       uri info
   * @param hostName host name
   * @return host resource representation
   */
  @GET
  @Path("{hostName}")
  @Produces("text/plain")
  @ApiOperation(value = "Returns information about a single host", response = HostResponse.HostResponseWrapper.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_OR_HOST_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response getHost(String body, @Context HttpHeaders headers, @Context UriInfo ui,
    @ApiParam(value = "host name", required = true) @PathParam("hostName") String hostName
  ) {
    return handleRequest(headers, body, ui, Request.Type.GET,
        createHostResource(m_clusterName, hostName));
  }

  /**
   * Handles GET /clusters/{clusterID}/hosts and /hosts
   * Get all hosts for a cluster.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return host collection resource representation
   */
  @GET
  @Produces("text/plain")
  @ApiOperation(value = "Returns a collection of all hosts", response = HostResponse.HostResponseWrapper.class,
          responseContainer = "List")
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "Hosts/*", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION, defaultValue = "Hosts/host_name.asc", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
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
  })
  public Response getHosts(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET,
        createHostResource(m_clusterName, null));
  }

  /**
   * Handles POST /clusters/{clusterID}/hosts
   * Create hosts by specifying an array of hosts in the http body.
   * This is used to create multiple hosts in a single request.
   *
   * @param body     http body
   * @param headers  http headers
   * @param ui       uri info
   *
   * @return status code only, 201 if successful
   */
  @POST
  @Produces("text/plain")
  @ApiOperation(value = "Creates multiple hosts in a single request")
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = HOST_REQUEST_TYPE, paramType = PARAM_TYPE_BODY, allowMultiple = true)
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_CREATED, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_ACCEPTED, message = MSG_REQUEST_ACCEPTED),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = UNKNOWN_HOSTS),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_CONFLICT, message = HOST_ALREADY_EXISTS),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response createHosts(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.POST,
        createHostResource(m_clusterName, null));
  }

  /**
   * Handles POST /clusters/{clusterID}/hosts/{hostID}
   * Create a specific host.
   *
   * @param body     http body
   * @param headers  http headers
   * @param ui       uri info
   * @param hostName host name
   *
   * @return host resource representation
   */
  @POST
  @Path("{hostName}")
  @Produces("text/plain")
  @ApiOperation(value = "Creates a host")
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = HOST_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_CREATED, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_ACCEPTED, message = MSG_REQUEST_ACCEPTED),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_CONFLICT, message = HOST_ALREADY_EXISTS),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response createHost(String body, @Context HttpHeaders headers, @Context UriInfo ui,
    @ApiParam(value = "host name", required = true) @PathParam("hostName") String hostName
  ) {
    return handleRequest(headers, body, ui, Request.Type.POST,
        createHostResource(m_clusterName, hostName));
  }

  /**
   * Handles PUT /clusters/{clusterID}/hosts/{hostID}
   * Updates a specific host.
   *
   * @param body     http body
   * @param headers  http headers
   * @param ui       uri info
   * @param hostName host name
   *
   * @return information regarding updated host
   */
  @PUT
  @Path("{hostName}")
  @Produces("text/plain")
  @ApiOperation(value = "Updates a host")
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = HOST_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
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
  public Response updateHost(String body, @Context HttpHeaders headers, @Context UriInfo ui,
     @ApiParam(value = "host name", required = true) @PathParam("hostName") String hostName
  ) {
    return handleRequest(headers, body, ui, Request.Type.PUT,
        createHostResource(m_clusterName, hostName));
  }

  /**
   * Handles PUT /clusters/{clusterID}/hosts
   * Updates multiple hosts.
   *
   * @param body     http body
   * @param headers  http headers
   * @param ui       uri info
   *
   * @return information regarding updated host
   */
  @PUT
  @Produces("text/plain")
  @ApiOperation(value = "Updates multiple hosts in a single request")
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = HOST_REQUEST_TYPE, paramType = PARAM_TYPE_BODY, allowMultiple = true)
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
  public Response updateHosts(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.PUT,
        createHostResource(m_clusterName, null));
  }

  /**
   * Handles DELETE /clusters/{clusterID}/hosts/{hostID}
   * Deletes a specific host.
   *
   * @param headers  http headers
   * @param ui       uri info
   * @param hostName host name
   *
   * @return host resource representation
   */
  @DELETE
  @Path("{hostName}")
  @Produces("text/plain")
  @ApiOperation(value = "Deletes a host")
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_OR_HOST_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deleteHost(@Context HttpHeaders headers, @Context UriInfo ui,
    @ApiParam(value = "host name", required = true) @PathParam("hostName") String hostName
  ) {
    return handleRequest(headers, null, ui, Request.Type.DELETE,
        createHostResource(m_clusterName, hostName));
  }

  @DELETE
  @Produces("text/plain")
  @ApiOperation(value = "Deletes multiple hosts in a single request")
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = HOST_REQUEST_TYPE, paramType = PARAM_TYPE_BODY, allowMultiple = true)
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_OR_HOST_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deleteHosts(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.DELETE,
            createHostResource(m_clusterName, null));
  }

  /**
   * Get the host_components sub-resource.
   *
   * @param hostName host name
   * @return the host_components service
   */
  @Path("{hostName}/host_components")
  public HostComponentService getHostComponentHandler(@PathParam("hostName") String hostName) {
    return new HostComponentService(m_clusterName, hostName);
  }

  /**
   * Get the kerberos_identities sub-resource.
   *
   * @param hostName host name
   * @return the host_components service
   */
  @Path("{hostName}/kerberos_identities")
  public HostKerberosIdentityService getHostKerberosIdentityHandler(@PathParam("hostName") String hostName) {
    return new HostKerberosIdentityService(m_clusterName, hostName);
  }

  /**
   * Get the alerts sub-resource.
   *
   * @param hostName host name
   * @return the alerts service
   */
  @Path("{hostName}/alerts")
  public AlertService getAlertHandler(@PathParam("hostName") String hostName) {
    return new AlertService(m_clusterName, null, hostName);
  }

  /**
   * Gets the alert history service
   *
   * @param request the request
   * @param hostName the host name
   * @return the alert history service
   */
  @Path("{hostName}/alert_history")
  public AlertHistoryService getAlertHistoryService(
      @Context javax.ws.rs.core.Request request,
      @PathParam("hostName") String hostName) {

    return new AlertHistoryService(m_clusterName, null, hostName);
  }

  /**
   * Gets the host stack versions service.
   *
   * @param request the request
   * @param hostName the host name
   * @return the host stack versions service
   */
  @Path("{hostName}/stack_versions")
  public HostStackVersionService getHostStackVersionService(@Context javax.ws.rs.core.Request request,
      @PathParam("hostName") String hostName) {

    return new HostStackVersionService(hostName, m_clusterName);
  }

  /**
   * Create a service resource instance.
   *
   * @param clusterName  cluster
   * @param hostName     host name
   * @return a host resource instance
   */
  protected ResourceInstance createHostResource(String clusterName, String hostName) {
    Map<Resource.Type,String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Host, hostName);
    if (clusterName != null) {
      mapIds.put(Resource.Type.Cluster, clusterName);
    }

    return createResource(Resource.Type.Host, mapIds);
  }
}
