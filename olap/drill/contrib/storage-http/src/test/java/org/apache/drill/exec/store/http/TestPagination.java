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
import org.apache.drill.common.logical.security.PlainCredentialsProvider;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.physical.rowSet.DirectRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Files;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.QueryRowSetIterator;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TestPagination extends ClusterTest {
  private static final int MOCK_SERVER_PORT = 8092;
  private static String TEST_CSV_RESPONSE;
  private static String TEST_CSV_RESPONSE_2;
  private static String TEST_CSV_RESPONSE_3;
  private static String TEST_CSV_RESPONSE_4;
  private static String TEST_JSON_PAGE1;
  private static String TEST_JSON_PAGE2;
  private static String TEST_JSON_PAGE3;
  private static String TEST_XML_PAGE1;
  private static String TEST_XML_PAGE2;
  private static String TEST_XML_PAGE3;

  @BeforeClass
  public static void setup() throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher));

    TEST_CSV_RESPONSE = Files.asCharSource(DrillFileUtils.getResourceAsFile("/data/response.csv"), Charsets.UTF_8).read();
    TEST_CSV_RESPONSE_2 = Files.asCharSource(DrillFileUtils.getResourceAsFile("/data/response_2.csv"), Charsets.UTF_8).read();
    TEST_CSV_RESPONSE_3 = Files.asCharSource(DrillFileUtils.getResourceAsFile("/data/response_3.csv"), Charsets.UTF_8).read();
    TEST_CSV_RESPONSE_4 = Files.asCharSource(DrillFileUtils.getResourceAsFile("/data/response_4.csv"), Charsets.UTF_8).read();

    TEST_JSON_PAGE1 = Files.asCharSource(DrillFileUtils.getResourceAsFile("/data/p1.json"), Charsets.UTF_8).read();
    TEST_JSON_PAGE2 = Files.asCharSource(DrillFileUtils.getResourceAsFile("/data/p2.json"), Charsets.UTF_8).read();
    TEST_JSON_PAGE3 = Files.asCharSource(DrillFileUtils.getResourceAsFile("/data/p3.json"), Charsets.UTF_8).read();

    TEST_XML_PAGE1 = Files.asCharSource(DrillFileUtils.getResourceAsFile("/data/response_1.xml"), Charsets.UTF_8).read();
    TEST_XML_PAGE2 = Files.asCharSource(DrillFileUtils.getResourceAsFile("/data/response_2.xml"), Charsets.UTF_8).read();
    TEST_XML_PAGE3 = Files.asCharSource(DrillFileUtils.getResourceAsFile("/data/response_3.xml"), Charsets.UTF_8).read();

    dirTestWatcher.copyResourceToRoot(Paths.get("data/"));
    makeMockConfig(cluster);
    makeLiveConfig(cluster);
  }

  /**
   * Create configs against live external servers. Must be tested manually, and
   * subject to the whims of the external site. Timeout is 10 seconds to allow
   * for real-world delays.
   */
  public static void makeLiveConfig(ClusterFixture cluster) {

    Map<String, String> uaHeaders = new HashMap<>();
    uaHeaders.put("User-Agent",  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36");

    HttpPaginatorConfig githubPagePaginator = HttpPaginatorConfig.builder()
      .pageParam("page")
      .pageSizeParam("per_page")
      .pageSize(5)
      .method("PAGE")
      .build();

    HttpApiConfig githubConfig = HttpApiConfig.builder()
      .url("https://api.github.com/orgs/{org}/repos")
      .method("get")
      .requireTail(false)
      .headers(uaHeaders)
      .inputType("json")
      .paginator(githubPagePaginator)
      .build();

    Map<String, HttpApiConfig> configs = new HashMap<>();
    configs.put("github", githubConfig);

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
  public static void makeMockConfig(ClusterFixture cluster) {

    Map<String, String> headers = new HashMap<>();
    headers.put("header1", "value1");
    headers.put("header2", "value2");


    HttpPaginatorConfig offsetPaginatorForJson = HttpPaginatorConfig.builder()
      .limitParam("limit")
      .offsetParam("offset")
      .method("offset")
      .pageSize(2)
      .build();

    HttpApiConfig mockJsonConfigWithPaginator = HttpApiConfig.builder()
      .url("http://localhost:8092/json")
      .method("get")
      .headers(headers)
      .requireTail(false)
      .paginator(offsetPaginatorForJson)
      .inputType("json")
      .build();

    HttpPaginatorConfig pagePaginatorForXML = HttpPaginatorConfig.builder()
      .method("page")
      .pageParam("page")
      .pageSizeParam("pageSize")
      .pageSize(3)
      .build();

    List<String> params = new ArrayList<>();
    params.add("foo");

    HttpApiConfig mockXmlConfigWithPaginator = HttpApiConfig.builder()
      .url("http://localhost:8092/xml")
      .method("GET")
      .requireTail(false)
      .params(params)
      .paginator(pagePaginatorForXML)
      .inputType("xml")
      .xmlDataLevel(2)
      .build();

    HttpApiConfig mockXmlConfigWithPaginatorAndUrlParams = HttpApiConfig.builder()
      .url("http://localhost:8092/xml/{org}")
      .method("GET")
      .requireTail(false)
      .params(params)
      .paginator(pagePaginatorForXML)
      .inputType("xml")
      .xmlDataLevel(2)
      .build();


    HttpApiConfig mockCsvConfigWithPaginator = HttpApiConfig.builder()
      .url("http://localhost:8092/csv")
      .method("get")
      .paginator(offsetPaginatorForJson)
      .inputType("csv")
      .requireTail(false)
      .dataPath("results")
      .build();


    Map<String, HttpApiConfig> configs = new HashMap<>();
    configs.put("csv_paginator", mockCsvConfigWithPaginator);
    configs.put("json_paginator", mockJsonConfigWithPaginator);
    configs.put("xml_paginator", mockXmlConfigWithPaginator);
    configs.put("xml_paginator_url_params", mockXmlConfigWithPaginatorAndUrlParams);

    HttpStoragePluginConfig mockStorageConfigWithWorkspace =
      new HttpStoragePluginConfig(false, configs, 2, "", 80, "", "", "", null, PlainCredentialsProvider.EMPTY_CREDENTIALS_PROVIDER);
    mockStorageConfigWithWorkspace.setEnabled(true);
    cluster.defineStoragePlugin("local", mockStorageConfigWithWorkspace);
  }


  @Test
  @Ignore("Requires Live Connection to Github")
  public void testPagePaginationWithURLParameters() throws Exception {
    String sql = "SELECT * FROM live.github WHERE org='apache' LIMIT 15";
    List<QueryDataBatch> results = client.queryBuilder().sql(sql).results();
    assertEquals(3, results.size());

    int count = 0;
    for(QueryDataBatch b : results){
      count += b.getHeader().getRowCount();
      b.release();
    }
    assertEquals(3, results.size());
    assertEquals(15, count);
  }

  @Test
  public void simpleJSONPaginatorQuery() throws Exception {
    String sql = "SELECT * FROM `local`.`json_paginator` LIMIT 4";
    try (MockWebServer server = startServer()) {

      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON_PAGE1));
      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON_PAGE2));
      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON_PAGE3));

      List<QueryDataBatch> results = client.queryBuilder()
        .sql(sql)
        .results();

      int count = 0;
      for(QueryDataBatch b : results){
        count += b.getHeader().getRowCount();
        b.release();
      }
      assertEquals(2, results.size());
      assertEquals(4, count);
    }
  }

  @Test
  public void simpleJSONPaginatorQueryWithoutLimit() throws Exception {
    String sql = "SELECT * FROM `local`.`json_paginator`";
    try (MockWebServer server = startServer()) {

      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON_PAGE1));
      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON_PAGE2));
      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON_PAGE3));

      List<QueryDataBatch> results = client.queryBuilder()
        .sql(sql)
        .results();

      int count = 0;
      for(QueryDataBatch b : results){
        count += b.getHeader().getRowCount();
        b.release();
      }
      assertEquals(3, results.size());
      assertEquals(5, count);
    }
  }

  @Test
  public void simpleJSONPaginatorQueryWithoutLimitAndEvenResults() throws Exception {
    String sql = "SELECT * FROM `local`.`json_paginator`";
    try (MockWebServer server = startServer()) {

      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON_PAGE1));
      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_JSON_PAGE2));
      server.enqueue(new MockResponse().setResponseCode(404).setBody(""));

      List<QueryDataBatch> results = client.queryBuilder()
        .sql(sql)
        .results();

      int count = 0;
      for(QueryDataBatch b : results){
        count += b.getHeader().getRowCount();
        b.release();
      }
      assertEquals(2, results.size());
      assertEquals(4, count);
    }
  }

  @Test
  public void simpleCSVPaginatorQuery() throws Exception {
    String sql = "SELECT * FROM `local`.`csv_paginator` LIMIT 6";
    try (MockWebServer server = startServer()) {

      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_CSV_RESPONSE));
      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_CSV_RESPONSE_2));
      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_CSV_RESPONSE_3));

      List<QueryDataBatch> results = client.queryBuilder()
        .sql(sql)
        .results();

      int count = 0;
      for(QueryDataBatch b : results){
        count += b.getHeader().getRowCount();
        b.release();
      }
      assertEquals(6, count);
    }
  }

  @Test
  public void simpleCSVPaginatorQueryWithoutLimit() throws Exception {
    String sql = "SELECT * FROM `local`.`csv_paginator`";
    try (MockWebServer server = startServer()) {

      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_CSV_RESPONSE));
      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_CSV_RESPONSE_2));
      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_CSV_RESPONSE_3));
      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_CSV_RESPONSE_4));

      List<QueryDataBatch> results = client.queryBuilder()
        .sql(sql)
        .results();

      int count = 0;
      for(QueryDataBatch b : results){
        count += b.getHeader().getRowCount();
        b.release();
      }
      assertEquals(7, count);
    }
  }

  @Test
  public void simpleCSVPaginatorQueryWithoutLimitAndEvenResults() throws Exception {
    String sql = "SELECT * FROM `local`.`csv_paginator`";
    try (MockWebServer server = startServer()) {

      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_CSV_RESPONSE));
      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_CSV_RESPONSE_2));
      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_CSV_RESPONSE_3));
      server.enqueue(new MockResponse().setResponseCode(404).setBody(""));

      List<QueryDataBatch> results = client.queryBuilder()
        .sql(sql)
        .results();

      int count = 0;
      for(QueryDataBatch b : results){
        count += b.getHeader().getRowCount();
        b.release();
      }
      assertEquals(6, count);
    }
  }

  @Test
  public void simpleXMLPaginatorQuery() throws Exception {
    String sql = "SELECT * FROM `local`.`xml_paginator` LIMIT 6";
    try (MockWebServer server = startServer()) {

      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_XML_PAGE1));
      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_XML_PAGE2));
      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_XML_PAGE3));

      List<QueryDataBatch> results = client.queryBuilder()
        .sql(sql)
        .results();

      int count = 0;
      for(QueryDataBatch b : results){
        count += b.getHeader().getRowCount();
        b.release();
      }
      assertEquals(2, results.size());
      assertEquals(6, count);
    }
  }

  @Test
  public void simpleXMLPaginatorQueryWithoutLimit() throws Exception {
    String sql = "SELECT * FROM `local`.`xml_paginator`";
    try (MockWebServer server = startServer()) {

      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_XML_PAGE1));
      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_XML_PAGE2));
      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_XML_PAGE3));

      List<QueryDataBatch> results = client.queryBuilder()
        .sql(sql)
        .results();

      int count = 0;
      for(QueryDataBatch b : results){
        count += b.getHeader().getRowCount();
        b.release();
      }
      assertEquals(3, results.size());
      assertEquals(8, count);
    }
  }

  @Test
  public void testAggregateQuery() throws Exception {
    // Note that since the data arrives in multiple batches,
    // in order to access the contents, we have to receive the batches and parse them.
    // This is the case even with aggregate queries.

    String sql = "SELECT ZONE, COUNT(*) AS row_count FROM `local`.`xml_paginator` GROUP BY ZONE";
    try (MockWebServer server = startServer()) {

      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_XML_PAGE1));
      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_XML_PAGE2));
      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_XML_PAGE3));

      QueryRowSetIterator iterator = client.queryBuilder().sql(sql).rowSetIterator();

      TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("ZONE", MinorType.VARCHAR)
        .add("row_count", MinorType.BIGINT)
        .build();

      RowSet expectedFirstRow = new RowSetBuilder(client.allocator(), expectedSchema)
        .addRow("4", 5)
        .build();

      RowSet expectedSecondRow = new RowSetBuilder(client.allocator(), expectedSchema)
        .addRow("3", 3)
        .build();

      int count = 0;

      while (iterator.hasNext()) {
        DirectRowSet results = iterator.next();
        if (results.rowCount() > 0) {
          if (count == 0) {
            RowSetUtilities.verify(expectedFirstRow, results);
          } else if (count == 1) {
            RowSetUtilities.verify(expectedSecondRow, results);
          }
          count++;
        }
      }
    }
  }

  @Test
  public void simpleXMLPaginatorQueryWithoutLimitAndEvenResults() throws Exception {
    String sql = "SELECT * FROM `local`.`xml_paginator`";
    try (MockWebServer server = startServer()) {

      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_XML_PAGE1));
      server.enqueue(new MockResponse().setResponseCode(200).setBody(TEST_XML_PAGE2));
      server.enqueue(new MockResponse().setResponseCode(200).setBody(""));
      List<QueryDataBatch> results = client.queryBuilder()
        .sql(sql)
        .results();

      int count = 0;
      for(QueryDataBatch b : results){
        count += b.getHeader().getRowCount();
        b.release();
      }

      // Expects 2 batches with a total of six records.
      assertEquals(2, results.size());
      assertEquals(6, count);
    }
  }

  /**
   * Helper function to start the MockHTTPServer
   * @return Started Mock server
   * @throws IOException If the server cannot start, throws IOException
   */
  public MockWebServer startServer() throws IOException {
    MockWebServer server = new MockWebServer();
    server.start(MOCK_SERVER_PORT);
    return server;
  }
}
