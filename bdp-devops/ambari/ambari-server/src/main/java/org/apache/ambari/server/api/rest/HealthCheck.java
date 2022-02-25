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
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.ambari.annotations.ApiIgnore;

/**
 * A simple POJO to do a health check on the server to see if its running
 * or not
 */

@Path("/check")
public class HealthCheck {
  private static final String status = "RUNNING";
  // This method is called if TEXT_PLAIN is request

  @GET @ApiIgnore // until documented
  @Produces(MediaType.TEXT_PLAIN)
  public String plainTextCheck() {
    return status;
  }

  // This method is called if XML is request
  @GET @ApiIgnore // until documented
  @Produces(MediaType.TEXT_XML)
  public String xmlCheck() {
    return "<?xml version=\"1.0\"?>" + "<status> " + status + "</status>";
  }

  // This method is called if HTML is request
  @GET @ApiIgnore // until documented
  @Produces(MediaType.TEXT_HTML)
  public String  htmlCheck() {
    return "<html> " + "<title>" + "Status" + "</title>"
        + "<body><h1>" + status + "</body></h1>" + "</html> ";
  }
}

