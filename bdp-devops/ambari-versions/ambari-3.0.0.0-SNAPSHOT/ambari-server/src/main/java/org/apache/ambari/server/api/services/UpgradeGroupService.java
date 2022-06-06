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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.spi.Resource;

/**
 * Endpoint for cluster upgrade groupings.
 */
public class UpgradeGroupService extends BaseService {

  private String m_clusterName = null;
  private String m_upgradeId = null;

  UpgradeGroupService(String clusterName, String upgradeId) {
    m_clusterName = clusterName;
    m_upgradeId = upgradeId;
  }

  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getGroups(String body,
      @Context HttpHeaders headers,
      @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET,
        createResourceInstance(null));
  }

  @GET @ApiIgnore // until documented
  @Path("{upgradeGroupId}")
  @Produces("text/plain")
  public Response getGroup(String body,
      @Context HttpHeaders headers,
      @Context UriInfo ui, @PathParam("upgradeGroupId") Long id) {
    return handleRequest(headers, body, ui, Request.Type.GET,
        createResourceInstance(id));
  }

  @Path("{upgradeGroupId}/upgrade_items")
  public UpgradeItemService getUpgradeItemService(
      @Context HttpHeaders headers,
      @PathParam("upgradeGroupId") Long groupId) {
    return new UpgradeItemService(m_clusterName, m_upgradeId, groupId.toString());
  }

  /**
   * @param groupId the specific group id
   * @return the resource instance
   */
  private ResourceInstance createResourceInstance(Long groupId) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, m_clusterName);
    mapIds.put(Resource.Type.Upgrade, m_upgradeId);
    mapIds.put(Resource.Type.Request, m_upgradeId);

    if (null != groupId) {
      mapIds.put(Resource.Type.UpgradeGroup, groupId.toString());
    }

    return createResource(Resource.Type.UpgradeGroup, mapIds);
  }
}
