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
 * Endpoint for alert notices.
 */
public class AlertNoticeService extends BaseService {

  private String clusterName = null;

  /**
   * Constructor.
   *
   * @param clusterName
   *          the cluster name (not {@code null}).
   */
  AlertNoticeService(String clusterName) {
    this.clusterName = clusterName;
  }

  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getNotices(
      @Context HttpHeaders headers,
      @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET,
        createResourceInstance(clusterName, null));
  }

  @GET @ApiIgnore // until documented
  @Path("{alertNoticeId}")
  @Produces("text/plain")
  public Response getNotice(
      @Context HttpHeaders headers,
      @Context UriInfo ui, @PathParam("alertNoticeId") Long id) {
    return handleRequest(headers, null, ui, Request.Type.GET,
        createResourceInstance(clusterName, id));
  }

  /**
   * Create an alert history resource instance
   *
   * @param clusterName
   * @return
   */
  private ResourceInstance createResourceInstance(String clusterName,
      Long alertNoticeId) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, clusterName);

    if (null != alertNoticeId) {
      mapIds.put(Resource.Type.AlertNotice, alertNoticeId.toString());
    }

    return createResource(Resource.Type.AlertNotice, mapIds);
  }
}
