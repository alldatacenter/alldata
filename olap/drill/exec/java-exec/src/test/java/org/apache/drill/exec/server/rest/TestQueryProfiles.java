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
package org.apache.drill.exec.server.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.proto.UserBitShared.QueryType;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestQueryProfiles extends ClusterTest {

  private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");
  private static final int TIMEOUT = 3000; // for debugging
  private static String[] SQL = new String[5];
  private static int portNumber;

  private final OkHttpClient httpClient = new OkHttpClient.Builder()
      .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
      .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
      .readTimeout(TIMEOUT, TimeUnit.SECONDS).build();

  private final ObjectMapper mapper = new ObjectMapper();

  @BeforeClass
  public static void setup() throws Exception {
    ClusterFixtureBuilder builder = new ClusterFixtureBuilder(dirTestWatcher)
        .configProperty(ExecConstants.HTTP_ENABLE, true)
        .configProperty(ExecConstants.HTTP_PORT_HUNT, true);
    startCluster(builder);
    portNumber = cluster.drillbit().getWebServerPort();
  }

  @Test
  public void testAdorableQuery() throws IOException {
    String sql = "SELECT * FROM cp.`employee.json` LIMIT 20";
    SQL[0] = sql;
    QueryWrapper query = new QueryWrapper(sql, QueryType.SQL.name(), "10", null, null, null);
    assertEquals(200, runQuery(query));
  }

  @Test
  public void testBadQuery() throws IOException {
    String sql = "SELECT * FROM cp.`employee123.json` LIMIT 20";
    SQL[1] = sql;
    QueryWrapper query = new QueryWrapper(sql, QueryType.SQL.name(), null, null, null, null);
    int code = runQuery(query);
    assertEquals(200, code);
  }

  @Test
  public void testCompletedProfiles() throws Exception {
    String url = String.format("http://localhost:%d/profiles/completed.json", portNumber);
    Request request = new Request.Builder().url(url).build();
    try (Response response = httpClient.newCall(request).execute()) {
      String respon_body = response.body().string();
      JSONObject json_data = (JSONObject) new JSONParser().parse(respon_body);
      JSONArray finishedQueries = (JSONArray) json_data.get("finishedQueries");
      JSONObject firstData = (JSONObject) finishedQueries.get(0);
      JSONObject secondData = (JSONObject) finishedQueries.get(1);

      assertEquals(2, finishedQueries.size());
      assertEquals(SQL[1], firstData.get("query").toString());
      assertEquals("Failed", firstData.get("state").toString());
      assertEquals(SQL[0], secondData.get("query").toString());
      assertEquals("Succeeded", secondData.get("state").toString());
    }
  }

  @Test
  public void testQueryProfiles() throws Exception {
    String url = String.format("http://localhost:%d/profiles.json", portNumber);
    Request request = new Request.Builder().url(url).build();
    try (Response response = httpClient.newCall(request).execute()) {
      String respon_body = response.body().string();
      JSONObject json_data = (JSONObject) new JSONParser().parse(respon_body);
      JSONArray finishedQueries = (JSONArray) json_data.get("finishedQueries");
      JSONObject firstData = (JSONObject) finishedQueries.get(0);
      JSONObject secondData = (JSONObject) finishedQueries.get(1);

      assertEquals(5, json_data.size());
      assertEquals("[]", json_data.get("runningQueries").toString());
      assertEquals(2, finishedQueries.size());
      assertEquals(SQL[1], firstData.get("query").toString());
      assertEquals("Failed", firstData.get("state").toString());
      assertEquals(SQL[0], secondData.get("query").toString());
      assertEquals("Succeeded", secondData.get("state").toString());
    }
  }

  @Test
  public void testRunningProfiles() throws Exception {
    String url = String.format("http://localhost:%d/profiles/running.json", portNumber);
    Request request = new Request.Builder().url(url).build();
    try (Response response = httpClient.newCall(request).execute()) {
      String respon_body = response.body().string();
      JSONObject json_data = (JSONObject) new JSONParser().parse(respon_body);
      assertEquals(4, json_data.size());
      assertEquals("[]", json_data.get("runningQueries").toString());
    }
  }

  private int runQuery(QueryWrapper query) throws IOException {
    ObjectWriter writer = mapper.writerFor(QueryWrapper.class);
    String json = writer.writeValueAsString(query);
    String url = String.format("http://localhost:%d/query.json", portNumber);
    Request request = new Request.Builder().url(url).post(RequestBody.create(json, JSON_MEDIA_TYPE)).build();
    try (Response response = httpClient.newCall(request).execute()) {
      return response.code();
    }
  }
}
