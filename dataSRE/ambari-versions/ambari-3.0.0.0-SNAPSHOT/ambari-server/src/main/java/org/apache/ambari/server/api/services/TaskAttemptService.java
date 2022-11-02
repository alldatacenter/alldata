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
 * Service responsible for task attempt resource requests.
 */
public class TaskAttemptService extends BaseService {
  private String jobId;
  private String workflowId;
  private String clusterName;

  /**
   * Constructor.
   * 
   * @param clusterName
   *          cluster name
   * @param workflowId
   *          workflow id
   * @param jobId
   *          job id
   */
  public TaskAttemptService(String clusterName, String workflowId, String jobId) {
    this.clusterName = clusterName;
    this.workflowId = workflowId;
    this.jobId = jobId;
  }

  /**
   * Handles: GET
   * /workflows/{workflowId}/jobs/{jobId}/taskattempts/{taskattemptid} Get a
   * specific taskattempt.
   * 
   * @param headers
   *          http headers
   * @param ui
   *          uri info
   * @param taskAttemptId
   *          task attempt id
   * 
   * @return task attempt instance representation
   */
  @GET @ApiIgnore // until documented
  @Path("{taskAttemptId}")
  @Produces("text/plain")
  public Response getTaskAttempt(String body, @Context HttpHeaders headers,
      @Context UriInfo ui, @PathParam("taskAttemptId") String taskAttemptId) {
    return handleRequest(
        headers,
        body,
        ui,
        Request.Type.GET,
        createTaskAttemptResource(clusterName, workflowId, jobId, taskAttemptId));
  }

  /**
   * Handles: GET /workflows/{workflowId}/jobs/taskattempts Get all task
   * attempts.
   * 
   * @param headers
   *          http headers
   * @param ui
   *          uri info
   * 
   * @return task attempt collection resource representation
   */
  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getTaskAttempts(String body, @Context HttpHeaders headers,
      @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET,
        createTaskAttemptResource(clusterName, workflowId, jobId, null));
  }

  /**
   * Create a task attempt resource instance.
   * 
   * @param clusterName
   *          cluster name
   * @param workflowId
   *          workflow id
   * @param jobId
   *          job id
   * @param taskAttemptId
   *          task attempt id
   * 
   * @return a task attempt resource instance
   */
  ResourceInstance createTaskAttemptResource(String clusterName,
      String workflowId, String jobId, String taskAttemptId) {
    Map<Resource.Type,String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, clusterName);
    mapIds.put(Resource.Type.Workflow, workflowId);
    mapIds.put(Resource.Type.Job, jobId);
    mapIds.put(Resource.Type.TaskAttempt, taskAttemptId);
    return createResource(Resource.Type.TaskAttempt, mapIds);
  }
}
