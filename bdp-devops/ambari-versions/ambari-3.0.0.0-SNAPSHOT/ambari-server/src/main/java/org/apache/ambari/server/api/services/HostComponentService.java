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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.annotations.SwaggerPreferredParent;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.HostComponentProcessResponse;
import org.apache.ambari.server.controller.HostComponentSwagger;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.http.HttpStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Service responsible for host_components resource requests.
 */
@Api(value = "Host Components", description = "Endpoint for host component specific operations")
@SwaggerPreferredParent(preferredParent = ClusterService.class)
public class HostComponentService extends BaseService {

  public static final String HOST_ROLE_REQUEST_TYPE = "org.apache.ambari.server.controller.ServiceComponentHostResponse";

  /**
   * Parent cluster id.
   */
  private String m_clusterName;

  /**
   * Parent host id.
   */
  private String m_hostName;

  /**
   * Constructor.
   *
   * @param clusterName cluster id
   * @param hostName    host id
   */
  public HostComponentService(String clusterName, String hostName) {
    m_clusterName = clusterName;
    m_hostName = hostName;
  }

  /**
   * Handles GET /clusters/{clusterID}/hosts/{hostID}/host_components/{hostComponentID}
   * Get a specific host_component.
   *
   * @param headers           http headers
   * @param ui                uri info
   * @param hostComponentName host_component id
   * @return host_component resource representation
   */
  @GET
  @Path("{hostComponentName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get single host component for a host", response = HostComponentSwagger.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_OR_HOST_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
  })
  public Response getHostComponent(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                   @PathParam("hostComponentName") String hostComponentName, @QueryParam("format") String format) {

    //todo: needs to be refactored when properly handling exceptions
    if (m_hostName == null) {
      // don't allow case where host is not in url but a host_component instance resource is requested
      String s = "Invalid request. Must provide host information when requesting a host_resource instance resource.";
      return Response.status(400).entity(s).build();
    }

    if (format != null && format.equals("client_config_tar")) {
      return createClientConfigResource(body, headers, ui, hostComponentName);
    }

    return handleRequest(headers, body, ui, Request.Type.GET,
        createHostComponentResource(m_clusterName, m_hostName, hostComponentName));
  }

  /**
   * Handles GET /clusters/{clusterID}/hosts/{hostID}/host_components/
   * Get all host components for a host.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return host_component collection resource representation
   */
  @GET
  @Path("") // This is needed if class level path is not present otherwise no Swagger docs will be generated for this method
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all host components for a host", response = HostComponentSwagger.class, responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_OR_HOST_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
  })
  public Response getHostComponents(String body, @Context HttpHeaders headers, @Context UriInfo ui, @QueryParam("format") String format) {
    if (format != null && format.equals("client_config_tar")) {
      return createClientConfigResource(body, headers, ui, null);
    }
    return handleRequest(headers, body, ui, Request.Type.GET,
        createHostComponentResource(m_clusterName, m_hostName, null));
  }

  /**
   * Handles POST /clusters/{clusterID}/hosts/{hostID}/host_components
   * Create host components by specifying an array of host components in the http body.
   * This is used to create multiple host components in a single request.
   *
   * @param body              http body
   * @param headers           http headers
   * @param ui                uri info
   *
   * @return status code only, 201 if successful
   */
  @POST @ApiIgnore // ignored as doesnt work as intended, not able to accept multiple HostRoles
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Create new host components")
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = HOST_ROLE_REQUEST_TYPE, paramType = PARAM_TYPE_BODY,  allowMultiple = true)
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
  public Response createHostComponents(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.POST,
        createHostComponentResource(m_clusterName, m_hostName, null));
  }

  /**
   * Handles POST /clusters/{clusterID}/hosts/{hostID}/host_components/{hostComponentID}
   * Create a specific host_component.
   *
   * @param body              http body
   * @param headers           http headers
   * @param ui                uri info
   * @param hostComponentName host_component id
   *
   * @return host_component resource representation
   */
  @POST
  @Path("{hostComponentName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Create new host component")
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = HOST_ROLE_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
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
  public Response createHostComponent(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                   @PathParam("hostComponentName") String hostComponentName) {

    return handleRequest(headers, body, ui, Request.Type.POST,
        createHostComponentResource(m_clusterName, m_hostName, hostComponentName));
  }

  /**
   * Handles PUT /clusters/{clusterID}/hosts/{hostID}/host_components/{hostComponentID}
   * Updates a specific host_component.
   *
   * @param body              http body
   * @param headers           http headers
   * @param ui                uri info
   * @param hostComponentName host_component id
   *
   * @return information regarding updated host_component
   */
  @PUT
  @Path("{hostComponentName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Update host component detail")
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = HOST_ROLE_REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
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
  public Response updateHostComponent(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                      @PathParam("hostComponentName") String hostComponentName) {

    return handleRequest(headers, body, ui, Request.Type.PUT,
        createHostComponentResource(m_clusterName, m_hostName, hostComponentName));
  }

  /**
   * Handles PUT /clusters/{clusterID}/hosts/{hostID}/host_components
   * Updates multiple host_component resources.
   *
   * @param body              http body
   * @param headers           http headers
   * @param ui                uri info
   *
   * @return information regarding updated host_component resources
   */
  @PUT @ApiIgnore // ignored as doesnt work as intended, not able to accept multiple HostRoles
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Update multiple host component details")
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = HOST_ROLE_REQUEST_TYPE, paramType = PARAM_TYPE_BODY,  allowMultiple = true)
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
  public Response updateHostComponents(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.PUT,
        createHostComponentResource(m_clusterName, m_hostName, null));
  }

  /**
   * Handles DELETE /clusters/{clusterID}/hosts/{hostID}/host_components/{hostComponentID}
   * Delete a specific host_component.
   *
   * @param headers           http headers
   * @param ui                uri info
   * @param hostComponentName host_component id
   *
   * @return host_component resource representation
   */
  @DELETE
  @Path("{hostComponentName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Delete host component")
  @ApiImplicitParams({
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_OR_HOST_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deleteHostComponent(@Context HttpHeaders headers, @Context UriInfo ui,
                                   @PathParam("hostComponentName") String hostComponentName) {

    return handleRequest(headers, null, ui, Request.Type.DELETE,
        createHostComponentResource(m_clusterName, m_hostName, hostComponentName));
  }

  /**
   * Handles DELETE /clusters/{clusterID}/hosts/{hostID}/host_components
   * Deletes multiple host_component resources.
   *
   * @param headers           http headers
   * @param ui                uri info
   *
   * @return host_component resource representation
   */
  @DELETE
  @Path("") // This is needed if class level path is not present otherwise no Swagger docs will be generated for this method
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Delete host components")
  @ApiImplicitParams({
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_OR_HOST_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deleteHostComponents(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.DELETE,
        createHostComponentResource(m_clusterName, m_hostName, null));
  }


  @GET
  @Path("{hostComponentName}/processes")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get processes of a specific host component", response = HostComponentProcessResponse.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_OR_HOST_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
  })
  public Response getProcesses(@Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("hostComponentName") String hostComponentName) {
    Map<Resource.Type,String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, m_clusterName);
    mapIds.put(Resource.Type.Host, m_hostName);
    mapIds.put(Resource.Type.HostComponent, hostComponentName);

    ResourceInstance ri = createResource(Resource.Type.HostComponentProcess, mapIds);

    return handleRequest(headers, null, ui, Request.Type.GET, ri);
  }

  /**
   * Create a host_component resource instance.
   *
   * @param clusterName       cluster name
   * @param hostName          host name
   * @param hostComponentName host_component name
   *
   * @return a host resource instance
   */
  ResourceInstance createHostComponentResource(String clusterName, String hostName, String hostComponentName) {
    Map<Resource.Type,String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, clusterName);
    mapIds.put(Resource.Type.Host, hostName);
    mapIds.put(Resource.Type.HostComponent, hostComponentName);

    return createResource(Resource.Type.HostComponent, mapIds);
  }

  private Response createClientConfigResource(String body, HttpHeaders headers, UriInfo ui,
                                              String hostComponentName) {
    Map<Resource.Type,String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, m_clusterName);
    mapIds.put(Resource.Type.Host, m_hostName);
    mapIds.put(Resource.Type.Component, hostComponentName);

    Response response = handleRequest(headers, body, ui, Request.Type.GET,
            createResource(Resource.Type.ClientConfig, mapIds));

    //If response has errors return response
    if (response.getStatus() != 200) {
      return response;
    }

    String filePrefixName;

    if (StringUtils.isEmpty(hostComponentName)) {
      filePrefixName = m_hostName + "(" + Resource.InternalType.Host.toString().toUpperCase()+")";
    } else {
      filePrefixName = hostComponentName;
    }

    Validate.notNull(filePrefixName, "compressed config file name should not be null");
    String fileName =  filePrefixName + "-configs" + Configuration.DEF_ARCHIVE_EXTENSION;

    Response.ResponseBuilder rb = Response.status(Response.Status.OK);
    Configuration configs = new Configuration();
    String tmpDir = configs.getProperty(Configuration.SERVER_TMP_DIR.getKey());
    File file = new File(tmpDir,fileName);
    InputStream resultInputStream = null;
    try {
      resultInputStream = new FileInputStream(file);
    } catch (IOException e) {
      e.printStackTrace();
    }

    String contentType = Configuration.DEF_ARCHIVE_CONTENT_TYPE;
    rb.header("Content-Disposition",  "attachment; filename=\"" + fileName + "\"");
    rb.entity(resultInputStream);
    return rb.type(contentType).build();

  }

}
