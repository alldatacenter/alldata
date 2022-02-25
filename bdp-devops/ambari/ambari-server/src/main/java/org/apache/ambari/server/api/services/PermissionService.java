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

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.spi.Resource;

/**
 * Service responsible for permission resource requests.
 */
@Path("/permissions/")
public class PermissionService extends BaseService {

  /**
   * Handles: GET /permissions/{permissionID}
   * Get a specific permission.
   *
   * @param headers        http headers
   * @param ui             uri info
   * @param permissionId   permission id
   *
   * @return permission instance representation
   */
  @GET @ApiIgnore // until documented
  @Path("{permissionId}")
  @Produces("text/plain")
  public Response getPermission(@Context HttpHeaders headers, @Context UriInfo ui,
                          @PathParam("permissionId") String permissionId) {

    return handleRequest(headers, null, ui, Request.Type.GET, createPermissionResource(permissionId));
  }

  /**
   * Handles: GET  /permissions
   * Get all permissions.
   *
   * @param headers  http headers
   * @param ui       uri info
   *
   * @return permission collection resource representation
   */
  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getPermissions(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET, createPermissionResource(null));
  }

  /**
   * Handles: POST /permissions/{permissionID}
   * Create a specific permission.
   *
   * @param headers    http headers
   * @param ui         uri info
   * @param permissionId   permission id
   *
   * @return information regarding the created permission
   */
  @POST @ApiIgnore // until documented
  @Path("{permissionId}")
  @Produces("text/plain")
  public Response createPermission(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("permissionId") String permissionId) {

    return handleRequest(headers, body, ui, Request.Type.POST, createPermissionResource(permissionId));
  }

  /**
   * Handles: PUT /permissions/{permissionID}
   * Update a specific permission.
   *
   * @param headers   http headers
   * @param ui        uri info
   * @param permissionId  permission id
   *
   * @return information regarding the updated permission
   */
  @PUT @ApiIgnore // until documented
  @Path("{permissionId}")
  @Produces("text/plain")
  public Response updatePermission(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("permissionId") String permissionId) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createPermissionResource(permissionId));
  }

  /**
   * Handles: DELETE /permissions/{permissionID}
   * Delete a specific permission.
   *
   * @param headers   http headers
   * @param ui        uri info
   * @param permissionId  permission id
   *
   * @return information regarding the deleted permission
   */
  @DELETE @ApiIgnore // until documented
  @Path("{permissionId}")
  @Produces("text/plain")
  public Response deletePermission(@Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("permissionId") String permissionId) {

    return handleRequest(headers, null, ui, Request.Type.DELETE, createPermissionResource(permissionId));
  }

  /**
   * Gets the role (permission) authorization service.
   *
   * @param request      the request
   * @param permissionId the permission id
   * @return the RoleAuthorizationService
   */
  @Path("{permissionId}/authorizations")
  public RoleAuthorizationService getRoleAuthorizations(
      @Context javax.ws.rs.core.Request request, @PathParam("permissionId") String permissionId) {
    return new RoleAuthorizationService(permissionId);
  }

  // ----- helper methods ----------------------------------------------------

  /**
   * Create a permission resource.
   *
   * @param permissionId permission name
   *
   * @return a permission resource instance
   */
  protected ResourceInstance createPermissionResource(String permissionId) {
    return createResource(Resource.Type.Permission,
        Collections.singletonMap(Resource.Type.Permission, permissionId));
  }
}
