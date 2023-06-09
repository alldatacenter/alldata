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
import com.netease.arctic.ams.server.AmsTestBase;
import com.netease.arctic.ams.server.ArcticMetaStore;
import com.netease.arctic.ams.server.config.Configuration;
import com.netease.arctic.ams.server.controller.response.OkResponse;
import io.javalin.testtools.JavalinTest;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TerminalControllerTest {
  private final Logger LOG = LoggerFactory.getLogger(TerminalControllerTest.class);

  protected static final Object ANY = new Object();

  @Before
  public void startMetastore() throws Exception {
    if (ArcticMetaStore.conf == null){
      ArcticMetaStore.conf = new Configuration();
    }
  }


  @Test
  public void testGetExamples() {
    JavalinTest.test((app, client) -> {
      app.get("/", ctx -> TerminalController.getExamples(ctx));
      final okhttp3.Response resp = client.get("/", x -> {});
      OkResponse result = JSONObject.parseObject(resp.body().string(), OkResponse.class);
      assert result.getCode() == 200;
    });
  }

  @Test
  public void testGetSqlExamples() {
    JavalinTest.test((app, client) -> {
      app.get("/{exampleName}/", ctx -> TerminalController.getSqlExamples(ctx));
      final okhttp3.Response resp = client.get("/CreateTable/", x -> {});
      OkResponse result = JSONObject.parseObject(resp.body().string(), OkResponse.class);
      assert result.getCode() == 200;
    });
  }

  @Test
  public void testTerminal() {
    // test execute sql
    JavalinTest.test((app, client) -> {
      JSONObject requestJson = new JSONObject();
      requestJson.put("sql", "create database arctic_test;");
      app.post("/{catalog}/", ctx -> TerminalController.executeScript(ctx));
      app.exception(Exception.class, (e, cxt) -> LOG.error("", e));
      final okhttp3.Response resp1 = client.post("/" + AmsTestBase.catalog.name() + "/", requestJson, x -> {
      });
      OkResponse result = JSONObject.parseObject(resp1.body().string(), OkResponse.class);
      assert result.getCode() == 200;
    });

      // test get latest info
    JavalinTest.test((app, client) -> {
      app.get("/", ctx -> TerminalController.getLatestInfo(ctx));
      final okhttp3.Response resp = client.get("/", x -> {});
      OkResponse result = JSONObject.parseObject(resp.body().string(), OkResponse.class);
      assert result.getCode() == 200;
    });

      // test get sql running logs
    JavalinTest.test((app, client) -> {
      app.get("/{sessionId}/", ctx -> TerminalController.getLogs(ctx));
      final okhttp3.Response resp3 = client.get("/1/", x -> {});
      OkResponse result = JSONObject.parseObject(resp3.body().string(), OkResponse.class);
      assert result.getCode() == 200;
    });

    // test get sql status and result
    JavalinTest.test((app, client) -> {
      app.get("/{sessionId}/", ctx -> TerminalController.getSqlResult(ctx));
      final okhttp3.Response resp4 = client.get("/1/", x -> {});
      OkResponse result = JSONObject.parseObject(resp4.body().string(), OkResponse.class);
      assert result.getCode() == 200;
    });

      // test stop sql
    JavalinTest.test((app, client) -> {
      app.put("/{sessionId}/", ctx -> TerminalController.stopSql(ctx));
      final okhttp3.Response resp5 = client.put("/1/", new JSONObject(), x -> {});
      OkResponse result = JSONObject.parseObject(resp5.body().string(), OkResponse.class);
      assert result.getCode() == 200;
    });
  }

}
