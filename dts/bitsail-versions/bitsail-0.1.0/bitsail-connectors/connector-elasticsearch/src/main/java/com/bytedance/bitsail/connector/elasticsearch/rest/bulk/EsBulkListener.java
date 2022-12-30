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

import lombok.AllArgsConstructor;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.rest.RestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.bytedance.bitsail.connector.elasticsearch.base.EsConstants.ILLEGAL_REST_STATUS_CODE;

@AllArgsConstructor
public class EsBulkListener implements BulkProcessor.Listener {
  private static final Logger LOGGER = LoggerFactory.getLogger(EsBulkListener.class);

  private static final int FAILURE_RESPONSE_IDENTIFIER = 0;
  private static final int SUCCESS_RESPONSE_IDENTIFIER = 1;

  private final EsBulkRequestFailureHandler failureHandler;
  private final AtomicReference<Throwable> failureThrowable;
  private final AtomicInteger pendingActions;

  @Override
  public void beforeBulk(long executionId, BulkRequest request) {
    LOGGER.debug("before bulk: id={}, request={}", executionId, request);
  }

  @Override
  public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
    if (response.hasFailures()) {
      int successCount = 0;
      try {
        for (int i = 0; i < response.getItems().length; ++i) {
          successCount += handleBulkItemResponse(response.getItems()[i], request.requests().get(i));
        }
      } catch (Throwable t) {
        failureThrowable.compareAndSet(null, t);
      }
      pendingActions.getAndAdd(-successCount);
    } else {
      pendingActions.getAndAdd(-request.numberOfActions());
    }
  }

  @Override
  public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
    try {
      for (DocWriteRequest<?> req : request.requests()) {
        handleFailedRequest(req, failure, ILLEGAL_REST_STATUS_CODE);
      }
    } catch (Throwable t) {
      failureThrowable.compareAndSet(null, t);
    }
    pendingActions.getAndAdd(-request.numberOfActions());
  }

  /**
   * Handle each response.
   * @param response A response of a bulk requests.
   * @param actionRequest The corresponding request.
   * @return Returns 0 if it is a failure response. Otherwise 1.
   * @throws Throwable Exceptions thrown when handling failure response.
   */
  private int handleBulkItemResponse(BulkItemResponse response, DocWriteRequest<?> actionRequest) throws Throwable {
    if (response.isFailed() && Objects.nonNull(response.getFailure().getCause())) {
      Throwable failure = response.getFailure().getCause();
      RestStatus restStatus = response.getFailure().getStatus();
      int restStatusCode = Objects.isNull(restStatus) ? ILLEGAL_REST_STATUS_CODE : restStatus.getStatus();
      handleFailedRequest(actionRequest, failure, restStatusCode);
      return FAILURE_RESPONSE_IDENTIFIER;
    }
    return SUCCESS_RESPONSE_IDENTIFIER;
  }

  private void handleFailedRequest(DocWriteRequest<?> actionRequest, Throwable failure, int restStatusCode) throws Throwable {
    if (actionRequest instanceof ActionRequest) {
      failureHandler.onFailure((ActionRequest) actionRequest, failure, restStatusCode);
    } else {
      throw new IOException("Found request which is not ActionRequest: " + actionRequest.getClass());
    }
  }
}
