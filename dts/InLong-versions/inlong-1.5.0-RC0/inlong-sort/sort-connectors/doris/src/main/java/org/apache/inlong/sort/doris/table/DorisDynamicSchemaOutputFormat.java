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

package org.apache.inlong.sort.doris.table;

import org.apache.commons.lang3.StringUtils;
import org.apache.doris.flink.cfg.DorisExecutionOptions;
import org.apache.doris.flink.cfg.DorisOptions;
import org.apache.doris.flink.cfg.DorisReadOptions;
import org.apache.doris.flink.exception.DorisException;
import org.apache.doris.flink.exception.StreamLoadException;
import org.apache.doris.flink.rest.RestService;
import org.apache.doris.flink.rest.models.Schema;
import org.apache.doris.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.io.RichOutputFormat;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.runtime.util.ExecutorThreadFactory;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.RowKind;
import org.apache.flink.util.Preconditions;
import org.apache.inlong.sort.base.dirty.DirtyData;
import org.apache.inlong.sort.base.dirty.DirtyOptions;
import org.apache.inlong.sort.base.dirty.DirtyType;
import org.apache.inlong.sort.base.dirty.sink.DirtySink;
import org.apache.inlong.sort.base.format.DynamicSchemaFormatFactory;
import org.apache.inlong.sort.base.format.JsonDynamicSchemaFormat;
import org.apache.inlong.sort.base.metric.MetricOption;
import org.apache.inlong.sort.base.metric.MetricState;
import org.apache.inlong.sort.base.metric.sub.SinkTableMetricData;
import org.apache.inlong.sort.base.util.MetricStateUtils;
import org.apache.inlong.sort.doris.model.RespContent;
import org.apache.inlong.sort.doris.util.DorisParseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.apache.inlong.sort.base.Constants.DIRTY_BYTES_OUT;
import static org.apache.inlong.sort.base.Constants.DIRTY_RECORDS_OUT;
import static org.apache.inlong.sort.base.Constants.INLONG_METRIC_STATE_NAME;
import static org.apache.inlong.sort.base.Constants.NUM_BYTES_OUT;
import static org.apache.inlong.sort.base.Constants.NUM_RECORDS_OUT;

/**
 * DorisDynamicSchemaOutputFormat, copy from {@link org.apache.doris.flink.table.DorisDynamicOutputFormat}
 * It is used in the multiple sink scenario, in this scenario, we directly convert the data format by
 * 'sink.multiple.format' in the data stream to doris json that is used to load
 */
public class DorisDynamicSchemaOutputFormat<T> extends RichOutputFormat<T> {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(DorisDynamicSchemaOutputFormat.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String COLUMNS_KEY = "columns";
    private static final String DORIS_DELETE_SIGN = "__DORIS_DELETE_SIGN__";
    /**
     * Mark the record for delete
     */
    private static final String DORIS_DELETE_TRUE = "1";
    /**
     * Mark the record for not delete
     */
    private static final String DORIS_DELETE_FALSE = "0";
    private static final String FIELD_DELIMITER_KEY = "column_separator";
    private static final String FIELD_DELIMITER_DEFAULT = "\t";
    private static final String LINE_DELIMITER_KEY = "line_delimiter";
    private static final String LINE_DELIMITER_DEFAULT = "\n";
    private static final String NULL_VALUE = "\\N";
    private static final String ESCAPE_DELIMITERS_KEY = "escape_delimiters";
    private static final String ESCAPE_DELIMITERS_DEFAULT = "false";
    private static final String UNIQUE_KEYS_TYPE = "UNIQUE_KEYS";
    @SuppressWarnings({"rawtypes"})
    private final Map<String, List> batchMap = new HashMap<>();
    private final Map<String, String> columnsMap = new HashMap<>();
    private final List<String> errorTables = new ArrayList<>();
    private final DorisOptions options;
    private final DorisReadOptions readOptions;
    private final DorisExecutionOptions executionOptions;
    private final Map<String, Exception> flushExceptionMap = new HashMap<>();
    private final AtomicLong readInNum = new AtomicLong(0);
    private final AtomicLong writeOutNum = new AtomicLong(0);
    private final AtomicLong errorNum = new AtomicLong(0);
    private final AtomicLong ddlNum = new AtomicLong(0);
    private final String inlongMetric;
    private final String auditHostAndPorts;
    private final boolean multipleSink;
    private final String tableIdentifier;
    private final String databasePattern;
    private final String tablePattern;
    private final String dynamicSchemaFormat;
    private final boolean ignoreSingleTableErrors;
    private long batchBytes = 0L;
    private int size;
    private DorisStreamLoad dorisStreamLoad;
    private transient volatile boolean closed = false;
    private transient volatile boolean flushing = false;
    private transient ScheduledExecutorService scheduler;
    private transient ScheduledFuture<?> scheduledFuture;
    private transient JsonDynamicSchemaFormat jsonDynamicSchemaFormat;
    private transient SinkTableMetricData metricData;
    private transient ListState<MetricState> metricStateListState;
    private transient MetricState metricState;
    private final String[] fieldNames;
    private volatile boolean jsonFormat;
    private volatile RowData.FieldGetter[] fieldGetters;
    private String fieldDelimiter;
    private String lineDelimiter;
    private final LogicalType[] logicalTypes;
    private final DirtyOptions dirtyOptions;
    private @Nullable final DirtySink<Object> dirtySink;

    public DorisDynamicSchemaOutputFormat(DorisOptions option,
            DorisReadOptions readOptions,
            DorisExecutionOptions executionOptions,
            String tableIdentifier,
            LogicalType[] logicalTypes,
            String[] fieldNames,
            String dynamicSchemaFormat,
            String databasePattern,
            String tablePattern,
            boolean ignoreSingleTableErrors,
            String inlongMetric,
            String auditHostAndPorts,
            boolean multipleSink,
            DirtyOptions dirtyOptions,
            @Nullable DirtySink<Object> dirtySink) {
        this.options = option;
        this.readOptions = readOptions;
        this.executionOptions = executionOptions;
        this.tableIdentifier = tableIdentifier;
        this.fieldNames = fieldNames;
        this.multipleSink = multipleSink;
        this.inlongMetric = inlongMetric;
        this.auditHostAndPorts = auditHostAndPorts;
        this.logicalTypes = logicalTypes;
        this.dynamicSchemaFormat = dynamicSchemaFormat;
        this.databasePattern = databasePattern;
        this.tablePattern = tablePattern;
        this.ignoreSingleTableErrors = ignoreSingleTableErrors;
        this.dirtyOptions = dirtyOptions;
        this.dirtySink = dirtySink;
    }

    /**
     * A builder used to set parameters to the output format's configuration in a fluent way.
     *
     * @return builder
     */
    public static DorisDynamicSchemaOutputFormat.Builder builder() {
        return new DorisDynamicSchemaOutputFormat.Builder();
    }

    private String parseKeysType() {
        try {
            Schema schema = RestService.getSchema(options, readOptions, LOG);
            return schema.getKeysType();
        } catch (DorisException e) {
            throw new RuntimeException("Failed fetch doris table schema: " + options.getTableIdentifier());
        }
    }

    private void handleStreamLoadProp() {
        Properties props = executionOptions.getStreamLoadProp();
        boolean ifEscape = Boolean.parseBoolean(props.getProperty(ESCAPE_DELIMITERS_KEY, ESCAPE_DELIMITERS_DEFAULT));
        this.fieldDelimiter = props.getProperty(FIELD_DELIMITER_KEY, FIELD_DELIMITER_DEFAULT);
        this.lineDelimiter = props.getProperty(LINE_DELIMITER_KEY, LINE_DELIMITER_DEFAULT);
        if (ifEscape) {
            this.fieldDelimiter = DorisParseUtils.escapeString(fieldDelimiter);
            this.lineDelimiter = DorisParseUtils.escapeString(lineDelimiter);
            props.remove(ESCAPE_DELIMITERS_KEY);
        }

        // add column key when fieldNames is not empty
        if (!props.containsKey(COLUMNS_KEY) && fieldNames != null && fieldNames.length > 0) {
            String columns = Arrays.stream(fieldNames)
                    .map(item -> String.format("`%s`", item.trim().replace("`", "")))
                    .collect(Collectors.joining(","));
            props.put(COLUMNS_KEY, columns);
        }

        // if enable batch delete, the columns must add tag '__DORIS_DELETE_SIGN__'
        String columns = (String) props.get(COLUMNS_KEY);
        if (!columns.contains(DORIS_DELETE_SIGN) && enableBatchDelete()) {
            props.put(COLUMNS_KEY, String.format("%s,%s", columns, DORIS_DELETE_SIGN));
        }
    }

    private boolean enableBatchDelete() {
        if (multipleSink) {
            return executionOptions.getEnableDelete();
        }
        try {
            Schema schema = RestService.getSchema(options, readOptions, LOG);
            return executionOptions.getEnableDelete() || UNIQUE_KEYS_TYPE.equals(schema.getKeysType());
        } catch (DorisException e) {
            throw new RuntimeException("Failed fetch doris single table schema: " + options.getTableIdentifier(), e);
        }
    }

    @Override
    public void configure(Configuration configuration) {
    }

    @Override
    public void open(int taskNumber, int numTasks) throws IOException {
        Properties loadProps = executionOptions.getStreamLoadProp();
        dorisStreamLoad = new DorisStreamLoad(getBackend(), options.getUsername(), options.getPassword(), loadProps);
        if (!multipleSink) {
            this.jsonFormat = true;
            handleStreamLoadProp();
            this.fieldGetters = new RowData.FieldGetter[logicalTypes.length];
            for (int i = 0; i < logicalTypes.length; i++) {
                fieldGetters[i] = RowData.createFieldGetter(logicalTypes[i], i);
                if ("DATE".equalsIgnoreCase(logicalTypes[i].toString())) {
                    int finalI = i;
                    fieldGetters[i] = row -> DorisParseUtils.epochToDate(row.getInt(finalI));
                }
            }
        }

        if (multipleSink && StringUtils.isNotBlank(dynamicSchemaFormat)) {
            jsonDynamicSchemaFormat =
                    (JsonDynamicSchemaFormat) DynamicSchemaFormatFactory.getFormat(dynamicSchemaFormat);
        }
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
            metricData = new SinkTableMetricData(metricOption, getRuntimeContext().getMetricGroup());
            if (multipleSink) {
                metricData.registerSubMetricsGroup(metricState);
            }
        }
        if (dirtySink != null) {
            try {
                dirtySink.open(new Configuration());
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
        if (executionOptions.getBatchIntervalMs() != 0 && executionOptions.getBatchSize() != 1) {
            this.scheduler = new ScheduledThreadPoolExecutor(1,
                    new ExecutorThreadFactory("doris-streamload-output-format"));
            this.scheduledFuture = this.scheduler.scheduleWithFixedDelay(() -> {
                if (!closed && !flushing) {
                    flush();
                }
            }, executionOptions.getBatchIntervalMs(), executionOptions.getBatchIntervalMs(), TimeUnit.MILLISECONDS);
        }
    }

    private boolean checkFlushException(String tableIdentifier) {
        Exception ex = flushExceptionMap.get(tableIdentifier);
        if (!multipleSink || ex == null) {
            return false;
        }
        if (!ignoreSingleTableErrors) {
            throw new RuntimeException("Writing records to streamload failed, tableIdentifier=" + tableIdentifier, ex);
        }
        return true;
    }

    @Override
    public synchronized void writeRecord(T row) {
        addBatch(row);
        boolean valid = (executionOptions.getBatchSize() > 0 && size >= executionOptions.getBatchSize())
                || batchBytes >= executionOptions.getMaxBatchBytes();
        if (valid && !flushing) {
            flush();
        }
    }

    public void addSingle(T row) {
        if (row instanceof RowData) {
            try {
                RowData rowData = (RowData) row;
                Map<String, String> valueMap = new HashMap<>();
                StringJoiner value = new StringJoiner(this.fieldDelimiter);
                for (int i = 0; i < rowData.getArity() && i < fieldGetters.length; ++i) {
                    Object field = fieldGetters[i].getFieldOrNull(rowData);
                    if (jsonFormat) {
                        String data = field != null ? field.toString() : null;
                        valueMap.put(this.fieldNames[i], data);
                        batchBytes += this.fieldNames[i].getBytes(StandardCharsets.UTF_8).length;
                        if (data != null) {
                            batchBytes += data.getBytes(StandardCharsets.UTF_8).length;
                        }
                    } else {
                        String data = field != null ? field.toString() : NULL_VALUE;
                        value.add(data);
                        batchBytes += data.getBytes(StandardCharsets.UTF_8).length;
                    }
                }
                // add doris delete sign
                if (enableBatchDelete()) {
                    if (jsonFormat) {
                        valueMap.put(DORIS_DELETE_SIGN, DorisParseUtils.parseDeleteSign(rowData.getRowKind()));
                    } else {
                        value.add(DorisParseUtils.parseDeleteSign(rowData.getRowKind()));
                    }
                }
                Object data = jsonFormat ? valueMap : value.toString();
                List mapData = batchMap.getOrDefault(tableIdentifier, new ArrayList<String>());
                mapData.add(data);
                batchMap.putIfAbsent(tableIdentifier, mapData);
            } catch (Exception e) {
                LOG.error(String.format("serialize error, raw data: %s", row), e);
                handleDirtyData(row, DirtyType.SERIALIZE_ERROR, e);
            }
        } else if (row instanceof String) {
            batchBytes += ((String) row).getBytes(StandardCharsets.UTF_8).length;
            List mapData = batchMap.getOrDefault(tableIdentifier, new ArrayList<String>());
            mapData.add(row);
            batchMap.putIfAbsent(tableIdentifier, mapData);
        } else {
            LOG.error(String.format("The type of element should be 'RowData' or 'String' only., raw data: %s", row));
            handleDirtyData(row, DirtyType.UNSUPPORTED_DATA_TYPE,
                    new RuntimeException("The type of element should be 'RowData' or 'String' only."));
        }

    }

    private void addBatch(T row) {
        readInNum.incrementAndGet();
        if (!multipleSink) {
            addSingle(row);
            return;
        }
        if (row instanceof RowData) {
            RowData rowData = (RowData) row;
            JsonNode rootNode;
            try {
                rootNode = jsonDynamicSchemaFormat.deserialize(rowData.getBinary(0));
            } catch (Exception e) {
                LOG.error(String.format("deserialize error, raw data: %s", new String(rowData.getBinary(0))), e);
                handleDirtyData(new String(rowData.getBinary(0)), DirtyType.DESERIALIZE_ERROR, e);
                return;
            }
            boolean isDDL = jsonDynamicSchemaFormat.extractDDLFlag(rootNode);
            if (isDDL) {
                ddlNum.incrementAndGet();
                // Ignore ddl change for now
                return;
            }
            String tableIdentifier;
            List<RowKind> rowKinds;
            JsonNode physicalData;
            JsonNode updateBeforeNode;
            List<Map<String, String>> physicalDataList;
            List<Map<String, String>> updateBeforeList = null;
            try {
                tableIdentifier = StringUtils.join(
                        jsonDynamicSchemaFormat.parse(rootNode, databasePattern), ".",
                        jsonDynamicSchemaFormat.parse(rootNode, tablePattern));
                if (checkFlushException(tableIdentifier)) {
                    return;
                }
                rowKinds = Preconditions.checkNotNull(jsonDynamicSchemaFormat
                        .opType2RowKind(jsonDynamicSchemaFormat.getOpType(rootNode)));
                physicalData = Preconditions.checkNotNull(jsonDynamicSchemaFormat.getPhysicalData(rootNode));
                Preconditions.checkArgument(!physicalData.isEmpty());
                physicalDataList = Preconditions.checkNotNull(jsonDynamicSchemaFormat.jsonNode2Map(physicalData));
                updateBeforeNode = jsonDynamicSchemaFormat.getUpdateBefore(rootNode);
                if (updateBeforeNode != null) {
                    updateBeforeList = jsonDynamicSchemaFormat.jsonNode2Map(updateBeforeNode);
                }
            } catch (Exception e) {
                LOG.error(String.format("json parse error, raw data: %s", new String(rowData.getBinary(0))), e);
                handleDirtyData(new String(rowData.getBinary(0)), DirtyType.JSON_PROCESS_ERROR, e);
                return;
            }
            for (int i = 0; i < physicalDataList.size(); i++) {
                for (RowKind rowKind : rowKinds) {
                    if (updateBeforeList != null && updateBeforeList.size() > i) {
                        addRow(rowKind, rootNode, physicalData, updateBeforeNode,
                                physicalDataList.get(i), updateBeforeList.get(i));
                    } else {
                        addRow(rowKind, rootNode, physicalData, updateBeforeNode,
                                physicalDataList.get(i), null);
                    }
                }
            }
        } else {
            LOG.error(String.format("The type of element should be 'RowData' only, raw data: %s", row));
            handleDirtyData(row, DirtyType.UNSUPPORTED_DATA_TYPE,
                    new RuntimeException("The type of element should be 'RowData' only."));
        }
    }

    @SuppressWarnings({"unchecked"})
    private void addRow(RowKind rowKind, JsonNode rootNode, JsonNode physicalNode, JsonNode updateBeforeNode,
            Map<String, String> physicalData, Map<String, String> updateBeforeData) {
        switch (rowKind) {
            case INSERT:
            case UPDATE_AFTER:
                handleColumnsChange(tableIdentifier, rootNode, physicalNode);
                batchBytes += physicalData.toString().getBytes(StandardCharsets.UTF_8).length;
                size++;
                batchMap.computeIfAbsent(tableIdentifier, k -> new ArrayList<>())
                        .add(physicalData);
                if (enableBatchDelete()) {
                    physicalData.put(DORIS_DELETE_SIGN, DORIS_DELETE_FALSE);
                }
                break;
            case DELETE:
                handleColumnsChange(tableIdentifier, rootNode, physicalNode);
                batchBytes += physicalData.toString().getBytes(StandardCharsets.UTF_8).length;
                size++;
                // add doris delete sign
                if (enableBatchDelete()) {
                    physicalData.put(DORIS_DELETE_SIGN, DORIS_DELETE_TRUE);
                }
                batchMap.computeIfAbsent(tableIdentifier, k -> new ArrayList<>())
                        .add(physicalData);
                break;
            case UPDATE_BEFORE:
                if (updateBeforeData != null) {
                    handleColumnsChange(tableIdentifier, rootNode, updateBeforeNode);
                    batchBytes += updateBeforeData.toString()
                            .getBytes(StandardCharsets.UTF_8).length;
                    size++;
                    // add doris delete sign
                    if (enableBatchDelete()) {
                        updateBeforeData.put(DORIS_DELETE_SIGN, DORIS_DELETE_TRUE);
                    }
                    batchMap.computeIfAbsent(tableIdentifier, k -> new ArrayList<>())
                            .add(updateBeforeData);
                }
                break;
            default:
        }
    }

    private void handleDirtyData(Object dirtyData, DirtyType dirtyType, Exception e) {
        errorNum.incrementAndGet();
        if (!dirtyOptions.ignoreDirty()) {
            RuntimeException ex;
            if (e instanceof RuntimeException) {
                ex = (RuntimeException) e;
            } else {
                ex = new RuntimeException(e);
            }
            throw ex;
        }

        if (multipleSink) {
            handleMultipleDirtyData(dirtyData, dirtyType, e);
            return;
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
        metricData.invokeDirty(1, dirtyData.toString().getBytes(StandardCharsets.UTF_8).length);
    }

    private void handleMultipleDirtyData(Object dirtyData, DirtyType dirtyType, Exception e) {
        JsonNode rootNode;
        try {
            rootNode = jsonDynamicSchemaFormat.deserialize(((RowData) dirtyData).getBinary(0));
        } catch (Exception ex) {
            handleDirtyData(dirtyData, DirtyType.DESERIALIZE_ERROR, e);
            return;
        }

        if (dirtySink != null) {
            DirtyData.Builder<Object> builder = DirtyData.builder();
            try {
                builder.setData(dirtyData)
                        .setDirtyType(dirtyType)
                        .setLabels(jsonDynamicSchemaFormat.parse(rootNode, dirtyOptions.getLabels()))
                        .setLogTag(jsonDynamicSchemaFormat.parse(rootNode, dirtyOptions.getLogTag()))
                        .setDirtyMessage(e.getMessage())
                        .setIdentifier(jsonDynamicSchemaFormat.parse(rootNode, dirtyOptions.getIdentifier()));
                dirtySink.invoke(builder.build());
            } catch (Exception ex) {
                if (!dirtyOptions.ignoreSideOutputErrors()) {
                    throw new RuntimeException(ex);
                }
                LOG.warn("Dirty sink failed", ex);
            }
        }
        try {
            metricData.outputDirtyMetricsWithEstimate(
                    jsonDynamicSchemaFormat.parse(rootNode, databasePattern),
                    jsonDynamicSchemaFormat.parse(rootNode, tablePattern), 1,
                    ((RowData) dirtyData).getBinary(0).length);
        } catch (Exception ex) {
            metricData.invokeDirty(1, dirtyData.toString().getBytes(StandardCharsets.UTF_8).length);
        }
    }

    private void handleColumnsChange(String tableIdentifier, JsonNode rootNode, JsonNode physicalData) {
        String columns = parseColumns(rootNode, physicalData);
        String oldColumns = columnsMap.get(tableIdentifier);
        if (columns == null && oldColumns != null || (columns != null && !columns.equals(oldColumns))) {
            flushSingleTable(tableIdentifier, batchMap.get(tableIdentifier));
            if (!errorTables.contains(tableIdentifier)) {
                columnsMap.put(tableIdentifier, columns);
            } else {
                batchMap.remove(tableIdentifier);
                columnsMap.remove(tableIdentifier);
                errorTables.remove(tableIdentifier);
            }
        }
    }

    private String parseColumns(JsonNode rootNode, JsonNode physicalData) {
        // Add column key when fieldNames is not empty
        Iterator<String> fieldNames = null;
        try {
            RowType rowType = jsonDynamicSchemaFormat.extractSchema(rootNode);
            if (rowType != null) {
                fieldNames = rowType.getFieldNames().listIterator();
            }
        } catch (IllegalArgumentException e) {
            LOG.warn("extract schema failed", e);
            // Extract schema from physicalData
            JsonNode first = physicalData.isArray() ? physicalData.get(0) : physicalData;
            // Add column key when fieldNames is not empty
            fieldNames = first.fieldNames();
        }
        return genColumns(fieldNames);
    }

    private String genColumns(Iterator<String> fieldNames) {
        if (fieldNames != null && fieldNames.hasNext()) {
            StringBuilder sb = new StringBuilder();
            while (fieldNames.hasNext()) {
                String item = fieldNames.next();
                sb.append("`").append(item.trim()
                        .replace("`", "")).append("`,");
            }
            if (enableBatchDelete()) {
                sb.append(DORIS_DELETE_SIGN);
            } else {
                sb.deleteCharAt(sb.lastIndexOf(","));
            }
            return sb.toString();
        }
        return null;
    }

    @Override
    public synchronized void close() throws IOException {
        if (!closed) {
            closed = true;
            if (this.scheduledFuture != null) {
                scheduledFuture.cancel(false);
                this.scheduler.shutdown();
            }
            try {
                flush();
            } catch (Exception e) {
                LOG.warn("Writing records to doris failed.", e);
                throw new RuntimeException("Writing records to doris failed.", e);
            } finally {
                this.dorisStreamLoad.close();
            }
        }
    }

    @SuppressWarnings({"rawtypes"})
    public synchronized void flush() {
        flushing = true;
        if (!hasRecords()) {
            flushing = false;
            return;
        }

        for (Entry<String, List> kvs : batchMap.entrySet()) {
            flushSingleTable(kvs.getKey(), kvs.getValue());
        }
        if (!errorTables.isEmpty()) {
            // Clean the key that has errors
            errorTables.forEach(batchMap::remove);
            errorTables.clear();
        }
        batchBytes = 0;
        size = 0;
        LOG.info("Doris sink statistics: readInNum: {}, writeOutNum: {}, errorNum: {}, ddlNum: {}",
                readInNum.get(), writeOutNum.get(), errorNum.get(), ddlNum.get());
        flushing = false;
    }

    @SuppressWarnings({"rawtypes"})
    private void flushSingleTable(String tableIdentifier, List values) {
        if (checkFlushException(tableIdentifier) || values == null || values.isEmpty()) {
            return;
        }
        String loadValue = null;
        RespContent respContent = null;
        try {
            loadValue = OBJECT_MAPPER.writeValueAsString(values);
            respContent = load(tableIdentifier, loadValue);
            try {
                if (null != metricData && null != respContent) {
                    if (multipleSink) {
                        String[] tableWithDb = tableIdentifier.split("\\.");
                        metricData.outputMetrics(tableWithDb[0], null, tableWithDb[1],
                                respContent.getNumberLoadedRows(), respContent.getLoadBytes());
                    } else {
                        metricData.invoke(respContent.getNumberLoadedRows(), respContent.getLoadBytes());
                    }
                }
            } catch (Exception e) {
                LOG.warn("metricData invoke get err:", e);
            }
            writeOutNum.addAndGet(values.size());
            // Clean the data that has been loaded.
            values.clear();
        } catch (Exception e) {
            LOG.error(String.format("Flush table: %s error", tableIdentifier), e);
            // Makesure it is a dirty data
            if (respContent != null && StringUtils.isNotBlank(respContent.getErrorURL())) {
                flushExceptionMap.put(tableIdentifier, e);
                errorNum.getAndAdd(values.size());
                for (Object value : values) {
                    try {
                        handleDirtyData(OBJECT_MAPPER.readTree(OBJECT_MAPPER.writeValueAsString(value)),
                                DirtyType.BATCH_LOAD_ERROR, e);
                    } catch (IOException ex) {
                        if (!dirtyOptions.ignoreSideOutputErrors()) {
                            throw new RuntimeException(ex);
                        }
                        LOG.warn("Dirty sink failed", ex);
                    }
                }
                if (!ignoreSingleTableErrors) {
                    throw new RuntimeException(
                            String.format("Writing records to streamload of tableIdentifier:%s failed, the value: %s.",
                                    tableIdentifier, loadValue),
                            e);
                }
                errorTables.add(tableIdentifier);
                LOG.warn("The tableIdentifier: {} load failed and the data will be throw away in the future"
                        + " because the option 'sink.multiple.ignore-single-table-errors' is 'true'", tableIdentifier);
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private boolean hasRecords() {
        if (batchMap.isEmpty()) {
            return false;
        }
        boolean hasRecords = false;
        for (List value : batchMap.values()) {
            if (!value.isEmpty()) {
                hasRecords = true;
                break;
            }
        }
        return hasRecords;
    }

    private RespContent load(String tableIdentifier, String result) throws IOException {
        String[] tableWithDb = tableIdentifier.split("\\.");
        RespContent respContent = null;
        // Dynamic set COLUMNS_KEY for tableIdentifier every time for multiple sink scenario
        if (multipleSink) {
            executionOptions.getStreamLoadProp().put(COLUMNS_KEY, columnsMap.get(tableIdentifier));
        }
        for (int i = 0; i <= executionOptions.getMaxRetries(); i++) {
            try {
                respContent = dorisStreamLoad.load(tableWithDb[0], tableWithDb[1], result);
                break;
            } catch (StreamLoadException e) {
                LOG.error("doris sink error, retry times = {}", i, e);
                if (i >= executionOptions.getMaxRetries()) {
                    throw new IOException(e);
                }
                try {
                    dorisStreamLoad.setHostPort(getBackend());
                    LOG.warn("streamload error,switch be: {}",
                            dorisStreamLoad.getLoadUrlStr(tableWithDb[0], tableWithDb[1]), e);
                    Thread.sleep(1000L * i);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException("unable to flush; interrupted while doing another attempt", e);
                }
            }
        }
        return respContent;
    }

    private String getBackend() throws IOException {
        try {
            // get be url from fe
            return RestService.randomBackend(options, readOptions, LOG);
        } catch (IOException | DorisException e) {
            LOG.error("get backends info fail");
            throw new IOException(e);
        }
    }

    public void snapshotState(FunctionSnapshotContext context) throws Exception {
        if (metricData != null && metricStateListState != null) {
            MetricStateUtils.snapshotMetricStateForSinkMetricData(metricStateListState, metricData,
                    getRuntimeContext().getIndexOfThisSubtask());
        }
    }

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

    /**
     * Builder for {@link DorisDynamicSchemaOutputFormat}.
     */
    public static class Builder {

        private final DorisOptions.Builder optionsBuilder;
        private DorisReadOptions readOptions;
        private DorisExecutionOptions executionOptions;
        private String dynamicSchemaFormat;
        private String databasePattern;
        private String tablePattern;
        private boolean ignoreSingleTableErrors;
        private boolean multipleSink;
        private String inlongMetric;
        private String auditHostAndPorts;
        private String tableIdentifier;
        private DataType[] fieldDataTypes;
        private String[] fieldNames;
        private DirtyOptions dirtyOptions;
        private DirtySink<Object> dirtySink;

        public Builder() {
            this.optionsBuilder = DorisOptions.builder().setTableIdentifier("");
        }

        public DorisDynamicSchemaOutputFormat.Builder setFenodes(String fenodes) {
            this.optionsBuilder.setFenodes(fenodes);
            return this;
        }

        public DorisDynamicSchemaOutputFormat.Builder setUsername(String username) {
            this.optionsBuilder.setUsername(username);
            return this;
        }

        public DorisDynamicSchemaOutputFormat.Builder setPassword(String password) {
            this.optionsBuilder.setPassword(password);
            return this;
        }

        public DorisDynamicSchemaOutputFormat.Builder setTableIdentifier(String tableIdentifier) {
            this.tableIdentifier = tableIdentifier;
            return this;
        }

        public DorisDynamicSchemaOutputFormat.Builder setFieldDataTypes(DataType[] fieldDataTypes) {
            this.fieldDataTypes = fieldDataTypes;
            return this;
        }

        public DorisDynamicSchemaOutputFormat.Builder setFieldNames(String[] fieldNames) {
            this.fieldNames = fieldNames;
            return this;
        }

        public DorisDynamicSchemaOutputFormat.Builder setReadOptions(DorisReadOptions readOptions) {
            this.readOptions = readOptions;
            return this;
        }

        public DorisDynamicSchemaOutputFormat.Builder setExecutionOptions(DorisExecutionOptions executionOptions) {
            this.executionOptions = executionOptions;
            return this;
        }

        public DorisDynamicSchemaOutputFormat.Builder setDynamicSchemaFormat(
                String dynamicSchemaFormat) {
            this.dynamicSchemaFormat = dynamicSchemaFormat;
            return this;
        }

        public DorisDynamicSchemaOutputFormat.Builder setDatabasePattern(String databasePattern) {
            this.databasePattern = databasePattern;
            return this;
        }

        public DorisDynamicSchemaOutputFormat.Builder setTablePattern(String tablePattern) {
            this.tablePattern = tablePattern;
            return this;
        }

        public DorisDynamicSchemaOutputFormat.Builder setIgnoreSingleTableErrors(boolean ignoreSingleTableErrors) {
            this.ignoreSingleTableErrors = ignoreSingleTableErrors;
            return this;
        }

        public DorisDynamicSchemaOutputFormat.Builder setMultipleSink(boolean multipleSink) {
            this.multipleSink = multipleSink;
            return this;
        }

        public DorisDynamicSchemaOutputFormat.Builder setInlongMetric(String inlongMetric) {
            this.inlongMetric = inlongMetric;
            return this;
        }

        public DorisDynamicSchemaOutputFormat.Builder setAuditHostAndPorts(String auditHostAndPorts) {
            this.auditHostAndPorts = auditHostAndPorts;
            return this;
        }

        public DorisDynamicSchemaOutputFormat.Builder setDirtyOptions(DirtyOptions dirtyOptions) {
            this.dirtyOptions = dirtyOptions;
            return this;
        }

        public DorisDynamicSchemaOutputFormat.Builder setDirtySink(DirtySink<Object> dirtySink) {
            this.dirtySink = dirtySink;
            return this;
        }

        @SuppressWarnings({"rawtypes"})
        public DorisDynamicSchemaOutputFormat build() {
            LogicalType[] logicalTypes = null;
            if (!multipleSink) {
                logicalTypes = Arrays.stream(fieldDataTypes)
                        .map(DataType::getLogicalType).toArray(LogicalType[]::new);
            }
            return new DorisDynamicSchemaOutputFormat(
                    optionsBuilder.setTableIdentifier(tableIdentifier).build(), readOptions, executionOptions,
                    tableIdentifier, logicalTypes, fieldNames, dynamicSchemaFormat, databasePattern, tablePattern,
                    ignoreSingleTableErrors, inlongMetric, auditHostAndPorts, multipleSink, dirtyOptions, dirtySink);
        }
    }
}
