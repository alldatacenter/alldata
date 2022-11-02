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
package org.apache.ambari.server.security.unsecured.rest;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.security.CertificateManager;
import org.apache.ambari.server.security.SignCertResponse;
import org.apache.ambari.server.security.SignMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
@Path("/certs")
public class CertificateSign {
  private static final Logger LOG = LoggerFactory.getLogger(CertificateSign.class);
  private static CertificateManager certMan;

  @Inject
  public static void init(CertificateManager instance) {
    certMan = instance;
  }

  /**
   * Signs agent certificate
   * @response.representation.200.doc This API is invoked by Ambari agent running
   *  on a cluster to register with the server.
   * @response.representation.200.mediaType application/json
   * @response.representation.406.doc Error in register message format
   * @response.representation.408.doc Request Timed out
   * @param message Register message
   * @throws Exception
   */
  @Path("{hostName}")
  @POST @ApiIgnore // until documented
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public SignCertResponse signAgentCrt(@PathParam("hostName") String hostname,
                                       SignMessage message, @Context HttpServletRequest req) {
    return certMan.signAgentCrt(hostname, message.getCsr(), message.getPassphrase());
  }
}
