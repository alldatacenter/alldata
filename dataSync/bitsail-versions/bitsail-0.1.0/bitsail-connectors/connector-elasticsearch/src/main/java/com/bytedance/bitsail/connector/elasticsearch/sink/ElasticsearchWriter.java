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

package com.bytedance.bitsail.connector.elasticsearch.sink;

import com.bytedance.bitsail.base.connector.writer.v1.Writer;
import com.bytedance.bitsail.base.connector.writer.v1.state.EmptyState;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.row.Row;
import com.bytedance.bitsail.connector.elasticsearch.rest.EsRequestEmitter;
import com.bytedance.bitsail.connector.elasticsearch.rest.EsRestClientBuilder;
import com.bytedance.bitsail.connector.elasticsearch.rest.bulk.EsBulkListener;
import com.bytedance.bitsail.connector.elasticsearch.rest.bulk.EsBulkProcessorBuilder;
import com.bytedance.bitsail.connector.elasticsearch.rest.bulk.EsBulkRequestFailureHandler;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ElasticsearchWriter<CommitT> implements Writer<Row, CommitT, EmptyState> {
  private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchWriter.class);

  private final RestHighLevelClient restClient;
  private final AtomicReference<Throwable> failureThrowable;
  private final EsBulkRequestFailureHandler failureHandler;
  private final BulkProcessor bulkProcessor;
  private final EsRequestEmitter emitter;
  private final AtomicInteger pendingActions;

  public ElasticsearchWriter(BitSailConfiguration jobConf) {
    this.restClient = new EsRestClientBuilder(jobConf).build();
    this.failureThrowable = new AtomicReference<>();
    this.failureHandler = new EsBulkRequestFailureHandler(jobConf);
    this.pendingActions = new AtomicInteger(0);

    EsBulkProcessorBuilder builder = new EsBulkProcessorBuilder(jobConf);
    builder.setRestClient(restClient);
    builder.setListener(new EsBulkListener(failureHandler, failureThrowable, pendingActions));
    this.bulkProcessor = builder.build();

    this.emitter = new EsRequestEmitter(jobConf);
  }

  @Override
  public void write(Row element) {
    synchronized (this) {
      checkAsyncErrorsAndRequests();
      emitter.emit(element, bulkProcessor);
      pendingActions.getAndIncrement();
    }
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  @Override
  public void flush(boolean endOfInput) {
    synchronized (this) {
      checkAsyncErrorsAndRequests();
      while (pendingActions.get() != 0) {
        bulkProcessor.flush();
        checkAsyncErrorsAndRequests();
        try {
          TimeUnit.MILLISECONDS.sleep(10);
        } catch (Exception ignored) {
          //ignore
        }
      }
    }
  }

  @Override
  public List<CommitT> prepareCommit() {
    return Collections.emptyList();
  }

  @Override
  public List<EmptyState> snapshotState(long checkpointId) {
    this.flush(false);
    return Collections.emptyList();
  }

  @Override
  public void close() throws IOException {
    bulkProcessor.close();
    restClient.close();
    checkErrorAndRethrow();
  }

  private void checkErrorAndRethrow() {
    Throwable cause = failureThrowable.get();
    if (Objects.nonNull(cause)) {
      throw new RuntimeException("An error occurred in ElasticsearchWriter.", cause);
    }
  }

  private void checkAsyncErrorsAndRequests() {
    checkErrorAndRethrow();
    List<ActionRequest> failedRequest = failureHandler.getBufferedFailedRequest();
    failedRequest.forEach(actionRequest -> emitter.emit(actionRequest, bulkProcessor));
  }
}
