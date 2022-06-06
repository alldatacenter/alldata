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
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.ConfigurationResponse;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.http.HttpStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Service responsible for services resource requests.
 */
@Api(value = "Configurations", description = "Endpoint for configuration-specific operations")
public class ConfigurationService extends BaseService {

  public static final String CONFIGURATION_REQUEST_TYPE = "org.apache.ambari.server.controller.ConfigurationRequest";

  /**
   * Parent cluster name.
   */
  private String m_clusterName;

  /**
   * Constructor.
   *
   * @param clusterName cluster id
   */
  public ConfigurationService(String clusterName) {
    m_clusterName = clusterName;
  }

  @Path("service_config_versions")
  public ServiceConfigVersionService getServiceConfigVersionService() {
    return new ServiceConfigVersionService(m_clusterName);
  }

  /**
   * Handles URL: /clusters/{clusterId}/configurations
   * Get all services for a cluster.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return service collection resource representation
   */
  @GET
  @Path("") // This is needed if class level path is not present otherwise no Swagger docs will be generated for this method
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all configurations", response = ConfigurationResponse.class, responseContainer =
          RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
    @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, allowableValues = QUERY_FROM_VALUES, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
    @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, allowableValues = QUERY_TO_VALUES, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
  })
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_OR_HOST_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
  })
  public Response getConfigurations(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET, createConfigurationResource(m_clusterName));
  }

  /**
   * Handles URL: /clusters/{clusterId}/configurations
   * The body should contain:
   * <pre>
   * {
   *     "type":"type_string",
   *     "tag":"version_tag",
   *     "properties":
   *     {
   *         "key1":"value1",
   *         // ...
   *         "keyN":"valueN"
   *     }
   * }
   * </pre>
   *
   * To create multiple configurations is a request, provide an array of configuration properties.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return status code only, 201 if successful
   */
  @POST
  @Path("") // This is needed if class level path is not present otherwise no Swagger docs will be generated for this method
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Create new configurations")
  @ApiImplicitParams({
    @ApiImplicitParam(dataType = CONFIGURATION_REQUEST_TYPE, paramType = PARAM_TYPE_BODY,  allowMultiple = true)
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
  public Response createConfigurations(String body,@Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.POST, createConfigurationResource(m_clusterName));
  }

  /**
   * Create a service resource instance.
   *
   * @param clusterName cluster name
   *
   * @return a service resource instance
   */
  ResourceInstance createConfigurationResource(String clusterName) {
    Map<Resource.Type,String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, clusterName);
    mapIds.put(Resource.Type.Configuration, null);

    return createResource(Resource.Type.Configuration, mapIds);
  }
}
