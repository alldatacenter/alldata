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
 * Service responsible for instances resource requests.
 */
@Path("/instances/")
public class InstanceService extends BaseService {

  /**
   * Parent feed id.
   */
  private String m_feedName;

  /**
   * Constructor.
   */
  public InstanceService() {
  }

  /**
   * Constructor.
   *
   * @param feedName feed id
   */
  public InstanceService(String feedName) {
    m_feedName = feedName;
  }

  /**
   * Handles GET /feeds/{feedID}/instances/{instanceID} and /instances/{instanceID}
   * Get a specific instance.
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param instanceID  instance id
   *
   * @return instance resource representation
   */
  @GET @ApiIgnore // until documented
  @Path("{instanceID}")
  @Produces("text/plain")
  public Response getInstance(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                          @PathParam("instanceID") String instanceID) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createInstanceResource(m_feedName, instanceID, ui));
  }

  /**
   * Handles GET /feeds/{feedID}/instances and /instances
   * Get all instances for a feed.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return instance collection resource representation
   */
  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getInstances(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET,
        createInstanceResource(m_feedName, null, ui));
  }

  /**
   * Handles POST /feeds/{feedID}/instances/{instanceID}
   * Create a specific instance.
   *
   * @param body     http body
   * @param headers  http headers
   * @param ui       uri info
   * @param instanceID instance id
   *
   * @return instance resource representation
   */
  @POST @ApiIgnore // until documented
  @Path("{instanceID}")
  @Produces("text/plain")
  public Response createInstance(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("instanceID") String instanceID) {

    return handleRequest(headers, body, ui, Request.Type.POST,
        createInstanceResource(m_feedName, instanceID, ui));
  }

  /**
   * Handles PUT /feeds/{feedID}/instances/{instanceID}
   * Updates a specific instance.
   *
   * @param body     http body
   * @param headers  http headers
   * @param ui       uri info
   * @param instanceID instance id
   *
   * @return information regarding updated instance
   */
  @PUT @ApiIgnore // until documented
  @Path("{instanceID}")
  @Produces("text/plain")
  public Response updateInstance(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("instanceID") String instanceID) {

    return handleRequest(headers, body, ui, Request.Type.PUT,
        createInstanceResource(m_feedName, instanceID, ui));
  }

  /**
   * Handles DELETE /feeds/{feedID}/instances/{instanceID}
   * Deletes a specific instance.
   *
   * @param headers  http headers
   * @param ui       uri info
   * @param instanceID instance id
   *
   * @return instance resource representation
   */
  @DELETE @ApiIgnore // until documented
  @Path("{instanceID}")
  @Produces("text/plain")
  public Response deleteInstance(@Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("instanceID") String instanceID) {

    return handleRequest(headers, null, ui, Request.Type.DELETE,
        createInstanceResource(m_feedName, instanceID, ui));
  }

  /**
   * Create a service resource instance.
   *
   *
   *
   * @param feedName  feed
   * @param instanceID     instance name
   * @param ui           uri information
   *
   * @return a instance resource instance
   */
  ResourceInstance createInstanceResource(String feedName, String instanceID, UriInfo ui) {
    boolean isAttached = ui.getRequestUri().toString().contains("/feeds/");

    Map<Resource.Type,String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.DRInstance, instanceID);
    if (isAttached) {
      mapIds.put(Resource.Type.DRFeed, feedName);
    }

    return createResource(Resource.Type.DRInstance, mapIds);
  }
}
