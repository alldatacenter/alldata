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
 * RoleAuthorizationService is a read-only service responsible for role authorization resource requests.
 * <p/>
 * The result sets returned by this service are either the full set of available authorizations or
 * those related to a particular permission.
 */
@Path("/authorizations/")
public class RoleAuthorizationService extends BaseService {
  private String permissionId;

  /**
   * Constructs a new RoleAuthorizationService that is not linked to any role (or permission)
   */
  public RoleAuthorizationService() {
    this(null);
  }

  /**
   * Constructs a new RoleAuthorizationService that is linked to the specified permission
   *
   * @param permissionId the permission id of a permission (or role)
   */
  public RoleAuthorizationService(String permissionId) {
    this.permissionId = permissionId;
  }

  /**
   * Handles: GET  /permissions/{permission_id}/authorizations
   * Get all authorizations for the relative permission, or all if this RoleAuthorizationService is
   * not linked to a particular permission.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return authorizations collection resource representation
   */
  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getAuthorizations(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET, createAuthorizationResource(null));
  }

  /**
   * Handles: GET  /permissions/{permission_id}/authorizations/{authorization_id}
   * Get a specific authorization, potentially limited to the set of authorizations for a permission
   * if this RoleAuthorizationService is linked ot a particular permission.
   *
   * @param headers         http headers
   * @param ui              uri info
   * @param authorizationId authorization ID
   * @return authorization instance representation
   */
  @GET @ApiIgnore // until documented
  @Path("{authorization_id}")
  @Produces("text/plain")
  public Response getAuthorization(@Context HttpHeaders headers, @Context UriInfo ui,
                                   @PathParam("authorization_id") String authorizationId) {
    return handleRequest(headers, null, ui, Request.Type.GET, createAuthorizationResource(authorizationId));
  }

  /**
   * Create an authorization resource.
   *
   * @param authorizationId authorization id
   * @return an authorization resource instance
   */
  protected ResourceInstance createAuthorizationResource(String authorizationId) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Permission, permissionId);
    mapIds.put(Resource.Type.RoleAuthorization, authorizationId);
    return createResource(Resource.Type.RoleAuthorization, mapIds);
  }
}
