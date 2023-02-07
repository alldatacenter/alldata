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
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.apache.drill.test.TestBuilder.listOf;
import static org.apache.drill.test.TestBuilder.mapOf;

public class ElasticComplexTypesTest extends ClusterTest {

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
    restHighLevelClient = new RestHighLevelClient(
      RestClient.builder(HttpHost.create(TestElasticsearchSuite.elasticsearch.getHttpHostAddress())));

    String indexName = "arr";
    indexNames.add(indexName);
    CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName);

    restHighLevelClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);

    XContentBuilder builder = XContentFactory.jsonBuilder();
    builder.startObject();
    builder.field("string_arr", Arrays.asList("a", "b", "c", "d"));
    builder.field("int_arr", Arrays.asList(1, 2, 3, 4, 0));
    builder.field("nest_int_arr", Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4, 0)));
    builder.endObject();
    IndexRequest indexRequest = new IndexRequest(indexName).source(builder);
    restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
    restHighLevelClient.indices().refresh(new RefreshRequest(indexName), RequestOptions.DEFAULT);

    indexName = "map";
    indexNames.add(indexName);
    createIndexRequest = new CreateIndexRequest(indexName);

    restHighLevelClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);

    builder = XContentFactory.jsonBuilder();
    builder.startObject();
    builder.field("prim_field", 321);
    builder.field("nest_field", ImmutableMap.of("a", 123, "b", "abc"));
    builder.field("more_nest_field", ImmutableMap.of("a", 123, "b", ImmutableMap.of("c", "abc")));
    builder.field("map_arr", Collections.singletonList(ImmutableMap.of("a", 123, "b", ImmutableMap.of("c", "abc"))));
    builder.endObject();
    indexRequest = new IndexRequest(indexName).source(builder);
    restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
    restHighLevelClient.indices().refresh(new RefreshRequest(indexName), RequestOptions.DEFAULT);
  }

  @Test
  public void testSelectStarWithArray() throws Exception {
    testBuilder()
        .sqlQuery("select * from elastic.arr")
        .unOrdered()
        .baselineColumns("string_arr", "int_arr", "nest_int_arr")
        .baselineValues(listOf("a", "b", "c", "d"), listOf(1, 2, 3, 4, 0),
            listOf(listOf(1, 2), listOf(3, 4, 0)))
        .go();
  }

  @Test
  public void testSelectArrayElem() throws Exception {
    testBuilder()
        .sqlQuery("select string_arr[0] c1, int_arr[1] c2, nest_int_arr[0][1] c3 from elastic.arr")
        .unOrdered()
        .baselineColumns("c1", "c2", "c3")
        .baselineValues("a", 2, 2)
        .go();
  }

  @Test
  public void testSelectStarWithJson() throws Exception {
    testBuilder()
        .sqlQuery("select * from elastic.map")
        .unOrdered()
        .baselineColumns("prim_field", "nest_field", "more_nest_field", "map_arr")
        .baselineValues(321, mapOf("a", 123, "b", "abc"),
            mapOf("a", 123, "b", mapOf("c", "abc")),
            listOf(mapOf("a", 123, "b", mapOf("c", "abc"))))
        .go();
  }

  @Test
  public void testSelectNestedFields() throws Exception {
    testBuilder()
        .sqlQuery("select m.nest_field.a a, m.nest_field.b b, m.more_nest_field.b.c c, map_arr[0].b.c d from elastic.map m")
        .unOrdered()
        .baselineColumns("a", "b", "c", "d")
        .baselineValues(123, "abc", "abc", "abc")
        .go();
  }
}
