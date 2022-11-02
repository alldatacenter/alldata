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
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.internal.VersionDefinitionResourceProvider;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.commons.codec.binary.Base64;

import com.google.gson.JsonObject;

@Path("/version_definitions/")
public class VersionDefinitionService extends BaseService {

  @GET @ApiIgnore // until documented
  @Produces(MediaType.TEXT_PLAIN)
  public Response getServices(@Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, null, ui, Request.Type.GET,
      createResource(null));
  }

  @GET @ApiIgnore // until documented
  @Path("{versionId}")
  @Produces(MediaType.TEXT_PLAIN)
  public Response getService(@Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("versionId") String versionId) {

    return handleRequest(headers, null, ui, Request.Type.GET,
      createResource(versionId));
  }

  /**
   * Handles ANY /{versionNumber}/operating_systems requests.
   *
   * @param versionNumber the repository version id, whether it be from the DB or available.
   * @return operating systems service
   */
  @Path("{versionNumber}/operating_systems")
  public OperatingSystemService getOperatingSystemsHandler(@PathParam("versionNumber") String versionNumber) {
    final Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.VersionDefinition, versionNumber);
    return new OperatingSystemService(mapIds);
  }

  @POST @ApiIgnore // until documented
  @Produces(MediaType.TEXT_PLAIN)
  public Response createVersion(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.POST,
        createResource(null));
  }

  /**
   * Creates a version by directly POSTing the XML body.  Unfortunately the request processor
   * uses JSON, so an appropriate JSON structure must be made to get to the ResourceProvider.
   * @param body     the XML
   * @param headers  the headers
   * @param ui       the URI info
   */
  @POST @ApiIgnore // until documented
  @Consumes({MediaType.TEXT_XML})
  @Produces(MediaType.TEXT_PLAIN)
  public Response createVersionByXml(String body, @Context HttpHeaders headers,
      @Context UriInfo ui) throws Exception {

    String encoded = Base64.encodeBase64String(body.getBytes("UTF-8"));

    JsonObject obj = new JsonObject();
    obj.addProperty(VersionDefinitionResourceProvider.VERSION_DEF_BASE64_PROPERTY, encoded);

    JsonObject payload = new JsonObject();
    payload.add(VersionDefinitionResourceProvider.VERSION_DEF, obj);

    return handleRequest(headers, payload.toString(), ui, Request.Type.POST,
        createResource(null));
  }

  protected ResourceInstance createResource(String versionId) {
    return createResource(Resource.Type.VersionDefinition,
        Collections.singletonMap(Resource.Type.VersionDefinition, versionId));
  }

}
