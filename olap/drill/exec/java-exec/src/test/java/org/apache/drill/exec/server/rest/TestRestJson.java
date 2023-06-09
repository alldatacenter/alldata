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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.proto.UserBitShared.QueryType;
import org.apache.drill.exec.store.easy.text.TextFormatPlugin.TextFormatConfig;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Simple sanity test of the Drill REST client query JSON feature.
 * In Drill 1.18, this test verified the original, buffered JSON
 * output. In Drill 1.19, it verifies the streamed output (though only
 * for a small query.) A large test is available below, but is normally
 * disabled.
 * <p>
 * Ad-hoc tests can be run by setting a breakpoint in one of the tests,
 * then run the test in the debugger to start the REST server. Use your
 * favorite browser-based tool:
 * <ul>
 * <li>Method: <tt>POST</tt></li>
 * <li>Custom header: <tt>content-type: application/json</tt></li>
 * <li>URL: <tt>http://localhost:8047/query.json</tt></li>
 * <li>Body: <pre><code>
 * {"query": "SELECT * FROM cp.`employee.json` LIMIT 20",
 *  "queryType":"SQL"}</code></pre><li>
 * </ul>
 */
public class TestRestJson extends ClusterTest {

  public static final MediaType JSON_MEDIA_TYPE =
      MediaType.parse("application/json");
  public static final int TIMEOUT = 3000; // for debugging

  private static int portNumber;

  protected static File testDir;
  private final OkHttpClient httpClient = new OkHttpClient.Builder()
      .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
      .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
      .readTimeout(TIMEOUT, TimeUnit.SECONDS)
      .build();

  private final ObjectMapper mapper = new ObjectMapper();
  private final FileVerifier verifier = new FileVerifier("/rest");

  @BeforeClass
  public static void setup() throws Exception {
    ClusterFixtureBuilder builder = new ClusterFixtureBuilder(dirTestWatcher)
        .configProperty(ExecConstants.HTTP_ENABLE, true)
        .configProperty(ExecConstants.HTTP_PORT_HUNT, true);
    startCluster(builder);

    portNumber = cluster.drillbit().getWebServerPort();

    // Set up CSV storage plugin using headers.
    TextFormatConfig csvFormat = new TextFormatConfig(
        null,
        null,  // line delimiter
        null,  // field delimiter
        null,  // quote
        null,  // escape
        null,  // comment
        false, // skipFirstLine,
        true   // extractHeader
        );

    testDir = cluster.makeDataDir("data", "csv", csvFormat);
  }

  @Test
  public void testSmallQuery() throws IOException {
    File outFile = new File(dirTestWatcher.getTmpDir(), "small.json");
    String sql = "SELECT * FROM cp.`employee.json` LIMIT 20";
    QueryWrapper query = new QueryWrapper(sql, QueryType.SQL.name(),
        "10", null, null, null);
    runQuery(query, outFile);
    verifier.verifyFileWithResource(outFile, "small.json");
  }

  @Test
  public void testGroupby() throws IOException {
    File outFile = new File(dirTestWatcher.getTmpDir(), "group.json");
    String sql = "SELECT position_title, COUNT(*) as pc FROM cp.`employee.json` GROUP BY position_title";
    QueryWrapper query = new QueryWrapper(sql, QueryType.SQL.name(),
      null, null, null, null);
    runQuery(query, outFile);
    verifier.verifyFileWithResource(outFile, "group.json");
  }

  @Test
  public void testNoLimit() throws IOException {
    File outFile = new File(dirTestWatcher.getTmpDir(), "cust20.json");
    String sql = "SELECT * FROM cp.`employee.json` LIMIT 20";
    QueryWrapper query = new QueryWrapper(sql, QueryType.SQL.name(),
        null, null, null, null);
    runQuery(query, outFile);
    verifier.verifyFileWithResource(outFile, "cust20.json");
  }

  @Test
  public void testFailedQuery() throws IOException {
    File outFile = new File(dirTestWatcher.getTmpDir(), "failed.json");
    String sql = "SELECT * FROM cp.`employee.json` LIMIT 20";
    QueryWrapper query = new QueryWrapper(sql, QueryType.SQL.name(),
        null, "bogusUser", null, null);
    runQuery(query, outFile);
    verifier.verifyFileWithResource(outFile, "failed.json");
  }

  @Test
  public void testQueryWithException() throws IOException {
    File outFile = new File(dirTestWatcher.getTmpDir(), "exception.json");
    String sql = "SELECT * FROM cp.`employee123321123321.json` LIMIT 20";
    QueryWrapper query = new QueryWrapper(sql, QueryType.SQL.name(),
        null, null, null, null);
    runQuery(query, outFile);
    verifier.verifyFileWithResource(outFile, "exception.json");
  }

  @Test
  public void testQueryWithVerboseException() throws IOException {
    File outFile = new File(dirTestWatcher.getTmpDir(), "verboseExc.json");
    String sql = "SELECT * FROM cp.`employee123321123321.json` LIMIT 20";
    QueryWrapper query = new QueryWrapper(sql, QueryType.SQL.name(),
        null, null, null, ImmutableMap.of(ExecConstants.ENABLE_REST_VERBOSE_ERRORS_KEY, "true"));
    runQuery(query, outFile);
    verifier.verifyFileWithResource(outFile, "verboseExc.json");
  }

  @SuppressWarnings("unused")
  @Test
  @Ignore("Manual test")
  public void testLargeQuery() throws Exception {
    String tableName = writeBigFile();
    if (false) {
      // Time COUNT(*) which reads and discards data. Establishes
      // a minimum query run time.
      long start = System.currentTimeMillis();
      String sql = String.format(
          "SELECT COUNT(*) FROM dfs.data.`%s`", tableName);
      System.out.println(
          client.queryBuilder().sql(sql).singletonLong());
      long end = System.currentTimeMillis();
      System.out.println(String.format("COUNT(*) - Elapsed: %d ms", end - start));
    }

    // Run the query and dump to a file to do a rough check
    // to see if all results appear.
    String sql = String.format(
        "SELECT * FROM dfs.data.`%s`", tableName);
    if (true) {
      File outFile = new File(dirTestWatcher.getTmpDir(), "big.json");
      QueryWrapper query = new QueryWrapper(sql, QueryType.SQL.name(),
          null, null, null, null);
      long start = System.currentTimeMillis();
      runQuery(query, outFile);
      long end = System.currentTimeMillis();
      System.out.println(String.format("Elapsed: %d ms", end - start));
      System.out.println(String.format("Input size: ~%d bytes",
          LINE_COUNT * FIELD_COUNT * FIELD_WIDTH));
      System.out.println(String.format("Query to file - Output size: %d bytes", outFile.length()));
    }

    // Run the query and discard results. Determines the overhead, relative
    // to COUNT(*) of serializing to JSON and sending results. Avoids the above
    // cost of writing to a file.
    if (false) {
      QueryWrapper query = new QueryWrapper(sql, QueryType.SQL.name(),
          null, null, null, null);
      long start = System.currentTimeMillis();
      runQuery(query);
      long end = System.currentTimeMillis();
      System.out.println(String.format("Query only - Elapsed: %d ms", end - start));
    }
  }

  private static final int LINE_COUNT = 1_000_000;
  private static final int FIELD_COUNT = 20; // Limited to 26
  private static final int FIELD_WIDTH = 100;

  private String writeBigFile() throws IOException {
    File dataFile = new File(testDir, "big.csv");
    try (PrintWriter writer = new PrintWriter(
        new BufferedWriter(new FileWriter(dataFile)))) {
      writer.print("id");
      for (int j = 0; j < FIELD_COUNT; j++) {
        writer.print(",");
        writer.print((char) ('a' + j));
      }
      writer.println();
      for (int i = 0; i < LINE_COUNT; i++) {
        writer.print(i + 1);
        for (int j = 0; j < FIELD_COUNT; j++) {
          writer.print(",");
          int c = 'A' + (i + j) % 26;
          writer.print(StringUtils.repeat((char) c, FIELD_WIDTH));
        }
        writer.println();
      }
    }
    return dataFile.getName();
  }

  /**
   * Run a query by hitting the REST endpoint via an HTTP request. The
   * query is JSON, serialized from {@link QueryWrapper} sent via POST.
   * The result is written to the given file.
   *
   * @param query query definition
   * @param destFile destination for the results
   */
  private void runQuery(QueryWrapper query, File destFile) throws IOException {
    ObjectWriter writer = mapper.writerFor(QueryWrapper.class);
    String json = writer.writeValueAsString(query);
    String url = String.format("http://localhost:%d/query.json", portNumber);
    Request request = new Request.Builder()
        .url(url)
        .post(RequestBody.create(json, JSON_MEDIA_TYPE))
        .build();
    try (Response response = httpClient.newCall(request).execute()) {
      try (InputStream inStream = response.body().byteStream()) {
        FileUtils.copyInputStreamToFile(inStream, destFile);
      }
    }
  }

  private void runQuery(QueryWrapper query) throws IOException {
    ObjectWriter writer = mapper.writerFor(QueryWrapper.class);
    String json = writer.writeValueAsString(query);
    String url = String.format("http://localhost:%d/query.json", portNumber);
    Request request = new Request.Builder()
        .url(url)
        .post(RequestBody.create(json, JSON_MEDIA_TYPE))
        .build();
    try (Response response = httpClient.newCall(request).execute()) {
      try (InputStream inStream = response.body().byteStream()) {
        IOUtils.skip(inStream, Integer.MAX_VALUE);
      }
    }
  }
}
