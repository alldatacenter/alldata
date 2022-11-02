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
 * Read-only service responsible for Kerberos descriptor resource requests.
 * <p/>
 * The following Kerberos descriptor types are available
 * <ul>
 * <li>STACK - the default descriptor for the relevant stack</li>
 * <li>USER - the user-supplied updates to be applied to the default descriptor</li>
 * <li>COMPOSITE - the default descriptor for the relevant stack with the the user-supplied updates applied</li>
 * </ul>
 */
public class ClusterKerberosDescriptorService extends BaseService {
  /**
   * Parent cluster name.
   */
  private String clusterName;

  /**
   * Constructor.
   *
   * @param clusterName cluster id
   */
  public ClusterKerberosDescriptorService(String clusterName) {
    this.clusterName = clusterName;
  }

  /**
   * Handles URL: /clusters/{clusterID}/kerberos_descriptors
   * Gets Kerberos descriptors
   *
   * @param headers http headers
   * @param ui      uri info
   * @return Kerberos descriptor resource representation
   */
  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getKerberosDescriptors(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET, createResource(null));
  }

  /**
   * Handles URL: /clusters/{clusterID}/kerberos_descriptors
   * Get a specific composite Kerberos descriptor
   *
   * @param headers http headers
   * @param ui      uri info
   * @param type    Kerberos descriptor type (COMPOSITE, STACK, USER)
   * @return Kerberos descriptor instance representation
   */
  @GET @ApiIgnore // until documented
  @Path("{type}")
  @Produces("text/plain")
  public Response getKerberosDescriptor(@Context HttpHeaders headers, @Context UriInfo ui,
                                @PathParam("type") String type) {
    return handleRequest(headers, null, ui, Request.Type.GET, createResource(type));
  }

  /**
   * Create a composite Kerberos Descriptor resource instance.
   *
   * @param type the Kerberos descriptor type (COMPOSITE, STACK, USER)
   * @return a service resource instance
   */
  ResourceInstance createResource(String type) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Cluster, clusterName);
    mapIds.put(Resource.Type.ClusterKerberosDescriptor, type);
    return createResource(Resource.Type.ClusterKerberosDescriptor, mapIds);
  }
}
