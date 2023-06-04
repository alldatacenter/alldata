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

package com.bytedance.bitsail.connector.elasticsearch;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.connector.elasticsearch.option.ElasticsearchWriterOptions;
import com.bytedance.bitsail.connector.elasticsearch.rest.EsRestClientBuilder;
import com.bytedance.bitsail.connector.legacy.fake.option.FakeReaderOptions;
import com.bytedance.bitsail.test.connector.test.EmbeddedFlinkCluster;
import com.bytedance.bitsail.test.connector.test.testcontainers.elasticsearch.ElasticsearchCluster;
import com.bytedance.bitsail.test.connector.test.utils.JobConfUtils;

import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

public class ElasticsearchSinkITCase {

  private final int totalCount = 300;
  private final String index = "es_index_test";
  private final CountRequest countRequest = new CountRequest(index);
  private ElasticsearchCluster esCluster;
  private RestHighLevelClient client;

  @Before
  public void prepareEsCluster() throws Exception {
    esCluster = new ElasticsearchCluster();
    esCluster.startService();
    esCluster.checkClusterHealth();
    esCluster.createIndex(index);

    BitSailConfiguration jobConf = BitSailConfiguration.newDefault();
    jobConf.set(ElasticsearchWriterOptions.ES_HOSTS,
        Collections.singletonList(esCluster.getHttpHostAddress()));
    client = new EsRestClientBuilder(jobConf).build();
  }

  @After
  public void closeEsCluster() {
    esCluster.close();
  }

  @Test
  public void testBatch() throws Exception {
    BitSailConfiguration jobConf = JobConfUtils.fromClasspath("es_sink_test.json");

    jobConf.set(FakeReaderOptions.TOTAL_COUNT, totalCount);
    jobConf.set(FakeReaderOptions.RATE, 1000);
    jobConf.set(ElasticsearchWriterOptions.ES_INDEX, index);
    jobConf.set(ElasticsearchWriterOptions.ES_HOSTS,
        Collections.singletonList(esCluster.getHttpHostAddress()));

    EmbeddedFlinkCluster.submitJob(jobConf);

    esCluster.flush();
    CountResponse countResponse = client.count(countRequest, RequestOptions.DEFAULT);
    Assert.assertEquals(totalCount, countResponse.getCount());
  }

  @Test
  public void testStreaming() throws Exception {
    BitSailConfiguration jobConf = JobConfUtils.fromClasspath("es_sink_test.json");

    jobConf.set(CommonOptions.JOB_TYPE, "STREAMING");
    jobConf.set(CommonOptions.CheckPointOptions.CHECKPOINT_ENABLE, true);
    jobConf.set(FakeReaderOptions.TOTAL_COUNT, totalCount);
    jobConf.set(ElasticsearchWriterOptions.ES_INDEX, index);
    jobConf.set(ElasticsearchWriterOptions.ES_HOSTS,
        Collections.singletonList(esCluster.getHttpHostAddress()));

    EmbeddedFlinkCluster.submitJob(jobConf);

    esCluster.flush();
    CountResponse countResponse = client.count(countRequest, RequestOptions.DEFAULT);
    Assert.assertEquals(totalCount, countResponse.getCount());
  }
}
