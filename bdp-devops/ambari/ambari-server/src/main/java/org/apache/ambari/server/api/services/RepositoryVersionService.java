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
 * Service responsible for repository versions requests.
 */
public class RepositoryVersionService extends BaseService {

  /**
   * Extra properties to be inserted into created resource.
   */
  private Map<Resource.Type, String> parentKeyProperties;

  /**
   * Constructor.
   *
   * @param parentKeyProperties extra properties to be inserted into created resource
   */
  public RepositoryVersionService(Map<Resource.Type, String> parentKeyProperties) {
    this.parentKeyProperties = parentKeyProperties;
  }

  /**
   * Gets all repository versions.
   * Handles: GET /repository_versions requests.
   *
   * @param headers http headers
   * @param ui      uri info
   */
  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getRepositoryVersions(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET, createResource(null));
  }

  /**
   * Gets a single repository version.
   * Handles: GET /repository_versions/{repositoryVersionId} requests.
   *
   * @param headers               http headers
   * @param ui                    uri info
   * @param repositoryVersionId   the repository version id
   * @return information regarding the specified repository
   */
  @GET @ApiIgnore // until documented
  @Path("{repositoryVersionId}")
  @Produces("text/plain")
  public Response getRepositoryVersion(@Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("repositoryVersionId") String repositoryVersionId) {
    return handleRequest(headers, null, ui, Request.Type.GET, createResource(repositoryVersionId));
  }

  /**
   * Creates a repository version.
   * Handles: POST /repository_versions requests.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @return information regarding the created repository
   */
   @POST @ApiIgnore // until documented
   @Produces("text/plain")
   public Response createRepositoryVersion(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.POST, createResource(null));
  }

  /**
   * Deletes a repository version. Handles: DELETE /repository_versions/{repositoryVersionId} requests.
   *
   * @param headers               http headers
   * @param ui                    uri info
   * @param repositoryVersionId   the repository version id
   * @return information regarding the deleted repository
   */
  @DELETE @ApiIgnore // until documented
  @Path("{repositoryVersionId}")
  @Produces("text/plain")
  public Response deleteRepositoryVersion(@Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("repositoryVersionId") String repositoryVersionId) {
    return handleRequest(headers, null, ui, Request.Type.DELETE, createResource(repositoryVersionId));
  }

  /**
   * Updates a specific repository version. Handles: PUT /repository_versions/{repositoryVersionId} requests.
   *
   * @param headers               http headers
   * @param ui                    uri info
   * @param repositoryVersionId   the repository version id
   * @return information regarding the updated repository
   */
  @PUT @ApiIgnore // until documented
  @Path("{repositoryVersionId}")
  @Produces("text/plain")
  public Response updateRepositoryVersion(String body, @Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("repositoryVersionId") String repositoryVersionId) {
    return handleRequest(headers, body, ui, Request.Type.PUT, createResource(repositoryVersionId));
  }

  /**
   * Handles ANY /{repositoryVersionId}/operating_systems requests.
   *
   * @param repositoryVersionId the repository version id
   * @return operating systems service
   */
  @Path("{repositoryVersionId}/operating_systems")
  public OperatingSystemService getOperatingSystemsHandler(@PathParam("repositoryVersionId") String repositoryVersionId) {
    final Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.putAll(parentKeyProperties);
    mapIds.put(Resource.Type.RepositoryVersion, repositoryVersionId);
    return new OperatingSystemService(mapIds);
  }

  /**
   * Create a repository version resource instance.
   *
   * @param repositoryVersionId repository version id
   *
   * @return a repository resource instance
   */
  private ResourceInstance createResource(String repositoryVersionId) {
    final Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.putAll(parentKeyProperties);
    mapIds.put(Resource.Type.RepositoryVersion, repositoryVersionId);
    return createResource(Resource.Type.RepositoryVersion, mapIds);
  }
}
