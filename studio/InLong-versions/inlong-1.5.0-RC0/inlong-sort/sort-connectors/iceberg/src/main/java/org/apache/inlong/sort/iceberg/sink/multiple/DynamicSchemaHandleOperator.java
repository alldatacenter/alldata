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

package org.apache.inlong.sort.iceberg.sink.multiple;

import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.runtime.state.StateInitializationContext;
import org.apache.flink.runtime.state.StateSnapshotContext;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.streaming.api.operators.AbstractStreamOperator;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.runtime.tasks.ProcessingTimeCallback;
import org.apache.flink.streaming.runtime.tasks.ProcessingTimeService;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.RowType;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Table;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.SupportsNamespaces;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.exceptions.AlreadyExistsException;
import org.apache.iceberg.flink.CatalogLoader;
import org.apache.iceberg.flink.FlinkSchemaUtil;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableMap;
import org.apache.iceberg.types.Types.NestedField;
import org.apache.inlong.sort.base.dirty.DirtyData;
import org.apache.inlong.sort.base.dirty.DirtyOptions;
import org.apache.inlong.sort.base.dirty.DirtyType;
import org.apache.inlong.sort.base.dirty.sink.DirtySink;
import org.apache.inlong.sort.base.format.AbstractDynamicSchemaFormat;
import org.apache.inlong.sort.base.format.DynamicSchemaFormatFactory;
import org.apache.inlong.sort.base.metric.MetricOption;
import org.apache.inlong.sort.base.metric.MetricOption.RegisteredMetric;
import org.apache.inlong.sort.base.metric.MetricState;
import org.apache.inlong.sort.base.metric.sub.SinkTableMetricData;
import org.apache.inlong.sort.base.sink.MultipleSinkOption;
import org.apache.inlong.sort.base.sink.TableChange;
import org.apache.inlong.sort.base.sink.TableChange.AddColumn;
import org.apache.inlong.sort.base.util.MetricStateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static org.apache.inlong.sort.base.Constants.DIRTY_BYTES_OUT;
import static org.apache.inlong.sort.base.Constants.DIRTY_RECORDS_OUT;
import static org.apache.inlong.sort.base.Constants.INLONG_METRIC_STATE_NAME;
import static org.apache.inlong.sort.base.Constants.NUM_BYTES_OUT;
import static org.apache.inlong.sort.base.Constants.NUM_RECORDS_OUT;

public class DynamicSchemaHandleOperator extends AbstractStreamOperator<RecordWithSchema>
        implements
            OneInputStreamOperator<RowData, RecordWithSchema>,
            ProcessingTimeCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicSchemaHandleOperator.class);
    private static final long HELPER_DEBUG_INTERVEL = 10 * 60 * 1000;
    private static final long serialVersionUID = 1L;

    private final CatalogLoader catalogLoader;
    private final MultipleSinkOption multipleSinkOption;

    private transient Catalog catalog;
    private transient SupportsNamespaces asNamespaceCatalog;
    private transient AbstractDynamicSchemaFormat<JsonNode> dynamicSchemaFormat;
    private transient ProcessingTimeService processingTimeService;

    // record cache, wait schema to consume record
    private transient Map<TableIdentifier, Queue<RecordWithSchema>> recordQueues;

    // schema cache
    private transient Map<TableIdentifier, Schema> schemaCache;

    // blacklist to filter schema update failed table
    private transient Set<TableIdentifier> blacklist;

    private final DirtyOptions dirtyOptions;
    private @Nullable final DirtySink<Object> dirtySink;

    // metric
    private final String inlongMetric;
    private final String auditHostAndPorts;
    private @Nullable transient SinkTableMetricData metricData;
    private transient ListState<MetricState> metricStateListState;
    private transient MetricState metricState;

    public DynamicSchemaHandleOperator(CatalogLoader catalogLoader,
            MultipleSinkOption multipleSinkOption, DirtyOptions dirtyOptions,
            @Nullable DirtySink<Object> dirtySink, String inlongMetric, String auditHostAndPorts) {
        this.catalogLoader = catalogLoader;
        this.multipleSinkOption = multipleSinkOption;
        this.dirtyOptions = dirtyOptions;
        this.dirtySink = dirtySink;
        this.inlongMetric = inlongMetric;
        this.auditHostAndPorts = auditHostAndPorts;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void open() throws Exception {
        super.open();
        this.catalog = catalogLoader.loadCatalog();
        this.asNamespaceCatalog =
                catalog instanceof SupportsNamespaces ? (SupportsNamespaces) catalog : null;

        this.dynamicSchemaFormat = DynamicSchemaFormatFactory.getFormat(
                multipleSinkOption.getFormat(), multipleSinkOption.getFormatOption());

        this.processingTimeService = getRuntimeContext().getProcessingTimeService();
        processingTimeService.registerTimer(
                processingTimeService.getCurrentProcessingTime() + HELPER_DEBUG_INTERVEL, this);

        this.recordQueues = new HashMap<>();
        this.schemaCache = new HashMap<>();
        this.blacklist = new HashSet<>();

        // Initialize metric
        MetricOption metricOption = MetricOption.builder()
                .withInlongLabels(inlongMetric)
                .withInlongAudit(auditHostAndPorts)
                .withInitRecords(metricState != null ? metricState.getMetricValue(NUM_RECORDS_OUT) : 0L)
                .withInitBytes(metricState != null ? metricState.getMetricValue(NUM_BYTES_OUT) : 0L)
                .withInitDirtyRecords(metricState != null ? metricState.getMetricValue(DIRTY_RECORDS_OUT) : 0L)
                .withInitDirtyBytes(metricState != null ? metricState.getMetricValue(DIRTY_BYTES_OUT) : 0L)
                .withRegisterMetric(RegisteredMetric.ALL)
                .build();
        if (metricOption != null) {
            metricData = new SinkTableMetricData(metricOption, getRuntimeContext().getMetricGroup());
        }
    }

    @Override
    public void close() throws Exception {
        super.close();
        if (catalog instanceof Closeable) {
            ((Closeable) catalog).close();
        }
    }

    @Override
    public void processElement(StreamRecord<RowData> element) throws Exception {
        JsonNode jsonNode = null;
        try {
            jsonNode = dynamicSchemaFormat.deserialize(element.getValue().getBinary(0));
        } catch (Exception e) {
            LOGGER.error(String.format("Deserialize error, raw data: %s",
                    new String(element.getValue().getBinary(0))), e);
            handleDirtyData(new String(element.getValue().getBinary(0)),
                    null, DirtyType.DESERIALIZE_ERROR, e, TableIdentifier.of("unknow", "unknow"));
        }
        TableIdentifier tableId = null;
        try {
            tableId = parseId(jsonNode);
        } catch (Exception e) {
            LOGGER.error(String.format("Table identifier parse error, raw data: %s", jsonNode), e);
            handleDirtyData(jsonNode, jsonNode, DirtyType.TABLE_IDENTIFIER_PARSE_ERROR,
                    e, TableIdentifier.of("unknow", "unknow"));
        }
        if (blacklist.contains(tableId)) {
            return;
        }
        boolean isDDL = dynamicSchemaFormat.extractDDLFlag(jsonNode);
        if (isDDL) {
            execDDL(jsonNode, tableId);
        } else {
            execDML(jsonNode, tableId);
        }
    }

    private void handleDirtyData(Object dirtyData,
            JsonNode rootNode,
            DirtyType dirtyType,
            Exception e,
            TableIdentifier tableId) {
        if (!dirtyOptions.ignoreDirty()) {
            RuntimeException ex;
            if (e instanceof RuntimeException) {
                ex = (RuntimeException) e;
            } else {
                ex = new RuntimeException(e);
            }
            throw ex;
        }
        if (dirtySink != null) {
            DirtyData.Builder<Object> builder = DirtyData.builder();
            try {
                builder.setData(dirtyData)
                        .setDirtyType(dirtyType)
                        .setDirtyMessage(e.getMessage());
                if (rootNode != null) {
                    builder.setLabels(dynamicSchemaFormat.parse(rootNode, dirtyOptions.getLabels()))
                            .setLogTag(dynamicSchemaFormat.parse(rootNode, dirtyOptions.getLogTag()))
                            .setIdentifier(dynamicSchemaFormat.parse(rootNode, dirtyOptions.getIdentifier()));
                } else {
                    builder.setLabels(dirtyOptions.getLabels())
                            .setLogTag(dirtyOptions.getLogTag())
                            .setIdentifier(dirtyOptions.getIdentifier());
                }
                dirtySink.invoke(builder.build());
                if (metricData != null) {
                    metricData.outputDirtyMetricsWithEstimate(
                            tableId.namespace().toString(), null, tableId.name(), dirtyData);
                }
            } catch (Exception ex) {
                if (!dirtyOptions.ignoreSideOutputErrors()) {
                    throw new RuntimeException(ex);
                }
                LOG.warn("Dirty sink failed", ex);
            }
        }
    }

    @Override
    public void onProcessingTime(long timestamp) {
        LOG.info("Black list table: {} at time {}.", blacklist, timestamp);
        processingTimeService.registerTimer(
                processingTimeService.getCurrentProcessingTime() + HELPER_DEBUG_INTERVEL, this);
    }

    @Override
    public void snapshotState(StateSnapshotContext context) throws Exception {
        if (metricData != null && metricStateListState != null) {
            MetricStateUtils.snapshotMetricStateForSinkMetricData(metricStateListState, metricData,
                    getRuntimeContext().getIndexOfThisSubtask());
        }
    }

    @Override
    public void initializeState(StateInitializationContext context) throws Exception {
        // init metric state
        if (this.inlongMetric != null) {
            this.metricStateListState = context.getOperatorStateStore().getUnionListState(
                    new ListStateDescriptor<>(
                            String.format(INLONG_METRIC_STATE_NAME), TypeInformation.of(new TypeHint<MetricState>() {
                            })));
        }
        if (context.isRestored()) {
            metricState = MetricStateUtils.restoreMetricState(metricStateListState,
                    getRuntimeContext().getIndexOfThisSubtask(), getRuntimeContext().getNumberOfParallelSubtasks());
        }
    }

    private void execDDL(JsonNode jsonNode, TableIdentifier tableId) {
        // todo:parse ddl sql
    }

    private void execDML(JsonNode jsonNode, TableIdentifier tableId) {
        RecordWithSchema record = parseRecord(jsonNode, tableId);
        if (record == null) {
            return;
        }
        Schema schema = schemaCache.get(record.getTableId());
        Schema dataSchema = record.getSchema();
        recordQueues.compute(record.getTableId(), (k, v) -> {
            if (v == null) {
                v = new LinkedList<>();
            }
            v.add(record);
            return v;
        });
        if (schema == null) {
            handleTableCreateEventFromOperator(record.getTableId(), dataSchema);
        } else {
            handleSchemaInfoEvent(record.getTableId(), schema);
        }
    }

    // ======================== All coordinator interact request and response method ============================
    private void handleSchemaInfoEvent(TableIdentifier tableId, Schema schema) {
        schemaCache.put(tableId, schema);
        Schema latestSchema = schemaCache.get(tableId);
        Queue<RecordWithSchema> queue = recordQueues.get(tableId);
        while (queue != null && !queue.isEmpty()) {
            Schema dataSchema = queue.peek().getSchema();
            // if compatible, this means that the current schema is the latest schema
            // if not, prove the need to update the current schema
            if (isCompatible(latestSchema, dataSchema)) {
                RecordWithSchema recordWithSchema = queue.poll()
                        .refreshFieldId(latestSchema)
                        .refreshRowData((jsonNode, schema1) -> {
                            try {
                                return dynamicSchemaFormat.extractRowData(jsonNode, FlinkSchemaUtil.convert(schema1));
                            } catch (Exception e) {
                                LOG.warn("Ignore table {} schema change, old: {} new: {}.",
                                        tableId, dataSchema, latestSchema, e);
                                blacklist.add(tableId);
                                handleDirtyData(jsonNode, jsonNode, DirtyType.EXTRACT_ROWDATA_ERROR, e, tableId);
                            }
                            return Collections.emptyList();
                        });
                output.collect(new StreamRecord<>(recordWithSchema));
            } else {
                handldAlterSchemaEventFromOperator(tableId, latestSchema, dataSchema);
                break;
            }
        }
    }

    // ================================ All coordinator handle method ==============================================
    private void handleTableCreateEventFromOperator(TableIdentifier tableId, Schema schema) {
        if (!catalog.tableExists(tableId)) {
            if (asNamespaceCatalog != null && !asNamespaceCatalog.namespaceExists(tableId.namespace())) {
                try {
                    asNamespaceCatalog.createNamespace(tableId.namespace());
                    LOG.info("Auto create Database({}) in Catalog({}).", tableId.namespace(), catalog.name());
                } catch (AlreadyExistsException e) {
                    LOG.warn("Database({}) already exist in Catalog({})!", tableId.namespace(), catalog.name());
                }
            }
            ImmutableMap.Builder<String, String> properties = ImmutableMap.builder();
            properties.put("format-version", "2");
            properties.put("write.upsert.enabled", "true");
            // for hive visible
            properties.put("engine.hive.enabled", "true");
            try {
                catalog.createTable(tableId, schema, PartitionSpec.unpartitioned(), properties.build());
                LOG.info("Auto create Table({}) in Database({}) in Catalog({})!",
                        tableId.name(), tableId.namespace(), catalog.name());
            } catch (AlreadyExistsException e) {
                LOG.warn("Table({}) already exist in Database({}) in Catalog({})!",
                        tableId.name(), tableId.namespace(), catalog.name());
            }
        }
        handleSchemaInfoEvent(tableId, catalog.loadTable(tableId).schema());
    }

    private void handldAlterSchemaEventFromOperator(TableIdentifier tableId, Schema oldSchema, Schema newSchema) {
        Table table = catalog.loadTable(tableId);
        // The transactionality of changes is guaranteed by comparing the old schema with the current schema of the
        // table.
        // Judging whether changes can be made by schema comparison (currently only column additions are supported),
        // for scenarios that cannot be changed, it is always considered that there is a problem with the data.
        Transaction transaction = table.newTransaction();
        if (table.schema().sameSchema(oldSchema)) {
            List<TableChange> tableChanges = SchemaChangeUtils.diffSchema(oldSchema, newSchema);
            if (!canHandleWithSchemaUpdatePolicy(tableId, tableChanges)) {
                // If can not handle this schema update, should not push data into next operator
                return;
            }
            SchemaChangeUtils.applySchemaChanges(transaction.updateSchema(), tableChanges);
            LOG.info("Schema evolution in table({}) for table change: {}", tableId, tableChanges);
        }
        transaction.commitTransaction();
        handleSchemaInfoEvent(tableId, table.schema());
    }

    // =============================== Utils method =================================================================
    // The way to judge compatibility is whether all the field names in the old schema exist in the new schema
    private boolean isCompatible(Schema newSchema, Schema oldSchema) {
        for (NestedField field : oldSchema.columns()) {
            if (newSchema.findField(field.name()) == null) {
                return false;
            }
        }
        return true;
    }

    private TableIdentifier parseId(JsonNode data) throws IOException {
        String databaseStr = dynamicSchemaFormat.parse(data, multipleSinkOption.getDatabasePattern());
        String tableStr = dynamicSchemaFormat.parse(data, multipleSinkOption.getTablePattern());
        return TableIdentifier.of(databaseStr, tableStr);
    }

    private RecordWithSchema parseRecord(JsonNode data, TableIdentifier tableId) {
        try {
            List<String> pkListStr = dynamicSchemaFormat.extractPrimaryKeyNames(data);
            RowType schema = dynamicSchemaFormat.extractSchema(data, pkListStr);
            return new RecordWithSchema(
                    data,
                    FlinkSchemaUtil.convert(FlinkSchemaUtil.toSchema(schema)),
                    tableId,
                    pkListStr);
        } catch (Exception e) {
            handleDirtyData(data, data, DirtyType.EXTRACT_SCHEMA_ERROR, e, tableId);
        }
        return null;
    }

    private boolean canHandleWithSchemaUpdatePolicy(TableIdentifier tableId, List<TableChange> tableChanges) {
        boolean canHandle = true;
        for (TableChange tableChange : tableChanges) {
            canHandle &= MultipleSinkOption.canHandleWithSchemaUpdate(tableId.toString(), tableChange,
                    multipleSinkOption.getSchemaUpdatePolicy());
            if (!(tableChange instanceof AddColumn)) {
                // todo:currently iceberg can only handle addColumn, so always return false
                LOG.info("Ignore table {} schema change: {} because iceberg can't handle it.",
                        tableId, tableChange);
                canHandle = false;
            }
            if (!canHandle) {
                blacklist.add(tableId);
                break;
            }
        }
        return canHandle;
    }
}
