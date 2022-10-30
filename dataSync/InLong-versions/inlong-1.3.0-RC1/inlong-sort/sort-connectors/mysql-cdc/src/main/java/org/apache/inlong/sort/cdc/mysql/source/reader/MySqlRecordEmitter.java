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

package org.apache.inlong.sort.cdc.mysql.source.reader;

import com.ververica.cdc.connectors.mysql.source.utils.RecordUtils;
import io.debezium.data.Envelope;
import io.debezium.document.Array;
import io.debezium.relational.TableId;
import io.debezium.relational.history.HistoryRecord;
import io.debezium.relational.history.TableChanges;
import io.debezium.relational.history.TableChanges.TableChange;
import org.apache.flink.api.connector.source.SourceOutput;
import org.apache.flink.connector.base.source.reader.RecordEmitter;
import org.apache.flink.util.Collector;
import org.apache.inlong.sort.cdc.debezium.DebeziumDeserializationSchema;
import org.apache.inlong.sort.cdc.debezium.history.FlinkJsonTableChangeSerializer;
import org.apache.inlong.sort.cdc.mysql.source.metrics.MySqlSourceReaderMetrics;
import org.apache.inlong.sort.cdc.mysql.source.offset.BinlogOffset;
import org.apache.inlong.sort.cdc.mysql.source.split.MySqlSplitState;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.apache.inlong.sort.cdc.mysql.source.utils.RecordUtils.getBinlogPosition;
import static org.apache.inlong.sort.cdc.mysql.source.utils.RecordUtils.getFetchTimestamp;
import static org.apache.inlong.sort.cdc.mysql.source.utils.RecordUtils.getHistoryRecord;
import static org.apache.inlong.sort.cdc.mysql.source.utils.RecordUtils.getMessageTimestamp;
import static org.apache.inlong.sort.cdc.mysql.source.utils.RecordUtils.getWatermark;
import static org.apache.inlong.sort.cdc.mysql.source.utils.RecordUtils.isDataChangeRecord;
import static org.apache.inlong.sort.cdc.mysql.source.utils.RecordUtils.isHeartbeatEvent;
import static org.apache.inlong.sort.cdc.mysql.source.utils.RecordUtils.isHighWatermarkEvent;
import static org.apache.inlong.sort.cdc.mysql.source.utils.RecordUtils.isSchemaChangeEvent;
import static org.apache.inlong.sort.cdc.mysql.source.utils.RecordUtils.isWatermarkEvent;

/**
 * The {@link RecordEmitter} implementation for {@link MySqlSourceReader}.
 *
 * <p>The {@link RecordEmitter} buffers the snapshot records of split and call the binlog reader to
 * emit records rather than emit the records directly.</p>
 */
public final class MySqlRecordEmitter<T>
        implements RecordEmitter<SourceRecord, T, MySqlSplitState> {

    private static final Logger LOG = LoggerFactory.getLogger(MySqlRecordEmitter.class);
    private static final FlinkJsonTableChangeSerializer TABLE_CHANGE_SERIALIZER =
            new FlinkJsonTableChangeSerializer();

    private final DebeziumDeserializationSchema<T> debeziumDeserializationSchema;
    private final MySqlSourceReaderMetrics sourceReaderMetrics;
    private final boolean includeSchemaChanges;
    private final OutputCollector<T> outputCollector;
    private volatile long binlogPos = 0L;
    private volatile long binlogFileNum = 0L;
    private volatile Boolean iSnapShot = false;
    private volatile long messageTimestamp = 0L;
    private volatile long fetchDelay = 0L;
    private volatile Long snapDuration;
    private volatile long snapEarliestTime = 0L;
    private volatile long snapProcessTime = 0L;

    public MySqlRecordEmitter(
            DebeziumDeserializationSchema<T> debeziumDeserializationSchema,
            MySqlSourceReaderMetrics sourceReaderMetrics,
            boolean includeSchemaChanges) {
        this.debeziumDeserializationSchema = debeziumDeserializationSchema;
        this.sourceReaderMetrics = sourceReaderMetrics;
        this.includeSchemaChanges = includeSchemaChanges;
        this.outputCollector = new OutputCollector<>();
    }

    @Override
    public void emitRecord(SourceRecord element, SourceOutput<T> output, MySqlSplitState splitState)
            throws Exception {
        if (isWatermarkEvent(element)) {
            BinlogOffset watermark = getWatermark(element);
            if (isHighWatermarkEvent(element) && splitState.isSnapshotSplitState()) {
                splitState.asSnapshotSplitState().setHighWatermark(watermark);
            }
        } else if (isSchemaChangeEvent(element) && splitState.isBinlogSplitState()) {
            HistoryRecord historyRecord = getHistoryRecord(element);
            Array tableChanges =
                    historyRecord.document().getArray(HistoryRecord.Fields.TABLE_CHANGES);
            TableChanges changes = TABLE_CHANGE_SERIALIZER.deserialize(tableChanges, true);
            for (TableChange tableChange : changes) {
                splitState.asBinlogSplitState().recordSchema(tableChange.getId(), tableChange);
            }
            if (includeSchemaChanges) {
                BinlogOffset position = getBinlogPosition(element);
                splitState.asBinlogSplitState().setStartingOffset(position);
                emitElement(element, output, null);
            }
        } else if (isDataChangeRecord(element)) {
//            updateStartingOffsetForSplit(splitState, element);
//            reportMetrics(element);
//
//            final Map<TableId, TableChange> tableSchemas =
//                splitState.getMySQLSplit().getTableSchemas();
//            final TableChange tableSchema =
//                tableSchemas.getOrDefault(getTableId(element), null);
//
//            emitElement(element, output, tableSchema);
            if (splitState.isBinlogSplitState()) {
                BinlogOffset position = getBinlogPosition(element);
                splitState.asBinlogSplitState().setStartingOffset(position);
                reportPos(position);
                iSnapShot = false;
                updateMessageTimestamp(element);
            } else {
                if (splitState.isSnapshotSplitState()) {
                    iSnapShot = true;
                }
                updateMessageTimestampSnap(element);
            }
            fetchDelay = System.currentTimeMillis() - messageTimestamp;
            reportMetrics(element);

            final Map<TableId, TableChange> tableSchemas =
                    splitState.getMySQLSplit().getTableSchemas();
            final TableChange tableSchema =
                    tableSchemas.getOrDefault(RecordUtils.getTableId(element), null);

            debeziumDeserializationSchema.deserialize(
                    element,
                    new Collector<T>() {
                        @Override
                        public void collect(final T t) {
                            sourceReaderMetrics.outputMetrics(1L,
                                    t.toString().getBytes(StandardCharsets.UTF_8).length);
                            output.collect(t);
                        }

                        @Override
                        public void close() {
                            // do nothing
                        }
                    },
                    tableSchema);
        } else if (isHeartbeatEvent(element)) {
            updateStartingOffsetForSplit(splitState, element);
        } else {
            // unknown element
            LOG.info("Meet unknown element {}, just skip.", element);
        }
    }

    private void updateStartingOffsetForSplit(MySqlSplitState splitState, SourceRecord element) {
        if (splitState.isBinlogSplitState()) {
            BinlogOffset position = getBinlogPosition(element);
            splitState.asBinlogSplitState().setStartingOffset(position);
        }
    }

    private void emitElement(SourceRecord element, SourceOutput<T> output,
            TableChange tableSchema) throws Exception {
        outputCollector.output = output;
        debeziumDeserializationSchema.deserialize(element, outputCollector, tableSchema);
    }

    private void reportMetrics(SourceRecord element) {
        long now = System.currentTimeMillis();
        // record the latest process time
        sourceReaderMetrics.recordProcessTime(now);
        Long messageTimestamp = getMessageTimestamp(element);

        if (messageTimestamp != null && messageTimestamp > 0L) {
            // report fetch delay
            Long fetchTimestamp = getFetchTimestamp(element);
            if (fetchTimestamp != null && fetchTimestamp >= messageTimestamp) {
                sourceReaderMetrics.recordFetchDelay(fetchTimestamp - messageTimestamp);
            }
            // report emit delay
            sourceReaderMetrics.recordEmitDelay(now - messageTimestamp);
        }
    }

    private void reportPos(BinlogOffset position) {
        try {
            binlogPos = position.getPosition();
            binlogFileNum = Long.parseLong(position.getFilename().replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Can not translate binlog pos or flile name.");
            }
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

    private void updateMessageTimestampSnap(SourceRecord record) {
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
            if (snapDuration == null) {
                this.snapEarliestTime = tsMs;
                this.snapDuration = System.currentTimeMillis() - tsMs;
            }
            if (this.snapEarliestTime > tsMs) {
                this.snapEarliestTime = tsMs;
            }
            this.snapProcessTime = tsMs - this.snapEarliestTime;
        }
    }

    private static class OutputCollector<T> implements Collector<T> {

        private SourceOutput<T> output;

        @Override
        public void collect(T record) {
            output.collect(record);
        }

        @Override
        public void close() {
            // do nothing
        }
    }
}
