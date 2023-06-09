/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.yarn.appMaster.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.security.PermitAll;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.drill.yarn.appMaster.Dispatcher;
import org.apache.drill.yarn.appMaster.http.AbstractTasksModel.TaskModel;
import org.apache.drill.yarn.appMaster.http.ControllerModel.ClusterGroupModel;
import org.apache.drill.yarn.core.DoYUtil;
import org.apache.drill.yarn.core.DrillOnYarnConfig;
import org.apache.drill.yarn.core.NameValuePair;
import org.apache.drill.yarn.zk.ZKClusterCoordinatorDriver;

public class AmRestApi extends PageTree
{
  @Path("/config")
  @PermitAll
  public static class ConfigResource
  {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String,Object> getConfig( ) {
      Map<String, Object> map = new HashMap<>();
      for (NameValuePair pair : DrillOnYarnConfig.instance().getPairs()) {
        map.put(pair.getName(), pair.getValue());
      }
      return map;
    }
  }

  /**
   * Returns cluster status as a tree of JSON objects. Done as explicitly-defined
   * maps to specify the key names (which must not change to avoid breaking
   * compatibility) and to handle type conversions.
   */

  @Path("/status")
  @PermitAll
  public static class StatusResource
  {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String,Object> getStatus( ) {
      ControllerModel model = new ControllerModel( );
      dispatcher.getController().visit( model );

      Map<String,Object> root = new HashMap<>( );
      root.put( "state", model.state.toString() );

      Map<String, Object> summary = new HashMap<>();
      summary.put("drillMemoryMb", model.totalDrillMemory);
      summary.put("drillVcores", model.totalDrillVcores);
      summary.put("yarnMemoryMb", model.yarnMemory);
      summary.put("yarnVcores", model.yarnVcores);
      summary.put("liveBitCount", model.liveCount);
      summary.put("totalBitCount", model.taskCount);
      summary.put("targetBitCount", model.targetCount);
      summary.put("unmanagedCount", model.getUnmanagedCount());
      summary.put("blackListCount", model.getBlacklistCount());
      summary.put("freeNodeCount", model.getFreeNodeCount());
      root.put("summary", summary);

      List<Map<String, Object>> pools = new ArrayList<>();
      for (ClusterGroupModel pool : model.groups) {
        Map<String, Object> poolObj = new HashMap<>();
        poolObj.put("name", pool.name);
        poolObj.put("type", pool.type);
        poolObj.put("liveBitCount", pool.liveCount);
        poolObj.put("targetBitCount", pool.targetCount);
        poolObj.put("totalBitCount", pool.taskCount);
        poolObj.put("totalMemoryMb", pool.memory);
        poolObj.put("totalVcores", pool.vcores);
        pools.add(poolObj);
      }
      root.put("pools", pools);

      AbstractTasksModel.TasksModel tasksModel = new AbstractTasksModel.TasksModel();
      dispatcher.getController().visitTasks(tasksModel);
      List<Map<String, Object>> bits = new ArrayList<>();
      for (TaskModel task : tasksModel.results) {
        Map<String, Object> bitObj = new HashMap<>();
        bitObj.put("containerId", task.container.getId().toString());
        bitObj.put("host", task.getHost());
        bitObj.put("id", task.id);
        bitObj.put("live", task.isLive());
        bitObj.put("memoryMb", task.memoryMb);
        bitObj.put("vcores", task.vcores);
        bitObj.put("pool", task.groupName);
        bitObj.put("state", task.state);
        bitObj.put("trackingState", task.trackingState);
        bitObj.put("endpoint",
            ZKClusterCoordinatorDriver.asString(task.endpoint));
        bitObj.put("link", task.getLink());
        bitObj.put("startTime", task.getStartTime());
        bits.add(bitObj);
      }
      root.put("drillbits", bits);

      return root;
    }
  }

  /**
   * Stop the cluster. Uses a key to validate the request. The value of the key is
   * set in the Drill-on-YARN configuration file. The purpose is simply to prevent
   * accidental cluster shutdown when experimenting with the REST API; this is
   * not meant to be a security mechanism.
   */

  @Path("/stop")
  @PermitAll
  public static class StopResource
  {
    @DefaultValue( "" )
    @QueryParam( "key" )
    String key;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String,String> postStop( )
    {
      Map<String, String> error = checkKey(key);
      if (error != null) {
        return error;
      }

      dispatcher.getController().shutDown();
      return successResponse("Shutting down");
    }
  }

  @Path("/resize/{quantity}")
  @PermitAll
  public static class ResizeResource
  {
    @PathParam(value = "quantity")
    String quantity;
    @DefaultValue( "" )
    @QueryParam( "key" )
    String key;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String,String> postResize( )
    {
      ResizeRequest request = new ResizeRequest(key, quantity);
      if (request.error != null) {
        return request.error;
      }

      int curSize = dispatcher.getController().getTargetCount();
      dispatcher.getController().resizeTo(request.n);
      return successResponse("Resizing from " + curSize + " to " + request.n);
    }
  }

  protected static class ResizeRequest
  {
    Map<String,String> error;
    int n;

    public ResizeRequest( String key, String quantity ) {
      error = checkKey(key);
      if (error != null) {
        return;
      }
      try {
        n = Integer.parseInt(quantity);
      } catch (NumberFormatException e) {
        error = errorResponse("Invalid argument: " + quantity);
      }
      if (n < 0) {
        error = errorResponse("Invalid argument: " + quantity);
      }
    }
  }

  @Path("/grow/{quantity}")
  @PermitAll
  public static class GrowResource
  {
    @PathParam(value = "quantity")
    @DefaultValue( "1" )
    String quantity;
    @DefaultValue( "" )
    @QueryParam( "key" )
    String key;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String,String> postResize( )
    {
      ResizeRequest request = new ResizeRequest(key, quantity);
      if (request.error != null) {
        return request.error;
      }

      int curSize = dispatcher.getController().getTargetCount();
      int newSize = curSize + request.n;
      dispatcher.getController().resizeTo(newSize);
      return successResponse("Growing by " + request.n + " to " + newSize);
    }
  }

  @Path("/shrink/{quantity}")
  @PermitAll
  public static class ShrinkResource
  {
    @PathParam(value = "quantity")
    @DefaultValue( "1" )
    String quantity;
    @DefaultValue( "" )
    @QueryParam( "key" )
    String key;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String,String> postResize( )
    {
      ResizeRequest request = new ResizeRequest(key, quantity);
      if (request.error != null) {
        return request.error;
      }
      int curSize = dispatcher.getController().getTargetCount();
      int newSize = Math.max(curSize - request.n, 0);
      dispatcher.getController().resizeTo(newSize);
      return successResponse("Shrinking by " + request.n + " to " + newSize);
    }
  }

  private static Map<String, String> checkKey(String key) {
    String masterKey = DrillOnYarnConfig.config()
        .getString(DrillOnYarnConfig.HTTP_REST_KEY);
    if (!DoYUtil.isBlank(masterKey) && !masterKey.equals(key)) {
      return errorResponse("Invalid Key");
    }
    return null;
  }

  private static Map<String, String> errorResponse(String msg) {
    Map<String, String> resp = new HashMap<>();
    resp.put("status", "error");
    resp.put("message", msg);
    return resp;
  }

  private static Map<String, String> successResponse(String msg) {
    Map<String, String> resp = new HashMap<>();
    resp.put("status", "ok");
    resp.put("message", msg);
    return resp;
  }

  public AmRestApi(Dispatcher dispatcher) {
    super(dispatcher);

    register(ConfigResource.class);
    register(StatusResource.class);
    register(StopResource.class);
    register(ResizeResource.class);
    register(GrowResource.class);
    register(ShrinkResource.class);
  }
}
