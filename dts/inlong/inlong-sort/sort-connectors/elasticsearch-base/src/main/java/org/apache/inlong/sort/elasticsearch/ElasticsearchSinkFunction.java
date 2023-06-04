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

package org.apache.inlong.sort.elasticsearch;

import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.api.common.functions.Function;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.inlong.sort.base.metric.SinkMetricData;

import java.io.Serializable;

/**
 * Creates multiple ActionRequests from an element in a stream.
 *
 * This is used by sinks to prepare elements for sending them to Elasticsearch.
 *
 * @param <T> The type of the element handled by this {@code ElasticsearchSinkFunction}
 */
@PublicEvolving
public interface ElasticsearchSinkFunction<T, Request> extends Serializable, Function {

    /**
     * Initialization method for the function. It is called once before the actual working process
     * methods.
     */
    default void open(RuntimeContext ctx, SinkMetricData sinkMetricData) {
    }

    /**
     * Tear-down method for the function. It is called when the sink closes.
     */
    default void close() {
    }

    /**
     * Process the incoming element to produce multiple ActionsRequests. The
     * produced requests should be added to the provided {@link RequestIndexer}.
     *
     * @param element incoming element to process
     * @param ctx runtime context containing information about the sink instance
     * @param indexer request indexer that {@code ActionRequest} should be added to
     */
    void process(T element, RuntimeContext ctx, RequestIndexer<Request> indexer);

    default void initializeState(FunctionInitializationContext context) {
        // no initialization needed
    }

    default void snapshotState(FunctionSnapshotContext context) {

    }
}
