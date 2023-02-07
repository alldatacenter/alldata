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
package org.apache.drill.exec.store.openTSDB;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.exec.proto.UserBitShared.QueryType;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.test.ClientFixture;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.QueryTestUtil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.apache.drill.exec.store.openTSDB.TestDataHolder.DOWNSAMPLE_REQUEST_WITH_TAGS;
import static org.apache.drill.exec.store.openTSDB.TestDataHolder.DOWNSAMPLE_REQUEST_WTIHOUT_TAGS;
import static org.apache.drill.exec.store.openTSDB.TestDataHolder.END_PARAM_REQUEST_WITH_TAGS;
import static org.apache.drill.exec.store.openTSDB.TestDataHolder.END_PARAM_REQUEST_WTIHOUT_TAGS;
import static org.apache.drill.exec.store.openTSDB.TestDataHolder.POST_REQUEST_WITHOUT_TAGS;
import static org.apache.drill.exec.store.openTSDB.TestDataHolder.POST_REQUEST_WITH_TAGS;
import static org.apache.drill.exec.store.openTSDB.TestDataHolder.REQUEST_TO_NONEXISTENT_METRIC;
import static org.apache.drill.exec.store.openTSDB.TestDataHolder.SAMPLE_DATA_FOR_GET_TABLE_NAME_REQUEST;
import static org.apache.drill.exec.store.openTSDB.TestDataHolder.SAMPLE_DATA_FOR_GET_TABLE_REQUEST;
import static org.apache.drill.exec.store.openTSDB.TestDataHolder.SAMPLE_DATA_FOR_POST_DOWNSAMPLE_REQUEST_WITHOUT_TAGS;
import static org.apache.drill.exec.store.openTSDB.TestDataHolder.SAMPLE_DATA_FOR_POST_DOWNSAMPLE_REQUEST_WITH_TAGS;
import static org.apache.drill.exec.store.openTSDB.TestDataHolder.SAMPLE_DATA_FOR_POST_END_REQUEST_WITHOUT_TAGS;
import static org.apache.drill.exec.store.openTSDB.TestDataHolder.SAMPLE_DATA_FOR_POST_END_REQUEST_WITH_TAGS;
import static org.apache.drill.exec.store.openTSDB.TestDataHolder.SAMPLE_DATA_FOR_POST_REQUEST_WITH_TAGS;
import static org.junit.Assert.assertEquals;

public class TestOpenTSDBPlugin extends ClusterTest {

  private static int portNumber;

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(portNumber);

  @BeforeClass
  public static void setup() throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher));
    portNumber = QueryTestUtil.getFreePortNumber(10_000, 200);
    final StoragePluginRegistry pluginRegistry = cluster.drillbit().getContext().getStorage();
    OpenTSDBStoragePluginConfig storagePluginConfig =
        new OpenTSDBStoragePluginConfig(String.format("http://localhost:%s", portNumber));
    storagePluginConfig.setEnabled(true);
    pluginRegistry.put(OpenTSDBStoragePluginConfig.NAME, storagePluginConfig);
  }

  @Before
  public void init() {
    setupPostStubs();
    setupGetStubs();
  }

  private void setupGetStubs() {
    wireMockRule.stubFor(get(urlEqualTo("/api/suggest?type=metrics&max=" + Integer.MAX_VALUE))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(SAMPLE_DATA_FOR_GET_TABLE_NAME_REQUEST)));

    wireMockRule.stubFor(get(urlEqualTo("/api/query?start=47y-ago&m=sum:warp.speed.test"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(SAMPLE_DATA_FOR_GET_TABLE_REQUEST)
        ));
  }

  private void setupPostStubs() {
    wireMockRule.stubFor(post(urlEqualTo("/api/query"))
        .withRequestBody(equalToJson(POST_REQUEST_WITHOUT_TAGS))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(SAMPLE_DATA_FOR_GET_TABLE_REQUEST)));

    wireMockRule.stubFor(post(urlEqualTo("/api/query"))
        .withRequestBody(equalToJson(POST_REQUEST_WITH_TAGS))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(SAMPLE_DATA_FOR_POST_REQUEST_WITH_TAGS)));

    wireMockRule.stubFor(post(urlEqualTo("/api/query"))
        .withRequestBody(equalToJson(DOWNSAMPLE_REQUEST_WTIHOUT_TAGS))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(SAMPLE_DATA_FOR_POST_DOWNSAMPLE_REQUEST_WITHOUT_TAGS)));

    wireMockRule.stubFor(post(urlEqualTo("/api/query"))
        .withRequestBody(equalToJson(END_PARAM_REQUEST_WTIHOUT_TAGS))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(SAMPLE_DATA_FOR_POST_END_REQUEST_WITHOUT_TAGS)));

    wireMockRule.stubFor(post(urlEqualTo("/api/query"))
        .withRequestBody(equalToJson(DOWNSAMPLE_REQUEST_WITH_TAGS))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(SAMPLE_DATA_FOR_POST_DOWNSAMPLE_REQUEST_WITH_TAGS)));

    wireMockRule.stubFor(post(urlEqualTo("/api/query"))
        .withRequestBody(equalToJson(END_PARAM_REQUEST_WITH_TAGS))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(SAMPLE_DATA_FOR_POST_END_REQUEST_WITH_TAGS)));

    wireMockRule.stubFor(post(urlEqualTo("/api/query"))
        .withRequestBody(equalToJson(REQUEST_TO_NONEXISTENT_METRIC))
        .willReturn(aResponse()
            .withStatus(400)
            .withHeader("Content-Type", "application/json")
        ));
  }

  @Test
  public void testBasicQueryFromWithRequiredParams() throws Exception {
    String query =
            "select * from openTSDB.`(metric=warp.speed.test, start=47y-ago, aggregator=sum)`";
    assertEquals(18, runQuery(query));
  }

  @Test
  public void testBasicQueryGroupBy() throws Exception {
    String query =
            "select `timestamp`, sum(`aggregated value`) from openTSDB.`(metric=warp.speed.test, aggregator=sum, start=47y-ago)` group by `timestamp`";
    assertEquals(15, runQuery(query));
  }

  @Test
  public void testBasicQueryFromWithInterpolationParam() throws Exception {
    String query = "select * from openTSDB.`(metric=warp.speed.test, aggregator=sum, start=47y-ago, downsample=5y-avg)`";
    assertEquals(4, runQuery(query));
  }

  @Test
  public void testBasicQueryFromWithEndParam() throws Exception {
    String query = "select * from openTSDB.`(metric=warp.speed.test, aggregator=sum, start=47y-ago, end=1407165403000))`";
    assertEquals(5, runQuery(query));
  }

  @Test(expected = UserRemoteException.class)
  public void testBasicQueryWithoutTableName() throws Exception {
    runQuery("select * from openTSDB.``;");
  }

  @Test(expected = UserRemoteException.class)
  public void testBasicQueryWithNonExistentTableName() throws Exception {
    runQuery("select * from openTSDB.`warp.spee`");
  }

  @Test
  public void testPhysicalPlanSubmission() throws Exception {
    String query = "select * from openTSDB.`(metric=warp.speed.test, start=47y-ago, aggregator=sum)`";
    String plan = queryBuilder()
        .sql(query)
        .explainJson();
    queryBuilder()
        .query(QueryType.PHYSICAL, plan)
        .run();
  }

  @Test
  public void testDescribe() throws Exception {
    runQuery("use openTSDB");
    runQuery("describe `warp.speed.test`");
    assertEquals(1, runQuery("show tables"));
  }

  @Test
  public void testInformationSchemaWrongPluginConfig() throws Exception {
    try (ClusterFixture cluster = ClusterFixture.bareBuilder(dirTestWatcher).build();
         ClientFixture client = cluster.clientFixture()) {
      int portNumber = QueryTestUtil.getFreePortNumber(10_000, 200);
      final StoragePluginRegistry pluginRegistry = cluster.drillbit().getContext().getStorage();
      OpenTSDBStoragePluginConfig storagePluginConfig =
        new OpenTSDBStoragePluginConfig(String.format("http://localhost:%s/", portNumber));
      storagePluginConfig.setEnabled(true);
      pluginRegistry.put(OpenTSDBStoragePluginConfig.NAME, storagePluginConfig);
      String query = "select * from information_schema.`views`";
      client.queryBuilder()
        .sql(query)
        .run();
    }
  }

  private long runQuery(String query) throws Exception {
    return queryBuilder()
        .sql(query)
        .run()
        .recordCount();
  }
}
