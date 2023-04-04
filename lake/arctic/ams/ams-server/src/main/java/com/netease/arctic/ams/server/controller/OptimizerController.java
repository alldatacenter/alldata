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

package com.netease.arctic.ams.server.controller;

import com.netease.arctic.ams.server.controller.response.ErrorResponse;
import com.netease.arctic.ams.server.controller.response.OkResponse;
import com.netease.arctic.ams.server.controller.response.PageResult;
import com.netease.arctic.ams.server.model.Optimizer;
import com.netease.arctic.ams.server.model.OptimizerGroupInfo;
import com.netease.arctic.ams.server.model.OptimizerResourceInfo;
import com.netease.arctic.ams.server.model.TableOptimizeInfo;
import com.netease.arctic.ams.server.model.TableTaskStatus;
import com.netease.arctic.ams.server.optimize.IOptimizeService;
import com.netease.arctic.ams.server.optimize.TableOptimizeItem;
import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.ams.server.service.impl.ContainerMetaService;
import com.netease.arctic.ams.server.service.impl.OptimizerService;
import com.netease.arctic.table.TableIdentifier;
import io.javalin.http.Context;
import io.javalin.http.HttpCode;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * optimize controller.
 *
 * @Description: optimizer is a task to compact small files in arctic table.
 * OptimizerController is the optimizer interface's controller, through this interface, you can get the optimized table,
 * optimizer task, optimizer group information, scale out or release optimizer, etc.
 */
public class OptimizerController extends RestBaseController {
  private static final Logger LOG = LoggerFactory.getLogger(OptimizerController.class);
  private static final IOptimizeService iOptimizeService = ServiceContainer.getOptimizeService();

  /**
   * get optimize tables.
   * * @return List of {@link TableOptimizeInfo}
   */
  public static void getOptimizerTables(Context ctx) {
    String optimizerGroup = ctx.pathParam("optimizerGroup");
    Integer page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
    Integer pageSize = ctx.queryParamAsClass("pageSize", Integer.class).getOrDefault(20);
    String statusFilter = ctx.queryParam("statusFilter");
    List<String> statusList = new ArrayList<>();
    if (StringUtils.isNotEmpty(statusFilter)) {
      statusList = Arrays.stream(statusFilter.split(","))
          .map(item -> item.trim().toLowerCase()).collect(Collectors.toList());
    }
    int offset = (page - 1) * pageSize;

    try {
      List<TableOptimizeItem> arcticTableItemList = new ArrayList<>();
      List<TableIdentifier> tables = ServiceContainer.getOptimizeService().listCachedTables();

      for (TableIdentifier tableIdentifier : tables) {
        TableOptimizeItem arcticTableItem = iOptimizeService.getTableOptimizeItem(tableIdentifier);
        // if status is specified, we filter item by status
        if (statusList.size() > 0 &&
            !statusList.contains(
                arcticTableItem.getTableOptimizeRuntime().getOptimizeStatus().toString().toLowerCase())) {
          continue;
        }

        if ("all".equals(optimizerGroup)) {
          arcticTableItemList.add(arcticTableItem);
        } else {
          String groupName = arcticTableItem.getGroupNameCache();
          if (StringUtils.isNotEmpty(groupName)) {
            if (optimizerGroup.equals(groupName)) {
              arcticTableItemList.add(arcticTableItem);
            }
          }
        }
      }
      // sort the table list
      arcticTableItemList.sort((o1, o2) -> {
        // first we compare the status , and then we compare the start time when status are equal;
        int statDiff = o1.getTableOptimizeRuntime().getOptimizeStatus()
            .compareTo(o2.getTableOptimizeRuntime().getOptimizeStatus());
        // status order is asc, startTime order is desc
        if (statDiff == 0) {
          long timeDiff = o1.getTableOptimizeRuntime().getOptimizeStatusStartTime() -
              o2.getTableOptimizeRuntime().getOptimizeStatusStartTime();
          return timeDiff >= 0 ? (timeDiff == 0 ? 0 : -1) : 1;
        } else if (statDiff < 0) {
          return -1;
        } else {
          return 1;
        }
      });
      PageResult<TableOptimizeItem, TableOptimizeInfo> amsPageResult = PageResult.of(arcticTableItemList,
          offset, pageSize, TableOptimizeItem::buildTableOptimizeInfo);
      ctx.json(OkResponse.of(amsPageResult));
    } catch (Exception e) {
      LOG.error("Failed to get optimizerGroup tables", e);
      ctx.json(new ErrorResponse(HttpCode.BAD_REQUEST, "Failed to get optimizerGroup tables", ""));
    }
  }

  /**
   * get optimizers.
   */
  public static void getOptimizers(Context ctx) {
    String optimizerGroup = ctx.pathParam("optimizerGroup");
    Integer page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
    Integer pageSize = ctx.queryParamAsClass("pageSize", Integer.class).getOrDefault(20);

    int offset = (page - 1) * pageSize;

    try {
      OptimizerService optimizerService = new OptimizerService();
      List<Optimizer> optimizers;
      if (optimizerGroup.equals("all")) {
        optimizers = optimizerService.getOptimizers();
      } else {
        optimizers = optimizerService.getOptimizers(optimizerGroup);
      }

      PageResult<Optimizer, Optimizer> amsPageResult = PageResult.of(optimizers,
          offset, pageSize);
      ctx.json(OkResponse.of(amsPageResult));
    } catch (Exception e) {
      LOG.error("Failed to get optimizer", e);
      ctx.json(new ErrorResponse(HttpCode.BAD_REQUEST,
          "Failed to get optimizer", ""));
    }
  }

  /**
   * get optimizerGroup: optimizerGroupId, optimizerGroupName
   * url = /optimizerGroups.
   */
  public static void getOptimizerGroups(Context ctx) {
    try {
      OptimizerService optimizerService = new OptimizerService();
      ctx.json(OkResponse.of(optimizerService.getOptimizerGroups()));
    } catch (Exception e) {
      LOG.error("Failed to get optimizerGroups", e);
      ctx.json(new ErrorResponse(HttpCode.BAD_REQUEST,
          "Failed to get optimizerGroups", ""));
    }
  }

  /**
   * get optimizer info: occupationCore, occupationMemory
   */
  public static void getOptimizerGroupInfo(Context ctx) {
    String optimizerGroup = ctx.pathParam("optimizerGroup");
    try {
      OptimizerService optimizerService = new OptimizerService();
      if (optimizerGroup.equals("all")) {
        OptimizerResourceInfo optimizerResourceInfo = optimizerService.getOptimizerGroupsResourceInfo();
        if (null == optimizerResourceInfo) {
          optimizerResourceInfo = new OptimizerResourceInfo();
          optimizerResourceInfo.setOccupationCore(0);
          optimizerResourceInfo.setOccupationMemory(0);
        }
        ctx.json(OkResponse.of(optimizerResourceInfo));
      } else {
        OptimizerResourceInfo optimizerResourceInfo = optimizerService.getOptimizerGroupResourceInfo(optimizerGroup);
        if (null == optimizerResourceInfo) {
          optimizerResourceInfo = new OptimizerResourceInfo();
          optimizerResourceInfo.setOccupationCore(0);
          optimizerResourceInfo.setOccupationMemory(0);
        }
        ctx.json(OkResponse.of(optimizerResourceInfo));
      }
    } catch (Exception e) {
      LOG.error("Failed to get optimizerGroup info", e);
      ctx.json(new ErrorResponse(HttpCode.BAD_REQUEST, "Failed to get optimizerGroup info", ""));
    }
  }

  /**
   * release optimizer.
   *
   * @pathParam jobId
   */
  public static void releaseOptimizer(Context ctx) {
    String jobId = ctx.pathParam("jobId");
    try {
      ServiceContainer.getOptimizeExecuteService().stopOptimizer(Long.parseLong(jobId));
      ctx.json(OkResponse.of("Success to release optimizer"));
    } catch (Exception e) {
      LOG.error("Failed to release optimizer", e);
      ctx.json(new ErrorResponse(HttpCode.BAD_REQUEST, "Failed to release optimizer", ""));
    }
  }

  /**
   * scaleout optimizers, url:/optimizerGroups/{optimizerGroup}/optimizers.
   */
  public static void scaleOutOptimizer(Context ctx) {

    String optimizerGroup = ctx.pathParam("optimizerGroup");
    Map<String, Integer> map = ctx.bodyAsClass(Map.class);
    int parallelism = map.get("parallelism");

    try {
      String currentTime = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").format(new Date());
      String optimizerName = "arctic_optimizer_" + currentTime;
      OptimizerService optimizerService = new OptimizerService();
      OptimizerGroupInfo optimizerGroupInfo = optimizerService.getOptimizerGroupInfo(optimizerGroup);
      ContainerMetaService containerMetaService = new ContainerMetaService();
      String container = optimizerGroupInfo.getContainer();
      String containerType = containerMetaService.getContainerType(container);
      if (containerType.equals("local")) {
        String memory = optimizerGroupInfo.getProperties().get("memory");
        optimizerService.insertOptimizer(optimizerName, optimizerGroupInfo.getId(), optimizerGroupInfo.getName(),
            TableTaskStatus.STARTING,
            new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").format(new Date()),
            parallelism, Long.parseLong(memory), parallelism, container);
      } else if (containerType.equals("flink")) {
        int tmMemory = Integer.parseInt(optimizerGroupInfo.getProperties().get("taskmanager.memory"));
        int jmMemory = Integer.parseInt(optimizerGroupInfo.getProperties().get("jobmanager.memory"));
        long memory = jmMemory + (long) tmMemory * parallelism;

        optimizerService.insertOptimizer(optimizerName,
            optimizerGroupInfo.getId(),
            optimizerGroupInfo.getName(),
            TableTaskStatus.STARTING,
            new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").format(new Date()),
            parallelism + 1, memory, parallelism, container);
      } else {
        ctx.json(new ErrorResponse(HttpCode.BAD_REQUEST, containerType + " type container not support scaleout by ams",
            ""));
        return;
      }

      String optimizerId = optimizerService.selectOptimizerIdByOptimizerName(optimizerName);

      try {
        ServiceContainer.getOptimizeExecuteService().startOptimizer(Long.parseLong(optimizerId));
      } catch (Exception e) {
        LOG.error("Failed to scaleOut optimizer", e);
        optimizerService.deleteOptimizerByName(optimizerName);
        ctx.json(new ErrorResponse(HttpCode.BAD_REQUEST,
            "Failed to scaleOut optimizer", ""));
        return;
      }
      ctx.json(OkResponse.of("success to scaleOut optimizer"));
    } catch (Exception e) {
      LOG.error("Failed to scaleOut optimizer", e);
      ctx.json(new ErrorResponse(HttpCode.BAD_REQUEST, "Failed to scaleOut optimizer", ""));
    }
  }

  /**
   * create optimizeGroup: name, container, schedulePolicy, properties
   * url = /optimize/optimizerGroups
   */
  public static void createOptimizeGroup(Context ctx) {
    Map<String, Object> map = ctx.bodyAsClass(Map.class);
    try {
      String name = (String) map.get("name");
      String container = (String) map.get("container");
      String schedulePolicy = (String) map.get("schedulePolicy");
      Map<String, String> properties = (Map) map.get("properties");
      ServiceContainer.getOptimizeQueueService().createQueue(name, container, schedulePolicy, properties);
    } catch (Exception e) {
      LOG.error("Failed to create optimize group", e);
      ctx.json(new ErrorResponse(
          HttpCode.BAD_REQUEST, "Failed to create optimize group, " + e.getMessage(), ""));
      return;
    }
    ctx.json(OkResponse.of("Success to create optimize group"));
  }
}

