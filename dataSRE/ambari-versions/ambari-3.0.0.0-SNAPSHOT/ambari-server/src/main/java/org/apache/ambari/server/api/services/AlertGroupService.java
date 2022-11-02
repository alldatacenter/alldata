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
 * The {@link AlertGroupService} handles CRUD operations for a cluster's alert
 * groups.
 */
public class AlertGroupService extends BaseService {

  /**
   * Cluster name for cluster-based requests
   */
  private String m_clusterName = null;

  /**
   * Constructor.
   *
   * @param clusterName
   */
  AlertGroupService(String clusterName) {
    m_clusterName = clusterName;
  }

  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getGroups(@Context HttpHeaders headers,
      @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET,
        createAlertGroupResource(m_clusterName, null));
  }

  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  @Path("{groupId}")
  public Response getGroup(@Context HttpHeaders headers,
      @Context UriInfo ui, @PathParam("groupId") Long groupId) {
    return handleRequest(headers, null, ui, Request.Type.GET,
        createAlertGroupResource(m_clusterName, groupId));
  }

  @POST @ApiIgnore // until documented
  @Produces("text/plain")
  public Response createGroup(String body, @Context HttpHeaders headers,
      @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.POST,
        createAlertGroupResource(m_clusterName, null));
  }

  @PUT @ApiIgnore // until documented
  @Produces("text/plain")
  @Path("{groupId}")
  public Response updateGroup(String body, @Context HttpHeaders headers,
      @Context UriInfo ui, @PathParam("groupId") Long groupId) {
    return handleRequest(headers, body, ui, Request.Type.PUT,
        createAlertGroupResource(m_clusterName, groupId));
  }

  @DELETE @ApiIgnore // until documented
  @Produces("text/plain")
  @Path("{groupId}")
  public Response deleteGroup(String body, @Context HttpHeaders headers,
      @Context UriInfo ui, @PathParam("groupId") Long groupId) {
    return handleRequest(headers, body, ui, Request.Type.DELETE,
        createAlertGroupResource(m_clusterName, groupId));
  }

  /**
   * Create a request capturing the group ID and resource type for an alert
   * group.
   *
   * @param groupId
   *          the unique ID of the group to create the query for (not
   *          {@code null}).
   * @return the instance of the query.
   */
  private ResourceInstance createAlertGroupResource(String clusterName,
      Long groupId) {

    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, m_clusterName);
    mapIds.put(Resource.Type.AlertGroup,
        null == groupId ? null : groupId.toString());

    return createResource(Resource.Type.AlertGroup, mapIds);
  }
}
