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
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.controller.ComponentDependencyResponse;
import org.apache.ambari.server.controller.ExtensionLinkResponse;
import org.apache.ambari.server.controller.QuickLinksResponse;
import org.apache.ambari.server.controller.StackArtifactResponse;
import org.apache.ambari.server.controller.StackConfigurationDependencyResponse;
import org.apache.ambari.server.controller.StackConfigurationResponse;
import org.apache.ambari.server.controller.StackResponse;
import org.apache.ambari.server.controller.StackServiceArtifactResponse;
import org.apache.ambari.server.controller.StackServiceComponentResponse;
import org.apache.ambari.server.controller.StackServiceResponse;
import org.apache.ambari.server.controller.StackVersionResponse;
import org.apache.ambari.server.controller.ThemeResponse;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.http.HttpStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;


@Path("/stacks/")
@Api(value = "Stacks", description = "Endpoint for stack specific operations")
public class StacksService extends BaseService {

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all stacks",
      nickname = "StacksService#getStacks",
      notes = "Returns all stacks.",
      response = StackResponse.StackResponseSwagger.class,
      responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = "Filter stack details", defaultValue = "Stacks/stack_name", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_SORT, value = "Sort stack privileges (asc | desc)", defaultValue = "Stacks/stack_name.asc", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getStacks(String body, @Context HttpHeaders headers, @Context UriInfo ui) {
    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackResource(null));
  }

  @GET
  @Path("{stackName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get a stack", nickname = "StacksService#getStack", notes = "Returns stack details.",
      response = StackResponse.StackResponseSwagger.class, responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = "Filter stack details",
          defaultValue = "Stacks/*", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getStack(String body, @Context HttpHeaders headers,
                           @Context UriInfo ui,
                           @ApiParam @PathParam("stackName") String stackName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackResource(stackName));
  }

  @GET
  @Path("{stackName}/versions")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all versions for a stacks",
      nickname = "StacksService#getStackVersions",
      notes = "Returns all versions for a stack.",
      response = StackVersionResponse.StackVersionResponseSwagger.class,
      responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = "Filter stack version details",
          defaultValue = "Versions/stack_name,Versions/stack_version",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_SORT, value = "Sort stack privileges (asc | desc)",
          defaultValue = "Versions/stack_name.asc,Versions/stack_version.asc",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getStackVersions(String body,
                                   @Context HttpHeaders headers,
                                   @Context UriInfo ui,
                                   @ApiParam @PathParam("stackName") String stackName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackVersionResource(stackName, null));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get details for a stack version",
      nickname = "StacksService#getStackVersion",
      notes = "Returns the details for a stack version.",
      response = StackVersionResponse.StackVersionResponseSwagger.class)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = "Filter stack version details",
          defaultValue = "Versions/*", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getStackVersion(String body,
                                  @Context HttpHeaders headers,
                                  @Context UriInfo ui,
                                  @ApiParam @PathParam("stackName") String stackName,
                                  @ApiParam @PathParam("stackVersion") String stackVersion) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackVersionResource(stackName, stackVersion));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/links")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get extension links for a stack version",
      nickname = "StacksService#getStackVersionLinks",
      notes = "Returns the extension links for a stack version.",
      response = ExtensionLinkResponse.class,
      responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = "Filter extension link attributes",
          defaultValue = "ExtensionLink/link_id," +
              "ExtensionLink/stack_name," +
              "ExtensionLink/stack_version," +
              "ExtensionLink/extension_name," +
              "ExtensionLink/extension_version", dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_SORT, value = "Sort extension links (asc | desc)",
          defaultValue = "ExtensionLink/link_id.asc," +
              "ExtensionLink/stack_name.asc," +
              "ExtensionLink/stack_version.asc," +
              "ExtensionLink/extension_name.asc," +
              "ExtensionLink/extension_version.asc",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)

  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getStackVersionLinks(String body,
                                  @Context HttpHeaders headers,
                                  @Context UriInfo ui,
                                  @ApiParam @PathParam("stackName") String stackName,
                                  @ApiParam @PathParam("stackVersion") String stackVersion) {
    return handleRequest(headers, body, ui, Request.Type.GET,
        createExtensionLinkResource(stackName, stackVersion, null, null));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/configurations")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all configurations for a stack version",
      nickname = "StacksService#getStackLevelConfigurations",
      notes = "Returns all configurations for a stack version.",
      response = StackConfigurationResponse.StackConfigurationResponseSwagger.class,
      responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = "Filter returned attributes",
          defaultValue = "StackLevelConfigurations/stack_name," +
            "StackLevelConfigurations/stack_version," +
            "StackLevelConfigurations/property_name",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_SORT, value = "Sort configuration (asc | desc)",
          defaultValue = "StackLevelConfigurations/stack_name.asc," +
              "StackLevelConfigurations/stack_version.asc," +
              "StackLevelConfigurations/property_name.asc ",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)

  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getStackLevelConfigurations(String body, @Context HttpHeaders headers,
                                   @Context UriInfo ui,
                                   @ApiParam @PathParam("stackName") String stackName,
                                   @ApiParam @PathParam("stackVersion") String stackVersion) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackLevelConfigurationsResource(stackName, stackVersion, null));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/configurations/{propertyName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get configuration details for a given property",
      nickname = "StacksService#getStackLevelConfiguration",
      notes = "Returns the configuration details for a given property.",
      response = StackConfigurationResponse.StackConfigurationResponseSwagger.class)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = "Filter returned attributes",
          defaultValue = "StackLevelConfigurations/*",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getStackLevelConfiguration(String body, @Context HttpHeaders headers,
                                        @Context UriInfo ui,
                                        @ApiParam @PathParam("stackName") String stackName,
                                        @ApiParam @PathParam("stackVersion") String stackVersion,
                                        @ApiParam @PathParam("serviceName") String serviceName,
                                        @ApiParam @PathParam("propertyName") String propertyName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackLevelConfigurationsResource(stackName, stackVersion, propertyName));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/services")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all services for a stack version",
      nickname = "StacksService#getStackServices",
      notes = "Returns all services for a stack version.",
      response = StackServiceResponse.StackServiceResponseSwagger.class,
      responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = "Filter returned attributes",
          defaultValue = "StackServices/stack_name," +
              "StackServices/stack_version," +
              "StackServices/service_name",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_SORT, value = "Sort stack services (asc | desc)",
          defaultValue = "StackServices/stack_name.asc," +
              "StackServices/stack_version.asc," +
              "StackServices/service_name.asc",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getStackServices(String body, @Context HttpHeaders headers,
                                   @Context UriInfo ui,
                                   @ApiParam @PathParam("stackName") String stackName,
                                   @ApiParam @PathParam("stackVersion") String stackVersion) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackServiceResource(stackName, stackVersion, null));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get stack service details",
      nickname = "StacksService#getStackService",
      notes = "Returns the details of a stack service.",
      response = StackServiceResponse.StackServiceResponseSwagger.class)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = "Filter returned attributes",
          defaultValue = "StackServices/*",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getStackService(String body, @Context HttpHeaders headers,
                                  @Context UriInfo ui,
                                  @ApiParam @PathParam("stackName") String stackName,
                                  @ApiParam @PathParam("stackVersion") String stackVersion,
                                  @ApiParam @PathParam("serviceName") String serviceName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackServiceResource(stackName, stackVersion, serviceName));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/artifacts")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all stack artifacts",
      nickname = "StacksService#getStackArtifacts",
      notes = "Returns all stack artifacts (e.g: kerberos descriptor, metrics descriptor)",
      response = StackArtifactResponse.class,
      responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = "Filter returned attributes",
          defaultValue = "Artifacts/artifact_name," +
              "Artifacts/stack_name," +
              "Artifacts/stack_version",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getStackArtifacts(String body, @Context HttpHeaders headers,
                                              @Context UriInfo ui,
                                              @ApiParam @PathParam("stackName") String stackName,
                                              @ApiParam @PathParam("stackVersion") String stackVersion) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackArtifactsResource(stackName, stackVersion, null));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/artifacts/{artifactName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get stack artifact details",
      nickname = "StacksService#getStackArtifact",
      notes = "Returns the details of a stack artifact",
      response = StackArtifactResponse.class)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = "Filter returned attributes",
          defaultValue = "Artifacts/*",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getStackArtifact(String body, @Context HttpHeaders headers,
                                   @Context UriInfo ui, @PathParam("stackName") String stackName,
                                   @ApiParam @PathParam("stackVersion") String stackVersion,
                                   @ApiParam @PathParam("artifactName") String artifactName) {
    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackArtifactsResource(stackName, stackVersion, artifactName));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}/artifacts")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all artifacts for a stack service",
      nickname = "StacksService#getStackServiceArtifacts",
      notes = "Returns all stack service artifacts",
      response = StackServiceArtifactResponse.class,
      responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = "Filter returned attributes",
          defaultValue = "Artifacts/artifact_name," +
              "Artifacts/stack_name," +
              "Artifacts/stack_version",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_SORT, value = "Sort service artifacts (asc | desc)",
          defaultValue = "Artifacts/artifact_name.asc," +
              "Artifacts/stack_name.asc," +
              "Artifacts/stack_version.asc",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getStackServiceArtifacts(String body, @Context HttpHeaders headers,
                                  @Context UriInfo ui,
                                  @ApiParam @PathParam("stackName") String stackName,
                                  @ApiParam @PathParam("stackVersion") String stackVersion,
                                  @ApiParam @PathParam("serviceName") String serviceName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackServiceArtifactsResource(stackName, stackVersion, serviceName, null));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}/themes")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all themes for a stack service",
      nickname = "StacksService#getStackServiceThemes",
      notes = "Returns all stack themes",
      response = ThemeResponse.class,
      responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = "Filter returned attributes",
          defaultValue = "ThemeInfo/file_name," +
              "ThemeInfo/service_name," +
              "ThemeInfo/stack_name," +
              "ThemeInfo/stack_version",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_SORT, value = "Sort service artifacts (asc | desc)",
          defaultValue = "ThemeInfo/file_name.asc," +
              "ThemeInfo/service_name.asc," +
              "ThemeInfo/stack_name.asc," +
              "ThemeInfo/stack_version.asc",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getStackServiceThemes(String body, @Context HttpHeaders headers,
                                           @Context UriInfo ui,
                                           @ApiParam @PathParam("stackName") String stackName,
                                           @ApiParam @PathParam("stackVersion") String stackVersion,
                                           @ApiParam @PathParam("serviceName") String serviceName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
      createStackServiceThemesResource(stackName, stackVersion, serviceName, null));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}/themes/{themeName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get theme details for a stack service",
      nickname = "StacksService#getStackServiceTheme",
      notes = "Returns stack service theme details.",
      response = ThemeResponse.class)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = "Filter returned attributes",
          defaultValue = "ThemeInfo/*",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getStackServiceTheme(String body, @Context HttpHeaders headers,
                                           @Context UriInfo ui,
                                           @ApiParam @PathParam("stackName") String stackName,
                                           @ApiParam @PathParam("stackVersion") String stackVersion,
                                           @ApiParam @PathParam("serviceName") String serviceName,
                                           @ApiParam @PathParam("themeName") String themeName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
      createStackServiceThemesResource(stackName, stackVersion, serviceName, themeName));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}/quicklinks")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all quicklinks configurations for a stack service",
      nickname = "StacksService#getStackServiceQuickLinksConfigurations",
      notes = "Returns all quicklinks configurations for a stack service.",
      response = QuickLinksResponse.class,
      responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = "Filter returned attributes",
          defaultValue = "QuickLinkInfo/file_name," +
              "QuickLinkInfo/service_name," +
              "QuickLinkInfo/stack_name," +
              "QuickLinkInfo/stack_version",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_SORT, value = "Sort quick links (asc | desc)",
          defaultValue = "QuickLinkInfo/file_name.asc," +
              "QuickLinkInfo/service_name.asc," +
              "QuickLinkInfo/stack_name.asc," +
              "QuickLinkInfo/stack_version.asc",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getStackServiceQuickLinksConfigurations(String body, @Context HttpHeaders headers,
                                           @Context UriInfo ui,
                                           @ApiParam @PathParam("stackName") String stackName,
                                           @ApiParam @PathParam("stackVersion") String stackVersion,
                                           @ApiParam @PathParam("serviceName") String serviceName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
      createStackServiceQuickLinksResource(stackName, stackVersion, serviceName, null));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}/quicklinks/{quickLinksConfigurationName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get quicklinks configuration details",
      nickname = "StacksService#getStackServiceQuickLinksConfiguration",
      notes = "Returns the details of a quicklinks configuration.",
      response = QuickLinksResponse.class,
      responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = "Filter returned attributes",
          defaultValue = "QuickLinkInfo/*",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getStackServiceQuickLinksConfiguration(String body, @Context HttpHeaders headers,
                                           @Context UriInfo ui,
                                           @ApiParam @PathParam("stackName") String stackName,
                                           @ApiParam @PathParam("stackVersion") String stackVersion,
                                           @ApiParam @PathParam("serviceName") String serviceName,
                                           @ApiParam @PathParam("quickLinksConfigurationName") String quickLinksConfigurationName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
      createStackServiceQuickLinksResource(stackName, stackVersion, serviceName, quickLinksConfigurationName));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}/artifacts/{artifactName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get stack service artifact details",
      nickname = "StacksService#getStackServiceArtifact",
      notes = "Returns the details of a stack service artifact.",
      response = StackArtifactResponse.class
  )
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = "Filter returned attributes",
          defaultValue = "Artifacts/*",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getStackServiceArtifact(String body, @Context HttpHeaders headers,
                                           @Context UriInfo ui,
                                           @ApiParam @PathParam("stackName") String stackName,
                                           @ApiParam @PathParam("stackVersion") String stackVersion,
                                           @ApiParam @PathParam("serviceName") String serviceName,
                                           @ApiParam @PathParam("artifactName") String artifactName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackServiceArtifactsResource(stackName, stackVersion, serviceName, artifactName));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}/configurations")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all configurations for a stack service",
      nickname = "StacksService#getStackConfigurations",
      notes = "Returns all configurations for a stack service.",
      response = StackConfigurationResponse.StackConfigurationResponseSwagger.class,
      responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = "Filter returned attributes",
          defaultValue = "StackConfigurations/property_name," +
              "StackConfigurations/service_name," +
              "StackConfigurations/stack_name" +
              "StackConfigurations/stack_version",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_SORT, value = "Sort service configurations (asc | desc)",
          defaultValue = "StackConfigurations/property_name.asc," +
              "StackConfigurations/service_name.asc," +
              "StackConfigurations/stack_name.asc" +
              "StackConfigurations/stack_version.asc",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getStackConfigurations(String body,
                                         @Context HttpHeaders headers,
                                         @Context UriInfo ui,
                                         @ApiParam @PathParam("stackName") String stackName,
                                         @ApiParam @PathParam("stackVersion") String stackVersion,
                                         @ApiParam @PathParam("serviceName") String serviceName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackConfigurationResource(stackName, stackVersion, serviceName, null));
  }


  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}/configurations/{propertyName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get stack service configuration details",
      nickname = "StacksService#getStackConfiguration",
      notes = "Returns the details of a stack service configuration.",
      response = StackConfigurationResponse.StackConfigurationResponseSwagger.class)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = "Filter returned attributes",
          defaultValue = "StackConfigurations/*",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getStackConfiguration(String body, @Context HttpHeaders headers,
                                        @Context UriInfo ui,
                                        @ApiParam @PathParam("stackName") String stackName,
                                        @ApiParam @PathParam("stackVersion") String stackVersion,
                                        @ApiParam @PathParam("serviceName") String serviceName,
                                        @ApiParam @PathParam("propertyName") String propertyName) {
    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackConfigurationResource(stackName, stackVersion, serviceName, propertyName));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}/configurations/{propertyName}/dependencies")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all dependencies for a stack service configuration",
      nickname = "StacksService#getStackConfigurationDependencies",
      notes = "Returns all dependencies for a stack service configuration.",
      response = StackConfigurationDependencyResponse.StackConfigurationDependencyResponseSwagger.class,
      responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = "Filter returned attributes",
          defaultValue = "StackConfigurationDependency/stack_name," +
              "StackConfigurationDependency/stack_version," +
              "StackConfigurationDependency/service_name," +
              "StackConfigurationDependency/property_name," +
              "StackConfigurationDependency/dependency_name",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_SORT, value = "Sort configuration dependencies (asc | desc)",
          defaultValue = "StackConfigurationDependency/stack_name.asc," +
              "StackConfigurationDependency/stack_version.asc," +
              "StackConfigurationDependency/service_name.asc," +
              "StackConfigurationDependency/property_name.asc," +
              "StackConfigurationDependency/dependency_name.asc",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getStackConfigurationDependencies(String body, @Context HttpHeaders headers,
                                        @Context UriInfo ui,
                                        @ApiParam @PathParam("stackName") String stackName,
                                        @ApiParam @PathParam("stackVersion") String stackVersion,
                                        @ApiParam @PathParam("serviceName") String serviceName,
                                        @ApiParam @PathParam("propertyName") String propertyName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackConfigurationDependencyResource(stackName, stackVersion, serviceName, propertyName));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}/components")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all components for a stack service",
      nickname = "StacksService#getServiceComponents",
      notes = "Returns all components for a stack service.",
      response = StackServiceComponentResponse.StackServiceComponentResponseSwagger.class,
      responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = "Filter returned attributes",
          defaultValue = "StackServiceComponents/component_name," +
              "StackServiceComponents/service_name," +
              "StackServiceComponents/stack_name," +
              "StackServiceComponents/stack_version",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_SORT, value = "Sort service components (asc | desc)",
          defaultValue = "StackServiceComponents/component_name.asc," +
              "StackServiceComponents/service_name.asc," +
              "StackServiceComponents/stack_name.asc," +
              "StackServiceComponents/stack_version.asc",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getServiceComponents(String body,
                                       @Context HttpHeaders headers,
                                       @Context UriInfo ui,
                                       @ApiParam @PathParam("stackName") String stackName,
                                       @ApiParam @PathParam("stackVersion") String stackVersion,
                                       @ApiParam @PathParam("serviceName") String serviceName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackServiceComponentResource(stackName, stackVersion, serviceName, null));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}/components/{componentName}/dependencies")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get all dependencies for a stack service component",
      nickname = "StacksService#getServiceComponentDependencies",
      notes = "Returns all dependencies for a stack service component.",
      response = ComponentDependencyResponse.class,
      responseContainer = RESPONSE_CONTAINER_LIST)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = "Filter returned attributes",
          defaultValue = "Dependencies/stack_name," +
              "Dependencies/stack_version," +
              "Dependencies/dependent_service_name," +
              "Dependencies/dependent_component_name," +
              "Dependencies/component_name",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_SORT, value = "Sort component dependencies (asc | desc)",
          defaultValue = "Dependencies/stack_name.asc," +
              "Dependencies/stack_version.asc," +
              "Dependencies/dependent_service_name.asc," +
              "Dependencies/dependent_component_name.asc," +
              "Dependencies/component_name.asc",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_PAGE_SIZE, value = QUERY_PAGE_SIZE_DESCRIPTION, defaultValue = DEFAULT_PAGE_SIZE, dataType = DATA_TYPE_INT, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_FROM, value = QUERY_FROM_DESCRIPTION, defaultValue = DEFAULT_FROM, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY),
      @ApiImplicitParam(name = QUERY_TO, value = QUERY_TO_DESCRIPTION, dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getServiceComponentDependencies(String body, @Context HttpHeaders headers,
                                                  @Context UriInfo ui,
                                                  @ApiParam @PathParam("stackName") String stackName,
                                                  @ApiParam @PathParam("stackVersion") String stackVersion,
                                                  @ApiParam @PathParam("serviceName") String serviceName,
                                                  @ApiParam @PathParam("componentName") String componentName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackServiceComponentDependencyResource(stackName, stackVersion, serviceName, componentName, null));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}/components/{componentName}/dependencies/{dependencyName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get a stack service component dependency",
      nickname = "StacksService#getServiceComponentDependency",
      notes = "Returns a stack service component dependency.",
      response = ComponentDependencyResponse.class
  )
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = "Filter returned attributes",
          defaultValue = "Dependencies/*",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getServiceComponentDependency(String body, @Context HttpHeaders headers,
                                      @Context UriInfo ui,
                                      @ApiParam @PathParam("stackName") String stackName,
                                      @ApiParam @PathParam("stackVersion") String stackVersion,
                                      @ApiParam @PathParam("serviceName") String serviceName,
                                      @ApiParam @PathParam("componentName") String componentName,
                                      @ApiParam @PathParam("dependencyName") String dependencyName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackServiceComponentDependencyResource(stackName, stackVersion, serviceName, componentName, dependencyName));
  }

  @GET
  @Path("{stackName}/versions/{stackVersion}/services/{serviceName}/components/{componentName}")
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation(value = "Get details for a stack service component",
      nickname = "StacksService#getServiceComponent",
      notes = "Returns details for a stack service component.",
      response = StackServiceComponentResponse.StackServiceComponentResponseSwagger.class)
  @ApiImplicitParams({
      @ApiImplicitParam(name = QUERY_FIELDS, value = "Filter returned attributes",
          defaultValue = "StackServiceComponents/*",
          dataType = DATA_TYPE_STRING, paramType = PARAM_TYPE_QUERY)
  })
  @ApiResponses(value = {
      @ApiResponse(code = HttpStatus.SC_OK, message = MSG_SUCCESSFUL_OPERATION),
      @ApiResponse(code = HttpStatus.SC_NOT_FOUND, message = MSG_RESOURCE_NOT_FOUND),
      @ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = MSG_SERVER_ERROR)
  })
  public Response getServiceComponent(String body, @Context HttpHeaders headers,
                                      @Context UriInfo ui,
                                      @ApiParam @PathParam("stackName") String stackName,
                                      @ApiParam @PathParam("stackVersion") String stackVersion,
                                      @ApiParam @PathParam("serviceName") String serviceName,
                                      @ApiParam @PathParam("componentName") String componentName) {

    return handleRequest(headers, body, ui, Request.Type.GET,
        createStackServiceComponentResource(stackName, stackVersion, serviceName, componentName));
  }

  /**
   * Handles ANY /{stackName}/versions/{stackVersion}/operating_systems.
   *
   * @param stackName stack name
   * @param stackVersion stack version
   * @return operating system service
   */
  // TODO: find a way to handle this with Swagger (refactor or custom annotation?)
  @Path("{stackName}/versions/{stackVersion}/operating_systems")
  public OperatingSystemService getOperatingSystemsHandler(@ApiParam @PathParam("stackName") String stackName,
                                                           @ApiParam @PathParam("stackVersion") String stackVersion) {
    final Map<Resource.Type, String> stackProperties = new HashMap<>();
    stackProperties.put(Resource.Type.Stack, stackName);
    stackProperties.put(Resource.Type.StackVersion, stackVersion);
    return new OperatingSystemService(stackProperties);
  }

  /**
   * Handles ANY /{stackName}/versions/{stackVersion}/repository_versions.
   *
   * @param stackName stack name
   * @param stackVersion stack version
   * @return repository version service
   */
  // TODO: find a way to handle this with Swagger (refactor or custom annotation?)
  @Path("{stackName}/versions/{stackVersion}/repository_versions")
  public RepositoryVersionService getRepositoryVersionHandler(@ApiParam @PathParam("stackName") String stackName,
                                                              @ApiParam @PathParam("stackVersion") String stackVersion) {
    final Map<Resource.Type, String> stackProperties = new HashMap<>();
    stackProperties.put(Resource.Type.Stack, stackName);
    stackProperties.put(Resource.Type.StackVersion, stackVersion);
    return new RepositoryVersionService(stackProperties);
  }

  /**
   * Handles ANY /{stackName}/versions/{stackVersion}/compatible_repository_versions.
   *
   * @param stackName stack name
   * @param stackVersion stack version
   * @return repository version service
   */
  // TODO: find a way to handle this with Swagger (refactor or custom annotation?)
  @Path("{stackName}/versions/{stackVersion}/compatible_repository_versions")
  public CompatibleRepositoryVersionService getCompatibleRepositoryVersionHandler(
      @ApiParam @PathParam("stackName") String stackName,
      @ApiParam @PathParam("stackVersion") String stackVersion) {
    final Map<Resource.Type, String> stackProperties = new HashMap<>();
    stackProperties.put(Resource.Type.Stack, stackName);
    stackProperties.put(Resource.Type.StackVersion, stackVersion);
    return new CompatibleRepositoryVersionService(stackProperties);
  }

  ResourceInstance createStackServiceComponentResource(
      String stackName, String stackVersion, String serviceName, String componentName) {

    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);
    mapIds.put(Resource.Type.StackService, serviceName);
    mapIds.put(Resource.Type.StackServiceComponent, componentName);

    return createResource(Resource.Type.StackServiceComponent, mapIds);
  }

  ResourceInstance createStackServiceComponentDependencyResource(
      String stackName, String stackVersion, String serviceName, String componentName, String dependencyName) {

    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);
    mapIds.put(Resource.Type.StackService, serviceName);
    mapIds.put(Resource.Type.StackServiceComponent, componentName);
    mapIds.put(Resource.Type.StackServiceComponentDependency, dependencyName);

    return createResource(Resource.Type.StackServiceComponentDependency, mapIds);
  }

  ResourceInstance createStackConfigurationResource(String stackName,
                                                    String stackVersion, String serviceName, String propertyName) {

    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);
    mapIds.put(Resource.Type.StackService, serviceName);
    mapIds.put(Resource.Type.StackConfiguration, propertyName);

    return createResource(Resource.Type.StackConfiguration, mapIds);
  }

  ResourceInstance createStackConfigurationDependencyResource(String stackName,
                                                              String stackVersion, String serviceName, String propertyName) {

    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);
    mapIds.put(Resource.Type.StackService, serviceName);
    mapIds.put(Resource.Type.StackConfiguration, propertyName);

    return createResource(Resource.Type.StackConfigurationDependency, mapIds);
  }

  ResourceInstance createStackServiceResource(String stackName,
                                              String stackVersion, String serviceName) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);
    mapIds.put(Resource.Type.StackService, serviceName);

    return createResource(Resource.Type.StackService, mapIds);
  }

  ResourceInstance createStackVersionResource(String stackName,
                                              String stackVersion) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);

    return createResource(Resource.Type.StackVersion, mapIds);
  }

  ResourceInstance createStackLevelConfigurationsResource(String stackName,
      String stackVersion, String propertyName) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);
    mapIds.put(Resource.Type.StackLevelConfiguration, propertyName);

    return createResource(Resource.Type.StackLevelConfiguration, mapIds);
  }

  ResourceInstance createStackArtifactsResource(String stackName, String stackVersion, String artifactName) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);
    mapIds.put(Resource.Type.StackArtifact, artifactName);

    return createResource(Resource.Type.StackArtifact, mapIds);
  }

  ResourceInstance createStackServiceArtifactsResource(String stackName,
                                                       String stackVersion,
                                                       String serviceName,
                                                       String artifactName) {

    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);
    mapIds.put(Resource.Type.StackService, serviceName);
    mapIds.put(Resource.Type.StackArtifact, artifactName);

    return createResource(Resource.Type.StackArtifact, mapIds);
  }

  ResourceInstance createStackServiceThemesResource(String stackName, String stackVersion, String serviceName,
                                                    String themeName) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);
    mapIds.put(Resource.Type.StackService, serviceName);
    mapIds.put(Resource.Type.Theme, themeName);

    return createResource(Resource.Type.Theme, mapIds);
  }

  ResourceInstance createStackServiceQuickLinksResource(String stackName, String stackVersion, String serviceName,
      String quickLinksConfigurationName) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);
    mapIds.put(Resource.Type.StackService, serviceName);
    mapIds.put(Resource.Type.QuickLink, quickLinksConfigurationName);

    return createResource(Resource.Type.QuickLink, mapIds);
  }

  ResourceInstance createExtensionLinkResource(String stackName, String stackVersion,
                                  String extensionName, String extensionVersion) {
    Map<Resource.Type, String> mapIds = new HashMap<>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);
    mapIds.put(Resource.Type.Extension, extensionName);
    mapIds.put(Resource.Type.ExtensionVersion, extensionVersion);

    return createResource(Resource.Type.ExtensionLink, mapIds);
  }

  ResourceInstance createStackResource(String stackName) {

    return createResource(Resource.Type.Stack,
        Collections.singletonMap(Resource.Type.Stack, stackName));

  }
}

