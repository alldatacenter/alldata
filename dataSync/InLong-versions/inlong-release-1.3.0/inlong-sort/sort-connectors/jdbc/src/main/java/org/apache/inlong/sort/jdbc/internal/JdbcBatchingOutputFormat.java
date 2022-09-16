/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.jdbc.internal;

import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcStatementBuilder;
import org.apache.flink.connector.jdbc.internal.AbstractJdbcOutputFormat;
import org.apache.flink.connector.jdbc.internal.connection.JdbcConnectionProvider;
import org.apache.flink.connector.jdbc.internal.connection.SimpleJdbcConnectionProvider;
import org.apache.flink.connector.jdbc.internal.executor.JdbcBatchStatementExecutor;
import org.apache.flink.connector.jdbc.internal.options.JdbcDmlOptions;
import org.apache.flink.connector.jdbc.internal.options.JdbcOptions;
import org.apache.flink.connector.jdbc.statement.FieldNamedPreparedStatementImpl;
import org.apache.flink.connector.jdbc.utils.JdbcUtils;
import org.apache.flink.runtime.util.ExecutorThreadFactory;
import org.apache.flink.types.Row;
import org.apache.flink.util.Preconditions;
import org.apache.inlong.audit.AuditImp;
import org.apache.inlong.sort.base.metric.SinkMetricData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import static org.apache.flink.connector.jdbc.utils.JdbcUtils.setRecordToStatement;
import static org.apache.flink.util.Preconditions.checkNotNull;
import static org.apache.inlong.sort.base.Constants.AUDIT_SORT_INPUT;
import static org.apache.inlong.sort.base.Constants.DELIMITER;

/**
 * A JDBC outputFormat that supports batching records before writing records to database.
 * Add an option `inlong.metric` to support metrics.
 */
public class JdbcBatchingOutputFormat<
        In, JdbcIn, JdbcExec extends JdbcBatchStatementExecutor<JdbcIn>>
        extends AbstractJdbcOutputFormat<In> {

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

    private SinkMetricData sinkMetricData;
    private String inlongGroupId;
    private String inlongStreamId;
    private transient AuditImp auditImp;
    private Long dataSize = 0L;
    private Long rowSize = 0L;

    public JdbcBatchingOutputFormat(
            @Nonnull JdbcConnectionProvider connectionProvider,
            @Nonnull JdbcExecutionOptions executionOptions,
            @Nonnull StatementExecutorFactory<JdbcExec> statementExecutorFactory,
            @Nonnull RecordExtractor<In, JdbcIn> recordExtractor,
            String inlongMetric,
            String auditHostAndPorts) {
        super(connectionProvider);
        this.executionOptions = checkNotNull(executionOptions);
        this.statementExecutorFactory = checkNotNull(statementExecutorFactory);
        this.jdbcRecordExtractor = checkNotNull(recordExtractor);
        this.inlongMetric = inlongMetric;
        this.auditHostAndPorts = auditHostAndPorts;
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
        if (inlongMetric != null && !inlongMetric.isEmpty()) {
            String[] inlongMetricArray = inlongMetric.split(DELIMITER);
            inlongGroupId = inlongMetricArray[0];
            inlongStreamId = inlongMetricArray[1];
            String nodeId = inlongMetricArray[2];
            sinkMetricData = new SinkMetricData(inlongGroupId, inlongStreamId, nodeId, runtimeContext.getMetricGroup());
            sinkMetricData.registerMetricsForDirtyBytes();
            sinkMetricData.registerMetricsForDirtyRecords();
            sinkMetricData.registerMetricsForNumBytesOut();
            sinkMetricData.registerMetricsForNumRecordsOut();
            sinkMetricData.registerMetricsForNumBytesOutPerSecond();
            sinkMetricData.registerMetricsForNumRecordsOutPerSecond();
        }
        if (auditHostAndPorts != null) {
            AuditImp.getInstance().setAuditProxy(new HashSet<>(Arrays.asList(auditHostAndPorts.split(DELIMITER))));
            auditImp = AuditImp.getInstance();
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
                                            if (sinkMetricData.getNumRecordsOut() != null) {
                                                sinkMetricData.getNumRecordsOut().inc(rowSize);
                                            }
                                            if (sinkMetricData.getNumBytesOut() != null) {
                                                sinkMetricData.getNumBytesOut()
                                                        .inc(dataSize);
                                            }
                                            resetStateAfterFlush();
                                        } catch (Exception e) {
                                            if (sinkMetricData.getDirtyRecords() != null) {
                                                sinkMetricData.getDirtyRecords().inc(rowSize);
                                            }
                                            if (sinkMetricData.getDirtyBytes() != null) {
                                                sinkMetricData.getDirtyBytes().inc(dataSize);
                                            }
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

    private void outputMetricForAudit(long length) {
        if (auditImp != null) {
            auditImp.add(
                    AUDIT_SORT_INPUT,
                    inlongGroupId,
                    inlongStreamId,
                    System.currentTimeMillis(),
                    1,
                    length);
        }
    }

    @Override
    public final synchronized void writeRecord(In record) throws IOException {
        checkFlushException();

        rowSize++;
        dataSize = dataSize + record.toString().getBytes(StandardCharsets.UTF_8).length;
        outputMetricForAudit(dataSize);
        try {
            addToBatch(record, jdbcRecordExtractor.apply(record));
            batchCount++;
            if (executionOptions.getBatchSize() > 0
                    && batchCount >= executionOptions.getBatchSize()) {
                flush();
                if (sinkMetricData.getNumRecordsOut() != null) {
                    sinkMetricData.getNumRecordsOut().inc(rowSize);
                }
                if (sinkMetricData.getNumBytesOut() != null) {
                    sinkMetricData.getNumBytesOut()
                            .inc(dataSize);
                }
                resetStateAfterFlush();
            }
        } catch (Exception e) {
            if (sinkMetricData.getDirtyRecords() != null) {
                sinkMetricData.getDirtyRecords().inc(rowSize);
            }
            if (sinkMetricData.getDirtyBytes() != null) {
                sinkMetricData.getDirtyBytes().inc(dataSize);
            }
            resetStateAfterFlush();
            throw new IOException("Writing records to JDBC failed.", e);
        }
    }

    private void resetStateAfterFlush() {
        dataSize = 0L;
        rowSize = 0L;
    }

    protected void addToBatch(In original, JdbcIn extracted) throws SQLException {
        jdbcStatementExecutor.addToBatch(extracted);
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
            extends Function<RuntimeContext, T>, Serializable {

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
        public JdbcBatchingOutputFormat<Tuple2<Boolean, Row>, Row, JdbcBatchStatementExecutor<Row>>
        build() {
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
                        auditHostAndPorts);
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
                        ctx ->
                                createSimpleRowExecutor(
                                        sql,
                                        dml.getFieldTypes(),
                                        ctx.getExecutionConfig().isObjectReuseEnabled()),
                        tuple2 -> {
                            Preconditions.checkArgument(tuple2.f0);
                            return tuple2.f1;
                        },
                        inlongMetric,
                        auditHostAndPorts);
            }
        }
    }
}
