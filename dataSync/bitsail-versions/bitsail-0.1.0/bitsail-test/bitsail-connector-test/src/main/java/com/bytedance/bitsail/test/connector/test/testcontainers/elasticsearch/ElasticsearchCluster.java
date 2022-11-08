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

package com.bytedance.bitsail.test.connector.test.testcontainers.elasticsearch;

import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@SuppressWarnings("checkstyle:MagicNumber")
public class ElasticsearchCluster implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchCluster.class);

  private static final String ELASTICSEARCH_VERSION = "7.10.2";
  private static final DockerImageName ELASTICSEARCH_IMAGE = DockerImageName
      .parse("docker.elastic.co/elasticsearch/elasticsearch")
      .withTag(ELASTICSEARCH_VERSION);

  private ElasticsearchContainer esContainer;

  public void startService() {
    try {
      esContainer = new ElasticsearchContainer(ELASTICSEARCH_IMAGE)
          .withEnv("bootstrap.system_call_filter", "false")
          .withStartupTimeout(Duration.of(5, ChronoUnit.MINUTES));

      esContainer.start();
    } catch (Exception e) {
      LOG.error("failed to initialize Elasticsearch container.", e);
      if (Objects.nonNull(esContainer)) {
        esContainer.close();
      }
      throw new RuntimeException("failed to initialize Elasticsearch container.", e);
    }
  }

  @Override
  public void close() {
    esContainer.close();
  }

  public String getHttpHostAddress() {
    String host = esContainer.getHttpHostAddress();
    return host.replace("localhost", "127.0.0.1");
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  public void checkClusterHealth() throws IOException {
    RestClientBuilder builder = getRestClientBuilder();
    RestClient client = builder.build();

    Response response = client.performRequest(new Request("GET", "/_cluster/health"));
    Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    Assert.assertTrue(EntityUtils.toString(response.getEntity()).contains("cluster_name"));
  }

  public void createIndex(String indexName) throws IOException {
    RestClientBuilder builder = getRestClientBuilder();
    RestHighLevelClient client = new RestHighLevelClient(builder);

    client.indices().create(new CreateIndexRequest(indexName), RequestOptions.DEFAULT);
  }

  /**
   * Flush all indices in the cluster.
   */
  public void flush() throws IOException {
    RestClientBuilder builder = getRestClientBuilder();
    RestClient client = builder.build();

    client.performRequest(new Request("POST", "/_flush"));
    LOG.info("Flush all indices in cluster.");
  }

  private RestClientBuilder getRestClientBuilder() {
    return RestClient.builder(HttpHost.create(esContainer.getHttpHostAddress()));
  }
}