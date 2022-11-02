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
package org.apache.ambari.server.api.services.users;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.BaseService;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.controller.ActiveWidgetLayoutResponse;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.commons.lang.StringUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * WidgetLayout Service
 */
@Path("/users/{userName}/activeWidgetLayouts")
@Api(value = "Users", description = "Endpoint for User specific operations")
public class ActiveWidgetLayoutService extends BaseService {

  /**
   * Handles URL: /users/{userName}/activeWidgetLayouts
   * Get all instances for a view.
   *
   * @param headers  http headers
   * @param ui       uri info
   * @param userName user name
   *
   * @return instance collection resource representation
   */
  @GET
  @Produces("text/plain")
  @ApiOperation(value = "Get user widget layouts", nickname = "ActiveWidgetLayoutService#getServices", notes = "Returns all active widget layouts for user.", response = ActiveWidgetLayoutResponse.class, responseContainer = "List")
  @ApiImplicitParams({
    @ApiImplicitParam(name = "fields", value = "Filter user layout details", defaultValue = "WidgetLayoutInfo/*", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "sortBy", value = "Sort layouts (asc | desc)", defaultValue = "WidgetLayoutInfo/user_name.asc", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "page_size", value = "The number of resources to be returned for the paged response.", defaultValue = "10", dataType = "integer", paramType = "query"),
    @ApiImplicitParam(name = "from", value = "The starting page resource (inclusive). Valid values are :offset | \"start\"", defaultValue = "0", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "to", value = "The ending page resource (inclusive). Valid values are :offset | \"end\"", dataType = "string", paramType = "query")
  })
  public Response getServices(String body, @Context HttpHeaders headers, @Context UriInfo ui, @ApiParam(value = "user name", required = true)
                              @PathParam("userName") String userName) {

    return handleRequest(headers, body, ui, Request.Type.GET, createResource(userName));
  }

  /**
   *
   * @param body      body
   * @param headers   http headers
   * @param ui        uri info
   * @param userName  user name
   * @return
   */
  @PUT
  @Produces("text/plain")
  @ApiOperation(value = "Update user widget layouts", nickname = "ActiveWidgetLayoutService#updateServices", notes = "Updates user widget layout.")
  @ApiImplicitParams({
    @ApiImplicitParam(name = "body", value = "input parameters in json form", required = true, dataType = "org.apache.ambari.server.controller.ActiveWidgetLayoutRequest", paramType = "body")
  })
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful operation"),
    @ApiResponse(code = 500, message = "Server Error")}
  )
  public Response updateServices(String body, @Context HttpHeaders headers, @Context UriInfo ui, @ApiParam(value = "user name", required = true)
                                 @PathParam("userName") String userName) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createResource(userName));
  }

  private ResourceInstance createResource(String userName) {
    Map<Resource.Type,String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.User, StringUtils.lowerCase(userName));
    return createResource(Resource.Type.ActiveWidgetLayout, mapIds);
  }

}
