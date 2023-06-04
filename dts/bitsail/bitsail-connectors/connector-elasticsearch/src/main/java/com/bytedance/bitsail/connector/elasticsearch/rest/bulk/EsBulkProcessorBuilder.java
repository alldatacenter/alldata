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

package com.bytedance.bitsail.connector.elasticsearch.rest.bulk;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.util.Preconditions;
import com.bytedance.bitsail.connector.elasticsearch.option.ElasticsearchWriterOptions;

import lombok.Setter;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static com.bytedance.bitsail.connector.elasticsearch.base.EsConstants.BACKOFF_POLICY_CONSTANT;
import static com.bytedance.bitsail.connector.elasticsearch.base.EsConstants.BACKOFF_POLICY_EXPONENTIAL;
import static com.bytedance.bitsail.connector.elasticsearch.base.EsConstants.BACKOFF_POLICY_NONE;

@Setter
public class EsBulkProcessorBuilder {

  private final int bulkFlushMaxActions;
  private final int bulkFlushMaxMb;
  private final long bulkFlushInterval;
  private final BackoffPolicy backoffPolicy;

  private RestHighLevelClient restClient;
  private BulkProcessor.Listener listener;

  public EsBulkProcessorBuilder(BitSailConfiguration jobConf) {
    this.bulkFlushMaxActions = jobConf.get(ElasticsearchWriterOptions.BULK_FLUSH_MAX_ACTIONS);
    this.bulkFlushMaxMb = jobConf.get(ElasticsearchWriterOptions.BULK_FLUSH_MAX_SIZE_MB);
    this.bulkFlushInterval = jobConf.get(ElasticsearchWriterOptions.BULK_FLUSH_INTERVAL_MS);
    this.backoffPolicy = initBackoffPolicy(jobConf);
  }

  public BulkProcessor build() {
    Preconditions.checkNotNull(restClient, "RestHighLevelClient is not initialized!");
    Preconditions.checkNotNull(listener, "BulkProcessor listener is not initialized!");

    BiConsumer<BulkRequest, ActionListener<BulkResponse>> bulkConsumer =
        ((bulkRequest, bulkResponseActionListener) -> restClient.bulkAsync(bulkRequest, RequestOptions.DEFAULT, bulkResponseActionListener));

    BulkProcessor.Builder builder = BulkProcessor.builder(bulkConsumer, listener);
    builder.setBulkActions(bulkFlushMaxActions);
    builder.setBulkSize(new ByteSizeValue(bulkFlushMaxMb, ByteSizeUnit.MB));
    builder.setFlushInterval(new TimeValue(bulkFlushInterval, TimeUnit.MILLISECONDS));
    builder.setBackoffPolicy(backoffPolicy);

    return builder.build();
  }

  private BackoffPolicy initBackoffPolicy(BitSailConfiguration jobConf) {
    String backoffPolicyType = jobConf.get(ElasticsearchWriterOptions.BULK_BACKOFF_POLICY);
    Integer backoffDelay = jobConf.get(ElasticsearchWriterOptions.BULK_BACKOFF_DELAY_MS);
    Integer backoffRetryNum = jobConf.get(ElasticsearchWriterOptions.BULK_BACKOFF_MAX_RETRY_COUNT);

    switch (backoffPolicyType) {
      case BACKOFF_POLICY_CONSTANT:
        return BackoffPolicy.constantBackoff(new TimeValue(backoffDelay, TimeUnit.MILLISECONDS), backoffRetryNum);
      case BACKOFF_POLICY_EXPONENTIAL:
        return BackoffPolicy.exponentialBackoff(new TimeValue(backoffDelay, TimeUnit.MILLISECONDS), backoffRetryNum);
      case BACKOFF_POLICY_NONE:
        return BackoffPolicy.noBackoff();
      default:
        throw new BitSailException(CommonErrorCode.CONFIG_ERROR, "Found un-recognized backoff policy type: " + backoffPolicyType);
    }
  }
}
