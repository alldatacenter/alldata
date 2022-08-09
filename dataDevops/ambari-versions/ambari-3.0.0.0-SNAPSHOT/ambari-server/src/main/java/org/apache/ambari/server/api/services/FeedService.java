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

import java.util.Collections;

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
 * DR feed service.
 */
@Path("/feeds/")
public class FeedService extends BaseService {

  /**
   * Handles: GET /feeds/{feedName}
   * Get a specific feed.
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param feedName    feed id
   * @return feed instance representation
   */
  @GET @ApiIgnore // until documented
  @Path("{feedName}")
  @Produces("text/plain")
  public Response getFeed(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("feedName") String feedName) {

    return handleRequest(headers, body, ui, Request.Type.GET, createFeedResource(feedName));
  }

  /**
   * Handles: GET  /feeds
   * Get all feeds.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return feed collection resource representation
   */
  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getFeeds(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET, createFeedResource(null));
  }

  /**
   * Handles: POST /feeds/{feedName}
   * Create a specific feed.
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param feedName feed id
   * @return information regarding the created feed
   */
  @POST @ApiIgnore // until documented
  @Path("{feedName}")
  @Produces("text/plain")
  public Response createFeed(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("feedName") String feedName) {

    return handleRequest(headers, body, ui, Request.Type.POST, createFeedResource(feedName));
  }

  /**
   * Handles: PUT /feeds/{feedName}
   * Update a specific feed.
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param feedName feed id
   * @return information regarding the updated feed
   */
  @PUT @ApiIgnore // until documented
  @Path("{feedName}")
  @Produces("text/plain")
  public Response updateFeed(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("feedName") String feedName) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createFeedResource(feedName));
  }

  /**
   * Handles: DELETE /feeds/{feedName}
   * Delete a specific feed.
   *
   * @param headers     http headers
   * @param ui          uri info
   * @param feedName feed id
   * @return information regarding the deleted feed
   */
  @DELETE @ApiIgnore // until documented
  @Path("{feedName}")
  @Produces("text/plain")
  public Response deleteFeed(@Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("feedName") String feedName) {

    return handleRequest(headers, null, ui, Request.Type.DELETE, createFeedResource(feedName));
  }

  /**
   * Get the instances sub-resource
   *
   * @param feedName feed id
   * @return the instances service
   */
  @Path("{feedName}/instances")
  public InstanceService getHostHandler(@PathParam("feedName") String feedName) {
    return new InstanceService(feedName);
  }

  /**
   * Create a feed resource instance.
   *
   * @param feedName feed name
   *
   * @return a feed resource instance
   */
  ResourceInstance createFeedResource(String feedName) {
    return createResource(Resource.Type.DRFeed,
        Collections.singletonMap(Resource.Type.DRFeed, feedName));
  }
}
