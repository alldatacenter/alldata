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
 * DR target cluster service.
 */
@Path("/targets/")
public class TargetClusterService extends BaseService {

  /**
   * Handles: GET /targets/{targetName}
   * Get a specific target.
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param targetName    target id
   * @return target instance representation
   */
  @GET @ApiIgnore // until documented
  @Path("{targetName}")
  @Produces("text/plain")
  public Response getTargetCluster(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                          @PathParam("targetName") String targetName) {

    return handleRequest(headers, body, ui, Request.Type.GET, createTargetClusterResource(targetName));
  }

  /**
   * Handles: GET  /targets
   * Get all targets.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return target collection resource representation
   */
  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getTargetClusters(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET, createTargetClusterResource(null));
  }

  /**
   * Handles: POST /targets/{targetName}
   * Create a specific target.
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param targetName target id
   * @return information regarding the created target
   */
  @POST @ApiIgnore // until documented
  @Path("{targetName}")
  @Produces("text/plain")
  public Response createTargetCluster(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("targetName") String targetName) {

    return handleRequest(headers, body, ui, Request.Type.POST, createTargetClusterResource(targetName));
  }

  /**
   * Handles: PUT /targets/{targetName}
   * Update a specific target.
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param targetName target id
   * @return information regarding the updated target
   */
  @PUT @ApiIgnore // until documented
  @Path("{targetName}")
  @Produces("text/plain")
  public Response updateTargetCluster(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("targetName") String targetName) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createTargetClusterResource(targetName));
  }

  /**
   * Handles: DELETE /targets/{targetName}
   * Delete a specific target.
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param targetName target id
   * @return information regarding the deleted target
   */
  @DELETE @ApiIgnore // until documented
  @Path("{targetName}")
  @Produces("text/plain")
  public Response deleteTargetCluster(@Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("targetName") String targetName) {

    return handleRequest(headers, null, ui, Request.Type.DELETE, createTargetClusterResource(targetName));
  }

  /**
   * Create a target resource instance.
   *
   * @param targetName target name
   *
   * @return a target resource instance
   */
  ResourceInstance createTargetClusterResource(String targetName) {
    return createResource(Resource.Type.DRTargetCluster,
        Collections.singletonMap(Resource.Type.DRTargetCluster, targetName));
  }
}
