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

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.elasticsearch.option.ElasticsearchWriterOptions;

import org.elasticsearch.action.ActionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EsBulkRequestFailureHandler implements Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(EsBulkRequestFailureHandler.class);

  private final int ignoreFailureThreshold;
  private final ConcurrentLinkedQueue<ActionRequest> bufferedRequests;
  private int ignoreFailureCount;

  public EsBulkRequestFailureHandler(BitSailConfiguration jobConf) {
    this.ignoreFailureThreshold = jobConf.get(ElasticsearchWriterOptions.MAX_IGNORE_FAILED_REQUEST_THRESHOLD);
    this.ignoreFailureCount = 0;
    this.bufferedRequests = new ConcurrentLinkedQueue<>();
  }

  public void onFailure(ActionRequest actionRequest, Throwable failure, int restStatusCode) throws Throwable {
    LOG.error("Found failed action request. RestStatusCode:[{}], ActionRequest:[{}], Cause:[{}]",
        restStatusCode, actionRequest, failure.getMessage(), failure);
    bufferedRequests.add(actionRequest);
    if (++ignoreFailureCount > ignoreFailureThreshold) {
      throw new IOException("failed to handle request: " + actionRequest, failure);
    }
  }

  public List<ActionRequest> getBufferedFailedRequest() {
    List<ActionRequest> requests = new ArrayList<>(bufferedRequests);
    bufferedRequests.clear();
    return requests;
  }
}
