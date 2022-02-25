package org.apache.ambari.server.api.services;

import java.util.Collections;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@Path("/kerberos_descriptors/")
public class KerberosDescriptorService extends BaseService {

  /**
   * Handles: GET  /kerberos_descriptors
   * Get all kerberos descriptors.
   *
   * @param headers http headers
   * @param ui      uri info
   * @return a collection of kerberos descriptors
   */
  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getKerberosDescriptors(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET, createKerberosDescriptorResource(null));
  }

  @GET @ApiIgnore // until documented
  @Path("{kerberosDescriptorName}")
  @Produces("text/plain")
  public Response getKerberosDescriptor(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                        @PathParam("kerberosDescriptorName") String kerberosDescriptorName) {
    return handleRequest(headers, body, ui, Request.Type.GET, createKerberosDescriptorResource(kerberosDescriptorName));
  }

  @POST @ApiIgnore // until documented
  @Path("{kerberosDescriptorName}")
  @Produces("text/plain")
  public Response createKerberosDescriptor(String body, @Context HttpHeaders headers, @Context UriInfo ui,
                                           @PathParam("kerberosDescriptorName") String kerberosDescriptorName) {
    return handleRequest(headers, body, ui, Request.Type.POST, createKerberosDescriptorResource(kerberosDescriptorName));
  }

  /**
   * Handles: DELETE /kerberos_descriptors/{kerberosDescriptorName}
   * Delete a specific kerberos descriptor.
   *
   * @param headers                http headers
   * @param ui                     uri info
   * @param kerberosDescriptorName kerebros descriptor name
   * @return information regarding the deleted kerberos descriptor
   */
  @DELETE @ApiIgnore // until documented
  @Path("{kerberosDescriptorName}")
  @Produces("text/plain")
  public Response deleteKerberosDescriptor(@Context HttpHeaders headers, @Context UriInfo ui,
                                           @PathParam("kerberosDescriptorName") String kerberosDescriptorName) {
    return handleRequest(headers, null, ui, Request.Type.DELETE, createKerberosDescriptorResource(kerberosDescriptorName));
  }

  private ResourceInstance createKerberosDescriptorResource(String kerberosDescriptorName) {
    return createResource(Resource.Type.KerberosDescriptor,
        Collections.singletonMap(Resource.Type.KerberosDescriptor, kerberosDescriptorName));
  }
}
