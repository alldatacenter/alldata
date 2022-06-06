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
import org.apache.ambari.server.controller.UserAuthorizationResponse;
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
 * UserAuthorizationService is a read-only service responsible for user authorization resource requests.
 * <p/>
 * The result sets returned by this service represent the set of authorizations assigned to a given user.
 * Authorizations are tied to a resource, so a user may have the multiple authorization entries for the
 * same authorization id (for example VIEW.USE), however each will represnet a different view instance.
 */
@Path("/users/{userName}/authorizations")
@Api(value = "Users", description = "Endpoint for user specific operations")
public class UserAuthorizationService extends BaseService {

  /**
   * Handles: GET  /users/{user_name}/authorizations
   * Get all authorizations for the relative user.
   *
   * @param headers        http headers
   * @param ui             uri info
   * @param userName       user name
   * @return authorizations collection resource representation
   */
  @GET
  @Produces("text/plain")
  @ApiOperation(value = "Get all authorizations", nickname = "UserAuthorizationService#getAuthorizations", notes = "Returns all authorization for user.", response = UserAuthorizationResponse.class, responseContainer = "List")
  @ApiImplicitParams({
    @ApiImplicitParam(name = "fields", value = "Filter user authorization details", defaultValue = "AuthorizationInfo/*", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "sortBy", value = "Sort user authorizations (asc | desc)", defaultValue = "AuthorizationInfo/user_name.asc", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "page_size", value = "The number of resources to be returned for the paged response.", defaultValue = "10", dataType = "integer", paramType = "query"),
    @ApiImplicitParam(name = "from", value = "The starting page resource (inclusive). Valid values are :offset | \"start\"", defaultValue = "0", dataType = "string", paramType = "query"),
    @ApiImplicitParam(name = "to", value = "The ending page resource (inclusive). Valid values are :offset | \"end\"", dataType = "string", paramType = "query")
  })
  public Response getAuthorizations(@Context HttpHeaders headers, @Context UriInfo ui, @ApiParam(value = "user name", required = true)
                                    @PathParam ("userName") String userName) {
    return handleRequest(headers, null, ui, Request.Type.GET, createAuthorizationResource(userName,null));
  }

  /**
   * Handles: GET  /users/{userName}/authorizations/{authorization_id}
   * Get a specific authorization.
   *
   * @param headers         http headers
   * @param ui              uri info
   * @param userName        user name
   * @param authorizationId authorization ID
   * @return authorization instance representation
   */
  @GET
  @Path("{authorization_id}")
  @Produces("text/plain")
  @ApiOperation(value = "Get user authorization", nickname = "UserAuthorizationService#getAuthorization", notes = "Returns user authorization details.", response = UserAuthorizationResponse.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = "fields", value = "Filter user authorization details", defaultValue = "AuthorizationInfo/*", dataType = "string", paramType = "query")
  })
  @ApiResponses(value = {
    @ApiResponse(code = 200, message = "Successful operation", response = UserAuthorizationResponse.class)}
  )
  public Response getAuthorization(@Context HttpHeaders headers, @Context UriInfo ui, @ApiParam(value = "user name", required = true)
  @PathParam ("userName") String userName, @ApiParam(value = "Authorization Id", required = true) @PathParam("authorization_id") String authorizationId) {
    return handleRequest(headers, null, ui, Request.Type.GET, createAuthorizationResource(userName, authorizationId));
  }

  /**
   * Create an authorization resource.
   * @param userName         user name
   * @param authorizationId authorization id
   * @return an authorization resource instance
   */
  protected ResourceInstance createAuthorizationResource(String userName, String authorizationId) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.User, StringUtils.lowerCase(userName));
    mapIds.put(Resource.Type.UserAuthorization, authorizationId);
    return createResource(Resource.Type.UserAuthorization, mapIds);
  }
}
