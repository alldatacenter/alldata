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
 * Endpoint for cluster upgrade items.
 */
public class UpgradeItemService extends BaseService {

  private String m_clusterName = null;
  private String m_upgradeId = null;
  private String m_upgradeGroupId = null;

  UpgradeItemService(String clusterName, String upgradeId, String upgradeGroupId) {
    m_clusterName = clusterName;
    m_upgradeId = upgradeId;
    m_upgradeGroupId = upgradeGroupId;
  }

  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getUpgrades(
      @Context HttpHeaders headers,
      @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET,
        createResourceInstance(null));
  }

  @GET @ApiIgnore // until documented
  @Path("{upgradeItemId}")
  @Produces("text/plain")
  public Response getUpgradeItem(String body,
      @Context HttpHeaders headers,
      @Context UriInfo ui, @PathParam("upgradeItemId") Long id) {
    return handleRequest(headers, body, ui, Request.Type.GET,
        createResourceInstance(id));
  }

  /**
   * Handles:
   * PUT /clusters/{clusterId}/upgrades/{upgradeId}/upgrade_groups/{upgradeGroupId}/upgrade_items/{upgradeItemId}
   *
   * Change state of existing upgrade item.
   *
   * @param body     http body
   * @param headers  http headers
   * @param ui       uri info
   * @param id       the upgrade item id
   *
   * @return information regarding the created services
   */
  @PUT @ApiIgnore // until documented
  @Path("{upgradeItemId}")
  @Produces("text/plain")
  public Response updateUpgradeItem(String body,
                                    @Context HttpHeaders headers,
                                    @Context UriInfo ui, @PathParam("upgradeItemId") Long id) {
    return handleRequest(headers, body, ui, Request.Type.PUT, createResourceInstance(id));
  }

  @Path("{upgradeItemId}/tasks")
  public TaskService getTasks(
      @Context HttpHeaders headers,
      @Context UriInfo ui,
      @PathParam("upgradeItemId") Long id) {
    return new TaskService(m_clusterName, m_upgradeId, id.toString());
  }

  /**
   * @param upgradeItemId the specific item id
   * @return the resource instance
   */
  ResourceInstance createResourceInstance(Long upgradeItemId) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, m_clusterName);
    mapIds.put(Resource.Type.Upgrade, m_upgradeId);
    mapIds.put(Resource.Type.UpgradeGroup, m_upgradeGroupId);

    if (null != upgradeItemId) {
      mapIds.put(Resource.Type.UpgradeItem, upgradeItemId.toString());
    }

    return createResource(Resource.Type.UpgradeItem, mapIds);
  }
}
