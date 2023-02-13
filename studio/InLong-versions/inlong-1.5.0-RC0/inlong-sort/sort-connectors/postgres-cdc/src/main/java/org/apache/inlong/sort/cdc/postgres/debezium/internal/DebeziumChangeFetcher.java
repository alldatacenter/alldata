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

package org.apache.inlong.sort.cdc.postgres.debezium.internal;

import com.ververica.cdc.debezium.internal.DebeziumOffset;
import com.ververica.cdc.debezium.internal.DebeziumOffsetSerializer;
import com.ververica.cdc.debezium.internal.Handover;
import io.debezium.connector.SnapshotRecord;
import io.debezium.data.Envelope;
import io.debezium.data.Envelope.FieldName;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.relational.Column;
import io.debezium.relational.TableId;
import io.debezium.relational.history.TableChanges.TableChange;
import io.debezium.relational.history.TableChanges.TableChangeType;
import java.sql.Types;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.collections.CollectionUtils;
import org.apache.flink.annotation.Internal;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.util.Collector;
import org.apache.inlong.sort.cdc.base.debezium.DebeziumDeserializationSchema;
import org.apache.inlong.sort.cdc.base.util.RecordUtils;
import org.apache.inlong.sort.cdc.postgres.connection.PostgreSQLJdbcConnectionOptions;
import org.apache.inlong.sort.cdc.postgres.connection.PostgreSQLJdbcConnectionProvider;
import org.apache.inlong.sort.cdc.postgres.manager.PostgreSQLQueryVisitor;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Handler that convert change messages from {@link DebeziumEngine} to data in Flink. Considering
 * Debezium in different mode has different strategies to hold the lock, e.g. snapshot, the handler
 * also needs different strategy. In snapshot phase, the handler needs to hold the lock until the
 * snapshot finishes. But in non-snapshot phase, the handler only needs to hold the lock when
 * emitting the records.
 *
 * @param <T> The type of elements produced by the handler.
 */
@Internal
public class DebeziumChangeFetcher<T> {

    private static final Logger LOG = LoggerFactory.getLogger(DebeziumChangeFetcher.class);

    private final SourceFunction.SourceContext<T> sourceContext;

    /**
     * The lock that guarantees that record emission and state updates are atomic, from the view of
     * taking a checkpoint.
     */
    private final Object checkpointLock;

    /**
     * The schema to convert from Debezium's messages into Flink's objects.
     */
    private final DebeziumDeserializationSchema<T> deserialization;

    /**
     * A collector to emit records in batch (bundle).
     */
    private final DebeziumCollector debeziumCollector;

    private final DebeziumOffset debeziumOffset;

    private final DebeziumOffsetSerializer stateSerializer;

    private final String heartbeatTopicPrefix;
    private final Handover handover;
    private boolean isInDbSnapshotPhase;
    private volatile boolean isRunning = true;

    // ---------------------------------------------------------------------------------------
    // Metrics
    // ---------------------------------------------------------------------------------------

    /**
     * Timestamp of change event. If the event is a snapshot event, the timestamp is 0L.
     */
    private volatile long messageTimestamp = 0L;

    /**
     * The last record processing time.
     */
    private volatile long processTime = 0L;

    /**
     * currentFetchEventTimeLag = FetchTime - messageTimestamp, where the FetchTime is the time the
     * record fetched into the source operator.
     */
    private volatile long fetchDelay = 0L;

    /**
     * emitDelay = EmitTime - messageTimestamp, where the EmitTime is the time the record leaves the
     * source operator.
     */
    private volatile long emitDelay = 0L;

    /**
     * PostgreSQL connector properties
     */
    private Properties properties;

    private PostgreSQLQueryVisitor postgreSQLQueryVisitor;

    private Map<String, TableChangeHolder> tableChangeMap = new ConcurrentHashMap<>();

    /**
     * the duration time which update ddl
     */
    private static final long DDL_UPDATE_INTERVAL = 2000;

    // ------------------------------------------------------------------------

    public DebeziumChangeFetcher(SourceFunction.SourceContext<T> sourceContext,
            DebeziumDeserializationSchema<T> deserialization, Properties properties, boolean isInDbSnapshotPhase,
            String heartbeatTopicPrefix, Handover handover) {
        this.sourceContext = sourceContext;
        this.checkpointLock = sourceContext.getCheckpointLock();
        this.deserialization = deserialization;
        this.properties = properties;
        this.isInDbSnapshotPhase = isInDbSnapshotPhase;
        this.heartbeatTopicPrefix = heartbeatTopicPrefix;
        this.debeziumCollector = new DebeziumCollector();
        this.debeziumOffset = new DebeziumOffset();
        this.stateSerializer = DebeziumOffsetSerializer.INSTANCE;
        this.handover = handover;

        String url = String.format("jdbc:postgresql://%s:%s/%s", properties.getProperty("database.hostname"),
                properties.getProperty("database.port"), properties.getProperty("database.dbname"));
        PostgreSQLJdbcConnectionOptions jdbcConnectionOptions = new PostgreSQLJdbcConnectionOptions(url,
                properties.getProperty("database.user"), properties.getProperty("database.password"));
        this.postgreSQLQueryVisitor = new PostgreSQLQueryVisitor(
                new PostgreSQLJdbcConnectionProvider(jdbcConnectionOptions));
    }

    /**
     * Take a snapshot of the Debezium handler state.
     *
     * <p>Important: This method must be called under the checkpoint lock.</p>
     */
    public byte[] snapshotCurrentState() throws Exception {
        // this method assumes that the checkpoint lock is held
        assert Thread.holdsLock(checkpointLock);
        if (debeziumOffset.sourceOffset == null || debeziumOffset.sourcePartition == null) {
            return null;
        }

        return stateSerializer.serialize(debeziumOffset);
    }

    /**
     * Process change messages from the {@link Handover} and collect the processed messages by
     * {@link Collector}.
     */
    public void runFetchLoop() throws Exception {
        try {
            // begin snapshot database phase
            if (isInDbSnapshotPhase) {
                List<ChangeEvent<SourceRecord, SourceRecord>> events = handover.pollNext();

                synchronized (checkpointLock) {
                    LOG.info("Database snapshot phase can't perform checkpoint, acquired Checkpoint lock.");
                    handleBatch(events);
                    while (isRunning && isInDbSnapshotPhase) {
                        handleBatch(handover.pollNext());
                    }
                }
                LOG.info("Received record from streaming binlog phase, released checkpoint lock.");
            }

            // begin streaming binlog phase
            while (isRunning) {
                // If the handover is closed or has errors, exit.
                // If there is no streaming phase, the handover will be closed by the engine.
                handleBatch(handover.pollNext());
            }
        } catch (Handover.ClosedException e) {
            // ignore
        }
    }

    public void close() {
        isRunning = false;
        handover.close();
    }

    // ---------------------------------------------------------------------------------------
    // Metric getter
    // ---------------------------------------------------------------------------------------

    /**
     * The metric indicates delay from data generation to entry into the system.
     *
     * <p>Note: the metric is available during the binlog phase. Use 0 to indicate the metric is
     * unavailable.</p>
     */
    public long getFetchDelay() {
        return fetchDelay;
    }

    /**
     * The metric indicates delay from data generation to leaving the source operator.
     *
     * <p>Note: the metric is available during the binlog phase. Use 0 to indicate the metric is
     * unavailable.</p>
     */
    public long getEmitDelay() {
        return emitDelay;
    }

    public long getIdleTime() {
        return System.currentTimeMillis() - processTime;
    }

    // ---------------------------------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------------------------------

    private void handleBatch(List<ChangeEvent<SourceRecord, SourceRecord>> changeEvents) throws Exception {
        if (CollectionUtils.isEmpty(changeEvents)) {
            return;
        }
        this.processTime = System.currentTimeMillis();

        for (ChangeEvent<SourceRecord, SourceRecord> event : changeEvents) {
            SourceRecord record = event.value();
            updateMessageTimestamp(record);
            fetchDelay = isInDbSnapshotPhase ? 0L : processTime - messageTimestamp;

            if (isHeartbeatEvent(record)) {
                // keep offset update
                synchronized (checkpointLock) {
                    debeziumOffset.setSourcePartition(record.sourcePartition());
                    debeziumOffset.setSourceOffset(record.sourceOffset());
                }
                // drop heartbeat events
                continue;
            }

            deserialization.deserialize(record, debeziumCollector, getTableChange(record));
            // deserialization.deserialize(record, debeziumCollector);

            if (!isSnapshotRecord(record)) {
                LOG.debug("Snapshot phase finishes.");
                isInDbSnapshotPhase = false;
            }

            // emit the actual records. this also updates offset state atomically
            emitRecordsUnderCheckpointLock(debeziumCollector.records, record.sourcePartition(), record.sourceOffset());
        }
    }

    private TableChange getTableChange(SourceRecord record) throws Exception {
        Envelope.Operation op = Envelope.operationFor(record);
        Schema valueSchema;
        if (op == Envelope.Operation.DELETE) {
            valueSchema = record.valueSchema().field(Envelope.FieldName.BEFORE).schema();
        } else {
            valueSchema = record.valueSchema().field(FieldName.AFTER).schema();
        }
        List<Field> fields = valueSchema.fields();

        TableId tableId = RecordUtils.getTableId(record);
        String schema = tableId.schema();
        String table = tableId.table();
        String id = tableId.identifier();

        TableChange tableChange;
        TableChangeHolder holder;
        if (!tableChangeMap.containsKey(id)) {
            List<Map<String, Object>> columns = this.postgreSQLQueryVisitor.getTableColumnsMetaData(schema, table);
            LOG.info("columns: {}", columns);
            tableChange = initTableChange(tableId, TableChangeType.CREATE, columns);
            holder = new TableChangeHolder();
            holder.tableChange = tableChange;
            holder.timestamp = System.currentTimeMillis();
            tableChangeMap.put(id, holder);
        } else {
            holder = tableChangeMap.get(id);
            tableChange = holder.tableChange;
            // diff fieldMap and columns of tableChange, to see if new column has been added
            if (System.currentTimeMillis() - holder.timestamp > DDL_UPDATE_INTERVAL
                    && holder.tableChange.getTable() instanceof TableImpl) {
                TableImpl tableImpl = (TableImpl) holder.tableChange.getTable();
                List<Column> columnList = tableImpl.columns();
                // only support add or remove columns
                if (columnList.size() != fields.size()) {
                    List<Map<String, Object>> columns = this.postgreSQLQueryVisitor.getTableColumnsMetaData(schema,
                            table);
                    LOG.info("columns: {}", columns);
                    tableChange = initTableChange(tableId, TableChangeType.CREATE, columns);
                    // update tableChange
                    holder.tableChange = tableChange;
                }
                holder.timestamp = System.currentTimeMillis();
                tableChangeMap.put(id, holder);
            }
        }
        return tableChange;
    }

    private TableChange initTableChange(TableId tableId, TableChangeType type, List<Map<String, Object>> columns) {
        List<Column> sortedColumns = new ArrayList<>();
        List<String> pkColumnNames = new ArrayList<>();
        String defaultCharsetName = "utf-8";
        for (Map<String, Object> column : columns) {
            // TODO query charset from table information_schema
            int columnLength = -1;
            Integer characterMaxLen = (Integer) column.get("character_maximum_length");
            if (characterMaxLen != null) {
                columnLength = characterMaxLen;
            }
            Integer columnScale = -1;
            Integer numPrecision = (Integer) column.get("numeric_precision");
            if (numPrecision != null) {
                columnScale = numPrecision;
            }
            boolean optional = "YES".equals(column.get("is_nullable"));
            boolean autoIncremented = false;
            boolean generated = false;

            String columnName = (String) column.get("column_name");
            String constraintType = (String) column.get("constraint_type");
            if (constraintType != null && constraintType.equalsIgnoreCase("PRIMARY KEY")) {
                pkColumnNames.add(columnName);
            }
            int position = (Integer) column.get("ordinal_position");
            String typeName = (String) column.get("data_type");
            int jdbcType = getJdbcType(typeName);
            int componentType = jdbcType;
            String charsetName = "utf-8";
            ColumnImpl columnImpl = new ColumnImpl(columnName, position, jdbcType, componentType, typeName, null,
                    charsetName, defaultCharsetName, columnLength, columnScale, optional, autoIncremented, generated);
            sortedColumns.add(columnImpl);
        }
        TableImpl tableImpl = new TableImpl(tableId, sortedColumns, pkColumnNames, defaultCharsetName);
        return new TableChange(type, tableImpl);
    }

    private int getJdbcType(String dataType) {
        switch (dataType) {
            case "integer":
                return Types.INTEGER;
            case "timestamp without time zone":
            case "timestamp with time zone":
                return Types.TIMESTAMP;
            case "smallint":
                return Types.SMALLINT;
            case "boolean":
                return Types.BOOLEAN;
            case "text":
                return Types.LONGNVARCHAR;
            case "numeric":
                return Types.NUMERIC;
            case "bigint":
                return Types.BIGINT;
            case "double precision":
                return Types.DOUBLE;
            case "\"char\"":
                return Types.CHAR;
            case "real":
                return Types.REAL;
            case "character varying":
            case "interval":
            case "name":
            case "inet":
            default:
                return Types.VARCHAR;
        }
    }

    private void emitRecordsUnderCheckpointLock(Queue<T> records, Map<String, ?> sourcePartition,
            Map<String, ?> sourceOffset) {
        // Emit the records. Use the checkpoint lock to guarantee
        // atomicity of record emission and offset state update.
        // The synchronized checkpointLock is reentrant. It's safe to sync again in snapshot mode.
        synchronized (checkpointLock) {
            T record;
            while ((record = records.poll()) != null) {
                emitDelay = isInDbSnapshotPhase ? 0L : System.currentTimeMillis() - messageTimestamp;
                sourceContext.collect(record);
            }
            // update offset to state
            debeziumOffset.setSourcePartition(sourcePartition);
            debeziumOffset.setSourceOffset(sourceOffset);
        }
    }

    private void updateMessageTimestamp(SourceRecord record) {
        Schema schema = record.valueSchema();
        Struct value = (Struct) record.value();
        if (schema.field(Envelope.FieldName.SOURCE) == null) {
            return;
        }

        Struct source = value.getStruct(Envelope.FieldName.SOURCE);
        if (source.schema().field(Envelope.FieldName.TIMESTAMP) == null) {
            return;
        }

        Long tsMs = source.getInt64(Envelope.FieldName.TIMESTAMP);
        if (tsMs != null) {
            this.messageTimestamp = tsMs;
        }
    }

    private boolean isHeartbeatEvent(SourceRecord record) {
        String topic = record.topic();
        return topic != null && topic.startsWith(heartbeatTopicPrefix);
    }

    private boolean isSnapshotRecord(SourceRecord record) {
        Struct value = (Struct) record.value();
        if (value != null) {
            Struct source = value.getStruct(Envelope.FieldName.SOURCE);
            SnapshotRecord snapshotRecord = SnapshotRecord.fromSource(source);
            // even if it is the last record of snapshot, i.e. SnapshotRecord.LAST
            // we can still recover from checkpoint and continue to read the binlog,
            // because the checkpoint contains binlog position
            return SnapshotRecord.TRUE == snapshotRecord;
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------

    private class DebeziumCollector implements Collector<T> {

        private final Queue<T> records = new ArrayDeque<>();

        @Override
        public void collect(T record) {
            records.add(record);
        }

        @Override
        public void close() {
        }
    }

    private class TableChangeHolder {

        public TableChange tableChange;
        public long timestamp;
    }
}
