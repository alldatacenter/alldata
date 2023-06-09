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

package com.netease.arctic.ams.server.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.netease.arctic.ams.api.NoSuchObjectException;
import com.netease.arctic.ams.server.ArcticMetaStore;
import com.netease.arctic.ams.server.config.ArcticMetaStoreConf;
import com.netease.arctic.ams.server.model.Container;
import com.netease.arctic.ams.server.model.OptimizerGroupInfo;
import com.netease.arctic.ams.server.model.TableTaskStatus;
import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.optimizer.Optimizer;
import com.netease.arctic.optimizer.factory.OptimizerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import static com.netease.arctic.ams.api.properties.OptimizerProperties.AMS_SYSTEM_INFO;
import static com.netease.arctic.ams.api.properties.OptimizerProperties.CONTAINER_INFO;
import static com.netease.arctic.ams.api.properties.OptimizerProperties.OPTIMIZER_GROUP_INFO;
import static com.netease.arctic.ams.api.properties.OptimizerProperties.OPTIMIZER_JOB_INFO;

public class OptimizeExecuteService {

  public void startOptimizer(Long jobId) throws Exception {

    com.netease.arctic.ams.server.model.Optimizer optimizerMeta =
            ServiceContainer.getOptimizerService().getOptimizer(jobId);
    Container container = ServiceContainer.getContainerMetaService().getContainer(optimizerMeta.getContainer());
    OptimizerGroupInfo groupInfo =
            ServiceContainer.getOptimizerService().getOptimizerGroupInfo(optimizerMeta.getGroupName());
    Map<String, String> properties = new HashMap<>();
    JSONObject systemInfo = new JSONObject();
    systemInfo.put(
        ArcticMetaStoreConf.ARCTIC_HOME.key(),
        ArcticMetaStore.conf.getString(ArcticMetaStoreConf.ARCTIC_HOME));
    if (ArcticMetaStore.conf.getBoolean(ArcticMetaStoreConf.HA_ENABLE, false)) {
      systemInfo.put(
          ArcticMetaStoreConf.HA_ENABLE.key(),
          ArcticMetaStore.conf.getBoolean(ArcticMetaStoreConf.HA_ENABLE));
      systemInfo.put(
          ArcticMetaStoreConf.CLUSTER_NAME.key(),
          ArcticMetaStore.conf.getString(ArcticMetaStoreConf.CLUSTER_NAME));
      systemInfo.put(
          ArcticMetaStoreConf.ZOOKEEPER_SERVER.key(),
          String.valueOf(ArcticMetaStore.conf.getString(ArcticMetaStoreConf.ZOOKEEPER_SERVER)));
    } else {
      systemInfo.put(
          ArcticMetaStoreConf.THRIFT_BIND_HOST.key(),
          ArcticMetaStore.conf.getString(ArcticMetaStoreConf.THRIFT_BIND_HOST));
      systemInfo.put(
          ArcticMetaStoreConf.THRIFT_BIND_PORT.key(),
          String.valueOf(ArcticMetaStore.conf.getInteger(ArcticMetaStoreConf.THRIFT_BIND_PORT)));
    }
    properties.put(AMS_SYSTEM_INFO, JSONObject.toJSONString(systemInfo));
    properties.put(CONTAINER_INFO, JSONObject.toJSONString(container));
    properties.put(OPTIMIZER_GROUP_INFO, JSONObject.toJSONString(groupInfo));
    properties.put(OPTIMIZER_JOB_INFO, JSONObject.toJSONString(optimizerMeta));
    String type = container.getType();
    OptimizerFactory finalFactory = findOptimizerFactory(type);
    Optimizer optimizer = finalFactory.createOptimizer(OPTIMIZER_GROUP_INFO, properties);
    byte[] serialize = finalFactory.serialize(optimizer);
    ServiceContainer.getOptimizerService().addOptimizerInstance(jobId, serialize);
    optimizer.start();
  }

  public void stopOptimizer(Long jobId) throws Exception {
    com.netease.arctic.ams.server.model.Optimizer optimizerMeta =
            ServiceContainer.getOptimizerService().getOptimizer(jobId);
    byte[] optimizerByte = optimizerMeta.getInstance();
    if (optimizerByte == null) {
      throw new NoSuchObjectException("can not get optimizer instance");
    }
    Container container = ServiceContainer.getContainerMetaService().getContainer(optimizerMeta.getContainer());
    String type = container.getType();
    OptimizerFactory finalFactory = findOptimizerFactory(type);
    finalFactory.deserialize(optimizerByte).stop();
    ServiceContainer.getOptimizerService().deleteOptimizer(jobId);
  }

  public OptimizerFactory findOptimizerFactory(String type) throws NoSuchObjectException {
    ServiceLoader<OptimizerFactory> factories = ServiceLoader.load(OptimizerFactory.class);
    for (OptimizerFactory factory : factories) {
      if (factory.identify().equalsIgnoreCase(type)) {
        return factory;
      }
    }
    throw new NoSuchObjectException("no such OptimizerFactory impl named " + type);
  }

  public static class OptimizerMonitor {

    private static final long OPTIMIZER_JOB_TIMEOUT = 10 * 60 * 1000;

    public void monitorStatus() {
      long currentTime = System.currentTimeMillis();
      List<com.netease.arctic.ams.server.model.Optimizer> optimizers =
          ServiceContainer.getOptimizerService().getOptimizers();
      optimizers.forEach(optimizer -> {
        if ((currentTime - optimizer.getUpdateTime().getTime()) > OPTIMIZER_JOB_TIMEOUT) {
          ServiceContainer.getOptimizerService()
              .updateOptimizerStatus(optimizer.getJobId(), TableTaskStatus.FAILED);
        }
      });
    }
  }
}

