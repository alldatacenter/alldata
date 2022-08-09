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

package org.apache.ambari.server.api.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.KdcServerConnectionVerification;
import org.apache.ambari.server.StaticallyInject;

import com.google.inject.Inject;

/**
 * Service responsible for kerberos related resource requests.
 */
@StaticallyInject
@Path("/kdc_check/")
public class KdcServerReachabilityCheck {
  private static final String REACHABLE = "REACHABLE";
  private static final String UNREACHABLE = "UNREACHABLE";

  @Inject
  private static KdcServerConnectionVerification kdcConnectionChecker;

  /**
   * Handles: GET /kdc_check/{hosts}
   * Checks the reachability of the given KDC server(s). If a list of hosts is sent, checks will be
   * performs until the first one succeeds, else a failure will be returned.
   *
   * @param headers      http headers
   * @param ui           uri info
   * @param hosts A comma-delimited list of host names to check for a KDC server.
   *                     Each entry may contain port separate by a colon (:)
   * @return status whether KDC server is reachable or not
   */
  @GET @ApiIgnore // until documented
  @Path("{hosts}")
  @Produces(MediaType.TEXT_PLAIN)
  public String plainTextCheck(@Context HttpHeaders headers, @Context UriInfo ui,
                               @PathParam("hosts") String hosts) {
    String status = UNREACHABLE;
    if(hosts != null) {
      String[] kdcHosts = hosts.split(",");
      for(String kdcHost : kdcHosts) {
        kdcHost = kdcHost.trim();

        if (!kdcHost.isEmpty()) {
          if (kdcConnectionChecker.isKdcReachable(kdcHost)) {
            status = REACHABLE;

            // We found a success, so break since we only care about at least one successful connection
            break;
          }
        }
      }
    }

    return status;
  }

  // This method is called if XML is request
  @GET @ApiIgnore // until documented
  @Path("{hostname}")
  @Produces(MediaType.TEXT_XML)
  public String xmlCheck(@Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("hostname") String kdcServerHostName) {
    String status = UNREACHABLE;
    if (kdcConnectionChecker.isKdcReachable(kdcServerHostName)) {
      status = REACHABLE;
    } 
    return new StringBuilder()
    .append("<?xml version=\"1.0\"?>")
    .append("<status>").append(status).append("</status>")
    .toString();
  }

  // This method is called if HTML is request
  @GET @ApiIgnore // until documented
  @Path("{hostname}")
  @Produces(MediaType.TEXT_HTML)
  public String  htmlCheck(@Context HttpHeaders headers, @Context UriInfo ui,
      @PathParam("hostname") String kdcServerHostName) {
    String status = UNREACHABLE;
    if (kdcConnectionChecker.isKdcReachable(kdcServerHostName)) {
      status = REACHABLE;
    } 
    return new StringBuilder()
    .append("<html>\n")
    .append("<title>").append("Status").append("</title>\n")
    .append("<body><h1>").append(status).append("</body></h1>\n")
    .append("</html> ")
    .toString();
  }

}
