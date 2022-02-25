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
 * Service responsible for Remote Cluster resource requests.
 */
@Path("/remoteclusters")
public class RemoteClustersService extends BaseService {

  /**
   * Get the list of all Remote Clusters
   * @param headers
   * @param ui
   * @return collections of all remote clusters
   */
  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getRemoteClusters(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET, createRemoteClusterResource(null));
  }

  /**
   * Create a new RemoteAmbariCluster
   * @param body
   * @param headers
   * @param ui
   * @param clusterName
   * @return
   */
  @POST @ApiIgnore // until documented
  @Path("{clusterName}")
  @Produces("text/plain")
  public Response createRemoteCluster(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("clusterName") String clusterName) {
    return handleRequest(headers, body, ui, Request.Type.POST, createRemoteClusterResource(clusterName));
  }

  /**
   * Update a Remote Cluster
   * @param body
   * @param headers
   * @param ui
   * @param clusterName
   * @return
   */
  @PUT @ApiIgnore // until documented
  @Path("{clusterName}")
  @Produces("text/plain")
  public Response updateRemoteCluster(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                            @PathParam("clusterName") String clusterName) {
    return handleRequest(headers, body, ui, Request.Type.PUT, createRemoteClusterResource(clusterName));
  }

  /**
   * Delete a Remote Cluster
   * @param body
   * @param headers
   * @param ui
   * @param clusterName
   * @return
   */
  @DELETE @ApiIgnore // until documented
  @Path("{clusterName}")
  @Produces("text/plain")
  public Response deleteRemoteCluster(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                            @PathParam("clusterName") String clusterName) {
    return handleRequest(headers, body, ui, Request.Type.DELETE, createRemoteClusterResource(clusterName));
  }

  /**
   * Get information about a Remote Cluster
   * @param headers
   * @param ui
   * @param clusterName
   * @return
   */
  @GET @ApiIgnore // until documented
  @Path("{clusterName}")
  @Produces("text/plain")
  public Response getRemoteCluster(@Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("clusterName") String clusterName) {
    return handleRequest(headers, null, ui, Request.Type.GET, createRemoteClusterResource(clusterName));
  }

  // ----- helper methods ----------------------------------------------------

  /**
   * Create a Remote Cluster resource.
   *
   * @param clusterName Name of the Cluster
   *
   * @return a RemoteCluster resource Instance
   */
  private ResourceInstance createRemoteClusterResource(String clusterName) {
    return createResource(Resource.Type.RemoteCluster,Collections.singletonMap(Resource.Type.RemoteCluster, clusterName));
  }
}
