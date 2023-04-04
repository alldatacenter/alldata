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

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.netease.arctic.ams.api.MetaException;
import com.netease.arctic.ams.server.ArcticMetaStore;
import com.netease.arctic.ams.server.config.ConfigFileProperties;
import com.netease.arctic.ams.server.controller.response.OkResponse;
import com.netease.arctic.ams.server.controller.response.Response;
import com.netease.arctic.ams.server.model.Container;
import com.netease.arctic.ams.server.model.OptimizeQueueMeta;
import com.netease.arctic.ams.server.model.Optimizer;
import com.netease.arctic.ams.server.model.TableTaskStatus;
import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.ams.server.service.impl.OptimizerService;
import com.netease.arctic.ams.server.utils.JDBCSqlSessionFactoryProvider;
import io.javalin.testtools.JavalinTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: OptimizerController Test
 */
@PrepareForTest({
      JDBCSqlSessionFactoryProvider.class,
      ArcticMetaStore.class
})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*"})
public class OptimizerControllerTest {

  @BeforeClass
  public static void beforeClass() throws MetaException {
    createContainer("test1", "local");
    createOptimizeGroup("testOptimizeGroup", "test1");

    createContainer("test2", "local");
    createOptimizeGroup("testOptimizeGroup2", "test2");
  }
  
  @AfterClass
  public static void afterClass() {
    removeAllOptimizeGroup();
  }

  @Test
  public void testGetOptimizers() {

    OptimizerService optimizerService = new OptimizerService();
    optimizerService.insertOptimizer("test1", 1, "testOptimizeGroup",
            TableTaskStatus.STARTING,
            new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").format(new Date()),
            2, 1024, 1, "test1");
    
    JavalinTest.test((app, client) -> {
        app.get("/{optimizerGroup}/", OptimizerController::getOptimizers);
        final okhttp3.Response resp = client.get("/all?page=1&pageSize=20", x -> {});
        OkResponse result = JSONObject.parseObject(resp.body().string(), OkResponse.class);
        assert JSONObject.parseObject(result.getResult().toString()).
                getString("total").equals("1");
        assert result.getCode() == 200;
    });
    optimizerService.deleteOptimizerByName("test1");
  }

  @Test
  public void testGetOptimizersTable() {
    JavalinTest.test((app, client) -> {
      app.get("/{optimizerGroup}/", OptimizerController::getOptimizerTables);
      final okhttp3.Response resp = client.get("/all?page=1&pageSize=20", x -> {});
      Response result = JSONObject.parseObject(resp.body().string(), Response.class);
      assert result.getCode() == 200;
    });
  }

  @Test
  public void testGetOptimizerGroups() {
    JavalinTest.test((app, client) -> {
      app.get("/", OptimizerController::getOptimizerGroups);
      final okhttp3.Response resp = client.get("/", x -> {
      });
      OkResponse result = JSONObject.parseObject(resp.body().string(), OkResponse.class);
      assert JSONObject.parseObject(JSONObject.parseArray(result.getResult().toString()).get(0).toString()).
          getString("optimizerGroupName").equals("testOptimizeGroup");
      assert result.getCode() == 200;
    });
  }

  @Test
  public void testGetOptimizerGroupInfo() {
    OptimizerService optimizerService = new OptimizerService();
    optimizerService.insertOptimizer("test1", 1, "testOptimizeGroup",
            TableTaskStatus.STARTING,
            new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").format(new Date()),
            2, 1024, 1, "test1");
    JavalinTest.test((app, client) -> {
      app.get("/{optimizerGroup}", OptimizerController::getOptimizerGroupInfo);
      final okhttp3.Response resp = client.get("/all", x -> {});
      OkResponse result = JSONObject.parseObject(resp.body().string(), OkResponse.class);
      assert JSONObject.parseObject(result.getResult().toString()).getString("occupationCore").equals("2");
      assert result.getCode() == 200;
    });
    optimizerService.deleteOptimizerByName("test1");
  }

  @Test
  public void testScaleOutOptimizer() {
    JavalinTest.test((app, client) -> {
      app.post("/{optimizerGroup}", OptimizerController::scaleOutOptimizer);
      JSONObject  requestJson = new JSONObject();
      requestJson.put("parallelism", 1);
      final okhttp3.Response resp = client.post("/testOptimizeGroup", requestJson);
      Response result = JSONObject.parseObject(resp.body().string(), Response.class);
      assert result.getCode() == 200;
    });
  }

  @Test
  public void testReleaseOptimizer() {
    OptimizerService optimizerService = new OptimizerService();
    optimizerService.insertOptimizer("test2", 1, "testOptimizeGroup2",
            TableTaskStatus.STARTING,
            new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").format(new Date()),
            2, 1024, 1, "test2");
    JavalinTest.test((app, client) -> {
      List<Optimizer> optimizers = optimizerService.getOptimizers();
      long optimizerId = optimizers.get(0).getJobId();
      ServiceContainer.getOptimizeExecuteService().startOptimizer(optimizerId);
      app.post("/{jobId}", OptimizerController::releaseOptimizer);
      final okhttp3.Response resp = client.post("/" + optimizerId);
      Response result = JSONObject.parseObject(resp.body().string(), Response.class);
      assert result.getCode() == 200;
    });
  }

  @Test
  public void testCreateOptimizeGroup() {
    // normal case, code = 200
    Map<String, String> properties = Maps.newHashMap();
    properties.put("memory","1024");
    createOptimizeGroup("test_queue", "test1", "", properties, 200);
    // abnormal case 1, optimize group name already exists, code =400
    createOptimizeGroup("test_queue", "test1", "", properties, 400);
    // abnormal case 2, can not find such container config, code =400
    createOptimizeGroup("test_queue1", "", "", properties, 400);
    // abnormal case 3, scheduling policy only can be quota and balanced, code =400
    createOptimizeGroup("test_queue2", "test1", "test", properties, 400);
  }

  public void createOptimizeGroup
      (String name, String container, String schedulePolicy, Map<String, String> properties, int code) {
    JSONObject  requestJson = new JSONObject();
    requestJson.put("name",name);
    requestJson.put("container",container);
    requestJson.put("schedulePolicy",schedulePolicy);
    requestJson.put("properties",properties);
    JavalinTest.test((app, client) -> {
      app.post("/createQueue", OptimizerController::createOptimizeGroup);
      final okhttp3.Response resp = client.post("/createQueue", requestJson);
      OkResponse result = JSONObject.parseObject(resp.body().string(), OkResponse.class);
      assert  result.getCode() == code;
    });
  }

  private static void createOptimizeGroup(String name, String container) throws MetaException {
    OptimizeQueueMeta optimizeQueueMeta = new OptimizeQueueMeta();
    optimizeQueueMeta.setName(name);
    optimizeQueueMeta.setContainer(container);
    optimizeQueueMeta.setSchedulingPolicy(ConfigFileProperties.OPTIMIZE_SCHEDULING_POLICY_QUOTA);

    Map<String, String> map = Maps.newHashMap();
    map.put("memory", "1024");
    optimizeQueueMeta.setProperties(map);
    ServiceContainer.getOptimizeQueueService().createQueue(optimizeQueueMeta);
  }

  private static void createContainer(String name, String type) {
    Container container = new Container();
    container.setName(name);
    container.setType(type);
    Map<String, String> map = new HashMap<>();
    map.put("hadoop_home", "test");
    container.setProperties(map);
    ServiceContainer.getOptimizeQueueService().insertContainer(container);
  }

  private static void removeAllOptimizeGroup() {
    ServiceContainer.getOptimizeQueueService().removeAllQueue();
  }
}