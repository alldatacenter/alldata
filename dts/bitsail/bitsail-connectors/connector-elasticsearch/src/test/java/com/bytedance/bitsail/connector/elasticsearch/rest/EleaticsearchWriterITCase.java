/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.elasticsearch.rest;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.row.Row;
import com.bytedance.bitsail.connector.elasticsearch.option.ElasticsearchWriterOptions;
import com.bytedance.bitsail.connector.elasticsearch.rest.bulk.EsBulkListener;
import com.bytedance.bitsail.connector.elasticsearch.rest.bulk.EsBulkProcessorBuilder;
import com.bytedance.bitsail.connector.elasticsearch.rest.bulk.EsBulkRequestFailureHandler;
import com.bytedance.bitsail.connector.elasticsearch.sink.ElasticsearchWriter;
import com.bytedance.bitsail.test.connector.test.testcontainers.elasticsearch.ElasticsearchCluster;
import com.bytedance.bitsail.test.connector.test.utils.JobConfUtils;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class EleaticsearchWriterITCase {

  private final String docString = "{\n" +
      "    \"date\":\"20220810\",\n" +
      "    \"text_type\":\"text\",\n" +
      "    \"varchar_type\":\"varchar\",\n" +
      "    \"bigint_type\":\"bigint\",\n" +
      "    \"id\":100\n" +
      "}";
  private final Row row = new Row(new Object[] {
      "test_id", "varchar", "text", "bigint", "20220810"
  });
  private final String id = "test_id";
  private final String index = "es_index_test";

  private RestHighLevelClient client;
  private ElasticsearchCluster esCluster;

  private BitSailConfiguration jobConf;
  private IndexRequest indexRequest = new IndexRequest().index(index).id(id);
  private GetRequest getRequest = new GetRequest().index(index).id(id);

  @Before
  public void prepareEsCluster() throws Exception {
    esCluster = new ElasticsearchCluster();
    esCluster.startService();
    esCluster.checkClusterHealth();
    esCluster.createIndex(index);

    jobConf = BitSailConfiguration.newDefault();
    jobConf.set(ElasticsearchWriterOptions.ES_HOSTS,
        Collections.singletonList(esCluster.getHttpHostAddress()));

    client = new EsRestClientBuilder(jobConf).build();
  }

  @After
  public void closeEsCluster() throws Exception {
    client.close();
    esCluster.close();
  }

  @Test
  public void testRestClientBuilder() throws Exception {
    indexRequest.source(docString, XContentType.JSON);
    client.index(indexRequest, RequestOptions.DEFAULT);
    check(client, 100);
  }

  @Test
  public void testBulkProcessor() throws Exception {
    AtomicReference<Throwable> failureThrowable = new AtomicReference<>();
    EsBulkRequestFailureHandler failureHandler = new EsBulkRequestFailureHandler(jobConf);
    AtomicInteger pendingActions = new AtomicInteger(0);

    EsBulkProcessorBuilder builder = new EsBulkProcessorBuilder(jobConf);
    builder.setRestClient(client);
    builder.setListener(new EsBulkListener(failureHandler, failureThrowable, pendingActions));
    BulkProcessor bulkProcessor = builder.build();

    indexRequest.source(docString, XContentType.JSON);
    pendingActions.incrementAndGet();
    bulkProcessor.add(indexRequest);
    bulkProcessor.flush();
    while (pendingActions.get() > 0) {
      bulkProcessor.awaitClose(10, TimeUnit.SECONDS);
    }
    Assert.assertEquals(0, pendingActions.get());
    check(client, 100);
  }

  @Test
  public <CommitT, WriterStateT> void testEsWriter() throws Exception {
    BitSailConfiguration bitSailConf = JobConfUtils.fromClasspath("es_writer_parameter_test.json");
    bitSailConf.merge(jobConf, true);
    bitSailConf.set(ElasticsearchWriterOptions.ES_INDEX, index);

    ElasticsearchWriter<CommitT> writer = new ElasticsearchWriter<>(bitSailConf);
    writer.write(row);
    writer.flush(false);
    writer.close();

    check(client, id);
  }

  private void check(RestHighLevelClient client, Object id) throws Exception {
    GetResponse response = client.get(getRequest, RequestOptions.DEFAULT);

    Map<String, Object> source = response.getSource();
    Assert.assertEquals("20220810", source.get("date"));
    Assert.assertEquals("text", source.get("text_type"));
    Assert.assertEquals("varchar", source.get("varchar_type"));
    Assert.assertEquals("bigint", source.get("bigint_type"));
    Assert.assertEquals(id, source.get("id"));
  }
}
