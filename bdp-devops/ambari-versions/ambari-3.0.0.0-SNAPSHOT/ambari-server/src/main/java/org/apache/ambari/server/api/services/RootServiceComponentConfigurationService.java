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

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import java.util.Set;

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

import org.apache.ambari.server.api.resources.BaseResourceDefinition;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.http.HttpStatus;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Rest endpoint for managing ambari root service component configurations. Supports CRUD operations.
 * Ambari configurations are resources that relate to the ambari server instance even before a cluster is provisioned.
 * <p>
 * Ambari configuration resources may be shared with components and services in the cluster
 * (by recommending them as default values)
 * <p>
 * Eg. LDAP configuration is stored as Configuration.
 * The request payload has the form:
 * <p>
 * <pre>
 *      {
 *        "Configuration": {
 *          "service_name": "AMBARI",
 *          "component_name": "AMBARI_SERVER",
 *          "category": "ldap-configuration",
 *          "properties": {
 *             "authentication.ldap.primaryUrl": "localhost:33389"
 *             "authentication.ldap.secondaryUrl": "localhost:333"
 *             "authentication.ldap.baseDn": "dc=ambari,dc=apache,dc=org"
 *             // ......
 *          }
 *        }
 *      }
 * </pre>
 */
@Api(value = "Root Service Configurations", description = "Endpoint for Ambari root service component configuration related operations")
public class RootServiceComponentConfigurationService extends BaseService {

  private static final String REQUEST_TYPE =
      "org.apache.ambari.server.api.services.RootServiceComponentConfigurationRequestSwagger";

  public static final String DIRECTIVE_OPERATION = "op";

  private static final Set<String> DIRECTIVES = Sets.newHashSet(DIRECTIVE_OPERATION);

  public static final Map<BaseResourceDefinition.DirectiveType, Set<String>> DIRECTIVES_MAP =
      ImmutableMap.<BaseResourceDefinition.DirectiveType, Set<String>>builder()
          .put(BaseResourceDefinition.DirectiveType.CREATE, DIRECTIVES)
          .put(BaseResourceDefinition.DirectiveType.UPDATE, DIRECTIVES)
          .build();

  private final String serviceName;
  private final String componentName;

  public RootServiceComponentConfigurationService(String serviceName, String componentName) {
    this.serviceName = serviceName;
    this.componentName = componentName;
  }

  /**
   * Creates a root service component configuration resource.
   *
   * @param body    the payload in json format
   * @param headers http headers
   * @param uri     request uri information
   * @return
   */
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Creates a root service component configuration resource",
      nickname = "RootServiceComponentConfigurationService#createConfiguration")
  @ApiImplicitParams({
      @ApiImplicitParam(dataType = REQUEST_TYPE, paramType = PARAM_TYPE_BODY)
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
  public Response createConfiguration(String body, @Context HttpHeaders headers, @Context UriInfo uri) {
    return handleRequest(headers, body, uri, Request.Type.POST, createResource(null));
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Retrieve all root service component configuration resources",
      nickname = "RootServiceComponentConfigurationService#getConfigurations",
      notes = "Returns all root service component configurations.",
      response = RootServiceComponentConfigurationResponseSwagger.class,
      responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION,
          defaultValue = "Configuration/properties, Configuration/category, Configuration/component_name, Configuration/service_name",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_SORT, value = QUERY_SORT_DESCRIPTION,
          defaultValue = "Configuration/category",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getConfigurations(String body, @Context HttpHeaders headers, @Context UriInfo uri) {
    return handleRequest(headers, body, uri, Request.Type.GET, createResource(null));
  }

  @GET
  @Path("{category}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Retrieve the details of a root service component configuration resource",
      nickname = "RootServiceComponentConfigurationService#getConfiguration",
      response = RootServiceComponentConfigurationResponseSwagger.class)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "Configuration/*",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getConfiguration(String body, @Context HttpHeaders headers, @Context UriInfo uri,
                                   @PathParam("category") String category) {
    return handleRequest(headers, body, uri, Request.Type.GET, createResource(category));
  }

  @PUT
  @Path("{category}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Updates root service component configuration resources ",
      nickname = "RootServiceComponentConfigurationService#updateConfiguration")
  @ApiImplicitParams({
      @ApiImplicitParam(dataType = REQUEST_TYPE, paramType = PARAM_TYPE_BODY),
      @ApiImplicitParam(name = QUERY_FIELDS, value = QUERY_FILTER_DESCRIPTION, defaultValue = "Configuration/*",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
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
  public Response updateConfiguration(String body, @Context HttpHeaders headers, @Context UriInfo uri,
                                      @PathParam("category") String category) {
    return handleRequest(headers, body, uri, Request.Type.PUT, createResource(category));
  }

  @DELETE
  @Path("{category}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Deletes a root service component configuration resource",
      nickname = "RootServiceComponentConfigurationService#deleteConfiguration")
  @ApiResponses({
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
      @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
  })
  public Response deleteConfiguration(String body, @Context HttpHeaders headers, @Context UriInfo uri,
                                      @PathParam("category") String category) {
    return handleRequest(headers, body, uri, Request.Type.DELETE, createResource(category));
  }

  ResourceInstance createResource(String categoryName) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.RootService, serviceName);
    mapIds.put(Resource.Type.RootServiceComponent, componentName);
    mapIds.put(Resource.Type.RootServiceComponentConfiguration, categoryName);

    return createResource(Resource.Type.RootServiceComponentConfiguration, mapIds);
  }

}
