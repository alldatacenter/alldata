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
import com.netease.arctic.ams.server.controller.response.ErrorResponse;
import com.netease.arctic.ams.server.controller.response.OkResponse;
import com.netease.arctic.ams.server.controller.response.Response;
import com.netease.arctic.ams.server.model.CatalogSettingInfo;
import io.javalin.testtools.HttpClient;
import io.javalin.testtools.JavalinTest;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CatalogControllerTest {
  @Test
  public void testGetCatalogTypeList() {
    JavalinTest.test((app, client) -> {
      app.get("/", CatalogController::getCatalogTypeList);
      final okhttp3.Response resp = client.get("/", x -> {
      });
      OkResponse result = JSONObject.parseObject(resp.body().string(), OkResponse.class);
      assert result.getCode() == 200;
      List<Map<String, String>> resBody = (List<Map<String, String>>)result.getResult();
      assert  resBody.size() == 4;
    });
  }

  public void getCatalogDetail(Consumer consumer, HttpClient client) {
    // test get catalog detail
    final okhttp3.Response resp1 = client.get("/unittest", x -> {});
    final OkResponse result1 = JSONObject.parseObject(resp1.body().toString(), OkResponse.class);
    assert result1.getCode() == 200;
    final CatalogSettingInfo info = JSONObject.parseObject(result1.getResult().toString(), CatalogSettingInfo.class);
    assert info != null;
  }

  @Test
  public void testCreateCatalog() {
    String name = "unittest";
    String newAuthUser = "UnitTest";
    JavalinTest.test((app, client) -> {

      app.post("/", CatalogController::createCatalog);
      app.get("/{catalogName}", CatalogController::getCatalogDetail);
      app.put("/{catalogName}", CatalogController::updateCatalog);
      app.delete("/{catalogName}", CatalogController::deleteCatalog);

      // add one catalog
      String paramString = "{\"name\":\"unittest\",\"type\":\"ams\","
              + "\"storageConfig\":{"
              + "\"hadoop.core.site\":\"1\","
              + "\"hadoop.hdfs.site\":\"2\","
              + "\"hive.site\":\"3\"},"
              + "\"authConfig\":{\"auth.type\":\"SIMPLE\",\"auth.simple.hadoop_username\":\"arctic\"},"
              + "\"properties\":{\"warehouse\":\"/data/warehouse\"},\"tableFormatList\":[\"MIXED_HIVE\"]}";
      JSONObject param = JSONObject.parseObject(paramString);
      final okhttp3.Response resp = client.post("/", param);
      Response result = JSONObject.parseObject(resp.body().string(), Response.class);
      assert result.getCode() == 200;

      // test get catalog detail
      final okhttp3.Response resp1 = client.get("/unittest", x -> {});
      final OkResponse result1 = JSONObject.parseObject(resp1.body().string(), OkResponse.class);
      assert result1.getCode() == 200;
      final CatalogSettingInfo info = JSONObject.parseObject(result1.getResult().toString(), CatalogSettingInfo.class);
      assert info != null;
      assert info.getName().equals(name);
      // update catalog
      JSONObject authConfig = new JSONObject();
      authConfig.put("auth.type", "SIMPLE");
      authConfig.put("auth.simple.hadoop_username", newAuthUser);
      param.put("authConfig", authConfig);
      final okhttp3.Response resp2 = client.put("/unittest", param);
      final OkResponse result2 = JSONObject.parseObject(resp2.body().string(), OkResponse.class);
      assert result2.getCode() == 200;
      // get catalog after update
      final okhttp3.Response resp3 = client.get("/unittest", x -> {});
      final OkResponse result3 = JSONObject.parseObject(resp3.body().string(), OkResponse.class);
      assert result3.getCode() == 200;
      final CatalogSettingInfo infoPut = JSONObject.parseObject(result3.getResult().toString(), CatalogSettingInfo.class);
      assert infoPut != null;
      assert infoPut.getAuthConfig().get("auth.simple.hadoop_username").equals(newAuthUser);

      // delete catalog
      final okhttp3.Response resp4 = client.delete("/unittest");
      final OkResponse result4 = JSONObject.parseObject(resp4.body().string(), OkResponse.class);
      assert result4.getCode() == 200;
      // check delete result
      final okhttp3.Response resp5 = client.get("/unittest", x -> {});
      final OkResponse result5 = JSONObject.parseObject(resp5.body().string(), OkResponse.class);
      assert result5.getCode() == 200;
      assert result5.getResult() == null;

      // validate catalog property
      paramString = "{\"name\":\"unittest2\",\"type\":\"ams\","
          + "\"storageConfig\":{"
          + "\"hadoop.core.site\":\"1\","
          + "\"hadoop.hdfs.site\":\"2\","
          + "\"hive.site\":\"3\"},"
          + "\"authConfig\":{\"auth.type\":\"SIMPLE\",\"auth.simple.hadoop_username\":\"arctic\"},"
          + "\"properties\":{},\"tableFormatList\":[\"MIXED_HIVE\"]}";
      JSONObject param6 = JSONObject.parseObject(paramString);
      final okhttp3.Response resp6 = client.post("/", param6);
      String resp6Body = resp6.body().string();
      final ErrorResponse result6 = JSONObject.parseObject(resp6Body, ErrorResponse.class);
      assert result6.getCode() == 400;
      assert result6.getMessage().equals("Catalog type:ams require property:warehouse.");
    });
  }
}
