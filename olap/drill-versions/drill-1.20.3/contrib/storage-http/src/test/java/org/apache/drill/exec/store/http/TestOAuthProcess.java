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

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.drill.common.logical.security.CredentialsProvider;
import org.apache.drill.common.logical.security.PlainCredentialsProvider;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.oauth.PersistentTokenTable;
import org.apache.drill.exec.physical.rowSet.DirectRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.security.oauth.OAuthTokenCredentials;
import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Files;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestOAuthProcess extends ClusterTest {

  private static final Logger logger = LoggerFactory.getLogger(TestOAuthProcess.class);
  private static final int MOCK_SERVER_PORT = 47770;

  private static final int TIMEOUT = 30;
  private static final String CONNECTION_NAME = "localOauth";
  private final OkHttpClient httpClient = new OkHttpClient.Builder()
    .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
    .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
    .readTimeout(TIMEOUT, TimeUnit.SECONDS).build();

  private static String ACCESS_TOKEN_RESPONSE;
  private static String REFRESH_TOKEN_RESPONSE;
  private static String TEST_JSON_RESPONSE_WITH_DATATYPES;
  private static String hostname;

  @BeforeClass
  public static void setup() throws Exception {
    ACCESS_TOKEN_RESPONSE = Files.asCharSource(DrillFileUtils.getResourceAsFile("/data/oauth_access_token_response.json"), Charsets.UTF_8).read();
    REFRESH_TOKEN_RESPONSE = Files.asCharSource(DrillFileUtils.getResourceAsFile("/data/token_refresh.json"), Charsets.UTF_8).read();
    TEST_JSON_RESPONSE_WITH_DATATYPES = Files.asCharSource(DrillFileUtils.getResourceAsFile("/data/response2.json"), Charsets.UTF_8).read();

    ClusterFixtureBuilder builder = new ClusterFixtureBuilder(dirTestWatcher)
      .configProperty(ExecConstants.HTTP_ENABLE, true)
      .configProperty(ExecConstants.HTTP_PORT_HUNT, true);
    startCluster(builder);
    int portNumber = cluster.drillbit().getWebServerPort();
    hostname = "http://localhost:" + portNumber + "/storage/" + CONNECTION_NAME;

    Map<String, String> creds = new HashMap<>();
    creds.put("clientID", "12345");
    creds.put("clientSecret", "54321");
    creds.put("accessToken", null);
    creds.put("refreshToken", null);
    creds.put(OAuthTokenCredentials.TOKEN_URI, "http://localhost:" + MOCK_SERVER_PORT + "/get_access_token");

    CredentialsProvider credentialsProvider = new PlainCredentialsProvider(creds);

    HttpApiConfig connectionConfig = HttpApiConfig.builder()
      .url("http://localhost:" + MOCK_SERVER_PORT + "/getdata")
      .method("get")
      .requireTail(false)
      .inputType("json")
      .build();

    HttpOAuthConfig oAuthConfig = HttpOAuthConfig.builder()
      .callbackURL(hostname + "/update_oath2_authtoken")
      .build();

    Map<String, HttpApiConfig> configs = new HashMap<>();
    configs.put("test", connectionConfig);

    // Add storage plugin for test OAuth
    HttpStoragePluginConfig mockStorageConfigWithWorkspace =
      new HttpStoragePluginConfig(false, configs, TIMEOUT, "", 80, "", "", "",
        oAuthConfig, credentialsProvider);
    mockStorageConfigWithWorkspace.setEnabled(true);
    cluster.defineStoragePlugin("localOauth", mockStorageConfigWithWorkspace);
  }

  @Test
  public void testAccessToken() {
    String url = hostname + "/update_oath2_authtoken?code=ABCDEF";
    Request request = new Request.Builder().url(url).build();

    try (MockWebServer server = startServer()) {
      server.enqueue(new MockResponse().setResponseCode(200).setBody(ACCESS_TOKEN_RESPONSE));
      Response response = httpClient.newCall(request).execute();

      // Verify that the request succeeded w/o error
      assertEquals(200, response.code());

      // Verify that the access and refresh tokens were saved
      PersistentTokenTable tokenTable = ((HttpStoragePlugin) cluster
        .storageRegistry()
        .getPlugin("localOauth"))
        .getTokenTable();

      assertEquals("you_have_access", tokenTable.getAccessToken());
      assertEquals("refresh_me", tokenTable.getRefreshToken());

    } catch (Exception e) {
      logger.error(e.getMessage());
      fail();
    }
  }

  @Test
  public void testGetDataWithAuthentication() {
    String url = hostname + "/update_oath2_authtoken?code=ABCDEF";
    Request request = new Request.Builder().url(url).build();
    try (MockWebServer server = startServer()) {
      server.enqueue(new MockResponse().setResponseCode(200).setBody(ACCESS_TOKEN_RESPONSE));
      Response response = httpClient.newCall(request).execute();

      // Verify that the request succeeded w/o error
      assertEquals(200, response.code());

      // Verify that the access and refresh tokens were saved
      PersistentTokenTable tokenTable = ((HttpStoragePlugin) cluster.storageRegistry()
        .getPlugin("localOauth"))
        .getTokenRegistry()
        .getTokenTable("localOauth");

      assertEquals("you_have_access", tokenTable.getAccessToken());
      assertEquals("refresh_me", tokenTable.getRefreshToken());
      // Now execute a query and get query results.
      server.enqueue(new MockResponse()
        .setResponseCode(200)
        .setBody(TEST_JSON_RESPONSE_WITH_DATATYPES));

      String sql = "SELECT * FROM localOauth.test";
      DirectRowSet results = queryBuilder().sql(sql).rowSet();

      TupleMetadata expectedSchema = new SchemaBuilder()
        .add("col_1", MinorType.FLOAT8, DataMode.OPTIONAL)
        .add("col_2", MinorType.BIGINT, DataMode.OPTIONAL)
        .add("col_3", MinorType.VARCHAR, DataMode.OPTIONAL)
        .build();

      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addRow(1.0, 2, "3.0")
        .addRow(4.0, 5, "6.0")
        .build();

      RowSetUtilities.verify(expected, results);

    } catch (Exception e) {
      logger.error(e.getMessage());
      fail();
    }
  }

  @Test
  public void testGetDataWithTokenRefresh() {
    String url = hostname + "/update_oath2_authtoken?code=ABCDEF";
    Request request = new Request.Builder().url(url).build();
    try (MockWebServer server = startServer()) {
      server.enqueue(new MockResponse().setResponseCode(200).setBody(ACCESS_TOKEN_RESPONSE));
      Response response = httpClient.newCall(request).execute();

      // Verify that the request succeeded w/o error
      assertEquals(200, response.code());

      // Verify that the access and refresh tokens were saved
      PersistentTokenTable tokenTable = ((HttpStoragePlugin) cluster.storageRegistry().getPlugin("localOauth")).getTokenRegistry().getTokenTable("localOauth");

      assertEquals("you_have_access", tokenTable.getAccessToken());
      assertEquals("refresh_me", tokenTable.getRefreshToken());

      // Now execute a query and get a refresh token
      // The API should return a 401 error.  This should trigger Drill to automatically
      // fire off a second call with the refresh token and then a third request with the
      // new access token to obtain the actual data.
      server.enqueue(new MockResponse().setResponseCode(401).setBody("Access Denied"));
      server.enqueue(new MockResponse().setResponseCode(200).setBody(REFRESH_TOKEN_RESPONSE));
      server.enqueue(new MockResponse()
        .setResponseCode(200)
        .setBody(TEST_JSON_RESPONSE_WITH_DATATYPES));

      String sql = "SELECT * FROM localOauth.test";
      DirectRowSet results = queryBuilder().sql(sql).rowSet();

      // Verify that the access and refresh tokens were saved
      assertEquals("token 2.0", tokenTable.getAccessToken());
      assertEquals("refresh 2.0", tokenTable.getRefreshToken());

      TupleMetadata expectedSchema = new SchemaBuilder()
        .add("col_1", MinorType.FLOAT8, DataMode.OPTIONAL)
        .add("col_2", MinorType.BIGINT, DataMode.OPTIONAL)
        .add("col_3", MinorType.VARCHAR, DataMode.OPTIONAL)
        .build();

      RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
        .addRow(1.0, 2, "3.0")
        .addRow(4.0, 5, "6.0")
        .build();

      RowSetUtilities.verify(expected, results);

    } catch (Exception e) {
      logger.debug(e.getMessage());
      fail();
    }
  }

  /**
   * Helper function to start the MockHTTPServer
   * @return Started Mock server
   * @throws IOException If the server cannot start, throws IOException
   */
  public static MockWebServer startServer () throws IOException {
    MockWebServer server = new MockWebServer();
    server.start(MOCK_SERVER_PORT);
    return server;
  }
}
