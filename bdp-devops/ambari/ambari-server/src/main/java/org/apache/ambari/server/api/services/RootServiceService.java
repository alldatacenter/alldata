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
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.ApiModel;
import org.apache.ambari.server.controller.HostResponse;
import org.apache.ambari.server.controller.RootServiceComponentResponse;
import org.apache.ambari.server.controller.RootServiceHostComponentResponse;
import org.apache.ambari.server.controller.RootServiceResponse;
import org.apache.ambari.server.controller.internal.HostResourceProvider;
import org.apache.ambari.server.controller.internal.RootServiceComponentResourceProvider;
import org.apache.ambari.server.controller.internal.RootServiceHostComponentResourceProvider;
import org.apache.ambari.server.controller.internal.RootServiceResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.http.HttpStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/services")
@Api(value = "Services", description = "Endpoint for querying root-level services, ie. Ambari Server and Ambari Agents")
public class RootServiceService extends BaseService {

  private static final String KEY_COMPONENTS = "components";
  private static final String KEY_HOST_COMPONENTS = "hostComponents";

  private static final String DEFAULT_FIELDS_ROOT_SERVICES =
    RootServiceResourceProvider.SERVICE_NAME_PROPERTY_ID;

  private static final String DEFAULT_FIELDS_ROOT_SERVICE =
    RootServiceResourceProvider.SERVICE_NAME_PROPERTY_ID + FIELDS_SEPARATOR +
      KEY_COMPONENTS + PropertyHelper.EXTERNAL_PATH_SEP + RootServiceComponentResourceProvider.COMPONENT_NAME_PROPERTY_ID + FIELDS_SEPARATOR +
      KEY_COMPONENTS + PropertyHelper.EXTERNAL_PATH_SEP + RootServiceComponentResourceProvider.SERVICE_NAME_PROPERTY_ID;

  private static final String DEFAULT_FIELDS_ROOT_SERVICE_COMPONENTS =
    RootServiceComponentResourceProvider.COMPONENT_NAME_PROPERTY_ID + FIELDS_SEPARATOR +
    RootServiceComponentResourceProvider.SERVICE_NAME_PROPERTY_ID;

  private static final String DEFAULT_FIELDS_ROOT_SERVICE_COMPONENT =
    RootServiceComponentResourceProvider.ALL_PROPERTIES + FIELDS_SEPARATOR +
    KEY_HOST_COMPONENTS + PropertyHelper.EXTERNAL_PATH_SEP + RootServiceHostComponentResourceProvider.COMPONENT_NAME_PROPERTY_ID + FIELDS_SEPARATOR +
    KEY_HOST_COMPONENTS + PropertyHelper.EXTERNAL_PATH_SEP + RootServiceHostComponentResourceProvider.HOST_NAME_PROPERTY_ID + FIELDS_SEPARATOR +
    KEY_HOST_COMPONENTS + PropertyHelper.EXTERNAL_PATH_SEP + RootServiceHostComponentResourceProvider.SERVICE_NAME_PROPERTY_ID;

  private static final String DEFAULT_FIELDS_ROOT_SERVICE_HOST_COMPONENT =
    RootServiceHostComponentResourceProvider.COMPONENT_NAME_PROPERTY_ID + FIELDS_SEPARATOR +
    RootServiceHostComponentResourceProvider.HOST_NAME_PROPERTY_ID + FIELDS_SEPARATOR +
    RootServiceHostComponentResourceProvider.SERVICE_NAME_PROPERTY_ID;

  private static final String DEFAULT_FIELDS_HOSTS =
    HostResourceProvider.HOST_HOST_NAME_PROPERTY_ID;

  private static final String DEFAULT_FIELDS_HOST =
    HostResourceProvider.ALL_PROPERTIES;

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Returns the list of root-level services",
    response = RootServiceResponseWrapper.class, responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY,
      defaultValue = DEFAULT_FIELDS_ROOT_SERVICES),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response getRootServices(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    ResourceInstance resource = createServiceResource(null);
    return handleRequest(headers, body, ui, Request.Type.GET, resource);
  }
  
  @GET
  @Path("{serviceName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Returns information about the given root-level service, including a list of its components",
    response = RootServiceResponseWithComponentList.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY,
      defaultValue = DEFAULT_FIELDS_ROOT_SERVICE),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response getRootService(String body, @Context HttpHeaders headers, @Context UriInfo ui,
    @ApiParam(value = "service name", required = true) @PathParam("serviceName") String serviceName
  ) {
    ResourceInstance resource = createServiceResource(serviceName);
    return handleRequest(headers, body, ui, Request.Type.GET, resource);
  }

  @GET
  @Path("{serviceName}/hosts")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Returns the list of hosts for the given root-level service",
    response = HostResponse.HostResponseWrapper.class, responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY,
      defaultValue = DEFAULT_FIELDS_HOSTS),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response getRootHosts(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    ResourceInstance resource = createHostResource(null);
    return handleRequest(headers, body, ui, Request.Type.GET, resource);
  }
  
  @GET
  @Path("{serviceName}/hosts/{hostName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Returns information about the given host", response = HostResponse.HostResponseWrapper.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY,
      defaultValue = DEFAULT_FIELDS_HOST),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response getRootHost(String body, @Context HttpHeaders headers, @Context UriInfo ui,
    @ApiParam(value = "host name", required = true) @PathParam("hostName") String hostName
  ) {
    ResourceInstance resource = createHostResource(hostName);
    return handleRequest(headers, body, ui, Request.Type.GET, resource);
  }
  
  @GET
  @Path("{serviceName}/hosts/{hostName}/hostComponents")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Returns the list of components for the given root-level service on the given host",
    response = RootServiceHostComponentResponseWrapper.class, responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY,
      defaultValue = DEFAULT_FIELDS_ROOT_SERVICE_HOST_COMPONENT),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response getRootServiceHostComponents(String body, @Context HttpHeaders headers, @Context UriInfo ui,
    @ApiParam(value = "service name", required = true) @PathParam("serviceName") String serviceName,
    @ApiParam(value = "host name", required = true) @PathParam("hostName") String hostName
  ) {
    ResourceInstance resource = createHostComponentResource(serviceName, hostName, null);
    return handleRequest(headers, body, ui, Request.Type.GET, resource);
  }
  
  @GET
  @Path("{serviceName}/hosts/{hostName}/hostComponents/{hostComponent}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Returns information about the given component for the given root-level service on the given host",
    response = RootServiceHostComponentResponseWrapper.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY,
      defaultValue = DEFAULT_FIELDS_ROOT_SERVICE_HOST_COMPONENT),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response getRootServiceHostComponent(String body, @Context HttpHeaders headers, @Context UriInfo ui,
    @ApiParam(value = "service name", required = true) @PathParam("serviceName") String serviceName,
    @ApiParam(value = "host name", required = true) @PathParam("hostName") String hostName,
    @ApiParam(value = "component name", required = true) @PathParam("hostComponent") String hostComponent
  ) {
    ResourceInstance resource = createHostComponentResource(serviceName, hostName, hostComponent);
    return handleRequest(headers, body, ui, Request.Type.GET, resource);
  }
  
  @GET
  @Path("{serviceName}/components")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Returns the list of components for the given root-level service",
    response = RootServiceComponentResponseWrapper.class, responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY,
      defaultValue = DEFAULT_FIELDS_ROOT_SERVICE_COMPONENTS),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response getRootServiceComponents(String body, @Context HttpHeaders headers, @Context UriInfo ui,
    @ApiParam(value = "service name", required = true) @PathParam("serviceName") String serviceName
  ) {
    ResourceInstance resource = createServiceComponentResource(serviceName, null);
    return handleRequest(headers, body, ui, Request.Type.GET, resource);
  }
  
  @GET
  @Path("{serviceName}/components/{componentName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Returns information about the given component for the given root-level service",
    response = RootServiceComponentWithHostComponentList.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY,
      defaultValue = DEFAULT_FIELDS_ROOT_SERVICE_COMPONENT),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response getRootServiceComponent(String body, @Context HttpHeaders headers, @Context UriInfo ui,
    @ApiParam(value = "service name", required = true) @PathParam("serviceName") String serviceName,
    @ApiParam(value = "component name", required = true) @PathParam("componentName") String componentName
  ) {
    ResourceInstance resource = createServiceComponentResource(serviceName, componentName);
    return handleRequest(headers, body, ui, Request.Type.GET, resource);
  }

  @GET
  @Path("{serviceName}/components/{componentName}/hostComponents")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Returns the list of hosts for the given root-level service component",
    response = RootServiceHostComponentResponseWrapper.class, responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY,
      defaultValue = DEFAULT_FIELDS_ROOT_SERVICE_HOST_COMPONENT),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response getRootServiceComponentHosts(String body, @Context HttpHeaders headers, @Context UriInfo ui,
    @ApiParam(value = "service name", required = true) @PathParam("serviceName") String serviceName,
    @ApiParam(value = "component name", required = true) @PathParam("componentName") String componentName
  ) {
    ResourceInstance resource = createHostComponentResource(serviceName, null, componentName);
    return handleRequest(headers, body, ui, Request.Type.GET, resource);
  }

  @Path("{serviceName}/components/{componentName}/configurations")
  public RootServiceComponentConfigurationService getAmbariServerConfigurationHandler(@Context javax.ws.rs.core.Request request,
                                                                                      @PathParam("serviceName") String serviceName,
                                                                                      @PathParam("componentName") String componentName) {
    return new RootServiceComponentConfigurationService(serviceName, componentName);
  }

  protected ResourceInstance createServiceResource(String serviceName) {
    Map<Resource.Type, String> mapIds = Collections.singletonMap(Resource.Type.RootService, serviceName);
    return createResource(Resource.Type.RootService, mapIds);
  }
  
  protected ResourceInstance createServiceComponentResource(String serviceName, String componentName) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.RootService, serviceName);
    mapIds.put(Resource.Type.RootServiceComponent, componentName);
    return createResource(Resource.Type.RootServiceComponent, mapIds);
  }

  protected ResourceInstance createHostResource(String hostName) {
    return createResource(Resource.Type.Host, Collections.singletonMap(Resource.Type.Host, hostName));
  }

  protected ResourceInstance createHostComponentResource(String serviceName, String hostName, String componentName) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.RootService, serviceName);
    mapIds.put(Resource.Type.Host, hostName);
    mapIds.put(Resource.Type.RootServiceComponent, componentName);
    return createResource(Resource.Type.RootServiceHostComponent, mapIds);
  }

  private interface RootServiceResponseWrapper extends ApiModel {
    @ApiModelProperty(name = RootServiceResourceProvider.RESPONSE_KEY)
    @SuppressWarnings("unused")
    RootServiceResponse getRootServiceResponse();
  }

  private interface RootServiceResponseWithComponentList extends RootServiceResponseWrapper {
    @ApiModelProperty(name = KEY_COMPONENTS)
    @SuppressWarnings("unused")
    List<RootServiceComponentResponseWrapper> getComponents();
  }

  private interface RootServiceComponentResponseWrapper extends ApiModel {
    @ApiModelProperty(name = RootServiceComponentResourceProvider.RESPONSE_KEY)
    @SuppressWarnings("unused")
    RootServiceComponentResponse getRootServiceComponentResponse();
  }

  private interface RootServiceComponentWithHostComponentList extends RootServiceComponentResponseWrapper {
    @ApiModelProperty(name = KEY_HOST_COMPONENTS)
    @SuppressWarnings("unused")
    List<RootServiceHostComponentResponseWrapper> getHostComponents();
  }

  private interface RootServiceHostComponentResponseWrapper extends ApiModel {
    @ApiModelProperty(name = RootServiceHostComponentResourceProvider.RESPONSE_KEY)
    @SuppressWarnings("unused")
    RootServiceHostComponentResponse getRootServiceHostComponentResponse();
  }
}
