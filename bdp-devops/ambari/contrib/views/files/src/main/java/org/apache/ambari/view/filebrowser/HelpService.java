/**
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

package org.apache.ambari.view.filebrowser;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.commons.hdfs.HdfsService;
import org.json.simple.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * Help service
 */
public class HelpService extends HdfsService {

  /**
   * Constructor
   * @param context View Context instance
   */
  public HelpService(ViewContext context) {
    super(context);
  }

  /**
   * @param context
   * @param viewConfigs : extra properties that needs to be included into configs
   */
  public HelpService(ViewContext context, Map<String, String> viewConfigs) {
    super(context, viewConfigs);
  }

  /**
   * Version
   * @return version
   */
  @GET
  @Path("/version")
  @Produces(MediaType.TEXT_PLAIN)
  public Response version() {
    return Response.ok("0.0.1-SNAPSHOT").build();
  }

  /**
   * Description
   * @return description
   */
  @GET
  @Path("/description")
  @Produces(MediaType.TEXT_PLAIN)
  public Response description() {
    return Response.ok("Application to work with HDFS").build();
  }

  /**
   * Filesystem configuration
   * @return filesystem configuration
   */
  @GET
  @Path("/filesystem")
  @Produces(MediaType.TEXT_PLAIN)
  public Response filesystem() {
    return Response.ok(
        context.getProperties().get("webhdfs.url")).build();
  }

  /**
   * HDFS Status
   * @return status
   */
  @GET
  @Path("/hdfsStatus")
  @Produces(MediaType.APPLICATION_JSON)
  public Response hdfsStatus(){
    HdfsService.hdfsSmokeTest(context);
    return getOKResponse();
  }

  private Response getOKResponse() {
    JSONObject response = new JSONObject();
    response.put("message", "OK");
    response.put("trace", null);
    response.put("status", "200");
    return Response.ok().entity(response).type(MediaType.APPLICATION_JSON).build();
  }

}
