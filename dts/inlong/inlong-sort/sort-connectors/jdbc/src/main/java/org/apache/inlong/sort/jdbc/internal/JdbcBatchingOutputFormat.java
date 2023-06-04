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

package org.apache.inlong.sort.jdbc.internal;

import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcStatementBuilder;
import org.apache.flink.connector.jdbc.internal.connection.JdbcConnectionProvider;
import org.apache.flink.connector.jdbc.internal.connection.SimpleJdbcConnectionProvider;
import org.apache.flink.connector.jdbc.internal.converter.JdbcRowConverter;
import org.apache.flink.connector.jdbc.internal.executor.JdbcBatchStatementExecutor;
import org.apache.flink.connector.jdbc.internal.executor.TableBufferReducedStatementExecutor;
import org.apache.flink.connector.jdbc.internal.executor.TableBufferedStatementExecutor;
import org.apache.flink.connector.jdbc.internal.executor.TableSimpleStatementExecutor;
import org.apache.flink.connector.jdbc.internal.options.JdbcDmlOptions;
import org.apache.flink.connector.jdbc.internal.options.JdbcOptions;
import org.apache.flink.connector.jdbc.statement.FieldNamedPreparedStatementImpl;
import org.apache.flink.connector.jdbc.statement.StatementFactory;
import org.apache.flink.connector.jdbc.utils.JdbcUtils;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.runtime.util.ExecutorThreadFactory;
import org.apache.flink.table.data.RowData;
import org.apache.flink.types.Row;
import org.apache.flink.types.RowKind;
import org.apache.flink.util.Preconditions;
import org.apache.inlong.sort.base.dirty.DirtyData;
import org.apache.inlong.sort.base.dirty.DirtyOptions;
import org.apache.inlong.sort.base.dirty.DirtySinkHelper;
import org.apache.inlong.sort.base.dirty.DirtyType;
import org.apache.inlong.sort.base.dirty.sink.DirtySink;
import org.apache.inlong.sort.base.metric.MetricOption;
import org.apache.inlong.sort.base.metric.MetricOption.RegisteredMetric;
import org.apache.inlong.sort.base.metric.MetricState;
import org.apache.inlong.sort.base.metric.SinkMetricData;
import org.apache.inlong.sort.base.util.CalculateObjectSizeUtils;
import org.apache.inlong.sort.base.util.MetricStateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.apache.flink.connector.jdbc.utils.JdbcUtils.setRecordToStatement;
import static org.apache.flink.util.Preconditions.checkNotNull;
import static org.apache.inlong.sort.base.Constants.DIRTY_BYTES_OUT;
import static org.apache.inlong.sort.base.Constants.DIRTY_RECORDS_OUT;
import static org.apache.inlong.sort.base.Constants.INLONG_METRIC_STATE_NAME;
import static org.apache.inlong.sort.base.Constants.NUM_BYTES_OUT;
import static org.apache.inlong.sort.base.Constants.NUM_RECORDS_OUT;

/**
 * A JDBC outputFormat that supports batching records before writing records to database.
 * Add an option `inlong.metric` to support metrics.
 */
public class JdbcBatchingOutputFormat<In, JdbcIn, JdbcExec extends JdbcBatchStatementExecutor<JdbcIn>>
        extends
            AbstractJdbcOutputFormat<In> {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(JdbcBatchingOutputFormat.class);
    private final JdbcExecutionOptions executionOptions;
    private final StatementExecutorFactory<JdbcExec> statementExecutorFactory;
    private final RecordExtractor<In, JdbcIn> jdbcRecordExtractor;
    private final String inlongMetric;
    private final String auditHostAndPorts;
    private transient JdbcExec jdbcStatementExecutor;
    private transient int batchCount = 0;
    private transient volatile boolean closed = false;
    private transient ScheduledExecutorService scheduler;
    private transient ScheduledFuture<?> scheduledFuture;
    private transient volatile Exception flushException;
    private transient RuntimeContext runtimeContext;

    private transient ListState<MetricState> metricStateListState;
    private transient MetricState metricState;
    private SinkMetricData sinkMetricData;
    private Long dataSize = 0L;
    private Long rowSize = 0L;

    private final DirtyOptions dirtyOptions;
    private @Nullable final DirtySink<Object> dirtySink;

    public JdbcBatchingOutputFormat(
            @Nonnull JdbcConnectionProvider connectionProvider,
            @Nonnull JdbcExecutionOptions executionOptions,
            @Nonnull StatementExecutorFactory<JdbcExec> statementExecutorFactory,
            @Nonnull RecordExtractor<In, JdbcIn> recordExtractor,
            String inlongMetric,
            String auditHostAndPorts,
            DirtyOptions dirtyOptions,
            @Nullable DirtySink<Object> dirtySink) {
        super(connectionProvider);
        this.executionOptions = checkNotNull(executionOptions);
        this.statementExecutorFactory = checkNotNull(statementExecutorFactory);
        this.jdbcRecordExtractor = checkNotNull(recordExtractor);
        this.inlongMetric = inlongMetric;
        this.auditHostAndPorts = auditHostAndPorts;
        this.dirtyOptions = dirtyOptions;
        this.dirtySink = dirtySink;
    }

    public static Builder builder() {
        return new Builder();
    }

    static JdbcBatchStatementExecutor<Row> createSimpleRowExecutor(
            String sql, int[] fieldTypes, boolean objectReuse) {
        return JdbcBatchStatementExecutor.simple(
                sql,
                createRowJdbcStatementBuilder(fieldTypes),
                objectReuse ? Row::copy : Function.identity());
    }

    /**
     * Creates a {@link JdbcStatementBuilder} for {@link Row} using the provided SQL types array.
     * Uses {@link JdbcUtils#setRecordToStatement}
     */
    static JdbcStatementBuilder<Row> createRowJdbcStatementBuilder(int[] types) {
        return (st, record) -> setRecordToStatement(st, types, record);
    }

    /**
     * Connects to the target database and initializes the prepared statement.
     *
     * @param taskNumber The number of the parallel instance.
     */
    @Override
    public void open(int taskNumber, int numTasks) throws IOException {
        super.open(taskNumber, numTasks);
        this.runtimeContext = getRuntimeContext();
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
            sinkMetricData = new SinkMetricData(metricOption, runtimeContext.getMetricGroup());
        }
        if (dirtySink != null) {
            try {
                dirtySink.open(new Configuration());
            } catch (Exception e) {
                throw new IOException("failed to open dirty sink");
            }
        }
        jdbcStatementExecutor = createAndOpenStatementExecutor(statementExecutorFactory);
        if (executionOptions.getBatchIntervalMs() != 0 && executionOptions.getBatchSize() != 1) {
            this.scheduler =
                    Executors.newScheduledThreadPool(
                            1, new ExecutorThreadFactory("jdbc-upsert-output-format"));
            this.scheduledFuture =
                    this.scheduler.scheduleWithFixedDelay(
                            () -> {
                                synchronized (JdbcBatchingOutputFormat.this) {
                                    if (!closed) {
                                        try {
                                            flush();
                                            // report is only needed when TableMetricExecutor is not initialized
                                            if (sinkMetricData != null && dirtySink == null) {
                                                sinkMetricData.invoke(rowSize, dataSize);
                                            }
                                            resetStateAfterFlush();
                                        } catch (Exception e) {
                                            resetStateAfterFlush();
                                            flushException = e;
                                        }
                                    }
                                }
                            },
                            executionOptions.getBatchIntervalMs(),
                            executionOptions.getBatchIntervalMs(),
                            TimeUnit.MILLISECONDS);
        }
    }

    private JdbcExec createAndOpenStatementExecutor(
            StatementExecutorFactory<JdbcExec> statementExecutorFactory) throws IOException {
        JdbcExec exec = statementExecutorFactory.apply(getRuntimeContext());
        if (dirtySink != null) {
            try {
                JdbcExec newExecutor = enhanceExecutor(exec);
                if (newExecutor != null) {
                    exec = newExecutor;
                }
            } catch (Exception e) {
                LOG.error("tableStatementExecutor enhance failed", e);
            }
        }
        try {
            exec.prepareStatements(connectionProvider.getConnection());
        } catch (SQLException e) {
            throw new IOException("unable to open JDBC writer", e);
        }
        return exec;
    }

    private void checkFlushException() {
        if (flushException != null) {
            throw new RuntimeException("Writing records to JDBC failed.", flushException);
        }
    }

    /**
     * update before is used for metric computing only
     * @param record
     * @return
     */
    private boolean isValidRowKind(In record) {
        RowData rowData = (RowData) record;
        return RowKind.UPDATE_BEFORE != rowData.getRowKind();
    }

    void handleDirtyData(Object dirtyData, DirtyType dirtyType, Exception e) {
        if (!dirtyOptions.ignoreDirty()) {
            RuntimeException ex;
            if (e instanceof RuntimeException) {
                ex = (RuntimeException) e;
            } else {
                ex = new RuntimeException(e);
            }
            throw ex;
        }
        if (sinkMetricData != null) {
            sinkMetricData.invokeDirty(rowSize, dataSize);
        }
        if (dirtySink != null) {
            DirtyData.Builder<Object> builder = DirtyData.builder();
            try {
                builder.setData(dirtyData)
                        .setDirtyType(dirtyType)
                        .setLabels(dirtyOptions.getLabels())
                        .setLogTag(dirtyOptions.getLogTag())
                        .setDirtyMessage(e.getMessage())
                        .setIdentifier(dirtyOptions.getIdentifier());
                dirtySink.invoke(builder.build());
            } catch (Exception ex) {
                if (!dirtyOptions.ignoreSideOutputErrors()) {
                    throw new RuntimeException(ex);
                }
                LOG.warn("Dirty sink failed", ex);
            }
        }
    }

    @Override
    public final synchronized void writeRecord(In record) {

        updateMetric(record);

        if (!isValidRowKind(record)) {
            return;
        }

        checkFlushException();

        try {
            addToBatch(record, jdbcRecordExtractor.apply(record));
            batchCount++;
            if (executionOptions.getBatchSize() > 0
                    && batchCount >= executionOptions.getBatchSize()) {
                flush();
                if (sinkMetricData != null && dirtySink == null) {
                    sinkMetricData.invoke(rowSize, dataSize);
                }
                resetStateAfterFlush();
            }
        } catch (Exception e) {
            LOG.error(String.format("jdbc batch write record error, raw data: %s", record), e);
            handleDirtyData(record, DirtyType.EXTRACT_ROWDATA_ERROR, e);
            resetStateAfterFlush();
        }
    }

    private void updateMetric(In record) {
        rowSize++;
        dataSize += CalculateObjectSizeUtils.getDataSize(record);
    }

    private void resetStateAfterFlush() {
        dataSize = 0L;
        rowSize = 0L;
    }

    protected void addToBatch(In original, JdbcIn extracted) {
        try {
            jdbcStatementExecutor.addToBatch(extracted);
        } catch (Exception e) {
            handleDirtyData(extracted, DirtyType.DATA_TYPE_MAPPING_ERROR, e);
        }
    }

    @Override
    public void snapshotState(FunctionSnapshotContext context) throws Exception {
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
    public synchronized void flush() throws IOException {
        checkFlushException();

        for (int i = 0; i <= executionOptions.getMaxRetries(); i++) {
            try {
                attemptFlush();
                batchCount = 0;
                break;
            } catch (SQLException e) {
                LOG.error("JDBC executeBatch error, retry times = {}", i, e);
                if (i >= executionOptions.getMaxRetries()) {
                    throw new IOException(e);
                }
                try {
                    if (!connectionProvider.isConnectionValid()) {
                        updateExecutor(true);
                    }
                } catch (Exception exception) {
                    LOG.error(
                            "JDBC connection is not valid, and reestablish connection failed.",
                            exception);
                    throw new IOException("Reestablish JDBC connection failed", exception);
                }
                try {
                    Thread.sleep(1000 * i);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException(
                            "unable to flush; interrupted while doing another attempt", e);
                }
            }
        }
    }

    /**
     *  Use reflection to initialize TableMetricStatementExecutor, and replace the original executor
     *  or upsertExecutor to calculate metrics.
     */
    private JdbcExec enhanceExecutor(JdbcExec exec) throws NoSuchFieldException, IllegalAccessException {
        if (dirtySink == null) {
            return null;
        }
        final DirtySinkHelper dirtySinkHelper = new DirtySinkHelper<>(dirtyOptions, dirtySink);
        // enhance the actual executor to tablemetricstatementexecutor
        Field executorType;
        if (exec instanceof TableBufferReducedStatementExecutor) {
            executorType = TableBufferReducedStatementExecutor.class.getDeclaredField("upsertExecutor");
        } else if (exec instanceof TableBufferedStatementExecutor) {
            executorType = TableBufferedStatementExecutor.class.getDeclaredField("statementExecutor");
        } else {
            throw new RuntimeException("table enhance failed, can't enhance " + exec.getClass());
        }
        executorType.setAccessible(true);
        TableSimpleStatementExecutor executor = (TableSimpleStatementExecutor) executorType.get(exec);
        // get the factory and rowconverter to initialize TableMetricStatementExecutor.
        Field statementFactory = TableSimpleStatementExecutor.class.getDeclaredField("stmtFactory");
        Field rowConverter = TableSimpleStatementExecutor.class.getDeclaredField("converter");
        statementFactory.setAccessible(true);
        rowConverter.setAccessible(true);
        final StatementFactory stmtFactory = (StatementFactory) statementFactory.get(executor);
        final JdbcRowConverter converter = (JdbcRowConverter) rowConverter.get(executor);
        TableMetricStatementExecutor newExecutor =
                new TableMetricStatementExecutor(stmtFactory, converter, dirtySinkHelper, sinkMetricData);
        // for TableBufferedStatementExecutor, replace the executor
        if (exec instanceof TableBufferedStatementExecutor) {
            Field transform = TableBufferedStatementExecutor.class.getDeclaredField("valueTransform");
            transform.setAccessible(true);
            Function<RowData, RowData> valueTransform = (Function<RowData, RowData>) transform.get(exec);
            newExecutor.setValueTransform(valueTransform);
            return (JdbcExec) newExecutor;
        }
        // replace the sub-executor that generates flinkSQL for executors such as
        // TableBufferReducedExecutor or InsertOrUpdateExecutor
        executorType.set(exec, newExecutor);
        return null;
    }

    protected void attemptFlush() throws SQLException {
        jdbcStatementExecutor.executeBatch();
    }

    /**
     * Executes prepared statement and closes all resources of this instance.
     */
    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;

            if (this.scheduledFuture != null) {
                scheduledFuture.cancel(false);
                this.scheduler.shutdown();
            }

            if (batchCount > 0) {
                try {
                    flush();
                } catch (Exception e) {
                    LOG.warn("Writing records to JDBC failed.", e);
                    throw new RuntimeException("Writing records to JDBC failed.", e);
                }
            }

            try {
                if (jdbcStatementExecutor != null) {
                    jdbcStatementExecutor.closeStatements();
                }
            } catch (SQLException e) {
                LOG.warn("Close JDBC writer failed.", e);
            }
        }
        super.close();
        checkFlushException();
    }

    public void updateExecutor(boolean reconnect) throws SQLException, ClassNotFoundException {
        jdbcStatementExecutor.closeStatements();
        jdbcStatementExecutor.prepareStatements(
                reconnect
                        ? connectionProvider.reestablishConnection()
                        : connectionProvider.getConnection());
    }

    /**
     * An interface to extract a value from given argument.
     *
     * @param <F> The type of given argument
     * @param <T> The type of the return value
     */
    public interface RecordExtractor<F, T> extends Function<F, T>, Serializable {

        static <T> RecordExtractor<T, T> identity() {
            return x -> x;
        }
    }

    /**
     * A factory for creating {@link JdbcBatchStatementExecutor} instance.
     *
     * @param <T> The type of instance.
     */
    public interface StatementExecutorFactory<T extends JdbcBatchStatementExecutor<?>>
            extends
                Function<RuntimeContext, T>,
                Serializable {

    }

    /**
     * Builder for a {@link JdbcBatchingOutputFormat}.
     */
    public static class Builder {

        private JdbcOptions options;
        private String[] fieldNames;
        private String[] keyFields;
        private int[] fieldTypes;
        private String inlongMetric;
        private String auditHostAndPorts;
        private JdbcExecutionOptions.Builder executionOptionsBuilder =
                JdbcExecutionOptions.builder();
        private DirtyOptions dirtyOptions;
        private DirtySink<Object> dirtySink;

        /**
         * required, jdbc options.
         */
        public Builder setOptions(JdbcOptions options) {
            this.options = options;
            return this;
        }

        /**
         * required, field names of this jdbc sink.
         */
        public Builder setFieldNames(String[] fieldNames) {
            this.fieldNames = fieldNames;
            return this;
        }

        /**
         * required, upsert unique keys.
         */
        public Builder setKeyFields(String[] keyFields) {
            this.keyFields = keyFields;
            return this;
        }

        /**
         * required, field types of this jdbc sink.
         */
        public Builder setFieldTypes(int[] fieldTypes) {
            this.fieldTypes = fieldTypes;
            return this;
        }

        /**
         * required, inlongMetric
         */
        public Builder setinlongMetric(String inlongMetric) {
            this.inlongMetric = inlongMetric;
            return this;
        }

        /**
         * auditHostAndPorts
         */
        public Builder setAuditHostAndPorts(String auditHostAndPorts) {
            this.auditHostAndPorts = auditHostAndPorts;
            return this;
        }

        /**
         * optional, flush max size (includes all append, upsert and delete records), over this
         * number of records, will flush data.
         */
        public Builder setFlushMaxSize(int flushMaxSize) {
            executionOptionsBuilder.withBatchSize(flushMaxSize);
            return this;
        }

        /**
         * optional, flush interval mills, over this time, asynchronous threads will flush data.
         */
        public Builder setFlushIntervalMills(long flushIntervalMills) {
            executionOptionsBuilder.withBatchIntervalMs(flushIntervalMills);
            return this;
        }

        /**
         * optional, max retry times for jdbc connector.
         */
        public Builder setMaxRetryTimes(int maxRetryTimes) {
            executionOptionsBuilder.withMaxRetries(maxRetryTimes);
            return this;
        }

        /**
         * Finalizes the configuration and checks validity.
         *
         * @return Configured JdbcUpsertOutputFormat
         */
        public JdbcBatchingOutputFormat<Tuple2<Boolean, Row>, Row, JdbcBatchStatementExecutor<Row>> build() {
            checkNotNull(options, "No options supplied.");
            checkNotNull(fieldNames, "No fieldNames supplied.");
            JdbcDmlOptions dml =
                    JdbcDmlOptions.builder()
                            .withTableName(options.getTableName())
                            .withDialect(options.getDialect())
                            .withFieldNames(fieldNames)
                            .withKeyFields(keyFields)
                            .withFieldTypes(fieldTypes)
                            .build();
            if (dml.getKeyFields().isPresent() && dml.getKeyFields().get().length > 0) {
                return new TableJdbcUpsertOutputFormat(
                        new SimpleJdbcConnectionProvider(options),
                        dml,
                        executionOptionsBuilder.build(),
                        inlongMetric,
                        auditHostAndPorts,
                        dirtyOptions,
                        dirtySink);
            } else {
                // warn: don't close over builder fields
                String sql =
                        FieldNamedPreparedStatementImpl.parseNamedStatement(
                                options.getDialect()
                                        .getInsertIntoStatement(
                                                dml.getTableName(), dml.getFieldNames()),
                                new HashMap<>());
                return new JdbcBatchingOutputFormat<>(
                        new SimpleJdbcConnectionProvider(options),
                        executionOptionsBuilder.build(),
                        ctx -> createSimpleRowExecutor(
                                sql,
                                dml.getFieldTypes(),
                                ctx.getExecutionConfig().isObjectReuseEnabled()),
                        tuple2 -> {
                            Preconditions.checkArgument(tuple2.f0);
                            return tuple2.f1;
                        },
                        inlongMetric,
                        auditHostAndPorts,
                        dirtyOptions,
                        dirtySink);
            }
        }
    }
}
