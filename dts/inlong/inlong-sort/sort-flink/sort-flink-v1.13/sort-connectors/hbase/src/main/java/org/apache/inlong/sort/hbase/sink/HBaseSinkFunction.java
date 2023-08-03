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

package org.apache.inlong.sort.hbase.sink;

import org.apache.inlong.sort.base.dirty.DirtyData;
import org.apache.inlong.sort.base.dirty.DirtyOptions;
import org.apache.inlong.sort.base.dirty.DirtyType;
import org.apache.inlong.sort.base.dirty.sink.DirtySink;
import org.apache.inlong.sort.base.metric.MetricOption;
import org.apache.inlong.sort.base.metric.MetricOption.RegisteredMetric;
import org.apache.inlong.sort.base.metric.MetricState;
import org.apache.inlong.sort.base.metric.SinkMetricData;
import org.apache.inlong.sort.base.util.CalculateObjectSizeUtils;
import org.apache.inlong.sort.base.util.MetricStateUtils;

import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.hbase.sink.HBaseMutationConverter;
import org.apache.flink.connector.hbase.util.HBaseConfigurationUtil;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.runtime.util.ExecutorThreadFactory;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.table.data.RowData;
import org.apache.flink.types.RowKind;
import org.apache.flink.util.Preconditions;
import org.apache.flink.util.StringUtils;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.TableNotFoundException;
import org.apache.hadoop.hbase.client.BufferedMutator;
import org.apache.hadoop.hbase.client.BufferedMutatorParams;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.RetriesExhaustedWithDetailsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.inlong.sort.base.Constants.DIRTY_BYTES_OUT;
import static org.apache.inlong.sort.base.Constants.DIRTY_RECORDS_OUT;
import static org.apache.inlong.sort.base.Constants.INLONG_METRIC_STATE_NAME;
import static org.apache.inlong.sort.base.Constants.NUM_BYTES_OUT;
import static org.apache.inlong.sort.base.Constants.NUM_RECORDS_OUT;

/**
 * The sink function for HBase.
 *
 * <p>This class leverage {@link BufferedMutator} to buffer multiple {@link
 * org.apache.hadoop.hbase.client.Mutation Mutations} before sending the requests to cluster. The
 * buffering strategy can be configured by {@code bufferFlushMaxSizeInBytes}, {@code
 * bufferFlushMaxMutations} and {@code bufferFlushIntervalMillis}.
 */
@Internal
public class HBaseSinkFunction<T> extends RichSinkFunction<T>
        implements
            CheckpointedFunction,
            BufferedMutator.ExceptionListener {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(HBaseSinkFunction.class);

    private final String hTableName;
    private final byte[] serializedConfig;

    private final long bufferFlushMaxSizeInBytes;
    private final long bufferFlushMaxMutations;
    private final long bufferFlushIntervalMillis;
    private final HBaseMutationConverter<T> mutationConverter;
    private final String inlongMetric;
    private final String inlongAudit;
    /**
     * This is set from inside the {@link BufferedMutator.ExceptionListener} if a {@link Throwable}
     * was thrown.
     *
     * <p>
     * Errors will be checked and rethrown before processing each input element, and when the
     * sink is closed.
     * </p>
     */
    private final AtomicReference<Throwable> failureThrowable = new AtomicReference<>();
    private transient ListState<MetricState> metricStateListState;
    private transient MetricState metricState;
    private SinkMetricData sinkMetricData;
    private transient Connection connection;
    private transient BufferedMutator mutator;
    private transient ScheduledExecutorService executor;
    private transient ScheduledFuture scheduledFuture;
    private transient AtomicLong numPendingRequests;
    private transient RuntimeContext runtimeContext;
    private transient volatile boolean closed = false;
    private Long dataSize = 0L;
    private Long rowSize = 0L;
    private final DirtyOptions dirtyOptions;
    private @Nullable final DirtySink<Object> dirtySink;

    public HBaseSinkFunction(
            String hTableName,
            org.apache.hadoop.conf.Configuration conf,
            HBaseMutationConverter<T> mutationConverter,
            long bufferFlushMaxSizeInBytes,
            long bufferFlushMaxMutations,
            long bufferFlushIntervalMillis,
            String inlongMetric,
            String inlongAudit,
            DirtyOptions dirtyOptions,
            @Nullable DirtySink<Object> dirtySink) {
        this.hTableName = hTableName;
        // Configuration is not serializable
        this.serializedConfig = HBaseConfigurationUtil.serializeConfiguration(conf);
        this.mutationConverter = mutationConverter;
        this.bufferFlushMaxSizeInBytes = bufferFlushMaxSizeInBytes;
        this.bufferFlushMaxMutations = bufferFlushMaxMutations;
        this.bufferFlushIntervalMillis = bufferFlushIntervalMillis;
        this.inlongMetric = inlongMetric;
        this.inlongAudit = inlongAudit;
        this.dirtyOptions = dirtyOptions;
        this.dirtySink = dirtySink;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        LOGGER.info("Start hbase sink function open ...");
        org.apache.hadoop.conf.Configuration config = prepareRuntimeConfiguration();
        try {
            this.runtimeContext = getRuntimeContext();
            MetricOption metricOption = MetricOption.builder()
                    .withInlongLabels(inlongMetric)
                    .withAuditAddress(inlongAudit)
                    .withInitRecords(metricState != null ? metricState.getMetricValue(NUM_RECORDS_OUT) : 0L)
                    .withInitBytes(metricState != null ? metricState.getMetricValue(NUM_BYTES_OUT) : 0L)
                    .withInitDirtyRecords(metricState != null ? metricState.getMetricValue(DIRTY_RECORDS_OUT) : 0L)
                    .withInitDirtyBytes(metricState != null ? metricState.getMetricValue(DIRTY_BYTES_OUT) : 0L)
                    .withRegisterMetric(RegisteredMetric.ALL)
                    .build();
            if (metricOption != null) {
                sinkMetricData = new SinkMetricData(metricOption, runtimeContext.getMetricGroup());
            }
            if (dirtySink != null) {
                dirtySink.open(parameters);
            }
            this.mutationConverter.open();
            this.numPendingRequests = new AtomicLong(0);

            if (null == connection) {
                this.connection = ConnectionFactory.createConnection(config);
            }
            // create a parameter instance, set the table name and custom listener reference.
            BufferedMutatorParams params =
                    new BufferedMutatorParams(TableName.valueOf(hTableName)).listener(this);
            if (bufferFlushMaxSizeInBytes > 0) {
                params.writeBufferSize(bufferFlushMaxSizeInBytes);
            }
            this.mutator = connection.getBufferedMutator(params);

            if (bufferFlushIntervalMillis > 0 && bufferFlushMaxMutations != 1) {
                this.executor =
                        Executors.newScheduledThreadPool(
                                1, new ExecutorThreadFactory("hbase-upsert-sink-flusher"));
                this.scheduledFuture =
                        this.executor.scheduleWithFixedDelay(
                                () -> {
                                    if (closed) {
                                        return;
                                    }
                                    reportMetricAfterFlush();
                                },
                                bufferFlushIntervalMillis,
                                bufferFlushIntervalMillis,
                                TimeUnit.MILLISECONDS);
            }
        } catch (TableNotFoundException tnfe) {
            LOGGER.error("The table " + hTableName + " not found ", tnfe);
            throw new RuntimeException("HBase table '" + hTableName + "' not found.", tnfe);
        } catch (IOException ioe) {
            LOGGER.error("Exception while creating connection to HBase.", ioe);
            throw new RuntimeException("Cannot create connection to HBase.", ioe);
        }
        LOGGER.info("End hbase sink function open.");
    }

    private org.apache.hadoop.conf.Configuration prepareRuntimeConfiguration() throws IOException {
        // create default configuration from current runtime env (`hbase-site.xml` in classpath)
        // first,
        // and overwrite configuration using serialized configuration from client-side env
        // (`hbase-site.xml` in classpath).
        // user params from client-side have the highest priority
        org.apache.hadoop.conf.Configuration runtimeConfig =
                HBaseConfigurationUtil.deserializeConfiguration(
                        serializedConfig, HBaseConfigurationUtil.getHBaseConfiguration());

        // do validation: check key option(s) in final runtime configuration
        if (StringUtils.isNullOrWhitespaceOnly(runtimeConfig.get(HConstants.ZOOKEEPER_QUORUM))) {
            LOGGER.error(
                    "Can not connect to HBase without {} configuration",
                    HConstants.ZOOKEEPER_QUORUM);
            throw new IOException(
                    "Check HBase configuration failed, lost: '"
                            + HConstants.ZOOKEEPER_QUORUM
                            + "'!");
        }

        return runtimeConfig;
    }

    private void checkErrorAndRethrow() {
        Throwable cause = failureThrowable.get();
        if (cause != null) {
            LOGGER.error("An error occurred in HBaseSink.", cause);
            throw new RuntimeException(cause);
        }
    }

    @Override
    public void invoke(T value, Context context) {
        checkErrorAndRethrow();
        RowData rowData = (RowData) value;
        if (RowKind.UPDATE_BEFORE != rowData.getRowKind()) {
            Mutation mutation = null;
            try {
                mutation = Preconditions.checkNotNull(mutationConverter.convertToMutation(value));
                rowSize++;
                dataSize = dataSize + CalculateObjectSizeUtils.getDataSize(value);
            } catch (Exception e) {
                LOGGER.error("Convert to mutation error", e);
                if (!dirtyOptions.ignoreDirty()) {
                    throw new RuntimeException(e);
                }
                sinkMetricData.invokeDirtyWithEstimate(value);
                if (dirtySink != null) {
                    DirtyData.Builder<Object> builder = DirtyData.builder();
                    try {
                        builder.setData(rowData)
                                .setDirtyType(DirtyType.UNDEFINED)
                                .setLabels(dirtyOptions.getLabels())
                                .setLogTag(dirtyOptions.getLogTag())
                                .setDirtyMessage(e.getMessage())
                                .setIdentifier(dirtyOptions.getIdentifier());
                        dirtySink.invoke(builder.build());
                    } catch (Exception ex) {
                        if (!dirtyOptions.ignoreSideOutputErrors()) {
                            throw new RuntimeException(ex);
                        }
                        LOGGER.warn("Dirty sink failed", ex);
                    }
                }
                return;
            }
            try {
                mutator.mutate(mutation);
            } catch (Exception e) {
                failureThrowable.compareAndSet(null, e);
            }
        } else {
            rowSize++;
            dataSize = dataSize + value.toString().getBytes(StandardCharsets.UTF_8).length;
        }
        // flush when the buffer number of mutations greater than the configured max size.
        if (bufferFlushMaxMutations > 0
                && numPendingRequests.incrementAndGet() >= bufferFlushMaxMutations) {
            reportMetricAfterFlush();
        }
    }

    private void reportMetricAfterFlush() {
        try {
            flush();
            if (sinkMetricData != null) {
                sinkMetricData.invoke(rowSize, dataSize);
            }
            resetStateAfterFlush();
        } catch (Exception e) {
            // fail the sink and skip the rest of the items
            // if the failure handler decides to throw an exception
            failureThrowable.compareAndSet(null, e);
        }
    }

    private void resetStateAfterFlush() {
        dataSize = 0L;
        rowSize = 0L;
    }

    private void flush() throws IOException {
        // BufferedMutator is thread-safe
        mutator.flush();
        numPendingRequests.set(0);
        checkErrorAndRethrow();
    }

    @Override
    public void close() throws Exception {
        closed = true;

        if (mutator != null) {
            try {
                mutator.close();
            } catch (IOException e) {
                LOGGER.warn("Exception occurs while closing HBase BufferedMutator.", e);
            }
            this.mutator = null;
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (IOException e) {
                LOGGER.warn("Exception occurs while closing HBase Connection.", e);
            }
            this.connection = null;
        }

        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            if (executor != null) {
                executor.shutdownNow();
            }
        }
    }

    @Override
    public void snapshotState(FunctionSnapshotContext context) throws Exception {
        while (numPendingRequests.get() != 0) {
            reportMetricAfterFlush();
        }
        if (sinkMetricData != null && metricStateListState != null) {
            MetricStateUtils.snapshotMetricStateForSinkMetricData(metricStateListState, sinkMetricData,
                    getRuntimeContext().getIndexOfThisSubtask());
        }
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
    }

    @Override
    public void onException(RetriesExhaustedWithDetailsException exception, BufferedMutator mutator)
            throws RetriesExhaustedWithDetailsException {
        // fail the sink and skip the rest of the items
        // if the failure handler decides to throw an exception
        failureThrowable.compareAndSet(null, exception);
    }
}
