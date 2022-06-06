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
 * Service for extensions management.
 *
 * An extension version is like a stack version but it contains custom services.  Linking an extension
 * version to the current stack version allows the cluster to install the custom services contained in
 * the extension version.
 */
@Path("/extensions/")
public class ExtensionsService extends BaseService {

  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getExtensions(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createExtensionResource(null));
  }

  @GET @ApiIgnore // until documented
  @Path("{extensionName}")
  @Produces("text/plain")
  public Response getExtension(String body, @Context HttpHeaders headers,
                           @Context UriInfo ui,
                           @PathParam("extensionName") String extensionName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createExtensionResource(extensionName));
  }

  @GET @ApiIgnore // until documented
  @Path("{extensionName}/versions")
  @Produces("text/plain")
  public Response getExtensionVersions(String body,
                                   @Context HttpHeaders headers,
                                   @Context UriInfo ui, @PathParam("extensionName") String extensionName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createExtensionVersionResource(extensionName, null));
  }

  @GET @ApiIgnore // until documented
  @Path("{extensionName}/versions/{extensionVersion}")
  @Produces("text/plain")
  public Response getExtensionVersion(String body,
                                  @Context HttpHeaders headers,
                                  @Context UriInfo ui, @PathParam("extensionName") String extensionName,
                                  @PathParam("extensionVersion") String extensionVersion) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createExtensionVersionResource(extensionName, extensionVersion));
  }

  @GET @ApiIgnore // until documented
  @Path("{extensionName}/versions/{extensionVersion}/links")
  @Produces("text/plain")
  public Response getExtensionVersionLinks(String body,
                                  @Context HttpHeaders headers,
                                  @Context UriInfo ui, @PathParam("extensionName") String extensionName,
                                  @PathParam("extensionVersion") String extensionVersion) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createExtensionLinkResource(null, null, extensionName, extensionVersion));
  }

  ResourceInstance createExtensionVersionResource(String extensionName,
                                              String extensionVersion) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Extension, extensionName);
    mapIds.put(Resource.Type.ExtensionVersion, extensionVersion);

    return createResource(Resource.Type.ExtensionVersion, mapIds);
  }

  ResourceInstance createExtensionLinkResource(String stackName, String stackVersion,
                                  String extensionName, String extensionVersion) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);
    mapIds.put(Resource.Type.Extension, extensionName);
    mapIds.put(Resource.Type.ExtensionVersion, extensionVersion);

    return createResource(Resource.Type.ExtensionLink, mapIds);
  }

  ResourceInstance createExtensionResource(String extensionName) {

    return createResource(Resource.Type.Extension,
        Collections.singletonMap(Resource.Type.Extension, extensionName));

  }
}
