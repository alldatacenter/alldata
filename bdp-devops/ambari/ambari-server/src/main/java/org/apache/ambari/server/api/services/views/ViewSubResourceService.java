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

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.BaseService;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.view.ViewResourceHandler;

/**
 * View sub-resource service.
 */
public class ViewSubResourceService extends BaseService implements ViewResourceHandler {
  /**
   * The type of the sub-resource.
   */
  private final Resource.Type type;

  /**
   * The associated view name.
   */
  private final String viewName;

  /**
   * The view version.
   */
  private final String version;

  /**
   * The associated view instance name.
   */
  private final String instanceName;


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a view sub-resource service.
   */
  public ViewSubResourceService(Resource.Type type, ViewInstanceEntity viewInstanceDefinition) {
    ViewEntity viewEntity = viewInstanceDefinition.getViewEntity();

    this.type         = type;
    this.viewName     = viewEntity.getCommonName();
    this.version      = viewEntity.getVersion();
    this.instanceName = viewInstanceDefinition.getName();
  }


  // ----- ViewResourceHandler -----------------------------------------------

  @Override
  public Response handleRequest(HttpHeaders headers, UriInfo ui,
                                RequestType requestType, MediaType mediaType,
                                String resourceId) {
    return handleRequest(headers, null, ui, getRequestType(requestType),
        getMediaType(mediaType), createResource(resourceId));
  }

  @Override
  public Response handleRequest(HttpHeaders headers, UriInfo ui, String resourceId) {
    return handleRequest(headers, null, ui, Request.Type.GET,
        createResource(resourceId));
  }


  // ----- helper methods ----------------------------------------------------

  // create a resource with the given id
  protected ResourceInstance createResource(String resourceId) {
    Map<Resource.Type,String> mapIds = new HashMap<>();

    mapIds.put(Resource.Type.View, viewName);
    mapIds.put(Resource.Type.ViewVersion, version);
    mapIds.put(Resource.Type.ViewInstance, instanceName);

    if (resourceId != null) {
      mapIds.put(type, resourceId);
    }
    return super.createResource(type, mapIds);
  }

  // get the internal request type from the view API request type
  private Request.Type getRequestType(RequestType type) {
    switch (type) {
      case GET:
        return Request.Type.GET;
      case POST:
        return Request.Type.POST;
      case PUT:
        return Request.Type.PUT;
      case DELETE:
        return Request.Type.DELETE;
      case QUERY_POST:
        return Request.Type.QUERY_POST;
    }
    throw new IllegalArgumentException("Unknown resource type " + type);
  }

  // get the JAX-RS media type from the view media type
  private javax.ws.rs.core.MediaType getMediaType(MediaType type) {
    switch (type) {
      case TEXT_PLAIN:
        return javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
      case APPLICATION_JSON:
        return javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
    }
    throw new IllegalArgumentException("Unknown media type " + type);
  }
}
