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
 * Service responsible for handling REST requests for the /clusters/{cluster_name}/credentials endpoint.
 */
public class CredentialService extends BaseService {

  private final String clusterName;

  public CredentialService(String clusterName) {
    this.clusterName = clusterName;
  }

  /**
   * Handles: GET  /clusters/{cluster_name}/credentials
   * Get all credentials.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return credential collection resource representation
   */
  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getCredentials(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET, createCredentialResource(null));
  }

  /**
   * Handles: GET  /clusters/{cluster_name}/credentials/{alias}
   * Get a specific credential.
   *
   * @param headers http headers
   * @param ui      uri info
   * @param alias   alias (or credential ID)
   * @return credential instance representation
   */
  @GET @ApiIgnore // until documented
  @Path("{alias}")
  @Produces("text/plain")
  public Response getCredential(@Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("alias") String alias) {
    return handleRequest(headers, null, ui, Request.Type.GET, createCredentialResource(alias));
  }

  /**
   * Handles: POST /clusters/{cluster_name}/credentials/{alias}
   * Create a specific credential.
   *
   * @param headers http headers
   * @param ui      uri info
   * @param alias   alias (or credential ID)
   * @return information regarding the created credential
   */
  @POST @ApiIgnore // until documented
  @Path("{alias}")
  @Produces("text/plain")
  public Response createCredential(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                   @PathParam("alias") String alias) {
    return handleRequest(headers, body, ui, Request.Type.POST, createCredentialResource(alias));
  }

  /**
   * Handles: PUT /clusters/{cluster_name}/credentials/{alias}
   * Create a specific credential.
   *
   * @param headers http headers
   * @param ui      uri info
   * @param alias   alias (or credential ID)
   * @return information regarding the created credential
   */
  @PUT @ApiIgnore // until documented
  @Path("{alias}")
  @Produces("text/plain")
  public Response updateCredential(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                   @PathParam("alias") String alias) {
    return handleRequest(headers, body, ui, Request.Type.PUT, createCredentialResource(alias));
  }

  /**
   * Handles: DELETE /clusters/{cluster_name}/credentials/{alias}
   * Delete a specific credential.
   *
   * @param headers http headers
   * @param ui      uri info
   * @param alias   alias (or credential ID)
   * @return information regarding the deleted credential
   */
  @DELETE @ApiIgnore // until documented
  @Path("{alias}")
  @Produces("text/plain")
  public Response deleteCredential(@Context HttpHeaders headers, @Context UriInfo ui,
                                   @PathParam("alias") String alias) {
    return handleRequest(headers, null, ui, Request.Type.DELETE, createCredentialResource(alias));
  }

  /**
   * Create a credential resource instance.
   *
   * @param alias alias (or credential ID)
   * @return a credential resource instance
   */
  ResourceInstance createCredentialResource(String alias) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, this.clusterName);
    mapIds.put(Resource.Type.Credential, alias);

    return createResource(Resource.Type.Credential, mapIds);
  }
}
