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
package org.apache.ambari.server.resources.api.rest;

import java.io.File;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * Resource api.
 */
@Path("/")
public class GetResource {
  private static final Logger LOG = LoggerFactory.getLogger(GetResource.class);

  private static ResourceManager resourceManager;

  @Inject
  public static void init(ResourceManager instance) {
	  resourceManager = instance;
  }

  @GET @ApiIgnore // until documented
  @Path("{resourcePath:.*}")
  @Consumes(MediaType.TEXT_PLAIN)
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response getResource(@PathParam("resourcePath") String resourcePath,
      @Context HttpServletRequest req) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Received a resource request from agent, resourcePath={}", resourcePath);
    }
    File resourceFile = resourceManager.getResource(resourcePath);

    if (!resourceFile.exists()) {
    	return Response.status(HttpServletResponse.SC_NOT_FOUND).build();
    }

    return Response.ok(resourceFile).build();
  }
}
