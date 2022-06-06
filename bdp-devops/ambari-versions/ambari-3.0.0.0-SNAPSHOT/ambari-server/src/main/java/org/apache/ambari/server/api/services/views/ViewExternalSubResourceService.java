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

package org.apache.ambari.server.api.services.views;

import java.io.IOException;
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
import org.apache.ambari.server.api.services.BaseService;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;

/**
 * Service responsible for view sub-resource requests.
 */
public class ViewExternalSubResourceService  extends BaseService {

  /**
   * The resource type.
   */
  private final Resource.Type type;

  /**
   * The view name.
   */
  private final String viewName;

  /**
   * The view version.
   */
  private final String version;

  /**
   * The instance name.
   */
  private final String instanceName;

  /**
   * Mapping of resource names to services.
   */
  private final Map<String, Object> resourceServiceMap = new HashMap<>();

  // ----- Constructors ------------------------------------------------------

  public ViewExternalSubResourceService(Resource.Type type, ViewInstanceEntity viewInstanceDefinition) {
    ViewEntity viewEntity = viewInstanceDefinition.getViewEntity();

    this.type         = type;
    this.viewName     = viewEntity.getCommonName();
    this.version      = viewEntity.getVersion();
    this.instanceName = viewInstanceDefinition.getName();
  }

  /**
   * Handles URL: /resources
   * Get all external resources for a view.
   *
   * @param headers  http headers
   * @param ui       uri info
   *
   * @return instance collection resource representation
   */
  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getResources(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET,
        createResource(viewName, instanceName));
  }

  /**
   * Handles: GET /resources/{resourceName} Get a specific external resource.
   *
   * @param resourceName  resource name
   *
   * @return resource service instance representation
   *
   * @throws IllegalArgumentException if the given resource name is unknown
   */
  @Path("{resourceName}")
  public Object getResource(@PathParam("resourceName") String resourceName) throws IOException {

    Object service = resourceServiceMap.get(resourceName);
    if (service == null) {
      throw new IllegalArgumentException("A resource type " + resourceName + " for view instance " +
          viewName + "/" + instanceName + " can not be found.");
    }

    return service;
  }

  // ----- helper methods ----------------------------------------------------

  /**
   * Register a sub-resource service.
   *
   * @param resourceName  the resource name
   * @param service       the service
   */
  public void addResourceService(String resourceName, Object service) {
    resourceServiceMap.put(resourceName, service);
  }

  /**
   * Create an view instance resource.
   *
   * @param viewName      view name
   * @param instanceName  instance name
   *
   * @return a view instance resource
   */
  private ResourceInstance createResource(String viewName, String instanceName) {
    Map<Resource.Type,String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.View, viewName);
    mapIds.put(Resource.Type.ViewVersion, version);
    mapIds.put(Resource.Type.ViewInstance, instanceName);
    return createResource(type, mapIds);
  }
}
