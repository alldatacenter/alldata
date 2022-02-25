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
 * Endpoint for a detailed summary of the cluster upgrades.
 */
public class UpgradeSummaryService extends BaseService {

  private String m_clusterName = null;

  /**
   * Constructor.
   *
   * @param clusterName the cluster name (not {@code null}).
   */
  UpgradeSummaryService(String clusterName) {
    m_clusterName = clusterName;
  }

  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getUpgradeSummaries(@Context HttpHeaders headers,
                                      @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET,
        createResourceInstance(null));
  }

  @GET @ApiIgnore // until documented
  @Path("{requestId}")
  @Produces("text/plain")
  public Response getUpgradeSummary(@Context HttpHeaders headers,
                                    @Context UriInfo ui,
                                    @PathParam("requestId") Long requestId) {
    return handleRequest(headers, null, ui, Request.Type.GET,
        createResourceInstance(requestId));
  }

  /**
   * @param requestId the upgrade's request Id
   * @return the resource instance
   */
  private ResourceInstance createResourceInstance(Long requestId) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, m_clusterName);

    if (null != requestId) {
      mapIds.put(Resource.Type.UpgradeSummary, requestId.toString());
    }

    return createResource(Resource.Type.UpgradeSummary, mapIds);
  }
}
