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

import org.apache.flink.annotation.Internal;
import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.util.InstantiationUtil;
import org.apache.inlong.sort.base.dirty.DirtySinkHelper;
import org.apache.inlong.sort.base.metric.MetricOption;
import org.apache.inlong.sort.base.metric.MetricOption.RegisteredMetric;
import org.apache.inlong.sort.base.metric.MetricState;
import org.apache.inlong.sort.base.metric.SinkMetricData;
import org.apache.inlong.sort.base.metric.sub.SinkTableMetricData;
import org.apache.inlong.sort.base.util.MetricStateUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import static org.apache.flink.util.Preconditions.checkArgument;
import static org.apache.flink.util.Preconditions.checkNotNull;
import static org.apache.inlong.sort.base.Constants.DIRTY_BYTES_OUT;
import static org.apache.inlong.sort.base.Constants.DIRTY_RECORDS_OUT;
import static org.apache.inlong.sort.base.Constants.INLONG_METRIC_STATE_NAME;
import static org.apache.inlong.sort.base.Constants.NUM_BYTES_OUT;
import static org.apache.inlong.sort.base.Constants.NUM_RECORDS_OUT;

/**
 * Base class for all Flink Elasticsearch Sinks.
 *
 * <p>This class implements the common behaviour across Elasticsearch versions, such as the use of
 * an internal {@link BulkProcessor} to buffer multiple {@link Request}s before sending the
 * requests to the cluster, as well as passing input records to the user provided {@link
 * ElasticsearchSinkFunction} for processing.
 *
 * <p>The version specific API calls for different Elasticsearch versions should be defined by a
 * concrete implementation of a {@link ElasticsearchApiCallBridge}, which is provided to the
 * constructor of this class. This call bridge is used, for example, to create a Elasticsearch
 * {@link C}, handle failed item responses, etc.
 *
 * @param <T> Type of the elements handled by this sink
 * @param <C> Type of the Elasticsearch client, which implements {@link AutoCloseable}
 */
@Internal
public abstract class ElasticsearchSinkBase<T, Request, Builder, Listener, BulkItemResponse, BulkProcessor extends AutoCloseable, C extends AutoCloseable>
        extends
            RichSinkFunction<T>
        implements
            CheckpointedFunction {

    public static final String CONFIG_KEY_BULK_FLUSH_MAX_ACTIONS = "bulk.flush.max.actions";
    // ------------------------------------------------------------------------
    // Internal bulk processor configuration
    // ------------------------------------------------------------------------
    public static final String CONFIG_KEY_BULK_FLUSH_MAX_SIZE_MB = "bulk.flush.max.size.mb";
    public static final String CONFIG_KEY_BULK_FLUSH_INTERVAL_MS = "bulk.flush.interval.ms";
    public static final String CONFIG_KEY_BULK_FLUSH_BACKOFF_ENABLE = "bulk.flush.backoff.enable";
    public static final String CONFIG_KEY_BULK_FLUSH_BACKOFF_TYPE = "bulk.flush.backoff.type";
    public static final String CONFIG_KEY_BULK_FLUSH_BACKOFF_RETRIES = "bulk.flush.backoff.retries";
    public static final String CONFIG_KEY_BULK_FLUSH_BACKOFF_DELAY = "bulk.flush.backoff.delay";
    private static final long serialVersionUID = -1007596293618451942L;
    private final Integer bulkProcessorFlushMaxActions;
    private final Integer bulkProcessorFlushMaxSizeMb;
    private final Long bulkProcessorFlushIntervalMillis;
    private final BulkFlushBackoffPolicy bulkProcessorFlushBackoffPolicy;
    /**
     * The config map that contains configuration for the bulk flushing behaviours.
     *
     * <p>For {@link org.elasticsearch.client.transport.TransportClient} based implementations, this
     * config map would also contain Elasticsearch-shipped configuration, and therefore this config
     * map would also be forwarded when creating the Elasticsearch client.
     */
    private final Map<String, String> userConfig;
    /**
     * The function that is used to construct multiple {@link Request ActionRequests} from
     * each incoming element.
     */
    private final ElasticsearchSinkFunction<T, Request> elasticsearchSinkFunction;

    // ------------------------------------------------------------------------
    // User-facing API and configuration
    // ------------------------------------------------------------------------
    /**
     * User-provided handler for failed {@link Request ActionRequests}.
     */
    private final ActionRequestFailureHandler<Request> failureHandler;
    /**
     * Call bridge for different version-specific.
     */
    private final ElasticsearchApiCallBridge<Request, Builder, Listener, BulkItemResponse, BulkProcessor, C> callBridge;
    /**
     * This is set from inside the {@link Listener} if a {@link Throwable} was thrown
     * in callbacks and the user considered it should fail the sink via the
     * {@link ActionRequestFailureHandler#onFailure(Request, Throwable, int, RequestIndexer)} method.
     *
     * <p>Errors will be checked and rethrown before processing each input element, and when the
     * sink is closed.
     */
    private final AtomicReference<Throwable> failureThrowable = new AtomicReference<>();
    private final String inlongMetric;
    private final DirtySinkHelper<Object> dirtySinkHelper;
    private final String auditHostAndPorts;
    /**
     * Number of pending action requests not yet acknowledged by Elasticsearch. This value is
     * maintained only if {@link ElasticsearchSinkBase#flushOnCheckpoint} is {@code true}.
     *
     * <p>This is incremented whenever the user adds (or re-adds through the {@link
     * ActionRequestFailureHandler}) requests to the {@link RequestIndexer}.
     */
    private final AtomicLong numPendingRequests = new AtomicLong(0);

    // ------------------------------------------------------------------------
    // Internals for the Flink Elasticsearch Sink
    // ------------------------------------------------------------------------
    /**
     * If true, the producer will wait until all outstanding action requests have been sent to
     * Elasticsearch.
     */
    private boolean flushOnCheckpoint = true;
    /**
     * Provided to the user via the {@link ElasticsearchSinkFunction} to add ActionRequests
     */
    private transient RequestIndexer<Request> requestIndexer;
    /**
     * Provided to the {@link ActionRequestFailureHandler} to allow users to re-index failed
     * requests.
     */
    private transient BufferingNoOpRequestIndexer<Request> failureRequestIndexer;
    /**
     * Elasticsearch client created using the call bridge.
     */
    private transient C client;
    /**
     * Bulk processor to buffer and send requests to Elasticsearch, created using the client.
     */
    private transient BulkProcessor bulkProcessor;
    private SinkTableMetricData sinkMetricData;
    private transient ListState<MetricState> metricStateListState;
    private transient MetricState metricState;
    private final boolean multipleSink;

    public ElasticsearchSinkBase(
            ElasticsearchApiCallBridge<Request, Builder, Listener, BulkItemResponse, BulkProcessor, C> callBridge,
            Map<String, String> userConfig,
            ElasticsearchSinkFunction<T, Request> elasticsearchSinkFunction,
            ActionRequestFailureHandler<Request> failureHandler,
            String inlongMetric,
            DirtySinkHelper<Object> dirtySinkHelper,
            String auditHostAndPorts,
            boolean multipleSink) {
        this.inlongMetric = inlongMetric;
        this.dirtySinkHelper = dirtySinkHelper;
        this.callBridge = checkNotNull(callBridge);
        this.elasticsearchSinkFunction = checkNotNull(elasticsearchSinkFunction);
        this.failureHandler = checkNotNull(failureHandler);
        this.auditHostAndPorts = auditHostAndPorts;
        this.multipleSink = multipleSink;
        // we eagerly check if the user-provided sink function and failure handler is serializable;
        // otherwise, if they aren't serializable, users will merely get a non-informative error
        // message
        // "ElasticsearchSinkBase is not serializable"
        checkArgument(
                InstantiationUtil.isSerializable(elasticsearchSinkFunction),
                "The implementation of the provided ElasticsearchSinkFunction is not serializable. "
                        + "The object probably contains or references non-serializable fields.");
        checkArgument(
                InstantiationUtil.isSerializable(failureHandler),
                "The implementation of the provided ActionRequestFailureHandler is not serializable. "
                        + "The object probably contains or references non-serializable fields.");

        // extract and remove bulk processor related configuration from the user-provided config,
        // so that the resulting user config only contains configuration related to the
        // Elasticsearch client.
        checkNotNull(userConfig);
        // copy config so we can remove entries without side-effects
        userConfig = new HashMap<>(userConfig);
        ParameterTool params = ParameterTool.fromMap(userConfig);
        if (params.has(CONFIG_KEY_BULK_FLUSH_MAX_ACTIONS)) {
            bulkProcessorFlushMaxActions = params.getInt(CONFIG_KEY_BULK_FLUSH_MAX_ACTIONS);
            userConfig.remove(CONFIG_KEY_BULK_FLUSH_MAX_ACTIONS);
        } else {
            bulkProcessorFlushMaxActions = null;
        }
        if (params.has(CONFIG_KEY_BULK_FLUSH_MAX_SIZE_MB)) {
            bulkProcessorFlushMaxSizeMb = params.getInt(CONFIG_KEY_BULK_FLUSH_MAX_SIZE_MB);
            userConfig.remove(CONFIG_KEY_BULK_FLUSH_MAX_SIZE_MB);
        } else {
            bulkProcessorFlushMaxSizeMb = null;
        }
        if (params.has(CONFIG_KEY_BULK_FLUSH_INTERVAL_MS)) {
            bulkProcessorFlushIntervalMillis = params.getLong(CONFIG_KEY_BULK_FLUSH_INTERVAL_MS);
            userConfig.remove(CONFIG_KEY_BULK_FLUSH_INTERVAL_MS);
        } else {
            bulkProcessorFlushIntervalMillis = null;
        }
        boolean bulkProcessorFlushBackoffEnable =
                params.getBoolean(CONFIG_KEY_BULK_FLUSH_BACKOFF_ENABLE, true);
        userConfig.remove(CONFIG_KEY_BULK_FLUSH_BACKOFF_ENABLE);
        if (bulkProcessorFlushBackoffEnable) {
            this.bulkProcessorFlushBackoffPolicy = new BulkFlushBackoffPolicy();
            if (params.has(CONFIG_KEY_BULK_FLUSH_BACKOFF_TYPE)) {
                bulkProcessorFlushBackoffPolicy.setBackoffType(
                        FlushBackoffType.valueOf(params.get(CONFIG_KEY_BULK_FLUSH_BACKOFF_TYPE)));
                userConfig.remove(CONFIG_KEY_BULK_FLUSH_BACKOFF_TYPE);
            }
            if (params.has(CONFIG_KEY_BULK_FLUSH_BACKOFF_RETRIES)) {
                bulkProcessorFlushBackoffPolicy.setMaxRetryCount(
                        params.getInt(CONFIG_KEY_BULK_FLUSH_BACKOFF_RETRIES));
                userConfig.remove(CONFIG_KEY_BULK_FLUSH_BACKOFF_RETRIES);
            }
            if (params.has(CONFIG_KEY_BULK_FLUSH_BACKOFF_DELAY)) {
                bulkProcessorFlushBackoffPolicy.setDelayMillis(
                        params.getLong(CONFIG_KEY_BULK_FLUSH_BACKOFF_DELAY));
                userConfig.remove(CONFIG_KEY_BULK_FLUSH_BACKOFF_DELAY);
            }
        } else {
            bulkProcessorFlushBackoffPolicy = null;
        }
        this.userConfig = userConfig;
    }

    /**
     * Disable flushing on checkpoint. When disabled, the sink will not wait for all pending action
     * requests to be acknowledged by Elasticsearch on checkpoints.
     *
     * <p>NOTE: If flushing on checkpoint is disabled, the Flink Elasticsearch Sink does NOT provide
     * any strong guarantees for at-least-once delivery of action requests.
     */
    public void disableFlushOnCheckpoint() {
        this.flushOnCheckpoint = false;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        client = callBridge.createClient(userConfig);
        MetricOption metricOption = MetricOption.builder()
                .withInlongLabels(inlongMetric)
                .withAuditAddress(auditHostAndPorts)
                .withInitRecords(metricState != null ? metricState.getMetricValue(NUM_RECORDS_OUT) : 0L)
                .withInitBytes(metricState != null ? metricState.getMetricValue(NUM_BYTES_OUT) : 0L)
                .withInitDirtyRecords(metricState != null ? metricState.getMetricValue(DIRTY_RECORDS_OUT) : 0L)
                .withInitDirtyBytes(metricState != null ? metricState.getMetricValue(DIRTY_BYTES_OUT) : 0L)
                .withRegisterMetric(RegisteredMetric.ALL)
                .build();
        if (metricOption != null) {
            sinkMetricData = new SinkTableMetricData(metricOption, getRuntimeContext().getMetricGroup());
            if (multipleSink) {
                sinkMetricData.registerSubMetricsGroup(metricState);
            }
        }
        dirtySinkHelper.open(parameters);
        Listener listener = createListener();
        bulkProcessor = buildBulkProcessor(listener);
        requestIndexer = callBridge.createBulkProcessorIndexer(bulkProcessor, flushOnCheckpoint, numPendingRequests);
        callBridge.verifyClientConnection(client);
        failureRequestIndexer = new BufferingNoOpRequestIndexer<>();
        elasticsearchSinkFunction.open(getRuntimeContext(), sinkMetricData);
    }

    public abstract Listener createListener();

    @Override
    public void invoke(T value, Context context) {
        checkAsyncErrorsAndRequests();
        elasticsearchSinkFunction.process(value, getRuntimeContext(), requestIndexer);
    }

    @Override
    public void initializeState(FunctionInitializationContext context) throws Exception {
        if (this.inlongMetric != null) {
            this.metricStateListState = context.getOperatorStateStore().getUnionListState(
                    new ListStateDescriptor<>(
                            INLONG_METRIC_STATE_NAME, TypeInformation.of(new TypeHint<MetricState>() {
                            })));
        }
        if (context.isRestored()) {
            metricState = MetricStateUtils.restoreMetricState(metricStateListState,
                    getRuntimeContext().getIndexOfThisSubtask(), getRuntimeContext().getNumberOfParallelSubtasks());
        }
        elasticsearchSinkFunction.initializeState(context);
    }

    @Override
    public void snapshotState(FunctionSnapshotContext context) throws Exception {
        checkAsyncErrorsAndRequests();
        if (flushOnCheckpoint) {
            while (numPendingRequests.get() != 0) {
                flush(bulkProcessor);
                checkAsyncErrorsAndRequests();
            }
        }
        if (sinkMetricData != null && metricStateListState != null) {
            MetricStateUtils.snapshotMetricStateForSinkMetricData(metricStateListState, sinkMetricData,
                    getRuntimeContext().getIndexOfThisSubtask());
        }
        elasticsearchSinkFunction.snapshotState(context);
    }

    public abstract void flush(BulkProcessor bulkProcessor);

    @Override
    public void close() throws Exception {
        elasticsearchSinkFunction.close();
        if (bulkProcessor != null) {
            bulkProcessor.close();
            bulkProcessor = null;
        }
        if (client != null) {
            client.close();
            client = null;
        }
        callBridge.cleanup();
        // make sure any errors from callbacks are rethrown
        checkErrorAndRethrow();
    }

    /**
     * Build the {@link BulkProcessor}.
     *
     * <p>Note: this is exposed for testing purposes.
     */
    @VisibleForTesting
    protected BulkProcessor buildBulkProcessor(Listener listener) {
        checkNotNull(listener);
        Builder builder = callBridge.createBulkProcessorBuilder(client, listener);
        // if backoff retrying is disabled, bulkProcessorFlushBackoffPolicy will be null
        callBridge.configureBulkProcessorBackoff(
                builder, bulkProcessorFlushBackoffPolicy);
        /// This makes flush() blocking
        // builder.setConcurrentRequests(0);
        callBridge.configureBulkProcessor(builder, new BulkProcessorOptions(bulkProcessorFlushMaxActions,
                bulkProcessorFlushMaxSizeMb, bulkProcessorFlushIntervalMillis, 0));
        return callBridge.buildBulkProcessor(builder);
    }

    private void checkErrorAndRethrow() {
        Throwable cause = failureThrowable.get();
        if (cause != null) {
            throw new RuntimeException("An error occurred in ElasticsearchSink.", cause);
        }
    }

    private void checkAsyncErrorsAndRequests() {
        checkErrorAndRethrow();
        failureRequestIndexer.processBufferedRequests(requestIndexer);
    }

    @VisibleForTesting
    long getNumPendingRequests() {
        if (flushOnCheckpoint) {
            return numPendingRequests.get();
        } else {
            throw new UnsupportedOperationException(
                    "The number of pending requests is not maintained when flushing on checkpoint is disabled.");
        }
    }

    /**
     * Get pending requests for handle in subClass
     *
     * @return The numPendingRequests
     */
    public AtomicLong getPendingRequests() {
        return numPendingRequests;
    }

    public ElasticsearchApiCallBridge<Request, Builder, Listener, BulkItemResponse, BulkProcessor, C> getCallBridge() {
        return callBridge;
    }

    public AtomicReference<Throwable> getFailureThrowable() {
        return failureThrowable;
    }

    public boolean flushOnCheckpoint() {
        return flushOnCheckpoint;
    }

    public BufferingNoOpRequestIndexer<Request> getFailureRequestIndexer() {
        return failureRequestIndexer;
    }

    public ActionRequestFailureHandler<Request> getFailureHandler() {
        return failureHandler;
    }

    public DirtySinkHelper<Object> getDirtySinkHelper() {
        return dirtySinkHelper;
    }

    public SinkMetricData getSinkMetricData() {
        return sinkMetricData;
    }

    /**
     * Used to control whether the retry delay should increase exponentially or remain constant.
     */
    @PublicEvolving
    public enum FlushBackoffType {
        CONSTANT,
        EXPONENTIAL
    }

    /**
     * Provides a backoff policy for bulk requests. Whenever a bulk request is rejected due to
     * resource constraints (i.e. the client's internal thread pool is full), the backoff policy
     * decides how long the bulk processor will wait before the operation is retried internally.
     *
     * <p>This is a proxy for version specific backoff policies.
     */
    public static class BulkFlushBackoffPolicy implements Serializable {

        private static final long serialVersionUID = -6022851996101826049L;

        // the default values follow the Elasticsearch default settings for BulkProcessor
        private FlushBackoffType backoffType = FlushBackoffType.EXPONENTIAL;
        private int maxRetryCount = 8;
        private long delayMillis = 50;

        public FlushBackoffType getBackoffType() {
            return backoffType;
        }

        public void setBackoffType(FlushBackoffType backoffType) {
            this.backoffType = checkNotNull(backoffType);
        }

        public int getMaxRetryCount() {
            return maxRetryCount;
        }

        public void setMaxRetryCount(int maxRetryCount) {
            checkArgument(maxRetryCount >= 0);
            this.maxRetryCount = maxRetryCount;
        }

        public long getDelayMillis() {
            return delayMillis;
        }

        public void setDelayMillis(long delayMillis) {
            checkArgument(delayMillis >= 0);
            this.delayMillis = delayMillis;
        }
    }
}
