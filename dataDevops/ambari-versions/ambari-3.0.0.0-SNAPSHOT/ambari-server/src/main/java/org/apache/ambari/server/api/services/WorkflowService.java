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
 * Service responsible for workflow resource requests.
 */
public class WorkflowService extends BaseService {
  private String clusterName;

  /**
   * Constructor.
   * 
   * @param clusterName
   *          cluster id
   */
  public WorkflowService(String clusterName) {
    this.clusterName = clusterName;
  }

  /**
   * Handles: GET /workflows/{workflowId} Get a specific workflow.
   * 
   * @param headers
   *          http headers
   * @param ui
   *          uri info
   * @param workflowId
   *          workflow id
   * @return workflow instance representation
   */
  @GET @ApiIgnore // until documented
  @Path("{workflowId}")
  @Produces("text/plain")
  public Response getWorkflow(String body, @Context HttpHeaders headers,
      @Context UriInfo ui, @PathParam("workflowId") String workflowId) {
    return handleRequest(headers, body, ui, Request.Type.GET,
        createWorkflowResource(clusterName, workflowId));
  }

  /**
   * Handles: GET /workflows Get all workflows.
   * 
   * @param headers
   *          http headers
   * @param ui
   *          uri info
   * @return workflow collection resource representation
   */
  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getWorkflows(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET,
        createWorkflowResource(clusterName, null));
  }

  /**
   * Gets the jobs sub-resource.
   */
  @Path("{workflowId}/jobs")
  public JobService getJobHandler(@PathParam("workflowId") String workflowId) {
    return new JobService(clusterName, workflowId);
  }

  /**
   * Create a workflow resource instance.
   * 
   * @param clusterName
   *          cluster name
   * @param workflowId
   *          workflow id
   * 
   * @return a workflow resource instance
   */
  ResourceInstance createWorkflowResource(String clusterName, String workflowId) {
    Map<Resource.Type,String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, clusterName);
    mapIds.put(Resource.Type.Workflow, workflowId);
    return createResource(Resource.Type.Workflow, mapIds);
  }
}
