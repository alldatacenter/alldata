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
import com.netease.arctic.ams.server.controller.response.Response;
import io.javalin.testtools.JavalinTest;
import junit.framework.TestCase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginControllerTest extends TestCase {
  private final Logger LOG = LoggerFactory.getLogger(LoginControllerTest.class);

  @Test
  public void testGetCurrent() {
    JavalinTest.test((app, client) -> {

      app.get("/", ctx -> LoginController.getCurrent(ctx));
      final okhttp3.Response resp = client.get("/", x -> {});
      Response result = JSONObject.parseObject(resp.body().string(), Response.class);
      assert result.getCode() == 200;
    });
  }


  @Test
  public void testLogin() {
    JavalinTest.test((app, client) -> {
      app.post("/", ctx -> LoginController.login(ctx));

      JSONObject  requestJson = new JSONObject();
      requestJson.put("user", "admin111");
      requestJson.put("password", "admin");
      final okhttp3.Response resp = client.post("/", requestJson, x -> {});
      Response result = JSONObject.parseObject(resp.body().string(), Response.class);
      assert result.getCode() == 403;
    });
  }
}