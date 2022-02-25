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

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.annotations.ApiIgnore;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.spi.Resource;

/**
 * Service responsible for cluster pre-upgrade checks.
 */
public class PreUpgradeCheckService extends BaseService {

  /**
   * Cluster name.
   */
  private String clusterName = null;

  /**
   * Constructor.
   *
   * @param clusterName cluster name
   */
  public PreUpgradeCheckService(String clusterName) {
    this.clusterName = clusterName;
  }

  /**
   * Handles GET /rolling_upgrades_check request.
   *
   * @param headers headers
   * @param ui uri info
   * @return information about upgrade checks
   */
  @GET @ApiIgnore // until documented
  @Produces("text/plain")
  public Response getPreUpgradeChecks(@Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, null, ui, Request.Type.GET, createResource());
  }

  /**
   * Creates an upgrade check resource instance.
   *
   * @return an upgrade check resource instance
   */
  private ResourceInstance createResource() {
    return createResource(Resource.Type.PreUpgradeCheck, Collections.singletonMap(Resource.Type.Cluster, clusterName));
  }
}
