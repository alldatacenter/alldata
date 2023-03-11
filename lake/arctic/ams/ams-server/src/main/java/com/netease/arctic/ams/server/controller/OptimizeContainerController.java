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

import com.netease.arctic.ams.server.controller.response.OkResponse;
import com.netease.arctic.ams.server.model.Container;
import com.netease.arctic.ams.server.model.OptimizerGroupInfo;
import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.ams.server.service.impl.ContainerMetaService;
import com.netease.arctic.ams.server.service.impl.OptimizerService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OptimizeContainerController extends RestBaseController {
  private static Logger LOG = LoggerFactory.getLogger(OptimizeContainerController.class);
  private static OptimizerService optimizeService = ServiceContainer.getOptimizerService();
  private static ContainerMetaService containerMetaService = ServiceContainer.getContainerMetaService();

  /**
   * get container setting
   *
   * @param ctx
   */
  public static void getContainerSetting(Context ctx) {
    List<Container> containers = containerMetaService.getContainers();
    List<OptimizerGroupInfo> optimizerGroups = optimizeService.getAllOptimizerGroupInfo();
    Map<String, List<Map<String, String>>> optimizeGrouped = new HashMap<String, List<Map<String, String>>>();
    Map<String, Container> containerGrouped = new HashMap<String, Container>();

    if (containers != null) {
      containers.stream().forEach(item -> containerGrouped.put(item.getName(), item));
    }
    // group the optimizer by container;
    if (optimizerGroups != null) {
      optimizerGroups.stream().forEach(item -> {
        List groupList = optimizeGrouped.getOrDefault(item.getContainer(), new ArrayList<Map<String, String>>());
        Map<String, String> optimizeGroupItem = new HashMap<>();
        optimizeGroupItem.put("name", item.getName());
        // local 模式只有内存
        if (containerGrouped.get(item.getContainer()) == null) {
          // invalid container name, we ignore it
          LOG.warn("invalid container {}", item.getContainer());
          return;
        }
        if (containerGrouped.get(item.getContainer()).getType().equalsIgnoreCase("local")) {
          if (item.getProperties() != null) {
            optimizeGroupItem.put("memory", item.getProperties().get("memory"));
          }
        } else {
          if (item.getProperties() != null) {
            optimizeGroupItem.put("tmMemory", item.getProperties()
                    .getOrDefault("taskmanager.memory", "-1"));
            optimizeGroupItem.put("jmMemory", item.getProperties()
                    .getOrDefault("jobmanager.memory", "-1"));
          }
        }
        groupList.add(optimizeGroupItem);
        optimizeGrouped.put(item.getContainer(), groupList);
      });
    }

    List<Map<String, Object>> result = new ArrayList<>();
    containers.stream().forEach(item -> {
      Map<String, Object> obj = new HashMap<>();
      obj.put("name", item.getName());
      obj.put("type", item.getType());
      obj.put("properties", item.getProperties());
      obj.put("optimizeGroup", optimizeGrouped.get(item.getName()));
      result.add(obj);
    });

    ctx.json(OkResponse.of(result));
  }
}
