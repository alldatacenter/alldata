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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.spi.Resource;

/**
 * Service responsible for kerberos identity resource requests.
 */
public class HostKerberosIdentityService extends BaseService {

  /**
   * Parent cluster id.
   */
  private String clusterName;

  /**
   * Relevant hostname
   */
  private String hostName;

  /**
   * Constructor.
   *
   * @param clusterName cluster name
   * @param hostName    host name
   */
  public HostKerberosIdentityService(String clusterName, String hostName) {
    this.clusterName = clusterName;
    this.hostName = hostName;
  }

  /**
   * Handles GET: /clusters/{clusterID}/services/{serviceID}/components/{componentID}/kerberos_identities/{identityId}
   * Get a specific Kerberos identity.
   *
   * @param headers    http headers
   * @param ui         uri info
   * @param identityID Kerberos identity id
   * @param format     output format
   * @return a component resource representation
   */
  @GET @ApiIgnore // until documented
  @Path("{kerberosIdentityID}")
  @Produces("text/plain")
  public Response getKerberosIdentity(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                      @PathParam("kerberosIdentityID") String identityID,
                                      @QueryParam("format") String format) {

    MediaType mediaType;
    if ("csv".equalsIgnoreCase(format)) {
      mediaType = MEDIA_TYPE_TEXT_CSV_TYPE;
    } else {
      mediaType = null;
    }

    return handleRequest(headers, body, ui, Request.Type.GET, mediaType, createResource(clusterName, hostName, identityID));
  }

  /**
   * Handles GET: /clusters/{clusterID}/services/{serviceID}/components/{componentID}/kerberos_identities
   * Get all Kerberos identities for a service.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return component collection resource representation
   */
  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getKerberosIdentities(String body, @Context HttpHeaders headers, @Context UriInfo ui, @QueryParam("format") String format) {
    return getKerberosIdentity(body, headers, ui, null, format);
  }

  /**
   * Create a kerberos identity resource instance.
   *
   * @param clusterName cluster name
   * @param hostName    host name
   * @param identityId  Kerberos identity id
   * @return a component resource instance
   */
  ResourceInstance createResource(String clusterName, String hostName, String identityId) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, clusterName);
    mapIds.put(Resource.Type.Host, hostName);
    mapIds.put(Resource.Type.HostKerberosIdentity, identityId);

    return createResource(Resource.Type.HostKerberosIdentity, mapIds);
  }

}
