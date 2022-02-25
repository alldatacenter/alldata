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
 * Service for extension link management.
 *
 * An extension version is like a stack version but it contains custom services.  Linking an extension
 * version to the current stack version allows the cluster to install the custom services contained in
 * the extension version.
 */
@Path("/links/")
public class ExtensionLinksService extends BaseService {

  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getExtensionLinks(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.GET, createExtensionLinkResource(null));
  }

  @GET @ApiIgnore // until documented
  @Path("{linkId}")
  @Produces("text/plain")
  public Response getExtensionLink(String body, @Context HttpHeaders headers,
                                  @Context UriInfo ui, @PathParam("linkId") String linkId) {

    return handleRequest(headers, body, ui, Request.Type.GET, createExtensionLinkResource(linkId));
  }

  @POST @ApiIgnore // until documented
  @Produces("text/plain")
  public Response createExtensionLink(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.POST, createExtensionLinkResource(null));
  }

  @DELETE @ApiIgnore // until documented
  @Path("{linkId}")
  @Produces("text/plain")
  public Response deleteExtensionLink(@Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("linkId") String linkId) {

    return handleRequest(headers, null, ui, Request.Type.DELETE, createExtensionLinkResource(linkId));
  }

  @PUT @ApiIgnore // until documented
  @Produces("text/plain")
  public Response updateExtensionLink(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.PUT, createExtensionLinkResource(null));
  }

  @PUT @ApiIgnore // until documented
  @Path("{linkId}")
  @Produces("text/plain")
  public Response updateExtensionLink(String body, @Context HttpHeaders headers, @Context UriInfo ui,
          @PathParam("linkId") String linkId) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createExtensionLinkResource(linkId));
  }

  ResourceInstance createExtensionLinkResource(String linkId) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.ExtensionLink, linkId);
    return createResource(Resource.Type.ExtensionLink, mapIds);
  }

}
