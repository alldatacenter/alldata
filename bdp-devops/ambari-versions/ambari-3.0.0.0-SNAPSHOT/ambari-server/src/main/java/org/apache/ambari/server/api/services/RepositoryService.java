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
 * Service responsible for repository requests.
 */
public class RepositoryService extends BaseService {

  /**
   * Extra properties to be inserted into created resource.
   */
  private Map<Resource.Type, String> parentKeyProperties;

  /**
   * Constructor.
   *
   * @param parentKeyProperties extra properties to be inserted into created resource
   */
  public RepositoryService(Map<Resource.Type, String> parentKeyProperties) {
    this.parentKeyProperties = parentKeyProperties;
  }

  /**
   * Creates repository.
   * Handles: POST /repositories requests.
   *
   * @param body    body
   * @param headers http headers
   * @param ui      uri info
   */
  @POST @ApiIgnore // until documented
  @Produces("text/plain")
  public Response createRepository(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.POST, createResource(null));
  }

  /**
   * Creates repository.
   * Handles: POST /repositories requests.
   *
   *@param body    body
   * @param headers http headers
   * @param repoId  repository id
   * @param ui      uri info
   */
  @POST @ApiIgnore // until documented
  @Path("{repoId}")
  @Produces("text/plain")
  public Response createRepository(String body, @Context HttpHeaders headers, @Context UriInfo ui, @PathParam("repoId") String repoId) {
    return handleRequest(headers, body, ui, Request.Type.POST, createResource(repoId));
  }

  /**
   * Gets all repositories.
   * Handles: GET /repositories requests.
   *
   * @param headers http headers
   * @param ui      uri info
   */
  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getRepositories(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET, createResource(null));
  }

  /**
   * Gets a single repository.
   * Handles: GET /repositories/{repoId} requests.
   *
   * @param headers http headers
   * @param ui      uri info
   * @param repoId  repository id
   * @return information regarding the specified repository
   */
  @GET @ApiIgnore // until documented
  @Path("{repoId}")
  @Produces("text/plain")
  public Response getRepository(@Context HttpHeaders headers, @Context UriInfo ui, @PathParam("repoId") String repoId) {
    return handleRequest(headers, null, ui, Request.Type.GET, createResource(repoId));
  }

  /**
   * Updates a single repository.
   * Handles: PUT /repositories/{repoId} requests.
   *
   * @param headers http headers
   * @param ui      uri info
   * @param repoId  repository id
   * @return information regarding the specified repository
   */
  @PUT @ApiIgnore // until documented
  @Path("{repoId}")
  @Produces("text/plain")
  public Response updateRepository(String body, @Context HttpHeaders headers, @Context UriInfo ui, @PathParam("repoId") String repoId) {
    return handleRequest(headers, body, ui, Request.Type.PUT, createResource(repoId));
  }

  /**
   * Create a repository resource instance.
   *
   * @param repoId repository id
   *
   * @return a repository instance
   */
  private ResourceInstance createResource(String repoId) {
    final Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.putAll(parentKeyProperties);
    mapIds.put(Resource.Type.Repository, repoId);
    return createResource(Resource.Type.Repository, mapIds);
  }
}
