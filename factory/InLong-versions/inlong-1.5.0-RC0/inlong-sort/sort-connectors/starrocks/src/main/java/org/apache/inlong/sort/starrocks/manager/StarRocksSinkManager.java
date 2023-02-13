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

package org.apache.inlong.sort.starrocks.manager;

import com.starrocks.connector.flink.connection.StarRocksJdbcConnectionOptions;
import com.starrocks.connector.flink.connection.StarRocksJdbcConnectionProvider;
import com.starrocks.connector.flink.manager.StarRocksQueryVisitor;
import com.starrocks.connector.flink.manager.StarRocksSinkBufferEntity;
import com.starrocks.connector.flink.manager.StarRocksStreamLoadFailedException;
import com.starrocks.connector.flink.table.sink.StarRocksSinkOptions;
import com.starrocks.connector.flink.table.sink.StarRocksSinkSemantic;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.Histogram;
import org.apache.flink.runtime.metrics.DescriptiveStatisticsHistogram;
import org.apache.flink.runtime.util.ExecutorThreadFactory;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.table.api.TableColumn;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.constraints.UniqueConstraint;
import org.apache.flink.table.types.logical.LogicalTypeRoot;
import org.apache.inlong.sort.base.dirty.DirtySinkHelper;
import org.apache.inlong.sort.base.dirty.DirtyType;
import org.apache.inlong.sort.base.metric.sub.SinkTableMetricData;
import org.apache.inlong.sort.base.sink.SchemaUpdateExceptionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * StarRocks sink manager which caches and flushes data.
 */
public class StarRocksSinkManager implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(StarRocksSinkManager.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final StarRocksJdbcConnectionProvider jdbcConnProvider;
    private final StarRocksQueryVisitor starrocksQueryVisitor;
    private StarRocksStreamLoadVisitor starrocksStreamLoadVisitor;
    private final StarRocksSinkOptions sinkOptions;
    final LinkedBlockingDeque<StarRocksSinkBufferEntity> flushQueue = new LinkedBlockingDeque<>(1);

    private transient Counter totalFlushBytes;
    private transient Counter totalFlushRows;
    private transient Counter totalFlushTime;
    private transient Counter totalFlushTimeWithoutRetries;
    private transient Counter totalFlushSucceededTimes;
    private transient Counter totalFlushFailedTimes;
    private transient Histogram flushTimeNs;
    private transient Histogram offerTimeNs;

    private transient Counter totalFilteredRows;
    private transient Histogram commitAndPublishTimeMs;
    private transient Histogram streamLoadPutTimeMs;
    private transient Histogram readDataTimeMs;
    private transient Histogram writeDataTimeMs;
    private transient Histogram loadTimeMs;

    private static final String COUNTER_TOTAL_FLUSH_BYTES = "totalFlushBytes";
    private static final String COUNTER_TOTAL_FLUSH_ROWS = "totalFlushRows";
    private static final String COUNTER_TOTAL_FLUSH_COST_TIME_WITHOUT_RETRIES = "totalFlushTimeNsWithoutRetries";
    private static final String COUNTER_TOTAL_FLUSH_COST_TIME = "totalFlushTimeNs";
    private static final String COUNTER_TOTAL_FLUSH_SUCCEEDED_TIMES = "totalFlushSucceededTimes";
    private static final String COUNTER_TOTAL_FLUSH_FAILED_TIMES = "totalFlushFailedTimes";
    private static final String HISTOGRAM_FLUSH_TIME = "flushTimeNs";
    private static final String HISTOGRAM_OFFER_TIME_NS = "offerTimeNs";

    // from stream load result
    private static final String COUNTER_NUMBER_FILTERED_ROWS = "totalFilteredRows";
    private static final String HISTOGRAM_COMMIT_AND_PUBLISH_TIME_MS = "commitAndPublishTimeMs";
    private static final String HISTOGRAM_STREAM_LOAD_PUT_TIME_MS = "streamLoadPutTimeMs";
    private static final String HISTOGRAM_READ_DATA_TIME_MS = "readDataTimeMs";
    private static final String HISTOGRAM_WRITE_DATA_TIME_MS = "writeDataTimeMs";
    private static final String HISTOGRAM_LOAD_TIME_MS = "loadTimeMs";

    private final Map<String, StarRocksSinkBufferEntity> bufferMap = new ConcurrentHashMap<>();
    private static final long FLUSH_QUEUE_POLL_TIMEOUT = 3000;
    private volatile boolean closed = false;
    private volatile boolean flushThreadAlive = false;
    private volatile Throwable flushException;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledFuture;

    private final boolean multipleSink;
    private final SchemaUpdateExceptionPolicy schemaUpdatePolicy;
    private transient SinkTableMetricData metricData;

    private final DirtySinkHelper<Object> dirtySinkHelper;
    private String sinkMultipleFormat;

    /**
     * If a table writing throws exception, ignore it when receiving data later again
     */
    private Set<String> ignoreWriteTables = new HashSet<>();

    public void setSinkMetricData(SinkTableMetricData metricData) {
        this.metricData = metricData;
    }

    public StarRocksSinkManager(StarRocksSinkOptions sinkOptions,
            TableSchema flinkSchema,
            boolean multipleSink,
            SchemaUpdateExceptionPolicy schemaUpdatePolicy,
            DirtySinkHelper<Object> dirtySinkHelper) {
        this.sinkOptions = sinkOptions;
        StarRocksJdbcConnectionOptions jdbcOptions = new StarRocksJdbcConnectionOptions(sinkOptions.getJdbcUrl(),
                sinkOptions.getUsername(), sinkOptions.getPassword());
        this.jdbcConnProvider = new StarRocksJdbcConnectionProvider(jdbcOptions);
        this.starrocksQueryVisitor = new StarRocksQueryVisitor(jdbcConnProvider, sinkOptions.getDatabaseName(),
                sinkOptions.getTableName());

        this.multipleSink = multipleSink;
        this.schemaUpdatePolicy = schemaUpdatePolicy;

        this.dirtySinkHelper = dirtySinkHelper;
        init(flinkSchema);
    }

    public StarRocksSinkManager(StarRocksSinkOptions sinkOptions,
            TableSchema flinkSchema,
            StarRocksJdbcConnectionProvider jdbcConnProvider,
            StarRocksQueryVisitor starrocksQueryVisitor,
            boolean multipleSink,
            SchemaUpdateExceptionPolicy schemaUpdatePolicy,
            DirtySinkHelper<Object> dirtySinkHelper,
            String multipleformat) {
        this.sinkOptions = sinkOptions;
        this.jdbcConnProvider = jdbcConnProvider;
        this.starrocksQueryVisitor = starrocksQueryVisitor;

        this.multipleSink = multipleSink;
        this.schemaUpdatePolicy = schemaUpdatePolicy;

        this.dirtySinkHelper = dirtySinkHelper;
        this.sinkMultipleFormat = multipleformat;
        init(flinkSchema);
    }

    protected void init(TableSchema schema) {
        if (!multipleSink) {
            validateTableStructure(schema);
        }
        String version = starrocksQueryVisitor.getStarRocksVersion();
        this.starrocksStreamLoadVisitor = new StarRocksStreamLoadVisitor(
                sinkOptions,
                null == schema ? new String[]{} : schema.getFieldNames(),
                version.length() > 0 && !version.trim().startsWith("1."));
    }

    public void setRuntimeContext(RuntimeContext runtimeCtx) {
        totalFlushBytes = runtimeCtx.getMetricGroup().counter(COUNTER_TOTAL_FLUSH_BYTES);
        totalFlushRows = runtimeCtx.getMetricGroup().counter(COUNTER_TOTAL_FLUSH_ROWS);
        totalFlushTime = runtimeCtx.getMetricGroup().counter(COUNTER_TOTAL_FLUSH_COST_TIME);
        totalFlushTimeWithoutRetries = runtimeCtx.getMetricGroup()
                .counter(COUNTER_TOTAL_FLUSH_COST_TIME_WITHOUT_RETRIES);
        totalFlushSucceededTimes = runtimeCtx.getMetricGroup().counter(COUNTER_TOTAL_FLUSH_SUCCEEDED_TIMES);
        totalFlushFailedTimes = runtimeCtx.getMetricGroup().counter(COUNTER_TOTAL_FLUSH_FAILED_TIMES);
        flushTimeNs = runtimeCtx.getMetricGroup().histogram(HISTOGRAM_FLUSH_TIME,
                new DescriptiveStatisticsHistogram(sinkOptions.getSinkHistogramWindowSize()));
        offerTimeNs = runtimeCtx.getMetricGroup().histogram(HISTOGRAM_OFFER_TIME_NS,
                new DescriptiveStatisticsHistogram(sinkOptions.getSinkHistogramWindowSize()));

        totalFilteredRows = runtimeCtx.getMetricGroup().counter(COUNTER_NUMBER_FILTERED_ROWS);
        commitAndPublishTimeMs = runtimeCtx.getMetricGroup().histogram(HISTOGRAM_COMMIT_AND_PUBLISH_TIME_MS,
                new DescriptiveStatisticsHistogram(sinkOptions.getSinkHistogramWindowSize()));
        streamLoadPutTimeMs = runtimeCtx.getMetricGroup().histogram(HISTOGRAM_STREAM_LOAD_PUT_TIME_MS,
                new DescriptiveStatisticsHistogram(sinkOptions.getSinkHistogramWindowSize()));
        readDataTimeMs = runtimeCtx.getMetricGroup().histogram(HISTOGRAM_READ_DATA_TIME_MS,
                new DescriptiveStatisticsHistogram(sinkOptions.getSinkHistogramWindowSize()));
        writeDataTimeMs = runtimeCtx.getMetricGroup().histogram(HISTOGRAM_WRITE_DATA_TIME_MS,
                new DescriptiveStatisticsHistogram(sinkOptions.getSinkHistogramWindowSize()));
        loadTimeMs = runtimeCtx.getMetricGroup().histogram(HISTOGRAM_LOAD_TIME_MS,
                new DescriptiveStatisticsHistogram(sinkOptions.getSinkHistogramWindowSize()));
    }

    public void startAsyncFlushing() {
        // start flush thread
        Thread flushThread = new Thread(() -> {
            while (true) {
                try {
                    if (!asyncFlush()) {
                        LOGGER.info("StarRocks flush thread is about to exit.");
                        flushThreadAlive = false;
                        break;
                    }
                } catch (Exception e) {
                    flushException = e;
                }
            }
        });

        flushThread.setUncaughtExceptionHandler((t, e) -> {
            LOGGER.error("StarRocks flush thread uncaught exception occurred: " + e.getMessage(), e);
            flushException = e;
            flushThreadAlive = false;
        });
        flushThread.setName("starrocks-flush");
        flushThread.setDaemon(true);
        flushThread.start();
        flushThreadAlive = true;
    }

    public void startScheduler() throws IOException {
        if (StarRocksSinkSemantic.EXACTLY_ONCE.equals(sinkOptions.getSemantic())) {
            return;
        }
        stopScheduler();
        this.scheduler = Executors.newScheduledThreadPool(1, new ExecutorThreadFactory("starrocks-interval-sink"));
        this.scheduledFuture = this.scheduler.schedule(() -> {
            synchronized (StarRocksSinkManager.this) {
                if (!closed) {
                    try {
                        LOGGER.info("StarRocks interval Sinking triggered.");
                        if (bufferMap.isEmpty()) {
                            startScheduler();
                        }
                        flush(null, false);
                    } catch (Exception e) {
                        flushException = e;
                    }
                }
            }
        }, sinkOptions.getSinkMaxFlushInterval(), TimeUnit.MILLISECONDS);
    }

    public void stopScheduler() {
        if (this.scheduledFuture != null) {
            scheduledFuture.cancel(false);
            this.scheduler.shutdown();
        }
    }

    public final synchronized void writeRecords(String database, String table, String... records) throws IOException {
        checkFlushException();
        try {
            if (0 == records.length) {
                return;
            }
            String bufferKey = String.format("%s,%s", database, table);
            StarRocksSinkBufferEntity bufferEntity = bufferMap.computeIfAbsent(bufferKey,
                    k -> new StarRocksSinkBufferEntity(database, table, sinkOptions.getLabelPrefix()));
            for (String record : records) {
                byte[] bts = record.getBytes(StandardCharsets.UTF_8);
                bufferEntity.addToBuffer(bts);
            }
            if (StarRocksSinkSemantic.EXACTLY_ONCE.equals(sinkOptions.getSemantic())) {
                return;
            }
            if (bufferEntity.getBatchCount() >= sinkOptions.getSinkMaxRows()
                    || bufferEntity.getBatchSize() >= sinkOptions.getSinkMaxBytes()) {
                LOGGER.info(
                        String.format("StarRocks buffer Sinking triggered: db: [%s] table: [%s] rows[%d] label[%s].",
                                database, table, bufferEntity.getBatchCount(), bufferEntity.getLabel()));
                flush(bufferKey, false);
            }
        } catch (Exception e) {
            throw new IOException("Writing records to StarRocks failed.", e);
        }
    }

    public synchronized void flush(String bufferKey, boolean waitUtilDone) throws Exception {
        if (bufferMap.isEmpty()) {
            flushInternal(null, waitUtilDone);
            return;
        }
        if (null == bufferKey) {
            for (String key : bufferMap.keySet()) {
                flushInternal(key, waitUtilDone);
            }
            return;
        }
        flushInternal(bufferKey, waitUtilDone);
    }

    private synchronized void flushInternal(String bufferKey, boolean waitUtilDone) throws Exception {
        checkFlushException();
        if (null == bufferKey || bufferMap.isEmpty() || !bufferMap.containsKey(bufferKey)) {
            if (waitUtilDone) {
                waitAsyncFlushingDone();
            }
            return;
        }
        offer(bufferMap.get(bufferKey));
        bufferMap.remove(bufferKey);
        if (waitUtilDone) {
            // wait the last flush
            waitAsyncFlushingDone();
        }
    }

    public synchronized void close() {
        if (!closed) {
            closed = true;

            LOGGER.info("StarRocks Sink is about to close.");
            this.bufferMap.clear();

            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
                scheduler.shutdown();
            }
            if (jdbcConnProvider != null) {
                jdbcConnProvider.close();
            }

            offerEOF();
        }
        checkFlushException();
    }

    public Map<String, StarRocksSinkBufferEntity> getBufferedBatchMap() {
        Map<String, StarRocksSinkBufferEntity> clone = new HashMap<>();
        clone.putAll(bufferMap);
        return clone;
    }

    public void setBufferedBatchMap(Map<String, StarRocksSinkBufferEntity> bufferMap) throws IOException {
        if (!StarRocksSinkSemantic.EXACTLY_ONCE.equals(sinkOptions.getSemantic())) {
            return;
        }
        this.bufferMap.clear();
        this.bufferMap.putAll(bufferMap);
    }

    /**
     * async flush
     *
     * @return false if met eof and flush thread will exit.
     */
    private boolean asyncFlush() throws Exception {
        StarRocksSinkBufferEntity flushData = flushQueue.poll(FLUSH_QUEUE_POLL_TIMEOUT, TimeUnit.MILLISECONDS);
        if (flushData == null || (0 == flushData.getBatchCount() && !flushData.EOF())) {
            return true;
        }
        if (flushData.EOF()) {
            return false;
        }
        stopScheduler();

        String tableIdentifier = flushData.getDatabase() + "." + flushData.getTable();

        if (SchemaUpdateExceptionPolicy.STOP_PARTIAL == schemaUpdatePolicy && ignoreWriteTables.contains(
                tableIdentifier)) {
            LOGGER.warn(
                    String.format("Stop writing to db[%s] table[%s] because of former errors and stop_partial policy",
                            flushData.getDatabase(), flushData.getTable()));
            return true;
        }

        LOGGER.info(String.format("Async stream load: db[%s] table[%s] rows[%d] bytes[%d] label[%s].",
                flushData.getDatabase(), flushData.getTable(), flushData.getBatchCount(), flushData.getBatchSize(),
                flushData.getLabel()));
        long startWithRetries = System.nanoTime();
        for (int i = 0; i <= sinkOptions.getSinkMaxRetries(); i++) {
            try {
                long start = System.nanoTime();
                // flush to StarRocks with stream load
                Map<String, Object> result = starrocksStreamLoadVisitor.doStreamLoad(flushData);
                LOGGER.info(String.format("Async stream load finished: label[%s].", flushData.getLabel()));
                // metrics
                if (null != totalFlushBytes) {
                    totalFlushBytes.inc(flushData.getBatchSize());
                    totalFlushRows.inc(flushData.getBatchCount());
                    totalFlushTime.inc(System.nanoTime() - startWithRetries);
                    totalFlushTimeWithoutRetries.inc(System.nanoTime() - start);
                    totalFlushSucceededTimes.inc();
                    flushTimeNs.update(System.nanoTime() - start);
                    updateMetricsFromStreamLoadResult(result);

                    if (null != metricData) {
                        if (multipleSink) {
                            metricData.outputMetrics(flushData.getDatabase(), null, flushData.getTable(),
                                    flushData.getBatchCount(), flushData.getBatchSize());
                        } else {
                            metricData.invoke(flushData.getBatchCount(), flushData.getBatchSize());
                        }
                    }
                }
                startScheduler();
                break;
            } catch (Exception e) {
                if (totalFlushFailedTimes != null) {
                    totalFlushFailedTimes.inc();
                }
                LOGGER.warn("Failed to flush batch data to StarRocks, retry times = {}", i, e);
                if (i >= sinkOptions.getSinkMaxRetries()) {
                    handleDirtyData(flushData, e);

                    if (schemaUpdatePolicy == null
                            || schemaUpdatePolicy == SchemaUpdateExceptionPolicy.THROW_WITH_STOP) {
                        throw e;
                    } else if (schemaUpdatePolicy == SchemaUpdateExceptionPolicy.STOP_PARTIAL) {
                        ignoreWriteTables.add(tableIdentifier);
                    }
                }
                if (e instanceof StarRocksStreamLoadFailedException
                        && ((StarRocksStreamLoadFailedException) e).needReCreateLabel()) {
                    String oldLabel = flushData.getLabel();
                    flushData.reGenerateLabel();
                    LOGGER.warn(String.format("Batch label changed from [%s] to [%s]", oldLabel, flushData.getLabel()));
                }
                try {
                    Thread.sleep(1000L * Math.min(i + 1, 10));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Unable to flush, interrupted while doing another attempt", e);
                }
            }
        }
        return true;
    }

    private void handleDirtyData(StarRocksSinkBufferEntity flushData, Exception e) throws JsonProcessingException {
        // archive dirty data
        if (StarRocksSinkOptions.StreamLoadFormat.CSV.equals(sinkOptions.getStreamLoadFormat())) {
            for (byte[] row : flushData.getBuffer()) {
                dirtySinkHelper.invokeMultiple(new String(row, StandardCharsets.UTF_8), DirtyType.BATCH_LOAD_ERROR, e,
                        sinkMultipleFormat);
            }
        } else if (StarRocksSinkOptions.StreamLoadFormat.JSON.equals(sinkOptions.getStreamLoadFormat())) {
            for (byte[] row : flushData.getBuffer()) {
                dirtySinkHelper.invokeMultiple(OBJECT_MAPPER.readTree(new String(row, StandardCharsets.UTF_8)),
                        DirtyType.BATCH_LOAD_ERROR, e, sinkMultipleFormat);
            }
        }

        // upload metrics for dirty data
        if (null != metricData) {
            if (multipleSink) {
                metricData.outputDirtyMetrics(flushData.getDatabase(), null, flushData.getTable(),
                        flushData.getBatchCount(), flushData.getBatchSize());
            } else {
                metricData.invokeDirty(flushData.getBatchCount(), flushData.getBatchSize());
            }
        }
    }

    private void waitAsyncFlushingDone() throws InterruptedException {
        // wait for previous flushings
        offer(new StarRocksSinkBufferEntity(null, null, null));
        offer(new StarRocksSinkBufferEntity(null, null, null));
        checkFlushException();
    }

    void offer(StarRocksSinkBufferEntity bufferEntity) throws InterruptedException {
        if (!flushThreadAlive) {
            LOGGER.info(String.format("Flush thread already exit, ignore offer request for label[%s]",
                    bufferEntity.getLabel()));
            return;
        }

        long start = System.nanoTime();
        if (!flushQueue.offer(bufferEntity, sinkOptions.getSinkOfferTimeout(), TimeUnit.MILLISECONDS)) {
            throw new RuntimeException(
                    "Timeout while offering data to flushQueue, exceed " + sinkOptions.getSinkOfferTimeout()
                            + " ms, see " + StarRocksSinkOptions.SINK_BATCH_OFFER_TIMEOUT.key());
        }
        if (offerTimeNs != null) {
            offerTimeNs.update(System.nanoTime() - start);
        }
    }

    private void offerEOF() {
        try {
            offer(new StarRocksSinkBufferEntity(null, null, null).asEOF());
        } catch (Exception e) {
            LOGGER.warn("Writing EOF failed.", e);
        }
    }

    private void checkFlushException() {
        if (flushException != null) {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (int i = 0; i < stack.length; i++) {
                LOGGER.info(
                        stack[i].getClassName() + "." + stack[i].getMethodName() + " line:" + stack[i].getLineNumber());
            }
            throw new RuntimeException("Writing records to StarRocks failed.", flushException);
        }
    }

    private void validateTableStructure(TableSchema flinkSchema) {
        if (null == flinkSchema) {
            return;
        }
        Optional<UniqueConstraint> constraint = flinkSchema.getPrimaryKey();
        List<Map<String, Object>> rows = starrocksQueryVisitor.getTableColumnsMetaData();
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("Couldn't get the sink table's column info.");
        }
        // validate primary keys
        List<String> primayKeys = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            String keysType = rows.get(i).get("COLUMN_KEY").toString();
            if (!"PRI".equals(keysType)) {
                continue;
            }
            primayKeys.add(rows.get(i).get("COLUMN_NAME").toString().toLowerCase());
        }
        if (!primayKeys.isEmpty()) {
            if (!constraint.isPresent()) {
                throw new IllegalArgumentException("Primary keys not defined in the sink `TableSchema`.");
            }
            if (constraint.get().getColumns().size() != primayKeys.size() || !constraint.get().getColumns().stream()
                    .allMatch(col -> primayKeys.contains(col.toLowerCase()))) {
                throw new IllegalArgumentException(
                        "Primary keys of the flink `TableSchema` do not match with the ones from starrocks table.");
            }
            sinkOptions.enableUpsertDelete();
        }

        if (sinkOptions.hasColumnMappingProperty()) {
            return;
        }
        if (flinkSchema.getFieldCount() != rows.size()) {
            throw new IllegalArgumentException(
                    "Fields count of " + this.sinkOptions.getTableName() + " mismatch. \nflinkSchema["
                            + flinkSchema.getFieldNames().length + "]:"
                            + Arrays.asList(flinkSchema.getFieldNames()).stream().collect(Collectors.joining(","))
                            + "\n realTab[" + rows.size() + "]:"
                            + rows.stream().map((r) -> String.valueOf(r.get("COLUMN_NAME")))
                                    .collect(Collectors.joining(",")));
        }
        List<TableColumn> flinkCols = flinkSchema.getTableColumns();
        for (int i = 0; i < rows.size(); i++) {
            String starrocksField = rows.get(i).get("COLUMN_NAME").toString().toLowerCase();
            String starrocksType = rows.get(i).get("DATA_TYPE").toString().toLowerCase();
            List<TableColumn> matchedFlinkCols = flinkCols.stream()
                    .filter(col -> col.getName().toLowerCase().equals(starrocksField)
                            && (!typesMap.containsKey(starrocksType) || typesMap.get(starrocksType)
                                    .contains(col.getType().getLogicalType().getTypeRoot())))
                    .collect(Collectors.toList());
            if (matchedFlinkCols.isEmpty()) {
                throw new IllegalArgumentException("Fields name or type mismatch for:" + starrocksField);
            }
        }
    }

    private void updateMetricsFromStreamLoadResult(Map<String, Object> result) {
        if (result != null) {
            updateHisto(result, "CommitAndPublishTimeMs", this.commitAndPublishTimeMs);
            updateHisto(result, "StreamLoadPutTimeMs", this.streamLoadPutTimeMs);
            updateHisto(result, "ReadDataTimeMs", this.readDataTimeMs);
            updateHisto(result, "WriteDataTimeMs", this.writeDataTimeMs);
            updateHisto(result, "LoadTimeMs", this.loadTimeMs);
            updateCounter(result, "NumberFilteredRows", this.totalFilteredRows);
        }
    }

    private void updateCounter(Map<String, Object> result, String key, Counter counter) {
        if (result.containsKey(key)) {
            Object val = result.get(key);
            if (val != null) {
                try {
                    long longValue = Long.parseLong(val.toString());
                    counter.inc(longValue);
                } catch (Exception e) {
                    LOGGER.warn("Parse stream load result metric error", e);
                }
            }
        }
    }

    private void updateHisto(Map<String, Object> result, String key, Histogram histogram) {
        if (result.containsKey(key)) {
            Object val = result.get(key);
            if (val != null) {
                try {
                    long longValue = Long.parseLong(val.toString());
                    histogram.update(longValue);
                } catch (Exception e) {
                    LOGGER.warn("Parse stream load result metric error", e);
                }
            }
        }
    }

    private static final Map<String, List<LogicalTypeRoot>> typesMap = new HashMap<>();

    static {
        // validate table structure
        typesMap.put("bigint", Arrays.asList(LogicalTypeRoot.BIGINT, LogicalTypeRoot.INTEGER, LogicalTypeRoot.BINARY));
        typesMap.put("largeint", Arrays.asList(LogicalTypeRoot.DECIMAL, LogicalTypeRoot.BIGINT, LogicalTypeRoot.INTEGER,
                LogicalTypeRoot.BINARY));
        typesMap.put("char", Arrays.asList(LogicalTypeRoot.CHAR, LogicalTypeRoot.VARCHAR));
        typesMap.put("date", Arrays.asList(LogicalTypeRoot.DATE, LogicalTypeRoot.VARCHAR));
        typesMap.put("datetime", Arrays.asList(LogicalTypeRoot.TIMESTAMP_WITHOUT_TIME_ZONE,
                LogicalTypeRoot.TIMESTAMP_WITH_LOCAL_TIME_ZONE, LogicalTypeRoot.VARCHAR));
        typesMap.put("decimal", Arrays.asList(LogicalTypeRoot.DECIMAL, LogicalTypeRoot.BIGINT, LogicalTypeRoot.INTEGER,
                LogicalTypeRoot.DOUBLE, LogicalTypeRoot.FLOAT));
        typesMap.put("double", Arrays.asList(LogicalTypeRoot.DOUBLE, LogicalTypeRoot.BIGINT, LogicalTypeRoot.INTEGER));
        typesMap.put("float", Arrays.asList(LogicalTypeRoot.FLOAT, LogicalTypeRoot.INTEGER));
        typesMap.put("int", Arrays.asList(LogicalTypeRoot.INTEGER, LogicalTypeRoot.BINARY));
        typesMap.put("tinyint", Arrays.asList(LogicalTypeRoot.TINYINT, LogicalTypeRoot.INTEGER, LogicalTypeRoot.BINARY,
                LogicalTypeRoot.BOOLEAN));
        typesMap.put("smallint",
                Arrays.asList(LogicalTypeRoot.SMALLINT, LogicalTypeRoot.INTEGER, LogicalTypeRoot.BINARY));
        typesMap.put("varchar", Arrays.asList(LogicalTypeRoot.VARCHAR, LogicalTypeRoot.ARRAY, LogicalTypeRoot.MAP,
                LogicalTypeRoot.ROW));
        typesMap.put("string",
                Arrays.asList(LogicalTypeRoot.CHAR, LogicalTypeRoot.VARCHAR, LogicalTypeRoot.ARRAY, LogicalTypeRoot.MAP,
                        LogicalTypeRoot.ROW));
    }

}
