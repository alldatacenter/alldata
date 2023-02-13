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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.internal.connection.JdbcConnectionProvider;
import org.apache.flink.connector.jdbc.internal.connection.SimpleJdbcConnectionProvider;
import org.apache.flink.connector.jdbc.internal.executor.JdbcBatchStatementExecutor;
import org.apache.flink.connector.jdbc.internal.options.JdbcDmlOptions;
import org.apache.flink.connector.jdbc.internal.options.JdbcOptions;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.runtime.util.ExecutorThreadFactory;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;
import org.apache.flink.table.runtime.typeutils.InternalTypeInfo;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.RowKind;
import org.apache.inlong.sort.base.dirty.DirtySinkHelper;
import org.apache.inlong.sort.base.dirty.DirtyType;
import org.apache.inlong.sort.base.format.DynamicSchemaFormatFactory;
import org.apache.inlong.sort.base.format.JsonDynamicSchemaFormat;
import org.apache.inlong.sort.base.metric.MetricOption;
import org.apache.inlong.sort.base.metric.MetricState;
import org.apache.inlong.sort.base.metric.sub.SinkTableMetricData;
import org.apache.inlong.sort.base.sink.SchemaUpdateExceptionPolicy;
import org.apache.inlong.sort.base.util.MetricStateUtils;
import org.apache.inlong.sort.jdbc.table.AbstractJdbcDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.flink.util.Preconditions.checkNotNull;
import static org.apache.inlong.sort.base.Constants.DIRTY_BYTES_OUT;
import static org.apache.inlong.sort.base.Constants.DIRTY_RECORDS_OUT;
import static org.apache.inlong.sort.base.Constants.NUM_RECORDS_OUT;
import static org.apache.inlong.sort.base.Constants.NUM_BYTES_OUT;
import static org.apache.inlong.sort.base.Constants.INLONG_METRIC_STATE_NAME;

/**
 * A JDBC multi-table outputFormat that supports batching records before writing records to databases.
 * Add an option `inlong.metric` to support metrics.
 */
public class JdbcMultiBatchingOutputFormat<In, JdbcIn, JdbcExec extends JdbcBatchStatementExecutor<JdbcIn>>
        extends
            AbstractJdbcOutputFormat<In> {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(JdbcMultiBatchingOutputFormat.class);
    private final JdbcExecutionOptions executionOptions;
    private final String inlongMetric;
    private final String auditHostAndPorts;
    private transient int batchCount = 0;
    private transient volatile boolean closed = false;
    private transient ScheduledExecutorService scheduler;
    private transient ScheduledFuture<?> scheduledFuture;
    private transient RuntimeContext runtimeContext;
    private JdbcDmlOptions dmlOptions;
    private JdbcOptions jdbcOptions;
    private boolean appendMode;
    private transient Map<String, JdbcExec> jdbcExecMap = new HashMap<>();
    private transient Map<String, SimpleJdbcConnectionProvider> connectionExecProviderMap = new HashMap<>();
    private transient Map<String, RowType> rowTypeMap = new HashMap<>();
    private transient Map<String, List<String>> pkNameMap = new HashMap<>();
    private transient Map<String, List<GenericRowData>> recordsMap = new HashMap<>();
    private transient Map<String, Exception> tableExceptionMap = new HashMap<>();
    private transient Boolean stopWritingWhenTableException;

    private transient ListState<MetricState> metricStateListState;
    private final String sinkMultipleFormat;
    private final String databasePattern;
    private final String tablePattern;
    private final String schemaPattern;
    private transient MetricState metricState;
    private SinkTableMetricData sinkMetricData;
    private final SchemaUpdateExceptionPolicy schemaUpdateExceptionPolicy;
    private final DirtySinkHelper<Object> dirtySinkHelper;

    private static final DateTimeFormatter SQL_TIMESTAMP_WITH_LOCAL_TIMEZONE_FORMAT;
    private static final DateTimeFormatter SQL_TIME_FORMAT;

    static {
        SQL_TIME_FORMAT = (new DateTimeFormatterBuilder()).appendPattern("HH:mm:ss")
                .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true).toFormatter();
        SQL_TIMESTAMP_WITH_LOCAL_TIMEZONE_FORMAT =
                (new DateTimeFormatterBuilder()).append(DateTimeFormatter.ISO_LOCAL_DATE).appendLiteral('T')
                        .append(SQL_TIME_FORMAT).appendPattern("'Z'").toFormatter();
    }

    public JdbcMultiBatchingOutputFormat(
            @Nonnull JdbcConnectionProvider connectionProvider,
            @Nonnull JdbcExecutionOptions executionOptions,
            @Nonnull JdbcDmlOptions dmlOptions,
            @Nonnull boolean appendMode,
            @Nonnull JdbcOptions jdbcOptions,
            String sinkMultipleFormat,
            String databasePattern,
            String tablePattern,
            String schemaPattern,
            String inlongMetric,
            String auditHostAndPorts,
            SchemaUpdateExceptionPolicy schemaUpdateExceptionPolicy,
            DirtySinkHelper<Object> dirtySinkHelper) {
        super(connectionProvider);
        this.executionOptions = checkNotNull(executionOptions);
        this.dmlOptions = dmlOptions;
        this.appendMode = appendMode;
        this.jdbcOptions = jdbcOptions;
        this.sinkMultipleFormat = sinkMultipleFormat;
        this.databasePattern = databasePattern;
        this.tablePattern = tablePattern;
        this.schemaPattern = schemaPattern;
        this.inlongMetric = inlongMetric;
        this.auditHostAndPorts = auditHostAndPorts;
        this.schemaUpdateExceptionPolicy = schemaUpdateExceptionPolicy;
        this.dirtySinkHelper = dirtySinkHelper;
    }

    /**
     * Connects to the target database and initializes the prepared statement.
     *
     * @param taskNumber The number of the parallel instance.
     */
    @Override
    public void open(int taskNumber, int numTasks) throws IOException {
        this.runtimeContext = getRuntimeContext();
        MetricOption metricOption = MetricOption.builder()
                .withInlongLabels(inlongMetric)
                .withInlongAudit(auditHostAndPorts)
                .withInitRecords(metricState != null ? metricState.getMetricValue(NUM_RECORDS_OUT) : 0L)
                .withInitBytes(metricState != null ? metricState.getMetricValue(NUM_BYTES_OUT) : 0L)
                .withInitDirtyRecords(metricState != null ? metricState.getMetricValue(DIRTY_RECORDS_OUT) : 0L)
                .withInitDirtyBytes(metricState != null ? metricState.getMetricValue(DIRTY_BYTES_OUT) : 0L)
                .withRegisterMetric(MetricOption.RegisteredMetric.ALL)
                .build();
        if (metricOption != null) {
            sinkMetricData = new SinkTableMetricData(metricOption, runtimeContext.getMetricGroup());
            sinkMetricData.registerSubMetricsGroup(metricState);
        }
        jdbcExecMap = new HashMap<>();
        connectionExecProviderMap = new HashMap<>();
        pkNameMap = new HashMap<>();
        rowTypeMap = new HashMap<>();
        recordsMap = new HashMap<>();
        tableExceptionMap = new HashMap<>();
        stopWritingWhenTableException =
                schemaUpdateExceptionPolicy.equals(SchemaUpdateExceptionPolicy.ALERT_WITH_IGNORE)
                        || schemaUpdateExceptionPolicy.equals(SchemaUpdateExceptionPolicy.STOP_PARTIAL);
        if (executionOptions.getBatchIntervalMs() != 0 && executionOptions.getBatchSize() != 1) {
            this.scheduler =
                    Executors.newScheduledThreadPool(
                            1, new ExecutorThreadFactory("jdbc-upsert-output-format"));
            this.scheduledFuture =
                    this.scheduler.scheduleWithFixedDelay(
                            () -> {
                                synchronized (JdbcMultiBatchingOutputFormat.this) {
                                    if (!closed) {
                                        try {
                                            flush();
                                        } catch (Exception e) {
                                            LOG.info("Synchronized flush get Exception:", e);
                                        }
                                    }
                                }
                            },
                            executionOptions.getBatchIntervalMs(),
                            executionOptions.getBatchIntervalMs(),
                            TimeUnit.MILLISECONDS);
        }
    }

    /**
     * get or create  StatementExecutor for one table.
     *
     * @param tableIdentifier The table identifier for which to get statementExecutor.
     */
    private JdbcExec getOrCreateStatementExecutor(
            String tableIdentifier) throws IOException {
        if (StringUtils.isBlank(tableIdentifier)) {
            return null;
        }
        JdbcExec jdbcExec = jdbcExecMap.get(tableIdentifier);
        if (null != jdbcExec) {
            return jdbcExec;
        }
        if (!pkNameMap.containsKey(tableIdentifier)) {
            getAndSetPkNamesFromDb(tableIdentifier);
        }
        RowType rowType = rowTypeMap.get(tableIdentifier);
        LogicalType[] logicalTypes = rowType.getFields().stream()
                .map(RowType.RowField::getType)
                .toArray(LogicalType[]::new);
        String[] filedNames = rowType.getFields().stream()
                .map(RowType.RowField::getName)
                .toArray(String[]::new);
        TypeInformation<RowData> rowDataTypeInfo = InternalTypeInfo.of(rowType);
        List<String> pkNameList = null;
        if (null != pkNameMap.get(tableIdentifier)) {
            pkNameList = pkNameMap.get(tableIdentifier);
        }
        StatementExecutorFactory<JdbcExec> statementExecutorFactory = null;
        if (CollectionUtils.isNotEmpty(pkNameList) && !appendMode) {
            // upsert query
            JdbcDmlOptions createDmlOptions = JdbcDmlOptions.builder()
                    .withTableName(JdbcMultiBatchingComm.getTableNameFromIdentifier(tableIdentifier))
                    .withDialect(jdbcOptions.getDialect())
                    .withFieldNames(filedNames)
                    .withKeyFields(pkNameList.toArray(new String[pkNameList.size()]))
                    .build();
            statementExecutorFactory = ctx -> (JdbcExec) JdbcMultiBatchingComm.createBufferReduceExecutor(
                    createDmlOptions, ctx, rowDataTypeInfo, logicalTypes);

        } else {
            // append only query
            final String sql = dmlOptions
                    .getDialect()
                    .getInsertIntoStatement(JdbcMultiBatchingComm.getTableNameFromIdentifier(tableIdentifier),
                            filedNames);
            statementExecutorFactory = ctx -> (JdbcExec) JdbcMultiBatchingComm.createSimpleBufferedExecutor(
                    ctx,
                    dmlOptions.getDialect(),
                    filedNames,
                    logicalTypes,
                    sql,
                    rowDataTypeInfo);
        }

        jdbcExec = statementExecutorFactory.apply(getRuntimeContext());
        try {
            JdbcOptions jdbcExecOptions = JdbcMultiBatchingComm.getExecJdbcOptions(jdbcOptions, tableIdentifier);
            SimpleJdbcConnectionProvider tableConnectionProvider = new SimpleJdbcConnectionProvider(jdbcExecOptions);
            try {
                tableConnectionProvider.getOrEstablishConnection();
            } catch (Exception e) {
                LOG.error("unable to open JDBC writer, tableIdentifier:{} err:", tableIdentifier, e);
                return null;
            }
            connectionExecProviderMap.put(tableIdentifier, tableConnectionProvider);
            jdbcExec.prepareStatements(tableConnectionProvider.getConnection());
        } catch (Exception e) {
            return null;
        }
        jdbcExecMap.put(tableIdentifier, jdbcExec);
        return jdbcExec;
    }

    public void getAndSetPkNamesFromDb(String tableIdentifier) {
        try {
            AbstractJdbcDialect jdbcDialect = (AbstractJdbcDialect) jdbcOptions.getDialect();
            List<String> pkNames = jdbcDialect.getPkNamesFromDb(tableIdentifier, jdbcOptions);
            pkNameMap.put(tableIdentifier, pkNames);
        } catch (Exception e) {
            LOG.error("TableIdentifier:{} getAndSetPkNamesFromDb get err:", tableIdentifier, e);
        }
    }

    private void checkFlushException() {
        if (schemaUpdateExceptionPolicy.equals(SchemaUpdateExceptionPolicy.THROW_WITH_STOP)
                && !tableExceptionMap.isEmpty()) {
            String tableErr = "Writing table get failed, tables are:";
            for (Map.Entry<String, Exception> entry : tableExceptionMap.entrySet()) {
                LOG.error("Writing table:{} get err:{}", entry.getKey(), entry.getValue().getMessage());
                tableErr = tableErr + entry.getKey() + ",";
            }
            throw new RuntimeException(tableErr.substring(0, tableErr.length() - 1));
        }
    }

    /**
     * Extract and write record to recordsMap(buffer)
     *
     * @param row The record to write.
     */
    @Override
    public final synchronized void writeRecord(In row) throws IOException {
        checkFlushException();
        JsonDynamicSchemaFormat jsonDynamicSchemaFormat =
                (JsonDynamicSchemaFormat) DynamicSchemaFormatFactory.getFormat(sinkMultipleFormat);
        if (row instanceof RowData) {
            RowData rowData = (RowData) row;
            JsonNode rootNode = jsonDynamicSchemaFormat.deserialize(rowData.getBinary(0));
            String tableIdentifier = null;
            try {
                if (StringUtils.isBlank(schemaPattern)) {
                    tableIdentifier = StringUtils.join(
                            jsonDynamicSchemaFormat.parse(rootNode, databasePattern), ".",
                            jsonDynamicSchemaFormat.parse(rootNode, tablePattern));
                } else {
                    tableIdentifier = StringUtils.join(
                            jsonDynamicSchemaFormat.parse(rootNode, databasePattern), ".",
                            jsonDynamicSchemaFormat.parse(rootNode, schemaPattern), ".",
                            jsonDynamicSchemaFormat.parse(rootNode, tablePattern));
                }
            } catch (Exception e) {
                LOG.info("Cal tableIdentifier get Exception:", e);
                return;
            }

            GenericRowData record = null;
            try {
                RowType rowType = jsonDynamicSchemaFormat.extractSchema(rootNode);
                if (rowType != null) {
                    if (null != rowTypeMap.get(tableIdentifier)) {
                        if (!rowType.equals(rowTypeMap.get(tableIdentifier))) {
                            attemptFlush();
                            rowTypeMap.put(tableIdentifier, rowType);
                            updateOneExecutor(true, tableIdentifier);
                        }
                    } else {
                        rowTypeMap.put(tableIdentifier, rowType);
                    }
                }
                JsonNode physicalData = jsonDynamicSchemaFormat.getPhysicalData(rootNode);
                List<Map<String, String>> physicalDataList = jsonDynamicSchemaFormat.jsonNode2Map(physicalData);
                record = generateRecord(rowType, physicalDataList.get(0));
                List<RowKind> rowKinds = jsonDynamicSchemaFormat
                        .opType2RowKind(jsonDynamicSchemaFormat.getOpType(rootNode));
                record.setRowKind(rowKinds.get(rowKinds.size() - 1));
            } catch (Exception e) {
                LOG.warn("Extract schema failed", e);
                return;
            }
            try {
                recordsMap.computeIfAbsent(tableIdentifier, k -> new ArrayList<>())
                        .add(record);
                batchCount++;
                if (executionOptions.getBatchSize() > 0
                        && batchCount >= executionOptions.getBatchSize()) {
                    flush();
                }
            } catch (Exception e) {
                throw new IOException("Writing records to JDBC failed.", e);
            }
        }
    }

    /**
     * Convert fieldMap(data) to GenericRowData with rowType(schema)
     */
    protected GenericRowData generateRecord(RowType rowType, Map<String, String> fieldMap) {
        String[] fieldNames = rowType.getFieldNames().toArray(new String[0]);
        int arity = fieldNames.length;
        GenericRowData record = new GenericRowData(arity);
        for (int i = 0; i < arity; i++) {
            String fieldName = fieldNames[i];
            String fieldValue = fieldMap.get(fieldName);
            if (StringUtils.isBlank(fieldValue)) {
                record.setField(i, null);
            } else {
                switch (rowType.getFields().get(i).getType().getTypeRoot()) {
                    case BIGINT:
                        record.setField(i, Long.valueOf(fieldValue));
                        break;
                    case BOOLEAN:
                        record.setField(i, Boolean.valueOf(fieldValue));
                        break;
                    case DOUBLE:
                    case DECIMAL:
                        record.setField(i, Double.valueOf(fieldValue));
                        break;
                    case TIME_WITHOUT_TIME_ZONE:
                    case INTERVAL_DAY_TIME:
                        TimestampData timestampData = TimestampData.fromEpochMillis(Long.valueOf(fieldValue));
                        record.setField(i, timestampData);
                        break;
                    case BINARY:
                        record.setField(i, Arrays.toString(fieldValue.getBytes(StandardCharsets.UTF_8)));
                        break;

                    // support mysql
                    case INTEGER:
                        record.setField(i, Integer.valueOf(fieldValue));
                        break;
                    case SMALLINT:
                        record.setField(i, Short.valueOf(fieldValue));
                        break;
                    case TINYINT:
                        record.setField(i, Byte.valueOf(fieldValue));
                        break;
                    case FLOAT:
                        record.setField(i, Float.valueOf(fieldValue));
                        break;
                    case DATE:
                        record.setField(i, (int) LocalDate.parse(fieldValue).toEpochDay());
                        break;
                    case TIMESTAMP_WITH_LOCAL_TIME_ZONE:
                        TemporalAccessor parsedTimestampWithLocalZone =
                                SQL_TIMESTAMP_WITH_LOCAL_TIMEZONE_FORMAT.parse(fieldValue);
                        LocalTime localTime = parsedTimestampWithLocalZone.query(TemporalQueries.localTime());
                        LocalDate localDate = parsedTimestampWithLocalZone.query(TemporalQueries.localDate());
                        record.setField(i, TimestampData.fromInstant(LocalDateTime.of(localDate, localTime)
                                .toInstant(ZoneOffset.UTC)));
                        break;
                    case TIMESTAMP_WITHOUT_TIME_ZONE:
                        fieldValue = fieldValue.replace("T", " ");
                        TimestampData timestamp = TimestampData.fromTimestamp(Timestamp.valueOf(fieldValue));
                        record.setField(i, timestamp);
                        break;
                    default:
                        record.setField(i, StringData.fromString(fieldValue));
                }
            }
        }
        return record;
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
        attemptFlush();
        batchCount = 0;
    }

    /**
     * Write all recorde from recordsMap to db
     *
     * First batch writing.
     * If batch-writing occur exception, then rewrite one-by-one retry-times set by user.
     */
    protected void attemptFlush() throws IOException {
        for (Map.Entry<String, List<GenericRowData>> entry : recordsMap.entrySet()) {
            String tableIdentifier = entry.getKey();
            boolean stopTableIdentifierWhenException = stopWritingWhenTableException
                    && (null != tableExceptionMap.get(tableIdentifier));
            if (stopTableIdentifierWhenException) {
                continue;
            }
            List<GenericRowData> tableIdRecordList = entry.getValue();
            if (CollectionUtils.isEmpty(tableIdRecordList)) {
                continue;
            }
            JdbcExec jdbcStatementExecutor = null;
            Boolean flushFlag = false;
            Exception tableException = null;
            try {
                jdbcStatementExecutor = getOrCreateStatementExecutor(tableIdentifier);
                Long totalDataSize = 0L;
                for (GenericRowData record : tableIdRecordList) {
                    totalDataSize = totalDataSize + record.toString().getBytes(StandardCharsets.UTF_8).length;
                    jdbcStatementExecutor.addToBatch((JdbcIn) record);
                }
                jdbcStatementExecutor.executeBatch();
                flushFlag = true;
                outputMetrics(tableIdentifier, Long.valueOf(tableIdRecordList.size()),
                        totalDataSize, false);
            } catch (Exception e) {
                tableException = e;
                LOG.warn("Flush all data for tableIdentifier:{} get err:", tableIdentifier, e);
                getAndSetPkFromErrMsg(tableIdentifier, e.getMessage());
                updateOneExecutor(true, tableIdentifier);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException(
                            "unable to flush; interrupted while doing another attempt", e);
                }
            }

            if (!flushFlag) {
                for (GenericRowData record : tableIdRecordList) {
                    for (int retryTimes = 1; retryTimes <= executionOptions.getMaxRetries(); retryTimes++) {
                        try {
                            jdbcStatementExecutor = getOrCreateStatementExecutor(tableIdentifier);
                            jdbcStatementExecutor.addToBatch((JdbcIn) record);
                            jdbcStatementExecutor.executeBatch();
                            Long totalDataSize =
                                    Long.valueOf(record.toString().getBytes(StandardCharsets.UTF_8).length);
                            outputMetrics(tableIdentifier, 1L, totalDataSize, false);
                            flushFlag = true;
                            break;
                        } catch (Exception e) {
                            LOG.warn("Flush one record tableIdentifier:{} ,retryTimes:{} get err:",
                                    tableIdentifier, retryTimes, e);
                            getAndSetPkFromErrMsg(e.getMessage(), tableIdentifier);
                            tableException = e;
                            updateOneExecutor(true, tableIdentifier);
                            try {
                                Thread.sleep(1000 * retryTimes);
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                                throw new IOException(
                                        "unable to flush; interrupted while doing another attempt", e);
                            }
                        }
                    }
                    if (!flushFlag && null != tableException) {
                        LOG.info("Put tableIdentifier:{} exception:{}",
                                tableIdentifier, tableException.getMessage());
                        outputMetrics(tableIdentifier, Long.valueOf(tableIdRecordList.size()),
                                1L, true);
                        if (!schemaUpdateExceptionPolicy.equals(SchemaUpdateExceptionPolicy.THROW_WITH_STOP)) {
                            dirtySinkHelper.invokeMultiple(record, DirtyType.RETRY_LOAD_ERROR, tableException,
                                    sinkMultipleFormat);
                        }
                        tableExceptionMap.put(tableIdentifier, tableException);
                        if (stopWritingWhenTableException) {
                            LOG.info("Stop write table:{} because occur exception",
                                    tableIdentifier);
                            break;
                        }
                    }
                }
            }
            tableIdRecordList.clear();
        }
    }

    /**
     * Output metrics with estimate for pg or other type jdbc connectors.
     * tableIdentifier maybe: ${dbName}.${tbName} or ${dbName}.${schemaName}.${tbName}
     */
    private void outputMetrics(String tableIdentifier, Long rowSize, Long dataSize, boolean dirtyFlag) {
        String[] fieldArray = tableIdentifier.split("\\.");
        if (fieldArray.length == 3) {
            if (dirtyFlag) {
                sinkMetricData.outputDirtyMetrics(fieldArray[0], fieldArray[1], fieldArray[2],
                        rowSize, dataSize);
            } else {
                sinkMetricData.outputMetrics(fieldArray[0], fieldArray[1], fieldArray[2],
                        rowSize, dataSize);
            }
        } else if (fieldArray.length == 2) {
            if (dirtyFlag) {
                sinkMetricData.outputDirtyMetrics(fieldArray[0], null, fieldArray[1],
                        rowSize, dataSize);
            } else {
                sinkMetricData.outputMetrics(fieldArray[0], null, fieldArray[1],
                        rowSize, dataSize);
            }
        }
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
                if (null != jdbcExecMap) {
                    jdbcExecMap.forEach((tableIdentifier, jdbcExec) -> {
                        try {
                            jdbcExec.closeStatements();
                        } catch (SQLException e) {
                            LOG.error("jdbcExec executeBatch get err", e);
                        }
                    });
                }
            } catch (Exception e) {
                LOG.warn("Close JDBC writer failed.", e);
            }
        }
        super.close();
        checkFlushException();
    }

    public boolean getAndSetPkFromErrMsg(String errMsg, String tableIdentifier) {
        String rgex = "Detail: Key \\((.*?)\\)=\\(";
        Pattern pattern = Pattern.compile(rgex);
        Matcher m = pattern.matcher(errMsg);
        List<String> pkNameList = new ArrayList<>();
        if (m.find()) {
            String[] pkNameArray = m.group(1).split(",");
            for (String pkName : pkNameArray) {
                pkNameList.add(pkName.trim());
            }
            pkNameMap.put(tableIdentifier, pkNameList);
            return true;
        }
        return false;
    }

    public void updateOneExecutor(boolean reconnect, String tableIdentifier) {
        try {
            SimpleJdbcConnectionProvider tableConnectionProvider = connectionExecProviderMap.get(tableIdentifier);
            if (reconnect || null == tableConnectionProvider
                    || !tableConnectionProvider.isConnectionValid()) {
                JdbcExec tableJdbcExec = jdbcExecMap.get(tableIdentifier);
                if (null != tableJdbcExec) {
                    tableJdbcExec.closeStatements();
                    jdbcExecMap.remove(tableIdentifier);
                    getOrCreateStatementExecutor(tableIdentifier);
                }
            }
        } catch (SQLException | IOException e) {
            LOG.error("jdbcExec updateOneExecutor get err", e);
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

}
