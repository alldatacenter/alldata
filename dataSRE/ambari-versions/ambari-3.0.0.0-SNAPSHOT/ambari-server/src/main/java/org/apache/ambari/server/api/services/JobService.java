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
 * Service responsible for job resource requests.
 */
public class JobService extends BaseService {
  private String workflowId;
  private String clusterName;

  /**
   * Constructor.
   * 
   * @param clusterName
   *          cluster name
   * @param workflowId
   *          workflow id
   */
  public JobService(String clusterName, String workflowId) {
    this.clusterName = clusterName;
    this.workflowId = workflowId;
  }

  /**
   * Handles: GET /workflows/{workflowId}/jobs/{jobId} Get a specific job.
   * 
   * @param headers
   *          http headers
   * @param ui
   *          uri info
   * @param jobId
   *          job id
   * @return job instance representation
   */
  @GET @ApiIgnore // until documented
  @Path("{jobId}")
  @Produces("text/plain")
  public Response getJob(String body, @Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("jobId") String jobId) {
    return handleRequest(headers, body, ui, Request.Type.GET,
        createJobResource(clusterName, workflowId, jobId));
  }

  /**
   * Handles: GET /workflows/{workflowId}/jobs Get all jobs.
   * 
   * @param headers
   *          http headers
   * @param ui
   *          uri info
   * @return job collection resource representation
   */
  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getJobs(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET,
        createJobResource(clusterName, workflowId, null));
  }

  /**
   * Gets the task attempts sub-resource.
   */
  @Path("{jobId}/taskattempts")
  public TaskAttemptService getTaskAttemptHandler(
      @PathParam("jobId") String jobId) {
    return new TaskAttemptService(clusterName, workflowId, jobId);
  }

  /**
   * Create a job resource instance.
   * 
   * @param clusterName
   *          cluster name
   * @param workflowId
   *          workflow id
   * @param jobId
   *          job id
   * 
   * @return a job resource instance
   */
  ResourceInstance createJobResource(String clusterName, String workflowId,
      String jobId) {
    Map<Resource.Type,String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, clusterName);
    mapIds.put(Resource.Type.Workflow, workflowId);
    mapIds.put(Resource.Type.Job, jobId);
    return createResource(Resource.Type.Job, mapIds);
  }
}
