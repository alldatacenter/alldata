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
 * Service responsible for stack version requests.
 */
public class HostStackVersionService extends BaseService {
  /**
   * Name of the host.
   */
  private String hostName;

  /**
   * Parent cluster name.
   */
  private String clusterName;

  /**
   * Constructor.
   *
   * @param hostName name of the host
   */
  public HostStackVersionService(String hostName, String clusterName) {
    this.hostName = hostName;
    this.clusterName = clusterName;
  }

  /**
   * Gets all host host version.
   * Handles: GET /hosts/{hostname}/stack_versions requests.
   *
   * @param headers http headers
   * @param ui      uri info
   *
   * @return information regarding all host stack versions
   */
  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getHostStackVersions(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET, createResource(ui, clusterName, hostName, null));
  }

  /**
   * Gets a single host stack version.
   * Handles: GET /hosts/{hostname}/host_versions/{stackversionid} requests.
   *
   * @param headers        http headers
   * @param ui             uri info
   * @param stackVersionId host stack version id
   *
   * @return information regarding the specific host stack version
   */
  @GET @ApiIgnore // until documented
  @Path("{stackVersionId}")
  @Produces("text/plain")
  public Response getHostStackVersion(@Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("stackVersionId") String stackVersionId) {
    return handleRequest(headers, null, ui, Request.Type.GET, createResource(ui, clusterName, hostName, stackVersionId));
  }

  /**
   * Handles: POST /clusters/{clusterID}/hosts/{hostname}/host_versions requests
   * Distribute repositories/install packages on host.
   *
   * @param body        http body
   * @param headers     http headers
   * @param ui          uri info
   * @return information regarding the created services
   */
  @POST @ApiIgnore // until documented
  @Produces("text/plain")
  public Response createRequests(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.POST, createResource(ui, clusterName, hostName, null));
  }

  /**
   * Handles ANY /{stackVersionId}/repository_versions requests.
   *
   * @param stackVersionId host stack version id
   * @return repository version service
   */
  @Path("{stackVersionId}/repository_versions")
  public RepositoryVersionService getRepositoryVersionHandler(@PathParam("stackVersionId") String stackVersionId) {
    final Map<Resource.Type, String> stackVersionProperties = new HashMap<>();
    stackVersionProperties.put(Resource.Type.Host, hostName);
    stackVersionProperties.put(Resource.Type.HostStackVersion, stackVersionId);
    return new RepositoryVersionService(stackVersionProperties);
  }

  /**
   * Create a host stack version resource instance.
   *
   * @param clusterName
   * @param hostName host name
   * @param stackVersionId host stack version id
   * @return a host host version resource instance
   */
  private ResourceInstance createResource(UriInfo ui, String clusterName, String hostName, String stackVersionId) {
    final Map<Resource.Type, String> mapIds = new HashMap<>();
    if (clusterName != null) {
      mapIds.put(Resource.Type.Cluster, clusterName);
    }
    mapIds.put(Resource.Type.Host, hostName);
    mapIds.put(Resource.Type.HostStackVersion, stackVersionId);
    return createResource(Resource.Type.HostStackVersion, mapIds);
  }
}
