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
 * Service responsible for stage resource requests.
 */
public class StageService extends BaseService {
  /**
   * Parent cluster name.
   */
  private String m_clusterName;

  /**
   * Parent request id.
   */
  private String m_requestId;

  // ----- Constructors ------------------------------------------------------

  /**
   * Constructor.
   *
   * @param clusterName  cluster id
   * @param requestId    request id
   */
  public StageService(String clusterName, String requestId) {
    m_clusterName = clusterName;
    m_requestId = requestId;
  }

  // ----- StageService ------------------------------------------------------

  /**
   * Handles URL: /clusters/{clusterID}/requests/{requestID}/stages/{stageID} or
   * /requests/{requestId}/stages/{stageID}
   * Get a specific stage.
   *
   * @param headers  http headers
   * @param ui       uri info
   * @param stageId  stage id
   *
   * @return stage resource representation
   */
  @GET @ApiIgnore // until documented
  @Path("{stageId}")
  @Produces("text/plain")
  public Response getStage(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                           @PathParam("stageId") String stageId) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStageResource(m_clusterName, m_requestId, stageId));
  }

  /**
   * Handles URL: /clusters/{clusterId}/requests/{requestID}/stages or /requests/{requestID}/stages
   * Get all stages for a request.
   *
   * @param headers  http headers
   * @param ui       uri info
   *
   * @return stage collection resource representation
   */
  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getStages(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET,
        createStageResource(m_clusterName, m_requestId, null));
  }

  /**
   * Gets the tasks sub-resource.
   */
  @Path("{stageId}/tasks")
  public TaskService getTaskHandler(@PathParam("stageId") String stageId) {
    return new TaskService(m_clusterName, m_requestId, stageId);
  }

  /**
   * Handles: PUT /clusters/{clusterId}/requests/{requestId}/stages/{stageId} or /requests/{requestId}/stages/{stageId}
   * Change state of existing stages.
   *
   * @param body        http body
   * @param headers     http headers
   * @param ui          uri info
   * @return information regarding the created services
   */
  @PUT @ApiIgnore // until documented
  @Path("{stageId}")
  @Produces("text/plain")
  public Response updateStages(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                               @PathParam("stageId") String stageId) {
    return handleRequest(headers, body, ui, Request.Type.PUT, createStageResource(m_clusterName, m_requestId, stageId));
  }

  /**
   * Handles: POST /clusters/{clusterId}/requests/{requestId}/stages or /requests/{requestId}/stages
   * Create multiple services.
   *
   * @param body        http body
   * @param headers     http headers
   * @param ui          uri info
   * @return information regarding the created services
   */
  @POST @ApiIgnore // until documented
  @Produces("text/plain")
  public Response createStages(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.POST, createStageResource(m_clusterName, m_requestId, null));
  }

  /**
   * Create a stage resource instance.
   *
   * @param clusterName  cluster name
   * @param requestId    request id
   * @param stageId      stage id
   *
   * @return a stage resource instance
   */
  ResourceInstance createStageResource(String clusterName, String requestId, String stageId) {
    Map<Resource.Type,String> mapIds = new HashMap<>();

    if (clusterName != null) {
      mapIds.put(Resource.Type.Cluster, clusterName);
    }
    mapIds.put(Resource.Type.Request, requestId);
    mapIds.put(Resource.Type.Stage, stageId);

    return createResource(Resource.Type.Stage, mapIds);
  }
}
