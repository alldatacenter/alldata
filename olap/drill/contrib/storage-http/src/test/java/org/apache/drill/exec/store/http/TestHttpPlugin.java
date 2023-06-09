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

package org.apache.drill.exec.store.http;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.logical.security.PlainCredentialsProvider;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.security.UsernamePasswordCredentials;
import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.shaded.guava.com.google.common.io.Files;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.drill.test.rowSet.RowSetUtilities.mapArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.mapValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests the HTTP Storage plugin. Since the plugin makes use of REST requests,
 * this test class makes use of the okhttp3 MockWebServer to simulate a remote
 * web server. There are two unit tests that make remote REST calls, however
 * these tests are ignored by default.
 * <p>
 * The HTTP reader uses Drill's existing JSON reader class, so the unit tests
 * focus on testing the plugin configurations rather than how well it parses the
 * JSON as this is tested elsewhere.
 */
public class TestHttpPlugin extends ClusterTest {

  private static final int MOCK_SERVER_PORT = 8091;
  private static String TEST_JSON_RESPONSE;
  private static String TEST_CSV_RESPONSE;
  private static String TEST_XML_RESPONSE;
  private static String TEST_JSON_RESPONSE_WITH_DATATYPES;

  @BeforeClass
  public static void setup() throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher));

    TEST_JSON_RESPONSE = Files.asCharSource(DrillFileUtils.getResourceAsFile("/data/response.json"), Charsets.UTF_8).read();
    TEST_CSV_RESPONSE = Files.asCharSource(DrillFileUtils.getResourceAsFile("/data/response.csv"), Charsets.UTF_8).read();
    TEST_XML_RESPONSE = Files.asCharSource(DrillFileUtils.getResourceAsFile("/data/response.xml"), Charsets.UTF_8).read();
    TEST_JSON_RESPONSE_WITH_DATATYPES = Files.asCharSource(DrillFileUtils.getResourceAsFile("/data/response2.json"), Charsets.UTF_8).read();

    dirTestWatcher.copyResourceToRoot(Paths.get("data/"));
    makeLiveConfig();
    makeMockConfig();
  }

  /**
   * Create configs against live external servers. Must be tested manually, and
   * subject to the whims of the external site. Timeout is 10 seconds to allow
   * for real-world delays.
   */
  private static void makeLiveConfig() {

    HttpApiConfig sunriseConfig = HttpApiConfig.builder()
      .url("https://api.sunrise-sunset.org/json")
      .method("GET")
      .build();

    HttpApiConfig sunriseWithParamsConfig = HttpApiConfig.builder()
      .url("https://api.sunrise-sunset.org/json")
      .method("GET")
      .params(Arrays.asList("lat", "lng", "date"))
      .dataPath("results")
      .requireTail(false)
      .build();

    HttpApiConfig stockConfig = HttpApiConfig.builder()
      .url("https://api.worldtradingdata.com/api/v1/stock?symbol=SNAP,TWTR,VOD" +
        ".L&api_token=zuHlu2vZaehdZN6GmJdTiVlp7xgZn6gl6sfgmI4G6TY4ej0NLOzvy0TUl4D4")
      .method("get")
      .build();

    HttpApiConfig pokemonConfig = HttpApiConfig.builder()
      .url("https://pokeapi.co/api/v2/pokemon/{pokemon_name}")
      .method("get")
      .inputType("json")
      .requireTail(false)
      .build();

    Map<String, HttpApiConfig> configs = new HashMap<>();
    configs.put("stock", stockConfig);
    configs.put("sunrise", sunriseConfig);
    configs.put("sunrise2", sunriseWithParamsConfig);
    configs.put("pokemon", pokemonConfig);

    HttpStoragePluginConfig mockStorageConfigWithWorkspace =
        new HttpStoragePluginConfig(false, configs, 10, "", 80, "", "", "", null, PlainCredentialsProvider.EMPTY_CREDENTIALS_PROVIDER);
    mockStorageConfigWithWorkspace.setEnabled(true);
    cluster.defineStoragePlugin("live", mockStorageConfigWithWorkspace);
  }

  /**
   * Create configs for an in-process mock server. Used for normal automated unit
   * testing. Timeout is short to allow for timeout testing. The mock server is
   * useful, but won't catch bugs related to real-world server glitches.
   */
  private static void makeMockConfig() {

    Map<String, String> headers = new HashMap<>();
    headers.put("header1", "value1");
    headers.put("header2", "value2");

    // Use the mock server with HTTP parameters passed as table name.
    // The connection acts like a schema.
    // Ignores the message body except for data.
    HttpApiConfig mockSchema = HttpApiConfig.builder()
      .url("http://localhost:8091/json")
      .method("GET")
      .headers(headers)
      .authType("basic")
      .credentialsProvider(new PlainCredentialsProvider(ImmutableMap.of(
        UsernamePasswordCredentials.USERNAME, "user",
        UsernamePasswordCredentials.PASSWORD, "pass")))
      .dataPath("results")
      .errorOn400(true)
      .build();

    // Use the mock server with the HTTP parameters passed as WHERE
    // clause filters. The connection acts like a table.
    // Ignores the message body except for data.
    // This is the preferred approach, the base URL contains as much info as possible;
    // all other parameters are specified in SQL. See README for an example.
    HttpApiConfig mockTable = HttpApiConfig.builder()
      .url("http://localhost:8091/json")
      .method("GET")
      .headers(headers)
      .authType("basic")
      .userName("user")
      .password("pass")
      .params(Arrays.asList("lat", "lng", "date"))
      .dataPath("results")
      .requireTail(false)
      .build();

    HttpApiConfig mockPostConfig = HttpApiConfig.builder()
      .url("http://localhost:8091/")
      .method("POST")
      .headers(headers)
      .postBody("key1=value1\nkey2=value2")
      .build();

    HttpPaginatorConfig offsetPaginatorForJson = HttpPaginatorConfig.builder()
      .limitParam("limit")
      .offsetParam("offset")
      .method("offset")
      .pageSize(2)
      .build();

    HttpApiConfig mockJsonConfigWithPaginator = HttpApiConfig.builder()
      .url("http://localhost:8091/json")
      .method("get")
      .headers(headers)
      .requireTail(false)
      .paginator(offsetPaginatorForJson)
      .inputType("json")
      .build();

    HttpApiConfig mockPostConfigWithoutPostBody = HttpApiConfig.builder()
      .url("http://localhost:8091/")
      .method("POST")
      .headers(headers)
      .build();

    HttpApiConfig mockCsvConfig = HttpApiConfig.builder()
      .url("http://localhost:8091/csv")
      .method("GET")
      .headers(headers)
      .authType("basic")
      .userName("user")
      .password("pass")
      .dataPath("results")
      .inputType("csv")
      .build();

    HttpApiConfig mockCsvConfigWithPaginator = HttpApiConfig.builder()
      .url("http://localhost:8091/csv")
      .method("get")
      .paginator(offsetPaginatorForJson)
      .inputType("csv")
      .requireTail(false)
      .dataPath("results")
      .build();

    HttpApiConfig mockXmlConfig = HttpApiConfig.builder()
      .url("http://localhost:8091/xml")
      .method("GET")
      .headers(headers)
      .authType("basic")
      .userName("user")
      .password("pass")
      .dataPath("results")
      .inputType("xml")
      .xmlDataLevel(2)
      .build();

    HttpApiConfig mockGithubWithParam = HttpApiConfig.builder()
      .url("http://localhost:8091/orgs/{org}/repos")
      .method("GET")
      .headers(headers)
      .params(Arrays.asList("lat", "lng", "date"))
      .dataPath("results")
      .requireTail(false)
      .build();

    HttpApiConfig mockGithubWithDuplicateParam = HttpApiConfig.builder()
      .url("http://localhost:8091/orgs/{org}/repos")
      .method("GET")
      .headers(headers)
      .params(Arrays.asList("org", "lng", "date"))
      .dataPath("results")
      .requireTail(false)
      .build();

    HttpApiConfig mockGithubWithParamInQuery = HttpApiConfig.builder()
      .url("http://localhost:8091/orgs/{org}/repos?p1={p1}")
      .method("GET")
      .headers(headers)
      .params(Arrays.asList("p2", "p3"))
      .dataPath("results")
      .requireTail(false)
      .build();

    HttpApiConfig mockTableWithJsonOptions = HttpApiConfig.builder()
      .url("http://localhost:8091/json")
      .method("GET")
      .headers(headers)
      .requireTail(false)
      .jsonOptions(HttpJsonOptions.builder()
        .allTextMode(true)
        .build()
      )
      .build();

    Map<String, HttpApiConfig> configs = new HashMap<>();
    configs.put("csv_paginator", mockCsvConfigWithPaginator);
    configs.put("json_paginator", mockJsonConfigWithPaginator);
    configs.put("sunrise", mockSchema);
    configs.put("mocktable", mockTable);
    configs.put("mockpost", mockPostConfig);
    configs.put("nullPost", mockPostConfigWithoutPostBody);
    configs.put("mockcsv", mockCsvConfig);
    configs.put("mockxml", mockXmlConfig);
    configs.put("github", mockGithubWithParam);
    configs.put("github2", mockGithubWithDuplicateParam);
    configs.put("github3", mockGithubWithParamInQuery);
    configs.put("mockJsonAllText", mockTableWithJsonOptions);

    HttpStoragePluginConfig mockStorageConfigWithWorkspace =
        new HttpStoragePluginConfig(false, configs, 2, "", 80, "", "", "", null, PlainCredentialsProvider.EMPTY_CREDENTIALS_PROVIDER);
    mockStorageConfigWithWorkspace.setEnabled(true);
    cluster.defineStoragePlugin("local", mockStorageConfigWithWorkspace);
  }

  @Test
  public void verifyPluginConfig() throws Exception {
    String sql = "SELECT SCHEMA_NAME, TYPE FROM INFORMATION_SCHEMA.`SCHEMATA` WHERE TYPE='http'\n" +
        "ORDER BY SCHEMA_NAME";

    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("SCHEMA_NAME", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("TYPE", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .buildSchema();

    // Expect table-like connections to NOT appear here.
    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addRow("live", "http") // For table-like connections
        .addRow("live.stock", "http")
        .addRow("live.sunrise", "http")
        .addRow("local", "http")
        .addRow("local.mockcsv", "http")
        .addRow("local.mockpost", "http")
        .addRow("local.mockxml", "http")
        .addRow("local.nullpost", "http")
        .addRow("local.sunrise", "http")
        .build();

    RowSetUtilities.verify(expected, results);
  }

  /**
   * Evaluates the HTTP plugin with the results from an API that returns the
   * sunrise/sunset times for a given lat/long and date. API documentation is
   * available here: https://sunrise-sunset.org/api
   *
   * The API returns results in the following format:
   * <pre><code>
   * {
   *       "results":
   *       {
   *         "sunrise":"7:27:02 AM",
   *         "sunset":"5:05:55 PM",
   *         "solar_noon":"12:16:28 PM",
   *         "day_length":"9:38:53",
   *         "civil_twilight_begin":"6:58:14 AM",
   *         "civil_twilight_end":"5:34:43 PM",
   *         "nautical_twilight_begin":"6:25:47 AM",
   *         "nautical_twilight_end":"6:07:10 PM",
   *         "astronomical_twilight_begin":"5:54:14 AM",
   *         "astronomical_twilight_end":"6:38:43 PM"
   *       },
   *        "status":"OK"
   *     }
   * }</code></pre>
   *
   * @throws Exception
   *           Throws exception if something goes awry
   */
  @Test
  @Ignore("Requires Remote Server")
  public void simpleStarQuery() throws Exception {
    String sql = "SELECT * FROM live.sunrise.`?lat=36.7201600&lng=-4.4203400&date=2019-10-02`";

    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addMap("results")
      .add("sunrise", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("sunset", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("solar_noon", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("day_length", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("civil_twilight_begin", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("civil_twilight_end", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("nautical_twilight_begin", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("nautical_twilight_end", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("astronomical_twilight_begin", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("astronomical_twilight_end", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .resumeSchema()
      .add("status", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .build();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(mapValue("6:12:17 AM", "6:01:54 PM", "12:07:06 PM", "11:49:37",
        "5:47:49 AM", "6:26:22 PM", "5:17:51 AM", "6:56:21 PM", "4:47:41 AM", "7:26:31 PM"), "OK")
      .build();

    RowSetUtilities.verify(expected, results);
  }

  /**
   * As above, but we return only the contents of {@code results}, and use
   * filter push-down for the arguments.
   *
   * @throws Exception if anything goes wrong
   */
  @Test
  @Ignore("Requires Remote Server")
  public void wildcardQueryWithParams() throws Exception {
    String sql =
        "SELECT * FROM live.sunrise2\n" +
        "WHERE `lat`=36.7201600 AND `lng`=-4.4203400 AND `date`='2019-10-02'";

    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("sunrise", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("sunset", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("solar_noon", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("day_length", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("civil_twilight_begin", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("civil_twilight_end", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("nautical_twilight_begin", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("nautical_twilight_end", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("astronomical_twilight_begin", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("astronomical_twilight_end", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .build();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("6:12:17 AM", "6:01:54 PM", "12:07:06 PM", "11:49:37", "5:47:49 AM",
        "6:26:22 PM", "5:17:51 AM", "6:56:21 PM", "4:47:41 AM", "7:26:31 PM")
      .build();

    RowSetUtilities.verify(expected, results);
  }

  @Test
  @Ignore("Requires Remote Server")
  public void simpleSpecificQuery() throws Exception {
    String sql = "SELECT t1.results.sunrise AS sunrise, t1.results.sunset AS sunset\n" +
                 "FROM live.sunrise.`?lat=36.7201600&lng=-4.4203400&date=2019-10-02` AS t1";
    doSimpleSpecificQuery(sql);
  }

  @Test
  @Ignore("Requires Remote Server")
  public void simpleSpecificQueryWithParams() throws Exception {
    String sql =
        "SELECT sunrise, sunset\n" +
        "FROM live.sunrise2\n" +
        "WHERE `lat`=36.7201600 AND `lng`=-4.4203400 AND `date`='2019-10-02'";
    doSimpleSpecificQuery(sql);
  }

  private void doSimpleSpecificQuery(String sql) throws Exception {

    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("sunrise", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("sunset", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("6:12:17 AM", "6:01:54 PM")
      .build();

    RowSetUtilities.verify(expected, results);
  }

  @Test
  @Ignore("Requires Remote Server")
  public void liveTestWithURLParameters() throws Exception {
    String sql = "SELECT * FROM live.pokemon WHERE pokemon_name = 'ditto'";
    client.testBuilder()
      .sqlQuery(sql)
      .expectsNumRecords(1)
      .go();
  }

  @Test
  public void simpleTestWithJsonConfig() {
    String sql = "SELECT * FROM local.mockJsonAllText";

    try (MockWebServer server = startServer()) {
      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON_RESPONSE_WITH_DATATYPES));
      RowSet results = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
        .add("col_1", MinorType.VARCHAR, DataMode.OPTIONAL)
        .add("col_2", MinorType.VARCHAR, DataMode.OPTIONAL)
        .add("col_3", MinorType.VARCHAR, DataMode.OPTIONAL)
        .build();

      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addRow("1.0", "2", "3.0")
        .addRow("4.0", "5", "6.0")
        .build();

      RowSetUtilities.verify(expected, results);
    } catch (Exception e) {
      System.out.println(e.getMessage());
      fail();
    }
  }

  @Test
  public void simpleTestWithMockServerWithURLParams() throws Exception {
    String sql = "SELECT _response_url FROM local.github\n" +
        "WHERE `org` = 'apache'";

    try (MockWebServer server = startServer()) {
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setBody(TEST_JSON_RESPONSE)
      );

      RowSet results = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
          .add("_response_url", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
          .build();

      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
          .addRow("http://localhost:8091/orgs/apache/repos")
          .build();

      RowSetUtilities.verify(expected, results);
    }
  }

  @Test
  public void simpleTestWithMockServerWithURLParamsOfBooleanType() throws Exception {
    String sql = "SELECT _response_url FROM local.github\n" +
      "WHERE `org` = true";

    try (MockWebServer server = startServer()) {
      server.enqueue(
        new MockResponse()
          .setResponseCode(200)
          .setBody(TEST_JSON_RESPONSE)
      );

      RowSet results = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
        .add("_response_url", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .build();

      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addRow("http://localhost:8091/orgs/true/repos")
        .build();

      RowSetUtilities.verify(expected, results);
    }
  }

  @Test
  public void simpleTestWithMockServerWithURLParamsOfIntType() throws Exception {
    String sql = "SELECT _response_url FROM local.github\n" +
      "WHERE `org` = 1234";

    try (MockWebServer server = startServer()) {
      server.enqueue(
        new MockResponse()
          .setResponseCode(200)
          .setBody(TEST_JSON_RESPONSE)
      );

      RowSet results = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
        .add("_response_url", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .build();

      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addRow("http://localhost:8091/orgs/1234/repos")
        .build();

      RowSetUtilities.verify(expected, results);
    }
  }

  @Test
  @Ignore("Requires Remote Server")
  public void simpleTestWithUrlParamsInSubquery() throws Exception {
    String sql = "select pokemon_data.data.game_index AS game_index, pokemon_data.data.version.name AS name " +
      "from (select flatten(game_indices) as data " +
      "from live.pokemon " +
      "where pokemon_name='ditto' " +
      ") as pokemon_data WHERE pokemon_data.data.game_index=76";

    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("game_index", MinorType.BIGINT, TypeProtos.DataMode.OPTIONAL)
      .add("name", MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .build();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(76, "red")
      .addRow(76, "blue")
      .addRow(76, "yellow")
      .build();

    RowSetUtilities.verify(expected, results);
  }

  @Test
  public void simpleTestWithMockServerWithDuplicateURLParams() throws Exception {
    String sql = "SELECT _response_url FROM local.github2\n" +
      "WHERE `org` = 'apache'";

    try (MockWebServer server = startServer()) {
      server.enqueue(
        new MockResponse()
          .setResponseCode(200)
          .setBody(TEST_JSON_RESPONSE)
      );

      RowSet results = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
        .add("_response_url", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .build();

      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addRow("http://localhost:8091/orgs/apache/repos?org=apache")
        .build();

      RowSetUtilities.verify(expected, results);
    }
  }

  @Test
  public void testUrlParamsInQueryString() throws Exception {
    String sql = "SELECT _response_url FROM local.github3\n" +
      "WHERE `org` = 'apache' AND p1='param1' AND p2='param2'";

    try (MockWebServer server = startServer()) {
      server.enqueue(
        new MockResponse()
          .setResponseCode(200)
          .setBody(TEST_JSON_RESPONSE)
      );

      RowSet results = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
        .add("_response_url", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .build();

      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addRow("http://localhost:8091/orgs/apache/repos?p1=param1&p2=param2")
        .build();

      RowSetUtilities.verify(expected, results);
    }
  }

  /**
   * When the user has configured an API connection with URL parameters,
   * it is mandatory that those parameters are included in the WHERE clause. Drill
   * will throw an exception if that parameter is not present.
   * @throws Exception if anything goes wrong
   */
  @Test
  public void testUrlParamError() throws Exception {
    String sql = "SELECT _response_url FROM local.github\n";

    try (MockWebServer server = startServer()) {
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setBody(TEST_JSON_RESPONSE)
      );
      run(sql);
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("API Query with URL Parameters must be populated."));
    }
  }

  @Test
   public void testSerDeCSV() throws Exception {
    try (MockWebServer server = startServer()) {

      server.enqueue(
        new MockResponse().setResponseCode(200)
          .setBody(TEST_CSV_RESPONSE)
      );

      String sql = "SELECT COUNT(*) FROM local.mockcsv.`csv?arg1=4` ";
      String plan = queryBuilder().sql(sql).explainJson();
      long cnt = queryBuilder().physical(plan).singletonLong();
      assertEquals("Counts should match", 2L, cnt);
    }
  }

  @Test
  public void testSerDe() throws Exception {
    try (MockWebServer server = startServer()) {

      server.enqueue(
        new MockResponse().setResponseCode(200)
          .setBody(TEST_JSON_RESPONSE)
      );

      String sql = "SELECT COUNT(*) FROM local.sunrise.`?lat=36.7201600&lng=-4.4203400&date=2019-10-02`";
      String plan = queryBuilder().sql(sql).explainJson();
      long cnt = queryBuilder().physical(plan).singletonLong();
      assertEquals("Counts should match", 1L, cnt);
    }
  }

  @Test
  public void testApiConfigRequiresTailSerDe() throws Exception {
    String sql = "SELECT * FROM local.mocktable";

    queryBuilder()
      .sql(sql)
      .detailedPlanMatcher()
      .include("requireTail=false")
      .match();
  }

  @Test
  public void simpleTestWithMockServer() throws Exception {
    String sql = "SELECT * FROM local.sunrise.`?lat=36.7201600&lng=-4.4203400&date=2019-10-02`";
    doSimpleTestWithMockServer(sql);
  }

  @Test
  public void simpleTestWithMockServerWithParams() throws Exception {
    String sql = "SELECT * FROM local.mocktable\n" +
                 "WHERE `lat` = 36.7201600 AND `lng` = -4.4203400 AND `date` = '2019-10-02'";
    doSimpleTestWithMockServer(sql);
  }

  @Test
  public void testCsvResponse() throws Exception {
    String sql = "SELECT * FROM local.mockcsv.`csv?arg1=4`";
    try (MockWebServer server = startServer()) {

      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_CSV_RESPONSE));

      RowSet results = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
        .add("col1", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("col2", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("col3", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .build();

      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addRow("1", "2", "3")
        .addRow("4", "5", "6")
        .build();

      RowSetUtilities.verify(expected, results);
    }
  }

  @Test
  public void testXmlResponse() throws Exception {
    String sql = "SELECT * FROM local.mockxml.`?arg1=4` LIMIT 5";
    try (MockWebServer server = startServer()) {

      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_XML_RESPONSE));

      RowSet results = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
        .add("attributes", MinorType.MAP)
        .addNullable("COMMON", MinorType.VARCHAR)
        .addNullable("BOTANICAL", MinorType.VARCHAR)
        .addNullable("ZONE", MinorType.VARCHAR)
        .addNullable("LIGHT", MinorType.VARCHAR)
        .addNullable("PRICE", MinorType.VARCHAR)
        .addNullable("AVAILABILITY", MinorType.VARCHAR)
        .buildSchema();

      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addRow(mapArray(), "Bloodroot", "Sanguinaria canadensis", "4", "Mostly Shady", "$2.44", "031599")
        .addRow(mapArray(),"Columbine", "Aquilegia canadensis", "3", "Mostly Shady", "$9.37", "030699")
        .addRow(mapArray(),"Marsh Marigold", "Caltha palustris", "4", "Mostly Sunny", "$6.81", "051799")
        .addRow(mapArray(), "Cowslip", "Caltha palustris", "4", "Mostly Shady", "$9.90", "030699")
        .addRow(mapArray(), "Dutchman's-Breeches", "Dicentra cucullaria", "3", "Mostly Shady", "$6.44", "012099")
        .build();

      RowSetUtilities.verify(expected, results);
    }
  }

  @Test
  public void testImplicitFieldsWithJSON() throws Exception {
    String sql = "SELECT _response_code, _response_message, _response_protocol, _response_url FROM local.sunrise.`?lat=36.7201600&lng=-4.4203400&date=2019-10-02`";
    try (MockWebServer server = startServer()) {

      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON_RESPONSE));

      RowSet results = client.queryBuilder().sql(sql).rowSet();
      TupleMetadata expectedSchema = new SchemaBuilder()
        .add("_response_code", TypeProtos.MinorType.INT, TypeProtos.DataMode.OPTIONAL)
        .add("_response_message", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("_response_protocol", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("_response_url", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .build();

      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addRow(200, "OK", "http/1.1", "http://localhost:8091/json?lat=36.7201600&lng=-4.4203400&date=2019-10-02")
        .build();

      RowSetUtilities.verify(expected, results);
    }
  }

  @Test
  public void testImplicitFieldsWithCSV() throws Exception {
    String sql = "SELECT _response_code, _response_message, _response_protocol, _response_url FROM local.mockcsv.`csv?arg1=4`";
    try (MockWebServer server = startServer()) {

      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_CSV_RESPONSE));

      RowSet results = client.queryBuilder().sql(sql).rowSet();
      TupleMetadata expectedSchema = new SchemaBuilder()
        .add("_response_code", TypeProtos.MinorType.INT, TypeProtos.DataMode.OPTIONAL)
        .add("_response_message", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("_response_protocol", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("_response_url", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .build();

      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addRow(200, "OK", "http/1.1", "http://localhost:8091/csvcsv?arg1=4")
        .addRow(200, "OK", "http/1.1", "http://localhost:8091/csvcsv?arg1=4")
        .build();

      RowSetUtilities.verify(expected, results);
    }
  }

  @Test
  public void testImplicitFieldsWithXML() throws Exception {
    String sql = "SELECT _response_code, _response_message, _response_protocol, _response_url FROM local.mockxml.`?arg1=4` LIMIT 5";
    try (MockWebServer server = startServer()) {

      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_XML_RESPONSE));

      RowSet results = client.queryBuilder().sql(sql).rowSet();
      TupleMetadata expectedSchema = new SchemaBuilder()
        .add("_response_code", TypeProtos.MinorType.INT, TypeProtos.DataMode.OPTIONAL)
        .add("_response_message", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("_response_protocol", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("_response_url", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .build();

      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addRow(200, "OK", "http/1.1", "http://localhost:8091/xml?arg1=4")
        .addRow(200, "OK", "http/1.1", "http://localhost:8091/xml?arg1=4")
        .addRow(200, "OK", "http/1.1", "http://localhost:8091/xml?arg1=4")
        .addRow(200, "OK", "http/1.1", "http://localhost:8091/xml?arg1=4")
        .addRow(200, "OK", "http/1.1", "http://localhost:8091/xml?arg1=4")
        .build();

      RowSetUtilities.verify(expected, results);
    }
  }

  private void doSimpleTestWithMockServer(String sql) throws Exception {
    try (MockWebServer server = startServer()) {

      server.enqueue(
        new MockResponse().setResponseCode(200)
          .setBody(TEST_JSON_RESPONSE)
      );

      RowSet results = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
        .add("sunrise", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("sunset", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("solar_noon", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("day_length", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("civil_twilight_begin", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("civil_twilight_end", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("nautical_twilight_begin", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("nautical_twilight_end", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("astronomical_twilight_begin", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("astronomical_twilight_end", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .build();

      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addRow("6:13:58 AM", "5:59:55 PM", "12:06:56 PM", "11:45:57", "5:48:14 AM", "6:25:38 PM", "5:18:16 AM", "6:55:36 PM", "4:48:07 AM", "7:25:45 PM")
        .build();

      RowSetUtilities.verify(expected, results);
    }
  }

  @Test
  public void testPostWithMockServerAndNullPostbody() throws Exception {
    try (MockWebServer server = startServer()) {

      server.enqueue(
        new MockResponse()
          .setResponseCode(200)
          .setBody(TEST_JSON_RESPONSE)
      );

      String sql = "SELECT * FROM local.nullPost\n.`json?lat=36.7201600&lng=-4.4203400&date=2019-10-02`";
      RowSet results = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
        .addMap("results")
        .add("sunrise", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("sunset", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("solar_noon", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("day_length", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("civil_twilight_begin", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("civil_twilight_end", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("nautical_twilight_begin", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("nautical_twilight_end", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("astronomical_twilight_begin", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("astronomical_twilight_end", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .resumeSchema()
        .add("status", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .build();

      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addRow(mapValue("6:13:58 AM", "5:59:55 PM", "12:06:56 PM", "11:45:57", "5:48:14 AM", "6:25:38 PM", "5:18:16 AM", "6:55:36 PM", "4:48:07 AM", "7:25:45 PM"), "OK")
        .build();

      RowSetUtilities.verify(expected, results);

      RecordedRequest recordedRequest = server.takeRequest();
      assertEquals("POST", recordedRequest.getMethod());
    }
  }

  @Test
  public void testPostWithMockServer() throws Exception {
    try (MockWebServer server = startServer()) {

      server.enqueue(
        new MockResponse()
          .setResponseCode(200)
          .setBody(TEST_JSON_RESPONSE)
      );

      String sql = "SELECT * FROM local.mockPost.`json?lat=36.7201600&lng=-4.4203400&date=2019-10-02`";
      RowSet results = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
        .addMap("results")
        .add("sunrise", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("sunset", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("solar_noon", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("day_length", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("civil_twilight_begin", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("civil_twilight_end", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("nautical_twilight_begin", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("nautical_twilight_end", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("astronomical_twilight_begin", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("astronomical_twilight_end", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .resumeSchema()
        .add("status", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .build();

      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addRow(mapValue("6:13:58 AM", "5:59:55 PM", "12:06:56 PM", "11:45:57", "5:48:14 AM", "6:25:38 PM", "5:18:16 AM", "6:55:36 PM", "4:48:07 AM", "7:25:45 PM"), "OK")
        .build();

      RowSetUtilities.verify(expected, results);

      RecordedRequest recordedRequest = server.takeRequest();
      assertEquals("POST", recordedRequest.getMethod());
      assertEquals(recordedRequest.getHeader("header1"), "value1");
      assertEquals(recordedRequest.getHeader("header2"), "value2");
    }
  }

  @Test
  public void specificTestWithMockServer() throws Exception {
    try (MockWebServer server = startServer()) {

      server.enqueue(
        new MockResponse().setResponseCode(200)
          .setBody(TEST_JSON_RESPONSE)
      );

      String sql = "SELECT sunrise, sunset FROM local.sunrise.`?lat=36.7201600&lng=-4.4203400&date=2019-10-02` AS t1";

      RowSet results = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
        .add("sunrise", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("sunset", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .buildSchema();

      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addRow("6:13:58 AM", "5:59:55 PM")
        .build();

      RowSetUtilities.verify(expected, results);
    }
  }

  @Test
  public void testLimitPushdown() throws Exception {
    String sql = "SELECT sunrise, sunset FROM local.sunrise.`?lat=36.7201600&lng=-4.4203400&date=2019-10-02` AS t1 LIMIT 5";

    queryBuilder()
      .sql(sql)
      .planMatcher()
      .include("Limit", "maxRecords=5")
      .match();
  }

  @Test
  public void testLimitPushdownWithFilter() throws Exception {
    String sql = "SELECT sunrise, sunset FROM live.sunrise2 WHERE `date`='2019-10-02' LIMIT 5";

    queryBuilder()
      .sql(sql)
      .planMatcher()
      .include("Limit", "maxRecords=5", "filters=\\{date=2019-10-02\\}")
      .match();
  }

  @Test
  public void testSlowResponse() throws Exception {
    try (MockWebServer server = startServer()) {

      server.enqueue(
        new MockResponse().setResponseCode(200)
          .setBody(TEST_JSON_RESPONSE)
          .setBodyDelay(6, TimeUnit.SECONDS)
      );

      String sql = "SELECT sunrise AS sunrise, sunset AS sunset FROM local.sunrise.`?lat=36.7201600&lng=-4.4203400&date=2019-10-02` AS t1";

      try {
        client.queryBuilder().sql(sql).rowSet();
        fail();
      } catch (Exception e) {
         assertTrue("Not timeout exception, " + e, e.getMessage().contains("DATA_READ ERROR: timeout"));
      }
    }
  }

  @Test
  public void testZeroByteResponse() throws Exception {
    try (MockWebServer server = startServer()) {

      server.enqueue(
        new MockResponse().setResponseCode(200)
          .setBody("")
      );

      String sql = "SELECT * FROM local.sunrise.`?lat=36.7201600&lng=-4.4203400&date=2019-10-02`";

      RowSet results = client.queryBuilder().sql(sql).rowSet();
      assertNull(results);
    }
  }

  @Test
  public void testZeroByteResponseFromCSV() throws Exception {
    try (MockWebServer server = startServer()) {

      server.enqueue(
        new MockResponse().setResponseCode(200)
          .setBody("")
      );

      String sql = "SELECT * FROM local.mockcsv.`csv?arg1=4`";

      RowSet results = client.queryBuilder().sql(sql).rowSet();
      assertNull(results);
    }
  }

  // The connection expects a response object of the form
  // { results: { ... } }, but there is no such object, which
  // is treated as a null (no data, no schema) result set.
  @Test
  public void testEmptyJSONObjectResponse() throws Exception {
    try (MockWebServer server = startServer()) {

      server.enqueue(
        new MockResponse().setResponseCode(200)
          .setBody("{}")
      );

      String sql = "SELECT * FROM local.sunrise.`?lat=36.7201600&lng=-4.4203400&date=2019-10-02`";

      RowSet results = client.queryBuilder().sql(sql).rowSet();
      assertNull(results);
    }
  }

  @Test
  public void testNullContent() throws Exception {
    try (MockWebServer server = startServer()) {

      server.enqueue(
        new MockResponse().setResponseCode(200)
          .setBody("{results: null}")
      );

      String sql = "SELECT * FROM local.sunrise.`?lat=36.7201600&lng=-4.4203400&date=2019-10-02`";

      RowSet results = client.queryBuilder().sql(sql).rowSet();
      assertNull(results);
    }
  }

  // Note that, in this test, the response is not empty. Instead, the
  // response has a single row with no columns.
  @Test
  public void testEmptyContent() throws Exception {
    try (MockWebServer server = startServer()) {

      server.enqueue(
        new MockResponse().setResponseCode(200)
          .setBody("{results: {} }")
      );

      String sql = "SELECT * FROM local.sunrise.`?lat=36.7201600&lng=-4.4203400&date=2019-10-02`";

      RowSet results = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
        .buildSchema();

      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addRow()
        .build();

      RowSetUtilities.verify(expected, results);
    }
  }

  @Test
  public void testErrorResponse() throws Exception {
    try (MockWebServer server = startServer()) {

      server.enqueue(
        new MockResponse().setResponseCode(404)
          .setBody("{}")
      );

      String sql = "SELECT * FROM local.sunrise.`?lat=36.7201600&lng=-4.4203400&date=2019-10-02`";

      try {
        client.queryBuilder().sql(sql).rowSet();
        fail();
      } catch (Exception e) {
        String msg = e.getMessage();
        assertTrue(msg.contains("DATA_READ ERROR: HTTP request failed"));
        assertTrue(msg.contains("Response code: 404"));
        assertTrue(msg.contains("Response message: Client Error"));
        assertTrue(msg.contains("Connection: sunrise"));
        assertTrue(msg.contains("Plugin: local"));
      }
    }
  }

  @Test
  public void testNoErrorOn404() throws Exception {
    try (MockWebServer server = startServer()) {

      server.enqueue(
        new MockResponse()
          .setResponseCode(404)
          .setBody("{}")
      );

      String sql = "SELECT _response_code, _response_message, _response_protocol, _response_url FROM local.mocktable";
      RowSet results = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
        .add("_response_code", TypeProtos.MinorType.INT, TypeProtos.DataMode.OPTIONAL)
        .add("_response_message", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("_response_protocol", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("_response_url", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .build();

      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addRow(404, "Client Error", "http/1.1", "http://localhost:8091/json")
        .build();

      RowSetUtilities.verify(expected, results);
    }
  }

  @Test
  public void testHeaders() throws Exception {
    try (MockWebServer server = startServer()) {

      server.enqueue(
        new MockResponse().setResponseCode(200)
          .setBody(TEST_JSON_RESPONSE)
      );

      String sql = "SELECT * FROM local.sunrise.`?lat=36.7201600&lng=-4.4203400&date=2019-10-02`";
      RowSet results = client.queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
        .add("sunrise", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("sunset", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("solar_noon", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("day_length", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("civil_twilight_begin", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("civil_twilight_end", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("nautical_twilight_begin", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("nautical_twilight_end", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("astronomical_twilight_begin", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .add("astronomical_twilight_end", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
        .build();

      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addRow("6:13:58 AM", "5:59:55 PM", "12:06:56 PM", "11:45:57", "5:48:14 AM",
                "6:25:38 PM", "5:18:16 AM", "6:55:36 PM", "4:48:07 AM", "7:25:45 PM")
        .build();

      RowSetUtilities.verify(expected, results);

      RecordedRequest request = server.takeRequest();
      assertEquals("value1", request.getHeader("header1"));
      assertEquals("value2", request.getHeader("header2"));
      assertEquals("Basic dXNlcjpwYXNz", request.getHeader("Authorization"));
    }
  }

  /**
   * Helper function to start the MockHTTPServer
   * @return Started Mock server
   * @throws IOException If the server cannot start, throws IOException
   */
  public static MockWebServer startServer() throws IOException, InterruptedException {
    MockWebServer server = new MockWebServer();
//    server.takeRequest(25, TimeUnit.SECONDS);
    server.start(MOCK_SERVER_PORT);
    return server;
  }
}
