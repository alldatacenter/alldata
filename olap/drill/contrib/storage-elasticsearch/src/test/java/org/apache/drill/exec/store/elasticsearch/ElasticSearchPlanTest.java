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
package org.apache.drill.exec.store.elasticsearch;

import org.apache.drill.common.logical.security.PlainCredentialsProvider;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

public class ElasticSearchPlanTest extends ClusterTest {

  public static RestHighLevelClient restHighLevelClient;

  private static String indexName;

  @BeforeClass
  public static void init() throws Exception {
    TestElasticsearchSuite.initElasticsearch();
    startCluster(ClusterFixture.builder(dirTestWatcher));

    ElasticsearchStorageConfig config = new ElasticsearchStorageConfig(
        Collections.singletonList(TestElasticsearchSuite.getAddress()),
        null, null, null, PlainCredentialsProvider.EMPTY_CREDENTIALS_PROVIDER);
    config.setEnabled(true);
    cluster.defineStoragePlugin("elastic", config);

    prepareData();
  }

  @AfterClass
  public static void cleanUp() throws IOException {
    restHighLevelClient.indices().delete(new DeleteIndexRequest(indexName), RequestOptions.DEFAULT);
    TestElasticsearchSuite.tearDownCluster();
  }

  private static void prepareData() throws IOException {
    restHighLevelClient = new RestHighLevelClient(RestClient.builder(HttpHost.create(TestElasticsearchSuite.getAddress())));

    indexName = "nation";
    CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName);

    restHighLevelClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);

    XContentBuilder builder = XContentFactory.jsonBuilder();
    builder.startObject();
    builder.field("n_nationkey", 0);
    builder.field("n_name", "ALGERIA");
    builder.field("n_regionkey", 1);
    builder.endObject();
    IndexRequest indexRequest = new IndexRequest(indexName).source(builder);
    restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);

    restHighLevelClient.indices().refresh(new RefreshRequest(indexName), RequestOptions.DEFAULT);
  }

  @Test
  public void testProjectPushDown() throws Exception {
    queryBuilder()
        .sql("select n_name, n_nationkey from elastic.`nation`")
        .planMatcher()
        .include("ElasticsearchProject.*n_name.*n_nationkey")
        .exclude("\\*\\*")
        .match();
  }

  @Test
  public void testFilterPushDown() throws Exception {
    queryBuilder()
        .sql("select n_name, n_nationkey from elastic.`nation` where n_nationkey = 0")
        .planMatcher()
        .include("ElasticsearchFilter")
        .match();
  }

  @Test
  public void testFilterPushDownWithJoin() throws Exception {
    String query = "select * from elastic.`nation` e\n" +
        "join elastic.`nation` s on e.n_nationkey = s.n_nationkey where e.n_name = 'algeria'";

    queryBuilder()
        .sql(query)
        .planMatcher()
        .include("ElasticsearchFilter")
        .match();
  }

  @Test
  public void testAggregationPushDown() throws Exception {
    queryBuilder()
        .sql("select count(*) from elastic.`nation`")
        .planMatcher()
        .include("ElasticsearchAggregate.*COUNT")
        .match();
  }

  @Test
  public void testLimitWithSortPushDown() throws Exception {
    queryBuilder()
        .sql("select n_nationkey from elastic.`nation` order by n_name limit 3")
        .planMatcher()
        .include("ElasticsearchSort.*sort.*fetch")
        .match();
  }

  @Test
  public void testAggregationWithGroupByPushDown() throws Exception {
    queryBuilder()
        .sql("select sum(n_nationkey) from elastic.`nation` group by n_regionkey")
        .planMatcher()
        .include("ElasticsearchAggregate.*SUM")
        .match();
  }
}
