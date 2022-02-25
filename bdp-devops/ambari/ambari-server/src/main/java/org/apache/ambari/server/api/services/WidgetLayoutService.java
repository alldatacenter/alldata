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
 * WidgetLayout Service
 */
public class WidgetLayoutService extends BaseService {
  
  private final String clusterName;

  public  WidgetLayoutService(String clusterName) {
    this.clusterName = clusterName;
  }

  @GET @ApiIgnore // until documented
  @Path("{widgetLayoutId}")
  @Produces("text/plain")
  public Response getService(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("widgetLayoutId") String widgetLayoutId) {

    return handleRequest(headers, body, ui, Request.Type.GET,
            createResource(widgetLayoutId));
  }

  /**
   * Handles URL: /widget_layouts
   * Get all instances for a view.
   *
   * @param headers  http headers
   * @param ui       uri info
   *
   * @return instance collection resource representation
   */
  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getServices(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET,
            createResource(null));
  }

  @POST @ApiIgnore // until documented
  @Path("{widgetLayoutId}")
  @Produces("text/plain")
  public Response createService(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("widgetLayoutId") String widgetLayoutId) {
    return handleRequest(headers, body, ui, Request.Type.POST,
            createResource(widgetLayoutId));
  }

  @POST @ApiIgnore // until documented
  @Produces("text/plain")
  public Response createServices(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.POST,
            createResource(null));
  }

  @PUT @ApiIgnore // until documented
  @Path("{widgetLayoutId}")
  @Produces("text/plain")
  public Response updateService(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("widgetLayoutId") String widgetLayoutId) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createResource(widgetLayoutId));
  }

  @PUT @ApiIgnore // until documented
  @Produces("text/plain")
  public Response updateServices(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createResource(null));
  }

  @DELETE @ApiIgnore // until documented
  @Path("{widgetLayoutId}")
  @Produces("text/plain")
  public Response deleteService(@Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("widgetLayoutId") String widgetLayoutId) {

    return handleRequest(headers, null, ui, Request.Type.DELETE, createResource(widgetLayoutId));
  }

  private ResourceInstance createResource(String widgetLayoutId) {
    Map<Resource.Type,String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.WidgetLayout, widgetLayoutId);
    mapIds.put(Resource.Type.Cluster, clusterName);
    return createResource(Resource.Type.WidgetLayout, mapIds);
  }
}
