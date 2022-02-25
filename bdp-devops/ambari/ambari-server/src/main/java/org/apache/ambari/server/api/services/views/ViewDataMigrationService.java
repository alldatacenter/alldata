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
package org.apache.ambari.server.api.services.views;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.ambari.server.api.services.BaseService;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.view.ViewDataMigrationUtility;
import org.apache.ambari.server.view.ViewRegistry;
import org.apache.ambari.view.migration.ViewDataMigrationException;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Service responsible for data migration between view instances.
 */
@Path("/views/{viewName}/versions/{version}/instances/{instanceName}/migrate")
@Api(tags = "Views", description = "Endpoint for view specific operations")
public class ViewDataMigrationService extends BaseService {
  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(ViewDataMigrationService.class);

  /**
   * The singleton view registry.
   */
  private ViewRegistry viewRegistry = ViewRegistry.getInstance();

  /**
   * The view data migration utility.
   */
  private ViewDataMigrationUtility viewDataMigrationUtility;

  /**
   * Migrates view instance persistence data from origin view instance
   * specified in the path params.
   *
   * @param viewName           view id
   * @param viewVersion        version id
   * @param instanceName       instance id
   * @param originViewVersion  the origin view version
   * @param originInstanceName the origin view instance name
   */
  @PUT
  @Path("{originVersion}/{originInstanceName}")
  @ApiOperation(value = "Migrate view instance data", notes = "Migrates view instance persistence data from origin view instance specified in the path params.")
  @ApiResponses({
    @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
    @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_CLUSTER_NOT_FOUND),
    @ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = MSG_NOT_AUTHENTICATED),
    @ApiResponse(code = HttpStatus.SC_FORBIDDEN, message = MSG_PERMISSION_DENIED),
    @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR),
    @ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = MSG_INVALID_ARGUMENTS),
  })
  public Response migrateData( @ApiParam(value = "view name") @PathParam("viewName") String viewName,
                               @ApiParam(value = "view version") @PathParam("version") String viewVersion,
                               @ApiParam(value = "instance name") @PathParam("instanceName") String instanceName,
                               @ApiParam(value = "origin version") @PathParam("originVersion") String originViewVersion,
                               @ApiParam(value = "origin instance name") @PathParam("originInstanceName") String originInstanceName)
      throws ViewDataMigrationException {

    if (!viewRegistry.checkAdmin()) {
      throw new WebApplicationException(Response.Status.FORBIDDEN);
    }

    LOG.info("Data Migration to view instance " + viewName + "/" + viewVersion + "/" + instanceName +
        " from " + viewName + "/" + originViewVersion + "/" + originInstanceName);

    ViewInstanceEntity instanceDefinition = viewRegistry.getInstanceDefinition(
        viewName, viewVersion, instanceName);
    ViewInstanceEntity originInstanceDefinition = viewRegistry.getInstanceDefinition(
        viewName, originViewVersion, originInstanceName);

    getViewDataMigrationUtility().migrateData(instanceDefinition, originInstanceDefinition, false);

    Response.ResponseBuilder builder = Response.status(Response.Status.OK);
    return builder.build();
  }

  protected ViewDataMigrationUtility getViewDataMigrationUtility() {
    if (viewDataMigrationUtility == null) {
      viewDataMigrationUtility = new ViewDataMigrationUtility(viewRegistry);
    }
    return viewDataMigrationUtility;
  }

  protected void setViewDataMigrationUtility(ViewDataMigrationUtility viewDataMigrationUtility) {
    this.viewDataMigrationUtility = viewDataMigrationUtility;
  }
}
