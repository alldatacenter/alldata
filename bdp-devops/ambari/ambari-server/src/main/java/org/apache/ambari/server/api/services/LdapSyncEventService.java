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
 * Service responsible for ldap sync event resource requests.
 */
@Path("/ldap_sync_events/")
public class LdapSyncEventService extends BaseService {
  /**
   * Handles: GET /ldap_sync_events/{eventId}
   * Get a specific view.
   *
   * @param headers   http headers
   * @param ui        uri info
   * @param eventId   event id
   *
   * @return view instance representation
   */
  @GET @ApiIgnore // until documented
  @Path("{eventId}")
  @Produces("text/plain")
  public Response getEvent(@Context HttpHeaders headers, @Context UriInfo ui,
                          @PathParam("eventId") String eventId) {

    return handleRequest(headers, null, ui, Request.Type.GET, createEventResource(eventId));
  }

  /**
   * Handles: GET  /ldap_sync_events
   * Get all events.
   *
   * @param headers  http headers
   * @param ui       uri info
   *
   * @return view collection resource representation
   */
  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getEvents(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET, createEventResource(null));
  }

  /**
   * Handles: POST /ldap_sync_events/
   * Create an event.
   *
   * @param headers    http headers
   * @param ui         uri info
   *
   * @return information regarding the created view
   */
  @POST @ApiIgnore // until documented
  @Produces("text/plain")
  public Response createEvent(String body, @Context HttpHeaders headers, @Context UriInfo ui) {

    return handleRequest(headers, body, ui, Request.Type.POST, createEventResource(null));
  }

  /**
   * Handles: PUT /ldap_sync_events/{eventId}
   * Update a specific event.
   *
   * @param headers  http headers
   * @param ui       uri info
   * @param eventId  event id
   *
   * @return information regarding the updated event
   */
  @PUT @ApiIgnore // until documented
  @Path("{eventId}")
  @Produces("text/plain")
  public Response updateEvent(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("eventId") String eventId) {

    return handleRequest(headers, body, ui, Request.Type.PUT, createEventResource(eventId));
  }

  /**
   * Handles: DELETE /ldap_sync_events/{eventId}
   * Delete a specific view.
   *
   * @param headers  http headers
   * @param ui       uri info
   * @param eventId  event id
   *
   * @return information regarding the deleted event
   */
  @DELETE @ApiIgnore // until documented
  @Path("{eventId}")
  @Produces("text/plain")
  public Response deleteEvent(@Context HttpHeaders headers, @Context UriInfo ui,
                             @PathParam("eventId") String eventId) {

    return handleRequest(headers, null, ui, Request.Type.DELETE, createEventResource(eventId));
  }

  // ----- helper methods ----------------------------------------------------

  /**
   * Create a view resource.
   *
   * @param eventId the event id
   *
   * @return an event resource instance
   */
  private ResourceInstance createEventResource(String eventId) {
    return createResource(Resource.Type.LdapSyncEvent,
        Collections.singletonMap(Resource.Type.LdapSyncEvent, eventId));
  }
}
