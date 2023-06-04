/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.elasticsearch6.utils;

import org.apache.flink.util.ExceptionUtils;
import org.apache.inlong.sort.elasticsearch.ActionRequestFailureHandler;
import org.apache.inlong.sort.elasticsearch.RequestIndexer;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.common.util.concurrent.EsRejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An ActionRequestFailureHandler that re-adds requests that failed
 * due to temporary {@link EsRejectedExecutionException}
 * (which means that Elasticsearch node queues are currently full),
 * and fails for all other failures.
 * Copies from {@link org.apache.flink.streaming.connectors.elasticsearch.util.RetryRejectedExecutionFailureHandler}
 */
public class RetryRejectedExecutionFailureHandler implements ActionRequestFailureHandler<DocWriteRequest<?>> {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryRejectedExecutionFailureHandler.class);

    @Override
    public void onFailure(DocWriteRequest<?> actionRequest, Throwable failure, int restStatusCode,
            RequestIndexer<DocWriteRequest<?>> indexer) throws Throwable {
        LOGGER.error("Failed Elasticsearch item request: {}", failure.getMessage(), failure);
        if (ExceptionUtils.findThrowable(failure, EsRejectedExecutionException.class).isPresent()) {
            indexer.add(actionRequest);
        } else {
            // rethrow all other failures
            throw failure;
        }
    }
}
