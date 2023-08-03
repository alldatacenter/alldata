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

package com.netease.arctic.server.dashboard.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import com.netease.arctic.ams.api.resource.Resource;
import com.netease.arctic.ams.api.resource.ResourceGroup;
import com.netease.arctic.ams.api.resource.ResourceType;
import com.netease.arctic.server.DefaultOptimizingService;
import com.netease.arctic.server.dashboard.model.OptimizerResourceInfo;
import com.netease.arctic.server.dashboard.model.TableOptimizingInfo;
import com.netease.arctic.server.dashboard.response.OkResponse;
import com.netease.arctic.server.dashboard.response.PageResult;
import com.netease.arctic.server.dashboard.utils.OptimizingUtil;
import com.netease.arctic.server.resource.ContainerMetadata;
import com.netease.arctic.server.resource.OptimizerInstance;
import com.netease.arctic.server.resource.ResourceContainers;
import com.netease.arctic.server.table.ServerTableIdentifier;
import com.netease.arctic.server.table.TableRuntime;
import com.netease.arctic.server.table.TableService;
import io.javalin.http.Context;
import javax.ws.rs.BadRequestException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * optimize controller.
 *
 * @Description: optimizer is a task to compact small files in arctic table.
 * OptimizerController is the optimizer interface's controller,
 * through this interface, you can getRuntime the optimized table,
 * optimizer task, optimizer group information, scale out or release optimizer, etc.
 */
public class OptimizerController {
  private static final String ALL_GROUP = "all";
  private final TableService tableService;
  private final DefaultOptimizingService optimizerManager;

  public OptimizerController(TableService tableService, DefaultOptimizingService optimizerManager) {
    this.tableService = tableService;
    this.optimizerManager = optimizerManager;
  }

  /**
   * getRuntime optimize tables.
   * * @return List of {@link TableOptimizingInfo}
   */
  public void getOptimizerTables(Context ctx) {
    String optimizerGroup = ctx.pathParam("optimizerGroup");
    Integer page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
    Integer pageSize = ctx.queryParamAsClass("pageSize", Integer.class).getOrDefault(20);
    int offset = (page - 1) * pageSize;

    List<TableRuntime> tableRuntimes = new ArrayList<>();
    List<ServerTableIdentifier> tables = tableService.listManagedTables();
    for (ServerTableIdentifier identifier : tables) {
      TableRuntime tableRuntime = tableService.getRuntime(identifier);
      if (tableRuntime == null) {
        continue;
      }
      if (ALL_GROUP.equals(optimizerGroup) || tableRuntime.getOptimizerGroup().equals(optimizerGroup)) {
        tableRuntimes.add(tableRuntime);
      }
    }
    tableRuntimes.sort((o1, o2) -> {
      // first we compare the status , and then we compare the start time when status are equal;
      int statDiff = o1.getOptimizingStatus().compareTo(o2.getOptimizingStatus());
      // status order is asc, startTime order is desc
      if (statDiff == 0) {
        long timeDiff = o1.getCurrentStatusStartTime() - o2.getCurrentStatusStartTime();
        return timeDiff >= 0 ? (timeDiff == 0 ? 0 : -1) : 1;
      } else {
        return statDiff;
      }
    });
    PageResult<TableOptimizingInfo> amsPageResult = PageResult.of(tableRuntimes,
        offset, pageSize, OptimizingUtil::buildTableOptimizeInfo);
    ctx.json(OkResponse.of(amsPageResult));
  }

  /**
   * getRuntime optimizers.
   */
  public void getOptimizers(Context ctx) {
    String optimizerGroup = ctx.pathParam("optimizerGroup");
    Integer page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
    Integer pageSize = ctx.queryParamAsClass("pageSize", Integer.class).getOrDefault(20);

    int offset = (page - 1) * pageSize;

    List<OptimizerInstance> optimizers;
    if (optimizerGroup.equals("all")) {
      optimizers = optimizerManager.listOptimizers();
    } else {
      optimizers = optimizerManager.listOptimizers(optimizerGroup);
    }
    List<OptimizerInstance> optimizerList = new ArrayList<>(optimizers);
    optimizerList.sort(Comparator.comparingLong(OptimizerInstance::getStartTime).reversed());
    List<JSONObject> result = optimizerList.stream().map(e -> {
      JSONObject jsonObject = (JSONObject) JSON.toJSON(e);
      jsonObject.put("jobId", e.getResourceId());
      jsonObject.put("optimizerGroup", e.getGroupName());
      jsonObject.put("coreNumber", e.getThreadCount());
      jsonObject.put("memory", e.getMemoryMb());
      jsonObject.put("jobStatus", "RUNNING");
      jsonObject.put("container", e.getContainerName());
      return jsonObject;
    }).collect(Collectors.toList());

    PageResult<JSONObject> amsPageResult = PageResult.of(result,
        offset, pageSize);
    ctx.json(OkResponse.of(amsPageResult));
  }

  /**
   * getRuntime optimizerGroup: optimizerGroupId, optimizerGroupName
   * url = /optimizerGroups.
   */
  public void getOptimizerGroups(Context ctx) {
    List<JSONObject> result = optimizerManager.listResourceGroups().stream()
        .filter(resourceGroup -> !ResourceContainers.EXTERNAL_CONTAINER_NAME.equals(resourceGroup.getContainer()))
        .map(e -> {
          JSONObject jsonObject = new JSONObject();
          jsonObject.put("optimizerGroupName", e.getName());
          return jsonObject;
        }).collect(Collectors.toList());
    ctx.json(OkResponse.of(result));
  }

  /**
   * getRuntime optimizer info: occupationCore, occupationMemory
   */
  public void getOptimizerGroupInfo(Context ctx) {
    String optimizerGroup = ctx.pathParam("optimizerGroup");
    List<OptimizerInstance> optimizers;
    if (optimizerGroup.equals("all")) {
      optimizers = optimizerManager.listOptimizers();
    } else {
      optimizers = optimizerManager.listOptimizers(optimizerGroup);
    }
    OptimizerResourceInfo optimizerResourceInfo = new OptimizerResourceInfo();
    optimizers.forEach(e -> {
      optimizerResourceInfo.addOccupationCore(e.getThreadCount());
      optimizerResourceInfo.addOccupationMemory(e.getMemoryMb());
    });
    ctx.json(OkResponse.of(optimizerResourceInfo));
  }

  /**
   * release optimizer.
   *
   * @pathParam jobId
   */
  public void releaseOptimizer(Context ctx) {
    String resourceId = ctx.pathParam("jobId");
    Preconditions.checkArgument(!resourceId.isEmpty(), "resource id can not be empty, maybe it's a external optimizer");

    List<OptimizerInstance> optimizerInstances = optimizerManager.listOptimizers()
        .stream()
        .filter(e -> resourceId.equals(e.getResourceId()))
        .collect(Collectors.toList());
    Preconditions.checkState(optimizerInstances.size() > 0, String.format("The resource ID %s has not been indexed" +
        " to any optimizer.", resourceId));
    Resource resource = optimizerManager.getResource(resourceId);
    resource.getProperties().putAll(optimizerInstances.get(0).getProperties());
    ResourceContainers.get(resource.getContainerName()).releaseOptimizer(resource);
    optimizerManager.deleteResource(resourceId);
    optimizerManager.deleteOptimizer(resource.getGroupName(), resourceId);
    ctx.json(OkResponse.of("Success to release optimizer"));
  }

  /**
   * scale out optimizers, url:/optimizerGroups/{optimizerGroup}/optimizers.
   */
  public void scaleOutOptimizer(Context ctx) {
    String optimizerGroup = ctx.pathParam("optimizerGroup");
    Map<String, Integer> map = ctx.bodyAsClass(Map.class);
    int parallelism = map.get("parallelism");

    ResourceGroup resourceGroup = optimizerManager.getResourceGroup(optimizerGroup);
    Resource resource = new Resource.Builder(resourceGroup.getContainer(), resourceGroup.getName(),
        ResourceType.OPTIMIZER)
        .setProperties(resourceGroup.getProperties())
        .setThreadCount(parallelism)
        .build();
    ResourceContainers.get(resource.getContainerName()).requestResource(resource);
    optimizerManager.createResource(resource);
    ctx.json(OkResponse.of("success to scaleOut optimizer"));
  }

  /**
   * get {@link List<OptimizerResourceInfo>}
   * url = /optimize/resourceGroups
   */
  public void getResourceGroup(Context ctx) {
    List<OptimizerResourceInfo> result =
        optimizerManager.listResourceGroups().stream().map(group -> {
          List<OptimizerInstance> optimizers = optimizerManager.listOptimizers(group.getName());
          OptimizerResourceInfo optimizerResourceInfo = new OptimizerResourceInfo();
          optimizerResourceInfo.setResourceGroup(optimizerManager.getResourceGroup(group.getName()));
          optimizers.forEach(optimizer -> {
            optimizerResourceInfo.addOccupationCore(optimizer.getThreadCount());
            optimizerResourceInfo.addOccupationMemory(optimizer.getMemoryMb());
          });
          return optimizerResourceInfo;
        }).collect(Collectors.toList());
    ctx.json(OkResponse.of(result));
  }

  /**
   * create optimizeGroup: name, container, schedulePolicy, properties
   * url = /optimize/resourceGroups/create
   */
  public void createResourceGroup(Context ctx) {
    Map<String, Object> map = ctx.bodyAsClass(Map.class);
    String name = (String) map.get("name");
    String container = (String) map.get("container");
    Map<String, String> properties = (Map) map.get("properties");
    if (optimizerManager.getResourceGroup(name) != null) {
      throw new BadRequestException(String.format("Optimizer group:%s already existed.", name));
    }
    ResourceGroup.Builder builder = new ResourceGroup.Builder(name, container);
    builder.addProperties(properties);
    optimizerManager.createResourceGroup(builder.build());
    ctx.json(OkResponse.of("The optimizer group has been successfully created."));
  }

  /**
   * update optimizeGroup: name, container, schedulePolicy, properties
   * url = /optimize/resourceGroups/update
   */
  public void updateResourceGroup(Context ctx) {
    Map<String, Object> map = ctx.bodyAsClass(Map.class);
    String name = (String) map.get("name");
    String container = (String) map.get("container");
    Map<String, String> properties = (Map) map.get("properties");
    ResourceGroup.Builder builder = new ResourceGroup.Builder(name, container);
    builder.addProperties(properties);
    optimizerManager.updateResourceGroup(builder.build());
    ctx.json(OkResponse.of("The optimizer group has been successfully updated."));
  }

  /**
   * delete optimizeGroup
   * url = /optimize/resourceGroups/{resourceGroupName}
   */
  public void deleteResourceGroup(Context ctx) {
    String name = ctx.pathParam("resourceGroupName");
    optimizerManager.deleteResourceGroup(name);
    ctx.json(OkResponse.of("The optimizer group has been successfully deleted."));
  }

  /**
   * check if optimizerGroup can be deleted
   * url = /optimize/resourceGroups/delete/check
   */
  public void deleteCheckResourceGroup(Context ctx) {
    String name = ctx.pathParam("resourceGroupName");
    ctx.json(OkResponse.of(optimizerManager.canDeleteResourceGroup(name)));
  }

  /**
   * check if optimizerGroup can be deleted
   * url = /optimize/containers/get
   */
  public void getContainers(Context ctx) {
    ctx.json(OkResponse.of(ResourceContainers.getMetadataList()
        .stream()
        .map(ContainerMetadata::getName)
        .collect(Collectors.toList())));
  }
}

