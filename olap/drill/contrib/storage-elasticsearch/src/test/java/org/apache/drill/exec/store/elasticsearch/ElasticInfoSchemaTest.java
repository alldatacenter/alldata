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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ElasticInfoSchemaTest extends ClusterTest {

  private static final List<String> indexNames = new ArrayList<>();

  public static RestHighLevelClient restHighLevelClient;

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
    for (String indexName : indexNames) {
      restHighLevelClient.indices().delete(new DeleteIndexRequest(indexName), RequestOptions.DEFAULT);
    }
    TestElasticsearchSuite.tearDownCluster();
  }

  private static void prepareData() throws IOException {
    restHighLevelClient = new RestHighLevelClient(RestClient.builder(HttpHost.create(TestElasticsearchSuite.getAddress())));

    String indexName = "t1";
    indexNames.add(indexName);
    CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName);

    restHighLevelClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);

    XContentBuilder builder = XContentFactory.jsonBuilder();
    builder.startObject();
    builder.field("string_field", "a");
    builder.field("int_field", 123);
    builder.endObject();
    IndexRequest indexRequest = new IndexRequest(indexName).source(builder);
    restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
    restHighLevelClient.indices().refresh(new RefreshRequest(indexName), RequestOptions.DEFAULT);

    indexName = "t2";
    indexNames.add(indexName);
    createIndexRequest = new CreateIndexRequest(indexName);

    restHighLevelClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);

    builder = XContentFactory.jsonBuilder();
    builder.startObject();
    builder.field("another_int_field", 321);
    builder.field("another_string_field", "b");
    builder.endObject();
    indexRequest = new IndexRequest(indexName).source(builder);
    restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
    restHighLevelClient.indices().refresh(new RefreshRequest(indexName), RequestOptions.DEFAULT);
  }

  @Test
  public void testShowTables() throws Exception {
    testBuilder()
        .sqlQuery("show tables in elastic")
        .unOrdered()
        .baselineColumns("TABLE_SCHEMA", "TABLE_NAME")
        .baselineValues("elastic", "t1")
        .baselineValues("elastic", "t2")
        .go();
  }

  @Test
  public void testShowTablesLike() throws Exception {
    testBuilder()
        .sqlQuery("show tables in elastic like '%2%'")
        .unOrdered()
        .baselineColumns("TABLE_SCHEMA", "TABLE_NAME")
        .baselineValues("elastic", "t2")
        .go();
  }
}
